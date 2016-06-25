/*
 * Copyright 2016-present Open Networking Laboratory
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onosproject.store.primitives.impl;

import static com.google.common.base.Preconditions.checkNotNull;
import io.atomix.catalyst.concurrent.Listener;
import io.atomix.catalyst.concurrent.Listeners;
import io.atomix.catalyst.concurrent.ThreadContext;
import io.atomix.catalyst.serializer.SerializationException;
import io.atomix.catalyst.transport.Address;
import io.atomix.catalyst.transport.Connection;
import io.atomix.catalyst.transport.MessageHandler;
import io.atomix.catalyst.transport.TransportException;
import io.atomix.catalyst.util.Assert;
import io.atomix.catalyst.util.reference.ReferenceCounted;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import org.apache.commons.io.IOUtils;
import org.onlab.util.Tools;
import org.onosproject.cluster.PartitionId;
import org.onosproject.store.cluster.messaging.MessagingException;
import org.onosproject.store.cluster.messaging.MessagingService;

import com.google.common.base.MoreObjects;
import com.google.common.base.Throwables;
import com.google.common.collect.Maps;

/**
 * {@link Connection} implementation for CopycatTransport.
 */
public class CopycatTransportConnection implements Connection {

    private final Listeners<Throwable> exceptionListeners = new Listeners<>();
    private final Listeners<Connection> closeListeners = new Listeners<>();

    static final byte SUCCESS = 0x03;
    static final byte FAILURE = 0x04;

    private final long connectionId;
    private final CopycatTransport.Mode mode;
    private final Address remoteAddress;
    private final MessagingService messagingService;
    private final String outboundMessageSubject;
    private final String inboundMessageSubject;
    private final ThreadContext context;
    private final Map<Class<?>, InternalHandler> handlers = Maps.newConcurrentMap();

    CopycatTransportConnection(long connectionId,
            CopycatTransport.Mode mode,
            PartitionId partitionId,
            Address address,
            MessagingService messagingService,
            ThreadContext context) {
        this.connectionId = connectionId;
        this.mode = checkNotNull(mode);
        this.remoteAddress = checkNotNull(address);
        this.messagingService = checkNotNull(messagingService);
        if (mode == CopycatTransport.Mode.CLIENT) {
            this.outboundMessageSubject = String.format("onos-copycat-%s", partitionId);
            this.inboundMessageSubject = String.format("onos-copycat-%s-%d", partitionId, connectionId);
        } else {
            this.outboundMessageSubject = String.format("onos-copycat-%s-%d", partitionId, connectionId);
            this.inboundMessageSubject = String.format("onos-copycat-%s", partitionId);
        }
        this.context = checkNotNull(context);
    }

    public void setBidirectional() {
        messagingService.registerHandler(inboundMessageSubject, (sender, payload) -> {
            try (DataInputStream input = new DataInputStream(new ByteArrayInputStream(payload))) {
                if (input.readLong() !=  connectionId) {
                    throw new IllegalStateException("Invalid connection Id");
                }
                return handle(IOUtils.toByteArray(input));
            } catch (IOException e) {
                Throwables.propagate(e);
                return null;
            }
        });
    }

    @Override
    public <T, U> CompletableFuture<U> send(T message) {
        ThreadContext context = ThreadContext.currentContextOrThrow();
        CompletableFuture<U> result = new CompletableFuture<>();
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            new DataOutputStream(baos).writeLong(connectionId);
            context.serializer().writeObject(message, baos);
            if (message instanceof ReferenceCounted) {
                ((ReferenceCounted<?>) message).release();
            }
            messagingService.sendAndReceive(CopycatTransport.toEndpoint(remoteAddress),
                                            outboundMessageSubject,
                                            baos.toByteArray(),
                                            context.executor())
                    .whenComplete((r, e) -> {
                        Throwable wrappedError = e;
                        if (e != null) {
                            Throwable rootCause = Throwables.getRootCause(e);
                            if (MessagingException.class.isAssignableFrom(rootCause.getClass())) {
                                wrappedError = new TransportException(e);
                            }
                        }
                        handleResponse(r, wrappedError, result, context);
                    });
        } catch (SerializationException | IOException e) {
            result.completeExceptionally(e);
        }
        return result;
    }

    private <T> void handleResponse(byte[] response,
                                    Throwable error,
                                    CompletableFuture<T> future,
                                    ThreadContext context) {
        if (error != null) {
            context.execute(() -> future.completeExceptionally(error));
            return;
        }
        checkNotNull(response);
        InputStream input = new ByteArrayInputStream(response);
        try {
            byte status = (byte) input.read();
            if (status == FAILURE) {
                Throwable t = context.serializer().readObject(input);
                context.execute(() -> future.completeExceptionally(t));
            } else {
                context.execute(() -> future.complete(context.serializer().readObject(input)));
            }
        } catch (IOException e) {
            context.execute(() -> future.completeExceptionally(e));
        }
    }

    @Override
    public <T, U> Connection handler(Class<T> type, MessageHandler<T, U> handler) {
        Assert.notNull(type, "type");
        handlers.put(type, new InternalHandler(handler, ThreadContext.currentContextOrThrow()));
        return null;
    }

   public CompletableFuture<byte[]> handle(byte[] message) {
        try {
            Object request = context.serializer().readObject(new ByteArrayInputStream(message));
            InternalHandler handler = handlers.get(request.getClass());
            if (handler == null) {
                return Tools.exceptionalFuture(new IllegalStateException(
                        "No handler registered for " + request.getClass()));
            }
            return handler.handle(request).handle((result, error) -> {
                try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                    baos.write(error != null ? FAILURE : SUCCESS);
                    context.serializer().writeObject(error != null ? error : result, baos);
                    return baos.toByteArray();
                } catch (IOException e) {
                    Throwables.propagate(e);
                    return null;
                }
            });
        } catch (Exception e) {
            return Tools.exceptionalFuture(e);
        }
    }

    @Override
    public Listener<Throwable> exceptionListener(Consumer<Throwable> listener) {
        return exceptionListeners.add(listener);
    }

    @Override
    public Listener<Connection> closeListener(Consumer<Connection> listener) {
        return closeListeners.add(listener);
    }

    @Override
    public CompletableFuture<Void> close() {
        closeListeners.forEach(listener -> listener.accept(this));
        if (mode == CopycatTransport.Mode.CLIENT) {
            messagingService.unregisterHandler(inboundMessageSubject);
        }
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public int hashCode() {
        return Objects.hash(connectionId);
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof CopycatTransportConnection)) {
            return false;
        }
        return connectionId == ((CopycatTransportConnection) other).connectionId;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("id", connectionId)
                .toString();
    }

    @SuppressWarnings("rawtypes")
    private final class InternalHandler {

        private final MessageHandler handler;
        private final ThreadContext context;

        private InternalHandler(MessageHandler handler, ThreadContext context) {
            this.handler = handler;
            this.context = context;
        }

        @SuppressWarnings("unchecked")
        public CompletableFuture<Object> handle(Object message) {
            CompletableFuture<Object> answer = new CompletableFuture<>();
            context.execute(() -> handler.handle(message).whenComplete((r, e) -> {
                if (e != null) {
                    answer.completeExceptionally((Throwable) e);
                } else {
                    answer.complete(r);
                }
            }));
            return answer;
        }
    }
}

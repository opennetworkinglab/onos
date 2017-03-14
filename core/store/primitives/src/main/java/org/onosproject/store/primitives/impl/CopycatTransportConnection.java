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

import com.google.common.base.MoreObjects;
import com.google.common.base.Throwables;
import com.google.common.collect.Maps;
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
import org.apache.commons.io.IOUtils;
import org.onlab.util.Tools;
import org.onosproject.cluster.PartitionId;
import org.onosproject.store.cluster.messaging.MessagingException;
import org.onosproject.store.cluster.messaging.MessagingService;
import org.slf4j.Logger;

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

import static com.google.common.base.Preconditions.checkNotNull;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * {@link Connection} implementation for CopycatTransport.
 */
public class CopycatTransportConnection implements Connection {

    private final Logger log = getLogger(getClass());
    private final Listeners<Throwable> exceptionListeners = new Listeners<>();
    private final Listeners<Connection> closeListeners = new Listeners<>();

    static final byte MESSAGE = 0x00;
    static final byte CONNECT = 0x01;
    static final byte CLOSE = 0x02;
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

    /**
     * Creates a new connection to the server side of a connection.
     *
     * @return A completable future to be completed once the connection has been created.
     */
    public CompletableFuture<Connection> connect() {
        log.debug("Connecting to {}", remoteAddress);
        messagingService.registerHandler(inboundMessageSubject, (sender, payload) -> {
            try (DataInputStream input = new DataInputStream(new ByteArrayInputStream(payload))) {
                byte type = input.readByte();
                switch (type) {
                    case CONNECT:
                        throw new IllegalStateException("Cannot connect to client");
                    case CLOSE:
                        log.debug("Received close event");
                        cleanup();
                        return CompletableFuture.completedFuture(success());
                    case MESSAGE:
                        if (input.readLong() != connectionId) {
                            throw new IllegalStateException("Invalid connection ID");
                        }
                        return handle(IOUtils.toByteArray(input));
                    default:
                        throw new IllegalStateException("Invalid message type");
                }
            } catch (IOException e) {
                Throwables.propagate(e);
                return null;
            }
        });
        return sendStatus(CONNECT).thenApplyAsync(v -> this, context.executor());
    }

    /**
     * Sends a status message ({@code CONNECT} or {@code CLOSE}) to the other side of the connection.
     */
    private CompletableFuture<Void> sendStatus(int status) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            DataOutputStream dos = new DataOutputStream(baos);
            dos.writeByte(status);
            dos.writeLong(connectionId);
            messagingService.sendAndReceive(CopycatTransport.toEndpoint(remoteAddress),
                                            outboundMessageSubject,
                                            baos.toByteArray(),
                                            context.executor())
                    .whenComplete((response, error) -> {
                        Throwable wrappedError = error;
                        if (error != null) {
                            Throwable rootCause = Throwables.getRootCause(error);
                            if (MessagingException.class.isAssignableFrom(rootCause.getClass())) {
                                wrappedError = new TransportException(error);
                            }
                            future.completeExceptionally(wrappedError);
                        } else {
                            InputStream input = new ByteArrayInputStream(response);
                            try {
                                if (input.read() == FAILURE) {
                                    Throwable t = context.serializer().readObject(input);
                                    future.completeExceptionally(t);
                                } else {
                                    future.complete(null);
                                }
                            } catch (IOException e) {
                                future.completeExceptionally(e);
                            }
                        }
                    });
            return future;
        } catch (IOException e) {
            return Tools.exceptionalFuture(e);
        }
    }

    /**
     * Returns a {@link CompletableFuture} completed with a {@code CopycatTransportConnection.SUCCESS} status.
     *
     * @return A future completed with the {@code CopycatTransportConnection.SUCCESS} status.
     */
    static byte[] success() {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            baos.write(SUCCESS);
            return baos.toByteArray();
        } catch (IOException e) {
            Throwables.propagate(e);
            return null;
        }
    }

    @Override
    public <T, U> CompletableFuture<U> send(T message) {
        ThreadContext context = ThreadContext.currentContextOrThrow();
        CompletableFuture<U> result = new CompletableFuture<>();
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            DataOutputStream dos = new DataOutputStream(baos);
            dos.writeByte(MESSAGE);
            dos.writeLong(connectionId);
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
                        handleResponse(r, wrappedError, result);
                    });
        } catch (SerializationException | IOException e) {
            result.completeExceptionally(e);
        }
        return result;
    }

    /**
     * Handles a response received from the other side of the connection.
     */
    private <T> void handleResponse(
            byte[] response,
            Throwable error,
            CompletableFuture<T> future) {
        if (error != null) {
            future.completeExceptionally(error);
            return;
        }
        checkNotNull(response);
        InputStream input = new ByteArrayInputStream(response);
        try {
            byte status = (byte) input.read();
            if (status == FAILURE) {
                Throwable t = context.serializer().readObject(input);
                future.completeExceptionally(t);
            } else {
                try {
                    future.complete(context.serializer().readObject(input));
                } catch (SerializationException e) {
                    future.completeExceptionally(e);
                }
            }
        } catch (IOException e) {
            future.completeExceptionally(e);
        }
    }

    @Override
    public <T, U> Connection handler(Class<T> type, MessageHandler<T, U> handler) {
        Assert.notNull(type, "type");
        handlers.put(type, new InternalHandler(handler, ThreadContext.currentContextOrThrow()));
        return null;
    }

    /**
     * Handles a message received from the other side of the connection.
     *
     * @param message the message to handle.
     * @return a completable future to be completed with the serialized response.
     */
    CompletableFuture<byte[]> handle(byte[] message) {
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
        log.debug("Closing connection to {}", remoteAddress);
        return sendStatus(CLOSE).whenComplete((result, error) -> cleanup());
    }

    /**
     * Cleans up and closes the connection.
     */
    void cleanup() {
        if (mode == CopycatTransport.Mode.CLIENT) {
            messagingService.unregisterHandler(inboundMessageSubject);
        }
        closeListeners.forEach(listener -> listener.accept(this));
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
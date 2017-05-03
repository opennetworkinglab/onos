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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Function;

import com.google.common.base.Throwables;
import io.atomix.catalyst.concurrent.Listener;
import io.atomix.catalyst.concurrent.Listeners;
import io.atomix.catalyst.concurrent.ThreadContext;
import io.atomix.catalyst.serializer.SerializationException;
import io.atomix.catalyst.transport.Connection;
import io.atomix.catalyst.transport.TransportException;
import io.atomix.catalyst.util.reference.ReferenceCounted;
import org.apache.commons.io.IOUtils;
import org.onlab.util.Tools;
import org.onosproject.cluster.PartitionId;
import org.onosproject.store.cluster.messaging.Endpoint;
import org.onosproject.store.cluster.messaging.MessagingException;
import org.onosproject.store.cluster.messaging.MessagingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.onosproject.store.primitives.impl.CopycatTransport.CLOSE;
import static org.onosproject.store.primitives.impl.CopycatTransport.FAILURE;
import static org.onosproject.store.primitives.impl.CopycatTransport.MESSAGE;
import static org.onosproject.store.primitives.impl.CopycatTransport.SUCCESS;

/**
 * Base Copycat Transport connection.
 */
public class CopycatTransportConnection implements Connection {
    private final Logger log = LoggerFactory.getLogger(getClass());
    private final long connectionId;
    private final String localSubject;
    private final String remoteSubject;
    private final PartitionId partitionId;
    private final Endpoint endpoint;
    private final MessagingService messagingService;
    private final ThreadContext context;
    private final Map<Class, InternalHandler> handlers = new ConcurrentHashMap<>();
    private final Listeners<Throwable> exceptionListeners = new Listeners<>();
    private final Listeners<Connection> closeListeners = new Listeners<>();

    CopycatTransportConnection(
            long connectionId,
            Mode mode,
            PartitionId partitionId,
            Endpoint endpoint,
            MessagingService messagingService,
            ThreadContext context) {
        this.connectionId = connectionId;
        this.partitionId = checkNotNull(partitionId, "partitionId cannot be null");
        this.localSubject = mode.getLocalSubject(partitionId, connectionId);
        this.remoteSubject = mode.getRemoteSubject(partitionId, connectionId);
        this.endpoint = checkNotNull(endpoint, "endpoint cannot be null");
        this.messagingService = checkNotNull(messagingService, "messagingService cannot be null");
        this.context = checkNotNull(context, "context cannot be null");
        messagingService.registerHandler(localSubject, this::handle);
    }

    @Override
    public CompletableFuture<Void> send(Object message) {
        ThreadContext context = ThreadContext.currentContextOrThrow();
        CompletableFuture<Void> future = new CompletableFuture<>();
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            DataOutputStream dos = new DataOutputStream(baos);
            dos.writeByte(MESSAGE);
            context.serializer().writeObject(message, baos);
            if (message instanceof ReferenceCounted) {
                ((ReferenceCounted<?>) message).release();
            }

            messagingService.sendAsync(endpoint, remoteSubject, baos.toByteArray())
                    .whenComplete((r, e) -> {
                        if (e != null) {
                            context.executor().execute(() -> future.completeExceptionally(e));
                        } else {
                            context.executor().execute(() -> future.complete(null));
                        }
                    });
        } catch (SerializationException | IOException e) {
            future.completeExceptionally(e);
        }
        return future;
    }

    @Override
    public <T, U> CompletableFuture<U> sendAndReceive(T message) {
        ThreadContext context = ThreadContext.currentContextOrThrow();
        CompletableFuture<U> future = new CompletableFuture<>();
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            DataOutputStream dos = new DataOutputStream(baos);
            dos.writeByte(MESSAGE);
            context.serializer().writeObject(message, baos);
            if (message instanceof ReferenceCounted) {
                ((ReferenceCounted<?>) message).release();
            }
            messagingService.sendAndReceive(endpoint,
                                            remoteSubject,
                                            baos.toByteArray(),
                                            context.executor())
                    .whenComplete((response, error) -> handleResponse(response, error, future));
        } catch (SerializationException | IOException e) {
            future.completeExceptionally(e);
        }
        return future;
    }

    /**
     * Handles a response received from the other side of the connection.
     */
    private <T> void handleResponse(
            byte[] response,
            Throwable error,
            CompletableFuture<T> future) {
        if (error != null) {
            Throwable rootCause = Throwables.getRootCause(error);
            if (rootCause instanceof MessagingException || rootCause instanceof SocketException) {
                future.completeExceptionally(new TransportException(error));
                if (rootCause instanceof MessagingException.NoRemoteHandler) {
                    close(rootCause);
                }
            } else {
                future.completeExceptionally(error);
            }
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

    /**
     * Handles a message sent to the connection.
     */
    private CompletableFuture<byte[]> handle(Endpoint sender, byte[] payload) {
        try (DataInputStream input = new DataInputStream(new ByteArrayInputStream(payload))) {
            byte type = input.readByte();
            switch (type) {
                case MESSAGE:
                    return handleMessage(IOUtils.toByteArray(input));
                case CLOSE:
                    return handleClose();
                default:
                    throw new IllegalStateException("Invalid message type");
            }
        } catch (IOException e) {
            Throwables.propagate(e);
            return null;
        }
    }

    /**
     * Handles a message from the other side of the connection.
     */
    @SuppressWarnings("unchecked")
    private CompletableFuture<byte[]> handleMessage(byte[] message) {
        try {
            Object request = context.serializer().readObject(new ByteArrayInputStream(message));
            InternalHandler handler = handlers.get(request.getClass());
            if (handler == null) {
                log.warn("No handler registered on connection {}-{} for type {}",
                         partitionId, connectionId, request.getClass());
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

    /**
     * Handles a close request from the other side of the connection.
     */
    private CompletableFuture<byte[]> handleClose() {
        CompletableFuture<byte[]> future = new CompletableFuture<>();
        context.executor().execute(() -> {
            close(null);
            ByteBuffer responseBuffer = ByteBuffer.allocate(1);
            responseBuffer.put(SUCCESS);
            future.complete(responseBuffer.array());
        });
        return future;
    }

    @Override
    public <T, U> Connection handler(Class<T> type, Consumer<T> handler) {
        return handler(type, r -> {
            handler.accept(r);
            return null;
        });
    }

    @Override
    public <T, U> Connection handler(Class<T> type, Function<T, CompletableFuture<U>> handler) {
        if (log.isTraceEnabled()) {
            log.trace("Registered handler on connection {}-{}: {}", partitionId, connectionId, type);
        }
        handlers.put(type, new InternalHandler(handler, ThreadContext.currentContextOrThrow()));
        return this;
    }

    @Override
    public Listener<Throwable> onException(Consumer<Throwable> consumer) {
        return exceptionListeners.add(consumer);
    }

    @Override
    public Listener<Connection> onClose(Consumer<Connection> consumer) {
        return closeListeners.add(consumer);
    }

    @Override
    public CompletableFuture<Void> close() {
        log.debug("Closing connection {}-{}", partitionId, connectionId);

        ByteBuffer requestBuffer = ByteBuffer.allocate(1);
        requestBuffer.put(CLOSE);

        ThreadContext context = ThreadContext.currentContextOrThrow();
        CompletableFuture<Void> future = new CompletableFuture<>();
        messagingService.sendAndReceive(endpoint, remoteSubject, requestBuffer.array(), context.executor())
                .whenComplete((payload, error) -> {
                    close(error);
                    Throwable wrappedError = error;
                    if (error != null) {
                        Throwable rootCause = Throwables.getRootCause(error);
                        if (MessagingException.class.isAssignableFrom(rootCause.getClass())) {
                            wrappedError = new TransportException(error);
                        }
                        future.completeExceptionally(wrappedError);
                    } else {
                        ByteBuffer responseBuffer = ByteBuffer.wrap(payload);
                        if (responseBuffer.get() == SUCCESS) {
                            future.complete(null);
                        } else {
                            future.completeExceptionally(new TransportException("Failed to close connection"));
                        }
                    }
                });
        return future;
    }

    /**
     * Cleans up the connection, unregistering handlers registered on the MessagingService.
     */
    private void close(Throwable error) {
        log.debug("Connection {}-{} closed", partitionId, connectionId);
        messagingService.unregisterHandler(localSubject);
        if (error != null) {
            exceptionListeners.accept(error);
        }
        closeListeners.accept(this);
    }

    /**
     * Connection mode used to indicate whether this side of the connection is
     * a client or server.
     */
    enum Mode {

        /**
         * Represents the client side of a bi-directional connection.
         */
        CLIENT {
            @Override
            String getLocalSubject(PartitionId partitionId, long connectionId) {
                return String.format("onos-copycat-%s-%d-client", partitionId, connectionId);
            }

            @Override
            String getRemoteSubject(PartitionId partitionId, long connectionId) {
                return String.format("onos-copycat-%s-%d-server", partitionId, connectionId);
            }
        },

        /**
         * Represents the server side of a bi-directional connection.
         */
        SERVER {
            @Override
            String getLocalSubject(PartitionId partitionId, long connectionId) {
                return String.format("onos-copycat-%s-%d-server", partitionId, connectionId);
            }

            @Override
            String getRemoteSubject(PartitionId partitionId, long connectionId) {
                return String.format("onos-copycat-%s-%d-client", partitionId, connectionId);
            }
        };

        /**
         * Returns the local messaging service subject for the connection in this mode.
         * Subjects generated by the connection mode are guaranteed to be globally unique.
         *
         * @param partitionId the partition ID to which the connection belongs.
         * @param connectionId the connection ID.
         * @return the globally unique local subject for the connection.
         */
        abstract String getLocalSubject(PartitionId partitionId, long connectionId);

        /**
         * Returns the remote messaging service subject for the connection in this mode.
         * Subjects generated by the connection mode are guaranteed to be globally unique.
         *
         * @param partitionId the partition ID to which the connection belongs.
         * @param connectionId the connection ID.
         * @return the globally unique remote subject for the connection.
         */
        abstract String getRemoteSubject(PartitionId partitionId, long connectionId);
    }

    /**
     * Internal container for a handler/context pair.
     */
    private static class InternalHandler {
        private final Function handler;
        private final ThreadContext context;

        InternalHandler(Function handler, ThreadContext context) {
            this.handler = handler;
            this.context = context;
        }

        @SuppressWarnings("unchecked")
        CompletableFuture<Object> handle(Object message) {
            CompletableFuture<Object> future = new CompletableFuture<>();
            context.executor().execute(() -> {
                CompletableFuture<Object> responseFuture = (CompletableFuture<Object>) handler.apply(message);
                if (responseFuture != null) {
                    responseFuture.whenComplete((r, e) -> {
                        if (e != null) {
                            future.completeExceptionally((Throwable) e);
                        } else {
                            future.complete(r);
                        }
                    });
                }
            });
            return future;
        }
    }
}

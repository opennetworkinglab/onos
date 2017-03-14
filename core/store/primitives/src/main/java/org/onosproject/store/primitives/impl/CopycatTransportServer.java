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

import com.google.common.collect.Maps;
import io.atomix.catalyst.concurrent.CatalystThreadFactory;
import io.atomix.catalyst.concurrent.SingleThreadContext;
import io.atomix.catalyst.concurrent.ThreadContext;
import io.atomix.catalyst.transport.Address;
import io.atomix.catalyst.transport.Connection;
import io.atomix.catalyst.transport.Server;
import org.apache.commons.io.IOUtils;
import org.onlab.util.Tools;
import org.onosproject.cluster.PartitionId;
import org.onosproject.store.cluster.messaging.Endpoint;
import org.onosproject.store.cluster.messaging.MessagingService;
import org.slf4j.Logger;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * {@link Server} implementation for {@link CopycatTransport}.
 */
public class CopycatTransportServer implements Server {

    private final Logger log = getLogger(getClass());
    private final AtomicBoolean listening = new AtomicBoolean(false);
    private CompletableFuture<Void> listenFuture = new CompletableFuture<>();
    private final ScheduledExecutorService executorService;
    private final PartitionId partitionId;
    private final MessagingService messagingService;
    private final String messageSubject;
    private final Map<Long, CopycatTransportConnection> connections = Maps.newConcurrentMap();

    CopycatTransportServer(PartitionId partitionId, MessagingService messagingService) {
        this.partitionId = checkNotNull(partitionId);
        this.messagingService = checkNotNull(messagingService);
        this.messageSubject = String.format("onos-copycat-%s", partitionId);
        this.executorService = Executors.newScheduledThreadPool(Math.min(4, Runtime.getRuntime().availableProcessors()),
                                                                new CatalystThreadFactory("copycat-server-p" + partitionId + "-%d"));
    }

    @Override
    public CompletableFuture<Void> listen(Address address, Consumer<Connection> listener) {
        if (listening.compareAndSet(false, true)) {
            ThreadContext context = ThreadContext.currentContextOrThrow();
            listen(listener, context);
        }
        return listenFuture;
    }

    /**
     * Starts the server listening via the {@code MessageService} on the partition subject.
     */
    private void listen(Consumer<Connection> listener, ThreadContext context) {
        messagingService.registerHandler(messageSubject, (sender, payload) -> {
            try (DataInputStream input = new DataInputStream(new ByteArrayInputStream(payload))) {
                byte type = input.readByte();
                long connectionId = input.readLong();
                switch (type) {
                    case CopycatTransportConnection.CONNECT:
                        return handleConnect(sender, connectionId, listener, context);
                    case CopycatTransportConnection.CLOSE:
                        return handleClose(connectionId);
                    case CopycatTransportConnection.MESSAGE:
                        return handleMessage(connectionId, IOUtils.toByteArray(input));
                    default:
                        throw new IllegalStateException("Invalid message type");
                }
            } catch (IOException e) {
                return Tools.exceptionalFuture(e);
            }
        });
        context.execute(() -> {
            listenFuture.complete(null);
        });
    }

    /**
     * Handles a connect message from a client.
     */
    private CompletableFuture<byte[]> handleConnect(
            Endpoint endpoint,
            long connectionId,
            Consumer<Connection> listener,
            ThreadContext context) {
        CopycatTransportConnection connection = connections.computeIfAbsent(connectionId, k -> {
            CopycatTransportConnection newConnection = new CopycatTransportConnection(connectionId,
                                                                                      CopycatTransport.Mode.SERVER,
                                                                                      partitionId,
                                                                                      CopycatTransport.toAddress(endpoint),
                                                                                      messagingService,
                                                                                      getOrCreateContext(context));
            log.debug("Created new incoming connection {}", connectionId);
            newConnection.closeListener(c -> connections.remove(connectionId, c));
            return newConnection;
        });

        CompletableFuture<byte[]> future = new CompletableFuture<>();
        context.executor().execute(() -> {
            listener.accept(connection);
            future.complete(CopycatTransportConnection.success());
        });
        return future;
    }

    /**
     * Handles a close message from a client.
     */
    private CompletableFuture<byte[]> handleClose(long connectionId) {
        CopycatTransportConnection connection = connections.remove(connectionId);
        if (connection != null) {
            log.debug("Closed connection {}", connectionId);
            connection.cleanup();
            return CompletableFuture.completedFuture(CopycatTransportConnection.success());
        }
        return Tools.exceptionalFuture(new IllegalStateException("Cannot close unknown connection " + connectionId));
    }

    /**
     * Handles a message from a client.
     */
    private CompletableFuture<byte[]> handleMessage(long connectionId, byte[] message) {
        CopycatTransportConnection connection = connections.get(connectionId);
        if (connection != null) {
            return connection.handle(message);
        }
        return Tools.exceptionalFuture(new IllegalStateException("Unknown connection " + connectionId));
    }

    @Override
    public CompletableFuture<Void> close() {
        messagingService.unregisterHandler(messageSubject);
        executorService.shutdown();
        return CompletableFuture.completedFuture(null);
    }

    /**
     * Returns the current execution context or creates one.
     */
    private ThreadContext getOrCreateContext(ThreadContext parentContext) {
        ThreadContext context = ThreadContext.currentContext();
        if (context != null) {
            return context;
        }
        return new SingleThreadContext(executorService, parentContext.serializer().clone());
    }
}
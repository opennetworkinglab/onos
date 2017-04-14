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

import com.google.common.base.Throwables;
import com.google.common.collect.Sets;
import io.atomix.catalyst.concurrent.ThreadContext;
import io.atomix.catalyst.transport.Address;
import io.atomix.catalyst.transport.Client;
import io.atomix.catalyst.transport.Connection;
import io.atomix.catalyst.transport.TransportException;
import org.onosproject.cluster.PartitionId;
import org.onosproject.store.cluster.messaging.Endpoint;
import org.onosproject.store.cluster.messaging.MessagingException;
import org.onosproject.store.cluster.messaging.MessagingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.ConnectException;
import java.nio.ByteBuffer;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import static com.google.common.base.MoreObjects.toStringHelper;
import static com.google.common.base.Preconditions.checkNotNull;
import static org.onosproject.store.primitives.impl.CopycatTransport.CONNECT;
import static org.onosproject.store.primitives.impl.CopycatTransport.SUCCESS;

/**
 * Copycat transport client implementation.
 */
public class CopycatTransportClient implements Client {
    private final Logger log = LoggerFactory.getLogger(getClass());
    private final PartitionId partitionId;
    private final String serverSubject;
    private final MessagingService messagingService;
    private final Set<CopycatTransportConnection> connections = Sets.newConcurrentHashSet();

    public CopycatTransportClient(PartitionId partitionId, MessagingService messagingService) {
        this.partitionId = checkNotNull(partitionId, "partitionId cannot be null");
        this.serverSubject = String.format("onos-copycat-%s", partitionId);
        this.messagingService = checkNotNull(messagingService, "messagingService cannot be null");
    }

    @Override
    public CompletableFuture<Connection> connect(Address address) {
        CompletableFuture<Connection> future = new CompletableFuture<>();
        ThreadContext context = ThreadContext.currentContextOrThrow();
        Endpoint endpoint = CopycatTransport.toEndpoint(address);

        log.debug("Connecting to {}", address);

        ByteBuffer requestBuffer = ByteBuffer.allocate(1);
        requestBuffer.put(CONNECT);

        // Send a connect request to the server to get a unique connection ID.
        messagingService.sendAndReceive(endpoint, serverSubject, requestBuffer.array(), context.executor())
                .whenComplete((payload, error) -> {
                    Throwable wrappedError = error;
                    if (error != null) {
                        Throwable rootCause = Throwables.getRootCause(error);
                        if (MessagingException.class.isAssignableFrom(rootCause.getClass())) {
                            wrappedError = new TransportException(error);
                        }
                        log.warn("Connection to {} failed! Reason: {}", address, wrappedError);
                        future.completeExceptionally(wrappedError);
                    } else {
                        // If the connection is successful, the server will send back a
                        // connection ID indicating where to send messages for the connection.
                        ByteBuffer responseBuffer = ByteBuffer.wrap(payload);
                        if (responseBuffer.get() == SUCCESS) {
                            long connectionId = responseBuffer.getLong();
                            CopycatTransportConnection connection = new CopycatTransportConnection(
                                    connectionId,
                                    CopycatTransportConnection.Mode.CLIENT,
                                    partitionId,
                                    endpoint,
                                    messagingService,
                                    context);
                            connection.onClose(connections::remove);
                            connections.add(connection);
                            future.complete(connection);
                            log.debug("Created connection {}-{} to {}", partitionId, connectionId, address);
                        } else {
                            log.warn("Connection to {} failed!");
                            future.completeExceptionally(new ConnectException());
                        }
                    }
                });
        return future;
    }

    @Override
    public CompletableFuture<Void> close() {
        return CompletableFuture.allOf(connections.stream().map(Connection::close).toArray(CompletableFuture[]::new));
    }

    @Override
    public String toString() {
        return toStringHelper(this)
                .add("partitionId", partitionId)
                .toString();
    }
}


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

import com.google.common.collect.Sets;
import io.atomix.catalyst.concurrent.ThreadContext;
import io.atomix.catalyst.transport.Address;
import io.atomix.catalyst.transport.Connection;
import io.atomix.catalyst.transport.Server;
import org.apache.commons.lang3.RandomUtils;
import org.onosproject.cluster.PartitionId;
import org.onosproject.store.cluster.messaging.MessagingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import static com.google.common.base.MoreObjects.toStringHelper;
import static com.google.common.base.Preconditions.checkNotNull;
import static org.onosproject.store.primitives.impl.CopycatTransport.CONNECT;
import static org.onosproject.store.primitives.impl.CopycatTransport.FAILURE;
import static org.onosproject.store.primitives.impl.CopycatTransport.SUCCESS;

/**
 * Copycat transport server implementation.
 */
public class CopycatTransportServer implements Server {
    private final Logger log = LoggerFactory.getLogger(getClass());
    private final PartitionId partitionId;
    private final String serverSubject;
    private final MessagingService messagingService;
    private final Set<CopycatTransportConnection> connections = Sets.newConcurrentHashSet();

    public CopycatTransportServer(PartitionId partitionId, MessagingService messagingService) {
        this.partitionId = checkNotNull(partitionId, "partitionId cannot be null");
        this.serverSubject = String.format("onos-copycat-%s", partitionId);
        this.messagingService = checkNotNull(messagingService, "messagingService cannot be null");
    }

    @Override
    public CompletableFuture<Void> listen(Address address, Consumer<Connection> consumer) {
        ThreadContext context = ThreadContext.currentContextOrThrow();
        messagingService.registerHandler(serverSubject, (sender, payload) -> {

            // Only connect messages can be sent to the server. Once a connect message
            // is received, the connection will register a separate handler for messaging.
            ByteBuffer requestBuffer = ByteBuffer.wrap(payload);
            if (requestBuffer.get() != CONNECT) {
                ByteBuffer responseBuffer = ByteBuffer.allocate(1);
                responseBuffer.put(FAILURE);
                return CompletableFuture.completedFuture(responseBuffer.array());
            }

            // Create the connection and ensure state is cleaned up when the connection is closed.
            long connectionId = RandomUtils.nextLong();
            CopycatTransportConnection connection = new CopycatTransportConnection(
                    connectionId,
                    CopycatTransportConnection.Mode.SERVER,
                    partitionId,
                    sender,
                    messagingService,
                    context);
            connection.onClose(connections::remove);
            connections.add(connection);

            CompletableFuture<byte[]> future = new CompletableFuture<>();

            // We need to ensure the connection event is called on the Copycat thread
            // and that the future is not completed until the Copycat server has been
            // able to register message handlers, otherwise some messages can be received
            // prior to any handlers being registered.
            context.executor().execute(() -> {
                log.debug("Created connection {}-{}", partitionId, connectionId);
                consumer.accept(connection);

                ByteBuffer responseBuffer = ByteBuffer.allocate(9);
                responseBuffer.put(SUCCESS);
                responseBuffer.putLong(connectionId);
                future.complete(responseBuffer.array());
            });
            return future;
        });
        return CompletableFuture.completedFuture(null);
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

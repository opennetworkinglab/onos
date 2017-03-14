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
import io.atomix.catalyst.transport.Client;
import io.atomix.catalyst.transport.Connection;
import org.apache.commons.lang.math.RandomUtils;
import org.onosproject.cluster.PartitionId;
import org.onosproject.store.cluster.messaging.MessagingService;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * {@link Client} implementation for {@link CopycatTransport}.
 */
public class CopycatTransportClient implements Client {

    private final PartitionId partitionId;
    private final MessagingService messagingService;
    private final Set<CopycatTransportConnection> connections = Sets.newConcurrentHashSet();

    CopycatTransportClient(PartitionId partitionId, MessagingService messagingService) {
        this.partitionId = checkNotNull(partitionId);
        this.messagingService = checkNotNull(messagingService);
    }

    @Override
    public CompletableFuture<Connection> connect(Address remoteAddress) {
        ThreadContext context = ThreadContext.currentContextOrThrow();
        CopycatTransportConnection connection = new CopycatTransportConnection(
                nextConnectionId(),
                CopycatTransport.Mode.CLIENT,
                partitionId,
                remoteAddress,
                messagingService,
                context);
        connection.closeListener(connections::remove);
        connections.add(connection);
        return connection.connect();
    }

    @Override
    public CompletableFuture<Void> close() {
        return CompletableFuture.allOf(connections.stream().map(Connection::close).toArray(CompletableFuture[]::new));
    }

    private long nextConnectionId() {
        return RandomUtils.nextLong();
    }
}
/*
 * Copyright 2016 Open Networking Laboratory
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
import static org.slf4j.LoggerFactory.getLogger;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

import org.apache.commons.lang.math.RandomUtils;
import org.onosproject.cluster.PartitionId;
import org.onosproject.store.cluster.messaging.MessagingService;
import org.slf4j.Logger;

import com.google.common.collect.Sets;
import com.google.common.primitives.Longs;

import io.atomix.catalyst.transport.Address;
import io.atomix.catalyst.transport.Client;
import io.atomix.catalyst.transport.Connection;
import io.atomix.catalyst.util.concurrent.ThreadContext;

/**
 * {@link Client} implementation for {@link CopycatTransport}.
 */
public class CopycatTransportClient implements Client {

    private final Logger log = getLogger(getClass());
    private final PartitionId partitionId;
    private final MessagingService messagingService;
    private final CopycatTransport.Mode mode;
    private final String newConnectionMessageSubject;
    private final Set<CopycatTransportConnection> connections = Sets.newConcurrentHashSet();

    CopycatTransportClient(PartitionId partitionId, MessagingService messagingService, CopycatTransport.Mode mode) {
        this.partitionId = checkNotNull(partitionId);
        this.messagingService = checkNotNull(messagingService);
        this.mode = checkNotNull(mode);
        this.newConnectionMessageSubject = String.format("onos-copycat-server-connection-%s", partitionId);
    }

    @Override
    public CompletableFuture<Connection> connect(Address remoteAddress) {
        ThreadContext context = ThreadContext.currentContextOrThrow();
        return messagingService.sendAndReceive(CopycatTransport.toEndpoint(remoteAddress),
                                               newConnectionMessageSubject,
                                               Longs.toByteArray(nextConnectionId()))
                .thenApplyAsync(bytes -> {
                    long connectionId = Longs.fromByteArray(bytes);
                    CopycatTransportConnection connection = new CopycatTransportConnection(
                            connectionId,
                            CopycatTransport.Mode.CLIENT,
                            partitionId,
                            remoteAddress,
                            messagingService,
                            context);
                    if (mode == CopycatTransport.Mode.CLIENT) {
                        connection.setBidirectional();
                    }
                    log.debug("Created new outgoing connection[id={}] to {}", connectionId, remoteAddress);
                    connections.add(connection);
                    return connection;
                }, context.executor());
    }

    @Override
    public CompletableFuture<Void> close() {
        return CompletableFuture.allOf(connections.stream().map(Connection::close).toArray(CompletableFuture[]::new));
    }

    private long nextConnectionId() {
        return RandomUtils.nextLong();
    }
 }

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
import com.google.common.collect.Maps;
import io.atomix.catalyst.transport.Address;
import io.atomix.catalyst.transport.Client;
import io.atomix.catalyst.transport.Server;
import io.atomix.catalyst.transport.Transport;
import org.onlab.packet.IpAddress;
import org.onosproject.cluster.PartitionId;
import org.onosproject.store.cluster.messaging.Endpoint;
import org.onosproject.store.cluster.messaging.MessagingService;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Map;

import static com.google.common.base.MoreObjects.toStringHelper;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Copycat transport implementation built on {@link MessagingService}.
 */
public class CopycatTransport implements Transport {
    private final PartitionId partitionId;
    private final MessagingService messagingService;
    private static final Map<Address, Endpoint> EP_LOOKUP_CACHE = Maps.newConcurrentMap();

    static final byte MESSAGE = 0x01;
    static final byte CONNECT = 0x02;
    static final byte CLOSE = 0x03;

    static final byte SUCCESS = 0x01;
    static final byte FAILURE = 0x02;

    public CopycatTransport(PartitionId partitionId, MessagingService messagingService) {
        this.partitionId = checkNotNull(partitionId, "partitionId cannot be null");
        this.messagingService = checkNotNull(messagingService, "messagingService cannot be null");
    }

    @Override
    public Client client() {
        return new CopycatTransportClient(partitionId, messagingService);
    }

    @Override
    public Server server() {
        return new CopycatTransportServer(partitionId, messagingService);
    }

    @Override
    public String toString() {
        return toStringHelper(this).toString();
    }

    /**
     * Maps {@link Address address} to {@link Endpoint endpoint}.
     * @param address address
     * @return end point
     */
    static Endpoint toEndpoint(Address address) {
        return EP_LOOKUP_CACHE.computeIfAbsent(address, a -> {
            try {
                return new Endpoint(IpAddress.valueOf(InetAddress.getByName(a.host())), a.port());
            } catch (UnknownHostException e) {
                Throwables.propagate(e);
                return null;
            }
        });
    }
}

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

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.Map;

import org.onlab.packet.IpAddress;
import org.onosproject.cluster.PartitionId;
import org.onosproject.store.cluster.messaging.Endpoint;
import org.onosproject.store.cluster.messaging.MessagingService;

import com.google.common.base.Throwables;
import com.google.common.collect.Maps;

import io.atomix.catalyst.transport.Address;
import io.atomix.catalyst.transport.Client;
import io.atomix.catalyst.transport.Server;
import io.atomix.catalyst.transport.Transport;

/**
 * Custom {@link Transport transport} for Copycat interactions
 * built on top of {@link MessagingService}.
 *
 * @see CopycatTransportServer
 * @see CopycatTransportClient
 */
public class CopycatTransport implements Transport {

    /**
     * Transport Mode.
     */
    public enum Mode {
        /**
         * Signifies transport for client {@literal ->} server interaction.
         */
        CLIENT,

        /**
         * Signified transport for server {@literal ->} server interaction.
         */
        SERVER
    }

    private final Mode mode;
    private final PartitionId partitionId;
    private final MessagingService messagingService;
    private static final Map<Address, Endpoint> EP_LOOKUP_CACHE = Maps.newConcurrentMap();
    private static final Map<Endpoint, Address> ADDRESS_LOOKUP_CACHE = Maps.newConcurrentMap();

    public CopycatTransport(Mode mode, PartitionId partitionId, MessagingService messagingService) {
        this.mode = checkNotNull(mode);
        this.partitionId = checkNotNull(partitionId);
        this.messagingService = checkNotNull(messagingService);
    }

    @Override
    public Client client() {
        return new CopycatTransportClient(partitionId,
                                          messagingService,
                                          mode);
    }

    @Override
    public Server server() {
        return new CopycatTransportServer(partitionId,
                                          messagingService);
    }

    /**
     * Maps {@link Address address} to {@link Endpoint endpoint}.
     * @param address address
     * @return end point
     */
    public static Endpoint toEndpoint(Address address) {
        return EP_LOOKUP_CACHE.computeIfAbsent(address, a -> {
            try {
                return new Endpoint(IpAddress.valueOf(InetAddress.getByName(a.host())), a.port());
            } catch (UnknownHostException e) {
                Throwables.propagate(e);
                return null;
            }
        });
    }

    /**
     * Maps {@link Endpoint endpoint} to {@link Address address}.
     * @param endpoint end point
     * @return address
     */
    public static Address toAddress(Endpoint endpoint) {
        return ADDRESS_LOOKUP_CACHE.computeIfAbsent(endpoint, ep -> {
            try {
                InetAddress host = InetAddress.getByAddress(endpoint.host().toOctets());
                int port = endpoint.port();
                return new Address(new InetSocketAddress(host, port));
            } catch (UnknownHostException e) {
                Throwables.propagate(e);
                return null;
            }
        });
    }
}

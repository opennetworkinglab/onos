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

import org.onosproject.store.cluster.messaging.MessagingService;

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
    private final String clusterName;
    private final MessagingService messagingService;

    public CopycatTransport(Mode mode, String clusterName, MessagingService messagingService) {
        this.mode = checkNotNull(mode);
        this.clusterName = checkNotNull(clusterName);
        this.messagingService = checkNotNull(messagingService);
    }

    @Override
    public Client client() {
        return new CopycatTransportClient(clusterName,
                                          messagingService,
                                          mode);
    }

    @Override
    public Server server() {
        return new CopycatTransportServer(clusterName,
                                          messagingService);
    }
}

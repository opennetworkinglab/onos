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
package org.onosproject.tetopology.management.api;

import org.onosproject.event.ListenerService;

/**
 * TE Topology Service API.
 */
public interface TeTopologyService
    extends ListenerService<TeTopologyEvent, TeTopologyListener> {

    /**
     * Returns a collection of currently known networks.
     *
     * @return a collection of networks
     */
    Networks getNetworks();

    /**
     * Returns the network identified by its network id.
     *
     * @param  networkId network id in URI format
     * @return value of network
     */
    Network getNetwork(KeyId networkId);

    /**
     * Updates the network.
     *
     * @param network network to be updated
     */
    void updateNetwork(Network network);

    /**
     * Removes the network.
     *
     * @param  networkId network id in URI format
     */
    void removeNetwork(KeyId networkId);
}

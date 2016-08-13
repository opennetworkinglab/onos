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

import java.util.List;

import org.onosproject.store.Store;

/**
 * Inventory of TE network topology.
 */
public interface TeTopologyStore
        extends Store<TeTopologyEvent, TeTopologyStoreDelegate> {

    /**
     * Returns a collection of currently known networks.
     *
     * @param  type TE topology type
     * @return a collection of stored internal TE networks
     */
    List<InternalTeNetwork> getNetworks(TeTopologyType type);

    /**
     * Returns the network.
     *
     * @param  networkId network id in URI format
     * @return value of internal TE network
     */
    InternalTeNetwork getNetwork(KeyId networkId);

    /**
     * Updates the network.
     *
     * @param network value of the network to be updated
     */
    void updateNetwork(InternalTeNetwork network);

    /**
     * Removes the network.
     *
     * @param  networkId network id in URI format
     */
    void removeNetwork(KeyId networkId);

}

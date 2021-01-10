/*
 * Copyright 2021-present Open Networking Foundation
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
package org.onosproject.kubevirtnetworking.api;

import org.onosproject.store.Store;

import java.util.Set;

/**
 * Manages inventory of kubevirt network states; not intended for direct use.
 */
public interface KubevirtNetworkStore
        extends Store<KubevirtNetworkEvent, KubevirtNetworkStoreDelegate> {

    /**
     * Creates the new kubevirt network.
     *
     * @param network kubevirt network
     */
    void createNetwork(KubevirtNetwork network);

    /**
     * Update the kubevirt network.
     *
     * @param network kubevirt network
     */
    void updateNetwork(KubevirtNetwork network);

    /**
     * Removes the kubevirt network with the given network identifier.
     *
     * @param networkId network identifier
     * @return removed kubevirt network; null if failed
     */
    KubevirtNetwork removeNetwork(String networkId);

    /**
     * Returns the kubevirt network with the given network identifier.
     *
     * @param networkId network identifier
     * @return network; null if not found
     */
    KubevirtNetwork network(String networkId);

    /**
     * Returns all kubevirt networks.
     *
     * @return set of kubevirt networks
     */
    Set<KubevirtNetwork> networks();

    /**
     * Removes all kubevirt networks.
     */
    void clear();
}

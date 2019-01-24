/*
 * Copyright 2019-present Open Networking Foundation
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
package org.onosproject.k8snetworking.api;

import org.onosproject.store.Store;

import java.util.Set;

/**
 * Manages inventory of kubernetes network states; not intended for direct use.
 */
public interface K8sNetworkStore extends Store<K8sNetworkEvent, K8sNetworkStoreDelegate> {

    /**
     * Creates the new kubernetes network.
     *
     * @param network kubernetes network
     */
    void createNetwork(K8sNetwork network);

    /**
     * Update the kubernetes network.
     *
     * @param network kubernetes network
     */
    void updateNetwork(K8sNetwork network);

    /**
     * Removes the kubernetes network with the given network identifier.
     *
     * @param networkId network identifier
     * @return removed kubernetes network; null if failed
     */
    K8sNetwork removeNetwork(String networkId);

    /**
     * Returns the kubernetes network with the given network identifier.
     *
     * @param networkId network identifier
     * @return network; null if not found
     */
    K8sNetwork network(String networkId);

    /**
     * Returns all kubernetes networks.
     *
     * @return set of kubernetes networks
     */
    Set<K8sNetwork> networks();

    /**
     * Creates a new kubernetes port.
     *
     * @param port kubernetes port
     */
    void createPort(K8sPort port);

    /**
     * Update the kubernetes port.
     *
     * @param port kubernetes port
     */
    void updatePort(K8sPort port);

    /**
     * Removes the kubernetes port with the given port identifier.
     *
     * @param portId port identifier
     * @return port; null if not found
     */
    K8sPort removePort(String portId);

    /**
     * Returns all kubernetes ports.
     *
     * @return set of kubernetes ports
     */
    Set<K8sPort> ports();

    /**
     * Return the kubernetes port with the given port identifier.
     *
     * @param portId port identifier
     * @return kubernetes port
     */
    K8sPort port(String portId);

    /**
     * Removes all kubernetes networks and ports.
     */
    void clear();
}

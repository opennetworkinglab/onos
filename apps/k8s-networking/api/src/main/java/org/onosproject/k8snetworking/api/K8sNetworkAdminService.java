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

/**
 * Service for administering the inventory of kubernetes network, subnet.
 */
public interface K8sNetworkAdminService extends K8sNetworkService {

    /**
     * Creates a kubernetes network with the given information.
     *
     * @param network the new network
     */
    void createNetwork(K8sNetwork network);

    /**
     * Updates the kubernetes network with the given information.
     *
     * @param network the updated network
     */
    void updateNetwork(K8sNetwork network);

    /**
     * Removes the network.
     *
     * @param networkId network identifier
     */
    void removeNetwork(String networkId);

    /**
     * Creates a kubernetes port with the given information.
     *
     * @param port the new port
     */
    void createPort(K8sPort port);

    /**
     * Updates the kubernetes port with the given information.
     *
     * @param port the updated port
     */
    void updatePort(K8sPort port);

    /**
     * Removes the port.
     *
     * @param portId port identifier
     */
    void removePort(String portId);

    /**
     * Clears the existing network and port states.
     */
    void clear();
}

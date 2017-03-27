/*
 * Copyright 2017-present Open Networking Laboratory
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
package org.onosproject.openstacknetworking.api;

import org.openstack4j.model.network.Network;
import org.openstack4j.model.network.Port;
import org.openstack4j.model.network.Subnet;

/**
 * Service for administering the inventory of OpenStack network, subnet and port.
 */
public interface OpenstackNetworkAdminService {

    /**
     * Creates a network with the given information.
     *
     * @param network the new network
     */
    void createNetwork(Network network);

    /**
     * Updates the network with the given information.
     *
     * @param network the updated network
     */
    void updateNetwork(Network network);

    /**
     * Removes the network with the given network id.
     *
     * @param networkId network id
     */
    void removeNetwork(String networkId);

    /**
     * Creates a subnet with the given information.
     *
     * @param subnet the new subnet
     */
    void createSubnet(Subnet subnet);

    /**
     * Updates a subnet with the given information.
     *
     * @param subnet the updated subnet
     */
    void updateSubnet(Subnet subnet);

    /**
     * Removes the subnet with the given subnet id.
     *
     * @param subnetId subnet id
     */
    void removeSubnet(String subnetId);

    /**
     * Creates a port with the given information.
     *
     * @param port the new port
     */
    void createPort(Port port);

    /**
     * Updates the port with the given information.
     *
     * @param port the updated port
     */
    void updatePort(Port port);

    /**
     * Removes the port with the given port id.
     *
     * @param portId port id
     */
    void removePort(String portId);

    /**
     * Clears the existing network, subnet and port states.
     */
    void clear();
}

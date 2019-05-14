/*
 * Copyright 2017-present Open Networking Foundation
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

import org.onosproject.store.Store;
import org.openstack4j.model.network.Network;
import org.openstack4j.model.network.Port;
import org.openstack4j.model.network.Subnet;

import java.util.Set;

/**
 * Manages inventory of OpenStack network states; not intended for direct use.
 */
public interface OpenstackNetworkStore
        extends Store<OpenstackNetworkEvent, OpenstackNetworkStoreDelegate> {

    /**
     * Creates the new network.
     *
     * @param network openstack network
     */
    void createNetwork(Network network);

    /**
     * Updates the network.
     *
     * @param network openstack network
     */
    void updateNetwork(Network network);

    /**
     * Removes the network with the given network id.
     *
     * @param networkId network id
     * @return removed openstack network; null if failed
     */
    Network removeNetwork(String networkId);

    /**
     * Returns the network with the given network id.
     *
     * @param networkId network id
     * @return network; null if not found
     */
    Network network(String networkId);

    /**
     * Returns all networks.
     *
     * @return set of networks
     */
    Set<Network> networks();

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
     * @return removed subnet; null if failed
     */
    Subnet removeSubnet(String subnetId);

    /**
     * Returns the subnet with the supplied subnet ID.
     *
     * @param subnetId subnet id
     * @return subnet
     */
    Subnet subnet(String subnetId);

    /**
     * Returns all subnets registered in the service.
     *
     * @return set of subnet
     */
    Set<Subnet> subnets();

    /**
     * Creates the new port.
     *
     * @param port the new port
     */
    void createPort(Port port);

    /**
     * Updates the port.
     *
     * @param port port
     */
    void updatePort(Port port);

    /**
     * Removes the port.
     *
     * @param portId port id
     * @return removed port; null if failed
     */
    Port removePort(String portId);

    /**
     * Returns the port with the given port id.
     *
     * @param portId port id
     * @return port
     */
    Port port(String portId);

    /**
     * Returns all ports.
     *
     * @return set of ports
     */
    Set<Port> ports();

    /**
     * Returns external peer router with the given IP address.
     *
     * @param ipAddress IP address
     * @return external peer router
     */
    ExternalPeerRouter externalPeerRouter(String ipAddress);

    /**
     * Returns all external peer routers.
     *
     * @return set of external peer routers
     */
    Set<ExternalPeerRouter> externalPeerRouters();

    /**
     * Creates a new external peer router.
     *
     * @param peerRouter the new external peer router
     */
    void createExternalPeerRouter(ExternalPeerRouter peerRouter);

    /**
     * Updates an existing external peer router.
     *
     * @param peerRouter the updated external peer router
     */
    void updateExternalPeerRouter(ExternalPeerRouter peerRouter);

    /**
     * Removes an existing external peer router with the given IP address.
     *
     * @param ipAddress IP address
     * @return removed external peer router
     */
    ExternalPeerRouter removeExternalPeerRouter(String ipAddress);

    /**
     * Removes the existing network and ports.
     */
    void clear();
}

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
package org.onosproject.openstackinterface;

import org.onlab.packet.Ip4Address;
import org.onosproject.net.Port;

import java.util.Collection;
import java.util.Optional;

/**
 * Handles port management REST API from Openstack for VMs.
 */
public interface OpenstackInterfaceService {

    /**
     * Returns port information list for the network ID given.
     *
     * @param networkId Network ID of the ports
     * @return port information list
     */
    Collection<OpenstackPort> ports(String networkId);

    /**
     * Returns port information list.
     *
     * @return port information list
     */
    Collection<OpenstackPort> ports();
    /**
     * Returns port information for the port given.
     *
     * @param port port reference
     * @return port information
     */
    OpenstackPort port(Port port);

    /**
     * Returns port information for the port ID given.
     *
     * @param portId port id
     * @return port information
     */
    OpenstackPort port(String portId);

    /**
     * Returns network information list for the network ID given.
     *
     * @param networkId network id
     * @return network information, or null if not present
     */
    OpenstackNetwork network(String networkId);

    /**
     * Returns the information of all openstack networks.
     *
     * @return collection of network information
     */
    Collection<OpenstackNetwork> networks();

    /**
     * Returns subnet information for the subnet ID give.
     *
     * @param subnetId subnet id
     * @return subnet information, or null if not present
     */
    OpenstackSubnet subnet(String subnetId);

    /**
     * Returns collection of openstack subnet information.
     *
     * @return collection of openststack subnet information
     */
    Collection<OpenstackSubnet> subnets();

    /**
     * Returns the router information list.
     *
     * @return router information list
     */
    Collection<OpenstackRouter> routers();

    /**
     * Returns the router information for the router ID given.
     *
     * @param routerId router id
     * @return router information
     */
    OpenstackRouter router(String routerId);

    /**
     * Returns Security Group information of the security groupd id given.
     *
     * @param id security group id
     * @return security group information
     */
    OpenstackSecurityGroup securityGroup(String id);

    /**
     * Returns collection of OpenStack floating IP information.
     *
     * @return collection of OpenStack floating IP information
     */
    Collection<OpenstackFloatingIP> floatingIps();

    /**
     * Updates a floating IP and its association with an internal port.
     *
     * @param id floating ip id
     * @param portId port id
     * @param fixedIpAddress fixed ip address of the port
     * @return true if the update succeed
     */
    boolean updateFloatingIp(String id, String portId, Optional<Ip4Address> fixedIpAddress);



}

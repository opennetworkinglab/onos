/*
 * Copyright 2015 Open Networking Laboratory
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
package org.onosproject.openstackswitching;

import org.onosproject.net.Port;

import java.util.Collection;

/**
 * Handles port management REST API from Openstack for VMs.
 */
public interface OpenstackSwitchingService {

    /**
     * Store the port information created by Openstack.
     *
     * @param openstackPort port information
     */
    void createPorts(OpenstackPort openstackPort);

    /**
     * Removes flow rules corresponding to the port removed by Openstack.
     *
     */
    void deletePort(String uuid);

    /**
     * Updates flow rules corresponding to the port information updated by Openstack.
     *
     * @param openstackPort
     */
    void updatePort(OpenstackPort openstackPort);

    /**
     * Stores the network information created by openstack.
     *
     * @param openstackNetwork network information
     */
    void createNetwork(OpenstackNetwork openstackNetwork);

    /**
     * Stores the subnet information created by openstack.
     *
     * @param openstackSubnet subnet information
     */
    void createSubnet(OpenstackSubnet openstackSubnet);

    /**
     * Returns port information list for the network ID given.
     *
     * @param networkId Network ID of the ports
     * @return port information list
     */
    Collection<OpenstackPort> ports(String networkId);

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
     * @param portId Port ID
     * @return port information
     */
    OpenstackPort port(String portId);

    /**
     * Returns network information list for the network ID given.
     *
     * @param networkId Network ID
     * @return network information list, or null if not present
     */
    OpenstackNetwork network(String networkId);


    /**
     * Returns subnet information for the subnet ID give.
     *
     * @param subnetId Subnet ID
     * @return subnet information, or null if not present
     */
    OpenstackSubnet subnet(String subnetId);
}

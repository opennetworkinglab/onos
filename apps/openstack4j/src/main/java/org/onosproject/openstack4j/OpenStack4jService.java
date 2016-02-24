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
package org.onosproject.openstack4j;

import org.openstack4j.api.OSClient;
import org.openstack4j.model.network.Network;
import org.openstack4j.model.network.Port;
import org.openstack4j.model.network.Subnet;
import org.openstack4j.model.network.options.PortListOptions;

import java.util.List;

/**
 * Wrapper service of openstack4j.
 */
public interface OpenStack4jService {

    /**
     * Returns OpenStack REST client.
     *
     * @param endpoint endpoint URL
     * @param tenant tenant name
     * @param user user name
     * @param password password
     * @return openstack rest client or null if auth failed
     */
    OSClient getClient(String endpoint, String tenant, String user, String password);

    /**
     * Returns list of networks with a client information.
     *
     * @param client openstack rest client
     * @return list of networks or empty list if no networks
     */
    List<Network> getNetworks(OSClient client);

    /**
     * Returns network information.
     *
     * @param client openstack rest client
     * @param networkId network id
     * @return network or null if not found
     */
    Network getNetwork(OSClient client, String networkId);

    /**
     * Returns all ports.
     *
     * @param client openstack rest client
     * @return list of port or empty list if no ports
     */
    List<Port> getPorts(OSClient client);

    /**
     * Returns ports with a given options.
     *
     * @param client openstack rest client
     * @param options port list options
     * @return port list or empty list if no ports
     */
    List<Port> getPorts(OSClient client, PortListOptions options);

    /**
     * Returns port with a given port ID.
     *
     * @param client openstack rest client
     * @param portId port id
     * @return port or null if not found
     */
    Port getPort(OSClient client, String portId);

    /**
     * Returns all subnets.
     *
     * @param client openstack rest client
     * @return subnet list or empty list if no subnets
     */
    List<Subnet> getSubnets(OSClient client);

    /**
     * Returns subnet with a given subnet ID.
     *
     * @param client openstack rest client
     * @param subnetId subnet id
     * @return subnet or null if not found
     */
    Subnet getSubnet(OSClient client, String subnetId);
}

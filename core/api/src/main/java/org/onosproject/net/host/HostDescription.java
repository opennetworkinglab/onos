/*
 * Copyright 2014-present Open Networking Laboratory
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
package org.onosproject.net.host;

import java.util.Set;

import org.onosproject.net.Description;
import org.onosproject.net.HostLocation;
import org.onlab.packet.IpAddress;
import org.onlab.packet.MacAddress;
import org.onlab.packet.VlanId;

/**
 * Information describing host and its location.
 */
public interface HostDescription extends Description {

    /**
     * Returns the MAC address associated with this host (NIC).
     *
     * @return the MAC address of this host
     */
    MacAddress hwAddress();

    /**
     * Returns the VLAN associated with this host.
     *
     * @return the VLAN ID value
     */
    VlanId vlan();

    /**
     * Returns the most recent location of the host on the network edge.
     *
     * @return the most recent host location
     */
    HostLocation location();

    /**
     * Returns all locations of the host on the network edge.
     *
     * @return all host locations
     */
    Set<HostLocation> locations();

    /**
     * Returns the IP address associated with this host's MAC.
     *
     * @return host IP address
     */
    Set<IpAddress> ipAddress();

    /**
     * Returns true if configured by NetworkConfiguration.
     * @return configured/learnt dynamically
     */
    default boolean configured() {
        return false;
    }
}

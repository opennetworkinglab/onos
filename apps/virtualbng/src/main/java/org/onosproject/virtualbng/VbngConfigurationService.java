/*
 * Copyright 2015-present Open Networking Foundation
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
package org.onosproject.virtualbng;

import org.onlab.packet.IpAddress;
import org.onlab.packet.MacAddress;
import org.onosproject.net.ConnectPoint;

import java.util.Map;

/**
 * Provides information about the virtual BNG configuration.
 */
public interface VbngConfigurationService {

    /**
     * Gets the IP address configured for the next hop.
     *
     * @return the IP address of next hop
     */
    IpAddress getNextHopIpAddress();

    /**
     * Gets the MAC address configured for all the public IP addresses.
     *
     * @return the MAC address
     */
    MacAddress getPublicFacingMac();

    /**
     * Gets the IP address configured for XOS server.
     *
     * @return the IP address configured for the XOS server
     */
    IpAddress getXosIpAddress();

    /**
     * Gets the REST communication port configured for XOS server.
     *
     * @return the REST communication port configured for XOS server
     */
    int getXosRestPort();

    /**
     * Gets the host to port map.
     *
     * @return host to port map
     */
    Map<String, ConnectPoint> getNodeToPort();

    /**
     * Evaluates whether an IP address is an assigned public IP address.
     *
     * @param ipAddress the IP address to evaluate
     * @return true if the input IP address is an assigned public IP address,
     *         otherwise false
     */
    boolean isAssignedPublicIpAddress(IpAddress ipAddress);

    /**
     * Gets an available public IP address from local public IP prefixes.
     *
     * @param privateIpAddress a private IP address
     * @return an available public IP address if it exists, otherwise null
     */
    IpAddress getAvailablePublicIpAddress(IpAddress privateIpAddress);

    /**
     * Gets the public IP address already assigned for a private IP address.
     *
     * @param privateIpAddress a private IP address
     * @return the assigned public IP address if it exists, otherwise null
     */
    IpAddress getAssignedPublicIpAddress(IpAddress privateIpAddress);

    /**
     * Recycles the public IP address assigned for a private IP address, and
     * at the same time deletes the mapping entry from this private IP address
     * to the public IP address.
     *
     * @param privateIpAddress a private IP address
     * @return the assigned public IP address if it exists, otherwise null
     */
    IpAddress recycleAssignedPublicIpAddress(IpAddress privateIpAddress);

    /**
     * Gets all the mapping entries from private IP address to public IP
     * address.
     *
     * @return the address map from private IP address to public IP address
     */
    Map<IpAddress, IpAddress> getIpAddressMappings();

    /**
     * Tries to assign a given public IP address to a private IP address. If
     * success, then sets up the mapping from this private IP address to the
     * public IP address, and stores the mapping.
     *
     * @param publicIpAddress the public IP address try to assign
     * @param privateIpAddress a private IP address
     * @return true if this public IP address is available, otherwise false
     */
    boolean assignSpecifiedPublicIp(IpAddress publicIpAddress,
                                    IpAddress privateIpAddress);
}

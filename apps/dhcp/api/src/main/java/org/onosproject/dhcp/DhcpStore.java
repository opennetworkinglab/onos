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
package org.onosproject.dhcp;

import org.onlab.packet.Ip4Address;
import org.onlab.packet.MacAddress;
import org.onosproject.net.HostId;

import java.util.Map;


/**
 * DHCPStore Interface.
 */
public interface DhcpStore {

    /**
     * Appends all the IPs in a given range to the free pool of IPs.
     *
     * @param startIP Start IP for the range
     * @param endIP End IP for the range
     */
    void populateIPPoolfromRange(Ip4Address startIP, Ip4Address endIP);

    /**
     * Returns an IP Address for a Mac ID, in response to a DHCP DISCOVER message.
     *
     * @param hostId Host ID of the client requesting an IP
     * @param requestedIP requested IP address
     * @return IP address assigned to the Mac address; null if no available IP
     */
    Ip4Address suggestIP(HostId hostId, Ip4Address requestedIP);


    /**
     * Assigns the requested IP to the Mac ID, in response to a DHCP REQUEST message.
     *
     * @param hostId Host Id of the client requesting an IP
     * @param ipAssignment ip assignment
     * @return returns true if the assignment was successful, false otherwise
     */
    boolean assignIP(HostId hostId, IpAssignment ipAssignment);


    /**
     * Sets the default time for which suggested IP mappings are valid.
     *
     * @param timeInSeconds default time for IP mappings to be valid
     */
    void setDefaultTimeoutForPurge(int timeInSeconds);

    /**
     * Releases the IP assigned to a Mac ID into the free pool.
     *
     * @param hostId the host ID for which the mapping needs to be changed
     * @return released ip
     */
    Ip4Address releaseIP(HostId hostId);

    /**
     * Returns a collection of all the MacAddress to IPAddress mapping assigned to the hosts.
     *
     * @return the collection of the mappings
     */
    Map<HostId, IpAssignment> listAssignedMapping();

    /**
     * Returns a collection of all the MacAddress to IPAddress mapping.
     *
     * @return the collection of the mappings
     */
    Map<HostId, IpAssignment> listAllMapping();

    /**
     * Assigns the requested IP to the MAC ID (if available) for an indefinite period of time.
     *
     * @param macAddress mac address of the client
     * @param ipAssignment ip address and dhcp options requested for the client
     * @return true if the mapping was successfully registered, false otherwise
     */
    boolean assignStaticIP(MacAddress macAddress, IpAssignment ipAssignment);

    /**
     * Removes a static IP mapping associated with the given MAC ID from the DHCP Server.
     *
     * @param macAddress mac address of the client
     * @return true if the mapping was successfully registered, false otherwise
     */
    boolean removeStaticIP(MacAddress macAddress);

    /**
     * Returns the list of all the available IPs with the server.
     *
     * @return list of available IPs
     */
    Iterable<Ip4Address> getAvailableIPs();

    /**
     * Returns IpAssignment from map.
     *
     * @param hostId host identification
     * @return IpAssignment
     */
    IpAssignment getIpAssignmentFromAllocationMap(HostId hostId);
}

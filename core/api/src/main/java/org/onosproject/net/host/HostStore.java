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

import org.onlab.packet.IpAddress;
import org.onlab.packet.MacAddress;
import org.onlab.packet.VlanId;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Host;
import org.onosproject.net.HostId;
import org.onosproject.net.HostLocation;
import org.onosproject.net.provider.ProviderId;
import org.onosproject.store.Store;

import java.util.Set;

/**
 * Manages inventory of end-station hosts; not intended for direct use.
 */
public interface HostStore extends Store<HostEvent, HostStoreDelegate> {

    /**
     * Creates a new host or updates the existing one based on the specified
     * description.
     *
     * @param providerId      provider identification
     * @param hostId          host identification
     * @param hostDescription host description data
     * @param replaceIps      replace IP set if true, merge IP set otherwise
     * @return appropriate event or null if no change resulted
     */
    HostEvent createOrUpdateHost(ProviderId providerId, HostId hostId,
                                 HostDescription hostDescription,
                                 boolean replaceIps);

    /**
     * Removes the specified host from the inventory.
     *
     * @param hostId host identification
     * @return remove event or null if host was not found
     */
    HostEvent removeHost(HostId hostId);

    /**
     * Removes the specified ip from the host entry.
     *
     * @param hostId host identification
     * @param ipAddress ipAddress to be removed
     * @return remove event or null if host was not found
     */
    HostEvent removeIp(HostId hostId, IpAddress ipAddress);

    /**
     * Removes the specified location from the host entry.
     *
     * @param hostId host identification
     * @param location location to be removed
     */
    void removeLocation(HostId hostId, HostLocation location);

    /**
     * Returns the number of hosts in the store.
     *
     * @return host count
     */
    int getHostCount();

    /**
     * Returns a collection of all hosts in the store.
     *
     * @return iterable collection of all hosts
     */
    Iterable<Host> getHosts();

    /**
     * Returns the host with the specified identifer.
     *
     * @param hostId host identification
     * @return host or null if not found
     */
    Host getHost(HostId hostId);

    /**
     * Returns the set of all hosts within the specified VLAN.
     *
     * @param vlanId vlan id
     * @return set of hosts in the vlan
     */
    Set<Host> getHosts(VlanId vlanId);

    /**
     * Returns the set of hosts with the specified MAC address.
     *
     * @param mac mac address
     * @return set of hosts with the given mac
     */
    Set<Host> getHosts(MacAddress mac);

    /**
     * Returns the set of hosts with the specified IP address.
     *
     * @param ip ip address
     * @return set of hosts with the given IP
     */
    Set<Host> getHosts(IpAddress ip);

    /**
     * Returns the set of hosts whose location falls on the given connection point.
     *
     * @param connectPoint connection point
     * @return set of hosts
     */
    Set<Host> getConnectedHosts(ConnectPoint connectPoint);

    /**
     * Returns the set of hosts whose location falls on the given device.
     *
     * @param deviceId infrastructure device identifier
     * @return set of hosts
     */
    Set<Host> getConnectedHosts(DeviceId deviceId);

}

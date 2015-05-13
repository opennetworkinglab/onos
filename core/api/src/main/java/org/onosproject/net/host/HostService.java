/*
 * Copyright 2014 Open Networking Laboratory
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

import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Host;
import org.onosproject.net.HostId;
import org.onlab.packet.IpAddress;
import org.onlab.packet.MacAddress;
import org.onlab.packet.VlanId;

/**
 * Service for interacting with the inventory of end-station hosts.
 */
public interface HostService {

    /**
     * Returns the number of end-station hosts known to the system.
     *
     * @return number of end-station hosts
     */
    int getHostCount();

    /**
     * Returns a collection of all end-station hosts.
     *
     * @return collection of hosts
     */
    Iterable<Host> getHosts();

    /**
     * Returns the host with the specified identifier.
     *
     * @param hostId host identifier
     * @return host or null if one with the given identifier is not known
     */
    Host getHost(HostId hostId);

    /**
     * Returns the set of hosts that belong to the specified VLAN.
     *
     * @param vlanId vlan identifier
     * @return set of hosts in the given vlan id
     */
    Set<Host> getHostsByVlan(VlanId vlanId);

    /**
     * Returns the set of hosts that have the specified MAC address.
     *
     * @param mac mac address
     * @return set of hosts with the given mac
     */
    Set<Host> getHostsByMac(MacAddress mac);

    /**
     * Returns the set of hosts that have the specified IP address.
     *
     * @param ip ip address
     * @return set of hosts with the given IP
     */
    Set<Host> getHostsByIp(IpAddress ip);

    // TODO: consider adding Host getHostByIp(IpAddress ip, VlanId vlan);

    /**
     * Returns the set of hosts whose most recent location is the specified
     * connection point.
     *
     * @param connectPoint connection point
     * @return set of hosts connected to the connection point
     */
    Set<Host> getConnectedHosts(ConnectPoint connectPoint);

    /**
     * Returns the set of hosts whose most recent location is the specified
     * infrastructure device.
     *
     * @param deviceId device identifier
     * @return set of hosts connected to the device
     */
    Set<Host> getConnectedHosts(DeviceId deviceId);

    /**
     * Requests the host service to monitor hosts with the given IP address and
     * notify listeners of changes.
     *
     * @param ip IP address of the host to monitor
     */
    void startMonitoringIp(IpAddress ip);

    /**
     * Stops the host service from monitoring an IP address.
     *
     * @param ip IP address to stop monitoring
     */
    // TODO clients can cancel other client's requests
    void stopMonitoringIp(IpAddress ip);

    /**
     * Requests the host service to resolve the MAC address for the given IP
     * address. This will trigger a notification to the host listeners if the MAC
     * address is found.
     *
     * @param ip IP address to find the MAC address for
     */
    void requestMac(IpAddress ip);

    /**
     * Returns the addresses information for all connection points.
     *
     * @return the set of address bindings for all connection points
     */
    Set<PortAddresses> getAddressBindings();

    /**
     * Retrieves the addresses that have been bound to the given connection
     * point.
     *
     * @param connectPoint the connection point to retrieve address bindings for
     * @return addresses bound to the port
     */
    Set<PortAddresses> getAddressBindingsForPort(ConnectPoint connectPoint);

    /**
     * Adds the specified host listener.
     *
     * @param listener host listener
     */
    void addListener(HostListener listener);

    /**
     * Removes the specified host listener.
     *
     * @param listener host listener
     */
    void removeListener(HostListener listener);

}

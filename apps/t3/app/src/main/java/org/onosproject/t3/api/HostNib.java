/*
 * Copyright 2020-present Open Networking Foundation
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

package org.onosproject.t3.api;

import com.google.common.collect.ImmutableSet;
import org.onlab.packet.IpAddress;
import org.onlab.packet.MacAddress;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.Host;
import org.onosproject.net.HostId;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * Represents Network Information Base (NIB) for hosts
 * and supports alternative functions to
 * {@link org.onosproject.net.host.HostService} for offline data.
 */
public class HostNib extends AbstractNib {

    // TODO with method optimization, store into subdivided structures at the first load
    private Set<Host> hosts;

    // use the singleton helper to create the instance
    protected HostNib() {
    }

    /**
     * Sets a set of hosts.
     *
     * @param hosts host set
     */
    public void setHosts(Set<Host> hosts) {
        this.hosts = hosts;
    }

    /**
     * Returns the set of hosts.
     *
     * @return host set
     */
    public Set<Host> getHosts() {
        return ImmutableSet.copyOf(hosts);
    }

    /**
     * Returns the host with the specified identifier.
     *
     * @param hostId host identifier
     * @return host or null if one with the given identifier is not known
     */
    public Host getHost(HostId hostId) {
        return hosts.stream()
                .filter(host -> host.id().equals(hostId))
                .findFirst().orElse(null);
    }

    /**
     * Returns the set of hosts whose most recent location is the specified
     * connection point.
     *
     * @param connectPoint connection point
     * @return set of hosts connected to the connection point
     */
    public Set<Host> getConnectedHosts(ConnectPoint connectPoint) {
        // TODO extend this method to support matching on auxLocations as well
        Set<Host> connectedHosts = hosts.stream()
                .filter(host -> host.locations().contains(connectPoint))
                .collect(Collectors.toSet());
        return connectedHosts != null ? ImmutableSet.copyOf(connectedHosts) : ImmutableSet.of();
    }

    /**
     * Returns the set of hosts that have the specified IP address.
     *
     * @param ip ip address
     * @return set of hosts with the given IP
     */
    public Set<Host> getHostsByIp(IpAddress ip) {
        Set<Host> hostsByIp = hosts.stream()
                .filter(host -> host.ipAddresses().contains(ip))
                .collect(Collectors.toSet());
        return hostsByIp != null ? ImmutableSet.copyOf(hostsByIp) : ImmutableSet.of();
    }

    /**
     * Returns the set of hosts that have the specified MAC address.
     *
     * @param mac mac address
     * @return set of hosts with the given mac
     */
    public Set<Host> getHostsByMac(MacAddress mac) {
        Set<Host> hostsByMac = hosts.stream()
                .filter(host -> host.mac().equals(mac))
                .collect(Collectors.toSet());
        return hostsByMac != null ? ImmutableSet.copyOf(hostsByMac) : ImmutableSet.of();
    }

    /**
     * Returns the singleton instance of hosts NIB.
     *
     * @return instance of hosts NIB
     */
    public static HostNib getInstance() {
        return HostNib.SingletonHelper.INSTANCE;
    }

    private static class SingletonHelper {
        private static final HostNib INSTANCE = new HostNib();
    }

}

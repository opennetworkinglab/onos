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

package org.onosproject.incubator.net.virtual.impl;

import org.onlab.packet.IpAddress;
import org.onlab.packet.MacAddress;
import org.onlab.packet.VlanId;
import org.onosproject.incubator.net.virtual.NetworkId;
import org.onosproject.incubator.net.virtual.VirtualHost;
import org.onosproject.incubator.net.virtual.VirtualNetworkService;
import org.onosproject.incubator.net.virtual.event.AbstractVirtualListenerManager;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Host;
import org.onosproject.net.HostId;
import org.onosproject.net.host.HostEvent;
import org.onosproject.net.host.HostListener;
import org.onosproject.net.host.HostService;

import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Host service implementation built on the virtual network service.
 */
public class VirtualNetworkHostManager
        extends AbstractVirtualListenerManager<HostEvent, HostListener>
        implements HostService {

    private static final String HOST_NULL = "Host ID cannot be null";

    /**
     * Creates a new virtual network host service object.
     *
     * @param virtualNetworkManager virtual network manager service
     * @param networkId a virtual network identifier
     */
    public VirtualNetworkHostManager(VirtualNetworkService virtualNetworkManager,
                                     NetworkId networkId) {
        super(virtualNetworkManager, networkId, HostEvent.class);
    }


    @Override
    public int getHostCount() {
        return manager.getVirtualHosts(this.networkId()).size();
    }

    @Override
    public Iterable<Host> getHosts() {
        return getHostsColl();
    }

    @Override
    public Host getHost(HostId hostId) {
        checkNotNull(hostId, HOST_NULL);
        Optional<VirtualHost> foundHost =
                manager.getVirtualHosts(this.networkId())
                .stream()
                .filter(host -> hostId.equals(host.id()))
                .findFirst();
        if (foundHost.isPresent()) {
            return foundHost.get();
        }
        return null;
    }

    /**
     * Gets a collection of virtual hosts.
     *
     * @return collection of virtual hosts.
     */
    private Collection<Host> getHostsColl() {
        return manager.getVirtualHosts(this.networkId())
                .stream().collect(Collectors.toSet());
    }

    /**
     * Filters specified collection.
     *
     * @param collection collection of hosts to filter
     * @param predicate condition to filter on
     * @return collection of virtual hosts that satisfy the filter condition
     */
    private Set<Host> filter(Collection<Host> collection, Predicate<Host> predicate) {
        return collection.stream().filter(predicate).collect(Collectors.toSet());
    }

    @Override
    public Set<Host> getHostsByVlan(VlanId vlanId) {
        checkNotNull(vlanId, "VLAN identifier cannot be null");
        return filter(getHostsColl(), host -> Objects.equals(host.vlan(), vlanId));
    }

    @Override
    public Set<Host> getHostsByMac(MacAddress mac) {
        checkNotNull(mac, "MAC address cannot be null");
        return filter(getHostsColl(), host -> Objects.equals(host.mac(), mac));
    }

    @Override
    public Set<Host> getHostsByIp(IpAddress ip) {
        checkNotNull(ip, "IP address cannot be null");
        return filter(getHostsColl(), host -> host.ipAddresses().contains(ip));
    }

    @Override
    public Set<Host> getConnectedHosts(ConnectPoint connectPoint) {
        checkNotNull(connectPoint, "Connect point cannot be null");
        return filter(getHostsColl(), host -> host.location().equals(connectPoint));
    }

    @Override
    public Set<Host> getConnectedHosts(DeviceId deviceId) {
        checkNotNull(deviceId, "Device identifier cannot be null");
        return filter(getHostsColl(), host -> host.location().deviceId().equals(deviceId));
    }

    @Override
    public void startMonitoringIp(IpAddress ip) {
        //TODO check what needs to be done here
    }

    @Override
    public void stopMonitoringIp(IpAddress ip) {
        //TODO check what needs to be done here
    }

    @Override
    public void requestMac(IpAddress ip) {
        //TODO check what needs to be done here
    }
}

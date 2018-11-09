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
package org.onosproject.incubator.net.virtual;

import org.onlab.packet.IpAddress;
import org.onlab.packet.MacAddress;
import org.onlab.packet.VlanId;
import org.onosproject.incubator.net.tunnel.TunnelId;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DeviceId;
import org.onosproject.net.HostId;
import org.onosproject.net.HostLocation;
import org.onosproject.net.Link;
import org.onosproject.net.PortNumber;
import org.onosproject.net.TenantId;
import org.onosproject.net.intent.Intent;
import org.onosproject.store.Store;

import java.util.Set;

/**
 * Mechanism for distributing and storing virtual network model information.
 */
public interface VirtualNetworkStore
        extends Store<VirtualNetworkEvent, VirtualNetworkStoreDelegate> {

    /**
     * Adds a new tenant ID to the store.
     *
     * @param tenantId tenant identifier
     */
    void addTenantId(TenantId tenantId);

    /**
     * Removes the specified tenant ID from the store.
     *
     * @param tenantId tenant identifier
     */
    void removeTenantId(TenantId tenantId);

    /**
     * Returns set of registered tenant IDs.
     *
     * @return set of tenant identifiers
     */
    Set<TenantId> getTenantIds();

    /**
     * Adds a new virtual network for the specified tenant to the store.
     *
     * @param tenantId tenant identifier
     * @return the virtual network
     */
    VirtualNetwork addNetwork(TenantId tenantId);

    /**
     * Removes the specified virtual network from the store.
     *
     * @param networkId network identifier
     */
    void removeNetwork(NetworkId networkId);

    /**
     * Adds a new virtual device to the store. This device will have no ports.
     *
     * @param networkId network identifier
     * @param deviceId  device identifier
     * @return the virtual device
     */
    VirtualDevice addDevice(NetworkId networkId, DeviceId deviceId);

    /**
     * Removes the specified virtual device from the given network.
     *
     * @param networkId network identifier
     * @param deviceId  device identifier
     */
    void removeDevice(NetworkId networkId, DeviceId deviceId);

    /**
     * Adds a new virtual host to the store.
     *
     * @param networkId network identifier
     * @param hostId    host identifier
     * @param mac       mac address
     * @param vlan      vlan identifier
     * @param location  host location
     * @param ips       set of ip addresses
     * @return the virtual host
     */
    VirtualHost addHost(NetworkId networkId, HostId hostId, MacAddress mac,
                        VlanId vlan, HostLocation location, Set<IpAddress> ips);

    /**
     * Removes the specified virtual host from the store.
     *
     * @param networkId network identifier
     * @param hostId    host identifier
     */
    void removeHost(NetworkId networkId, HostId hostId);

    /**
     * Adds a new virtual link.
     *
     * @param networkId  network identifier
     * @param src        source end-point of the link
     * @param dst        destination end-point of the link
     * @param state      link state
     * @param realizedBy underlying tunnel identifier using which this link is realized
     * @return the virtual link
     */
    VirtualLink addLink(NetworkId networkId, ConnectPoint src, ConnectPoint dst, Link.State state, TunnelId realizedBy);

    /**
     * Updates the tunnelId in the virtual link.
     *
     * @param virtualLink virtual link
     * @param tunnelId    tunnel identifier
     * @param state       link state
     */
    void updateLink(VirtualLink virtualLink, TunnelId tunnelId, Link.State state);

    /**
     * Removes the specified link from the store.
     *
     * @param networkId network identifier
     * @param src       source connection point
     * @param dst       destination connection point
     * @return the virtual link
     */
    VirtualLink removeLink(NetworkId networkId, ConnectPoint src, ConnectPoint dst);

    /**
     * Adds a new virtual port to the network.
     *
     * @param networkId  network identifier
     * @param deviceId   device identifier
     * @param portNumber port number
     * @param realizedBy underlying port which realizes the virtual port
     * @return the virtual port
     */
    VirtualPort addPort(NetworkId networkId, DeviceId deviceId,
                        PortNumber portNumber, ConnectPoint realizedBy);

    /**
     * Binds an existing virtual port to the network.
     *
     * @param networkId  network identifier
     * @param deviceId   device identifier
     * @param portNumber port number
     * @param realizedBy underlying port which realizes the virtual port
     */
     void bindPort(NetworkId networkId, DeviceId deviceId,
                        PortNumber portNumber, ConnectPoint realizedBy);

    /**
     * Updates port state of an existing virtual port.
     *
     * @param networkId  network identifier
     * @param deviceId   device identifier
     * @param portNumber port number
     * @param isEnabled  indicator whether the port is up and active
     */
     void updatePortState(NetworkId networkId, DeviceId deviceId,
                        PortNumber portNumber, boolean isEnabled);

    /**
     * Removes the specified port from the given device and network.
     *
     * @param networkId  network identifier
     * @param deviceId   device identifier
     * @param portNumber port number
     */
    void removePort(NetworkId networkId, DeviceId deviceId, PortNumber portNumber);

    /**
     * Returns the list of networks.
     *
     * @param tenantId tenant identifier
     * @return set of virtual networks
     */
    Set<VirtualNetwork> getNetworks(TenantId tenantId);

    /**
     * Returns the virtual network for the given network identifier.
     *
     * @param networkId network identifier
     * @return the virtual network
     */
    VirtualNetwork getNetwork(NetworkId networkId);

    /**
     * Returns the list of devices in the specified virtual network.
     *
     * @param networkId network identifier
     * @return set of virtual devices
     */
    Set<VirtualDevice> getDevices(NetworkId networkId);

    /**
     * Returns the list of hosts in the specified virtual network.
     *
     * @param networkId network identifier
     * @return set of virtual hosts
     */
    Set<VirtualHost> getHosts(NetworkId networkId);

    /**
     * Returns the list of virtual links in the specified virtual network.
     *
     * @param networkId network identifier
     * @return set of virtual links
     */
    Set<VirtualLink> getLinks(NetworkId networkId);

    /**
     * Returns the virtual link matching the network identifier, source connect point,
     * and destination connect point.
     *
     * @param networkId network identifier
     * @param src       source connect point
     * @param dst       destination connect point
     * @return virtual link
     */
    VirtualLink getLink(NetworkId networkId, ConnectPoint src, ConnectPoint dst);

    /**
     * Returns the list of ports of the specified virtual device.
     *
     * @param networkId network identifier
     * @param deviceId  device identifier
     * @return set of virtual networks
     */
    Set<VirtualPort> getPorts(NetworkId networkId, DeviceId deviceId);

    /**
     * Adds the intent to tunnel identifier mapping to the store.
     *
     * @param intent   intent
     * @param tunnelId tunnel identifier
     * @deprecated in Kingfisher Release (1.10)
     */
    @Deprecated
    void addTunnelId(Intent intent, TunnelId tunnelId);

    /**
     * Return the set of tunnel identifiers store against the intent.
     *
     * @param intent intent
     * @return set of tunnel identifiers
     * @deprecated in Kingfisher Release (1.10)
     */
    @Deprecated
    Set<TunnelId> getTunnelIds(Intent intent);

    /**
     * Removes the intent to tunnel identifier mapping from the store.
     *
     * @param intent   intent
     * @param tunnelId tunnel identifier
     * @deprecated in Kingfisher Release (1.10)
     */
    @Deprecated
    void removeTunnelId(Intent intent, TunnelId tunnelId);
}

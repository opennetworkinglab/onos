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
package org.onosproject.incubator.net.virtual;

import org.onosproject.incubator.net.tunnel.TunnelId;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Port;
import org.onosproject.net.PortNumber;
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
     * Renmoves the specified virtual device from the given network.
     *
     * @param networkId network identifier
     * @param deviceId  device identifier
     */
    void removeDevice(NetworkId networkId, DeviceId deviceId);

    /**
     * Adds a new virtual link.
     *
     * @param networkId  network identifier
     * @param src        source end-point of the link
     * @param dst        destination end-point of the link
     * @param realizedBy underlying tunnel using which this link is realized
     * @return the virtual link
     */
    VirtualLink addLink(NetworkId networkId, ConnectPoint src, ConnectPoint dst,
                        TunnelId realizedBy);

    /**
     * Removes the specified link from the store.
     *
     * @param networkId network identifier
     * @param src       source connection point
     * @param dst       destination connection point
     */
    void removeLink(NetworkId networkId, ConnectPoint src, ConnectPoint dst);

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
                        PortNumber portNumber, Port realizedBy);

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
     * Returns the list of devices in the specified virtual network.
     *
     * @param networkId network identifier
     * @return set of virtual devices
     */
    Set<VirtualDevice> getDevices(NetworkId networkId);

    /**
     * Returns the list of virtual links in the specified virtual network.
     *
     * @param networkId network identifier
     * @return set of virtual links
     */
    Set<VirtualLink> getLinks(NetworkId networkId);

    /**
     * Returns the list of ports of the specified virtual device.
     *
     * @param networkId network identifier
     * @param deviceId   device identifier
     * @return set of virtual networks
     */
    Set<VirtualPort> getPorts(NetworkId networkId, DeviceId deviceId);

}

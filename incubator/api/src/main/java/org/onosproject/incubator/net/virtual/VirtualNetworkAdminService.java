/*
 * Copyright 2015-present Open Networking Laboratory
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

import com.google.common.annotations.Beta;
import org.onlab.packet.IpAddress;
import org.onlab.packet.MacAddress;
import org.onlab.packet.VlanId;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DeviceId;
import org.onosproject.net.HostId;
import org.onosproject.net.HostLocation;
import org.onosproject.net.PortNumber;

import java.util.Set;

/**
 * Service for managing the inventory of virtual networks.
 */
@Beta
public interface VirtualNetworkAdminService extends VirtualNetworkService {

    /**
     * Registers the specified, externally generated tenant identifier.
     *
     * @param tenantId tenant identifier
     */
    void registerTenantId(TenantId tenantId);

    /**
     * Unregisters the specified, externally generated tenant identifier.
     *
     * @param tenantId tenant identifier
     * @throws IllegalStateException if there are networks still owned by this tenant
     */
    void unregisterTenantId(TenantId tenantId);

    /**
     * Returns the set of tenant identifiers known to the system.
     *
     * @return set of known tenant identifiers
     */
    Set<TenantId> getTenantIds();

    /**
     * Creates a new virtual network for the specified tenant.
     *
     * @param tenantId tenant identifier
     * @return newly created virtual network
     */
    VirtualNetwork createVirtualNetwork(TenantId tenantId);

    /**
     * Removes the specified virtual network and all its devices and links.
     *
     * @param networkId network identifier
     */
    void removeVirtualNetwork(NetworkId networkId);

    /**
     * Creates a new virtual device within the specified network. The device id
     * must be unique within the bounds of the network.
     *
     * @param networkId network identifier
     * @param deviceId  device identifier
     * @return newly created virtual device
     * @throws org.onlab.util.ItemNotFoundException if no such network found
     */
    VirtualDevice createVirtualDevice(NetworkId networkId, DeviceId deviceId);

    /**
     * Removes the specified virtual device and all its ports and affiliated links.
     *
     * @param networkId network identifier
     * @param deviceId  device identifier
     * @throws org.onlab.util.ItemNotFoundException if no such network or device found
     */
    void removeVirtualDevice(NetworkId networkId, DeviceId deviceId);

    /**
     * Creates a new virtual host within the specified network. The host id
     * must be unique within the bounds of the network.
     *
     * @param networkId network identifier
     * @param hostId    host identifier
     * @param mac       mac address
     * @param vlan      vlan identifier
     * @param location  host location
     * @param ips       set of ip addresses
     * @return newly created virtual host
     * @throws org.onlab.util.ItemNotFoundException if no such network found
     */
    VirtualHost createVirtualHost(NetworkId networkId, HostId hostId, MacAddress mac,
                                  VlanId vlan, HostLocation location, Set<IpAddress> ips);

    /**
     * Removes the specified virtual host.
     *
     * @param networkId network identifier
     * @param hostId  host identifier
     * @throws org.onlab.util.ItemNotFoundException if no such network or host found
     */
    void removeVirtualHost(NetworkId networkId, HostId hostId);

    /**
     * Creates a new virtual link within the specified network.
     *
     * @param networkId  network identifier
     * @param src        source connection point
     * @param dst        destination connection point
     * @return newly created virtual link
     * @throws org.onlab.util.ItemNotFoundException if no such network found
     */
    VirtualLink createVirtualLink(NetworkId networkId,
                                  ConnectPoint src, ConnectPoint dst);

    // TODO: Discuss whether we should provide an alternate createVirtualLink
    // which is backed by a Path instead; I'm leaning towards not doing that.

    /**
     * Removes the specified virtual link.
     *
     * @param networkId network identifier
     * @param src       source connection point
     * @param dst       destination connection point
     * @throws org.onlab.util.ItemNotFoundException if no such network or link found
     */
    void removeVirtualLink(NetworkId networkId, ConnectPoint src, ConnectPoint dst);

    /**
     * Creates a new virtual port on the specified device.
     *
     * @param networkId  network identifier
     * @param deviceId   virtual device identifier
     * @param portNumber virtual port number
     * @param realizedBy underlying physical port using which this virtual port is realized
     * @return newly created port
     * @throws org.onlab.util.ItemNotFoundException if no such network or device is found
     */
    VirtualPort createVirtualPort(NetworkId networkId, DeviceId deviceId,
                                  PortNumber portNumber, ConnectPoint realizedBy);

    /**
     * Binds an existing virtual port on the specified device.
     *
     * @param networkId  network identifier
     * @param deviceId   virtual device identifier
     * @param portNumber virtual port number
     * @param realizedBy underlying physical port using which this virtual port is realized
     * @throws org.onlab.util.ItemNotFoundException if no such network or device is found
     */
    void bindVirtualPort(NetworkId networkId, DeviceId deviceId,
                                  PortNumber portNumber, ConnectPoint realizedBy);

    /**
     * Removes the specified virtual port.
     *
     * @param networkId  network identifier
     * @param deviceId   device identifier
     * @param portNumber port number
     * @throws org.onlab.util.ItemNotFoundException if no such network or port found
     */
    void removeVirtualPort(NetworkId networkId, DeviceId deviceId, PortNumber portNumber);
}

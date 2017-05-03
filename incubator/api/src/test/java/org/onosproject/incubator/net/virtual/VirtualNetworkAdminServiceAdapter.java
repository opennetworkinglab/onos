/*
 * Copyright 2017-present Open Networking Laboratory
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
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DeviceId;
import org.onosproject.net.HostId;
import org.onosproject.net.HostLocation;
import org.onosproject.net.PortNumber;

import java.util.Set;

/**
 * Test adapter for virtual network admin service.
 */
public class VirtualNetworkAdminServiceAdapter
        extends VirtualNetworkServiceAdapter
        implements VirtualNetworkAdminService {

    @Override
    public void registerTenantId(TenantId tenantId) {

    }

    @Override
    public void unregisterTenantId(TenantId tenantId) {

    }

    @Override
    public Set<TenantId> getTenantIds() {
        return null;
    }

    @Override
    public VirtualNetwork createVirtualNetwork(TenantId tenantId) {
        return null;
    }

    @Override
    public void removeVirtualNetwork(NetworkId networkId) {

    }

    @Override
    public VirtualDevice createVirtualDevice(NetworkId networkId, DeviceId deviceId) {
        return null;
    }

    @Override
    public void removeVirtualDevice(NetworkId networkId, DeviceId deviceId) {

    }

    @Override
    public VirtualHost createVirtualHost(NetworkId networkId, HostId hostId,
                                         MacAddress mac, VlanId vlan,
                                         HostLocation location, Set<IpAddress> ips) {
        return null;
    }

    @Override
    public void removeVirtualHost(NetworkId networkId, HostId hostId) {

    }

    @Override
    public VirtualLink createVirtualLink(NetworkId networkId, ConnectPoint src, ConnectPoint dst) {
        return null;
    }

    @Override
    public void removeVirtualLink(NetworkId networkId, ConnectPoint src, ConnectPoint dst) {

    }

    @Override
    public VirtualPort createVirtualPort(NetworkId networkId, DeviceId deviceId,
                                         PortNumber portNumber, ConnectPoint realizedBy) {
        return null;
    }

    @Override
    public void bindVirtualPort(NetworkId networkId, DeviceId deviceId,
                                PortNumber portNumber, ConnectPoint realizedBy) {

    }

    @Override
    public void removeVirtualPort(NetworkId networkId, DeviceId deviceId, PortNumber portNumber) {

    }
}

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
package org.onosproject.incubator.store.virtual.impl;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Service;
import org.onosproject.incubator.net.tunnel.TunnelId;
import org.onosproject.incubator.net.virtual.DefaultVirtualDevice;
import org.onosproject.incubator.net.virtual.DefaultVirtualNetwork;
import org.onosproject.incubator.net.virtual.NetworkId;
import org.onosproject.incubator.net.virtual.TenantId;
import org.onosproject.incubator.net.virtual.VirtualDevice;
import org.onosproject.incubator.net.virtual.VirtualLink;
import org.onosproject.incubator.net.virtual.VirtualNetwork;
import org.onosproject.incubator.net.virtual.VirtualNetworkEvent;
import org.onosproject.incubator.net.virtual.VirtualNetworkStore;
import org.onosproject.incubator.net.virtual.VirtualNetworkStoreDelegate;
import org.onosproject.incubator.net.virtual.VirtualPort;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Port;
import org.onosproject.net.PortNumber;
import org.onosproject.store.AbstractStore;
import org.slf4j.Logger;

import java.util.Set;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Implementation of the network store.
 */
@Component(immediate = true)
@Service
public class DistributedVirtualNetworkStore
        extends AbstractStore<VirtualNetworkEvent, VirtualNetworkStoreDelegate>
        implements VirtualNetworkStore {

    private final Logger log = getLogger(getClass());

    // TODO: track tenants by ID
    // TODO: track networks by ID and by tenants
    // TODO: track devices by network ID and device ID
    // TODO: track devices by network ID
    // TODO: setup block allocator for network IDs

    // TODO: notify delegate

    @Activate
    public void activate() {
        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        log.info("Stopped");
    }

    @Override
    public void addTenantId(TenantId tenantId) {
    }

    @Override
    public void removeTenantId(TenantId tenantId) {
    }

    @Override
    public Set<TenantId> getTenantIds() {
        return null;
    }

    @Override
    public VirtualNetwork addNetwork(TenantId tenantId) {
        return new DefaultVirtualNetwork(genNetworkId(), tenantId);
    }

    private NetworkId genNetworkId() {
        return NetworkId.networkId(0); // TODO: use a block allocator
    }


    @Override
    public void removeNetwork(NetworkId networkId) {
    }

    @Override
    public VirtualDevice addDevice(NetworkId networkId, DeviceId deviceId) {
        return new DefaultVirtualDevice(networkId, deviceId);
    }

    @Override
    public void removeDevice(NetworkId networkId, DeviceId deviceId) {
    }

    @Override
    public VirtualLink addLink(NetworkId networkId, ConnectPoint src, ConnectPoint dst, TunnelId realizedBy) {
        return null;
    }

    @Override
    public void removeLink(NetworkId networkId, ConnectPoint src, ConnectPoint dst) {
    }

    @Override
    public VirtualPort addPort(NetworkId networkId, DeviceId deviceId, PortNumber portNumber, Port realizedBy) {
        return null;
    }

    @Override
    public void removePort(NetworkId networkId, DeviceId deviceId, PortNumber portNumber) {
    }

    @Override
    public Set<VirtualNetwork> getNetworks(TenantId tenantId) {
        return null;
    }

    @Override
    public Set<VirtualDevice> getDevices(NetworkId networkId) {
        return null;
    }

    @Override
    public Set<VirtualLink> getLinks(NetworkId networkId) {
        return null;
    }

    @Override
    public Set<VirtualPort> getPorts(NetworkId networkId, DeviceId deviceId) {
        return null;
    }
}

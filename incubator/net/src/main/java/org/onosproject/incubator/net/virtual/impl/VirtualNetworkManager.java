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
package org.onosproject.incubator.net.virtual.impl;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onosproject.incubator.net.tunnel.TunnelId;
import org.onosproject.incubator.net.virtual.NetworkId;
import org.onosproject.incubator.net.virtual.TenantId;
import org.onosproject.incubator.net.virtual.VirtualDevice;
import org.onosproject.incubator.net.virtual.VirtualLink;
import org.onosproject.incubator.net.virtual.VirtualNetwork;
import org.onosproject.incubator.net.virtual.VirtualNetworkAdminService;
import org.onosproject.incubator.net.virtual.VirtualNetworkEvent;
import org.onosproject.incubator.net.virtual.VirtualNetworkListener;
import org.onosproject.incubator.net.virtual.VirtualNetworkProvider;
import org.onosproject.incubator.net.virtual.VirtualNetworkProviderRegistry;
import org.onosproject.incubator.net.virtual.VirtualNetworkProviderService;
import org.onosproject.incubator.net.virtual.VirtualNetworkService;
import org.onosproject.incubator.net.virtual.VirtualNetworkStore;
import org.onosproject.incubator.net.virtual.VirtualNetworkStoreDelegate;
import org.onosproject.incubator.net.virtual.VirtualPort;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Port;
import org.onosproject.net.PortNumber;
import org.onosproject.net.provider.AbstractListenerProviderRegistry;
import org.onosproject.net.provider.AbstractProviderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Implementation of the virtual network service.
 */
@Component(immediate = true)
@Service
public class VirtualNetworkManager
        extends AbstractListenerProviderRegistry<VirtualNetworkEvent, VirtualNetworkListener,
                                                 VirtualNetworkProvider, VirtualNetworkProviderService>
        implements VirtualNetworkService, VirtualNetworkAdminService, VirtualNetworkProviderRegistry {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private static final String TENANT_NULL = "Tenant ID cannot be null";
    private static final String NETWORK_NULL = "Network ID cannot be null";
    private static final String DEVICE_NULL = "Device ID cannot be null";
    private static final String LINK_POINT_NULL = "Link end-point cannot be null";

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected VirtualNetworkStore store;

    private VirtualNetworkStoreDelegate delegate = new InternalStoreDelegate();

    // TODO: figure out how to coordinate "implementation" of a virtual network in a cluster

    @Activate
    protected void activate() {
        store.setDelegate(delegate);
        log.info("Started");
    }

    @Deactivate
    protected void deactivate() {
        store.unsetDelegate(delegate);
        log.info("Stopped");
    }

    @Override
    public void registerTenantId(TenantId tenantId) {
        checkNotNull(tenantId, TENANT_NULL);
        store.addTenantId(tenantId);
    }

    @Override
    public void unregisterTenantId(TenantId tenantId) {
        checkNotNull(tenantId, TENANT_NULL);
        store.removeTenantId(tenantId);
    }

    @Override
    public Set<TenantId> getTenantIds() {
        return store.getTenantIds();
    }

    @Override
    public VirtualNetwork createVirtualNetwork(TenantId tenantId) {
        checkNotNull(tenantId, TENANT_NULL);
        return store.addNetwork(tenantId);
    }

    @Override
    public void removeVirtualNetwork(NetworkId networkId) {
        checkNotNull(networkId, NETWORK_NULL);
        store.removeNetwork(networkId);
    }

    @Override
    public VirtualDevice createVirtualDevice(NetworkId networkId, DeviceId deviceId) {
        checkNotNull(networkId, NETWORK_NULL);
        checkNotNull(deviceId, DEVICE_NULL);
        return store.addDevice(networkId, deviceId);
    }

    @Override
    public void removeVirtualDevice(NetworkId networkId, DeviceId deviceId) {
        checkNotNull(networkId, NETWORK_NULL);
        checkNotNull(deviceId, DEVICE_NULL);
        store.removeDevice(networkId, deviceId);
    }

    @Override
    public VirtualLink createVirtualLink(NetworkId networkId,
                                         ConnectPoint src, ConnectPoint dst,
                                         TunnelId realizedBy) {
        checkNotNull(networkId, NETWORK_NULL);
        checkNotNull(src, LINK_POINT_NULL);
        checkNotNull(dst, LINK_POINT_NULL);
        checkNotNull(realizedBy, "Tunnel ID cannot be null");
        return store.addLink(networkId, src, dst, realizedBy);
    }

    @Override
    public void removeVirtualLink(NetworkId networkId, ConnectPoint src, ConnectPoint dst) {
        checkNotNull(networkId, NETWORK_NULL);
        checkNotNull(src, LINK_POINT_NULL);
        checkNotNull(dst, LINK_POINT_NULL);
        store.removeLink(networkId, src, dst);
    }

    @Override
    public VirtualPort createVirtualPort(NetworkId networkId, DeviceId deviceId,
                                         PortNumber portNumber, Port realizedBy) {
        checkNotNull(networkId, NETWORK_NULL);
        checkNotNull(deviceId, DEVICE_NULL);
        checkNotNull(portNumber, "Port description cannot be null");
        return store.addPort(networkId, deviceId, portNumber, realizedBy);
    }

    @Override
    public void removeVirtualPort(NetworkId networkId, DeviceId deviceId, PortNumber portNumber) {
        checkNotNull(networkId, NETWORK_NULL);
        checkNotNull(deviceId, DEVICE_NULL);
        checkNotNull(portNumber, "Port number cannot be null");
        store.removePort(networkId, deviceId, portNumber);
    }

    @Override
    public Set<VirtualNetwork> getVirtualNetworks(TenantId tenantId) {
        checkNotNull(tenantId, TENANT_NULL);
        return store.getNetworks(tenantId);
    }

    @Override
    public Set<VirtualDevice> getVirtualDevices(NetworkId networkId) {
        checkNotNull(networkId, NETWORK_NULL);
        return store.getDevices(networkId);
    }

    @Override
    public Set<VirtualLink> getVirtualLinks(NetworkId networkId) {
        checkNotNull(networkId, NETWORK_NULL);
        return store.getLinks(networkId);
    }

    @Override
    public Set<VirtualPort> getVirtualPorts(NetworkId networkId, DeviceId deviceId) {
        checkNotNull(networkId, NETWORK_NULL);
        checkNotNull(deviceId, DEVICE_NULL);
        return store.getPorts(networkId, deviceId);
    }

    @Override
    public <T> T get(NetworkId networkId, Class<T> serviceClass) {
        checkNotNull(networkId, NETWORK_NULL);
        return null;
    }

    @Override
    protected VirtualNetworkProviderService createProviderService(VirtualNetworkProvider provider) {
        return new InternalVirtualNetworkProviderService(provider);
    }

    // Service issued to registered virtual network providers so that they
    // can interact with the core.
    private class InternalVirtualNetworkProviderService
            extends AbstractProviderService<VirtualNetworkProvider>
            implements VirtualNetworkProviderService {
        InternalVirtualNetworkProviderService(VirtualNetworkProvider provider) {
            super(provider);
        }
    }

    // Auxiliary store delegate to receive notification about changes in
    // the virtual network configuration store state - by the store itself.
    private class InternalStoreDelegate implements VirtualNetworkStoreDelegate {
        @Override
        public void notify(VirtualNetworkEvent event) {
            post(event);
        }
    }

}

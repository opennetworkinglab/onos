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
package org.onosproject.incubator.net.virtual.impl;

import com.google.common.collect.Maps;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onlab.packet.IpAddress;
import org.onlab.packet.MacAddress;
import org.onlab.packet.VlanId;
import org.onosproject.incubator.net.tunnel.TunnelId;
import org.onosproject.incubator.net.virtual.NetworkId;
import org.onosproject.incubator.net.virtual.TenantId;
import org.onosproject.incubator.net.virtual.VirtualDevice;
import org.onosproject.incubator.net.virtual.VirtualHost;
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
import org.onosproject.net.HostId;
import org.onosproject.net.HostLocation;
import org.onosproject.net.Link;
import org.onosproject.net.Port;
import org.onosproject.net.PortNumber;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.link.LinkService;
import org.onosproject.net.provider.AbstractListenerProviderRegistry;
import org.onosproject.net.provider.AbstractProviderService;
import org.onosproject.net.topology.TopologyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
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
    private static final String VIRTUAL_LINK_NULL = "Virtual Link cannot be null";

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected VirtualNetworkStore store;

    private VirtualNetworkStoreDelegate delegate = this::post;

    // TODO: figure out how to coordinate "implementation" of a virtual network in a cluster

    @Activate
    protected void activate() {
        store.setDelegate(delegate);
        eventDispatcher.addSink(VirtualNetworkEvent.class, listenerRegistry);

        log.info("Started");
    }

    @Deactivate
    protected void deactivate() {
        store.unsetDelegate(delegate);
        eventDispatcher.removeSink(VirtualNetworkEvent.class);

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
    public VirtualHost createVirtualHost(NetworkId networkId, HostId hostId, MacAddress mac,
                                         VlanId vlan, HostLocation location, Set<IpAddress> ips) {
        checkNotNull(networkId, NETWORK_NULL);
        checkNotNull(hostId, DEVICE_NULL);
        return store.addHost(networkId, hostId, mac, vlan, location, ips);
    }

    @Override
    public void removeVirtualHost(NetworkId networkId, HostId hostId) {
        checkNotNull(networkId, NETWORK_NULL);
        checkNotNull(hostId, DEVICE_NULL);
        store.removeHost(networkId, hostId);
    }

    @Override
    public VirtualLink createVirtualLink(NetworkId networkId,
                                         ConnectPoint src, ConnectPoint dst) {
        checkNotNull(networkId, NETWORK_NULL);
        checkNotNull(src, LINK_POINT_NULL);
        checkNotNull(dst, LINK_POINT_NULL);
        VirtualLink virtualLink = store.addLink(networkId, src, dst, Link.State.INACTIVE, null);
        checkNotNull(virtualLink, VIRTUAL_LINK_NULL);

        if (virtualLink.providerId() != null) {
            VirtualNetworkProvider provider = getProvider(virtualLink.providerId());
            if (provider != null) {
                TunnelId tunnelId = provider.createTunnel(networkId, mapVirtualToPhysicalPort(networkId, src),
                                                          mapVirtualToPhysicalPort(networkId, dst));
                store.updateLink(virtualLink, tunnelId, Link.State.INACTIVE);
            }
        }
        return virtualLink;
    }

    /**
     * Maps the virtual connect point to a physical connect point.
     *
     * @param networkId network identifier
     * @param virtualCp virtual connect point
     * @return physical connect point
     */
    private ConnectPoint mapVirtualToPhysicalPort(NetworkId networkId,
                                                  ConnectPoint virtualCp) {
        Set<VirtualPort> ports = store.getPorts(networkId, virtualCp.deviceId());
        for (VirtualPort port : ports) {
            if (port.element().id().equals(virtualCp.elementId()) &&
                    port.number().equals(virtualCp.port())) {
                return new ConnectPoint(port.realizedBy().element().id(), port.realizedBy().number());
            }
        }
        return null;
    }

    /**
     * Maps the physical connect point to a virtual connect point.
     *
     * @param networkId network identifier
     * @param physicalCp physical connect point
     * @return virtual connect point
     */
    private ConnectPoint mapPhysicalToVirtualToPort(NetworkId networkId,
                                                  ConnectPoint physicalCp) {
        Set<VirtualPort> ports = store.getPorts(networkId, null);
        for (VirtualPort port : ports) {
            if (port.realizedBy().element().id().equals(physicalCp.elementId()) &&
                    port.realizedBy().number().equals(physicalCp.port())) {
                return new ConnectPoint(port.element().id(), port.number());
            }
        }
        return null;
    }

    @Override
    public void removeVirtualLink(NetworkId networkId, ConnectPoint src, ConnectPoint dst) {
        checkNotNull(networkId, NETWORK_NULL);
        checkNotNull(src, LINK_POINT_NULL);
        checkNotNull(dst, LINK_POINT_NULL);
        VirtualLink virtualLink = store.removeLink(networkId, src, dst);

        if (virtualLink != null && virtualLink.providerId() != null) {
            VirtualNetworkProvider provider = getProvider(virtualLink.providerId());
            if (provider != null) {
                provider.destroyTunnel(networkId, virtualLink.tunnelId());
            }
        }
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

    private VirtualNetwork getVirtualNetwork(NetworkId networkId) {
        checkNotNull(networkId, NETWORK_NULL);
        return store.getNetwork(networkId);
    }

    @Override
    public Set<VirtualDevice> getVirtualDevices(NetworkId networkId) {
        checkNotNull(networkId, NETWORK_NULL);
        return store.getDevices(networkId);
    }

    @Override
    public Set<VirtualHost> getVirtualHosts(NetworkId networkId) {
        checkNotNull(networkId, NETWORK_NULL);
        return store.getHosts(networkId);
    }

    @Override
    public Set<VirtualLink> getVirtualLinks(NetworkId networkId) {
        checkNotNull(networkId, NETWORK_NULL);
        return store.getLinks(networkId);
    }

    @Override
    public Set<VirtualPort> getVirtualPorts(NetworkId networkId, DeviceId deviceId) {
        checkNotNull(networkId, NETWORK_NULL);
        return store.getPorts(networkId, deviceId);
    }

    private final Map<ServiceKey, VnetService> networkServices = Maps.newConcurrentMap();

    @Override
    public <T> T get(NetworkId networkId, Class<T> serviceClass) {
        checkNotNull(networkId, NETWORK_NULL);
        ServiceKey serviceKey = networkServiceKey(networkId, serviceClass);
        VnetService service = lookup(serviceKey);
        if (service == null) {
            service = create(serviceKey);
        }
        return (T) service;
    }

    private VnetService lookup(ServiceKey serviceKey) {
        return networkServices.get(serviceKey);
    }

    private <T> ServiceKey networkServiceKey(NetworkId networkId, Class<T> serviceClass) {
        return new ServiceKey(networkId, serviceClass);
    }


    private VnetService create(ServiceKey serviceKey) {
        VirtualNetwork network = getVirtualNetwork(serviceKey.networkId());
        VnetService service;
        if (serviceKey.serviceClass.equals(DeviceService.class)) {
            service = new VirtualNetworkDeviceService(this, network);
        } else if (serviceKey.serviceClass.equals(LinkService.class)) {
            service = new VirtualNetworkLinkService(this, network);
        } else if (serviceKey.serviceClass.equals(TopologyService.class)) {
            service = new VirtualNetworkTopologyService(this, network);
        } else {
            return null;
        }
        networkServices.put(serviceKey, service);
        return service;
    }

    private class ServiceKey {
        final NetworkId networkId;
        final Class serviceClass;

        public ServiceKey(NetworkId networkId, Class serviceClass) {
            checkNotNull(networkId, NETWORK_NULL);
            this.networkId = networkId;
            this.serviceClass = serviceClass;
        }

        public NetworkId networkId() {
            return networkId;
        }

        public Class serviceClass() {
            return serviceClass;
        }
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

        @Override
        public void tunnelUp(NetworkId networkId, ConnectPoint src, ConnectPoint dst, TunnelId tunnelId) {

            ConnectPoint srcVirtualCp = mapPhysicalToVirtualToPort(networkId, src);
            ConnectPoint dstVirtualCp = mapPhysicalToVirtualToPort(networkId, dst);
            if ((srcVirtualCp == null) || (dstVirtualCp == null)) {
                log.error("Src or dst virtual connection point was not found.");
            }

            VirtualLink virtualLink = store.getLink(networkId, srcVirtualCp, dstVirtualCp);
            if (virtualLink != null) {
                store.updateLink(virtualLink, tunnelId, Link.State.ACTIVE);
            }
        }

        @Override
        public void tunnelDown(NetworkId networkId, ConnectPoint src, ConnectPoint dst, TunnelId tunnelId) {
            ConnectPoint srcVirtualCp = mapPhysicalToVirtualToPort(networkId, src);
            ConnectPoint dstVirtualCp = mapPhysicalToVirtualToPort(networkId, dst);
            if ((srcVirtualCp == null) || (dstVirtualCp == null)) {
                log.error("Src or dst virtual connection point was not found.");
            }

            VirtualLink virtualLink = store.getLink(networkId, srcVirtualCp, dstVirtualCp);
            if (virtualLink != null) {
                store.updateLink(virtualLink, tunnelId, Link.State.INACTIVE);
            }
        }
    }

}

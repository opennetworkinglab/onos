/*
 * Copyright 2018-present Open Networking Foundation
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

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import org.onlab.osgi.DefaultServiceDirectory;
import org.onlab.osgi.ServiceDirectory;
import org.onlab.packet.IpAddress;
import org.onlab.packet.MacAddress;
import org.onlab.packet.VlanId;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.incubator.net.tunnel.TunnelId;
import org.onosproject.incubator.net.virtual.DefaultVirtualLink;
import org.onosproject.incubator.net.virtual.NetworkId;
import org.onosproject.net.TenantId;
import org.onosproject.incubator.net.virtual.VirtualDevice;
import org.onosproject.incubator.net.virtual.VirtualHost;
import org.onosproject.incubator.net.virtual.VirtualLink;
import org.onosproject.incubator.net.virtual.VirtualNetwork;
import org.onosproject.incubator.net.virtual.VirtualNetworkAdminService;
import org.onosproject.incubator.net.virtual.VirtualNetworkEvent;
import org.onosproject.incubator.net.virtual.VirtualNetworkListener;
import org.onosproject.incubator.net.virtual.VirtualNetworkService;
import org.onosproject.incubator.net.virtual.VirtualNetworkStore;
import org.onosproject.incubator.net.virtual.VirtualNetworkStoreDelegate;
import org.onosproject.incubator.net.virtual.VirtualPort;
import org.onosproject.incubator.net.virtual.VnetService;
import org.onosproject.incubator.net.virtual.event.VirtualEvent;
import org.onosproject.incubator.net.virtual.event.VirtualListenerRegistryManager;
import org.onosproject.incubator.net.virtual.provider.VirtualNetworkProvider;
import org.onosproject.incubator.net.virtual.provider.VirtualNetworkProviderRegistry;
import org.onosproject.incubator.net.virtual.provider.VirtualNetworkProviderService;
import org.onosproject.mastership.MastershipAdminService;
import org.onosproject.mastership.MastershipService;
import org.onosproject.mastership.MastershipTermService;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DeviceId;
import org.onosproject.net.HostId;
import org.onosproject.net.HostLocation;
import org.onosproject.net.Link;
import org.onosproject.net.PortNumber;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.flow.FlowRuleService;
import org.onosproject.net.flowobjective.FlowObjectiveService;
import org.onosproject.net.group.GroupService;
import org.onosproject.net.host.HostService;
import org.onosproject.net.intent.IntentService;
import org.onosproject.net.link.LinkService;
import org.onosproject.net.meter.MeterService;
import org.onosproject.net.packet.PacketService;
import org.onosproject.net.provider.AbstractListenerProviderRegistry;
import org.onosproject.net.provider.AbstractProviderService;
import org.onosproject.net.topology.PathService;
import org.onosproject.net.topology.TopologyService;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Implementation of the virtual network service.
 */
@Component(service = {
                   VirtualNetworkService.class,
                   VirtualNetworkAdminService.class,
                   VirtualNetworkService.class,
                   VirtualNetworkProviderRegistry.class
            })
public class VirtualNetworkManager
        extends AbstractListenerProviderRegistry<VirtualNetworkEvent,
        VirtualNetworkListener, VirtualNetworkProvider, VirtualNetworkProviderService>
        implements VirtualNetworkService, VirtualNetworkAdminService, VirtualNetworkProviderRegistry {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private static final String TENANT_NULL = "Tenant ID cannot be null";
    private static final String NETWORK_NULL = "Network ID cannot be null";
    private static final String DEVICE_NULL = "Device ID cannot be null";
    private static final String LINK_POINT_NULL = "Link end-point cannot be null";

    private static final String VIRTUAL_NETWORK_APP_ID_STRING =
            "org.onosproject.virtual-network";

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected VirtualNetworkStore store;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected CoreService coreService;

    private VirtualNetworkStoreDelegate delegate = this::post;

    private ServiceDirectory serviceDirectory = new DefaultServiceDirectory();
    private ApplicationId appId;

    // TODO: figure out how to coordinate "implementation" of a virtual network in a cluster

    /**
     * Only used for Junit test methods outside of this package.
     *
     * @param store virtual network store
     */
    public void setStore(VirtualNetworkStore store) {
        this.store = store;
    }

    @Activate
    public void activate() {
        eventDispatcher.addSink(VirtualNetworkEvent.class, listenerRegistry);
        eventDispatcher.addSink(VirtualEvent.class,
                                VirtualListenerRegistryManager.getInstance());
        store.setDelegate(delegate);
        appId = coreService.registerApplication(VIRTUAL_NETWORK_APP_ID_STRING);
        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        store.unsetDelegate(delegate);
        eventDispatcher.removeSink(VirtualNetworkEvent.class);
        eventDispatcher.removeSink(VirtualEvent.class);
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
    public VirtualHost createVirtualHost(NetworkId networkId, HostId hostId,
                                         MacAddress mac, VlanId vlan,
                                         HostLocation location, Set<IpAddress> ips) {
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
        ConnectPoint physicalSrc = mapVirtualToPhysicalPort(networkId, src);
        checkNotNull(physicalSrc, LINK_POINT_NULL);
        ConnectPoint physicalDst = mapVirtualToPhysicalPort(networkId, dst);
        checkNotNull(physicalDst, LINK_POINT_NULL);

        VirtualNetworkProvider provider = getProvider(DefaultVirtualLink.PID);
        Link.State state = Link.State.INACTIVE;
        if (provider != null) {
            boolean traversable = provider.isTraversable(physicalSrc, physicalDst);
            state = traversable ? Link.State.ACTIVE : Link.State.INACTIVE;
        }
        return store.addLink(networkId, src, dst, state, null);
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
            if (port.number().equals(virtualCp.port())) {
                return new ConnectPoint(port.realizedBy().deviceId(),
                                        port.realizedBy().port());
            }
        }
        return null;
    }

    /**
     * Maps the physical connect point to a virtual connect point.
     *
     * @param networkId  network identifier
     * @param physicalCp physical connect point
     * @return virtual connect point
     */
    private ConnectPoint mapPhysicalToVirtualToPort(NetworkId networkId,
                                                    ConnectPoint physicalCp) {
        Set<VirtualPort> ports = store.getPorts(networkId, null);
        for (VirtualPort port : ports) {
            if (port.realizedBy().deviceId().equals(physicalCp.elementId()) &&
                    port.realizedBy().port().equals(physicalCp.port())) {
                return new ConnectPoint(port.element().id(), port.number());
            }
        }
        return null;
    }

    @Override
    public void removeVirtualLink(NetworkId networkId, ConnectPoint src,
                                  ConnectPoint dst) {
        checkNotNull(networkId, NETWORK_NULL);
        checkNotNull(src, LINK_POINT_NULL);
        checkNotNull(dst, LINK_POINT_NULL);
        store.removeLink(networkId, src, dst);
    }

    @Override
    public VirtualPort createVirtualPort(NetworkId networkId, DeviceId deviceId,
                                         PortNumber portNumber, ConnectPoint realizedBy) {
        checkNotNull(networkId, NETWORK_NULL);
        checkNotNull(deviceId, DEVICE_NULL);
        checkNotNull(portNumber, "Port description cannot be null");
        return store.addPort(networkId, deviceId, portNumber, realizedBy);
    }

    @Override
    public void bindVirtualPort(NetworkId networkId, DeviceId deviceId,
                PortNumber portNumber, ConnectPoint realizedBy) {
        checkNotNull(networkId, NETWORK_NULL);
        checkNotNull(deviceId, DEVICE_NULL);
        checkNotNull(portNumber, "Port description cannot be null");
        checkNotNull(realizedBy, "Physical port description cannot be null");

        store.bindPort(networkId, deviceId, portNumber, realizedBy);
    }

    @Override
    public void updatePortState(NetworkId networkId, DeviceId deviceId,
                PortNumber portNumber, boolean isEnabled) {
        checkNotNull(networkId, NETWORK_NULL);
        checkNotNull(deviceId, DEVICE_NULL);
        checkNotNull(portNumber, "Port description cannot be null");

        store.updatePortState(networkId, deviceId, portNumber, isEnabled);
    }

    @Override
    public void removeVirtualPort(NetworkId networkId, DeviceId deviceId,
                                  PortNumber portNumber) {
        checkNotNull(networkId, NETWORK_NULL);
        checkNotNull(deviceId, DEVICE_NULL);
        checkNotNull(portNumber, "Port number cannot be null");
        store.removePort(networkId, deviceId, portNumber);
    }

    @Override
    public ServiceDirectory getServiceDirectory() {
        return serviceDirectory;
    }

    @Override
    public Set<VirtualNetwork> getVirtualNetworks(TenantId tenantId) {
        checkNotNull(tenantId, TENANT_NULL);
        return store.getNetworks(tenantId);
    }

    @Override
    public VirtualNetwork getVirtualNetwork(NetworkId networkId) {
        checkNotNull(networkId, NETWORK_NULL);
        return store.getNetwork(networkId);
    }

    @Override
    public TenantId getTenantId(NetworkId networkId) {
        VirtualNetwork virtualNetwork = getVirtualNetwork(networkId);
        checkNotNull(virtualNetwork, "The network does not exist.");
        return virtualNetwork.tenantId();
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

    @Override
    public Set<DeviceId> getPhysicalDevices(NetworkId networkId, DeviceId deviceId) {
        checkNotNull(networkId, "Network ID cannot be null");
        checkNotNull(deviceId, "Virtual device ID cannot be null");
        Set<VirtualPort> virtualPortSet = getVirtualPorts(networkId, deviceId);
        Set<DeviceId> physicalDeviceSet = new HashSet<>();

        virtualPortSet.forEach(virtualPort -> {
            if (virtualPort.realizedBy() != null) {
                physicalDeviceSet.add(virtualPort.realizedBy().deviceId());
            }
        });

        return ImmutableSet.copyOf(physicalDeviceSet);
    }

    private final Map<ServiceKey, VnetService> networkServices = Maps.newConcurrentMap();

    @Override
    @SuppressWarnings("unchecked")
    public <T> T get(NetworkId networkId, Class<T> serviceClass) {
        checkNotNull(networkId, NETWORK_NULL);
        ServiceKey serviceKey = networkServiceKey(networkId, serviceClass);
        VnetService service = lookup(serviceKey);
        if (service == null) {
            service = create(serviceKey);
        }
        return (T) service;
    }

    @Override
    public ApplicationId getVirtualNetworkApplicationId(NetworkId networkId) {
        return appId;
    }

    /**
     * Returns the Vnet service matching the service key.
     *
     * @param serviceKey service key
     * @return vnet service
     */
    private VnetService lookup(ServiceKey serviceKey) {
        return networkServices.get(serviceKey);
    }

    /**
     * Creates a new service key using the specified network identifier and service class.
     *
     * @param networkId    network identifier
     * @param serviceClass service class
     * @param <T>          type of service
     * @return service key
     */
    private <T> ServiceKey networkServiceKey(NetworkId networkId, Class<T> serviceClass) {
        return new ServiceKey(networkId, serviceClass);
    }


    /**
     * Create a new vnet service instance.
     *
     * @param serviceKey service key
     * @return vnet service
     */
    private VnetService create(ServiceKey serviceKey) {
        VirtualNetwork network = getVirtualNetwork(serviceKey.networkId());
        checkNotNull(network, NETWORK_NULL);

        VnetService service;
        if (serviceKey.serviceClass.equals(DeviceService.class)) {
            service = new VirtualNetworkDeviceManager(this, network.id());
        } else if (serviceKey.serviceClass.equals(LinkService.class)) {
            service = new VirtualNetworkLinkManager(this, network.id());
        } else if (serviceKey.serviceClass.equals(TopologyService.class)) {
            service = new VirtualNetworkTopologyManager(this, network.id());
        } else if (serviceKey.serviceClass.equals(IntentService.class)) {
            service = new VirtualNetworkIntentManager(this, network.id());
        } else if (serviceKey.serviceClass.equals(HostService.class)) {
            service = new VirtualNetworkHostManager(this, network.id());
        } else if (serviceKey.serviceClass.equals(PathService.class)) {
            service = new VirtualNetworkPathManager(this, network.id());
        } else if (serviceKey.serviceClass.equals(FlowRuleService.class)) {
            service = new VirtualNetworkFlowRuleManager(this, network.id());
        } else if (serviceKey.serviceClass.equals(PacketService.class)) {
            service = new VirtualNetworkPacketManager(this, network.id());
        } else if (serviceKey.serviceClass.equals(GroupService.class)) {
            service = new VirtualNetworkGroupManager(this, network.id());
        } else if (serviceKey.serviceClass.equals(MeterService.class)) {
            service = new VirtualNetworkMeterManager(this, network.id());
        } else if (serviceKey.serviceClass.equals(FlowObjectiveService.class)) {
            service = new VirtualNetworkFlowObjectiveManager(this, network.id());
        } else if (serviceKey.serviceClass.equals(MastershipService.class) ||
                serviceKey.serviceClass.equals(MastershipAdminService.class) ||
                serviceKey.serviceClass.equals(MastershipTermService.class)) {
            service = new VirtualNetworkMastershipManager(this, network.id());
        } else {
            return null;
        }
        networkServices.put(serviceKey, service);
        return service;
    }

    /**
     * Service key class.
     */
    private static class ServiceKey {
        final NetworkId networkId;
        final Class serviceClass;

        /**
         * Constructor for service key.
         *
         * @param networkId    network identifier
         * @param serviceClass service class
         */
        ServiceKey(NetworkId networkId, Class serviceClass) {

            checkNotNull(networkId, NETWORK_NULL);
            this.networkId = networkId;
            this.serviceClass = serviceClass;
        }

        /**
         * Returns the network identifier.
         *
         * @return network identifier
         */
        public NetworkId networkId() {
            return networkId;
        }

        /**
         * Returns the service class.
         *
         * @return service class
         */
        public Class serviceClass() {
            return serviceClass;
        }

        @Override
        public int hashCode() {
            return Objects.hash(networkId, serviceClass);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj instanceof ServiceKey) {
                ServiceKey that = (ServiceKey) obj;
                return Objects.equals(this.networkId, that.networkId) &&
                        Objects.equals(this.serviceClass, that.serviceClass);
            }
            return false;
        }
    }

    @Override
    protected VirtualNetworkProviderService
    createProviderService(VirtualNetworkProvider provider) {
        return new InternalVirtualNetworkProviderService(provider);
    }

    /**
     * Service issued to registered virtual network providers so that they
     * can interact with the core.
     */
    private class InternalVirtualNetworkProviderService
            extends AbstractProviderService<VirtualNetworkProvider>
            implements VirtualNetworkProviderService {
        /**
         * Constructor.
         * @param provider virtual network provider
         */
        InternalVirtualNetworkProviderService(VirtualNetworkProvider provider) {
            super(provider);
        }

        @Override
        public void topologyChanged(Set<Set<ConnectPoint>> clusters) {
            Set<TenantId> tenantIds = getTenantIds();
            tenantIds.forEach(tenantId -> {
                Set<VirtualNetwork> virtualNetworks = getVirtualNetworks(tenantId);

                virtualNetworks.forEach(virtualNetwork -> {
                    Set<VirtualLink> virtualLinks = getVirtualLinks(virtualNetwork.id());

                    virtualLinks.forEach(virtualLink -> {
                        if (isVirtualLinkInCluster(virtualNetwork.id(),
                                                   virtualLink, clusters)) {
                            store.updateLink(virtualLink, virtualLink.tunnelId(),
                                             Link.State.ACTIVE);
                        } else {
                            store.updateLink(virtualLink, virtualLink.tunnelId(),
                                             Link.State.INACTIVE);
                        }
                    });
                });
            });
        }

        /**
         * Determines if the virtual link (both source and destination connect point)
         * is in a cluster.
         *
         * @param networkId   virtual network identifier
         * @param virtualLink virtual link
         * @param clusters    topology clusters
         * @return true if the virtual link is in a cluster.
         */
        private boolean isVirtualLinkInCluster(NetworkId networkId, VirtualLink virtualLink,
                                               Set<Set<ConnectPoint>> clusters) {
            ConnectPoint srcPhysicalCp =
                    mapVirtualToPhysicalPort(networkId, virtualLink.src());
            ConnectPoint dstPhysicalCp =
                    mapVirtualToPhysicalPort(networkId, virtualLink.dst());

            final boolean[] foundSrc = {false};
            final boolean[] foundDst = {false};
            clusters.forEach(connectPoints -> {
                connectPoints.forEach(connectPoint -> {
                    if (connectPoint.equals(srcPhysicalCp)) {
                        foundSrc[0] = true;
                    } else if (connectPoint.equals(dstPhysicalCp)) {
                        foundDst[0] = true;
                    }
                });
                if (foundSrc[0] && foundDst[0]) {
                    return;
                }
            });
            return foundSrc[0] && foundDst[0];
        }

        @Override
        public void tunnelUp(NetworkId networkId, ConnectPoint src,
                             ConnectPoint dst, TunnelId tunnelId) {
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
        public void tunnelDown(NetworkId networkId, ConnectPoint src,
                               ConnectPoint dst, TunnelId tunnelId) {
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

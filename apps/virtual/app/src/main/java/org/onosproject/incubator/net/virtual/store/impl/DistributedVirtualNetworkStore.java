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
package org.onosproject.incubator.net.virtual.store.impl;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import org.onlab.packet.IpAddress;
import org.onlab.packet.MacAddress;
import org.onlab.packet.VlanId;
import org.onlab.util.KryoNamespace;
import org.onosproject.core.CoreService;
import org.onosproject.core.IdGenerator;
import org.onosproject.incubator.net.tunnel.TunnelId;
import org.onosproject.incubator.net.virtual.DefaultVirtualDevice;
import org.onosproject.incubator.net.virtual.DefaultVirtualHost;
import org.onosproject.incubator.net.virtual.DefaultVirtualLink;
import org.onosproject.incubator.net.virtual.DefaultVirtualNetwork;
import org.onosproject.incubator.net.virtual.DefaultVirtualPort;
import org.onosproject.incubator.net.virtual.NetworkId;
import org.onosproject.net.TenantId;
import org.onosproject.incubator.net.virtual.VirtualDevice;
import org.onosproject.incubator.net.virtual.VirtualHost;
import org.onosproject.incubator.net.virtual.VirtualLink;
import org.onosproject.incubator.net.virtual.VirtualNetwork;
import org.onosproject.incubator.net.virtual.VirtualNetworkEvent;
import org.onosproject.incubator.net.virtual.VirtualNetworkIntent;
import org.onosproject.incubator.net.virtual.VirtualNetworkService;
import org.onosproject.incubator.net.virtual.VirtualNetworkStore;
import org.onosproject.incubator.net.virtual.VirtualNetworkStoreDelegate;
import org.onosproject.incubator.net.virtual.VirtualPort;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.HostId;
import org.onosproject.net.HostLocation;
import org.onosproject.net.Link;
import org.onosproject.net.PortNumber;
import org.onosproject.net.intent.Intent;
import org.onosproject.net.intent.Key;
import org.onosproject.store.AbstractStore;
import org.onosproject.store.serializers.KryoNamespaces;
import org.onosproject.store.service.ConsistentMap;
import org.onosproject.store.service.DistributedSet;
import org.onosproject.store.service.MapEvent;
import org.onosproject.store.service.MapEventListener;
import org.onosproject.store.service.Serializer;
import org.onosproject.store.service.SetEvent;
import org.onosproject.store.service.SetEventListener;
import org.onosproject.store.service.StorageService;
import org.onosproject.store.service.WallClockTimestamp;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.slf4j.Logger;

import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiFunction;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Implementation of the virtual network store.
 */
@Component(immediate = true, service = VirtualNetworkStore.class)
public class DistributedVirtualNetworkStore
        extends AbstractStore<VirtualNetworkEvent, VirtualNetworkStoreDelegate>
        implements VirtualNetworkStore {

    private final Logger log = getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected StorageService storageService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected CoreService coreService;

    private IdGenerator idGenerator;

    // Track tenants by ID
    private DistributedSet<TenantId> tenantIdSet;

    // Listener for tenant events
    private final SetEventListener<TenantId> setListener = new InternalSetListener();

    // Track virtual networks by network Id
    private ConsistentMap<NetworkId, VirtualNetwork> networkIdVirtualNetworkConsistentMap;
    private Map<NetworkId, VirtualNetwork> networkIdVirtualNetworkMap;

    // Listener for virtual network events
    private final MapEventListener<NetworkId, VirtualNetwork> virtualNetworkMapListener =
            new InternalMapListener<>((mapEventType, virtualNetwork) -> {
                VirtualNetworkEvent.Type eventType =
                    mapEventType.equals(MapEvent.Type.INSERT)
                            ? VirtualNetworkEvent.Type.NETWORK_ADDED :
                    mapEventType.equals(MapEvent.Type.UPDATE)
                            ? VirtualNetworkEvent.Type.NETWORK_UPDATED :
                    mapEventType.equals(MapEvent.Type.REMOVE)
                            ? VirtualNetworkEvent.Type.NETWORK_REMOVED : null;
                return eventType == null ? null : new VirtualNetworkEvent(eventType, virtualNetwork.id());
            });

    // Listener for virtual device events
    private final MapEventListener<VirtualDeviceId, VirtualDevice> virtualDeviceMapListener =
            new InternalMapListener<>((mapEventType, virtualDevice) -> {
                VirtualNetworkEvent.Type eventType =
                        mapEventType.equals(MapEvent.Type.INSERT)
                                ? VirtualNetworkEvent.Type.VIRTUAL_DEVICE_ADDED :
                        mapEventType.equals(MapEvent.Type.UPDATE)
                                ? VirtualNetworkEvent.Type.VIRTUAL_DEVICE_UPDATED :
                        mapEventType.equals(MapEvent.Type.REMOVE)
                                ? VirtualNetworkEvent.Type.VIRTUAL_DEVICE_REMOVED : null;
                return eventType == null ? null :
                        new VirtualNetworkEvent(eventType, virtualDevice.networkId(), virtualDevice);
            });

    // Track virtual network IDs by tenant Id
    private ConsistentMap<TenantId, Set<NetworkId>> tenantIdNetworkIdSetConsistentMap;
    private Map<TenantId, Set<NetworkId>> tenantIdNetworkIdSetMap;

    // Track virtual devices by device Id
    private ConsistentMap<VirtualDeviceId, VirtualDevice> deviceIdVirtualDeviceConsistentMap;
    private Map<VirtualDeviceId, VirtualDevice> deviceIdVirtualDeviceMap;

    // Track device IDs by network Id
    private ConsistentMap<NetworkId, Set<DeviceId>> networkIdDeviceIdSetConsistentMap;
    private Map<NetworkId, Set<DeviceId>> networkIdDeviceIdSetMap;

    // Track virtual hosts by host Id
    private ConsistentMap<HostId, VirtualHost> hostIdVirtualHostConsistentMap;
    private Map<HostId, VirtualHost> hostIdVirtualHostMap;

    // Track host IDs by network Id
    private ConsistentMap<NetworkId, Set<HostId>> networkIdHostIdSetConsistentMap;
    private Map<NetworkId, Set<HostId>> networkIdHostIdSetMap;

    // Track virtual links by network Id
    private ConsistentMap<NetworkId, Set<VirtualLink>> networkIdVirtualLinkSetConsistentMap;
    private Map<NetworkId, Set<VirtualLink>> networkIdVirtualLinkSetMap;

    // Track virtual ports by network Id
    private ConsistentMap<NetworkId, Set<VirtualPort>> networkIdVirtualPortSetConsistentMap;
    private Map<NetworkId, Set<VirtualPort>> networkIdVirtualPortSetMap;

    // Track intent ID to TunnelIds
    private ConsistentMap<Key, Set<TunnelId>> intentKeyTunnelIdSetConsistentMap;
    private Map<Key, Set<TunnelId>> intentKeyTunnelIdSetMap;

    private static final Serializer SERIALIZER = Serializer
            .using(new KryoNamespace.Builder().register(KryoNamespaces.API)
                           .register(TenantId.class)
                           .register(NetworkId.class)
                           .register(VirtualNetwork.class)
                           .register(DefaultVirtualNetwork.class)
                           .register(VirtualDevice.class)
                           .register(VirtualDeviceId.class)
                           .register(DefaultVirtualDevice.class)
                           .register(VirtualHost.class)
                           .register(DefaultVirtualHost.class)
                           .register(VirtualLink.class)
                           .register(DefaultVirtualLink.class)
                           .register(VirtualPort.class)
                           .register(DefaultVirtualPort.class)
                           .register(Device.class)
                           .register(TunnelId.class)
                           .register(VirtualNetworkIntent.class)
                           .register(WallClockTimestamp.class)
                           .nextId(KryoNamespaces.BEGIN_USER_CUSTOM_ID)
                           .build());

    /**
     * Distributed network store service activate method.
     */
    @Activate
    public void activate() {
        idGenerator = coreService.getIdGenerator(VirtualNetworkService.VIRTUAL_NETWORK_TOPIC);

        tenantIdSet = storageService.<TenantId>setBuilder()
                .withSerializer(SERIALIZER)
                .withName("onos-tenantId")
                .withRelaxedReadConsistency()
                .build()
                .asDistributedSet();
        tenantIdSet.addListener(setListener);

        networkIdVirtualNetworkConsistentMap = storageService.<NetworkId, VirtualNetwork>consistentMapBuilder()
                .withSerializer(SERIALIZER)
                .withName("onos-networkId-virtualnetwork")
                .withRelaxedReadConsistency()
                .build();
        networkIdVirtualNetworkConsistentMap.addListener(virtualNetworkMapListener);
        networkIdVirtualNetworkMap = networkIdVirtualNetworkConsistentMap.asJavaMap();

        tenantIdNetworkIdSetConsistentMap = storageService.<TenantId, Set<NetworkId>>consistentMapBuilder()
                .withSerializer(SERIALIZER)
                .withName("onos-tenantId-networkIds")
                .withRelaxedReadConsistency()
                .build();
        tenantIdNetworkIdSetMap = tenantIdNetworkIdSetConsistentMap.asJavaMap();

        deviceIdVirtualDeviceConsistentMap = storageService.<VirtualDeviceId, VirtualDevice>consistentMapBuilder()
                .withSerializer(SERIALIZER)
                .withName("onos-deviceId-virtualdevice")
                .withRelaxedReadConsistency()
                .build();
        deviceIdVirtualDeviceConsistentMap.addListener(virtualDeviceMapListener);
        deviceIdVirtualDeviceMap = deviceIdVirtualDeviceConsistentMap.asJavaMap();

        networkIdDeviceIdSetConsistentMap = storageService.<NetworkId, Set<DeviceId>>consistentMapBuilder()
                .withSerializer(SERIALIZER)
                .withName("onos-networkId-deviceIds")
                .withRelaxedReadConsistency()
                .build();
        networkIdDeviceIdSetMap = networkIdDeviceIdSetConsistentMap.asJavaMap();

        hostIdVirtualHostConsistentMap = storageService.<HostId, VirtualHost>consistentMapBuilder()
                .withSerializer(SERIALIZER)
                .withName("onos-hostId-virtualhost")
                .withRelaxedReadConsistency()
                .build();
        hostIdVirtualHostMap = hostIdVirtualHostConsistentMap.asJavaMap();

        networkIdHostIdSetConsistentMap = storageService.<NetworkId, Set<HostId>>consistentMapBuilder()
                .withSerializer(SERIALIZER)
                .withName("onos-networkId-hostIds")
                .withRelaxedReadConsistency()
                .build();
        networkIdHostIdSetMap = networkIdHostIdSetConsistentMap.asJavaMap();

        networkIdVirtualLinkSetConsistentMap = storageService.<NetworkId, Set<VirtualLink>>consistentMapBuilder()
                .withSerializer(SERIALIZER)
                .withName("onos-networkId-virtuallinks")
                .withRelaxedReadConsistency()
                .build();
        networkIdVirtualLinkSetMap = networkIdVirtualLinkSetConsistentMap.asJavaMap();

        networkIdVirtualPortSetConsistentMap = storageService.<NetworkId, Set<VirtualPort>>consistentMapBuilder()
                .withSerializer(SERIALIZER)
                .withName("onos-networkId-virtualports")
                .withRelaxedReadConsistency()
                .build();
        networkIdVirtualPortSetMap = networkIdVirtualPortSetConsistentMap.asJavaMap();

        intentKeyTunnelIdSetConsistentMap = storageService.<Key, Set<TunnelId>>consistentMapBuilder()
                .withSerializer(SERIALIZER)
                .withName("onos-intentKey-tunnelIds")
                .withRelaxedReadConsistency()
                .build();
        intentKeyTunnelIdSetMap = intentKeyTunnelIdSetConsistentMap.asJavaMap();

        log.info("Started");
    }

    /**
     * Distributed network store service deactivate method.
     */
    @Deactivate
    public void deactivate() {
        tenantIdSet.removeListener(setListener);
        networkIdVirtualNetworkConsistentMap.removeListener(virtualNetworkMapListener);
        deviceIdVirtualDeviceConsistentMap.removeListener(virtualDeviceMapListener);
        log.info("Stopped");
    }

    @Override
    public void addTenantId(TenantId tenantId) {
        tenantIdSet.add(tenantId);
    }

    @Override
    public void removeTenantId(TenantId tenantId) {
        //Remove all the virtual networks of this tenant
        Set<VirtualNetwork> networkIdSet = getNetworks(tenantId);
        if (networkIdSet != null) {
            networkIdSet.forEach(virtualNetwork -> removeNetwork(virtualNetwork.id()));
        }

        tenantIdSet.remove(tenantId);
    }

    @Override
    public Set<TenantId> getTenantIds() {
        return ImmutableSet.copyOf(tenantIdSet);
    }

    @Override
    public VirtualNetwork addNetwork(TenantId tenantId) {

        checkState(tenantIdSet.contains(tenantId), "The tenant has not been registered. " + tenantId.id());
        VirtualNetwork virtualNetwork = new DefaultVirtualNetwork(genNetworkId(), tenantId);
        //TODO update both maps in one transaction.
        networkIdVirtualNetworkMap.put(virtualNetwork.id(), virtualNetwork);

        Set<NetworkId> networkIdSet = tenantIdNetworkIdSetMap.get(tenantId);
        if (networkIdSet == null) {
            networkIdSet = new HashSet<>();
        }
        networkIdSet.add(virtualNetwork.id());
        tenantIdNetworkIdSetMap.put(tenantId, networkIdSet);

        return virtualNetwork;
    }

    /**
     * Returns a new network identifier from a virtual network block of identifiers.
     *
     * @return NetworkId network identifier
     */
    private NetworkId genNetworkId() {
        NetworkId networkId;
        do {
            networkId = NetworkId.networkId(idGenerator.getNewId());
        } while (!networkId.isVirtualNetworkId());

        return networkId;
    }

    @Override
    public void removeNetwork(NetworkId networkId) {
        // Make sure that the virtual network exists before attempting to remove it.
        checkState(networkExists(networkId), "The network does not exist.");

        //Remove all the devices of this network
        Set<VirtualDevice> deviceSet = getDevices(networkId);
        if (deviceSet != null) {
            deviceSet.forEach(virtualDevice -> removeDevice(networkId, virtualDevice.id()));
        }
        //TODO update both maps in one transaction.

        VirtualNetwork virtualNetwork = networkIdVirtualNetworkMap.remove(networkId);
        if (virtualNetwork == null) {
            return;
        }
        TenantId tenantId = virtualNetwork.tenantId();

        Set<NetworkId> networkIdSet = new HashSet<>();
        tenantIdNetworkIdSetMap.get(tenantId).forEach(networkId1 -> {
            if (networkId1.id().equals(networkId.id())) {
                networkIdSet.add(networkId1);
            }
        });

        tenantIdNetworkIdSetMap.compute(virtualNetwork.tenantId(), (id, existingNetworkIds) -> {
            if (existingNetworkIds == null || existingNetworkIds.isEmpty()) {
                return new HashSet<>();
            } else {
                return new HashSet<>(Sets.difference(existingNetworkIds, networkIdSet));
            }
        });
    }

    /**
     * Returns if the network identifier exists.
     *
     * @param networkId network identifier
     * @return true if the network identifier exists, false otherwise.
     */
    private boolean networkExists(NetworkId networkId) {
        checkNotNull(networkId, "The network identifier cannot be null.");
        return (networkIdVirtualNetworkMap.containsKey(networkId));
    }

    @Override
    public VirtualDevice addDevice(NetworkId networkId, DeviceId deviceId) {
        checkState(networkExists(networkId), "The network has not been added.");

        Set<DeviceId> deviceIdSet = networkIdDeviceIdSetMap.get(networkId);
        if (deviceIdSet == null) {
            deviceIdSet = new HashSet<>();
        }

        checkState(!deviceIdSet.contains(deviceId), "The device already exists.");

        VirtualDevice virtualDevice = new DefaultVirtualDevice(networkId, deviceId);
        //TODO update both maps in one transaction.
        deviceIdVirtualDeviceMap.put(new VirtualDeviceId(networkId, deviceId), virtualDevice);
        deviceIdSet.add(deviceId);
        networkIdDeviceIdSetMap.put(networkId, deviceIdSet);
        return virtualDevice;
    }

    @Override
    public void removeDevice(NetworkId networkId, DeviceId deviceId) {
        checkState(networkExists(networkId), "The network has not been added.");
        //Remove all the virtual ports of the this device
        Set<VirtualPort> virtualPorts = getPorts(networkId, deviceId);
        if (virtualPorts != null) {
            virtualPorts.forEach(virtualPort -> removePort(networkId, deviceId, virtualPort.number()));
        }
        //TODO update both maps in one transaction.

        Set<DeviceId> deviceIdSet = new HashSet<>();
        networkIdDeviceIdSetMap.get(networkId).forEach(deviceId1 -> {
            if (deviceId1.equals(deviceId)) {
                deviceIdSet.add(deviceId1);
            }
        });

        if (!deviceIdSet.isEmpty()) {
            networkIdDeviceIdSetMap.compute(networkId, (id, existingDeviceIds) -> {
                if (existingDeviceIds == null || existingDeviceIds.isEmpty()) {
                    return new HashSet<>();
                } else {
                    return new HashSet<>(Sets.difference(existingDeviceIds, deviceIdSet));
                }
            });

            deviceIdVirtualDeviceMap.remove(new VirtualDeviceId(networkId, deviceId));
        }
    }

    @Override
    public VirtualHost addHost(NetworkId networkId, HostId hostId, MacAddress mac,
                               VlanId vlan, HostLocation location, Set<IpAddress> ips) {
        checkState(networkExists(networkId), "The network has not been added.");
        checkState(virtualPortExists(networkId, location.deviceId(), location.port()),
                "The virtual port has not been created.");
        Set<HostId> hostIdSet = networkIdHostIdSetMap.get(networkId);
        if (hostIdSet == null) {
            hostIdSet = new HashSet<>();
        }
        VirtualHost virtualhost = new DefaultVirtualHost(networkId, hostId, mac, vlan, location, ips);
        //TODO update both maps in one transaction.
        hostIdVirtualHostMap.put(hostId, virtualhost);
        hostIdSet.add(hostId);
        networkIdHostIdSetMap.put(networkId, hostIdSet);
        return virtualhost;
    }

    @Override
    public void removeHost(NetworkId networkId, HostId hostId) {
        checkState(networkExists(networkId), "The network has not been added.");
        //TODO update both maps in one transaction.

        Set<HostId> hostIdSet = new HashSet<>();
        networkIdHostIdSetMap.get(networkId).forEach(hostId1 -> {
            if (hostId1.equals(hostId)) {
                hostIdSet.add(hostId1);
            }
        });

        networkIdHostIdSetMap.compute(networkId, (id, existingHostIds) -> {
            if (existingHostIds == null || existingHostIds.isEmpty()) {
                return new HashSet<>();
            } else {
                return new HashSet<>(Sets.difference(existingHostIds, hostIdSet));
            }
        });

        hostIdVirtualHostMap.remove(hostId);
    }

    /**
     * Returns if the given virtual port exists.
     *
     * @param networkId network identifier
     * @param deviceId virtual device Id
     * @param portNumber virtual port number
     * @return true if the virtual port exists, false otherwise.
     */
    private boolean virtualPortExists(NetworkId networkId, DeviceId deviceId, PortNumber portNumber) {
        Set<VirtualPort> virtualPortSet = networkIdVirtualPortSetMap.get(networkId);
        if (virtualPortSet != null) {
            return virtualPortSet.stream().anyMatch(
                    p -> p.element().id().equals(deviceId) &&
                            p.number().equals(portNumber));
        } else {
            return false;
        }
    }

    @Override
    public VirtualLink addLink(NetworkId networkId, ConnectPoint src, ConnectPoint dst,
                               Link.State state, TunnelId realizedBy) {
        checkState(networkExists(networkId), "The network has not been added.");
        checkState(virtualPortExists(networkId, src.deviceId(), src.port()),
                "The source virtual port has not been added.");
        checkState(virtualPortExists(networkId, dst.deviceId(), dst.port()),
                "The destination virtual port has not been added.");
        Set<VirtualLink> virtualLinkSet = networkIdVirtualLinkSetMap.get(networkId);
        if (virtualLinkSet == null) {
            virtualLinkSet = new HashSet<>();
        }

        // validate that the link does not already exist in this network
        checkState(getLink(networkId, src, dst) == null,
                "The virtual link already exists");
        checkState(getLink(networkId, src, null) == null,
                "The source connection point has been used by another link");
        checkState(getLink(networkId, null, dst) == null,
                "The destination connection point has been used by another link");

        VirtualLink virtualLink = DefaultVirtualLink.builder()
                .networkId(networkId)
                .src(src)
                .dst(dst)
                .state(state)
                .tunnelId(realizedBy)
                .build();

        virtualLinkSet.add(virtualLink);
        networkIdVirtualLinkSetMap.put(networkId, virtualLinkSet);
        return virtualLink;
    }

    @Override
    public void updateLink(VirtualLink virtualLink, TunnelId tunnelId, Link.State state) {
        checkState(networkExists(virtualLink.networkId()), "The network has not been added.");
        Set<VirtualLink> virtualLinkSet = networkIdVirtualLinkSetMap.get(virtualLink.networkId());
        if (virtualLinkSet == null) {
            virtualLinkSet = new HashSet<>();
            networkIdVirtualLinkSetMap.put(virtualLink.networkId(), virtualLinkSet);
            log.warn("The updated virtual link {} has not been added", virtualLink);
            return;
        }
        if (!virtualLinkSet.remove(virtualLink)) {
            log.warn("The updated virtual link {} does not exist", virtualLink);
            return;
        }

        VirtualLink newVirtualLink = DefaultVirtualLink.builder()
                .networkId(virtualLink.networkId())
                .src(virtualLink.src())
                .dst(virtualLink.dst())
                .tunnelId(tunnelId)
                .state(state)
                .build();

        virtualLinkSet.add(newVirtualLink);
        networkIdVirtualLinkSetMap.put(newVirtualLink.networkId(), virtualLinkSet);
    }

    @Override
    public VirtualLink removeLink(NetworkId networkId, ConnectPoint src, ConnectPoint dst) {
        checkState(networkExists(networkId), "The network has not been added.");

        final VirtualLink virtualLink = getLink(networkId, src, dst);
        if (virtualLink == null) {
            log.warn("The removed virtual link between {} and {} does not exist", src, dst);
            return null;
        }
        Set<VirtualLink> virtualLinkSet = new HashSet<>();
        virtualLinkSet.add(virtualLink);

        networkIdVirtualLinkSetMap.compute(networkId, (id, existingVirtualLinks) -> {
            if (existingVirtualLinks == null || existingVirtualLinks.isEmpty()) {
                return new HashSet<>();
            } else {
                return new HashSet<>(Sets.difference(existingVirtualLinks, virtualLinkSet));
            }
        });
        return virtualLink;
    }

    @Override
    public VirtualPort addPort(NetworkId networkId, DeviceId deviceId,
                               PortNumber portNumber, ConnectPoint realizedBy) {
        checkState(networkExists(networkId), "The network has not been added.");
        Set<VirtualPort> virtualPortSet = networkIdVirtualPortSetMap.get(networkId);

        if (virtualPortSet == null) {
            virtualPortSet = new HashSet<>();
        }

        VirtualDevice device = deviceIdVirtualDeviceMap.get(new VirtualDeviceId(networkId, deviceId));
        checkNotNull(device, "The device has not been created for deviceId: " + deviceId);

        checkState(!virtualPortExists(networkId, deviceId, portNumber),
                "The requested Port Number has been added.");

        VirtualPort virtualPort = new DefaultVirtualPort(networkId, device,
                                                         portNumber, realizedBy);
        virtualPortSet.add(virtualPort);
        networkIdVirtualPortSetMap.put(networkId, virtualPortSet);
        notifyDelegate(new VirtualNetworkEvent(VirtualNetworkEvent.Type.VIRTUAL_PORT_ADDED,
                                               networkId, device, virtualPort));
        return virtualPort;
    }

    @Override
    public void bindPort(NetworkId networkId, DeviceId deviceId,
                         PortNumber portNumber, ConnectPoint realizedBy) {

        Set<VirtualPort> virtualPortSet = networkIdVirtualPortSetMap
                .get(networkId);

        Optional<VirtualPort> virtualPortOptional = virtualPortSet.stream().filter(
                p -> p.element().id().equals(deviceId) &&
                        p.number().equals(portNumber)).findFirst();
        checkState(virtualPortOptional.isPresent(), "The virtual port has not been added.");

        VirtualDevice device = deviceIdVirtualDeviceMap.get(new VirtualDeviceId(networkId, deviceId));
        checkNotNull(device, "The device has not been created for deviceId: "
                + deviceId);

        VirtualPort vPort = virtualPortOptional.get();
        virtualPortSet.remove(vPort);
        vPort = new DefaultVirtualPort(networkId, device, portNumber, realizedBy);
        virtualPortSet.add(vPort);
        networkIdVirtualPortSetMap.put(networkId, virtualPortSet);
        notifyDelegate(new VirtualNetworkEvent(VirtualNetworkEvent.Type.VIRTUAL_PORT_UPDATED,
                                               networkId, device, vPort));
    }

    @Override
    public void updatePortState(NetworkId networkId, DeviceId deviceId,
                                PortNumber portNumber, boolean isEnabled) {
        checkState(networkExists(networkId), "No network with NetworkId %s exists.", networkId);

        VirtualDevice device = deviceIdVirtualDeviceMap.get(new VirtualDeviceId(networkId, deviceId));
        checkNotNull(device, "No device %s exists in NetworkId: %s", deviceId, networkId);

        Set<VirtualPort> virtualPortSet = networkIdVirtualPortSetMap.get(networkId);
        checkNotNull(virtualPortSet, "No port has been created for NetworkId: %s", networkId);

        Optional<VirtualPort> virtualPortOptional = virtualPortSet.stream().filter(
                p -> p.element().id().equals(deviceId) &&
                        p.number().equals(portNumber)).findFirst();
        checkState(virtualPortOptional.isPresent(), "The virtual port has not been added.");

        VirtualPort oldPort = virtualPortOptional.get();
        if (oldPort.isEnabled() == isEnabled) {
            log.debug("No change in port state - port not updated");
            return;
        }
        VirtualPort newPort = new DefaultVirtualPort(networkId, device, portNumber, isEnabled,
                oldPort.realizedBy());
        virtualPortSet.remove(oldPort);
        virtualPortSet.add(newPort);
        networkIdVirtualPortSetMap.put(networkId, virtualPortSet);
        notifyDelegate(new VirtualNetworkEvent(VirtualNetworkEvent.Type.VIRTUAL_PORT_UPDATED,
                                               networkId, device, newPort));
        log.debug("port state changed from {} to {}", oldPort.isEnabled(), isEnabled);
    }

    @Override
    public void removePort(NetworkId networkId, DeviceId deviceId, PortNumber portNumber) {
        checkState(networkExists(networkId), "The network has not been added.");
        VirtualDevice device = deviceIdVirtualDeviceMap.get(new VirtualDeviceId(networkId, deviceId));
        checkNotNull(device, "The device has not been created for deviceId: "
                + deviceId);

        if (networkIdVirtualPortSetMap.get(networkId) == null) {
            log.warn("No port has been created for NetworkId: {}", networkId);
            return;
        }

        Set<VirtualPort> virtualPortSet = new HashSet<>();
        networkIdVirtualPortSetMap.get(networkId).forEach(port -> {
            if (port.element().id().equals(deviceId) && port.number().equals(portNumber)) {
                virtualPortSet.add(port);
            }
        });

        if (!virtualPortSet.isEmpty()) {
            AtomicBoolean portRemoved = new AtomicBoolean(false);
            networkIdVirtualPortSetMap.compute(networkId, (id, existingVirtualPorts) -> {
                if (existingVirtualPorts == null || existingVirtualPorts.isEmpty()) {
                    return new HashSet<>();
                } else {
                    portRemoved.set(true);
                    return new HashSet<>(Sets.difference(existingVirtualPorts, virtualPortSet));
                }
            });
            if (portRemoved.get()) {
                virtualPortSet.forEach(virtualPort -> notifyDelegate(
                        new VirtualNetworkEvent(VirtualNetworkEvent.Type.VIRTUAL_PORT_REMOVED,
                                                networkId, device, virtualPort)
                ));

                //Remove all the virtual links connected to this virtual port
                Set<VirtualLink> existingVirtualLinks = networkIdVirtualLinkSetMap.get(networkId);
                if (existingVirtualLinks != null && !existingVirtualLinks.isEmpty()) {
                    Set<VirtualLink> virtualLinkSet = new HashSet<>();
                    ConnectPoint cp = new ConnectPoint(deviceId, portNumber);
                    existingVirtualLinks.forEach(virtualLink -> {
                        if (virtualLink.src().equals(cp) || virtualLink.dst().equals(cp)) {
                            virtualLinkSet.add(virtualLink);
                        }
                    });
                    virtualLinkSet.forEach(virtualLink ->
                            removeLink(networkId, virtualLink.src(), virtualLink.dst()));
                }

                //Remove all the hosts connected to this virtual port
                Set<HostId> hostIdSet = new HashSet<>();
                hostIdVirtualHostMap.forEach((hostId, virtualHost) -> {
                    if (virtualHost.location().deviceId().equals(deviceId) &&
                            virtualHost.location().port().equals(portNumber)) {
                        hostIdSet.add(hostId);
                    }
                });
                hostIdSet.forEach(hostId -> removeHost(networkId, hostId));
            }
        }
    }

    @Override
    public Set<VirtualNetwork> getNetworks(TenantId tenantId) {
        Set<NetworkId> networkIdSet = tenantIdNetworkIdSetMap.get(tenantId);
        Set<VirtualNetwork> virtualNetworkSet = new HashSet<>();
        if (networkIdSet != null) {
            networkIdSet.forEach(networkId -> {
                if (networkIdVirtualNetworkMap.get(networkId) != null) {
                    virtualNetworkSet.add(networkIdVirtualNetworkMap.get(networkId));
                }
            });
        }
        return ImmutableSet.copyOf(virtualNetworkSet);
    }

    @Override
    public VirtualNetwork getNetwork(NetworkId networkId) {
        return networkIdVirtualNetworkMap.get(networkId);
    }

    @Override
    public Set<VirtualDevice> getDevices(NetworkId networkId) {
        checkState(networkExists(networkId), "The network has not been added.");
        Set<DeviceId> deviceIdSet = networkIdDeviceIdSetMap.get(networkId);
        Set<VirtualDevice> virtualDeviceSet = new HashSet<>();
        if (deviceIdSet != null) {
            deviceIdSet.forEach(deviceId -> virtualDeviceSet.add(
                    deviceIdVirtualDeviceMap.get(new VirtualDeviceId(networkId, deviceId))));
        }
        return ImmutableSet.copyOf(virtualDeviceSet);
    }

    @Override
    public Set<VirtualHost> getHosts(NetworkId networkId) {
        checkState(networkExists(networkId), "The network has not been added.");
        Set<HostId> hostIdSet = networkIdHostIdSetMap.get(networkId);
        Set<VirtualHost> virtualHostSet = new HashSet<>();
        if (hostIdSet != null) {
            hostIdSet.forEach(hostId -> virtualHostSet.add(hostIdVirtualHostMap.get(hostId)));
        }
        return ImmutableSet.copyOf(virtualHostSet);
    }

    @Override
    public Set<VirtualLink> getLinks(NetworkId networkId) {
        checkState(networkExists(networkId), "The network has not been added.");
        Set<VirtualLink> virtualLinkSet = networkIdVirtualLinkSetMap.get(networkId);
        if (virtualLinkSet == null) {
            virtualLinkSet = new HashSet<>();
        }
        return ImmutableSet.copyOf(virtualLinkSet);
    }

    @Override
    public VirtualLink getLink(NetworkId networkId, ConnectPoint src, ConnectPoint dst) {
        Set<VirtualLink> virtualLinkSet = networkIdVirtualLinkSetMap.get(networkId);
        if (virtualLinkSet == null) {
            return null;
        }

        VirtualLink virtualLink = null;
        for (VirtualLink link : virtualLinkSet) {
            if (src == null && link.dst().equals(dst)) {
                virtualLink = link;
                break;
            } else if (dst == null && link.src().equals(src)) {
                virtualLink = link;
                break;
            } else if (link.src().equals(src) && link.dst().equals(dst)) {
                virtualLink = link;
                break;
            }
        }
        return virtualLink;
    }

    @Override
    public Set<VirtualPort> getPorts(NetworkId networkId, DeviceId deviceId) {
        checkState(networkExists(networkId), "The network has not been added.");
        Set<VirtualPort> virtualPortSet = networkIdVirtualPortSetMap.get(networkId);
        if (virtualPortSet == null) {
            virtualPortSet = new HashSet<>();
        }

        if (deviceId == null) {
            return ImmutableSet.copyOf(virtualPortSet);
        }

        Set<VirtualPort> portSet = new HashSet<>();
        virtualPortSet.forEach(virtualPort -> {
            if (virtualPort.element().id().equals(deviceId)) {
                portSet.add(virtualPort);
            }
        });
        return ImmutableSet.copyOf(portSet);
    }

    @Override
    public void addTunnelId(Intent intent, TunnelId tunnelId) {
        // Add the tunnelId to the intent key set map
        Set<TunnelId> tunnelIdSet = intentKeyTunnelIdSetMap.remove(intent.key());
        if (tunnelIdSet == null) {
            tunnelIdSet = new HashSet<>();
        }
        tunnelIdSet.add(tunnelId);
        intentKeyTunnelIdSetMap.put(intent.key(), tunnelIdSet);
    }

    @Override
    public Set<TunnelId> getTunnelIds(Intent intent) {
        Set<TunnelId> tunnelIdSet = intentKeyTunnelIdSetMap.get(intent.key());
        return tunnelIdSet == null ? new HashSet<TunnelId>() : ImmutableSet.copyOf(tunnelIdSet);
    }

    @Override
    public void removeTunnelId(Intent intent, TunnelId tunnelId) {
        Set<TunnelId> tunnelIdSet = new HashSet<>();
        intentKeyTunnelIdSetMap.get(intent.key()).forEach(tunnelId1 -> {
            if (tunnelId1.equals(tunnelId)) {
                tunnelIdSet.add(tunnelId);
            }
        });

        if (!tunnelIdSet.isEmpty()) {
            intentKeyTunnelIdSetMap.compute(intent.key(), (key, existingTunnelIds) -> {
                if (existingTunnelIds == null || existingTunnelIds.isEmpty()) {
                    return new HashSet<>();
                } else {
                    return new HashSet<>(Sets.difference(existingTunnelIds, tunnelIdSet));
                }
            });
        }
    }

    /**
     * Listener class to map listener set events to the virtual network events.
     */
    private class InternalSetListener implements SetEventListener<TenantId> {
        @Override
        public void event(SetEvent<TenantId> event) {
            VirtualNetworkEvent.Type type = null;
            switch (event.type()) {
                case ADD:
                    type = VirtualNetworkEvent.Type.TENANT_REGISTERED;
                    break;
                case REMOVE:
                    type = VirtualNetworkEvent.Type.TENANT_UNREGISTERED;
                    break;
                default:
                    log.error("Unsupported event type: " + event.type());
            }
            notifyDelegate(new VirtualNetworkEvent(type, null));
        }
    }

    /**
     * Listener class to map listener map events to the virtual network events.
     */
    private class InternalMapListener<K, V> implements MapEventListener<K, V> {

        private final BiFunction<MapEvent.Type, V, VirtualNetworkEvent> createEvent;

        InternalMapListener(BiFunction<MapEvent.Type, V, VirtualNetworkEvent> createEvent) {
            this.createEvent = createEvent;
        }

        @Override
        public void event(MapEvent<K, V> event) {
            checkNotNull(event.key());
            VirtualNetworkEvent vnetEvent = null;
            switch (event.type()) {
                case INSERT:
                    vnetEvent = createEvent.apply(event.type(), event.newValue().value());
                    break;
                case UPDATE:
                    if ((event.oldValue().value() != null) && (event.newValue().value() == null)) {
                        vnetEvent = createEvent.apply(MapEvent.Type.REMOVE, event.oldValue().value());
                    } else {
                        vnetEvent = createEvent.apply(event.type(), event.newValue().value());
                    }
                    break;
                case REMOVE:
                    if (event.oldValue() != null) {
                        vnetEvent = createEvent.apply(event.type(), event.oldValue().value());
                    }
                    break;
                default:
                    log.error("Unsupported event type: " + event.type());
            }
            if (vnetEvent != null) {
                notifyDelegate(vnetEvent);
            }
        }
    }

    /**
     * A wrapper class to isolate device id from other virtual networks.
     */

    private static class VirtualDeviceId {

        NetworkId networkId;
        DeviceId deviceId;

        public VirtualDeviceId(NetworkId networkId, DeviceId deviceId) {
            this.networkId = networkId;
            this.deviceId = deviceId;
        }

        public NetworkId getNetworkId() {
            return networkId;
        }

        public DeviceId getDeviceId() {
            return deviceId;
        }

        @Override
        public int hashCode() {
            return Objects.hash(networkId, deviceId);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }

            if (obj instanceof VirtualDeviceId) {
                VirtualDeviceId that = (VirtualDeviceId) obj;
                return this.deviceId.equals(that.deviceId) &&
                        this.networkId.equals(that.networkId);
            }
            return false;
        }
    }
}

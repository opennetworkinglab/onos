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
package org.onosproject.incubator.store.virtual.impl;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
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
import org.onosproject.incubator.net.virtual.TenantId;
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
import org.onosproject.net.Port;
import org.onosproject.net.PortNumber;
import org.onosproject.net.intent.Intent;
import org.onosproject.net.intent.IntentData;
import org.onosproject.net.intent.IntentState;
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
import org.slf4j.Logger;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Implementation of the virtual network store.
 */
@Component(immediate = true)
@Service
public class DistributedVirtualNetworkStore
        extends AbstractStore<VirtualNetworkEvent, VirtualNetworkStoreDelegate>
        implements VirtualNetworkStore {

    private final Logger log = getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected StorageService storageService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
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
    private final MapEventListener<NetworkId, VirtualNetwork> virtualMapListener = new InternalMapListener();

    // Track virtual network IDs by tenant Id
    private ConsistentMap<TenantId, Set<NetworkId>> tenantIdNetworkIdSetConsistentMap;
    private Map<TenantId, Set<NetworkId>> tenantIdNetworkIdSetMap;

    // Track virtual devices by device Id
    private ConsistentMap<DeviceId, VirtualDevice> deviceIdVirtualDeviceConsistentMap;
    private Map<DeviceId, VirtualDevice> deviceIdVirtualDeviceMap;

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

    // Track intent key to intent data
    private ConsistentMap<Key, IntentData> intentKeyIntentDataConsistentMap;
    private Map<Key, IntentData> intentKeyIntentDataMap;

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
                           .register(DefaultVirtualDevice.class)
                           .register(VirtualHost.class)
                           .register(DefaultVirtualHost.class)
                           .register(VirtualLink.class)
                           .register(DefaultVirtualLink.class)
                           .register(VirtualPort.class)
                           .register(DefaultVirtualPort.class)
                           .register(Device.class)
                           .register(TunnelId.class)
                           .register(IntentData.class)
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
        networkIdVirtualNetworkConsistentMap.addListener(virtualMapListener);
        networkIdVirtualNetworkMap = networkIdVirtualNetworkConsistentMap.asJavaMap();

        tenantIdNetworkIdSetConsistentMap = storageService.<TenantId, Set<NetworkId>>consistentMapBuilder()
                .withSerializer(SERIALIZER)
                .withName("onos-tenantId-networkIds")
                .withRelaxedReadConsistency()
                .build();
        tenantIdNetworkIdSetMap = tenantIdNetworkIdSetConsistentMap.asJavaMap();

        deviceIdVirtualDeviceConsistentMap = storageService.<DeviceId, VirtualDevice>consistentMapBuilder()
                .withSerializer(SERIALIZER)
                .withName("onos-deviceId-virtualdevice")
                .withRelaxedReadConsistency()
                .build();
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

        intentKeyIntentDataConsistentMap = storageService.<Key, IntentData>consistentMapBuilder()
                .withSerializer(SERIALIZER)
                .withName("onos-intentKey-intentData")
                .withRelaxedReadConsistency()
                .build();
        intentKeyIntentDataMap = intentKeyIntentDataConsistentMap.asJavaMap();

        log.info("Started");
    }

    /**
     * Distributed network store service deactivate method.
     */
    @Deactivate
    public void deactivate() {
        tenantIdSet.removeListener(setListener);
        networkIdVirtualNetworkConsistentMap.removeListener(virtualMapListener);
        log.info("Stopped");
    }

    /**
     * This method is used for Junit tests to set the CoreService instance, which
     * is required to set the IdGenerator instance.
     *
     * @param coreService core service instance
     */
    public void setCoreService(CoreService coreService) {
        this.coreService = coreService;
    }

    @Override
    public void addTenantId(TenantId tenantId) {
        tenantIdSet.add(tenantId);
    }

    @Override
    public void removeTenantId(TenantId tenantId) {
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
        if (networkExists(networkId)) {
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
        VirtualDevice virtualDevice = new DefaultVirtualDevice(networkId, deviceId);
        //TODO update both maps in one transaction.
        deviceIdVirtualDeviceMap.put(deviceId, virtualDevice);
        deviceIdSet.add(deviceId);
        networkIdDeviceIdSetMap.put(networkId, deviceIdSet);
        return virtualDevice;
    }

    @Override
    public void removeDevice(NetworkId networkId, DeviceId deviceId) {
        checkState(networkExists(networkId), "The network has not been added.");
        //TODO update both maps in one transaction.

        Set<DeviceId> deviceIdSet = new HashSet<>();
        networkIdDeviceIdSetMap.get(networkId).forEach(deviceId1 -> {
            if (deviceId1.equals(deviceId)) {
                deviceIdSet.add(deviceId1);
            }
        });

        if (deviceIdSet != null) {
            networkIdDeviceIdSetMap.compute(networkId, (id, existingDeviceIds) -> {
                if (existingDeviceIds == null || existingDeviceIds.isEmpty()) {
                    return new HashSet<>();
                } else {
                    return new HashSet<>(Sets.difference(existingDeviceIds, deviceIdSet));
                }
            });

            deviceIdVirtualDeviceMap.remove(deviceId);
        }
        //TODO remove virtual links and ports when removing the virtual device
    }

    @Override
    public VirtualHost addHost(NetworkId networkId, HostId hostId, MacAddress mac,
                               VlanId vlan, HostLocation location, Set<IpAddress> ips) {
        checkState(networkExists(networkId), "The network has not been added.");
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

        if (hostIdSet != null) {
            networkIdHostIdSetMap.compute(networkId, (id, existingHostIds) -> {
                if (existingHostIds == null || existingHostIds.isEmpty()) {
                    return new HashSet<>();
                } else {
                    return new HashSet<>(Sets.difference(existingHostIds, hostIdSet));
                }
            });

            hostIdVirtualHostMap.remove(hostId);
        }
    }

    @Override
    public VirtualLink addLink(NetworkId networkId, ConnectPoint src, ConnectPoint dst,
                               Link.State state, TunnelId realizedBy) {
        checkState(networkExists(networkId), "The network has not been added.");
        Set<VirtualLink> virtualLinkSet = networkIdVirtualLinkSetMap.get(networkId);
        if (virtualLinkSet == null) {
            virtualLinkSet = new HashSet<>();
        }
        // validate that the link does not already exist in this network
        checkState(getLink(networkId, src, dst) == null, "The virtual link already exists");

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
        }
        virtualLinkSet.remove(virtualLink);

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
            return null;
        }
        Set<VirtualLink> virtualLinkSet = new HashSet<>();
        virtualLinkSet.add(virtualLink);

        if (virtualLinkSet != null) {
            networkIdVirtualLinkSetMap.compute(networkId, (id, existingVirtualLinks) -> {
                if (existingVirtualLinks == null || existingVirtualLinks.isEmpty()) {
                    return new HashSet<>();
                } else {
                    return new HashSet<>(Sets.difference(existingVirtualLinks, virtualLinkSet));
                }
            });
        }
        return virtualLink;
    }

    @Override
    public VirtualPort addPort(NetworkId networkId, DeviceId deviceId, PortNumber portNumber, Port realizedBy) {
        checkState(networkExists(networkId), "The network has not been added.");
        Set<VirtualPort> virtualPortSet = networkIdVirtualPortSetMap.get(networkId);
        if (virtualPortSet == null) {
            virtualPortSet = new HashSet<>();
        }
        Device device = deviceIdVirtualDeviceMap.get(deviceId);
        checkNotNull(device, "The device has not been created for deviceId: " + deviceId);
        VirtualPort virtualPort = new DefaultVirtualPort(networkId, device, portNumber, realizedBy);
        virtualPortSet.add(virtualPort);
        networkIdVirtualPortSetMap.put(networkId, virtualPortSet);
        return virtualPort;
    }

    @Override
    public void removePort(NetworkId networkId, DeviceId deviceId, PortNumber portNumber) {
        checkState(networkExists(networkId), "The network has not been added.");

        Set<VirtualPort> virtualPortSet = new HashSet<>();
        networkIdVirtualPortSetMap.get(networkId).forEach(port -> {
            if (port.element().id().equals(deviceId) && port.number().equals(portNumber)) {
                virtualPortSet.add(port);
            }
        });

        if (virtualPortSet != null) {
            networkIdVirtualPortSetMap.compute(networkId, (id, existingVirtualPorts) -> {
                if (existingVirtualPorts == null || existingVirtualPorts.isEmpty()) {
                    return new HashSet<>();
                } else {
                    return new HashSet<>(Sets.difference(existingVirtualPorts, virtualPortSet));
                }
            });
        }
    }

    @Override
    public Set<VirtualNetwork> getNetworks(TenantId tenantId) {
        Set<NetworkId> networkIdSet = tenantIdNetworkIdSetMap.get(tenantId);
        Set<VirtualNetwork> virtualNetworkSet = new HashSet<>();
        if (networkIdSet != null) {
            networkIdSet.forEach(networkId -> virtualNetworkSet.add(networkIdVirtualNetworkMap.get(networkId)));
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
            deviceIdSet.forEach(deviceId -> virtualDeviceSet.add(deviceIdVirtualDeviceMap.get(deviceId)));
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
            if (link.src().equals(src) && link.dst().equals(dst)) {
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
    public synchronized void addOrUpdateIntent(Intent intent, IntentState state) {
        checkNotNull(intent, "Intent cannot be null");
        IntentData intentData = removeIntent(intent.key());
        if (intentData == null) {
            intentData = new IntentData(intent, state, new WallClockTimestamp(System.currentTimeMillis()));
        } else {
            intentData = new IntentData(intent, state, intentData.version());
        }
        intentKeyIntentDataMap.put(intent.key(), intentData);
    }

    @Override
    public IntentData removeIntent(Key intentKey) {
        checkNotNull(intentKey, "Intent key cannot be null");
        return intentKeyIntentDataMap.remove(intentKey);
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

    @Override
    public Set<Intent> getIntents() {
        Set<Intent> intents = new HashSet<>();
        intentKeyIntentDataMap.values().forEach(intentData -> intents.add(intentData.intent()));
        return ImmutableSet.copyOf(intents);
    }

    @Override
    public Intent getIntent(Key key) {
        IntentData intentData = intentKeyIntentDataMap.get(key);
        return intentData == null ? null : intentData.intent();
    }

    @Override
    public Set<IntentData> getIntentData() {
        return ImmutableSet.copyOf(intentKeyIntentDataMap.values());
    }

    @Override
    public IntentData getIntentData(Key key) {
        IntentData intentData = intentKeyIntentDataMap.get(key);
        return intentData ==  null ? null : new IntentData(intentData);
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
    private class InternalMapListener implements MapEventListener<NetworkId, VirtualNetwork> {
        @Override
        public void event(MapEvent<NetworkId, VirtualNetwork> event) {
            NetworkId networkId = checkNotNull(event.key());
            VirtualNetworkEvent.Type type = null;
            switch (event.type()) {
                case INSERT:
                    type = VirtualNetworkEvent.Type.NETWORK_ADDED;
                    break;
                case UPDATE:
                    if ((event.oldValue().value() != null) && (event.newValue().value() == null)) {
                        type = VirtualNetworkEvent.Type.NETWORK_REMOVED;
                    } else {
                        type = VirtualNetworkEvent.Type.NETWORK_UPDATED;
                    }
                    break;
                case REMOVE:
                    type = VirtualNetworkEvent.Type.NETWORK_REMOVED;
                    break;
                default:
                    log.error("Unsupported event type: " + event.type());
            }
            notifyDelegate(new VirtualNetworkEvent(type, networkId));
        }
    }
}

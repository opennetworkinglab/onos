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
package org.onosproject.l3vpn.netl3vpn.impl;

import com.google.common.collect.ImmutableSet;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onlab.util.KryoNamespace;
import org.onosproject.l3vpn.netl3vpn.AccessInfo;
import org.onosproject.l3vpn.netl3vpn.BgpInfo;
import org.onosproject.l3vpn.netl3vpn.DeviceInfo;
import org.onosproject.l3vpn.netl3vpn.FullMeshVpnConfig;
import org.onosproject.l3vpn.netl3vpn.HubSpokeVpnConfig;
import org.onosproject.l3vpn.netl3vpn.InterfaceInfo;
import org.onosproject.l3vpn.netl3vpn.NetL3VpnStore;
import org.onosproject.l3vpn.netl3vpn.ProtocolInfo;
import org.onosproject.l3vpn.netl3vpn.RouteProtocol;
import org.onosproject.l3vpn.netl3vpn.VpnConfig;
import org.onosproject.l3vpn.netl3vpn.VpnInstance;
import org.onosproject.l3vpn.netl3vpn.VpnType;
import org.onosproject.net.DeviceId;
import org.onosproject.store.serializers.KryoNamespaces;
import org.onosproject.store.service.ConsistentMap;
import org.onosproject.store.service.DistributedSet;
import org.onosproject.store.service.Serializer;
import org.onosproject.store.service.StorageService;
import org.onosproject.yang.model.LeafListKey;
import org.onosproject.yang.model.ListKey;
import org.onosproject.yang.model.NodeKey;
import org.onosproject.yang.model.ResourceId;
import org.onosproject.yang.model.SchemaId;
import org.slf4j.Logger;

import java.util.Map;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Manages the pool of available VPN instances and its associated devices
 * and interface information.
 */
@Component(immediate = true)
@Service
public class DistributedNetL3VpnStore implements NetL3VpnStore {

    private static final Serializer L3VPN_SERIALIZER = Serializer
            .using(new KryoNamespace.Builder().register(KryoNamespaces.API)
                           .register(KryoNamespaces.API)
                           .register(VpnInstance.class)
                           .register(VpnType.class)
                           .register(VpnConfig.class)
                           .register(FullMeshVpnConfig.class)
                           .register(HubSpokeVpnConfig.class)
                           .register(DeviceInfo.class)
                           .register(ResourceId.class)
                           .register(NodeKey.class)
                           .register(SchemaId.class)
                           .register(LeafListKey.class)
                           .register(ListKey.class)
                           .register(AccessInfo.class)
                           .register(InterfaceInfo.class)
                           .register(BgpInfo.class)
                           .register(RouteProtocol.class)
                           .register(ProtocolInfo.class)
                           .build());

    private static final String FREE_ID_NULL = "Free ID cannot be null";
    private static final String VPN_NAME_NULL = "VPN name cannot be null";
    private static final String VPN_INS_NULL = "VPN instance cannot be null";
    private static final String ACCESS_INFO_NULL = "Access info cannot be null";
    private static final String BGP_INFO_NULL = "BGP info cannot be null";
    private static final String INT_INFO_NULL = "Interface info cannot be null";
    private static final String DEV_ID_NULL = "Device Id cannot be null";

    private final Logger log = getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected StorageService storageService;

    /**
     * Freed id list of NET L3VPN.
     */
    private DistributedSet<Long> freedIdList;

    /**
     * Map of interface info with access info as key.
     */
    private ConsistentMap<AccessInfo, InterfaceInfo> intInfoMap;

    /**
     * Map of VPN instance with VPN name as key.
     */
    private ConsistentMap<String, VpnInstance> vpnInsMap;

    /**
     * Map of BGP information and the device id.
     */
    private ConsistentMap<BgpInfo, DeviceId> bgpInfoMap;

    @Activate
    protected void activate() {
        vpnInsMap = storageService.<String, VpnInstance>consistentMapBuilder()
                .withName("onos-l3vpn-instance-map")
                .withSerializer(L3VPN_SERIALIZER)
                .build();

        intInfoMap = storageService
                .<AccessInfo, InterfaceInfo>consistentMapBuilder()
                .withName("onos-l3vpn-int-info-map")
                .withSerializer(L3VPN_SERIALIZER)
                .build();

        bgpInfoMap = storageService.<BgpInfo, DeviceId>consistentMapBuilder()
                .withName("onos-l3vpn-bgp-info-map")
                .withSerializer(L3VPN_SERIALIZER)
                .build();

        freedIdList = storageService.<Long>setBuilder()
                .withName("onos-l3vpn-id-freed-list")
                .withSerializer(Serializer.using(KryoNamespaces.API))
                .build()
                .asDistributedSet();

        log.info("Started");
    }

    @Deactivate
    protected void deactivate() {
        log.info("Stopped");
    }

    @Override
    public Iterable<Long> getFreedIdList() {
        return ImmutableSet.copyOf(freedIdList);
    }

    @Override
    public Map<String, VpnInstance> getVpnInstances() {
        return vpnInsMap.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue()
                        .value()));
    }

    @Override
    public Map<BgpInfo, DeviceId> getBgpInfo() {
        return bgpInfoMap.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue()
                        .value()));
    }

    @Override
    public Map<AccessInfo, InterfaceInfo> getInterfaceInfo() {
        return intInfoMap.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue()
                        .value()));
    }

    @Override
    public void addIdToFreeList(Long id) {
        checkNotNull(id, FREE_ID_NULL);
        freedIdList.add(id);
    }

    @Override
    public void addVpnInsIfAbsent(String name, VpnInstance instance) {
        checkNotNull(name, VPN_NAME_NULL);
        checkNotNull(instance, VPN_INS_NULL);
        vpnInsMap.putIfAbsent(name, instance);
    }

    @Override
    public void addVpnIns(String name, VpnInstance instance) {
        checkNotNull(name, VPN_NAME_NULL);
        checkNotNull(instance, VPN_INS_NULL);
        vpnInsMap.put(name, instance);
    }

    @Override
    public void addInterfaceInfo(AccessInfo accessInfo, InterfaceInfo intInfo) {
        checkNotNull(accessInfo, ACCESS_INFO_NULL);
        checkNotNull(intInfo, INT_INFO_NULL);
        intInfoMap.put(accessInfo, intInfo);
    }

    @Override
    public void addBgpInfo(BgpInfo bgpInfo, DeviceId devId) {
        checkNotNull(devId, BGP_INFO_NULL);
        checkNotNull(devId, DEV_ID_NULL);
        bgpInfoMap.put(bgpInfo, devId);
    }

    @Override
    public boolean removeInterfaceInfo(AccessInfo accessInfo) {
        checkNotNull(accessInfo, ACCESS_INFO_NULL);

        if (intInfoMap.remove(accessInfo) == null) {
            log.error("Interface info deletion for access info {} has failed.",
                      accessInfo.toString());
            return false;
        }
        return true;
    }

    @Override
    public boolean removeVpnInstance(String vpnName) {
        checkNotNull(vpnName, VPN_NAME_NULL);

        if (vpnInsMap.remove(vpnName) == null) {
            log.error("Vpn instance deletion for vpn name {} has failed.",
                      vpnName);
            return false;
        }
        return true;
    }

    @Override
    public boolean removeIdFromFreeList(Long id) {
        checkNotNull(id, FREE_ID_NULL);

        if (!freedIdList.remove(id)) {
            log.error("Id from free id list {} deletion has failed.",
                      id.toString());
            return false;
        }
        return true;
    }

    @Override
    public boolean removeBgpInfo(BgpInfo bgpInfo) {
        checkNotNull(bgpInfo, BGP_INFO_NULL);

        if (bgpInfoMap.remove(bgpInfo) == null) {
            log.error("Device id deletion for BGP info {} has failed.",
                      bgpInfo.toString());
            return false;
        }
        return true;
    }
}

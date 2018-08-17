/*
 * Copyright 2017-present Open Networking Foundation
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

package org.onosproject.evpnopenflow.rsc.vpnport.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.Sets;
import org.onlab.packet.IpAddress;
import org.onlab.packet.IpPrefix;
import org.onlab.packet.MacAddress;
import org.onlab.util.KryoNamespace;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.evpnopenflow.rsc.BasePort;
import org.onosproject.evpnopenflow.rsc.BasePortId;
import org.onosproject.evpnopenflow.rsc.DefaultVpnPort;
import org.onosproject.evpnopenflow.rsc.VpnInstanceId;
import org.onosproject.evpnopenflow.rsc.VpnPort;
import org.onosproject.evpnopenflow.rsc.VpnPortId;
import org.onosproject.evpnopenflow.rsc.baseport.BasePortService;
import org.onosproject.evpnopenflow.rsc.vpnport.VpnPortEvent;
import org.onosproject.evpnopenflow.rsc.vpnport.VpnPortListener;
import org.onosproject.evpnopenflow.rsc.vpnport.VpnPortService;
import org.onosproject.net.DeviceId;
import org.onosproject.store.serializers.KryoNamespaces;
import org.onosproject.store.service.EventuallyConsistentMap;
import org.onosproject.store.service.StorageService;
import org.onosproject.store.service.WallClockTimestamp;
import org.onosproject.vtnrsc.AllocationPool;
import org.onosproject.vtnrsc.BindingHostId;
import org.onosproject.vtnrsc.DefaultSubnet;
import org.onosproject.vtnrsc.DefaultTenantNetwork;
import org.onosproject.vtnrsc.DefaultVirtualPort;
import org.onosproject.vtnrsc.FixedIp;
import org.onosproject.vtnrsc.HostRoute;
import org.onosproject.vtnrsc.PhysicalNetwork;
import org.onosproject.vtnrsc.SegmentationId;
import org.onosproject.vtnrsc.Subnet;
import org.onosproject.vtnrsc.SubnetId;
import org.onosproject.vtnrsc.TenantId;
import org.onosproject.vtnrsc.TenantNetwork;
import org.onosproject.vtnrsc.TenantNetworkId;
import org.onosproject.vtnrsc.VirtualPort;
import org.onosproject.vtnrsc.VirtualPortId;
import org.onosproject.vtnrsc.subnet.SubnetService;
import org.onosproject.vtnrsc.tenantnetwork.TenantNetworkService;
import org.onosproject.vtnrsc.virtualport.VirtualPortService;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.onosproject.evpnopenflow.rsc.EvpnConstants.APP_ID;
import static org.onosproject.evpnopenflow.rsc.EvpnConstants.DELETE;
import static org.onosproject.evpnopenflow.rsc.EvpnConstants.EVENT_NOT_NULL;
import static org.onosproject.evpnopenflow.rsc.EvpnConstants.EVPN_VPN_PORT_START;
import static org.onosproject.evpnopenflow.rsc.EvpnConstants.EVPN_VPN_PORT_STOP;
import static org.onosproject.evpnopenflow.rsc.EvpnConstants.INTERFACE_ID;
import static org.onosproject.evpnopenflow.rsc.EvpnConstants.JSON_NOT_NULL;
import static org.onosproject.evpnopenflow.rsc.EvpnConstants.LISTENER_NOT_NULL;
import static org.onosproject.evpnopenflow.rsc.EvpnConstants.RESPONSE_NOT_NULL;
import static org.onosproject.evpnopenflow.rsc.EvpnConstants.SET;
import static org.onosproject.evpnopenflow.rsc.EvpnConstants.SLASH;
import static org.onosproject.evpnopenflow.rsc.EvpnConstants.UPDATE;
import static org.onosproject.evpnopenflow.rsc.EvpnConstants.VPN_INSTANCE;
import static org.onosproject.evpnopenflow.rsc.EvpnConstants.VPN_PORT_CREATION_FAILED;
import static org.onosproject.evpnopenflow.rsc.EvpnConstants.VPN_PORT_DELETE_FAILED;
import static org.onosproject.evpnopenflow.rsc.EvpnConstants.VPN_PORT_ID;
import static org.onosproject.evpnopenflow.rsc.EvpnConstants.VPN_PORT_ID_NOT_NULL;
import static org.onosproject.evpnopenflow.rsc.EvpnConstants.VPN_PORT_IS_NOT_EXIST;
import static org.onosproject.evpnopenflow.rsc.EvpnConstants.VPN_PORT_NOT_NULL;
import static org.onosproject.evpnopenflow.rsc.EvpnConstants.VPN_PORT_STORE;
import static org.onosproject.evpnopenflow.rsc.EvpnConstants.VPN_PORT_UPDATE_FAILED;

/**
 * Provides implementation of the VpnPort service.
 */
@Component(immediate = true, service = VpnPortService.class)
public class VpnPortManager implements VpnPortService {

    private final Logger log = LoggerFactory.getLogger(getClass());
    private final Set<VpnPortListener> listeners = Sets
            .newCopyOnWriteArraySet();

    protected EventuallyConsistentMap<VpnPortId, VpnPort> vpnPortStore;
    protected ApplicationId appId;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected StorageService storageService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected BasePortService basePortService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected VirtualPortService virtualPortService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected TenantNetworkService tenantNetworkService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected SubnetService subnetService;

    @Activate

    public void activate() {
        appId = coreService.registerApplication(APP_ID);
        KryoNamespace.Builder serializer = KryoNamespace.newBuilder()
                .register(KryoNamespaces.API).register(VpnPort.class)
                .register(VpnPortId.class);
        vpnPortStore = storageService
                .<VpnPortId, VpnPort>eventuallyConsistentMapBuilder()
                .withName(VPN_PORT_STORE).withSerializer(serializer)
                .withTimestampProvider((k, v) -> new WallClockTimestamp())
                .build();
        log.info(EVPN_VPN_PORT_START);
    }

    @Deactivate
    public void deactivate() {
        vpnPortStore.destroy();
        log.info(EVPN_VPN_PORT_STOP);
    }

    @Override
    public boolean exists(VpnPortId vpnPortId) {
        checkNotNull(vpnPortId, VPN_PORT_ID_NOT_NULL);
        return vpnPortStore.containsKey(vpnPortId);
    }

    @Override
    public VpnPort getPort(VpnPortId vpnPortId) {
        checkNotNull(vpnPortId, VPN_PORT_ID_NOT_NULL);
        return vpnPortStore.get(vpnPortId);
    }

    @Override
    public Collection<VpnPort> getPorts() {
        return Collections.unmodifiableCollection(vpnPortStore.values());
    }

    @Override
    public boolean createPorts(Iterable<VpnPort> vpnPorts) {
        checkNotNull(vpnPorts, VPN_PORT_NOT_NULL);
        for (VpnPort vpnPort : vpnPorts) {
            log.info(VPN_PORT_ID, vpnPort.id().toString());
            vpnPortStore.put(vpnPort.id(), vpnPort);
            if (!vpnPortStore.containsKey(vpnPort.id())) {
                log.info(VPN_PORT_CREATION_FAILED, vpnPort.id().toString());
                return false;
            }
            notifyListeners(new VpnPortEvent(VpnPortEvent.Type.VPN_PORT_SET,
                                             vpnPort));
        }
        return true;
    }

    @Override
    public boolean updatePorts(Iterable<VpnPort> vpnPorts) {
        checkNotNull(vpnPorts, VPN_PORT_NOT_NULL);
        for (VpnPort vpnPort : vpnPorts) {
            if (!vpnPortStore.containsKey(vpnPort.id())) {
                log.info(VPN_PORT_IS_NOT_EXIST, vpnPort.id().toString());
                return false;
            }
            vpnPortStore.put(vpnPort.id(), vpnPort);
            if (!vpnPort.equals(vpnPortStore.get(vpnPort.id()))) {
                log.info(VPN_PORT_UPDATE_FAILED, vpnPort.id().toString());
                return false;
            }
            notifyListeners(new VpnPortEvent(VpnPortEvent.Type.VPN_PORT_UPDATE,
                                             vpnPort));
        }
        return true;
    }

    @Override
    public boolean removePorts(Iterable<VpnPortId> vpnPortIds) {
        checkNotNull(vpnPortIds, VPN_PORT_NOT_NULL);
        for (VpnPortId vpnPortid : vpnPortIds) {
            VpnPort vpnPort = vpnPortStore.get(vpnPortid);
            vpnPortStore.remove(vpnPortid);
            if (vpnPortStore.containsKey(vpnPortid)) {
                log.info(VPN_PORT_DELETE_FAILED, vpnPortid.toString());
                return false;
            }
            notifyListeners(new VpnPortEvent(VpnPortEvent.Type.VPN_PORT_DELETE,
                                             vpnPort));
        }
        return true;
    }

    @Override
    public void processGluonConfig(String action, String key, JsonNode value) {
        Collection<VpnPort> vpnPorts;
        switch (action) {
            case DELETE:
                String[] list = key.split(SLASH);
                VpnPortId vpnPortId
                        = VpnPortId.vpnPortId(list[list.length - 1]);
                Set<VpnPortId> vpnPortIds = Sets.newHashSet(vpnPortId);
                removePorts(vpnPortIds);
                // After removing vpn port and also remove virtual port from vtn
                VirtualPortId virtualPortId
                        = VirtualPortId.portId(list[list.length - 1]);
                Set<VirtualPortId> virtualPortIds
                        = Sets.newHashSet(virtualPortId);
                virtualPortService.removePorts(virtualPortIds);
                break;
            case SET:
                checkNotNull(value, RESPONSE_NOT_NULL);
                vpnPorts = changeJsonToSub(value);
                createPorts(vpnPorts);
                break;
            case UPDATE:
                checkNotNull(value, RESPONSE_NOT_NULL);
                vpnPorts = changeJsonToSub(value);
                updatePorts(vpnPorts);
                break;
            default:
                log.info("Invalid action is received while processing VPN " +
                                 "port configuration");
        }
    }

    /**
     * Creates dummy gluon network to the VTN.
     *
     * @param state        the base port state
     * @param adminStateUp the base port admin status
     * @param tenantID     the base port tenant ID
     */
    private void createDummyGluonNetwork(boolean adminStateUp, String state,
                                         TenantId tenantID) {
        String id = "11111111-1111-1111-1111-111111111111";
        String name = "GluonNetwork";
        String segmentationID = "50";
        String physicalNetwork = "None";

        TenantNetwork network = new DefaultTenantNetwork(TenantNetworkId.networkId(id), name,
                                                         adminStateUp,
                                                         TenantNetwork.State.valueOf(state),
                                                         false, tenantID,
                                                         false,
                                                         TenantNetwork.Type.LOCAL,
                                                         PhysicalNetwork.physicalNetwork(physicalNetwork),
                                                         SegmentationId.segmentationId(segmentationID));

        Set<TenantNetwork> networksSet = Sets.newHashSet(network);
        tenantNetworkService.createNetworks(networksSet);
    }


    /**
     * Creates dummy gluon subnet to the VTN.
     *
     * @param tenantId the base port tenant ID
     */
    public void createDummySubnet(TenantId tenantId) {
        String id = "22222222-2222-2222-2222-222222222222";
        String subnetName = "GluonSubnet";
        String cidr = "0.0.0.0/0";
        String gatewayIp = "0.0.0.0";
        Set<HostRoute> hostRoutes = Sets.newHashSet();
        TenantNetworkId tenantNetworkId = null;
        Set<AllocationPool> allocationPools = Sets.newHashSet();
        Iterable<TenantNetwork> networks
                = tenantNetworkService.getNetworks();

        for (TenantNetwork tenantNetwork : networks) {
            if (tenantNetwork.name().equals("GluonNetwork")) {
                tenantNetworkId = tenantNetwork.id();
                break;
            }
        }
        Subnet subnet = new DefaultSubnet(SubnetId.subnetId(id), subnetName,
                                          tenantNetworkId,
                                          tenantId, IpAddress.Version.INET,
                                          IpPrefix.valueOf(cidr),
                                          IpAddress.valueOf(gatewayIp),
                                          false, false, hostRoutes,
                                          null,
                                          null,
                                          allocationPools);

        Set<Subnet> subnetsSet = Sets.newHashSet(subnet);
        subnetService.createSubnets(subnetsSet);
    }

    /**
     * Returns a collection of vpnPort from subnetNodes.
     *
     * @param vpnPortNodes the vpnPort json node
     * @return list of vpnports
     */
    private Collection<VpnPort> changeJsonToSub(JsonNode vpnPortNodes) {
        checkNotNull(vpnPortNodes, JSON_NOT_NULL);
        Map<VpnPortId, VpnPort> vpnPortMap = new HashMap<>();
        String interfaceId = vpnPortNodes.get(INTERFACE_ID).asText();
        VpnPortId vpnPortId = VpnPortId.vpnPortId(interfaceId);
        VpnInstanceId vpnInstanceId = VpnInstanceId
                .vpnInstanceId(vpnPortNodes.get(VPN_INSTANCE).asText());
        VpnPort vpnPort = new DefaultVpnPort(vpnPortId, vpnInstanceId);
        vpnPortMap.put(vpnPortId, vpnPort);
        // update ip address and tenant network information in vtn
        TenantNetworkId tenantNetworkId = null;
        Map<VirtualPortId, VirtualPort> vPortMap = new HashMap<>();
        BasePortId basePortId = BasePortId.portId(interfaceId);
        VirtualPortId virtualPortId = VirtualPortId.portId(interfaceId);
        BasePort bPort = basePortService.getPort(basePortId);
        if (bPort != null) {
            FixedIp fixedIp = FixedIp.fixedIp(SubnetId.subnetId(basePortId.toString()),
                                              IpAddress.valueOf(vpnPortNodes
                                                                        .get("ipaddress").asText()));
            Set<FixedIp> fixedIps = new HashSet<>();
            fixedIps.add(fixedIp);
            Map<String, String> strMap = new HashMap<>();
            boolean adminStateUp = bPort.adminStateUp();
            strMap.put("name", bPort.name());
            strMap.put("deviceOwner", bPort.deviceOwner());
            strMap.put("bindingVnicType", bPort.bindingVnicType());
            strMap.put("bindingVifType", bPort.bindingVifType());
            strMap.put("bindingVifDetails", bPort.bindingVifDetails());
            String state = bPort.state();
            MacAddress macAddress = bPort.macAddress();
            TenantId tenantId = bPort.tenantId();
            DeviceId deviceId = bPort.deviceId();
            BindingHostId bindingHostId = bPort.bindingHostId();
            // Creates Dummy Gluon Network and Subnet
            createDummyGluonNetwork(adminStateUp, state, tenantId);
            createDummySubnet(tenantId);

            Iterable<TenantNetwork> networks
                    = tenantNetworkService.getNetworks();

            for (TenantNetwork tenantNetwork : networks) {
                if (tenantNetwork.name().equals("GluonNetwork")) {
                    tenantNetworkId = tenantNetwork.id();
                    break;
                }
            }
            if (tenantNetworkId != null) {

                DefaultVirtualPort vPort = new DefaultVirtualPort(virtualPortId,
                                                                  tenantNetworkId,
                                                                  adminStateUp,
                                                                  strMap, isState(state),
                                                                  macAddress, tenantId,
                                                                  deviceId, fixedIps,
                                                                  bindingHostId,
                                                                  null,
                                                                  null);
                vPortMap.put(virtualPortId, vPort);
                Collection<VirtualPort> virtualPorts
                        = Collections.unmodifiableCollection(vPortMap.values());
                virtualPortService.createPorts(virtualPorts);
            }
        }

        return Collections.unmodifiableCollection(vpnPortMap.values());
    }

    /**
     * Returns BasePort State.
     *
     * @param state the base port state
     * @return the basePort state
     */
    private VirtualPort.State isState(String state) {
        if (state.equals("ACTIVE")) {
            return VirtualPort.State.ACTIVE;
        } else {
            return VirtualPort.State.DOWN;
        }

    }

    @Override
    public void addListener(VpnPortListener listener) {
        checkNotNull(listener, LISTENER_NOT_NULL);
        listeners.add(listener);
    }

    @Override
    public void removeListener(VpnPortListener listener) {
        checkNotNull(listener, LISTENER_NOT_NULL);
        listeners.remove(listener);
    }

    /**
     * Notifies specify event to all listeners.
     *
     * @param event Vpn Port event
     */
    private void notifyListeners(VpnPortEvent event) {
        checkNotNull(event, EVENT_NOT_NULL);
        listeners.forEach(listener -> {
            listener.event(event);
        });
    }
}

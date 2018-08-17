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

package org.onosproject.evpnopenflow.rsc.baseport.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.Sets;
import org.onlab.packet.IpAddress;
import org.onlab.packet.MacAddress;
import org.onlab.util.KryoNamespace;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.evpnopenflow.rsc.BasePort;
import org.onosproject.evpnopenflow.rsc.BasePortId;
import org.onosproject.evpnopenflow.rsc.DefaultBasePort;
import org.onosproject.evpnopenflow.rsc.baseport.BasePortEvent;
import org.onosproject.evpnopenflow.rsc.baseport.BasePortListener;
import org.onosproject.evpnopenflow.rsc.baseport.BasePortService;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Host;
import org.onosproject.store.serializers.KryoNamespaces;
import org.onosproject.store.service.EventuallyConsistentMap;
import org.onosproject.store.service.MultiValuedTimestamp;
import org.onosproject.store.service.StorageService;
import org.onosproject.store.service.WallClockTimestamp;
import org.onosproject.vtnrsc.AllowedAddressPair;
import org.onosproject.vtnrsc.BindingHostId;
import org.onosproject.vtnrsc.DefaultFloatingIp;
import org.onosproject.vtnrsc.FixedIp;
import org.onosproject.vtnrsc.FloatingIp;
import org.onosproject.vtnrsc.FloatingIpId;
import org.onosproject.vtnrsc.RouterId;
import org.onosproject.vtnrsc.SecurityGroup;
import org.onosproject.vtnrsc.SubnetId;
import org.onosproject.vtnrsc.TenantId;
import org.onosproject.vtnrsc.TenantNetwork;
import org.onosproject.vtnrsc.TenantNetworkId;
import org.onosproject.vtnrsc.TenantRouter;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.onosproject.evpnopenflow.rsc.EvpnConstants.APP_ID;
import static org.onosproject.evpnopenflow.rsc.EvpnConstants.BASE_PORT_STORE;
import static org.onosproject.evpnopenflow.rsc.EvpnConstants.LISTENER_NOT_NULL;
import static org.onosproject.evpnopenflow.rsc.EvpnConstants.RESPONSE_NOT_NULL;

/**
 * Provides implementation of the BasePort APIs.
 */
@Component(immediate = true, service = BasePortService.class)
public class BasePortManager implements BasePortService {

    private final Set<BasePortListener> listeners = Sets
            .newCopyOnWriteArraySet();
    private final Logger log = LoggerFactory.getLogger(getClass());
    private static final String BASEPORT_ID_NULL = "BasePort ID cannot be " +
            "null";
    private static final String BASEPORT_NOT_NULL = "BasePort  cannot be " +
            "null";
    private static final String TENANTID_NOT_NULL = "TenantId  cannot be null";
    private static final String NETWORKID_NOT_NULL = "NetworkId  cannot be null";
    private static final String DEVICEID_NOT_NULL = "DeviceId  cannot be null";
    private static final String FIXEDIP_NOT_NULL = "FixedIp  cannot be null";
    private static final String MAC_NOT_NULL = "Mac address  cannot be null";
    private static final String IP_NOT_NULL = "Ip  cannot be null";
    private static final String EVENT_NOT_NULL = "event cannot be null";
    private static final String SET = "set";
    private static final String UPDATE = "update";
    private static final String DELETE = "delete";
    private static final String SLASH = "/";
    private static final String PROTON_BASE_PORT = "Port";
    private static final String JSON_NOT_NULL = "JsonNode can not be null";

    protected EventuallyConsistentMap<BasePortId, BasePort> vPortStore;
    protected ApplicationId appId;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected StorageService storageService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected CoreService coreService;

    @Activate
    public void activate() {

        appId = coreService.registerApplication(APP_ID);

        KryoNamespace.Builder serializer = KryoNamespace.newBuilder()
                .register(KryoNamespaces.API)
                .register(MultiValuedTimestamp.class)
                .register(TenantNetworkId.class)
                .register(Host.class)
                .register(TenantNetwork.class)
                .register(TenantNetworkId.class)
                .register(TenantId.class)
                .register(SubnetId.class)
                .register(BasePortId.class)
                .register(BasePort.State.class)
                .register(AllowedAddressPair.class)
                .register(FixedIp.class)
                .register(FloatingIp.class)
                .register(FloatingIpId.class)
                .register(FloatingIp.Status.class)
                .register(UUID.class)
                .register(DefaultFloatingIp.class)
                .register(BindingHostId.class)
                .register(SecurityGroup.class)
                .register(IpAddress.class)
                .register(DefaultBasePort.class)
                .register(RouterId.class)
                .register(TenantRouter.class)
                .register(BasePort.class);
        vPortStore = storageService
                .<BasePortId, BasePort>eventuallyConsistentMapBuilder()
                .withName(BASE_PORT_STORE).withSerializer(serializer)
                .withTimestampProvider((k, v) -> new WallClockTimestamp())
                .build();
        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        vPortStore.destroy();
        log.info("Stoppped");
    }

    @Override
    public boolean exists(BasePortId vPortId) {
        checkNotNull(vPortId, BASEPORT_ID_NULL);
        return vPortStore.containsKey(vPortId);
    }

    @Override
    public BasePort getPort(BasePortId vPortId) {
        checkNotNull(vPortId, BASEPORT_ID_NULL);
        return vPortStore.get(vPortId);
    }

    @Override
    public BasePort getPort(FixedIp fixedIP) {
        checkNotNull(fixedIP, FIXEDIP_NOT_NULL);
        List<BasePort> vPorts = new ArrayList<>();
        vPortStore.values().forEach(p -> {
            for (FixedIp fixedIp : p.fixedIps()) {
                if (fixedIp.equals(fixedIP)) {
                    vPorts.add(p);
                    break;
                }
            }
        });
        if (vPorts.size() == 0) {
            return null;
        }
        return vPorts.get(0);
    }

    @Override
    public BasePort getPort(MacAddress mac) {
        checkNotNull(mac, MAC_NOT_NULL);
        List<BasePort> vPorts = new ArrayList<>();
        vPortStore.values().forEach(p -> {
            if (p.macAddress().equals(mac)) {
                vPorts.add(p);
            }
        });
        if (vPorts.size() == 0) {
            return null;
        }
        return vPorts.get(0);
    }

    @Override
    public BasePort getPort(TenantNetworkId networkId, IpAddress ip) {
        checkNotNull(networkId, NETWORKID_NOT_NULL);
        checkNotNull(ip, IP_NOT_NULL);
        List<BasePort> vPorts = new ArrayList<>();
        vPortStore.values().stream().filter(p -> p.networkId().equals(networkId))
                .forEach(p -> {
                    for (FixedIp fixedIp : p.fixedIps()) {
                        if (fixedIp.ip().equals(ip)) {
                            vPorts.add(p);
                            break;
                        }
                    }
                });
        if (vPorts.size() == 0) {
            return null;
        }
        return vPorts.get(0);
    }

    @Override
    public Collection<BasePort> getPorts() {
        return Collections.unmodifiableCollection(vPortStore.values());
    }

    @Override
    public Collection<BasePort> getPorts(TenantNetworkId networkId) {
        checkNotNull(networkId, NETWORKID_NOT_NULL);
        return vPortStore.values().stream().filter(d -> d.networkId().equals(networkId))
                .collect(Collectors.toList());
    }

    @Override
    public Collection<BasePort> getPorts(TenantId tenantId) {
        checkNotNull(tenantId, TENANTID_NOT_NULL);
        return vPortStore.values().stream().filter(d -> d.tenantId().equals(tenantId))
                .collect(Collectors.toList());
    }

    @Override
    public Collection<BasePort> getPorts(DeviceId deviceId) {
        checkNotNull(deviceId, DEVICEID_NOT_NULL);
        return vPortStore.values().stream().filter(d -> d.deviceId().equals(deviceId))
                .collect(Collectors.toList());
    }

    @Override
    public boolean createPorts(Iterable<BasePort> vPorts) {
        checkNotNull(vPorts, BASEPORT_NOT_NULL);
        for (BasePort vPort : vPorts) {
            log.info("vPortId is  {} ", vPort.portId().toString());
            vPortStore.put(vPort.portId(), vPort);
            if (!vPortStore.containsKey(vPort.portId())) {
                log.info("The basePort is created failed whose identifier is" +
                                 " {} ",
                         vPort.portId().toString());
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean updatePorts(Iterable<BasePort> vPorts) {
        checkNotNull(vPorts, BASEPORT_NOT_NULL);
        for (BasePort vPort : vPorts) {
            vPortStore.put(vPort.portId(), vPort);
            if (!vPortStore.containsKey(vPort.portId())) {
                log.info("The basePort is not exist whose identifier is {}",
                         vPort.portId().toString());
                return false;
            }

            vPortStore.put(vPort.portId(), vPort);

            if (!vPort.equals(vPortStore.get(vPort.portId()))) {
                log.info("The basePort is updated failed whose  identifier " +
                                 "is {}",
                         vPort.portId().toString());
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean removePorts(Iterable<BasePortId> vPortIds) {
        checkNotNull(vPortIds, BASEPORT_ID_NULL);
        for (BasePortId vPortId : vPortIds) {
            vPortStore.remove(vPortId);
            if (vPortStore.containsKey(vPortId)) {
                log.info("The basePort is removed failed whose identifier is" +
                                 " {}",
                         vPortId.toString());
                return false;
            }
        }
        return true;
    }

    /**
     * Returns a collection of basePorts from subnetNodes.
     *
     * @param vPortNodes the basePort json node
     * @return BasePort collection of vpn ports
     */
    private Collection<BasePort> changeJsonToSub(JsonNode vPortNodes) {
        checkNotNull(vPortNodes, JSON_NOT_NULL);
        Set<FixedIp> fixedIps = null;
        TenantNetworkId tenantNetworkId = null;
        Map<BasePortId, BasePort> vportMap = new HashMap<>();
        Map<String, String> strMap = new HashMap<>();
        BasePortId basePortId = BasePortId.portId(vPortNodes.get("id").asText());
        String name = vPortNodes.get("name").asText();
        TenantId tenantId = TenantId
                .tenantId(vPortNodes.get("tenant_id").asText());
        Boolean adminStateUp = vPortNodes.get("admin_state_up").asBoolean();
        String state = vPortNodes.get("status").asText();
        MacAddress macAddress = MacAddress
                .valueOf(vPortNodes.get("mac_address").asText());
        DeviceId deviceId = DeviceId
                .deviceId(vPortNodes.get("device_id").asText());
        String deviceOwner = vPortNodes.get("device_owner").asText();
        BindingHostId bindingHostId = BindingHostId
                .bindingHostId(vPortNodes.get("host_id").asText());
        String bindingVnicType = vPortNodes.get("vnic_type").asText();
        String bindingVifType = vPortNodes.get("vif_type").asText();
        String bindingVifDetails = vPortNodes.get("vif_details").asText();
        strMap.put("name", name);
        strMap.put("deviceOwner", deviceOwner);
        strMap.put("bindingVnicType", bindingVnicType);
        strMap.put("bindingVifType", bindingVifType);
        strMap.put("bindingVifDetails", bindingVifDetails);
        BasePort prevBasePort = getPort(basePortId);
        if (prevBasePort != null) {
            fixedIps = prevBasePort.fixedIps();
            tenantNetworkId = prevBasePort.networkId();
        }
        BasePort vPort = new DefaultBasePort(basePortId,
                                             tenantNetworkId,
                                             adminStateUp,
                                             strMap, state,
                                             macAddress, tenantId,
                                             deviceId, fixedIps,
                                             bindingHostId,
                                             null,
                                             null);
        vportMap.put(basePortId, vPort);

        return Collections.unmodifiableCollection(vportMap.values());
    }

    /**
     * Returns BasePort State.
     *
     * @param state the base port state
     * @return the basePort state
     */
    private BasePort.State isState(String state) {
        if (state.equals("ACTIVE")) {
            return BasePort.State.ACTIVE;
        } else {
            return BasePort.State.DOWN;
        }

    }

    /**
     * process Etcd response for port information.
     *
     * @param action can be either update or delete
     * @param key    can contain the id and also target information
     * @param value  content of the port configuration
     */
    @Override
    public void processGluonConfig(String action, String key, JsonNode value) {
        Collection<BasePort> basePorts;
        switch (action) {
            case DELETE:
                String[] list = key.split(SLASH);
                BasePortId basePortId
                        = BasePortId.portId(list[list.length - 1]);
                Set<BasePortId> basePortIds = Sets.newHashSet(basePortId);
                removePorts(basePortIds);
                break;
            case SET:
                checkNotNull(value, RESPONSE_NOT_NULL);
                basePorts = changeJsonToSub(value);
                createPorts(basePorts);
                break;
            case UPDATE:
                checkNotNull(value, RESPONSE_NOT_NULL);
                basePorts = changeJsonToSub(value);
                updatePorts(basePorts);
                break;
            default:
                log.info("Invalid action is received while processing VPN " +
                                 "port configuration");
        }
    }

    private void parseEtcdResponse(JsonNode jsonNode,
                                   String key,
                                   String action) {
        JsonNode modifyValue = null;
        if (action.equals(SET)) {
            modifyValue = jsonNode.get(key);
        }
        String[] list = key.split(SLASH);
        String target = list[list.length - 2];
        if (target.equals(PROTON_BASE_PORT)) {
            processGluonConfig(action, key, modifyValue);
        }
    }

    @Override
    public void addListener(BasePortListener listener) {
        checkNotNull(listener, LISTENER_NOT_NULL);
        listeners.add(listener);
    }

    @Override
    public void removeListener(BasePortListener listener) {
        checkNotNull(listener, LISTENER_NOT_NULL);
        listeners.remove(listener);
    }

    /**
     * Notifies specify event to all listeners.
     *
     * @param event vpn af config event
     */
    private void notifyListeners(BasePortEvent event) {
        checkNotNull(event, EVENT_NOT_NULL);
        listeners.forEach(listener -> listener.event(event));
    }
}

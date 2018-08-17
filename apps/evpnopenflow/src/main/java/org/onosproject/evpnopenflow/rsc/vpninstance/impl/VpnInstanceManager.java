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

package org.onosproject.evpnopenflow.rsc.vpninstance.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.Sets;
import org.onlab.util.KryoNamespace;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.evpnopenflow.rsc.DefaultVpnInstance;
import org.onosproject.evpnopenflow.rsc.VpnAfConfig;
import org.onosproject.evpnopenflow.rsc.VpnInstance;
import org.onosproject.evpnopenflow.rsc.VpnInstanceId;
import org.onosproject.evpnopenflow.rsc.vpnafconfig.VpnAfConfigService;
import org.onosproject.evpnopenflow.rsc.vpninstance.VpnInstanceService;
import org.onosproject.evpnrouteservice.EvpnInstanceName;
import org.onosproject.evpnrouteservice.RouteDistinguisher;
import org.onosproject.evpnrouteservice.VpnRouteTarget;
import org.onosproject.routeservice.RouteAdminService;
import org.onosproject.store.serializers.KryoNamespaces;
import org.onosproject.store.service.EventuallyConsistentMap;
import org.onosproject.store.service.StorageService;
import org.onosproject.store.service.WallClockTimestamp;
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
import static org.onosproject.evpnopenflow.rsc.EvpnConstants.COMMA;
import static org.onosproject.evpnopenflow.rsc.EvpnConstants.DELETE;
import static org.onosproject.evpnopenflow.rsc.EvpnConstants.DESCRIPTION;
import static org.onosproject.evpnopenflow.rsc.EvpnConstants.EVPN_VPN_INSTANCE_START;
import static org.onosproject.evpnopenflow.rsc.EvpnConstants.EVPN_VPN_INSTANCE_STOP;
import static org.onosproject.evpnopenflow.rsc.EvpnConstants.ID;
import static org.onosproject.evpnopenflow.rsc.EvpnConstants.INSTANCE_ID;
import static org.onosproject.evpnopenflow.rsc.EvpnConstants.IPV4_FAMILY;
import static org.onosproject.evpnopenflow.rsc.EvpnConstants.JSON_NOT_NULL;
import static org.onosproject.evpnopenflow.rsc.EvpnConstants.RESPONSE_NOT_NULL;
import static org.onosproject.evpnopenflow.rsc.EvpnConstants.ROUTE_DISTINGUISHERS;
import static org.onosproject.evpnopenflow.rsc.EvpnConstants.SET;
import static org.onosproject.evpnopenflow.rsc.EvpnConstants.SLASH;
import static org.onosproject.evpnopenflow.rsc.EvpnConstants.UPDATE;
import static org.onosproject.evpnopenflow.rsc.EvpnConstants.VPN_INSTANCE_CREATION_FAILED;
import static org.onosproject.evpnopenflow.rsc.EvpnConstants.VPN_INSTANCE_DELETE_FAILED;
import static org.onosproject.evpnopenflow.rsc.EvpnConstants.VPN_INSTANCE_ID_NOT_NULL;
import static org.onosproject.evpnopenflow.rsc.EvpnConstants.VPN_INSTANCE_IS_NOT_EXIST;
import static org.onosproject.evpnopenflow.rsc.EvpnConstants.VPN_INSTANCE_NAME;
import static org.onosproject.evpnopenflow.rsc.EvpnConstants.VPN_INSTANCE_NOT_NULL;
import static org.onosproject.evpnopenflow.rsc.EvpnConstants.VPN_INSTANCE_STORE;
import static org.onosproject.evpnopenflow.rsc.EvpnConstants.VPN_INSTANCE_UPDATE_FAILED;


/**
 * Provides implementation of the VpnInstance APIs.
 */
@Component(immediate = true, service = VpnInstanceService.class)
public class VpnInstanceManager implements VpnInstanceService {

    private final Logger log = LoggerFactory.getLogger(getClass());

    protected EventuallyConsistentMap<VpnInstanceId, VpnInstance> vpnInstanceStore;
    protected ApplicationId appId;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected StorageService storageService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected RouteAdminService routeService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected VpnAfConfigService vpnAfConfigService;

    @Activate
    public void activate() {
        appId = coreService.registerApplication(APP_ID);
        KryoNamespace.Builder serializer = KryoNamespace.newBuilder()
                .register(KryoNamespaces.API).register(VpnInstance.class)
                .register(VpnInstanceId.class);
        vpnInstanceStore = storageService
                .<VpnInstanceId, VpnInstance>eventuallyConsistentMapBuilder()
                .withName(VPN_INSTANCE_STORE).withSerializer(serializer)
                .withTimestampProvider((k, v) -> new WallClockTimestamp())
                .build();
        log.info(EVPN_VPN_INSTANCE_START);
    }

    @Deactivate
    public void deactivate() {
        vpnInstanceStore.destroy();
        log.info(EVPN_VPN_INSTANCE_STOP);
    }

    @Override
    public boolean exists(VpnInstanceId vpnInstanceId) {
        checkNotNull(vpnInstanceId, VPN_INSTANCE_ID_NOT_NULL);
        return vpnInstanceStore.containsKey(vpnInstanceId);
    }

    @Override
    public VpnInstance getInstance(VpnInstanceId vpnInstanceId) {
        checkNotNull(vpnInstanceId, VPN_INSTANCE_ID_NOT_NULL);
        return vpnInstanceStore.get(vpnInstanceId);
    }

    @Override
    public Collection<VpnInstance> getInstances() {
        return Collections.unmodifiableCollection(vpnInstanceStore.values());
    }

    @Override
    public boolean createInstances(Iterable<VpnInstance> vpnInstances) {
        checkNotNull(vpnInstances, VPN_INSTANCE_NOT_NULL);
        for (VpnInstance vpnInstance : vpnInstances) {
            log.info(INSTANCE_ID, vpnInstance.id().toString());
            vpnInstanceStore.put(vpnInstance.id(), vpnInstance);
            if (!vpnInstanceStore.containsKey(vpnInstance.id())) {
                log.info(VPN_INSTANCE_CREATION_FAILED,
                         vpnInstance.id().toString());
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean updateInstances(Iterable<VpnInstance> vpnInstances) {
        checkNotNull(vpnInstances, VPN_INSTANCE_NOT_NULL);
        for (VpnInstance vpnInstance : vpnInstances) {
            if (!vpnInstanceStore.containsKey(vpnInstance.id())) {
                log.info(VPN_INSTANCE_IS_NOT_EXIST,
                         vpnInstance.id().toString());
                return false;
            }
            vpnInstanceStore.put(vpnInstance.id(), vpnInstance);
            if (!vpnInstance.equals(vpnInstanceStore.get(vpnInstance.id()))) {
                log.info(VPN_INSTANCE_UPDATE_FAILED,
                         vpnInstance.id().toString());
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean removeInstances(Iterable<VpnInstanceId> vpnInstanceIds) {
        checkNotNull(vpnInstanceIds, VPN_INSTANCE_ID_NOT_NULL);
        for (VpnInstanceId vpnInstanceId : vpnInstanceIds) {
            vpnInstanceStore.remove(vpnInstanceId);
            if (vpnInstanceStore.containsKey(vpnInstanceId)) {
                log.info(VPN_INSTANCE_DELETE_FAILED, vpnInstanceId.toString());
                return false;
            }
        }
        return true;
    }

    @Override
    public void processGluonConfig(String action, String key, JsonNode value) {
        Collection<VpnInstance> vpnInstances;
        switch (action) {
            case DELETE:
                String[] list = key.split(SLASH);
                VpnInstanceId vpnInstanceId = VpnInstanceId
                        .vpnInstanceId(list[list.length - 1]);
                Set<VpnInstanceId> vpnInstanceIds
                        = Sets.newHashSet(vpnInstanceId);
                removeInstances(vpnInstanceIds);
                break;
            case SET:
                checkNotNull(value, RESPONSE_NOT_NULL);
                vpnInstances = changeJsonToSub(value);
                createInstances(vpnInstances);
                break;
            case UPDATE:
                checkNotNull(value, RESPONSE_NOT_NULL);
                vpnInstances = changeJsonToSub(value);
                updateInstances(vpnInstances);
                break;
            default:
                log.info("Invalid action is received while processing VPN " +
                                 "instance configuration");
        }
    }

    @Override
    public void updateImpExpRouteTargets(String routeTargetType,
                                         Set<VpnRouteTarget> exportRouteTargets,
                                         Set<VpnRouteTarget> importRouteTargets,
                                         VpnRouteTarget vpnRouteTarget) {
        switch (routeTargetType) {
            case "export_extcommunity":
                exportRouteTargets.add(vpnRouteTarget);
                break;
            case "import_extcommunity":
                importRouteTargets.add(vpnRouteTarget);
                break;
            case "both":
                exportRouteTargets.add(vpnRouteTarget);
                importRouteTargets.add(vpnRouteTarget);
                break;
            default:
                log.info("Invalid route target type has received");
                break;
        }
    }

    /**
     * Returns a collection of vpnInstances from subnetNodes.
     *
     * @param vpnInstanceNodes the vpnInstance json node
     * @return returns the collection of vpn instances
     */
    private Collection<VpnInstance> changeJsonToSub(JsonNode vpnInstanceNodes) {
        checkNotNull(vpnInstanceNodes, JSON_NOT_NULL);

        Set<VpnRouteTarget> exportRouteTargets = new HashSet<>();
        Set<VpnRouteTarget> importRouteTargets = new HashSet<>();
        Set<VpnRouteTarget> configRouteTargets = new HashSet<>();

        Map<VpnInstanceId, VpnInstance> vpnInstanceMap = new HashMap<>();
        VpnInstanceId id = VpnInstanceId
                .vpnInstanceId(vpnInstanceNodes.get(ID).asText());
        EvpnInstanceName name = EvpnInstanceName
                .evpnName(vpnInstanceNodes.get(VPN_INSTANCE_NAME).asText());
        String description = vpnInstanceNodes.get(DESCRIPTION).asText();
        RouteDistinguisher routeDistinguisher = RouteDistinguisher
                .routeDistinguisher(vpnInstanceNodes.get(ROUTE_DISTINGUISHERS)
                                            .asText());
        String routeTargets = vpnInstanceNodes.get(IPV4_FAMILY).asText();
        String[] list = routeTargets.split(COMMA);

        for (String routeTarget : list) {
            // Converting route target string into route target object and
            // then storing into configuration route target set.
            VpnRouteTarget vpnRouteTarget
                    = VpnRouteTarget.routeTarget(routeTarget);
            configRouteTargets.add(vpnRouteTarget);
            VpnAfConfig vpnAfConfig
                    = vpnAfConfigService.getVpnAfConfig(vpnRouteTarget);
            if (vpnAfConfig == null) {
                log.info("Not able to find vpn af config for the give vpn " +
                                 "route target");
                break;
            }
            updateImpExpRouteTargets(vpnAfConfig.routeTargetType(),
                                     exportRouteTargets,
                                     importRouteTargets,
                                     vpnRouteTarget);
        }

        VpnInstance vpnInstance = new DefaultVpnInstance(id, name, description,
                                                         routeDistinguisher,
                                                         exportRouteTargets,
                                                         importRouteTargets,
                                                         configRouteTargets);
        vpnInstanceMap.put(id, vpnInstance);
        return Collections.unmodifiableCollection(vpnInstanceMap.values());
    }
}

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

package org.onosproject.evpnopenflow.rsc.vpnafconfig.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.Sets;
import org.onlab.util.KryoNamespace;
import org.onosproject.core.CoreService;
import org.onosproject.evpnopenflow.rsc.DefaultVpnAfConfig;
import org.onosproject.evpnopenflow.rsc.VpnAfConfig;
import org.onosproject.evpnopenflow.rsc.vpnafconfig.VpnAfConfigEvent;
import org.onosproject.evpnopenflow.rsc.vpnafconfig.VpnAfConfigListener;
import org.onosproject.evpnopenflow.rsc.vpnafconfig.VpnAfConfigService;
import org.onosproject.evpnrouteservice.VpnRouteTarget;
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
import java.util.Map;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.onosproject.evpnopenflow.rsc.EvpnConstants.APP_ID;
import static org.onosproject.evpnopenflow.rsc.EvpnConstants.DELETE;
import static org.onosproject.evpnopenflow.rsc.EvpnConstants.EVENT_NOT_NULL;
import static org.onosproject.evpnopenflow.rsc.EvpnConstants.EVPN_VPN_AF_CONFIG_START;
import static org.onosproject.evpnopenflow.rsc.EvpnConstants.EVPN_VPN_AF_CONFIG_STOP;
import static org.onosproject.evpnopenflow.rsc.EvpnConstants.EXPORT_ROUTE_POLICY;
import static org.onosproject.evpnopenflow.rsc.EvpnConstants.IMPORT_ROUTE_POLICY;
import static org.onosproject.evpnopenflow.rsc.EvpnConstants.INVALID_ACTION_VPN_AF_CONFIG;
import static org.onosproject.evpnopenflow.rsc.EvpnConstants.JSON_NOT_NULL;
import static org.onosproject.evpnopenflow.rsc.EvpnConstants.LISTENER_NOT_NULL;
import static org.onosproject.evpnopenflow.rsc.EvpnConstants.RESPONSE_NOT_NULL;
import static org.onosproject.evpnopenflow.rsc.EvpnConstants.ROUTE_TARGET_CANNOT_NOT_NULL;
import static org.onosproject.evpnopenflow.rsc.EvpnConstants.ROUTE_TARGET_DELETE_FAILED;
import static org.onosproject.evpnopenflow.rsc.EvpnConstants.ROUTE_TARGET_VALUE;
import static org.onosproject.evpnopenflow.rsc.EvpnConstants.SET;
import static org.onosproject.evpnopenflow.rsc.EvpnConstants.SLASH;
import static org.onosproject.evpnopenflow.rsc.EvpnConstants.UPDATE;
import static org.onosproject.evpnopenflow.rsc.EvpnConstants.VPN_AF_CONFIG_CREATION_FAILED;
import static org.onosproject.evpnopenflow.rsc.EvpnConstants.VPN_AF_CONFIG_IS_NOT_EXIST;
import static org.onosproject.evpnopenflow.rsc.EvpnConstants.VPN_AF_CONFIG_NOT_NULL;
import static org.onosproject.evpnopenflow.rsc.EvpnConstants.VPN_AF_CONFIG_STORE;
import static org.onosproject.evpnopenflow.rsc.EvpnConstants.VPN_AF_CONFIG_UPDATE_FAILED;
import static org.onosproject.evpnopenflow.rsc.EvpnConstants.VPN_INSTANCE_ID_NOT_NULL;
import static org.onosproject.evpnopenflow.rsc.EvpnConstants.VRF_RT_TYPE;
import static org.onosproject.evpnopenflow.rsc.EvpnConstants.VRF_RT_VALUE;

/**
 * Provides implementation of the VPN af config APIs.
 */
@Component(immediate = true, service = VpnAfConfigService.class)
public class VpnAfConfigManager implements VpnAfConfigService {
    private final Logger log = LoggerFactory.getLogger(getClass());
    private final Set<VpnAfConfigListener> listeners = Sets
            .newCopyOnWriteArraySet();

    protected EventuallyConsistentMap<VpnRouteTarget, VpnAfConfig>
            vpnAfConfigStore;
    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected StorageService storageService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected CoreService coreService;

    @Activate
    public void activate() {
        coreService.registerApplication(APP_ID);
        KryoNamespace.Builder serializer = KryoNamespace.newBuilder()
                .register(KryoNamespaces.API).register(VpnAfConfig.class)
                .register(VpnRouteTarget.class);
        vpnAfConfigStore = storageService
                .<VpnRouteTarget, VpnAfConfig>eventuallyConsistentMapBuilder()
                .withName(VPN_AF_CONFIG_STORE).withSerializer(serializer)
                .withTimestampProvider((k, v) -> new WallClockTimestamp())
                .build();
        log.info(EVPN_VPN_AF_CONFIG_START);
    }

    @Deactivate
    public void deactivate() {
        vpnAfConfigStore.destroy();
        log.info(EVPN_VPN_AF_CONFIG_STOP);
    }

    @Override
    public boolean exists(VpnRouteTarget routeTarget) {
        checkNotNull(routeTarget, ROUTE_TARGET_CANNOT_NOT_NULL);
        return vpnAfConfigStore.containsKey(routeTarget);
    }

    @Override
    public VpnAfConfig getVpnAfConfig(VpnRouteTarget routeTarget) {
        checkNotNull(routeTarget, ROUTE_TARGET_CANNOT_NOT_NULL);
        return vpnAfConfigStore.get(routeTarget);
    }

    @Override
    public Collection<VpnAfConfig> getVpnAfConfigs() {
        return Collections.unmodifiableCollection(vpnAfConfigStore.values());
    }

    @Override
    public boolean createVpnAfConfigs(Iterable<VpnAfConfig> vpnAfConfigs) {
        checkNotNull(vpnAfConfigs, VPN_AF_CONFIG_NOT_NULL);
        for (VpnAfConfig vpnAfConfig : vpnAfConfigs) {
            log.info(ROUTE_TARGET_VALUE, vpnAfConfig
                    .routeTarget().getRouteTarget());
            vpnAfConfigStore.put(vpnAfConfig.routeTarget(), vpnAfConfig);
            if (!vpnAfConfigStore.containsKey(vpnAfConfig.routeTarget())) {
                log.info(VPN_AF_CONFIG_CREATION_FAILED,
                         vpnAfConfig.routeTarget().getRouteTarget());
                return false;
            }
            notifyListeners(new VpnAfConfigEvent(VpnAfConfigEvent
                                                         .Type
                                                         .VPN_AF_CONFIG_SET,
                                                 vpnAfConfig));
        }
        return true;
    }

    @Override
    public boolean updateVpnAfConfigs(Iterable<VpnAfConfig> vpnAfConfigs) {
        checkNotNull(vpnAfConfigs, VPN_AF_CONFIG_NOT_NULL);
        for (VpnAfConfig vpnAfConfig : vpnAfConfigs) {
            if (!vpnAfConfigStore.containsKey(vpnAfConfig.routeTarget())) {
                log.info(VPN_AF_CONFIG_IS_NOT_EXIST,
                         vpnAfConfig.routeTarget().getRouteTarget());
                return false;
            }
            vpnAfConfigStore.put(vpnAfConfig.routeTarget(), vpnAfConfig);
            if (!vpnAfConfig.equals(vpnAfConfigStore
                                            .get(vpnAfConfig.routeTarget()))) {
                log.info(VPN_AF_CONFIG_UPDATE_FAILED,
                         vpnAfConfig.routeTarget().getRouteTarget());
                return false;
            }
            notifyListeners(new VpnAfConfigEvent(VpnAfConfigEvent
                                                         .Type
                                                         .VPN_AF_CONFIG_UPDATE,
                                                 vpnAfConfig));
        }
        return true;
    }

    @Override
    public boolean removeVpnAfConfigs(Iterable<VpnRouteTarget> routeTargets) {
        checkNotNull(routeTargets, VPN_INSTANCE_ID_NOT_NULL);
        for (VpnRouteTarget routeTarget : routeTargets) {
            VpnAfConfig vpnAfConfig = vpnAfConfigStore.get(routeTarget);
            vpnAfConfigStore.remove(routeTarget);
            if (vpnAfConfigStore.containsKey(routeTarget)) {
                log.info(ROUTE_TARGET_DELETE_FAILED,
                         routeTarget.getRouteTarget());
                return false;
            }
            notifyListeners(new VpnAfConfigEvent(VpnAfConfigEvent
                                                         .Type
                                                         .VPN_AF_CONFIG_DELETE,
                                                 vpnAfConfig));
        }
        return true;
    }

    @Override
    public void processGluonConfig(String action, String key, JsonNode value) {
        Collection<VpnAfConfig> vpnAfConfigs;
        switch (action) {
            case DELETE:
                String[] list = key.split(SLASH);
                VpnRouteTarget routeTarget = VpnRouteTarget
                        .routeTarget(list[list.length - 1]);
                Set<VpnRouteTarget> routeTargets
                        = Sets.newHashSet(routeTarget);
                removeVpnAfConfigs(routeTargets);
                break;
            case SET:
                checkNotNull(value, RESPONSE_NOT_NULL);
                vpnAfConfigs = changeJsonToSub(value);
                createVpnAfConfigs(vpnAfConfigs);
                break;
            case UPDATE:
                checkNotNull(value, RESPONSE_NOT_NULL);
                vpnAfConfigs = changeJsonToSub(value);
                updateVpnAfConfigs(vpnAfConfigs);
                break;
            default:
                log.info(INVALID_ACTION_VPN_AF_CONFIG);
                break;
        }
    }

    /**
     * Returns a collection of vpn af configuration.
     *
     * @param vpnAfConfigNode the vpn af configuration json node
     * @return returns the collection of vpn af configuration
     */
    private Collection<VpnAfConfig> changeJsonToSub(JsonNode vpnAfConfigNode) {
        checkNotNull(vpnAfConfigNode, JSON_NOT_NULL);
        Map<VpnRouteTarget, VpnAfConfig> vpnAfConfigMap = new HashMap<>();
        String exportRoutePolicy
                = vpnAfConfigNode.get(EXPORT_ROUTE_POLICY).asText();
        String importRoutePolicy
                = vpnAfConfigNode.get(IMPORT_ROUTE_POLICY).asText();
        String routeTargetType = vpnAfConfigNode.get(VRF_RT_TYPE).asText();
        VpnRouteTarget routeTarget = VpnRouteTarget
                .routeTarget(vpnAfConfigNode.get(VRF_RT_VALUE).asText());

        VpnAfConfig vpnAfConfig = new DefaultVpnAfConfig(exportRoutePolicy,
                                                         importRoutePolicy,
                                                         routeTarget,
                                                         routeTargetType);
        vpnAfConfigMap.put(routeTarget, vpnAfConfig);

        return Collections.unmodifiableCollection(vpnAfConfigMap.values());
    }

    @Override
    public void addListener(VpnAfConfigListener listener) {
        checkNotNull(listener, LISTENER_NOT_NULL);
        listeners.add(listener);
    }

    @Override
    public void removeListener(VpnAfConfigListener listener) {
        checkNotNull(listener, LISTENER_NOT_NULL);
        listeners.remove(listener);
    }

    /**
     * Notifies specify event to all listeners.
     *
     * @param event vpn af config event
     */
    private void notifyListeners(VpnAfConfigEvent event) {
        checkNotNull(event, EVENT_NOT_NULL);
        listeners.forEach(listener -> listener.event(event));
    }
}

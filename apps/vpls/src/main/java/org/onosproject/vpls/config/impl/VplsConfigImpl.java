/*
 * Copyright 2016-present Open Networking Laboratory
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
package org.onosproject.vpls.config.impl;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.SetMultimap;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onlab.packet.VlanId;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.incubator.net.intf.Interface;
import org.onosproject.incubator.net.intf.InterfaceService;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.EncapsulationType;
import org.onosproject.net.config.ConfigFactory;
import org.onosproject.net.config.NetworkConfigEvent;
import org.onosproject.net.config.NetworkConfigListener;
import org.onosproject.net.config.NetworkConfigRegistry;
import org.onosproject.net.config.NetworkConfigService;
import org.onosproject.net.config.basics.SubjectFactories;
import org.onosproject.vpls.config.VplsAppConfig;
import org.onosproject.vpls.config.VplsConfig;
import org.onosproject.vpls.config.VplsConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Implementation of VPLSConfigurationService which reads VPLS configuration
 * from the network configuration service.
 */
@Component(immediate = true)
@Service
public class VplsConfigImpl implements VplsConfigService {
    private static final String VPLS_APP = "org.onosproject.vpls";
    private static final String VPLS = "vpls";
    private static final String EMPTY = "";
    private static final String CONFIG_NULL = "VPLS configuration not defined";
    private static final String APP_ID_NULL = "VPLS application ID is null";
    private static final String CONFIG_CHANGED = "VPLS configuration changed: {}";
    private static final String CHECK_CONFIG =
            "Checking the interface configuration";
    private static final String NET_CONF_EVENT =
            "Received NetworkConfigEvent {}";

    private final Logger log = LoggerFactory.getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected NetworkConfigRegistry registry;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected InterfaceService interfaceService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected NetworkConfigService configService;

    private final Set<String> vplsAffectedByApi = new HashSet<>();

    private VplsAppConfig vplsAppConfig = new VplsAppConfig();

    private SetMultimap<String, String> ifacesOfVpls = HashMultimap.create();
    private SetMultimap<String, String> oldIfacesOfVpls = HashMultimap.create();
    private SetMultimap<String, Interface> vplsIfaces = HashMultimap.create();

    private Map<String, EncapsulationType> vplsEncaps = Maps.newHashMap();

    private final InternalNetworkConfigListener configListener =
            new InternalNetworkConfigListener();

    private ConfigFactory<ApplicationId, VplsAppConfig> vplsConfigFactory =
            new ConfigFactory<ApplicationId, VplsAppConfig>(
                    SubjectFactories.APP_SUBJECT_FACTORY, VplsAppConfig.class, VPLS) {
                @Override
                public VplsAppConfig createConfig() {
                    return new VplsAppConfig();
                }
            };

    private ApplicationId vplsAppId;

    @Activate
    protected void active() {
        configService.addListener(configListener);
        registry.registerConfigFactory(vplsConfigFactory);
        loadConfiguration();
        log.info("Started");
    }

    @Deactivate
    protected  void deactive() {
        registry.unregisterConfigFactory(vplsConfigFactory);
        configService.removeListener(configListener);
        log.info("Stopped");
    }

    @Override
    public void addVpls(String vplsName, Set<String> ifaces, String encap) {
        EncapsulationType encapType = EncapsulationType.enumFromString(encap);

        if (ifacesOfVpls.containsKey(vplsName)) {
            if (ifaces.isEmpty()) {
                return;
            }
            ifaces.forEach(iface -> vplsAppConfig.addIface(vplsName, iface));
            vplsAppConfig.setEncap(vplsName, encapType);
        } else {
            vplsAppConfig.addVpls(new VplsConfig(vplsName, ifaces, encapType));
        }

        vplsAffectedByApi.add(vplsName);
        applyConfig(vplsAppConfig);
    }

    @Override
    public void removeVpls(String vplsName) {
        if (ifacesOfVpls.containsKey(vplsName)) {
            vplsAppConfig.removeVpls(vplsName);
            vplsAffectedByApi.add(vplsName);
            applyConfig(vplsAppConfig);
        }
    }

    @Override
    public void addIface(String vplsName, String iface) {
        if (ifacesOfVpls.containsKey(vplsName)) {
            vplsAppConfig.addIface(vplsName, iface);
            vplsAffectedByApi.add(vplsName);
            applyConfig(vplsAppConfig);
        }
    }

    @Override
    public void setEncap(String vplsName, String encap) {
        EncapsulationType encapType = EncapsulationType.enumFromString(encap);

        if (ifacesOfVpls.containsKey(vplsName)) {
            vplsAppConfig.setEncap(vplsName, encapType);
            vplsAffectedByApi.add(vplsName);
            applyConfig(vplsAppConfig);
        }
    }

    @Override
    public void removeIface(String iface) {
        if (ifacesOfVpls.containsValue(iface)) {
            VplsConfig vpls = vplsAppConfig.vplsFromIface(iface);
            vplsAppConfig.removeIface(vpls, iface);
            vplsAffectedByApi.add(vpls.name());
            applyConfig(vplsAppConfig);
        }
    }

    @Override
    public void cleanVplsConfig() {
        ifacesOfVpls.entries().forEach(e -> {
            vplsAppConfig.removeVpls(e.getKey());
            vplsAffectedByApi.add(e.getKey());
        });
        applyConfig(vplsAppConfig);
    }

    @Override
    public EncapsulationType encap(String vplsName) {
        EncapsulationType encap = null;
        if (vplsEncaps.containsKey(vplsName)) {
            encap = vplsEncaps.get(vplsName);
        }

        return encap;
    }

    @Override
    public Set<String> vplsAffectedByApi() {
        Set<String> vplsNames = ImmutableSet.copyOf(vplsAffectedByApi);
        vplsAffectedByApi.clear();
        return vplsNames;
    }

    @Override
    public Set<Interface> allIfaces() {
        Set<Interface> interfaces = new HashSet<>();
        interfaceService.getInterfaces().stream()
                .filter(iface -> iface.ipAddressesList() == null ||
                        iface.ipAddressesList().isEmpty())
                .forEach(interfaces::add);
        return interfaces;
    }

    @Override
    public Set<Interface> ifaces() {
        Set<Interface> interfaces = new HashSet<>();
        vplsIfaces.values().forEach(interfaces::add);
        return interfaces;
    }

    @Override
    public Set<Interface> ifaces(String vplsName) {
        Set<Interface> vplsInterfaces = new HashSet<>();
        vplsIfaces.get(vplsName).forEach(vplsInterfaces::add);
        return vplsInterfaces;
    }

    @Override
    public Set<String> vplsNames() {
        return ifacesOfVpls.keySet();
    }

    @Override
    public Set<String> vplsNamesOld() {
        return oldIfacesOfVpls.keySet();
    }

    @Override
    public SetMultimap<String, Interface> ifacesByVplsName() {
        return ImmutableSetMultimap.copyOf(vplsIfaces);
    }

    @Override
    public SetMultimap<String, Interface> ifacesByVplsName(VlanId vlan,
                                                           ConnectPoint connectPoint) {
        String vplsName =
                vplsIfaces.entries().stream()
                        .filter(e -> e.getValue().connectPoint().equals(connectPoint))
                        .filter(e -> e.getValue().vlan().equals(vlan))
                        .map(Map.Entry::getKey)
                        .findFirst()
                        .orElse(null);
        SetMultimap<String, Interface> result = HashMultimap.create();
        if (vplsName != null && vplsIfaces.containsKey(vplsName)) {
            vplsIfaces.get(vplsName)
                    .forEach(intf -> result.put(vplsName, intf));
            return result;
        }
        return null;
    }

    @Override
    public Map<String, EncapsulationType> encapByVplsName() {
        return ImmutableMap.copyOf(vplsEncaps);
    }

    /**
     * Retrieves the VPLS configuration from network configuration.
     */
    private void loadConfiguration() {
        loadAppId();

        vplsAppConfig = configService.getConfig(vplsAppId, VplsAppConfig.class);

        if (vplsAppConfig == null) {
            log.warn(CONFIG_NULL);
            configService.addConfig(vplsAppId, VplsAppConfig.class);
            return;
        }

        oldIfacesOfVpls = ifacesOfVpls;
        ifacesOfVpls = getConfigInterfaces();
        vplsIfaces = getConfigCPointsFromIfaces();
        vplsEncaps = getConfigEncap();

        log.debug(CONFIG_CHANGED, ifacesOfVpls);
    }

    /**
     * Retrieves the application identifier from core service.
     */
    private void loadAppId() {
        vplsAppId = coreService.getAppId(VPLS_APP);
        if (vplsAppId == null) {
            log.warn(APP_ID_NULL);
        }
    }

    /**
     * Applies a given configuration to the VPLS application.
     */
    private void applyConfig(VplsAppConfig vplsAppConfig) {
        loadAppId();
        configService.applyConfig(vplsAppId, VplsAppConfig.class, vplsAppConfig.node());
    }

    /**
     * Retrieves the VPLS names and related encapsulation types from the
     * configuration.
     *
     * @return a map of VPLS names and associated encapsulation types
     */
    private Map<String, EncapsulationType> getConfigEncap() {
        Map<String, EncapsulationType> configEncap = new HashMap<>();

        vplsAppConfig.vplss().forEach(vpls -> {
            configEncap.put(vpls.name(), vpls.encap());
        });

        return configEncap;
    }

    /**
     * Retrieves the VPLS names and related interfaces names from the configuration.
     *
     * @return a map of VPLS names and related interface names
     */
    private SetMultimap<String, String> getConfigInterfaces() {
        SetMultimap<String, String> confIntfByVpls =
                HashMultimap.create();

        vplsAppConfig.vplss().forEach(vpls -> {
            if (vpls.ifaces().isEmpty()) {
                confIntfByVpls.put(vpls.name(), EMPTY);
            } else {
                vpls.ifaces().forEach(iface -> confIntfByVpls.put(vpls.name(), iface));
            }
        });

        return confIntfByVpls;
    }

    /**
     * Retrieves the VPLS names and related interfaces from the configuration.
     *
     * @return a map of VPLS names and related interfaces
     */
    private SetMultimap<String, Interface> getConfigCPointsFromIfaces() {
        log.debug(CHECK_CONFIG);

        SetMultimap<String, Interface> confCPointsByIntf =
                HashMultimap.create();

        ifacesOfVpls.entries().forEach(vpls -> {
            interfaceService.getInterfaces()
                    .stream()
                    .filter(intf -> intf.ipAddressesList().isEmpty())
                    .filter(intf -> intf.name().equals(vpls.getValue()))
                    .forEach(intf -> confCPointsByIntf.put(vpls.getKey(), intf));
        });

        return confCPointsByIntf;
    }

    /**
     * Listener for VPLS configuration events.
     */
    private class InternalNetworkConfigListener implements NetworkConfigListener {
        @Override
        public void event(NetworkConfigEvent event) {
            if (event.configClass() == VplsConfigService.CONFIG_CLASS) {
                log.debug(NET_CONF_EVENT, event.configClass());
                switch (event.type()) {
                    case CONFIG_ADDED:
                    case CONFIG_UPDATED:
                    case CONFIG_REMOVED:
                        loadConfiguration();
                        break;
                    default:
                        break;
                }
            }
        }
    }
}
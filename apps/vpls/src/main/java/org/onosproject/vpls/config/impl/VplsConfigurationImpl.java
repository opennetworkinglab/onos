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
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSetMultimap;
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
import org.onosproject.net.config.NetworkConfigRegistry;
import org.onosproject.net.config.NetworkConfigService;
import org.onosproject.net.config.NetworkConfigListener;
import org.onosproject.net.config.NetworkConfigEvent;
import org.onosproject.net.config.ConfigFactory;
import org.onosproject.net.config.basics.SubjectFactories;
import org.onosproject.vpls.config.VplsConfig;
import org.onosproject.vpls.config.VplsNetworkConfig;
import org.onosproject.vpls.config.VplsConfigurationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;

/**
 * Implementation of VPLSConfigurationService which reads VPLS configuration
 * from the network configuration service.
 */
@Component(immediate = true)
@Service
public class VplsConfigurationImpl implements VplsConfigurationService {
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

    private VplsConfig vplsConfig = new VplsConfig();

    private SetMultimap<String, String> ifacesOfVpls = HashMultimap.create();
    private SetMultimap<String, String> oldIfacesOfVpls = HashMultimap.create();
    private SetMultimap<String, Interface> vplsNetworks = HashMultimap.create();

    private final InternalNetworkConfigListener configListener =
            new InternalNetworkConfigListener();

    private ConfigFactory<ApplicationId, VplsConfig> vplsConfigFactory =
            new ConfigFactory<ApplicationId, VplsConfig>(
                    SubjectFactories.APP_SUBJECT_FACTORY, VplsConfig.class, VPLS) {
                @Override
                public VplsConfig createConfig() {
                    return new VplsConfig();
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

    /**
     * Retrieves the VPLS configuration from network configuration.
     */
    private void loadConfiguration() {
        loadAppId();

        vplsConfig = configService.getConfig(vplsAppId, VplsConfig.class);

        if (vplsConfig == null) {
            log.warn(CONFIG_NULL);
            configService.addConfig(vplsAppId, VplsConfig.class);
            return;
        }

        oldIfacesOfVpls = ifacesOfVpls;
        ifacesOfVpls = getConfigInterfaces();
        vplsNetworks = getConfigCPoints();
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
    private void applyConfig(VplsConfig vplsConfig) {
        loadAppId();
        configService.applyConfig(vplsAppId, VplsConfig.class, vplsConfig.node());
    }

    /**
     * Retrieves the VPLS names and associated interfaces names from the configuration.
     *
     * @return a map VPLS names and associated interface names
     */
    private SetMultimap<String, String> getConfigInterfaces() {
        SetMultimap<String, String> confIntfByVpls =
                HashMultimap.create();

        vplsConfig.vplsNetworks().forEach(vpls -> {
            if (vpls.ifaces().isEmpty()) {
                confIntfByVpls.put(vpls.name(), EMPTY);
            } else {
                vpls.ifaces().forEach(iface -> confIntfByVpls.put(vpls.name(), iface));
            }
        });

        return confIntfByVpls;
    }

    /**
     * Retrieves the VPLS names and associated interfaces from the configuration.
     *
     * @return a map VPLS names and associated interfaces
     */
    private SetMultimap<String, Interface> getConfigCPoints() {
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
            if (event.configClass() == VplsConfigurationService.CONFIG_CLASS) {
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

    @Override
    public void addVpls(String name, Set<String> ifaces) {
        VplsNetworkConfig vpls;

        if (ifacesOfVpls.containsKey(name)) {
            if (ifaces.isEmpty()) {
                return;
            }

            ifaces.forEach(iface ->
                    vplsConfig.addInterfaceToVpls(name, iface));
        } else {
            vpls = new VplsNetworkConfig(name, ifaces);
            vplsConfig.addVpls(vpls);
        }

        vplsAffectedByApi.add(name);
        applyConfig(vplsConfig);
    }

    @Override
    public void removeVpls(String name) {
        if (ifacesOfVpls.containsKey(name)) {
            vplsConfig.removeVpls(name);
            vplsAffectedByApi.add(name);
            applyConfig(vplsConfig);
        }
    }

    @Override
    public void addInterfaceToVpls(String name, String iface) {
        if (ifacesOfVpls.containsKey(name)) {
            vplsConfig.addInterfaceToVpls(name, iface);
            vplsAffectedByApi.add(name);
            applyConfig(vplsConfig);
        }
    }

    @Override
    public void removeInterfaceFromVpls(String iface) {
        if (ifacesOfVpls.containsValue(iface)) {
            VplsNetworkConfig vpls = vplsConfig.getVplsFromInterface(iface);
            vplsConfig.removeInterfaceFromVpls(vpls, iface);
            vplsAffectedByApi.add(vpls.name());
            applyConfig(vplsConfig);
        }
    }

    @Override
    public void cleanVpls() {
        ifacesOfVpls.entries().forEach(e -> {
            vplsConfig.removeVpls(e.getKey());
            vplsAffectedByApi.add(e.getKey());
        });
        applyConfig(vplsConfig);
    }

    @Override
    public Set<String> getVplsAffectedByApi() {
        Set<String> vplsNames = ImmutableSet.copyOf(vplsAffectedByApi);

        vplsAffectedByApi.clear();

        return vplsNames;
    }

    @Override
    public Set<Interface> getAllInterfaces() {
        Set<Interface> allInterfaces = new HashSet<>();
        vplsNetworks.values().forEach(allInterfaces::add);

        return allInterfaces;
    }

    @Override
    public Set<Interface> getVplsInterfaces(String name) {
        Set<Interface> vplsInterfaces = new HashSet<>();
        vplsNetworks.get(name).forEach(vplsInterfaces::add);

        return vplsInterfaces;
    }

    @Override
    public Set<String> getAllVpls() {
        return ifacesOfVpls.keySet();
    }

    @Override
    public Set<String> getOldVpls() {
        return oldIfacesOfVpls.keySet();
    }

    @Override
    public SetMultimap<String, Interface> getVplsNetworks() {
        return ImmutableSetMultimap.copyOf(vplsNetworks);
    }

    @Override
    public SetMultimap<String, Interface> getVplsNetwork(VlanId vlan,
                                                        ConnectPoint connectPoint) {
        String vplsNetworkName =
                vplsNetworks.entries().stream()
                        .filter(e -> e.getValue().connectPoint().equals(connectPoint))
                        .filter(e -> e.getValue().vlan().equals(vlan))
                        .map(e -> e.getKey())
                        .findFirst()
                        .orElse(null);
        SetMultimap<String, Interface> result = HashMultimap.create();
        if (vplsNetworkName != null && vplsNetworks.containsKey(vplsNetworkName)) {
            vplsNetworks.get(vplsNetworkName)
                    .forEach(intf -> result.put(vplsNetworkName, intf));
            return result;
        }
        return null;
    }
}

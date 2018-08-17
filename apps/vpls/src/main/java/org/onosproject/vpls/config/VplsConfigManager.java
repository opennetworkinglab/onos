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

package org.onosproject.vpls.config;

import com.google.common.collect.Sets;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.onosproject.cluster.LeadershipService;
import org.onosproject.cluster.NodeId;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.net.intf.Interface;
import org.onosproject.net.intf.InterfaceService;
import org.onosproject.net.config.ConfigFactory;
import org.onosproject.net.config.NetworkConfigEvent;
import org.onosproject.net.config.NetworkConfigListener;
import org.onosproject.net.config.NetworkConfigRegistry;
import org.onosproject.net.config.NetworkConfigService;
import org.onosproject.net.config.basics.SubjectFactories;
import org.onosproject.vpls.VplsManager;
import org.onosproject.vpls.api.VplsData;
import org.onosproject.vpls.api.Vpls;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.onlab.util.Tools.groupedThreads;

/**
 * Component for the management of the VPLS configuration.
 */
@Component(immediate = true)
public class VplsConfigManager {
    private static final Class<VplsAppConfig> CONFIG_CLASS = VplsAppConfig.class;
    private static final String NET_CONF_EVENT = "Received NetworkConfigEvent {}";
    private static final String CONFIG_NULL = "VPLS configuration not defined";
    private static final int INITIAL_RELOAD_CONFIG_DELAY = 0;
    private static final int INITIAL_RELOAD_CONFIG_PERIOD = 1000;
    private static final int NUM_THREADS = 1;
    private static final String VPLS = "vpls";
    private final Logger log = LoggerFactory.getLogger(getClass());

    private final NetworkConfigListener configListener =
            new InternalNetworkConfigListener();

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected NetworkConfigService configService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected InterfaceService interfaceService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected NetworkConfigRegistry registry;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected Vpls vpls;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected LeadershipService leadershipService;

    private ScheduledExecutorService reloadExecutor =
            Executors.newScheduledThreadPool(NUM_THREADS,
                                             groupedThreads("onos/apps/vpls",
                                                            "config-reloader-%d",
                                                            log)
            );

    private ConfigFactory<ApplicationId, VplsAppConfig> vplsConfigFactory =
            new ConfigFactory<ApplicationId, VplsAppConfig>(
                    SubjectFactories.APP_SUBJECT_FACTORY, VplsAppConfig.class, VPLS) {
                @Override
                public VplsAppConfig createConfig() {
                    return new VplsAppConfig();
                }
            };

    protected ApplicationId appId;

    @Activate
    void activate() {
        appId = coreService.registerApplication(VplsManager.VPLS_APP);
        configService.addListener(configListener);

        // Load config when VPLS service started and there is a leader for VPLS;
        // otherwise, try again after a period.
        reloadExecutor.scheduleAtFixedRate(() -> {
            NodeId vplsLeaderNode = leadershipService.getLeader(appId.name());
            if (vpls == null || vplsLeaderNode == null) {
                return;
            }
            reloadConfiguration();
            reloadExecutor.shutdown();
        }, INITIAL_RELOAD_CONFIG_DELAY, INITIAL_RELOAD_CONFIG_PERIOD, TimeUnit.MILLISECONDS);
        registry.registerConfigFactory(vplsConfigFactory);
    }

    @Deactivate
    void deactivate() {
        configService.removeListener(configListener);
        registry.unregisterConfigFactory(vplsConfigFactory);
    }

    /**
     * Retrieves the VPLS configuration from network configuration.
     * Checks difference between new configuration and old configuration.
     */
    private synchronized void reloadConfiguration() {
        VplsAppConfig vplsAppConfig = configService.getConfig(appId, VplsAppConfig.class);

        if (vplsAppConfig == null) {
            log.warn(CONFIG_NULL);

            // If the config is null, removes all VPLS.
            vpls.removeAllVpls();
            return;
        }

        // If there exists a update time in the configuration; it means the
        // configuration was pushed by VPLS store; ignore this configuration.
        long updateTime = vplsAppConfig.updateTime();
        if (updateTime != -1L) {
            return;
        }

        Collection<VplsData> oldVplses = vpls.getAllVpls();
        Collection<VplsData> newVplses;

        // Generates collection of new VPLSs
        newVplses = vplsAppConfig.vplss().stream()
                .map(vplsConfig -> {
                    Set<Interface> interfaces = vplsConfig.ifaces().stream()
                            .map(this::getInterfaceByName)
                            .filter(Objects::nonNull)
                            .collect(Collectors.toSet());
                    VplsData vplsData = VplsData.of(vplsConfig.name(), vplsConfig.encap());
                    vplsData.addInterfaces(interfaces);
                    return vplsData;
                }).collect(Collectors.toSet());

        if (newVplses.containsAll(oldVplses) && oldVplses.containsAll(newVplses)) {
            // no update, ignore
            return;
        }

        // To add or update
        newVplses.forEach(newVplsData -> {
            boolean vplsExists = false;
            for (VplsData oldVplsData : oldVplses) {
                if (oldVplsData.name().equals(newVplsData.name())) {
                    vplsExists = true;

                    // VPLS exists; but need to be updated.
                    if (!oldVplsData.equals(newVplsData)) {
                        // Update VPLS
                        Set<Interface> newInterfaces = newVplsData.interfaces();
                        Set<Interface> oldInterfaces = oldVplsData.interfaces();

                        Set<Interface> ifaceToAdd = newInterfaces.stream()
                                .filter(iface -> !oldInterfaces.contains(iface))
                                .collect(Collectors.toSet());

                        Set<Interface> ifaceToRem = oldInterfaces.stream()
                                .filter(iface -> !newInterfaces.contains(iface))
                                .collect(Collectors.toSet());

                        vpls.addInterfaces(oldVplsData, ifaceToAdd);
                        vpls.removeInterfaces(oldVplsData, ifaceToRem);
                        vpls.setEncapsulationType(oldVplsData, newVplsData.encapsulationType());
                    }
                }
            }
            // VPLS not exist; add new VPLS
            if (!vplsExists) {
                vpls.createVpls(newVplsData.name(), newVplsData.encapsulationType());
                vpls.addInterfaces(newVplsData, newVplsData.interfaces());
            }
        });

        // VPLS not exists in old VPLS configuration; remove it.
        Set<VplsData> vplsToRemove = Sets.newHashSet();
        oldVplses.forEach(oldVpls -> {
            Set<String> newVplsNames = newVplses.stream()
                    .map(VplsData::name)
                    .collect(Collectors.toSet());

            if (!newVplsNames.contains(oldVpls.name())) {
                // To avoid ConcurrentModificationException; do remove after this
                // iteration.
                vplsToRemove.add(oldVpls);
            }
        });
        vplsToRemove.forEach(vpls::removeVpls);
    }

    /**
     * Gets network interface by a given name.
     *
     * @param interfaceName the interface name
     * @return the network interface if there exist with the given name; null
     * otherwise
     */
    private Interface getInterfaceByName(String interfaceName) {
        return interfaceService.getInterfaces().stream()
                .filter(iface -> iface.name().equals(interfaceName))
                .findFirst()
                .orElse(null);
    }

    /**
     * Listener for VPLS configuration events.
     * Reloads VPLS configuration when configuration added or updated.
     * Removes all VPLS when configuration removed or unregistered.
     */
    private class InternalNetworkConfigListener implements NetworkConfigListener {
        @Override
        public void event(NetworkConfigEvent event) {
            if (event.configClass() == CONFIG_CLASS) {
                log.debug(NET_CONF_EVENT, event.configClass());
                switch (event.type()) {
                    case CONFIG_ADDED:
                    case CONFIG_UPDATED:
                        reloadConfiguration();
                        break;
                    case CONFIG_REMOVED:
                    case CONFIG_UNREGISTERED:
                        vpls.removeAllVpls();
                        break;
                    default:
                        break;
                }
            }
        }
    }
}

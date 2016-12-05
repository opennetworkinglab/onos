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
package org.onosproject.vpls;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.SetMultimap;
import com.google.common.collect.Sets;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.onlab.packet.MacAddress;
import org.onlab.packet.VlanId;
import org.onosproject.app.ApplicationService;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.incubator.net.intf.Interface;
import org.onosproject.incubator.net.intf.InterfaceEvent;
import org.onosproject.incubator.net.intf.InterfaceListener;
import org.onosproject.incubator.net.intf.InterfaceService;
import org.onosproject.net.EncapsulationType;
import org.onosproject.net.FilteredConnectPoint;
import org.onosproject.net.Host;
import org.onosproject.net.config.NetworkConfigEvent;
import org.onosproject.net.config.NetworkConfigListener;
import org.onosproject.net.config.NetworkConfigService;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.criteria.Criterion;
import org.onosproject.net.flow.criteria.VlanIdCriterion;
import org.onosproject.net.host.HostEvent;
import org.onosproject.net.host.HostListener;
import org.onosproject.net.host.HostService;
import org.onosproject.net.intent.Intent;
import org.onosproject.net.intent.IntentService;
import org.onosproject.net.intent.Key;
import org.onosproject.routing.IntentSynchronizationService;
import org.onosproject.vpls.config.VplsConfigService;
import org.slf4j.Logger;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.onosproject.vpls.IntentInstaller.PREFIX_BROADCAST;
import static org.onosproject.vpls.IntentInstaller.PREFIX_UNICAST;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Application to create L2 broadcast overlay networks using VLANs.
 */
@Component(immediate = true)
public class Vpls {
    static final String VPLS_APP = "org.onosproject.vpls";

    private static final String HOST_FCP_NOT_FOUND =
            "Filtered connected point for host {} not found";
    private static final String HOST_EVENT = "Received HostEvent {}";
    private static final String INTF_CONF_EVENT =
            "Received InterfaceConfigEvent {}";
    private static final String NET_CONF_EVENT =
            "Received NetworkConfigEvent {}";

    private final Logger log = getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ApplicationService applicationService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected HostService hostService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected IntentService intentService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected InterfaceService interfaceService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected IntentSynchronizationService intentSynchronizer;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected NetworkConfigService configService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected VplsConfigService vplsConfigService;

    private final HostListener hostListener = new InternalHostListener();

    private final InternalInterfaceListener interfaceListener =
            new InternalInterfaceListener();

    private final InternalNetworkConfigListener configListener =
            new InternalNetworkConfigListener();

    private IntentInstaller intentInstaller;

    private ApplicationId appId;


    @Activate
    public void activate() {
        appId = coreService.registerApplication(VPLS_APP);

        intentInstaller = new IntentInstaller(appId,
                                              intentService,
                                              intentSynchronizer);

        applicationService.registerDeactivateHook(appId, () -> {
            intentSynchronizer.removeIntentsByAppId(appId);
        });

        hostService.addListener(hostListener);
        interfaceService.addListener(interfaceListener);
        configService.addListener(configListener);

        setupConnectivity(false);

        log.info("Activated");
    }

    @Deactivate
    public void deactivate() {
        configService.removeListener(configListener);
        intentSynchronizer.removeIntentsByAppId(appId);
        log.info("Deactivated");
    }

    /**
     * Sets up connectivity for all VPLSs.
     *
     * @param isNetworkConfigEvent true if this function is triggered
     *                             by NetworkConfigEvent; false otherwise
     */
    private void setupConnectivity(boolean isNetworkConfigEvent) {
        SetMultimap<String, Interface> networkInterfaces =
                vplsConfigService.ifacesByVplsName();

        Set<String> vplsAffectedByApi =
                new HashSet<>(vplsConfigService.vplsAffectedByApi());

        if (isNetworkConfigEvent && vplsAffectedByApi.isEmpty()) {
            vplsAffectedByApi.addAll(vplsConfigService.vplsNamesOld());
        }

        networkInterfaces.asMap().forEach((vplsName, interfaces) -> {
            Set<Host> hosts = Sets.newHashSet();
            interfaces.forEach(intf -> {
                // Add hosts that belongs to the specific VPLS
                hostService.getConnectedHosts(intf.connectPoint())
                        .stream()
                        .filter(host -> host.vlan().equals(intf.vlan()))
                        .forEach(hosts::add);
            });

            EncapsulationType encap =
                    vplsConfigService.encap(vplsName);

            setupConnectivity(vplsName, interfaces, hosts, encap,
                              vplsAffectedByApi.contains(vplsName));
            vplsAffectedByApi.remove(vplsName);
        });

        if (!vplsAffectedByApi.isEmpty()) {
            for (String vplsName : vplsAffectedByApi) {
                withdrawIntents(vplsName, Lists.newArrayList());
            }
        }
    }

    /**
     * Sets up connectivity for specific VPLS.
     *
     * @param vplsName      the VPLS name
     * @param interfaces    the interfaces that belong to the VPLS
     * @param hosts         the hosts that belong to the VPLS
     * @param encap         the encapsulation type
     * @param affectedByApi true if this function is triggered from the APIs;
     *                      false otherwise
     */
    private void setupConnectivity(String vplsName,
                                   Collection<Interface> interfaces,
                                   Set<Host> hosts,
                                   EncapsulationType encap,
                                   boolean affectedByApi) {

        List<Intent> intents = Lists.newArrayList();
        List<Key> keys = Lists.newArrayList();
        Set<FilteredConnectPoint> fcPoints = buildFCPoints(interfaces);

        intents.addAll(buildBroadcastIntents(
                vplsName, fcPoints, encap, affectedByApi));
        intents.addAll(buildUnicastIntents(
                vplsName, hosts, fcPoints, encap, affectedByApi));

        if (affectedByApi) {
            intents.forEach(intent -> keys.add(intent.key()));
            withdrawIntents(vplsName, keys);
        }

        intentInstaller.submitIntents(intents);
    }

    /**
     * Withdraws intents belonging to a VPLS, given a VPLS name.
     *
     * @param vplsName the VPLS name
     * @param keys     the keys of the intents to be installed
     */
    private void withdrawIntents(String vplsName, List<Key> keys) {
        List<Intent> intents = Lists.newArrayList();

        intentInstaller.getIntentsFromVpls(vplsName)
                .forEach(intent -> {
                    if (!keys.contains(intent.key())) {
                        intents.add(intent);
                    }
                });

        intentInstaller.withdrawIntents(intents);
    }

    /**
     * Sets up broadcast intents between any given filtered connect point.
     *
     * @param vplsName      the VPLS name
     * @param fcPoints      the set of filtered connect points
     * @param encap         the encapsulation type
     * @param affectedByApi true if the function triggered from APIs;
     *                      false otherwise
     * @return the set of broadcast intents
     */
    private Set<Intent> buildBroadcastIntents(String vplsName,
                                              Set<FilteredConnectPoint> fcPoints,
                                              EncapsulationType encap,
                                              boolean affectedByApi) {
        Set<Intent> intents = Sets.newHashSet();
        fcPoints.forEach(point -> {
            Set<FilteredConnectPoint> otherPoints =
                    fcPoints.stream()
                            .filter(fcp -> !fcp.equals(point))
                            .collect(Collectors.toSet());

            Key brcKey = intentInstaller.buildKey(PREFIX_BROADCAST,
                                                  point.connectPoint(),
                                                  vplsName,
                                                  MacAddress.BROADCAST);

            if ((!intentInstaller.intentExists(brcKey) || affectedByApi)
                    && !otherPoints.isEmpty()) {
                intents.add(intentInstaller.buildBrcIntent(brcKey,
                                                           point,
                                                           otherPoints,
                                                           encap));
            }
        });

        return ImmutableSet.copyOf(intents);
    }

    /**
     * Sets up unicast intents between any given filtered connect point.
     *
     * @param vplsName      the VPLS name
     * @param hosts         the set of destination hosts
     * @param fcPoints      the set of filtered connect points
     * @param encap         the encapsulation type
     * @param affectedByApi true if the function triggered from APIs;
     *                      false otherwise
     * @return the set of unicast intents
     */
    private Set<Intent> buildUnicastIntents(String vplsName,
                                            Set<Host> hosts,
                                            Set<FilteredConnectPoint> fcPoints,
                                            EncapsulationType encap,
                                            boolean affectedByApi) {
        Set<Intent> intents = Sets.newHashSet();
        hosts.forEach(host -> {
            FilteredConnectPoint hostPoint = getHostPoint(host, fcPoints);

            if (hostPoint == null) {
                log.warn(HOST_FCP_NOT_FOUND, host);
                return;
            }

            Set<FilteredConnectPoint> otherPoints =
                    fcPoints.stream()
                            .filter(fcp -> !fcp.equals(hostPoint))
                            .collect(Collectors.toSet());

            Key uniKey = intentInstaller.buildKey(PREFIX_UNICAST,
                                                  host.location(),
                                                  vplsName,
                                                  host.mac());

            if ((!intentInstaller.intentExists(uniKey) || affectedByApi) &&
                    !otherPoints.isEmpty()) {
                intents.add(intentInstaller.buildUniIntent(uniKey,
                                                           otherPoints,
                                                           hostPoint,
                                                           host,
                                                           encap));
            }
        });

        return ImmutableSet.copyOf(intents);
    }

    /**
     * Returns the filtered connect point associated to a given host.
     *
     * @param host the target host
     * @param fcps the filtered connected points
     * @return null if not found; the filtered connect point otherwise
     */
    private FilteredConnectPoint getHostPoint(Host host,
                                              Set<FilteredConnectPoint> fcps) {
        return fcps.stream()
                .filter(fcp -> fcp.connectPoint().equals(host.location()))
                .filter(fcp -> {
                    VlanIdCriterion vlanCriterion =
                            (VlanIdCriterion) fcp.trafficSelector().
                                    getCriterion(Criterion.Type.VLAN_VID);
                    return vlanCriterion == null ||
                            vlanCriterion.vlanId().equals(host.vlan());
                })
                .findFirst()
                .orElse(null);
    }

    /**
     * Computes a set of filtered connect points from a list of given interfaces.
     *
     * @param interfaces the interfaces to compute
     * @return the set of filtered connect points
     */
    private Set<FilteredConnectPoint> buildFCPoints(Collection<Interface> interfaces) {
        // Build all filtered connected points in the VPLS
        return interfaces
                .stream()
                .map(intf -> {
                    TrafficSelector.Builder selectorBuilder =
                            DefaultTrafficSelector.builder();
                    if (!intf.vlan().equals(VlanId.NONE)) {
                        selectorBuilder.matchVlanId(intf.vlan());
                    }
                    return new FilteredConnectPoint(intf.connectPoint(),
                                                    selectorBuilder.build());
                })
                .collect(Collectors.toSet());
    }

    /**
     * Listener for host events.
     */
    private class InternalHostListener implements HostListener {
        @Override
        public void event(HostEvent event) {
            log.debug(HOST_EVENT, event);
            switch (event.type()) {
                case HOST_ADDED:
                case HOST_UPDATED:
                case HOST_REMOVED:
                    setupConnectivity(false);
                    break;

                default:
                    break;
            }
        }
    }

    /**
     * Listener for interface configuration events.
     */
    private class InternalInterfaceListener implements InterfaceListener {
        @Override
        public void event(InterfaceEvent event) {
            log.debug(INTF_CONF_EVENT, event);
            switch (event.type()) {
                case INTERFACE_ADDED:
                case INTERFACE_UPDATED:
                case INTERFACE_REMOVED:
                    setupConnectivity(false);
                    break;
                default:
                    break;
            }
        }
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
                        setupConnectivity(true);
                        break;
                    default:
                        break;
                }
            }
        }
    }
}

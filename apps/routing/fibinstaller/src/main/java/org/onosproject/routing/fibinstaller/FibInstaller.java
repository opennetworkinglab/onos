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

package org.onosproject.routing.fibinstaller;

import com.google.common.collect.ConcurrentHashMultiset;
import com.google.common.collect.Maps;
import com.google.common.collect.Multiset;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Modified;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.onlab.packet.Ethernet;
import org.onlab.packet.IpAddress;
import org.onlab.packet.IpPrefix;
import org.onlab.packet.MacAddress;
import org.onlab.packet.VlanId;
import org.onlab.util.Tools;
import org.onosproject.app.ApplicationService;
import org.onosproject.cfg.ComponentConfigService;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.incubator.net.config.basics.McastConfig;
import org.onosproject.incubator.net.intf.Interface;
import org.onosproject.incubator.net.intf.InterfaceService;
import org.onosproject.incubator.net.routing.ResolvedRoute;
import org.onosproject.incubator.net.routing.Route;
import org.onosproject.incubator.net.routing.RouteEvent;
import org.onosproject.incubator.net.routing.RouteListener;
import org.onosproject.incubator.net.routing.RouteService;
import org.onosproject.net.DeviceId;
import org.onosproject.net.config.ConfigFactory;
import org.onosproject.net.config.NetworkConfigEvent;
import org.onosproject.net.config.NetworkConfigListener;
import org.onosproject.net.config.NetworkConfigRegistry;
import org.onosproject.net.config.NetworkConfigService;
import org.onosproject.net.config.basics.SubjectFactories;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.flow.criteria.Criteria;
import org.onosproject.net.flowobjective.DefaultFilteringObjective;
import org.onosproject.net.flowobjective.DefaultForwardingObjective;
import org.onosproject.net.flowobjective.DefaultNextObjective;
import org.onosproject.net.flowobjective.DefaultObjectiveContext;
import org.onosproject.net.flowobjective.FilteringObjective;
import org.onosproject.net.flowobjective.FlowObjectiveService;
import org.onosproject.net.flowobjective.ForwardingObjective;
import org.onosproject.net.flowobjective.NextObjective;
import org.onosproject.net.flowobjective.ObjectiveContext;
import org.onosproject.routing.InterfaceProvisionRequest;
import org.onosproject.routing.NextHop;
import org.onosproject.routing.NextHopGroupKey;
import org.onosproject.routing.Router;
import org.onosproject.routing.RouterInfo;
import org.onosproject.routing.RoutingService;
import org.onosproject.routing.config.RoutingConfiguration;
import org.onosproject.routing.config.RoutersConfig;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Dictionary;
import java.util.Map;
import java.util.Set;

/**
 * Programs routes to a single OpenFlow switch.
 */
@Component(immediate = true)
public class FibInstaller {

    private final Logger log = LoggerFactory.getLogger(getClass());
    private static final String APP_NAME = "org.onosproject.fibinstaller";

    private static final int PRIORITY_OFFSET = 100;
    private static final int PRIORITY_MULTIPLIER = 5;

    // FIXME: This should be eliminated when we have an API in SR that
    //        programs the fabric switches for VR
    public static final short ASSIGNED_VLAN = 4094;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected RouteService routeService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected InterfaceService interfaceService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected NetworkConfigService networkConfigService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected NetworkConfigRegistry networkConfigRegistry;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ComponentConfigService componentConfigService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected FlowObjectiveService flowObjectiveService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DeviceService deviceService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ApplicationService applicationService;

    @Property(name = "routeToNextHop", boolValue = false,
            label = "Install a /32 or /128 route to each next hop")
    private boolean routeToNextHop = false;

    // Device id of data-plane switch - should be learned from config
    private DeviceId deviceId;

    private Router interfaceManager;

    private ApplicationId coreAppId;
    private ApplicationId routerAppId;
    private ApplicationId fibAppId;

    // Reference count for how many times a next hop is used by a route
    private final Multiset<IpAddress> nextHopsCount = ConcurrentHashMultiset.create();

    // Mapping from prefix to its current next hop
    private final Map<IpPrefix, IpAddress> prefixToNextHop = Maps.newHashMap();

    // Mapping from next hop IP to next hop object containing group info
    private final Map<IpAddress, Integer> nextHops = Maps.newHashMap();

    private final InternalRouteListener routeListener = new InternalRouteListener();
    private final InternalNetworkConfigListener configListener = new InternalNetworkConfigListener();

    private ConfigFactory<ApplicationId, McastConfig> mcastConfigFactory =
            new ConfigFactory<ApplicationId, McastConfig>(SubjectFactories.APP_SUBJECT_FACTORY,
                    McastConfig.class, "multicast") {
                @Override
                public McastConfig createConfig() {
                    return new McastConfig();
                }
            };

    @Activate
    protected void activate(ComponentContext context) {
        componentConfigService.registerProperties(getClass());
        modified(context);

        RoutingConfiguration.register(networkConfigRegistry);

        coreAppId = coreService.registerApplication(CoreService.CORE_APP_NAME);
        routerAppId = coreService.registerApplication(RoutingService.ROUTER_APP_ID);
        fibAppId = coreService.registerApplication(APP_NAME);

        networkConfigRegistry.registerConfigFactory(mcastConfigFactory);

        networkConfigService.addListener(configListener);

        processRouterConfig();

        applicationService.registerDeactivateHook(fibAppId, () -> cleanUp());

        log.info("Started");
    }

    @Deactivate
    protected void deactivate() {
        networkConfigService.removeListener(configListener);

        RoutingConfiguration.unregister(networkConfigRegistry);

        componentConfigService.unregisterProperties(getClass(), false);

        log.info("Stopped");
    }

    @Modified
    protected void modified(ComponentContext context) {
        Dictionary<?, ?> properties = context.getProperties();
        if (properties == null) {
            return;
        }

        String strRouteToNextHop = Tools.get(properties, "routeToNextHop");
        routeToNextHop = Boolean.parseBoolean(strRouteToNextHop);

        log.info("routeToNextHop set to {}", routeToNextHop);
    }

    private void processRouterConfig() {
        Set<RoutersConfig.Router> routerConfigs =
                RoutingConfiguration.getRouterConfigurations(networkConfigService, routerAppId);
        if (routerConfigs.isEmpty()) {
            log.info("Router config not available");
            return;
        }
        RoutersConfig.Router routerConfig = routerConfigs.stream().findFirst().get();

        if (interfaceManager == null) {
            deviceId = routerConfig.controlPlaneConnectPoint().deviceId();
            log.info("Router device ID is {}", deviceId);

            routeService.addListener(routeListener);

            interfaceManager = createRouter(RouterInfo.from(routerConfig));
        } else {
            interfaceManager.changeConfiguration(RouterInfo.from(routerConfig), false);
        }
    }

    /**
     * Removes filtering objectives and routes before deactivate.
     */
    private void cleanUp() {
        //remove the route listener
        routeService.removeListener(routeListener);

        //clean up the routes.
        prefixToNextHop.entrySet().stream()
                .map(e -> new Route(Route.Source.UNDEFINED, e.getKey(), e.getValue()))
                .forEach(this::deleteRoute);

        if (interfaceManager != null) {
            interfaceManager.cleanup();
        }
    }

    private Router createRouter(RouterInfo info) {
        return new Router(
                info,
                interfaceService,
                deviceService,
                this::provisionInterface,
                this::unprovisionInterface,
                false);
    }

    private void updateRoute(ResolvedRoute route) {
        addNextHop(route);

        Integer nextId;
        synchronized (this) {
            nextId = nextHops.get(route.nextHop());
        }

        flowObjectiveService.forward(deviceId,
                generateRibForwardingObj(route.prefix(), nextId).add());
        log.trace("Sending forwarding objective {} -> nextId:{}", route, nextId);
    }

    private synchronized void deleteRoute(ResolvedRoute route) {
        deleteRoute(route.route());
    }

    private void deleteRoute(Route route) {
        //Integer nextId = nextHops.get(route.nextHop());

        /* Group group = deleteNextHop(route.prefix());
        if (group == null) {
            log.warn("Group not found when deleting {}", route);
            return;
        }*/

        flowObjectiveService.forward(deviceId,
                generateRibForwardingObj(route.prefix(), null).remove());
    }

    private ForwardingObjective.Builder generateRibForwardingObj(IpPrefix prefix,
                                                                 Integer nextId) {
        TrafficSelector selector = buildIpSelectorFromIpPrefix(prefix).build();
        int priority = prefix.prefixLength() * PRIORITY_MULTIPLIER + PRIORITY_OFFSET;

        ForwardingObjective.Builder fwdBuilder = DefaultForwardingObjective.builder()
                .fromApp(fibAppId)
                .makePermanent()
                .withSelector(selector)
                .withPriority(priority)
                .withFlag(ForwardingObjective.Flag.SPECIFIC);

        if (nextId == null) {
            // Route withdraws are not specified with next hops. Generating
            // dummy treatment as there is no equivalent nextId info.
            fwdBuilder.withTreatment(DefaultTrafficTreatment.builder().build());
        } else {
            fwdBuilder.nextStep(nextId);
        }
        return fwdBuilder;
    }

    /**
     * Method to build IPv4 or IPv6 selector.
     *
     * @param prefixToMatch the prefix to match
     * @return the traffic selector builder
     */
    private TrafficSelector.Builder buildIpSelectorFromIpPrefix(IpPrefix prefixToMatch) {
        TrafficSelector.Builder selectorBuilder = DefaultTrafficSelector.builder();
        // If the prefix is IPv4
        if (prefixToMatch.isIp4()) {
            selectorBuilder.matchEthType(Ethernet.TYPE_IPV4);
            selectorBuilder.matchIPDst(prefixToMatch);
            return selectorBuilder;
        }
        // If the prefix is IPv6
        selectorBuilder.matchEthType(Ethernet.TYPE_IPV6);
        selectorBuilder.matchIPv6Dst(prefixToMatch);
        return selectorBuilder;
    }

    private synchronized void addNextHop(ResolvedRoute route) {
        prefixToNextHop.put(route.prefix(), route.nextHop());
        if (nextHopsCount.count(route.nextHop()) == 0) {
            // There was no next hop in the multiset
            Interface egressIntf = interfaceService.getMatchingInterface(route.nextHop());
            if (egressIntf == null) {
                log.warn("no egress interface found for {}", route);
                return;
            }

            NextHopGroupKey groupKey = new NextHopGroupKey(route.nextHop());

            NextHop nextHop = new NextHop(route.nextHop(), route.nextHopMac(), groupKey);

            TrafficTreatment.Builder treatment = DefaultTrafficTreatment.builder()
                    .setEthSrc(egressIntf.mac())
                    .setEthDst(nextHop.mac());

            TrafficSelector.Builder metabuilder = null;
            if (!egressIntf.vlan().equals(VlanId.NONE)) {
                treatment.pushVlan()
                        .setVlanId(egressIntf.vlan())
                        .setVlanPcp((byte) 0);
            } else {
                // untagged outgoing port may require internal vlan in some pipelines
                metabuilder = DefaultTrafficSelector.builder();
                metabuilder.matchVlanId(VlanId.vlanId(ASSIGNED_VLAN));
            }

            treatment.setOutput(egressIntf.connectPoint().port());

            int nextId = flowObjectiveService.allocateNextId();
            NextObjective.Builder nextBuilder = DefaultNextObjective.builder()
                    .withId(nextId)
                    .addTreatment(treatment.build())
                    .withType(NextObjective.Type.SIMPLE)
                    .fromApp(fibAppId);
            if (metabuilder != null) {
                nextBuilder.withMeta(metabuilder.build());
            }

            NextObjective nextObjective = nextBuilder.add(); // TODO add callbacks
            flowObjectiveService.next(deviceId, nextObjective);

            nextHops.put(nextHop.ip(), nextId);

            if (routeToNextHop) {
                // Install route to next hop
                ForwardingObjective fob =
                        generateRibForwardingObj(IpPrefix.valueOf(route.nextHop(), 32), nextId).add();
                flowObjectiveService.forward(deviceId, fob);
            }
        }

        nextHopsCount.add(route.nextHop());
    }

    /*private synchronized Group deleteNextHop(IpPrefix prefix) {
        IpAddress nextHopIp = prefixToNextHop.remove(prefix);
        NextHop nextHop = nextHops.get(nextHopIp);
        if (nextHop == null) {
            log.warn("No next hop found when removing prefix {}", prefix);
            return null;
        }

        Group group = groupService.getGroup(deviceId,
                                            new DefaultGroupKey(appKryo.
                                                                serialize(nextHop.group())));

        // FIXME disabling group deletes for now until we verify the logic is OK
        if (nextHopsCount.remove(nextHopIp, 1) <= 1) {
            // There was one or less next hops, so there are now none

            log.debug("removing group for next hop {}", nextHop);

            nextHops.remove(nextHopIp);

            groupService.removeGroup(deviceId,
                                     new DefaultGroupKey(appKryo.build().serialize(nextHop.group())),
                                     appId);
        }

        return group;
    }*/

    private void provisionInterface(InterfaceProvisionRequest intf) {
        updateInterfaceFilters(intf, true);
    }

    private void unprovisionInterface(InterfaceProvisionRequest intf) {
        updateInterfaceFilters(intf, false);
    }

    /**
     * Installs or removes flow objectives relating to an interface.
     *
     * @param intf interface to update objectives for
     * @param install true to install the objectives, false to remove them
     */
    private void updateInterfaceFilters(InterfaceProvisionRequest intf, boolean install) {
        updateFilteringObjective(intf, install);
        updateMcastFilteringObjective(intf, install);
    }

    /**
     * Installs or removes unicast filtering objectives relating to an interface.
     *
     * @param routerIntf interface to update objectives for
     * @param install true to install the objectives, false to remove them
     */
    private void updateFilteringObjective(InterfaceProvisionRequest routerIntf, boolean install) {
        Interface intf = routerIntf.intf();
        VlanId assignedVlan = (egressVlan().equals(VlanId.NONE)) ?
                VlanId.vlanId(ASSIGNED_VLAN) :
                egressVlan();

        FilteringObjective.Builder fob = DefaultFilteringObjective.builder();
        // first add filter for the interface
        fob.withKey(Criteria.matchInPort(intf.connectPoint().port()))
            .addCondition(Criteria.matchEthDst(intf.mac()))
            .addCondition(Criteria.matchVlanId(intf.vlan()));
        fob.withPriority(PRIORITY_OFFSET);
        if (intf.vlan() == VlanId.NONE) {
            TrafficTreatment tt = DefaultTrafficTreatment.builder()
                    .pushVlan().setVlanId(assignedVlan).build();
            fob.withMeta(tt);
        }
        fob.permit().fromApp(fibAppId);
        sendFilteringObjective(install, fob, intf);

        // then add the same mac/vlan filters for control-plane connect point
        fob.withKey(Criteria.matchInPort(routerIntf.controlPlaneConnectPoint().port()));
        sendFilteringObjective(install, fob, intf);
    }

    /**
     * Installs or removes multicast filtering objectives relating to an interface.
     *
     * @param routerIntf interface to update objectives for
     * @param install true to install the objectives, false to remove them
     */
    private void updateMcastFilteringObjective(InterfaceProvisionRequest routerIntf, boolean install) {
        Interface intf = routerIntf.intf();
        VlanId assignedVlan = (egressVlan().equals(VlanId.NONE)) ?
                VlanId.vlanId(ASSIGNED_VLAN) :
                egressVlan();

        FilteringObjective.Builder fob = DefaultFilteringObjective.builder();
        // first add filter for the interface
        fob.withKey(Criteria.matchInPort(intf.connectPoint().port()))
                .addCondition(Criteria.matchEthDstMasked(MacAddress.IPV4_MULTICAST,
                        MacAddress.IPV4_MULTICAST_MASK))
                .addCondition(Criteria.matchVlanId(ingressVlan()));
        fob.withPriority(PRIORITY_OFFSET);
        TrafficTreatment tt = DefaultTrafficTreatment.builder()
                .pushVlan().setVlanId(assignedVlan).build();
        fob.withMeta(tt);

        fob.permit().fromApp(fibAppId);
        sendFilteringObjective(install, fob, intf);
    }

    private void sendFilteringObjective(boolean install, FilteringObjective.Builder fob,
                                        Interface intf) {

        ObjectiveContext context = new DefaultObjectiveContext(
                (objective) -> log.info("Installed filter for interface {}", intf),
                (objective, error) ->
                        log.error("Failed to install filter for interface {}: {}", intf, error));

        FilteringObjective filter = install ? fob.add(context) : fob.remove(context);

        flowObjectiveService.filter(deviceId, filter);
    }

    private VlanId ingressVlan() {
        McastConfig mcastConfig =
                networkConfigService.getConfig(coreAppId, McastConfig.class);
        return (mcastConfig != null) ? mcastConfig.ingressVlan() : VlanId.NONE;
    }

    private VlanId egressVlan() {
        McastConfig mcastConfig =
                networkConfigService.getConfig(coreAppId, McastConfig.class);
        return (mcastConfig != null) ? mcastConfig.egressVlan() : VlanId.NONE;
    }

    /**
     * Listener for route changes.
     */
    private class InternalRouteListener implements RouteListener {
        @Override
        public void event(RouteEvent event) {
            ResolvedRoute route = event.subject();
            switch (event.type()) {
            case ROUTE_ADDED:
            case ROUTE_UPDATED:
                updateRoute(route);
                break;
            case ROUTE_REMOVED:
                deleteRoute(route);
                break;
            default:
                break;
            }
        }
    }

    /**
     * Listener for network config events.
     */
    private class InternalNetworkConfigListener implements NetworkConfigListener {
        @Override
        public void event(NetworkConfigEvent event) {
            if (event.configClass().equals(RoutingService.ROUTER_CONFIG_CLASS)) {
                switch (event.type()) {
                case CONFIG_ADDED:
                case CONFIG_UPDATED:
                    processRouterConfig();
                    break;
                case CONFIG_REGISTERED:
                    break;
                case CONFIG_UNREGISTERED:
                    break;
                case CONFIG_REMOVED:
                    cleanUp();
                    break;
                default:
                    break;
                }
            }
        }
    }
}

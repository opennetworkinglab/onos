/*
 * Copyright 2015 Open Networking Laboratory
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
package org.onosproject.bgprouter;

import com.google.common.collect.ConcurrentHashMultiset;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multiset;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.onlab.packet.Ethernet;
import org.onlab.packet.IpAddress;
import org.onlab.packet.IpPrefix;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.net.config.NetworkConfigService;
import org.onosproject.incubator.net.intf.Interface;
import org.onosproject.incubator.net.intf.InterfaceService;
import org.onosproject.net.DeviceId;
import org.onosproject.net.device.DeviceEvent;
import org.onosproject.net.device.DeviceListener;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.flow.criteria.Criteria;
import org.onosproject.net.flowobjective.DefaultFilteringObjective;
import org.onosproject.net.flowobjective.DefaultForwardingObjective;
import org.onosproject.net.flowobjective.DefaultNextObjective;
import org.onosproject.net.flowobjective.FilteringObjective;
import org.onosproject.net.flowobjective.FlowObjectiveService;
import org.onosproject.net.flowobjective.ForwardingObjective;
import org.onosproject.net.flowobjective.NextObjective;
import org.onosproject.net.flowobjective.Objective;
import org.onosproject.net.flowobjective.ObjectiveContext;
import org.onosproject.net.flowobjective.ObjectiveError;
import org.onosproject.net.packet.PacketService;
import org.onosproject.routing.FibEntry;
import org.onosproject.routing.FibListener;
import org.onosproject.routing.FibUpdate;
import org.onosproject.routing.RoutingService;
import org.onosproject.routing.config.BgpConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/* For test only - will be removed before Cardinal release
import org.onlab.packet.Ip4Address;
import org.onlab.packet.Ip4Prefix;
import org.onlab.packet.MacAddress;
import java.util.Collections;
import static org.onlab.util.Tools.delay;
*/

/**
 * BgpRouter component.
 */
@Component(immediate = true)
public class BgpRouter {

    private static final Logger log = LoggerFactory.getLogger(BgpRouter.class);

    private static final String BGP_ROUTER_APP = "org.onosproject.bgprouter";

    private static final int PRIORITY_OFFSET = 100;
    private static final int PRIORITY_MULTIPLIER = 5;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected RoutingService routingService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected InterfaceService interfaceService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected NetworkConfigService networkConfigService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected PacketService packetService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected FlowObjectiveService flowObjectiveService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DeviceService deviceService;

    private ApplicationId appId;

    // Reference count for how many times a next hop is used by a route
    private final Multiset<IpAddress> nextHopsCount = ConcurrentHashMultiset.create();

    // Mapping from prefix to its current next hop
    private final Map<IpPrefix, IpAddress> prefixToNextHop = Maps.newHashMap();

    // Mapping from next hop IP to next hop object containing group info
    private final Map<IpAddress, Integer> nextHops = Maps.newHashMap();

    // Stores FIB updates that are waiting for groups to be set up
    private final Multimap<NextHopGroupKey, FibEntry> pendingUpdates = HashMultimap.create();

    // Device id of data-plane switch - should be learned from config
    private DeviceId deviceId;

    // Device id of control-plane switch (OVS) connected to BGP Speaker - should be
    // learned from config
    private DeviceId ctrlDeviceId;

    // Responsible for handling BGP traffic (encapsulated within OF messages)
    // between the data-plane switch and the Quagga VM using a control plane OVS.
    private TunnellingConnectivityManager connectivityManager;

    private DeviceListener deviceListener;
    private IcmpHandler icmpHandler;

    @Activate
    protected void activate() {
        appId = coreService.registerApplication(BGP_ROUTER_APP);

        ApplicationId routerAppId = coreService.getAppId(RoutingService.ROUTER_APP_ID);
        BgpConfig bgpConfig =
                networkConfigService.getConfig(routerAppId, RoutingService.CONFIG_CLASS);

        if (bgpConfig == null) {
            log.error("No BgpConfig found");
            return;
        }

        getDeviceConfiguration(bgpConfig);

        connectivityManager = new TunnellingConnectivityManager(appId,
                                                                bgpConfig,
                                                                interfaceService,
                                                                packetService,
                                                                flowObjectiveService);

        icmpHandler = new IcmpHandler(interfaceService, packetService);
        deviceListener = new InnerDeviceListener();
        routingService.addFibListener(new InternalFibListener());
        routingService.start();
        deviceService.addListener(deviceListener);
        connectivityManager.start();
        icmpHandler.start();

        // Initialize devices now if they are already connected
        if (deviceService.isAvailable(deviceId)) {
            processIntfFilters(true, interfaceService.getInterfaces());
        }

        if (deviceService.isAvailable(ctrlDeviceId)) {
            connectivityManager.notifySwitchAvailable();
        }

        log.info("BgpRouter started");
    }

    @Deactivate
    protected void deactivate() {
        routingService.stop();
        connectivityManager.stop();
        icmpHandler.stop();
        deviceService.removeListener(deviceListener);
        //processIntfFilters(false, configService.getInterfaces()); //TODO necessary?
        log.info("BgpRouter stopped");
    }

    private void getDeviceConfiguration(BgpConfig bgpConfig) {
        Optional<BgpConfig.BgpSpeakerConfig> bgpSpeaker =
                bgpConfig.bgpSpeakers().stream().findAny();

        if (!bgpSpeaker.isPresent()) {
            log.error("BGP speaker configuration not found");
            return;
        }

        ctrlDeviceId = bgpSpeaker.get().connectPoint().deviceId();

        Optional<IpAddress> peerAddress =
                bgpSpeaker.get().peers().stream().findAny();

        if (!peerAddress.isPresent()) {
            log.error("BGP speaker must have peers configured");
            return;
        }

        Interface intf = interfaceService.getMatchingInterface(peerAddress.get());

        if (intf == null) {
            log.error("No interface found for peer");
            return;
        }

        // Assume all peers are configured on the same device - this is required
        // by the BGP router
        deviceId = intf.connectPoint().deviceId();

        log.info("Router dpid: {}", deviceId);
        log.info("Control Plane OVS dpid: {}", ctrlDeviceId);
    }

    private void updateFibEntry(Collection<FibUpdate> updates) {
        Map<FibEntry, Integer> toInstall = new HashMap<>(updates.size());

        for (FibUpdate update : updates) {
            FibEntry entry = update.entry();

            addNextHop(entry);

            Integer nextId;
            synchronized (pendingUpdates) {
                nextId = nextHops.get(entry.nextHopIp());
            }

            toInstall.put(update.entry(), nextId);
        }

        installFlows(toInstall);
    }

    private void installFlows(Map<FibEntry, Integer> entriesToInstall) {

        for (Map.Entry<FibEntry, Integer> entry : entriesToInstall.entrySet()) {
            FibEntry fibEntry = entry.getKey();
            Integer nextId = entry.getValue();

            flowObjectiveService.forward(deviceId,
                generateRibForwardingObj(fibEntry.prefix(), nextId).add());
            log.trace("Sending forwarding objective {} -> nextId:{}", fibEntry, nextId);
        }

    }

    private synchronized void deleteFibEntry(Collection<FibUpdate> withdraws) {

        for (FibUpdate update : withdraws) {
            FibEntry entry = update.entry();
            //Integer nextId = nextHops.get(entry.nextHopIp());

           /* Group group = deleteNextHop(entry.prefix());
            if (group == null) {
                log.warn("Group not found when deleting {}", entry);
                return;
            }*/

            flowObjectiveService.forward(deviceId,
                generateRibForwardingObj(entry.prefix(), null).remove());

        }

    }

    private ForwardingObjective.Builder generateRibForwardingObj(IpPrefix prefix,
                                                                 Integer nextId) {
        TrafficSelector selector = DefaultTrafficSelector.builder()
                .matchEthType(Ethernet.TYPE_IPV4)
                .matchIPDst(prefix)
                .build();

        int priority = prefix.prefixLength() * PRIORITY_MULTIPLIER + PRIORITY_OFFSET;

        ForwardingObjective.Builder fwdBuilder = DefaultForwardingObjective.builder()
                .fromApp(appId)
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

    private synchronized void addNextHop(FibEntry entry) {
        prefixToNextHop.put(entry.prefix(), entry.nextHopIp());
        if (nextHopsCount.count(entry.nextHopIp()) == 0) {
            // There was no next hop in the multiset

            Interface egressIntf = interfaceService.getMatchingInterface(entry.nextHopIp());
            if (egressIntf == null) {
                log.warn("no egress interface found for {}", entry);
                return;
            }

            NextHopGroupKey groupKey = new NextHopGroupKey(entry.nextHopIp());

            NextHop nextHop = new NextHop(entry.nextHopIp(), entry.nextHopMac(), groupKey);

            TrafficTreatment treatment = DefaultTrafficTreatment.builder()
                    .setEthSrc(egressIntf.mac())
                    .setEthDst(nextHop.mac())
                    .pushVlan()
                    .setVlanId(egressIntf.vlan())
                    .setVlanPcp((byte) 0)
                    .setOutput(egressIntf.connectPoint().port())
                    .build();

            int nextId = flowObjectiveService.allocateNextId();

            NextObjective nextObjective = DefaultNextObjective.builder()
                    .withId(nextId)
                    .addTreatment(treatment)
                    .withType(NextObjective.Type.SIMPLE)
                    .fromApp(appId)
                    .add(); // TODO add callbacks

            flowObjectiveService.next(deviceId, nextObjective);

            nextHops.put(nextHop.ip(), nextId);

        }

        nextHopsCount.add(entry.nextHopIp());
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

    private class InternalFibListener implements FibListener {

        @Override
        public void update(Collection<FibUpdate> updates,
                           Collection<FibUpdate> withdraws) {
            BgpRouter.this.deleteFibEntry(withdraws);
            BgpRouter.this.updateFibEntry(updates);
        }
    }

    private void processIntfFilters(boolean install, Set<Interface> intfs) {
        log.info("Processing {} router interfaces", intfs.size());
        for (Interface intf : intfs) {
            if (!intf.connectPoint().deviceId().equals(deviceId)) {
                // Ignore interfaces if they are not on the router switch
                continue;
            }

            FilteringObjective.Builder fob = DefaultFilteringObjective.builder();
            fob.withKey(Criteria.matchInPort(intf.connectPoint().port()))
               .addCondition(Criteria.matchEthDst(intf.mac()))
               .addCondition(Criteria.matchVlanId(intf.vlan()));
            intf.ipAddresses().stream()
                .forEach(ipaddr -> fob.addCondition(
                                   Criteria.matchIPDst(
                                        IpPrefix.valueOf(ipaddr.ipAddress(), 32))));
            fob.permit().fromApp(appId);
            flowObjectiveService.filter(
                  deviceId,
                  fob.add(new ObjectiveContext() {
                      @Override
                      public void onSuccess(Objective objective) {
                          log.info("Successfully installed interface based "
                                  + "filtering objectives for intf {}", intf);
                      }

                      @Override
                      public void onError(Objective objective,
                                          ObjectiveError error) {
                          log.error("Failed to install interface filters for intf {}: {}",
                                   intf, error);
                          // TODO something more than just logging
                      }
                  }));
        }
    }

    // Triggers driver setup when a device is (re)detected.
    private class InnerDeviceListener implements DeviceListener {
        @Override
        public void event(DeviceEvent event) {
            switch (event.type()) {
                case DEVICE_ADDED:
                case DEVICE_AVAILABILITY_CHANGED:
                    if (deviceService.isAvailable(event.subject().id())) {
                        log.info("Device connected {}", event.subject().id());
                        if (event.subject().id().equals(deviceId)) {
                            processIntfFilters(true, interfaceService.getInterfaces());

                            /* For test only - will be removed before Cardinal release
                            delay(1000);
                            FibEntry fibEntry = new FibEntry(Ip4Prefix.valueOf("10.1.0.0/16"),
                                    Ip4Address.valueOf("192.168.10.1"),
                                    MacAddress.valueOf("DE:AD:BE:EF:FE:ED"));
                            FibUpdate fibUpdate = new FibUpdate(FibUpdate.Type.UPDATE, fibEntry);
                            updateFibEntry(Collections.singletonList(fibUpdate));
                            */
                        }

                        if (event.subject().id().equals(ctrlDeviceId)) {
                            connectivityManager.notifySwitchAvailable();
                        }
                    }
                    break;

                    // TODO other cases
                case DEVICE_UPDATED:
                    break;
                case DEVICE_REMOVED:
                    break;
                case DEVICE_SUSPENDED:
                    break;
                case PORT_ADDED:
                    break;
                case PORT_UPDATED:
                    break;
                case PORT_REMOVED:
                    break;
                default:
                    break;
            }
        }
    }
}

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
import org.onlab.packet.Ip4Address;
import org.onlab.packet.Ip4Prefix;
import org.onlab.packet.Ip6Address;
import org.onlab.packet.IpAddress;
import org.onlab.packet.IpPrefix;
import org.onlab.packet.MacAddress;
import org.onlab.util.KryoNamespace;
import org.onosproject.config.NetworkConfigService;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.net.DeviceId;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.FlowRuleService;
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
import org.onosproject.net.group.GroupService;
import org.onosproject.net.packet.PacketService;
import org.onosproject.routing.FibEntry;
import org.onosproject.routing.FibListener;
import org.onosproject.routing.FibUpdate;
import org.onosproject.routing.RoutingService;
import org.onosproject.routing.config.BgpSpeaker;
import org.onosproject.routing.config.Interface;
import org.onosproject.routing.config.RoutingConfigurationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.onlab.util.Tools.delay;

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
    protected FlowRuleService flowService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected GroupService groupService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected RoutingService routingService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected RoutingConfigurationService configService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected PacketService packetService;

    //
    // NOTE: Unused reference - needed to guarantee that the
    // NetworkConfigReader component is activated and the network configuration
    // is read.
    //
    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected NetworkConfigService networkConfigService;

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

    //private final GroupListener groupListener = new InternalGroupListener();

    private TunnellingConnectivityManager connectivityManager;

    private IcmpHandler icmpHandler;

    private KryoNamespace appKryo = new KryoNamespace.Builder()
                    .register(IpAddress.Version.class)
                    .register(IpAddress.class)
                    .register(Ip4Address.class)
                    .register(Ip6Address.class)
                    .register(byte[].class)
                    .register(NextHopGroupKey.class)
                    .build();


    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected FlowObjectiveService flowObjectiveService;

    @Activate
    protected void activate() {
        appId = coreService.registerApplication(BGP_ROUTER_APP);
        getDeviceConfiguration(configService.getBgpSpeakers());

        //groupService.addListener(groupListener);

        processIntfFilters(true, configService.getInterfaces());

        connectivityManager = new TunnellingConnectivityManager(appId,
                                                                configService,
                                                                packetService,
                                                                flowObjectiveService);

        icmpHandler = new IcmpHandler(configService, packetService);

        routingService.addFibListener(new InternalFibListener());
        routingService.start();

        connectivityManager.start();

        icmpHandler.start();

        log.info("BgpRouter started");

        delay(1000);

        FibEntry fibEntry = new FibEntry(Ip4Prefix.valueOf("10.1.0.0/16"),
                                         Ip4Address.valueOf("192.168.10.1"),
                                         MacAddress.valueOf("DE:AD:BE:EF:FE:ED"));
        FibUpdate fibUpdate = new FibUpdate(FibUpdate.Type.UPDATE, fibEntry);
        updateFibEntry(Collections.singletonList(fibUpdate));
    }

    @Deactivate
    protected void deactivate() {
        routingService.stop();
        connectivityManager.stop();
        icmpHandler.stop();
        processIntfFilters(false, configService.getInterfaces());

        //groupService.removeListener(groupListener);

        log.info("BgpRouter stopped");
    }

    private void getDeviceConfiguration(Map<String, BgpSpeaker> bgps) {
        if (bgps == null || bgps.values().isEmpty()) {
            log.error("BGP speakers configuration is missing");
            return;
        }
        for (BgpSpeaker s : bgps.values()) {
            ctrlDeviceId = s.connectPoint().deviceId();
            if (s.interfaceAddresses() == null || s.interfaceAddresses().isEmpty()) {
                log.error("BGP Router must have interfaces configured");
                return;
            }
            deviceId = s.interfaceAddresses().get(0).connectPoint().deviceId();
            break;
        }

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

                /*
                group = groupService.getGroup(deviceId,
                                              new DefaultGroupKey(
                                              appKryo.serialize(nextHop.group())));

                if (group == null) {
                    log.debug("Adding pending flow {}", update.entry());
                    pendingUpdates.put(nextHop.group(), update.entry());
                    continue;
                }*/
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
                                         generateRibFlowRule(fibEntry.prefix(), nextId).add());
            log.trace("Sending forwarding objective {} -> nextId:{}", fibEntry, nextId);
        }

    }

    private synchronized void deleteFibEntry(Collection<FibUpdate> withdraws) {

        for (FibUpdate update : withdraws) {
            FibEntry entry = update.entry();
            Integer nextId = nextHops.get(entry.nextHopIp());

            /*Group group = deleteNextHop(entry.prefix());
            if (group == null) {
                log.warn("Group not found when deleting {}", entry);
                return;
            }*/

            flowObjectiveService.forward(deviceId,
                                         generateRibFlowRule(entry.prefix(), nextId).remove());

        }

    }

    private ForwardingObjective.Builder generateRibFlowRule(IpPrefix prefix, Integer nextId) {
        TrafficSelector selector = DefaultTrafficSelector.builder()
                .matchEthType(Ethernet.TYPE_IPV4)
                .matchIPDst(prefix)
                .build();

        int priority = prefix.prefixLength() * PRIORITY_MULTIPLIER + PRIORITY_OFFSET;

        ForwardingObjective.Builder fwdBuilder = DefaultForwardingObjective.builder()
                .fromApp(appId)
                .makePermanent()
                .nextStep(nextId)
                .withSelector(selector)
                .withPriority(priority)
                .withFlag(ForwardingObjective.Flag.SPECIFIC);

        return fwdBuilder;


    }

    private synchronized void addNextHop(FibEntry entry) {
        prefixToNextHop.put(entry.prefix(), entry.nextHopIp());
        if (nextHopsCount.count(entry.nextHopIp()) == 0) {
            // There was no next hop in the multiset

            Interface egressIntf = configService.getMatchingInterface(entry.nextHopIp());
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
                    .add();

            flowObjectiveService.next(deviceId, nextObjective);

            /*
            GroupBucket bucket = DefaultGroupBucket.createIndirectGroupBucket(treatment);

            GroupDescription groupDescription
                    = new DefaultGroupDescription(deviceId,
                                                  GroupDescription.Type.INDIRECT,
                                                  new GroupBuckets(Collections
                                                           .singletonList(bucket)),
                                                  new DefaultGroupKey(appKryo.serialize(groupKey)),
                                                  appId);

            groupService.addGroup(groupDescription);
            */

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
        *//*if (nextHopsCount.remove(nextHopIp, 1) <= 1) {
            // There was one or less next hops, so there are now none

            log.debug("removing group for next hop {}", nextHop);

            nextHops.remove(nextHopIp);

            groupService.removeGroup(deviceId,
                                     new DefaultGroupKey(appKryo.build().serialize(nextHop.group())),
                                     appId);
        }*//*

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
            FilteringObjective.Builder fob = DefaultFilteringObjective.builder();
            fob.withKey(Criteria.matchInPort(intf.connectPoint().port()))
               .addCondition(Criteria.matchEthDst(intf.mac()))
               .addCondition(Criteria.matchVlanId(intf.vlan()));
            intf.ipAddresses().stream()
                .forEach(ipaddr -> fob.addCondition(
                                   Criteria.matchIPDst(ipaddr.subnetAddress())));
            fob.permit().fromApp(appId);
            flowObjectiveService.filter(deviceId, fob.add());
        }
    }

   /* private class InternalGroupListener implements GroupListener {

        @Override
        public void event(GroupEvent event) {
            Group group = event.subject();

            if (event.type() == GroupEvent.Type.GROUP_ADDED ||
                    event.type() == GroupEvent.Type.GROUP_UPDATED) {
                synchronized (pendingUpdates) {

                    NextHopGroupKey nhGroupKey =
                            appKryo.deserialize(group.appCookie().key());
                    Map<FibEntry, Group> entriesToInstall =
                            pendingUpdates.removeAll(nhGroupKey)
                                    .stream()
                                    .collect(Collectors
                                                     .toMap(e -> e, e -> group));

                    installFlows(entriesToInstall);
                }
            }
        }
    }*/
}

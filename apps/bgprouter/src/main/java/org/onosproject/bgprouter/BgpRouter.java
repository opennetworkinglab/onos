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
import com.google.common.collect.Multiset;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.onlab.packet.Ethernet;
import org.onlab.packet.MacAddress;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.net.DeviceId;
import org.onosproject.net.flow.DefaultFlowRule;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.FlowRule;
import org.onosproject.net.flow.FlowRuleOperations;
import org.onosproject.net.flow.FlowRuleOperationsContext;
import org.onosproject.net.flow.FlowRuleService;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.group.DefaultGroupBucket;
import org.onosproject.net.group.DefaultGroupDescription;
import org.onosproject.net.group.Group;
import org.onosproject.net.group.GroupBucket;
import org.onosproject.net.group.GroupBuckets;
import org.onosproject.net.group.GroupDescription;
import org.onosproject.net.group.GroupKey;
import org.onosproject.net.group.GroupService;
import org.onosproject.net.packet.PacketService;
import org.onosproject.routing.FibListener;
import org.onosproject.routing.FibUpdate;
import org.onosproject.routing.RoutingService;
import org.onosproject.routing.config.Interface;
import org.onosproject.routing.config.RoutingConfigurationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * BgpRouter component.
 */
@Component(immediate = true)
public class BgpRouter {

    private static final Logger log = LoggerFactory.getLogger(BgpRouter.class);

    private static final String BGP_ROUTER_APP = "org.onosproject.bgprouter";

    private static final int PRIORITY = 1;

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

    private ApplicationId appId;

    private final Multiset<NextHop> nextHops = ConcurrentHashMultiset.create();
    private final Map<NextHop, NextHopGroupKey> groups = new HashMap<>();

    private DeviceId deviceId = DeviceId.deviceId("of:0000000000000001"); // TODO config

    private TunnellingConnectivityManager connectivityManager;

    private InternalTableHandler provisionStaticTables = new InternalTableHandler();

    @Activate
    protected void activate() {
        log.info("Bgp1Router started");
        appId = coreService.registerApplication(BGP_ROUTER_APP);

        provisionStaticTables.provision(true);

        connectivityManager = new TunnellingConnectivityManager(appId,
                                                                configService,
                                                                packetService);

        routingService.start(new InternalFibListener());

        connectivityManager.start();

        log.info("BgpRouter started");
    }

    @Deactivate
    protected void deactivate() {
        routingService.stop();
        connectivityManager.stop();
        provisionStaticTables.provision(false);

        log.info("BgpRouter stopped");
    }

    private void updateFibEntry(Collection<FibUpdate> updates) {
        for (FibUpdate update : updates) {
            NextHop nextHop = new NextHop(update.entry().nextHopIp(),
                                          update.entry().nextHopMac());

            addNextHop(nextHop);

            TrafficSelector selector = DefaultTrafficSelector.builder()
                    .matchEthType(Ethernet.TYPE_IPV4)
                    .matchIPDst(update.entry().prefix())
                    .build();

            // TODO ensure group exists
            NextHopGroupKey groupKey = groups.get(nextHop);
            Group group = groupService.getGroup(deviceId, groupKey);
            if (group == null) {
                // TODO handle this
                log.warn("oops, group {} wasn't there");
                continue;
            }

            TrafficTreatment treatment = DefaultTrafficTreatment.builder()
                    .group(group.id())
                    .build();

            FlowRule flowRule = new DefaultFlowRule(deviceId, selector, treatment,
                                                    PRIORITY, appId, 0, true,
                                                    FlowRule.Type.IP);

            flowService.applyFlowRules(flowRule);
        }
    }

    private void deleteFibEntry(Collection<FibUpdate> withdraws) {
        for (FibUpdate update : withdraws) {
            NextHop nextHop = new NextHop(update.entry().nextHopIp(),
                                          update.entry().nextHopMac());

            deleteNextHop(nextHop);

            TrafficSelector selector = DefaultTrafficSelector.builder()
                    .matchIPDst(update.entry().prefix())
                    .build();

            FlowRule flowRule = new DefaultFlowRule(deviceId, selector, null,
                                                    PRIORITY, appId, 0, true,
                                                    FlowRule.Type.IP);

            flowService.removeFlowRules(flowRule);
        }
    }

    private void addNextHop(NextHop nextHop) {
        if (nextHops.add(nextHop, 1) == 0) {
            // There was no next hop in the multiset

            Interface egressIntf = configService.getMatchingInterface(nextHop.ip());
            if (egressIntf == null) {
                log.warn("no egress interface found for {}", nextHop);
                return;
            }

            NextHopGroupKey groupKey = new NextHopGroupKey(nextHop.ip());
            groups.put(nextHop, groupKey);

            TrafficTreatment treatment = DefaultTrafficTreatment.builder()
                    .setEthSrc(egressIntf.mac())
                    .setEthDst(nextHop.mac())
                    .setVlanId(egressIntf.vlan())
                    .setOutput(egressIntf.connectPoint().port())
                    .build();

            GroupBucket bucket = DefaultGroupBucket.createIndirectGroupBucket(treatment);

            GroupDescription groupDescription
                    = new DefaultGroupDescription(deviceId,
                                                  GroupDescription.Type.INDIRECT,
                                                  new GroupBuckets(Collections
                                                                           .singletonList(bucket)),
                                                  groupKey,
                                                  appId);

            groupService.addGroup(groupDescription);
        }
    }

    private void deleteNextHop(NextHop nextHop) {
        if (nextHops.remove(nextHop, 1) <= 1) {
            // There was one or less next hops, so there are now none

            log.debug("removing group");

            GroupKey groupKey = groups.remove(nextHop);
            groupService.removeGroup(deviceId, groupKey, appId);
        }
    }

    private class InternalFibListener implements FibListener {

        @Override
        public void update(Collection<FibUpdate> updates,
                           Collection<FibUpdate> withdraws) {
            BgpRouter.this.deleteFibEntry(withdraws);
            BgpRouter.this.updateFibEntry(updates);
        }
    }

    private class InternalTableHandler {

        private static final int CONTROLLER_PRIORITY = 255;
        private static final int DROP_PRIORITY = 0;


        public void provision(boolean install) {

            processTableZero(install);
            processTableOne(install);
            processTableTwo(install);
            processTableThree(install);
            processTableFive(install);
            processTableSix(install);
            processTableNine(install);

        }

        private void processTableZero(boolean install) {
            TrafficSelector.Builder selector;
            TrafficTreatment.Builder treatment;

            selector = DefaultTrafficSelector.builder();
            treatment = DefaultTrafficTreatment.builder();

            selector.matchEthDst(MacAddress.BROADCAST);
            treatment.transition(FlowRule.Type.VLAN_MPLS);

            FlowRule rule = new DefaultFlowRule(deviceId, selector.build(),
                                                treatment.build(), CONTROLLER_PRIORITY,
                                                appId, 0, true);

            FlowRuleOperations.Builder ops = FlowRuleOperations.builder();

            ops = install ? ops.add(rule) : ops.remove(rule);


            //Drop rule
            selector = DefaultTrafficSelector.builder();
            treatment = DefaultTrafficTreatment.builder();

            treatment.drop();

            rule = new DefaultFlowRule(deviceId, selector.build(),
                                       treatment.build(), DROP_PRIORITY,
                                       appId, 0, true, FlowRule.Type.VLAN_MPLS);

            ops = install ? ops.add(rule) : ops.remove(rule);

            flowService.apply(ops.build(new FlowRuleOperationsContext() {
                @Override
                public void onSuccess(FlowRuleOperations ops) {
                    log.info("Provisioned default table for bgp router");
                }

                @Override
                public void onError(FlowRuleOperations ops) {
                    log.info("Failed to provision default table for bgp router");
                }
            }));

        }

        private void processTableOne(boolean install) {
            TrafficSelector.Builder selector = DefaultTrafficSelector.builder();
            TrafficTreatment.Builder treatment = DefaultTrafficTreatment.builder();
            FlowRuleOperations.Builder ops = FlowRuleOperations.builder();
            FlowRule rule;

            selector.matchEthType(Ethernet.TYPE_IPV4);
            treatment.transition(FlowRule.Type.VLAN);

            rule = new DefaultFlowRule(deviceId, selector.build(),
                                  treatment.build(), CONTROLLER_PRIORITY,
                                  appId, 0, true, FlowRule.Type.VLAN_MPLS);

            ops = install ? ops.add(rule) : ops.remove(rule);

            selector = DefaultTrafficSelector.builder();
            treatment = DefaultTrafficTreatment.builder();

            selector.matchEthType(Ethernet.TYPE_ARP);
            treatment.transition(FlowRule.Type.VLAN);

            rule = new DefaultFlowRule(deviceId, selector.build(),
                                       treatment.build(), CONTROLLER_PRIORITY,
                                       appId, 0, true, FlowRule.Type.VLAN_MPLS);

            ops = install ? ops.add(rule) : ops.remove(rule);

            selector = DefaultTrafficSelector.builder();
            treatment = DefaultTrafficTreatment.builder();

            selector.matchEthType(Ethernet.TYPE_VLAN);
            treatment.transition(FlowRule.Type.VLAN);

            rule = new DefaultFlowRule(deviceId, selector.build(),
                                       treatment.build(), CONTROLLER_PRIORITY,
                                       appId, 0, true, FlowRule.Type.VLAN_MPLS);

            ops = install ? ops.add(rule) : ops.remove(rule);

            //Drop rule
            selector = DefaultTrafficSelector.builder();
            treatment = DefaultTrafficTreatment.builder();

            treatment.drop();

            rule = new DefaultFlowRule(deviceId, selector.build(),
                                       treatment.build(), DROP_PRIORITY,
                                       appId, 0, true, FlowRule.Type.VLAN_MPLS);

            ops = install ? ops.add(rule) : ops.remove(rule);

            flowService.apply(ops.build(new FlowRuleOperationsContext() {
                @Override
                public void onSuccess(FlowRuleOperations ops) {
                    log.info("Provisioned vlan/mpls table for bgp router");
                }

                @Override
                public void onError(FlowRuleOperations ops) {
                    log.info("Failed to provision vlan/mpls table for bgp router");
                }
            }));

        }

        private void processTableTwo(boolean install) {
            TrafficSelector.Builder selector = DefaultTrafficSelector.builder();
            TrafficTreatment.Builder treatment = DefaultTrafficTreatment.builder();
            FlowRuleOperations.Builder ops = FlowRuleOperations.builder();
            FlowRule rule;

            //Drop rule

            treatment.drop();

            rule = new DefaultFlowRule(deviceId, selector.build(),
                                       treatment.build(), DROP_PRIORITY,
                                       appId, 0, true, FlowRule.Type.VLAN);

            ops = install ? ops.add(rule) : ops.remove(rule);

            flowService.apply(ops.build(new FlowRuleOperationsContext() {
                @Override
                public void onSuccess(FlowRuleOperations ops) {
                    log.info("Provisioned vlan table for bgp router");
                }

                @Override
                public void onError(FlowRuleOperations ops) {
                    log.info("Failed to provision vlan table for bgp router");
                }
            }));
        }



        private void processTableThree(boolean install) {
            TrafficSelector.Builder selector = DefaultTrafficSelector.builder();
            TrafficTreatment.Builder treatment = DefaultTrafficTreatment.builder();
            FlowRuleOperations.Builder ops = FlowRuleOperations.builder();
            FlowRule rule;

            selector.matchEthType(Ethernet.TYPE_ARP);
            treatment.punt();

            rule = new DefaultFlowRule(deviceId, selector.build(),
                                       treatment.build(), CONTROLLER_PRIORITY,
                                       appId, 0, true, FlowRule.Type.ETHER);

            ops = install ? ops.add(rule) : ops.remove(rule);

            selector = DefaultTrafficSelector.builder();
            treatment = DefaultTrafficTreatment.builder();

            selector.matchEthType(Ethernet.TYPE_IPV4);
            treatment.transition(FlowRule.Type.COS);

            rule = new DefaultFlowRule(deviceId, selector.build(),
                                       treatment.build(), CONTROLLER_PRIORITY,
                                       appId, 0, true, FlowRule.Type.ETHER);

            ops = install ? ops.add(rule) : ops.remove(rule);

            //Drop rule
            selector = DefaultTrafficSelector.builder();
            treatment = DefaultTrafficTreatment.builder();

            treatment.drop();

            rule = new DefaultFlowRule(deviceId, selector.build(),
                                       treatment.build(), DROP_PRIORITY,
                                       appId, 0, true, FlowRule.Type.VLAN_MPLS);

            ops = install ? ops.add(rule) : ops.remove(rule);

            flowService.apply(ops.build(new FlowRuleOperationsContext() {
                @Override
                public void onSuccess(FlowRuleOperations ops) {
                    log.info("Provisioned ether table for bgp router");
                }

                @Override
                public void onError(FlowRuleOperations ops) {
                    log.info("Failed to provision ether table for bgp router");
                }
            }));


        }

        private void processTableFive(boolean install) {
            TrafficSelector.Builder selector = DefaultTrafficSelector.builder();
            TrafficTreatment.Builder treatment = DefaultTrafficTreatment.builder();
            FlowRuleOperations.Builder ops = FlowRuleOperations.builder();
            FlowRule rule;

            treatment.transition(FlowRule.Type.IP);

            rule = new DefaultFlowRule(deviceId, selector.build(),
                                       treatment.build(), DROP_PRIORITY,
                                       appId, 0, true, FlowRule.Type.COS);

            ops = install ? ops.add(rule) : ops.remove(rule);

            flowService.apply(ops.build(new FlowRuleOperationsContext() {
                @Override
                public void onSuccess(FlowRuleOperations ops) {
                    log.info("Provisioned cos table for bgp router");
                }

                @Override
                public void onError(FlowRuleOperations ops) {
                    log.info("Failed to provision cos table for bgp router");
                }
            }));

        }

        private void processTableSix(boolean install) {
            TrafficSelector.Builder selector = DefaultTrafficSelector.builder();
            TrafficTreatment.Builder treatment = DefaultTrafficTreatment.builder();
            FlowRuleOperations.Builder ops = FlowRuleOperations.builder();
            FlowRule rule;

            //Drop rule

            treatment.drop();

            rule = new DefaultFlowRule(deviceId, selector.build(),
                                       treatment.build(), DROP_PRIORITY,
                                       appId, 0, true, FlowRule.Type.IP);

            ops = install ? ops.add(rule) : ops.remove(rule);

            flowService.apply(ops.build(new FlowRuleOperationsContext() {
                @Override
                public void onSuccess(FlowRuleOperations ops) {
                    log.info("Provisioned FIB table for bgp router");
                }

                @Override
                public void onError(FlowRuleOperations ops) {
                    log.info("Failed to provision FIB table for bgp router");
                }
            }));
        }

        private void processTableNine(boolean install) {
            TrafficSelector.Builder selector = DefaultTrafficSelector.builder();
            TrafficTreatment.Builder treatment = DefaultTrafficTreatment.builder();
            FlowRuleOperations.Builder ops = FlowRuleOperations.builder();
            FlowRule rule;

            treatment.punt();

            rule = new DefaultFlowRule(deviceId, selector.build(),
                                       treatment.build(), CONTROLLER_PRIORITY,
                                       appId, 0, true, FlowRule.Type.ACL);

            ops = install ? ops.add(rule) : ops.remove(rule);

            flowService.apply(ops.build(new FlowRuleOperationsContext() {
                @Override
                public void onSuccess(FlowRuleOperations ops) {
                    log.info("Provisioned Local table for bgp router");
                }

                @Override
                public void onError(FlowRuleOperations ops) {
                    log.info("Failed to provision Local table for bgp router");
                }
            }));
        }

    }
}

/*
 * Copyright 2016-present Open Networking Foundation
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
package org.onosproject.driver.pipeline;


import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.RemovalCause;
import com.google.common.cache.RemovalNotification;

import org.onlab.osgi.ServiceDirectory;
import org.onlab.packet.Ethernet;
import org.onlab.packet.IPv4;
import org.onlab.packet.TpPort;
import org.onlab.packet.VlanId;
import org.onlab.util.KryoNamespace;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;
import org.onosproject.net.behaviour.NextGroup;
import org.onosproject.net.behaviour.Pipeliner;
import org.onosproject.net.behaviour.PipelinerContext;
import org.onosproject.net.driver.AbstractHandlerBehaviour;
import org.onosproject.net.flow.DefaultFlowRule;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.FlowRule;
import org.onosproject.net.flow.FlowRuleOperations;
import org.onosproject.net.flow.FlowRuleOperationsContext;
import org.onosproject.net.flow.FlowRuleService;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.flow.criteria.Criterion;
import org.onosproject.net.flow.criteria.Criteria;
import org.onosproject.net.flow.criteria.EthCriterion;
import org.onosproject.net.flow.criteria.PortCriterion;
import org.onosproject.net.flow.criteria.EthTypeCriterion;
import org.onosproject.net.flow.criteria.IPCriterion;
import org.onosproject.net.flow.criteria.VlanIdCriterion;
import org.onosproject.net.flow.instructions.Instruction;
import org.onosproject.net.flow.instructions.L2ModificationInstruction;
import org.onosproject.net.flowobjective.FilteringObjective;
import org.onosproject.net.flowobjective.FlowObjectiveStore;
import org.onosproject.net.flowobjective.ForwardingObjective;
import org.onosproject.net.flowobjective.NextObjective;
import org.onosproject.net.flowobjective.Objective;
import org.onosproject.net.flowobjective.ObjectiveError;
import org.onosproject.net.group.DefaultGroupBucket;
import org.onosproject.net.group.DefaultGroupDescription;
import org.onosproject.net.group.DefaultGroupKey;
import org.onosproject.net.group.Group;
import org.onosproject.net.group.GroupBucket;
import org.onosproject.net.group.GroupBuckets;
import org.onosproject.net.group.GroupDescription;
import org.onosproject.net.group.GroupEvent;
import org.onosproject.net.group.GroupKey;
import org.onosproject.net.group.GroupListener;
import org.onosproject.net.group.GroupService;
import org.slf4j.Logger;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.onlab.util.Tools.groupedThreads;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Driver for Centec's V350 switches.
 */
public class CentecV350Pipeline extends AbstractHandlerBehaviour implements Pipeliner {

    protected static final int PORT_VLAN_TABLE = 0;
    protected static final int FILTER_TABLE = 1;
    // TMAC is configured in MAC Table to redirect packets to ROUTE_TABLE.
    protected static final int MAC_TABLE = 2;
    protected static final int ROUTE_TABLE = 3;

    private static final long DEFAULT_METADATA = 100;
    private static final long DEFAULT_METADATA_MASK = 0xffffffffffffffffL;

    // Priority used in PORT_VLAN Table, the only priority accepted is PORT_VLAN_TABLE_PRIORITY.
    // The packet passed PORT+VLAN check will goto FILTER Table.
    private static final int PORT_VLAN_TABLE_PRIORITY = 0xffff;

    // Priority used in Filter Table.
    private static final int FILTER_TABLE_CONTROLLER_PRIORITY = 500;
    // TMAC priority should be lower than controller.
    private static final int FILTER_TABLE_TMAC_PRIORITY = 200;
    private static final int FILTER_TABLE_HIGHEST_PRIORITY = 0xffff;

    // Priority used in MAC Table.
    // We do exact matching for DMAC+metadata, so priority is ignored and required to be set to 0xffff.
    private static final int MAC_TABLE_PRIORITY = 0xffff;

    // Priority used in Route Table.
    // We do LPM matching in Route Table, so priority is ignored and required to be set to 0xffff.
    private static final int ROUTE_TABLE_PRIORITY = 0xffff;

    private static final short BGP_PORT = 179;

    private final Logger log = getLogger(getClass());

    private ServiceDirectory serviceDirectory;
    private FlowRuleService flowRuleService;
    private CoreService coreService;
    private GroupService groupService;
    private FlowObjectiveStore flowObjectiveStore;
    private DeviceId deviceId;
    private ApplicationId appId;

    private KryoNamespace appKryo = new KryoNamespace.Builder()
            .register(GroupKey.class)
            .register(DefaultGroupKey.class)
            .register(CentecV350Group.class)
            .register(byte[].class)
            .build("CentecV350Pipeline");

    private Cache<GroupKey, NextObjective> pendingGroups;

    private ScheduledExecutorService groupChecker =
            Executors.newScheduledThreadPool(2, groupedThreads("onos/pipeliner",
                    "centec-V350-%d", log));

    @Override
    public void init(DeviceId deviceId, PipelinerContext context) {
        this.serviceDirectory = context.directory();
        this.deviceId = deviceId;

        pendingGroups = CacheBuilder.newBuilder()
                .expireAfterWrite(20, TimeUnit.SECONDS)
                .removalListener((RemovalNotification<GroupKey, NextObjective> notification) -> {
                    if (notification.getCause() == RemovalCause.EXPIRED) {
                        fail(notification.getValue(), ObjectiveError.GROUPINSTALLATIONFAILED);
                    }
                }).build();

        groupChecker.scheduleAtFixedRate(new GroupChecker(), 0, 500, TimeUnit.MILLISECONDS);

        coreService = serviceDirectory.get(CoreService.class);
        flowRuleService = serviceDirectory.get(FlowRuleService.class);
        groupService = serviceDirectory.get(GroupService.class);
        flowObjectiveStore = context.store();

        groupService.addListener(new InnerGroupListener());

        appId = coreService.registerApplication(
                "org.onosproject.driver.CentecV350Pipeline");

        initializePipeline();
    }

    @Override
    public void filter(FilteringObjective filteringObjective) {
        if (filteringObjective.type() == FilteringObjective.Type.PERMIT) {
            processFilter(filteringObjective,
                    filteringObjective.op() == Objective.Operation.ADD,
                    filteringObjective.appId());
        } else {
            fail(filteringObjective, ObjectiveError.UNSUPPORTED);
        }
    }

    @Override
    public void forward(ForwardingObjective fwd) {
        Collection<FlowRule> rules;
        FlowRuleOperations.Builder flowBuilder = FlowRuleOperations.builder();

        rules = processForward(fwd);
        switch (fwd.op()) {
            case ADD:
                rules.stream()
                        .filter(Objects::nonNull)
                        .forEach(flowBuilder::add);
                break;
            case REMOVE:
                rules.stream()
                        .filter(Objects::nonNull)
                        .forEach(flowBuilder::remove);
                break;
            default:
                fail(fwd, ObjectiveError.UNKNOWN);
                log.warn("Unknown forwarding type {}", fwd.op());
        }


        flowRuleService.apply(flowBuilder.build(new FlowRuleOperationsContext() {
            @Override
            public void onSuccess(FlowRuleOperations ops) {
                pass(fwd);
            }

            @Override
            public void onError(FlowRuleOperations ops) {
                fail(fwd, ObjectiveError.FLOWINSTALLATIONFAILED);
            }
        }));

    }

    @Override
    public void next(NextObjective nextObjective) {
        switch (nextObjective.type()) {
            case SIMPLE:
                Collection<TrafficTreatment> treatments = nextObjective.next();
                if (treatments.size() == 1) {
                    TrafficTreatment treatment = treatments.iterator().next();

                    // Since we do not support strip_vlan in PORT_VLAN table, we use mod_vlan
                    // to modify the packet to desired vlan.
                    // Note: if we use push_vlan here, the switch will add a second VLAN tag to the outgoing
                    // packet, which is not what we want.
                    TrafficTreatment.Builder treatmentWithoutPushVlan = DefaultTrafficTreatment.builder();
                    VlanId modVlanId;
                    for (Instruction ins : treatment.allInstructions()) {
                        if (ins.type() == Instruction.Type.L2MODIFICATION) {
                            L2ModificationInstruction l2ins = (L2ModificationInstruction) ins;
                            switch (l2ins.subtype()) {
                                case ETH_DST:
                                    treatmentWithoutPushVlan.setEthDst(
                                            ((L2ModificationInstruction.ModEtherInstruction) l2ins).mac());
                                    break;
                                case ETH_SRC:
                                    treatmentWithoutPushVlan.setEthSrc(
                                            ((L2ModificationInstruction.ModEtherInstruction) l2ins).mac());
                                    break;
                                case VLAN_ID:
                                    modVlanId = ((L2ModificationInstruction.ModVlanIdInstruction) l2ins).vlanId();
                                    treatmentWithoutPushVlan.setVlanId(modVlanId);
                                    break;
                                default:
                                    break;
                            }
                        } else if (ins.type() == Instruction.Type.OUTPUT) {
                            //long portNum = ((Instructions.OutputInstruction) ins).port().toLong();
                            treatmentWithoutPushVlan.add(ins);
                        } else {
                            // Ignore the vlan_pcp action since it's does matter much.
                            log.warn("Driver does not handle this type of TrafficTreatment"
                                    + " instruction in nextObjectives:  {}", ins.type());
                        }
                    }

                    GroupBucket bucket =
                            DefaultGroupBucket.createIndirectGroupBucket(treatmentWithoutPushVlan.build());
                    final GroupKey key = new DefaultGroupKey(appKryo.serialize(nextObjective.id()));
                    GroupDescription groupDescription
                            = new DefaultGroupDescription(deviceId,
                            GroupDescription.Type.INDIRECT,
                            new GroupBuckets(Collections
                                    .singletonList(bucket)),
                            key,
                            null, // let group service determine group id
                            nextObjective.appId());
                    groupService.addGroup(groupDescription);
                    pendingGroups.put(key, nextObjective);
                }
                break;
            case HASHED:
            case BROADCAST:
            case FAILOVER:
                fail(nextObjective, ObjectiveError.UNSUPPORTED);
                log.warn("Unsupported next objective type {}", nextObjective.type());
                break;
            default:
                fail(nextObjective, ObjectiveError.UNKNOWN);
                log.warn("Unknown next objective type {}", nextObjective.type());
        }

    }

    @Override
    public void purgeAll(ApplicationId appId) {
        flowRuleService.purgeFlowRules(deviceId, appId);
    }

    private Collection<FlowRule> processForward(ForwardingObjective fwd) {
        switch (fwd.flag()) {
            case SPECIFIC:
                return processSpecific(fwd);
            case VERSATILE:
                return processVersatile(fwd);
            default:
                fail(fwd, ObjectiveError.UNKNOWN);
                log.warn("Unknown forwarding flag {}", fwd.flag());
        }
        return Collections.emptySet();
    }

    private Collection<FlowRule> processVersatile(ForwardingObjective fwd) {
        log.warn("Driver does not support versatile forwarding objective");
        fail(fwd, ObjectiveError.UNSUPPORTED);
        return Collections.emptySet();
    }

    private Collection<FlowRule> processSpecific(ForwardingObjective fwd) {
        log.debug("Processing specific forwarding objective");
        TrafficSelector selector = fwd.selector();
        EthTypeCriterion ethType =
                (EthTypeCriterion) selector.getCriterion(Criterion.Type.ETH_TYPE);
        if (ethType == null || ethType.ethType().toShort() != Ethernet.TYPE_IPV4) {
            fail(fwd, ObjectiveError.UNSUPPORTED);
            return Collections.emptySet();
        }

        // Must have metadata as key.
        TrafficSelector filteredSelector =
                DefaultTrafficSelector.builder()
                        .matchEthType(Ethernet.TYPE_IPV4)
                        .matchMetadata(DEFAULT_METADATA)
                        .matchIPDst(
                                ((IPCriterion)
                                        selector.getCriterion(Criterion.Type.IPV4_DST)).ip())
                        .build();

        TrafficTreatment.Builder tb = DefaultTrafficTreatment.builder();

        if (fwd.nextId() != null) {
            NextGroup next = flowObjectiveStore.getNextGroup(fwd.nextId());
            GroupKey key = appKryo.deserialize(next.data());
            Group group = groupService.getGroup(deviceId, key);
            if (group == null) {
                log.warn("The group left!");
                fail(fwd, ObjectiveError.GROUPMISSING);
                return Collections.emptySet();
            }
            tb.group(group.id());
        }

        FlowRule.Builder ruleBuilder = DefaultFlowRule.builder()
                .fromApp(fwd.appId())
                .withPriority(ROUTE_TABLE_PRIORITY)
                .forDevice(deviceId)
                .withSelector(filteredSelector)
                .withTreatment(tb.build());

        if (fwd.permanent()) {
            ruleBuilder.makePermanent();
        } else {
            ruleBuilder.makeTemporary(fwd.timeout());
        }

        ruleBuilder.forTable(ROUTE_TABLE);

        return Collections.singletonList(ruleBuilder.build());

    }

    private void processFilter(FilteringObjective filt, boolean install,
                               ApplicationId applicationId) {
        PortCriterion p;
        if (!filt.key().equals(Criteria.dummy()) &&
                filt.key().type() == Criterion.Type.IN_PORT) {
            p = (PortCriterion) filt.key();
        } else {
            log.warn("No key defined in filtering objective from app: {}. Not"
                    + "processing filtering objective", applicationId);
            fail(filt, ObjectiveError.UNKNOWN);
            return;
        }

        // Convert filtering conditions for switch-intfs into flow rules.
        FlowRuleOperations.Builder ops = FlowRuleOperations.builder();

        for (Criterion c : filt.conditions()) {
            // Here we do a trick to install 2 flow rules to MAC_TABLE and ROUTE_TABLE.
            if (c.type() == Criterion.Type.ETH_DST) {
                EthCriterion e = (EthCriterion) c;

                // Install TMAC flow rule.
                log.debug("adding rule for Termination MAC in Filter Table: {}", e.mac());
                TrafficSelector.Builder selector = DefaultTrafficSelector.builder();
                TrafficTreatment.Builder treatment = DefaultTrafficTreatment.builder();
                selector.matchEthDst(e.mac());
                // Add IPv4 matching explicitly since we will redirect it to ROUTE Table
                // through MAC table.
                selector.matchEthType(Ethernet.TYPE_IPV4);
                treatment.transition(MAC_TABLE);
                FlowRule rule = DefaultFlowRule.builder()
                        .forDevice(deviceId)
                        .withSelector(selector.build())
                        .withTreatment(treatment.build())
                        .withPriority(FILTER_TABLE_TMAC_PRIORITY)
                        .fromApp(applicationId)
                        .makePermanent()
                        .forTable(FILTER_TABLE).build();
                ops =  install ? ops.add(rule) : ops.remove(rule);

                // Must install another rule to direct the IPv4 packets that hit TMAC to
                // Route table.
                log.debug("adding rule for Termination MAC in MAC Table: {}", e.mac());
                selector = DefaultTrafficSelector.builder();
                treatment = DefaultTrafficTreatment.builder();
                selector.matchEthDst(e.mac());
                // MAC_Table must have metadata matching configured, use the default metadata.
                selector.matchMetadata(DEFAULT_METADATA);
                treatment.transition(ROUTE_TABLE);
                rule = DefaultFlowRule.builder()
                        .forDevice(deviceId)
                        .withSelector(selector.build())
                        .withTreatment(treatment.build())
                        .withPriority(MAC_TABLE_PRIORITY)
                        .fromApp(applicationId)
                        .makePermanent()
                        .forTable(MAC_TABLE).build();
                ops =  install ? ops.add(rule) : ops.remove(rule);
            } else if (c.type() == Criterion.Type.VLAN_VID) {
                VlanIdCriterion v = (VlanIdCriterion) c;
                log.debug("adding rule for VLAN: {}", v.vlanId());
                TrafficSelector.Builder selector = DefaultTrafficSelector.builder();
                TrafficTreatment.Builder treatment = DefaultTrafficTreatment.builder();
                selector.matchVlanId(v.vlanId());
                selector.matchInPort(p.port());
                // Although the accepted packets will be sent to filter table, we must
                // explicitly set goto_table instruction here.
                treatment.writeMetadata(DEFAULT_METADATA, DEFAULT_METADATA_MASK);
                // set default metadata written by PORT_VLAN Table.
                treatment.transition(FILTER_TABLE);
                // We do not support strip vlan here, treatment.deferred().popVlan();
                // PORT_VLAN table only accept 0xffff priority since it does exact match only.
                FlowRule rule = DefaultFlowRule.builder()
                        .forDevice(deviceId)
                        .withSelector(selector.build())
                        .withTreatment(treatment.build())
                        .withPriority(PORT_VLAN_TABLE_PRIORITY)
                        .fromApp(applicationId)
                        .makePermanent()
                        .forTable(PORT_VLAN_TABLE).build();
                ops = install ? ops.add(rule) : ops.remove(rule);
            } else if (c.type() == Criterion.Type.IPV4_DST) {
                IPCriterion ipaddr = (IPCriterion) c;
                log.debug("adding IP filtering rules in FILTER table: {}", ipaddr.ip());
                TrafficSelector.Builder selector = DefaultTrafficSelector.builder();
                TrafficTreatment.Builder treatment = DefaultTrafficTreatment.builder();
                selector.matchEthType(Ethernet.TYPE_IPV4);
                selector.matchIPDst(ipaddr.ip()); // router IPs to the controller
                treatment.setOutput(PortNumber.CONTROLLER);
                FlowRule rule = DefaultFlowRule.builder()
                        .forDevice(deviceId)
                        .withSelector(selector.build())
                        .withTreatment(treatment.build())
                        .withPriority(FILTER_TABLE_CONTROLLER_PRIORITY)
                        .fromApp(applicationId)
                        .makePermanent()
                        .forTable(FILTER_TABLE).build();
                ops =  install ? ops.add(rule) : ops.remove(rule);
            } else {
                log.warn("Driver does not currently process filtering condition"
                        + " of type: {}", c.type());
                fail(filt, ObjectiveError.UNSUPPORTED);
            }
        }

        // apply filtering flow rules
        flowRuleService.apply(ops.build(new FlowRuleOperationsContext() {
            @Override
            public void onSuccess(FlowRuleOperations ops) {
                pass(filt);
                log.info("Applied filtering rules");
            }

            @Override
            public void onError(FlowRuleOperations ops) {
                fail(filt, ObjectiveError.FLOWINSTALLATIONFAILED);
                log.info("Failed to apply filtering rules");
            }
        }));
    }

    private void pass(Objective obj) {
        obj.context().ifPresent(context -> context.onSuccess(obj));
    }

    private void fail(Objective obj, ObjectiveError error) {
        obj.context().ifPresent(context -> context.onError(obj, error));
    }

    private void initializePipeline() {
        // CENTEC_V350: PORT_VLAN_TABLE->FILTER_TABLE->MAC_TABLE(TMAC)->ROUTE_TABLE.
        processPortVlanTable(true);
        processFilterTable(true);
    }

    private void processPortVlanTable(boolean install) {
        // By default the packet are dropped, need install port+vlan by some ways.

        // XXX can we add table-miss-entry to drop? Code says drops by default
        // XXX TTP description says default goes to table1.
        // It also says that match is only on vlan -- not port-vlan -- which one is true?
    }

    private void processFilterTable(boolean install) {
        TrafficSelector.Builder selector = DefaultTrafficSelector.builder();
        TrafficTreatment.Builder treatment = DefaultTrafficTreatment
                .builder();
        FlowRuleOperations.Builder ops = FlowRuleOperations.builder();
        FlowRule rule;

        // Punt ARP packets to controller by default.
        selector.matchEthType(Ethernet.TYPE_ARP);
        treatment.punt();
        rule = DefaultFlowRule.builder()
                .forDevice(deviceId)
                .withSelector(selector.build())
                .withTreatment(treatment.build())
                .withPriority(FILTER_TABLE_CONTROLLER_PRIORITY)
                .fromApp(appId)
                .makePermanent()
                .forTable(FILTER_TABLE).build();
        ops = install ? ops.add(rule) : ops.remove(rule);

        // Punt BGP packets to controller directly.
        selector = DefaultTrafficSelector.builder();
        treatment = DefaultTrafficTreatment.builder();
        selector.matchEthType(Ethernet.TYPE_IPV4)
                .matchIPProtocol(IPv4.PROTOCOL_TCP)
                .matchTcpSrc(TpPort.tpPort(BGP_PORT));
        treatment.punt();
        rule = DefaultFlowRule.builder()
                .forDevice(deviceId)
                .withPriority(FILTER_TABLE_HIGHEST_PRIORITY)
                .withSelector(selector.build())
                .withTreatment(treatment.build())
                .fromApp(appId)
                .makePermanent()
                .forTable(FILTER_TABLE).build();
        ops = install ? ops.add(rule) : ops.remove(rule);

        selector = DefaultTrafficSelector.builder();
        treatment = DefaultTrafficTreatment.builder();
        selector.matchEthType(Ethernet.TYPE_IPV4)
                .matchIPProtocol(IPv4.PROTOCOL_TCP)
                .matchTcpDst(TpPort.tpPort(BGP_PORT));
        treatment.punt();
        rule = DefaultFlowRule.builder()
                .forDevice(deviceId)
                .withPriority(FILTER_TABLE_HIGHEST_PRIORITY)
                .withSelector(selector.build())
                .withTreatment(treatment.build())
                .fromApp(appId)
                .makePermanent()
                .forTable(FILTER_TABLE).build();

        ops = install ? ops.add(rule) : ops.remove(rule);

        // Packet will be discard in PORT_VLAN table, no need to install rule in
        // filter table.

        // XXX does not tell me if packets are going to be dropped by default in
        // filter table or not? TTP says it will be dropped by default

        flowRuleService.apply(ops.build(new FlowRuleOperationsContext() {
            @Override
            public void onSuccess(FlowRuleOperations ops) {
                log.info("Provisioned filter table");
            }

            @Override
            public void onError(FlowRuleOperations ops) {
                log.info("Failed to provision filter table");
            }
        }));
    }

    private class InnerGroupListener implements GroupListener {
        @Override
        public void event(GroupEvent event) {
            if (event.type() == GroupEvent.Type.GROUP_ADDED) {
                GroupKey key = event.subject().appCookie();

                NextObjective obj = pendingGroups.getIfPresent(key);
                if (obj != null) {
                    flowObjectiveStore.putNextGroup(obj.id(), new CentecV350Group(key));
                    pass(obj);
                    pendingGroups.invalidate(key);
                }
            }
        }
    }


    private class GroupChecker implements Runnable {

        @Override
        public void run() {
            Set<GroupKey> keys = pendingGroups.asMap().keySet().stream()
                    .filter(key -> groupService.getGroup(deviceId, key) != null)
                    .collect(Collectors.toSet());

            keys.forEach(key -> {
                NextObjective obj = pendingGroups.getIfPresent(key);
                if (obj == null) {
                    return;
                }
                pass(obj);
                pendingGroups.invalidate(key);
                log.info("Heard back from group service for group {}. "
                        + "Applying pending forwarding objectives", obj.id());
                flowObjectiveStore.putNextGroup(obj.id(), new CentecV350Group(key));
            });
        }
    }

    private class CentecV350Group implements NextGroup {

        private final GroupKey key;

        public CentecV350Group(GroupKey key) {
            this.key = key;
        }

        @SuppressWarnings("unused")
        public GroupKey key() {
            return key;
        }

        @Override
        public byte[] data() {
            return appKryo.serialize(key);
        }

    }

    @Override
    public List<String> getNextMappings(NextGroup nextGroup) {
        // TODO Implementation deferred to vendor
        return null;
    }
}

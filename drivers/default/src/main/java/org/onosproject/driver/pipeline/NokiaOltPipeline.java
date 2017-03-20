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
package org.onosproject.driver.pipeline;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.RemovalCause;
import com.google.common.cache.RemovalNotification;
import com.google.common.collect.Lists;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.onlab.osgi.ServiceDirectory;
import org.onlab.packet.EthType;
import org.onlab.packet.IPv4;
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
import org.onosproject.net.flow.criteria.Criteria;
import org.onosproject.net.flow.criteria.Criterion;
import org.onosproject.net.flow.criteria.EthTypeCriterion;
import org.onosproject.net.flow.criteria.IPCriterion;
import org.onosproject.net.flow.criteria.IPProtocolCriterion;
import org.onosproject.net.flow.criteria.PortCriterion;
import org.onosproject.net.flow.criteria.VlanIdCriterion;
import org.onosproject.net.flow.instructions.Instruction;
import org.onosproject.net.flow.instructions.Instructions;
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
import org.onosproject.store.serializers.KryoNamespaces;
import org.onosproject.store.service.StorageService;
import org.slf4j.Logger;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.Iterator;


import static org.slf4j.LoggerFactory.getLogger;

/**
 * Pipeliner for OLT device.
 */

public class NokiaOltPipeline extends AbstractHandlerBehaviour implements Pipeliner {

    private static final Integer QQ_TABLE = 1;
    private static final short MCAST_VLAN = 4000;
    private static final String OLTCOOKIES = "olt-cookies-must-be-unique";
    private static final int EAPOL_FLOW_PRIORITY = 1200;
    private final Logger log = getLogger(getClass());

    private ServiceDirectory serviceDirectory;
    private FlowRuleService flowRuleService;
    private GroupService groupService;
    private CoreService coreService;
    private StorageService storageService;

    private DeviceId deviceId;
    private ApplicationId appId;


    protected FlowObjectiveStore flowObjectiveStore;

    private Cache<GroupKey, NextObjective> pendingGroups;

    protected static KryoNamespace appKryo = new KryoNamespace.Builder()
            .register(KryoNamespaces.API)
            .register(GroupKey.class)
            .register(DefaultGroupKey.class)
            .register(OLTPipelineGroup.class)
            .build("OltPipeline");
    @Override
    public void init(DeviceId deviceId, PipelinerContext context) {
        log.debug("Initiate OLT pipeline");
        this.serviceDirectory = context.directory();
        this.deviceId = deviceId;

        flowRuleService = serviceDirectory.get(FlowRuleService.class);
        coreService = serviceDirectory.get(CoreService.class);
        groupService = serviceDirectory.get(GroupService.class);
        flowObjectiveStore = context.store();
        storageService = serviceDirectory.get(StorageService.class);

        appId = coreService.registerApplication(
                "org.onosproject.driver.OLTPipeline");


        pendingGroups = CacheBuilder.newBuilder()
                .expireAfterWrite(20, TimeUnit.SECONDS)
                .removalListener((RemovalNotification<GroupKey, NextObjective> notification) -> {
                    if (notification.getCause() == RemovalCause.EXPIRED) {
                        fail(notification.getValue(), ObjectiveError.GROUPINSTALLATIONFAILED);
                    }
                }).build();

        groupService.addListener(new InnerGroupListener());

    }

    @Override
    public void filter(FilteringObjective filter) {
        Instructions.OutputInstruction output;

        if (filter.meta() != null && !filter.meta().immediate().isEmpty()) {
            output = (Instructions.OutputInstruction) filter.meta().immediate().stream()
                    .filter(t -> t.type().equals(Instruction.Type.OUTPUT))
                    .limit(1)
                    .findFirst().get();

            if (output == null || !output.port().equals(PortNumber.CONTROLLER)) {
                log.error("OLT can only filter packet to controller");
                fail(filter, ObjectiveError.UNSUPPORTED);
                return;
            }
        } else {
            fail(filter, ObjectiveError.BADPARAMS);
            return;
        }

        if (filter.key().type() != Criterion.Type.IN_PORT) {
            fail(filter, ObjectiveError.BADPARAMS);
            return;
        }

        EthTypeCriterion ethType = (EthTypeCriterion)
                filterForCriterion(filter.conditions(), Criterion.Type.ETH_TYPE);

        if (ethType == null) {
            fail(filter, ObjectiveError.BADPARAMS);
            return;
        }

        if (ethType.ethType().equals(EthType.EtherType.EAPOL.ethType())) {
            provisionEapol(filter, ethType, output);
        } else if (ethType.ethType().equals(EthType.EtherType.IPV4.ethType())) {
            IPProtocolCriterion ipProto = (IPProtocolCriterion)
                    filterForCriterion(filter.conditions(), Criterion.Type.IP_PROTO);
            if (ipProto.protocol() == IPv4.PROTOCOL_IGMP) {
                provisionIgmp(filter, ethType, ipProto, output);
            } else {
                log.error("OLT can only filter igmp");
                fail(filter, ObjectiveError.UNSUPPORTED);
            }
        } else {
            log.error("OLT can only filter eapol and igmp");
            fail(filter, ObjectiveError.UNSUPPORTED);
        }

    }

    private void installObjective(FlowRule.Builder ruleBuilder, Objective objective) {
        FlowRuleOperations.Builder flowBuilder = FlowRuleOperations.builder();
        switch (objective.op()) {

            case ADD:
                flowBuilder.add(ruleBuilder.build());
                break;
            case REMOVE:
                flowBuilder.remove(ruleBuilder.build());
                break;
            default:
                log.warn("Unknown operation {}", objective.op());
        }

        flowRuleService.apply(flowBuilder.build(new FlowRuleOperationsContext() {
            @Override
            public void onSuccess(FlowRuleOperations ops) {
                objective.context().ifPresent(context -> context.onSuccess(objective));
            }

            @Override
            public void onError(FlowRuleOperations ops) {
                objective.context()
                        .ifPresent(context -> context.onError(objective, ObjectiveError.FLOWINSTALLATIONFAILED));
            }
        }));
    }

    @Override
    public void forward(ForwardingObjective fwd) {

        if (checkForMulticast(fwd)) {
            processMulticastRule(fwd);
            return;
        }

        if (checkForEAPOL(fwd)) {
            log.warn("Discarding EAPOL flow which is not supported on this pipeline");
            return;
        }

        TrafficTreatment treatment = fwd.treatment();

        List<Instruction> instructions = treatment.allInstructions();

        Optional<Instruction> vlanIntruction = instructions.stream()
                .filter(i -> i.type() == Instruction.Type.L2MODIFICATION)
                .filter(i -> ((L2ModificationInstruction) i).subtype() ==
                        L2ModificationInstruction.L2SubType.VLAN_PUSH ||
                        ((L2ModificationInstruction) i).subtype() ==
                                L2ModificationInstruction.L2SubType.VLAN_POP)
                .findAny();

        if (vlanIntruction.isPresent()) {
            L2ModificationInstruction vlanIns =
                    (L2ModificationInstruction) vlanIntruction.get();

            if (vlanIns.subtype() == L2ModificationInstruction.L2SubType.VLAN_PUSH) {
                installUpstreamRules(fwd);
            } else if (vlanIns.subtype() == L2ModificationInstruction.L2SubType.VLAN_POP) {
                installDownstreamRules(fwd);
            } else {
                log.error("Unknown OLT operation: {}", fwd);
                fail(fwd, ObjectiveError.UNSUPPORTED);
                return;
            }

            pass(fwd);
        } else {
            TrafficSelector selector = fwd.selector();

            if (fwd.treatment() != null) {
                // Deal with SPECIFIC and VERSATILE in the same manner.
                FlowRule.Builder ruleBuilder = DefaultFlowRule.builder()
                        .forDevice(deviceId)
                        .withSelector(selector)
                        .fromApp(fwd.appId())
                        .withPriority(fwd.priority())
                        .withTreatment(fwd.treatment());

                if (fwd.permanent()) {
                    ruleBuilder.makePermanent();
                } else {
                    ruleBuilder.makeTemporary(fwd.timeout());
                }
                installObjective(ruleBuilder, fwd);

            } else {
                log.error("No treatment error: {}", fwd);
                fail(fwd, ObjectiveError.UNSUPPORTED);
            }
        }

    }


    @Override
    public void next(NextObjective nextObjective) {
        if (nextObjective.type() != NextObjective.Type.BROADCAST) {
            log.error("OLT only supports broadcast groups.");
            fail(nextObjective, ObjectiveError.BADPARAMS);
        }

        if (nextObjective.next().size() != 1) {
            log.error("OLT only supports singleton broadcast groups.");
            fail(nextObjective, ObjectiveError.BADPARAMS);
        }

        TrafficTreatment treatment = nextObjective.next().stream().findFirst().get();


        GroupBucket bucket = DefaultGroupBucket.createAllGroupBucket(treatment);
        GroupKey key = new DefaultGroupKey(appKryo.serialize(nextObjective.id()));


        pendingGroups.put(key, nextObjective);

        switch (nextObjective.op()) {
            case ADD:
                GroupDescription groupDesc =
                        new DefaultGroupDescription(deviceId,
                                                    GroupDescription.Type.ALL,
                                                    new GroupBuckets(Collections.singletonList(bucket)),
                                                    key,
                                                    null,
                                                    nextObjective.appId());
                groupService.addGroup(groupDesc);
                break;
            case REMOVE:
                groupService.removeGroup(deviceId, key, nextObjective.appId());
                break;
            case ADD_TO_EXISTING:
                groupService.addBucketsToGroup(deviceId, key,
                                               new GroupBuckets(Collections.singletonList(bucket)),
                                               key, nextObjective.appId());
                break;
            case REMOVE_FROM_EXISTING:
                groupService.removeBucketsFromGroup(deviceId, key,
                                                    new GroupBuckets(Collections.singletonList(bucket)),
                                                    key, nextObjective.appId());
                break;
            default:
                log.warn("Unknown next objective operation: {}", nextObjective.op());
        }


    }

    private void processMulticastRule(ForwardingObjective fwd) {
        if (fwd.nextId() == null) {
            log.error("Multicast objective does not have a next id");
            fail(fwd, ObjectiveError.BADPARAMS);
        }

        GroupKey key = getGroupForNextObjective(fwd.nextId());

        if (key == null) {
            log.error("Group for forwarding objective missing: {}", fwd);
            fail(fwd, ObjectiveError.GROUPMISSING);
        }

        Group group = groupService.getGroup(deviceId, key);
        TrafficTreatment treatment =
                buildTreatment(Instructions.createGroup(group.id()));

        FlowRule rule = DefaultFlowRule.builder()
                .fromApp(fwd.appId())
                .forDevice(deviceId)
                .forTable(0)
                .makePermanent()
                .withPriority(fwd.priority())
                .withSelector(fwd.selector())
                .withTreatment(treatment)
                .build();

        FlowRuleOperations.Builder builder = FlowRuleOperations.builder();
        switch (fwd.op()) {

            case ADD:
                builder.add(rule);
                break;
            case REMOVE:
                builder.remove(rule);
                break;
            case ADD_TO_EXISTING:
            case REMOVE_FROM_EXISTING:
                break;
            default:
                log.warn("Unknown forwarding operation: {}", fwd.op());
        }

        applyFlowRules(builder, fwd);

    }

    private boolean checkForMulticast(ForwardingObjective fwd) {

        IPCriterion ip = (IPCriterion) filterForCriterion(fwd.selector().criteria(),
                                                          Criterion.Type.IPV4_DST);

        if (ip == null) {
            return false;
        }

        return ip.ip().isMulticast();

    }

    private boolean checkForEAPOL(ForwardingObjective fwd) {
        EthTypeCriterion ethType = (EthTypeCriterion)
                filterForCriterion(fwd.selector().criteria(), Criterion.Type.ETH_TYPE);

        return ethType != null && ethType.ethType().equals(EthType.EtherType.EAPOL.ethType());
    }
    private GroupKey getGroupForNextObjective(Integer nextId) {
        NextGroup next = flowObjectiveStore.getNextGroup(nextId);
        return appKryo.deserialize(next.data());

    }

    private void installDownstreamRules(ForwardingObjective fwd) {
        List<Pair<Instruction, Instruction>> vlanOps =
                vlanOps(fwd,
                        L2ModificationInstruction.L2SubType.VLAN_POP);

        if (vlanOps == null) {
            return;
        }

        Instructions.OutputInstruction output = (Instructions.OutputInstruction) fetchOutput(fwd, "downstream");

        if (output == null) {
            return;
        }

        Pair<Instruction, Instruction> popAndRewrite = vlanOps.remove(0);

        TrafficSelector selector = fwd.selector();

        Criterion outerVlan = selector.getCriterion(Criterion.Type.VLAN_VID);
        Criterion innerVlan = selector.getCriterion(Criterion.Type.INNER_VLAN_VID);
        Criterion inport = selector.getCriterion(Criterion.Type.IN_PORT);
        Criterion bullshit = Criteria.matchMetadata(output.port().toLong());

        if (outerVlan == null || innerVlan == null || inport == null) {
            log.error("Forwarding objective is underspecified: {}", fwd);
            fail(fwd, ObjectiveError.BADPARAMS);
            return;
        }

        Criterion innerVid = Criteria.matchVlanId(((VlanIdCriterion) innerVlan).vlanId());

        FlowRule.Builder outer = DefaultFlowRule.builder()
                .fromApp(fwd.appId())
                .forDevice(deviceId)
                .makePermanent()
                .withPriority(fwd.priority())
                .withSelector(buildSelector(inport, outerVlan, bullshit))
                .withTreatment(buildTreatment(popAndRewrite.getLeft(),
                        Instructions.transition(QQ_TABLE)));

        FlowRule.Builder inner = DefaultFlowRule.builder()
                .fromApp(fwd.appId())
                .forDevice(deviceId)
                .forTable(QQ_TABLE)
                .makePermanent()
                .withPriority(fwd.priority())
                .withSelector(buildSelector(inport, innerVid))
                .withTreatment(buildTreatment(popAndRewrite.getLeft(),
                                              output));

        applyRules(fwd, inner, outer);

    }

    private boolean hasUntaggedVlanTag(TrafficSelector selector) {
        Iterator<Criterion> iter = selector.criteria().iterator();

        while (iter.hasNext()) {
            Criterion criterion = iter.next();
            if (criterion.type() == Criterion.Type.VLAN_VID &&
                    ((VlanIdCriterion) criterion).vlanId().toShort() == VlanId.UNTAGGED) {
                return true;
            }
        }

        return false;
    }

    private void installUpstreamRules(ForwardingObjective fwd) {
        List<Pair<Instruction, Instruction>> vlanOps =
                vlanOps(fwd,
                        L2ModificationInstruction.L2SubType.VLAN_PUSH);
        FlowRule.Builder inner;

        if (vlanOps == null) {
            return;
        }

        Instruction output = fetchOutput(fwd, "upstream");

        if (output == null) {
            return;
        }

        Pair<Instruction, Instruction> innerPair = vlanOps.remove(0);

        Pair<Instruction, Instruction> outerPair = vlanOps.remove(0);


        if (hasUntaggedVlanTag(fwd.selector())) {
            inner = DefaultFlowRule.builder()
                    .fromApp(fwd.appId())
                    .forDevice(deviceId)
                    .makePermanent()
                    .withPriority(fwd.priority())
                    .withSelector(fwd.selector())
                    .withTreatment(buildTreatment(innerPair.getLeft(),
                            innerPair.getRight(),
                            Instructions.transition(QQ_TABLE)));
        } else {
            inner = DefaultFlowRule.builder()
                    .fromApp(fwd.appId())
                    .forDevice(deviceId)
                    .makePermanent()
                    .withPriority(fwd.priority())
                    .withSelector(fwd.selector())
                    .withTreatment(buildTreatment(
                            innerPair.getRight(),
                            Instructions.transition(QQ_TABLE)));
        }


        PortCriterion inPort = (PortCriterion)
                fwd.selector().getCriterion(Criterion.Type.IN_PORT);

        VlanId cVlanId = ((L2ModificationInstruction.ModVlanIdInstruction)
                innerPair.getRight()).vlanId();

        FlowRule.Builder outer = DefaultFlowRule.builder()
                .fromApp(fwd.appId())
                .forDevice(deviceId)
                .forTable(QQ_TABLE)
                .makePermanent()
                .withPriority(fwd.priority())
                .withSelector(buildSelector(inPort,
                                            Criteria.matchVlanId(cVlanId)))
                .withTreatment(buildTreatment(outerPair.getLeft(),
                                              outerPair.getRight(),
                                              output));

        applyRules(fwd, inner, outer);

    }

    private Instruction fetchOutput(ForwardingObjective fwd, String direction) {
        Instruction output = fwd.treatment().allInstructions().stream()
                .filter(i -> i.type() == Instruction.Type.OUTPUT)
                .findFirst().orElse(null);

        if (output == null) {
            log.error("OLT {} rule has no output", direction);
            fail(fwd, ObjectiveError.BADPARAMS);
            return null;
        }
        return output;
    }

    private List<Pair<Instruction, Instruction>> vlanOps(ForwardingObjective fwd,
                                                         L2ModificationInstruction.L2SubType type) {

        List<Pair<Instruction, Instruction>> vlanOps = findVlanOps(
                fwd.treatment().allInstructions(), type);

        if (vlanOps == null) {
            String direction = type == L2ModificationInstruction.L2SubType.VLAN_POP
                    ? "downstream" : "upstream";
            log.error("Missing vlan operations in {} forwarding: {}", direction, fwd);
            fail(fwd, ObjectiveError.BADPARAMS);
            return null;
        }
        return vlanOps;
    }


    private List<Pair<Instruction, Instruction>> findVlanOps(List<Instruction> instructions,
                                                             L2ModificationInstruction.L2SubType type) {

        List<Instruction> vlanPushs = findL2Instructions(
                type,
                instructions);
        List<Instruction> vlanSets = findL2Instructions(
                L2ModificationInstruction.L2SubType.VLAN_ID,
                instructions);

        if (vlanPushs.size() != vlanSets.size()) {
            return null;
        }

        List<Pair<Instruction, Instruction>> pairs = Lists.newArrayList();

        for (int i = 0; i < vlanPushs.size(); i++) {
            pairs.add(new ImmutablePair<>(vlanPushs.get(i), vlanSets.get(i)));
        }
        return pairs;
    }

    private List<Instruction> findL2Instructions(L2ModificationInstruction.L2SubType subType,
                                                 List<Instruction> actions) {
        return actions.stream()
                .filter(i -> i.type() == Instruction.Type.L2MODIFICATION)
                .filter(i -> ((L2ModificationInstruction) i).subtype() == subType)
                .collect(Collectors.toList());
    }

    private void provisionEapol(FilteringObjective filter,
                                EthTypeCriterion ethType,
                                Instructions.OutputInstruction output) {

        TrafficSelector selector = buildSelector(filter.key(), ethType);
        TrafficTreatment treatment = buildTreatment(output);
        buildAndApplyRule(filter, selector, treatment, EAPOL_FLOW_PRIORITY);

    }

    private void provisionIgmp(FilteringObjective filter, EthTypeCriterion ethType,
                               IPProtocolCriterion ipProto,
                               Instructions.OutputInstruction output) {
        TrafficSelector selector = buildSelector(filter.key(), ethType, ipProto);
        TrafficTreatment treatment = buildTreatment(output);
        buildAndApplyRule(filter, selector, treatment);
    }

    private void buildAndApplyRule(FilteringObjective filter, TrafficSelector selector,
                                   TrafficTreatment treatment) {
        buildAndApplyRule(filter, selector, treatment, filter.priority());
    }

    private void buildAndApplyRule(FilteringObjective filter, TrafficSelector selector,
                                   TrafficTreatment treatment, int priority) {
        FlowRule rule = DefaultFlowRule.builder()
                .fromApp(filter.appId())
                .forDevice(deviceId)
                .forTable(0)
                .makePermanent()
                .withSelector(selector)
                .withTreatment(treatment)
                .withPriority(priority)
                .build();

        FlowRuleOperations.Builder opsBuilder = FlowRuleOperations.builder();

        switch (filter.type()) {
            case PERMIT:
                opsBuilder.add(rule);
                break;
            case DENY:
                opsBuilder.remove(rule);
                break;
            default:
                log.warn("Unknown filter type : {}", filter.type());
                fail(filter, ObjectiveError.UNSUPPORTED);
        }

        applyFlowRules(opsBuilder, filter);
    }

    private void applyRules(ForwardingObjective fwd,
                            FlowRule.Builder inner, FlowRule.Builder outer) {
        FlowRuleOperations.Builder builder = FlowRuleOperations.builder();
        switch (fwd.op()) {
            case ADD:
                builder.add(inner.build()).add(outer.build());
                break;
            case REMOVE:
                builder.remove(inner.build()).remove(outer.build());
                break;
            case ADD_TO_EXISTING:
                break;
            case REMOVE_FROM_EXISTING:
                break;
            default:
                log.warn("Unknown forwarding operation: {}", fwd.op());
        }

        applyFlowRules(builder, fwd);
    }

    private void applyFlowRules(FlowRuleOperations.Builder builder,
                                Objective objective) {
        flowRuleService.apply(builder.build(new FlowRuleOperationsContext() {
            @Override
            public void onSuccess(FlowRuleOperations ops) {
                pass(objective);
            }

            @Override
            public void onError(FlowRuleOperations ops) {
                fail(objective, ObjectiveError.FLOWINSTALLATIONFAILED);
            }
        }));
    }

    private Criterion filterForCriterion(Collection<Criterion> criteria, Criterion.Type type) {
        return criteria.stream()
                .filter(c -> c.type().equals(type))
                .limit(1)
                .findFirst().orElse(null);
    }

    private TrafficSelector buildSelector(Criterion... criteria) {


        TrafficSelector.Builder sBuilder = DefaultTrafficSelector.builder();

        for (Criterion c : criteria) {
            sBuilder.add(c);
        }

        return sBuilder.build();
    }

    private TrafficTreatment buildTreatment(Instruction... instructions) {


        TrafficTreatment.Builder tBuilder = DefaultTrafficTreatment.builder();

        for (Instruction i : instructions) {
            tBuilder.add(i);
        }

        return tBuilder.build();
    }


    private void fail(Objective obj, ObjectiveError error) {
        obj.context().ifPresent(context -> context.onError(obj, error));
    }

    private void pass(Objective obj) {
        obj.context().ifPresent(context -> context.onSuccess(obj));
    }


    private class InnerGroupListener implements GroupListener {
        @Override
        public void event(GroupEvent event) {
            if (event.type() == GroupEvent.Type.GROUP_ADDED || event.type() == GroupEvent.Type.GROUP_UPDATED) {
                GroupKey key = event.subject().appCookie();

                NextObjective obj = pendingGroups.getIfPresent(key);
                if (obj != null) {
                    flowObjectiveStore.putNextGroup(obj.id(), new OLTPipelineGroup(key));
                    pass(obj);
                    pendingGroups.invalidate(key);
                }
            }
        }
    }

    private static class OLTPipelineGroup implements NextGroup {

        private final GroupKey key;

        public OLTPipelineGroup(GroupKey key) {
            this.key = key;
        }

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

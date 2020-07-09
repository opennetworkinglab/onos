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
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.onlab.osgi.ServiceDirectory;
import org.onlab.packet.EthType;
import org.onlab.packet.IPv4;
import org.onlab.packet.IPv6;
import org.onlab.packet.IpPrefix;
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
import org.onosproject.net.flow.criteria.UdpPortCriterion;
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

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.onosproject.core.CoreService.CORE_APP_NAME;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Pipeliner for OLT device.
 */

public class OltPipeline extends AbstractHandlerBehaviour implements Pipeliner {

    private static final Integer QQ_TABLE = 1;
    private static final int NO_ACTION_PRIORITY = 500;
    private static final String DOWNSTREAM = "downstream";
    private static final String UPSTREAM = "upstream";
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
                log.warn("OLT can only filter packet to controller");
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
            provisionEthTypeBasedFilter(filter, ethType, output);
        } else if (ethType.ethType().equals(EthType.EtherType.LLDP.ethType())) {
            provisionEthTypeBasedFilter(filter, ethType, output);

        } else if (ethType.ethType().equals(EthType.EtherType.IPV4.ethType())) {
            IPProtocolCriterion ipProto = (IPProtocolCriterion)
                    filterForCriterion(filter.conditions(), Criterion.Type.IP_PROTO);
            if (ipProto == null) {
                log.warn("OLT can only filter IGMP and DHCP");
                fail(filter, ObjectiveError.UNSUPPORTED);
                return;
            }
            if (ipProto.protocol() == IPv4.PROTOCOL_IGMP) {
                provisionIgmp(filter, ethType, ipProto, output);
            } else if (ipProto.protocol() == IPv4.PROTOCOL_UDP) {
                UdpPortCriterion udpSrcPort = (UdpPortCriterion)
                        filterForCriterion(filter.conditions(), Criterion.Type.UDP_SRC);

                UdpPortCriterion udpDstPort = (UdpPortCriterion)
                        filterForCriterion(filter.conditions(), Criterion.Type.UDP_DST);

                if ((udpSrcPort.udpPort().toInt() == 67 && udpDstPort.udpPort().toInt() == 68) ||
                    (udpSrcPort.udpPort().toInt() == 68 && udpDstPort.udpPort().toInt() == 67)) {
                    provisionDhcp(filter, ethType, ipProto, udpSrcPort, udpDstPort, output);
                } else {
                    log.warn("Filtering rule with unsupported UDP src {} or dst {} port", udpSrcPort, udpDstPort);
                    fail(filter, ObjectiveError.UNSUPPORTED);
                }
            } else {
                log.warn("Currently supporting only IGMP and DHCP filters for IPv4 packets");
                fail(filter, ObjectiveError.UNSUPPORTED);
            }
        } else if (ethType.ethType().equals(EthType.EtherType.IPV6.ethType())) {
            IPProtocolCriterion ipProto = (IPProtocolCriterion)
                    filterForCriterion(filter.conditions(), Criterion.Type.IP_PROTO);
            if (ipProto == null) {
                log.warn("OLT can only filter DHCP");
                fail(filter, ObjectiveError.UNSUPPORTED);
                return;
            }
            if (ipProto.protocol() == IPv6.PROTOCOL_UDP) {
                UdpPortCriterion udpSrcPort = (UdpPortCriterion)
                        filterForCriterion(filter.conditions(), Criterion.Type.UDP_SRC);

                UdpPortCriterion udpDstPort = (UdpPortCriterion)
                        filterForCriterion(filter.conditions(), Criterion.Type.UDP_DST);

                if ((udpSrcPort.udpPort().toInt() == 546 && udpDstPort.udpPort().toInt() == 547) ||
                    (udpSrcPort.udpPort().toInt() == 547 && udpDstPort.udpPort().toInt() == 546)) {
                    provisionDhcp(filter, ethType, ipProto, udpSrcPort, udpDstPort, output);
                } else {
                    log.warn("Filtering rule with unsupported UDP src {} or dst {} port", udpSrcPort, udpDstPort);
                    fail(filter, ObjectiveError.UNSUPPORTED);
                }
            } else {
                log.warn("Currently supporting only DHCP filters for IPv6 packets");
                fail(filter, ObjectiveError.UNSUPPORTED);
            }
        } else {
            log.warn("\nOnly the following are Supported in OLT for filter ->\n"
                    + "ETH TYPE : EAPOL, LLDP and IPV4\n"
                    + "IPV4 TYPE: IGMP and UDP (for DHCP)"
                    + "IPV6 TYPE: UDP (for DHCP)");
            fail(filter, ObjectiveError.UNSUPPORTED);
        }

    }


    @Override
    public void forward(ForwardingObjective fwd) {
        log.debug("Installing forwarding objective {}", fwd);
        if (checkForMulticast(fwd)) {
            processMulticastRule(fwd);
            return;
        }

        TrafficTreatment treatment = fwd.treatment();

        List<Instruction> instructions = treatment.allInstructions();

        Optional<Instruction> vlanInstruction = instructions.stream()
                .filter(i -> i.type() == Instruction.Type.L2MODIFICATION)
                .filter(i -> ((L2ModificationInstruction) i).subtype() ==
                        L2ModificationInstruction.L2SubType.VLAN_PUSH ||
                        ((L2ModificationInstruction) i).subtype() ==
                                L2ModificationInstruction.L2SubType.VLAN_POP)
                .findAny();


        if (!vlanInstruction.isPresent()) {
            installNoModificationRules(fwd);
        } else {
            L2ModificationInstruction vlanIns =
                    (L2ModificationInstruction) vlanInstruction.get();
            if (vlanIns.subtype() == L2ModificationInstruction.L2SubType.VLAN_PUSH) {
                installUpstreamRules(fwd);
            } else if (vlanIns.subtype() == L2ModificationInstruction.L2SubType.VLAN_POP) {
                installDownstreamRules(fwd);
            } else {
                log.error("Unknown OLT operation: {}", fwd);
                fail(fwd, ObjectiveError.UNSUPPORTED);
                return;
            }
        }

        pass(fwd);

    }


    @Override
    public void next(NextObjective nextObjective) {
        if (nextObjective.type() != NextObjective.Type.BROADCAST) {
            log.error("OLT only supports broadcast groups.");
            fail(nextObjective, ObjectiveError.BADPARAMS);
            return;
        }

        if (nextObjective.next().size() != 1 && !nextObjective.op().equals(Objective.Operation.REMOVE)) {
            log.error("OLT only supports singleton broadcast groups.");
            fail(nextObjective, ObjectiveError.BADPARAMS);
            return;
        }

        Optional<TrafficTreatment> treatmentOpt = nextObjective.next().stream().findFirst();
        if (treatmentOpt.isEmpty() && !nextObjective.op().equals(Objective.Operation.REMOVE)) {
            log.error("Next objective {} does not have a treatment", nextObjective);
            fail(nextObjective, ObjectiveError.BADPARAMS);
            return;
        }

        GroupKey key = new DefaultGroupKey(appKryo.serialize(nextObjective.id()));

        pendingGroups.put(key, nextObjective);
        log.trace("NextObjective Operation {}", nextObjective.op());
        switch (nextObjective.op()) {
            case ADD:
                GroupDescription groupDesc =
                        new DefaultGroupDescription(deviceId,
                                                    GroupDescription.Type.ALL,
                                                    new GroupBuckets(
                                                            Collections.singletonList(
                                                                buildBucket(treatmentOpt.get()))),
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
                                               new GroupBuckets(
                                                       Collections.singletonList(
                                                               buildBucket(treatmentOpt.get()))),
                                               key, nextObjective.appId());
                break;
            case REMOVE_FROM_EXISTING:
                groupService.removeBucketsFromGroup(deviceId, key,
                                                    new GroupBuckets(
                                                            Collections.singletonList(
                                                                buildBucket(treatmentOpt.get()))),
                                                    key, nextObjective.appId());
                break;
            default:
                log.warn("Unknown next objective operation: {}", nextObjective.op());
        }


    }

    private GroupBucket buildBucket(TrafficTreatment treatment) {
        return DefaultGroupBucket.createAllGroupBucket(treatment);
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

        TrafficSelector.Builder selectorBuilder = buildIpv4SelectorForMulticast(fwd);

        FlowRule rule = DefaultFlowRule.builder()
                .fromApp(fwd.appId())
                .forDevice(deviceId)
                .forTable(0)
                .makePermanent()
                .withPriority(fwd.priority())
                .withSelector(selectorBuilder.build())
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

    private TrafficSelector.Builder buildIpv4SelectorForMulticast(ForwardingObjective fwd) {
        TrafficSelector.Builder builderToUpdate = DefaultTrafficSelector.builder();

        Optional<Criterion> vlanIdCriterion = readFromSelector(fwd.meta(), Criterion.Type.VLAN_VID);
        if (vlanIdCriterion.isPresent()) {
            VlanId assignedVlan = ((VlanIdCriterion) vlanIdCriterion.get()).vlanId();
            builderToUpdate.matchVlanId(assignedVlan);
        }

        Optional<Criterion> innerVlanIdCriterion = readFromSelector(fwd.meta(), Criterion.Type.INNER_VLAN_VID);
        if (innerVlanIdCriterion.isPresent()) {
            VlanId assignedInnerVlan = ((VlanIdCriterion) innerVlanIdCriterion.get()).vlanId();
            builderToUpdate.matchMetadata(assignedInnerVlan.toShort());
        }

        Optional<Criterion> ethTypeCriterion = readFromSelector(fwd.selector(), Criterion.Type.ETH_TYPE);
        if (ethTypeCriterion.isPresent()) {
            EthType ethType = ((EthTypeCriterion) ethTypeCriterion.get()).ethType();
            builderToUpdate.matchEthType(ethType.toShort());
        }

        Optional<Criterion> ipv4DstCriterion = readFromSelector(fwd.selector(), Criterion.Type.IPV4_DST);
        if (ipv4DstCriterion.isPresent()) {
            IpPrefix ipv4Dst = ((IPCriterion) ipv4DstCriterion.get()).ip();
            builderToUpdate.matchIPDst(ipv4Dst);
        }

        return builderToUpdate;
    }

    static Optional<Criterion> readFromSelector(TrafficSelector selector, Criterion.Type type) {
        if (selector == null) {
            return Optional.empty();
        }
        Criterion criterion = selector.getCriterion(type);
        return (criterion == null)
                ? Optional.empty() : Optional.of(criterion);
    }

    private boolean checkForMulticast(ForwardingObjective fwd) {

        IPCriterion ip = (IPCriterion) filterForCriterion(fwd.selector().criteria(),
                                                          Criterion.Type.IPV4_DST);

        if (ip == null) {
            return false;
        }

        return ip.ip().isMulticast();

    }

    private GroupKey getGroupForNextObjective(Integer nextId) {
        NextGroup next = flowObjectiveStore.getNextGroup(nextId);
        return appKryo.deserialize(next.data());

    }

    private void installNoModificationRules(ForwardingObjective fwd) {
        Instructions.OutputInstruction output = (Instructions.OutputInstruction) fetchOutput(fwd, DOWNSTREAM);
        Instructions.MetadataInstruction writeMetadata = fetchWriteMetadata(fwd);
        Instructions.MeterInstruction meter = (Instructions.MeterInstruction) fetchMeter(fwd);

        TrafficSelector selector = fwd.selector();

        Criterion inport = selector.getCriterion(Criterion.Type.IN_PORT);
        Criterion outerVlan = selector.getCriterion(Criterion.Type.VLAN_VID);
        Criterion innerVlan = selector.getCriterion(Criterion.Type.INNER_VLAN_VID);

        if (inport == null || output == null || innerVlan == null || outerVlan == null) {
            // Avoid logging a non-error from lldp, bbdp and eapol core flows.
            if (!fwd.appId().name().equals(CORE_APP_NAME)) {
                log.error("Forwarding objective is underspecified: {}", fwd);
            } else {
                log.debug("Not installing unsupported core generated flow {}", fwd);
            }
            fail(fwd, ObjectiveError.BADPARAMS);
            return;
        }


        FlowRule.Builder outer = DefaultFlowRule.builder()
                .fromApp(fwd.appId())
                .forDevice(deviceId)
                .makePermanent()
                .withPriority(fwd.priority())
                .withSelector(buildSelector(inport, outerVlan))
                .withTreatment(buildTreatment(output, writeMetadata, meter));

        applyRules(fwd, outer);
    }

    private void installDownstreamRules(ForwardingObjective fwd) {
        Instructions.OutputInstruction output = (Instructions.OutputInstruction) fetchOutput(fwd, DOWNSTREAM);

        if (output == null) {
            return;
        }

        TrafficSelector selector = fwd.selector();

        Criterion outerVlan = selector.getCriterion(Criterion.Type.VLAN_VID);
        Criterion outerPbit = selector.getCriterion(Criterion.Type.VLAN_PCP);
        Criterion innerVlanCriterion = selector.getCriterion(Criterion.Type.INNER_VLAN_VID);
        Criterion inport = selector.getCriterion(Criterion.Type.IN_PORT);
        Criterion dstMac = selector.getCriterion(Criterion.Type.ETH_DST);

        if (outerVlan == null || innerVlanCriterion == null || inport == null) {
            // Avoid logging a non-error from lldp, bbdp and eapol core flows.
            if (!fwd.appId().name().equals(CORE_APP_NAME)) {
                log.error("Forwarding objective is underspecified: {}", fwd);
            } else {
                log.debug("Not installing unsupported core generated flow {}", fwd);
            }
            fail(fwd, ObjectiveError.BADPARAMS);
            return;
        }

        VlanId innerVlan = ((VlanIdCriterion) innerVlanCriterion).vlanId();
        Criterion innerVid = Criteria.matchVlanId(innerVlan);

        if (innerVlan.toShort() == VlanId.ANY_VALUE) {
            TrafficSelector outerSelector = buildSelector(inport, outerVlan, outerPbit, dstMac);
            installDownstreamRulesForAnyVlan(fwd, output, outerSelector, buildSelector(inport,
                    Criteria.matchVlanId(VlanId.ANY)));
        } else {
            // Required to differentiate the same match flows
            // Please note that S tag and S p bit values will be same for the same service - so conflict flows!
            // Metadata match criteria solves the conflict issue - but not used by the voltha
            // Maybe - find a better way to solve the above problem
            Criterion metadata = Criteria.matchMetadata(innerVlan.toShort());
            TrafficSelector outerSelector = buildSelector(inport, metadata, outerVlan, outerPbit, dstMac);
            installDownstreamRulesForVlans(fwd, output, outerSelector, buildSelector(inport, innerVid));
        }
    }

    private void installDownstreamRulesForVlans(ForwardingObjective fwd, Instruction output,
                                                TrafficSelector outerSelector, TrafficSelector innerSelector) {

        List<Pair<Instruction, Instruction>> vlanOps =
                vlanOps(fwd,
                        L2ModificationInstruction.L2SubType.VLAN_POP);

        if (vlanOps == null || vlanOps.isEmpty()) {
            return;
        }

        Pair<Instruction, Instruction> popAndRewrite = vlanOps.remove(0);

        TrafficTreatment innerTreatment;
        VlanId setVlanId = ((L2ModificationInstruction.ModVlanIdInstruction) popAndRewrite.getRight()).vlanId();
        if (VlanId.NONE.equals(setVlanId)) {
            innerTreatment = (buildTreatment(popAndRewrite.getLeft(), fetchMeter(fwd),
                    writeMetadataIncludingOnlyTp(fwd), output));
        } else {
            innerTreatment = (buildTreatment(popAndRewrite.getRight(),
                    fetchMeter(fwd), writeMetadataIncludingOnlyTp(fwd), output));
        }

        List<Instruction> setVlanPcps = findL2Instructions(L2ModificationInstruction.L2SubType.VLAN_PCP,
                fwd.treatment().allInstructions());

        Instruction innerPbitSet = null;

        if (setVlanPcps != null && !setVlanPcps.isEmpty()) {
            innerPbitSet = setVlanPcps.get(0);
        }

        VlanId remarkInnerVlan = null;
        Optional<Criterion> vlanIdCriterion = readFromSelector(innerSelector, Criterion.Type.VLAN_VID);
        if (vlanIdCriterion.isPresent()) {
            remarkInnerVlan = ((VlanIdCriterion) vlanIdCriterion.get()).vlanId();
        }

        Instruction modVlanId = null;
        if (innerPbitSet != null) {
            modVlanId = Instructions.modVlanId(remarkInnerVlan);
        }

        //match: in port (nni), s-tag
        //action: pop vlan (s-tag), write metadata, go to table 1, meter
        FlowRule.Builder outer = DefaultFlowRule.builder()
                .fromApp(fwd.appId())
                .forDevice(deviceId)
                .makePermanent()
                .withPriority(fwd.priority())
                .withSelector(outerSelector)
                .withTreatment(buildTreatment(popAndRewrite.getLeft(), modVlanId,
                        innerPbitSet, fetchMeter(fwd), fetchWriteMetadata(fwd), Instructions.transition(QQ_TABLE)));

        //match: in port (nni), c-tag
        //action: immediate: write metadata and pop, meter, output
        FlowRule.Builder inner = DefaultFlowRule.builder()
                .fromApp(fwd.appId())
                .forDevice(deviceId)
                .forTable(QQ_TABLE)
                .makePermanent()
                .withPriority(fwd.priority())
                .withSelector(innerSelector)
                .withTreatment(innerTreatment);
        applyRules(fwd, inner, outer);
    }

    private void installDownstreamRulesForAnyVlan(ForwardingObjective fwd, Instruction output,
                                                  TrafficSelector outerSelector, TrafficSelector innerSelector) {

        //match: in port (nni), s-tag
        //action: immediate: write metadata, pop vlan, meter and go to table 1
        FlowRule.Builder outer = DefaultFlowRule.builder()
                .fromApp(fwd.appId())
                .forDevice(deviceId)
                .makePermanent()
                .withPriority(fwd.priority())
                .withSelector(outerSelector)
                .withTreatment(buildTreatment(Instructions.popVlan(), fetchMeter(fwd),
                        fetchWriteMetadata(fwd), Instructions.transition(QQ_TABLE)));

        //match: in port (nni) and s-tag
        //action: immediate : write metadata, meter and output
        FlowRule.Builder inner = DefaultFlowRule.builder()
                .fromApp(fwd.appId())
                .forDevice(deviceId)
                .forTable(QQ_TABLE)
                .makePermanent()
                .withPriority(fwd.priority())
                .withSelector(innerSelector)
                .withTreatment(buildTreatment(fetchMeter(fwd),
                        writeMetadataIncludingOnlyTp(fwd), output));

        applyRules(fwd, inner, outer);
    }

    private void installUpstreamRules(ForwardingObjective fwd) {
        List<Pair<Instruction, Instruction>> vlanOps =
                vlanOps(fwd,
                        L2ModificationInstruction.L2SubType.VLAN_PUSH);

        if (vlanOps == null || vlanOps.isEmpty()) {
            return;
        }

        Instruction output = fetchOutput(fwd, UPSTREAM);

        if (output == null) {
            return;
        }

        Pair<Instruction, Instruction> outerPair = vlanOps.remove(0);

        boolean noneValueVlanStatus = checkNoneVlanCriteria(fwd);
        boolean anyValueVlanStatus = checkAnyVlanMatchCriteria(fwd);

        if (anyValueVlanStatus) {
            installUpstreamRulesForAnyVlan(fwd, output, outerPair);
        } else {
            Pair<Instruction, Instruction> innerPair = outerPair;
            outerPair = vlanOps.remove(0);
            installUpstreamRulesForVlans(fwd, output, innerPair, outerPair, noneValueVlanStatus);
        }
    }

    private void installUpstreamRulesForVlans(ForwardingObjective fwd, Instruction output,
                                              Pair<Instruction, Instruction> innerPair,
                                              Pair<Instruction, Instruction> outerPair, Boolean noneValueVlanStatus) {

        List<Instruction> setVlanPcps = findL2Instructions(L2ModificationInstruction.L2SubType.VLAN_PCP,
                fwd.treatment().allInstructions());

        Instruction innerPbitSet = null;
        Instruction outerPbitSet = null;

        if (setVlanPcps != null && !setVlanPcps.isEmpty()) {
            innerPbitSet = setVlanPcps.get(0);
            outerPbitSet = setVlanPcps.get(1);
        }

        TrafficTreatment innerTreatment;
        if (noneValueVlanStatus) {
            innerTreatment = buildTreatment(innerPair.getLeft(), innerPair.getRight(), fetchMeter(fwd),
                    fetchWriteMetadata(fwd), innerPbitSet,
                    Instructions.transition(QQ_TABLE));
        } else {
            innerTreatment = buildTreatment(innerPair.getRight(), fetchMeter(fwd), fetchWriteMetadata(fwd),
                    innerPbitSet, Instructions.transition(QQ_TABLE));
        }

        //match: in port, vlanId (0 or None)
        //action:
        //if vlanId None, push & set c-tag go to table 1
        //if vlanId 0 or any specific vlan, set c-tag, write metadata, meter and go to table 1
        FlowRule.Builder inner = DefaultFlowRule.builder()
                .fromApp(fwd.appId())
                .forDevice(deviceId)
                .makePermanent()
                .withPriority(fwd.priority())
                .withSelector(fwd.selector())
                .withTreatment(innerTreatment);

        PortCriterion inPort = (PortCriterion)
                fwd.selector().getCriterion(Criterion.Type.IN_PORT);

        VlanId cVlanId = ((L2ModificationInstruction.ModVlanIdInstruction)
                innerPair.getRight()).vlanId();

        //match: in port, c-tag
        //action: immediate: push s-tag, write metadata, meter and output
        FlowRule.Builder outer = DefaultFlowRule.builder()
                .fromApp(fwd.appId())
                .forDevice(deviceId)
                .forTable(QQ_TABLE)
                .makePermanent()
                .withPriority(fwd.priority())
                .withTreatment(buildTreatment(outerPair.getLeft(), outerPair.getRight(),
                        fetchMeter(fwd), writeMetadataIncludingOnlyTp(fwd),
                        outerPbitSet, output));

        if (innerPbitSet != null) {
            byte innerPbit = ((L2ModificationInstruction.ModVlanPcpInstruction)
                    innerPbitSet).vlanPcp();
            outer.withSelector(buildSelector(inPort, Criteria.matchVlanId(cVlanId), Criteria.matchVlanPcp(innerPbit)));
        } else {
            outer.withSelector(buildSelector(inPort, Criteria.matchVlanId(cVlanId)));
        }

        applyRules(fwd, inner, outer);
    }

    private void installUpstreamRulesForAnyVlan(ForwardingObjective fwd, Instruction output,
                                                Pair<Instruction, Instruction> outerPair) {

        log.debug("Installing upstream rules for any value vlan");

        //match: in port and any-vlan (coming from OLT app.)
        //action: write metadata, go to table 1 and meter
        FlowRule.Builder inner = DefaultFlowRule.builder()
                .fromApp(fwd.appId())
                .forDevice(deviceId)
                .makePermanent()
                .withPriority(fwd.priority())
                .withSelector(fwd.selector())
                .withTreatment(buildTreatment(Instructions.transition(QQ_TABLE), fetchMeter(fwd),
                        fetchWriteMetadata(fwd)));

        //match: in port and any-vlan (coming from OLT app.)
        //action: immediate: push:QinQ, vlanId (s-tag), write metadata, meter and output
        FlowRule.Builder outer = DefaultFlowRule.builder()
                .fromApp(fwd.appId())
                .forDevice(deviceId)
                .forTable(QQ_TABLE)
                .makePermanent()
                .withPriority(fwd.priority())
                .withSelector(fwd.selector())
                .withTreatment(buildTreatment(outerPair.getLeft(), outerPair.getRight(),
                        fetchMeter(fwd), writeMetadataIncludingOnlyTp(fwd), output));

        applyRules(fwd, inner, outer);
    }

    private boolean checkNoneVlanCriteria(ForwardingObjective fwd) {
        // Add the VLAN_PUSH treatment if we're matching on VlanId.NONE
        Criterion vlanMatchCriterion = filterForCriterion(fwd.selector().criteria(), Criterion.Type.VLAN_VID);
        boolean noneValueVlanStatus = false;
        if (vlanMatchCriterion != null) {
            noneValueVlanStatus = ((VlanIdCriterion) vlanMatchCriterion).vlanId().equals(VlanId.NONE);
        }
        return noneValueVlanStatus;
    }

    private boolean checkAnyVlanMatchCriteria(ForwardingObjective fwd) {
        Criterion anyValueVlanCriterion = fwd.selector().criteria().stream()
                .filter(c -> c.type().equals(Criterion.Type.VLAN_VID))
                .filter(vc -> ((VlanIdCriterion) vc).vlanId().toShort() == VlanId.ANY_VALUE)
                .findAny().orElse(null);

        if (anyValueVlanCriterion == null) {
            log.debug("Any value vlan match criteria is not found, criteria {}",
                      fwd.selector().criteria());
            return false;
        }

        return true;
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

    private Instruction fetchMeter(ForwardingObjective fwd) {
        Instruction meter = fwd.treatment().metered();

        if (meter == null) {
            log.debug("Meter instruction is not found for the forwarding objective {}", fwd);
            return null;
        }

        log.debug("Meter instruction is found.");
        return meter;
    }

    private Instructions.MetadataInstruction fetchWriteMetadata(ForwardingObjective fwd) {
        Instructions.MetadataInstruction writeMetadata = fwd.treatment().writeMetadata();

        if (writeMetadata == null) {
            log.warn("Write metadata is not found for the forwarding obj");
            fail(fwd, ObjectiveError.BADPARAMS);
            return null;
        }

        log.debug("Write metadata is found {}", writeMetadata);
        return writeMetadata;
    }

    private List<Pair<Instruction, Instruction>> vlanOps(ForwardingObjective fwd,
                                                         L2ModificationInstruction.L2SubType type) {

        List<Pair<Instruction, Instruction>> vlanOps = findVlanOps(
                fwd.treatment().allInstructions(), type);

        if (vlanOps == null || vlanOps.isEmpty()) {
            String direction = type == L2ModificationInstruction.L2SubType.VLAN_POP
                    ? DOWNSTREAM : UPSTREAM;
            log.error("Missing vlan operations in {} forwarding: {}", direction, fwd);
            fail(fwd, ObjectiveError.BADPARAMS);
            return ImmutableList.of();
        }
        return vlanOps;
    }


    private List<Pair<Instruction, Instruction>> findVlanOps(List<Instruction> instructions,
                                                             L2ModificationInstruction.L2SubType type) {

        List<Instruction> vlanOperations = findL2Instructions(
                type,
                instructions);
        List<Instruction> vlanSets = findL2Instructions(
                L2ModificationInstruction.L2SubType.VLAN_ID,
                instructions);

        if (vlanOperations.size() != vlanSets.size()) {
            return ImmutableList.of();
        }

        List<Pair<Instruction, Instruction>> pairs = Lists.newArrayList();

        for (int i = 0; i < vlanOperations.size(); i++) {
            pairs.add(new ImmutablePair<>(vlanOperations.get(i), vlanSets.get(i)));
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

    private void provisionEthTypeBasedFilter(FilteringObjective filter,
                                             EthTypeCriterion ethType,
                                             Instructions.OutputInstruction output) {

        Instruction meter = filter.meta().metered();
        Instruction writeMetadata = filter.meta().writeMetadata();

        Criterion vlanId = filterForCriterion(filter.conditions(), Criterion.Type.VLAN_VID);

        TrafficSelector selector = buildSelector(filter.key(), ethType, vlanId);
        TrafficTreatment treatment = buildTreatment(output, meter, writeMetadata);
        buildAndApplyRule(filter, selector, treatment);

    }

    private void provisionIgmp(FilteringObjective filter, EthTypeCriterion ethType,
                               IPProtocolCriterion ipProto,
                               Instructions.OutputInstruction output) {

        Instruction meter = filter.meta().metered();
        Instruction writeMetadata = filter.meta().writeMetadata();

        // cTag
        VlanIdCriterion vlanId = (VlanIdCriterion) filterForCriterion(filter.conditions(),
                Criterion.Type.VLAN_VID);
        Criterion cTagPriority = filterForCriterion(filter.conditions(), Criterion.Type.VLAN_PCP);

        TrafficSelector selector = buildSelector(filter.key(), ethType, ipProto, vlanId, cTagPriority);
        TrafficTreatment treatment = buildTreatment(output, meter, writeMetadata);
        buildAndApplyRule(filter, selector, treatment);
    }

    private void provisionDhcp(FilteringObjective filter, EthTypeCriterion ethType,
                               IPProtocolCriterion ipProto,
                               UdpPortCriterion udpSrcPort,
                               UdpPortCriterion udpDstPort,
                               Instructions.OutputInstruction output) {

        Instruction meter = filter.meta().metered();
        Instruction writeMetadata = filter.meta().writeMetadata();

        VlanIdCriterion vlanId = (VlanIdCriterion)
                filterForCriterion(filter.conditions(), Criterion.Type.VLAN_VID);

        TrafficSelector selector = buildSelector(filter.key(), ethType, ipProto, udpSrcPort, udpDstPort, vlanId);
        TrafficTreatment treatment = buildTreatment(output, meter, writeMetadata);
        buildAndApplyRule(filter, selector, treatment);
    }

    private void buildAndApplyRule(FilteringObjective filter, TrafficSelector selector,
                                   TrafficTreatment treatment) {
        FlowRule rule = DefaultFlowRule.builder()
                .fromApp(filter.appId())
                .forDevice(deviceId)
                .forTable(0)
                .makePermanent()
                .withSelector(selector)
                .withTreatment(treatment)
                .withPriority(filter.priority())
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

    private void applyRules(ForwardingObjective fwd, FlowRule.Builder... fwdBuilders) {
        FlowRuleOperations.Builder builder = FlowRuleOperations.builder();
        switch (fwd.op()) {
            case ADD:
                for (FlowRule.Builder fwdBuilder : fwdBuilders) {
                    builder.add(fwdBuilder.build());
                }
                break;
            case REMOVE:
                for (FlowRule.Builder fwdBuilder : fwdBuilders) {
                    builder.remove(fwdBuilder.build());
                }
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

        Arrays.stream(criteria).filter(Objects::nonNull).forEach(sBuilder::add);

        return sBuilder.build();
    }

    private TrafficTreatment buildTreatment(Instruction... instructions) {

        TrafficTreatment.Builder tBuilder = DefaultTrafficTreatment.builder();

        Arrays.stream(instructions).filter(Objects::nonNull).forEach(tBuilder::add);

        return tBuilder.build();
    }

    private Instruction writeMetadataIncludingOnlyTp(ForwardingObjective fwd) {

        return Instructions.writeMetadata(
                fetchWriteMetadata(fwd).metadata() & 0xFFFF00000000L, 0L);
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
            GroupKey key = event.subject().appCookie();
            NextObjective obj = pendingGroups.getIfPresent(key);
            if (obj == null) {
                log.debug("No pending group for {}, moving on", key);
                return;
            }
            log.trace("Event {} for group {}, handling pending" +
                              "NextGroup {}", event.type(), key, obj.id());
            if (event.type() == GroupEvent.Type.GROUP_ADDED ||
                    event.type() == GroupEvent.Type.GROUP_UPDATED) {
                    flowObjectiveStore.putNextGroup(obj.id(), new OLTPipelineGroup(key));
                    pass(obj);
                    pendingGroups.invalidate(key);
            } else if (event.type() == GroupEvent.Type.GROUP_REMOVED) {
                    flowObjectiveStore.removeNextGroup(obj.id());
                    pass(obj);
                    pendingGroups.invalidate(key);
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

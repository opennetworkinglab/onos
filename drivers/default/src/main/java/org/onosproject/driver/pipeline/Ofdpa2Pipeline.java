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

import static org.slf4j.LoggerFactory.getLogger;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import org.onlab.osgi.ServiceDirectory;
import org.onlab.packet.Ethernet;
import org.onlab.packet.IpPrefix;
import org.onlab.packet.MacAddress;
import org.onlab.packet.VlanId;
import org.onlab.util.KryoNamespace;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.driver.extensions.OfdpaMatchVlanVid;
import org.onosproject.driver.extensions.OfdpaSetVlanVid;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Port;
import org.onosproject.net.PortNumber;
import org.onosproject.net.behaviour.NextGroup;
import org.onosproject.net.behaviour.Pipeliner;
import org.onosproject.net.behaviour.PipelinerContext;
import org.onosproject.net.device.DeviceService;
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
import org.onosproject.net.flow.criteria.EthCriterion;
import org.onosproject.net.flow.criteria.EthTypeCriterion;
import org.onosproject.net.flow.criteria.ExtensionCriterion;
import org.onosproject.net.flow.criteria.IPCriterion;
import org.onosproject.net.flow.criteria.MplsBosCriterion;
import org.onosproject.net.flow.criteria.MplsCriterion;
import org.onosproject.net.flow.criteria.PortCriterion;
import org.onosproject.net.flow.criteria.VlanIdCriterion;
import org.onosproject.net.flow.instructions.Instruction;
import org.onosproject.net.flow.instructions.Instructions.OutputInstruction;
import org.onosproject.net.flow.instructions.L2ModificationInstruction;
import org.onosproject.net.flow.instructions.L2ModificationInstruction.L2SubType;
import org.onosproject.net.flow.instructions.L2ModificationInstruction.ModVlanIdInstruction;
import org.onosproject.net.flow.instructions.L3ModificationInstruction;
import org.onosproject.net.flow.instructions.L3ModificationInstruction.L3SubType;
import org.onosproject.net.flowobjective.FilteringObjective;
import org.onosproject.net.flowobjective.FlowObjectiveStore;
import org.onosproject.net.flowobjective.ForwardingObjective;
import org.onosproject.net.flowobjective.NextObjective;
import org.onosproject.net.flowobjective.Objective;
import org.onosproject.net.flowobjective.ObjectiveError;
import org.onosproject.net.group.DefaultGroupKey;
import org.onosproject.net.group.Group;
import org.onosproject.net.group.GroupKey;
import org.onosproject.net.group.GroupService;
import org.onosproject.store.serializers.KryoNamespaces;
import org.slf4j.Logger;

/**
 * Driver for Broadcom's OF-DPA v2.0 TTP.
 *
 */
public class Ofdpa2Pipeline extends AbstractHandlerBehaviour implements Pipeliner {
    protected static final int PORT_TABLE = 0;
    protected static final int VLAN_TABLE = 10;
    protected static final int TMAC_TABLE = 20;
    protected static final int UNICAST_ROUTING_TABLE = 30;
    protected static final int MULTICAST_ROUTING_TABLE = 40;
    protected static final int MPLS_TABLE_0 = 23;
    protected static final int MPLS_TABLE_1 = 24;
    protected static final int BRIDGING_TABLE = 50;
    protected static final int ACL_TABLE = 60;
    protected static final int MAC_LEARNING_TABLE = 254;
    protected static final long OFPP_MAX = 0xffffff00L;

    protected static final int HIGHEST_PRIORITY = 0xffff;
    protected static final int DEFAULT_PRIORITY = 0x8000;
    protected static final int LOWEST_PRIORITY = 0x0;

    private final Logger log = getLogger(getClass());
    protected ServiceDirectory serviceDirectory;
    protected FlowRuleService flowRuleService;
    protected CoreService coreService;
    protected GroupService groupService;
    protected FlowObjectiveStore flowObjectiveStore;
    protected DeviceId deviceId;
    protected ApplicationId driverId;
    protected DeviceService deviceService;
    protected static KryoNamespace appKryo = new KryoNamespace.Builder()
        .register(KryoNamespaces.API)
        .register(GroupKey.class)
        .register(DefaultGroupKey.class)
        .register(Ofdpa2GroupHandler.OfdpaNextGroup.class)
        .register(ArrayDeque.class)
        .build("Ofdpa2Pipeline");

    protected Ofdpa2GroupHandler groupHandler;

    protected Set<IPCriterion> sentIpFilters = Collections.newSetFromMap(
                                               new ConcurrentHashMap<>());

    @Override
    public void init(DeviceId deviceId, PipelinerContext context) {
        this.deviceId = deviceId;

        // Initialize OFDPA group handler
        groupHandler = new Ofdpa2GroupHandler();
        groupHandler.init(deviceId, context);

        serviceDirectory = context.directory();
        coreService = serviceDirectory.get(CoreService.class);
        flowRuleService = serviceDirectory.get(FlowRuleService.class);
        groupService = serviceDirectory.get(GroupService.class);
        flowObjectiveStore = context.store();
        deviceService = serviceDirectory.get(DeviceService.class);

        driverId = coreService.registerApplication(
                "org.onosproject.driver.Ofdpa2Pipeline");

        initializePipeline();
    }

    protected void initializePipeline() {
        // OF-DPA does not require initializing the pipeline as it puts default
        // rules automatically in the hardware. However emulation of OFDPA in
        // software switches does require table-miss-entries.
    }

    //////////////////////////////////////
    //  Flow Objectives
    //////////////////////////////////////

    @Override
    public void filter(FilteringObjective filteringObjective) {
        if (filteringObjective.type() == FilteringObjective.Type.PERMIT) {
            processFilter(filteringObjective,
                          filteringObjective.op() == Objective.Operation.ADD,
                          filteringObjective.appId());
        } else {
            // Note that packets that don't match the PERMIT filter are
            // automatically denied. The DENY filter is used to deny packets
            // that are otherwise permitted by the PERMIT filter.
            // Use ACL table flow rules here for DENY filtering objectives
            log.debug("filter objective other than PERMIT currently not supported");
            fail(filteringObjective, ObjectiveError.UNSUPPORTED);
        }
    }

    @Override
    public void forward(ForwardingObjective fwd) {
        Collection<FlowRule> rules;
        FlowRuleOperations.Builder flowOpsBuilder = FlowRuleOperations.builder();

        rules = processForward(fwd);
        if (rules == null || rules.isEmpty()) {
            // Assumes fail message has already been generated to the objective
            // context. Returning here prevents spurious pass message to be
            // generated by FlowRule service for empty flowOps.
            return;
        }
        switch (fwd.op()) {
            case ADD:
                rules.stream()
                        .filter(Objects::nonNull)
                        .forEach(flowOpsBuilder::add);
                break;
            case REMOVE:
                rules.stream()
                        .filter(Objects::nonNull)
                        .forEach(flowOpsBuilder::remove);
                break;
            default:
                fail(fwd, ObjectiveError.UNKNOWN);
                log.warn("Unknown forwarding type {}", fwd.op());
        }

        flowRuleService.apply(flowOpsBuilder.build(new FlowRuleOperationsContext() {
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
        NextGroup nextGroup = flowObjectiveStore.getNextGroup(nextObjective.id());
        switch (nextObjective.op()) {
        case ADD:
            if (nextGroup != null) {
                log.warn("Cannot add next {} that already exists in device {}",
                         nextObjective.id(), deviceId);
                return;
            }
            log.debug("Processing NextObjective id{} in dev{} - add group",
                      nextObjective.id(), deviceId);
            groupHandler.addGroup(nextObjective);
            break;
        case ADD_TO_EXISTING:
            if (nextGroup != null) {
                log.debug("Processing NextObjective id{} in dev{} - add bucket",
                          nextObjective.id(), deviceId);
                groupHandler.addBucketToGroup(nextObjective, nextGroup);
            } else {
                // it is possible that group-chain has not been fully created yet
                log.debug("Waiting to add bucket to group for next-id:{} in dev:{}",
                          nextObjective.id(), deviceId);
                // by design only one pending bucket is allowed for the group
                groupHandler.pendingBuckets.put(nextObjective.id(), nextObjective);
            }
            break;
        case REMOVE:
            if (nextGroup == null) {
                log.warn("Cannot remove next {} that does not exist in device {}",
                         nextObjective.id(), deviceId);
                return;
            }
            log.debug("Processing NextObjective id{}  in dev{} - remove group",
                      nextObjective.id(), deviceId);
            groupHandler.removeGroup(nextObjective, nextGroup);
            break;
        case REMOVE_FROM_EXISTING:
            if (nextGroup == null) {
                log.warn("Cannot remove from next {} that does not exist in device {}",
                         nextObjective.id(), deviceId);
                return;
            }
            log.debug("Processing NextObjective id{} in dev{} - remove bucket",
                      nextObjective.id(), deviceId);
            groupHandler.removeBucketFromGroup(nextObjective, nextGroup);
            break;
        default:
            log.warn("Unsupported operation {}", nextObjective.op());
        }
    }

    //////////////////////////////////////
    //  Flow handling
    //////////////////////////////////////

    /**
     * As per OFDPA 2.0 TTP, filtering of VLAN ids, MAC addresses (for routing)
     * and IP addresses configured on switch ports happen in different tables.
     * Note that IP filtering rules need to be added to the ACL table, as there
     * is no mechanism to send to controller via IP table.
     *
     * @param filt      the filtering objective
     * @param install   indicates whether to add or remove the objective
     * @param applicationId     the application that sent this objective
     */
    protected void processFilter(FilteringObjective filt,
                                 boolean install, ApplicationId applicationId) {
        // This driver only processes filtering criteria defined with switch
        // ports as the key
        PortCriterion portCriterion = null;
        EthCriterion ethCriterion = null;
        VlanIdCriterion vidCriterion = null;
        Collection<IPCriterion> ips = new ArrayList<IPCriterion>();
        if (!filt.key().equals(Criteria.dummy()) &&
                filt.key().type() == Criterion.Type.IN_PORT) {
            portCriterion = (PortCriterion) filt.key();
        } else {
            log.warn("No key defined in filtering objective from app: {}. Not"
                    + "processing filtering objective", applicationId);
            fail(filt, ObjectiveError.BADPARAMS);
            return;
        }
        log.debug("Received filtering objective for dev/port: {}/{}", deviceId,
                 portCriterion.port());
        // convert filtering conditions for switch-intfs into flowrules
        FlowRuleOperations.Builder ops = FlowRuleOperations.builder();
        for (Criterion criterion : filt.conditions()) {
            if (criterion.type() == Criterion.Type.ETH_DST ||
                    criterion.type() == Criterion.Type.ETH_DST_MASKED) {
                ethCriterion = (EthCriterion) criterion;
            } else if (criterion.type() == Criterion.Type.VLAN_VID) {
                vidCriterion = (VlanIdCriterion) criterion;
            } else if (criterion.type() == Criterion.Type.IPV4_DST) {
                ips.add((IPCriterion) criterion);
            } else {
                log.error("Unsupported filter {}", criterion);
                fail(filt, ObjectiveError.UNSUPPORTED);
                return;
            }
        }

        VlanId assignedVlan = null;
        if (vidCriterion != null) {
            // Use the VLAN in metadata whenever a metadata is provided
            if (filt.meta() != null) {
                assignedVlan = readVlanFromTreatment(filt.meta());
            // Use the VLAN in criterion if metadata is not present and the traffic is tagged
            } else if (!vidCriterion.vlanId().equals(VlanId.NONE)) {
                assignedVlan = vidCriterion.vlanId();
            }

            if (assignedVlan == null) {
                log.error("Driver fails to extract VLAN information. "
                        + "Not proccessing VLAN filters on device {}.", deviceId);
                log.debug("VLAN ID in criterion={}, metadata={}",
                        readVlanFromTreatment(filt.meta()), vidCriterion.vlanId());
                fail(filt, ObjectiveError.BADPARAMS);
                return;
            }
        }

        if (ethCriterion == null || ethCriterion.mac().equals(MacAddress.NONE)) {
            // NOTE: it is possible that a filtering objective only has vidCriterion
            log.debug("filtering objective missing dstMac, cannot program TMAC table");
        } else {
            for (FlowRule tmacRule : processEthDstFilter(portCriterion, ethCriterion,
                                                         vidCriterion, assignedVlan,
                                                         applicationId)) {
                log.debug("adding MAC filtering rules in TMAC table: {} for dev: {}",
                          tmacRule, deviceId);
                ops = install ? ops.add(tmacRule) : ops.remove(tmacRule);
            }
        }

        if (vidCriterion == null) {
            // NOTE: it is possible that a filtering objective only has ethCriterion
            log.debug("filtering objective missing dstMac or VLAN, "
                    + "cannot program VLAN Table");
        } else {
            /*
             * NOTE: Separate vlan filtering rules and assignment rules
             * into different stage in order to guarantee that filtering rules
             * always go first, as required by ofdpa.
             */
            List<FlowRule> allRules = processVlanIdFilter(
                    portCriterion, vidCriterion, assignedVlan, applicationId);
            List<FlowRule> filteringRules = new ArrayList<>();
            List<FlowRule> assignmentRules = new ArrayList<>();

            allRules.forEach(flowRule -> {
                ExtensionCriterion extCriterion =
                        (ExtensionCriterion) flowRule.selector().getCriterion(Criterion.Type.EXTENSION);
                VlanId vlanId = ((OfdpaMatchVlanVid) extCriterion.extensionSelector()).vlanId();
                if (!vlanId.equals(VlanId.NONE)) {
                    filteringRules.add(flowRule);
                } else {
                    assignmentRules.add(flowRule);
                }
            });

            for (FlowRule filteringRule : filteringRules) {
                log.debug("adding VLAN filtering rule in VLAN table: {} for dev: {}",
                        filteringRule, deviceId);
                ops = install ? ops.add(filteringRule) : ops.remove(filteringRule);
            }

            ops.newStage();

            for (FlowRule assignmentRule : assignmentRules) {
                log.debug("adding VLAN assignment rule in VLAN table: {} for dev: {}",
                        assignmentRule, deviceId);
                ops = install ? ops.add(assignmentRule) : ops.remove(assignmentRule);
            }
        }

        for (IPCriterion ipaddr : ips) {
            // since we ignore port information for IP rules, and the same (gateway) IP
            // can be configured on multiple ports, we make sure that we send
            // only a single rule to the switch.
            if (!sentIpFilters.contains(ipaddr)) {
                sentIpFilters.add(ipaddr);
                log.debug("adding IP filtering rules in ACL table {} for dev: {}",
                          ipaddr, deviceId);
                TrafficSelector.Builder selector = DefaultTrafficSelector.builder();
                TrafficTreatment.Builder treatment = DefaultTrafficTreatment.builder();
                selector.matchEthType(Ethernet.TYPE_IPV4);
                selector.matchIPDst(ipaddr.ip());
                treatment.setOutput(PortNumber.CONTROLLER);
                FlowRule rule = DefaultFlowRule.builder()
                        .forDevice(deviceId)
                        .withSelector(selector.build())
                        .withTreatment(treatment.build())
                        .withPriority(HIGHEST_PRIORITY)
                        .fromApp(applicationId)
                        .makePermanent()
                        .forTable(ACL_TABLE).build();
                ops = install ? ops.add(rule) : ops.remove(rule);
            }
        }

        // apply filtering flow rules
        flowRuleService.apply(ops.build(new FlowRuleOperationsContext() {
            @Override
            public void onSuccess(FlowRuleOperations ops) {
                log.info("Applied {} filtering rules in device {}",
                         ops.stages().get(0).size(), deviceId);
                pass(filt);
            }

            @Override
            public void onError(FlowRuleOperations ops) {
                log.info("Failed to apply all filtering rules in dev {}", deviceId);
                fail(filt, ObjectiveError.FLOWINSTALLATIONFAILED);
            }
        }));

    }

    /**
     * Allows untagged packets into pipeline by assigning a vlan id.
     * Vlan assignment is done by the application.
     * Allows tagged packets into pipeline as per configured port-vlan info.
     *
     * @param portCriterion   port on device for which this filter is programmed
     * @param vidCriterion   vlan assigned to port, or NONE for untagged
     * @param assignedVlan   assigned vlan-id for untagged packets
     * @param applicationId  for application programming this filter
     * @return list of FlowRule for port-vlan filters
     */
    protected List<FlowRule> processVlanIdFilter(PortCriterion portCriterion,
                                                 VlanIdCriterion vidCriterion,
                                                 VlanId assignedVlan,
                                                 ApplicationId applicationId) {
        List<FlowRule> rules = new ArrayList<>();
        TrafficSelector.Builder selector = DefaultTrafficSelector.builder();
        TrafficTreatment.Builder treatment = DefaultTrafficTreatment.builder();
        TrafficSelector.Builder preSelector = null;
        TrafficTreatment.Builder preTreatment = null;

        treatment.transition(TMAC_TABLE);

        if (vidCriterion.vlanId() == VlanId.NONE) {
            // untagged packets are assigned vlans
            OfdpaMatchVlanVid ofdpaMatchVlanVid = new OfdpaMatchVlanVid(VlanId.NONE);
            selector.extension(ofdpaMatchVlanVid, deviceId);
            OfdpaSetVlanVid ofdpaSetVlanVid = new OfdpaSetVlanVid(assignedVlan);
            treatment.extension(ofdpaSetVlanVid, deviceId);

            preSelector = DefaultTrafficSelector.builder();
            OfdpaMatchVlanVid preOfdpaMatchVlanVid = new OfdpaMatchVlanVid(assignedVlan);
            preSelector.extension(preOfdpaMatchVlanVid, deviceId);
            preTreatment = DefaultTrafficTreatment.builder().transition(TMAC_TABLE);

        } else {
            OfdpaMatchVlanVid ofdpaMatchVlanVid = new OfdpaMatchVlanVid(vidCriterion.vlanId());
            selector.extension(ofdpaMatchVlanVid, deviceId);

            if (!assignedVlan.equals(vidCriterion.vlanId())) {
                OfdpaSetVlanVid ofdpaSetVlanVid = new OfdpaSetVlanVid(assignedVlan);
                treatment.extension(ofdpaSetVlanVid, deviceId);
            }
        }

        // ofdpa cannot match on ALL portnumber, so we need to use separate
        // rules for each port.
        List<PortNumber> portnums = new ArrayList<>();
        if (portCriterion.port() == PortNumber.ALL) {
            for (Port port : deviceService.getPorts(deviceId)) {
                if (port.number().toLong() > 0 && port.number().toLong() < OFPP_MAX) {
                    portnums.add(port.number());
                }
            }
        } else {
            portnums.add(portCriterion.port());
        }

        for (PortNumber pnum : portnums) {
            // create rest of flowrule
            selector.matchInPort(pnum);
            FlowRule rule = DefaultFlowRule.builder()
                    .forDevice(deviceId)
                    .withSelector(selector.build())
                    .withTreatment(treatment.build())
                    .withPriority(DEFAULT_PRIORITY)
                    .fromApp(applicationId)
                    .makePermanent()
                    .forTable(VLAN_TABLE).build();

            if (preSelector != null) {
                preSelector.matchInPort(pnum);
                FlowRule preRule = DefaultFlowRule.builder()
                        .forDevice(deviceId)
                        .withSelector(preSelector.build())
                        .withTreatment(preTreatment.build())
                        .withPriority(DEFAULT_PRIORITY)
                        .fromApp(applicationId)
                        .makePermanent()
                        .forTable(VLAN_TABLE).build();
                rules.add(preRule);
            }

            rules.add(rule);
        }
        return rules;
    }

    /**
     * Allows routed packets with correct destination MAC to be directed
     * to unicast-IP routing table or MPLS forwarding table.
     *
     * @param portCriterion  port on device for which this filter is programmed
     * @param ethCriterion   dstMac of device for which is filter is programmed
     * @param vidCriterion   vlan assigned to port, or NONE for untagged
     * @param assignedVlan   assigned vlan-id for untagged packets
     * @param applicationId  for application programming this filter
     * @return list of FlowRule for port-vlan filters

     */
    protected List<FlowRule> processEthDstFilter(PortCriterion portCriterion,
                                                 EthCriterion ethCriterion,
                                                 VlanIdCriterion vidCriterion,
                                                 VlanId assignedVlan,
                                                 ApplicationId applicationId) {
        // Consider PortNumber.ANY as wildcard. Match ETH_DST only
        if (portCriterion != null && portCriterion.port() == PortNumber.ANY) {
            return processEthDstOnlyFilter(ethCriterion, applicationId);
        }

        // Multicast MAC
        if (ethCriterion.mask() != null) {
            return processMcastEthDstFilter(ethCriterion, applicationId);
        }

        //handling untagged packets via assigned VLAN
        if (vidCriterion.vlanId() == VlanId.NONE) {
            vidCriterion = (VlanIdCriterion) Criteria.matchVlanId(assignedVlan);
        }
        // ofdpa cannot match on ALL portnumber, so we need to use separate
        // rules for each port.
        List<PortNumber> portnums = new ArrayList<PortNumber>();
        if (portCriterion.port() == PortNumber.ALL) {
            for (Port port : deviceService.getPorts(deviceId)) {
                if (port.number().toLong() > 0 && port.number().toLong() < OFPP_MAX) {
                    portnums.add(port.number());
                }
            }
        } else {
            portnums.add(portCriterion.port());
        }

        List<FlowRule> rules = new ArrayList<FlowRule>();
        for (PortNumber pnum : portnums) {
            OfdpaMatchVlanVid ofdpaMatchVlanVid = new OfdpaMatchVlanVid(vidCriterion.vlanId());
            // for unicast IP packets
            TrafficSelector.Builder selector = DefaultTrafficSelector.builder();
            TrafficTreatment.Builder treatment = DefaultTrafficTreatment.builder();
            selector.matchInPort(pnum);
            selector.extension(ofdpaMatchVlanVid, deviceId);
            selector.matchEthType(Ethernet.TYPE_IPV4);
            selector.matchEthDst(ethCriterion.mac());
            treatment.transition(UNICAST_ROUTING_TABLE);
            FlowRule rule = DefaultFlowRule.builder()
                    .forDevice(deviceId)
                    .withSelector(selector.build())
                    .withTreatment(treatment.build())
                    .withPriority(DEFAULT_PRIORITY)
                    .fromApp(applicationId)
                    .makePermanent()
                    .forTable(TMAC_TABLE).build();
            rules.add(rule);
            //for MPLS packets
            selector = DefaultTrafficSelector.builder();
            treatment = DefaultTrafficTreatment.builder();
            selector.matchInPort(pnum);
            selector.extension(ofdpaMatchVlanVid, deviceId);
            selector.matchEthType(Ethernet.MPLS_UNICAST);
            selector.matchEthDst(ethCriterion.mac());
            treatment.transition(MPLS_TABLE_0);
            rule = DefaultFlowRule.builder()
                    .forDevice(deviceId)
                    .withSelector(selector.build())
                    .withTreatment(treatment.build())
                    .withPriority(DEFAULT_PRIORITY)
                    .fromApp(applicationId)
                    .makePermanent()
                    .forTable(TMAC_TABLE).build();
            rules.add(rule);
        }
        return rules;
    }

    protected List<FlowRule> processEthDstOnlyFilter(EthCriterion ethCriterion,
            ApplicationId applicationId) {
        TrafficSelector.Builder selector = DefaultTrafficSelector.builder();
        TrafficTreatment.Builder treatment = DefaultTrafficTreatment.builder();
        selector.matchEthType(Ethernet.TYPE_IPV4);
        selector.matchEthDst(ethCriterion.mac());
        treatment.transition(UNICAST_ROUTING_TABLE);
        FlowRule rule = DefaultFlowRule.builder()
                .forDevice(deviceId)
                .withSelector(selector.build())
                .withTreatment(treatment.build())
                .withPriority(DEFAULT_PRIORITY)
                .fromApp(applicationId)
                .makePermanent()
                .forTable(TMAC_TABLE).build();
        return ImmutableList.<FlowRule>builder().add(rule).build();
    }

    protected List<FlowRule> processMcastEthDstFilter(EthCriterion ethCriterion,
            ApplicationId applicationId) {
        TrafficSelector.Builder selector = DefaultTrafficSelector.builder();
        TrafficTreatment.Builder treatment = DefaultTrafficTreatment.builder();
        selector.matchEthType(Ethernet.TYPE_IPV4);
        selector.matchEthDstMasked(ethCriterion.mac(), ethCriterion.mask());
        treatment.transition(MULTICAST_ROUTING_TABLE);
        FlowRule rule = DefaultFlowRule.builder()
                .forDevice(deviceId)
                .withSelector(selector.build())
                .withTreatment(treatment.build())
                .withPriority(DEFAULT_PRIORITY)
                .fromApp(applicationId)
                .makePermanent()
                .forTable(TMAC_TABLE).build();
        return ImmutableList.<FlowRule>builder().add(rule).build();
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

    /**
     * In the OF-DPA 2.0 pipeline, versatile forwarding objectives go to the
     * ACL table.
     * @param fwd  the forwarding objective of type 'versatile'
     * @return     a collection of flow rules to be sent to the switch. An empty
     *             collection may be returned if there is a problem in processing
     *             the flow rule
     */
    protected Collection<FlowRule> processVersatile(ForwardingObjective fwd) {
        log.info("Processing versatile forwarding objective");

        EthTypeCriterion ethType =
                (EthTypeCriterion) fwd.selector().getCriterion(Criterion.Type.ETH_TYPE);
        if (ethType == null) {
            log.error("Versatile forwarding objective must include ethType");
            fail(fwd, ObjectiveError.BADPARAMS);
            return Collections.emptySet();
        }
        if (fwd.nextId() == null && fwd.treatment() == null) {
            log.error("Forwarding objective {} from {} must contain "
                    + "nextId or Treatment", fwd.selector(), fwd.appId());
            fail(fwd, ObjectiveError.BADPARAMS);
            return Collections.emptySet();
        }

        TrafficSelector.Builder sbuilder = DefaultTrafficSelector.builder();
        fwd.selector().criteria().forEach(criterion -> {
            if (criterion instanceof VlanIdCriterion) {
                VlanId vlanId = ((VlanIdCriterion) criterion).vlanId();
                // ensure that match does not include vlan = NONE as OF-DPA does not
                // match untagged packets this way in the ACL table.
                if (vlanId.equals(VlanId.NONE)) {
                    return;
                }
                OfdpaMatchVlanVid ofdpaMatchVlanVid =
                        new OfdpaMatchVlanVid(vlanId);
                sbuilder.extension(ofdpaMatchVlanVid, deviceId);
            } else {
                sbuilder.add(criterion);
            }
        });

        // XXX driver does not currently do type checking as per Tables 65-67 in
        // OFDPA 2.0 spec. The only allowed treatment is a punt to the controller.
        TrafficTreatment.Builder ttBuilder = DefaultTrafficTreatment.builder();
        if (fwd.treatment() != null) {
            for (Instruction ins : fwd.treatment().allInstructions()) {
                if (ins instanceof OutputInstruction) {
                    OutputInstruction o = (OutputInstruction) ins;
                    if (o.port() == PortNumber.CONTROLLER) {
                        ttBuilder.add(o);
                    } else {
                        log.warn("Only allowed treatments in versatile forwarding "
                                + "objectives are punts to the controller");
                    }
                } else {
                    log.warn("Cannot process instruction in versatile fwd {}", ins);
                }
            }
        }
        if (fwd.nextId() != null) {
            // overide case
            NextGroup next = getGroupForNextObjective(fwd.nextId());
            List<Deque<GroupKey>> gkeys = appKryo.deserialize(next.data());
            // we only need the top level group's key to point the flow to it
            Group group = groupService.getGroup(deviceId, gkeys.get(0).peekFirst());
            if (group == null) {
                log.warn("Group with key:{} for next-id:{} not found in dev:{}",
                         gkeys.get(0).peekFirst(), fwd.nextId(), deviceId);
                fail(fwd, ObjectiveError.GROUPMISSING);
                return Collections.emptySet();
            }
            ttBuilder.deferred().group(group.id());
        }

        FlowRule.Builder ruleBuilder = DefaultFlowRule.builder()
                .fromApp(fwd.appId())
                .withPriority(fwd.priority())
                .forDevice(deviceId)
                .withSelector(sbuilder.build())
                .withTreatment(ttBuilder.build())
                .makePermanent()
                .forTable(ACL_TABLE);
        return Collections.singletonList(ruleBuilder.build());
    }

    /**
     * In the OF-DPA 2.0 pipeline, specific forwarding refers to the IP table
     * (unicast or multicast) or the L2 table (mac + vlan) or the MPLS table.
     *
     * @param fwd the forwarding objective of type 'specific'
     * @return    a collection of flow rules. Typically there will be only one
     *            for this type of forwarding objective. An empty set may be
     *            returned if there is an issue in processing the objective.
     */
    protected Collection<FlowRule> processSpecific(ForwardingObjective fwd) {
        log.debug("Processing specific fwd objective:{} in dev:{} with next:{}",
                  fwd.id(), deviceId, fwd.nextId());
        boolean isEthTypeObj = isSupportedEthTypeObjective(fwd);
        boolean isEthDstObj = isSupportedEthDstObjective(fwd);

        if (isEthTypeObj) {
            return processEthTypeSpecific(fwd);
        } else if (isEthDstObj) {
            return processEthDstSpecific(fwd);
        } else {
            log.warn("processSpecific: Unsupported forwarding objective "
                    + "criteria fwd:{} in dev:{}", fwd.nextId(), deviceId);
            fail(fwd, ObjectiveError.UNSUPPORTED);
            return Collections.emptySet();
        }
    }

    private boolean isSupportedEthTypeObjective(ForwardingObjective fwd) {
        TrafficSelector selector = fwd.selector();
        EthTypeCriterion ethType = (EthTypeCriterion) selector
                .getCriterion(Criterion.Type.ETH_TYPE);
        return !((ethType == null) ||
                ((ethType.ethType().toShort() != Ethernet.TYPE_IPV4) &&
                        (ethType.ethType().toShort() != Ethernet.MPLS_UNICAST)));
    }

    private boolean isSupportedEthDstObjective(ForwardingObjective fwd) {
        TrafficSelector selector = fwd.selector();
        EthCriterion ethDst = (EthCriterion) selector
                .getCriterion(Criterion.Type.ETH_DST);
        VlanIdCriterion vlanId = (VlanIdCriterion) selector
                .getCriterion(Criterion.Type.VLAN_VID);
        return !(ethDst == null && vlanId == null);
    }

    /**
     * Handles forwarding rules to the IP and MPLS tables.
     *
     * @param fwd the forwarding objective
     * @return A collection of flow rules, or an empty set
     */
    protected Collection<FlowRule> processEthTypeSpecific(ForwardingObjective fwd) {
        TrafficSelector selector = fwd.selector();
        EthTypeCriterion ethType =
                (EthTypeCriterion) selector.getCriterion(Criterion.Type.ETH_TYPE);
        boolean defaultRule = false;
        boolean popMpls = false;
        int forTableId;
        TrafficSelector.Builder filteredSelector = DefaultTrafficSelector.builder();
        TrafficTreatment.Builder tb = DefaultTrafficTreatment.builder();
        TrafficSelector.Builder complementarySelector = DefaultTrafficSelector.builder();

        /*
         * NOTE: The switch does not support matching 0.0.0.0/0.
         * Split it into 0.0.0.0/1 and 128.0.0.0/1
         */
        if (ethType.ethType().toShort() == Ethernet.TYPE_IPV4) {
            IpPrefix ipv4Dst = ((IPCriterion) selector.getCriterion(Criterion.Type.IPV4_DST)).ip();
            if (ipv4Dst.isMulticast()) {
                if (ipv4Dst.prefixLength() != 32) {
                    log.warn("Multicast specific forwarding objective can only be /32");
                    fail(fwd, ObjectiveError.BADPARAMS);
                    return ImmutableSet.of();
                }
                VlanId assignedVlan = readVlanFromSelector(fwd.meta());
                if (assignedVlan == null) {
                    log.warn("VLAN ID required by multicast specific fwd obj is missing. Abort.");
                    fail(fwd, ObjectiveError.BADPARAMS);
                    return ImmutableSet.of();
                }
                OfdpaMatchVlanVid ofdpaMatchVlanVid = new OfdpaMatchVlanVid(assignedVlan);
                filteredSelector.extension(ofdpaMatchVlanVid, deviceId);
                filteredSelector.matchEthType(Ethernet.TYPE_IPV4).matchIPDst(ipv4Dst);
                forTableId = MULTICAST_ROUTING_TABLE;
                log.debug("processing IPv4 multicast specific forwarding objective {} -> next:{}"
                        + " in dev:{}", fwd.id(), fwd.nextId(), deviceId);
            } else {
                if (ipv4Dst.prefixLength() > 0) {
                    filteredSelector.matchEthType(Ethernet.TYPE_IPV4).matchIPDst(ipv4Dst);
                } else {
                    filteredSelector.matchEthType(Ethernet.TYPE_IPV4)
                            .matchIPDst(IpPrefix.valueOf("0.0.0.0/1"));
                    complementarySelector.matchEthType(Ethernet.TYPE_IPV4)
                            .matchIPDst(IpPrefix.valueOf("128.0.0.0/1"));
                    defaultRule = true;
                }
                forTableId = UNICAST_ROUTING_TABLE;
                log.debug("processing IPv4 unicast specific forwarding objective {} -> next:{}"
                        + " in dev:{}", fwd.id(), fwd.nextId(), deviceId);
            }

            if (fwd.treatment() != null) {
                for (Instruction instr : fwd.treatment().allInstructions()) {
                    if (instr instanceof L3ModificationInstruction &&
                            ((L3ModificationInstruction) instr).subtype() == L3SubType.DEC_TTL) {
                        tb.deferred().add(instr);
                    }
                }
            }

        } else {
            filteredSelector
                .matchEthType(Ethernet.MPLS_UNICAST)
                .matchMplsLabel(((MplsCriterion)
                        selector.getCriterion(Criterion.Type.MPLS_LABEL)).label());
            MplsBosCriterion bos = (MplsBosCriterion) selector
                                        .getCriterion(Criterion.Type.MPLS_BOS);
            if (bos != null) {
                filteredSelector.matchMplsBos(bos.mplsBos());
            }
            forTableId = MPLS_TABLE_1;
            log.debug("processing MPLS specific forwarding objective {} -> next:{}"
                    + " in dev {}", fwd.id(), fwd.nextId(), deviceId);

            if (fwd.treatment() != null) {
                for (Instruction instr : fwd.treatment().allInstructions()) {
                    if (instr instanceof L2ModificationInstruction &&
                            ((L2ModificationInstruction) instr).subtype() == L2SubType.MPLS_POP) {
                        popMpls = true;
                        tb.immediate().add(instr);
                    }
                    if (instr instanceof L3ModificationInstruction &&
                            ((L3ModificationInstruction) instr).subtype() == L3SubType.DEC_TTL) {
                        // FIXME Should modify the app to send the correct DEC_MPLS_TTL instruction
                        tb.immediate().decMplsTtl();
                    }
                    if (instr instanceof L3ModificationInstruction &&
                            ((L3ModificationInstruction) instr).subtype() == L3SubType.TTL_IN) {
                        tb.immediate().add(instr);
                    }
                }
            }
        }

        if (fwd.nextId() != null) {
            if (forTableId == MPLS_TABLE_1 && !popMpls) {
                log.warn("SR CONTINUE case cannot be handled as MPLS ECMP "
                        + "is not implemented in OF-DPA yet. Aborting this flow {} -> next:{}"
                        + "in this device {}", fwd.id(), fwd.nextId(), deviceId);
                // XXX We could convert to forwarding to a single-port, via a
                // MPLS interface, or a MPLS SWAP (with-same) but that would
                // have to be handled in the next-objective. Also the pop-mpls
                // logic used here won't work in non-BoS case.
                fail(fwd, ObjectiveError.FLOWINSTALLATIONFAILED);
                return Collections.emptySet();
            }

            NextGroup next = getGroupForNextObjective(fwd.nextId());
            if (next != null) {
                List<Deque<GroupKey>> gkeys = appKryo.deserialize(next.data());
                // we only need the top level group's key to point the flow to it
                Group group = groupService.getGroup(deviceId, gkeys.get(0).peekFirst());
                if (group == null) {
                    log.warn("Group with key:{} for next-id:{} not found in dev:{}",
                             gkeys.get(0).peekFirst(), fwd.nextId(), deviceId);
                    fail(fwd, ObjectiveError.GROUPMISSING);
                    return Collections.emptySet();
                }
                tb.deferred().group(group.id());
            } else {
                log.warn("Cannot find group for nextId:{} in dev:{}. Aborting fwd:{}",
                         fwd.nextId(), deviceId, fwd.id());
                fail(fwd, ObjectiveError.FLOWINSTALLATIONFAILED);
                return Collections.emptySet();
            }
        }
        tb.transition(ACL_TABLE);
        FlowRule.Builder ruleBuilder = DefaultFlowRule.builder()
                .fromApp(fwd.appId())
                .withPriority(fwd.priority())
                .forDevice(deviceId)
                .withSelector(filteredSelector.build())
                .withTreatment(tb.build())
                .forTable(forTableId);

        if (fwd.permanent()) {
            ruleBuilder.makePermanent();
        } else {
            ruleBuilder.makeTemporary(fwd.timeout());
        }
        Collection<FlowRule> flowRuleCollection = new ArrayList<>();
        flowRuleCollection.add(ruleBuilder.build());
        if (defaultRule) {
            FlowRule.Builder rule = DefaultFlowRule.builder()
                .fromApp(fwd.appId())
                .withPriority(fwd.priority())
                .forDevice(deviceId)
                .withSelector(complementarySelector.build())
                .withTreatment(tb.build())
                .forTable(forTableId);
            if (fwd.permanent()) {
                rule.makePermanent();
            } else {
                rule.makeTemporary(fwd.timeout());
            }
            flowRuleCollection.add(rule.build());
            log.debug("Default rule 0.0.0.0/0 is being installed two rules");
        }

        return flowRuleCollection;
    }

    /**
     * Handles forwarding rules to the L2 bridging table. Flow actions are not
     * allowed in the bridging table - instead we use L2 Interface group or
     * L2 flood group
     *
     * @param fwd the forwarding objective
     * @return A collection of flow rules, or an empty set
     */
    protected Collection<FlowRule> processEthDstSpecific(ForwardingObjective fwd) {
        List<FlowRule> rules = new ArrayList<>();

        // Build filtered selector
        TrafficSelector selector = fwd.selector();
        EthCriterion ethCriterion = (EthCriterion) selector
                .getCriterion(Criterion.Type.ETH_DST);
        VlanIdCriterion vlanIdCriterion = (VlanIdCriterion) selector
                .getCriterion(Criterion.Type.VLAN_VID);

        if (vlanIdCriterion == null) {
            log.warn("Forwarding objective for bridging requires vlan. Not "
                    + "installing fwd:{} in dev:{}", fwd.id(), deviceId);
            fail(fwd, ObjectiveError.BADPARAMS);
            return Collections.emptySet();
        }

        TrafficSelector.Builder filteredSelectorBuilder =
                DefaultTrafficSelector.builder();
        // Do not match MacAddress for subnet broadcast entry
        if (!ethCriterion.mac().equals(MacAddress.NONE)) {
            filteredSelectorBuilder.matchEthDst(ethCriterion.mac());
            log.debug("processing L2 forwarding objective:{} -> next:{} in dev:{}",
                      fwd.id(), fwd.nextId(), deviceId);
        } else {
            log.debug("processing L2 Broadcast forwarding objective:{} -> next:{} "
                    + "in dev:{} for vlan:{}",
                      fwd.id(), fwd.nextId(), deviceId, vlanIdCriterion.vlanId());
        }
        OfdpaMatchVlanVid ofdpaMatchVlanVid = new OfdpaMatchVlanVid(vlanIdCriterion.vlanId());
        filteredSelectorBuilder.extension(ofdpaMatchVlanVid, deviceId);
        TrafficSelector filteredSelector = filteredSelectorBuilder.build();

        if (fwd.treatment() != null) {
            log.warn("Ignoring traffic treatment in fwd rule {} meant for L2 table"
                    + "for dev:{}. Expecting only nextId", fwd.id(), deviceId);
        }

        TrafficTreatment.Builder treatmentBuilder = DefaultTrafficTreatment.builder();
        if (fwd.nextId() != null) {
            NextGroup next = getGroupForNextObjective(fwd.nextId());
            if (next != null) {
                List<Deque<GroupKey>> gkeys = appKryo.deserialize(next.data());
                // we only need the top level group's key to point the flow to it
                Group group = groupService.getGroup(deviceId, gkeys.get(0).peekFirst());
                if (group != null) {
                    treatmentBuilder.deferred().group(group.id());
                } else {
                    log.warn("Group with key:{} for next-id:{} not found in dev:{}",
                             gkeys.get(0).peekFirst(), fwd.nextId(), deviceId);
                    fail(fwd, ObjectiveError.GROUPMISSING);
                    return Collections.emptySet();
                }
            }
        }
        treatmentBuilder.immediate().transition(ACL_TABLE);
        TrafficTreatment filteredTreatment = treatmentBuilder.build();

        // Build bridging table entries
        FlowRule.Builder flowRuleBuilder = DefaultFlowRule.builder();
        flowRuleBuilder.fromApp(fwd.appId())
                .withPriority(fwd.priority())
                .forDevice(deviceId)
                .withSelector(filteredSelector)
                .withTreatment(filteredTreatment)
                .forTable(BRIDGING_TABLE);
        if (fwd.permanent()) {
            flowRuleBuilder.makePermanent();
        } else {
            flowRuleBuilder.makeTemporary(fwd.timeout());
        }
        rules.add(flowRuleBuilder.build());
        return rules;
    }

    protected NextGroup getGroupForNextObjective(Integer nextId) {
        NextGroup next = flowObjectiveStore.getNextGroup(nextId);
        if (next != null) {
            List<Deque<GroupKey>> gkeys = appKryo.deserialize(next.data());
            if (gkeys != null && !gkeys.isEmpty()) {
                return next;
            } else {
               log.warn("Empty next group found in FlowObjective store for "
                       + "next-id:{} in dev:{}", nextId, deviceId);
            }
        } else {
            log.warn("next-id {} not found in Flow objective store for dev:{}",
                     nextId, deviceId);
        }
        return null;
    }

    protected static void pass(Objective obj) {
        obj.context().ifPresent(context -> context.onSuccess(obj));
    }

    protected static void fail(Objective obj, ObjectiveError error) {
        obj.context().ifPresent(context -> context.onError(obj, error));
    }

    @Override
    public List<String> getNextMappings(NextGroup nextGroup) {
        List<String> mappings = new ArrayList<>();
        List<Deque<GroupKey>> gkeys = appKryo.deserialize(nextGroup.data());
        for (Deque<GroupKey> gkd : gkeys) {
            Group lastGroup = null;
            StringBuffer gchain = new StringBuffer();
            for (GroupKey gk : gkd) {
                Group g = groupService.getGroup(deviceId, gk);
                if (g == null) {
                    gchain.append("  NoGrp").append(" -->");
                    continue;
                }
                gchain.append("  0x").append(Integer.toHexString(g.id().id()))
                    .append(" -->");
                lastGroup = g;
            }
            // add port information for last group in group-chain
            List<Instruction> lastGroupIns = new ArrayList<Instruction>();
            if (lastGroup != null) {
                lastGroupIns = lastGroup.buckets().buckets().get(0)
                                    .treatment().allInstructions();
            }
            for (Instruction i: lastGroupIns) {
                if (i instanceof OutputInstruction) {
                    gchain.append(" port:").append(((OutputInstruction) i).port());
                }
            }
            mappings.add(gchain.toString());
        }
        return mappings;
    }

    protected static VlanId readVlanFromSelector(TrafficSelector selector) {
        if (selector == null) {
            return null;
        }
        Criterion criterion = selector.getCriterion(Criterion.Type.VLAN_VID);
        return (criterion == null)
                ? null : ((VlanIdCriterion) criterion).vlanId();
    }

    protected static IpPrefix readIpDstFromSelector(TrafficSelector selector) {
        if (selector == null) {
            return null;
        }
        Criterion criterion = selector.getCriterion(Criterion.Type.IPV4_DST);
        return (criterion == null) ? null : ((IPCriterion) criterion).ip();
    }

    private static VlanId readVlanFromTreatment(TrafficTreatment treatment) {
        if (treatment == null) {
            return null;
        }
        for (Instruction i : treatment.allInstructions()) {
            if (i instanceof ModVlanIdInstruction) {
                return ((ModVlanIdInstruction) i).vlanId();
            }
        }
        return null;
    }
}

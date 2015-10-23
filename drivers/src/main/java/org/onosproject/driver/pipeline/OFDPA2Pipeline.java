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
package org.onosproject.driver.pipeline;

import static org.onlab.util.Tools.groupedThreads;
import static org.slf4j.LoggerFactory.getLogger;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.onlab.osgi.ServiceDirectory;
import org.onlab.packet.Data;
import org.onlab.packet.Ethernet;
import org.onlab.packet.IPv4;
import org.onlab.packet.IpPrefix;
import org.onlab.packet.MPLS;
import org.onlab.packet.MacAddress;
import org.onlab.packet.MplsLabel;
import org.onlab.packet.UDP;
import org.onlab.packet.VlanId;
import org.onlab.util.KryoNamespace;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.core.DefaultGroupId;
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
import org.onosproject.net.flow.criteria.IPCriterion;
import org.onosproject.net.flow.criteria.PortCriterion;
import org.onosproject.net.flow.criteria.VlanIdCriterion;
import org.onosproject.net.flow.instructions.Instruction;
import org.onosproject.net.flow.instructions.Instructions.OutputInstruction;
import org.onosproject.net.flow.instructions.L2ModificationInstruction;
import org.onosproject.net.flow.instructions.L2ModificationInstruction.ModEtherInstruction;
import org.onosproject.net.flow.instructions.L2ModificationInstruction.ModVlanIdInstruction;
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
import org.onosproject.net.packet.DefaultOutboundPacket;
import org.onosproject.net.packet.OutboundPacket;
import org.onosproject.net.packet.PacketContext;
import org.onosproject.net.packet.PacketProcessor;
import org.onosproject.net.packet.PacketService;
import org.onosproject.store.serializers.KryoNamespaces;
import org.slf4j.Logger;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.RemovalCause;
import com.google.common.cache.RemovalNotification;

/**
 * Driver for Broadcom's OF-DPA v2.0 TTP.
 *
 */
public class OFDPA2Pipeline extends AbstractHandlerBehaviour implements Pipeliner {

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

    private static final int HIGHEST_PRIORITY = 0xffff;
    private static final int DEFAULT_PRIORITY = 0x8000;
    protected static final int LOWEST_PRIORITY = 0x0;

    /*
     * Group keys are normally generated by using the next Objective id. In the
     * case of a next objective resulting in a group chain, each group derives a
     * group key from the next objective id in the following way:
     * The upper 4 bits of the group-key are used to denote the position of the
     * group in the group chain. For example, in the chain
     * group0 --> group1 --> group2 --> port
     * group0's group key would have the upper 4 bits as 0, group1's upper four
     * bits would be 1, and so on
     */
    private static final int GROUP0MASK = 0x0;
    private static final int GROUP1MASK = 0x10000000;

    /*
     * OFDPA requires group-id's to have a certain form.
     * L2 Interface Groups have <4bits-0><12bits-vlanid><16bits-portid>
     * L3 Unicast Groups have <4bits-2><28bits-index>
     */
    private static final int L2INTERFACEMASK = 0x0;
    private static final int L3UNICASTMASK = 0x20000000;
    //private static final int MPLSINTERFACEMASK = 0x90000000;
    private static final int L3ECMPMASK = 0x70000000;

    /*
     * This driver assigns all incoming untagged packets the same VLAN ID
     */
    private static final short UNTAGGED_ASSIGNED_VLAN = 0xffa; // 4090


    private final Logger log = getLogger(getClass());
    private ServiceDirectory serviceDirectory;
    protected FlowRuleService flowRuleService;
    private CoreService coreService;
    private GroupService groupService;
    private FlowObjectiveStore flowObjectiveStore;
    protected DeviceId deviceId;
    protected ApplicationId driverId;
    protected PacketService packetService;
    protected DeviceService deviceService;
    private InternalPacketProcessor processor = new InternalPacketProcessor();
    private KryoNamespace appKryo = new KryoNamespace.Builder()
        .register(KryoNamespaces.API)
        .register(GroupKey.class)
        .register(DefaultGroupKey.class)
        .register(OfdpaGroupChain.class)
        .register(byte[].class)
        .build();

    private Cache<GroupKey, OfdpaGroupChain> pendingNextObjectives;
    private ConcurrentHashMap<GroupKey, GroupChainElem> pendingGroups;

    private ScheduledExecutorService groupChecker =
            Executors.newScheduledThreadPool(2, groupedThreads("onos/pipeliner",
                                                               "ofdpa2-%d"));
    private Set<IPCriterion> sentIpFilters = Collections.newSetFromMap(
                                               new ConcurrentHashMap<IPCriterion, Boolean>());

    @Override
    public void init(DeviceId deviceId, PipelinerContext context) {
        this.serviceDirectory = context.directory();
        this.deviceId = deviceId;

        pendingNextObjectives = CacheBuilder.newBuilder()
                .expireAfterWrite(20, TimeUnit.SECONDS)
                .removalListener((RemovalNotification<GroupKey, OfdpaGroupChain> notification) -> {
                    if (notification.getCause() == RemovalCause.EXPIRED) {
                        fail(notification.getValue().nextObjective(),
                             ObjectiveError.GROUPINSTALLATIONFAILED);
                    }
                }).build();

        groupChecker.scheduleAtFixedRate(new GroupChecker(), 0, 500, TimeUnit.MILLISECONDS);
        pendingGroups = new ConcurrentHashMap<GroupKey, GroupChainElem>();

        coreService = serviceDirectory.get(CoreService.class);
        flowRuleService = serviceDirectory.get(FlowRuleService.class);
        groupService = serviceDirectory.get(GroupService.class);
        flowObjectiveStore = context.store();
        packetService = serviceDirectory.get(PacketService.class);
        deviceService = serviceDirectory.get(DeviceService.class);
        packetService.addProcessor(processor, PacketProcessor.director(2));
        groupService.addListener(new InnerGroupListener());

        driverId = coreService.registerApplication(
                "org.onosproject.driver.OFDPA2Pipeline");

        // OF-DPA does not require initializing the pipeline as it puts default
        // rules automatically in the hardware. However emulation of OFDPA in
        // software switches does require table-miss-entries.
        initializePipeline();

    }

    protected void initializePipeline() {

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
        switch (fwd.op()) {
            case ADD:
                rules.stream()
                        .filter(rule -> rule != null)
                        .forEach(flowOpsBuilder::add);
                break;
            case REMOVE:
                rules.stream()
                        .filter(rule -> rule != null)
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
        switch (nextObjective.type()) {
        case SIMPLE:
            Collection<TrafficTreatment> treatments = nextObjective.next();
            if (treatments.size() != 1) {
                log.error("Next Objectives of type Simple should only have a "
                        + "single Traffic Treatment. Next Objective Id:{}", nextObjective.id());
               fail(nextObjective, ObjectiveError.BADPARAMS);
               return;
            }
            processSimpleNextObjective(nextObjective);
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
    private void processFilter(FilteringObjective filt,
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
            fail(filt, ObjectiveError.UNKNOWN);
            return;
        }
        // convert filtering conditions for switch-intfs into flowrules
        FlowRuleOperations.Builder ops = FlowRuleOperations.builder();
        for (Criterion criterion : filt.conditions()) {
            if (criterion.type() == Criterion.Type.ETH_DST) {
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

        if (ethCriterion == null) {
            log.debug("filtering objective missing dstMac, cannot program TMAC table");
        } else {
            for (FlowRule tmacRule : processEthDstFilter(portCriterion, ethCriterion,
                                                         vidCriterion, applicationId)) {
                log.debug("adding MAC filtering rules in TMAC table: {} for dev: {}",
                          tmacRule, deviceId);
                ops = install ? ops.add(tmacRule) : ops.remove(tmacRule);
            }
        }

        if (ethCriterion == null || vidCriterion == null) {
            log.debug("filtering objective missing dstMac or vlan, cannot program"
                    + "Vlan Table");
        } else {
            for (FlowRule vlanRule : processVlanIdFilter(portCriterion, vidCriterion,
                                                         applicationId)) {
                log.debug("adding VLAN filtering rule in VLAN table: {} for dev: {}",
                          vlanRule, deviceId);
                ops = install ? ops.add(vlanRule) : ops.remove(vlanRule);
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
     * Allows tagged packets into pipeline as per configured port-vlan info.
     * @param portCriterion   port on device for which this filter is programmed
     * @param vidCriterion   vlan assigned to port, or NONE for untagged
     * @param applicationId  for application programming this filter
     * @return list of FlowRule for port-vlan filters
     */
    protected List<FlowRule> processVlanIdFilter(PortCriterion portCriterion,
                                                 VlanIdCriterion vidCriterion,
                                                 ApplicationId applicationId) {
        List<FlowRule> rules = new ArrayList<FlowRule>();
        TrafficSelector.Builder selector = DefaultTrafficSelector.builder();
        TrafficTreatment.Builder treatment = DefaultTrafficTreatment.builder();
        selector.matchVlanId(vidCriterion.vlanId());
        if (vidCriterion.vlanId() == VlanId.NONE) {
            // untagged packets are assigned vlans
            treatment.pushVlan().setVlanId(VlanId.vlanId(UNTAGGED_ASSIGNED_VLAN));
            // XXX ofdpa may require an additional vlan match on the assigned vlan
            // and it may not require the push.
        }
        treatment.transition(TMAC_TABLE);

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
        for (PortNumber pnum : portnums) {
            selector.matchInPort(pnum);
            FlowRule rule = DefaultFlowRule.builder()
                    .forDevice(deviceId)
                    .withSelector(selector.build())
                    .withTreatment(treatment.build())
                    .withPriority(DEFAULT_PRIORITY)
                    .fromApp(applicationId)
                    .makePermanent()
                    .forTable(VLAN_TABLE).build();
            rules.add(rule);
        }
        return rules;
    }

    /**
     * Allows routed packets with correct destination MAC to be directed
     * to unicast-IP routing table or MPLS forwarding table.
     * XXX need to add rule for multicast routing.
     *
     * @param portCriterion  port on device for which this filter is programmed
     * @param ethCriterion   dstMac of device for which is filter is programmed
     * @param vidCriterion   vlan assigned to port, or NONE for untagged
     * @param applicationId  for application programming this filter
     * @return list of FlowRule for port-vlan filters

     */
    protected List<FlowRule> processEthDstFilter(PortCriterion portCriterion,
                                                 EthCriterion ethCriterion,
                                                 VlanIdCriterion vidCriterion,
                                                 ApplicationId applicationId) {
        //handling untagged packets via assigned VLAN
        if (vidCriterion.vlanId() == VlanId.NONE) {
            vidCriterion = (VlanIdCriterion) Criteria
                                .matchVlanId(VlanId.vlanId(UNTAGGED_ASSIGNED_VLAN));
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
            // for unicast IP packets
            TrafficSelector.Builder selector = DefaultTrafficSelector.builder();
            TrafficTreatment.Builder treatment = DefaultTrafficTreatment.builder();
            selector.matchInPort(pnum);
            selector.matchVlanId(vidCriterion.vlanId());
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
            selector.matchVlanId(vidCriterion.vlanId());
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
    private Collection<FlowRule> processVersatile(ForwardingObjective fwd) {
        log.info("Processing versatile forwarding objective");
        TrafficSelector selector = fwd.selector();

        EthTypeCriterion ethType =
                (EthTypeCriterion) selector.getCriterion(Criterion.Type.ETH_TYPE);
        if (ethType == null) {
            log.error("Versatile forwarding objective must include ethType");
            fail(fwd, ObjectiveError.BADPARAMS);
            return Collections.emptySet();
        }
        if (fwd.nextId() == null && fwd.treatment() == null) {
            log.error("Forwarding objective {} from {} must contain "
                    + "nextId or Treatment", fwd.selector(), fwd.appId());
            return Collections.emptySet();
        }
        // XXX driver does not currently do type checking as per Tables 65-67 in
        // OFDPA 2.0 spec. The only allowed treatment is a punt to the controller.
        if (fwd.treatment() != null &&
                fwd.treatment().allInstructions().size() == 1 &&
                fwd.treatment().allInstructions().get(0).type() == Instruction.Type.OUTPUT) {
            OutputInstruction o = (OutputInstruction) fwd.treatment().allInstructions().get(0);
            if (o.port() == PortNumber.CONTROLLER) {
                FlowRule.Builder ruleBuilder = DefaultFlowRule.builder()
                        .fromApp(fwd.appId())
                        .withPriority(fwd.priority())
                        .forDevice(deviceId)
                        .withSelector(fwd.selector())
                        .withTreatment(fwd.treatment())
                        .makePermanent()
                        .forTable(ACL_TABLE);
                return Collections.singletonList(ruleBuilder.build());
            } else {
                log.warn("Only allowed treatments in versatile forwarding "
                        + "objectives are punts to the controller");
                return Collections.emptySet();
            }
        }

        if (fwd.nextId() != null) {
            // XXX overide case
            log.warn("versatile objective --> next Id not yet implemeted");
        }
        return Collections.emptySet();
    }

    /**
     * In the OF-DPA 2.0 pipeline, specific forwarding refers to the IP table
     * (unicast or multicast) or the L2 table (mac + vlan).
     *
     * @param fwd the forwarding objective of type 'specific'
     * @return    a collection of flow rules. Typically there will be only one
     *            for this type of forwarding objective. An empty set may be
     *            returned if there is an issue in processing the objective.
     */
    private Collection<FlowRule> processSpecific(ForwardingObjective fwd) {
        log.debug("Processing specific forwarding objective");
        TrafficSelector selector = fwd.selector();
        EthTypeCriterion ethType =
                (EthTypeCriterion) selector.getCriterion(Criterion.Type.ETH_TYPE);
        // XXX currently supporting only the L3 unicast table
        if (ethType == null || ethType.ethType().toShort() != Ethernet.TYPE_IPV4) {
            fail(fwd, ObjectiveError.UNSUPPORTED);
            return Collections.emptySet();
        }

        TrafficSelector filteredSelector =
                DefaultTrafficSelector.builder()
                        .matchEthType(Ethernet.TYPE_IPV4)
                        .matchIPDst(
                                ((IPCriterion)
                                        selector.getCriterion(Criterion.Type.IPV4_DST)).ip())
                        .build();

        TrafficTreatment.Builder tb = DefaultTrafficTreatment.builder();

        if (fwd.nextId() != null) {
            NextGroup next = flowObjectiveStore.getNextGroup(fwd.nextId());
            List<GroupKey> gkeys = appKryo.deserialize(next.data());
            Group group = groupService.getGroup(deviceId, gkeys.get(0));
            if (group == null) {
                log.warn("The group left!");
                fail(fwd, ObjectiveError.GROUPMISSING);
                return Collections.emptySet();
            }
            tb.deferred().group(group.id());
        }
        tb.transition(ACL_TABLE);
        FlowRule.Builder ruleBuilder = DefaultFlowRule.builder()
                .fromApp(fwd.appId())
                .withPriority(fwd.priority())
                .forDevice(deviceId)
                .withSelector(filteredSelector)
                .withTreatment(tb.build());

        if (fwd.permanent()) {
            ruleBuilder.makePermanent();
        } else {
            ruleBuilder.makeTemporary(fwd.timeout());
        }

        ruleBuilder.forTable(UNICAST_ROUTING_TABLE);
        return Collections.singletonList(ruleBuilder.build());
    }

    private void pass(Objective obj) {
        if (obj.context().isPresent()) {
            obj.context().get().onSuccess(obj);
        }
    }

    private void fail(Objective obj, ObjectiveError error) {
        if (obj.context().isPresent()) {
            obj.context().get().onError(obj, error);
        }
    }

    //////////////////////////////////////
    //  Group handling
    //////////////////////////////////////

    /**
     * As per the OFDPA 2.0 TTP, packets are sent out of ports by using
     * a chain of groups, namely an L3 Unicast Group that points to an L2 Interface
     * Group which in turns points to an output port. The Next Objective passed
     * in by the application has to be broken up into a group chain
     * to satisfy this TTP.
     *
     * @param nextObj  the nextObjective of type SIMPLE
     */
    private void processSimpleNextObjective(NextObjective nextObj) {
        // break up simple next objective to GroupChain objects
        TrafficTreatment treatment = nextObj.next().iterator().next();
        // for the l2interface group, get vlan and port info
        // for the l3unicast group, get the src/dst mac and vlan info
        TrafficTreatment.Builder l3utt = DefaultTrafficTreatment.builder();
        TrafficTreatment.Builder l2itt = DefaultTrafficTreatment.builder();
        VlanId vlanid = null;
        long portNum = 0;
        for (Instruction ins : treatment.allInstructions()) {
            if (ins.type() == Instruction.Type.L2MODIFICATION) {
                L2ModificationInstruction l2ins = (L2ModificationInstruction) ins;
                switch (l2ins.subtype()) {
                case ETH_DST:
                    l3utt.setEthDst(((ModEtherInstruction) l2ins).mac());
                    break;
                case ETH_SRC:
                    l3utt.setEthSrc(((ModEtherInstruction) l2ins).mac());
                    break;
                case VLAN_ID:
                    vlanid = ((ModVlanIdInstruction) l2ins).vlanId();
                    l3utt.setVlanId(vlanid);
                    break;
                case DEC_MPLS_TTL:
                case MPLS_LABEL:
                case MPLS_POP:
                case MPLS_PUSH:
                case VLAN_PCP:
                case VLAN_POP:
                case VLAN_PUSH:
                default:
                    break;
                }
            } else if (ins.type() == Instruction.Type.OUTPUT) {
                portNum = ((OutputInstruction) ins).port().toLong();
                l2itt.add(ins);
            } else {
                log.warn("Driver does not handle this type of TrafficTreatment"
                        + " instruction in nextObjectives:  {}", ins.type());
            }
        }

        // assemble information for ofdpa l2interface group
        int l2gk = nextObj.id() | GROUP1MASK;
        final GroupKey l2groupkey = new DefaultGroupKey(appKryo.serialize(l2gk));
        Integer l2groupId = L2INTERFACEMASK | (vlanid.toShort() << 16) | (int) portNum;

        // assemble information for ofdpa l3unicast group
        int l3gk = nextObj.id() | GROUP0MASK;
        final GroupKey l3groupkey = new DefaultGroupKey(appKryo.serialize(l3gk));
        Integer l3groupId = L3UNICASTMASK | (int) portNum;
        l3utt.group(new DefaultGroupId(l2groupId));
        GroupChainElem gce = new GroupChainElem(l3groupkey, l3groupId,
                                                l3utt.build(), nextObj.appId());

        // create object for local and distributed storage
        List<GroupKey> gkeys = new ArrayList<GroupKey>();
        gkeys.add(l3groupkey); // group0 in chain
        gkeys.add(l2groupkey); // group1 in chain
        OfdpaGroupChain ofdpaGrp = new OfdpaGroupChain(gkeys, nextObj);

        // store l2groupkey with the groupChainElem for the l3group that depends on it
        pendingGroups.put(l2groupkey, gce);

        // store l3groupkey with the ofdpaGroupChain for the nextObjective that depends on it
        pendingNextObjectives.put(l3groupkey, ofdpaGrp);

        // create group description for the ofdpa l2interfacegroup and send to groupservice
        GroupBucket bucket =
                DefaultGroupBucket.createIndirectGroupBucket(l2itt.build());
        GroupDescription groupDescription = new DefaultGroupDescription(deviceId,
                             GroupDescription.Type.INDIRECT,
                             new GroupBuckets(Collections.singletonList(bucket)),
                             l2groupkey,
                             l2groupId,
                             nextObj.appId());
        groupService.addGroup(groupDescription);
    }

    /**
     * Processes next element of a group chain. Assumption is that if this
     * group points to another group, the latter has already been created
     * and this driver has received notification for it. A second assumption is
     * that if there is another group waiting for this group then the appropriate
     * stores already have the information to act upon the notification for the
     * creating of this group.
     *
     * @param gce the group chain element to be processed next
     */
    private void processGroupChain(GroupChainElem gce) {
        GroupBucket bucket = DefaultGroupBucket
                .createIndirectGroupBucket(gce.getBucketActions());
        GroupDescription groupDesc = new DefaultGroupDescription(deviceId,
                             GroupDescription.Type.INDIRECT,
                             new GroupBuckets(Collections.singletonList(bucket)),
                             gce.getGkey(),
                             gce.getGivenGroupId(),
                             gce.getAppId());
        groupService.addGroup(groupDesc);
    }


    private class GroupChecker implements Runnable {
        @Override
        public void run() {
            Set<GroupKey> keys = pendingGroups.keySet().stream()
                    .filter(key -> groupService.getGroup(deviceId, key) != null)
                    .collect(Collectors.toSet());
            Set<GroupKey> otherkeys = pendingNextObjectives.asMap().keySet().stream()
                    .filter(otherkey -> groupService.getGroup(deviceId, otherkey) != null)
                    .collect(Collectors.toSet());
            keys.addAll(otherkeys);

            keys.stream().forEach(key -> {
                //first check for group chain
                GroupChainElem gce = pendingGroups.remove(key);
                if (gce != null) {
                    log.info("Group service processed group key {}. Processing next "
                            + "group in group chain with group key {}",
                            appKryo.deserialize(key.key()),
                            appKryo.deserialize(gce.getGkey().key()));
                    processGroupChain(gce);
                } else {
                    OfdpaGroupChain obj = pendingNextObjectives.getIfPresent(key);
                    log.info("Group service processed group key {}. Done implementing "
                            + "next objective: {}", appKryo.deserialize(key.key()),
                            obj.nextObjective().id());
                    if (obj != null) {
                        pass(obj.nextObjective());
                        pendingNextObjectives.invalidate(key);
                        flowObjectiveStore.putNextGroup(obj.nextObjective().id(), obj);
                    }
                }
            });
        }
    }

    private class InnerGroupListener implements GroupListener {
        @Override
        public void event(GroupEvent event) {
            log.debug("received group event of type {}", event.type());
            if (event.type() == GroupEvent.Type.GROUP_ADDED) {
                GroupKey key = event.subject().appCookie();
                // first check for group chain
                GroupChainElem gce = pendingGroups.remove(key);
                if (gce != null) {
                    log.info("group ADDED with group key {} .. "
                            + "Processing next group in group chain with group key {}",
                            appKryo.deserialize(key.key()),
                            appKryo.deserialize(gce.getGkey().key()));
                    processGroupChain(gce);
                } else {
                    OfdpaGroupChain obj = pendingNextObjectives.getIfPresent(key);
                    if (obj != null) {
                        log.info("group ADDED with key {}.. Done implementing next "
                                + "objective: {}",
                                appKryo.deserialize(key.key()), obj.nextObjective().id());
                        pass(obj.nextObjective());
                        pendingNextObjectives.invalidate(key);
                        flowObjectiveStore.putNextGroup(obj.nextObjective().id(), obj);
                    }
                }
            }
        }
    }

    /**
     * Represents a group-chain that implements a Next-Objective from
     * the application. Includes information about the next objective Id, and the
     * group keys for the groups in the group chain. The chain is expected to
     * look like group0 --> group 1 --> outPort. Information about the groups
     * themselves can be fetched from the Group Service using the group keys from
     * objects instantiating this class.
     */
    private class OfdpaGroupChain implements NextGroup {
        private final NextObjective nextObj;
        private final List<GroupKey> gkeys;

        /** expected group chain: group0 --> group1 --> port. */
        public OfdpaGroupChain(List<GroupKey> gkeys, NextObjective nextObj) {
            this.gkeys = gkeys;
            this.nextObj = nextObj;
        }

        @SuppressWarnings("unused")
        public List<GroupKey> groupKeys() {
            return gkeys;
        }

        public NextObjective nextObjective() {
            return nextObj;
        }

        @Override
        public byte[] data() {
            return appKryo.serialize(gkeys);
        }

    }

    /**
     * Represents a group element that is part of a chain of groups.
     * Stores enough information to create a Group Description to add the group
     * to the switch by requesting the Group Service. Objects instantiating this
     * class are meant to be temporary and live as long as it is needed to wait for
     * preceding groups in the group chain to be created.
     */
    private class GroupChainElem {
        private TrafficTreatment bucketActions;
        private Integer givenGroupId;
        private GroupKey gkey;
        private ApplicationId appId;

        public GroupChainElem(GroupKey gkey, Integer givenGroupId,
                              TrafficTreatment tr, ApplicationId appId) {
            this.bucketActions = tr;
            this.givenGroupId = givenGroupId;
            this.gkey = gkey;
            this.appId = appId;
        }

        public TrafficTreatment getBucketActions() {
            return bucketActions;
        }

        public Integer getGivenGroupId() {
            return givenGroupId;
        }

        public GroupKey getGkey() {
            return gkey;
        }

        public ApplicationId getAppId() {
            return appId;
        }

    }

    //////////////////////////////////////
    //  Test code to be used for future
    //  static-flow-pusher app
    //////////////////////////////////////

    public void processStaticFlows() {
        //processPortTable();
        processGroupTable();
        processVlanTable();
        processTmacTable();
        processIpTable();
        //processMcastTable();
        //processBridgingTable();
        processAclTable();
        sendPackets();
        processMplsTable();
    }

    protected void processGroupTable() {
        TrafficTreatment.Builder act = DefaultTrafficTreatment.builder();

        act.popVlan(); // to send out untagged packets
        act.setOutput(PortNumber.portNumber(24));
        GroupBucket bucket =
                DefaultGroupBucket.createIndirectGroupBucket(act.build());
        final GroupKey groupkey = new DefaultGroupKey(appKryo.serialize(500));
        Integer groupId = 0x00c80018; //l2 interface, vlan 200, port 24
        GroupDescription groupDescription = new DefaultGroupDescription(deviceId,
                             GroupDescription.Type.INDIRECT,
                             new GroupBuckets(Collections.singletonList(bucket)),
                             groupkey,
                             groupId,
                             driverId);
        groupService.addGroup(groupDescription);

        TrafficTreatment.Builder act2 = DefaultTrafficTreatment.builder();
        act2.setOutput(PortNumber.portNumber(40));
        GroupBucket bucket2 = DefaultGroupBucket.createIndirectGroupBucket(act2.build());
        final GroupKey groupkey2 = new DefaultGroupKey(appKryo.serialize(502));
        Integer groupId2 = 0x00c50028; //l2 interface, vlan 197, port 40
        GroupDescription groupDescription2 = new DefaultGroupDescription(deviceId,
                             GroupDescription.Type.INDIRECT,
                             new GroupBuckets(Collections.singletonList(bucket2)),
                             groupkey2,
                             groupId2,
                             driverId);
        groupService.addGroup(groupDescription2);

        while (groupService.getGroup(deviceId, groupkey2) == null) {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

        //Now for L3 Unicast group
        TrafficTreatment.Builder act3 = DefaultTrafficTreatment.builder();
        act3.setEthDst(MacAddress.valueOf(0x2020));
        act3.setEthSrc(MacAddress.valueOf(0x1010));
        act3.setVlanId(VlanId.vlanId((short) 200));
        act3.group(new DefaultGroupId(0x00c80018)); // point to L2 interface
        // MPLS interface group - does not work for popping single label
        //Integer secGroupId = MPLSINTERFACEMASK | 38; // 0x90000026
        Integer groupId3 = L3UNICASTMASK | 1; // 0x20000001
        GroupBucket bucket3 =
                DefaultGroupBucket.createIndirectGroupBucket(act3.build());
        final GroupKey groupkey3 = new DefaultGroupKey(appKryo.serialize(503));
        GroupDescription groupDescription3 = new DefaultGroupDescription(deviceId,
                             GroupDescription.Type.INDIRECT,
                             new GroupBuckets(Collections.singletonList(bucket3)),
                             groupkey3,
                             groupId3,
                             driverId);
        groupService.addGroup(groupDescription3);

        //Another L3 Unicast group
        TrafficTreatment.Builder act4 = DefaultTrafficTreatment.builder();
        act4.setEthDst(MacAddress.valueOf(0x3030));
        act4.setEthSrc(MacAddress.valueOf(0x1010));
        act4.setVlanId(VlanId.vlanId((short) 197));
        act4.group(new DefaultGroupId(0x00c50028)); // point to L2 interface
        Integer groupId4 = L3UNICASTMASK | 2; // 0x20000002
        GroupBucket bucket4 =
                DefaultGroupBucket.createIndirectGroupBucket(act4.build());
        final GroupKey groupkey4 = new DefaultGroupKey(appKryo.serialize(504));
        GroupDescription groupDescription4 = new DefaultGroupDescription(deviceId,
                             GroupDescription.Type.INDIRECT,
                             new GroupBuckets(Collections.singletonList(bucket4)),
                             groupkey4,
                             groupId4,
                             driverId);
        groupService.addGroup(groupDescription4);

        while (groupService.getGroup(deviceId, groupkey4) == null) {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

        // L3 ecmp group
        TrafficTreatment.Builder act5 = DefaultTrafficTreatment.builder();
        act5.group(new DefaultGroupId(0x20000001));
        TrafficTreatment.Builder act6 = DefaultTrafficTreatment.builder();
        act6.group(new DefaultGroupId(0x20000002));
        GroupBucket buckete1 =
                DefaultGroupBucket.createSelectGroupBucket(act5.build());
        GroupBucket buckete2 =
                DefaultGroupBucket.createSelectGroupBucket(act6.build());
        List<GroupBucket> bktlist = new ArrayList<GroupBucket>();
        bktlist.add(buckete1);
        bktlist.add(buckete2);
        final GroupKey groupkey5 = new DefaultGroupKey(appKryo.serialize(505));
        Integer groupId5 = L3ECMPMASK | 5; // 0x70000005
        GroupDescription groupDescription5 = new DefaultGroupDescription(deviceId,
                             GroupDescription.Type.SELECT,
                             new GroupBuckets(bktlist),
                             groupkey5,
                             groupId5,
                             driverId);
        groupService.addGroup(groupDescription5);


    }

    @SuppressWarnings("deprecation")
    protected void processMplsTable() {
        FlowRuleOperations.Builder ops = FlowRuleOperations.builder();
        TrafficSelector.Builder selector = DefaultTrafficSelector.builder();
        selector.matchEthType(Ethernet.MPLS_UNICAST);
        selector.matchMplsLabel(MplsLabel.mplsLabel(0xff)); //255
        selector.matchMplsBos(true);
        TrafficTreatment.Builder treatment = DefaultTrafficTreatment.builder();
        treatment.decMplsTtl(); // nw_ttl does not work
        treatment.copyTtlIn();
        treatment.popMpls(Ethernet.TYPE_IPV4);
        treatment.deferred().group(new DefaultGroupId(0x20000001)); // point to L3 Unicast
        //treatment.deferred().group(new DefaultGroupId(0x70000005)); // point to L3 ECMP
        treatment.transition(ACL_TABLE);
        FlowRule test = DefaultFlowRule.builder().forDevice(deviceId)
                .withSelector(selector.build()).withTreatment(treatment.build())
                .withPriority(DEFAULT_PRIORITY).fromApp(driverId).makePermanent()
                .forTable(24).build();
        ops = ops.add(test);

        flowRuleService.apply(ops.build(new FlowRuleOperationsContext() {
            @Override
            public void onSuccess(FlowRuleOperations ops) {
                log.info("Initialized mpls table");
            }

            @Override
            public void onError(FlowRuleOperations ops) {
                log.info("Failed to initialize mpls table");
            }
        }));

    }

    protected void processPortTable() {
        FlowRuleOperations.Builder ops = FlowRuleOperations.builder();
        TrafficSelector.Builder selector = DefaultTrafficSelector.builder();
        selector.matchInPort(PortNumber.portNumber(0)); // should be maskable?
        TrafficTreatment.Builder treatment = DefaultTrafficTreatment.builder();
        treatment.transition(VLAN_TABLE);
        FlowRule tmisse = DefaultFlowRule.builder()
                .forDevice(deviceId)
                .withSelector(selector.build())
                .withTreatment(treatment.build())
                .withPriority(LOWEST_PRIORITY)
                .fromApp(driverId)
                .makePermanent()
                .forTable(PORT_TABLE).build();
        ops = ops.add(tmisse);

        flowRuleService.apply(ops.build(new FlowRuleOperationsContext() {
            @Override
            public void onSuccess(FlowRuleOperations ops) {
                log.info("Initialized port table");
            }

            @Override
            public void onError(FlowRuleOperations ops) {
                log.info("Failed to initialize port table");
            }
        }));

    }

    private void processVlanTable() {
        // Table miss entry is not required as ofdpa default is to drop
        // In OF terms, the absence of a t.m.e. also implies drop
        FlowRuleOperations.Builder ops = FlowRuleOperations.builder();
        TrafficSelector.Builder selector = DefaultTrafficSelector.builder();
        TrafficTreatment.Builder treatment = DefaultTrafficTreatment.builder();
        selector.matchInPort(PortNumber.portNumber(12));
        selector.matchVlanId(VlanId.vlanId((short) 100));
        treatment.transition(TMAC_TABLE);
        FlowRule rule = DefaultFlowRule.builder()
                .forDevice(deviceId)
                .withSelector(selector.build())
                .withTreatment(treatment.build())
                .withPriority(DEFAULT_PRIORITY)
                .fromApp(driverId)
                .makePermanent()
                .forTable(VLAN_TABLE).build();
        ops =  ops.add(rule);
        flowRuleService.apply(ops.build(new FlowRuleOperationsContext() {
            @Override
            public void onSuccess(FlowRuleOperations ops) {
                log.info("Initialized vlan table");
            }

            @Override
            public void onError(FlowRuleOperations ops) {
                log.info("Failed to initialize vlan table");
            }
        }));
    }

    protected void processTmacTable() {
        //table miss entry
        FlowRuleOperations.Builder ops = FlowRuleOperations.builder();
        TrafficSelector.Builder selector = DefaultTrafficSelector.builder();
        TrafficTreatment.Builder treatment = DefaultTrafficTreatment.builder();
        selector.matchInPort(PortNumber.portNumber(12));
        selector.matchVlanId(VlanId.vlanId((short) 100));
        selector.matchEthType(Ethernet.TYPE_IPV4);
        selector.matchEthDst(MacAddress.valueOf("00:00:00:00:00:02"));
        treatment.transition(UNICAST_ROUTING_TABLE);
        FlowRule rule = DefaultFlowRule.builder()
                .forDevice(deviceId)
                .withSelector(selector.build())
                .withTreatment(treatment.build())
                .withPriority(DEFAULT_PRIORITY)
                .fromApp(driverId)
                .makePermanent()
                .forTable(TMAC_TABLE).build();
        ops = ops.add(rule);

        selector.matchEthType(Ethernet.MPLS_UNICAST);
        treatment.transition(MPLS_TABLE_0);
        FlowRule rulempls = DefaultFlowRule.builder()
                .forDevice(deviceId)
                .withSelector(selector.build())
                .withTreatment(treatment.build())
                .withPriority(DEFAULT_PRIORITY)
                .fromApp(driverId)
                .makePermanent()
                .forTable(TMAC_TABLE).build();
        ops = ops.add(rulempls);

        flowRuleService.apply(ops.build(new FlowRuleOperationsContext() {
            @Override
            public void onSuccess(FlowRuleOperations ops) {
                log.info("Initialized tmac table");
            }

            @Override
            public void onError(FlowRuleOperations ops) {
                log.info("Failed to initialize tmac table");
            }
        }));
    }

    protected void processIpTable() {
        //table miss entry
        FlowRuleOperations.Builder ops = FlowRuleOperations.builder();
        TrafficSelector.Builder selector = DefaultTrafficSelector.builder();
        TrafficTreatment.Builder treatment = DefaultTrafficTreatment.builder();
        selector.matchEthType(Ethernet.TYPE_IPV4);
        selector.matchIPDst(IpPrefix.valueOf("2.0.0.0/16"));
        treatment.deferred().group(new DefaultGroupId(0x20000001));
        treatment.transition(ACL_TABLE);
        FlowRule rule = DefaultFlowRule.builder()
                .forDevice(deviceId)
                .withSelector(selector.build())
                .withTreatment(treatment.build())
                .withPriority(30000)
                .fromApp(driverId)
                .makePermanent()
                .forTable(UNICAST_ROUTING_TABLE).build();
        ops =  ops.add(rule);
        flowRuleService.apply(ops.build(new FlowRuleOperationsContext() {
            @Override
            public void onSuccess(FlowRuleOperations ops) {
                log.info("Initialized IP table");
            }

            @Override
            public void onError(FlowRuleOperations ops) {
                log.info("Failed to initialize unicast IP table");
            }
        }));
    }

    protected void processAclTable() {
        //table miss entry
        FlowRuleOperations.Builder ops = FlowRuleOperations.builder();
        TrafficSelector.Builder selector = DefaultTrafficSelector.builder();
        TrafficTreatment.Builder treatment = DefaultTrafficTreatment.builder();
        selector.matchEthDst(MacAddress.valueOf("00:00:00:00:00:02"));
        treatment.deferred().group(new DefaultGroupId(0x20000001));
        FlowRule rule = DefaultFlowRule.builder()
                .forDevice(deviceId)
                .withSelector(selector.build())
                .withTreatment(treatment.build())
                .withPriority(60000)
                .fromApp(driverId)
                .makePermanent()
                .forTable(ACL_TABLE).build();
        ops =  ops.add(rule);
        flowRuleService.apply(ops.build(new FlowRuleOperationsContext() {
            @Override
            public void onSuccess(FlowRuleOperations ops) {
                log.info("Initialized Acl table");
            }

            @Override
            public void onError(FlowRuleOperations ops) {
                log.info("Failed to initialize Acl table");
            }
        }));
    }

    private void sendPackets() {
        Ethernet eth = new Ethernet();
        eth.setDestinationMACAddress("00:00:00:00:00:02");
        eth.setSourceMACAddress("00:00:00:11:22:33");
        eth.setVlanID((short) 100);
        eth.setEtherType(Ethernet.MPLS_UNICAST);
        MPLS mplsPkt = new MPLS();
        mplsPkt.setLabel(255);
        mplsPkt.setTtl((byte) 5);

        IPv4 ipv4 = new IPv4();

        ipv4.setDestinationAddress("4.0.5.6");
        ipv4.setSourceAddress("1.0.2.3");
        ipv4.setTtl((byte) 64);
        ipv4.setChecksum((short) 0);

        UDP udp = new UDP();
        udp.setDestinationPort(666);
        udp.setSourcePort(333);
        udp.setPayload(new Data(new byte[]{(byte) 1, (byte) 2}));
        udp.setChecksum((short) 0);

        ipv4.setPayload(udp);
        mplsPkt.setPayload(ipv4);
        eth.setPayload(mplsPkt);

        TrafficTreatment treatment = DefaultTrafficTreatment.builder()
                .setOutput(PortNumber.portNumber(24))
                .build();
        OutboundPacket packet = new DefaultOutboundPacket(deviceId,
                                                          treatment,
                                                          ByteBuffer.wrap(eth.serialize()));


        Ethernet eth2 = new Ethernet();
        eth2.setDestinationMACAddress("00:00:00:00:00:02");
        eth2.setSourceMACAddress("00:00:00:11:22:33");
        eth2.setVlanID((short) 100);
        eth2.setEtherType(Ethernet.TYPE_IPV4);

        IPv4 ipv42 = new IPv4();
        ipv42.setDestinationAddress("2.0.0.2");
        ipv42.setSourceAddress("1.0.9.9");
        ipv42.setTtl((byte) 64);
        ipv42.setChecksum((short) 0);

        UDP udp2 = new UDP();
        udp2.setDestinationPort(999);
        udp2.setSourcePort(333);
        udp2.setPayload(new Data(new byte[]{(byte) 1, (byte) 2}));
        udp2.setChecksum((short) 0);

        ipv42.setPayload(udp2);
        eth2.setPayload(ipv42);

        TrafficTreatment treatment2 = DefaultTrafficTreatment.builder()
                .setOutput(PortNumber.portNumber(26))
                .build();
        OutboundPacket packet2 = new DefaultOutboundPacket(deviceId,
                                                          treatment2,
                                                          ByteBuffer.wrap(eth2.serialize()));


        log.info("Emitting packets now");
        packetService.emit(packet);
        packetService.emit(packet);
        packetService.emit(packet2);
        packetService.emit(packet);
        packetService.emit(packet);
        log.info("Done emitting packets");
    }

    private class InternalPacketProcessor implements PacketProcessor {

        @Override
        public void process(PacketContext context) {


        }
    }

}

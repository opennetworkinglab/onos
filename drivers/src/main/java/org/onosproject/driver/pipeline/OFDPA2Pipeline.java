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
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
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
import org.onosproject.net.flow.criteria.Criterion.Type;
import org.onosproject.net.flow.criteria.EthCriterion;
import org.onosproject.net.flow.criteria.EthTypeCriterion;
import org.onosproject.net.flow.criteria.IPCriterion;
import org.onosproject.net.flow.criteria.MplsBosCriterion;
import org.onosproject.net.flow.criteria.MplsCriterion;
import org.onosproject.net.flow.criteria.PortCriterion;
import org.onosproject.net.flow.criteria.VlanIdCriterion;
import org.onosproject.net.flow.instructions.Instruction;
import org.onosproject.net.flow.instructions.Instructions.OutputInstruction;
import org.onosproject.net.flow.instructions.L2ModificationInstruction;
import org.onosproject.net.flow.instructions.L2ModificationInstruction.L2SubType;
import org.onosproject.net.flow.instructions.L2ModificationInstruction.ModEtherInstruction;
import org.onosproject.net.flow.instructions.L2ModificationInstruction.ModMplsLabelInstruction;
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
    protected static final int DEFAULT_PRIORITY = 0x8000;
    protected static final int LOWEST_PRIORITY = 0x0;

    /*
     * OFDPA requires group-id's to have a certain form.
     * L2 Interface Groups have <4bits-0><12bits-vlanid><16bits-portid>
     * L3 Unicast Groups have <4bits-2><28bits-index>
     * MPLS Interface Groups have <4bits-9><4bits:0><24bits-index>
     * L3 ECMP Groups have <4bits-7><28bits-index>
     * L2 Flood Groups have <4bits-4><12bits-vlanid><16bits-index>
     * L3 VPN Groups have <4bits-9><4bits-2><24bits-index>
     */
    private static final int L2INTERFACEMASK = 0x0;
    private static final int L3UNICASTMASK = 0x20000000;
    private static final int MPLSINTERFACEMASK = 0x90000000;
    private static final int L3ECMPMASK = 0x70000000;
    private static final int L2FLOODMASK = 0x40000000;
    private static final int L3VPNMASK = 0x92000000;

    private final Logger log = getLogger(getClass());
    private ServiceDirectory serviceDirectory;
    protected FlowRuleService flowRuleService;
    private CoreService coreService;
    protected GroupService groupService;
    protected FlowObjectiveStore flowObjectiveStore;
    protected DeviceId deviceId;
    protected ApplicationId driverId;
    protected PacketService packetService;
    protected DeviceService deviceService;
    private InternalPacketProcessor processor = new InternalPacketProcessor();
    protected KryoNamespace appKryo = new KryoNamespace.Builder()
        .register(KryoNamespaces.API)
        .register(GroupKey.class)
        .register(DefaultGroupKey.class)
        .register(OfdpaNextGroup.class)
        .register(byte[].class)
        .register(ArrayDeque.class)
        .build();

    private Cache<GroupKey, OfdpaNextGroup> pendingNextObjectives;
    private ConcurrentHashMap<GroupKey, Set<GroupChainElem>> pendingGroups;

    private ScheduledExecutorService groupChecker =
            Executors.newScheduledThreadPool(2, groupedThreads("onos/pipeliner",
                                                               "ofdpa2-%d"));
    private Set<IPCriterion> sentIpFilters = Collections.newSetFromMap(
                                               new ConcurrentHashMap<IPCriterion, Boolean>());

    // local stores for port-vlan mapping
    Map<PortNumber, VlanId> port2Vlan = new ConcurrentHashMap<PortNumber, VlanId>();
    Map<VlanId, Set<PortNumber>> vlan2Port = new ConcurrentHashMap<VlanId,
                                                        Set<PortNumber>>();

    // index number for group creation
    AtomicInteger l3vpnindex = new AtomicInteger(0);


    @Override
    public void init(DeviceId deviceId, PipelinerContext context) {
        this.serviceDirectory = context.directory();
        this.deviceId = deviceId;

        pendingNextObjectives = CacheBuilder.newBuilder()
                .expireAfterWrite(20, TimeUnit.SECONDS)
                .removalListener((
                     RemovalNotification<GroupKey, OfdpaNextGroup> notification) -> {
                         if (notification.getCause() == RemovalCause.EXPIRED) {
                             fail(notification.getValue().nextObjective(),
                                  ObjectiveError.GROUPINSTALLATIONFAILED);
                         }
                }).build();

        groupChecker.scheduleAtFixedRate(new GroupChecker(), 0, 500, TimeUnit.MILLISECONDS);
        pendingGroups = new ConcurrentHashMap<GroupKey, Set<GroupChainElem>>();

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
            addGroup(nextObjective);
            break;
        case ADD_TO_EXISTING:
            if (nextGroup != null) {
                log.debug("Processing NextObjective id{} in dev{} - add bucket",
                          nextObjective.id(), deviceId);
                addBucketToGroup(nextObjective);
            } else {
                // it is possible that group-chain has not been fully created yet
                waitToAddBucketToGroup(nextObjective);
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
            removeGroup(nextObjective);
            break;
        case REMOVE_FROM_EXISTING:
            if (nextGroup == null) {
                log.warn("Cannot remove from next {} that does not exist in device {}",
                         nextObjective.id(), deviceId);
                return;
            }
            log.debug("Processing NextObjective id{} in dev{} - remove bucket",
                      nextObjective.id(), deviceId);
            removeBucketFromGroup(nextObjective);
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

        VlanId assignedVlan = null;
        if (vidCriterion != null && vidCriterion.vlanId() == VlanId.NONE) {
            // untagged packets are assigned vlans in OF-DPA
            if (filt.meta() == null) {
                log.error("Missing metadata in filtering objective required "
                        + "for vlan assignment in dev {}", deviceId);
                fail(filt, ObjectiveError.BADPARAMS);
                return;
            }
            for (Instruction i : filt.meta().allInstructions()) {
                if (i instanceof ModVlanIdInstruction) {
                    assignedVlan = ((ModVlanIdInstruction) i).vlanId();
                }
            }
            if (assignedVlan == null) {
                log.error("Driver requires an assigned vlan-id to tag incoming "
                        + "untagged packets. Not processing vlan filters on "
                        + "device {}", deviceId);
                fail(filt, ObjectiveError.BADPARAMS);
                return;
            }
        }

        if (ethCriterion == null) {
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

        if (ethCriterion == null || vidCriterion == null) {
            log.debug("filtering objective missing dstMac or vlan, cannot program"
                    + "Vlan Table");
        } else {
            for (FlowRule vlanRule : processVlanIdFilter(portCriterion, vidCriterion,
                                                         assignedVlan,
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
        List<FlowRule> rules = new ArrayList<FlowRule>();
        TrafficSelector.Builder selector = DefaultTrafficSelector.builder();
        TrafficTreatment.Builder treatment = DefaultTrafficTreatment.builder();
        selector.matchVlanId(vidCriterion.vlanId());
        treatment.transition(TMAC_TABLE);

        VlanId storeVlan = null;
        if (vidCriterion.vlanId() == VlanId.NONE) {
            // untagged packets are assigned vlans
            treatment.pushVlan().setVlanId(assignedVlan);
            // XXX ofdpa will require an additional vlan match on the assigned vlan
            // and it may not require the push. This is not in compliance with OF
            // standard. Waiting on what the exact flows are going to look like.
            storeVlan = assignedVlan;
        } else {
            storeVlan = vidCriterion.vlanId();
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

        for (PortNumber pnum : portnums) {
            // update storage
            port2Vlan.put(pnum, storeVlan);
            Set<PortNumber> vlanPorts = vlan2Port.get(storeVlan);
            if (vlanPorts == null) {
                vlanPorts = Collections.newSetFromMap(
                                    new ConcurrentHashMap<PortNumber, Boolean>());
                vlanPorts.add(pnum);
                vlan2Port.put(storeVlan, vlanPorts);
            } else {
                vlanPorts.add(pnum);
            }
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
     * (unicast or multicast) or the L2 table (mac + vlan) or the MPLS table.
     *
     * @param fwd the forwarding objective of type 'specific'
     * @return    a collection of flow rules. Typically there will be only one
     *            for this type of forwarding objective. An empty set may be
     *            returned if there is an issue in processing the objective.
     */
    protected Collection<FlowRule> processSpecific(ForwardingObjective fwd) {
        TrafficSelector selector = fwd.selector();
        EthTypeCriterion ethType =
                (EthTypeCriterion) selector.getCriterion(Criterion.Type.ETH_TYPE);
        if ((ethType == null) ||
                (ethType.ethType().toShort() != Ethernet.TYPE_IPV4) &&
                (ethType.ethType().toShort() != Ethernet.MPLS_UNICAST)) {
            log.warn("processSpecific: Unsupported "
                    + "forwarding objective criteraia");
            fail(fwd, ObjectiveError.UNSUPPORTED);
            return Collections.emptySet();
        }

        int forTableId = -1;
        TrafficSelector.Builder filteredSelector = DefaultTrafficSelector.builder();
        if (ethType.ethType().toShort() == Ethernet.TYPE_IPV4) {
            filteredSelector.matchEthType(Ethernet.TYPE_IPV4)
                .matchIPDst(((IPCriterion)
                        selector.getCriterion(Criterion.Type.IPV4_DST)).ip());
            forTableId = UNICAST_ROUTING_TABLE;
            log.debug("processing IPv4 specific forwarding objective {} in dev:{}",
                      fwd.id(), deviceId);
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
            log.debug("processing MPLS specific forwarding objective {} in dev {}",
                      fwd.id(), deviceId);
        }

        TrafficTreatment.Builder tb = DefaultTrafficTreatment.builder();
        boolean popMpls = false;
        if (fwd.treatment() != null) {
            for (Instruction i : fwd.treatment().allInstructions()) {
                tb.add(i);
                if (i instanceof L2ModificationInstruction &&
                    ((L2ModificationInstruction) i).subtype() == L2SubType.MPLS_POP) {
                        popMpls = true;
                }
            }
        }

        if (fwd.nextId() != null) {
            if (forTableId == MPLS_TABLE_1 && !popMpls) {
                log.warn("SR CONTINUE case cannot be handled as MPLS ECMP "
                        + "is not implemented in OF-DPA yet. Aborting this flow "
                        + "in this device {}", deviceId);
                // XXX We could convert to forwarding to a single-port, via a
                // MPLS interface, or a MPLS SWAP (with-same) but that would
                // have to be handled in the next-objective. Also the pop-mpls
                // logic used here won't work in non-BoS case.
                return Collections.emptySet();
            }

            NextGroup next = flowObjectiveStore.getNextGroup(fwd.nextId());
            List<Deque<GroupKey>> gkeys = appKryo.deserialize(next.data());
            // we only need the top level group's key to point the flow to it
            Group group = groupService.getGroup(deviceId, gkeys.get(0).peekFirst());
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
                .withSelector(filteredSelector.build())
                .withTreatment(tb.build())
                .forTable(forTableId);

        if (fwd.permanent()) {
            ruleBuilder.makePermanent();
        } else {
            ruleBuilder.makeTemporary(fwd.timeout());
        }

        return Collections.singletonList(ruleBuilder.build());
    }

    private void pass(Objective obj) {
        if (obj.context().isPresent()) {
            obj.context().get().onSuccess(obj);
        }
    }

    protected void fail(Objective obj, ObjectiveError error) {
        if (obj.context().isPresent()) {
            obj.context().get().onError(obj, error);
        }
    }

    //////////////////////////////////////
    //  Group handling
    //////////////////////////////////////

    private void addGroup(NextObjective nextObjective) {
        switch (nextObjective.type()) {
        case SIMPLE:
            Collection<TrafficTreatment> treatments = nextObjective.next();
            if (treatments.size() != 1) {
                log.error("Next Objectives of type Simple should only have a "
                        + "single Traffic Treatment. Next Objective Id:{}",
                        nextObjective.id());
               fail(nextObjective, ObjectiveError.BADPARAMS);
               return;
            }
            processSimpleNextObjective(nextObjective);
            break;
        case BROADCAST:
            processBroadcastNextObjective(nextObjective);
            break;
        case HASHED:
            processHashedNextObjective(nextObjective);
            break;
        case FAILOVER:
            fail(nextObjective, ObjectiveError.UNSUPPORTED);
            log.warn("Unsupported next objective type {}", nextObjective.type());
            break;
        default:
            fail(nextObjective, ObjectiveError.UNKNOWN);
            log.warn("Unknown next objective type {}", nextObjective.type());
        }
    }

    /**
     * As per the OFDPA 2.0 TTP, packets are sent out of ports by using
     * a chain of groups. The simple Next Objective passed
     * in by the application has to be broken up into a group chain
     * comprising of an L3 Unicast Group that points to an L2 Interface
     * Group which in-turn points to an output port. In some cases, the simple
     * next Objective can just be an L2 interface without the need for chaining.
     *
     * @param nextObj  the nextObjective of type SIMPLE
     */
    private void processSimpleNextObjective(NextObjective nextObj) {
        // break up simple next objective to GroupChain objects
        TrafficTreatment treatment = nextObj.next().iterator().next();

        GroupInfo groupInfo = createL2L3Chain(treatment, nextObj.id(),
                                              nextObj.appId(), false,
                                              nextObj.meta());
        if (groupInfo == null) {
            log.error("Could not process nextObj={} in dev:{}", nextObj.id(), deviceId);
            return;
        }
        // create object for local and distributed storage
        Deque<GroupKey> gkeyChain = new ArrayDeque<>();
        gkeyChain.addFirst(groupInfo.innerGrpDesc.appCookie());
        gkeyChain.addFirst(groupInfo.outerGrpDesc.appCookie());
        OfdpaNextGroup ofdpaGrp = new OfdpaNextGroup(
                                           Collections.singletonList(gkeyChain),
                                           nextObj);

        // store l3groupkey with the ofdpaGroupChain for the nextObjective that depends on it
        pendingNextObjectives.put(groupInfo.outerGrpDesc.appCookie(), ofdpaGrp);

        // now we are ready to send the l2 groupDescription (inner), as all the stores
        // that will get async replies have been updated. By waiting to update
        // the stores, we prevent nasty race conditions.
        groupService.addGroup(groupInfo.innerGrpDesc);
    }

    /**
     * Creates one of two possible group-chains from the treatment
     * passed in. Depending on the MPLS boolean, this method either creates
     * an L3Unicast Group --> L2Interface Group, if mpls is false;
     * or MPLSInterface Group --> L2Interface Group, if mpls is true;
     * The returned 'inner' group description is always the L2 Interface group.
     *
     * @param treatment that needs to be broken up to create the group chain
     * @param nextId of the next objective that needs this group chain
     * @param appId of the application that sent this next objective
     * @param mpls determines if L3Unicast or MPLSInterface group is created
     * @param meta metadata passed in by the application as part of the nextObjective
     * @return GroupInfo containing the GroupDescription of the
     *         L2Interface group(inner) and the GroupDescription of the (outer)
     *         L3Unicast/MPLSInterface group. May return null if there is an
     *         error in processing the chain
     */
    private GroupInfo createL2L3Chain(TrafficTreatment treatment, int nextId,
                                      ApplicationId appId, boolean mpls,
                                      TrafficSelector meta) {
        // for the l2interface group, get vlan and port info
        // for the outer group, get the src/dst mac, and vlan info
        TrafficTreatment.Builder outerTtb = DefaultTrafficTreatment.builder();
        TrafficTreatment.Builder innerTtb = DefaultTrafficTreatment.builder();
        VlanId vlanid = null;
        long portNum = 0;
        for (Instruction ins : treatment.allInstructions()) {
            if (ins.type() == Instruction.Type.L2MODIFICATION) {
                L2ModificationInstruction l2ins = (L2ModificationInstruction) ins;
                switch (l2ins.subtype()) {
                case ETH_DST:
                    outerTtb.setEthDst(((ModEtherInstruction) l2ins).mac());
                    break;
                case ETH_SRC:
                    outerTtb.setEthSrc(((ModEtherInstruction) l2ins).mac());
                    break;
                case VLAN_ID:
                    vlanid = ((ModVlanIdInstruction) l2ins).vlanId();
                    outerTtb.setVlanId(vlanid);
                    break;
                case VLAN_POP:
                    innerTtb.popVlan();
                    break;
                case DEC_MPLS_TTL:
                case MPLS_LABEL:
                case MPLS_POP:
                case MPLS_PUSH:
                case VLAN_PCP:
                case VLAN_PUSH:
                default:
                    break;
                }
            } else if (ins.type() == Instruction.Type.OUTPUT) {
                portNum = ((OutputInstruction) ins).port().toLong();
                innerTtb.add(ins);
            } else {
                log.warn("Driver does not handle this type of TrafficTreatment"
                        + " instruction in nextObjectives:  {}", ins.type());
            }
        }

        if (vlanid == null) {
            //use the vlanid associated with the port
            vlanid = port2Vlan.get(PortNumber.portNumber(portNum));
        }

        if (vlanid == null) {
            // use metadata
            for (Criterion metaCriterion : meta.criteria()) {
                if (metaCriterion.type() == Type.VLAN_VID) {
                    vlanid = ((VlanIdCriterion) metaCriterion).vlanId();
                }
            }
        }

        if (vlanid == null) {
            log.error("Driver cannot process an L2/L3 group chain without "
                    + "egress vlan information for dev: {} port:{}",
                    deviceId, portNum);
            return null;
        }

        // assemble information for ofdpa l2interface group
        Integer l2groupId = L2INTERFACEMASK | (vlanid.toShort() << 16) | (int) portNum;
        // a globally unique groupkey that is different for ports in the same devices
        // but different for the same portnumber on different devices. Also different
        // for the various group-types created out of the same next objective.
        int l2gk = 0x0ffffff & (deviceId.hashCode() << 8 | (int) portNum);
        final GroupKey l2groupkey = new DefaultGroupKey(appKryo.serialize(l2gk));

        // assemble information for outer group
        GroupDescription outerGrpDesc = null;
        if (mpls) {
            // outer group is MPLSInteface
            Integer mplsgroupId = MPLSINTERFACEMASK | (int) portNum;
            // using mplsinterfacemask in groupkey to differentiate from l2interface
            int mplsgk = MPLSINTERFACEMASK | (0x0ffffff & (deviceId.hashCode() << 8 | (int) portNum));
            final GroupKey mplsgroupkey = new DefaultGroupKey(appKryo.serialize(mplsgk));
            outerTtb.group(new DefaultGroupId(l2groupId));
            // create the mpls-interface group description to wait for the
            // l2 interface group to be processed
            GroupBucket mplsinterfaceGroupBucket =
                    DefaultGroupBucket.createIndirectGroupBucket(outerTtb.build());
            outerGrpDesc = new DefaultGroupDescription(
                                   deviceId,
                                   GroupDescription.Type.INDIRECT,
                                   new GroupBuckets(Collections.singletonList(
                                                        mplsinterfaceGroupBucket)),
                                   mplsgroupkey,
                                   mplsgroupId,
                                   appId);
            log.debug("Trying MPLS-Interface: device:{} gid:{} gkey:{} nextid:{}",
                      deviceId, Integer.toHexString(mplsgroupId),
                      mplsgroupkey, nextId);
        } else {
            // outer group is L3Unicast
            Integer l3groupId = L3UNICASTMASK | (int) portNum;
            int l3gk = L3UNICASTMASK | (0x0ffffff & (deviceId.hashCode() << 8 | (int) portNum));
            final GroupKey l3groupkey = new DefaultGroupKey(appKryo.serialize(l3gk));
            outerTtb.group(new DefaultGroupId(l2groupId));
            // create the l3unicast group description to wait for the
            // l2 interface group to be processed
            GroupBucket l3unicastGroupBucket =
                    DefaultGroupBucket.createIndirectGroupBucket(outerTtb.build());
            outerGrpDesc = new DefaultGroupDescription(
                                   deviceId,
                                   GroupDescription.Type.INDIRECT,
                                   new GroupBuckets(Collections.singletonList(
                                                        l3unicastGroupBucket)),
                                   l3groupkey,
                                   l3groupId,
                                   appId);
            log.debug("Trying L3Unicast: device:{} gid:{} gkey:{} nextid:{}",
                      deviceId, Integer.toHexString(l3groupId),
                      l3groupkey, nextId);
        }

        // store l2groupkey with the groupChainElem for the outer-group that depends on it
        GroupChainElem gce = new GroupChainElem(outerGrpDesc, 1);
        Set<GroupChainElem> gceSet = Collections.newSetFromMap(
                                         new ConcurrentHashMap<GroupChainElem, Boolean>());
        gceSet.add(gce);
        Set<GroupChainElem> retval = pendingGroups.putIfAbsent(l2groupkey, gceSet);
        if (retval != null) {
            retval.add(gce);
        }

        // create group description for the inner l2interfacegroup
        GroupBucket l2interfaceGroupBucket =
                DefaultGroupBucket.createIndirectGroupBucket(innerTtb.build());
        GroupDescription l2groupDescription =
                             new DefaultGroupDescription(
                                     deviceId,
                                     GroupDescription.Type.INDIRECT,
                                     new GroupBuckets(Collections.singletonList(
                                                          l2interfaceGroupBucket)),
                                     l2groupkey,
                                     l2groupId,
                                     appId);
        log.debug("Trying L2Interface: device:{} gid:{} gkey:{} nextId:{}",
                  deviceId, Integer.toHexString(l2groupId),
                  l2groupkey, nextId);
        return new GroupInfo(l2groupDescription, outerGrpDesc);

    }

    /**
     * As per the OFDPA 2.0 TTP, packets are sent out of ports by using
     * a chain of groups. The broadcast Next Objective passed in by the application
     * has to be broken up into a group chain comprising of an
     * L2 Flood group whose buckets point to L2 Interface groups.
     *
     * @param nextObj  the nextObjective of type BROADCAST
     */
    private void processBroadcastNextObjective(NextObjective nextObj) {
        // break up broadcast next objective to multiple groups
        Collection<TrafficTreatment> buckets = nextObj.next();

        // each treatment is converted to an L2 interface group
        VlanId vlanid = null;
        List<GroupDescription> l2interfaceGroupDescs = new ArrayList<>();
        List<Deque<GroupKey>> allGroupKeys = new ArrayList<>();
        for (TrafficTreatment treatment : buckets) {
            TrafficTreatment.Builder newTreatment = DefaultTrafficTreatment.builder();
            PortNumber portNum = null;
            // ensure that the only allowed treatments are pop-vlan and output
            for (Instruction ins : treatment.allInstructions()) {
                if (ins.type() == Instruction.Type.L2MODIFICATION) {
                    L2ModificationInstruction l2ins = (L2ModificationInstruction) ins;
                    switch (l2ins.subtype()) {
                    case VLAN_POP:
                        newTreatment.add(l2ins);
                        break;
                    default:
                        log.debug("action {} not permitted for broadcast nextObj",
                                  l2ins.subtype());
                        break;
                    }
                } else if (ins.type() == Instruction.Type.OUTPUT) {
                    portNum = ((OutputInstruction) ins).port();
                    newTreatment.add(ins);
                } else {
                    log.debug("TrafficTreatment of type {} not permitted in "
                            + " broadcast nextObjective", ins.type());
                }
            }

            // also ensure that all ports are in the same vlan
            VlanId thisvlanid = port2Vlan.get(portNum);
            if (vlanid == null) {
                vlanid = thisvlanid;
            } else {
                if (!vlanid.equals(thisvlanid)) {
                    log.error("Driver requires all ports in a broadcast nextObj "
                            + "to be in the same vlan. Different vlans found "
                            + "{} and {}. Aborting group creation", vlanid, thisvlanid);
                    return;
                }
            }

            // assemble info for l2 interface group
            int l2gk = 0x0ffffff & (deviceId.hashCode() << 8 | (int) portNum.toLong());
            final GroupKey l2groupkey = new DefaultGroupKey(appKryo.serialize(l2gk));
            Integer l2groupId = L2INTERFACEMASK | (vlanid.toShort() << 16) |
                                    (int) portNum.toLong();
            GroupBucket l2interfaceGroupBucket =
                    DefaultGroupBucket.createIndirectGroupBucket(newTreatment.build());
            GroupDescription l2interfaceGroupDescription =
                        new DefaultGroupDescription(
                                                    deviceId,
                            GroupDescription.Type.INDIRECT,
                            new GroupBuckets(Collections.singletonList(
                                                 l2interfaceGroupBucket)),
                            l2groupkey,
                            l2groupId,
                            nextObj.appId());
            log.debug("Trying L2-Interface: device:{} gid:{} gkey:{} nextid:{}",
                      deviceId, Integer.toHexString(l2groupId),
                      l2groupkey, nextObj.id());

            Deque<GroupKey> gkeyChain = new ArrayDeque<>();
            gkeyChain.addFirst(l2groupkey);

            // store the info needed to create this group
            l2interfaceGroupDescs.add(l2interfaceGroupDescription);
            allGroupKeys.add(gkeyChain);
        }

        // assemble info for l2 flood group
        Integer l2floodgroupId = L2FLOODMASK | (vlanid.toShort() << 16) | nextObj.id();
        int l2floodgk = L2FLOODMASK | nextObj.id() << 12;
        final GroupKey l2floodgroupkey = new DefaultGroupKey(appKryo.serialize(l2floodgk));
        // collection of group buckets pointing to all the l2 interface groups
        List<GroupBucket> l2floodBuckets = new ArrayList<>();
        for (GroupDescription l2intGrpDesc : l2interfaceGroupDescs) {
            TrafficTreatment.Builder ttb = DefaultTrafficTreatment.builder();
            ttb.group(new DefaultGroupId(l2intGrpDesc.givenGroupId()));
            GroupBucket abucket = DefaultGroupBucket.createAllGroupBucket(ttb.build());
            l2floodBuckets.add(abucket);
        }
        // create the l2flood group-description to wait for all the
        // l2interface groups to be processed
        GroupDescription l2floodGroupDescription =
                                new DefaultGroupDescription(
                                        deviceId,
                                        GroupDescription.Type.ALL,
                                        new GroupBuckets(l2floodBuckets),
                                        l2floodgroupkey,
                                        l2floodgroupId,
                                        nextObj.appId());
        GroupChainElem gce = new GroupChainElem(l2floodGroupDescription,
                                                l2interfaceGroupDescs.size());
        log.debug("Trying L2-Flood: device:{} gid:{} gkey:{} nextid:{}",
                  deviceId, Integer.toHexString(l2floodgroupId),
                  l2floodgroupkey, nextObj.id());

        // create objects for local and distributed storage
        allGroupKeys.forEach(gkeyChain -> gkeyChain.addFirst(l2floodgroupkey));
        OfdpaNextGroup ofdpaGrp = new OfdpaNextGroup(allGroupKeys, nextObj);

        // store l2floodgroupkey with the ofdpaGroupChain for the nextObjective
        // that depends on it
        pendingNextObjectives.put(l2floodgroupkey, ofdpaGrp);

        for (GroupDescription l2intGrpDesc : l2interfaceGroupDescs) {
            // store all l2groupkeys with the groupChainElem for the l2floodgroup
            // that depends on it
            Set<GroupChainElem> gceSet = Collections.newSetFromMap(
                                             new ConcurrentHashMap<GroupChainElem, Boolean>());
            gceSet.add(gce);
            Set<GroupChainElem> retval = pendingGroups.putIfAbsent(
                                             l2intGrpDesc.appCookie(), gceSet);
            if (retval != null) {
                retval.add(gce);
            }

            // create and send groups for all l2 interface groups
            groupService.addGroup(l2intGrpDesc);
        }
    }

    /**
     * Utility class for moving group information around.
     *
     */
    private class GroupInfo {
        private GroupDescription innerGrpDesc;
        private GroupDescription outerGrpDesc;

        GroupInfo(GroupDescription innerGrpDesc, GroupDescription outerGrpDesc) {
            this.innerGrpDesc = innerGrpDesc;
            this.outerGrpDesc = outerGrpDesc;
        }
    }

    /**
     * As per the OFDPA 2.0 TTP, packets are sent out of ports by using
     * a chain of groups. The hashed Next Objective passed in by the application
     * has to be broken up into a group chain comprising of an
     * L3 ECMP group as the top level group. Buckets of this group can point
     * to a variety of groups in a group chain, depending on the whether
     * MPLS labels are being pushed or not.
     * <p>
     * NOTE: We do not create MPLS ECMP groups as they are unimplemented in
     *       OF-DPA 2.0 (even though it is in the spec). Therefore we do not
     *       check the nextObjective meta.
     *
     * @param nextObj  the nextObjective of type HASHED
     */
    private void processHashedNextObjective(NextObjective nextObj) {
        // break up hashed next objective to multiple groups
        Collection<TrafficTreatment> buckets = nextObj.next();

        // storage for all group keys in the chain of groups created
        List<Deque<GroupKey>> allGroupKeys = new ArrayList<>();
        List<GroupInfo> unsentGroups = new ArrayList<>();
        for (TrafficTreatment bucket : buckets) {
            //figure out how many labels are pushed in each bucket
            int labelsPushed = 0;
            MplsLabel innermostLabel = null;
            for (Instruction ins : bucket.allInstructions()) {
                if (ins.type() == Instruction.Type.L2MODIFICATION) {
                    L2ModificationInstruction l2ins = (L2ModificationInstruction) ins;
                    if (l2ins.subtype() == L2SubType.MPLS_PUSH) {
                        labelsPushed++;
                    }
                    if (l2ins.subtype() == L2SubType.MPLS_LABEL) {
                        if (innermostLabel == null) {
                            innermostLabel = ((ModMplsLabelInstruction) l2ins).mplsLabel();
                        }
                    }
                }
            }

            Deque<GroupKey> gkeyChain = new ArrayDeque<>();
            // XXX we only deal with 0 and 1 label push right now
            if (labelsPushed == 0) {
                GroupInfo nolabelGroupInfo = createL2L3Chain(bucket, nextObj.id(),
                                                             nextObj.appId(), false,
                                                             nextObj.meta());
                if (nolabelGroupInfo == null) {
                    log.error("Could not process nextObj={} in dev:{}",
                              nextObj.id(), deviceId);
                    return;
                }
                gkeyChain.addFirst(nolabelGroupInfo.innerGrpDesc.appCookie());
                gkeyChain.addFirst(nolabelGroupInfo.outerGrpDesc.appCookie());

                // we can't send the inner group description yet, as we have to
                // create the dependent ECMP group first. So we store..
                unsentGroups.add(nolabelGroupInfo);

            } else if (labelsPushed == 1) {
                GroupInfo onelabelGroupInfo = createL2L3Chain(bucket, nextObj.id(),
                                                              nextObj.appId(), true,
                                                              nextObj.meta());
                if (onelabelGroupInfo == null) {
                    log.error("Could not process nextObj={} in dev:{}",
                              nextObj.id(), deviceId);
                    return;
                }
                // we need to add another group to this chain - the L3VPN group
                TrafficTreatment.Builder l3vpnTtb = DefaultTrafficTreatment.builder();
                l3vpnTtb.pushMpls()
                            .setMpls(innermostLabel)
                            .setMplsBos(true)
                            .copyTtlOut()
                            .group(new DefaultGroupId(
                                 onelabelGroupInfo.outerGrpDesc.givenGroupId()));
                GroupBucket l3vpnGrpBkt  =
                        DefaultGroupBucket.createIndirectGroupBucket(l3vpnTtb.build());
                int l3vpngroupId = L3VPNMASK | l3vpnindex.incrementAndGet();
                int l3vpngk = L3VPNMASK | nextObj.id() << 12 | l3vpnindex.get();
                GroupKey l3vpngroupkey = new DefaultGroupKey(appKryo.serialize(l3vpngk));
                GroupDescription l3vpnGroupDesc =
                        new DefaultGroupDescription(
                                deviceId,
                                GroupDescription.Type.INDIRECT,
                                new GroupBuckets(Collections.singletonList(
                                                     l3vpnGrpBkt)),
                                l3vpngroupkey,
                                l3vpngroupId,
                                nextObj.appId());
                GroupChainElem l3vpnGce = new GroupChainElem(l3vpnGroupDesc, 1);
                Set<GroupChainElem> gceSet = Collections.newSetFromMap(
                                                 new ConcurrentHashMap<GroupChainElem, Boolean>());
                gceSet.add(l3vpnGce);
                Set<GroupChainElem> retval = pendingGroups
                        .putIfAbsent(onelabelGroupInfo.outerGrpDesc.appCookie(), gceSet);
                if (retval != null) {
                    retval.add(l3vpnGce);
                }

                gkeyChain.addFirst(onelabelGroupInfo.innerGrpDesc.appCookie());
                gkeyChain.addFirst(onelabelGroupInfo.outerGrpDesc.appCookie());
                gkeyChain.addFirst(l3vpngroupkey);

                //now we can replace the outerGrpDesc with the one we just created
                onelabelGroupInfo.outerGrpDesc = l3vpnGroupDesc;

                // we can't send the innermost group yet, as we have to create
                // the dependent ECMP group first. So we store ...
                unsentGroups.add(onelabelGroupInfo);

                log.debug("Trying L3VPN: device:{} gid:{} gkey:{} nextId:{}",
                          deviceId, Integer.toHexString(l3vpngroupId),
                          l3vpngroupkey, nextObj.id());

            } else {
                log.warn("Driver currently does not handle more than 1 MPLS "
                        + "labels. Not processing nextObjective {}", nextObj);
                return;
            }

            // all groups in this chain
            allGroupKeys.add(gkeyChain);
        }

        // now we can create the outermost L3 ECMP group
        List<GroupBucket> l3ecmpGroupBuckets = new ArrayList<>();
        for (GroupInfo gi : unsentGroups) {
            // create ECMP bucket to point to the outer group
            TrafficTreatment.Builder ttb = DefaultTrafficTreatment.builder();
            ttb.group(new DefaultGroupId(gi.outerGrpDesc.givenGroupId()));
            GroupBucket sbucket = DefaultGroupBucket
                    .createSelectGroupBucket(ttb.build());
            l3ecmpGroupBuckets.add(sbucket);
        }
        int l3ecmpGroupId = L3ECMPMASK | nextObj.id() << 12;
        GroupKey l3ecmpGroupKey = new DefaultGroupKey(appKryo.serialize(l3ecmpGroupId));
        GroupDescription l3ecmpGroupDesc =
                new DefaultGroupDescription(
                        deviceId,
                        GroupDescription.Type.SELECT,
                        new GroupBuckets(l3ecmpGroupBuckets),
                        l3ecmpGroupKey,
                        l3ecmpGroupId,
                        nextObj.appId());
        GroupChainElem l3ecmpGce = new GroupChainElem(l3ecmpGroupDesc,
                                                      l3ecmpGroupBuckets.size());

        // create objects for local and distributed storage
        allGroupKeys.forEach(gkeyChain -> gkeyChain.addFirst(l3ecmpGroupKey));
        OfdpaNextGroup ofdpaGrp = new OfdpaNextGroup(allGroupKeys, nextObj);

        // store l3ecmpGroupKey with the ofdpaGroupChain for the nextObjective
        // that depends on it
        pendingNextObjectives.put(l3ecmpGroupKey, ofdpaGrp);

        log.debug("Trying L3ECMP: device:{} gid:{} gkey:{} nextId:{}",
                  deviceId, Integer.toHexString(l3ecmpGroupId),
                  l3ecmpGroupKey, nextObj.id());
        // finally we are ready to send the innermost groups
        for (GroupInfo gi : unsentGroups) {
            log.debug("Sending innermost group {} in group chain on device {} ",
                      Integer.toHexString(gi.innerGrpDesc.givenGroupId()), deviceId);
            Set<GroupChainElem> gceSet = Collections.newSetFromMap(
                                             new ConcurrentHashMap<GroupChainElem, Boolean>());
            gceSet.add(l3ecmpGce);
            Set<GroupChainElem> retval = pendingGroups
                    .putIfAbsent(gi.outerGrpDesc.appCookie(), gceSet);
            if (retval != null) {
                retval.add(l3ecmpGce);
            }

            groupService.addGroup(gi.innerGrpDesc);
        }

    }

    private void addBucketToGroup(NextObjective nextObjective) {
        // TODO Auto-generated method stub
    }

    private void waitToAddBucketToGroup(NextObjective nextObjective) {
        // TODO Auto-generated method stub
    }

    private void removeBucketFromGroup(NextObjective nextObjective) {
        // TODO Auto-generated method stub
    }

    private void removeGroup(NextObjective nextObjective) {
        // TODO Auto-generated method stub
    }

    /**
     * Processes next element of a group chain. Assumption is that if this
     * group points to another group, the latter has already been created
     * and this driver has received notification for it. A second assumption is
     * that if there is another group waiting for this group then the appropriate
     * stores already have the information to act upon the notification for the
     * creating of this group.
     * <p>
     * The processing of the GroupChainElement depends on the number of groups
     * this element is waiting on. For all group types other than SIMPLE, a
     * GroupChainElement could be waiting on multiple groups.
     *
     * @param gce the group chain element to be processed next
     */
    private void processGroupChain(GroupChainElem gce) {
        int waitOnGroups = gce.decrementAndGetGroupsWaitedOn();
        if (waitOnGroups != 0) {
            log.debug("GCE: {} not ready to be processed", gce);
            return;
        }
        log.debug("GCE: {} ready to be processed", gce);
        groupService.addGroup(gce.groupDescription);
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
                Set<GroupChainElem> gceSet = pendingGroups.remove(key);
                if (gceSet != null) {
                    for (GroupChainElem gce : gceSet) {
                        log.info("Group service processed group key {} in device {}. "
                                + "Processing next group in group chain with group id {}",
                                key, deviceId,
                                Integer.toHexString(gce.groupDescription.givenGroupId()));
                        processGroupChain(gce);
                    }
                } else {
                    OfdpaNextGroup obj = pendingNextObjectives.getIfPresent(key);
                    if (obj != null) {
                        log.info("Group service processed group key {} in device:{}. "
                                + "Done implementing next objective: {} <<-->> gid:{}",
                                key, deviceId, obj.nextObjective().id(),
                                Integer.toHexString(groupService.getGroup(deviceId, key)
                                                    .givenGroupId()));
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
            log.trace("received group event of type {}", event.type());
            if (event.type() == GroupEvent.Type.GROUP_ADDED) {
                GroupKey key = event.subject().appCookie();
                // first check for group chain
                Set<GroupChainElem> gceSet = pendingGroups.remove(key);
                if (gceSet != null) {
                    for (GroupChainElem gce : gceSet) {
                        log.info("group ADDED with group key {} .. "
                                + "Processing next group in group chain with group key {}",
                                key,
                                gce.groupDescription.appCookie());
                        processGroupChain(gce);
                    }
                } else {
                    OfdpaNextGroup obj = pendingNextObjectives.getIfPresent(key);
                    if (obj != null) {
                        log.info("group ADDED with key {} in dev {}.. Done implementing next "
                                + "objective: {} <<-->> gid:{}",
                                key, deviceId, obj.nextObjective().id(),
                                Integer.toHexString(groupService.getGroup(deviceId, key)
                                                    .givenGroupId()));
                        pass(obj.nextObjective());
                        pendingNextObjectives.invalidate(key);
                        flowObjectiveStore.putNextGroup(obj.nextObjective().id(), obj);
                    }
                }
            }
        }
    }

    /**
     * Represents an entire group-chain that implements a Next-Objective from
     * the application. The objective is represented as a list of deques, where
     * each deque can is a separate chain of groups.
     * <p>
     * For example, an ECMP group with 3 buckets, where each bucket points to
     * a group chain of L3 Unicast and L2 interface groups will look like this:
     * <ul>
     * <li>List[0] is a Deque of GroupKeyECMP(first)-GroupKeyL3(middle)-GroupKeyL2(last)
     * <li>List[1] is a Deque of GroupKeyECMP(first)-GroupKeyL3(middle)-GroupKeyL2(last)
     * <li>List[2] is a Deque of GroupKeyECMP(first)-GroupKeyL3(middle)-GroupKeyL2(last)
     * </ul>
     * where the first element of each deque is the same, representing the
     * top level ECMP group, while every other element represents a unique groupKey.
     * <p>
     * Also includes information about the next objective that
     * resulted in this group-chain.
     *
     */
    private class OfdpaNextGroup implements NextGroup {
        private final NextObjective nextObj;
        private final List<Deque<GroupKey>> gkeys;

        public OfdpaNextGroup(List<Deque<GroupKey>> gkeys, NextObjective nextObj) {
            this.gkeys = gkeys;
            this.nextObj = nextObj;
        }

        @SuppressWarnings("unused")
        public List<Deque<GroupKey>> groupKey() {
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
        private GroupDescription groupDescription;
        private AtomicInteger waitOnGroups;

        GroupChainElem(GroupDescription groupDescription, int waitOnGroups) {
            this.groupDescription = groupDescription;
            this.waitOnGroups = new AtomicInteger(waitOnGroups);
        }

        /**
         * This methods atomically decrements the counter for the number of
         * groups this GroupChainElement is waiting on, for notifications from
         * the Group Service. When this method returns a value of 0, this
         * GroupChainElement is ready to be processed.
         *
         * @return integer indication of the number of notifications being waited on
         */
        int decrementAndGetGroupsWaitedOn() {
            return waitOnGroups.decrementAndGet();
        }

        @Override
        public String toString() {
            return (Integer.toHexString(groupDescription.givenGroupId()) +
                    " groupKey: " + groupDescription.appCookie() +
                    " waiting-on-groups: " + waitOnGroups.get() +
                    " device: " + deviceId);
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

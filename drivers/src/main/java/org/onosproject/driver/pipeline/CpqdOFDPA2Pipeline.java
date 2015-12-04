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

import static org.slf4j.LoggerFactory.getLogger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.onlab.packet.Ethernet;
import org.onlab.packet.VlanId;
import org.onosproject.core.ApplicationId;
import org.onosproject.net.Port;
import org.onosproject.net.PortNumber;
import org.onosproject.net.behaviour.NextGroup;
import org.onosproject.net.flow.DefaultFlowRule;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.FlowRule;
import org.onosproject.net.flow.FlowRuleOperations;
import org.onosproject.net.flow.FlowRuleOperationsContext;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.flow.criteria.Criteria;
import org.onosproject.net.flow.criteria.Criterion;
import org.onosproject.net.flow.criteria.EthCriterion;
import org.onosproject.net.flow.criteria.EthTypeCriterion;
import org.onosproject.net.flow.criteria.IPCriterion;
import org.onosproject.net.flow.criteria.MplsBosCriterion;
import org.onosproject.net.flow.criteria.MplsCriterion;
import org.onosproject.net.flow.criteria.PortCriterion;
import org.onosproject.net.flow.criteria.VlanIdCriterion;
import org.onosproject.net.flow.instructions.Instruction;
import org.onosproject.net.flowobjective.ForwardingObjective;
import org.onosproject.net.flowobjective.ObjectiveError;
import org.onosproject.net.group.Group;
import org.onosproject.net.group.GroupKey;
import org.slf4j.Logger;


/**
 * Driver for software switch emulation of the OFDPA 2.0 pipeline.
 * The software switch is the CPqD OF 1.3 switch.
 */
public class CpqdOFDPA2Pipeline extends OFDPA2Pipeline {

    private final Logger log = getLogger(getClass());

    /*
     * Cpqd emulation does not require the non-OF standard rules for
     * matching untagged packets.
     *
     * (non-Javadoc)
     * @see org.onosproject.driver.pipeline.OFDPA2Pipeline#processVlanIdFilter
     */
    @Override
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

    /*
     * Cpqd emulation does not handle vlan tags and mpls labels correctly.
     * Workaround requires popping off the VLAN tags in the TMAC table.
     *
     * (non-Javadoc)
     * @see org.onosproject.driver.pipeline.OFDPA2Pipeline#processEthDstFilter
     */
    @Override
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
            /*
             * Note: CpqD switches do not handle MPLS-related operation properly
             * for a packet with VLAN tag. We pop VLAN here as a workaround.
             * Side effect: HostService learns redundant hosts with same MAC but
             * different VLAN. No known side effect on the network reachability.
             */
            treatment.popVlan();
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
            // workaround here again
            treatment.popVlan();
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

    /*
     * Cpqd emulation allows MPLS ecmp.
     *
     * (non-Javadoc)
     * @see org.onosproject.driver.pipeline.OFDPA2Pipeline#processEthTypeSpecific
     */
    @Override
    protected Collection<FlowRule> processEthTypeSpecific(ForwardingObjective fwd) {
        TrafficSelector selector = fwd.selector();
        EthTypeCriterion ethType =
                (EthTypeCriterion) selector.getCriterion(Criterion.Type.ETH_TYPE);
        if ((ethType == null) ||
                (ethType.ethType().toShort() != Ethernet.TYPE_IPV4) &&
                (ethType.ethType().toShort() != Ethernet.MPLS_UNICAST)) {
            log.warn("processSpecific: Unsupported forwarding objective criteria"
                    + "ethType:{} in dev:{}", ethType, deviceId);
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
            log.debug("processing IPv4 specific forwarding objective {} -> next:{}"
                    + " in dev:{}", fwd.id(), fwd.nextId(), deviceId);
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
        }

        TrafficTreatment.Builder tb = DefaultTrafficTreatment.builder();
        if (fwd.treatment() != null) {
            for (Instruction i : fwd.treatment().allInstructions()) {
                /*
                 * NOTE: OF-DPA does not support immediate instruction in
                 * L3 unicast and MPLS table.
                 */
                tb.deferred().add(i);
            }
        }

        if (fwd.nextId() != null) {
            NextGroup next = getGroupForNextObjective(fwd.nextId());
            if (next != null) {
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

    @Override
    protected void initializePipeline() {
        processPortTable();
        // vlan table processing not required, as default is to drop packets
        // which can be accomplished without a table-miss-entry.
        processTmacTable();
        processIpTable();
        processMplsTable();
        processBridgingTable();
        processAclTable();
    }

    protected void processPortTable() {
        FlowRuleOperations.Builder ops = FlowRuleOperations.builder();
        TrafficSelector.Builder selector = DefaultTrafficSelector.builder();
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

    protected void processTmacTable() {
        //table miss entry
        FlowRuleOperations.Builder ops = FlowRuleOperations.builder();
        TrafficSelector.Builder selector = DefaultTrafficSelector.builder();
        TrafficTreatment.Builder treatment = DefaultTrafficTreatment.builder();
        selector = DefaultTrafficSelector.builder();
        treatment = DefaultTrafficTreatment.builder();
        treatment.transition(BRIDGING_TABLE);
        FlowRule rule = DefaultFlowRule.builder()
                .forDevice(deviceId)
                .withSelector(selector.build())
                .withTreatment(treatment.build())
                .withPriority(LOWEST_PRIORITY)
                .fromApp(driverId)
                .makePermanent()
                .forTable(TMAC_TABLE).build();
        ops =  ops.add(rule);
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
        selector = DefaultTrafficSelector.builder();
        treatment = DefaultTrafficTreatment.builder();
        treatment.deferred().setOutput(PortNumber.CONTROLLER);
        treatment.transition(ACL_TABLE);
        FlowRule rule = DefaultFlowRule.builder()
                .forDevice(deviceId)
                .withSelector(selector.build())
                .withTreatment(treatment.build())
                .withPriority(LOWEST_PRIORITY)
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

    protected void processMplsTable() {
        //table miss entry
        FlowRuleOperations.Builder ops = FlowRuleOperations.builder();
        TrafficSelector.Builder selector = DefaultTrafficSelector.builder();
        TrafficTreatment.Builder treatment = DefaultTrafficTreatment.builder();
        selector = DefaultTrafficSelector.builder();
        treatment = DefaultTrafficTreatment.builder();
        treatment.transition(MPLS_TABLE_1);
        FlowRule rule = DefaultFlowRule.builder()
                .forDevice(deviceId)
                .withSelector(selector.build())
                .withTreatment(treatment.build())
                .withPriority(LOWEST_PRIORITY)
                .fromApp(driverId)
                .makePermanent()
                .forTable(MPLS_TABLE_0).build();
        ops =  ops.add(rule);

        treatment.transition(ACL_TABLE);
        rule = DefaultFlowRule.builder()
                .forDevice(deviceId)
                .withSelector(selector.build())
                .withTreatment(treatment.build())
                .withPriority(LOWEST_PRIORITY)
                .fromApp(driverId)
                .makePermanent()
                .forTable(MPLS_TABLE_1).build();
        ops = ops.add(rule);

        flowRuleService.apply(ops.build(new FlowRuleOperationsContext() {
            @Override
            public void onSuccess(FlowRuleOperations ops) {
                log.info("Initialized MPLS tables");
            }

            @Override
            public void onError(FlowRuleOperations ops) {
                log.info("Failed to initialize MPLS tables");
            }
        }));
    }

    private void processBridgingTable() {
        //table miss entry
        FlowRuleOperations.Builder ops = FlowRuleOperations.builder();
        TrafficSelector.Builder selector = DefaultTrafficSelector.builder();
        TrafficTreatment.Builder treatment = DefaultTrafficTreatment.builder();
        selector = DefaultTrafficSelector.builder();
        treatment = DefaultTrafficTreatment.builder();
        treatment.transition(ACL_TABLE);
        FlowRule rule = DefaultFlowRule.builder()
                .forDevice(deviceId)
                .withSelector(selector.build())
                .withTreatment(treatment.build())
                .withPriority(LOWEST_PRIORITY)
                .fromApp(driverId)
                .makePermanent()
                .forTable(BRIDGING_TABLE).build();
        ops =  ops.add(rule);
        flowRuleService.apply(ops.build(new FlowRuleOperationsContext() {
            @Override
            public void onSuccess(FlowRuleOperations ops) {
                log.info("Initialized Bridging table");
            }

            @Override
            public void onError(FlowRuleOperations ops) {
                log.info("Failed to initialize Bridging table");
            }
        }));
    }

    protected void processAclTable() {
        //table miss entry - catch all to executed action-set
        FlowRuleOperations.Builder ops = FlowRuleOperations.builder();
        TrafficSelector.Builder selector = DefaultTrafficSelector.builder();
        TrafficTreatment.Builder treatment = DefaultTrafficTreatment.builder();
        selector = DefaultTrafficSelector.builder();
        treatment = DefaultTrafficTreatment.builder();
        FlowRule rule = DefaultFlowRule.builder()
                .forDevice(deviceId)
                .withSelector(selector.build())
                .withTreatment(treatment.build())
                .withPriority(LOWEST_PRIORITY)
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

}

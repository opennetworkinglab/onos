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

package org.onosproject.drivers.corsa;

import com.google.common.collect.ImmutableSet;
import org.onlab.packet.Ethernet;
import org.onlab.packet.IPv4;
import org.onlab.packet.VlanId;
import org.onosproject.net.flow.DefaultFlowRule;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.FlowRule;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.flow.criteria.Criterion;
import org.onosproject.net.flow.criteria.IPCriterion;
import org.onosproject.net.flow.criteria.IPProtocolCriterion;
import org.onosproject.net.flow.instructions.Instruction;
import org.onosproject.net.flow.instructions.Instructions;
import org.onosproject.net.flow.instructions.L2ModificationInstruction;
import org.onosproject.net.flowobjective.ForwardingObjective;
import org.onosproject.net.flowobjective.ObjectiveError;
import org.slf4j.Logger;

import java.util.Collection;
import java.util.Collections;

import static org.onosproject.net.flow.FlowRule.Builder;
import static org.onosproject.net.flow.instructions.L2ModificationInstruction.L2SubType.ETH_DST;
import static org.onosproject.net.flow.instructions.L2ModificationInstruction.L2SubType.ETH_SRC;
import static org.onosproject.net.flow.instructions.L2ModificationInstruction.L2SubType.VLAN_ID;
import static org.onosproject.net.flow.instructions.L2ModificationInstruction.L2SubType.VLAN_POP;
import static org.slf4j.LoggerFactory.getLogger;

public class CorsaPipelineV39 extends CorsaPipelineV3 {

    private final Logger log = getLogger(getClass());

    private static final Short NATIVE_VLAN = 4095;

    @Override
    public void initializePipeline() {

        processMeterTable(true);           //Meter Table
        processPortBasedProtoTable(true);
        processVlanCheckTable(true);       //Table 1
        processVlanMacXlateTable(true);    //Table 2
        processVlanCircuitTable(true);     //Table 3
        processL3IFMacDATable(true);       //Table 5
        processEtherTable(true);           //Table 6
        //TODO: to be implemented for intents
        processFibTable(true);           //Table 7
        //processLocalTable(true);         //Table 9
    }

    @Override
    protected void processVlanCheckTable(boolean install) {
        //current device pipeline reports errors, but it is a bug
        processTableMissGoTo(true, VLAN_CHECK_TABLE, VLAN_MAC_XLATE_TABLE, "Provisioned vlan tagged");
        //Tag untagged packets
        processUntaggedPackets(install);

    }

    private void processUntaggedPackets(boolean install) {

        deviceService.getPorts(deviceId).forEach(port -> {
            if (!port.number().isLogical()) {

                TrafficTreatment.Builder treatment = DefaultTrafficTreatment.builder()
                        .pushVlan().setVlanId(VlanId.vlanId(NATIVE_VLAN))
                        .transition(VLAN_MAC_XLATE_TABLE);

                TrafficSelector.Builder selector = DefaultTrafficSelector.builder()
                        .matchVlanId(VlanId.NONE)
                        .matchInPort(port.number());

                Builder rule = DefaultFlowRule.builder()
                        .forDevice(deviceId)
                        .withTreatment(treatment.build())
                        .withSelector(selector.build())
                        .withPriority(CONTROLLER_PRIORITY)
                        .fromApp(appId)
                        .makePermanent()
                        .forTable(VLAN_CHECK_TABLE);

                processFlowRule(install, rule.build(), "Provisioned vlan untagged packet table");
            }
        });
    }

    @Override
    protected void processVlanCircuitTable(boolean install) {
        //Default action
        processTableMissDrop(install, VLAN_CIRCUIT_TABLE, "Provisioned vlan circuit table drop");
        //FIXME: it should be done only per port based when intent is installed
        //Manage untagged packets
        processRouterPacket(install);
    }

    private void processRouterPacket(boolean install) {

        deviceService.getPorts(deviceId).forEach(port -> {
            if (!port.number().isLogical()) {
                TrafficSelector.Builder selector = DefaultTrafficSelector.builder()
                        .matchVlanId(VlanId.vlanId(NATIVE_VLAN))
                        .matchInPort(port.number());

                TrafficTreatment.Builder treatment = DefaultTrafficTreatment.builder()
                        .setVlanPcp((byte) 0)
                        .setQueue(0)
                        .meter(defaultMeterId)
                        .transition(L3_IF_MAC_DA_TABLE);

                FlowRule rule = DefaultFlowRule.builder()
                        .forDevice(deviceId)
                        .withSelector(selector.build())
                        .withTreatment(treatment.build())
                        .withPriority(CONTROLLER_PRIORITY)
                        .fromApp(appId)
                        .makePermanent()
                        .forTable(VLAN_CIRCUIT_TABLE).build();
                processFlowRule(install, rule, "Provisioned vlan circuit table");
            }
        });
    }

    @Override
    protected void processL3IFMacDATable(boolean install) {
        int table = L3_IF_MAC_DA_TABLE;

        //Default action
        processTableMissDrop(install, table, "Provisioned l3 table drop");

        TrafficSelector.Builder selector = DefaultTrafficSelector.builder();

        TrafficTreatment.Builder treatment = DefaultTrafficTreatment.builder()
                .transition(ETHER_TABLE);

        FlowRule rule = DefaultFlowRule.builder()
                .forDevice(deviceId)
                .withSelector(selector.build())
                .withTreatment(treatment.build())
                .withPriority(1)
                .fromApp(appId)
                .makePermanent()
                .forTable(table).build();
        processFlowRule(install, rule, "Provisioned l3 table");
    }

    protected void processEtherTable(boolean install) {

        //Default action
        processTableMissDrop(install, ETHER_TABLE, "Provisioned ether type table drop");

        //IP to FIB_TABLE
        TrafficSelector.Builder selector = DefaultTrafficSelector.builder()
                .matchEthType(Ethernet.TYPE_IPV4);

        TrafficTreatment.Builder treatment = DefaultTrafficTreatment.builder().transition(FIB_TABLE);

        FlowRule rule = DefaultFlowRule.builder()
                .forDevice(deviceId)
                .withSelector(selector.build())
                .withTreatment(treatment.build())
                .withPriority(CONTROLLER_PRIORITY)
                .fromApp(appId)
                .makePermanent()
                .forTable(ETHER_TABLE).build();
        processFlowRule(install, rule, "Provisioned ether type table ip");
    }

    @Override
    protected Collection<FlowRule> processArpTraffic(ForwardingObjective fwd, Builder rule) {
        rule.forTable(PORT_BASED_PROTO_TABLE);
        rule.withPriority(255);
        return Collections.singletonList(rule.build());
    }

    @Override
    protected Collection<FlowRule> processLinkDiscovery(ForwardingObjective fwd, Builder rule) {
        rule.forTable(PORT_BASED_PROTO_TABLE);
        rule.withPriority(255);
        return Collections.singletonList(rule.build());
    }

    @Override
    protected Collection<FlowRule> processIpTraffic(ForwardingObjective fwd, Builder rule) {
        IPCriterion ipSrc = (IPCriterion) fwd.selector()
                .getCriterion(Criterion.Type.IPV4_SRC);
        if (ipSrc != null) {
            log.warn("Driver does not currently handle matching Src IP");
            fail(fwd, ObjectiveError.UNSUPPORTED);
            return ImmutableSet.of();
        }
        IPCriterion ipDst = (IPCriterion) fwd.selector()
                .getCriterion(Criterion.Type.IPV4_DST);
        if (ipDst != null) {
            log.error("Driver handles Dst IP matching as specific forwarding "
                    + "objective, not versatile");
            fail(fwd, ObjectiveError.UNSUPPORTED);
            return ImmutableSet.of();
        }
        IPProtocolCriterion ipProto = (IPProtocolCriterion) fwd.selector()
                .getCriterion(Criterion.Type.IP_PROTO);
        if (ipProto != null && ipProto.protocol() == IPv4.PROTOCOL_TCP) {
            log.warn("Driver automatically punts all packets reaching the "
                    + "LOCAL table to the controller");
            pass(fwd);
            return ImmutableSet.of();
        }
        return ImmutableSet.of();
    }

    @Override
    protected CorsaTrafficTreatment processNextTreatment(TrafficTreatment treatment) {
        TrafficTreatment.Builder tb = DefaultTrafficTreatment.builder();
        treatment.immediate().stream()
                .filter(i -> {
                    switch (i.type()) {
                        case L2MODIFICATION:
                            L2ModificationInstruction l2i = (L2ModificationInstruction) i;
                            return l2i.subtype() == VLAN_ID ||
                                    l2i.subtype() == VLAN_POP ||
                                    l2i.subtype() == ETH_DST ||
                                    l2i.subtype() == ETH_SRC;
                        case OUTPUT:
                            return true;
                        default:
                            return false;
                    }
                }).forEach(i -> tb.add(i));

        TrafficTreatment t = tb.build();

        boolean isPresentModVlanId = false;
        boolean isPresentModEthSrc = false;
        boolean isPresentModEthDst = false;
        boolean isPresentOutpuPort = false;

        for (Instruction instruction : t.immediate()) {
            switch (instruction.type()) {
                case L2MODIFICATION:
                    L2ModificationInstruction l2i = (L2ModificationInstruction) instruction;
                    if (l2i instanceof L2ModificationInstruction.ModVlanIdInstruction) {
                        isPresentModVlanId = true;
                    }

                    if (l2i instanceof L2ModificationInstruction.ModEtherInstruction) {
                        L2ModificationInstruction.L2SubType subType = l2i.subtype();
                        if (subType.equals(L2ModificationInstruction.L2SubType.ETH_SRC)) {
                            isPresentModEthSrc = true;
                        } else if (subType.equals(L2ModificationInstruction.L2SubType.ETH_DST)) {
                            isPresentModEthDst = true;
                        }
                    }
                    break;
                case OUTPUT:
                    isPresentOutpuPort = true;
                    break;
                default:
            }
        }
        CorsaTrafficTreatmentType type = CorsaTrafficTreatmentType.ACTIONS;
        /**
         * These are the allowed groups for CorsaPipelinev39
         */
        if (isPresentModVlanId && isPresentModEthSrc && isPresentModEthDst && isPresentOutpuPort) {
            type = CorsaTrafficTreatmentType.GROUP;

        } else if ((!isPresentModVlanId && isPresentModEthSrc && isPresentModEthDst && isPresentOutpuPort) ||
                (!isPresentModVlanId && !isPresentModEthSrc && isPresentModEthDst && isPresentOutpuPort) ||
                (!isPresentModVlanId && !isPresentModEthSrc && !isPresentModEthDst && isPresentOutpuPort)) {
            type = CorsaTrafficTreatmentType.GROUP;
            TrafficTreatment.Builder tb2 = DefaultTrafficTreatment.builder(t);
            tb2.add(Instructions.popVlan());
            t = tb2.build();
        }
        CorsaTrafficTreatment corsaTreatment = new CorsaTrafficTreatment(type, t);
        return corsaTreatment;
    }
}

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
package org.onosproject.drivers.corsa;

import com.google.common.collect.ImmutableSet;
import org.onlab.packet.Ethernet;
import org.onlab.packet.MacAddress;
import org.onlab.packet.VlanId;
import org.onosproject.net.flow.DefaultFlowRule;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.FlowRule;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.flow.criteria.Criterion;
import org.onosproject.net.flow.criteria.EthCriterion;
import org.onosproject.net.flow.criteria.IPCriterion;
import org.onosproject.net.flow.criteria.PortCriterion;
import org.onosproject.net.flow.criteria.VlanIdCriterion;
import org.onosproject.net.flow.instructions.Instruction;
import org.onosproject.net.flow.instructions.L2ModificationInstruction;
import org.onosproject.net.flowobjective.FilteringObjective;
import org.onosproject.net.flowobjective.ForwardingObjective;
import org.onosproject.net.flowobjective.NextObjective;
import org.onosproject.net.flowobjective.ObjectiveError;
import org.onosproject.net.meter.Band;
import org.onosproject.net.meter.DefaultBand;
import org.onosproject.net.meter.DefaultMeterRequest;
import org.onosproject.net.meter.Meter;
import org.onosproject.net.meter.MeterId;
import org.onosproject.net.meter.MeterRequest;
import org.slf4j.Logger;

import java.util.Collection;
import java.util.Collections;

import static org.onosproject.net.flow.FlowRule.Builder;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Implementation of the Corsa pipeline handler for pipeline version 3.
 */
public class CorsaPipelineV3 extends AbstractCorsaPipeline {

    private final Logger log = getLogger(getClass());

    protected static final int PORT_BASED_PROTO_TABLE = 0;
    protected static final int VLAN_CHECK_TABLE = 1;
    protected static final int VLAN_MAC_XLATE_TABLE = 2;
    protected static final int VLAN_CIRCUIT_TABLE = 3;
    protected static final int PRIORITY_MAP_TABLE = 4;
    protected static final int L3_IF_MAC_DA_TABLE = 5;
    protected static final int ETHER_TABLE = 6;
    protected static final int FIB_TABLE = 7;
    protected static final int LOCAL_TABLE = 9;

    protected static final byte MAX_VLAN_PCP = 7;

    protected MeterId defaultMeterId = null;

    @Override
    protected CorsaTrafficTreatment processNextTreatment(TrafficTreatment treatment) {
        TrafficTreatment.Builder tb = DefaultTrafficTreatment.builder();



        treatment.immediate().stream()
                .filter(i -> {
                    switch (i.type()) {
                        case L2MODIFICATION:
                            L2ModificationInstruction l2i = (L2ModificationInstruction) i;
                            if (l2i instanceof L2ModificationInstruction.ModVlanIdInstruction ||
                                    l2i instanceof L2ModificationInstruction.ModEtherInstruction) {
                                return true;
                            }
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
                case OUTPUT:
                    isPresentOutpuPort = true;
                default:
            }
        }
        CorsaTrafficTreatmentType type = CorsaTrafficTreatmentType.ACTIONS;
        /**
         * This represents the allowed group for CorsaPipelinev3
         */
        if (isPresentModVlanId &&
                isPresentModEthSrc &&
                isPresentModEthDst &&
                isPresentOutpuPort) {
            type = CorsaTrafficTreatmentType.GROUP;
        }
        CorsaTrafficTreatment corsaTreatment = new CorsaTrafficTreatment(type, t);
        return corsaTreatment;
    }

    @Override
    protected TrafficTreatment.Builder processSpecificRoutingTreatment() {
        return DefaultTrafficTreatment.builder().deferred();
    }

    @Override
    protected Builder processSpecificRoutingRule(Builder rb) {
        return rb.forTable(FIB_TABLE);
    }

    @Override
    protected Collection<FlowRule> processSpecificSwitch(ForwardingObjective fwd) {
        TrafficSelector filteredSelector =
                DefaultTrafficSelector.builder()
                        .matchInPort(
                                ((PortCriterion) fwd.selector().getCriterion(Criterion.Type.IN_PORT)).port())
                        .matchVlanId(
                                ((VlanIdCriterion) fwd.selector().getCriterion(Criterion.Type.VLAN_VID)).vlanId())
                        .build();

        Builder ruleBuilder = DefaultFlowRule.builder()
                .fromApp(fwd.appId())
                .withPriority(fwd.priority())
                .forDevice(deviceId)
                .withSelector(filteredSelector)
                .forTable(VLAN_CIRCUIT_TABLE);

        if (fwd.treatment() != null) {
            ruleBuilder.withTreatment(fwd.treatment());
        } else {
            if (fwd.nextId() != null) {
                NextObjective nextObjective = pendingNext.getIfPresent(fwd.nextId());
                if (nextObjective != null) {
                    pendingNext.invalidate(fwd.nextId());
                    TrafficTreatment.Builder treatment = DefaultTrafficTreatment.builder()
                            .setVlanPcp((byte) 0)
                            .setQueue(0)
                            .meter(defaultMeterId);
                    nextObjective.next().forEach(trafficTreatment -> {
                        trafficTreatment.allInstructions().forEach(instruction -> {
                           treatment.add(instruction);
                        });
                    });
                    ruleBuilder.withTreatment(treatment.build());
                } else {
                    log.warn("The group left!");
                    fwd.context().ifPresent(c -> c.onError(fwd, ObjectiveError.GROUPMISSING));
                    return ImmutableSet.of();
                }
            } else {
                log.warn("Missing NextObjective ID for ForwardingObjective {}", fwd.id());
                fail(fwd, ObjectiveError.BADPARAMS);
                return ImmutableSet.of();
            }
        }

        if (fwd.permanent()) {
            ruleBuilder.makePermanent();
        } else {
            ruleBuilder.makeTemporary(fwd.timeout());
        }

        return Collections.singletonList(ruleBuilder.build());
    }

    @Override
    protected Collection<FlowRule> processArpTraffic(ForwardingObjective fwd, Builder rule) {
        //TODO
        return ImmutableSet.of();
    }

    @Override
    protected Collection<FlowRule> processLinkDiscovery(ForwardingObjective fwd, Builder rule) {
        //TODO
        return ImmutableSet.of();
    }

    @Override
    protected Collection<FlowRule> processIpTraffic(ForwardingObjective fwd, Builder rule) {
        //TODO
        return ImmutableSet.of();
    }

    @Override
    protected Builder processEthFiler(FilteringObjective filt, EthCriterion eth, PortCriterion port) {
        log.debug("adding rule for MAC: {}", eth.mac());
        TrafficSelector.Builder selector = DefaultTrafficSelector.builder();
        TrafficTreatment.Builder treatment = DefaultTrafficTreatment.builder();
        selector.matchEthDst(eth.mac());
        selector.matchInPort(port.port());
        treatment.transition(ETHER_TABLE);
        return DefaultFlowRule.builder()
                .withSelector(selector.build())
                .withTreatment(treatment.build())
                .withPriority(CONTROLLER_PRIORITY)
                .makePermanent()
                .forTable(L3_IF_MAC_DA_TABLE);
    }

    @Override
    protected Builder processVlanFiler(FilteringObjective filt, VlanIdCriterion vlan, PortCriterion port) {
        log.debug("adding rule for VLAN: {}", vlan.vlanId());
        TrafficSelector.Builder selector = DefaultTrafficSelector.builder();
        TrafficTreatment.Builder treatment = DefaultTrafficTreatment.builder();
        selector.matchVlanId(vlan.vlanId());
        selector.matchInPort(port.port());
                /* Static treatment for VLAN_CIRCUIT_TABLE */
        treatment.setVlanPcp(MAX_VLAN_PCP);
        treatment.setQueue(0);
        treatment.meter(MeterId.meterId(defaultMeterId.id())); /* use default meter (Green) */
        treatment.transition(L3_IF_MAC_DA_TABLE);
        return DefaultFlowRule.builder()
                .withSelector(selector.build())
                .withTreatment(treatment.build())
                .withPriority(CONTROLLER_PRIORITY)
                .makePermanent()
                .forTable(VLAN_CIRCUIT_TABLE);
    }

    @Override
    protected Builder processIpFilter(FilteringObjective filt, IPCriterion ip, PortCriterion port) {
        log.debug("adding rule for IP: {}", ip.ip());
        TrafficSelector.Builder selector = DefaultTrafficSelector.builder();
        TrafficTreatment.Builder treatment = DefaultTrafficTreatment.builder();
        selector.matchEthType(Ethernet.TYPE_IPV4);
        selector.matchIPDst(ip.ip());
        treatment.transition(LOCAL_TABLE);
        return DefaultFlowRule.builder()
                .withSelector(selector.build())
                .withTreatment(treatment.build())
                .withPriority(HIGHEST_PRIORITY)
                .makePermanent()
                .forTable(FIB_TABLE);
    }

    @Override
    public void initializePipeline() {
        processMeterTable(true);
        processPortBasedProtoTable(true); /* Table 0 */
        processVlanCheckTable(true);      /* Table 1 */
        processVlanMacXlateTable(true);   /* Table 2 */
        processVlanCircuitTable(true);    /* Table 3 */
        processPriorityMapTable(true);    /* Table 4 */
        processL3IFMacDATable(true);      /* Table 5 */
        processEtherTable(true);          /* Table 6 */
        processFibTable(true);            /* Table 7 */
        processLocalTable(true);          /* Table 9 */
    }

    protected void processMeterTable(boolean install) {
        //Green meter : Pass all traffic
        Band dropBand = DefaultBand.builder()
                .ofType(Band.Type.DROP)
                .withRate(0xFFFFFFFF)   /* Max Rate */
                .build();
        MeterRequest.Builder ops = DefaultMeterRequest.builder()
                .forDevice(deviceId)
                .withBands(Collections.singletonList(dropBand))
                .fromApp(appId);

        Meter meter = meterService.submit(install ? ops.add() : ops.remove());
        defaultMeterId = meter.id();
    }

    protected void processPortBasedProtoTable(boolean install) {
        /* Default action */
        processTableMissGoTo(install, PORT_BASED_PROTO_TABLE, VLAN_CHECK_TABLE, "Provisioned port-based table");
    }

    protected void processVlanCheckTable(boolean install) {

        /* Default action */
        processTableMissDrop(install, VLAN_CHECK_TABLE, "Provisioned vlantable drop");

        processTaggedPackets(install);

    }

    /* Tagged packets to VLAN_MAC_XLATE */
    protected void processTaggedPackets(boolean install) {
        TrafficSelector.Builder selector = DefaultTrafficSelector.builder();
        selector.matchVlanId(VlanId.ANY);

        TrafficTreatment.Builder treatment = DefaultTrafficTreatment.builder();
        treatment.transition(VLAN_MAC_XLATE_TABLE);

        FlowRule rule = DefaultFlowRule.builder()
                .forDevice(deviceId)
                .withSelector(selector.build())
                .withTreatment(treatment.build())
                .withPriority(CONTROLLER_PRIORITY)
                .fromApp(appId)
                .makePermanent()
                .forTable(VLAN_CHECK_TABLE).build();
        processFlowRule(install, rule, "Provisioned vlan table tagged packets");
    }

    protected void processVlanMacXlateTable(boolean install) {
        /* Default action */
        processTableMissGoTo(install, VLAN_MAC_XLATE_TABLE, VLAN_CIRCUIT_TABLE, "Provisioned vlan mac table");
    }

    protected void processVlanCircuitTable(boolean install) {
        /* Default action */
        processTableMissDrop(install, VLAN_CIRCUIT_TABLE, "Provisioned vlan circuit");
    }

    private void processPriorityMapTable(boolean install) {
        /* Not required currently */
    }

    protected void processL3IFMacDATable(boolean install) {
        int table = L3_IF_MAC_DA_TABLE;

        /* Default action */
        processTableMissDrop(install, table, "Provisioned l3 table drop");

        /* Allow MAC broadcast frames on all ports */
        TrafficSelector.Builder selector = DefaultTrafficSelector.builder();
        selector.matchEthDst(MacAddress.BROADCAST);

        TrafficTreatment.Builder treatment = DefaultTrafficTreatment.builder();
        treatment.transition(ETHER_TABLE);

        FlowRule rule = DefaultFlowRule.builder()
                .forDevice(deviceId)
                .withSelector(selector.build())
                .withTreatment(treatment.build())
                .withPriority(CONTROLLER_PRIORITY)
                .fromApp(appId)
                .makePermanent()
                .forTable(table).build();
        processFlowRule(install, rule, "Provisioned l3 table");
    }


    protected void processEtherTable(boolean install) {
        int table = ETHER_TABLE;

        /* Default action */
        processTableMissDrop(install, table, "Provisioned ether type table drop");

        /* Arp to controller */
        TrafficSelector.Builder selector = DefaultTrafficSelector.builder();
        selector.matchEthType(Ethernet.TYPE_ARP);

        TrafficTreatment.Builder treatment = DefaultTrafficTreatment.builder();
        treatment.punt();

        FlowRule rule = DefaultFlowRule.builder()
                .forDevice(deviceId)
                .withSelector(selector.build())
                .withTreatment(treatment.build())
                .withPriority(CONTROLLER_PRIORITY)
                .fromApp(appId)
                .makePermanent()
                .forTable(table).build();
        processFlowRule(install, rule, "Provisioned ether type table arp");

        /* IP to FIB_TABLE */
        selector = DefaultTrafficSelector.builder();
        selector.matchEthType(Ethernet.TYPE_IPV4);

        treatment = DefaultTrafficTreatment.builder();
        treatment.transition(FIB_TABLE);

        rule = DefaultFlowRule.builder()
                .forDevice(deviceId)
                .withSelector(selector.build())
                .withTreatment(treatment.build())
                .withPriority(CONTROLLER_PRIORITY)
                .fromApp(appId)
                .makePermanent()
                .forTable(table).build();
        processFlowRule(install, rule, "Provisioned ether type table ip");
    }

    protected void processFibTable(boolean install) {
        /* Default action */
        processTableMissDrop(install, FIB_TABLE, "Provisioned fib drop");
    }

    private void processLocalTable(boolean install) {
        int table = LOCAL_TABLE;
        /* Default action */
        processTableMissDrop(install, table, "Provisioned local table drop");

        /* Send all protocols to controller */
        TrafficSelector.Builder selector = DefaultTrafficSelector.builder();

        TrafficTreatment.Builder treatment = DefaultTrafficTreatment.builder();
        treatment.punt();

        FlowRule rule = DefaultFlowRule.builder()
                .forDevice(deviceId)
                .withSelector(selector.build())
                .withTreatment(treatment.build())
                .withPriority(CONTROLLER_PRIORITY)
                .fromApp(appId)
                .makePermanent()
                .forTable(table).build();
        processFlowRule(install, rule, "Provisioned ether type table to controller");
    }


}

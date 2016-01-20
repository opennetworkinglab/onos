package org.onosproject.driver.pipeline;

import org.onlab.packet.Ethernet;
import org.onlab.packet.MacAddress;
import org.onlab.packet.VlanId;
import org.onosproject.core.ApplicationId;
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
import org.onosproject.net.flow.criteria.IPCriterion;
import org.onosproject.net.flow.criteria.PortCriterion;
import org.onosproject.net.flow.criteria.VlanIdCriterion;
import org.onosproject.net.flow.instructions.Instructions;
import org.onosproject.net.flow.instructions.L2ModificationInstruction;
import org.onosproject.net.flowobjective.FilteringObjective;
import org.onosproject.net.flowobjective.ForwardingObjective;
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

import static org.slf4j.LoggerFactory.getLogger;

public class CorsaPipelineV3 extends OVSCorsaPipeline {

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

    private MeterId defaultMeterId = null;

    @Override
    protected TrafficTreatment processNextTreatment(TrafficTreatment treatment) {
        TrafficTreatment.Builder tb = DefaultTrafficTreatment.builder();

        treatment.immediate().stream()
                .filter(i -> i instanceof L2ModificationInstruction.ModVlanIdInstruction ||
                             i instanceof L2ModificationInstruction.ModEtherInstruction ||
                             i instanceof Instructions.OutputInstruction)
                .forEach(i -> tb.add(i));
        return tb.build();
    }

    @Override
    protected TrafficTreatment.Builder processSpecificRoutingTreatment() {
        return DefaultTrafficTreatment.builder().deferred();
    }

    @Override
    protected FlowRule.Builder processSpecificRoutingRule(FlowRule.Builder rb) {
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

        FlowRule.Builder ruleBuilder = DefaultFlowRule.builder()
                .fromApp(fwd.appId())
                .withPriority(fwd.priority())
                .forDevice(deviceId)
                .withSelector(filteredSelector)
                .withTreatment(fwd.treatment())
                .forTable(VLAN_CIRCUIT_TABLE);

        if (fwd.permanent()) {
            ruleBuilder.makePermanent();
        } else {
            ruleBuilder.makeTemporary(fwd.timeout());
        }

        return Collections.singletonList(ruleBuilder.build());
    }

    @Override
    protected void processFilter(FilteringObjective filt, boolean install,
                               ApplicationId applicationId) {
        // This driver only processes filtering criteria defined with switch
        // ports as the key
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
        // convert filtering conditions for switch-intfs into flowrules
        FlowRuleOperations.Builder ops = FlowRuleOperations.builder();
        for (Criterion c : filt.conditions()) {
            if (c.type() == Criterion.Type.ETH_DST) {
                EthCriterion e = (EthCriterion) c;
                log.debug("adding rule for MAC: {}", e.mac());
                TrafficSelector.Builder selector = DefaultTrafficSelector.builder();
                TrafficTreatment.Builder treatment = DefaultTrafficTreatment.builder();
                selector.matchEthDst(e.mac());
                selector.matchInPort(p.port());
                treatment.transition(ETHER_TABLE);
                FlowRule rule = DefaultFlowRule.builder()
                        .forDevice(deviceId)
                        .withSelector(selector.build())
                        .withTreatment(treatment.build())
                        .withPriority(CONTROLLER_PRIORITY)
                        .fromApp(applicationId)
                        .makePermanent()
                        .forTable(L3_IF_MAC_DA_TABLE).build();
                ops =  install ? ops.add(rule) : ops.remove(rule);
            } else if (c.type() == Criterion.Type.VLAN_VID) {
                VlanIdCriterion v = (VlanIdCriterion) c;
                log.debug("adding rule for VLAN: {}", v.vlanId());
                TrafficSelector.Builder selector = DefaultTrafficSelector.builder();
                TrafficTreatment.Builder treatment = DefaultTrafficTreatment.builder();
                selector.matchVlanId(v.vlanId());
                selector.matchInPort(p.port());
                /* Static treatment for VLAN_CIRCUIT_TABLE */
                treatment.setVlanPcp(MAX_VLAN_PCP);
                treatment.setQueue(0);
                treatment.meter(MeterId.meterId(defaultMeterId.id())); /* use default meter (Green) */
                treatment.transition(L3_IF_MAC_DA_TABLE);
                FlowRule rule = DefaultFlowRule.builder()
                        .forDevice(deviceId)
                        .withSelector(selector.build())
                        .withTreatment(treatment.build())
                        .withPriority(CONTROLLER_PRIORITY)
                        .fromApp(applicationId)
                        .makePermanent()
                        .forTable(VLAN_CIRCUIT_TABLE).build();
                ops = install ? ops.add(rule) : ops.remove(rule);
            } else if (c.type() == Criterion.Type.IPV4_DST) {
                IPCriterion ip = (IPCriterion) c;
                log.debug("adding rule for IP: {}", ip.ip());
                TrafficSelector.Builder selector = DefaultTrafficSelector.builder();
                TrafficTreatment.Builder treatment = DefaultTrafficTreatment.builder();
                selector.matchEthType(Ethernet.TYPE_IPV4);
                selector.matchIPDst(ip.ip());
                treatment.transition(LOCAL_TABLE);
                FlowRule rule = DefaultFlowRule.builder()
                        .forDevice(deviceId)
                        .withSelector(selector.build())
                        .withTreatment(treatment.build())
                        .withPriority(HIGHEST_PRIORITY)
                        .fromApp(applicationId)
                        .makePermanent()
                        .forTable(FIB_TABLE).build();

                ops = install ? ops.add(rule) : ops.remove(rule);
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

    private void processMeterTable(boolean install) {
        /* Green meter : Pass all traffic */
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

    private void processPortBasedProtoTable(boolean install) {
        /* Default action */
        processTableMissGoTo(install, PORT_BASED_PROTO_TABLE, VLAN_CHECK_TABLE);
    }

    private void processVlanCheckTable(boolean install) {
        int table = VLAN_CHECK_TABLE;

        /* Default action */
        processTableMissDrop(install, table);

        /* Tagged packets to VLAN_MAC_XLATE */
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
                .forTable(table).build();
        processFlowRule(install, rule);
    }

    private void processVlanMacXlateTable(boolean install) {
        /* Default action */
        processTableMissGoTo(install, VLAN_MAC_XLATE_TABLE, VLAN_CIRCUIT_TABLE);
    }

    private void processVlanCircuitTable(boolean install) {
        /* Default action */
        processTableMissDrop(install, VLAN_CIRCUIT_TABLE);
    }

    private void processPriorityMapTable(boolean install) {
        /* Not required currently */
    }

    private void processL3IFMacDATable(boolean install) {
        int table = L3_IF_MAC_DA_TABLE;

        /* Default action */
        processTableMissDrop(install, table);

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
        processFlowRule(install, rule);
    }


    private void processEtherTable(boolean install) {
        int table = ETHER_TABLE;

        /* Default action */
        processTableMissDrop(install, table);

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
        processFlowRule(install, rule);

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
        processFlowRule(install, rule);
    }

    private void processFibTable(boolean install) {
        /* Default action */
        processTableMissDrop(install, FIB_TABLE);
    }

    private void processLocalTable(boolean install) {
        int table = LOCAL_TABLE;
        /* Default action */
        processTableMissDrop(install, table);

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
        processFlowRule(install, rule);
    }

    /* Init helper: Apply flow rule */
    private void processFlowRule(boolean install, FlowRule rule) {
        FlowRuleOperations.Builder ops = FlowRuleOperations.builder();
        ops = install ? ops.add(rule) : ops.remove(rule);

        flowRuleService.apply(ops.build(new FlowRuleOperationsContext() {
            @Override
            public void onSuccess(FlowRuleOperations ops) {
                log.info("Flow provision success: " + ops.toString() + ", " + rule.toString());
            }

            @Override
            public void onError(FlowRuleOperations ops) {
                log.info("Flow provision error: " + ops.toString() + ", " + rule.toString());
            }
        }));
    }

    /* Init helper: Table Miss = Drop */
    private void processTableMissDrop(boolean install, int table) {
        TrafficSelector.Builder selector = DefaultTrafficSelector.builder();

        TrafficTreatment.Builder treatment = DefaultTrafficTreatment.builder();
        treatment.drop();

        FlowRule rule = DefaultFlowRule.builder()
                .forDevice(deviceId)
                .withSelector(selector.build())
                .withTreatment(treatment.build())
                .withPriority(DROP_PRIORITY)
                .fromApp(appId)
                .makePermanent()
                .forTable(table).build();

        processFlowRule(install, rule);
    }

    /* Init helper: Table Miss = GoTo */
    private void processTableMissGoTo(boolean install, int table, int goTo) {
        TrafficSelector.Builder selector = DefaultTrafficSelector.builder();

        TrafficTreatment.Builder treatment = DefaultTrafficTreatment.builder();
        treatment.transition(goTo);

        FlowRule rule = DefaultFlowRule.builder()
                .forDevice(deviceId)
                .withSelector(selector.build())
                .withTreatment(treatment.build())
                .withPriority(DROP_PRIORITY)
                .fromApp(appId)
                .makePermanent()
                .forTable(table).build();

        processFlowRule(install, rule);
    }
}

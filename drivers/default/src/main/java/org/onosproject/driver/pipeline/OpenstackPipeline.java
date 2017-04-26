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

import org.onlab.osgi.ServiceDirectory;
import org.onlab.packet.MacAddress;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.net.DeviceId;
import org.onosproject.net.behaviour.Pipeliner;
import org.onosproject.net.behaviour.PipelinerContext;
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
import org.onosproject.net.flow.criteria.IPCriterion;
import org.onosproject.net.flow.criteria.PortCriterion;
import org.onosproject.net.flow.criteria.TunnelIdCriterion;
import org.onosproject.net.flow.criteria.VlanIdCriterion;
import org.onosproject.net.flow.instructions.Instruction;
import org.onosproject.net.flowobjective.FilteringObjective;
import org.onosproject.net.flowobjective.FlowObjectiveStore;
import org.onosproject.net.flowobjective.ForwardingObjective;
import org.onosproject.net.flowobjective.NextObjective;
import org.onosproject.net.flowobjective.Objective;
import org.onosproject.net.flowobjective.ObjectiveError;
import org.slf4j.Logger;

import java.util.Optional;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Driver for OpenstackSwitching.
 */
public class OpenstackPipeline extends DefaultSingleTablePipeline
        implements Pipeliner {

    private final Logger log = getLogger(getClass());
    protected FlowObjectiveStore flowObjectiveStore;
    protected DeviceId deviceId;
    protected ApplicationId appId;
    protected FlowRuleService flowRuleService;

    private static final int SRC_VNI_TABLE = 0;
    private static final int ACL_TABLE = 1;
    private static final int JUMP_TABLE = 2;
    private static final int ROUTING_TABLE = 3;
    private static final int FORWARDING_TABLE = 4;
    private static final int DUMMY_TABLE = 10;
    private static final int LAST_TABLE = FORWARDING_TABLE;

    private static final int DROP_PRIORITY = 0;
    private static final int HIGH_PRIORITY = 30000;
    private static final int TIME_OUT = 0;
    private static final String VIRTUAL_GATEWAY_MAC = "fe:00:00:00:00:02";


    @Override
    public void init(DeviceId deviceId, PipelinerContext context) {
        super.init(deviceId, context);
        ServiceDirectory serviceDirectory = context.directory();
        this.deviceId = deviceId;

        CoreService coreService = serviceDirectory.get(CoreService.class);
        flowRuleService = serviceDirectory.get(FlowRuleService.class);
        flowObjectiveStore = context.store();

        appId = coreService.registerApplication(
                "org.onosproject.driver.OpenstackPipeline");

        initializePipeline();
    }

    @Override
    public void filter(FilteringObjective filteringObjective) {
        super.filter(filteringObjective);
    }

    @Override
    public void next(NextObjective nextObjective) {
        super.next(nextObjective);
    }

    @Override
    public void forward(ForwardingObjective forwardingObjective) {
        FlowRule flowRule;

        switch (forwardingObjective.flag()) {
            case SPECIFIC:
                flowRule = processSpecific(forwardingObjective);
                break;
            case VERSATILE:
                flowRule = processVersatile(forwardingObjective);
                break;
            default:
                fail(forwardingObjective, ObjectiveError.UNKNOWN);
                log.warn("Unknown forwarding flag {}", forwardingObjective.flag());
                return;
        }

        if (forwardingObjective.op().equals(Objective.Operation.ADD)) {
            applyRules(true, flowRule);
        } else {
            applyRules(false, flowRule);
        }

    }

    private void initializePipeline() {
        connectTables(SRC_VNI_TABLE, ACL_TABLE);
        connectTables(ACL_TABLE, JUMP_TABLE);
        setUpTableMissEntry(ACL_TABLE);
        setupJumpTable();
    }

    private void connectTables(int fromTable, int toTable) {
        TrafficSelector.Builder selector = DefaultTrafficSelector.builder();
        TrafficTreatment.Builder treatment = DefaultTrafficTreatment.builder();

        treatment.transition(toTable);

        FlowRule flowRule = DefaultFlowRule.builder()
                .forDevice(deviceId)
                .withSelector(selector.build())
                .withTreatment(treatment.build())
                .withPriority(DROP_PRIORITY)
                .fromApp(appId)
                .makePermanent()
                .forTable(fromTable)
                .build();

        applyRules(true, flowRule);
    }

    private void setUpTableMissEntry(int table) {
        TrafficSelector.Builder selector = DefaultTrafficSelector.builder();
        TrafficTreatment.Builder treatment = DefaultTrafficTreatment.builder();

        treatment.drop();

        FlowRule flowRule = DefaultFlowRule.builder()
                .forDevice(deviceId)
                .withSelector(selector.build())
                .withTreatment(treatment.build())
                .withPriority(DROP_PRIORITY)
                .fromApp(appId)
                .makePermanent()
                .forTable(table)
                .build();

        applyRules(true, flowRule);
    }

    private void setupJumpTable() {
        TrafficSelector.Builder selector = DefaultTrafficSelector.builder();
        TrafficTreatment.Builder treatment = DefaultTrafficTreatment.builder();

        selector.matchEthDst(MacAddress.valueOf(VIRTUAL_GATEWAY_MAC));
        treatment.transition(ROUTING_TABLE);

        FlowRule flowRule = DefaultFlowRule.builder()
                .forDevice(deviceId)
                .withSelector(selector.build())
                .withTreatment(treatment.build())
                .withPriority(HIGH_PRIORITY)
                .fromApp(appId)
                .makePermanent()
                .forTable(JUMP_TABLE)
                .build();

        applyRules(true, flowRule);

        selector = DefaultTrafficSelector.builder();
        treatment = DefaultTrafficTreatment.builder();

        treatment.transition(FORWARDING_TABLE);

        flowRule = DefaultFlowRule.builder()
                .forDevice(deviceId)
                .withSelector(selector.build())
                .withTreatment(treatment.build())
                .withPriority(DROP_PRIORITY)
                .fromApp(appId)
                .makePermanent()
                .forTable(JUMP_TABLE)
                .build();

        applyRules(true, flowRule);
    }

    private void applyRules(boolean install, FlowRule flowRule) {
        FlowRuleOperations.Builder flowOpsBuilder = FlowRuleOperations.builder();

        flowOpsBuilder = install ? flowOpsBuilder.add(flowRule) : flowOpsBuilder.remove(flowRule);

        flowRuleService.apply(flowOpsBuilder.build(new FlowRuleOperationsContext() {
            @Override
            public void onSuccess(FlowRuleOperations ops) {
                log.debug("Provisioned vni or forwarding table");
            }

            @Override
            public void onError(FlowRuleOperations ops) {
                log.debug("Failed to privision vni or forwarding table");
            }
        }));
    }

    private FlowRule processVersatile(ForwardingObjective forwardingObjective) {
        log.debug("Processing versatile forwarding objective");

        FlowRule.Builder ruleBuilder = DefaultFlowRule.builder()
                .forDevice(deviceId)
                .withSelector(forwardingObjective.selector())
                .withTreatment(forwardingObjective.treatment())
                .withPriority(forwardingObjective.priority())
                .fromApp(forwardingObjective.appId())
                .forTable(SRC_VNI_TABLE);

        if (forwardingObjective.permanent()) {
            ruleBuilder.makePermanent();
        } else {
            ruleBuilder.makeTemporary(TIME_OUT);
        }

        return ruleBuilder.build();
    }

    private FlowRule processSpecific(ForwardingObjective forwardingObjective) {
        log.debug("Processing specific forwarding objective");

        TrafficTreatment.Builder treatment = DefaultTrafficTreatment.builder();
        Optional<Instruction> group = forwardingObjective.treatment().immediate().stream()
                .filter(i -> i.type() == Instruction.Type.GROUP).findAny();
        int tableType = tableType(forwardingObjective);
        if (tableType != LAST_TABLE && !group.isPresent()) {
            treatment.transition(nextTable(tableType));
        }
        forwardingObjective.treatment().allInstructions().stream()
                .filter(i -> i.type() != Instruction.Type.NOACTION).forEach(treatment::add);

        FlowRule.Builder ruleBuilder = DefaultFlowRule.builder()
                .forDevice(deviceId)
                .withSelector(forwardingObjective.selector())
                .withTreatment(treatment.build())
                .withPriority(forwardingObjective.priority())
                .fromApp(forwardingObjective.appId())
                .forTable(tableType);

        if (forwardingObjective.permanent()) {
            ruleBuilder.makePermanent();
        } else {
            ruleBuilder.makeTemporary(TIME_OUT);
        }

        return ruleBuilder.build();
    }

    private int tableType(ForwardingObjective fo) {

        IPCriterion ipSrc = (IPCriterion) fo.selector().getCriterion(Criterion.Type.IPV4_SRC);
        IPCriterion ipDst = (IPCriterion) fo.selector().getCriterion(Criterion.Type.IPV4_DST);
        TunnelIdCriterion tunnelId =
                (TunnelIdCriterion) fo.selector().getCriterion(Criterion.Type.TUNNEL_ID);
        VlanIdCriterion vlanId = (VlanIdCriterion) fo.selector().getCriterion(Criterion.Type.VLAN_VID);
        PortCriterion inPort = (PortCriterion) fo.selector().getCriterion(Criterion.Type.IN_PORT);
        Optional<Instruction> output = fo.treatment().immediate().stream()
                .filter(i -> i.type() == Instruction.Type.OUTPUT).findAny();
        Optional<Instruction> group = fo.treatment().immediate().stream()
                .filter(i -> i.type() == Instruction.Type.GROUP).findAny();

        // TODO: Add the Connection Tracking Table
        if (inPort != null) {
            return SRC_VNI_TABLE;
        } else if ((tunnelId != null && ipSrc != null && ipDst != null) ||
                (vlanId != null && ipSrc != null && ipDst != null) ||
                (ipSrc != null && group.isPresent())) {
            return ROUTING_TABLE;
        } else if (output.isPresent() || (ipDst != null && group.isPresent())) {
            return FORWARDING_TABLE;
        } else if ((ipSrc != null && ipSrc.ip().prefixLength() == 32 &&
                ipDst != null && ipDst.ip().prefixLength() == 32) ||
                (ipSrc != null && ipSrc.ip().prefixLength() == 32 && ipDst == null) ||
                (ipDst != null && ipDst.ip().prefixLength() == 32 && ipSrc == null) ||
                (ipDst != null && ipDst.ip().prefixLength() == 32 && ipSrc != null) ||
                (ipSrc != null && ipSrc.ip().prefixLength() == 32 && ipDst != null)) {
            return ACL_TABLE;
        }

        return DUMMY_TABLE;
    }

    private int nextTable(int baseTable) {
        return baseTable + 1;
    }

    private void fail(Objective obj, ObjectiveError error) {
        obj.context().ifPresent(context -> context.onError(obj, error));
    }
}


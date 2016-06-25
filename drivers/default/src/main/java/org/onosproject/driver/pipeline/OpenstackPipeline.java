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
import org.onlab.packet.Ethernet;
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
import org.onosproject.net.flow.criteria.EthTypeCriterion;
import org.onosproject.net.flow.criteria.UdpPortCriterion;
import org.onosproject.net.flowobjective.FilteringObjective;
import org.onosproject.net.flowobjective.FlowObjectiveStore;
import org.onosproject.net.flowobjective.ForwardingObjective;
import org.onosproject.net.flowobjective.NextObjective;
import org.onosproject.net.flowobjective.Objective;
import org.onosproject.net.flowobjective.ObjectiveError;
import org.slf4j.Logger;

import java.util.Collection;
import java.util.Collections;
import java.util.Objects;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Driver for OpenstackSwitching.
 */
public class OpenstackPipeline extends DefaultSingleTablePipeline
        implements Pipeliner {

    private final Logger log = getLogger(getClass());
    private CoreService coreService;
    private ServiceDirectory serviceDirectory;
    protected FlowObjectiveStore flowObjectiveStore;
    protected DeviceId deviceId;
    protected ApplicationId appId;
    protected FlowRuleService flowRuleService;

    protected static final int VNI_TABLE = 0;
    protected static final int FORWARDING_TABLE = 1;
    protected static final int ACL_TABLE = 2;

    private static final int DROP_PRIORITY = 0;
    private static final int TIME_OUT = 0;
    private static final int DHCP_SERVER_PORT = 67;
    private static final int DHCP_CLIENT_PORT = 68;


    @Override
    public void init(DeviceId deviceId, PipelinerContext context) {
        super.init(deviceId, context);
        this.serviceDirectory = context.directory();
        this.deviceId = deviceId;

        coreService = serviceDirectory.get(CoreService.class);
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
        Collection<FlowRule> rules;
        FlowRuleOperations.Builder flowOpsBuilder = FlowRuleOperations.builder();

        rules = processForward(forwardingObjective);

        switch (forwardingObjective.op()) {
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
                fail(forwardingObjective, ObjectiveError.UNKNOWN);
                log.warn("Unknown forwarding type {}");
        }

        flowRuleService.apply(flowOpsBuilder.build(new FlowRuleOperationsContext() {
            @Override
            public void onSuccess(FlowRuleOperations ops) {
                pass(forwardingObjective);
            }

            @Override
            public void onError(FlowRuleOperations ops) {
                fail(forwardingObjective, ObjectiveError.FLOWINSTALLATIONFAILED);
            }
        }));
    }

    private void initializePipeline() {
        processVniTable(true);
        processForwardingTable(true);
        processAclTable(true);
    }

    private void processVniTable(boolean install) {
        TrafficSelector.Builder selector = DefaultTrafficSelector.builder();
        TrafficTreatment.Builder treatment = DefaultTrafficTreatment.builder();

        treatment.transition(FORWARDING_TABLE);

        FlowRule flowRule = DefaultFlowRule.builder()
                .forDevice(deviceId)
                .withSelector(selector.build())
                .withTreatment(treatment.build())
                .withPriority(DROP_PRIORITY)
                .fromApp(appId)
                .makePermanent()
                .forTable(VNI_TABLE)
                .build();

        applyRules(install, flowRule);
    }

    private void processForwardingTable(boolean install) {
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
                .forTable(FORWARDING_TABLE)
                .build();

        applyRules(install, flowRule);
    }

    private void processAclTable(boolean install) {
        TrafficSelector.Builder selector = DefaultTrafficSelector.builder();
        TrafficTreatment.Builder treatment = DefaultTrafficTreatment.builder();

        treatment.wipeDeferred();
        treatment.drop();

        FlowRule flowRule = DefaultFlowRule.builder()
                .forDevice(deviceId)
                .withSelector(selector.build())
                .withTreatment(treatment.build())
                .withPriority(DROP_PRIORITY)
                .fromApp(appId)
                .makePermanent()
                .forTable(ACL_TABLE)
                .build();

        applyRules(install, flowRule);
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

    private Collection<FlowRule> processForward(ForwardingObjective forwardingObjective) {
        switch (forwardingObjective.flag()) {
            case SPECIFIC:
                return processSpecific(forwardingObjective);
            case VERSATILE:
                return processVersatile(forwardingObjective);
            default:
                fail(forwardingObjective, ObjectiveError.UNKNOWN);
                log.warn("Unknown forwarding flag {}", forwardingObjective.flag());
        }
        return Collections.emptySet();
    }

    private Collection<FlowRule> processVersatile(ForwardingObjective forwardingObjective) {
        log.debug("Processing versatile forwarding objective");

        FlowRule.Builder ruleBuilder = DefaultFlowRule.builder()
                .forDevice(deviceId)
                .withSelector(forwardingObjective.selector())
                .withTreatment(forwardingObjective.treatment())
                .withPriority(forwardingObjective.priority())
                .fromApp(forwardingObjective.appId());

        if (forwardingObjective.permanent()) {
            ruleBuilder.makePermanent();
        } else {
            ruleBuilder.makeTemporary(TIME_OUT);
        }

        //ARP & DHCP Rule
        EthTypeCriterion ethCriterion =
                (EthTypeCriterion) forwardingObjective.selector().getCriterion(Criterion.Type.ETH_TYPE);
        UdpPortCriterion udpPortCriterion = (UdpPortCriterion) forwardingObjective
                .selector().getCriterion(Criterion.Type.UDP_DST);
        if (ethCriterion != null) {
            if (ethCriterion.ethType().toShort() == Ethernet.TYPE_ARP ||
                    ethCriterion.ethType().toShort() == Ethernet.TYPE_LLDP) {
                ruleBuilder.forTable(VNI_TABLE);
                return Collections.singletonList(ruleBuilder.build());
            } else if (udpPortCriterion != null && udpPortCriterion.udpPort().toInt() == DHCP_SERVER_PORT) {
                ruleBuilder.forTable(VNI_TABLE);
                return Collections.singletonList(ruleBuilder.build());
            }
        }
        return Collections.emptySet();
    }

    private Collection<FlowRule> processSpecific(ForwardingObjective forwardingObjective) {
        log.debug("Processing specific forwarding objective");

        FlowRule.Builder ruleBuilder = DefaultFlowRule.builder()
                .forDevice(deviceId)
                .withSelector(forwardingObjective.selector())
                .withTreatment(forwardingObjective.treatment())
                .withPriority(forwardingObjective.priority())
                .fromApp(forwardingObjective.appId());

        if (forwardingObjective.permanent()) {
            ruleBuilder.makePermanent();
        } else {
            ruleBuilder.makeTemporary(TIME_OUT);
        }

        //VNI Table Rule
        if (forwardingObjective.selector().getCriterion(Criterion.Type.IN_PORT) != null) {
            TrafficTreatment.Builder tBuilder = DefaultTrafficTreatment.builder();
            forwardingObjective.treatment().allInstructions().forEach(tBuilder::add);
            tBuilder.transition(FORWARDING_TABLE);
            ruleBuilder.withTreatment(tBuilder.build());
            ruleBuilder.forTable(VNI_TABLE);
        } else if (forwardingObjective.selector().getCriterion(Criterion.Type.TUNNEL_ID) != null) {
            TrafficTreatment.Builder tBuilder = DefaultTrafficTreatment.builder();
            tBuilder.deferred();
            forwardingObjective.treatment().allInstructions().forEach(tBuilder::add);
            tBuilder.transition(ACL_TABLE);
            ruleBuilder.withTreatment(tBuilder.build());
            ruleBuilder.forTable(FORWARDING_TABLE);
        } else {
            ruleBuilder.forTable(ACL_TABLE);
        }

        return Collections.singletonList(ruleBuilder.build());
    }


    private void pass(Objective obj) {
        obj.context().ifPresent(context -> context.onSuccess(obj));
    }

    private void fail(Objective obj, ObjectiveError error) {
        obj.context().ifPresent(context -> context.onError(obj, error));
    }
}


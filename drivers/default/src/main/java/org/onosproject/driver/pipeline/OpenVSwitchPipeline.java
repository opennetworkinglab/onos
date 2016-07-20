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

import java.util.Collection;
import java.util.Collections;
import java.util.Objects;

import org.onlab.osgi.ServiceDirectory;
import org.onlab.packet.EthType.EtherType;
import org.onlab.packet.IpAddress;
import org.onlab.packet.IpPrefix;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;
import org.onosproject.net.behaviour.Pipeliner;
import org.onosproject.net.behaviour.PipelinerContext;
import org.onosproject.net.device.DeviceService;
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
import org.onosproject.net.flow.criteria.Criterion.Type;
import org.onosproject.net.flow.criteria.IPCriterion;
import org.onosproject.net.flow.instructions.Instructions;
import org.onosproject.net.flowobjective.FilteringObjective;
import org.onosproject.net.flowobjective.FlowObjectiveStore;
import org.onosproject.net.flowobjective.ForwardingObjective;
import org.onosproject.net.flowobjective.NextObjective;
import org.onosproject.net.flowobjective.Objective;
import org.onosproject.net.flowobjective.ObjectiveError;
import org.slf4j.Logger;

/**
 * Driver for standard OpenVSwitch.
 */
public class OpenVSwitchPipeline extends DefaultSingleTablePipeline
        implements Pipeliner {

    private static final String VTN_APP_ID = "org.onosproject.app.vtn";
    private final Logger log = getLogger(getClass());
    private CoreService coreService;
    private ServiceDirectory serviceDirectory;
    protected FlowObjectiveStore flowObjectiveStore;
    protected DeviceId deviceId;
    protected ApplicationId appId;
    protected FlowRuleService flowRuleService;
    protected DeviceService deviceService;
    private static final int TIME_OUT = 0;
    private static final int CLASSIFIER_TABLE = 0;
    private static final int ENCAP_OUTPUT_TABLE = 4;
    private static final int TUN_SEND_TABLE = 7;
    private static final int ARP_TABLE = 10;
    private static final int DNAT_TABLE = 20;
    private static final int L3FWD_TABLE = 30;
    private static final int SNAT_TABLE = 40;
    private static final int MAC_TABLE = 50;
    private static final int TABLE_MISS_PRIORITY = 0;
    private static final String USERDATA_IP = "169.254.169.254";

    @Override
    public void init(DeviceId deviceId, PipelinerContext context) {
        super.init(deviceId, context);
        this.serviceDirectory = context.directory();
        this.deviceId = deviceId;

        coreService = serviceDirectory.get(CoreService.class);
        flowRuleService = serviceDirectory.get(FlowRuleService.class);
        flowObjectiveStore = context.store();
        appId = coreService
                .registerApplication("org.onosproject.driver.OpenVSwitchPipeline");
        initializePipeline();
    }

    @Override
    public void filter(FilteringObjective filteringObjective) {
        super.filter(filteringObjective);
    }

    @Override
    public void forward(ForwardingObjective fwd) {
        if (!VTN_APP_ID.equals(fwd.appId().name())) {
            super.forward(fwd);
            return;
        }
        Collection<FlowRule> rules;
        FlowRuleOperations.Builder flowOpsBuilder = FlowRuleOperations
                .builder();

        rules = processForward(fwd);
        switch (fwd.op()) {
        case ADD:
            rules.stream().filter(Objects::nonNull)
                    .forEach(flowOpsBuilder::add);
            break;
        case REMOVE:
            rules.stream().filter(Objects::nonNull)
                    .forEach(flowOpsBuilder::remove);
            break;
        default:
            fail(fwd, ObjectiveError.UNKNOWN);
            log.warn("Unknown forwarding type {}", fwd.op());
        }

        flowRuleService.apply(flowOpsBuilder
                .build(new FlowRuleOperationsContext() {
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
        super.next(nextObjective);
    }

    private void initializePipeline() {
        processClassifierTable(true);
        processArpTable(true);
        processDnatTable(true);
        processL3fwdTable(true);
        processSnatTable(true);
        processMacTable(true);
    }

    private void processClassifierTable(boolean install) {
        TrafficSelector.Builder selector = DefaultTrafficSelector.builder();
        TrafficTreatment.Builder treatment = DefaultTrafficTreatment.builder();

        treatment.transition(MAC_TABLE);

        FlowRule rule;
        rule = DefaultFlowRule.builder().forDevice(deviceId)
                .withSelector(selector.build())
                .withTreatment(treatment.build())
                .withPriority(TABLE_MISS_PRIORITY).fromApp(appId)
                .makePermanent().forTable(CLASSIFIER_TABLE).build();

        applyRules(install, rule);
    }

    private void processArpTable(boolean install) {
        TrafficSelector.Builder selector = DefaultTrafficSelector.builder();
        TrafficTreatment.Builder treatment = DefaultTrafficTreatment.builder();

        treatment.transition(MAC_TABLE);

        FlowRule rule;
        rule = DefaultFlowRule.builder().forDevice(deviceId)
                .withSelector(selector.build())
                .withTreatment(treatment.build())
                .withPriority(TABLE_MISS_PRIORITY).fromApp(appId)
                .makePermanent().forTable(ARP_TABLE).build();

        applyRules(install, rule);
    }

    private void processDnatTable(boolean install) {
        TrafficSelector.Builder selector = DefaultTrafficSelector.builder();
        TrafficTreatment.Builder treatment = DefaultTrafficTreatment.builder();

        treatment.transition(MAC_TABLE);

        FlowRule rule;
        rule = DefaultFlowRule.builder().forDevice(deviceId)
                .withSelector(selector.build())
                .withTreatment(treatment.build())
                .withPriority(TABLE_MISS_PRIORITY).fromApp(appId)
                .makePermanent().forTable(DNAT_TABLE).build();

        applyRules(install, rule);
    }

    private void processL3fwdTable(boolean install) {
        TrafficSelector.Builder selector = DefaultTrafficSelector.builder();
        TrafficTreatment.Builder treatment = DefaultTrafficTreatment.builder();

        treatment.transition(SNAT_TABLE);

        FlowRule rule;
        rule = DefaultFlowRule.builder().forDevice(deviceId)
                .withSelector(selector.build())
                .withTreatment(treatment.build())
                .withPriority(TABLE_MISS_PRIORITY).fromApp(appId)
                .makePermanent().forTable(L3FWD_TABLE).build();

        applyRules(install, rule);
    }

    private void processSnatTable(boolean install) {
        TrafficSelector.Builder selector = DefaultTrafficSelector.builder();
        TrafficTreatment.Builder treatment = DefaultTrafficTreatment.builder();

        treatment.transition(MAC_TABLE);
        treatment.add(Instructions.createOutput(PortNumber.CONTROLLER));
        FlowRule rule;
        rule = DefaultFlowRule.builder().forDevice(deviceId)
                .withSelector(selector.build())
                .withTreatment(treatment.build())
                .withPriority(TABLE_MISS_PRIORITY).fromApp(appId)
                .makePermanent().forTable(SNAT_TABLE).build();

        applyRules(install, rule);
    }

    private void processMacTable(boolean install) {
        TrafficSelector.Builder selector = DefaultTrafficSelector.builder();
        TrafficTreatment.Builder treatment = DefaultTrafficTreatment.builder();

        treatment.drop();

        FlowRule rule;
        rule = DefaultFlowRule.builder().forDevice(deviceId)
                .withSelector(selector.build())
                .withTreatment(treatment.build())
                .withPriority(TABLE_MISS_PRIORITY).fromApp(appId)
                .makePermanent().forTable(MAC_TABLE).build();

        applyRules(install, rule);
    }

    private void applyRules(boolean install, FlowRule rule) {
        FlowRuleOperations.Builder ops = FlowRuleOperations.builder();

        ops = install ? ops.add(rule) : ops.remove(rule);
        flowRuleService.apply(ops.build(new FlowRuleOperationsContext() {
            @Override
            public void onSuccess(FlowRuleOperations ops) {
                log.info("ONOSW provisioned " + rule.tableId() + " table");
            }

            @Override
            public void onError(FlowRuleOperations ops) {
                log.info("ONOSW failed to provision " + rule.tableId() + " table");
            }
        }));
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

    private Collection<FlowRule> processVersatile(ForwardingObjective fwd) {
        log.debug("Processing versatile forwarding objective");
        TrafficSelector selector = fwd.selector();
        TrafficTreatment tb = fwd.treatment();
        FlowRule.Builder ruleBuilder = DefaultFlowRule.builder().fromApp(fwd.appId()).withPriority(fwd.priority())
                .forDevice(deviceId).withSelector(selector).withTreatment(tb).makeTemporary(TIME_OUT);
        ruleBuilder.withPriority(fwd.priority());
        if (fwd.priority() == 100) {
            ruleBuilder.forTable(ENCAP_OUTPUT_TABLE);
        } else if (fwd.priority() == 200) {
            ruleBuilder.forTable(TUN_SEND_TABLE);
        } else {
            ruleBuilder.forTable(CLASSIFIER_TABLE);
        }

        if (fwd.permanent()) {
            ruleBuilder.makePermanent();
        }
        return Collections.singletonList(ruleBuilder.build());
    }

    private Collection<FlowRule> processSpecific(ForwardingObjective fwd) {
        log.debug("Processing specific forwarding objective");
        TrafficSelector selector = fwd.selector();
        TrafficTreatment tb = fwd.treatment();
        FlowRule.Builder ruleBuilder = DefaultFlowRule.builder()
                .fromApp(fwd.appId()).withPriority(fwd.priority())
                .forDevice(deviceId).withSelector(selector)
                .withTreatment(tb).makeTemporary(TIME_OUT);
        ruleBuilder.withPriority(fwd.priority());
        if (fwd.permanent()) {
            ruleBuilder.makePermanent();
        }
        Integer transition = null;
        Integer forTable = null;
        // MAC table flow rules
        if (selector.getCriterion(Type.TUNNEL_ID) != null
                && (selector.getCriterion(Type.ETH_DST) != null
                        || selector.getCriterion(Type.ETH_SRC) != null)) {
            forTable = MAC_TABLE;
            return reassemblyFlowRule(ruleBuilder, tb, transition, forTable);
        }
        // CLASSIFIER table flow rules
        if (selector.getCriterion(Type.IN_PORT) != null) {
            forTable = CLASSIFIER_TABLE;
            if (selector.getCriterion(Type.ETH_SRC) != null
                    && selector.getCriterion(Type.ETH_DST) != null) {
                transition = L3FWD_TABLE;
            } else if (selector.getCriterion(Type.ETH_SRC) != null
                    || selector.getCriterion(Type.TUNNEL_ID) != null) {
                transition = MAC_TABLE;
            } else if (selector.getCriterion(Type.IPV4_DST) != null) {
                transition = DNAT_TABLE;
            } else if (selector.getCriterion(Type.ETH_TYPE) != null
                    && selector.getCriterion(Type.ETH_TYPE).equals(Criteria
                            .matchEthType(EtherType.ARP.ethType().toShort()))) {
                transition = ARP_TABLE;
            }
            return reassemblyFlowRule(ruleBuilder, tb, transition, forTable);
        }
        // ARP table flow rules
        if (selector.getCriterion(Type.ETH_TYPE) != null
                && selector.getCriterion(Type.ETH_TYPE).equals(Criteria
                        .matchEthType(EtherType.ARP.ethType().toShort()))) {
            // CLASSIFIER table arp flow rules
            if (selector.getCriterion(Type.TUNNEL_ID) == null) {
                if (selector.getCriterion(Type.ARP_OP) != null) {
                    forTable = CLASSIFIER_TABLE;
                    return reassemblyFlowRule(ruleBuilder, tb, null, forTable);
                }
                transition = ARP_TABLE;
                forTable = CLASSIFIER_TABLE;
                return reassemblyFlowRule(ruleBuilder, tb, transition, forTable);
            }
            forTable = ARP_TABLE;
            return reassemblyFlowRule(ruleBuilder, tb, transition, forTable);
        }
        // SNAT table flow rules
        if (selector.getCriterion(Type.TUNNEL_ID) != null
                && selector.getCriterion(Type.IPV4_SRC) != null) {
            transition = MAC_TABLE;
            forTable = SNAT_TABLE;
            return reassemblyFlowRule(ruleBuilder, tb, transition, forTable);
        }
        // L3FWD table flow rules
        if (selector.getCriterion(Type.TUNNEL_ID) != null
                && selector.getCriterion(Type.IPV4_DST) != null) {
            transition = MAC_TABLE;
            forTable = L3FWD_TABLE;
            return reassemblyFlowRule(ruleBuilder, tb, transition, forTable);
        }
        // DNAT table flow rules
        if (selector.getCriterion(Type.IPV4_DST) != null) {
            IPCriterion ipCriterion = (IPCriterion) selector.getCriterion(Type.IPV4_DST);
            IpPrefix ipPrefix = ipCriterion.ip();
            // specific CLASSIFIER table flow rules for userdata
            if (ipPrefix.address().equals(IpAddress.valueOf(USERDATA_IP))) {
                forTable = CLASSIFIER_TABLE;
                transition = MAC_TABLE;
                return reassemblyFlowRule(ruleBuilder, tb, transition, forTable);
            }
            transition = L3FWD_TABLE;
            forTable = DNAT_TABLE;
            return reassemblyFlowRule(ruleBuilder, tb, transition, forTable);
        }
        return Collections.singletonList(ruleBuilder.build());
    }

    private Collection<FlowRule> reassemblyFlowRule(FlowRule.Builder ruleBuilder,
                                                    TrafficTreatment tb,
                                                    Integer transition,
                                                    Integer forTable) {
        if (transition != null) {
            TrafficTreatment.Builder newTraffic = DefaultTrafficTreatment
                    .builder();
            tb.allInstructions().forEach(t -> newTraffic.add(t));
            newTraffic.transition(transition);
            ruleBuilder.withTreatment(newTraffic.build());
        } else {
            ruleBuilder.withTreatment(tb);
        }
        if (forTable != null) {
            ruleBuilder.forTable(forTable);
        }
        return Collections.singletonList(ruleBuilder.build());
    }

    private void fail(Objective obj, ObjectiveError error) {
        obj.context().ifPresent(context -> context.onError(obj, error));
    }

    private void pass(Objective obj) {
        obj.context().ifPresent(context -> context.onSuccess(obj));
    }
}
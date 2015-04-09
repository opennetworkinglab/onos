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

import org.onlab.osgi.ServiceDirectory;
import org.onlab.packet.Ethernet;
import org.onlab.packet.MacAddress;
import org.onlab.packet.VlanId;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.net.DeviceId;
import org.onosproject.net.behaviour.Pipeliner;
import org.onosproject.net.behaviour.PipelinerContext;
import org.onosproject.net.driver.AbstractBehaviour;
import org.onosproject.net.flow.DefaultFlowRule;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.FlowRule;
import org.onosproject.net.flow.FlowRuleOperations;
import org.onosproject.net.flow.FlowRuleOperationsContext;
import org.onosproject.net.flow.FlowRuleService;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.flowobjective.FilteringObjective;
import org.onosproject.net.flowobjective.ForwardingObjective;
import org.onosproject.net.flowobjective.NextObjective;
import org.slf4j.Logger;

import java.util.Collection;
import java.util.concurrent.Future;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Corsa pipeline handler.
 */
public class OVSCorsaPipeline extends AbstractBehaviour implements Pipeliner {

    private static final int CONTROLLER_PRIORITY = 255;
    private static final int DROP_PRIORITY = 0;
    private static final int HIGHEST_PRIORITY = 0xffff;

    private final Logger log = getLogger(getClass());

    private ServiceDirectory serviceDirectory;
    private FlowRuleService flowRuleService;
    private CoreService coreService;
    private DeviceId deviceId;
    private ApplicationId appId;

    @Override
    public void init(DeviceId deviceId, PipelinerContext context) {
        this.serviceDirectory = context.directory();
        this.deviceId = deviceId;


        coreService = serviceDirectory.get(CoreService.class);
        flowRuleService = serviceDirectory.get(FlowRuleService.class);

        appId = coreService.registerApplication(
                "org.onosproject.driver.OVSCorsaPipeline");

        pushDefaultRules();

    }

    @Override
    public Future<Boolean> filter(Collection<FilteringObjective> filteringObjectives) {
        return null;
    }

    @Override
    public Future<Boolean> forward(Collection<ForwardingObjective> forwardObjectives) {
        return null;
    }

    @Override
    public Future<Boolean> next(Collection<NextObjective> nextObjectives) {
        return null;
    }

    private void pushDefaultRules() {
        boolean install = true;
        processTableZero(install);
        processTableOne(install);
        processTableTwo(install);
        processTableFour(install);
        processTableFive(install);
        processTableSix(install);
        processTableNine(install);
    }

    private void processTableZero(boolean install) {
        TrafficSelector.Builder selector;
        TrafficTreatment.Builder treatment;

        // Bcast rule
        selector = DefaultTrafficSelector.builder();
        treatment = DefaultTrafficTreatment.builder();

        selector.matchEthDst(MacAddress.BROADCAST);
        treatment.transition(FlowRule.Type.VLAN_MPLS);

        FlowRule rule = new DefaultFlowRule(deviceId, selector.build(),
                                            treatment.build(),
                                            CONTROLLER_PRIORITY, appId, 0,
                                            true, FlowRule.Type.FIRST);

        FlowRuleOperations.Builder ops = FlowRuleOperations.builder();

        ops = install ? ops.add(rule) : ops.remove(rule);


        //Drop rule
        selector = DefaultTrafficSelector.builder();
        treatment = DefaultTrafficTreatment.builder();

        treatment.drop();

        rule = new DefaultFlowRule(deviceId, selector.build(),
                                   treatment.build(), DROP_PRIORITY, appId,
                                   0, true, FlowRule.Type.FIRST);

        ops = install ? ops.add(rule) : ops.remove(rule);

        flowRuleService.apply(ops.build(new FlowRuleOperationsContext() {
            @Override
            public void onSuccess(FlowRuleOperations ops) {
                log.info("Provisioned default table for bgp router");
            }

            @Override
            public void onError(FlowRuleOperations ops) {
                log.info("Failed to provision default table for bgp router");
            }
        }));

    }

    private void processTableOne(boolean install) {
        TrafficSelector.Builder selector = DefaultTrafficSelector.builder();
        TrafficTreatment.Builder treatment = DefaultTrafficTreatment
                .builder();
        FlowRuleOperations.Builder ops = FlowRuleOperations.builder();
        FlowRule rule;

        selector.matchVlanId(VlanId.ANY);
        treatment.transition(FlowRule.Type.VLAN);

        rule = new DefaultFlowRule(deviceId, selector.build(),
                                   treatment.build(), CONTROLLER_PRIORITY,
                                   appId, 0, true, FlowRule.Type.VLAN_MPLS);

        ops = install ? ops.add(rule) : ops.remove(rule);

        flowRuleService.apply(ops.build(new FlowRuleOperationsContext() {
            @Override
            public void onSuccess(FlowRuleOperations ops) {
                log.info("Provisioned vlan/mpls table for bgp router");
            }

            @Override
            public void onError(FlowRuleOperations ops) {
                log.info(
                        "Failed to provision vlan/mpls table for bgp router");
            }
        }));

    }

    private void processTableTwo(boolean install) {
        TrafficSelector.Builder selector;
        TrafficTreatment.Builder treatment;
        FlowRuleOperations.Builder ops = FlowRuleOperations.builder();
        FlowRule rule;


        //Drop rule
        selector = DefaultTrafficSelector.builder();
        treatment = DefaultTrafficTreatment.builder();

        treatment.drop();

        rule = new DefaultFlowRule(deviceId, selector.build(),
                                   treatment.build(), DROP_PRIORITY, appId,
                                   0, true, FlowRule.Type.VLAN);

        ops = install ? ops.add(rule) : ops.remove(rule);

        flowRuleService.apply(ops.build(new FlowRuleOperationsContext() {
            @Override
            public void onSuccess(FlowRuleOperations ops) {
                log.info("Provisioned vlan table for bgp router");
            }

            @Override
            public void onError(FlowRuleOperations ops) {
                log.info("Failed to provision vlan table for bgp router");
            }
        }));
    }

    private void processTableFour(boolean install) {
        TrafficSelector.Builder selector = DefaultTrafficSelector.builder();
        TrafficTreatment.Builder treatment = DefaultTrafficTreatment
                .builder();
        FlowRuleOperations.Builder ops = FlowRuleOperations.builder();
        FlowRule rule;

        selector.matchEthType(Ethernet.TYPE_ARP);
        treatment.punt();

        rule = new DefaultFlowRule(deviceId, selector.build(),
                                   treatment.build(), CONTROLLER_PRIORITY,
                                   appId, 0, true, FlowRule.Type.ETHER);

        ops = install ? ops.add(rule) : ops.remove(rule);

        selector = DefaultTrafficSelector.builder();
        treatment = DefaultTrafficTreatment.builder();

        selector.matchEthType(Ethernet.TYPE_IPV4);
        treatment.transition(FlowRule.Type.COS);

        rule = new DefaultFlowRule(deviceId, selector.build(),
                                   treatment.build(), CONTROLLER_PRIORITY,
                                   appId, 0, true, FlowRule.Type.ETHER);

        ops = install ? ops.add(rule) : ops.remove(rule);

        //Drop rule
        selector = DefaultTrafficSelector.builder();
        treatment = DefaultTrafficTreatment.builder();

        treatment.drop();

        rule = new DefaultFlowRule(deviceId, selector.build(),
                                   treatment.build(), DROP_PRIORITY, appId,
                                   0, true, FlowRule.Type.ETHER);

        ops = install ? ops.add(rule) : ops.remove(rule);

        flowRuleService.apply(ops.build(new FlowRuleOperationsContext() {
            @Override
            public void onSuccess(FlowRuleOperations ops) {
                log.info("Provisioned ether table for bgp router");
            }

            @Override
            public void onError(FlowRuleOperations ops) {
                log.info("Failed to provision ether table for bgp router");
            }
        }));

    }

    private void processTableFive(boolean install) {
        TrafficSelector.Builder selector = DefaultTrafficSelector.builder();
        TrafficTreatment.Builder treatment = DefaultTrafficTreatment
                .builder();
        FlowRuleOperations.Builder ops = FlowRuleOperations.builder();
        FlowRule rule;

        treatment.transition(FlowRule.Type.IP);

        rule = new DefaultFlowRule(deviceId, selector.build(),
                                   treatment.build(), DROP_PRIORITY, appId,
                                   0, true, FlowRule.Type.COS);

        ops = install ? ops.add(rule) : ops.remove(rule);

        flowRuleService.apply(ops.build(new FlowRuleOperationsContext() {
            @Override
            public void onSuccess(FlowRuleOperations ops) {
                log.info("Provisioned cos table for bgp router");
            }

            @Override
            public void onError(FlowRuleOperations ops) {
                log.info("Failed to provision cos table for bgp router");
            }
        }));

    }

    private void processTableSix(boolean install) {
        TrafficSelector.Builder selector;
        TrafficTreatment.Builder treatment;
        FlowRuleOperations.Builder ops = FlowRuleOperations.builder();
        FlowRule rule;

        //Drop rule
        selector = DefaultTrafficSelector.builder();
        treatment = DefaultTrafficTreatment.builder();

        treatment.drop();

        rule = new DefaultFlowRule(deviceId, selector.build(),
                                   treatment.build(), DROP_PRIORITY, appId,
                                   0, true, FlowRule.Type.IP);

        ops = install ? ops.add(rule) : ops.remove(rule);

        flowRuleService.apply(ops.build(new FlowRuleOperationsContext() {
            @Override
            public void onSuccess(FlowRuleOperations ops) {
                log.info("Provisioned FIB table for bgp router");
            }

            @Override
            public void onError(FlowRuleOperations ops) {
                log.info("Failed to provision FIB table for bgp router");
            }
        }));
    }

    private void processTableNine(boolean install) {
        TrafficSelector.Builder selector = DefaultTrafficSelector.builder();
        TrafficTreatment.Builder treatment = DefaultTrafficTreatment
                .builder();
        FlowRuleOperations.Builder ops = FlowRuleOperations.builder();
        FlowRule rule;

        treatment.punt();

        rule = new DefaultFlowRule(deviceId, selector.build(),
                                   treatment.build(), CONTROLLER_PRIORITY,
                                   appId, 0, true, FlowRule.Type.DEFAULT);

        ops = install ? ops.add(rule) : ops.remove(rule);

        flowRuleService.apply(ops.build(new FlowRuleOperationsContext() {
            @Override
            public void onSuccess(FlowRuleOperations ops) {
                log.info("Provisioned Local table for bgp router");
            }

            @Override
            public void onError(FlowRuleOperations ops) {
                log.info("Failed to provision Local table for bgp router");
            }
        }));
    }

}

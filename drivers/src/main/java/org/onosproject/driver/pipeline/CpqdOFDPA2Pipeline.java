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

import org.onosproject.net.flow.DefaultFlowRule;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.FlowRule;
import org.onosproject.net.flow.FlowRuleOperations;
import org.onosproject.net.flow.FlowRuleOperationsContext;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.slf4j.Logger;


/**
 * Driver for software switch emulation of the OFDPA 2.0 pipeline.
 * The software switch is the CPqD OF 1.3 switch.
 */
public class CpqdOFDPA2Pipeline extends OFDPA2Pipeline {

    private final Logger log = getLogger(getClass());

    @Override
    protected void initializePipeline() {
        processPortTable();
        processTmacTable();
        processIpTable();
        processBridgingTable();
        processAclTable();
        // XXX implement table miss entries and default groups
        //processVlanTable();
        //processMPLSTable();
        //processGroupTable();
    }

    @Override
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

    @Override
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

    @Override
    protected void processIpTable() {
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

    @Override
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

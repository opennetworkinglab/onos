/*
 * Copyright 2017-present Open Networking Laboratory
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

package org.onosproject.drivers.hp;

import org.onosproject.net.PortNumber;
import org.onosproject.net.flow.DefaultFlowRule;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.FlowRule;
import org.onosproject.net.flow.FlowRuleOperations;
import org.onosproject.net.flow.FlowRuleOperationsContext;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.flow.criteria.EthCriterion;
import org.onosproject.net.flow.criteria.IPCriterion;
import org.onosproject.net.flow.criteria.PortCriterion;
import org.onosproject.net.flow.criteria.VlanIdCriterion;
import org.onosproject.net.flowobjective.FilteringObjective;
import org.slf4j.Logger;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Driver for HP3800 hybrid switches.
 */
public class HPPipelineV3800 extends AbstractHPPipeline {

    private static final int HP_TABLE_ZERO = 0;
    private static final int HP_HARDWARE_TABLE = 100;
    private static final int HP_SOFTWARE_TABLE = 200;

    private final Logger log = getLogger(getClass());


    @Override
    protected FlowRule.Builder setDefaultTableIdForFlowObjective(FlowRule.Builder ruleBuilder) {
        log.debug("Setting default table id to hardware table {}", HP_HARDWARE_TABLE);
        return ruleBuilder.forTable(HP_HARDWARE_TABLE);
    }

    @Override
    protected void initializePipeline() {
        log.debug("Installing table zero {}", HP_TABLE_ZERO);
        installHPTableZero();
        log.debug("Installing scavenger rule to hardware table {} because it is default objective table",
                 HP_HARDWARE_TABLE);
        installHPHardwareTable();
        log.debug("Installing software table {}", HP_SOFTWARE_TABLE);
        installHPSoftwareTable();
    }

    @Override
    public void filter(FilteringObjective filter) {
        log.error("Unsupported FilteringObjective: : filtering method send");
    }

    @Override
    protected FlowRule.Builder processEthFiler(FilteringObjective filt, EthCriterion eth, PortCriterion port) {
        log.error("Unsupported FilteringObjective: processEthFilter invoked");
        return null;
    }

    @Override
    protected FlowRule.Builder processVlanFiler(FilteringObjective filt, VlanIdCriterion vlan, PortCriterion port) {
        log.error("Unsupported FilteringObjective: processVlanFilter invoked");
        return null;
    }

    @Override
    protected FlowRule.Builder processIpFilter(FilteringObjective filt, IPCriterion ip, PortCriterion port) {
        log.error("Unsupported FilteringObjective: processIpFilter invoked");
        return null;
    }

    /**
     * HP Table 0 initialization.
     * Installs rule goto HP hardware table in HP table zero
     */
    private void installHPTableZero() {
        TrafficSelector.Builder selector = DefaultTrafficSelector.builder();
        TrafficTreatment.Builder treatment = DefaultTrafficTreatment.builder();

        treatment.transition(HP_HARDWARE_TABLE);

        FlowRule rule = DefaultFlowRule.builder().forDevice(this.deviceId)
                .withSelector(selector.build())
                .withTreatment(treatment.build())
                .withPriority(0)
                .fromApp(appId)
                .makePermanent()
                .forTable(HP_TABLE_ZERO)
                .build();

        this.applyRules(true, rule);

        log.info("Installed table {}", HP_TABLE_ZERO);
    }

    /**
     * HP hardware table initialization.
     * Installs scavenger rule in HP hardware table.
     */
    private void installHPHardwareTable() {
        TrafficSelector.Builder selector = DefaultTrafficSelector.builder();

        TrafficTreatment.Builder treatment = DefaultTrafficTreatment.builder();
        treatment.setOutput(PortNumber.NORMAL);

        FlowRule rule = DefaultFlowRule.builder().forDevice(this.deviceId)
                .withSelector(selector.build())
                .withTreatment(treatment.build())
                .withPriority(0)
                .fromApp(appId)
                .makePermanent()
                .forTable(HP_HARDWARE_TABLE)
                .build();

        this.applyRules(true, rule);

        log.info("Installed table {}", HP_HARDWARE_TABLE);
    }

    /**
     * HP software table initialization.
     */
    private void installHPSoftwareTable() {
        log.info("No rules installed in table {}", HP_SOFTWARE_TABLE);
    }


    /**
     * Applies FlowRule.
     * Installs or removes FlowRule.
     *
     * @param install - whether to install or remove rule
     * @param rule    - the rule to be installed or removed
     */
    private void applyRules(boolean install, FlowRule rule) {
        FlowRuleOperations.Builder ops = FlowRuleOperations.builder();

        ops = install ? ops.add(rule) : ops.remove(rule);
        flowRuleService.apply(ops.build(new FlowRuleOperationsContext() {
            @Override
            public void onSuccess(FlowRuleOperations ops) {
                log.trace("Provisioned rule: " + rule.toString());
                log.trace("HP3800 driver: provisioned " + rule.tableId() + " table");
            }

            @Override
            public void onError(FlowRuleOperations ops) {
                log.info("HP3800 driver: failed to provision " + rule.tableId() + " table");
            }
        }));
    }
}

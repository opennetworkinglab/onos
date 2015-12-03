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

import org.onlab.packet.Ethernet;
import org.onosproject.net.flow.DefaultFlowRule;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.FlowRule;
import org.onosproject.net.flow.FlowRuleOperations;
import org.onosproject.net.flow.FlowRuleOperationsContext;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.flowobjective.ForwardingObjective;
import org.onosproject.net.flowobjective.ObjectiveError;
import org.slf4j.Logger;

import java.util.Collection;
import java.util.Collections;

/**
 * Driver for Corsa TTP.
 *
 */
public class CorsaPipeline extends OVSCorsaPipeline {

    private final Logger log = getLogger(getClass());

    @Override
    protected void processVlanMplsTable(boolean install) {
        TrafficSelector.Builder selector = DefaultTrafficSelector.builder();
        TrafficTreatment.Builder treatment = DefaultTrafficTreatment
                .builder();
        FlowRuleOperations.Builder ops = FlowRuleOperations.builder();
        FlowRule rule;
        // corsa uses non-OF-standard way to match on presence of VLAN tags
        selector.matchEthType(Ethernet.TYPE_VLAN);
        treatment.transition(VLAN_TABLE);

        rule = DefaultFlowRule.builder()
                .forDevice(deviceId)
                .withSelector(selector.build())
                .withTreatment(treatment.build())
                .withPriority(CONTROLLER_PRIORITY)
                .fromApp(appId)
                .makePermanent()
                .forTable(VLAN_MPLS_TABLE).build();

        ops = install ? ops.add(rule) : ops.remove(rule);

        flowRuleService.apply(ops.build(new FlowRuleOperationsContext() {
            @Override
            public void onSuccess(FlowRuleOperations ops) {
                log.info("Provisioned vlan/mpls table");
            }

            @Override
            public void onError(FlowRuleOperations ops) {
                log.info(
                        "Failed to provision vlan/mpls table");
            }
        }));
    }

    @Override
    protected Collection<FlowRule> processSpecificSwitch(ForwardingObjective fwd) {
        /* Not supported by until CorsaPipelineV3 */
        log.warn("Vlan switching not supported in corsa-v1 driver");
        fail(fwd, ObjectiveError.UNSUPPORTED);
        return Collections.emptySet();
    }

}

/*
 * Copyright 2018 Open Networking Foundation
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

package org.onosproject.drivers.polatis.snmp;

import com.google.common.collect.Range;
import org.onosproject.core.CoreService;
import org.onosproject.net.PortNumber;
import org.onosproject.net.driver.HandlerBehaviour;
import org.onosproject.net.flow.DefaultFlowRule;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.FlowEntry;
import org.onosproject.net.flow.FlowRule;
import org.onosproject.net.flow.FlowRuleService;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.flow.criteria.Criterion;
import org.onosproject.net.flow.criteria.PortCriterion;
import org.onosproject.net.flow.instructions.Instruction;
import org.onosproject.net.flow.instructions.Instructions;

import org.snmp4j.smi.OID;
import org.snmp4j.smi.UnsignedInteger32;
import org.snmp4j.smi.Variable;
import org.snmp4j.smi.VariableBinding;

import java.util.List;
import java.util.Set;

/**
 * Polatis optical utilities.
 */
public final class PolatisOpticalUtility {

    private static final int DEFAULT_PRIORITY = 88;
    private static final String DEFAULT_APP = "org.onosproject.drivers.polatis.snmp";
    public static final Range<Long> POWER_RANGE = Range.closed(-6000L, 2800L);

    private static final String PORT_ENTRY_OID = ".1.3.6.1.4.1.26592.2.2.2.1.2";
    private static final String PORT_PATCH_OID = PORT_ENTRY_OID + ".1.2";

    private PolatisOpticalUtility() {
    }

    /**
     * Transforms a flow FlowRule object to a variable binding.
     * @param rule FlowRule object
     * @param delete whether it is a delete or edit request
     * @return variable binding
     */
    public static VariableBinding fromFlowRule(FlowRule rule, boolean delete) {
        Set<Criterion> criterions = rule.selector().criteria();
        PortNumber inPort = criterions.stream()
                .filter(c -> c instanceof PortCriterion)
                .map(c -> ((PortCriterion) c).port())
                .findAny()
                .orElse(null);
        long input = inPort.toLong();
        List<Instruction> instructions = rule.treatment().immediate();
        PortNumber outPort = instructions.stream()
                .filter(c -> c instanceof Instructions.OutputInstruction)
                .map(c -> ((Instructions.OutputInstruction) c).port())
                .findAny()
                .orElse(null);
        long output = outPort.toLong();
        OID oid = new OID(PORT_PATCH_OID + "." + input);
        Variable var = new UnsignedInteger32(delete ? 0 : output);
        return new VariableBinding(oid, var);
    }

    /**
     * Finds the FlowRule from flow rule store by the given ports and channel.
     * Returns an extra flow to remove the flow by ONOS if not found.
     * @param behaviour the parent driver handler
     * @param inPort the input port
     * @param outPort the output port
     * @return the flow rule
     */
    public static FlowRule toFlowRule(HandlerBehaviour behaviour, PortNumber inPort,
                                      PortNumber outPort) {
        FlowRuleService service = behaviour.handler().get(FlowRuleService.class);
        Iterable<FlowEntry> entries = service.getFlowEntries(behaviour.data().deviceId());
        // Try to Find the flow from flow rule store.
        for (FlowEntry entry : entries) {
            Set<Criterion> criterions = entry.selector().criteria();
            // input port
            PortNumber ip = criterions.stream()
                    .filter(c -> c instanceof PortCriterion)
                    .map(c -> ((PortCriterion) c).port())
                    .findAny()
                    .orElse(null);
            // output port
            PortNumber op = entry.treatment().immediate().stream()
                    .filter(c -> c instanceof Instructions.OutputInstruction)
                    .map(c -> ((Instructions.OutputInstruction) c).port())
                    .findAny()
                    .orElse(null);
            if (inPort.equals(ip) && outPort.equals(op)) {
                // Find the flow.
                return entry;
            }
        }
        // Cannot find the flow from store. So report an extra flow to remove the flow by ONOS.
        TrafficSelector selector = DefaultTrafficSelector.builder()
                .matchInPort(inPort)
                .build();
        TrafficTreatment treatment = DefaultTrafficTreatment.builder()
                .setOutput(outPort)
                .build();
        return DefaultFlowRule.builder()
                .forDevice(behaviour.data().deviceId())
                .withSelector(selector)
                .withTreatment(treatment)
                .makePermanent()
                .withPriority(DEFAULT_PRIORITY)
                .fromApp(behaviour.handler().get(CoreService.class).getAppId(DEFAULT_APP))
                .build();

    }
}

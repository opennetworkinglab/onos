/*
 * Copyright 2016 Open Networking Foundation
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

package org.onosproject.drivers.oplink;

import com.google.common.collect.Range;
import org.onlab.util.Frequency;
import org.onosproject.core.CoreService;
import org.onosproject.driver.extensions.OplinkAttenuation;
import org.onosproject.net.ChannelSpacing;
import org.onosproject.net.GridType;
import org.onosproject.net.Lambda;
import org.onosproject.net.OchSignalType;
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
import org.onosproject.net.flow.criteria.Criteria;
import org.onosproject.net.flow.criteria.Criterion;
import org.onosproject.net.flow.criteria.OchSignalCriterion;
import org.onosproject.net.flow.criteria.PortCriterion;
import org.onosproject.net.flow.instructions.Instruction;
import org.onosproject.net.flow.instructions.Instructions;

import java.util.List;
import java.util.Set;

/**
 * Oplink optical utilities.
 */
public final class OplinkOpticalUtility {

    // Lambda information supported by device.
    public static final GridType GRID_TYPE = GridType.DWDM;
    public static final int SLOT_GRANULARITY = 4;
    // Channel spacing is 50GHz, 12.5GHz * 4
    public static final ChannelSpacing CHANNEL_SPACING = ChannelSpacing.CHL_50GHZ;
    // Start frequency supported by device.
    public static final Frequency START_CENTER_FREQ = Frequency.ofGHz(191_350);
    // Stop frequency supported by device.
    public static final Frequency STOP_CENTER_FREQ = Frequency.ofGHz(196_100);

    // Power multiply factor, the power accuracy supported by device is 0.01dBm.
    // Transforms the double typed number to long typed power for ONOS.
    public static final int POWER_MULTIPLIER = 100;
    // Attenuation range supported by device, [0, 25.5dB].
    public static final Range<Long> RANGE_ATT = Range.closed(0L, 2550L);
    // General power range for fiber switch, [-60dBm, 60dBm].
    public static final Range<Long> RANGE_GENERAL = Range.closed(-6000L, 6000L);

    // Default attenuation value if the attenuation instruction is not found.
    private static final int DEFAULT_ATT = 0;
    // Default flow priority for an extra flow.
    private static final int DEFAULT_PRIORITY = 88;
    // Default application name.
    private static final String DEFAULT_APP = "org.onosproject.drivers.oplink";

    private OplinkOpticalUtility() {
    }

    /**
     * Transforms a flow FlowRule object to an OplinkCrossConnect object.
     * @param behaviour the parent driver handler
     * @param rule FlowRule object
     * @return cross connect object
     */
    public static OplinkCrossConnect fromFlowRule(HandlerBehaviour behaviour, FlowRule rule) {
        // TrafficSelector
        Set<Criterion> criterions = rule.selector().criteria();
        int channel = criterions.stream()
                .filter(c -> c instanceof OchSignalCriterion)
                .map(c -> ((OchSignalCriterion) c).lambda().spacingMultiplier())
                .findAny()
                .orElse(null);
        PortNumber inPort = criterions.stream()
                .filter(c -> c instanceof PortCriterion)
                .map(c -> ((PortCriterion) c).port())
                .findAny()
                .orElse(null);
        // TrafficTreatment
        List<Instruction> instructions = rule.treatment().immediate();
        PortNumber outPort = instructions.stream()
                .filter(c -> c instanceof Instructions.OutputInstruction)
                .map(c -> ((Instructions.OutputInstruction) c).port())
                .findAny()
                .orElse(null);
        int attenuation = instructions.stream()
                .filter(c -> c instanceof Instructions.ExtensionInstructionWrapper)
                .map(c -> ((Instructions.ExtensionInstructionWrapper) c).extensionInstruction())
                .filter(c -> c instanceof OplinkAttenuation)
                .map(c -> ((OplinkAttenuation) c).getAttenuation())
                .findAny()
                .orElse(DEFAULT_ATT);
        return new OplinkCrossConnect(inPort, outPort, channel, attenuation);
    }

    /**
     * Finds the FlowRule from flow rule store by the given cross connect information.
     * Returns an extra flow to remove the flow by ONOS if not found.
     * @param behaviour the parent driver handler
     * @param cfg cross connect information
     * @return the flow rule
     */
    public static FlowRule toFlowRule(HandlerBehaviour behaviour, OplinkCrossConnect cfg) {
        return toFlowRule(behaviour, cfg.getInPort(), cfg.getOutPort(), cfg.getChannel());
    }

    /**
     * Finds the FlowRule from flow rule store by the given ports and channel.
     * Returns an extra flow to remove the flow by ONOS if not found.
     * @param behaviour the parent driver handler
     * @param inPort the input port
     * @param outPort the output port
     * @param channel the specified channel
     * @return the flow rule
     */
    public static FlowRule toFlowRule(HandlerBehaviour behaviour, PortNumber inPort,
                                      PortNumber outPort, Integer channel) {
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
            // channel
            Integer ch = criterions.stream()
                    .filter(c -> c instanceof OchSignalCriterion)
                    .map(c -> ((OchSignalCriterion) c).lambda().spacingMultiplier())
                    .findAny()
                    .orElse(null);
            // output port
            PortNumber op = entry.treatment().immediate().stream()
                    .filter(c -> c instanceof Instructions.OutputInstruction)
                    .map(c -> ((Instructions.OutputInstruction) c).port())
                    .findAny()
                    .orElse(null);
            if (inPort.equals(ip) && channel.equals(ch) && outPort.equals(op)) {
                // Find the flow.
                return entry;
            }
        }
        // Cannot find the flow from store. So report an extra flow to remove the flow by ONOS.
        TrafficSelector selector = DefaultTrafficSelector.builder()
                .matchInPort(inPort)
                .add(Criteria.matchOchSignalType(OchSignalType.FIXED_GRID))
                .add(Criteria.matchLambda(Lambda.ochSignal(GRID_TYPE, CHANNEL_SPACING, channel, SLOT_GRANULARITY)))
                .build();
        TrafficTreatment treatment = DefaultTrafficTreatment.builder()
                .setOutput(outPort)
                .build();
        return DefaultFlowRule.builder()
                .forDevice(behaviour.data().deviceId())
                .withSelector(selector)
                .withTreatment(treatment)
                .withPriority(DEFAULT_PRIORITY)
                .makePermanent()
                .fromApp(behaviour.handler().get(CoreService.class).getAppId(DEFAULT_APP))
                .build();

    }
}

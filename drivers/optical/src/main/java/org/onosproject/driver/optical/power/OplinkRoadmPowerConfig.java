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

package org.onosproject.driver.optical.power;

import java.util.List;
import java.util.Optional;

import com.google.common.collect.Range;
import org.onosproject.driver.extensions.OplinkAttenuation;
import org.onosproject.net.OchSignal;
import org.onosproject.net.driver.AbstractHandlerBehaviour;
import org.onosproject.net.Direction;
import org.onosproject.net.Port;
import org.onosproject.net.PortNumber;
import org.onosproject.net.behaviour.PowerConfig;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.flow.DefaultFlowRule;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.FlowEntry;
import org.onosproject.net.flow.FlowRule;
import org.onosproject.net.flow.FlowRuleService;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.flow.criteria.Criterion;
import org.onosproject.net.flow.criteria.OchSignalCriterion;
import org.onosproject.net.flow.criteria.PortCriterion;
import org.onosproject.net.flow.instructions.ExtensionTreatment;
import org.onosproject.net.flow.instructions.ExtensionTreatmentType;
import org.onosproject.net.flow.instructions.Instruction;
import org.onosproject.net.flow.instructions.Instructions;
import org.onosproject.net.optical.OpticalAnnotations;
import org.onosproject.openflow.controller.Dpid;
import org.onosproject.openflow.controller.OpenFlowController;
import org.onosproject.openflow.controller.OpenFlowSwitch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Port Power (Gain and attenuation) implementation for Oplink 1-SLOT-8D ROADM.
 *
 * An Oplink ROADM port exposes OchSignal resources.
 * Optical Power can be set at port level or channel/wavelength level (attenuation).
 *
 */

public class OplinkRoadmPowerConfig extends AbstractHandlerBehaviour
                                    implements PowerConfig<Object> {

    private static final int LINE_IN = 1;
    private static final int LINE_OUT = 2;
    private static final int AUX_OUT_1 = 3;
    private static final int AUX_OUT_2 = 4;
    private static final int EXPRESS_OUT_1 = 5;
    private static final int EXPRESS_OUT_2 = 6;
    private static final int EXPRESS_OUT_3 = 7;
    private static final int EXPRESS_OUT_4 = 8;
    private static final int EXPRESS_OUT_5 = 9;
    private static final int EXPRESS_OUT_6 = 10;
    private static final int EXPRESS_OUT_7 = 11;
    private static final int AUX_IN_1 = 12;
    private static final int AUX_IN_2 = 13;
    private static final int EXPRESS_IN_1 = 14;
    private static final int EXPRESS_IN_2 = 15;
    private static final int EXPRESS_IN_3 = 16;
    private static final int EXPRESS_IN_4 = 17;
    private static final int EXPRESS_IN_5 = 18;
    private static final int EXPRESS_IN_6 = 19;
    private static final int EXPRESS_IN_7 = 20;

    protected final Logger log = LoggerFactory.getLogger(getClass());

    // Component type
    private enum Type {
        NONE,
        PORT,
        CHANNEL
    }

    // Get the type if component is valid
    private Type getType(Object component) {
        if (component == null || component instanceof Direction) {
            return Type.PORT;
        } else if (component instanceof OchSignal) {
            return Type.CHANNEL;
        } else {
            return Type.NONE;
        }
    }

    private OpenFlowSwitch getOpenFlowDevice() {
        final OpenFlowController controller = this.handler().get(OpenFlowController.class);
        final Dpid dpid = Dpid.dpid(this.data().deviceId().uri());
        OpenFlowSwitch sw = controller.getSwitch(dpid);
        if (sw == null || !sw.isConnected()) {
            return null;
        } else {
            return sw;
        }
    }

    // Find matching flow on device
    private FlowEntry findFlow(PortNumber portNum, OchSignal och) {
        FlowRuleService service = this.handler().get(FlowRuleService.class);
        Iterable<FlowEntry> flowEntries = service.getFlowEntries(this.data().deviceId());

        // Return first matching flow
        for (FlowEntry entry : flowEntries) {
            TrafficSelector selector = entry.selector();
            OchSignalCriterion entrySigid =
                    (OchSignalCriterion) selector.getCriterion(Criterion.Type.OCH_SIGID);
            if (entrySigid != null && och.equals(entrySigid.lambda())) {
                PortCriterion entryPort =
                        (PortCriterion) selector.getCriterion(Criterion.Type.IN_PORT);
                if (entryPort != null && portNum.equals(entryPort.port())) {
                    return entry;
                }
            }
        }
        log.warn("No matching flow found");
        return null;
    }

    @Override
    public Optional<Long> getTargetPower(PortNumber portNum, Object component) {
        Long returnVal = null;
        // Check if switch is connected, otherwise do not return value in store,
        // which is obsolete.
        if (getOpenFlowDevice() != null) {
            switch (getType(component)) {
                case PORT:
                    // Will be implemented in the future.
                    break;
                case CHANNEL:
                    returnVal = getChannelAttenuation(portNum, (OchSignal) component);
                    break;
                default:
                    break;
            }
        }
        return Optional.ofNullable(returnVal);
    }

    @Override
    public Optional<Long> currentPower(PortNumber portNum, Object component) {
        Long returnVal = null;
        // Check if switch is connected, otherwise do not return value in store,
        // which is obsolete.
        if (getOpenFlowDevice() != null) {
            switch (getType(component)) {
                case PORT:
                    returnVal = getCurrentPortPower(portNum);
                    break;
                case CHANNEL:
                    returnVal = getCurrentChannelPower(portNum, (OchSignal) component);
                    break;
                default:
                    break;
            }
        }
        return Optional.ofNullable(returnVal);
    }

    @Override
    public void setTargetPower(PortNumber portNum, Object component, long power) {
        if (getOpenFlowDevice() != null) {
            switch (getType(component)) {
                case PORT:
                    setTargetPortPower(portNum, power);
                    break;
                case CHANNEL:
                    setChannelAttenuation(portNum, (OchSignal) component, power);
                    break;
                default:
                    break;
            }
        } else {
            log.warn("OpenFlow handshaker driver not found or device is not connected");
        }
    }

    @Override
    public Optional<Range<Long>> getTargetPowerRange(PortNumber port, Object component) {
        Range<Long> range = null;
        switch (getType(component)) {
            case PORT:
                range = getTargetPortPowerRange(port);
                break;
            case CHANNEL:
                range = getChannelAttenuationRange(port);
                break;
            default:
                break;
        }
        return Optional.ofNullable(range);
    }

    @Override
    public Optional<Range<Long>> getInputPowerRange(PortNumber port, Object component) {
        Range<Long> range = null;
        switch (getType(component)) {
            case PORT:
                range = getInputPortPowerRange(port);
                break;
            default:
                break;
        }
        return Optional.ofNullable(range);
    }

    private Long getChannelAttenuation(PortNumber portNum, OchSignal och) {
        FlowEntry flowEntry = findFlow(portNum, och);
        if (flowEntry != null) {
            List<Instruction> instructions = flowEntry.treatment().allInstructions();
            for (Instruction ins : instructions) {
                if (ins.type() == Instruction.Type.EXTENSION) {
                    ExtensionTreatment ext = ((Instructions.ExtensionInstructionWrapper) ins).extensionInstruction();
                    if (ext.type() == ExtensionTreatmentType.ExtensionTreatmentTypes.OPLINK_ATTENUATION.type()) {
                        return (long) ((OplinkAttenuation) ext).getAttenuation();
                    }
                }
            }
        }
        return null;
    }

    private Long getCurrentPortPower(PortNumber portNum) {
        DeviceService deviceService = this.handler().get(DeviceService.class);
        Port port = deviceService.getPort(this.data().deviceId(), portNum);
        if (port != null) {
            String currentPower = port.annotations().value(OpticalAnnotations.CURRENT_POWER);
            if (currentPower != null) {
                return Long.valueOf(currentPower);
            }
        }
        return null;
    }

    private Long getCurrentChannelPower(PortNumber portNum, OchSignal och) {
        FlowEntry flowEntry = findFlow(portNum, och);
        if (flowEntry != null) {
            // TODO put somewhere else if possible
            // We put channel power in packets
            return flowEntry.packets();
        }
        return null;
    }

    private void setTargetPortPower(PortNumber portNum, long power) {
        OpenFlowSwitch device = getOpenFlowDevice();
        device.sendMsg(device.factory().buildOplinkPortPowerSet()
                .setXid(0)
                .setPort((int) portNum.toLong())
                .setPowerValue((int) power)
                .build());
    }

    private void setChannelAttenuation(PortNumber portNum, OchSignal och, long power) {
        FlowEntry flowEntry = findFlow(portNum, och);
        if (flowEntry != null) {
            List<Instruction> instructions = flowEntry.treatment().allInstructions();
            for (Instruction ins : instructions) {
                if (ins.type() == Instruction.Type.EXTENSION) {
                    ExtensionTreatment ext = ((Instructions.ExtensionInstructionWrapper) ins).extensionInstruction();
                    if (ext.type() == ExtensionTreatmentType.ExtensionTreatmentTypes.OPLINK_ATTENUATION.type()) {
                        ((OplinkAttenuation) ext).setAttenuation((int) power);
                        FlowRuleService service = this.handler().get(FlowRuleService.class);
                        service.applyFlowRules(flowEntry);
                        return;
                    }
                }
            }
            addAttenuation(flowEntry, power);
        } else {
            log.warn("Target channel power not set");
        }
    }

    // Replace flow with new flow containing Oplink attenuation extension instruction. Also resets
    // metrics.
    private void addAttenuation(FlowEntry flowEntry, long power) {
        FlowRule.Builder flowBuilder = new DefaultFlowRule.Builder();
        flowBuilder.withCookie(flowEntry.id().value());
        flowBuilder.withPriority(flowEntry.priority());
        flowBuilder.forDevice(flowEntry.deviceId());
        flowBuilder.forTable(flowEntry.tableId());
        if (flowEntry.isPermanent()) {
            flowBuilder.makePermanent();
        } else {
            flowBuilder.makeTemporary(flowEntry.timeout());
        }

        flowBuilder.withSelector(flowEntry.selector());

        // Copy original instructions and add attenuation instruction
        TrafficTreatment.Builder treatmentBuilder = DefaultTrafficTreatment.builder();
        flowEntry.treatment().allInstructions().forEach(ins -> treatmentBuilder.add(ins));
        treatmentBuilder.add(Instructions.extension(new OplinkAttenuation((int) power), this.data().deviceId()));
        flowBuilder.withTreatment(treatmentBuilder.build());

        FlowRuleService service = this.handler().get(FlowRuleService.class);
        service.applyFlowRules(flowBuilder.build());
    }

    // Returns the acceptable target range for an output Port, null otherwise
    private Range<Long> getTargetPortPowerRange(PortNumber port) {
        Range<Long> range = null;
        long num = port.toLong();
        if (num == LINE_OUT) {
            range = Range.closed(100L, 2040L);
        } else if (num >= AUX_OUT_1 && num <= EXPRESS_OUT_7) {
            range = Range.closed(-680L, 1530L);
        }
        return range;
    }

    // Returns the acceptable attenuation range for a connection (represented as
    // a flow with attenuation instruction). Port can be either the input or
    // output port of the connection. Returns null if the connection does not
    // support attenuation.
    private Range<Long> getChannelAttenuationRange(PortNumber port) {
        Range<Long> range = null;
        long num = port.toLong();
        // Only connections from AuxIn to LineOut or ExpressIn to LineOut support
        // attenuation.
        if (num == LINE_OUT ||
            num >= AUX_IN_1 && num <= EXPRESS_IN_7) {
            range = Range.closed(0L, 2550L);
        }
        return range;
    }

    // Returns the working input power range for an input port, null if the port
    // is not an input port.
    private Range<Long> getInputPortPowerRange(PortNumber port) {
        Range<Long> range = null;
        long portNum = port.toLong();
        if (portNum == LINE_IN) {
            // TODO implement support for IR and ER range
            // only supports LR right now
            range = Range.closed(-2600L, 540L);
        } else if (portNum == AUX_IN_1 || portNum == AUX_IN_2) {
            range = Range.closed(-1250L, 1590L);
        } else if (portNum >= EXPRESS_IN_1 && portNum <= EXPRESS_IN_7) {
            range = Range.closed(-1420L, 1420L);
        }
        return range;
    }
}

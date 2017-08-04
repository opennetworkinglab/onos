/*
 * Copyright 2016-present Open Networking Foundation
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

import java.util.concurrent.atomic.AtomicInteger;
import java.util.List;

import com.google.common.collect.Range;

import org.onosproject.driver.extensions.OplinkAttenuation;
import org.onosproject.net.OchSignal;
import org.onosproject.net.Direction;
import org.onosproject.net.Port;
import org.onosproject.net.PortNumber;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.driver.DriverHandler;
import org.onosproject.net.driver.HandlerBehaviour;
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
import org.onosproject.openflow.controller.OpenFlowOpticalSwitch;
import org.onosproject.openflow.controller.OpenFlowSwitch;
import org.onosproject.openflow.controller.PortDescPropertyType;
import org.projectfloodlight.openflow.protocol.OFObject;
import org.projectfloodlight.openflow.protocol.OFPortOptical;

import org.slf4j.Logger;

import static org.slf4j.LoggerFactory.getLogger;
import static org.onosproject.net.Device.Type;

/**
 * Oplink power config utility.
 */
public class OplinkPowerConfigUtil {

    // Parent driver handler behaviour
    private HandlerBehaviour behaviour;
    // Transaction id to use.
    private final AtomicInteger xidCounter = new AtomicInteger(0);
    // Log
    private final Logger log = getLogger(getClass());

    // Component type
    private enum ComponentType {
        NONE,
        PORT,
        CHANNEL
    }

    // Port properties for oplink devices, currently supports EDFA and ROADM.
    // This type is mapped to OFPortDescPropOpticalTransport#getPortType() value.
    private enum PortDescType {
        NONE,
        PA_LINE_IN,
        PA_LINE_OUT,
        BA_LINE_IN,
        BA_LINE_OUT,
        EXP_IN,
        EXP_OUT,
        AUX_IN,
        AUX_OUT,
    }

    /**
     * Power threshold of each port, in 0.01 dB
     * Note:
     * These threshold configurations are just in use for a short time.
     * In the future, the power threshold would be obtained from physical device.
     */
    // EDFA
    private static final long EDFA_POWER_IN_WEST_LOW_THRES = -1900L;
    private static final long EDFA_POWER_IN_WEST_HIGH_THRES = 0L;
    private static final long EDFA_POWER_IN_EAST_LOW_THRES = -3100L;
    private static final long EDFA_POWER_IN_EAST_HIGH_THRES = 700L;
    private static final long EDFA_POWER_OUT_LOW_THRES = 0L;
    private static final long EDFA_POWER_OUT_HIGH_THRES = 1900L;
    // ROADM
    private static final long ROADM_POWER_LINE_IN_LOW_THRES = -3000L;
    private static final long ROADM_POWER_LINE_IN_HIGH_THRES = 2350L;
    private static final long ROADM_POWER_LINE_OUT_LOW_THRES = 0L;
    private static final long ROADM_POWER_LINE_OUT_HIGH_THRES = 2350L;
    private static final long ROADM_POWER_OTHER_IN_LOW_THRES = -1500L;
    private static final long ROADM_POWER_OTHER_IN_HIGH_THRES = 2000L;
    private static final long ROADM_POWER_OTHER_OUT_LOW_THRES = -600L;
    private static final long ROADM_POWER_OTHER_OUT_HIGH_THRES = 1500L;
    private static final long ROADM_MIN_ATTENUATION = 0L;
    private static final long ROADM_MAX_ATTENUATION = 2500L;
    // SWITCH
    private static final long SWITCH_POWER_LOW_THRES = -6000L;
    private static final long SWITCH_POWER_HIGH_THRES = 6000L;

    /**
     * Create a new OplinkPowerConfigUtil.
     * @param behaviour driver handler behaviour
     */
    public OplinkPowerConfigUtil(HandlerBehaviour behaviour) {
        this.behaviour = behaviour;
    }

    /**
     * Obtains specified port/channel target power.
     *
     * @param port the port number
     * @param component the port component
     * @return target power value in .01 dBm
     */
    public Long getTargetPower(PortNumber port, Object component) {
        switch (getComponentType(component)) {
            case PORT:
                return getPortPower(port, OpticalAnnotations.TARGET_POWER);
            case CHANNEL:
                return getChannelAttenuation(port, (OchSignal) component);
            default:
                return null;
        }
    }

    /**
     * Obtains specified port/channel current power.
     *
     * @param port the port number
     * @param component the port component
     * @return current power value in .01 dBm
     */
    public Long getCurrentPower(PortNumber port, Object component) {
        switch (getComponentType(component)) {
            case PORT:
                return getPortPower(port, OpticalAnnotations.CURRENT_POWER);
            case CHANNEL:
                return getCurrentChannelPower(port, (OchSignal) component);
            default:
                return null;
        }
    }

    /**
     * Sets specified port target power or channel attenuation.
     *
     * @param port the port number
     * @param component the port component
     * @param power target power in .01 dBm
     */
    public void setTargetPower(PortNumber port, Object component, long power) {
        switch (getComponentType(component)) {
            case PORT:
                setPortPower(port, power);
                break;
            case CHANNEL:
                setChannelAttenuation(port, (OchSignal) component, power);
               break;
            default:
                break;
        }
    }

    /**
     * Returns the acceptable target range for an output port/channel, null otherwise.
     *
     * @param port the port number
     * @param component the port component
     * @return power range
     */
    public Range<Long> getTargetPowerRange(PortNumber port, Object component) {
        switch (getComponentType(component)) {
            case PORT:
                return getTargetPortPowerRange(port);
            case CHANNEL:
                return getChannelAttenuationRange(port);
            default:
                return null;
        }
    }

    /**
     * Returns the working input power range for an input port, null otherwise.
     *
     * @param port the port number
     * @param component the port component
     * @return power range
     */
    public Range<Long> getInputPowerRange(PortNumber port, Object component) {
        switch (getComponentType(component)) {
            case PORT:
                return getInputPortPowerRange(port);
            default:
                return null;
        }
    }

    /**
     * Returns specified component type.
     *
     * @param component the port component
     * @return component type
     */
    private ComponentType getComponentType(Object component) {
        if (component == null || component instanceof Direction) {
            return ComponentType.PORT;
        } else if (component instanceof OchSignal) {
            return ComponentType.CHANNEL;
        }
        return ComponentType.NONE;
    }

    /**
     * Returns current switch known to this OF controller.
     *
     * @return current switch
     */
    private OpenFlowSwitch getOpenFlowDevice() {
        final DriverHandler handler = behaviour.handler();
        final OpenFlowController controller = handler.get(OpenFlowController.class);
        final Dpid dpid = Dpid.dpid(handler.data().deviceId().uri());
        OpenFlowSwitch sw = controller.getSwitch(dpid);
        if (sw == null || !sw.isConnected()) {
            log.warn("OpenFlow handshaker driver not found or device is not connected, dpid = {}", dpid);
            return null;
        }
        return sw;
    }

    /**
     * Find oplink port description type from optical ports.
     *
     * @param opsw switch
     * @param portNum the port number
     * @return port oplink port description type
     */
    private PortDescType getPortDescType(OpenFlowOpticalSwitch opsw, PortNumber portNum) {
        for (PortDescPropertyType type : opsw.getPortTypes()) {
            List<? extends OFObject> portsOf = opsw.getPortsOf(type);
            for (OFObject op : portsOf) {
                if (op instanceof OFPortOptical) {
                    OFPortOptical opticalPort = (OFPortOptical) op;
                    if ((long) opticalPort.getPortNo().getPortNumber() == portNum.toLong()) {
                        return PortDescType.values()[opticalPort.getDesc().get(0).getPortType()];
                    }
                }
            }
        }
        return PortDescType.NONE;
    }

    /**
     * Returns the target port power range.
     *
     * @param portNum the port number
     * @return power range
     */
    private Range<Long> getTargetPortPowerRange(PortNumber portNum) {
        OpenFlowSwitch ofs = getOpenFlowDevice();
        if (ofs == null) {
            return null;
        }
        PortDescType portType = getPortDescType((OpenFlowOpticalSwitch) ofs, portNum);
        Type devType = ofs.deviceType();
        // FIXME
        // Short time hard code.
        // The power range will be obtained from physical device in the future.
        switch (devType) {
            case OPTICAL_AMPLIFIER:
                if (portType == PortDescType.PA_LINE_OUT || portType == PortDescType.BA_LINE_OUT) {
                    return Range.closed(EDFA_POWER_OUT_LOW_THRES, EDFA_POWER_OUT_HIGH_THRES);
                }
                break;
            case ROADM:
                if (portType == PortDescType.PA_LINE_OUT) {
                    return Range.closed(ROADM_POWER_LINE_OUT_LOW_THRES, ROADM_POWER_LINE_OUT_HIGH_THRES);
                } else if (portType == PortDescType.EXP_OUT || portType == PortDescType.AUX_OUT) {
                    return Range.closed(ROADM_POWER_OTHER_OUT_LOW_THRES, ROADM_POWER_OTHER_OUT_HIGH_THRES);
                }
                break;
            case FIBER_SWITCH:
                return Range.closed(SWITCH_POWER_LOW_THRES, SWITCH_POWER_HIGH_THRES);
            default:
                log.warn("Unexpected device type: {}", devType);
                break;
        }
        // Unexpected port or device type. Do not need warning here for port polling.
        return null;
    }

    /**
     * Returns the input port power range.
     *
     * @param portNum the port number
     * @return power range
     */
    private Range<Long> getInputPortPowerRange(PortNumber portNum) {
        OpenFlowSwitch ofs = getOpenFlowDevice();
        if (ofs == null) {
            return null;
        }
        PortDescType portType = getPortDescType((OpenFlowOpticalSwitch) ofs, portNum);
        Type devType = ofs.deviceType();
        // FIXME
        // Short time hard code.
        // The port type and power range will be obtained from physical device in the future.
        switch (devType) {
            case OPTICAL_AMPLIFIER:
                if (portType == PortDescType.PA_LINE_IN) {
                    return Range.closed(EDFA_POWER_IN_WEST_LOW_THRES, EDFA_POWER_IN_WEST_HIGH_THRES);
                } else if (portType == PortDescType.BA_LINE_IN) {
                    return Range.closed(EDFA_POWER_IN_EAST_LOW_THRES, EDFA_POWER_IN_EAST_HIGH_THRES);
                }
                break;
            case ROADM:
                if (portType == PortDescType.PA_LINE_IN) {
                    return Range.closed(ROADM_POWER_LINE_IN_LOW_THRES, ROADM_POWER_LINE_IN_HIGH_THRES);
                } else if (portType == PortDescType.EXP_IN || portType == PortDescType.AUX_IN) {
                    return Range.closed(ROADM_POWER_OTHER_IN_LOW_THRES, ROADM_POWER_OTHER_IN_HIGH_THRES);
                }
                break;
            case FIBER_SWITCH:
                return Range.closed(SWITCH_POWER_LOW_THRES, SWITCH_POWER_HIGH_THRES);
            default:
                log.warn("Unexpected device type: {}", devType);
                break;
        }
        // Unexpected port or device type. Do not need warning here for port polling.
        return null;
    }

    /**
     * Returns the acceptable attenuation range for a connection (represented as
     * a flow with attenuation instruction). Port can be either the input or
     * output port of the connection. Returns null if the connection does not
     * support attenuation.
     *
     * @param portNum the port number
     * @return attenuation range
     */
    private Range<Long> getChannelAttenuationRange(PortNumber portNum) {
        OpenFlowSwitch ofs = getOpenFlowDevice();
        if (ofs == null) {
            return null;
        }
        if (ofs.deviceType() != Type.ROADM) {
            return null;
        }
        PortDescType portType = getPortDescType((OpenFlowOpticalSwitch) ofs, portNum);
        // Short time hard code.
        // The port type and attenuation range will be obtained from physical device in the future.
        if (portType == PortDescType.PA_LINE_OUT || portType == PortDescType.EXP_IN ||
                portType == PortDescType.AUX_IN) {
            return Range.closed(ROADM_MIN_ATTENUATION, ROADM_MAX_ATTENUATION);
        }
        // Unexpected port. Do not need warning here for port polling.
        return null;
    }

    /**
     * Find specified port power from port description.
     *
     * @param portNum the port number
     * @param annotation annotation in port description
     * @return power value in 0.01 dBm
     */
    private Long getPortPower(PortNumber portNum, String annotation) {
        // Check if switch is connected, otherwise do not return value in store, which is obsolete.
        if (getOpenFlowDevice() == null) {
            // Warning already exists in method getOpenFlowDevice()
            return null;
        }
        final DriverHandler handler = behaviour.handler();
        DeviceService deviceService = handler.get(DeviceService.class);
        Port port = deviceService.getPort(handler.data().deviceId(), portNum);
        if (port == null) {
            log.warn("Unexpected port: {}", portNum);
            return null;
        }
        String power = port.annotations().value(annotation);
        if (power == null) {
            // Do not need warning here for port polling.
            log.debug("Cannot get {} from port {}.", annotation, portNum);
            return null;
        }
        return Long.valueOf(power);
    }

    /**
     * Sets specified port power value.
     *
     * @param portNum the port number
     * @param power power value
     */
    private void setPortPower(PortNumber portNum, long power) {
        OpenFlowSwitch device = getOpenFlowDevice();
        // Check if switch is connected
        if (device == null) {
            return;
        }
        device.sendMsg(device.factory().buildOplinkPortPowerSet()
                .setXid(xidCounter.getAndIncrement())
                .setPort((int) portNum.toLong())
                .setPowerValue((int) power)
                .build());
    }

    /**
     * Gets specified channel attenuation.
     *
     * @param portNum the port number
     * @param och channel signal
     * @return atteuation in 0.01 dB
     */
    private Long getChannelAttenuation(PortNumber portNum, OchSignal och) {
        FlowEntry flowEntry = findFlow(portNum, och);
        if (flowEntry == null) {
            return null;
        }
        List<Instruction> instructions = flowEntry.treatment().allInstructions();
        for (Instruction ins : instructions) {
            if (ins.type() != Instruction.Type.EXTENSION) {
                continue;
            }
            ExtensionTreatment ext = ((Instructions.ExtensionInstructionWrapper) ins).extensionInstruction();
            if (ext.type() == ExtensionTreatmentType.ExtensionTreatmentTypes.OPLINK_ATTENUATION.type()) {
                return (long) ((OplinkAttenuation) ext).getAttenuation();
            }
        }
        return null;
    }

    /**
     * Sets specified channle attenuation.
     *
     * @param portNum the port number
     * @param och channel signal
     * @param power attenuation in 0.01 dB
     */
    private void setChannelAttenuation(PortNumber portNum, OchSignal och, long power) {
        FlowEntry flowEntry = findFlow(portNum, och);
        if (flowEntry == null) {
            log.warn("Target channel power not set");
            return;
        }
        final DriverHandler handler = behaviour.handler();
        for (Instruction ins : flowEntry.treatment().allInstructions()) {
            if (ins.type() != Instruction.Type.EXTENSION) {
                continue;
            }
            ExtensionTreatment ext = ((Instructions.ExtensionInstructionWrapper) ins).extensionInstruction();
            if (ext.type() == ExtensionTreatmentType.ExtensionTreatmentTypes.OPLINK_ATTENUATION.type()) {
                ((OplinkAttenuation) ext).setAttenuation((int) power);
                FlowRuleService service = handler.get(FlowRuleService.class);
                service.applyFlowRules(flowEntry);
                return;
            }
        }
        addAttenuation(flowEntry, power);
    }

    /**
     * Gets specified channle current power.
     *
     * @param portNum the port number
     * @param och channel signal
     * @return power value in 0.01 dBm
     */
    private Long getCurrentChannelPower(PortNumber portNum, OchSignal och) {
        FlowEntry flowEntry = findFlow(portNum, och);
        if (flowEntry != null) {
            // TODO put somewhere else if possible
            // We put channel power in packets
            return flowEntry.packets();
        }
        return null;
    }

    /**
     * Find matching flow on device.
     *
     * @param portNum the port number
     * @param och channel signal
     * @return flow entry
     */
    private FlowEntry findFlow(PortNumber portNum, OchSignal och) {
        final DriverHandler handler = behaviour.handler();
        FlowRuleService service = handler.get(FlowRuleService.class);
        Iterable<FlowEntry> flowEntries = service.getFlowEntries(handler.data().deviceId());

        // Return first matching flow
        for (FlowEntry entry : flowEntries) {
            TrafficSelector selector = entry.selector();
            OchSignalCriterion entrySigid =
                    (OchSignalCriterion) selector.getCriterion(Criterion.Type.OCH_SIGID);
            // Check channel
            if (entrySigid != null && och.equals(entrySigid.lambda())) {
                // Check input port
                PortCriterion entryPort =
                        (PortCriterion) selector.getCriterion(Criterion.Type.IN_PORT);
                if (entryPort != null && portNum.equals(entryPort.port())) {
                    return entry;
                }

                // Check output port
                TrafficTreatment treatment = entry.treatment();
                for (Instruction instruction : treatment.allInstructions()) {
                    if (instruction.type() == Instruction.Type.OUTPUT &&
                        ((Instructions.OutputInstruction) instruction).port().equals(portNum)) {
                        return entry;
                    }
                }
            }
        }
        log.warn("No matching flow found");
        return null;
    }

    /**
     * Replace flow with new flow containing Oplink attenuation extension instruction. Also resets metrics.
     *
     * @param flowEntry flow entry
     * @param power power value
     */
    private void addAttenuation(FlowEntry flowEntry, long power) {
        FlowRule.Builder flowBuilder = new DefaultFlowRule.Builder()
                .withCookie(flowEntry.id().value())
                .withPriority(flowEntry.priority())
                .forDevice(flowEntry.deviceId())
                .forTable(flowEntry.tableId());
        if (flowEntry.isPermanent()) {
            flowBuilder.makePermanent();
        } else {
            flowBuilder.makeTemporary(flowEntry.timeout());
        }
        flowBuilder.withSelector(flowEntry.selector());
        // Copy original instructions and add attenuation instruction
        TrafficTreatment.Builder treatmentBuilder = DefaultTrafficTreatment.builder();
        flowEntry.treatment().allInstructions().forEach(ins -> treatmentBuilder.add(ins));
        final DriverHandler handler = behaviour.handler();
        treatmentBuilder.add(Instructions.extension(new OplinkAttenuation((int) power), handler.data().deviceId()));
        flowBuilder.withTreatment(treatmentBuilder.build());

        FlowRuleService service = handler.get(FlowRuleService.class);
        service.applyFlowRules(flowBuilder.build());
    }
}

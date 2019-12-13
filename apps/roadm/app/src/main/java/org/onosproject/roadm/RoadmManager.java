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
package org.onosproject.roadm;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Range;
import org.onlab.util.Frequency;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.net.ChannelSpacing;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Direction;
import org.onosproject.net.ModulationScheme;
import org.onosproject.net.OchSignal;
import org.onosproject.net.Port;
import org.onosproject.net.OchSignalType;
import org.onosproject.net.PortNumber;
import org.onosproject.net.behaviour.LambdaQuery;
import org.onosproject.net.behaviour.ModulationConfig;
import org.onosproject.net.behaviour.PowerConfig;
import org.onosproject.net.behaviour.protection.ProtectedTransportEndpointState;
import org.onosproject.net.behaviour.protection.ProtectionConfigBehaviour;
import org.onosproject.net.behaviour.protection.TransportEndpointState;
import org.onosproject.net.device.DeviceEvent;
import org.onosproject.net.device.DeviceListener;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.flow.DefaultFlowRule;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.FlowEntry;
import org.onosproject.net.flow.FlowId;
import org.onosproject.net.flow.FlowRule;
import org.onosproject.net.flow.FlowRuleService;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.flow.criteria.Criteria;
import org.onosproject.net.flow.instructions.Instruction;
import org.onosproject.net.flow.instructions.Instructions;
import org.onosproject.net.flow.instructions.L0ModificationInstruction;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.StreamSupport;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.onosproject.net.optical.OpticalAnnotations.INPUT_PORT_STATUS;
import static org.onosproject.roadm.RoadmUtil.OPS_OPT_AUTO;
import static org.onosproject.roadm.RoadmUtil.OPS_OPT_FORCE;
import static org.onosproject.roadm.RoadmUtil.OPS_OPT_MANUAL;

/**
 * Application for monitoring and configuring ROADM devices.
 */
@Component(immediate = true, service = RoadmService.class)
public class RoadmManager implements RoadmService {

    private static final String APP_NAME = "org.onosproject.roadm";
    private ApplicationId appId;

    private final Logger log = LoggerFactory.getLogger(getClass());

    private DeviceListener deviceListener = new InternalDeviceListener();

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected RoadmStore roadmStore;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected DeviceService deviceService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected FlowRuleService flowRuleService;

    @Activate
    protected void activate() {
        appId = coreService.registerApplication(APP_NAME);
        deviceService.addListener(deviceListener);
        initDevices();

        log.info("Started");
    }

    @Deactivate
    protected void deactivate() {
        deviceService.removeListener(deviceListener);

        log.info("Stopped");
    }

    @Deprecated
    @Override
    public void setProtectionSwitchWorkingPath(DeviceId deviceId, int index) {
        checkNotNull(deviceId);
        ProtectionConfigBehaviour behaviour = getProtectionConfig(deviceId);
        if (behaviour == null) {
            return;
        }
        Map<ConnectPoint, ProtectedTransportEndpointState> map = getProtectionSwitchStates(behaviour);
        if (map == null) {
            log.warn("Failed to get protected transport endpoint state in device {}", deviceId);
            return;
        }
        if (map.isEmpty()) {
            log.warn("No protected transport endpoint state found in device {}", deviceId);
            return;
        }
        behaviour.switchToManual(map.keySet().toArray(new ConnectPoint[0])[0], index);
    }

    @Deprecated
    @Override
    public String getProtectionSwitchPortState(DeviceId deviceId, PortNumber portNumber) {
        checkNotNull(deviceId);
        ProtectionConfigBehaviour behaviour = getProtectionConfig(deviceId);
        if (behaviour == null) {
            return null;
        }
        Map<ConnectPoint, ProtectedTransportEndpointState> map = getProtectionSwitchStates(behaviour);
        if (map == null) {
            log.warn("Failed to get protected transport endpoint state in device {}", deviceId);
            return null;
        }
        for (ProtectedTransportEndpointState state : map.values()) {
            for (TransportEndpointState element : state.pathStates()) {
                if (element.description().output().connectPoint().port().equals(portNumber)) {
                    return element.attributes().get(INPUT_PORT_STATUS);
                }
            }
        }
        // Do not need warning here for port polling.
        log.debug("Unable to get port status, device: {}, port: {}", deviceId, portNumber);
        return null;
    }

    @Override
    public void configProtectionSwitch(DeviceId deviceId, String operation, ConnectPoint identifier, int index) {
        checkNotNull(deviceId);
        ProtectionConfigBehaviour behaviour = getProtectionConfig(deviceId);
        if (behaviour == null) {
            return;
        }
        // automatic operation
        if (OPS_OPT_AUTO.equals(operation)) {
            behaviour.switchToAutomatic(identifier);
            return;
        }
        // force or manual operation
        if (OPS_OPT_MANUAL.equals(operation)) {
            behaviour.switchToManual(identifier, index);
        } else if (OPS_OPT_FORCE.equals(operation)) {
            behaviour.switchToForce(identifier, index);
        }
    }

    @Override
    public Map<ConnectPoint, ProtectedTransportEndpointState> getProtectionSwitchStates(DeviceId deviceId) {
        checkNotNull(deviceId);
        ProtectionConfigBehaviour behaviour = getProtectionConfig(deviceId);
        if (behaviour == null) {
            return ImmutableMap.of();
        }
        return getProtectionSwitchStates(behaviour);
    }


    @Override
    public void setTargetPortPower(DeviceId deviceId, PortNumber portNumber, double power) {
        checkNotNull(deviceId);
        checkNotNull(portNumber);
        PowerConfig<Object> powerConfig = getPowerConfig(deviceId);
        if (powerConfig != null) {
            roadmStore.setTargetPower(deviceId, portNumber, power);
            powerConfig.setTargetPower(portNumber, Direction.ALL, power);
        } else {
            log.warn("Unable to set target port power for device {}", deviceId);
        }
    }

    @Override
    public Double getTargetPortPower(DeviceId deviceId, PortNumber portNumber) {
        checkNotNull(deviceId);
        checkNotNull(portNumber);
        // Request target port power when it doesn't exist. Inactive updating mode.
        Double power = roadmStore.getTargetPower(deviceId, portNumber);
        if (power == null) {
            return syncTargetPortPower(deviceId, portNumber);
        }
        return power;
    }

    @Override
    public Double syncTargetPortPower(DeviceId deviceId, PortNumber portNumber) {
        checkNotNull(deviceId);
        checkNotNull(portNumber);
        PowerConfig<Object> powerConfig = getPowerConfig(deviceId);
        if (powerConfig != null) {
            Optional<Double> pl = powerConfig.getTargetPower(portNumber, Direction.ALL);
            if (pl.isPresent()) {
                roadmStore.setTargetPower(deviceId, portNumber, pl.get());
                return pl.get();
            } else {
                roadmStore.removeTargetPower(deviceId, portNumber);
            }
        }
        return null;
    }

    @Override
    public void setAttenuation(DeviceId deviceId, PortNumber portNumber,
                               OchSignal ochSignal, double attenuation) {
        checkNotNull(deviceId);
        checkNotNull(portNumber);
        checkNotNull(ochSignal);
        PowerConfig<Object> powerConfig = getPowerConfig(deviceId);
        if (powerConfig != null) {
            powerConfig.setTargetPower(portNumber, ochSignal, attenuation);
        } else {
            log.warn("Cannot set attenuation for channel index {} on device {}",
                    ochSignal.spacingMultiplier(), deviceId);
        }
    }

    @Override
    public Double getAttenuation(DeviceId deviceId, PortNumber portNumber, OchSignal ochSignal) {
        checkNotNull(deviceId);
        checkNotNull(portNumber);
        checkNotNull(ochSignal);
        PowerConfig<Object> powerConfig = getPowerConfig(deviceId);
        if (powerConfig != null) {
            Optional<Double> attenuation = powerConfig.getTargetPower(portNumber, ochSignal);
            if (attenuation.isPresent()) {
                return attenuation.get();
            }
        }
        return null;
    }

    @Override
    public Double getCurrentPortPower(DeviceId deviceId, PortNumber portNumber) {
        checkNotNull(deviceId);
        checkNotNull(portNumber);
        PowerConfig<Object> powerConfig = getPowerConfig(deviceId);
        if (powerConfig != null) {
            Optional<Double> currentPower = powerConfig.currentPower(portNumber, Direction.ALL);
            if (currentPower.isPresent()) {
                return currentPower.get();
            }
        }
        return null;
    }

    @Override
    public Double getCurrentChannelPower(DeviceId deviceId, PortNumber portNumber, OchSignal ochSignal) {
        checkNotNull(deviceId);
        checkNotNull(portNumber);
        checkNotNull(ochSignal);
        PowerConfig<Object> powerConfig = getPowerConfig(deviceId);
        if (powerConfig != null) {
            Optional<Double> currentPower = powerConfig.currentPower(portNumber, ochSignal);
            if (currentPower.isPresent()) {
                return currentPower.get();
            }
        }
        return null;
    }

    @Override
    public Set<OchSignal> queryLambdas(DeviceId deviceId, PortNumber portNumber) {
        checkNotNull(deviceId);
        checkNotNull(portNumber);
        LambdaQuery lambdaQuery = getLambdaQuery(deviceId);
        if (lambdaQuery != null) {
            return lambdaQuery.queryLambdas(portNumber);
        }
        return Collections.emptySet();
    }

    @Override
    public ModulationScheme getModulation(DeviceId deviceId, PortNumber portNumber) {
        checkNotNull(deviceId);
        checkNotNull(portNumber);
        Device device = deviceService.getDevice(deviceId);
        Direction component = Direction.ALL;
        if (device.is(ModulationConfig.class)) {
            ModulationConfig<Object> modulationConfig = device.as(ModulationConfig.class);
            Optional<ModulationScheme> scheme = modulationConfig.getModulationScheme(portNumber, component);
            if (scheme.isPresent()) {
                return scheme.get();
            }
        }
        return null;
    }

    @Override
    public void setModulation(DeviceId deviceId, PortNumber portNumber, String modulation) {
        checkNotNull(deviceId);
        checkNotNull(portNumber);
        Device device = deviceService.getDevice(deviceId);
        Direction component = Direction.ALL;
        if (device.is(ModulationConfig.class)) {
            ModulationConfig<Object> modulationConfig = device.as(ModulationConfig.class);
            long bitRate = 0;
            if (modulation.equalsIgnoreCase(ModulationScheme.DP_QPSK.name())) {
                bitRate = 100;
            } else {
                bitRate = 200;
            }
            modulationConfig.setModulationScheme(portNumber, component, bitRate);
        }

    }

    @Override
    public FlowId createConnection(DeviceId deviceId, int priority, boolean isPermanent,
                                   int timeout, PortNumber inPort, PortNumber outPort, OchSignal ochSignal) {
        checkNotNull(deviceId);
        checkNotNull(inPort);
        checkNotNull(outPort);

        //Creation of selector.
        TrafficSelector selector = DefaultTrafficSelector.builder()
                .add(Criteria.matchInPort(inPort))
                .add(Criteria.matchOchSignalType(OchSignalType.FIXED_GRID))
                .add(Criteria.matchLambda(ochSignal))
                .build();

        //Creation of treatment
        TrafficTreatment treatment = DefaultTrafficTreatment.builder()
                .add(Instructions.modL0Lambda(ochSignal))
                .add(Instructions.createOutput(outPort))
                .build();

        FlowRule.Builder flowBuilder = DefaultFlowRule.builder()
                .forDevice(deviceId)
                .fromApp(appId)
                .withPriority(priority)
                .withSelector(selector)
                .withTreatment(treatment);
        if (isPermanent) {
            flowBuilder.makePermanent();
        } else {
            flowBuilder.makeTemporary(timeout);
        }

        FlowRule flowRule = flowBuilder.build();
        flowRuleService.applyFlowRules(flowRule);

        log.info("Created connection from input port {} to output port {}",
                inPort.toLong(), outPort.toLong());

        return flowRule.id();
    }

    @Override
    public FlowId createConnection(DeviceId deviceId, int priority, boolean isPermanent,
                                   int timeout, PortNumber inPort, PortNumber outPort,
                                   OchSignal ochSignal, Double attenuation) {
        checkNotNull(deviceId);
        checkNotNull(inPort);
        checkNotNull(outPort);
        FlowId flowId = createConnection(deviceId, priority, isPermanent, timeout, inPort, outPort, ochSignal);
        delayedSetAttenuation(deviceId, outPort, ochSignal, attenuation);
        return flowId;
    }

    @Override
    public Frequency getWavelength(DeviceId deviceId, PortNumber portNumber) {
        checkNotNull(deviceId);
        checkNotNull(portNumber);
        Optional<FlowEntry> optFlow = StreamSupport
                .stream(flowRuleService.getFlowEntries(deviceId).spliterator(), false)
                .filter(flow -> {
                    return flow.treatment().allInstructions().stream().filter(instr -> {
                        if (instr.type().equals(Instruction.Type.OUTPUT)) {
                            return ((Instructions.OutputInstruction) instr).port().equals(portNumber);
                        } else if (instr.type().equals(Instruction.Type.L0MODIFICATION)) {
                            return ((L0ModificationInstruction) instr).subtype()
                                    .equals(L0ModificationInstruction.L0SubType.OCH);
                        }
                        return false;
                    }).count() == 2;
                }).findFirst();
        if (optFlow.isPresent()) {
            Optional<Instruction> instruction = optFlow.get().treatment().allInstructions().stream().filter(instr -> {
                if (instr.type().equals(Instruction.Type.L0MODIFICATION)) {
                    return ((L0ModificationInstruction) instr).subtype()
                            .equals(L0ModificationInstruction.L0SubType.OCH);
                }
                return false;
            }).findAny();
            if (instruction.isPresent()) {
                return ((L0ModificationInstruction.ModOchSignalInstruction) instruction.get()).lambda()
                        .centralFrequency();
            }
        }
        return null;
    }

    @Override
    public void removeConnection(DeviceId deviceId, FlowId flowId) {
        checkNotNull(deviceId);
        checkNotNull(flowId);
        for (FlowEntry entry : flowRuleService.getFlowEntries(deviceId)) {
            if (entry.id().equals(flowId)) {
                flowRuleService.removeFlowRules(entry);
                log.info("Deleted connection {}", entry.id());
                break;
            }
        }
    }

    @Override
    public boolean hasPortTargetPower(DeviceId deviceId, PortNumber portNumber) {
        checkNotNull(deviceId);
        checkNotNull(portNumber);
        PowerConfig<Object> powerConfig = getPowerConfig(deviceId);
        if (powerConfig != null) {
            Optional<Range<Double>> range = powerConfig.getTargetPowerRange(portNumber, Direction.ALL);
            return range.isPresent();
        }
        return false;
    }

    @Override
    public boolean portTargetPowerInRange(DeviceId deviceId, PortNumber portNumber, double power) {
        checkNotNull(deviceId);
        checkNotNull(portNumber);
        PowerConfig<Object> powerConfig = getPowerConfig(deviceId);
        if (powerConfig != null) {
            Optional<Range<Double>> range = powerConfig.getTargetPowerRange(portNumber, Direction.ALL);
            return range.isPresent() && range.get().contains(power);
        }
        return false;
    }

    @Override
    public boolean attenuationInRange(DeviceId deviceId, PortNumber outPort, double att) {
        checkNotNull(deviceId);
        checkNotNull(outPort);
        PowerConfig<Object> powerConfig = getPowerConfig(deviceId);
        if (powerConfig != null) {
            OchSignal stubOch = OchSignal.newDwdmSlot(ChannelSpacing.CHL_50GHZ, 0);
            Optional<Range<Double>> range = powerConfig.getTargetPowerRange(outPort, stubOch);
            return range.isPresent() && range.get().contains(att);
        }
        return false;
    }

    @Override
    public boolean validInputPort(DeviceId deviceId, PortNumber portNumber) {
        checkNotNull(deviceId);
        checkNotNull(portNumber);
        PowerConfig<Object> powerConfig = getPowerConfig(deviceId);
        if (powerConfig != null) {
            Optional<Range<Double>> range = powerConfig.getInputPowerRange(portNumber, Direction.ALL);
            return range.isPresent();
        }
        return false;
    }

    @Override
    public boolean validOutputPort(DeviceId deviceId, PortNumber portNumber) {
        return hasPortTargetPower(deviceId, portNumber);
    }

    @Override
    public boolean validChannel(DeviceId deviceId, PortNumber portNumber, OchSignal ochSignal) {
        checkNotNull(deviceId);
        checkNotNull(portNumber);
        checkNotNull(ochSignal);
        LambdaQuery lambdaQuery = getLambdaQuery(deviceId);
        if (lambdaQuery != null) {
            Set<OchSignal> channels = lambdaQuery.queryLambdas(portNumber);
            return channels.contains(ochSignal);
        }
        return false;
    }

    @Override
    public boolean channelAvailable(DeviceId deviceId, OchSignal ochSignal) {
        checkNotNull(deviceId);
        checkNotNull(ochSignal);
        for (FlowEntry entry : flowRuleService.getFlowEntries(deviceId)) {
            if (ChannelData.fromFlow(entry).ochSignal().equals(ochSignal)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean validConnection(DeviceId deviceId, PortNumber inPort, PortNumber outPort) {
        checkNotNull(deviceId);
        checkNotNull(inPort);
        checkNotNull(outPort);
        return validInputPort(deviceId, inPort) && validOutputPort(deviceId, outPort);
    }

    @Override
    public Range<Double> targetPortPowerRange(DeviceId deviceId, PortNumber portNumber) {
        checkNotNull(deviceId);
        checkNotNull(portNumber);
        PowerConfig<Object> powerConfig = getPowerConfig(deviceId);
        if (powerConfig != null) {
            Optional<Range<Double>> range = powerConfig.getTargetPowerRange(portNumber, Direction.ALL);
            if (range.isPresent()) {
                return range.get();
            }
        }
        return null;
    }

    @Override
    public Range<Double> attenuationRange(DeviceId deviceId, PortNumber portNumber, OchSignal ochSignal) {
        checkNotNull(deviceId);
        checkNotNull(portNumber);
        checkNotNull(ochSignal);
        PowerConfig<Object> powerConfig = getPowerConfig(deviceId);
        if (powerConfig != null) {
            Optional<Range<Double>> range = powerConfig.getTargetPowerRange(portNumber, ochSignal);
            if (range.isPresent()) {
                return range.get();
            }
        }
        return null;
    }

    @Override
    public Range<Double> inputPortPowerRange(DeviceId deviceId, PortNumber portNumber) {
        checkNotNull(deviceId);
        checkNotNull(portNumber);
        PowerConfig<Object> powerConfig = getPowerConfig(deviceId);
        if (powerConfig != null) {
            Optional<Range<Double>> range = powerConfig.getInputPowerRange(portNumber, Direction.ALL);
            if (range.isPresent()) {
                return range.get();
            }
        }
        return null;
    }

    private PowerConfig<Object> getPowerConfig(DeviceId deviceId) {
        Device device = deviceService.getDevice(deviceId);
        if (device != null && device.is(PowerConfig.class)) {
            return device.as(PowerConfig.class);
        }
        // Do not need warning here for port polling.
        log.debug("Unable to load PowerConfig for {}", deviceId);
        return null;
    }

    private LambdaQuery getLambdaQuery(DeviceId deviceId) {
        Device device = deviceService.getDevice(deviceId);
        if (device != null && device.is(LambdaQuery.class)) {
            return device.as(LambdaQuery.class);
        }
        // Do not need warning here for port polling.
        log.debug("Unable to load LambdaQuery for {}", deviceId);
        return null;
    }

    private ProtectionConfigBehaviour getProtectionConfig(DeviceId deviceId) {
        Device device = deviceService.getDevice(deviceId);
        if (device != null && device.is(ProtectionConfigBehaviour.class)) {
            return device.as(ProtectionConfigBehaviour.class);
        }
        // Do not need warning here for port polling.
        log.debug("Unable to load ProtectionConfigBehaviour for {}", deviceId);
        return null;
    }

    // Initialize all devices
    private void initDevices() {
        for (Device device : deviceService.getDevices(Device.Type.ROADM)) {
            initDevice(device.id());
            //FIXME
            // As roadm application is a optional tool for now.
            // The target power initialization will be enhanced later,
            // hopefully using an formal optical subsystem.
            // setAllInitialTargetPortPowers(device.id());
        }
    }

    // Initialize RoadmStore for a device to support target power
    private void initDevice(DeviceId deviceId) {
        if (!roadmStore.deviceAvailable(deviceId)) {
            roadmStore.addDevice(deviceId);
        }
        log.info("Initialized device {}", deviceId);
    }

    // Sets the target port powers for a port on a device
    // Attempts to read target powers from store. If no value is found then
    // default value is used instead.
    private void setInitialTargetPortPower(DeviceId deviceId, PortNumber portNumber) {
        PowerConfig<Object> powerConfig = getPowerConfig(deviceId);
        if (powerConfig == null) {
            log.warn("Unable to set default initial powers for port {} on device {}", portNumber, deviceId);
            return;
        }

        Optional<Range<Double>> range = powerConfig.getTargetPowerRange(portNumber, Direction.ALL);
        if (!range.isPresent()) {
            log.warn("No target power range found for port {} on device {}", portNumber, deviceId);
            return;
        }

        Double power = roadmStore.getTargetPower(deviceId, portNumber);
        if (power == null) {
            // Set default to middle of the range
            power = (range.get().lowerEndpoint() + range.get().upperEndpoint()) / 2;
            roadmStore.setTargetPower(deviceId, portNumber, power);
        }
        powerConfig.setTargetPower(portNumber, Direction.ALL, power);
    }

    // Sets the target port powers for each each port on a device
    // Attempts to read target powers from store. If no value is found then
    // default value is used instead
    private void setAllInitialTargetPortPowers(DeviceId deviceId) {
        PowerConfig<Object> powerConfig = getPowerConfig(deviceId);
        if (powerConfig == null) {
            log.warn("Unable to set default initial powers for device {}", deviceId);
            return;
        }

        List<Port> ports = deviceService.getPorts(deviceId);
        for (Port port : ports) {
            Optional<Range<Double>> range = powerConfig.getTargetPowerRange(port.number(), Direction.ALL);
            if (range.isPresent()) {
                Double power = roadmStore.getTargetPower(deviceId, port.number());
                if (power == null) {
                    // Set default to middle of the range
                    power = (range.get().lowerEndpoint() + range.get().upperEndpoint()) / 2;
                    roadmStore.setTargetPower(deviceId, port.number(), power);
                }
                powerConfig.setTargetPower(port.number(), Direction.ALL, power);
            } else {
                log.warn("No target power range found for port {} on device {}", port.number(), deviceId);
            }
        }
    }

    // Delay the call to setTargetPower because the flow may not be in the store yet
    // Tested with Lumentum ROADM-20 1 seconds was not enough, increased to 5 seconds
    private void delayedSetAttenuation(DeviceId deviceId, PortNumber outPort,
                                       OchSignal ochSignal, Double attenuation) {
        Runnable setAtt = () -> {
            try {
                TimeUnit.SECONDS.sleep(5);
            } catch (InterruptedException e) {
                log.warn("Thread interrupted. Setting attenuation early.");
                Thread.currentThread().interrupt();
            }
            setAttenuation(deviceId, outPort, ochSignal, attenuation);
        };
        new Thread(setAtt).start();
    }

    // get protection endpoint states
    private Map<ConnectPoint, ProtectedTransportEndpointState> getProtectionSwitchStates(
            ProtectionConfigBehaviour behaviour) {
        Map<ConnectPoint, ProtectedTransportEndpointState> map;
        try {
            map = behaviour.getProtectionEndpointStates().get();
        } catch (InterruptedException e1) {
            log.error("Interrupted.", e1);
            Thread.currentThread().interrupt();
            return ImmutableMap.of();
        } catch (ExecutionException e1) {
            log.error("Exception caught.", e1);
            return ImmutableMap.of();
        }
        return map;
    }

    // Listens to device events.
    private class InternalDeviceListener implements DeviceListener {
        @Override
        public void event(DeviceEvent deviceEvent) {
            Device device = deviceEvent.subject();

            switch (deviceEvent.type()) {
                case DEVICE_ADDED:
                case DEVICE_UPDATED:
                    initDevice(device.id());
                    break;
                case PORT_ADDED:
                case PORT_UPDATED:
                    //FIXME
                    // As roadm application is a optional tool for now.
                    // The target power initialization will be enhanced later,
                    // hopefully using an formal optical subsystem.
                    // setInitialTargetPortPower(device.id(), deviceEvent.port().number());
                    break;
                default:
                    break;

            }
        }
    }
}

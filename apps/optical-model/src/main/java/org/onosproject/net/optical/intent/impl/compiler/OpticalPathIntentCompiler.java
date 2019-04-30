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
package org.onosproject.net.optical.intent.impl.compiler;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Link;
import org.onosproject.net.PortNumber;
import org.onosproject.net.Port;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.net.Device.Type;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.device.DeviceServiceAdapter;
import org.onosproject.net.flow.DefaultFlowRule;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.FlowRule;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.flow.criteria.Criteria;
import org.onosproject.net.flow.instructions.Instructions;
import org.onosproject.net.intent.FlowRuleIntent;
import org.onosproject.net.intent.Intent;
import org.onosproject.net.intent.IntentCompiler;
import org.onosproject.net.intent.IntentExtensionService;
import org.onosproject.net.intent.OpticalPathIntent;
import org.onosproject.net.intent.PathIntent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Component(immediate = true)
public class OpticalPathIntentCompiler implements IntentCompiler<OpticalPathIntent> {

    private static final Logger log = LoggerFactory.getLogger(OpticalPathIntentCompiler.class);

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected IntentExtensionService intentManager;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected DeviceService deviceService = new DeviceServiceAdapter();

    private ApplicationId appId;
    // Devices which are wavelength transparent and thus do not require wavelength-based match/actions
    private static final Set<Type> TRANSPARENT_DEVICES =
            ImmutableSet.of(Type.OPTICAL_AMPLIFIER, Type.FIBER_SWITCH);
    // Devices which don't accept flow rules
    private static final Set<Type> NO_FLOWRULE_DEVICES =
            ImmutableSet.of(Type.OPTICAL_AMPLIFIER);

    @Activate
    public void activate() {
        appId = coreService.registerApplication("org.onosproject.net.intent");
        intentManager.registerCompiler(OpticalPathIntent.class, this);
    }

    @Deactivate
    public void deactivate() {
        intentManager.unregisterCompiler(OpticalPathIntent.class);
    }

    @Override
    public List<Intent> compile(OpticalPathIntent intent, List<Intent> installable) {
        log.debug("Compiling optical path intent between {} and {}", intent.src(), intent.dst());

        // Create rules for forward and reverse path
        List<FlowRule> rules = createRules(intent);
        if (intent.isBidirectional()) {
            rules.addAll(createReverseRules(intent));
        }

        return Collections.singletonList(
                new FlowRuleIntent(appId,
                        intent.key(),
                        rules,
                        intent.resources(),
                        PathIntent.ProtectionType.PRIMARY,
                        intent.resourceGroup()
                )
        );
    }

    /**
     * Create rules for the forward path of the intent.
     *
     * @param intent the intent
     * @return list of flow rules
     */
    private List<FlowRule> createRules(OpticalPathIntent intent) {
        TrafficSelector.Builder selectorBuilder = DefaultTrafficSelector.builder();
        selectorBuilder.matchInPort(intent.src().port());

        List<FlowRule> rules = new LinkedList<>();

        /*
         * especial case for 0 hop when srcDeviceId = dstDeviceId
         * and path contain only one fake default path.
         */
        if (intent.src().deviceId().equals(intent.dst().deviceId()) &&
                intent.path().links().size() == 1) {
            log.debug("handling 0 hop case for intent {}", intent);
            TrafficTreatment.Builder treatmentBuilder = DefaultTrafficTreatment.builder();
            if (!isTransparent(intent.src().deviceId())) {
                treatmentBuilder.add(Instructions.modL0Lambda(intent.lambda()));
            }
            treatmentBuilder.setOutput(intent.dst().port());

            FlowRule rule = DefaultFlowRule.builder()
                    .forDevice(intent.src().deviceId())
                    .withSelector(selectorBuilder.build())
                    .withTreatment(treatmentBuilder.build())
                    .withPriority(intent.priority())
                    .fromApp(appId)
                    .makePermanent()
                    .build();
            rules.add(rule);
            return rules;
        }

        ConnectPoint current = intent.src();

        for (Link link : intent.path().links()) {
            TrafficTreatment.Builder treatmentBuilder = DefaultTrafficTreatment.builder();
            if (!isTransparent(current.deviceId())) {
                treatmentBuilder.add(Instructions.modL0Lambda(intent.lambda()));
            }
            treatmentBuilder.setOutput(link.src().port());

            FlowRule rule = DefaultFlowRule.builder()
                    .forDevice(current.deviceId())
                    .withSelector(selectorBuilder.build())
                    .withTreatment(treatmentBuilder.build())
                    .withPriority(intent.priority())
                    .fromApp(appId)
                    .makePermanent()
                    .build();
            selectorBuilder = DefaultTrafficSelector.builder();

            if (!isNoFlowRule(current.deviceId())) {
                rules.add(rule);
            }

            current = link.dst();
            selectorBuilder.matchInPort(link.dst().port());
            if (!isTransparent(current.deviceId())) {
                selectorBuilder.add(Criteria.matchLambda(intent.lambda()));
                selectorBuilder.add(Criteria.matchOchSignalType(intent.signalType()));
            }
        }

        // Build the egress ROADM rule
        TrafficTreatment.Builder treatmentLast = DefaultTrafficTreatment.builder();
        treatmentLast.setOutput(intent.dst().port());

        FlowRule rule = new DefaultFlowRule.Builder()
                .forDevice(intent.dst().deviceId())
                .withSelector(selectorBuilder.build())
                .withTreatment(treatmentLast.build())
                .withPriority(intent.priority())
                .fromApp(appId)
                .makePermanent()
                .build();

        if (!isNoFlowRule(intent.dst().deviceId())) {
            rules.add(rule);
        }

        return rules;
    }

    /**
     * Create rules for the reverse path of the intent.
     *
     * @param intent the intent
     * @return list of flow rules
     */
    private List<FlowRule> createReverseRules(OpticalPathIntent intent) {
        TrafficSelector.Builder selectorBuilder = DefaultTrafficSelector.builder();
        selectorBuilder.matchInPort(reversePort(intent.dst().deviceId(), intent.dst().port()));

        List<FlowRule> rules = new LinkedList<>();

        /*
         * especial case for 0 hop when srcDeviceId = dstDeviceId
         * and path contain only one fake default path.
         */
        if (intent.src().deviceId().equals(intent.dst().deviceId()) &&
                intent.path().links().size() == 1) {
            log.debug("handling 0 hop reverse path case for intent {}", intent);
            TrafficTreatment.Builder treatmentBuilder = DefaultTrafficTreatment.builder();
            if (!isTransparent(intent.src().deviceId())) {
                treatmentBuilder.add(Instructions.modL0Lambda(intent.lambda()));
            }
            treatmentBuilder.setOutput(reversePort(intent.src().deviceId(), intent.src().port()));

            FlowRule rule = DefaultFlowRule.builder()
                    .forDevice(intent.src().deviceId())
                    .withSelector(selectorBuilder.build())
                    .withTreatment(treatmentBuilder.build())
                    .withPriority(intent.priority())
                    .fromApp(appId)
                    .makePermanent()
                    .build();
            rules.add(rule);
            return rules;
        }

        ConnectPoint current = intent.dst();

        for (Link link : Lists.reverse(intent.path().links())) {
            TrafficTreatment.Builder treatmentBuilder = DefaultTrafficTreatment.builder();
            if (!isTransparent(current.deviceId())) {
                treatmentBuilder.add(Instructions.modL0Lambda(intent.lambda()));
            }
            treatmentBuilder.setOutput(reversePort(link.dst().deviceId(), link.dst().port()));

            FlowRule rule = DefaultFlowRule.builder()
                    .forDevice(current.deviceId())
                    .withSelector(selectorBuilder.build())
                    .withTreatment(treatmentBuilder.build())
                    .withPriority(intent.priority())
                    .fromApp(appId)
                    .makePermanent()
                    .build();
            selectorBuilder = DefaultTrafficSelector.builder();

            if (!isNoFlowRule(current.deviceId())) {
                rules.add(rule);
            }

            current = link.src();
            selectorBuilder.matchInPort(reversePort(link.src().deviceId(), link.src().port()));
            if (!isTransparent(current.deviceId())) {
                selectorBuilder.add(Criteria.matchLambda(intent.lambda()));
                selectorBuilder.add(Criteria.matchOchSignalType(intent.signalType()));
            }
        }

        // Build the egress ROADM rule
        TrafficTreatment.Builder treatmentLast = DefaultTrafficTreatment.builder();
        treatmentLast.setOutput(reversePort(intent.src().deviceId(), intent.src().port()));

        FlowRule rule = new DefaultFlowRule.Builder()
                .forDevice(intent.src().deviceId())
                .withSelector(selectorBuilder.build())
                .withTreatment(treatmentLast.build())
                .withPriority(intent.priority())
                .fromApp(appId)
                .makePermanent()
                .build();

        if (!isNoFlowRule(intent.src().deviceId())) {
            rules.add(rule);
        }

        return rules;
    }

    /**
     * Returns the PortNum of reverse port if annotation is present, otherwise return PortNum of the port itself.
     * In the OpenROADM YANG models it is used the term "partner-port.
     *
     * @param portNumber the port
     * @return the PortNum of reverse port if annotation is present, otherwise PortNum of the port itself.
     */
    private PortNumber reversePort(DeviceId deviceId, PortNumber portNumber) {
        Port port = deviceService.getPort(deviceId, portNumber);

        String reversePort = port.annotations().value(OpticalPathIntent.REVERSE_PORT_ANNOTATION_KEY);
        if (reversePort != null) {
            PortNumber reversePortNumber = PortNumber.portNumber(reversePort);
            return reversePortNumber;
        } else {
            return portNumber;
        }
    }

    /**
     * Returns true if device does not accept flow rules, false otherwise.
     *
     * @param deviceId the device
     * @return true if device does not accept flow rule, false otherwise
     */
    private boolean isNoFlowRule(DeviceId deviceId) {
        return NO_FLOWRULE_DEVICES.contains(
                Optional.ofNullable(deviceService.getDevice(deviceId))
                        .map(Device::type)
                        .orElse(Type.OTHER));
    }

    /**
     * Returns true if device is wavelength transparent, false otherwise.
     *
     * @param deviceId the device
     * @return true if wavelength transparent, false otherwise
     */
    private boolean isTransparent(DeviceId deviceId) {
        return TRANSPARENT_DEVICES.contains(
                Optional.ofNullable(deviceService.getDevice(deviceId))
                        .map(Device::type)
                        .orElse(Type.OTHER));
    }
}

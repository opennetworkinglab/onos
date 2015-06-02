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
package org.onosproject.net.intent.impl.compiler;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DeviceId;
import org.onosproject.net.OchPort;
import org.onosproject.net.OduCltPort;
import org.onosproject.net.OduSignalType;
import org.onosproject.net.Port;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.flow.DefaultFlowRule;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.FlowRule;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.intent.FlowRuleIntent;
import org.onosproject.net.intent.Intent;
import org.onosproject.net.intent.IntentCompiler;
import org.onosproject.net.intent.IntentExtensionService;
import org.onosproject.net.intent.IntentId;
import org.onosproject.net.intent.IntentService;
import org.onosproject.net.intent.OpticalCircuitIntent;
import org.onosproject.net.intent.OpticalConnectivityIntent;
import org.onosproject.net.intent.impl.IntentCompilationException;
import org.onosproject.net.resource.device.DeviceResourceService;
import org.onosproject.net.resource.link.LinkResourceAllocations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * An intent compiler for {@link org.onosproject.net.intent.OpticalCircuitIntent}.
 */
@Component(immediate = true)
public class OpticalCircuitIntentCompiler implements IntentCompiler<OpticalCircuitIntent> {

    private static final Logger log = LoggerFactory.getLogger(OpticalCircuitIntentCompiler.class);

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected IntentExtensionService intentManager;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DeviceService deviceService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DeviceResourceService deviceResourceService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected IntentService intentService;

    private ApplicationId appId;

    @Activate
    public void activate() {
        appId = coreService.registerApplication("org.onosproject.net.intent");
        intentManager.registerCompiler(OpticalCircuitIntent.class, this);
    }

    @Deactivate
    public void deactivate() {
        intentManager.unregisterCompiler(OpticalCircuitIntent.class);
    }

    @Override
    public List<Intent> compile(OpticalCircuitIntent intent, List<Intent> installable,
                                Set<LinkResourceAllocations> resources) {
        // Check if ports are OduClt ports
        ConnectPoint src = intent.getSrc();
        ConnectPoint dst = intent.getDst();
        Port srcPort = deviceService.getPort(src.deviceId(), src.port());
        Port dstPort = deviceService.getPort(dst.deviceId(), dst.port());
        checkArgument(srcPort instanceof OduCltPort);
        checkArgument(dstPort instanceof OduCltPort);

        log.debug("Compiling optical circuit intent between {} and {}", src, dst);

        // Reserve OduClt ports
        if (!deviceResourceService.requestPorts(new HashSet(Arrays.asList(srcPort, dstPort)), intent)) {
            throw new IntentCompilationException("Unable to reserve ports for intent " + intent);
        }

        LinkedList<Intent> intents = new LinkedList<>();

        FlowRuleIntent circuitIntent;
        OpticalConnectivityIntent connIntent = findOpticalConnectivityIntent(intent);

        // Create optical connectivity intent if needed
        if (connIntent == null) {
            // Find OCh ports with available resources
            Pair<OchPort, OchPort> ochPorts = findPorts(intent);

            if (ochPorts == null) {
                return Collections.emptyList();
            }

            // Create optical connectivity intent
            ConnectPoint srcCP = new ConnectPoint(src.elementId(), ochPorts.getLeft().number());
            ConnectPoint dstCP = new ConnectPoint(dst.elementId(), ochPorts.getRight().number());
            // FIXME: hardcoded ODU signal type
            connIntent = OpticalConnectivityIntent.builder()
                    .appId(appId)
                    .src(srcCP)
                    .dst(dstCP)
                    .signalType(OduSignalType.ODU4)
                    .bidirectional(intent.isBidirectional())
                    .build();
            intents.add(connIntent);
        }

        // Create optical circuit intent
        List<FlowRule> rules = new LinkedList<>();
        rules.add(connectPorts(src, connIntent.getSrc()));
        rules.add(connectPorts(connIntent.getDst(), dst));

        // Create flow rules for reverse path
        if (intent.isBidirectional()) {
            rules.add(connectPorts(connIntent.getSrc(), src));
            rules.add(connectPorts(dst, connIntent.getDst()));
        }

        circuitIntent = new FlowRuleIntent(appId, rules, intent.resources());

        // Save circuit to connectivity intent mapping
        deviceResourceService.requestMapping(connIntent.id(), circuitIntent.id());
        intents.add(circuitIntent);

        return intents;
    }

    /**
     * Checks if current allocations on given resource can satisfy request.
     *
     * @param request
     * @param resource
     * @return
     */
    private boolean isAvailable(Intent request, IntentId resource) {
        Set<IntentId> mapping = deviceResourceService.getMapping(resource);

        // TODO: hardcoded 10 x 10G
        return mapping.size() < 10;
    }

    /**
     * Returns existing and available optical connectivity intent that matches the given circuit intent.
     *
     * @param circuitIntent optical circuit intent
     * @return existing optical connectivity intent, null otherwise.
     */
    private OpticalConnectivityIntent findOpticalConnectivityIntent(OpticalCircuitIntent circuitIntent) {
        for (Intent intent : intentService.getIntents()) {
            if (!(intent instanceof OpticalConnectivityIntent)) {
                continue;
            }

            OpticalConnectivityIntent connIntent = (OpticalConnectivityIntent) intent;

            ConnectPoint src = circuitIntent.getSrc();
            ConnectPoint dst = circuitIntent.getDst();
            if (!src.equals(connIntent.getSrc()) && !dst.equals(connIntent.getDst())) {
                continue;
            }

            if (isAvailable(circuitIntent, connIntent.id())) {
                return connIntent;
            }
        }

        return null;
    }

    private OchPort findAvailableOchPort(DeviceId deviceId, OpticalCircuitIntent circuitIntent) {
        List<Port> ports = deviceService.getPorts(deviceId);

        for (Port port : ports) {
            if (!(port instanceof OchPort)) {
                continue;
            }

            // Port is not used
            IntentId intentId = deviceResourceService.getAllocations(port);
            if (intentId == null) {
                return (OchPort) port;
            }

            // Port is used but has free resources
            if (isAvailable(circuitIntent, intentId)) {
                return (OchPort) port;
            }
        }

        return null;
    }

    // TODO: Add constraints for OduClt to OCh port mappings
    // E.g., ports need to belong to same line card.
    private Pair<OchPort, OchPort> findPorts(OpticalCircuitIntent intent) {

        OchPort srcPort = findAvailableOchPort(intent.getSrc().deviceId(), intent);
        if (srcPort == null) {
            return null;
        }

        OchPort dstPort = findAvailableOchPort(intent.getDst().deviceId(), intent);
        if (dstPort == null) {
            return null;
        }

        return Pair.of(srcPort, dstPort);
    }

    /**
     * Builds flow rule for mapping between two ports.
     *
     * @param src source port
     * @param dst destination port
     * @return flow rules
     */
    private FlowRule connectPorts(ConnectPoint src, ConnectPoint dst) {
        checkArgument(src.deviceId().equals(dst.deviceId()));

        TrafficSelector.Builder selectorBuilder = DefaultTrafficSelector.builder();
        TrafficTreatment.Builder treatmentBuilder = DefaultTrafficTreatment.builder();

        selectorBuilder.matchInPort(src.port());
        //selectorBuilder.add(Criteria.matchCltSignalType)
        treatmentBuilder.setOutput(dst.port());
        //treatmentBuilder.add(Instructions.modL1OduSignalType)

        FlowRule flowRule = DefaultFlowRule.builder()
                .forDevice(src.deviceId())
                .withSelector(selectorBuilder.build())
                .withTreatment(treatmentBuilder.build())
                .withPriority(100)
                .fromApp(appId)
                .makePermanent()
                .build();

        return flowRule;
    }
}

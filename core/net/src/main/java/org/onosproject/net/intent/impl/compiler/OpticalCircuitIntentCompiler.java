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
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Modified;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.onlab.util.Tools;
import org.onosproject.cfg.ComponentConfigService;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.net.AnnotationKeys;
import org.onosproject.net.ConnectPoint;
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
import org.onosproject.net.newresource.ResourceAllocation;
import org.onosproject.net.newresource.ResourcePath;
import org.onosproject.net.newresource.ResourceService;
import org.onosproject.net.resource.device.IntentSetMultimap;
import org.onosproject.net.resource.link.LinkResourceAllocations;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Dictionary;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * An intent compiler for {@link org.onosproject.net.intent.OpticalCircuitIntent}.
 */
// For now, remove component designation until dependency on the new resource manager is available.
// @Component(immediate = true)
public class OpticalCircuitIntentCompiler implements IntentCompiler<OpticalCircuitIntent> {

    private static final Logger log = LoggerFactory.getLogger(OpticalCircuitIntentCompiler.class);

    private static final int DEFAULT_MAX_CAPACITY = 10;

    @Property(name = "maxCapacity", intValue = DEFAULT_MAX_CAPACITY,
            label = "Maximum utilization of an optical connection.")

    private int maxCapacity = DEFAULT_MAX_CAPACITY;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ComponentConfigService cfgService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected IntentExtensionService intentManager;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DeviceService deviceService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ResourceService resourceService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected IntentSetMultimap intentSetMultimap;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected IntentService intentService;

    private ApplicationId appId;

    @Modified
    public void modified(ComponentContext context) {
        Dictionary properties = context.getProperties();

        //TODO for reduction check if the new capacity is smaller than the size of the current mapping
        String propertyString = Tools.get(properties, "maxCapacity");

        //Ignore if propertyString is empty
        if (!propertyString.isEmpty()) {
            try {
                int temp = Integer.parseInt(propertyString);
                //Ensure value is non-negative but allow zero as a way to shutdown the link
                if (temp >= 0) {
                    maxCapacity = temp;
                }
            } catch (NumberFormatException e) {
                //Malformed arguments lead to no change of value (user should be notified of error)
              log.error("The value '{}' for maxCapacity was not parsable as an integer.", propertyString, e);
            }
        } else {
            //Notify of empty value but do not return (other properties will also go in this function)
            log.error("The value for maxCapacity was set to an empty value.");
        }

    }

    @Activate
    public void activate(ComponentContext context) {
        appId = coreService.registerApplication("org.onosproject.net.intent");
        intentManager.registerCompiler(OpticalCircuitIntent.class, this);
        cfgService.registerProperties(getClass());
        modified(context);
    }

    @Deactivate
    public void deactivate() {
        intentManager.unregisterCompiler(OpticalCircuitIntent.class);
        cfgService.unregisterProperties(getClass(), false);
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
        ResourcePath srcPortPath = new ResourcePath(src.deviceId(), src.port());
        ResourcePath dstPortPath = new ResourcePath(dst.deviceId(), dst.port());
        List<ResourceAllocation> allocation = resourceService.allocate(intent.id(), srcPortPath, dstPortPath);
        if (allocation.isEmpty()) {
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
            intentService.submit(connIntent);
        }

        // Create optical circuit intent
        List<FlowRule> rules = new LinkedList<>();
        rules.add(connectPorts(src, connIntent.getSrc(), intent.priority()));
        rules.add(connectPorts(connIntent.getDst(), dst, intent.priority()));

        // Create flow rules for reverse path
        if (intent.isBidirectional()) {
            rules.add(connectPorts(connIntent.getSrc(), src, intent.priority()));
            rules.add(connectPorts(dst, connIntent.getDst(), intent.priority()));
        }

        circuitIntent = new FlowRuleIntent(appId, rules, intent.resources());

        // Save circuit to connectivity intent mapping
        intentSetMultimap.allocateMapping(connIntent.id(), intent.id());
        intents.add(circuitIntent);

        return intents;
    }

    /**
     * Checks if current allocations on given resource can satisfy request.
     * If the resource is null, return true.
     *
     * @param resource the resource on which to map the intent
     * @return true if the resource can accept the request, false otherwise
     */
    private boolean isAvailable(IntentId resource) {
        if (resource == null) {
            return true;
        }

        Set<IntentId> mapping = intentSetMultimap.getMapping(resource);

        if (mapping == null) {
            return true;
        }

        return mapping.size() < maxCapacity;
    }

    private boolean isAllowed(OpticalCircuitIntent circuitIntent, OpticalConnectivityIntent connIntent) {
        ConnectPoint srcStaticPort = staticPort(circuitIntent.getSrc());
        if (srcStaticPort != null) {
            if (!srcStaticPort.equals(connIntent.getSrc())) {
                return false;
            }
        }

        ConnectPoint dstStaticPort = staticPort(circuitIntent.getDst());
        if (dstStaticPort != null) {
            if (!dstStaticPort.equals(connIntent.getDst())) {
                return false;
            }
        }

        return true;
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
            // Ignore if the intents don't have identical src and dst devices
            if (!src.deviceId().equals(connIntent.getSrc().deviceId()) &&
                    !dst.deviceId().equals(connIntent.getDst().deviceId())) {
                continue;
            }

            if (!isAllowed(circuitIntent, connIntent)) {
                continue;
            }

            if (isAvailable(connIntent.id())) {
                return connIntent;
            }
        }

        return null;
    }

    private ConnectPoint staticPort(ConnectPoint connectPoint) {
        Port port = deviceService.getPort(connectPoint.deviceId(), connectPoint.port());

        String staticPort = port.annotations().value(AnnotationKeys.STATIC_PORT);

        // FIXME: need a better way to match the port
        if (staticPort != null) {
            for (Port p : deviceService.getPorts(connectPoint.deviceId())) {
                if (staticPort.equals(p.number().name())) {
                    return new ConnectPoint(p.element().id(), p.number());
                }
            }
        }

        return null;
    }

    private OchPort findAvailableOchPort(ConnectPoint oduPort) {
        // First see if the port mappings are constrained
        ConnectPoint ochCP = staticPort(oduPort);

        if (ochCP != null) {
            OchPort ochPort = (OchPort) deviceService.getPort(ochCP.deviceId(), ochCP.port());
            Optional<IntentId> intentId =
                    resourceService.getResourceAllocation(new ResourcePath(ochCP.deviceId(), ochCP.port()))
                            .map(ResourceAllocation::consumer)
                            .filter(x -> x instanceof IntentId)
                            .map(x -> (IntentId) x);

            if (isAvailable(intentId.orElse(null))) {
                return ochPort;
            }
        }

        // No port constraints, so find any port that works
        List<Port> ports = deviceService.getPorts(oduPort.deviceId());

        for (Port port : ports) {
            if (!(port instanceof OchPort)) {
                continue;
            }

            Optional<IntentId> intentId =
                    resourceService.getResourceAllocation(new ResourcePath(oduPort.deviceId(), port.number()))
                            .map(ResourceAllocation::consumer)
                            .filter(x -> x instanceof IntentId)
                            .map(x -> (IntentId) x);
            if (isAvailable(intentId.orElse(null))) {
                return (OchPort) port;
            }
        }

        return null;
    }

    private Pair<OchPort, OchPort> findPorts(OpticalCircuitIntent intent) {

        OchPort srcPort = findAvailableOchPort(intent.getSrc());
        if (srcPort == null) {
            return null;
        }

        OchPort dstPort = findAvailableOchPort(intent.getDst());
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
    private FlowRule connectPorts(ConnectPoint src, ConnectPoint dst, int priority) {
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
                .withPriority(priority)
                .fromApp(appId)
                .makePermanent()
                .build();

        return flowRule;
    }
}

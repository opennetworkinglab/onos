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
package org.onosproject.net.optical.intent.impl.compiler;

import com.google.common.base.Strings;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
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
import org.onosproject.net.CltSignalType;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.OduSignalId;
import org.onosproject.net.OduSignalType;
import org.onosproject.net.OduSignalUtils;
import org.onosproject.net.Port;
import org.onosproject.net.TributarySlot;
import org.onosproject.net.behaviour.TributarySlotQuery;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.driver.Driver;
import org.onosproject.net.driver.DriverService;
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
import org.onosproject.net.intent.IntentId;
import org.onosproject.net.intent.IntentService;
import org.onosproject.net.intent.OpticalCircuitIntent;
import org.onosproject.net.intent.OpticalConnectivityIntent;
import org.onosproject.net.optical.OchPort;
import org.onosproject.net.optical.OduCltPort;
import org.onosproject.net.intent.IntentSetMultimap;
import org.onosproject.net.resource.ResourceAllocation;
import org.onosproject.net.resource.Resource;
import org.onosproject.net.resource.ResourceService;
import org.onosproject.net.resource.Resources;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;

import java.util.Collections;
import java.util.Dictionary;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.google.common.base.Preconditions.checkArgument;
import static org.onosproject.net.optical.device.OpticalDeviceServiceView.opticalView;

/**
 * An intent compiler for {@link org.onosproject.net.intent.OpticalCircuitIntent}.
 */
@Component(immediate = true)
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

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DriverService driverService;

    private ApplicationId appId;

    @Modified
    public void modified(ComponentContext context) {
        if (context == null) {
            return;
        }

        Dictionary properties = context.getProperties();

        //TODO for reduction check if the new capacity is smaller than the size of the current mapping
        String propertyString = Tools.get(properties, "maxCapacity");

        //Ignore if propertyString is empty or null
        if (!Strings.isNullOrEmpty(propertyString)) {
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
        deviceService = opticalView(deviceService);
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
    public List<Intent> compile(OpticalCircuitIntent intent, List<Intent> installable) {
        // Check if ports are OduClt ports
        ConnectPoint src = intent.getSrc();
        ConnectPoint dst = intent.getDst();
        Port srcPort = deviceService.getPort(src.deviceId(), src.port());
        Port dstPort = deviceService.getPort(dst.deviceId(), dst.port());
        checkArgument(srcPort instanceof OduCltPort);
        checkArgument(dstPort instanceof OduCltPort);

        log.debug("Compiling optical circuit intent between {} and {}", src, dst);

        // Release of intent resources here is only a temporary solution for handling the
        // case of recompiling due to intent restoration (when intent state is FAILED).
        // TODO: try to release intent resources in IntentManager.
        resourceService.release(intent.id());

        // Check OduClt ports availability
        Resource srcPortResource = Resources.discrete(src.deviceId(), src.port()).resource();
        Resource dstPortResource = Resources.discrete(dst.deviceId(), dst.port()).resource();
        // If ports are not available, compilation fails
        if (!Stream.of(srcPortResource, dstPortResource).allMatch(resourceService::isAvailable)) {
            throw new OpticalIntentCompilationException("Ports for the intent are not available. Intent: " + intent);
        }
        List<Resource> ports = ImmutableList.of(srcPortResource, dstPortResource);

        // Check if both devices support multiplexing (usage of TributarySlots)
        boolean multiplexingSupported = isMultiplexingSupported(intent.getSrc())
                && isMultiplexingSupported(intent.getDst());

        OpticalConnectivityIntent connIntent = findOpticalConnectivityIntent(intent.getSrc(), intent.getDst(),
                intent.getSignalType(), multiplexingSupported);

        if (connIntent != null && !multiplexingSupported) {
            return compile(intent, src, dst, Optional.of(connIntent), ports, false);
        }

        // Create optical connectivity intent if needed - no optical intent or not enough slots available
        if (connIntent == null) {
            return compile(intent, src, dst, Optional.empty(), ports, multiplexingSupported);
        }

        List<Resource> slots = availableSlotResources(connIntent.getSrc(), connIntent.getDst(),
                intent.getSignalType());
        if (slots.isEmpty()) {
            return compile(intent, src, dst, Optional.empty(), ports, true);
        }

        return compile(intent, src, dst, Optional.of(connIntent), ImmutableList.<Resource>builder()
                .addAll(ports).addAll(slots).build(), false);

    }

    private List<Intent> compile(OpticalCircuitIntent intent, ConnectPoint src, ConnectPoint dst,
                                 Optional<OpticalConnectivityIntent> existingConnectivity,
                                 List<Resource> resources, boolean supportsMultiplexing) {
        OpticalConnectivityIntent connectivityIntent;
        List<Resource> required;
        if (existingConnectivity.isPresent()) {
            connectivityIntent = existingConnectivity.get();
            required = resources;
        } else {
            // Find OCh ports with available resources
            Pair<OchPort, OchPort> ochPorts = findPorts(intent.getSrc(), intent.getDst(), intent.getSignalType());

            if (ochPorts == null) {
                throw new OpticalIntentCompilationException("Unable to find suitable OCH ports for intent " + intent);
            }

            ConnectPoint srcCP = new ConnectPoint(src.elementId(), ochPorts.getLeft().number());
            ConnectPoint dstCP = new ConnectPoint(dst.elementId(), ochPorts.getRight().number());

            // Create optical connectivity intent
            connectivityIntent = OpticalConnectivityIntent.builder()
                    .appId(appId)
                    .src(srcCP)
                    .dst(dstCP)
                    .signalType(ochPorts.getLeft().signalType())
                    .bidirectional(intent.isBidirectional())
                    .build();

            if (!supportsMultiplexing) {
                required = resources;
            } else {
                List<Resource> slots = availableSlotResources(srcCP, dstCP, intent.getSignalType());
                if (slots.isEmpty()) {
                    throw new OpticalIntentCompilationException("Unable to find Tributary Slots for intent " + intent);
                }
                required = ImmutableList.<Resource>builder().addAll(resources).addAll(slots).build();
            }
        }

        if (resourceService.allocate(intent.id(), required).isEmpty()) {
            throw new OpticalIntentCompilationException("Unable to allocate resources for intent " + intent
                    + ": resources=" + required);
        }

        intentService.submit(connectivityIntent);

        // Save circuit to connectivity intent mapping
        intentSetMultimap.allocateMapping(connectivityIntent.id(), intent.id());

        FlowRuleIntent circuitIntent = createFlowRule(intent, connectivityIntent, required.stream().
                flatMap(x -> Tools.stream(x.valueAs(TributarySlot.class)))
                .collect(Collectors.toSet()));
        return ImmutableList.of(circuitIntent);
    }

    private List<Resource> availableSlotResources(ConnectPoint src, ConnectPoint dst, CltSignalType signalType) {
        OduSignalType oduSignalType = OduSignalUtils.mappingCltSignalTypeToOduSignalType(signalType);
        int requestedTsNum = oduSignalType.tributarySlots();
        Set<TributarySlot> commonTributarySlots = findCommonTributarySlotsOnCps(src, dst);
        if (commonTributarySlots.isEmpty()) {
            return Collections.emptyList();
        }
        if (commonTributarySlots.size() < requestedTsNum) {
            return Collections.emptyList();
        }

        Set<TributarySlot> tributarySlots = commonTributarySlots.stream()
                .limit(requestedTsNum)
                .collect(Collectors.toSet());

        final List<ConnectPoint> portsList = ImmutableList.of(src, dst);
        List<Resource> tributarySlotResources = portsList.stream()
                .flatMap(cp -> tributarySlots
                        .stream()
                        .map(ts-> Resources.discrete(cp.deviceId(), cp.port()).resource().child(ts)))
                .collect(Collectors.toList());

        if (!tributarySlotResources.stream().allMatch(resourceService::isAvailable)) {
            log.debug("Resource allocation for {} on {} and {} failed (resource request: {})",
                    signalType, src, dst, tributarySlotResources);
            return Collections.emptyList();
        }
        return tributarySlotResources;
    }

    private FlowRuleIntent createFlowRule(OpticalCircuitIntent higherIntent,
                                          OpticalConnectivityIntent lowerIntent, Set<TributarySlot> slots) {
        // Create optical circuit intent
        List<FlowRule> rules = new LinkedList<>();
        // at the source: ODUCLT port mapping to OCH port
        rules.add(connectPorts(higherIntent.getSrc(), lowerIntent.getSrc(), higherIntent.priority(), slots));
        // at the destination: OCH port mapping to ODUCLT port
        rules.add(connectPorts(lowerIntent.getDst(), higherIntent.getDst(), higherIntent.priority(), slots));

        // Create flow rules for reverse path
        if (higherIntent.isBidirectional()) {
           // at the destination: OCH port mapping to ODUCLT port
            rules.add(connectPorts(lowerIntent.getSrc(), higherIntent.getSrc(), higherIntent.priority(), slots));
            // at the source: ODUCLT port mapping to OCH port
            rules.add(connectPorts(higherIntent.getDst(), lowerIntent.getDst(), higherIntent.priority(), slots));
        }

        return new FlowRuleIntent(appId, rules, higherIntent.resources());
    }

    /**
     * Returns existing and available optical connectivity intent that matches the given circuit intent.
     *
     * @param src source connect point of optical circuit intent
     * @param dst destination connect point of optical circuit intent
     * @param signalType signal type of optical circuit intent
     * @param multiplexingSupported indicates whether ODU multiplexing is supported
     * @return existing optical connectivity intent, null otherwise.
     */
    private OpticalConnectivityIntent findOpticalConnectivityIntent(ConnectPoint src,
                                                                    ConnectPoint dst,
                                                                    CltSignalType signalType,
                                                                    boolean multiplexingSupported) {

        OduSignalType oduSignalType = OduSignalUtils.mappingCltSignalTypeToOduSignalType(signalType);

        return Tools.stream(intentService.getIntents())
                .filter(x -> x instanceof OpticalConnectivityIntent)
                .map(x -> (OpticalConnectivityIntent) x)
                .filter(x -> src.deviceId().equals(x.getSrc().deviceId()))
                .filter(x -> dst.deviceId().equals(x.getDst().deviceId()))
                .filter(x -> isAllowed(src, x.getSrc()))
                .filter(x -> isAllowed(dst, x.getDst()))
                .filter(x -> isAvailable(x.id()))
                .filter(x -> !multiplexingSupported ||
                        isAvailableTributarySlots(x.getSrc(), x.getDst(), oduSignalType.tributarySlots()))
                .findFirst()
                .orElse(null);
    }

    private boolean isAllowed(ConnectPoint circuitCp, ConnectPoint connectivityCp) {
        ConnectPoint staticPort = staticPort(circuitCp);
        return staticPort == null || staticPort.equals(connectivityCp);
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

    private boolean isAvailableTributarySlots(ConnectPoint src, ConnectPoint dst, int requestedTsNum) {
        Set<TributarySlot> common = findCommonTributarySlotsOnCps(src, dst);
        if (common.isEmpty()) {
            log.debug("No available TributarySlots");
            return false;
        }
        if (common.size() < requestedTsNum) {
            log.debug("Not enough available TributarySlots={} < requestedTsNum={}", common.size(), requestedTsNum);
            return false;
        }
        return true;
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

    private Pair<OchPort, OchPort> findPorts(ConnectPoint src, ConnectPoint dst, CltSignalType signalType) {
        // According to the OpticalCircuitIntent's signalType find OCH ports with available TributarySlots resources
        switch (signalType) {
            case CLT_1GBE:
            case CLT_10GBE:
                // First search for OCH ports with OduSignalType of ODU2. If not found - search for those with ODU4
                return findPorts(src, dst, OduSignalType.ODU2)
                        .orElse(findPorts(src, dst, OduSignalType.ODU4).orElse(null));
            case CLT_100GBE:
                return findPorts(src, dst, OduSignalType.ODU4).orElse(null);
            case CLT_40GBE:
            default:
                return null;
        }
    }

    private Optional<Pair<OchPort, OchPort>> findPorts(ConnectPoint src, ConnectPoint dst,
                                                       OduSignalType ochPortSignalType) {
        return findAvailableOchPort(src, ochPortSignalType)
                .flatMap(srcOch ->
                        findAvailableOchPort(dst, ochPortSignalType).map(dstOch -> Pair.of(srcOch, dstOch)));
    }

    private Optional<OchPort> findAvailableOchPort(ConnectPoint oduPort, OduSignalType ochPortSignalType) {
        // First see if the port mappings are constrained
        ConnectPoint ochCP = staticPort(oduPort);

        if (ochCP != null) {
            OchPort ochPort = (OchPort) deviceService.getPort(ochCP.deviceId(), ochCP.port());
            Optional<IntentId> intentId =
                    resourceService.getResourceAllocations(Resources.discrete(ochCP.deviceId(), ochCP.port()).id())
                            .stream()
                            .map(ResourceAllocation::consumerId)
                            .map(ResourceHelper::getIntentId)
                            .flatMap(Tools::stream)
                            .findAny();

            if (isAvailable(intentId.orElse(null))) {
                return Optional.of(ochPort);
            }
            return Optional.empty();
        }

        // No port constraints, so find any port that works
        List<Port> ports = deviceService.getPorts(oduPort.deviceId());

        for (Port port : ports) {
            if (!(port instanceof OchPort)) {
                continue;
            }
            // This should be the first allocation on the OCH port
            if (!resourceService.isAvailable(Resources.discrete(oduPort.deviceId(), port.number()).resource())) {
                continue;
            }
            // OchPort is required to have the requested oduSignalType
            if (((OchPort) port).signalType() != ochPortSignalType) {
                continue;
            }

            Optional<IntentId> intentId =
                    resourceService.getResourceAllocations(Resources.discrete(oduPort.deviceId(), port.number()).id())
                            .stream()
                            .map(ResourceAllocation::consumerId)
                            .map(ResourceHelper::getIntentId)
                            .flatMap(Tools::stream)
                            .findAny();

            if (isAvailable(intentId.orElse(null))) {
                return Optional.of((OchPort) port);
            }
        }

        return Optional.empty();
    }

    /**
     * Builds flow rule for mapping between two ports.
     *
     * @param src source port
     * @param dst destination port
     * @param priority
     * @param slots Set of TributarySlots
     * @return flow rules
     */
    private FlowRule connectPorts(ConnectPoint src, ConnectPoint dst, int priority, Set<TributarySlot> slots) {
        checkArgument(src.deviceId().equals(dst.deviceId()));

        TrafficSelector.Builder selectorBuilder = DefaultTrafficSelector.builder();
        TrafficTreatment.Builder treatmentBuilder = DefaultTrafficTreatment.builder();

        selectorBuilder.matchInPort(src.port());
        if (!slots.isEmpty()) {
            Port srcPort = deviceService.getPort(src.deviceId(), src.port());
            Port dstPort = deviceService.getPort(dst.deviceId(), dst.port());
            OduSignalType oduCltPortOduSignalType;
            OduSignalType ochPortOduSignalType;

            if (srcPort instanceof OduCltPort) {
                oduCltPortOduSignalType =
                        OduSignalUtils.mappingCltSignalTypeToOduSignalType(((OduCltPort) srcPort).signalType());
                ochPortOduSignalType = ((OchPort) dstPort).signalType();

                selectorBuilder.add(Criteria.matchOduSignalType(oduCltPortOduSignalType));
                // use Instruction of OduSignalId only in case of ODU Multiplexing
                if (oduCltPortOduSignalType != ochPortOduSignalType) {
                    OduSignalId oduSignalId = OduSignalUtils.buildOduSignalId(ochPortOduSignalType, slots);
                    treatmentBuilder.add(Instructions.modL1OduSignalId(oduSignalId));
                }
            } else { // srcPort is OchPort
                oduCltPortOduSignalType =
                        OduSignalUtils.mappingCltSignalTypeToOduSignalType(((OduCltPort) dstPort).signalType());
                ochPortOduSignalType = ((OchPort) srcPort).signalType();

                selectorBuilder.add(Criteria.matchOduSignalType(oduCltPortOduSignalType));
                // use Criteria of OduSignalId only in case of ODU Multiplexing
                if (oduCltPortOduSignalType != ochPortOduSignalType) {
                    OduSignalId oduSignalId = OduSignalUtils.buildOduSignalId(ochPortOduSignalType, slots);
                    selectorBuilder.add(Criteria.matchOduSignalId(oduSignalId));
                }
            }
        }
        treatmentBuilder.setOutput(dst.port());

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

    private boolean isMultiplexingSupported(ConnectPoint cp) {
        Driver driver = driverService.getDriver(cp.deviceId());
        return driver != null
                && driver.hasBehaviour(TributarySlotQuery.class)
                && staticPort(cp) == null;
    }

    /**
     * Finds the common TributarySlots available on the two connect points.
     *
     * @param src source connect point
     * @param dst dest connect point
     * @return set of common TributarySlots on both connect points
     */
    Set<TributarySlot> findCommonTributarySlotsOnCps(ConnectPoint src, ConnectPoint dst) {
        Set<TributarySlot> forward = findTributarySlotsOnCp(src);
        Set<TributarySlot> backward = findTributarySlotsOnCp(dst);
        return Sets.intersection(forward, backward);
    }

    /**
     * Finds the TributarySlots available on the connect point.
     *
     * @param cp connect point
     * @return set of TributarySlots available on the connect point
     */
    Set<TributarySlot> findTributarySlotsOnCp(ConnectPoint cp) {
        return resourceService.getAvailableResourceValues(
                Resources.discrete(cp.deviceId(), cp.port()).id(),
                TributarySlot.class);
    }
}

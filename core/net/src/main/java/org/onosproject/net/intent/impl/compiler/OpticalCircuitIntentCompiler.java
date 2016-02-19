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
import org.onosproject.net.DeviceId;
import org.onosproject.net.OchPort;
import org.onosproject.net.OduCltPort;
import org.onosproject.net.OduSignalId;
import org.onosproject.net.OduSignalType;
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
import org.onosproject.net.intent.impl.IntentCompilationException;
import org.onosproject.net.newresource.ResourceAllocation;
import org.onosproject.net.newresource.Resource;
import org.onosproject.net.newresource.ResourceService;
import org.onosproject.net.newresource.Resources;
import org.onosproject.net.resource.device.IntentSetMultimap;
import org.onosproject.net.resource.link.LinkResourceAllocations;
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

import static com.google.common.base.Preconditions.checkArgument;

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

        // Release of intent resources here is only a temporary solution for handling the
        // case of recompiling due to intent restoration (when intent state is FAILED).
        // TODO: try to release intent resources in IntentManager.
        resourceService.release(intent.id());

        // Reserve OduClt ports
        Resource srcPortResource = Resources.discrete(src.deviceId(), src.port()).resource();
        Resource dstPortResource = Resources.discrete(dst.deviceId(), dst.port()).resource();
        List<ResourceAllocation> allocation = resourceService.allocate(intent.id(), srcPortResource, dstPortResource);
        if (allocation.isEmpty()) {
            throw new IntentCompilationException("Unable to reserve ports for intent " + intent);
        }

        // Check if both devices support multiplexing (usage of TributarySlots)
        boolean multiplexingSupported = isMultiplexingSupported(intent);

        LinkedList<Intent> intents = new LinkedList<>();
        // slots are used only for devices supporting multiplexing
        Set<TributarySlot> slots = Collections.emptySet();

        OpticalConnectivityIntent connIntent = findOpticalConnectivityIntent(intent, multiplexingSupported);
        if ((connIntent != null) && multiplexingSupported) {
            // Allocate TributarySlots on existing OCH ports
            slots = assignTributarySlots(intent, Pair.of(connIntent.getSrc(), connIntent.getDst()));
        }

        // Create optical connectivity intent if needed - no optical intent or not enough slots available
        if (connIntent == null || (multiplexingSupported && slots.isEmpty())) {
            // Find OCh ports with available resources
            Pair<OchPort, OchPort> ochPorts = findPorts(intent);

            if (ochPorts == null) {
                // Release port allocations if unsuccessful
                resourceService.release(intent.id());
                throw new IntentCompilationException("Unable to find suitable OCH ports for intent " + intent);
            }

            ConnectPoint srcCP = new ConnectPoint(src.elementId(), ochPorts.getLeft().number());
            ConnectPoint dstCP = new ConnectPoint(dst.elementId(), ochPorts.getRight().number());

            if (multiplexingSupported) {
                // Allocate TributarySlots on OCH ports
                slots = assignTributarySlots(intent, Pair.of(srcCP, dstCP));
                if (slots.isEmpty()) {
                    // Release port allocations if unsuccessful
                    resourceService.release(intent.id());
                    throw new IntentCompilationException("Unable to find Tributary Slots for intent " + intent);
                }
            }

            // Create optical connectivity intent
            OduSignalType signalType = ochPorts.getLeft().signalType();
            connIntent = OpticalConnectivityIntent.builder()
                    .appId(appId)
                    .src(srcCP)
                    .dst(dstCP)
                    .signalType(signalType)
                    .bidirectional(intent.isBidirectional())
                    .build();
            intentService.submit(connIntent);
        }

        // Create optical circuit intent
        List<FlowRule> rules = new LinkedList<>();
        // at the source: ODUCLT port mapping to OCH port
        rules.add(connectPorts(src, connIntent.getSrc(), intent.priority(), slots));
        // at the destination: OCH port mapping to ODUCLT port
        rules.add(connectPorts(connIntent.getDst(), dst, intent.priority(), slots));

        // Create flow rules for reverse path
        if (intent.isBidirectional()) {
           // at the destination: OCH port mapping to ODUCLT port
            rules.add(connectPorts(connIntent.getSrc(), src, intent.priority(), slots));
            // at the source: ODUCLT port mapping to OCH port
            rules.add(connectPorts(dst, connIntent.getDst(), intent.priority(), slots));
        }

        FlowRuleIntent circuitIntent = new FlowRuleIntent(appId, rules, intent.resources());

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
     * @param multiplexingSupported indicates whether ODU multiplexing is supported
     * @return existing optical connectivity intent, null otherwise.
     */
    private OpticalConnectivityIntent findOpticalConnectivityIntent(OpticalCircuitIntent circuitIntent,
            boolean multiplexingSupported) {

        OduSignalType oduSignalType = mappingCltSignalTypeToOduSignalType(circuitIntent.getSignalType());

        for (Intent intent : intentService.getIntents()) {
            if (!(intent instanceof OpticalConnectivityIntent)) {
                continue;
            }

            OpticalConnectivityIntent connIntent = (OpticalConnectivityIntent) intent;

            ConnectPoint src = circuitIntent.getSrc();
            ConnectPoint dst = circuitIntent.getDst();
            // Ignore if the intents don't have identical src and dst devices
            if (!src.deviceId().equals(connIntent.getSrc().deviceId()) ||
                    !dst.deviceId().equals(connIntent.getDst().deviceId())) {
                continue;
            }

            if (!isAllowed(circuitIntent, connIntent)) {
                continue;
            }

            if (!isAvailable(connIntent.id())) {
                continue;
            }

            if (multiplexingSupported) {
                if (!isAvailableTributarySlots(connIntent, oduSignalType.tributarySlots())) {
                    continue;
                }
            }

            return connIntent;
        }

        return null;
    }

    private boolean isAvailableTributarySlots(OpticalConnectivityIntent connIntent, int requestedTsNum) {
        Set<TributarySlot> common = findCommonTributarySlotsOnCps(connIntent.getSrc(), connIntent.getDst());
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

    private Set<TributarySlot> assignTributarySlots(OpticalCircuitIntent intent,
            Pair<ConnectPoint, ConnectPoint> ports) {

        OduSignalType oduSignalType = mappingCltSignalTypeToOduSignalType(intent.getSignalType());
        int requestedTsNum = oduSignalType.tributarySlots();
        Set<TributarySlot> commonTributarySlots = findCommonTributarySlotsOnCps(ports.getLeft(), ports.getRight());
        if (commonTributarySlots.isEmpty()) {
            return Collections.emptySet();
        }
        if (commonTributarySlots.size() < requestedTsNum) {
            return Collections.emptySet();
        }

        Set<TributarySlot> tributarySlots = commonTributarySlots.stream()
                .limit(requestedTsNum)
                .collect(Collectors.toSet());

        final List<ConnectPoint> portsList = ImmutableList.of(ports.getLeft(), ports.getRight());
        List<Resource> tributarySlotResources = portsList.stream()
                .flatMap(cp -> tributarySlots
                        .stream()
                        .map(ts-> Resources.discrete(cp.deviceId(), cp.port()).resource().child(ts)))
                .collect(Collectors.toList());

        List<ResourceAllocation> allocations = resourceService.allocate(intent.id(), tributarySlotResources);
        if (allocations.isEmpty()) {
            log.debug("Resource allocation for {} failed (resource request: {})",
                    intent, tributarySlotResources);
            return Collections.emptySet();
        }
        return tributarySlots;
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

    private OchPort findAvailableOchPort(ConnectPoint oduPort, OduSignalType ochPortSignalType) {
        // First see if the port mappings are constrained
        ConnectPoint ochCP = staticPort(oduPort);

        if (ochCP != null) {
            OchPort ochPort = (OchPort) deviceService.getPort(ochCP.deviceId(), ochCP.port());
            Optional<IntentId> intentId =
                    resourceService.getResourceAllocations(Resources.discrete(ochCP.deviceId(), ochCP.port()).id())
                            .stream()
                            .map(ResourceAllocation::consumer)
                            .filter(x -> x instanceof IntentId)
                            .map(x -> (IntentId) x)
                            .findAny();

            if (isAvailable(intentId.orElse(null))) {
                return ochPort;
            }
            return null;
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
                            .map(ResourceAllocation::consumer)
                            .filter(x -> x instanceof IntentId)
                            .map(x -> (IntentId) x)
                            .findAny();

            if (isAvailable(intentId.orElse(null))) {
                return (OchPort) port;
            }
        }

        return null;
    }

    private Pair<OchPort, OchPort> findPorts(OpticalCircuitIntent intent) {
        Pair<OchPort, OchPort> ochPorts = null;
        // According to the OpticalCircuitIntent's signalType find OCH ports with available TributarySlots resources
        switch (intent.getSignalType()) {
            case CLT_1GBE:
            case CLT_10GBE:
                // First search for OCH ports with OduSignalType of ODU2. If not found - search for those with ODU4
                ochPorts = findPorts(intent, OduSignalType.ODU2);
                if (ochPorts == null) {
                    ochPorts = findPorts(intent, OduSignalType.ODU4);
                }
                break;
            case CLT_100GBE:
                ochPorts = findPorts(intent, OduSignalType.ODU4);
                break;
            case CLT_40GBE:
            default:
                break;
        }
        return ochPorts;
    }

    private Pair<OchPort, OchPort> findPorts(OpticalCircuitIntent intent, OduSignalType ochPortSignalType) {
        OchPort srcPort = findAvailableOchPort(intent.getSrc(), ochPortSignalType);
        if (srcPort == null) {
            return null;
        }

        OchPort dstPort = findAvailableOchPort(intent.getDst(), ochPortSignalType);
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
                oduCltPortOduSignalType = mappingCltSignalTypeToOduSignalType(((OduCltPort) srcPort).signalType());
                ochPortOduSignalType = ((OchPort) dstPort).signalType();

                selectorBuilder.add(Criteria.matchOduSignalType(oduCltPortOduSignalType));
                // use Instruction of OduSignalId only in case of ODU Multiplexing
                if (oduCltPortOduSignalType != ochPortOduSignalType) {
                    OduSignalId oduSignalId = buildOduSignalId(ochPortOduSignalType, slots);
                    treatmentBuilder.add(Instructions.modL1OduSignalId(oduSignalId));
                }
            } else { // srcPort is OchPort
                oduCltPortOduSignalType = mappingCltSignalTypeToOduSignalType(((OduCltPort) dstPort).signalType());
                ochPortOduSignalType = ((OchPort) srcPort).signalType();

                selectorBuilder.add(Criteria.matchOduSignalType(oduCltPortOduSignalType));
                // use Criteria of OduSignalId only in case of ODU Multiplexing
                if (oduCltPortOduSignalType != ochPortOduSignalType) {
                    OduSignalId oduSignalId = buildOduSignalId(ochPortOduSignalType, slots);
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

    private OduSignalId buildOduSignalId(OduSignalType ochPortSignalType, Set<TributarySlot> slots) {
        int tributaryPortNumber = findFirstTributarySlotIndex(slots);
        int tributarySlotLen = ochPortSignalType.tributarySlots();
        byte[] tributarySlotBitmap = new byte[OduSignalId.TRIBUTARY_SLOT_BITMAP_SIZE];

        slots.forEach(ts -> tributarySlotBitmap[(byte) (ts.index() - 1) / 8] |= 0x1 << ((ts.index() - 1) % 8));
        return OduSignalId.oduSignalId(tributaryPortNumber, tributarySlotLen, tributarySlotBitmap);
    }

    private int findFirstTributarySlotIndex(Set<TributarySlot> tributarySlots) {
        return (int) tributarySlots.stream().findFirst().get().index();
    }

    private boolean isTributarySlotBehaviourSupported(DeviceId deviceId) {
        Driver driver = driverService.getDriver(deviceId);
        return (driver != null && driver.hasBehaviour(TributarySlotQuery.class));
    }

    private boolean isMultiplexingSupported(OpticalCircuitIntent intent) {
        ConnectPoint src = intent.getSrc();
        ConnectPoint dst = intent.getDst();

        if (!isTributarySlotBehaviourSupported(src.deviceId()) ||
                !isTributarySlotBehaviourSupported(dst.deviceId())) {
            return false;
        }

        ConnectPoint srcStaticPort = staticPort(src);
        if (srcStaticPort != null) {
            return false;
        }
        ConnectPoint dstStaticPort = staticPort(dst);
        if (dstStaticPort != null) {
            return false;
        }

        return true;
    }

    /**
     * Maps from Intent's OduClt SignalType to OduSignalType.
     *
     * @param cltSignalType OduClt port signal type
     * @return OduSignalType the result of mapping CltSignalType to OduSignalType
     */
    OduSignalType mappingCltSignalTypeToOduSignalType(CltSignalType cltSignalType) {
        OduSignalType oduSignalType = OduSignalType.ODU0;
        switch (cltSignalType) {
            case CLT_1GBE:
                oduSignalType = OduSignalType.ODU0;
                break;
            case CLT_10GBE:
                oduSignalType = OduSignalType.ODU2;
                break;
            case CLT_40GBE:
                oduSignalType = OduSignalType.ODU3;
                break;
            case CLT_100GBE:
                oduSignalType = OduSignalType.ODU4;
                break;
            default:
                log.error("Unsupported CltSignalType {}", cltSignalType);
                break;
        }
        return oduSignalType;
    }

    /**
     * Finds the common TributarySlots available on the two connect points.
     *
     * @param srcCp source connect point
     * @param dstCp dest connect point
     * @return set of common TributarySlots on both connect points
     */
    Set<TributarySlot> findCommonTributarySlotsOnCps(ConnectPoint srcCp, ConnectPoint dstCp) {
        Set<TributarySlot> forward = findTributarySlotsOnCp(srcCp);
        Set<TributarySlot> backward = findTributarySlotsOnCp(dstCp);
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

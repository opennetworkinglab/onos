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


import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.onlab.graph.ScalarWeight;
import org.onlab.graph.Weight;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Link;
import org.onosproject.net.LinkKey;
import org.onosproject.net.OduSignalId;
import org.onosproject.net.OduSignalType;
import org.onosproject.net.OduSignalUtils;
import org.onosproject.net.Path;
import org.onosproject.net.Port;
import org.onosproject.net.TributarySlot;
import org.onosproject.net.device.DeviceService;
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
import org.onosproject.net.intent.OpticalOduIntent;
import org.onosproject.net.intent.PathIntent;
import org.onosproject.net.optical.OduCltPort;
import org.onosproject.net.optical.OtuPort;
import org.onosproject.net.resource.Resource;
import org.onosproject.net.resource.ResourceService;
import org.onosproject.net.resource.ResourceAllocation;
import org.onosproject.net.resource.Resources;
import org.onosproject.net.topology.LinkWeigher;
import org.onosproject.net.topology.Topology;
import org.onosproject.net.topology.TopologyEdge;
import org.onosproject.net.topology.TopologyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.google.common.base.Preconditions.checkArgument;
import static org.onosproject.net.LinkKey.linkKey;
import static org.onosproject.net.optical.device.OpticalDeviceServiceView.opticalView;

/**
 * An intent compiler for {@link org.onosproject.net.intent.OpticalOduIntent}.
 */
@Component(immediate = true)
public class OpticalOduIntentCompiler implements IntentCompiler<OpticalOduIntent> {

    private static final Logger log = LoggerFactory.getLogger(OpticalOduIntentCompiler.class);

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected IntentExtensionService intentManager;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected TopologyService topologyService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected DeviceService deviceService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected ResourceService resourceService;

    private ApplicationId appId;

    @Activate
    public void activate() {
        deviceService = opticalView(deviceService);
        appId = coreService.registerApplication("org.onosproject.net.intent");
        intentManager.registerCompiler(OpticalOduIntent.class, this);
    }

    @Deactivate
    public void deactivate() {
        intentManager.unregisterCompiler(OpticalOduIntent.class);
    }

    @Override
    public List<Intent> compile(OpticalOduIntent intent, List<Intent> installable) {
        // Check if ports are OduClt ports
        ConnectPoint src = intent.getSrc();
        ConnectPoint dst = intent.getDst();
        Port srcPort = deviceService.getPort(src.deviceId(), src.port());
        Port dstPort = deviceService.getPort(dst.deviceId(), dst.port());
        checkArgument(srcPort instanceof OduCltPort);
        checkArgument(dstPort instanceof OduCltPort);

        log.debug("Compiling optical ODU intent between {} and {}", src, dst);

        // Release of intent resources here is only a temporary solution for handling the
        // case of recompiling due to intent restoration (when intent state is FAILED).
        // TODO: try to release intent resources in IntentManager.
        resourceService.release(intent.key());

        // Check OduClt ports availability
        Resource srcPortResource = Resources.discrete(src.deviceId(), src.port()).resource();
        Resource dstPortResource = Resources.discrete(dst.deviceId(), dst.port()).resource();
        // If ports are not available, compilation fails
        if (!Stream.of(srcPortResource, dstPortResource).allMatch(resourceService::isAvailable)) {
            throw new OpticalIntentCompilationException("Ports for the intent are not available. Intent: " + intent);
        }
        List<Resource> intentResources = new ArrayList<>();
        intentResources.add(srcPortResource);
        intentResources.add(dstPortResource);

        // Calculate available light paths
        Set<Path> paths = getOpticalPaths(intent);

        if (paths.isEmpty()) {
            throw new OpticalIntentCompilationException("Unable to find suitable lightpath for intent " + intent);
        }

        // Use first path that can be successfully reserved
        for (Path path : paths) {

            // Find available Tributary Slots on both directions of path
            Map<LinkKey, Set<TributarySlot>> slotsMap = findAvailableTributarySlots(intent, path);
            if (slotsMap.isEmpty()) {
                continue;
            }
            List<Resource> tributarySlotResources = convertToResources(slotsMap);
            if (!tributarySlotResources.stream().allMatch(resourceService::isAvailable)) {
                continue;
            }

            intentResources.addAll(tributarySlotResources);

            allocateResources(intent, intentResources);

            List<FlowRule> rules = new LinkedList<>();

            // Create rules for forward and reverse path
            rules = createRules(intent, intent.getSrc(), intent.getDst(), path, slotsMap, false);
            if (intent.isBidirectional()) {
                rules.addAll(createRules(intent, intent.getDst(), intent.getSrc(), path, slotsMap, true));
            }

            return Collections.singletonList(
                    new FlowRuleIntent(appId,
                                       intent.key(),
                                       rules,
                                       ImmutableSet.copyOf(path.links()),
                                       PathIntent.ProtectionType.PRIMARY,
                                       intent.resourceGroup()));
        }

        throw new OpticalIntentCompilationException("Unable to find suitable lightpath for intent " + intent);
    }

    /**
     * Find available TributarySlots across path.
     *
     * @param intent
     * @param path path in OTU topology
     * @return Map of Linkey and Set of available TributarySlots on its ports
     */
    private Map<LinkKey, Set<TributarySlot>> findAvailableTributarySlots(OpticalOduIntent intent, Path path) {
        Set<LinkKey> linkRequest = Sets.newHashSetWithExpectedSize(path.links().size());
        for (int i = 0; i < path.links().size(); i++) {
            LinkKey link = linkKey(path.links().get(i));
            linkRequest.add(link);
        }

        return findTributarySlots(intent, linkRequest);
    }

    private List<Resource> convertToResources(Map<LinkKey, Set<TributarySlot>> slotsMap) {
       // Same TributarySlots are used for both directions
        Set<Resource> resources = slotsMap.entrySet().stream()
                .flatMap(x -> x.getValue()
                        .stream()
                        .flatMap(ts -> Stream.of(
                                Resources.discrete(x.getKey().src().deviceId(), x.getKey().src().port())
                                        .resource().child(ts),
                                Resources.discrete(x.getKey().dst().deviceId(), x.getKey().dst().port())
                                        .resource().child(ts))))
                .collect(Collectors.toSet());
        return (ImmutableList.copyOf(resources));
    }

    private void allocateResources(Intent intent, List<Resource> resources) {
        // reserve all of required resources
        List<ResourceAllocation> allocations = resourceService.allocate(intent.key(), resources);
        if (allocations.isEmpty()) {
            log.info("Resource allocation for {} failed (resource request: {})", intent, resources);
            throw new OpticalIntentCompilationException("Unable to allocate resources: " + resources);
        }
    }

    private Map<LinkKey, Set<TributarySlot>> findTributarySlots(OpticalOduIntent intent, Set<LinkKey> links) {
        OduSignalType oduSignalType = OduSignalUtils.mappingCltSignalTypeToOduSignalType(intent.getSignalType());
        int requestedTsNum = oduSignalType.tributarySlots();

        Map<LinkKey, Set<TributarySlot>> slotsMap = new HashMap<>();
        for (LinkKey link : links) {
            Set<TributarySlot> common = findCommonTributarySlotsOnCps(link.src(), link.dst());
            if (common.isEmpty() || (common.size() < requestedTsNum)) {
                log.debug("Failed to find TributarySlots on link {} requestedTsNum={}", link, requestedTsNum);
                return Collections.emptyMap(); // failed to find enough available TributarySlots on a link
            }
            slotsMap.put(link, common.stream()
                                .limit(requestedTsNum)
                                .collect(Collectors.toSet()));
        }
        return slotsMap;
    }

    /**
     * Calculates optical paths in OTU topology.
     *
     * @param intent optical ODU intent
     * @return set of paths in OTU topology
     */
    private Set<Path> getOpticalPaths(OpticalOduIntent intent) {
        // Route in OTU topology
        Topology topology = topologyService.currentTopology();


        class Weigher implements LinkWeigher {
            @Override
            public Weight weight(TopologyEdge edge) {
                if (edge.link().state() == Link.State.INACTIVE) {
                    return ScalarWeight.toWeight(-1);
                }
                if (edge.link().type() != Link.Type.OPTICAL) {
                    return ScalarWeight.toWeight(-1);
                }
                // Find path with available TributarySlots resources
                if (!isAvailableTributarySlots(intent, edge.link())) {
                    return ScalarWeight.toWeight(-1);
                }
                return ScalarWeight.toWeight(1);
            }

            @Override
            public Weight getInitialWeight() {
                return null;
            }

            @Override
            public Weight getNonViableWeight() {
                return null;
            }
        }


        LinkWeigher weigher = new Weigher();

        ConnectPoint start = intent.getSrc();
        ConnectPoint end = intent.getDst();

        return topologyService.getPaths(topology, start.deviceId(), end.deviceId(), weigher);
    }

    private boolean isAvailableTributarySlots(OpticalOduIntent intent, Link link) {
        OduSignalType oduSignalType = OduSignalUtils.mappingCltSignalTypeToOduSignalType(intent.getSignalType());
        int requestedTsNum = oduSignalType.tributarySlots();

        Set<TributarySlot> common = findCommonTributarySlotsOnCps(link.src(), link.dst());
        if (common.isEmpty() || (common.size() < requestedTsNum)) {
            log.debug("Not enough available TributarySlots on link {} requestedTsNum={}", link, requestedTsNum);
            return false;
        }
        return true;
    }

    /**
     * Create rules for the forward (or the reverse) path of the intent.
     *
     * @param intent OpticalOduIntent intent
     * @param path path found for intent
     * @param slotsMap Map of LinkKey and TributarySlots resources
     * @return list of flow rules
     */
    private List<FlowRule> createRules(OpticalOduIntent intent, ConnectPoint src, ConnectPoint dst,
            Path path, Map<LinkKey, Set<TributarySlot>> slotsMap, boolean reverse) {
        // Build the ingress OTN rule
        TrafficSelector.Builder selector = DefaultTrafficSelector.builder();
        selector.matchInPort(src.port());
        OduSignalType oduCltPortOduSignalType =
                OduSignalUtils.mappingCltSignalTypeToOduSignalType(intent.getSignalType());
        selector.add(Criteria.matchOduSignalType(oduCltPortOduSignalType));

        List<FlowRule> rules = new LinkedList<>();
        ConnectPoint current = src;

        List<Link> links = ((!reverse) ? path.links() : Lists.reverse(path.links()));

        for (Link link : links) {
            Set<TributarySlot> slots = slotsMap.get(linkKey(link));
            OtuPort otuPort = (OtuPort) (deviceService.getPort(link.src().deviceId(), link.src().port()));
            OduSignalType otuPortOduSignalType =
                    OduSignalUtils.mappingOtuSignalTypeToOduSignalType(otuPort.signalType());

            TrafficTreatment.Builder treat = DefaultTrafficTreatment.builder();
            OduSignalId oduSignalId = null;
            // use Instruction of OduSignalId only in case of ODU Multiplexing
            if (oduCltPortOduSignalType != otuPortOduSignalType) {
                oduSignalId = OduSignalUtils.buildOduSignalId(otuPortOduSignalType, slots);
                treat.add(Instructions.modL1OduSignalId(oduSignalId));
            }
            ConnectPoint next = ((!reverse) ? link.src() : link.dst());
            treat.setOutput(next.port());

            FlowRule rule = createFlowRule(intent, current.deviceId(), selector.build(), treat.build());
            rules.add(rule);

            current = ((!reverse) ? link.dst() : link.src());
            selector = DefaultTrafficSelector.builder();
            selector.matchInPort(current.port());
            selector.add(Criteria.matchOduSignalType(oduCltPortOduSignalType));
            // use Criteria of OduSignalId only in case of ODU Multiplexing
            if (oduCltPortOduSignalType != otuPortOduSignalType) {
                selector.add(Criteria.matchOduSignalId(oduSignalId));
            }
        }

        // Build the egress OTN rule
        TrafficTreatment.Builder treatLast = DefaultTrafficTreatment.builder();
        treatLast.setOutput(dst.port());

        FlowRule rule = createFlowRule(intent, dst.deviceId(), selector.build(), treatLast.build());
        rules.add(rule);

        return rules;
    }

    private FlowRule createFlowRule(OpticalOduIntent intent, DeviceId deviceId,
            TrafficSelector selector, TrafficTreatment treat) {
        return DefaultFlowRule.builder()
                .forDevice(deviceId)
                .withSelector(selector)
                .withTreatment(treat)
                .withPriority(intent.priority())
                .fromApp(appId)
                .makePermanent()
                .build();
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

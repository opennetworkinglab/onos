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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.onlab.util.Frequency;
import org.onosproject.net.AnnotationKeys;
import org.onosproject.net.ChannelSpacing;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DefaultOchSignalComparator;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Link;
import org.onosproject.net.OchSignal;
import org.onosproject.net.OchSignalType;
import org.onosproject.net.Path;
import org.onosproject.net.Port;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.intent.Intent;
import org.onosproject.net.intent.IntentCompiler;
import org.onosproject.net.intent.IntentExtensionService;
import org.onosproject.net.intent.OpticalConnectivityIntent;
import org.onosproject.net.intent.OpticalPathIntent;
import org.onosproject.net.optical.OchPort;
import org.onosproject.net.resource.ResourceAllocation;
import org.onosproject.net.resource.Resource;
import org.onosproject.net.resource.ResourceService;
import org.onosproject.net.resource.Resources;
import org.onosproject.net.topology.LinkWeight;
import org.onosproject.net.topology.Topology;
import org.onosproject.net.topology.TopologyEdge;
import org.onosproject.net.topology.TopologyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.google.common.base.Preconditions.checkArgument;
import static org.onosproject.net.optical.device.OpticalDeviceServiceView.opticalView;

/**
 * An intent compiler for {@link org.onosproject.net.intent.OpticalConnectivityIntent}.
 */
@Component(immediate = true)
public class OpticalConnectivityIntentCompiler implements IntentCompiler<OpticalConnectivityIntent> {

    protected static final Logger log = LoggerFactory.getLogger(OpticalConnectivityIntentCompiler.class);
    // By default, allocate 50 GHz lambdas (4 slots of 12.5 GHz) for each intent.
    private static final int SLOT_COUNT = 4;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected IntentExtensionService intentManager;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected TopologyService topologyService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DeviceService deviceService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ResourceService resourceService;

    @Activate
    public void activate() {
        deviceService = opticalView(deviceService);
        intentManager.registerCompiler(OpticalConnectivityIntent.class, this);
    }

    @Deactivate
    public void deactivate() {
        intentManager.unregisterCompiler(OpticalConnectivityIntent.class);
    }

    @Override
    public List<Intent> compile(OpticalConnectivityIntent intent,
                                List<Intent> installable) {
        // Check if source and destination are optical OCh ports
        ConnectPoint src = intent.getSrc();
        ConnectPoint dst = intent.getDst();
        Port srcPort = deviceService.getPort(src.deviceId(), src.port());
        Port dstPort = deviceService.getPort(dst.deviceId(), dst.port());
        checkArgument(srcPort instanceof OchPort);
        checkArgument(dstPort instanceof OchPort);

        log.debug("Compiling optical connectivity intent between {} and {}", src, dst);

        // Release of intent resources here is only a temporary solution for handling the
        // case of recompiling due to intent restoration (when intent state is FAILED).
        // TODO: try to release intent resources in IntentManager.
        resourceService.release(intent.id());

        // Check OCh port availability
        Resource srcPortResource = Resources.discrete(src.deviceId(), src.port()).resource();
        Resource dstPortResource = Resources.discrete(dst.deviceId(), dst.port()).resource();
        // If ports are not available, compilation fails
        if (!Stream.of(srcPortResource, dstPortResource).allMatch(resourceService::isAvailable)) {
            throw new OpticalIntentCompilationException("Ports for the intent are not available. Intent: " + intent);
        }

        List<Resource> resources = new ArrayList<>();
        resources.add(srcPortResource);
        resources.add(dstPortResource);

        // Calculate available light paths
        Set<Path> paths = getOpticalPaths(intent);

        if (paths.isEmpty()) {
            throw new OpticalIntentCompilationException("Unable to find suitable lightpath for intent " + intent);
        }

        // Static or dynamic lambda allocation
        String staticLambda = srcPort.annotations().value(AnnotationKeys.STATIC_LAMBDA);
        OchPort srcOchPort = (OchPort) srcPort;
        OchPort dstOchPort = (OchPort) dstPort;

        Path firstPath = paths.iterator().next();
        // FIXME: need to actually reserve the lambda for static lambda's
        // static lambda case: early return
        if (staticLambda != null) {
            allocateResources(intent, resources);

            OchSignal lambda = new OchSignal(Frequency.ofHz(Long.parseLong(staticLambda)),
                    srcOchPort.lambda().channelSpacing(),
                    srcOchPort.lambda().slotGranularity());
            return ImmutableList.of(createIntent(intent, firstPath, lambda));
        }

        // FIXME: also check destination OCh port
        // non-tunable case: early return
        if (!srcOchPort.isTunable() || !dstOchPort.isTunable()) {
            allocateResources(intent, resources);

            OchSignal lambda = srcOchPort.lambda();
            return ImmutableList.of(createIntent(intent, firstPath, lambda));
        }

        // remaining cases
        // Use first path that the required resources are available
        Optional<Map.Entry<Path, List<OchSignal>>> found = paths.stream()
                .map(path -> Maps.immutableEntry(path, findFirstAvailableOch(path)))
                .filter(entry -> !entry.getValue().isEmpty())
                .filter(entry -> convertToResources(entry.getKey().links(),
                        entry.getValue()).stream().allMatch(resourceService::isAvailable))
                .findFirst();

        if (found.isPresent()) {
            resources.addAll(convertToResources(found.get().getKey().links(), found.get().getValue()));

            allocateResources(intent, resources);

            OchSignal ochSignal = OchSignal.toFixedGrid(found.get().getValue(), ChannelSpacing.CHL_50GHZ);
            return ImmutableList.of(createIntent(intent, found.get().getKey(), ochSignal));
        } else {
            throw new OpticalIntentCompilationException("Unable to find suitable lightpath for intent " + intent);
        }
    }

    private Intent createIntent(OpticalConnectivityIntent parentIntent, Path path, OchSignal lambda) {
        // Create installable optical path intent
        // Only support fixed grid for now
        OchSignalType signalType = OchSignalType.FIXED_GRID;

        return OpticalPathIntent.builder()
                .appId(parentIntent.appId())
                .src(parentIntent.getSrc())
                .dst(parentIntent.getDst())
                // calling paths.iterator().next() is safe because of non-empty set
                .path(path)
                .lambda(lambda)
                .signalType(signalType)
                .bidirectional(parentIntent.isBidirectional())
                .build();
    }

    private void allocateResources(Intent intent, List<Resource> resources) {
        // reserve all of required resources
        List<ResourceAllocation> allocations = resourceService.allocate(intent.id(), resources);
        if (allocations.isEmpty()) {
            log.info("Resource allocation for {} failed (resource request: {})", intent, resources);
            throw new OpticalIntentCompilationException("Unable to allocate resources: " + resources);
        }
    }

    private List<OchSignal> findFirstAvailableOch(Path path) {
        Set<OchSignal> lambdas = findCommonLambdasOverLinks(path.links());
        if (lambdas.isEmpty()) {
            return Collections.emptyList();
        }

        return findFirstLambda(lambdas, slotCount());
    }

    private List<Resource> convertToResources(List<Link> links, List<OchSignal> lambda) {
        return links.stream()
                .flatMap(x -> Stream.of(
                        Resources.discrete(x.src().deviceId(), x.src().port()).resource(),
                        Resources.discrete(x.dst().deviceId(), x.dst().port()).resource()
                ))
                .flatMap(x -> lambda.stream().map(x::child))
                .collect(Collectors.toList());
    }

    /**
     * Get the number of 12.5 GHz slots required for the path.
     *
     * For now this returns a constant value of 4 (i.e., fixed grid 50 GHz slot),
     * but in the future can depend on optical reach, line rate, transponder port capabilities, etc.
     *
     * @return number of slots
     */
    private int slotCount() {
        return SLOT_COUNT;
    }

    private Set<OchSignal> findCommonLambdasOverLinks(List<Link> links) {
        return links.stream()
                .flatMap(x -> Stream.of(
                        Resources.discrete(x.src().deviceId(), x.src().port()).id(),
                        Resources.discrete(x.dst().deviceId(), x.dst().port()).id()
                ))
                .map(x -> resourceService.getAvailableResourceValues(x, OchSignal.class))
                .map(x -> (Set<OchSignal>) ImmutableSet.copyOf(x))
                .reduce(Sets::intersection)
                .orElse(Collections.emptySet());
    }

    /**
     * Returns list of consecutive resources in given set of lambdas.
     *
     * @param lambdas list of lambdas
     * @param count number of consecutive lambdas to return
     * @return list of consecutive lambdas
     */
    private List<OchSignal> findFirstLambda(Set<OchSignal> lambdas, int count) {
        // Sort available lambdas
        List<OchSignal> lambdaList = new ArrayList<>(lambdas);
        lambdaList.sort(new DefaultOchSignalComparator());

        // Look ahead by count and ensure spacing multiplier is as expected (i.e., no gaps)
        for (int i = 0; i < lambdaList.size() - count; i++) {
            if (lambdaList.get(i).spacingMultiplier() + 2 * count ==
                    lambdaList.get(i + count).spacingMultiplier()) {
                return lambdaList.subList(i, i + count);
            }
        }

        return Collections.emptyList();
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

    /**
     * Calculates optical paths in WDM topology.
     *
     * @param intent optical connectivity intent
     * @return set of paths in WDM topology
     */
    private Set<Path> getOpticalPaths(OpticalConnectivityIntent intent) {
        // Route in WDM topology
        Topology topology = topologyService.currentTopology();
        LinkWeight weight = new LinkWeight() {

            @Override
            public double weight(TopologyEdge edge) {
                // Disregard inactive or non-optical links
                if (edge.link().state() == Link.State.INACTIVE) {
                    return -1;
                }
                if (edge.link().type() != Link.Type.OPTICAL) {
                    return -1;
                }
                // Adhere to static port mappings
                DeviceId srcDeviceId = edge.link().src().deviceId();
                if (srcDeviceId.equals(intent.getSrc().deviceId())) {
                    ConnectPoint srcStaticPort = staticPort(intent.getSrc());
                    if (srcStaticPort != null) {
                        return srcStaticPort.equals(edge.link().src()) ? 1 : -1;
                    }
                }
                DeviceId dstDeviceId = edge.link().dst().deviceId();
                if (dstDeviceId.equals(intent.getDst().deviceId())) {
                    ConnectPoint dstStaticPort = staticPort(intent.getDst());
                    if (dstStaticPort != null) {
                        return dstStaticPort.equals(edge.link().dst()) ? 1 : -1;
                    }
                }

                return 1;
            }
        };

        ConnectPoint start = intent.getSrc();
        ConnectPoint end = intent.getDst();
        Set<Path> paths = topologyService.getPaths(topology, start.deviceId(),
                end.deviceId(), weight);

        return paths;
    }
}

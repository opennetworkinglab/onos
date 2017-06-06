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
import org.onosproject.net.resource.Resource;
import org.onosproject.net.resource.ResourceAllocation;
import org.onosproject.net.resource.ResourceService;
import org.onosproject.net.resource.Resources;
import org.onosproject.net.topology.AdapterLinkWeigher;
import org.onosproject.net.topology.LinkWeight;
import org.onosproject.net.topology.Topology;
import org.onosproject.net.topology.TopologyEdge;
import org.onosproject.net.topology.TopologyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
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
        checkArgument(deviceService.getPort(src.deviceId(), src.port()) instanceof OchPort);
        checkArgument(deviceService.getPort(dst.deviceId(), dst.port()) instanceof OchPort);
        List<Resource> resources = new LinkedList<>();

        log.debug("Compiling optical connectivity intent between {} and {}", src, dst);

        // Release of intent resources here is only a temporary solution for handling the
        // case of recompiling due to intent restoration (when intent state is FAILED).
        // TODO: try to release intent resources in IntentManager.
        resourceService.release(intent.key());

        // Check OCh port availability
        // If ports are not available, compilation fails
        // Else add port to resource reservation list
        Resource srcPortResource = Resources.discrete(src.deviceId(), src.port()).resource();
        Resource dstPortResource = Resources.discrete(dst.deviceId(), dst.port()).resource();
        if (!Stream.of(srcPortResource, dstPortResource).allMatch(resourceService::isAvailable)) {
            log.error("Ports for the intent are not available. Intent: {}", intent);
            throw new OpticalIntentCompilationException("Ports for the intent are not available. Intent: " + intent);
        }
        resources.add(srcPortResource);
        resources.add(dstPortResource);

        // Find first path that has the required resources
        Stream<Path> paths = getOpticalPaths(intent);
        Optional<Map.Entry<Path, List<OchSignal>>> found = paths
                .map(path ->
                        Maps.immutableEntry(path, findFirstAvailableLambda(path)))
                .filter(entry -> !entry.getValue().isEmpty())
                .filter(entry -> convertToResources(entry.getKey(),
                        entry.getValue()).stream().allMatch(resourceService::isAvailable))
                .findFirst();

        // Allocate resources and create optical path intent
        if (found.isPresent()) {
            resources.addAll(convertToResources(found.get().getKey(), found.get().getValue()));

            allocateResources(intent, resources);

            OchSignal ochSignal = OchSignal.toFixedGrid(found.get().getValue(), ChannelSpacing.CHL_50GHZ);
            return ImmutableList.of(createIntent(intent, found.get().getKey(), ochSignal));
        } else {
            log.error("Unable to find suitable lightpath for intent {}", intent);
            throw new OpticalIntentCompilationException("Unable to find suitable lightpath for intent " + intent);
        }
    }

    /**
     * Create installable optical path intent.
     * Only supports fixed grid for now.
     *
     * @param parentIntent this intent (used for resource tracking)
     * @param path the path to use
     * @param lambda the lambda to use
     * @return optical path intent
     */
    private Intent createIntent(OpticalConnectivityIntent parentIntent, Path path, OchSignal lambda) {
        OchSignalType signalType = OchSignalType.FIXED_GRID;

        return OpticalPathIntent.builder()
                .appId(parentIntent.appId())
                .key(parentIntent.key())
                .src(parentIntent.getSrc())
                .dst(parentIntent.getDst())
                .path(path)
                .lambda(lambda)
                .signalType(signalType)
                .bidirectional(parentIntent.isBidirectional())
                .resourceGroup(parentIntent.resourceGroup())
                .build();
    }

    /**
     * Convert given lambda as discrete resource of all path ports.
     *
     * @param path the path
     * @param lambda the lambda
     * @return list of discrete resources
     */
    private List<Resource> convertToResources(Path path, Collection<OchSignal> lambda) {
        return path.links().stream()
                .flatMap(x -> Stream.of(
                        Resources.discrete(x.src().deviceId(), x.src().port()).resource(),
                        Resources.discrete(x.dst().deviceId(), x.dst().port()).resource()
                ))
                .flatMap(x -> lambda.stream().map(x::child))
                .collect(Collectors.toList());
    }

    /**
     * Reserve all required resources for this intent.
     *
     * @param intent the intent
     * @param resources list of resources to reserve
     */
    private void allocateResources(Intent intent, List<Resource> resources) {
        List<ResourceAllocation> allocations = resourceService.allocate(intent.key(), resources);
        if (allocations.isEmpty()) {
            log.error("Resource allocation for {} failed (resource request: {})", intent.key(), resources);
            if (log.isDebugEnabled()) {
                log.debug("requested resources:\n\t{}", resources.stream()
                                  .map(Resource::toString)
                                  .collect(Collectors.joining("\n\t")));
            }
            throw new OpticalIntentCompilationException("Unable to allocate resources: " + resources);
        }
    }

    /**
     * Find the first available lambda on the given path by checking all the port resources.
     *
     * @param path the path
     * @return list of consecutive and available OChSignals
     */
    private List<OchSignal> findFirstAvailableLambda(Path path) {
        Set<OchSignal> lambdas = findCommonLambdas(path);
        if (lambdas.isEmpty()) {
            return Collections.emptyList();
        }

        return findFirstLambda(lambdas, slotCount());
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

    /**
     * Find common lambdas on all ports that compose the path.
     *
     * @param path the path
     * @return set of common lambdas
     */
    private Set<OchSignal> findCommonLambdas(Path path) {
        return path.links().stream()
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
    private Stream<Path> getOpticalPaths(OpticalConnectivityIntent intent) {
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
        Stream<Path> paths = topologyService.getKShortestPaths(topology,
                                                            start.deviceId(),
                                                            end.deviceId(),
                                                            AdapterLinkWeigher.adapt(weight));
        if (log.isDebugEnabled()) {
            return paths
                    .map(path -> {
                        // no-op map stage to add debug logging
                        log.debug("Candidate path: {}",
                                  path.links().stream()
                                      .map(lk -> lk.src() + "-" + lk.dst())
                                      .collect(Collectors.toList()));
                        return path;
                    });
        }
        return paths;
    }
}

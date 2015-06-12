/*
 * Copyright 2014-2015 Open Networking Laboratory
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

import java.util.List;
import java.util.Set;

import com.google.common.collect.ImmutableSet;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.onlab.util.Frequency;
import org.onosproject.net.AnnotationKeys;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Link;
import org.onosproject.net.OchPort;
import org.onosproject.net.OchSignal;
import org.onosproject.net.OchSignalType;
import org.onosproject.net.OmsPort;
import org.onosproject.net.Path;
import org.onosproject.net.Port;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.intent.Intent;
import org.onosproject.net.intent.IntentCompiler;
import org.onosproject.net.intent.IntentExtensionService;
import org.onosproject.net.intent.OpticalConnectivityIntent;
import org.onosproject.net.intent.OpticalPathIntent;
import org.onosproject.net.intent.impl.IntentCompilationException;
import org.onosproject.net.resource.link.DefaultLinkResourceRequest;
import org.onosproject.net.resource.device.DeviceResourceService;
import org.onosproject.net.resource.link.LambdaResource;
import org.onosproject.net.resource.link.LambdaResourceAllocation;
import org.onosproject.net.resource.link.LinkResourceAllocations;
import org.onosproject.net.resource.link.LinkResourceRequest;
import org.onosproject.net.resource.link.LinkResourceService;
import org.onosproject.net.resource.ResourceAllocation;
import org.onosproject.net.resource.ResourceType;
import org.onosproject.net.topology.LinkWeight;
import org.onosproject.net.topology.Topology;
import org.onosproject.net.topology.TopologyEdge;
import org.onosproject.net.topology.TopologyService;

import com.google.common.collect.ImmutableList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * An intent compiler for {@link org.onosproject.net.intent.OpticalConnectivityIntent}.
 */
@Component(immediate = true)
public class OpticalConnectivityIntentCompiler implements IntentCompiler<OpticalConnectivityIntent> {

    protected static final Logger log = LoggerFactory.getLogger(OpticalConnectivityIntentCompiler.class);

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected IntentExtensionService intentManager;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected TopologyService topologyService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DeviceService deviceService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected LinkResourceService linkResourceService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DeviceResourceService deviceResourceService;

    @Activate
    public void activate() {
        intentManager.registerCompiler(OpticalConnectivityIntent.class, this);
    }

    @Deactivate
    public void deactivate() {
        intentManager.unregisterCompiler(OpticalConnectivityIntent.class);
    }

    @Override
    public List<Intent> compile(OpticalConnectivityIntent intent,
                                List<Intent> installable,
                                Set<LinkResourceAllocations> resources) {
        // Check if source and destination are optical OCh ports
        ConnectPoint src = intent.getSrc();
        ConnectPoint dst = intent.getDst();
        Port srcPort = deviceService.getPort(src.deviceId(), src.port());
        Port dstPort = deviceService.getPort(dst.deviceId(), dst.port());
        checkArgument(srcPort instanceof OchPort);
        checkArgument(dstPort instanceof OchPort);

        log.debug("Compiling optical connectivity intent between {} and {}", src, dst);

        // Reserve OCh ports
        if (!deviceResourceService.requestPorts(ImmutableSet.of(srcPort, dstPort), intent)) {
            throw new IntentCompilationException("Unable to reserve ports for intent " + intent);
        }

        // Calculate available light paths
        Set<Path> paths = getOpticalPaths(intent);

        // Use first path that can be successfully reserved
        for (Path path : paths) {

            // Static or dynamic lambda allocation
            String staticLambda = srcPort.annotations().value(AnnotationKeys.STATIC_LAMBDA);
            OchPort srcOchPort = (OchPort) srcPort;
            OchPort dstOchPort = (OchPort) dstPort;
            OchSignal ochSignal;

            // FIXME: need to actually reserve the lambda for static lambda's
            if (staticLambda != null) {
                ochSignal = new OchSignal(Frequency.ofHz(Long.valueOf(staticLambda)),
                        srcOchPort.lambda().channelSpacing(),
                        srcOchPort.lambda().slotGranularity());
            } else if (!srcOchPort.isTunable() || !dstOchPort.isTunable()) {
                // FIXME: also check OCh port
                ochSignal = srcOchPort.lambda();
            } else {
                // Request and reserve lambda on path
                LinkResourceAllocations linkAllocs = assignWavelength(intent, path);
                if (linkAllocs == null) {
                    continue;
                }
                LambdaResourceAllocation lambdaAlloc = getWavelength(path, linkAllocs);
                OmsPort omsPort = (OmsPort) deviceService.getPort(path.src().deviceId(), path.src().port());
                ochSignal = new OchSignal(lambdaAlloc, omsPort.maxFrequency(), omsPort.grid());
            }

            // Create installable optical path intent
            // Only support fixed grid for now
            OchSignalType signalType = OchSignalType.FIXED_GRID;

            Intent newIntent = OpticalPathIntent.builder()
                    .appId(intent.appId())
                    .src(intent.getSrc())
                    .dst(intent.getDst())
                    .path(path)
                    .lambda(ochSignal)
                    .signalType(signalType)
                    .bidirectional(intent.isBidirectional())
                    .build();

            return ImmutableList.of(newIntent);
        }

        // Release port allocations if unsuccessful
        deviceResourceService.releasePorts(intent.id());

        throw new IntentCompilationException("Unable to find suitable lightpath for intent " + intent);
    }

    /**
     * Find the lambda allocated to the path.
     *
     * @param path the path
     * @param linkAllocs the link allocations
     * @return
     */
    private LambdaResourceAllocation getWavelength(Path path, LinkResourceAllocations linkAllocs) {
        for (Link link : path.links()) {
            for (ResourceAllocation alloc : linkAllocs.getResourceAllocation(link)) {
                if (alloc.type() == ResourceType.LAMBDA) {
                    return (LambdaResourceAllocation) alloc;
                }
            }
        }

        return null;
    }

    /**
     * Request and reserve first available wavelength across path.
     *
     * @param path path in WDM topology
     * @return first available lambda resource allocation
     */
    private LinkResourceAllocations assignWavelength(Intent intent, Path path) {
        LinkResourceRequest.Builder request =
                DefaultLinkResourceRequest.builder(intent.id(), path.links())
                .addLambdaRequest();

        LinkResourceAllocations allocations = linkResourceService.requestResources(request.build());

        if (!checkWavelengthContinuity(allocations, path)) {
            linkResourceService.releaseResources(allocations);
            return null;
        }

        return allocations;
    }

    /**
     * Checks wavelength continuity constraint across path, i.e., an identical lambda is used on all links.
     * @return true if wavelength continuity is met, false otherwise
     */
    private boolean checkWavelengthContinuity(LinkResourceAllocations allocations, Path path) {
        if (allocations == null) {
            return false;
        }

        LambdaResource lambda = null;

        for (Link link : path.links()) {
            for (ResourceAllocation alloc : allocations.getResourceAllocation(link)) {
                if (alloc.type() == ResourceType.LAMBDA) {
                    LambdaResource nextLambda = ((LambdaResourceAllocation) alloc).lambda();
                    if (nextLambda == null) {
                        return false;
                    }
                    if (lambda == null) {
                        lambda = nextLambda;
                        continue;
                    }
                    if (!lambda.equals(nextLambda)) {
                        return false;
                    }
                }
            }
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

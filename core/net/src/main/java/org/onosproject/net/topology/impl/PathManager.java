/*
 * Copyright 2014 Open Networking Laboratory
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
package org.onosproject.net.topology.impl;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DefaultEdgeLink;
import org.onosproject.net.DefaultPath;
import org.onosproject.net.DeviceId;
import org.onosproject.net.EdgeLink;
import org.onosproject.net.ElementId;
import org.onosproject.net.Host;
import org.onosproject.net.HostId;
import org.onosproject.net.HostLocation;
import org.onosproject.net.Link;
import org.onosproject.net.Path;
import org.onosproject.net.PortNumber;
import org.onosproject.net.host.HostService;
import org.onosproject.net.provider.ProviderId;
import org.onosproject.net.topology.LinkWeight;
import org.onosproject.net.topology.PathService;
import org.onosproject.net.topology.Topology;
import org.onosproject.net.topology.TopologyService;
import org.slf4j.Logger;

import java.util.List;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.slf4j.LoggerFactory.getLogger;
import static org.onosproject.security.AppGuard.checkPermission;
import static org.onosproject.security.AppPermission.Type.*;


/**
 * Provides implementation of a path selection service atop the current
 * topology and host services.
 */
@Component(immediate = true)
@Service
public class PathManager implements PathService {

    private static final String ELEMENT_ID_NULL = "Element ID cannot be null";

    private static final ProviderId PID = new ProviderId("core", "org.onosproject.core");
    private static final PortNumber P0 = PortNumber.portNumber(0);

    private static final EdgeLink NOT_HOST = new NotHost();

    private final Logger log = getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected TopologyService topologyService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected HostService hostService;

    @Activate
    public void activate() {
        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        log.info("Stopped");
    }

    @Override
    public Set<Path> getPaths(ElementId src, ElementId dst) {
        checkPermission(TOPOLOGY_READ);

        return getPaths(src, dst, null);
    }

    @Override
    public Set<Path> getPaths(ElementId src, ElementId dst, LinkWeight weight) {
        checkPermission(TOPOLOGY_READ);

        checkNotNull(src, ELEMENT_ID_NULL);
        checkNotNull(dst, ELEMENT_ID_NULL);

        // Get the source and destination edge locations
        EdgeLink srcEdge = getEdgeLink(src, true);
        EdgeLink dstEdge = getEdgeLink(dst, false);

        // If either edge is null, bail with no paths.
        if (srcEdge == null || dstEdge == null) {
            return ImmutableSet.of();
        }

        DeviceId srcDevice = srcEdge != NOT_HOST ? srcEdge.dst().deviceId() : (DeviceId) src;
        DeviceId dstDevice = dstEdge != NOT_HOST ? dstEdge.src().deviceId() : (DeviceId) dst;

        // If the source and destination are on the same edge device, there
        // is just one path, so build it and return it.
        if (srcDevice.equals(dstDevice)) {
            return edgeToEdgePaths(srcEdge, dstEdge);
        }

        // Otherwise get all paths between the source and destination edge
        // devices.
        Topology topology = topologyService.currentTopology();
        Set<Path> paths = weight == null ?
                topologyService.getPaths(topology, srcDevice, dstDevice) :
                topologyService.getPaths(topology, srcDevice, dstDevice, weight);

        return edgeToEdgePaths(srcEdge, dstEdge, paths);
    }

    // Finds the host edge link if the element ID is a host id of an existing
    // host. Otherwise, if the host does not exist, it returns null and if
    // the element ID is not a host ID, returns NOT_HOST edge link.
    private EdgeLink getEdgeLink(ElementId elementId, boolean isIngress) {
        if (elementId instanceof HostId) {
            // Resolve the host, return null.
            Host host = hostService.getHost((HostId) elementId);
            if (host == null) {
                return null;
            }
            return new DefaultEdgeLink(PID, new ConnectPoint(elementId, P0),
                                       host.location(), isIngress);
        }
        return NOT_HOST;
    }

    // Produces a set of edge-to-edge paths using the set of infrastructure
    // paths and the given edge links.
    private Set<Path> edgeToEdgePaths(EdgeLink srcLink, EdgeLink dstLink) {
        Set<Path> endToEndPaths = Sets.newHashSetWithExpectedSize(1);
        endToEndPaths.add(edgeToEdgePath(srcLink, dstLink, null));
        return endToEndPaths;
    }

    // Produces a set of edge-to-edge paths using the set of infrastructure
    // paths and the given edge links.
    private Set<Path> edgeToEdgePaths(EdgeLink srcLink, EdgeLink dstLink, Set<Path> paths) {
        Set<Path> endToEndPaths = Sets.newHashSetWithExpectedSize(paths.size());
        for (Path path : paths) {
            endToEndPaths.add(edgeToEdgePath(srcLink, dstLink, path));
        }
        return endToEndPaths;
    }

    // Produces a direct edge-to-edge path.
    private Path edgeToEdgePath(EdgeLink srcLink, EdgeLink dstLink, Path path) {
        List<Link> links = Lists.newArrayListWithCapacity(2);
        // Add source and destination edge links only if they are real and
        // add the infrastructure path only if it is not null.
        if (srcLink != NOT_HOST) {
            links.add(srcLink);
        }
        if (path != null) {
            links.addAll(path.links());
        }
        if (dstLink != NOT_HOST) {
            links.add(dstLink);
        }
        return new DefaultPath(PID, links, 2);
    }

    // Special value for edge link to represent that this is really not an
    // edge link since the src or dst are really an infrastructure device.
    private static class NotHost extends DefaultEdgeLink implements EdgeLink {
        NotHost() {
            super(PID, new ConnectPoint(HostId.NONE, P0),
                  new HostLocation(DeviceId.NONE, P0, 0L), false);
        }
    }
}

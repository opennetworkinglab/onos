package org.onlab.onos.net.trivial.path;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onlab.onos.net.ConnectPoint;
import org.onlab.onos.net.DefaultEdgeLink;
import org.onlab.onos.net.DefaultPath;
import org.onlab.onos.net.DeviceId;
import org.onlab.onos.net.EdgeLink;
import org.onlab.onos.net.ElementId;
import org.onlab.onos.net.Host;
import org.onlab.onos.net.HostId;
import org.onlab.onos.net.HostLocation;
import org.onlab.onos.net.Link;
import org.onlab.onos.net.Path;
import org.onlab.onos.net.PortNumber;
import org.onlab.onos.net.host.HostService;
import org.onlab.onos.net.path.PathService;
import org.onlab.onos.net.provider.ProviderId;
import org.onlab.onos.net.topology.LinkWeight;
import org.onlab.onos.net.topology.Topology;
import org.onlab.onos.net.topology.TopologyService;
import org.slf4j.Logger;

import java.util.List;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.onlab.onos.net.DeviceId.deviceId;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Provides implementation of a path selection service atop the current
 * topology and host services.
 */
@Component(immediate = true)
@Service
public class SimplePathManager implements PathService {

    private static final String ELEMENT_ID_NULL = "Element ID cannot be null";

    private static final ProviderId PID = new ProviderId("org.onlab.onos.core");
    private static final PortNumber P0 = PortNumber.portNumber(0);

    private static final EdgeLink NOT_HOST = new NotHost();

    private final Logger log = getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected TopologyService topologyService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected HostService hostService;

    @Activate
    public void setUp() {
        log.info("Started");
    }

    @Deactivate
    public void tearDown() {
        log.info("Stopped");
    }

    @Override
    public Set<Path> getPaths(ElementId src, ElementId dst) {
        return getPaths(src, dst, null);
    }

    @Override
    public Set<Path> getPaths(ElementId src, ElementId dst, LinkWeight weight) {
        checkNotNull(src, ELEMENT_ID_NULL);
        checkNotNull(dst, ELEMENT_ID_NULL);

        // Get the source and destination edge locations
        EdgeLink srcEdge = getEdgeLink(src, true);
        EdgeLink dstEdge = getEdgeLink(dst, false);

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

    // Produces a set of direct edge-to-edge paths.
    private Set<Path> edgeToEdgePaths(EdgeLink srcLink, EdgeLink dstLink) {
        Set<Path> endToEndPaths = Sets.newHashSetWithExpectedSize(1);
        if (srcLink != NOT_HOST || dstLink != NOT_HOST) {
            endToEndPaths.add(edgeToEdgePath(srcLink, dstLink));
        }
        return endToEndPaths;
    }

    // Produces a direct edge-to-edge path.
    private Path edgeToEdgePath(EdgeLink srcLink, EdgeLink dstLink) {
        List<Link> links = Lists.newArrayListWithCapacity(2);
        // Add source and destination edge links only if they are real.
        if (srcLink != NOT_HOST) {
            links.add(srcLink);
        }
        if (dstLink != NOT_HOST) {
            links.add(dstLink);
        }
        return new DefaultPath(PID, links, 2);
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

    // Produces an edge-to-edge path using the specified infrastructure path
    // and edge links.
    private Path edgeToEdgePath(EdgeLink srcLink, EdgeLink dstLink, Path path) {
        List<Link> links = Lists.newArrayListWithCapacity(path.links().size() + 2);
        links.add(srcLink);
        links.addAll(path.links());
        links.add(dstLink);
        return new DefaultPath(path.providerId(), links, path.cost() + 2);
    }

    // Special value for edge link to represent that this is really not an
    // edge link since the src or dst are really an infrastructure device.
    private static class NotHost extends DefaultEdgeLink implements EdgeLink {
        NotHost() {
            super(PID, new ConnectPoint(HostId.hostId("nic:none"), P0),
                  new HostLocation(deviceId("none:none"), P0, 0L), false);
        }
    }
}

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
import org.onlab.onos.net.Host;
import org.onlab.onos.net.HostId;
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
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Provides implementation of a path selection service atop the current
 * topology and host services.
 */
@Component(immediate = true)
@Service
public class PathManager implements PathService {

    private static final String DEVICE_ID_NULL = "Device ID cannot be null";
    private static final String HOST_ID_NULL = "Host ID cannot be null";

    private static final ProviderId PID = new ProviderId("org.onlab.onos.core");
    private static final PortNumber P0 = PortNumber.portNumber(0);

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
    public Set<Path> getPaths(DeviceId src, DeviceId dst) {
        checkNotNull(src, DEVICE_ID_NULL);
        checkNotNull(dst, DEVICE_ID_NULL);
        Topology topology = topologyService.currentTopology();
        return topologyService.getPaths(topology, src, dst);
    }

    @Override
    public Set<Path> getPaths(DeviceId src, DeviceId dst, LinkWeight weight) {
        checkNotNull(src, DEVICE_ID_NULL);
        checkNotNull(dst, DEVICE_ID_NULL);
        Topology topology = topologyService.currentTopology();
        return topologyService.getPaths(topology, src, dst, weight);
    }

    @Override
    public Set<Path> getPaths(HostId src, HostId dst) {
        return getPaths(src, dst, null);
    }

    @Override
    public Set<Path> getPaths(HostId src, HostId dst, LinkWeight weight) {
        checkNotNull(src, HOST_ID_NULL);
        checkNotNull(dst, HOST_ID_NULL);

        // Resolve the source host, bail if unable.
        Host srcHost = hostService.getHost(src);
        if (srcHost == null) {
            return Sets.newHashSet();
        }

        // Resolve the destination host, bail if unable.
        Host dstHost = hostService.getHost(dst);
        if (dstHost == null) {
            return Sets.newHashSet();
        }

        // Get the source and destination edge locations
        EdgeLink srcEdge = new DefaultEdgeLink(PID, new ConnectPoint(src, P0),
                                               srcHost.location(), true);
        EdgeLink dstEdge = new DefaultEdgeLink(PID, new ConnectPoint(dst, P0),
                                               dstHost.location(), false);

        // If the source and destination are on the same edge device, there
        // is just one path, so build it and return it.
        if (srcEdge.dst().deviceId().equals(dstEdge.src().deviceId())) {
            return edgeToEdgePaths(srcEdge, dstEdge);
        }

        // Otherwise get all paths between the source and destination edge
        // devices.
        Topology topology = topologyService.currentTopology();
        Set<Path> paths = weight == null ?
                topologyService.getPaths(topology, srcEdge.dst().deviceId(),
                                         dstEdge.src().deviceId()) :
                topologyService.getPaths(topology, srcEdge.dst().deviceId(),
                                         dstEdge.src().deviceId(), weight);

        return edgeToEdgePaths(srcEdge, dstEdge, paths);
    }

    // Produces a set of direct edge-to-edge paths.
    private Set<Path> edgeToEdgePaths(EdgeLink srcLink, EdgeLink dstLink) {
        Set<Path> endToEndPaths = Sets.newHashSetWithExpectedSize(1);
        endToEndPaths.add(edgeToEdgePath(srcLink, dstLink));
        return endToEndPaths;
    }

    // Produces a direct edge-to-edge path.
    private Path edgeToEdgePath(EdgeLink srcLink, EdgeLink dstLink) {
        List<Link> links = Lists.newArrayListWithCapacity(2);
        links.add(srcLink);
        links.add(dstLink);
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

}

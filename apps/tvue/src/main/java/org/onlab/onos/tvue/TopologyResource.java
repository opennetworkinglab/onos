package org.onlab.onos.tvue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.onlab.onos.net.ConnectPoint;
import org.onlab.onos.net.Device;
import org.onlab.onos.net.ElementId;
import org.onlab.onos.net.Link;
import org.onlab.onos.net.device.DeviceService;
import org.onlab.onos.net.host.HostService;
import org.onlab.onos.net.link.LinkService;
import org.onlab.onos.net.topology.Topology;
import org.onlab.onos.net.topology.TopologyGraph;
import org.onlab.onos.net.topology.TopologyService;
import org.onlab.onos.net.topology.TopologyVertex;
import org.onlab.rest.BaseResource;

import javax.ws.rs.GET;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.onlab.onos.net.DeviceId.deviceId;

/**
 * Topology viewer resource.
 */
@javax.ws.rs.Path("topology")
public class TopologyResource extends BaseResource {

    @javax.ws.rs.Path("/graph")
    @GET
    @Produces("application/json")
    public Response graph() {
        ObjectMapper mapper = new ObjectMapper();

        // Fetch the services we'll be using.
        DeviceService deviceService = get(DeviceService.class);
        HostService hostService = get(HostService.class);
        TopologyService topologyService = get(TopologyService.class);

        // Fetch the current topology and its graph that we'll use to render.
        Topology topo = topologyService.currentTopology();
        TopologyGraph graph = topologyService.getGraph(topo);

        // Build all interior vertexes, i.e. no end-station hosts yet
        ArrayNode vertexesNode = mapper.createArrayNode();
        for (TopologyVertex vertex : graph.getVertexes()) {
            vertexesNode.add(json(mapper, vertex.deviceId(), 2,
                                  deviceService.isAvailable(vertex.deviceId())));
        }

        // Now scan all links and count number of them between the same devices
        // using a normalized link key.
        Map<String, AggLink> linkRecords = aggregateLinks();

        // Now build all interior edges using the aggregated links.
        ArrayNode edgesNode = mapper.createArrayNode();
        for (AggLink lr : linkRecords.values()) {
            edgesNode.add(json(mapper, lr.links.size(), lr.link.src(), lr.link.dst()));
        }

        // Merge the exterior and interior vertexes and inject host links as
        // the exterior edges.
//        Iterator<Host> hosts = hostService.getHosts();
//        while (hosts.hasNext()) {
//            Host host = hosts.next();
//            vertexesNode.add(json(mapper, host.id().ip().toString(), 3, true));
//            edgesNode.add(json(mapper, 1, host.ip().toString(),
//                               host.location().elementId().uri()));
//        }

        // Now put the vertexes and edges into a root node and ship them off
        ObjectNode rootNode = mapper.createObjectNode();
        rootNode.put("vertexes", vertexesNode);
        rootNode.put("edges", edgesNode);
        return Response.ok(rootNode.toString()).build();
    }



    /**
     * Returns a JSON array of all paths between the specified hosts.
     *
     * @param src source host id
     * @param dst target host id
     * @return JSON array of paths
     */
    @javax.ws.rs.Path("/paths/{src}/{dst}")
    @GET
    @Produces("application/json")
    public Response paths(@PathParam("src") String src, @PathParam("dst") String dst) {
        ObjectMapper mapper = new ObjectMapper();

        DeviceService deviceService = get(DeviceService.class);
        TopologyService topologyService = get(TopologyService.class);
        Topology topology  = topologyService.currentTopology();

        ArrayNode pathsNode = mapper.createArrayNode();
        Device srcDevice = deviceService.getDevice(deviceId(src));
        Device dstDevice = deviceService.getDevice(deviceId(dst));

//        if (srcDevice != null && dstDevice != null) {
//            for (Path path : topologyService.getPaths(topology, srcDevice, dstDevice))
//                pathsNode.add(json(mapper, path));
//        }

        // Now put the vertexes and edges into a root node and ship them off
        ObjectNode rootNode = mapper.createObjectNode();
        rootNode.put("paths", pathsNode);
        return Response.ok(rootNode.toString()).build();
    }

    // Scan all links and counts number of them between the same devices
    // using a normalized link key.
    private Map<String, AggLink> aggregateLinks() {
        Map<String, AggLink> aggLinks = new HashMap<>();
        LinkService linkService = get(LinkService.class);
        for (Link link : linkService.getLinks()) {
            String key = key(link);
            AggLink lr = aggLinks.get(key);
            if (lr == null) {
                lr = new AggLink(key);
                aggLinks.put(key, lr);
            }
            lr.addLink(link);
        }
        return aggLinks;
    }





    // Produces JSON for a graph vertex.
    private ObjectNode json(ObjectMapper mapper, ElementId id, int group,
                            boolean isOnline) {
        return mapper.createObjectNode()
                .put("name", id.uri().getSchemeSpecificPart())
                .put("group", group)
                .put("online", isOnline);
    }

    // Produces JSON for a graph edge.
    private ObjectNode json(ObjectMapper mapper, int count,
                            ConnectPoint src, ConnectPoint dst) {
        return json(mapper, count, id(src), id(dst));
    }

    // Produces JSON for a graph edge.
    private ObjectNode json(ObjectMapper mapper, int count, String src, String dst) {
        return mapper.createObjectNode()
                .put("source", src).put("target", dst).put("value", count);
    }

    // Aggregate link of all links between the same devices regardless of
    // their direction.
    private class AggLink {
        Link link; // representative links

        final String key;
        final Set<Link> links = new HashSet<>();

        AggLink(String key) {
            this.key = key;
        }

        void addLink(Link link) {
            links.add(link);
            if (this.link == null) {
                this.link = link;
            }
        }
    }

    // Returns a canonical key for the specified link.
    static String key(Link link) {
        String s = id(link.src());
        String d = id(link.dst());
        return s.compareTo(d) > 0 ? d + s : s + d;
    }

    // Returns a formatted string for the element associated with the given
    // connection point.
    private static String id(ConnectPoint cp) {
        return cp.elementId().uri().getSchemeSpecificPart();
    }

}

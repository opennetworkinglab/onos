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
package org.onosproject.tvue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.ElementId;
import org.onosproject.net.Host;
import org.onosproject.net.Link;
import org.onosproject.net.Path;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.host.HostService;
import org.onosproject.net.link.LinkService;
import org.onosproject.net.topology.PathService;
import org.onosproject.net.topology.Topology;
import org.onosproject.net.topology.TopologyGraph;
import org.onosproject.net.topology.TopologyService;
import org.onosproject.net.topology.TopologyVertex;
import org.onlab.packet.IpAddress;
import org.onlab.rest.BaseResource;

import javax.ws.rs.GET;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.onosproject.net.DeviceId.deviceId;
import static org.onosproject.net.HostId.hostId;
import static org.onosproject.net.PortNumber.portNumber;

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
                                  vertex.deviceId().uri().getSchemeSpecificPart(),
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
        for (Host host : hostService.getHosts()) {
            Set<IpAddress> ipAddresses = host.ipAddresses();
            IpAddress ipAddress = ipAddresses.isEmpty() ? null : ipAddresses.iterator().next();
            String label = ipAddress != null ? ipAddress.toString() : host.mac().toString();
            vertexesNode.add(json(mapper, host.id(), 3, label, true));
            edgesNode.add(json(mapper, 1, host.location(), new ConnectPoint(host.id(), portNumber(-1))));
        }

        // Now put the vertexes and edges into a root node and ship them off
        ObjectNode rootNode = mapper.createObjectNode();
        rootNode.set("vertexes", vertexesNode);
        rootNode.set("edges", edgesNode);
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
        PathService pathService = get(PathService.class);
        Set<Path> paths = pathService.getPaths(elementId(src), elementId(dst));

        ArrayNode pathsNode = mapper.createArrayNode();
        for (Path path : paths) {
            pathsNode.add(json(mapper, path));
        }

        // Now put the vertexes and edges into a root node and ship them off
        ObjectNode rootNode = mapper.createObjectNode();
        rootNode.set("paths", pathsNode);
        return Response.ok(rootNode.toString()).build();
    }

    // Creates either device ID or host ID as appropriate.
    private ElementId elementId(String id) {
        return id.startsWith("nic:") ? hostId(id) : deviceId(id);
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
                            String label, boolean isOnline) {
        return mapper.createObjectNode()
                .put("name", id.toString())
                .put("label", label)
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

    // Produces JSON representation of a network path.
    private ArrayNode json(ObjectMapper mapper, Path path) {
        ArrayNode pathNode = mapper.createArrayNode();
        for (Link link : path.links()) {
            ObjectNode linkNode = mapper.createObjectNode()
                    .put("src", id(link.src()))
                    .put("dst", id(link.dst()));
            pathNode.add(linkNode);
        }
        return pathNode;
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
        return cp.elementId().toString();
    }

}

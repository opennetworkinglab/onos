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
package org.onlab.onos.gui;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.onlab.onos.net.ConnectPoint;
import org.onlab.onos.net.Device;
import org.onlab.onos.net.Host;
import org.onlab.onos.net.HostLocation;
import org.onlab.onos.net.Link;
import org.onlab.onos.net.device.DeviceService;
import org.onlab.onos.net.host.HostService;
import org.onlab.onos.net.link.LinkService;
import org.onlab.onos.net.topology.Topology;
import org.onlab.onos.net.topology.TopologyGraph;
import org.onlab.onos.net.topology.TopologyService;
import org.onlab.onos.net.topology.TopologyVertex;
import org.onlab.packet.IpAddress;
import org.onlab.packet.MacAddress;
import org.onlab.rest.BaseResource;

import javax.ws.rs.GET;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * Topology viewer resource.
 */
@javax.ws.rs.Path("topology")
public class TopologyResource extends BaseResource {

    @javax.ws.rs.Path("/graph")
    @GET
    @Produces("application/json")
    public Response graph() {
        // Fetch the services we'll be using.
        DeviceService deviceService = get(DeviceService.class);
        HostService hostService = get(HostService.class);
        TopologyService topologyService = get(TopologyService.class);

        // Fetch the current topology and its graph that we'll use to render.
        Topology topo = topologyService.currentTopology();
        TopologyGraph graph = topologyService.getGraph(topo);

        ObjectMapper mapper = new ObjectMapper();
        ObjectNode rootNode = mapper.createObjectNode();
        rootNode.set("devices", getDevices(mapper, deviceService, graph));
        rootNode.set("links", getLinks(mapper, topo, graph));
        rootNode.set("hosts", getHosts(mapper, hostService));
        return Response.ok(rootNode.toString()).build();
    }

    // Encodes all infrastructure devices.
    private ArrayNode getDevices(ObjectMapper mapper, DeviceService deviceService,
                                 TopologyGraph graph) {
        ArrayNode devices = mapper.createArrayNode();
        for (TopologyVertex vertex : graph.getVertexes()) {
            devices.add(json(mapper, deviceService.getDevice(vertex.deviceId()),
                             deviceService.isAvailable(vertex.deviceId())));
        }
        return devices;
    }

    // Encodes all infrastructure links.
    private ArrayNode getLinks(ObjectMapper mapper, Topology topo, TopologyGraph graph) {
        // Now scan all links and count number of them between the same devices
        // using a normalized link key.
        Map<String, AggLink> linkRecords = aggregateLinks();

        // Now build all interior edges using the aggregated links.
        ArrayNode links = mapper.createArrayNode();
        for (AggLink lr : linkRecords.values()) {
            links.add(json(mapper, lr));
        }
        return links;
    }

    // Encodes all end-station hosts.
    private ArrayNode getHosts(ObjectMapper mapper, HostService hostService) {
        ArrayNode hosts = mapper.createArrayNode();
        for (Host host : hostService.getHosts()) {
            Set<IpAddress> ipAddresses = host.ipAddresses();
            IpAddress ipAddress = ipAddresses.isEmpty() ? null : ipAddresses.iterator().next();
            String label = ipAddress != null ? ipAddress.toString() : host.mac().toString();
            hosts.add(json(mapper, host));
        }
        return hosts;
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

    // Produces JSON for a device.
    private ObjectNode json(ObjectMapper mapper, Device device, boolean isOnline) {
        ObjectNode node = mapper.createObjectNode()
                .put("id", device.id().toString())
                .put("type", device.type().toString().toLowerCase())
                .put("online", isOnline);
        node.set("labels", labels(mapper,
                                  device.id().uri().getSchemeSpecificPart(),
                                  MacAddress.valueOf(device.chassisId().value()).toString(),
                                  device.serialNumber()));
        return node;
    }

    // Produces JSON for a link.
    private ObjectNode json(ObjectMapper mapper, AggLink aggLink) {
        Link link = aggLink.link;
        return mapper.createObjectNode()
                .put("src", link.src().deviceId().toString())
                .put("dst", link.dst().deviceId().toString())
                .put("type", link.type().toString().toLowerCase())
                .put("linkWidth", aggLink.links.size());
    }

    // Produces JSON for a device.
    private ObjectNode json(ObjectMapper mapper, Host host) {
        ObjectNode json = mapper.createObjectNode()
                .put("id", host.id().toString());
        json.set("cp", location(mapper, host.location()));
        json.set("labels", labels(mapper, ip(host.ipAddresses()),
                                  host.mac().toString()));
        return json;
    }

    private String ip(Set<IpAddress> ipAddresses) {
        Iterator<IpAddress> it = ipAddresses.iterator();
        return it.hasNext() ? it.next().toString() : "unknown";
    }

    private ObjectNode location(ObjectMapper mapper, HostLocation location) {
        return mapper.createObjectNode()
                .put("device", location.deviceId().toString())
                .put("port", location.port().toLong());
    }

    private ArrayNode labels(ObjectMapper mapper, String... labels) {
        ArrayNode json = mapper.createArrayNode();
        for (String label : labels) {
            json.add(label);
        }
        return json;
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

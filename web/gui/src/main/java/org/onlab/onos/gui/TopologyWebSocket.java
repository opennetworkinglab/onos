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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.eclipse.jetty.websocket.WebSocket;
import org.onlab.onos.event.Event;
import org.onlab.onos.net.Annotations;
import org.onlab.onos.net.Device;
import org.onlab.onos.net.DeviceId;
import org.onlab.onos.net.Link;
import org.onlab.onos.net.Path;
import org.onlab.onos.net.device.DeviceEvent;
import org.onlab.onos.net.device.DeviceService;
import org.onlab.onos.net.link.LinkEvent;
import org.onlab.onos.net.topology.Topology;
import org.onlab.onos.net.topology.TopologyEdge;
import org.onlab.onos.net.topology.TopologyEvent;
import org.onlab.onos.net.topology.TopologyGraph;
import org.onlab.onos.net.topology.TopologyListener;
import org.onlab.onos.net.topology.TopologyService;
import org.onlab.onos.net.topology.TopologyVertex;
import org.onlab.osgi.ServiceDirectory;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.onlab.onos.net.DeviceId.deviceId;
import static org.onlab.onos.net.device.DeviceEvent.Type.DEVICE_ADDED;
import static org.onlab.onos.net.device.DeviceEvent.Type.DEVICE_REMOVED;
import static org.onlab.onos.net.link.LinkEvent.Type.LINK_ADDED;
import static org.onlab.onos.net.link.LinkEvent.Type.LINK_REMOVED;

/**
 * Web socket capable of interacting with the GUI topology view.
 */
public class TopologyWebSocket implements WebSocket.OnTextMessage, TopologyListener {

    private final ServiceDirectory directory;
    private final TopologyService topologyService;
    private final DeviceService deviceService;

    private final ObjectMapper mapper = new ObjectMapper();

    private Connection connection;

    // TODO: extract into an external & durable state; good enough for now and demo
    private static Map<String, ObjectNode> metaUi = new HashMap<>();

    private static final String COMPACT = "%s/%s-%s/%s";


    /**
     * Creates a new web-socket for serving data to GUI topology view.
     *
     * @param directory service directory
     */
    public TopologyWebSocket(ServiceDirectory directory) {
        this.directory = directory;
        topologyService = directory.get(TopologyService.class);
        deviceService = directory.get(DeviceService.class);
    }

    @Override
    public void onOpen(Connection connection) {
        this.connection = connection;

        // Register for topology events...
        if (topologyService != null && deviceService != null) {
            topologyService.addListener(this);

            Topology topology = topologyService.currentTopology();
            TopologyGraph graph = topologyService.getGraph(topology);
            for (TopologyVertex vertex : graph.getVertexes()) {
                sendMessage(message(new DeviceEvent(DEVICE_ADDED,
                                                    deviceService.getDevice(vertex.deviceId()))));
            }

            for (TopologyEdge edge : graph.getEdges()) {
                sendMessage(message(new LinkEvent(LINK_ADDED, edge.link())));
            }

        } else {
            sendMessage(message("error", "No topology service!!!"));
        }
    }

    @Override
    public void onClose(int closeCode, String message) {
        TopologyService topologyService = directory.get(TopologyService.class);
        if (topologyService != null) {
            topologyService.removeListener(this);
        }
    }

    @Override
    public void onMessage(String data) {
        try {
            ObjectNode event = (ObjectNode) mapper.reader().readTree(data);
            String type = event.path("event").asText("unknown");
            ObjectNode payload = (ObjectNode) event.path("payload");

            switch (type) {
                case "updateMeta":
                    metaUi.put(payload.path("id").asText(), payload);
                    break;
                case "requestPath":
                    findPath(deviceId(payload.path("one").asText()),
                             deviceId(payload.path("two").asText()));
                default:
                    break;
            }
        } catch (IOException e) {
            System.out.println("Received: " + data);
        }
    }

    private void findPath(DeviceId one, DeviceId two) {
        Set<Path> paths = topologyService.getPaths(topologyService.currentTopology(),
                                                   one, two);
        if (!paths.isEmpty()) {
            ObjectNode payload = mapper.createObjectNode();
            ArrayNode links = mapper.createArrayNode();

            Path path = paths.iterator().next();
            for (Link link : path.links()) {
                links.add(compactLinkString(link));
            }

            payload.set("links", links);
            sendMessage(envelope("showPath", payload));
        }
        // TODO: when no path, send a message to the client
    }

    /**
     * Returns a compact string representing the given link.
     *
     * @param link infrastructure link
     * @return formatted link string
     */
    public static String compactLinkString(Link link) {
        return String.format(COMPACT, link.src().deviceId(), link.src().port(),
                             link.dst().deviceId(), link.dst().port());
    }


    private void sendMessage(String data) {
        try {
            connection.sendMessage(data);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Produces a link event message to the client.
    private String message(DeviceEvent event) {
        Device device = event.subject();
        ObjectNode payload = mapper.createObjectNode()
                .put("id", device.id().toString())
                .put("type", device.type().toString().toLowerCase())
                .put("online", deviceService.isAvailable(device.id()));

        // Generate labels: id, chassis id, no-label, optional-name
        ArrayNode labels = mapper.createArrayNode();
        labels.add(device.id().toString());
        labels.add(device.chassisId().toString());
        labels.add(" "); // compact no-label view
        labels.add(device.annotations().value("name"));

        // Add labels, props and stuff the payload into envelope.
        payload.set("labels", labels);
        payload.set("props", props(device.annotations()));

        ObjectNode meta = metaUi.get(device.id().toString());
        if (meta != null) {
            payload.set("metaUi", meta);
        }

        String type = (event.type() == DEVICE_ADDED) ? "addDevice" :
                ((event.type() == DEVICE_REMOVED) ? "removeDevice" : "updateDevice");
        return envelope(type, payload);
    }

    // Produces a link event message to the client.
    private String message(LinkEvent event) {
        Link link = event.subject();
        ObjectNode payload = mapper.createObjectNode()
                .put("type", link.type().toString().toLowerCase())
                .put("linkWidth", 2)
                .put("src", link.src().deviceId().toString())
                .put("srcPort", link.src().port().toString())
                .put("dst", link.dst().deviceId().toString())
                .put("dstPort", link.dst().port().toString());
        String type = (event.type() == LINK_ADDED) ? "addLink" :
                ((event.type() == LINK_REMOVED) ? "removeLink" : "removeLink");
        return envelope(type, payload);
    }

    // Produces JSON structure from annotations.
    private JsonNode props(Annotations annotations) {
        ObjectNode props = mapper.createObjectNode();
        for (String key : annotations.keys()) {
            props.put(key, annotations.value(key));
        }
        return props;
    }

    // Produces a log message event bound to the client.
    private String message(String severity, String message) {
        return envelope("message",
                        mapper.createObjectNode()
                                .put("severity", severity)
                                .put("message", message));
    }

    // Puts the payload into an envelope and returns it.
    private String envelope(String type, ObjectNode payload) {
        ObjectNode event = mapper.createObjectNode();
        event.put("event", type);
        event.set("payload", payload);
        return event.toString();
    }

    @Override
    public void event(TopologyEvent event) {
        for (Event reason : event.reasons()) {
            if (reason instanceof DeviceEvent) {
                sendMessage(message((DeviceEvent) reason));
            } else if (reason instanceof LinkEvent) {
                sendMessage(message((LinkEvent) reason));
            }
        }
    }
}


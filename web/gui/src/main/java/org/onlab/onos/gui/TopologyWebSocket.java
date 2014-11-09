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
import org.onlab.onos.mastership.MastershipEvent;
import org.onlab.onos.mastership.MastershipListener;
import org.onlab.onos.mastership.MastershipService;
import org.onlab.onos.net.Annotations;
import org.onlab.onos.net.Device;
import org.onlab.onos.net.DeviceId;
import org.onlab.onos.net.Host;
import org.onlab.onos.net.HostId;
import org.onlab.onos.net.HostLocation;
import org.onlab.onos.net.Link;
import org.onlab.onos.net.Path;
import org.onlab.onos.net.device.DeviceEvent;
import org.onlab.onos.net.device.DeviceListener;
import org.onlab.onos.net.device.DeviceService;
import org.onlab.onos.net.host.HostEvent;
import org.onlab.onos.net.host.HostListener;
import org.onlab.onos.net.host.HostService;
import org.onlab.onos.net.intent.IntentId;
import org.onlab.onos.net.link.LinkEvent;
import org.onlab.onos.net.link.LinkListener;
import org.onlab.onos.net.link.LinkService;
import org.onlab.onos.net.topology.PathService;
import org.onlab.osgi.ServiceDirectory;
import org.onlab.packet.IpAddress;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.onlab.onos.net.DeviceId.deviceId;
import static org.onlab.onos.net.HostId.hostId;
import static org.onlab.onos.net.device.DeviceEvent.Type.DEVICE_ADDED;
import static org.onlab.onos.net.device.DeviceEvent.Type.DEVICE_REMOVED;
import static org.onlab.onos.net.link.LinkEvent.Type.LINK_ADDED;
import static org.onlab.onos.net.link.LinkEvent.Type.LINK_REMOVED;

/**
 * Web socket capable of interacting with the GUI topology view.
 */
public class TopologyWebSocket implements WebSocket.OnTextMessage {

    private final ServiceDirectory directory;

    private final ObjectMapper mapper = new ObjectMapper();

    private Connection connection;

    private final DeviceService deviceService;
    private final LinkService linkService;
    private final HostService hostService;
    private final MastershipService mastershipService;

    private final DeviceListener deviceListener = new InternalDeviceListener();
    private final LinkListener linkListener = new InternalLinkListener();
    private final HostListener hostListener = new InternalHostListener();
    private final MastershipListener mastershipListener = new InternalMastershipListener();

    // TODO: extract into an external & durable state; good enough for now and demo
    private static Map<String, ObjectNode> metaUi = new HashMap<>();

    private static final String COMPACT = "%s/%s-%s/%s";


    /**
     * Creates a new web-socket for serving data to GUI topology view.
     *
     * @param directory service directory
     */
    public TopologyWebSocket(ServiceDirectory directory) {
        this.directory = checkNotNull(directory, "Directory cannot be null");
        deviceService = directory.get(DeviceService.class);
        linkService = directory.get(LinkService.class);
        hostService = directory.get(HostService.class);
        mastershipService = directory.get(MastershipService.class);
    }

    @Override
    public void onOpen(Connection connection) {
        this.connection = connection;
        deviceService.addListener(deviceListener);
        linkService.addListener(linkListener);
        hostService.addListener(hostListener);
        mastershipService.addListener(mastershipListener);

        sendAllDevices();
        sendAllLinks();
    }

    private void sendAllDevices() {
        for (Device device : deviceService.getDevices()) {
            sendMessage(deviceMessage(new DeviceEvent(DEVICE_ADDED, device)));
        }
    }

    private void sendAllLinks() {
        for (Link link : linkService.getLinks()) {
            sendMessage(linkMessage(new LinkEvent(LINK_ADDED, link)));
        }
    }

    @Override
    public void onClose(int closeCode, String message) {
        deviceService.removeListener(deviceListener);
        linkService.removeListener(linkListener);
        hostService.removeListener(hostListener);
        mastershipService.removeListener(mastershipListener);
    }

    @Override
    public void onMessage(String data) {
        try {
            ObjectNode event = (ObjectNode) mapper.reader().readTree(data);
            String type = string(event, "event", "unknown");
            if (type.equals("showDetails")) {
                showDetails(event);
            } else if (type.equals("updateMeta")) {
                updateMetaInformation(event);
            } else if (type.equals("requestPath")) {
                sendPath(event);
            } else if (type.equals("requestTraffic")) {
                sendTraffic(event);
            } else if (type.equals("cancelTraffic")) {
                cancelTraffic(event);
            }
        } catch (IOException e) {
            System.out.println("Received: " + data);
        }
    }

    // Sends the specified data to the client.
    private void sendMessage(ObjectNode data) {
        try {
            connection.sendMessage(data.toString());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Retrieves the payload from the specified event.
    private ObjectNode payload(ObjectNode event) {
        return (ObjectNode) event.path("payload");
    }

    // Returns the specified node property as a number
    private long number(ObjectNode node, String name) {
        return node.path(name).asLong();
    }

    // Returns the specified node property as a string.
    private String string(ObjectNode node, String name) {
        return node.path(name).asText();
    }

    // Returns the specified node property as a string.
    private String string(ObjectNode node, String name, String defaultValue) {
        return node.path(name).asText(defaultValue);
    }

    // Returns the specified set of IP addresses as a string.
    private String ip(Set<IpAddress> ipAddresses) {
        Iterator<IpAddress> it = ipAddresses.iterator();
        return it.hasNext() ? it.next().toString() : "unknown";
    }

    // Encodes the specified host location into a JSON object.
    private ObjectNode location(ObjectMapper mapper, HostLocation location) {
        return mapper.createObjectNode()
                .put("device", location.deviceId().toString())
                .put("port", location.port().toLong());
    }

    // Encodes the specified list of labels a JSON array.
    private ArrayNode labels(ObjectMapper mapper, String... labels) {
        ArrayNode json = mapper.createArrayNode();
        for (String label : labels) {
            json.add(label);
        }
        return json;
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
    private ObjectNode message(String severity, long id, String message) {
        return envelope("message", id,
                        mapper.createObjectNode()
                                .put("severity", severity)
                                .put("message", message));
    }

    // Puts the payload into an envelope and returns it.
    private ObjectNode envelope(String type, long sid, ObjectNode payload) {
        ObjectNode event = mapper.createObjectNode();
        event.put("event", type);
        if (sid > 0) {
            event.put("sid", sid);
        }
        event.set("payload", payload);
        return event;
    }

    // Sends back device or host details.
    private void showDetails(ObjectNode event) {
        ObjectNode payload = payload(event);
        String type = string(payload, "type", "unknown");
        if (type.equals("device")) {
            sendMessage(deviceDetails(deviceId(string(payload, "id")),
                                      number(event, "sid")));
        } else if (type.equals("host")) {
            sendMessage(hostDetails(hostId(string(payload, "id")),
                                    number(event, "sid")));
        }
    }

    // Updates device/host meta information.
    private void updateMetaInformation(ObjectNode event) {
        ObjectNode payload = payload(event);
        metaUi.put(string(payload, "id"), payload);
    }

    // Sends path message.
    private void sendPath(ObjectNode event) {
        ObjectNode payload = payload(event);
        long id = number(event, "sid");
        DeviceId one = deviceId(string(payload, "one"));
        DeviceId two = deviceId(string(payload, "two"));

        ObjectNode response = findPath(one, two);
        if (response != null) {
            sendMessage(envelope("showPath", id, response));
        } else {
            sendMessage(message("warn", id, "No path found"));
        }
    }

    // Sends traffic message.
    private void sendTraffic(ObjectNode event) {
        ObjectNode payload = payload(event);
        long id = number(event, "sid");
        IntentId intentId = IntentId.valueOf(payload.path("intentId").asLong());

        if (payload != null) {
            payload.put("traffic", true);
            sendMessage(envelope("showPath", id, payload));
        } else {
            sendMessage(message("warn", id, "No path found"));
        }
    }

    // Cancels sending traffic messages.
    private void cancelTraffic(ObjectNode event) {
        // TODO: implement this
    }

    // Finds the path between the specified devices.
    private ObjectNode findPath(DeviceId one, DeviceId two) {
        PathService pathService = directory.get(PathService.class);
        Set<Path> paths = pathService.getPaths(one, two);
        if (paths.isEmpty()) {
            return null;
        } else {
            return pathMessage(paths.iterator().next());
        }
    }

    // Produces a path message to the client.
    private ObjectNode pathMessage(Path path) {
        ObjectNode payload = mapper.createObjectNode();
        ArrayNode links = mapper.createArrayNode();
        for (Link link : path.links()) {
            links.add(compactLinkString(link));
        }

        payload.set("links", links);
        return payload;
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


    // Produces a link event message to the client.
    private ObjectNode deviceMessage(DeviceEvent event) {
        Device device = event.subject();
        ObjectNode payload = mapper.createObjectNode()
                .put("id", device.id().toString())
                .put("type", device.type().toString().toLowerCase())
                .put("online", deviceService.isAvailable(device.id()));

        // Generate labels: id, chassis id, no-label, optional-name
        ArrayNode labels = mapper.createArrayNode();
        labels.add(device.id().toString());
        labels.add(device.chassisId().toString());
        labels.add(""); // compact no-label view
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
        return envelope(type, 0, payload);
    }

    // Produces a link event message to the client.
    private ObjectNode linkMessage(LinkEvent event) {
        Link link = event.subject();
        ObjectNode payload = mapper.createObjectNode()
                .put("id", compactLinkString(link))
                .put("type", link.type().toString().toLowerCase())
                .put("linkWidth", 2)
                .put("src", link.src().deviceId().toString())
                .put("srcPort", link.src().port().toString())
                .put("dst", link.dst().deviceId().toString())
                .put("dstPort", link.dst().port().toString());
        String type = (event.type() == LINK_ADDED) ? "addLink" :
                ((event.type() == LINK_REMOVED) ? "removeLink" : "updateLink");
        return envelope(type, 0, payload);
    }

    // Produces a host event message to the client.
    private ObjectNode hostMessage(HostEvent event) {
        Host host = event.subject();
        ObjectNode payload = mapper.createObjectNode()
                .put("id", host.id().toString());
        payload.set("cp", location(mapper, host.location()));
        payload.set("labels", labels(mapper, ip(host.ipAddresses()),
                                     host.mac().toString()));
        return payload;
    }


    // Returns device details response.
    private ObjectNode deviceDetails(DeviceId deviceId, long sid) {
        Device device = deviceService.getDevice(deviceId);
        Annotations annot = device.annotations();
        int portCount = deviceService.getPorts(deviceId).size();
        return envelope("showDetails", sid,
                        json(deviceId.toString(),
                             device.type().toString().toLowerCase(),
                             new Prop("Name", annot.value("name")),
                             new Prop("Vendor", device.manufacturer()),
                             new Prop("H/W Version", device.hwVersion()),
                             new Prop("S/W Version", device.swVersion()),
                             new Prop("Serial Number", device.serialNumber()),
                             new Separator(),
                             new Prop("Latitude", annot.value("latitude")),
                             new Prop("Longitude", annot.value("longitude")),
                             new Prop("Ports", Integer.toString(portCount))));
    }

    // Returns host details response.
    private ObjectNode hostDetails(HostId hostId, long sid) {
        Host host = hostService.getHost(hostId);
        Annotations annot = host.annotations();
        return envelope("showDetails", sid,
                        json(hostId.toString(), "host",
                             new Prop("MAC", host.mac().toString()),
                             new Prop("IP", host.ipAddresses().toString()),
                             new Separator(),
                             new Prop("Latitude", annot.value("latitude")),
                             new Prop("Longitude", annot.value("longitude"))));
    }

    // Produces JSON property details.
    private ObjectNode json(String id, String type, Prop... props) {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode result = mapper.createObjectNode()
                .put("id", id).put("type", type);
        ObjectNode pnode = mapper.createObjectNode();
        ArrayNode porder = mapper.createArrayNode();
        for (Prop p : props) {
            porder.add(p.key);
            pnode.put(p.key, p.value);
        }
        result.set("propOrder", porder);
        result.set("props", pnode);
        return result;
    }

    // Auxiliary key/value carrier.
    private class Prop {
        private final String key;
        private final String value;

        protected Prop(String key, String value) {
            this.key = key;
            this.value = value;
        }
    }

    private class Separator extends Prop {
        protected Separator() {
            super("-", "");
        }
    }

    private class InternalDeviceListener implements DeviceListener {
        @Override
        public void event(DeviceEvent event) {
            sendMessage(deviceMessage(event));
        }
    }

    private class InternalLinkListener implements LinkListener {
        @Override
        public void event(LinkEvent event) {
            sendMessage(linkMessage(event));
        }
    }

    private class InternalHostListener implements HostListener {
        @Override
        public void event(HostEvent event) {
            sendMessage(hostMessage(event));
        }
    }

    private class InternalMastershipListener implements MastershipListener {
        @Override
        public void event(MastershipEvent event) {

        }
    }
}


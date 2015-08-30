/*
 * Copyright 2015 Open Networking Laboratory
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
package org.onosproject.ui.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.onlab.osgi.ServiceDirectory;
import org.onlab.packet.IpAddress;
import org.onosproject.cluster.ClusterEvent;
import org.onosproject.cluster.ClusterService;
import org.onosproject.cluster.ControllerNode;
import org.onosproject.cluster.NodeId;
import org.onosproject.core.CoreService;
import org.onosproject.incubator.net.PortStatisticsService;
import org.onosproject.incubator.net.tunnel.OpticalTunnelEndPoint;
import org.onosproject.incubator.net.tunnel.Tunnel;
import org.onosproject.incubator.net.tunnel.TunnelService;
import org.onosproject.mastership.MastershipService;
import org.onosproject.net.Annotated;
import org.onosproject.net.AnnotationKeys;
import org.onosproject.net.Annotations;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DefaultEdgeLink;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.EdgeLink;
import org.onosproject.net.Host;
import org.onosproject.net.HostId;
import org.onosproject.net.HostLocation;
import org.onosproject.net.Link;
import org.onosproject.net.PortNumber;
import org.onosproject.net.device.DeviceEvent;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.flow.FlowEntry;
import org.onosproject.net.flow.FlowRuleService;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.flow.instructions.Instruction;
import org.onosproject.net.flow.instructions.Instructions.OutputInstruction;
import org.onosproject.net.host.HostEvent;
import org.onosproject.net.host.HostService;
import org.onosproject.net.intent.IntentService;
import org.onosproject.net.link.LinkEvent;
import org.onosproject.net.link.LinkService;
import org.onosproject.net.provider.ProviderId;
import org.onosproject.net.statistic.StatisticService;
import org.onosproject.net.topology.Topology;
import org.onosproject.net.topology.TopologyService;
import org.onosproject.ui.JsonUtils;
import org.onosproject.ui.UiConnection;
import org.onosproject.ui.UiMessageHandler;
import org.onosproject.ui.impl.topo.ServicesBundle;
import org.onosproject.ui.topo.PropertyPanel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Strings.isNullOrEmpty;
import static org.onosproject.cluster.ClusterEvent.Type.INSTANCE_ADDED;
import static org.onosproject.cluster.ClusterEvent.Type.INSTANCE_REMOVED;
import static org.onosproject.cluster.ControllerNode.State.ACTIVE;
import static org.onosproject.net.DefaultEdgeLink.createEdgeLink;
import static org.onosproject.net.PortNumber.portNumber;
import static org.onosproject.net.device.DeviceEvent.Type.DEVICE_ADDED;
import static org.onosproject.net.device.DeviceEvent.Type.DEVICE_REMOVED;
import static org.onosproject.net.host.HostEvent.Type.HOST_ADDED;
import static org.onosproject.net.host.HostEvent.Type.HOST_REMOVED;
import static org.onosproject.net.link.LinkEvent.Type.LINK_ADDED;
import static org.onosproject.net.link.LinkEvent.Type.LINK_REMOVED;
import static org.onosproject.ui.topo.TopoConstants.CoreButtons;
import static org.onosproject.ui.topo.TopoConstants.Properties;
import static org.onosproject.ui.topo.TopoUtils.compactLinkString;

/**
 * Facility for creating messages bound for the topology viewer.
 */
public abstract class TopologyViewMessageHandlerBase extends UiMessageHandler {

    protected static final Logger log =
            LoggerFactory.getLogger(TopologyViewMessageHandlerBase.class);

    private static final ProviderId PID =
            new ProviderId("core", "org.onosproject.core", true);

    protected static final String SHOW_HIGHLIGHTS = "showHighlights";

    protected ServiceDirectory directory;
    protected ClusterService clusterService;
    protected DeviceService deviceService;
    protected LinkService linkService;
    protected HostService hostService;
    protected MastershipService mastershipService;
    protected IntentService intentService;
    protected FlowRuleService flowService;
    protected StatisticService flowStatsService;
    protected PortStatisticsService portStatsService;
    protected TopologyService topologyService;
    protected TunnelService tunnelService;

    protected ServicesBundle servicesBundle;

    private String version;

    // TODO: extract into an external & durable state; good enough for now and demo
    private static Map<String, ObjectNode> metaUi = new ConcurrentHashMap<>();

    /**
     * Returns read-only view of the meta-ui information.
     *
     * @return map of id to meta-ui mementos
     */
    static Map<String, ObjectNode> getMetaUi() {
        return Collections.unmodifiableMap(metaUi);
    }

    @Override
    public void init(UiConnection connection, ServiceDirectory directory) {
        super.init(connection, directory);
        this.directory = checkNotNull(directory, "Directory cannot be null");
        clusterService = directory.get(ClusterService.class);
        deviceService = directory.get(DeviceService.class);
        linkService = directory.get(LinkService.class);
        hostService = directory.get(HostService.class);
        mastershipService = directory.get(MastershipService.class);
        intentService = directory.get(IntentService.class);
        flowService = directory.get(FlowRuleService.class);
        flowStatsService = directory.get(StatisticService.class);
        portStatsService = directory.get(PortStatisticsService.class);
        topologyService = directory.get(TopologyService.class);
        tunnelService = directory.get(TunnelService.class);

        servicesBundle = new ServicesBundle(intentService, deviceService,
                                            hostService, linkService,
                                            flowService,
                                            flowStatsService, portStatsService);

        String ver = directory.get(CoreService.class).version().toString();
        version = ver.replace(".SNAPSHOT", "*").replaceFirst("~.*$", "");
    }

    // Returns the specified set of IP addresses as a string.
    private String ip(Set<IpAddress> ipAddresses) {
        Iterator<IpAddress> it = ipAddresses.iterator();
        return it.hasNext() ? it.next().toString() : "unknown";
    }

    // Produces JSON structure from annotations.
    private JsonNode props(Annotations annotations) {
        ObjectNode props = objectNode();
        if (annotations != null) {
            for (String key : annotations.keys()) {
                props.put(key, annotations.value(key));
            }
        }
        return props;
    }

    // Produces an informational log message event bound to the client.
    protected ObjectNode info(long id, String message) {
        return message("info", id, message);
    }

    // Produces a warning log message event bound to the client.
    protected ObjectNode warning(long id, String message) {
        return message("warning", id, message);
    }

    // Produces an error log message event bound to the client.
    protected ObjectNode error(long id, String message) {
        return message("error", id, message);
    }

    // Produces a log message event bound to the client.
    private ObjectNode message(String severity, long id, String message) {
        ObjectNode payload = objectNode()
                .put("severity", severity)
                .put("message", message);

        return JsonUtils.envelope("message", id, payload);
    }

    // Produces a cluster instance message to the client.
    protected ObjectNode instanceMessage(ClusterEvent event, String messageType) {
        ControllerNode node = event.subject();
        int switchCount = mastershipService.getDevicesOf(node.id()).size();
        ObjectNode payload = objectNode()
                .put("id", node.id().toString())
                .put("ip", node.ip().toString())
                .put("online", clusterService.getState(node.id()) == ACTIVE)
                .put("uiAttached", node.equals(clusterService.getLocalNode()))
                .put("switches", switchCount);

        ArrayNode labels = arrayNode();
        labels.add(node.id().toString());
        labels.add(node.ip().toString());

        // Add labels, props and stuff the payload into envelope.
        payload.set("labels", labels);
        addMetaUi(node.id().toString(), payload);

        String type = messageType != null ? messageType :
                ((event.type() == INSTANCE_ADDED) ? "addInstance" :
                        ((event.type() == INSTANCE_REMOVED ? "removeInstance" :
                                "addInstance")));
        return JsonUtils.envelope(type, 0, payload);
    }

    // Produces a device event message to the client.
    protected ObjectNode deviceMessage(DeviceEvent event) {
        Device device = event.subject();
        ObjectNode payload = objectNode()
                .put("id", device.id().toString())
                .put("type", device.type().toString().toLowerCase())
                .put("online", deviceService.isAvailable(device.id()))
                .put("master", master(device.id()));

        // Generate labels: id, chassis id, no-label, optional-name
        String name = device.annotations().value(AnnotationKeys.NAME);
        ArrayNode labels = arrayNode();
        labels.add("");
        labels.add(isNullOrEmpty(name) ? device.id().toString() : name);
        labels.add(device.id().toString());

        // Add labels, props and stuff the payload into envelope.
        payload.set("labels", labels);
        payload.set("props", props(device.annotations()));
        addGeoLocation(device, payload);
        addMetaUi(device.id().toString(), payload);

        String type = (event.type() == DEVICE_ADDED) ? "addDevice" :
                ((event.type() == DEVICE_REMOVED) ? "removeDevice" : "updateDevice");
        return JsonUtils.envelope(type, 0, payload);
    }

    // Produces a link event message to the client.
    protected ObjectNode linkMessage(LinkEvent event) {
        Link link = event.subject();
        ObjectNode payload = objectNode()
                .put("id", compactLinkString(link))
                .put("type", link.type().toString().toLowerCase())
                .put("online", link.state() == Link.State.ACTIVE)
                .put("linkWidth", 1.2)
                .put("src", link.src().deviceId().toString())
                .put("srcPort", link.src().port().toString())
                .put("dst", link.dst().deviceId().toString())
                .put("dstPort", link.dst().port().toString());
        String type = (event.type() == LINK_ADDED) ? "addLink" :
                ((event.type() == LINK_REMOVED) ? "removeLink" : "updateLink");
        return JsonUtils.envelope(type, 0, payload);
    }

    // Produces a host event message to the client.
    protected ObjectNode hostMessage(HostEvent event) {
        Host host = event.subject();
        String hostType = host.annotations().value(AnnotationKeys.TYPE);
        ObjectNode payload = objectNode()
                .put("id", host.id().toString())
                .put("type", isNullOrEmpty(hostType) ? "endstation" : hostType)
                .put("ingress", compactLinkString(edgeLink(host, true)))
                .put("egress", compactLinkString(edgeLink(host, false)));
        payload.set("cp", hostConnect(host.location()));
        payload.set("labels", labels(ip(host.ipAddresses()),
                                     host.mac().toString()));
        payload.set("props", props(host.annotations()));
        addGeoLocation(host, payload);
        addMetaUi(host.id().toString(), payload);

        String type = (event.type() == HOST_ADDED) ? "addHost" :
                ((event.type() == HOST_REMOVED) ? "removeHost" : "updateHost");
        return JsonUtils.envelope(type, 0, payload);
    }

    // Encodes the specified host location into a JSON object.
    private ObjectNode hostConnect(HostLocation location) {
        return objectNode()
                .put("device", location.deviceId().toString())
                .put("port", location.port().toLong());
    }

    // Encodes the specified list of labels a JSON array.
    private ArrayNode labels(String... labels) {
        ArrayNode json = arrayNode();
        for (String label : labels) {
            json.add(label);
        }
        return json;
    }

    // Returns the name of the master node for the specified device id.
    private String master(DeviceId deviceId) {
        NodeId master = mastershipService.getMasterFor(deviceId);
        return master != null ? master.toString() : "";
    }

    // Generates an edge link from the specified host location.
    private EdgeLink edgeLink(Host host, boolean ingress) {
        return new DefaultEdgeLink(PID, new ConnectPoint(host.id(), portNumber(0)),
                                   host.location(), ingress);
    }

    // Adds meta UI information for the specified object.
    private void addMetaUi(String id, ObjectNode payload) {
        ObjectNode meta = metaUi.get(id);
        if (meta != null) {
            payload.set("metaUi", meta);
        }
    }

    // Adds a geo location JSON to the specified payload object.
    private void addGeoLocation(Annotated annotated, ObjectNode payload) {
        Annotations annotations = annotated.annotations();
        if (annotations == null) {
            return;
        }

        String slat = annotations.value(AnnotationKeys.LATITUDE);
        String slng = annotations.value(AnnotationKeys.LONGITUDE);
        try {
            if (slat != null && slng != null && !slat.isEmpty() && !slng.isEmpty()) {
                double lat = Double.parseDouble(slat);
                double lng = Double.parseDouble(slng);
                ObjectNode loc = objectNode()
                        .put("type", "latlng").put("lat", lat).put("lng", lng);
                payload.set("location", loc);
            }
        } catch (NumberFormatException e) {
            log.warn("Invalid geo data latitude={}; longiture={}", slat, slng);
        }
    }

    // Updates meta UI information for the specified object.
    protected void updateMetaUi(ObjectNode payload) {
        metaUi.put(JsonUtils.string(payload, "id"),
                   JsonUtils.node(payload, "memento"));
    }


    // -----------------------------------------------------------------------
    // Create models of the data to return, that overlays can adjust / augment

    // Returns property panel model for summary response.
    protected PropertyPanel summmaryMessage(long sid) {
        Topology topology = topologyService.currentTopology();

        return new PropertyPanel("ONOS Summary", "node")
            .addProp(Properties.DEVICES, topology.deviceCount())
            .addProp(Properties.LINKS, topology.linkCount())
            .addProp(Properties.HOSTS, hostService.getHostCount())
            .addProp(Properties.TOPOLOGY_SSCS, topology.clusterCount())
            .addSeparator()
            .addProp(Properties.INTENTS, intentService.getIntentCount())
            .addProp(Properties.TUNNELS, tunnelService.tunnelCount())
            .addProp(Properties.FLOWS, flowService.getFlowRuleCount())
            .addProp(Properties.VERSION, version);
    }

    // Returns property panel model for device details response.
    protected PropertyPanel deviceDetails(DeviceId deviceId, long sid) {
        Device device = deviceService.getDevice(deviceId);
        Annotations annot = device.annotations();
        String name = annot.value(AnnotationKeys.NAME);
        int portCount = deviceService.getPorts(deviceId).size();
        int flowCount = getFlowCount(deviceId);
        int tunnelCount = getTunnelCount(deviceId);

        String title = isNullOrEmpty(name) ? deviceId.toString() : name;
        String typeId = device.type().toString().toLowerCase();

        PropertyPanel pp = new PropertyPanel(title, typeId)
            .id(deviceId.toString())

            .addProp(Properties.URI, deviceId.toString())
            .addProp(Properties.VENDOR, device.manufacturer())
            .addProp(Properties.HW_VERSION, device.hwVersion())
            .addProp(Properties.SW_VERSION, device.swVersion())
            .addProp(Properties.SERIAL_NUMBER, device.serialNumber())
            .addProp(Properties.PROTOCOL, annot.value(AnnotationKeys.PROTOCOL))
            .addSeparator()

            .addProp(Properties.LATITUDE, annot.value(AnnotationKeys.LATITUDE))
            .addProp(Properties.LONGITUDE, annot.value(AnnotationKeys.LONGITUDE))
            .addSeparator()

            .addProp(Properties.PORTS, portCount)
            .addProp(Properties.FLOWS, flowCount)
            .addProp(Properties.TUNNELS, tunnelCount)

            .addButton(CoreButtons.SHOW_DEVICE_VIEW)
            .addButton(CoreButtons.SHOW_FLOW_VIEW)
            .addButton(CoreButtons.SHOW_PORT_VIEW)
            .addButton(CoreButtons.SHOW_GROUP_VIEW);

        return pp;
    }

    protected int getFlowCount(DeviceId deviceId) {
        int count = 0;
        for (FlowEntry flowEntry : flowService.getFlowEntries(deviceId)) {
            count++;
        }
        return count;
    }

    protected int getTunnelCount(DeviceId deviceId) {
        int count = 0;
        Collection<Tunnel> tunnels = tunnelService.queryAllTunnels();
        for (Tunnel tunnel : tunnels) {
            OpticalTunnelEndPoint src = (OpticalTunnelEndPoint) tunnel.src();
            OpticalTunnelEndPoint dst = (OpticalTunnelEndPoint) tunnel.dst();
            DeviceId srcDevice = (DeviceId) src.elementId().get();
            DeviceId dstDevice = (DeviceId) dst.elementId().get();
            if (srcDevice.toString().equals(deviceId.toString()) ||
                dstDevice.toString().equals(deviceId.toString())) {
                count++;
            }
        }
        return count;
    }

    // Counts all flow entries that egress on the links of the given device.
    private Map<Link, Integer> getLinkFlowCounts(DeviceId deviceId) {
        // get the flows for the device
        List<FlowEntry> entries = new ArrayList<>();
        for (FlowEntry flowEntry : flowService.getFlowEntries(deviceId)) {
            entries.add(flowEntry);
        }

        // get egress links from device, and include edge links
        Set<Link> links = new HashSet<>(linkService.getDeviceEgressLinks(deviceId));
        Set<Host> hosts = hostService.getConnectedHosts(deviceId);
        if (hosts != null) {
            for (Host host : hosts) {
                links.add(createEdgeLink(host, false));
            }
        }

        // compile flow counts per link
        Map<Link, Integer> counts = new HashMap<>();
        for (Link link : links) {
            counts.put(link, getEgressFlows(link, entries));
        }
        return counts;
    }

    // Counts all entries that egress on the link source port.
    private int getEgressFlows(Link link, List<FlowEntry> entries) {
        int count = 0;
        PortNumber out = link.src().port();
        for (FlowEntry entry : entries) {
            TrafficTreatment treatment = entry.treatment();
            for (Instruction instruction : treatment.allInstructions()) {
                if (instruction.type() == Instruction.Type.OUTPUT &&
                        ((OutputInstruction) instruction).port().equals(out)) {
                    count++;
                }
            }
        }
        return count;
    }

    // Returns host details response.
    protected PropertyPanel hostDetails(HostId hostId, long sid) {
        Host host = hostService.getHost(hostId);
        Annotations annot = host.annotations();
        String type = annot.value(AnnotationKeys.TYPE);
        String name = annot.value(AnnotationKeys.NAME);
        String vlan = host.vlan().toString();

        String title = isNullOrEmpty(name) ? hostId.toString() : name;
        String typeId = isNullOrEmpty(type) ? "endstation" : type;

        PropertyPanel pp = new PropertyPanel(title, typeId)
            .id(hostId.toString())
            .addProp(Properties.MAC, host.mac())
            .addProp(Properties.IP, host.ipAddresses(), "[\\[\\]]")
            .addProp(Properties.VLAN, vlan.equals("-1") ? "none" : vlan)
            .addSeparator()
            .addProp(Properties.LATITUDE, annot.value(AnnotationKeys.LATITUDE))
            .addProp(Properties.LONGITUDE, annot.value(AnnotationKeys.LONGITUDE));

        // Potentially add button descriptors here
        return pp;
    }

}

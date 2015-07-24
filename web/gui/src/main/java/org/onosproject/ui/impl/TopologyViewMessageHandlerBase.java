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
import com.google.common.collect.ImmutableList;
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
import org.onosproject.net.LinkKey;
import org.onosproject.net.NetworkResource;
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
import org.onosproject.net.intent.FlowRuleIntent;
import org.onosproject.net.intent.Intent;
import org.onosproject.net.intent.IntentService;
import org.onosproject.net.intent.LinkCollectionIntent;
import org.onosproject.net.intent.OpticalConnectivityIntent;
import org.onosproject.net.intent.OpticalPathIntent;
import org.onosproject.net.intent.PathIntent;
import org.onosproject.net.link.LinkEvent;
import org.onosproject.net.link.LinkService;
import org.onosproject.net.provider.ProviderId;
import org.onosproject.net.statistic.Load;
import org.onosproject.net.statistic.StatisticService;
import org.onosproject.net.topology.Topology;
import org.onosproject.net.topology.TopologyService;
import org.onosproject.ui.JsonUtils;
import org.onosproject.ui.UiConnection;
import org.onosproject.ui.UiMessageHandler;
import org.onosproject.ui.topo.PropertyPanel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.DecimalFormat;
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
import static org.onosproject.net.DeviceId.deviceId;
import static org.onosproject.net.HostId.hostId;
import static org.onosproject.net.LinkKey.linkKey;
import static org.onosproject.net.PortNumber.P0;
import static org.onosproject.net.PortNumber.portNumber;
import static org.onosproject.net.device.DeviceEvent.Type.DEVICE_ADDED;
import static org.onosproject.net.device.DeviceEvent.Type.DEVICE_REMOVED;
import static org.onosproject.net.host.HostEvent.Type.HOST_ADDED;
import static org.onosproject.net.host.HostEvent.Type.HOST_REMOVED;
import static org.onosproject.net.link.LinkEvent.Type.LINK_ADDED;
import static org.onosproject.net.link.LinkEvent.Type.LINK_REMOVED;
import static org.onosproject.ui.impl.TopologyViewMessageHandlerBase.StatsType.FLOW;
import static org.onosproject.ui.impl.TopologyViewMessageHandlerBase.StatsType.PORT;

/**
 * Facility for creating messages bound for the topology viewer.
 *
 * @deprecated in Cardinal Release
 */
@Deprecated
public abstract class TopologyViewMessageHandlerBase extends UiMessageHandler {

    protected static final Logger log =
            LoggerFactory.getLogger(TopologyViewMessageHandlerBase.class);

    private static final ProviderId PID =
            new ProviderId("core", "org.onosproject.core", true);
    private static final String COMPACT = "%s/%s-%s/%s";

    private static final double KILO = 1024;
    private static final double MEGA = 1024 * KILO;
    private static final double GIGA = 1024 * MEGA;

    private static final String GBITS_UNIT = "Gb";
    private static final String MBITS_UNIT = "Mb";
    private static final String KBITS_UNIT = "Kb";
    private static final String BITS_UNIT = "b";
    private static final String GBYTES_UNIT = "GB";
    private static final String MBYTES_UNIT = "MB";
    private static final String KBYTES_UNIT = "KB";
    private static final String BYTES_UNIT = "B";
    //4 Kilo Bytes as threshold
    private static final double BPS_THRESHOLD = 4 * KILO;

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

    protected enum StatsType {
        FLOW, PORT
    }

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

    // Produces a set of all hosts listed in the specified JSON array.
    protected Set<Host> getHosts(ArrayNode array) {
        Set<Host> hosts = new HashSet<>();
        if (array != null) {
            for (JsonNode node : array) {
                try {
                    addHost(hosts, hostId(node.asText()));
                } catch (IllegalArgumentException e) {
                    log.debug("Skipping ID {}", node.asText());
                }
            }
        }
        return hosts;
    }

    // Adds the specified host to the set of hosts.
    private void addHost(Set<Host> hosts, HostId hostId) {
        Host host = hostService.getHost(hostId);
        if (host != null) {
            hosts.add(host);
        }
    }


    // Produces a set of all devices listed in the specified JSON array.
    protected Set<Device> getDevices(ArrayNode array) {
        Set<Device> devices = new HashSet<>();
        if (array != null) {
            for (JsonNode node : array) {
                try {
                    addDevice(devices, deviceId(node.asText()));
                } catch (IllegalArgumentException e) {
                    log.debug("Skipping ID {}", node.asText());
                }
            }
        }
        return devices;
    }

    private void addDevice(Set<Device> devices, DeviceId deviceId) {
        Device device = deviceService.getDevice(deviceId);
        if (device != null) {
            devices.add(device);
        }
    }

    protected void addHover(Set<Host> hosts, Set<Device> devices, String hover) {
        try {
            addHost(hosts, hostId(hover));
        } catch (IllegalArgumentException e) {
            try {
                addDevice(devices, deviceId(hover));
            } catch (IllegalArgumentException ne) {
                log.debug("Skipping ID {}", hover);
            }
        }
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

    // Returns summary response.
    protected PropertyPanel summmaryMessage(long sid) {
        Topology topology = topologyService.currentTopology();
        PropertyPanel pp = new PropertyPanel("ONOS Summary", "node")
            .add(new PropertyPanel.Prop("Devices", format(topology.deviceCount())))
            .add(new PropertyPanel.Prop("Links", format(topology.linkCount())))
            .add(new PropertyPanel.Prop("Hosts", format(hostService.getHostCount())))
            .add(new PropertyPanel.Prop("Topology SCCs", format(topology.clusterCount())))
            .add(new PropertyPanel.Separator())
            .add(new PropertyPanel.Prop("Intents", format(intentService.getIntentCount())))
            .add(new PropertyPanel.Prop("Tunnels", format(tunnelService.tunnelCount())))
            .add(new PropertyPanel.Prop("Flows", format(flowService.getFlowRuleCount())))
            .add(new PropertyPanel.Prop("Version", version));

        return pp;
    }

    // Returns device details response.
    protected ObjectNode deviceDetails(DeviceId deviceId, long sid) {
        Device device = deviceService.getDevice(deviceId);
        Annotations annot = device.annotations();
        String name = annot.value(AnnotationKeys.NAME);
        int portCount = deviceService.getPorts(deviceId).size();
        int flowCount = getFlowCount(deviceId);
        int tunnelCount = getTunnelCount(deviceId);
        return JsonUtils.envelope("showDetails", sid,
                                  json(isNullOrEmpty(name) ? deviceId.toString() : name,
                                       device.type().toString().toLowerCase(),
                                       new Prop("URI", deviceId.toString()),
                                       new Prop("Vendor", device.manufacturer()),
                                       new Prop("H/W Version", device.hwVersion()),
                                       new Prop("S/W Version", device.swVersion()),
                                       new Prop("Serial Number", device.serialNumber()),
                                       new Prop("Protocol", annot.value(AnnotationKeys.PROTOCOL)),
                                       new Separator(),
                                       new Prop("Master", master(deviceId)),
                                       new Prop("Latitude", annot.value(AnnotationKeys.LATITUDE)),
                                       new Prop("Longitude", annot.value(AnnotationKeys.LONGITUDE)),
                                       new Separator(),
                                       new Prop("Ports", Integer.toString(portCount)),
                                       new Prop("Flows", Integer.toString(flowCount)),
                                       new Prop("Tunnels", Integer.toString(tunnelCount))
                                  ));
    }

    protected int getFlowCount(DeviceId deviceId) {
        int count = 0;
        Iterator<FlowEntry> it = flowService.getFlowEntries(deviceId).iterator();
        while (it.hasNext()) {
            count++;
            it.next();
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
            if (srcDevice.toString().equals(deviceId.toString())
             || dstDevice.toString().equals(deviceId.toString())) {
                count++;
            }
        }
        return count;
    }

    // Counts all entries that egress on the given device links.
    protected Map<Link, Integer> getFlowCounts(DeviceId deviceId) {
        List<FlowEntry> entries = new ArrayList<>();
        Set<Link> links = new HashSet<>(linkService.getDeviceEgressLinks(deviceId));
        Set<Host> hosts = hostService.getConnectedHosts(deviceId);
        Iterator<FlowEntry> it = flowService.getFlowEntries(deviceId).iterator();
        while (it.hasNext()) {
            entries.add(it.next());
        }

        // Add all edge links to the set
        if (hosts != null) {
            for (Host host : hosts) {
                links.add(new DefaultEdgeLink(host.providerId(),
                                              new ConnectPoint(host.id(), P0),
                                              host.location(), false));
            }
        }

        Map<Link, Integer> counts = new HashMap<>();
        for (Link link : links) {
            counts.put(link, getEgressFlows(link, entries));
        }
        return counts;
    }

    // Counts all entries that egress on the link source port.
    private Integer getEgressFlows(Link link, List<FlowEntry> entries) {
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
    protected ObjectNode hostDetails(HostId hostId, long sid) {
        Host host = hostService.getHost(hostId);
        Annotations annot = host.annotations();
        String type = annot.value(AnnotationKeys.TYPE);
        String name = annot.value(AnnotationKeys.NAME);
        String vlan = host.vlan().toString();
        return JsonUtils.envelope("showDetails", sid,
                                  json(isNullOrEmpty(name) ? hostId.toString() : name,
                                       isNullOrEmpty(type) ? "endstation" : type,
                                       new Prop("MAC", host.mac().toString()),
                                       new Prop("IP", host.ipAddresses().toString().replaceAll("[\\[\\]]", "")),
                                       new Prop("VLAN", vlan.equals("-1") ? "none" : vlan),
                                       new Separator(),
                                       new Prop("Latitude", annot.value(AnnotationKeys.LATITUDE)),
                                       new Prop("Longitude", annot.value(AnnotationKeys.LONGITUDE))));
    }


    // Produces JSON message to trigger flow traffic overview visualization
    protected ObjectNode trafficSummaryMessage(StatsType type) {
        ObjectNode payload = objectNode();
        ArrayNode paths = arrayNode();
        payload.set("paths", paths);

        ObjectNode pathNodeN = objectNode();
        ArrayNode linksNodeN = arrayNode();
        ArrayNode labelsN = arrayNode();

        pathNodeN.put("class", "plain").put("traffic", false);
        pathNodeN.set("links", linksNodeN);
        pathNodeN.set("labels", labelsN);
        paths.add(pathNodeN);

        ObjectNode pathNodeT = objectNode();
        ArrayNode linksNodeT = arrayNode();
        ArrayNode labelsT = arrayNode();

        pathNodeT.put("class", "secondary").put("traffic", true);
        pathNodeT.set("links", linksNodeT);
        pathNodeT.set("labels", labelsT);
        paths.add(pathNodeT);

        Map<LinkKey, BiLink> biLinks = consolidateLinks(linkService.getLinks());
        addEdgeLinks(biLinks);
        for (BiLink link : biLinks.values()) {
            boolean bi = link.two != null;
            if (type == FLOW) {
                link.addLoad(getLinkLoad(link.one));
                link.addLoad(bi ? getLinkLoad(link.two) : null);
            } else if (type == PORT) {
                //For a bi-directional traffic links, use
                //the max link rate of either direction
                link.addLoad(portStatsService.load(link.one.src()),
                             BPS_THRESHOLD,
                             portStatsService.load(link.one.dst()),
                             BPS_THRESHOLD);
            }
            if (link.hasTraffic) {
                linksNodeT.add(compactLinkString(link.one));
                labelsT.add(type == PORT ?
                                    formatBitRate(link.rate) + "ps" :
                                    formatBytes(link.bytes));
            } else {
                linksNodeN.add(compactLinkString(link.one));
                labelsN.add("");
            }
        }
        return JsonUtils.envelope("showTraffic", 0, payload);
    }

    private Load getLinkLoad(Link link) {
        if (link.src().elementId() instanceof DeviceId) {
            return flowStatsService.load(link);
        }
        return null;
    }

    private void addEdgeLinks(Map<LinkKey, BiLink> biLinks) {
        hostService.getHosts().forEach(host -> {
            addLink(biLinks, createEdgeLink(host, true));
            addLink(biLinks, createEdgeLink(host, false));
        });
    }

    private Map<LinkKey, BiLink> consolidateLinks(Iterable<Link> links) {
        Map<LinkKey, BiLink> biLinks = new HashMap<>();
        for (Link link : links) {
            addLink(biLinks, link);
        }
        return biLinks;
    }

    // Produces JSON message to trigger flow overview visualization
    protected ObjectNode flowSummaryMessage(Set<Device> devices) {
        ObjectNode payload = objectNode();
        ArrayNode paths = arrayNode();
        payload.set("paths", paths);

        for (Device device : devices) {
            Map<Link, Integer> counts = getFlowCounts(device.id());
            for (Link link : counts.keySet()) {
                addLinkFlows(link, paths, counts.get(link));
            }
        }
        return JsonUtils.envelope("showTraffic", 0, payload);
    }

    private void addLinkFlows(Link link, ArrayNode paths, Integer count) {
        ObjectNode pathNode = objectNode();
        ArrayNode linksNode = arrayNode();
        ArrayNode labels = arrayNode();
        boolean noFlows = count == null || count == 0;
        pathNode.put("class", noFlows ? "secondary" : "primary");
        pathNode.put("traffic", false);
        pathNode.set("links", linksNode.add(compactLinkString(link)));
        pathNode.set("labels", labels.add(noFlows ? "" : (count.toString() +
                (count == 1 ? " flow" : " flows"))));
        paths.add(pathNode);
    }


    // Produces JSON message to trigger traffic visualization
    protected ObjectNode trafficMessage(TrafficClass... trafficClasses) {
        ObjectNode payload = objectNode();
        ArrayNode paths = arrayNode();
        payload.set("paths", paths);

        // Classify links based on their traffic traffic first...
        Map<LinkKey, BiLink> biLinks = classifyLinkTraffic(trafficClasses);

        // Then separate the links into their respective classes and send them out.
        Map<String, ObjectNode> pathNodes = new HashMap<>();
        for (BiLink biLink : biLinks.values()) {
            boolean hasTraffic = biLink.hasTraffic;
            String tc = (biLink.classes() + (hasTraffic ? " animated" : "")).trim();
            ObjectNode pathNode = pathNodes.get(tc);
            if (pathNode == null) {
                pathNode = objectNode()
                        .put("class", tc).put("traffic", hasTraffic);
                pathNode.set("links", arrayNode());
                pathNode.set("labels", arrayNode());
                pathNodes.put(tc, pathNode);
                paths.add(pathNode);
            }
            ((ArrayNode) pathNode.path("links")).add(compactLinkString(biLink.one));
            ((ArrayNode) pathNode.path("labels")).add(hasTraffic ? formatBytes(biLink.bytes) : "");
        }

        return JsonUtils.envelope("showTraffic", 0, payload);
    }

    // Classifies the link traffic according to the specified classes.
    private Map<LinkKey, BiLink> classifyLinkTraffic(TrafficClass... trafficClasses) {
        Map<LinkKey, BiLink> biLinks = new HashMap<>();
        for (TrafficClass trafficClass : trafficClasses) {
            for (Intent intent : trafficClass.intents) {
                boolean isOptical = intent instanceof OpticalConnectivityIntent;
                List<Intent> installables = intentService.getInstallableIntents(intent.key());
                if (installables != null) {
                    for (Intent installable : installables) {
                        String type = isOptical ? trafficClass.type + " optical" : trafficClass.type;
                        if (installable instanceof PathIntent) {
                            classifyLinks(type, biLinks, trafficClass.showTraffic,
                                          ((PathIntent) installable).path().links());
                        } else if (installable instanceof FlowRuleIntent) {
                            classifyLinks(type, biLinks, trafficClass.showTraffic,
                                          linkResources(installable));
                        } else if (installable instanceof LinkCollectionIntent) {
                            classifyLinks(type, biLinks, trafficClass.showTraffic,
                                          ((LinkCollectionIntent) installable).links());
                        } else if (installable instanceof OpticalPathIntent) {
                            classifyLinks(type, biLinks, trafficClass.showTraffic,
                                          ((OpticalPathIntent) installable).path().links());
                        }
                    }
                }
            }
        }
        return biLinks;
    }

    // Extracts links from the specified flow rule intent resources
    private Collection<Link> linkResources(Intent installable) {
        ImmutableList.Builder<Link> builder = ImmutableList.builder();
        for (NetworkResource r : installable.resources()) {
            if (r instanceof Link) {
                builder.add((Link) r);
            }
        }
        return builder.build();
    }


    // Adds the link segments (path or tree) associated with the specified
    // connectivity intent
    private void classifyLinks(String type, Map<LinkKey, BiLink> biLinks,
                               boolean showTraffic, Iterable<Link> links) {
        if (links != null) {
            for (Link link : links) {
                BiLink biLink = addLink(biLinks, link);
                if (showTraffic) {
                    biLink.addLoad(getLinkLoad(link));
                }
                biLink.addClass(type);
            }
        }
    }


    static BiLink addLink(Map<LinkKey, BiLink> biLinks, Link link) {
        LinkKey key = canonicalLinkKey(link);
        BiLink biLink = biLinks.get(key);
        if (biLink != null) {
            biLink.setOther(link);
        } else {
            biLink = new BiLink(key, link);
            biLinks.put(key, biLink);
        }
        return biLink;
    }

    // Poor-mans formatting to get the labels with byte counts looking nice.
    private String formatBytes(long bytes) {
        String unit;
        double value;
        if (bytes > GIGA) {
            value = bytes / GIGA;
            unit = GBYTES_UNIT;
        } else if (bytes > MEGA) {
            value = bytes / MEGA;
            unit = MBYTES_UNIT;
        } else if (bytes > KILO) {
            value = bytes / KILO;
            unit = KBYTES_UNIT;
        } else {
            value = bytes;
            unit = BYTES_UNIT;
        }
        DecimalFormat format = new DecimalFormat("#,###.##");
        return format.format(value) + " " + unit;
    }

    // Poor-mans formatting to get the labels with bit rate looking nice.
    private String formatBitRate(long bytes) {
        String unit;
        double value;
        //Convert to bits
        long bits = bytes * 8;
        if (bits > GIGA) {
            value = bits / GIGA;
            unit = GBITS_UNIT;

            // NOTE: temporary hack to clip rate at 10.0 Gbps
            //  Added for the CORD Fabric demo at ONS 2015
            if (value > 10.0) {
                value = 10.0;
            }

        } else if (bits > MEGA) {
            value = bits / MEGA;
            unit = MBITS_UNIT;
        } else if (bits > KILO) {
            value = bits / KILO;
            unit = KBITS_UNIT;
        } else {
            value = bits;
            unit = BITS_UNIT;
        }
        DecimalFormat format = new DecimalFormat("#,###.##");
        return format.format(value) + " " + unit;
    }

    // Formats the given number into a string.
    private String format(Number number) {
        DecimalFormat format = new DecimalFormat("#,###");
        return format.format(number);
    }

    // Produces compact string representation of a link.
    private static String compactLinkString(Link link) {
        return String.format(COMPACT, link.src().elementId(), link.src().port(),
                             link.dst().elementId(), link.dst().port());
    }

    protected ObjectNode json(PropertyPanel pp) {
        ObjectNode result = objectNode()
                .put("title", pp.title()).put("type", pp.typeId());
        ObjectNode pnode = objectNode();
        ArrayNode porder = arrayNode();
        for (PropertyPanel.Prop p : pp.properties()) {
            porder.add(p.key());
            pnode.put(p.key(), p.value());
        }
        result.set("propOrder", porder);
        result.set("props", pnode);
        return result;
    }

    // Produces JSON property details.
    private ObjectNode json(String id, String type, Prop... props) {
        ObjectNode result = objectNode()
                .put("id", id).put("type", type);
        ObjectNode pnode = objectNode();
        ArrayNode porder = arrayNode();
        for (Prop p : props) {
            porder.add(p.key);
            pnode.put(p.key, p.value);
        }
        result.set("propOrder", porder);
        result.set("props", pnode);
        return result;
    }

    // Produces canonical link key, i.e. one that will match link and its inverse.
    static LinkKey canonicalLinkKey(Link link) {
        String sn = link.src().elementId().toString();
        String dn = link.dst().elementId().toString();
        return sn.compareTo(dn) < 0 ?
                linkKey(link.src(), link.dst()) : linkKey(link.dst(), link.src());
    }

    // Representation of link and its inverse and any traffic data.
    static class BiLink {
        public final LinkKey key;
        public final Link one;
        public Link two;
        public boolean hasTraffic = false;
        public long bytes = 0;

        private Set<String> classes = new HashSet<>();
        private long rate;

        BiLink(LinkKey key, Link link) {
            this.key = key;
            this.one = link;
        }

        void setOther(Link link) {
            this.two = link;
        }

        void addLoad(Load load) {
            addLoad(load, 0);
        }

        void addLoad(Load load, double threshold) {
            if (load != null) {
                this.hasTraffic = hasTraffic || load.rate() > threshold;
                this.bytes += load.latest();
                this.rate += load.rate();
            }
        }

        void addLoad(Load srcLinkLoad,
                     double srcLinkThreshold,
                     Load dstLinkLoad,
                     double dstLinkThreshold) {
            //use the max of link load at source or destination
            if (srcLinkLoad != null) {
                this.hasTraffic = hasTraffic || srcLinkLoad.rate() > srcLinkThreshold;
                this.bytes = srcLinkLoad.latest();
                this.rate = srcLinkLoad.rate();
            }

            if (dstLinkLoad != null) {
                if (dstLinkLoad.rate() > this.rate) {
                    this.bytes = dstLinkLoad.latest();
                    this.rate = dstLinkLoad.rate();
                    this.hasTraffic = hasTraffic || dstLinkLoad.rate() > dstLinkThreshold;
                }
            }
        }

        void addClass(String trafficClass) {
            classes.add(trafficClass);
        }

        String classes() {
            StringBuilder sb = new StringBuilder();
            classes.forEach(c -> sb.append(c).append(" "));
            return sb.toString().trim();
        }
    }

    // Auxiliary key/value carrier.
    static class Prop {
        public final String key;
        public final String value;

        protected Prop(String key, String value) {
            this.key = key;
            this.value = value;
        }
    }

    // Auxiliary properties separator
    static class Separator extends Prop {
        protected Separator() {
            super("-", "");
        }
    }

    // Auxiliary carrier of data for requesting traffic message.
    static class TrafficClass {
        public final boolean showTraffic;
        public final String type;
        public final Iterable<Intent> intents;

        TrafficClass(String type, Iterable<Intent> intents) {
            this(type, intents, false);
        }

        TrafficClass(String type, Iterable<Intent> intents, boolean showTraffic) {
            this.type = type;
            this.intents = intents;
            this.showTraffic = showTraffic;
        }
    }

}

/*
 * Copyright 2014,2015 Open Networking Laboratory
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
package org.onosproject.gui;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.onlab.osgi.ServiceDirectory;
import org.onlab.packet.IpAddress;
import org.onosproject.cluster.ClusterEvent;
import org.onosproject.cluster.ClusterService;
import org.onosproject.cluster.ControllerNode;
import org.onosproject.cluster.NodeId;
import org.onosproject.core.CoreService;
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

/**
 * Facility for creating messages bound for the topology viewer.
 */
public abstract class TopologyViewMessages {

    protected static final Logger log = LoggerFactory.getLogger(TopologyViewMessages.class);

    private static final ProviderId PID = new ProviderId("core", "org.onosproject.core", true);
    private static final String COMPACT = "%s/%s-%s/%s";

    private static final double KB = 1024;
    private static final double MB = 1024 * KB;
    private static final double GB = 1024 * MB;

    private static final String GB_UNIT = "GB";
    private static final String MB_UNIT = "MB";
    private static final String KB_UNIT = "KB";
    private static final String B_UNIT = "B";

    protected final ServiceDirectory directory;
    protected final ClusterService clusterService;
    protected final DeviceService deviceService;
    protected final LinkService linkService;
    protected final HostService hostService;
    protected final MastershipService mastershipService;
    protected final IntentService intentService;
    protected final FlowRuleService flowService;
    protected final StatisticService statService;
    protected final TopologyService topologyService;

    protected final ObjectMapper mapper = new ObjectMapper();
    private final String version;

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

    /**
     * Creates a messaging facility for creating messages for topology viewer.
     *
     * @param directory service directory
     */
    protected TopologyViewMessages(ServiceDirectory directory) {
        this.directory = checkNotNull(directory, "Directory cannot be null");
        clusterService = directory.get(ClusterService.class);
        deviceService = directory.get(DeviceService.class);
        linkService = directory.get(LinkService.class);
        hostService = directory.get(HostService.class);
        mastershipService = directory.get(MastershipService.class);
        intentService = directory.get(IntentService.class);
        flowService = directory.get(FlowRuleService.class);
        statService = directory.get(StatisticService.class);
        topologyService = directory.get(TopologyService.class);

        String ver = directory.get(CoreService.class).version().toString();
        version = ver.replace(".SNAPSHOT", "*").replaceFirst("~.*$", "");
    }

    // Retrieves the payload from the specified event.
    protected ObjectNode payload(ObjectNode event) {
        return (ObjectNode) event.path("payload");
    }

    // Returns the specified node property as a number
    protected long number(ObjectNode node, String name) {
        return node.path(name).asLong();
    }

    // Returns the specified node property as a string.
    protected String string(ObjectNode node, String name) {
        return node.path(name).asText();
    }

    // Returns the specified node property as a string.
    protected String string(ObjectNode node, String name, String defaultValue) {
        return node.path(name).asText(defaultValue);
    }

    // Returns the specified set of IP addresses as a string.
    private String ip(Set<IpAddress> ipAddresses) {
        Iterator<IpAddress> it = ipAddresses.iterator();
        return it.hasNext() ? it.next().toString() : "unknown";
    }

    // Produces JSON structure from annotations.
    private JsonNode props(Annotations annotations) {
        ObjectNode props = mapper.createObjectNode();
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
        return envelope("message", id,
                        mapper.createObjectNode()
                                .put("severity", severity)
                                .put("message", message));
    }

    // Puts the payload into an envelope and returns it.
    protected ObjectNode envelope(String type, long sid, ObjectNode payload) {
        ObjectNode event = mapper.createObjectNode();
        event.put("event", type);
        if (sid > 0) {
            event.put("sid", sid);
        }
        event.set("payload", payload);
        return event;
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
        ObjectNode payload = mapper.createObjectNode()
                .put("id", node.id().toString())
                .put("ip", node.ip().toString())
                .put("online", clusterService.getState(node.id()) == ACTIVE)
                .put("uiAttached", event.subject().equals(clusterService.getLocalNode()))
                .put("switches", switchCount);

        ArrayNode labels = mapper.createArrayNode();
        labels.add(node.id().toString());
        labels.add(node.ip().toString());

        // Add labels, props and stuff the payload into envelope.
        payload.set("labels", labels);
        addMetaUi(node.id().toString(), payload);

        String type = messageType != null ? messageType :
                ((event.type() == INSTANCE_ADDED) ? "addInstance" :
                        ((event.type() == INSTANCE_REMOVED ? "removeInstance" :
                                "addInstance")));
        return envelope(type, 0, payload);
    }

    // Produces a device event message to the client.
    protected ObjectNode deviceMessage(DeviceEvent event) {
        Device device = event.subject();
        ObjectNode payload = mapper.createObjectNode()
                .put("id", device.id().toString())
                .put("type", device.type().toString().toLowerCase())
                .put("online", deviceService.isAvailable(device.id()))
                .put("master", master(device.id()));

        // Generate labels: id, chassis id, no-label, optional-name
        String name = device.annotations().value(AnnotationKeys.NAME);
        ArrayNode labels = mapper.createArrayNode();
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
        return envelope(type, 0, payload);
    }

    // Produces a link event message to the client.
    protected ObjectNode linkMessage(LinkEvent event) {
        Link link = event.subject();
        ObjectNode payload = mapper.createObjectNode()
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
        return envelope(type, 0, payload);
    }

    // Produces a host event message to the client.
    protected ObjectNode hostMessage(HostEvent event) {
        Host host = event.subject();
        String hostType = host.annotations().value(AnnotationKeys.TYPE);
        ObjectNode payload = mapper.createObjectNode()
                .put("id", host.id().toString())
                .put("type", isNullOrEmpty(hostType) ? "endstation" : hostType)
                .put("ingress", compactLinkString(edgeLink(host, true)))
                .put("egress", compactLinkString(edgeLink(host, false)));
        payload.set("cp", hostConnect(mapper, host.location()));
        payload.set("labels", labels(mapper, ip(host.ipAddresses()),
                                     host.mac().toString()));
        payload.set("props", props(host.annotations()));
        addGeoLocation(host, payload);
        addMetaUi(host.id().toString(), payload);

        String type = (event.type() == HOST_ADDED) ? "addHost" :
                ((event.type() == HOST_REMOVED) ? "removeHost" : "updateHost");
        return envelope(type, 0, payload);
    }

    // Encodes the specified host location into a JSON object.
    private ObjectNode hostConnect(ObjectMapper mapper, HostLocation location) {
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
                ObjectNode loc = mapper.createObjectNode()
                        .put("type", "latlng").put("lat", lat).put("lng", lng);
                payload.set("location", loc);
            }
        } catch (NumberFormatException e) {
            log.warn("Invalid geo data latitude={}; longiture={}", slat, slng);
        }
    }

    // Updates meta UI information for the specified object.
    protected void updateMetaUi(ObjectNode event) {
        ObjectNode payload = payload(event);
        metaUi.put(string(payload, "id"), (ObjectNode) payload.path("memento"));
    }

    // Returns summary response.
    protected ObjectNode summmaryMessage(long sid) {
        Topology topology = topologyService.currentTopology();
        return envelope("showSummary", sid,
                        json("ONOS Summary", "node",
                             new Prop("Devices", format(topology.deviceCount())),
                             new Prop("Links", format(topology.linkCount())),
                             new Prop("Hosts", format(hostService.getHostCount())),
                             new Prop("Topology SCCs", format(topology.clusterCount())),
                             new Separator(),
                             new Prop("Intents", format(intentService.getIntentCount())),
                             new Prop("Flows", format(flowService.getFlowRuleCount())),
                             new Prop("Version", version)));
    }

    // Returns device details response.
    protected ObjectNode deviceDetails(DeviceId deviceId, long sid) {
        Device device = deviceService.getDevice(deviceId);
        Annotations annot = device.annotations();
        String name = annot.value(AnnotationKeys.NAME);
        int portCount = deviceService.getPorts(deviceId).size();
        int flowCount = getFlowCount(deviceId);
        return envelope("showDetails", sid,
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
                             new Prop("Flows", Integer.toString(flowCount))));
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
            for (Instruction instruction : treatment.instructions()) {
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
        return envelope("showDetails", sid,
                        json(isNullOrEmpty(name) ? hostId.toString() : name,
                             isNullOrEmpty(type) ? "endstation" : type,
                             new Prop("MAC", host.mac().toString()),
                             new Prop("IP", host.ipAddresses().toString().replaceAll("[\\[\\]]", "")),
                             new Prop("VLAN", vlan.equals("-1") ? "none" : vlan),
                             new Separator(),
                             new Prop("Latitude", annot.value(AnnotationKeys.LATITUDE)),
                             new Prop("Longitude", annot.value(AnnotationKeys.LONGITUDE))));
    }


    // Produces JSON message to trigger traffic overview visualization
    protected ObjectNode trafficSummaryMessage(long sid) {
        ObjectNode payload = mapper.createObjectNode();
        ArrayNode paths = mapper.createArrayNode();
        payload.set("paths", paths);

        ObjectNode pathNodeN = mapper.createObjectNode();
        ArrayNode linksNodeN = mapper.createArrayNode();
        ArrayNode labelsN = mapper.createArrayNode();

        pathNodeN.put("class", "plain").put("traffic", false);
        pathNodeN.set("links", linksNodeN);
        pathNodeN.set("labels", labelsN);
        paths.add(pathNodeN);

        ObjectNode pathNodeT = mapper.createObjectNode();
        ArrayNode linksNodeT = mapper.createArrayNode();
        ArrayNode labelsT = mapper.createArrayNode();

        pathNodeT.put("class", "secondary").put("traffic", true);
        pathNodeT.set("links", linksNodeT);
        pathNodeT.set("labels", labelsT);
        paths.add(pathNodeT);

        for (BiLink link : consolidateLinks(linkService.getLinks())) {
            boolean bi = link.two != null;
            if (isInfrastructureEgress(link.one) ||
                    (bi && isInfrastructureEgress(link.two))) {
                link.addLoad(statService.load(link.one));
                link.addLoad(bi ? statService.load(link.two) : null);
                if (link.hasTraffic) {
                    linksNodeT.add(compactLinkString(link.one));
                    labelsT.add(formatBytes(link.bytes));
                } else {
                    linksNodeN.add(compactLinkString(link.one));
                    labelsN.add("");
                }
            }
        }
        return envelope("showTraffic", sid, payload);
    }

    private Collection<BiLink> consolidateLinks(Iterable<Link> links) {
        Map<LinkKey, BiLink> biLinks = new HashMap<>();
        for (Link link : links) {
            addLink(biLinks, link);
        }
        return biLinks.values();
    }

    // Produces JSON message to trigger flow overview visualization
    protected ObjectNode flowSummaryMessage(long sid, Set<Device> devices) {
        ObjectNode payload = mapper.createObjectNode();
        ArrayNode paths = mapper.createArrayNode();
        payload.set("paths", paths);

        for (Device device : devices) {
            Map<Link, Integer> counts = getFlowCounts(device.id());
            for (Link link : counts.keySet()) {
                addLinkFlows(link, paths, counts.get(link));
            }
        }
        return envelope("showTraffic", sid, payload);
    }

    private void addLinkFlows(Link link, ArrayNode paths, Integer count) {
        ObjectNode pathNode = mapper.createObjectNode();
        ArrayNode linksNode = mapper.createArrayNode();
        ArrayNode labels = mapper.createArrayNode();
        boolean noFlows = count == null || count == 0;
        pathNode.put("class", noFlows ? "secondary" : "primary");
        pathNode.put("traffic", false);
        pathNode.set("links", linksNode.add(compactLinkString(link)));
        pathNode.set("labels", labels.add(noFlows ? "" : (count.toString() +
                (count == 1 ? " flow" : " flows"))));
        paths.add(pathNode);
    }


    // Produces JSON message to trigger traffic visualization
    protected ObjectNode trafficMessage(long sid, TrafficClass... trafficClasses) {
        ObjectNode payload = mapper.createObjectNode();
        ArrayNode paths = mapper.createArrayNode();
        payload.set("paths", paths);

        // Classify links based on their traffic traffic first...
        Map<LinkKey, BiLink> biLinks = classifyLinkTraffic(trafficClasses);

        // Then separate the links into their respective classes and send them out.
        Map<String, ObjectNode> pathNodes = new HashMap<>();
        for (BiLink biLink : biLinks.values()) {
            boolean hasTraffic = biLink.hasTraffic;
            String tc = (biLink.classes + (hasTraffic ? " animated" : "")).trim();
            ObjectNode pathNode = pathNodes.get(tc);
            if (pathNode == null) {
                pathNode = mapper.createObjectNode()
                        .put("class", tc).put("traffic", hasTraffic);
                pathNode.set("links", mapper.createArrayNode());
                pathNode.set("labels", mapper.createArrayNode());
                pathNodes.put(tc, pathNode);
                paths.add(pathNode);
            }
            ((ArrayNode) pathNode.path("links")).add(compactLinkString(biLink.one));
            ((ArrayNode) pathNode.path("labels")).add(hasTraffic ? formatBytes(biLink.bytes) : "");
        }

        return envelope("showTraffic", sid, payload);
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


    // Adds the link segments (path or tree) associated with the specified
    // connectivity intent
    private void classifyLinks(String type, Map<LinkKey, BiLink> biLinks,
                               boolean showTraffic, Iterable<Link> links) {
        if (links != null) {
            for (Link link : links) {
                BiLink biLink = addLink(biLinks, link);
                if (isInfrastructureEgress(link)) {
                    if (showTraffic) {
                        biLink.addLoad(statService.load(link));
                    }
                    biLink.addClass(type);
                }
            }
        }
    }


    private BiLink addLink(Map<LinkKey, BiLink> biLinks, Link link) {
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


    // Adds the link segments (path or tree) associated with the specified
    // connectivity intent
    protected void addPathTraffic(ArrayNode paths, String type, String trafficType,
                                  Iterable<Link> links) {
        ObjectNode pathNode = mapper.createObjectNode();
        ArrayNode linksNode = mapper.createArrayNode();

        if (links != null) {
            ArrayNode labels = mapper.createArrayNode();
            boolean hasTraffic = false;
            for (Link link : links) {
                if (isInfrastructureEgress(link)) {
                    linksNode.add(compactLinkString(link));
                    Load load = statService.load(link);
                    String label = "";
                    if (load.rate() > 0) {
                        hasTraffic = true;
                        label = formatBytes(load.latest());
                    }
                    labels.add(label);
                }
            }
            pathNode.put("class", hasTraffic ? type + " " + trafficType : type);
            pathNode.put("traffic", hasTraffic);
            pathNode.set("links", linksNode);
            pathNode.set("labels", labels);
            paths.add(pathNode);
        }
    }

    // Poor-mans formatting to get the labels with byte counts looking nice.
    private String formatBytes(long bytes) {
        String unit;
        double value;
        if (bytes > GB) {
            value = bytes / GB;
            unit = GB_UNIT;
        } else if (bytes > MB) {
            value = bytes / MB;
            unit = MB_UNIT;
        } else if (bytes > KB) {
            value = bytes / KB;
            unit = KB_UNIT;
        } else {
            value = bytes;
            unit = B_UNIT;
        }
        DecimalFormat format = new DecimalFormat("#,###.##");
        return format.format(value) + " " + unit;
    }

    // Formats the given number into a string.
    private String format(Number number) {
        DecimalFormat format = new DecimalFormat("#,###");
        return format.format(number);
    }

    private boolean isInfrastructureEgress(Link link) {
        return link.src().elementId() instanceof DeviceId;
    }

    // Produces compact string representation of a link.
    private static String compactLinkString(Link link) {
        return String.format(COMPACT, link.src().elementId(), link.src().port(),
                             link.dst().elementId(), link.dst().port());
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

    // Produces canonical link key, i.e. one that will match link and its inverse.
    private LinkKey canonicalLinkKey(Link link) {
        String sn = link.src().elementId().toString();
        String dn = link.dst().elementId().toString();
        return sn.compareTo(dn) < 0 ?
                linkKey(link.src(), link.dst()) : linkKey(link.dst(), link.src());
    }

    // Representation of link and its inverse and any traffic data.
    private class BiLink {
        public final LinkKey key;
        public final Link one;
        public Link two;
        public boolean hasTraffic = false;
        public long bytes = 0;
        public String classes = "";

        BiLink(LinkKey key, Link link) {
            this.key = key;
            this.one = link;
        }

        void setOther(Link link) {
            this.two = link;
        }

        void addLoad(Load load) {
            if (load != null) {
                this.hasTraffic = hasTraffic || load.rate() > 0;
                this.bytes += load.latest();
            }
        }

        void addClass(String trafficClass) {
            classes = classes + " " + trafficClass;
        }
    }

    // Auxiliary key/value carrier.
    private class Prop {
        public final String key;
        public final String value;

        protected Prop(String key, String value) {
            this.key = key;
            this.value = value;
        }
    }

    // Auxiliary properties separator
    private class Separator extends Prop {
        protected Separator() {
            super("-", "");
        }
    }

    // Auxiliary carrier of data for requesting traffic message.
    protected class TrafficClass {
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

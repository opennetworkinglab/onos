/*
 * Copyright 2015-present Open Networking Laboratory
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
import org.onlab.util.DefaultHashMap;
import org.onosproject.cluster.ClusterEvent;
import org.onosproject.cluster.ControllerNode;
import org.onosproject.cluster.NodeId;
import org.onosproject.core.CoreService;
import org.onosproject.incubator.net.tunnel.OpticalTunnelEndPoint;
import org.onosproject.incubator.net.tunnel.Tunnel;
import org.onosproject.net.Annotated;
import org.onosproject.net.AnnotationKeys;
import org.onosproject.net.Annotations;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DefaultEdgeLink;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.EdgeLink;
import org.onosproject.net.ElementId;
import org.onosproject.net.Host;
import org.onosproject.net.HostId;
import org.onosproject.net.HostLocation;
import org.onosproject.net.Link;
import org.onosproject.net.device.DeviceEvent;
import org.onosproject.net.flow.FlowEntry;
import org.onosproject.net.host.HostEvent;
import org.onosproject.net.link.LinkEvent;
import org.onosproject.net.provider.ProviderId;
import org.onosproject.net.topology.Topology;
import org.onosproject.ui.JsonUtils;
import org.onosproject.ui.UiConnection;
import org.onosproject.ui.UiMessageHandler;
import org.onosproject.ui.impl.topo.util.ServicesBundle;
import org.onosproject.ui.topo.PropertyPanel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static com.google.common.base.Strings.isNullOrEmpty;
import static org.onosproject.net.PortNumber.portNumber;
import static org.onosproject.ui.topo.TopoConstants.CoreButtons;
import static org.onosproject.ui.topo.TopoConstants.Properties;
import static org.onosproject.ui.topo.TopoUtils.compactLinkString;

/**
 * Facility for creating messages bound for the topology viewer.
 */
public abstract class TopologyViewMessageHandlerBase extends UiMessageHandler {

    private static final String NO_GEO_VALUE = "0.0";
    private static final String DASH = "-";

    // nav paths are the view names for hot-link navigation from topo view...
    private static final String DEVICE_NAV_PATH = "device";
    private static final String HOST_NAV_PATH = "host";

    // default to an "add" event...
    private static final DefaultHashMap<ClusterEvent.Type, String> CLUSTER_EVENT =
            new DefaultHashMap<>("addInstance");

    // default to an "update" event...
    private static final DefaultHashMap<DeviceEvent.Type, String> DEVICE_EVENT =
            new DefaultHashMap<>("updateDevice");
    private static final DefaultHashMap<LinkEvent.Type, String> LINK_EVENT =
            new DefaultHashMap<>("updateLink");
    private static final DefaultHashMap<HostEvent.Type, String> HOST_EVENT =
            new DefaultHashMap<>("updateHost");

    // but call out specific events that we care to differentiate...
    static {
        CLUSTER_EVENT.put(ClusterEvent.Type.INSTANCE_REMOVED, "removeInstance");

        DEVICE_EVENT.put(DeviceEvent.Type.DEVICE_ADDED, "addDevice");
        DEVICE_EVENT.put(DeviceEvent.Type.DEVICE_REMOVED, "removeDevice");

        LINK_EVENT.put(LinkEvent.Type.LINK_ADDED, "addLink");
        LINK_EVENT.put(LinkEvent.Type.LINK_REMOVED, "removeLink");

        HOST_EVENT.put(HostEvent.Type.HOST_ADDED, "addHost");
        HOST_EVENT.put(HostEvent.Type.HOST_REMOVED, "removeHost");
        HOST_EVENT.put(HostEvent.Type.HOST_MOVED, "moveHost");
    }

    protected static final Logger log =
            LoggerFactory.getLogger(TopologyViewMessageHandlerBase.class);

    private static final ProviderId PID =
            new ProviderId("core", "org.onosproject.core", true);

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


    protected ServicesBundle services;

    private String version;


    @Override
    public void init(UiConnection connection, ServiceDirectory directory) {
        super.init(connection, directory);
        services = new ServicesBundle(directory);
        setVersionString(directory);
    }

    // Creates a palatable version string to display on the summary panel
    private void setVersionString(ServiceDirectory directory) {
        String ver = directory.get(CoreService.class).version().toString();
        version = ver.replace(".SNAPSHOT", "*").replaceFirst("~.*$", "");
    }

    // Returns the first of the given set of IP addresses as a string.
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
    protected ObjectNode info(String message) {
        return message("info", message);
    }

    // Produces a warning log message event bound to the client.
    protected ObjectNode warning(String message) {
        return message("warning", message);
    }

    // Produces an error log message event bound to the client.
    protected ObjectNode error(String message) {
        return message("error", message);
    }

    // Produces a log message event bound to the client.
    private ObjectNode message(String severity, String message) {
        ObjectNode payload = objectNode()
                .put("severity", severity)
                .put("message", message);

        return JsonUtils.envelope("message", payload);
    }

    // Produces a cluster instance message to the client.
    protected ObjectNode instanceMessage(ClusterEvent event, String msgType) {
        ControllerNode node = event.subject();
        int switchCount = services.mastership().getDevicesOf(node.id()).size();
        ObjectNode payload = objectNode()
                .put("id", node.id().toString())
                .put("ip", node.ip().toString())
                .put("online", services.cluster().getState(node.id()).isActive())
                .put("ready", services.cluster().getState(node.id()).isReady())
                .put("uiAttached", node.equals(services.cluster().getLocalNode()))
                .put("switches", switchCount);

        ArrayNode labels = arrayNode();
        labels.add(node.id().toString());
        labels.add(node.ip().toString());

        // Add labels, props and stuff the payload into envelope.
        payload.set("labels", labels);
        addMetaUi(node.id().toString(), payload);

        String type = msgType != null ? msgType : CLUSTER_EVENT.get(event.type());
        return JsonUtils.envelope(type, payload);
    }

    // Produces a device event message to the client.
    protected ObjectNode deviceMessage(DeviceEvent event) {
        Device device = event.subject();
        String uiType = device.annotations().value(AnnotationKeys.UI_TYPE);
        String devType = uiType != null ? uiType :
                device.type().toString().toLowerCase();
        String name = device.annotations().value(AnnotationKeys.NAME);
        name = isNullOrEmpty(name) ? device.id().toString() : name;

        ObjectNode payload = objectNode()
                .put("id", device.id().toString())
                .put("type", devType)
                .put("online", services.device().isAvailable(device.id()))
                .put("master", master(device.id()));

        payload.set("labels", labels("", name, device.id().toString()));
        payload.set("props", props(device.annotations()));
        addGeoLocation(device, payload);
        addMetaUi(device.id().toString(), payload);

        String type = DEVICE_EVENT.get(event.type());
        return JsonUtils.envelope(type, payload);
    }

    // Produces a link event message to the client.
    protected ObjectNode linkMessage(LinkEvent event) {
        Link link = event.subject();
        ObjectNode payload = objectNode()
                .put("id", compactLinkString(link))
                .put("type", link.type().toString().toLowerCase())
                .put("expected", link.isExpected())
                .put("online", link.state() == Link.State.ACTIVE)
                .put("linkWidth", 1.2)
                .put("src", link.src().deviceId().toString())
                .put("srcPort", link.src().port().toString())
                .put("dst", link.dst().deviceId().toString())
                .put("dstPort", link.dst().port().toString());
        String type = LINK_EVENT.get(event.type());
        return JsonUtils.envelope(type, payload);
    }

    // Produces a host event message to the client.
    protected ObjectNode hostMessage(HostEvent event) {
        Host host = event.subject();
        Host prevHost = event.prevSubject();
        String hostType = host.annotations().value(AnnotationKeys.UI_TYPE);
        String ip = ip(host.ipAddresses());

        ObjectNode payload = objectNode()
                .put("id", host.id().toString())
                .put("type", isNullOrEmpty(hostType) ? "endstation" : hostType)
                .put("ingress", compactLinkString(edgeLink(host, true)))
                .put("egress", compactLinkString(edgeLink(host, false)));

        payload.set("cp", hostConnect(host.location()));
        if (prevHost != null && prevHost.location() != null) {
            payload.set("prevCp", hostConnect(prevHost.location()));
        }
        payload.set("labels", labels(nameForHost(host), ip, host.mac().toString()));
        payload.set("props", props(host.annotations()));
        addGeoLocation(host, payload);
        addMetaUi(host.id().toString(), payload);

        String type = HOST_EVENT.get(event.type());
        return JsonUtils.envelope(type, payload);
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
        NodeId master = services.mastership().getMasterFor(deviceId);
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
        boolean validLat = slat != null && !slat.equals(NO_GEO_VALUE);
        boolean validLng = slng != null && !slng.equals(NO_GEO_VALUE);
        if (validLat && validLng) {
            try {
                double lat = Double.parseDouble(slat);
                double lng = Double.parseDouble(slng);
                ObjectNode loc = objectNode()
                        .put("locType", "geo")
                        .put("latOrY", lat)
                        .put("longOrX", lng);
                payload.set("location", loc);
            } catch (NumberFormatException e) {
                log.warn("Invalid geo data: latitude={}, longitude={}", slat, slng);
            }
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
    protected PropertyPanel summmaryMessage() {
        Topology topology = services.topology().currentTopology();

        return new PropertyPanel("ONOS Summary", "node")
                .addProp(Properties.VERSION, version)
                .addSeparator()
                .addProp(Properties.DEVICES, services.device().getDeviceCount())
                .addProp(Properties.LINKS, topology.linkCount())
                .addProp(Properties.HOSTS, services.host().getHostCount())
                .addProp(Properties.TOPOLOGY_SSCS, topology.clusterCount())
                .addSeparator()
                .addProp(Properties.INTENTS, services.intent().getIntentCount())
                .addProp(Properties.TUNNELS, services.tunnel().tunnelCount())
                .addProp(Properties.FLOWS, services.flow().getFlowRuleCount());
    }

    // Returns property panel model for device details response.
    protected PropertyPanel deviceDetails(DeviceId deviceId) {
        Device device = services.device().getDevice(deviceId);
        Annotations annot = device.annotations();
        String name = annot.value(AnnotationKeys.NAME);
        int portCount = services.device().getPorts(deviceId).size();
        int flowCount = getFlowCount(deviceId);
        int tunnelCount = getTunnelCount(deviceId);

        String title = isNullOrEmpty(name) ? deviceId.toString() : name;
        String typeId = device.type().toString().toLowerCase();

        return new PropertyPanel(title, typeId)
                .navPath(DEVICE_NAV_PATH)
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
                .addButton(CoreButtons.SHOW_GROUP_VIEW)
                .addButton(CoreButtons.SHOW_METER_VIEW);
    }

    protected int getFlowCount(DeviceId deviceId) {
        int count = 0;
        for (FlowEntry flowEntry : services.flow().getFlowEntries(deviceId)) {
            count++;
        }
        return count;
    }

    protected int getTunnelCount(DeviceId deviceId) {
        int count = 0;
        Collection<Tunnel> tunnels = services.tunnel().queryAllTunnels();
        for (Tunnel tunnel : tunnels) {
            //Only OpticalTunnelEndPoint has a device
            if (!(tunnel.src() instanceof OpticalTunnelEndPoint) ||
                    !(tunnel.dst() instanceof OpticalTunnelEndPoint)) {
                continue;
            }

            Optional<ElementId> srcElementId = ((OpticalTunnelEndPoint) tunnel.src()).elementId();
            Optional<ElementId> dstElementId = ((OpticalTunnelEndPoint) tunnel.dst()).elementId();
            if (!srcElementId.isPresent() || !dstElementId.isPresent()) {
                continue;
            }
            DeviceId srcDeviceId = (DeviceId) srcElementId.get();
            DeviceId dstDeviceId = (DeviceId) dstElementId.get();
            if (srcDeviceId.equals(deviceId) || dstDeviceId.equals(deviceId)) {
                count++;
            }
        }
        return count;
    }

    private boolean useDefaultName(String annotName) {
        return isNullOrEmpty(annotName) || DASH.equals(annotName);
    }

    private String nameForHost(Host host) {
        String name = host.annotations().value(AnnotationKeys.NAME);
        return useDefaultName(name) ? ip(host.ipAddresses()) : name;
    }

    // Returns host details response.
    protected PropertyPanel hostDetails(HostId hostId) {
        Host host = services.host().getHost(hostId);
        Annotations annot = host.annotations();
        String type = annot.value(AnnotationKeys.TYPE);
        String vlan = host.vlan().toString();
        String typeId = isNullOrEmpty(type) ? "endstation" : type;

        return new PropertyPanel(nameForHost(host), typeId)
                .navPath(HOST_NAV_PATH)
                .id(hostId.toString())
                .addProp(Properties.MAC, host.mac())
                .addProp(Properties.IP, host.ipAddresses(), "[\\[\\]]")
                .addProp(Properties.VLAN, "-1".equals(vlan) ? "none" : vlan)
                .addSeparator()
                .addProp(Properties.LATITUDE, annot.value(AnnotationKeys.LATITUDE))
                .addProp(Properties.LONGITUDE, annot.value(AnnotationKeys.LONGITUDE));
    }

}

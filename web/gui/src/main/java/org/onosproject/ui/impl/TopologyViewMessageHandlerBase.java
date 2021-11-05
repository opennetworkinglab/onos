/*
 * Copyright 2015-present Open Networking Foundation
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
import com.google.common.collect.ImmutableSet;
import org.onlab.osgi.ServiceDirectory;
import org.onlab.packet.IpAddress;
import org.onlab.packet.VlanId;
import org.onlab.util.DefaultHashMap;
import org.onosproject.cluster.ClusterEvent;
import org.onosproject.cluster.ControllerNode;
import org.onosproject.cluster.NodeId;
import org.onosproject.core.CoreService;
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
import org.onosproject.net.config.NetworkConfigService;
import org.onosproject.net.config.basics.BasicDeviceConfig;
import org.onosproject.net.config.basics.BasicElementConfig;
import org.onosproject.net.config.basics.BasicHostConfig;
import org.onosproject.net.device.DeviceEvent;
import org.onosproject.net.driver.Driver;
import org.onosproject.net.host.HostEvent;
import org.onosproject.net.link.LinkEvent;
import org.onosproject.net.provider.ProviderId;
import org.onosproject.net.topology.Topology;
import org.onosproject.ui.JsonUtils;
import org.onosproject.ui.UiConnection;
import org.onosproject.ui.impl.topo.TopoologyTrafficMessageHandlerAbstract;
import org.onosproject.ui.impl.topo.util.ServicesBundle;
import org.onosproject.ui.lion.LionBundle;
import org.onosproject.ui.topo.PropertyPanel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static com.google.common.base.Strings.isNullOrEmpty;
import static org.onosproject.net.AnnotationKeys.DRIVER;
import static org.onosproject.net.AnnotationKeys.UI_TYPE;
import static org.onosproject.net.PortNumber.portNumber;
import static org.onosproject.net.config.basics.BasicElementConfig.LOC_TYPE_GEO;
import static org.onosproject.net.config.basics.BasicElementConfig.LOC_TYPE_GRID;
import static org.onosproject.ui.topo.TopoConstants.CoreButtons;
import static org.onosproject.ui.topo.TopoConstants.Properties.*;
import static org.onosproject.ui.topo.TopoUtils.compactLinkString;

/**
 * Facility for creating messages bound for the topology viewer.
 */
public abstract class TopologyViewMessageHandlerBase extends TopoologyTrafficMessageHandlerAbstract {

    private static final String NO_GEO_VALUE = "0.0";
    private static final String DASH = "-";
    private static final String SLASH = " / ";

    // nav paths are the view names for hot-link navigation from topo view...
    private static final String DEVICE_NAV_PATH = "device";
    private static final String HOST_NAV_PATH = "host";

    // link panel label keys
    private static final String LPL_FRIENDLY = "lp_label_friendly";
    private static final String LPL_A_TYPE = "lp_label_a_type";
    private static final String LPL_A_ID = "lp_label_a_id";
    private static final String LPL_A_FRIENDLY = "lp_label_a_friendly";
    private static final String LPL_A_PORT = "lp_label_a_port";
    private static final String LPL_B_TYPE = "lp_label_b_type";
    private static final String LPL_B_ID = "lp_label_b_id";
    private static final String LPL_B_FRIENDLY = "lp_label_b_friendly";
    private static final String LPL_B_PORT = "lp_label_b_port";
    private static final String LPL_A2B = "lp_label_a2b";
    private static final String LPL_B2A = "lp_label_b2a";
    private static final String LPV_NO_LINK = "lp_value_no_link";

    // other Lion keys
    private static final String HOST = "host";
    private static final String DEVICE = "device";
    private static final String EXPECTED = "expected";
    private static final String NOT_EXPECTED = "not_expected";

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

    private static final DefaultHashMap<Device.Type, String> DEVICE_GLYPHS =
            new DefaultHashMap<>("m_unknown");

    static {
        DEVICE_GLYPHS.put(Device.Type.SWITCH, "m_switch");
        DEVICE_GLYPHS.put(Device.Type.ROUTER, "m_router");
        DEVICE_GLYPHS.put(Device.Type.ROADM, "m_roadm");
        DEVICE_GLYPHS.put(Device.Type.OLS, "m_roadm");
        DEVICE_GLYPHS.put(Device.Type.OTN, "m_otn");
        DEVICE_GLYPHS.put(Device.Type.TERMINAL_DEVICE, "m_otn");
        DEVICE_GLYPHS.put(Device.Type.ROADM_OTN, "m_roadm_otn");
        DEVICE_GLYPHS.put(Device.Type.BALANCER, "m_balancer");
        DEVICE_GLYPHS.put(Device.Type.IPS, "m_ips");
        DEVICE_GLYPHS.put(Device.Type.IDS, "m_ids");
        DEVICE_GLYPHS.put(Device.Type.CONTROLLER, "m_controller");
        DEVICE_GLYPHS.put(Device.Type.VIRTUAL, "m_virtual");
        DEVICE_GLYPHS.put(Device.Type.FIBER_SWITCH, "m_fiberSwitch");
        DEVICE_GLYPHS.put(Device.Type.MICROWAVE, "m_microwave");
        DEVICE_GLYPHS.put(Device.Type.OLT, "m_olt");
        DEVICE_GLYPHS.put(Device.Type.ONU, "m_onu");
        DEVICE_GLYPHS.put(Device.Type.OPTICAL_AMPLIFIER, "unknown"); // TODO glyph needed
        DEVICE_GLYPHS.put(Device.Type.OTHER, "m_other");
        DEVICE_GLYPHS.put(Device.Type.SERVER, "m_endpoint");
    }

    private static final String DEFAULT_HOST_GLYPH = "m_endstation";
    private static final String LINK_GLYPH = "m_ports";


    static final Logger log =
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

    /**
     * Clears any meta-ui information.
     */
    public static void clearMetaUi() {
        metaUi.clear();
    }

    private static final String LION_TOPO = "core.view.Topo";

    private static final Set<String> REQ_LION_BUNDLES = ImmutableSet.of(
            LION_TOPO
    );

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

    @Override
    public Set<String> requiredLionBundles() {
        return REQ_LION_BUNDLES;
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
        IpAddress nodeIp = node.ip();
        int switchCount = services.mastership().getDevicesOf(node.id()).size();
        ObjectNode payload = objectNode()
                .put("id", node.id().toString())
                .put("ip", nodeIp != null ? nodeIp.toString() : node.host())
                .put("online", services.cluster().getState(node.id()).isActive())
                .put("ready", services.cluster().getState(node.id()).isReady())
                .put("uiAttached", node.equals(services.cluster().getLocalNode()))
                .put("switches", switchCount);

        ArrayNode labels = arrayNode();
        labels.add(node.id().toString());
        labels.add(nodeIp != null ? nodeIp.toString() : node.host());

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
        String driverName = device.annotations().value(DRIVER);
        Driver driver = driverName == null ? null : services.driver().getDriver(driverName);
        String devType = uiType != null ? uiType :
                (driver != null ? driver.getProperty(AnnotationKeys.UI_TYPE) : null);
        if (devType == null) {
            devType = device.type().toString().toLowerCase();
        }
        String name = device.annotations().value(AnnotationKeys.NAME);
        name = isNullOrEmpty(name) ? device.id().toString() : name;

        ObjectNode payload = objectNode()
                .put("id", device.id().toString())
                .put("type", devType)
                .put("online", services.device().isAvailable(device.id()))
                .put("master", master(device.id()));

        payload.set("labels", labels("", name, device.id().toString()));
        payload.set("props", props(device.annotations()));

        BasicDeviceConfig cfg = get(NetworkConfigService.class)
                .getConfig(device.id(), BasicDeviceConfig.class);
        if (!addLocation(cfg, payload)) {
            addMetaUi(device.id().toString(), payload);
        }

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
        String connectionType = host.annotations().value(AnnotationKeys.CONNECTION_TYPE);
        String ip = ip(host.ipAddresses());

        ObjectNode payload = objectNode()
                .put("id", host.id().toString())
                .put("type", isNullOrEmpty(hostType) ? "endstation" : hostType)
                .put("connectionType", isNullOrEmpty(connectionType) ? "wired" : connectionType);

        // set most recent connect point (and previous if we know it)
        payload.set("cp", hostConnect(host.location()));
        if (prevHost != null && prevHost.location() != null) {
            payload.set("prevCp", hostConnect(prevHost.location()));
        }

        // set ALL connect points
        addAllCps(host.locations(), payload);

        payload.set("labels", labels(nameForHost(host), ip, host.mac().toString(), ""));
        payload.set("props", props(host.annotations()));

        BasicHostConfig cfg = get(NetworkConfigService.class)
                .getConfig(host.id(), BasicHostConfig.class);
        if (!addLocation(cfg, payload)) {
            addMetaUi(host.id().toString(), payload);
        }

        String type = HOST_EVENT.get(event.type());
        return JsonUtils.envelope(type, payload);
    }

    private void addAllCps(Set<HostLocation> locations, ObjectNode payload) {
        ArrayNode cps = arrayNode();
        locations.forEach(loc -> cps.add(hostConnect(loc)));
        payload.set("allCps", cps);
    }

    // Encodes the specified host location into a JSON object.
    private ObjectNode hostConnect(HostLocation location) {
        return objectNode()
                .put("device", location.deviceId().toString())
                .put("port", location.port().toString());
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

    private boolean addLocation(BasicElementConfig cfg, ObjectNode payload) {
        if (cfg != null) {
            String locType = cfg.locType();
            boolean isGeo = Objects.equals(locType, LOC_TYPE_GEO);
            boolean isGrid = Objects.equals(locType, LOC_TYPE_GRID);
            if (isGeo || isGrid) {
                try {
                    ObjectNode loc = objectNode()
                            .put("locType", locType)
                            .put("latOrY", isGeo ? cfg.latitude() : cfg.gridY())
                            .put("longOrX", isGeo ? cfg.longitude() : cfg.gridX());
                    payload.set("location", loc);
                    return true;
                } catch (NumberFormatException e) {
                    log.warn("Invalid location data: {}", cfg);
                }
            }
        }
        return false;
    }

    // Updates meta UI information for the specified object.
    protected void updateMetaUi(ObjectNode payload) {
        metaUi.put(JsonUtils.string(payload, "id"),
                   JsonUtils.node(payload, "memento"));
    }


    // -----------------------------------------------------------------------
    // Create models of the data to return, that overlays can adjust / augment

    private String lookupGlyph(Device device) {
        String uiType = device.annotations().value(UI_TYPE);
        if (uiType != null && !uiType.equalsIgnoreCase("undefined")) {
            return uiType;
        } else {
            return DEVICE_GLYPHS.get(device.type());
        }
    }


    // Returns property panel model for summary response.
    protected PropertyPanel summmaryMessage() {
        // chose NOT to add debug messages, since this is called every few seconds
        Topology topology = services.topology().currentTopology();
        LionBundle lion = getLionBundle(LION_TOPO);
        String panelTitle = lion.getSafe("title_panel_summary");

        return new PropertyPanel(panelTitle, "bird")
                .addProp(VERSION, lion.getSafe(VERSION), version)
                .addSeparator()
                .addProp(DEVICES, lion.getSafe(DEVICES), services.device().getDeviceCount())
                .addProp(LINKS, lion.getSafe(LINKS), topology.linkCount())
                .addProp(HOSTS, lion.getSafe(HOSTS), services.host().getHostCount())
                .addProp(TOPOLOGY_SSCS, lion.getSafe(TOPOLOGY_SSCS), topology.clusterCount())
                .addSeparator()
                .addProp(INTENTS, lion.getSafe(INTENTS), services.intent().getIntentCount())
                .addProp(FLOWS, lion.getSafe(FLOWS), services.flow().getFlowRuleCount());
    }


    private String friendlyDevice(DeviceId deviceId) {
        Device device = services.device().getDevice(deviceId);
        Annotations annot = device.annotations();
        String name = annot.value(AnnotationKeys.NAME);
        return isNullOrEmpty(name) ? deviceId.toString() : name;
    }

    // Generates a property panel model for device details response
    protected PropertyPanel deviceDetails(DeviceId deviceId) {
        log.debug("generate prop panel data for device {}", deviceId);
        Device device = services.device().getDevice(deviceId);
        Annotations annot = device.annotations();
        String proto = annot.value(AnnotationKeys.PROTOCOL);
        String title = friendlyDevice(deviceId);
        LionBundle lion = getLionBundle(LION_TOPO);

        PropertyPanel pp = new PropertyPanel(title, lookupGlyph(device))
                .navPath(DEVICE_NAV_PATH)
                .id(deviceId.toString());
        addDeviceBasicProps(pp, deviceId, device, proto, lion);
        addLocationProps(pp, annot, lion);
        addDeviceCountStats(pp, deviceId, lion);
        addDeviceCoreButtons(pp);
        return pp;
    }

    private void addDeviceBasicProps(PropertyPanel pp, DeviceId deviceId,
                                     Device device, String proto, LionBundle lion) {
        pp.addProp(URI, lion.getSafe(URI), deviceId.toString())
                .addProp(VENDOR, lion.getSafe(VENDOR), device.manufacturer())
                .addProp(HW_VERSION, lion.getSafe(HW_VERSION), device.hwVersion())
                .addProp(SW_VERSION, lion.getSafe(SW_VERSION), device.swVersion())
                .addProp(SERIAL_NUMBER, lion.getSafe(SERIAL_NUMBER), device.serialNumber())
                .addProp(PROTOCOL, lion.getSafe(PROTOCOL), proto)
                .addSeparator();
    }

    // only add location properties if we have them
    private void addLocationProps(PropertyPanel pp, Annotations annot,
                                  LionBundle lion) {
        String slat = annot.value(AnnotationKeys.LATITUDE);
        String slng = annot.value(AnnotationKeys.LONGITUDE);
        String sgrY = annot.value(AnnotationKeys.GRID_Y);
        String sgrX = annot.value(AnnotationKeys.GRID_X);

        boolean validLat = slat != null && !slat.equals(NO_GEO_VALUE);
        boolean validLng = slng != null && !slng.equals(NO_GEO_VALUE);
        if (validLat && validLng) {
            pp.addProp(LATITUDE, lion.getSafe(LATITUDE), slat)
                    .addProp(LONGITUDE, lion.getSafe(LONGITUDE), slng)
                    .addSeparator();

        } else if (sgrY != null && sgrX != null) {
            pp.addProp(GRID_Y, lion.getSafe(GRID_Y), sgrY)
                    .addProp(GRID_X, lion.getSafe(GRID_X), sgrX)
                    .addSeparator();
        }
        // else, no location
    }

    private void addDeviceCountStats(PropertyPanel pp, DeviceId deviceId, LionBundle lion) {
        int portCount = services.device().getPorts(deviceId).size();
        int flowCount = getFlowCount(deviceId);
        int tunnelCount = getTunnelCount(deviceId);

        pp.addProp(PORTS, lion.getSafe(PORTS), portCount)
                .addProp(FLOWS, lion.getSafe(FLOWS), flowCount)
                .addProp(TUNNELS, lion.getSafe(TUNNELS), tunnelCount);
    }

    private void addDeviceCoreButtons(PropertyPanel pp) {
        pp.addButton(CoreButtons.SHOW_DEVICE_VIEW)
                .addButton(CoreButtons.SHOW_FLOW_VIEW)
                .addButton(CoreButtons.SHOW_PORT_VIEW)
                .addButton(CoreButtons.SHOW_GROUP_VIEW)
                .addButton(CoreButtons.SHOW_METER_VIEW);
    }

    protected int getFlowCount(DeviceId deviceId) {
        return services.flow().getFlowRuleCount(deviceId);
    }

    @Deprecated
    protected int getTunnelCount(DeviceId deviceId) {
        return 0;
    }

    private boolean useDefaultName(String annotName) {
        return isNullOrEmpty(annotName) || DASH.equals(annotName);
    }

    private String nameForHost(Host host) {
        String name = host.annotations().value(AnnotationKeys.NAME);
        return useDefaultName(name) ? ip(host.ipAddresses()) : name;
    }

    private String glyphForHost(Annotations annot) {
        String uiType = annot.value(AnnotationKeys.UI_TYPE);
        return isNullOrEmpty(uiType) ? DEFAULT_HOST_GLYPH : "m_" + uiType;
    }

    // Generates a property panel model for a host details response
    protected PropertyPanel hostDetails(HostId hostId) {
        log.debug("generate prop panel data for host {}", hostId);
        Host host = services.host().getHost(hostId);
        Annotations annot = host.annotations();
        String glyphId = glyphForHost(annot);
        LionBundle lion = getLionBundle(LION_TOPO);

        PropertyPanel pp = new PropertyPanel(nameForHost(host), glyphId)
                .navPath(HOST_NAV_PATH)
                .id(hostId.toString());
        addHostBasicProps(pp, host, lion);
        addLocationProps(pp, annot, lion);
        return pp;
    }

    private void addHostBasicProps(PropertyPanel pp, Host host, LionBundle lion) {
        pp.addProp(LPL_FRIENDLY, lion.getSafe(LPL_FRIENDLY), nameForHost(host))
                .addProp(MAC, lion.getSafe(MAC), host.mac())
                .addProp(IP, lion.getSafe(IP), host.ipAddresses(), "[\\[\\]]")
                .addProp(VLAN, lion.getSafe(VLAN), displayVlan(host.vlan(), lion))
                .addSeparator();
    }

    private String displayVlan(VlanId vlan, LionBundle lion) {
        return VlanId.NONE.equals(vlan) ? lion.getSafe(VLAN_NONE) : vlan.toString();
    }

    // Generates a property panel model for a link details response (edge-link)
    protected PropertyPanel edgeLinkDetails(HostId hid, ConnectPoint cp) {
        log.debug("generate prop panel data for edgelink {} {}", hid, cp);
        LionBundle lion = getLionBundle(LION_TOPO);
        String title = lion.getSafe("title_edge_link");

        PropertyPanel pp = new PropertyPanel(title, LINK_GLYPH);
        addLinkHostProps(pp, hid, lion);
        addLinkCpBProps(pp, cp, lion);
        return pp;
    }

    // Generates a property panel model for a link details response (infra-link)
    protected PropertyPanel infraLinkDetails(ConnectPoint cpA, ConnectPoint cpB) {
        log.debug("generate prop panel data for infralink {} {}", cpA, cpB);
        LionBundle lion = getLionBundle(LION_TOPO);
        String title = lion.getSafe("title_infra_link");

        PropertyPanel pp = new PropertyPanel(title, LINK_GLYPH);
        addLinkCpAProps(pp, cpA, lion);
        addLinkCpBProps(pp, cpB, lion);
        addLinkBackingProps(pp, cpA, cpB, lion);
        return pp;
    }

    private void addLinkHostProps(PropertyPanel pp, HostId hostId, LionBundle lion) {
        Host host = services.host().getHost(hostId);

        pp.addProp(LPL_A_TYPE, lion.getSafe(LPL_A_TYPE), lion.getSafe(HOST))
                .addProp(LPL_A_ID, lion.getSafe(LPL_A_ID), hostId.toString())
                .addProp(LPL_A_FRIENDLY, lion.getSafe(LPL_A_FRIENDLY), nameForHost(host))
                .addSeparator();
    }

    private void addLinkCpAProps(PropertyPanel pp, ConnectPoint cp, LionBundle lion) {
        DeviceId did = cp.deviceId();

        pp.addProp(LPL_A_TYPE, lion.getSafe(LPL_A_TYPE), lion.getSafe(DEVICE))
                .addProp(LPL_A_ID, lion.getSafe(LPL_A_ID), did.toString())
                .addProp(LPL_A_FRIENDLY, lion.getSafe(LPL_A_FRIENDLY), friendlyDevice(did))
                .addProp(LPL_A_PORT, lion.getSafe(LPL_A_PORT), cp.port().toString())
                .addSeparator();
    }

    private void addLinkCpBProps(PropertyPanel pp, ConnectPoint cp, LionBundle lion) {
        DeviceId did = cp.deviceId();

        pp.addProp(LPL_B_TYPE, lion.getSafe(LPL_B_TYPE), lion.getSafe(DEVICE))
                .addProp(LPL_B_ID, lion.getSafe(LPL_B_ID), did.toString())
                .addProp(LPL_B_FRIENDLY, lion.getSafe(LPL_B_FRIENDLY), friendlyDevice(did))
                .addProp(LPL_B_PORT, lion.getSafe(LPL_B_PORT), cp.port().toString())
                .addSeparator();
    }

    private void addLinkBackingProps(PropertyPanel pp, ConnectPoint cpA,
                                     ConnectPoint cpB, LionBundle lion) {
        Link a2b = services.link().getLink(cpA, cpB);
        Link b2a = services.link().getLink(cpB, cpA);

        pp.addProp(LPL_A2B, lion.getSafe(LPL_A2B), linkPropString(a2b, lion))
                .addProp(LPL_B2A, lion.getSafe(LPL_B2A), linkPropString(b2a, lion));
    }

    private String linkPropString(Link link, LionBundle lion) {
        if (link == null) {
            return lion.getSafe(LPV_NO_LINK);
        }
        return lion.getSafe(link.type()) + SLASH +
                lion.getSafe(link.state()) + SLASH +
                lion.getSafe(link.isExpected() ? EXPECTED : NOT_EXPECTED);
    }
}

/*
 * Copyright 2017-present Open Networking Foundation
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
package org.onosproject.openstacknetworkingui;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.collect.Streams;
import org.apache.commons.io.IOUtils;
import org.onlab.osgi.ServiceDirectory;
import org.onlab.util.DefaultHashMap;
import org.onosproject.cluster.ClusterService;
import org.onosproject.cluster.NodeId;
import org.onosproject.mastership.MastershipService;
import org.onosproject.net.AnnotationKeys;
import org.onosproject.net.Annotations;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Element;
import org.onosproject.net.Host;
import org.onosproject.net.HostId;
import org.onosproject.net.Path;
import org.onosproject.net.config.NetworkConfigService;
import org.onosproject.net.config.basics.BasicDeviceConfig;
import org.onosproject.net.config.basics.BasicElementConfig;
import org.onosproject.net.device.DeviceEvent;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.driver.Driver;
import org.onosproject.net.driver.DriverService;
import org.onosproject.net.host.HostService;
import org.onosproject.net.topology.PathService;
import org.onosproject.ui.JsonUtils;
import org.onosproject.ui.RequestHandler;
import org.onosproject.ui.UiConnection;
import org.onosproject.ui.UiMessageHandler;
import org.onosproject.ui.topo.Highlights;
import org.onosproject.ui.topo.HostHighlight;
import org.onosproject.ui.topo.NodeBadge;
import org.onosproject.ui.topo.NodeBadge.Status;
import org.onosproject.ui.topo.TopoJson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static com.google.common.base.Strings.isNullOrEmpty;
import static org.onosproject.net.AnnotationKeys.DRIVER;
import static org.onosproject.net.DefaultEdgeLink.createEdgeLink;
import static org.onosproject.net.Device.Type.SWITCH;
import static org.onosproject.net.config.basics.BasicElementConfig.LOC_TYPE_GEO;
import static org.onosproject.net.config.basics.BasicElementConfig.LOC_TYPE_GRID;
import static org.onosproject.net.device.DeviceEvent.Type.DEVICE_REMOVED;

/**
 * OpenStack Networking UI message handler.
 */
public class OpenstackNetworkingUiMessageHandler extends UiMessageHandler {

    private static final String OPENSTACK_NETWORKING_UI_START = "openstackNetworkingUiStart";
    private static final String OPENSTACK_NETWORKING_UI_UPDATE = "openstackNetworkingUiUpdate";
    private static final String OPENSTACK_NETWORKING_UI_STOP = "openstackNetworkingUiStop";
    private static final String ANNOTATION_NETWORK_ID = "networkId";
    private static final String FLOW_TRACE_REQUEST = "flowTraceRequest";
    private static final String SRC_IP = "srcIp";
    private static final String DST_IP = "dstIp";
    private static final String ANNOTATION_SEGMENT_ID = "segId";
    private static final String AUTHORIZATION = "Authorization";
    private static final String COMMAND = "command";
    private static final String FLOW_TRACE = "flowtrace";
    private static final String REVERSE = "reverse";
    private static final String TRANSACTION_ID = "transaction_id";
    private static final String TRANSACTION_VALUE = "sona";
    private static final String APP_REST_URL = "app_rest_url";
    private static final String MATCHING_FIELDS = "matchingfields";
    private static final String SOURCE_IP = "source_ip";
    private static final String DESTINATION_IP = "destination_ip";
    private static final String TO_GATEWAY = "to_gateway";
    private static final String IP_PROTOCOL = "ip_protocol";
    private static final String HTTP = "http://";
    private static final String OPENSTACK_NETWORKING_UI_RESULT = ":8181/onos/openstacknetworkingui/result";

    private static final String ID = "id";
    private static final String MODE = "mode";
    private static final String MOUSE = "mouse";

    private enum Mode { IDLE, MOUSE }

    private final Logger log = LoggerFactory.getLogger(getClass());

    private DeviceService deviceService;
    private HostService hostService;
    private PathService pathService;
    private ClusterService clusterService;
    private DriverService driverService;
    private MastershipService mastershipService;
    private String restUrl;
    private String restAuthInfo;
    private Mode currentMode = Mode.IDLE;
    private Element elementOfNote;
    private final Client client = ClientBuilder.newClient();
    private static Map<String, ObjectNode> metaUi = new ConcurrentHashMap<>();
    private static final DefaultHashMap<DeviceEvent.Type, String> DEVICE_EVENT =
            new DefaultHashMap<>("updateDevice");

    static {
        DEVICE_EVENT.put(DEVICE_REMOVED, "removeDevice");
    }

    @Override
    public void init(UiConnection connection, ServiceDirectory directory) {
        super.init(connection, directory);
        deviceService = directory.get(DeviceService.class);
        hostService = directory.get(HostService.class);
        pathService = directory.get(PathService.class);
        clusterService = directory.get(ClusterService.class);
        driverService = directory.get((DriverService.class));
        mastershipService = directory.get(MastershipService.class);

        // Removes non switch devices such as an ovsdb device
        removeNonSwitchDevices();
    }

    private void removeNonSwitchDevices() {
        Streams.stream(deviceService.getAvailableDevices())
                .filter(device -> device.type() != SWITCH)
                .forEach(device -> sendMessage(deviceMessage(new DeviceEvent(DEVICE_REMOVED, device))));
    }

    // Produces a device event message to the client.
    protected ObjectNode deviceMessage(DeviceEvent event) {
        Device device = event.subject();
        String uiType = device.annotations().value(AnnotationKeys.UI_TYPE);
        String driverName = device.annotations().value(DRIVER);
        Driver driver = driverName == null ? null : driverService.getDriver(driverName);
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
                .put("online", deviceService.isAvailable(device.id()))
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

    // Returns the name of the master node for the specified device id.
    private String master(DeviceId deviceId) {
        NodeId master = mastershipService.getMasterFor(deviceId);
        return master != null ? master.toString() : "";
    }

    // Encodes the specified list of labels a JSON array.
    private ArrayNode labels(String... labels) {
        ArrayNode json = arrayNode();
        for (String label : labels) {
            json.add(label);
        }
        return json;
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

    // Adds meta UI information for the specified object.
    private void addMetaUi(String id, ObjectNode payload) {
        ObjectNode meta = metaUi.get(id);
        if (meta != null) {
            payload.set("metaUi", meta);
        }
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
    @Override
    protected Collection<RequestHandler> createRequestHandlers() {
        return ImmutableSet.of(
                new DisplayStartHandler(),
                new DisplayUpdateHandler(),
                new DisplayStopHandler(),
                new FlowTraceRequestHandler()
        );
    }

    public void setRestUrl(String ipAddress) {
        restUrl = "http://" + ipAddress + ":8000/trace_request";
    }

    public String restUrl() {
        return restUrl;
    }

    public void setRestAuthInfo(String id, String password) {
        restAuthInfo = Base64.getEncoder().encodeToString(id.concat(":").concat(password).getBytes());
    }

    public String restAuthInfo() {
        return restAuthInfo;
    }

    private final class DisplayStartHandler extends RequestHandler {

        public DisplayStartHandler() {
            super(OPENSTACK_NETWORKING_UI_START);
        }

        @Override
        public void process(ObjectNode payload) {
            String mode = string(payload, MODE);

            log.debug("Start Display: mode [{}]", mode);
            clearState();
            clearForMode();

            switch (mode) {
                case MOUSE:
                    currentMode = Mode.MOUSE;
                    sendMouseData();
                    break;

                default:
                    currentMode = Mode.IDLE;
                    break;
            }
        }
    }

    private final class FlowTraceRequestHandler extends RequestHandler {
        public FlowTraceRequestHandler() {
            super(FLOW_TRACE_REQUEST);
        }

        @Override
        public void process(ObjectNode payload) {
            String srcIp = string(payload, SRC_IP);
            String dstIp = string(payload, DST_IP);
            log.debug("SendEvent called with src IP: {}, dst IP: {}", srcIp, dstIp);

            ObjectNode objectNode = getFlowTraceRequestAsJson(srcIp, dstIp);
            InputStream byteArrayInputStream
                    = new ByteArrayInputStream(objectNode.toString().getBytes());

            Invocation.Builder builder = getClientBuilder(restUrl);

            if (builder == null) {
                log.error("Fail to get the client builder for the trace from {} to {}", srcIp, dstIp);
                return;
            }

            try {
                Response response = builder.header(AUTHORIZATION, restAuthInfo.toString())
                        .post(Entity.entity(IOUtils.toString(byteArrayInputStream, StandardCharsets.UTF_8),
                                MediaType.APPLICATION_JSON_TYPE));

                log.debug("Response from server: {}", response);

                if (response.getStatus() != 200) {
                    log.error("FlowTraceRequest failed because of {}", response);
                }

            } catch (IOException e) {
                log.error("Exception occured because of {}", e.toString());
            }

        }
    }

    private ObjectNode getFlowTraceRequestAsJson(String srcIp, String dstIp) {
        ObjectMapper mapper = new ObjectMapper();
        String controllerUrl = HTTP + clusterService.getLocalNode().ip()
                + OPENSTACK_NETWORKING_UI_RESULT;

        ObjectNode objectNode = mapper.createObjectNode();

        objectNode.put(COMMAND, FLOW_TRACE)
                .put(REVERSE, false)
                .put(TRANSACTION_ID, TRANSACTION_VALUE)
                .put(APP_REST_URL, controllerUrl);

        if (srcIp.equals(dstIp)) {
            objectNode.putObject(MATCHING_FIELDS)
                    .put(SOURCE_IP, srcIp)
                    .put(DESTINATION_IP, dstIp)
                    .put(TO_GATEWAY, true)
                    .put(IP_PROTOCOL, 1);

        } else {
            objectNode.putObject(MATCHING_FIELDS)
                    .put(SOURCE_IP, srcIp)
                    .put(DESTINATION_IP, dstIp);
        }
        return  objectNode;
    }

    private Invocation.Builder getClientBuilder(String url) {
        if (Strings.isNullOrEmpty(url)) {
            log.warn("URL in not set");
            return null;
        }

        WebTarget wt = client.target(url);

        return wt.request(MediaType.APPLICATION_JSON_TYPE);
    }

    private final class DisplayUpdateHandler extends RequestHandler {
        public DisplayUpdateHandler() {
            super(OPENSTACK_NETWORKING_UI_UPDATE);
        }

        @Override
        public void process(ObjectNode payload) {
            String id = string(payload, ID);
            log.debug("Update Display: id [{}]", id);
            if (!Strings.isNullOrEmpty(id)) {
                updateForMode(id);
            } else {
                clearForMode();
            }
        }
    }

    private final class DisplayStopHandler extends RequestHandler {
        public DisplayStopHandler() {
            super(OPENSTACK_NETWORKING_UI_STOP);
        }

        @Override
        public void process(ObjectNode payload) {
            log.debug("Stop Display");
            clearState();
            clearForMode();
        }
    }

    // === ------------

    private void clearState() {
        currentMode = Mode.IDLE;
        elementOfNote = null;
    }

    private void updateForMode(String id) {

        try {
            HostId hid = HostId.hostId(id);
            elementOfNote = hostService.getHost(hid);

        } catch (Exception e) {
            try {
                DeviceId did = DeviceId.deviceId(id);
                elementOfNote = deviceService.getDevice(did);

            } catch (Exception e2) {
                log.debug("Unable to process ID [{}]", id);
                elementOfNote = null;
            }
        }

        switch (currentMode) {
            case MOUSE:
                sendMouseData();
                break;

            default:
                break;
        }

    }

    private void clearForMode() {
        sendHighlights(new Highlights());
    }

    private void sendHighlights(Highlights highlights) {
        sendMessage(TopoJson.highlightsMessage(highlights));
    }

    public void sendMessagetoUi(String type, ObjectNode payload) {
        sendMessage(JsonUtils.envelope(type, payload));
    }

    private int getVni(Host host) {
        String vni = host.annotations().value(ANNOTATION_SEGMENT_ID);

        return vni == null ? 0 : Integer.valueOf(vni).intValue();
    }

    private void sendMouseData() {
        Highlights highlights = new Highlights();

        if (elementOfNote != null && elementOfNote instanceof Device) {
            DeviceId deviceId = (DeviceId) elementOfNote.id();

            List<OpenstackLink> edgeLinks = edgeLinks(deviceId);

            edgeLinks.forEach(edgeLink -> {
                highlights.add(edgeLink.highlight(OpenstackLink.RequestType.DEVICE_SELECTED));
            });

            hostService.getConnectedHosts(deviceId).forEach(host -> {
                HostHighlight hostHighlight = new HostHighlight(host.id().toString());
                hostHighlight.setBadge(createBadge(getVni(host)));
                highlights.add(hostHighlight);
            });

            sendHighlights(highlights);

        } else if (elementOfNote != null && elementOfNote instanceof Host) {

            HostId hostId = HostId.hostId(elementOfNote.id().toString());
            if (!hostMadeFromOpenstack(hostId)) {
                return;
            }

            List<OpenstackLink> openstackLinks = linksInSameNetwork(hostId);

            openstackLinks.forEach(openstackLink -> {
                highlights.add(openstackLink.highlight(OpenstackLink.RequestType.HOST_SELECTED));
            });

            hostHighlightsInSameNetwork(hostId).forEach(highlights::add);

            sendHighlights(highlights);

        }
    }

    private boolean hostMadeFromOpenstack(HostId hostId) {
        return hostService.getHost(hostId).annotations()
                .value(ANNOTATION_NETWORK_ID) == null ? false : true;
    }

    private String networkId(HostId hostId) {
        return hostService.getHost(hostId).annotations().value(ANNOTATION_NETWORK_ID);
    }

    private Set<HostHighlight> hostHighlightsInSameNetwork(HostId hostId) {

        Set<HostHighlight> hostHighlights = Sets.newHashSet();
        Streams.stream(hostService.getHosts())
                .filter(host -> isHostInSameNetwork(host, networkId(hostId)))
                .forEach(host -> {
                    HostHighlight hostHighlight = new HostHighlight(host.id().toString());
                    hostHighlight.setBadge(createBadge(getVni(host)));
                    hostHighlights.add(hostHighlight);
                });

        return hostHighlights;
    }

    private List<OpenstackLink> edgeLinks(DeviceId deviceId) {
        OpenstackLinkMap openstackLinkMap = new OpenstackLinkMap();

        hostService.getConnectedHosts(deviceId).forEach(host -> {
            openstackLinkMap.add(createEdgeLink(host, true));
            openstackLinkMap.add(createEdgeLink(host, false));
        });

        List<OpenstackLink> edgeLinks = Lists.newArrayList();

        openstackLinkMap.biLinks().forEach(edgeLinks::add);

        return edgeLinks;
    }

    private List<OpenstackLink> linksInSameNetwork(HostId hostId) {
        OpenstackLinkMap linkMap = new OpenstackLinkMap();

        Streams.stream(hostService.getHosts())
                .filter(host -> isHostInSameNetwork(host, networkId(hostId)))
                .forEach(host -> {
                    linkMap.add(createEdgeLink(host, true));
                    linkMap.add(createEdgeLink(host, false));

                    Set<Path> paths = pathService.getPaths(hostId,
                            host.id());

                    if (!paths.isEmpty()) {
                        paths.forEach(path -> path.links().forEach(linkMap::add));
                    }
                });

        List<OpenstackLink> openstackLinks = Lists.newArrayList();

        linkMap.biLinks().forEach(openstackLinks::add);

        return openstackLinks;
    }

    private boolean isHostInSameNetwork(Host host, String networkId) {
        return hostService.getHost(host.id()).annotations()
                .value(ANNOTATION_NETWORK_ID).equals(networkId);
    }

    private NodeBadge createBadge(int n) {
        return NodeBadge.number(Status.INFO, n, "Openstack Node");
    }
}

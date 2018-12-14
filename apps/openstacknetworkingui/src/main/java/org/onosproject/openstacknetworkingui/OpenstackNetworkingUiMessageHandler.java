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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Charsets;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.collect.Streams;
import org.apache.sshd.client.SshClient;
import org.apache.sshd.client.channel.ClientChannel;
import org.apache.sshd.client.channel.ClientChannelEvent;
import org.apache.sshd.client.future.OpenFuture;
import org.apache.sshd.client.session.ClientSession;
import org.apache.sshd.common.util.io.NoCloseInputStream;
import org.onlab.osgi.ServiceDirectory;
import org.onlab.packet.IpAddress;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Element;
import org.onosproject.net.Host;
import org.onosproject.net.HostId;
import org.onosproject.net.Path;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.host.HostService;
import org.onosproject.net.topology.PathService;
import org.onosproject.openstacknetworking.api.InstancePort;
import org.onosproject.openstacknetworking.api.InstancePortService;
import org.onosproject.openstacknetworking.api.OpenstackNetwork.Type;
import org.onosproject.openstacknetworking.api.OpenstackNetworkService;
import org.onosproject.openstacknode.api.OpenstackNode;
import org.onosproject.openstacknode.api.OpenstackNodeService;
import org.onosproject.openstacknode.api.OpenstackSshAuth;
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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import static java.util.concurrent.Executors.newSingleThreadExecutor;
import static org.onlab.util.Tools.groupedThreads;
import static org.onosproject.net.DefaultEdgeLink.createEdgeLink;
import static org.onosproject.openstacknetworking.api.Constants.DEFAULT_GATEWAY_MAC_STR;
import static org.onosproject.openstacknetworking.api.OpenstackNetwork.Type.GENEVE;
import static org.onosproject.openstacknetworking.api.OpenstackNetwork.Type.GRE;
import static org.onosproject.openstacknetworking.api.OpenstackNetwork.Type.VLAN;
import static org.onosproject.openstacknetworking.api.OpenstackNetwork.Type.VXLAN;

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

    private static final String ID = "id";
    private static final String MODE = "mode";
    private static final String MOUSE = "mouse";
    private static final String TRACE_RESULT = "traceResult";
    private static final String IS_SUCCESS = "isSuccess";
    private static final String TRACE_SUCCESS = "traceSuccess";
    private static final String FLOW_TRACE_RESULT = "flowTraceResult";
    private static final String SRC_DEVICE_ID = "srcDeviceId";
    private static final String DST_DEVICE_ID = "dstDeviceId";
    private static final String UPLINK = "uplink";
    private static final String OVS_VERSION_2_8 = "2.8";
    private static final String OVS_VERSION_2_7 = "2.7";
    private static final String OVS_VERSION_2_6 = "2.6";

    private static final String DL_DST = "dl_dst=";
    private static final String NW_DST = "nw_dst=";
    private static final String DEFAULT_REQUEST_STRING = "sudo ovs-appctl ofproto/trace br-int ip";
    private static final String IN_PORT = "in_port=";
    private static final String NW_SRC = "nw_src=";
    private static final String COMMA = ",";
    private static final String TUN_ID = "tun_id=";

    private static final long TIMEOUT_MS = 5000;
    private static final long WAIT_OUTPUT_STREAM_SECOND = 2;
    private static final int SSH_PORT = 22;

    private enum Mode { IDLE, MOUSE }

    private final Logger log = LoggerFactory.getLogger(getClass());

    private DeviceService deviceService;
    private HostService hostService;
    private PathService pathService;
    private OpenstackNodeService osNodeService;
    private InstancePortService instancePortService;
    private OpenstackNetworkService osNetService;
    private Mode currentMode = Mode.IDLE;
    private Element elementOfNote;

    private final ExecutorService eventExecutor = newSingleThreadExecutor(
            groupedThreads(this.getClass().getSimpleName(), "event-handler", log));

    @Override
    public void init(UiConnection connection, ServiceDirectory directory) {
        super.init(connection, directory);
        deviceService = directory.get(DeviceService.class);
        hostService = directory.get(HostService.class);
        pathService = directory.get(PathService.class);
        osNodeService = directory.get(OpenstackNodeService.class);
        instancePortService = directory.get(InstancePortService.class);
        osNetService = directory.get(OpenstackNetworkService.class);
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
                    eventExecutor.execute(OpenstackNetworkingUiMessageHandler.this::sendMouseData);

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
            String srcDeviceId = string(payload, SRC_DEVICE_ID);
            String dstDeviceId = string(payload, DST_DEVICE_ID);
            boolean uplink = bool(payload, UPLINK);
            log.info("Flow trace request called with" +
                            "src IP: {}, dst IP: {}, src device ID: {}, dst device Id: {}, uplink: ",
                    srcIp,
                    dstIp,
                    srcDeviceId,
                    dstDeviceId,
                    uplink);
            eventExecutor.execute(() -> processFlowTraceRequest(srcIp, dstIp, srcDeviceId, dstDeviceId, uplink));
        }
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
                eventExecutor.execute(() -> updateForMode(id));
            } else {
                eventExecutor.execute(OpenstackNetworkingUiMessageHandler.this::clearForMode);
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

    /**
     * Sends JSON-based message to UI.
     * @param type type
     * @param payload payload
     */
    public void sendMessagetoUi(String type, ObjectNode payload) {
        sendMessage(JsonUtils.envelope(type, payload));
    }

    private int getVni(Host host) {
        String vni = host.annotations().value(ANNOTATION_SEGMENT_ID);

        return vni == null ? 0 : Integer.parseInt(vni);
    }

    private void sendMouseData() {
        Highlights highlights = new Highlights();

        if (elementOfNote instanceof Device) {
            DeviceId deviceId = (DeviceId) elementOfNote.id();

            List<OpenstackLink> edgeLinks = edgeLinks(deviceId);

            edgeLinks.forEach(edgeLink ->
                    highlights.add(edgeLink.highlight(OpenstackLink.RequestType.DEVICE_SELECTED)));

            hostService.getConnectedHosts(deviceId).forEach(host -> {
                HostHighlight hostHighlight = new HostHighlight(host.id().toString());
                hostHighlight.setBadge(createBadge(getVni(host)));
                highlights.add(hostHighlight);
            });

            sendHighlights(highlights);

        } else if (elementOfNote instanceof Host) {

            HostId hostId = HostId.hostId(elementOfNote.id().toString());
            if (!hostMadeFromOpenstack(hostId)) {
                return;
            }

            List<OpenstackLink> openstackLinks = linksInSameNetwork(hostId);

            openstackLinks.forEach(openstackLink ->
                    highlights.add(openstackLink.highlight(OpenstackLink.RequestType.HOST_SELECTED)));

            hostHighlightsInSameNetwork(hostId).forEach(highlights::add);

            sendHighlights(highlights);

        }
    }

    private boolean hostMadeFromOpenstack(HostId hostId) {
        return hostService.getHost(hostId).annotations().value(ANNOTATION_NETWORK_ID) != null;
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

        edgeLinks.addAll(openstackLinkMap.biLinks());

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

        openstackLinks.addAll(linkMap.biLinks());

        return openstackLinks;
    }

    private boolean isHostInSameNetwork(Host host, String networkId) {
        return hostService.getHost(host.id()).annotations()
                .value(ANNOTATION_NETWORK_ID).equals(networkId);
    }

    private NodeBadge createBadge(int n) {
        return NodeBadge.number(Status.INFO, n, "Openstack Node");
    }

    private void processFlowTraceRequest(String srcIp, String dstIp, String srcDeviceId, String dstDeviceId,
                                         boolean uplink) {
        boolean traceSuccess = true;

        ObjectMapper mapper = new ObjectMapper();

        ObjectNode traceResult = mapper.createObjectNode();

        ArrayNode traceResultArray = traceResult.putArray(TRACE_RESULT);

        OpenstackNode srcOpenstackNode = osNodeService.node(DeviceId.deviceId(srcDeviceId));
        if (srcOpenstackNode == null) {
            log.error("There's no openstack node information for device {}", srcDeviceId);
            return;
        }

        if (srcOpenstackNode.sshAuthInfo() == null) {
            log.error("Openstack node {} has no SSH authentication information..",
                    srcOpenstackNode.hostname());
            return;
        }

        String traceResultString = sendTraceRequestToNode(srcIp, dstIp, srcOpenstackNode, uplink);

        if (traceResultString == null) {
            return;
        }

        log.debug("traceResultString raw data: {}", traceResultString);

        ObjectNode traceResultJson = null;

        Device srcDevice = deviceService.getDevice(srcOpenstackNode.intgBridge());
        if (srcDevice.swVersion().startsWith(OVS_VERSION_2_8) ||
                srcDevice.swVersion().startsWith(OVS_VERSION_2_7)) {
            traceResultJson = Ovs28FlowTraceResultParser.flowTraceResultInJson(
                    traceResultString.trim(), srcOpenstackNode.hostname());
        } else {
            log.error("Currently OVS version {} is not supported",
                    deviceService.getDevice(srcOpenstackNode.intgBridge()));
        }

        if (traceResultJson == null) {
            return;
        }

        traceResultArray.add(traceResultJson);

        log.debug("traceResultForward Json: {}", traceResultJson);

        if (!traceResultJson.get(IS_SUCCESS).asBoolean()) {
            traceSuccess = false;
        }

        traceResult.put(TRACE_SUCCESS, traceSuccess);

        traceResult.put(SRC_IP, srcIp);
        traceResult.put(DST_IP, dstIp);
        traceResult.put(SRC_DEVICE_ID, srcDeviceId);
        traceResult.put(DST_DEVICE_ID, dstDeviceId);
        traceResult.put(UPLINK, uplink);

        log.debug("traceResult Json: {}", traceResult);

        sendMessagetoUi(FLOW_TRACE_RESULT, traceResult);

    }

    private String sendTraceRequestToNode(String srcIp, String dstIp,
                                          OpenstackNode openstackNode, boolean uplink) {

        Optional<InstancePort> instancePort = instancePortService.instancePorts().stream()
                .filter(port -> port.ipAddress().getIp4Address().toString().equals(srcIp)
                        && port.deviceId().equals(openstackNode.intgBridge()))
                .findAny();

        if (!instancePort.isPresent()) {
            return null;
        }

        String requestString = traceRequestString(srcIp, dstIp,
                instancePort.get(), osNetService, uplink);

        return sendTraceRequestToNode(requestString, openstackNode);
    }

    private String traceRequestString(String srcIp,
                                      String dstIp,
                                      InstancePort srcInstancePort,
                                      OpenstackNetworkService osNetService,
                                      boolean uplink) {

        StringBuilder requestStringBuilder = new StringBuilder(DEFAULT_REQUEST_STRING);

        if (uplink) {

            requestStringBuilder.append(COMMA)
                    .append(IN_PORT)
                    .append(srcInstancePort.portNumber().toString())
                    .append(COMMA)
                    .append(NW_SRC)
                    .append(srcIp)
                    .append(COMMA);

            Type netType = osNetService.networkType(srcInstancePort.networkId());

            if (netType == VXLAN || netType == VLAN || netType == GRE || netType == GENEVE) {
                if (srcIp.equals(dstIp)) {
                    dstIp = osNetService.gatewayIp(srcInstancePort.portId());
                    requestStringBuilder.append(DL_DST)
                            .append(DEFAULT_GATEWAY_MAC_STR).append(COMMA);
                } else if (!osNetService.ipPrefix(srcInstancePort.portId()).contains(IpAddress.valueOf(dstIp))) {
                    requestStringBuilder.append(DL_DST)
                            .append(DEFAULT_GATEWAY_MAC_STR)
                            .append(COMMA);
                }
            } else {
                if (srcIp.equals(dstIp)) {
                    dstIp = osNetService.gatewayIp(srcInstancePort.portId());
                }
            }

            requestStringBuilder.append(NW_DST)
                    .append(dstIp)
                    .append("\n");
        } else {
            requestStringBuilder.append(COMMA)
                    .append(NW_SRC)
                    .append(dstIp)
                    .append(COMMA);

            if (osNetService.networkType(srcInstancePort.networkId()).equals(VXLAN) ||
                    osNetService.networkType(srcInstancePort.networkId()).equals(VLAN)) {
                requestStringBuilder.append(TUN_ID)
                        .append(osNetService.segmentId(srcInstancePort.networkId()))
                        .append(COMMA);
            }
            requestStringBuilder.append(NW_DST)
                    .append(srcIp)
                    .append("\n");

        }

        return requestStringBuilder.toString();
    }

    private String sendTraceRequestToNode(String requestString,
                                                OpenstackNode node) {
        String traceResult = null;
        OpenstackSshAuth sshAuth = node.sshAuthInfo();

        try (SshClient client = SshClient.setUpDefaultClient()) {
            client.start();

            try (ClientSession session = client
                    .connect(sshAuth.id(), node.managementIp().getIp4Address().toString(), SSH_PORT)
                    .verify(TIMEOUT_MS, TimeUnit.SECONDS).getSession()) {
                session.addPasswordIdentity(sshAuth.password());
                session.auth().verify(TIMEOUT_MS, TimeUnit.SECONDS);


                try (ClientChannel channel = session.createChannel(ClientChannel.CHANNEL_SHELL)) {

                    log.debug("requestString: {}", requestString);
                    final InputStream inputStream =
                            new ByteArrayInputStream(requestString.getBytes());

                    OutputStream outputStream = new ByteArrayOutputStream();
                    OutputStream errStream = new ByteArrayOutputStream();

                    channel.setIn(new NoCloseInputStream(inputStream));
                    channel.setErr(errStream);
                    channel.setOut(outputStream);

                    Collection<ClientChannelEvent> eventList = Lists.newArrayList();
                    eventList.add(ClientChannelEvent.OPENED);

                    OpenFuture channelFuture = channel.open();

                    if (channelFuture.await(TIMEOUT_MS, TimeUnit.SECONDS)) {

                        long timeoutExpiredMs = System.currentTimeMillis() + TIMEOUT_MS;

                        while (!channelFuture.isOpened()) {
                            if ((timeoutExpiredMs - System.currentTimeMillis()) <= 0) {
                                log.error("Failed to open channel");
                                return null;
                            }
                        }
                        TimeUnit.SECONDS.sleep(WAIT_OUTPUT_STREAM_SECOND);

                        traceResult = ((ByteArrayOutputStream) outputStream).toString(Charsets.UTF_8.name());

                        channel.close();
                    }
                } finally {
                    session.close();
                }
            } finally {
                client.stop();
            }

        } catch (Exception e) {
            log.error("Exception occurred because of {}", e.toString());
        }

        return traceResult;
    }
}

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
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.eclipse.jetty.websocket.WebSocket;
import org.onlab.onos.cluster.ClusterEvent;
import org.onlab.onos.cluster.ClusterEventListener;
import org.onlab.onos.cluster.ControllerNode;
import org.onlab.onos.core.ApplicationId;
import org.onlab.onos.core.CoreService;
import org.onlab.onos.mastership.MastershipEvent;
import org.onlab.onos.mastership.MastershipListener;
import org.onlab.onos.net.ConnectPoint;
import org.onlab.onos.net.Device;
import org.onlab.onos.net.Host;
import org.onlab.onos.net.HostId;
import org.onlab.onos.net.HostLocation;
import org.onlab.onos.net.Link;
import org.onlab.onos.net.device.DeviceEvent;
import org.onlab.onos.net.device.DeviceListener;
import org.onlab.onos.net.flow.DefaultTrafficSelector;
import org.onlab.onos.net.flow.DefaultTrafficTreatment;
import org.onlab.onos.net.flow.TrafficSelector;
import org.onlab.onos.net.flow.TrafficTreatment;
import org.onlab.onos.net.host.HostEvent;
import org.onlab.onos.net.host.HostListener;
import org.onlab.onos.net.intent.HostToHostIntent;
import org.onlab.onos.net.intent.Intent;
import org.onlab.onos.net.intent.IntentEvent;
import org.onlab.onos.net.intent.IntentListener;
import org.onlab.onos.net.intent.MultiPointToSinglePointIntent;
import org.onlab.onos.net.link.LinkEvent;
import org.onlab.onos.net.link.LinkListener;
import org.onlab.osgi.ServiceDirectory;
import org.onlab.packet.Ethernet;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import static com.google.common.base.Strings.isNullOrEmpty;
import static org.onlab.onos.cluster.ClusterEvent.Type.INSTANCE_ADDED;
import static org.onlab.onos.net.DeviceId.deviceId;
import static org.onlab.onos.net.HostId.hostId;
import static org.onlab.onos.net.device.DeviceEvent.Type.DEVICE_ADDED;
import static org.onlab.onos.net.device.DeviceEvent.Type.DEVICE_UPDATED;
import static org.onlab.onos.net.host.HostEvent.Type.HOST_ADDED;
import static org.onlab.onos.net.link.LinkEvent.Type.LINK_ADDED;

/**
 * Web socket capable of interacting with the GUI topology view.
 */
public class TopologyViewWebSocket
        extends TopologyViewMessages
        implements WebSocket.OnTextMessage, WebSocket.OnControl {

    private static final long MAX_AGE_MS = 15000;

    private static final byte PING = 0x9;
    private static final byte PONG = 0xA;
    private static final byte[] PING_DATA = new byte[]{(byte) 0xde, (byte) 0xad};

    private static final String APP_ID = "org.onlab.onos.gui";

    private static final long SUMMARY_FREQUENCY_SEC = 3000;
    private static final long TRAFFIC_FREQUENCY_SEC = 1500;

    private static final Comparator<? super ControllerNode> NODE_COMPARATOR =
            new Comparator<ControllerNode>() {
                @Override
                public int compare(ControllerNode o1, ControllerNode o2) {
                    return o1.id().toString().compareTo(o2.id().toString());
                }
            };

    private final ApplicationId appId;

    private Connection connection;
    private FrameConnection control;

    private final ClusterEventListener clusterListener = new InternalClusterListener();
    private final MastershipListener mastershipListener = new InternalMastershipListener();
    private final DeviceListener deviceListener = new InternalDeviceListener();
    private final LinkListener linkListener = new InternalLinkListener();
    private final HostListener hostListener = new InternalHostListener();
    private final IntentListener intentListener = new InternalIntentListener();

    // Timers and objects being monitored
    private final Timer timer = new Timer("topology-view");

    private TimerTask trafficTask;
    private ObjectNode trafficEvent;

    private TimerTask summaryTask;
    private ObjectNode summaryEvent;

    private long lastActive = System.currentTimeMillis();
    private boolean listenersRemoved = false;

    private TopologyViewIntentFilter intentFilter;

    /**
     * Creates a new web-socket for serving data to GUI topology view.
     *
     * @param directory service directory
     */
    public TopologyViewWebSocket(ServiceDirectory directory) {
        super(directory);

        intentFilter = new TopologyViewIntentFilter(intentService, deviceService,
                                                    hostService, linkService);
        appId = directory.get(CoreService.class).registerApplication(APP_ID);
    }

    /**
     * Issues a close on the connection.
     */
    synchronized void close() {
        removeListeners();
        if (connection.isOpen()) {
            connection.close();
        }
    }

    /**
     * Indicates if this connection is idle.
     *
     * @return true if idle or closed
     */
    synchronized boolean isIdle() {
        boolean idle = (System.currentTimeMillis() - lastActive) > MAX_AGE_MS;
        if (idle || !connection.isOpen()) {
            return true;
        }
        try {
            control.sendControl(PING, PING_DATA, 0, PING_DATA.length);
        } catch (IOException e) {
            log.warn("Unable to send ping message due to: ", e);
        }
        return false;
    }

    @Override
    public void onOpen(Connection connection) {
        log.info("GUI client connected");
        this.connection = connection;
        this.control = (FrameConnection) connection;
        addListeners();

        sendAllInstances(null);
        sendAllDevices();
        sendAllLinks();
        sendAllHosts();
    }

    @Override
    public synchronized void onClose(int closeCode, String message) {
        removeListeners();
        timer.cancel();
        log.info("GUI client disconnected");
    }

    @Override
    public boolean onControl(byte controlCode, byte[] data, int offset, int length) {
        lastActive = System.currentTimeMillis();
        return true;
    }

    @Override
    public void onMessage(String data) {
        lastActive = System.currentTimeMillis();
        try {
            processMessage((ObjectNode) mapper.reader().readTree(data));
        } catch (Exception e) {
            log.warn("Unable to parse GUI request {} due to {}", data, e);
            log.warn("Boom!!!!", e);
        }
    }

    // Processes the specified event.
    private void processMessage(ObjectNode event) {
        String type = string(event, "event", "unknown");
        if (type.equals("requestDetails")) {
            requestDetails(event);
        } else if (type.equals("updateMeta")) {
            updateMetaUi(event);

        } else if (type.equals("addHostIntent")) {
            createHostIntent(event);
        } else if (type.equals("addMultiSourceIntent")) {
            createMultiSourceIntent(event);

        } else if (type.equals("requestTraffic")) {
            requestTraffic(event);
        } else if (type.equals("requestAllTraffic")) {
            requestAllTraffic(event);
        } else if (type.equals("requestDeviceLinkFlows")) {
            requestDeviceLinkFlows(event);
        } else if (type.equals("cancelTraffic")) {
            cancelTraffic(event);

        } else if (type.equals("requestSummary")) {
            requestSummary(event);
        } else if (type.equals("cancelSummary")) {
            cancelSummary(event);
        }
    }

    // Sends the specified data to the client.
    private synchronized void sendMessage(ObjectNode data) {
        try {
            if (connection.isOpen()) {
                connection.sendMessage(data.toString());
            }
        } catch (IOException e) {
            log.warn("Unable to send message {} to GUI due to {}", data, e);
        }
    }

    // Sends all controller nodes to the client as node-added messages.
    private void sendAllInstances(String messageType) {
        List<ControllerNode> nodes = new ArrayList<>(clusterService.getNodes());
        Collections.sort(nodes, NODE_COMPARATOR);
        for (ControllerNode node : nodes) {
            sendMessage(instanceMessage(new ClusterEvent(INSTANCE_ADDED, node),
                                        messageType));
        }
    }

    // Sends all devices to the client as device-added messages.
    private void sendAllDevices() {
        for (Device device : deviceService.getDevices()) {
            sendMessage(deviceMessage(new DeviceEvent(DEVICE_ADDED, device)));
        }
    }

    // Sends all links to the client as link-added messages.
    private void sendAllLinks() {
        for (Link link : linkService.getLinks()) {
            sendMessage(linkMessage(new LinkEvent(LINK_ADDED, link)));
        }
    }

    // Sends all hosts to the client as host-added messages.
    private void sendAllHosts() {
        for (Host host : hostService.getHosts()) {
            sendMessage(hostMessage(new HostEvent(HOST_ADDED, host)));
        }
    }

    // Sends back device or host details.
    private void requestDetails(ObjectNode event) {
        ObjectNode payload = payload(event);
        String type = string(payload, "class", "unknown");
        long sid = number(event, "sid");

        if (type.equals("device")) {
            sendMessage(deviceDetails(deviceId(string(payload, "id")), sid));
        } else if (type.equals("host")) {
            sendMessage(hostDetails(hostId(string(payload, "id")), sid));
        }
    }


    // Creates host-to-host intent.
    private void createHostIntent(ObjectNode event) {
        ObjectNode payload = payload(event);
        long id = number(event, "sid");
        // TODO: add protection against device ids and non-existent hosts.
        HostId one = hostId(string(payload, "one"));
        HostId two = hostId(string(payload, "two"));

        HostToHostIntent intent =
                new HostToHostIntent(appId, one, two,
                                     DefaultTrafficSelector.builder().build(),
                                     DefaultTrafficTreatment.builder().build());
        startMonitoring(event);
        intentService.submit(intent);
    }

    // Creates multi-source-to-single-dest intent.
    private void createMultiSourceIntent(ObjectNode event) {
        ObjectNode payload = payload(event);
        long id = number(event, "sid");
        // TODO: add protection against device ids and non-existent hosts.
        Set<HostId> src = getHostIds((ArrayNode) payload.path("src"));
        HostId dst = hostId(string(payload, "dst"));
        Host dstHost = hostService.getHost(dst);

        Set<ConnectPoint> ingressPoints = getHostLocations(src);

        // FIXME: clearly, this is not enough
        TrafficSelector selector = DefaultTrafficSelector.builder()
                .matchEthType(Ethernet.TYPE_IPV4)
                .matchEthDst(dstHost.mac()).build();
        TrafficTreatment treatment = DefaultTrafficTreatment.builder().build();

        MultiPointToSinglePointIntent intent =
                new MultiPointToSinglePointIntent(appId, selector, treatment,
                                                  ingressPoints, dstHost.location());
        startMonitoring(event);
        intentService.submit(intent);
    }

    private Set<ConnectPoint> getHostLocations(Set<HostId> hostIds) {
        Set<ConnectPoint> points = new HashSet<>();
        for (HostId hostId : hostIds) {
            points.add(getHostLocation(hostId));
        }
        return points;
    }

    private HostLocation getHostLocation(HostId hostId) {
        return hostService.getHost(hostId).location();
    }

    // Produces a list of host ids from the specified JSON array.
    private Set<HostId> getHostIds(ArrayNode ids) {
        Set<HostId> hostIds = new HashSet<>();
        for (JsonNode id : ids) {
            hostIds.add(hostId(id.asText()));
        }
        return hostIds;
    }


    private synchronized long startMonitoring(ObjectNode event) {
        if (trafficTask != null) {
            stopMonitoring();
        }
        trafficEvent = event;
        trafficTask = new TrafficMonitor();
        timer.schedule(trafficTask, TRAFFIC_FREQUENCY_SEC, TRAFFIC_FREQUENCY_SEC);
        return number(event, "sid");
    }

    private synchronized void stopMonitoring() {
        if (trafficTask != null) {
            trafficTask.cancel();
            trafficTask = null;
            trafficEvent = null;
        }
    }

    // Subscribes for host traffic messages.
    private synchronized void requestAllTraffic(ObjectNode event) {
        long sid = startMonitoring(event);
        sendMessage(trafficSummaryMessage(sid));
    }

    private void requestDeviceLinkFlows(ObjectNode event) {
        ObjectNode payload = payload(event);
        long sid = startMonitoring(event);

        // Get the set of selected hosts and their intents.
        ArrayNode ids = (ArrayNode) payload.path("ids");
        Set<Host> hosts = new HashSet<>();
        Set<Device> devices = getDevices(ids);

        // If there is a hover node, include it in the hosts and find intents.
        String hover = string(payload, "hover");
        if (!isNullOrEmpty(hover)) {
            addHover(hosts, devices, hover);
        }
        sendMessage(flowSummaryMessage(sid, devices));
    }


    // Subscribes for host traffic messages.
    private synchronized void requestTraffic(ObjectNode event) {
        ObjectNode payload = payload(event);
        if (!payload.has("ids")) {
            return;
        }

        long sid = startMonitoring(event);

        // Get the set of selected hosts and their intents.
        ArrayNode ids = (ArrayNode) payload.path("ids");
        Set<Host> hosts = getHosts(ids);
        Set<Device> devices = getDevices(ids);
        Set<Intent> intents = intentFilter.findPathIntents(hosts, devices);

        // If there is a hover node, include it in the hosts and find intents.
        String hover = string(payload, "hover");
        Set<Intent> hoverIntents;
        if (!isNullOrEmpty(hover)) {
            addHover(hosts, devices, hover);
            hoverIntents = intentFilter.findPathIntents(hosts, devices);
            intents.removeAll(hoverIntents);

            // Send an initial message to highlight all links of all monitored intents.
            sendMessage(trafficMessage(sid,
                                       new TrafficClass("primary", hoverIntents),
                                       new TrafficClass("secondary", intents)));

        } else {
            // Send an initial message to highlight all links of all monitored intents.
            sendMessage(trafficMessage(sid, new TrafficClass("primary", intents)));
        }
    }

    // Cancels sending traffic messages.
    private void cancelTraffic(ObjectNode event) {
        sendMessage(trafficMessage(number(event, "sid")));
        stopMonitoring();
    }


    // Subscribes for summary messages.
    private synchronized void requestSummary(ObjectNode event) {
        if (summaryTask == null) {
            summaryEvent = event;
            summaryTask = new SummaryMonitor();
            timer.schedule(summaryTask, SUMMARY_FREQUENCY_SEC, SUMMARY_FREQUENCY_SEC);
        }
        sendMessage(summmaryMessage(number(event, "sid")));
    }

    // Cancels sending summary messages.
    private synchronized void cancelSummary(ObjectNode event) {
        if (summaryTask != null) {
            summaryTask.cancel();
            summaryTask = null;
            summaryEvent = null;
        }
    }


    // Adds all internal listeners.
    private void addListeners() {
        clusterService.addListener(clusterListener);
        mastershipService.addListener(mastershipListener);
        deviceService.addListener(deviceListener);
        linkService.addListener(linkListener);
        hostService.addListener(hostListener);
        intentService.addListener(intentListener);
    }

    // Removes all internal listeners.
    private synchronized void removeListeners() {
        if (!listenersRemoved) {
            listenersRemoved = true;
            clusterService.removeListener(clusterListener);
            mastershipService.removeListener(mastershipListener);
            deviceService.removeListener(deviceListener);
            linkService.removeListener(linkListener);
            hostService.removeListener(hostListener);
            intentService.removeListener(intentListener);
        }
    }

    // Cluster event listener.
    private class InternalClusterListener implements ClusterEventListener {
        @Override
        public void event(ClusterEvent event) {
            sendMessage(instanceMessage(event, null));
        }
    }

    // Mastership change listener
    private class InternalMastershipListener implements MastershipListener {
        @Override
        public void event(MastershipEvent event) {
            sendAllInstances("updateInstance");
            Device device = deviceService.getDevice(event.subject());
            sendMessage(deviceMessage(new DeviceEvent(DEVICE_UPDATED, device)));
        }
    }

    // Device event listener.
    private class InternalDeviceListener implements DeviceListener {
        @Override
        public void event(DeviceEvent event) {
            sendMessage(deviceMessage(event));
        }
    }

    // Link event listener.
    private class InternalLinkListener implements LinkListener {
        @Override
        public void event(LinkEvent event) {
            sendMessage(linkMessage(event));
        }
    }

    // Host event listener.
    private class InternalHostListener implements HostListener {
        @Override
        public void event(HostEvent event) {
            sendMessage(hostMessage(event));
        }
    }

    // Intent event listener.
    private class InternalIntentListener implements IntentListener {
        @Override
        public void event(IntentEvent event) {
            if (trafficEvent != null) {
                requestTraffic(trafficEvent);
            }
        }
    }

    private class TrafficMonitor extends TimerTask {
        @Override
        public void run() {
            if (trafficEvent != null) {
                String type = string(trafficEvent, "event", "unknown");
                if (type.equals("requestAllTraffic")) {
                    requestAllTraffic(trafficEvent);
                } else if (type.equals("requestDeviceLinkFlows")) {
                    requestDeviceLinkFlows(trafficEvent);
                } else {
                    requestTraffic(trafficEvent);
                }
            }
        }
    }

    private class SummaryMonitor extends TimerTask {
        @Override
        public void run() {
            if (summaryEvent != null) {
                requestSummary(summaryEvent);
            }
        }
    }

}


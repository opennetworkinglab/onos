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
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.eclipse.jetty.websocket.WebSocket;
import org.onlab.osgi.ServiceDirectory;
import org.onosproject.cluster.ClusterEvent;
import org.onosproject.cluster.ClusterEventListener;
import org.onosproject.cluster.ControllerNode;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.event.AbstractEventAccumulator;
import org.onosproject.event.Event;
import org.onosproject.event.EventAccumulator;
import org.onosproject.mastership.MastershipAdminService;
import org.onosproject.mastership.MastershipEvent;
import org.onosproject.mastership.MastershipListener;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.Device;
import org.onosproject.net.Host;
import org.onosproject.net.HostId;
import org.onosproject.net.HostLocation;
import org.onosproject.net.Link;
import org.onosproject.net.device.DeviceEvent;
import org.onosproject.net.device.DeviceListener;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.FlowRuleEvent;
import org.onosproject.net.flow.FlowRuleListener;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.host.HostEvent;
import org.onosproject.net.host.HostListener;
import org.onosproject.net.intent.HostToHostIntent;
import org.onosproject.net.intent.Intent;
import org.onosproject.net.intent.IntentEvent;
import org.onosproject.net.intent.IntentListener;
import org.onosproject.net.intent.MultiPointToSinglePointIntent;
import org.onosproject.net.link.LinkEvent;
import org.onosproject.net.link.LinkListener;

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
import static org.onosproject.cluster.ClusterEvent.Type.INSTANCE_ADDED;
import static org.onosproject.net.DeviceId.deviceId;
import static org.onosproject.net.HostId.hostId;
import static org.onosproject.net.device.DeviceEvent.Type.DEVICE_ADDED;
import static org.onosproject.net.device.DeviceEvent.Type.DEVICE_UPDATED;
import static org.onosproject.net.host.HostEvent.Type.HOST_ADDED;
import static org.onosproject.net.link.LinkEvent.Type.LINK_ADDED;

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

    private static final String APP_ID = "org.onosproject.gui";

    private static final long TRAFFIC_FREQUENCY = 5000;
    private static final long SUMMARY_FREQUENCY = 30000;

    private static final Comparator<? super ControllerNode> NODE_COMPARATOR =
            new Comparator<ControllerNode>() {
                @Override
                public int compare(ControllerNode o1, ControllerNode o2) {
                    return o1.id().toString().compareTo(o2.id().toString());
                }
            };


    private final Timer timer = new Timer("topology-view");

    private static final int MAX_EVENTS = 1000;
    private static final int MAX_BATCH_MS = 5000;
    private static final int MAX_IDLE_MS = 1000;

    private final ApplicationId appId;

    private Connection connection;
    private FrameConnection control;

    private final ClusterEventListener clusterListener = new InternalClusterListener();
    private final MastershipListener mastershipListener = new InternalMastershipListener();
    private final DeviceListener deviceListener = new InternalDeviceListener();
    private final LinkListener linkListener = new InternalLinkListener();
    private final HostListener hostListener = new InternalHostListener();
    private final IntentListener intentListener = new InternalIntentListener();
    private final FlowRuleListener flowListener = new InternalFlowListener();

    private final EventAccumulator eventAccummulator = new InternalEventAccummulator();

    private TimerTask trafficTask;
    private ObjectNode trafficEvent;

    private TimerTask summaryTask;
    private ObjectNode summaryEvent;

    private long lastActive = System.currentTimeMillis();
    private boolean listenersRemoved = false;

    private TopologyViewIntentFilter intentFilter;

    // Current selection context
    private Set<Host> selectedHosts;
    private Set<Device> selectedDevices;
    private List<Intent> selectedIntents;
    private int currentIntentIndex = -1;

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
        if (idle || (connection != null && !connection.isOpen())) {
            return true;
        } else if (connection != null) {
            try {
                control.sendControl(PING, PING_DATA, 0, PING_DATA.length);
            } catch (IOException e) {
                log.warn("Unable to send ping message due to: ", e);
            }
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
            log.debug("Boom!!!", e);
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

        } else if (type.equals("requestRelatedIntents")) {
            stopTrafficMonitoring();
            requestRelatedIntents(event);

        } else if (type.equals("requestNextRelatedIntent")) {
            stopTrafficMonitoring();
            requestAnotherRelatedIntent(event, +1);
        } else if (type.equals("requestPrevRelatedIntent")) {
            stopTrafficMonitoring();
            requestAnotherRelatedIntent(event, -1);
        } else if (type.equals("requestSelectedIntentTraffic")) {
            requestSelectedIntentTraffic(event);
            startTrafficMonitoring(event);

        } else if (type.equals("requestAllTraffic")) {
            requestAllTraffic(event);
            startTrafficMonitoring(event);

        } else if (type.equals("requestDeviceLinkFlows")) {
            requestDeviceLinkFlows(event);
            startTrafficMonitoring(event);

        } else if (type.equals("cancelTraffic")) {
            cancelTraffic(event);

        } else if (type.equals("requestSummary")) {
            requestSummary(event);
            startSummaryMonitoring(event);
        } else if (type.equals("cancelSummary")) {
            stopSummaryMonitoring();

        } else if (type.equals("equalizeMasters")) {
            equalizeMasters(event);
        }
    }

    // Sends the specified data to the client.
    protected synchronized void sendMessage(ObjectNode data) {
        try {
            if (connection.isOpen()) {
                connection.sendMessage(data.toString());
            }
        } catch (IOException e) {
            log.warn("Unable to send message {} to GUI due to {}", data, e);
            log.debug("Boom!!!", e);
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
        // Send optical first, others later for layered rendering
        for (Device device : deviceService.getDevices()) {
            if (device.type() == Device.Type.ROADM) {
                sendMessage(deviceMessage(new DeviceEvent(DEVICE_ADDED, device)));
            }
        }
        for (Device device : deviceService.getDevices()) {
            if (device.type() != Device.Type.ROADM) {
                sendMessage(deviceMessage(new DeviceEvent(DEVICE_ADDED, device)));
            }
        }
    }

    // Sends all links to the client as link-added messages.
    private void sendAllLinks() {
        // Send optical first, others later for layered rendering
        for (Link link : linkService.getLinks()) {
            if (link.type() == Link.Type.OPTICAL) {
                sendMessage(linkMessage(new LinkEvent(LINK_ADDED, link)));
            }
        }
        for (Link link : linkService.getLinks()) {
            if (link.type() != Link.Type.OPTICAL) {
                sendMessage(linkMessage(new LinkEvent(LINK_ADDED, link)));
            }
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

        intentService.submit(intent);
        startMonitoringIntent(event, intent);
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
                .matchEthDst(dstHost.mac()).build();
        TrafficTreatment treatment = DefaultTrafficTreatment.builder().build();

        MultiPointToSinglePointIntent intent =
                new MultiPointToSinglePointIntent(appId, selector, treatment,
                                                  ingressPoints, dstHost.location());

        intentService.submit(intent);
        startMonitoringIntent(event, intent);
    }


    private synchronized void startMonitoringIntent(ObjectNode event, Intent intent) {
        selectedHosts = new HashSet<>();
        selectedDevices = new HashSet<>();
        selectedIntents = new ArrayList<>();
        selectedIntents.add(intent);
        currentIntentIndex = -1;
        requestAnotherRelatedIntent(event, +1);
        requestSelectedIntentTraffic(event);
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


    private synchronized long startTrafficMonitoring(ObjectNode event) {
        stopTrafficMonitoring();
        trafficEvent = event;
        trafficTask = new TrafficMonitor();
        timer.schedule(trafficTask, TRAFFIC_FREQUENCY, TRAFFIC_FREQUENCY);
        return number(event, "sid");
    }

    private synchronized void stopTrafficMonitoring() {
        if (trafficTask != null) {
            trafficTask.cancel();
            trafficTask = null;
            trafficEvent = null;
        }
    }

    // Subscribes for host traffic messages.
    private synchronized void requestAllTraffic(ObjectNode event) {
        long sid = startTrafficMonitoring(event);
        sendMessage(trafficSummaryMessage(sid));
    }

    private void requestDeviceLinkFlows(ObjectNode event) {
        ObjectNode payload = payload(event);
        long sid = startTrafficMonitoring(event);

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


    // Requests related intents message.
    private synchronized void requestRelatedIntents(ObjectNode event) {
        ObjectNode payload = payload(event);
        if (!payload.has("ids")) {
            return;
        }

        long sid = number(event, "sid");

        // Cancel any other traffic monitoring mode.
        stopTrafficMonitoring();

        // Get the set of selected hosts and their intents.
        ArrayNode ids = (ArrayNode) payload.path("ids");
        selectedHosts = getHosts(ids);
        selectedDevices = getDevices(ids);
        selectedIntents = intentFilter.findPathIntents(selectedHosts, selectedDevices,
                                                       intentService.getIntents());
        currentIntentIndex = -1;

        if (haveSelectedIntents()) {
            // Send a message to highlight all links of all monitored intents.
            sendMessage(trafficMessage(sid, new TrafficClass("primary", selectedIntents)));
        }

        // FIXME: Re-introduce one the client click vs hover gesture stuff is sorted out.
//        String hover = string(payload, "hover");
//        if (!isNullOrEmpty(hover)) {
//            // If there is a hover node, include it in the selection and find intents.
//            processHoverExtendedSelection(sid, hover);
//        }
    }

    private boolean haveSelectedIntents() {
        return selectedIntents != null && !selectedIntents.isEmpty();
    }

    // Processes the selection extended with hovered item to segregate items
    // into primary (those including the hover) vs secondary highlights.
    private void processHoverExtendedSelection(long sid, String hover) {
        Set<Host> hoverSelHosts = new HashSet<>(selectedHosts);
        Set<Device> hoverSelDevices = new HashSet<>(selectedDevices);
        addHover(hoverSelHosts, hoverSelDevices, hover);

        List<Intent> primary = selectedIntents == null ? new ArrayList<>() :
                intentFilter.findPathIntents(hoverSelHosts, hoverSelDevices,
                                             selectedIntents);
        Set<Intent> secondary = new HashSet<>(selectedIntents);
        secondary.removeAll(primary);

        // Send a message to highlight all links of all monitored intents.
        sendMessage(trafficMessage(sid, new TrafficClass("primary", primary),
                                   new TrafficClass("secondary", secondary)));
    }

    // Requests next or previous related intent.
    private void requestAnotherRelatedIntent(ObjectNode event, int offset) {
        if (haveSelectedIntents()) {
            currentIntentIndex = currentIntentIndex + offset;
            if (currentIntentIndex < 0) {
                currentIntentIndex = selectedIntents.size() - 1;
            } else if (currentIntentIndex >= selectedIntents.size()) {
                currentIntentIndex = 0;
            }
            sendSelectedIntent(event);
        }
    }

    // Sends traffic information on the related intents with the currently
    // selected intent highlighted.
    private void sendSelectedIntent(ObjectNode event) {
        Intent selectedIntent = selectedIntents.get(currentIntentIndex);
        log.info("Requested next intent {}", selectedIntent.id());

        Set<Intent> primary = new HashSet<>();
        primary.add(selectedIntent);

        Set<Intent> secondary = new HashSet<>(selectedIntents);
        secondary.remove(selectedIntent);

        // Send a message to highlight all links of the selected intent.
        sendMessage(trafficMessage(number(event, "sid"),
                                   new TrafficClass("primary", primary),
                                   new TrafficClass("secondary", secondary)));
    }

    // Requests monitoring of traffic for the selected intent.
    private void requestSelectedIntentTraffic(ObjectNode event) {
        if (haveSelectedIntents()) {
            if (currentIntentIndex < 0) {
                currentIntentIndex = 0;
            }
            Intent selectedIntent = selectedIntents.get(currentIntentIndex);
            log.info("Requested traffic for selected {}", selectedIntent.id());

            Set<Intent> primary = new HashSet<>();
            primary.add(selectedIntent);

            // Send a message to highlight all links of the selected intent.
            sendMessage(trafficMessage(number(event, "sid"),
                                       new TrafficClass("primary", primary, true)));
        }
    }

    // Cancels sending traffic messages.
    private void cancelTraffic(ObjectNode event) {
        selectedIntents = null;
        sendMessage(trafficMessage(number(event, "sid")));
        stopTrafficMonitoring();
    }


    private synchronized long startSummaryMonitoring(ObjectNode event) {
        stopSummaryMonitoring();
        summaryEvent = event;
        summaryTask = new SummaryMonitor();
        timer.schedule(summaryTask, SUMMARY_FREQUENCY, SUMMARY_FREQUENCY);
        return number(event, "sid");
    }

    private synchronized void stopSummaryMonitoring() {
        if (summaryEvent != null) {
            summaryTask.cancel();
            summaryTask = null;
            summaryEvent = null;
        }
    }

    // Subscribes for summary messages.
    private synchronized void requestSummary(ObjectNode event) {
        sendMessage(summmaryMessage(number(event, "sid")));
    }


    // Forces mastership role rebalancing.
    private void equalizeMasters(ObjectNode event) {
        directory.get(MastershipAdminService.class).balanceRoles();
    }


    // Adds all internal listeners.
    private void addListeners() {
        clusterService.addListener(clusterListener);
        mastershipService.addListener(mastershipListener);
        deviceService.addListener(deviceListener);
        linkService.addListener(linkListener);
        hostService.addListener(hostListener);
        intentService.addListener(intentListener);
        flowService.addListener(flowListener);
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
            flowService.removeListener(flowListener);
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
            eventAccummulator.add(event);
        }
    }

    // Link event listener.
    private class InternalLinkListener implements LinkListener {
        @Override
        public void event(LinkEvent event) {
            sendMessage(linkMessage(event));
            eventAccummulator.add(event);
        }
    }

    // Host event listener.
    private class InternalHostListener implements HostListener {
        @Override
        public void event(HostEvent event) {
            sendMessage(hostMessage(event));
            eventAccummulator.add(event);
        }
    }

    // Intent event listener.
    private class InternalIntentListener implements IntentListener {
        @Override
        public void event(IntentEvent event) {
            if (trafficEvent != null) {
                requestSelectedIntentTraffic(trafficEvent);
            }
            eventAccummulator.add(event);
        }
    }

    // Intent event listener.
    private class InternalFlowListener implements FlowRuleListener {
        @Override
        public void event(FlowRuleEvent event) {
            eventAccummulator.add(event);
        }
    }

    // Periodic update of the traffic information
    private class TrafficMonitor extends TimerTask {
        @Override
        public void run() {
            try {
                if (trafficEvent != null) {
                    String type = string(trafficEvent, "event", "unknown");
                    if (type.equals("requestAllTraffic")) {
                        requestAllTraffic(trafficEvent);
                    } else if (type.equals("requestDeviceLinkFlows")) {
                        requestDeviceLinkFlows(trafficEvent);
                    } else if (type.equals("requestSelectedIntentTraffic")) {
                        requestSelectedIntentTraffic(trafficEvent);
                    }
                }
            } catch (Exception e) {
                log.warn("Unable to handle traffic request due to {}", e.getMessage());
                log.debug("Boom!", e);
            }
        }
    }

    // Periodic update of the summary information
    private class SummaryMonitor extends TimerTask {
        @Override
        public void run() {
            try {
                if (summaryEvent != null) {
                    requestSummary(summaryEvent);
                }
            } catch (Exception e) {
                log.warn("Unable to handle summary request due to {}", e.getMessage());
                log.debug("Boom!", e);
            }
        }
    }

    // Accumulates events to drive methodic update of the summary pane.
    private class InternalEventAccummulator extends AbstractEventAccumulator {
        protected InternalEventAccummulator() {
            super(new Timer("topo-summary"), MAX_EVENTS, MAX_BATCH_MS, MAX_IDLE_MS);
        }

        @Override
        public void processEvents(List<Event> events) {
            try {
                if (summaryEvent != null) {
                    sendMessage(summmaryMessage(0));
                }
            } catch (Exception e) {
                log.warn("Unable to handle summary request due to {}", e.getMessage());
                log.debug("Boom!", e);
            }
        }
    }
}


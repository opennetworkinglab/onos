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
import org.onlab.onos.net.ConnectPoint;
import org.onlab.onos.net.Device;
import org.onlab.onos.net.Host;
import org.onlab.onos.net.HostId;
import org.onlab.onos.net.Link;
import org.onlab.onos.net.device.DeviceEvent;
import org.onlab.onos.net.device.DeviceListener;
import org.onlab.onos.net.flow.DefaultTrafficSelector;
import org.onlab.onos.net.flow.DefaultTrafficTreatment;
import org.onlab.onos.net.host.HostEvent;
import org.onlab.onos.net.host.HostListener;
import org.onlab.onos.net.intent.HostToHostIntent;
import org.onlab.onos.net.intent.Intent;
import org.onlab.onos.net.intent.IntentEvent;
import org.onlab.onos.net.intent.IntentListener;
import org.onlab.onos.net.intent.MultiPointToSinglePointIntent;
import org.onlab.onos.net.intent.OpticalConnectivityIntent;
import org.onlab.onos.net.intent.PathIntent;
import org.onlab.onos.net.intent.PointToPointIntent;
import org.onlab.onos.net.link.LinkEvent;
import org.onlab.onos.net.link.LinkListener;
import org.onlab.osgi.ServiceDirectory;

import java.io.IOException;
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
import static org.onlab.onos.net.host.HostEvent.Type.HOST_ADDED;
import static org.onlab.onos.net.intent.IntentState.INSTALLED;
import static org.onlab.onos.net.link.LinkEvent.Type.LINK_ADDED;

/**
 * Web socket capable of interacting with the GUI topology view.
 */
public class TopologyWebSocket
        extends TopologyMessages
        implements WebSocket.OnTextMessage, WebSocket.OnControl {

    private static final long MAX_AGE_MS = 15000;

    private static final byte PING = 0x9;
    private static final byte PONG = 0xA;
    private static final byte[] PING_DATA = new byte[]{(byte) 0xde, (byte) 0xad};

    private static final String APP_ID = "org.onlab.onos.gui";

    private static final long TRAFFIC_FREQUENCY_SEC = 5000;

    private final ApplicationId appId;

    private Connection connection;
    private FrameConnection control;

    private final ClusterEventListener clusterListener = new InternalClusterListener();
    private final DeviceListener deviceListener = new InternalDeviceListener();
    private final LinkListener linkListener = new InternalLinkListener();
    private final HostListener hostListener = new InternalHostListener();
    private final IntentListener intentListener = new InternalIntentListener();

    // Intents that are being monitored for the GUI
    private ObjectNode monitorRequest;
    private final Timer timer = new Timer("intent-traffic-monitor");
    private final TimerTask timerTask = new IntentTrafficMonitor();

    private long lastActive = System.currentTimeMillis();
    private boolean listenersRemoved = false;

    /**
     * Creates a new web-socket for serving data to GUI topology view.
     *
     * @param directory service directory
     */
    public TopologyWebSocket(ServiceDirectory directory) {
        super(directory);
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
        timer.schedule(timerTask, TRAFFIC_FREQUENCY_SEC, TRAFFIC_FREQUENCY_SEC);

        sendAllInstances();
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
            ObjectNode event = (ObjectNode) mapper.reader().readTree(data);
            String type = string(event, "event", "unknown");
            if (type.equals("requestDetails")) {
                requestDetails(event);
            } else if (type.equals("updateMeta")) {
                updateMetaUi(event);
            } else if (type.equals("addHostIntent")) {
                createHostIntent(event);
            } else if (type.equals("requestTraffic")) {
                requestTraffic(event);
            } else if (type.equals("cancelTraffic")) {
                cancelTraffic(event);
            }
        } catch (Exception e) {
            log.warn("Unable to parse GUI request {} due to {}", data, e);
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
    private void sendAllInstances() {
        for (ControllerNode node : clusterService.getNodes()) {
            sendMessage(instanceMessage(new ClusterEvent(INSTANCE_ADDED, node)));
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

        HostToHostIntent hostIntent = new HostToHostIntent(appId, one, two,
                                                           DefaultTrafficSelector.builder().build(),
                                                           DefaultTrafficTreatment.builder().build());
        monitorRequest = event;
        intentService.submit(hostIntent);
    }

    // Sends traffic message.
    private synchronized void requestTraffic(ObjectNode event) {
        ObjectNode payload = payload(event);
        long sid = number(event, "sid");
        monitorRequest = event;

        // Get the set of selected hosts and their intents.
        Set<Host> hosts = getHosts((ArrayNode) payload.path("ids"));
        Set<Intent> intents = findPathIntents(hosts);

        // If there is a hover node, include it in the hosts and find intents.
        String hover = string(payload, "hover");
        Set<Intent> hoverIntents;
        if (!isNullOrEmpty(hover)) {
            addHost(hosts, hostId(hover));
            hoverIntents = findPathIntents(hosts);
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
        monitorRequest = null;
    }

    // Finds all path (host-to-host or point-to-point) intents that pertains
    // to the given hosts.
    private Set<Intent> findPathIntents(Set<Host> hosts) {
        // Derive from this the set of edge connect points.
        Set<ConnectPoint> edgePoints = getEdgePoints(hosts);

        // Iterate over all intents and produce a set that contains only those
        // intents that target all selected hosts or derived edge connect points.
        return getIntents(hosts, edgePoints);
    }

    // Produces a set of intents that target all selected hosts or connect points.
    private Set<Intent> getIntents(Set<Host> hosts, Set<ConnectPoint> edgePoints) {
        Set<Intent> intents = new HashSet<>();
        if (hosts.isEmpty()) {
            return intents;
        }

        Set<OpticalConnectivityIntent> opticalIntents = new HashSet<>();

        for (Intent intent : intentService.getIntents()) {
            if (intentService.getIntentState(intent.id()) == INSTALLED) {
                boolean isRelevant = false;
                if (intent instanceof HostToHostIntent) {
                    isRelevant = isIntentRelevant((HostToHostIntent) intent, hosts);
                } else if (intent instanceof PointToPointIntent) {
                    isRelevant = isIntentRelevant((PointToPointIntent) intent, edgePoints);
                } else if (intent instanceof MultiPointToSinglePointIntent) {
                    isRelevant = isIntentRelevant((MultiPointToSinglePointIntent) intent, edgePoints);
                } else if (intent instanceof OpticalConnectivityIntent) {
                    opticalIntents.add((OpticalConnectivityIntent) intent);
                }
                // TODO: add other intents, e.g. SinglePointToMultiPointIntent

                if (isRelevant) {
                    intents.add(intent);
                }
            }
        }

        for (OpticalConnectivityIntent intent : opticalIntents) {
            if (isIntentRelevant(intent, intents)) {
                intents.add(intent);
            }
        }
        return intents;
    }

    // Indicates whether the specified intent involves all of the given hosts.
    private boolean isIntentRelevant(HostToHostIntent intent, Set<Host> hosts) {
        for (Host host : hosts) {
            HostId id = host.id();
            // Bail if intent does not involve this host.
            if (!id.equals(intent.one()) && !id.equals(intent.two())) {
                return false;
            }
        }
        return true;
    }

    // Indicates whether the specified intent involves all of the given edge points.
    private boolean isIntentRelevant(PointToPointIntent intent,
                                     Set<ConnectPoint> edgePoints) {
        for (ConnectPoint point : edgePoints) {
            // Bail if intent does not involve this edge point.
            if (!point.equals(intent.egressPoint()) &&
                    !point.equals(intent.ingressPoint())) {
                return false;
            }
        }
        return true;
    }

    // Indicates whether the specified intent involves all of the given edge points.
    private boolean isIntentRelevant(MultiPointToSinglePointIntent intent,
                                     Set<ConnectPoint> edgePoints) {
        for (ConnectPoint point : edgePoints) {
            // Bail if intent does not involve this edge point.
            if (!point.equals(intent.egressPoint()) &&
                    !intent.ingressPoints().contains(point)) {
                return false;
            }
        }
        return true;
    }

    // Indicates whether the specified intent involves all of the given edge points.
    private boolean isIntentRelevant(OpticalConnectivityIntent opticalIntent,
                                     Set<Intent> intents) {
        Link ccSrc = getFirstLink(opticalIntent.getSrcConnectPoint(), false);
        Link ccDst = getFirstLink(opticalIntent.getDst(), true);

        for (Intent intent : intents) {
            List<Intent> installables = intentService.getInstallableIntents(intent.id());
            for (Intent installable : installables) {
                if (installable instanceof PathIntent) {
                    List<Link> links = ((PathIntent) installable).path().links();
                    if (links.size() == 3) {
                        Link tunnel = links.get(1);
                        if (tunnel.src().equals(ccSrc.src()) &&
                                tunnel.dst().equals(ccDst.dst())) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    private Link getFirstLink(ConnectPoint point, boolean ingress) {
        for (Link link : linkService.getLinks(point)) {
            if (point.equals(ingress ? link.src() : link.dst())) {
                return link;
            }
        }
        return null;
    }

    // Produces a set of all host ids listed in the specified JSON array.
    private Set<Host> getHosts(ArrayNode array) {
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

    private void addHost(Set<Host> hosts, HostId hostId) {
        Host host = hostService.getHost(hostId);
        if (host != null) {
            hosts.add(host);
        }
    }

    // Produces a set of edge points from the specified set of hosts.
    private Set<ConnectPoint> getEdgePoints(Set<Host> hosts) {
        Set<ConnectPoint> edgePoints = new HashSet<>();
        for (Host host : hosts) {
            edgePoints.add(host.location());
        }
        return edgePoints;
    }


    // Adds all internal listeners.
    private void addListeners() {
        clusterService.addListener(clusterListener);
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
            sendMessage(instanceMessage(event));
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
            if (monitorRequest != null) {
                requestTraffic(monitorRequest);
            }
        }
    }

    private class IntentTrafficMonitor extends TimerTask {
        @Override
        public void run() {
            if (monitorRequest != null) {
                requestTraffic(monitorRequest);
            }
        }
    }
}


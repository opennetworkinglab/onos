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

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.eclipse.jetty.websocket.WebSocket;
import org.onlab.onos.cluster.ClusterEvent;
import org.onlab.onos.cluster.ClusterEventListener;
import org.onlab.onos.cluster.ControllerNode;
import org.onlab.onos.core.ApplicationId;
import org.onlab.onos.core.CoreService;
import org.onlab.onos.mastership.MastershipEvent;
import org.onlab.onos.mastership.MastershipListener;
import org.onlab.onos.net.Device;
import org.onlab.onos.net.Host;
import org.onlab.onos.net.HostId;
import org.onlab.onos.net.Link;
import org.onlab.onos.net.Path;
import org.onlab.onos.net.device.DeviceEvent;
import org.onlab.onos.net.device.DeviceListener;
import org.onlab.onos.net.flow.DefaultTrafficSelector;
import org.onlab.onos.net.flow.DefaultTrafficTreatment;
import org.onlab.onos.net.host.HostEvent;
import org.onlab.onos.net.host.HostListener;
import org.onlab.onos.net.intent.HostToHostIntent;
import org.onlab.onos.net.intent.Intent;
import org.onlab.onos.net.intent.IntentEvent;
import org.onlab.onos.net.intent.IntentId;
import org.onlab.onos.net.intent.IntentListener;
import org.onlab.onos.net.intent.PathIntent;
import org.onlab.onos.net.link.LinkEvent;
import org.onlab.onos.net.link.LinkListener;
import org.onlab.osgi.ServiceDirectory;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.onlab.onos.cluster.ClusterEvent.Type.INSTANCE_ADDED;
import static org.onlab.onos.net.DeviceId.deviceId;
import static org.onlab.onos.net.HostId.hostId;
import static org.onlab.onos.net.device.DeviceEvent.Type.DEVICE_ADDED;
import static org.onlab.onos.net.host.HostEvent.Type.HOST_ADDED;
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

    private final ApplicationId appId;

    private Connection connection;
    private FrameConnection control;

    private final ClusterEventListener clusterListener = new InternalClusterListener();
    private final DeviceListener deviceListener = new InternalDeviceListener();
    private final LinkListener linkListener = new InternalLinkListener();
    private final HostListener hostListener = new InternalHostListener();
    private final MastershipListener mastershipListener = new InternalMastershipListener();
    private final IntentListener intentListener = new InternalIntentListener();

    // Intents that are being monitored for the GUI
    private static Map<IntentId, Long> intentsToMonitor = new ConcurrentHashMap<>();

    private long lastActive = System.currentTimeMillis();

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
        if (connection.isOpen()) {
            removeListeners();
            connection.close();
        }
    }

    /**
     * Indicates if this connection is idle.
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

        sendAllInstances();
        sendAllDevices();
        sendAllLinks();
        sendAllHosts();
    }

    @Override
    public synchronized void onClose(int closeCode, String message) {
        if (connection.isOpen()) {
            removeListeners();
        }
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
            } else if (type.equals("requestPath")) {
                createHostIntent(event);
            } else if (type.equals("requestTraffic")) {
                sendTraffic(event);
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
        if (type.equals("device")) {
            sendMessage(deviceDetails(deviceId(string(payload, "id")),
                                      number(event, "sid")));
        } else if (type.equals("host")) {
            sendMessage(hostDetails(hostId(string(payload, "id")),
                                    number(event, "sid")));
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
        intentsToMonitor.put(hostIntent.id(), number(event, "sid"));
        intentService.submit(hostIntent);
    }

    // Sends traffic message.
    private void sendTraffic(ObjectNode event) {
        ObjectNode payload = payload(event);
        long id = number(event, "sid");
        IntentId intentId = IntentId.valueOf(payload.path("intentId").asLong());

        if (payload != null) {
            payload.put("traffic", true);
            sendMessage(envelope("showPath", id, payload));
        } else {
            sendMessage(warning(id, "No path found"));
        }
    }

    // Cancels sending traffic messages.
    private void cancelTraffic(ObjectNode event) {
        // TODO: implement this
    }


    // Adds all internal listeners.
    private void addListeners() {
        clusterService.addListener(clusterListener);
        deviceService.addListener(deviceListener);
        linkService.addListener(linkListener);
        hostService.addListener(hostListener);
        mastershipService.addListener(mastershipListener);
        intentService.addListener(intentListener);
    }

    // Removes all internal listeners.
    private void removeListeners() {
        clusterService.removeListener(clusterListener);
        deviceService.removeListener(deviceListener);
        linkService.removeListener(linkListener);
        hostService.removeListener(hostListener);
        mastershipService.removeListener(mastershipListener);
        intentService.removeListener(intentListener);
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

    // Mastership event listener.
    private class InternalMastershipListener implements MastershipListener {
        @Override
        public void event(MastershipEvent event) {
            // TODO: Is DeviceEvent.Type.DEVICE_MASTERSHIP_CHANGED the same?

        }
    }

    // Intent event listener.
    private class InternalIntentListener implements IntentListener {
        @Override
        public void event(IntentEvent event) {
            Intent intent = event.subject();
            Long sid = intentsToMonitor.get(intent.id());
            if (sid != null) {
                List<Intent> installable = intentService.getInstallableIntents(intent.id());
                if (installable != null && !installable.isEmpty()) {
                    PathIntent pathIntent = (PathIntent) installable.iterator().next();
                    Path path = pathIntent.path();
                    ObjectNode payload = pathMessage(path, "host")
                            .put("intentId", intent.id().toString());
                    sendMessage(envelope("showPath", sid, payload));
                }
            }
        }
    }

}


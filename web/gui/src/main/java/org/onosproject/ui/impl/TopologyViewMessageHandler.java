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
import com.google.common.collect.ImmutableSet;
import org.onlab.osgi.ServiceDirectory;
import org.onlab.util.AbstractAccumulator;
import org.onlab.util.Accumulator;
import org.onosproject.cluster.ClusterEvent;
import org.onosproject.cluster.ClusterEventListener;
import org.onosproject.cluster.ControllerNode;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.event.Event;
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
import org.onosproject.net.intent.IntentEvent;
import org.onosproject.net.intent.IntentListener;
import org.onosproject.net.intent.MultiPointToSinglePointIntent;
import org.onosproject.net.link.LinkEvent;
import org.onosproject.net.link.LinkListener;
import org.onosproject.ui.RequestHandler;
import org.onosproject.ui.UiConnection;
import org.onosproject.ui.impl.TrafficMonitor.Mode;
import org.onosproject.ui.topo.Highlights;
import org.onosproject.ui.topo.NodeSelection;
import org.onosproject.ui.topo.PropertyPanel;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;

import static java.util.concurrent.Executors.newSingleThreadExecutor;
import static org.onlab.util.Tools.groupedThreads;
import static org.onosproject.cluster.ClusterEvent.Type.INSTANCE_ADDED;
import static org.onosproject.net.DeviceId.deviceId;
import static org.onosproject.net.HostId.hostId;
import static org.onosproject.net.device.DeviceEvent.Type.*;
import static org.onosproject.net.host.HostEvent.Type.HOST_ADDED;
import static org.onosproject.net.link.LinkEvent.Type.LINK_ADDED;
import static org.onosproject.ui.JsonUtils.envelope;
import static org.onosproject.ui.topo.TopoJson.highlightsMessage;
import static org.onosproject.ui.topo.TopoJson.json;

/**
 * Web socket capable of interacting with the GUI topology view.
 */
public class TopologyViewMessageHandler extends TopologyViewMessageHandlerBase {

    // incoming event types
    private static final String REQ_DETAILS = "requestDetails";
    private static final String UPDATE_META = "updateMeta";
    private static final String ADD_HOST_INTENT = "addHostIntent";
    private static final String ADD_MULTI_SRC_INTENT = "addMultiSourceIntent";
    private static final String REQ_RELATED_INTENTS = "requestRelatedIntents";
    private static final String REQ_NEXT_INTENT = "requestNextRelatedIntent";
    private static final String REQ_PREV_INTENT = "requestPrevRelatedIntent";
    private static final String REQ_SEL_INTENT_TRAFFIC = "requestSelectedIntentTraffic";
    private static final String REQ_ALL_FLOW_TRAFFIC = "requestAllFlowTraffic";
    private static final String REQ_ALL_PORT_TRAFFIC = "requestAllPortTraffic";
    private static final String REQ_DEV_LINK_FLOWS = "requestDeviceLinkFlows";
    private static final String CANCEL_TRAFFIC = "cancelTraffic";
    private static final String REQ_SUMMARY = "requestSummary";
    private static final String CANCEL_SUMMARY = "cancelSummary";
    private static final String EQ_MASTERS = "equalizeMasters";
    private static final String SPRITE_LIST_REQ = "spriteListRequest";
    private static final String SPRITE_DATA_REQ = "spriteDataRequest";
    private static final String TOPO_START = "topoStart";
    private static final String TOPO_HEARTBEAT = "topoHeartbeat";
    private static final String TOPO_SELECT_OVERLAY = "topoSelectOverlay";
    private static final String TOPO_STOP = "topoStop";

    // outgoing event types
    private static final String SHOW_SUMMARY = "showSummary";
    private static final String SHOW_DETAILS = "showDetails";
    private static final String SPRITE_LIST_RESPONSE = "spriteListResponse";
    private static final String SPRITE_DATA_RESPONSE = "spriteDataResponse";
    private static final String UPDATE_INSTANCE = "updateInstance";

    // fields
    private static final String ID = "id";
    private static final String DEVICE = "device";
    private static final String HOST = "host";
    private static final String CLASS = "class";
    private static final String UNKNOWN = "unknown";
    private static final String ONE = "one";
    private static final String TWO = "two";
    private static final String SRC = "src";
    private static final String DST = "dst";
    private static final String DATA = "data";
    private static final String NAME = "name";
    private static final String NAMES = "names";
    private static final String ACTIVATE = "activate";
    private static final String DEACTIVATE = "deactivate";


    private static final String APP_ID = "org.onosproject.gui";

    private static final long TRAFFIC_PERIOD = 5000;
    private static final long SUMMARY_PERIOD = 30000;

    private static final Comparator<? super ControllerNode> NODE_COMPARATOR =
            (o1, o2) -> o1.id().toString().compareTo(o2.id().toString());


    private final Timer timer = new Timer("onos-topology-view");

    private static final int MAX_EVENTS = 1000;
    private static final int MAX_BATCH_MS = 5000;
    private static final int MAX_IDLE_MS = 1000;

    private ApplicationId appId;

    private final ClusterEventListener clusterListener = new InternalClusterListener();
    private final MastershipListener mastershipListener = new InternalMastershipListener();
    private final DeviceListener deviceListener = new InternalDeviceListener();
    private final LinkListener linkListener = new InternalLinkListener();
    private final HostListener hostListener = new InternalHostListener();
    private final IntentListener intentListener = new InternalIntentListener();
    private final FlowRuleListener flowListener = new InternalFlowListener();

    private final Accumulator<Event> eventAccummulator = new InternalEventAccummulator();
    private final ExecutorService msgSender =
            newSingleThreadExecutor(groupedThreads("onos/gui", "msg-sender"));

    private TopoOverlayCache overlayCache;
    private TrafficMonitor traffic;

    private TimerTask summaryTask = null;
    private boolean summaryRunning = false;

    private boolean listenersRemoved = false;


    @Override
    public void init(UiConnection connection, ServiceDirectory directory) {
        super.init(connection, directory);
        appId = directory.get(CoreService.class).registerApplication(APP_ID);
        traffic = new TrafficMonitor(TRAFFIC_PERIOD, servicesBundle, this);
    }

    @Override
    public void destroy() {
        cancelAllRequests();
        removeListeners();
        super.destroy();
    }

    @Override
    protected Collection<RequestHandler> createRequestHandlers() {
        return ImmutableSet.of(
                new TopoStart(),
                new TopoHeartbeat(),
                new TopoSelectOverlay(),
                new TopoStop(),
                new ReqSummary(),
                new CancelSummary(),
                new SpriteListReq(),
                new SpriteDataReq(),
                new RequestDetails(),
                new UpdateMeta(),
                new EqMasters(),

                // TODO: migrate traffic related to separate app
                new AddHostIntent(),
                new AddMultiSourceIntent(),

                new ReqAllFlowTraffic(),
                new ReqAllPortTraffic(),
                new ReqDevLinkFlows(),
                new ReqRelatedIntents(),
                new ReqNextIntent(),
                new ReqPrevIntent(),
                new ReqSelectedIntentTraffic(),

                new CancelTraffic()
        );
    }

    /**
     * Injects the topology overlay cache.
     *
     * @param overlayCache injected cache
     */
    void setOverlayCache(TopoOverlayCache overlayCache) {
        this.overlayCache = overlayCache;
    }

    // ==================================================================

    private final class TopoStart extends RequestHandler {
        private TopoStart() {
            super(TOPO_START);
        }

        @Override
        public void process(long sid, ObjectNode payload) {
            addListeners();
            sendAllInstances(null);
            sendAllDevices();
            sendAllLinks();
            sendAllHosts();
        }
    }

    private final class TopoHeartbeat extends RequestHandler {
        private TopoHeartbeat() {
            super(TOPO_HEARTBEAT);
        }

        @Override
        public void process(long sid, ObjectNode payload) {
            // place holder for now
        }
    }

    private final class TopoSelectOverlay extends RequestHandler {
        private TopoSelectOverlay() {
            super(TOPO_SELECT_OVERLAY);
        }

        @Override
        public void process(long sid, ObjectNode payload) {
            String deact = string(payload, DEACTIVATE);
            String act = string(payload, ACTIVATE);
            overlayCache.switchOverlay(deact, act);
        }
    }

    private final class TopoStop extends RequestHandler {
        private TopoStop() {
            super(TOPO_STOP);
        }

        @Override
        public void process(long sid, ObjectNode payload) {
            stopSummaryMonitoring();
            traffic.stopMonitoring();
        }
    }

    private final class ReqSummary extends RequestHandler {
        private ReqSummary() {
            super(REQ_SUMMARY);
        }

        @Override
        public void process(long sid, ObjectNode payload) {
            requestSummary(sid);
            startSummaryMonitoring();
        }
    }

    private final class CancelSummary extends RequestHandler {
        private CancelSummary() {
            super(CANCEL_SUMMARY);
        }

        @Override
        public void process(long sid, ObjectNode payload) {
            stopSummaryMonitoring();
        }
    }

    private final class SpriteListReq extends RequestHandler {
        private SpriteListReq() {
            super(SPRITE_LIST_REQ);
        }

        @Override
        public void process(long sid, ObjectNode payload) {
            ObjectNode root = objectNode();
            ArrayNode names = arrayNode();
            get(SpriteService.class).getNames().forEach(names::add);
            root.set(NAMES, names);
            sendMessage(SPRITE_LIST_RESPONSE, sid, root);
        }
    }

    private final class SpriteDataReq extends RequestHandler {
        private SpriteDataReq() {
            super(SPRITE_DATA_REQ);
        }

        @Override
        public void process(long sid, ObjectNode payload) {
            String name = string(payload, NAME);
            ObjectNode root = objectNode();
            root.set(DATA, get(SpriteService.class).get(name));
            sendMessage(SPRITE_DATA_RESPONSE, sid, root);
        }
    }

    private final class RequestDetails extends RequestHandler {
        private RequestDetails() {
            super(REQ_DETAILS);
        }

        @Override
        public void process(long sid, ObjectNode payload) {
            String type = string(payload, CLASS, UNKNOWN);
            String id = string(payload, ID);
            PropertyPanel pp = null;

            if (type.equals(DEVICE)) {
                pp = deviceDetails(deviceId(id), sid);
                overlayCache.currentOverlay().modifyDeviceDetails(pp);
            } else if (type.equals(HOST)) {
                pp = hostDetails(hostId(id), sid);
                overlayCache.currentOverlay().modifyHostDetails(pp);
            }

            sendMessage(envelope(SHOW_DETAILS, sid, json(pp)));
        }
    }

    private final class UpdateMeta extends RequestHandler {
        private UpdateMeta() {
            super(UPDATE_META);
        }

        @Override
        public void process(long sid, ObjectNode payload) {
            updateMetaUi(payload);
        }
    }

    private final class EqMasters extends RequestHandler {
        private EqMasters() {
            super(EQ_MASTERS);
        }

        @Override
        public void process(long sid, ObjectNode payload) {
            directory.get(MastershipAdminService.class).balanceRoles();
        }
    }


    // ========= -----------------------------------------------------------------

    // === TODO: move traffic related classes to traffic app

    private final class AddHostIntent extends RequestHandler {
        private AddHostIntent() {
            super(ADD_HOST_INTENT);
        }

        @Override
        public void process(long sid, ObjectNode payload) {
            // TODO: add protection against device ids and non-existent hosts.
            HostId one = hostId(string(payload, ONE));
            HostId two = hostId(string(payload, TWO));

            HostToHostIntent intent = HostToHostIntent.builder()
                    .appId(appId)
                    .one(one)
                    .two(two)
                    .build();

            intentService.submit(intent);
            if (overlayCache.isActive(TrafficOverlay.TRAFFIC_ID)) {
                traffic.monitor(intent);
            }
        }
    }

    private final class AddMultiSourceIntent extends RequestHandler {
        private AddMultiSourceIntent() {
            super(ADD_MULTI_SRC_INTENT);
        }

        @Override
        public void process(long sid, ObjectNode payload) {
            // TODO: add protection against device ids and non-existent hosts.
            Set<HostId> src = getHostIds((ArrayNode) payload.path(SRC));
            HostId dst = hostId(string(payload, DST));
            Host dstHost = hostService.getHost(dst);

            Set<ConnectPoint> ingressPoints = getHostLocations(src);

            // FIXME: clearly, this is not enough
            TrafficSelector selector = DefaultTrafficSelector.builder()
                    .matchEthDst(dstHost.mac()).build();
            TrafficTreatment treatment = DefaultTrafficTreatment.emptyTreatment();

            MultiPointToSinglePointIntent intent =
                    MultiPointToSinglePointIntent.builder()
                            .appId(appId)
                            .selector(selector)
                            .treatment(treatment)
                            .ingressPoints(ingressPoints)
                            .egressPoint(dstHost.location())
                            .build();

            intentService.submit(intent);
            if (overlayCache.isActive(TrafficOverlay.TRAFFIC_ID)) {
                traffic.monitor(intent);
            }
        }
    }

    // ========= -----------------------------------------------------------------

    private final class ReqAllFlowTraffic extends RequestHandler {
        private ReqAllFlowTraffic() {
            super(REQ_ALL_FLOW_TRAFFIC);
        }

        @Override
        public void process(long sid, ObjectNode payload) {
            traffic.monitor(Mode.ALL_FLOW_TRAFFIC);
        }
    }

    private final class ReqAllPortTraffic extends RequestHandler {
        private ReqAllPortTraffic() {
            super(REQ_ALL_PORT_TRAFFIC);
        }

        @Override
        public void process(long sid, ObjectNode payload) {
            traffic.monitor(Mode.ALL_PORT_TRAFFIC);
        }
    }

    private final class ReqDevLinkFlows extends RequestHandler {
        private ReqDevLinkFlows() {
            super(REQ_DEV_LINK_FLOWS);
        }

        @Override
        public void process(long sid, ObjectNode payload) {
            NodeSelection nodeSelection =
                    new NodeSelection(payload, deviceService, hostService);
            traffic.monitor(Mode.DEV_LINK_FLOWS, nodeSelection);
        }
    }

    private final class ReqRelatedIntents extends RequestHandler {
        private ReqRelatedIntents() {
            super(REQ_RELATED_INTENTS);
        }

        @Override
        public void process(long sid, ObjectNode payload) {
            NodeSelection nodeSelection =
                    new NodeSelection(payload, deviceService, hostService);
            traffic.monitor(Mode.RELATED_INTENTS, nodeSelection);
        }
    }

    private final class ReqNextIntent extends RequestHandler {
        private ReqNextIntent() {
            super(REQ_NEXT_INTENT);
        }

        @Override
        public void process(long sid, ObjectNode payload) {
            traffic.selectNextIntent();
        }
    }

    private final class ReqPrevIntent extends RequestHandler {
        private ReqPrevIntent() {
            super(REQ_PREV_INTENT);
        }

        @Override
        public void process(long sid, ObjectNode payload) {
            traffic.selectPreviousIntent();
        }
    }

    private final class ReqSelectedIntentTraffic extends RequestHandler {
        private ReqSelectedIntentTraffic() {
            super(REQ_SEL_INTENT_TRAFFIC);
        }

        @Override
        public void process(long sid, ObjectNode payload) {
            traffic.monitor(Mode.SELECTED_INTENT);
        }
    }

    private final class CancelTraffic extends RequestHandler {
        private CancelTraffic() {
            super(CANCEL_TRAFFIC);
        }

        @Override
        public void process(long sid, ObjectNode payload) {
            traffic.stopMonitoring();
        }
    }

    //=======================================================================

    // Converts highlights to JSON format and sends the message to the client
    protected void sendHighlights(Highlights highlights) {
        sendMessage(highlightsMessage(highlights));
    }

    // Subscribes for summary messages.
    private synchronized void requestSummary(long sid) {
        PropertyPanel pp = summmaryMessage(sid);
        overlayCache.currentOverlay().modifySummary(pp);
        sendMessage(envelope(SHOW_SUMMARY, sid, json(pp)));
    }


    private void cancelAllRequests() {
        stopSummaryMonitoring();
        traffic.stopMonitoring();
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


    private synchronized void startSummaryMonitoring() {
        stopSummaryMonitoring();
        summaryTask = new SummaryMonitor();
        timer.schedule(summaryTask, SUMMARY_PERIOD, SUMMARY_PERIOD);
        summaryRunning = true;
    }

    private synchronized void stopSummaryMonitoring() {
        if (summaryTask != null) {
            summaryTask.cancel();
            summaryTask = null;
        }
        summaryRunning = false;
    }


    // Adds all internal listeners.
    private synchronized void addListeners() {
        listenersRemoved = false;
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
            msgSender.execute(() -> sendMessage(instanceMessage(event, null)));
        }
    }

    // Mastership change listener
    private class InternalMastershipListener implements MastershipListener {
        @Override
        public void event(MastershipEvent event) {
            msgSender.execute(() -> {
                sendAllInstances(UPDATE_INSTANCE);
                Device device = deviceService.getDevice(event.subject());
                if (device != null) {
                    sendMessage(deviceMessage(new DeviceEvent(DEVICE_UPDATED, device)));
                }
            });
        }
    }

    // Device event listener.
    private class InternalDeviceListener implements DeviceListener {
        @Override
        public void event(DeviceEvent event) {
            if (event.type() != PORT_STATS_UPDATED) {
                msgSender.execute(() -> sendMessage(deviceMessage(event)));
                eventAccummulator.add(event);
            }
        }
    }

    // Link event listener.
    private class InternalLinkListener implements LinkListener {
        @Override
        public void event(LinkEvent event) {
            msgSender.execute(() -> sendMessage(linkMessage(event)));
            eventAccummulator.add(event);
        }
    }

    // Host event listener.
    private class InternalHostListener implements HostListener {
        @Override
        public void event(HostEvent event) {
            msgSender.execute(() -> sendMessage(hostMessage(event)));
            eventAccummulator.add(event);
        }
    }

    // Intent event listener.
    private class InternalIntentListener implements IntentListener {
        @Override
        public void event(IntentEvent event) {
            msgSender.execute(traffic::pokeIntent);
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


    // === SUMMARY MONITORING

    // Periodic update of the summary information
    private class SummaryMonitor extends TimerTask {
        @Override
        public void run() {
            try {
                if (summaryRunning) {
                    msgSender.execute(() -> requestSummary(0));
                }
            } catch (Exception e) {
                log.warn("Unable to handle summary request due to {}", e.getMessage());
                log.warn("Boom!", e);
            }
        }
    }

    // Accumulates events to drive methodic update of the summary pane.
    private class InternalEventAccummulator extends AbstractAccumulator<Event> {
        protected InternalEventAccummulator() {
            super(new Timer("topo-summary"), MAX_EVENTS, MAX_BATCH_MS, MAX_IDLE_MS);
        }

        @Override
        public void processItems(List<Event> items) {
            // Start-of-Debugging -- Keep in until ONOS-2572 is fixed for reals
            long now = System.currentTimeMillis();
            String me = this.toString();
            String miniMe = me.replaceAll("^.*@", "me@");
            log.debug("Time: {}; this: {}, processing items ({} events)",
                      now, miniMe, items.size());
            // End-of-Debugging

            try {
                if (summaryRunning) {
                    msgSender.execute(() -> requestSummary(0));
                }
            } catch (Exception e) {
                log.warn("Unable to handle summary request due to {}", e.getMessage());
                log.debug("Boom!", e);
            }
        }
    }
}

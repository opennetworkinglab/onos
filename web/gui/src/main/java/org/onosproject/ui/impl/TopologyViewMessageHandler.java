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
import org.onosproject.net.intent.Intent;
import org.onosproject.net.intent.IntentEvent;
import org.onosproject.net.intent.IntentListener;
import org.onosproject.net.intent.MultiPointToSinglePointIntent;
import org.onosproject.net.link.LinkEvent;
import org.onosproject.net.link.LinkListener;
import org.onosproject.ui.JsonUtils;
import org.onosproject.ui.RequestHandler;
import org.onosproject.ui.UiConnection;
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

import static com.google.common.base.Strings.isNullOrEmpty;
import static java.util.concurrent.Executors.newSingleThreadExecutor;
import static org.onlab.util.Tools.groupedThreads;
import static org.onosproject.cluster.ClusterEvent.Type.INSTANCE_ADDED;
import static org.onosproject.net.DeviceId.deviceId;
import static org.onosproject.net.HostId.hostId;
import static org.onosproject.net.device.DeviceEvent.Type.*;
import static org.onosproject.net.host.HostEvent.Type.HOST_ADDED;
import static org.onosproject.net.link.LinkEvent.Type.LINK_ADDED;

/**
 * Web socket capable of interacting with the GUI topology view.
 */
public class TopologyViewMessageHandler extends TopologyViewMessageHandlerBase {

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


    private static final String APP_ID = "org.onosproject.gui";

    private static final long TRAFFIC_FREQUENCY = 5000;
    private static final long SUMMARY_FREQUENCY = 30000;

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

    private TimerTask trafficTask = null;
    private TrafficEvent trafficEvent = null;

    private TimerTask summaryTask = null;
    private boolean summaryRunning = false;

    private boolean listenersRemoved = false;

    private TopologyViewIntentFilter intentFilter;

    // Current selection context
    private Set<Host> selectedHosts;
    private Set<Device> selectedDevices;
    private List<Intent> selectedIntents;
    private int currentIntentIndex = -1;


    @Override
    public void init(UiConnection connection, ServiceDirectory directory) {
        super.init(connection, directory);
        intentFilter = new TopologyViewIntentFilter(intentService, deviceService,
                                                    hostService, linkService);
        appId = directory.get(CoreService.class).registerApplication(APP_ID);
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
                new ReqRelatedIntents(),
                new ReqNextIntent(),
                new ReqPrevIntent(),
                new ReqSelectedIntentTraffic(),
                new ReqAllFlowTraffic(),
                new ReqAllPortTraffic(),
                new ReqDevLinkFlows(),
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

    /**
     * @deprecated in Cardinal Release
     */
    @Deprecated
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

    /**
     * @deprecated in Cardinal Release
     */
    @Deprecated
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
            String deact = string(payload, "deactivate");
            String act = string(payload, "activate");
            overlayCache.switchOverlay(deact, act);
        }
    }

    /**
     * @deprecated in Cardinal Release
     */
    @Deprecated
    private final class TopoStop extends RequestHandler {
        private TopoStop() {
            super(TOPO_STOP);
        }

        @Override
        public void process(long sid, ObjectNode payload) {
            stopSummaryMonitoring();
            stopTrafficMonitoring();
        }
    }

    /**
     * @deprecated in Cardinal Release
     */
    @Deprecated
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
            root.set("names", names);
            sendMessage("spriteListResponse", sid, root);
        }
    }

    private final class SpriteDataReq extends RequestHandler {
        private SpriteDataReq() {
            super(SPRITE_DATA_REQ);
        }

        @Override
        public void process(long sid, ObjectNode payload) {
            String name = string(payload, "name");
            ObjectNode root = objectNode();
            root.set("data", get(SpriteService.class).get(name));
            sendMessage("spriteDataResponse", sid, root);
        }
    }

    private final class RequestDetails extends RequestHandler {
        private RequestDetails() {
            super(REQ_DETAILS);
        }

        @Override
        public void process(long sid, ObjectNode payload) {
            String type = string(payload, "class", "unknown");
            String id = string(payload, "id");

            if (type.equals("device")) {
                sendMessage(deviceDetails(deviceId(id), sid));
            } else if (type.equals("host")) {
                sendMessage(hostDetails(hostId(id), sid));
            }
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

    // === TODO: move traffic related classes to traffic app

    private final class AddHostIntent extends RequestHandler {
        private AddHostIntent() {
            super(ADD_HOST_INTENT);
        }

        @Override
        public void process(long sid, ObjectNode payload) {
            // TODO: add protection against device ids and non-existent hosts.
            HostId one = hostId(string(payload, "one"));
            HostId two = hostId(string(payload, "two"));

            HostToHostIntent intent = HostToHostIntent.builder()
                    .appId(appId)
                    .one(one)
                    .two(two)
                    .build();

            intentService.submit(intent);
            startMonitoringIntent(intent);
        }
    }

    private final class AddMultiSourceIntent extends RequestHandler {
        private AddMultiSourceIntent() {
            super(ADD_MULTI_SRC_INTENT);
        }

        @Override
        public void process(long sid, ObjectNode payload) {
            // TODO: add protection against device ids and non-existent hosts.
            Set<HostId> src = getHostIds((ArrayNode) payload.path("src"));
            HostId dst = hostId(string(payload, "dst"));
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
            startMonitoringIntent(intent);
        }
    }

    private final class ReqRelatedIntents extends RequestHandler {
        private ReqRelatedIntents() {
            super(REQ_RELATED_INTENTS);
        }

        @Override
        public void process(long sid, ObjectNode payload) {
            // Cancel any other traffic monitoring mode.
            stopTrafficMonitoring();

            if (!payload.has("ids")) {
                return;
            }

            // Get the set of selected hosts and their intents.
            ArrayNode ids = (ArrayNode) payload.path("ids");
            selectedHosts = getHosts(ids);
            selectedDevices = getDevices(ids);
            selectedIntents = intentFilter.findPathIntents(
                    selectedHosts, selectedDevices, intentService.getIntents());
            currentIntentIndex = -1;

            if (haveSelectedIntents()) {
                // Send a message to highlight all links of all monitored intents.
                sendMessage(trafficMessage(new TrafficClass("primary", selectedIntents)));
            }

            // TODO: Re-introduce once the client click vs hover gesture stuff is sorted out.
//        String hover = string(payload, "hover");
//        if (!isNullOrEmpty(hover)) {
//            // If there is a hover node, include it in the selection and find intents.
//            processHoverExtendedSelection(sid, hover);
//        }
        }
    }

    private final class ReqNextIntent extends RequestHandler {
        private ReqNextIntent() {
            super(REQ_NEXT_INTENT);
        }

        @Override
        public void process(long sid, ObjectNode payload) {
            stopTrafficMonitoring();
            requestAnotherRelatedIntent(+1);
        }
    }

    private final class ReqPrevIntent extends RequestHandler {
        private ReqPrevIntent() {
            super(REQ_PREV_INTENT);
        }

        @Override
        public void process(long sid, ObjectNode payload) {
            stopTrafficMonitoring();
            requestAnotherRelatedIntent(-1);
        }
    }

    private final class ReqSelectedIntentTraffic extends RequestHandler {
        private ReqSelectedIntentTraffic() {
            super(REQ_SEL_INTENT_TRAFFIC);
        }

        @Override
        public void process(long sid, ObjectNode payload) {
            trafficEvent = new TrafficEvent(TrafficEvent.Type.SEL_INTENT, payload);
            requestSelectedIntentTraffic();
            startTrafficMonitoring();
        }
    }

    private final class ReqAllFlowTraffic extends RequestHandler {
        private ReqAllFlowTraffic() {
            super(REQ_ALL_FLOW_TRAFFIC);
        }

        @Override
        public void process(long sid, ObjectNode payload) {
            trafficEvent = new TrafficEvent(TrafficEvent.Type.ALL_FLOW_TRAFFIC, payload);
            requestAllFlowTraffic();
        }
    }

    private final class ReqAllPortTraffic extends RequestHandler {
        private ReqAllPortTraffic() {
            super(REQ_ALL_PORT_TRAFFIC);
        }

        @Override
        public void process(long sid, ObjectNode payload) {
            trafficEvent = new TrafficEvent(TrafficEvent.Type.ALL_PORT_TRAFFIC, payload);
            requestAllPortTraffic();
        }
    }

    private final class ReqDevLinkFlows extends RequestHandler {
        private ReqDevLinkFlows() {
            super(REQ_DEV_LINK_FLOWS);
        }

        @Override
        public void process(long sid, ObjectNode payload) {
            trafficEvent = new TrafficEvent(TrafficEvent.Type.DEV_LINK_FLOWS, payload);
            requestDeviceLinkFlows(payload);
        }
    }

    private final class CancelTraffic extends RequestHandler {
        private CancelTraffic() {
            super(CANCEL_TRAFFIC);
        }

        @Override
        public void process(long sid, ObjectNode payload) {
            selectedIntents = null;
            sendMessage(trafficMessage());
            stopTrafficMonitoring();
        }
    }

    //=======================================================================


    // Sends the specified data to the client.
    protected synchronized void sendMessage(ObjectNode data) {
        UiConnection connection = connection();
        if (connection != null) {
            connection.sendMessage(data);
        }
    }

    // Subscribes for summary messages.
    private synchronized void requestSummary(long sid) {
        PropertyPanel pp = summmaryMessage(sid);
        overlayCache.currentOverlay().modifySummary(pp);
        ObjectNode json = JsonUtils.envelope("showSummary", sid, json(pp));
        sendMessage(json);
    }


    private void cancelAllRequests() {
        stopSummaryMonitoring();
        stopTrafficMonitoring();
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


    private synchronized void startMonitoringIntent(Intent intent) {
        selectedHosts = new HashSet<>();
        selectedDevices = new HashSet<>();
        selectedIntents = new ArrayList<>();
        selectedIntents.add(intent);
        currentIntentIndex = -1;
        requestAnotherRelatedIntent(+1);
        requestSelectedIntentTraffic();
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


    private synchronized void startTrafficMonitoring() {
        stopTrafficMonitoring();
        trafficTask = new TrafficMonitor();
        timer.schedule(trafficTask, TRAFFIC_FREQUENCY, TRAFFIC_FREQUENCY);
    }

    private synchronized void stopTrafficMonitoring() {
        if (trafficTask != null) {
            trafficTask.cancel();
            trafficTask = null;
        }
    }

    // Subscribes for flow traffic messages.
    private synchronized void requestAllFlowTraffic() {
        startTrafficMonitoring();
        sendMessage(trafficSummaryMessage(StatsType.FLOW));
    }

    // Subscribes for port traffic messages.
    private synchronized void requestAllPortTraffic() {
        startTrafficMonitoring();
        sendMessage(trafficSummaryMessage(StatsType.PORT));
    }

    private void requestDeviceLinkFlows(ObjectNode payload) {
        startTrafficMonitoring();

        // Get the set of selected hosts and their intents.
        ArrayNode ids = (ArrayNode) payload.path("ids");
        Set<Host> hosts = new HashSet<>();
        Set<Device> devices = getDevices(ids);

        // If there is a hover node, include it in the hosts and find intents.
        String hover = JsonUtils.string(payload, "hover");
        if (!isNullOrEmpty(hover)) {
            addHover(hosts, devices, hover);
        }
        sendMessage(flowSummaryMessage(devices));
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
        sendMessage(trafficMessage(new TrafficClass("primary", primary),
                                   new TrafficClass("secondary", secondary)));
    }

    // Requests next or previous related intent.
    private void requestAnotherRelatedIntent(int offset) {
        if (haveSelectedIntents()) {
            currentIntentIndex = currentIntentIndex + offset;
            if (currentIntentIndex < 0) {
                currentIntentIndex = selectedIntents.size() - 1;
            } else if (currentIntentIndex >= selectedIntents.size()) {
                currentIntentIndex = 0;
            }
            sendSelectedIntent();
        }
    }

    // Sends traffic information on the related intents with the currently
    // selected intent highlighted.
    private void sendSelectedIntent() {
        Intent selectedIntent = selectedIntents.get(currentIntentIndex);
        log.info("Requested next intent {}", selectedIntent.id());

        Set<Intent> primary = new HashSet<>();
        primary.add(selectedIntent);

        Set<Intent> secondary = new HashSet<>(selectedIntents);
        secondary.remove(selectedIntent);

        // Send a message to highlight all links of the selected intent.
        sendMessage(trafficMessage(new TrafficClass("primary", primary),
                                   new TrafficClass("secondary", secondary)));
    }

    // Requests monitoring of traffic for the selected intent.
    private void requestSelectedIntentTraffic() {
        if (haveSelectedIntents()) {
            if (currentIntentIndex < 0) {
                currentIntentIndex = 0;
            }
            Intent selectedIntent = selectedIntents.get(currentIntentIndex);
            log.info("Requested traffic for selected {}", selectedIntent.id());

            Set<Intent> primary = new HashSet<>();
            primary.add(selectedIntent);

            // Send a message to highlight all links of the selected intent.
            sendMessage(trafficMessage(new TrafficClass("primary", primary, true)));
        }
    }

    private synchronized void startSummaryMonitoring() {
        stopSummaryMonitoring();
        summaryTask = new SummaryMonitor();
        timer.schedule(summaryTask, SUMMARY_FREQUENCY, SUMMARY_FREQUENCY);
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
                sendAllInstances("updateInstance");
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
            if (trafficTask != null) {
                msgSender.execute(TopologyViewMessageHandler.this::requestSelectedIntentTraffic);
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

    // encapsulate
    private static class TrafficEvent {
        enum Type {
            ALL_FLOW_TRAFFIC, ALL_PORT_TRAFFIC, DEV_LINK_FLOWS, SEL_INTENT
        }

        private final Type type;
        private final ObjectNode payload;

        TrafficEvent(Type type, ObjectNode payload) {
            this.type = type;
            this.payload = payload;
        }
    }

    // Periodic update of the traffic information
    private class TrafficMonitor extends TimerTask {
        @Override
        public void run() {
            try {
                if (trafficEvent != null) {
                    switch (trafficEvent.type) {
                        case ALL_FLOW_TRAFFIC:
                            requestAllFlowTraffic();
                            break;
                        case ALL_PORT_TRAFFIC:
                            requestAllPortTraffic();
                            break;
                        case DEV_LINK_FLOWS:
                            requestDeviceLinkFlows(trafficEvent.payload);
                            break;
                        case SEL_INTENT:
                            requestSelectedIntentTraffic();
                            break;
                        default:
                            // nothing to do
                            break;
                    }
                }
            } catch (Exception e) {
                log.warn("Unable to handle traffic request due to {}", e.getMessage());
                log.warn("Boom!", e);
            }
        }
    }

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

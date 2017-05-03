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
import com.google.common.collect.ImmutableSet;
import org.onlab.osgi.ServiceDirectory;
import org.onlab.util.AbstractAccumulator;
import org.onlab.util.Accumulator;
import org.onosproject.cluster.ClusterEvent;
import org.onosproject.cluster.ClusterEventListener;
import org.onosproject.cluster.ControllerNode;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.core.DefaultApplicationId;
import org.onosproject.event.Event;
import org.onosproject.mastership.MastershipEvent;
import org.onosproject.mastership.MastershipListener;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
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
import org.onosproject.net.intent.IntentService;
import org.onosproject.net.intent.IntentState;
import org.onosproject.net.intent.Key;
import org.onosproject.net.intent.MultiPointToSinglePointIntent;
import org.onosproject.net.link.LinkEvent;
import org.onosproject.net.link.LinkListener;
import org.onosproject.ui.JsonUtils;
import org.onosproject.ui.RequestHandler;
import org.onosproject.ui.UiConnection;
import org.onosproject.ui.impl.TrafficMonitorBase.Mode;
import org.onosproject.ui.topo.Highlights;
import org.onosproject.ui.topo.NodeSelection;
import org.onosproject.ui.topo.PropertyPanel;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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
import static org.onosproject.ui.JsonUtils.string;
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
    private static final String REMOVE_INTENT = "removeIntent";
    private static final String REMOVE_INTENTS = "removeIntents";
    private static final String RESUBMIT_INTENT = "resubmitIntent";
    private static final String ADD_MULTI_SRC_INTENT = "addMultiSourceIntent";
    private static final String REQ_RELATED_INTENTS = "requestRelatedIntents";
    private static final String REQ_NEXT_INTENT = "requestNextRelatedIntent";
    private static final String REQ_PREV_INTENT = "requestPrevRelatedIntent";
    private static final String REQ_SEL_INTENT_TRAFFIC = "requestSelectedIntentTraffic";
    private static final String SEL_INTENT = "selectIntent";
    private static final String REQ_ALL_TRAFFIC = "requestAllTraffic";
    private static final String REQ_DEV_LINK_FLOWS = "requestDeviceLinkFlows";
    private static final String CANCEL_TRAFFIC = "cancelTraffic";
    private static final String REQ_SUMMARY = "requestSummary";
    private static final String CANCEL_SUMMARY = "cancelSummary";
    private static final String EQ_MASTERS = "equalizeMasters";
    private static final String SPRITE_LIST_REQ = "spriteListRequest";
    private static final String SPRITE_DATA_REQ = "spriteDataRequest";
    private static final String TOPO_START = "topoStart";
    private static final String TOPO_SELECT_OVERLAY = "topoSelectOverlay";
    private static final String TOPO_STOP = "topoStop";
    private static final String SEL_PROTECTED_INTENT = "selectProtectedIntent";
    private static final String CANCEL_PROTECTED_INTENT_HIGHLIGHT = "cancelProtectedIntentHighlight";

    // outgoing event types
    private static final String SHOW_SUMMARY = "showSummary";
    private static final String SHOW_DETAILS = "showDetails";
    private static final String SPRITE_LIST_RESPONSE = "spriteListResponse";
    private static final String SPRITE_DATA_RESPONSE = "spriteDataResponse";
    private static final String UPDATE_INSTANCE = "updateInstance";
    private static final String TOPO_START_DONE = "topoStartDone";

    // fields
    private static final String PAYLOAD = "payload";
    private static final String EXTRA = "extra";
    private static final String ID = "id";
    private static final String KEY = "key";
    private static final String APP_ID = "appId";
    private static final String APP_NAME = "appName";
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
    private static final String PURGE = "purge";
    private static final String TRAFFIC_TYPE = "trafficType";

    // field values
    private static final String FLOW_STATS_BYTES = "flowStatsBytes";
    private static final String PORT_STATS_BIT_SEC = "portStatsBitSec";
    private static final String PORT_STATS_PKT_SEC = "portStatsPktSec";

    private static final String MY_APP_ID = "org.onosproject.gui";

    private static final long TRAFFIC_PERIOD = 5000;
    private static final long SUMMARY_PERIOD = 30000;

    private static final Comparator<? super ControllerNode> NODE_COMPARATOR =
            Comparator.comparing(o -> o.id().toString());


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
            newSingleThreadExecutor(groupedThreads("onos/gui", "msg-sender", log));

    private TopoOverlayCache overlayCache;
    private TrafficMonitor traffic;
    private ProtectedIntentMonitor protectedIntentMonitor;

    private TimerTask summaryTask = null;
    private boolean summaryRunning = false;

    private volatile boolean listenersRemoved = false;


    @Override
    public void init(UiConnection connection, ServiceDirectory directory) {
        super.init(connection, directory);
        appId = directory.get(CoreService.class).registerApplication(MY_APP_ID);
        traffic = new TrafficMonitor(TRAFFIC_PERIOD, services, this);
        protectedIntentMonitor = new ProtectedIntentMonitor(TRAFFIC_PERIOD, services, this);
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
                new RemoveIntent(),
                new ResubmitIntent(),
                new RemoveIntents(),

                new ReqAllTraffic(),
                new ReqDevLinkFlows(),
                new ReqRelatedIntents(),
                new ReqNextIntent(),
                new ReqPrevIntent(),
                new ReqSelectedIntentTraffic(),
                new SelIntent(),
                new SelProtectedIntent(),

                new CancelTraffic(),
                new CancelProtectedIntentHighlight()
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
        public void process(ObjectNode payload) {
            addListeners();
            sendAllInstances(null);
            sendAllDevices();
            sendAllLinks();
            sendAllHosts();
            sendTopoStartDone();
        }
    }

    private final class TopoSelectOverlay extends RequestHandler {
        private TopoSelectOverlay() {
            super(TOPO_SELECT_OVERLAY);
        }

        @Override
        public void process(ObjectNode payload) {
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
        public void process(ObjectNode payload) {
            removeListeners();
            stopSummaryMonitoring();
            traffic.stopMonitoring();
        }
    }

    private final class ReqSummary extends RequestHandler {
        private ReqSummary() {
            super(REQ_SUMMARY);
        }

        @Override
        public void process(ObjectNode payload) {
            requestSummary();
            startSummaryMonitoring();
        }
    }

    private final class CancelSummary extends RequestHandler {
        private CancelSummary() {
            super(CANCEL_SUMMARY);
        }

        @Override
        public void process(ObjectNode payload) {
            stopSummaryMonitoring();
        }
    }

    private final class SpriteListReq extends RequestHandler {
        private SpriteListReq() {
            super(SPRITE_LIST_REQ);
        }

        @Override
        public void process(ObjectNode payload) {
            ObjectNode root = objectNode();
            ArrayNode names = arrayNode();
            get(SpriteService.class).getNames().forEach(names::add);
            root.set(NAMES, names);
            sendMessage(SPRITE_LIST_RESPONSE, root);
        }
    }

    private final class SpriteDataReq extends RequestHandler {
        private SpriteDataReq() {
            super(SPRITE_DATA_REQ);
        }

        @Override
        public void process(ObjectNode payload) {
            String name = string(payload, NAME);
            ObjectNode root = objectNode();
            root.set(DATA, get(SpriteService.class).get(name));
            sendMessage(SPRITE_DATA_RESPONSE, root);
        }
    }

    private final class RequestDetails extends RequestHandler {
        private RequestDetails() {
            super(REQ_DETAILS);
        }

        @Override
        public void process(ObjectNode payload) {
            String type = string(payload, CLASS, UNKNOWN);
            String id = string(payload, ID);
            PropertyPanel pp = null;

            if (type.equals(DEVICE)) {
                DeviceId did = deviceId(id);
                pp = deviceDetails(did);
                overlayCache.currentOverlay().modifyDeviceDetails(pp, did);
            } else if (type.equals(HOST)) {
                HostId hid = hostId(id);
                pp = hostDetails(hid);
                overlayCache.currentOverlay().modifyHostDetails(pp, hid);
            }

            sendMessage(envelope(SHOW_DETAILS, json(pp)));
        }
    }

    private final class UpdateMeta extends RequestHandler {
        private UpdateMeta() {
            super(UPDATE_META);
        }

        @Override
        public void process(ObjectNode payload) {
            updateMetaUi(payload);
        }
    }

    private final class EqMasters extends RequestHandler {
        private EqMasters() {
            super(EQ_MASTERS);
        }

        @Override
        public void process(ObjectNode payload) {
            services.mastershipAdmin().balanceRoles();
        }
    }


    // ========= -----------------------------------------------------------------

    // === TODO: move traffic related classes to traffic app

    private final class AddHostIntent extends RequestHandler {
        private AddHostIntent() {
            super(ADD_HOST_INTENT);
        }

        @Override
        public void process(ObjectNode payload) {
            // TODO: add protection against device ids and non-existent hosts.
            HostId one = hostId(string(payload, ONE));
            HostId two = hostId(string(payload, TWO));

            HostToHostIntent intent = HostToHostIntent.builder()
                    .appId(appId)
                    .one(one)
                    .two(two)
                    .build();

            services.intent().submit(intent);
            if (overlayCache.isActive(TrafficOverlay.TRAFFIC_ID)) {
                traffic.monitor(intent);
            }
        }
    }

    private Intent findIntentByPayload(ObjectNode payload) {
        Intent intent;
        Key key;
        int appId = Integer.parseInt(string(payload, APP_ID));
        String appName = string(payload, APP_NAME);
        ApplicationId applicId = new DefaultApplicationId(appId, appName);
        String stringKey = string(payload, KEY);
        try {
            // FIXME: If apps use different string key, but they contains
            // same numeric value (e.g. "020", "0x10", "16", "#10")
            // and one intent using long key (e.g. 16L)
            // this function might return wrong intent.

            long longKey = Long.decode(stringKey);
            key = Key.of(longKey, applicId);
            intent = services.intent().getIntent(key);

            if (intent == null) {
                // Intent might using string key, not long key
                key = Key.of(stringKey, applicId);
                intent = services.intent().getIntent(key);
            }
        } catch (NumberFormatException ex) {
            // string key
            key = Key.of(stringKey, applicId);
            intent = services.intent().getIntent(key);
        }

        log.debug("Attempting to select intent by key={}", key);

        return intent;
    }

    private final class RemoveIntent extends RequestHandler {
        private RemoveIntent() {
            super(REMOVE_INTENT);
        }

        private boolean isIntentToBePurged(ObjectNode payload) {
            return bool(payload, PURGE);
        }

        @Override
        public void process(ObjectNode payload) {
            Intent intent = findIntentByPayload(payload);
            if (intent == null) {
                log.warn("Unable to find intent from payload {}", payload);
            } else {
                log.debug("Withdrawing / Purging intent {}", intent.key());
                if (isIntentToBePurged(payload)) {
                    services.intent().purge(intent);
                } else {
                    services.intent().withdraw(intent);
                }
            }
        }
    }

    private final class ResubmitIntent extends RequestHandler {
        private ResubmitIntent() {
            super(RESUBMIT_INTENT);
        }

        @Override
        public void process(ObjectNode payload) {
            Intent intent = findIntentByPayload(payload);
            if (intent == null) {
                log.warn("Unable to find intent from payload {}", payload);
            } else {
                log.debug("Resubmitting intent {}", intent.key());
                services.intent().submit(intent);
            }
        }
    }

    private final class AddMultiSourceIntent extends RequestHandler {
        private AddMultiSourceIntent() {
            super(ADD_MULTI_SRC_INTENT);
        }

        @Override
        public void process(ObjectNode payload) {
            // TODO: add protection against device ids and non-existent hosts.
            Set<HostId> src = getHostIds((ArrayNode) payload.path(SRC));
            HostId dst = hostId(string(payload, DST));
            Host dstHost = services.host().getHost(dst);

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

            services.intent().submit(intent);
            if (overlayCache.isActive(TrafficOverlay.TRAFFIC_ID)) {
                traffic.monitor(intent);
            }
        }
    }

    private final class RemoveIntents extends RequestHandler {
        private RemoveIntents() {
            super(REMOVE_INTENTS);
        }


        @Override
        public void process(ObjectNode payload) {
            IntentService intentService = get(IntentService.class);
            for (Intent intent : intentService.getIntents()) {
                if (intentService.getIntentState(intent.key()) == IntentState.WITHDRAWN) {
                    intentService.purge(intent);
                }
            }

        }
    }

    // ========= -----------------------------------------------------------------

    private final class ReqAllTraffic extends RequestHandler {
        private ReqAllTraffic() {
            super(REQ_ALL_TRAFFIC);
        }

        @Override
        public void process(ObjectNode payload) {
            String trafficType = string(payload, TRAFFIC_TYPE, FLOW_STATS_BYTES);

            switch (trafficType) {
                case FLOW_STATS_BYTES:
                    traffic.monitor(Mode.ALL_FLOW_TRAFFIC_BYTES);
                    break;
                case PORT_STATS_BIT_SEC:
                    traffic.monitor(Mode.ALL_PORT_TRAFFIC_BIT_PS);
                    break;
                case PORT_STATS_PKT_SEC:
                    traffic.monitor(Mode.ALL_PORT_TRAFFIC_PKT_PS);
                    break;
                default:
                    break;
            }
        }
    }

    private NodeSelection makeNodeSelection(ObjectNode payload) {
        return new NodeSelection(payload, services.device(), services.host(),
                                 services.link());
    }


    private final class ReqDevLinkFlows extends RequestHandler {
        private ReqDevLinkFlows() {
            super(REQ_DEV_LINK_FLOWS);
        }

        @Override
        public void process(ObjectNode payload) {
            traffic.monitor(Mode.DEV_LINK_FLOWS, makeNodeSelection(payload));
        }
    }

    private final class ReqRelatedIntents extends RequestHandler {
        private ReqRelatedIntents() {
            super(REQ_RELATED_INTENTS);
        }

        @Override
        public void process(ObjectNode payload) {
            traffic.monitor(Mode.RELATED_INTENTS, makeNodeSelection(payload));
        }
    }

    private final class ReqNextIntent extends RequestHandler {
        private ReqNextIntent() {
            super(REQ_NEXT_INTENT);
        }

        @Override
        public void process(ObjectNode payload) {
            traffic.selectNextIntent();
        }
    }

    private final class ReqPrevIntent extends RequestHandler {
        private ReqPrevIntent() {
            super(REQ_PREV_INTENT);
        }

        @Override
        public void process(ObjectNode payload) {
            traffic.selectPreviousIntent();
        }
    }

    private final class ReqSelectedIntentTraffic extends RequestHandler {
        private ReqSelectedIntentTraffic() {
            super(REQ_SEL_INTENT_TRAFFIC);
        }

        @Override
        public void process(ObjectNode payload) {
            traffic.monitor(Mode.SELECTED_INTENT);
        }
    }

    private final class SelIntent extends RequestHandler {
        private SelIntent() {
            super(SEL_INTENT);
        }

        @Override
        public void process(ObjectNode payload) {
            Intent intent = findIntentByPayload(payload);
            if (intent == null) {
                log.warn("Unable to find intent from payload {}", payload);
            } else {
                log.debug("starting to monitor intent {}", intent.key());
                traffic.monitor(intent);
            }
        }
    }

    private final class SelProtectedIntent extends RequestHandler {
        private SelProtectedIntent() {
            super(SEL_PROTECTED_INTENT);
        }

        @Override
        public void process(ObjectNode payload) {
            Intent intent = findIntentByPayload(payload);
            if (intent == null) {
                log.warn("Unable to find protected intent from payload {}", payload);
            } else {
                log.debug("starting to monitor protected intent {}", intent.key());
                protectedIntentMonitor.monitor(intent);
            }
        }
    }

    private final class CancelTraffic extends RequestHandler {
        private CancelTraffic() {
            super(CANCEL_TRAFFIC);
        }

        @Override
        public void process(ObjectNode payload) {
            traffic.stopMonitoring();
        }
    }

    private final class CancelProtectedIntentHighlight extends RequestHandler {
        private CancelProtectedIntentHighlight() {
            super(CANCEL_PROTECTED_INTENT_HIGHLIGHT);
        }

        @Override
        public void process(ObjectNode payload) {
            protectedIntentMonitor.stopMonitoring();
        }
    }

    //=======================================================================

    // Converts highlights to JSON format and sends the message to the client
    void sendHighlights(Highlights highlights) {
        sendMessage(highlightsMessage(highlights));
    }

    // Subscribes for summary messages.
    private synchronized void requestSummary() {
        PropertyPanel pp = summmaryMessage();
        overlayCache.currentOverlay().modifySummary(pp);
        sendMessage(envelope(SHOW_SUMMARY, json(pp)));
    }


    private void cancelAllRequests() {
        stopSummaryMonitoring();
        traffic.stopMonitoring();
    }

    // Sends all controller nodes to the client as node-added messages.
    private void sendAllInstances(String messageType) {
        List<ControllerNode> nodes = new ArrayList<>(services.cluster().getNodes());
        nodes.sort(NODE_COMPARATOR);
        for (ControllerNode node : nodes) {
            sendMessage(instanceMessage(new ClusterEvent(INSTANCE_ADDED, node),
                                        messageType));
        }
    }

    // Sends all devices to the client as device-added messages.
    private void sendAllDevices() {
        // Send optical first, others later for layered rendering
        for (Device device : services.device().getDevices()) {
            if ((device.type() == Device.Type.ROADM) ||
                    (device.type() == Device.Type.OTN)) {
                sendMessage(deviceMessage(new DeviceEvent(DEVICE_ADDED, device)));
            }
        }
        for (Device device : services.device().getDevices()) {
            if ((device.type() != Device.Type.ROADM) &&
                    (device.type() != Device.Type.OTN)) {
                sendMessage(deviceMessage(new DeviceEvent(DEVICE_ADDED, device)));
            }
        }
    }

    // Sends all links to the client as link-added messages.
    private void sendAllLinks() {
        // Send optical first, others later for layered rendering
        for (Link link : services.link().getLinks()) {
            if (link.type() == Link.Type.OPTICAL) {
                sendMessage(composeLinkMessage(new LinkEvent(LINK_ADDED, link)));
            }
        }
        for (Link link : services.link().getLinks()) {
            if (link.type() != Link.Type.OPTICAL) {
                sendMessage(composeLinkMessage(new LinkEvent(LINK_ADDED, link)));
            }
        }
    }

    // Temporary mechanism to support topology overlays adding their own
    // properties to the link events.
    private ObjectNode composeLinkMessage(LinkEvent event) {
        // start with base message
        ObjectNode msg = linkMessage(event);
        Map<String, String> additional =
                overlayCache.currentOverlay().additionalLinkData(event);

        if (additional != null) {
            // attach additional key-value pairs as extra data structure
            ObjectNode payload = (ObjectNode) msg.get(PAYLOAD);
            payload.set(EXTRA, createExtra(additional));
        }
        return msg;
    }

    private ObjectNode createExtra(Map<String, String> additional) {
        ObjectNode extra = objectNode();
        for (Map.Entry<String, String> entry : additional.entrySet()) {
            extra.put(entry.getKey(), entry.getValue());
        }
        return extra;
    }

    // Sends all hosts to the client as host-added messages.
    private void sendAllHosts() {
        for (Host host : services.host().getHosts()) {
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
        return services.host().getHost(hostId).location();
    }

    // Produces a list of host ids from the specified JSON array.
    private Set<HostId> getHostIds(ArrayNode ids) {
        Set<HostId> hostIds = new HashSet<>();
        for (JsonNode id : ids) {
            hostIds.add(hostId(id.asText()));
        }
        return hostIds;
    }

    private void sendTopoStartDone() {
        sendMessage(JsonUtils.envelope(TOPO_START_DONE, objectNode()));
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
        services.cluster().addListener(clusterListener);
        services.mastership().addListener(mastershipListener);
        services.device().addListener(deviceListener);
        services.link().addListener(linkListener);
        services.host().addListener(hostListener);
        services.intent().addListener(intentListener);
        services.flow().addListener(flowListener);
    }

    // Removes all internal listeners.
    private synchronized void removeListeners() {
        if (!listenersRemoved) {
            listenersRemoved = true;
            services.cluster().removeListener(clusterListener);
            services.mastership().removeListener(mastershipListener);
            services.device().removeListener(deviceListener);
            services.link().removeListener(linkListener);
            services.host().removeListener(hostListener);
            services.intent().removeListener(intentListener);
            services.flow().removeListener(flowListener);
        }
    }

    // Cluster event listener.
    // TODO: Superceded by UiSharedTopologyModel.ModelEventListener
    @Deprecated
    private class InternalClusterListener implements ClusterEventListener {
        @Override
        public void event(ClusterEvent event) {
            msgSender.execute(() -> sendMessage(instanceMessage(event, null)));
        }
    }

    // Mastership change listener
    // TODO: Superceded by UiSharedTopologyModel.ModelEventListener
    @Deprecated
    private class InternalMastershipListener implements MastershipListener {
        @Override
        public void event(MastershipEvent event) {
            msgSender.execute(() -> {
                sendAllInstances(UPDATE_INSTANCE);
                Device device = services.device().getDevice(event.subject());
                if (device != null) {
                    sendMessage(deviceMessage(new DeviceEvent(DEVICE_UPDATED, device)));
                }
            });
        }
    }

    // Device event listener.
    // TODO: Superceded by UiSharedTopologyModel.ModelEventListener
    @Deprecated
    private class InternalDeviceListener implements DeviceListener {
        @Override
        public void event(DeviceEvent event) {
            if (event.type() != PORT_STATS_UPDATED) {
                msgSender.execute(() -> sendMessage(deviceMessage(event)));
                msgSender.execute(traffic::pokeIntent);
                eventAccummulator.add(event);
            }
        }
    }

    // Link event listener.
    // TODO: Superceded by UiSharedTopologyModel.ModelEventListener
    @Deprecated
    private class InternalLinkListener implements LinkListener {
        @Override
        public void event(LinkEvent event) {
            msgSender.execute(() -> sendMessage(composeLinkMessage(event)));
            msgSender.execute(traffic::pokeIntent);
            eventAccummulator.add(event);
        }
    }

    // Host event listener.
    // TODO: Superceded by UiSharedTopologyModel.ModelEventListener
    @Deprecated
    private class InternalHostListener implements HostListener {
        @Override
        public void event(HostEvent event) {
            msgSender.execute(() -> sendMessage(hostMessage(event)));
            msgSender.execute(traffic::pokeIntent);
            eventAccummulator.add(event);
        }
    }

    // Intent event listener.
    // TODO: Superceded by UiSharedTopologyModel.ModelEventListener
    @Deprecated
    private class InternalIntentListener implements IntentListener {
        @Override
        public void event(IntentEvent event) {
            msgSender.execute(traffic::pokeIntent);
            eventAccummulator.add(event);
        }
    }

    // Intent event listener.
    // TODO: Superceded by UiSharedTopologyModel.ModelEventListener
    @Deprecated
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
                    msgSender.execute(() -> requestSummary());
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
                    msgSender.execute(() -> requestSummary());
                }
            } catch (Exception e) {
                log.warn("Unable to handle summary request due to {}", e.getMessage());
                log.debug("Boom!", e);
            }
        }
    }
}

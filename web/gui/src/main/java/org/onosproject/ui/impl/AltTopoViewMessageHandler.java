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
 *
 */

package org.onosproject.ui.impl;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import org.onlab.osgi.ServiceDirectory;
import org.onosproject.core.CoreService;
import org.onosproject.ui.JsonUtils;
import org.onosproject.ui.RequestHandler;
import org.onosproject.ui.UiConnection;
import org.onosproject.ui.UiMessageHandler;
import org.onosproject.ui.impl.topo.OverlayService;
import org.onosproject.ui.impl.topo.SummaryData;
import org.onosproject.ui.impl.topo.TopoUiEvent;
import org.onosproject.ui.impl.topo.TopoUiListener;
import org.onosproject.ui.impl.topo.TopoUiModelService;
import org.onosproject.ui.impl.topo.overlay.AbstractSummaryGenerator;
import org.onosproject.ui.impl.topo.overlay.SummaryGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.System.currentTimeMillis;
import static org.onosproject.ui.impl.topo.TopoUiEvent.Type.SUMMARY_UPDATE;

/**
 * Facility for handling inbound messages from the topology view, and
 * generating outbound messages for the same.
 */
public class AltTopoViewMessageHandler extends UiMessageHandler
            implements OverlayService {

    private static final String TOPO_START = "topoStart";
    private static final String TOPO_HEARTBEAT = "topoHeartbeat";
    private static final String TOPO_STOP = "topoStop";
    private static final String REQ_SUMMARY = "requestSummary";
    private static final String CANCEL_SUMMARY = "cancelSummary";

    private final Logger log = LoggerFactory.getLogger(getClass());

    protected ServiceDirectory directory;
    protected TopoUiModelService modelService;

    private ModelListener modelListener;
    private String version;
    private SummaryGenerator defaultSummaryGenerator;
    private SummaryGenerator currentSummaryGenerator;


    private boolean topoActive = false;

    @Override
    public void init(UiConnection connection, ServiceDirectory directory) {
        super.init(connection, directory);
        this.directory = checkNotNull(directory, "Directory cannot be null");
        modelService = directory.get(TopoUiModelService.class);
        defaultSummaryGenerator = new DefSummaryGenerator("node", "ONOS Summary");

        bindEventHandlers();
        modelListener = new ModelListener();
        version = getVersion();
        currentSummaryGenerator = defaultSummaryGenerator;
    }

    @Override
    public void destroy() {
        cancelAllMonitoring();
        stopListeningToModel();
        super.destroy();
    }


    @Override
    protected Collection<RequestHandler> createRequestHandlers() {
        return ImmutableSet.of(
                new TopoStart(),
                new TopoHeartbeat(),
                new TopoStop(),
                new ReqSummary(),
                new CancelSummary()
                // TODO: add more handlers here.....
        );
    }

    // =====================================================================

    private void cancelAllMonitoring() {
        // TODO:
    }

    private void startListeningToModel() {
        topoActive = true;
        modelService.addListener(modelListener);
    }

    private void stopListeningToModel() {
        topoActive = false;
        modelService.removeListener(modelListener);
    }

    private String getVersion() {
        String ver = directory.get(CoreService.class).version().toString();
        return ver.replace(".SNAPSHOT", "*").replaceFirst("~.*$", "");
    }

    // =====================================================================
    // Overlay Service
    // TODO: figure out how we are going to switch overlays in and out...

    private final Map<String, SummaryGenerator> summGenCache = Maps.newHashMap();

    @Override
    public void addSummaryGenerator(String overlayId, SummaryGenerator generator) {
        log.info("Adding custom Summary Generator for overlay [{}]", overlayId);
        summGenCache.put(overlayId, generator);
    }

    @Override
    public void removeSummaryGenerator(String overlayId) {
        summGenCache.remove(overlayId);
        log.info("Custom Summary Generator for overlay [{}] removed", overlayId);
    }



    // =====================================================================
    // Request Handlers for (topo view) events from the UI...

    private final class TopoStart extends RequestHandler {
        private TopoStart() {
            super(TOPO_START);
        }

        @Override
        public void process(long sid, ObjectNode payload) {
            startListeningToModel();
            sendMessages(modelService.getInitialState());
        }
    }

    private final class TopoHeartbeat extends RequestHandler {
        private TopoHeartbeat() {
            super(TOPO_HEARTBEAT);
        }
        @Override
        public void process(long sid, ObjectNode payload) {
            modelListener.nudge();
        }
    }

    private final class TopoStop extends RequestHandler {
        private TopoStop() {
            super(TOPO_STOP);
        }

        @Override
        public void process(long sid, ObjectNode payload) {
            stopListeningToModel();
        }
    }

    private final class ReqSummary extends RequestHandler {
        private ReqSummary() {
            super(REQ_SUMMARY);
        }

        @Override
        public void process(long sid, ObjectNode payload) {
            modelService.startSummaryMonitoring();
            // NOTE: showSummary messages forwarded through the model listener
        }
    }

    private final class CancelSummary extends RequestHandler {
        private CancelSummary() {
            super(CANCEL_SUMMARY);
        }

        @Override
        public void process(long sid, ObjectNode payload) {
            modelService.stopSummaryMonitoring();
        }
    }

    // =====================================================================

    private final class DefSummaryGenerator extends AbstractSummaryGenerator {
        public DefSummaryGenerator(String iconId, String title) {
            super(iconId, title);
        }

        @Override
        public ObjectNode generateSummary() {
            SummaryData data = modelService.getSummaryData();
            iconId("node");
            title("ONOS Summary");
            clearProps();
            prop("Devices", format(data.deviceCount()));
            prop("Links", format(data.linkCount()));
            prop("Hosts", format(data.hostCount()));
            prop("Topology SCCs", format(data.clusterCount()));
            separator();
            prop("Intents", format(data.intentCount()));
            prop("Flows", format(data.flowRuleCount()));
            prop("Version", version);
            return buildObjectNode();
        }
    }

    // =====================================================================
    // Private Helper Methods...

    private void sendMessages(Collection<ObjectNode> messages) {
        if (topoActive) {
            UiConnection connection = connection();
            if (connection != null) {
                messages.forEach(connection::sendMessage);
            }
        }
    }

    private void sendMessages(ObjectNode message) {
        if (topoActive) {
            UiConnection connection = connection();
            if (connection != null) {
                connection.sendMessage(message);
            }
        }
    }

    // =====================================================================
    // Our listener for model events so we can push changes out to the UI...

    private class ModelListener implements TopoUiListener {
        private static final long AWAKE_THRESHOLD_MS = 6000;

        private long lastNudged = currentTimeMillis();

        @Override
        public void event(TopoUiEvent event) {
            log.debug("Handle Event: {}", event);
            ModelEventHandler handler = eventHandlerBinding.get(event.type());

            // any handlers not bound explicitly are assumed to be pass-thru...
            if (handler == null) {
                handler = passThruHandler;
            }
            handler.handleEvent(event);
        }

        @Override
        public boolean isAwake() {
            return currentTimeMillis() - lastNudged < AWAKE_THRESHOLD_MS;
        }

        public void nudge() {
            lastNudged = currentTimeMillis();
        }
    }


    // =====================================================================
    // Model Event Handler definitions and bindings...

    private interface ModelEventHandler {
        void handleEvent(TopoUiEvent event);
    }

    private ModelEventHandler passThruHandler = event -> {
        // simply forward the event message as is
        ObjectNode message = event.subject();
        if (message != null) {
            sendMessages(event.subject());
        }
    };

    private ModelEventHandler summaryHandler = event -> {
        // use the currently selected summary generator to create the body..
        ObjectNode payload = currentSummaryGenerator.generateSummary();
        sendMessages(JsonUtils.envelope("showSummary", payload));
    };


    // TopoUiEvent type binding of handlers
    private final Map<TopoUiEvent.Type, ModelEventHandler>
            eventHandlerBinding = new HashMap<>();

    private void bindEventHandlers() {
        eventHandlerBinding.put(SUMMARY_UPDATE, summaryHandler);
        // NOTE: no need to bind pass-thru handlers
    }
}

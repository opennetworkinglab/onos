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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.eclipse.jetty.websocket.WebSocket;
import org.onlab.osgi.ServiceDirectory;
import org.onlab.osgi.ServiceNotFoundException;
import org.onosproject.cluster.ClusterService;
import org.onosproject.cluster.ControllerNode;
import org.onosproject.ui.UiConnection;
import org.onosproject.ui.UiExtensionService;
import org.onosproject.ui.UiMessageHandler;
import org.onosproject.ui.UiMessageHandlerFactory;
import org.onosproject.ui.UiTopoLayoutService;
import org.onosproject.ui.UiTopoOverlayFactory;
import org.onosproject.ui.impl.topo.UiTopoSession;
import org.onosproject.ui.impl.topo.model.UiSharedTopologyModel;
import org.onosproject.ui.model.topo.UiTopoLayout;
import org.onosproject.ui.topo.TopoConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Web socket capable of interacting with the Web UI.
 */
public class UiWebSocket
        implements UiConnection, WebSocket.OnTextMessage, WebSocket.OnControl {

    private static final Logger log = LoggerFactory.getLogger(UiWebSocket.class);

    private static final String EVENT = "event";
    private static final String SID = "sid";
    private static final String PAYLOAD = "payload";
    private static final String UNKNOWN = "unknown";

    private static final String ID = "id";
    private static final String IP = "ip";
    private static final String CLUSTER_NODES = "clusterNodes";
    private static final String USER = "user";
    private static final String BOOTSTRAP = "bootstrap";

    public static final String TOPO = "topo";

    private static final long MAX_AGE_MS = 30_000;

    private static final byte PING = 0x9;
    private static final byte PONG = 0xA;
    private static final byte[] PING_DATA = new byte[]{(byte) 0xde, (byte) 0xad};

    private final ObjectMapper mapper = new ObjectMapper();
    private final ServiceDirectory directory;
    private final UiTopoSession topoSession;

    private Connection connection;
    private FrameConnection control;
    private String userName;
    private String currentView;

    private long lastActive = System.currentTimeMillis();

    private Map<String, UiMessageHandler> handlers;
    private TopoOverlayCache overlayCache;

    /**
     * Creates a new web-socket for serving data to the Web UI.
     *
     * @param directory service directory
     * @param userName  user name of the logged-in user
     */
    public UiWebSocket(ServiceDirectory directory, String userName) {
        this.directory = directory;
        this.userName = userName;
        this.topoSession =
                new UiTopoSession(this, directory.get(UiSharedTopologyModel.class),
                        directory.get(UiTopoLayoutService.class));
    }

    @Override
    public String userName() {
        return userName;
    }

    @Override
    public UiTopoLayout currentLayout() {
        return topoSession.currentLayout();
    }

    @Override
    public void setCurrentLayout(UiTopoLayout topoLayout) {
        topoSession.setCurrentLayout(topoLayout);
    }

    @Override
    public String currentView() {
        return currentView;
    }

    @Override
    public void setCurrentView(String viewId) {
        currentView = viewId;
        topoSession.enableEvent(viewId.equals(TOPO));
    }

    /**
     * Provides a reference to the topology session.
     *
     * @return topo session reference
     */
    public UiTopoSession topoSession() {
        return topoSession;
    }

    /**
     * Issues a close on the connection.
     */
    synchronized void close() {
        destroyHandlersAndOverlays();
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
        long quietFor = System.currentTimeMillis() - lastActive;
        boolean idle = quietFor > MAX_AGE_MS;
        if (idle || (connection != null && !connection.isOpen())) {
            log.debug("IDLE (or closed) websocket [{} ms]", quietFor);
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
    public synchronized void onOpen(Connection connection) {
        this.connection = connection;
        this.control = (FrameConnection) connection;
        try {
            topoSession.init();
            createHandlersAndOverlays();
            sendBootstrapData();
            log.info("GUI client connected -- user <{}>", userName);

        } catch (ServiceNotFoundException e) {
            log.warn("Unable to open GUI connection; services have been shut-down", e);
            this.connection.close();
            this.connection = null;
            this.control = null;
        }
    }

    @Override
    public synchronized void onClose(int closeCode, String message) {
        topoSession.destroy();
        destroyHandlersAndOverlays();
        log.info("GUI client disconnected [close-code={}, message={}]",
                closeCode, message);
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
            ObjectNode message = (ObjectNode) mapper.reader().readTree(data);
            String type = message.path(EVENT).asText(UNKNOWN);
            UiMessageHandler handler = handlers.get(type);
            if (handler != null) {
                log.debug("RX message: {}", message);
                handler.process(message);
            } else {
                log.warn("No GUI message handler for type {}", type);
            }
        } catch (Exception e) {
            log.warn("Unable to parse GUI message {} due to {}", data, e);
            log.debug("Boom!!!", e);
        }
    }

    @Override
    public synchronized void sendMessage(ObjectNode message) {
        try {
            if (connection.isOpen()) {
                connection.sendMessage(message.toString());
                log.debug("TX message: {}", message);
            }
        } catch (IOException e) {
            log.warn("Unable to send message {} to GUI due to {}", message, e);
            log.debug("Boom!!!", e);
        }
    }

    @Override
    public synchronized void sendMessage(String type, long sid, ObjectNode payload) {
        ObjectNode message = mapper.createObjectNode();
        message.put(EVENT, type);
        if (sid > 0) {
            message.put(SID, sid);
        }
        message.set(PAYLOAD, payload != null ? payload : mapper.createObjectNode());
        sendMessage(message);
    }

    // Creates new message handlers.
    private synchronized void createHandlersAndOverlays() {
        log.debug("Creating handlers and overlays...");
        handlers = new HashMap<>();
        overlayCache = new TopoOverlayCache();

        UiExtensionService service = directory.get(UiExtensionService.class);
        service.getExtensions().forEach(ext -> {
            UiMessageHandlerFactory factory = ext.messageHandlerFactory();
            if (factory != null) {
                factory.newHandlers().forEach(handler -> {
                    try {
                        handler.init(this, directory);
                        handler.messageTypes().forEach(type -> handlers.put(type, handler));

                        // need to inject the overlay cache into topology message handler
                        if (handler instanceof TopologyViewMessageHandler) {
                            ((TopologyViewMessageHandler) handler).setOverlayCache(overlayCache);
                        }
                    } catch (Exception e) {
                        log.warn("Unable to setup handler {} due to", handler, e);
                    }
                });
            }

            UiTopoOverlayFactory overlayFactory = ext.topoOverlayFactory();
            if (overlayFactory != null) {
                overlayFactory.newOverlays().forEach(overlayCache::add);
            }
        });
        log.debug("#handlers = {}, #overlays = {}", handlers.size(),
                overlayCache.size());
    }

    // Destroys message handlers.
    private synchronized void destroyHandlersAndOverlays() {
        log.debug("Destroying handlers and overlays...");
        handlers.forEach((type, handler) -> handler.destroy());
        handlers.clear();

        if (overlayCache != null) {
            overlayCache.destroy();
            overlayCache = null;
        }
    }

    // Sends initial information (username and cluster member information)
    // to allow GUI to display logged-in user, and to be able to
    // fail-over to an alternate cluster member if necessary.
    private void sendBootstrapData() {
        ClusterService service = directory.get(ClusterService.class);
        ArrayNode instances = mapper.createArrayNode();

        for (ControllerNode node : service.getNodes()) {
            ObjectNode instance = mapper.createObjectNode()
                    .put(ID, node.id().toString())
                    .put(IP, node.ip().toString())
                    .put(TopoConstants.Glyphs.UI_ATTACHED,
                            node.equals(service.getLocalNode()));
            instances.add(instance);
        }

        ObjectNode payload = mapper.createObjectNode();
        payload.set(CLUSTER_NODES, instances);
        payload.put(USER, userName);
        sendMessage(BOOTSTRAP, 0, payload);
    }

}


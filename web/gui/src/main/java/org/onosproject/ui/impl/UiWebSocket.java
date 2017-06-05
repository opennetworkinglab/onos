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
import org.onosproject.ui.GlyphConstants;
import org.onosproject.ui.UiConnection;
import org.onosproject.ui.UiExtension;
import org.onosproject.ui.UiExtensionService;
import org.onosproject.ui.UiMessageHandler;
import org.onosproject.ui.UiMessageHandlerFactory;
import org.onosproject.ui.UiSessionToken;
import org.onosproject.ui.UiTokenService;
import org.onosproject.ui.UiTopo2OverlayFactory;
import org.onosproject.ui.UiTopoLayoutService;
import org.onosproject.ui.UiTopoOverlayFactory;
import org.onosproject.ui.impl.topo.Topo2Jsonifier;
import org.onosproject.ui.impl.topo.Topo2OverlayCache;
import org.onosproject.ui.impl.topo.Topo2TrafficMessageHandler;
import org.onosproject.ui.impl.topo.Topo2ViewMessageHandler;
import org.onosproject.ui.impl.topo.UiTopoSession;
import org.onosproject.ui.impl.topo.model.UiSharedTopologyModel;
import org.onosproject.ui.model.topo.UiTopoLayout;
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
    private static final String PAYLOAD = "payload";
    private static final String UNKNOWN = "unknown";
    private static final String AUTHENTICATION = "authentication";
    private static final String TOKEN = "token";
    private static final String ERROR = "error";

    private static final String ID = "id";
    private static final String IP = "ip";
    private static final String CLUSTER_NODES = "clusterNodes";
    private static final String USER = "user";
    private static final String BOOTSTRAP = "bootstrap";

    private static final String TOPO = "topo";

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
    private Topo2OverlayCache overlay2Cache;

    private UiSessionToken sessionToken;


    /**
     * Creates a new web-socket for serving data to the Web UI.
     *
     * @param directory service directory
     * @param userName  user name of the logged-in user
     */
    public UiWebSocket(ServiceDirectory directory, String userName) {
        this.directory = directory;
        this.userName = userName;

        Topo2Jsonifier t2json = new Topo2Jsonifier(directory, userName);
        UiSharedTopologyModel sharedModel = directory.get(UiSharedTopologyModel.class);
        UiTopoLayoutService layoutService = directory.get(UiTopoLayoutService.class);

        sharedModel.injectJsonifier(t2json);
        topoSession = new UiTopoSession(this, t2json, sharedModel, layoutService);
        sessionToken = null;
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
        tokenService().revokeToken(sessionToken);
        log.info("Session token revoked");

        sessionToken = null;

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

            if (sessionToken == null) {
                authenticate(type, message);

            } else {
                UiMessageHandler handler = handlers.get(type);
                if (handler != null) {
                    log.debug("RX message: {}", message);
                    handler.process(message);
                } else {
                    log.warn("No GUI message handler for type {}", type);
                }
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
    public synchronized void sendMessage(String type, ObjectNode payload) {
        ObjectNode message = mapper.createObjectNode();
        message.put(EVENT, type);
        message.set(PAYLOAD, payload != null ? payload : mapper.createObjectNode());
        sendMessage(message);
    }

    // Creates new message handlers.
    private synchronized void createHandlersAndOverlays() {
        log.debug("Creating handlers and overlays...");
        handlers = new HashMap<>();
        overlayCache = new TopoOverlayCache();
        overlay2Cache = new Topo2OverlayCache();

        Map<Class<?>, UiMessageHandler> handlerInstances = new HashMap<>();

        UiExtensionService service = directory.get(UiExtensionService.class);
        service.getExtensions().forEach(ext -> {
            UiMessageHandlerFactory factory = ext.messageHandlerFactory();
            if (factory != null) {
                factory.newHandlers().forEach(handler -> {
                    try {
                        handler.init(this, directory);
                        handler.messageTypes().forEach(type -> handlers.put(type, handler));
                        handlerInstances.put(handler.getClass(), handler);

                    } catch (Exception e) {
                        log.warn("Unable to setup handler {} due to", handler, e);
                    }
                });
            }

            registerOverlays(ext);
        });

        handlerCrossConnects(handlerInstances);

        log.debug("#handlers = {}, #overlays = {}", handlers.size(), overlayCache.size());
    }

    private void authenticate(String type, ObjectNode message) {
        if (!AUTHENTICATION.equals(type)) {
            log.warn("Non-Authenticated Web Socket: {}", message);
            return;
        }

        String tokstr = message.path(PAYLOAD).path(TOKEN).asText(UNKNOWN);
        UiSessionToken token = new UiSessionToken(tokstr);

        if (tokenService().isTokenValid(token)) {
            sessionToken = token;
            log.info("Session token authenticated");
            log.debug("WebSocket authenticated: {}", message);
        } else {
            log.warn("Invalid Authentication Token: {}", message);
            sendMessage(ERROR, notAuthorized(token));
        }
    }

    private ObjectNode notAuthorized(UiSessionToken token) {
        return mapper.createObjectNode()
                .put("message", "invalid authentication token")
                .put("badToken", token.toString());
    }

    private void registerOverlays(UiExtension ext) {
        UiTopoOverlayFactory overlayFactory = ext.topoOverlayFactory();
        if (overlayFactory != null) {
            overlayFactory.newOverlays().forEach(overlayCache::add);
        }

        UiTopo2OverlayFactory overlay2Factory = ext.topo2OverlayFactory();
        if (overlay2Factory != null) {
            overlay2Factory.newOverlays().forEach(overlay2Cache::add);
        }
    }

    private void handlerCrossConnects(Map<Class<?>, UiMessageHandler> handlers) {
        TopologyViewMessageHandler topomh = (TopologyViewMessageHandler)
                handlers.get(TopologyViewMessageHandler.class);
        if (topomh != null) {
            topomh.setOverlayCache(overlayCache);
        }

        Topo2ViewMessageHandler topo2mh = (Topo2ViewMessageHandler)
                handlers.get(Topo2ViewMessageHandler.class);
        if (topo2mh != null) {
            topo2mh.setOverlayCache(overlay2Cache);

            // We also need a link to Topo2Traffic
            Topo2TrafficMessageHandler topo2traffic = (Topo2TrafficMessageHandler)
                    handlers.get(Topo2TrafficMessageHandler.class);
            if (topo2traffic != null) {
                topo2mh.setTrafficHandler(topo2traffic);
            } else {
                log.error("No topo2 traffic handler found");
            }
        }
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
        if (overlay2Cache != null) {
            overlay2Cache.destroy();
            overlay2Cache = null;
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
                    .put(GlyphConstants.UI_ATTACHED,
                         node.equals(service.getLocalNode()));
            instances.add(instance);
        }

        ObjectNode payload = mapper.createObjectNode();
        payload.set(CLUSTER_NODES, instances);
        payload.put(USER, userName);
        sendMessage(BOOTSTRAP, payload);
    }

    private UiTokenService tokenService() {
        UiTokenService service = directory.get(UiTokenService.class);
        if (service == null) {
            log.error("Unable to reference UiTokenService");
        }
        return service;
    }
}

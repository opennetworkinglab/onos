/*
 * Copyright 2015-present Open Networking Foundation
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
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.WebSocketAdapter;
import org.onlab.osgi.ServiceDirectory;
import org.onlab.osgi.ServiceNotFoundException;
import org.onlab.packet.IpAddress;
import org.onosproject.cluster.ClusterService;
import org.onosproject.cluster.ControllerNode;
import org.onosproject.ui.GlyphConstants;
import org.onosproject.ui.UiConnection;
import org.onosproject.ui.UiExtension;
import org.onosproject.ui.UiExtensionService;
import org.onosproject.ui.UiGlyph;
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
import org.onosproject.ui.impl.topo.Traffic2Overlay;
import org.onosproject.ui.impl.topo.UiTopoSession;
import org.onosproject.ui.impl.topo.model.UiSharedTopologyModel;
import org.onosproject.ui.lion.LionBundle;
import org.onosproject.ui.model.topo.UiTopoLayout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import static org.onosproject.ui.impl.UiWebSocketServlet.PING_DELAY_MS;

/**
 * Web socket capable of interacting with the Web UI.
 */

public class UiWebSocket extends WebSocketAdapter implements UiConnection {

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

    private static final String UBERLION = "uberlion";
    private static final String LION = "lion";
    private static final String LOCALE = "locale";

    private static final String TOPO = "topo";

    private static final String GLYPHS = "glyphs";

    private static final long MAX_AGE_MS = 30_000;

    private static final byte[] PING_DATA = new byte[]{(byte) 0xde, (byte) 0xad};
    private static final ByteBuffer PING = ByteBuffer.wrap(PING_DATA);

    private final ObjectMapper mapper = new ObjectMapper();
    private final ServiceDirectory directory;
    private final UiTopoSession topoSession;

    private String userName;
    private String currentView;

    private long lastActive = System.currentTimeMillis();

    private Map<String, UiMessageHandler> handlers;
    private TopoOverlayCache overlayCache;
    private Topo2OverlayCache overlay2Cache;

    private Map<String, LionBundle> lionBundleMap;

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

    private ObjectNode objectNode() {
        return mapper.createObjectNode();
    }

    private ArrayNode arrayNode() {
        return mapper.createArrayNode();
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
    void close() {
        destroyHandlersAndOverlays();
        if (isConnected()) {
            getSession().close();
        }
    }

    /**
     * Indicates if this connection is idle.
     *
     * @return true if idle or closed
     */
    boolean isIdle() {
        long quietFor = System.currentTimeMillis() - lastActive;
        boolean idle = quietFor > MAX_AGE_MS;
        if (idle || isNotConnected()) {
            log.debug("IDLE (or closed) websocket [{} ms]", quietFor);
            return true;

        } else if (isConnected() && quietFor > PING_DELAY_MS) {
            try {
                getRemote().sendPing(PING);
                lastActive = System.currentTimeMillis();
            } catch (IOException e) {
                log.warn("Unable to send ping message due to: ", e);
            }
        }
        return false;
    }

    @Override
    public void onWebSocketConnect(Session session) {
        super.onWebSocketConnect(session);
        try {
            topoSession.init();
            createHandlersAndOverlays();
            sendBootstrapData();
            sendUberLionBundle();
            lastActive = System.currentTimeMillis();
            log.info("GUI client connected -- user <{}>", userName);

        } catch (ServiceNotFoundException e) {
            log.warn("Unable to open GUI connection; services have been shut-down", e);
            getSession().close();
        }
    }

    @Override
    public void onWebSocketClose(int closeCode, String reason) {
        try {
            try {
                tokenService().revokeToken(sessionToken);
                log.info("Session token revoked");
            } catch (ServiceNotFoundException e) {
                log.error("Unable to reference UiTokenService");
            }
            sessionToken = null;

            topoSession.destroy();
            destroyHandlersAndOverlays();
        } catch (Exception e) {
            log.warn("Unexpected error", e);
        }
        super.onWebSocketClose(closeCode, reason);
        log.info("GUI client disconnected [close-code={}, message={}]",
                 closeCode, reason);
    }

    @Override
    public void onWebSocketText(String data) {
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

        } catch (Error | Exception e) {
            log.warn("Unable to parse GUI message {} due to", data, e);
            log.debug("Boom!!!", e);
        }
    }

    @Override
    public void onWebSocketBinary(byte[] payload, int offset, int length) {
        lastActive = System.currentTimeMillis();
        log.warn("Binary messages are currently not supported");
    }

    @Override
    public synchronized void sendMessage(ObjectNode message) {
        try {
            if (isConnected()) {
                getRemote().sendString(message.toString());
                log.debug("TX message: {}", message);
            }
        } catch (IOException e) {
            log.warn("Unable to send message {} to GUI due to {}", message, e);
            log.debug("Boom!!!", e);
        }
    }

    @Override
    public synchronized void sendMessage(String type, ObjectNode payload) {
        ObjectNode message = objectNode();
        message.put(EVENT, type);
        message.set(PAYLOAD, payload != null ? payload : objectNode());
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
        lionBundleMap = generateLionMap(service);

        service.getExtensions().forEach(ext -> {
            UiMessageHandlerFactory factory = ext.messageHandlerFactory();
            if (factory != null) {
                factory.newHandlers().forEach(handler -> {
                    try {
                        handler.init(this, directory);
                        injectLionBundles(handler, lionBundleMap);
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

        overlay2Cache.switchOverlay(null, Traffic2Overlay.OVERLAY_ID);

        log.debug("#handlers = {}, #overlays = Topo: {}, Topo2: {}",
                  handlers.size(), overlayCache.size(), overlay2Cache.size());
    }

    private Map<String, LionBundle> generateLionMap(UiExtensionService service) {
        Map<String, LionBundle> bundles = new HashMap<>();
        service.getExtensions().forEach(ext -> {
            ext.lionBundles().forEach(lb -> bundles.put(lb.id(), lb));
        });
        return bundles;
    }

    private void injectLionBundles(UiMessageHandler handler,
                                   Map<String, LionBundle> lionBundleMap) {
        handler.requiredLionBundles().forEach(lbid -> {
            LionBundle lb = lionBundleMap.get(lbid);
            if (lb != null) {
                handler.cacheLionBundle(lb);
            } else {
                log.warn("handler {}: Lion bundle {} non existent!",
                         handler.getClass().getName(), lbid);
            }
        });
    }

    private void authenticate(String type, ObjectNode message) {
        if (!AUTHENTICATION.equals(type)) {
            log.warn("WebSocket not authenticated: {}", message);
            sendMessage(ERROR, notAuthorized(null));
            close();
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
        return objectNode()
                .put("message", "invalid authentication token")
                .put("badToken", token != null ? token.toString() : "null");
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
                topo2traffic.setOverlayCache(overlay2Cache);
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
        ArrayNode instances = arrayNode();

        for (ControllerNode node : service.getNodes()) {
            IpAddress nodeIp = node.ip();
            ObjectNode instance = objectNode()
                    .put(ID, node.id().toString())
                    .put(IP, nodeIp != null ? nodeIp.toString() : node.host())
                    .put(GlyphConstants.UI_ATTACHED,
                         node.equals(service.getLocalNode()));
            instances.add(instance);
        }

        ArrayNode glyphInstances = arrayNode();
        UiExtensionService uiExtensionService = directory.get(UiExtensionService.class);
        for (UiGlyph glyph : uiExtensionService.getGlyphs()) {
            ObjectNode glyphInstance = objectNode()
                    .put(GlyphConstants.ID, glyph.id())
                    .put(GlyphConstants.VIEWBOX, glyph.viewbox())
                    .put(GlyphConstants.PATH, glyph.path());
            glyphInstances.add(glyphInstance);
        }

        ObjectNode payload = objectNode();
        payload.set(CLUSTER_NODES, instances);
        payload.set(GLYPHS, glyphInstances);
        payload.put(USER, userName);

        sendMessage(BOOTSTRAP, payload);
    }

    private UiTokenService tokenService() {
        return directory.get(UiTokenService.class);
    }

    // sends the collated localization bundle data up to the client.
    private void sendUberLionBundle() {
        UiExtensionService service = directory.get(UiExtensionService.class);
        ObjectNode lion = objectNode();

        service.getExtensions().forEach(ext -> {
            ext.lionBundles().forEach(lb -> {
                ObjectNode lionMap = objectNode();
                lb.getItems().forEach(item -> lionMap.put(item.key(), item.value()));
                lion.set(lb.id(), lionMap);
            });
        });

        ObjectNode payload = objectNode();
        payload.set(LION, lion);
        payload.put(LOCALE, Locale.getDefault().toString());
        sendMessage(UBERLION, payload);
    }
}

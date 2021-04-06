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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.BooleanNode;
import com.fasterxml.jackson.databind.node.DoubleNode;
import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.LongNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.ShortNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.onlab.util.Tools;
import org.onosproject.cfg.ComponentConfigService;
import org.onosproject.mastership.MastershipService;
import org.onosproject.store.serializers.KryoNamespaces;
import org.onosproject.store.service.ConsistentMap;
import org.onosproject.store.service.MapEvent;
import org.onosproject.store.service.MapEventListener;
import org.onosproject.store.service.Serializer;
import org.onosproject.store.service.StorageService;
import org.onosproject.ui.UiExtension;
import org.onosproject.ui.UiExtensionService;
import org.onosproject.ui.UiGlyph;
import org.onosproject.ui.UiGlyphFactory;
import org.onosproject.ui.UiMessageHandlerFactory;
import org.onosproject.ui.UiPreferencesService;
import org.onosproject.ui.UiSessionToken;
import org.onosproject.ui.UiTokenService;
import org.onosproject.ui.UiTopo2OverlayFactory;
import org.onosproject.ui.UiTopoHighlighterFactory;
import org.onosproject.ui.UiTopoMap;
import org.onosproject.ui.UiTopoMapFactory;
import org.onosproject.ui.UiTopoOverlayFactory;
import org.onosproject.ui.UiView;
import org.onosproject.ui.UiViewHidden;
import org.onosproject.ui.impl.topo.Topo2TrafficMessageHandler;
import org.onosproject.ui.impl.topo.Topo2ViewMessageHandler;
import org.onosproject.ui.impl.topo.Traffic2Overlay;
import org.onosproject.ui.impl.topo.model.UiSharedTopologyModel;
import org.onosproject.ui.lion.LionBundle;
import org.onosproject.ui.lion.LionUtils;
import org.onosproject.ui.topo.AbstractTopoMonitor;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.Dictionary;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.google.common.collect.ImmutableList.of;
import static java.util.stream.Collectors.toSet;
import static org.onosproject.security.AppGuard.checkPermission;
import static org.onosproject.security.AppPermission.Type.GLYPH_READ;
import static org.onosproject.security.AppPermission.Type.GLYPH_WRITE;
import static org.onosproject.security.AppPermission.Type.UI_READ;
import static org.onosproject.security.AppPermission.Type.UI_WRITE;
import static org.onosproject.ui.UiView.Category.NETWORK;
import static org.onosproject.ui.UiView.Category.PLATFORM;
import static org.onosproject.ui.impl.OsgiPropertyConstants.TRAFFIC_REFRESH_MS;
import static org.onosproject.ui.impl.OsgiPropertyConstants.TRAFFIC_REFRESH_MS_DEFAULT;
import static org.onosproject.ui.impl.lion.BundleStitcher.generateBundles;

/**
 * Manages the user interface extensions.
 */
@Component(immediate = true,
        service = {
                UiExtensionService.class,
                UiPreferencesService.class,
                SpriteService.class,
                UiTokenService.class
        },
        property = {
                TRAFFIC_REFRESH_MS + ":Integer=" + TRAFFIC_REFRESH_MS_DEFAULT,
        })
public class UiExtensionManager
        implements UiExtensionService, UiPreferencesService, SpriteService,
        UiTokenService {

    private static final ClassLoader CL = UiExtensionManager.class.getClassLoader();

    private static final String ONOS_USER_PREFERENCES = "onos-ui-user-preferences";
    private static final String ONOS_SESSION_TOKENS = "onos-ui-session-tokens";
    private static final String CORE = "core";
    private static final String GUI_ADDED = "guiAdded";
    private static final String GUI_REMOVED = "guiRemoved";
    private static final String GLYPH_ADDED = "glyphAdded";
    private static final String GLYPH_REMOVED = "glyphRemoved";
    private static final String UPDATE_PREFS = "updatePrefs";
    private static final String SLASH = "/";

    private static final int IDX_USER = 0;
    private static final int IDX_KEY = 1;

    private static final String LION_BASE = "/org/onosproject/ui/lion";

    private static final String[] LION_TAGS = {
            // framework component localization
            "core.fw.Mast",
            "core.fw.Nav",
            "core.fw.QuickHelp",

            // view component localization
            "core.view.App",
            "core.view.Cluster",
            "core.view.Topo",
            "core.view.Flow",

            // TODO: More to come...
    };


    private final Logger log = LoggerFactory.getLogger(getClass());

    // First thing to do is to set the locale (before creating core extension).
    private final Locale runtimeLocale = LionUtils.setupRuntimeLocale();

    // List of all extensions
    private final List<UiExtension> extensions = Lists.newArrayList();

    private final List<UiGlyph> glyphs = Lists.newArrayList();
    private final List<UiTopoHighlighterFactory> highlighterFactories = Lists.newArrayList();

    // Map of views to extensions
    private final Map<String, UiExtension> views = Maps.newHashMap();

    // Core views & core extension
    private final UiExtension core = createCoreExtension();

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected MastershipService mastershipService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected StorageService storageService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected ComponentConfigService cfgService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    private UiSharedTopologyModel sharedModel;

    // User preferences
    private ConsistentMap<String, ObjectNode> prefsConsistentMap;
    private Map<String, ObjectNode> prefs;
    private final MapEventListener<String, ObjectNode> prefsListener =
            new InternalPrefsListener();

    // Session tokens
    private ConsistentMap<UiSessionToken, String> tokensConsistentMap;
    private Map<UiSessionToken, String> tokens;
    private final SessionTokenGenerator tokenGen =
            new SessionTokenGenerator();

    private final ObjectMapper mapper = new ObjectMapper();

    private final ExecutorService eventHandlingExecutor =
            Executors.newSingleThreadExecutor(
                    Tools.groupedThreads("onos/ui-ext-manager", "event-handler", log));

    private LionBundle navLion;

    protected int trafficRefreshMs = TRAFFIC_REFRESH_MS_DEFAULT;

    private String lionNavText(String id) {
        return navLion.getValue("nav_item_" + id);
    }

    private UiView mkView(UiView.Category cat, String id, String iconId) {
        return new UiView(cat, id, lionNavText(id), iconId);
    }

    private UiExtension createCoreExtension() {
        List<LionBundle> lionBundles = generateBundles(LION_BASE, LION_TAGS);

        navLion = lionBundles.stream()
                .filter(f -> f.id().equals("core.fw.Nav")).findFirst().get();

        List<UiView> coreViews = of(
                mkView(PLATFORM, "app", "nav_apps"),
                mkView(PLATFORM, "settings", "nav_settings"),
                mkView(PLATFORM, "cluster", "nav_cluster"),
                mkView(PLATFORM, "processor", "nav_processors"),
                mkView(PLATFORM, "partition", "nav_partitions"),

                mkView(NETWORK, "topo", "nav_topo"),
//                mkView(NETWORK, "topo2", "nav_topo2"),
                mkView(NETWORK, "device", "nav_devs"),

                new UiViewHidden("flow"),
                new UiViewHidden("port"),
                new UiViewHidden("group"),
                new UiViewHidden("meter"),
                new UiViewHidden("pipeconf"),

                mkView(NETWORK, "link", "nav_links"),
                mkView(NETWORK, "host", "nav_hosts"),
                mkView(NETWORK, "intent", "nav_intents")
        );

        UiMessageHandlerFactory messageHandlerFactory =
                () -> ImmutableList.of(
                        new UserPreferencesMessageHandler(),
                        new TopologyViewMessageHandler(),
                        new Topo2ViewMessageHandler(),
                        new Topo2TrafficMessageHandler(),
                        new MapSelectorMessageHandler(),
                        new DeviceViewMessageHandler(),
                        new LinkViewMessageHandler(),
                        new HostViewMessageHandler(),
                        new FlowViewMessageHandler(),
                        new PortViewMessageHandler(),
                        new GroupViewMessageHandler(),
                        new MeterViewMessageHandler(),
                        new IntentViewMessageHandler(),
                        new ApplicationViewMessageHandler(),
                        new SettingsViewMessageHandler(),
                        new ClusterViewMessageHandler(),
                        new ProcessorViewMessageHandler(),
                        new PartitionViewMessageHandler(),
                        new PipeconfViewMessageHandler()
                );

        UiTopoOverlayFactory topoOverlayFactory =
                () -> ImmutableList.of(
                        new TrafficOverlay(),
                        new ProtectedIntentOverlay()
                );

        UiTopo2OverlayFactory topo2OverlayFactory =
                () -> ImmutableList.of(
                        new Traffic2Overlay()
                );

        UiTopoMapFactory topoMapFactory =
                () -> ImmutableList.of(
                        new UiTopoMap("australia", "Australia", "*australia", 1.0),
                        new UiTopoMap("americas", "North, Central and South America", "*americas", 0.7),
                        new UiTopoMap("n_america", "North America", "*n_america", 0.9),
                        new UiTopoMap("s_america", "South America", "*s_america", 0.9),
                        new UiTopoMap("usa", "United States", "*continental_us", 1.3),
                        new UiTopoMap("bayareaGEO", "Bay Area, California", "*bayarea", 1.0),
                        new UiTopoMap("europe", "Europe", "*europe", 10.0),
                        new UiTopoMap("italy", "Italy", "*italy", 0.8),
                        new UiTopoMap("uk", "United Kingdom and Ireland", "*uk", 2.0),
                        new UiTopoMap("japan", "Japan", "*japan", 0.8),
                        new UiTopoMap("s_korea", "South Korea", "*s_korea", 0.75),
                        new UiTopoMap("taiwan", "Taiwan", "*taiwan", 0.7),
                        new UiTopoMap("africa", "Africa", "*africa", 0.7),
                        new UiTopoMap("oceania", "Oceania", "*oceania", 0.7),
                        new UiTopoMap("asia", "Asia", "*asia", 0.7)
                );

        return new UiExtension.Builder(CL, coreViews)
                .lionBundles(lionBundles)
                .messageHandlerFactory(messageHandlerFactory)
                .topoOverlayFactory(topoOverlayFactory)
                .topo2OverlayFactory(topo2OverlayFactory)
                .topoMapFactory(topoMapFactory)
                .resourcePath(CORE)
                .ui2()
                .build();
    }


    @Activate
    public void activate(ComponentContext context) {
        Serializer serializer = Serializer.using(KryoNamespaces.API,
                     ObjectNode.class, ArrayNode.class,
                     JsonNodeFactory.class, LinkedHashMap.class,
                     TextNode.class, BooleanNode.class,
                     LongNode.class, DoubleNode.class, ShortNode.class,
                     IntNode.class, NullNode.class, UiSessionToken.class);

        prefsConsistentMap = storageService.<String, ObjectNode>consistentMapBuilder()
                .withName(ONOS_USER_PREFERENCES)
                .withSerializer(serializer)
                .withRelaxedReadConsistency()
                .build();
        prefsConsistentMap.addListener(prefsListener);
        prefs = prefsConsistentMap.asJavaMap();

        tokensConsistentMap = storageService.<UiSessionToken, String>consistentMapBuilder()
                .withName(ONOS_SESSION_TOKENS)
                .withSerializer(serializer)
                .withRelaxedReadConsistency()
                .build();
        tokens = tokensConsistentMap.asJavaMap();
        cfgService.registerProperties(getClass());

        register(core);

        log.info("Started");
    }

    @Deactivate
    public void deactivate(ComponentContext context) {
        cfgService.unregisterProperties(getClass(), false);
        prefsConsistentMap.removeListener(prefsListener);
        eventHandlingExecutor.shutdown();
        UiWebSocketServlet.closeAll();
        unregister(core);
        log.info("Stopped");
    }

    @Modified
    protected void modified(ComponentContext context) {
        Dictionary<?, ?> properties = context.getProperties();
        Integer trafficRefresh = Tools.getIntegerProperty(properties, TRAFFIC_REFRESH_MS);

        if (trafficRefresh != null && trafficRefresh > 10) {
            AbstractTopoMonitor.setTrafficPeriod(trafficRefresh);
        } else if (trafficRefresh != null) {
            log.warn("trafficRefresh must be greater than 10");
        }

        log.info("Settings: trafficRefresh={}", trafficRefresh);
    }

    @Override
    public synchronized void register(UiExtension extension) {
        checkPermission(UI_WRITE);
        if (!extensions.contains(extension)) {
            extensions.add(extension);
            for (UiView view : extension.views()) {
                views.put(view.id(), extension);
            }
            UiWebSocketServlet.sendToAll(GUI_ADDED, null);
        }
    }

    @Override
    public synchronized void unregister(UiExtension extension) {
        checkPermission(UI_WRITE);
        extensions.remove(extension);
        extension.views().stream()
                .map(UiView::id).collect(toSet()).forEach(views::remove);
        UiWebSocketServlet.sendToAll(GUI_REMOVED, null);
    }

    @Override
    public synchronized void register(UiGlyphFactory glyphFactory) {
        checkPermission(GLYPH_WRITE);
        boolean glyphAdded = false;
        for (UiGlyph glyph : glyphFactory.glyphs()) {
            if (!glyphs.contains(glyph)) {
                glyphs.add(glyph);
                glyphAdded = true;
            }
        }
        if (glyphAdded) {
            UiWebSocketServlet.sendToAll(GLYPH_ADDED, null);
        }
    }

    @Override
    public synchronized void unregister(UiGlyphFactory glyphFactory) {
        checkPermission(GLYPH_WRITE);
        boolean glyphRemoved = false;
        for (UiGlyph glyph : glyphFactory.glyphs()) {
            glyphs.remove(glyph);
            glyphRemoved = true;
        }
        if (glyphRemoved) {
            UiWebSocketServlet.sendToAll(GLYPH_REMOVED, null);
        }
    }

    @Override
    public synchronized void register(UiTopoHighlighterFactory factory) {
        checkPermission(UI_WRITE);
        if (!highlighterFactories.contains(factory)) {
            highlighterFactories.add(factory);
            UiWebSocketServlet.sendToAll(GUI_ADDED, null);
        }
    }

    @Override
    public synchronized void unregister(UiTopoHighlighterFactory factory) {
        checkPermission(UI_WRITE);
        highlighterFactories.remove(factory);
        UiWebSocketServlet.sendToAll(GUI_REMOVED, null);
    }

    @Override
    public synchronized List<UiExtension> getExtensions() {
        checkPermission(UI_READ);
        return ImmutableList.copyOf(extensions);
    }

    @Override
    public synchronized List<UiGlyph> getGlyphs() {
        checkPermission(GLYPH_READ);
        return ImmutableList.copyOf(glyphs);
    }

    @Override
    public synchronized List<UiTopoHighlighterFactory> getTopoHighlighterFactories() {
        checkPermission(UI_READ);
        return ImmutableList.copyOf(highlighterFactories);
    }

    @Override
    public synchronized UiExtension getViewExtension(String viewId) {
        checkPermission(UI_READ);
        return views.get(viewId);
    }

    @Override
    public synchronized LionBundle getNavLionBundle() {
        return navLion;
    }

    @Override
    public void refreshModel() {
        sharedModel.reload();
    }

    @Override
    public Set<String> getUserNames() {
        ImmutableSet.Builder<String> builder = ImmutableSet.builder();
        prefs.keySet().forEach(k -> builder.add(userName(k)));
        return builder.build();
    }

    @Override
    public Map<String, ObjectNode> getPreferences(String username) {
        ImmutableMap.Builder<String, ObjectNode> builder = ImmutableMap.builder();
        prefs.entrySet().stream()
                .filter(e -> e.getKey().startsWith(username + SLASH))
                .forEach(e -> builder.put(keyName(e.getKey()), e.getValue()));
        return builder.build();
    }

    @Override
    public ObjectNode getPreference(String username, String key) {
        return prefs.get(key(username, key));
    }

    @Override
    public void setPreference(String username, String key, ObjectNode value) {
        if (value != null) {
            prefs.put(key(username, key), value);
        } else {
            prefs.remove(key(username, key));
        }
    }

    // =====================================================================
    // Provisional tracking of sprite definitions

    private final Map<String, JsonNode> sprites = Maps.newHashMap();

    @Override
    public Set<String> getNames() {
        return ImmutableSet.copyOf(sprites.keySet());
    }

    @Override
    public void put(String name, JsonNode spriteData) {
        log.info("Registered sprite definition [{}]", name);
        sprites.put(name, spriteData);
    }

    @Override
    public JsonNode get(String name) {
        return sprites.get(name);
    }

    private String key(String userName, String keyName) {
        return userName + SLASH + keyName;
    }


    private String userName(String key) {
        return key.split(SLASH)[IDX_USER];
    }

    private String keyName(String key) {
        return key.split(SLASH)[IDX_KEY];
    }


    // =====================================================================
    // UiTokenService

    @Override
    public UiSessionToken issueToken(String username) {
        UiSessionToken token = new UiSessionToken(tokenGen.nextSessionId());
        tokens.put(token, username);
        log.debug("UiSessionToken issued: {}", token);
        return token;
    }

    @Override
    public void revokeToken(UiSessionToken token) {
        if (token != null) {
            tokens.remove(token);
            log.debug("UiSessionToken revoked: {}", token);
        }
    }

    @Override
    public boolean isTokenValid(UiSessionToken token) {
        return token != null && tokens.containsKey(token);
    }

    private final class SessionTokenGenerator {
        private final SecureRandom random = new SecureRandom();

        /*
            This works by choosing 130 bits from a cryptographically secure
            random bit generator, and encoding them in base-32.

            128 bits is considered to be cryptographically strong, but each
            digit in a base 32 number can encode 5 bits, so 128 is rounded up
            to the next multiple of 5.

            This encoding is compact and efficient, with 5 random bits per
            character. Compare this to a random UUID, which only has 3.4 bits
            per character in standard layout, and only 122 random bits in total.

            Note that SecureRandom objects are expensive to initialize, so
            we'll want to keep it around and re-use it.
         */

        private String nextSessionId() {
            return new BigInteger(130, random).toString(32);
        }
    }

    // Auxiliary listener to preference map events.
    private class InternalPrefsListener
            implements MapEventListener<String, ObjectNode> {
        @Override
        public void event(MapEvent<String, ObjectNode> event) {
            eventHandlingExecutor.execute(() -> {
                String userName = userName(event.key());
                if (event.type() == MapEvent.Type.INSERT || event.type() == MapEvent.Type.UPDATE) {
                    UiWebSocketServlet.sendToUser(userName, UPDATE_PREFS, jsonPrefs());
                }
            });
        }

        private ObjectNode jsonPrefs() {
            ObjectNode json = mapper.createObjectNode();
            prefs.forEach((key, value) -> json.set(keyName(key), value));
            return json;
        }
    }
}

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
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onlab.util.Tools;
import org.onosproject.mastership.MastershipService;
import org.onosproject.store.serializers.KryoNamespaces;
import org.onosproject.store.service.ConsistentMap;
import org.onosproject.store.service.MapEvent;
import org.onosproject.store.service.MapEventListener;
import org.onosproject.store.service.Serializer;
import org.onosproject.store.service.StorageService;
import org.onosproject.ui.UiExtension;
import org.onosproject.ui.UiExtensionService;
import org.onosproject.ui.UiMessageHandlerFactory;
import org.onosproject.ui.UiPreferencesService;
import org.onosproject.ui.UiTopoMap;
import org.onosproject.ui.UiTopoMapFactory;
import org.onosproject.ui.UiTopoOverlayFactory;
import org.onosproject.ui.UiView;
import org.onosproject.ui.UiViewHidden;
import org.onosproject.ui.impl.topo.Topo2ViewMessageHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.google.common.collect.ImmutableList.of;
import static java.util.stream.Collectors.toSet;
import static org.onosproject.security.AppGuard.checkPermission;
import static org.onosproject.security.AppPermission.Type.UI_READ;
import static org.onosproject.security.AppPermission.Type.UI_WRITE;
import static org.onosproject.ui.UiView.Category.NETWORK;
import static org.onosproject.ui.UiView.Category.PLATFORM;

/**
 * Manages the user interface extensions.
 */
@Component(immediate = true)
@Service
public class UiExtensionManager
        implements UiExtensionService, UiPreferencesService, SpriteService {

    private static final ClassLoader CL = UiExtensionManager.class.getClassLoader();

    private static final String ONOS_USER_PREFERENCES = "onos-ui-user-preferences";
    private static final String CORE = "core";
    private static final String GUI_ADDED = "guiAdded";
    private static final String GUI_REMOVED = "guiRemoved";
    private static final String UPDATE_PREFS = "updatePrefs";
    private static final String SLASH = "/";

    private static final int IDX_USER = 0;
    private static final int IDX_KEY = 1;

    private final Logger log = LoggerFactory.getLogger(getClass());

    // List of all extensions
    private final List<UiExtension> extensions = Lists.newArrayList();

    // Map of views to extensions
    private final Map<String, UiExtension> views = Maps.newHashMap();

    // Core views & core extension
    private final UiExtension core = createCoreExtension();

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected MastershipService mastershipService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected StorageService storageService;

    // User preferences
    private ConsistentMap<String, ObjectNode> prefsConsistentMap;
    private Map<String, ObjectNode> prefs;
    private final MapEventListener<String, ObjectNode> prefsListener =
            new InternalPrefsListener();

    private final ObjectMapper mapper = new ObjectMapper();

    private final ExecutorService eventHandlingExecutor =
            Executors.newSingleThreadExecutor(
                    Tools.groupedThreads("onos/ui-ext-manager", "event-handler", log));

    // Creates core UI extension
    private UiExtension createCoreExtension() {
        List<UiView> coreViews = of(
                new UiView(PLATFORM, "app", "Applications", "nav_apps"),
                new UiView(PLATFORM, "settings", "Settings", "nav_settings"),
                new UiView(PLATFORM, "cluster", "Cluster Nodes", "nav_cluster"),
                new UiView(PLATFORM, "processor", "Packet Processors", "nav_processors"),
                new UiView(NETWORK, "topo", "Topology", "nav_topo"),

                // FIXME: leave commented out for now, while still under development
//                new UiView(NETWORK, "topo2", "New-Topo"),
//                new UiView(NETWORK, "topoX", "Topo-X"),

                new UiView(NETWORK, "device", "Devices", "nav_devs"),
                new UiViewHidden("flow"),
                new UiViewHidden("port"),
                new UiViewHidden("group"),
                new UiViewHidden("meter"),
                new UiView(NETWORK, "link", "Links", "nav_links"),
                new UiView(NETWORK, "host", "Hosts", "nav_hosts"),
                new UiView(NETWORK, "intent", "Intents", "nav_intents"),
                new UiView(NETWORK, "tunnel", "Tunnels", "nav_tunnels")
        );

        UiMessageHandlerFactory messageHandlerFactory =
                () -> ImmutableList.of(
                        new UserPreferencesMessageHandler(),
                        new TopologyViewMessageHandler(),
                        new Topo2ViewMessageHandler(),
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
                        new TunnelViewMessageHandler()
                );

        UiTopoOverlayFactory topoOverlayFactory =
                () -> ImmutableList.of(
                        new TrafficOverlay()
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
                .messageHandlerFactory(messageHandlerFactory)
                .topoOverlayFactory(topoOverlayFactory)
                .topoMapFactory(topoMapFactory)
                .resourcePath(CORE)
                .build();
    }

    @Activate
    public void activate() {
        Serializer serializer = Serializer.using(KryoNamespaces.API,
                ObjectNode.class, ArrayNode.class,
                JsonNodeFactory.class, LinkedHashMap.class,
                TextNode.class, BooleanNode.class,
                LongNode.class, DoubleNode.class, ShortNode.class,
                IntNode.class, NullNode.class);

        prefsConsistentMap = storageService.<String, ObjectNode>consistentMapBuilder()
                .withName(ONOS_USER_PREFERENCES)
                .withSerializer(serializer)
                .withRelaxedReadConsistency()
                .build();
        prefsConsistentMap.addListener(prefsListener);
        prefs = prefsConsistentMap.asJavaMap();
        register(core);
        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        prefsConsistentMap.removeListener(prefsListener);
        eventHandlingExecutor.shutdown();
        UiWebSocketServlet.closeAll();
        unregister(core);
        log.info("Stopped");
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
        extension.views().stream().map(UiView::id).collect(toSet()).forEach(views::remove);
        UiWebSocketServlet.sendToAll(GUI_REMOVED, null);
    }

    @Override
    public synchronized List<UiExtension> getExtensions() {
        checkPermission(UI_READ);
        return ImmutableList.copyOf(extensions);
    }

    @Override
    public synchronized UiExtension getViewExtension(String viewId) {
        checkPermission(UI_READ);
        return views.get(viewId);
    }

    @Override
    public Set<String> getUserNames() {
        ImmutableSet.Builder<String> builder = ImmutableSet.builder();
        prefs.keySet().forEach(k -> builder.add(userName(k)));
        return builder.build();
    }

    @Override
    public Map<String, ObjectNode> getPreferences(String userName) {
        ImmutableMap.Builder<String, ObjectNode> builder = ImmutableMap.builder();
        prefs.entrySet().stream()
                .filter(e -> e.getKey().startsWith(userName + SLASH))
                .forEach(e -> builder.put(keyName(e.getKey()), e.getValue()));
        return builder.build();
    }

    @Override
    public void setPreference(String userName, String preference, ObjectNode value) {
        prefs.put(key(userName, preference), value);
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
            prefs.entrySet().forEach(e -> json.set(keyName(e.getKey()), e.getValue()));
            return json;
        }
    }
}

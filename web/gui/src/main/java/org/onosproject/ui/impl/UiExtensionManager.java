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
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onosproject.mastership.MastershipService;
import org.onosproject.ui.UiExtension;
import org.onosproject.ui.UiExtensionService;
import org.onosproject.ui.UiMessageHandlerFactory;
import org.onosproject.ui.UiTopoOverlayFactory;
import org.onosproject.ui.UiView;
import org.onosproject.ui.UiViewHidden;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.google.common.collect.ImmutableList.of;
import static java.util.stream.Collectors.toSet;
import static org.onosproject.ui.UiView.Category.NETWORK;
import static org.onosproject.ui.UiView.Category.PLATFORM;

/**
 * Manages the user interface extensions.
 */
@Component(immediate = true)
@Service
public class UiExtensionManager implements UiExtensionService, SpriteService {

    private static final ClassLoader CL =
            UiExtensionManager.class.getClassLoader();
    private static final String CORE = "core";

    private final Logger log = LoggerFactory.getLogger(getClass());

    // List of all extensions
    private final List<UiExtension> extensions = Lists.newArrayList();

    // Map of views to extensions
    private final Map<String, UiExtension> views = Maps.newHashMap();

    // Core views & core extension
    private final UiExtension core = createCoreExtension();


    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected MastershipService mastershipService;

    // Creates core UI extension
    private UiExtension createCoreExtension() {
        List<UiView> coreViews = of(
                new UiView(PLATFORM, "app", "Applications", "nav_apps"),
                new UiView(PLATFORM, "settings", "Settings", "nav_settings"),
                new UiView(PLATFORM, "cluster", "Cluster Nodes", "nav_cluster"),
                new UiView(PLATFORM, "processor", "Packet Processors", "nav_processors"),
                new UiView(NETWORK, "topo", "Topology", "nav_topo"),
                new UiView(NETWORK, "device", "Devices", "nav_devs"),
                new UiViewHidden("flow"),
                new UiViewHidden("port"),
                new UiViewHidden("group"),
                new UiView(NETWORK, "link", "Links", "nav_links"),
                new UiView(NETWORK, "host", "Hosts", "nav_hosts"),
                new UiView(NETWORK, "intent", "Intents", "nav_intents"),
                //TODO add a new type of icon for tunnel
                new UiView(NETWORK, "tunnel", "Tunnels", "nav_links")
        );

        UiMessageHandlerFactory messageHandlerFactory =
                () -> ImmutableList.of(
                        new TopologyViewMessageHandler(),
                        new DeviceViewMessageHandler(),
                        new LinkViewMessageHandler(),
                        new HostViewMessageHandler(),
                        new FlowViewMessageHandler(),
                        new PortViewMessageHandler(),
                        new GroupViewMessageHandler(),
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

        return new UiExtension.Builder(CL, coreViews)
                .messageHandlerFactory(messageHandlerFactory)
                .topoOverlayFactory(topoOverlayFactory)
                .resourcePath(CORE)
                .build();
    }

    @Activate
    public void activate() {
        register(core);
        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        UiWebSocketServlet.closeAll();
        unregister(core);
        log.info("Stopped");
    }

    @Override
    public synchronized void register(UiExtension extension) {
        if (!extensions.contains(extension)) {
            extensions.add(extension);
            for (UiView view : extension.views()) {
                views.put(view.id(), extension);
            }
        }
    }

    @Override
    public synchronized void unregister(UiExtension extension) {
        extensions.remove(extension);
        extension.views().stream()
                .map(UiView::id).collect(toSet()).forEach(views::remove);
    }

    @Override
    public synchronized List<UiExtension> getExtensions() {
        return ImmutableList.copyOf(extensions);
    }

    @Override
    public synchronized UiExtension getViewExtension(String viewId) {
        return views.get(viewId);
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

}

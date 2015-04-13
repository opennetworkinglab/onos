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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Service;
import org.onosproject.ui.UiExtension;
import org.onosproject.ui.UiExtensionService;
import org.onosproject.ui.UiMessageHandlerFactory;
import org.onosproject.ui.UiView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

import static com.google.common.collect.ImmutableList.of;
import static java.util.stream.Collectors.toSet;

/**
 * Manages the user interface extensions.
 */
@Component(immediate = true)
@Service
public class UiExtensionManager implements UiExtensionService {

    private final Logger log = LoggerFactory.getLogger(getClass());

    // List of all extensions
    private final List<UiExtension> extensions = Lists.newArrayList();

    // Map of views to extensions
    private final Map<String, UiExtension> views = Maps.newHashMap();

    // Core views & core extension
    private final UiExtension core = createCoreExtension();


    // Creates core UI extension
    private static UiExtension createCoreExtension() {
        List<UiView> coreViews = of(new UiView("topo", "Topology View"),
                                    new UiView("device", "Devices"),
                                    new UiView("host", "Hosts"),
                                    new UiView("app", "Applications"),
                                    new UiView("intent", "Intents"),
                                    new UiView("cluster", "Cluster Nodes"),
                                    new UiView("sample", "Sample"));
        UiMessageHandlerFactory messageHandlerFactory =
                () -> ImmutableList.of(
                        new TopologyViewMessageHandler(),
                        new DeviceViewMessageHandler(),
                        new HostViewMessageHandler(),
                        new ApplicationViewMessageHandler(),
                        new IntentViewMessageHandler(),
                        new ClusterViewMessageHandler()
                );
        return new UiExtension(coreViews, messageHandlerFactory, "core",
                               UiExtensionManager.class.getClassLoader());
    }

    @Activate
    public void activate() {
        register(core);
        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
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
        extension.views().stream().map(UiView::id).collect(toSet()).forEach(views::remove);
    }

    @Override
    public synchronized List<UiExtension> getExtensions() {
        return ImmutableList.copyOf(extensions);
    }

    @Override
    public synchronized UiExtension getViewExtension(String viewId) {
        return views.get(viewId);
    }
}

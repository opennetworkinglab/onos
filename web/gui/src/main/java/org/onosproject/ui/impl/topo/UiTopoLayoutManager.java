/*
 *  Copyright 2016-present Open Networking Foundation
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.onosproject.ui.impl.topo;

import com.google.common.collect.ImmutableSet;
import org.onosproject.net.config.NetworkConfigEvent;
import org.onosproject.net.config.NetworkConfigListener;
import org.onosproject.net.config.NetworkConfigRegistry;
import org.onosproject.net.config.basics.BasicUiTopoLayoutConfig;
import org.onosproject.net.region.RegionId;
import org.onosproject.ui.UiTopoLayoutService;
import org.onosproject.ui.model.topo.UiRegion;
import org.onosproject.ui.model.topo.UiTopoLayout;
import org.onosproject.ui.model.topo.UiTopoLayoutId;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.onosproject.ui.model.topo.UiTopoLayoutId.DEFAULT_ID;

/**
 * Manages the user interface topology layouts.
 * Note that these layouts are persisted and distributed across the cluster.
 */
@Component(immediate = true, service = UiTopoLayoutService.class)
public class UiTopoLayoutManager implements UiTopoLayoutService {

    private static final String ID_NULL = "Layout ID cannot be null";
    private static final String LAYOUT_NULL = "Layout cannot be null";

    private final Logger log = LoggerFactory.getLogger(getClass());

    private final InternalConfigListener cfgListener = new InternalConfigListener();
    private final Map<UiTopoLayoutId, UiTopoLayout> layoutMap = new HashMap<>();

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected NetworkConfigRegistry cfgService;


    @Activate
    public void activate() {

        // Create and add the default layout, if needed.
        layoutMap.computeIfAbsent(DEFAULT_ID, UiTopoLayout::new);

        cfgService.addListener(cfgListener);
        cfgListener.initAllConfigs();

        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        cfgService.removeListener(cfgListener);

        log.info("Stopped");
    }


    @Override
    public UiTopoLayout getRootLayout() {
        return getLayout(DEFAULT_ID);
    }

    @Override
    public Set<UiTopoLayout> getLayouts() {
        return ImmutableSet.copyOf(layoutMap.values());
    }

    @Override
    public boolean addLayout(UiTopoLayout layout) {
        checkNotNull(layout, LAYOUT_NULL);
        return layoutMap.put(layout.id(), layout) == null;
    }

    @Override
    public UiTopoLayout getLayout(UiTopoLayoutId layoutId) {
        checkNotNull(layoutId, ID_NULL);
        return layoutMap.get(layoutId);
    }

    @Override
    public UiTopoLayout getLayout(RegionId regionId) {
        if (regionId == null || regionId.equals(UiRegion.NULL_ID)) {
            return getRootLayout();
        }

        List<UiTopoLayout> matchingLayouts = layoutMap.values().stream()
                .filter(l -> Objects.equals(regionId, l.regionId()))
                .collect(Collectors.toList());
        return matchingLayouts.isEmpty() ? null : matchingLayouts.get(0);
    }

    @Override
    public Set<UiTopoLayout> getPeerLayouts(UiTopoLayoutId layoutId) {
        checkNotNull(layoutId, ID_NULL);

        UiTopoLayout layout = layoutMap.get(layoutId);
        if (layout == null || layout.isRoot()) {
            return Collections.emptySet();
        }

        UiTopoLayoutId parentId = layout.parent();
        return layoutMap.values().stream()
                // all layouts who are NOT me (or root) and who share my parent...
                .filter(l -> !Objects.equals(l.id(), layoutId) &&
                        !Objects.equals(l.id(), UiTopoLayoutId.DEFAULT_ID) &&
                        Objects.equals(l.parent(), parentId))
                .collect(Collectors.toSet());
    }

    @Override
    public Set<UiTopoLayout> getChildren(UiTopoLayoutId layoutId) {
        checkNotNull(layoutId, ID_NULL);
        return layoutMap.values().stream()
                .filter(l -> !l.isRoot() && Objects.equals(l.parent(), layoutId))
                .collect(Collectors.toSet());
    }

    @Override
    public boolean removeLayout(UiTopoLayout layout) {
        checkNotNull(layout, LAYOUT_NULL);
        return layoutMap.remove(layout.id()) != null;
    }

    /*
     * Listens for changes to layout configs, updating instances as necessary
     */
    private class InternalConfigListener implements NetworkConfigListener {

        // look up the current config by layout ID and apply it
        private void updateLayoutConfig(UiTopoLayoutId id) {
            BasicUiTopoLayoutConfig cfg =
                    cfgService.getConfig(id, BasicUiTopoLayoutConfig.class);

            log.info("Updating Layout via config... {}: {}", id, cfg);

            UiTopoLayout layout = layoutMap.get(id);

            // NOTE: if a value is null, then that null-ness should be set
            // TODO: add setters on UiTopoLayout and implement...
//            layout
//              .region(cfg.region())
//              .parent(cfg.parent())
//              .geomap(cfg.geomap())
//              .sprites(cfg.sprites())
//              .scale(cfg.scale())
//              .offsetX(cfg.offsetX())
//              .offsetY(cfg.offsetY());
        }

        private void initAllConfigs() {
            log.info("Initializing layout configurations...");
            layoutMap.keySet().forEach(this::updateLayoutConfig);
        }

        @Override
        public void event(NetworkConfigEvent event) {
            UiTopoLayoutId id = (UiTopoLayoutId) event.subject();
            updateLayoutConfig(id);
        }

        @Override
        public boolean isRelevant(NetworkConfigEvent event) {
            return (event.type() == NetworkConfigEvent.Type.CONFIG_ADDED ||
                    event.type() == NetworkConfigEvent.Type.CONFIG_UPDATED) &&
                    event.configClass().equals(BasicUiTopoLayoutConfig.class);
        }
    }
}

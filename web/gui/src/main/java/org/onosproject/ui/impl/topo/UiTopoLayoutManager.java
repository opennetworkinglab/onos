/*
 *  Copyright 2016-present Open Networking Laboratory
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
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onlab.util.KryoNamespace;
import org.onosproject.net.region.RegionId;
import org.onosproject.store.serializers.KryoNamespaces;
import org.onosproject.store.service.ConsistentMap;
import org.onosproject.store.service.Serializer;
import org.onosproject.store.service.StorageService;
import org.onosproject.ui.UiTopoLayoutService;
import org.onosproject.ui.model.topo.UiRegion;
import org.onosproject.ui.model.topo.UiTopoLayout;
import org.onosproject.ui.model.topo.UiTopoLayoutId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
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
@Component(immediate = true)
@Service
public class UiTopoLayoutManager implements UiTopoLayoutService {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private static final String ID_NULL = "Layout ID cannot be null";
    private static final String LAYOUT_NULL = "Layout cannot be null";

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected StorageService storageService;

    private ConsistentMap<UiTopoLayoutId, UiTopoLayout> layouts;
    private Map<UiTopoLayoutId, UiTopoLayout> layoutMap;

    @Activate
    public void activate() {
        KryoNamespace.Builder kryoBuilder = new KryoNamespace.Builder()
                .register(KryoNamespaces.API)
                .register(UiTopoLayoutId.class)
                .register(UiTopoLayout.class);

        layouts = storageService.<UiTopoLayoutId, UiTopoLayout>consistentMapBuilder()
                .withSerializer(Serializer.using(kryoBuilder.build()))
                .withName("onos-topo-layouts")
                .withRelaxedReadConsistency()
                .build();
        layoutMap = layouts.asJavaMap();

        // Create and add the default layout, if needed.
        layoutMap.computeIfAbsent(DEFAULT_ID, k -> new UiTopoLayout(k, null, null));

        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
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
        return layouts.put(layout.id(), layout) == null;
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
        return layouts.remove(layout.id()) != null;
    }

}

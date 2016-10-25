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
package org.onosproject.ui;

import org.onosproject.net.region.RegionId;
import org.onosproject.ui.model.topo.UiTopoLayout;
import org.onosproject.ui.model.topo.UiTopoLayoutId;

import java.util.Set;

/**
 * Service for managing {@link UiTopoLayout} instances.
 * Those instances are used in conjunction with modeling the region-based
 * topology views.
 */
public interface UiTopoLayoutService {

    /**
     * Returns the top-level root layout, which always exists and cannot
     * be removed or associated directly with a region.
     *
     * @return root topology layout
     */
    UiTopoLayout getRootLayout();

    /**
     * Returns the set of available layouts (not including the root layout).
     *
     * @return set of available layouts
     */
    Set<UiTopoLayout> getLayouts();

    /**
     * Adds a layout to the system or updates an existing one.
     *
     * @param layout the layout to add or update
     * @return true if added; false if updated
     */
    boolean addLayout(UiTopoLayout layout);

    /**
     * Returns the layout with the specified identifier.
     *
     * @param layoutId layout identifier
     * @return layout or null if no such layout is found
     */
    UiTopoLayout getLayout(UiTopoLayoutId layoutId);

    /**
     * Returns the layout which has the backing region identified by
     * the given region identifier.
     *
     * @param regionId region identifier
     * @return corresponding layout
     */
    UiTopoLayout getLayout(RegionId regionId);

    /**
     * Returns the set of peer layouts of the specified layout. That is,
     * those layouts that share the same parent.
     *
     * @param layoutId layout identifier
     * @return set of peer layouts; empty set if layout has no peers
     */
    Set<UiTopoLayout> getPeerLayouts(UiTopoLayoutId layoutId);

    /**
     * Returns the set of the child layouts of the specified layout.
     *
     * @param layoutId layout identifier
     * @return set of child layouts; empty set if layout has no children
     */
    Set<UiTopoLayout> getChildren(UiTopoLayoutId layoutId);

    /**
     * Removes a layout from the system.
     *
     * @param layout the layout to remove
     * @return true if removed; false if no longer registered
     */
    boolean removeLayout(UiTopoLayout layout);

}

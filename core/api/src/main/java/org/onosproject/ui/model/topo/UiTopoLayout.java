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

package org.onosproject.ui.model.topo;

import org.onosproject.net.region.Region;
import org.onosproject.net.region.RegionId;

/**
 * Represents a specific "subset" of the UI model of the network topology
 * that a user might wish to view. Backed by a {@link Region}.
 */
public class UiTopoLayout {

    private final UiTopoLayoutId id;
    private final Region region;
    private final UiTopoLayoutId parent;

    /**
     * Created a new UI topology layout.
     *
     * @param id     layout identifier
     * @param region backing region
     * @param parent identifier of the parent layout
     */
    public UiTopoLayout(UiTopoLayoutId id, Region region, UiTopoLayoutId parent) {
        this.id = id;
        this.region = region;
        // NOTE: root layout is its own parent...
        this.parent = parent != null ? parent : this.id;
    }

    @Override
    public String toString() {
        return "{UiTopoLayout: " + id + "}";
    }

    /**
     * Returns the UI layout identifier.
     *
     * @return identifier of the layout
     */
    public UiTopoLayoutId id() {
        return id;
    }

    /**
     * Returns the backing region with which this layout is associated. Note
     * that this may be null (for the root layout).
     *
     * @return backing region
     */
    public Region region() {
        return region;
    }

    /**
     * Returns the identifier of the backing region. If this is the default
     * layout, the null-region ID will be returned, otherwise the ID of the
     * backing region for this layout will be returned; null in the case that
     * there is no backing region.
     *
     * @return backing region identifier
     */
    public RegionId regionId() {
        return isRoot() ? UiRegion.NULL_ID
                : (region == null ? null : region.id());
    }

    /**
     * Returns the parent layout identifier.
     *
     * @return parent layout identifier
     */
    public UiTopoLayoutId parent() {
        return parent;
    }

    /**
     * Returns true if this layout instance is at the top of the
     * hierarchy tree.
     *
     * @return true if this is the root layout
     */
    public boolean isRoot() {
        return id.equals(parent);
    }
}

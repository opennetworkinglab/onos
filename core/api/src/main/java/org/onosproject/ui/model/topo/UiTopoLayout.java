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

/**
 * Represents a specific "subset" of the UI model of the network topology
 * that a user might wish to view. Backed by a {@link Region}.
 */
public class UiTopoLayout {

    private final UiTopoLayoutId id;
    private final Region region;

    /**
     * Created a new UI topology layout.
     *
     * @param id     layout identifier
     * @param region backing region
     */
    public UiTopoLayout(UiTopoLayoutId id, Region region) {
        this.id = id;
        this.region = region;
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
     * Returns the backing region with which this layout is associated.
     *
     * @return backing region
     */
    public Region region() {
        return region;
    }

    // TODO: additional properties pertinent to the layout
}

/*
 * Copyright 2016-present Open Networking Laboratory
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

package org.onosproject.net.config.basics;

import org.onosproject.net.config.Config;
import org.onosproject.net.region.RegionId;
import org.onosproject.ui.model.topo.UiTopoLayoutId;

import static org.onosproject.net.region.RegionId.regionId;

/**
 * Basic configuration for UI topology layouts.
 */
public class BasicUiTopoLayoutConfig extends Config<UiTopoLayoutId> {

    private static final String REGION = "region";
    private static final String PARENT = "parent";

    @Override
    public boolean isValid() {
        return hasOnlyFields(REGION, PARENT);
    }

    /**
     * Returns the identifier of the backing region. This will be
     * null if there is no backing region.
     *
     * @return backing region identity
     */
    public RegionId getRegion() {
        String r = get(REGION, null);
        return r == null ? null : regionId(r);
    }

    /**
     * Returns the identifier of the parent layout.
     *
     * @return layout identifier of parent
     */
    public UiTopoLayoutId getParent() {
        String p = get(PARENT, null);
        return p == null ? UiTopoLayoutId.DEFAULT_ID : UiTopoLayoutId.layoutId(p);
    }

    // TODO: implement setters
}

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

package org.onosproject.ui.model.topo;

import org.onosproject.net.region.RegionId;

import static com.google.common.base.MoreObjects.toStringHelper;

/**
 * A synthetic link that encapsulates a UiLink instance and the region to
 * which it belongs.
 */
public class UiSynthLink {

    private final RegionId regionId;
    private final UiLink link;

    /**
     * Constructs a synthetic link with the given parameters.
     *
     * @param regionId the region to which the link belongs
     * @param link     the link instance
     */
    public UiSynthLink(RegionId regionId, UiLink link) {
        this.regionId = regionId;
        this.link = link;
    }

    @Override
    public String toString() {
        return toStringHelper(this)
                .add("region", regionId)
                .add("link", link)
                .toString();
    }

    /**
     * Returns the region identifier.
     *
     * @return the region ID
     */
    public RegionId regionId() {
        return regionId;
    }

    /**
     * Returns the link.
     *
     * @return the link
     */
    public UiLink link() {
        return link;
    }
}

/*
 * Copyright 2016-present Open Networking Foundation
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


/**
 * Designates a link between two region nodes.
 */
public class UiRegionLink extends UiLink {

    private static final String E_NOT_REGION_ID =
            "UI link identifier not region to region";

    // private (synthetic) region - region link
    private final RegionId regionA;
    private final RegionId regionB;

    /**
     * Creates a region to region UI link. Note that it is expected that the
     * link identifier is one that has region IDs as source and destination.
     *
     * @param topology parent topology
     * @param id       canonicalized link ID
     * @throws IllegalArgumentException if the link ID is not region-region
     */
    public UiRegionLink(UiTopology topology, UiLinkId id) {
        super(topology, id);
        regionA = id.regionA();
        regionB = id.regionB();
        if (regionA == null || regionB == null) {
            throw new IllegalArgumentException(E_NOT_REGION_ID);
        }
    }

    @Override
    public String endPointA() {
        return regionA.id();
    }

    @Override
    public String endPointB() {
        return regionB.id();
    }

    // no ports for end-points A and B

    /**
     * Returns the identity of the first region.
     *
     * @return first region ID
     */
    public RegionId regionA() {
        return regionA;
    }

    /**
     * Returns the identity of the second region.
     *
     * @return second region ID
     */
    public RegionId regionB() {
        return regionB;
    }
}

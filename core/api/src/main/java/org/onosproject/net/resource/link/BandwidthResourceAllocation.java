/*
 * Copyright 2014 Open Networking Laboratory
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
package org.onosproject.net.resource.link;

import com.google.common.base.MoreObjects;
import org.onosproject.net.resource.ResourceAllocation;
import org.onosproject.net.resource.ResourceType;

/**
 * Representation of allocated bandwidth resource.
 */
public class BandwidthResourceAllocation extends BandwidthResourceRequest
        implements ResourceAllocation {

    @Override
    public ResourceType type() {
        return ResourceType.BANDWIDTH;
    }

    /**
     * Creates a new {@link BandwidthResourceAllocation} with {@link BandwidthResource}
     * object.
     *
     * @param bandwidth allocated bandwidth
     */
    public BandwidthResourceAllocation(BandwidthResource bandwidth) {
        super(bandwidth);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("bandwidth", bandwidth())
                .toString();
    }
}

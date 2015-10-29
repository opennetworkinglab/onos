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

import java.util.Objects;

/**
 * Representation of allocated bandwidth resource.
 *
 * @deprecated in Emu Release
 */
@Deprecated
public class BandwidthResourceAllocation implements ResourceAllocation {
    private final BandwidthResource bandwidth;

    /**
     * Creates a new {@link BandwidthResourceRequest} with {@link BandwidthResource}
     * object.
     *
     * @param bandwidth {@link BandwidthResource} object to be requested
     */
    public BandwidthResourceAllocation(BandwidthResource bandwidth) {
        this.bandwidth = bandwidth;
    }

    /**
     * Returns the bandwidth resource.
     *
     * @return the bandwidth resource
     */
    public BandwidthResource bandwidth() {
        return bandwidth;
    }

    @Override
    public ResourceType type() {
        return ResourceType.BANDWIDTH;
    }

    @Override
    public int hashCode() {
        return bandwidth.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        final BandwidthResourceAllocation other = (BandwidthResourceAllocation) obj;
        return Objects.equals(this.bandwidth, other.bandwidth());
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("bandwidth", bandwidth)
                .toString();
    }
}

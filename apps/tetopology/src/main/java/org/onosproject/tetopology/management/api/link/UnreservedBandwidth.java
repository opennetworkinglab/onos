/*
 * Copyright 2016 Open Networking Laboratory
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
package org.onosproject.tetopology.management.api.link;

import java.math.BigDecimal;

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;

/**
 * Representation of a link's unreserved bandwidth.
 */
public class UnreservedBandwidth {
    private final short priority;
    private final BigDecimal bandwidth;

    /**
     * Create an instance of UnreservedBandwidth.
     *
     * @param priority allocatable priority of unreserved bandwidth
     * @param bandwidth bandwidth
     */
    public UnreservedBandwidth(short priority, BigDecimal bandwidth) {
        this.priority = priority;
        this.bandwidth = bandwidth;
    }

    /**
     * Returns the priority.
     *
     * @return the priority
     */
    public short priority() {
        return priority;
    }

    /**
     * Returns the bandwidth.
     *
     * @return the bandwidth
     */
    public BigDecimal bandwidth() {
        return bandwidth;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(priority, bandwidth);
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (object instanceof UnreservedBandwidth) {
            UnreservedBandwidth that = (UnreservedBandwidth) object;
            return Objects.equal(this.priority, that.priority) &&
                    Objects.equal(this.bandwidth, that.bandwidth);
        }
        return false;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("priority", priority)
                .add("bandwidth", bandwidth)
                .toString();
    }

}

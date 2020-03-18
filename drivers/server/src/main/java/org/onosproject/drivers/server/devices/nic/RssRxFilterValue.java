/*
 * Copyright 2018-present Open Networking Foundation
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

package org.onosproject.drivers.server.devices.nic;

import java.util.Objects;

import static com.google.common.base.Preconditions.checkArgument;
import static org.onosproject.drivers.server.Constants.MSG_NIC_FLOW_FILTER_RSS_NEGATIVE;

/**
 * A Receice-Side Scaling (RSS)-based Rx filter value.
 */
public final class RssRxFilterValue extends RxFilterValue implements Comparable {

    private int rssHash;

    /**
     * Constructs an RSS-based Rx filter.
     *
     * @param cpuId CPU ID of the server this tag will lead to
     */
    public RssRxFilterValue(int cpuId) {
        super(cpuId);
        setValue(0);
    }

    /**
     * Constructs an RSS-based Rx filter with specific hash.
     *
     * @param rssHash a hash value
     * @param cpuId CPU ID of the server this tag will lead to
     */
    public RssRxFilterValue(int rssHash, int cpuId) {
        super(cpuId);
        setValue(rssHash);
    }

    /**
     * Constructs an RSS-based Rx filter out of an existing one.
     *
     * @param other a source RssRxFilterValue object
     */
    public RssRxFilterValue(RssRxFilterValue other) {
        super(other.cpuId);
        setValue(other.value());
    }

    /**
     * Returns the value of this Rx filter.
     *
     * @return Flow rule as a string
     */
    public int value() {
        return this.rssHash;
    }

    /**
     * Sets the value of this Rx filter.
     *
     * @param rssHash the RSS hash
     */
    public void setValue(int rssHash) {
        checkArgument(rssHash >= 0, MSG_NIC_FLOW_FILTER_RSS_NEGATIVE);
        this.rssHash = rssHash;
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.rssHash);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if ((obj == null) || (!(obj instanceof RssRxFilterValue))) {
            return false;
        }

        RssRxFilterValue other = (RssRxFilterValue) obj;

        return this.value() == other.value();
    }

    @Override
    public int compareTo(Object other) {
        if (this == other) {
            return 0;
        }

        if (other == null) {
            return -1;
        }

        if (other instanceof RssRxFilterValue) {
            RssRxFilterValue otherRxVal = (RssRxFilterValue) other;

            int thisHash  = this.value();
            int otherHash = otherRxVal.value();

            if (thisHash > otherHash) {
                return 1;
            } else if (thisHash < otherHash) {
                return -1;
            } else {
                return 0;
            }
        }

        return -1;
    }

    @Override
    public String toString() {
        return Integer.toString(this.value());
    }

}

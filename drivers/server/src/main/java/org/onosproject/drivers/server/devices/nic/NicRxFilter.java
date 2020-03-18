/*
 * Copyright 2017-present Open Networking Foundation
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

import org.apache.commons.lang.ArrayUtils;
import com.google.common.base.MoreObjects;

import java.util.Set;
import java.util.Map;
import java.util.HashSet;
import java.util.HashMap;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.onosproject.drivers.server.Constants.MSG_NIC_FLOW_FILTER_NULL;
import static org.onosproject.drivers.server.Constants.MSG_NIC_FLOW_FILTER_MECH_NULL;

/**
 * Filtering mechanisms supported by a NIC device.
 */
public class NicRxFilter {

    /**
     * Supported Rx filters.
     */
    public enum RxFilter {

        /**
         * NIC dispatches traffic according to VLAN tags.
         */
        VLAN("vlan"),
        /**
         * NIC dispatches traffic according to the destination MAC addresses.
         */
        MAC("mac"),
        /**
         * NIC dispatches traffic according to MPLS tags.
         */
        MPLS("mpls"),
        /**
         * NIC dispatches traffic according to (generic) flow rules.
         */
        FLOW("flow"),
        /**
         * NIC dispatches traffic by hashing the values of some header fields.
         * This is also known as Receive-Side Scaling (RSS).
         */
        RSS("rss");

        private String rxFilter;

        // Statically maps primitives with enum types
        private static final Map<String, RxFilter> MAP = new HashMap<String, RxFilter>();
        static {
            for (RxFilter rxFilter : RxFilter.values()) {
                MAP.put(rxFilter.toString(), rxFilter);
            }
        }

        public static RxFilter getByName(String rxFilter) {
            return MAP.get(rxFilter.toLowerCase());
        }

        public static boolean isSupported(RxFilter rxFilter) {
            return MAP.containsKey(rxFilter.toString());
        }

        public static boolean isSupportedSet(Set<RxFilter> rxFilters) {
            for (RxFilter rf : rxFilters) {
                if (!MAP.containsKey(rf.toString())) {
                    return false;
                }
            }
            return true;
        }

        private RxFilter(String rxFilter) {
            this.rxFilter = rxFilter;
        }

        @Override
        public String toString() {
            return this.rxFilter;
        }

    }

    private Set<RxFilter> rxFilters;

    public NicRxFilter() {
        this.rxFilters = new HashSet<RxFilter>();
    }

    public NicRxFilter(RxFilter rxFilter) {
        checkNotNull(rxFilter, MSG_NIC_FLOW_FILTER_NULL);

        if (!ArrayUtils.contains(RxFilter.values(), rxFilter)) {
            throw new IllegalArgumentException(String.valueOf(rxFilter));
        }

        this.rxFilters = new HashSet<RxFilter>();
        rxFilters.add(rxFilter);
    }

    public NicRxFilter(NicRxFilter other) {
        checkNotNull(other, MSG_NIC_FLOW_FILTER_MECH_NULL);
        this.rxFilters = new HashSet<RxFilter>(other.rxFilters);
    }

    /**
     * Returns the set of Rx filters supported by this NIC.
     *
     * @return set of supported Rx filters
     */
    public Set<RxFilter> rxFilters() {
        return this.rxFilters;
    }

    /**
     * Adds a new Rx filter to this NIC.
     *
     * @param rxFilter an Rx filter to be added
     */
    public void addRxFilter(RxFilter rxFilter) {
        checkNotNull(rxFilter, MSG_NIC_FLOW_FILTER_NULL);
        this.rxFilters.add(rxFilter);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .omitNullValues()
                .add("rxFilters", rxFilters)
                .toString();
    }

}

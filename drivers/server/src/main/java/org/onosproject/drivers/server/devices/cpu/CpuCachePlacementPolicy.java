/*
 * Copyright 2020-present Open Networking Foundation
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

package org.onosproject.drivers.server.devices.cpu;

import java.util.Map;
import java.util.HashMap;

/**
 * Representation of a CPU cache's placement policies.
 */
public enum CpuCachePlacementPolicy {

    /**
     * Direct-mapped cache: the cache is organized into multiple
     * sets with a single cache line per set. A memory block can
     * only occupy a single cache line. A direct-mapped cache
     * corresponds to an 1-way set-associative cache.
     */
    DIRECT_MAPPED("Direct-Mapped"),
    /**
     * Fully-associative cache: the cache is organized into a
     * single cache set with multiple cache lines.
     * A memory block can occupy any of the cache lines.
     * A fully-associative cache with m lines corresponds to an
     * m-way set-associative cache.
     */
    FULLY_ASSOCIATIVE("Fully-Associative"),
    /**
     * Set-associative cache: the cache is organized into ‘n’
     * sets and each set contains ‘m’ cache lines. A memory block
     * is first mapped onto a set and then placed into any cache
     * line of this set.
     */
    SET_ASSOCIATIVE("Set-Associative");

    private String cachePolicy;

    // Statically maps CPU cache placement policies to enum types
    private static final Map<String, CpuCachePlacementPolicy> MAP =
        new HashMap<String, CpuCachePlacementPolicy>();
    static {
        for (CpuCachePlacementPolicy cp : CpuCachePlacementPolicy.values()) {
            MAP.put(cp.toString().toLowerCase(), cp);
        }
    }

    private CpuCachePlacementPolicy(String cachePolicy) {
        this.cachePolicy = cachePolicy;
    }

    public static CpuCachePlacementPolicy getByName(String cp) {
        return MAP.get(cp.toLowerCase());
    }

    @Override
    public String toString() {
        return this.cachePolicy;
    }

}

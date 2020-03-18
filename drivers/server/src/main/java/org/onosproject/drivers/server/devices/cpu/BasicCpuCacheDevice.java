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

/**
 * Represents an abstraction of a CPU cache.
 */
public interface BasicCpuCacheDevice {

    /**
     * Returns the ID of this cache.
     * A CPU cache ID is a pair of CPU cache level and type.
     *
     * @return cache ID
     */
    CpuCacheId cacheId();

    /**
     * Returns the placement policy of this cache.
     *
     * @return cache placement policy
     */
    CpuCachePlacementPolicy policy();

    /**
     * Returns the vendor of this cache.
     * Typically, a cache is part of a CPU,
     * therefore shares the same vendor.
     *
     * @return cache vendor
     */
    CpuVendor vendor();

    /**
     * Returns the capacity of this cache in kilo bytes.
     *
     * @return cache capacity in kilo bytes
     */
    long capacity();

    /**
     * Returns the number of sets this cache is split across.
     *
     * @return number of cache sets
     */
    int sets();

    /**
     * Returns the ways of associativity of this cache.
     *
     * @return ways of associativity
     */
    int associativityWays();

    /**
     * Returns the cache line's length in bytes.
     *
     * @return cache line's length in bytes
     */
    int lineLength();

    /**
     * Returns whether this CPU cache is shared among multiple cores
     * or dedicated to a specific core.
     *
     * @return sharing status of the CPU cache
     */
    boolean isShared();

}

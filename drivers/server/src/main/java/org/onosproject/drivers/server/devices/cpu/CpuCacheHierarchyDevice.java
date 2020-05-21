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

/**
 * Represents an abstraction of a CPU cache hierarchy.
 */
public interface CpuCacheHierarchyDevice {

    /**
     * Returns the vendor of this cache hierarchy.
     *
     * @return cache hierarchy vendor
     */
    CpuVendor vendor();

    /**
     * Returns the number of the system's CPU sockets.
     * This number affects the number of caches present in the system.
     *
     * @return number of CPU sockets
     */
    int socketsNb();

    /**
     * Returns the number of the system's CPU cores.
     * This number affects the number of caches present in the system.
     *
     * @return number of CPU cores
     */
    int coresNb();

    /**
     * Returns the number of cache levels of this cache.
     *
     * @return cache level
     */
    int levels();

    /**
     * Returns the capacity local to each CPU core in kilo bytes.
     *
     * @return per CPU core local cache capacity in kilo bytes
     */
    long perCoreCapacity();

    /**
     * Returns the capacity of the last-level cache in kilo bytes.
     *
     * @return last-level cache's capacity in kilo bytes
     */
    long llcCapacity();

    /**
     * Returns the capacity of the entire cache hierarchy in kilo bytes.
     *
     * @return entire cache hierarchy's capacity in kilo bytes
     */
    long totalCapacity();

    /**
     * Returns the CPU cache hierarchy.
     *
     * @return CPU cache hierarchy
     */
    Map<CpuCacheId, BasicCpuCacheDevice> cacheHierarchy();

}

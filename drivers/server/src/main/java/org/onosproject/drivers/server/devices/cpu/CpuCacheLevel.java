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
 * Representation of a CPU cache's levels.
 */
public enum CpuCacheLevel {

    /**
     * Depending on the hardware architecture and vendor,
     * a CPU cache hierarchy might comprise of multiple levels
     * of CPU caches. Some hardware architectures use 3 levels
     * of CPU caches, while others use 4. In the former case,
     * the last-level cache (LLC) is the L3 cache, while in the
     * latter case LLC corresponds to the cache after L3
     * (conceptually L4).
     */
    REGISTER("Register"), // CPU register
    L1("L1"),             // L1 cache
    L2("L2"),             // L2 cache
    L3("L3"),             // L3 cache
    L4("L4"),             // L4 cache
    LLC("LLC");           // Last-level cache

    private String level;

    // Statically maps CPU cache levels to enum types
    private static final Map<String, CpuCacheLevel> MAP =
        new HashMap<String, CpuCacheLevel>();
    static {
        for (CpuCacheLevel cl : CpuCacheLevel.values()) {
            MAP.put(cl.toString().toLowerCase(), cl);
        }
    }

    private CpuCacheLevel(String level) {
        this.level = level;
    }

    public static CpuCacheLevel getByName(String cl) {
        return MAP.get(cl.toLowerCase());
    }

    public static int toNumber(CpuCacheLevel cacheLevel) {
        return Integer.parseInt(cacheLevel.toString().split("L")[1]);
    }

    public static boolean isLlc(CpuCacheLevel cacheLevel) {
        return (cacheLevel == LLC);
    }

    @Override
    public String toString() {
        return this.level;
    }

}

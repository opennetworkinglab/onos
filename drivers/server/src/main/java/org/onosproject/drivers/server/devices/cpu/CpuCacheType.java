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
 * Representation of a CPU cache's types.
 */
public enum CpuCacheType {

    /**
     * A CPU cache can store data, CPU instructions,
     * or both (i.e., unified case).
     */
    DATA("Data"),
    INSTRUCTION("Instruction"),
    UNIFIED("Unified");

    private String cacheType;

    // Statically maps CPU cache types to enum types
    private static final Map<String, CpuCacheType> MAP =
        new HashMap<String, CpuCacheType>();
    static {
        for (CpuCacheType ct : CpuCacheType.values()) {
            MAP.put(ct.toString().toLowerCase(), ct);
        }
    }

    private CpuCacheType(String cacheType) {
        this.cacheType = cacheType;
    }

    public static CpuCacheType getByName(String ct) {
        return MAP.get(ct.toLowerCase());
    }

    public static boolean isData(CpuCacheType cacheType) {
        return (cacheType == DATA);
    }

    public static boolean isInstruction(CpuCacheType cacheType) {
        return (cacheType == INSTRUCTION);
    }

    public static boolean isUnified(CpuCacheType cacheType) {
        return (cacheType == UNIFIED);
    }

    @Override
    public String toString() {
        return this.cacheType;
    }

}

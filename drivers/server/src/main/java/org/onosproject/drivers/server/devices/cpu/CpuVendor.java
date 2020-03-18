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

package org.onosproject.drivers.server.devices.cpu;

import java.util.Map;
import java.util.HashMap;

/**
 * Representation of a CPU vendor.
 */
public enum CpuVendor {

    INTEL("GenuineIntel"),
    AMD("AuthenticAMD");

    private String vendor;

    // Statically maps CPU vendor names to enum types
    private static final Map<String, CpuVendor> MAP =
        new HashMap<String, CpuVendor>();
    static {
        for (CpuVendor cv : CpuVendor.values()) {
            MAP.put(cv.toString().toLowerCase(), cv);
        }
    }

    private CpuVendor(String vendor) {
        this.vendor = vendor;
    }

    public static CpuVendor getByName(String cv) {
        return MAP.get(cv.toLowerCase());
    }

    @Override
    public String toString() {
        return this.vendor;
    }

}

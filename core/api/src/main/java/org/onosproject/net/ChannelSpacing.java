/*
 * Copyright 2015-present Open Networking Foundation
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
package org.onosproject.net;

import org.onlab.util.Frequency;

/**
 * Represents interval frequency between two neighboring wavelengths.
 */
public enum ChannelSpacing {
    CHL_100GHZ(100_000),        // 100 GHz
    CHL_50GHZ(50_000),          // 50 GHz
    CHL_25GHZ(25_000),          // 25 GHz
    CHL_12P5GHZ(12_500),        // 12.5 GHz
    CHL_6P25GHZ(6_250),         // 6.25 GHz
    CHL_0GHZ(0);                // 0 GHz (Unknown)

    private final Frequency frequency;

    /**
     * Creates an instance with the specified interval in MHz.
     *
     * @param value interval of neighboring wavelengths in MHz.
     */
    ChannelSpacing(long value) {
        this.frequency = Frequency.ofMHz(value);
    }

    public Frequency frequency() {
        return frequency;
    }
}

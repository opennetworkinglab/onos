/*
 * Copyright 2015 Open Networking Laboratory
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
    CHL_100GHZ(100),        // 100 GHz
    CHL_50GHZ(50),          // 50 GHz
    CHL_25GHZ(25),          // 25 GHz
    CHL_12P5GHZ(12.5),      // 12.5 GHz
    CHL_6P25GHZ(6.25);       // 6.25 GHz

    private final Frequency frequency;

    /**
     * Creates an instance with the specified interval in GHz.
     *
     * @param value interval of neighboring wavelengths in GHz.
     */
    ChannelSpacing(double value) {
        this.frequency = Frequency.ofGHz(value);
    }

    public Frequency frequency() {
        return frequency;
    }
}

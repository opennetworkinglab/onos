/*
 * Copyright 2021-present Open Networking Foundation
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

package org.onosproject.net.pi.runtime;

import com.google.common.annotations.Beta;

/**
 * Type of meter band of a protocol-independent pipeline.
 */
@Beta
public enum PiMeterBandType {
    /**
     * Committed band config.
     */
    COMMITTED("committed"),

    /**
     * Peak band config.
     */
    PEAK("peak");

    private final String humanReadableName;

    PiMeterBandType(String humanReadableName) {
        this.humanReadableName = humanReadableName;
    }

    /**
     * Returns a human readable representation of this PI meter band type (useful
     * for logging).
     *
     * @return string
     */
    public String humanReadableName() {
        return humanReadableName;
    }
}

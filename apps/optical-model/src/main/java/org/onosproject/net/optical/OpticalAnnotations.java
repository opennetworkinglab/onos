/*
 * Copyright 2016-present Open Networking Laboratory
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
package org.onosproject.net.optical;

import com.google.common.annotations.Beta;

/**
 * Collection of keys for annotation for optical devices.
 */
@Beta
public final class OpticalAnnotations {

    private OpticalAnnotations() {}

    /**
     * Annotation key for mininum frequency in Hz.
     * Value is expected to be an integer.
     */
    public static final String MIN_FREQ_HZ = "minFrequency";

    /**
     * Annotation key for maximum frequency in Hz.
     * Value is expected be an integer.
     */
    public static final String MAX_FREQ_HZ = "maxFrequency";

    /**
     * Annotation key for grid in Hz.
     * Value is expected to be an integer.
     */
    public static final String GRID_HZ = "grid";

    /**
     * Annotation key for optical port's target power.
     * Value is expected to be an integer.
     */
    public static final String TARGET_POWER = "targetPower";

    /**
     * Annotation key for optical port's current power.
     * Value is expected to be an integer.
     */
    public static final String CURRENT_POWER = "currentPower";

    /**
     * Annotation key for optical port's neighbor's DeviceId#toString().
     */
    public static final String NEIGHBOR_ID = "neighborDeviceId";

    /**
     * Annotation key for optical port's neighbor's PortNumber#toString().
     * Value is expected to be an integer.
     */
    public static final String NEIGHBOR_PORT = "neighborPort";
}

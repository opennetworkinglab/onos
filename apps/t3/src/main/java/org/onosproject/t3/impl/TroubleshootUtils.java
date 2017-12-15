/*
 * Copyright 2018-present Open Networking Foundation
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

package org.onosproject.t3.impl;

import com.google.common.collect.ImmutableMap;

import java.util.Map;

/**
 * Utility class for the troubleshooting tool.
 */
final class TroubleshootUtils {

    private TroubleshootUtils() {
        //Banning construction
    }

    /**
     * Map defining if a specific driver is for a HW switch.
     */
    //Done with builder() instead of of() for clarity
    static Map<String, Boolean> hardwareOfdpaMap = ImmutableMap.<String, Boolean>builder()
            .put("ofdpa", true)
            .put("ofdpa3", true)
            .put("qmx-ofdpa3", true)
            .put("as7712-32x-premium", true)
            .put("as5912-54x-premium", true)
            .put("as5916-54x-premium", true)
            .put("accton-ofdpa3", true)
            .put("znyx-ofdpa", true)
            .build();
}

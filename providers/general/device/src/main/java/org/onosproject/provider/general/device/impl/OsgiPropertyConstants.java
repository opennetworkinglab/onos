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

package org.onosproject.provider.general.device.impl;

/**
 * Constants for default values of configurable properties.
 */
public final class OsgiPropertyConstants {

    private OsgiPropertyConstants() {}

    public static final String STATS_POLL_FREQUENCY = "deviceStatsPollFrequency";
    public static final int STATS_POLL_FREQUENCY_DEFAULT = 10;

    public static final String PROBE_FREQUENCY = "deviceProbeFrequency";
    public static final int PROBE_FREQUENCY_DEFAULT = 10;

    public static final String OP_TIMEOUT_SHORT = "deviceOperationTimeoutShort";
    public static final int OP_TIMEOUT_SHORT_DEFAULT = 10;

}

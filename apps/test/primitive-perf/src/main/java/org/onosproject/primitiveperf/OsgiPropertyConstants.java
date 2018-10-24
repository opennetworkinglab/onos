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

package org.onosproject.primitiveperf;

/**
 * Name/Value constants for properties.
 */
public final class OsgiPropertyConstants {
    private OsgiPropertyConstants() {
    }

    public static final String NUM_CLIENTS = "numClients";
    public static final int NUM_CLIENTS_DEFAULT = 8;

    public static final String WRITE_PERCENTAGE = "writePercentage";
    public static final int WRITE_PERCENTAGE_DEFAULT = 100;

    public static final String NUM_KEYS = "numKeys";
    public static final int NUM_KEYS_DEFAULT = 100000;

    public static final String KEY_LENGTH = "keyLength";
    public static final int KEY_LENGTH_DEFAULT = 32;

    public static final String NUM_UNIQUE_VALUES = "numValues";
    public static final int NUM_UNIQUE_VALUES_DEFAULT = 100;

    public static final String VALUE_LENGTH = "valueLength";
    public static final int VALUE_LENGTH_DEFAULT = 1024;

    public static final String INCLUDE_EVENTS = "includeEvents";
    public static final boolean INCLUDE_EVENTS_DEFAULT = false;

    public static final String DETERMINISTIC = "deterministic";
    public static final boolean DETERMINISTIC_DEFAULT = true;

}

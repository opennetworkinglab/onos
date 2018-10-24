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

package org.onosproject.intentperf;

/**
 * Name/Value constants for properties.
 */
public final class OsgiPropertyConstants {
    private OsgiPropertyConstants() {
    }

    public static final String NUM_KEYS = "numKeys";
    public static final int NUM_KEYS_DEFAULT = 40000;

    public static final String NUM_WORKERS = "numWorkers";
    public static final int NUM_WORKERS_DEFAULT = 1;

    public static final String CYCLE_PERIOD = "cyclePeriod";
    public static final int CYCLE_PERIOD_DEFAULT = 1000; //ms

    public static final String NUM_NEIGHBORS = "numNeighbors";
    public static final int NUM_NEIGHBORS_DEFAULT = 0;
}

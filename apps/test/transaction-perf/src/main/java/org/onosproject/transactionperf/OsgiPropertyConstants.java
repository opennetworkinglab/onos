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

package org.onosproject.transactionperf;

/**
 * Name/Value constants for properties.
 */
public final class OsgiPropertyConstants {
    private OsgiPropertyConstants() {
    }

    public static final String MAP_NAME = "mapName";
    public static final String MAP_NAME_DEFAULT = "transaction-perf";

    public static final String READ_PERCENTAGE = "readPercentage";
    public static final double READ_PERCENTAGE_DEFAULT = .9;

    public static final String TOTAL_OPERATIONS = "totalOperationsPerTransaction";
    public static final int TOTAL_OPERATIONS_DEFAULT = 1000;

    public static final String WITH_CONTENTION = "withContention";
    public static final boolean WITH_CONTENTION_DEFAULT = false;

    public static final String WITH_RETRIES = "withRetries";
    public static final boolean WITH_RETRIES_DEFAULT = false;

    public static final String REPORT_INTERVAL_SECONDS = "reportIntervalSeconds";
    public static final int REPORT_INTERVAL_SECONDS_DEFAULT = 1;
}

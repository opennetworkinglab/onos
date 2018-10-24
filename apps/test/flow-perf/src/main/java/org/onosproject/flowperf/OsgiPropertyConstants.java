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

package org.onosproject.flowperf;

/**
 * Name/Value constants for properties.
 */
public final class OsgiPropertyConstants {
    private OsgiPropertyConstants() {
    }

    public static final String TOTAL_FLOWS = "totalFlows";
    public static final int TOTAL_FLOWS_DEFAULT = 100000;

    public static final String BATCH_SIZE = "batchSize";
    public static final int BATCH_SIZE_DEFAULT = 200;

    public static final String TOTAL_THREADS = "totalThreads";
    public static final int TOTAL_THREADS_DEFAULT = 1;
}

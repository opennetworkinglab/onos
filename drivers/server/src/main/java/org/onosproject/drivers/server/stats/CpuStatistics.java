/*
 * Copyright 2017-present Open Networking Foundation
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

package org.onosproject.drivers.server.stats;

/**
 * CPU statistics API.
 */
public interface CpuStatistics {

    /**
     * Returns the ID of a CPU core.
     *
     * @return CPU core identifier
     */
    int id();

    /**
     * Returns the load of this CPU core.
     * This is a value in [0, 1].
     * Zero means no load, while one means fully loaded.
     *
     * @return load of a CPU core
     */
    float load();

    /**
     * Returns the status (true=busy, false=free) of a CPU core.
     *
     * @return boolean CPU core status
     */
    boolean busy();

}

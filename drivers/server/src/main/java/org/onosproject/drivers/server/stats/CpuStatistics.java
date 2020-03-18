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

import java.util.Optional;

/**
 * CPU statistics API.
 */
public interface CpuStatistics {

    /**
     * Minimum and maximum CPU load values.
     */
    static final float MIN_CPU_LOAD = (float) 0.0;
    static final float MAX_CPU_LOAD = (float) 1.0;

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
     * Returns the hardware queue identifier associated with this CPU core.
     *
     * @return hardware queue identifier
     */
    int queue();

    /**
     * Returns the status (true=busy, false=free) of a CPU core.
     *
     * @return boolean CPU core status
     */
    boolean busy();

    /**
     * Returns the amount of time in ms since the CPU has been busy,
     * or a negative value if the CPU is idle.
     *
     * @return int time in ms since the CPU has been busy
     */
    int busySince();

    /**
     * Returns the unit of throughput values.
     *
     * @return throughput monitoring unit
     */
    Optional<MonitoringUnit> throughputUnit();

    /**
     * Returns the average throughput of this CPU core,
     * expressed in throughputUnit() monitoring units.
     *
     * @return average throughput of a CPU core
     */
    Optional<Float> averageThroughput();

    /**
     * Returns the unit of latency values.
     *
     * @return latency monitoring unit
     */
    Optional<MonitoringUnit> latencyUnit();

    /**
     * Returns the minimum latency incurred by a CPU core,
     * expressed in latencyUnit() monitoring units.
     *
     * @return minimum latency incurred by a CPU core
     */
    Optional<Float> minLatency();

    /**
     * Returns the average latency incurred by a CPU core,
     * expressed in latencyUnit() monitoring units.
     *
     * @return average latency incurred by a CPU core
     */
    Optional<Float> averageLatency();

    /**
     * Returns the maximum latency incurred by a CPU core,
     * expressed in latencyUnit() monitoring units.
     *
     * @return maximum latency incurred by a CPU core
     */
    Optional<Float> maxLatency();

}

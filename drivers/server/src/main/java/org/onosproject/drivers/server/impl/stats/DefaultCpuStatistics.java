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

package org.onosproject.drivers.server.impl.stats;

import org.onosproject.drivers.server.devices.cpu.CpuCoreId;
import org.onosproject.drivers.server.stats.CpuStatistics;
import org.onosproject.drivers.server.stats.MonitoringUnit;

import org.onosproject.net.DeviceId;

import com.google.common.base.MoreObjects;

import java.util.Optional;

import static org.onosproject.drivers.server.stats.MonitoringUnit.LatencyUnit;
import static org.onosproject.drivers.server.stats.MonitoringUnit.ThroughputUnit;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkArgument;
import static org.onosproject.drivers.server.Constants.MSG_CPU_CORE_NEGATIVE;
import static org.onosproject.drivers.server.Constants.MSG_CPU_LOAD_NEGATIVE;
import static org.onosproject.drivers.server.Constants.MSG_DEVICE_ID_NULL;

/**
 * Default implementation for CPU statistics.
 */
public final class DefaultCpuStatistics implements CpuStatistics {

    private static final LatencyUnit DEF_LATENCY_UNIT = LatencyUnit.NANO_SECOND;
    private static final ThroughputUnit DEF_THROUGHPUT_UNIT = ThroughputUnit.MBPS;

    private final DeviceId deviceId;

    private final int id;
    private final float load;
    private final int queue;
    private final int busySince;
    private final Optional<MonitoringUnit> throughputUnit;
    private final Optional<Float> averageThroughput;
    private final Optional<MonitoringUnit> latencyUnit;
    private final Optional<Float> minLatency;
    private final Optional<Float> averageLatency;
    private final Optional<Float> maxLatency;

    private DefaultCpuStatistics(DeviceId deviceId, int id, float load, int queue, int busySince) {
        this(deviceId, id, load, queue, busySince, null, -1, null, -1, -1, -1);
    }

    private DefaultCpuStatistics(DeviceId deviceId, int id, float load, int queue, int busySince,
            MonitoringUnit throughputUnit, float averageThroughput, MonitoringUnit latencyUnit,
            float minLatency, float averageLatency, float maxLatency) {
        checkNotNull(deviceId, MSG_DEVICE_ID_NULL);
        checkArgument((id >= 0) && (id < CpuCoreId.MAX_CPU_CORE_NB), MSG_CPU_CORE_NEGATIVE);
        checkArgument((load >= CpuStatistics.MIN_CPU_LOAD) &&
                      (load <= CpuStatistics.MAX_CPU_LOAD), MSG_CPU_LOAD_NEGATIVE);

        this.deviceId = deviceId;
        this.id = id;
        this.load = load;
        this.queue = queue;
        this.busySince = busySince;

        this.throughputUnit = (throughputUnit == null) ?
                Optional.empty() : Optional.ofNullable(throughputUnit);
        this.averageThroughput = (averageThroughput < 0) ?
                Optional.empty() : Optional.ofNullable(averageThroughput);
        this.latencyUnit = (latencyUnit == null) ?
                Optional.empty() : Optional.ofNullable(latencyUnit);
        this.minLatency = (minLatency < 0) ?
                Optional.empty() : Optional.ofNullable(minLatency);
        this.averageLatency = (averageLatency < 0) ?
                Optional.empty() : Optional.ofNullable(averageLatency);
        this.maxLatency = (maxLatency < 0) ?
                Optional.empty() : Optional.ofNullable(maxLatency);
    }

    // Constructor for serializer
    private DefaultCpuStatistics() {
        this.deviceId = null;
        this.id = 0;
        this.load = 0;
        this.queue = 0;
        this.busySince = -1;

        this.throughputUnit = null;
        this.averageThroughput = null;
        this.latencyUnit = null;
        this.minLatency = null;
        this.averageLatency = null;
        this.maxLatency = null;
    }

    /**
     * Creates a builder for DefaultCpuStatistics object.
     *
     * @return builder object for DefaultCpuStatistics object
     */
    public static DefaultCpuStatistics.Builder builder() {
        return new Builder();
    }

    @Override
    public int id() {
        return this.id;
    }

    @Override
    public float load() {
        return this.load;
    }

    @Override
    public int queue() {
        return this.queue;
    }

    @Override
    public boolean busy() {
        return this.busySince >= 0;
    }

    @Override
    public int busySince() {
        return this.busySince;
    }

    @Override
    public Optional<MonitoringUnit> throughputUnit() {
        return this.throughputUnit;
    }

    @Override
    public Optional<Float> averageThroughput() {
        return this.averageThroughput;
    }

    @Override
    public Optional<MonitoringUnit> latencyUnit() {
        return this.latencyUnit;
    }

    @Override
    public Optional<Float> minLatency() {
        return this.minLatency;
    }

    @Override
    public Optional<Float> averageLatency() {
        return this.averageLatency;
    }

    @Override
    public Optional<Float> maxLatency() {
        return this.maxLatency;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .omitNullValues()
                .add("device", deviceId)
                .add("id",     id())
                .add("load",   load())
                .add("queue",  queue())
                .add("busySince", busySince())
                .add("throughputUnit", throughputUnit.orElse(null))
                .add("averageThroughput", averageThroughput.orElse(null))
                .add("latencyUnit", latencyUnit.orElse(null))
                .add("minLatency", minLatency.orElse(null))
                .add("averageLatency", averageLatency.orElse(null))
                .add("maxLatency", maxLatency.orElse(null))
                .toString();
    }

    public static final class Builder {

        DeviceId deviceId;
        int      id;
        float    load = 0;
        int      queue = -1;
        int      busySince = -1;

        MonitoringUnit throughputUnit = DEF_THROUGHPUT_UNIT;
        float averageThroughput = -1;
        MonitoringUnit latencyUnit = DEF_LATENCY_UNIT;
        float minLatency = -1;
        float averageLatency = -1;
        float maxLatency = -1;

        private Builder() {

        }

        /**
         * Sets the device identifier.
         *
         * @param deviceId device identifier
         * @return builder object
         */
        public Builder setDeviceId(DeviceId deviceId) {
            this.deviceId = deviceId;

            return this;
        }

        /**
         * Sets the CPU ID.
         *
         * @param id CPU ID
         * @return builder object
         */
        public Builder setId(int id) {
            this.id = id;

            return this;
        }

        /**
         * Sets the CPU load.
         *
         * @param load CPU load
         * @return builder object
         */
        public Builder setLoad(float load) {
            this.load = load;

            return this;
        }

        /**
         * Sets the hardware queue ID associated with this core.
         *
         * @param queue hardware queue ID
         * @return builder object
         */
        public Builder setQueue(int queue) {
            this.queue = queue;

            return this;
        }

        /**
         * Sets the CPU status (free or busy since some ms).
         *
         * @param busySince CPU busy time in ms, -1 if not busy
         * @return builder object
         */
        public Builder setBusySince(int busySince) {
            this.busySince = busySince;

            return this;
        }

        /**
         * Sets the throughput unit.
         *
         * @param throughputUnitStr throughput unit as a string
         * @return builder object
         */
        public Builder setThroughputUnit(String throughputUnitStr) {
            this.throughputUnit = ThroughputUnit.getByName(throughputUnitStr);

            return this;
        }

        /**
         * Sets the average throughput.
         *
         * @param averageThroughput average throughput
         * @return builder object
         */
        public Builder setAverageThroughput(float averageThroughput) {
            this.averageThroughput = averageThroughput;

            return this;
        }

        /**
         * Sets the latency unit.
         *
         * @param latencyUnitStr latency unit as a string
         * @return builder object
         */
        public Builder setLatencyUnit(String latencyUnitStr) {
            this.latencyUnit = LatencyUnit.getByName(latencyUnitStr);

            return this;
        }

        /**
         * Sets the minimum latency.
         *
         * @param minLatency minimum latency
         * @return builder object
         */
        public Builder setMinLatency(float minLatency) {
            this.minLatency = minLatency;

            return this;
        }

        /**
         * Sets the average latency.
         *
         * @param averageLatency average latency
         * @return builder object
         */
        public Builder setAverageLatency(float averageLatency) {
            this.averageLatency = averageLatency;

            return this;
        }

        /**
         * Sets the maximum latency.
         *
         * @param maxLatency maximum latency
         * @return builder object
         */
        public Builder setMaxLatency(float maxLatency) {
            this.maxLatency = maxLatency;

            return this;
        }

        /**
         * Creates a DefaultCpuStatistics object.
         *
         * @return DefaultCpuStatistics object
         */
        public DefaultCpuStatistics build() {
            return new DefaultCpuStatistics(
                deviceId, id, load, queue, busySince,
                throughputUnit, averageThroughput,
                latencyUnit, minLatency, averageLatency, maxLatency);
        }

    }

}

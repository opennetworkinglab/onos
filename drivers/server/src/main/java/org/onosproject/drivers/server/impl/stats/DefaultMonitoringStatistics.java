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
import org.onosproject.drivers.server.stats.MemoryStatistics;
import org.onosproject.drivers.server.stats.MonitoringStatistics;
import org.onosproject.drivers.server.stats.TimingStatistics;

import org.onosproject.net.DeviceId;
import org.onosproject.net.device.PortStatistics;

import com.google.common.base.MoreObjects;

import java.util.Collection;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkArgument;
import static org.onosproject.drivers.server.Constants.MSG_CPU_CORE_NEGATIVE;
import static org.onosproject.drivers.server.Constants.MSG_DEVICE_ID_NULL;
import static org.onosproject.drivers.server.Constants.MSG_STATS_CPU_NULL;
import static org.onosproject.drivers.server.Constants.MSG_STATS_MEMORY_NULL;
import static org.onosproject.drivers.server.Constants.MSG_STATS_NIC_NULL;
import static org.onosproject.drivers.server.Constants.MSG_STATS_TIMING_NULL;

/**
 * Default monitoring statistics for server devices.
 * Includes CPU, main memory, NIC, and timing statistics.
 */
public final class DefaultMonitoringStatistics implements MonitoringStatistics {

    private final DeviceId deviceId;

    private final TimingStatistics           timingStatistics;
    private final Collection<CpuStatistics>  cpuStatistics;
    private final MemoryStatistics           memoryStatistics;
    private final Collection<PortStatistics> nicStatistics;

    private DefaultMonitoringStatistics(
            DeviceId                   deviceId,
            TimingStatistics           timingStatistics,
            Collection<CpuStatistics>  cpuStatistics,
            MemoryStatistics           memoryStatistics,
            Collection<PortStatistics> nicStatistics) {
        checkNotNull(deviceId,         MSG_DEVICE_ID_NULL);
        checkNotNull(timingStatistics, MSG_STATS_TIMING_NULL);
        checkNotNull(cpuStatistics,    MSG_STATS_CPU_NULL);
        checkNotNull(memoryStatistics, MSG_STATS_MEMORY_NULL);
        checkNotNull(nicStatistics,    MSG_STATS_NIC_NULL);

        this.deviceId = deviceId;
        this.timingStatistics = timingStatistics;
        this.cpuStatistics = cpuStatistics;
        this.memoryStatistics = memoryStatistics;
        this.nicStatistics = nicStatistics;
    }

    // Constructor for serializer
    private DefaultMonitoringStatistics() {
        this.deviceId = null;
        this.timingStatistics = null;
        this.cpuStatistics = null;
        this.memoryStatistics = null;
        this.nicStatistics = null;
    }

    /**
     * Creates a builder for DefaultMonitoringStatistics object.
     *
     * @return builder object for DefaultMonitoringStatistics object
     */
    public static DefaultMonitoringStatistics.Builder builder() {
        return new Builder();
    }

    @Override
    public TimingStatistics timingStatistics() {
        return this.timingStatistics;
    }

    @Override
    public Collection<CpuStatistics> cpuStatisticsAll() {
        return this.cpuStatistics;
    }

    @Override
    public CpuStatistics cpuStatistics(int cpuId) {
        checkArgument((cpuId >= 0) && (cpuId < CpuCoreId.MAX_CPU_CORE_NB),
            MSG_CPU_CORE_NEGATIVE);
        for (CpuStatistics cs : this.cpuStatistics) {
            if (cs.id() == cpuId) {
                return cs;
            }
        }
        return null;
    }

    @Override
    public MemoryStatistics memoryStatistics() {
        return this.memoryStatistics;
    }

    @Override
    public Collection<PortStatistics> nicStatisticsAll() {
        return this.nicStatistics;
    }

    @Override
    public PortStatistics nicStatistics(int nicId) {
        checkArgument(nicId >= 0, "NIC ID must be a non-negative integer");
        for (PortStatistics ns : this.nicStatistics) {
            if (ns.portNumber().toLong() == nicId) {
                return ns;
            }
        }
        return null;
    }

    @Override
    public int numberOfNics() {
        return this.nicStatistics.size();
    }

    @Override
    public int numberOfCpus() {
        return this.cpuStatistics.size();
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .omitNullValues()
                .add("timingStatistics", timingStatistics())
                .add("cpuStatistics",    cpuStatisticsAll())
                .add("memoryStatistics", memoryStatistics())
                .add("nicStatistics",    nicStatisticsAll())
                .toString();
    }

    public static final class Builder {

        DeviceId                   deviceId;
        TimingStatistics           timingStatistics;
        Collection<CpuStatistics>  cpuStatistics;
        MemoryStatistics           memoryStatistics;
        Collection<PortStatistics> nicStatistics;

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
         * Sets timing statistics.
         *
         * @param timingStatistics timing statistics
         * @return builder object
         */
        public Builder setTimingStatistics(TimingStatistics timingStatistics) {
            this.timingStatistics = timingStatistics;

            return this;
        }

        /**
         * Sets CPU statistics.
         *
         * @param cpuStatistics CPU statistics
         * @return builder object
         */
        public Builder setCpuStatistics(Collection<CpuStatistics> cpuStatistics) {
            this.cpuStatistics = cpuStatistics;

            return this;
        }

        /**
         * Sets memory statistics.
         *
         * @param memoryStatistics memory statistics
         * @return builder object
         */
        public Builder setMemoryStatistics(MemoryStatistics memoryStatistics) {
            this.memoryStatistics = memoryStatistics;

            return this;
        }

        /**
         * Sets NIC statistics.
         *
         * @param nicStatistics NIC statistics
         * @return builder object
         */
        public Builder setNicStatistics(Collection<PortStatistics> nicStatistics) {
            this.nicStatistics = nicStatistics;

            return this;
        }

        /**
         * Creates a MonitoringStatistics object.
         *
         * @return DefaultMonitoringStatistics object
         */
        public DefaultMonitoringStatistics build() {
            return new DefaultMonitoringStatistics(
                deviceId, timingStatistics, cpuStatistics,
                memoryStatistics, nicStatistics);
        }

    }

}

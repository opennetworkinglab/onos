/*
 * Copyright 2020-present Open Networking Foundation
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

import org.onosproject.drivers.server.stats.MemoryStatistics;
import org.onosproject.drivers.server.stats.MonitoringUnit;

import org.onosproject.net.DeviceId;

import com.google.common.base.MoreObjects;
import com.google.common.base.Strings;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static org.onosproject.drivers.server.Constants.MSG_STATS_MEMORY_FREE_NEGATIVE;
import static org.onosproject.drivers.server.Constants.MSG_STATS_MEMORY_TOTAL_NEGATIVE;
import static org.onosproject.drivers.server.Constants.MSG_STATS_MEMORY_USED_NEGATIVE;
import static org.onosproject.drivers.server.Constants.MSG_STATS_UNIT_NULL;
import static org.onosproject.drivers.server.stats.MonitoringUnit.CapacityUnit;

/**
 * Default implementation for main memory statistics.
 */
public final class DefaultMemoryStatistics implements MemoryStatistics {

    private static final CapacityUnit DEF_MEM_UNIT = CapacityUnit.KILOBYTES;

    private final DeviceId deviceId;
    private final MonitoringUnit unit;
    private long used;
    private long free;
    private long total;

    private DefaultMemoryStatistics(DeviceId deviceId, MonitoringUnit unit,
                                    long used, long free, long total) {
        checkNotNull(unit, MSG_STATS_UNIT_NULL);
        checkArgument(used >= 0, MSG_STATS_MEMORY_USED_NEGATIVE);
        checkArgument(free >= 0, MSG_STATS_MEMORY_FREE_NEGATIVE);
        checkArgument((total >= 0) &&
                      (used + free == total), MSG_STATS_MEMORY_TOTAL_NEGATIVE);

        this.deviceId = deviceId;
        this.unit = unit;
        this.used = used;
        this.free = free;
        this.total = total;
    }

    // Constructor for serializer
    private DefaultMemoryStatistics() {
        this.deviceId = null;
        this.unit = null;
        this.used = 0;
        this.free = 0;
        this.total = 0;
    }

    /**
     * Creates a builder for DefaultMemoryStatistics object.
     *
     * @return builder object for DefaultMemoryStatistics object
     */
    public static DefaultMemoryStatistics.Builder builder() {
        return new Builder();
    }

    @Override
    public MonitoringUnit unit() {
        return this.unit;
    }

    @Override
    public long used() {
        return this.used;
    }

    @Override
    public long free() {
        return this.free;
    }

    @Override
    public long total() {
        return this.total;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .omitNullValues()
                .add("device", deviceId)
                .add("unit", this.unit())
                .add("used", this.used())
                .add("free", this.free())
                .add("total", this.total())
                .toString();
    }

    public static final class Builder {

        DeviceId deviceId;
        MonitoringUnit unit = DEF_MEM_UNIT;
        long used;
        long free;
        long total;

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
         * Sets memory statistics unit.
         *
         * @param unitStr memory statistics unit as a string
         * @return builder object
         */
        public Builder setUnit(String unitStr) {
            if (!Strings.isNullOrEmpty(unitStr)) {
                this.unit = CapacityUnit.getByName(unitStr);
            }

            return this;
        }

        /**
         * Sets the amount of used main memory.
         *
         * @param used used main memory
         * @return builder object
         */
        public Builder setMemoryUsed(long used) {
            this.used = used;
            return this;
        }

        /**
         * Sets the amount of free main memory.
         *
         * @param free free main memory
         * @return builder object
         */
        public Builder setMemoryFree(long free) {
            this.free = free;
            return this;
        }

        /**
         * Sets the total amount of main memory.
         *
         * @param total total main memory
         * @return builder object
         */
        public Builder setMemoryTotal(long total) {
            this.total = total;
            return this;
        }

        /**
         * Creates a DefaultMemoryStatistics object.
         *
         * @return DefaultMemoryStatistics object
         */
        public DefaultMemoryStatistics build() {
            return new DefaultMemoryStatistics(
                deviceId, unit, used, free, total);
        }

    }

}

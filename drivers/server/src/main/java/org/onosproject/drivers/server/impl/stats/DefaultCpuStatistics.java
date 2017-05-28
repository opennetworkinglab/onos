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

import org.onosproject.drivers.server.stats.CpuStatistics;

import org.onosproject.net.DeviceId;
import com.google.common.base.MoreObjects;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkArgument;

/**
 * Default implementation for CPU statistics.
 */
public final class DefaultCpuStatistics implements CpuStatistics {

    private static final float MIN_CPU_LOAD = (float) 0.0;
    private static final float MAX_CPU_LOAD = (float) 1.0;

    // Upper limit of CPU cores in one machine
    public static final int MAX_CPU_NB = 512;

    private final DeviceId deviceId;

    private final int id;
    private final float load;
    private final boolean isBusy;

    private DefaultCpuStatistics(
            DeviceId deviceId,
            int      id,
            float    load,
            boolean  isBusy) {
        checkNotNull(deviceId, "Device ID is NULL");
        checkArgument(
            (id >= 0) && (id < MAX_CPU_NB),
            "CPU core ID must be in [0, " + String.valueOf(MAX_CPU_NB - 1) + "]"
        );
        checkArgument(
            (load >= MIN_CPU_LOAD) && (load <= MAX_CPU_LOAD),
            "CPU load must be in [" + MIN_CPU_LOAD + ", " + MAX_CPU_LOAD + "]"
        );

        this.deviceId = deviceId;
        this.id       = id;
        this.load     = load;
        this.isBusy   = isBusy;
    }

    // Constructor for serializer
    private DefaultCpuStatistics() {
        this.deviceId = null;
        this.id       = 0;
        this.load     = 0;
        this.isBusy   = false;
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
    public boolean busy() {
        return this.isBusy;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .omitNullValues()
                .add("device", deviceId)
                .add("id",     id())
                .add("load",   load())
                .add("isBusy", busy())
                .toString();
    }

    public static final class Builder {

        DeviceId deviceId;
        int      id;
        float    load;
        boolean  isBusy;

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
         * @param id the CPU ID
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
         * Sets the CPU status (free or busy).
         *
         * @param isBusy CPU status
         * @return builder object
         */
        public Builder setIsBusy(boolean isBusy) {
            this.isBusy = isBusy;

            return this;
        }

        /**
         * Creates a DefaultCpuStatistics object.
         *
         * @return DefaultCpuStatistics object
         */
        public DefaultCpuStatistics build() {
            return new DefaultCpuStatistics(
                deviceId,
                id,
                load,
                isBusy
            );
        }
    }

}

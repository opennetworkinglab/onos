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

package org.onosproject.drivers.server.impl.devices;

import org.onosproject.drivers.server.devices.CpuDevice;
import org.onosproject.drivers.server.devices.CpuVendor;

import org.onosproject.drivers.server.impl.stats.DefaultCpuStatistics;

import com.google.common.base.MoreObjects;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkArgument;

import java.util.Objects;

/**
 * Default implementation for CPU core devices.
 */
public class DefaultCpuDevice implements CpuDevice {

    private final int       id;
    private final CpuVendor vendor;
    private final long      frequency;

    // Maximum CPU core frequency in MHz
    public static final long MAX_FREQUENCY_MHZ = 4500;

    public DefaultCpuDevice(int id, CpuVendor vendor, long frequency) {
        checkArgument(
            (id >= 0) && (id < DefaultCpuStatistics.MAX_CPU_NB),
            "CPU core ID must be in [0, " +
            String.valueOf(DefaultCpuStatistics.MAX_CPU_NB - 1) + "]"
        );
        checkNotNull(
            vendor,
            "CPU core vendor cannot be null"
        );
        checkArgument(
            (frequency > 0) && (frequency <= MAX_FREQUENCY_MHZ),
            "CPU core frequency (MHz) must be positive and less or equal than " +
            MAX_FREQUENCY_MHZ + " MHz"
        );

        this.id        = id;
        this.vendor    = vendor;
        this.frequency = frequency;
    }

    @Override
    public int id() {
        return this.id;
    }

    @Override
    public CpuVendor vendor() {
        return this.vendor;
    }

    @Override
    public long frequency() {
        return this.frequency;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .omitNullValues()
                .add("id",        id())
                .add("vendor",    vendor())
                .add("frequency", frequency())
                .toString();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof CpuDevice)) {
            return false;
        }
        CpuDevice device = (CpuDevice) obj;
        return  this.id() ==  device.id() &&
                this.vendor() == device.vendor() &&
                this.frequency() == device.frequency();
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, vendor, frequency);
    }

}

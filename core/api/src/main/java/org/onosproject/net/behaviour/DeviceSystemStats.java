/*
 * Copyright 2016-present Open Networking Foundation
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
package org.onosproject.net.behaviour;

import com.google.common.base.MoreObjects;

import java.util.Objects;

/**
 * A representation of system stats of device.
 */
public class DeviceSystemStats {

    private final DeviceMemoryStats memory;
    private final DeviceCpuStats cpu;

    /**
     * Creates deviceSystemStats object.
     *
     * @param memoryStats memory statisics of the device
     * @param cpuStats cpu statistics of the device
     */
    public DeviceSystemStats(DeviceMemoryStats memoryStats, DeviceCpuStats cpuStats) {
        this.memory = memoryStats;
        this.cpu = cpuStats;
    }

    /**
     * Get memory usage statistics.
     *
     * @return deviceMemoryStats, device memory usage stats in KB
     */
    public DeviceMemoryStats getMemory() {
        return this.memory;
    }

    /**
     * Get cpu usage statistics.
     *
     * @return deviceCpuStats, device cpu usage stats
     */
    public DeviceCpuStats getCpu() {
        return this.cpu;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("memory", memory)
                .add("cpu", cpu)
                .toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        DeviceSystemStats that = (DeviceSystemStats) o;
        return Objects.equals(memory, that.memory) &&
                Objects.equals(cpu, that.cpu);
    }

    @Override
    public int hashCode() {
        return Objects.hash(memory, cpu);
    }
}



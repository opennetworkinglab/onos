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
 * A representation of memory stats of device.
 */

public class DeviceMemoryStats {
    private long free = 0;  // in Bytes
    private long used = 0;
    private long total = 0;

    /**
     * Instantiates DeviceMemoryStats object.
     */
    public DeviceMemoryStats() {
    }

    /**
     * Creates DeviceMemoryStats object with given data.
     *
     * @param free free memory
     * @param used used memory
     * @param total total memory
     */
    public DeviceMemoryStats(long free, long used, long total) {
        this.free = free;
        this.used = used;
        this.total = total;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("free", free)
                .add("used", used)
                .add("total", total)
                .toString();
    }

    /**
     * Get total memory of device.
     *
     * @return totalMmeory, total memory
     */
    public long getTotal() {
        return total;
    }

    /**
     * Get used memory of device.
     *
     * @return usedMemory, used memory
     */
    public long getUsed() {
        return used;
    }

    /**
     * Get free memory of device.
     *
     * @return freeMemory, free memory
     */
    public long getFree() {
        return free;
    }

    /**
     * Set free memory of device.
     *
     * @param free free memory stats of device
     */
    public void setFree(long free) {
        this.free = free;
    }

    /**
     * Set used memory of device.
     *
     * @param used used memory stats of device
     */
    public void setUsed(long used) {
        this.used = used;
    }

    /**
     * Set total memory of device.
     *
     * @param total total memory stats of device
     */
    public void setTotal(long total) {
        this.total = total;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        DeviceMemoryStats that = (DeviceMemoryStats) o;
        return free == that.free &&
                used == that.used &&
                total == that.total;
    }

    @Override
    public int hashCode() {
        return Objects.hash(free, used, total);
    }
}


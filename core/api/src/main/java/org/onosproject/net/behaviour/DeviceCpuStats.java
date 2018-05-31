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
 * A representation of cpu stats of device.
 */
public class DeviceCpuStats {
    private float used;

    /**
     * Instantiates DeviceCpuStats object with default value.
     */
    public DeviceCpuStats() {
        used = 0.0f;
    }

    /**
     * Creates DeviceCpuStats object with given value.
     *
     * @param used cpu usage of device
     */
    public DeviceCpuStats(float used) {
        this.used = used;
    }

    /**
     * Get cpu usage of device.
     *
     * @return usedCpu, cpu usage stats of device
     */
    public float getUsed() {
        return used;
    }

    /**
     * Set cpu usage of device.
     *
     * @param used cpu usage of device
     */
    public void setUsed(float used) {
        this.used = used;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("used", used)
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
        DeviceCpuStats that = (DeviceCpuStats) o;
        return Float.compare(that.used, used) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(used);
    }
}

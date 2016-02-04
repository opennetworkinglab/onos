/*
 * Copyright 2016 Open Networking Laboratory
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
package org.onosproject.cpman.impl;

import org.onosproject.cpman.SystemInfo;

/**
 * Implementation class of storing system specification.
 */
public final class DefaultSystemInfo implements SystemInfo {
    private int numOfCores;
    private int numOfCpus;
    private int cpuSpeedMhz;
    private int totalMemoryMbytes;

    private DefaultSystemInfo(int numOfCores, int numOfCpus,
                              int cpuSpeedMhz, int totalMemoryMbytes) {
        this.numOfCores = numOfCores;
        this.numOfCpus = numOfCpus;
        this.cpuSpeedMhz = cpuSpeedMhz;
        this.totalMemoryMbytes = totalMemoryMbytes;
    }

    @Override
    public int coreCount() {
        return this.numOfCores;
    }

    @Override
    public int cpuCount() {
        return this.numOfCpus;
    }

    @Override
    public int cpuSpeed() {
        return this.cpuSpeedMhz;
    }

    @Override
    public int totalMemory() {
        return this.totalMemoryMbytes;
    }

    /**
     * ControlMetricsSystemSpec builder class.
     */
    public static final class Builder implements SystemInfo.Builder {
        private int numOfCores;
        private int numOfCpus;
        private int cpuSpeedMHz;
        private int totalMemoryBytes;

        @Override
        public SystemInfo.Builder numOfCores(int numOfCores) {
            this.numOfCores = numOfCores;
            return this;
        }

        @Override
        public Builder numOfCpus(int numOfCpus) {
            this.numOfCpus = numOfCpus;
            return this;
        }

        @Override
        public Builder cpuSpeed(int cpuSpeedMhz) {
            this.cpuSpeedMHz = cpuSpeedMhz;
            return this;
        }

        @Override
        public Builder totalMemory(int totalMemoryBytes) {
            this.totalMemoryBytes = totalMemoryBytes;
            return this;
        }

        @Override
        public DefaultSystemInfo build() {
            return new DefaultSystemInfo(numOfCores, numOfCpus, cpuSpeedMHz, totalMemoryBytes);
        }
    }
}

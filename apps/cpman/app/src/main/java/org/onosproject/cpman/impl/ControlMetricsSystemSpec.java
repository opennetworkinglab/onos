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

/**
 * Control metrics class for storing system specification.
 */
public final class ControlMetricsSystemSpec {
    private int numOfCores;
    private int numOfCpus;
    private int cpuSpeed;               // in MHz
    private long totalMemory;           // in bytes

    private ControlMetricsSystemSpec(int numOfCores, int numOfCpus,
                                     int cpuSpeed, long totalMemory) {
        this.numOfCores = numOfCores;
        this.numOfCpus = numOfCpus;
        this.cpuSpeed = cpuSpeed;
        this.totalMemory = totalMemory;
    }

    /**
     * Returns number of CPU cores.
     *
     * @return number of CPU cores
     */
    public int numOfCores() {
        return this.numOfCores;
    }

    /**
     * Returns number of CPUs.
     *
     * @return number of CPUs
     */
    public int numOfCpus() {
        return this.numOfCpus;
    }

    /**
     * Returns CPU speed in MHz.
     *
     * @return CPU speed
     */
    public int cpuSpeed() {
        return this.cpuSpeed;
    }

    /**
     * Returns the total amount of memory.
     *
     * @return memory size
     */
    public long totalMemory() {
        return this.totalMemory;
    }

    /**
     * ControlMetricsSystemSpec builder class.
     */
    public static final class Builder {
        private int numOfCores;
        private int numOfCpus;
        private int cpuSpeed;               // in MHz
        private long totalMemory;           // in bytes

        /**
         * Sets number of CPU cores.
         *
         * @param numOfCores number of CPU cores
         * @return Builder object
         */
        public Builder numOfCores(int numOfCores) {
            this.numOfCores = numOfCores;
            return this;
        }

        /**
         * Sets number of CPUs.
         * @param numOfCpus number of CPUs
         * @return Builder object
         */
        public Builder numOfCpus(int numOfCpus) {
            this.numOfCpus = numOfCpus;
            return this;
        }

        /**
         * Sets CPU speed.
         *
         * @param cpuSpeed CPU speed
         * @return Builder object
         */
        public Builder cpuSpeed(int cpuSpeed) {
            this.cpuSpeed = cpuSpeed;
            return this;
        }

        /**
         * Sets total amount of memory.
         *
         * @param totalMemory memory size
         * @return Builder object
         */
        public Builder totalMemory(long totalMemory) {
            this.totalMemory = totalMemory;
            return this;
        }

        /**
         * Builds a ControlMetricsSystemSpec object.
         *
         * @return ControlMetricsSystemSpec object
         */
        public ControlMetricsSystemSpec build() {
            return new ControlMetricsSystemSpec(numOfCores, numOfCpus, cpuSpeed, totalMemory);
        }
    }
}

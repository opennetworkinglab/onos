/*
 * Copyright 2016-present Open Networking Laboratory
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
package org.onosproject.cpman;

/**
 * System information interface.
 */
public interface SystemInfo {

    /**
     * Returns number of CPU cores.
     *
     * @return number of CPU cores
     */
    int coreCount();

    /**
     * Returns number of CPUs.
     *
     * @return number of CPUs
     */
    int cpuCount();

    /**
     * Returns CPU speed in MHz.
     *
     * @return CPU speed
     */
    int cpuSpeed();

    /**
     * Returns the total amount of memory in Mega Bytes.
     *
     * @return memory size
     */
    int totalMemory();

    /**
     * A builder of SystemInfo.
     */
    interface Builder {

        /**
         * Sets number of CPU cores.
         *
         * @param numOfCores number of CPU cores
         * @return Builder object
         */
        Builder numOfCores(int numOfCores);

        /**
         * Sets number of CPUs.
         * @param numOfCpus number of CPUs
         * @return Builder object
         */
        Builder numOfCpus(int numOfCpus);

        /**
         * Sets CPU speed.
         *
         * @param cpuSpeedMhz CPU speed in Mhz
         * @return Builder object
         */
        Builder cpuSpeed(int cpuSpeedMhz);

        /**
         * Sets total amount of memory.
         *
         * @param totalMemoryMbytes memory size in Mega Bytes
         * @return Builder object
         */
        Builder totalMemory(int totalMemoryMbytes);

        /**
         * Builds a SystemInfo object.
         *
         * @return SystemInfo object
         */
        SystemInfo build();
    }
}

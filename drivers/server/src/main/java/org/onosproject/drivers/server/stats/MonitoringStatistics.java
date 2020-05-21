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

package org.onosproject.drivers.server.stats;

import org.onosproject.net.device.PortStatistics;

import java.util.Collection;

/**
 * Server statistics API.
 */
public interface MonitoringStatistics {

    /**
     * Returns timing statistics related to
     * tasks requested by the controller.
     *
     * @return timing statistics
     */
    TimingStatistics timingStatistics();

    /**
     * Returns the CPU statistics of a server device.
     * Includes the statistics of all CPUs.
     *
     * @return CPU statistics
     */
    Collection<CpuStatistics> cpuStatisticsAll();

    /**
     * Returns the statistics of a particular CPU
     * of a server device.
     *
     * @param cpuId ID of the CPU
     * @return CpuStatistics object for this CPU
     */
    CpuStatistics cpuStatistics(int cpuId);

    /**
     * Returns main memory statistics of a server device.
     *
     * @return main memory statistics
     */
    MemoryStatistics memoryStatistics();

    /**
     * Returns the NIC statistics of a server device.
     * Includes the statistics of all NICs.
     *
     * @return set of PortStatistics
     */
    Collection<PortStatistics> nicStatisticsAll();

    /**
     * Returns the statistics of a particular NIC
     * of a server device.
     *
     * @param nicId ID of the NIC
     * @return PortStatistics object for this NIC
     */
    PortStatistics nicStatistics(int nicId);

    /**
     * Returns the number of CPUs being monitored.
     *
     * @return number of CPUs
     */
    int numberOfCpus();

    /**
     * Returns the number of NICs being monitored.
     *
     * @return number of NICs
     */
    int numberOfNics();

}

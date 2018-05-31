/*
 * Copyright 2015-present Open Networking Foundation
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
package org.onosproject.ovsdb.controller.driver;


import org.onosproject.net.behaviour.DeviceCpuStats;
import org.onosproject.net.behaviour.DeviceMemoryStats;
import org.onosproject.ovsdb.controller.OvsdbClientService;
import org.onosproject.ovsdb.rfc.table.CpuMemoryData;
import org.onosproject.ovsdb.rfc.table.OvsdbTable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

import static org.onosproject.ovsdb.controller.OvsdbConstant.*;
import static org.onosproject.ovsdb.controller.OvsdbConstant.SWINVENTORY_DBNAME;

/**
 * A representation of an ovsdb client for Pica device.
 */
public final class PicaOvsdbClient {

    private final Logger log = LoggerFactory.getLogger(DefaultOvsdbClient.class);

    private OvsdbClientService ovsdbClientService;

    /**
     * Creates an OvsdbClient for pica device.
     *
     * @param ovsdbClient default ovsdb client
     */
    public PicaOvsdbClient(OvsdbClientService ovsdbClient) {
        this.ovsdbClientService = ovsdbClient;
    }

    /**
     * Get memory usage of pica device.
     *
     * @return memoryStats, memory usage statistics if present
     */
    public Optional<DeviceMemoryStats> getDeviceMemoryUsage() {

        Optional<Object> deviceMemoryDataObject = ovsdbClientService.getFirstRow(
                SWINVENTORY_DBNAME, OvsdbTable.CPUMEMORYDATA);

        if (!deviceMemoryDataObject.isPresent()) {
            log.debug("Could not find {} column in {} table", DEVICE_MEMORY, CPU_MEMORY_DATA);
            return Optional.empty();
        }
        CpuMemoryData deviceMemoryData = (CpuMemoryData) deviceMemoryDataObject.get();

        long totalMem = deviceMemoryData.getTotalMemoryStats();
        long usedMem = deviceMemoryData.getUsedMemoryStats();
        long freeMem = deviceMemoryData.getFreeMemoryStats();

        DeviceMemoryStats deviceMemoryStats = new DeviceMemoryStats();
        deviceMemoryStats.setFree(freeMem);
        deviceMemoryStats.setUsed(usedMem);
        deviceMemoryStats.setTotal(totalMem);

        return Optional.of(deviceMemoryStats);
    }

    /**
     * Get cpu usage of pica device.
     *
     * @return cpuStats, cpu usage statistics if present
     */
    public Optional<DeviceCpuStats> getDeviceCpuUsage() {

        Optional<Object> deviceCpuDataObject = ovsdbClientService.getFirstRow(
                SWINVENTORY_DBNAME, OvsdbTable.CPUMEMORYDATA);

        if (!deviceCpuDataObject.isPresent()) {
            log.debug("Could not find {} column in {} table", DEVICE_CPU, CPU_MEMORY_DATA);
            return Optional.empty();
        }
        CpuMemoryData deviceCpuData = (CpuMemoryData) deviceCpuDataObject.get();

        log.debug("GOT CpuMemoryData as {} ", deviceCpuData);

        float freeCpuStat = deviceCpuData.getFreeCpuStats();
        DeviceCpuStats deviceCpuStats = new DeviceCpuStats();
        deviceCpuStats.setUsed(100.0f - freeCpuStat);
        return Optional.of(deviceCpuStats);
    }
}

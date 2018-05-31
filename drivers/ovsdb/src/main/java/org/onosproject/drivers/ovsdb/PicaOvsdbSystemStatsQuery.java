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

package org.onosproject.drivers.ovsdb;


import org.onlab.packet.IpAddress;
import org.onosproject.net.DeviceId;
import org.onosproject.net.behaviour.DeviceCpuStats;
import org.onosproject.net.behaviour.DeviceMemoryStats;
import org.onosproject.net.behaviour.DeviceSystemStatisticsQuery;
import org.onosproject.net.behaviour.DeviceSystemStats;
import org.onosproject.net.driver.AbstractHandlerBehaviour;
import org.onosproject.net.driver.DriverHandler;
import org.onosproject.ovsdb.controller.OvsdbClientService;
import org.onosproject.ovsdb.controller.OvsdbController;
import org.onosproject.ovsdb.controller.OvsdbNodeId;
import org.onosproject.ovsdb.controller.driver.PicaOvsdbClient;

import java.util.Optional;

/**
 * The implementation of PicaOvsdbSystemStatsQuery.
 */
public class PicaOvsdbSystemStatsQuery extends AbstractHandlerBehaviour
        implements DeviceSystemStatisticsQuery {

    // OvsdbNodeId(IP) is used in the adaptor while DeviceId(ovsdb:IP)
    // is used in the core. So DeviceId need be changed to OvsdbNodeId.
    private OvsdbNodeId changeDeviceIdToNodeId(DeviceId deviceId) {
        String[] splits = deviceId.toString().split(":");
        if (splits == null || splits.length < 1) {
            return null;
        }
        IpAddress ipAddress = IpAddress.valueOf(splits[1]);
        return new OvsdbNodeId(ipAddress, 0);
    }

    // Used for getting OvsdbClientService.
    private OvsdbClientService getOvsdbClientService(DriverHandler handler) {
        OvsdbController ovsController = handler.get(OvsdbController.class);
        DeviceId deviceId = handler.data().deviceId();
        OvsdbNodeId nodeId = changeDeviceIdToNodeId(deviceId);
        return ovsController.getOvsdbClient(nodeId);
    }

    /**
     * Get cpu usage of device.
     *
     * @return cpuStats, device cpu usage stats if available
     */
    private Optional<DeviceCpuStats> getCpuUsage() {
        OvsdbClientService client = getOvsdbClientService(handler());
        if (client == null) {
            return Optional.empty();
        }
        PicaOvsdbClient picaOvsdbClient = new PicaOvsdbClient(client);
        return picaOvsdbClient.getDeviceCpuUsage();
    }

    /**
     * Get memory usage of device in KB.
     *
     * @return memoryStats, device memory usage stats if available
     */
    private Optional<DeviceMemoryStats> getMemoryUsage() {
        OvsdbClientService client = getOvsdbClientService(handler());
        if (client == null) {
            return Optional.empty();
        }
        PicaOvsdbClient picaOvsdbClient = new PicaOvsdbClient(client);
        return picaOvsdbClient.getDeviceMemoryUsage();
    }

    /**
     * Get system stats (cpu/mmeory usage) of device.
     *
     * @return deviceSystemStats, system stats (cpu/memory usage) of device if available
     */
    @Override
    public Optional<DeviceSystemStats> getDeviceSystemStats() {
        Optional<DeviceCpuStats> cpuStats = getCpuUsage();
        Optional<DeviceMemoryStats> memoryStats = getMemoryUsage();

        if (cpuStats.isPresent() && memoryStats.isPresent()) {
            DeviceSystemStats systemStats = new DeviceSystemStats(memoryStats.get(), cpuStats.get());
            return Optional.of(systemStats);
        } else {
            return Optional.empty();
        }
    }
}

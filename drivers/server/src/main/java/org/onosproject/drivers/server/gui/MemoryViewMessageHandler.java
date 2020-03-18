/*
 * Copyright 2020-present Open Networking Foundation
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

package org.onosproject.drivers.server.gui;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.primitives.Floats;

import org.onosproject.drivers.server.behavior.MonitoringStatisticsDiscovery;
import org.onosproject.drivers.server.devices.RestServerSBDevice;
import org.onosproject.drivers.server.stats.MemoryStatistics;
import org.onosproject.drivers.server.stats.MonitoringStatistics;
import org.onosproject.drivers.server.stats.MonitoringUnit.CapacityUnit;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.device.DeviceService;
import org.onosproject.ui.RequestHandler;
import org.onosproject.ui.chart.ChartModel;

import org.apache.commons.lang3.ArrayUtils;
import org.joda.time.LocalDateTime;
import org.slf4j.Logger;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.onosproject.drivers.server.Constants.MSG_UI_DATA_MEMORY_NULL;
import static org.onosproject.drivers.server.gui.MetricType.MEMORY;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Message handler for passing memory data to the Web UI.
 */
public class MemoryViewMessageHandler extends BaseViewMessageHandler {

    private static final Logger log = getLogger(MemoryViewMessageHandler.class);

    private static final String MEMORY_DATA_REQ = "memoryDataRequest";
    private static final String MEMORY_DATA_RESP = "memoryDataResponse";
    private static final String MEMORY_LABEL = "memorys";

    @Override
    protected Collection<RequestHandler> createRequestHandlers() {
        return ImmutableSet.of(new MemoryMessageRequest());
    }

    private final class MemoryMessageRequest
        extends BaseViewMessageHandler.ControlMessageRequest {

        // Memory UI has 3 columns (used, free, and total memory)
        public static final int MEM_COLUMNS_NB = 3;

        private MemoryMessageRequest() {
            super(MEMORY_DATA_REQ, MEMORY_DATA_RESP, MEMORY_LABEL);
        }

        @Override
        protected String[] getSeries() {
            return createMappedSeries(MEMORY, MEM_SUBMETRIC_MAP, MEM_COLUMNS_NB);
        }

        @Override
        protected void populateChart(ChartModel cm, ObjectNode payload) {
            DeviceService ds = get(DeviceService.class);
            if ((ds == null) || (ds.getAvailableDeviceCount() == 0)) {
                fillDataWhenNoDevicePresent(cm, MEMORY, MEM_COLUMNS_NB);
                return;
            }

            String uri = string(payload, "devId");

            // Project only one device over time
            if (!Strings.isNullOrEmpty(uri)) {
                DeviceId deviceId = DeviceId.deviceId(uri);
                RestServerSBDevice serverDev =
                    (RestServerSBDevice) basicDriver.getController().getDevice(deviceId);

                Map<Integer, Float[]> data = null;
                MonitoringStatistics monStats = serverDriver.getGlobalMonitoringStatistics(deviceId);
                if (monStats == null) {
                    data = populateZeroMapDataHistory(deviceId, MEM_COLUMNS_NB);
                } else {
                    data = populateMemoryDataHistory(deviceId, MEM_COLUMNS_NB, monStats);
                }
                checkNotNull(data, MSG_UI_DATA_MEMORY_NULL);

                // Generate a timestamp
                LocalDateTime ldt = new LocalDateTime(timestamp);

                // Project the data
                populateMapMetrics(cm, data, ldt, MEMORY, NUM_OF_DATA_POINTS);

                Set<DeviceId> deviceIds = Sets.newHashSet();
                for (Device device : ds.getAvailableDevices()) {
                    // Only devices that support this type of monitoring behaviour are considered
                    if (device.is(MonitoringStatisticsDiscovery.class) && serverDev.isActive()) {
                        deviceIds.add(device.id());
                    }
                }

                // Drop down list to select devices
                attachDeviceList(cm, deviceIds);
            } else {
                for (Device device : ds.getAvailableDevices()) {
                    // Only devices that support this type of monitoring behaviour are considered
                    if (!device.is(MonitoringStatisticsDiscovery.class)) {
                        continue;
                    }

                    DeviceId deviceId = device.id();
                    RestServerSBDevice serverDev =
                        (RestServerSBDevice) basicDriver.getController().getDevice(deviceId);

                    Map<Integer, Float> data = null;
                    MonitoringStatistics monStats = serverDriver.getGlobalMonitoringStatistics(deviceId);
                    if (monStats == null) {
                        data = populateZeroMapData(deviceId, MEM_COLUMNS_NB);
                    } else {
                        data = populateMemoryData(deviceId, MEM_COLUMNS_NB, monStats);
                    }
                    checkNotNull(data, MSG_UI_DATA_MEMORY_NULL);

                    // Map them to the memory submetrics
                    Map<String, Object> local = Maps.newHashMap();
                    for (int i = 0; i < data.size(); i++) {
                        local.put(getMappedLabel(MEMORY, MEM_SUBMETRIC_MAP, i), data.get(i));
                    }

                    // Last piece of data is the device ID
                    if (serverDev.isActive()) {
                        local.put(LABEL, deviceId);
                        populateMetric(cm.addDataPoint(deviceId), local);
                    } else {
                        local.put(LABEL, "");
                        populateMetric(cm.addDataPoint(""), local);
                    }
                }
            }
        }

        /**
         * Turn the current monitoring data into a data
         * structure that can feed the Memory UI memory.
         *
         * @param deviceId the device ID being monitored
         * @param length the length of the array
         * @param monStats a MonitoringStatistics object
         * @return a map of memory metrics to their values
         */
        private Map<Integer, Float> populateMemoryData(
                DeviceId deviceId, int length, MonitoringStatistics monStats) {
            Map<Integer, Float> data = initializeMapData(MEM_COLUMNS_NB);

            MemoryStatistics memStats = monStats.memoryStatistics();

            CapacityUnit capacityUnit = (CapacityUnit) memStats.unit();
            Float used = new Float(memStats.used());
            Float free = new Float(memStats.free());
            Float total = new Float(memStats.total());

            // Unit conversions
            used = CapacityUnit.toGigaBytes(used, capacityUnit);
            free = CapacityUnit.toGigaBytes(free, capacityUnit);
            total = CapacityUnit.toGigaBytes(total, capacityUnit);

            // Store them locally
            addToCache(deviceId, length, 0, used);
            addToCache(deviceId, length, 1, free);
            addToCache(deviceId, length, 2, total);

            // And into the map
            data.put(0, used);
            data.put(1, free);
            data.put(2, total);

            return data;
        }

        /**
         * Turn the monitoring data history into a
         * data structure that can feed the Memory UI memory.
         *
         * @param deviceId the device ID being monitored
         * @param length the length of the array
         * @param monStats a MonitoringStatistics object
         * @return a map of memory metrics to their arrays of values
         */
        private Map<Integer, Float[]> populateMemoryDataHistory(
                DeviceId deviceId, int length, MonitoringStatistics monStats) {
            Map<Integer, Float[]> data = initializeMapDataHistory(MEM_COLUMNS_NB);

            MemoryStatistics memStats = monStats.memoryStatistics();

            CapacityUnit capacityUnit = (CapacityUnit) memStats.unit();
            Float used = new Float(memStats.used());
            Float free = new Float(memStats.free());
            Float total = new Float(memStats.total());

            // Unit conversions
            used = CapacityUnit.toGigaBytes(used, capacityUnit);
            free = CapacityUnit.toGigaBytes(free, capacityUnit);
            total = CapacityUnit.toGigaBytes(total, capacityUnit);

            // Store them locally
            addToCache(deviceId, length, 0, used);
            addToCache(deviceId, length, 1, free);
            addToCache(deviceId, length, 2, total);

            for (int i = 0; i < length; i++) {
                LruCache<Float> loadCache = getDataHistory(deviceId, i);
                if (loadCache == null) {
                    continue;
                }
                float[] floatArray = Floats.toArray(Arrays.asList(loadCache.values().toArray(new Float[0])));

                // Fill the missing points
                float[] filledLoadArray = fillData(floatArray, NUM_OF_DATA_POINTS);

                // Set the data
                data.put(i, ArrayUtils.toObject(filledLoadArray));
            }

            // Keep a timestamp
            timestamp = System.currentTimeMillis();

            return data;
        }

    }

}

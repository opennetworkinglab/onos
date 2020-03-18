/*
 * Copyright 2018-present Open Networking Foundation
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

import org.onosproject.drivers.server.behavior.CpuStatisticsDiscovery;
import org.onosproject.drivers.server.devices.RestServerSBDevice;
import org.onosproject.drivers.server.stats.CpuStatistics;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.device.DeviceService;
import org.onosproject.ui.RequestHandler;
import org.onosproject.ui.chart.ChartModel;

import org.apache.commons.lang.ArrayUtils;
import org.joda.time.LocalDateTime;
import org.slf4j.Logger;

import java.util.Arrays;
import java.util.Collection;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.onosproject.drivers.server.Constants.MSG_UI_DATA_CPU_NULL;
import static org.onosproject.drivers.server.gui.MetricType.CPU;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Message handler for passing CPU load data to the Web UI.
 */
public class CpuViewMessageHandler extends BaseViewMessageHandler {

    private static final Logger log = getLogger(CpuViewMessageHandler.class);

    private static final String CPU_DATA_REQ = "cpuDataRequest";
    private static final String CPU_DATA_RESP = "cpuDataResponse";
    private static final String CPUS_LABEL = "cpus";

    @Override
    protected Collection<RequestHandler> createRequestHandlers() {
        return ImmutableSet.of(new CpuMessageRequest());
    }

    private final class CpuMessageRequest
        extends BaseViewMessageHandler.ControlMessageRequest {

        private CpuMessageRequest() {
            super(CPU_DATA_REQ, CPU_DATA_RESP, CPUS_LABEL);
        }

        @Override
        protected String[] getSeries() {
            return createIndexedSeries(CPU, MAX_COLUMNS_NB);
        }

        @Override
        protected void populateChart(ChartModel cm, ObjectNode payload) {
            DeviceService ds = get(DeviceService.class);
            if ((ds == null) || (ds.getAvailableDeviceCount() == 0)) {
                fillDataWhenNoDevicePresent(cm, CPU, MAX_COLUMNS_NB);
                return;
            }

            String uri = string(payload, "devId");

            // Project only one device over time
            if (!Strings.isNullOrEmpty(uri)) {
                DeviceId deviceId = DeviceId.deviceId(uri);
                RestServerSBDevice serverDev =
                    (RestServerSBDevice) basicDriver.getController().getDevice(deviceId);

                List<CpuStatistics> cpuStats = null;
                Map<Integer, Float[]> data = null;
                try {
                    cpuStats = new ArrayList(serverDriver.getCpuStatistics(deviceId));
                    data = populateCpuDataHistory(deviceId, serverDev.numberOfCpus(), cpuStats);
                } catch (Exception ex) {
                    data = populateZeroMapDataHistory(deviceId, MAX_COLUMNS_NB);
                }
                checkNotNull(data, MSG_UI_DATA_CPU_NULL);

                // Generate a timestamp
                LocalDateTime ldt = new LocalDateTime(timestamp);

                // Project the data
                populateMapMetrics(cm, data, ldt, CPU, NUM_OF_DATA_POINTS);

                Set<DeviceId> deviceIds = Sets.newHashSet();
                for (Device device : ds.getAvailableDevices()) {
                    // Only devices that support CPU monitoring are considered
                    if (device.is(CpuStatisticsDiscovery.class) && serverDev.isActive()) {
                        deviceIds.add(device.id());
                    }
                }

                // Drop down list to select devices
                attachDeviceList(cm, deviceIds);
            } else {
                for (Device device : ds.getAvailableDevices()) {
                    // Only devices that support CPU monitoring are considered
                    if (!device.is(CpuStatisticsDiscovery.class)) {
                        continue;
                    }

                    DeviceId deviceId = device.id();
                    RestServerSBDevice serverDev =
                        (RestServerSBDevice) basicDriver.getController().getDevice(deviceId);

                    List<CpuStatistics> cpuStats = null;
                    Map<Integer, Float> data = null;
                    try {
                        cpuStats = new ArrayList(serverDriver.getCpuStatistics(deviceId));
                        data = populateCpuData(deviceId, serverDev.numberOfCpus(), cpuStats);
                    } catch (Exception ex) {
                        data = populateZeroMapData(deviceId, MAX_COLUMNS_NB);
                    }
                    checkNotNull(data, MSG_UI_DATA_CPU_NULL);

                    // Map them to the CPU cores
                    Map<String, Object> local = Maps.newHashMap();
                    for (int i = 0; i < data.size(); i++) {
                        local.put(getIndexedLabel(CPU, i), data.get(i));
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
         * structure that can feed the CPU UI memory.
         *
         * @param deviceId the device ID being monitored
         * @param length the length of the array
         * @param cpuStats the CPU load per core
         * @return a map of CPU metrics to their values
         */
        private Map<Integer, Float> populateCpuData(
                DeviceId deviceId, int length, List<CpuStatistics> cpuStats) {
            Map<Integer, Float> data = initializeMapData(MAX_COLUMNS_NB);

            for (CpuStatistics stats : cpuStats) {
                int index = stats.id();

                // Store it locally
                addToCache(deviceId, length, index, stats.load());

                // Project the floating point load value in [0, 1] to [0, 100]
                Float projectedVal = new Float(stats.load() * (float) 100);

                // Now the data is in the right form
                data.put(index, projectedVal);
            }

            return data;
        }

        /**
         * Turn the monitoring data history into a
         * data structure that can feed the CPU UI memory.
         *
         * @param deviceId the device ID being monitored
         * @param length the length of the array
         * @param cpuStats the CPU load per core
         * @return a map of CPU metrics to their arrays of values
         */
        private Map<Integer, Float[]> populateCpuDataHistory(
                DeviceId deviceId, int length, List<CpuStatistics> cpuStats) {
            Map<Integer, Float[]> data = initializeMapDataHistory(MAX_COLUMNS_NB);

            for (CpuStatistics stats : cpuStats) {
                int index = stats.id();

                // Store it locally
                addToCache(deviceId, length, index, stats.load());

                LruCache<Float> loadCache = getDataHistory(deviceId, index);
                if (loadCache == null) {
                    continue;
                }
                float[] floatArray = Floats.toArray(Arrays.asList(loadCache.values().toArray(new Float[0])));

                // Project the load array to the range of [0, 100]
                for (int j = 0; j < floatArray.length; j++) {
                    floatArray[j] = floatArray[j] * (float) 100;
                }

                // Fill the missing points
                float[] filledLoadArray = fillData(floatArray, NUM_OF_DATA_POINTS);

                // Set the data
                data.put(index, ArrayUtils.toObject(filledLoadArray));
            }

            // Keep a timestamp
            timestamp = System.currentTimeMillis();

            return data;
        }

    }

}

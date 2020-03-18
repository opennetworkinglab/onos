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

import org.onosproject.drivers.server.behavior.MonitoringStatisticsDiscovery;
import org.onosproject.drivers.server.devices.RestServerSBDevice;
import org.onosproject.drivers.server.stats.CpuStatistics;
import org.onosproject.drivers.server.stats.MonitoringStatistics;
import org.onosproject.drivers.server.stats.MonitoringUnit.LatencyUnit;
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
import static org.onosproject.drivers.server.Constants.MSG_UI_DATA_LATENCY_NULL;
import static org.onosproject.drivers.server.gui.MetricType.LATENCY;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Message handler for passing latency data to the Web UI.
 */
public class LatencyViewMessageHandler extends BaseViewMessageHandler {

    private static final Logger log = getLogger(LatencyViewMessageHandler.class);

    private static final String LATENCY_DATA_REQ = "latencyDataRequest";
    private static final String LATENCY_DATA_RESP = "latencyDataResponse";
    private static final String LATENCY_LABEL = "latencys";

    @Override
    protected Collection<RequestHandler> createRequestHandlers() {
        return ImmutableSet.of(new LatencyMessageRequest());
    }

    private final class LatencyMessageRequest
        extends BaseViewMessageHandler.ControlMessageRequest {

        private LatencyMessageRequest() {
            super(LATENCY_DATA_REQ, LATENCY_DATA_RESP, LATENCY_LABEL);
        }

        @Override
        protected String[] getSeries() {
            return createIndexedSeries(LATENCY, MAX_COLUMNS_NB);
        }

        @Override
        protected void populateChart(ChartModel cm, ObjectNode payload) {
            DeviceService ds = get(DeviceService.class);
            if ((ds == null) || (ds.getAvailableDeviceCount() == 0)) {
                fillDataWhenNoDevicePresent(cm, LATENCY, MAX_COLUMNS_NB);
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
                    data = populateZeroMapDataHistory(deviceId, MAX_COLUMNS_NB);
                } else {
                    data = populateLatencyDataHistory(deviceId, serverDev.numberOfCpus(), monStats);
                }
                checkNotNull(data, MSG_UI_DATA_LATENCY_NULL);

                // Generate a timestamp
                LocalDateTime ldt = new LocalDateTime(timestamp);

                // Project the data
                populateMapMetrics(cm, data, ldt, LATENCY, NUM_OF_DATA_POINTS);

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
                        data = populateZeroMapData(deviceId, MAX_COLUMNS_NB);
                    } else {
                        data = populateLatencyData(deviceId, serverDev.numberOfCpus(), monStats);
                    }
                    checkNotNull(data, MSG_UI_DATA_LATENCY_NULL);

                    // Map them to the CPU cores
                    Map<String, Object> local = Maps.newHashMap();
                    for (int i = 0; i < data.size(); i++) {
                        local.put(getIndexedLabel(LATENCY, i), data.get(i));
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
         * structure that can feed the Latency UI memory.
         *
         * @param deviceId the device ID being monitored
         * @param length the length of the array
         * @param monStats a MonitoringStatistics object
         * @return a map of latency metrics to their values
         */
        private Map<Integer, Float> populateLatencyData(
                DeviceId deviceId, int length, MonitoringStatistics monStats) {
            Map<Integer, Float> data = initializeMapData(MAX_COLUMNS_NB);

            for (CpuStatistics stats : monStats.cpuStatisticsAll()) {
                int index = stats.id();

                // TODO: Use min and max latency to plot bars plots with error bars
                Float value = null;
                if ((stats.averageLatency().isPresent()) && (stats.load() > MIN_CPU_LOAD)) {
                    value = stats.averageLatency().get();
                } else {
                    value = new Float(0);
                }

                // Unit conversion
                LatencyUnit latencyUnit = null;
                if (stats.latencyUnit().isPresent()) {
                    latencyUnit = (LatencyUnit) stats.latencyUnit().get();
                } else {
                    latencyUnit = LatencyUnit.NANO_SECOND;
                }
                value = LatencyUnit.toNano(value, latencyUnit);

                // Store it locally
                addToCache(deviceId, length, index, value);

                // And into the map
                data.put(index, value);
            }

            return data;
        }

        /**
         * Turn the monitoring data history into a
         * data structure that can feed the Latency UI memory.
         *
         * @param deviceId the device ID being monitored
         * @param length the length of the array
         * @param monStats a MonitoringStatistics object
         * @return a map of latency metrics to their arrays of values
         */
        private Map<Integer, Float[]> populateLatencyDataHistory(
                DeviceId deviceId, int length, MonitoringStatistics monStats) {
            Map<Integer, Float[]> data = initializeMapDataHistory(MAX_COLUMNS_NB);

            for (CpuStatistics stats : monStats.cpuStatisticsAll()) {
                int index = stats.id();

                // TODO: Use min and max latency to plot bars plots with error bars
                Float value = null;
                if ((stats.averageLatency().isPresent()) && (stats.load() > MIN_CPU_LOAD)) {
                    value = stats.averageLatency().get();
                } else {
                    value = new Float(0);
                }

                // Unit conversion
                LatencyUnit latencyUnit = null;
                if (stats.latencyUnit().isPresent()) {
                    latencyUnit = (LatencyUnit) stats.latencyUnit().get();
                } else {
                    latencyUnit = LatencyUnit.NANO_SECOND;
                }
                value = LatencyUnit.toNano(value, latencyUnit);

                // Store it locally
                addToCache(deviceId, length, index, value);

                LruCache<Float> loadCache = getDataHistory(deviceId, index);
                if (loadCache == null) {
                    continue;
                }
                float[] floatArray = Floats.toArray(Arrays.asList(loadCache.values().toArray(new Float[0])));

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

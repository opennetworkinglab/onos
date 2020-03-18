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

import org.onosproject.drivers.server.BasicServerDriver;
import org.onosproject.drivers.server.ServerDevicesDiscovery;

import org.onosproject.net.DeviceId;
import org.onosproject.ui.UiMessageHandler;
import org.onosproject.ui.chart.ChartModel;
import org.onosproject.ui.chart.ChartRequestHandler;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Maps;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.LocalDateTime;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.IntStream;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.onosproject.drivers.server.Constants.MSG_UI_SUBMETRIC_NULL;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Base message handler for passing server data to the Web UI.
 */
public abstract class BaseViewMessageHandler extends UiMessageHandler {

    private static final Logger log = getLogger(BaseViewMessageHandler.class);

    // Time axis
    protected long timestamp = 0L;

    // Instance of the basic server driver
    protected static BasicServerDriver basicDriver = new BasicServerDriver();

    // Instance of the server driver
    protected static ServerDevicesDiscovery serverDriver = new ServerDevicesDiscovery();

    // A local memory to store monitoring data
    protected static Map<DeviceId, Map<Integer, LruCache<Float>>> devDataMap =
        new HashMap<DeviceId, Map<Integer, LruCache<Float>>>();

    // Data series length
    public static final int NUM_OF_DATA_POINTS = 30;

    // The maximum number of columns that can be projected
    public static final int MAX_COLUMNS_NB = 16;

    // Minimum CPU load
    public static final float MIN_CPU_LOAD = (float) 0.01;

    // Time axis
    public static final String TIME_FORMAT = "HH:mm:ss";

    // Device IDs
    public static final String DEVICE_IDS = "deviceIds";

    protected static final Map<Integer, String> MEM_SUBMETRIC_MAP = new HashMap<Integer, String>();
    static {
        MEM_SUBMETRIC_MAP.put(0, "used");
        MEM_SUBMETRIC_MAP.put(1, "free");
        MEM_SUBMETRIC_MAP.put(2, "total");
    }

    // Chart designer
    protected abstract class ControlMessageRequest extends ChartRequestHandler {

        protected ControlMessageRequest(String req, String res, String label) {
            super(req, res, label);
        }

        @Override
        protected abstract String[] getSeries();

        @Override
        protected abstract void populateChart(ChartModel cm, ObjectNode payload);

        /**
         * Returns a x-axis label for a monitoring value based on a map of submetrics.
         *
         * @param metric label metric
         * @param map a map of submetrics
         * @param index submetric index
         * @return a data label
         */
        protected String getMappedLabel(MetricType metric, Map<Integer, String> map, int index) {
            String submetric = map.get(index);
            checkNotNull(submetric, MSG_UI_SUBMETRIC_NULL);
            return StringUtils.lowerCase(metric.name()) + "_" + submetric;
        }

        /**
         * Returns a x-axis label for a monitoring value based on a simple index.
         *
         * @param metric label metric
         * @param index label index
         * @return a data label
         */
        protected String getIndexedLabel(MetricType metric, int index) {
            return StringUtils.lowerCase(metric.name()) + "_" + Integer.toString(index);
        }

        /**
         * Fills an array of strings acting as x-axis based on a map of submetrics.
         *
         * @param metric x-axis metric
         * @param map a map of submetrics
         * @param length the length of the array
         * @return an array of strings
         */
        protected String[] createMappedSeries(MetricType metric, Map<Integer, String> map, int length) {
            if (length <= 0) {
                return null;
            }

            if (length > MAX_COLUMNS_NB) {
                length = MAX_COLUMNS_NB;
            }

            String[] series = IntStream.range(0, length)
                .mapToObj(i -> getMappedLabel(metric, map, i))
                .toArray(String[]::new);

            return series;
        }

        /**
         * Fills an array of strings acting as x-axis based on a simple index.
         *
         * @param metric x-axis metric
         * @param length the length of the array
         * @return an array of strings
         */
        protected String[] createIndexedSeries(MetricType metric, int length) {
            if (length <= 0) {
                return null;
            }

            if (length > MAX_COLUMNS_NB) {
                length = MAX_COLUMNS_NB;
            }

            String[] series = IntStream.range(0, length)
                .mapToObj(i -> getIndexedLabel(metric, i))
                .toArray(String[]::new);

            return series;
        }

        /**
         * Returns a map of monitoring parameters to their load history buffers.
         *
         * @param deviceId the device being monitored
         * @param length the length of the array
         * @return a map of monitoring parameters to their load history buffers
         */
        protected Map<Integer, LruCache<Float>> fetchCacheForDevice(DeviceId deviceId, int length) {
            if (!isValid(deviceId, length - 1)) {
                log.error("Invalid access to data history by device {} with {} metrics", deviceId, length);
                return null;
            }

            if (devDataMap.containsKey(deviceId)) {
                return devDataMap.get(deviceId);
            }

            Map<Integer, LruCache<Float>> dataMap = new HashMap<Integer, LruCache<Float>>();
            for (int i = 0; i < length; i++) {
                dataMap.put(i, new LruCache<Float>(NUM_OF_DATA_POINTS));
            }

            devDataMap.put(deviceId, dataMap);

            return dataMap;
        }

        /**
         * Adds a value into a buffer with the latest data entries.
         *
         * @param deviceId the device being monitored
         * @param length the length of the array
         * @param index the data index
         * @param value the data value
         */
        protected void addToCache(
                DeviceId deviceId, int length, int index, float value) {
            if (!isValid(deviceId, length - 1) ||
                !isValid(deviceId, index)) {
                log.error("Invalid access to data {} history by device {} with {} metrics",
                    index, deviceId, length);
                return;
            }

            Map<Integer, LruCache<Float>> dataMap = devDataMap.get(deviceId);
            if (dataMap == null) {
                dataMap = fetchCacheForDevice(deviceId, length);
                checkNotNull(dataMap, "Failed to add measurement in the cache");
            }

            if (dataMap.get(index) != null) {
                dataMap.get(index).add(value);
            }
        }

        /**
         * Returns a buffer with the latest
         * entries of a device's monitoring parameter.
         *
         * @param deviceId the device being monitored
         * @param index a data index
         * @return a history of values
         */
        protected LruCache<Float> getDataHistory(DeviceId deviceId, int index) {
            if (!isValid(deviceId, index)) {
                log.error("Invalid access to index {} history by device {}", index, deviceId);
                return null;
            }

            Map<Integer, LruCache<Float>> dataMap = devDataMap.get(deviceId);
            if (dataMap == null) {
                return null;
            }

            return dataMap.get(index);
        }

        /**
         * Fill the UI memory's current values with zeros.
         *
         * @param deviceId the device ID being monitored
         * @param length the length of the array
         * @return a map of monitoring parameters to their initial values
         */
        protected Map<Integer, Float> populateZeroMapData(DeviceId deviceId, int length) {
            Map<Integer, Float> data = initializeMapData(length);

            for (int i = 0; i < length; i++) {
                // Store it locally
                addToCache(deviceId, length, i, 0);
            }

            return data;
        }

        /**
         * Fill the UI memory's history with zeros.
         *
         * @param deviceId the device ID being monitored
         * @param length the length of the array
         * @return a map of monitoring parameters to their initial arrays of values
         */
        protected Map<Integer, Float[]> populateZeroMapDataHistory(DeviceId deviceId, int length) {
            Map<Integer, Float[]> data = initializeMapDataHistory(length);

            for (int i = 0; i < length; i++) {
                addToCache(deviceId, length, i, 0);
            }

            // Keep a timestamp
            timestamp = System.currentTimeMillis();

            return data;
        }

        /**
         * Populate a specific metric with data.
         *
         * @param dataPoint the particular part of the chart to be fed
         * @param data the data to feed the metric of the chart
         */
        protected void populateMetric(ChartModel.DataPoint dataPoint, Map<String, Object> data) {
            data.forEach(dataPoint::data);
        }

        /**
         * Populate the metrics to the Web UI.
         *
         * @param cm the chart to be fed with data
         * @param data the data to feed the chart
         * @param time a timestamp
         * @param metric a metric
         * @param numberOfPoints the number of data points
         */
        protected void populateMapMetrics(
                ChartModel            cm,
                Map<Integer, Float[]> data,
                LocalDateTime         time,
                MetricType            metric,
                int                   numberOfPoints) {
            for (int i = 0; i < numberOfPoints; i++) {
                Map<String, Object> local = Maps.newHashMap();
                for (int j = 0; j < data.size(); j++) {
                    if (data.containsKey(j)) {
                        local.put(getIndexedLabel(metric, j), data.get(j)[i]);
                    }
                }

                String calculated = time.minusSeconds(numberOfPoints - i).toString(TIME_FORMAT);
                local.put(LABEL, calculated);

                populateMetric(cm.addDataPoint(calculated), local);
            }
        }

        /**
         * Checks the validity of a device's information.
         *
         * @param deviceId the device being monitored
         * @param length the length of the array
         * @return boolean data validity status
         */
        protected boolean isValid(DeviceId deviceId, int length) {
            return ((deviceId != null) && (length >= 0) &&
                    (length < MAX_COLUMNS_NB));
        }

        /**
         * Create a data structure with zero-initialized data.
         *
         * @param length the length of the array
         * @return a map of metrics to their initial values
         */
        protected Map<Integer, Float> initializeMapData(int length) {
            Map<Integer, Float> data = Maps.newHashMap();

            for (int i = 0; i < length; i++) {
                data.put(i, (float) 0);
            }

            return data;
        }

        /**
         * Create a map data structure with zero-initialized arrays of data.
         *
         * @param length the length of the array
         * @return a map of metrics to their initial arrays of values
         */
        protected Map<Integer, Float[]> initializeMapDataHistory(int length) {
            Map<Integer, Float[]> data = Maps.newHashMap();

            for (int i = 0; i < length; i++) {
                data.put(i, ArrayUtils.toObject(new float[NUM_OF_DATA_POINTS]));
            }

            return data;
        }

        /**
         * Fill the contents of an input array until a desired point.
         *
         * @param origin the original array with the data
         * @param expectedLength the desired length of the array
         * @return an array of a certain length
         */
        protected float[] fillData(float[] origin, int expectedLength) {
            if (origin.length == expectedLength) {
                return origin;
            } else {
                int desiredLength = origin.length;
                if (origin.length > expectedLength) {
                    desiredLength = expectedLength;
                }

                float[] filled = new float[expectedLength];
                for (int i = 0; i < desiredLength; i++) {
                    filled[i] = origin[i];
                }

                for (int i = desiredLength - 1; i < expectedLength; i++) {
                    filled[i] = origin[origin.length - 1];
                }

                return filled;
            }
        }

        /**
         * Attach the list of all devices to the top of the chart.
         *
         * @param cm the chart to be fed with data
         * @param deviceIds the set of Device IDs to show up
         */
        protected void attachDeviceList(ChartModel cm, Set<DeviceId> deviceIds) {
            checkNotNull(deviceIds, "No device IDs provided to chart");
            ArrayNode array = arrayNode();
            deviceIds.forEach(id -> array.add(id.toString()));
            cm.addAnnotation(DEVICE_IDS, array);
        }

        /**
         * Returns zero-initialized data for a metric when no devices are present.
         *
         * @param cm the chart to be fed with data
         * @param metric a metric to reset
         * @param length the length of the data array
         */
        protected void fillDataWhenNoDevicePresent(
                ChartModel cm, MetricType metric, int length) {
            Map<String, Object> local = Maps.newHashMap();
            for (int i = 0; i < length; i++) {
                local.put(getIndexedLabel(metric, i), new Float(0));
            }

            local.put(LABEL, "No Servers");
            populateMetric(cm.addDataPoint(""), local);
        }

    }

}

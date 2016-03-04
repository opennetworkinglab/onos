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

package org.onosproject.ui.chart;

import com.google.common.collect.Maps;

import java.util.Arrays;
import java.util.Map;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A simple model of chart data.
 *
 * <p>
 * Note that this is not a full MVC type model; the expected usage pattern
 * is to create an empty chart, add data points (by consulting the business model),
 * and produce the list of data points which contain a label and a set of data
 * values for all serials.
 */
public class ChartModel {

    // key is series name, value is series index
    private final Map<String, Integer> seriesMap;
    private final DataPoint[] dataPoints;

    /**
     * Constructs a chart model with initialized series set.
     *
     * @param size datapoints size
     * @param series a set of series
     */
    public ChartModel(int size, String... series) {
        checkNotNull(series, "series cannot be null");
        checkArgument(series.length > 0, "must be at least one series");
        seriesMap = Maps.newConcurrentMap();

        for (int index = 0; index < series.length; index++) {
            seriesMap.put(series[index], index);
        }

        checkArgument(size > 0, "must have at least one data point");
        dataPoints = new DataPoint[size];
    }

    private void checkDataPoint(DataPoint dataPoint) {
        checkArgument(dataPoint.getSize() == seriesCount(),
                "data size should be equal to number of series");
    }

    /**
     * Returns the number of series in this chart model.
     *
     * @return number of series
     */
    public int seriesCount() {
        return seriesMap.size();
    }

    /**
     * Shifts all of the data points to the left,
     * and adds a new data point to the tail of the array.
     *
     * @param label label name
     * @param values a set of data values
     */
    public void addDataPoint(String label, Double[] values) {
        DataPoint dp = new DataPoint(label, values);
        checkDataPoint(dp);

        for (int index = 1; index < dataPoints.length; index++) {
            dataPoints[index - 1] = dataPoints[index];
        }
        dataPoints[dataPoints.length - 1] = dp;
    }

    /**
     * Returns all of series.
     *
     * @return an array of series
     */
    public String[] getSeries() {
        return seriesMap.keySet().toArray(new String[seriesMap.size()]);
    }

    /**
     * Returns all of data points.
     *
     * @return an array of data points
     */
    public DataPoint[] getDataPoints() {
        return Arrays.copyOf(dataPoints, dataPoints.length);
    }

    /**
     * Returns the last element inside all of data points.
     *
     * @return data point
     */
    public DataPoint getLastDataPoint() {
        return dataPoints[dataPoints.length - 1];
    }

    /**
     * A class of data point.
     */
    public class DataPoint {
        // values for all series
        private final Double[] values;
        private final String label;

        /**
         * Constructs a data point.
         *
         * @param label label name
         * @param values a set of data values for all series
         */
        public DataPoint(String label, Double[] values) {
            this.label = label;
            this.values = values;
        }

        /**
         * Returns the label name of this data point.
         *
         * @return label name
         */
        public String getLabel() {
            return label;
        }

        /**
         * Returns the size of data point.
         * This should be identical to the size of series.
         *
         * @return size of data point
         */
        public int getSize() {
            return values.length;
        }

        /**
         * Returns the value of the data point of the given series.
         *
         * @param series series name
         * @return data value of a specific series
         */
        public Double getValue(String series) {
            return values[seriesMap.get(series)];
        }
    }
}

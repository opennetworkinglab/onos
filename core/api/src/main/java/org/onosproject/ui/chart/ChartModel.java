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

package org.onosproject.ui.chart;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A simple model of time series chart data.
 * <p>
 * Note that this is not a full MVC type model; the expected usage pattern
 * is to create an empty chart, add data points (by consulting the business
 * model), and produce the list of data points which contain a label and a set
 * of data values for all serials.
 */
public class ChartModel {

    private final Set<String> seriesSet;
    private final String[] seriesArray;
    private final List<Object> labels = Lists.newArrayList();
    private final List<DataPoint> dataPoints = Lists.newArrayList();
    private final Map<String, Annot> annotations = new HashMap<>();

    /**
     * Constructs a chart model with initialized series set.
     *
     * @param series a set of series
     */
    public ChartModel(String... series) {
        checkNotNull(series, "series cannot be null");
        checkArgument(series.length > 0, "must be at least one series");

        seriesSet = Sets.newHashSet(series);

        if (seriesSet.size() != series.length) {
            throw new IllegalArgumentException("duplicate series detected");
        }

        this.seriesArray = Arrays.copyOf(series, series.length);
    }

    private void checkDataPoint(DataPoint dataPoint) {
        checkArgument(dataPoint.size() == seriesCount(),
                "data size should be equal to number of series");
    }

    /**
     * Checks the validity of the given series.
     *
     * @param series series name
     */
    private void checkSeries(String series) {
        checkNotNull(series, "must provide a series name");
        if (!seriesSet.contains(series)) {
            throw new IllegalArgumentException("unknown series: " + series);
        }
    }

    /**
     * Returns the number of series in this chart model.
     *
     * @return number of series
     */
    public int seriesCount() {
        return seriesSet.size();
    }

    /**
     * Adds a data point to the chart model.
     *
     * @param label label instance
     * @return the data point, for chaining
     */
    public DataPoint addDataPoint(Object label) {
        DataPoint dp = new DataPoint();
        labels.add(label);
        dataPoints.add(dp);
        return dp;
    }

    /**
     * Returns all of series.
     *
     * @return an array of series
     */
    public String[] getSeries() {
        return seriesArray;
    }

    /**
     * Returns all of data points in order.
     *
     * @return an array of data points
     */
    public DataPoint[] getDataPoints() {
        return dataPoints.toArray(new DataPoint[dataPoints.size()]);
    }

    /**
     * Returns all of labels in order.
     *
     * @return an array of labels
     */
    public Object[] getLabels() {
        return labels.toArray(new Object[labels.size()]);
    }

    /**
     * Returns the number of data points in this chart model.
     *
     * @return number of data points
     */
    public int dataPointCount() {
        return dataPoints.size();
    }

    /**
     * Returns the last element inside all of data points.
     *
     * @return data point
     */
    public DataPoint getLastDataPoint() {
        return dataPoints.get(dataPoints.size() - 1);
    }

    /**
     * Inserts a new annotation.
     *
     * @param key key of annotation
     * @param value value of annotation
     */
    public void addAnnotation(String key, Object value) {
        annotations.put(key, new Annot(key, value));
    }

    /**
     * Returns the annotations in this chart.
     *
     * @return annotations
     */
    public Collection<Annot> getAnnotations() {
        return new ArrayList<>(annotations.values());
    }

    /**
     * Model of an annotation.
     */
    public class Annot {
        private final String key;
        private final Object value;

        /**
         * Constructs an annotation with the given key and value.
         *
         * @param key the key
         * @param value the value
         */
        public Annot(String key, Object value) {
            this.key = key;
            this.value = value;
        }

        /**
         * Returns the annotation's key.
         *
         * @return key
         */
        public String key() {
            return key;
        }

        /**
         * Returns the annotation's value.
         *
         * @return value
         */
        public Object value() {
            return value;
        }

        /**
         * Returns the value as a string.
         * This default implementation uses the value's toString() method.
         *
         * @return the value as a string
         */
        public String valueAsString() {
            return value.toString();
        }
    }

    /**
     * A class of data point.
     */
    public class DataPoint {
        // values for all series
        private final Map<String, Object> data = Maps.newHashMap();

        /**
         * Sets the data value for the given series of this data point.
         *
         * @param series series name
         * @param value  value to set
         * @return self, for chaining
         */
        public DataPoint data(String series, Object value) {
            checkSeries(series);
            data.put(series, value);
            return this;
        }

        /**
         * Returns the data value with the given series for this data point.
         *
         * @param series  series name
         * @return data value
         */
        public Object get(String series) {
            return data.get(series);
        }

        /**
         * Return the data value with the same order of series.
         *
         * @return an array of ordered data values
         */
        public Object[] getAll() {
            Object[] value = new Object[getSeries().length];
            int idx = 0;
            for (String s : getSeries()) {
                value[idx] = get(s);
                idx++;
            }
            return value;
        }

        /**
         * Returns the size of data point.
         *
         * @return the size of data point
         */
        public int size() {
            return data.size();
        }

        /**
         * Returns the value of the data point as a string, using the
         * formatter appropriate for the series.
         *
         * @param series series
         * @return formatted data point value
         */
        public String getAsString(String series) {
            return get(series).toString();
        }

        /**
         * Returns the row as an array of formatted strings.
         *
         * @return the string format of data points
         */
        public String[] getAsStrings() {
            List<String> formatted = new ArrayList<>(size());
            for (String c : seriesArray) {
                formatted.add(getAsString(c));
            }
            return formatted.toArray(new String[formatted.size()]);
        }
    }
}

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

import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Database for storing a metric.
 */
public interface MetricsDatabase {
    /**
     * Returns the metric name of this database.
     *
     * @return metric name
     */
    String metricName();

    /**
     * Returns the resource name of this database.
     *
     * @return resource name
     */
    String resourceName();

    /**
     * Update metric value by specifying metric type.
     *
     * @param metricType    metric type (e.g., load, usage, etc.)
     * @param value         metric value
     */
    void updateMetric(String metricType, double value);

    /**
     * Update metric value by specifying metric type in a certain time.
     *
     * @param metricType    metric type (e.g., load, usage, etc.)
     * @param value         metric value
     * @param time          update time in seconds
     */
    void updateMetric(String metricType, double value, long time);

    /**
     * Update metric values of a collection of metric types.
     *
     * @param metrics       a collection of metrics which consists of a pair of
     *                      metric type and metric value
     * @param time          update time in seconds
     */
    void updateMetrics(Map<String, Double> metrics, long time);

    /**
     * Update metric values of a collection of metric types.
     *
     * @param metrics       a collection of metrics which consists of a pair of
     *                      metric type and metric value
     */
    void updateMetrics(Map<String, Double> metrics);

    /**
     * Returns most recent metric value of a given metric type.
     *
     * @param metricType metric type
     * @return metric value
     */
    double recentMetric(String metricType);

    /**
     * Return most recent metric values of a given metric type for a given period.
     *
     * @param metricType    metric type
     * @param duration      duration
     * @param unit          time unit
     * @return a collection of metric value
     */
    double[] recentMetrics(String metricType, int duration, TimeUnit unit);

    /**
     * Returns minimum metric value of a given metric type.
     *
     * @param metricType    metric type
     * @return metric value
     */
    double minMetric(String metricType);

    /**
     * Returns maximum metric value of a given metric type.
     *
     * @param metricType    metric type
     * @return metric value
     */
    double maxMetric(String metricType);

    /**
     * Returns a collection of metric values of a given metric type for a day.
     *
     * @param metricType    metric type
     * @return a collection of metric value
     */
    double[] metrics(String metricType);

    /**
     * Returns a collection of metric values of a given metric type for
     * a given period.
     *
     * @param metricType    metric type
     * @param startTime     start time
     * @param endTime       end time
     * @return a collection of metric value
     */
    double[] metrics(String metricType, long startTime, long endTime);

    /**
     * Returns the latest metric update time.
     *
     * @param metricType    metric type
     * @return timestamp
     */
    long lastUpdate(String metricType);

    /**
     * A builder of MetricsDatabase.
     */
    interface Builder {

        /**
         * Sets the metric name.
         *
         * @param metricName metric name
         * @return builder object
         */
        Builder withMetricName(String metricName);

        /**
         * Sets the resource name.
         *
         * @param resourceName resource name
         * @return builder object
         */
        Builder withResourceName(String resourceName);

        /**
         * Add a new metric to be monitored.
         *
         * @param metricType control metric type
         * @return builder object
         */
        Builder addMetricType(String metricType);

        /**
         * Builds a metric database instance.
         *
         * @return metric database instance
         */
        MetricsDatabase build();
    }
}

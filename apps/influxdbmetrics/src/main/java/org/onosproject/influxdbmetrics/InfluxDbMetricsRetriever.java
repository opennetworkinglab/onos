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
package org.onosproject.influxdbmetrics;

import org.onosproject.cluster.NodeId;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * A Metric retriever interface for querying metrics value from influxDB server.
 */
public interface InfluxDbMetricsRetriever {

    /**
     * Returns last metric values from all nodes.
     *
     * @return all metrics from all nodes
     */
    Map<NodeId, Map<String, InfluxMetric>> allMetrics();

    /**
     * Returns last metric values from a node.
     *
     * @param nodeId node identification
     * @return all metrics from a given node
     */
    Map<String, InfluxMetric> metricsByNodeId(NodeId nodeId);

    /**
     * Returns a collection of last metric values from all nodes.
     *
     * @param metricName metric name
     * @return a collection of metrics from all nodes
     */
    Map<NodeId, InfluxMetric> metricsByName(String metricName);

    /**
     * Returns a last metric value from a given node.
     *
     * @param nodeId node identification
     * @param metricName metric name
     * @return a metric value from a given node
     */
    InfluxMetric metric(NodeId nodeId, String metricName);

    /**
     * Returns metric values of all nodes within a given period of time.
     *
     * @param period projected period
     * @param unit time unit
     * @return all metric values of all nodes
     */
    Map<NodeId, Map<String, List<InfluxMetric>>> allMetrics(int period, TimeUnit unit);

    /**
     * Returns metric values of a node within a given period of time.
     *
     * @param nodeId node identification
     * @param period projected period
     * @param unit time unit
     * @return metric value of a node
     */
    Map<String, List<InfluxMetric>> metricsByNodeId(NodeId nodeId, int period, TimeUnit unit);

    /**
     * Returns a collection of last metric values of all nodes within a given period of time.
     *
     * @param metricName metric name
     * @param period projected period
     * @param unit time unit
     * @return metric value of all nodes
     */
    Map<NodeId, List<InfluxMetric>> metricsByName(String metricName, int period, TimeUnit unit);

    /**
     * Returns metric value of a given node within a given period of time.
     *
     * @param nodeId node identification
     * @param metricName metric name
     * @param period projected period
     * @param unit time unit
     * @return metric value of a node
     */
    List<InfluxMetric> metric(NodeId nodeId, String metricName, int period, TimeUnit unit);

    /**
     * Configures default parameters for influx database metrics retriever.
     *
     * @param address   IP address of influxDB server
     * @param port      Port number of influxDB server
     * @param database  Database name of influxDB server
     * @param username  Username of influxDB server
     * @param password  Password of influxDB server
     */
    void config(String address, int port, String database, String username, String password);
}

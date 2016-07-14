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

import org.onlab.metrics.MetricsReporter;

/**
 * A Metric reporter interface for reporting all metrics to influxDB server.
 */
public interface InfluxDbMetricsReporter extends MetricsReporter {

    /**
     * Configures default parameters for influx database metrics reporter.
     *
     * @param address   IP address of influxDB server
     * @param port      Port number of influxDB server
     * @param database  Database name of influxDB server
     * @param username  Username of influxDB server
     * @param password  Password of influxDB server
     */
    void config(String address, int port, String database, String username, String password);
}

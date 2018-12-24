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
package org.onosproject.openstacktelemetry.api.config;

import java.util.Map;

/**
 * Configuration API of InfluxDB for publishing openstack telemetry.
 */
public interface InfluxDbTelemetryConfig extends TelemetryConfigProperties {

    /**
     * Obtains InfluxDB server IP address.
     *
     * @return InfluxDB server IP address
     */
    String address();

    /**
     * Obtains InfluxDB server port number.
     *
     * @return InfluxDB server port number
     */
    int port();

    /**
     * Obtains InfluxDB username for accessing.
     *
     * @return InfluxDB username
     */
    String username();

    /**
     * Obtains InfluxDB password for accessing.
     *
     * @return InfluxDB password
     */
    String password();

    /**
     * Obtains InfluxDB database name.
     *
     * @return InfluxDB database name
     */
    String database();

    /**
     * Obtains InfluxDB measurement name.
     *
     * @return InfluxDB measurement name
     */
    String measurement();

    /**
     * Obtains InfluxDB enable batch flag.
     *
     * @return InfluxDB enable batch flag
     */
    boolean enableBatch();

    /**
     * Obtains InfluxDB config maps.
     *
     * @return InfluxDB config map
     */
    Map<String, Object> configMap();

    /**
     * Builder class of InfluxDbTelemetryConfig.
     */
    interface Builder extends TelemetryConfigProperties.Builder {

        /**
         * Sets InfluxDB server IP address.
         *
         * @param address InfluxDB server IP address
         * @return builder instances
         */
        Builder withAddress(String address);

        /**
         * Sets InfluxDB server port number.
         *
         * @param port InfluxDB server port number
         * @return builder instance
         */
        Builder withPort(int port);

        /**
         * Sets InfluxDB username.
         *
         * @param username InfluxDB username
         * @return builder instance
         */
        Builder withUsername(String username);

        /**
         * Sets InfluxDB password.
         *
         * @param password InfluxDB password
         * @return builder instance
         */
        Builder withPassword(String password);

        /**
         * Sets InfluxDB measurement.
         *
         * @param measurement InfluxDB measurement
         * @return builder instance
         */
        Builder withMeasurement(String measurement);

        /**
         * Sets InfluxDB database.
         *
         * @param database InfluxDB database
         * @return builder instance
         */
        Builder withDatabase(String database);

        /**
         * Sets InfluxDB enable batch flag.
         *
         * @param enableBatch enable batch flag
         * @return builder instance
         */
        Builder withEnableBatch(boolean enableBatch);

        /**
         * Sets other InfluxDB configuration map.
         *
         * @param configMap InfluxDB configuration map
         * @return builder instance
         */
        Builder withConfigMap(Map<String, Object> configMap);

        /**
         * Creates a InfluxDB telemetry config instance.
         *
         * @return InfluxDB telemetry config instance
         */
        InfluxDbTelemetryConfig build();
    }
}
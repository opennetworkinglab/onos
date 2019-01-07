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

import org.onosproject.net.Annotations;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * An interface for telemetry config.
 */
public interface TelemetryConfig extends Annotations {

    /**
     * Telemetry configuration type.
     */
    enum ConfigType {
        /**
         * Indicates KAFKA telemetry config.
         */
        KAFKA,

        /**
         * Indicates GRPC telemetry config.
         */
        GRPC,

        /**
         * Indicates REST telemetry config.
         */
        REST,

        /**
         * Indicates InfluxDB telemetry config.
         */
        INFLUXDB,

        /**
         * Indicates prometheus telemetry config.
         */
        PROMETHEUS,

        /**
         * Indicates unknown telemetry config.
         */
        UNKNOWN
    }

    enum Status {
        /**
         * Signifies that the service is in enable status.
         */
        ENABLED,

        /**
         * Signifies that the service is in disable status.
         */
        DISABLED,

        /**
         * Signifies that the service is in pending status.
         */
        PENDING,

        /**
         * Signifies that the service is in unknown status.
         */
        UNKNOWN,
    }

    /**
     * Returns the telemetry configuration name.
     *
     * @return configuration name
     */
    String name();

    /**
     * Returns the telemetry configuration type.
     *
     * @return configuration type
     */
    ConfigType type();

    /**
     * Returns all the parent configurations from which this configuration inherits
     * properties.
     *
     * @return list of parent configurations
     */
    List<TelemetryConfig> parents();

    /**
     * Returns the off-platform application manufacturer name.
     *
     * @return manufacturer name
     */
    String manufacturer();

    /**
     * Returns the off-platform application software version.
     *
     * @return software version
     */
    String swVersion();

    /**
     * Returns the service status.
     *
     * @return service status
     */
    Status status();

    /**
     * Returns the set of annotations as map of key/value properties.
     *
     * @return map of properties
     */
    Map<String, String> properties();

    /**
     * Gets the value of the given property name.
     *
     * @param name property name
     * @return the value of the property,
     *         or null if the property is not defined in this configuration nor
     *         in any of its ancestors
     */
    String getProperty(String name);

    /**
     * Get the value of the given property name.
     *
     * @param name property name
     * @param defaultValue to use if the property is not defined in this configuration
     *                     nor in any of its ancestors
     * @return the value of the property,
     *         or null if the property is not defined in this configuration nor
     *         in any of its ancestors
     */
    default String getProperty(String name, String defaultValue) {
        return Optional.ofNullable(getProperty(name)).orElse(defaultValue);
    }

    /**
     * Merges the specified config properties into this one, giving preference to
     * the other config when dealing with conflicts.
     *
     * @param other other configuration
     * @return merged configuration
     */
    TelemetryConfig merge(TelemetryConfig other);

    /**
     * Obtains the cloned instance with updated properties.
     *
     * @param properties telemetry config properties
     * @return a cloned instance
     */
    TelemetryConfig updateProperties(Map<String, String> properties);

    /**
     * Obtains the cloned instance with updated status.
     *
     * @param status service status
     * @return a cloned instance
     */
    TelemetryConfig updateStatus(Status status);
}

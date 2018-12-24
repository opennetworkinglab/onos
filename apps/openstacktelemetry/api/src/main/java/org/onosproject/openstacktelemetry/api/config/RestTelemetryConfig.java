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
 * Configuration API of REST for publishing openstack telemetry.
 */
public interface RestTelemetryConfig extends TelemetryConfigProperties {

    /**
     * Obtains REST IP address.
     *
     * @return REST IP address
     */
    String address();

    /**
     * Obtains REST port number.
     *
     * @return REST port number
     */
    int port();

    /**
     * Obtains default REST server endpoint.
     *
     * @return default REST server endpoint
     */
    String endpoint();

    /**
     * Obtains HTTP method for publishing network metrics.
     *
     * @return HTTP method
     */
    String method();

    /**
     * Obtains request media type.
     *
     * @return request media type
     */
    String requestMediaType();

    /**
     * Obtains response media type.
     *
     * @return response media type
     */
    String responseMediaType();

    /**
     * Obtains REST config maps.
     *
     * @return REST config map
     */
    Map<String, Object> configMap();

    /**
     * Builder class for RestTelemetryConfig.
     */
    interface Builder extends TelemetryConfigProperties.Builder {

        /**
         * Sets REST server IP address.
         *
         * @param address REST server IP address
         * @return builder instance
         */
        Builder withAddress(String address);

        /**
         * Sets REST server port number.
         *
         * @param port REST server port number
         * @return builder instance
         */
        Builder withPort(int port);

        /**
         * Sets REST server default endpoint.
         *
         * @param endpoint REST server default endpoint
         * @return builder instance
         */
        Builder withEndpoint(String endpoint);

        /**
         * Sets HTTP method.
         *
         * @param method HTTP method
         * @return builder instance
         */
        Builder withMethod(String method);

        /**
         * Sets REST request media type.
         *
         * @param mediaType REST request media type
         * @return builder instance
         */
        Builder withRequestMediaType(String mediaType);

        /**
         * Sets REST response media type.
         *
         * @param mediaType REST response media type
         * @return builder instance
         */
        Builder withResponseMediaType(String mediaType);

        /**
         * Sets REST config map.
         *
         * @param configMap REST config map
         * @return builder instance
         */
        Builder withConfigMap(Map<String, Object> configMap);

        /**
         * Creates a REST telemetry config instance.
         *
         * @return REST telemetry config instance
         */
        RestTelemetryConfig build();
    }
}
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

public interface PrometheusTelemetryConfig extends TelemetryConfigProperties {

    /**
     * Obtains prometheus exporter IP address.
     *
     * @return IP address which prometheus exporter binds
     */
    String address();

    /**
     * Obtains prometheus exporter port number.
     *
     * @return prometheus exporter port number
     */
    int port();

    /**
     * Obtains prometheus config maps.
     *
     * @return prometheus config map
     */
    Map<String, Object> configMap();


    /**
     * Builder class of PrometheusTelemetryConfig.
     */
    interface Builder extends TelemetryConfigProperties.Builder {

        /**
         * Sets prometheus exporter IP address.
         *
         * @param address prometheus exporter IP
         * @return builder instance
         */
        Builder withAddress(String address);

        /**
         * Sets prometheus exporter port number.
         *
         * @param port prometheus exporter port
         * @return builder instance
         */
        Builder withPort(int port);

        /**
         * Sets other prometheus configuration map.
         *
         * @param configMap prometheus configuration map
         * @return builder instance
         */
        Builder withConfigMap(Map<String, Object> configMap);

        /**
         * Creates a prometheus telemetry config instance.
         *
         * @return prometheus telemetry config instance
         */
        PrometheusTelemetryConfig build();
    }
}
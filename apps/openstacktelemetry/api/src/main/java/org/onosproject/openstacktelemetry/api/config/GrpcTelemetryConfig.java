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
 * Configuration API of gRPC for publishing openstack telemetry.
 */
public interface GrpcTelemetryConfig extends TelemetryConfigProperties {

    /**
     * Obtains gRPC server IP address.
     *
     * @return gRPC server IP address
     */
    String address();

    /**
     * Obtains gRPC server port number.
     *
     * @return gRPC server port number
     */
    int port();

    /**
     * Obtains usePlaintext gRPC config flag.
     *
     * @return usePlaintext gRPC config flag
     */
    boolean usePlaintext();

    /**
     * Obtains max inbound message size.
     *
     * @return max inbound message size
     */
    int maxInboundMsgSize();

    /**
     * Obtains kafka config maps.
     *
     * @return kafka config map
     */
    Map<String, Object> configMap();

    /**
     * Builder class of GrpcTelemetryConfig.
     */
    interface Builder extends TelemetryConfigProperties.Builder {

        /**
         * Sets gRPC server IP address.
         *
         * @param address gRPC server IP address
         * @return builder instances
         */
        Builder withAddress(String address);

        /**
         * Sets gRPC server port number.
         *
         * @param port gRPC server port number
         * @return builder instance
         */
        Builder withPort(int port);

        /**
         * Sets usePlaintext config flag.
         *
         * @param usePlaintext usePlaintext config flag
         * @return builder instance
         */
        Builder withUsePlaintext(boolean usePlaintext);

        /**
         * Sets maximum inbound message size.
         *
         * @param maxInboundMsgSize maximum inbound message size
         * @return builder instance
         */
        Builder withMaxInboundMsgSize(int maxInboundMsgSize);

        /**
         * Sets other gRPC configuration map.
         *
         * @param configMap gRPC configuration map
         * @return builder instance
         */
        Builder withConfigMap(Map<String, Object> configMap);

        /**
         * Creates a gRPC telemetry config instance.
         *
         * @return gRPC telemetry config instance
         */
        GrpcTelemetryConfig build();
    }
}
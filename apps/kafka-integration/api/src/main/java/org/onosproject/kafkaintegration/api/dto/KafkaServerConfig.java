/*
 * Copyright 2016-present Open Networking Foundation
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
package org.onosproject.kafkaintegration.api.dto;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * DTO to hold Kafka Server Configuration information.
 *
 */
public final class KafkaServerConfig {

    private final String ipAddress;

    private final String port;

    private final int numOfRetries;

    private final int maxInFlightRequestsPerConnection;

    private final int acksRequired;

    private final String keySerializer;

    private final String valueSerializer;

    private KafkaServerConfig(String ipAddress, String port, int numOfRetries,
                              int maxInFlightRequestsPerConnection,
                              int requestRequiredAcks, String keySerializer,
                              String valueSerializer) {

        this.ipAddress = checkNotNull(ipAddress, "Ip Address Cannot be null");
        this.port = checkNotNull(port, "Port Number cannot be null");
        this.numOfRetries = numOfRetries;
        this.maxInFlightRequestsPerConnection =
                maxInFlightRequestsPerConnection;
        this.acksRequired = requestRequiredAcks;
        this.keySerializer = keySerializer;
        this.valueSerializer = valueSerializer;
    }

    public final String getIpAddress() {
        return ipAddress;
    }

    public final String getPort() {
        return port;
    }

    public final int getNumOfRetries() {
        return numOfRetries;
    }

    public final int getMaxInFlightRequestsPerConnection() {
        return maxInFlightRequestsPerConnection;
    }

    public final int getAcksRequired() {
        return acksRequired;
    }

    public final String getKeySerializer() {
        return keySerializer;
    }

    public final String getValueSerializer() {
        return valueSerializer;
    }

    /**
     * To create an instance of the builder.
     *
     * @return instance of builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder class for KafkaServerConfig.
     */
    public static final class Builder {
        private String ipAddress;

        private String port;

        private int numOfRetries;

        private int maxInFlightRequestsPerConnection;

        private int acksRequired;

        private String keySerializer;

        private String valueSerializer;

        public Builder ipAddress(String ipAddress) {
            this.ipAddress = ipAddress;
            return this;
        }

        public Builder port(String port) {
            this.port = port;
            return this;
        }

        public Builder numOfRetries(int numOfRetries) {
            this.numOfRetries = numOfRetries;
            return this;
        }

        public Builder maxInFlightRequestsPerConnection(int maxInFlightRequestsPerConnection) {
            this.maxInFlightRequestsPerConnection =
                    maxInFlightRequestsPerConnection;
            return this;
        }

        public Builder acksRequired(int acksRequired) {
            this.acksRequired = acksRequired;
            return this;
        }

        public Builder keySerializer(String keySerializer) {
            this.keySerializer = keySerializer;
            return this;
        }

        public Builder valueSerializer(String valueSerializer) {
            this.valueSerializer = valueSerializer;
            return this;
        }

        public KafkaServerConfig build() {
            checkNotNull(ipAddress, "App name cannot be null");
            checkNotNull(port, "Subscriber group ID cannot " + "be " + "null");

            return new KafkaServerConfig(ipAddress, port, numOfRetries,
                                         maxInFlightRequestsPerConnection,
                                         acksRequired, keySerializer,
                                         valueSerializer);
        }
    }
}

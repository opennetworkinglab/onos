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
 * Configuration API of Kafka for publishing openstack telemetry.
 */
public interface KafkaTelemetryConfig extends TelemetryConfigProperties {

    /**
     * Obtains kafka IP address.
     *
     * @return kafka IP address
     */
    String address();

    /**
     * Obtains kafka port number.
     *
     * @return kafka port number
     */
    int port();

    /**
     * Obtains numbers of request retries.
     *
     * @return number of request retries
     */
    int retries();

    /**
     * Obtains required acknowledgement.
     *
     * @return required acknowledgement
     */
    String requiredAcks();

    /**
     * Obtains batch size.
     *
     * @return batch size
     */
    int batchSize();

    /**
     * Obtains linger.
     *
     * @return linger
     */
    int lingerMs();

    /**
     * Obtains memory buffer size.
     *
     * @return memory buffer size
     */
    int memoryBuffer();

    /**
     * Obtains kafka key serializer.
     *
     * @return kafka key serializer
     */
    String keySerializer();

    /**
     * Obtains kafka value serializer.
     *
     * @return kafka value serializer
     */
    String valueSerializer();

    /**
     * Obtains kafka key.
     *
     * @return kafka key
     */
    String key();

    /**
     * Obtains kafka topic.
     *
     * @return kafka topic
     */
    String topic();

    /**
     * Obtains kafka message codec.
     *
     * @return kafka message codec
     */
    String codec();

    /**
     * Obtains kafka config maps.
     *
     * @return kafka config map
     */
    Map<String, Object> configMap();

    /**
     * Builder class of KafkaTelemetryConfig.
     */
    interface Builder extends TelemetryConfigProperties.Builder {

        /**
         * Sets kafka IP address.
         *
         * @param address kafka IP address
         * @return builder instances
         */
        Builder withAddress(String address);

        /**
         * Sets kafka port number.
         *
         * @param port kafka port number
         * @return builder instance
         */
        Builder withPort(int port);

        /**
         * Sets number of request retries.
         *
         * @param retries number of request retries
         * @return builder instance
         */
        Builder withRetries(int retries);

        /**
         * Sets the required acknowledgment.
         *
         * @param requiredAcks required acknowledgement
         * @return builder instance
         */
        Builder withRequiredAcks(String requiredAcks);

        /**
         * Sets batch size.
         *
         * @param batchSize batch size
         * @return builder instance
         */
        Builder withBatchSize(int batchSize);

        /**
         * Sets linger ms.
         *
         * @param lingerMs linger ms
         * @return builder instance
         */
        Builder withLingerMs(int lingerMs);

        /**
         * Sets memory buffer size.
         *
         * @param memoryBuffer memory buffer size
         * @return builder instance
         */
        Builder withMemoryBuffer(int memoryBuffer);

        /**
         * Sets kafka key serializer.
         *
         * @param keySerializer kafka key serializer
         * @return builder instance
         */
        Builder withKeySerializer(String keySerializer);

        /**
         * Sets kafka value serializer.
         *
         * @param valueSerializer kafka value serializer
         * @return builder instance
         */
        Builder withValueSerializer(String valueSerializer);

        /**
         * Sets kafka key.
         *
         * @param key kafka key
         * @return builder instance
         */
        Builder withKey(String key);

        /**
         * Sets kafka topic.
         *
         * @param topic kafka topic
         * @return builder instance
         */
        Builder withTopic(String topic);

        /**
         * Sets kafka message codec.
         *
         * @param codec kafka message codec
         * @return builder instance
         */
        Builder withCodec(String codec);

        /**
         * Sets other kafka configuration map.
         *
         * @param configMap kafka configuration map
         * @return builder instance
         */
        Builder withConfigMap(Map<String, Object> configMap);

        /**
         * Creates a kafka telemetry config instance.
         *
         * @return kafka telemetry config instance
         */
        KafkaTelemetryConfig build();
    }
}
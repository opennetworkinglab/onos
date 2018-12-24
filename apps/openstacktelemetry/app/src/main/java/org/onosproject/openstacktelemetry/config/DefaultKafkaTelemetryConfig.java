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
package org.onosproject.openstacktelemetry.config;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import org.onosproject.openstacktelemetry.api.config.KafkaTelemetryConfig;
import org.onosproject.openstacktelemetry.api.config.TelemetryConfig;
import org.onosproject.openstacktelemetry.api.config.TelemetryConfigProperties;

import java.util.Map;
import java.util.Objects;

import static com.google.common.base.MoreObjects.toStringHelper;
import static com.google.common.base.Preconditions.checkNotNull;
import static org.onosproject.openstacktelemetry.api.config.TelemetryConfig.ConfigType.KAFKA;

/**
 * A configuration file contains Kafka telemetry parameters.
 */
public final class DefaultKafkaTelemetryConfig implements KafkaTelemetryConfig {

    protected static final String ADDRESS = "address";
    protected static final String PORT = "port";
    protected static final String RETRIES = "retries";
    protected static final String REQUIRED_ACKS = "requiredAcks";
    protected static final String BATCH_SIZE = "batchSize";
    protected static final String LINGER_MS = "lingerMs";
    protected static final String MEMORY_BUFFER = "memoryBuffer";
    protected static final String KEY_SERIALIZER = "keySerializer";
    protected static final String VALUE_SERIALIZER = "valueSerializer";
    protected static final String KEY = "key";
    protected static final String TOPIC = "topic";
    protected static final String CODEC = "codec";
    protected static final String CONFIG_MAP = "configMap";

    private final String address;
    private final int port;
    private final int retries;
    private final String requiredAcks;
    private final int batchSize;
    private final int lingerMs;
    private final int memoryBuffer;
    private final String keySerializer;
    private final String valueSerializer;
    private final String key;
    private final String topic;
    private final String codec;
    private final Map<String, Object> configMap;

    private DefaultKafkaTelemetryConfig(String address, int port, int retries,
                                        String requiredAcks, int batchSize,
                                        int lingerMs, int memoryBuffer,
                                        String keySerializer,
                                        String valueSerializer,
                                        String key, String topic, String codec,
                                        Map<String, Object> configMap) {
        this.address = address;
        this.port = port;
        this.retries = retries;
        this.requiredAcks = requiredAcks;
        this.batchSize = batchSize;
        this.lingerMs = lingerMs;
        this.memoryBuffer = memoryBuffer;
        this.keySerializer = keySerializer;
        this.valueSerializer = valueSerializer;
        this.key = key;
        this.topic = topic;
        this.codec = codec;
        this.configMap = configMap;
    }

    @Override
    public String address() {
        return address;
    }

    @Override
    public int port() {
        return port;
    }

    @Override
    public int retries() {
        return retries;
    }

    @Override
    public String requiredAcks() {
        return requiredAcks;
    }

    @Override
    public int batchSize() {
        return batchSize;
    }

    @Override
    public int lingerMs() {
        return lingerMs;
    }

    @Override
    public int memoryBuffer() {
        return memoryBuffer;
    }

    @Override
    public String keySerializer() {
        return keySerializer;
    }

    @Override
    public String valueSerializer() {
        return valueSerializer;
    }

    @Override
    public String key() {
        return key;
    }

    @Override
    public String topic() {
        return topic;
    }

    @Override
    public String codec() {
        return codec;
    }

    @Override
    public Map<String, Object> configMap() {
        if (configMap != null) {
            return ImmutableMap.copyOf(configMap);
        } else {
            return Maps.newConcurrentMap();
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj instanceof DefaultKafkaTelemetryConfig) {
            final DefaultKafkaTelemetryConfig other = (DefaultKafkaTelemetryConfig) obj;
            return Objects.equals(this.address, other.address) &&
                    Objects.equals(this.port, other.port) &&
                    Objects.equals(this.retries, other.retries) &&
                    Objects.equals(this.requiredAcks, other.requiredAcks) &&
                    Objects.equals(this.batchSize, other.batchSize) &&
                    Objects.equals(this.lingerMs, other.lingerMs) &&
                    Objects.equals(this.memoryBuffer, other.memoryBuffer) &&
                    Objects.equals(this.keySerializer, other.keySerializer) &&
                    Objects.equals(this.valueSerializer, other.valueSerializer) &&
                    Objects.equals(this.key, other.key) &&
                    Objects.equals(this.topic, other.topic) &&
                    Objects.equals(this.codec, other.codec) &&
                    Objects.equals(this.configMap, other.configMap);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(address, port, retries, requiredAcks, batchSize,
                lingerMs, memoryBuffer, keySerializer, valueSerializer,
                key, topic, codec, configMap);
    }

    @Override
    public String toString() {
        return toStringHelper(this)
                .add(ADDRESS, address)
                .add(PORT, port)
                .add(RETRIES, retries)
                .add(REQUIRED_ACKS, requiredAcks)
                .add(BATCH_SIZE, batchSize)
                .add(LINGER_MS, lingerMs)
                .add(MEMORY_BUFFER, memoryBuffer)
                .add(KEY_SERIALIZER, keySerializer)
                .add(VALUE_SERIALIZER, valueSerializer)
                .add(KEY, key)
                .add(TOPIC, topic)
                .add(CODEC, codec)
                .add(CONFIG_MAP, configMap)
                .toString();
    }

    @Override
    public TelemetryConfigProperties.Builder createBuilder() {
        return new DefaultBuilder();
    }

    /**
     * Builds a kafka telemetry config from telemetry config instance.
     *
     * @param config telemetry config
     * @return kafka telemetry config
     */
    public static KafkaTelemetryConfig fromTelemetryConfig(TelemetryConfig config) {
        if (config.type() != KAFKA) {
            return null;
        }

        int retries = Strings.isNullOrEmpty(config.getProperty(RETRIES)) ? 0 :
                Integer.valueOf(config.getProperty(RETRIES));
        int batchSize = Strings.isNullOrEmpty(config.getProperty(BATCH_SIZE)) ? 0 :
                Integer.valueOf(config.getProperty(BATCH_SIZE));
        int lingerMs = Strings.isNullOrEmpty(config.getProperty(LINGER_MS)) ? 0 :
                Integer.valueOf(config.getProperty(LINGER_MS));
        int memoryBuffer = Strings.isNullOrEmpty(config.getProperty(MEMORY_BUFFER)) ? 0 :
                Integer.valueOf(config.getProperty(MEMORY_BUFFER));

        return new DefaultBuilder()
                .withAddress(config.getProperty(ADDRESS))
                .withPort(Integer.valueOf(config.getProperty(PORT)))
                .withRetries(retries)
                .withRequiredAcks(config.getProperty(REQUIRED_ACKS))
                .withBatchSize(batchSize)
                .withLingerMs(lingerMs)
                .withMemoryBuffer(memoryBuffer)
                .withKeySerializer(config.getProperty(KEY_SERIALIZER))
                .withValueSerializer(config.getProperty(VALUE_SERIALIZER))
                .withKey(config.getProperty(KEY))
                .withTopic(config.getProperty(TOPIC))
                .withCodec(config.getProperty(CODEC))
                .build();
    }

    /**
     * Builder class of DefaultKafkaTelemetryConfig.
     */
    public static final class DefaultBuilder implements Builder {
        private String address;
        private int port;
        private int retries;
        private String requiredAcks;
        private int batchSize;
        private int lingerMs;
        private int memoryBuffer;
        private String keySerializer;
        private String valueSerializer;
        private String key;
        private String topic;
        private String codec;
        private Map<String, Object> configMap;

        @Override
        public Builder withAddress(String address) {
            this.address = address;
            return this;
        }

        @Override
        public Builder withPort(int port) {
            this.port = port;
            return this;
        }

        @Override
        public Builder withRetries(int retries) {
            this.retries = retries;
            return this;
        }

        @Override
        public Builder withRequiredAcks(String requiredAcks) {
            this.requiredAcks = requiredAcks;
            return this;
        }

        @Override
        public Builder withBatchSize(int batchSize) {
            this.batchSize = batchSize;
            return this;
        }

        @Override
        public Builder withLingerMs(int lingerMs) {
            this.lingerMs = lingerMs;
            return this;
        }

        @Override
        public Builder withMemoryBuffer(int memoryBuffer) {
            this.memoryBuffer = memoryBuffer;
            return this;
        }

        @Override
        public Builder withKeySerializer(String keySerializer) {
            this.keySerializer = keySerializer;
            return this;
        }

        @Override
        public Builder withValueSerializer(String valueSerializer) {
            this.valueSerializer = valueSerializer;
            return this;
        }

        @Override
        public Builder withKey(String key) {
            this.key = key;
            return this;
        }

        @Override
        public Builder withTopic(String topic) {
            this.topic = topic;
            return this;
        }

        @Override
        public Builder withCodec(String codec) {
            this.codec = codec;
            return this;
        }

        @Override
        public Builder withConfigMap(Map<String, Object> configMap) {
            this.configMap = configMap;
            return this;
        }

        @Override
        public KafkaTelemetryConfig build() {
            checkNotNull(address, "Kafka server address cannot be null");

            return new DefaultKafkaTelemetryConfig(address, port, retries,
                    requiredAcks, batchSize, lingerMs, memoryBuffer, keySerializer,
                    valueSerializer, key, topic, codec, configMap);
        }
    }
}
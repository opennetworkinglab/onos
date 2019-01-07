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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.common.testing.EqualsTester;
import org.junit.Before;
import org.junit.Test;
import org.onosproject.openstacktelemetry.api.config.KafkaTelemetryConfig;
import org.onosproject.openstacktelemetry.api.config.TelemetryConfig;
import org.onosproject.openstacktelemetry.api.DefaultTelemetryConfig;

import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.onlab.junit.ImmutableClassChecker.assertThatClassIsImmutable;
import static org.onosproject.openstacktelemetry.api.config.TelemetryConfig.ConfigType.KAFKA;
import static org.onosproject.openstacktelemetry.api.config.TelemetryConfig.Status.DISABLED;
import static org.onosproject.openstacktelemetry.config.DefaultKafkaTelemetryConfig.ADDRESS;
import static org.onosproject.openstacktelemetry.config.DefaultKafkaTelemetryConfig.BATCH_SIZE;
import static org.onosproject.openstacktelemetry.config.DefaultKafkaTelemetryConfig.CODEC;
import static org.onosproject.openstacktelemetry.config.DefaultKafkaTelemetryConfig.KEY;
import static org.onosproject.openstacktelemetry.config.DefaultKafkaTelemetryConfig.KEY_SERIALIZER;
import static org.onosproject.openstacktelemetry.config.DefaultKafkaTelemetryConfig.LINGER_MS;
import static org.onosproject.openstacktelemetry.config.DefaultKafkaTelemetryConfig.MEMORY_BUFFER;
import static org.onosproject.openstacktelemetry.config.DefaultKafkaTelemetryConfig.PORT;
import static org.onosproject.openstacktelemetry.config.DefaultKafkaTelemetryConfig.REQUIRED_ACKS;
import static org.onosproject.openstacktelemetry.config.DefaultKafkaTelemetryConfig.RETRIES;
import static org.onosproject.openstacktelemetry.config.DefaultKafkaTelemetryConfig.TOPIC;
import static org.onosproject.openstacktelemetry.config.DefaultKafkaTelemetryConfig.VALUE_SERIALIZER;
import static org.onosproject.openstacktelemetry.config.DefaultKafkaTelemetryConfig.fromTelemetryConfig;

public final class DefaultKafkaTelemetryConfigTest {

    private static final String IP_ADDRESS_1 = "10.10.10.1";
    private static final String IP_ADDRESS_2 = "20.20.20.1";

    private static final int PORT_1 = 80;
    private static final int PORT_2 = 8080;

    private static final int RETRIES_1 = 1;
    private static final int RETRIES_2 = 2;

    private static final int BATCH_SIZE_1 = 100;
    private static final int BATCH_SIZE_2 = 200;

    private static final int MEMORY_BUFFER_1 = 1000;
    private static final int MEMORY_BUFFER_2 = 2000;

    private static final String REQUIRED_ACKS_1 = "all";
    private static final String REQUIRED_ACKS_2 = "none";

    private static final int LINGER_MS_1 = 1;
    private static final int LINGER_MS_2 = 2;

    private static final String KEY_SERIALIZER_1 = "keyserializer1";
    private static final String KEY_SERIALIZER_2 = "keyserializer2";
    private static final String VALUE_SERIALIZER_1 = "valueserializer1";
    private static final String VALUE_SERIALIZER_2 = "valueserializer2";

    private static final String KEY_1 = "key1";
    private static final String KEY_2 = "key2";

    private static final String TOPIC_1 = "topic1";
    private static final String TOPIC_2 = "topic2";

    private static final String CODEC_1 = "codec1";
    private static final String CODEC_2 = "codec2";

    private static final Map<String, Object> CONFIG_MAP_1 =
            ImmutableMap.of("key1", "value1");
    private static final Map<String, Object> CONFIG_MAP_2 =
            ImmutableMap.of("key2", "value2");

    private static final String DUMMY = "dummy";

    private KafkaTelemetryConfig config1;
    private KafkaTelemetryConfig sameAsConfig1;
    private KafkaTelemetryConfig config2;

    /**
     * Initial setup for this unit test.
     */
    @Before
    public void setup() {

        KafkaTelemetryConfig.Builder builder1 =
                new DefaultKafkaTelemetryConfig.DefaultBuilder();
        KafkaTelemetryConfig.Builder builder2 =
                new DefaultKafkaTelemetryConfig.DefaultBuilder();
        KafkaTelemetryConfig.Builder builder3 =
                new DefaultKafkaTelemetryConfig.DefaultBuilder();

        config1 = builder1
                .withAddress(IP_ADDRESS_1)
                .withPort(PORT_1)
                .withRetries(RETRIES_1)
                .withBatchSize(BATCH_SIZE_1)
                .withMemoryBuffer(MEMORY_BUFFER_1)
                .withRequiredAcks(REQUIRED_ACKS_1)
                .withLingerMs(LINGER_MS_1)
                .withKeySerializer(KEY_SERIALIZER_1)
                .withValueSerializer(VALUE_SERIALIZER_1)
                .withKey(KEY_1)
                .withTopic(TOPIC_1)
                .withCodec(CODEC_1)
                .withConfigMap(CONFIG_MAP_1)
                .build();

        sameAsConfig1 = builder2
                .withAddress(IP_ADDRESS_1)
                .withPort(PORT_1)
                .withRetries(RETRIES_1)
                .withBatchSize(BATCH_SIZE_1)
                .withMemoryBuffer(MEMORY_BUFFER_1)
                .withRequiredAcks(REQUIRED_ACKS_1)
                .withLingerMs(LINGER_MS_1)
                .withKeySerializer(KEY_SERIALIZER_1)
                .withValueSerializer(VALUE_SERIALIZER_1)
                .withKey(KEY_1)
                .withTopic(TOPIC_1)
                .withCodec(CODEC_1)
                .withConfigMap(CONFIG_MAP_1)
                .build();

        config2 = builder3
                .withAddress(IP_ADDRESS_2)
                .withPort(PORT_2)
                .withRetries(RETRIES_2)
                .withBatchSize(BATCH_SIZE_2)
                .withMemoryBuffer(MEMORY_BUFFER_2)
                .withRequiredAcks(REQUIRED_ACKS_2)
                .withLingerMs(LINGER_MS_2)
                .withKeySerializer(KEY_SERIALIZER_2)
                .withValueSerializer(VALUE_SERIALIZER_2)
                .withKey(KEY_2)
                .withTopic(TOPIC_2)
                .withCodec(CODEC_2)
                .withConfigMap(CONFIG_MAP_2)
                .build();
    }

    /**
     * Tests class immutability.
     */
    @Test
    public void testImmutability() {
        assertThatClassIsImmutable(DefaultKafkaTelemetryConfig.class);
    }

    /**
     * Tests object equality.
     */
    @Test
    public void testEquality() {
        new EqualsTester()
                .addEqualityGroup(config1, sameAsConfig1)
                .addEqualityGroup(config2).testEquals();
    }

    /**
     * Tests object construction.
     */
    @Test
    public void testConstruction() {
        KafkaTelemetryConfig config = config1;

        assertThat(config.address(), is(IP_ADDRESS_1));
        assertThat(config.port(), is(PORT_1));
        assertThat(config.retries(), is(RETRIES_1));
        assertThat(config.batchSize(), is(BATCH_SIZE_1));
        assertThat(config.memoryBuffer(), is(MEMORY_BUFFER_1));
        assertThat(config.requiredAcks(), is(REQUIRED_ACKS_1));
        assertThat(config.lingerMs(), is(LINGER_MS_1));
        assertThat(config.keySerializer(), is(KEY_SERIALIZER_1));
        assertThat(config.valueSerializer(), is(VALUE_SERIALIZER_1));
        assertThat(config.key(), is(KEY_1));
        assertThat(config.topic(), is(TOPIC_1));
        assertThat(config.codec(), is(CODEC_1));
        assertThat(config.configMap(), is(CONFIG_MAP_1));
    }

    /**
     * Tests props extraction.
     */
    @Test
    public void testPropsExtraction() {
        Map<String, String> props = Maps.newConcurrentMap();
        props.put(ADDRESS, IP_ADDRESS_1);
        props.put(PORT, String.valueOf(PORT_1));
        props.put(RETRIES, String.valueOf(RETRIES_1));
        props.put(BATCH_SIZE, String.valueOf(BATCH_SIZE_1));
        props.put(MEMORY_BUFFER, String.valueOf(MEMORY_BUFFER_1));
        props.put(REQUIRED_ACKS, REQUIRED_ACKS_1);
        props.put(LINGER_MS, String.valueOf(LINGER_MS_1));
        props.put(KEY_SERIALIZER, KEY_SERIALIZER_1);
        props.put(VALUE_SERIALIZER, VALUE_SERIALIZER_1);
        props.put(KEY, KEY_1);
        props.put(TOPIC, TOPIC_1);
        props.put(CODEC, CODEC_1);

        TelemetryConfig config = new DefaultTelemetryConfig(DUMMY, KAFKA,
                ImmutableList.of(), DUMMY, DUMMY, DISABLED, props);

        KafkaTelemetryConfig kafkaConfig = fromTelemetryConfig(config);
        assertThat(kafkaConfig.address(), is(IP_ADDRESS_1));
        assertThat(kafkaConfig.port(), is(PORT_1));
        assertThat(kafkaConfig.retries(), is(RETRIES_1));
        assertThat(kafkaConfig.batchSize(), is(BATCH_SIZE_1));
        assertThat(kafkaConfig.memoryBuffer(), is(MEMORY_BUFFER_1));
        assertThat(kafkaConfig.requiredAcks(), is(REQUIRED_ACKS_1));
        assertThat(kafkaConfig.lingerMs(), is(LINGER_MS_1));
        assertThat(kafkaConfig.keySerializer(), is(KEY_SERIALIZER_1));
        assertThat(kafkaConfig.valueSerializer(), is(VALUE_SERIALIZER_1));
        assertThat(kafkaConfig.key(), is(KEY_1));
        assertThat(kafkaConfig.topic(), is(TOPIC_1));
        assertThat(kafkaConfig.codec(), is(CODEC_1));
    }
}
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
import org.onosproject.openstacktelemetry.api.config.GrpcTelemetryConfig;
import org.onosproject.openstacktelemetry.api.config.TelemetryConfig;
import org.onosproject.openstacktelemetry.api.DefaultTelemetryConfig;

import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.onlab.junit.ImmutableClassChecker.assertThatClassIsImmutable;
import static org.onosproject.openstacktelemetry.api.config.TelemetryConfig.ConfigType.GRPC;
import static org.onosproject.openstacktelemetry.api.config.TelemetryConfig.Status.ENABLED;
import static org.onosproject.openstacktelemetry.config.DefaultGrpcTelemetryConfig.ADDRESS;
import static org.onosproject.openstacktelemetry.config.DefaultGrpcTelemetryConfig.MAX_INBOUND_MSG_SIZE;
import static org.onosproject.openstacktelemetry.config.DefaultGrpcTelemetryConfig.PORT;
import static org.onosproject.openstacktelemetry.config.DefaultGrpcTelemetryConfig.USE_PLAINTEXT;
import static org.onosproject.openstacktelemetry.config.DefaultGrpcTelemetryConfig.fromTelemetryConfig;

/**
 * Unit tests for DefaultGrpcTelemetryConfig class.
 */
public final class DefaultGrpcTelemetryConfigTest {

    private static final String IP_ADDRESS_1 = "10.10.10.1";
    private static final String IP_ADDRESS_2 = "20.20.20.1";

    private static final int PORT_1 = 80;
    private static final int PORT_2 = 8080;

    private static final int MSG_SIZE_1 = 1000;
    private static final int MSG_SIZE_2 = 2000;

    private static final boolean USE_PLAIN_TEXT_1 = true;
    private static final boolean USE_PLAIN_TEXT_2 = true;

    private static final Map<String, Object> CONFIG_MAP_1 =
            ImmutableMap.of("key1", "value1");
    private static final Map<String, Object> CONFIG_MAP_2 =
            ImmutableMap.of("key2", "value2");

    private static final String DUMMY = "dummy";

    private GrpcTelemetryConfig config1;
    private GrpcTelemetryConfig sameAsConfig1;
    private GrpcTelemetryConfig config2;

    /**
     * Initial setup for this unit test.
     */
    @Before
    public void setup() {

        GrpcTelemetryConfig.Builder builder1 =
                new DefaultGrpcTelemetryConfig.DefaultBuilder();
        GrpcTelemetryConfig.Builder builder2 =
                new DefaultGrpcTelemetryConfig.DefaultBuilder();
        GrpcTelemetryConfig.Builder builder3 =
                new DefaultGrpcTelemetryConfig.DefaultBuilder();

        config1 = builder1
                .withAddress(IP_ADDRESS_1)
                .withPort(PORT_1)
                .withMaxInboundMsgSize(MSG_SIZE_1)
                .withUsePlaintext(USE_PLAIN_TEXT_1)
                .withConfigMap(CONFIG_MAP_1)
                .build();

        sameAsConfig1 = builder2
                .withAddress(IP_ADDRESS_1)
                .withPort(PORT_1)
                .withMaxInboundMsgSize(MSG_SIZE_1)
                .withUsePlaintext(USE_PLAIN_TEXT_1)
                .withConfigMap(CONFIG_MAP_1)
                .build();

        config2 = builder3
                .withAddress(IP_ADDRESS_2)
                .withPort(PORT_2)
                .withMaxInboundMsgSize(MSG_SIZE_2)
                .withUsePlaintext(USE_PLAIN_TEXT_2)
                .withConfigMap(CONFIG_MAP_2)
                .build();
    }

    /**
     * Tests class immutability.
     */
    @Test
    public void testImmutability() {
        assertThatClassIsImmutable(DefaultGrpcTelemetryConfig.class);
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
        GrpcTelemetryConfig config = config1;

        assertThat(config.address(), is(IP_ADDRESS_1));
        assertThat(config.port(), is(PORT_1));
        assertThat(config.maxInboundMsgSize(), is(MSG_SIZE_1));
        assertThat(config.usePlaintext(), is(USE_PLAIN_TEXT_1));
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
        props.put(MAX_INBOUND_MSG_SIZE, String.valueOf(MSG_SIZE_1));
        props.put(USE_PLAINTEXT, String.valueOf(USE_PLAIN_TEXT_1));
        TelemetryConfig config = new DefaultTelemetryConfig(DUMMY, GRPC,
                ImmutableList.of(), DUMMY, DUMMY, ENABLED, props);

        GrpcTelemetryConfig grpcConfig = fromTelemetryConfig(config);
        assertThat(grpcConfig.address(), is(IP_ADDRESS_1));
        assertThat(grpcConfig.port(), is(PORT_1));
        assertThat(grpcConfig.maxInboundMsgSize(), is(MSG_SIZE_1));
        assertThat(grpcConfig.usePlaintext(), is(USE_PLAIN_TEXT_1));
    }
}
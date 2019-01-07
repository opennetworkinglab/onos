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
import org.onosproject.openstacktelemetry.api.config.PrometheusTelemetryConfig;
import org.onosproject.openstacktelemetry.api.config.TelemetryConfig;
import org.onosproject.openstacktelemetry.api.DefaultTelemetryConfig;

import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.onlab.junit.ImmutableClassChecker.assertThatClassIsImmutable;
import static org.onosproject.openstacktelemetry.api.config.TelemetryConfig.ConfigType.PROMETHEUS;
import static org.onosproject.openstacktelemetry.api.config.TelemetryConfig.Status.DISABLED;
import static org.onosproject.openstacktelemetry.config.DefaultPrometheusTelemetryConfig.ADDRESS;
import static org.onosproject.openstacktelemetry.config.DefaultPrometheusTelemetryConfig.PORT;
import static org.onosproject.openstacktelemetry.config.DefaultPrometheusTelemetryConfig.fromTelemetryConfig;

/**
 * Unit tests for DefaultPrometheusTelemetryConfig class.
 */
public class DefaultPrometheusTelemetryConfigTest {
    private static final String IP_ADDRESS_1 = "10.10.1.1";
    private static final String IP_ADDRESS_2 = "10.10.1.2";

    private static final int PORT_1 = 50050;
    private static final int PORT_2 = 50051;

    private static final Map<String, Object> CONFIG_MAP_1 =
            ImmutableMap.of("key1", "value1");
    private static final Map<String, Object> CONFIG_MAP_2 =
            ImmutableMap.of("key2", "value2");

    private static final String DUMMY = "dummy";

    private PrometheusTelemetryConfig config1;
    private PrometheusTelemetryConfig sameAsConfig1;
    private PrometheusTelemetryConfig config2;

    /**
     * Initial setup for this unit test.
     */
    @Before
    public void setup() {

        PrometheusTelemetryConfig.Builder builder1 =
                new DefaultPrometheusTelemetryConfig.DefaultBuilder();
        PrometheusTelemetryConfig.Builder builder2 =
                new DefaultPrometheusTelemetryConfig.DefaultBuilder();
        PrometheusTelemetryConfig.Builder builder3 =
                new DefaultPrometheusTelemetryConfig.DefaultBuilder();

        config1 = builder1
                .withAddress(IP_ADDRESS_1)
                .withPort(PORT_1)
                .withConfigMap(CONFIG_MAP_1)
                .build();

        sameAsConfig1 = builder2
                .withAddress(IP_ADDRESS_1)
                .withPort(PORT_1)
                .withConfigMap(CONFIG_MAP_1)
                .build();

        config2 = builder3
                .withAddress(IP_ADDRESS_2)
                .withPort(PORT_2)
                .withConfigMap(CONFIG_MAP_2)
                .build();
    }

    /**
     * Tests class immutability.
     */
    @Test
    public void testImmutability() {
        assertThatClassIsImmutable(DefaultPrometheusTelemetryConfig.class);
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
        PrometheusTelemetryConfig config = config1;

        assertThat(config.address(), is(IP_ADDRESS_1));
        assertThat(config.port(), is(PORT_1));
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

        TelemetryConfig config = new DefaultTelemetryConfig(DUMMY, PROMETHEUS,
                ImmutableList.of(), DUMMY, DUMMY, DISABLED, props);

        PrometheusTelemetryConfig prometheusConfig = fromTelemetryConfig(config);
        assertThat(prometheusConfig.address(), is(IP_ADDRESS_1));
        assertThat(prometheusConfig.port(), is(PORT_1));
    }
}
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
import org.onosproject.openstacktelemetry.api.config.InfluxDbTelemetryConfig;
import org.onosproject.openstacktelemetry.api.config.TelemetryConfig;
import org.onosproject.openstacktelemetry.api.DefaultTelemetryConfig;

import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.onlab.junit.ImmutableClassChecker.assertThatClassIsImmutable;
import static org.onosproject.openstacktelemetry.api.config.TelemetryConfig.ConfigType.INFLUXDB;
import static org.onosproject.openstacktelemetry.api.config.TelemetryConfig.Status.DISABLED;
import static org.onosproject.openstacktelemetry.config.DefaultInfluxDbTelemetryConfig.ADDRESS;
import static org.onosproject.openstacktelemetry.config.DefaultInfluxDbTelemetryConfig.DATABASE;
import static org.onosproject.openstacktelemetry.config.DefaultInfluxDbTelemetryConfig.ENABLE_BATCH;
import static org.onosproject.openstacktelemetry.config.DefaultInfluxDbTelemetryConfig.MEASUREMENT;
import static org.onosproject.openstacktelemetry.config.DefaultInfluxDbTelemetryConfig.PASSWORD;
import static org.onosproject.openstacktelemetry.config.DefaultInfluxDbTelemetryConfig.PORT;
import static org.onosproject.openstacktelemetry.config.DefaultInfluxDbTelemetryConfig.USERNAME;
import static org.onosproject.openstacktelemetry.config.DefaultInfluxDbTelemetryConfig.fromTelemetryConfig;

/**
 * Unit tests for DefaultInfluxDbTelemetryConfig class.
 */
public final class DefaultInfluxDbTelemetryConfigTest {

    private static final String IP_ADDRESS_1 = "10.10.10.1";
    private static final String IP_ADDRESS_2 = "20.20.20.1";

    private static final int PORT_1 = 80;
    private static final int PORT_2 = 8080;

    private static final String DATABASE_1 = "database1";
    private static final String DATABASE_2 = "database2";

    private static final String MEASUREMENT_1 = "measurement1";
    private static final String MEASUREMENT_2 = "measurement2";

    private static final String USERNAME_1 = "username1";
    private static final String USERNAME_2 = "username2";

    private static final String PASSWORD_1 = "password1";
    private static final String PASSWORD_2 = "password2";

    private static final boolean ENABLE_BATCH_1 = true;
    private static final boolean ENABLE_BATCH_2 = false;

    private static final Map<String, Object> CONFIG_MAP_1 =
            ImmutableMap.of("key1", "value1");
    private static final Map<String, Object> CONFIG_MAP_2 =
            ImmutableMap.of("key2", "value2");

    private static final String DUMMY = "dummy";

    private InfluxDbTelemetryConfig config1;
    private InfluxDbTelemetryConfig sameAsConfig1;
    private InfluxDbTelemetryConfig config2;

    /**
     * Initial setup for this unit test.
     */
    @Before
    public void setup() {

        InfluxDbTelemetryConfig.Builder builder1 =
                new DefaultInfluxDbTelemetryConfig.DefaultBuilder();
        InfluxDbTelemetryConfig.Builder builder2 =
                new DefaultInfluxDbTelemetryConfig.DefaultBuilder();
        InfluxDbTelemetryConfig.Builder builder3 =
                new DefaultInfluxDbTelemetryConfig.DefaultBuilder();

        config1 = builder1
                .withAddress(IP_ADDRESS_1)
                .withPort(PORT_1)
                .withDatabase(DATABASE_1)
                .withMeasurement(MEASUREMENT_1)
                .withUsername(USERNAME_1)
                .withPassword(PASSWORD_1)
                .withEnableBatch(ENABLE_BATCH_1)
                .withConfigMap(CONFIG_MAP_1)
                .build();

        sameAsConfig1 = builder2
                .withAddress(IP_ADDRESS_1)
                .withPort(PORT_1)
                .withDatabase(DATABASE_1)
                .withMeasurement(MEASUREMENT_1)
                .withUsername(USERNAME_1)
                .withPassword(PASSWORD_1)
                .withEnableBatch(ENABLE_BATCH_1)
                .withConfigMap(CONFIG_MAP_1)
                .build();

        config2 = builder3
                .withAddress(IP_ADDRESS_2)
                .withPort(PORT_2)
                .withDatabase(DATABASE_2)
                .withMeasurement(MEASUREMENT_2)
                .withUsername(USERNAME_2)
                .withPassword(PASSWORD_2)
                .withEnableBatch(ENABLE_BATCH_2)
                .withConfigMap(CONFIG_MAP_2)
                .build();
    }

    /**
     * Tests class immutability.
     */
    @Test
    public void testImmutability() {
        assertThatClassIsImmutable(DefaultInfluxDbTelemetryConfig.class);
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
        InfluxDbTelemetryConfig config = config1;

        assertThat(config.address(), is(IP_ADDRESS_1));
        assertThat(config.port(), is(PORT_1));
        assertThat(config.database(), is(DATABASE_1));
        assertThat(config.measurement(), is(MEASUREMENT_1));
        assertThat(config.username(), is(USERNAME_1));
        assertThat(config.password(), is(PASSWORD_1));
        assertThat(config.enableBatch(), is(ENABLE_BATCH_1));
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
        props.put(USERNAME, USERNAME_1);
        props.put(PASSWORD, PASSWORD_1);
        props.put(DATABASE, DATABASE_1);
        props.put(ENABLE_BATCH, String.valueOf(ENABLE_BATCH_1));
        props.put(MEASUREMENT, MEASUREMENT_1);

        TelemetryConfig config = new DefaultTelemetryConfig(DUMMY, INFLUXDB,
                ImmutableList.of(), DUMMY, DUMMY, DISABLED, props);

        InfluxDbTelemetryConfig influxDbConfig = fromTelemetryConfig(config);
        assertThat(influxDbConfig.address(), is(IP_ADDRESS_1));
        assertThat(influxDbConfig.port(), is(PORT_1));
        assertThat(influxDbConfig.database(), is(DATABASE_1));
        assertThat(influxDbConfig.measurement(), is(MEASUREMENT_1));
        assertThat(influxDbConfig.username(), is(USERNAME_1));
        assertThat(influxDbConfig.password(), is(PASSWORD_1));
        assertThat(influxDbConfig.enableBatch(), is(ENABLE_BATCH_1));
    }
}
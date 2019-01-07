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
package org.onosproject.openstacktelemetry.api;

import com.google.common.collect.Maps;
import com.google.common.testing.EqualsTester;
import org.junit.Before;
import org.junit.Test;
import org.onosproject.openstacktelemetry.api.config.TelemetryConfig;
import org.onosproject.openstacktelemetry.api.config.TelemetryConfig.ConfigType;

import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.onlab.junit.ImmutableClassChecker.assertThatClassIsImmutable;
import static org.onosproject.openstacktelemetry.api.config.TelemetryConfig.Status.DISABLED;
import static org.onosproject.openstacktelemetry.api.config.TelemetryConfig.Status.ENABLED;

/**
 * Unit tests for DefaultTelemetryConfig class.
 */
public final class DefaultTelemetryConfigTest {

    private static final String NAME_1 = "grpc";
    private static final String NAME_2 = "kafka";

    private static final ConfigType TYPE_1 = ConfigType.GRPC;
    private static final ConfigType TYPE_2 = ConfigType.KAFKA;

    private static final String MANUFACTURER_1 = "grpc.io";
    private static final String MANUFACTURER_2 = "kafka.apache.org";

    private static final String SW_VERSION_1 = "1.0";
    private static final String SW_VERSION_2 = "1.0";

    private static final Map<String, String> PROP_1 = Maps.newConcurrentMap();
    private static final Map<String, String> PROP_2 = Maps.newConcurrentMap();

    private static final String PROP_1_KEY_1 = "key11";
    private static final String PROP_1_KEY_2 = "key12";
    private static final String PROP_1_VALUE_1 = "value11";
    private static final String PROP_1_VALUE_2 = "value12";
    private static final String PROP_2_KEY_1 = "key21";
    private static final String PROP_2_KEY_2 = "key22";
    private static final String PROP_2_VALUE_1 = "value21";
    private static final String PROP_2_VALUE_2 = "value22";

    private static final TelemetryConfig.Status STATUS_1 = ENABLED;
    private static final TelemetryConfig.Status STATUS_2 = DISABLED;

    private TelemetryConfig config1;
    private TelemetryConfig sameAsConfig1;
    private TelemetryConfig config2;

    /**
     * Initial setup for this unit test.
     */
    @Before
    public void setup() {
        PROP_1.put(PROP_1_KEY_1, PROP_1_VALUE_1);
        PROP_1.put(PROP_1_KEY_2, PROP_1_VALUE_2);
        PROP_2.put(PROP_2_KEY_1, PROP_2_VALUE_1);
        PROP_2.put(PROP_2_KEY_2, PROP_2_VALUE_2);

        config1 = new DefaultTelemetryConfig(NAME_1, TYPE_1, null,
                MANUFACTURER_1, SW_VERSION_1, STATUS_1, PROP_1);
        sameAsConfig1 = new DefaultTelemetryConfig(NAME_1, TYPE_1, null,
                MANUFACTURER_1, SW_VERSION_1, STATUS_1, PROP_1);
        config2 = new DefaultTelemetryConfig(NAME_2, TYPE_2, null,
                MANUFACTURER_2, SW_VERSION_2, STATUS_2, PROP_2);
    }

    /**
     * Tests class immutability.
     */
    @Test
    public void testImmutability() {
        assertThatClassIsImmutable(DefaultTelemetryConfig.class);
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
        TelemetryConfig config = config1;

        assertEquals(config.name(), NAME_1);
        assertEquals(config.type(), TYPE_1);
        assertEquals(config.manufacturer(), MANUFACTURER_1);
        assertEquals(config.swVersion(), SW_VERSION_1);
        assertEquals(config.properties(), PROP_1);
        assertEquals(config.status(), STATUS_1);
    }
}

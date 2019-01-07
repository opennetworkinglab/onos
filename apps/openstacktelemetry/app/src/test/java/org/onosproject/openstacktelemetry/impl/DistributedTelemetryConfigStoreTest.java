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
package org.onosproject.openstacktelemetry.impl;

import com.google.common.collect.Maps;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onosproject.TestApplicationId;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreServiceAdapter;
import org.onosproject.openstacktelemetry.api.DefaultTelemetryConfig;
import org.onosproject.openstacktelemetry.api.config.TelemetryConfig;
import org.onosproject.openstacktelemetry.api.config.TelemetryConfig.ConfigType;
import org.onosproject.store.service.TestStorageService;

import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.onosproject.openstacktelemetry.api.config.TelemetryConfig.Status.DISABLED;
import static org.onosproject.openstacktelemetry.api.config.TelemetryConfig.Status.ENABLED;

/**
 * Distributed TelemetryConfig store test suite.
 */
public class DistributedTelemetryConfigStoreTest {

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
    private TelemetryConfig config2;

    private DistributedTelemetryConfigStore configStore;

    /**
     * Sets up the telemetry config store and the storage service test harness.
     */
    @Before
    public void setUp() {
        configStore = new DistributedTelemetryConfigStore();
        configStore.storageService = new TestStorageService();
        configStore.coreService = new TestCoreService();
        configStore.setDelegate(event -> {
        });
        configStore.activate();

        initTelemetryConfigs();
    }

    /**
     * Tears down the telemetry config store.
     */
    @After
    public void tearDown() {
        configStore.deactivate();
    }

    /**
     * Tests adding, removing and getting.
     */
    @Test
    public void basics() {
        configStore.createTelemetryConfig(config1);
        assertTrue("There should be one telemetry config in the set.",
                configStore.telemetryConfigs().contains(config1));
        assertTrue("The same telemetry config should be returned.",
                configStore.telemetryConfigsByType(ConfigType.GRPC).contains(config1));
        assertEquals("The telemetry config should be the same.",
                configStore.telemetryConfig(NAME_1), config1);
        configStore.removeTelemetryConfig(NAME_1);
        assertFalse("There should be no telemetry config in the set.",
                configStore.telemetryConfigs().contains(config1));

        configStore.createTelemetryConfig(config1);
        configStore.createTelemetryConfig(config2);
        assertEquals("There should be two configs in the sets.",
                configStore.telemetryConfigs().size(), 2);
    }

    /**
     * Test core service; For generate test application ID.
     */
    public class TestCoreService extends CoreServiceAdapter {
        @Override
        public ApplicationId registerApplication(String name) {
            return TestApplicationId.create(name);
        }
    }

    private void initTelemetryConfigs() {
        PROP_1.put(PROP_1_KEY_1, PROP_1_VALUE_1);
        PROP_1.put(PROP_1_KEY_2, PROP_1_VALUE_2);
        PROP_2.put(PROP_2_KEY_1, PROP_2_VALUE_1);
        PROP_2.put(PROP_2_KEY_2, PROP_2_VALUE_2);

        config1 = new DefaultTelemetryConfig(NAME_1, TYPE_1, null,
                MANUFACTURER_1, SW_VERSION_1, STATUS_1, PROP_1);
        config2 = new DefaultTelemetryConfig(NAME_2, TYPE_2, null,
                MANUFACTURER_2, SW_VERSION_2, STATUS_2, PROP_2);
    }
}

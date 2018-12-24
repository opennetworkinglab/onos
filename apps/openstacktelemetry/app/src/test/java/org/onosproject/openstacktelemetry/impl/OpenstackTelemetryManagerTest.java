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

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onosproject.openstacktelemetry.api.TelemetryAdminService;

import static org.junit.Assert.assertEquals;

/**
 * Unit tests for OpenstackTelemetryService class.
 */
public final class OpenstackTelemetryManagerTest {

    private static final TelemetryAdminService GRPC_SERVICE = new GrpcTelemetryManager();
    private static final TelemetryAdminService INFLUXDB_SERVICE = new InfluxDbTelemetryManager();
    private static final TelemetryAdminService KAFKA_SERVICE = new KafkaTelemetryManager();
    private static final TelemetryAdminService PROMETHEUS_SERVICE = new PrometheusTelemetryManager();
    private static final TelemetryAdminService REST_SERVICE = new PrometheusTelemetryManager();

    private OpenstackTelemetryManager manager;

    /**
     * Initializes the unit test.
     */
    @Before
    public void setUp() {
        manager = new OpenstackTelemetryManager();

        manager.telemetryConfigService = new TelemetryConfigManager();

        manager.activate();
    }

    /**
     * Tests addTelemetryService method.
     */
    @Test
    public void testAddTelemetryService() {
        addDefaultServices();

        TelemetryAdminService kafkaService = new KafkaTelemetryManager();

        assertEquals(5, manager.telemetryServices().size());

        manager.addTelemetryService(kafkaService);

        assertEquals(6, manager.telemetryServices().size());
    }

    /**
     * Tests removeTelemetryService method.
     */
    @Test
    public void testRemoveTelemetryService() {
        addDefaultServices();

        assertEquals(5, manager.telemetryServices().size());

        manager.removeTelemetryService(GRPC_SERVICE);

        assertEquals(4, manager.telemetryServices().size());
    }

    /**
     * Tears down the unit test.
     */
    @After
    public void tearDown() {
        manager.deactivate();
    }

    private void addDefaultServices() {
        manager.addTelemetryService(GRPC_SERVICE);
        manager.addTelemetryService(INFLUXDB_SERVICE);
        manager.addTelemetryService(KAFKA_SERVICE);
        manager.addTelemetryService(PROMETHEUS_SERVICE);
        manager.addTelemetryService(REST_SERVICE);
    }
}

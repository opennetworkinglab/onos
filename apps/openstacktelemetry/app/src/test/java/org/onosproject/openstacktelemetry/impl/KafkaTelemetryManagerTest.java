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

import org.junit.Test;
import org.onlab.junit.TestUtils;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for kafka telemetry manager.
 */
public final class KafkaTelemetryManagerTest {

    private KafkaTelemetryManager manager;
    private OpenstackTelemetryServiceAdapter telemetryService =
            new OpenstackTelemetryServiceAdapter();

    /**
     * Tests codec register activation and deactivation.
     */
    @Test
    public void testActivateDeactivate() {
        manager = new KafkaTelemetryManager();

        TestUtils.setField(manager, "openstackTelemetryService", telemetryService);

        manager.activate();

        assertTrue(telemetryService.services.contains(manager));

        manager.deactivate();

        assertFalse(telemetryService.services.contains(manager));
    }
}

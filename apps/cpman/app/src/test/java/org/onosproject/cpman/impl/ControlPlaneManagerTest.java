/*
 * Copyright 2016-present Open Networking Foundation
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
package org.onosproject.cpman.impl;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onosproject.core.CoreServiceAdapter;
import org.onosproject.cpman.impl.message.ControlMessageServiceAdaptor;
import org.onosproject.cpman.impl.message.ControlPlaneMonitorServiceAdaptor;

/**
 * Set of tests of the ONOS application component.
 */
public class ControlPlaneManagerTest {

    private ControlPlaneManager cpMan;

    /**
     * Sets up the services required by the CPMan application.
     */
    @Before
    public void setUp() {
        cpMan = new ControlPlaneManager();
        cpMan.coreService = new CoreServiceAdapter();
        cpMan.messageService = new ControlMessageServiceAdaptor();
        cpMan.monitorService = new ControlPlaneMonitorServiceAdaptor();
        cpMan.activate();
    }

    /**
     * Tears down the CPMan application.
     */
    @After
    public void tearDown() {
        cpMan.deactivate();
    }

    /**
     * Tests the control metric aggregating function.
     *
     * @throws Exception if metric collection fails.
     */
    @Test
    public void testMetricsAggregation() throws Exception {
    }

    /**
     * Tests the control metric collecting function.
     *
     * @throws Exception
     */
    @Test
    public void testMetricsCollection() throws Exception {
    }
}
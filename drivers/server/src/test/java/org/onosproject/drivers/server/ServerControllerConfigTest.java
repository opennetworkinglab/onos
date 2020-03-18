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

package org.onosproject.drivers.server;

import org.onosproject.net.DeviceId;
import org.onosproject.net.behaviour.ControllerConfig;
import org.onosproject.net.behaviour.ControllerInfo;
import org.onosproject.net.driver.Behaviour;
import org.onosproject.net.driver.DefaultDriverData;
import org.onosproject.net.driver.Driver;
import org.onosproject.net.driver.DriverData;
import org.onosproject.net.driver.DriverHandler;
import org.onosproject.net.driver.DriverService;
import org.onosproject.net.driver.DriverServiceAdapter;
import org.onosproject.mastership.MastershipServiceAdapter;
import org.onosproject.mastership.MastershipService;
import org.onosproject.protocol.rest.RestSBController;
import org.onosproject.protocol.rest.RestSBDevice;

import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;

/**
 * Unit tests for methods of ServerControllerConfig.
 */
public class ServerControllerConfigTest {

    // Controller information used during the tests
    private static List<ControllerInfo> controllers;

    // Device used during the tests
    private DeviceId restDeviceId1;
    private RestSBDevice restDevice1;

    // Test Driver service used during the tests
    private DriverService driverService;

    // Test mastership service used during the tests
    private MastershipService mastershipService;

    // Test Rest SB controller used during the tests
    private RestSBController controller;

    @Before
    public void setUp() throws Exception {
        restDeviceId1 = TestConfig.REST_DEV_ID1;
        assertThat(restDeviceId1, notNullValue());

        restDevice1 = TestConfig.REST_DEV1;
        assertThat(restDevice1, notNullValue());

        controllers = TestConfig.CONTROLLERS;
        assertThat(controllers, notNullValue());

        driverService = new TestDriverService();
        mastershipService = new TestMastershipService();
        controller = new RestSBControllerMock();
    }

    /**
     * Test of setControllers().
     */
    @Test
    public void testSetControllers() {
        // Get device handler
        DriverHandler driverHandler = null;
        try {
            driverHandler = driverService.createHandler(restDeviceId1);
        } catch (Exception e) {
            throw e;
        }
        assertThat(driverHandler, notNullValue());

        // TODO: Fix this test
    }

    /**
     * Test of getControllers().
     */
    @Test
    public void testGetControllers() {
        // Get device handler
        DriverHandler driverHandler = null;
        try {
            driverHandler = driverService.createHandler(restDeviceId1);
        } catch (Exception e) {
            throw e;
        }
        assertThat(driverHandler, notNullValue());

        // Ask for the controllers of this device
        List<ControllerInfo> receivedControllers = null;

        // TODO: Fix this test
    }

    /**
     * Test class for driver handler.
     */
    private class TestDriverHandler implements DriverHandler {

        private final DeviceId deviceId;

        TestDriverHandler(DeviceId deviceId) {
            this.deviceId = deviceId;
        }

        @Override
        public Driver driver() {
            return null;
        }

        @Override
        public DriverData data() {
            // Just create a fake driver data
            return new DefaultDriverData(null, deviceId);
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T extends Behaviour> T behaviour(Class<T> behaviourClass) {
            // Let's create the behavior
            ControllerConfig controllerConfing = null;
            try {
                controllerConfing = new ServerControllerConfig();
            } catch (Exception e) {
                // Do nothing
            }

            if (controllerConfing == null) {
                return null;
            }

            // Set the handler
            controllerConfing.setHandler(this);

            // Done, return the behavior
            return (T) controllerConfing;
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> T get(Class<T> serviceClass) {
            if (serviceClass == MastershipService.class) {
                return (T) mastershipService;
            } else if (serviceClass == RestSBController.class) {
                return (T) controller;
            }
            return null;
        }

        @Override
        public boolean hasBehaviour(Class<? extends Behaviour> behaviourClass) {
            return true;
        }
    }

    /**
     * Test class for driver service.
     */
    private class TestDriverService extends DriverServiceAdapter {
        @Override
        public DriverHandler createHandler(DeviceId deviceId, String... credentials) {
            return new TestDriverHandler(deviceId);
        }

    }

    /**
     * Test class for mastership service.
     */
    private final class TestMastershipService extends MastershipServiceAdapter {
        @Override
        public boolean isLocalMaster(DeviceId deviceId) {
            return true;
        }
    }

}

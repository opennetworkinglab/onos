/*
 * Copyright 2015 Open Networking Laboratory
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

package org.onosproject.driver.ovsdb;

import com.google.common.collect.ImmutableMap;
import org.junit.Before;
import org.junit.Test;
import org.onosproject.net.DeviceId;
import org.onosproject.net.behaviour.ControllerConfig;
import org.onosproject.net.device.DeviceServiceAdapter;
import org.onosproject.net.driver.DefaultDriver;
import org.onosproject.net.driver.DefaultDriverData;
import org.onosproject.net.driver.DefaultDriverHandler;
import org.onosproject.ovsdb.controller.driver.OvsdbClientServiceAdapter;
import org.onosproject.ovsdb.controller.driver.OvsdbControllerAdapter;

/**
 * Created by Andrea on 10/7/15.
 */
public class OvsdbControllerConfigTest {


    private static final DeviceId DEVICE_ID = DeviceId.deviceId("foo");

    private DefaultDriver ddc;
    private DefaultDriverData data;
    private DefaultDriverHandler handler;

    private TestDeviceService deviceService = new TestDeviceService();
    private TestOvsdbController controller = new TestOvsdbController();
    private TestOvsdbClient client = new TestOvsdbClient();

    private OvsdbControllerConfig controllerConfig;


    @Before
    public void setUp() {
        controllerConfig = new OvsdbControllerConfig();

        ddc = new DefaultDriver("foo.bar", null, "Circus", "lux", "1.2a",
                                ImmutableMap.of(ControllerConfig.class,
                                                OvsdbControllerConfig.class),
                                ImmutableMap.of("foo", "bar"));
        data = new DefaultDriverData(ddc, DEVICE_ID);
        handler = new DefaultDriverHandler(data);
        //handler.controllerConfig.setHandler(handler);
        //TODO setTestService directory on handler
        //TODO setup ovsdb fake controller with fake ovsdbclient
        //TODO setup fake device service
    }

    @Test
    public void testGetControllers() throws Exception {
//        DriverService driverService = new Driv
//        AbstractBehaviour ab = new AbstractBehaviour();
//        DriverHandler handler = handler();
//        List<ControllerInfo> controllersList =
//              controllerConfig.getControllers(DeviceId.deviceId("0000000000000018"));
//        log.info("controllers " + controllersList);

    }

    @Test
    public void testSetControllers() throws Exception {

    }


    private class TestDeviceService extends DeviceServiceAdapter {

    }

    private class TestOvsdbController extends OvsdbControllerAdapter {


    }

    private class TestOvsdbClient extends OvsdbClientServiceAdapter {

    }
}
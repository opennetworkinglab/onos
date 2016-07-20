/*
 * Copyright 2016-present Open Networking Laboratory
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

package org.onosproject.protocol.rest.ctl;

import org.junit.Before;
import org.junit.Test;
import org.onlab.packet.IpAddress;
import org.onosproject.protocol.rest.DefaultRestSBDevice;
import org.onosproject.protocol.rest.RestSBDevice;

import static org.junit.Assert.*;

/**
 * Basic testing for RestSBController.
 */
public class RestSBControllerImplTest {

    RestSBControllerImpl controller;

    RestSBDevice device1;
    RestSBDevice device2;


    @Before
    public void setUp() {
        controller = new RestSBControllerImpl();
        controller.activate();
        device1 = new DefaultRestSBDevice(IpAddress.valueOf("127.0.0.1"), 8080, "foo", "bar", "http", null, true);
        device2 = new DefaultRestSBDevice(IpAddress.valueOf("127.0.0.2"), 8080, "foo1", "bar2", "http", null, true);
        controller.addDevice(device1);
    }

    @Test
    public void basics() {
        assertTrue("Device1 non added", controller.getDevices().containsValue(device1));
        assertEquals("Device1 added but with wrong key", controller.getDevices()
                .get(device1.deviceId()), device1);
        assertEquals("Incorrect Get Device by ID", controller.getDevice(device1.deviceId()), device1);
        assertEquals("Incorrect Get Device by IP, Port", controller.getDevice(device1.ip(), device1.port()), device1);
        controller.addDevice(device2);
        assertTrue("Device2 non added", controller.getDevices().containsValue(device2));
        controller.removeDevice(device2.deviceId());
        assertFalse("Device2 not removed", controller.getDevices().containsValue(device2));
    }
}
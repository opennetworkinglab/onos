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

package org.onosproject.protocol.restconf.ctl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.junit.Before;
import org.junit.Test;
import org.onlab.packet.IpAddress;
import org.onosproject.protocol.rest.DefaultRestSBDevice;
import org.onosproject.protocol.rest.RestSBDevice;

/**
 * Basic testing for RestSBController.
 */
public class RestConfSBControllerImplTest {

    RestConfSBControllerImpl restConfController;

    RestSBDevice device3;

    ExecutorService executor = Executors.newSingleThreadExecutor();

    @Before
    public void setUp() {
        restConfController = new RestConfSBControllerImpl();
        restConfController.activate();
        device3 = new DefaultRestSBDevice(IpAddress.valueOf("127.0.0.1"), 8181,
                                          "", "", "http", null, true);
        restConfController.addDevice(device3);

    }

    @Test
    public void basics() {
        assertTrue("Device3 non added",
                   restConfController.getDevices().containsValue(device3));
        assertEquals("Device3 added but with wrong key",
                     restConfController.getDevices().get(device3.deviceId()),
                     device3);
        assertEquals("Incorrect Get Device by ID",
                     restConfController.getDevice(device3.deviceId()), device3);
        assertEquals("Incorrect Get Device by IP, Port",
                     restConfController.getDevice(device3.ip(), device3.port()),
                     device3);
        restConfController.removeDevice(device3.deviceId());
        assertFalse("Device3 not removed",
                    restConfController.getDevices().containsValue(device3));
    }
}

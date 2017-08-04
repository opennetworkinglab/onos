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

package org.onosproject.net.config.basics;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import org.junit.Before;
import org.junit.Test;
import org.onosproject.net.key.DeviceKeyId;
import org.onosproject.net.DeviceId;
import org.onosproject.net.config.ConfigApplyDelegate;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.onosproject.net.Device.Type.OTN;
import static org.onosproject.net.Device.Type.SWITCH;

/**
 * Test class for BasicDeviceConfig.
 */
public class BasicDeviceConfigTest {

    private static final String DRIVER = "fooDriver";
    private static final String MANUFACTURER = "fooManufacturer";
    private static final String HW_VERSION = "0.0";
    private static final String SW_VERSION = "0.0";
    private static final String SERIAL = "1234";
    private static final String MANAGEMENT_ADDRESS = "12.34.56.78:99";
    private static final DeviceKeyId DEVICE_KEY_ID = DeviceKeyId.deviceKeyId("fooDeviceKeyId");
    private static final String DRIVER_NEW = "barDriver";
    private static final String MANUFACTURER_NEW = "barManufacturer";
    private static final String HW_VERSION_NEW = "1.1";
    private static final String SW_VERSION_NEW = "1.1";
    private static final String SERIAL_NEW = "5678";
    private static final String MANAGEMENT_ADDRESS_NEW = "99.87.65.43:12";
    private static final DeviceKeyId DEVICE_KEY_ID_NEW = DeviceKeyId.deviceKeyId("barDeviceKeyId");

    private static final String NAME1 = "fooProtocol:fooIP:fooPort";

    private final ConfigApplyDelegate delegate = config -> {
    };
    private final ObjectMapper mapper = new ObjectMapper();

    private static final BasicDeviceConfig SW_BDC = new BasicDeviceConfig();

    @Before
    public void setUp() {
        SW_BDC.init(DeviceId.deviceId(NAME1), NAME1, JsonNodeFactory.instance.objectNode(), mapper, delegate);
        SW_BDC.type(SWITCH).manufacturer(MANUFACTURER).hwVersion(HW_VERSION)
                .swVersion(SW_VERSION).serial(SERIAL).managementAddress(MANAGEMENT_ADDRESS).driver(DRIVER)
                .deviceKeyId(DEVICE_KEY_ID);
    }

    @Test
    public void testCorrectConfiguration() {
        assertTrue("Configuration contains not valid fields", SW_BDC.isValid());
        assertEquals("Incorrect type", SWITCH, SW_BDC.type());
        assertEquals("Incorrect driver", DRIVER, SW_BDC.driver());
        assertEquals("Incorrect manufacturer", MANUFACTURER, SW_BDC.manufacturer());
        assertEquals("Incorrect HwVersion", HW_VERSION, SW_BDC.hwVersion());
        assertEquals("Incorrect swVersion", SW_VERSION, SW_BDC.swVersion());
        assertEquals("Incorrect serial", SERIAL, SW_BDC.serial());
        assertEquals("Incorrect management Address", MANAGEMENT_ADDRESS, SW_BDC.managementAddress());
        assertEquals("Incorrect deviceKeyId", DEVICE_KEY_ID, SW_BDC.deviceKeyId());
    }


    @Test
    public void testSetType() {
        SW_BDC.type(OTN);
        assertEquals("Incorrect type", OTN, SW_BDC.type());
    }


    @Test
    public void testSetDriver() {
        SW_BDC.driver(DRIVER_NEW);
        assertEquals("Incorrect driver", DRIVER_NEW, SW_BDC.driver());
    }


    @Test
    public void testSetManufacturer() {
        SW_BDC.manufacturer(MANUFACTURER_NEW);
        assertEquals("Incorrect manufacturer", MANUFACTURER_NEW, SW_BDC.manufacturer());
    }


    @Test
    public void testSetHwVersion() {
        SW_BDC.hwVersion(HW_VERSION_NEW);
        assertEquals("Incorrect HwVersion", HW_VERSION_NEW, SW_BDC.hwVersion());
    }


    @Test
    public void testSetSwVersion() {
        SW_BDC.swVersion(SW_VERSION_NEW);
        assertEquals("Incorrect swVersion", SW_VERSION_NEW, SW_BDC.swVersion());
    }

    @Test
    public void testSetSerial() {
        SW_BDC.serial(SERIAL_NEW);
        assertEquals("Incorrect serial", SERIAL_NEW, SW_BDC.serial());
    }

    @Test
    public void testSetManagementAddress() {
        SW_BDC.managementAddress(MANAGEMENT_ADDRESS_NEW);
        assertEquals("Incorrect managementAddress", MANAGEMENT_ADDRESS_NEW, SW_BDC.managementAddress());
    }

    @Test
    public void testSetDeviceKeyId() {
        // change device key id
        SW_BDC.deviceKeyId(DEVICE_KEY_ID_NEW);
        assertEquals("Incorrect deviceKeyId", DEVICE_KEY_ID_NEW, SW_BDC.deviceKeyId());
        // clear device key id
        SW_BDC.deviceKeyId(null);
        assertEquals("Incorrect deviceKeyId", null, SW_BDC.deviceKeyId());
    }
}
/*
 * Copyright 2015-present Open Networking Foundation
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
package org.onosproject.net.device.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import org.junit.Before;
import org.junit.Test;
import org.onlab.packet.ChassisId;
import org.onosproject.net.AnnotationKeys;
import org.onosproject.net.DefaultAnnotations;
import org.onosproject.net.DeviceId;
import org.onosproject.net.SparseAnnotations;
import org.onosproject.net.config.ConfigApplyDelegate;
import org.onosproject.net.config.basics.BasicDeviceConfig;
import org.onosproject.net.device.DefaultDeviceDescription;
import org.onosproject.net.device.DeviceDescription;

import java.net.URI;

import static org.junit.Assert.assertEquals;
import static org.onosproject.net.Device.Type.ROADM;
import static org.onosproject.net.Device.Type.SWITCH;
import static org.onosproject.net.Device.Type.OTN;

public class BasicDeviceOperatorTest {

    private static final String NAME1 = "of:foo";
    private static final String NAME2 = "of:bar";
    private static final String NAME3 = "of:otn";
    private static final String OWNER = "somebody";
    private static final URI DURI = URI.create(NAME1);
    private static final String MFR = "whitebox";
    private static final String HW = "1.1.x";
    private static final String SW = "3.9.1";
    private static final String SN = "43311-12345";
    private static final ChassisId CID = new ChassisId();
    private static final String DRIVER = "fooDriver";
    private static final String MANUFACTURER = "fooManufacturer";
    private static final String HW_VERSION = "0.0";
    private static final String SW_VERSION = "0.0";
    private static final String SERIAL = "1234";
    private static final String MANAGEMENT_ADDRESS = "12.34.56.78:99";

    private static final SparseAnnotations SA = DefaultAnnotations.builder()
            .set(AnnotationKeys.DRIVER, NAME2).build();

    private static final DeviceDescription DEV1 = new DefaultDeviceDescription(
            DURI, SWITCH, MFR, HW, SW, SN, CID, SA);

    private final ConfigApplyDelegate delegate = config -> { };
    private final ObjectMapper mapper = new ObjectMapper();

    private static final BasicDeviceConfig SW_BDC = new BasicDeviceConfig();
    private static final BasicDeviceConfig RD_BDC = new BasicDeviceConfig();
    private static final BasicDeviceConfig OT_BDC = new BasicDeviceConfig();

    @Before
    public void setUp() {
        SW_BDC.init(DeviceId.deviceId(NAME1), NAME1, JsonNodeFactory.instance.objectNode(), mapper, delegate);
        SW_BDC.type(SWITCH).driver(NAME1).owner(OWNER);
        RD_BDC.init(DeviceId.deviceId(NAME2), NAME2, JsonNodeFactory.instance.objectNode(), mapper, delegate);
        RD_BDC.type(ROADM).manufacturer(MANUFACTURER).hwVersion(HW_VERSION)
                .swVersion(SW_VERSION).serial(SERIAL).managementAddress(MANAGEMENT_ADDRESS).driver(DRIVER).owner(OWNER);
        OT_BDC.init(DeviceId.deviceId(NAME3), NAME3, JsonNodeFactory.instance.objectNode(), mapper, delegate);
        OT_BDC.type(OTN);
    }

    @Test
    public void testDescOps() {
        DeviceDescription desc = BasicDeviceOperator.combine(null, DEV1);
        assertEquals(desc, DEV1);

        // override driver name
        desc = BasicDeviceOperator.combine(SW_BDC, DEV1);
        assertEquals(NAME1, desc.annotations().value(AnnotationKeys.DRIVER));

        // override Device Information
        desc = BasicDeviceOperator.combine(RD_BDC, DEV1);
        assertEquals("Wrong type", ROADM, desc.type());
        assertEquals("Wrong manufacturer", MANUFACTURER, desc.manufacturer());
        assertEquals("Wrong HwVersion", HW_VERSION, desc.hwVersion());
        assertEquals("Wrong swVersion", SW_VERSION, desc.swVersion());
        assertEquals("Wrong serial", SERIAL, desc.serialNumber());
        assertEquals("Wrong management Address", MANAGEMENT_ADDRESS,
                     desc.annotations().value(AnnotationKeys.MANAGEMENT_ADDRESS));

        // override Device type
        desc = BasicDeviceOperator.combine(OT_BDC, DEV1);
        assertEquals(OTN, desc.type());
    }
}

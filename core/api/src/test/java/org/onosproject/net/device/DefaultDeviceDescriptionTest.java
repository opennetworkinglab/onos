/*
 * Copyright 2014-present Open Networking Laboratory
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
package org.onosproject.net.device;

import org.junit.Test;
import org.onlab.packet.ChassisId;
import org.onosproject.net.DefaultAnnotations;
import org.onosproject.net.Device;

import java.net.URI;

import com.google.common.testing.EqualsTester;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.onlab.junit.ImmutableClassChecker.assertThatClassIsImmutableBaseClass;
import static org.onosproject.net.Device.Type.ROADM;
import static org.onosproject.net.Device.Type.ROUTER;
import static org.onosproject.net.Device.Type.SWITCH;

/**
 * Test of the default device description.
 */
public class DefaultDeviceDescriptionTest {


    private static final URI DURI = URI.create("of:foo");
    private static final String MFR = "whitebox";
    private static final String HW = "1.1.x";
    private static final String SW = "3.9.1";
    private static final String SN = "43311-12345";
    private static final ChassisId CID = new ChassisId();
    private static final DefaultAnnotations DA =
            DefaultAnnotations.builder().set("Key", "Value").build();

    private static final URI DURI2 = URI.create("of:foo2");
    private static final String MFR2 = "whitebox2";
    private static final String HW2 = "1.1.x2";
    private static final String SW2 = "3.9.12";
    private static final String SN2 = "43311-123452";
    private static final ChassisId CID2 = new ChassisId(2);
    private static final DefaultAnnotations DA2 =
            DefaultAnnotations.builder().set("2ndKey", "2ndValue").build();

    private void checkValues(DeviceDescription device,
                             URI uri,
                             Device.Type type,
                             String manufacturer,
                             String hw,
                             String sw,
                             String serial,
                             String containsUri,
                             long chassisId,
                             String annotationsString,
                             boolean defaultAvailable) {
        assertEquals("incorrect uri", uri, device.deviceUri());
        assertEquals("incorrect type", type, device.type());
        assertEquals("incorrect manufacturer", device.manufacturer(), manufacturer);
        assertEquals("incorrect hw", device.hwVersion(), hw);
        assertEquals("incorrect sw", device.swVersion(), sw);
        assertEquals("incorrect serial", device.serialNumber(), serial);
        assertTrue("incorrect toString", device.toString().contains(containsUri));
        assertTrue("Incorrect chassis",
                   device.chassisId() == null ? chassisId == 0 :
                                                chassisId == device.chassisId().value());
        assertTrue("incorrect annotations", device.toString().contains(annotationsString));
        assertEquals("incorrect default available", defaultAvailable, device.isDefaultAvailable());
    }

    @Test
    public void basics() {
        DeviceDescription device =
                new DefaultDeviceDescription(DURI, SWITCH, MFR, HW, SW, SN, CID, DA);
        checkValues(device, DURI, SWITCH, MFR, HW, SW, SN, "uri=of:foo",
                    CID.value(), "Key=Value", true);
    }

    /**
     * Tests equals, hashCode, and toString.
     */
    @Test
    public void testEquals() {
        DeviceDescription device1 =
                new DefaultDeviceDescription(DURI, SWITCH, MFR, HW, SW, SN, CID, DA);
        DeviceDescription sameAsDevice1 =
                new DefaultDeviceDescription(DURI, SWITCH, MFR, HW, SW, SN, CID, DA);
        DeviceDescription device2 =
                new DefaultDeviceDescription(DURI2, SWITCH, MFR, HW, SW, SN, CID, DA);
        DeviceDescription device3 =
                new DefaultDeviceDescription(DURI, ROUTER, MFR2, HW, SW, SN, CID, DA);
        DeviceDescription device4 =
                new DefaultDeviceDescription(DURI, SWITCH, MFR, HW2, SW, SN, CID, DA);
        DeviceDescription device5 =
                new DefaultDeviceDescription(DURI, SWITCH, MFR, HW, SW2, SN, CID, DA);
        DeviceDescription device6 =
                new DefaultDeviceDescription(DURI, SWITCH, MFR, HW, SW, SN2, CID, DA);
        DeviceDescription device7 =
                new DefaultDeviceDescription(DURI, SWITCH, MFR, HW, SW, SN, CID2, DA);
        DeviceDescription device8 =
                new DefaultDeviceDescription(DURI, SWITCH, MFR, HW, SW, SN, CID, DA2);

        new EqualsTester()
                .addEqualityGroup(device1, sameAsDevice1)
                .addEqualityGroup(device2)
                .addEqualityGroup(device3)
                .addEqualityGroup(device4)
                .addEqualityGroup(device5)
                .addEqualityGroup(device6)
                .addEqualityGroup(device7)
                .addEqualityGroup(device8)
                .testEquals();
    }

    /**
     * Tests base + annotations constructor.
     */
    @Test
    public void testConstructorWithBaseAndAnnotations() {
        DeviceDescription base =
                new DefaultDeviceDescription(DURI, SWITCH, MFR, HW, SW, SN, CID, DA);
        DeviceDescription device = new DefaultDeviceDescription(base, DA2);

        checkValues(device, DURI, SWITCH, MFR, HW, SW, SN, "uri=of:foo",
                    CID.value(), "2ndKey=2ndValue", true);
    }

    /**
     * Tests base + type + annotations constructor.
     */
    @Test
    public void testConstructorWithBaseAndType() {
        DeviceDescription base =
                new DefaultDeviceDescription(DURI, ROADM, MFR, HW, SW, SN, CID, DA);
        DeviceDescription device = new DefaultDeviceDescription(base, ROADM, DA);

        checkValues(device, DURI, ROADM, MFR, HW, SW, SN, "uri=of:foo",
                    CID.value(), "Key=Value", true);
    }

    /**
     * Tests base + annotations + isDefaultAvailable constructor.
     */
    @Test
    public void testConstructorWithBaseAndIsDefault() {
        DeviceDescription base =
                new DefaultDeviceDescription(DURI, SWITCH, MFR, HW, SW, SN, CID, DA);
        DeviceDescription device = new DefaultDeviceDescription(base, false, DA2);

        checkValues(device, DURI, SWITCH, MFR, HW, SW, SN, "uri=of:foo",
                    CID.value(), "2ndKey=2ndValue", false);
    }

    /**
     * Tests empty constructor.
     */
    @Test
    public void testBareConstructor() {
        DeviceDescription device = new DefaultDeviceDescription();

        checkValues(device, null, null, null, null, null, null, "uri=null",
                    CID.value(), "", true);

        assertEquals("incorrect uri", null, device.deviceUri());
        assertEquals("incorrect type", null, device.type());
        assertEquals("incorrect manufacturer", null, device.manufacturer());
        assertEquals("incorrect hw", null, device.hwVersion());
        assertEquals("incorrect sw", null, device.swVersion());
        assertEquals("incorrect serial", null, device.serialNumber());
        assertEquals("Incorrect chassis", null, device.chassisId());
        assertEquals("incorrect annotations", null, device.annotations());
        assertTrue("incorrect default available", device.isDefaultAvailable());
    }

    /**
     * Tests immutability.
     */
    @Test
    public void testImmutable() {
        assertThatClassIsImmutableBaseClass(DefaultDeviceDescription.class);
    }

}

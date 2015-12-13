/*
 * Copyright 2014 Open Networking Laboratory
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

import java.net.URI;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
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

    @Test
    public void basics() {
        DeviceDescription device =
                new DefaultDeviceDescription(DURI, SWITCH, MFR, HW, SW, SN, CID, DA);
        assertEquals("incorrect uri", DURI, device.deviceUri());
        assertEquals("incorrect type", SWITCH, device.type());
        assertEquals("incorrect manufacturer", MFR, device.manufacturer());
        assertEquals("incorrect hw", HW, device.hwVersion());
        assertEquals("incorrect sw", SW, device.swVersion());
        assertEquals("incorrect serial", SN, device.serialNumber());
        assertTrue("incorrect toString", device.toString().contains("uri=of:foo"));
        assertTrue("Incorrect chassis", device.chassisId().value() == 0);
        assertTrue("incorrect annotatios", device.toString().contains("Key=Value"));
    }

}

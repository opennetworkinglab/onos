/*
 * Copyright 2014-present Open Networking Foundation
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
package org.onosproject.net;

import com.google.common.testing.EqualsTester;
import org.junit.Test;
import org.onosproject.net.provider.ProviderId;
import org.onlab.packet.ChassisId;

import static org.junit.Assert.assertEquals;
import static org.onosproject.net.Device.Type.SWITCH;
import static org.onosproject.net.DeviceId.deviceId;

/**
 * Test of the default device model entity.
 */
public class DefaultDeviceTest {

    static final ProviderId PID = new ProviderId("of", "foo");
    static final DeviceId DID1 = deviceId("of:foo");
    static final DeviceId DID2 = deviceId("of:bar");
    static final String MFR = "whitebox";
    static final String HW = "1.1.x";
    static final String SW = "3.9.1";
    static final String SN1 = "43311-12345";
    static final String SN2 = "42346-43512";
    static final ChassisId CID = new ChassisId();

    @Test
    public void testEquality() {
        Device d1 = new DefaultDevice(PID, DID1, SWITCH, MFR, HW, SW, SN1, CID);
        Device d2 = new DefaultDevice(PID, DID1, SWITCH, MFR, HW, SW, SN1, CID);
        Device d3 = new DefaultDevice(PID, DID2, SWITCH, MFR, HW, SW, SN2, CID);
        Device d4 = new DefaultDevice(PID, DID2, SWITCH, MFR, HW, SW, SN2, CID);
        Device d5 = new DefaultDevice(PID, DID2, SWITCH, MFR, HW, SW, SN1, CID);

        new EqualsTester().addEqualityGroup(d1, d2)
                .addEqualityGroup(d3, d4)
                .addEqualityGroup(d5)
                .testEquals();
    }

    @Test
    public void basics() {
        Device device = new DefaultDevice(PID, DID1, SWITCH, MFR, HW, SW, SN1, CID);
        validate(device);
    }

    @Test
    public void annotations() {
        Device device = new DefaultDevice(PID, DID1, SWITCH, MFR, HW, SW, SN1, CID,
                                          DefaultAnnotations.builder().set("foo", "bar").build());
        validate(device);
        assertEquals("incorrect provider", "bar", device.annotations().value("foo"));
    }

    private void validate(Device device) {
        assertEquals("incorrect provider", PID, device.providerId());
        assertEquals("incorrect id", DID1, device.id());
        assertEquals("incorrect type", SWITCH, device.type());
        assertEquals("incorrect manufacturer", MFR, device.manufacturer());
        assertEquals("incorrect hw", HW, device.hwVersion());
        assertEquals("incorrect sw", SW, device.swVersion());
        assertEquals("incorrect serial", SN1, device.serialNumber());
    }
}

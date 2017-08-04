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
import static org.onosproject.net.Port.Type.COPPER;
import static org.onosproject.net.Port.Type.FIBER;
import static org.onosproject.net.PortNumber.portNumber;

/**
 * Test of the default port model entity.
 */
public class DefaultPortTest {

    private static final ProviderId PID = new ProviderId("of", "foo");
    private static final DeviceId DID1 = deviceId("of:foo");
    private static final DeviceId DID2 = deviceId("of:bar");
    private static final PortNumber P1 = portNumber(1);
    private static final PortNumber P2 = portNumber(2);
    private static final long SP1 = 1_000_000;

    @Test
    public void testEquality() {
        Device device = new DefaultDevice(PID, DID1, SWITCH, "m", "h", "s", "n",
                                          new ChassisId());
        Port p1 = new DefaultPort(device, portNumber(1), true, COPPER, SP1);
        Port p2 = new DefaultPort(device, portNumber(1), true, COPPER, SP1);
        Port p3 = new DefaultPort(device, portNumber(2), true, FIBER, SP1);
        Port p4 = new DefaultPort(device, portNumber(2), true, FIBER, SP1);
        Port p5 = new DefaultPort(device, portNumber(1), false);

        new EqualsTester().addEqualityGroup(p1, p2)
                .addEqualityGroup(p3, p4)
                .addEqualityGroup(p5)
                .testEquals();
    }

    @Test
    public void basics() {
        Device device = new DefaultDevice(PID, DID1, SWITCH, "m", "h", "s", "n",
                                          new ChassisId());
        Port port = new DefaultPort(device, portNumber(1), true, FIBER, SP1);
        assertEquals("incorrect element", device, port.element());
        assertEquals("incorrect number", portNumber(1), port.number());
        assertEquals("incorrect state", true, port.isEnabled());
        assertEquals("incorrect speed", SP1, port.portSpeed());
        assertEquals("incorrect type", FIBER, port.type());
    }

}

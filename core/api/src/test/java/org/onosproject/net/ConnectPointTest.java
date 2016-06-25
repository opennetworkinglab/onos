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
package org.onosproject.net;

import com.google.common.testing.EqualsTester;
import org.junit.Test;

import static junit.framework.TestCase.fail;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.onosproject.net.DeviceId.deviceId;
import static org.onosproject.net.PortNumber.portNumber;

/**
 * Test of the connection point entity.
 */
public class ConnectPointTest {

    private static final DeviceId DID1 = deviceId("1");
    private static final DeviceId DID2 = deviceId("2");
    private static final PortNumber P1 = portNumber(1);
    private static final PortNumber P2 = portNumber(2);

    @Test
    public void basics() {
        ConnectPoint p = new ConnectPoint(DID1, P2);
        assertEquals("incorrect element id", DID1, p.deviceId());
        assertEquals("incorrect element id", P2, p.port());
    }

    @Test
    public void testEquality() {
        new EqualsTester()
                .addEqualityGroup(new ConnectPoint(DID1, P1), new ConnectPoint(DID1, P1))
                .addEqualityGroup(new ConnectPoint(DID1, P2), new ConnectPoint(DID1, P2))
                .addEqualityGroup(new ConnectPoint(DID2, P1), new ConnectPoint(DID2, P1))
                .testEquals();
    }

    @Test
    public void testParseDeviceConnectPoint() {
        String cp = "of:0011223344556677/1";

        ConnectPoint connectPoint = ConnectPoint.deviceConnectPoint(cp);
        assertEquals("of:0011223344556677", connectPoint.deviceId().toString());
        assertEquals("1", connectPoint.port().toString());

        expectDeviceParseException("");
        expectDeviceParseException("1/");
        expectDeviceParseException("1/1/1");
        expectDeviceParseException("of:0011223344556677/word");
    }

    /**
     * Parse a device connect point and expect an exception to be thrown.
     *
     * @param string string to parse
     */
    private static void expectDeviceParseException(String string) {
        try {
            ConnectPoint.deviceConnectPoint(string);
            fail("Expected exception was not thrown");
        } catch (Exception e) {
            assertTrue(true);
        }
    }

    @Test
    public void testParseHostConnectPoint() {
        String cp = "16:3A:BD:6E:31:E4/-1/1";

        ConnectPoint connectPoint = ConnectPoint.hostConnectPoint(cp);
        assertEquals("16:3A:BD:6E:31:E4/None", connectPoint.hostId().toString());
        assertEquals("1", connectPoint.port().toString());

        expectHostParseException("");
        expectHostParseException("1/");
        expectHostParseException("1/1");
        expectHostParseException("1/1/1/1");
        expectHostParseException("16:3A:BD:6E:31:E4/word/1");
        expectHostParseException("16:3A:BD:6E:31:E4/1/word");
    }

    /**
     * Parse a host connect point and expect an exception to be thrown.
     *
     * @param string string to parse
     */
    private static void expectHostParseException(String string) {
        try {
            ConnectPoint.hostConnectPoint(string);
            fail("Expected exception was not thrown");
        } catch (Exception e) {
            assertTrue(true);
        }
    }
}

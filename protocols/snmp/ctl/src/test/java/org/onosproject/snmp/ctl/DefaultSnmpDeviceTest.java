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

package org.onosproject.snmp.ctl;

import org.junit.Test;
import org.onosproject.net.DeviceId;

import static org.junit.Assert.*;

/**
 * Test class for DefaultSnmpDevice.
 */
public class DefaultSnmpDeviceTest {

    private final String snmpHost = "1.1.1.1";
    private int snmpPort = 1;
    private final String username = "test";
    private final String community = "test";
    private final DeviceId deviceId = DeviceId.deviceId("snmp:1.1.1.1:1");
    private final String deviceInfo = "host: 1.1.1.1. port: 1";
    private final String defaultProtocol = "udp";
    private final String tcpProtocol = "tcp";

    DefaultSnmpDevice device = new DefaultSnmpDevice("1.1.1.1", 1, "test", "test");

    @Test
    public void basics() throws Exception {
        assertTrue("Device should be reachable", device.isReachable());
        assertEquals("Incorrect protocol", defaultProtocol, device.getProtocol());
        assertEquals("Incorrect host", snmpHost, device.getSnmpHost());
        assertEquals("Incorrect port", snmpPort, device.getSnmpPort());
        assertEquals("Incorrect username", username, device.getUsername());
        assertEquals("Incorrect community", community, device.getCommunity());
        assertEquals("Incorrect deviceID", deviceId, device.deviceId());
        assertEquals("Incorrect deviceInfo", deviceInfo, device.deviceInfo());
        device.disconnect();
        assertFalse("Device should not be reachable", device.isReachable());
    }
}

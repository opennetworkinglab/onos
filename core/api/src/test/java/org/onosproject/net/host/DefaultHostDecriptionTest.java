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
package org.onosproject.net.host;

import org.junit.Test;
import org.onosproject.net.DeviceId;
import org.onosproject.net.HostLocation;
import org.onosproject.net.PortNumber;
import org.onlab.packet.IpAddress;
import org.onlab.packet.MacAddress;
import org.onlab.packet.VlanId;

import com.google.common.collect.ImmutableSet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Test for the default host description.
 */
public class DefaultHostDecriptionTest {

    private static final MacAddress MAC = MacAddress.valueOf("00:00:11:00:00:01");
    private static final VlanId VLAN = VlanId.vlanId((short) 10);
    private static final IpAddress IP = IpAddress.valueOf("10.0.0.1");

    private static final HostLocation LOC = new HostLocation(
            DeviceId.deviceId("of:foo"),
            PortNumber.portNumber(100),
            123L
    );

    @Test
    public void basics() {
        HostDescription host =
                new DefaultHostDescription(MAC, VLAN, LOC, IP);
        assertEquals("incorrect mac", MAC, host.hwAddress());
        assertEquals("incorrect vlan", VLAN, host.vlan());
        assertEquals("incorrect location", LOC, host.location());
        assertEquals("incorrect ip's", ImmutableSet.of(IP), host.ipAddress());
        assertTrue("incorrect toString", host.toString().contains("vlan=10"));
    }

}

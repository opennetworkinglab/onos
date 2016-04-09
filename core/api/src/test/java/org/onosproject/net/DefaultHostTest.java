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

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.google.common.testing.EqualsTester;

public class DefaultHostTest extends  TestDeviceParams {

    @Test
    public void testEquality() {
        Host h1 = new DefaultHost(PID, HID1, MAC1, VLAN1, LOC1, IPSET1);
        Host h2 = new DefaultHost(PID, HID1, MAC1, VLAN1, LOC1, IPSET1);
        Host h3 = new DefaultHost(PID, HID2, MAC2, VLAN2, LOC2, IPSET2);
        Host h4 = new DefaultHost(PID, HID2, MAC2, VLAN2, LOC2, IPSET2);
        Host h5 = new DefaultHost(PID, HID2, MAC2, VLAN1, LOC2, IPSET1);

        new EqualsTester().addEqualityGroup(h1, h2)
                .addEqualityGroup(h3, h4)
                .addEqualityGroup(h5)
                .testEquals();
    }

    @Test
    public void basics() {
        Host host = new DefaultHost(PID, HID1, MAC1, VLAN1, LOC1, IPSET1);
        assertEquals("incorrect provider", PID, host.providerId());
        assertEquals("incorrect id", HID1, host.id());
        assertEquals("incorrect type", MAC1, host.mac());
        assertEquals("incorrect VLAN", VLAN1, host.vlan());
        assertEquals("incorrect location", LOC1, host.location());
        assertEquals("incorrect IP's", IPSET1, host.ipAddresses());
    }

}

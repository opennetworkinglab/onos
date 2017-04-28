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
import static org.junit.Assert.assertTrue;

import com.google.common.collect.ImmutableSet;
import com.google.common.testing.EqualsTester;
import java.util.Set;
import org.junit.Test;

public class DefaultHostTest extends TestDeviceParams {
    private static final Set<HostLocation> LOCATIONS = ImmutableSet.of(LOC1, LOC2, LOC3);
    private static final Host SINGLE_HOMED_HOST =
            new DefaultHost(PID, HID1, MAC1, VLAN1, LOC1, IPSET1);
    private static final Host MULTI_HOMED_HOST =
            new DefaultHost(PID, HID1, MAC1, VLAN1, LOCATIONS, IPSET1, false);

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
        assertEquals("incorrect provider", PID, SINGLE_HOMED_HOST.providerId());
        assertEquals("incorrect id", HID1, SINGLE_HOMED_HOST.id());
        assertEquals("incorrect type", MAC1, SINGLE_HOMED_HOST.mac());
        assertEquals("incorrect VLAN", VLAN1, SINGLE_HOMED_HOST.vlan());
        assertEquals("incorrect location", LOC1, SINGLE_HOMED_HOST.location());
        assertEquals("incorrect IPs", IPSET1, SINGLE_HOMED_HOST.ipAddresses());
    }

    @Test
    public void testLocation() {
        assertEquals("Latest location should be LOC3", LOC3, MULTI_HOMED_HOST.location());
    }

    @Test
    public void testLocations() {
        Set<HostLocation> locations = MULTI_HOMED_HOST.locations();

        assertEquals("There should be 3 locations", locations.size(), 3);
        assertTrue("Host location contains 1st location", locations.contains(LOC1));
        assertTrue("Host location contains 2nd location", locations.contains(LOC2));
        assertTrue("Host location contains 3rd location", locations.contains(LOC3));
    }

}

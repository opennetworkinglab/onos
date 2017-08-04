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
package org.onosproject.net.host;

import org.junit.Test;
import org.onosproject.net.HostLocation;

import com.google.common.collect.ImmutableSet;
import org.onosproject.net.TestDeviceParams;

import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Test for the default host description.
 */
public class DefaultHostDecriptionTest extends TestDeviceParams {
    private static final Set<HostLocation> LOCATIONS = ImmutableSet.of(LOC1, LOC2, LOC3);
    private static final HostDescription SINGLE_HOMED_HOST_DESCR =
            new DefaultHostDescription(MAC1, VLAN1, LOC1, IP1);
    private static final HostDescription MULTI_HOMED_HOST_DESCR =
            new DefaultHostDescription(MAC1, VLAN1, LOCATIONS, IPSET1, false);

    @Test
    public void basics() {
        assertEquals("incorrect mac", MAC1, SINGLE_HOMED_HOST_DESCR.hwAddress());
        assertEquals("incorrect vlan", VLAN1, SINGLE_HOMED_HOST_DESCR.vlan());
        assertEquals("incorrect location", LOC1, SINGLE_HOMED_HOST_DESCR.location());
        assertEquals("incorrect IPs", ImmutableSet.of(IP1), SINGLE_HOMED_HOST_DESCR.ipAddress());
        assertTrue("incorrect toString", SINGLE_HOMED_HOST_DESCR.toString().contains("vlan=11"));
    }

    @Test
    public void testLocation() {
        assertEquals("Latest location should be LOC3", LOC3, MULTI_HOMED_HOST_DESCR.location());
    }

    @Test
    public void testLocations() {
        Set<HostLocation> locations = MULTI_HOMED_HOST_DESCR.locations();

        assertEquals("There should be 3 locations", locations.size(), 3);
        assertTrue("Host location contains 1st location", locations.contains(LOC1));
        assertTrue("Host location contains 2nd location", locations.contains(LOC2));
        assertTrue("Host location contains 3rd location", locations.contains(LOC3));
    }
}

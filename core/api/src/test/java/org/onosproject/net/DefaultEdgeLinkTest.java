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

import static org.junit.Assert.assertEquals;
import static org.onosproject.net.DefaultEdgeLink.createEdgeLink;
import static org.onosproject.net.DefaultLinkTest.cp;
import static org.onosproject.net.DeviceId.deviceId;
import static org.onosproject.net.HostId.hostId;
import static org.onosproject.net.PortNumber.portNumber;

/**
 * Test of the default edge link model entity.
 */
public class DefaultEdgeLinkTest {

    private static final ProviderId PID = new ProviderId("of", "foo");
    private static final DeviceId DID1 = deviceId("of:foo");
    private static final HostId HID1 = hostId("00:00:00:00:00:01/-1");
    private static final HostId HID2 = hostId("00:00:00:00:00:01/-1");
    private static final PortNumber P0 = portNumber(0);
    private static final PortNumber P1 = portNumber(1);

    @Test
    public void testEquality() {
        EdgeLink l1 = new DefaultEdgeLink(PID, cp(HID1, P0),
                                          new HostLocation(DID1, P1, 123L), true);
        EdgeLink l2 = new DefaultEdgeLink(PID, cp(HID1, P0),
                                          new HostLocation(DID1, P1, 123L), true);

        EdgeLink l3 = new DefaultEdgeLink(PID, cp(HID2, P0),
                                          new HostLocation(DID1, P1, 123L), false);
        EdgeLink l4 = new DefaultEdgeLink(PID, cp(HID2, P0),
                                          new HostLocation(DID1, P1, 123L), false);

        new EqualsTester().addEqualityGroup(l1, l2)
                .addEqualityGroup(l3, l4)
                .testEquals();
    }

    @Test
    public void basics() {
        HostLocation hostLocation = new HostLocation(DID1, P1, 123L);
        EdgeLink link = new DefaultEdgeLink(PID, cp(HID1, P0), hostLocation, false);
        assertEquals("incorrect src", cp(HID1, P0), link.dst());
        assertEquals("incorrect dst", hostLocation, link.src());
        assertEquals("incorrect type", Link.Type.EDGE, link.type());
        assertEquals("incorrect hostId", HID1, link.hostId());
        assertEquals("incorrect connect point", hostLocation, link.hostLocation());
        assertEquals("incorrect time", 123L, link.hostLocation().time());
    }

    @Test
    public void phantomIngress() {
        HostLocation hostLocation = new HostLocation(DID1, P1, 123L);
        EdgeLink link = createEdgeLink(hostLocation, true);
        assertEquals("incorrect dst", hostLocation, link.dst());
        assertEquals("incorrect type", Link.Type.EDGE, link.type());
        assertEquals("incorrect connect point", hostLocation, link.hostLocation());
        assertEquals("incorrect time", 123L, link.hostLocation().time());
    }

    @Test
    public void phantomEgress() {
        ConnectPoint hostLocation = new ConnectPoint(DID1, P1);
        EdgeLink link = createEdgeLink(hostLocation, false);
        assertEquals("incorrect src", hostLocation, link.src());
        assertEquals("incorrect type", Link.Type.EDGE, link.type());
        assertEquals("incorrect connect point", hostLocation, link.hostLocation());
        assertEquals("incorrect time", 0L, link.hostLocation().time());
    }

}

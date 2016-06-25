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
import org.onosproject.net.provider.ProviderId;

import static org.junit.Assert.assertEquals;
import static org.onosproject.net.DeviceId.deviceId;
import static org.onosproject.net.Link.Type.DIRECT;
import static org.onosproject.net.Link.Type.INDIRECT;
import static org.onosproject.net.PortNumber.portNumber;

/**
 * Test of the default link model entity.
 */
public class DefaultLinkTest {

    private static final ProviderId PID = new ProviderId("of", "foo");
    private static final DeviceId DID1 = deviceId("of:foo");
    private static final DeviceId DID2 = deviceId("of:bar");
    private static final PortNumber P1 = portNumber(1);
    private static final PortNumber P2 = portNumber(2);

    public static ConnectPoint cp(ElementId id, PortNumber pn) {
        return new ConnectPoint(id, pn);
    }

    @Test
    public void testEquality() {
        Link l1 = new DefaultLink(PID, cp(DID1, P1), cp(DID2, P2), DIRECT, Link.State.ACTIVE);
        Link l2 = new DefaultLink(PID, cp(DID1, P1), cp(DID2, P2), DIRECT, Link.State.ACTIVE);
        Link l3 = new DefaultLink(PID, cp(DID1, P2), cp(DID2, P2), DIRECT, Link.State.ACTIVE);
        Link l4 = new DefaultLink(PID, cp(DID1, P2), cp(DID2, P2), DIRECT, Link.State.ACTIVE);
        Link l5 = new DefaultLink(PID, cp(DID1, P2), cp(DID2, P2), INDIRECT, Link.State.ACTIVE);

        new EqualsTester().addEqualityGroup(l1, l2)
                .addEqualityGroup(l3, l4)
                .addEqualityGroup(l5)
                .testEquals();
    }

    @Test
    public void basics() {
        Link link = new DefaultLink(PID, cp(DID1, P1), cp(DID2, P2), DIRECT, Link.State.ACTIVE);
        assertEquals("incorrect src", cp(DID1, P1), link.src());
        assertEquals("incorrect dst", cp(DID2, P2), link.dst());
        assertEquals("incorrect type", DIRECT, link.type());
    }

}

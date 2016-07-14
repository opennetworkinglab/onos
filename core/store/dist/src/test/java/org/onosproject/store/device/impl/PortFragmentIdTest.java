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
package org.onosproject.store.device.impl;

import static org.onosproject.net.DeviceId.deviceId;

import org.junit.Test;
import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;
import org.onosproject.net.provider.ProviderId;

import com.google.common.testing.EqualsTester;

public class PortFragmentIdTest {

    private static final ProviderId PID = new ProviderId("of", "foo");
    private static final ProviderId PIDA = new ProviderId("of", "bar", true);

    private static final DeviceId DID1 = deviceId("of:foo");
    private static final DeviceId DID2 = deviceId("of:bar");

    private static final PortNumber PN1 = PortNumber.portNumber(1);
    private static final PortNumber PN2 = PortNumber.portNumber(2);

    @Test
    public final void testEquals() {
        new EqualsTester()
        .addEqualityGroup(new PortFragmentId(DID1, PID, PN1),
                          new PortFragmentId(DID1, PID, PN1))
        .addEqualityGroup(new PortFragmentId(DID2, PID, PN1),
                          new PortFragmentId(DID2, PID, PN1))
        .addEqualityGroup(new PortFragmentId(DID1, PIDA, PN1),
                          new PortFragmentId(DID1, PIDA, PN1))
        .addEqualityGroup(new PortFragmentId(DID2, PIDA, PN1),
                          new PortFragmentId(DID2, PIDA, PN1))

        .addEqualityGroup(new PortFragmentId(DID1, PID, PN2),
                          new PortFragmentId(DID1, PID, PN2))
        .addEqualityGroup(new PortFragmentId(DID2, PID, PN2),
                          new PortFragmentId(DID2, PID, PN2))
        .addEqualityGroup(new PortFragmentId(DID1, PIDA, PN2),
                          new PortFragmentId(DID1, PIDA, PN2))
        .addEqualityGroup(new PortFragmentId(DID2, PIDA, PN2),
                          new PortFragmentId(DID2, PIDA, PN2))
        .testEquals();
    }

}

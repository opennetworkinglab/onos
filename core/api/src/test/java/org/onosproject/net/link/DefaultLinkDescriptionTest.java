/*
 * Copyright 2014 Open Networking Laboratory
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
package org.onosproject.net.link;

import org.junit.Test;
import org.onosproject.net.DefaultAnnotations;
import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.onosproject.net.DefaultLinkTest.cp;
import static org.onosproject.net.DeviceId.deviceId;
import static org.onosproject.net.Link.Type.DIRECT;
import static org.onosproject.net.PortNumber.portNumber;

/**
 * Test of the default link description.
 */
public class DefaultLinkDescriptionTest {

    private static final DeviceId DID1 = deviceId("of:foo");
    private static final DeviceId DID2 = deviceId("of:bar");
    private static final PortNumber P1 = portNumber(1);
    private static final DefaultAnnotations DA =
            DefaultAnnotations.builder().set("Key", "Value").build();

    @Test
    public void basics() {
        LinkDescription desc = new DefaultLinkDescription(cp(DID1, P1), cp(DID2, P1), DIRECT, DA);
        assertEquals("incorrect src", cp(DID1, P1), desc.src());
        assertEquals("incorrect dst", cp(DID2, P1), desc.dst());
        assertEquals("incorrect type", DIRECT, desc.type());
        assertTrue("incorrect annotatios", desc.toString().contains("Key=Value"));
    }

}

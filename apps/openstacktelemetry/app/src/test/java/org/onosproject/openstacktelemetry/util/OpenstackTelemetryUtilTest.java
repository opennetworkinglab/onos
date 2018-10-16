/*
 * Copyright 2018-present Open Networking Foundation
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
package org.onosproject.openstacktelemetry.util;

import org.junit.Test;
import org.onlab.packet.IPv4;

import static org.junit.Assert.assertEquals;
import static org.onosproject.openstacktelemetry.util.OpenstackTelemetryUtil.getProtocolNameFromType;
import static org.onosproject.openstacktelemetry.util.OpenstackTelemetryUtil.getProtocolTypeFromString;

public class OpenstackTelemetryUtilTest {

    private static final String PROTOCOL_TCP_L = "TCP";
    private static final String PROTOCOL_TCP_S = "tcp";
    private static final String PROTOCOL_UDP_L = "UDP";
    private static final String PROTOCOL_UDP_S = "udp";
    private static final String PROTOCOL_ANY_L = "ANY";
    private static final String PROTOCOL_ANY_S = "any";

    /**
     * Tests getting a protocol type from a protocol name.
     */
    @Test
    public void testGetProtocolTypeFromString() {
        assertEquals(IPv4.PROTOCOL_TCP, getProtocolTypeFromString(PROTOCOL_TCP_L));
        assertEquals(IPv4.PROTOCOL_TCP, getProtocolTypeFromString(PROTOCOL_TCP_S));

        assertEquals(IPv4.PROTOCOL_UDP, getProtocolTypeFromString(PROTOCOL_UDP_L));
        assertEquals(IPv4.PROTOCOL_UDP, getProtocolTypeFromString(PROTOCOL_UDP_S));

        assertEquals(0, getProtocolTypeFromString(PROTOCOL_ANY_L));
        assertEquals(0, getProtocolTypeFromString(PROTOCOL_ANY_S));
    }

    /**
     * Tests getting a protocol name from a protocol type.
     */
    @Test
    public void testGetProtocolNameFromType() {
        assertEquals(PROTOCOL_TCP_S, getProtocolNameFromType(IPv4.PROTOCOL_TCP));
        assertEquals(PROTOCOL_UDP_S, getProtocolNameFromType(IPv4.PROTOCOL_UDP));
        assertEquals(PROTOCOL_ANY_S, getProtocolNameFromType((byte) 0));
    }
}

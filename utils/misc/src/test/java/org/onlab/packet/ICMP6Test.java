/*
 * Copyright 2015-present Open Networking Foundation
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



package org.onlab.packet;

import org.apache.commons.lang3.StringUtils;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

/**
 * Tests for class {@link ICMP6}.
 */
public class ICMP6Test {
    private static final byte[] IPV6_SOURCE_ADDRESS = {
            (byte) 0xfe, (byte) 0x80, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
            (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x01
    };
    private static final byte[] IPV6_DESTINATION_ADDRESS = {
            (byte) 0xfe, (byte) 0x80, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
            (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x02
    };

    private static IPv6 ipv6 = new IPv6();
    private static byte[] bytePacket = {
            ICMP6.ECHO_REQUEST,       // type
            (byte) 0x00,              // code
            (byte) 0x82, (byte) 0xbc, // checksum
    };

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        ipv6.setSourceAddress(IPV6_SOURCE_ADDRESS);
        ipv6.setDestinationAddress(IPV6_DESTINATION_ADDRESS);
        ipv6.setNextHeader(IPv6.PROTOCOL_ICMP6);
    }

    /**
     * Tests serialize and setters.
     */
    @Test
    public void testSerialize() {
        ICMP6 icmp6 = new ICMP6();
        icmp6.setIcmpType(ICMP6.ECHO_REQUEST);
        icmp6.setIcmpCode((byte) 0);
        icmp6.setParent(ipv6);

        assertArrayEquals(bytePacket, icmp6.serialize());
    }

    @Test
    public void testDeserializeBadInput() throws Exception {
        PacketTestUtils.testDeserializeBadInput(ICMP6.deserializer());
    }

    @Test
    public void testDeserializeTruncated() throws Exception {
        PacketTestUtils.testDeserializeTruncated(ICMP6.deserializer(), bytePacket);
    }

    /**
     * Tests deserialize and getters.
     */
    @Test
    public void testDeserialize() throws Exception {
        ICMP6 icmp6 = ICMP6.deserializer().deserialize(bytePacket, 0, bytePacket.length);

        assertThat(icmp6.getIcmpType(), is(ICMP6.ECHO_REQUEST));
        assertThat(icmp6.getIcmpCode(), is((byte) 0x00));
        assertThat(icmp6.getChecksum(), is((short) 0x82bc));
    }

    /**
     * Tests comparator.
     */
    @Test
    public void testEqual() {
        ICMP6 icmp61 = new ICMP6();
        icmp61.setIcmpType(ICMP6.ECHO_REQUEST);
        icmp61.setIcmpCode((byte) 0);
        icmp61.setChecksum((short) 0);

        ICMP6 icmp62 = new ICMP6();
        icmp62.setIcmpType(ICMP6.ECHO_REPLY);
        icmp62.setIcmpCode((byte) 0);
        icmp62.setChecksum((short) 0);

        assertTrue(icmp61.equals(icmp61));
        assertFalse(icmp61.equals(icmp62));
    }

    /**
     * Tests toString.
     */
    @Test
    public void testToStringIcmp6() throws Exception {
        ICMP6 icmp6 = ICMP6.deserializer().deserialize(bytePacket, 0, bytePacket.length);
        String str = icmp6.toString();

        assertTrue(StringUtils.contains(str, "icmpType=" + ICMP6.ECHO_REQUEST));
        assertTrue(StringUtils.contains(str, "icmpCode=" + (byte) 0x00));
        assertTrue(StringUtils.contains(str, "checksum=" + (short) 0x82bc));
    }
}

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



package org.onlab.packet;

import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.nio.ByteBuffer;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.*;
import static org.onlab.packet.IPv6.*;

/**
 * Tests for class {@link IPv6}.
 */
public class IPv6Test {
    private static final byte[] SOURCE_ADDRESS = {
            (byte) 0x20, (byte) 0x01, (byte) 0x0f, (byte) 0x18, (byte) 0x01, (byte) 0x13, (byte) 0x02, (byte) 0x15,
            (byte) 0xca, (byte) 0x2a, (byte) 0x14, (byte) 0xff, (byte) 0xfe, (byte) 0x35, (byte) 0x26, (byte) 0xce
    };
    private static final byte[] DESTINATION_ADDRESS = {
            (byte) 0x20, (byte) 0x01, (byte) 0x0f, (byte) 0x18, (byte) 0x01, (byte) 0x13, (byte) 0x02, (byte) 0x15,
            (byte) 0xe6, (byte) 0xce, (byte) 0x8f, (byte) 0xff, (byte) 0xfe, (byte) 0x54, (byte) 0x37, (byte) 0xc8
    };
    private static final byte[] SOLICITATION_NODE_ADDRESS = {
            (byte) 0xff, (byte) 0x02, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
            (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x01, (byte) 0xff, (byte) 0x54, (byte) 0x37, (byte) 0xc8
    };
    private static final byte[] MULTICAST_ADDRESS = {
            (byte) 0x33, (byte) 0x33, (byte) 0xfe, (byte) 0x54, (byte) 0x37, (byte) 0xc8
    };
    private static final byte[] LINK_LOCAL_ADDRESS_1 = {
            (byte) 0xfe, (byte) 0x80, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
            (byte) 0x50, (byte) 0x74, (byte) 0xf2, (byte) 0xff, (byte) 0xfe, (byte) 0xb1, (byte) 0xa8, (byte) 0x7f
    };
    private static final byte[] MAC_ADDRESS_1 = {
            (byte) 0x52, (byte) 0x74, (byte) 0xf2, (byte) 0xb1, (byte) 0xa8, (byte) 0x7f
    };
    private static final byte[] LINK_LOCAL_ADDRESS_2 = {
            (byte) 0xfe, (byte) 0x80, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
            (byte) 0x02, (byte) 0x0c, (byte) 0x29, (byte) 0xff, (byte) 0xfe, (byte) 0x8f, (byte) 0x99, (byte) 0xd2
    };
    private static final byte[] MAC_ADDRESS_2 = {
            (byte) 0x00, (byte) 0x0c, (byte) 0x29, (byte) 0x8f, (byte) 0x99, (byte) 0xd2
    };

    private static Data data;
    private static UDP udp;
    private static byte[] bytePacket;

    private Deserializer<IPv6> deserializer;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        data = new Data();
        data.setData("testSerialize".getBytes());
        udp = new UDP();
        udp.setPayload(data);

        byte[] bytePayload = udp.serialize();
        byte[] byteHeader = {
                (byte) 0x69, (byte) 0x31, (byte) 0x35, (byte) 0x79,
                (byte) (bytePayload.length >> 8 & 0xff), (byte) (bytePayload.length & 0xff),
                (byte) 0x11, (byte) 0x20,
                (byte) 0x20, (byte) 0x01, (byte) 0x0f, (byte) 0x18, (byte) 0x01, (byte) 0x13, (byte) 0x02, (byte) 0x15,
                (byte) 0xca, (byte) 0x2a, (byte) 0x14, (byte) 0xff, (byte) 0xfe, (byte) 0x35, (byte) 0x26, (byte) 0xce,
                (byte) 0x20, (byte) 0x01, (byte) 0x0f, (byte) 0x18, (byte) 0x01, (byte) 0x13, (byte) 0x02, (byte) 0x15,
                (byte) 0xe6, (byte) 0xce, (byte) 0x8f, (byte) 0xff, (byte) 0xfe, (byte) 0x54, (byte) 0x37, (byte) 0xc8,
        };
        bytePacket = new byte[byteHeader.length + bytePayload.length];
        System.arraycopy(byteHeader, 0, bytePacket, 0, byteHeader.length);
        System.arraycopy(bytePayload, 0, bytePacket, byteHeader.length, bytePayload.length);
    }

    @Before
    public void setUp() {
        deserializer = deserializer();
    }

    /**
     * Tests serialize and setters.
     */
    @Test
    public void testSerialize() {
        IPv6 ipv6 = new IPv6();
        ipv6.setPayload(udp);
        ipv6.setVersion((byte) 6);
        ipv6.setTrafficClass((byte) 0x93);
        ipv6.setFlowLabel(0x13579);
        ipv6.setNextHeader(PROTOCOL_UDP);
        ipv6.setHopLimit((byte) 32);
        ipv6.setSourceAddress(SOURCE_ADDRESS);
        ipv6.setDestinationAddress(DESTINATION_ADDRESS);

        assertArrayEquals(ipv6.serialize(), bytePacket);
    }

    @Test
    public void testDeserializeBadInput() throws Exception {
        PacketTestUtils.testDeserializeBadInput(deserializer);
    }

    @Test
    public void testDeserializeTruncated() throws Exception {
        // Run the truncation test only on the IPv6 header
        byte[] ipv6Header = new byte[FIXED_HEADER_LENGTH];
        ByteBuffer.wrap(bytePacket).get(ipv6Header);

        PacketTestUtils.testDeserializeTruncated(deserializer, ipv6Header);
    }

    /**
     * Tests deserialize and getters.
     */
    @Test
    public void testDeserialize() throws DeserializationException {
        IPv6 ipv6 = deserializer.deserialize(bytePacket, 0, bytePacket.length);

        assertThat(ipv6.getVersion(), is((byte) 6));
        assertThat(ipv6.getTrafficClass(), is((byte) 0x93));
        assertThat(ipv6.getFlowLabel(), is(0x13579));
        assertThat(ipv6.getNextHeader(), is(PROTOCOL_UDP));
        assertThat(ipv6.getHopLimit(), is((byte) 32));
        assertArrayEquals(ipv6.getSourceAddress(), SOURCE_ADDRESS);
        assertArrayEquals(ipv6.getDestinationAddress(), DESTINATION_ADDRESS);
    }

    /**
     * Tests comparator.
     */
    @Test
    public void testEqual() {
        IPv6 packet1 = new IPv6();
        packet1.setPayload(udp);
        packet1.setVersion((byte) 6);
        packet1.setTrafficClass((byte) 0x93);
        packet1.setFlowLabel(0x13579);
        packet1.setNextHeader(PROTOCOL_UDP);
        packet1.setHopLimit((byte) 32);
        packet1.setSourceAddress(SOURCE_ADDRESS);
        packet1.setDestinationAddress(DESTINATION_ADDRESS);

        IPv6 packet2 = new IPv6();
        packet2.setPayload(udp);
        packet2.setVersion((byte) 6);
        packet2.setTrafficClass((byte) 0x93);
        packet2.setFlowLabel(0x13579);
        packet2.setNextHeader(PROTOCOL_UDP);
        packet2.setHopLimit((byte) 32);
        packet2.setSourceAddress(DESTINATION_ADDRESS);
        packet2.setDestinationAddress(SOURCE_ADDRESS);

        assertTrue(packet1.equals(packet1));
        assertFalse(packet1.equals(packet2));
    }

    /**
     * Tests toString.
     */
    @Test
    public void testToStringIPv6() throws Exception {
        IPv6 ipv6 = deserializer.deserialize(bytePacket, 0, bytePacket.length);
        String str = ipv6.toString();

        assertTrue(StringUtils.contains(str, "version=" + (byte) 6));
        assertTrue(StringUtils.contains(str, "trafficClass=" + (byte) 0x93));
        assertTrue(StringUtils.contains(str, "flowLabel=" + 0x13579));
        assertTrue(StringUtils.contains(str, "nextHeader=" + PROTOCOL_UDP));
        assertTrue(StringUtils.contains(str, "hopLimit=" + (byte) 32));
        // TODO: test IPv6 source and destination address
    }

    /**
     * Tests the proper operation of the solicitationNodeAddress function.
     */
    @Test
    public void testSolicitationNodeAddress() {
        assertArrayEquals(SOLICITATION_NODE_ADDRESS, getSolicitNodeAddress(DESTINATION_ADDRESS));
    }

    /**
     * Tests the proper operation of the multicastAddress function.
     */
    @Test
    public void testMulticastAddress() {
        assertArrayEquals(MULTICAST_ADDRESS, getMCastMacAddress(DESTINATION_ADDRESS));
    }

    /**
     * Tests the proper operation of the isLinkLocalAddress function.
     */
    @Test
    public void testIsLinkLocalAddress() {
        assertFalse(isLinkLocalAddress(SOURCE_ADDRESS));
        assertFalse(isLinkLocalAddress(DESTINATION_ADDRESS));
        assertFalse(isLinkLocalAddress(SOLICITATION_NODE_ADDRESS));
        assertTrue(isLinkLocalAddress(LINK_LOCAL_ADDRESS_1));
        assertTrue(isLinkLocalAddress(LINK_LOCAL_ADDRESS_2));
    }

    /**
     * Tests the proper operation of the linkLocalAddress function.
     */
    @Test
    public void testLinkLocalAddress() {
        assertArrayEquals(getLinkLocalAddress(MAC_ADDRESS_1), LINK_LOCAL_ADDRESS_1);
        assertArrayEquals(getLinkLocalAddress(MAC_ADDRESS_2), LINK_LOCAL_ADDRESS_2);
    }

    /**
     * Tests the proper operation of the macAddress function.
     */
    @Test
    public void testMacAddress() {
        assertArrayEquals(getMacAddress(LINK_LOCAL_ADDRESS_1), MAC_ADDRESS_1);
        assertArrayEquals(getMacAddress(LINK_LOCAL_ADDRESS_2), MAC_ADDRESS_2);
    }
}

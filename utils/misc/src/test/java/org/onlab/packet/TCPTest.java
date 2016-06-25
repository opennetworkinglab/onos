/*
 * Copyright 2015-present Open Networking Laboratory
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
 * Tests for class {@link TCP}.
 */
public class TCPTest {
    private static final byte[] IPV4_SOURCE_ADDRESS = {
            (byte) 192, (byte) 168, (byte) 1, (byte) 1
    };
    private static final byte[] IPV4_DESTINATION_ADDRESS = {
            (byte) 192, (byte) 168, (byte) 1, (byte) 2
    };
    private static final byte[] IPV6_SOURCE_ADDRESS = {
            (byte) 0xfe, (byte) 0x80, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
            (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x01
    };
    private static final byte[] IPV6_DESTINATION_ADDRESS = {
            (byte) 0xfe, (byte) 0x80, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
            (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x02
    };

    private static IPv4 ipv4 = new IPv4();
    private static IPv6 ipv6 = new IPv6();
    private static byte[] bytePacketTCP4 = {
            (byte) 0x00, (byte) 0x50, (byte) 0x00, (byte) 0x60, // src,dst port
            (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x10, // seq
            (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x20, // ack
            (byte) 0x50, (byte) 0x02, // offset,flag
            (byte) 0x10, (byte) 0x00, // window
            (byte) 0x1b, (byte) 0xae, // checksum
            (byte) 0x00, (byte) 0x01  // urgent
    };
    private static byte[] bytePacketTCP6 = {
            (byte) 0x00, (byte) 0x50, (byte) 0x00, (byte) 0x60, // src,dst port
            (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x10, // seq
            (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x20, // ack
            (byte) 0x50, (byte) 0x02, // offset,flag
            (byte) 0x10, (byte) 0x00, // window
            (byte) 0xa1, (byte) 0xfd, // checksum
            (byte) 0x00, (byte) 0x01  // urgent
    };

    private static Deserializer<TCP> deserializer;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        deserializer = TCP.deserializer();

        ipv4.setSourceAddress(IPv4.toIPv4Address(IPV4_SOURCE_ADDRESS));
        ipv4.setDestinationAddress(IPv4.toIPv4Address(IPV4_DESTINATION_ADDRESS));
        ipv4.setProtocol(IPv4.PROTOCOL_TCP);

        ipv6.setSourceAddress(IPV6_SOURCE_ADDRESS);
        ipv6.setDestinationAddress(IPV6_DESTINATION_ADDRESS);
        ipv6.setNextHeader(IPv6.PROTOCOL_TCP);
    }

    /**
     * Tests serialize and setters.
     */
    @Test
    public void testSerialize() {
        TCP tcp = new TCP();
        tcp.setSourcePort(0x50);
        tcp.setDestinationPort(0x60);
        tcp.setSequence(0x10);
        tcp.setAcknowledge(0x20);
        tcp.setDataOffset((byte) 0x5);
        tcp.setFlags((short) 0x2);
        tcp.setWindowSize((short) 0x1000);
        tcp.setUrgentPointer((short) 0x1);

        tcp.setParent(ipv4);
        assertArrayEquals(bytePacketTCP4, tcp.serialize());
        tcp.resetChecksum();
        tcp.setParent(ipv6);
        assertArrayEquals(bytePacketTCP6, tcp.serialize());
    }

    @Test
    public void testDeserializeBadInput() throws Exception {
        PacketTestUtils.testDeserializeBadInput(deserializer);
    }

    @Test
    public void testDeserializeTruncated() throws Exception {
        PacketTestUtils.testDeserializeTruncated(deserializer, bytePacketTCP4);
    }

    /**
     * Tests deserialize and getters.
     */
    @Test
    public void testDeserialize() throws Exception {
        TCP tcp = deserializer.deserialize(bytePacketTCP4, 0, bytePacketTCP4.length);

        assertThat(tcp.getSourcePort(), is(0x50));
        assertThat(tcp.getDestinationPort(), is(0x60));
        assertThat(tcp.getSequence(), is(0x10));
        assertThat(tcp.getAcknowledge(), is(0x20));
        assertThat(tcp.getDataOffset(), is((byte) 0x5));
        assertThat(tcp.getFlags(), is((short) 0x2));
        assertThat(tcp.getWindowSize(), is((short) 0x1000));
        assertThat(tcp.getUrgentPointer(), is((short) 0x1));
        assertThat(tcp.getChecksum(), is((short) 0x1bae));
    }

    /**
     * Tests comparator.
     */
    @Test
    public void testEqual() {
        TCP tcp1 = new TCP();
        tcp1.setSourcePort(0x50);
        tcp1.setDestinationPort(0x60);
        tcp1.setSequence(0x10);
        tcp1.setAcknowledge(0x20);
        tcp1.setDataOffset((byte) 0x5);
        tcp1.setFlags((short) 0x2);
        tcp1.setWindowSize((short) 0x1000);
        tcp1.setUrgentPointer((short) 0x1);

        TCP tcp2 = new TCP();
        tcp2.setSourcePort(0x70);
        tcp2.setDestinationPort(0x60);
        tcp2.setSequence(0x10);
        tcp2.setAcknowledge(0x20);
        tcp2.setDataOffset((byte) 0x5);
        tcp2.setFlags((short) 0x2);
        tcp2.setWindowSize((short) 0x1000);
        tcp2.setUrgentPointer((short) 0x1);

        assertTrue(tcp1.equals(tcp1));
        assertFalse(tcp1.equals(tcp2));
    }

    /**
     * Tests toString.
     */
    @Test
    public void testToStringTcp() throws Exception {
        TCP tcp = deserializer.deserialize(bytePacketTCP4, 0, bytePacketTCP4.length);
        String str = tcp.toString();

        assertTrue(StringUtils.contains(str, "sourcePort=" + 0x50));
        assertTrue(StringUtils.contains(str, "destinationPort=" + 0x60));
        assertTrue(StringUtils.contains(str, "sequence=" + 0x10));
        assertTrue(StringUtils.contains(str, "acknowledge=" + 0x20));
        assertTrue(StringUtils.contains(str, "dataOffset=" + (byte) 0x5));
        assertTrue(StringUtils.contains(str, "flags=" + (short) 0x2));
        assertTrue(StringUtils.contains(str, "windowSize=" + (short) 0x1000));
        assertTrue(StringUtils.contains(str, "checksum=" + (short) 0x1bae));
        assertTrue(StringUtils.contains(str, "urgentPointer=" + (short) 0x1));
    }
}

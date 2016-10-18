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
import org.junit.Before;
import org.junit.Test;

import java.nio.ByteBuffer;
import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for the ARP class.
 */
public class ArpTest {

    private Deserializer<ARP> deserializer = ARP.deserializer();

    private final byte hwAddressLength = 6;
    private final byte protoAddressLength = 4;

    private MacAddress srcMac = MacAddress.valueOf(1);
    private MacAddress targetMac = MacAddress.valueOf(2);
    private Ip4Address srcIp = Ip4Address.valueOf(1);
    private Ip4Address targetIp = Ip4Address.valueOf(2);

    private byte[] byteHeader;

    @Before
    public void setUp() {
        ByteBuffer bb = ByteBuffer.allocate(ARP.INITIAL_HEADER_LENGTH +
                2 * hwAddressLength + 2 * protoAddressLength);
        bb.putShort(ARP.HW_TYPE_ETHERNET);
        bb.putShort(ARP.PROTO_TYPE_IP);
        bb.put(hwAddressLength);
        bb.put(protoAddressLength);
        bb.putShort(ARP.OP_REPLY);

        bb.put(srcMac.toBytes());
        bb.put(srcIp.toOctets());
        bb.put(targetMac.toBytes());
        bb.put(targetIp.toOctets());

        byteHeader = bb.array();
    }

    @Test
    public void testDeserializeBadInput() throws Exception {
        PacketTestUtils.testDeserializeBadInput(deserializer);
    }

    @Test
    public void testDeserializeTruncated() throws Exception {
        PacketTestUtils.testDeserializeTruncated(deserializer, byteHeader);
    }

    /**
     * Tests deserialize and getters.
     */
    @Test
    public void testDeserialize() throws Exception {
        ARP arp = deserializer.deserialize(byteHeader, 0, byteHeader.length);

        assertEquals(ARP.HW_TYPE_ETHERNET, arp.getHardwareType());
        assertEquals(ARP.PROTO_TYPE_IP, arp.getProtocolType());
        assertEquals(hwAddressLength, arp.getHardwareAddressLength());
        assertEquals(protoAddressLength, arp.getProtocolAddressLength());
        assertEquals(ARP.OP_REPLY, arp.getOpCode());

        assertTrue(Arrays.equals(srcMac.toBytes(), arp.getSenderHardwareAddress()));
        assertTrue(Arrays.equals(srcIp.toOctets(), arp.getSenderProtocolAddress()));
        assertTrue(Arrays.equals(targetMac.toBytes(), arp.getTargetHardwareAddress()));
        assertTrue(Arrays.equals(targetIp.toOctets(), arp.getTargetProtocolAddress()));
    }

    /**
     * Tests toString.
     */
    @Test
    public void testToStringArp() throws Exception {
        ARP arp = deserializer.deserialize(byteHeader, 0, byteHeader.length);
        String str = arp.toString();
        assertTrue(StringUtils.contains(str, "hardwareAddressLength=" + hwAddressLength));
        assertTrue(StringUtils.contains(str, "protocolAddressLength=" + protoAddressLength));
        assertTrue(StringUtils.contains(str, "senderHardwareAddress=" + srcMac));
        assertTrue(StringUtils.contains(str, "senderProtocolAddress=" + srcIp));
        assertTrue(StringUtils.contains(str, "targetHardwareAddress=" + targetMac));
        assertTrue(StringUtils.contains(str, "targetProtocolAddress=" + targetIp));
    }
}

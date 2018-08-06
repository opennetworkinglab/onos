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
package org.onlab.packet;

import org.junit.Before;
import org.junit.Test;

import java.nio.ByteBuffer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.onlab.packet.Ethernet.TYPE_IPV4;

/**
 * Unit tests for the ICMPEcho class.
 */
public class ICMPEchoTest {
    private Deserializer<ICMPEcho> deserializer;

    protected byte icmpType = ICMP.TYPE_ECHO_REQUEST;
    protected byte icmpCode = ICMP.CODE_ECHO_REQEUST;
    protected short checksum = 870;
    private short identifier = 1;
    private short sequenceNum = 2;
    private MacAddress srcMac = MacAddress.valueOf("11:22:33:44:55:66");
    private IpAddress srcIp = IpAddress.valueOf("10.0.0.3");
    private MacAddress dstMac = MacAddress.valueOf("66:55:44:33:22:11");
    private IpAddress dstIp = IpAddress.valueOf("10.0.0.1");

    private byte[] headerBytes;

    /**
     * Initial setup for this unit test.
     */
    @Before
    public void setUp() throws Exception {
        deserializer = ICMPEcho.deserializer();

        ByteBuffer bb = ByteBuffer.allocate(ICMPEcho.ICMP_ECHO_HEADER_LENGTH);

        bb.putShort(identifier);
        bb.putShort(sequenceNum);

        headerBytes = bb.array();
    }


    /**
     * Tests the deserializeBadInput.
     *
     * @throws Exception exception
     */
    @Test
    public void testDeserializeBadInput() throws Exception {
        PacketTestUtils.testDeserializeBadInput(deserializer);
    }

    /**
     * Tests the deserialzeTruncates.
     *
     * @throws Exception exception
     */
    @Test
    public void testDeserializeTruncated() throws Exception {
        PacketTestUtils.testDeserializeTruncated(deserializer, headerBytes);
    }

    /**
     * Tests deserialize, serialize, getters and setters.
     *
     * @throws Exception exception
     */
    @Test
    public void testIcmpEchoPacket() throws Exception {
        ICMPEcho icmpEcho = new ICMPEcho();

        icmpEcho.setIdentifier(identifier);
        icmpEcho.setSequenceNum(sequenceNum);

        ByteBuffer bb = ByteBuffer.wrap(icmpEcho.serialize());

        ICMPEcho deserializedIcmpEcho = deserializer.deserialize(bb.array(), 0, bb.array().length);

        assertTrue(deserializedIcmpEcho.equals(icmpEcho));

        assertEquals(deserializedIcmpEcho.getIdentifier(), icmpEcho.getIdentifier());
        assertEquals(deserializedIcmpEcho.getSequenceNum(), icmpEcho.getSequenceNum());
    }

    @Test
    public void testDeserializeFromIpPacket() throws Exception {
        ICMPEcho icmpEcho = new ICMPEcho();
        icmpEcho.setIdentifier(identifier)
                .setSequenceNum(sequenceNum);
        ByteBuffer byteBufferIcmpEcho = ByteBuffer.wrap(icmpEcho.serialize());

        ICMP icmp = new ICMP();
        icmp.setIcmpType(icmpType)
                .setIcmpCode(icmpCode)
                .setChecksum(checksum);

        icmp.setPayload(ICMPEcho.deserializer().deserialize(byteBufferIcmpEcho.array(),
                0, ICMPEcho.ICMP_ECHO_HEADER_LENGTH));
        ByteBuffer byteBufferIcmp = ByteBuffer.wrap(icmp.serialize());


        IPv4 iPacket = new IPv4();
        iPacket.setDestinationAddress(dstIp.toString());
        iPacket.setSourceAddress(srcIp.toString());
        iPacket.setTtl((byte) 64);
        iPacket.setChecksum((short) 0);
        iPacket.setDiffServ((byte) 0);
        iPacket.setProtocol(IPv4.PROTOCOL_ICMP);

        iPacket.setPayload(ICMP.deserializer().deserialize(byteBufferIcmp.array(),
                0,
                byteBufferIcmp.array().length));

        Ethernet ethPacket = new Ethernet();

        ethPacket.setEtherType(TYPE_IPV4);
        ethPacket.setSourceMACAddress(srcMac);
        ethPacket.setDestinationMACAddress(dstMac);
        ethPacket.setPayload(iPacket);

        validatePacket(ethPacket);
    }

    private void validatePacket(Ethernet ethPacket) {
        ICMP icmp = (ICMP) ethPacket.getPayload().getPayload();
        ICMPEcho icmpEcho = (ICMPEcho) icmp.getPayload();

        assertEquals(icmp.getIcmpType(), icmpType);
        assertEquals(icmp.getIcmpCode(), icmpCode);
        assertEquals(icmp.getChecksum(), checksum);
        assertEquals(icmpEcho.getIdentifier(), identifier);
        assertEquals(icmpEcho.getSequenceNum(), sequenceNum);
    }

}

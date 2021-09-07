/*
 * Copyright 2021-present Open Networking Foundation
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
package org.onlab.packet.bmp;

import com.google.common.testing.EqualsTester;
import org.apache.commons.lang.StringUtils;
import org.junit.Before;
import org.junit.Test;
import org.onlab.packet.Deserializer;
import org.onlab.packet.PacketTestUtils;

import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Unit test for BmpPeer class.
 */
public class BmpPeerTest {
    private Deserializer<BmpPeer> deserializer;

    private byte type = 0;
    private byte flags = 0;
    private byte[] peerDistinguisher = new byte[BmpPeer.PEER_DISTINGUISHER];
    private InetAddress peerAddress;
    private int peerAs = 60300;
    private int peerBgpId = 65323;
    private int seconds = 1024;
    private int microseconds = 0;

    private byte[] headerBytes;


    @Before
    public void setUp() throws Exception {
        deserializer = BmpPeer.deserializer();
        peerAddress = InetAddress.getByAddress(new byte[]{127, 0, 0, 1});

        ByteBuffer bb = ByteBuffer.allocate(BmpPeer.PEER_HEADER_MINIMUM_LENGTH);

        bb.put(this.type);
        bb.put(this.flags);
        bb.put(this.peerDistinguisher);
        bb.put(new byte[]{0, 0, 0, 0,
                0, 0, 0, 0,
                0, 0, 0, 0,
                127, 0, 0, 1});
        bb.putInt(this.peerAs);
        bb.putInt(this.peerBgpId);
        bb.putInt(this.seconds);
        bb.putInt(this.microseconds);

        headerBytes = bb.array();
    }

    @Test
    public void testDeserializeBadInput() throws Exception {
        PacketTestUtils.testDeserializeBadInput(deserializer);
    }

    @Test
    public void testDeserializeTruncated() throws Exception {
        PacketTestUtils.testDeserializeTruncated(deserializer, headerBytes);
    }

    /**
     * Test Deserialize and getters.
     *
     * @throws Exception
     */
    @Test
    public void testDeserialize() throws Exception {
        BmpPeer bmpPeer = deserializer.deserialize(headerBytes, 0, headerBytes.length);

        assertEquals(type, bmpPeer.getType());
        assertEquals(flags, bmpPeer.getFlag());
        assertEquals(peerDistinguisher.length, bmpPeer.getPeerDistinguisher().length);
        assertEquals(peerAddress.getHostAddress(), bmpPeer.getIntAddress().getHostAddress());
        assertEquals(peerAs, bmpPeer.getPeerAs());
        assertEquals(peerBgpId, bmpPeer.getPeerBgpId());
        assertEquals(seconds, bmpPeer.getSeconds());
        assertEquals(microseconds, bmpPeer.getMicroseconds());
    }

    /**
     * Tests toString.
     *
     * @throws Exception
     */
    @Test
    public void testToStringBmp() throws Exception {
        BmpPeer bmpPeer = deserializer.deserialize(headerBytes, 0, headerBytes.length);
        String str = bmpPeer.toString();

        assertTrue(StringUtils.contains(str, "flags=" + flags));
        assertTrue(StringUtils.contains(str, "type=" + type));
        assertTrue(StringUtils.contains(str, "peerDistinguisher=" + Arrays.toString(peerDistinguisher)));
        assertTrue(StringUtils.contains(str, "peerAddress=" + peerAddress.getHostAddress()));
        assertTrue(StringUtils.contains(str, "peerAs=" + peerAs));
        assertTrue(StringUtils.contains(str, "peerBgpId=" + peerBgpId));
        assertTrue(StringUtils.contains(str, "seconds=" + seconds));
        assertTrue(StringUtils.contains(str, "microseconds=" + microseconds));
    }

    /**
     * Tests equals method.
     *
     * @throws Exception
     */
    @Test
    public void testEquality() throws Exception {
        BmpPeer bmpPeer = deserializer.deserialize(headerBytes, 0, headerBytes.length);
        new EqualsTester()
                .addEqualityGroup(bmpPeer).testEquals();
    }
}

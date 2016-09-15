/*
 * Copyright 2016-present Open Networking Laboratory
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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;

/**
 * Unit tests for IP class.
 */
public class IpTest {
    private byte v4Version = 4;
    private byte v4HeaderLength = 6;
    private byte badVersion = 5;
    private byte badHeaderLength = 6;
    private static Data data;
    private static UDP udp;
    private byte[] v4HeaderBytes;
    private byte[] v6HeaderBytes;
    private byte[] badHeaderBytes;

    @Before
    public void setUp() throws Exception {

        ByteBuffer v4bb = ByteBuffer.allocate(v4HeaderLength * 4);
        v4bb.put((byte) ((v4Version & 0xf) << 4));
        v4HeaderBytes = v4bb.array();

        ByteBuffer badBb = ByteBuffer.allocate(badHeaderLength * 4);
        badBb.put((byte) ((badVersion & 0xf) << 4));
        badHeaderBytes = badBb.array();

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
        v6HeaderBytes = new byte[byteHeader.length + bytePayload.length];
        System.arraycopy(byteHeader, 0, v6HeaderBytes, 0, byteHeader.length);
        System.arraycopy(bytePayload, 0, v6HeaderBytes, byteHeader.length, bytePayload.length);
    }

    @Test
    public void testDeserialize() throws Exception {
        Deserializer ipDeserializer = IP.deserializer();
        IPacket v4Packet = ipDeserializer.deserialize(v4HeaderBytes, 0, v4HeaderLength * 4);
        IPacket v6Packet = ipDeserializer.deserialize(v6HeaderBytes, 0, v6HeaderBytes.length);
        assertThat(v4Packet, is(instanceOf(IPv4.class)));
        assertThat(v6Packet, is(instanceOf(IPv6.class)));

        IPv6 ipv6 = (IPv6) v6Packet;
        assertThat(ipv6.getVersion(), is((byte) 6));
        assertThat(ipv6.getTrafficClass(), is((byte) 0x93));
        assertThat(ipv6.getFlowLabel(), is(0x13579));
        assertThat(ipv6.getNextHeader(), is(IPv6.PROTOCOL_UDP));
        assertThat(ipv6.getHopLimit(), is((byte) 32));
    }

    @Test(expected = DeserializationException.class)
    public void testBadIpVersion() throws Exception {
        Deserializer ipDeserializer = IP.deserializer();
        ipDeserializer.deserialize(badHeaderBytes, 0, badHeaderLength * 4);
    }
}
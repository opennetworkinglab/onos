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
import org.junit.Before;
import org.junit.Test;

import java.nio.ByteBuffer;

import static junit.framework.TestCase.assertNotNull;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.onlab.packet.ICMP.TYPE_ECHO_REQUEST;
import static org.onlab.packet.ICMP6.ECHO_REQUEST;
import static org.onlab.packet.IPv4.PROTOCOL_ICMP;
import static org.onlab.packet.MPLS.PROTOCOL_IPV4;
import static org.onlab.packet.MPLS.PROTOCOL_IPV6;
import static org.onlab.packet.MPLS.PROTOCOL_MPLS;

/**
 * Unit tests for MPLS class.
 */
public class MplsTest {

    private Deserializer<MPLS> deserializer;

    private int label = 1048575;
    private byte bos = 1;
    private byte ttl = 20;
    private byte protocol = PROTOCOL_IPV4;

    private byte[] bytes;
    private byte[] truncatedBytes;

    // Define packets to deserialize
    private static final MacAddress SRC_MAC = MacAddress.valueOf("00:00:00:00:00:01");
    private static final MacAddress DST_MAC = MacAddress.valueOf("00:00:00:00:00:02");

    private static final ICMPEcho ICMP_ECHO = new ICMPEcho()
            .setIdentifier((short) 0)
            .setSequenceNum((short) 0);

    private static final ICMP ICMP = (ICMP) new ICMP()
            .setIcmpType(TYPE_ECHO_REQUEST)
            .setPayload(ICMP_ECHO);

    private static final Ip4Address SRC_IPV4 = Ip4Address.valueOf("10.0.1.1");
    private static final Ip4Address DST_IPV4 = Ip4Address.valueOf("10.0.0.254");

    private static final IPv4 IPV4 = (IPv4) new IPv4()
            .setDestinationAddress(DST_IPV4.toInt())
            .setSourceAddress(SRC_IPV4.toInt())
            .setTtl((byte) 64)
            .setProtocol(PROTOCOL_ICMP)
            .setPayload(ICMP);

    private static final ICMP6 ICMP6 = new ICMP6()
            .setIcmpType(ECHO_REQUEST);

    private static final Ip6Address SRC_IPV6 = Ip6Address.valueOf("2000::101");
    private static final Ip6Address DST_IPV6 = Ip6Address.valueOf("2000::ff");

    private static final IPv6 IPV6 = (IPv6) new IPv6()
            .setDestinationAddress(DST_IPV6.toOctets())
            .setSourceAddress(SRC_IPV6.toOctets())
            .setHopLimit((byte) 255)
            .setNextHeader(IPv6.PROTOCOL_ICMP6)
            .setPayload(ICMP6);

    private static final MPLS MPLS_IPV4 = new MPLS();
    private static final MPLS MPLS_BOS_IPV4 = new MPLS();
    private static final MPLS MPLS_IPV6 = new MPLS();
    private static final MPLS MPLS_BOS_IPV6 = new MPLS();

    private static final Ethernet ETH_IPV4 = (Ethernet) new Ethernet()
            .setEtherType(Ethernet.MPLS_UNICAST)
            .setDestinationMACAddress(DST_MAC)
            .setSourceMACAddress(SRC_MAC)
            .setPayload(MPLS_IPV4);

    private static final Ethernet ETH_IPV6 = (Ethernet) new Ethernet()
            .setEtherType(Ethernet.MPLS_UNICAST)
            .setDestinationMACAddress(DST_MAC)
            .setSourceMACAddress(SRC_MAC)
            .setPayload(MPLS_IPV6);

    @Before
    public void setUp() throws Exception {
        // Setup packets
        deserializer = MPLS.deserializer();

        byte[] ipv4 = IPV4.serialize();
        ByteBuffer bb = ByteBuffer.allocate(MPLS.HEADER_LENGTH + IPV4.getTotalLength());
        bb.putInt(((label & 0x000fffff) << 12) | ((bos & 0x1) << 8 | (ttl & 0xff)));
        bb.put(ipv4);

        bytes = bb.array();

        bb = ByteBuffer.allocate(MPLS.HEADER_LENGTH);
        bb.putInt(((label & 0x000fffff) << 12) | ((bos & 0x1) << 8 | (ttl & 0xff)));

        truncatedBytes = bb.array();

        MPLS_BOS_IPV4.setLabel(101);
        MPLS_BOS_IPV4.setPayload(IPV4);
        MPLS_IPV4.setLabel(1);
        MPLS_IPV4.setPayload(MPLS_BOS_IPV4);
        ETH_IPV4.setPayload(MPLS_IPV4);

        MPLS_BOS_IPV6.setLabel(201);
        MPLS_BOS_IPV6.setPayload(IPV6);
        MPLS_IPV6.setLabel(2);
        MPLS_IPV6.setPayload(MPLS_BOS_IPV6);
        ETH_IPV6.setPayload(MPLS_IPV6);
    }

    @Test
    public void testDeserializeBadInput() throws Exception {
        PacketTestUtils.testDeserializeBadInput(deserializer);
    }

    @Test
    public void testDeserializeTruncated() throws Exception {
        PacketTestUtils.testDeserializeTruncated(deserializer, truncatedBytes);
    }

    /**
     * Tests deserialize and getters.
     */
    @Test
    public void testDeserialize() throws Exception {
        MPLS mpls = deserializer.deserialize(bytes, 0, bytes.length);

        assertEquals(label, mpls.label);
        assertEquals(bos, mpls.bos);
        assertEquals(ttl, mpls.ttl);
        assertEquals(protocol, mpls.protocol);
    }

    /**
     * Tests toString.
     */
    @Test
    public void testToStringMpls() throws Exception {
        MPLS mpls = deserializer.deserialize(bytes, 0, bytes.length);
        String str = mpls.toString();

        assertTrue(StringUtils.contains(str, "label=" + label));
        assertTrue(StringUtils.contains(str, "bos=" + bos));
        assertTrue(StringUtils.contains(str, "ttl=" + ttl));
        assertTrue(StringUtils.contains(str, "protocol=" + protocol));
    }

    @Test
    public void testIpv4OverMplsDeserialize() throws Exception {
        // Serialize
        byte[] packet = ETH_IPV4.serialize();
        assertThat(MPLS_IPV4.protocol, is(PROTOCOL_MPLS));
        assertThat(MPLS_IPV4.bos, is((byte) 0));
        assertThat(MPLS_BOS_IPV4.protocol, is(PROTOCOL_IPV4));
        assertThat(MPLS_BOS_IPV4.bos, is((byte) 1));
        // Deserialize
        Ethernet ethernet;
        ethernet = Ethernet.deserializer().deserialize(packet, 0, packet.length);

        // Verify
        assertNotNull(ethernet);
        assertThat(ethernet.getSourceMAC(), is(ETH_IPV4.getSourceMAC()));
        assertThat(ethernet.getDestinationMAC(), is(ETH_IPV4.getDestinationMAC()));
        assertThat(ethernet.getEtherType(), is(Ethernet.MPLS_UNICAST));
        assertTrue(ethernet.getPayload() instanceof MPLS);
        MPLS mpls = (MPLS) ethernet.getPayload();
        assertThat(mpls.getLabel(), is(1));
        assertThat(mpls.getTtl(), is((byte) 0));
        assertThat(mpls.protocol, is(PROTOCOL_MPLS));
        assertThat(mpls.bos, is((byte) 0));
        assertTrue(mpls.getPayload() instanceof MPLS);
        mpls = (MPLS) mpls.getPayload();
        assertThat(mpls.getLabel(), is(101));
        assertThat(mpls.getTtl(), is((byte) 0));
        assertThat(mpls.protocol, is(PROTOCOL_IPV4));
        assertThat(mpls.bos, is((byte) 1));
        IPv4 ip = (IPv4) mpls.getPayload();
        assertThat(ip.getSourceAddress(), is(SRC_IPV4.toInt()));
        assertThat(ip.getDestinationAddress(), is(DST_IPV4.toInt()));
        assertTrue(ip.getPayload() instanceof ICMP);
        ICMP icmp = (ICMP) ip.getPayload();
        assertThat(icmp.getIcmpType(), is(TYPE_ECHO_REQUEST));
    }

    @Test
    public void testIpv6OverMplsDeserialize() throws Exception {
        // Serialize
        byte[] packet = ETH_IPV6.serialize();
        assertThat(MPLS_IPV6.protocol, is(PROTOCOL_MPLS));
        assertThat(MPLS_IPV6.bos, is((byte) 0));
        assertThat(MPLS_BOS_IPV6.protocol, is(PROTOCOL_IPV6));
        assertThat(MPLS_BOS_IPV6.bos, is((byte) 1));
        // Deserialize
        Ethernet ethernet;
        ethernet = Ethernet.deserializer().deserialize(packet, 0, packet.length);

        // Verify
        assertNotNull(ethernet);
        assertThat(ethernet.getSourceMAC(), is(ETH_IPV6.getSourceMAC()));
        assertThat(ethernet.getDestinationMAC(), is(ETH_IPV6.getDestinationMAC()));
        assertThat(ethernet.getEtherType(), is(Ethernet.MPLS_UNICAST));
        assertTrue(ethernet.getPayload() instanceof MPLS);
        MPLS mpls = (MPLS) ethernet.getPayload();
        assertThat(mpls.getLabel(), is(2));
        assertThat(mpls.getTtl(), is((byte) 0));
        assertThat(mpls.protocol, is(PROTOCOL_MPLS));
        assertThat(mpls.bos, is((byte) 0));
        assertTrue(mpls.getPayload() instanceof MPLS);
        mpls = (MPLS) mpls.getPayload();
        assertThat(mpls.getLabel(), is(201));
        assertThat(mpls.getTtl(), is((byte) 0));
        assertThat(mpls.protocol, is(PROTOCOL_IPV6));
        assertThat(mpls.bos, is((byte) 1));
        IPv6 ip = (IPv6) mpls.getPayload();
        assertThat(ip.getSourceAddress(), is(SRC_IPV6.toOctets()));
        assertThat(ip.getDestinationAddress(), is(DST_IPV6.toOctets()));
        assertTrue(ip.getPayload() instanceof ICMP6);
        ICMP6 icmp = (ICMP6) ip.getPayload();
        assertThat(icmp.getIcmpType(), is(ECHO_REQUEST));
    }
}

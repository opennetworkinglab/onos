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
package org.onlab.packet.ndp;

import org.apache.commons.lang3.StringUtils;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;
import org.junit.BeforeClass;
import org.junit.Test;
import org.onlab.packet.DeserializationException;
import org.onlab.packet.Deserializer;
import org.onlab.packet.Ethernet;
import org.onlab.packet.ICMP6;
import org.onlab.packet.IPv6;
import org.onlab.packet.Ip6Address;
import org.onlab.packet.MacAddress;
import org.onlab.packet.PacketTestUtils;

import java.nio.ByteBuffer;
import java.util.Arrays;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.core.IsCollectionContaining.hasItem;
import static org.junit.Assert.*;

/**
 * Tests for class {@link NeighborAdvertisement}.
 */
public class NeighborAdvertisementTest {
    private static final byte[] TARGET_ADDRESS = {
        (byte) 0x20, (byte) 0x01, (byte) 0x0f, (byte) 0x18,
        (byte) 0x01, (byte) 0x13, (byte) 0x02, (byte) 0x15,
        (byte) 0xca, (byte) 0x2a, (byte) 0x14, (byte) 0xff,
        (byte) 0xfe, (byte) 0x35, (byte) 0x26, (byte) 0xce
    };
    private static final MacAddress MAC_ADDRESS =
            MacAddress.valueOf("11:22:33:44:55:66");
    private static final MacAddress MAC_ADDRESS2 =
            MacAddress.valueOf("10:20:30:40:50:60");
    private static final byte[] IPV6_SOURCE_ADDRESS = {
            (byte) 0xfe, (byte) 0x80, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
            (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x01
    };
    private static final byte[] IPV6_DESTINATION_ADDRESS = {
            (byte) 0xfe, (byte) 0x80, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
            (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x02
    };
    private static final Ip6Address IP_6_ADDRESS = Ip6Address.valueOf(IPV6_DESTINATION_ADDRESS);

    private static byte[] bytePacket;

    private Deserializer<NeighborAdvertisement> deserializer
            = NeighborAdvertisement.deserializer();

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        byte[] byteHeader = {
            (byte) 0xe0, (byte) 0x00, (byte) 0x00, (byte) 0x00,
            (byte) 0x20, (byte) 0x01, (byte) 0x0f, (byte) 0x18,
            (byte) 0x01, (byte) 0x13, (byte) 0x02, (byte) 0x15,
            (byte) 0xca, (byte) 0x2a, (byte) 0x14, (byte) 0xff,
            (byte) 0xfe, (byte) 0x35, (byte) 0x26, (byte) 0xce,
            (byte) 0x02, (byte) 0x01, (byte) 0x11, (byte) 0x22,
            (byte) 0x33, (byte) 0x44, (byte) 0x55, (byte) 0x66
        };
        bytePacket = new byte[byteHeader.length];
        System.arraycopy(byteHeader, 0, bytePacket, 0, byteHeader.length);
    }

    /**
     * Tests serialize and setters.
     */
    @Test
    public void testSerialize() {
        NeighborAdvertisement na = new NeighborAdvertisement();
        na.setRouterFlag((byte) 1);
        na.setSolicitedFlag((byte) 1);
        na.setOverrideFlag((byte) 1);
        na.setTargetAddress(TARGET_ADDRESS);
        na.addOption(NeighborDiscoveryOptions.TYPE_TARGET_LL_ADDRESS,
                     MAC_ADDRESS.toBytes());

        assertArrayEquals(na.serialize(), bytePacket);
    }

    @Test
    public void testDeserializeBadInput() throws Exception {
        PacketTestUtils.testDeserializeBadInput(NeighborAdvertisement.deserializer());
    }

    @Test
    public void testDeserializeTruncated() throws Exception {
        // Run the truncation test only on the NeighborAdvertisement header
        byte[] naHeader = new byte[NeighborAdvertisement.HEADER_LENGTH];
        ByteBuffer.wrap(bytePacket).get(naHeader);

        PacketTestUtils.testDeserializeTruncated(NeighborAdvertisement.deserializer(), naHeader);
    }

    /**
     * Tests deserialize and getters.
     */
    @Test
    public void testDeserialize() throws DeserializationException {
        NeighborAdvertisement na = deserializer.deserialize(bytePacket, 0, bytePacket.length);

        assertThat(na.getRouterFlag(), is((byte) 1));
        assertThat(na.getSolicitedFlag(), is((byte) 1));
        assertThat(na.getOverrideFlag(), is((byte) 1));
        assertArrayEquals(na.getTargetAddress(), TARGET_ADDRESS);

        // Check the option(s)
        assertThat(na.getOptions().size(), is(1));
        NeighborDiscoveryOptions.Option option = na.getOptions().get(0);
        assertThat(option.type(),
                   is(NeighborDiscoveryOptions.TYPE_TARGET_LL_ADDRESS));
        assertArrayEquals(option.data(), MAC_ADDRESS.toBytes());
    }

    /**
     * Tests comparator.
     */
    @Test
    public void testEqual() {
        NeighborAdvertisement na1 = new NeighborAdvertisement();
        na1.setRouterFlag((byte) 1);
        na1.setSolicitedFlag((byte) 1);
        na1.setOverrideFlag((byte) 1);
        na1.setTargetAddress(TARGET_ADDRESS);
        na1.addOption(NeighborDiscoveryOptions.TYPE_TARGET_LL_ADDRESS,
                      MAC_ADDRESS.toBytes());

        NeighborAdvertisement na2 = new NeighborAdvertisement();
        na2.setRouterFlag((byte) 1);
        na2.setSolicitedFlag((byte) 1);
        na2.setOverrideFlag((byte) 0);
        na2.setTargetAddress(TARGET_ADDRESS);
        na2.addOption(NeighborDiscoveryOptions.TYPE_TARGET_LL_ADDRESS,
                      MAC_ADDRESS.toBytes());

        assertTrue(na1.equals(na1));
        assertFalse(na1.equals(na2));
    }

    /**
     * Tests toString.
     */
    @Test
    public void testToStringNA() throws Exception {
        NeighborAdvertisement na = deserializer.deserialize(bytePacket, 0, bytePacket.length);
        String str = na.toString();

        assertTrue(StringUtils.contains(str, "routerFlag=" + (byte) 1));
        assertTrue(StringUtils.contains(str, "solicitedFlag=" + (byte) 1));
        assertTrue(StringUtils.contains(str, "overrideFlag=" + (byte) 1));
        // TODO: need to handle TARGET_ADDRESS
    }

    /**
     * Test Neighbor Advertisement reply build.
     */
    @Test
    public void testBuildNdpAdv() {
        Ethernet eth = new Ethernet();
        eth.setSourceMACAddress(MAC_ADDRESS);
        eth.setDestinationMACAddress(MAC_ADDRESS2);

        IPv6 ipv6 = new IPv6();
        ipv6.setSourceAddress(IPV6_SOURCE_ADDRESS);
        ipv6.setDestinationAddress(IPV6_DESTINATION_ADDRESS);
        ipv6.setNextHeader(IPv6.PROTOCOL_ICMP6);

        eth.setEtherType(Ethernet.TYPE_IPV6);
        eth.setPayload(ipv6);

        ICMP6 icmp6 = new ICMP6();
        icmp6.setIcmpType(ICMP6.NEIGHBOR_SOLICITATION);
        icmp6.setIcmpCode(NeighborAdvertisement.RESERVED_CODE);
        ipv6.setPayload(icmp6);

        final Ethernet ethResponse = NeighborAdvertisement.buildNdpAdv(IP_6_ADDRESS, MAC_ADDRESS2, eth);

        assertTrue(ethResponse.getDestinationMAC().equals(MAC_ADDRESS));
        assertTrue(ethResponse.getSourceMAC().equals(MAC_ADDRESS2));
        assertTrue(ethResponse.getEtherType() == Ethernet.TYPE_IPV6);

        final IPv6 responseIpv6 = (IPv6) ethResponse.getPayload();

        assertArrayEquals(responseIpv6.getSourceAddress(), ipv6.getDestinationAddress());
        assertArrayEquals(responseIpv6.getDestinationAddress(), ipv6.getSourceAddress());
        assertTrue(responseIpv6.getNextHeader() == IPv6.PROTOCOL_ICMP6);

        final ICMP6 responseIcmp6 = (ICMP6) responseIpv6.getPayload();

        assertTrue(responseIcmp6.getIcmpType() == ICMP6.NEIGHBOR_ADVERTISEMENT);
        assertTrue(responseIcmp6.getIcmpCode() == NeighborAdvertisement.RESERVED_CODE);

        final NeighborAdvertisement responseNadv = (NeighborAdvertisement) responseIcmp6.getPayload();

        assertArrayEquals(responseNadv.getTargetAddress(), IPV6_DESTINATION_ADDRESS);
        assertTrue(responseNadv.getSolicitedFlag() == NeighborAdvertisement.NDP_SOLICITED_FLAG);
        assertTrue(responseNadv.getOverrideFlag() == NeighborAdvertisement.NDP_OVERRIDE_FLAG);
        assertThat(responseNadv.getOptions(),
                hasItem(hasOption(NeighborDiscoveryOptions.TYPE_TARGET_LL_ADDRESS, MAC_ADDRESS2.toBytes())));
    }

    private NeighborDiscoveryOptionMatcher hasOption(byte type, byte[] data) {
        return new NeighborDiscoveryOptionMatcher(type, data);
    }

    private static class NeighborDiscoveryOptionMatcher extends TypeSafeMatcher<NeighborDiscoveryOptions.Option> {

        private final byte type;
        private final byte[] data;
        private String reason = "";

        NeighborDiscoveryOptionMatcher(byte type, byte[] data) {
            this.type = type;
            this.data = data;
        }

        @Override
        protected boolean matchesSafely(NeighborDiscoveryOptions.Option option) {
            if (type != option.type()) {
                reason = "Wrong Option type";
                return false;
            }
            if (!Arrays.equals(data, option.data())) {
                reason = "Wrong Option data";
                return false;
            }
            return true;
        }

        @Override
        public void describeTo(Description description) {
            description.appendText(reason);
        }
    }
}

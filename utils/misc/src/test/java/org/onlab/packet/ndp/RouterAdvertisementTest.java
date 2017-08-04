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
import org.junit.BeforeClass;
import org.junit.Test;
import org.onlab.packet.DeserializationException;
import org.onlab.packet.Deserializer;
import org.onlab.packet.MacAddress;
import org.onlab.packet.PacketTestUtils;

import java.nio.ByteBuffer;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

/**
 * Tests for class {@link RouterAdvertisement}.
 */
public class RouterAdvertisementTest {
    private static final MacAddress MAC_ADDRESS =
        MacAddress.valueOf("11:22:33:44:55:66");

    private static byte[] bytePacket;

    private Deserializer<RouterAdvertisement> deserializer
            = RouterAdvertisement.deserializer();

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        byte[] byteHeader = {
            (byte) 0x03, (byte) 0xc0, (byte) 0x02, (byte) 0x58,
            (byte) 0x00, (byte) 0x00, (byte) 0x03, (byte) 0xe8,
            (byte) 0x00, (byte) 0x00, (byte) 0x01, (byte) 0xf4,
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
        RouterAdvertisement ra = new RouterAdvertisement();
        ra.setCurrentHopLimit((byte) 3);
        ra.setMFlag((byte) 1);
        ra.setOFlag((byte) 1);
        ra.setRouterLifetime((short) 0x258);
        ra.setReachableTime(0x3e8);
        ra.setRetransmitTimer(0x1f4);
        ra.addOption(NeighborDiscoveryOptions.TYPE_TARGET_LL_ADDRESS,
                     MAC_ADDRESS.toBytes());

        assertArrayEquals(ra.serialize(), bytePacket);
    }

    @Test
    public void testDeserializeBadInput() throws Exception {
        PacketTestUtils.testDeserializeBadInput(RouterAdvertisement.deserializer());
    }

    @Test
    public void testDeserializeTruncated() throws Exception {
        // Run the truncation test only on the RouterAdvertisement header
        byte[] raHeader = new byte[RouterAdvertisement.HEADER_LENGTH];
        ByteBuffer.wrap(bytePacket).get(raHeader);

        PacketTestUtils.testDeserializeTruncated(RouterAdvertisement.deserializer(), raHeader);
    }

    /**
     * Tests deserialize and getters.
     */
    @Test
    public void testDeserialize() throws DeserializationException {
        RouterAdvertisement ra = deserializer.deserialize(bytePacket, 0, bytePacket.length);

        assertThat(ra.getCurrentHopLimit(), is((byte) 3));
        assertThat(ra.getMFlag(), is((byte) 1));
        assertThat(ra.getOFlag(), is((byte) 1));
        assertThat(ra.getRouterLifetime(), is((short) 0x258));
        assertThat(ra.getReachableTime(), is(0x3e8));
        assertThat(ra.getRetransmitTimer(), is(0x1f4));

        // Check the option(s)
        assertThat(ra.getOptions().size(), is(1));
        NeighborDiscoveryOptions.Option option = ra.getOptions().get(0);
        assertThat(option.type(),
                   is(NeighborDiscoveryOptions.TYPE_TARGET_LL_ADDRESS));
        assertArrayEquals(option.data(), MAC_ADDRESS.toBytes());
    }

    /**
     * Tests comparator.
     */
    @Test
    public void testEqual() {
        RouterAdvertisement ra1 = new RouterAdvertisement();
        ra1.setCurrentHopLimit((byte) 3);
        ra1.setMFlag((byte) 1);
        ra1.setOFlag((byte) 1);
        ra1.setRouterLifetime((short) 0x258);
        ra1.setReachableTime(0x3e8);
        ra1.setRetransmitTimer(0x1f4);
        ra1.addOption(NeighborDiscoveryOptions.TYPE_TARGET_LL_ADDRESS,
                      MAC_ADDRESS.toBytes());

        RouterAdvertisement ra2 = new RouterAdvertisement();
        ra2.setCurrentHopLimit((byte) 3);
        ra2.setMFlag((byte) 0);
        ra2.setOFlag((byte) 0);
        ra2.setRouterLifetime((short) 0x1f4);
        ra2.setReachableTime(0x3e8);
        ra2.setRetransmitTimer(0x1f4);
        ra2.addOption(NeighborDiscoveryOptions.TYPE_TARGET_LL_ADDRESS,
                      MAC_ADDRESS.toBytes());

        assertTrue(ra1.equals(ra1));
        assertFalse(ra1.equals(ra2));
    }

    /**
     * Tests toString.
     */
    @Test
    public void testToStringRA() throws Exception {
        RouterAdvertisement ra = deserializer.deserialize(bytePacket, 0, bytePacket.length);
        String str = ra.toString();

        assertTrue(StringUtils.contains(str, "currentHopLimit=" + (byte) 3));
        assertTrue(StringUtils.contains(str, "mFlag=" + (byte) 1));
        assertTrue(StringUtils.contains(str, "oFlag=" + (byte) 1));
        assertTrue(StringUtils.contains(str, "routerLifetime=" + (short) 0x258));
        assertTrue(StringUtils.contains(str, "reachableTime=" + 0x3e8));
        assertTrue(StringUtils.contains(str, "retransmitTimer=" + 0x1f4));

        // TODO: need to handle options
    }
}

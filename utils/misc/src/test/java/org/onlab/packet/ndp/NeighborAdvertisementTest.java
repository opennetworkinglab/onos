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
}

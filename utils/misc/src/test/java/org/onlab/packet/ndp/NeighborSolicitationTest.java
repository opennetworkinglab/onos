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
 * Tests for class {@link NeighborSolicitation}.
 */
public class NeighborSolicitationTest {
    private static final byte[] TARGET_ADDRESS = {
        (byte) 0x20, (byte) 0x01, (byte) 0x0f, (byte) 0x18,
        (byte) 0x01, (byte) 0x13, (byte) 0x02, (byte) 0x15,
        (byte) 0xca, (byte) 0x2a, (byte) 0x14, (byte) 0xff,
        (byte) 0xfe, (byte) 0x35, (byte) 0x26, (byte) 0xce
    };
    private static final byte[] TARGET_ADDRESS2 = {
        (byte) 0x20, (byte) 0x01, (byte) 0x0f, (byte) 0x18,
        (byte) 0x01, (byte) 0x13, (byte) 0x02, (byte) 0x15,
        (byte) 0xe6, (byte) 0xce, (byte) 0x8f, (byte) 0xff,
        (byte) 0xfe, (byte) 0x54, (byte) 0x37, (byte) 0xc8
    };
    private static final MacAddress MAC_ADDRESS =
        MacAddress.valueOf("11:22:33:44:55:66");

    private static byte[] bytePacket;

    private Deserializer<NeighborSolicitation> deserializer
            = NeighborSolicitation.deserializer();

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        byte[] byteHeader = {
            (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
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
        NeighborSolicitation ns = new NeighborSolicitation();
        ns.setTargetAddress(TARGET_ADDRESS);
        ns.addOption(NeighborDiscoveryOptions.TYPE_TARGET_LL_ADDRESS,
                     MAC_ADDRESS.toBytes());

        assertArrayEquals(ns.serialize(), bytePacket);
    }

    @Test
    public void testDeserializeBadInput() throws Exception {
        PacketTestUtils.testDeserializeBadInput(NeighborSolicitation.deserializer());
    }

    @Test
    public void testDeserializeTruncated() throws Exception {
        // Run the truncation test only on the NeighborSolicitation header
        byte[] nsHeader = new byte[NeighborSolicitation.HEADER_LENGTH];
        ByteBuffer.wrap(bytePacket).get(nsHeader);

        PacketTestUtils.testDeserializeTruncated(NeighborSolicitation.deserializer(), nsHeader);
    }

    /**
     * Tests deserialize and getters.
     */
    @Test
    public void testDeserialize() throws DeserializationException {
        NeighborSolicitation ns = deserializer.deserialize(bytePacket, 0, bytePacket.length);

        assertArrayEquals(ns.getTargetAddress(), TARGET_ADDRESS);

        // Check the option(s)
        assertThat(ns.getOptions().size(), is(1));
        NeighborDiscoveryOptions.Option option = ns.getOptions().get(0);
        assertThat(option.type(),
                   is(NeighborDiscoveryOptions.TYPE_TARGET_LL_ADDRESS));
        assertArrayEquals(option.data(), MAC_ADDRESS.toBytes());
    }

    /**
     * Tests comparator.
     */
    @Test
    public void testEqual() {
        NeighborSolicitation ns1 = new NeighborSolicitation();
        ns1.setTargetAddress(TARGET_ADDRESS);
        ns1.addOption(NeighborDiscoveryOptions.TYPE_TARGET_LL_ADDRESS,
                      MAC_ADDRESS.toBytes());

        NeighborSolicitation ns2 = new NeighborSolicitation();
        ns2.setTargetAddress(TARGET_ADDRESS2);
        ns2.addOption(NeighborDiscoveryOptions.TYPE_TARGET_LL_ADDRESS,
                      MAC_ADDRESS.toBytes());

        assertTrue(ns1.equals(ns1));
        assertFalse(ns1.equals(ns2));
    }

    /**
     * Tests toString.
     */
    @Test
    public void testToStringNS() throws Exception {
        NeighborSolicitation ns = deserializer.deserialize(bytePacket, 0, bytePacket.length);
        String str = ns.toString();

        // TODO: need to handle TARGET_ADDRESS and Options
    }
}

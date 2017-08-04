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

import org.junit.BeforeClass;
import org.junit.Test;
import org.onlab.packet.Deserializer;
import org.onlab.packet.MacAddress;
import org.onlab.packet.PacketTestUtils;

import java.nio.ByteBuffer;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.*;

/**
 * Tests for class {@link RouterSolicitation}.
 */
public class RouterSolicitationTest {
    private static final MacAddress MAC_ADDRESS1 =
        MacAddress.valueOf("11:22:33:44:55:66");
    private static final MacAddress MAC_ADDRESS2 =
        MacAddress.valueOf("11:22:33:44:55:00");

    private static byte[] bytePacket;

    private Deserializer<RouterSolicitation> deserializer
            = RouterSolicitation.deserializer();

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        byte[] byteHeader = {
            (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
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
        RouterSolicitation rs = new RouterSolicitation();
        rs.addOption(NeighborDiscoveryOptions.TYPE_TARGET_LL_ADDRESS,
                     MAC_ADDRESS1.toBytes());

        assertArrayEquals(rs.serialize(), bytePacket);
    }

    @Test
    public void testDeserializeBadInput() throws Exception {
        PacketTestUtils.testDeserializeBadInput(RouterSolicitation.deserializer());
    }

    @Test
    public void testDeserializeTruncated() throws Exception {
        // Run the truncation test only on the RouterSolicitation header
        byte[] rsHeader = new byte[RouterSolicitation.HEADER_LENGTH];
        ByteBuffer.wrap(bytePacket).get(rsHeader);

        PacketTestUtils.testDeserializeTruncated(RouterSolicitation.deserializer(), rsHeader);
    }

    /**
     * Tests deserialize and getters.
     */
    @Test
    public void testDeserialize() throws Exception {
        RouterSolicitation rs = deserializer.deserialize(bytePacket, 0, bytePacket.length);

        // Check the option(s)
        assertThat(rs.getOptions().size(), is(1));
        NeighborDiscoveryOptions.Option option = rs.getOptions().get(0);
        assertThat(option.type(),
                   is(NeighborDiscoveryOptions.TYPE_TARGET_LL_ADDRESS));
        assertArrayEquals(option.data(), MAC_ADDRESS1.toBytes());
    }

    /**
     * Tests comparator.
     */
    @Test
    public void testEqual() {
        RouterSolicitation rs1 = new RouterSolicitation();
        rs1.addOption(NeighborDiscoveryOptions.TYPE_TARGET_LL_ADDRESS,
                      MAC_ADDRESS1.toBytes());

        RouterSolicitation rs2 = new RouterSolicitation();
        rs2.addOption(NeighborDiscoveryOptions.TYPE_TARGET_LL_ADDRESS,
                      MAC_ADDRESS2.toBytes());

        assertTrue(rs1.equals(rs1));
        assertFalse(rs1.equals(rs2));
    }

    /**
     * Tests toString.
     */
    @Test
    public void testToStringRS() throws Exception {
        RouterSolicitation rs = deserializer.deserialize(bytePacket, 0, bytePacket.length);
        String str = rs.toString();

        // TODO: need to handle Options
    }
}

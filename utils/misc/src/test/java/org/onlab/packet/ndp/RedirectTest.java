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
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

/**
 * Tests for class {@link Redirect}.
 */
public class RedirectTest {
    private static final byte[] TARGET_ADDRESS = {
        (byte) 0x20, (byte) 0x01, (byte) 0x0f, (byte) 0x18,
        (byte) 0x01, (byte) 0x13, (byte) 0x02, (byte) 0x15,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00
    };
    private static final byte[] DESTINATION_ADDRESS = {
        (byte) 0x20, (byte) 0x01, (byte) 0x0f, (byte) 0x18,
        (byte) 0x01, (byte) 0x13, (byte) 0x02, (byte) 0x15,
        (byte) 0xca, (byte) 0x2a, (byte) 0x14, (byte) 0xff,
        (byte) 0xfe, (byte) 0x35, (byte) 0x26, (byte) 0xce
    };
    private static final byte[] DESTINATION_ADDRESS2 = {
        (byte) 0x20, (byte) 0x01, (byte) 0x0f, (byte) 0x18,
        (byte) 0x01, (byte) 0x13, (byte) 0x02, (byte) 0x15,
        (byte) 0xe6, (byte) 0xce, (byte) 0x8f, (byte) 0xff,
        (byte) 0xfe, (byte) 0x54, (byte) 0x37, (byte) 0xc8
    };
    private static final MacAddress MAC_ADDRESS =
        MacAddress.valueOf("11:22:33:44:55:66");

    private static byte[] bytePacket;

    private Deserializer<Redirect> deserializer = Redirect.deserializer();

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        byte[] byteHeader = {
            (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
            (byte) 0x20, (byte) 0x01, (byte) 0x0f, (byte) 0x18,
            (byte) 0x01, (byte) 0x13, (byte) 0x02, (byte) 0x15,
            (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
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
        Redirect rd = new Redirect();
        rd.setTargetAddress(TARGET_ADDRESS);
        rd.setDestinationAddress(DESTINATION_ADDRESS);
        rd.addOption(NeighborDiscoveryOptions.TYPE_TARGET_LL_ADDRESS,
                     MAC_ADDRESS.toBytes());

        assertArrayEquals(rd.serialize(), bytePacket);
    }

    @Test
    public void testDeserializeBadInput() throws Exception {
        PacketTestUtils.testDeserializeBadInput(Redirect.deserializer());
    }

    @Test
    public void testDeserializeTruncated() throws Exception {
        // Run the truncation test only on the Redirect header
        byte[] rdHeader = new byte[Redirect.HEADER_LENGTH];
        ByteBuffer.wrap(bytePacket).get(rdHeader);

        PacketTestUtils.testDeserializeTruncated(Redirect.deserializer(), rdHeader);
    }

    /**
     * Tests deserialize and getters.
     */
    @Test
    public void testDeserialize() throws DeserializationException {
        Redirect rd = deserializer.deserialize(bytePacket, 0, bytePacket.length);

        assertArrayEquals(rd.getTargetAddress(), TARGET_ADDRESS);
        assertArrayEquals(rd.getDestinationAddress(), DESTINATION_ADDRESS);

        // Check the option(s)
        assertThat(rd.getOptions().size(), is(1));
        NeighborDiscoveryOptions.Option option = rd.getOptions().get(0);
        assertThat(option.type(),
                   is(NeighborDiscoveryOptions.TYPE_TARGET_LL_ADDRESS));
        assertArrayEquals(option.data(), MAC_ADDRESS.toBytes());
    }

    /**
     * Tests comparator.
     */
    @Test
    public void testEqual() {
        Redirect rd1 = new Redirect();
        rd1.setTargetAddress(TARGET_ADDRESS);
        rd1.setDestinationAddress(DESTINATION_ADDRESS);
        rd1.addOption(NeighborDiscoveryOptions.TYPE_TARGET_LL_ADDRESS,
                      MAC_ADDRESS.toBytes());

        Redirect rd2 = new Redirect();
        rd2.setTargetAddress(TARGET_ADDRESS);
        rd2.setDestinationAddress(DESTINATION_ADDRESS2);
        rd2.addOption(NeighborDiscoveryOptions.TYPE_TARGET_LL_ADDRESS,
                      MAC_ADDRESS.toBytes());

        assertTrue(rd1.equals(rd1));
        assertFalse(rd1.equals(rd2));
    }

    /**
     * Tests toString.
     */
    @Test
    public void testToStringRedirect() throws Exception {
        Redirect rd = deserializer.deserialize(bytePacket, 0, bytePacket.length);
        String str = rd.toString();

        // TODO: need to handle TARGET_ADDRESS and DESTINATION_ADDRESS
    }
}

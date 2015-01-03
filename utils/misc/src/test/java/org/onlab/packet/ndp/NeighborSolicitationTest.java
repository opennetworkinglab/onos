/*
 * Copyright 2014 Open Networking Laboratory
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
import org.onlab.packet.Data;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;
/**
 * Tests for class {@link NeighborSolicitation}.
 */
public class NeighborSolicitationTest {
    private static final byte[] TARGET_ADDRESS = {
            (byte) 0x20, (byte) 0x01, (byte) 0x0f, (byte) 0x18, (byte) 0x01, (byte) 0x13, (byte) 0x02, (byte) 0x15,
            (byte) 0xca, (byte) 0x2a, (byte) 0x14, (byte) 0xff, (byte) 0xfe, (byte) 0x35, (byte) 0x26, (byte) 0xce
    };
    private static final byte[] TARGET_ADDRESS2 = {
            (byte) 0x20, (byte) 0x01, (byte) 0x0f, (byte) 0x18, (byte) 0x01, (byte) 0x13, (byte) 0x02, (byte) 0x15,
            (byte) 0xe6, (byte) 0xce, (byte) 0x8f, (byte) 0xff, (byte) 0xfe, (byte) 0x54, (byte) 0x37, (byte) 0xc8
    };
    private static Data data;
    private static byte[] bytePacket;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        data = new Data();
        data.setData("".getBytes());

        byte[] bytePayload = data.serialize();
        byte[] byteHeader = {
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x20, (byte) 0x01, (byte) 0x0f, (byte) 0x18, (byte) 0x01, (byte) 0x13, (byte) 0x02, (byte) 0x15,
                (byte) 0xca, (byte) 0x2a, (byte) 0x14, (byte) 0xff, (byte) 0xfe, (byte) 0x35, (byte) 0x26, (byte) 0xce
        };
        bytePacket = new byte[byteHeader.length + bytePayload.length];
        System.arraycopy(byteHeader, 0, bytePacket, 0, byteHeader.length);
        System.arraycopy(bytePayload, 0, bytePacket, byteHeader.length, bytePayload.length);
    }

    /**
     * Tests serialize and setters.
     */
    @Test
    public void testSerialize() {
        NeighborSolicitation ns = new NeighborSolicitation();
        ns.setTargetAddress(TARGET_ADDRESS);
        ns.setPayload(data);

        assertArrayEquals(ns.serialize(), bytePacket);
    }

    /**
     * Tests deserialize and getters.
     */
    @Test
    public void testDeserialize() {
        NeighborSolicitation ns = new NeighborSolicitation();
        ns.deserialize(bytePacket, 0, bytePacket.length);

        assertArrayEquals(ns.getTargetAddress(), TARGET_ADDRESS);
    }

    /**
     * Tests comparator.
     */
    @Test
    public void testEqual() {
        NeighborSolicitation ns1 = new NeighborSolicitation();
        ns1.setTargetAddress(TARGET_ADDRESS);

        NeighborSolicitation ns2 = new NeighborSolicitation();
        ns2.setTargetAddress(TARGET_ADDRESS2);

        assertTrue(ns1.equals(ns1));
        assertFalse(ns1.equals(ns2));
    }
}

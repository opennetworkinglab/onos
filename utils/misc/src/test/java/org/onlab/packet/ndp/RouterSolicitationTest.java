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

/**
 * Tests for class {@link RouterSolicitation}.
 */
public class RouterSolicitationTest {
    private static final byte[] OPTION = {
        (byte) 0x01, (byte) 0x08, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x01
    };
    private static Data data;
    private static byte[] bytePacket;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        data = new Data();
        data.setData(OPTION);

        byte[] bytePayload = data.serialize();
        byte[] byteHeader = {(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00};
        bytePacket = new byte[byteHeader.length + bytePayload.length];
        System.arraycopy(byteHeader, 0, bytePacket, 0, byteHeader.length);
        System.arraycopy(bytePayload, 0, bytePacket, byteHeader.length, bytePayload.length);
    }

    /**
     * Tests serialize and setters.
     */
    @Test
    public void testSerialize() {
        RouterSolicitation rs = new RouterSolicitation();
        rs.setPayload(data);

        assertArrayEquals(rs.serialize(), bytePacket);
    }

    /**
     * Tests deserialize and getters.
     */
    @Test
    public void testDeserialize() {
        RouterSolicitation rs = new RouterSolicitation();
        rs.deserialize(bytePacket, 0, bytePacket.length);
    }

    /**
     * Tests comparator.
     */
    @Test
    public void testEqual() {
        RouterSolicitation rs1 = new RouterSolicitation();

        assertTrue(rs1.equals(rs1));
    }
}

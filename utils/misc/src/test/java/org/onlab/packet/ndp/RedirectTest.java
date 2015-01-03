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
 * Tests for class {@link Redirect}.
 */
public class RedirectTest {
    private static final byte[] TARGET_ADDRESS = {
            (byte) 0x20, (byte) 0x01, (byte) 0x0f, (byte) 0x18, (byte) 0x01, (byte) 0x13, (byte) 0x02, (byte) 0x15,
            (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00
    };
    private static final byte[] DESTINATION_ADDRESS = {
            (byte) 0x20, (byte) 0x01, (byte) 0x0f, (byte) 0x18, (byte) 0x01, (byte) 0x13, (byte) 0x02, (byte) 0x15,
            (byte) 0xca, (byte) 0x2a, (byte) 0x14, (byte) 0xff, (byte) 0xfe, (byte) 0x35, (byte) 0x26, (byte) 0xce
    };
    private static final byte[] DESTINATION_ADDRESS2 = {
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
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
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
        Redirect rd = new Redirect();
        rd.setTargetAddress(TARGET_ADDRESS);
        rd.setDestinationAddress(DESTINATION_ADDRESS);
        rd.setPayload(data);

        assertArrayEquals(rd.serialize(), bytePacket);
    }

    /**
     * Tests deserialize and getters.
     */
    @Test
    public void testDeserialize() {
        Redirect rd = new Redirect();
        rd.deserialize(bytePacket, 0, bytePacket.length);

        assertArrayEquals(rd.getTargetAddress(), TARGET_ADDRESS);
        assertArrayEquals(rd.getDestinationAddress(), DESTINATION_ADDRESS);
    }

    /**
     * Tests comparator.
     */
    @Test
    public void testEqual() {
        Redirect rd1 = new Redirect();
        rd1.setTargetAddress(TARGET_ADDRESS);
        rd1.setDestinationAddress(DESTINATION_ADDRESS);

        Redirect rd2 = new Redirect();
        rd2.setTargetAddress(TARGET_ADDRESS);
        rd2.setDestinationAddress(DESTINATION_ADDRESS2);

        assertTrue(rd1.equals(rd1));
        assertFalse(rd1.equals(rd2));
    }
}

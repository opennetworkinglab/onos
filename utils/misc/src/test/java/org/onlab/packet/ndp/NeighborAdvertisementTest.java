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

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;

/**
 * Tests for class {@link NeighborAdvertisement}.
 */
public class NeighborAdvertisementTest {
    private static final byte[] TARGET_ADDRESS = {
            (byte) 0x20, (byte) 0x01, (byte) 0x0f, (byte) 0x18, (byte) 0x01, (byte) 0x13, (byte) 0x02, (byte) 0x15,
            (byte) 0xca, (byte) 0x2a, (byte) 0x14, (byte) 0xff, (byte) 0xfe, (byte) 0x35, (byte) 0x26, (byte) 0xce
    };
    private static Data data;
    private static byte[] bytePacket;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        data = new Data();
        data.setData("".getBytes());

        byte[] bytePayload = data.serialize();
        byte[] byteHeader = {
                (byte) 0xe0, (byte) 0x00, (byte) 0x00, (byte) 0x00,
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
        NeighborAdvertisement na = new NeighborAdvertisement();
        na.setRouterFlag((byte) 1);
        na.setSolicitedFlag((byte) 1);
        na.setOverrideFlag((byte) 1);
        na.setTargetAddress(TARGET_ADDRESS);
        na.setPayload(data);

        assertArrayEquals(na.serialize(), bytePacket);
    }

    /**
     * Tests deserialize and getters.
     */
    @Test
    public void testDeserialize() {
        NeighborAdvertisement na = new NeighborAdvertisement();
        na.deserialize(bytePacket, 0, bytePacket.length);

        assertThat(na.getRouterFlag(), is((byte) 1));
        assertThat(na.getSolicitedFlag(), is((byte) 1));
        assertThat(na.getOverrideFlag(), is((byte) 1));
        assertArrayEquals(na.getTargetAddress(), TARGET_ADDRESS);
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

        NeighborAdvertisement na2 = new NeighborAdvertisement();
        na2.setRouterFlag((byte) 1);
        na2.setSolicitedFlag((byte) 1);
        na2.setOverrideFlag((byte) 0);
        na2.setTargetAddress(TARGET_ADDRESS);

        assertTrue(na1.equals(na1));
        assertFalse(na1.equals(na2));
    }
}

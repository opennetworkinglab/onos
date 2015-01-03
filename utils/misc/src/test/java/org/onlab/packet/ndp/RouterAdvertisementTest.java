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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

/**
 * Tests for class {@link RouterAdvertisement}.
 */
public class RouterAdvertisementTest {
    private static Data data;
    private static byte[] bytePacket;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        data = new Data();
        data.setData("".getBytes());

        byte[] bytePayload = data.serialize();
        byte[] byteHeader = {
                (byte) 0x03, (byte) 0xc0, (byte) 0x02, (byte) 0x58,
                (byte) 0x00, (byte) 0x00, (byte) 0x03, (byte) 0xe8,
                (byte) 0x00, (byte) 0x00, (byte) 0x01, (byte) 0xf4
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
        RouterAdvertisement ra = new RouterAdvertisement();
        ra.setCurrentHopLimit((byte) 3);
        ra.setMFlag((byte) 1);
        ra.setOFlag((byte) 1);
        ra.setRouterLifetime((short) 0x258);
        ra.setReachableTime(0x3e8);
        ra.setRetransmitTimer(0x1f4);
        ra.setPayload(data);

        assertArrayEquals(ra.serialize(), bytePacket);
    }

    /**
     * Tests deserialize and getters.
     */
    @Test
    public void testDeserialize() {
        RouterAdvertisement ra = new RouterAdvertisement();
        ra.deserialize(bytePacket, 0, bytePacket.length);

        assertThat(ra.getCurrentHopLimit(), is((byte) 3));
        assertThat(ra.getMFlag(), is((byte) 1));
        assertThat(ra.getOFlag(), is((byte) 1));
        assertThat(ra.getRouterLifetime(), is((short) 0x258));
        assertThat(ra.getReachableTime(), is(0x3e8));
        assertThat(ra.getRetransmitTimer(), is(0x1f4));
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

        RouterAdvertisement ra2 = new RouterAdvertisement();
        ra2.setCurrentHopLimit((byte) 3);
        ra2.setMFlag((byte) 0);
        ra2.setOFlag((byte) 0);
        ra2.setRouterLifetime((short) 0x1f4);
        ra2.setReachableTime(0x3e8);
        ra2.setRetransmitTimer(0x1f4);

        assertTrue(ra1.equals(ra1));
        assertFalse(ra1.equals(ra2));
    }
}

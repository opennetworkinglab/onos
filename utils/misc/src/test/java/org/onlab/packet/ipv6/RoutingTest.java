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

package org.onlab.packet.ipv6;

import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.onlab.packet.Data;
import org.onlab.packet.DeserializationException;
import org.onlab.packet.Deserializer;
import org.onlab.packet.UDP;

import java.util.Arrays;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.*;

/**
 * Tests for class {@link Routing}.
 */
public class RoutingTest {
    private static Data data;
    private static UDP udp;
    private static byte[] routingData = {
            (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
            (byte) 0x20, (byte) 0x01, (byte) 0x0f, (byte) 0x18,
            (byte) 0x01, (byte) 0x13, (byte) 0x02, (byte) 0x15,
            (byte) 0xca, (byte) 0x2a, (byte) 0x14, (byte) 0xff,
            (byte) 0xfe, (byte) 0x35, (byte) 0x26, (byte) 0xce
    };
    private static byte[] bytePacket;

    private Deserializer<Routing> deserializer;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        data = new Data();
        data.setData("testSerialize".getBytes());
        udp = new UDP();
        udp.setPayload(data);

        byte[] bytePayload = udp.serialize();
        byte[] byteHeader = {
                (byte) 0x11, (byte) 0x02, (byte) 0x00, (byte) 0x03,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x20, (byte) 0x01, (byte) 0x0f, (byte) 0x18,
                (byte) 0x01, (byte) 0x13, (byte) 0x02, (byte) 0x15,
                (byte) 0xca, (byte) 0x2a, (byte) 0x14, (byte) 0xff,
                (byte) 0xfe, (byte) 0x35, (byte) 0x26, (byte) 0xce
        };
        bytePacket = new byte[byteHeader.length + bytePayload.length];
        System.arraycopy(byteHeader, 0, bytePacket, 0, byteHeader.length);
        System.arraycopy(bytePayload, 0, bytePacket, byteHeader.length, bytePayload.length);
    }

    @Before
    public void setUp() {
        deserializer = Routing.deserializer();
    }

    /**
     * Tests serialize and setters.
     */
    @Test
    public void testSerialize() {
        Routing routing = new Routing();
        routing.setNextHeader((byte) 0x11);
        routing.setHeaderExtLength((byte) 0x02);
        routing.setRoutingType((byte) 0x00);
        routing.setSegmntsLeft((byte) 0x03);
        routing.setRoutingData(routingData);
        routing.setPayload(udp);

        assertArrayEquals(routing.serialize(), bytePacket);
    }

    /**
     * Tests deserialize and getters.
     */
    @Test
    public void testDeserialize() throws DeserializationException {
        Routing routing = deserializer.deserialize(bytePacket, 0, bytePacket.length);

        assertThat(routing.getNextHeader(), is((byte) 0x11));
        assertThat(routing.getHeaderExtLength(), is((byte) 0x02));
        assertThat(routing.getRoutingType(), is((byte) 0x00));
        assertThat(routing.getSegmentsLeft(), is((byte) 0x03));
        assertArrayEquals(routing.getRoutingData(), routingData);
    }

    /**
     * Tests comparator.
     */
    @Test
    public void testEqual() {
        Routing routing1 = new Routing();
        routing1.setNextHeader((byte) 0x11);
        routing1.setHeaderExtLength((byte) 0x02);
        routing1.setRoutingType((byte) 0x00);
        routing1.setSegmntsLeft((byte) 0x03);
        routing1.setRoutingData(routingData);

        Routing routing2 = new Routing();
        routing2.setNextHeader((byte) 0x11);
        routing2.setHeaderExtLength((byte) 0x02);
        routing2.setRoutingType((byte) 0x00);
        routing2.setSegmntsLeft((byte) 0x02);
        routing2.setRoutingData(routingData);

        assertTrue(routing1.equals(routing1));
        assertFalse(routing1.equals(routing2));
    }

    /**
     * Tests toString.
     */
    @Test
    public void testToStringRouting() throws Exception {
        Routing routing = deserializer.deserialize(bytePacket, 0, bytePacket.length);
        String str = routing.toString();

        assertTrue(StringUtils.contains(str, "nextHeader=" + (byte) 0x11));
        assertTrue(StringUtils.contains(str, "headerExtLength=" + (byte) 0x02));
        assertTrue(StringUtils.contains(str, "routingType=" + (byte) 0x00));
        assertTrue(StringUtils.contains(str, "segmentsLeft=" + (byte) 0x03));
        assertTrue(StringUtils.contains(str, "routingData=" + Arrays.toString(routingData)));
    }
}

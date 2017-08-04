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
import org.onlab.packet.Deserializer;
import org.onlab.packet.IPv6;
import org.onlab.packet.UDP;

import java.util.Arrays;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.*;

/**
 * Tests for class {@link BaseOptions}.
 */
public class BaseOptionsTest {
    private static Data data;
    private static UDP udp;
    private static byte[] options = {
            (byte) 0x00, (byte) 0x03,
            (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0x00
    };
    private static byte[] bytePacket;

    private Deserializer<BaseOptions> deserializer;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        data = new Data();
        data.setData("testSerialize".getBytes());
        udp = new UDP();
        udp.setPayload(data);

        byte[] bytePayload = udp.serialize();
        byte[] byteHeader = {
                (byte) 0x11, (byte) 0x00, (byte) 0x00, (byte) 0x03,
                (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0x00
        };
        bytePacket = new byte[byteHeader.length + bytePayload.length];
        System.arraycopy(byteHeader, 0, bytePacket, 0, byteHeader.length);
        System.arraycopy(bytePayload, 0, bytePacket, byteHeader.length, bytePayload.length);
    }

    @Before
    public void setUp() {
        deserializer = BaseOptions.deserializer();
    }

    /**
     * Tests serialize and setters.
     */
    @Test
    public void testSerialize() {
        BaseOptions baseopt = new BaseOptions();
        baseopt.setNextHeader((byte) 0x11);
        baseopt.setHeaderExtLength((byte) 0x00);
        baseopt.setOptions(options);
        baseopt.setPayload(udp);

        assertArrayEquals(baseopt.serialize(), bytePacket);
    }

    /**
     * Tests deserialize and getters.
     */
    @Test
    public void testDeserialize() throws Exception {
        BaseOptions baseopt = deserializer.deserialize(bytePacket, 0, bytePacket.length);

        assertThat(baseopt.getNextHeader(), is((byte) 0x11));
        assertThat(baseopt.getHeaderExtLength(), is((byte) 0x00));
        assertArrayEquals(baseopt.getOptions(), options);
    }

    /**
     * Tests comparator.
     */
    @Test
    public void testEqual() {
        BaseOptions baseopt1 = new BaseOptions();
        baseopt1.setNextHeader((byte) 0x11);
        baseopt1.setHeaderExtLength((byte) 0x00);
        baseopt1.setOptions(options);
        baseopt1.setType(IPv6.PROTOCOL_HOPOPT);

        BaseOptions baseopt2 = new BaseOptions();
        baseopt2.setNextHeader((byte) 0x11);
        baseopt2.setHeaderExtLength((byte) 0x00);
        baseopt2.setOptions(options);
        baseopt1.setType(IPv6.PROTOCOL_DSTOPT);

        assertTrue(baseopt1.equals(baseopt1));
        assertFalse(baseopt1.equals(baseopt2));
    }

    /**
     * Tests toString.
     */
    @Test
    public void testToStringBaseOptions() throws Exception {
        BaseOptions baseopt = deserializer.deserialize(bytePacket, 0, bytePacket.length);
        String str = baseopt.toString();

        assertTrue(StringUtils.contains(str, "nextHeader=" + (byte) 0x11));
        assertTrue(StringUtils.contains(str, "headerExtLength=" + (byte) 0x00));
        assertTrue(StringUtils.contains(str, "options=" + Arrays.toString(options)));
    }
}

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

import java.util.Arrays;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

/**
 * Tests for class {@link EncapSecurityPayload}.
 */
public class EncapSecurityPayloadTest {
    private static Data data;
    private static byte[] dataByte = new byte[32];
    private static byte[] bytePacket;

    private Deserializer<EncapSecurityPayload> deserializer;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        Arrays.fill(dataByte, (byte) 0xff);
        data = new Data().setData(dataByte);

        byte[] bytePayload = data.serialize();
        byte[] byteHeader = {
                (byte) 0x13, (byte) 0x57, (byte) 0x24, (byte) 0x68,
                (byte) 0x00, (byte) 0xff, (byte) 0xff, (byte) 0x00
        };
        bytePacket = new byte[byteHeader.length + bytePayload.length];
        System.arraycopy(byteHeader, 0, bytePacket, 0, byteHeader.length);
        System.arraycopy(bytePayload, 0, bytePacket, byteHeader.length, bytePayload.length);
    }

    @Before
    public void setUp() {
        deserializer = EncapSecurityPayload.deserializer();
    }

    /**
     * Tests serialize and setters.
     */
    @Test
    public void testSerialize() {
        EncapSecurityPayload esp = new EncapSecurityPayload();
        esp.setSecurityParamIndex(0x13572468);
        esp.setSequence(0xffff00);
        esp.setPayload(data);

        assertArrayEquals(esp.serialize(), bytePacket);
    }

    /**
     * Tests deserialize and getters.
     */
    @Test
    public void testDeserialize() throws DeserializationException {
        EncapSecurityPayload esp = deserializer.deserialize(bytePacket, 0, bytePacket.length);

        assertThat(esp.getSecurityParamIndex(), is(0x13572468));
        assertThat(esp.getSequence(), is(0xffff00));
    }

    /**
     * Tests comparator.
     */
    @Test
    public void testEqual() {
        EncapSecurityPayload esp1 = new EncapSecurityPayload();
        esp1.setSecurityParamIndex(0x13572468);
        esp1.setSequence(0xffff00);

        EncapSecurityPayload esp2 = new EncapSecurityPayload();
        esp2.setSecurityParamIndex(0x13572468);
        esp2.setSequence(0xfffff0);

        assertTrue(esp1.equals(esp1));
        assertFalse(esp1.equals(esp2));
    }

    /**
     * Tests toString.
     */
    @Test
    public void testToStringESP() throws Exception {
        EncapSecurityPayload esp = deserializer.deserialize(bytePacket, 0, bytePacket.length);
        String str = esp.toString();

        assertTrue(StringUtils.contains(str, "securityParamIndex=" + 0x13572468));
        assertTrue(StringUtils.contains(str, "sequence=" + 0xffff00));
    }
}

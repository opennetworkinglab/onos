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
import org.onlab.packet.UDP;

import java.util.Arrays;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.*;

/**
 * Tests for class {@link Authentication}.
 */
public class AuthenticationTest {
    private static Data data;
    private static UDP udp;
    private static byte[] icv = {
            (byte) 0x11, (byte) 0x22, (byte) 0x33, (byte) 0x44
    };
    private static byte[] bytePacket;

    private Deserializer<Authentication> deserializer;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        data = new Data();
        data.setData("testSerialize".getBytes());
        udp = new UDP();
        udp.setPayload(data);

        byte[] bytePayload = udp.serialize();
        byte[] byteHeader = {
                (byte) 0x11, (byte) 0x02, (byte) 0x00, (byte) 0x00,
                (byte) 0x13, (byte) 0x57, (byte) 0x24, (byte) 0x68,
                (byte) 0x00, (byte) 0xff, (byte) 0xff, (byte) 0x00,
                (byte) 0x11, (byte) 0x22, (byte) 0x33, (byte) 0x44
        };
        bytePacket = new byte[byteHeader.length + bytePayload.length];
        System.arraycopy(byteHeader, 0, bytePacket, 0, byteHeader.length);
        System.arraycopy(bytePayload, 0, bytePacket, byteHeader.length, bytePayload.length);
    }

    @Before
    public void setUp() {
        deserializer = Authentication.deserializer();
    }

    /**
     * Tests serialize and setters.
     */
    @Test
    public void testSerialize() {
        Authentication auth = new Authentication();
        auth.setNextHeader((byte) 0x11);
        auth.setPayloadLength((byte) 0x02);
        auth.setSecurityParamIndex(0x13572468);
        auth.setSequence(0xffff00);
        auth.setIngegrityCheck(icv);
        auth.setPayload(udp);

        assertArrayEquals(auth.serialize(), bytePacket);
    }

    /**
     * Tests deserialize and getters.
     */
    @Test
    public void testDeserialize() throws Exception {
        Authentication auth = deserializer.deserialize(bytePacket, 0, bytePacket.length);

        assertThat(auth.getNextHeader(), is((byte) 0x11));
        assertThat(auth.getPayloadLength(), is((byte) 0x02));
        assertThat(auth.getSecurityParamIndex(), is(0x13572468));
        assertThat(auth.getSequence(), is(0xffff00));
        assertArrayEquals(auth.getIntegrityCheck(), icv);
    }

    /**
     * Tests comparator.
     */
    @Test
    public void testEqual() {
        Authentication auth1 = new Authentication();
        auth1.setNextHeader((byte) 0x11);
        auth1.setPayloadLength((byte) 0x02);
        auth1.setSecurityParamIndex(0x13572468);
        auth1.setSequence(0xffff00);
        auth1.setIngegrityCheck(icv);

        Authentication auth2 = new Authentication();
        auth2.setNextHeader((byte) 0x11);
        auth2.setPayloadLength((byte) 0x02);
        auth2.setSecurityParamIndex(0x13572467);
        auth2.setSequence(0xffff00);
        auth2.setIngegrityCheck(icv);

        assertTrue(auth1.equals(auth1));
        assertFalse(auth1.equals(auth2));
    }

    /**
     * Tests toString.
     */
    @Test
    public void testToStringAuthentication() throws Exception {
        Authentication auth = deserializer.deserialize(bytePacket, 0, bytePacket.length);
        String str = auth.toString();

        assertTrue(StringUtils.contains(str, "nextHeader=" + (byte) 0x11));
        assertTrue(StringUtils.contains(str, "payloadLength=" + (byte) 0x02));
        assertTrue(StringUtils.contains(str, "securityParamIndex=" + 0x13572468));
        assertTrue(StringUtils.contains(str, "sequence=" + 0xffff00));
        assertTrue(StringUtils.contains(str, "integrityCheck=" + Arrays.toString(icv)));
    }
}

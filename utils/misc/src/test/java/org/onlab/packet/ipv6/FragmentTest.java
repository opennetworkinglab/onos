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

package org.onlab.packet.ipv6;

import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.onlab.packet.Data;
import org.onlab.packet.DeserializationException;
import org.onlab.packet.Deserializer;
import org.onlab.packet.UDP;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

/**
 * Tests for class {@link Fragment}.
 */
public class FragmentTest {
    private static Data data;
    private static UDP udp;
    private static byte[] bytePacket;

    private Deserializer<Fragment> deserializer;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        data = new Data();
        data.setData("testSerialize".getBytes());
        udp = new UDP();
        udp.setPayload(data);

        byte[] bytePayload = udp.serialize();
        byte[] byteHeader = {
                (byte) 0x11, (byte) 0x00, (byte) 0x00, (byte) 0xf9,
                (byte) 0x00, (byte) 0x00, (byte) 0x13, (byte) 0x57
        };
        bytePacket = new byte[byteHeader.length + bytePayload.length];
        System.arraycopy(byteHeader, 0, bytePacket, 0, byteHeader.length);
        System.arraycopy(bytePayload, 0, bytePacket, byteHeader.length, bytePayload.length);
    }

    @Before
    public void setUp() {
        deserializer = Fragment.deserializer();
    }

    /**
     * Tests serialize and setters.
     */
    @Test
    public void testSerialize() {
        Fragment frag = new Fragment();
        frag.setNextHeader((byte) 0x11);
        frag.setFragmentOffset((short) 0x1f);
        frag.setMoreFragment((byte) 1);
        frag.setIdentification(0x1357);
        frag.setPayload(udp);

        assertArrayEquals(frag.serialize(), bytePacket);
    }

    /**
     * Tests deserialize and getters.
     */
    @Test
    public void testDeserialize() throws DeserializationException {
        Fragment frag = deserializer.deserialize(bytePacket, 0, bytePacket.length);

        assertThat(frag.getNextHeader(), is((byte) 0x11));
        assertThat(frag.getFragmentOffset(), is((short) 0x1f));
        assertThat(frag.getMoreFragment(), is((byte) 1));
        assertThat(frag.getIdentification(), is(0x1357));
    }

    /**
     * Tests comparator.
     */
    @Test
    public void testEqual() {
        Fragment frag1 = new Fragment();
        frag1.setNextHeader((byte) 0x11);
        frag1.setFragmentOffset((short) 0x1f);
        frag1.setMoreFragment((byte) 1);
        frag1.setIdentification(0x1357);

        Fragment frag2 = new Fragment();
        frag2.setNextHeader((byte) 0x11);
        frag2.setFragmentOffset((short) 0x1f);
        frag2.setMoreFragment((byte) 1);
        frag2.setIdentification(0x1358);

        assertTrue(frag1.equals(frag1));
        assertFalse(frag1.equals(frag2));
    }

    /**
     * Tests toString.
     */
    @Test
    public void testToStringFragment() throws Exception {
        Fragment frag = deserializer.deserialize(bytePacket, 0, bytePacket.length);
        String str = frag.toString();

        assertTrue(StringUtils.contains(str, "nextHeader=" + (byte) 0x11));
        assertTrue(StringUtils.contains(str, "fragmentOffset=" + (short) 0x1f));
        assertTrue(StringUtils.contains(str, "moreFragment=" + (byte) 1));
        assertTrue(StringUtils.contains(str, "identification=" + 0x1357));
    }
}

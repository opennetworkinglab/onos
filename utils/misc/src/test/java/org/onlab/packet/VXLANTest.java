/*
 * Copyright 2017-present Open Networking Foundation
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



package org.onlab.packet;

import com.google.common.testing.EqualsTester;
import org.apache.commons.lang3.StringUtils;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

/**
 * Tests for class {@link VXLAN}.
 */
public class VXLANTest {

    private static final byte[] BYTE_PACKET_VXLAN = {
            (byte) 0x08, // flags (8 bits)
            (byte) 0x00, (byte) 0x00, (byte) 0x00, // rsvd1 (24 bits)
            (byte) 0x12, (byte) 0x34, (byte) 0x56, // vni (24 bits)
            (byte) 0x00, // rsvd2 (8 bits)
    };

    private static Deserializer<VXLAN> deserializer;

    private static final UDP UDP_HDR = new UDP();

    private static final int TEST_UDP_SRCPORT = 0x50;
    private static final int TEST_FLAGS = 0x08;
    private static final int TEST_VNI1 = 0x123456;
    private static final int TEST_VNI2 = 0x654321;


    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        deserializer = VXLAN.deserializer();
        UDP_HDR.setSourcePort(TEST_UDP_SRCPORT);
        UDP_HDR.setDestinationPort(UDP.VXLAN_UDP_PORT);
    }

    /**
     * Tests serialize and setters.
     */
    @Test
    public void testSerialize() {
        VXLAN vxlan = new VXLAN();
        vxlan.setFlag((byte) TEST_FLAGS);
        vxlan.setVni(TEST_VNI1);
        vxlan.setParent(UDP_HDR);
        assertArrayEquals("Serialized packet is not matched", BYTE_PACKET_VXLAN, vxlan.serialize());
    }

    /**
     * Tests deserialize bad input.
     */
    @Test
    public void testDeserializeBadInput() throws Exception {
        PacketTestUtils.testDeserializeBadInput(deserializer);
    }

    /**
     * Tests deserialize truncated.
     */
    @Test
    public void testDeserializeTruncated() throws Exception {
        PacketTestUtils.testDeserializeTruncated(deserializer, BYTE_PACKET_VXLAN);
    }

    /**
     * Tests deserialize and getters.
     */
    @Test
    public void testDeserialize() throws Exception {
        VXLAN vxlan = deserializer.deserialize(BYTE_PACKET_VXLAN, 0, BYTE_PACKET_VXLAN.length);

        assertThat(vxlan.getFlag(), is((byte) TEST_FLAGS));
        assertThat(vxlan.getVni(), is(TEST_VNI1));
    }

    /**
     * Tests comparator.
     */
    @Test
    public void testEqual() {
        VXLAN vxlan1 = new VXLAN();
        vxlan1.setFlag((byte) TEST_FLAGS);
        vxlan1.setVni(TEST_VNI1);

        VXLAN vxlan2 = new VXLAN();
        vxlan2.setFlag((byte) TEST_FLAGS);
        vxlan2.setVni(TEST_VNI2);

        new EqualsTester()
                .addEqualityGroup(vxlan1, vxlan1)
                .addEqualityGroup(vxlan2).testEquals();
    }

    /**
     * Tests toString.
     */
    @Test
    public void testToStringVXLAN() throws Exception {
        VXLAN vxlan = deserializer.deserialize(BYTE_PACKET_VXLAN, 0, BYTE_PACKET_VXLAN.length);
        String str = vxlan.toString();

        assertTrue(StringUtils.contains(str, "flags=" + TEST_FLAGS));
        assertTrue(StringUtils.contains(str, "vni=" + TEST_VNI1));
    }
}

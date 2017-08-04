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

package org.onlab.packet;

import org.junit.Before;
import org.junit.Test;

import java.nio.ByteBuffer;
import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for the LLDP class.
 */
public class LLDPTest {

    private Deserializer<LLDP> deserializer;

    private byte[] chassisValue = new byte[] {0x1, 0x2, 0x3, 0x4, 0x5, 0x6, 0x7};
    private byte[] portValue = new byte[] {0x1, 0x2, 0x3, 0x4, 0x5};
    private byte[] ttlValue = new byte[] {0x0, 0x20};

    private short optionalTlvSize = 6;
    private byte[] optionalTlvValue = new byte[] {0x6, 0x5, 0x4, 0x3, 0x2, 0x1};

    private byte[] bytes;

    @Before
    public void setUp() throws Exception {
        deserializer = LLDP.deserializer();

        // Each TLV is 2 bytes for the type+length, plus the size of the value
        // There are 2 zero-bytes at the end
        ByteBuffer bb = ByteBuffer.allocate(2 + LLDP.CHASSIS_TLV_SIZE +
                                            2 + LLDP.PORT_TLV_SIZE +
                                            2 + LLDP.TTL_TLV_SIZE +
                                            2 + optionalTlvSize +
                                            2);

        // Chassis TLV
        bb.putShort(getTypeLength(LLDP.CHASSIS_TLV_TYPE, LLDP.CHASSIS_TLV_SIZE));
        bb.put(chassisValue);

        // Port TLV
        bb.putShort(getTypeLength(LLDP.PORT_TLV_TYPE, LLDP.PORT_TLV_SIZE));
        bb.put(portValue);

        // TTL TLV
        bb.putShort(getTypeLength(LLDP.TTL_TLV_TYPE, LLDP.TTL_TLV_SIZE));
        bb.put(ttlValue);

        // Optional TLV
        bb.putShort(getTypeLength(LLDPOrganizationalTLV.ORGANIZATIONAL_TLV_TYPE, optionalTlvSize));
        bb.put(optionalTlvValue);

        bb.putShort((short) 0);

        bytes = bb.array();

    }

    private short getTypeLength(byte type, short length) {
        return (short) ((0x7f & type) << 9 | 0x1ff & length);
    }

    @Test
    public void testDeserializeBadInput() throws Exception {
        PacketTestUtils.testDeserializeBadInput(deserializer);
    }

    @Test
    public void testDeserializeTruncated() throws Exception {
        PacketTestUtils.testDeserializeTruncated(deserializer, bytes);
    }

    /**
     * Tests deserialize and getters.
     */
    @Test
    public void testDeserialize() throws Exception {
        LLDP lldp = deserializer.deserialize(bytes, 0, bytes.length);

        assertEquals(LLDP.CHASSIS_TLV_TYPE, lldp.getChassisId().getType());
        assertEquals(LLDP.CHASSIS_TLV_SIZE, lldp.getChassisId().getLength());
        assertTrue(Arrays.equals(chassisValue, lldp.getChassisId().getValue()));

        assertEquals(LLDP.PORT_TLV_TYPE, lldp.getPortId().getType());
        assertEquals(LLDP.PORT_TLV_SIZE, lldp.getPortId().getLength());
        assertTrue(Arrays.equals(portValue, lldp.getPortId().getValue()));

        assertEquals(LLDP.TTL_TLV_TYPE, lldp.getTtl().getType());
        assertEquals(LLDP.TTL_TLV_SIZE, lldp.getTtl().getLength());
        assertTrue(Arrays.equals(ttlValue, lldp.getTtl().getValue()));

        assertEquals(1, lldp.getOptionalTLVList().size());
        LLDPTLV optionalTlv = lldp.getOptionalTLVList().get(0);

        assertEquals(LLDPOrganizationalTLV.ORGANIZATIONAL_TLV_TYPE, optionalTlv.getType());
        assertEquals(optionalTlvSize, optionalTlv.getLength());
        assertTrue(Arrays.equals(optionalTlvValue, optionalTlv.getValue()));
    }

    /**
     * Tests toString.
     */
    @Test
    public void testToStringLLDP() throws Exception {
        LLDP lldp = deserializer.deserialize(bytes, 0, bytes.length);
        String str = lldp.toString();

        // TODO: need to add LLDP toString unit test
    }
}

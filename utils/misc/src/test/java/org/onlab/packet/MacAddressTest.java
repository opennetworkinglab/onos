/*
 * Copyright 2017-present Open Networking Laboratory
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

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Tests for class {@link MacAddress}.
 */
public class MacAddressTest {
    private static final MacAddress MAC_NORMAL = MacAddress.valueOf("00:00:00:00:00:01");
    private static final MacAddress MAC_BCAST = MacAddress.valueOf("ff:ff:ff:ff:ff:ff");
    private static final MacAddress MAC_MCAST = MacAddress.valueOf("01:00:5e:00:00:00");
    private static final MacAddress MAC_MCAST_2 = MacAddress.valueOf("01:00:0c:cc:cc:cc");
    private static final MacAddress MAC_LLDP = MacAddress.valueOf("01:80:c2:00:00:00");
    private static final MacAddress MAC_LLDP_2 = MacAddress.valueOf("01:80:c2:00:00:03");
    private static final MacAddress MAC_LLDP_3 = MacAddress.valueOf("01:80:c2:00:00:0e");
    private static final MacAddress MAC_ONOS = MacAddress.valueOf("a4:23:05:01:02:03");
    private static final MacAddress MAC_ONOS_EQUAL = MacAddress.valueOf("a4:23:05:01:02:03");

    private static final byte[] OUI_ONOS = {(byte) 0xa4, (byte) 0x23, (byte) 0x05};
    private static final byte[] MAC_ONOS_BYTE = {
            (byte) 0xa4, (byte) 0x23, (byte) 0x05, (byte) 0x01, (byte) 0x02, (byte) 0x03
    };
    private static final long MAC_ONOS_LONG = 180470314762755L;
    private static final String MAC_ONOS_STR = "A4:23:05:01:02:03";
    private static final String MAC_ONOS_STR_NO_COLON = "A42305010203";

    private static final int LENGTH = 6;

    private static final String INVALID_STR = "invalid";
    private static final byte[] INVALID_BYTE = {(byte) 0xaa};

    @Test
    public void testValueOfString() throws Exception {
        assertArrayEquals(MAC_ONOS_BYTE, MacAddress.valueOf(MAC_ONOS_STR).toBytes());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testValueOfInvalidString() throws Exception {
        MacAddress.valueOf(INVALID_STR);
    }

    @Test
    public void testValueOfByte() throws Exception {
        assertArrayEquals(MAC_ONOS_BYTE, MacAddress.valueOf(MAC_ONOS_BYTE).toBytes());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testValueOfInvalidByte() throws Exception {
        MacAddress.valueOf(INVALID_BYTE);
    }

    @Test
    public void testValueOfLong() throws Exception {
        assertArrayEquals(MAC_ONOS_BYTE, MacAddress.valueOf(MAC_ONOS_LONG).toBytes());
    }

    @Test
    public void testLength() throws Exception {
        assertEquals(LENGTH, MAC_NORMAL.length());
    }

    @Test
    public void testToBytes() throws Exception {
        assertArrayEquals(MAC_ONOS_BYTE, MAC_ONOS.toBytes());
    }

    @Test
    public void testToLong() throws Exception {
        assertEquals(MAC_ONOS_LONG, MAC_ONOS.toLong());
    }

    @Test
    public void testIsBroadcast() throws Exception {
        assertFalse(MAC_NORMAL.isBroadcast());
        assertTrue(MAC_BCAST.isBroadcast());
        assertFalse(MAC_MCAST.isBroadcast());
        assertFalse(MAC_MCAST_2.isBroadcast());
        assertFalse(MAC_LLDP.isBroadcast());
        assertFalse(MAC_LLDP_2.isBroadcast());
        assertFalse(MAC_LLDP_3.isBroadcast());
        assertFalse(MAC_ONOS.isBroadcast());
    }

    @Test
    public void testIsMulticast() throws Exception {
        // Note: LLDP MACs are also a multicast MACs
        assertFalse(MAC_NORMAL.isMulticast());
        assertFalse(MAC_BCAST.isMulticast());
        assertTrue(MAC_MCAST.isMulticast());
        assertTrue(MAC_MCAST_2.isMulticast());
        assertTrue(MAC_LLDP.isMulticast());
        assertTrue(MAC_LLDP_2.isMulticast());
        assertTrue(MAC_LLDP_3.isMulticast());
        assertFalse(MAC_ONOS.isMulticast());
    }

    @Test
    @Deprecated
    public void testIsLinkLocal() throws Exception {
        assertFalse(MAC_NORMAL.isLinkLocal());
        assertFalse(MAC_BCAST.isLinkLocal());
        assertFalse(MAC_MCAST.isLinkLocal());
        assertFalse(MAC_MCAST_2.isLinkLocal());
        assertTrue(MAC_LLDP.isLinkLocal());
        assertTrue(MAC_LLDP_2.isLinkLocal());
        assertTrue(MAC_LLDP_3.isLinkLocal());
        assertFalse(MAC_ONOS.isLinkLocal());
    }


    @Test
    public void testIsLldp() throws Exception {
        assertFalse(MAC_NORMAL.isLldp());
        assertFalse(MAC_BCAST.isLldp());
        assertFalse(MAC_MCAST.isLldp());
        assertFalse(MAC_MCAST_2.isLldp());
        assertTrue(MAC_LLDP.isLldp());
        assertTrue(MAC_LLDP_2.isLldp());
        assertTrue(MAC_LLDP_3.isLldp());
        assertFalse(MAC_ONOS.isLldp());
    }

    @Test
    public void testIsOnos() throws Exception {
        assertFalse(MAC_NORMAL.isOnos());
        assertFalse(MAC_BCAST.isOnos());
        assertFalse(MAC_MCAST.isOnos());
        assertFalse(MAC_MCAST_2.isOnos());
        assertFalse(MAC_LLDP.isOnos());
        assertFalse(MAC_LLDP_2.isOnos());
        assertFalse(MAC_LLDP_3.isOnos());
        assertTrue(MAC_ONOS.isOnos());
    }

    @Test
    public void testOui() throws Exception {
        assertArrayEquals(MAC_ONOS.oui(), OUI_ONOS);
    }

    @Test
    public void testEquals() throws Exception {
        assertTrue(MAC_ONOS.equals(MAC_ONOS));
        assertFalse(MAC_ONOS.equals(MAC_ONOS_STR));
        assertTrue(MAC_ONOS.equals(MAC_ONOS_EQUAL));
        assertFalse(MAC_ONOS.equals(MAC_NORMAL));
        assertFalse(MAC_ONOS.equals(MAC_BCAST));
        assertFalse(MAC_ONOS.equals(MAC_MCAST));
        assertFalse(MAC_ONOS.equals(MAC_MCAST_2));
        assertFalse(MAC_ONOS.equals(MAC_LLDP));
        assertFalse(MAC_ONOS.equals(MAC_LLDP_2));
        assertFalse(MAC_ONOS.equals(MAC_LLDP_3));
    }

    @Test
    public void testHashCode() throws Exception {
        assertEquals(Long.hashCode(MAC_ONOS_LONG), MAC_ONOS.hashCode());
    }

    @Test
    public void testToString() throws Exception {
        assertEquals(MAC_ONOS_STR, MAC_ONOS.toString());
    }

    @Test
    public void testToStringNoColon() throws Exception {
        assertEquals(MAC_ONOS_STR_NO_COLON, MAC_ONOS.toStringNoColon());
    }

}
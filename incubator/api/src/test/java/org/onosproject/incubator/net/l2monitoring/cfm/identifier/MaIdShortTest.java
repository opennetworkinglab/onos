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
package org.onosproject.incubator.net.l2monitoring.cfm.identifier;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Before;
import org.junit.Test;
import org.onosproject.incubator.net.l2monitoring.cfm.service.CfmConfigException;

public class MaIdShortTest {

    MaIdShort maId1;
    MaIdShort maId2;
    MaIdShort maId3;
    MaIdShort maId4;
    MaIdShort maId5;

    @Before
    public void setUp() throws Exception, CfmConfigException {
        maId1 = MaIdCharStr.asMaId("ma-1-1");
        maId2 = MaIdPrimaryVid.asMaId((short) 1234);
        maId3 = MaId2Octet.asMaId(33333);
        maId4 = MaIdRfc2685VpnId.asMaIdHex("aa:BB:cc:DD:ee:ff:11");
        maId5 = MaIdIccY1731.asMaId("ABC", "DEFGHIJK");
    }

    @Test
    public void testMaName() {
        assertEquals("ma-1-1", maId1.maName());
        assertEquals("1234", maId2.maName());
        assertEquals("33333", maId3.maName());
        assertEquals("aa:BB:cc:DD:ee:ff:11".toLowerCase(), maId4.maName());
        assertEquals("ABCDEFGHIJK", maId5.maName());
    }

    @Test
    public void testGetNameLength() {
        assertEquals(6, maId1.getNameLength());
        assertEquals(2, maId2.getNameLength());
        assertEquals(2, maId3.getNameLength());
        assertEquals(7, maId4.getNameLength());
        assertEquals(11, maId5.getNameLength());
    }

    @Test
    public void testMaNameWrong() {
        try {
            MaIdCharStr.asMaId(null);
            fail("Expected exception");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("MA Name must follow pattern"));
        }

        try {
            MaIdCharStr.asMaId("");
            fail("Expected exception");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("MA Name must follow pattern"));
        }

        try {
            MaIdCharStr.asMaId("This is a name with spaces - not allowed");
            fail("Expected exception");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("MA Name must follow pattern"));
        }

        try {
            MaIdCharStr.asMaId("This-name-is-too-long-at-forty-eight-characters-in-total");
            fail("Expected exception");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("MA Name must follow pattern"));
        }


        try {
            MaIdPrimaryVid.asMaId("abcdef");
            fail("Expected exception");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("MA Name must be numeric"));
        }

        try {
            MaIdPrimaryVid.asMaId("-20");
            fail("Expected exception");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("MA Id must be between 0 and 4095"));
        }

        try {
            MaIdPrimaryVid.asMaId("5000");
            fail("Expected exception");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("MA Id must be between 0 and 4095"));
        }


        try {
            MaId2Octet.asMaId("abcdef");
            fail("Expected exception");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("MA Name must be numeric"));
        }

        try {
            MaId2Octet.asMaId("-20");
            fail("Expected exception");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("MA Id must be between 0 and 65535"));
        }

        try {
            MaId2Octet.asMaId("70000");
            fail("Expected exception");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("MA Id must be between 0 and 65535"));
        }


        try {
            MaIdRfc2685VpnId.asMaIdHex("aa:bb:cc:dd:ee:ff"); //Need 7
            fail("Expected exception");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("MA Name must follow pattern"));
        }

        try {
            MaIdIccY1731.asMaId("ABCDEFG", "HIJKL"); //7 too long for ICC
            fail("Expected exception");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("ICC part must follow pattern"));
        }

        try {
            MaIdIccY1731.asMaId("A", "BCDEFGHIJKLMNO"); //14 too long for UMC
            fail("Expected exception");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("UMC part must follow pattern"));
        }

    }

    @Test
    public void testEquals() {
        //For char string
        assertFalse(maId1.equals(null));
        assertFalse(maId1.equals(new String("test")));

        assertTrue(maId1.equals(maId1));
        assertFalse(maId1.equals(maId2));

        //For primary vid
        assertFalse(maId2.equals(null));
        assertFalse(maId2.equals(new String("test")));

        assertTrue(maId2.equals(maId2));
        assertFalse(maId2.equals(maId1));

        //For 2 octet
        assertFalse(maId3.equals(null));
        assertFalse(maId3.equals(new String("test")));

        assertTrue(maId3.equals(maId3));
        assertFalse(maId3.equals(maId1));

        //rfc2685vpn
        assertFalse(maId4.equals(null));
        assertFalse(maId4.equals(new String("test")));

        assertTrue(maId4.equals(maId4));
        assertFalse(maId4.equals(maId1));


        //ICC-Y1731
        assertFalse(maId5.equals(null));
        assertFalse(maId5.equals(new String("test")));

        assertTrue(maId5.equals(maId5));
        assertFalse(maId5.equals(maId1));
}

    @Test
    public void testHashCode() {
        assertEquals(maId1.hashCode(), maId1.hashCode());
        assertEquals(maId2.hashCode(), maId2.hashCode());
        assertEquals(maId3.hashCode(), maId3.hashCode());
        assertEquals(maId4.hashCode(), maId4.hashCode());
        assertEquals(maId5.hashCode(), maId5.hashCode());
    }
}

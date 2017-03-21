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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Before;
import org.junit.Test;
import org.onlab.packet.MacAddress;
import org.onosproject.incubator.net.l2monitoring.cfm.service.CfmConfigException;

import com.google.common.net.InternetDomainName;

public class MdIdTest {

    MdId mdId1;
    MdId mdId2;
    MdId mdId3;
    MdId mdId4;

    @Before
    public void setUp() throws Exception, CfmConfigException {
        mdId1 = MdIdCharStr.asMdId("md-1");
        mdId2 = MdIdDomainName.asMdId("md.domain.tld");
        mdId3 = MdIdMacUint.asMdId(MacAddress.valueOf("AA:BB:cc:dd:ee:ff"), 54321);
        mdId4 = MdIdNone.asMdId();
    }

    @Test
    public void testMdNameWrong() {

        try {
            MdIdCharStr.asMdId("");
            fail("Expected an exception");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("MD Name must follow pattern"));
        }

        try {
            MdIdCharStr.asMdId("name with spaces and other stuff");
            fail("Expected an exception");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("MD Name must follow pattern"));
        }

        try {
            MdIdCharStr.asMdId("NameIsTooLongItShouldNotExceedFortyFiveCharacters");
            fail("Expected an exception");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("MD Name must follow pattern"));
        }

        try {
            MdIdCharStr.asMdId(null);
            fail("Expected an exception");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("MD Name must follow pattern"));
        }


        try {
            MdIdDomainName.asMdId((String) null);
            fail("Expected an exception");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("MD Name must follow internet domain"));
        }

        try {
            MdIdDomainName.asMdId("name with spaces");
            fail("Expected an exception");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("MD Name must follow internet domain"));
        }

        try {
            MdIdDomainName.asMdId(InternetDomainName
                    .from("a.really.long.domain.name.which.is.more.than.45.chars.long"));
            fail("Expected an exception");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("MD Domain Name must be between 1 and 45 chars long"));
        }

        try {
            MdIdMacUint.asMdId("AA:BB:cc:dd:ee:ff:70000");
            fail("Expected an exception");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("uInt must be between 0 and 65535"));
        }

        try {
            MdIdMacUint.asMdId("something:12345");
            fail("Expected an exception");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("MD Name must follow pattern"));
        }
}

    @Test
    public void testMdName() {
        assertEquals("md-1", mdId1.mdName());
        assertEquals("md.domain.tld", mdId2.mdName());
        assertEquals("AA:BB:cc:dd:ee:ff:54321".toUpperCase(), mdId3.mdName());
        assertNull(mdId4.mdName());
    }

    @Test
    public void testGetNameLength() {
        assertEquals(4, mdId1.getNameLength());
        assertEquals(13, mdId2.getNameLength());
        assertEquals(8, mdId3.getNameLength());
        assertEquals(0, mdId4.getNameLength());
    }

    @Test
    public void testEquals() {
        //For char string
        assertFalse(mdId1.equals(null));
        assertFalse(mdId1.equals(new String("test")));

        assertTrue(mdId1.equals(mdId1));
        assertFalse(mdId1.equals(mdId2));

        //For DomainName
        assertFalse(mdId2.equals(null));
        assertFalse(mdId2.equals(new String("test")));

        assertTrue(mdId2.equals(mdId2));
        assertFalse(mdId2.equals(mdId1));

        //For MacUint
        assertFalse(mdId3.equals(null));
        assertFalse(mdId3.equals(new String("test")));

        assertTrue(mdId3.equals(mdId3));
        assertFalse(mdId3.equals(mdId1));

        //For None
        assertFalse(mdId4.equals(null));
        assertFalse(mdId4.equals(new String("test")));

        assertTrue(mdId4.equals(mdId4));
        assertFalse(mdId4.equals(mdId1));

    }

    @Test
    public void testHashCode() {
        assertEquals(mdId1.hashCode(), mdId1.hashCode());
        assertEquals(mdId2.hashCode(), mdId2.hashCode());
        assertEquals(mdId3.hashCode(), mdId3.hashCode());
        assertEquals(mdId4.hashCode(), mdId4.hashCode());
    }
}

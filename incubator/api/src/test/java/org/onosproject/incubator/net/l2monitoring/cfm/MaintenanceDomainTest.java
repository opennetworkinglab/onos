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
package org.onosproject.incubator.net.l2monitoring.cfm;

import java.util.Collection;

import org.junit.Before;
import org.junit.Test;
import org.onosproject.incubator.net.l2monitoring.cfm.MaintenanceDomain.MdLevel;
import org.onosproject.incubator.net.l2monitoring.cfm.identifier.MaIdCharStr;
import org.onosproject.incubator.net.l2monitoring.cfm.identifier.MdIdCharStr;
import org.onosproject.incubator.net.l2monitoring.cfm.identifier.MdIdDomainName;
import org.onosproject.incubator.net.l2monitoring.cfm.identifier.MdIdMacUint;
import org.onosproject.incubator.net.l2monitoring.cfm.identifier.MdIdNone;
import org.onosproject.incubator.net.l2monitoring.cfm.service.CfmConfigException;

import static org.junit.Assert.*;

public class MaintenanceDomainTest {

    MaintenanceDomain md1;
    MaintenanceDomain md2;
    MaintenanceDomain md3;
    MaintenanceDomain md4;

    @Before
    public void setUp() {

        try {
            MaintenanceAssociation ma1 =
                DefaultMaintenanceAssociation
                .builder(MaIdCharStr.asMaId("ma-1-1"), 4)
                .build();

            md1 = DefaultMaintenanceDomain.builder(MdIdCharStr.asMdId("md-1"))
                    .mdLevel(MdLevel.LEVEL3)
                    .addToMaList(ma1)
                    .mdNumericId((short) 1)
                    .build();

            md2 = DefaultMaintenanceDomain.builder(MdIdDomainName.asMdId("test1.onosproject.org"))
                    .mdLevel(MdLevel.LEVEL4)
                    .build();

            md3 = DefaultMaintenanceDomain.builder(MdIdMacUint.asMdId("00:11:22:33:44:55:8191"))
                    .mdLevel(MdLevel.LEVEL5)
                    .build();

            md4 = DefaultMaintenanceDomain.builder(MdIdNone.asMdId())
                    .build();

        } catch (CfmConfigException e) {
            fail("Unexpected exception " + e.getMessage());
        }
    }

    @Test
    public void testMdNameType() {
        assertEquals("md-1", md1.mdId().mdName());
        assertEquals(MdIdCharStr.class, md1.mdId().getClass());
        assertEquals(4, md1.mdId().getNameLength());

        assertEquals("test1.onosproject.org", md2.mdId().mdName());
        assertEquals(MdIdDomainName.class, md2.mdId().getClass());
        assertEquals(21, md2.mdId().getNameLength());

        assertEquals("00:11:22:33:44:55:8191", md3.mdId().mdName());
        assertEquals(MdIdMacUint.class, md3.mdId().getClass());
        assertEquals(8, md3.mdId().getNameLength());

        assertEquals(null, md4.mdId().mdName());
        assertEquals(MdIdNone.class, md4.mdId().getClass());
        assertEquals(0, md4.mdId().getNameLength());
    }

    @Test
    public void testMdLevel() {
        assertEquals(MdLevel.LEVEL3, md1.mdLevel());
        assertEquals(MdLevel.LEVEL4, md2.mdLevel());
        assertEquals(MdLevel.LEVEL5, md3.mdLevel());
        assertEquals(MdLevel.LEVEL0, md4.mdLevel());
    }

    @Test
    public void testMaintenanceAssociationList() {
        assertEquals(1, md1.maintenanceAssociationList().size());

        assertEquals(0, md2.maintenanceAssociationList().size());
    }

    @Test
    public void testWithMaintenanceAssociationList() throws CfmConfigException {
        Collection<MaintenanceAssociation> maList = md1.maintenanceAssociationList();
        maList.add(DefaultMaintenanceAssociation
                .builder(MaIdCharStr.asMaId("ma-1-2"), 4)
                .build());

        md1 = md1.withMaintenanceAssociationList(maList);
        assertEquals(2, md1.maintenanceAssociationList().size());
    }

    @Test
    public void testMdNumericid() throws CfmConfigException {
        assertEquals(1, md1.mdNumericId());
    }

    @Test
    public void testEquals() throws CfmConfigException {
        assertFalse(md1.equals(md2));

        assertFalse(md1.equals(null));

        assertFalse(md1.equals(new String("test")));
    }

    @Test
    public void testHashCode() throws CfmConfigException {
        assertFalse(md1.hashCode() == md2.hashCode());

        assertTrue(md1.hashCode() == md1.hashCode());
    }

    @Test
    public void testCopyToBuilder() throws CfmConfigException {
        MaintenanceDomain mdCopy = DefaultMaintenanceDomain.builder(md1).build();

        assertEquals(md1, mdCopy);
    }

    @Test
    public void testToString() {
        assertEquals("DefaultMaintenanceDomain{mdId=md-1, level=LEVEL3}",
                                                            md1.toString());
        assertEquals("DefaultMaintenanceDomain{mdId=test1.onosproject.org, level=LEVEL4}",
                                                            md2.toString());
        assertEquals("DefaultMaintenanceDomain{mdId=00:11:22:33:44:55:8191, level=LEVEL5}",
                                                            md3.toString());
        assertEquals("DefaultMaintenanceDomain{mdId=, level=LEVEL0}", md4.toString());
    }
}

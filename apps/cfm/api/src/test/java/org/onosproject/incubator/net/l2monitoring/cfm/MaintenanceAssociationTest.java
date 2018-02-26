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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collection;

import org.junit.Before;
import org.junit.Test;
import org.onosproject.incubator.net.l2monitoring.cfm.MaintenanceAssociation.CcmInterval;
import org.onosproject.incubator.net.l2monitoring.cfm.identifier.MaId2Octet;
import org.onosproject.incubator.net.l2monitoring.cfm.identifier.MaIdCharStr;
import org.onosproject.incubator.net.l2monitoring.cfm.identifier.MaIdPrimaryVid;
import org.onosproject.incubator.net.l2monitoring.cfm.identifier.MaIdRfc2685VpnId;
import org.onosproject.incubator.net.l2monitoring.cfm.identifier.MepId;
import org.onosproject.incubator.net.l2monitoring.cfm.service.CfmConfigException;

public class MaintenanceAssociationTest {

    MaintenanceAssociation ma1;
    MaintenanceAssociation ma2;
    MaintenanceAssociation ma3;
    MaintenanceAssociation ma4;
    MaintenanceAssociation ma5;

    @Before
    public void setUp() throws Exception {
        try {
            ma1 = DefaultMaintenanceAssociation.builder(MaIdCharStr.asMaId("ma-1"), 10)
                    .ccmInterval(CcmInterval.INTERVAL_1MIN)
                    .maNumericId((short) 1)
                    .build();

            ma2 = DefaultMaintenanceAssociation.builder(MaIdPrimaryVid.asMaId("1024"), 10)
                    .build();

            ma3 = DefaultMaintenanceAssociation.builder(MaId2Octet.asMaId("33333"), 10)
                    .build();

            ma4 = DefaultMaintenanceAssociation.builder(MaIdRfc2685VpnId
                    .asMaIdHex("0A:0B:0C:0D:0E:0F:00"), 10).build();

        } catch (CfmConfigException e) {
            throw new Exception(e);
        }
    }

    @Test
    public void testMaName() {
        assertEquals("ma-1", ma1.maId().maName());

        assertEquals("1024", ma2.maId().maName());

        assertEquals("33333", ma3.maId().maName());

        assertEquals("0A:0B:0C:0D:0E:0F:00".toLowerCase(), ma4.maId().maName());
    }

    @Test
    public void testCcmInterval() {
        assertEquals(CcmInterval.INTERVAL_1MIN, ma1.ccmInterval());
    }

    @Test
    public void testComponentList() {
        assertNotNull(ma1.componentList());
    }

    @Test
    public void testWithComponentList() {
        Collection<Component> componentList2 = new ArrayList<>();
        MaintenanceAssociation ma2 = ma1.withComponentList(componentList2);
        assertNotNull(ma2.componentList());
    }

    @Test
    public void testRemoteMepIdList() {
        assertNotNull(ma1.remoteMepIdList());
    }

    @Test
    public void testWithRemoteMepIdList() throws CfmConfigException {
        Collection<MepId> remoteMepIdList2 = new ArrayList<>();
        remoteMepIdList2.add(MepId.valueOf((short) 450));
        remoteMepIdList2.add(MepId.valueOf((short) 451));
        remoteMepIdList2.add(MepId.valueOf((short) 452));
        MaintenanceAssociation ma2 = ma1.withRemoteMepIdList(remoteMepIdList2);
        assertEquals(3, ma2.remoteMepIdList().size());
    }

    @Test
    public void testMaNumericId() {
        assertEquals(1, ma1.maNumericId());
    }

    @Test
    public void testCopyThroughBuilder() throws CfmConfigException {
        MaintenanceAssociation maCopy =
                    DefaultMaintenanceAssociation.builder(ma3).build();
        assertEquals(ma3, maCopy);
    }

    @Test
    public void testEquals() {
        //For char string
        assertFalse(ma1.equals(null));
        assertFalse(ma1.equals(new String("test")));

        assertTrue(ma1.equals(ma1));
        assertFalse(ma1.equals(ma2));
    }

    @Test
    public void testHashCode() {
        assertEquals(ma1.hashCode(), ma1.hashCode());
    }

}

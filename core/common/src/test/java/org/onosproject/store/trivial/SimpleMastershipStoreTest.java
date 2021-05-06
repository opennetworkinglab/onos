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
package org.onosproject.store.trivial;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import com.google.common.collect.Lists;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onosproject.cluster.NodeId;
import org.onosproject.mastership.MastershipEvent;
import org.onosproject.mastership.MastershipTerm;
import org.onosproject.net.DeviceId;

import com.google.common.collect.Sets;
import com.google.common.util.concurrent.Futures;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.onosproject.mastership.MastershipEvent.Type.*;
import static org.onosproject.net.MastershipRole.*;

/**
 * Test for the simple MastershipStore implementation.
 */
public class SimpleMastershipStoreTest {

    private static final DeviceId DID1 = DeviceId.deviceId("of:01");
    private static final DeviceId DID2 = DeviceId.deviceId("of:02");
    private static final DeviceId DID3 = DeviceId.deviceId("of:03");
    private static final DeviceId DID4 = DeviceId.deviceId("of:04");

    private static final NodeId N1 = new NodeId("local");
    private static final NodeId N2 = new NodeId("other");
    private static final NodeId N3 = new NodeId("other2");
    private static final NodeId N4 = new NodeId("other3");

    private SimpleMastershipStore sms;

    @Before
    public void setUp() throws Exception {
        sms = new SimpleMastershipStore();
        sms.activate();
    }

    @After
    public void tearDown() throws Exception {
        sms.deactivate();
    }

    @Test
    public void getRole() {
        //special case, no backup or master
        put(DID1, N1, false, false);
        assertEquals("wrong role", NONE, sms.getRole(N1, DID1));

        //backup exists but we aren't mapped
        put(DID2, N1, false, true);
        assertEquals("wrong role", STANDBY, sms.getRole(N1, DID2));

        //N2 is master
        put(DID3, N2, true, true);
        assertEquals("wrong role", MASTER, sms.getRole(N2, DID3));

        //N2 is master but N1 is only in backups set
        put(DID4, N1, false, true);
        put(DID4, N2, true, false);
        assertEquals("wrong role", STANDBY, sms.getRole(N1, DID4));
    }

    @Test
    public void getMaster() {
        put(DID3, N2, true, true);
        assertEquals("wrong role", MASTER, sms.getRole(N2, DID3));
        assertEquals("wrong device", N2, sms.getMaster(DID3));
    }

    @Test
    public void setMaster() {
        put(DID1, N1, false, false);
        assertEquals("wrong event", MASTER_CHANGED, Futures.getUnchecked(sms.setMaster(N1, DID1)).type());
        assertEquals("wrong role", MASTER, sms.getRole(N1, DID1));
        //set node that's already master - should be ignored
        assertNull("wrong event", Futures.getUnchecked(sms.setMaster(N1, DID1)));

        //set STANDBY to MASTER
        put(DID2, N1, false, true);
        assertEquals("wrong role", STANDBY, sms.getRole(N1, DID2));
        assertEquals("wrong event", MASTER_CHANGED, Futures.getUnchecked(sms.setMaster(N1, DID2)).type());
        assertEquals("wrong role", MASTER, sms.getRole(N1, DID2));
    }

    @Test
    public void getDevices() {
        Set<DeviceId> d = Sets.newHashSet(DID1, DID2);

        put(DID1, N2, true, true);
        put(DID2, N2, true, true);
        put(DID3, N1, true, true);
        assertTrue("wrong devices", d.equals(sms.getDevices(N2)));
    }

    @Test
    public void getTermFor() {
        put(DID1, N1, true, true);
        assertEquals("wrong term", MastershipTerm.of(N1, 0), sms.getTermFor(DID1));

        //switch to N2 and back - 2 term switches
        sms.setMaster(N2, DID1);
        sms.setMaster(N1, DID1);
        assertEquals("wrong term", MastershipTerm.of(N1, 2), sms.getTermFor(DID1));
    }

    @Test
    public void requestRole() {
        //NONE - become MASTER
        put(DID1, N1, false, false);
        assertEquals("wrong role", MASTER, Futures.getUnchecked(sms.requestRole(DID1)));

        //was STANDBY - become MASTER
        put(DID2, N1, false, true);
        assertEquals("wrong role", MASTER, Futures.getUnchecked(sms.requestRole(DID2)));

        //other MASTER - stay STANDBY
        put(DID3, N2, true, false);
        assertEquals("wrong role", STANDBY, Futures.getUnchecked(sms.requestRole(DID3)));

        //local (N1) is MASTER - stay MASTER
        put(DID4, N1, true, true);
        assertEquals("wrong role", MASTER, Futures.getUnchecked(sms.requestRole(DID4)));
    }

    @Test
    public void unsetMaster() {
        //NONE - record backup but take no other action
        put(DID1, N1, false, false);
        sms.setStandby(N1, DID1);
        assertTrue("not backed up", sms.backups.get(DID1).contains(N1));
        int prev = sms.termMap.get(DID1).get();
        sms.setStandby(N1, DID1);
        assertEquals("term should not change", prev, sms.termMap.get(DID1).get());

        //no backup, MASTER
        put(DID1, N1, true, false);
        assertNull("expect no MASTER event", Futures.getUnchecked(sms.setStandby(N1, DID1)).roleInfo().master());
        assertNull("wrong node", sms.masterMap.get(DID1));

        //backup, switch
        sms.masterMap.clear();
        put(DID1, N1, true, true);
        put(DID1, N2, false, true);
        put(DID2, N2, true, true);
        MastershipEvent event = Futures.getUnchecked(sms.setStandby(N1, DID1));
        assertEquals("wrong event", MASTER_CHANGED, event.type());
        assertEquals("wrong master", N2, event.roleInfo().master());
    }

    @Test
    public void demote() {
        put(DID1, N1, true, false);
        put(DID1, N2, false, true);
        put(DID1, N3, false, true);
        List<NodeId> stdbys = Lists.newArrayList(N2, N3);
        // N1 master, N2 and N3 backups
        assertEquals("wrong role", MASTER, sms.getRole(N1, DID1));
        assertEquals("wrong backups", stdbys, sms.backups.getOrDefault(DID1, new ArrayList<>()));
        // No effect, it is the master
        sms.demote(N1, DID1);
        assertEquals("wrong role", MASTER, sms.getRole(N1, DID1));
        assertEquals("wrong backups", stdbys, sms.backups.getOrDefault(DID1, new ArrayList<>()));
        // No effect, it is not part of the mastership
        sms.demote(N4, DID1);
        assertEquals("wrong role", MASTER, sms.getRole(N1, DID1));
        assertEquals("wrong backups", stdbys, sms.backups.getOrDefault(DID1, new ArrayList<>()));
        // Demote N2
        stdbys = Lists.newArrayList(N3, N2);
        sms.demote(N2, DID1);
        assertEquals("wrong role", MASTER, sms.getRole(N1, DID1));
        assertEquals("wrong backups", stdbys, sms.backups.getOrDefault(DID1, new ArrayList<>()));
    }

    //helper to populate master/backup structures
    private void put(DeviceId dev, NodeId node, boolean master, boolean backup) {
        if (master) {
            sms.masterMap.put(dev, node);
        } else if (backup) {
            List<NodeId> stbys = sms.backups.getOrDefault(dev, new ArrayList<>());
            stbys.add(node);
            sms.backups.put(dev, stbys);
        }
        sms.termMap.put(dev, new AtomicInteger());
    }
}

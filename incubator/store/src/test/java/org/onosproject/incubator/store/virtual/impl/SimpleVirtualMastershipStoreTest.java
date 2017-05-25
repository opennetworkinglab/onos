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

package org.onosproject.incubator.store.virtual.impl;

import com.google.common.collect.Sets;
import com.google.common.util.concurrent.Futures;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onosproject.cluster.NodeId;
import org.onosproject.incubator.net.virtual.NetworkId;
import org.onosproject.mastership.MastershipEvent;
import org.onosproject.mastership.MastershipTerm;
import org.onosproject.net.DeviceId;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.onosproject.mastership.MastershipEvent.Type.MASTER_CHANGED;
import static org.onosproject.net.MastershipRole.MASTER;
import static org.onosproject.net.MastershipRole.NONE;
import static org.onosproject.net.MastershipRole.STANDBY;

public class SimpleVirtualMastershipStoreTest {

    private static final NetworkId VNID1 = NetworkId.networkId(1);

    private static final DeviceId VDID1 = DeviceId.deviceId("of:01");
    private static final DeviceId VDID2 = DeviceId.deviceId("of:02");
    private static final DeviceId VDID3 = DeviceId.deviceId("of:03");
    private static final DeviceId VDID4 = DeviceId.deviceId("of:04");

    private static final NodeId N1 = new NodeId("local");
    private static final NodeId N2 = new NodeId("other");

    private SimpleVirtualMastershipStore sms;

    @Before
    public void setUp() throws Exception {
        sms = new SimpleVirtualMastershipStore();
        sms.activate();
    }

    @After
    public void tearDown() throws Exception {
        sms.deactivate();
    }

    @Test
    public void getRole() {
        //special case, no backup or master
        put(VNID1, VDID1, N1, false, false);
        assertEquals("wrong role", NONE, sms.getRole(VNID1, N1, VDID1));

        //backup exists but we aren't mapped
        put(VNID1, VDID2, N1, false, true);
        assertEquals("wrong role", STANDBY, sms.getRole(VNID1, N1, VDID2));

        //N2 is master
        put(VNID1, VDID3, N2, true, true);
        assertEquals("wrong role", MASTER, sms.getRole(VNID1, N2, VDID3));

        //N2 is master but N1 is only in backups set
        put(VNID1, VDID4, N1, false, true);
        put(VNID1, VDID4, N2, true, false);
        assertEquals("wrong role", STANDBY, sms.getRole(VNID1, N1, VDID4));
    }

    @Test
    public void getMaster() {
        put(VNID1, VDID3, N2, true, true);
        assertEquals("wrong role", MASTER, sms.getRole(VNID1, N2, VDID3));
        assertEquals("wrong node", N2, sms.getMaster(VNID1, VDID3));
    }

    @Test
    public void setMaster() {
        put(VNID1, VDID1, N1, false, false);
        assertEquals("wrong event", MASTER_CHANGED,
                     Futures.getUnchecked(sms.setMaster(VNID1, N1, VDID1)).type());
        assertEquals("wrong role", MASTER, sms.getRole(VNID1, N1, VDID1));
        //set node that's already master - should be ignored
        assertNull("wrong event",
                   Futures.getUnchecked(sms.setMaster(VNID1, N1, VDID1)));

        //set STANDBY to MASTER
        put(VNID1, VDID2, N1, false, true);
        assertEquals("wrong role", STANDBY, sms.getRole(VNID1, N1, VDID2));
        assertEquals("wrong event", MASTER_CHANGED,
                     Futures.getUnchecked(sms.setMaster(VNID1, N1, VDID2)).type());
        assertEquals("wrong role", MASTER, sms.getRole(VNID1, N1, VDID2));
    }

    @Test
    public void getDevices() {
        Set<DeviceId> d = Sets.newHashSet(VDID1, VDID2);

        put(VNID1, VDID1, N2, true, true);
        put(VNID1, VDID2, N2, true, true);
        put(VNID1, VDID3, N1, true, true);
        assertTrue("wrong devices", d.equals(sms.getDevices(VNID1, N2)));
    }

    @Test
    public void getTermFor() {
        put(VNID1, VDID1, N1, true, true);
        assertEquals("wrong term", MastershipTerm.of(N1, 0),
                     sms.getTermFor(VNID1, VDID1));

        //switch to N2 and back - 2 term switches
        sms.setMaster(VNID1, N2, VDID1);
        sms.setMaster(VNID1, N1, VDID1);
        assertEquals("wrong term", MastershipTerm.of(N1, 2),
                     sms.getTermFor(VNID1, VDID1));
    }

    @Test
    public void requestRole() {
        //NONE - become MASTER
        put(VNID1, VDID1, N1, false, false);
        assertEquals("wrong role", MASTER,
                     Futures.getUnchecked(sms.requestRole(VNID1, VDID1)));

        //was STANDBY - become MASTER
        put(VNID1, VDID2, N1, false, true);
        assertEquals("wrong role", MASTER,
                     Futures.getUnchecked(sms.requestRole(VNID1, VDID2)));

        //other MASTER - stay STANDBY
        put(VNID1, VDID3, N2, true, false);
        assertEquals("wrong role", STANDBY,
                     Futures.getUnchecked(sms.requestRole(VNID1, VDID3)));

        //local (N1) is MASTER - stay MASTER
        put(VNID1, VDID4, N1, true, true);
        assertEquals("wrong role", MASTER,
                     Futures.getUnchecked(sms.requestRole(VNID1, VDID4)));
    }

    @Test
    public void unsetMaster() {
        //NONE - record backup but take no other action
        put(VNID1, VDID1, N1, false, false);
        sms.setStandby(VNID1, N1, VDID1);
        assertTrue("not backed up", sms.backupsByNetwork.get(VNID1)
                .get(VDID1).contains(N1));
        int prev = sms.termMapByNetwork.get(VNID1).get(VDID1).get();
        sms.setStandby(VNID1, N1, VDID1);
        assertEquals("term should not change", prev, sms.termMapByNetwork.get(VNID1)
                .get(VDID1).get());

        //no backup, MASTER
        put(VNID1, VDID1, N1, true, false);
        assertNull("expect no MASTER event",
                   Futures.getUnchecked(sms.setStandby(VNID1, N1, VDID1)).roleInfo().master());
        assertNull("wrong node", sms.masterMapByNetwork.get(VNID1).get(VDID1));

        //backup, switch
        sms.masterMapByNetwork.get(VNID1).clear();
        put(VNID1, VDID1, N1, true, true);
        put(VNID1, VDID1, N2, false, true);
        put(VNID1, VDID2, N2, true, true);
        MastershipEvent event = Futures.getUnchecked(sms.setStandby(VNID1, N1, VDID1));
        assertEquals("wrong event", MASTER_CHANGED, event.type());
        assertEquals("wrong master", N2, event.roleInfo().master());
    }

    //helper to populate master/backup structures
    private void put(NetworkId networkId, DeviceId dev, NodeId node,
                     boolean master, boolean backup) {
        if (master) {
            sms.masterMapByNetwork
                    .computeIfAbsent(networkId, k -> new HashMap<>())
                    .put(dev, node);
        } else if (backup) {
            List<NodeId> stbys = sms.backupsByNetwork
                    .computeIfAbsent(networkId, k -> new HashMap<>())
                    .getOrDefault(dev, new ArrayList<>());
            stbys.add(node);
            sms.backupsByNetwork.get(networkId).put(dev, stbys);
        }

        sms.termMapByNetwork
                .computeIfAbsent(networkId, k -> new HashMap<>())
                .put(dev, new AtomicInteger());
    }
}
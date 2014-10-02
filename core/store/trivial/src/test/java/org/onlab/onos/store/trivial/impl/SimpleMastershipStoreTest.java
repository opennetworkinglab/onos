package org.onlab.onos.store.trivial.impl;

import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onlab.onos.cluster.MastershipTerm;
import org.onlab.onos.cluster.NodeId;
import org.onlab.onos.net.DeviceId;

import com.google.common.collect.Sets;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.onlab.onos.net.MastershipRole.*;
import static org.onlab.onos.cluster.MastershipEvent.Type.*;

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
        assertEquals("wrong event", MASTER_CHANGED, sms.setMaster(N1, DID1).type());
        assertEquals("wrong role", MASTER, sms.getRole(N1, DID1));
        //set node that's already master - should be ignored
        assertNull("wrong event", sms.setMaster(N1, DID1));

        //set STANDBY to MASTER
        put(DID2, N1, false, true);
        assertEquals("wrong role", STANDBY, sms.getRole(N1, DID2));
        assertEquals("wrong event", MASTER_CHANGED, sms.setMaster(N1, DID2).type());
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
        assertEquals("wrong role", MASTER, sms.requestRole(DID1));

        //STANDBY without backup - become MASTER
        put(DID2, N1, false, true);
        assertEquals("wrong role", MASTER, sms.requestRole(DID2));

        //STANDBY with backup - stay STANDBY
        put(DID3, N2, false, true);
        assertEquals("wrong role", STANDBY, sms.requestRole(DID3));

        //local (N1) is MASTER - stay MASTER
        put(DID4, N1, true, true);
        assertEquals("wrong role", MASTER, sms.requestRole(DID4));
    }

    @Test
    public void unsetMaster() {
        //NONE - record backup but take no other action
        put(DID1, N1, false, false);
        sms.unsetMaster(N1, DID1);
        assertTrue("not backed up", sms.backups.contains(N1));
        sms.termMap.clear();
        sms.unsetMaster(N1, DID1);
        assertTrue("term not set", sms.termMap.containsKey(DID1));

        //no backup, MASTER
        put(DID1, N1, true, true);
        assertNull("wrong event", sms.unsetMaster(N1, DID1));
        assertNull("wrong node", sms.masterMap.get(DID1));

        //backup, switch
        sms.masterMap.clear();
        put(DID1, N1, true, true);
        put(DID2, N2, true, true);
        assertEquals("wrong event", MASTER_CHANGED, sms.unsetMaster(N1, DID1).type());
    }

    //helper to populate master/backup structures
    private void put(DeviceId dev, NodeId node, boolean store, boolean backup) {
        if (store) {
            sms.masterMap.put(dev, node);
        }
        if (backup) {
            sms.backups.add(node);
        }
        sms.termMap.put(dev, new AtomicInteger());
    }
}

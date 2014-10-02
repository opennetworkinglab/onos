package org.onlab.onos.store.cluster.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.onlab.onos.net.MastershipRole.*;

import java.util.Set;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.onlab.onos.cluster.ClusterEventListener;
import org.onlab.onos.cluster.ClusterService;
import org.onlab.onos.cluster.ControllerNode;
import org.onlab.onos.cluster.ControllerNode.State;
import org.onlab.onos.cluster.DefaultControllerNode;
import org.onlab.onos.cluster.MastershipEvent.Type;
import org.onlab.onos.cluster.MastershipTerm;
import org.onlab.onos.cluster.NodeId;
import org.onlab.onos.net.DeviceId;
import org.onlab.onos.store.common.StoreManager;
import org.onlab.onos.store.common.StoreService;
import org.onlab.onos.store.common.TestStoreManager;
import org.onlab.onos.store.serializers.KryoSerializationManager;
import org.onlab.onos.store.serializers.KryoSerializationService;
import org.onlab.packet.IpPrefix;

import com.google.common.collect.Sets;
import com.hazelcast.config.Config;
import com.hazelcast.core.Hazelcast;

/**
 * Test of the Hazelcast-based distributed MastershipStore implementation.
 */
public class DistributedMastershipStoreTest {

    private static final DeviceId DID1 = DeviceId.deviceId("of:01");
    private static final DeviceId DID2 = DeviceId.deviceId("of:02");
    private static final DeviceId DID3 = DeviceId.deviceId("of:03");
    private static final DeviceId DID4 = DeviceId.deviceId("of:04");

    private static final IpPrefix IP = IpPrefix.valueOf("127.0.0.1");

    private static final NodeId N1 = new NodeId("node1");
    private static final NodeId N2 = new NodeId("node2");

    private static final ControllerNode CN1 = new DefaultControllerNode(N1, IP);
    private static final ControllerNode CN2 = new DefaultControllerNode(N2, IP);

    private DistributedMastershipStore dms;
    private TestDistributedMastershipStore testStore;
    private KryoSerializationManager serializationMgr;
    private StoreManager storeMgr;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
    }

    @Before
    public void setUp() throws Exception {
        // TODO should find a way to clean Hazelcast instance without shutdown.
        Config config = TestStoreManager.getTestConfig();

        storeMgr = new TestStoreManager(Hazelcast.newHazelcastInstance(config));
        storeMgr.activate();

        serializationMgr = new KryoSerializationManager();
        serializationMgr.activate();

        dms = new TestDistributedMastershipStore(storeMgr, serializationMgr);
        dms.clusterService = new TestClusterService();
        dms.activate();

        testStore = (TestDistributedMastershipStore) dms;
    }

    @After
    public void tearDown() throws Exception {
        dms.deactivate();

        serializationMgr.deactivate();

        storeMgr.deactivate();
    }

    @Test
    public void getRole() {
        assertEquals("wrong role:", NONE, dms.getRole(N1, DID1));
        testStore.put(DID1, N1, true, true, true);
        assertEquals("wrong role:", MASTER, dms.getRole(N1, DID1));
        assertEquals("wrong role:", STANDBY, dms.getRole(N2, DID1));
    }

    @Test
    public void getMaster() {
        assertTrue("wrong store state:", dms.rawMasters.isEmpty());

        testStore.put(DID1, N1, true, false, false);
        assertEquals("wrong master:", N1, dms.getMaster(DID1));
        assertNull("wrong master:", dms.getMaster(DID2));
    }

    @Test
    public void getDevices() {
        assertTrue("wrong store state:", dms.rawMasters.isEmpty());

        testStore.put(DID1, N1, true, false, false);
        testStore.put(DID2, N1, true, false, false);
        testStore.put(DID3, N2, true, false, false);

        assertEquals("wrong devices",
                Sets.newHashSet(DID1, DID2), dms.getDevices(N1));
    }

    @Test
    public void requestRoleAndTerm() {
        //CN1 is "local"
        testStore.setCurrent(CN1);

        //if already MASTER, nothing should happen
        testStore.put(DID2, N1, true, false, false);
        assertEquals("wrong role for MASTER:", MASTER, dms.requestRole(DID2));
        assertTrue("wrong state for store:",
                dms.backups.isEmpty() & dms.rawTerms.isEmpty());

        //populate maps with DID1, N1 thru NONE case
        assertEquals("wrong role for NONE:", MASTER, dms.requestRole(DID1));
        assertTrue("wrong state for store:",
                !dms.backups.isEmpty() & !dms.rawTerms.isEmpty());
        assertEquals("wrong term",
                MastershipTerm.of(N1, 0), dms.getTermFor(DID1));

        //CN2 now local. DID2 has N1 as MASTER so N2 is STANDBY
        testStore.setCurrent(CN2);
        assertEquals("wrong role for STANDBY:", STANDBY, dms.requestRole(DID2));
        assertEquals("wrong number of entries:", 2, dms.rawTerms.size());

        //change term and requestRole() again; should persist
        testStore.increment(DID2);
        assertEquals("wrong role for STANDBY:", STANDBY, dms.requestRole(DID2));
        assertEquals("wrong term", MastershipTerm.of(N1, 1), dms.getTermFor(DID2));
    }

    @Test
    public void setMaster() {
        //populate maps with DID1, N1 as MASTER thru NONE case
        testStore.setCurrent(CN1);
        assertEquals("wrong role for NONE:", MASTER, dms.requestRole(DID1));
        assertNull("wrong event:", dms.setMaster(N1, DID1));

        //switch over to N2
        assertEquals("wrong event:", Type.MASTER_CHANGED, dms.setMaster(N2, DID1).type());
        assertEquals("wrong term", MastershipTerm.of(N2, 1), dms.getTermFor(DID1));

        //orphan switch - should be rare case
        assertEquals("wrong event:", Type.MASTER_CHANGED, dms.setMaster(N2, DID2).type());
        assertEquals("wrong term", MastershipTerm.of(N2, 0), dms.getTermFor(DID2));
        //disconnect and reconnect - sign of failing re-election or single-instance channel
        testStore.reset(true, false, false);
        dms.setMaster(N2, DID2);
        assertEquals("wrong term", MastershipTerm.of(N2, 1), dms.getTermFor(DID2));
    }

    @Test
    public void unsetMaster() {
        //populate maps with DID1, N1 as MASTER thru NONE case
        testStore.setCurrent(CN1);
        assertEquals("wrong role for NONE:", MASTER, dms.requestRole(DID1));
        //no backup, no new MASTER/event
        assertNull("wrong event:", dms.unsetMaster(N1, DID1));
        //add backup CN2, get it elected MASTER
        dms.requestRole(DID1);
        testStore.setCurrent(CN2);
        dms.requestRole(DID1);
        assertEquals("wrong event:", Type.MASTER_CHANGED, dms.unsetMaster(N1, DID1).type());
        assertEquals("wrong master", N2, dms.getMaster(DID1));

        //STANDBY - nothing here, either
        assertNull("wrong event:", dms.unsetMaster(N1, DID1));
        assertEquals("wrong role for node:", STANDBY, dms.getRole(N1, DID1));

        //NONE - nothing happens
        assertNull("wrong event:", dms.unsetMaster(N1, DID2));
        assertEquals("wrong role for node:", NONE, dms.getRole(N1, DID2));
    }

    private class TestDistributedMastershipStore extends
            DistributedMastershipStore {
        public TestDistributedMastershipStore(StoreService storeService,
                KryoSerializationService kryoSerializationService) {
            this.storeService = storeService;
            this.kryoSerializationService = kryoSerializationService;
        }

        //helper to populate master/backup structures
        public void put(DeviceId dev, NodeId node,
                boolean store, boolean backup, boolean term) {
            if (store) {
                dms.rawMasters.put(serialize(dev), serialize(node));
            }
            if (backup) {
                dms.backups.put(serialize(node), (byte) 0);
            }
            if (term) {
                dms.rawTerms.put(serialize(dev), 0);
            }
        }

        //clears structures
        public void reset(boolean store, boolean backup, boolean term) {
            if (store) {
                dms.rawMasters.clear();
            }
            if (backup) {
                dms.backups.clear();
            }
            if (term) {
                dms.rawTerms.clear();
            }
        }

        //increment term for a device
        public void increment(DeviceId dev) {
            Integer t = dms.rawTerms.get(serialize(dev));
            if (t != null) {
                dms.rawTerms.put(serialize(dev), ++t);
            }
        }

        //sets the "local" node
        public void setCurrent(ControllerNode node) {
            ((TestClusterService) clusterService).current = node;
        }
    }

    private class TestClusterService implements ClusterService {

        protected ControllerNode current;

        @Override
        public ControllerNode getLocalNode() {
            return current;
        }

        @Override
        public Set<ControllerNode> getNodes() {
            return Sets.newHashSet(CN1, CN2);
        }

        @Override
        public ControllerNode getNode(NodeId nodeId) {
            return null;
        }

        @Override
        public State getState(NodeId nodeId) {
            return null;
        }

        @Override
        public void addListener(ClusterEventListener listener) {
        }

        @Override
        public void removeListener(ClusterEventListener listener) {
        }

    }
}

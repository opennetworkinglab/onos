/*
 * Copyright 2014-2015 Open Networking Laboratory
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
package org.onosproject.store.mastership.impl;

/**
 * Test of the Hazelcast-based distributed MastershipStore implementation.
 */
public class DistributedMastershipStoreTest {
/*
    private static final DeviceId DID1 = DeviceId.deviceId("of:01");
    private static final DeviceId DID2 = DeviceId.deviceId("of:02");
    private static final DeviceId DID3 = DeviceId.deviceId("of:03");

    private static final IpAddress IP = IpAddress.valueOf("127.0.0.1");

    private static final NodeId N1 = new NodeId("node1");
    private static final NodeId N2 = new NodeId("node2");

    private static final ControllerNode CN1 = new DefaultControllerNode(N1, IP);
    private static final ControllerNode CN2 = new DefaultControllerNode(N2, IP);

    private DistributedMastershipStore dms;
    private TestDistributedMastershipStore testStore;
    private KryoSerializer serializationMgr;
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
        TestStoreManager testStoreMgr = new TestStoreManager();
        testStoreMgr.setHazelcastInstance(testStoreMgr.initSingleInstance());
        storeMgr = testStoreMgr;
        storeMgr.activate();

        serializationMgr = new KryoSerializer();

        dms = new TestDistributedMastershipStore(storeMgr, serializationMgr);
        dms.clusterService = new TestClusterService();
        dms.activate();

        testStore = (TestDistributedMastershipStore) dms;
    }

    @After
    public void tearDown() throws Exception {
        dms.deactivate();

        storeMgr.deactivate();
    }

    @Test
    @Ignore("Disabled this test due to intermittent failures seen on Jenkins runs")
    public void getRole() {
        assertEquals("wrong role:", NONE, dms.getRole(N1, DID1));
        testStore.put(DID1, N1, true, false, true);
        assertEquals("wrong role:", MASTER, dms.getRole(N1, DID1));
        testStore.put(DID1, N2, false, true, false);
        assertEquals("wrong role:", STANDBY, dms.getRole(N2, DID1));
    }

    @Test
    public void getMaster() {
        assertTrue("wrong store state:", dms.roleMap.isEmpty());

        testStore.put(DID1, N1, true, false, false);
        TestTools.assertAfter(100, () -> //wait for up to 100ms
            assertEquals("wrong master:", N1, dms.getMaster(DID1)));
        assertNull("wrong master:", dms.getMaster(DID2));
    }

    @Test
    public void getDevices() {
        assertTrue("wrong store state:", dms.roleMap.isEmpty());

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
        testStore.put(DID2, N1, true, false, true);
        assertEquals("wrong role for MASTER:", MASTER, Futures.getUnchecked(dms.requestRole(DID2)));

        //populate maps with DID1, N1 thru NONE case
        assertEquals("wrong role for NONE:", MASTER, Futures.getUnchecked(dms.requestRole(DID1)));
        assertTrue("wrong state for store:", !dms.terms.isEmpty());
        assertEquals("wrong term",
                MastershipTerm.of(N1, 1), dms.getTermFor(DID1));

        //CN2 now local. DID2 has N1 as MASTER so N2 is STANDBY
        testStore.setCurrent(CN2);
        assertEquals("wrong role for STANDBY:", STANDBY, Futures.getUnchecked(dms.requestRole(DID2)));
        assertEquals("wrong number of entries:", 2, dms.terms.size());

        //change term and requestRole() again; should persist
        testStore.increment(DID2);
        assertEquals("wrong role for STANDBY:", STANDBY, Futures.getUnchecked(dms.requestRole(DID2)));
        assertEquals("wrong term", MastershipTerm.of(N1, 1), dms.getTermFor(DID2));
    }

    @Test
    public void setMaster() {
        //populate maps with DID1, N1 as MASTER thru NONE case
        testStore.setCurrent(CN1);
        assertEquals("wrong role for NONE:", MASTER, Futures.getUnchecked(dms.requestRole(DID1)));
        assertNull("wrong event:", Futures.getUnchecked(dms.setMaster(N1, DID1)));

        //switch over to N2
        assertEquals("wrong event:", Type.MASTER_CHANGED, Futures.getUnchecked(dms.setMaster(N2, DID1)).type());
        System.out.println(dms.getTermFor(DID1).master() + ":" + dms.getTermFor(DID1).termNumber());
        assertEquals("wrong term", MastershipTerm.of(N2, 2), dms.getTermFor(DID1));

        //orphan switch - should be rare case
        assertEquals("wrong event:", Type.MASTER_CHANGED, Futures.getUnchecked(dms.setMaster(N2, DID2)).type());
        assertEquals("wrong term", MastershipTerm.of(N2, 1), dms.getTermFor(DID2));
        //disconnect and reconnect - sign of failing re-election or single-instance channel
        dms.roleMap.clear();
        dms.setMaster(N2, DID2);
        assertEquals("wrong term", MastershipTerm.of(N2, 2), dms.getTermFor(DID2));
    }

    @Test
    public void relinquishRole() {
        //populate maps with DID1, N1 as MASTER thru NONE case
        testStore.setCurrent(CN1);
        assertEquals("wrong role for NONE:", MASTER, Futures.getUnchecked(dms.requestRole(DID1)));
        //no backup, no new MASTER/event
        assertNull("wrong event:", Futures.getUnchecked(dms.relinquishRole(N1, DID1)));

        dms.requestRole(DID1);

        //add backup CN2, get it elected MASTER by relinquishing
        testStore.setCurrent(CN2);
        assertEquals("wrong role for NONE:", STANDBY, Futures.getUnchecked(dms.requestRole(DID1)));
        assertEquals("wrong event:", Type.MASTER_CHANGED, Futures.getUnchecked(dms.relinquishRole(N1, DID1)).type());
        assertEquals("wrong master", N2, dms.getMaster(DID1));

        //all nodes "give up" on device, which goes back to NONE.
        assertNull("wrong event:", Futures.getUnchecked(dms.relinquishRole(N2, DID1)));
        assertEquals("wrong role for node:", NONE, dms.getRole(N2, DID1));

        assertEquals("wrong number of retired nodes", 2,
                dms.roleMap.get(DID1).nodesOfRole(NONE).size());

        //bring nodes back
        assertEquals("wrong role for NONE:", MASTER, Futures.getUnchecked(dms.requestRole(DID1)));
        testStore.setCurrent(CN1);
        assertEquals("wrong role for NONE:", STANDBY, Futures.getUnchecked(dms.requestRole(DID1)));
        assertEquals("wrong number of backup nodes", 1,
                dms.roleMap.get(DID1).nodesOfRole(STANDBY).size());

        //If STANDBY, should drop to NONE
        assertEquals("wrong event:", Type.BACKUPS_CHANGED, Futures.getUnchecked(dms.relinquishRole(N1, DID1)).type());
        assertEquals("wrong role for node:", NONE, dms.getRole(N1, DID1));

        //NONE - nothing happens
        assertEquals("wrong event:", Type.BACKUPS_CHANGED, Futures.getUnchecked(dms.relinquishRole(N1, DID2)).type());
        assertEquals("wrong role for node:", NONE, dms.getRole(N1, DID2));

    }

    @Ignore("Ignore until Delegate spec. is clear.")
    @Test
    public void testEvents() throws InterruptedException {
        //shamelessly copy other distributed store tests
        final CountDownLatch addLatch = new CountDownLatch(1);

        MastershipStoreDelegate checkAdd = new MastershipStoreDelegate() {
            @Override
            public void notify(MastershipEvent event) {
                assertEquals("wrong event:", Type.MASTER_CHANGED, event.type());
                assertEquals("wrong subject", DID1, event.subject());
                assertEquals("wrong subject", N1, event.roleInfo().master());
                addLatch.countDown();
            }
        };

        dms.setDelegate(checkAdd);
        dms.setMaster(N1, DID1);
        //this will fail until we do something about single-instance-ness
        assertTrue("Add event fired", addLatch.await(1, TimeUnit.SECONDS));
    }

    private class TestDistributedMastershipStore extends
            DistributedMastershipStore {
        public TestDistributedMastershipStore(StoreService storeService,
                KryoSerializer kryoSerialization) {
            this.storeService = storeService;
            this.serializer = kryoSerialization;
        }

        //helper to populate master/backup structures
        public void put(DeviceId dev, NodeId node,
                boolean master, boolean backup, boolean term) {
            RoleValue rv = dms.roleMap.get(dev);
            if (rv == null) {
                rv = new RoleValue();
            }

            if (master) {
                rv.add(MASTER, node);
                rv.reassign(node, STANDBY, NONE);
            }
            if (backup) {
                rv.add(STANDBY, node);
                rv.remove(MASTER, node);
                rv.remove(NONE, node);
            }
            if (term) {
                dms.terms.put(dev, 0);
            }
            dms.roleMap.put(dev, rv);
        }

        //a dumb utility function.
        public void dump() {
            for (Map.Entry<DeviceId, RoleValue> el : dms.roleMap.entrySet()) {
                System.out.println("DID: " + el.getKey());
                for (MastershipRole role : MastershipRole.values()) {
                    System.out.println("\t" + role.toString() + ":");
                    for (NodeId n : el.getValue().nodesOfRole(role)) {
                        System.out.println("\t\t" + n);
                    }
                }
            }
        }

        //increment term for a device
        public void increment(DeviceId dev) {
            Integer t = dms.terms.get(dev);
            if (t != null) {
                dms.terms.put(dev, ++t);
            }
        }

        //sets the "local" node
        public void setCurrent(ControllerNode node) {
            ((TestClusterService) clusterService).current = node;
        }
    }

    private class TestClusterService extends ClusterServiceAdapter {

        protected ControllerNode current;

        @Override
        public ControllerNode getLocalNode() {
            return current;
        }

        @Override
        public Set<ControllerNode> getNodes() {
            return Sets.newHashSet(CN1, CN2);
        }

    }
*/
}

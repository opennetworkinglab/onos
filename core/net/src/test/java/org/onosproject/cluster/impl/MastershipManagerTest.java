/*
 * Copyright 2014-present Open Networking Laboratory
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
package org.onosproject.cluster.impl;

import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onlab.junit.TestUtils;
import org.onlab.packet.IpAddress;
import org.onosproject.cluster.ClusterService;
import org.onosproject.cluster.ControllerNode;
import org.onosproject.cluster.DefaultControllerNode;
import org.onosproject.cluster.NodeId;
import org.onosproject.common.event.impl.TestEventDispatcher;
import org.onosproject.mastership.MastershipService;
import org.onosproject.mastership.MastershipStore;
import org.onosproject.mastership.MastershipTermService;
import org.onosproject.net.DeviceId;
import org.onosproject.net.region.Region;
import org.onosproject.net.region.RegionId;
import org.onosproject.net.region.RegionStore;
import org.onosproject.net.region.impl.RegionManager;
import org.onosproject.store.cluster.StaticClusterService;
import org.onosproject.store.region.impl.DistributedRegionStore;
import org.onosproject.store.service.TestStorageService;
import org.onosproject.store.trivial.SimpleMastershipStore;

import com.google.common.collect.Sets;
import com.google.common.util.concurrent.Futures;

import static org.junit.Assert.*;
import static org.onosproject.net.MastershipRole.MASTER;
import static org.onosproject.net.MastershipRole.NONE;
import static org.onosproject.net.MastershipRole.STANDBY;
import static org.onosproject.net.NetTestTools.injectEventDispatcher;
import static org.onosproject.net.region.Region.Type.METRO;

/**
 * Test codifying the mastership service contracts.
 */
public class MastershipManagerTest {

    private static final NodeId NID_LOCAL = new NodeId("local");
    private static final NodeId NID_OTHER = new NodeId("foo");
    private static final IpAddress LOCALHOST = IpAddress.valueOf("127.0.0.1");
    private static final DeviceId DEV_MASTER = DeviceId.deviceId("of:1");
    private static final DeviceId DEV_OTHER = DeviceId.deviceId("of:2");

    private static final RegionId RID1 = RegionId.regionId("r1");
    private static final RegionId RID2 = RegionId.regionId("r2");
    private static final DeviceId DID1 = DeviceId.deviceId("foo:d1");
    private static final DeviceId DID2 = DeviceId.deviceId("foo:d2");
    private static final DeviceId DID3 = DeviceId.deviceId("foo:d3");
    private static final NodeId NID1 = NodeId.nodeId("n1");
    private static final NodeId NID2 = NodeId.nodeId("n2");
    private static final NodeId NID3 = NodeId.nodeId("n3");
    private static final NodeId NID4 = NodeId.nodeId("n4");
    private static final ControllerNode CNODE1 =
            new DefaultControllerNode(NID1, IpAddress.valueOf("127.0.1.1"));
    private static final ControllerNode CNODE2 =
            new DefaultControllerNode(NID2, IpAddress.valueOf("127.0.1.2"));
    private static final ControllerNode CNODE3 =
            new DefaultControllerNode(NID3, IpAddress.valueOf("127.0.1.3"));
    private static final ControllerNode CNODE4 =
            new DefaultControllerNode(NID4, IpAddress.valueOf("127.0.1.4"));


    private MastershipManager mgr;
    protected MastershipService service;
    private TestRegionManager regionManager;
    private RegionStore regionStore;
    private TestClusterService testClusterService;

    @Before
    public void setUp() throws Exception {
        mgr = new MastershipManager();
        service = mgr;
        injectEventDispatcher(mgr, new TestEventDispatcher());
        testClusterService = new TestClusterService();
        mgr.clusterService = testClusterService;
        mgr.store = new TestSimpleMastershipStore(mgr.clusterService);
        regionStore = new DistributedRegionStore();
        TestUtils.setField(regionStore, "storageService", new TestStorageService());
        TestUtils.callMethod(regionStore, "activate",
                             new Class<?>[] {});
        regionManager = new TestRegionManager();
        TestUtils.setField(regionManager, "store", regionStore);
        regionManager.activate();
        mgr.regionService = regionManager;
        mgr.activate();
    }

    @After
    public void tearDown() {
        mgr.deactivate();
        mgr.clusterService = null;
        injectEventDispatcher(mgr, null);
        regionManager.deactivate();
        mgr.regionService = null;
        mgr.store = null;
    }

    @Test
    public void setRole() {
        mgr.setRole(NID_OTHER, DEV_MASTER, MASTER);
        assertEquals("wrong local role:", NONE, mgr.getLocalRole(DEV_MASTER));
        assertEquals("wrong obtained role:", STANDBY, Futures.getUnchecked(mgr.requestRoleFor(DEV_MASTER)));

        //set to master
        mgr.setRole(NID_LOCAL, DEV_MASTER, MASTER);
        assertEquals("wrong local role:", MASTER, mgr.getLocalRole(DEV_MASTER));
    }

    @Test
    public void relinquishMastership() {
        //no backups - should just turn to NONE for device.
        mgr.setRole(NID_LOCAL, DEV_MASTER, MASTER);
        assertEquals("wrong role:", MASTER, mgr.getLocalRole(DEV_MASTER));
        mgr.relinquishMastership(DEV_MASTER);
        assertNull("wrong master:", mgr.getMasterFor(DEV_OTHER));
        assertEquals("wrong role:", NONE, mgr.getLocalRole(DEV_MASTER));

        //not master, nothing should happen
        mgr.setRole(NID_LOCAL, DEV_OTHER, NONE);
        mgr.relinquishMastership(DEV_OTHER);
        assertNull("wrong role:", mgr.getMasterFor(DEV_OTHER));

        //provide NID_OTHER as backup and relinquish
        mgr.setRole(NID_LOCAL, DEV_MASTER, MASTER);
        assertEquals("wrong master:", NID_LOCAL, mgr.getMasterFor(DEV_MASTER));
        mgr.setRole(NID_OTHER, DEV_MASTER, STANDBY);
        mgr.relinquishMastership(DEV_MASTER);
        assertEquals("wrong master:", NID_OTHER, mgr.getMasterFor(DEV_MASTER));
    }

    @Test
    public void requestRoleFor() {
        mgr.setRole(NID_LOCAL, DEV_MASTER, MASTER);
        mgr.setRole(NID_OTHER, DEV_OTHER, MASTER);

        //local should be master for one but standby for other
        assertEquals("wrong role:", MASTER, Futures.getUnchecked(mgr.requestRoleFor(DEV_MASTER)));
        assertEquals("wrong role:", STANDBY, Futures.getUnchecked(mgr.requestRoleFor(DEV_OTHER)));
    }

    @Test
    public void getMasterFor() {
        mgr.setRole(NID_LOCAL, DEV_MASTER, MASTER);
        mgr.setRole(NID_OTHER, DEV_OTHER, MASTER);
        assertEquals("wrong master:", NID_LOCAL, mgr.getMasterFor(DEV_MASTER));
        assertEquals("wrong master:", NID_OTHER, mgr.getMasterFor(DEV_OTHER));

        //have NID_OTHER hand over DEV_OTHER to NID_LOCAL
        mgr.setRole(NID_LOCAL, DEV_OTHER, MASTER);
        assertEquals("wrong master:", NID_LOCAL, mgr.getMasterFor(DEV_OTHER));
    }

    @Test
    public void getDevicesOf() {
        mgr.setRole(NID_LOCAL, DEV_MASTER, MASTER);
        mgr.setRole(NID_LOCAL, DEV_OTHER, STANDBY);
        assertEquals("should be one device:", 1, mgr.getDevicesOf(NID_LOCAL).size());
        //hand both devices to NID_LOCAL
        mgr.setRole(NID_LOCAL, DEV_OTHER, MASTER);
        assertEquals("should be two devices:", 2, mgr.getDevicesOf(NID_LOCAL).size());
    }

    @Test
    public void termService() {
        MastershipTermService ts = mgr;

        //term = 1 for both
        mgr.setRole(NID_LOCAL, DEV_MASTER, MASTER);
        assertEquals("inconsistent term: ", 1, ts.getMastershipTerm(DEV_MASTER).termNumber());

        //hand devices to NID_LOCAL and back: term = 1 + 2
        mgr.setRole(NID_OTHER, DEV_MASTER, MASTER);
        mgr.setRole(NID_LOCAL, DEV_MASTER, MASTER);
        assertEquals("inconsistent terms: ", 3, ts.getMastershipTerm(DEV_MASTER).termNumber());
    }

    @Test
    public void balanceWithRegion1() {
        //set up region - 2 sets of masters with 1 node in each
        Set<NodeId> masterSet1 = ImmutableSet.of(NID1);
        Set<NodeId> masterSet2 = ImmutableSet.of(NID2);
        List<Set<NodeId>> masters = ImmutableList.of(masterSet1, masterSet2);
        Region r = regionManager.createRegion(RID1, "R1", METRO, masters);
        regionManager.addDevices(RID1, ImmutableSet.of(DID1, DID2));
        Set<DeviceId> deviceIds = regionManager.getRegionDevices(RID1);
        assertEquals("incorrect device count", 2, deviceIds.size());

        testClusterService.put(CNODE1, ControllerNode.State.ACTIVE);
        testClusterService.put(CNODE2, ControllerNode.State.ACTIVE);

        //set master to non region nodes
        mgr.setRole(NID_LOCAL, DID1, MASTER);
        mgr.setRole(NID_LOCAL, DID2, MASTER);
        assertEquals("wrong local role:", MASTER, mgr.getLocalRole(DID1));
        assertEquals("wrong local role:", MASTER, mgr.getLocalRole(DID2));
        assertEquals("wrong master:", NID_LOCAL, mgr.getMasterFor(DID1));
        assertEquals("wrong master:", NID_LOCAL, mgr.getMasterFor(DID2));

        //do region balancing
        mgr.useRegionForBalanceRoles = true;
        mgr.balanceRoles();
        assertEquals("wrong master:", NID1, mgr.getMasterFor(DID1));
        assertEquals("wrong master:", NID1, mgr.getMasterFor(DID2));

        // make N1 inactive
        testClusterService.put(CNODE1, ControllerNode.State.INACTIVE);
        mgr.balanceRoles();
        assertEquals("wrong master:", NID2, mgr.getMasterFor(DID1));
        assertEquals("wrong master:", NID2, mgr.getMasterFor(DID2));

    }

    @Test
    public void balanceWithRegion2() {
        //set up region - 2 sets of masters with (3 nodes, 1 node)
        Set<NodeId> masterSet1 = ImmutableSet.of(NID1, NID3, NID4);
        Set<NodeId> masterSet2 = ImmutableSet.of(NID2);
        List<Set<NodeId>> masters = ImmutableList.of(masterSet1, masterSet2);
        Region r = regionManager.createRegion(RID1, "R1", METRO, masters);
        Set<DeviceId> deviceIdsOrig = ImmutableSet.of(DID1, DID2, DID3, DEV_OTHER);
        regionManager.addDevices(RID1, deviceIdsOrig);
        Set<DeviceId> deviceIds = regionManager.getRegionDevices(RID1);
        assertEquals("incorrect device count", deviceIdsOrig.size(), deviceIds.size());
        assertEquals("incorrect devices in region", deviceIdsOrig, deviceIds);

        testClusterService.put(CNODE1, ControllerNode.State.ACTIVE);
        testClusterService.put(CNODE2, ControllerNode.State.ACTIVE);
        testClusterService.put(CNODE3, ControllerNode.State.ACTIVE);
        testClusterService.put(CNODE4, ControllerNode.State.ACTIVE);

        //set master to non region nodes
        deviceIdsOrig.forEach(deviceId1 -> mgr.setRole(NID_LOCAL, deviceId1, MASTER));
        checkDeviceMasters(deviceIds, Sets.newHashSet(NID_LOCAL), deviceId ->
                assertEquals("wrong local role:", MASTER, mgr.getLocalRole(deviceId)));

        //do region balancing
        mgr.useRegionForBalanceRoles = true;
        mgr.balanceRoles();
        Set<NodeId> expectedMasters = Sets.newHashSet(NID1, NID3, NID4);
        checkDeviceMasters(deviceIds, expectedMasters);

        // make N1 inactive
        testClusterService.put(CNODE1, ControllerNode.State.INACTIVE);
        expectedMasters.remove(NID1);
        mgr.balanceRoles();
        checkDeviceMasters(deviceIds, expectedMasters);

        // make N4 inactive
        testClusterService.put(CNODE4, ControllerNode.State.INACTIVE);
        expectedMasters.remove(NID4);
        mgr.balanceRoles();
        checkDeviceMasters(deviceIds, expectedMasters);

        // make N3 inactive
        testClusterService.put(CNODE3, ControllerNode.State.INACTIVE);
        expectedMasters = Sets.newHashSet(NID2);
        mgr.balanceRoles();
        checkDeviceMasters(deviceIds, expectedMasters);

        // make N3 active
        testClusterService.put(CNODE3, ControllerNode.State.ACTIVE);
        expectedMasters = Sets.newHashSet(NID3);
        mgr.balanceRoles();
        checkDeviceMasters(deviceIds, expectedMasters);

        // make N4 active
        testClusterService.put(CNODE4, ControllerNode.State.ACTIVE);
        expectedMasters.add(NID4);
        mgr.balanceRoles();
        checkDeviceMasters(deviceIds, expectedMasters);

        // make N1 active
        testClusterService.put(CNODE1, ControllerNode.State.ACTIVE);
        expectedMasters.add(NID1);
        mgr.balanceRoles();
        checkDeviceMasters(deviceIds, expectedMasters);
    }

    private void checkDeviceMasters(Set<DeviceId> deviceIds, Set<NodeId> expectedMasters) {
        checkDeviceMasters(deviceIds, expectedMasters, null);
    }

    private void checkDeviceMasters(Set<DeviceId> deviceIds, Set<NodeId> expectedMasters,
                                     Consumer<DeviceId> checkRole) {
        // each device's master must be contained in the list of expectedMasters
        deviceIds.forEach(deviceId -> {
            assertTrue("wrong master:", expectedMasters.contains(mgr.getMasterFor(deviceId)));
            if (checkRole != null) {
                checkRole.accept(deviceId);
            }
        });
        // each node in expectedMasters must have approximately the same number of devices
        if (expectedMasters.size() > 1) {
            int minValue = Integer.MAX_VALUE;
            int maxDevices = -1;
            for (NodeId nodeId: expectedMasters) {
                int numDevicesManagedByNode = mgr.getDevicesOf(nodeId).size();
                if (numDevicesManagedByNode < minValue) {
                    minValue = numDevicesManagedByNode;
                }
                if (numDevicesManagedByNode > maxDevices) {
                    maxDevices = numDevicesManagedByNode;
                }
                assertTrue("not balanced:", maxDevices - minValue <= 1);
            }
        }
    }

    private final class TestClusterService extends StaticClusterService {

        ControllerNode local = new DefaultControllerNode(NID_LOCAL, LOCALHOST);

        @Override
        public ControllerNode getLocalNode() {
            return local;
        }

        public void put(ControllerNode cn, ControllerNode.State state) {
            nodes.put(cn.id(), cn);
            nodeStates.put(cn.id(), state);
        }
    }

    private final class TestSimpleMastershipStore extends SimpleMastershipStore
            implements MastershipStore {

        public TestSimpleMastershipStore(ClusterService clusterService) {
            super.clusterService = clusterService;
        }
    }

    private class TestRegionManager extends RegionManager {
        TestRegionManager() {
            eventDispatcher = new TestEventDispatcher();
        }
    }
}

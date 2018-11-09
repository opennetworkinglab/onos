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

package org.onosproject.incubator.net.virtual.impl;

import com.google.common.collect.Sets;
import com.google.common.util.concurrent.Futures;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onlab.junit.TestUtils;
import org.onlab.osgi.ServiceDirectory;
import org.onlab.osgi.TestServiceDirectory;
import org.onlab.packet.IpAddress;
import org.onosproject.cluster.ClusterService;
import org.onosproject.cluster.ClusterServiceAdapter;
import org.onosproject.cluster.ControllerNode;
import org.onosproject.cluster.DefaultControllerNode;
import org.onosproject.cluster.NodeId;
import org.onosproject.common.event.impl.TestEventDispatcher;
import org.onosproject.core.CoreService;
import org.onosproject.event.EventDeliveryService;
import org.onosproject.net.TenantId;
import org.onosproject.incubator.net.virtual.VirtualNetwork;
import org.onosproject.incubator.net.virtual.VirtualNetworkMastershipStore;
import org.onosproject.incubator.net.virtual.VirtualNetworkStore;
import org.onosproject.incubator.net.virtual.store.impl.DistributedVirtualNetworkStore;
import org.onosproject.incubator.net.virtual.store.impl.SimpleVirtualMastershipStore;
import org.onosproject.mastership.MastershipService;
import org.onosproject.mastership.MastershipTermService;
import org.onosproject.net.DeviceId;
import org.onosproject.net.NetTestTools;
import org.onosproject.store.service.TestStorageService;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.onosproject.net.MastershipRole.MASTER;
import static org.onosproject.net.MastershipRole.STANDBY;
import static org.onosproject.net.MastershipRole.NONE;

public class VirtualNetworkMastershipManagerTest {

    private static final NodeId NID_LOCAL = new NodeId("local");
    private static final NodeId NID_OTHER = new NodeId("foo");
    private static final IpAddress LOCALHOST = IpAddress.valueOf("127.0.0.1");

    private static final TenantId TID = TenantId.tenantId("1");

    private static final DeviceId VDID1 = DeviceId.deviceId("foo:vd1");
    private static final DeviceId VDID2 = DeviceId.deviceId("foo:vd2");
    private static final DeviceId VDID3 = DeviceId.deviceId("foo:vd3");
    private static final DeviceId VDID4 = DeviceId.deviceId("foo:vd4");

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

    private VirtualNetworkManager manager;
    private DistributedVirtualNetworkStore virtualNetworkManagerStore;

    private VirtualNetworkMastershipManager mastershipMgr1;
    private VirtualNetworkMastershipManager mastershipMgr2;
    protected MastershipService service;
    private TestClusterService testClusterService;
    private EventDeliveryService eventDeliveryService;

    private VirtualNetwork vnet1;
    private VirtualNetwork vnet2;

    @Before
    public void setUp() throws Exception {
        virtualNetworkManagerStore = new DistributedVirtualNetworkStore();

        CoreService coreService = new TestCoreService();
        TestUtils.setField(virtualNetworkManagerStore, "coreService", coreService);
        TestUtils.setField(virtualNetworkManagerStore, "storageService", new TestStorageService());
        virtualNetworkManagerStore.activate();

        manager = new VirtualNetworkManager();
        manager.store = virtualNetworkManagerStore;
        TestUtils.setField(manager, "coreService", coreService);

        eventDeliveryService = new TestEventDispatcher();
        NetTestTools.injectEventDispatcher(manager, eventDeliveryService);

        SimpleVirtualMastershipStore store = new SimpleVirtualMastershipStore();
        TestUtils.setField(store, "coreService", coreService);
        store.activate();

        testClusterService = new TestClusterService();

        ServiceDirectory testDirectory = new TestServiceDirectory()
                .add(VirtualNetworkStore.class, virtualNetworkManagerStore)
                .add(CoreService.class, coreService)
                .add(EventDeliveryService.class, eventDeliveryService)
                .add(ClusterService.class, testClusterService)
                .add(VirtualNetworkMastershipStore.class, store);
        TestUtils.setField(manager, "serviceDirectory", testDirectory);

        manager.activate();

        createVnets();

        mastershipMgr1 = new VirtualNetworkMastershipManager(manager, vnet1.id());
        mastershipMgr2 = new VirtualNetworkMastershipManager(manager, vnet2.id());
        service = mastershipMgr1;
    }

    private void createVnets() {
        manager.registerTenantId(TID);

        vnet1 = manager.createVirtualNetwork(TID);
        manager.createVirtualDevice(vnet1.id(), VDID1);
        manager.createVirtualDevice(vnet1.id(), VDID2);

        vnet2 = manager.createVirtualNetwork(TID);
        manager.createVirtualDevice(vnet2.id(), VDID3);
        manager.createVirtualDevice(vnet2.id(), VDID4);
    }

    @After
    public void tearDown() {
        manager.deactivate();
        virtualNetworkManagerStore.deactivate();
    }

    @Test
    public void setRole() {
        mastershipMgr1.setRole(NID_OTHER, VDID1, MASTER);
        assertEquals("wrong local role:", NONE, mastershipMgr1.getLocalRole(VDID1));
        assertEquals("wrong obtained role:", STANDBY, Futures.getUnchecked(mastershipMgr1.requestRoleFor(VDID1)));

        //set to master
        mastershipMgr1.setRole(NID_LOCAL, VDID1, MASTER);
        assertEquals("wrong local role:", MASTER, mastershipMgr1.getLocalRole(VDID1));
    }

    @Test
    public void relinquishMastership() {
        //no backups - should just turn to NONE for device.
        mastershipMgr1.setRole(NID_LOCAL, VDID1, MASTER);
        assertEquals("wrong role:", MASTER, mastershipMgr1.getLocalRole(VDID1));
        mastershipMgr1.relinquishMastership(VDID1);
        assertNull("wrong master:", mastershipMgr1.getMasterFor(VDID2));
        assertEquals("wrong role:", NONE, mastershipMgr1.getLocalRole(VDID1));

        //not master, nothing should happen
        mastershipMgr1.setRole(NID_LOCAL, VDID2, NONE);
        mastershipMgr1.relinquishMastership(VDID2);
        assertNull("wrong role:", mastershipMgr1.getMasterFor(VDID2));

        //provide NID_OTHER as backup and relinquish
        mastershipMgr1.setRole(NID_LOCAL, VDID1, MASTER);
        assertEquals("wrong master:", NID_LOCAL, mastershipMgr1.getMasterFor(VDID1));
        mastershipMgr1.setRole(NID_OTHER, VDID1, STANDBY);
        mastershipMgr1.relinquishMastership(VDID1);
        assertEquals("wrong master:", NID_OTHER, mastershipMgr1.getMasterFor(VDID1));
    }

    @Test
    public void requestRoleFor() {
        mastershipMgr1.setRole(NID_LOCAL, VDID1, MASTER);
        mastershipMgr1.setRole(NID_OTHER, VDID2, MASTER);

        //local should be master for one but standby for other
        assertEquals("wrong role:", MASTER, Futures.getUnchecked(mastershipMgr1.requestRoleFor(VDID1)));
        assertEquals("wrong role:", STANDBY, Futures.getUnchecked(mastershipMgr1.requestRoleFor(VDID2)));
    }

    @Test
    public void getMasterFor() {
        mastershipMgr1.setRole(NID_LOCAL, VDID1, MASTER);
        mastershipMgr1.setRole(NID_OTHER, VDID2, MASTER);
        assertEquals("wrong master:", NID_LOCAL, mastershipMgr1.getMasterFor(VDID1));
        assertEquals("wrong master:", NID_OTHER, mastershipMgr1.getMasterFor(VDID2));

        //have NID_OTHER hand over VDID2 to NID_LOCAL
        mastershipMgr1.setRole(NID_LOCAL, VDID2, MASTER);
        assertEquals("wrong master:", NID_LOCAL, mastershipMgr1.getMasterFor(VDID2));
    }

    @Test
    public void getDevicesOf() {
        mastershipMgr1.setRole(NID_LOCAL, VDID1, MASTER);
        mastershipMgr1.setRole(NID_LOCAL, VDID2, STANDBY);
        assertEquals("should be one device:", 1, mastershipMgr1.getDevicesOf(NID_LOCAL).size());
        //hand both devices to NID_LOCAL
        mastershipMgr1.setRole(NID_LOCAL, VDID2, MASTER);
        assertEquals("should be two devices:", 2, mastershipMgr1.getDevicesOf(NID_LOCAL).size());
    }

    @Test
    public void termService() {
        MastershipTermService ts = mastershipMgr1;

        //term = 1 for both
        mastershipMgr1.setRole(NID_LOCAL, VDID1, MASTER);
        assertEquals("inconsistent term: ", 1,
                     ts.getMastershipTerm(VDID1).termNumber());

        //hand devices to NID_LOCAL and back: term = 1 + 2
        mastershipMgr1.setRole(NID_OTHER, VDID1, MASTER);
        mastershipMgr1.setRole(NID_LOCAL, VDID1, MASTER);
        assertEquals("inconsistent terms: ",
                     3, ts.getMastershipTerm(VDID1).termNumber());
    }

    @Test
    public void balanceWithVnets() {

        testClusterService.put(CNODE1, ControllerNode.State.ACTIVE);
        testClusterService.put(CNODE2, ControllerNode.State.ACTIVE);

        mastershipMgr1.setRole(NID_LOCAL, VDID1, MASTER);
        mastershipMgr1.setRole(NID_LOCAL, VDID2, MASTER);
        assertEquals("wrong local role:", MASTER, mastershipMgr1.getLocalRole(VDID1));
        assertEquals("wrong local role:", MASTER, mastershipMgr1.getLocalRole(VDID2));
        assertEquals("wrong master:", NID_LOCAL, mastershipMgr1.getMasterFor(VDID1));
        assertEquals("wrong master:", NID_LOCAL, mastershipMgr1.getMasterFor(VDID2));

        //do balancing according to vnet Id.
        mastershipMgr1.balanceRoles();
        assertEquals("wrong master:", NID1, mastershipMgr1.getMasterFor(VDID1));
        assertEquals("wrong master:", NID1, mastershipMgr1.getMasterFor(VDID2));

        mastershipMgr2.setRole(NID_LOCAL, VDID3, MASTER);
        mastershipMgr2.setRole(NID_LOCAL, VDID4, MASTER);
        assertEquals("wrong local role:", MASTER, mastershipMgr2.getLocalRole(VDID3));
        assertEquals("wrong local role:", MASTER, mastershipMgr2.getLocalRole(VDID4));
        assertEquals("wrong master:", NID_LOCAL, mastershipMgr2.getMasterFor(VDID3));
        assertEquals("wrong master:", NID_LOCAL, mastershipMgr2.getMasterFor(VDID4));

        //do balancing according to vnet Id.
        mastershipMgr2.balanceRoles();
        assertEquals("wrong master:", NID2, mastershipMgr2.getMasterFor(VDID3));
        assertEquals("wrong master:", NID2, mastershipMgr2.getMasterFor(VDID4));

        // make N1 inactive
        testClusterService.put(CNODE1, ControllerNode.State.INACTIVE);
        mastershipMgr1.balanceRoles();
        assertEquals("wrong master:", NID2, mastershipMgr1.getMasterFor(VDID1));
        assertEquals("wrong master:", NID2, mastershipMgr1.getMasterFor(VDID2));
    }

    private final class TestClusterService extends ClusterServiceAdapter {

        final Map<NodeId, ControllerNode> nodes = new HashMap<>();
        final Map<NodeId, ControllerNode.State> nodeStates = new HashMap<>();

        ControllerNode local = new DefaultControllerNode(NID_LOCAL, LOCALHOST);

        @Override
        public ControllerNode getLocalNode() {
            return local;
        }

        @Override
        public Set<ControllerNode> getNodes() {
            return Sets.newHashSet(nodes.values());
        }

        @Override
        public ControllerNode getNode(NodeId nodeId) {
            return nodes.get(nodeId);
        }

        @Override
        public ControllerNode.State getState(NodeId nodeId) {
            return nodeStates.get(nodeId);
        }

        public void put(ControllerNode cn, ControllerNode.State state) {
            nodes.put(cn.id(), cn);
            nodeStates.put(cn.id(), state);
        }
    }

    private final class TestSimpleMastershipStore extends SimpleVirtualMastershipStore
            implements VirtualNetworkMastershipStore {

        public TestSimpleMastershipStore(ClusterService clusterService) {
            super.clusterService = clusterService;
        }
    }
}
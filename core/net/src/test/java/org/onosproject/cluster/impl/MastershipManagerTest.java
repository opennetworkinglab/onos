/*
 * Copyright 2014 Open Networking Laboratory
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

import java.util.Set;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onosproject.cluster.ClusterEventListener;
import org.onosproject.cluster.ClusterService;
import org.onosproject.cluster.ControllerNode;
import org.onosproject.cluster.ControllerNode.State;
import org.onosproject.cluster.DefaultControllerNode;
import org.onosproject.cluster.NodeId;
import org.onosproject.event.impl.TestEventDispatcher;
import org.onosproject.mastership.MastershipService;
import org.onosproject.mastership.MastershipStore;
import org.onosproject.mastership.MastershipTermService;
import org.onosproject.net.DeviceId;
import org.onosproject.store.trivial.impl.SimpleMastershipStore;
import org.onlab.packet.IpAddress;

import com.google.common.collect.Sets;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.onosproject.net.MastershipRole.*;

/**
 * Test codifying the mastership service contracts.
 */
public class MastershipManagerTest {

    private static final NodeId NID_LOCAL = new NodeId("local");
    private static final NodeId NID_OTHER = new NodeId("foo");
    private static final IpAddress LOCALHOST = IpAddress.valueOf("127.0.0.1");
    private static final DeviceId DEV_MASTER = DeviceId.deviceId("of:1");
    private static final DeviceId DEV_OTHER = DeviceId.deviceId("of:2");

    private MastershipManager mgr;
    protected MastershipService service;

    @Before
    public void setUp() {
        mgr = new MastershipManager();
        service = mgr;
        mgr.eventDispatcher = new TestEventDispatcher();
        mgr.clusterService = new TestClusterService();
        mgr.store = new TestSimpleMastershipStore(mgr.clusterService);
        mgr.activate();
    }

    @After
    public void tearDown() {
        mgr.deactivate();
        mgr.clusterService = null;
        mgr.eventDispatcher = null;
        mgr.store = null;
    }

    @Test
    public void setRole() {
        mgr.setRole(NID_OTHER, DEV_MASTER, MASTER);
        assertEquals("wrong local role:", NONE, mgr.getLocalRole(DEV_MASTER));
        assertEquals("wrong obtained role:", STANDBY, mgr.requestRoleFor(DEV_MASTER));

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
        assertEquals("wrong role:", MASTER, mgr.requestRoleFor(DEV_MASTER));
        assertEquals("wrong role:", STANDBY, mgr.requestRoleFor(DEV_OTHER));
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

    private final class TestClusterService implements ClusterService {

        ControllerNode local = new DefaultControllerNode(NID_LOCAL, LOCALHOST);

        @Override
        public ControllerNode getLocalNode() {
            return local;
        }

        @Override
        public Set<ControllerNode> getNodes() {
            return Sets.newHashSet();
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

    private final class TestSimpleMastershipStore extends SimpleMastershipStore
            implements MastershipStore {

        public TestSimpleMastershipStore(ClusterService clusterService) {
            super.clusterService = clusterService;
        }
    }
}

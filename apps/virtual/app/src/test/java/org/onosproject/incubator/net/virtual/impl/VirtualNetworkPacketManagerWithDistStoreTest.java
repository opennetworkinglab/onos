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

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.onlab.junit.TestUtils;
import org.onlab.osgi.ComponentContextAdapter;
import org.onosproject.cfg.ComponentConfigAdapter;
import org.onosproject.cluster.ClusterService;
import org.onosproject.cluster.ClusterServiceAdapter;
import org.onosproject.cluster.NodeId;
import org.onosproject.incubator.net.virtual.store.impl.DistributedVirtualPacketStore;
import org.onosproject.mastership.MastershipServiceAdapter;
import org.onosproject.net.DeviceId;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.packet.DefaultOutboundPacket;
import org.onosproject.net.packet.OutboundPacket;
import org.onosproject.store.cluster.messaging.ClusterCommunicationServiceAdapter;
import org.onosproject.store.cluster.messaging.MessageSubject;

import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import static org.junit.Assert.assertNull;

/**
 * Junit tests for VirtualNetworkPacketManager using DistributedVirtualPacketStore..
 * This test class extends VirtualNetworkPacketManagerTest - all the tests defined in
 * VirtualNetworkPacketManagerTest will run using DistributedVirtualPacketStore.
 */
public class VirtualNetworkPacketManagerWithDistStoreTest extends VirtualNetworkPacketManagerTest {

    private DistributedVirtualPacketStore distStore;
    private ClusterService clusterService = new ClusterServiceAdapter();

    @Before
    public void setUp() throws TestUtils.TestUtilsException {
        setUpDistPacketStore();
        super.setUp();
        TestUtils.setField(packetManager1, "storageService", storageService);
    }

    private void setUpDistPacketStore() throws TestUtils.TestUtilsException {
        distStore = new DistributedVirtualPacketStore();
        TestUtils.setField(distStore, "cfgService", new ComponentConfigAdapter());
        TestUtils.setField(distStore, "storageService", storageService);
        TestUtils.setField(distStore, "clusterService", clusterService);
        TestUtils.setField(distStore, "communicationService", new TestClusterCommunicationService());
        TestUtils.setField(distStore, "mastershipService", new TestMastershipService());

        distStore.activate(new ComponentContextAdapter());
        packetStore = distStore; // super.setUp() will cause Distributed store to be used.
    }

    @After
    public void tearDown() {
        distStore.deactivate();
    }

    @Override
    @Test
    @Ignore("Ignore until there is MastershipService support for virtual devices")
    public void emitTest() {
        super.emitTest();
    }

    /**
     * Tests the correct usage of emit() for a outbound packet - master of packet's
     * sendThrough is not local node.
     */
    @Test
    @Ignore("Ignore until there is MastershipService support for virtual devices")
    public void emit2Test() {
        OutboundPacket packet =
                new DefaultOutboundPacket(VDID2, DefaultTrafficTreatment.emptyTreatment(), ByteBuffer.allocate(5));
        packetManager1.emit(packet);
        assertNull("Packet should not have been emmitted", emittedPacket);
    }

    private final class TestMastershipService extends MastershipServiceAdapter {
        @Override
        public NodeId getMasterFor(DeviceId deviceId) {
            if (VDID1.equals(deviceId)) {
                return clusterService.getLocalNode().id();
            }
            return new NodeId("abc");
        }
    }

    private final class TestClusterCommunicationService extends ClusterCommunicationServiceAdapter {
        @Override
        public <M> CompletableFuture<Void> unicast(M message, MessageSubject subject,
                                                   Function<M, byte[]> encoder, NodeId toNodeId) {
            return new CompletableFuture<>();
        }
    }

}

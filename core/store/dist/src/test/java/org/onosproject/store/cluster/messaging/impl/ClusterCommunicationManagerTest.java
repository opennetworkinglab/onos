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
package org.onosproject.store.cluster.messaging.impl;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.onosproject.cluster.DefaultControllerNode;
import org.onosproject.cluster.NodeId;
import org.onosproject.store.cluster.impl.ClusterNodesDelegate;
import org.onlab.packet.IpAddress;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Tests of the cluster communication manager.
 */
public class ClusterCommunicationManagerTest {

    private static final NodeId N1 = new NodeId("n1");
    private static final NodeId N2 = new NodeId("n2");

    private static final int P1 = 9881;
    private static final int P2 = 9882;

    private static final IpAddress IP = IpAddress.valueOf("127.0.0.1");

    private ClusterCommunicationManager ccm1;
    private ClusterCommunicationManager ccm2;

    private TestDelegate cnd1 = new TestDelegate();
    private TestDelegate cnd2 = new TestDelegate();

    private DefaultControllerNode node1 = new DefaultControllerNode(N1, IP, P1);
    private DefaultControllerNode node2 = new DefaultControllerNode(N2, IP, P2);

    @Before
    public void setUp() throws Exception {

        NettyMessagingManager messagingService = new NettyMessagingManager();
        messagingService.activate();

        ccm1 = new ClusterCommunicationManager();
        ccm1.activate();

        ccm2 = new ClusterCommunicationManager();
        ccm2.activate();

//        ccm1.initialize(node1, cnd1);
//        ccm2.initialize(node2, cnd2);
    }

    @After
    public void tearDown() {
        ccm1.deactivate();
        ccm2.deactivate();
    }

    @Ignore("FIXME: failing randomly?")
    @Test
    public void connect() throws Exception {
        cnd1.latch = new CountDownLatch(1);
        cnd2.latch = new CountDownLatch(1);

//        ccm1.addNode(node2);
        validateDelegateEvent(cnd1, Op.DETECTED, node2.id());
        validateDelegateEvent(cnd2, Op.DETECTED, node1.id());
    }

    @Test
    @Ignore
    public void disconnect() throws Exception {
        cnd1.latch = new CountDownLatch(1);
        cnd2.latch = new CountDownLatch(1);

//        ccm1.addNode(node2);
        validateDelegateEvent(cnd1, Op.DETECTED, node2.id());
        validateDelegateEvent(cnd2, Op.DETECTED, node1.id());

        cnd1.latch = new CountDownLatch(1);
        cnd2.latch = new CountDownLatch(1);
        ccm1.deactivate();
//
//        validateDelegateEvent(cnd2, Op.VANISHED, node1.id());
    }

    private void validateDelegateEvent(TestDelegate delegate, Op op, NodeId nodeId)
            throws InterruptedException {
        assertTrue("did not connect in time", delegate.latch.await(2500, TimeUnit.MILLISECONDS));
        assertEquals("incorrect event", op, delegate.op);
        assertEquals("incorrect event node", nodeId, delegate.nodeId);
    }

    enum Op { DETECTED, VANISHED, REMOVED }

    private class TestDelegate implements ClusterNodesDelegate {

        Op op;
        CountDownLatch latch;
        NodeId nodeId;

        @Override
        public DefaultControllerNode nodeDetected(NodeId nodeId, IpAddress ip, int tcpPort) {
            latch(nodeId, Op.DETECTED);
            return new DefaultControllerNode(nodeId, ip, tcpPort);
        }

        @Override
        public void nodeVanished(NodeId nodeId) {
            latch(nodeId, Op.VANISHED);
        }

        @Override
        public void nodeRemoved(NodeId nodeId) {
            latch(nodeId, Op.REMOVED);
        }

        private void latch(NodeId nodeId, Op op) {
            this.op = op;
            this.nodeId = nodeId;
            latch.countDown();
        }
    }
}

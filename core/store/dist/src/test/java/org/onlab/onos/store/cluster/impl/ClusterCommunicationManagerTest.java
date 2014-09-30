package org.onlab.onos.store.cluster.impl;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onlab.onos.cluster.DefaultControllerNode;
import org.onlab.onos.cluster.NodeId;
import org.onlab.packet.IpPrefix;

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

    private static final IpPrefix IP = IpPrefix.valueOf("127.0.0.1");

    private ClusterCommunicationManager ccm1;
    private ClusterCommunicationManager ccm2;

    private TestDelegate cnd1 = new TestDelegate();
    private TestDelegate cnd2 = new TestDelegate();

    private DefaultControllerNode node1 = new DefaultControllerNode(N1, IP, P1);
    private DefaultControllerNode node2 = new DefaultControllerNode(N2, IP, P2);

    @Before
    public void setUp() {
        MessageSerializer messageSerializer = new MessageSerializer();
        messageSerializer.activate();

        ccm1 = new ClusterCommunicationManager();
        ccm1.serializationService = messageSerializer;
        ccm1.activate();

        ccm2 = new ClusterCommunicationManager();
        ccm2.serializationService = messageSerializer;
        ccm2.activate();

        ccm1.startUp(node1, cnd1);
        ccm2.startUp(node2, cnd2);
    }

    @After
    public void tearDown() {
        ccm1.deactivate();
        ccm2.deactivate();
    }

    @Test
    public void connect() throws Exception {
        cnd1.latch = new CountDownLatch(1);
        cnd2.latch = new CountDownLatch(1);

        ccm1.addNode(node2);
        validateDelegateEvent(cnd1, Op.DETECTED, node2.id());
        validateDelegateEvent(cnd2, Op.DETECTED, node1.id());
    }

    @Test
    public void disconnect() throws Exception {
        cnd1.latch = new CountDownLatch(1);
        cnd2.latch = new CountDownLatch(1);

        ccm1.addNode(node2);
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

    enum Op { DETECTED, VANISHED, REMOVED };

    private class TestDelegate implements ClusterNodesDelegate {

        Op op;
        CountDownLatch latch;
        NodeId nodeId;

        @Override
        public DefaultControllerNode nodeDetected(NodeId nodeId, IpPrefix ip, int tcpPort) {
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
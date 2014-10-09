package org.onlab.onos.store.cluster.impl;

import org.junit.After;
import org.junit.Before;
import org.onlab.onos.cluster.DefaultControllerNode;
import org.onlab.onos.cluster.NodeId;
import org.onlab.onos.store.cluster.messaging.impl.ClusterCommunicationManager;
import org.onlab.onos.store.cluster.messaging.impl.MessageSerializer;
import org.onlab.netty.NettyMessagingService;
import org.onlab.packet.IpPrefix;

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

    private DefaultControllerNode node1 = new DefaultControllerNode(N1, IP, P1);
    private DefaultControllerNode node2 = new DefaultControllerNode(N2, IP, P2);

    @Before
    public void setUp() throws Exception {
        MessageSerializer messageSerializer = new MessageSerializer();
        messageSerializer.activate();

        NettyMessagingService messagingService = new NettyMessagingService();
        messagingService.activate();

        ccm1 = new ClusterCommunicationManager();
        ccm1.activate();

        ccm2 = new ClusterCommunicationManager();
        ccm2.activate();
    }

    @After
    public void tearDown() {
        ccm1.deactivate();
        ccm2.deactivate();
    }
}
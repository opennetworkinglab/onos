/*
 * Copyright 2016-present Open Networking Foundation
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
package org.onosproject.store.intent.impl;

import org.junit.Before;
import org.junit.Test;
import org.onlab.junit.NullScheduledExecutor;
import org.onlab.packet.IpAddress;
import org.onosproject.cluster.ClusterServiceAdapter;
import org.onosproject.cluster.ControllerNode;
import org.onosproject.cluster.DefaultControllerNode;
import org.onosproject.cluster.Leader;
import org.onosproject.cluster.Leadership;
import org.onosproject.cluster.LeadershipEvent;
import org.onosproject.cluster.LeadershipEventListener;
import org.onosproject.cluster.LeadershipService;
import org.onosproject.cluster.LeadershipServiceAdapter;
import org.onosproject.cluster.NodeId;
import org.onosproject.common.event.impl.TestEventDispatcher;
import org.onosproject.net.intent.Key;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import static junit.framework.TestCase.assertFalse;
import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.anyString;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.reset;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for the WorkPartitionManager class.
 */
public class WorkPartitionManagerTest {

    private final LeadershipEvent event
            = new LeadershipEvent(LeadershipEvent.Type.CANDIDATES_CHANGED,
                                  new Leadership(ELECTION_PREFIX + "0",
                                                 new Leader(MY_NODE_ID, 0, 0),
                                                 Arrays.asList(MY_NODE_ID, OTHER_NODE_ID)));

    private static final NodeId MY_NODE_ID = new NodeId("local");
    private static final NodeId OTHER_NODE_ID = new NodeId("other");
    private static final NodeId INACTIVE_NODE_ID = new NodeId("inactive");

    private static final String ELECTION_PREFIX = "work-partition-";

        private LeadershipService leadershipService;
    private LeadershipEventListener leaderListener;

    private WorkPartitionManager partitionManager;

    @Before
    public void setUp() {
        leadershipService = createMock(LeadershipService.class);

        leadershipService.addListener(anyObject(LeadershipEventListener.class));
        expectLastCall().andDelegateTo(new TestLeadershipService());
        for (int i = 0; i < WorkPartitionManager.NUM_PARTITIONS; i++) {
            expect(leadershipService.runForLeadership(ELECTION_PREFIX + i))
                .andReturn(null)
                .times(1);
        }

        partitionManager = new WorkPartitionManager()
                .withScheduledExecutor(new NullScheduledExecutor());

        partitionManager.clusterService = new TestClusterService();
        partitionManager.localNodeId = MY_NODE_ID;
        partitionManager.leadershipService = leadershipService;
        partitionManager.eventDispatcher = new TestEventDispatcher();
    }

    /**
     * Configures a mock leadership service to have the specified number of
     * partitions owned by the local node and all other partitions owned by a
     * (fake) remote node.
     *
     * @param numMine number of partitions that should be owned by the local node
     */
    private void setUpLeadershipService(int numMine) {
        List<NodeId> allNodes = Arrays.asList(MY_NODE_ID, OTHER_NODE_ID);
        for (int i = 0; i < numMine; i++) {
            expect(leadershipService.getLeadership(ELECTION_PREFIX + i))
                                    .andReturn(new Leadership(ELECTION_PREFIX + i,
                                                              new Leader(MY_NODE_ID, 1, 1000),
                                                              allNodes))
                                    .anyTimes();
        }

        for (int i = numMine; i < WorkPartitionManager.NUM_PARTITIONS; i++) {
            expect(leadershipService.getLeadership(ELECTION_PREFIX + i))
                                    .andReturn(new Leadership(ELECTION_PREFIX + i,
                                                              new Leader(OTHER_NODE_ID, 1, 1000),
                                                              allNodes))
                                    .anyTimes();
        }
        for (int i = 0; i < WorkPartitionManager.NUM_PARTITIONS; i++) {
            expect(leadershipService.getCandidates(ELECTION_PREFIX + i))
            .andReturn(Arrays.asList(MY_NODE_ID, OTHER_NODE_ID))
            .anyTimes();
        }
    }

    /**
     * Tests that the PartitionManager's activate method correctly runs for
     * all the leader elections that it should.
     */
    @Test
    public void testActivate() {
        reset(leadershipService);

        leadershipService.addListener(anyObject(LeadershipEventListener.class));

        for (int i = 0; i < WorkPartitionManager.NUM_PARTITIONS; i++) {
            expect(leadershipService.runForLeadership(ELECTION_PREFIX + i))
                .andReturn(null)
                .times(1);
        }

        replay(leadershipService);

        partitionManager.activate();

        verify(leadershipService);
    }

    /**
     * Tests that the isMine method returns the correct result based on the
     * underlying leadership service data.
     */
    @Test
    public void testIsMine() {
        // We'll own only the first partition
        setUpLeadershipService(1);
        replay(leadershipService);

        Key myKey = new ControllableHashKey(0);
        Key notMyKey = new ControllableHashKey(1);

        assertTrue(partitionManager.isMine(myKey, Key::hash));
        assertFalse(partitionManager.isMine(notMyKey, Key::hash));

        // Make us the owner of 4 partitions now
        reset(leadershipService);
        setUpLeadershipService(4);
        replay(leadershipService);

        assertTrue(partitionManager.isMine(myKey, Key::hash));
        // notMyKey is now my key because because we're in control of that
        // partition now
        assertTrue(partitionManager.isMine(notMyKey, Key::hash));

        assertFalse(partitionManager.isMine(new ControllableHashKey(4), Key::hash));
    }

    /**
     * Tests sending in LeadershipServiceEvents in the case when we have
     * too many partitions. The event will trigger the partition manager to
     * schedule a rebalancing activity.
     */
    @Test
    public void testRebalanceScheduling() {
        // We have all the partitions so we'll need to relinquish some
        setUpLeadershipService(WorkPartitionManager.NUM_PARTITIONS);

        replay(leadershipService);

        partitionManager.activate();
        // Send in the event
        leaderListener.event(event);

        assertTrue(partitionManager.rebalanceScheduled.get());

        verify(leadershipService);
    }

    /**
     * Tests rebalance will trigger the right now of leadership withdraw calls.
     */
    @Test
    public void testRebalance() {
        // We have all the partitions so we'll need to relinquish some
        setUpLeadershipService(WorkPartitionManager.NUM_PARTITIONS);

        leadershipService.withdraw(anyString());
        expectLastCall().times(7);

        replay(leadershipService);

        partitionManager.activate();

        // trigger rebalance
        partitionManager.doRebalance();

        verify(leadershipService);
    }

    /**
     * Tests that attempts to rebalance when the paritions are already
     * evenly distributed does not result in any relinquish attempts.
     */
    @Test
    public void testNoRebalance() {
        // Partitions are already perfectly balanced among the two active instances
        setUpLeadershipService(WorkPartitionManager.NUM_PARTITIONS / 2);
        replay(leadershipService);

        partitionManager.activate();

        // trigger rebalance
        partitionManager.doRebalance();

        verify(leadershipService);

        reset(leadershipService);
        // We have a smaller share than we should
        setUpLeadershipService(WorkPartitionManager.NUM_PARTITIONS / 2 - 1);
        replay(leadershipService);

        // trigger rebalance
        partitionManager.doRebalance();

        verify(leadershipService);
    }

    /**
     * LeadershipService that allows us to grab a reference to
     * PartitionManager's LeadershipEventListener.
     */
    public class TestLeadershipService extends LeadershipServiceAdapter {
        @Override
        public void addListener(LeadershipEventListener listener) {
            leaderListener = listener;
        }
    }

    /**
     * ClusterService set up with a very simple cluster - 3 nodes, one is the
     * current node, one is a different active node, and one is an inactive node.
     */
    private class TestClusterService extends ClusterServiceAdapter {

        private final ControllerNode self =
                new DefaultControllerNode(MY_NODE_ID, IpAddress.valueOf(1));
        private final ControllerNode otherNode =
                new DefaultControllerNode(OTHER_NODE_ID, IpAddress.valueOf(2));
        private final ControllerNode inactiveNode =
                new DefaultControllerNode(INACTIVE_NODE_ID, IpAddress.valueOf(3));

        Set<ControllerNode> nodes;

        public TestClusterService() {
            nodes = new HashSet<>();
            nodes.add(self);
            nodes.add(otherNode);
            nodes.add(inactiveNode);
        }

        @Override
        public ControllerNode getLocalNode() {
            return self;
        }

        @Override
        public Set<ControllerNode> getNodes() {
            return nodes;
        }

        @Override
        public ControllerNode getNode(NodeId nodeId) {
            return nodes.stream()
                    .filter(c -> c.id().equals(nodeId))
                    .findFirst()
                    .get();
        }

        @Override
        public ControllerNode.State getState(NodeId nodeId) {
            return nodeId.equals(INACTIVE_NODE_ID) ? ControllerNode.State.INACTIVE :
                   ControllerNode.State.ACTIVE;
        }
    }

    /**
     * A key that always hashes to a value provided to the constructor. This
     * allows us to control the hash of the key for unit tests.
     */
    private class ControllableHashKey extends Key {

        protected ControllableHashKey(long hash) {
            super(hash);
        }

        @Override
        public int hashCode() {
            return Objects.hash(hash());
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof ControllableHashKey)) {
                return false;
            }

            ControllableHashKey that = (ControllableHashKey) obj;

            return Objects.equals(this.hash(), that.hash());
        }

        @Override
        public int compareTo(Key o) {
            Long thisHash = hash();
            return thisHash.compareTo(o.hash());
        }
    }
}

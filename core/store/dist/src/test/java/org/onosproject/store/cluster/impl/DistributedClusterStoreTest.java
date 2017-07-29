/*
 * Copyright 2016-present Open Networking Laboratory
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
package org.onosproject.store.cluster.impl;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onlab.packet.IpAddress;
import org.onosproject.cfg.ComponentConfigAdapter;
import org.onosproject.cluster.ClusterEvent;
import org.onosproject.cluster.ClusterMetadataServiceAdapter;
import org.onosproject.cluster.ClusterStore;
import org.onosproject.cluster.ClusterStoreDelegate;
import org.onosproject.cluster.ControllerNode;
import org.onosproject.cluster.DefaultControllerNode;
import org.onosproject.cluster.NodeId;
import org.onosproject.core.Version;
import org.onosproject.core.VersionServiceAdapter;
import org.onosproject.store.cluster.messaging.impl.NettyMessagingManager;

import java.util.Set;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertFalse;

/**
 * Unit test for DistributedClusterStore.
 */
public class DistributedClusterStoreTest {
    DistributedClusterStore distributedClusterStore;
    ClusterStore clusterStore;
    NodeId nodeId;
    ControllerNode local;
    private static final NodeId NID1 = new NodeId("foo");
    private static final NodeId NID2 = new NodeId("bar");
    private static final NodeId NID3 = new NodeId("buz");

    private static final IpAddress IP1 = IpAddress.valueOf("127.0.0.1");
    private static final IpAddress IP2 = IpAddress.valueOf("127.0.0.2");
    private static final IpAddress IP3 = IpAddress.valueOf("127.0.0.3");

    private static final int PORT1 = 1;
    private static final int PORT2 = 2;
    private  static Set<ControllerNode> nodes;

    private TestDelegate delegate = new TestDelegate();
    private class TestDelegate implements ClusterStoreDelegate {
    private ClusterEvent event;
        @Override
        public void notify(ClusterEvent event) {
            this.event = event;
        }
    }

    @Before
    public void setUp() throws Exception {
        distributedClusterStore = new DistributedClusterStore();
        distributedClusterStore.clusterMetadataService = new ClusterMetadataServiceAdapter() {
            @Override
            public ControllerNode getLocalNode() {
                return new DefaultControllerNode(NID1, IP1);
            }
        };
        distributedClusterStore.messagingService = new NettyMessagingManager();
        distributedClusterStore.cfgService = new ComponentConfigAdapter();
        distributedClusterStore.versionService = new VersionServiceAdapter() {
            @Override
            public Version version() {
                return Version.version("1.1.1");
            }
        };
        distributedClusterStore.activate();
        clusterStore = distributedClusterStore;
    }

    @After
    public void tearDown() throws Exception {
        distributedClusterStore.deactivate();
    }

    @Test
    public void testEmpty() {
        nodeId = new NodeId("newNode");
        assertThat(clusterStore.getNode((nodeId)), is(nullValue()));
        assertFalse(clusterStore.hasDelegate());
        assertThat(clusterStore.getState(nodeId), is(ControllerNode.State.INACTIVE));
        assertThat(clusterStore.getVersion(nodeId), is(nullValue()));
    }

    @Test
    public void addNodes() {
        clusterStore.setDelegate(delegate);
        assertThat(clusterStore.hasDelegate(), is(true));
        clusterStore.addNode(NID1, IP1, PORT1);
        clusterStore.addNode(NID2, IP2, PORT2);
        clusterStore.removeNode(NID1);

        assertThat(clusterStore.getNode(NID1), is(nullValue()));
        clusterStore.addNode(NID3, IP3, PORT2);

        clusterStore.markFullyStarted(true);
        assertThat(clusterStore.getState(clusterStore.getLocalNode().id()),
                is(ControllerNode.State.READY));
        clusterStore.markFullyStarted(false);
        assertThat(clusterStore.getState(clusterStore.getLocalNode().id()),
                is(ControllerNode.State.ACTIVE));
        nodes = clusterStore.getNodes();
        assertThat(nodes.size(), is(2));
        clusterStore.markFullyStarted(true);

        clusterStore.unsetDelegate(delegate);
        assertThat(clusterStore.hasDelegate(), is(false));
    }
}

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

package org.onosproject.ui.model.topo;

import org.junit.Before;
import org.junit.Test;
import org.onlab.packet.IpAddress;
import org.onosproject.cluster.ControllerNode;
import org.onosproject.cluster.DefaultControllerNode;
import org.onosproject.cluster.NodeId;
import org.onosproject.ui.model.AbstractUiModelTest;

import static org.junit.Assert.assertEquals;

/**
 * Unit tests for {@link UiClusterMember}.
 */
public class UiClusterMemberTest extends AbstractUiModelTest {

    private static final NodeId NODE_ID = NodeId.nodeId("Node-1");
    private static final IpAddress NODE_IP = IpAddress.valueOf("1.2.3.4");

    private static final ControllerNode CNODE_1 =
            new DefaultControllerNode(NODE_ID, NODE_IP);

    private UiTopology topo;
    private UiClusterMember member;

    @Before
    public void setUp() {
        topo = new UiTopology();
    }

    @Test
    public void basic() {
        title("basic");
        member = new UiClusterMember(topo, CNODE_1);
        print(member);

        assertEquals("wrong id", NODE_ID, member.id());
        assertEquals("wrong IP", NODE_IP, member.ip());
        assertEquals("unex. online", false, member.isOnline());
        assertEquals("unex. ready", false, member.isReady());
        assertEquals("unex. device count", 0, member.deviceCount());
    }
}

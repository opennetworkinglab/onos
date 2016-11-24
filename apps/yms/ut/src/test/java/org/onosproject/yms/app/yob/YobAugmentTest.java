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

package org.onosproject.yms.app.yob;

import org.junit.Test;
import org.onosproject.yang.gen.v1.urn.ip.topo.rev20140101.ymsiptopology.node.AugmentedTopoNode;
import org.onosproject.yang.gen.v1.urn.ip.topo.rev20140101.ymsiptopology.node.DefaultAugmentedTopoNode;
import org.onosproject.yang.gen.v1.urn.topo.rev20140101.YmsTopologyOpParam;
import org.onosproject.yang.gen.v1.urn.topo.rev20140101.ymstopology.DefaultNode;
import org.onosproject.yms.app.ydt.YangRequestWorkBench;
import org.onosproject.yms.app.ydt.YdtExtendedContext;
import org.onosproject.yms.ydt.YdtContext;

import java.io.IOException;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.onosproject.yms.app.yob.YobTestUtils.NODE;
import static org.onosproject.yms.app.yob.YobTestUtils.ROOT_DATA_RESOURCE;
import static org.onosproject.yms.app.yob.YobTestUtils.ROUTER_ID;
import static org.onosproject.yms.app.yob.YobTestUtils.ROUTER_IP;
import static org.onosproject.yms.app.yob.YobTestUtils.STR_LEAF_VALUE;
import static org.onosproject.yms.app.yob.YobTestUtils.TOPOLOGY;
import static org.onosproject.yms.ydt.YdtContextOperationType.NONE;

/**
 * Test the YANG object building for the YANG data tree based on the non
 * schema augmented nodes.
 */
public class YobAugmentTest {

    private YobTestUtils utils = YobTestUtils.instance();

    @Test
    public void augmentedLeaf() throws IOException {

        YangRequestWorkBench ydtBuilder = new YangRequestWorkBench(
                ROOT_DATA_RESOURCE, null, null, utils.schemaRegistry(), true);

        ydtBuilder.addChild(TOPOLOGY, null, NONE);
        ydtBuilder.addChild(NODE, null);
        ydtBuilder.addLeaf(ROUTER_ID, "urn:ip:topo", STR_LEAF_VALUE);

        YdtContext logicalRoot = ydtBuilder.getRootNode();
        YdtExtendedContext appRoot =
                (YdtExtendedContext) logicalRoot.getFirstChild();

        DefaultYobBuilder yobBuilder = new DefaultYobBuilder();
        Object yangObject = yobBuilder.getYangObject(appRoot,
                                                     utils.schemaRegistry());
        assertNotNull("Fail to create augmented YANG object", yangObject);

        assertEquals("invalid augmented node created", YmsTopologyOpParam.class,
                     yangObject.getClass());

        YmsTopologyOpParam topology = (YmsTopologyOpParam) yangObject;
        assertNotNull("failed to build augmented node", topology.node());
        assertEquals("Single node entry is expected", 1, topology.node().size());
        assertEquals("Node type is not DefaultNode", DefaultNode.class,
                     topology.node().get(0).getClass());

        DefaultNode node = (DefaultNode) topology.node().get(0);
        assertNotNull("Augmented info is missing", node.yangAugmentedInfo(
                AugmentedTopoNode.class));
        assertEquals("Augmented class is incorrect",
                     DefaultAugmentedTopoNode.class,
                     node.yangAugmentedInfo(AugmentedTopoNode.class)
                             .getClass());

        DefaultAugmentedTopoNode augmentedNode = (DefaultAugmentedTopoNode)
                node.yangAugmentedInfo(AugmentedTopoNode.class);
        assertThat("Augmented leaf value is incorrect",
                   augmentedNode.routerId(), is(STR_LEAF_VALUE));
    }

    @Test
    public void augmentedLeaves() throws IOException {
        YangRequestWorkBench ydtBuilder = new YangRequestWorkBench(
                ROOT_DATA_RESOURCE, null, null, utils.schemaRegistry(), true);

        ydtBuilder.addChild(TOPOLOGY, null, NONE);
        ydtBuilder.addChild(NODE, null);
        ydtBuilder.addLeaf(ROUTER_ID, "urn:ip:topo", STR_LEAF_VALUE);
        ydtBuilder.traverseToParent();
        ydtBuilder.addLeaf(ROUTER_IP, "urn:ip:topo", STR_LEAF_VALUE);

        YdtContext logicalRoot = ydtBuilder.getRootNode();
        YdtExtendedContext appRoot =
                (YdtExtendedContext) logicalRoot.getFirstChild();

        DefaultYobBuilder yobBuilder = new DefaultYobBuilder();
        Object yangObject = yobBuilder.getYangObject(appRoot,
                                                     utils.schemaRegistry());
        assertNotNull("Fail to create augmented YANG object", yangObject);

        assertEquals("invalid augmented node created",
                     YmsTopologyOpParam.class, yangObject.getClass());

        YmsTopologyOpParam topology = (YmsTopologyOpParam) yangObject;
        assertNotNull("failed to build augmented node", topology.node());
        assertEquals("Single node entry is expected", 1,
                     topology.node().size());
        assertEquals("Node type is not DefaultNode", DefaultNode.class,
                     topology.node().get(0).getClass());

        DefaultNode node = (DefaultNode) topology.node().get(0);
        assertNotNull("Augmented info is missing", node.yangAugmentedInfo(
                AugmentedTopoNode.class));
        assertEquals("Augmented class is incorrect",
                     DefaultAugmentedTopoNode.class,
                     node.yangAugmentedInfo(AugmentedTopoNode.class)
                             .getClass());

        DefaultAugmentedTopoNode augmentedNode = (DefaultAugmentedTopoNode)
                node.yangAugmentedInfo(AugmentedTopoNode.class);
        assertThat("Augmented router id is incorrect",
                   augmentedNode.routerId(), is(STR_LEAF_VALUE));
        assertThat("Augmented router ip is incorrect",
                   augmentedNode.routerIp(), is(STR_LEAF_VALUE));
    }
}

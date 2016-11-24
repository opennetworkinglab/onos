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
import org.onosproject.yang.gen.v1.urn.topo.rev20140101.YmsTopologyOpParam;
import org.onosproject.yang.gen.v1.urn.topo.rev20140101.ymstopology.DefaultNode;
import org.onosproject.yang.gen.v1.urn.topo.rev20140101.ymstopology.Node;
import org.onosproject.yang.gen.v1.urn.topo.rev20140101.ymstopology.node.choice1.Case1a;
import org.onosproject.yang.gen.v1.urn.topo.rev20140101.ymstopology.node.choice1.Case1b;
import org.onosproject.yang.gen.v1.urn.topo.rev20140101.ymstopology.node.choice1.DefaultCase1a;
import org.onosproject.yang.gen.v1.urn.topo.rev20140101.ymstopology.node.choice1.DefaultCase1b;
import org.onosproject.yang.gen.v1.urn.topo.rev20140101.ymstopology.node.choice1.case1b.choice1b.Case1Bi;
import org.onosproject.yang.gen.v1.urn.topo.rev20140101.ymstopology.node.choice1.case1b.choice1b.DefaultCase1Bi;
import org.onosproject.yms.app.ydt.YangRequestWorkBench;
import org.onosproject.yms.app.ydt.YdtExtendedContext;
import org.onosproject.yms.ydt.YdtContext;

import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.onosproject.yms.app.yob.YobTestUtils.LEAF_1A1;
import static org.onosproject.yms.app.yob.YobTestUtils.LEAF_1A2;
import static org.onosproject.yms.app.yob.YobTestUtils.LEAF_1BIA;
import static org.onosproject.yms.app.yob.YobTestUtils.LEAF_1BIB;
import static org.onosproject.yms.app.yob.YobTestUtils.NODE;
import static org.onosproject.yms.app.yob.YobTestUtils.ROOT_DATA_RESOURCE;
import static org.onosproject.yms.app.yob.YobTestUtils.STR_LEAF_VALUE;
import static org.onosproject.yms.app.yob.YobTestUtils.TOPOLOGY;
import static org.onosproject.yms.ydt.YdtContextOperationType.NONE;

/**
 * Test the YANG object building for the YANG data tree based on the non
 * schema choice and case nodes.
 */
public class YobChoiceTest {

    private YobTestUtils utils = YobTestUtils.instance();

    @Test
    public void caseInChoice() throws IOException {
        YangRequestWorkBench ydtBuilder = new YangRequestWorkBench(
                ROOT_DATA_RESOURCE, null, null, utils.schemaRegistry(), true);

        ydtBuilder.addChild(TOPOLOGY, null, NONE);
        ydtBuilder.addChild(NODE, null);
        ydtBuilder.addLeaf(LEAF_1A1, null, STR_LEAF_VALUE);

        YdtContext logicalRoot = ydtBuilder.getRootNode();
        YdtExtendedContext appRoot =
                (YdtExtendedContext) logicalRoot.getFirstChild();

        DefaultYobBuilder yobBuilder = new DefaultYobBuilder();
        Object yangObject = yobBuilder.getYangObject(appRoot,
                                                     utils.schemaRegistry());
        assertNotNull(yangObject);
        assertEquals("YANG object created is not topology object",
                     YmsTopologyOpParam.class, yangObject.getClass());

        YmsTopologyOpParam topology = (YmsTopologyOpParam) yangObject;
        assertNotNull("Failed to build the object", topology.node());
        assertEquals("Single node entry is expected", 1,
                     topology.node().size());
        assertEquals("Node type is not DefaultNode", DefaultNode.class,
                     topology.node().get(0).getClass());

        Node node = topology.node().get(0);
        assertNotNull("choice1 is not set in node", node.choice1());
        assertEquals("choice 1 type is not ", DefaultCase1a.class,
                     node.choice1().getClass());

        Case1a case1a = (Case1a) node.choice1();
        assertNotNull("leaf1a1 is not set in case", case1a.leaf1A1());
        assertEquals("leaf1a1 type is not correct", String.class,
                     case1a.leaf1A1().getClass());
        assertEquals("leaf1a1 value is not correct", STR_LEAF_VALUE,
                     case1a.leaf1A1());

    }

    @Test
    public void caseWithMultiAttribute() throws IOException {
        YangRequestWorkBench ydtBuilder = new YangRequestWorkBench(
                ROOT_DATA_RESOURCE, null, null, utils.schemaRegistry(), true);

        ydtBuilder.addChild(TOPOLOGY, null, NONE);
        ydtBuilder.addChild(NODE, null);
        ydtBuilder.addLeaf(LEAF_1A1, null, STR_LEAF_VALUE);
        ydtBuilder.traverseToParent();
        ydtBuilder.addLeaf(LEAF_1A2, null, STR_LEAF_VALUE);

        YdtContext logicalRoot = ydtBuilder.getRootNode();
        YdtExtendedContext appRoot =
                (YdtExtendedContext) logicalRoot.getFirstChild();

        DefaultYobBuilder yobBuilder = new DefaultYobBuilder();
        Object yangObject = yobBuilder.getYangObject(appRoot,
                                                     utils.schemaRegistry());
        assertNotNull(yangObject);
        assertEquals("YANG object created is not topology object",
                     YmsTopologyOpParam.class, yangObject.getClass());

        YmsTopologyOpParam topology = (YmsTopologyOpParam) yangObject;
        assertNotNull("Failed to build the object", topology.node());
        assertEquals("Single node entry is expected", 1,
                     topology.node().size());
        assertEquals("Node type is not DefaultNode", DefaultNode.class,
                     topology.node().get(0).getClass());

        Node node = topology.node().get(0);
        assertNotNull("choice1 is not set in node", node.choice1());
        assertEquals("choice 1 type is not ", DefaultCase1a.class,
                     node.choice1().getClass());

        Case1a case1a = (Case1a) node.choice1();
        assertNotNull("leaf1a1 is not set in case", case1a.leaf1A1());
        assertEquals("leaf1a1 type is not correct", String.class,
                     case1a.leaf1A1().getClass());
        assertEquals("leaf1a1 value is not correct", STR_LEAF_VALUE,
                     case1a.leaf1A1());

        assertNotNull("leaf1a2 is not set in case", case1a.leaf1A2());
        assertEquals("leaf1a2 type is not correct", String.class,
                     case1a.leaf1A2().getClass());
        assertEquals("leaf1a1 value is not correct", STR_LEAF_VALUE,
                     case1a.leaf1A1());

    }

    @Test
    public void recursiveChoice() throws IOException {
        YangRequestWorkBench ydtBuilder = new YangRequestWorkBench(
                ROOT_DATA_RESOURCE, null, null, utils.schemaRegistry(), true);

        ydtBuilder.addChild(TOPOLOGY, null, NONE);
        ydtBuilder.addChild(NODE, null);
        ydtBuilder.addLeaf(LEAF_1BIA, null, STR_LEAF_VALUE);

        YdtContext logicalRoot = ydtBuilder.getRootNode();
        YdtExtendedContext appRoot =
                (YdtExtendedContext) logicalRoot.getFirstChild();

        DefaultYobBuilder yobBuilder = new DefaultYobBuilder();
        Object yangObject = yobBuilder.getYangObject(appRoot,
                                                     utils.schemaRegistry());
        assertNotNull(yangObject);
        assertEquals("YANG object created is not topology object",
                     YmsTopologyOpParam.class, yangObject.getClass());

        YmsTopologyOpParam topology = (YmsTopologyOpParam) yangObject;
        assertNotNull("Failed to build the object", topology.node());
        assertEquals("Single node entry is expected", 1,
                     topology.node().size());
        assertEquals("Node type is not DefaultNode", DefaultNode.class,
                     topology.node().get(0).getClass());

        Node node = topology.node().get(0);
        assertNotNull("Choice 1 is not set in Node", node.choice1());
        assertEquals("Choice 1 is not of type DefaultCase1b",
                     DefaultCase1b.class, node.choice1().getClass());

        Case1b case1b = (Case1b) node.choice1();
        assertNotNull("Case1b does not have child choice1b ",
                      case1b.choice1b());
        assertEquals("choice1b is not of type DefaultCase1Bi",
                     DefaultCase1Bi.class, case1b.choice1b().getClass());

        Case1Bi case1Bi = (Case1Bi) case1b.choice1b();
        assertNotNull("leaf1bia is not set", case1Bi.leaf1Bia());
        assertEquals("leaf1bia type is not string", String.class,
                     case1Bi.leaf1Bia().getClass());
        assertEquals("leaf1bia value is wrong", STR_LEAF_VALUE,
                     case1Bi.leaf1Bia());
    }

    @Test
    public void recursiveChoiceWithMultipleAttribute() throws IOException {
        YangRequestWorkBench ydtBuilder = new YangRequestWorkBench(
                ROOT_DATA_RESOURCE, null, null, utils.schemaRegistry(), true);

        ydtBuilder.addChild(TOPOLOGY, null, NONE);
        ydtBuilder.addChild(NODE, null);
        ydtBuilder.addLeaf(LEAF_1BIA, null, STR_LEAF_VALUE);
        ydtBuilder.traverseToParent();
        ydtBuilder.addLeaf(LEAF_1BIB, null, STR_LEAF_VALUE);

        YdtContext logicalRoot = ydtBuilder.getRootNode();
        YdtExtendedContext appRoot =
                (YdtExtendedContext) logicalRoot.getFirstChild();

        DefaultYobBuilder yobBuilder = new DefaultYobBuilder();
        Object yangObject = yobBuilder.getYangObject(appRoot,
                                                     utils.schemaRegistry());
        assertNotNull(yangObject);
        assertEquals("YANG object created is not topology object",
                     YmsTopologyOpParam.class, yangObject.getClass());

        YmsTopologyOpParam topology = (YmsTopologyOpParam) yangObject;
        assertNotNull("Failed to build the object", topology.node());
        assertEquals("Single node entry is expected", 1,
                     topology.node().size());
        assertEquals("Node type is not DefaultNode", DefaultNode.class,
                     topology.node().get(0).getClass());

        Node node = topology.node().get(0);
        assertNotNull("Choice 1 is not set in Node", node.choice1());
        assertEquals("Choice 1 is not of type DefaultCase1b",
                     DefaultCase1b.class,
                     node.choice1().getClass());

        Case1b case1b = (Case1b) node.choice1();
        assertNotNull("Case1b does not have child choice1b ",
                      case1b.choice1b());
        assertEquals("choice1b is not of type DefaultCase1Bi",
                     DefaultCase1Bi.class,
                     case1b.choice1b().getClass());

        Case1Bi case1Bi = (Case1Bi) case1b.choice1b();
        assertNotNull("leaf1bia is not set", case1Bi.leaf1Bia());
        assertEquals("leaf1bia type is not string", String.class,
                     case1Bi.leaf1Bia().getClass());
        assertEquals("leaf1bia value is wrong", STR_LEAF_VALUE,
                     case1Bi.leaf1Bia());

        assertNotNull("leaf1bib is not set", case1Bi.leaf1Bib());
        assertEquals("leaf1bia type is not string", String.class,
                     case1Bi.leaf1Bib().getClass());
        assertEquals("leaf1bia value is wrong", STR_LEAF_VALUE,
                     case1Bi.leaf1Bib());
    }
}

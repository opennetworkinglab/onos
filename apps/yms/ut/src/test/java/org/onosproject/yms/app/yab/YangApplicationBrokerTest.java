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

package org.onosproject.yms.app.yab;

import org.junit.Test;
import org.onosproject.yangutils.datamodel.YangAugment;
import org.onosproject.yangutils.datamodel.YangNode;
import org.onosproject.yms.app.ydt.YangRequestWorkBench;
import org.onosproject.yms.app.ydt.YdtAppContext;
import org.onosproject.yms.app.ydt.YdtAppNodeOperationType;
import org.onosproject.yms.app.ydt.YdtNode;
import org.onosproject.yms.ydt.YdtContext;

import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.onosproject.yms.ydt.YdtContextOperationType.DELETE;
import static org.onosproject.yms.ydt.YdtContextOperationType.MERGE;
import static org.onosproject.yms.ydt.YmsOperationType.EDIT_CONFIG_REQUEST;
import static org.onosproject.yms.ydt.YmsOperationType.QUERY_CONFIG_REQUEST;
import static org.onosproject.yms.ydt.YmsOperationType.RPC_REQUEST;

/**
 * Unit test case for YANG application broker.
 */
public class YangApplicationBrokerTest {

    MockYmsManager ymsManager = new MockYmsManager();

    /**
     * Returns YANG data tree to check edit operation of container.
     *
     * @return YANG data tree
     */
    private YangRequestWorkBench buildYdtForEditOperationWithoutDelete() {
        String rootName = "root";
        YangRequestWorkBench defaultYdtBuilder =
                (YangRequestWorkBench) ymsManager.getYdtBuilder(rootName, null,
                                                                EDIT_CONFIG_REQUEST);
        defaultYdtBuilder.addChild("test", "ydt.test", MERGE);
        defaultYdtBuilder.addChild("cont1", null, MERGE);
        defaultYdtBuilder.addChild("cont2", null, MERGE);
        defaultYdtBuilder.addChild("cont3", null, MERGE);
        defaultYdtBuilder.addLeaf("leaf1", null, "1");
        defaultYdtBuilder.traverseToParent();
        defaultYdtBuilder.traverseToParent();
        defaultYdtBuilder.traverseToParent();
        defaultYdtBuilder.addLeaf("leaf4", null, "4");
        defaultYdtBuilder.traverseToParent();
        defaultYdtBuilder.traverseToParent();
        defaultYdtBuilder.addChild("cont4", null, MERGE);
        defaultYdtBuilder.addChild("cont5", null, MERGE);
        defaultYdtBuilder.addLeaf("leaf9", null, "9");
        defaultYdtBuilder.traverseToParent();
        defaultYdtBuilder.traverseToParent();
        defaultYdtBuilder.traverseToParent();
        defaultYdtBuilder.addLeaf("leaf10", null, "10");
        return defaultYdtBuilder;
    }

    private YangRequestWorkBench buildYdtForKeyLeavesInDeleteTree() {
        String rootName = "root";
        YangRequestWorkBench defaultYdtBuilder =
                (YangRequestWorkBench) ymsManager.getYdtBuilder(rootName, null,
                                                                EDIT_CONFIG_REQUEST);
        defaultYdtBuilder.addChild("test", "ydt.test", MERGE);
        defaultYdtBuilder.addChild("list2", null, MERGE);
        defaultYdtBuilder.addLeaf("leaf5", null, "5");
        defaultYdtBuilder.traverseToParent();
        defaultYdtBuilder.addLeaf("leaf6", null, "6");
        defaultYdtBuilder.traverseToParent();
        defaultYdtBuilder.addLeaf("leaf7", null, "7");
        defaultYdtBuilder.traverseToParent();
        defaultYdtBuilder.addChild("cont7", null, DELETE);
        defaultYdtBuilder.traverseToParent();
        defaultYdtBuilder.traverseToParent();
        return defaultYdtBuilder;
    }

    /**
     * Returns YANG data tree to check delete operation of container.
     *
     * @return YANG data tree
     */
    private YangRequestWorkBench buildYdtForEditOperationWithDelete() {
        String rootName = "rootNode";
        YangRequestWorkBench defaultYdtBuilder =
                (YangRequestWorkBench) ymsManager.getYdtBuilder(rootName, null,
                                                                EDIT_CONFIG_REQUEST);
        defaultYdtBuilder.addChild("test", "ydt.test", MERGE);
        defaultYdtBuilder.addChild("cont1", null, MERGE);
        defaultYdtBuilder.addChild("cont2", null, DELETE);
        defaultYdtBuilder.addChild("cont3", null, DELETE);
        defaultYdtBuilder.addLeaf("leaf1", null, "1");
        defaultYdtBuilder.traverseToParent();
        defaultYdtBuilder.traverseToParent();
        defaultYdtBuilder.traverseToParent();
        defaultYdtBuilder.addLeaf("leaf4", null, "4");
        defaultYdtBuilder.traverseToParent();
        defaultYdtBuilder.traverseToParent();
        defaultYdtBuilder.addChild("cont4", null, DELETE);
        defaultYdtBuilder.addChild("cont5", null, DELETE);
        defaultYdtBuilder.addLeaf("leaf9", null, "9");
        defaultYdtBuilder.traverseToParent();
        defaultYdtBuilder.traverseToParent();
        defaultYdtBuilder.traverseToParent();
        defaultYdtBuilder.addLeaf("leaf10", null, "10");
        return defaultYdtBuilder;
    }

    /**
     * Returns YANG data tree to check edit operation of list.
     *
     * @return YANG data tree
     */
    private YangRequestWorkBench buildYdtForListEditOperationWithoutDelete() {
        String rootName = "listWithoutDelete";
        Set<String> valueSet = new LinkedHashSet<>();
        valueSet.add("10");
        YangRequestWorkBench defaultYdtBuilder =
                (YangRequestWorkBench) ymsManager.getYdtBuilder(rootName, null,
                                                                EDIT_CONFIG_REQUEST);
        defaultYdtBuilder.addChild("test", "ydt.test", MERGE);
        defaultYdtBuilder.addChild("cont1", null, MERGE);
        defaultYdtBuilder.addChild("list1", null, MERGE);
        defaultYdtBuilder.addLeaf("leaf2", null, "2");
        defaultYdtBuilder.traverseToParent();
        defaultYdtBuilder.addLeaf("leaf3", null, "3");
        defaultYdtBuilder.traverseToParent();
        defaultYdtBuilder.traverseToParent();
        defaultYdtBuilder.addLeaf("leaf4", null, "4");
        defaultYdtBuilder.traverseToParent();
        defaultYdtBuilder.traverseToParent();
        defaultYdtBuilder.addChild("list2", null, MERGE);
        defaultYdtBuilder.addLeaf("leaf5", null, "5");
        defaultYdtBuilder.traverseToParent();
        defaultYdtBuilder.addLeaf("leaf6", null, "6");
        defaultYdtBuilder.traverseToParent();
        defaultYdtBuilder.addLeaf("leaf7", null, "7");
        defaultYdtBuilder.traverseToParent();
        defaultYdtBuilder.addLeaf("leaflist8", null, valueSet);
        defaultYdtBuilder.traverseToParent();
        defaultYdtBuilder.traverseToParent();
        defaultYdtBuilder.addLeaf("leaf10", null, "10");
        return defaultYdtBuilder;
    }

    /**
     * Returns YANG data tree to check delete operation of list.
     *
     * @return YANG data tree
     */
    private YangRequestWorkBench buildYdtForListEditOperationWithDelete() {
        String rootName = "listWithDelete";
        YangRequestWorkBench defaultYdtBuilder =
                (YangRequestWorkBench) ymsManager.getYdtBuilder(rootName, null,
                                                                EDIT_CONFIG_REQUEST);
        defaultYdtBuilder.addChild("test", "ydt.test", MERGE);
        defaultYdtBuilder.addChild("cont1", null, MERGE);
        defaultYdtBuilder.addChild("list1", null, DELETE);
        defaultYdtBuilder.addLeaf("leaf2", null, "2");
        defaultYdtBuilder.traverseToParent();
        defaultYdtBuilder.addLeaf("leaf3", null, "3");
        defaultYdtBuilder.traverseToParent();
        defaultYdtBuilder.traverseToParent();
        defaultYdtBuilder.addLeaf("leaf4", null, "4");
        defaultYdtBuilder.traverseToParent();
        defaultYdtBuilder.traverseToParent();
        defaultYdtBuilder.addChild("list2", null, DELETE);
        defaultYdtBuilder.addLeaf("leaf5", null, "5");
        defaultYdtBuilder.traverseToParent();
        defaultYdtBuilder.addLeaf("leaf6", null, "6");
        defaultYdtBuilder.traverseToParent();
        defaultYdtBuilder.traverseToParent();
        defaultYdtBuilder.addLeaf("leaf10", null, "10");
        return defaultYdtBuilder;
    }

    /**
     * Returns YANG data tree to check query operation of container.
     *
     * @return YANG data tree
     */
    private YangRequestWorkBench buildYdtForQueryOperation() {
        String rootName = "root";
        YangRequestWorkBench defaultYdtBuilder =
                (YangRequestWorkBench) ymsManager.getYdtBuilder(rootName, null,
                                                                QUERY_CONFIG_REQUEST);
        defaultYdtBuilder.addChild("test", "ydt.test");
        defaultYdtBuilder.addChild("cont1", null);
        defaultYdtBuilder.addChild("cont2", null);
        defaultYdtBuilder.addChild("cont3", null);
        defaultYdtBuilder.traverseToParent();
        defaultYdtBuilder.traverseToParent();
        defaultYdtBuilder.traverseToParent();
        defaultYdtBuilder.addChild("cont4", null);
        defaultYdtBuilder.addChild("cont5", null);
        return defaultYdtBuilder;
    }

    /**
     * Returns YANG data tree to check query operation of list.
     *
     * @return YANG data tree
     */
    private YangRequestWorkBench buildYdtForListQueryOperation() {
        String rootName = "listQuery";
        YangRequestWorkBench defaultYdtBuilder =
                (YangRequestWorkBench) ymsManager.getYdtBuilder(rootName, null,
                                                                QUERY_CONFIG_REQUEST);
        defaultYdtBuilder.addChild("test", "ydt.test");
        defaultYdtBuilder.addChild("cont1", null);
        defaultYdtBuilder.addChild("list1", null);
        defaultYdtBuilder.addLeaf("leaf2", null, "2");
        defaultYdtBuilder.traverseToParent();
        defaultYdtBuilder.traverseToParent();
        defaultYdtBuilder.traverseToParent();
        defaultYdtBuilder.addChild("list2", null);
        defaultYdtBuilder.addLeaf("leaf5", null, "5");
        defaultYdtBuilder.traverseToParent();
        defaultYdtBuilder.addLeaf("leaf6", null, "6");
        return defaultYdtBuilder;
    }

    /**
     * Returns YANG data tree to check delete operation of a node.
     *
     * @return YANG data tree
     */
    private YangRequestWorkBench buildYdtWithOneDeleteNode() {
        String rootName = "root";
        YangRequestWorkBench defaultYdtBuilder =
                (YangRequestWorkBench) ymsManager.getYdtBuilder(rootName, null,
                                                                EDIT_CONFIG_REQUEST);
        defaultYdtBuilder.addChild("test", "ydt.test");
        defaultYdtBuilder.addChild("cont1", null, MERGE);
        defaultYdtBuilder.traverseToParent();
        defaultYdtBuilder.addChild("cont4", null, DELETE);
        defaultYdtBuilder.traverseToParent();
        defaultYdtBuilder.addLeaf("leaf10", null, "10");
        return defaultYdtBuilder;
    }

    /**
     * Returns YANG data tree to check delete operation of last node.
     *
     * @return YANG data tree
     */
    private YangRequestWorkBench buildYdtWithDeleteNodeAsLastChild() {
        String rootName = "root";
        YangRequestWorkBench defaultYdtBuilder =
                (YangRequestWorkBench) ymsManager.getYdtBuilder(rootName, null,
                                                                EDIT_CONFIG_REQUEST);
        defaultYdtBuilder.addChild("test", "ydt.test", MERGE);
        defaultYdtBuilder.addChild("cont1", null, MERGE);
        defaultYdtBuilder.traverseToParent();
        defaultYdtBuilder.addChild("list2", null, MERGE);
        defaultYdtBuilder.addLeaf("leaf5", null, "10");
        defaultYdtBuilder.traverseToParent();
        defaultYdtBuilder.addLeaf("leaf6", null, "10");
        defaultYdtBuilder.traverseToParent();
        defaultYdtBuilder.traverseToParent();
        defaultYdtBuilder.addChild("cont4", null, DELETE);
        return defaultYdtBuilder;
    }

    /**
     * Returns YANG data tree to with delete operation of all the nodes.
     *
     * @return YANG data tree
     */
    private YangRequestWorkBench buildYdtWithAllDeleteNode() {
        String rootName = "root";
        YangRequestWorkBench defaultYdtBuilder =
                (YangRequestWorkBench) ymsManager.getYdtBuilder(rootName, null,
                                                                EDIT_CONFIG_REQUEST);
        defaultYdtBuilder.addChild("test", "ydt.test", DELETE);
        defaultYdtBuilder.addChild("cont1", null, DELETE);
        defaultYdtBuilder.traverseToParent();
        defaultYdtBuilder.addChild("list2", null, DELETE);
        defaultYdtBuilder.addLeaf("leaf5", null, "10");
        defaultYdtBuilder.traverseToParent();
        defaultYdtBuilder.addLeaf("leaf6", null, "10");
        defaultYdtBuilder.traverseToParent();
        defaultYdtBuilder.traverseToParent();
        defaultYdtBuilder.addChild("cont4", null, DELETE);
        return defaultYdtBuilder;
    }

    /**
     * Returns YANG data tree to check rpc operation with only input.
     *
     * @return YANG data tree
     */
    private YangRequestWorkBench buildYdtForRpcWithOnlyInput() {
        String rootName = "root";
        YangRequestWorkBench defaultYdtBuilder =
                (YangRequestWorkBench) ymsManager.getYdtBuilder(rootName, null,
                                                                RPC_REQUEST);
        defaultYdtBuilder.addChild("test", "ydt.test");
        defaultYdtBuilder.addChild("rock-the-house1", null);
        defaultYdtBuilder.addChild("input", null);
        defaultYdtBuilder.addLeaf("leaf13", null, "5");
        defaultYdtBuilder.traverseToParent();
        defaultYdtBuilder.traverseToParent();
        defaultYdtBuilder.traverseToParent();
        defaultYdtBuilder.traverseToParent();
        return defaultYdtBuilder;
    }

    /**
     * Returns YANG data tree to check rpc operation with only output.
     *
     * @return YANG data tree
     */
    private YangRequestWorkBench buildYdtForRpcWithOnlyOutput() {
        String rootName = "root";
        YangRequestWorkBench defaultYdtBuilder =
                (YangRequestWorkBench) ymsManager.getYdtBuilder(rootName, null,
                                                                RPC_REQUEST);
        defaultYdtBuilder.addChild("test", "ydt.test");
        defaultYdtBuilder.addChild("rock-the-house2", null);
        defaultYdtBuilder.addChild("output", null);
        defaultYdtBuilder.addLeaf("leaf14", null, "14");
        defaultYdtBuilder.traverseToParent();
        defaultYdtBuilder.traverseToParent();
        defaultYdtBuilder.traverseToParent();
        defaultYdtBuilder.traverseToParent();
        return defaultYdtBuilder;
    }

    /**
     * Returns YANG data tree to check rpc operation with both input and output.
     *
     * @return YANG data tree
     */
    private YangRequestWorkBench buildYdtForRpcWithBothInputOutput() {
        String rootName = "root";
        YangRequestWorkBench defaultYdtBuilder =
                (YangRequestWorkBench) ymsManager.getYdtBuilder(rootName, null,
                                                                RPC_REQUEST);
        defaultYdtBuilder.addChild("test", "ydt.test");
        defaultYdtBuilder.addChild("rock-the-house", null);
        defaultYdtBuilder.addChild("input", null);
        defaultYdtBuilder.addLeaf("zip-code", null, "5");
        defaultYdtBuilder.traverseToParent();
        defaultYdtBuilder.traverseToParent();
        defaultYdtBuilder.addChild("output", null);
        defaultYdtBuilder.addLeaf("hello", null, "5");
        defaultYdtBuilder.traverseToParent();
        defaultYdtBuilder.traverseToParent();
        defaultYdtBuilder.traverseToParent();
        defaultYdtBuilder.traverseToParent();
        return defaultYdtBuilder;
    }

    /**
     * Returns YANG data tree to check rpc operation.
     *
     * @return YANG data tree
     */
    private YangRequestWorkBench buildYdtForRpc() {
        String rootName = "root";
        YangRequestWorkBench defaultYdtBuilder =
                (YangRequestWorkBench) ymsManager.getYdtBuilder(rootName, null,
                                                                RPC_REQUEST);
        defaultYdtBuilder.addChild("test", "ydt.test");
        defaultYdtBuilder.addChild("rock-the-house3", null);
        defaultYdtBuilder.traverseToParent();
        defaultYdtBuilder.traverseToParent();
        return defaultYdtBuilder;
    }

    /**
     * Returns YANG data tree to check query operation with multiple level of
     * augment.
     *
     * @return YANG data tree
     */
    private YangRequestWorkBench buildYdtForQueryWithMultipleAugment() {
        String rootName = "root";
        YangRequestWorkBench defaultYdtBuilder =
                (YangRequestWorkBench) ymsManager.getYdtBuilder(rootName, null,
                                                                QUERY_CONFIG_REQUEST);
        defaultYdtBuilder.addChild("test", "ydt.test");
        defaultYdtBuilder.traverseToParent();
        return defaultYdtBuilder;
    }

    /**
     * Returns YANG data tree to check delete operation with multiple level of
     * augment.
     *
     * @return YANG data tree
     */
    private YangRequestWorkBench buildYdtForDeleteWithMultipleAugment() {
        String rootName = "root";
        YangRequestWorkBench defaultYdtBuilder =
                (YangRequestWorkBench) ymsManager.getYdtBuilder(rootName, null,
                                                                EDIT_CONFIG_REQUEST);
        defaultYdtBuilder.addChild("test", "ydt.test");
        defaultYdtBuilder.addChild("cont4", null, DELETE);
        defaultYdtBuilder.traverseToParent();
        defaultYdtBuilder.traverseToParent();
        return defaultYdtBuilder;
    }

    /**
     * Checks whether YANG data tree and delete tree is correct.
     */
    @Test
    public void validateDeleteTreeOnlyOneNodeInDeleteList()
            throws IOException, CloneNotSupportedException {
        YangRequestWorkBench defaultYdtBuilder =
                buildYdtForEditOperationWithDelete();
        YdtAppContext appContext =
                defaultYdtBuilder.getAppRootNode().getFirstChild();
        YdtContext ydtContext = appContext.getModuleContext();
        List<YdtContext> deleteNodes = appContext.getDeleteNodes();

        YdtContext cont1YdtContext;
        YdtContext cont2YdtContext;
        YdtContext cont3YdtContext;
        YdtContext cont4YdtContext;
        YdtContext deleteTree;

        // verify whether ydt tree is correct
        assertThat(ydtContext.getName(), is("test"));

        cont1YdtContext = ydtContext.getFirstChild();
        assertThat(cont1YdtContext.getName(), is("cont1"));

        cont2YdtContext = cont1YdtContext.getFirstChild();
        assertThat(cont2YdtContext.getName(), is("cont2"));

        cont3YdtContext = cont2YdtContext.getFirstChild();
        assertThat(cont3YdtContext.getName(), is("cont3"));

        ydtContext = cont3YdtContext.getFirstChild();
        assertThat(ydtContext.getName(), is("leaf1"));
        assertThat(ydtContext.getValue(), is("1"));

        ydtContext = cont2YdtContext.getNextSibling();
        assertThat(ydtContext.getName(), is("leaf4"));
        assertThat(ydtContext.getValue(), is("4"));

        cont4YdtContext = cont1YdtContext.getNextSibling();
        assertThat(cont4YdtContext.getName(), is("cont4"));

        ydtContext = cont4YdtContext.getFirstChild();
        assertThat(ydtContext.getName(), is("cont5"));

        ydtContext = ydtContext.getFirstChild();
        assertThat(ydtContext.getName(), is("leaf9"));
        assertThat(ydtContext.getValue(), is("9"));

        ydtContext = cont4YdtContext.getNextSibling();
        assertThat(ydtContext.getName(), is("leaf10"));
        assertThat(ydtContext.getValue(), is("10"));

        // build delete tree
        YangApplicationBroker yab = new YangApplicationBroker(null);
        deleteTree = yab.buildDeleteTree(deleteNodes);

        // verify whether delete ydt tree is correct
        assertThat(deleteTree.getFirstChild().getName(), is("test"));

        cont1YdtContext = deleteTree.getFirstChild().getFirstChild();
        assertThat(cont1YdtContext.getName(), is("cont1"));

        cont2YdtContext = cont1YdtContext.getFirstChild();
        assertThat(cont2YdtContext.getName(), is("cont2"));

        cont3YdtContext = cont2YdtContext.getFirstChild();
        assertThat(cont3YdtContext.getName(), is("cont3"));

        ydtContext = cont3YdtContext.getFirstChild();
        assertThat(ydtContext.getName(), is("leaf1"));
        assertThat(ydtContext.getValue(), is("1"));

        assertThat(cont2YdtContext.getNextSibling(), nullValue());

        cont4YdtContext = cont1YdtContext.getNextSibling();
        assertThat(cont4YdtContext.getName(), is("cont4"));

        ydtContext = cont4YdtContext.getFirstChild();
        assertThat(ydtContext.getName(), is("cont5"));

        ydtContext = ydtContext.getFirstChild();
        assertThat(ydtContext.getName(), is("leaf9"));
        assertThat(ydtContext.getValue(), is("9"));

        assertThat(cont4YdtContext.getNextSibling(), nullValue());

        // ydtTree after removing delete nodes
        ydtContext = appContext.getModuleContext();
        assertThat(ydtContext.getName(), is("test"));

        cont1YdtContext = ydtContext.getFirstChild();
        assertThat(cont1YdtContext.getName(), is("cont1"));

        ydtContext = cont1YdtContext.getFirstChild();
        assertThat(ydtContext.getName(), is("leaf4"));
        assertThat(ydtContext.getValue(), is("4"));

        ydtContext = cont1YdtContext.getNextSibling();
        assertThat(ydtContext.getName(), is("leaf10"));
        assertThat(ydtContext.getValue(), is("10"));
    }

    /**
     * Checks whether YANG data tree and delete tree is correct.
     */
    @Test
    public void validateListDeleteTree()
            throws IOException, CloneNotSupportedException {
        YangRequestWorkBench defaultYdtBuilder =
                buildYdtForListEditOperationWithDelete();
        YdtAppContext appContext =
                defaultYdtBuilder.getAppRootNode().getFirstChild();
        YdtContext ydtContext = appContext.getModuleContext();
        List<YdtContext> deleteNodes = appContext.getDeleteNodes();

        YdtContext cont1YdtContext;
        YdtContext list1YdtContext;
        YdtContext list2YdtContext;
        YdtContext deleteTree;

        // verify whether ydt tree is correct
        assertThat(ydtContext.getName(), is("test"));

        cont1YdtContext = ydtContext.getFirstChild();
        assertThat(cont1YdtContext.getName(), is("cont1"));

        list1YdtContext = cont1YdtContext.getFirstChild();
        assertThat(list1YdtContext.getName(), is("list1"));

        ydtContext = list1YdtContext.getFirstChild();
        assertThat(ydtContext.getName(), is("leaf2"));
        assertThat(ydtContext.getValue(), is("2"));

        ydtContext = ydtContext.getNextSibling();
        assertThat(ydtContext.getName(), is("leaf3"));
        assertThat(ydtContext.getValue(), is("3"));

        ydtContext = list1YdtContext.getNextSibling();
        assertThat(ydtContext.getName(), is("leaf4"));
        assertThat(ydtContext.getValue(), is("4"));

        list2YdtContext = cont1YdtContext.getNextSibling();
        assertThat(list2YdtContext.getName(), is("list2"));

        ydtContext = list2YdtContext.getFirstChild();
        assertThat(ydtContext.getName(), is("leaf5"));
        assertThat(ydtContext.getValue(), is("5"));

        ydtContext = ydtContext.getNextSibling();
        assertThat(ydtContext.getName(), is("leaf6"));
        assertThat(ydtContext.getValue(), is("6"));

        ydtContext = list2YdtContext.getNextSibling();
        assertThat(ydtContext.getName(), is("leaf10"));
        assertThat(ydtContext.getValue(), is("10"));

        // build delete tree
        YangApplicationBroker yab = new YangApplicationBroker(null);
        deleteTree = yab.buildDeleteTree(deleteNodes);

        assertThat(deleteTree.getFirstChild().getName(), is("test"));

        cont1YdtContext = deleteTree.getFirstChild().getFirstChild();
        assertThat(cont1YdtContext.getName(), is("cont1"));

        list1YdtContext = cont1YdtContext.getFirstChild();
        assertThat(list1YdtContext.getName(), is("list1"));

        ydtContext = list1YdtContext.getFirstChild();
        assertThat(ydtContext.getName(), is("leaf2"));
        assertThat(ydtContext.getValue(), is("2"));

        ydtContext = ydtContext.getNextSibling();
        assertThat(ydtContext.getName(), is("leaf3"));
        assertThat(ydtContext.getValue(), is("3"));

        assertThat(list1YdtContext.getNextSibling(), nullValue());

        list2YdtContext = cont1YdtContext.getNextSibling();
        assertThat(list2YdtContext.getName(), is("list2"));

        ydtContext = list2YdtContext.getFirstChild();
        assertThat(ydtContext.getName(), is("leaf5"));
        assertThat(ydtContext.getValue(), is("5"));

        ydtContext = ydtContext.getNextSibling();
        assertThat(ydtContext.getName(), is("leaf6"));
        assertThat(ydtContext.getValue(), is("6"));

        assertThat(ydtContext.getNextSibling(), nullValue());

        // verify whether ydt tree is correct
        ydtContext = appContext.getModuleContext();
        assertThat(ydtContext.getName(), is("test"));

        cont1YdtContext = ydtContext.getFirstChild();
        assertThat(cont1YdtContext.getName(), is("cont1"));

        ydtContext = cont1YdtContext.getFirstChild();
        assertThat(ydtContext.getName(), is("leaf4"));
        assertThat(ydtContext.getValue(), is("4"));

        ydtContext = cont1YdtContext.getNextSibling();
        assertThat(ydtContext.getName(), is("leaf10"));
        assertThat(ydtContext.getValue(), is("10"));
    }

    /**
     * Checks whether there is no exception when there is valid edit
     * request.
     */
    @Test
    public void testExecuteEditOperationWithoutDelete()
            throws IOException, CloneNotSupportedException {
        YangRequestWorkBench defaultYdtBuilder =
                buildYdtForEditOperationWithoutDelete();
        ymsManager.executeOperation(defaultYdtBuilder);
    }

    /**
     * Checks whether there is no exception when there is valid delete
     * request.
     */
    @Test
    public void testExecuteEditOperationWithDelete()
            throws IOException, CloneNotSupportedException {
        YangRequestWorkBench defaultYdtBuilder =
                buildYdtForEditOperationWithDelete();
        ymsManager.executeOperation(defaultYdtBuilder);
    }

    /**
     * Checks whether there is no exception when there is valid edit
     * request for list.
     */
    @Test
    public void testExecuteListEditOperationWithoutDelete()
            throws IOException, CloneNotSupportedException {
        YangRequestWorkBench defaultYdtBuilder =
                buildYdtForListEditOperationWithoutDelete();
        ymsManager.executeOperation(defaultYdtBuilder);
    }

    /**
     * Checks whether there is no exception when there is valid delete
     * request for list.
     */
    @Test
    public void testExecuteListEditOperationWithDelete()
            throws IOException, CloneNotSupportedException {
        YangRequestWorkBench defaultYdtBuilder =
                buildYdtForListEditOperationWithDelete();
        ymsManager.executeOperation(defaultYdtBuilder);
    }

    /**
     * Checks whether there is no exception when there is valid query
     * request.
     */
    @Test
    public void testExecuteQueryOperation()
            throws IOException, CloneNotSupportedException {
        YangRequestWorkBench defaultYdtBuilder = buildYdtForQueryOperation();
        ymsManager.executeOperation(defaultYdtBuilder);
    }

    /**
     * Checks whether there is no exception when there is valid query
     * request for list.
     */
    @Test
    public void testExecuteListQueryOperation()
            throws IOException, CloneNotSupportedException {
        YangRequestWorkBench defaultYdtBuilder =
                buildYdtForListQueryOperation();
        ymsManager.executeOperation(defaultYdtBuilder);
    }

    /**
     * Checks whether delete tree is updated correctly.
     */
    @Test
    public void testSiblingsInDeleteTree()
            throws IOException, CloneNotSupportedException {
        YangRequestWorkBench defaultYdtBuilder = buildYdtWithOneDeleteNode();
        YdtAppContext appContext =
                defaultYdtBuilder.getAppRootNode().getFirstChild();
        YdtContext ydtContext = appContext.getModuleContext();
        List<YdtContext> deleteNodes = appContext.getDeleteNodes();

        // verify whether ydt tree is correct
        assertThat(ydtContext.getName(), is("test"));

        ydtContext = ydtContext.getFirstChild();
        assertThat(ydtContext.getName(), is("cont1"));

        ydtContext = ydtContext.getNextSibling();
        assertThat(ydtContext.getName(), is("cont4"));

        ydtContext = ydtContext.getNextSibling();
        assertThat(ydtContext.getName(), is("leaf10"));
        assertThat(ydtContext.getValue(), is("10"));

        // build delete tree
        YangApplicationBroker yab = new YangApplicationBroker(null);
        YdtContext deleteTree = yab.buildDeleteTree(deleteNodes);

        assertThat(deleteTree.getFirstChild().getName(), is("test"));

        ydtContext = deleteTree.getFirstChild().getFirstChild();
        assertThat(ydtContext.getName(), is("cont4"));

        assertThat(ydtContext.getNextSibling(), nullValue());
        assertThat(ydtContext.getPreviousSibling(), nullValue());

        ydtContext = appContext.getModuleContext();

        // verify whether ydt tree is correct
        assertThat(ydtContext.getName(), is("test"));

        ydtContext = ydtContext.getFirstChild();
        assertThat(ydtContext.getName(), is("cont1"));

        ydtContext = ydtContext.getNextSibling();
        assertThat(ydtContext.getName(), is("leaf10"));
        assertThat(ydtContext.getValue(), is("10"));

        assertThat(ydtContext.getNextSibling(), nullValue());
    }

    /**
     * Checks last child is updated correctly after delete tree is built.
     */
    @Test
    public void testLastChildInYdtTree()
            throws IOException, CloneNotSupportedException {
        YangRequestWorkBench defaultYdtBuilder =
                buildYdtWithDeleteNodeAsLastChild();
        YdtAppContext appContext =
                defaultYdtBuilder.getAppRootNode().getFirstChild();
        YdtContext ydtContext = appContext.getModuleContext();
        List<YdtContext> deleteNodes = appContext.getDeleteNodes();
        assertThat(YdtAppNodeOperationType.BOTH,
                   is(appContext.getOperationType()));

        // verify whether ydt tree is correct
        assertThat(ydtContext.getName(), is("test"));

        ydtContext = ydtContext.getFirstChild();
        assertThat(ydtContext.getName(), is("cont1"));

        ydtContext = ydtContext.getNextSibling();
        assertThat(ydtContext.getName(), is("list2"));

        ydtContext = ydtContext.getNextSibling();
        assertThat(ydtContext.getName(), is("cont4"));

        assertThat(ydtContext.getNextSibling(), nullValue());

        // build delete tree
        YangApplicationBroker yab = new YangApplicationBroker(null);
        YdtContext deleteTree = yab.buildDeleteTree(deleteNodes);

        assertThat(deleteTree.getFirstChild().getName(), is("test"));

        ydtContext = deleteTree.getFirstChild().getFirstChild();
        assertThat(ydtContext.getName(), is("cont4"));

        ydtContext = deleteTree.getFirstChild().getLastChild();
        assertThat(ydtContext.getName(), is("cont4"));

        assertThat(ydtContext.getNextSibling(), nullValue());
        assertThat(ydtContext.getPreviousSibling(), nullValue());

        ydtContext = appContext.getModuleContext();

        assertThat(ydtContext.getLastChild().getName(), is("list2"));

        // verify whether ydt tree is correct
        assertThat(ydtContext.getName(), is("test"));

        ydtContext = ydtContext.getFirstChild();
        assertThat(ydtContext.getName(), is("cont1"));

        ydtContext = ydtContext.getNextSibling();
        assertThat(ydtContext.getName(), is("list2"));

        assertThat(ydtContext.getNextSibling(), nullValue());
    }

    /**
     * Checks YDT tree with all delete nodes.
     */
    @Test
    public void testYdtTreeWithAllDeleteNodes()
            throws IOException, CloneNotSupportedException {
        YangRequestWorkBench defaultYdtBuilder = buildYdtWithAllDeleteNode();
        YdtAppContext appContext =
                defaultYdtBuilder.getAppRootNode().getFirstChild();
        YdtContext ydtContext = appContext.getModuleContext();
        List<YdtContext> deleteNodes = appContext.getDeleteNodes();

        assertThat(YdtAppNodeOperationType.DELETE_ONLY,
                   is(appContext.getOperationType()));

        // verify whether ydt tree is correct
        assertThat(ydtContext.getName(), is("test"));

        ydtContext = ydtContext.getFirstChild();
        assertThat(ydtContext.getName(), is("cont1"));

        ydtContext = ydtContext.getNextSibling();
        assertThat(ydtContext.getName(), is("list2"));

        ydtContext = ydtContext.getNextSibling();
        assertThat(ydtContext.getName(), is("cont4"));

        assertThat(ydtContext.getNextSibling(), nullValue());

        // build delete tree
        YangApplicationBroker yab = new YangApplicationBroker(null);
        YdtContext deleteTree = yab.buildDeleteTree(deleteNodes);

        assertThat(deleteTree.getFirstChild().getName(), is("test"));

        ydtContext = deleteTree.getFirstChild().getFirstChild();
        assertThat(ydtContext.getName(), is("cont1"));

        ydtContext = ydtContext.getNextSibling();
        assertThat(ydtContext.getName(), is("list2"));

        ydtContext = ydtContext.getNextSibling();
        assertThat(ydtContext.getName(), is("cont4"));

        assertThat(ydtContext.getNextSibling(), nullValue());
    }

    /**
     * Checks whether key leaves are also available when there is delete
     * request for list.
     */
    @Test
    public void testKeyLeavesInDeleteTree() throws IOException, CloneNotSupportedException {
        YangRequestWorkBench defaultYdtBuilder = buildYdtForKeyLeavesInDeleteTree();

        YdtAppContext appContext =
                defaultYdtBuilder.getAppRootNode().getFirstChild();
        YdtContext ydtContext = appContext.getModuleContext();
        List<YdtContext> deleteNodes = appContext.getDeleteNodes();

        assertThat(YdtAppNodeOperationType.BOTH, is(appContext.getOperationType()));

        // verify whether ydt tree is correct
        assertThat(ydtContext.getName(), is("test"));

        ydtContext = ydtContext.getFirstChild();
        assertThat(ydtContext.getName(), is("list2"));

        ydtContext = ydtContext.getFirstChild();
        assertThat(ydtContext.getName(), is("leaf5"));
        assertThat(ydtContext.getValue(), is("5"));

        ydtContext = ydtContext.getNextSibling();
        assertThat(ydtContext.getName(), is("leaf6"));
        assertThat(ydtContext.getValue(), is("6"));

        ydtContext = ydtContext.getNextSibling();
        assertThat(ydtContext.getName(), is("leaf7"));
        assertThat(ydtContext.getValue(), is("7"));

        ydtContext = ydtContext.getNextSibling();
        assertThat(ydtContext.getName(), is("cont7"));

        assertThat(ydtContext.getNextSibling(), nullValue());

        // build delete tree
        YangApplicationBroker yab = new YangApplicationBroker(null);
        YdtContext deleteTree = yab.buildDeleteTree(deleteNodes);

        assertThat(deleteTree.getFirstChild().getName(), is("test"));

        ydtContext = deleteTree.getFirstChild().getFirstChild();
        assertThat(ydtContext.getName(), is("list2"));

        ydtContext = ydtContext.getFirstChild();
        assertThat(ydtContext, notNullValue());

        ydtContext = ydtContext.getNextSibling();
        assertThat(ydtContext, notNullValue());

        ydtContext = ydtContext.getNextSibling();
        assertThat(ydtContext.getName(), is("cont7"));

        assertThat(ydtContext.getNextSibling(), nullValue());

        ydtContext = appContext.getModuleContext();

        // verify whether ydt tree is correct
        assertThat(ydtContext.getName(), is("test"));

        ydtContext = ydtContext.getFirstChild();
        assertThat(ydtContext.getName(), is("list2"));

        ydtContext = ydtContext.getFirstChild();
        assertThat(ydtContext.getName(), is("leaf5"));
        assertThat(ydtContext.getValue(), is("5"));

        ydtContext = ydtContext.getNextSibling();
        assertThat(ydtContext.getName(), is("leaf6"));
        assertThat(ydtContext.getValue(), is("6"));

        ydtContext = ydtContext.getNextSibling();
        assertThat(ydtContext.getName(), is("leaf7"));
        assertThat(ydtContext.getValue(), is("7"));

        assertThat(ydtContext.getNextSibling(), nullValue());
    }

    /**
     * Checks YDT tree and application tree for query request with mutiple
     * augments.
     */
    @Test
    public void testApptreeForQueryWithMultipleAugment()
            throws IOException, CloneNotSupportedException {
        YangRequestWorkBench defaultYdtBuilder = buildYdtForQueryWithMultipleAugment();
        YdtAppContext appContext = defaultYdtBuilder.getAppRootNode()
                .getFirstChild();
        YdtContext ydtNode = appContext.getModuleContext();
        YangNode yangNode = (YangNode) ((YdtNode) ydtNode).getYangSchemaNode();

        YangApplicationBroker yab = new YangApplicationBroker(defaultYdtBuilder.
                getYangSchemaRegistry());
        yab.setAugGenMethodSet(defaultYdtBuilder.getAugGenMethodSet());
        yab.processAugmentForChildNode(appContext, yangNode);

        assertThat(appContext.getModuleContext().getName(), is("test"));

        appContext = appContext.getFirstChild();

        String augmentName = ((YangAugment) appContext
                .getAugmentingSchemaNode()).getTargetNode().get(0)
                .getResolvedNode().getJavaClassNameOrBuiltInType();
        assertThat(augmentName, is("cont4"));

        assertThat(appContext.getFirstChild(), nullValue());
        assertThat(appContext.getLastChild(), nullValue());
    }

    /**
     * Checks whether there is no exception when there is valid query request
     * for data resource with multiple augments.
     */
    @Test
    public void testQueryWithMultipleAugment()
            throws IOException, CloneNotSupportedException {
        YangRequestWorkBench defaultYdtBuilder = buildYdtForQueryWithMultipleAugment();
        ymsManager.executeOperation(defaultYdtBuilder);
    }

    /**
     * Checks whether YDT is updated correctly for delete with multiple augment.
     */
    @Test
    public void testYdtForDeleteWithMultipleAugment()
            throws IOException, CloneNotSupportedException {
        YangRequestWorkBench defaultYdtBuilder =
                buildYdtForDeleteWithMultipleAugment();
        YdtAppContext appContext = defaultYdtBuilder.getAppRootNode()
                .getFirstChild();

        YangApplicationBroker yab = new YangApplicationBroker(defaultYdtBuilder.
                getYangSchemaRegistry());
        yab.setAugGenMethodSet(defaultYdtBuilder.getAugGenMethodSet());
        YdtContext deleteTree = yab.buildDeleteTree(appContext.getDeleteNodes());
        yab.processAugmentedNodesForDelete(deleteTree.getFirstChild(),
                                           appContext);

        assertThat(appContext.getModuleContext().getName(), is("test"));

        appContext = appContext.getFirstChild();
        String augmentName = ((YangAugment) appContext
                .getAugmentingSchemaNode()).getTargetNode().get(0)
                .getResolvedNode().getJavaClassNameOrBuiltInType();
        assertThat(augmentName, is("cont4"));

        assertThat(appContext.getFirstChild(), nullValue());
        assertThat(appContext.getLastChild(), nullValue());

        YdtContext ydtContext = deleteTree.getFirstChild();
        assertThat(ydtContext.getName(), is("test"));

        ydtContext = ydtContext.getFirstChild();
        assertThat(ydtContext.getName(), is("cont4"));
    }

    /**
     * Checks whether there is no exception when there is valid delete request
     * for data resource with multiple augments.
     */
    @Test
    public void testDeleteWithMultipleAugment() {
        YangRequestWorkBench defaultYdtBuilder =
                buildYdtForDeleteWithMultipleAugment();
        ymsManager.executeOperation(defaultYdtBuilder);
    }

    /**
     * Checks execute operation for rpc request with only output.
     */
    @Test
    public void testRpcWithOutput()
            throws IOException, CloneNotSupportedException {
        YangRequestWorkBench defaultYdtBuilder =
                buildYdtForRpcWithOnlyOutput();
        ymsManager.executeOperation(defaultYdtBuilder);
    }

    /**
     * Checks execute operation for rpc request with only input.
     */
    @Test
    public void testRpcWithInput()
            throws IOException, CloneNotSupportedException {
        YangRequestWorkBench defaultYdtBuilder =
                buildYdtForRpcWithOnlyInput();
        ymsManager.executeOperation(defaultYdtBuilder);
    }

    /**
     * Checks execute operation for rpc request with input and output.
     */
    @Test
    public void testRpcWithInputOutput()
            throws IOException, CloneNotSupportedException {
        YangRequestWorkBench defaultYdtBuilder =
                buildYdtForRpcWithBothInputOutput();
        ymsManager.executeOperation(defaultYdtBuilder);
    }

    /**
     * Checks execute operation for rpc request without input and
     * output.
     */
    @Test
    public void testRpcWithoutInputOutput()
            throws IOException, CloneNotSupportedException {
        YangRequestWorkBench defaultYdtBuilder =
                buildYdtForRpc();
        ymsManager.executeOperation(defaultYdtBuilder);
    }
}

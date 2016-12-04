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

package org.onosproject.yms.app.ydt;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.onosproject.yms.app.ydt.exceptions.YdtException;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.onosproject.yms.app.ydt.YdtTestConstants.E_LEAF;
import static org.onosproject.yms.app.ydt.YdtTestConstants.E_LIST;
import static org.onosproject.yms.app.ydt.YdtTestConstants.E_TOPARENT;
import static org.onosproject.yms.app.ydt.YdtTestConstants.INV;
import static org.onosproject.yms.app.ydt.YdtTestConstants.LISTNS;
import static org.onosproject.yms.app.ydt.YdtTestConstants.LWC;
import static org.onosproject.yms.app.ydt.YdtTestUtils.getTestYdtBuilder;
import static org.onosproject.yms.app.ydt.YdtTestUtils.getYdtBuilder;
import static org.onosproject.yms.app.ydt.YdtTestUtils.listWithContainer1Ydt;
import static org.onosproject.yms.app.ydt.YdtTestUtils.listWithContainer2Ydt;
import static org.onosproject.yms.app.ydt.YdtTestUtils.listWithContainerYdt;
import static org.onosproject.yms.app.ydt.YdtTestUtils.listWithoutContainerYdt;
import static org.onosproject.yms.app.ydt.YdtTestUtils.validateLeafContents;
import static org.onosproject.yms.app.ydt.YdtTestUtils.validateLeafListContents;
import static org.onosproject.yms.app.ydt.YdtTestUtils.validateNodeContents;
import static org.onosproject.yms.app.ydt.YdtTestUtils.walkINTree;
import static org.onosproject.yms.ydt.YdtContextOperationType.MERGE;
import static org.onosproject.yms.ydt.YdtType.SINGLE_INSTANCE_NODE;

public class ListTest {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    private Set<String> valueSet = new HashSet();

    private static final String[] ERROR = {
            "rootlist is missing some of the keys of listwithcontainer.",
            "Duplicate entry with name invalid.",
            "Some of the key elements are not unique in listwithcontainer.",
            "Too few key parameters in listwithcontainer." +
                    " Expected 2; actual 1.",
            "Too many key parameters in listwithcontainer." +
                    " Expected 2; actual 3.",
            "Application with name \"" + "invalid\" doesn't exist.",
            "Too many instances of listwithcontainer. Expected maximum " +
                    "instances 3.",
            "Duplicate entry found under invalidinterval leaf-list node.",
            "YANG file error : Input value \"string\" is not a valid uint16.",
            "Schema node with name listwithcontainer doesn't exist.",
            "Duplicate entry with name rootlist."
    };

    private static final String[] EXPECTED = {
            "Entry Node is list.",
            "Entry Node is rootlist.",
            "Entry Node is listwithoutcontainer.",
            "Entry Node is invalidinterval.",
            "Exit Node is invalidinterval.",
            "Exit Node is listwithoutcontainer.",
            "Exit Node is rootlist.",
            "Exit Node is list."
    };

    List<String> keysValueList = new ArrayList<>();

    /**
     * Creates and validates rootlist module with listwithoutcontainer node.
     */
    @Test
    public void listwithoutcontainerTest() throws YdtException {
        YangRequestWorkBench ydtBuilder = listWithoutContainerYdt();
        validateTree(ydtBuilder);
        // walker test
        walkINTree(ydtBuilder, EXPECTED);
    }

    /**
     * Creates and validates rootlist module with listwithcontainer node
     * using addMultiInstanceChild interface for adding multi instance node.
     */
    @Test
    public void listwithcontainerTest() throws YdtException {
        YangRequestWorkBench ydtBuilder = listWithContainerYdt();
        validateListwithcontainerTree(ydtBuilder);
    }

    /**
     * Creates and validates rootlist module with listwithcontainer
     * node using addChild interface for adding multi instance node.
     */
    @Test
    public void listwithcontainer1Test() throws YdtException {
        YangRequestWorkBench ydtBuilder = listWithContainer1Ydt();
        validateListwithcontainerTree(ydtBuilder);
    }

    /**
     * Creates and validates rootlist module with multiple instances of
     * listwithcontainer node using addMultiInstanceChild interface for adding
     * multi instance node.
     */
    @Test
    public void listwithcontainer2Test() throws YdtException {
        YangRequestWorkBench ydtBuilder = listWithContainer2Ydt();
    }

    /**
     * Validates the given built ydt.
     */
    private void validateTree(YangRequestWorkBench ydtBuilder) {

        // assign root node to ydtNode for validating purpose.
        YdtNode ydtNode = (YdtNode) ydtBuilder.getRootNode();
        // Logical root node does not have operation type
        validateNodeContents(ydtNode, "list", null);

        ydtNode = ydtNode.getFirstChild();
        validateNodeContents(ydtNode, "rootlist", MERGE);
        ydtNode = ydtNode.getFirstChild();
        validateNodeContents(ydtNode, "listwithoutcontainer", MERGE);
        ydtNode = ydtNode.getFirstChild();
        validateLeafContents(ydtNode, INV, "12");
    }

    /**
     * Validates the given list with container built ydt.
     */
    private void validateListwithcontainerTree(
            YangRequestWorkBench ydtBuilder) {

        valueSet.add("1");
        valueSet.add("2");
        // assign root node to ydtNode for validating purpose.
        YdtNode ydtNode = (YdtNode) ydtBuilder.getRootNode();
        // Logical root node does not have operation type
        validateNodeContents(ydtNode, "list", null);

        ydtNode = ydtNode.getFirstChild();
        validateNodeContents(ydtNode, "rootlist", MERGE);
        ydtNode = ydtNode.getFirstChild();
        validateNodeContents(ydtNode, LWC, MERGE);
        ydtNode = ydtNode.getFirstChild();
        validateLeafContents(ydtNode, "invalid", "12");
        ydtNode = ydtNode.getNextSibling();
        validateLeafContents(ydtNode, "invalid1", "12");
        ydtNode = ydtNode.getNextSibling();
        validateLeafListContents(ydtNode, INV, valueSet);
        ydtNode = ydtNode.getNextSibling();
        validateNodeContents(ydtNode, "interface", MERGE);
        ydtNode = ydtNode.getFirstChild();
        validateLeafContents(ydtNode, INV, "12");
        ydtNode = ydtNode.getNextSibling();
        validateLeafContents(ydtNode, "invalid", "121");
    }

    /**
     * Tests the negative error scenario when application name for ydt is
     * invalid.
     */
    @Test
    public void negative1Test() {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage(ERROR[5]);
        getYdtBuilder("list", "invalid", "ydt.invalid", MERGE);
    }

    /**
     * Tests the negative error scenario when list node is not having all
     * key elements.
     */
    @Test
    public void negative2Test() throws YdtException {

        YangRequestWorkBench ydtBuilder = getTestYdtBuilder(LISTNS);
        ydtBuilder.addChild(LWC, LISTNS);
        ydtBuilder.addLeaf("invalid", LISTNS, "12");
        ydtBuilder.traverseToParent();

        traversToParentErrorMsgValidator(ydtBuilder, ERROR[0]);
    }

    /**
     * Tests the negative error scenario when duplicate entry of list node
     * is created.
     */
    @Test
    public void negative3Test() throws YdtException {
        YangRequestWorkBench ydtBuilder = getTestYdtBuilder(LISTNS);
        ydtBuilder.addChild(LWC, LISTNS);
        ydtBuilder.addLeaf("invalid", LISTNS, "12");
        ydtBuilder.traverseToParent();
        leafErrorMsgValidator(ydtBuilder, "invalid", "12", ERROR[1]);
    }

    /**
     * Tests the negative error scenario when key elements of list node
     * are not unique.
     */
    @Test
    public void negative4Test() throws YdtException {
        YangRequestWorkBench ydtBuilder = getTestYdtBuilder(LISTNS);
        ydtBuilder.addChild(LWC, LISTNS);
        ydtBuilder.addLeaf("invalid", LISTNS, "12");
        ydtBuilder.traverseToParent();
        ydtBuilder.addLeaf("invalid1", LISTNS, "12");
        ydtBuilder.traverseToParent();
        ydtBuilder.traverseToParent();

        ydtBuilder.addChild(LWC, LISTNS);
        ydtBuilder.addLeaf("invalid", LISTNS, "12");
        ydtBuilder.traverseToParent();
        ydtBuilder.addLeaf("invalid1", LISTNS, "12");
        ydtBuilder.traverseToParent();
        ydtBuilder.traverseToParent();

        traversToParentErrorMsgValidator(ydtBuilder, ERROR[2]);
    }

    /**
     * Tests the negative error scenario when all key elements of list node
     * are not supplied.
     */
    @Test
    public void negative5Test() throws YdtException {
        keysValueList.clear();
        keysValueList.add("1");
        keysValueList.add("2");
        keysValueList.add("2");
        YangRequestWorkBench ydtBuilder = getTestYdtBuilder(LISTNS);
        listNodeErrorMsgValidator(ydtBuilder, keysValueList, ERROR[4]);

        keysValueList.clear();
        keysValueList.add("1");
        ydtBuilder = getTestYdtBuilder(LISTNS);
        listNodeErrorMsgValidator(ydtBuilder, keysValueList, ERROR[3]);
    }

    /**
     * Tests the negative error scenario when instances of a list node are
     * created above the allowed limit.
     */
    @Test
    public void negative6Test() throws YdtException {
        keysValueList.clear();
        keysValueList.add("1");
        keysValueList.add("1");
        YangRequestWorkBench ydtBuilder = getTestYdtBuilder(LISTNS);
        ydtBuilder.addMultiInstanceChild(LWC, LISTNS, keysValueList, null);
        ydtBuilder.traverseToParent();

        ydtBuilder.addChild(LWC, LISTNS);
        ydtBuilder.addLeaf("invalid", LISTNS, "12");
        ydtBuilder.traverseToParent();
        ydtBuilder.addLeaf("invalid1", LISTNS, "121");
        ydtBuilder.traverseToParent();
        ydtBuilder.traverseToParent();

        ydtBuilder.addChild(LWC, LISTNS);
        ydtBuilder.addLeaf("invalid", LISTNS, "12");
        ydtBuilder.traverseToParent();
        ydtBuilder.addLeaf("invalid1", LISTNS, "1211");
        ydtBuilder.traverseToParent();
        ydtBuilder.traverseToParent();

        ydtBuilder.addChild(LWC, LISTNS);
        ydtBuilder.addLeaf("invalid", LISTNS, "12");
        ydtBuilder.traverseToParent();
        ydtBuilder.addLeaf("invalid1", LISTNS, "21");
        ydtBuilder.traverseToParent();
        ydtBuilder.traverseToParent();
        traversToParentErrorMsgValidator(ydtBuilder, ERROR[6]);
    }

    /**
     * Tests the negative error scenario when list node is not having all
     * key elements.
     */
    @Test
    public void negative7Test() throws YdtException {
        YangRequestWorkBench ydtBuilder = getTestYdtBuilder(LISTNS);
        ydtBuilder.addChild(LWC, LISTNS);
        traversToParentErrorMsgValidator(ydtBuilder, ERROR[0]);
    }

    /**
     * Tests the negative error scenario when duplicate key entry is created
     * inside leaf-list node.
     */
    @Test
    public void negative8Test() throws YdtException {
        YangRequestWorkBench ydtBuilder = getTestYdtBuilder(LISTNS);
        ydtBuilder.addChild(LWC, LISTNS);
        ydtBuilder.addLeaf("invalid", LISTNS, "12");
        ydtBuilder.traverseToParent();
        ydtBuilder.addLeaf("invalid1", LISTNS, "12");
        ydtBuilder.traverseToParent();
        ydtBuilder.addLeaf(INV, LISTNS, "12");
        ydtBuilder.traverseToParent();
        leafErrorMsgValidator(ydtBuilder, INV, "12", ERROR[7]);
    }

    //TODO negative scenario will be handled later
//    /**
//     * Tests the negative error scenario when string is passed for uint16 type
//     * leaf node.
//     */
//    @Test
//    public void negative9Test() throws YdtException {
//        YangRequestWorkBench ydtBuilder = getTestYdtBuilder(LISTNS);
//        ydtBuilder.addChild(LWC, LISTNS);
//        ydtBuilder.addLeaf("invalid", LISTNS, "12");
//        ydtBuilder.traverseToParent();
//        ydtBuilder.addLeaf("invalid1", LISTNS, "12");
//        ydtBuilder.traverseToParent();
//        leafErrorMsgValidator(ydtBuilder, INV, "string", ERROR[8]);
//    }
//
//    /**
//     * Tests the negative error scenario when duplicate key entry created
//     * inside a leaf-list node.
//     */
//    @Test
//    public void negative10Test() throws YdtException {
//        valueSet.clear();
//        valueSet.add("1");
//        valueSet.add("2");
//        valueSet.add("12");
//        YangRequestWorkBench ydtBuilder = getTestYdtBuilder(LISTNS);
//        ydtBuilder.addChild(LWC, LISTNS);
//        ydtBuilder.addLeaf("invalid", LISTNS, "12");
//        ydtBuilder.traverseToParent();
//        ydtBuilder.addLeaf("invalid1", LISTNS, "12");
//        ydtBuilder.traverseToParent();
//        ydtBuilder.addLeaf(INV, LISTNS, "12");
//        ydtBuilder.traverseToParent();
//        thrown.expect(IllegalArgumentException.class);
//        thrown.expectMessage(ERROR[7]);
//        ydtBuilder.addLeaf(INV, LISTNS, valueSet);
//    }

//    /**
//     * Tests the negative error scenario when string is passed for uint16 type
//     * key entry inside a leaf-list node.
//     */
//    @Test
//    public void negative11Test() throws YdtException {
//        valueSet.clear();
//        valueSet.add("string");
//        YangRequestWorkBench ydtBuilder = getTestYdtBuilder(LISTNS);
//        ydtBuilder.addChild(LWC, LISTNS);
//        ydtBuilder.addLeaf("invalid", LISTNS, "12");
//        ydtBuilder.traverseToParent();
//        ydtBuilder.addLeaf("invalid1", LISTNS, "12");
//        ydtBuilder.traverseToParent();
//        ydtBuilder.addLeaf(INV, LISTNS, "12");
//        ydtBuilder.traverseToParent();
//        thrown.expect(DataTypeException.class);
//        thrown.expectMessage(ERROR[8]);
//        ydtBuilder.addLeaf(INV, LISTNS, valueSet);
//    }

    /**
     * Tests the negative error scenario when list node addition requested
     * with single instance request type.
     */
    @Test
    public void negative12Test() {
        YangRequestWorkBench ydtBuilder = getTestYdtBuilder(LISTNS);
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage(ERROR[9]);
        ydtBuilder.addChild(LWC, LISTNS, SINGLE_INSTANCE_NODE, MERGE);
    }

    /**
     * Tests the negative error scenario when application with requested
     * name is already part of tree.
     */
    @Test
    public void negative13Test() {
        YangRequestWorkBench ydtBuilder = getTestYdtBuilder(LISTNS);
        ydtBuilder.traverseToParent();
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage(ERROR[10]);
        ydtBuilder.addChild("rootlist", LISTNS, MERGE);
    }

    /**
     * Validate the error message obtained by adding multi instance node in
     * current context against the given error string.
     *
     * @param bldr  ydt builder
     * @param list  list of key values
     * @param error error string
     */
    private void listNodeErrorMsgValidator(YangRequestWorkBench bldr,
                                           List<String> list, String error) {
        /*
         * This try catch is explicitly written to use as utility in other
         * test cases.
         */
        boolean isExpOccurred = false;
        try {
            bldr.addMultiInstanceChild(LWC, LISTNS, list, null);
        } catch (IllegalArgumentException e) {
            isExpOccurred = true;
            assertEquals(e.getMessage(), error);
        }
        assertEquals(E_LIST + LWC, isExpOccurred, true);
    }

    /**
     * Validate the error message obtained by traversing back to parent of
     * current context against the given error string.
     *
     * @param ydtBuilder ydt builder
     * @param error      error string
     */
    private void traversToParentErrorMsgValidator(
            YangRequestWorkBench ydtBuilder, String error) {
        /*
         * This try catch is explicitly written to use as utility in other
         * test cases.
         */
        boolean isExpOccurred = false;
        try {
            ydtBuilder.traverseToParent();
        } catch (IllegalStateException e) {
            isExpOccurred = true;
            assertEquals(e.getMessage(), error);
        }
        assertEquals(E_TOPARENT, isExpOccurred, true);
    }

    /**
     * Validate the error message obtained by adding leaf node in
     * current context against the given error string.
     *
     * @param bldr  ydt builder
     * @param name  name of the leaf
     * @param val   leaf value
     * @param error error string
     */
    private void leafErrorMsgValidator(
            YangRequestWorkBench bldr, String name, String val, String error) {
        /*
         * This try catch is explicitly written to use as utility in other
         * test cases.
         */
        boolean isExpOccurred = false;
        try {
            bldr.addLeaf(name, LISTNS, val);
        } catch (IllegalArgumentException e) {
            isExpOccurred = true;
            assertEquals(e.getMessage(), error);
        }
        assertEquals(E_LEAF + name, isExpOccurred, true);
    }
}

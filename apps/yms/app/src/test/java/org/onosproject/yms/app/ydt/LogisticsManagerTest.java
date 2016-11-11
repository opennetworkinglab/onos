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

import org.junit.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.onosproject.yms.app.ydt.YdtTestUtils.logisticsManagerYdt;
import static org.onosproject.yms.app.ydt.YdtTestUtils.validateLeafContents;
import static org.onosproject.yms.app.ydt.YdtTestUtils.validateLeafListContents;
import static org.onosproject.yms.app.ydt.YdtTestUtils.validateNodeContents;
import static org.onosproject.yms.app.ydt.YdtTestUtils.walkINTree;
import static org.onosproject.yms.ydt.YdtContextOperationType.MERGE;

public class LogisticsManagerTest {

    // Logger list is used for walker testing.
    private final List<String> logger = new ArrayList<>();

    private Set<String> valueSet = new HashSet();

    private static final String[] EXPECTED = {
            "Entry Node is logisticsmanager.",
            "Entry Node is customssupervisor.",
            "Entry Node is supervisor.",
            "Exit Node is supervisor.",
            "Exit Node is customssupervisor.",
            "Entry Node is merchandisersupervisor.",
            "Entry Node is supervisor.",
            "Exit Node is supervisor.",
            "Exit Node is merchandisersupervisor.",
            "Entry Node is materialsupervisor.",
            "Entry Node is supervisor.",
            "Entry Node is name.",
            "Exit Node is name.",
            "Entry Node is departmentId.",
            "Exit Node is departmentId.",
            "Exit Node is supervisor.",
            "Entry Node is supervisor.",
            "Entry Node is name.",
            "Exit Node is name.",
            "Entry Node is departmentId.",
            "Exit Node is departmentId.",
            "Exit Node is supervisor.",
            "Exit Node is materialsupervisor.",
            "Entry Node is purchasingsupervisor.",
            "Entry Node is supervisor.",
            "Entry Node is purchasing-specialist.",
            "Exit Node is purchasing-specialist.",
            "Entry Node is support.",
            "Exit Node is support.",
            "Exit Node is supervisor.",
            "Exit Node is purchasingsupervisor.",
            "Entry Node is warehousesupervisor.",
            "Entry Node is supervisor.",
            "Exit Node is supervisor.",
            "Exit Node is warehousesupervisor.",
            "Entry Node is tradingsupervisor.",
            "Entry Node is supervisor.",
            "Exit Node is supervisor.",
            "Exit Node is tradingsupervisor.",
            "Entry Node is employeeid.",
            "Entry Node is employeeid.",
            "Exit Node is employeeid.",
            "Exit Node is employeeid.",
            "Exit Node is logisticsmanager."
    };

    /**
     * Creates and validates logistics manager ydt.
     */
    @Test
    public void logisticsManagerTest() {
        YangRequestWorkBench ydtBuilder = logisticsManagerYdt();
        validateTree(ydtBuilder);
        // walker test
        walkINTree(ydtBuilder, EXPECTED);
    }

    /**
     * Validates the given built ydt.
     */
    private void validateTree(YangRequestWorkBench ydtBuilder) {
        valueSet.add("1");
        valueSet.add("2");
        valueSet.add("3");
        valueSet.add("4");
        valueSet.add("5");
        // assign root node to ydtNode for validating purpose.
        YdtNode ydtNode = (YdtNode) ydtBuilder.getRootNode();
        // Logical root node does not have operation type
        validateNodeContents(ydtNode, "logisticsmanager", null);

        ydtNode = ydtNode.getFirstChild();
        // Logical root node does not have operation type
        validateNodeContents(ydtNode, "customssupervisor", MERGE);
        ydtNode = ydtNode.getFirstChild();
        validateLeafContents(ydtNode, "supervisor", "abc");

        ydtNode = ydtNode.getParent();

        ydtNode = ydtNode.getNextSibling();
        validateNodeContents(ydtNode, "merchandisersupervisor", MERGE);
        ydtNode = ydtNode.getFirstChild();
        validateLeafContents(ydtNode, "supervisor", "abc");

        ydtNode = ydtNode.getParent();

        ydtNode = ydtNode.getNextSibling();
        validateNodeContents(ydtNode, "materialsupervisor", MERGE);
        ydtNode = ydtNode.getFirstChild();
        validateNodeContents(ydtNode, "supervisor", MERGE);

        ydtNode = ydtNode.getFirstChild();
        validateLeafContents(ydtNode, "name", "abc");
        ydtNode = ydtNode.getNextSibling();
        validateLeafContents(ydtNode, "departmentId", "xyz");

        ydtNode = ydtNode.getParent();

        ydtNode = ydtNode.getNextSibling();
        validateNodeContents(ydtNode, "supervisor", MERGE);

        ydtNode = ydtNode.getFirstChild();
        validateLeafContents(ydtNode, "name", "ab");
        ydtNode = ydtNode.getNextSibling();
        validateLeafContents(ydtNode, "departmentId", "xy");

        ydtNode = ydtNode.getParent().getParent();
        ydtNode = ydtNode.getNextSibling();
        validateNodeContents(ydtNode, "purchasingsupervisor", MERGE);
        ydtNode = ydtNode.getFirstChild();
        validateNodeContents(ydtNode, "supervisor", MERGE);

        ydtNode = ydtNode.getFirstChild();
        validateLeafContents(ydtNode, "purchasing-specialist", "abc");
        ydtNode = ydtNode.getNextSibling();
        validateLeafContents(ydtNode, "support", "xyz");

        ydtNode = ydtNode.getParent().getParent();

        ydtNode = ydtNode.getNextSibling();
        validateNodeContents(ydtNode, "warehousesupervisor", MERGE);
        ydtNode = ydtNode.getFirstChild();
        validateLeafListContents(ydtNode, "supervisor", valueSet);

        ydtNode = ydtNode.getParent();

        ydtNode = ydtNode.getNextSibling();
        validateNodeContents(ydtNode, "tradingsupervisor", MERGE);
        ydtNode = ydtNode.getFirstChild();
        validateLeafContents(ydtNode, "supervisor", "abc");

        ydtNode = ydtNode.getParent();

        ydtNode = ydtNode.getNextSibling();
        validateNodeContents(ydtNode, "employeeid", MERGE);
        ydtNode = ydtNode.getFirstChild();
        validateLeafListContents(ydtNode, "employeeid", valueSet);
    }
}

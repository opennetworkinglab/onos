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

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.onosproject.yms.app.ydt.YdtTestUtils.validateLeafContents;
import static org.onosproject.yms.app.ydt.YdtTestUtils.validateLeafListContents;
import static org.onosproject.yms.app.ydt.YdtTestUtils.validateNodeContents;
import static org.onosproject.yms.ydt.YdtContextOperationType.MERGE;

public class EmptyLeafListTest {

    // Logger list is used for walker testing.
    private final List<String> logger = new ArrayList<>();

    private static final String[] EXPECTED = {
            "Entry Node is empty.",
            "Entry Node is EmptyLeafList.",
            "Entry Node is l1.",
            "Exit Node is l1.",
            "Entry Node is l2.",
            "Exit Node is l2.",
            "Entry Node is l3.",
            "Exit Node is l3.",
            "Entry Node is list1.",
            "Exit Node is list1.",
            "Entry Node is list2.",
            "Exit Node is list2.",
            "Entry Node is list3.",
            "Exit Node is list3.",
            "Exit Node is EmptyLeafList.",
            "Exit Node is empty."
    };

    /**
     * Creates and validates empty leaf list ydt.
     */
    @Test
    public void emptyListTest() throws IOException {

        //TODO need to be handled later
//        YangRequestWorkBench ydtBuilder = emptyLeafListYdt();
//        validateTree(ydtBuilder);
//        walkINTree(ydtBuilder, EXPECTED);
    }

    /**
     * Validates the given built ydt.
     */
    private void validateTree(YangRequestWorkBench ydtBuilder) {

        Set<String> valueSet = new HashSet();

        // Assign root node to ydtNode for validating purpose.
        YdtNode ydtNode = (YdtNode) ydtBuilder.getRootNode();
        // Logical root node does not have operation type
        validateNodeContents(ydtNode, "empty", null);
        ydtNode = ydtNode.getFirstChild();
        validateNodeContents(ydtNode, "EmptyLeafList", MERGE);
        ydtNode = ydtNode.getFirstChild();
        validateLeafContents(ydtNode, "l1", null);
        ydtNode = ydtNode.getNextSibling();
        validateLeafContents(ydtNode, "l2", null);
        ydtNode = ydtNode.getNextSibling();
        validateLeafContents(ydtNode, "l3", null);
        ydtNode = ydtNode.getNextSibling();

        validateLeafListContents(ydtNode, "list1", valueSet);
        ydtNode = ydtNode.getNextSibling();
        validateLeafListContents(ydtNode, "list2", valueSet);
        ydtNode = ydtNode.getNextSibling();
        validateLeafListContents(ydtNode, "list3", valueSet);
    }
}

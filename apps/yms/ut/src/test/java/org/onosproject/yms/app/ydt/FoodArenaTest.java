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
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.onosproject.yms.app.ydt.YdtAppNodeOperationType.DELETE_ONLY;
import static org.onosproject.yms.app.ydt.YdtTestUtils.foodArenaYdt;
import static org.onosproject.yms.app.ydt.YdtTestUtils.getYdtBuilder;
import static org.onosproject.yms.app.ydt.YdtTestUtils.validateLeafContents;
import static org.onosproject.yms.app.ydt.YdtTestUtils.validateNodeContents;
import static org.onosproject.yms.app.ydt.YdtTestUtils.walkINTree;
import static org.onosproject.yms.ydt.YdtContextOperationType.DELETE;
import static org.onosproject.yms.ydt.YdtContextOperationType.MERGE;
import static org.onosproject.yms.ydt.YdtContextOperationType.NONE;

public class FoodArenaTest {

    // Logger list is used for walker testing.
    private final List<String> logger = new ArrayList<>();

    private static final String[] EXPECTED = {
            "Entry Node is foodarena.",
            "Entry Node is food.",
            "Entry Node is food.",
            "Entry Node is chocolate.",
            "Exit Node is chocolate.",
            "Exit Node is food.",
            "Exit Node is food.",
            "Exit Node is foodarena."
    };

    /**
     * Creates and validates food arena ydt.
     */
    @Test
    public void foodArenaTest() throws IOException {

        YangRequestWorkBench ydtBuilder = foodArenaYdt();
        validateTree(ydtBuilder);
        walkINTree(ydtBuilder, EXPECTED);
    }

    /**
     * Creates and validates food arena ydt.
     */
    @Test
    public void foodArenaDeleteOperationTest() throws IOException {

        YangRequestWorkBench ydtBuilder;
        ydtBuilder = getYdtBuilder("foodarena", "food", "ydt.food", NONE);
        ydtBuilder.addChild("food", "ydt.food", DELETE);
        YdtAppContext appRootNode = ydtBuilder.getAppRootNode();
        assertEquals(DELETE_ONLY, appRootNode.getFirstChild().getOperationType());
    }

    /**
     * Validates the given built ydt.
     */
    private void validateTree(YangRequestWorkBench ydtBuilder) {
        // Assign root node to ydtNode for validating purpose.
        YdtNode ydtNode = (YdtNode) ydtBuilder.getRootNode();
        // Logical root node does not have operation type
        validateNodeContents(ydtNode, "foodarena", null);
        ydtNode = ydtNode.getFirstChild();
        validateNodeContents(ydtNode, "food", MERGE);
        ydtNode = ydtNode.getFirstChild();
        validateNodeContents(ydtNode, "food", MERGE);
        ydtNode = ydtNode.getFirstChild();
        validateLeafContents(ydtNode, "chocolate", "dark");
    }
}

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

import static org.onosproject.yms.app.ydt.YdtTestUtils.bitYdt;
import static org.onosproject.yms.app.ydt.YdtTestUtils.validateLeafContents;
import static org.onosproject.yms.app.ydt.YdtTestUtils.validateNodeContents;
import static org.onosproject.yms.ydt.YdtContextOperationType.MERGE;

public class YdtBitTest {

    /*
        BINARY

        Positive scenario
        input with position 0
        input with position 1
        input with position 2
    */

    /**
     * Creates and validates bit ydt covering different positive scenario.
     */
    @Test
    public void positiveTest() {
        YangRequestWorkBench ydtBuilder = bitYdt();
        validateTree(ydtBuilder);
    }

    /**
     * Validates the given built ydt.
     */
    private void validateTree(YangRequestWorkBench ydtBuilder) {

        // assign root node to ydtNode for validating purpose.
        YdtNode ydtNode = (YdtNode) ydtBuilder.getRootNode();
        // Logical root node does not have operation type
        validateNodeContents(ydtNode, "builtInType", null);
        ydtNode = ydtNode.getFirstChild();
        validateNodeContents(ydtNode, "bit", MERGE);
        ydtNode = ydtNode.getFirstChild();
        validateNodeContents(ydtNode, "bitList", MERGE);
        ydtNode = ydtNode.getFirstChild();
        validateLeafContents(ydtNode, "bit", "disable-nagle");
        ydtNode = ydtNode.getParent();
        ydtNode = ydtNode.getNextSibling();
        validateNodeContents(ydtNode, "bitList", MERGE);
        ydtNode = ydtNode.getFirstChild();
        validateLeafContents(ydtNode, "bit", "auto-sense-speed");
        ydtNode = ydtNode.getParent();
        ydtNode = ydtNode.getNextSibling();
        validateNodeContents(ydtNode, "bitList", MERGE);
        ydtNode = ydtNode.getFirstChild();
        validateLeafContents(ydtNode, "bit", "ten-Mb-only");
    }

    //TODO negative scenario will be handled later
//    /*
//        Negative scenario
//
//        input with position 0
//        input with position 1
//        input with position 2
//    */
//
//    /**
//     * Tests all the negative scenario's for bit data type.
//     */
//    @Test
//    public void negativeTest() {
//        validateErrMsg("bit", BITNS, "0", BIT, "bitList");
//        validateErrMsg("bit", BITNS, "default", BIT, "bitList");
//        validateErrMsg("bit", BITNS, "1", BIT, "bitList");
//        validateErrMsg("bit", BITNS, "", BIT, "bitList");
//    }
}

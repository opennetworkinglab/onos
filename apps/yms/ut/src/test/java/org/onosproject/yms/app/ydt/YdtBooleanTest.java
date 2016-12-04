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

import static org.onosproject.yms.app.ydt.YdtTestUtils.booleanYdt;
import static org.onosproject.yms.app.ydt.YdtTestUtils.validateLeafContents;
import static org.onosproject.yms.app.ydt.YdtTestUtils.validateNodeContents;
import static org.onosproject.yms.ydt.YdtContextOperationType.MERGE;

public class YdtBooleanTest {

    /*
        BOOLEAN
        Positive scenario
        input with in "booleanList" and false
    */

    /**
     * Creates and validates boolean ydt covering different positive scenario.
     */
    @Test
    public void positiveTest() {
        YangRequestWorkBench ydtBuilder = booleanYdt();
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
        validateNodeContents(ydtNode, "bool", MERGE);
        ydtNode = ydtNode.getFirstChild();
        validateNodeContents(ydtNode, "booleanList", MERGE);
        ydtNode = ydtNode.getFirstChild();
        validateLeafContents(ydtNode, "boolean", "true");
        ydtNode = ydtNode.getParent();
        ydtNode = ydtNode.getNextSibling();
        validateNodeContents(ydtNode, "booleanList", MERGE);
        ydtNode = ydtNode.getFirstChild();
        validateLeafContents(ydtNode, "boolean", "false");
    }

    //TODO negative scenario will be handled later
//    /*
//        Negative scenario
//
//        input with in non zero value in case of "booleanList"
//        input with zero value in case of false
//        input with empty value in case of false
//    */
//
//    /**
//     * Tests all the negative scenario's for boolean data type.
//     */
//    @Test
//    public void negativeTest() {
//        validateErrMsg("boolean", BOOLNS, "10", BOOL, "booleanList");
//        validateErrMsg("boolean", BOOLNS, "0", BOOL, "booleanList");
//        validateErrMsg("boolean", BOOLNS, "", BOOL, "booleanList");
//        validateErrMsg("boolean", BOOLNS, "-1", BOOL, "booleanList");
//        validateErrMsg("boolean", BOOLNS, "tru", BOOL, "booleanList");
//        validateErrMsg("boolean", BOOLNS, "boolean", BOOL, "booleanList");
//    }
}

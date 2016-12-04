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
import org.onosproject.yms.app.ydt.exceptions.YdtException;

import static org.onosproject.yms.app.ydt.YdtTestConstants.TYPE;
import static org.onosproject.yms.app.ydt.YdtTestUtils.emptyTypeYdt;
import static org.onosproject.yms.app.ydt.YdtTestUtils.validateLeafContents;
import static org.onosproject.yms.app.ydt.YdtTestUtils.validateNodeContents;
import static org.onosproject.yms.ydt.YdtContextOperationType.MERGE;

public class YdtEmptyTest {

    /*
        EMPTY
        Positive scenario
        input with in empty.
    */

    /**
     * Creates and validates empty ydt covering different positive scenario.
     */
    @Test
    public void positiveTest() throws YdtException {
        YangRequestWorkBench ydtBuilder = emptyTypeYdt();
        validateTree(ydtBuilder);
    }

    /**
     * Validates the given built ydt.
     */
    private void validateTree(YangRequestWorkBench ydtBuilder) {

        // assign root node to ydtNode for validating purpose.
        YdtNode ydtNode = (YdtNode) ydtBuilder.getRootNode();
        // Logical root node does not have operation type
        validateNodeContents(ydtNode, TYPE, null);

        ydtNode = ydtNode.getFirstChild();
        validateNodeContents(ydtNode, "emptydata", MERGE);
        ydtNode = ydtNode.getFirstChild();
        validateNodeContents(ydtNode, "emptyList", MERGE);
        ydtNode = ydtNode.getFirstChild();
        validateLeafContents(ydtNode, "empty", "");
    }

    //TODO negative scenario will be handled later
//    /*
//        Negative scenario
//
//        input with " "
//        input with "tab"
//        input with """"
//    */
//
//    /**
//     * Tests all the negative scenario's for empty data type.
//     */
//    @Test
//    public void negativeTest() throws YdtException {
//        validateErrMsg("empty", EMPTYNS, " ", EMPTY, "emptyList");
//        validateErrMsg("empty", EMPTYNS, "    ", EMPTY, "emptyList");
//        validateErrMsg("empty", EMPTYNS, " ", EMPTY, "emptyList");
//    }
}

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

import static org.onosproject.yms.app.ydt.YdtAppNodeOperationType.OTHER_EDIT;
import static org.onosproject.yms.app.ydt.YdtTestConstants.AUGNS;
import static org.onosproject.yms.app.ydt.YdtTestConstants.AUGSE;
import static org.onosproject.yms.app.ydt.YdtTestUtils.augmentSequenceYdt;
import static org.onosproject.yms.app.ydt.YdtTestUtils.validateAppLogicalNodeContents;
import static org.onosproject.yms.app.ydt.YdtTestUtils.validateAppModuleNodeContents;
import static org.onosproject.yms.app.ydt.YdtTestUtils.validateAppNodeContents;
import static org.onosproject.yms.app.ydt.YdtTestUtils.validateLeafContents;
import static org.onosproject.yms.app.ydt.YdtTestUtils.validateNodeContents;
import static org.onosproject.yms.app.ydt.YdtTestUtils.walkINTree;
import static org.onosproject.yms.ydt.YdtContextOperationType.MERGE;

public class AugmentSequenceTest {

    private static final String[] EXPECTED = {
            "Entry Node is augment.",
            "Entry Node is augmentSequence.",
            "Entry Node is l1.",
            "Entry Node is leaf1.",
            "Exit Node is leaf1.",

            "Entry Node is c1.",
            "Entry Node is leaf2.",
            "Exit Node is leaf2.",
            "Exit Node is c1.",

            "Entry Node is c2.",
            "Entry Node is leaf2.",
            "Exit Node is leaf2.",
            "Exit Node is c2.",

            "Exit Node is l1.",
            "Exit Node is augmentSequence.",
            "Exit Node is augment.",
    };

    /**
     * Creates and validates sequence of augment in ydt.
     */
    @Test
    public void augmentTest() {
        YangRequestWorkBench ydtBuilder = augmentSequenceYdt();
        validateTree(ydtBuilder);
        validateAppTree(ydtBuilder);
        walkINTree(ydtBuilder, EXPECTED);
    }

    /**
     * Validates the given built ydt.
     */
    private void validateTree(YangRequestWorkBench ydtBuilder) {

        // Assign root node to ydtNode for validating purpose.
        YdtNode ydtNode = (YdtNode) ydtBuilder.getRootNode();
        // Logical root node does not have operation type
        validateNodeContents(ydtNode, "augment", null);
        ydtNode = ydtNode.getFirstChild();
        validateNodeContents(ydtNode, "augmentSequence", MERGE);
        ydtNode = ydtNode.getFirstChild();
        validateNodeContents(ydtNode, "l1", MERGE);
        ydtNode = ydtNode.getFirstChild();

        validateLeafContents(ydtNode, "leaf1", "1");
        ydtNode = ydtNode.getNextSibling();

        //Augmenting leaf2
        validateNodeContents(ydtNode, "c1", MERGE);
        ydtNode = ydtNode.getFirstChild();
        validateLeafContents(ydtNode, "leaf2", "2");
        ydtNode = ydtNode.getParent();
        ydtNode = ydtNode.getNextSibling();

        //Augmenting leaf3
        validateNodeContents(ydtNode, "c2", MERGE);
        ydtNode = ydtNode.getFirstChild();
        validateLeafContents(ydtNode, "leaf2", "3");
    }

    /**
     * Validates the given built ydt application tree.
     */
    private void validateAppTree(YangRequestWorkBench ydtBuilder) {

        // Assign root node to ydtNode for validating purpose.
        YdtAppContext ydtAppContext = ydtBuilder.getAppRootNode();
        // Logical root node does not have operation type
        validateAppLogicalNodeContents(ydtAppContext);
        ydtAppContext = ydtAppContext.getFirstChild();
        validateAppModuleNodeContents(ydtAppContext, "augmentSequence",
                                      OTHER_EDIT);
        ydtAppContext = ydtAppContext.getFirstChild();

        //Inside list checking the first augmented leaf
        validateAppNodeContents(ydtAppContext, AUGSE, AUGNS, OTHER_EDIT);
    }
}

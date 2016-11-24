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

import static org.onosproject.yms.app.ydt.YdtTestUtils.ietfNetwork1Ydt;
import static org.onosproject.yms.app.ydt.YdtTestUtils.validateLeafContents;
import static org.onosproject.yms.app.ydt.YdtTestUtils.validateNodeContents;
import static org.onosproject.yms.app.ydt.YdtTestUtils.walkINTree;
import static org.onosproject.yms.ydt.YdtContextOperationType.MERGE;

public class IetfNetworkTest {

    private static final String[] EXPECTED = {
            "Entry Node is yms-ietf-network.",
            "Entry Node is yms-ietf-network.",
            "Entry Node is networks.",
            "Entry Node is network.",
            "Entry Node is network-id.",
            "Exit Node is network-id.",
            "Entry Node is supporting-network.",
            "Entry Node is network-ref.",
            "Exit Node is network-ref.",
            "Exit Node is supporting-network.",
            "Entry Node is node.",
            "Entry Node is node-id.",
            "Exit Node is node-id.",
            "Entry Node is supporting-node.",
            "Entry Node is network-ref.",
            "Exit Node is network-ref.",
            "Entry Node is node-ref.",
            "Exit Node is node-ref.",
            "Exit Node is supporting-node.",
            "Exit Node is node.",
            "Exit Node is network.",
            "Exit Node is networks.",
            "Entry Node is networks-state.",
            "Entry Node is network.",
            "Entry Node is network-ref.",
            "Exit Node is network-ref.",
            "Entry Node is server-provided.",
            "Exit Node is server-provided.",
            "Exit Node is network.",
            "Exit Node is networks-state.",
            "Exit Node is yms-ietf-network.",
            "Exit Node is yms-ietf-network."
    };

    /**
     * Creates and validates ietf network ydt.
     */
    @Test
    public void ietfNetwork1Test() {
        YangRequestWorkBench ydtBuilder = ietfNetwork1Ydt();
        validateTree(ydtBuilder);
        walkINTree(ydtBuilder, EXPECTED);
    }

    /**
     * Validates the given built ydt.
     */
    private void validateTree(YangRequestWorkBench ydtBuilder) {

        // Assign root node to ydtNode for validating purpose.
        YdtNode ydtNode = (YdtNode) ydtBuilder.getRootNode();
        // Logical root node does not have operation type
        validateNodeContents(ydtNode, "yms-ietf-network", null);
        ydtNode = ydtNode.getFirstChild();
        validateNodeContents(ydtNode, "yms-ietf-network", MERGE);
        ydtNode = ydtNode.getFirstChild();
        validateNodeContents(ydtNode, "networks", MERGE);
        ydtNode = ydtNode.getFirstChild();
        validateNodeContents(ydtNode, "network", MERGE);
        ydtNode = ydtNode.getFirstChild();
        validateLeafContents(ydtNode, "network-id", "network1");

        ydtNode = ydtNode.getNextSibling();
        validateNodeContents(ydtNode, "supporting-network", MERGE);
        ydtNode = ydtNode.getFirstChild();
        validateLeafContents(ydtNode, "network-ref", "network2");
        ydtNode = ydtNode.getParent();

        ydtNode = ydtNode.getNextSibling();
        validateNodeContents(ydtNode, "node", MERGE);
        ydtNode = ydtNode.getFirstChild();
        validateLeafContents(ydtNode, "node-id", "node1");
        ydtNode = ydtNode.getNextSibling();
        validateNodeContents(ydtNode, "supporting-node", MERGE);
        ydtNode = ydtNode.getFirstChild();
        validateLeafContents(ydtNode, "network-ref", "network3");
        ydtNode = ydtNode.getNextSibling();
        validateLeafContents(ydtNode, "node-ref", "network4");

        ydtNode = ydtNode.getParent().getParent().getParent().getParent();

        ydtNode = ydtNode.getNextSibling();
        validateNodeContents(ydtNode, "networks-state", MERGE);
        ydtNode = ydtNode.getFirstChild();
        validateNodeContents(ydtNode, "network", MERGE);
        ydtNode = ydtNode.getFirstChild();
        validateLeafContents(ydtNode, "network-ref", "network5");
        ydtNode = ydtNode.getNextSibling();
        validateLeafContents(ydtNode, "server-provided", "true");
    }
}

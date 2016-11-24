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
import static org.onosproject.yms.app.ydt.YdtTestConstants.AUG1;
import static org.onosproject.yms.app.ydt.YdtTestConstants.IETF;
import static org.onosproject.yms.app.ydt.YdtTestConstants.SLINK;
import static org.onosproject.yms.app.ydt.YdtTestConstants.STP;
import static org.onosproject.yms.app.ydt.YdtTestConstants.TOPONS;
import static org.onosproject.yms.app.ydt.YdtTestUtils.ietfNetworkTopologyYdt;
import static org.onosproject.yms.app.ydt.YdtTestUtils.validateAppLogicalNodeContents;
import static org.onosproject.yms.app.ydt.YdtTestUtils.validateAppModuleNodeContents;
import static org.onosproject.yms.app.ydt.YdtTestUtils.validateAppNodeContents;
import static org.onosproject.yms.app.ydt.YdtTestUtils.validateLeafContents;
import static org.onosproject.yms.app.ydt.YdtTestUtils.validateNodeContents;
import static org.onosproject.yms.app.ydt.YdtTestUtils.walkINTree;
import static org.onosproject.yms.ydt.YdtContextOperationType.MERGE;

public class IetfTopologyTest {

    private static final String[] EXPECTED = {
            "Entry Node is yms-ietf-network.",
            "Entry Node is yms-ietf-network.",
            "Entry Node is networks.",
            "Entry Node is network.",
            "Entry Node is network-id.",
            "Exit Node is network-id.",
            "Entry Node is link.",
            "Entry Node is link-id.",
            "Exit Node is link-id.",
            "Entry Node is source.",
            "Entry Node is source-node.",
            "Exit Node is source-node.",
            "Entry Node is source-tp.",
            "Exit Node is source-tp.",
            "Exit Node is source.",

            "Entry Node is destination.",
            "Entry Node is dest-node.",
            "Exit Node is dest-node.",
            "Entry Node is dest-tp.",
            "Exit Node is dest-tp.",
            "Exit Node is destination.",

            "Entry Node is supporting-link.",
            "Entry Node is network-ref.",
            "Exit Node is network-ref.",
            "Entry Node is link-ref.",
            "Exit Node is link-ref.",
            "Exit Node is supporting-link.",

            "Entry Node is supporting-link.",
            "Entry Node is network-ref.",
            "Exit Node is network-ref.",
            "Entry Node is link-ref.",
            "Exit Node is link-ref.",
            "Exit Node is supporting-link.",
            "Exit Node is link.",

            "Entry Node is supporting-network.",
            "Entry Node is network-ref.",
            "Exit Node is network-ref.",
            "Exit Node is supporting-network.",
            "Entry Node is node.",
            "Entry Node is node-id.",
            "Exit Node is node-id.",
            "Entry Node is t-point.",
            "Entry Node is tp-id.",
            "Exit Node is tp-id.",

            "Entry Node is supporting-termination-point.",
            "Entry Node is network-ref.",
            "Exit Node is network-ref.",
            "Entry Node is node-ref.",
            "Exit Node is node-ref.",
            "Entry Node is tp-ref.",
            "Exit Node is tp-ref.",

            "Exit Node is supporting-termination-point.",
            "Exit Node is t-point.",
            "Entry Node is supporting-node.",
            "Entry Node is network-ref.",
            "Exit Node is network-ref.",
            "Entry Node is node-ref.",
            "Exit Node is node-ref.",
            "Exit Node is supporting-node.",

            "Exit Node is node.",
            // last augmented sibling in network
            "Entry Node is link-id.",
            "Exit Node is link-id.",

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
        YangRequestWorkBench ydtBuilder = ietfNetworkTopologyYdt();
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

        // Validating augmented child
        validateNodeContents(ydtNode, "link", MERGE);
        ydtNode = ydtNode.getFirstChild();
        validateLeafContents(ydtNode, "link-id", "id1");
        ydtNode = ydtNode.getNextSibling();

        // Inside source node
        validateNodeContents(ydtNode, "source", MERGE);
        ydtNode = ydtNode.getFirstChild();
        validateLeafContents(ydtNode, "source-node", "source1");
        ydtNode = ydtNode.getNextSibling();
        validateLeafContents(ydtNode, "source-tp", "source2");
        ydtNode = ydtNode.getParent();
        ydtNode = ydtNode.getNextSibling();

        //Inside destination node
        validateNodeContents(ydtNode, "destination", MERGE);
        ydtNode = ydtNode.getFirstChild();
        validateLeafContents(ydtNode, "dest-node", "dest1");
        ydtNode = ydtNode.getNextSibling();
        validateLeafContents(ydtNode, "dest-tp", "dest2");
        ydtNode = ydtNode.getParent();
        ydtNode = ydtNode.getNextSibling();

        //Inside supporting links
        validateNodeContents(ydtNode, SLINK, MERGE);
        ydtNode = ydtNode.getFirstChild();
        validateLeafContents(ydtNode, "network-ref", "network1");
        ydtNode = ydtNode.getNextSibling();
        validateLeafContents(ydtNode, "link-ref", "id2");
        ydtNode = ydtNode.getParent();
        ydtNode = ydtNode.getNextSibling();

        //Inside another supporting links instance
        validateNodeContents(ydtNode, SLINK, MERGE);
        ydtNode = ydtNode.getFirstChild();
        validateLeafContents(ydtNode, "network-ref", "network2");
        ydtNode = ydtNode.getNextSibling();
        validateLeafContents(ydtNode, "link-ref", "id3");
        ydtNode = ydtNode.getParent();
        ydtNode = ydtNode.getParent();

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

        //Inside termination-point
        validateNodeContents(ydtNode, "t-point", MERGE);
        ydtNode = ydtNode.getFirstChild();
        validateLeafContents(ydtNode, "tp-id", "tp_id1");
        ydtNode = ydtNode.getNextSibling();

        //Inside supporting-termination-point
        validateNodeContents(ydtNode, STP, MERGE);
        ydtNode = ydtNode.getFirstChild();
        validateLeafContents(ydtNode, "network-ref", "network-ref");
        ydtNode = ydtNode.getNextSibling();
        validateLeafContents(ydtNode, "node-ref", "node-ref");
        ydtNode = ydtNode.getNextSibling();
        validateLeafContents(ydtNode, "tp-ref", "tp-ref");
        ydtNode = ydtNode.getParent();
        ydtNode = ydtNode.getParent();

        ydtNode = ydtNode.getNextSibling();
        validateNodeContents(ydtNode, "supporting-node", MERGE);
        ydtNode = ydtNode.getFirstChild();
        validateLeafContents(ydtNode, "network-ref", "network3");
        ydtNode = ydtNode.getNextSibling();
        validateLeafContents(ydtNode, "node-ref", "network4");

        ydtNode = ydtNode.getParent().getParent();

        ydtNode = ydtNode.getNextSibling();
        validateLeafContents(ydtNode, "link-id", "id1");
        ydtNode = ydtNode.getParent().getParent();

        ydtNode = ydtNode.getNextSibling();
        validateNodeContents(ydtNode, "networks-state", MERGE);
        ydtNode = ydtNode.getFirstChild();
        validateNodeContents(ydtNode, "network", MERGE);
        ydtNode = ydtNode.getFirstChild();
        validateLeafContents(ydtNode, "network-ref", "network5");
        ydtNode = ydtNode.getNextSibling();
        validateLeafContents(ydtNode, "server-provided", "true");
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
        validateAppModuleNodeContents(ydtAppContext, IETF, OTHER_EDIT);
        ydtAppContext = ydtAppContext.getFirstChild();

        //Inside link node
        validateAppNodeContents(ydtAppContext, AUG1, TOPONS, OTHER_EDIT);
    }
}

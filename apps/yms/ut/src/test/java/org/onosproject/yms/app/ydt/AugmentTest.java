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

import java.util.HashSet;
import java.util.Set;

import static org.onosproject.yms.app.ydt.YdtAppNodeOperationType.BOTH;
import static org.onosproject.yms.app.ydt.YdtTestConstants.A2L;
import static org.onosproject.yms.app.ydt.YdtTestConstants.A5L;
import static org.onosproject.yms.app.ydt.YdtTestConstants.A6L;
import static org.onosproject.yms.app.ydt.YdtTestConstants.AUG1;
import static org.onosproject.yms.app.ydt.YdtTestConstants.IETF;
import static org.onosproject.yms.app.ydt.YdtTestConstants.NETNS;
import static org.onosproject.yms.app.ydt.YdtTestConstants.SLINK;
import static org.onosproject.yms.app.ydt.YdtTestConstants.STP;
import static org.onosproject.yms.app.ydt.YdtTestConstants.TOPONS;
import static org.onosproject.yms.app.ydt.YdtTestUtils.augmentNetworkYdt;
import static org.onosproject.yms.app.ydt.YdtTestUtils.validateAppLogicalNodeContents;
import static org.onosproject.yms.app.ydt.YdtTestUtils.validateAppModuleNodeContents;
import static org.onosproject.yms.app.ydt.YdtTestUtils.validateAppNodeContents;
import static org.onosproject.yms.app.ydt.YdtTestUtils.validateLeafContents;
import static org.onosproject.yms.app.ydt.YdtTestUtils.validateLeafListContents;
import static org.onosproject.yms.app.ydt.YdtTestUtils.validateNodeContents;
import static org.onosproject.yms.app.ydt.YdtTestUtils.walkINTree;
import static org.onosproject.yms.ydt.YdtContextOperationType.DELETE;
import static org.onosproject.yms.ydt.YdtContextOperationType.MERGE;

public class AugmentTest {

    private Set<String> valueSet = new HashSet();

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

            "Entry Node is augment1.",
            "Entry Node is value1.",
            "Exit Node is value1.",
            "Exit Node is augment1.",

            "Entry Node is augment2.",
            "Entry Node is key1.",
            "Exit Node is key1.",
            "Entry Node is key2.",
            "Exit Node is key2.",

            "Entry Node is augment5.",

            "Entry Node is augment6leafList.",
            "Exit Node is augment6leafList.",

            "Entry Node is value5.",
            "Exit Node is value5.",
            "Exit Node is augment5.",

            "Entry Node is augment5leafList.",
            "Exit Node is augment5leafList.",

            "Entry Node is augment3.",

            "Entry Node is augment4.",
            "Entry Node is value4.",
            "Exit Node is value4.",
            "Exit Node is augment4.",

            "Entry Node is augment5.",

            "Entry Node is leaf6.",
            "Exit Node is leaf6.",

            "Entry Node is value5.",
            "Exit Node is value5.",
            "Exit Node is augment5.",

            "Entry Node is augment6.",
            "Entry Node is value6.",
            "Exit Node is value6.",
            "Exit Node is augment6.",

            "Entry Node is value3.",
            "Exit Node is value3.",
            "Exit Node is augment3.",

            "Entry Node is augment3leaf.",
            "Exit Node is augment3leaf.",

            "Exit Node is augment2.",

            "Entry Node is augment2leafList.",
            "Exit Node is augment2leafList.",

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

            "Entry Node is augment1.",
            "Entry Node is value1.",
            "Exit Node is value1.",
            "Exit Node is augment1.",

            "Entry Node is augment1-leaf.",
            "Exit Node is augment1-leaf.",

            "Entry Node is augment2.",

            "Entry Node is augment3.",
            "Entry Node is value3.",
            "Exit Node is value3.",
            "Exit Node is augment3.",

            "Entry Node is augment4leaf.",
            "Exit Node is augment4leaf.",

            "Entry Node is value2.",
            "Exit Node is value2.",
            "Exit Node is augment2.",
            "Entry Node is augment2leafList.",
            "Exit Node is augment2leafList.",
            "Entry Node is augment2leaf.",
            "Exit Node is augment2leaf.",

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

            "Entry Node is augmentNetwork.",
            "Entry Node is node.",
            "Entry Node is name.",
            "Exit Node is name.",
            "Entry Node is cont1s.",
            "Entry Node is cont1s.",
            "Entry Node is fine.",
            "Exit Node is fine.",

            // augmenting node augment1 under cont1s
            "Entry Node is augment1.",
            "Entry Node is value1.",
            "Exit Node is value1.",
            "Exit Node is augment1.",

            "Exit Node is cont1s.",
            "Exit Node is cont1s.",
            "Exit Node is node.",
            "Exit Node is augmentNetwork.",

            "Exit Node is yms-ietf-network."
    };

    /**
     * Creates and validates ietf network augment ydt.
     */
    @Test
    public void augmentTest() {
        YangRequestWorkBench ydtBuilder = augmentNetworkYdt();
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
        ydtNode = ydtNode.getNextSibling();

        validateNodeContents(ydtNode, "augment1", MERGE);
        ydtNode = ydtNode.getFirstChild();
        validateLeafContents(ydtNode, "value1", "1");
        ydtNode = ydtNode.getParent();

        ydtNode = ydtNode.getNextSibling();
        validateNodeContents(ydtNode, "augment2", MERGE);
        ydtNode = ydtNode.getFirstChild();
        validateLeafContents(ydtNode, "key1", "1");
        ydtNode = ydtNode.getNextSibling();
        validateLeafContents(ydtNode, "key2", "2");

        ydtNode = ydtNode.getNextSibling();
        validateNodeContents(ydtNode, "augment5", DELETE);
        ydtNode = ydtNode.getFirstChild();
        valueSet.add("1");
        valueSet.add("2");
        validateLeafListContents(ydtNode, A6L, valueSet);
        ydtNode = ydtNode.getNextSibling();

        validateLeafContents(ydtNode, "value5", "5");
        ydtNode = ydtNode.getParent();

        ydtNode = ydtNode.getNextSibling();
        validateLeafListContents(ydtNode, A5L, valueSet);

        ydtNode = ydtNode.getNextSibling();
        validateNodeContents(ydtNode, "augment3", MERGE);
        ydtNode = ydtNode.getFirstChild();

        validateNodeContents(ydtNode, "augment4", DELETE);
        ydtNode = ydtNode.getFirstChild();
        validateLeafContents(ydtNode, "value4", "4");
        ydtNode = ydtNode.getParent();
        ydtNode = ydtNode.getNextSibling();

        validateNodeContents(ydtNode, "augment5", MERGE);
        ydtNode = ydtNode.getFirstChild();

        validateLeafContents(ydtNode, "leaf6", "6");
        ydtNode = ydtNode.getNextSibling();

        validateLeafContents(ydtNode, "value5", "5");
        ydtNode = ydtNode.getParent();
        ydtNode = ydtNode.getNextSibling();

        validateNodeContents(ydtNode, "augment6", DELETE);
        ydtNode = ydtNode.getFirstChild();
        validateLeafContents(ydtNode, "value6", "6");
        ydtNode = ydtNode.getParent();
        ydtNode = ydtNode.getNextSibling();

        validateLeafContents(ydtNode, "value3", "3");
        ydtNode = ydtNode.getParent();
        ydtNode = ydtNode.getNextSibling();
        validateLeafContents(ydtNode, "augment3leaf", "3");

        ydtNode = ydtNode.getParent();

        ydtNode = ydtNode.getNextSibling();
        validateLeafListContents(ydtNode, A2L, valueSet);

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

        validateTerminationPointAugment(ydtNode);
    }

    /**
     * Validates the termination point node in given built ydt.
     */
    private void validateTerminationPointAugment(YdtNode ydtNode) {

        //Inside supporting-termination-point
        validateNodeContents(ydtNode, STP, MERGE);
        ydtNode = ydtNode.getFirstChild();
        validateLeafContents(ydtNode, "network-ref", "network-ref");
        ydtNode = ydtNode.getNextSibling();
        validateLeafContents(ydtNode, "node-ref", "node-ref");
        ydtNode = ydtNode.getNextSibling();
        validateLeafContents(ydtNode, "tp-ref", "tp-ref");

        ydtNode = ydtNode.getNextSibling();
        validateNodeContents(ydtNode, "augment1", MERGE);
        ydtNode = ydtNode.getFirstChild();
        validateLeafContents(ydtNode, "value1", "1");
        ydtNode = ydtNode.getParent();

        ydtNode = ydtNode.getNextSibling();
        validateLeafContents(ydtNode, "augment1-leaf", "1");

        ydtNode = ydtNode.getNextSibling();
        validateNodeContents(ydtNode, "augment2", MERGE);
        ydtNode = ydtNode.getFirstChild();

        validateNodeContents(ydtNode, "augment3", MERGE);
        ydtNode = ydtNode.getFirstChild();
        validateLeafContents(ydtNode, "value3", "3");
        ydtNode = ydtNode.getParent();

        ydtNode = ydtNode.getNextSibling();
        validateLeafContents(ydtNode, "augment4leaf", "4");

        ydtNode = ydtNode.getNextSibling();
        validateLeafContents(ydtNode, "value2", "2");
        ydtNode = ydtNode.getParent();
        ydtNode = ydtNode.getNextSibling();
        validateLeafListContents(ydtNode, A2L, valueSet);

        ydtNode = ydtNode.getNextSibling();
        validateLeafContents(ydtNode, "augment2leaf", "2");

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
        ydtNode = ydtNode.getParent().getParent().getParent();

        validateAugmentNetworkModule(ydtNode);
    }

    /**
     * Validates the given built ydt for augment network module.
     */
    private void validateAugmentNetworkModule(YdtNode ydtNode) {

        ydtNode = ydtNode.getNextSibling();
        //augmenting network module node
        validateNodeContents(ydtNode, "augmentNetwork", MERGE);
        ydtNode = ydtNode.getFirstChild();
        validateNodeContents(ydtNode, "node", MERGE);
        ydtNode = ydtNode.getFirstChild();
        validateLeafContents(ydtNode, "name", "node1");
        ydtNode = ydtNode.getNextSibling();
        validateNodeContents(ydtNode, "cont1s", MERGE);
        ydtNode = ydtNode.getFirstChild();
        validateNodeContents(ydtNode, "cont1s", MERGE);
        ydtNode = ydtNode.getFirstChild();
        validateLeafContents(ydtNode, "fine", "leaf");

        // checking augmenting node augment1
        ydtNode = ydtNode.getNextSibling();
        validateNodeContents(ydtNode, "augment1", DELETE);
        ydtNode = ydtNode.getFirstChild();
        validateLeafContents(ydtNode, "value1", "1");
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
        validateAppModuleNodeContents(ydtAppContext, IETF, BOTH);
        ydtAppContext = ydtAppContext.getFirstChild();

        //Inside link node
        validateAppNodeContents(ydtAppContext, AUG1, TOPONS, BOTH);

        ydtAppContext = ydtAppContext.getParent();
        validateAugmentNetworkAppTree(ydtAppContext);
    }

    /**
     * Validates the given built ydt application tree for augmenting network
     * module.
     */
    private void validateAugmentNetworkAppTree(YdtAppContext ydtAppContext) {

        ydtAppContext = ydtAppContext.getNextSibling();
        //augmenting network module node
        validateAppModuleNodeContents(ydtAppContext, "augmentNetwork", BOTH);
        ydtAppContext = ydtAppContext.getFirstChild();
        validateAppNodeContents(ydtAppContext, "/node", NETNS, BOTH);
    }
}

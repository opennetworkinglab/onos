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
import org.onosproject.yms.app.yob.DefaultYobBuilder;
import org.onosproject.yms.app.ytb.DefaultYangTreeBuilder;
import org.onosproject.yms.ydt.YdtContext;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import static org.onosproject.yms.app.ydt.YdtAppNodeOperationType.OTHER_EDIT;
import static org.onosproject.yms.app.ydt.YdtTestUtils.identityRefYdt;
import static org.onosproject.yms.app.ydt.YdtTestUtils.validateAppLogicalNodeContents;
import static org.onosproject.yms.app.ydt.YdtTestUtils.validateAppModuleNodeContents;
import static org.onosproject.yms.app.ydt.YdtTestUtils.validateLeafContents;
import static org.onosproject.yms.app.ydt.YdtTestUtils.validateLeafListContents;
import static org.onosproject.yms.app.ydt.YdtTestUtils.validateNodeContents;
import static org.onosproject.yms.app.ydt.YdtTestUtils.walkINTree;
import static org.onosproject.yms.ydt.YdtContextOperationType.MERGE;
import static org.onosproject.yms.ydt.YmsOperationType.EDIT_CONFIG_REPLY;

public class IdentityTest {

    Set<String> valueSet = new HashSet();

    private static final String[] EXPECTED = {
            "Entry Node is identityref.",
            "Entry Node is crypto-base.",
            "Entry Node is crypto.",
            "Exit Node is crypto.",
            "Entry Node is abc-zeunion.",
            "Exit Node is abc-zeunion.",
            "Entry Node is level2.",
            "Exit Node is level2.",
            "Entry Node is level3.",
            "Exit Node is level3.",
            "Entry Node is level4.",
            "Exit Node is level4.",
            "Entry Node is abc-type.",
            "Exit Node is abc-type.",
            "Exit Node is crypto-base.",
            "Exit Node is identityref.",
    };

    /**
     * Creates and validates identity ref in ydt.
     */
    @Test
    public void identityRefTest() {
        YangRequestWorkBench ydtBuilder = identityRefYdt();
        validateTree(ydtBuilder);
        validateAppTree(ydtBuilder);
        walkINTree(ydtBuilder, EXPECTED);

        //TODO need to be handled later
//        validateYangObject(ydtBuilder);
    }

    /**
     * Validates the given built ydt.
     */
    private void validateTree(YangRequestWorkBench ydtBuilder) {

        valueSet.add("crypto-alg");
        // Assign root node to ydtNode for validating purpose.
        YdtNode ydtNode = (YdtNode) ydtBuilder.getRootNode();
        // Logical root node does not have operation type
        validateNodeContents(ydtNode, "identityref", null);
        ydtNode = ydtNode.getFirstChild();
        validateNodeContents(ydtNode, "crypto-base", MERGE);
        ydtNode = ydtNode.getFirstChild();

        validateLeafContents(ydtNode, "crypto", "crypto-alg");
        ydtNode = ydtNode.getNextSibling();
        validateLeafContents(ydtNode, "abc-zeunion", "crypto-alg");
        ydtNode = ydtNode.getNextSibling();
        validateLeafContents(ydtNode, "level2", "crypto-alg2");
        ydtNode = ydtNode.getNextSibling();
        validateLeafContents(ydtNode, "level3", "crypto-alg3");
        ydtNode = ydtNode.getNextSibling();
        validateLeafContents(ydtNode, "level4", "crypto-alg3");
        ydtNode = ydtNode.getNextSibling();
        validateLeafListContents(ydtNode, "abc-type", valueSet);

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
        validateAppModuleNodeContents(ydtAppContext, "crypto-base",
                                      OTHER_EDIT);
    }

    /**
     * Creates Ydt from YO using YTB.
     */
    private void validateYangObject(YangRequestWorkBench ydtBuilder) {

        YdtContext rootCtx = ydtBuilder.getRootNode();

        YdtContext childCtx = rootCtx.getFirstChild();

        DefaultYobBuilder builder = new DefaultYobBuilder();

        Object yangObject = builder.getYangObject(
                (YdtExtendedContext) childCtx, YdtTestUtils
                        .getSchemaRegistry());

        List<Object> list = new LinkedList<>();
        list.add(yangObject);
        // Builds YANG tree in YTB.
        DefaultYangTreeBuilder treeBuilder = new DefaultYangTreeBuilder();
        YangRequestWorkBench defaultYdtBuilder =
                (YangRequestWorkBench) treeBuilder.getYdtBuilderForYo(
                        list, "identityref", "ydt.crypto-base",
                        EDIT_CONFIG_REPLY, YdtTestUtils
                                .getSchemaRegistry());

        // Validate the created YDT
        walkINTree(defaultYdtBuilder, EXPECTED);
        validateTree(defaultYdtBuilder);
    }
}

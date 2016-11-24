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

import java.util.ArrayList;
import java.util.List;

import static org.onosproject.yms.app.ydt.YdtTestUtils.helloOnos;
import static org.onosproject.yms.app.ydt.YdtTestUtils.validateLeafContents;
import static org.onosproject.yms.app.ydt.YdtTestUtils.validateNodeContents;
import static org.onosproject.yms.app.ydt.YdtTestUtils.walkINTree;
import static org.onosproject.yms.ydt.YdtContextOperationType.MERGE;

public class RpcTest {

    // Logger list is used for walker testing.
    private final List<String> logger = new ArrayList<>();

    private static final String[] EXPECTED = {
            "Entry Node is Hello-ONOS.",
            "Entry Node is Hello_ONOS.",
            "Entry Node is hello-world.",
            "Entry Node is input.",
            "Entry Node is name.",
            "Exit Node is name.",
            "Entry Node is surName.",
            "Exit Node is surName.",
            "Entry Node is stringList.",
            "Entry Node is string1.",
            "Exit Node is string1.",
            "Entry Node is string2.",
            "Exit Node is string2.",
            "Exit Node is stringList.",
            "Exit Node is input.",
            "Exit Node is hello-world.",
            "Exit Node is Hello_ONOS.",
            "Exit Node is Hello-ONOS."
    };

    /**
     * Creates and validates hello onos ydt.
     */
    @Test
    public void rpc1Test() {
        YangRequestWorkBench ydtBuilder = helloOnos();
        validateTree(ydtBuilder);
        // walker test
        walkINTree(ydtBuilder, EXPECTED);
    }

    /**
     * Validates the given built ydt.
     */
    private void validateTree(YangRequestWorkBench ydtBuilder) {

        // assign root node to ydtNode for validating purpose.
        YdtNode ydtNode = (YdtNode) ydtBuilder.getRootNode();
        // Logical root node does not have operation type
        validateNodeContents(ydtNode, "Hello-ONOS", null);
        ydtNode = ydtNode.getFirstChild();
        validateNodeContents(ydtNode, "Hello_ONOS", MERGE);
        ydtNode = ydtNode.getFirstChild();
        validateNodeContents(ydtNode, "hello-world", MERGE);
        ydtNode = ydtNode.getFirstChild();
        validateNodeContents(ydtNode, "input", MERGE);
        ydtNode = ydtNode.getFirstChild();
        validateLeafContents(ydtNode, "name", "onos");
        ydtNode = ydtNode.getNextSibling();
        validateLeafContents(ydtNode, "surName", "yang");
        ydtNode = ydtNode.getNextSibling();
        validateNodeContents(ydtNode, "stringList", MERGE);
        ydtNode = ydtNode.getFirstChild();
        validateLeafContents(ydtNode, "string1", "ON");
        ydtNode = ydtNode.getNextSibling();
        validateLeafContents(ydtNode, "string2", "LAB");
    }
}

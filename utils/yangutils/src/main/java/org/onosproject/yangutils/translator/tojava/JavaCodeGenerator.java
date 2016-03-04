/*
 * Copyright 2016 Open Networking Laboratory
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

package org.onosproject.yangutils.translator.tojava;

import java.io.IOException;

import org.onosproject.yangutils.datamodel.YangNode;

/**
 * Implementation of Java code generator based on application schema.
 */
public final class JavaCodeGenerator {

    /**
     * Default constructor.
     */
    private JavaCodeGenerator() {
    }

    /**
     * Generate Java code files corresponding to the YANG schema.
     *
     * @param rootNode root node of the data model tree
     * @param codeGenDir code generation directory
     * @throws IOException when fails to generate java code file the current node
     */
    public static void generateJavaCode(YangNode rootNode, String codeGenDir) throws IOException {
        YangNode curNode = rootNode;
        TraversalType curTraversal = TraversalType.ROOT;

        while (!(curNode == null)) {
            if (curTraversal != TraversalType.PARENT) {
                curNode.generateJavaCodeEntry(codeGenDir);
            }
            if (curTraversal != TraversalType.PARENT && curNode.getChild() != null) {
                curTraversal = TraversalType.CHILD;
                curNode = curNode.getChild();
            } else if (curNode.getNextSibling() != null) {
                curNode.generateJavaCodeExit();
                curTraversal = TraversalType.SIBILING;
                curNode = curNode.getNextSibling();
            } else {
                curNode.generateJavaCodeExit();
                curTraversal = TraversalType.PARENT;
                curNode = curNode.getParent();
            }
        }
    }
}

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

package org.onosproject.yangutils.translator.tojava;

import java.io.IOException;

import org.onosproject.yangutils.datamodel.YangNode;
import org.onosproject.yangutils.datamodel.exceptions.DataModelException;
import org.onosproject.yangutils.translator.exception.TranslatorException;
import org.onosproject.yangutils.translator.tojava.utils.YangPluginConfig;

import static org.onosproject.yangutils.translator.tojava.TraversalType.CHILD;
import static org.onosproject.yangutils.translator.tojava.TraversalType.PARENT;
import static org.onosproject.yangutils.translator.tojava.TraversalType.ROOT;
import static org.onosproject.yangutils.translator.tojava.TraversalType.SIBILING;

/**
 * Representation of java code generator based on application schema.
 */
public final class JavaCodeGeneratorUtil {

    /**
     * Current YANG node.
     */
    private static YangNode curNode;

    /**
     * Creates a java code generator util object.
     */
    private JavaCodeGeneratorUtil() {
    }

    /**
     * Returns current YANG node.
     *
     * @return current YANG node
     */
    public static YangNode getCurNode() {
        return curNode;
    }

    /**
     * Sets current YANG node.
     *
     * @param node current YANG node
     */
    public static void setCurNode(YangNode node) {
        curNode = node;
    }

    /**
     * Generates Java code files corresponding to the YANG schema.
     *
     * @param rootNode root node of the data model tree
     * @param yangPlugin YANG plugin config
     * @throws IOException when fails to generate java code file the current
     *             node
     */
    public static void generateJavaCode(YangNode rootNode, YangPluginConfig yangPlugin) throws IOException {

        YangNode curNode = rootNode;
        TraversalType curTraversal = ROOT;

        while (curNode != null) {
            if (curTraversal != PARENT) {
                setCurNode(curNode);
                generateCodeEntry(curNode, yangPlugin);
            }
            if (curTraversal != PARENT && curNode.getChild() != null) {
                curTraversal = CHILD;
                curNode = curNode.getChild();
            } else if (curNode.getNextSibling() != null) {
                generateCodeExit(curNode);
                curTraversal = SIBILING;
                curNode = curNode.getNextSibling();
            } else {
                generateCodeExit(curNode);
                curTraversal = PARENT;
                curNode = curNode.getParent();
            }
        }
    }

    /**
     * Generates the current nodes code snippet.
     *
     * @param curNode current data model node for which the code needs to be
     *            generated
     * @param yangPlugin YANG plugin config
     * @throws IOException IO operation exception
     */
    private static void generateCodeEntry(YangNode curNode, YangPluginConfig yangPlugin) throws IOException {

        if (curNode instanceof JavaCodeGenerator) {
            ((JavaCodeGenerator) curNode).generateCodeEntry(yangPlugin);
        } else {
            throw new TranslatorException(
                    "Generated data model node cannot be translated to target language code");
        }
    }

    /**
     * Generates the current nodes code target code from the snippet.
     *
     * @param curNode current data model node for which the code needs to be
     *            generated
     * @throws IOException IO operation exception
     */
    private static void generateCodeExit(YangNode curNode) throws IOException {

        if (curNode instanceof JavaCodeGenerator) {
            ((JavaCodeGenerator) curNode).generateCodeExit();
        } else {
            throw new TranslatorException(
                    "Generated data model node cannot be translated to target language code");
        }
    }

    /**
     * Free other YANG nodes of data-model tree when error occurs while file generation of current node.
     */
    public static void freeRestResources() {

        YangNode curNode = getCurNode();
        YangNode tempNode = curNode;
        TraversalType curTraversal = ROOT;

        while (curNode != tempNode.getParent()) {

            if (curTraversal != PARENT && curNode.getChild() != null) {
                curTraversal = CHILD;
                curNode = curNode.getChild();
            } else if (curNode.getNextSibling() != null) {
                curTraversal = SIBILING;
                if (curNode != tempNode) {
                    free(curNode);
                }
                curNode = curNode.getNextSibling();
            } else {
                curTraversal = PARENT;
                if (curNode != tempNode) {
                    free(curNode);
                }
                curNode = curNode.getParent();
            }
        }
    }

    /**
     * Free the current node.
     *
     * @param node YANG node
     */
    private static void free(YangNode node) {

        YangNode parent = node.getParent();
        parent.setChild(null);

        if (node.getNextSibling() != null) {
            parent.setChild(node.getNextSibling());
        } else if (node.getPreviousSibling() != null) {
            parent.setChild(node.getPreviousSibling());
        }
        node = null;
    }

    /**
     * Delete Java code files corresponding to the YANG schema.
     *
     * @param rootNode root node of data-model tree
     * @throws IOException when fails to delete java code file the current node
     * @throws DataModelException when fails to do datamodel operations
     */
    public static void translatorErrorHandler(YangNode rootNode) throws IOException, DataModelException {

        /**
         * Free other resources where translator has failed.
         */
        freeRestResources();

        /**
         * Start removing all open files.
         */
        YangNode curNode = rootNode;
        setCurNode(curNode.getChild());
        TraversalType curTraversal = ROOT;

        while (curNode != null) {

            if (curTraversal != PARENT) {
                close(curNode);
            }
            if (curTraversal != PARENT && curNode.getChild() != null) {
                curTraversal = CHILD;
                curNode = curNode.getChild();
            } else if (curNode.getNextSibling() != null) {
                curTraversal = SIBILING;
                curNode = curNode.getNextSibling();
            } else {
                curTraversal = PARENT;
                curNode = curNode.getParent();
            }
        }

        freeRestResources();
    }

    /**
     * Closes all the current open file handles of node and delete all generated files.
     *
     * @param curNode current YANG node
     * @throws IOException when fails to do IO operations
     */
    private static void close(YangNode curNode) throws IOException {

        if (((HasTempJavaCodeFragmentFiles) curNode).getTempJavaCodeFragmentFiles() != null) {
            ((HasTempJavaCodeFragmentFiles) curNode).getTempJavaCodeFragmentFiles().close(true);
        }
    }
}

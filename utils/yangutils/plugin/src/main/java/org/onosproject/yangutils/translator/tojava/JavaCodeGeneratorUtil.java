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
import org.onosproject.yangutils.translator.exception.TranslatorException;
import org.onosproject.yangutils.utils.io.impl.YangPluginConfig;

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
     * Creates a java code generator utility object.
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
     * @param rootNode   root node of the data model tree
     * @param yangPlugin YANG plugin config
     * @throws TranslatorException when fails to generate java code file the current
     *                             node
     */
    public static void generateJavaCode(YangNode rootNode, YangPluginConfig yangPlugin)
            throws TranslatorException {

        YangNode codeGenNode = rootNode;
        TraversalType curTraversal = ROOT;

        while (codeGenNode != null) {
            if (curTraversal != PARENT) {
                if (!(codeGenNode instanceof JavaCodeGenerator)) {
                    throw new TranslatorException("Unsupported node to generate code");
                }

                setCurNode(codeGenNode);
                try {
                    generateCodeEntry(codeGenNode, yangPlugin);
                } catch (Exception e) {
                    throw new TranslatorException(e.getMessage());
                }

            }
            if (curTraversal != PARENT && codeGenNode.getChild() != null) {
                curTraversal = CHILD;
                codeGenNode = codeGenNode.getChild();
            } else if (codeGenNode.getNextSibling() != null) {
                try {
                    generateCodeExit(codeGenNode);
                } catch (Exception e) {
                    throw new TranslatorException(e.getMessage());
                }
                curTraversal = SIBILING;
                codeGenNode = codeGenNode.getNextSibling();
            } else {
                try {
                    generateCodeExit(codeGenNode);
                } catch (Exception e) {
                    throw new TranslatorException(e.getMessage());
                }
                curTraversal = PARENT;
                codeGenNode = codeGenNode.getParent();
            }
        }
    }

    /**
     * Generates the current nodes code snippet.
     *
     * @param codeGenNode current data model node for which the code needs to be
     *                    generated
     * @param yangPlugin  YANG plugin config
     * @throws TranslatorException when fails to generate java code file the current
     *                             node
     */
    private static void generateCodeEntry(YangNode codeGenNode, YangPluginConfig yangPlugin)
            throws TranslatorException {

        if (codeGenNode instanceof JavaCodeGenerator) {
            ((JavaCodeGenerator) codeGenNode).generateCodeEntry(yangPlugin);
        } else {
            throw new TranslatorException(
                    "Generated data model node cannot be translated to target language code");
        }
    }

    /**
     * Generates the current nodes code target code from the snippet.
     *
     * @param codeGenNode current data model node for which the code needs to be
     *                    generated
     * @throws TranslatorException when fails to generate java code file the current
     *                             node
     */
    private static void generateCodeExit(YangNode codeGenNode)
            throws TranslatorException {

        if (codeGenNode instanceof JavaCodeGenerator) {
            ((JavaCodeGenerator) codeGenNode).generateCodeExit();
        } else {
            throw new TranslatorException(
                    "Generated data model node cannot be translated to target language code");
        }
    }

    /**
     * Free other YANG nodes of data-model tree when error occurs while file
     * generation of current node.
     */
    private static void freeRestResources() {

        YangNode freedNode = getCurNode();
        if (getCurNode() != null) {
            YangNode tempNode = freedNode;
            TraversalType curTraversal = ROOT;

            while (freedNode != tempNode.getParent()) {

                if (curTraversal != PARENT && freedNode.getChild() != null) {
                    curTraversal = CHILD;
                    freedNode = freedNode.getChild();
                } else if (freedNode.getNextSibling() != null) {
                    curTraversal = SIBILING;
                    if (freedNode != tempNode) {
                        free(freedNode);
                    }
                    freedNode = freedNode.getNextSibling();
                } else {
                    curTraversal = PARENT;
                    if (freedNode != tempNode) {
                        free(freedNode);
                    }
                    freedNode = freedNode.getParent();
                }
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
     */
    public static void translatorErrorHandler(YangNode rootNode)
            throws IOException {

        if (rootNode != null) {
            /**
             * Free other resources where translator has failed.
             */
            freeRestResources();

            /**
             * Start removing all open files.
             */
            YangNode tempNode = rootNode;
            setCurNode(tempNode.getChild());
            TraversalType curTraversal = ROOT;

            while (tempNode != null) {

                if (curTraversal != PARENT) {
                    close(tempNode);
                }
                if (curTraversal != PARENT && tempNode.getChild() != null) {
                    curTraversal = CHILD;
                    tempNode = tempNode.getChild();
                } else if (tempNode.getNextSibling() != null) {
                    curTraversal = SIBILING;
                    tempNode = tempNode.getNextSibling();
                } else {
                    curTraversal = PARENT;
                    tempNode = tempNode.getParent();
                }
            }

            freeRestResources();
        }
    }

    /**
     * Closes all the current open file handles of node and delete all generated
     * files.
     *
     * @param node current YANG node
     * @throws IOException when fails to do IO operations
     */
    private static void close(YangNode node)
            throws IOException {
        if (node instanceof JavaCodeGenerator && ((TempJavaCodeFragmentFilesContainer) node)
                .getTempJavaCodeFragmentFiles() != null) {
            ((TempJavaCodeFragmentFilesContainer) node).getTempJavaCodeFragmentFiles().freeTemporaryResources(true);
        }
    }
}

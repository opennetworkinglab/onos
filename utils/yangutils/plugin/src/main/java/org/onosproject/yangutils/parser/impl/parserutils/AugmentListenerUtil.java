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

package org.onosproject.yangutils.parser.impl.parserutils;

import java.util.ArrayList;
import java.util.List;

import org.onosproject.yangutils.datamodel.CollisionDetector;
import org.onosproject.yangutils.datamodel.YangModule;
import org.onosproject.yangutils.datamodel.YangNode;
import org.onosproject.yangutils.datamodel.YangNodeIdentifier;
import org.onosproject.yangutils.datamodel.YangSubModule;
import org.onosproject.yangutils.datamodel.exceptions.DataModelException;
import org.onosproject.yangutils.datamodel.utils.Parsable;
import org.onosproject.yangutils.parser.antlrgencode.GeneratedYangParser;
import org.onosproject.yangutils.parser.exceptions.ParserException;
import org.onosproject.yangutils.parser.impl.TreeWalkListener;

import static org.onosproject.yangutils.datamodel.utils.YangConstructType.AUGMENT_DATA;
import static org.onosproject.yangutils.utils.io.impl.YangIoUtils.getCapitalCase;

/**
 * Represents a utility which provides listener utilities augment node.
 */
public final class AugmentListenerUtil {

    /**
     * Prefix to be added to generated java file for augment node.
     */
    private static final String AUGMENTED = "Augmented";

    /**
     * The number of time augment has updated the same target node in same module/submodule.
     */
    private static int occurrenceCount = 1;

    /**
     * List of names for augment's generated java file.
     */
    private static List<String> augmentJavaFileNameList = new ArrayList<>();

    private static final int ONE = 1;
    private static final int TWO = 2;
    private static final int ZERO = 0;

    /**
     * Creates an instance of augment java file name generator utility.
     */
    private AugmentListenerUtil() {
    }

    /**
     * Sets the augment java file name list.
     *
     * @param nameList name list
     */
    private static void setAugmentJavaFileNameList(List<String> nameList) {
        augmentJavaFileNameList = nameList;
    }

    /**
     * Returns augment java file name list.
     *
     * @return augment java file name list
     */
    public static List<String> getAugmentJavaFileNameList() {
        return augmentJavaFileNameList;
    }

    /**
     * Sets occurrence count.
     *
     * @param occurrence occurrence count
     */
    private static void setOccurrenceCount(int occurrence) {
        occurrenceCount = occurrence;
    }

    /**
     * Returns occurrence count.
     *
     * @return occurrence count
     */
    private static int getOccurrenceCount() {
        return occurrenceCount;
    }

    /**
     * Generates name for augment node also detects collision for java file generation of augment node when
     * augment is updating the same target node in same parent multiple times.
     *
     * @param curData parsable data
     * @param targetNodes list of target nodes
     * @param listener tree walk listener
     * @return name for augment node
     */
    public static String generateNameForAugmentNode(Parsable curData, List<YangNodeIdentifier> targetNodes,
            TreeWalkListener listener) {

        String curPrefix = getParentsPrefix((YangNode) curData);
        YangNodeIdentifier nodeId = targetNodes.get(targetNodes.size() - 1);
        boolean isPrefix = isPrefixPresent(nodeId, curPrefix);
        String generateName = createValidNameForAugment(nodeId, isPrefix);

        if (listener.getParsedDataStack().peek() instanceof CollisionDetector) {
            try {
                ((CollisionDetector) listener.getParsedDataStack().peek()).detectCollidingChild(generateName,
                        AUGMENT_DATA);
            } catch (DataModelException e) {
                return updateNameWhenHasMultipleOuccrrence(nodeId, isPrefix);
            }
        }

        clearOccurrenceCount();
        return generateName;
    }

    /**
     * Creates a name identifier for augment.
     *
     * @param nodeId node identifier
     * @param isPrefix if prefix is present or it is not equals to parent's prefix
     * @return valid name for augment
     */
    public static String createValidNameForAugment(YangNodeIdentifier nodeId, boolean isPrefix) {
        getAugmentJavaFileNameList().add(createName(nodeId, isPrefix));
        setAugmentJavaFileNameList(getAugmentJavaFileNameList());
        return getAugmentJavaFileNameList().get(getAugmentJavaFileNameList().size() - 1);
    }

    /**
     * Creates name for the current augment file.
     *
     * @param nodeId node identifier
     * @param isPrefix if prefix is present or it is not equals to parent's prefix
     */
    private static String createName(YangNodeIdentifier nodeId, boolean isPrefix) {
        if (isPrefix) {
            return AUGMENTED + getCapitalCase(nodeId.getPrefix()) + getCapitalCase(nodeId.getName());
        } else {
            return AUGMENTED + getCapitalCase(nodeId.getName());
        }
    }

    /**
     * Updates occurrence count of augment.
     */
    private static void updateOccurenceCount() {
        int count = getOccurrenceCount();
        count++;
        setOccurrenceCount(count);
    }

    /**
     * Updates the list of name when augment has occurred multiple times to update the same target node
     * and returns a valid name for augment node's generated java file.
     *
     * @param nodeId YANG node identifier
     * @param isPrefix true if a prefix is present and it is not equals to parents prefix
     * @return valid name for augment node
     */
    public static String updateNameWhenHasMultipleOuccrrence(YangNodeIdentifier nodeId, boolean isPrefix) {
        String name = "";
        updateOccurenceCount();

        if (getOccurrenceCount() == TWO) {
            String previousAugmentsName = getAugmentJavaFileNameList().get(getAugmentJavaFileNameList().size() - ONE);
            getAugmentJavaFileNameList().remove(ZERO);
            getAugmentJavaFileNameList().add(previousAugmentsName + ONE);
            //TODO: update when already contains the name.
            name = createName(nodeId, isPrefix) + TWO;
        } else {
            name = createName(nodeId, isPrefix) + getOccurrenceCount();
        }
        getAugmentJavaFileNameList().add(name);
        return name;
    }

    /**
     * Resets occurrence count to one.
     */
    public static void clearOccurrenceCount() {
        setOccurrenceCount(ONE);
    }

    /**
     * Returns true if a prefix is present and it is not equals to parents prefix.
     *
     * @param nodeId YANG node identifier
     * @param parentsPrefix parent's prefix
     * @return true if a prefix is present and it is not equals to parents prefix
     */
    private static boolean isPrefixPresent(YangNodeIdentifier nodeId, String parentsPrefix) {
        return nodeId.getPrefix() != null && nodeId.getPrefix() != parentsPrefix;
    }

    /**
     * Validates whether current node in target path is valid or not.
     *
     * @param curNode current YANG node
     * @param targetNodes list of target nodes
     * @param ctx augment statement context
     */
    public static void validateNodeInTargetPath(YangNode curNode, List<YangNodeIdentifier> targetNodes,
            GeneratedYangParser.AugmentStatementContext ctx) {

        curNode = curNode.getChild();
        YangNode tempNode = validateCurrentTargetNode(targetNodes, curNode);
        if (tempNode != null) {
            switch (tempNode.getNodeType()) {
                case CONTAINER_NODE:
                    break;
                case LIST_NODE:
                    break;
                case CHOICE_NODE:
                    break;
                case CASE_NODE:
                    break;
                case INPUT_NODE:
                    break;
                case OUTPUT_NODE:
                    break;
                case NOTIFICATION_NODE:
                    break;
                default:
                    throw parserException(ctx);
            }
        } else {
            throw parserException(ctx);
        }
    }

    /**
     * Validates whether nodes in target node list are valid or not.
     *
     * @param targetNodes target node
     * @param curNode YANG node
     * @return true or false
     */
    private static YangNode validateCurrentTargetNode(List<YangNodeIdentifier> targetNodes, YangNode curNode) {
        YangNode tempNode = null;
        while (curNode != null) {
            tempNode = curNode;
            for (int i = 1; i < targetNodes.size(); i++) {
                if (curNode.getName().equals(targetNodes.get(i).getName())) {
                    if (curNode.getChild() != null && targetNodes.size() - 1 != i) {
                        curNode = curNode.getChild();
                    } else if (curNode.getChild() != null && targetNodes.size() - 1 == i) {
                        return curNode;
                    } else if (curNode.getChild() == null && targetNodes.size() - 1 == i) {
                        return curNode;
                    } else {
                        break;
                    }
                } else {
                    curNode = tempNode;
                    break;
                }
            }
            curNode = curNode.getNextSibling();
        }
        return null;
    }

    /**
     * Builds parser exception.
     *
     * @param ctx augment statement context
     * @return parser exception
     */
    public static ParserException parserException(GeneratedYangParser.AugmentStatementContext ctx) {
        int line = ctx.getStart().getLine();
        int charPositionInLine = ctx.getStart().getCharPositionInLine();
        ParserException exception = new ParserException("invalid target node path.");
        exception.setLine(line);
        exception.setCharPosition(charPositionInLine);
        return exception;
    }

    /**
     * Returns parent nodes prefix.
     *
     * @param curNode current YANG node
     * @return parent nodes prefix
     */
    public static String getParentsPrefix(YangNode curNode) {
        String curPrefix = null;
        if (curNode instanceof YangModule) {
            curPrefix = ((YangModule) curNode).getPrefix();
        } else if (curNode instanceof YangSubModule) {
            curPrefix = ((YangSubModule) curNode).getPrefix();
        }
        return curPrefix;
    }
}

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

package org.onosproject.yangutils.linker.impl;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import org.onosproject.yangutils.datamodel.YangAugment;
import org.onosproject.yangutils.datamodel.YangAugmentableNode;
import org.onosproject.yangutils.datamodel.YangAugmentedInfo;
import org.onosproject.yangutils.datamodel.YangCase;
import org.onosproject.yangutils.datamodel.YangChoice;
import org.onosproject.yangutils.datamodel.YangImport;
import org.onosproject.yangutils.datamodel.YangInclude;
import org.onosproject.yangutils.datamodel.YangLeaf;
import org.onosproject.yangutils.datamodel.YangLeafList;
import org.onosproject.yangutils.datamodel.YangLeafRef;
import org.onosproject.yangutils.datamodel.YangLeavesHolder;
import org.onosproject.yangutils.datamodel.YangNode;
import org.onosproject.yangutils.datamodel.YangNodeIdentifier;
import org.onosproject.yangutils.datamodel.YangReferenceResolver;
import org.onosproject.yangutils.datamodel.utils.YangConstructType;
import org.onosproject.yangutils.linker.exceptions.LinkerException;

import static org.onosproject.yangutils.utils.UtilConstants.COLON;
import static org.onosproject.yangutils.utils.UtilConstants.EMPTY_STRING;
import static org.onosproject.yangutils.utils.UtilConstants.SLASH_FOR_STRING;

/**
 * Represent utilities for YANG linker.
 */
public final class YangLinkerUtils {

    private static final int IDENTIFIER_LENGTH = 64;
    private static final Pattern IDENTIFIER_PATTERN = Pattern.compile("[a-zA-Z_][a-zA-Z0-9_.-]*");
    private static final String XML = "xml";

    private YangLinkerUtils() {
    }

    /**
     * Detects collision between target nodes leaf/leaf-list or child node with augmented leaf/leaf-list or child node.
     *
     * @param targetNode target node
     * @param augment    augment node
     */
    private static void detectCollision(YangNode targetNode, YangAugment augment) {
        YangNode targetNodesChild = targetNode.getChild();
        YangNode augmentsChild = augment.getChild();
        YangNode parent = targetNode;
        if (targetNode instanceof YangAugment) {
            parent = targetNode.getParent();
        } else {
            while (parent.getParent() != null) {
                parent = parent.getParent();
            }
        }
        if (targetNode instanceof YangChoice) {
            // no need to check here.
        } else {
            detectCollisionInLeaveHolders(targetNode, augment);
            while (augmentsChild != null) {
                detectCollisionInChildNodes(targetNodesChild, augmentsChild, targetNode.getName(), parent.getName());
                augmentsChild = augmentsChild.getNextSibling();
            }
        }
    }

    /*Detects collision between leaves/leaflists*/
    private static void detectCollisionInLeaveHolders(YangNode targetNode, YangAugment augment) {
        YangLeavesHolder targetNodesLeavesHolder = (YangLeavesHolder) targetNode;
        YangNode parent = targetNode;
        if (targetNode instanceof YangAugment) {
            parent = targetNode.getParent();
        } else {
            while (parent.getParent() != null) {
                parent = parent.getParent();
            }
        }
        if (augment.getListOfLeaf() != null && augment.getListOfLeaf().size() != 0
                && targetNodesLeavesHolder.getListOfLeaf() != null) {
            for (YangLeaf leaf : augment.getListOfLeaf()) {
                for (YangLeaf targetLeaf : targetNodesLeavesHolder.getListOfLeaf()) {
                    if (targetLeaf.getName().equals(leaf.getName())) {
                        throw new LinkerException("target node " + targetNode.getName()
                                + " contains augmented leaf " + leaf.getName() + " in module "
                                + parent.getName());
                    }
                }
            }
        } else if (augment.getListOfLeafList() != null
                && augment.getListOfLeafList().size() != 0
                && augment.getListOfLeafList() != null) {
            for (YangLeafList leafList : augment.getListOfLeafList()) {
                for (YangLeafList targetLeafList : targetNodesLeavesHolder.getListOfLeafList()) {
                    if (targetLeafList.getName().equals(leafList.getName())) {
                        throw new LinkerException("target node " + targetNode.getName()
                                + " contains augmented leaf-list" + leafList.getName() + " in module "
                                + parent.getName());
                    }
                }
            }
        }
    }

    /*Detects collision for child nodes.*/
    private static void detectCollisionInChildNodes(YangNode targetNodesChild, YangNode augmentsChild, String
            targetName, String parentName) {
        while (augmentsChild != null) {
            while (targetNodesChild != null) {
                if (targetNodesChild.getName().equals(augmentsChild.getName())) {
                    throw new LinkerException("target node " + targetName
                            + " contains augmented child node" + augmentsChild.getName() + " in module "
                            + parentName);
                }
                targetNodesChild = targetNodesChild.getNextSibling();
            }
            augmentsChild = augmentsChild.getNextSibling();
        }
    }

    /**
     * Detects collision between target nodes and its all leaf/leaf-list or child node with augmented leaf/leaf-list or
     * child node.
     *
     * @param targetNode target node
     * @param augment    augment node
     */
    static void detectCollisionForAugmentedNode(YangNode targetNode, YangAugment augment) {
        // Detect collision for target node and augment node.
        detectCollision(targetNode, augment);
        List<YangAugmentedInfo> yangAugmentedInfo = ((YangAugmentableNode) targetNode).getAugmentedInfoList();
        // Detect collision for target augment node and current augment node.
        for (YangAugmentedInfo info : yangAugmentedInfo) {
            detectCollision((YangAugment) info, augment);
        }
    }

    /**
     * Returns list of path names that are needed from augment.
     *
     * @param augment            instance of YANG augment
     * @param remainingAncestors ancestor count to move in augment path
     * @return list of path names needed in leafref
     */
    public static List<String> getPathWithAugment(YangAugment augment, int remainingAncestors) {
        String augmentName = augment.getName();
        List<String> listOfPathName = new ArrayList<>();
        if (augmentName.contains(SLASH_FOR_STRING)) {
            String[] augmentNodeNames = augmentName.split(SLASH_FOR_STRING);
            for (String valueInAugment : augmentNodeNames) {
                if (valueInAugment != null && valueInAugment != EMPTY_STRING && !valueInAugment.isEmpty()) {
                    listOfPathName.add(valueInAugment);
                }
            }
        }
        for (int countOfAncestor = 0; countOfAncestor < remainingAncestors; countOfAncestor++) {
            listOfPathName.remove(listOfPathName.size() - 1);
        }
        return listOfPathName;
    }

    /**
     * Skips the invalid nodes which cannot have data from YANG.
     *
     * @param currentParent current parent node reference
     * @param leafref       instance of YANG leafref
     * @return parent node which can hold data
     * @throws LinkerException a violation of linker rules
     */
    public static YangNode skipInvalidDataNodes(YangNode currentParent, YangLeafRef leafref) throws LinkerException {
        while (currentParent instanceof YangChoice || currentParent instanceof YangCase) {
            if (currentParent.getParent() == null) {
                throw new LinkerException("YANG file error: The target node, in the leafref path " +
                        leafref.getPath() + ", is invalid.");
            }
            currentParent = currentParent.getParent();
        }
        return currentParent;
    }

    /**
     * Checks and return valid node identifier.
     *
     * @param nodeIdentifierString string from yang file
     * @param yangConstruct        yang construct for creating error message
     * @return valid node identifier
     */
    public static YangNodeIdentifier getValidNodeIdentifier(String nodeIdentifierString,
                                                            YangConstructType yangConstruct) {
        String[] tmpData = nodeIdentifierString.split(Pattern.quote(COLON));
        if (tmpData.length == 1) {
            YangNodeIdentifier nodeIdentifier = new YangNodeIdentifier();
            nodeIdentifier.setName(getValidIdentifier(tmpData[0], yangConstruct));
            return nodeIdentifier;
        } else if (tmpData.length == 2) {
            YangNodeIdentifier nodeIdentifier = new YangNodeIdentifier();
            nodeIdentifier.setPrefix(getValidIdentifier(tmpData[0], yangConstruct));
            nodeIdentifier.setName(getValidIdentifier(tmpData[1], yangConstruct));
            return nodeIdentifier;
        } else {
            throw new LinkerException("YANG file error : " +
                    YangConstructType.getYangConstructType(yangConstruct) + " name " + nodeIdentifierString +
                    " is not valid.");
        }
    }

    /**
     * Validates identifier and returns concatenated string if string contains plus symbol.
     *
     * @param identifier    string from yang file
     * @param yangConstruct yang construct for creating error message=
     * @return concatenated string after removing double quotes
     */
    public static String getValidIdentifier(String identifier, YangConstructType yangConstruct) {

        if (identifier.length() > IDENTIFIER_LENGTH) {
            throw new LinkerException("YANG file error : " +
                    YangConstructType.getYangConstructType(yangConstruct) + " name " + identifier + " is " +
                    "greater than 64 characters.");
        } else if (!IDENTIFIER_PATTERN.matcher(identifier).matches()) {
            throw new LinkerException("YANG file error : " +
                    YangConstructType.getYangConstructType(yangConstruct) + " name " + identifier + " is not " +
                    "valid.");
        } else if (identifier.toLowerCase().startsWith(XML)) {
            throw new LinkerException("YANG file error : " +
                    YangConstructType.getYangConstructType(yangConstruct) + " identifier " + identifier +
                    " must not start with (('X'|'x') ('M'|'m') ('L'|'l')).");
        } else {
            return identifier;
        }
    }

    /**
     * Updates the priority for all the input files.
     *
     * @param yangNodeSet set of YANG files info
     */
    public static void updateFilePriority(Set<YangNode> yangNodeSet) {
        for (YangNode yangNode : yangNodeSet) {
            updateFilePriorityOfNode(yangNode);
        }
    }

    /**
     * Updates priority of the node.
     *
     * @param yangNode YANG node information
     */
    public static void updateFilePriorityOfNode(YangNode yangNode) {
        int curNodePriority = yangNode.getPriority();
        if (yangNode instanceof YangReferenceResolver) {
            List<YangImport> yangImportList = ((YangReferenceResolver) yangNode).getImportList();
            Iterator<YangImport> importInfoIterator = yangImportList.iterator();
            // Run through the imported list to update priority.
            while (importInfoIterator.hasNext()) {
                YangImport yangImport = importInfoIterator.next();
                YangNode importedNode = yangImport.getImportedNode();
                if (curNodePriority >= importedNode.getPriority()) {
                    importedNode.setPriority(curNodePriority + 1);
                    updateFilePriorityOfNode(importedNode);
                }
            }

            List<YangInclude> yangIncludeList = ((YangReferenceResolver) yangNode).getIncludeList();
            Iterator<YangInclude> includeInfoIterator = yangIncludeList.iterator();
            // Run through the imported list to update priority.
            while (includeInfoIterator.hasNext()) {
                YangInclude yangInclude = includeInfoIterator.next();
                YangNode includedNode = yangInclude.getIncludedNode();
                if (curNodePriority >= includedNode.getPriority()) {
                    includedNode.setPriority(curNodePriority + 1);
                    updateFilePriorityOfNode(includedNode);
                }
            }
        }
    }
}

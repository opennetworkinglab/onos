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

import java.util.List;

import org.onosproject.yangutils.datamodel.YangAugment;
import org.onosproject.yangutils.datamodel.YangAugmentableNode;
import org.onosproject.yangutils.datamodel.YangAugmentedInfo;
import org.onosproject.yangutils.datamodel.YangChoice;
import org.onosproject.yangutils.datamodel.YangLeaf;
import org.onosproject.yangutils.datamodel.YangLeafList;
import org.onosproject.yangutils.datamodel.YangLeavesHolder;
import org.onosproject.yangutils.datamodel.YangNode;
import org.onosproject.yangutils.linker.exceptions.LinkerException;

/**
 * Represent utilities for YANG linker.
 */
public final class YangLinkerUtils {

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
        YangLeavesHolder augmentsLeavesHolder = augment;
        if (targetNode instanceof YangChoice) {
            if (augmentsLeavesHolder.getListOfLeaf() != null
                    || augmentsLeavesHolder.getListOfLeafList() != null) {
                throw new LinkerException("target node " + targetNode.getName()
                        + "is a instance of choice. it can " +
                        "only be augmented with leaf using a case node.");
            }
        } else {
            YangLeavesHolder targetNodesLeavesHolder = (YangLeavesHolder) targetNode;

            YangNode parent = targetNode;
            if (targetNode instanceof YangAugment) {
                parent = targetNode.getParent();
            } else {
                while (parent.getParent() != null) {
                    parent = parent.getParent();
                }
            }
            if (augmentsLeavesHolder.getListOfLeaf() != null && augmentsLeavesHolder.getListOfLeaf().size() != 0
                    && targetNodesLeavesHolder.getListOfLeaf() != null) {
                for (YangLeaf leaf : augmentsLeavesHolder.getListOfLeaf()) {
                    for (YangLeaf targetLeaf : targetNodesLeavesHolder.getListOfLeaf()) {
                        if (targetLeaf.getName().equals(leaf.getName())) {
                            throw new LinkerException("target node " + targetNode.getName()
                                    + " contains augmented leaf " + leaf.getName() + " in module "
                                    + parent.getName());
                        }
                    }
                }
            } else if (augmentsLeavesHolder.getListOfLeafList() != null
                    && augmentsLeavesHolder.getListOfLeafList().size() != 0
                    && targetNodesLeavesHolder.getListOfLeafList() != null) {
                for (YangLeafList leafList : augmentsLeavesHolder.getListOfLeafList()) {
                    for (YangLeafList targetLeafList : targetNodesLeavesHolder.getListOfLeafList()) {
                        if (targetLeafList.getName().equals(leafList.getName())) {
                            throw new LinkerException("target node " + targetNode.getName()
                                    + " contains augmented leaf-list" + leafList.getName() + " in module "
                                    + parent.getName());
                        }
                    }
                }
            } else {
                while (augmentsChild != null) {
                    while (targetNodesChild != null) {
                        if (targetNodesChild.getName().equals(augmentsChild.getName())) {
                            throw new LinkerException("target node " + targetNode.getName()
                                    + " contains augmented child node" + augmentsChild.getName() + " in module "
                                    + parent.getName());
                        }
                        targetNodesChild = targetNodesChild.getNextSibling();
                    }
                    augmentsChild = augmentsChild.getNextSibling();
                }
            }
        }
    }

    /**
     * Detects collision between target nodes and its all leaf/leaf-list or child node with augmented leaf/leaf-list or
     * child node.
     *
     * @param targetNode target node
     * @param augment    augment node
     */
    public static void detectCollisionForAugmentedNode(YangNode targetNode, YangAugment augment) {
        // Detect collision for target node and augment node.
        detectCollision(targetNode, augment);
        List<YangAugmentedInfo> yangAugmentedInfo = ((YangAugmentableNode) targetNode).getAugmentedInfoList();
        // Detect collision for target augment node and current augment node.
        for (YangAugmentedInfo info : yangAugmentedInfo) {
            detectCollision((YangAugment) info, augment);
        }
    }
}

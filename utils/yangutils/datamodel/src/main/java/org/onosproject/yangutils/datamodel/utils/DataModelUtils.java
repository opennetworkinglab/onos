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

package org.onosproject.yangutils.datamodel.utils;

import java.util.List;
import java.util.Set;

import org.onosproject.yangutils.datamodel.CollisionDetector;
import org.onosproject.yangutils.datamodel.ResolvableType;
import org.onosproject.yangutils.datamodel.YangLeaf;
import org.onosproject.yangutils.datamodel.YangLeafList;
import org.onosproject.yangutils.datamodel.YangLeavesHolder;
import org.onosproject.yangutils.datamodel.YangNode;
import org.onosproject.yangutils.datamodel.YangReferenceResolver;
import org.onosproject.yangutils.datamodel.YangResolutionInfo;
import org.onosproject.yangutils.datamodel.YangRpc;
import org.onosproject.yangutils.datamodel.YangType;
import org.onosproject.yangutils.datamodel.exceptions.DataModelException;


/**
 * Represents utilities for data model tree.
 */
public final class DataModelUtils {
    /**
     * Creates a new data model tree utility.
     */
    private DataModelUtils() {
    }

    /**
     * Detects the colliding identifier name in a given YANG node and its child.
     *
     * @param identifierName name for which collision detection is to be checked
     * @param dataType       type of YANG node asking for detecting collision
     * @param node           instance of calling node
     * @throws DataModelException a violation of data model rules
     */
    public static void detectCollidingChildUtil(String identifierName, YangConstructType dataType, YangNode node)
            throws DataModelException {
        if (dataType == YangConstructType.USES_DATA || dataType == YangConstructType.GROUPING_DATA) {
            detectCollidingForUsesGrouping(identifierName, dataType, node);
        } else {
            if (node instanceof YangLeavesHolder) {
                YangLeavesHolder leavesHolder = (YangLeavesHolder) node;
                detectCollidingLeaf(leavesHolder.getListOfLeaf(), identifierName);
                detectCollidingLeafList(leavesHolder.getListOfLeafList(), identifierName);
            }
            node = node.getChild();
            while (node != null) {
                Parsable parsable = (Parsable) node;
                if (node instanceof CollisionDetector
                        && parsable.getYangConstructType() != YangConstructType.USES_DATA
                        && parsable.getYangConstructType() != YangConstructType.GROUPING_DATA) {
                    ((CollisionDetector) node).detectSelfCollision(identifierName, dataType);
                }
                node = node.getNextSibling();
            }
        }
    }

    /**
     * Detects colliding of uses and grouping only with uses and grouping respectively.
     *
     * @param identifierName name for which collision detection is to be checked
     * @param dataType       type of YANG node asking for detecting collision
     * @param node           node instance of calling node
     * @throws DataModelException a violation of data model rules
     */
    public static void detectCollidingForUsesGrouping(String identifierName, YangConstructType dataType, YangNode node)
            throws DataModelException {

        node = node.getChild();
        while (node != null) {
            Parsable parsable = (Parsable) node;
            if (node instanceof CollisionDetector
                    && parsable.getYangConstructType() == dataType) {
                ((CollisionDetector) node).detectSelfCollision(identifierName, dataType);
            }
            node = node.getNextSibling();
        }
    }

    /**
     * Detects the colliding identifier name in a given leaf node.
     *
     * @param listOfLeaf     List of leaves to detect collision
     * @param identifierName name for which collision detection is to be checked
     * @throws DataModelException a violation of data model rules
     */
    private static void detectCollidingLeaf(List<YangLeaf> listOfLeaf, String identifierName)
            throws DataModelException {

        if (listOfLeaf == null) {
            return;
        }
        for (YangLeaf leaf : listOfLeaf) {
            if (leaf.getName().equals(identifierName)) {
                throw new DataModelException("YANG file error: Duplicate input identifier detected, same as leaf \""
                        + leaf.getName() + "\"");
            }
        }
    }

    /**
     * Detects the colliding identifier name in a given leaf-list node.
     *
     * @param listOfLeafList list of leaf-lists to detect collision
     * @param identifierName name for which collision detection is to be checked
     * @throws DataModelException a violation of data model rules
     */
    private static void detectCollidingLeafList(List<YangLeafList> listOfLeafList, String identifierName)
            throws DataModelException {

        if (listOfLeafList == null) {
            return;
        }
        for (YangLeafList leafList : listOfLeafList) {
            if (leafList.getName().equals(identifierName)) {
                throw new DataModelException("YANG file error: Duplicate input identifier detected, same as leaf " +
                        "list \"" + leafList.getName() + "\"");
            }
        }
    }

    /**
     * Add a resolution information.
     *
     * @param resolutionInfo information about the YANG construct which has to be resolved
     * @throws DataModelException a violation of data model rules
     */
    public static void addResolutionInfo(YangResolutionInfo resolutionInfo)
            throws DataModelException {

        /* get the module node to add maintain the list of nested reference */
        YangNode curNode = resolutionInfo.getEntityToResolveInfo()
                .getHolderOfEntityToResolve();
        while (!(curNode instanceof YangReferenceResolver)) {
            curNode = curNode.getParent();
            if (curNode == null) {
                throw new DataModelException("Internal datamodel error: Datamodel tree is not correct");
            }
        }
        YangReferenceResolver resolutionNode = (YangReferenceResolver) curNode;

        if (resolutionInfo.getEntityToResolveInfo()
                .getEntityToResolve() instanceof YangType) {
            resolutionNode.addToResolutionList(resolutionInfo,
                    ResolvableType.YANG_DERIVED_DATA_TYPE);
        } else {
            resolutionNode.addToResolutionList(resolutionInfo,
                    ResolvableType.YANG_USES);
        }

    }

    /**
     * Resolve linking for a resolution list.
     *
     * @param resolutionList    resolution list for which linking to be done
     * @param dataModelRootNode module/sub-module node
     * @throws DataModelException a violation of data model rules
     */
    public static void resolveLinkingForResolutionList(List<YangResolutionInfo> resolutionList,
            YangReferenceResolver dataModelRootNode)
            throws DataModelException {

        for (YangResolutionInfo resolutionInfo : resolutionList) {
            resolutionInfo.resolveLinkingForResolutionInfo(dataModelRootNode);
        }
    }

    /**
     * Links type/uses referring to typedef/uses of inter YANG file.
     *
     * @param resolutionList    resolution list for which linking to be done
     * @param dataModelRootNode module/sub-module node
     * @throws DataModelException a violation of data model rules
     */
    public static void linkInterFileReferences(List<YangResolutionInfo> resolutionList,
            YangReferenceResolver dataModelRootNode)
            throws DataModelException {
        /*
         * Run through the resolution list, find type/uses referring to inter
         * file typedef/grouping, ask for linking.
         */
        for (YangResolutionInfo resolutionInfo : resolutionList) {
            resolutionInfo.linkInterFile(dataModelRootNode);
        }
    }

    /**
     * Checks if there is any rpc defined in the module or sub-module.
     *
     * @param rootNode root node of the data model
     * @return status of rpc's existence
     */
    public static boolean isRpcChildNodePresent(YangNode rootNode) {
        YangNode childNode = rootNode.getChild();
        while (childNode != null) {
            if (childNode instanceof YangRpc) {
                return true;
            }
            childNode = childNode.getNextSibling();
        }
        return false;
    }

    /**
     * Returns referred node in a given set.
     *
     * @param yangNodeSet YANG node set
     * @param refNodeName name of the node which is referred
     * @return referred node's reference
     */
    public static YangNode findReferredNode(Set<YangNode> yangNodeSet, String refNodeName) {
        /*
         * Run through the YANG files to see which YANG file matches the
         * referred node name.
         */
        for (YangNode yangNode : yangNodeSet) {
            if (yangNode.getName().equals(refNodeName)) {
                return yangNode;
            }
        }
        return null;
    }

    /**
     * Returns the contained data model parent node.
     *
     * @param currentNode current node which parent contained node is required
     * @return parent node in which the current node is an attribute
     */
    public static YangNode getParentNodeInGenCode(YangNode currentNode) {

        /*
         * TODO: recursive parent lookup to support choice/augment/uses. TODO:
         * need to check if this needs to be updated for
         * choice/case/augment/grouping
         */
        return currentNode.getParent();
    }
}

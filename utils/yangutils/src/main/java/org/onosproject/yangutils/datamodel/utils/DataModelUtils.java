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

import org.onosproject.yangutils.datamodel.CollisionDetector;
import org.onosproject.yangutils.datamodel.HasResolutionInfo;
import org.onosproject.yangutils.datamodel.YangImport;
import org.onosproject.yangutils.datamodel.YangLeaf;
import org.onosproject.yangutils.datamodel.YangLeafList;
import org.onosproject.yangutils.datamodel.YangLeavesHolder;
import org.onosproject.yangutils.datamodel.YangNode;
import org.onosproject.yangutils.datamodel.YangResolutionInfo;
import org.onosproject.yangutils.datamodel.exceptions.DataModelException;
import org.onosproject.yangutils.utils.YangConstructType;


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
     * @param identifierName name for which collision detection is to be
     * checked
     * @param dataType type of YANG node asking for detecting collision
     * @param node instance of calling node
     * @throws DataModelException a violation of data model rules
     */
    public static void detectCollidingChildUtil(String identifierName, YangConstructType dataType, YangNode node)
            throws DataModelException {

        if (dataType == YangConstructType.LEAF_DATA) {
            YangLeavesHolder leavesHolder = (YangLeavesHolder) node;
            if (leavesHolder.getListOfLeaf() != null) {
                detectCollidingLeaf(leavesHolder, identifierName);
            }
        }
        if (dataType == YangConstructType.LEAF_LIST_DATA) {
            if (((YangLeavesHolder) node).getListOfLeafList() != null) {
                YangLeavesHolder leavesHolder = (YangLeavesHolder) node;
                detectCollidingLeafList(leavesHolder, identifierName);
            }
        }
        node = node.getChild();
        while (node != null) {
            if (node instanceof CollisionDetector) {
                ((CollisionDetector) node).detectSelfCollision(identifierName, dataType);
            }
            node = node.getNextSibling();
        }
    }

    /**
     * Detects the colliding identifier name in a given leaf node.
     *
     * @param leavesHolder leaves node against which collision to be checked
     * @param identifierName name for which collision detection is to be
     * checked
     * @throws DataModelException a violation of data model rules
     */
    private static void detectCollidingLeaf(YangLeavesHolder leavesHolder, String identifierName)
            throws DataModelException {

        for (YangLeaf leaf : leavesHolder.getListOfLeaf()) {
            if (leaf.getLeafName().equals(identifierName)) {
                throw new DataModelException("YANG file error: Duplicate input identifier detected, same as leaf \""
                        + leaf.getLeafName() + "\"");
            }
        }
    }

    /**
     * Detects the colliding identifier name in a given leaf-list node.
     *
     * @param leavesHolder leaves node against which collision to be checked
     * @param identifierName name for which collision detection is to be
     * checked
     * @throws DataModelException a violation of data model rules
     */
    private static void detectCollidingLeafList(YangLeavesHolder leavesHolder, String identifierName)
            throws DataModelException {

        for (YangLeafList leafList : leavesHolder.getListOfLeafList()) {
            if (leafList.getLeafName().equals(identifierName)) {
                throw new DataModelException("YANG file error: Duplicate input identifier detected, same as leaf " +
                        "list \"" + leafList.getLeafName() + "\"");
            }
        }
    }

    /**
     * Add a resolution information.
     *
     * @param resolutionInfo information about the YANG construct which has to
     * be resolved
     * @throws DataModelException a violation of data model rules
     */
    public static void addResolutionInfo(YangResolutionInfo resolutionInfo)
            throws DataModelException {



        /* get the module node to add maintain the list of nested reference */
        YangNode curNode = resolutionInfo.getEntityToResolveInfo()
                .getHolderOfEntityToResolve();
        while (!(curNode instanceof HasResolutionInfo)) {
            curNode = curNode.getParent();
            if (curNode == null) {
                throw new DataModelException("Internal datamodel error: Datamodel tree is not correct");
            }
        }
        HasResolutionInfo resolutionNode = (HasResolutionInfo) curNode;

        if (!isPrefixValid(resolutionInfo.getEntityToResolveInfo().getEntityPrefix(),
                resolutionNode)) {
            throw new DataModelException("The prefix used is not valid");
        }
        resolutionNode.addToResolutionList(resolutionInfo);
    }

    private static boolean isPrefixValid(String entityPrefix, HasResolutionInfo resolutionNode) {
        if (entityPrefix == null) {
            return true;
        }

        if (resolutionNode.getPrefix().contentEquals(entityPrefix)) {
            return true;
        }

        if (resolutionNode.getImportList() != null) {
            for (YangImport importedInfo : resolutionNode.getImportList()) {
                if (importedInfo.getPrefixId().contentEquals(entityPrefix)) {
                    return true;
                }
            }
        }

        if (resolutionNode.getIncludeList() != null) {
            /**
             * TODO: check if the prefix matches with the imported data

             for (YangInclude includedInfo : resolutionNode.getIncludeList()) {
             if (includedInfo.contentEquals(prefix)) {
             return true;
             }
             }*/
        }

        return false;
    }

    /**
     * Resolve linking for a resolution list.
     *
     * @param resolutionList resolution list for which linking to be done
     * @param dataModelRootNode module/sub-module node
     * @throws DataModelException a violation of data model rules
     */
    public static void resolveLinkingForResolutionList(List<YangResolutionInfo> resolutionList,
            HasResolutionInfo dataModelRootNode)
            throws DataModelException {

        for (YangResolutionInfo resolutionInfo : resolutionList) {
            resolutionInfo.resolveLinkingForResolutionInfo(dataModelRootNode.getPrefix());
        }
    }
}

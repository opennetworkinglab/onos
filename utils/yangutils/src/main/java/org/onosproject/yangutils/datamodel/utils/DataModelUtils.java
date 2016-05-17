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

import java.util.Iterator;
import java.util.List;

import org.onosproject.yangutils.datamodel.CollisionDetector;
import org.onosproject.yangutils.datamodel.YangImport;
import org.onosproject.yangutils.datamodel.YangLeaf;
import org.onosproject.yangutils.datamodel.YangLeafList;
import org.onosproject.yangutils.datamodel.YangLeavesHolder;
import org.onosproject.yangutils.datamodel.YangNode;
import org.onosproject.yangutils.datamodel.YangReferenceResolver;
import org.onosproject.yangutils.datamodel.YangResolutionInfo;
import org.onosproject.yangutils.datamodel.YangRpc;
import org.onosproject.yangutils.datamodel.exceptions.DataModelException;
import org.onosproject.yangutils.parser.Parsable;
import org.onosproject.yangutils.plugin.manager.YangFileInfo;
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
                        && (parsable.getYangConstructType() != YangConstructType.USES_DATA)
                        && (parsable.getYangConstructType() != YangConstructType.GROUPING_DATA)) {
                    ((CollisionDetector) node).detectSelfCollision(identifierName, dataType);
                }
                node = node.getNextSibling();
            }
        }
    }

    /**
     * Detects colliding of uses and grouping only with uses and grouping respectively.
     *
     * @param identifierName name for which collision detection is to be
     * checked
     * @param dataType type of YANG node asking for detecting collision
     * @param node node instance of calling node
     * @throws DataModelException a violation of data model rules
     */
    public static void detectCollidingForUsesGrouping(String identifierName, YangConstructType dataType, YangNode node)
            throws DataModelException {

        node = node.getChild();
        while (node != null) {
            Parsable parsable = (Parsable) node;
            if (node instanceof CollisionDetector
                    && (parsable.getYangConstructType() == dataType)) {
                ((CollisionDetector) node).detectSelfCollision(identifierName, dataType);
            }
            node = node.getNextSibling();
        }
    }

    /**
     * Detects the colliding identifier name in a given leaf node.
     *
     * @param listOfLeaf List of leaves to detect collision
     * @param identifierName name for which collision detection is to be
     * checked
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
     * @param identifierName name for which collision detection is to be
     * checked
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
     * @param resolutionInfo information about the YANG construct which has to
     * be resolved
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

        if (!isPrefixValid(resolutionInfo.getEntityToResolveInfo().getEntityPrefix(),
                resolutionNode)) {
            throw new DataModelException("The prefix used is not valid");
        }
        resolutionNode.addToResolutionList(resolutionInfo);
    }

    /**
     * Evaluates whether the prefix in uses/type is valid.
     *
     * @param entityPrefix prefix in the current module/sub-module
     * @param resolutionNode uses/type node which has the prefix with it
     * @return whether prefix is valid or not
     */
    private static boolean isPrefixValid(String entityPrefix, YangReferenceResolver resolutionNode) {
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
            YangReferenceResolver dataModelRootNode)
            throws DataModelException {

        for (YangResolutionInfo resolutionInfo : resolutionList) {
            resolutionInfo.resolveLinkingForResolutionInfo(dataModelRootNode.getPrefix());
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
     * Returns module's data model node to which sub-module belongs to.
     *
     * @param yangFileInfo YANG file information
     * @param belongsToModuleName name of the module to which sub-module belongs to
     * @return module node to which sub-module belongs to
     * @throws DataModelException when belongs to module node is not found
     */
    public static YangNode findBelongsToModuleNode(List<YangFileInfo> yangFileInfo,
                String belongsToModuleName) throws DataModelException {
        Iterator<YangFileInfo> yangFileIterator = yangFileInfo.iterator();
        while (yangFileIterator.hasNext()) {
            YangFileInfo yangFile = yangFileIterator.next();
            YangNode yangNode = yangFile.getRootNode();
            if (yangNode.getName().equals(belongsToModuleName)) {
                return yangNode;
            }
        }
        throw new DataModelException("YANG file error : Module " + belongsToModuleName + " to which sub-module " +
                "belongs to is not found.");
    }
}

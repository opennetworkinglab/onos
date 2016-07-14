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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import org.onosproject.yangutils.datamodel.YangAtomicPath;
import org.onosproject.yangutils.datamodel.YangAugment;
import org.onosproject.yangutils.datamodel.YangImport;
import org.onosproject.yangutils.datamodel.YangInclude;
import org.onosproject.yangutils.datamodel.YangLeaf;
import org.onosproject.yangutils.datamodel.YangLeafList;
import org.onosproject.yangutils.datamodel.YangLeavesHolder;
import org.onosproject.yangutils.datamodel.YangModule;
import org.onosproject.yangutils.datamodel.YangNode;
import org.onosproject.yangutils.datamodel.YangNodeIdentifier;
import org.onosproject.yangutils.datamodel.YangSubModule;
import org.onosproject.yangutils.datamodel.YangUses;
import org.onosproject.yangutils.linker.exceptions.LinkerException;

/**
 * Represents x-path linking.
 *
 * @param <T> x-path linking can be done for target node or for target leaf/leaf-list
 */
public class YangXpathLinker<T> {

    /**
     * Enum for prefix resolver type when augment has come in path.
     */
    private static enum PrefixResolverType {

        /**
         * When prefix changes from inter file to intra file.
         */
        INTER_TO_INTRA,

        /**
         * When prefix changes from intra file to inter file.
         */
        INTRA_TO_INTER,

        /**
         * When prefix changes from one inter file to other inter file.
         */
        INTER_TO_INTER,

        /**
         * When no prefix change occurres.
         */
        NO_PREFIX_CHANGE_FOR_INTRA,

        /**
         * When no prefix change occurres.
         */
        NO_PREFIX_CHANGE_FOR_INTER
    }

    private List<YangAtomicPath> absPaths;
    private YangNode rootNode;
    private PrefixResolverType type;
    private String curPrefix;
    private Map<YangAtomicPath, YangNode> resolvedNodes;

    /**
     * Creates an instance of x-path linker.
     */
    public YangXpathLinker() {
        absPaths = new ArrayList<>();
        setResolvedNodes(new HashMap<>());
    }

    /**
     * Returns list of target nodes paths.
     *
     * @return target nodes paths
     */
    private List<YangAtomicPath> getAbsPaths() {
        return absPaths;
    }

    /**
     * Sets target nodes paths.
     *
     * @param absPaths target nodes paths
     */
    private void setAbsPaths(List<YangAtomicPath> absPaths) {
        this.absPaths = absPaths;
    }

    /**
     * Returns current prefix.
     *
     * @return current prefix
     */
    private String getCurPrefix() {
        return curPrefix;
    }

    /**
     * Sets current prefix.
     *
     * @param curPrefix current prefix
     */
    private void setCurPrefix(String curPrefix) {
        this.curPrefix = curPrefix;
    }

    /**
     * Return root node.
     *
     * @return root Node
     */
    private YangNode getRootNode() {
        return rootNode;
    }

    /**
     * Sets root node.
     *
     * @param rootNode root node
     */
    private void setRootNode(YangNode rootNode) {
        this.rootNode = rootNode;
    }

    /**
     * Returns prefix resolver type.
     *
     * @return prefix resolver type
     */
    private PrefixResolverType getPrefixResolverType() {
        return type;
    }

    /**
     * Sets prefix resolver type.
     *
     * @param type prefix resolver type
     */
    private void setPrefixResolverType(PrefixResolverType type) {
        this.type = type;
    }

    /**
     * Returns resolved nodes.
     *
     * @return resolved nodes
     */
    public Map<YangAtomicPath, YangNode> getResolvedNodes() {
        return resolvedNodes;
    }

    /**
     * Sets resolved nodes.
     *
     * @param resolvedNodes resolved nodes
     */
    private void setResolvedNodes(Map<YangAtomicPath, YangNode> resolvedNodes) {
        this.resolvedNodes = resolvedNodes;
    }

    /**
     * Adds node to resolved nodes.
     *
     * @param path absolute path
     * @param node resolved node
     */
    private void addToResolvedNodes(YangAtomicPath path, YangNode node) {
        getResolvedNodes().put(path, node);
    }

    /**
     * Returns list of augment nodes.
     *
     * @param node root node
     * @return list of augment nodes
     */
    public List<YangAugment> getListOfYangAugment(YangNode node) {
        node = node.getChild();
        List<YangAugment> augments = new ArrayList<>();
        while (node != null) {
            if (node instanceof YangAugment) {
                augments.add((YangAugment) node);
            }
            node = node.getNextSibling();
        }
        return augments;
    }

    /**
     * Process absolute node path for target leaf.
     *
     * @param absPaths absolute path node list
     * @param root root node
     * @return linked target node
     */
    public T processLeafRefXpathLinking(List<YangAtomicPath> absPaths, YangNode root) {

        YangNode targetNode = null;
        setRootNode(root);
        YangAtomicPath leafRefPath = absPaths.get(absPaths.size() - 1);

        // When leaf-ref path contains only one absolute path.
        if (absPaths.size() == 1) {
            targetNode = getTargetNodewhenSizeIsOne(absPaths);
        } else {
            absPaths.remove(absPaths.size() - 1);

            setAbsPaths(absPaths);
            targetNode = parseData(root);
        }
        if (targetNode == null) {
            targetNode = parsePath(getIncludedNode(root));
        }

        if (targetNode != null) {
            YangLeaf targetLeaf = searchReferredLeaf(targetNode, leafRefPath.getNodeIdentifier().getName());
            if (targetLeaf == null) {
                YangLeafList targetLeafList = searchReferredLeafList(targetNode,
                        leafRefPath.getNodeIdentifier().getName());
                if (targetLeafList != null) {
                    return (T) targetLeafList;
                } else {
                    throw new LinkerException(
                            "YANG file error: Unable to find base leaf/leaf-list for given leafref "
                                    + leafRefPath.getNodeIdentifier().getName());
                }
            }
            return (T) targetLeaf;
        }
        return null;
    }

    /**
     * Returns target node when leaf-ref has only one absolute path in list.
     *
     * @param absPaths absolute paths
     * @return target node
     */
    private YangNode getTargetNodewhenSizeIsOne(List<YangAtomicPath> absPaths) {
        if (absPaths.get(0).getNodeIdentifier().getPrefix() != null
                && !absPaths.get(0).getNodeIdentifier().getPrefix().equals(getRootsPrefix(getRootNode()))) {
            return getImportedNode(getRootNode(), absPaths.get(0).getNodeIdentifier());
        }
        return getRootNode();

    }

    /**
     * Process absolute node path linking for augment.
     *
     * @param absPaths absolute path node list
     * @param root root node
     * @return linked target node
     */
    public YangNode processAugmentXpathLinking(List<YangAtomicPath> absPaths, YangNode root) {

        setAbsPaths(absPaths);
        setRootNode(root);

        YangNode targetNode = parseData(root);

        if (targetNode == null) {
            targetNode = parsePath(getIncludedNode(root));
        }
        return targetNode;

    }

    /**
     * Searches for the referred leaf in target node.
     *
     * @param targetNode target node
     * @param leafName leaf name
     * @return target leaf
     */
    private YangLeaf searchReferredLeaf(YangNode targetNode, String leafName) {
        if (!(targetNode instanceof YangLeavesHolder)) {
            throw new LinkerException("Refered node " + targetNode.getName() +
                    "should be of type leaves holder ");
        }
        YangLeavesHolder holder = (YangLeavesHolder) targetNode;
        List<YangLeaf> leaves = holder.getListOfLeaf();
        for (YangLeaf leaf : leaves) {
            if (leaf.getName().equals(leafName)) {
                return leaf;
            }
        }
        return null;
    }

    /**
     * Searches for the referred leaf-list in target node.
     *
     * @param targetNode target node
     * @param leafListName leaf-list name
     * @return target leaf-list
     */
    private YangLeafList searchReferredLeafList(YangNode targetNode, String leafListName) {
        if (!(targetNode instanceof YangLeavesHolder)) {
            throw new LinkerException("Refered node " + targetNode.getName() +
                    "should be of type leaves holder ");
        }
        YangLeavesHolder holder = (YangLeavesHolder) targetNode;
        List<YangLeafList> leavesList = holder.getListOfLeafList();
        for (YangLeafList leafList : leavesList) {
            if (leafList.getName().equals(leafListName)) {
                return leafList;
            }
        }
        return null;
    }

    /**
     * Process linking using for node identifier for inter/intra file.
     *
     * @param root root node
     * @return linked target node
     */
    private YangNode parseData(YangNode root) {
        String rootPrefix = getRootsPrefix(root);
        Iterator<YangAtomicPath> pathIterator = getAbsPaths().iterator();
        YangAtomicPath path = pathIterator.next();
        if (path.getNodeIdentifier().getPrefix() != null
                && !path.getNodeIdentifier().getPrefix().equals(rootPrefix)) {
            return parsePath(getImportedNode(root, path.getNodeIdentifier()));
        } else {
            return parsePath(root);
        }
    }

    /**
     * Process linking of target node in root node.
     *
     * @param root root node
     * @return linked target node
     */
    private YangNode parsePath(YangNode root) {

        YangNode tempNode = root;
        Stack<YangNode> linkerStack = new Stack<>();
        Iterator<YangAtomicPath> pathIterator = getAbsPaths().iterator();
        YangAtomicPath tempPath = pathIterator.next();
        setCurPrefix(tempPath.getNodeIdentifier().getPrefix());
        int index = 0;
        YangNode tempAugment = null;
        do {

            if (tempNode instanceof YangUses) {
                tempNode = handleUsesNode(tempNode, tempPath.getNodeIdentifier());
                if (pathIterator.hasNext()) {
                    tempPath = pathIterator.next();
                    index++;
                } else {
                    addToResolvedNodes(tempPath, tempNode);
                    return tempNode;
                }
            }

            if (tempPath.getNodeIdentifier().getPrefix() == null) {
                tempAugment = resolveIntraFileAugment(tempPath, root);
            } else {
                tempAugment = resolveInterFileAugment(tempPath, root);
            }

            if (tempAugment != null) {
                linkerStack.push(tempNode);
                tempNode = tempAugment;
            }

            tempNode = searchTargetNode(tempNode, tempPath.getNodeIdentifier());
            if (tempNode == null && linkerStack.size() != 0) {
                tempNode = linkerStack.peek();
                linkerStack.pop();
                tempNode = searchTargetNode(tempNode, tempPath.getNodeIdentifier());
            }

            if (tempNode != null) {
                addToResolvedNodes(tempPath, tempNode);
            }

            if (index == getAbsPaths().size() - 1) {
                break;
            }
            tempPath = pathIterator.next();
            index++;
        } while (validate(tempNode, index));
        return tempNode;
    }

    /**
     * Resolves intra file augment linking.
     *
     * @param tempPath temporary absolute path
     * @param root root node
     * @return linked target node
     */
    private YangNode resolveIntraFileAugment(YangAtomicPath tempPath, YangNode root) {
        YangNode tempAugment = null;
        setPrefixResolverType(PrefixResolverType.NO_PREFIX_CHANGE_FOR_INTRA);
        if (getCurPrefix() != tempPath.getNodeIdentifier().getPrefix()) {
            setPrefixResolverType(PrefixResolverType.INTRA_TO_INTER);
            root = getIncludedNode(getRootNode());
        }

        setCurPrefix(tempPath.getNodeIdentifier().getPrefix());
        tempAugment = getAugment(tempPath.getNodeIdentifier(), root, getAbsPaths());
        if (tempAugment == null) {
            tempAugment = getAugment(tempPath.getNodeIdentifier(), getRootNode(), getAbsPaths());
        }
        return tempAugment;
    }

    /**
     * Resolves inter file augment linking.
     *
     * @param tempPath temporary absolute path
     * @param root root node
     * @return linked target node
     */
    private YangNode resolveInterFileAugment(YangAtomicPath tempPath, YangNode root) {

        YangNode tempAugment = null;
        if (tempPath.getNodeIdentifier().getPrefix().equals(getCurPrefix())) {
            setPrefixResolverType(PrefixResolverType.NO_PREFIX_CHANGE_FOR_INTER);
        } else {
            setCurPrefix(tempPath.getNodeIdentifier().getPrefix());
            setPrefixResolverType(PrefixResolverType.INTER_TO_INTER);
            if (getCurPrefix() == null) {
                setPrefixResolverType(PrefixResolverType.INTER_TO_INTRA);
            }
            root = getImportedNode(getRootNode(), tempPath.getNodeIdentifier());
        }
        tempAugment = getAugment(tempPath.getNodeIdentifier(), root, getAbsPaths());
        if (tempAugment == null && getPrefixResolverType().equals(PrefixResolverType.INTER_TO_INTER)) {
            return resolveInterToInterFileAugment(root);
        }
        return tempAugment;
    }

    /**
     * Resolves augment when prefix changed from inter file to inter file.
     * it may be possible that the prefix used in imported module is different the
     * given list of node identifiers.
     *
     * @param root root node
     * @return target node
     */
    private YangNode resolveInterToInterFileAugment(YangNode root) {
        List<YangAugment> augments = getListOfYangAugment(root);
        int index;
        List<YangAtomicPath> absPaths = new ArrayList<>();
        for (YangAugment augment : augments) {
            index = 0;

            for (YangAtomicPath path : augment.getTargetNode()) {

                if (!searchForAugmentInImportedNode(path.getNodeIdentifier(), index)) {
                    absPaths.clear();
                    break;
                }
                absPaths.add(path);
                index++;
            }
            if (!absPaths.isEmpty() && absPaths.size() == getAbsPaths().size() - 1) {
                return augment;
            } else {
                absPaths.clear();
            }
        }
        return null;
    }

    /**
     * Searches for the augment node in imported module when prefix has changed from
     * inter file to inter file.
     * @param nodeId node id
     * @param index index
     * @return true if found
     */
    private boolean searchForAugmentInImportedNode(YangNodeIdentifier nodeId, int index) {
        YangNodeIdentifier tempNodeId = getAbsPaths().get(index).getNodeIdentifier();
        return nodeId.getName().equals(tempNodeId.getName());
    }

    /**
     * Returns augment node.
     *
     * @param tempNodeId temporary absolute path id
     * @param root root node
     * @return linked target node
     */
    private YangNode getAugment(YangNodeIdentifier tempNodeId, YangNode root, List<YangAtomicPath> absPaths) {
        String augmentName = getAugmentNodeIdentifier(tempNodeId, absPaths);
        if (augmentName != null) {
            return searchAugmentNode(root, augmentName);
        }
        return null;
    }

    /**
     * Process linking using import list.
     *
     * @param root root node
     * @param nodeId node identifier
     * @return linked target node
     */
    private YangNode getImportedNode(YangNode root, YangNodeIdentifier nodeId) {

        List<YangImport> importList = new ArrayList<>();

        if (root instanceof YangModule) {
            importList = ((YangModule) root).getImportList();
        } else {
            importList = ((YangSubModule) root).getImportList();
        }

        for (YangImport imported : importList) {
            if (imported.getPrefixId().equals(nodeId.getPrefix())) {
                return imported.getImportedNode();
            }
        }

        return root;
    }

    /**
     * Process linking using include list.
     *
     * @param root root node
     * @return linked target node
     */
    private YangNode getIncludedNode(YangNode root) {

        List<YangInclude> includeList = new ArrayList<>();

        if (root instanceof YangModule) {
            includeList = ((YangModule) root).getIncludeList();
        } else {
            includeList = ((YangSubModule) root).getIncludeList();
        }

        for (YangInclude included : includeList) {
            return included.getIncludedNode();
        }

        return root;
    }

    /**
     * Returns augments node id.
     *
     * @param nodeId node identifier
     * @return augment node id
     */
    private String getAugmentNodeIdentifier(YangNodeIdentifier nodeId, List<YangAtomicPath> absPaths) {

        Iterator<YangAtomicPath> nodeIdIterator = absPaths.iterator();
        YangAtomicPath tempNodeId = null;
        StringBuilder builder = new StringBuilder();
        while (nodeIdIterator.hasNext()) {
            tempNodeId = nodeIdIterator.next();
            if (!tempNodeId.getNodeIdentifier().equals(nodeId)) {
                if (tempNodeId.getNodeIdentifier().getPrefix() != null
                        && (getPrefixResolverType().equals(PrefixResolverType.INTER_TO_INTER)
                                || getPrefixResolverType().equals(PrefixResolverType.INTRA_TO_INTER))) {
                    builder.append("/" + tempNodeId.getNodeIdentifier().getPrefix());
                    builder.append(":" + tempNodeId.getNodeIdentifier().getName());
                } else {
                    builder.append("/" + tempNodeId.getNodeIdentifier().getName());
                }
            } else {
                return builder.toString();
            }
        }
        return null;
    }

    /**
     * Searches augment node in root node.
     *
     * @param node root node
     * @param tempNodeId node identifier
     * @return target augment node
     */
    private YangNode searchAugmentNode(YangNode node, String tempNodeId) {
        node = node.getChild();
        while (node != null) {
            if (node instanceof YangAugment) {
                if (((YangAugment) node).getName().equals(tempNodeId)) {
                    return node;
                }
            }
            node = node.getNextSibling();
        }
        return null;
    }

    /**
     * Validates for target node if target node found or not.
     *
     * @param tempNode temporary node
     * @param index current index of list
     * @return false if target node found
     */
    private boolean validate(YangNode tempNode, int index) {

        int size = getAbsPaths().size();
        if (tempNode != null && index != size) {
            return true;
        } else if (tempNode != null && index == size) {
            return false;
            // this is your target node.
        } else if (tempNode == null && index != size) {
            return false;
            // this could be in submodule as well.
        }
        return false;
    }

    /**
     * Searches target node in root node.
     *
     * @param node root node
     * @param curNodeId YANG node identifier
     * @return linked target node
     */
    private YangNode searchTargetNode(YangNode node, YangNodeIdentifier curNodeId) {

        if (node != null) {
            node = node.getChild();
        }

        while (node != null) {
            if (node.getName().equals(curNodeId.getName())) {
                return node;
            }
            node = node.getNextSibling();
        }
        return null;
    }

    /**
     * Handles linking when uses node is present.
     *
     * @param node uses node
     * @param curNodeId current node id
     * @return linked node
     */
    private YangNode handleUsesNode(YangNode node, YangNodeIdentifier curNodeId) {
        YangNode tempNode = null;
        tempNode = searchInUsesNode((YangUses) node, curNodeId);
        if (tempNode != null) {
            return tempNode;
        }
        return null;
    }

    /**
     * Searches target node in uses resolved list.
     *
     * @param uses uses node
     * @param curNodeId current node id
     * @return linked target node
     */
    private YangNode searchInUsesNode(YangUses uses, YangNodeIdentifier curNodeId) {

        List<YangNode> resolvedNodes = uses.getUsesResolvedNodeList();
        for (YangNode node : resolvedNodes) {
            if (node.getName().equals(curNodeId.getName())) {
                return node;
            }
        }
        return null;
    }

    /**
     * Returns root prefix.
     *
     * @param root root node
     * @return root prefix
     */
    private String getRootsPrefix(YangNode root) {
        if (root instanceof YangModule) {
            return ((YangModule) root).getPrefix();
        } else {
            return ((YangSubModule) root).getPrefix();
        }
    }

}

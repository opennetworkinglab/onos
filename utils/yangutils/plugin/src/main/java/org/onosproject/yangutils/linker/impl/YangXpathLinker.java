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
import org.onosproject.yangutils.datamodel.YangCase;
import org.onosproject.yangutils.datamodel.YangChoice;
import org.onosproject.yangutils.datamodel.YangGrouping;
import org.onosproject.yangutils.datamodel.YangImport;
import org.onosproject.yangutils.datamodel.YangInclude;
import org.onosproject.yangutils.datamodel.YangInput;
import org.onosproject.yangutils.datamodel.YangLeaf;
import org.onosproject.yangutils.datamodel.YangLeafList;
import org.onosproject.yangutils.datamodel.YangLeafRef;
import org.onosproject.yangutils.datamodel.YangLeavesHolder;
import org.onosproject.yangutils.datamodel.YangModule;
import org.onosproject.yangutils.datamodel.YangNode;
import org.onosproject.yangutils.datamodel.YangNodeIdentifier;
import org.onosproject.yangutils.datamodel.YangOutput;
import org.onosproject.yangutils.datamodel.YangSubModule;
import org.onosproject.yangutils.datamodel.YangTypeDef;
import org.onosproject.yangutils.datamodel.YangUses;
import org.onosproject.yangutils.linker.exceptions.LinkerException;

import static org.onosproject.yangutils.linker.impl.PrefixResolverType.INTER_TO_INTER;
import static org.onosproject.yangutils.linker.impl.PrefixResolverType.INTER_TO_INTRA;
import static org.onosproject.yangutils.linker.impl.PrefixResolverType.INTRA_TO_INTER;
import static org.onosproject.yangutils.linker.impl.PrefixResolverType.NO_PREFIX_CHANGE_FOR_INTER;
import static org.onosproject.yangutils.linker.impl.PrefixResolverType.NO_PREFIX_CHANGE_FOR_INTRA;
import static org.onosproject.yangutils.utils.UtilConstants.INPUT;
import static org.onosproject.yangutils.utils.UtilConstants.OUTPUT;

/**
 * Represents x-path linking.
 *
 * @param <T> x-path linking can be done for target node or for target leaf/leaf-list
 */
public class YangXpathLinker<T> {

    private List<YangAtomicPath> absPaths;
    private YangNode rootNode;
    private Map<YangAtomicPath, PrefixResolverType> prefixResolverTypes;
    private String curPrefix;

    /**
     * Creates an instance of x-path linker.
     */
    public YangXpathLinker() {
        absPaths = new ArrayList<>();
    }

    /**
     * Returns prefix resolver list.
     *
     * @return prefix resolver list
     */
    private Map<YangAtomicPath, PrefixResolverType> getPrefixResolverTypes() {
        return prefixResolverTypes;
    }

    /**
     * Sets prefix resolver list.
     *
     * @param prefixResolverTypes prefix resolver list.
     */
    private void setPrefixResolverTypes(Map<YangAtomicPath, PrefixResolverType> prefixResolverTypes) {
        this.prefixResolverTypes = prefixResolverTypes;
    }

    /**
     * Adds to the prefix resolver type map.
     *
     * @param type resolver type
     * @param path absolute path
     */
    private void addToPrefixResolverList(PrefixResolverType type, YangAtomicPath path) {
        getPrefixResolverTypes().put(path, type);
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
     * Adds node to resolved nodes.
     *
     * @param path absolute path
     * @param node resolved node
     */
    private void addToResolvedNodes(YangAtomicPath path, YangNode node) {
        path.setResolvedNode(node);
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
     * @param atomicPaths atomic path node list
     * @param root     root node
     * @param leafref  instance of YANG leafref
     * @return linked target node
     */
    T processLeafRefXpathLinking(List<YangAtomicPath> atomicPaths, YangNode root, YangLeafRef leafref) {

        YangNode targetNode;
        setRootNode(root);
        setPrefixResolverTypes(new HashMap<>());
        parsePrefixResolverList(atomicPaths);
        YangAtomicPath leafRefPath = atomicPaths.get(atomicPaths.size() - 1);

        // When leaf-ref path contains only one absolute path.
        if (atomicPaths.size() == 1) {
            targetNode = getTargetNodewhenSizeIsOne(atomicPaths);
        } else {
            atomicPaths.remove(atomicPaths.size() - 1);

            setAbsPaths(atomicPaths);
            targetNode = parseData(root);
        }
        if (targetNode == null) {
            targetNode = searchInSubModule(root);
        }

        // Invalid path presence in the node list is checked.
        validateInvalidNodesInThePath(leafref);

        if (targetNode != null) {
            YangLeaf targetLeaf = searchReferredLeaf(targetNode, leafRefPath.getNodeIdentifier().getName());
            if (targetLeaf == null) {
                YangLeafList targetLeafList = searchReferredLeafList(targetNode,
                        leafRefPath.getNodeIdentifier().getName());
                if (targetLeafList != null) {
                    return (T) targetLeafList;
                } else {
                    LinkerException linkerException = new LinkerException("YANG file error: Unable to find base " +
                            "leaf/leaf-list for given leafref path "
                            + leafref.getPath());
                    linkerException.setCharPosition(leafref.getCharPosition());
                    linkerException.setLine(leafref.getLineNumber());
                    throw linkerException;
                }
            }
            return (T) targetLeaf;
        }
        return null;
    }

    /**
     * Validates the nodes in the path for any invalid node.
     *
     * @param leafref instance of YANG leafref
     */
    private void validateInvalidNodesInThePath(YangLeafRef leafref) {
        for (YangAtomicPath absolutePath : (Iterable<YangAtomicPath>) leafref.getAtomicPath()) {
            YangNode nodeInPath = absolutePath.getResolvedNode();

            if (nodeInPath instanceof YangGrouping || nodeInPath instanceof YangUses
                    || nodeInPath instanceof YangTypeDef || nodeInPath instanceof YangCase
                    || nodeInPath instanceof YangChoice) {
                LinkerException linkerException = new LinkerException("YANG file error: The target node, in the " +
                        "leafref path " + leafref.getPath() + ", is invalid.");
                linkerException.setCharPosition(leafref.getCharPosition());
                linkerException.setLine(leafref.getLineNumber());
                throw linkerException;
            }
        }
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
     * @param root     root node
     * @return linked target node
     */
    public YangNode processAugmentXpathLinking(List<YangAtomicPath> absPaths, YangNode root) {

        setAbsPaths(absPaths);
        setRootNode(root);
        setPrefixResolverTypes(new HashMap<>());
        parsePrefixResolverList(absPaths);

        YangNode targetNode = parseData(root);

        if (targetNode == null) {
            targetNode = searchInSubModule(root);
        }
        return targetNode;

    }

    /**
     * Searches for the referred leaf in target node.
     *
     * @param targetNode target node
     * @param leafName   leaf name
     * @return target leaf
     */
    private YangLeaf searchReferredLeaf(YangNode targetNode, String leafName) {
        if (!(targetNode instanceof YangLeavesHolder)) {
            throw new LinkerException("Refered node " + targetNode.getName() +
                    "should be of type leaves holder ");
        }
        YangLeavesHolder holder = (YangLeavesHolder) targetNode;
        List<YangLeaf> leaves = holder.getListOfLeaf();
        if (leaves != null && !leaves.isEmpty()) {
            for (YangLeaf leaf : leaves) {
                if (leaf.getName().equals(leafName)) {
                    return leaf;
                }
            }
        }
        return null;
    }

    /**
     * Searches for the referred leaf-list in target node.
     *
     * @param targetNode   target node
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
        if (leavesList != null && !leavesList.isEmpty()) {
            for (YangLeafList leafList : leavesList) {
                if (leafList.getName().equals(leafListName)) {
                    return leafList;
                }
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
        YangNode tempAugment;
        do {

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
     * @param root     root node
     * @return linked target node
     */
    private YangNode resolveIntraFileAugment(YangAtomicPath tempPath, YangNode root) {
        YangNode tempAugment;
        if (getCurPrefix() != tempPath.getNodeIdentifier().getPrefix()) {
            root = getIncludedNode(getRootNode(), tempPath.getNodeIdentifier().getName());
            if (root == null) {
                root = getIncludedNode(getRootNode(), getAugmentNodeIdentifier(tempPath.getNodeIdentifier(), absPaths,
                        getRootNode()));
                if (root == null) {
                    root = getRootNode();
                }
            }
        } else {
            if (getCurPrefix() != null) {
                root = getImportedNode(root, tempPath.getNodeIdentifier());
            }
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
     * @param root     root node
     * @return linked target node
     */
    private YangNode resolveInterFileAugment(YangAtomicPath tempPath, YangNode root) {

        YangNode tempAugment;
        if (!tempPath.getNodeIdentifier().getPrefix().equals(getCurPrefix())) {
            setCurPrefix(tempPath.getNodeIdentifier().getPrefix());
            root = getImportedNode(getRootNode(), tempPath.getNodeIdentifier());
        }
        tempAugment = getAugment(tempPath.getNodeIdentifier(), root, getAbsPaths());
        if (tempAugment == null) {
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
     *
     * @param nodeId node id
     * @param index  index
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
     * @param root       root node
     * @return linked target node
     */
    private YangNode getAugment(YangNodeIdentifier tempNodeId, YangNode root, List<YangAtomicPath> absPaths) {
        String augmentName = getAugmentNodeIdentifier(tempNodeId, absPaths, root);
        if (augmentName != null) {
            return searchAugmentNode(root, augmentName);
        }
        return null;
    }

    /**
     * Process linking using import list.
     *
     * @param root   root node
     * @param nodeId node identifier
     * @return linked target node
     */
    private YangNode getImportedNode(YangNode root, YangNodeIdentifier nodeId) {

        List<YangImport> importList;

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
     * Searches in sub-module node.
     *
     * @param root root node
     * @return target linked node
     */
    private YangNode searchInSubModule(YangNode root) {
        List<YangInclude> includeList;
        YangNode tempNode;
        if (root instanceof YangModule) {
            includeList = ((YangModule) root).getIncludeList();
        } else {
            includeList = ((YangSubModule) root).getIncludeList();
        }

        for (YangInclude included : includeList) {
            tempNode = parseData(included.getIncludedNode());
            if (tempNode != null) {
                return tempNode;
            }
        }
        return null;
    }

    /**
     * Process linking using include list.
     *
     * @param root         root node
     * @param tempPathName temporary path node name
     * @return linked target node
     */
    private YangNode getIncludedNode(YangNode root, String tempPathName) {

        List<YangInclude> includeList;

        if (root instanceof YangModule) {
            includeList = ((YangModule) root).getIncludeList();
        } else {
            includeList = ((YangSubModule) root).getIncludeList();
        }

        for (YangInclude included : includeList) {
            if (verifyChildNode(included.getIncludedNode(), tempPathName)) {
                return included.getIncludedNode();
            }
        }

        return null;
    }

    /**
     * Verifies for child nodes in sub module.
     *
     * @param node submodule node
     * @param name name of child node
     * @return true if child node found
     */
    private boolean verifyChildNode(YangNode node, String name) {
        node = node.getChild();
        while (node != null) {
            if (node.getName().equals(name)) {
                return true;
            }
            node = node.getNextSibling();
        }
        return false;
    }


    /**
     * Returns augment's node id.
     *
     * @param nodeId   node identifier
     * @param absPaths absolute paths
     * @param root     root node
     * @return augment's node id
     */
    private String getAugmentNodeIdentifier(YangNodeIdentifier nodeId, List<YangAtomicPath> absPaths, YangNode root) {

        Iterator<YangAtomicPath> nodeIdIterator = absPaths.iterator();
        YangAtomicPath tempNodeId;
        StringBuilder builder = new StringBuilder();
        String id;
        PrefixResolverType type;
        while (nodeIdIterator.hasNext()) {
            tempNodeId = nodeIdIterator.next();
            if (!tempNodeId.getNodeIdentifier().equals(nodeId)) {
                type = getPrefixResolverTypes().get(tempNodeId);
                switch (type) {
                    case INTER_TO_INTRA:
                        id = "/" + tempNodeId.getNodeIdentifier().getName();
                        break;
                    case INTRA_TO_INTER:
                        if (!getRootsPrefix(root).equals(tempNodeId.getNodeIdentifier().getPrefix())) {
                            id = "/" + tempNodeId.getNodeIdentifier().getPrefix() + ":" + tempNodeId.getNodeIdentifier()
                                    .getName();
                        } else {
                            id = "/" + tempNodeId.getNodeIdentifier().getName();
                        }
                        break;
                    case INTER_TO_INTER:
                        id = "/" + tempNodeId.getNodeIdentifier().getPrefix() + ":" + tempNodeId.getNodeIdentifier()
                                .getName();
                        break;
                    case NO_PREFIX_CHANGE_FOR_INTRA:
                        id = "/" + tempNodeId.getNodeIdentifier().getName();
                        break;
                    case NO_PREFIX_CHANGE_FOR_INTER:
                        if (!getRootsPrefix(root).equals(tempNodeId.getNodeIdentifier().getPrefix())) {
                            id = "/" + tempNodeId.getNodeIdentifier().getPrefix() + ":" + tempNodeId.getNodeIdentifier()
                                    .getName();
                        } else {
                            id = "/" + tempNodeId.getNodeIdentifier().getName();
                        }
                        break;
                    default:
                        id = "/" + tempNodeId.getNodeIdentifier().getName();
                        break;
                }
                builder.append(id);
            } else {
                return builder.toString();
            }
        }
        return null;
    }

    /**
     * Searches augment node in root node.
     *
     * @param node       root node
     * @param tempNodeId node identifier
     * @return target augment node
     */

    private YangNode searchAugmentNode(YangNode node, String tempNodeId) {
        node = node.getChild();
        while (node != null) {
            if (node instanceof YangAugment) {
                if (node.getName().equals(tempNodeId)) {
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
     * @param index    current index of list
     * @return false if target node found
     */
    private boolean validate(YangNode tempNode, int index) {

        int size = getAbsPaths().size();
        if (tempNode != null && index != size) {
            return true;
        } else if (tempNode != null) {
            return false;
            // this is your target node.
        } else if (index != size) {
            return true;
            // this could be in submodule as well.
        }
        return false;
    }

    /**
     * Searches target node in root node.
     *
     * @param node      root node
     * @param curNodeId YANG node identifier
     * @return linked target node
     */
    private YangNode searchTargetNode(YangNode node, YangNodeIdentifier curNodeId) {

        if (node != null) {
            node = node.getChild();
        }

        while (node != null) {
            if (node instanceof YangInput) {
                if (curNodeId.getName().equalsIgnoreCase(INPUT)) {
                    return node;
                }
            } else if (node instanceof YangOutput) {
                if (curNodeId.getName().equalsIgnoreCase(OUTPUT)) {
                    return node;
                }
            }
            if (node.getName().equals(curNodeId.getName())) {
                return node;
            }
            node = node.getNextSibling();
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

    /**
     * Resolves prefix and provides prefix resolver list.
     *
     * @param absolutePaths absolute paths
     */
    private void parsePrefixResolverList(List<YangAtomicPath> absolutePaths) {
        Iterator<YangAtomicPath> pathIterator = absolutePaths.iterator();
        YangAtomicPath absPath;
        String prePrefix;
        String curPrefix = null;
        while (pathIterator.hasNext()) {
            prePrefix = curPrefix;
            absPath = pathIterator.next();
            curPrefix = absPath.getNodeIdentifier().getPrefix();
            if (curPrefix != null) {
                if (!curPrefix.equals(prePrefix)) {
                    if (prePrefix != null) {
                        addToPrefixResolverList(INTER_TO_INTER, absPath);
                    } else {
                        addToPrefixResolverList(INTRA_TO_INTER, absPath);
                    }
                } else {
                    addToPrefixResolverList(NO_PREFIX_CHANGE_FOR_INTER, absPath);
                }
            } else {
                if (prePrefix != null) {
                    addToPrefixResolverList(INTER_TO_INTRA, absPath);
                } else {
                    addToPrefixResolverList(NO_PREFIX_CHANGE_FOR_INTRA, absPath);
                }
            }
        }

    }

}

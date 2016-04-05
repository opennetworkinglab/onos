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

package org.onosproject.yangutils.datamodel;

import java.util.Stack;

import org.onosproject.yangutils.datamodel.exceptions.DataModelException;

/**
 * Represents resolution object which will be resolved by linker.
 */
public class YangResolutionInfo<T> {

    // Prefix associated with the linking.
    private String prefix;

    // Parsable node for which resolution is to be performed.
    private T entityToResolve;

    // Holder of the YANG construct for which resolution has to be carried out.
    private YangNode holderOfEntityToResolve;

    // Error Line number.
    private int lineNumber;

    // Error character position.
    private int charPosition;

    // Status of resolution.
    private boolean isResolved;

    /*
     * Stack for type/uses is maintained for hierarchical references, this is
     * used during resolution.
     */
    private Stack<T> partialResolvedStack;

    // Flag to indicate whether more references are detected.
    private boolean isMoreReferenceDetected;

    // Module/Sub-module prefix.
    private String resolutionInfoRootNodePrefix;

    /**
     * Create a resolution information object.
     */
    private YangResolutionInfo() {

    }

    /**
     * Creates a resolution information object with all the inputs.
     *
     * @param dataNode current parsable data node
     * @param resolutionType type of resolution whether grouping/typedef
     * @param holderNode parent YANG node
     * @param prefix imported module prefix
     * @param lineNumber error line number
     * @param charPositionInLine error character position in line
     */
    public YangResolutionInfo(T dataNode, ResolutionType resolutionType,
            YangNode holderNode, String prefix, int lineNumber,
            int charPositionInLine) {
        this.setHolderOfEntityToResolve(holderNode);
        this.setEntityToResolve(dataNode);
        this.setPrefix(prefix);
        this.setLineNumber(lineNumber);
        this.setCharPosition(charPositionInLine);
        setPartialResolvedStack(new Stack<T>());
    }

    /**
     * Creates a resolution information object with all the inputs except prefix.
     *
     * @param dataNode current parsable data node
     * @param resolutionType type of resolution whether grouping/typedef
     * @param holderNode parent YANG node
     * @param lineNumber error line number
     * @param charPositionInLine error character position in line
     */
    public YangResolutionInfo(T dataNode, ResolutionType resolutionType,
            YangNode holderNode, int lineNumber,
            int charPositionInLine) {
        this.setHolderOfEntityToResolve(holderNode);
        this.setEntityToResolve(dataNode);
        this.setLineNumber(lineNumber);
        this.setCharPosition(charPositionInLine);
    }

    /**
     * Resolve linking with all the ancestors node for a resolution info.
     *
     * @param resolutionInfoNodePrefix module/sub-module prefix
     * @throws DataModelException DataModelException a violation of data model rules
     */
    public void resolveLinkingForResolutionInfo(String resolutionInfoNodePrefix) throws DataModelException {

        this.resolutionInfoRootNodePrefix = resolutionInfoNodePrefix;

        // Current node to resolve, it can be a YANG type or YANG uses.
        T entityToResolve = getEntityToResolve();

        // Check if linking is already done
        if (entityToResolve instanceof Resolvable) {
            Resolvable resolvable = (Resolvable) entityToResolve;
            if (resolvable.getResolvableStatus() == ResolvableStatus.RESOLVED ||
                    resolvable.getResolvableStatus() == ResolvableStatus.PARTIALLY_RESOLVED) {
                return;
            }
        } else {
            throw new DataModelException("Data Model Exception: Entity to resolved is other than type/uses");
        }

        // Push the initial YANG type to the stack.
        getPartialResolvedStack().push(entityToResolve);

        // Get holder of entity to resolve
        YangNode curNode = getHolderOfEntityToResolve();

        resolveLinkingWithAncestors(curNode);
    }

    /**
     * Resolves linking with ancestors.
     *
     * @param curNode current node for which ancestors to be checked
     * @throws DataModelException a violation of data model rules
     */
    private void resolveLinkingWithAncestors(YangNode curNode) throws DataModelException {

        while (curNode != null) {
            YangNode node = curNode.getChild();
            if (resolveLinkingForNodesChildAndSibling(node, curNode)) {
                return;
            }
            curNode = curNode.getParent();
        }

        // If curNode is null, it indicates an error condition in YANG file.
        DataModelException dataModelException = new DataModelException("YANG file error: Unable to find base " +
                "typedef/grouping for given type/uses");
        dataModelException.setLine(getLineNumber());
        dataModelException.setCharPosition(getCharPosition());
        throw dataModelException;
    }

    /**
     * Resolves linking for a node child and siblings.
     *
     * @param node current node
     * @param parentNode parent node of current node
     * @return flag to indicate whether resolution is done
     * @throws DataModelException
     */
    private boolean resolveLinkingForNodesChildAndSibling(YangNode node, YangNode parentNode)
            throws DataModelException {
        while ((node != null)) {
            isMoreReferenceDetected = false;
            // Check if node is of type, typedef or grouping
            if (isNodeOfResolveType(node)) {
                if (resolveLinkingForNode(node, parentNode)) {
                    return true;
                }
            }
            if (isMoreReferenceDetected) {
                /*
                 * If more reference are present, tree traversal must start from
                 * first child again, to check the availability of
                 * typedef/grouping.
                 */
                node = parentNode.getChild();
            } else {
                node = node.getNextSibling();
            }
        }
        return false;
    }

    /**
     * Resolves linking for a node.
     *
     * @param node current node
     * @param parentNode parent node of current node
     * @return flag to indicate whether resolution is done
     * @throws DataModelException a violation of data model rules
     */
    private boolean resolveLinkingForNode(YangNode node, YangNode parentNode) throws DataModelException {

        /*
         * Check if name of node name matches with the entity name under
         * resolution.
         */
        if (isNodeNameSameAsResolutionInfoName(node)) {
            // Adds reference of entity to the node under resolution.
            addReferredEntityLink(node);
            // Check if referred entity has further reference to uses/type.
            if (!(isMoreReferencePresent(node))) {
                // Resolve all the entities in stack.
                resolveStackAndAddToStack(node);
                return true;
            } else {
                // Adds referred type/uses to the stack.
                addToPartialResolvedStack(node);
                /*
                 * Check whether referred type is resolved, partially resolved
                 * or unresolved.
                 */
                if (isReferenceFullyResolved()) {
                    // Resolve the stack which is complete.
                    resolveCompleteStack();
                    return true;
                } else if (isReferencePartiallyResolved()) {
                    /*
                     * Update the resolution type to partially resolved for all
                     * type/uses in stack
                     */
                    updateResolutionTypeToPartial();
                    return true;
                } else {
                    /*
                     * Check if prefix is present to find that the derived
                     * reference is for intra file or inter file, if it's
                     * inter-file return and stop further processing.
                     */
                    if (isExternalPrefixPresent(node)) {
                        /*
                         * Update the resolution type to partially resolved for
                         * all type/uses in stack
                         */
                        updateResolutionTypeToPartial();
                        return true;
                    } else {
                        /*
                         * If prefix is not present it indicates intra-file
                         * dependency in this case set the node back to first
                         * child, as referred entity may appear in any order and
                         * continue with the resolution.
                         */
                        isMoreReferenceDetected = true;
                        return false;
                    }
                }
            }
        }
        return false;
    }

    /**
     * Update resolution type to partial for all type/uses in stack.
     *
     * @throws DataModelException a violation of data model rules
     */
    private void updateResolutionTypeToPartial() throws DataModelException {
        // For all entries in stack calls for the resolution in type/uses.
        for (T entity : getPartialResolvedStack()) {
            if (!(entity instanceof Resolvable)) {
                throw new DataModelException("Data Model Exception: Entity to resolved is other than type/uses");
            }
            if (((Resolvable) entity).getResolvableStatus() == ResolvableStatus.UNRESOLVED) {
                // Sets the resolution status in inside the type/uses.
                ((Resolvable) entity).setResolvableStatus(ResolvableStatus.PARTIALLY_RESOLVED);
            }
        }
    }

    /**
     * Adds referred type/uses to the stack and resolve the stack.
     *
     * @param node typedef/grouping node
     * @throws DataModelException a violation of data model rules
     */
    private void resolveStackAndAddToStack(YangNode node) throws DataModelException {
        if (getEntityToResolve() instanceof YangType) {
            // Adds to the stack only for YANG typedef.
            getPartialResolvedStack().push((T) ((YangTypeDef) node).getDataType());
        }
        // Don't add to stack in case of YANG grouping.

        // Resolve the complete stack.
        resolveCompleteStack();
    }

    /**
     * Check if the referred type/uses is partially resolved.
     *
     * @return true if reference is partially resolved, otherwise false
     */
    private boolean isReferencePartiallyResolved() {
        if (getPartialResolvedStack().peek() instanceof YangType) {
            /*
             * Checks if type is partially resolved.
             */
            if (((YangType) getPartialResolvedStack().peek())
                    .getResolvableStatus() == ResolvableStatus.PARTIALLY_RESOLVED) {
                return true;
            }
        } else if (getPartialResolvedStack().peek() instanceof YangUses) {
            if (((YangUses) getPartialResolvedStack().peek())
                    .getResolvableStatus() == ResolvableStatus.PARTIALLY_RESOLVED) {
                return true;
            }
        }
        return false;
    }

    /**
     * Check if the referred type/uses is resolved.
     *
     * @return true if reference is resolved, otherwise false
     */
    private boolean isReferenceFullyResolved() {
        if (getPartialResolvedStack().peek() instanceof YangType) {
            /*
             * Checks if type is partially resolved.
             */
            if (((YangType) getPartialResolvedStack().peek()).getResolvableStatus() == ResolvableStatus.RESOLVED) {
                return true;
            }
        } else if (getPartialResolvedStack().peek() instanceof YangUses) {
            if (((YangUses) getPartialResolvedStack().peek()).getResolvableStatus() == ResolvableStatus.RESOLVED) {
                return true;
            }
        }
        return false;
    }

    /**
     * Check if node is of resolve type i.e. of type typedef or grouping.
     *
     * @param node typedef/grouping node
     * @return true if node is of resolve type otherwise false
     * @throws DataModelException a violation of data model rules
     */
    private boolean isNodeOfResolveType(YangNode node) throws DataModelException {
        if (getPartialResolvedStack().peek() instanceof YangType && entityToResolve instanceof YangType) {
            if (node instanceof YangTypeDef) {
                return true;
            }
        } else if (getPartialResolvedStack().peek() instanceof YangUses && entityToResolve instanceof YangUses) {
            if (node instanceof YangGrouping) {
                return true;
            }
        } else {
            throw new DataModelException("Data Model Exception: Entity to resolved is other than type/uses");
        }
        return false;
    }

    /**
     * Check if node name is same as name in resolution info, i.e. name of
     * typedef/grouping is same as name of type/uses.
     *
     * @param node typedef/grouping node
     * @return true if node name is same as name in resolution info, otherwise
     * false
     * @throws DataModelException a violation of data model rules
     */
    private boolean isNodeNameSameAsResolutionInfoName(YangNode node) throws DataModelException {
        if (getPartialResolvedStack().peek() instanceof YangType) {
            if (node.getName().equals(((YangType<?>) getPartialResolvedStack().peek()).getDataTypeName())) {
                return true;
            }
        } else if (getPartialResolvedStack().peek() instanceof YangUses) {
            if (node.getName().equals(((YangUses) getPartialResolvedStack().peek()).getName())) {
                return true;
            }
        } else {
            throw new DataModelException("Data Model Exception: Entity to resolved is other than type/uses");
        }
        return false;
    }

    /**
     * Adds reference of grouping/typedef in uses/type.
     *
     * @param node grouping/typedef node
     * @throws DataModelException a violation of data model rules
     */
    private void addReferredEntityLink(YangNode node) throws DataModelException {
        if (getPartialResolvedStack().peek() instanceof YangType) {
            YangDerivedInfo<?> derivedInfo = (YangDerivedInfo<?>) ((YangType<?>) getPartialResolvedStack().peek())
                    .getDataTypeExtendedInfo();
            derivedInfo.setReferredTypeDef((YangTypeDef) node);
        } else if (getPartialResolvedStack().peek() instanceof YangUses) {
            ((YangUses) getPartialResolvedStack().peek()).setRefGroup((YangGrouping) node);
        } else {
            throw new DataModelException("Data Model Exception: Entity to resolved is other than type/uses");
        }
    }

    /**
     * Checks if typedef/grouping has further reference to type/typedef.
     *
     * @param node grouping/typedef node
     * @return true if referred entity is resolved, otherwise false
     * @throws DataModelException a violation of data model rules
     */
    private boolean isMoreReferencePresent(YangNode node) throws DataModelException {
        if (getEntityToResolve() instanceof YangType) {
            /*
             * Checks if typedef type is built-in type
             */
            if ((((YangTypeDef) node).getDataType().getDataType() != YangDataTypes.DERIVED)) {
                return false;
            }
        } else if (getEntityToResolve() instanceof YangUses) {
            /*
             * Search if the grouping has any uses child, if so return false,
             * else return true.
             */
            if (getUsesInGrouping(node) == null) {
                return false;
            }
        } else {
            throw new DataModelException("Data Model Exception: Entity to resolved is other than type/uses");
        }
        return true;
    }

    /**
     * Return if there is any uses in grouping.
     *
     * @param node grouping/typedef node
     * @return if there is any uses in grouping, otherwise return null
     */
    private YangUses getUsesInGrouping(YangNode node) {
        YangNode curNode = ((YangGrouping) node).getChild();
        while (curNode != null) {
            if (curNode instanceof YangUses) {
                break;
            }
            curNode = curNode.getNextSibling();
        }
        return (YangUses) curNode;
    }

    /**
     * Resolve the complete stack.
     *
     * @throws DataModelException a violation of data model rules
     */
    private void resolveCompleteStack() throws DataModelException {
        // For all entries in stack calls for the resolution in type/uses.
        for (T entity : getPartialResolvedStack()) {
            if (!(entity instanceof Resolvable)) {
                throw new DataModelException("Data Model Exception: Entity to resolved is other than type/uses");
            }
            ((Resolvable) entity).resolve();
            // Sets the resolution status in inside the type/uses.
            ((Resolvable) entity).setResolvableStatus(ResolvableStatus.RESOLVED);
        }
        /*
         * Sets the resolution status in resolution info present in resolution
         * list.
         */
        setIsResolved(true);
    }

    /**
     * Adds to partial resolved stack.
     *
     * @param node grouping/typedef node
     * @throws DataModelException a violation of data model rules
     */
    private void addToPartialResolvedStack(YangNode node) throws DataModelException {
        if (getPartialResolvedStack().peek() instanceof YangType) {
            // Adds to the stack only for YANG typedef.
            getPartialResolvedStack().push((T) ((YangTypeDef) node).getDataType());
        } else if (getPartialResolvedStack().peek() instanceof YangUses) {
            getPartialResolvedStack().push((T) getUsesInGrouping(node));
        } else {
            throw new DataModelException("Data Model Exception: Entity to resolved is other than type/uses");
        }
    }

    /**
     * Check if prefix is associated with type/uses.
     *
     * @param node typedef/grouping node
     * @return true if prefix is present, otherwise false
     * @throws DataModelException a violation of data model rules
     */
    private boolean isExternalPrefixPresent(YangNode node) throws DataModelException {
        if (getEntityToResolve() instanceof YangType) {
            if (((YangTypeDef) node).getDataType().getPrefix() != null &&
                    (!((YangTypeDef) node).getDataType().getPrefix().equals(resolutionInfoRootNodePrefix))) {
                return true;
            }
        } else if (getEntityToResolve() instanceof YangUses) {
            if (getUsesInGrouping(node).getPrefix() != null) {
                return true;
            }
        } else {
            throw new DataModelException("Data Model Exception: Entity to resolved is other than type/uses");
        }
        return false;
    }

    /**
     * Returns prefix of imported module.
     *
     * @return prefix of imported module
     */
    public String getPrefix() {
        return prefix;
    }

    /**
     * Sets prefix of imported module.
     *
     * @param prefix of imported module
     */
    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    /**
     * Returns parsable entity which is to be resolved.
     *
     * @return parsable entity which is to be resolved
     */
    public T getEntityToResolve() {
        return entityToResolve;
    }

    /**
     * Sets parsable entity to be resolved.
     *
     * @param entityToResolve YANG entity to be resolved
     */
    public void setEntityToResolve(T entityToResolve) {
        this.entityToResolve = entityToResolve;
    }

    /**
     * Returns parent YANG node holder for the entity to be resolved.
     *
     * @return parent YANG node holder
     */
    public YangNode getHolderOfEntityToResolve() {
        return holderOfEntityToResolve;
    }

    /**
     * Sets parent YANG node holder for the entity to be resolved.
     *
     * @param holderOfEntityToResolve parent YANG node holder
     */
    public void setHolderOfEntityToResolve(YangNode holderOfEntityToResolve) {
        this.holderOfEntityToResolve = holderOfEntityToResolve;
    }

    /**
     * Returns error position.
     *
     * @return error position
     */
    public int getCharPosition() {
        return charPosition;
    }

    /**
     * Sets error position.
     *
     * @param charPosition position of error
     */
    public void setCharPosition(int charPosition) {
        this.charPosition = charPosition;
    }

    /**
     * Returns error character position in line.
     *
     * @return error character position in line
     */
    public int getLineNumber() {
        return lineNumber;
    }

    /**
     * Sets error character position in line.
     *
     * @param lineNumber error character position in line
     */
    public void setLineNumber(int lineNumber) {
        this.lineNumber = lineNumber;
    }

    /**
     * Returns status of resolution.
     *
     * @return resolution status
     */
    public boolean isResolved() {
        return isResolved;
    }

    /**
     * Sets status of resolution.
     *
     * @param isResolved resolution status
     */
    public void setIsResolved(boolean isResolved) {
        this.isResolved = isResolved;
    }

    /**
     * Returns stack of YANG type with partially resolved YANG construct hierarchy.
     *
     * @return partial resolved YANG construct stack
     */
    public Stack<T> getPartialResolvedStack() {
        return partialResolvedStack;
    }

    /**
     * Sets stack of YANG type with partially resolved YANG construct hierarchy.
     *
     * @param partialResolvedStack partial resolved YANG construct stack
     */
    public void setPartialResolvedStack(Stack<T> partialResolvedStack) {
        this.partialResolvedStack = partialResolvedStack;
    }
}

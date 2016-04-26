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

import static org.onosproject.yangutils.datamodel.ResolvableStatus.INTRA_FILE_RESOLVED;
import static org.onosproject.yangutils.datamodel.ResolvableStatus.LINKED;
import static org.onosproject.yangutils.datamodel.ResolvableStatus.RESOLVED;
import static org.onosproject.yangutils.datamodel.ResolvableStatus.UNRESOLVED;

/**
 * Represents resolution object which will be resolved by linker.
 *
 * @param <T> type of resolution entity uses / type
 */
public class YangResolutionInfo<T> {

    /**
     * Information about the entity that needs to be resolved.
     */
    private YangEntityToResolveInfo<T> entityToResolveInfo;

    // Error Line number.
    private int lineNumber;

    // Error character position.
    private int charPosition;

    /*
     * Stack for type/uses is maintained for hierarchical references, this is
     * used during resolution.
     */
    private Stack<YangEntityToResolveInfo<T>> partialResolvedStack;

    // Module/Sub-module prefix.
    private String resolutionInfoRootNodePrefix;

    /**
     * It is private to ensure the overloaded method be invoked to create an
     * object.
     */
    @SuppressWarnings("unused")
    private YangResolutionInfo() {

    }

    /**
     * Creates a resolution information object with all the inputs.
     *
     * @param dataNode current parsable data node
     * @param holderNode parent YANG node
     * @param lineNumber error line number
     * @param charPositionInLine error character position in line
     */
    public YangResolutionInfo(T dataNode, YangNode holderNode, int lineNumber, int charPositionInLine) {
        setEntityToResolveInfo(new YangEntityToResolveInfo<T>());
        getEntityToResolveInfo().setEntityToResolve(dataNode);
        getEntityToResolveInfo().setHolderOfEntityToResolve(holderNode);
        this.setLineNumber(lineNumber);
        this.setCharPosition(charPositionInLine);
        setPartialResolvedStack(new Stack<YangEntityToResolveInfo<T>>());
    }

    /**
     * Resolve linking with all the ancestors node for a resolution info.
     *
     * @param resolutionInfoNodePrefix module/sub-module prefix
     * @throws DataModelException DataModelException a violation of data model
     *                            rules
     */
    public void resolveLinkingForResolutionInfo(String resolutionInfoNodePrefix)
            throws DataModelException {

        this.resolutionInfoRootNodePrefix = resolutionInfoNodePrefix;

        // Current node to resolve, it can be a YANG type or YANG uses.
        T entityToResolve = getEntityToResolveInfo().getEntityToResolve();

        // Check if linking is already done
        if (entityToResolve instanceof Resolvable) {
            Resolvable resolvable = (Resolvable) entityToResolve;
            if (resolvable.getResolvableStatus() == RESOLVED) {
                /**
                 * entity is already resolved, so nothing to do
                 */
                return;
            }
        } else {
            throw new DataModelException("Data Model Exception: Entity to resolved is other than type/uses");
        }

        // Push the initial entity to resolve in stack.
        addInPartialResolvedStack(getEntityToResolveInfo());

        linkAndResolvePartialResolvedStack();
    }

    /**
     * Resolves linking with ancestors.
     *
     * @throws DataModelException a violation of data model rules
     */
    private void linkAndResolvePartialResolvedStack()
            throws DataModelException {

        while (getPartialResolvedStack().size() != 0) {

            // Current node to resolve, it can be a YANG type or YANG uses.
            T entityToResolve = getCurrentEntityToResolveFromStack();
            // Check if linking is already done
            if (entityToResolve instanceof Resolvable) {

                Resolvable resolvable = (Resolvable) entityToResolve;
                switch (resolvable.getResolvableStatus()) {
                    case RESOLVED: {
                        /*
                         * If the entity is already resolved in the stack, then
                         * pop it and continue with the remaining stack elements
                         * to resolve
                         */
                        getPartialResolvedStack().pop();
                        break;
                    }

                    case LINKED: {
                        /*
                         * If the top of the stack is already linked then
                         * resolve the references and pop the entity and
                         * continue with remaining stack elements to resolve
                         */
                        resolveTopOfStack();
                        getPartialResolvedStack().pop();
                        break;
                    }

                    case INTRA_FILE_RESOLVED: {
                        /*
                         * TODO: this needs to be deleted, when inter-file
                         * resolution is implmeneted
                         */
                        getPartialResolvedStack().pop();
                        break;
                    }

                    case UNRESOLVED: {
                        linkTopOfStackReferenceUpdateStack();

                        if (resolvable.getResolvableStatus() == UNRESOLVED) {
                            // If current entity is still not resolved, then linking/resolution has failed.
                            DataModelException dataModelException =
                                    new DataModelException("YANG file error: Unable to find base "
                                            + "typedef/grouping for given type/uses");
                            dataModelException.setLine(getLineNumber());
                            dataModelException.setCharPosition(getCharPosition());
                            throw dataModelException;
                        }
                        break;
                    }
                    default: {
                        throw new DataModelException("Data Model Exception: Unsupported, linker state");
                    }

                }

            } else {
                throw new DataModelException("Data Model Exception: Entity to resolved is other than type/uses");
            }

        }

    }

    /**
     * Resolve the current entity in the stack.
     */
    private void resolveTopOfStack()
            throws DataModelException {
        ((Resolvable) getCurrentEntityToResolveFromStack()).resolve();

        if (((Resolvable) getCurrentEntityToResolveFromStack()).getResolvableStatus()
                != INTRA_FILE_RESOLVED) {
            // Sets the resolution status in inside the type/uses.
            ((Resolvable) getCurrentEntityToResolveFromStack()).setResolvableStatus(RESOLVED);
        }
    }

    /**
     * Resolves linking for a node child and siblings.
     *
     * @throws DataModelException data model error
     */
    private void linkTopOfStackReferenceUpdateStack()
            throws DataModelException {

        if (!isSelfFileReference()) {
            /*
             * TODO: use mojo utilities to load the referred module/sub-module
             * and get the reference to the corresponding referred entity
             */

            ((Resolvable) getCurrentEntityToResolveFromStack()).setResolvableStatus(INTRA_FILE_RESOLVED);
            return;
        }

        /**
         * Try to resolve the top of the stack and update partial resolved stack
         * if there is recursive references
         */
        YangNode potentialAncestorWithReferredNode = getPartialResolvedStack().peek()
                .getHolderOfEntityToResolve();

        /**
         * Traverse up in the ancestor tree to check if the referred node is
         * defined
         */
        while (potentialAncestorWithReferredNode != null) {

            /**
             * Check for the referred node defined in a ancestor scope
             */
            YangNode potentialReferredNode = potentialAncestorWithReferredNode.getChild();
            if (isReferredNodeInSiblingListProcessed(potentialReferredNode)) {
                return;
            }

            potentialAncestorWithReferredNode = potentialAncestorWithReferredNode.getParent();
        }
    }

    /**
     * Check if the reference in self file or in external file.
     *
     * @return true if self file reference, false otherwise
     * @throws DataModelException a violation of data model rules
     */
    private boolean isSelfFileReference()
            throws DataModelException {
        String prefix;
        if (getCurrentEntityToResolveFromStack() instanceof YangType) {
            prefix = ((YangType<?>) getCurrentEntityToResolveFromStack()).getPrefix();
        } else if (getCurrentEntityToResolveFromStack() instanceof YangUses) {
            prefix = ((YangUses) getCurrentEntityToResolveFromStack()).getPrefix();
        } else {
            throw new DataModelException("Data Model Exception: Entity to resolved is other than type/uses");
        }

        if (prefix == null) {
            return true;
        }
        return prefix.contentEquals(resolutionInfoRootNodePrefix);

    }

    /**
     * Check for the referred node defined in a ancestor scope.
     *
     * @param potentialReferredNode potential referred node
     * @return status of resolution and updating the partial resolved stack with
     * the any recursive references
     * @throws DataModelException data model errors
     */
    private boolean isReferredNodeInSiblingListProcessed(YangNode potentialReferredNode)
            throws DataModelException {
        while (potentialReferredNode != null) {

            // Check if the potential referred node is the actual referred node
            if (isReferredNode(potentialReferredNode)) {

                // Adds reference link of entity to the node under resolution.
                addReferredEntityLink(potentialReferredNode);

                /**
                 * resolve the reference and update the partial resolution stack
                 * with any further recursive references
                 */
                addUnresolvedRecursiveReferenceToStack(potentialReferredNode);

                /*
                 * return true, since the reference is linked and any recursive
                 * unresolved references is added to the stack
                 */
                return true;
            }

            potentialReferredNode = potentialReferredNode.getNextSibling();
        }
        return false;
    }

    /**
     * Check if the potential referred node is the actual referred node.
     *
     * @param potentialReferredNode typedef/grouping node
     * @return true if node is of resolve type otherwise false
     * @throws DataModelException a violation of data model rules
     */
    private boolean isReferredNode(YangNode potentialReferredNode)
            throws DataModelException {
        if (getCurrentEntityToResolveFromStack() instanceof YangType) {
            if (potentialReferredNode instanceof YangTypeDef) {
                /*
                 * Check if name of node name matches with the entity being
                 * resolved
                 */
                return isNodeNameSameAsResolutionInfoName(potentialReferredNode);
            }
        } else if (getCurrentEntityToResolveFromStack() instanceof YangUses) {
            if (potentialReferredNode instanceof YangGrouping) {
                /*
                 * Check if name of node name matches with the entity being
                 * resolved
                 */
                return isNodeNameSameAsResolutionInfoName(potentialReferredNode);
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

    private boolean isNodeNameSameAsResolutionInfoName(YangNode node)
            throws DataModelException {
        if (getCurrentEntityToResolveFromStack() instanceof YangType) {
            if (node.getName().contentEquals(
                    ((YangType<?>) getCurrentEntityToResolveFromStack())
                            .getDataTypeName())) {
                return true;
            }
        } else if (getCurrentEntityToResolveFromStack() instanceof YangUses) {
            if (node.getName().contentEquals(
                    ((YangUses) getCurrentEntityToResolveFromStack()).getName())) {
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
     * @param referredNode grouping/typedef node being referred
     * @throws DataModelException a violation of data model rules
     */
    private void addReferredEntityLink(YangNode referredNode)
            throws DataModelException {
        if (getCurrentEntityToResolveFromStack() instanceof YangType) {
            YangDerivedInfo<?> derivedInfo = (YangDerivedInfo<?>)
                    ((YangType<?>) getCurrentEntityToResolveFromStack()).getDataTypeExtendedInfo();
            derivedInfo.setReferredTypeDef((YangTypeDef) referredNode);
        } else if (getCurrentEntityToResolveFromStack() instanceof YangUses) {
            ((YangUses) getCurrentEntityToResolveFromStack())
                    .setRefGroup((YangGrouping) referredNode);
        } else {
            throw new DataModelException("Data Model Exception: Entity to resolved is other than type/uses");
        }

        // Sets the resolution status in inside the type/uses.
        ((Resolvable) getCurrentEntityToResolveFromStack()).setResolvableStatus(LINKED);
    }

    /**
     * Checks if type/grouping has further reference to typedef/ unresolved
     * uses. Add it to the partial resolve stack and return the status of
     * addition to stack.
     *
     * @param referredNode grouping/typedef node
     * @throws DataModelException a violation of data model rules
     */
    private void addUnresolvedRecursiveReferenceToStack(YangNode referredNode)
            throws DataModelException {
        if (getCurrentEntityToResolveFromStack() instanceof YangType) {
            /*
             * Checks if typedef type is derived
             */
            if (((YangTypeDef) referredNode).getTypeDefBaseType().getDataType()
                    == YangDataTypes.DERIVED) {

                YangEntityToResolveInfo<YangType<?>> unResolvedEntityInfo = new YangEntityToResolveInfo<YangType<?>>();
                unResolvedEntityInfo.setEntityToResolve(((YangTypeDef) referredNode)
                        .getTypeDefBaseType());
                unResolvedEntityInfo.setHolderOfEntityToResolve(referredNode);
                addInPartialResolvedStack((YangEntityToResolveInfo<T>) unResolvedEntityInfo);
            }

        } else if (getCurrentEntityToResolveFromStack() instanceof YangUses) {
            /*
             * Search if the grouping has any un resolved uses child, if so
             * return true, else return false.
             */
            addUnResolvedUsesToStack(referredNode);
        } else {
            throw new DataModelException("Data Model Exception: Entity to resolved is other than type/uses");
        }
    }

    /**
     * Return if there is any unresolved uses in grouping.
     *
     * @param node grouping/typedef node
     */
    private void addUnResolvedUsesToStack(YangNode node) {

        /**
         * Search the grouping node's children for presence of uses node.
         */
        YangNode curNode = node.getChild();
        while (curNode != null) {
            if (curNode instanceof YangUses) {
                ResolvableStatus curResolveStatus = ((Resolvable) curNode).getResolvableStatus();
                if (curResolveStatus == UNRESOLVED) {
                    /**
                     * The current uses is not resolved, add it to partial
                     * resolved stack
                     */
                    YangEntityToResolveInfo<YangUses> unResolvedEntityInfo = new YangEntityToResolveInfo<YangUses>();
                    unResolvedEntityInfo.setEntityToResolve((YangUses) curNode);
                    unResolvedEntityInfo.setHolderOfEntityToResolve(node);
                    addInPartialResolvedStack((YangEntityToResolveInfo<T>) unResolvedEntityInfo);

                }
            }
            curNode = curNode.getNextSibling();
        }
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
     * Returns stack of YANG type with partially resolved YANG construct
     * hierarchy.
     *
     * @return partial resolved YANG construct stack
     */
    private Stack<YangEntityToResolveInfo<T>> getPartialResolvedStack() {
        return partialResolvedStack;
    }

    /**
     * Sets stack of YANG type with partially resolved YANG construct hierarchy.
     *
     * @param partialResolvedStack partial resolved YANG construct stack
     */
    private void setPartialResolvedStack(Stack<YangEntityToResolveInfo<T>> partialResolvedStack) {
        this.partialResolvedStack = partialResolvedStack;
    }

    /**
     * Sets stack of YANG type with partially resolved YANG construct hierarchy.
     *
     * @param partialResolvedInfo partial resolved YANG construct stack
     */
    private void addInPartialResolvedStack(YangEntityToResolveInfo<T> partialResolvedInfo) {
        getPartialResolvedStack().push(partialResolvedInfo);
    }

    /**
     * Retrieves the next entity in the stack that needs to be resolved. It is
     * assumed that the caller ensures that the stack is not empty.
     *
     * @return next entity in the stack that needs to be resolved
     */
    private T getCurrentEntityToResolveFromStack() {
        return getPartialResolvedStack().peek().getEntityToResolve();
    }

    /**
     * Retrieves information about the entity that needs to be resolved.
     *
     * @return information about the entity that needs to be resolved
     */
    public YangEntityToResolveInfo<T> getEntityToResolveInfo() {
        return entityToResolveInfo;
    }

    /**
     * Sets information about the entity that needs to be resolved.
     *
     * @param entityToResolveInfo information about the entity that needs to be
     * resolved
     */
    public void setEntityToResolveInfo(YangEntityToResolveInfo<T> entityToResolveInfo) {
        this.entityToResolveInfo = entityToResolveInfo;
    }
}

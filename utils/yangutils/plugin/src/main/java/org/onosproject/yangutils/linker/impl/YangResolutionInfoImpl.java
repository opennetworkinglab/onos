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

import java.io.Serializable;
import java.util.Stack;

import org.onosproject.yangutils.datamodel.Resolvable;
import org.onosproject.yangutils.datamodel.YangDataTypes;
import org.onosproject.yangutils.datamodel.YangDerivedInfo;
import org.onosproject.yangutils.datamodel.YangGrouping;
import org.onosproject.yangutils.datamodel.YangImport;
import org.onosproject.yangutils.datamodel.YangInclude;
import org.onosproject.yangutils.datamodel.YangNode;
import org.onosproject.yangutils.datamodel.YangReferenceResolver;
import org.onosproject.yangutils.datamodel.YangResolutionInfo;
import org.onosproject.yangutils.datamodel.YangType;
import org.onosproject.yangutils.datamodel.YangTypeDef;
import org.onosproject.yangutils.datamodel.YangUses;
import org.onosproject.yangutils.datamodel.exceptions.DataModelException;
import org.onosproject.yangutils.datamodel.utils.ResolvableStatus;
import org.onosproject.yangutils.linker.YangLinkingPhase;

import static org.onosproject.yangutils.datamodel.utils.ResolvableStatus.INTER_FILE_LINKED;
import static org.onosproject.yangutils.datamodel.utils.ResolvableStatus.INTRA_FILE_RESOLVED;
import static org.onosproject.yangutils.datamodel.utils.ResolvableStatus.LINKED;
import static org.onosproject.yangutils.datamodel.utils.ResolvableStatus.RESOLVED;
import static org.onosproject.yangutils.datamodel.utils.ResolvableStatus.UNRESOLVED;
import static org.onosproject.yangutils.linker.YangLinkingPhase.INTER_FILE;
import static org.onosproject.yangutils.linker.YangLinkingPhase.INTRA_FILE;
import static org.onosproject.yangutils.utils.UtilConstants.GROUPING_LINKER_ERROR;
import static org.onosproject.yangutils.utils.UtilConstants.TYPEDEF_LINKER_ERROR;

/**
 * Represents implementation of resolution object which will be resolved by
 * linker.
 *
 * @param <T> type of resolution entity uses / type
 */
public class YangResolutionInfoImpl<T>
        implements YangResolutionInfo<T>, Serializable {

    private static final long serialVersionUID = 806201658L;

    /**
     * Information about the entity that needs to be resolved.
     */
    private YangEntityToResolveInfoImpl<T> entityToResolveInfo;

    /**
     * Error line number.
     */
    private  transient int lineNumber;

    /**
     * Error character position in number.
     */
    private transient int charPosition;

    /**
     * Current module/sub-module reference, will be used in inter-file/
     * inter-jar scenario to get the import/include list.
     */
    private transient YangReferenceResolver curReferenceResolver;

    /**
     * Stack for type/uses is maintained for hierarchical references, this is
     * used during resolution.
     */
    private Stack<YangEntityToResolveInfoImpl<T>> partialResolvedStack;

    /**
     * It is private to ensure the overloaded method be invoked to create an
     * object.
     */
    @SuppressWarnings("unused")
    private YangResolutionInfoImpl() {

    }

    /**
     * Creates a resolution information object with all the inputs.
     *
     * @param dataNode           current parsable data node
     * @param holderNode         parent YANG node
     * @param lineNumber         error line number
     * @param charPositionInLine error character position in line
     */
    public YangResolutionInfoImpl(T dataNode, YangNode holderNode, int lineNumber, int charPositionInLine) {
        setEntityToResolveInfo(new YangEntityToResolveInfoImpl<>());
        getEntityToResolveInfo().setEntityToResolve(dataNode);
        getEntityToResolveInfo().setHolderOfEntityToResolve(holderNode);
        this.setLineNumber(lineNumber);
        this.setCharPosition(charPositionInLine);
        setPartialResolvedStack(new Stack<>());
    }

    @Override
    public void resolveLinkingForResolutionInfo(YangReferenceResolver dataModelRootNode)
            throws DataModelException {

        setCurReferenceResolver(dataModelRootNode);

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
                     * If the entity is already resolved in the stack, then pop
                     * it and continue with the remaining stack elements to
                     * resolve
                     */
                    getPartialResolvedStack().pop();
                    break;
                }

                case LINKED: {
                    /*
                     * If the top of the stack is already linked then resolve
                     * the references and pop the entity and continue with
                     * remaining stack elements to resolve.
                     */
                    resolveTopOfStack(INTRA_FILE);
                    getPartialResolvedStack().pop();
                    break;
                }

                case INTRA_FILE_RESOLVED: {
                    /*
                     * Pop the top of the stack.
                     */
                    getPartialResolvedStack().pop();
                    break;
                }

                case UNRESOLVED: {
                    linkTopOfStackReferenceUpdateStack();

                    if (resolvable.getResolvableStatus() == UNRESOLVED) {
                        // If current entity is still not resolved, then
                        // linking/resolution has failed.
                        String errorInfo;
                        if (resolvable instanceof YangType) {
                            errorInfo = TYPEDEF_LINKER_ERROR;
                        } else {
                            errorInfo = GROUPING_LINKER_ERROR;
                        }
                        DataModelException dataModelException = new DataModelException(errorInfo);
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
     * Resolves the current entity in the stack.
     */
    private void resolveTopOfStack(YangLinkingPhase linkingPhase)
            throws DataModelException {
        ((Resolvable) getCurrentEntityToResolveFromStack()).resolve();
        if (((Resolvable) getCurrentEntityToResolveFromStack()).getResolvableStatus() != INTRA_FILE_RESOLVED) {
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

        /*
         * Check if self file reference is there, this will not check for the
         * scenario when prefix is not present and type/uses is present in
         * sub-module from include list.
         */
        if (!isCandidateForSelfFileReference()) {
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

        /*
         * In case prefix is not present it's a candidate for inter-file
         * resolution via include list.
         */
        if (getRefPrefix() == null) {
            ((Resolvable) getCurrentEntityToResolveFromStack()).setResolvableStatus(INTRA_FILE_RESOLVED);
        }
    }

    /**
     * Checks if the reference in self file or in external file.
     *
     * @return true if self file reference, false otherwise
     * @throws DataModelException a violation of data model rules
     */
    private boolean isCandidateForSelfFileReference()
            throws DataModelException {
        String prefix = getRefPrefix();
        return prefix == null || prefix.contentEquals(getCurReferenceResolver().getPrefix());
    }

    /**
     * Checks for the referred node defined in a ancestor scope.
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
                addReferredEntityLink(potentialReferredNode, LINKED);

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
     * Checks if the potential referred node is the actual referred node.
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
     * Checks if node name is same as name in resolution info, i.e. name of
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
     * @param linkedStatus linked status if success.
     * @throws DataModelException a violation of data model rules
     */
    private void addReferredEntityLink(YangNode referredNode, ResolvableStatus linkedStatus)
            throws DataModelException {
        if (getCurrentEntityToResolveFromStack() instanceof YangType) {
            YangDerivedInfo<?> derivedInfo = (YangDerivedInfo<?>) ((YangType<?>) getCurrentEntityToResolveFromStack())
                    .getDataTypeExtendedInfo();
            derivedInfo.setReferredTypeDef((YangTypeDef) referredNode);
        } else if (getCurrentEntityToResolveFromStack() instanceof YangUses) {
            ((YangUses) getCurrentEntityToResolveFromStack())
                    .setRefGroup((YangGrouping) referredNode);
        } else {
            throw new DataModelException("Data Model Exception: Entity to resolved is other than type/uses");
        }

        // Sets the resolution status in inside the type/uses.
        ((Resolvable) getCurrentEntityToResolveFromStack()).setResolvableStatus(linkedStatus);
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
            if (((YangTypeDef) referredNode).getTypeDefBaseType().getDataType() == YangDataTypes.DERIVED) {

                YangEntityToResolveInfoImpl<YangType<?>> unResolvedEntityInfo = new YangEntityToResolveInfoImpl<>();
                unResolvedEntityInfo.setEntityToResolve(((YangTypeDef) referredNode)
                        .getTypeDefBaseType());
                unResolvedEntityInfo.setHolderOfEntityToResolve(referredNode);
                addInPartialResolvedStack((YangEntityToResolveInfoImpl<T>) unResolvedEntityInfo);
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
     * Returns if there is any unresolved uses in grouping.
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
                YangEntityToResolveInfoImpl<YangUses> unResolvedEntityInfo = new YangEntityToResolveInfoImpl<>();
                unResolvedEntityInfo.setEntityToResolve((YangUses) curNode);
                unResolvedEntityInfo.setHolderOfEntityToResolve(node);
                addInPartialResolvedStack((YangEntityToResolveInfoImpl<T>) unResolvedEntityInfo);

            }
            curNode = curNode.getNextSibling();
        }
    }

    /**
     * Returns stack of YANG type with partially resolved YANG construct
     * hierarchy.
     *
     * @return partial resolved YANG construct stack
     */
    private Stack<YangEntityToResolveInfoImpl<T>> getPartialResolvedStack() {
        return partialResolvedStack;
    }

    /**
     * Sets stack of YANG type with partially resolved YANG construct hierarchy.
     *
     * @param partialResolvedStack partial resolved YANG construct stack
     */
    private void setPartialResolvedStack(Stack<YangEntityToResolveInfoImpl<T>> partialResolvedStack) {
        this.partialResolvedStack = partialResolvedStack;
    }

    /**
     * Sets stack of YANG type with partially resolved YANG construct hierarchy.
     *
     * @param partialResolvedInfo partial resolved YANG construct stack
     */
    private void addInPartialResolvedStack(YangEntityToResolveInfoImpl<T> partialResolvedInfo) {
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

    @Override
    public YangEntityToResolveInfoImpl<T> getEntityToResolveInfo() {
        return entityToResolveInfo;
    }

    /**
     * Sets information about the entity that needs to be resolved.
     *
     * @param entityToResolveInfo information about the entity that needs to be
     *                            resolved
     */
    private void setEntityToResolveInfo(YangEntityToResolveInfoImpl<T> entityToResolveInfo) {
        this.entityToResolveInfo = entityToResolveInfo;
    }

    @Override
    public int getLineNumber() {
        return lineNumber;
    }

    @Override
    public int getCharPosition() {
        return charPosition;
    }

    @Override
    public void setLineNumber(int lineNumber) {
        this.lineNumber = lineNumber;
    }

    @Override
    public void setCharPosition(int charPositionInLine) {
        this.charPosition = charPositionInLine;
    }

    /**
     * Returns current module/sub-module reference, will be used in inter-file/
     * inter-jar scenario to get the import/include list.
     *
     * @return current module/sub-module reference
     */
    private YangReferenceResolver getCurReferenceResolver() {
        return curReferenceResolver;
    }

    /**
     * Sets current module/sub-module reference, will be used in inter-file/
     * inter-jar scenario to get the import/include list.
     *
     * @param curReferenceResolver current module/sub-module reference
     */
    private void setCurReferenceResolver(YangReferenceResolver curReferenceResolver) {
        this.curReferenceResolver = curReferenceResolver;
    }

    @Override
    public void linkInterFile(YangReferenceResolver dataModelRootNode)
            throws DataModelException {

        setCurReferenceResolver(dataModelRootNode);

        // Current node to resolve, it can be a YANG type or YANG uses.
        T entityToResolve = getEntityToResolveInfo().getEntityToResolve();

        // Check if linking is already done
        if (entityToResolve instanceof Resolvable) {
            Resolvable resolvable = (Resolvable) entityToResolve;
            if (resolvable.getResolvableStatus() == RESOLVED) {
                return;
            }
        } else {
            throw new DataModelException("Data Model Exception: Entity to resolved is not Resolvable");
        }

        // Push the initial entity to resolve in stack.
        addInPartialResolvedStack(getEntityToResolveInfo());

        // Inter file linking and resolution.
        linkInterFileAndResolve();
    }

    /**
     * Returns the referenced prefix of entity under resolution.
     *
     * @return referenced prefix of entity under resolution
     * @throws DataModelException a violation in data model rule
     */
    private String getRefPrefix()
            throws DataModelException {
        String refPrefix;
        if (getCurrentEntityToResolveFromStack() instanceof YangType) {
            refPrefix = ((YangType<?>) getCurrentEntityToResolveFromStack()).getPrefix();
        } else if (getCurrentEntityToResolveFromStack() instanceof YangUses) {
            refPrefix = ((YangUses) getCurrentEntityToResolveFromStack()).getPrefix();
        } else {
            throw new DataModelException("Data Model Exception: Entity to resolved is other than type/uses");
        }
        return refPrefix;
    }

    /**
     * Performs inter file linking and resolution.
     *
     * @throws DataModelException a violation in data model rule
     */
    private void linkInterFileAndResolve()
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
                     * If the entity is already resolved in the stack, then pop
                     * it and continue with the remaining stack elements to
                     * resolve
                     */
                    getPartialResolvedStack().pop();
                    break;
                }

                case INTER_FILE_LINKED: {
                    /*
                     * If the top of the stack is already linked then resolve
                     * the references and pop the entity and continue with
                     * remaining stack elements to resolve
                     */
                    resolveTopOfStack(INTER_FILE);
                    getPartialResolvedStack().pop();
                    break;
                }

                case INTRA_FILE_RESOLVED: {
                    /*
                     * If the top of the stack is intra file resolved then check
                     * if top of stack is linked, if not link it using
                     * import/include list and push the linked referred entity
                     * to the stack, otherwise only push it to the stack.
                     */
                    linkInterFileTopOfStackRefUpdateStack();
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
     * Links the top of the stack if it's inter-file and update stack.
     *
     * @throws DataModelException data model error
     */
    private void linkInterFileTopOfStackRefUpdateStack()
            throws DataModelException {

        /*
         * Obtain the referred node of top of stack entity under resolution
         */
        T referredNode = getRefNode();

        /*
         * Check for null for scenario when it's not linked and inter-file
         * linking is required.
         */
        if (referredNode == null) {

            /*
             * Check if prefix is null or not, to identify whether to search in
             * import list or include list.
             */
            if (getRefPrefix() != null && !getRefPrefix().contentEquals(getCurReferenceResolver().getPrefix())) {
                if (resolveWithImport()) {
                    return;
                }
            } else {
                if (resolveWithInclude()) {
                    return;
                }
            }
            // Exception when referred typedef/grouping is not found.
            DataModelException dataModelException = new DataModelException("YANG file error: Referred " +
                    "typedef/grouping for a given type/uses can't be found.");
            dataModelException.setLine(getLineNumber());
            dataModelException.setCharPosition(getCharPosition());
            throw dataModelException;
        } else {
            /*
             * If referred node is already linked, then just change the status
             * and push to the stack.
             */
            ((Resolvable) getCurrentEntityToResolveFromStack()).setResolvableStatus(INTER_FILE_LINKED);
            addUnresolvedRecursiveReferenceToStack((YangNode) referredNode);
        }
    }

    /**
     * Finds and resolves with include list.
     *
     * @return true if resolved, false otherwise
     * @throws DataModelException a violation in data model rule
     */
    private boolean resolveWithInclude()
            throws DataModelException {
        /*
         * Run through all the nodes in include list and search for referred
         * typedef/grouping at the root level.
         */
        for (YangInclude yangInclude : getCurReferenceResolver().getIncludeList()) {
            YangNode linkedNode = null;
            if (getCurrentEntityToResolveFromStack() instanceof YangType) {
                linkedNode = findRefTypedef(yangInclude.getIncludedNode());
            } else if (getCurrentEntityToResolveFromStack() instanceof YangUses) {
                linkedNode = findRefGrouping(yangInclude.getIncludedNode());
            }
            if (linkedNode != null) {
                // Add the link to external entity.
                addReferredEntityLink(linkedNode, INTER_FILE_LINKED);
                /*
                 * Update the current reference resolver to external
                 * module/sub-module containing the referred typedef/grouping.
                 */
                setCurReferenceResolver((YangReferenceResolver) yangInclude.getIncludedNode());
                // Add the type/uses of referred typedef/grouping to the stack.
                addUnresolvedRecursiveReferenceToStack(linkedNode);
                return true;
            }
        }
        // If referred node can't be found return false.
        return false;
    }

    /**
     * Finds and resolves with import list.
     *
     * @return true if resolved, false otherwise
     * @throws DataModelException a violation in data model rule
     */
    private boolean resolveWithImport()
            throws DataModelException {
        /*
         * Run through import list to find the referred typedef/grouping.
         */
        for (YangImport yangImport : getCurReferenceResolver().getImportList()) {
            /*
             * Match the prefix attached to entity under resolution with the
             * imported/included module/sub-module's prefix. If found, search
             * for the referred typedef/grouping at the root level.
             */
            if (yangImport.getPrefixId().contentEquals(getRefPrefix())) {
                YangNode linkedNode = null;
                if (getCurrentEntityToResolveFromStack() instanceof YangType) {
                    linkedNode = findRefTypedef(yangImport.getImportedNode());
                } else if (getCurrentEntityToResolveFromStack() instanceof YangUses) {
                    linkedNode = findRefGrouping(yangImport.getImportedNode());
                }
                if (linkedNode != null) {
                    // Add the link to external entity.
                    addReferredEntityLink(linkedNode, INTER_FILE_LINKED);
                    /*
                     * Update the current reference resolver to external
                     * module/sub-module containing the referred
                     * typedef/grouping.
                     */
                    setCurReferenceResolver((YangReferenceResolver) yangImport.getImportedNode());
                    // Add the type/uses of referred typedef/grouping to the
                    // stack.
                    addUnresolvedRecursiveReferenceToStack(linkedNode);
                    return true;
                }
                /*
                 * If referred node can't be found at root level break for loop,
                 * and return false.
                 */
                break;
            }
        }
        // If referred node can't be found return false.
        return false;
    }

    /**
     * Returns referred typedef/grouping node.
     *
     * @return referred typedef/grouping node
     * @throws DataModelException a violation in data model rule
     */
    private T getRefNode()
            throws DataModelException {
        if (getCurrentEntityToResolveFromStack() instanceof YangType) {
            YangDerivedInfo<?> derivedInfo = (YangDerivedInfo<?>) ((YangType<?>) getCurrentEntityToResolveFromStack())
                    .getDataTypeExtendedInfo();
            return (T) derivedInfo.getReferredTypeDef();
        } else if (getCurrentEntityToResolveFromStack() instanceof YangUses) {
            return (T) ((YangUses) getCurrentEntityToResolveFromStack()).getRefGroup();
        } else {
            throw new DataModelException("Data Model Exception: Entity to resolved is other than type/uses");
        }
    }

    /**
     * Finds the referred grouping node at the root level of imported/included node.
     *
     * @param refNode module/sub-module node
     * @return referred grouping
     */
    private YangNode findRefGrouping(YangNode refNode) {
        YangNode tmpNode = refNode.getChild();
        while (tmpNode != null) {
            if (tmpNode instanceof YangGrouping) {
                if (tmpNode.getName()
                        .equals(((YangUses) getCurrentEntityToResolveFromStack()).getName())) {
                    return tmpNode;
                }
            }
            tmpNode = tmpNode.getNextSibling();
        }
        return null;
    }

    /**
     * Finds the referred typedef node at the root level of imported/included node.
     *
     * @param refNode module/sub-module node
     * @return referred typedef
     */
    private YangNode findRefTypedef(YangNode refNode) {
        YangNode tmpNode = refNode.getChild();
        while (tmpNode != null) {
            if (tmpNode instanceof YangTypeDef) {
                if (tmpNode.getName()
                        .equals(((YangType) getCurrentEntityToResolveFromStack()).getDataTypeName())) {
                    return tmpNode;
                }
            }
            tmpNode = tmpNode.getNextSibling();
        }
        return null;
    }
}

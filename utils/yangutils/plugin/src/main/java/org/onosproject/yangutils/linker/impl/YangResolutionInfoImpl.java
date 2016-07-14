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
import java.util.Iterator;
import java.util.List;
import java.util.Stack;

import org.onosproject.yangutils.datamodel.Resolvable;
import org.onosproject.yangutils.datamodel.ResolvableType;
import org.onosproject.yangutils.datamodel.YangAtomicPath;
import org.onosproject.yangutils.datamodel.YangAugment;
import org.onosproject.yangutils.datamodel.YangAugmentableNode;
import org.onosproject.yangutils.datamodel.YangBase;
import org.onosproject.yangutils.datamodel.YangDerivedInfo;
import org.onosproject.yangutils.datamodel.YangEntityToResolveInfo;
import org.onosproject.yangutils.datamodel.YangFeature;
import org.onosproject.yangutils.datamodel.YangFeatureHolder;
import org.onosproject.yangutils.datamodel.YangGrouping;
import org.onosproject.yangutils.datamodel.YangIfFeature;
import org.onosproject.yangutils.datamodel.YangIdentity;
import org.onosproject.yangutils.datamodel.YangIdentityRef;
import org.onosproject.yangutils.datamodel.YangImport;
import org.onosproject.yangutils.datamodel.YangInclude;
import org.onosproject.yangutils.datamodel.YangInput;
import org.onosproject.yangutils.datamodel.YangLeaf;
import org.onosproject.yangutils.datamodel.YangLeafList;
import org.onosproject.yangutils.datamodel.YangLeafRef;
import org.onosproject.yangutils.datamodel.YangLeavesHolder;
import org.onosproject.yangutils.datamodel.YangList;
import org.onosproject.yangutils.datamodel.YangModule;
import org.onosproject.yangutils.datamodel.YangNode;
import org.onosproject.yangutils.datamodel.YangNodeIdentifier;
import org.onosproject.yangutils.datamodel.YangOutput;
import org.onosproject.yangutils.datamodel.YangPathArgType;
import org.onosproject.yangutils.datamodel.YangPathPredicate;
import org.onosproject.yangutils.datamodel.YangReferenceResolver;
import org.onosproject.yangutils.datamodel.YangRelativePath;
import org.onosproject.yangutils.datamodel.YangResolutionInfo;
import org.onosproject.yangutils.datamodel.YangRpc;
import org.onosproject.yangutils.datamodel.YangSubModule;
import org.onosproject.yangutils.datamodel.YangType;
import org.onosproject.yangutils.datamodel.YangTypeDef;
import org.onosproject.yangutils.datamodel.YangUses;
import org.onosproject.yangutils.datamodel.YangXPathResolver;
import org.onosproject.yangutils.datamodel.exceptions.DataModelException;
import org.onosproject.yangutils.datamodel.utils.ResolvableStatus;
import org.onosproject.yangutils.datamodel.utils.builtindatatype.YangDataTypes;
import org.onosproject.yangutils.linker.YangLinkingPhase;
import org.onosproject.yangutils.linker.exceptions.LinkerException;

import static org.onosproject.yangutils.datamodel.utils.ResolvableStatus.INTER_FILE_LINKED;
import static org.onosproject.yangutils.datamodel.utils.ResolvableStatus.INTRA_FILE_RESOLVED;
import static org.onosproject.yangutils.datamodel.utils.ResolvableStatus.LINKED;
import static org.onosproject.yangutils.datamodel.utils.ResolvableStatus.RESOLVED;
import static org.onosproject.yangutils.datamodel.utils.ResolvableStatus.UNDEFINED;
import static org.onosproject.yangutils.datamodel.utils.ResolvableStatus.UNRESOLVED;
import static org.onosproject.yangutils.linker.YangLinkingPhase.INTER_FILE;
import static org.onosproject.yangutils.linker.YangLinkingPhase.INTRA_FILE;
import static org.onosproject.yangutils.linker.impl.YangLinkerUtils.detectCollisionForAugmentedNode;
import static org.onosproject.yangutils.utils.UtilConstants.FEATURE_LINKER_ERROR;
import static org.onosproject.yangutils.utils.UtilConstants.GROUPING_LINKER_ERROR;
import static org.onosproject.yangutils.utils.UtilConstants.INPUT;
import static org.onosproject.yangutils.utils.UtilConstants.LEAFREF;
import static org.onosproject.yangutils.utils.UtilConstants.IDENTITYREF;
import static org.onosproject.yangutils.utils.UtilConstants.LEAFREF_LINKER_ERROR;
import static org.onosproject.yangutils.utils.UtilConstants.OUTPUT;
import static org.onosproject.yangutils.utils.UtilConstants.TYPEDEF_LINKER_ERROR;
import static org.onosproject.yangutils.utils.UtilConstants.IDENTITYREF_LINKER_ERROR;
import static org.onosproject.yangutils.utils.UtilConstants.BASE_LINKER_ERROR;

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
    private transient int lineNumber;

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

        /**
         * Current node to resolve, it can be a YANG type, YANG uses or YANG if-feature or
         * YANG leafref or YANG base or YANG identityref.
         */
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
            throw new DataModelException("Data Model Exception: Entity to resolved is other than " +
                    "type/uses/if-feature/leafref/base/identityref");
        }

        // Push the initial entity to resolve in stack.
        addInPartialResolvedStack(getEntityToResolveInfo());

        linkAndResolvePartialResolvedStack();

        addDerivedRefTypeToRefTypeResolutionList();
    }

    /**
     * Resolves linking with ancestors.
     *
     * @throws DataModelException a violation of data model rules
     */
    private void linkAndResolvePartialResolvedStack()
            throws DataModelException {

        while (getPartialResolvedStack().size() != 0) {

            /**
             * Current node to resolve, it can be a YANG type or YANG uses or
             * YANG if-feature or YANG leafref or YANG base or YANG identityref.
             */
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
                            } else if (resolvable instanceof YangUses) {
                                errorInfo = GROUPING_LINKER_ERROR;
                            } else if (resolvable instanceof YangIfFeature) {
                                errorInfo = FEATURE_LINKER_ERROR;
                            } else if (resolvable instanceof YangBase) {
                                errorInfo = BASE_LINKER_ERROR;
                            } else if (resolvable instanceof YangIdentityRef) {
                                errorInfo = IDENTITYREF_LINKER_ERROR;
                            } else {
                                errorInfo = LEAFREF_LINKER_ERROR;
                            }
                            DataModelException dataModelException =
                                    new DataModelException(errorInfo);
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
                throw new DataModelException(
                        "Data Model Exception: Entity to resolved is other than type/uses/if-feature" +
                                "/leafref/base/identityref");
            }
        }

    }

    /**
     * Adds the leafref/identityref type to the type, which has derived type referring to
     * typedef with leafref/identityref type.
     */
    private void addDerivedRefTypeToRefTypeResolutionList() throws DataModelException {

        YangNode potentialAncestorWithReferredNode = getEntityToResolveInfo().getHolderOfEntityToResolve();

        // If holder is typedef return.
        if (potentialAncestorWithReferredNode instanceof YangTypeDef) {
            return;
        }

        // If entity is not type return.
        if (!(getEntityToResolveInfo().getEntityToResolve() instanceof YangType)) {
            return;
        }

        YangType yangType = (YangType) getEntityToResolveInfo().getEntityToResolve();

        // If type is not resolved return.
        if (yangType.getResolvableStatus() != RESOLVED) {
            return;
        }

        YangDerivedInfo derivedInfo = (YangDerivedInfo) yangType.getDataTypeExtendedInfo();

        /*
         * If the derived types referred type is not leafref/identityref return
         */
        if ((derivedInfo.getEffectiveBuiltInType() != YangDataTypes.LEAFREF) &&
                (derivedInfo.getEffectiveBuiltInType() != YangDataTypes.IDENTITYREF)) {
            return;
        }

        T extendedInfo = (T) derivedInfo.getReferredTypeDef().getTypeDefBaseType().getDataTypeExtendedInfo();

        while (extendedInfo instanceof YangDerivedInfo) {
            YangDerivedInfo derivedInfoFromTypedef = (YangDerivedInfo) extendedInfo;
            extendedInfo = (T) derivedInfoFromTypedef.getReferredTypeDef().getTypeDefBaseType()
                    .getDataTypeExtendedInfo();
        }

        /*
         * Backup the derived types leafref/identityref info, delete all the info in current type,
         * but for resolution status as resolved. Copy the backed up leafref/identityref to types extended info,
         * create a leafref/identityref resolution info using the current resolution info and
         * add to leafref/identityref resolution list.
         */
        if (derivedInfo.getEffectiveBuiltInType() == YangDataTypes.LEAFREF) {
            YangLeafRef leafRefInTypeDef = (YangLeafRef) extendedInfo;
            yangType.resetYangType();

            yangType.setResolvableStatus(RESOLVED);
            yangType.setDataType(YangDataTypes.LEAFREF);
            yangType.setDataTypeName(LEAFREF);
            yangType.setDataTypeExtendedInfo(leafRefInTypeDef);
            leafRefInTypeDef.setResolvableStatus(UNRESOLVED);

            // Add resolution information to the list.
            YangResolutionInfoImpl resolutionInfoImpl = new YangResolutionInfoImpl<>(leafRefInTypeDef,
                                                                potentialAncestorWithReferredNode,
                                                                getLineNumber(), getCharPosition());
            getCurReferenceResolver().addToResolutionList(resolutionInfoImpl,
                                                          ResolvableType.YANG_LEAFREF);
            getCurReferenceResolver().resolveSelfFileLinking(ResolvableType.YANG_LEAFREF);

        } else if (derivedInfo.getEffectiveBuiltInType() == YangDataTypes.IDENTITYREF) {

            YangIdentityRef identityRefInTypeDef = (YangIdentityRef) extendedInfo;
            yangType.resetYangType();

            yangType.setResolvableStatus(RESOLVED);
            yangType.setDataType(YangDataTypes.IDENTITYREF);
            yangType.setDataTypeName(IDENTITYREF);
            yangType.setDataTypeExtendedInfo(identityRefInTypeDef);
            identityRefInTypeDef.setResolvableStatus(UNRESOLVED);

            // Add resolution information to the list.
            YangResolutionInfoImpl resolutionInfoImpl = new YangResolutionInfoImpl<>(identityRefInTypeDef,
                                            potentialAncestorWithReferredNode, getLineNumber(), getCharPosition());
            getCurReferenceResolver().addToResolutionList(resolutionInfoImpl,
                                                          ResolvableType.YANG_IDENTITYREF);
            getCurReferenceResolver().resolveSelfFileLinking(ResolvableType.YANG_IDENTITYREF);
        }
    }

    /**
     * Resolves the current entity in the stack.
     */
    private void resolveTopOfStack(YangLinkingPhase linkingPhase)
            throws DataModelException {
        ((Resolvable) getCurrentEntityToResolveFromStack()).resolve();
        if (((Resolvable) getCurrentEntityToResolveFromStack()).getResolvableStatus() != INTRA_FILE_RESOLVED
                && ((Resolvable) getCurrentEntityToResolveFromStack()).getResolvableStatus() != UNDEFINED) {
            // Sets the resolution status in inside the type/uses/if-feature/leafref.
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

        if (getCurrentEntityToResolveFromStack() instanceof YangIfFeature) {
            resolveSelfFileLinkingForIfFeature(potentialAncestorWithReferredNode);
            return;
        } else if (getCurrentEntityToResolveFromStack() instanceof YangLeafRef) {
            resolveSelfFileLinkingForLeafref(potentialAncestorWithReferredNode);
            return;
        } else if ((getCurrentEntityToResolveFromStack() instanceof YangIdentityRef) ||
                (getCurrentEntityToResolveFromStack() instanceof YangBase)) {
            resolveSelfFileLinkingForBaseAndIdentityref();
            return;
        } else {

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

        /*
         * In case prefix is not present it's a candidate for inter-file
         * resolution via include list.
         */
        if (getRefPrefix() == null) {
            ((Resolvable) getCurrentEntityToResolveFromStack()).setResolvableStatus(INTRA_FILE_RESOLVED);
        }
    }

    /**
     * Resolves self file linking for leafref.
     *
     * @param potentialAncestorWithReferredNode leafref holder node
     * @throws DataModelException a violation of data model rules
     */
    private void resolveSelfFileLinkingForLeafref(YangNode potentialAncestorWithReferredNode)
            throws DataModelException {

        YangNode ancestorWithTheReferredNode = potentialAncestorWithReferredNode;
        YangLeafRef leafref = (YangLeafRef) getCurrentEntityToResolveFromStack();
        boolean referredLeafFound = false;

        /*
         * Takes absolute path and takes the root node as module/sub-module,
         * then sends the list of nodes for finding the target leaf.
         */
        if (leafref.getPathType() == YangPathArgType.ABSOLUTE_PATH) {
            List<YangAtomicPath> atomicPathList = leafref.getAtomicPath();
            if (atomicPathList != null && !atomicPathList.isEmpty()) {
                Iterator<YangAtomicPath> listOfYangAtomicPath = atomicPathList.listIterator();
                if (getCurReferenceResolver() instanceof YangModule) {
                    YangModule rootNode = (YangModule) getCurReferenceResolver();
                    // Sends list of nodes for finding the target leaf.
                    referredLeafFound = isLeafReferenceFound(listOfYangAtomicPath, rootNode,
                            referredLeafFound, potentialAncestorWithReferredNode);
                } else if (getCurReferenceResolver() instanceof YangSubModule) {
                    YangSubModule rootNode = (YangSubModule) getCurReferenceResolver();
                    // Sends list of nodes for finding the target leaf.
                    referredLeafFound = isLeafReferenceFound(listOfYangAtomicPath, rootNode,
                            referredLeafFound, potentialAncestorWithReferredNode);
                }
            }
            /*
             * Takes relative path, goes to the parent node by using the
             * ancestor count and sends the list of nodes for finding the target
             * leaf.
             */
        } else if (leafref.getPathType() == YangPathArgType.RELATIVE_PATH) {
            YangRelativePath yangRelativePath = leafref.getRelativePath();
            int parentNodes = yangRelativePath.getAncestorNodeCount();
            List<YangAtomicPath> atomicPathList = yangRelativePath.getAtomicPathList();
            if (atomicPathList != null && !atomicPathList.isEmpty()) {
                Iterator<YangAtomicPath> listOfAtomicPath = atomicPathList.listIterator();
                // Gets the root node from ancestor count.
                YangNode rootparentNode = getRootNodeWithAncestorCount(parentNodes, ancestorWithTheReferredNode);
                // Sends list of nodes for finding the target leaf.
                referredLeafFound = isLeafReferenceFound(listOfAtomicPath, rootparentNode,
                        referredLeafFound, potentialAncestorWithReferredNode);
            }
        }
        if (referredLeafFound) {
            return;
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
     * Resolves self file linking for base/identityref.
     *
     * @throws DataModelException a violation of data model rules
     */
    private void resolveSelfFileLinkingForBaseAndIdentityref()
            throws DataModelException {

        boolean referredIdentityFound = false;
        String nodeName = null;

        if (getCurrentEntityToResolveFromStack() instanceof YangIdentityRef) {
           nodeName = ((YangIdentityRef) getCurrentEntityToResolveFromStack()).getName();
        }

        if (getCurrentEntityToResolveFromStack() instanceof YangBase) {
            nodeName = ((YangBase) getCurrentEntityToResolveFromStack()).getBaseIdentifier().getName();
        }

        if (getCurReferenceResolver() instanceof YangModule) {
            YangModule rootNode = (YangModule) getCurReferenceResolver();
            // Sends list of nodes for finding the target identity.
            referredIdentityFound = isIdentityReferenceFound(nodeName, rootNode);
        } else if (getCurReferenceResolver() instanceof YangSubModule) {
            YangSubModule rootNode = (YangSubModule) getCurReferenceResolver();
            // Sends list of nodes for finding the target identity.
            referredIdentityFound = isIdentityReferenceFound(nodeName, rootNode);
        }

        if (referredIdentityFound) {
            return;
        }

        /*
         * In case prefix is not present it's a candidate for inter-file resolution via include list.
         */
        if (getRefPrefix() == null) {
            ((Resolvable) getCurrentEntityToResolveFromStack()).setResolvableStatus(INTRA_FILE_RESOLVED);
        }
    }

    /**
     * Returns the root parent with respect to the ancestor count from leafref.
     *
     * @param ancestorCount count of node where parent node can be reached
     * @param currentParent current parent node
     * @return root node
     * @throws DataModelException a violation of data model rules
     */
    private YangNode getRootNodeWithAncestorCount(int ancestorCount, YangNode currentParent)
            throws DataModelException {

        int currentParentCount = 1;
        while (currentParentCount < ancestorCount) {
            if (currentParent.getParent() == null) {
                throw new DataModelException("YANG file error: The target node of leafref is invalid.");
            }
            currentParent = currentParent.getParent();
            currentParentCount = currentParentCount + 1;
        }
        return currentParent;
    }

    /**
     * Resolves self file linking for if-feature.
     *
     * @param potentialAncestorWithReferredNode if-feature holder node
     * @throws DataModelException DataModelException a violation of data model
     *                            rules
     */
    private void resolveSelfFileLinkingForIfFeature(YangNode potentialAncestorWithReferredNode)
            throws DataModelException {

        YangFeatureHolder featureHolder = getFeatureHolder(potentialAncestorWithReferredNode);
        YangNode potentialReferredNode = (YangNode) featureHolder;
        if (isReferredNode(potentialReferredNode)) {

            // Adds reference link of entity to the node under resolution.
            addReferredEntityLink(potentialReferredNode, LINKED);

            /**
             * resolve the reference and update the partial resolution stack
             * with any further recursive references
             */
            addUnresolvedRecursiveReferenceToStack(potentialReferredNode);
            return;
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
     * Returns the status of the referred leaf/leaf-list found for leafref.
     *
     * @param listOfYangAtomicPath list of atomic paths
     * @param ancestorWithTheReferredNode the parent node of leafref
     * @param referredLeafFound status of referred leaf/leaf-list
     * @param potentialAncestorWithReferredNode holder of the leafref leaf
     * @return status of referred leaf
     * @throws DataModelException a violation of data model rules
     */
    private boolean isLeafReferenceFound(Iterator<YangAtomicPath> listOfYangAtomicPath,
            YangNode ancestorWithTheReferredNode, boolean referredLeafFound, YangNode potentialAncestorWithReferredNode)
            throws DataModelException {

        while (listOfYangAtomicPath.hasNext()) {
            YangAtomicPath atomicPath = listOfYangAtomicPath.next();
            String nodeName = atomicPath.getNodeIdentifier().getName();

            // When child is not present, only leaf/leaf-list is available in the node.
            if (ancestorWithTheReferredNode.getChild() == null) {
                referredLeafFound = isReferredLeafOrLeafListFound(ancestorWithTheReferredNode, nodeName, (T) LINKED);
                break;
            }
            ancestorWithTheReferredNode = ancestorWithTheReferredNode.getChild();

            // Checks all the siblings under the node and returns the matched node.
            YangNode nodeFound = isReferredNodeInSiblingProcessedForLeafref(ancestorWithTheReferredNode, nodeName);

            // When node is not found in all the siblings, leaf-list may be the node we have to find.
            if (nodeFound == null) {
                referredLeafFound = isReferredLeafOrLeafListFound(ancestorWithTheReferredNode.getParent(), nodeName,
                        (T) LINKED);
            } else {
                ancestorWithTheReferredNode = nodeFound;

                // For the node check if path predicate is present and fill its values.
                List<YangPathPredicate> pathPredicateList = atomicPath.getPathPredicatesList();
                if (pathPredicateList != null && !pathPredicateList.isEmpty()) {
                    Iterator<YangPathPredicate> listOfYangPathPredicate = pathPredicateList.listIterator();
                    fillPathPredicatesForTheNode(ancestorWithTheReferredNode, listOfYangPathPredicate,
                            potentialAncestorWithReferredNode);
                }
            }

            // If leaf is also not found and node is also not found return the status as false.
            if (!referredLeafFound && nodeFound == null) {
                break;
            }
        }
        return referredLeafFound;
    }

    /**
     * Returns the status of the referred identity found for base/identityref.
     *
     * @param nodeName the name of the base nodeidentifier/identityref nodeidentifier
     * @param ancestorWithTheReferredNode the parent node of base/identityref
     * @return status of referred base/identityref
     * @throws DataModelException a violation of data model rules
     */
    private boolean isIdentityReferenceFound(String nodeName, YangNode ancestorWithTheReferredNode)
            throws DataModelException {

        // When child is not present return.
        if (ancestorWithTheReferredNode.getChild() == null) {
            return false;
        }

        ancestorWithTheReferredNode = ancestorWithTheReferredNode.getChild();

        // Checks all the siblings under the node and returns the matched node.
        YangNode nodeFound = isReferredNodeInSiblingProcessedForIdentity(ancestorWithTheReferredNode, nodeName);

        if (nodeFound != null) {
            // Adds reference link of entity to the node under resolution.
            addReferredEntityLink(nodeFound, LINKED);

            /**
             * resolve the reference and update the partial resolution stack with any further recursive references
             */
            addUnresolvedRecursiveReferenceToStack(nodeFound);
            return true;
        }

        return false;
    }

    /**
     * Fills the referred leaf or leaf-list inside the path predicates.
     *
     * @param ancestorWithTheReferredNode the actual node where YANG list will be present
     * @param listOfYangPathPredicate the path predicates present for the node
     * @param potentialAncestorWithReferredNode the current leaf node parent
     * @throws DataModelException a violation of data model rules
     */
    private void fillPathPredicatesForTheNode(YangNode ancestorWithTheReferredNode,
            Iterator<YangPathPredicate> listOfYangPathPredicate, YangNode potentialAncestorWithReferredNode)
            throws DataModelException {

        while (listOfYangPathPredicate.hasNext()) {
            if (!(ancestorWithTheReferredNode instanceof YangList)) {
                throw new DataModelException("YANG file error: The path predicates are applicable only for list");
            }
            YangPathPredicate pathPredicate = listOfYangPathPredicate.next();
            YangNodeIdentifier leftNode = pathPredicate.getNodeIdentifier();
            YangRelativePath relativePath = pathPredicate.getRightRelativePath();

            // Checks that the left axis is filled in the path predicate.
            boolean isLeftLeafOrLeafListSetForLeftAxis = getLeftLeafOrLeafListInPredicate(
                    (YangList) ancestorWithTheReferredNode, pathPredicate, leftNode);
            if (!isLeftLeafOrLeafListSetForLeftAxis) {
                throw new DataModelException(
                        "YANG file error: The path predicate is not referring to an existing leaf/leaflist");
            }
            int parentNodes = relativePath.getAncestorNodeCount();

            // Finds the root node for the right relative path.
            YangNode rootParentNode = getRootNodeWithAncestorCount(parentNodes, potentialAncestorWithReferredNode);

            // Finds the leaf/leaf-list from the right side relative path.
            resolveRightAxisNodeInPathPredicate(relativePath, rootParentNode, pathPredicate);
        }
    }

    /**
     * Resolves the right axis node in the path predicate.
     *
     * @param relativePath the relative path in the path predicate
     * @param rootParentNode parent node from where the node has to be found
     * @param pathPredicate data tree reference in YANG list
     * @throws DataModelException a violation of data model rules
     */
    private void resolveRightAxisNodeInPathPredicate(YangRelativePath relativePath, YangNode rootParentNode,
            YangPathPredicate pathPredicate) throws DataModelException {

        List<YangAtomicPath> absolutePathList = relativePath.getAtomicPathList();
        if (absolutePathList != null && !absolutePathList.isEmpty()) {
            Iterator<YangAtomicPath> listOfYangAtomicPathForRightRelative = absolutePathList.listIterator();
            while (listOfYangAtomicPathForRightRelative.hasNext()) {
                boolean isRightAxisNodeFound = false;
                YangAtomicPath absolutePathInPredicate = listOfYangAtomicPathForRightRelative.next();
                String nodeNameInAtomicPath = absolutePathInPredicate.getNodeIdentifier().getName();

                // When child is not there check the leaf/leaf-list.
                if (rootParentNode.getChild() == null) {
                    isRightAxisNodeFound = isReferredLeafOrLeafListFound(rootParentNode,
                            nodeNameInAtomicPath, (T) pathPredicate);
                    if (!isRightAxisNodeFound) {
                        throw new DataModelException(
                                "YANG file error: The path predicates is not referring to an existing leaf/leaflist");
                    }
                    break;
                }
                rootParentNode = rootParentNode.getChild();
                YangNode nodeFoundInTheRelativePath = isReferredNodeInSiblingProcessedForLeafref(
                        rootParentNode, nodeNameInAtomicPath);

                if (nodeFoundInTheRelativePath == null) {

                    // When node is not found check the leaf/leaf-list.
                    isRightAxisNodeFound = isReferredLeafOrLeafListFound(rootParentNode.getParent(),
                            nodeNameInAtomicPath, (T) pathPredicate);
                } else {
                    rootParentNode = nodeFoundInTheRelativePath;
                }
                if (!isRightAxisNodeFound && nodeFoundInTheRelativePath == null) {
                    throw new DataModelException(
                            "YANG file error: The path predicates is not referring to an existing leaf/leaflist");
                }
            }
        }
    }

    /**
     * Returns the status, if referred leaf/leaf-list is found.
     *
     * @param ancestorWithTheReferredNode the parent node of leaf/leaf-list
     * @param nodeName the name of the leaf/leaf-list
     * @param statusOrPathPredicate the status to be set for the leaf-ref or the path predicate
     * @return status of the target node is found
     * @throws DataModelException a violation of data model rules
     */
    private boolean isReferredLeafOrLeafListFound(YangNode ancestorWithTheReferredNode, String nodeName,
            T statusOrPathPredicate) throws DataModelException {

        if (!(ancestorWithTheReferredNode instanceof YangLeavesHolder)) {
            throw new DataModelException("Yang file error: The target node of leafref is invalid.");
        }
        YangLeavesHolder leavesHolder = (YangLeavesHolder) ancestorWithTheReferredNode;
        if (leavesHolder.getListOfLeaf() != null) {
            Iterator<YangLeaf> yangLeavesList = leavesHolder.getListOfLeaf().listIterator();
            while (yangLeavesList.hasNext()) {
                YangLeaf yangleaf = yangLeavesList.next();
                if (yangleaf.getName().contentEquals(nodeName)) {
                    if (statusOrPathPredicate instanceof ResolvableStatus) {
                        ResolvableStatus status = (ResolvableStatus) statusOrPathPredicate;

                        // Sets the referred leaf to YANG leafref.
                        ((YangLeafRef) getCurrentEntityToResolveFromStack()).setReferredLeafOrLeafList(yangleaf);

                        // Adds reference link of entity to the node under resolution.
                        addReferredEntityLink(ancestorWithTheReferredNode, status);
                        addUnResolvedLeafRefTypeToStack((T) yangleaf, ancestorWithTheReferredNode);
                        return true;
                    } else if (statusOrPathPredicate instanceof YangPathPredicate) {
                        YangPathPredicate pathPredicate = (YangPathPredicate) statusOrPathPredicate;

                        // Sets the right axis node.
                        pathPredicate.setRightAxisNode(yangleaf);
                        return true;
                    } else {
                        throw new DataModelException("YANG file error: The target node of leafref is invalid.");
                    }
                }
            }
        }
        if (leavesHolder.getListOfLeafList() != null) {
            Iterator<YangLeafList> yangLeafListList = leavesHolder.getListOfLeafList().listIterator();
            while (yangLeafListList.hasNext()) {
                YangLeafList yangLeaflist = yangLeafListList.next();
                if (yangLeaflist.getName().contentEquals(nodeName)) {
                    if (statusOrPathPredicate instanceof ResolvableStatus) {
                        ResolvableStatus status = (ResolvableStatus) statusOrPathPredicate;

                        // Sets the referred leaf-list to YANG leafref.
                        ((YangLeafRef) getCurrentEntityToResolveFromStack()).setReferredLeafOrLeafList(yangLeaflist);

                        // Adds reference link of entity to the node under resolution.
                        addReferredEntityLink(ancestorWithTheReferredNode, status);
                        addUnResolvedLeafRefTypeToStack((T) yangLeaflist, ancestorWithTheReferredNode);
                        return true;
                    } else if (statusOrPathPredicate instanceof YangPathPredicate) {
                        YangPathPredicate pathPredicate = (YangPathPredicate) statusOrPathPredicate;
                        pathPredicate.setRightAxisNode(yangLeaflist);
                        return true;
                    } else {
                        throw new DataModelException("YANG file error: The target node of leafref is invalid.");
                    }
                }
            }
        }
        return false;
    }

    /**
     * Adds the unresolved constructs to stack which has to be resolved for leafref.
     *
     * @param yangleafOrLeafList YANG leaf or leaf list which holds the type
     * @param ancestorWithTheReferredNode holder of the YANG leaf or leaf list
     */
    private void addUnResolvedLeafRefTypeToStack(T yangleafOrLeafList, YangNode ancestorWithTheReferredNode) {

        YangType referredTypeInLeafOrLeafList;
        if (yangleafOrLeafList instanceof YangLeaf) {
            YangLeaf leaf = (YangLeaf) yangleafOrLeafList;
            referredTypeInLeafOrLeafList = leaf.getDataType();
            if (referredTypeInLeafOrLeafList.getDataType() == YangDataTypes.LEAFREF) {
                YangEntityToResolveInfoImpl<YangLeafRef<?>> unResolvedEntityInfo = new YangEntityToResolveInfoImpl<>();
                unResolvedEntityInfo.setEntityToResolve((YangLeafRef<?>) leaf.getDataType().getDataTypeExtendedInfo());
                unResolvedEntityInfo.setHolderOfEntityToResolve(ancestorWithTheReferredNode);
                addInPartialResolvedStack((YangEntityToResolveInfoImpl<T>) unResolvedEntityInfo);
            } else if (referredTypeInLeafOrLeafList.getDataType() == YangDataTypes.DERIVED) {
                YangEntityToResolveInfoImpl<YangType<?>> unResolvedEntityInfo = new YangEntityToResolveInfoImpl<>();
                unResolvedEntityInfo.setEntityToResolve(referredTypeInLeafOrLeafList);
                unResolvedEntityInfo.setHolderOfEntityToResolve(ancestorWithTheReferredNode);
                addInPartialResolvedStack((YangEntityToResolveInfoImpl<T>) unResolvedEntityInfo);
            }
        } else {
            YangLeafList leafList = (YangLeafList) yangleafOrLeafList;
            referredTypeInLeafOrLeafList = leafList.getDataType();
            if (referredTypeInLeafOrLeafList.getDataType() == YangDataTypes.LEAFREF) {
                YangEntityToResolveInfoImpl<YangLeafRef<?>> unResolvedEntityInfo = new YangEntityToResolveInfoImpl<>();
                unResolvedEntityInfo
                        .setEntityToResolve((YangLeafRef<?>) leafList.getDataType().getDataTypeExtendedInfo());
                unResolvedEntityInfo.setHolderOfEntityToResolve(ancestorWithTheReferredNode);
                addInPartialResolvedStack((YangEntityToResolveInfoImpl<T>) unResolvedEntityInfo);
            } else if (referredTypeInLeafOrLeafList.getDataType() == YangDataTypes.DERIVED) {
                YangEntityToResolveInfoImpl<YangType<?>> unResolvedEntityInfo = new YangEntityToResolveInfoImpl<>();
                unResolvedEntityInfo.setEntityToResolve(referredTypeInLeafOrLeafList);
                unResolvedEntityInfo.setHolderOfEntityToResolve(ancestorWithTheReferredNode);
                addInPartialResolvedStack((YangEntityToResolveInfoImpl<T>) unResolvedEntityInfo);
            }
        }
    }

    /**
     * Returns true if referred leaf/leaf-list is found in a particular node. This is for path in path predicate.
     *
     * @param listForLeaf list node where referred leaf is found
     * @param pathPredicate path predicate instance where the value of nodes to be found are available
     * @param leftNode node identifier of the left side parameter in path predicate
     * @return status of the leaf/leaf-list found
     */
    private boolean getLeftLeafOrLeafListInPredicate(YangList listForLeaf, YangPathPredicate pathPredicate,
            YangNodeIdentifier leftNode) {

        if (listForLeaf.getListOfLeaf() != null) {
            Iterator<YangLeaf> yangLeavesUnderList = listForLeaf.getListOfLeaf().listIterator();
            while (yangLeavesUnderList.hasNext()) {
                YangLeaf yangleafUnderList = yangLeavesUnderList.next();
                if (yangleafUnderList.getName().contentEquals(leftNode.getName())) {
                    pathPredicate.setLeftAxisNode(yangleafUnderList);
                    return true;
                }
            }
        }
        if (listForLeaf.getListOfLeafList() != null) {
            Iterator<YangLeafList> yangLeavesListUnderList = listForLeaf.getListOfLeafList().listIterator();
            while (yangLeavesListUnderList.hasNext()) {
                YangLeafList yangleafListUnderList = yangLeavesListUnderList.next();
                if (yangleafListUnderList.getName().contentEquals(leftNode.getName())) {
                    pathPredicate.setLeftAxisNode(yangleafListUnderList);
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Returns feature holder(module/sub-module node) .
     *
     * @param potentialAncestorWithReferredNode if-feature holder node
     */
    private YangFeatureHolder getFeatureHolder(YangNode potentialAncestorWithReferredNode) {
        while (potentialAncestorWithReferredNode != null) {
            if (potentialAncestorWithReferredNode instanceof YangFeatureHolder) {
                return (YangFeatureHolder) potentialAncestorWithReferredNode;
            }
            potentialAncestorWithReferredNode = potentialAncestorWithReferredNode.getParent();
        }
        return null;
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
     * Checks for the referred parent node for the leafref path.
     *
     * @param potentialReferredNode potential referred node
     * @return the reffered parent node of leaf/leaf-list
     * @throws DataModelException data model errors
     */
    private YangNode isReferredNodeInSiblingProcessedForLeafref(YangNode potentialReferredNode, String referredNodeName)
            throws DataModelException {

        while (potentialReferredNode != null) {
            if (potentialReferredNode instanceof YangInput) {
                if (referredNodeName.equalsIgnoreCase(INPUT)) {
                    return potentialReferredNode;
                }
            } else if (potentialReferredNode instanceof YangOutput) {
                if (referredNodeName.equalsIgnoreCase(OUTPUT)) {
                    return potentialReferredNode;
                }
            }
            // Check if the potential referred node is the actual referred node
            if (isReferredNodeForLeafref(potentialReferredNode, referredNodeName)) {
                if (potentialReferredNode instanceof YangGrouping || potentialReferredNode instanceof YangTypeDef) {
                    if (potentialReferredNode.getParent() instanceof YangRpc) {
                        potentialReferredNode = potentialReferredNode.getNextSibling();
                    } else {
                        throw new DataModelException("YANG file error: The target node of leafref is invalid.");
                    }
                }
                return potentialReferredNode;
            }
            potentialReferredNode = potentialReferredNode.getNextSibling();
        }
        return null;
    }

    /**
     * Checks for the referred parent node for the base/identity.
     *
     * @param potentialReferredNode potential referred node
     * @return the reffered parent node of base/identity.
     * @throws DataModelException data model errors
     */
    private YangNode isReferredNodeInSiblingProcessedForIdentity(YangNode potentialReferredNode,
                            String referredNodeName)  throws DataModelException {

        while (potentialReferredNode != null) {
            if (potentialReferredNode instanceof YangIdentity) {
                // Check if the potential referred node is the actual referred node
                if (isReferredNodeForIdentity(potentialReferredNode, referredNodeName)) {
                    return potentialReferredNode;
                }
            }
            potentialReferredNode = potentialReferredNode.getNextSibling();
        }
        return null;
    }

    /**
     * Checks if the current reference node name and the name in the path are equal.
     *
     * @param currentReferredNode the node where the reference is pointed
     * @param nameOfNodeinPath name of the node in the path
     * @return status of the match between the name
     * @throws DataModelException a violation of data model rules
     */
    private boolean isReferredNodeForLeafref(YangNode currentReferredNode, String nameOfNodeinPath)
            throws DataModelException {

        if (getCurrentEntityToResolveFromStack() instanceof YangLeafRef) {
            /*
             * Check if name of node name matches with the current reference
             * node.
             */
            return currentReferredNode.getName().contentEquals(nameOfNodeinPath);
        } else {
            throw new DataModelException("Data Model Exception: Entity to resolved is other than leafref");
        }
    }

    /**
     * Checks if the current reference node name and the name in the base/identityref base are equal.
     *
     * @param currentReferredNode the node where the reference is pointed
     * @param nameOfIdentityRefBase name of the base in the base/identityref base
     * @return status of the match between the name
     * @throws DataModelException a violation of data model rules
     */
    private boolean isReferredNodeForIdentity(YangNode currentReferredNode, String nameOfIdentityRefBase)
            throws DataModelException {

        if ((getCurrentEntityToResolveFromStack() instanceof YangIdentityRef) ||
                (getCurrentEntityToResolveFromStack() instanceof YangBase)) {
            /*
             * Check if name of node name matches with the current reference node.
             */
            return currentReferredNode.getName().contentEquals(nameOfIdentityRefBase);
        } else {
            throw new DataModelException("Data Model Exception: Entity to resolved is other than identityref");
        }
    }

    /**
     * Checks for the referred node defined in a ancestor scope.
     *
     * @param potentialReferredNode potential referred node
     * @return status of resolution and updating the partial resolved stack with
     * the any recursive references
     * @throws DataModelException a violation of data model rules
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
        } else if (getCurrentEntityToResolveFromStack() instanceof YangIfFeature) {
            if (potentialReferredNode instanceof YangFeatureHolder) {
                /*
                 * Check if name of node name matches with the entity being
                 * resolved
                 */
                return isNodeNameSameAsResolutionInfoName(potentialReferredNode);
            }
        } else if ((getCurrentEntityToResolveFromStack() instanceof YangBase) ||
                (getCurrentEntityToResolveFromStack() instanceof YangIdentityRef)) {
            if (potentialReferredNode instanceof YangIdentity) {
                /*
                 * Check if name of node name matches with the entity being
                 * resolved
                 */
                return isNodeNameSameAsResolutionInfoName(potentialReferredNode);
            }
        } else {
            throw new DataModelException("Data Model Exception: Entity to resolved is other than type/" +
                                                 "uses/base/identityref");
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
        } else if (getCurrentEntityToResolveFromStack() instanceof YangIfFeature) {
            return isFeatureDefinedInNode(node);
        } else if (getCurrentEntityToResolveFromStack() instanceof YangBase) {
            if (node.getName().contentEquals(
                    ((YangBase) getCurrentEntityToResolveFromStack()).getBaseIdentifier().getName())) {
                return true;
            }
        } else if (getCurrentEntityToResolveFromStack() instanceof YangIdentityRef) {
            if (node.getName().contentEquals(
                    ((YangIdentityRef) getCurrentEntityToResolveFromStack()).getName())) {
                return true;
            }
        } else {
            throw new DataModelException("Data Model Exception: Entity to resolved is other than type/uses");
        }
        return false;
    }

    private boolean isFeatureDefinedInNode(YangNode node) throws DataModelException {
        YangNodeIdentifier ifFeature = ((YangIfFeature) getCurrentEntityToResolveFromStack()).getName();
        List<YangFeature> featureList = ((YangFeatureHolder) node).getFeatureList();
        if (featureList != null && !featureList.isEmpty()) {
            Iterator<YangFeature> iterator = featureList.iterator();
            while (iterator.hasNext()) {
                YangFeature feature = iterator.next();
                if (ifFeature.getName().equals(feature.getName())) {
                    ((YangIfFeature) getCurrentEntityToResolveFromStack()).setReferredFeature(feature);
                    ((YangIfFeature) getCurrentEntityToResolveFromStack()).setReferredFeatureHolder(node);
                    return true;
                }
            }
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
        } else if (getCurrentEntityToResolveFromStack() instanceof YangIfFeature) {
            // do nothing , referred node is already set
        } else if (getCurrentEntityToResolveFromStack() instanceof YangLeafRef) {
            // do nothing , referred node is already set
        } else if (getCurrentEntityToResolveFromStack() instanceof YangBase) {
            ((YangBase) getCurrentEntityToResolveFromStack()).setReferredIdentity((YangIdentity) referredNode);
        } else if (getCurrentEntityToResolveFromStack() instanceof YangIdentityRef) {
            ((YangIdentityRef) getCurrentEntityToResolveFromStack()).setReferredIdentity((YangIdentity) referredNode);
        } else {
            throw new DataModelException("Data Model Exception: Entity to resolved is other than type" +
                                                 "/uses/base/identityref");
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
        } else if (getCurrentEntityToResolveFromStack() instanceof YangIfFeature) {
            addUnResolvedIfFeatureToStack(referredNode);
        } else if (getCurrentEntityToResolveFromStack() instanceof YangLeafRef) {
            // do nothing , referred node is already set
            throw new DataModelException("Data Model Exception: Entity to resolved is other than type/uses");
        } else if ((getCurrentEntityToResolveFromStack() instanceof YangBase) ||
                (getCurrentEntityToResolveFromStack() instanceof YangIdentityRef)) {
            /*
             * Search if the identity has any un resolved base, if so return true, else return false.
             */
            addUnResolvedBaseToStack(referredNode);
        } else {
            throw new DataModelException("Data Model Exception: Entity to resolved is other than type/uses/" +
                                                 "base/identityref");
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
     * Returns if there is any unresolved if-feature in feature.
     *
     * @param node module/submodule node
     */
    private void addUnResolvedIfFeatureToStack(YangNode node) {
        YangFeature refFeature = ((YangIfFeature) getCurrentEntityToResolveFromStack()).getReferredFeature();
        List<YangIfFeature> ifFeatureList = refFeature.getIfFeatureList();
        if (ifFeatureList != null && !ifFeatureList.isEmpty()) {
            Iterator<YangIfFeature> ifFeatureIterator = ifFeatureList.iterator();
            while (ifFeatureIterator.hasNext()) {
                YangIfFeature ifFeature = ifFeatureIterator.next();
                YangEntityToResolveInfo<YangIfFeature> unResolvedEntityInfo = new YangEntityToResolveInfoImpl<>();
                unResolvedEntityInfo.setEntityToResolve(ifFeature);
                unResolvedEntityInfo.setHolderOfEntityToResolve(node);
                addInPartialResolvedStack((YangEntityToResolveInfoImpl<T>) unResolvedEntityInfo);
            }
        }
    }

    /**
     * Returns if there is any unresolved base in identity.
     *
     * @param node module/submodule node
     */
    private void addUnResolvedBaseToStack(YangNode node) {

        YangIdentity curNode = (YangIdentity) node;
        if (curNode.getBaseNode() != null) {
            if (curNode.getBaseNode().getResolvableStatus() != RESOLVED) {
                YangEntityToResolveInfoImpl<YangBase> unResolvedEntityInfo = new YangEntityToResolveInfoImpl<>();
                unResolvedEntityInfo.setEntityToResolve(curNode.getBaseNode());
                unResolvedEntityInfo.setHolderOfEntityToResolve(node);
                addInPartialResolvedStack((YangEntityToResolveInfoImpl<T>) unResolvedEntityInfo);

            }
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

        if (entityToResolve instanceof YangXPathResolver) {
            //Process x-path linking.
            processXPathLinking(getEntityToResolveInfo(), dataModelRootNode);

        } else {

            // Push the initial entity to resolve in stack.
            addInPartialResolvedStack(getEntityToResolveInfo());

            // Inter file linking and resolution.
            linkInterFileAndResolve();

            addDerivedRefTypeToRefTypeResolutionList();
        }
    }

    /**
     * Process x-path linking for augment and leaf-ref.
     *
     * @param entityToResolveInfo entity to resolve
     * @param root root node
     */
    private void processXPathLinking(YangEntityToResolveInfoImpl<T> entityToResolveInfo,
                                     YangReferenceResolver root) {
        YangXpathLinker<T> xPathLinker = new YangXpathLinker<T>();
        T entityToResolve = entityToResolveInfo.getEntityToResolve();
        if (entityToResolve instanceof YangAugment) {
            YangNode targetNode = null;
            YangAugment augment = (YangAugment) entityToResolve;
            targetNode = xPathLinker.processAugmentXpathLinking(augment.getTargetNode(),
                    (YangNode) root);
            if (targetNode != null) {
                if (targetNode instanceof YangAugmentableNode) {
                    detectCollisionForAugmentedNode(targetNode, augment);
                    ((YangAugmentableNode) targetNode).addAugmentation(augment);
                    augment.setAugmentedNode(targetNode);
                    Resolvable resolvable = (Resolvable) entityToResolve;
                    resolvable.setResolvableStatus(RESOLVED);
                } else {
                    throw new LinkerException("Invalid target node type " + targetNode.getNodeType() + " for "
                            + augment.getName());
                }
            } else {
                throw new LinkerException("Failed to link " + augment.getName());
            }
        } else if (entityToResolve instanceof YangLeafRef) {
            YangLeafRef leafRef = (YangLeafRef) entityToResolve;
            Object target = xPathLinker.processLeafRefXpathLinking(leafRef.getAtomicPath(),
                    (YangNode) root);
            if (target != null) {
                YangLeaf leaf = null;
                YangLeafList leafList = null;
                leafRef.setReferredLeafOrLeafList(target);
                if (target instanceof YangLeaf) {
                    leaf = (YangLeaf) target;
                    leafRef.setEffectiveDataType(leaf.getDataType());
                } else {
                    leafList = (YangLeafList) target;
                    leafRef.setEffectiveDataType(leafList.getDataType());
                }
                leafRef.setResolvableStatus(RESOLVED);
                //TODO: add logic for leaf-ref for path predicates.
            } else {
                throw new LinkerException("YANG file error: Unable to find base leaf/leaf-list for given leafref "
                        + leafRef.getPath());
            }
        }
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
        } else if (getCurrentEntityToResolveFromStack() instanceof YangIfFeature) {
            refPrefix = ((YangIfFeature) getCurrentEntityToResolveFromStack()).getPrefix();
        } else if (getCurrentEntityToResolveFromStack() instanceof YangLeafRef) {
            refPrefix = refPrefixForLeafRef();
        } else if (getCurrentEntityToResolveFromStack() instanceof YangBase) {
            refPrefix = ((YangBase) getCurrentEntityToResolveFromStack()).getBaseIdentifier().getPrefix();
        } else if (getCurrentEntityToResolveFromStack() instanceof YangIdentityRef) {
            refPrefix = ((YangIdentityRef) getCurrentEntityToResolveFromStack()).getPrefix();
        } else {
            throw new DataModelException("Data Model Exception: Entity to resolved is other than " +
                                                 "type/uses/base/identityref");
        }
        return refPrefix;
    }

    /**
     * Returns the referenced prefix for leafref under resolution.
     *
     * @return referenced prefix of leafref under resolution
     */
    private String refPrefixForLeafRef() {

        String refPrefix;
        if (((YangLeafRef) getCurrentEntityToResolveFromStack()).getPathType() == YangPathArgType.ABSOLUTE_PATH) {
            List<YangAtomicPath> theList = ((YangLeafRef) getCurrentEntityToResolveFromStack()).getAtomicPath();
            YangAtomicPath absPath = theList.iterator().next();
            refPrefix = absPath.getNodeIdentifier().getPrefix();
        } else {
            YangRelativePath relativePath = ((YangLeafRef) getCurrentEntityToResolveFromStack()).getRelativePath();
            List<YangAtomicPath> theList = relativePath.getAtomicPathList();
            YangAtomicPath absPath = theList.iterator().next();
            refPrefix = absPath.getNodeIdentifier().getPrefix();
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

                    case UNDEFINED: {
                        /*
                         * In case of if-feature resolution, if referred "feature" is not
                         * defined then the resolvable status will be undefined.
                         */
                        getPartialResolvedStack().pop();
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

            if (getCurrentEntityToResolveFromStack() instanceof YangIfFeature) {
                ((YangIfFeature) getCurrentEntityToResolveFromStack()).setResolvableStatus(UNDEFINED);
                return;
            }
            // If current entity is still not resolved, then
            // linking/resolution has failed.
            String errorInfo;
            if (getCurrentEntityToResolveFromStack() instanceof YangType) {
                errorInfo = TYPEDEF_LINKER_ERROR;
            } else if (getCurrentEntityToResolveFromStack() instanceof YangUses) {
                errorInfo = GROUPING_LINKER_ERROR;
            } else if (getCurrentEntityToResolveFromStack() instanceof YangIfFeature) {
                errorInfo = FEATURE_LINKER_ERROR;
            } else if (getCurrentEntityToResolveFromStack() instanceof YangBase) {
                errorInfo = BASE_LINKER_ERROR;
            } else if (getCurrentEntityToResolveFromStack() instanceof YangIdentityRef) {
                errorInfo = IDENTITYREF_LINKER_ERROR;
            } else {
                errorInfo = LEAFREF_LINKER_ERROR;
            }
            DataModelException dataModelException = new DataModelException(errorInfo);
            dataModelException.setLine(getLineNumber());
            dataModelException.setCharPosition(getCharPosition());
            throw dataModelException;
        } else {
            /*
             * If referred node is already linked, then just change the status
             * and push to the stack.
             */
            if (getCurrentEntityToResolveFromStack() instanceof YangLeafRef) {
                ((Resolvable) getCurrentEntityToResolveFromStack()).setResolvableStatus(INTER_FILE_LINKED);
                if (referredNode instanceof YangLeaf) {
                    YangLeaf yangleaf = (YangLeaf) referredNode;
                    addUnResolvedLeafRefTypeToStack((T) yangleaf, (YangNode) yangleaf.getContainedIn());
                } else if (referredNode instanceof YangLeafList) {
                    YangLeafList yangLeafList = (YangLeafList) referredNode;
                    addUnResolvedLeafRefTypeToStack((T) yangLeafList, (YangNode) yangLeafList.getContainedIn());
                }
            } else {
                ((Resolvable) getCurrentEntityToResolveFromStack()).setResolvableStatus(INTER_FILE_LINKED);
                addUnresolvedRecursiveReferenceToStack((YangNode) referredNode);
            }
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
            } else if (getCurrentEntityToResolveFromStack() instanceof YangIfFeature) {
                linkedNode = findRefFeature(yangInclude.getIncludedNode());
            } else if (getCurrentEntityToResolveFromStack() instanceof YangLeafRef) {
                boolean referredNode = findRefLeaf(yangInclude.getIncludedNode());
                /*
                 * Update the current reference resolver to external
                 * module/sub-module containing the referred typedef/grouping.
                 */
                setCurReferenceResolver((YangReferenceResolver) yangInclude.getIncludedNode());

                return referredNode;
            } else if (getCurrentEntityToResolveFromStack() instanceof YangBase) {
                linkedNode = findRefIdentity(yangInclude.getIncludedNode());
            } else if (getCurrentEntityToResolveFromStack() instanceof YangIdentityRef) {
                linkedNode = findRefIdentityRef(yangInclude.getIncludedNode());
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
                } else if (getCurrentEntityToResolveFromStack() instanceof YangIfFeature) {
                    linkedNode = findRefFeature(yangImport.getImportedNode());
                } else if (getCurrentEntityToResolveFromStack() instanceof YangLeafRef) {
                    boolean referredNode = findRefLeaf(yangImport.getImportedNode());
                    /*
                     * Update the current reference resolver to external
                     * module/sub-module containing the referred
                     * typedef/grouping.
                     */
                    setCurReferenceResolver((YangReferenceResolver) yangImport.getImportedNode());

                    return referredNode;
                } else if (getCurrentEntityToResolveFromStack() instanceof YangBase) {
                    linkedNode = findRefIdentity(yangImport.getImportedNode());
                } else if (getCurrentEntityToResolveFromStack() instanceof YangIdentityRef) {
                    linkedNode = findRefIdentityRef(yangImport.getImportedNode());
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
     * Returns the status of referred leaf.
     *
     * @param importedNode the root node from a YANG file
     * @return status of the referred leaf
     * @throws DataModelException
     */
    private boolean findRefLeaf(YangNode importedNode) throws DataModelException {

        boolean isReferredNodeFound = false;
        List<YangAtomicPath> absolutePathList = ((YangLeafRef) getCurrentEntityToResolveFromStack())
                .getAtomicPath();
        if (absolutePathList != null && !absolutePathList.isEmpty()) {
            Iterator<YangAtomicPath> listOfYangAtomicPath = absolutePathList.listIterator();

            while (listOfYangAtomicPath.hasNext()) {
                YangAtomicPath absolutePath = listOfYangAtomicPath.next();
                String nodeName = absolutePath.getNodeIdentifier().getName();

                if (importedNode.getChild() == null) {
                    isReferredNodeFound = isReferredLeafOrLeafListFound(importedNode, nodeName, (T) INTER_FILE_LINKED);
                    break;
                }
                importedNode = importedNode.getChild();

                YangNode nodeFound = isReferredNodeInSiblingProcessedForLeafref(importedNode, nodeName);
                if (nodeFound == null) {
                    isReferredNodeFound = isReferredLeafOrLeafListFound(importedNode.getParent(), nodeName,
                            (T) INTER_FILE_LINKED);
                } else {
                    importedNode = nodeFound;
                }
            }
        }
        // TODO: Path predicates filling for inter file has to be done.
        return isReferredNodeFound;
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
        } else if (getCurrentEntityToResolveFromStack() instanceof YangIfFeature) {
            return (T) ((YangIfFeature) getCurrentEntityToResolveFromStack()).getReferredFeatureHolder();
        } else if (getCurrentEntityToResolveFromStack() instanceof YangLeafRef) {
            return (T) ((YangLeafRef) getCurrentEntityToResolveFromStack()).getReferredLeafOrLeafList();
        } else if (getCurrentEntityToResolveFromStack() instanceof YangBase) {
            return (T) ((YangBase) getCurrentEntityToResolveFromStack()).getReferredIdentity();
        } else if (getCurrentEntityToResolveFromStack() instanceof YangIdentityRef) {
            return (T) ((YangIdentityRef) getCurrentEntityToResolveFromStack()).getReferredIdentity();
        } else {
            throw new DataModelException("Data Model Exception: Entity to resolved is other than type" +
                                                 "/uses/base/identityref");
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
     * Finds the referred feature node at the root level of imported/included node.
     *
     * @param refNode module/sub-module node
     * @return referred feature
     */
    private YangNode findRefFeature(YangNode refNode) {
        YangNodeIdentifier ifFeature = ((YangIfFeature) getCurrentEntityToResolveFromStack()).getName();
        List<YangFeature> featureList = ((YangFeatureHolder) refNode).getFeatureList();

        if (featureList != null && !featureList.isEmpty()) {
            Iterator<YangFeature> iterator = featureList.iterator();
            while (iterator.hasNext()) {
                YangFeature feature = iterator.next();
                if (ifFeature.getName().equals(feature.getName())) {
                    ((YangIfFeature) getCurrentEntityToResolveFromStack()).setReferredFeature(feature);
                    return refNode;
                }
            }
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

    /**
     * Finds the referred identity node at the root level of imported/included node.
     *
     * @param refNode module/sub-module node
     * @return referred identity
     */
    private YangNode findRefIdentity(YangNode refNode) {
        YangNode tmpNode = refNode.getChild();
        while (tmpNode != null) {
            if (tmpNode instanceof YangIdentity) {
                if (tmpNode.getName()
                        .equals(((YangBase) getCurrentEntityToResolveFromStack()).getBaseIdentifier().getName())) {
                    return tmpNode;
                }
            }
            tmpNode = tmpNode.getNextSibling();
        }
        return null;
    }

    /**
     * Finds the referred identity node at the root level of imported/included node.
     *
     * @param refNode module/sub-module node
     * @return referred identity
     */
    private YangNode findRefIdentityRef(YangNode refNode) {
        YangNode tmpNode = refNode.getChild();
        while (tmpNode != null) {
            if (tmpNode instanceof YangIdentity) {
                if (tmpNode.getName()
                        .equals(((YangIdentityRef) getCurrentEntityToResolveFromStack())
                                        .getBaseIdentity().getName())) {
                    return tmpNode;
                }
            }
            tmpNode = tmpNode.getNextSibling();
        }
        return null;
    }
}

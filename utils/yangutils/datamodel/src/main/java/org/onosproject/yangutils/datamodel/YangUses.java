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

import java.util.LinkedList;
import java.util.List;

import org.onosproject.yangutils.datamodel.exceptions.DataModelException;
import org.onosproject.yangutils.datamodel.utils.Parsable;
import org.onosproject.yangutils.datamodel.utils.ResolvableStatus;
import org.onosproject.yangutils.datamodel.utils.YangConstructType;

import static org.onosproject.yangutils.datamodel.utils.DataModelUtils.detectCollidingChildUtil;
import static org.onosproject.yangutils.datamodel.utils.DataModelUtils.getParentNodeInGenCode;

/*-
 * Reference RFC 6020.
 *
 * The "uses" statement is used to reference a "grouping" definition. It takes
 * one argument, which is the name of the grouping.
 *
 * The effect of a "uses" reference to a grouping is that the nodes defined by
 * the grouping are copied into the current schema tree, and then updated
 * according to the "refine" and "augment" statements.
 *
 * The identifiers defined in the grouping are not bound to a namespace until
 * the contents of the grouping are added to the schema tree via a "uses"
 * statement that does not appear inside a "grouping" statement, at which point
 * they are bound to the namespace of the current module.
 *
 * The uses's sub-statements
 *
 *                +--------------+---------+-------------+------------------+
 *                | substatement | section | cardinality |data model mapping|
 *                +--------------+---------+-------------+------------------+
 *                | augment      | 7.15    | 0..1        | -child nodes     |
 *                | description  | 7.19.3  | 0..1        | -string          |
 *                | if-feature   | 7.18.2  | 0..n        | -TODO            |
 *                | refine       | 7.12.2  | 0..1        | -TODO            |
 *                | reference    | 7.19.4  | 0..1        | -string          |
 *                | status       | 7.19.2  | 0..1        | -YangStatus      |
 *                | when         | 7.19.5  | 0..1        | -TODO            |
 *                +--------------+---------+-------------+------------------+
 */

/**
 * Represents data model node to maintain information defined in YANG uses.
 */
public class YangUses
        extends YangNode
        implements YangCommonInfo, Parsable, Resolvable, CollisionDetector {

    private static final long serialVersionUID = 806201617L;

    /**
     * YANG node identifier.
     */
    private YangNodeIdentifier nodeIdentifier;

    /**
     * Referred group.
     */
    private YangGrouping refGroup;

    /**
     * Description of YANG uses.
     */
    private String description;

    /**
     * YANG reference.
     */
    private String reference;

    /**
     * Status of YANG uses.
     */
    private YangStatusType status;

    /**
     * Status of resolution. If completely resolved enum value is "RESOLVED",
     * if not enum value is "UNRESOLVED", in case reference of grouping/typedef
     * is added to uses/type but it's not resolved value of enum should be
     * "INTRA_FILE_RESOLVED".
     */
    private ResolvableStatus resolvableStatus;

    /**
     * Effective list of nodes of grouping that needs to replicated at YANG uses.
     */
    private List<YangNode> resolvedGroupingNodes;

    /**
     * Effective list of leaves of grouping that needs to replicated at YANG uses.
     */
    private List<List<YangLeaf>> resolvedGroupingLeaves;

    /**
     * Effective list of leaf lists of grouping that needs to replicated at YANG uses.
     */
    private List<List<YangLeafList>> resolvedGroupingLeafLists;

    /**
     * Creates an YANG uses node.
     */
    public YangUses() {
        super(YangNodeType.USES_NODE);
        nodeIdentifier = new YangNodeIdentifier();
        resolvableStatus = ResolvableStatus.UNRESOLVED;
        resolvedGroupingNodes = new LinkedList<YangNode>();
        resolvedGroupingLeaves = new LinkedList<List<YangLeaf>>();
        resolvedGroupingLeafLists = new LinkedList<List<YangLeafList>>();
    }

    /**
     * Returns the referred group.
     *
     * @return the referred group
     */
    public YangGrouping getRefGroup() {
        return refGroup;
    }

    /**
     * Sets the referred group.
     *
     * @param refGroup the referred group
     */
    public void setRefGroup(YangGrouping refGroup) {
        this.refGroup = refGroup;
    }

    /**
     * Returns the description.
     *
     * @return the description
     */
    @Override
    public String getDescription() {
        return description;
    }

    /**
     * Sets the description.
     *
     * @param description set the description
     */
    @Override
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Returns the textual reference.
     *
     * @return the reference
     */
    @Override
    public String getReference() {
        return reference;
    }

    /**
     * Sets the textual reference.
     *
     * @param reference the reference to set
     */
    @Override
    public void setReference(String reference) {
        this.reference = reference;
    }

    /**
     * Returns the status.
     *
     * @return the status
     */
    @Override
    public YangStatusType getStatus() {
        return status;
    }

    /**
     * Sets the status.
     *
     * @param status the status to set
     */
    @Override
    public void setStatus(YangStatusType status) {
        this.status = status;
    }

    /**
     * Returns the type of the data.
     *
     * @return returns USES_DATA
     */
    @Override
    public YangConstructType getYangConstructType() {
        return YangConstructType.USES_DATA;
    }

    /**
     * Validates the data on entering the corresponding parse tree node.
     *
     * @throws DataModelException a violation of data model rules
     */
    @Override
    public void validateDataOnEntry()
            throws DataModelException {
        // TODO auto-generated method stub, to be implemented by parser
    }

    /**
     * Validates the data on exiting the corresponding parse tree node.
     *
     * @throws DataModelException a violation of data model rules
     */
    @Override
    public void validateDataOnExit()
            throws DataModelException {
        // TODO auto-generated method stub, to be implemented by parser
    }

    @Override
    public String getName() {
        return nodeIdentifier.getName();
    }

    @Override
    public void setName(String name) {
        nodeIdentifier.setName(name);
    }

    /**
     * Returns node identifier.
     *
     * @return node identifier
     */
    public YangNodeIdentifier getNodeIdentifier() {
        return nodeIdentifier;
    }

    /**
     * Sets node identifier.
     *
     * @param nodeIdentifier the node identifier
     */
    public void setNodeIdentifier(YangNodeIdentifier nodeIdentifier) {
        this.nodeIdentifier = nodeIdentifier;
    }

    /**
     * Returns prefix associated with uses.
     *
     * @return prefix associated with uses
     */
    public String getPrefix() {
        return nodeIdentifier.getPrefix();
    }

    /**
     * Get prefix associated with uses.
     *
     * @param prefix prefix associated with uses
     */
    public void setPrefix(String prefix) {
        nodeIdentifier.setPrefix(prefix);
    }

    @Override
    public void resolve()
            throws DataModelException {

        YangGrouping referredGrouping = getRefGroup();

        if (referredGrouping == null) {
            throw new DataModelException("YANG uses linker error, cannot resolve uses");
        }

        YangNode usesParentNode = getParentNodeInGenCode(this);
        if (!(usesParentNode instanceof YangLeavesHolder)
                || !(usesParentNode instanceof CollisionDetector)) {
            throw new DataModelException("YANG uses holder construct is wrong");
        }

        YangLeavesHolder usesParentLeavesHolder = (YangLeavesHolder) usesParentNode;
        if (referredGrouping.getListOfLeaf() != null
                && referredGrouping.getListOfLeaf().size() != 0) {
            addLeavesOfGrouping(
                    cloneLeavesList(referredGrouping.getListOfLeaf(),
                            usesParentLeavesHolder));
        }

        if (referredGrouping.getListOfLeafList() != null
                && referredGrouping.getListOfLeafList().size() != 0) {
            addListOfLeafListOfGrouping(
                    cloneListOfLeafList(referredGrouping.getListOfLeafList(),
                            usesParentLeavesHolder));
        }

        YangNode childInGrouping = referredGrouping.getChild();

        while (childInGrouping != null) {
            if (childInGrouping instanceof YangEnumeration
                    || childInGrouping instanceof YangUnion
                    || childInGrouping instanceof YangTypeDef) {

                /*
                 * No need to copy the leaves, union / enum class, as these will
                 * be generated in the scope of grouping
                 */
                childInGrouping = childInGrouping.getNextSibling();
                continue;
            } else if (childInGrouping instanceof YangUses) {
                addResolvedUsesInfoOfGrouping((YangUses) childInGrouping,
                        usesParentLeavesHolder);
            } else {
                addNodeOfGrouping(childInGrouping);
            }

            childInGrouping = childInGrouping.getNextSibling();
        }
    }

    /**
     * Clone the resolved uses contained in grouping to the uses of grouping.
     *
     * @param usesInGrouping resolved uses in grouping
     * @param usesHolder     holder of uses
     */
    private void addResolvedUsesInfoOfGrouping(YangUses usesInGrouping,
            YangLeavesHolder usesHolder) throws DataModelException {
        for (YangNode usesResolvedNode : usesInGrouping.getUsesResolvedNodeList()) {
            addNodeOfGrouping(usesResolvedNode);
        }

        for (List<YangLeaf> leavesList : usesInGrouping.getUsesResolvedLeavesList()) {
            addLeavesOfGrouping(cloneLeavesList(leavesList, usesHolder));
        }

        for (List<YangLeafList> listOfLeafLists : usesInGrouping.getUsesResolvedListOfLeafList()) {
            addListOfLeafListOfGrouping(
                    cloneListOfLeafList(listOfLeafLists, usesHolder));
        }
    }

    /**
     * Clone the list of leaves and return the cloned list leaves.
     *
     * @param listOfLeaves   list of leaves to be cloned
     * @param usesParentNode parent of the cloned location
     * @return cloned list of leaves
     * @throws DataModelException a violation in data model rule
     */
    private List<YangLeaf> cloneLeavesList(List<YangLeaf> listOfLeaves,
            YangLeavesHolder usesParentNode) throws DataModelException {
        if (listOfLeaves == null || listOfLeaves.size() == 0) {
            throw new DataModelException("No leaves to clone");
        }

        List<YangLeaf> newLeavesList = new LinkedList<YangLeaf>();
        for (YangLeaf leaf : listOfLeaves) {
            YangLeaf clonedLeaf;
            try {
                ((CollisionDetector) usesParentNode).detectCollidingChild(leaf.getName(),
                        YangConstructType.LEAF_DATA);
                clonedLeaf = leaf.clone();
            } catch (CloneNotSupportedException | DataModelException e) {
                throw new DataModelException(e.getMessage());
            }

            clonedLeaf.setContainedIn(usesParentNode);
            newLeavesList.add(clonedLeaf);
        }

        return newLeavesList;
    }

    /**
     * Clone the list of leaf list.
     *
     * @param listOfLeafList list of leaf list that needs to be cloned
     * @param usesParentNode parent of uses
     * @return cloned list of leaf list
     */
    private List<YangLeafList> cloneListOfLeafList(List<YangLeafList> listOfLeafList,
            YangLeavesHolder usesParentNode) throws DataModelException {
        if (listOfLeafList == null || listOfLeafList.size() == 0) {
            throw new DataModelException("No leaf lists to clone");
        }

        List<YangLeafList> newListOfLeafList = new LinkedList<YangLeafList>();
        for (YangLeafList leafList : listOfLeafList) {
            YangLeafList clonedLeafList;
            try {
                ((CollisionDetector) usesParentNode).detectCollidingChild(leafList.getName(),
                        YangConstructType.LEAF_LIST_DATA);
                clonedLeafList = leafList.clone();
            } catch (CloneNotSupportedException | DataModelException e) {
                throw new DataModelException(e.getMessage());
            }

            clonedLeafList.setContainedIn(usesParentNode);
            newListOfLeafList.add(clonedLeafList);
        }

        return newListOfLeafList;
    }

    @Override
    public ResolvableStatus getResolvableStatus() {
        return resolvableStatus;
    }

    @Override
    public void setResolvableStatus(ResolvableStatus resolvableStatus) {
        this.resolvableStatus = resolvableStatus;
    }

    @Override
    public void detectCollidingChild(String identifierName, YangConstructType dataType)
            throws DataModelException {
        detectCollidingChildUtil(identifierName, dataType, this);
    }

    @Override
    public void detectSelfCollision(String identifierName, YangConstructType dataType)
            throws DataModelException {

        if (getName().equals(identifierName)) {
            throw new DataModelException("YANG file error: Duplicate input identifier detected, same as uses \""
                    + getName() + "\"");
        }
    }

    /**
     * Adds the node under grouping to the effective uses resolved info.
     *
     * @param nodeInGrouping node defined under grouping which needs to be copied in
     *                       the context of uses
     */
    public void addNodeOfGrouping(YangNode nodeInGrouping) {
        resolvedGroupingNodes.add(nodeInGrouping);
    }

    /**
     * Returns the effective list of nodes added due to uses linking.
     *
     * @return effective list of nodes added due to uses linking
     */
    public List<YangNode> getUsesResolvedNodeList() {
        return resolvedGroupingNodes;
    }

    /**
     * Adds the leaves under grouping to the effective uses resolved info.
     *
     * @param leavesInGrouping Leaves defined under grouping which needs to be copied in
     *                         the context of uses
     */
    public void addLeavesOfGrouping(List<YangLeaf> leavesInGrouping) {
        resolvedGroupingLeaves.add(leavesInGrouping);
    }

    /**
     * Returns the effective list of Leaves added due to uses linking.
     *
     * @return effective list of Leaves added due to uses linking
     */
    public List<List<YangLeaf>> getUsesResolvedLeavesList() {
        return resolvedGroupingLeaves;
    }

    /**
     * Adds the leaf-lists under grouping to the effective uses resolved info.
     *
     * @param leafListsInGrouping leaf-lists defined under grouping which needs to be copied in
     *                            the context of uses
     */
    public void addListOfLeafListOfGrouping(List<YangLeafList> leafListsInGrouping) {
        resolvedGroupingLeafLists.add(leafListsInGrouping);
    }

    /**
     * Returns the effective list of Leaves added due to uses linking.
     *
     * @return effective list of Leaves added due to uses linking
     */
    public List<List<YangLeafList>> getUsesResolvedListOfLeafList() {
        return resolvedGroupingLeafLists;
    }
}

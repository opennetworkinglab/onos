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
import org.onosproject.yangutils.datamodel.utils.YangConstructType;

import static org.onosproject.yangutils.datamodel.utils.DataModelUtils.detectCollidingChildUtil;
import static org.onosproject.yangutils.datamodel.utils.YangConstructType.CASE_DATA;

/*-
 * Reference RFC 6020.
 *
 * The "case" statement is used to define branches of the choice. It takes as an
 * argument an identifier, followed by a block of sub-statements that holds
 * detailed case information.
 *
 * The identifier is used to identify the case node in the schema tree. A case
 * node does not exist in the data tree.
 *
 * Within a "case" statement, the "anyxml", "choice", "container", "leaf",
 * "list", "leaf-list", and "uses" statements can be used to define child nodes
 * to the case node. The identifiers of all these child nodes MUST be unique
 * within all cases in a choice. For example, the following is illegal:
 *
 * choice interface-type {     // This example is illegal YANG
 *        case a {
 *            leaf ethernet { ... }
 *        }
 *        case b {
 *            container ethernet { ...}
 *        }
 *    }
 *
 *  As a shorthand, the "case" statement can be omitted if the branch
 *  contains a single "anyxml", "container", "leaf", "list", or
 *  "leaf-list" statement.  In this case, the identifier of the case node
 *  is the same as the identifier in the branch statement.  The following
 *  example:
 *
 *    choice interface-type {
 *        container ethernet { ... }
 *    }
 *
 *  is equivalent to:
 *
 *    choice interface-type {
 *        case ethernet {
 *            container ethernet { ... }
 *        }
 *    }
 *
 *  The case identifier MUST be unique within a choice.
 *
 *  The case's sub-statements
 *
 *                +--------------+---------+-------------+------------------+
 *                | substatement | section | cardinality |data model mapping|
 *                +--------------+---------+-------------+------------------+
 *                | anyxml       | 7.10    | 0..n        |-not supported    |
 *                | choice       | 7.9     | 0..n        |-child nodes      |
 *                | container    | 7.5     | 0..n        |-child nodes      |
 *                | description  | 7.19.3  | 0..1        |-string           |
 *                | if-feature   | 7.18.2  | 0..n        |-TODO             |
 *                | leaf         | 7.6     | 0..n        |-YangLeaf         |
 *                | leaf-list    | 7.7     | 0..n        |-YangLeafList     |
 *                | list         | 7.8     | 0..n        |-child nodes      |
 *                | reference    | 7.19.4  | 0..1        |-string           |
 *                | status       | 7.19.2  | 0..1        |-YangStatus       |
 *                | uses         | 7.12    | 0..n        |-child node       |
 *                | when         | 7.19.5  | 0..1        |-TODO             |
 *                +--------------+---------+-------------+------------------+
 */

/**
 * Represents data model node to maintain information defined in YANG case.
 */
public class YangCase
        extends YangNode
        implements YangLeavesHolder, YangCommonInfo, Parsable, CollisionDetector, YangAugmentationHolder {

    private static final long serialVersionUID = 806201603L;

    /**
     * Case name.
     */
    private String name;

    // TODO: default field identification for the case

    /**
     * Description of case.
     */
    private String description;

    /**
     * List of leaves.
     */
    private List<YangLeaf> listOfLeaf;

    /**
     * List of leaf lists.
     */
    private List<YangLeafList> listOfLeafList;

    /**
     * Reference of the module.
     */
    private String reference;

    /**
     * Status of the node.
     */
    private YangStatusType status;

    /**
     * Creates a choice node.
     */
    public YangCase() {
        super(YangNodeType.CASE_NODE);
    }

    /**
     * Returns the case name.
     *
     * @return case name
     */
    @Override
    public String getName() {
        return name;
    }

    /**
     * Sets the case name.
     *
     * @param name case name
     */
    @Override
    public void setName(String name) {
        this.name = name;
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
     * Returns the list of leaves.
     *
     * @return the list of leaves
     */
    @Override
    public List<YangLeaf> getListOfLeaf() {
        return listOfLeaf;
    }

    /**
     * Sets the list of leaves.
     *
     * @param leafsList the list of leaf to set
     */
    @Override
    public void setListOfLeaf(List<YangLeaf> leafsList) {
        listOfLeaf = leafsList;
    }

    /**
     * Adds a leaf.
     *
     * @param leaf the leaf to be added
     */
    @Override
    public void addLeaf(YangLeaf leaf) {
        if (getListOfLeaf() == null) {
            setListOfLeaf(new LinkedList<YangLeaf>());
        }

        getListOfLeaf().add(leaf);
    }

    /**
     * Returns the list of leaf-list.
     *
     * @return the list of leaf-list
     */
    @Override
    public List<YangLeafList> getListOfLeafList() {
        return listOfLeafList;
    }

    /**
     * Sets the list of leaf-list.
     *
     * @param listOfLeafList the list of leaf-list to set
     */
    @Override
    public void setListOfLeafList(List<YangLeafList> listOfLeafList) {
        this.listOfLeafList = listOfLeafList;
    }

    /**
     * Adds a leaf-list.
     *
     * @param leafList the leaf-list to be added
     */
    @Override
    public void addLeafList(YangLeafList leafList) {
        if (getListOfLeafList() == null) {
            setListOfLeafList(new LinkedList<YangLeafList>());
        }

        getListOfLeafList().add(leafList);
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
     * @return returns CASE_DATA
     */
    @Override
    public YangConstructType getYangConstructType() {
        return CASE_DATA;
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
    public void detectCollidingChild(String identifierName, YangConstructType dataType)
            throws DataModelException {
        if (!(getParent() instanceof YangChoice)) {
            throw new DataModelException("Internal Data Model Tree Error: Invalid/Missing holder in case " +
                    getName());
        }
        // Traverse up in tree to ask parent choice start collision detection.
        ((CollisionDetector) getParent()).detectCollidingChild(identifierName, dataType);
    }

    @Override
    public void detectSelfCollision(String identifierName, YangConstructType dataType)
            throws DataModelException {

        if (dataType == CASE_DATA) {
            if (getName().equals(identifierName)) {
                throw new DataModelException("YANG File Error: Identifier collision detected in case \"" +
                        getName() + "\"");
            }
            return;
        }

        // Asks helper to detect colliding child.
        detectCollidingChildUtil(identifierName, dataType, this);
    }
}

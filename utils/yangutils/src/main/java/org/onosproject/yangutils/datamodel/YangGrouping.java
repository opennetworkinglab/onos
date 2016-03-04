/*
 * Copyright 2016 Open Networking Laboratory
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
import org.onosproject.yangutils.parser.Parsable;
import org.onosproject.yangutils.utils.YangConstructType;
import org.onosproject.yangutils.translator.CachedFileHandle;

/*-
 * Reference RFC 6020.
 *
 *  The "grouping" statement is used to define a reusable block of nodes,
 *  which may be used locally in the module, in modules that include it,
 *  and by other modules that import from it.  It takes one argument,
 *  which is an identifier, followed by a block of sub-statements that
 *  holds detailed grouping information.
 *
 *  The "grouping" statement is not a data definition statement and, as
 *  such, does not define any nodes in the schema tree.
 *
 *  A grouping is like a "structure" or a "record" in conventional
 *  programming languages.
 *  Once a grouping is defined, it can be referenced in a "uses"
 *  statement.  A grouping MUST NOT reference itself,
 *  neither directly nor indirectly through a chain of other groupings.
 *
 *  If the grouping is defined at the top level of a YANG module or
 *  submodule, the grouping's identifier MUST be unique within the
 *  module.
 *
 *  A grouping is more than just a mechanism for textual substitution,
 *  but defines a collection of nodes.  Identifiers appearing inside the
 *  grouping are resolved relative to the scope in which the grouping is
 *  defined, not where it is used.  Prefix mappings, type names, grouping
 *  names, and extension usage are evaluated in the hierarchy where the
 *  "grouping" statement appears.  For extensions, this means that
 *  extensions are applied to the grouping node, not the uses node.
 *
 *  The grouping's sub-statements
 *
 *                +--------------+---------+-------------+------------------+
 *                | substatement | section | cardinality |data model mapping|
 *                +--------------+---------+-------------+------------------+
 *                | anyxml       | 7.10    | 0..n        |-not supported    |
 *                | choice       | 7.9     | 0..n        |-child nodes      |
 *                | container    | 7.5     | 0..n        |-child nodes      |
 *                | description  | 7.19.3  | 0..1        |-string           |
 *                | grouping     | 7.11    | 0..n        |-child nodes      |
 *                | leaf         | 7.6     | 0..n        |-YangLeaf         |
 *                | leaf-list    | 7.7     | 0..n        |-YangLeafList     |
 *                | list         | 7.8     | 0..n        |-child nodes      |
 *                | reference    | 7.19.4  | 0..1        |-string           |
 *                | status       | 7.19.2  | 0..1        |-YangStatus       |
 *                | typedef      | 7.3     | 0..n        |-child nodes      |
 *                | uses         | 7.12    | 0..n        |-child nodes      |
 *                +--------------+---------+-------------+------------------+
 */

/**
 * Data model node to maintain information defined in YANG grouping.
 */
public class YangGrouping extends YangNode
        implements YangLeavesHolder, YangCommonInfo, Parsable {

    /**
     * Name of the grouping.
     */
    private String name;

    /**
     * Description.
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
     * Package of the generated java code.
     */
    private String pkg;

    /**
     * Creates the grouping node.
     */
    public YangGrouping() {
        super(YangNodeType.GROUPING_NODE);
    }

    /**
     * Get YANG grouping name.
     *
     * @return YANG grouping name
     */
    @Override
    public String getName() {
        return name;
    }

    /**
     * Set YANG grouping name.
     *
     * @param name YANG grouping name
     */
    @Override
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Get the description.
     *
     * @return the description
     */
    @Override
    public String getDescription() {
        return description;
    }

    /**
     * Set the description.
     *
     * @param description set the description
     */
    @Override
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Get the list of leaves.
     *
     * @return the list of leaves
     */
    @Override
    public List<YangLeaf> getListOfLeaf() {
        return listOfLeaf;
    }

    /**
     * Set the list of leaves.
     *
     * @param leafsList the list of leaf to set
     */
    private void setListOfLeaf(List<YangLeaf> leafsList) {
        listOfLeaf = leafsList;
    }

    /**
     * Add a leaf.
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
     * Get the list of leaf-list.
     *
     * @return the list of leaf-list
     */
    @Override
    public List<YangLeafList> getListOfLeafList() {
        return listOfLeafList;
    }

    /**
     * Set the list of leaf-list.
     *
     * @param listOfLeafList the list of leaf-list to set
     */
    private void setListOfLeafList(List<YangLeafList> listOfLeafList) {
        this.listOfLeafList = listOfLeafList;
    }

    /**
     * Add a leaf-list.
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
     * Get the textual reference.
     *
     * @return the reference
     */
    @Override
    public String getReference() {
        return reference;
    }

    /**
     * Set the textual reference.
     *
     * @param reference the reference to set
     */
    @Override
    public void setReference(String reference) {
        this.reference = reference;
    }

    /**
     * Get the status.
     *
     * @return the status
     */
    @Override
    public YangStatusType getStatus() {
        return status;
    }

    /**
     * Set the status.
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
     * @return returns GROUPING_DATA
     */
    @Override
    public YangConstructType getYangConstructType() {
        return YangConstructType.GROUPING_DATA;
    }

    /**
     * Validate the data on entering the corresponding parse tree node.
     *
     * @throws DataModelException a violation of data model rules
     */
    @Override
    public void validateDataOnEntry() throws DataModelException {
        // TODO auto-generated method stub, to be implemented by parser
    }

    /**
     * Validate the data on exiting the corresponding parse tree node.
     *
     * @throws DataModelException a violation of data model rules
     */
    @Override
    public void validateDataOnExit() throws DataModelException {
        // TODO auto-generated method stub, to be implemented by parser
    }

    /**
     * Generate the code for YANG grouping.
     *
     * @param codeGenDir code generated directory.
     */
    @Override
    public void generateJavaCodeEntry(String codeGenDir) {
        // TODO Auto-generated method stub

    }

    /**
     * Free the resources used to generate java files corresponding to YANG
     * grouping info and generate valid java files.
     */
    @Override
    public void generateJavaCodeExit() {
        // TODO Auto-generated method stub

    }

    /**
     * Get the mapped java package.
     *
     * @return the java package
     */
    @Override
    public String getPackage() {
        return pkg;
    }

    /**
     * Set the mapped java package.
     *
     * @param pakg the package to set
     */
    @Override
    public void setPackage(String pakg) {
        pkg = pakg;

    }

    @Override
    public CachedFileHandle getFileHandle() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void setFileHandle(CachedFileHandle fileHandle) {
        // TODO Auto-generated method stub

    }
}

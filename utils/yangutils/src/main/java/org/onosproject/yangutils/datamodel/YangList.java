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
import org.onosproject.yangutils.parser.ParsableDataType;

/*-
 *  The "list" statement is used to define an interior data node in the
 *  schema tree.  A list node may exist in multiple instances in the data
 *  tree.  Each such instance is known as a list entry.  The "list"
 *  statement takes one argument, which is an identifier, followed by a
 *  block of sub-statements that holds detailed list information.
 *
 *  A list entry is uniquely identified by the values of the list's keys,
 *  if defined.
 *
 *  The list's sub-statements
 *
 *                +--------------+---------+-------------+------------------+
 *                | substatement | section | cardinality |data model mapping|
 *                +--------------+---------+-------------+------------------+
 *                | anyxml       | 7.10    | 0..n        |-not supported    |
 *                | choice       | 7.9     | 0..n        |-child nodes      |
 *                | config       | 7.19.1  | 0..1        |-boolean          |
 *                | container    | 7.5     | 0..n        |-child nodes      |
 *                | description  | 7.19.3  | 0..1        |-string           |
 *                | grouping     | 7.11    | 0..n        |-child nodes      |
 *                | if-feature   | 7.18.2  | 0..n        |-TODO             |
 *                | key          | 7.8.2   | 0..1        |-String list      |
 *                | leaf         | 7.6     | 0..n        |-YangLeaf         |
 *                | leaf-list    | 7.7     | 0..n        |-YangLeafList     |
 *                | list         | 7.8     | 0..n        |-child nodes      |
 *                | max-elements | 7.7.4   | 0..1        |-int              |
 *                | min-elements | 7.7.3   | 0..1        |-int              |
 *                | must         | 7.5.3   | 0..n        |-TODO             |
 *                | ordered-by   | 7.7.5   | 0..1        |-TODO             |
 *                | reference    | 7.19.4  | 0..1        |-string           |
 *                | status       | 7.19.2  | 0..1        |-YangStatus       |
 *                | typedef      | 7.3     | 0..n        |-child nodes      |
 *                | unique       | 7.8.3   | 0..n        |-TODO             |
 *                | uses         | 7.12    | 0..n        |-child nodes(TODO)|
 *                | when         | 7.19.5  | 0..1        |-TODO             |
 *                +--------------+---------+-------------+------------------+
 */

/**
 * List data represented in YANG.
 */
public class YangList extends YangNode
        implements YangLeavesHolder, YangCommonInfo, Parsable {

    /**
     * name of the YANG list.
     */
    private String name;

    /**
     * If list maintains config data.
     */
    private boolean isConfig;

    /**
     * Description of list.
     */
    private String description;

    /**
     * Reference RFC 6020.
     *
     * The "key" statement, which MUST be present if the list represents
     * configuration, and MAY be present otherwise, takes as an argument a
     * string that specifies a space-separated list of leaf identifiers of this
     * list. A leaf identifier MUST NOT appear more than once in the key. Each
     * such leaf identifier MUST refer to a child leaf of the list. The leafs
     * can be defined directly in sub-statements to the list, or in groupings
     * used in the list.
     *
     * The combined values of all the leafs specified in the key are used to
     * uniquely identify a list entry. All key leafs MUST be given values when a
     * list entry is created. Thus, any default values in the key leafs or their
     * types are ignored. It also implies that any mandatory statement in the
     * key leafs are ignored.
     *
     * A leaf that is part of the key can be of any built-in or derived type,
     * except it MUST NOT be the built-in type "empty".
     *
     * All key leafs in a list MUST have the same value for their "config" as
     * the list itself.
     *
     * List of key leaf names.
     */
    private List<String> keyList;

    /**
     * List of leaves.
     */
    @SuppressWarnings("rawtypes")
    private List<YangLeaf> listOfLeaf;

    /**
     * List of leaf-lists.
     */
    @SuppressWarnings("rawtypes")
    private List<YangLeafList> listOfLeafList;

    /**
     * The "max-elements" statement, which is optional, takes as an argument a
     * positive integer or the string "unbounded", which puts a constraint on
     * valid list entries. A valid leaf-list or list always has at most
     * max-elements entries.
     *
     * If no "max-elements" statement is present, it defaults to "unbounded".
     */
    private int maxElelements;

    /**
     * The "min-elements" statement, which is optional, takes as an argument a
     * non-negative integer that puts a constraint on valid list entries. A
     * valid leaf-list or list MUST have at least min-elements entries.
     *
     * If no "min-elements" statement is present, it defaults to zero.
     *
     * The behavior of the constraint depends on the type of the leaf-list's or
     * list's closest ancestor node in the schema tree that is not a non-
     * presence container:
     *
     * o If this ancestor is a case node, the constraint is enforced if any
     * other node from the case exists.
     *
     * o Otherwise, it is enforced if the ancestor node exists.
     */
    private int minElements;

    /**
     * reference.
     */
    private String reference;

    /**
     * Status of the node.
     */

    private YangStatusType status;

    /**
     * Constructor.
     *
     * @param type list node
     */
    public YangList(YangNodeType type) {
        super(type);
    }

    /* (non-Javadoc)
     * @see org.onosproject.yangutils.datamodel.YangNode#getName()
     */
    @Override
    public String getName() {
        return name;
    }

    /* (non-Javadoc)
     * @see org.onosproject.yangutils.datamodel.YangNode#setName(java.lang.String)
     */
    @Override
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Get the config flag.
     *
     * @return the isConfig
     */
    public boolean isConfig() {
        return isConfig;
    }

    /**
     * Set the config flag.
     *
     * @param isCfg the config flag.
     */
    public void setConfig(boolean isCfg) {
        isConfig = isCfg;
    }

    /**
     * Get the description.
     *
     * @return the description.
     */
    public String getDescription() {
        return description;
    }

    /**
     * Set the description.
     *
     * @param description set the description.
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Get the list of key field names.
     *
     * @return the list of key field names.
     */
    public List<String> getKeyList() {
        return keyList;
    }

    /**
     * Set the list of key field names.
     *
     * @param keyList the list of key field names.
     */
    private void setKeyList(List<String> keyList) {
        this.keyList = keyList;
    }

    /**
     * Add a key field name.
     *
     * @param key key field name.
     */
    public void addKey(String key) {
        if (getKeyList() == null) {
            setKeyList(new LinkedList<String>());
        }

        getKeyList().add(key);
    }

    /**
     * Get the list of leaves.
     *
     * @return the list of leaves.
     */
    @SuppressWarnings("rawtypes")
    public List<YangLeaf> getListOfLeaf() {
        return listOfLeaf;
    }

    /**
     * Set the list of leaves.
     *
     * @param leafsList the list of leaf to set.
     */
    @SuppressWarnings("rawtypes")
    private void setListOfLeaf(List<YangLeaf> leafsList) {
        listOfLeaf = leafsList;
    }

    /**
     * Add a leaf.
     *
     * @param leaf the leaf to be added.
     */
    @SuppressWarnings("rawtypes")
    public void addLeaf(YangLeaf<?> leaf) {
        if (getListOfLeaf() == null) {
            setListOfLeaf(new LinkedList<YangLeaf>());
        }

        getListOfLeaf().add(leaf);
    }

    /**
     * Get the list of leaf-list.
     *
     * @return the list of leaf-list.
     */
    @SuppressWarnings("rawtypes")
    public List<YangLeafList> getListOfLeafList() {
        return listOfLeafList;
    }

    /**
     * Set the list of leaf-list.
     *
     * @param listOfLeafList the list of leaf-list to set.
     */
    @SuppressWarnings("rawtypes")
    private void setListOfLeafList(List<YangLeafList> listOfLeafList) {
        this.listOfLeafList = listOfLeafList;
    }

    /**
     * Add a leaf-list.
     *
     * @param leafList the leaf-list to be added.
     */
    @SuppressWarnings("rawtypes")
    public void addLeafList(YangLeafList<?> leafList) {
        if (getListOfLeafList() == null) {
            setListOfLeafList(new LinkedList<YangLeafList>());
        }

        getListOfLeafList().add(leafList);
    }

    /**
     * Get the max elements.
     *
     * @return the max elements.
     */
    public int getMaxElelements() {
        return maxElelements;
    }

    /**
     * Set the max elements.
     *
     * @param maxElelements the max elements.
     */
    public void setMaxElelements(int maxElelements) {
        this.maxElelements = maxElelements;
    }

    /**
     * Get the minimum elements.
     *
     * @return the minimum elements.
     */
    public int getMinElements() {
        return minElements;
    }

    /**
     * Set the minimum elements.
     *
     * @param minElements the minimum elements.
     */
    public void setMinElements(int minElements) {
        this.minElements = minElements;
    }

    /**
     * Get the textual reference.
     *
     * @return the reference.
     */
    public String getReference() {
        return reference;
    }

    /**
     * Set the textual reference.
     *
     * @param reference the reference to set.
     */
    public void setReference(String reference) {
        this.reference = reference;
    }

    /**
     * Get the status.
     *
     * @return the status.
     */
    public YangStatusType getStatus() {
        return status;
    }

    /**
     * Set the status.
     *
     * @param status the status to set.
     */
    public void setStatus(YangStatusType status) {
        this.status = status;
    }

    /**
     * Returns the type of the parsed data.
     *
     * @return returns LIST_DATA.
     */
    public ParsableDataType getParsableDataType() {
        return ParsableDataType.LIST_DATA;
    }

    /**
     * Validate the data on entering the corresponding parse tree node.
     *
     * @throws DataModelException a violation of data model rules.
     */
    public void validateDataOnEntry() throws DataModelException {
        // TODO auto-generated method stub, to be implemented by parser
    }

    /**
     * Validate the data on exiting the corresponding parse tree node.
     *
     * @throws DataModelException a violation of data model rules.
     */
    public void validateDataOnExit() throws DataModelException {
        // TODO auto-generated method stub, to be implemented by parser
    }

    /* (non-Javadoc)
     * @see org.onosproject.yangutils.translator.CodeGenerator#generateJavaCodeEntry()
     */
    public void generateJavaCodeEntry() {
        // TODO Auto-generated method stub

    }

    /* (non-Javadoc)
     * @see org.onosproject.yangutils.translator.CodeGenerator#generateJavaCodeExit()
     */
    public void generateJavaCodeExit() {
        // TODO Auto-generated method stub

    }

    /* (non-Javadoc)
     * @see org.onosproject.yangutils.datamodel.YangNode#getPackage()
     */
    @Override
    public String getPackage() {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see org.onosproject.yangutils.datamodel.YangNode#setPackage(java.lang.String)
     */
    @Override
    public void setPackage(String pkg) {
        // TODO Auto-generated method stub

    }
}

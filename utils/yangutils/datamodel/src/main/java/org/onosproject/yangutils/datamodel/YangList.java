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

/*
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
 * Represents list data represented in YANG.
 */
public class YangList
        extends YangNode
        implements YangLeavesHolder, YangCommonInfo, Parsable, CollisionDetector, YangAugmentationHolder {

    private static final long serialVersionUID = 806201609L;

    /**
     * Name of the YANG list.
     */
    private String name;

    /**
     * If list maintains config data.
     */
    private Boolean isConfig;

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
    private List<YangLeaf> listOfLeaf;

    /**
     * List of leaf-lists.
     */
    private List<YangLeafList> listOfLeafList;

    /**
     * Reference RFC 6020.
     *
     * The "max-elements" statement, which is optional, takes as an argument a
     * positive integer or the string "unbounded", which puts a constraint on
     * valid list entries. A valid leaf-list or list always has at most
     * max-elements entries.
     *
     * If no "max-elements" statement is present, it defaults to "unbounded".
     */
    private int maxElements = Integer.MAX_VALUE;

    /**
     * Reference RFC 6020.
     *
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
    private int minElements = 0;

    /**
     * reference.
     */
    private String reference;

    /**
     * Status of the node.
     */

    private YangStatusType status = YangStatusType.CURRENT;

    /**
     * Creates a YANG list object.
     */
    public YangList() {
        super(YangNodeType.LIST_NODE);
    }

    /**
     * Returns the YANG list name.
     *
     * @return YANG list name
     */
    @Override
    public String getName() {
        return name;
    }

    /**
     * Sets the YANG list name.
     *
     * @param name YANG list name
     */
    @Override
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Returns the config flag.
     *
     * @return the isConfig
     */
    public Boolean isConfig() {
        return isConfig;
    }

    /**
     * Sets the config flag.
     *
     * @param isCfg the config flag
     */
    public void setConfig(boolean isCfg) {
        isConfig = isCfg;
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
     * Returns the list of key field names.
     *
     * @return the list of key field names
     */
    public List<String> getKeyList() {
        return keyList;
    }

    /**
     * Sets the list of key field names.
     *
     * @param keyList the list of key field names
     */
    private void setKeyList(List<String> keyList) {
        this.keyList = keyList;
    }

    /**
     * Adds a key field name.
     *
     * @param key key field name.
     * @throws DataModelException a violation of data model rules
     */
    public void addKey(String key)
            throws DataModelException {
        if (getKeyList() == null) {
            setKeyList(new LinkedList<String>());
        }

        if (getKeyList().contains(key)) {
            throw new DataModelException("A leaf identifier must not appear more than once in the\n" +
                    "   key");
        }

        getKeyList().add(key);
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
     * Returns the max elements.
     *
     * @return the max elements
     */
    public int getMaxElements() {
        return maxElements;
    }

    /**
     * Sets the max elements.
     *
     * @param max the max elements
     */
    public void setMaxElements(int max) {
        maxElements = max;
    }

    /**
     * Returns the minimum elements.
     *
     * @return the minimum elements
     */
    public int getMinElements() {
        return minElements;
    }

    /**
     * Sets the minimum elements.
     *
     * @param minElements the minimum elements
     */
    public void setMinElements(int minElements) {
        this.minElements = minElements;
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
     * Returns the type of the parsed data.
     *
     * @return returns LIST_DATA
     */
    @Override
    public YangConstructType getYangConstructType() {
        return YangConstructType.LIST_DATA;
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
        List<String> keys = getKeyList();
        List<YangLeaf> leaves = getListOfLeaf();
        List<YangLeafList> leafLists = getListOfLeafList();

        setDefaultConfigValueToChild(leaves, leafLists);
        validateConfig(leaves, leafLists);

        /* A list must have atleast one key leaf if config is true */
        if (isConfig && (keys == null || leaves == null && leafLists == null) && !isUsesPresentInList()
                && !isListPresentInGrouping()) {
            throw new DataModelException("A list must have atleast one key leaf if config is true;");
        } else if (keys != null) {
            validateKey(leaves, leafLists, keys);
        }
    }

    /**
     * Sets the config's value to all leaf if leaf's config statement is not
     * specified.
     *
     * @param leaves list of leaf attributes of YANG list
     * @param leafLists list of leaf-list attributes of YANG list
     */
    private void setDefaultConfigValueToChild(List<YangLeaf> leaves, List<YangLeafList> leafLists) {

        /*
         * If "config" is not specified, the default is the same as the parent
         * schema node's "config" value.
         */
        if (leaves != null) {
            for (YangLeaf leaf : leaves) {
                if (leaf.isConfig() == null) {
                    leaf.setConfig(isConfig);
                }
            }
        }

        /*
         * If "config" is not specified, the default is the same as the parent
         * schema node's "config" value.
         */
        if (leafLists != null) {
            for (YangLeafList leafList : leafLists) {
                if (leafList.isConfig() == null) {
                    leafList.setConfig(isConfig);
                }
            }
        }
    }

    /**
     * Validates config statement of YANG list.
     *
     * @param leaves list of leaf attributes of YANG list
     * @param leafLists list of leaf-list attributes of YANG list
     * @throws DataModelException a violation of data model rules
     */
    private void validateConfig(List<YangLeaf> leaves, List<YangLeafList> leafLists)
            throws DataModelException {

        /*
         * If a node has "config" set to "false", no node underneath it can have
         * "config" set to "true".
         */
        if (!isConfig && leaves != null) {
            for (YangLeaf leaf : leaves) {
                if (leaf.isConfig()) {
                    throw new DataModelException("If a list has \"config\" set to \"false\", no node underneath " +
                            "it can have \"config\" set to \"true\".");
                }
            }
        }

        if (!isConfig && leafLists != null) {
            for (YangLeafList leafList : leafLists) {
                if (leafList.isConfig()) {
                    throw new DataModelException("If a list has \"config\" set to \"false\", no node underneath " +
                            "it can have \"config\" set to \"true\".");
                }
            }
        }
    }

    /**
     * Validates key statement of list.
     *
     * @param leaves list of leaf attributes of list
     * @param leafLists list of leaf-list attributes of list
     * @param keys list of key attributes of list
     * @throws DataModelException a violation of data model rules
     */
    private void validateKey(List<YangLeaf> leaves, List<YangLeafList> leafLists, List<String> keys)
            throws
            DataModelException {
        boolean leafFound = false;
        List<YangLeaf> keyLeaves = new LinkedList<>();
        List<YangLeafList> keyLeafLists = new LinkedList<>();

        /*
         * 1. Leaf identifier must refer to a child leaf of the list 2. A leaf
         * that is part of the key must not be the built-in type "empty".
         */
        for (String key : keys) {
            if (leaves != null && !leaves.isEmpty()) {
                for (YangLeaf leaf : leaves) {
                    if (key.equals(leaf.getName())) {
                        if (leaf.getDataType().getDataType() == YangDataTypes.EMPTY) {
                            throw new DataModelException(" A leaf that is part of the key must not be the built-in " +
                                    "type \"empty\".");
                        }
                        leafFound = true;
                        keyLeaves.add(leaf);
                        break;
                    }
                }
            }

            if (leafLists != null && !leafLists.isEmpty()) {
                for (YangLeafList leafList : leafLists) {
                    if (key.equals(leafList.getName())) {
                        if (leafList.getDataType().getDataType() == YangDataTypes.EMPTY) {
                            throw new DataModelException(" A leaf-list that is part of the key" +
                                    " must not be the built-in type \"empty\".");
                        }
                        leafFound = true;
                        keyLeafLists.add(leafList);
                        break;
                    }
                }
            }

            if (!leafFound && !isUsesPresentInList() && !isListPresentInGrouping()) {
                throw new DataModelException("An identifier, in key, must refer to a child leaf of the list");
            }
            leafFound = false;
        }

        /*
         * All key leafs in a list MUST have the same value for their "config"
         * as the list itself.
         */
        for (YangLeaf keyLeaf : keyLeaves) {
            if (isConfig != keyLeaf.isConfig()) {
                throw new DataModelException("All key leafs in a list must have the same value for their" +
                        " \"config\" as the list itself.");
            }
        }

         /*
         * All key leafs in a list MUST have the same value for their "config"
         * as the list itself.
         */
        for (YangLeafList keyLeafList : keyLeafLists) {
            if (isConfig() != keyLeafList.isConfig()) {
                throw new DataModelException("All key leaf-lists in a list must have the same value for their" +
                        " \"config\" as the list itself.");
            }
        }
    }

    @Override
    public void detectCollidingChild(String identifierName, YangConstructType dataType)
            throws DataModelException {
        // Asks helper to detect colliding child.
        detectCollidingChildUtil(identifierName, dataType, this);
    }

    @Override
    public void detectSelfCollision(String identifierName, YangConstructType dataType)
            throws DataModelException {
        if (getName().equals(identifierName)) {
            throw new DataModelException("YANG file error: Duplicate input identifier detected, same as list \"" +
                    getName() + "\"");
        }
    }

    private boolean isUsesPresentInList() {
        YangNode node = getChild();
        while (node != null) {
            if (node instanceof YangUses) {
                return true;
            }
            node = node.getNextSibling();
        }
        return false;
        // TODO When grouping linking is done this method has to be modified.
    }

    private boolean isListPresentInGrouping() {
        YangNode node = getParent();
        while (node != null) {
            if (node instanceof YangGrouping) {
                return true;
            }
            node = node.getParent();
        }
        return false;
        // TODO When grouping linking is done this method has to be modified.
    }
}

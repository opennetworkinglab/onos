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
import org.onosproject.yangutils.utils.io.CachedFileHandle;
/*-
 * Reference RFC 6020.
 *
 * The "container" statement is used to define an interior data node in the
 * schema tree. It takes one argument, which is an identifier, followed by a
 * block of sub-statements that holds detailed container information.
 *
 * A container node does not have a value, but it has a list of child nodes in
 * the data tree. The child nodes are defined in the container's sub-statements.
 *
 * Containers with Presence
 *
 * YANG supports two styles of containers, those that exist only for organizing
 * the hierarchy of data nodes, and those whose presence in the configuration
 * has an explicit meaning.
 *
 * In the first style, the container has no meaning of its own, existing only to
 * contain child nodes. This is the default style.
 *
 * For example, the set of scrambling options for Synchronous Optical Network
 * (SONET) interfaces may be placed inside a "scrambling" container to enhance
 * the organization of the configuration hierarchy, and to keep these nodes
 * together. The "scrambling" node itself has no meaning, so removing the node
 * when it becomes empty relieves the user from performing this task.
 *
 * In the second style, the presence of the container itself is configuration
 * data, representing a single bit of configuration data. The container acts as
 * both a configuration knob and a means of organizing related configuration.
 * These containers are explicitly created and deleted.
 *
 * YANG calls this style a "presence container" and it is indicated using the
 * "presence" statement, which takes as its argument a text string indicating
 * what the presence of the node means.
 *
 * The container's Substatements
 *
 *                +--------------+---------+-------------+------------------+
 *                | substatement | section | cardinality |data model mapping|
 *                +--------------+---------+-------------+------------------+
 *                | anyxml       | 7.10    | 0..n        | -not supported   |
 *                | choice       | 7.9     | 0..n        | -child nodes     |
 *                | config       | 7.19.1  | 0..1        | -boolean         |
 *                | container    | 7.5     | 0..n        | -child nodes     |
 *                | description  | 7.19.3  | 0..1        | -string          |
 *                | grouping     | 7.11    | 0..n        | -child nodes     |
 *                | if-feature   | 7.18.2  | 0..n        | -TODO            |
 *                | leaf         | 7.6     | 0..n        | -YangLeaf        |
 *                | leaf-list    | 7.7     | 0..n        | -YangLeafList    |
 *                | list         | 7.8     | 0..n        | -child nodes     |
 *                | must         | 7.5.3   | 0..n        | -TODO            |
 *                | presence     | 7.5.5   | 0..1        | -boolean         |
 *                | reference    | 7.19.4  | 0..1        | -string          |
 *                | status       | 7.19.2  | 0..1        | -YangStatus      |
 *                | typedef      | 7.3     | 0..n        | -child nodes     |
 *                | uses         | 7.12    | 0..n        | -child nodes     |
 *                | when         | 7.19.5  | 0..1        | -TODO            |
 *                +--------------+---------+-------------+------------------+
 */

/**
 * Data model node to maintain information defined in YANG container.
 */
public class YangContainer extends YangNode implements YangLeavesHolder, YangCommonInfo, Parsable {

    /**
     * Name of the container.
     */
    private String name;

    /**
     * If container maintains config data.
     */
    private boolean isConfig;

    /**
     * Description of container.
     */
    private String description;

    /**
     * List of leaves contained.
     */
    @SuppressWarnings("rawtypes")
    private List<YangLeaf> listOfLeaf;

    /**
     * List of leaf-lists contained.
     */
    @SuppressWarnings("rawtypes")
    private List<YangLeafList> listOfLeafList;

    /**
     * If it is a presence container, then the textual documentation of presence
     * usage.
     */
    private String presence;

    /**
     * Reference of the module.
     */
    private String reference;

    /**
     * Status of the node.
     */
    private YangStatusType status;

    /**
     * package of the generated java code.
     */
    private String pkg;

    /**
     * Cached Java File Handle.
     */
    private CachedFileHandle fileHandle;

    /**
     * Create a container node.
     */
    public YangContainer() {
        super(YangNodeType.CONTAINER_NODE);
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
     * Get the presence string if present.
     *
     * @return the isPressence.
     */
    public String getPresence() {
        return presence;
    }

    /**
     * Set the presence string.
     *
     * @param presence the presence flag
     */
    public void setPresence(String presence) {
        this.presence = presence;
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
     * Get the cached file handle.
     *
     * @return the fileHandle
     */
    public CachedFileHandle getFileHandle() {
        return fileHandle;
    }

    /**
     * Set the cached file handle.
     *
     * @param handle the fileHandle to set
     */
    public void setFileHandle(CachedFileHandle handle) {
        fileHandle = handle;
    }

    /**
     * Returns the type of the data.
     *
     * @return returns CONTAINER_DATA.
     */
    public ParsableDataType getParsableDataType() {
        return ParsableDataType.CONTAINER_DATA;
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
     * @param pcg the package to set
     */
    @Override
    public void setPackage(String pcg) {
        pkg = pcg;
    }

    /**
     * Generate the java code corresponding to YANG container.
     */
    public void generateJavaCodeEntry() {
        //TODO: autogenerated method stub, to be implemented
    return;
    }

    /**
     * Free resources used to generate code.
     */
    public void generateJavaCodeExit() {
          //TODO: autogenerated method stub, to be implemented
        return;
    }
}

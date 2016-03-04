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

import static org.onosproject.yangutils.datamodel.utils.DataModelUtils.detectCollidingChildUtil;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import org.onosproject.yangutils.datamodel.exceptions.DataModelException;
import org.onosproject.yangutils.parser.Parsable;
import org.onosproject.yangutils.translator.CachedFileHandle;
import org.onosproject.yangutils.translator.GeneratedFileType;
import org.onosproject.yangutils.translator.tojava.utils.JavaIdentifierSyntax;
import org.onosproject.yangutils.utils.UtilConstants;
import org.onosproject.yangutils.utils.YangConstructType;
import org.onosproject.yangutils.utils.io.impl.FileSystemUtil;
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
public class YangContainer extends YangNode implements YangLeavesHolder, YangCommonInfo, Parsable, CollisionDetector {

    /**
     * Name of the container.
     */
    private String name;

    /**
     * If container maintains config data.
     */
    private Boolean isConfig;

    /**
     * Description of container.
     */
    private String description;

    /**
     * List of leaves contained.
     */
    private List<YangLeaf> listOfLeaf;

    /**
     * List of leaf-lists contained.
     */
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
    private YangStatusType status = YangStatusType.CURRENT;

    /**
     * Package of the generated java code.
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

    /**
     * Get the YANG name of container.
     *
     * @return the name of container as defined in YANG file
     */
    @Override
    public String getName() {
        return name;
    }

    /**
     * Set the YANG name of container.
     *
     * @param name the name of container as defined in YANG file
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
    public Boolean isConfig() {
        return isConfig;
    }

    /**
     * Set the config flag.
     *
     * @param isCfg the config flag
     */
    public void setConfig(boolean isCfg) {
        isConfig = isCfg;
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
     * Get the presence string if present.
     *
     * @return the presence
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
     * Get the cached file handle.
     *
     * @return the fileHandle
     */
    @Override
    public CachedFileHandle getFileHandle() {
        return fileHandle;
    }

    /**
     * Set the cached file handle.
     *
     * @param handle the fileHandle to set
     */
    @Override
    public void setFileHandle(CachedFileHandle handle) {
        fileHandle = handle;
    }

    /**
     * Returns the type of the data.
     *
     * @return returns CONTAINER_DATA
     */
    @Override
    public YangConstructType getYangConstructType() {
        return YangConstructType.CONTAINER_DATA;
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
        List<YangLeaf> leaves = getListOfLeaf();
        List<YangLeafList> leafLists = getListOfLeafList();

        setDefaultConfigValueToChild(leaves, leafLists);
        validateConfig(leaves, leafLists);
    }

    /**
     * Sets the config's value to all leaf if leaf's config statement is not
     * specified.
     *
     * @param leaves list of leaf attributes of container
     * @param leafLists list of leaf-list attributes of container
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
     * Validates config statement of container.
     *
     * @param leaves list of leaf attributes of container
     * @param leafLists list of leaf-list attributes of container
     * @throws DataModelException a violation of data model rules
     */
    private void validateConfig(List<YangLeaf> leaves, List<YangLeafList> leafLists) throws DataModelException {

        /*
         * If a node has "config" set to "false", no node underneath it can have
         * "config" set to "true".
         */
        if ((!isConfig) && (leaves != null)) {
            for (YangLeaf leaf : leaves) {
                if (leaf.isConfig()) {
                    throw new DataModelException("If a container has \"config\" set to \"false\", no node underneath " +
                            "it can have \"config\" set to \"true\".");
                }
            }
        }

        if ((!isConfig) && (leafLists != null)) {
            for (YangLeafList leafList : leafLists) {
                if (leafList.isConfig()) {
                    throw new DataModelException("If a container has \"config\" set to \"false\", no node underneath " +
                            "it can have \"config\" set to \"true\".");
                }
            }
        }
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
     *
     * @param codeGenDir code generation directory
     * @throws IOException when fails to generate the source files.
     */
    @Override
    public void generateJavaCodeEntry(String codeGenDir) throws IOException {
        YangNode parent = getParent();
        String contPkg = JavaIdentifierSyntax.getPackageFromParent(parent.getPackage(), parent.getName());

        contPkg = JavaIdentifierSyntax.getCamelCase(contPkg).toLowerCase();
        setPackage(contPkg);

        CachedFileHandle handle = null;
        try {
            FileSystemUtil.createPackage(codeGenDir + getPackage(), parent.getName() + UtilConstants.CHILDREN);
            handle = FileSystemUtil.createSourceFiles(getPackage(), getName(),
                    GeneratedFileType.GENERATE_INTERFACE_WITH_BUILDER);
            handle.setRelativeFilePath(getPackage().replace(".", "/"));
            handle.setCodeGenFilePath(codeGenDir);
        } catch (IOException e) {
            throw new IOException("Failed to create the source files.");
        }
        setFileHandle(handle);

        addLeavesAttributes();
        addLeafListAttributes();
        addAttributeInParent();
    }

    /**
     * Adds current node attribute to parent file.
     */
    private void addAttributeInParent() {
        if (getParent() != null) {
            getParent().getFileHandle().addAttributeInfo(null, getName(), false);
        }
    }

    @Override
    public void generateJavaCodeExit() throws IOException {
        getFileHandle().close();
        return;
    }

    /**
     * Adds leaf attributes in generated files.
     */
    private void addLeavesAttributes() {

        List<YangLeaf> leaves = getListOfLeaf();
        if (leaves != null) {
            for (YangLeaf leaf : leaves) {
                getFileHandle().addAttributeInfo(leaf.getDataType(), leaf.getLeafName(), false);
            }
        }
    }

    /**
     * Adds leaf list's attributes in generated files.
     */
    private void addLeafListAttributes() {
        List<YangLeafList> leavesList = getListOfLeafList();
        if (leavesList != null) {
            for (YangLeafList leafList : leavesList) {
                getFileHandle().addAttributeInfo(leafList.getDataType(), leafList.getLeafName(), true);
            }
        }
        return;
    }

    @Override
    public void detectCollidingChild(String identifierName, YangConstructType dataType) throws DataModelException {
        // Asks helper to detect colliding child.
        detectCollidingChildUtil(identifierName, dataType, this);
    }

    @Override
    public void detectSelfCollision(String identifierName, YangConstructType dataType) throws DataModelException {
        if (this.getName().equals(identifierName)) {
            throw new DataModelException("YANG file error: Duplicate input identifier detected, same as container \""
                    + this.getName() + "\"");
        }
    }
}

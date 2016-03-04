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

import org.onosproject.yangutils.datamodel.exceptions.DataModelException;
import static org.onosproject.yangutils.datamodel.utils.DataModelUtils.detectCollidingChildUtil;
import org.onosproject.yangutils.parser.Parsable;
import org.onosproject.yangutils.translator.CachedFileHandle;
import org.onosproject.yangutils.utils.YangConstructType;

import java.util.LinkedList;
import java.util.List;

/*
 *  Reference RFC 6020.
 *
 *  While the primary unit in YANG is a module, a YANG module can itself
 *  be constructed out of several submodules.  Submodules allow a module
 *  designer to split a complex model into several pieces where all the
 *  submodules contribute to a single namespace, which is defined by the
 *  module that includes the submodules.
 *
 *  The "submodule" statement defines the submodule's name, and groups
 *  all statements that belong to the submodule together.  The
 *  "submodule" statement's argument is the name of the submodule,
 *  followed by a block of sub-statements that hold detailed submodule
 *  information.
 *
 *  The submodule's sub-statements
 *
 *                +--------------+---------+-------------+------------------+
 *                | substatement | section | cardinality |data model mapping|
 *                +--------------+---------+-------------+------------------+
 *                | anyxml       | 7.10    | 0..n        | - not supported  |
 *                | augment      | 7.15    | 0..n        | - child nodes    |
 *                | belongs-to   | 7.2.2   | 1           | - YangBelongsTo  |
 *                | choice       | 7.9     | 0..n        | - child nodes    |
 *                | contact      | 7.1.8   | 0..1        | - string         |
 *                | container    | 7.5     | 0..n        | - child nodes    |
 *                | description  | 7.19.3  | 0..1        | - string         |
 *                | deviation    | 7.18.3  | 0..n        | - TODO           |
 *                | extension    | 7.17    | 0..n        | - TODO           |
 *                | feature      | 7.18.1  | 0..n        | - TODO           |
 *                | grouping     | 7.11    | 0..n        | - child nodes    |
 *                | identity     | 7.16    | 0..n        | - TODO           |
 *                | import       | 7.1.5   | 0..n        | - YangImport     |
 *                | include      | 7.1.6   | 0..n        | - YangInclude    |
 *                | leaf         | 7.6     | 0..n        | - YangLeaf       |
 *                | leaf-list    | 7.7     | 0..n        | - YangLeafList   |
 *                | list         | 7.8     | 0..n        | - child nodes    |
 *                | notification | 7.14    | 0..n        | - TODO           |
 *                | organization | 7.1.7   | 0..1        | - string         |
 *                | reference    | 7.19.4  | 0..1        | - string         |
 *                | revision     | 7.1.9   | 0..n        | - string         |
 *                | rpc          | 7.13    | 0..n        | - TODO           |
 *                | typedef      | 7.3     | 0..n        | - child nodes    |
 *                | uses         | 7.12    | 0..n        | - child nodes    |
 *                | YANG-version | 7.1.2   | 0..1        | - int            |
 *                +--------------+---------+-------------+------------------+
 */
/**
 * Data model node to maintain information defined in YANG sub-module.
 */
public class YangSubModule extends YangNode
        implements YangLeavesHolder, YangDesc, YangReference, Parsable, CollisionDetector {

    /**
     * Name of sub module.
     */
    private String name;

    /**
     * Module to which it belongs to.
     */
    private YangBelongsTo belongsTo;

    /**
     * Reference RFC 6020.
     *
     * The "contact" statement provides contact information for the module. The
     * argument is a string that is used to specify contact information for the
     * person or persons to whom technical queries concerning this module should
     * be sent, such as their name, postal address, telephone number, and
     * electronic mail address.
     */
    private String contact;

    /**
     * Description.
     */
    private String description;

    /**
     * List of YANG modules imported.
     */
    private List<YangImport> importList;

    /**
     * List of YANG sub-modules included.
     */
    private List<YangInclude> includeList;

    /**
     * List of leaves at root level in the sub-module.
     */
    private List<YangLeaf> listOfLeaf;

    /**
     * List of leaf-lists at root level in the sub-module.
     */
    private List<YangLeafList> listOfLeafList;

    /**
     * organization owner of the sub-module.
     */
    private String organization;

    /**
     * reference of the sub-module.
     */
    private String reference;

    /**
     * revision info of the sub-module.
     */
    private YangRevision revision;

    /**
     * YANG version.
     */
    private byte version;

    /**
     * package of the generated java code.
     */
    private String pkg;

    /**
     * Create a sub module node.
     */
    public YangSubModule() {
        super(YangNodeType.SUB_MODULE_NODE);
    }

    /**
     * Get the YANG name of the sub module.
     *
     * @return YANG name of the sub module
     */
    @Override
    public String getName() {
        return name;
    }

    /**
     * Set YANG name of the sub module.
     *
     * @param subModuleName YANG name of the sub module
     */
    @Override
    public void setName(String subModuleName) {
        name = subModuleName;
    }

    /**
     * Get the module info.
     *
     * @return the belongs to info
     */
    public YangBelongsTo getBelongsTo() {
        return belongsTo;
    }

    /**
     * Set the module info.
     *
     * @param belongsTo module info to set
     */
    public void setBelongsTo(YangBelongsTo belongsTo) {
        this.belongsTo = belongsTo;
    }

    /**
     * Get the contact.
     *
     * @return the contact
     */
    public String getContact() {
        return contact;
    }

    /**
     * Set the contact.
     *
     * @param contact the contact to set
     */
    public void setContact(String contact) {
        this.contact = contact;
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
     * Get the list of imported modules.
     *
     * @return the list of imported modules
     */
    public List<YangImport> getImportList() {
        return importList;
    }

    /**
     * prevent setting the import list from outside.
     *
     * @param importList the import list to set
     */
    private void setImportList(List<YangImport> importList) {
        this.importList = importList;
    }

    /**
     * Add the imported module information to the import list.
     *
     * @param importedModule module being imported
     */
    public void addImportedInfo(YangImport importedModule) {

        if (getImportList() == null) {
            setImportList(new LinkedList<YangImport>());
        }

        getImportList().add(importedModule);

        return;
    }

    /**
     * Get the list of included sub modules.
     *
     * @return the included list of sub modules
     */
    public List<YangInclude> getIncludeList() {
        return includeList;
    }

    /**
     * Set the list of included sub modules.
     *
     * @param includeList the included list to set
     */
    private void setIncludeList(List<YangInclude> includeList) {
        this.includeList = includeList;
    }

    /**
     * Add the included sub module information to the include list.
     *
     * @param includeModule submodule being included
     */
    public void addIncludedInfo(YangInclude includeModule) {

        if (getIncludeList() == null) {
            setIncludeList(new LinkedList<YangInclude>());
        }

        getIncludeList().add(includeModule);
        return;
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
     * Get the sub-modules organization.
     *
     * @return the organization
     */
    public String getOrganization() {
        return organization;
    }

    /**
     * Set the sub-modules organization.
     *
     * @param org the organization to set
     */
    public void setOrganization(String org) {
        organization = org;
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
     * Get the revision.
     *
     * @return the revision
     */
    public YangRevision getRevision() {
        return revision;
    }

    /**
     * Set the revision.
     *
     * @param revision the revision to set
     */
    public void setRevision(YangRevision revision) {
        this.revision = revision;
    }

    /**
     * Get the version.
     *
     * @return the version
     */
    public byte getVersion() {
        return version;
    }

    /**
     * Set the version.
     *
     * @param version the version to set
     */
    public void setVersion(byte version) {
        this.version = version;
    }

    /**
     * Returns the type of the parsed data.
     *
     * @return returns SUB_MODULE_DATA
     */
    @Override
    public YangConstructType getYangConstructType() {
        return YangConstructType.SUB_MODULE_DATA;
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
     * Generates java code for sub-module.
     *
     * @param codeGenDir code generation directory.
     */
    @Override
    public void generateJavaCodeEntry(String codeGenDir) {
        // TODO Auto-generated method stub
    }

    /**
     * Free resources used to generate code.
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

    @Override
    public void detectCollidingChild(String identifierName, YangConstructType dataType) throws DataModelException {
        // Asks helper to detect colliding child.
        detectCollidingChildUtil(identifierName, dataType, this);
    }

    @Override
    public void detectSelfCollision(String identifierName, YangConstructType dataType) throws DataModelException {
        // Not required as module doesn't have any parent.
    }
}

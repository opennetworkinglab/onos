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
/*
 * Reference:RFC 6020.
 * The "module" statement defines the module's name,
 * and groups all statements that belong to the module together. The "module"
 * statement's argument is the name of the module, followed by a block of
 * sub statements that hold detailed module information.
 * The module's sub statements
 *
 *                +--------------+---------+-------------+-----------------------+
 *                |sub statement | section | cardinality | data model mapping    |
 *                +--------------+---------+-------------+-----------------------+
 *                | anyxml       | 7.10    | 0..n        | not supported         |
 *                | augment      | 7.15    | 0..n        | child nodes           |
 *                | choice       | 7.9     | 0..n        | child nodes           |
 *                | contact      | 7.1.8   | 0..1        | string                |
 *                | container    | 7.5     | 0..n        | child nodes           |
 *                | description  | 7.19.3  | 0..1        | string                |
 *                | deviation    | 7.18.3  | 0..n        | TODO                  |
 *                | extension    | 7.17    | 0..n        | TODO                  |
 *                | feature      | 7.18.1  | 0..n        | TODO                  |
 *                | grouping     | 7.11    | 0..n        | child nodes           |
 *                | identity     | 7.16    | 0..n        | TODO                  |
 *                | import       | 7.1.5   | 0..n        | list of import info   |
 *                | include      | 7.1.6   | 0..n        | list of include info  |
 *                | leaf         | 7.6     | 0..n        | list of leaf info     |
 *                | leaf-list    | 7.7     | 0..n        | list of leaf-list info|
 *                | list         | 7.8     | 0..n        | child nodes           |
 *                | namespace    | 7.1.3   | 1           | string/uri            |
 *                | notification | 7.14    | 0..n        | TODO                  |
 *                | organization | 7.1.7   | 0..1        | string                |
 *                | prefix       | 7.1.4   | 1           | string                |
 *                | reference    | 7.19.4  | 0..1        | string                |
 *                | revision     | 7.1.9   | 0..n        | revision              |
 *                | rpc          | 7.13    | 0..n        | TODO                  |
 *                | typedef      | 7.3     | 0..n        | child nodes           |
 *                | uses         | 7.12    | 0..n        | child nodes           |
 *                | YANG-version | 7.1.2   | 0..1        | int                   |
 *                +--------------+---------+-------------+-----------------------+
 */

import org.onosproject.yangutils.datamodel.exceptions.DataModelException;
import org.onosproject.yangutils.parser.Parsable;
import org.onosproject.yangutils.parser.ParsableDataType;

/**
 * Data model node to maintain information defined in YANG module.
 */
public class YangModule extends YangNode implements YangLeavesHolder, YangDesc, YangReference, Parsable {

    /**
     * Name of the module.
     */
    private String name;

    /**
     * Reference:RFC 6020.
     * The "contact" statement provides contact information for the module. The
     * argument is a string that is used to specify contact information for the
     * person or persons to whom technical queries concerning this module should
     * be sent, such as their name, postal address, telephone number, and
     * electronic mail address.
     */
    private String contact;

    /**
     * Reference:RFC 6020.
     * The "description" statement takes as an argument a string that contains a
     * human-readable textual description of this definition. The text is
     * provided in a language (or languages) chosen by the module developer; for
     * the sake of interoperability.
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
     * List of leaves at root level in the module.
     */
    @SuppressWarnings("rawtypes")
    private List<YangLeaf> listOfLeaf;

    /**
     * List of leaf-lists at root level in the module.
     */
    @SuppressWarnings("rawtypes")
    private List<YangLeafList> listOfLeafList;

    /**
     * Name space of the module.
     */
    private YangNameSpace nameSpace;

    /**
     * Reference:RFC 6020.
     * The "organization" statement defines the party responsible for this
     * module. The argument is a string that is used to specify a textual
     * description of the organization(s) under whose auspices this module was
     * developed.
     */
    private String organization;

    /**
     * Prefix to refer to the objects in module.
     */
    private String prefix;

    /**
     * Reference of the module.
     */
    private String reference;

    /**
     * Revision info of the module.
     */
    private YangRevision revision;

    /**
     * YANG version.
     */
    private byte version;

    /**
     * Create a YANG node of module type.
     */
    public YangModule() {
        super(YangNodeType.MODULE_NODE);
    }

    /**
     * Get the module name.
     *
     * @return the module name.
     */
    public String getName() {
        return name;
    }

    /**
     * set the module name.
     *
     * @param moduleName the module name to set.
     */
    public void setName(String moduleName) {
        name = moduleName;
    }

    /**
     * Get the contact details of the module owner.
     *
     * @return the contact details of YANG owner.
     */
    public String getContact() {
        return contact;
    }

    /**
     * Set the contact details of the module owner.
     *
     * @param contact the contact details of YANG owner.
     */
    public void setContact(String contact) {
        this.contact = contact;
    }

    /**
     * Get the description of module.
     *
     * @return the description of YANG module.
     */
    public String getDescription() {
        return description;
    }

    /**
     * Set the description of module.
     *
     * @param description set the description of YANG module.
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Get the list of imported modules.
     *
     * @return the list of imported modules.
     */
    public List<YangImport> getImportList() {
        return importList;
    }

    /**
     * prevent setting the import list from outside.
     *
     * @param importList the import list to set.
     */
    private void setImportList(List<YangImport> importList) {
        this.importList = importList;
    }

    /**
     * Add the imported module information to the import list.
     *
     * @param importedModule module being imported.
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
     * @return the included list of sub modules.
     */
    public List<YangInclude> getIncludeList() {
        return includeList;
    }

    /**
     * Set the list of included sub modules.
     *
     * @param includeList the included list to set.
     */
    private void setIncludeList(List<YangInclude> includeList) {
        this.includeList = includeList;
    }

    /**
     * Add the included sub module information to the include list.
     *
     * @param includeModule submodule being included.
     */
    public void addIncludedInfo(YangInclude includeModule) {

        if (getIncludeList() == null) {
            setIncludeList(new LinkedList<YangInclude>());
        }

        getIncludeList().add(includeModule);
        return;
    }

    /**
     * Get the list of leaves in module.
     *
     * @return the list of leaves.
     */
    @SuppressWarnings("rawtypes")
    public List<YangLeaf> getListOfLeaf() {
        return listOfLeaf;
    }

    /**
     * Set the list of leaf in module.
     *
     * @param leafsList the list of leaf to set.
     */
    @SuppressWarnings("rawtypes")
    private void setListOfLeaf(List<YangLeaf> leafsList) {
        listOfLeaf = leafsList;
    }

    /**
     * Add a leaf in module.
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
     * Get the list of leaf-list from module.
     *
     * @return the list of leaf-list.
     */
    @SuppressWarnings("rawtypes")
    public List<YangLeafList> getListOfLeafList() {
        return listOfLeafList;
    }

    /**
     * Set the list of leaf-list in module.
     *
     * @param listOfLeafList the list of leaf-list to set.
     */
    @SuppressWarnings("rawtypes")
    private void setListOfLeafList(List<YangLeafList> listOfLeafList) {
        this.listOfLeafList = listOfLeafList;
    }

    /**
     * Add a leaf-list in module.
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
     * Get the name space of module elements.
     *
     * @return the nameSpace.
     */
    public YangNameSpace getNameSpace() {
        return nameSpace;
    }

    /**
     * Set the name space of module elements.
     *
     * @param nameSpace the nameSpace to set.
     */
    public void setNameSpace(YangNameSpace nameSpace) {
        this.nameSpace = nameSpace;
    }

    /**
     * Get the modules organization.
     *
     * @return the organization.
     */
    public String getOrganization() {
        return organization;
    }

    /**
     * Set the modules organization.
     *
     * @param org the organization to set.
     */
    public void setOrganization(String org) {
        this.organization = org;
    }

    /**
     * Get the prefix.
     *
     * @return the prefix
     */
    public String getPrefix() {
        return prefix;
    }

    /**
     * Set the prefix.
     *
     * @param prefix the prefix to set.
     */
    public void setPrefix(String prefix) {
        this.prefix = prefix;
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
     * Get the revision.
     *
     * @return the revision.
     */
    public YangRevision getRevision() {
        return revision;
    }

    /**
     * Set the revision.
     *
     * @param revision the revision to set.
     */
    public void setRevision(YangRevision revision) {
        this.revision = revision;
    }

    /**
     * Get the version.
     *
     * @return the version.
     */
    public byte getVersion() {
        return version;
    }

    /**
     * Set the version.
     *
     * @param version the version to set.
     */
    public void setVersion(byte version) {
        this.version = version;
    }

    /**
     * Returns the type of the parsed data.
     *
     * @return returns MODULE_DATA.
     */
    public ParsableDataType getParsableDataType() {
        return ParsableDataType.MODULE_DATA;
    }

    /**
     * Validate the data on entering the corresponding parse tree node.
     *
     * @throws DataModelException a violation of data model rules
     */
    public void validateDataOnEntry() throws DataModelException {
        // TODO auto-generated method stub, to be implemented by parser
    }

    /**
     * Validate the data on exiting the corresponding parse tree node.
     *
     * @throws DataModelException a violation of data model rules
     */
    public void validateDataOnExit() throws DataModelException {
        // TODO auto-generated method stub, to be implemented by parser
    }
}

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

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.onosproject.yangutils.datamodel.exceptions.DataModelException;
import org.onosproject.yangutils.datamodel.utils.Parsable;
import org.onosproject.yangutils.datamodel.utils.YangConstructType;

import static org.onosproject.yangutils.datamodel.utils.DataModelUtils.detectCollidingChildUtil;
import static org.onosproject.yangutils.datamodel.utils.DataModelUtils.linkInterFileReferences;
import static org.onosproject.yangutils.datamodel.utils.DataModelUtils.resolveLinkingForResolutionList;

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
 * Represents data model node to maintain information defined in YANG sub-module.
 */
public class YangSubModule
        extends YangNode
        implements YangLeavesHolder, YangDesc, YangReference, Parsable, CollisionDetector, YangReferenceResolver,
        RpcNotificationContainer {

    private static final long serialVersionUID = 806201614L;

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
     * <p>
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
     * Organization owner of the sub-module.
     */
    private String organization;

    /**
     * Reference of the sub-module.
     */
    private String reference;

    /**
     * Revision info of the sub-module.
     */
    private YangRevision revision;

    /**
     * YANG version.
     */
    private byte version;

    /**
     * Prefix of parent module.
     */
    private String prefix;

    /*-
     * Reference RFC 6020.
     *
     * Nested typedefs and groupings.
     * Typedefs and groupings may appear nested under many YANG statements,
     * allowing these to be lexically scoped by the hierarchy under which
     * they appear.  This allows types and groupings to be defined near
     * where they are used, rather than placing them at the top level of the
     * hierarchy.  The close proximity increases readability.
     *
     * Scoping also allows types to be defined without concern for naming
     * conflicts between types in different submodules.  Type names can be
     * specified without adding leading strings designed to prevent name
     * collisions within large modules.
     *
     * Finally, scoping allows the module author to keep types and groupings
     * private to their module or submodule, preventing their reuse.  Since
     * only top-level types and groupings (i.e., those appearing as
     * sub-statements to a module or submodule statement) can be used outside
     * the module or submodule, the developer has more control over what
     * pieces of their module are presented to the outside world, supporting
     * the need to hide internal information and maintaining a boundary
     * between what is shared with the outside world and what is kept
     * private.
     *
     * Scoped definitions MUST NOT shadow definitions at a higher scope.  A
     * type or grouping cannot be defined if a higher level in the schema
     * hierarchy has a definition with a matching identifier.
     *
     * A reference to an unprefixed type or grouping, or one which uses the
     * prefix of the current module, is resolved by locating the closest
     * matching "typedef" or "grouping" statement among the immediate
     * sub-statements of each ancestor statement.
     */
    private List<YangResolutionInfo> derivedTypeResolutionList;

    /**
     * uses resolution list.
     */
    private List<YangResolutionInfo> usesResolutionList;

    /**
     * Creates a sub module node.
     */
    public YangSubModule() {
        super(YangNodeType.SUB_MODULE_NODE);
        derivedTypeResolutionList = new LinkedList<>();
        usesResolutionList = new LinkedList<>();
        importList = new LinkedList<YangImport>();
        includeList = new LinkedList<YangInclude>();
        listOfLeaf = new LinkedList<YangLeaf>();
        listOfLeafList = new LinkedList<YangLeafList>();
    }

    /**
     * Returns the YANG name of the sub module.
     *
     * @return YANG name of the sub module
     */
    @Override
    public String getName() {
        return name;
    }

    /**
     * Sets YANG name of the sub module.
     *
     * @param subModuleName YANG name of the sub module
     */
    @Override
    public void setName(String subModuleName) {
        name = subModuleName;
    }

    /**
     * Returns the module info.
     *
     * @return the belongs to info
     */
    public YangBelongsTo getBelongsTo() {
        return belongsTo;
    }

    /**
     * Sets the module info.
     *
     * @param belongsTo module info to set
     */
    public void setBelongsTo(YangBelongsTo belongsTo) {
        this.belongsTo = belongsTo;
    }

    /**
     * Returns the contact.
     *
     * @return the contact
     */
    public String getContact() {
        return contact;
    }

    /**
     * Sets the contact.
     *
     * @param contact the contact to set
     */
    public void setContact(String contact) {
        this.contact = contact;
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
     * Returns the list of imported modules.
     *
     * @return the list of imported modules
     */
    @Override
    public List<YangImport> getImportList() {
        return importList;
    }

    /**
     * Adds the imported module information to the import list.
     *
     * @param importedModule module being imported
     */
    @Override
    public void addToImportList(YangImport importedModule) {
        getImportList().add(importedModule);
    }

    @Override
    public void setImportList(List<YangImport> importList) {
        this.importList = importList;
    }

    /**
     * Returns the list of included sub modules.
     *
     * @return the included list of sub modules
     */
    @Override
    public List<YangInclude> getIncludeList() {
        return includeList;
    }

    /**
     * Returns the included sub module information to the include list.
     *
     * @param includeModule submodule being included
     */
    @Override
    public void addToIncludeList(YangInclude includeModule) {
        getIncludeList().add(includeModule);
    }

    @Override
    public void setIncludeList(List<YangInclude> includeList) {
        this.includeList = includeList;
    }

    @Override
    public String getPrefix() {
        return prefix;
    }

    @Override
    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    @Override
    public void resolveSelfFileLinking(ResolvableType type)
            throws DataModelException {
        // Get the list to be resolved.
        List<YangResolutionInfo> resolutionList = getUnresolvedResolutionList(type);
        // Resolve linking for a resolution list.
        resolveLinkingForResolutionList(resolutionList, this);
    }

    @Override
    public void resolveInterFileLinking(ResolvableType type)
            throws DataModelException {
        // Get the list to be resolved.
        List<YangResolutionInfo> resolutionList = getUnresolvedResolutionList(type);
        // Resolve linking for a resolution list.
        linkInterFileReferences(resolutionList, this);
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
        getListOfLeafList().add(leafList);
    }

    /**
     * Returns the sub-modules organization.
     *
     * @return the organization
     */
    public String getOrganization() {
        return organization;
    }

    /**
     * Sets the sub-modules organization.
     *
     * @param org the organization to set
     */
    public void setOrganization(String org) {
        organization = org;
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
     * Returns the revision.
     *
     * @return the revision
     */
    public YangRevision getRevision() {
        return revision;
    }

    /**
     * Sets the revision.
     *
     * @param revision the revision to set
     */
    public void setRevision(YangRevision revision) {
        this.revision = revision;
    }

    /**
     * Returns the version.
     *
     * @return the version
     */
    public byte getVersion() {
        return version;
    }

    /**
     * Sets the version.
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
        // Asks helper to detect colliding child.
        detectCollidingChildUtil(identifierName, dataType, this);
    }

    @Override
    public void detectSelfCollision(String identifierName, YangConstructType dataType)
            throws DataModelException {
        // Not required as module doesn't have any parent.
    }

    @Override
    public List<YangResolutionInfo> getUnresolvedResolutionList(ResolvableType type) {
        if (type == ResolvableType.YANG_DERIVED_DATA_TYPE) {
            return derivedTypeResolutionList;
        } else {
            return usesResolutionList;
        }
    }

    @Override
    public void addToResolutionList(YangResolutionInfo resolutionInfo,
                                    ResolvableType type) {
        if (type == ResolvableType.YANG_DERIVED_DATA_TYPE) {
            derivedTypeResolutionList.add(resolutionInfo);
        } else if (type == ResolvableType.YANG_USES) {
            usesResolutionList.add(resolutionInfo);
        }
    }

    @Override
    public void setResolutionList(List<YangResolutionInfo> resolutionList,
                                  ResolvableType type) {
        if (type == ResolvableType.YANG_DERIVED_DATA_TYPE) {
            derivedTypeResolutionList = resolutionList;
        } else if (type == ResolvableType.YANG_USES) {
            usesResolutionList = resolutionList;
        }

    }

    /**
     * Links the sub-module with module.
     *
     * @param yangNodeSet YANG file information set
     * @throws DataModelException a violation in data model rule
     */
    public void linkWithModule(Set<YangNode> yangNodeSet)
            throws DataModelException {
        getBelongsTo().linkWithModule(yangNodeSet);
    }

    @Override
    public void addReferencesToIncludeList(Set<YangNode> yangNodeSet)
            throws DataModelException {
        Iterator<YangInclude> includeInfoIterator = getIncludeList().iterator();
        // Run through the included list to add references.
        while (includeInfoIterator.hasNext()) {
            YangInclude yangInclude = includeInfoIterator.next();
            YangSubModule subModule = null;
            subModule = yangInclude.addReferenceToInclude(yangNodeSet);
            // Check if the referred sub-modules parent is self
            if (!(subModule.getBelongsTo().getModuleNode() == getBelongsTo().getModuleNode())) {
                yangInclude.reportIncludeError();
            }
        }
    }

    @Override
    public void addReferencesToImportList(Set<YangNode> yangNodeSet)
            throws DataModelException {
        Iterator<YangImport> importInfoIterator = getImportList().iterator();
        // Run through the imported list to add references.
        while (importInfoIterator.hasNext()) {
            YangImport yangImport = importInfoIterator.next();
            yangImport.addReferenceToImport(yangNodeSet);
        }
    }
}

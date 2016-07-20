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

import java.io.Serializable;
import java.util.Set;

import org.onosproject.yangutils.datamodel.exceptions.DataModelException;
import org.onosproject.yangutils.datamodel.utils.Parsable;
import org.onosproject.yangutils.datamodel.utils.YangConstructType;

import static org.onosproject.yangutils.datamodel.utils.DataModelUtils.findReferredNode;

/*
 *  Reference:RFC 6020.
 *  The "import" statement makes definitions from one module available
 *  inside another module or submodule.  The argument is the name of the
 *  module to import, and the statement is followed by a block of
 *  sub statements that holds detailed import information.
 *  When a module is imported, the importing module may:
 *  o  use any grouping and typedef defined at the top level in the
 *     imported module or its submodules.
 *
 *  o  use any extension, feature, and identity defined in the imported
 *     module or its submodules.
 *
 *  o  use any node in the imported module's schema tree in "must",
 *     "path", and "when" statements, or as the target node in "augment"
 *     and "deviation" statements.
 *
 *  The mandatory "prefix" sub statement assigns a prefix for the imported
 *  module that is scoped to the importing module or submodule.  Multiple
 *  "import" statements may be specified to import from different
 *  modules.
 *  When the optional "revision-date" sub-statement is present, any
 *  typedef, grouping, extension, feature, and identity referenced by
 *  definitions in the local module are taken from the specified revision
 *  of the imported module.  It is an error if the specified revision of
 *  the imported module does not exist.  If no "revision-date"
 *  sub-statement is present, it is undefined from which revision of the
 *  module they are taken.
 *
 *  Multiple revisions of the same module MUST NOT be imported.
 *
 *                       The import's Substatements
 *
 *                +---------------+---------+-------------+------------------+
 *                | substatement  | section | cardinality |data model mapping|
 *                +---------------+---------+-------------+------------------+
 *                | prefix        | 7.1.4   | 1           | string           |
 *                | revision-date | 7.1.5.1 | 0..1        | string           |
 *                +---------------+---------+-------------+------------------+
 */

/**
 * Represents the information about the imported modules.
 */
public class YangImport
        implements Parsable, LocationInfo, Serializable {

    private static final long serialVersionUID = 806201642L;

    /**
     * Name of the module that is being imported.
     */
    private String name;

    /**
     * Prefix used to identify the entities from the imported module.
     */
    private String prefixId;

    /**
     * Reference:RFC 6020.
     * <p>
     * The import's "revision-date" statement is used to specify the exact
     * version of the module to import. The "revision-date" statement MUST match
     * the most recent "revision" statement in the imported module. organization
     * which defined the YANG module.
     */
    private String revision;

    /**
     * Reference to node which is imported.
     */
    private YangNode importedNode;

    // Error Line number.
    private transient int lineNumber;

    // Error character position.
    private transient int charPosition;

    /**
     * Creates a YANG import.
     */
    public YangImport() {
    }

    /**
     * Returns the imported module name.
     *
     * @return the module name
     */
    public String getModuleName() {
        return name;
    }

    /**
     * Sets module name.
     *
     * @param moduleName the module name to set
     */
    public void setModuleName(String moduleName) {
        name = moduleName;
    }

    /**
     * Returns the prefix used to identify the entities from the imported module.
     *
     * @return the prefix used to identify the entities from the imported
     * module
     */
    public String getPrefixId() {
        return prefixId;
    }

    /**
     * Sets prefix identifier.
     *
     * @param prefixId set the prefix identifier of the imported module
     */
    public void setPrefixId(String prefixId) {
        this.prefixId = prefixId;
    }

    /**
     * Returns the revision of the imported module.
     *
     * @return the revision of the imported module
     */
    public String getRevision() {
        return revision;
    }

    /**
     * Sets the revision of the imported module.
     *
     * @param rev set the revision of the imported module
     */
    public void setRevision(String rev) {
        revision = rev;
    }

    /**
     * Returns the type of the parsed data.
     *
     * @return returns IMPORT_DATA
     */
    @Override
    public YangConstructType getYangConstructType() {
        return YangConstructType.IMPORT_DATA;
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

    /**
     * Returns imported node.
     *
     * @return imported node
     */
    public YangNode getImportedNode() {
        return importedNode;
    }

    /**
     * Sets imported node.
     *
     * @param importedNode imported node
     */
    public void setImportedNode(YangNode importedNode) {
        this.importedNode = importedNode;
    }

    @Override
    public int getLineNumber() {
        return lineNumber;
    }

    @Override
    public int getCharPosition() {
        return charPosition;
    }

    @Override
    public void setLineNumber(int lineNumber) {
        this.lineNumber = lineNumber;
    }

    @Override
    public void setCharPosition(int charPositionInLine) {
        charPosition = charPositionInLine;
    }

    /**
     * Adds reference to an import.
     *
     * @param yangNodeSet YANG file info set
     * @throws DataModelException a violation of data model rules
     */
    public void addReferenceToImport(Set<YangNode> yangNodeSet) throws DataModelException {
        String importedModuleName = getModuleName();
        String importedModuleRevision = getRevision();
        YangNode moduleNode = null;
        /*
         * Find the imported module node for a given module name with a
         * specified revision if revision is not null.
         */
        if (importedModuleRevision != null) {
            String importedModuleNameWithRevision = importedModuleName + "@" + importedModuleRevision;
            moduleNode = findReferredNode(yangNodeSet, importedModuleNameWithRevision);
        }

        /*
         * Find the imported module node for a given module name without
         * revision if can't find with revision.
         */
        if (moduleNode == null) {
            moduleNode = findReferredNode(yangNodeSet, importedModuleName);
        }

        if (moduleNode != null) {
            if (moduleNode instanceof YangModule) {
                if (getRevision() == null || getRevision().isEmpty()) {
                    setImportedNode(moduleNode);
                    return;
                }
                // Match revision if import is with revision.
                if (((YangModule) moduleNode).getRevision().getRevDate().equals(importedModuleRevision)) {
                    setImportedNode(moduleNode);
                    return;
                }
            }
        }

        // Exception if there is no match.
        DataModelException exception = new DataModelException("YANG file error : Imported module "
                + importedModuleName + " with revision " + importedModuleRevision + " is not found.");
        exception.setLine(getLineNumber());
        exception.setCharPosition(getCharPosition());
        throw exception;
    }
}

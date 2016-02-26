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
import org.onosproject.yangutils.parser.Parsable;
import org.onosproject.yangutils.utils.YangConstructType;

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
 * Maintains the information about the imported modules.
 */
public class YangImport implements Parsable {

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
     *
     * The import's "revision-date" statement is used to specify the exact
     * version of the module to import. The "revision-date" statement MUST match
     * the most recent "revision" statement in the imported module. organization
     * which defined the YANG module.
     */
    private String revision;

    /**
     * Default constructor.
     */
    public YangImport() {

    }

    /**
     * Get the imported module name.
     *
     * @return the module name
     */
    public String getModuleName() {
        return name;
    }

    /**
     * Set module name.
     *
     * @param moduleName the module name to set
     */
    public void setModuleName(String moduleName) {
        name = moduleName;
    }

    /**
     * Get the prefix used to identify the entities from the imported module.
     *
     * @return the prefix used to identify the entities from the imported
     *         module
     */
    public String getPrefixId() {
        return prefixId;
    }

    /**
     * Set prefix identifier.
     *
     * @param prefixId set the prefix identifier of the imported module
     */
    public void setPrefixId(String prefixId) {
        this.prefixId = prefixId;
    }

    /**
     * Get the revision of the imported module.
     *
     * @return the revision of the imported module
     */
    public String getRevision() {
        return revision;
    }

    /**
     * Set the revision of the imported module.
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
}

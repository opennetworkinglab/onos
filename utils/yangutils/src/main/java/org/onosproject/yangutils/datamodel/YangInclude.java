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
 * Reference:RFC 6020.
 * The "include" statement is used to make content from a submodule
 *  available to that submodule's parent module, or to another submodule
 *  of that parent module.  The argument is an identifier that is the
 *  name of the submodule to include.
 *  The includes's Substatements
 *
 *                +---------------+---------+-------------+------------------+
 *                | substatement  | section | cardinality |data model mapping|
 *                +---------------+---------+-------------+------------------+
 *                | revision-date | 7.1.5.1 | 0..1        | string           |
 *                +---------------+---------+-------------+------------------+
 */
/**
 * Maintains the information about the included sub-modules.
 *
 */
public class YangInclude implements Parsable {

    /**
     * Name of the sub-module that is being included.
     */
    private String subModuleName;

    /**
     * The include's "revision-date" statement is used to specify the exact
     * version of the submodule to import.
     */
    private String revision;

    /**
     * Default constructor.
     */
    public YangInclude() {
    }

    /**
     * Get the name of included sub-module.
     *
     * @return the sub-module name
     */
    public String getSubModuleName() {
        return subModuleName;
    }

    /**
     * Set the name of included sub-modules.
     *
     * @param subModuleName the sub-module name to set
     */
    public void setSubModuleName(String subModuleName) {
        this.subModuleName = subModuleName;
    }

    /**
     * Get the revision.
     *
     * @return the revision
     */
    public String getRevision() {
        return revision;
    }

    /**
     * Set the revision.
     *
     * @param revision the revision to set
     */
    public void setRevision(String revision) {
        this.revision = revision;
    }

    /**
     * Returns the type of parsed data.
     *
     * @return returns INCLUDE_DATA
     */
    @Override
    public YangConstructType getYangConstructType() {
        return YangConstructType.INCLUDE_DATA;
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

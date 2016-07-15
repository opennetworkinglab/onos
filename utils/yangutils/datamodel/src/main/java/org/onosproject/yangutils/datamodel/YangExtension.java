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
import org.onosproject.yangutils.datamodel.exceptions.DataModelException;
import org.onosproject.yangutils.datamodel.utils.Parsable;
import org.onosproject.yangutils.datamodel.utils.YangConstructType;

/*-
 * Reference RFC 6020.
 *
 * The "extension" statement allows the definition of new statements
 * within the YANG language.  This new statement definition can be
 * imported and used by other modules.
 *
 * The statement's argument is an identifier that is the new keyword for
 * the extension and must be followed by a block of sub-statements that
 * holds detailed extension information.  The purpose of the "extension"
 * statement is to define a keyword, so that it can be imported and used
 * by other modules.
 *
 * The extension can be used like a normal YANG statement, with the
 * statement name followed by an argument if one is defined by the
 * extension, and an optional block of sub-statements.  The statement's
 * name is created by combining the prefix of the module in which the
 * extension was defined, a colon (":"), and the extension's keyword,
 * with no interleaving whitespace.  The sub-statements of an extension
 * are defined by the extension, using some mechanism outside the scope
 * of this specification.  Syntactically, the sub-statements MUST be YANG
 * statements, or also defined using "extension" statements.
 *
 * The extension's Sub-statements
 *
 *                +--------------+---------+-------------+------------------+
 *                | substatement | section | cardinality |data model mapping|
 *                +--------------+---------+-------------+------------------+
 *                | description  | 7.19.3  | 0..1        | -string          |
 *                | reference    | 7.19.4  | 0..1        | -string          |
 *                | status       | 7.19.2  | 0..1        | -YangStatus      |
 *                | argument     | 7.17.2  | 0..1        | -string          |
 *                +--------------+---------+-------------+------------------+
 */

/**
 * Represents data model node to maintain information defined in YANG extension.
 */
public class YangExtension
        implements YangCommonInfo, Serializable, Parsable {

    private static final long serialVersionUID = 806201605L;

    /**
     * Name of the extension.
     */
    private String name;

    /**
     * Name of the argument.
     */
    private String argumentName;

    /**
     * Description of extension.
     */
    private String description;

    /**
     * Reference of the extension.
     */
    private String reference;

    /**
     * Status of the extension.
     */
    private YangStatusType status = YangStatusType.CURRENT;

    /**
     * Returns the YANG name of extension.
     *
     * @return the name of extension as defined in YANG file
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the YANG name of extension.
     *
     * @param name the name of extension as defined in YANG file
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Returns the YANG argument name of extension.
     *
     * @return the name of argument as defined in YANG file
     */
    public String getArgumentName() {
        return argumentName;
    }

    /**
     * Sets the YANG argument name of extension.
     *
     * @param argumentName the name of argument as defined in YANG file
     */
    public void setArgumentName(String argumentName) {
        this.argumentName = argumentName;
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
     * Returns the type of the data.
     *
     * @return returns EXTENSION_DATA
     */
    @Override
    public YangConstructType getYangConstructType() {
        return YangConstructType.EXTENSION_DATA;
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
        // TODO : to be implemented
    }
}

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

/*-
 *   Reference 6020.
 *
 *  The "belongs-to" statement specifies the module to which the
 *  submodule belongs.  The argument is an identifier that is the name of
 *  the module.
 *
 *  A submodule MUST only be included by the module to which it belongs,
 *  or by another submodule that belongs to that module.
 *
 *  The mandatory "prefix" sub-statement assigns a prefix for the module
 *  to which the submodule belongs.  All definitions in the local
 *  submodule and any included submodules can be accessed by using the
 *  prefix.
 *
 *  The belongs-to's sub-statements
 *
 *                +--------------+---------+-------------+
 *                | substatement | section | cardinality |
 *                +--------------+---------+-------------+
 *                | prefix       | 7.1.4   | 1           |
 *                +--------------+---------+-------------+
 */

/**
 * Represents the belongs-to data type information.
 */
public class YangBelongsTo implements Parsable, LocationInfo, Serializable {

    private static final long serialVersionUID = 806201639L;

    /**
     * Reference RFC 6020.
     *
     * The "belongs-to" statement specifies the module to which the submodule
     * belongs. The argument is an identifier that is the name of the module.
     */
    private String belongsToModuleName;

    /**
     * Module node to which sub-module belongs to.
     */
    private YangNode moduleNode;

    /**
     * Reference RFC 6020.
     *
     * The mandatory "prefix" substatement assigns a prefix for the module to
     * which the submodule belongs. All definitions in the local submodule and
     * any included submodules can be accessed by using the prefix.
     */
    private String prefix;

    // Error Line number.
    private transient int lineNumber;

    // Error character position.
    private transient int charPosition;

    /**
     * Create a belongs to object.
     */
    public YangBelongsTo() {

    }

    /**
     * Returns the belongs to module name.
     *
     * @return the belongs to module name
     */
    public String getBelongsToModuleName() {
        return belongsToModuleName;
    }

    /**
     * Sets the belongs to module name.
     *
     * @param belongsToModuleName the belongs to module name to set
     */
    public void setBelongsToModuleName(String belongsToModuleName) {
        this.belongsToModuleName = belongsToModuleName;
    }

    /**
     * Returns the prefix.
     *
     * @return the prefix
     */
    public String getPrefix() {
        return prefix;
    }

    /**
     * Sets the prefix.
     *
     * @param prefix the prefix to set
     */
    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    /**
     * Returns the module data model node.
     *
     * @return the module data model node
     */
    public YangNode getModuleNode() {
        return moduleNode;
    }

    /**
     * Sets the module node.
     *
     * @param moduleNode module data model node
     */
    public void setModuleNode(YangNode moduleNode) {
        this.moduleNode = moduleNode;
    }

    /**
     * Returns the type of the data as belongs-to.
     *
     * @return ParsedDataType returns BELONGS_TO_DATA
     */
    @Override
    public YangConstructType getYangConstructType() {
        return YangConstructType.BELONGS_TO_DATA;
    }

    /**
     * Validates the data on entering the corresponding parse tree node.
     *
     * @throws DataModelException a violation of data model rules
     */
    @Override
    public void validateDataOnEntry() throws DataModelException {
        // TODO auto-generated method stub, to be implemented by parser
    }

    /**
     * Validates the data on exiting the corresponding parse tree node.
     *
     * @throws DataModelException a violation of data model rules
     */
    @Override
    public void validateDataOnExit() throws DataModelException {
        // TODO auto-generated method stub, to be implemented by parser
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
     * Links the belongs to with a module.
     *
     * @param yangNodeSet YANG file information set
     * @throws DataModelException a violation in data model rule
     */
    public void linkWithModule(Set<YangNode> yangNodeSet)
            throws DataModelException {
        String belongsToModuleName = getBelongsToModuleName();
        YangNode moduleNode = findReferredNode(yangNodeSet, belongsToModuleName);
        if (moduleNode != null) {
            if (moduleNode instanceof YangModule) {
                setModuleNode(moduleNode);
                return;
            }
        }
        DataModelException exception = new DataModelException("YANG file error : Module " + belongsToModuleName +
                "to which sub-module belongs to is not found.");
        exception.setLine(getLineNumber());
        exception.setCharPosition(getCharPosition());
        throw exception;
    }
}

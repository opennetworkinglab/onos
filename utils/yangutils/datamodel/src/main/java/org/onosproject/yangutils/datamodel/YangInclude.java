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
 * Represents the information about the included sub-modules.
 */
public class YangInclude
        implements Parsable, LocationInfo, Serializable {

    private static final long serialVersionUID = 806201644L;

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
     * Reference to node which is included.
     */
    private YangNode includedNode;

    // Error Line number.
    private transient int lineNumber;

    // Error character position.
    private transient int charPosition;

    /**
     * Creates a YANG include.
     */
    public YangInclude() {
    }

    /**
     * Returns the name of included sub-module.
     *
     * @return the sub-module name
     */
    public String getSubModuleName() {
        return subModuleName;
    }

    /**
     * Sets the name of included sub-modules.
     *
     * @param subModuleName the sub-module name to set
     */
    public void setSubModuleName(String subModuleName) {
        this.subModuleName = subModuleName;
    }

    /**
     * Returns the revision.
     *
     * @return the revision
     */
    public String getRevision() {
        return revision;
    }

    /**
     * Sets the revision.
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

    public YangNode getIncludedNode() {
        return includedNode;
    }

    public void setIncludedNode(YangNode includedNode) {
        this.includedNode = includedNode;
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
     * Adds reference to an include.
     *
     * @param yangNodeSet YANG node set
     * @return YANG sub module node
     * @throws DataModelException a violation of data model rules
     */
    public YangSubModule addReferenceToInclude(Set<YangNode> yangNodeSet) throws DataModelException {
        String includedSubModuleName = getSubModuleName();
        String includedSubModuleRevision = getRevision();
        YangNode subModuleNode = null;

        /*
         * Find the included sub-module node for a given module name with a
         * specified revision if revision is not null.
         */
        if (includedSubModuleRevision != null) {
            String includedSubModuleNameWithRevision = includedSubModuleName + "@" + includedSubModuleRevision;
            subModuleNode = findReferredNode(yangNodeSet, includedSubModuleNameWithRevision);
        }

        /*
         * Find the imported sub module node for a given module name without
         * revision if can't find with revision.
         */
        if (subModuleNode == null) {
            subModuleNode = findReferredNode(yangNodeSet, includedSubModuleName);
        }

        if (subModuleNode != null) {
            if (subModuleNode instanceof YangSubModule) {
                if (getRevision() == null || getRevision().isEmpty()) {
                    setIncludedNode(subModuleNode);
                    return (YangSubModule) subModuleNode;
                }
                // Match revision if inclusion is with revision.
                if (((YangSubModule) subModuleNode).getRevision().getRevDate().equals(includedSubModuleRevision)) {
                    setIncludedNode(subModuleNode);
                    return (YangSubModule) subModuleNode;
                }
            }
        }
        // Exception if there is no match.
        DataModelException exception = new DataModelException("YANG file error : Included sub module " +
                includedSubModuleName + "with a given revision is not found.");
        exception.setLine(getLineNumber());
        exception.setCharPosition(getCharPosition());
        throw exception;
    }

    /**
     * Reports an error when included sub-module doesn't meet condition that
     * "included sub-modules should belong module, as defined by the
     * "belongs-to" statement or sub-modules are only allowed to include other
     * sub-modules belonging to the same module.
     *
     * @throws DataModelException a violation in data model rule
     */
    public void reportIncludeError() throws DataModelException {
        DataModelException exception = new DataModelException("YANG file error : Included sub-module " +
                getSubModuleName() + "doesn't belongs to parent module also it doesn't belongs" +
                "to sub-module belonging to the same parent module.");
        exception.setLine(getLineNumber());
        exception.setCharPosition(getCharPosition());
        throw exception;
    }
}

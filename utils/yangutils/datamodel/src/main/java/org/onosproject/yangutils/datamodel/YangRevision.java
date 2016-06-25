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

/*
 *  Reference:RFC 6020.
 *  The "revision" statement specifies the editorial revision history of
 *  the module, including the initial revision.  A series of revision
 *  statements detail the changes in the module's definition.  The
 *  argument is a date string in the format "YYYY-MM-DD", followed by a
 *  block of sub-statements that holds detailed revision information.  A
 *  module SHOULD have at least one initial "revision" statement.  For
 *  every published editorial change, a new one SHOULD be added in front
 *  of the revisions sequence, so that all revisions are in reverse
 *  chronological order.
 *  The revision's sub-statement
 *
 *                +--------------+---------+-------------+------------------+
 *                | substatement | section | cardinality |data model mapping|
 *                +--------------+---------+-------------+------------------+
 *                | description  | 7.19.3  | 0..1        |string            |
 *                | reference    | 7.19.4  | 0..1        |sring            |
 *                +--------------+---------+-------------+------------------+
 */
/**
 * Represents the information about the revision.
 */
public class YangRevision implements YangDesc, YangReference, Parsable, Serializable {

    private static final long serialVersionUID = 8062016052L;

    /**
     * Revision date. Date string in the format "YYYY-MM-DD"
     */
    private String revDate;

    /**
     * Description of revision.
     */
    private String description;

    /**
     * Textual reference for revision.
     */
    private String reference;

    /**
     * Creates a YANG revision object.
     */
    public YangRevision() {
    }

    /**
     * Returns the revision date.
     *
     * @return the revision date
     */
    public String getRevDate() {
        return revDate;
    }

    /**
     * Sets the revision date.
     *
     * @param revDate the revision date to set
     */
    public void setRevDate(String revDate) {
        this.revDate = revDate;
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
     * Returns the type of the parsed data.
     *
     * @return returns REVISION_DATA
     */
    @Override
    public YangConstructType getYangConstructType() {
        return YangConstructType.REVISION_DATA;
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
}

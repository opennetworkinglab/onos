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
 * Reference RFC 6020.
 *
 * The "when" statement makes its parent data definition statement
 * conditional.  The node defined by the parent data definition
 * statement is only valid when the condition specified by the "when"
 * statement is satisfied.
 *
 * The statement's argument is an XPath expression, which is used to formally
 * specify this condition.  If the XPath  expression conceptually evaluates to
 * "true" for a particular instance, then the node defined by the parent data
 * definition statement is valid; otherwise, it is not.
 *
 *  The when's sub-statements
 *
 *                +---------------+---------+-------------+------------------+
 *                | substatement  | section | cardinality |data model mapping|
 *                +---------------+---------+-------------+------------------+
 *                | description   | 7.19.3  | 0..1        | -string          |
 *                | reference     | 7.19.4  | 0..1        | -string          |
 *                +---------------+---------+-------------+------------------+
 */

/**
 * Represents information defined in YANG when.
 */
public class YangWhen implements YangDesc, YangReference, Parsable, Serializable {

    private static final long serialVersionUID = 806201646L;

    /**
     * When condition info.
     */
    private String condition;

    /**
     * Description string.
     */
    private String description;

    /**
     * Reference string.
     */
    private String reference;

    /**
     * Creates a YANG when restriction.
     */
    public YangWhen() {
    }

    /**
     * Returns the condition.
     *
     * @return the condition
     */
    public String getCondition() {
        return condition;
    }

    /**
     * Sets the condition.
     *
     * @param condition the condition to set
     */
    public void setCondition(String condition) {
        this.condition = condition;
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
     * @return returns WHEN_DATA
     */
    @Override
    public YangConstructType getYangConstructType() {
        return YangConstructType.WHEN_DATA;
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

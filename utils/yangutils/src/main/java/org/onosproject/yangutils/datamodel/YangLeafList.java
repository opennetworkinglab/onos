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
 *  Where the "leaf" statement is used to define a simple scalar variable
 *  of a particular type, the "leaf-list" statement is used to define an
 *  array of a particular type.  The "leaf-list" statement takes one
 *  argument, which is an identifier, followed by a block of
 *  sub-statements that holds detailed leaf-list information.
 *
 *  The values in a leaf-list MUST be unique.
 *
 * The leaf-list's sub-statements
 *
 *                +--------------+---------+-------------+------------------+
 *                | substatement | section | cardinality |data model mapping|
 *                +--------------+---------+-------------+------------------+
 *                | config       | 7.19.1  | 0..1        | -boolean         |
 *                | description  | 7.19.3  | 0..1        | -string          |
 *                | if-feature   | 7.18.2  | 0..n        | -TODO            |
 *                | max-elements | 7.7.4   | 0..1        | -int             |
 *                | min-elements | 7.7.3   | 0..1        | -int             |
 *                | must         | 7.5.3   | 0..n        | -TODO            |
 *                | ordered-by   | 7.7.5   | 0..1        | -TODO            |
 *                | reference    | 7.19.4  | 0..1        | -string          |
 *                | status       | 7.19.2  | 0..1        | -YangStatus      |
 *                | type         | 7.4     | 1           | -YangType        |
 *                | units        | 7.3.3   | 0..1        | -string          |
 *                | when         | 7.19.5  | 0..1        | -TODO            |
 *                +--------------+---------+-------------+------------------+
 */
/**
 * Leaf-list data represented in YANG.
 */
public class YangLeafList implements YangCommonInfo, Parsable {

    /**
     * Name of leaf-list.
     */
    private String name;

    /**
     * If the leaf-list is a config parameter.
     */
    private Boolean isConfig;

    /**
     * Description of leaf-list.
     */
    private String description;

    /**
     * Reference:RFC 6020.
     *
     * The "max-elements" statement, which is optional, takes as an argument a
     * positive integer or the string "unbounded", which puts a constraint on
     * valid list entries. A valid leaf-list or list always has at most
     * max-elements entries.
     *
     * If no "max-elements" statement is present, it defaults to "unbounded".
     */
    private int maxElelements = Integer.MAX_VALUE;

    /**
     * Reference:RFC 6020.
     *
     * The "min-elements" statement, which is optional, takes as an argument a
     * non-negative integer that puts a constraint on valid list entries. A
     * valid leaf-list or list MUST have at least min-elements entries.
     *
     * If no "min-elements" statement is present, it defaults to zero.
     *
     * The behavior of the constraint depends on the type of the leaf-list's or
     * list's closest ancestor node in the schema tree that is not a non-
     * presence container:
     *
     * o If this ancestor is a case node, the constraint is enforced if any
     * other node from the case exists.
     *
     * o Otherwise, it is enforced if the ancestor node exists.
     */
    private int minElements = 0;

    /**
     * The textual reference to this leaf-list.
     */
    private String reference;

    /**
     * Status of the leaf-list in the YANG definition.
     */
    private YangStatusType status = YangStatusType.CURRENT;

    /**
     * Textual units.
     */
    private String units;

    /**
     * Data type of leaf-list.
     */
    private YangType<?> dataType;

    /**
     * Default Constructor to create a YANG leaf-list.
     */
    public YangLeafList() {
    }

    /**
     * Get the leaf-list name.
     *
     * @return the leaf-list name
     */
    public String getLeafName() {
        return name;
    }

    /**
     * Set the leaf-list name.
     *
     * @param leafListName the leaf-list name to set
     */
    public void setLeafName(String leafListName) {
        name = leafListName;
    }

    /**
     * Get the config flag.
     *
     * @return the config flag
     */
    public Boolean isConfig() {
        return isConfig;
    }

    /**
     * Set the config flag.
     *
     * @param isCfg the config flag
     */
    public void setConfig(boolean isCfg) {
        isConfig = isCfg;
    }

    /**
     * Get the description.
     *
     * @return the description
     */
    @Override
    public String getDescription() {
        return description;
    }

    /**
     * Set the description.
     *
     * @param description set the description
     */
    @Override
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Get the max elements no.
     *
     * @return the max elements no
     */
    public int getMaxElelements() {
        return maxElelements;
    }

    /**
     * Set the max elements no.
     *
     * @param maxElelements max elements no
     */
    public void setMaxElelements(int maxElelements) {
        this.maxElelements = maxElelements;
    }

    /**
     * Get the min elements no.
     *
     * @return the min elements no
     */
    public int getMinElements() {
        return minElements;
    }

    /**
     * Set the min elements no.
     *
     * @param minElements the min elements no
     */
    public void setMinElements(int minElements) {
        this.minElements = minElements;
    }

    /**
     * Get the textual reference.
     *
     * @return the reference
     */
    @Override
    public String getReference() {
        return reference;
    }

    /**
     * Set the textual reference.
     *
     * @param reference the reference to set
     */
    @Override
    public void setReference(String reference) {
        this.reference = reference;
    }

    /**
     * Get the status.
     *
     * @return the status
     */
    @Override
    public YangStatusType getStatus() {
        return status;
    }

    /**
     * Set the status.
     *
     * @param status the status to set
     */
    @Override
    public void setStatus(YangStatusType status) {
        this.status = status;
    }

    /**
     * Get the units.
     *
     * @return the units
     */
    public String getUnits() {
        return units;
    }

    /**
     * Set the units.
     *
     * @param units the units to set
     */
    public void setUnits(String units) {
        this.units = units;
    }

    /**
     * Get the data type.
     *
     * @return the data type
     */
    public YangType<?> getDataType() {
        return dataType;
    }

    /**
     * Set the data type.
     *
     * @param dataType the data type to set
     */
    public void setDataType(YangType<?> dataType) {
        this.dataType = dataType;
    }

    /**
     * Returns the type of the parsed data.
     *
     * @return returns LEAF_LIST_DATA
     */
    @Override
    public YangConstructType getYangConstructType() {
        return YangConstructType.LEAF_LIST_DATA;
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

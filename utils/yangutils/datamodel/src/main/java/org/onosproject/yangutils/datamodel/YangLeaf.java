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
 * Reference:RFC 6020.
 *  The "leaf" statement is used to define a leaf node in the schema
 *  tree.  It takes one argument, which is an identifier, followed by a
 *  block of sub-statements that holds detailed leaf information.
 *
 *  A leaf node has a value, but no child nodes in the data tree.
 *  Conceptually, the value in the data tree is always in the canonical
 *  form.
 *
 *  A leaf node exists in zero or one instances in the data tree.
 *
 *  The "leaf" statement is used to define a scalar variable of a
 *  particular built-in or derived type.
 *
 * The leaf's sub-statements
 *
 *       +--------------+---------+-------------+------------------+
 *       | substatement | section | cardinality |data model mapping|
 *       +--------------+---------+-------------+------------------+
 *       | config       | 7.19.1  | 0..1        | - boolean        |
 *       | default      | 7.6.4   | 0..1        | - TODO           |
 *       | description  | 7.19.3  | 0..1        | - string         |
 *       | if-feature   | 7.18.2  | 0..n        | - TODO           |
 *       | mandatory    | 7.6.5   | 0..1        | - boolean        |
 *       | must         | 7.5.3   | 0..n        | - TODO           |
 *       | reference    | 7.19.4  | 0..1        | - string         |
 *       | status       | 7.19.2  | 0..1        | - YangStatus     |
 *       | type         | 7.6.3   | 1           | - YangType       |
 *       | units        | 7.3.3   | 0..1        | - String         |
 *       | when         | 7.19.5  | 0..1        | - TODO           |
 *       +--------------+---------+-------------+------------------+
 */

/**
 * Represents leaf data represented in YANG.
 */
public class YangLeaf
        implements YangCommonInfo, Parsable, Cloneable, Serializable {

    private static final long serialVersionUID = 806201635L;

    /**
     * Name of leaf.
     */
    private String name;

    /**
     * If the leaf is a config parameter.
     */
    private Boolean isConfig;

    /**
     * description of leaf.
     */
    private String description;

    /**
     * If mandatory leaf.
     */
    private boolean isMandatory;

    /**
     * The textual reference to this leaf.
     */
    private String reference;

    /**
     * Status of leaf in YANG definition.
     */
    private YangStatusType status = YangStatusType.CURRENT;

    /**
     * Textual units info.
     */
    private String units;

    /**
     * Data type of the leaf.
     */
    private YangType<?> dataType;

    /**
     * Default value in string, needs to be converted to the target object,
     * based on the type.
     */
    private String defaultValueInString;

    /**
     * YANG Node in which the leaf is contained.
     */
    private transient YangLeavesHolder containedIn;

    /**
     * Creates a YANG leaf.
     */
    public YangLeaf() {
    }

    /**
     * Returns the name of leaf.
     *
     * @return the leaf name
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the name of leaf.
     *
     * @param leafName the leaf name to set
     */
    public void setLeafName(String leafName) {
        name = leafName;
    }

    /**
     * Returns the config flag.
     *
     * @return if config flag
     */
    public Boolean isConfig() {
        return isConfig;
    }

    /**
     * Sets the config flag.
     *
     * @param isCfg the flag value to set
     */
    public void setConfig(boolean isCfg) {
        isConfig = isCfg;
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
     * Returns if the leaf is mandatory.
     *
     * @return if leaf is mandatory
     */
    public boolean isMandatory() {
        return isMandatory;
    }

    /**
     * Sets if the leaf is mandatory.
     *
     * @param isReq if the leaf is mandatory
     */
    public void setMandatory(boolean isReq) {
        isMandatory = isReq;
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
     * Returns the units.
     *
     * @return the units
     */
    public String getUnits() {
        return units;
    }

    /**
     * Sets the units.
     *
     * @param units the units to set
     */
    public void setUnits(String units) {
        this.units = units;
    }

    /**
     * Returns the default value.
     *
     * @return the default value
     */
    public String getDefaultValueInString() {
        return defaultValueInString;
    }

    /**
     * Sets the default value.
     *
     * @param defaultValueInString the default value
     */
    public void setDefaultValueInString(String defaultValueInString) {
        this.defaultValueInString = defaultValueInString;
    }

    /**
     * Returns the data type.
     *
     * @return the data type
     */
    public YangType<?> getDataType() {
        return dataType;
    }

    /**
     * Sets the data type.
     *
     * @param dataType the data type to set
     */
    public void setDataType(YangType<?> dataType) {
        this.dataType = dataType;
    }

    /**
     * Retrieves the YANG node in which the leaf is defined.
     *
     * @return the YANG node in which the leaf is defined
     */
    public YangLeavesHolder getContainedIn() {
        return containedIn;
    }

    /**
     * Assigns the YANG node in which the leaf is defined.
     *
     * @param containedIn the YANG node in which the leaf is defined
     */
    public void setContainedIn(YangLeavesHolder containedIn) {
        this.containedIn = containedIn;
    }

    @Override
    public YangLeaf clone()
            throws CloneNotSupportedException {
        return (YangLeaf) super.clone();
    }

    /**
     * Returns the type of the parsed data.
     *
     * @return returns LEAF_DATA
     */
    @Override
    public YangConstructType getYangConstructType() {
        return YangConstructType.LEAF_DATA;
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
}

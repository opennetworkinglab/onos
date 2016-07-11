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
import java.util.LinkedList;
import java.util.List;
import org.onosproject.yangutils.datamodel.exceptions.DataModelException;
import org.onosproject.yangutils.datamodel.utils.Parsable;
import org.onosproject.yangutils.datamodel.utils.YangConstructType;

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
 *                | if-feature   | 7.18.2  | 0..n        | -YangIfFeature   |
 *                | max-elements | 7.7.4   | 0..1        | -int             |
 *                | min-elements | 7.7.3   | 0..1        | -int             |
 *                | must         | 7.5.3   | 0..n        | -YangMust        |
 *                | ordered-by   | 7.7.5   | 0..1        | -TODO            |
 *                | reference    | 7.19.4  | 0..1        | -string          |
 *                | status       | 7.19.2  | 0..1        | -YangStatus      |
 *                | type         | 7.4     | 1           | -YangType        |
 *                | units        | 7.3.3   | 0..1        | -string          |
 *                | when         | 7.19.5  | 0..1        | -YangWhen        |
 *                +--------------+---------+-------------+------------------+
 */

/**
 * Represents leaf-list data represented in YANG.
 */
public class YangLeafList
        implements YangCommonInfo, Parsable, Cloneable, Serializable,
        YangMustHolder, YangWhenHolder, YangIfFeatureHolder, YangDataNode {

    private static final long serialVersionUID = 806201637L;

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
    private YangMaxElement maxElement;

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
    private YangMinElement minElements;

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
     * YANG Node in which the leaf is contained.
     */
    private transient YangLeavesHolder containedIn;

    /**
     * List of must statement constraints.
     */
    private List<YangMust> mustConstraintList;

    /**
     * When data of the leaf.
     */
    private YangWhen when;

    /**
     * List of if-feature.
     */
    private List<YangIfFeature> ifFeatureList;

    /**
     * Creates a YANG leaf-list.
     */
    public YangLeafList() {
        setMinElements(new YangMinElement());
        setMaxElements(new YangMaxElement());
    }

    /**
     * Returns the leaf-list name.
     *
     * @return the leaf-list name
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the leaf-list name.
     *
     * @param leafListName the leaf-list name to set
     */
    public void setLeafName(String leafListName) {
        name = leafListName;
    }

    /**
     * Returns the config flag.
     *
     * @return the config flag
     */
    public Boolean isConfig() {
        return isConfig;
    }

    /**
     * Sets the config flag.
     *
     * @param isCfg the config flag
     */
    public void setConfig(boolean isCfg) {
        isConfig = isCfg;
    }

    /**
     * Returns the when.
     *
     * @return the when
     */
    @Override
    public YangWhen getWhen() {
        return when;
    }

    /**
     * Sets the when.
     *
     * @param when the when to set
     */
    @Override
    public void setWhen(YangWhen when) {
        this.when = when;
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
     * Returns the maximum elements number.
     *
     * @return the maximum elements number
     */
    public YangMaxElement getMaxElements() {
        return maxElement;
    }

    /**
     * Sets the maximum elements number.
     *
     * @param maxElement maximum elements number
     */
    public void setMaxElements(YangMaxElement maxElement) {
        this.maxElement = maxElement;
    }

    /**
     * Returns the minimum elements number.
     *
     * @return the minimum elements number
     */
    public YangMinElement getMinElements() {
        return minElements;
    }

    /**
     * Sets the minimum elements number.
     *
     * @param minElements the minimum elements number
     */
    public void setMinElements(YangMinElement minElements) {
        this.minElements = minElements;
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
    public YangLeafList clone()
            throws CloneNotSupportedException {
        return (YangLeafList) super.clone();
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

    @Override
    public List<YangIfFeature> getIfFeatureList() {
        return ifFeatureList;
    }

    @Override
    public void addIfFeatureList(YangIfFeature ifFeature) {
        if (getIfFeatureList() == null) {
            setIfFeatureList(new LinkedList<>());
        }
        getIfFeatureList().add(ifFeature);
    }

    @Override
    public void setIfFeatureList(List<YangIfFeature> ifFeatureList) {
        this.ifFeatureList = ifFeatureList;
    }

    @Override
    public List<YangMust> getListOfMust() {
        return mustConstraintList;
    }

    @Override
    public void setListOfMust(List<YangMust> mustConstraintList) {
        this.mustConstraintList = mustConstraintList;
    }

    @Override
    public void addMust(YangMust must) {
        if (getListOfMust() == null) {
            setListOfMust(new LinkedList<>());
        }
        getListOfMust().add(must);
    }
}

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

import java.util.LinkedList;
import java.util.List;

import org.onosproject.yangutils.datamodel.exceptions.DataModelException;
import org.onosproject.yangutils.datamodel.utils.Parsable;
import org.onosproject.yangutils.datamodel.utils.YangConstructType;

import static org.onosproject.yangutils.datamodel.utils.DataModelUtils.detectCollidingChildUtil;

/*-
 * Reference RFC 6020.
 *
 * The "typedef" statement defines a new type that may be used locally in the
 * module, in modules or submodules which include it, and by other modules that
 * import from it. The new type is called the "derived type", and the type from
 * which it was derived is called the "base type". All derived types can be
 * traced back to a YANG built-in type.
 *
 * The "typedef" statement's argument is an identifier that is the name of the
 * type to be defined, and MUST be followed by a block of sub-statements that
 * holds detailed typedef information.
 *
 * The name of the type MUST NOT be one of the YANG built-in types. If the
 * typedef is defined at the top level of a YANG module or submodule, the name
 * of the type to be defined MUST be unique within the module.
 * The typedef's sub-statements
 *
 *                +--------------+---------+-------------+------------------+
 *                | substatement | section | cardinality |data model mapping|
 *                +--------------+---------+-------------+------------------+
 *                | default      | 7.3.4   | 0..1        |-string           |
 *                | description  | 7.19.3  | 0..1        |-string           |
 *                | reference    | 7.19.4  | 0..1        |-string           |
 *                | status       | 7.19.2  | 0..1        |-YangStatus       |
 *                | type         | 7.3.2   | 1           |-yangType         |
 *                | units        | 7.3.3   | 0..1        |-string           |
 *                +--------------+---------+-------------+------------------+
 */

/**
 * Represents data model node to maintain information defined in YANG typedef.
 */
public class YangTypeDef extends YangNode implements YangCommonInfo, Parsable, YangTypeHolder, CollisionDetector {

    private static final long serialVersionUID = 806201615L;

    /**
     * Default value in string, needs to be converted to the target object,
     * based on the type.
     */
    private String defaultValueInString;

    /**
     * Description of new type.
     */
    private String description;

    /**
     * reference string.
     */
    private String reference;

    /**
     * Status of the data type.
     */
    private YangStatusType status;

    /**
     * Name of the typedef.
     */
    private String name;

    /**
     * Units of the data type.
     */
    private String units;

    /**
     * List of YANG type, for typedef it will have single type.
     * This is done to unify the code with union.
     */
    private List<YangType<?>> typeList;

    /**
     * Creates a typedef node.
     */
    public YangTypeDef() {
        super(YangNodeType.TYPEDEF_NODE);
        typeList = new LinkedList<>();
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
     * Returns the data type.
     *
     * @return the data type
     */
    public YangType<?> getTypeDefBaseType() {
        if (!getTypeList().isEmpty()) {
            return getTypeList().get(0);
        }
        return null;
    }

    /**
     * Sets the data type.
     *
     * @param dataType the data type
     */
    public void setDataType(YangType<?> dataType) {
        getTypeList().add(0, dataType);
    }

    /**
     * Returns the unit.
     *
     * @return the units
     */
    public String getUnits() {
        return units;
    }

    /**
     * Sets the unit.
     *
     * @param units the units to set
     */
    public void setUnits(String units) {
        this.units = units;
    }

    /**
     * Returns the type of the data.
     *
     * @return returns TYPEDEF_DATA
     */
    @Override
    public YangConstructType getYangConstructType() {
        return YangConstructType.TYPEDEF_DATA;
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

    /**
     * Returns the YANG name of the typedef.
     *
     * @return YANG name of the typedef
     */
    @Override
    public String getName() {
        return name;
    }

    /**
     * Sets YANG name of the typedef.
     *
     * @param name YANG name of the typedef
     */
    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public List<YangType<?>> getTypeList() {
        return typeList;
    }

    @Override
    public void detectCollidingChild(String identifierName, YangConstructType dataType) throws DataModelException {
        // Asks helper to detect colliding child.
        detectCollidingChildUtil(identifierName, dataType, this);
    }

    @Override
    public void detectSelfCollision(String identifierName, YangConstructType dataType) throws DataModelException {
        if (getName().equals(identifierName)) {
            throw new DataModelException("YANG file error: Duplicate input identifier detected, same as typedef \""
                    + getName() + "\"");
        }
    }
}

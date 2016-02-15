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
import org.onosproject.yangutils.parser.ParsableDataType;

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
 * Data model node to maintain information defined in YANG typedef.
 */
public class YangTypeDef extends YangNode implements YangCommonInfo, Parsable {

    /**
     * Name of derived data type.
     */
    private String derivedName;

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
     * Derived data type.
     */
    @SuppressWarnings("rawtypes")
    private YangType derivedType;

    /**
     * Units of the data type.
     */
    private String units;

    /**
     * Create a typedef node.
     */
    public YangTypeDef() {
        super(YangNodeType.TYPEDEF_NODE);
    }

    /**
     * Get the data type name.
     *
     * @return the data type name.
     */
    public String getDerivedName() {
        return derivedName;
    }

    /**
     * Set the data type name.
     *
     * @param derrivedName data type name.
     */
    public void setDerivedName(String derrivedName) {
        derivedName = derrivedName;
    }

    /**
     * Get the default value.
     *
     * @return the default value.
     */
    public String getDefaultValueInString() {
        return defaultValueInString;
    }

    /**
     * Set the default value.
     *
     * @param defaultValueInString the default value.
     */
    public void setDefaultValueInString(String defaultValueInString) {
        this.defaultValueInString = defaultValueInString;
    }

    /**
     * Get the description.
     *
     * @return the description.
     */
    public String getDescription() {
        return description;
    }

    /**
     * Set the description.
     *
     * @param description set the description.
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Get the textual reference.
     *
     * @return the reference.
     */
    public String getReference() {
        return reference;
    }

    /**
     * Set the textual reference.
     *
     * @param reference the reference to set.
     */
    public void setReference(String reference) {
        this.reference = reference;
    }

    /**
     * Get the status.
     *
     * @return the status.
     */
    public YangStatusType getStatus() {
        return status;
    }

    /**
     * Set the status.
     *
     * @param status the status to set.
     */
    public void setStatus(YangStatusType status) {
        this.status = status;
    }

    /**
     * Get the referenced type.
     *
     * @return the referenced type.
     */
    @SuppressWarnings("rawtypes")
    public YangType getDerivedType() {
        return derivedType;
    }

    /**
     * Get the referenced type.
     *
     * @param derivedType the referenced type.
     */
    @SuppressWarnings("rawtypes")
    public void setDerivedType(YangType derivedType) {
        this.derivedType = derivedType;
    }

    /**
     * Get the unit.
     *
     * @return the units
     */
    public String getUnits() {
        return units;
    }

    /**
     * Set the unit.
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
    public ParsableDataType getParsableDataType() {
        return ParsableDataType.TYPEDEF_DATA;
    }

    /**
     * Validate the data on entering the corresponding parse tree node.
     *
     * @throws DataModelException a violation of data model rules.
     */
    public void validateDataOnEntry() throws DataModelException {
        // TODO auto-generated method stub, to be implemented by parser
    }

    /**
     * Validate the data on exiting the corresponding parse tree node.
     *
     * @throws DataModelException a violation of data model rules.
     */
    public void validateDataOnExit() throws DataModelException {
        // TODO auto-generated method stub, to be implemented by parser
    }

    /* (non-Javadoc)
     * @see org.onosproject.yangutils.datamodel.YangNode#getName()
     */
    @Override
    public String getName() {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see org.onosproject.yangutils.datamodel.YangNode#setName(java.lang.String)
     */
    @Override
    public void setName(String name) {
        // TODO Auto-generated method stub

    }

    /* (non-Javadoc)
     * @see org.onosproject.yangutils.translator.CodeGenerator#generateJavaCodeEntry()
     */
    public void generateJavaCodeEntry() {
        // TODO Auto-generated method stub

    }

    /* (non-Javadoc)
     * @see org.onosproject.yangutils.translator.CodeGenerator#generateJavaCodeExit()
     */
    public void generateJavaCodeExit() {
        // TODO Auto-generated method stub

    }

    /* (non-Javadoc)
     * @see org.onosproject.yangutils.datamodel.YangNode#getPackage()
     */
    @Override
    public String getPackage() {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see org.onosproject.yangutils.datamodel.YangNode#setPackage(java.lang.String)
     */
    @Override
    public void setPackage(String pkg) {
        // TODO Auto-generated method stub

    }
}

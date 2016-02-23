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
import org.onosproject.yangutils.translator.CachedFileHandle;

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
     * Derived data type. The type will be set when the parser detects the type
     * parsing. Hence it is of raw type and it not know at the time of creation
     * of the object. i.e. in entry parse, it will not be know, in the exit
     * parse we may know the type implicitly based on the restriction. We must
     * know and validate the base built in type, by the linking phase. It is a
     * RAW type and it usage needs to be validate in linking phase.
     */
    private YangType<?> derivedType;

    /**
     * Units of the data type.
     */
    private String units;

    /**
     * YANG base built in data type.
     */
    private YangDataTypes baseBuiltInType;

    /**
     * package of the generated java code.
     */
    private String pkg;

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
    @Override
    public String getDescription() {
        return description;
    }

    /**
     * Set the description.
     *
     * @param description set the description.
     */
    @Override
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Get the textual reference.
     *
     * @return the reference.
     */
    @Override
    public String getReference() {
        return reference;
    }

    /**
     * Set the textual reference.
     *
     * @param reference the reference to set.
     */
    @Override
    public void setReference(String reference) {
        this.reference = reference;
    }

    /**
     * Get the status.
     *
     * @return the status.
     */
    @Override
    public YangStatusType getStatus() {
        return status;
    }

    /**
     * Set the status.
     *
     * @param status the status to set.
     */
    @Override
    public void setStatus(YangStatusType status) {
        this.status = status;
    }

    /**
     * Get the referenced type.
     *
     * @return the referenced type.
     */
    public YangType<?> getDerivedType() {
        return derivedType;
    }

    /**
     * Get the referenced type.
     *
     * @param derivedType the referenced type.
     */
    public void setDerivedType(YangType<?> derivedType) {
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
     * Get the base built in YANG data type.
     *
     * @return base built in YANG data type.
     */
    public YangDataTypes getBaseBuiltInType() {
        return baseBuiltInType;
    }

    /**
     * Set the base built in YANG data type.
     *
     * @param baseBuiltInType base built in YANG data type.
     */
    public void setBaseBuiltInType(YangDataTypes baseBuiltInType) {
        this.baseBuiltInType = baseBuiltInType;
    }

    /**
     * Returns the type of the data.
     *
     * @return returns TYPEDEF_DATA
     */
    @Override
    public ParsableDataType getParsableDataType() {
        return ParsableDataType.TYPEDEF_DATA;
    }

    /**
     * Validate the data on entering the corresponding parse tree node.
     *
     * @throws DataModelException a violation of data model rules.
     */
    @Override
    public void validateDataOnEntry() throws DataModelException {
        // TODO auto-generated method stub, to be implemented by parser
    }

    /**
     * Validate the data on exiting the corresponding parse tree node.
     *
     * @throws DataModelException a violation of data model rules.
     */
    @Override
    public void validateDataOnExit() throws DataModelException {
        // TODO auto-generated method stub, to be implemented by parser
    }

    /**
     * Get the YANG name of the typedef.
     *
     * @return YANG name of the typedef.
     */
    @Override
    public String getName() {
        return derivedName;
    }

    /**
     * Set YANG name of the typedef.
     *
     * @param name YANG name of the typedef.
     */
    @Override
    public void setName(String name) {
        // TODO Auto-generated method stub

    }

    /**
     * Generate java code snippet corresponding to YANG typedef.
     */
    @Override
    public void generateJavaCodeEntry() {
        // TODO Auto-generated method stub

    }

    /**
     * Free resource used for code generation of YANG typedef.
     */
    @Override
    public void generateJavaCodeExit() {
        // TODO Auto-generated method stub

    }

    /**
     * Get the mapped java package.
     *
     * @return the java package
     */
    @Override
    public String getPackage() {
        return pkg;
    }

    /**
     * Set the mapped java package.
     *
     * @param pakg mapped java package.
     */
    @Override
    public void setPackage(String pakg) {
        pkg = pakg;

    }

    /**
     * Get the file handle of the cached file used during code generation.
     *
     * @return cached file handle.
     */
    @Override
    public CachedFileHandle getFileHandle() {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * Set the file handle to be used used for code generation.
     *
     * @param fileHandle cached file handle.
     */
    @Override
    public void setFileHandle(CachedFileHandle fileHandle) {
        // TODO Auto-generated method stub

    }
}

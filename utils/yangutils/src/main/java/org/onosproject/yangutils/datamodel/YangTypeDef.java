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

import java.io.IOException;

import org.onosproject.yangutils.datamodel.exceptions.DataModelException;
import org.onosproject.yangutils.parser.Parsable;
import org.onosproject.yangutils.translator.CachedFileHandle;
import org.onosproject.yangutils.translator.GeneratedFileType;
import org.onosproject.yangutils.translator.tojava.utils.JavaIdentifierSyntax;
import org.onosproject.yangutils.utils.UtilConstants;
import org.onosproject.yangutils.utils.YangConstructType;
import org.onosproject.yangutils.utils.io.impl.FileSystemUtil;

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
     * Maintain the derived type information.
     */
    private YangType<YangDerivedType> derivedType;

    /**
     * Units of the data type.
     */
    private String units;

    /**
     * package of the generated java code.
     */
    private String pkg;

    /**
     * Cached Java File Handle.
     */
    private CachedFileHandle fileHandle;

    /**
     * Create a typedef node.
     */
    public YangTypeDef() {
        super(YangNodeType.TYPEDEF_NODE);
    }

    /**
     * Get the default value.
     *
     * @return the default value
     */
    public String getDefaultValueInString() {
        return defaultValueInString;
    }

    /**
     * Set the default value.
     *
     * @param defaultValueInString the default value
     */
    public void setDefaultValueInString(String defaultValueInString) {
        this.defaultValueInString = defaultValueInString;
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
     * Get the derived type.
     *
     * @return the derived type
     */
    public YangType<YangDerivedType> getDerivedType() {
        return derivedType;
    }

    /**
     * Set the derived type.
     *
     * @param derivedType the derived type
     */
    public void setDerivedType(YangType<YangDerivedType> derivedType) {
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
    @Override
    public YangConstructType getYangConstructType() {
        return YangConstructType.TYPEDEF_DATA;
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
        YangType<YangDerivedType> type = getDerivedType();
        if (type == null) {
            throw new DataModelException("Typedef does not have type info.");
        }
        if (type.getDataType() != YangDataTypes.DERIVED
                || type.getDataTypeName() == null) {
            throw new DataModelException("Typedef type is not derived.");
        }

        YangDerivedType derivedTypeInfo = type.getDataTypeExtendedInfo();
        if (derivedTypeInfo == null) {
            throw new DataModelException("derrived type does not have derived info.");
        }

        YangType<?> baseType = derivedTypeInfo.getBaseType();
        if (baseType == null) {
            throw new DataModelException("Base type of a derived type is missing.");
        }

        if (derivedTypeInfo.getEffectiveYangBuiltInType() == null) {
            /* resolve the effective type from the data tree. */
            /*
             * TODO: try to resolve the nested reference, if possible in the
             * partial tree, otherwise we need to resolve finally when the
             * complete module is created.
             */
            YangModule.addToResolveList(this);
        }
    }

    /**
     * Get the YANG name of the typedef.
     *
     * @return YANG name of the typedef
     */
    @Override
    public String getName() {
        if (getDerivedType() != null) {
            return getDerivedType().getDataTypeName();
        }
        return null;
    }

    /**
     * Set YANG name of the typedef.
     *
     * @param name YANG name of the typedef
     */
    @Override
    public void setName(String name) {
        if (getDerivedType() == null) {
            throw new RuntimeException(
                    "Derrived Type info needs to be set in parser when the typedef listner is processed");
        }
        getDerivedType().setDataTypeName(name);
        getDerivedType().setDataType(YangDataTypes.DERIVED);
    }

    /**
     * Generate java code snippet corresponding to YANG typedef.
     *
     * @param codeGenDir code generation directory
     * @throws IOException when fails to generate files for typedef
     */
    @Override
    public void generateJavaCodeEntry(String codeGenDir) throws IOException {

        YangNode parent = getParent();
        String typeDefPkg = JavaIdentifierSyntax.getPackageFromParent(parent.getPackage(), parent.getName());

        typeDefPkg = JavaIdentifierSyntax.getCamelCase(typeDefPkg).toLowerCase();
        setPackage(typeDefPkg);

        CachedFileHandle handle = null;
        try {
            FileSystemUtil.createPackage(codeGenDir + getPackage(), parent.getName() + UtilConstants.CHILDREN);
            handle = FileSystemUtil.createSourceFiles(getPackage(), getName(),
                    GeneratedFileType.GENERATE_TYPEDEF_CLASS);
            handle.setRelativeFilePath(getPackage().replace(".", "/"));
            handle.setCodeGenFilePath(codeGenDir);
        } catch (IOException e) {
            throw new IOException("Failed to create the source files.");
        }
        setFileHandle(handle);
        getDerivedType().getDataTypeExtendedInfo().getBaseType().setJavaPackage(getPackage());
        addAttributeInfo();
        addAttributeInParent();
    }

    /**
     * Adds current node attribute to parent file.
     */
    private void addAttributeInParent() {
        if (getParent() != null) {
            getParent().getFileHandle().addAttributeInfo(null, getName(), false);
        }
    }

    /**
     * Adds attribute to file handle.
     */
    private void addAttributeInfo() {
        getFileHandle().addAttributeInfo(getDerivedType().getDataTypeExtendedInfo().getBaseType(),
                JavaIdentifierSyntax.getCamelCase(getName()), false);
    }

    /**
     * Free resource used for code generation of YANG typedef.
     *
     * @throws IOException when fails to generate files
     */
    @Override
    public void generateJavaCodeExit() throws IOException {
        getFileHandle().close();
        return;
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
     * @param pakg mapped java package
     */
    @Override
    public void setPackage(String pakg) {
        pkg = pakg;

    }

    /**
     * Get the file handle of the cached file used during code generation.
     *
     * @return cached file handle
     */
    @Override
    public CachedFileHandle getFileHandle() {
        return fileHandle;
    }

    /**
     * Set the file handle to be used used for code generation.
     *
     * @param handle cached file handle
     */
    @Override
    public void setFileHandle(CachedFileHandle handle) {
        fileHandle = handle;
    }
}

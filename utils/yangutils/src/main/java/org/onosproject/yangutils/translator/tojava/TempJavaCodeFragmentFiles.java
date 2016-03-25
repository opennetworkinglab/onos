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

package org.onosproject.yangutils.translator.tojava;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.onosproject.yangutils.datamodel.YangLeaf;
import org.onosproject.yangutils.datamodel.YangLeafList;
import org.onosproject.yangutils.datamodel.YangLeavesHolder;
import org.onosproject.yangutils.datamodel.YangNode;
import org.onosproject.yangutils.datamodel.YangTypeDef;
import org.onosproject.yangutils.translator.tojava.utils.JavaCodeSnippetGen;
import org.onosproject.yangutils.translator.tojava.utils.JavaIdentifierSyntax;
import org.onosproject.yangutils.utils.UtilConstants;
import org.onosproject.yangutils.utils.io.impl.FileSystemUtil;
import org.onosproject.yangutils.utils.io.impl.JavaDocGen;
import org.onosproject.yangutils.utils.io.impl.JavaDocGen.JavaDocType;

import static org.onosproject.yangutils.translator.tojava.GeneratedJavaFileType.BUILDER_CLASS_MASK;
import static org.onosproject.yangutils.translator.tojava.GeneratedJavaFileType.BUILDER_INTERFACE_MASK;
import static org.onosproject.yangutils.translator.tojava.GeneratedJavaFileType.GENERATE_INTERFACE_WITH_BUILDER;
import static org.onosproject.yangutils.translator.tojava.GeneratedJavaFileType.GENERATE_TYPEDEF_CLASS;
import static org.onosproject.yangutils.translator.tojava.GeneratedJavaFileType.IMPL_CLASS_MASK;
import static org.onosproject.yangutils.translator.tojava.GeneratedJavaFileType.INTERFACE_MASK;
import static org.onosproject.yangutils.translator.tojava.GeneratedTempFileType.ATTRIBUTES_MASK;
import static org.onosproject.yangutils.translator.tojava.GeneratedTempFileType.CONSTRUCTOR_IMPL_MASK;
import static org.onosproject.yangutils.translator.tojava.GeneratedTempFileType.EQUALS_IMPL_MASK;
import static org.onosproject.yangutils.translator.tojava.GeneratedTempFileType.GETTER_FOR_CLASS_MASK;
import static org.onosproject.yangutils.translator.tojava.GeneratedTempFileType.GETTER_FOR_INTERFACE_MASK;
import static org.onosproject.yangutils.translator.tojava.GeneratedTempFileType.HASH_CODE_IMPL_MASK;
import static org.onosproject.yangutils.translator.tojava.GeneratedTempFileType.SETTER_FOR_CLASS_MASK;
import static org.onosproject.yangutils.translator.tojava.GeneratedTempFileType.SETTER_FOR_INTERFACE_MASK;
import static org.onosproject.yangutils.translator.tojava.GeneratedTempFileType.TO_STRING_IMPL_MASK;
import static org.onosproject.yangutils.translator.tojava.JavaAttributeInfo.getAttributeInfoOfLeaf;
import static org.onosproject.yangutils.translator.tojava.JavaAttributeInfo.getAttributeInfoOfTypeDef;
import static org.onosproject.yangutils.translator.tojava.JavaAttributeInfo.getCurNodeAsAttributeInParent;
import static org.onosproject.yangutils.translator.tojava.utils.JavaCodeSnippetGen.getJavaAttributeDefination;
import static org.onosproject.yangutils.translator.tojava.utils.JavaFileGenerator.generateBuilderClassFile;
import static org.onosproject.yangutils.translator.tojava.utils.JavaFileGenerator.generateBuilderInterfaceFile;
import static org.onosproject.yangutils.translator.tojava.utils.JavaFileGenerator.generateImplClassFile;
import static org.onosproject.yangutils.translator.tojava.utils.JavaFileGenerator.generateInterfaceFile;
import static org.onosproject.yangutils.translator.tojava.utils.JavaFileGenerator.generateTypeDefClassFile;
import static org.onosproject.yangutils.translator.tojava.utils.JavaFileGeneratorUtils.getFileObject;
import static org.onosproject.yangutils.translator.tojava.utils.JavaIdentifierSyntax.getParentNodeInGenCode;
import static org.onosproject.yangutils.translator.tojava.utils.MethodsGenerator.getBuildString;
import static org.onosproject.yangutils.translator.tojava.utils.MethodsGenerator.getConstructor;
import static org.onosproject.yangutils.translator.tojava.utils.MethodsGenerator.getDefaultConstructorString;
import static org.onosproject.yangutils.translator.tojava.utils.MethodsGenerator.getEqualsMethod;
import static org.onosproject.yangutils.translator.tojava.utils.MethodsGenerator.getGetterForClass;
import static org.onosproject.yangutils.translator.tojava.utils.MethodsGenerator.getGetterString;
import static org.onosproject.yangutils.translator.tojava.utils.MethodsGenerator.getHashCodeMethod;
import static org.onosproject.yangutils.translator.tojava.utils.MethodsGenerator.getOfMethod;
import static org.onosproject.yangutils.translator.tojava.utils.MethodsGenerator.getOverRideString;
import static org.onosproject.yangutils.translator.tojava.utils.MethodsGenerator.getSetterForClass;
import static org.onosproject.yangutils.translator.tojava.utils.MethodsGenerator.getSetterForTypeDefClass;
import static org.onosproject.yangutils.translator.tojava.utils.MethodsGenerator.getSetterString;
import static org.onosproject.yangutils.translator.tojava.utils.MethodsGenerator.getToStringMethod;
import static org.onosproject.yangutils.translator.tojava.utils.MethodsGenerator.getTypeDefConstructor;
import static org.onosproject.yangutils.translator.tojava.utils.MethodsGenerator.parseBuilderInterfaceBuildMethodString;
import static org.onosproject.yangutils.utils.UtilConstants.BUILDER;
import static org.onosproject.yangutils.utils.UtilConstants.EMPTY_STRING;
import static org.onosproject.yangutils.utils.UtilConstants.FOUR_SPACE_INDENTATION;
import static org.onosproject.yangutils.utils.UtilConstants.IMPL;
import static org.onosproject.yangutils.utils.UtilConstants.INTERFACE;
import static org.onosproject.yangutils.utils.UtilConstants.NEW_LINE;
import static org.onosproject.yangutils.utils.UtilConstants.PERIOD;
import static org.onosproject.yangutils.utils.UtilConstants.SLASH;
import static org.onosproject.yangutils.utils.io.impl.FileSystemUtil.createPackage;
import static org.onosproject.yangutils.utils.io.impl.JavaDocGen.getJavaDoc;
import static org.onosproject.yangutils.utils.io.impl.JavaDocGen.JavaDocType.GETTER_METHOD;
import static org.onosproject.yangutils.utils.io.impl.JavaDocGen.JavaDocType.TYPE_DEF_CONSTRUCTOR;
import static org.onosproject.yangutils.utils.io.impl.JavaDocGen.JavaDocType.TYPE_DEF_SETTER_METHOD;
import static org.onosproject.yangutils.utils.io.impl.YangIoUtils.clean;
import static org.onosproject.yangutils.utils.io.impl.YangIoUtils.insertDataIntoJavaFile;
import static org.onosproject.yangutils.utils.io.impl.YangIoUtils.mergeJavaFiles;

/**
 * Provides implementation of java code fragments temporary implementations.
 */
public class TempJavaCodeFragmentFiles {

    /**
     * The variable which guides the types of temporary files generated using
     * the temporary generated file types mask.
     */
    private int generatedTempFiles;

    /**
     * The variable which guides the types of files generated using
     * the generated file types mask.
     */
    private int generatedJavaFiles;

    /**
     * Absolute path where the target java file needs to be generated.
     */
    private String absoluteDirPath;

    /**
     * Name of java file that needs to be generated.
     */
    private String generatedJavaClassName;

    /**
     * File type extension for java classes.
     */
    private static final String JAVA_FILE_EXTENSION = ".java";

    /**
     * File type extension for temporary classes.
     */
    private static final String TEMP_FILE_EXTENSION = ".tmp";

    /**
     * Folder suffix for temporary files folder.
     */
    private static final String TEMP_FOLDER_NAME_SUFIX = "-Temp";

    /**
     * File name for getter method.
     */
    private static final String GETTER_METHOD_FILE_NAME = "GetterMethod";

    /**
     * File name for getter method implementation.
     */
    private static final String GETTER_METHOD_IMPL_FILE_NAME = "GetterMethodImpl";

    /**
     * File name for setter method.
     */
    private static final String SETTER_METHOD_FILE_NAME = "SetterMethod";

    /**
     * File name for setter method implementation.
     */
    private static final String SETTER_METHOD_IMPL_FILE_NAME = "SetterMethodImpl";

    /**
     * File name for constructor.
     */
    private static final String CONSTRUCTOR_FILE_NAME = "Constructor";

    /**
     * File name for attributes.
     */
    private static final String ATTRIBUTE_FILE_NAME = "Attributes";

    /**
     * File name for to string method.
     */
    private static final String TO_STRING_METHOD_FILE_NAME = "ToString";

    /**
     * File name for hash code method.
     */
    private static final String HASH_CODE_METHOD_FILE_NAME = "HashCode";

    /**
     * File name for equals method.
     */
    private static final String EQUALS_METHOD_FILE_NAME = "Equals";

    /**
     * File name for interface java file name suffix.
     */
    private static final String INTERFACE_FILE_NAME_SUFFIX = EMPTY_STRING;

    /**
     * File name for builder interface file name suffix.
     */
    private static final String BUILDER_INTERFACE_FILE_NAME_SUFFIX = BUILDER + INTERFACE;

    /**
     * File name for builder class file name suffix.
     */
    private static final String BUILDER_CLASS_FILE_NAME_SUFFIX = BUILDER;

    /**
     * File name for impl class file name suffix.
     */
    private static final String IMPL_CLASS_FILE_NAME_SUFFIX = IMPL;

    /**
     * File name for typedef class file name suffix.
     */
    private static final String TYPEDEF_CLASS_FILE_NAME_SUFFIX = EMPTY_STRING;

    /**
     * Java file handle for interface file.
     */
    private File interfaceJavaFileHandle;

    /**
     * Java file handle for builder interface file.
     */
    private File builderInterfaceJavaFileHandle;

    /**
     * Java file handle for builder class file.
     */
    private File builderClassJavaFileHandle;

    /**
     * Java file handle for impl class file.
     */
    private File implClassJavaFileHandle;

    /**
     * Java file handle for typedef class file.
     */
    private File typedefClassJavaFileHandle;

    /**
     * Temporary file handle for attribute.
     */
    private File attributesTempFileHandle;

    /**
     * Temporary file handle for getter of interface.
     */
    private File getterInterfaceTempFileHandle;

    /**
     * Temporary file handle for getter of class.
     */
    private File getterImplTempFileHandle;

    /**
     * Temporary file handle for setter of interface.
     */
    private File setterInterfaceTempFileHandle;

    /**
     * Temporary file handle for setter of class.
     */
    private File setterImplTempFileHandle;

    /**
     * Temporary file handle for constructor of class.
     */
    private File constructorImplTempFileHandle;

    /**
     * Temporary file handle for hash code method of class.
     */
    private File hashCodeImplTempFileHandle;

    /**
     * Temporary file handle for equals method of class.
     */
    private File equalsImplTempFileHandle;

    /**
     * Temporary file handle for to string method of class.
     */
    private File toStringImplTempFileHandle;

    /**
     * Java attribute info.
     */
    private JavaAttributeInfo newAttrInfo;

    /**
     * Current YANG node.
     */
    private YangNode curYangNode;

    /**
     * Is attribute added.
     */
    private boolean isAttributePresent = false;

    /**
     * Construct an object of temporary java code fragment.
     *
     * @param genFileType file generation type
     * @param genDir file generation directory
     * @param className class name
     * @throws IOException when fails to create new file handle
     */
    public TempJavaCodeFragmentFiles(int genFileType, String genDir, String className)
            throws IOException {

        generatedTempFiles = 0;
        absoluteDirPath = genDir;
        generatedJavaClassName = className;
        generatedJavaFiles = genFileType;
        /**
         * Initialize getter when generation file type matches to interface
         * mask.
         */
        if ((genFileType & INTERFACE_MASK) != 0) {
            generatedTempFiles |= GETTER_FOR_INTERFACE_MASK;
        }

        /**
         * Initialize getter and setter when generation file type matches to
         * builder interface mask.
         */
        if ((genFileType & BUILDER_INTERFACE_MASK) != 0) {
            generatedTempFiles |= GETTER_FOR_INTERFACE_MASK;
            generatedTempFiles |= SETTER_FOR_INTERFACE_MASK;
        }

        /**
         * Initialize getterImpl, setterImpl and attributes when generation file
         * type matches to builder class mask.
         */
        if ((genFileType & BUILDER_CLASS_MASK) != 0) {
            generatedTempFiles |= ATTRIBUTES_MASK;
            generatedTempFiles |= GETTER_FOR_CLASS_MASK;
            generatedTempFiles |= SETTER_FOR_CLASS_MASK;
        }

        /**
         * Initialize getterImpl, attributes, constructor, hash code, equals and
         * to strings when generation file type matches to impl class mask.
         */
        if ((genFileType & IMPL_CLASS_MASK) != 0) {
            generatedTempFiles |= ATTRIBUTES_MASK;
            generatedTempFiles |= GETTER_FOR_CLASS_MASK;
            generatedTempFiles |= CONSTRUCTOR_IMPL_MASK;
            generatedTempFiles |= HASH_CODE_IMPL_MASK;
            generatedTempFiles |= EQUALS_IMPL_MASK;
            generatedTempFiles |= TO_STRING_IMPL_MASK;
        }

        /**
         * Initialize getterImpl, attributes,  hash code, equals and
         * to strings when generation file type matches to typeDef class mask.
         */
        if ((genFileType & GENERATE_TYPEDEF_CLASS) != 0) {
            generatedTempFiles |= ATTRIBUTES_MASK;
            generatedTempFiles |= GETTER_FOR_CLASS_MASK;
            generatedTempFiles |= HASH_CODE_IMPL_MASK;
            generatedTempFiles |= EQUALS_IMPL_MASK;
            generatedTempFiles |= TO_STRING_IMPL_MASK;
        }

        if ((generatedTempFiles & ATTRIBUTES_MASK) != 0) {
            setAttributesTempFileHandle(getTemporaryFileHandle(ATTRIBUTE_FILE_NAME));
        }

        if ((generatedTempFiles & GETTER_FOR_INTERFACE_MASK) != 0) {
            setGetterInterfaceTempFileHandle(getTemporaryFileHandle(GETTER_METHOD_FILE_NAME));
        }

        if ((generatedTempFiles & SETTER_FOR_INTERFACE_MASK) != 0) {
            setSetterInterfaceTempFileHandle(getTemporaryFileHandle(SETTER_METHOD_FILE_NAME));
        }

        if ((generatedTempFiles & GETTER_FOR_CLASS_MASK) != 0) {
            setGetterImplTempFileHandle(getTemporaryFileHandle(GETTER_METHOD_IMPL_FILE_NAME));
        }

        if ((generatedTempFiles & SETTER_FOR_CLASS_MASK) != 0) {
            setSetterImplTempFileHandle(getTemporaryFileHandle(SETTER_METHOD_IMPL_FILE_NAME));
        }

        if ((generatedTempFiles & CONSTRUCTOR_IMPL_MASK) != 0) {
            setConstructorImplTempFileHandle(getTemporaryFileHandle(CONSTRUCTOR_FILE_NAME));
        }

        if ((generatedTempFiles & HASH_CODE_IMPL_MASK) != 0) {
            setHashCodeImplTempFileHandle(getTemporaryFileHandle(HASH_CODE_METHOD_FILE_NAME));
        }

        if ((generatedTempFiles & EQUALS_IMPL_MASK) != 0) {
            setEqualsImplTempFileHandle(getTemporaryFileHandle(EQUALS_METHOD_FILE_NAME));
        }
        if ((generatedTempFiles & TO_STRING_IMPL_MASK) != 0) {
            setToStringImplTempFileHandle(getTemporaryFileHandle(TO_STRING_METHOD_FILE_NAME));
        }

    }

    /**
     * Returns java file handle for interface file.
     *
     * @return java file handle for interface file
     */
    public File getInterfaceJavaFileHandle() {

        return interfaceJavaFileHandle;
    }

    /**
     * Sets the java file handle for interface file.
     *
     * @param interfaceJavaFileHandle java file handle
     */
    public void setInterfaceJavaFileHandle(File interfaceJavaFileHandle) {

        this.interfaceJavaFileHandle = interfaceJavaFileHandle;
    }

    /**
     * Returns java file handle for builder interface file.
     *
     * @return java file handle for builder interface file
     */
    public File getBuilderInterfaceJavaFileHandle() {

        return builderInterfaceJavaFileHandle;
    }

    /**
     * Sets the java file handle for builder interface file.
     *
     * @param builderInterfaceJavaFileHandle java file handle
     */
    public void setBuilderInterfaceJavaFileHandle(File builderInterfaceJavaFileHandle) {

        this.builderInterfaceJavaFileHandle = builderInterfaceJavaFileHandle;
    }

    /**
     * Returns java file handle for builder class file.
     *
     * @return java file handle for builder class file
     */
    public File getBuilderClassJavaFileHandle() {

        return builderClassJavaFileHandle;
    }

    /**
     * Sets the java file handle for builder class file.
     *
     * @param builderClassJavaFileHandle java file handle
     */
    public void setBuilderClassJavaFileHandle(File builderClassJavaFileHandle) {

        this.builderClassJavaFileHandle = builderClassJavaFileHandle;
    }

    /**
     * Returns java file handle for impl class file.
     *
     * @return java file handle for impl class file
     */
    public File getImplClassJavaFileHandle() {

        return implClassJavaFileHandle;
    }

    /**
     * Sets the java file handle for impl class file.
     *
     * @param implClassJavaFileHandle java file handle
     */
    public void setImplClassJavaFileHandle(File implClassJavaFileHandle) {

        this.implClassJavaFileHandle = implClassJavaFileHandle;
    }

    /**
     * Returns java file handle for typedef class file.
     *
     * @return java file handle for typedef class file
     */
    public File getTypedefClassJavaFileHandle() {

        return typedefClassJavaFileHandle;
    }

    /**
     * Sets the java file handle for typedef class file.
     *
     * @param typedefClassJavaFileHandle java file handle
     */
    public void setTypedefClassJavaFileHandle(File typedefClassJavaFileHandle) {

        this.typedefClassJavaFileHandle = typedefClassJavaFileHandle;
    }

    /**
     * Returns attribute's temporary file handle.
     *
     * @return temporary file handle
     */
    public File getAttributesTempFileHandle() {

        return attributesTempFileHandle;
    }

    /**
     * Sets attribute's temporary file handle.
     *
     * @param attributeForClass file handle for attribute
     */
    public void setAttributesTempFileHandle(File attributeForClass) {

        attributesTempFileHandle = attributeForClass;
    }

    /**
     * Returns getter methods's temporary file handle.
     *
     * @return temporary file handle
     */
    public File getGetterInterfaceTempFileHandle() {

        return getterInterfaceTempFileHandle;
    }

    /**
     * Sets to getter method's temporary file handle.
     *
     * @param getterForInterface file handle for to getter method
     */
    public void setGetterInterfaceTempFileHandle(File getterForInterface) {

        getterInterfaceTempFileHandle = getterForInterface;
    }

    /**
     * Returns getter method's impl's temporary file handle.
     *
     * @return temporary file handle
     */
    public File getGetterImplTempFileHandle() {

        return getterImplTempFileHandle;
    }

    /**
     * Sets to getter method's impl's temporary file handle.
     *
     * @param getterImpl file handle for to getter method's impl
     */
    public void setGetterImplTempFileHandle(File getterImpl) {

        getterImplTempFileHandle = getterImpl;
    }

    /**
     * Returns setter method's temporary file handle.
     *
     * @return temporary file handle
     */
    public File getSetterInterfaceTempFileHandle() {

        return setterInterfaceTempFileHandle;
    }

    /**
     * Sets to setter method's temporary file handle.
     *
     * @param setterForInterface file handle for to setter method
     */
    public void setSetterInterfaceTempFileHandle(File setterForInterface) {

        setterInterfaceTempFileHandle = setterForInterface;
    }

    /**
     * Returns setter method's impl's temporary file handle.
     *
     * @return temporary file handle
     */
    public File getSetterImplTempFileHandle() {

        return setterImplTempFileHandle;
    }

    /**
     * Sets to setter method's impl's temporary file handle.
     *
     * @param setterImpl file handle for to setter method's implementation class
     */
    public void setSetterImplTempFileHandle(File setterImpl) {

        setterImplTempFileHandle = setterImpl;
    }

    /**
     * Returns constructor's temporary file handle.
     *
     * @return temporary file handle
     */
    public File getConstructorImplTempFileHandle() {

        return constructorImplTempFileHandle;
    }

    /**
     * Sets to constructor's temporary file handle.
     *
     * @param constructor file handle for to constructor
     */
    public void setConstructorImplTempFileHandle(File constructor) {

        constructorImplTempFileHandle = constructor;
    }

    /**
     * Returns hash code method's temporary file handle.
     *
     * @return temporary file handle
     */
    public File getHashCodeImplTempFileHandle() {

        return hashCodeImplTempFileHandle;
    }

    /**
     * Sets hash code method's temporary file handle.
     *
     * @param hashCodeMethod file handle for hash code method
     */
    public void setHashCodeImplTempFileHandle(File hashCodeMethod) {

        hashCodeImplTempFileHandle = hashCodeMethod;
    }

    /**
     * Returns equals mehtod's temporary file handle.
     *
     * @return temporary file handle
     */
    public File getEqualsImplTempFileHandle() {

        return equalsImplTempFileHandle;
    }

    /**
     * Sets equals method's temporary file handle.
     *
     * @param equalsMethod file handle for to equals method
     */
    public void setEqualsImplTempFileHandle(File equalsMethod) {

        equalsImplTempFileHandle = equalsMethod;
    }

    /**
     * Returns to string method's temporary file handle.
     *
     * @return temporary file handle
     */
    public File getToStringImplTempFileHandle() {

        return toStringImplTempFileHandle;
    }

    /**
     * Sets to string method's temporary file handle.
     *
     * @param toStringMethod file handle for to string method
     */
    public void setToStringImplTempFileHandle(File toStringMethod) {

        toStringImplTempFileHandle = toStringMethod;
    }

    /**
     * Returns java attribute info.
     *
     * @return java attribute info
     */
    public JavaAttributeInfo getNewAttrInfo() {

        return newAttrInfo;
    }

    /**
     * Sets java attribute info.
     *
     * @param newAttrInfo java attribute info
     */
    public void setNewAttrInfo(JavaAttributeInfo newAttrInfo) {

        if (newAttrInfo != null) {
            isAttributePresent = true;
        }
        this.newAttrInfo = newAttrInfo;
    }

    /**
     * Returns current YANG node.
     *
     * @return current YANG node
     */
    public YangNode getCurYangNode() {

        return curYangNode;
    }

    /**
     * Sets current YANG node.
     *
     * @param curYangNode YANG node
     */
    public void setCurYangNode(YangNode curYangNode) {

        this.curYangNode = curYangNode;
    }

    /**
     * Adds attribute for class.
     *
     * @param attr attribute info
     * @throws IOException when fails to append to temporary file
     */
    public void addAttribute(JavaAttributeInfo attr) throws IOException {

        appendToFile(getAttributesTempFileHandle(), parseAttribute(attr) + FOUR_SPACE_INDENTATION);
    }

    /**
     * Adds getter for interface.
     *
     * @param attr attribute info
     * @throws IOException when fails to append to temporary file
     */
    public void addGetterForInterface(JavaAttributeInfo attr) throws IOException {

        appendToFile(getGetterInterfaceTempFileHandle(), getGetterString(attr) + NEW_LINE);
    }

    /**
     * Adds getter method's impl for class.
     *
     * @param attr attribute info
     * @param genFiletype generated file type
     * @throws IOException when fails to append to temporary file
     */
    public void addGetterImpl(JavaAttributeInfo attr, int genFiletype) throws IOException {

        if ((genFiletype & BUILDER_CLASS_MASK) != 0) {
            appendToFile(getGetterImplTempFileHandle(), getOverRideString() + getGetterForClass(attr) + NEW_LINE);
        } else {
            appendToFile(getGetterImplTempFileHandle(), getJavaDoc(GETTER_METHOD, attr.getAttributeName(), false)
                    + getGetterForClass(attr) + NEW_LINE);
        }
    }

    /**
     * Adds setter for interface.
     *
     * @param attr attribute info
     * @throws IOException when fails to append to temporary file
     */
    public void addSetterForInterface(JavaAttributeInfo attr) throws IOException {

        appendToFile(getSetterInterfaceTempFileHandle(),
                getSetterString(attr, generatedJavaClassName) + NEW_LINE);
    }

    /**
     * Adds setter's implementation for class.
     *
     * @param attr attribute info
     * @throws IOException when fails to append to temporary file
     */
    public void addSetterImpl(JavaAttributeInfo attr) throws IOException {

        appendToFile(getSetterImplTempFileHandle(),
                getOverRideString() + getSetterForClass(attr, generatedJavaClassName) + NEW_LINE);
    }

    /**
     * Adds build method for interface.
     *
     * @return build method for interface
     * @throws IOException when fails to append to temporary file
     */
    public String addBuildMethodForInterface() throws IOException {

        return parseBuilderInterfaceBuildMethodString(generatedJavaClassName);
    }

    /**
     * Adds build method's implementation for class.
     *
     * @return build method implementation for class
     * @throws IOException when fails to append to temporary file
     */
    public String addBuildMethodImpl() throws IOException {

        return getBuildString(generatedJavaClassName) + NEW_LINE;
    }

    /**
     * Adds constructor for class.
     *
     * @param attr attribute info
     * @throws IOException when fails to append to temporary file
     */
    public void addConstructor(JavaAttributeInfo attr) throws IOException {

        appendToFile(getConstructorImplTempFileHandle(), getConstructor(generatedJavaClassName, attr));
    }

    /**
     * Adds default constructor for class.
     *
     * @param modifier modifier for constructor.
     * @param toAppend string which need to be appended with the class name
     * @return default constructor for class
     * @throws IOException when fails to append to file
     */
    public String addDefaultConstructor(String modifier, String toAppend) throws IOException {

        return NEW_LINE + getDefaultConstructorString(generatedJavaClassName + toAppend, modifier);
    }

    /**
     * Adds typedef constructor for class.
     *
     * @return typedef constructor for class
     * @throws IOException when fails to append to file
     */
    public String addTypeDefConstructor() throws IOException {

        return NEW_LINE + getJavaDoc(TYPE_DEF_CONSTRUCTOR, generatedJavaClassName, false)
                + getTypeDefConstructor(newAttrInfo, generatedJavaClassName) + NEW_LINE;
    }

    /**
     * Adds default constructor for class.
     *
     * @return default constructor for class
     * @throws IOException when fails to append to file
     */
    public String addTypeDefsSetter() throws IOException {

        return getJavaDoc(TYPE_DEF_SETTER_METHOD, generatedJavaClassName, false) + getSetterForTypeDefClass(newAttrInfo)
                + NEW_LINE;
    }

    /**
     * Adds default constructor for class.
     *
     * @return default constructor for class
     * @throws IOException when fails to append to file
     */
    public String addOfMethod() throws IOException {

        return JavaDocGen.getJavaDoc(JavaDocType.OF_METHOD, generatedJavaClassName, false)
                + getOfMethod(generatedJavaClassName, newAttrInfo);
    }

    /**
     * Adds hash code method for class.
     *
     * @param attr attribute info
     * @throws IOException when fails to append to temporary file
     */
    public void addHashCodeMethod(JavaAttributeInfo attr) throws IOException {

        appendToFile(getHashCodeImplTempFileHandle(), getHashCodeMethod(attr) + NEW_LINE);
    }

    /**
     * Adds equals method for class.
     *
     * @param attr attribute info
     * @throws IOException when fails to append to temporary file
     */
    public void addEqualsMethod(JavaAttributeInfo attr) throws IOException {

        appendToFile(getEqualsImplTempFileHandle(), getEqualsMethod(attr) + NEW_LINE);
    }

    /**
     * Adds ToString method for class.
     *
     * @param attr attribute info
     * @throws IOException when fails to append to temporary file
     */
    public void addToStringMethod(JavaAttributeInfo attr) throws IOException {

        appendToFile(getToStringImplTempFileHandle(), getToStringMethod(attr) + NEW_LINE);
    }

    /**
     * Returns a temporary file handle for the specific file type.
     *
     * @param fileName file name
     * @return temporary file handle
     * @throws IOException when fails to create new file handle
     */
    private File getTemporaryFileHandle(String fileName) throws IOException {

        String path = getTempDirPath();
        File dir = new File(path);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        File file = new File(path + fileName + TEMP_FILE_EXTENSION);
        if (!file.exists()) {
            file.createNewFile();
        }
        return file;
    }

    /**
     * Returns a temporary file handle for the specific file type.
     *
     * @param fileName file name
     * @return temporary file handle
     * @throws IOException when fails to create new file handle
     */
    private File getJavaFileHandle(String fileName) throws IOException {

        createPackage(absoluteDirPath, getJavaFileInfo().getJavaName());

        return getFileObject(getDirPath(), fileName, JAVA_FILE_EXTENSION, getJavaFileInfo());
    }

    /**
     * Returns data from the temporary files.
     *
     * @param file temporary file handle
     * @return stored data from temporary files
     * @throws IOException when failed to get data from the given file
     */
    public String getTemporaryDataFromFileHandle(File file) throws IOException {

        String path = getTempDirPath();
        if (new File(path + file.getName()).exists()) {
            return FileSystemUtil.readAppendFile(path + file.getName(), EMPTY_STRING);
        } else {
            throw new IOException("Unable to get data from the given "
                    + file.getName() + " file for " + generatedJavaClassName + PERIOD);
        }
    }

    /**
     * Returns temporary directory path.
     *
     * @return directory path
     */
    private String getTempDirPath() {

        return absoluteDirPath.replace(PERIOD, UtilConstants.SLASH)
                + SLASH + generatedJavaClassName + TEMP_FOLDER_NAME_SUFIX + SLASH;
    }

    /**
     * Parse attribute to get the attribute string.
     *
     * @param attr attribute info
     * @return attribute string
     */
    private String parseAttribute(JavaAttributeInfo attr) {

        /*
         * TODO: check if this utility needs to be called or move to the caller
         */
        String attributeName = JavaIdentifierSyntax
                .getCamelCase(JavaIdentifierSyntax.getSmallCase(attr.getAttributeName()));
        if (attr.isQualifiedName()) {
            return getJavaAttributeDefination(attr.getImportInfo().getPkgInfo(), attr.getImportInfo().getClassInfo(),
                    attributeName, attr.isListAttr());
        } else {
            return getJavaAttributeDefination(null, attr.getImportInfo().getClassInfo(), attributeName,
                    attr.isListAttr());
        }
    }

    /**
     * Append content to temporary file.
     *
     * @param file temporary file
     * @param data data to be appended
     * @throws IOException when fails to append to file
     */
    private void appendToFile(File file, String data) throws IOException {

        try {
            insertDataIntoJavaFile(file, data);
        } catch (IOException ex) {
            throw new IOException("failed to write in temp file.");
        }
    }

    /**
     * Adds current node info as and attribute to the parent generated file.
     *
     * @param curNode current node which needs to be added as an attribute in
     *            the parent generated code
     * @param isList is list construct
     * @throws IOException IO operation exception
     */
    public void addCurNodeInfoInParentTempFile(YangNode curNode,
            boolean isList) throws IOException {

        YangNode parent = getParentNodeInGenCode(curNode);
        if (!(parent instanceof JavaCodeGenerator)) {
            throw new RuntimeException("missing parent node to contain current node info in generated file");
        }
        JavaAttributeInfo javaAttributeInfo = getCurNodeAsAttributeInParent(curNode,
                parent, isList);

        if (!(parent instanceof HasTempJavaCodeFragmentFiles)) {
            throw new RuntimeException("missing parent temp file handle");
        }
        ((HasTempJavaCodeFragmentFiles) parent)
                .getTempJavaCodeFragmentFiles()
                .addJavaSnippetInfoToApplicableTempFiles(javaAttributeInfo);
    }

    /**
     * Adds leaf attributes in generated files.
     *
     * @param listOfLeaves list of YANG leaf
     * @param curNode current data model node
     * @throws IOException IO operation fail
     */
    private void addLeavesInfoToTempFiles(List<YangLeaf> listOfLeaves,
            YangNode curNode) throws IOException {

        if (listOfLeaves != null) {
            for (YangLeaf leaf : listOfLeaves) {
                JavaAttributeInfo javaAttributeInfo = getAttributeInfoOfLeaf(curNode,
                        leaf.getDataType(),
                        leaf.getLeafName(), false);
                addJavaSnippetInfoToApplicableTempFiles(javaAttributeInfo);
            }
        }
    }

    /**
     * Adds leaf list's attributes in generated files.
     *
     * @param listOfLeafList list of YANG leaves
     * @param curNode cached file handle
     * @throws IOException IO operation fail
     */
    private void addLeafListInfoToTempFiles(List<YangLeafList> listOfLeafList,
            YangNode curNode) throws IOException {

        if (listOfLeafList != null) {

            /*
             * Check if the attribute is of type list, then the java.lang.list
             * needs to be imported.
             */
            if (listOfLeafList.size() != 0) {
                if (!(curNode instanceof HasJavaImportData)) {
                    throw new RuntimeException("missing import info in current data model node");

                }
                ((HasJavaImportData) curNode).getJavaImportData()
                        .setIfListImported(true);

            }

            for (YangLeafList leafList : listOfLeafList) {
                JavaAttributeInfo javaAttributeInfo = getAttributeInfoOfLeaf(
                        curNode, leafList.getDataType(), leafList.getLeafName(),
                        true);
                addJavaSnippetInfoToApplicableTempFiles(javaAttributeInfo);
            }
        }
    }

    /**
     * Add all the leaves in the current data model node as part of the
     * generated temporary file.
     *
     * @param curNode java file info of the generated file
     * @throws IOException IO operation fail
     */
    public void addCurNodeLeavesInfoToTempFiles(YangNode curNode) throws IOException {

        if (curNode instanceof YangLeavesHolder) {
            YangLeavesHolder leavesHolder = (YangLeavesHolder) curNode;
            addLeavesInfoToTempFiles(leavesHolder.getListOfLeaf(), curNode);
            addLeafListInfoToTempFiles(leavesHolder.getListOfLeafList(), curNode);
        }
    }

    /**
     * Adds leaf attributes in generated files.
     *
     * @param curNode current data model node
     * @throws IOException IO operation fail
     */
    public void addTypeDefAttributeToTempFiles(YangNode curNode) throws IOException {

        JavaAttributeInfo javaAttributeInfo = getAttributeInfoOfTypeDef(curNode,
                ((YangTypeDef) curNode).getDerivedType().getDataTypeExtendedInfo().getBaseType(),
                ((YangTypeDef) curNode).getName(), false);
        addJavaSnippetInfoToApplicableTempFiles(javaAttributeInfo);
    }

    /**
     * Add the new attribute info to the target generated temporary files.
     *
     * @param newAttrInfo the attribute info that needs to be added to temporary
     *            files
     * @throws IOException IO operation fail
     */
    void addJavaSnippetInfoToApplicableTempFiles(JavaAttributeInfo newAttrInfo)
            throws IOException {

        setNewAttrInfo(newAttrInfo);
        if (isAttributePresent) {
            if ((generatedTempFiles & ATTRIBUTES_MASK) != 0) {
                addAttribute(newAttrInfo);
            }

            if ((generatedTempFiles & GETTER_FOR_INTERFACE_MASK) != 0) {
                addGetterForInterface(newAttrInfo);
            }

            if ((generatedTempFiles & SETTER_FOR_INTERFACE_MASK) != 0) {
                addSetterForInterface(newAttrInfo);
            }

            if ((generatedTempFiles & GETTER_FOR_CLASS_MASK) != 0) {
                addGetterImpl(newAttrInfo, generatedJavaFiles);
            }

            if ((generatedTempFiles & SETTER_FOR_CLASS_MASK) != 0) {
                addSetterImpl(newAttrInfo);
            }

            if ((generatedTempFiles & CONSTRUCTOR_IMPL_MASK) != 0) {
                addConstructor(newAttrInfo);
            }

            if ((generatedTempFiles & HASH_CODE_IMPL_MASK) != 0) {
                addHashCodeMethod(newAttrInfo);
            }

            if ((generatedTempFiles & EQUALS_IMPL_MASK) != 0) {
                addEqualsMethod(newAttrInfo);
            }

            if ((generatedTempFiles & TO_STRING_IMPL_MASK) != 0) {
                addToStringMethod(newAttrInfo);
            }
        }
        return;
    }

    /**
     * Return java file info.
     *
     * @return java file info
     */
    private JavaFileInfo getJavaFileInfo() {

        return ((HasJavaFileInfo) getCurYangNode()).getJavaFileInfo();
    }

    /**
     * Returns java class name.
     *
     * @param suffix for the class name based on the file type
     * @return java class name
     */
    private String getJavaClassName(String suffix) {

        return JavaIdentifierSyntax.getCaptialCase(getJavaFileInfo().getJavaName()) + suffix;
    }

    /**
     * Returns the directory path.
     *
     * @return directory path
     */
    private String getDirPath() {

        return getJavaFileInfo().getPackageFilePath();
    }

    /**
     * Construct java code exit.
     *
     * @param fileType generated file type
     * @param curNode current YANG node
     * @throws IOException when fails to generate java files
     */
    public void generateJavaFile(int fileType, YangNode curNode) throws IOException {

        setCurYangNode(curNode);
        List<String> imports = new ArrayList<>();
        if (curNode instanceof HasJavaImportData && isAttributePresent) {
            imports = ((HasJavaImportData) curNode).getJavaImportData().getImports(getNewAttrInfo());
        }
        /**
         * Start generation of files.
         */
        if ((fileType & INTERFACE_MASK) != 0 | (fileType & BUILDER_INTERFACE_MASK) != 0
                | fileType == GENERATE_INTERFACE_WITH_BUILDER) {

            /**
             * Create interface file.
             */
            setInterfaceJavaFileHandle(getJavaFileHandle(getJavaClassName(INTERFACE_FILE_NAME_SUFFIX)));
            setInterfaceJavaFileHandle(
                    generateInterfaceFile(getInterfaceJavaFileHandle(), imports, curNode, isAttributePresent));
            /**
             * Create builder interface file.
             */
            setBuilderInterfaceJavaFileHandle(getJavaFileHandle(getJavaClassName(BUILDER_INTERFACE_FILE_NAME_SUFFIX)));
            setBuilderInterfaceJavaFileHandle(
                    generateBuilderInterfaceFile(getBuilderInterfaceJavaFileHandle(), curNode, isAttributePresent));
            /**
             * Append builder interface file to interface file and close it.
             */
            mergeJavaFiles(getBuilderInterfaceJavaFileHandle(), getInterfaceJavaFileHandle());
            insertDataIntoJavaFile(getInterfaceJavaFileHandle(), JavaCodeSnippetGen.getJavaClassDefClose());

        }

        if (curNode instanceof HasJavaImportData && isAttributePresent) {
            imports.add(((HasJavaImportData) curNode).getJavaImportData().getImportForHashAndEquals());
            imports.add(((HasJavaImportData) curNode).getJavaImportData().getImportForToString());
            java.util.Collections.sort(imports);
        }

        if ((fileType & BUILDER_CLASS_MASK) != 0 | (fileType & IMPL_CLASS_MASK) != 0
                | fileType == GENERATE_INTERFACE_WITH_BUILDER) {

            /**
             * Create builder class file.
             */
            setBuilderClassJavaFileHandle(getJavaFileHandle(getJavaClassName(BUILDER_CLASS_FILE_NAME_SUFFIX)));
            setBuilderClassJavaFileHandle(
                    generateBuilderClassFile(getBuilderClassJavaFileHandle(), imports, curNode, isAttributePresent));
            /**
             * Create impl class file.
             */
            setImplClassJavaFileHandle(getJavaFileHandle(getJavaClassName(IMPL_CLASS_FILE_NAME_SUFFIX)));
            setImplClassJavaFileHandle(
                    generateImplClassFile(getImplClassJavaFileHandle(), curNode, isAttributePresent));
            /**
             * Append impl class to builder class and close it.
             */
            mergeJavaFiles(getImplClassJavaFileHandle(), getBuilderClassJavaFileHandle());
            insertDataIntoJavaFile(getBuilderClassJavaFileHandle(), JavaCodeSnippetGen.getJavaClassDefClose());

        }

        /**
         * Creates type def class file.
         */
        if ((fileType & GENERATE_TYPEDEF_CLASS) != 0) {
            setTypedefClassJavaFileHandle(getJavaFileHandle(getJavaClassName(TYPEDEF_CLASS_FILE_NAME_SUFFIX)));
            setTypedefClassJavaFileHandle(generateTypeDefClassFile(getTypedefClassJavaFileHandle(), curNode, imports));
        }

        /**
         * Close all the file handles.
         */
        close();
    }

    /**
     * Removes all temporary file handles.
     *
     * @throws IOException when failed to delete the temporary files
     */
    private void close() throws IOException {

        if ((generatedJavaFiles & INTERFACE_MASK) != 0) {
            closeFile(getInterfaceJavaFileHandle(), false);
        }

        if ((generatedJavaFiles & BUILDER_CLASS_MASK) != 0) {
            closeFile(getBuilderClassJavaFileHandle(), false);
        }

        if ((generatedJavaFiles & BUILDER_INTERFACE_MASK) != 0) {
            closeFile(getBuilderInterfaceJavaFileHandle(), true);
        }

        if ((generatedJavaFiles & IMPL_CLASS_MASK) != 0) {
            closeFile(getImplClassJavaFileHandle(), true);
        }

        if ((generatedJavaFiles & GENERATE_TYPEDEF_CLASS) != 0) {
            closeFile(getTypedefClassJavaFileHandle(), false);
        }

        /**
         * Close all temporary file handles and delete the files.
         */
        closeFile(getGetterInterfaceTempFileHandle(), true);
        closeFile(getGetterImplTempFileHandle(), true);
        closeFile(getSetterInterfaceTempFileHandle(), true);
        closeFile(getSetterImplTempFileHandle(), true);
        closeFile(getConstructorImplTempFileHandle(), true);
        closeFile(getAttributesTempFileHandle(), true);
        closeFile(getHashCodeImplTempFileHandle(), true);
        closeFile(getToStringImplTempFileHandle(), true);
        closeFile(getEqualsImplTempFileHandle(), true);
    }

    /**
     * Closes the file handle for temporary file.
     *
     * @param fileName temporary file's name
     * @throws IOException when failed to close the file handle
     */
    private void closeFile(File file, boolean toBeDeleted) throws IOException {

        if (file.exists()) {
            FileSystemUtil.updateFileHandle(file, null, true);
            if (toBeDeleted) {
                file.delete();
            }
            clean(getTempDirPath());
        }
    }
}

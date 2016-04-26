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

package org.onosproject.yangutils.translator.tojava;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.onosproject.yangutils.datamodel.HasType;
import org.onosproject.yangutils.datamodel.YangEnum;
import org.onosproject.yangutils.datamodel.YangEnumeration;
import org.onosproject.yangutils.datamodel.YangLeaf;
import org.onosproject.yangutils.datamodel.YangLeafList;
import org.onosproject.yangutils.datamodel.YangLeavesHolder;
import org.onosproject.yangutils.datamodel.YangNode;
import org.onosproject.yangutils.datamodel.YangType;
import org.onosproject.yangutils.datamodel.YangCase;
import org.onosproject.yangutils.translator.exception.TranslatorException;

import static org.onosproject.yangutils.datamodel.YangNodeType.MODULE_NODE;
import static org.onosproject.yangutils.translator.tojava.GeneratedJavaFileType.BUILDER_CLASS_MASK;
import static org.onosproject.yangutils.translator.tojava.GeneratedJavaFileType.BUILDER_INTERFACE_MASK;
import static org.onosproject.yangutils.translator.tojava.GeneratedJavaFileType.GENERATE_ENUM_CLASS;
import static org.onosproject.yangutils.translator.tojava.GeneratedJavaFileType.GENERATE_RPC_INTERFACE;
import static org.onosproject.yangutils.translator.tojava.GeneratedJavaFileType.GENERATE_TYPEDEF_CLASS;
import static org.onosproject.yangutils.translator.tojava.GeneratedJavaFileType.GENERATE_UNION_CLASS;
import static org.onosproject.yangutils.translator.tojava.GeneratedJavaFileType.IMPL_CLASS_MASK;
import static org.onosproject.yangutils.translator.tojava.GeneratedJavaFileType.INTERFACE_MASK;
import static org.onosproject.yangutils.translator.tojava.GeneratedTempFileType.ATTRIBUTES_MASK;
import static org.onosproject.yangutils.translator.tojava.GeneratedTempFileType.CONSTRUCTOR_FOR_TYPE_MASK;
import static org.onosproject.yangutils.translator.tojava.GeneratedTempFileType.CONSTRUCTOR_IMPL_MASK;
import static org.onosproject.yangutils.translator.tojava.GeneratedTempFileType.ENUM_IMPL_MASK;
import static org.onosproject.yangutils.translator.tojava.GeneratedTempFileType.EQUALS_IMPL_MASK;
import static org.onosproject.yangutils.translator.tojava.GeneratedTempFileType.FROM_STRING_IMPL_MASK;
import static org.onosproject.yangutils.translator.tojava.GeneratedTempFileType.GETTER_FOR_CLASS_MASK;
import static org.onosproject.yangutils.translator.tojava.GeneratedTempFileType.GETTER_FOR_INTERFACE_MASK;
import static org.onosproject.yangutils.translator.tojava.GeneratedTempFileType.HASH_CODE_IMPL_MASK;
import static org.onosproject.yangutils.translator.tojava.GeneratedTempFileType.OF_STRING_IMPL_MASK;
import static org.onosproject.yangutils.translator.tojava.GeneratedTempFileType.RPC_IMPL_MASK;
import static org.onosproject.yangutils.translator.tojava.GeneratedTempFileType.SETTER_FOR_CLASS_MASK;
import static org.onosproject.yangutils.translator.tojava.GeneratedTempFileType.SETTER_FOR_INTERFACE_MASK;
import static org.onosproject.yangutils.translator.tojava.GeneratedTempFileType.TO_STRING_IMPL_MASK;
import static org.onosproject.yangutils.translator.tojava.JavaAttributeInfo.getAttributeInfoOfEnumAttribute;
import static org.onosproject.yangutils.translator.tojava.JavaAttributeInfo.getAttributeInfoOfLeaf;
import static org.onosproject.yangutils.translator.tojava.JavaAttributeInfo.getAttributeInfoOfType;
import static org.onosproject.yangutils.translator.tojava.JavaAttributeInfo.getCurNodeAsAttributeInParent;
import static org.onosproject.yangutils.translator.tojava.JavaAttributeInfo.getFromStringAttributeInfo;
import static org.onosproject.yangutils.translator.tojava.utils.JavaCodeSnippetGen.generateEnumAttributeString;
import static org.onosproject.yangutils.translator.tojava.utils.JavaCodeSnippetGen.getJavaAttributeDefination;
import static org.onosproject.yangutils.translator.tojava.utils.JavaCodeSnippetGen.getJavaClassDefClose;
import static org.onosproject.yangutils.translator.tojava.utils.JavaFileGenerator.generateBuilderClassFile;
import static org.onosproject.yangutils.translator.tojava.utils.JavaFileGenerator.generateBuilderInterfaceFile;
import static org.onosproject.yangutils.translator.tojava.utils.JavaFileGenerator.generateEnumClassFile;
import static org.onosproject.yangutils.translator.tojava.utils.JavaFileGenerator.generateImplClassFile;
import static org.onosproject.yangutils.translator.tojava.utils.JavaFileGenerator.generateInterfaceFile;
import static org.onosproject.yangutils.translator.tojava.utils.JavaFileGenerator.generateRpcInterfaceFile;
import static org.onosproject.yangutils.translator.tojava.utils.JavaFileGenerator.generateTypeDefClassFile;
import static org.onosproject.yangutils.translator.tojava.utils.JavaFileGenerator.generateUnionClassFile;
import static org.onosproject.yangutils.translator.tojava.utils.JavaFileGeneratorUtils.getFileObject;
import static org.onosproject.yangutils.translator.tojava.utils.JavaIdentifierSyntax.getCamelCase;
import static org.onosproject.yangutils.translator.tojava.utils.JavaIdentifierSyntax.getCaptialCase;
import static org.onosproject.yangutils.translator.tojava.utils.JavaIdentifierSyntax.getPackageDirPathFromJavaJPackage;
import static org.onosproject.yangutils.translator.tojava.utils.JavaIdentifierSyntax.getParentNodeInGenCode;
import static org.onosproject.yangutils.translator.tojava.utils.JavaIdentifierSyntax.getSmallCase;
import static org.onosproject.yangutils.translator.tojava.utils.MethodsGenerator.getBuildString;
import static org.onosproject.yangutils.translator.tojava.utils.MethodsGenerator.getConstructor;
import static org.onosproject.yangutils.translator.tojava.utils.MethodsGenerator.getDefaultConstructorString;
import static org.onosproject.yangutils.translator.tojava.utils.MethodsGenerator.getEqualsMethod;
import static org.onosproject.yangutils.translator.tojava.utils.MethodsGenerator.getFromStringMethod;
import static org.onosproject.yangutils.translator.tojava.utils.MethodsGenerator.getGetterForClass;
import static org.onosproject.yangutils.translator.tojava.utils.MethodsGenerator.getGetterString;
import static org.onosproject.yangutils.translator.tojava.utils.MethodsGenerator.getHashCodeMethod;
import static org.onosproject.yangutils.translator.tojava.utils.MethodsGenerator.getOfMethod;
import static org.onosproject.yangutils.translator.tojava.utils.MethodsGenerator.getOfMethodStringAndJavaDoc;
import static org.onosproject.yangutils.translator.tojava.utils.MethodsGenerator.getOverRideString;
import static org.onosproject.yangutils.translator.tojava.utils.MethodsGenerator.getRpcStringMethod;
import static org.onosproject.yangutils.translator.tojava.utils.MethodsGenerator.getSetterForClass;
import static org.onosproject.yangutils.translator.tojava.utils.MethodsGenerator.getSetterString;
import static org.onosproject.yangutils.translator.tojava.utils.MethodsGenerator.getToStringMethod;
import static org.onosproject.yangutils.translator.tojava.utils.MethodsGenerator.getTypeConstructorStringAndJavaDoc;
import static org.onosproject.yangutils.translator.tojava.utils.MethodsGenerator.parseBuilderInterfaceBuildMethodString;
import static org.onosproject.yangutils.translator.tojava.utils.TempJavaCodeFragmentFilesUtils.addArrayListImport;
import static org.onosproject.yangutils.translator.tojava.utils.TempJavaCodeFragmentFilesUtils.addAugmentedInfoImport;
import static org.onosproject.yangutils.translator.tojava.utils.TempJavaCodeFragmentFilesUtils.addHasAugmentationImport;
import static org.onosproject.yangutils.translator.tojava.utils.TempJavaCodeFragmentFilesUtils.addImportsToStringAndHasCodeMethods;
import static org.onosproject.yangutils.translator.tojava.utils.TempJavaCodeFragmentFilesUtils.closeFile;
import static org.onosproject.yangutils.translator.tojava.utils.TempJavaCodeFragmentFilesUtils.isAugmentedInfoExtended;
import static org.onosproject.yangutils.translator.tojava.utils.TempJavaCodeFragmentFilesUtils.isHasAugmentationExtended;
import static org.onosproject.yangutils.translator.tojava.utils.TempJavaCodeFragmentFilesUtils.prepareJavaFileGeneratorForExtendsList;
import static org.onosproject.yangutils.utils.UtilConstants.BUILDER;
import static org.onosproject.yangutils.utils.UtilConstants.EMPTY_STRING;
import static org.onosproject.yangutils.utils.UtilConstants.FOUR_SPACE_INDENTATION;
import static org.onosproject.yangutils.utils.UtilConstants.IMPL;
import static org.onosproject.yangutils.utils.UtilConstants.INTERFACE;
import static org.onosproject.yangutils.utils.UtilConstants.NEW_LINE;
import static org.onosproject.yangutils.utils.UtilConstants.PACKAGE_INFO_JAVADOC_OF_CHILD;
import static org.onosproject.yangutils.utils.UtilConstants.PERIOD;
import static org.onosproject.yangutils.utils.UtilConstants.SLASH;
import static org.onosproject.yangutils.utils.UtilConstants.IMPORT;
import static org.onosproject.yangutils.utils.UtilConstants.SEMI_COLAN;
import static org.onosproject.yangutils.utils.io.impl.FileSystemUtil.createPackage;
import static org.onosproject.yangutils.utils.io.impl.FileSystemUtil.readAppendFile;
import static org.onosproject.yangutils.utils.io.impl.JavaDocGen.JavaDocType.GETTER_METHOD;
import static org.onosproject.yangutils.utils.io.impl.JavaDocGen.JavaDocType.OF_METHOD;
import static org.onosproject.yangutils.utils.io.impl.JavaDocGen.generateJavaDocForRpc;
import static org.onosproject.yangutils.utils.io.impl.JavaDocGen.getJavaDoc;
import static org.onosproject.yangutils.utils.io.impl.YangIoUtils.clean;
import static org.onosproject.yangutils.utils.io.impl.YangIoUtils.insertDataIntoJavaFile;
import static org.onosproject.yangutils.utils.io.impl.YangIoUtils.mergeJavaFiles;

/**
 * Represents implementation of java code fragments temporary implementations.
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
     * Contains all the class name which will be extended by generated files.
     */
    private List<String> extendsList = new ArrayList<>();

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
     * File name for of string method.
     */
    private static final String OF_STRING_METHOD_FILE_NAME = "OfString";

    /**
     * File name for temporary enum class.
     */
    private static final String ENUM_CLASS_TEMP_FILE_NAME = "EnumClass";

    /**
     * File name for construction for special type like union, typedef.
     */
    private static final String CONSTRUCTOR_FOR_TYPE_FILE_NAME = "ConstructorForType";

    /**
     * File name for from string method.
     */
    private static final String FROM_STRING_METHOD_FILE_NAME = "FromString";

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
     * File name for enum class file name suffix.
     */
    private static final String ENUM_CLASS_FILE_NAME_SUFFIX = EMPTY_STRING;

    /**
     * File name for rpc method.
     */
    private static final String RPC_FILE_NAME = "Rpc";

    /**
     * File name for generated class file for special type like union, typedef suffix.
     */
    private static final String RPC_INTERFACE_FILE_NAME_SUFFIX = "Service";

    /**
     * File name for generated class file for special type like union, typedef suffix.
     */
    private static final String UNION_TYPE_CLASS_FILE_NAME_SUFFIX = EMPTY_STRING;

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
     * Java file handle for type class like union, typedef file.
     */
    private File typeClassJavaFileHandle;

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
     * Temporary file handle for enum class file.
     */
    private File enumClassTempFileHandle;

    /**
     * Temporary file handle for of string method of class.
     */
    private File ofStringImplTempFileHandle;

    /**
     * Temporary file handle for constructor for type class.
     */
    private File constructorForTypeTempFileHandle;

    /**
     * Temporary file handle for from string method of class.
     */
    private File fromStringImplTempFileHandle;

    /**
     * Temporary file handle for rpc interface.
     */
    private File rpcInterfaceImplTempFileHandle;

    /**
     * Java file handle for rpc interface file.
     */
    private File rpcInterfaceJavaFileHandle;

    /**
     * Java attribute info.
     */
    private JavaAttributeInfo newAttrInfo;

    /**
     * Current YANG node.
     */
    private YangNode curYangNode;

    /**
     * Import info for case.
     */
    private JavaQualifiedTypeInfo caseImportInfo;

    /**
     * Current enum's value.
     */
    private int enumValue;

    /**
     * Is attribute added.
     */
    private boolean isAttributePresent = false;

    /*
     * Java file handle for enum class.
      */
    private File enumClassJavaFileHandle;

    /**
     * Creates an instance of temporary java code fragment.
     *
     * @param genFileType file generation type
     * @param genDir      file generation directory
     * @param className   class name
     * @throws IOException when fails to create new file handle
     */
    public TempJavaCodeFragmentFiles(int genFileType, String genDir, String className)
            throws IOException {

        setExtendsList(new ArrayList<>());
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

        if ((genFileType & GENERATE_RPC_INTERFACE) != 0) {
            generatedTempFiles |= RPC_IMPL_MASK;
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
            generatedTempFiles |= OF_STRING_IMPL_MASK;
            generatedTempFiles |= CONSTRUCTOR_FOR_TYPE_MASK;
            generatedTempFiles |= FROM_STRING_IMPL_MASK;
        }

        /**
         * Initialize getterImpl, attributes, hash code, equals, of string,
         * constructor, union's to string, from string when generation
         * file type matches to union class mask.
         */
        if ((genFileType & GENERATE_UNION_CLASS) != 0) {
            generatedTempFiles |= ATTRIBUTES_MASK;
            generatedTempFiles |= GETTER_FOR_CLASS_MASK;
            generatedTempFiles |= HASH_CODE_IMPL_MASK;
            generatedTempFiles |= EQUALS_IMPL_MASK;
            generatedTempFiles |= OF_STRING_IMPL_MASK;
            generatedTempFiles |= CONSTRUCTOR_FOR_TYPE_MASK;
            generatedTempFiles |= TO_STRING_IMPL_MASK;
            generatedTempFiles |= FROM_STRING_IMPL_MASK;
        }
        /**
         * Initialize enum when generation file type matches to enum class mask.
         */
        if ((genFileType & GENERATE_ENUM_CLASS) != 0) {
            generatedTempFiles |= ENUM_IMPL_MASK;
        }

        /**
         * Set temporary file handles.
         */
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
        if ((generatedTempFiles & ENUM_IMPL_MASK) != 0) {
            setEnumClassTempFileHandle(getTemporaryFileHandle(ENUM_CLASS_TEMP_FILE_NAME));
        }
        if ((generatedTempFiles & OF_STRING_IMPL_MASK) != 0) {
            setOfStringImplTempFileHandle(getTemporaryFileHandle(OF_STRING_METHOD_FILE_NAME));
        }

        if ((generatedTempFiles & CONSTRUCTOR_FOR_TYPE_MASK) != 0) {
            setConstructorForTypeTempFileHandle(getTemporaryFileHandle(CONSTRUCTOR_FOR_TYPE_FILE_NAME));
        }

        if ((generatedTempFiles & FROM_STRING_IMPL_MASK) != 0) {
            setFromStringImplTempFileHandle(getTemporaryFileHandle(FROM_STRING_METHOD_FILE_NAME));
        }

        if ((generatedTempFiles & RPC_IMPL_MASK) != 0) {
            setRpcInterfaceImplTempFileHandle(getTemporaryFileHandle(RPC_FILE_NAME));
        }
    }

    /**
     * Returns java file handle for interface file.
     *
     * @return java file handle for interface file
     */
    private File getInterfaceJavaFileHandle() {
        return interfaceJavaFileHandle;
    }

    /**
     * Sets the java file handle for interface file.
     *
     * @param interfaceJavaFileHandle java file handle
     */
    private void setInterfaceJavaFileHandle(File interfaceJavaFileHandle) {
        this.interfaceJavaFileHandle = interfaceJavaFileHandle;
    }

    /**
     * Returns java file handle for builder interface file.
     *
     * @return java file handle for builder interface file
     */
    private File getBuilderInterfaceJavaFileHandle() {
        return builderInterfaceJavaFileHandle;
    }

    /**
     * Sets the java file handle for builder interface file.
     *
     * @param builderInterfaceJavaFileHandle java file handle
     */
    private void setBuilderInterfaceJavaFileHandle(File builderInterfaceJavaFileHandle) {
        this.builderInterfaceJavaFileHandle = builderInterfaceJavaFileHandle;
    }

    /**
     * Returns java file handle for builder class file.
     *
     * @return java file handle for builder class file
     */
    private File getBuilderClassJavaFileHandle() {
        return builderClassJavaFileHandle;
    }

    /**
     * Sets the java file handle for builder class file.
     *
     * @param builderClassJavaFileHandle java file handle
     */
    private void setBuilderClassJavaFileHandle(File builderClassJavaFileHandle) {
        this.builderClassJavaFileHandle = builderClassJavaFileHandle;
    }

    /**
     * Returns java file handle for impl class file.
     *
     * @return java file handle for impl class file
     */
    private File getImplClassJavaFileHandle() {
        return implClassJavaFileHandle;
    }

    /**
     * Sets the java file handle for impl class file.
     *
     * @param implClassJavaFileHandle java file handle
     */
    private void setImplClassJavaFileHandle(File implClassJavaFileHandle) {
        this.implClassJavaFileHandle = implClassJavaFileHandle;
    }

    /**
     * Returns java file handle for typedef class file.
     *
     * @return java file handle for typedef class file
     */
    private File getTypedefClassJavaFileHandle() {
        return typedefClassJavaFileHandle;
    }

    /**
     * Sets the java file handle for typedef class file.
     *
     * @param typedefClassJavaFileHandle java file handle
     */
    private void setTypedefClassJavaFileHandle(File typedefClassJavaFileHandle) {
        this.typedefClassJavaFileHandle = typedefClassJavaFileHandle;
    }

    /**
     * Returns enum class java file handle.
     *
     * @return enum class java file handle
     */
    private File getEnumClassJavaFileHandle() {
        return enumClassJavaFileHandle;
    }

    /**
     * Sets enum class java file handle.
     *
     * @param enumClassJavaFileHandle enum class java file handle
     */

    private void setEnumClassJavaFileHandle(File enumClassJavaFileHandle) {
        this.enumClassJavaFileHandle = enumClassJavaFileHandle;
    }

    /**
     * Returns java file handle for type class file.
     *
     * @return java file handle for type class file
     */
    private File getTypeClassJavaFileHandle() {
        return typeClassJavaFileHandle;
    }

    /**
     * Sets the java file handle for type class file.
     *
     * @param typeClassJavaFileHandle type file handle
     */
    private void setTypeClassJavaFileHandle(File typeClassJavaFileHandle) {
        this.typeClassJavaFileHandle = typeClassJavaFileHandle;
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
    private void setAttributesTempFileHandle(File attributeForClass) {
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
    private void setGetterInterfaceTempFileHandle(File getterForInterface) {
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
    private void setGetterImplTempFileHandle(File getterImpl) {
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
    private void setSetterInterfaceTempFileHandle(File setterForInterface) {
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
    private void setSetterImplTempFileHandle(File setterImpl) {
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
    private void setConstructorImplTempFileHandle(File constructor) {
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
    private void setHashCodeImplTempFileHandle(File hashCodeMethod) {
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
    private void setEqualsImplTempFileHandle(File equalsMethod) {
        equalsImplTempFileHandle = equalsMethod;
    }

    /**
     * Returns rpc method's temporary file handle.
     *
     * @return temporary file handle
     */
    public File getRpcInterfaceImplTempFileHandle() {
        return rpcInterfaceImplTempFileHandle;
    }

    /**
     * Sets rpc method's temporary file handle.
     *
     * @param rpcInterfaceImplTempFileHandle file handle for to rpc method
     */
    public void setRpcInterfaceImplTempFileHandle(File rpcInterfaceImplTempFileHandle) {
        this.rpcInterfaceImplTempFileHandle = rpcInterfaceImplTempFileHandle;
    }

    /**
     * Returns rpc method's java file handle.
     *
     * @return java file handle
     */
    public File getRpcInterfaceJavaFileHandle() {
        return rpcInterfaceJavaFileHandle;
    }

    /**
     * Sets rpc method's java file handle.
     *
     * @param rpcInterfaceJavaFileHandle file handle for to rpc method
     */
    public void setRpcInterfaceJavaFileHandle(File rpcInterfaceJavaFileHandle) {
        this.rpcInterfaceJavaFileHandle = rpcInterfaceJavaFileHandle;
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
    private void setToStringImplTempFileHandle(File toStringMethod) {
        toStringImplTempFileHandle = toStringMethod;
    }

    /**
     * Returns temporary file handle for enum class file.
     *
     * @return temporary file handle for enum class file
     */
    public File getEnumClassTempFileHandle() {
        return enumClassTempFileHandle;
    }

    /**
     * Sets temporary file handle for enum class file.
     *
     * @param enumClassTempFileHandle temporary file handle for enum class file
     */

    private void setEnumClassTempFileHandle(File enumClassTempFileHandle) {
        this.enumClassTempFileHandle = enumClassTempFileHandle;
    }

    /**
     * Returns of string method's temporary file handle.
     *
     * @return of string method's temporary file handle
     */
    public File getOfStringImplTempFileHandle() {
        return ofStringImplTempFileHandle;
    }

    /**
     * Set of string method's temporary file handle.
     *
     * @param ofStringImplTempFileHandle of string method's temporary file handle
     */
    private void setOfStringImplTempFileHandle(File ofStringImplTempFileHandle) {
        this.ofStringImplTempFileHandle = ofStringImplTempFileHandle;
    }

    /**
     * Returns type class constructor method's temporary file handle.
     *
     * @return type class constructor method's temporary file handle
     */
    public File getConstructorForTypeTempFileHandle() {
        return constructorForTypeTempFileHandle;
    }

    /**
     * Sets type class constructor method's temporary file handle.
     *
     * @param constructorForTypeTempFileHandle type class constructor method's temporary file handle
     */
    private void setConstructorForTypeTempFileHandle(File constructorForTypeTempFileHandle) {
        this.constructorForTypeTempFileHandle = constructorForTypeTempFileHandle;
    }

    /**
     * Returns from string method's temporary file handle.
     *
     * @return from string method's temporary file handle
     */
    public File getFromStringImplTempFileHandle() {
        return fromStringImplTempFileHandle;
    }

    /**
     * Sets from string method's temporary file handle.
     *
     * @param fromStringImplTempFileHandle from string method's temporary file handle
     */
    private void setFromStringImplTempFileHandle(File fromStringImplTempFileHandle) {
        this.fromStringImplTempFileHandle = fromStringImplTempFileHandle;
    }

    /**
     * Returns java attribute info.
     *
     * @return java attribute info
     */
    private JavaAttributeInfo getNewAttrInfo() {
        return newAttrInfo;
    }

    /**
     * Sets java attribute info.
     *
     * @param newAttrInfo java attribute info
     */
    private void setNewAttrInfo(JavaAttributeInfo newAttrInfo) {

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
    private YangNode getCurYangNode() {
        return curYangNode;
    }

    /**
     * Sets current YANG node.
     *
     * @param curYangNode YANG node
     */
    private void setCurYangNode(YangNode curYangNode) {
        this.curYangNode = curYangNode;
    }

    /**
     * Returns enum's value.
     *
     * @return enum's value
     */
    public int getEnumValue() {
        return enumValue;
    }

    /**
     * Sets enum's value.
     *
     * @param enumValue enum's value
     */
    public void setEnumValue(int enumValue) {
        this.enumValue = enumValue;
    }

    /**
     * Returns list of classes to be extended by generated files.
     *
     * @return list of classes to be extended by generated files
     */
    private List<String> getExtendsList() {
        return extendsList;
    }

    /**
     * Sets class to be extended by generated file.
     *
     * @param extendsList list of classes to be extended
     */

    private void setExtendsList(List<String> extendsList) {
        this.extendsList = extendsList;
    }

    /**
     * Adds class to the extends list.
     *
     * @param extend class to be extended
     */
    public void addToExtendsList(String extend) {
        getExtendsList().add(extend);
    }

    /**
     * Adds of string for type.
     *
     * @param attr attribute info
     * @throws IOException when fails to append to temporary file
     */
    private void addOfStringMethod(JavaAttributeInfo attr) throws IOException {
        appendToFile(getOfStringImplTempFileHandle(), getOfMethodStringAndJavaDoc(attr, generatedJavaClassName)
                + NEW_LINE);
    }

    /**
     * Adds type constructor.
     *
     * @param attr attribute info
     * @throws IOException when fails to append to temporary file
     */
    private void addTypeConstructor(JavaAttributeInfo attr) throws IOException {
        appendToFile(getConstructorForTypeTempFileHandle(), getTypeConstructorStringAndJavaDoc(attr,
                generatedJavaClassName) + NEW_LINE);
    }

    /**
     * Adds attribute for class.
     *
     * @param attr attribute info
     * @throws IOException when fails to append to temporary file
     */
    private void addAttribute(JavaAttributeInfo attr) throws IOException {
        appendToFile(getAttributesTempFileHandle(), parseAttribute(attr) + FOUR_SPACE_INDENTATION);
    }

    /**
     * Adds getter for interface.
     *
     * @param attr attribute info
     * @throws IOException when fails to append to temporary file
     */
    private void addGetterForInterface(JavaAttributeInfo attr) throws IOException {
        appendToFile(getGetterInterfaceTempFileHandle(), getGetterString(attr) + NEW_LINE);
    }

    /**
     * Adds getter method's impl for class.
     *
     * @param attr        attribute info
     * @param genFiletype generated file type
     * @throws IOException when fails to append to temporary file
     */
    private void addGetterImpl(JavaAttributeInfo attr, int genFiletype) throws IOException {

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
    private void addSetterForInterface(JavaAttributeInfo attr) throws IOException {
        appendToFile(getSetterInterfaceTempFileHandle(),
                getSetterString(attr, generatedJavaClassName) + NEW_LINE);
    }

    /**
     * Adds setter's implementation for class.
     *
     * @param attr attribute info
     * @throws IOException when fails to append to temporary file
     */
    private void addSetterImpl(JavaAttributeInfo attr) throws IOException {
        appendToFile(getSetterImplTempFileHandle(),
                getOverRideString() + getSetterForClass(attr, generatedJavaClassName) + NEW_LINE);
    }

    /**
     * Adds build method for interface.
     *
     * @return build method for interface
     */
    public String addBuildMethodForInterface() {
        return parseBuilderInterfaceBuildMethodString(generatedJavaClassName);
    }

    /**
     * Adds build method's implementation for class.
     *
     * @return build method implementation for class
     */
    public String addBuildMethodImpl() {
        return getBuildString(generatedJavaClassName) + NEW_LINE;
    }

    /**
     * Adds constructor for class.
     *
     * @param attr attribute info
     * @throws IOException when fails to append to temporary file
     */
    private void addConstructor(JavaAttributeInfo attr) throws IOException {
        appendToFile(getConstructorImplTempFileHandle(), getConstructor(generatedJavaClassName, attr));
    }

    /**
     * Adds default constructor for class.
     *
     * @param modifier modifier for constructor.
     * @param toAppend string which need to be appended with the class name
     * @return default constructor for class
     */
    public String addDefaultConstructor(String modifier, String toAppend) {
        return NEW_LINE + getDefaultConstructorString(generatedJavaClassName + toAppend, modifier);
    }

    /**
     * Adds default constructor for class.
     *
     * @return default constructor for class
     */
    public String addOfMethod() {
        return getJavaDoc(OF_METHOD, generatedJavaClassName, false)
                + getOfMethod(generatedJavaClassName, newAttrInfo);
    }

    /**
     * Adds hash code method for class.
     *
     * @param attr attribute info
     * @throws IOException when fails to append to temporary file
     */
    private void addHashCodeMethod(JavaAttributeInfo attr) throws IOException {
        appendToFile(getHashCodeImplTempFileHandle(), getHashCodeMethod(attr) + NEW_LINE);
    }

    /**
     * Adds equals method for class.
     *
     * @param attr attribute info
     * @throws IOException when fails to append to temporary file
     */
    private void addEqualsMethod(JavaAttributeInfo attr) throws IOException {
        appendToFile(getEqualsImplTempFileHandle(), getEqualsMethod(attr) + NEW_LINE);
    }

    /**
     * Adds ToString method for class.
     *
     * @param attr attribute info
     * @throws IOException when fails to append to temporary file
     */
    private void addToStringMethod(JavaAttributeInfo attr) throws IOException {
        appendToFile(getToStringImplTempFileHandle(), getToStringMethod(attr) + NEW_LINE);
    }

    /**
     * Adds enum class attributes to temporary file.
     *
     * @param curEnum current YANG enum
     * @throws IOException when fails to do IO operations.
     */
    private void addAttributesForEnumClass(JavaAttributeInfo curEnumInfo) throws IOException {
        appendToFile(getEnumClassTempFileHandle(),
                generateEnumAttributeString(curEnumInfo.getAttributeName(), getEnumValue()));
    }

    /**
     * Add from string method for union class.
     *
     * @param javaAttributeInfo       type attribute info
     * @param fromStringAttributeInfo from string attribute info
     * @throws IOException when fails to append to temporary file
     */
    private void addFromStringMethod(JavaAttributeInfo javaAttributeInfo,
                                     JavaAttributeInfo fromStringAttributeInfo) throws IOException {
        appendToFile(getFromStringImplTempFileHandle(), getFromStringMethod(javaAttributeInfo,
                fromStringAttributeInfo) + NEW_LINE);
    }

    /**
     * Adds rpc string information to applicable temp file.
     *
     * @param javaAttributeInfoOfInput  rpc's input node attribute info
     * @param javaAttributeInfoOfOutput rpc's output node attribute info
     * @param rpcName                   name of the rpc function
     * @throws IOException IO operation fail
     */
    private void addRpcString(JavaAttributeInfo javaAttributeInfoOfInput, JavaAttributeInfo javaAttributeInfoOfOutput,
                              String rpcName) throws IOException {
        String rpcInput = "";
        String rpcOutput = "void";
        if (javaAttributeInfoOfInput != null) {
            rpcInput = javaAttributeInfoOfInput.getAttributeName();
        }
        if (javaAttributeInfoOfOutput != null) {
            rpcOutput = javaAttributeInfoOfOutput.getAttributeName();
        }
        appendToFile(getRpcInterfaceImplTempFileHandle(), generateJavaDocForRpc(rpcName, rpcInput, rpcOutput) +
                getRpcStringMethod(rpcName, rpcInput, rpcOutput) + NEW_LINE);
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
            return readAppendFile(path + file.getName(), EMPTY_STRING);
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
        return getPackageDirPathFromJavaJPackage(absoluteDirPath) + SLASH + generatedJavaClassName
                + TEMP_FOLDER_NAME_SUFIX + SLASH;
    }

    /**
     * Parses attribute to get the attribute string.
     *
     * @param attr attribute info
     * @return attribute string
     */
    private String parseAttribute(JavaAttributeInfo attr) {

        /*
         * TODO: check if this utility needs to be called or move to the caller
         */
        String attributeName = getCamelCase(getSmallCase(attr.getAttributeName()), null);
        if (attr.isQualifiedName()) {
            return getJavaAttributeDefination(attr.getImportInfo().getPkgInfo(), attr.getImportInfo().getClassInfo(),
                    attributeName, attr.isListAttr());
        } else {
            return getJavaAttributeDefination(null, attr.getImportInfo().getClassInfo(), attributeName,
                    attr.isListAttr());
        }
    }

    /**
     * Appends content to temporary file.
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
     *                the parent generated code
     * @param isList  is list construct
     * @throws IOException IO operation exception
     */
    public void addCurNodeInfoInParentTempFile(YangNode curNode,
                                               boolean isList) throws IOException {

        YangNode parent = getParentNodeInGenCode(curNode);
        if (!(parent instanceof JavaCodeGenerator)) {
            throw new TranslatorException("missing parent node to contain current node info in generated file");
        }
        JavaAttributeInfo javaAttributeInfo = getCurNodeAsAttributeInParent(curNode,
                parent, isList);

        if (!(parent instanceof HasTempJavaCodeFragmentFiles)) {
            throw new TranslatorException("missing parent temp file handle");
        }
        ((HasTempJavaCodeFragmentFiles) parent)
                .getTempJavaCodeFragmentFiles()
                .addJavaSnippetInfoToApplicableTempFiles(javaAttributeInfo);
    }

    /**
     * Adds parent's info to current node import list.
     *
     * @param curNode current node for which import list needs to be updated
     */
    public void addParentInfoInCurNodeTempFile(YangNode curNode) {
        caseImportInfo = new JavaQualifiedTypeInfo();
        YangNode parent = getParentNodeInGenCode(curNode);
        if (!(parent instanceof JavaCodeGenerator)) {
            throw new TranslatorException("missing parent node to contain current node info in generated file");
        }

        if (!(curNode instanceof HasJavaFileInfo)) {
            throw new TranslatorException("missing java file information to get the package details "
                    + "of attribute corresponding to child node");
        }

        caseImportInfo.setClassInfo(getCaptialCase(getCamelCase(parent.getName(), null)));
        caseImportInfo.setPkgInfo(((HasJavaFileInfo) parent).getJavaFileInfo().getPackage());
        ((HasJavaImportData) curNode).getJavaImportData().addImportInfo(curNode, caseImportInfo);
    }

    /**
     * Adds leaf attributes in generated files.
     *
     * @param listOfLeaves list of YANG leaf
     * @param curNode      current data model node
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
     * @param curNode        cached file handle
     * @throws IOException IO operation fail
     */
    private void addLeafListInfoToTempFiles(List<YangLeafList> listOfLeafList,
                                            YangNode curNode) throws IOException {

        if (listOfLeafList != null) {

            /*
             * Check if the attribute is of type list, then the java.lang.list
             * needs to be imported.
             */
            if (!listOfLeafList.isEmpty()) {
                if (!(curNode instanceof HasJavaImportData)) {
                    throw new TranslatorException("missing import info in current data model node");

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
     * Adds all the leaves in the current data model node as part of the
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
     * Add all the type in the current data model node as part of the
     * generated temporary file.
     *
     * @param hasType YANG java data model node which has type info, eg union / typedef
     * @throws IOException IO operation fail
     */
    public void addTypeInfoToTempFiles(HasType hasType) throws IOException {

        List<YangType<?>> typeList = hasType.getTypeList();
        if (typeList != null) {
            for (YangType<?> yangType : typeList) {
                JavaAttributeInfo javaAttributeInfo = getAttributeInfoOfType((YangNode) hasType,
                        yangType, getTypeName(yangType.getDataTypeName()), false);
                addJavaSnippetInfoToApplicableTempFiles((YangNode) hasType, javaAttributeInfo);
            }
        }
    }

    private String getTypeName(String dataTypeName) {
        return dataTypeName;
        //TODO: implement the method.
    }

    /**
     * Adds enum attributes to temporary files.
     *
     * @param curNode current YANG node
     * @throws IOException when fails to do IO operations
     */
    public void addEnumAttributeToTempFiles(YangNode curNode) throws IOException {

        if (curNode instanceof YangEnumeration) {
            Set<YangEnum> enumSet = ((YangEnumeration) curNode).getEnumSet();
            for (YangEnum curEnum : enumSet) {
                JavaAttributeInfo javaAttributeInfo = getAttributeInfoOfEnumAttribute(curNode, curEnum.getNamedValue());
                setEnumValue(curEnum.getValue());
                addJavaSnippetInfoToApplicableTempFiles(javaAttributeInfo);
            }
        } else {
            throw new TranslatorException("curnode should be of type enum.");
        }
    }

    /**
     * Adds the new attribute info to the target generated temporary files for union class.
     *
     * @param hasType           the node for which the type is being added as an attribute
     * @param javaAttributeInfo the attribute info that needs to be added to temporary
     *                          files
     * @throws IOException IO operation fail
     */
    private void addJavaSnippetInfoToApplicableTempFiles(YangNode hasType, JavaAttributeInfo javaAttributeInfo)
            throws IOException {

        JavaAttributeInfo fromStringAttributeInfo = getFromStringAttributeInfo(hasType, javaAttributeInfo);

        if ((generatedTempFiles & FROM_STRING_IMPL_MASK) != 0) {
            addFromStringMethod(javaAttributeInfo, fromStringAttributeInfo);
        }
        addJavaSnippetInfoToApplicableTempFiles(javaAttributeInfo);
    }

    /**
     * Adds the JAVA rpc snippet information.
     *
     * @param javaAttributeInfoOfInput  rpc's input node attribute info
     * @param javaAttributeInfoOfOutput rpc's output node attribute info
     * @param rpcName                   name of the rpc function
     * @throws IOException IO operation fail
     */
    public void addJavaSnippetInfoToApplicableTempFiles(JavaAttributeInfo javaAttributeInfoOfInput,
                                                        JavaAttributeInfo javaAttributeInfoOfOutput,
                                                        String rpcName) throws IOException {
        if ((generatedTempFiles & RPC_IMPL_MASK) != 0) {
            addRpcString(javaAttributeInfoOfInput, javaAttributeInfoOfOutput, rpcName);
        }
    }

    /**
     * Adds the new attribute info to the target generated temporary files.
     *
     * @param newAttrInfo the attribute info that needs to be added to temporary
     *                    files
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

            if ((generatedTempFiles & ENUM_IMPL_MASK) != 0) {
                addAttributesForEnumClass(newAttrInfo);
            }

            if ((generatedTempFiles & OF_STRING_IMPL_MASK) != 0) {
                addOfStringMethod(newAttrInfo);
            }

            if ((generatedTempFiles & CONSTRUCTOR_FOR_TYPE_MASK) != 0) {
                addTypeConstructor(newAttrInfo);
            }

        }
    }

    /**
     * Returns java file info.
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
        return getCaptialCase(getJavaFileInfo().getJavaName()) + suffix;
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
     * Constructs java code exit.
     *
     * @param fileType generated file type
     * @param curNode  current YANG node
     * @throws IOException when fails to generate java files
     */
    public void generateJavaFile(int fileType, YangNode curNode) throws IOException {

        setCurYangNode(curNode);
        List<String> imports = new ArrayList<>();
        if (curNode instanceof HasJavaImportData && isAttributePresent) {
            imports = ((HasJavaImportData) curNode).getJavaImportData().getImports(getNewAttrInfo());
        }

        /**
         * Prepares java file generator for extends list.
         */
        prepareJavaFileGeneratorForExtendsList(getExtendsList());
        if (curNode.getNodeType().equals(MODULE_NODE)) {
            createPackage(absoluteDirPath, getJavaFileInfo().getJavaName());
        } else {
            createPackage(absoluteDirPath, ((HasJavaFileInfo) curNode.getParent()).getJavaFileInfo().getJavaName()
                    + PACKAGE_INFO_JAVADOC_OF_CHILD);
        }
        /**
         * Generate java code.
         */
        if ((fileType & INTERFACE_MASK) != 0 | (fileType & BUILDER_INTERFACE_MASK) != 0) {

            /**
             * Adds import for case.
             */
            if (curNode instanceof YangCase) {
                List<String> importData = ((HasJavaImportData) curNode).getJavaImportData().getImports();
                for (String  importInfo : importData) {
                    if (!imports.contains(importInfo)) {
                        imports.add(importInfo);
                    }
                }
            }

            /**
             * Adds import for HasAugmentation class.
             */
            if (isHasAugmentationExtended(getExtendsList())) {
                addHasAugmentationImport(curNode, imports, true);
            }

            if (isAugmentedInfoExtended(getExtendsList())) {
                addAugmentedInfoImport(curNode, imports, true);
            }

            /**
             * Create interface file.
             */
            setInterfaceJavaFileHandle(getJavaFileHandle(getJavaClassName(INTERFACE_FILE_NAME_SUFFIX)));
            setInterfaceJavaFileHandle(
                    generateInterfaceFile(getInterfaceJavaFileHandle(), imports, curNode, isAttributePresent));
            /**
             * Create builder interface file.
             */
            if ((fileType & BUILDER_INTERFACE_MASK) != 0) {

                setBuilderInterfaceJavaFileHandle(
                        getJavaFileHandle(getJavaClassName(BUILDER_INTERFACE_FILE_NAME_SUFFIX)));
                setBuilderInterfaceJavaFileHandle(
                        generateBuilderInterfaceFile(getBuilderInterfaceJavaFileHandle(), curNode, isAttributePresent));
                /**
                 * Append builder interface file to interface file and close it.
                 */
                mergeJavaFiles(getBuilderInterfaceJavaFileHandle(), getInterfaceJavaFileHandle());
            }
            insertDataIntoJavaFile(getInterfaceJavaFileHandle(), getJavaClassDefClose());

            if (isHasAugmentationExtended(getExtendsList())) {
                addHasAugmentationImport(curNode, imports, false);
            }
            if (isAugmentedInfoExtended(getExtendsList())) {
                addAugmentedInfoImport(curNode, imports, false);
            }

            if (curNode instanceof YangCase) {
                removeCaseImport(imports);
            }
        }

        if ((fileType & BUILDER_CLASS_MASK) != 0 | (fileType & IMPL_CLASS_MASK) != 0) {

            if (isAttributePresent) {
                addImportsToStringAndHasCodeMethods(curNode, imports);
            }
            if (isHasAugmentationExtended(getExtendsList())) {
                addAugmentedInfoImport(curNode, imports, true);
                addArrayListImport(curNode, imports, true);
            }

            /**
             * Create builder class file.
             */
            setBuilderClassJavaFileHandle(getJavaFileHandle(getJavaClassName(BUILDER_CLASS_FILE_NAME_SUFFIX)));
            setBuilderClassJavaFileHandle(
                    generateBuilderClassFile(getBuilderClassJavaFileHandle(), imports, curNode, isAttributePresent));
            /**
             * Create impl class file.
             */
            if ((fileType & IMPL_CLASS_MASK) != 0) {
                setImplClassJavaFileHandle(getJavaFileHandle(getJavaClassName(IMPL_CLASS_FILE_NAME_SUFFIX)));
                setImplClassJavaFileHandle(
                        generateImplClassFile(getImplClassJavaFileHandle(), curNode, isAttributePresent));
                /**
                 * Append impl class to builder class and close it.
                 */
                mergeJavaFiles(getImplClassJavaFileHandle(), getBuilderClassJavaFileHandle());
            }
            insertDataIntoJavaFile(getBuilderClassJavaFileHandle(), getJavaClassDefClose());
        }

        /**
         * Creates type def class file.
         */
        if ((fileType & GENERATE_TYPEDEF_CLASS) != 0) {
            addImportsToStringAndHasCodeMethods(curNode, imports);
            setTypedefClassJavaFileHandle(getJavaFileHandle(getJavaClassName(TYPEDEF_CLASS_FILE_NAME_SUFFIX)));
            setTypedefClassJavaFileHandle(generateTypeDefClassFile(getTypedefClassJavaFileHandle(), curNode, imports));
        }

        /**
         * Creates type class file.
         */
        if ((fileType & GENERATE_UNION_CLASS) != 0) {
            addImportsToStringAndHasCodeMethods(curNode, imports);
            setTypeClassJavaFileHandle(getJavaFileHandle(getJavaClassName(UNION_TYPE_CLASS_FILE_NAME_SUFFIX)));
            setTypeClassJavaFileHandle(generateUnionClassFile(getTypeClassJavaFileHandle(), curNode, imports));
        }

        /**
         * Creates type enum class file.
         */
        if ((fileType & GENERATE_ENUM_CLASS) != 0) {
            setEnumClassJavaFileHandle(getJavaFileHandle(getJavaClassName(ENUM_CLASS_FILE_NAME_SUFFIX)));
            setEnumClassJavaFileHandle(generateEnumClassFile(getEnumClassJavaFileHandle(), curNode));
        }

        /**
         * Creates rpc interface file.
         */
        if ((fileType & GENERATE_RPC_INTERFACE) != 0) {
            setRpcInterfaceJavaFileHandle(getJavaFileHandle(getJavaClassName(RPC_INTERFACE_FILE_NAME_SUFFIX)));
            setRpcInterfaceJavaFileHandle(generateRpcInterfaceFile(getRpcInterfaceJavaFileHandle(), curNode, imports));
        }

        /**
         * Close all the file handles.
         */
        close(false);
    }

    /**
     * Removes case import info from import list.
     *
     * @param imports list of imports
     * @return import for class
     */
    private List<String> removeCaseImport(List<String> imports) {
        if (imports != null && caseImportInfo != null) {
            String caseImport = IMPORT + caseImportInfo.getPkgInfo() + PERIOD + caseImportInfo.getClassInfo() +
                    SEMI_COLAN + NEW_LINE;
            imports.remove(caseImport);
        }
        return imports;
    }

    /**
     * Removes all temporary file handles.
     *
     * @param isErrorOccurred when translator fails to generate java files we need to close
     *                        all open file handles include temporary files and java files.
     * @throws IOException when failed to delete the temporary files
     */
    public void close(boolean isErrorOccurred) throws IOException {

        boolean isError = isErrorOccurred;
        /**
         * Close all java file handles and when error occurs delete the files.
         */
        if ((generatedJavaFiles & INTERFACE_MASK) != 0) {
            closeFile(getInterfaceJavaFileHandle(), isError);
        }
        if ((generatedJavaFiles & BUILDER_CLASS_MASK) != 0) {
            closeFile(getBuilderClassJavaFileHandle(), isError);
        }
        if ((generatedJavaFiles & BUILDER_INTERFACE_MASK) != 0) {
            closeFile(getBuilderInterfaceJavaFileHandle(), true);
        }
        if ((generatedJavaFiles & IMPL_CLASS_MASK) != 0) {
            closeFile(getImplClassJavaFileHandle(), true);
        }
        if ((generatedJavaFiles & GENERATE_TYPEDEF_CLASS) != 0) {
            closeFile(getTypedefClassJavaFileHandle(), isError);
        }
        if ((generatedJavaFiles & GENERATE_ENUM_CLASS) != 0) {
            closeFile(getEnumClassJavaFileHandle(), isError);
        }
        if ((generatedJavaFiles & GENERATE_UNION_CLASS) != 0) {
            closeFile(getTypeClassJavaFileHandle(), isError);
        }
        if ((generatedJavaFiles & GENERATE_RPC_INTERFACE) != 0) {
            closeFile(getRpcInterfaceJavaFileHandle(), isError);
        }

        /**
         * Close all temporary file handles and delete the files.
         */
        if ((generatedTempFiles & GETTER_FOR_INTERFACE_MASK) != 0) {
            closeFile(getGetterInterfaceTempFileHandle(), true);
        }
        if ((generatedTempFiles & GETTER_FOR_CLASS_MASK) != 0) {
            closeFile(getGetterImplTempFileHandle(), true);
        }
        if ((generatedTempFiles & SETTER_FOR_INTERFACE_MASK) != 0) {
            closeFile(getSetterInterfaceTempFileHandle(), true);
        }
        if ((generatedTempFiles & SETTER_FOR_CLASS_MASK) != 0) {
            closeFile(getSetterImplTempFileHandle(), true);
        }
        if ((generatedTempFiles & CONSTRUCTOR_IMPL_MASK) != 0) {
            closeFile(getConstructorImplTempFileHandle(), true);
        }
        if ((generatedTempFiles & ATTRIBUTES_MASK) != 0) {
            closeFile(getAttributesTempFileHandle(), true);
        }
        if ((generatedTempFiles & HASH_CODE_IMPL_MASK) != 0) {
            closeFile(getHashCodeImplTempFileHandle(), true);
        }
        if ((generatedTempFiles & TO_STRING_IMPL_MASK) != 0) {
            closeFile(getToStringImplTempFileHandle(), true);
        }
        if ((generatedTempFiles & EQUALS_IMPL_MASK) != 0) {
            closeFile(getEqualsImplTempFileHandle(), true);
        }
        if ((generatedTempFiles & ENUM_IMPL_MASK) != 0) {
            closeFile(getEnumClassTempFileHandle(), true);
        }
        if ((generatedTempFiles & CONSTRUCTOR_FOR_TYPE_MASK) != 0) {
            closeFile(getConstructorForTypeTempFileHandle(), true);
        }
        if ((generatedTempFiles & OF_STRING_IMPL_MASK) != 0) {
            closeFile(getOfStringImplTempFileHandle(), true);
        }
        if ((generatedTempFiles & FROM_STRING_IMPL_MASK) != 0) {
            closeFile(getFromStringImplTempFileHandle(), true);
        }
        if ((generatedTempFiles & RPC_IMPL_MASK) != 0) {
            closeFile(getRpcInterfaceImplTempFileHandle(), true);
        }
        clean(getTempDirPath());
        generatedTempFiles = 0;
    }
}

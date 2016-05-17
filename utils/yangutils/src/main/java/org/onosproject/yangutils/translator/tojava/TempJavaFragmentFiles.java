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

import org.onosproject.yangutils.datamodel.RpcNotificationContainer;
import org.onosproject.yangutils.datamodel.YangCase;
import org.onosproject.yangutils.datamodel.YangLeaf;
import org.onosproject.yangutils.datamodel.YangLeafList;
import org.onosproject.yangutils.datamodel.YangLeavesHolder;
import org.onosproject.yangutils.datamodel.YangNode;
import org.onosproject.yangutils.translator.exception.TranslatorException;
import org.onosproject.yangutils.translator.tojava.javamodel.JavaLeafInfoContainer;
import org.onosproject.yangutils.translator.tojava.utils.JavaExtendsListHolder;
import org.onosproject.yangutils.translator.tojava.utils.YangPluginConfig;

import static org.onosproject.yangutils.translator.tojava.GeneratedJavaFileType.BUILDER_CLASS_MASK;
import static org.onosproject.yangutils.translator.tojava.GeneratedJavaFileType.BUILDER_INTERFACE_MASK;
import static org.onosproject.yangutils.translator.tojava.GeneratedJavaFileType.GENERATE_ENUM_CLASS;
import static org.onosproject.yangutils.translator.tojava.GeneratedJavaFileType.GENERATE_SERVICE_AND_MANAGER;
import static org.onosproject.yangutils.translator.tojava.GeneratedJavaFileType.GENERATE_TYPE_CLASS;
import static org.onosproject.yangutils.translator.tojava.GeneratedJavaFileType.IMPL_CLASS_MASK;
import static org.onosproject.yangutils.translator.tojava.GeneratedJavaFileType.INTERFACE_MASK;
import static org.onosproject.yangutils.translator.tojava.GeneratedTempFileType.ATTRIBUTES_MASK;
import static org.onosproject.yangutils.translator.tojava.GeneratedTempFileType.EQUALS_IMPL_MASK;
import static org.onosproject.yangutils.translator.tojava.GeneratedTempFileType.FROM_STRING_IMPL_MASK;
import static org.onosproject.yangutils.translator.tojava.GeneratedTempFileType.GETTER_FOR_CLASS_MASK;
import static org.onosproject.yangutils.translator.tojava.GeneratedTempFileType.GETTER_FOR_INTERFACE_MASK;
import static org.onosproject.yangutils.translator.tojava.GeneratedTempFileType.HASH_CODE_IMPL_MASK;
import static org.onosproject.yangutils.translator.tojava.GeneratedTempFileType.SETTER_FOR_CLASS_MASK;
import static org.onosproject.yangutils.translator.tojava.GeneratedTempFileType.SETTER_FOR_INTERFACE_MASK;
import static org.onosproject.yangutils.translator.tojava.GeneratedTempFileType.TO_STRING_IMPL_MASK;
import static org.onosproject.yangutils.translator.tojava.JavaAttributeInfo.getAttributeInfoForTheData;
import static org.onosproject.yangutils.translator.tojava.JavaQualifiedTypeInfo.getQualifiedInfoOfFromString;
import static org.onosproject.yangutils.translator.tojava.JavaQualifiedTypeInfo.getQualifiedTypeInfoOfCurNode;
import static org.onosproject.yangutils.translator.tojava.utils.JavaCodeSnippetGen.getJavaAttributeDefination;
import static org.onosproject.yangutils.translator.tojava.utils.JavaCodeSnippetGen.getJavaClassDefClose;
import static org.onosproject.yangutils.translator.tojava.utils.JavaFileGenerator.generateBuilderClassFile;
import static org.onosproject.yangutils.translator.tojava.utils.JavaFileGenerator.generateBuilderInterfaceFile;
import static org.onosproject.yangutils.translator.tojava.utils.JavaFileGenerator.generateImplClassFile;
import static org.onosproject.yangutils.translator.tojava.utils.JavaFileGenerator.generateInterfaceFile;
import static org.onosproject.yangutils.translator.tojava.utils.JavaFileGeneratorUtils.getFileObject;
import static org.onosproject.yangutils.translator.tojava.utils.JavaIdentifierSyntax.getCamelCase;
import static org.onosproject.yangutils.translator.tojava.utils.JavaIdentifierSyntax.getCapitalCase;
import static org.onosproject.yangutils.translator.tojava.utils.JavaIdentifierSyntax.getPackageDirPathFromJavaJPackage;
import static org.onosproject.yangutils.translator.tojava.utils.JavaIdentifierSyntax.getParentNodeInGenCode;
import static org.onosproject.yangutils.translator.tojava.utils.MethodsGenerator.getBuildString;
import static org.onosproject.yangutils.translator.tojava.utils.MethodsGenerator.getDefaultConstructorString;
import static org.onosproject.yangutils.translator.tojava.utils.MethodsGenerator.getEqualsMethod;
import static org.onosproject.yangutils.translator.tojava.utils.MethodsGenerator.getFromStringMethod;
import static org.onosproject.yangutils.translator.tojava.utils.MethodsGenerator.getGetterForClass;
import static org.onosproject.yangutils.translator.tojava.utils.MethodsGenerator.getGetterString;
import static org.onosproject.yangutils.translator.tojava.utils.MethodsGenerator.getHashCodeMethod;
import static org.onosproject.yangutils.translator.tojava.utils.MethodsGenerator.getOfMethod;
import static org.onosproject.yangutils.translator.tojava.utils.MethodsGenerator.getOverRideString;
import static org.onosproject.yangutils.translator.tojava.utils.MethodsGenerator.getSetterForClass;
import static org.onosproject.yangutils.translator.tojava.utils.MethodsGenerator.getSetterString;
import static org.onosproject.yangutils.translator.tojava.utils.MethodsGenerator.getToStringMethod;
import static org.onosproject.yangutils.translator.tojava.utils.MethodsGenerator.parseBuilderInterfaceBuildMethodString;
import static org.onosproject.yangutils.translator.tojava.utils.TempJavaCodeFragmentFilesUtils.addArrayListImport;
import static org.onosproject.yangutils.translator.tojava.utils.TempJavaCodeFragmentFilesUtils.addAugmentationHoldersImport;
import static org.onosproject.yangutils.translator.tojava.utils.TempJavaCodeFragmentFilesUtils.addAugmentedInfoImport;
import static org.onosproject.yangutils.translator.tojava.utils.TempJavaCodeFragmentFilesUtils.closeFile;
import static org.onosproject.yangutils.translator.tojava.utils.TempJavaCodeFragmentFilesUtils.detectCollisionBwParentAndChildForImport;
import static org.onosproject.yangutils.translator.tojava.utils.TempJavaCodeFragmentFilesUtils.isAugmentationHolderExtended;
import static org.onosproject.yangutils.translator.tojava.utils.TempJavaCodeFragmentFilesUtils.isAugmentedInfoExtended;
import static org.onosproject.yangutils.translator.tojava.utils.TempJavaCodeFragmentFilesUtils.sortImports;
import static org.onosproject.yangutils.utils.UtilConstants.BUILDER;
import static org.onosproject.yangutils.utils.UtilConstants.EMPTY_STRING;
import static org.onosproject.yangutils.utils.UtilConstants.FOUR_SPACE_INDENTATION;
import static org.onosproject.yangutils.utils.UtilConstants.IMPL;
import static org.onosproject.yangutils.utils.UtilConstants.IMPORT;
import static org.onosproject.yangutils.utils.UtilConstants.INTERFACE;
import static org.onosproject.yangutils.utils.UtilConstants.NEW_LINE;
import static org.onosproject.yangutils.utils.UtilConstants.PERIOD;
import static org.onosproject.yangutils.utils.UtilConstants.SEMI_COLAN;
import static org.onosproject.yangutils.utils.UtilConstants.SLASH;
import static org.onosproject.yangutils.utils.io.impl.FileSystemUtil.createPackage;
import static org.onosproject.yangutils.utils.io.impl.FileSystemUtil.readAppendFile;
import static org.onosproject.yangutils.utils.io.impl.JavaDocGen.getJavaDoc;
import static org.onosproject.yangutils.utils.io.impl.JavaDocGen.JavaDocType.GETTER_METHOD;
import static org.onosproject.yangutils.utils.io.impl.JavaDocGen.JavaDocType.OF_METHOD;
import static org.onosproject.yangutils.utils.io.impl.YangIoUtils.getAbsolutePackagePath;
import static org.onosproject.yangutils.utils.io.impl.YangIoUtils.insertDataIntoJavaFile;
import static org.onosproject.yangutils.utils.io.impl.YangIoUtils.mergeJavaFiles;

/**
 * Represents implementation of java code fragments temporary implementations.
 * Manages the common temp file required for Java file(s) generated.
 */
public class TempJavaFragmentFiles {

    /**
     * Information about the java files being generated.
     */
    private JavaFileInfo javaFileInfo;

    /**
     * Imported class info.
     */
    private JavaImportData javaImportData;

    /**
     * The variable which guides the types of temporary files generated using
     * the temporary generated file types mask.
     */
    private int generatedTempFiles;

    /**
     * Absolute path where the target java file needs to be generated.
     */
    private String absoluteDirPath;

    /**
     * Contains all the interface(s)/class name which will be extended by generated files.
     */
    private JavaExtendsListHolder javaExtendsListHolder;
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
     * File name for setter method.
     */
    private static final String SETTER_METHOD_FILE_NAME = "SetterMethod";

    /**
     * File name for getter method implementation.
     */
    private static final String GETTER_METHOD_IMPL_FILE_NAME = "GetterMethodImpl";

    /**
     * File name for setter method implementation.
     */
    private static final String SETTER_METHOD_IMPL_FILE_NAME = "SetterMethodImpl";

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
     * Temporary file handle for attribute.
     */
    private File attributesTempFileHandle;

    /**
     * Temporary file handle for getter of interface.
     */
    private File getterInterfaceTempFileHandle;

    /**
     * Temporary file handle for setter of interface.
     */
    private File setterInterfaceTempFileHandle;

    /**
     * Temporary file handle for getter of class.
     */
    private File getterImplTempFileHandle;

    /**
     * Temporary file handle for setter of class.
     */
    private File setterImplTempFileHandle;

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
     * Temporary file handle for from string method of class.
     */
    private File fromStringImplTempFileHandle;

    /**
     * Import info for case.
     */
    private JavaQualifiedTypeInfo caseImportInfo;

    /**
     * Is attribute added.
     */
    private boolean isAttributePresent;

    /**
     * Creates an instance of temp java fragment files.
     */
    public TempJavaFragmentFiles() {
    }

    /**
     * Retrieves the absolute path where the file needs to be generated.
     *
     * @return absolute path where the file needs to be generated
     */
    private String getAbsoluteDirPath() {
        return absoluteDirPath;
    }

    /**
     * Sets absolute path where the file needs to be generated.
     *
     * @param absoluteDirPath absolute path where the file needs to be
     * generated.
     */
    void setAbsoluteDirPath(String absoluteDirPath) {
        this.absoluteDirPath = absoluteDirPath;
    }

    /**
     * Sets the generated java file information.
     *
     * @param javaFileInfo generated java file information
     */
    public void setJavaFileInfo(JavaFileInfo javaFileInfo) {
        this.javaFileInfo = javaFileInfo;
    }

    /**
     * Retrieves the generated java file information.
     *
     * @return generated java file information
     */
    public JavaFileInfo getJavaFileInfo() {
        return javaFileInfo;
    }

    /**
     * Retrieves the generated temp files.
     *
     * @return generated temp files
     */
    int getGeneratedTempFiles() {
        return generatedTempFiles;
    }

    /**
     * Clears the generated file mask.
     */
    void clearGeneratedTempFileMask() {
        generatedTempFiles = 0;
    }

    /**
     * Adds to generated temporary files.
     *
     * @param generatedTempFile generated file
     */
    void addGeneratedTempFile(int generatedTempFile) {
        generatedTempFiles |= generatedTempFile;
        setGeneratedTempFiles(generatedTempFiles);
    }

    /**
     * Sets generated file files.
     *
     * @param generatedTempFile generated file
     */
    void setGeneratedTempFiles(int fileType) {
        generatedTempFiles = fileType;
    }

    /**
     * Retrieves the generated Java files.
     *
     * @return generated Java files
     */
    int getGeneratedJavaFiles() {
        return getJavaFileInfo().getGeneratedFileTypes();
    }

    /**
     * Retrieves the mapped Java class name.
     *
     * @return mapped Java class name
     */
    String getGeneratedJavaClassName() {
        return getCapitalCase(getJavaFileInfo().getJavaName());
    }

    /**
     * Retrieves the import data for the generated Java file.
     *
     * @return import data for the generated Java file
     */
    public JavaImportData getJavaImportData() {
        return javaImportData;
    }

    /**
     * Sets import data for the generated Java file.
     *
     * @param javaImportData import data for the generated Java file
     */
    void setJavaImportData(JavaImportData javaImportData) {
        this.javaImportData = javaImportData;
    }

    /**
     * Retrieves the status of any attributes added.
     *
     * @return status of any attributes added
     */
    public boolean isAttributePresent() {
        return isAttributePresent;
    }

    /**
     * Sets status of any attributes added.
     *
     * @param attributePresent status of any attributes added
     */
    public void setAttributePresent(boolean attributePresent) {
        isAttributePresent = attributePresent;
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
     * @param fromStringImplTempFileHandle from string method's temporary file
     * handle
     */
    private void setFromStringImplTempFileHandle(File fromStringImplTempFileHandle) {
        this.fromStringImplTempFileHandle = fromStringImplTempFileHandle;
    }

    /**
     * Creates an instance of temporary java code fragment.
     *
     * @param javaFileInfo generated java file information
     * @throws IOException when fails to create new file handle
     */
    TempJavaFragmentFiles(JavaFileInfo javaFileInfo) throws IOException {
        setJavaExtendsListHolder(new JavaExtendsListHolder());
        setJavaImportData(new JavaImportData());
        setJavaFileInfo(javaFileInfo);
        setAbsoluteDirPath(getAbsolutePackagePath(getJavaFileInfo().getBaseCodeGenPath(),
                getJavaFileInfo().getPackageFilePath()));

        /*
         * Initialize getter when generation file type matches to interface
         * mask.
         */
        if ((getGeneratedJavaFiles() & INTERFACE_MASK) != 0) {
            addGeneratedTempFile(GETTER_FOR_INTERFACE_MASK);
        }

        /*
         * Initialize getter and setter when generation file type matches to
         * builder interface mask.
         */
        if ((getGeneratedJavaFiles() & BUILDER_INTERFACE_MASK) != 0) {
            addGeneratedTempFile(GETTER_FOR_INTERFACE_MASK);
            addGeneratedTempFile(SETTER_FOR_INTERFACE_MASK);
        }

        /*
         * Initialize getterImpl, setterImpl and attributes when generation file
         * type matches to builder class mask.
         */
        if ((getGeneratedJavaFiles() & BUILDER_CLASS_MASK) != 0) {
            addGeneratedTempFile(ATTRIBUTES_MASK);
            addGeneratedTempFile(GETTER_FOR_CLASS_MASK);
            addGeneratedTempFile(SETTER_FOR_CLASS_MASK);
        }

        /*
         * Initialize getterImpl, attributes, constructor, hash code, equals and
         * to strings when generation file type matches to impl class mask.
         */
        if ((getGeneratedJavaFiles() & IMPL_CLASS_MASK) != 0) {
            addGeneratedTempFile(ATTRIBUTES_MASK);
            addGeneratedTempFile(GETTER_FOR_CLASS_MASK);
            addGeneratedTempFile(HASH_CODE_IMPL_MASK);
            addGeneratedTempFile(EQUALS_IMPL_MASK);
            addGeneratedTempFile(TO_STRING_IMPL_MASK);
        }

        /*
         * Initialize temp files to generate type class.
         */
        if ((getGeneratedJavaFiles() & GENERATE_TYPE_CLASS) != 0) {
            addGeneratedTempFile(ATTRIBUTES_MASK);
            addGeneratedTempFile(GETTER_FOR_CLASS_MASK);
            addGeneratedTempFile(HASH_CODE_IMPL_MASK);
            addGeneratedTempFile(EQUALS_IMPL_MASK);
            addGeneratedTempFile(TO_STRING_IMPL_MASK);
            addGeneratedTempFile(FROM_STRING_IMPL_MASK);
        }

        /*
         * Initialize temp files to generate enum class.
         */
        if ((getGeneratedJavaFiles() & GENERATE_ENUM_CLASS) != 0) {
            addGeneratedTempFile(FROM_STRING_IMPL_MASK);
        }
        /*
         * Initialize getter and setter when generation file type matches to
         * builder interface mask.
         */
        if ((getGeneratedJavaFiles() & GENERATE_SERVICE_AND_MANAGER) != 0) {
            addGeneratedTempFile(GETTER_FOR_INTERFACE_MASK);
            addGeneratedTempFile(SETTER_FOR_INTERFACE_MASK);
            addGeneratedTempFile(GETTER_FOR_CLASS_MASK);
            addGeneratedTempFile(SETTER_FOR_CLASS_MASK);
        }

        /*
         * Set temporary file handles.
         */
        if ((getGeneratedTempFiles() & ATTRIBUTES_MASK) != 0) {
            setAttributesTempFileHandle(getTemporaryFileHandle(ATTRIBUTE_FILE_NAME));
        }

        if ((getGeneratedTempFiles() & GETTER_FOR_INTERFACE_MASK) != 0) {
            setGetterInterfaceTempFileHandle(getTemporaryFileHandle(GETTER_METHOD_FILE_NAME));
        }

        if ((getGeneratedTempFiles() & SETTER_FOR_INTERFACE_MASK) != 0) {
            setSetterInterfaceTempFileHandle(getTemporaryFileHandle(SETTER_METHOD_FILE_NAME));
        }

        if ((getGeneratedTempFiles() & GETTER_FOR_CLASS_MASK) != 0) {
            setGetterImplTempFileHandle(getTemporaryFileHandle(GETTER_METHOD_IMPL_FILE_NAME));
        }

        if ((getGeneratedTempFiles() & SETTER_FOR_CLASS_MASK) != 0) {
            setSetterImplTempFileHandle(getTemporaryFileHandle(SETTER_METHOD_IMPL_FILE_NAME));
        }

        if ((getGeneratedTempFiles() & HASH_CODE_IMPL_MASK) != 0) {
            setHashCodeImplTempFileHandle(getTemporaryFileHandle(HASH_CODE_METHOD_FILE_NAME));
        }
        if ((getGeneratedTempFiles() & EQUALS_IMPL_MASK) != 0) {
            setEqualsImplTempFileHandle(getTemporaryFileHandle(EQUALS_METHOD_FILE_NAME));
        }
        if ((getGeneratedTempFiles() & TO_STRING_IMPL_MASK) != 0) {
            setToStringImplTempFileHandle(getTemporaryFileHandle(TO_STRING_METHOD_FILE_NAME));
        }
        if ((getGeneratedTempFiles() & FROM_STRING_IMPL_MASK) != 0) {
            setFromStringImplTempFileHandle(getTemporaryFileHandle(FROM_STRING_METHOD_FILE_NAME));
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
    void setAttributesTempFileHandle(File attributeForClass) {
        attributesTempFileHandle = attributeForClass;
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
    void setGetterImplTempFileHandle(File getterImpl) {
        getterImplTempFileHandle = getterImpl;
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
    void setHashCodeImplTempFileHandle(File hashCodeMethod) {
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
    void setEqualsImplTempFileHandle(File equalsMethod) {
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
    void setToStringImplTempFileHandle(File toStringMethod) {
        toStringImplTempFileHandle = toStringMethod;
    }

    /**
     * Returns java extends list holder.
     *
     * @return java extends list holder
     */
    public JavaExtendsListHolder getJavaExtendsListHolder() {
        return javaExtendsListHolder;
    }

    /**
     * Sets java extends list holder.
     *
     * @param javaExtendsListHolder java extends list holder
     */
    public void setJavaExtendsListHolder(JavaExtendsListHolder javaExtendsListHolder) {
        this.javaExtendsListHolder = javaExtendsListHolder;
    }

    /**
     * Adds attribute for class.
     *
     * @param attr attribute info
     * @throws IOException when fails to append to temporary file
     */
    private void addAttribute(JavaAttributeInfo attr)
            throws IOException {
        appendToFile(getAttributesTempFileHandle(), parseAttribute(attr) + FOUR_SPACE_INDENTATION);
    }

    /**
     * Adds getter for interface.
     *
     * @param attr attribute info
     * @throws IOException when fails to append to temporary file
     */
    private void addGetterForInterface(JavaAttributeInfo attr)
            throws IOException {
        appendToFile(getGetterInterfaceTempFileHandle(),
                getGetterString(attr, getGeneratedJavaFiles()) + NEW_LINE);
    }

    /**
     * Adds setter for interface.
     *
     * @param attr attribute info
     * @throws IOException when fails to append to temporary file
     */
    private void addSetterForInterface(JavaAttributeInfo attr)
            throws IOException {
        appendToFile(getSetterInterfaceTempFileHandle(),
                getSetterString(attr, getGeneratedJavaClassName(), getGeneratedJavaFiles()) + NEW_LINE);
    }

    /**
     * Adds setter's implementation for class.
     *
     * @param attr attribute info
     * @throws IOException when fails to append to temporary file
     */
    private void addSetterImpl(JavaAttributeInfo attr)
            throws IOException {
        appendToFile(getSetterImplTempFileHandle(),
                getOverRideString() + getSetterForClass(attr, getGeneratedJavaClassName(), getGeneratedJavaFiles())
                        +
                        NEW_LINE);
    }

    /**
     * Adds getter method's impl for class.
     *
     * @param attr attribute info
     * @throws IOException when fails to append to temporary file
     */
    private void addGetterImpl(JavaAttributeInfo attr)
            throws IOException {
        if ((getGeneratedJavaFiles() & BUILDER_CLASS_MASK) != 0
                || (getGeneratedJavaFiles() & GENERATE_SERVICE_AND_MANAGER) != 0) {
            appendToFile(getGetterImplTempFileHandle(), getOverRideString() + getGetterForClass(attr,
                    getGeneratedJavaFiles()) + NEW_LINE);
        } else {
            appendToFile(getGetterImplTempFileHandle(),
                    getJavaDoc(GETTER_METHOD, getCapitalCase(attr.getAttributeName()), false)
                            + getGetterForClass(attr, getGeneratedJavaFiles()) + NEW_LINE);
        }
    }

    /**
     * Adds build method for interface.
     *
     * @return build method for interface
     * @throws IOException when fails to append to temporary file
     */
    String addBuildMethodForInterface()
            throws IOException {
        return parseBuilderInterfaceBuildMethodString(getGeneratedJavaClassName());
    }

    /**
     * Adds build method's implementation for class.
     *
     * @return build method implementation for class
     * @throws IOException when fails to append to temporary file
     */
    String addBuildMethodImpl()
            throws IOException {
        return getBuildString(getGeneratedJavaClassName()) + NEW_LINE;
    }

    /**
     * Adds default constructor for class.
     *
     * @param modifier modifier for constructor.
     * @param toAppend string which need to be appended with the class name
     * @return default constructor for class
     * @throws IOException when fails to append to file
     */
    String addDefaultConstructor(String modifier, String toAppend)
            throws IOException {
        return NEW_LINE + getDefaultConstructorString(getGeneratedJavaClassName() + toAppend, modifier);
    }

    /**
     * Adds default constructor for class.
     *
     * @return default constructor for class
     * @throws IOException when fails to append to file
     */
    public String addOfMethod()
            throws IOException {
        return getJavaDoc(OF_METHOD, getGeneratedJavaClassName(), false)
                + getOfMethod(getGeneratedJavaClassName(), null);
    }

    /**
     * Adds hash code method for class.
     *
     * @param attr attribute info
     * @throws IOException when fails to append to temporary file
     */
    private void addHashCodeMethod(JavaAttributeInfo attr)
            throws IOException {
        appendToFile(getHashCodeImplTempFileHandle(), getHashCodeMethod(attr) + NEW_LINE);
    }

    /**
     * Adds equals method for class.
     *
     * @param attr attribute info
     * @throws IOException when fails to append to temporary file
     */
    private void addEqualsMethod(JavaAttributeInfo attr)
            throws IOException {
        appendToFile(getEqualsImplTempFileHandle(), getEqualsMethod(attr) + NEW_LINE);
    }

    /**
     * Adds ToString method for class.
     *
     * @param attr attribute info
     * @throws IOException when fails to append to temporary file
     */
    private void addToStringMethod(JavaAttributeInfo attr)
            throws IOException {
        appendToFile(getToStringImplTempFileHandle(), getToStringMethod(attr) + NEW_LINE);
    }

    /**
     * Adds from string method for union class.
     *
     * @param javaAttributeInfo type attribute info
     * @param fromStringAttributeInfo from string attribute info
     * @throws IOException when fails to append to temporary file
     */
    private void addFromStringMethod(JavaAttributeInfo javaAttributeInfo,
            JavaAttributeInfo fromStringAttributeInfo)
            throws IOException {
        appendToFile(getFromStringImplTempFileHandle(), getFromStringMethod(javaAttributeInfo,
                fromStringAttributeInfo) + NEW_LINE);
    }

    /**
     * Returns a temporary file handle for the specific file type.
     *
     * @param fileName file name
     * @return temporary file handle
     * @throws IOException when fails to create new file handle
     */
    File getTemporaryFileHandle(String fileName)
            throws IOException {
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
    File getJavaFileHandle(String fileName) throws IOException {
        return getFileObject(getDirPath(), fileName, JAVA_FILE_EXTENSION, getJavaFileInfo());
    }

    /**
     * Returns data from the temporary files.
     *
     * @param file temporary file handle
     * @return stored data from temporary files
     * @throws IOException when failed to get data from the given file
     */
    public String getTemporaryDataFromFileHandle(File file)
            throws IOException {
        String path = getTempDirPath();
        if (new File(path + file.getName()).exists()) {
            return readAppendFile(path + file.getName(), EMPTY_STRING);
        } else {
            throw new IOException("Unable to get data from the given "
                    + file.getName() + " file for " + getGeneratedJavaClassName() + PERIOD);
        }
    }

    /**
     * Returns temporary directory path.
     *
     * @return directory path
     */
    String getTempDirPath() {
        return getPackageDirPathFromJavaJPackage(getAbsoluteDirPath()) + SLASH + getGeneratedJavaClassName()
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
        String attributeName = getCamelCase(attr.getAttributeName(), null);
        if (attr.isQualifiedName()) {
            return getJavaAttributeDefination(attr.getImportInfo().getPkgInfo(),
                    attr.getImportInfo().getClassInfo(),
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
    void appendToFile(File file, String data)
            throws IOException {
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
     * the parent generated code
     * @param isList is list construct
     * @throws IOException IO operation exception
     */
    public static void addCurNodeInfoInParentTempFile(YangNode curNode,
            boolean isList)
            throws IOException {
        YangNode parent = getParentNodeInGenCode(curNode);
        if (!(parent instanceof JavaCodeGenerator)) {
            throw new TranslatorException("missing parent node to contain current node info in generated file");
        }
        JavaAttributeInfo javaAttributeInfo = getCurNodeAsAttributeInParent(curNode,
                parent, isList);
        if (!(parent instanceof TempJavaCodeFragmentFilesContainer)) {
            throw new TranslatorException("missing parent temp file handle");
        }
        getNodesInterfaceFragmentFiles(parent)
                .addJavaSnippetInfoToApplicableTempFiles(javaAttributeInfo);
    }

    /**
     * Creates an attribute info object corresponding to a data model node and
     * return it.
     *
     * @param curNode current data model node for which the java code generation
     * is being handled
     * @param parentNode parent node in which the current node is an attribute
     * @param isListNode is the current added attribute needs to be a list
     * @return AttributeInfo attribute details required to add in temporary
     * files
     */
    public static JavaAttributeInfo getCurNodeAsAttributeInParent(
            YangNode curNode, YangNode parentNode, boolean isListNode) {
        String curNodeName = ((JavaFileInfoContainer) curNode).getJavaFileInfo().getJavaName();
        /*
         * Get the import info corresponding to the attribute for import in
         * generated java files or qualified access
         */
        JavaQualifiedTypeInfo qualifiedTypeInfo = getQualifiedTypeInfoOfCurNode(parentNode,
                getCapitalCase(curNodeName));
        if (!(parentNode instanceof TempJavaCodeFragmentFilesContainer)) {
            throw new TranslatorException("Parent node does not have file info");
        }
        TempJavaFragmentFiles tempJavaFragmentFiles = getNodesInterfaceFragmentFiles(parentNode);
        boolean isQualified = true;
        JavaImportData parentImportData = tempJavaFragmentFiles.getJavaImportData();
        if (isListNode) {
            parentImportData.setIfListImported(true);
        }
        if (!detectCollisionBwParentAndChildForImport(curNode, qualifiedTypeInfo)) {
            parentImportData.addImportInfo(qualifiedTypeInfo);
            isQualified = false;
        }
        return getAttributeInfoForTheData(qualifiedTypeInfo, curNodeName, null, isQualified, isListNode);
    }

    /**
     * Returns interface fragment files for node.
     *
     * @param node YANG node
     * @return interface fragment files for node
     */
    public static TempJavaFragmentFiles getNodesInterfaceFragmentFiles(YangNode node) {
        TempJavaFragmentFiles tempJavaFragmentFiles;
        if (node instanceof RpcNotificationContainer) {
            tempJavaFragmentFiles = ((TempJavaCodeFragmentFilesContainer) node)
                    .getTempJavaCodeFragmentFiles()
                    .getServiceTempFiles();
        } else {
            tempJavaFragmentFiles = ((TempJavaCodeFragmentFilesContainer) node)
                    .getTempJavaCodeFragmentFiles()
                    .getBeanTempFiles();
        }
        return tempJavaFragmentFiles;
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
        if (!(curNode instanceof JavaFileInfoContainer)) {
            throw new TranslatorException("missing java file information to get the package details "
                    + "of attribute corresponding to child node");
        }
        caseImportInfo.setClassInfo(getCapitalCase(getCamelCase(parent.getName(), null)));
        caseImportInfo.setPkgInfo(((JavaFileInfoContainer) parent).getJavaFileInfo().getPackage());
        ((TempJavaCodeFragmentFilesContainer) curNode).getTempJavaCodeFragmentFiles()
                .getBeanTempFiles().getJavaImportData().addImportInfo(caseImportInfo);
    }

    /**
     * Adds leaf attributes in generated files.
     *
     * @param listOfLeaves list of YANG leaf
     * @param yangPluginConfig
     * @throws IOException IO operation fail
     */
    private void addLeavesInfoToTempFiles(List<YangLeaf> listOfLeaves,
            YangPluginConfig yangPluginConfig)
            throws IOException {
        if (listOfLeaves != null) {
            for (YangLeaf leaf : listOfLeaves) {
                if (!(leaf instanceof JavaLeafInfoContainer)) {
                    throw new TranslatorException("Leaf does not have java information");
                }
                JavaLeafInfoContainer javaLeaf = (JavaLeafInfoContainer) leaf;
                javaLeaf.updateJavaQualifiedInfo();
                JavaAttributeInfo javaAttributeInfo = getAttributeInfoForTheData(
                        javaLeaf.getJavaQualifiedInfo(),
                        javaLeaf.getJavaName(yangPluginConfig.getConflictResolver()),
                        javaLeaf.getDataType(),
                        getIsQualifiedAccessOrAddToImportList(javaLeaf.getJavaQualifiedInfo()),
                        false);
                addJavaSnippetInfoToApplicableTempFiles(javaAttributeInfo);
            }
        }
    }

    /**
     * Adds leaf list's attributes in generated files.
     *
     * @param listOfLeafList list of YANG leaves
     * @param yangPluginConfig
     * @throws IOException IO operation fail
     */
    private void addLeafListInfoToTempFiles(List<YangLeafList> listOfLeafList, YangPluginConfig yangPluginConfig)
            throws IOException {
        if (listOfLeafList != null) {
            for (YangLeafList leafList : listOfLeafList) {
                if (!(leafList instanceof JavaLeafInfoContainer)) {
                    throw new TranslatorException("Leaf-list does not have java information");
                }
                JavaLeafInfoContainer javaLeaf = (JavaLeafInfoContainer) leafList;
                javaLeaf.updateJavaQualifiedInfo();
                getJavaImportData().setIfListImported(true);
                JavaAttributeInfo javaAttributeInfo = getAttributeInfoForTheData(
                        javaLeaf.getJavaQualifiedInfo(),
                        javaLeaf.getJavaName(yangPluginConfig.getConflictResolver()),
                        javaLeaf.getDataType(),
                        getIsQualifiedAccessOrAddToImportList(javaLeaf.getJavaQualifiedInfo()),
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
     * @param yangPluginConfig plugin config
     * @throws IOException IO operation fail
     */
    public void addCurNodeLeavesInfoToTempFiles(YangNode curNode,
            YangPluginConfig yangPluginConfig)
            throws IOException {
        if (!(curNode instanceof YangLeavesHolder)) {
            throw new TranslatorException("Data model node does not have any leaves");
        }
        YangLeavesHolder leavesHolder = (YangLeavesHolder) curNode;
        addLeavesInfoToTempFiles(leavesHolder.getListOfLeaf(), yangPluginConfig);
        addLeafListInfoToTempFiles(leavesHolder.getListOfLeafList(), yangPluginConfig);
    }

    /**
     * Adds the new attribute info to the target generated temporary files.
     *
     * @param newAttrInfo the attribute info that needs to be added to temporary
     * files
     * @throws IOException IO operation fail
     */
    void addJavaSnippetInfoToApplicableTempFiles(JavaAttributeInfo newAttrInfo)
            throws IOException {
        setAttributePresent(true);
        if ((getGeneratedTempFiles() & ATTRIBUTES_MASK) != 0) {
            addAttribute(newAttrInfo);
        }

        if ((getGeneratedTempFiles() & GETTER_FOR_INTERFACE_MASK) != 0) {
            addGetterForInterface(newAttrInfo);
        }

        if ((getGeneratedTempFiles() & SETTER_FOR_INTERFACE_MASK) != 0) {
            addSetterForInterface(newAttrInfo);
        }

        if ((getGeneratedTempFiles() & SETTER_FOR_CLASS_MASK) != 0) {
            addSetterImpl(newAttrInfo);
        }

        if ((getGeneratedTempFiles() & GETTER_FOR_CLASS_MASK) != 0) {
            addGetterImpl(newAttrInfo);
        }
        if ((getGeneratedTempFiles() & HASH_CODE_IMPL_MASK) != 0) {
            addHashCodeMethod(newAttrInfo);
        }
        if ((getGeneratedTempFiles() & EQUALS_IMPL_MASK) != 0) {
            addEqualsMethod(newAttrInfo);
        }
        if ((getGeneratedTempFiles() & TO_STRING_IMPL_MASK) != 0) {
            addToStringMethod(newAttrInfo);
        }

        if ((getGeneratedTempFiles() & FROM_STRING_IMPL_MASK) != 0) {
            JavaQualifiedTypeInfo qualifiedInfoOfFromString = getQualifiedInfoOfFromString(newAttrInfo);
            /*
             * Create a new java attribute info with qualified information of
             * wrapper classes.
             */
            JavaAttributeInfo fromStringAttributeInfo = getAttributeInfoForTheData(qualifiedInfoOfFromString,
                    newAttrInfo.getAttributeName(),
                    newAttrInfo.getAttributeType(),
                    getIsQualifiedAccessOrAddToImportList(qualifiedInfoOfFromString), false);

            addFromStringMethod(newAttrInfo, fromStringAttributeInfo);
        }
    }

    /**
     * Returns java class name.
     *
     * @param suffix for the class name based on the file type
     * @return java class name
     */
    String getJavaClassName(String suffix) {
        return getCapitalCase(getJavaFileInfo().getJavaName()) + suffix;
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
     * @param curNode current YANG node
     * @throws IOException when fails to generate java files
     */
    public void generateJavaFile(int fileType, YangNode curNode)
            throws IOException {
        List<String> imports = new ArrayList<>();
        if (isAttributePresent()) {
            imports = getJavaImportData().getImports();
        }
        createPackage(curNode);

        /*
         * Generate java code.
         */
        if ((fileType & INTERFACE_MASK) != 0 || (fileType &
                BUILDER_INTERFACE_MASK) != 0) {
            /*
             * Adds import for case.
             */
            if (curNode instanceof YangCase) {
                List<String> importData =
                        ((TempJavaCodeFragmentFilesContainer) curNode).getTempJavaCodeFragmentFiles()
                                .getBeanTempFiles().getJavaImportData().getImports();
                for (String importInfo : importData) {
                    if (!imports.contains(importInfo)) {
                        imports.add(importInfo);
                    }
                }
            }

            /*
             * Create interface file.
             */
            setInterfaceJavaFileHandle(getJavaFileHandle(getJavaClassName(INTERFACE_FILE_NAME_SUFFIX)));
            setInterfaceJavaFileHandle(
                    generateInterfaceFile(getInterfaceJavaFileHandle(), imports, curNode, isAttributePresent()));
            /*
             * Create builder interface file.
             */
            if ((fileType & BUILDER_INTERFACE_MASK) != 0) {
                setBuilderInterfaceJavaFileHandle(
                        getJavaFileHandle(getJavaClassName(BUILDER_INTERFACE_FILE_NAME_SUFFIX)));
                setBuilderInterfaceJavaFileHandle(
                        generateBuilderInterfaceFile(getBuilderInterfaceJavaFileHandle(), curNode,
                                isAttributePresent()));
                /*
                 * Append builder interface file to interface file and close it.
                 */
                mergeJavaFiles(getBuilderInterfaceJavaFileHandle(), getInterfaceJavaFileHandle());
            }
            insertDataIntoJavaFile(getInterfaceJavaFileHandle(), getJavaClassDefClose());
            if (isAugmentationHolderExtended(getJavaExtendsListHolder().getExtendsList())) {
                addAugmentationHoldersImport(curNode, imports, false);
            }
            if (isAugmentedInfoExtended(getJavaExtendsListHolder().getExtendsList())) {
                addAugmentedInfoImport(curNode, imports, false);
            }
            if (curNode instanceof YangCase) {
                removeCaseImport(imports);
            }
        }
        if ((fileType & BUILDER_CLASS_MASK) != 0 || (fileType & IMPL_CLASS_MASK) != 0) {
            if (isAttributePresent()) {
                addImportsToStringAndHasCodeMethods(curNode, imports);
            }
            if (isAugmentationHolderExtended(getJavaExtendsListHolder().getExtendsList())) {
                addAugmentedInfoImport(curNode, imports, true);
                addArrayListImport(curNode, imports, true);
            }
            sortImports(imports);
            /*
             * Create builder class file.
             */
            setBuilderClassJavaFileHandle(getJavaFileHandle(getJavaClassName(BUILDER_CLASS_FILE_NAME_SUFFIX)));
            setBuilderClassJavaFileHandle(
                    generateBuilderClassFile(getBuilderClassJavaFileHandle(), imports, curNode,
                            isAttributePresent()));
            /*
             * Create impl class file.
             */
            if ((fileType & IMPL_CLASS_MASK) != 0) {
                setImplClassJavaFileHandle(getJavaFileHandle(getJavaClassName(IMPL_CLASS_FILE_NAME_SUFFIX)));
                setImplClassJavaFileHandle(
                        generateImplClassFile(getImplClassJavaFileHandle(), curNode, isAttributePresent()));
                /*
                 * Append impl class to builder class and close it.
                 */
                mergeJavaFiles(getImplClassJavaFileHandle(), getBuilderClassJavaFileHandle());
            }
            insertDataIntoJavaFile(getBuilderClassJavaFileHandle(), getJavaClassDefClose());
            if (isAugmentationHolderExtended(getJavaExtendsListHolder().getExtendsList())) {
                addAugmentedInfoImport(curNode, imports, false);
                addArrayListImport(curNode, imports, false);
            }
        }
        /*
         * Close all the file handles.
         */
        freeTemporaryResources(false);
    }

    /**
     * Adds imports for ToString and HashCodeMethod.
     *
     * @param curNode current YANG node
     * @param imports import list
     * @return import list
     */
    public List<String> addImportsToStringAndHasCodeMethods(YangNode curNode, List<String> imports) {
        imports.add(getJavaImportData().getImportForHashAndEquals());
        imports.add(getJavaImportData().getImportForToString());
        return imports;
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
     * @param isErrorOccurred when translator fails to generate java files we
     * need to close all open file handles include temporary files
     * and java files.
     * @throws IOException when failed to delete the temporary files
     */
    public void freeTemporaryResources(boolean isErrorOccurred)
            throws IOException {
        boolean isError = isErrorOccurred;
        /*
         * Close all java file handles and when error occurs delete the files.
         */
        if ((getGeneratedJavaFiles() & INTERFACE_MASK) != 0) {
            closeFile(getInterfaceJavaFileHandle(), isError);
        }
        if ((getGeneratedJavaFiles() & BUILDER_CLASS_MASK) != 0) {
            closeFile(getBuilderClassJavaFileHandle(), isError);
        }
        if ((getGeneratedJavaFiles() & BUILDER_INTERFACE_MASK) != 0) {
            closeFile(getBuilderInterfaceJavaFileHandle(), true);
        }
        if ((getGeneratedJavaFiles() & IMPL_CLASS_MASK) != 0) {
            closeFile(getImplClassJavaFileHandle(), true);
        }

        /*
         * Close all temporary file handles and delete the files.
         */
        if ((getGeneratedTempFiles() & GETTER_FOR_CLASS_MASK) != 0) {
            closeFile(getGetterImplTempFileHandle(), true);
        }
        if ((getGeneratedTempFiles() & ATTRIBUTES_MASK) != 0) {
            closeFile(getAttributesTempFileHandle(), true);
        }
        if ((getGeneratedTempFiles() & HASH_CODE_IMPL_MASK) != 0) {
            closeFile(getHashCodeImplTempFileHandle(), true);
        }
        if ((getGeneratedTempFiles() & TO_STRING_IMPL_MASK) != 0) {
            closeFile(getToStringImplTempFileHandle(), true);
        }
        if ((getGeneratedTempFiles() & EQUALS_IMPL_MASK) != 0) {
            closeFile(getEqualsImplTempFileHandle(), true);
        }
        if ((getGeneratedTempFiles() & FROM_STRING_IMPL_MASK) != 0) {
            closeFile(getFromStringImplTempFileHandle(), true);
        }
    }

    /**
     * Returns if the attribute needs to be accessed in a qualified manner or
     * not, if it needs to be imported, then the same needs to be done.
     *
     * @param importInfo import info for the current attribute being added
     * @return status of the qualified access to the attribute
     */
    public boolean getIsQualifiedAccessOrAddToImportList(
            JavaQualifiedTypeInfo importInfo) {
        boolean isImportPkgEqualCurNodePkg;
        if (importInfo.getClassInfo().contentEquals(
                getGeneratedJavaClassName())) {
            /*
             * if the current class name is same as the attribute class name,
             * then the attribute must be accessed in a qualified manner.
             */
            return true;
        } else if (importInfo.getPkgInfo() != null) {
            /*
             * If the attribute type is having the package info, it is contender
             * for import list and also need to check if it needs to be a
             * qualified access.
             */
            isImportPkgEqualCurNodePkg = isImportPkgEqualCurNodePkg(importInfo);
            if (!isImportPkgEqualCurNodePkg) {
                /*
                 * If the package of the attribute added is not same as the
                 * current class package, then it must either be imported for
                 * access or it must be a qualified access.
                 */
                boolean isImportAdded = getJavaImportData().addImportInfo(importInfo);
                if (!isImportAdded) {
                    /*
                     * If the attribute type info is not imported, then it must
                     * be a qualified access.
                     */
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Checks if the import info is same as the package of the current generated
     * java file.
     *
     * @param importInfo import info for an attribute
     * @return true if the import info is same as the current nodes package
     * false otherwise
     */
    public boolean isImportPkgEqualCurNodePkg(JavaQualifiedTypeInfo importInfo) {
        return getJavaFileInfo().getPackage()
                .contentEquals(importInfo.getPkgInfo());
    }
}

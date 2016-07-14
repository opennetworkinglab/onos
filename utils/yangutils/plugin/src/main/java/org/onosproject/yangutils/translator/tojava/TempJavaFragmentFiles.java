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
import java.util.List;

import org.onosproject.yangutils.datamodel.YangAugment;
import org.onosproject.yangutils.datamodel.YangAugmentableNode;
import org.onosproject.yangutils.datamodel.YangCase;
import org.onosproject.yangutils.datamodel.YangLeaf;
import org.onosproject.yangutils.datamodel.YangLeafList;
import org.onosproject.yangutils.datamodel.YangLeavesHolder;
import org.onosproject.yangutils.datamodel.YangModule;
import org.onosproject.yangutils.datamodel.YangNode;
import org.onosproject.yangutils.datamodel.YangSubModule;
import org.onosproject.yangutils.translator.exception.TranslatorException;
import org.onosproject.yangutils.translator.tojava.javamodel.JavaLeafInfoContainer;
import org.onosproject.yangutils.translator.tojava.javamodel.YangJavaGrouping;
import org.onosproject.yangutils.translator.tojava.javamodel.YangJavaModule;
import org.onosproject.yangutils.translator.tojava.javamodel.YangJavaSubModule;
import org.onosproject.yangutils.translator.tojava.utils.JavaExtendsListHolder;
import org.onosproject.yangutils.utils.io.impl.YangPluginConfig;

import static org.onosproject.yangutils.datamodel.utils.DataModelUtils.getParentNodeInGenCode;
import static org.onosproject.yangutils.translator.tojava.GeneratedJavaFileType.BUILDER_CLASS_MASK;
import static org.onosproject.yangutils.translator.tojava.GeneratedJavaFileType.BUILDER_INTERFACE_MASK;
import static org.onosproject.yangutils.translator.tojava.GeneratedJavaFileType.GENERATE_ENUM_CLASS;
import static org.onosproject.yangutils.translator.tojava.GeneratedJavaFileType.GENERATE_INTERFACE_WITH_BUILDER;
import static org.onosproject.yangutils.translator.tojava.GeneratedJavaFileType.GENERATE_SERVICE_AND_MANAGER;
import static org.onosproject.yangutils.translator.tojava.GeneratedJavaFileType.GENERATE_TYPE_CLASS;
import static org.onosproject.yangutils.translator.tojava.GeneratedJavaFileType.IMPL_CLASS_MASK;
import static org.onosproject.yangutils.translator.tojava.GeneratedJavaFileType.INTERFACE_MASK;
import static org.onosproject.yangutils.translator.tojava.GeneratedJavaFileType.OPERATION_BUILDER_CLASS_MASK;
import static org.onosproject.yangutils.translator.tojava.GeneratedTempFileType.ATTRIBUTES_MASK;
import static org.onosproject.yangutils.translator.tojava.GeneratedTempFileType.AUGMENTE_CLASS_CONSTRUCTOR_MASK;
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
import static org.onosproject.yangutils.translator.tojava.javamodel.AttributesJavaDataType.updateJavaFileInfo;
import static org.onosproject.yangutils.translator.tojava.utils.JavaCodeSnippetGen.getJavaAttributeDefination;
import static org.onosproject.yangutils.translator.tojava.utils.JavaCodeSnippetGen.getJavaClassDefClose;
import static org.onosproject.yangutils.translator.tojava.utils.JavaCodeSnippetGen.sortImports;
import static org.onosproject.yangutils.translator.tojava.utils.JavaFileGenerator.generateBuilderClassFile;
import static org.onosproject.yangutils.translator.tojava.utils.JavaFileGenerator.generateBuilderInterfaceFile;
import static org.onosproject.yangutils.translator.tojava.utils.JavaFileGenerator.generateImplClassFile;
import static org.onosproject.yangutils.translator.tojava.utils.JavaFileGenerator.generateInterfaceFile;
import static org.onosproject.yangutils.translator.tojava.utils.JavaFileGenerator.generateOpParamBuilderClassFile;
import static org.onosproject.yangutils.translator.tojava.utils.JavaFileGenerator.generateOpParamImplClassFile;
import static org.onosproject.yangutils.translator.tojava.utils.JavaFileGeneratorUtils.addResolvedAugmentedDataNodeImports;
import static org.onosproject.yangutils.translator.tojava.utils.JavaFileGeneratorUtils.getFileObject;
import static org.onosproject.yangutils.translator.tojava.utils.JavaIdentifierSyntax.createPackage;
import static org.onosproject.yangutils.translator.tojava.utils.MethodsGenerator.getAugmentedConstructor;
import static org.onosproject.yangutils.translator.tojava.utils.MethodsGenerator.getBuildString;
import static org.onosproject.yangutils.translator.tojava.utils.MethodsGenerator.getDefaultConstructorString;
import static org.onosproject.yangutils.translator.tojava.utils.MethodsGenerator.getEqualsMethod;
import static org.onosproject.yangutils.translator.tojava.utils.MethodsGenerator.getFromStringMethod;
import static org.onosproject.yangutils.translator.tojava.utils.MethodsGenerator.getGetterForClass;
import static org.onosproject.yangutils.translator.tojava.utils.MethodsGenerator.getGetterString;
import static org.onosproject.yangutils.translator.tojava.utils.MethodsGenerator.getHashCodeMethod;
import static org.onosproject.yangutils.translator.tojava.utils.MethodsGenerator.getOverRideString;
import static org.onosproject.yangutils.translator.tojava.utils.MethodsGenerator.getSetterForClass;
import static org.onosproject.yangutils.translator.tojava.utils.MethodsGenerator.getSetterString;
import static org.onosproject.yangutils.translator.tojava.utils.MethodsGenerator.getToStringMethod;
import static org.onosproject.yangutils.translator.tojava.utils.MethodsGenerator.parseBuilderInterfaceBuildMethodString;
import static org.onosproject.yangutils.utils.UtilConstants.ACTIVATE;
import static org.onosproject.yangutils.utils.UtilConstants.BUILDER;
import static org.onosproject.yangutils.utils.UtilConstants.COMPONENT;
import static org.onosproject.yangutils.utils.UtilConstants.DEACTIVATE;
import static org.onosproject.yangutils.utils.UtilConstants.DEFAULT;
import static org.onosproject.yangutils.utils.UtilConstants.EMPTY_STRING;
import static org.onosproject.yangutils.utils.UtilConstants.FOUR_SPACE_INDENTATION;
import static org.onosproject.yangutils.utils.UtilConstants.IMPL;
import static org.onosproject.yangutils.utils.UtilConstants.IMPORT;
import static org.onosproject.yangutils.utils.UtilConstants.INTERFACE;
import static org.onosproject.yangutils.utils.UtilConstants.MANAGER;
import static org.onosproject.yangutils.utils.UtilConstants.NEW_LINE;
import static org.onosproject.yangutils.utils.UtilConstants.OPERATION;
import static org.onosproject.yangutils.utils.UtilConstants.PERIOD;
import static org.onosproject.yangutils.utils.UtilConstants.PRIVATE;
import static org.onosproject.yangutils.utils.UtilConstants.PROTECTED;
import static org.onosproject.yangutils.utils.UtilConstants.REFERENCE;
import static org.onosproject.yangutils.utils.UtilConstants.REFERENCE_CARDINALITY;
import static org.onosproject.yangutils.utils.UtilConstants.SEMI_COLAN;
import static org.onosproject.yangutils.utils.UtilConstants.SERVICE;
import static org.onosproject.yangutils.utils.UtilConstants.SLASH;
import static org.onosproject.yangutils.utils.UtilConstants.YANG_AUGMENTED_INFO;
import static org.onosproject.yangutils.utils.io.impl.FileSystemUtil.closeFile;
import static org.onosproject.yangutils.utils.io.impl.FileSystemUtil.readAppendFile;
import static org.onosproject.yangutils.utils.io.impl.JavaDocGen.JavaDocType.GETTER_METHOD;
import static org.onosproject.yangutils.utils.io.impl.JavaDocGen.getJavaDoc;
import static org.onosproject.yangutils.utils.io.impl.YangIoUtils.getAbsolutePackagePath;
import static org.onosproject.yangutils.utils.io.impl.YangIoUtils.getCamelCase;
import static org.onosproject.yangutils.utils.io.impl.YangIoUtils.getCapitalCase;
import static org.onosproject.yangutils.utils.io.impl.YangIoUtils.getPackageDirPathFromJavaJPackage;
import static org.onosproject.yangutils.utils.io.impl.YangIoUtils.insertDataIntoJavaFile;
import static org.onosproject.yangutils.utils.io.impl.YangIoUtils.mergeJavaFiles;
import static org.onosproject.yangutils.utils.io.impl.YangIoUtils.validateLineLength;

/**
 * Represents implementation of java code fragments temporary implementations. Manages the common temp file required for
 * Java file(s) generated.
 */
public class TempJavaFragmentFiles {

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
     * File name for augment copy constructor method.
     */
    private static final String AUGMENT_CONSTRUCTOR_FILE_NAME = "AugmentConstructor";

    /**
     * Information about the java files being generated.
     */
    private JavaFileInfo javaFileInfo;

    /**
     * Imported class info.
     */
    private JavaImportData javaImportData;

    /**
     * The variable which guides the types of temporary files generated using the temporary generated file types mask.
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
     * Retrieves the temporary file handle of op param builder class.
     *
     * @return op param builder temporary file handle
     */
    public File getBuilderOpParmClassJavaFileHandle() {
        return builderOpParmClassJavaFileHandle;
    }

    /**
     * Sets the java file handle for op param builder class.
     *
     * @param builderOpParmClassJavaFileHandle java file handle
     */
    public void setBuilderOpParmClassJavaFileHandle(File builderOpParmClassJavaFileHandle) {
        this.builderOpParmClassJavaFileHandle = builderOpParmClassJavaFileHandle;
    }

    /**
     * Java file handle for builder op param class file.
     */
    private File builderOpParmClassJavaFileHandle;

    /**
     * Java file handle for impl class file.
     */
    private File implClassJavaFileHandle;

    /**
     * Java file handle for op param class file.
     */
    private File opParamClassJavaFileHandle;

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
     * Base code gen path.
     */
    private String baseCodePath;

    /**
     * Temporary file handle for augments copy constructor method of class.
     */
    private File augmentConstructorImplTempFileHandle;

    TempJavaFragmentFiles() {
    }

    /**
     * Creates an instance of temporary java code fragment.
     *
     * @param javaFileInfo generated java file information
     * @throws IOException when fails to create new file handle
     */
    TempJavaFragmentFiles(JavaFileInfo javaFileInfo)
            throws IOException {
        setJavaExtendsListHolder(new JavaExtendsListHolder());
        setJavaImportData(new JavaImportData());
        setJavaFileInfo(javaFileInfo);
        if ((javaFileInfo.getGeneratedFileTypes() & GENERATE_SERVICE_AND_MANAGER) != 0) {
            setBaseCodePath(getJavaFileInfo().getPluginConfig().getCodeGenDir());
            setAbsoluteDirPath(getAbsolutePackagePath(getJavaFileInfo().getPluginConfig().getCodeGenDir(),
                    getJavaFileInfo().getPackageFilePath()));
        } else {
            setBaseCodePath(getJavaFileInfo().getBaseCodeGenPath());
            setAbsoluteDirPath(getAbsolutePackagePath(getJavaFileInfo().getBaseCodeGenPath(),
                    getJavaFileInfo().getPackageFilePath()));
        }
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
            addGeneratedTempFile(AUGMENTE_CLASS_CONSTRUCTOR_MASK);
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
        if ((getGeneratedTempFiles() & AUGMENTE_CLASS_CONSTRUCTOR_MASK) != 0) {
            setAugmentConstructorImplTempFileHandle(getTemporaryFileHandle(AUGMENT_CONSTRUCTOR_FILE_NAME));
        }

    }

    /**
     * Adds current node info as and attribute to the parent generated file.
     *
     * @param curNode      current node which needs to be added as an attribute in the parent generated code
     * @param isList       is list construct
     * @param pluginConfig plugin configurations
     * @throws IOException IO operation exception
     */
    public static void addCurNodeInfoInParentTempFile(YangNode curNode,
                                                      boolean isList, YangPluginConfig pluginConfig)
            throws IOException {
        YangNode parent = getParentNodeInGenCode(curNode);
        if (!(parent instanceof JavaCodeGenerator)) {
            throw new TranslatorException("missing parent node to contain current node info in generated file");
        }

        if (parent instanceof YangJavaGrouping) {
            /*
             * In case of grouping, there is no need to add the information, it
             * will be taken care in uses
             */
            return;
        }
        TempJavaBeanFragmentFiles tempJavaBeanFragmentFiles = ((JavaCodeGeneratorInfo) parent)
                .getTempJavaCodeFragmentFiles().getBeanTempFiles();

        JavaAttributeInfo javaAttributeInfo = getCurNodeAsAttributeInTarget(curNode,
                parent, isList, tempJavaBeanFragmentFiles);
        if (!(parent instanceof TempJavaCodeFragmentFilesContainer)) {
            throw new TranslatorException("missing parent temp file handle");
        }
        getNodesInterfaceFragmentFiles(parent, javaAttributeInfo, pluginConfig);
    }

    /**
     * Creates an attribute info object corresponding to a data model node and return it.
     *
     * @param curNode               current data model node for which the java code generation is being handled
     * @param targetNode            target node in which the current node is an attribute
     * @param isListNode            is the current added attribute needs to be a list
     * @param tempJavaFragmentFiles temp java fragment files
     * @return AttributeInfo attribute details required to add in temporary files
     */
    public static JavaAttributeInfo getCurNodeAsAttributeInTarget(YangNode curNode,
                                                                  YangNode targetNode, boolean isListNode,
                                                                  TempJavaFragmentFiles tempJavaFragmentFiles) {
        String curNodeName = ((JavaFileInfoContainer) curNode).getJavaFileInfo().getJavaName();
        if (curNodeName == null) {
            updateJavaFileInfo(curNode, null);
            curNodeName = ((JavaFileInfoContainer) curNode).getJavaFileInfo().getJavaName();
        }

        /*
         * Get the import info corresponding to the attribute for import in
         * generated java files or qualified access
         */
        JavaQualifiedTypeInfo qualifiedTypeInfo = getQualifiedTypeInfoOfCurNode(curNode,
                getCapitalCase(curNodeName));
        if (!(targetNode instanceof TempJavaCodeFragmentFilesContainer)) {
            throw new TranslatorException("Parent node does not have file info");
        }
        JavaImportData parentImportData = tempJavaFragmentFiles.getJavaImportData();
        JavaFileInfo fileInfo = ((JavaFileInfoContainer) targetNode).getJavaFileInfo();

        boolean isQualified;
        if ((targetNode instanceof YangJavaModule || targetNode instanceof YangJavaSubModule)
                && (qualifiedTypeInfo.getClassInfo().contentEquals(SERVICE)
                || qualifiedTypeInfo.getClassInfo().contentEquals(COMPONENT)
                || qualifiedTypeInfo.getClassInfo().contentEquals(getCapitalCase(ACTIVATE))
                || qualifiedTypeInfo.getClassInfo().contentEquals(getCapitalCase(DEACTIVATE))
                || qualifiedTypeInfo.getClassInfo().contentEquals(REFERENCE_CARDINALITY)
                || qualifiedTypeInfo.getClassInfo().contentEquals(REFERENCE))
                || qualifiedTypeInfo.getClassInfo().contentEquals(getCapitalCase(fileInfo.getJavaName() + SERVICE))
                || qualifiedTypeInfo.getClassInfo().contentEquals(getCapitalCase(fileInfo.getJavaName() + MANAGER))) {

            isQualified = true;
        } else {
            String className;
            if (targetNode instanceof YangJavaModule || targetNode instanceof YangJavaSubModule) {
                className = getCapitalCase(fileInfo.getJavaName()) + "Service";
            } else {
                className = getCapitalCase(fileInfo.getJavaName());
            }

            isQualified = parentImportData.addImportInfo(qualifiedTypeInfo,
                    className, fileInfo.getPackage());
        }

        if (isListNode) {
            parentImportData.setIfListImported(true);
        }

        return getAttributeInfoForTheData(qualifiedTypeInfo, curNodeName, null, isQualified, isListNode);
    }

    /**
     * Adds java code snippet for corresponding files.
     *
     * @param node   java node
     * @param attr   attribute info
     * @param config plugin configurations
     * @throws IOException when fails to do IO operations
     */
    public static void getNodesInterfaceFragmentFiles(YangNode node, JavaAttributeInfo attr,
                                                      YangPluginConfig config) throws IOException {
        TempJavaFragmentFiles tempJavaFragmentFiles;
        JavaFileInfo javaFileInfo = ((JavaFileInfoContainer) node).getJavaFileInfo();
        if ((javaFileInfo.getGeneratedFileTypes() & GENERATE_SERVICE_AND_MANAGER) != 0) {
            tempJavaFragmentFiles = ((TempJavaCodeFragmentFilesContainer) node)
                    .getTempJavaCodeFragmentFiles()
                    .getServiceTempFiles();
            tempJavaFragmentFiles.addJavaSnippetInfoToApplicableTempFiles(attr, config);
        }
        if ((javaFileInfo.getGeneratedFileTypes() & GENERATE_INTERFACE_WITH_BUILDER) != 0) {
            tempJavaFragmentFiles = ((TempJavaCodeFragmentFilesContainer) node)
                    .getTempJavaCodeFragmentFiles()
                    .getBeanTempFiles();
            tempJavaFragmentFiles.addJavaSnippetInfoToApplicableTempFiles(attr, config);
        }

    }

    /**
     * Returns base code path.
     *
     * @return base code path
     */
    public String getBaseCodePath() {
        return baseCodePath;
    }

    /**
     * Sets base code path.
     *
     * @param baseCodePath base code path
     */
    public void setBaseCodePath(String baseCodePath) {
        this.baseCodePath = baseCodePath;
    }

    /*
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
     * @param absoluteDirPath absolute path where the file needs to be generated.
     */
    void setAbsoluteDirPath(String absoluteDirPath) {
        this.absoluteDirPath = absoluteDirPath;
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
     * Sets the generated java file information.
     *
     * @param javaFileInfo generated java file information
     */
    public void setJavaFileInfo(JavaFileInfo javaFileInfo) {
        this.javaFileInfo = javaFileInfo;
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
     * Sets generated file files.
     *
     * @param fileType generated file type
     */
    void setGeneratedTempFiles(int fileType) {
        generatedTempFiles = fileType;
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
     * @param fromStringImplTempFileHandle from string method's temporary file handle
     */
    private void setFromStringImplTempFileHandle(File fromStringImplTempFileHandle) {
        this.fromStringImplTempFileHandle = fromStringImplTempFileHandle;
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
     * Returns augments copy constructor method impl class file.
     *
     * @return augments copy constructor method impl class file
     */
    public File getAugmentConstructorImplTempFileHandle() {
        return augmentConstructorImplTempFileHandle;
    }

    /**
     * Sets augments copy constructor method impl class.
     *
     * @param augmentConstructorImplTempFileHandle augments copy constructor method impl class file
     */
    public void setAugmentConstructorImplTempFileHandle(File augmentConstructorImplTempFileHandle) {
        this.augmentConstructorImplTempFileHandle = augmentConstructorImplTempFileHandle;
    }

    /**
     * Adds attribute for class.
     *
     * @param attr             attribute info
     * @param yangPluginConfig plugin configurations
     * @throws IOException when fails to append to temporary file
     */
    private void addAttribute(JavaAttributeInfo attr, YangPluginConfig yangPluginConfig)
            throws IOException {
        appendToFile(getAttributesTempFileHandle(), parseAttribute(attr, yangPluginConfig)
                + FOUR_SPACE_INDENTATION);
    }

    /**
     * Adds getter for interface.
     *
     * @param attr         attribute info
     * @param pluginConfig plugin configurations
     * @throws IOException when fails to append to temporary file
     */
    private void addGetterForInterface(JavaAttributeInfo attr, YangPluginConfig pluginConfig)
            throws IOException {
        appendToFile(getGetterInterfaceTempFileHandle(),
                getGetterString(attr, getGeneratedJavaFiles(), pluginConfig) + NEW_LINE);
    }

    /**
     * Adds setter for interface.
     *
     * @param attr         attribute info
     * @param pluginConfig plugin configurations
     * @throws IOException when fails to append to temporary file
     */
    private void addSetterForInterface(JavaAttributeInfo attr, YangPluginConfig pluginConfig)
            throws IOException {
        appendToFile(getSetterInterfaceTempFileHandle(),
                getSetterString(attr, getGeneratedJavaClassName(), getGeneratedJavaFiles(), pluginConfig)
                        + NEW_LINE);
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
     * @param attr         attribute info
     * @param pluginConfig plugin configurations
     * @throws IOException when fails to append to temporary file
     */
    private void addGetterImpl(JavaAttributeInfo attr, YangPluginConfig pluginConfig)
            throws IOException {
        if ((getGeneratedJavaFiles() & BUILDER_CLASS_MASK) != 0
                || (getGeneratedJavaFiles() & GENERATE_SERVICE_AND_MANAGER) != 0) {
            appendToFile(getGetterImplTempFileHandle(), getOverRideString() + getGetterForClass(attr,
                    getGeneratedJavaFiles()) + NEW_LINE);
        } else {
            appendToFile(getGetterImplTempFileHandle(),
                    getJavaDoc(GETTER_METHOD, getCapitalCase(attr.getAttributeName()), false, pluginConfig)
                            + getGetterForClass(attr, getGeneratedJavaFiles()) + NEW_LINE);
        }
    }

    /**
     * Adds build method for interface.
     *
     * @param pluginConfig plugin configurations
     * @return build method for interface
     * @throws IOException when fails to append to temporary file
     */
    String addBuildMethodForInterface(YangPluginConfig pluginConfig)
            throws IOException {
        return parseBuilderInterfaceBuildMethodString(getGeneratedJavaClassName(), pluginConfig);
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
     * @param modifier     modifier for constructor.
     * @param toAppend     string which need to be appended with the class name
     * @param pluginConfig plugin configurations
     * @return default constructor for class
     * @throws IOException when fails to append to file
     */
    String addDefaultConstructor(String modifier, String toAppend, YangPluginConfig pluginConfig)
            throws IOException {
        return NEW_LINE
                + getDefaultConstructorString(getGeneratedJavaClassName() + toAppend, modifier, pluginConfig);
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
     * @param javaAttributeInfo       type attribute info
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
     * Adds constructor for augment class.
     *
     * @param attr attribute info
     * @throws IOException when fails to append to temporary file
     */
    private void addAugmentConstructor(JavaAttributeInfo attr, YangPluginConfig pluginConfig)
            throws IOException {
        appendToFile(getAugmentConstructorImplTempFileHandle(), getAugmentedConstructor(attr, pluginConfig));
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
        String path = getTempDirPath(getAbsoluteDirPath());
        File dir = new File(path);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        File file = new File(path + fileName + TEMP_FILE_EXTENSION);
        if (!file.exists()) {
            file.createNewFile();
        } else {
            throw new IOException(fileName + " is reused due to YANG naming");
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
    File getJavaFileHandle(String fileName)
            throws IOException {
        return getFileObject(getDirPath(), fileName, JAVA_FILE_EXTENSION, getBaseCodePath());
    }

    /**
     * Returns data from the temporary files.
     *
     * @param file         temporary file handle
     * @param absolutePath absolute path
     * @return stored data from temporary files
     * @throws IOException when failed to get data from the given file
     */
    public String getTemporaryDataFromFileHandle(File file, String absolutePath)
            throws IOException {

        String path = getTempDirPath(absolutePath);
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
     * @param absolutePath absolute path
     * @return directory path
     */
    String getTempDirPath(String absolutePath) {
        return getPackageDirPathFromJavaJPackage(absolutePath) + SLASH + getGeneratedJavaClassName()
                + TEMP_FOLDER_NAME_SUFIX + SLASH;
    }

    /**
     * Parses attribute to get the attribute string.
     *
     * @param attr         attribute info
     * @param pluginConfig plugin configurations
     * @return attribute string
     */
    public String parseAttribute(JavaAttributeInfo attr, YangPluginConfig pluginConfig) {
        /*
         * TODO: check if this utility needs to be called or move to the caller
         */
        String attributeName = getCamelCase(attr.getAttributeName(), pluginConfig.getConflictResolver());
        String attributeAccessType = PRIVATE;
        if ((javaFileInfo.getGeneratedFileTypes() & GENERATE_INTERFACE_WITH_BUILDER) != 0) {
            attributeAccessType = PROTECTED;
        }
        if (attr.isQualifiedName()) {
            return getJavaAttributeDefination(attr.getImportInfo().getPkgInfo(),
                    attr.getImportInfo().getClassInfo(),
                    attributeName, attr.isListAttr(), attributeAccessType);
        } else {
            return getJavaAttributeDefination(null, attr.getImportInfo().getClassInfo(), attributeName,
                    attr.isListAttr(), attributeAccessType);
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
     * Adds parent's info to current node import list.
     *
     * @param curNode      current node for which import list needs to be updated
     * @param pluginConfig plugin configurations
     */
    public void addParentInfoInCurNodeTempFile(YangNode curNode, YangPluginConfig pluginConfig) {
        caseImportInfo = new JavaQualifiedTypeInfo();
        YangNode parent = getParentNodeInGenCode(curNode);
        if (!(parent instanceof JavaCodeGenerator)) {
            throw new TranslatorException("missing parent node to contain current node info in generated file");
        }
        if (!(curNode instanceof JavaFileInfoContainer)) {
            throw new TranslatorException("missing java file information to get the package details "
                    + "of attribute corresponding to child node");
        }
        caseImportInfo.setClassInfo(getCapitalCase(getCamelCase(parent.getName(),
                pluginConfig.getConflictResolver())));
        caseImportInfo.setPkgInfo(((JavaFileInfoContainer) parent).getJavaFileInfo().getPackage());

        JavaFileInfo fileInfo = ((JavaFileInfoContainer) curNode).getJavaFileInfo();

        ((TempJavaCodeFragmentFilesContainer) curNode).getTempJavaCodeFragmentFiles()
                .getBeanTempFiles().getJavaImportData().addImportInfo(caseImportInfo,
                getCapitalCase(fileInfo.getJavaName()), fileInfo.getPackage());
    }

    /**
     * Adds leaf attributes in generated files.
     *
     * @param listOfLeaves     list of YANG leaf
     * @param yangPluginConfig plugin config
     * @throws IOException IO operation fail
     */
    public void addLeavesInfoToTempFiles(List<YangLeaf> listOfLeaves,
                                         YangPluginConfig yangPluginConfig)
            throws IOException {
        if (listOfLeaves != null) {
            for (YangLeaf leaf : listOfLeaves) {
                if (!(leaf instanceof JavaLeafInfoContainer)) {
                    throw new TranslatorException("Leaf does not have java information");
                }
                addJavaSnippetInfoToApplicableTempFiles(getJavaAttributeOfLeaf(this, leaf, yangPluginConfig),
                        yangPluginConfig);
            }
        }
    }

    /**
     * Returns java attribute for leaf.
     *
     * @param tempJavaFragmentFiles temporary generated file
     * @param leaf             YANG leaf
     * @param yangPluginConfig plugin configurations
     * @return java attribute for leaf
     */
    public static JavaAttributeInfo getJavaAttributeOfLeaf(TempJavaFragmentFiles tempJavaFragmentFiles, YangLeaf leaf,
                                                            YangPluginConfig yangPluginConfig) {
        JavaLeafInfoContainer javaLeaf = (JavaLeafInfoContainer) leaf;
        javaLeaf.setConflictResolveConfig(yangPluginConfig.getConflictResolver());
        javaLeaf.updateJavaQualifiedInfo();
        return getAttributeInfoForTheData(
                javaLeaf.getJavaQualifiedInfo(),
                javaLeaf.getJavaName(yangPluginConfig.getConflictResolver()),
                javaLeaf.getDataType(),
                tempJavaFragmentFiles.getIsQualifiedAccessOrAddToImportList(javaLeaf.getJavaQualifiedInfo()),
                false);
    }

    /**
     * Adds leaf list's attributes in generated files.
     *
     * @param listOfLeafList   list of YANG leaves
     * @param yangPluginConfig plugin config
     * @throws IOException IO operation fail
     */
    public void addLeafListInfoToTempFiles(List<YangLeafList> listOfLeafList, YangPluginConfig yangPluginConfig)
            throws IOException {
        if (listOfLeafList != null) {
            for (YangLeafList leafList : listOfLeafList) {
                if (!(leafList instanceof JavaLeafInfoContainer)) {
                    throw new TranslatorException("Leaf-list does not have java information");
                }
                addJavaSnippetInfoToApplicableTempFiles(getJavaAttributeOfLeafList(this, leafList, yangPluginConfig),
                        yangPluginConfig);
            }
        }
    }

    /**
     * Returns java attribute for leaf-list.
     *
     * @param tempJavaFragmentFiles temporary generated file
     * @param leafList         YANG leaf-list
     * @param yangPluginConfig plugin configurations
     * @return java attribute for leaf-list
     */
    public static JavaAttributeInfo getJavaAttributeOfLeafList(TempJavaFragmentFiles tempJavaFragmentFiles,
                                                           YangLeafList leafList, YangPluginConfig yangPluginConfig) {
        JavaLeafInfoContainer javaLeaf = (JavaLeafInfoContainer) leafList;
        javaLeaf.setConflictResolveConfig(yangPluginConfig.getConflictResolver());
        javaLeaf.updateJavaQualifiedInfo();
        tempJavaFragmentFiles.getJavaImportData().setIfListImported(true);
        return getAttributeInfoForTheData(
                javaLeaf.getJavaQualifiedInfo(),
                javaLeaf.getJavaName(yangPluginConfig.getConflictResolver()),
                javaLeaf.getDataType(),
                tempJavaFragmentFiles.getIsQualifiedAccessOrAddToImportList(javaLeaf.getJavaQualifiedInfo()),
                true);
    }

    /**
     * Adds all the leaves in the current data model node as part of the
     * generated temporary file.
     *
     * @param curNode          java file info of the generated file
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
     * @param newAttrInfo  the attribute info that needs to be added to temporary files
     * @param pluginConfig plugin configurations
     * @throws IOException IO operation fail
     */
    void addJavaSnippetInfoToApplicableTempFiles(JavaAttributeInfo newAttrInfo, YangPluginConfig pluginConfig)
            throws IOException {
        setAttributePresent(true);
        if ((getGeneratedTempFiles() & ATTRIBUTES_MASK) != 0) {
            addAttribute(newAttrInfo, pluginConfig);
        }

        if ((getGeneratedTempFiles() & GETTER_FOR_INTERFACE_MASK) != 0) {
            addGetterForInterface(newAttrInfo, pluginConfig);
        }

        if ((getGeneratedTempFiles() & SETTER_FOR_INTERFACE_MASK) != 0) {
            addSetterForInterface(newAttrInfo, pluginConfig);
        }

        if ((getGeneratedTempFiles() & SETTER_FOR_CLASS_MASK) != 0) {
            addSetterImpl(newAttrInfo);
        }

        if ((getGeneratedTempFiles() & GETTER_FOR_CLASS_MASK) != 0) {
            addGetterImpl(newAttrInfo, pluginConfig);
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
            JavaQualifiedTypeInfo qualifiedInfoOfFromString = getQualifiedInfoOfFromString(newAttrInfo,
                    pluginConfig.getConflictResolver());
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
     * Returns java class name.
     *
     * @return java class name
     */
    String getImplClassName() {
        return getCapitalCase(DEFAULT) + getCapitalCase(getJavaFileInfo().getJavaName());
    }

    /**
     * Returns op param java class name.
     *
     * @return op param java class name
     */
    String getOpParamImplClassName() {
        return getCapitalCase(getJavaFileInfo().getJavaName()) + OPERATION;
    }

    /**
     * Returns op param builder java class name.
     *
     * @return op param builder java class name
     */
    String getOpParamBuilderImplClassName() {
        return getCapitalCase(getJavaFileInfo().getJavaName()) + OPERATION + BUILDER;
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
    public void generateJavaFile(int fileType, YangNode curNode)
            throws IOException {

        if (curNode instanceof YangModule || curNode instanceof YangSubModule) {
            addResolvedAugmentedDataNodeImports(curNode);
        }
        TempJavaBeanFragmentFiles tempJavaBeanFragmentFiles = ((JavaCodeGeneratorInfo) curNode)
                .getTempJavaCodeFragmentFiles().getBeanTempFiles();
        List<String> imports = ((JavaCodeGeneratorInfo) curNode).getTempJavaCodeFragmentFiles().getBeanTempFiles()
                .getJavaImportData().getImports();

        JavaFileInfo curInfo = ((JavaFileInfoContainer) curNode).getJavaFileInfo();

        createPackage(curNode);

        /*
         * Generate java code.
         */
        if ((fileType & INTERFACE_MASK) != 0 || (fileType &
                BUILDER_INTERFACE_MASK) != 0) {

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
                validateLineLength(getInterfaceJavaFileHandle());
            }
            insertDataIntoJavaFile(getInterfaceJavaFileHandle(), getJavaClassDefClose());

            if (curNode instanceof YangCase) {
                removeCaseImport(imports);
            }

            if (curNode instanceof YangAugment) {
                removeAugmentedInfoImport(imports);
            }
        }
        if ((fileType & BUILDER_CLASS_MASK) != 0 || (fileType & IMPL_CLASS_MASK) != 0) {
            if (isAttributePresent()) {
                addImportsToStringAndHasCodeMethods(imports, true);
            }
            if (curNode instanceof YangAugmentableNode) {
                addImportsForAugmentableClass(imports, true);
            }
            sortImports(imports);
            /*
             * Create impl class file.
             */
            setImplClassJavaFileHandle(getJavaFileHandle(getImplClassName()));
            setImplClassJavaFileHandle(
                    generateImplClassFile(getImplClassJavaFileHandle(), curNode, isAttributePresent(), imports));

            /*
             * Create builder class file.
             */
            if ((fileType & BUILDER_CLASS_MASK) != 0) {
                setBuilderClassJavaFileHandle(getJavaFileHandle(getJavaClassName(BUILDER_CLASS_FILE_NAME_SUFFIX)));
                setBuilderClassJavaFileHandle(
                        generateBuilderClassFile(getBuilderClassJavaFileHandle(), curNode,
                                isAttributePresent()));
                /*
                 * Append impl class to builder class and close it.
                 */
                mergeJavaFiles(getBuilderClassJavaFileHandle(), getImplClassJavaFileHandle());
                validateLineLength(getImplClassJavaFileHandle());
            }
            insertDataIntoJavaFile(getImplClassJavaFileHandle(), getJavaClassDefClose());
            if (curNode instanceof YangAugmentableNode) {
                addImportsForAugmentableClass(imports, false);
            }
            if (!(curNode instanceof YangModule)) {
                if (isAttributePresent()) {
                    addImportsToStringAndHasCodeMethods(imports, false);
                }
                if (curNode instanceof YangAugmentableNode) {
                    addYangAugmentedOpParamInfoImport(imports);
                }
                JavaQualifiedTypeInfo qualifiedTypeInfo = new JavaQualifiedTypeInfo();
                qualifiedTypeInfo.setClassInfo(getCapitalCase(DEFAULT) + getCapitalCase(getJavaFileInfo()
                        .getJavaName()));
                qualifiedTypeInfo.setPkgInfo(getJavaFileInfo().getPackage());
                getJavaExtendsListHolder().addToExtendsList(qualifiedTypeInfo, curNode,
                        tempJavaBeanFragmentFiles);
                addBitsetImport(imports, true);
                if (curNode instanceof YangAugment) {
                    addYangAugmentedOpParamInfoImport(imports);
                }
                /*
                 * Create impl class file.
                 */
                setOpParamClassJavaFileHandle(getJavaFileHandle(getOpParamImplClassName()));
                setOpParamClassJavaFileHandle(
                        generateOpParamImplClassFile(getOpParamClassJavaFileHandle(), curNode,
                                isAttributePresent(), imports));

                /*
                 * Create builder class file.
                 */
                if ((fileType & BUILDER_CLASS_MASK) != 0) {
                    setBuilderOpParmClassJavaFileHandle(getJavaFileHandle(getOpParamBuilderImplClassName()));
                    setBuilderOpParmClassJavaFileHandle(
                            generateOpParamBuilderClassFile(getBuilderOpParmClassJavaFileHandle(), curNode,
                                    isAttributePresent()));
                    /*
                     * Append impl class to builder class and close it.
                     */
                    mergeJavaFiles(getBuilderOpParmClassJavaFileHandle(), getOpParamClassJavaFileHandle());
                    validateLineLength(getOpParamClassJavaFileHandle());

                    addBitsetImport(imports, false);
                }
                insertDataIntoJavaFile(getOpParamClassJavaFileHandle(), getJavaClassDefClose());

            }
        }
        /*
         * Close all the file handles.
         */
        freeTemporaryResources(false);
    }

    /*Adds import for YANG augmented op param info.*/
    private void addYangAugmentedOpParamInfoImport(List<String> imports) {
        imports.add(getJavaImportData().getYangAugmentedOpParamInfoImport());
    }

    /**
     * Adds imports for ToString and HashCodeMethod.
     *
     * @param imports   import list
     * @param operation add or remove
     */
    public void addImportsToStringAndHasCodeMethods(List<String> imports, boolean operation) {
        if (operation) {
            imports.add(getJavaImportData().getImportForHashAndEquals());
            imports.add(getJavaImportData().getImportForToString());
        } else {
            imports.remove(getJavaImportData().getImportForHashAndEquals());
            imports.remove(getJavaImportData().getImportForToString());
        }
    }

    /**
     * Adds imports for bitset method.
     *
     * @param imports   import list
     * @param operation add or remove
     */
    public void addBitsetImport(List<String> imports, boolean operation) {
        if (operation) {
            imports.add(getJavaImportData().getImportForToBitSet());
        } else {
            imports.remove(getJavaImportData().getImportForToBitSet());
        }
    }


    /**
     * Adds import for map and hash map.
     *
     * @param imports    import list
     * @param operations true for adding and false for deletion
     */
    public void addImportsForAugmentableClass(List<String> imports, boolean operations) {
        if (operations) {
            imports.add(getJavaImportData().getHashMapImport());
            imports.add(getJavaImportData().getMapImport());
            imports.add(getJavaImportData().getYangAugmentedInfoImport());
        } else {
            imports.remove(getJavaImportData().getHashMapImport());
            imports.remove(getJavaImportData().getMapImport());
        }
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
     * Removes case import info from import list.
     *
     * @param imports list of imports
     * @return import for class
     */
    private void removeAugmentedInfoImport(List<String> imports) {
        imports.remove(getJavaImportData().getYangAugmentedInfoImport());

        for (JavaQualifiedTypeInfo type : getJavaImportData().getImportSet()) {
            if (type.getClassInfo().equals(YANG_AUGMENTED_INFO)) {
                getJavaImportData().getImportSet().remove(type);
                getJavaExtendsListHolder().getExtendsList().remove(type);
            }
        }
    }

    /**
     * Removes all temporary file handles.
     *
     * @param isErrorOccurred when translator fails to generate java files we need to close all open file handles
     *                        include temporary files and java files.
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
            closeFile(getBuilderClassJavaFileHandle(), true);
        }
        if ((getGeneratedJavaFiles() & OPERATION_BUILDER_CLASS_MASK) != 0) {
            closeFile(getBuilderOpParmClassJavaFileHandle(), true);
        }
        if ((getGeneratedJavaFiles() & BUILDER_INTERFACE_MASK) != 0) {
            closeFile(getBuilderInterfaceJavaFileHandle(), true);
        }
        if ((getGeneratedJavaFiles() & IMPL_CLASS_MASK) != 0) {
            closeFile(getImplClassJavaFileHandle(), isError);
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
        if ((getGeneratedTempFiles() & AUGMENTE_CLASS_CONSTRUCTOR_MASK) != 0) {
            closeFile(getAugmentConstructorImplTempFileHandle(), true);
        }
    }

    /**
     * Returns if the attribute needs to be accessed in a qualified manner or not, if it needs to be imported, then the
     * same needs to be done.
     *
     * @param importInfo import info for the current attribute being added
     * @return status of the qualified access to the attribute
     */
    public boolean getIsQualifiedAccessOrAddToImportList(
            JavaQualifiedTypeInfo importInfo) {

        return getJavaImportData().addImportInfo(importInfo, getGeneratedJavaClassName(),
                getJavaFileInfo().getPackage());
    }

    /**
     * Returns java file handle for op param class file.
     *
     * @return java file handle for op param class file
     */
    public File getOpParamClassJavaFileHandle() {
        return opParamClassJavaFileHandle;
    }

    /**
     * Sets the java file handle for op param class file.
     *
     * @param opParamClassJavaFileHandle java file handle
     */
    public void setOpParamClassJavaFileHandle(File opParamClassJavaFileHandle) {
        this.opParamClassJavaFileHandle = opParamClassJavaFileHandle;
    }
}

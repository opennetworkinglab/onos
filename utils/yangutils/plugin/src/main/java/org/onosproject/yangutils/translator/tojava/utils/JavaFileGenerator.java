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

package org.onosproject.yangutils.translator.tojava.utils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.onosproject.yangutils.datamodel.YangAugment;
import org.onosproject.yangutils.datamodel.YangAugmentableNode;
import org.onosproject.yangutils.datamodel.YangLeaf;
import org.onosproject.yangutils.datamodel.YangLeafList;
import org.onosproject.yangutils.datamodel.YangLeavesHolder;
import org.onosproject.yangutils.datamodel.YangModule;
import org.onosproject.yangutils.datamodel.YangNode;
import org.onosproject.yangutils.datamodel.YangType;
import org.onosproject.yangutils.datamodel.YangTypeDef;
import org.onosproject.yangutils.datamodel.utils.builtindatatype.YangDataTypes;
import org.onosproject.yangutils.translator.tojava.JavaAttributeInfo;
import org.onosproject.yangutils.translator.tojava.JavaCodeGeneratorInfo;
import org.onosproject.yangutils.translator.tojava.JavaFileInfo;
import org.onosproject.yangutils.translator.tojava.JavaFileInfoContainer;
import org.onosproject.yangutils.translator.tojava.JavaQualifiedTypeInfo;
import org.onosproject.yangutils.translator.tojava.TempJavaCodeFragmentFilesContainer;
import org.onosproject.yangutils.translator.tojava.TempJavaEnumerationFragmentFiles;
import org.onosproject.yangutils.translator.tojava.TempJavaEventFragmentFiles;
import org.onosproject.yangutils.translator.tojava.TempJavaServiceFragmentFiles;
import org.onosproject.yangutils.translator.tojava.YangJavaModelUtils;
import org.onosproject.yangutils.utils.io.impl.YangPluginConfig;

import static org.onosproject.yangutils.translator.tojava.GeneratedJavaFileType.BUILDER_CLASS_MASK;
import static org.onosproject.yangutils.translator.tojava.GeneratedJavaFileType.BUILDER_INTERFACE_MASK;
import static org.onosproject.yangutils.translator.tojava.GeneratedJavaFileType.GENERATE_ENUM_CLASS;
import static org.onosproject.yangutils.translator.tojava.GeneratedJavaFileType.GENERATE_EVENT_CLASS;
import static org.onosproject.yangutils.translator.tojava.GeneratedJavaFileType.GENERATE_EVENT_LISTENER_INTERFACE;
import static org.onosproject.yangutils.translator.tojava.GeneratedJavaFileType.GENERATE_EVENT_SUBJECT_CLASS;
import static org.onosproject.yangutils.translator.tojava.GeneratedJavaFileType.GENERATE_SERVICE_AND_MANAGER;
import static org.onosproject.yangutils.translator.tojava.GeneratedJavaFileType.GENERATE_TYPEDEF_CLASS;
import static org.onosproject.yangutils.translator.tojava.GeneratedJavaFileType.GENERATE_UNION_CLASS;
import static org.onosproject.yangutils.translator.tojava.GeneratedJavaFileType.IMPL_CLASS_MASK;
import static org.onosproject.yangutils.translator.tojava.GeneratedJavaFileType.INTERFACE_MASK;
import static org.onosproject.yangutils.translator.tojava.GeneratedJavaFileType.OPERATION_BUILDER_CLASS_MASK;
import static org.onosproject.yangutils.translator.tojava.GeneratedJavaFileType.OPERATION_CLASS_MASK;
import static org.onosproject.yangutils.translator.tojava.GeneratedTempFileType.ATTRIBUTES_MASK;
import static org.onosproject.yangutils.translator.tojava.GeneratedTempFileType.CONSTRUCTOR_FOR_TYPE_MASK;
import static org.onosproject.yangutils.translator.tojava.GeneratedTempFileType.CONSTRUCTOR_IMPL_MASK;
import static org.onosproject.yangutils.translator.tojava.GeneratedTempFileType.ENUM_IMPL_MASK;
import static org.onosproject.yangutils.translator.tojava.GeneratedTempFileType.EQUALS_IMPL_MASK;
import static org.onosproject.yangutils.translator.tojava.GeneratedTempFileType.EVENT_ENUM_MASK;
import static org.onosproject.yangutils.translator.tojava.GeneratedTempFileType.EVENT_METHOD_MASK;
import static org.onosproject.yangutils.translator.tojava.GeneratedTempFileType.EVENT_SUBJECT_ATTRIBUTE_MASK;
import static org.onosproject.yangutils.translator.tojava.GeneratedTempFileType.EVENT_SUBJECT_GETTER_MASK;
import static org.onosproject.yangutils.translator.tojava.GeneratedTempFileType.EVENT_SUBJECT_SETTER_MASK;
import static org.onosproject.yangutils.translator.tojava.GeneratedTempFileType.FROM_STRING_IMPL_MASK;
import static org.onosproject.yangutils.translator.tojava.GeneratedTempFileType.GETTER_FOR_CLASS_MASK;
import static org.onosproject.yangutils.translator.tojava.GeneratedTempFileType.GETTER_FOR_INTERFACE_MASK;
import static org.onosproject.yangutils.translator.tojava.GeneratedTempFileType.HASH_CODE_IMPL_MASK;
import static org.onosproject.yangutils.translator.tojava.GeneratedTempFileType.OF_STRING_IMPL_MASK;
import static org.onosproject.yangutils.translator.tojava.GeneratedTempFileType.RPC_IMPL_MASK;
import static org.onosproject.yangutils.translator.tojava.GeneratedTempFileType.RPC_INTERFACE_MASK;
import static org.onosproject.yangutils.translator.tojava.GeneratedTempFileType.SETTER_FOR_CLASS_MASK;
import static org.onosproject.yangutils.translator.tojava.GeneratedTempFileType.SETTER_FOR_INTERFACE_MASK;
import static org.onosproject.yangutils.translator.tojava.GeneratedTempFileType.TO_STRING_IMPL_MASK;
import static org.onosproject.yangutils.translator.tojava.JavaAttributeInfo.getAttributeInfoForTheData;
import static org.onosproject.yangutils.translator.tojava.JavaQualifiedTypeInfo.getQualifiedTypeInfoOfCurNode;
import static org.onosproject.yangutils.translator.tojava.TempJavaFragmentFiles.getCurNodeAsAttributeInTarget;
import static org.onosproject.yangutils.translator.tojava.utils.JavaCodeSnippetGen.addAugmentationAttribute;
import static org.onosproject.yangutils.translator.tojava.utils.JavaCodeSnippetGen.getEnumsValueAttribute;
import static org.onosproject.yangutils.translator.tojava.utils.JavaCodeSnippetGen.getEventEnumTypeStart;
import static org.onosproject.yangutils.translator.tojava.utils.JavaFileGeneratorUtils.getDataFromTempFileHandle;
import static org.onosproject.yangutils.translator.tojava.utils.JavaFileGeneratorUtils.initiateJavaFileGeneration;
import static org.onosproject.yangutils.translator.tojava.utils.MethodsGenerator.addActivateMethod;
import static org.onosproject.yangutils.translator.tojava.utils.MethodsGenerator.addDeActivateMethod;
import static org.onosproject.yangutils.translator.tojava.utils.MethodsGenerator.getAddAugmentInfoMethodImpl;
import static org.onosproject.yangutils.translator.tojava.utils.MethodsGenerator.getAugmentInfoImpl;
import static org.onosproject.yangutils.translator.tojava.utils.MethodsGenerator.getAugmentInfoMapImpl;
import static org.onosproject.yangutils.translator.tojava.utils.MethodsGenerator.getAugmentsDataMethodForManager;
import static org.onosproject.yangutils.translator.tojava.utils.MethodsGenerator.getAugmentsDataMethodForService;
import static org.onosproject.yangutils.translator.tojava.utils.MethodsGenerator.getBaseClassMethodImpl;
import static org.onosproject.yangutils.translator.tojava.utils.MethodsGenerator.getConstructorStart;
import static org.onosproject.yangutils.translator.tojava.utils.MethodsGenerator.getEnumsConstructor;
import static org.onosproject.yangutils.translator.tojava.utils.MethodsGenerator.getEnumsOfMethod;
import static org.onosproject.yangutils.translator.tojava.utils.MethodsGenerator.getEqualsMethodClose;
import static org.onosproject.yangutils.translator.tojava.utils.MethodsGenerator.getEqualsMethodOpen;
import static org.onosproject.yangutils.translator.tojava.utils.MethodsGenerator.getFromStringMethodClose;
import static org.onosproject.yangutils.translator.tojava.utils.MethodsGenerator.getFromStringMethodSignature;
import static org.onosproject.yangutils.translator.tojava.utils.MethodsGenerator.getGetter;
import static org.onosproject.yangutils.translator.tojava.utils.MethodsGenerator.getGetterForClass;
import static org.onosproject.yangutils.translator.tojava.utils.MethodsGenerator.getGetterString;
import static org.onosproject.yangutils.translator.tojava.utils.MethodsGenerator.getHashCodeMethodClose;
import static org.onosproject.yangutils.translator.tojava.utils.MethodsGenerator.getHashCodeMethodOpen;
import static org.onosproject.yangutils.translator.tojava.utils.MethodsGenerator.getIsFilterContentMatch;
import static org.onosproject.yangutils.translator.tojava.utils.MethodsGenerator.getOmitNullValueString;
import static org.onosproject.yangutils.translator.tojava.utils.MethodsGenerator.getOpParamConstructorStart;
import static org.onosproject.yangutils.translator.tojava.utils.MethodsGenerator.getOperationTypeSetter;
import static org.onosproject.yangutils.translator.tojava.utils.MethodsGenerator.getOperationTypegetter;
import static org.onosproject.yangutils.translator.tojava.utils.MethodsGenerator.getOverRideString;
import static org.onosproject.yangutils.translator.tojava.utils.MethodsGenerator.getSetterForClass;
import static org.onosproject.yangutils.translator.tojava.utils.MethodsGenerator.getSetterForLeaf;
import static org.onosproject.yangutils.translator.tojava.utils.MethodsGenerator.getSetterForLeafList;
import static org.onosproject.yangutils.translator.tojava.utils.MethodsGenerator.getSetterString;
import static org.onosproject.yangutils.translator.tojava.utils.MethodsGenerator.getToStringLeafListgetter;
import static org.onosproject.yangutils.translator.tojava.utils.MethodsGenerator.getToStringLeafgetter;
import static org.onosproject.yangutils.translator.tojava.utils.MethodsGenerator.getToStringMethodClose;
import static org.onosproject.yangutils.translator.tojava.utils.MethodsGenerator.getToStringMethodOpen;
import static org.onosproject.yangutils.translator.tojava.utils.MethodsGenerator.getToStringSelectLeafListgetter;
import static org.onosproject.yangutils.translator.tojava.utils.MethodsGenerator.getToStringSelectLeafgetter;
import static org.onosproject.yangutils.utils.UtilConstants.BASE64;
import static org.onosproject.yangutils.utils.UtilConstants.BITSET;
import static org.onosproject.yangutils.utils.UtilConstants.BUILDER;
import static org.onosproject.yangutils.utils.UtilConstants.CLOSE_CURLY_BRACKET;
import static org.onosproject.yangutils.utils.UtilConstants.CLOSE_PARENTHESIS;
import static org.onosproject.yangutils.utils.UtilConstants.COMMA;
import static org.onosproject.yangutils.utils.UtilConstants.CREATE;
import static org.onosproject.yangutils.utils.UtilConstants.DEFAULT;
import static org.onosproject.yangutils.utils.UtilConstants.DELETE;
import static org.onosproject.yangutils.utils.UtilConstants.EIGHT_SPACE_INDENTATION;
import static org.onosproject.yangutils.utils.UtilConstants.EMPTY_STRING;
import static org.onosproject.yangutils.utils.UtilConstants.ENCODE_TO_STRING;
import static org.onosproject.yangutils.utils.UtilConstants.ENUM;
import static org.onosproject.yangutils.utils.UtilConstants.EQUAL;
import static org.onosproject.yangutils.utils.UtilConstants.EVENT_LISTENER_STRING;
import static org.onosproject.yangutils.utils.UtilConstants.EVENT_STRING;
import static org.onosproject.yangutils.utils.UtilConstants.EVENT_SUBJECT_NAME_SUFFIX;
import static org.onosproject.yangutils.utils.UtilConstants.FILTER_LEAF;
import static org.onosproject.yangutils.utils.UtilConstants.FILTER_LEAF_LIST;
import static org.onosproject.yangutils.utils.UtilConstants.FOUR_SPACE_INDENTATION;
import static org.onosproject.yangutils.utils.UtilConstants.GET_ENCODER;
import static org.onosproject.yangutils.utils.UtilConstants.GET_FILTER_LEAF;
import static org.onosproject.yangutils.utils.UtilConstants.GET_FILTER_LEAF_LIST;
import static org.onosproject.yangutils.utils.UtilConstants.IMPORT;
import static org.onosproject.yangutils.utils.UtilConstants.INT;
import static org.onosproject.yangutils.utils.UtilConstants.JAVA_UTIL_IMPORT_BASE64_CLASS;
import static org.onosproject.yangutils.utils.UtilConstants.JAVA_UTIL_OBJECTS_IMPORT_PKG;
import static org.onosproject.yangutils.utils.UtilConstants.LOGGER_STATEMENT;
import static org.onosproject.yangutils.utils.UtilConstants.MANAGER;
import static org.onosproject.yangutils.utils.UtilConstants.MERGE;
import static org.onosproject.yangutils.utils.UtilConstants.NEW;
import static org.onosproject.yangutils.utils.UtilConstants.NEW_LINE;
import static org.onosproject.yangutils.utils.UtilConstants.OBJECT;
import static org.onosproject.yangutils.utils.UtilConstants.OPEN_CURLY_BRACKET;
import static org.onosproject.yangutils.utils.UtilConstants.OPEN_PARENTHESIS;
import static org.onosproject.yangutils.utils.UtilConstants.OPERATION_ENUM;
import static org.onosproject.yangutils.utils.UtilConstants.OP_PARAM_TYPE;
import static org.onosproject.yangutils.utils.UtilConstants.PERIOD;
import static org.onosproject.yangutils.utils.UtilConstants.PRIVATE;
import static org.onosproject.yangutils.utils.UtilConstants.PUBLIC;
import static org.onosproject.yangutils.utils.UtilConstants.REMOVE;
import static org.onosproject.yangutils.utils.UtilConstants.REPLACE;
import static org.onosproject.yangutils.utils.UtilConstants.RETURN;
import static org.onosproject.yangutils.utils.UtilConstants.SELECT_LEAF;
import static org.onosproject.yangutils.utils.UtilConstants.SELECT_LEAF_LIST;
import static org.onosproject.yangutils.utils.UtilConstants.SEMI_COLAN;
import static org.onosproject.yangutils.utils.UtilConstants.SERVICE_METHOD_STRING;
import static org.onosproject.yangutils.utils.UtilConstants.SPACE;
import static org.onosproject.yangutils.utils.UtilConstants.STATIC;
import static org.onosproject.yangutils.utils.UtilConstants.STRING_DATA_TYPE;
import static org.onosproject.yangutils.utils.UtilConstants.SUPER;
import static org.onosproject.yangutils.utils.UtilConstants.TO;
import static org.onosproject.yangutils.utils.io.impl.JavaDocGen.JavaDocType.GETTER_METHOD;
import static org.onosproject.yangutils.utils.io.impl.JavaDocGen.JavaDocType.TYPE_CONSTRUCTOR;
import static org.onosproject.yangutils.utils.io.impl.JavaDocGen.getJavaDoc;
import static org.onosproject.yangutils.utils.io.impl.YangIoUtils.getCapitalCase;
import static org.onosproject.yangutils.utils.io.impl.YangIoUtils.insertDataIntoJavaFile;
import static org.onosproject.yangutils.utils.io.impl.YangIoUtils.trimAtLast;
import static org.onosproject.yangutils.utils.io.impl.YangIoUtils.validateLineLength;

/**
 * Representation of java file generator.
 */
public final class JavaFileGenerator {

    private JavaFileGenerator() {
    }

    /**
     * Returns generated interface file for current node.
     *
     * @param file          file
     * @param imports       imports for the file
     * @param curNode       current YANG node
     * @param isAttrPresent if any attribute is present or not
     * @return interface file
     * @throws IOException when fails to write in file
     */
    public static File generateInterfaceFile(File file, List<String> imports, YangNode curNode,
                                             boolean isAttrPresent)
            throws IOException {

        JavaFileInfo javaFileInfo = ((JavaFileInfoContainer) curNode).getJavaFileInfo();

        String path;
        if (curNode instanceof YangModule) {
            path = javaFileInfo.getPluginConfig().getCodeGenDir() + javaFileInfo.getPackageFilePath();
        } else {
            path = javaFileInfo.getBaseCodeGenPath() + javaFileInfo.getPackageFilePath();
        }

        String className = getCapitalCase(javaFileInfo.getJavaName());

        initiateJavaFileGeneration(file, INTERFACE_MASK, imports, curNode, className);

        if (isAttrPresent) {
            /**
             * Add getter methods to interface file.
             */
            try {
                /**
                 * Getter methods.
                 */
                insertDataIntoJavaFile(file, getDataFromTempFileHandle(GETTER_FOR_INTERFACE_MASK,
                        ((TempJavaCodeFragmentFilesContainer) curNode).getTempJavaCodeFragmentFiles()
                                .getBeanTempFiles(), path));
            } catch (IOException e) {
                throw new IOException("No data found in temporary java code fragment files for " + className
                        + " while interface file generation");
            }
        }
        return validateLineLength(file);
    }

    /**
     * Returns generated builder interface file for current node.
     *
     * @param file          file
     * @param curNode       current YANG node
     * @param isAttrPresent if any attribute is present or not
     * @return builder interface file
     * @throws IOException when fails to write in file
     */
    public static File generateBuilderInterfaceFile(File file, YangNode curNode, boolean isAttrPresent)
            throws IOException {

        JavaFileInfo javaFileInfo = ((JavaFileInfoContainer) curNode).getJavaFileInfo();
        YangPluginConfig pluginConfig = javaFileInfo.getPluginConfig();

        String className = getCapitalCase(javaFileInfo.getJavaName());
        String path;
        if (curNode instanceof YangModule) {
            path = javaFileInfo.getPluginConfig().getCodeGenDir() + javaFileInfo.getPackageFilePath();
        } else {
            path = javaFileInfo.getBaseCodeGenPath() + javaFileInfo.getPackageFilePath();
        }

        initiateJavaFileGeneration(file, BUILDER_INTERFACE_MASK, null, curNode, className);
        List<String> methods = new ArrayList<>();
        if (isAttrPresent) {
            try {
                /**
                 * Getter methods.
                 */
                methods.add(FOUR_SPACE_INDENTATION + getDataFromTempFileHandle(GETTER_FOR_INTERFACE_MASK,
                        ((TempJavaCodeFragmentFilesContainer) curNode).getTempJavaCodeFragmentFiles()
                                .getBeanTempFiles(), path));
                /**
                 * Setter methods.
                 */
                methods.add(NEW_LINE);
                methods.add(FOUR_SPACE_INDENTATION + getDataFromTempFileHandle(SETTER_FOR_INTERFACE_MASK,
                        ((TempJavaCodeFragmentFilesContainer) curNode).getTempJavaCodeFragmentFiles()
                                .getBeanTempFiles(), path));
            } catch (IOException e) {
                throw new IOException("No data found in temporary java code fragment files for " + className
                        + " while builder interface file generation");
            }
        }
        /**
         * Add build method to builder interface file.
         */
        methods.add(
                ((TempJavaCodeFragmentFilesContainer) curNode).getTempJavaCodeFragmentFiles()
                        .addBuildMethodForInterface(pluginConfig));

        /**
         * Add getters and setters in builder interface.
         */
        for (String method : methods) {
            insertDataIntoJavaFile(file, method);
        }

        insertDataIntoJavaFile(file, CLOSE_CURLY_BRACKET + NEW_LINE);
        return validateLineLength(file);
    }

    /**
     * Returns generated builder class file for current node.
     *
     * @param file          file
     * @param curNode       current YANG node
     * @param isAttrPresent if any attribute is present or not
     * @return builder class file
     * @throws IOException when fails to write in file
     */
    public static File generateBuilderClassFile(File file, YangNode curNode,
                                                boolean isAttrPresent) throws IOException {

        JavaFileInfo javaFileInfo = ((JavaFileInfoContainer) curNode).getJavaFileInfo();
        YangPluginConfig pluginConfig = javaFileInfo.getPluginConfig();

        String className = getCapitalCase(javaFileInfo.getJavaName());

        String path;
        if (curNode instanceof YangModule) {
            path = javaFileInfo.getPluginConfig().getCodeGenDir() + javaFileInfo.getPackageFilePath();
        } else {
            path = javaFileInfo.getBaseCodeGenPath() + javaFileInfo.getPackageFilePath();
        }

        initiateJavaFileGeneration(file, BUILDER_CLASS_MASK, null, curNode, className);
        List<String> methods = new ArrayList<>();

        if (isAttrPresent) {
            /**
             * Add attribute strings.
             */
            try {
                insertDataIntoJavaFile(file,
                        NEW_LINE + FOUR_SPACE_INDENTATION + getDataFromTempFileHandle(ATTRIBUTES_MASK,
                                ((TempJavaCodeFragmentFilesContainer) curNode).getTempJavaCodeFragmentFiles()
                                        .getBeanTempFiles(), path));
            } catch (IOException e) {
                throw new IOException("No data found in temporary java code fragment files for " + className
                        + " while builder class file generation");
            }

            try {
                /**
                 * Getter methods.
                 */
                methods.add(getDataFromTempFileHandle(GETTER_FOR_CLASS_MASK,
                        ((TempJavaCodeFragmentFilesContainer) curNode).getTempJavaCodeFragmentFiles()
                                .getBeanTempFiles(), path));
                /**
                 * Setter methods.
                 */
                methods.add(getDataFromTempFileHandle(SETTER_FOR_CLASS_MASK,
                        ((TempJavaCodeFragmentFilesContainer) curNode).getTempJavaCodeFragmentFiles()
                                .getBeanTempFiles(), path));

                insertDataIntoJavaFile(file, NEW_LINE);
            } catch (IOException e) {
                throw new IOException("No data found in temporary java code fragment files for " + className
                        + " while builder class file generation");
            }
        } else {
            insertDataIntoJavaFile(file, NEW_LINE);
        }
        /**
         * Add default constructor and build method impl.
         */
        methods.add(((TempJavaCodeFragmentFilesContainer) curNode).getTempJavaCodeFragmentFiles()
                .addBuildMethodImpl());
        methods.add(((TempJavaCodeFragmentFilesContainer) curNode).getTempJavaCodeFragmentFiles()
                .addDefaultConstructor(PUBLIC, BUILDER, pluginConfig));

        /**
         * Add methods in builder class.
         */
        for (String method : methods) {
            insertDataIntoJavaFile(file, method);
        }
        insertDataIntoJavaFile(file, CLOSE_CURLY_BRACKET);
        return validateLineLength(file);
    }

    /**
     * Returns generated op param builder class file for current node.
     *
     * @param file          file handle
     * @param curNode       current YANG node
     * @param isAttrPresent if any attribute is present or not
     * @return builder class file
     * @throws IOException when fails to write in file
     */
    public static File generateOpParamBuilderClassFile(File file, YangNode curNode,
                                                       boolean isAttrPresent) throws IOException {

        JavaFileInfo javaFileInfo = ((JavaFileInfoContainer) curNode).getJavaFileInfo();
        YangPluginConfig pluginConfig = javaFileInfo.getPluginConfig();

        String className = getCapitalCase(javaFileInfo.getJavaName());

        initiateJavaFileGeneration(file, OPERATION_BUILDER_CLASS_MASK, null, curNode, className);
        List<String> methods = new ArrayList<>();

        if (isAttrPresent) {
            /**
             * Add attribute strings.
             */
            try {
                insertDataIntoJavaFile(file, FOUR_SPACE_INDENTATION + PRIVATE + SPACE +
                        OPERATION_ENUM + SPACE + OP_PARAM_TYPE + SEMI_COLAN + NEW_LINE);

            } catch (IOException e) {
                throw new IOException("No data found in temporary java code fragment files for " + className
                        + " while impl class file generation");
            }

            try {
                if (curNode instanceof YangLeavesHolder) {
                    YangLeavesHolder leavesHolder = (YangLeavesHolder) curNode;
                    List<YangLeaf> leaves = leavesHolder.getListOfLeaf();
                    List<YangLeafList> listOfLeafList = leavesHolder.getListOfLeafList();

                    if (leaves != null && !leaves.isEmpty()) {
                        insertDataIntoJavaFile(file, FOUR_SPACE_INDENTATION + PRIVATE + SPACE +
                                BITSET + SPACE + FILTER_LEAF + SPACE + EQUAL + SPACE +
                                NEW + SPACE + BITSET + OPEN_PARENTHESIS + CLOSE_PARENTHESIS + SEMI_COLAN
                                + NEW_LINE);

                        insertDataIntoJavaFile(file, FOUR_SPACE_INDENTATION + PRIVATE + SPACE +
                                BITSET + SPACE + SELECT_LEAF + SPACE + EQUAL + SPACE +
                                NEW + SPACE + BITSET + OPEN_PARENTHESIS + CLOSE_PARENTHESIS
                                + SEMI_COLAN + NEW_LINE);
                    }

                    if (listOfLeafList != null && !listOfLeafList.isEmpty()) {
                        insertDataIntoJavaFile(file, FOUR_SPACE_INDENTATION + PRIVATE + SPACE +
                                BITSET + SPACE + FILTER_LEAF_LIST + SPACE + EQUAL + SPACE +
                                NEW + SPACE + BITSET + OPEN_PARENTHESIS + CLOSE_PARENTHESIS
                                + SEMI_COLAN + NEW_LINE);

                        insertDataIntoJavaFile(file, FOUR_SPACE_INDENTATION + PRIVATE + SPACE +
                                BITSET + SPACE + SELECT_LEAF_LIST + SPACE + EQUAL + SPACE +
                                NEW + SPACE + BITSET + OPEN_PARENTHESIS + CLOSE_PARENTHESIS
                                + SEMI_COLAN + NEW_LINE);
                    }
                }

            } catch (IOException e) {
                throw new IOException("No data found in temporary java code fragment files for " + className
                        + " while impl class file generation");
            }

            try {
                /**
                 * Setter methods.
                 */
                methods.add(getSetterForLeaf(className, curNode, pluginConfig));
                methods.add(getSetterForLeafList(className, curNode, pluginConfig));

                if (curNode instanceof YangLeavesHolder) {
                    YangLeavesHolder leavesHolder = (YangLeavesHolder) curNode;
                    List<YangLeaf> leaves = leavesHolder.getListOfLeaf();
                    List<YangLeafList> listOfLeafList = leavesHolder.getListOfLeafList();

                    if (leaves != null && !leaves.isEmpty()) {
                        methods.add(getToStringLeafgetter());
                        methods.add(getToStringSelectLeafgetter());
                    }

                    if (listOfLeafList != null && !listOfLeafList.isEmpty()) {
                        methods.add(getToStringLeafListgetter());
                        methods.add(getToStringSelectLeafListgetter());
                    }
                }

                methods.add(getOperationTypegetter());
                methods.add(getOperationTypeSetter());
                insertDataIntoJavaFile(file, NEW_LINE);
            } catch (IOException e) {
                throw new IOException("No data found in temporary java code fragment files for " + className
                        + " while builder class file generation");
            }
        } else {
            insertDataIntoJavaFile(file, NEW_LINE);
        }

        /**
         * Add methods in builder class.
         */
        for (String method : methods) {
            insertDataIntoJavaFile(file, method);
        }
        insertDataIntoJavaFile(file, CLOSE_CURLY_BRACKET);
        return validateLineLength(file);
    }


    /**
     * Returns generated manager class file for current node.
     *
     * @param file    file
     * @param imports imports for the file
     * @param curNode current YANG node
     * @return builder class file
     * @throws IOException when fails to write in file
     */
    public static File generateManagerClassFile(File file, List<String> imports, YangNode curNode)
            throws IOException {

        JavaFileInfo javaFileInfo = ((JavaFileInfoContainer) curNode).getJavaFileInfo();

        String className = getCapitalCase(javaFileInfo.getJavaName()) + MANAGER;
        String path = javaFileInfo.getBaseCodeGenPath() + javaFileInfo.getPackageFilePath();

        initiateJavaFileGeneration(file, GENERATE_SERVICE_AND_MANAGER, imports, curNode, className);

        List<String> methods = new ArrayList<>();

        insertDataIntoJavaFile(file, LOGGER_STATEMENT);
        methods.add(addActivateMethod());
        methods.add(addDeActivateMethod());

        TempJavaServiceFragmentFiles tempJavaServiceFragmentFiles = ((JavaCodeGeneratorInfo) curNode)
                .getTempJavaCodeFragmentFiles().getServiceTempFiles();

        JavaAttributeInfo rootAttribute = getCurNodeAsAttributeInTarget(curNode, curNode, false,
                tempJavaServiceFragmentFiles);
        try {
            /**
             * Getter methods.
             */
            methods.add(getOverRideString() +
                    getGetterForClass(rootAttribute, GENERATE_SERVICE_AND_MANAGER) + NEW_LINE);
            /**
             * Setter methods.
             */
            methods.add(getOverRideString() +
                    getSetterForClass(rootAttribute, className, GENERATE_SERVICE_AND_MANAGER)
                    + NEW_LINE);

            methods.add(getAugmentsDataMethodForManager(curNode) + NEW_LINE);

            if (((JavaCodeGeneratorInfo) curNode).getTempJavaCodeFragmentFiles().getServiceTempFiles() != null) {
                JavaCodeGeneratorInfo javaGeninfo = (JavaCodeGeneratorInfo) curNode;
                /**
                 * Rpc methods
                 */
                methods.add(getDataFromTempFileHandle(RPC_IMPL_MASK,
                        javaGeninfo.getTempJavaCodeFragmentFiles().getServiceTempFiles(), path));
            }
            insertDataIntoJavaFile(file, NEW_LINE);

        } catch (IOException e) {
            throw new IOException("No data found in temporary java code fragment files for " + className
                    + " while manager class file generation");
        }

        /**
         * Add methods in builder class.
         */
        for (String method : methods) {
            insertDataIntoJavaFile(file, method);
        }
        return validateLineLength(file);
    }

    /**
     * Returns generated impl class file for current node.
     *
     * @param file          file
     * @param curNode       current YANG node
     * @param isAttrPresent if any attribute is present or not
     * @param imports       list of imports
     * @return impl class file
     * @throws IOException when fails to write in file
     */
    public static File generateImplClassFile(File file, YangNode curNode, boolean isAttrPresent, List<String> imports)
            throws IOException {

        JavaFileInfo javaFileInfo = ((JavaFileInfoContainer) curNode).getJavaFileInfo();
        YangPluginConfig pluginConfig = javaFileInfo.getPluginConfig();

        String className = getCapitalCase(javaFileInfo.getJavaName());
        String path;
        if (curNode instanceof YangModule) {
            path = javaFileInfo.getPluginConfig().getCodeGenDir() + javaFileInfo.getPackageFilePath();
        } else {
            path = javaFileInfo.getBaseCodeGenPath() + javaFileInfo.getPackageFilePath();
        }

        initiateJavaFileGeneration(file, IMPL_CLASS_MASK, imports, curNode, className);

        List<String> methods = new ArrayList<>();
        if (curNode instanceof YangAugmentableNode) {
            insertDataIntoJavaFile(file, addAugmentationAttribute());
        }
        if (isAttrPresent) {
            /**
             * Add attribute strings.
             */
            try {
                insertDataIntoJavaFile(file,
                        NEW_LINE + FOUR_SPACE_INDENTATION + getDataFromTempFileHandle(ATTRIBUTES_MASK,
                                ((TempJavaCodeFragmentFilesContainer) curNode).getTempJavaCodeFragmentFiles()
                                        .getBeanTempFiles(), path));
            } catch (IOException e) {
                throw new IOException("No data found in temporary java code fragment files for " + className
                        + " while impl class file generation");
            }

            try {
                /**
                 * Getter methods.
                 */
                methods.add(getDataFromTempFileHandle(GETTER_FOR_CLASS_MASK,
                        ((TempJavaCodeFragmentFilesContainer) curNode).getTempJavaCodeFragmentFiles()
                                .getBeanTempFiles(), path));

                /**
                 * Hash code method.
                 */
                methods.add(getHashCodeMethodClose(getHashCodeMethodOpen() +
                        getDataFromTempFileHandle(HASH_CODE_IMPL_MASK,
                                ((TempJavaCodeFragmentFilesContainer) curNode).getTempJavaCodeFragmentFiles()
                                        .getBeanTempFiles(), path).replace(NEW_LINE, EMPTY_STRING)));
                /**
                 * Equals method.
                 */
                methods.add(getEqualsMethodClose(getEqualsMethodOpen(getCapitalCase(DEFAULT) + className)
                        + getDataFromTempFileHandle(EQUALS_IMPL_MASK,
                        ((TempJavaCodeFragmentFilesContainer) curNode).getTempJavaCodeFragmentFiles()
                                .getBeanTempFiles(), path)));
                /**
                 * To string method.
                 */
                methods.add(getToStringMethodOpen() + getDataFromTempFileHandle(TO_STRING_IMPL_MASK,
                        ((TempJavaCodeFragmentFilesContainer) curNode).getTempJavaCodeFragmentFiles()
                                .getBeanTempFiles(), path)
                        + getToStringMethodClose());

            } catch (IOException e) {
                throw new IOException("No data found in temporary java code fragment files for " + className
                        + " while impl class file generation");
            }
        } else {
            insertDataIntoJavaFile(file, NEW_LINE);
        }
        try {

            /**
             * Constructor.
             */
            String constructor = getConstructorStart(className, pluginConfig);
            constructor = constructor + getDataFromTempFileHandle(CONSTRUCTOR_IMPL_MASK,
                    ((TempJavaCodeFragmentFilesContainer) curNode).getTempJavaCodeFragmentFiles()
                            .getBeanTempFiles(), path);

            methods.add(constructor + FOUR_SPACE_INDENTATION + CLOSE_CURLY_BRACKET);
        } catch (IOException e) {
            throw new IOException("No data found in temporary java code fragment files for " + className
                    + " while impl class file generation");
        }

        if (curNode instanceof YangAugmentableNode) {
            methods.add(getAddAugmentInfoMethodImpl());
            methods.add(getAugmentInfoImpl());
            methods.add(getAugmentInfoMapImpl(javaFileInfo.getPluginConfig()));
        }

        /**
         * Add methods in impl class.
         */
        for (String method : methods) {
            insertDataIntoJavaFile(file, method);
        }

        return validateLineLength(file);
    }

    /**
     * Returns generated op param class file for current node.
     *
     * @param file          file handle
     * @param curNode       current YANG node
     * @param isAttrPresent if any attribute is present or not
     * @param imports       import list
     * @return returns generated op param class file for current node
     * @throws IOException when fails to write in file
     */
    public static File generateOpParamImplClassFile(File file, YangNode curNode,
                                                    boolean isAttrPresent, List<String> imports)
            throws IOException {

        JavaFileInfo javaFileInfo = ((JavaFileInfoContainer) curNode).getJavaFileInfo();
        YangPluginConfig pluginConfig = javaFileInfo.getPluginConfig();

        String className = getCapitalCase(javaFileInfo.getJavaName());

        initiateJavaFileGeneration(file, OPERATION_CLASS_MASK, imports, curNode, className);

        List<String> methods = new ArrayList<>();

        if (isAttrPresent) {
            /**
             * Add attribute strings.
             */
            try {
                insertDataIntoJavaFile(file, FOUR_SPACE_INDENTATION + PUBLIC + SPACE + STATIC +
                        SPACE + ENUM + SPACE + OPERATION_ENUM + SPACE + OPEN_CURLY_BRACKET +
                        NEW_LINE + EIGHT_SPACE_INDENTATION + MERGE +
                        NEW_LINE + EIGHT_SPACE_INDENTATION + REPLACE +
                        NEW_LINE + EIGHT_SPACE_INDENTATION + CREATE +
                        NEW_LINE + EIGHT_SPACE_INDENTATION + DELETE +
                        NEW_LINE + EIGHT_SPACE_INDENTATION + REMOVE +
                        NEW_LINE + FOUR_SPACE_INDENTATION + CLOSE_CURLY_BRACKET + NEW_LINE);

            } catch (IOException e) {
                throw new IOException("No data found in temporary java code fragment files for " + className
                        + " while impl class file generation");
            }

            /**
             * Add attribute strings.
             */
            try {
                insertDataIntoJavaFile(file, FOUR_SPACE_INDENTATION + PRIVATE + SPACE +
                        OPERATION_ENUM + SPACE + OP_PARAM_TYPE + SEMI_COLAN + NEW_LINE);

            } catch (IOException e) {
                throw new IOException("No data found in temporary java code fragment files for " + className
                        + " while impl class file generation");
            }

            try {
                if (curNode instanceof YangLeavesHolder) {
                    YangLeavesHolder leavesHolder = (YangLeavesHolder) curNode;
                    List<YangLeaf> leaves = leavesHolder.getListOfLeaf();
                    List<YangLeafList> listOfLeafList = leavesHolder.getListOfLeafList();

                    if (leaves != null && !leaves.isEmpty()) {
                        insertDataIntoJavaFile(file, FOUR_SPACE_INDENTATION + PRIVATE + SPACE +
                                BITSET + SPACE + FILTER_LEAF + SPACE + EQUAL + SPACE +
                                NEW + SPACE + BITSET + OPEN_PARENTHESIS + CLOSE_PARENTHESIS
                                + SEMI_COLAN + NEW_LINE);

                        insertDataIntoJavaFile(file, FOUR_SPACE_INDENTATION + PRIVATE + SPACE +
                                BITSET + SPACE + SELECT_LEAF + SPACE + EQUAL + SPACE +
                                NEW + SPACE + BITSET + OPEN_PARENTHESIS + CLOSE_PARENTHESIS
                                + SEMI_COLAN + NEW_LINE);
                    }

                    if (listOfLeafList != null && !listOfLeafList.isEmpty()) {
                        insertDataIntoJavaFile(file, FOUR_SPACE_INDENTATION + PRIVATE + SPACE +
                                BITSET + SPACE + FILTER_LEAF_LIST + SPACE + EQUAL + SPACE +
                                NEW + SPACE + BITSET + OPEN_PARENTHESIS + CLOSE_PARENTHESIS
                                + SEMI_COLAN + NEW_LINE);

                        insertDataIntoJavaFile(file, FOUR_SPACE_INDENTATION + PRIVATE + SPACE +
                                BITSET + SPACE + SELECT_LEAF_LIST + SPACE + EQUAL + SPACE +
                                NEW + SPACE + BITSET + OPEN_PARENTHESIS + CLOSE_PARENTHESIS
                                + SEMI_COLAN + NEW_LINE);
                    }
                }

            } catch (IOException e) {
                throw new IOException("No data found in temporary java code fragment files for " + className
                        + " while impl class file generation");
            }

            if (curNode instanceof YangLeavesHolder) {
                YangLeavesHolder leavesHolder = (YangLeavesHolder) curNode;
                List<YangLeaf> leaves = leavesHolder.getListOfLeaf();
                List<YangLeafList> listOfLeafList = leavesHolder.getListOfLeafList();

                if (leaves != null && !leaves.isEmpty()) {
                    methods.add(getToStringLeafgetter());
                    methods.add(getToStringSelectLeafgetter());
                }

                if (listOfLeafList != null && !listOfLeafList.isEmpty()) {
                    methods.add(getToStringLeafListgetter());
                    methods.add(getToStringSelectLeafListgetter());
                }
            }

            methods.add(getOperationTypegetter());
            methods.add(getIsFilterContentMatch(className, curNode, pluginConfig));

        } else {
            insertDataIntoJavaFile(file, NEW_LINE);
        }
        String constructor = getOpParamConstructorStart(className, pluginConfig);

        constructor = constructor + EIGHT_SPACE_INDENTATION + SUPER + OPEN_PARENTHESIS
                + BUILDER.toLowerCase() + OBJECT
                + CLOSE_PARENTHESIS + SEMI_COLAN + NEW_LINE;

        if (curNode instanceof YangLeavesHolder) {
            YangLeavesHolder leavesHolder = (YangLeavesHolder) curNode;
            List<YangLeaf> leaves = leavesHolder.getListOfLeaf();
            List<YangLeafList> listOfLeafList = leavesHolder.getListOfLeafList();
            String filterLeaf = "";
            String filterLeafList = "";

            if (leaves != null && !leaves.isEmpty()) {
                filterLeaf = EIGHT_SPACE_INDENTATION + FILTER_LEAF + SPACE + EQUAL + SPACE
                        + BUILDER.toLowerCase() + OBJECT + PERIOD + GET_FILTER_LEAF + OPEN_PARENTHESIS
                        + CLOSE_PARENTHESIS + SEMI_COLAN + NEW_LINE;
            }

            if (listOfLeafList != null && !listOfLeafList.isEmpty()) {
                filterLeafList = EIGHT_SPACE_INDENTATION + FILTER_LEAF_LIST + SPACE + EQUAL + SPACE
                        + BUILDER.toLowerCase() + OBJECT + PERIOD + GET_FILTER_LEAF_LIST + OPEN_PARENTHESIS
                        + CLOSE_PARENTHESIS + SEMI_COLAN + NEW_LINE;
            }

            constructor = constructor + filterLeaf + filterLeafList;
        }

        methods.add(constructor + FOUR_SPACE_INDENTATION + CLOSE_CURLY_BRACKET);

        if (curNode instanceof YangAugment) {
            String clsName = getCapitalCase(DEFAULT) +
                    getCapitalCase(YangJavaModelUtils.getAugmentClassName((YangAugment) curNode, pluginConfig));
            methods.add(getBaseClassMethodImpl(clsName));
        }

        /**
         * Add methods in impl class.
         */
        for (String method : methods) {
            insertDataIntoJavaFile(file, method);
        }


        return validateLineLength(file);
    }

    /**
     * Generates class file for type def.
     *
     * @param file    generated file
     * @param curNode current YANG node
     * @param imports imports for file
     * @return type def class file
     * @throws IOException when fails to generate class file
     */
    public static File generateTypeDefClassFile(File file, YangNode curNode, List<String> imports)
            throws IOException {

        JavaFileInfo javaFileInfo = ((JavaFileInfoContainer) curNode).getJavaFileInfo();
        YangPluginConfig pluginConfig = javaFileInfo.getPluginConfig();

        // import
        String className = getCapitalCase(javaFileInfo.getJavaName());
        String path = javaFileInfo.getBaseCodeGenPath() + javaFileInfo.getPackageFilePath();
        YangTypeDef typeDef = (YangTypeDef) curNode;
        List<YangType<?>> types = typeDef.getTypeList();
        YangType type = types.get(0);
        if (type.getDataType().equals(YangDataTypes.BINARY)) {
            imports.add(IMPORT + JAVA_UTIL_OBJECTS_IMPORT_PKG + PERIOD + JAVA_UTIL_IMPORT_BASE64_CLASS);
        }

        initiateJavaFileGeneration(file, className, GENERATE_TYPEDEF_CLASS, imports, path, pluginConfig);

        List<String> methods = new ArrayList<>();

        /**
         * Add attribute strings.
         */
        try {
            insertDataIntoJavaFile(file,
                    NEW_LINE + FOUR_SPACE_INDENTATION + getDataFromTempFileHandle(ATTRIBUTES_MASK,
                            ((TempJavaCodeFragmentFilesContainer) curNode).getTempJavaCodeFragmentFiles()
                                    .getTypeTempFiles(), path));
        } catch (IOException e) {
            throw new IOException("No data found in temporary java code fragment files for " + className
                    + " while type def class file generation");
        }

        /**
         * Default constructor.
         */
        methods.add(((TempJavaCodeFragmentFilesContainer) curNode).getTempJavaCodeFragmentFiles()
                .addDefaultConstructor(PRIVATE, EMPTY_STRING, pluginConfig));

        try {

            /**
             * Type constructor.
             */
            methods.add(getDataFromTempFileHandle(CONSTRUCTOR_FOR_TYPE_MASK,
                    ((TempJavaCodeFragmentFilesContainer) curNode).getTempJavaCodeFragmentFiles().getTypeTempFiles(),
                    path));

            /**
             * Of method.
             */
            methods.add(getDataFromTempFileHandle(OF_STRING_IMPL_MASK,
                    ((TempJavaCodeFragmentFilesContainer) curNode).getTempJavaCodeFragmentFiles().getTypeTempFiles(),
                    path));

            /**
             * Getter method.
             */
            methods.add(getDataFromTempFileHandle(GETTER_FOR_CLASS_MASK,
                    ((TempJavaCodeFragmentFilesContainer) curNode).getTempJavaCodeFragmentFiles().getTypeTempFiles(),
                    path));

            /**
             * Hash code method.
             */
            methods.add(getHashCodeMethodClose(getHashCodeMethodOpen() +
                    getDataFromTempFileHandle(HASH_CODE_IMPL_MASK,
                            ((TempJavaCodeFragmentFilesContainer) curNode).getTempJavaCodeFragmentFiles()
                                    .getTypeTempFiles(), path)
                            .replace(NEW_LINE, EMPTY_STRING)));

            /**
             * Equals method.
             */
            methods.add(getEqualsMethodClose(getEqualsMethodOpen(className + EMPTY_STRING)
                    + getDataFromTempFileHandle(EQUALS_IMPL_MASK,
                    ((TempJavaCodeFragmentFilesContainer) curNode).getTempJavaCodeFragmentFiles()
                            .getTypeTempFiles(), path)));

            /**
             * To string method.
             */
             if (type.getDataType().equals(YangDataTypes.BINARY)) {
                    JavaQualifiedTypeInfo qualifiedTypeInfo = getQualifiedTypeInfoOfCurNode(curNode,
                                                                                            getCapitalCase("binary"));

                    JavaAttributeInfo attr =  getAttributeInfoForTheData(qualifiedTypeInfo, "binary", null, false,
                                                                         false);
                    String attributeName = attr.getAttributeName();
                    String bitsToStringMethod = MethodsGenerator.getOverRideString() + FOUR_SPACE_INDENTATION + PUBLIC
                            + SPACE + STRING_DATA_TYPE + SPACE + TO + STRING_DATA_TYPE + OPEN_PARENTHESIS
                            + CLOSE_PARENTHESIS + SPACE + OPEN_CURLY_BRACKET + NEW_LINE + EIGHT_SPACE_INDENTATION
                            + RETURN + SPACE + BASE64 + PERIOD + GET_ENCODER + OPEN_PARENTHESIS + CLOSE_PARENTHESIS
                            + PERIOD + ENCODE_TO_STRING + OPEN_PARENTHESIS + attributeName + CLOSE_PARENTHESIS
                            + SEMI_COLAN + NEW_LINE + FOUR_SPACE_INDENTATION + CLOSE_CURLY_BRACKET + NEW_LINE;
                    methods.add(bitsToStringMethod);
             } else if (type.getDataType().equals(YangDataTypes.BITS)) {
                    JavaQualifiedTypeInfo qualifiedTypeInfo = getQualifiedTypeInfoOfCurNode(curNode,
                                                                                            getCapitalCase("bits"));

                    JavaAttributeInfo attr =  getAttributeInfoForTheData(qualifiedTypeInfo, "bits", null, false, false);
                    String attributeName = attr.getAttributeName();
                    String bitsToStringMethod = MethodsGenerator.getOverRideString() + FOUR_SPACE_INDENTATION + PUBLIC
                            + SPACE + STRING_DATA_TYPE + SPACE + TO + STRING_DATA_TYPE + OPEN_PARENTHESIS
                            + CLOSE_PARENTHESIS + SPACE + OPEN_CURLY_BRACKET + NEW_LINE + EIGHT_SPACE_INDENTATION
                            + RETURN + SPACE + attributeName + PERIOD + TO + STRING_DATA_TYPE + OPEN_PARENTHESIS
                            + CLOSE_PARENTHESIS + SEMI_COLAN + NEW_LINE + FOUR_SPACE_INDENTATION + CLOSE_CURLY_BRACKET
                            + NEW_LINE;
                    methods.add(bitsToStringMethod);
            } else {
                methods.add(getToStringMethodOpen() + getDataFromTempFileHandle(TO_STRING_IMPL_MASK,
                    ((TempJavaCodeFragmentFilesContainer) curNode).getTempJavaCodeFragmentFiles().getTypeTempFiles(),
                    path) + getToStringMethodClose());
            }

            JavaCodeGeneratorInfo javaGeninfo = (JavaCodeGeneratorInfo) curNode;
            /**
             * From string method.
             */
            methods.add(getFromStringMethodSignature(className, pluginConfig)
                    + getDataFromTempFileHandle(FROM_STRING_IMPL_MASK, javaGeninfo.getTempJavaCodeFragmentFiles()
                    .getTypeTempFiles(), path)
                    + getFromStringMethodClose());

        } catch (IOException e) {
            throw new IOException("No data found in temporary java code fragment files for " + className
                    + " while type def class file generation");
        }

        for (String method : methods) {
            insertDataIntoJavaFile(file, method);
        }
        insertDataIntoJavaFile(file, CLOSE_CURLY_BRACKET + NEW_LINE);

        return validateLineLength(file);
    }

    /**
     * Generates class file for union type.
     *
     * @param file    generated file
     * @param curNode current YANG node
     * @param imports imports for file
     * @return type def class file
     * @throws IOException when fails to generate class file
     */
    public static File generateUnionClassFile(File file, YangNode curNode, List<String> imports)
            throws IOException {

        JavaFileInfo javaFileInfo = ((JavaFileInfoContainer) curNode).getJavaFileInfo();
        YangPluginConfig pluginConfig = javaFileInfo.getPluginConfig();

        String className = getCapitalCase(javaFileInfo.getJavaName());
        String path = javaFileInfo.getBaseCodeGenPath() + javaFileInfo.getPackageFilePath();

        initiateJavaFileGeneration(file, className, GENERATE_UNION_CLASS, imports, path, pluginConfig);

        List<String> methods = new ArrayList<>();

        /**
         * Add attribute strings.
         */
        try {
            insertDataIntoJavaFile(file,
                    NEW_LINE + FOUR_SPACE_INDENTATION + getDataFromTempFileHandle(ATTRIBUTES_MASK,
                            ((TempJavaCodeFragmentFilesContainer) curNode).getTempJavaCodeFragmentFiles()
                                    .getTypeTempFiles(), path));
        } catch (IOException e) {
            throw new IOException("No data found in temporary java code fragment files for " + className
                    + " while union class file generation");
        }

        /**
         * Default constructor.
         */
        methods.add(((TempJavaCodeFragmentFilesContainer) curNode).getTempJavaCodeFragmentFiles()
                .addDefaultConstructor(PRIVATE, EMPTY_STRING, pluginConfig));

        try {

            /**
             * Type constructor.
             */
            methods.add(getDataFromTempFileHandle(CONSTRUCTOR_FOR_TYPE_MASK,
                    ((TempJavaCodeFragmentFilesContainer) curNode).getTempJavaCodeFragmentFiles().getTypeTempFiles(),
                    path));

            /**
             * Of string method.
             */
            methods.add(getDataFromTempFileHandle(OF_STRING_IMPL_MASK,
                    ((TempJavaCodeFragmentFilesContainer) curNode).getTempJavaCodeFragmentFiles().getTypeTempFiles(),
                    path));

            /**
             * Getter method.
             */
            methods.add(getDataFromTempFileHandle(GETTER_FOR_CLASS_MASK,
                    ((TempJavaCodeFragmentFilesContainer) curNode).getTempJavaCodeFragmentFiles().getTypeTempFiles(),
                    path));

            /**
             * Hash code method.
             */
            methods.add(getHashCodeMethodClose(getHashCodeMethodOpen() +
                    getDataFromTempFileHandle(HASH_CODE_IMPL_MASK,
                            ((TempJavaCodeFragmentFilesContainer) curNode).getTempJavaCodeFragmentFiles()
                                    .getTypeTempFiles(), path)
                            .replace(NEW_LINE, EMPTY_STRING)));

            /**
             * Equals method.
             */
            methods.add(getEqualsMethodClose(getEqualsMethodOpen(className + EMPTY_STRING)
                    + getDataFromTempFileHandle(EQUALS_IMPL_MASK,
                    ((TempJavaCodeFragmentFilesContainer) curNode).getTempJavaCodeFragmentFiles()
                            .getTypeTempFiles(), path)));

            /**
             * To string method.
             */
            methods.add(getToStringMethodOpen() + getOmitNullValueString() +
                    getDataFromTempFileHandle(TO_STRING_IMPL_MASK,
                            ((TempJavaCodeFragmentFilesContainer) curNode).getTempJavaCodeFragmentFiles()
                                    .getTypeTempFiles(), path)
                    + getToStringMethodClose());

            /**
             * From string method.
             */
            methods.add(getFromStringMethodSignature(className, pluginConfig)
                    + getDataFromTempFileHandle(FROM_STRING_IMPL_MASK,
                    ((TempJavaCodeFragmentFilesContainer) curNode).getTempJavaCodeFragmentFiles()
                            .getTypeTempFiles(), path)
                    + getFromStringMethodClose());

        } catch (IOException e) {
            throw new IOException("No data found in temporary java code fragment files for " + className
                    + " while union class file generation");
        }

        for (String method : methods) {
            insertDataIntoJavaFile(file, method);
        }
        insertDataIntoJavaFile(file, CLOSE_CURLY_BRACKET + NEW_LINE);

        return validateLineLength(file);
    }

    /**
     * Generates class file for type enum.
     *
     * @param file    generated file
     * @param curNode current YANG node
     * @return class file for type enum
     * @throws IOException when fails to generate class file
     */
    public static File generateEnumClassFile(File file, YangNode curNode)
            throws IOException {

        JavaFileInfo javaFileInfo = ((JavaFileInfoContainer) curNode).getJavaFileInfo();
        YangPluginConfig pluginConfig = javaFileInfo.getPluginConfig();

        String className = javaFileInfo.getJavaName();
        String path = javaFileInfo.getBaseCodeGenPath() + javaFileInfo.getPackageFilePath();

        initiateJavaFileGeneration(file, getCapitalCase(className), GENERATE_ENUM_CLASS, null, path, pluginConfig);
        /**
         * Add attribute strings.
         */
        try {
            JavaCodeGeneratorInfo javaGeninfo = (JavaCodeGeneratorInfo) curNode;
            insertDataIntoJavaFile(file,
                    trimAtLast(trimAtLast(getDataFromTempFileHandle(ENUM_IMPL_MASK, javaGeninfo
                            .getTempJavaCodeFragmentFiles().getEnumerationTempFiles(), path), COMMA), NEW_LINE)
                            + SEMI_COLAN + NEW_LINE);
        } catch (IOException e) {
            throw new IOException("No data found in temporary java code fragment files for " + getCapitalCase(className)
                    + " while enum class file generation");
        }

        /**
         * Add an
         * attribute to get the enum's values.
         */
        insertDataIntoJavaFile(file, getEnumsValueAttribute(getCapitalCase(className)));

        /**
         * Add a constructor for enum.
         */
        insertDataIntoJavaFile(file, getJavaDoc(TYPE_CONSTRUCTOR, className, false, pluginConfig)
                + getEnumsConstructor(getCapitalCase(className)) + NEW_LINE);

        TempJavaEnumerationFragmentFiles enumFragFiles = ((TempJavaCodeFragmentFilesContainer) curNode)
                .getTempJavaCodeFragmentFiles()
                .getEnumerationTempFiles();
        insertDataIntoJavaFile(file, getEnumsOfMethod(className,
                enumFragFiles.getJavaAttributeForEnum(pluginConfig),
                enumFragFiles.getEnumSetJavaMap(),
                enumFragFiles.getEnumStringList(), pluginConfig)
                + NEW_LINE);

        /**
         * Add a getter method for enum.
         */
        insertDataIntoJavaFile(file, getJavaDoc(GETTER_METHOD, className, false, pluginConfig)
                + getGetter(INT, className, GENERATE_ENUM_CLASS) + NEW_LINE);

        try {
            insertDataIntoJavaFile(file, getFromStringMethodSignature(getCapitalCase(className), pluginConfig)
                    + getDataFromTempFileHandle(FROM_STRING_IMPL_MASK,
                    ((TempJavaCodeFragmentFilesContainer) curNode).getTempJavaCodeFragmentFiles()
                            .getEnumerationTempFiles(), path)
                    + getFromStringMethodClose());
        } catch (IOException e) {
            throw new IOException("No data found in temporary java code fragment files for " +
                    getCapitalCase(className) + " while enum class file generation");
        }

        insertDataIntoJavaFile(file, CLOSE_CURLY_BRACKET + NEW_LINE);

        return validateLineLength(file);
    }

    /**
     * Generates interface file for rpc.
     *
     * @param file               generated file
     * @param curNode            current YANG node
     * @param imports            imports for file
     * @return rpc class file
     * @throws IOException when fails to generate class file
     */
    public static File generateServiceInterfaceFile(File file, YangNode curNode, List<String> imports)
            throws IOException {

        JavaFileInfo javaFileInfo = ((JavaFileInfoContainer) curNode).getJavaFileInfo();

        TempJavaServiceFragmentFiles tempJavaServiceFragmentFiles = ((JavaCodeGeneratorInfo) curNode)
                .getTempJavaCodeFragmentFiles().getServiceTempFiles();
        String className = getCapitalCase(javaFileInfo.getJavaName()) + SERVICE_METHOD_STRING;
        String path = javaFileInfo.getBaseCodeGenPath() + javaFileInfo.getPackageFilePath();
        initiateJavaFileGeneration(file, GENERATE_SERVICE_AND_MANAGER, imports, curNode, className);

        List<String> methods = new ArrayList<>();
        JavaAttributeInfo rootAttribute = getCurNodeAsAttributeInTarget(curNode, curNode, false,
                tempJavaServiceFragmentFiles);

        try {
            /**
             * Getter methods.
             */
            methods.add(getGetterString(rootAttribute, GENERATE_SERVICE_AND_MANAGER,
                    javaFileInfo.getPluginConfig()) + NEW_LINE);
            /**
             * Setter methods.
             */
            methods.add(getSetterString(rootAttribute, className, GENERATE_SERVICE_AND_MANAGER,
                    javaFileInfo.getPluginConfig()) + NEW_LINE);

            methods.add(getAugmentsDataMethodForService(curNode) + NEW_LINE);

            if (((JavaCodeGeneratorInfo) curNode).getTempJavaCodeFragmentFiles().getServiceTempFiles() != null) {
                JavaCodeGeneratorInfo javaGeninfo = (JavaCodeGeneratorInfo) curNode;
                /**
                 * Rpc methods
                 */
                methods.add(getDataFromTempFileHandle(RPC_INTERFACE_MASK,
                        javaGeninfo.getTempJavaCodeFragmentFiles().getServiceTempFiles(), path));
            }
        } catch (IOException e) {
            throw new IOException("No data found in temporary java code fragment files for " + className
                    + " while rpc class file generation");
        }

        for (String method : methods) {
            insertDataIntoJavaFile(file, method);
        }
        insertDataIntoJavaFile(file, CLOSE_CURLY_BRACKET + NEW_LINE);

        return validateLineLength(file);
    }

    /**
     * Generates event file.
     *
     * @param file    generated file
     * @param curNode current YANG node
     * @param imports imports for file
     * @throws IOException when fails to generate class file
     */
    public static void generateEventFile(File file, YangNode curNode, List<String> imports) throws IOException {

        String className = getCapitalCase(((JavaFileInfoContainer) curNode).getJavaFileInfo().getJavaName())
                + EVENT_STRING;

        TempJavaEventFragmentFiles tempFiles = ((TempJavaCodeFragmentFilesContainer) curNode)
                .getTempJavaCodeFragmentFiles().getEventFragmentFiles();

        String path = ((JavaFileInfoContainer) curNode).getJavaFileInfo().getBaseCodeGenPath()
                + ((JavaFileInfoContainer) curNode).getJavaFileInfo().getPackageFilePath();
        initiateJavaFileGeneration(file, GENERATE_EVENT_CLASS, imports, curNode, className);
        try {
            insertDataIntoJavaFile(file, NEW_LINE + getEventEnumTypeStart() +
                    trimAtLast(getDataFromTempFileHandle(EVENT_ENUM_MASK, tempFiles, path), COMMA)
                    + FOUR_SPACE_INDENTATION + CLOSE_CURLY_BRACKET + NEW_LINE);

            insertDataIntoJavaFile(file, getDataFromTempFileHandle(EVENT_METHOD_MASK, tempFiles, path));

        } catch (IOException e) {
            throw new IOException("No data found in temporary java code fragment files for " + className
                    + " while event class file generation");
        }

        insertDataIntoJavaFile(file, CLOSE_CURLY_BRACKET + NEW_LINE);
        validateLineLength(file);
    }

    /**
     * Generates event listener file.
     *
     * @param file    generated file
     * @param curNode current YANG node
     * @param imports imports for file
     * @throws IOException when fails to generate class file
     */
    public static void generateEventListenerFile(File file, YangNode curNode, List<String> imports)
            throws IOException {

        String className = getCapitalCase(((JavaFileInfoContainer) curNode).getJavaFileInfo().getJavaName())
                + EVENT_LISTENER_STRING;

        initiateJavaFileGeneration(file, GENERATE_EVENT_LISTENER_INTERFACE, imports, curNode, className);
        insertDataIntoJavaFile(file, CLOSE_CURLY_BRACKET + NEW_LINE);
        validateLineLength(file);
    }

    /**
     * Generates event subject's file.
     *
     * @param file    file handle
     * @param curNode current YANG node
     * @throws IOException when fails to do IO exceptions
     */
    public static void generateEventSubjectFile(File file, YangNode curNode)
            throws IOException {

        String className = getCapitalCase(((JavaFileInfoContainer) curNode).getJavaFileInfo().getJavaName())
                + EVENT_SUBJECT_NAME_SUFFIX;

        initiateJavaFileGeneration(file, GENERATE_EVENT_SUBJECT_CLASS, null, curNode, className);

        String path = ((JavaFileInfoContainer) curNode).getJavaFileInfo().getBaseCodeGenPath()
                + ((JavaFileInfoContainer) curNode).getJavaFileInfo().getPackageFilePath();

        TempJavaEventFragmentFiles tempFiles = ((TempJavaCodeFragmentFilesContainer) curNode)
                .getTempJavaCodeFragmentFiles().getEventFragmentFiles();

        insertDataIntoJavaFile(file, NEW_LINE);
        try {
            insertDataIntoJavaFile(file, getDataFromTempFileHandle(EVENT_SUBJECT_ATTRIBUTE_MASK, tempFiles, path));

            insertDataIntoJavaFile(file, getDataFromTempFileHandle(EVENT_SUBJECT_GETTER_MASK, tempFiles, path));

            insertDataIntoJavaFile(file, getDataFromTempFileHandle(EVENT_SUBJECT_SETTER_MASK, tempFiles, path));

        } catch (IOException e) {
            throw new IOException("No data found in temporary java code fragment files for " + className
                    + " while event class file generation");
        }

        insertDataIntoJavaFile(file, CLOSE_CURLY_BRACKET + NEW_LINE);
        validateLineLength(file);
    }
}

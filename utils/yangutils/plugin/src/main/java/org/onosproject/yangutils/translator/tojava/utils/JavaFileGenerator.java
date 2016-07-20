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

import org.onosproject.yangutils.datamodel.YangNode;
import org.onosproject.yangutils.translator.tojava.JavaFileInfo;
import org.onosproject.yangutils.translator.tojava.JavaFileInfoContainer;
import org.onosproject.yangutils.translator.tojava.TempJavaCodeFragmentFiles;
import org.onosproject.yangutils.translator.tojava.TempJavaCodeFragmentFilesContainer;
import org.onosproject.yangutils.translator.tojava.TempJavaEnumerationFragmentFiles;
import org.onosproject.yangutils.translator.tojava.TempJavaServiceFragmentFiles;
import org.onosproject.yangutils.translator.tojava.JavaCodeGeneratorInfo;
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
import static org.onosproject.yangutils.translator.tojava.utils.JavaCodeSnippetGen.getAugmentedInfoAttribute;
import static org.onosproject.yangutils.translator.tojava.utils.JavaFileGeneratorUtils.getDataFromTempFileHandle;
import static org.onosproject.yangutils.translator.tojava.utils.JavaFileGeneratorUtils.getEnumsValueAttribute;
import static org.onosproject.yangutils.translator.tojava.utils.JavaFileGeneratorUtils.initiateJavaFileGeneration;
import static org.onosproject.yangutils.utils.io.impl.YangIoUtils.getCapitalCase;
import static org.onosproject.yangutils.translator.tojava.utils.MethodsGenerator.addActivateMethod;
import static org.onosproject.yangutils.translator.tojava.utils.MethodsGenerator.addDeActivateMethod;
import static org.onosproject.yangutils.translator.tojava.utils.MethodsGenerator.getAddAugmentInfoMethodImpl;
import static org.onosproject.yangutils.translator.tojava.utils.MethodsGenerator.getAugmentInfoListImpl;
import static org.onosproject.yangutils.translator.tojava.utils.MethodsGenerator.getConstructorStart;
import static org.onosproject.yangutils.translator.tojava.utils.MethodsGenerator.getEnumsConstrcutor;
import static org.onosproject.yangutils.translator.tojava.utils.MethodsGenerator.getEnumsOfMethod;
import static org.onosproject.yangutils.translator.tojava.utils.MethodsGenerator.getEqualsMethodClose;
import static org.onosproject.yangutils.translator.tojava.utils.MethodsGenerator.getEqualsMethodOpen;
import static org.onosproject.yangutils.translator.tojava.utils.MethodsGenerator.getFromStringMethodClose;
import static org.onosproject.yangutils.translator.tojava.utils.MethodsGenerator.getFromStringMethodSignature;
import static org.onosproject.yangutils.translator.tojava.utils.MethodsGenerator.getGetter;
import static org.onosproject.yangutils.translator.tojava.utils.MethodsGenerator.getHashCodeMethodClose;
import static org.onosproject.yangutils.translator.tojava.utils.MethodsGenerator.getHashCodeMethodOpen;
import static org.onosproject.yangutils.translator.tojava.utils.MethodsGenerator.getOmitNullValueString;
import static org.onosproject.yangutils.translator.tojava.utils.MethodsGenerator.getRemoveAugmentationImpl;
import static org.onosproject.yangutils.translator.tojava.utils.MethodsGenerator.getToStringMethodClose;
import static org.onosproject.yangutils.translator.tojava.utils.MethodsGenerator.getToStringMethodOpen;
import static org.onosproject.yangutils.translator.tojava.utils.TempJavaCodeFragmentFilesUtils.getEventEnumTypeStart;
import static org.onosproject.yangutils.translator.tojava.utils.TempJavaCodeFragmentFilesUtils.isAugmentationHolderExtended;
import static org.onosproject.yangutils.utils.UtilConstants.BUILDER;
import static org.onosproject.yangutils.utils.UtilConstants.CLOSE_CURLY_BRACKET;
import static org.onosproject.yangutils.utils.UtilConstants.COMMA;
import static org.onosproject.yangutils.utils.UtilConstants.EMPTY_STRING;
import static org.onosproject.yangutils.utils.UtilConstants.EVENT_LISTENER_STRING;
import static org.onosproject.yangutils.utils.UtilConstants.EVENT_STRING;
import static org.onosproject.yangutils.utils.UtilConstants.EVENT_SUBJECT_NAME_SUFFIX;
import static org.onosproject.yangutils.utils.UtilConstants.FOUR_SPACE_INDENTATION;
import static org.onosproject.yangutils.utils.UtilConstants.IMPL;
import static org.onosproject.yangutils.utils.UtilConstants.INT;
import static org.onosproject.yangutils.utils.UtilConstants.LOGGER_STATEMENT;
import static org.onosproject.yangutils.utils.UtilConstants.MANAGER;
import static org.onosproject.yangutils.utils.UtilConstants.NEW_LINE;
import static org.onosproject.yangutils.utils.UtilConstants.PRIVATE;
import static org.onosproject.yangutils.utils.UtilConstants.PUBLIC;
import static org.onosproject.yangutils.utils.UtilConstants.SEMI_COLAN;
import static org.onosproject.yangutils.utils.UtilConstants.SERVICE_METHOD_STRING;
import static org.onosproject.yangutils.utils.io.impl.JavaDocGen.getJavaDoc;
import static org.onosproject.yangutils.utils.io.impl.JavaDocGen.JavaDocType.GETTER_METHOD;
import static org.onosproject.yangutils.utils.io.impl.JavaDocGen.JavaDocType.TYPE_CONSTRUCTOR;
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
     * @param file file
     * @param imports imports for the file
     * @param curNode current YANG node
     * @param isAttrPresent if any attribute is present or not
     * @return interface file
     * @throws IOException when fails to write in file
     */
    public static File generateInterfaceFile(File file, List<String> imports, YangNode curNode,
            boolean isAttrPresent)
            throws IOException {

        JavaFileInfo javaFileInfo = ((JavaFileInfoContainer) curNode).getJavaFileInfo();

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
                                .getBeanTempFiles()));
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
     * @param file file
     * @param curNode current YANG node
     * @param isAttrPresent if any attribute is present or not
     * @return builder interface file
     * @throws IOException when fails to write in file
     */
    public static File generateBuilderInterfaceFile(File file, YangNode curNode, boolean isAttrPresent)
            throws IOException {

        JavaFileInfo javaFileInfo = ((JavaFileInfoContainer) curNode).getJavaFileInfo();
        YangPluginConfig pluginConfig = javaFileInfo.getPluginConfig();

        String className = getCapitalCase(javaFileInfo.getJavaName());
        String path = javaFileInfo.getBaseCodeGenPath() + javaFileInfo.getPackageFilePath();

        initiateJavaFileGeneration(file, className, BUILDER_INTERFACE_MASK, null, path, pluginConfig);
        List<String> methods = new ArrayList<>();
        if (isAttrPresent) {
            try {
                /**
                 * Getter methods.
                 */
                methods.add(FOUR_SPACE_INDENTATION + getDataFromTempFileHandle(GETTER_FOR_INTERFACE_MASK,
                        ((TempJavaCodeFragmentFilesContainer) curNode).getTempJavaCodeFragmentFiles()
                                .getBeanTempFiles()));
                /**
                 * Setter methods.
                 */
                methods.add(NEW_LINE);
                methods.add(FOUR_SPACE_INDENTATION + getDataFromTempFileHandle(SETTER_FOR_INTERFACE_MASK,
                        ((TempJavaCodeFragmentFilesContainer) curNode).getTempJavaCodeFragmentFiles()
                                .getBeanTempFiles()));
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
     * @param file file
     * @param imports imports for the file
     * @param curNode current YANG node
     * @param isAttrPresent if any attribute is present or not
     * @return builder class file
     * @throws IOException when fails to write in file
     */
    public static File generateBuilderClassFile(File file, List<String> imports, YangNode curNode,
            boolean isAttrPresent)
            throws IOException {

        JavaFileInfo javaFileInfo = ((JavaFileInfoContainer) curNode).getJavaFileInfo();
        YangPluginConfig pluginConfig = javaFileInfo.getPluginConfig();

        String className = getCapitalCase(javaFileInfo.getJavaName());
        String path = javaFileInfo.getBaseCodeGenPath() + javaFileInfo.getPackageFilePath();

        initiateJavaFileGeneration(file, className, BUILDER_CLASS_MASK, imports, path, pluginConfig);

        List<String> methods = new ArrayList<>();

        if (isAttrPresent) {
            /**
             * Add attribute strings.
             */
            try {
                insertDataIntoJavaFile(file,
                        NEW_LINE + FOUR_SPACE_INDENTATION + getDataFromTempFileHandle(ATTRIBUTES_MASK,
                                ((TempJavaCodeFragmentFilesContainer) curNode).getTempJavaCodeFragmentFiles()
                                        .getBeanTempFiles()));
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
                                .getBeanTempFiles()));
                /**
                 * Setter methods.
                 */
                methods.add(getDataFromTempFileHandle(SETTER_FOR_CLASS_MASK,
                        ((TempJavaCodeFragmentFilesContainer) curNode).getTempJavaCodeFragmentFiles()
                                .getBeanTempFiles()) +
                        NEW_LINE);
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
        return validateLineLength(file);
    }

    /**
     * Returns generated manager class file for current node.
     *
     * @param file file
     * @param imports imports for the file
     * @param curNode current YANG node
     * @param isAttrPresent if any attribute is present or not
     * @return builder class file
     * @throws IOException when fails to write in file
     */
    public static File generateManagerClassFile(File file, List<String> imports, YangNode curNode,
            boolean isAttrPresent)
            throws IOException {

        JavaFileInfo javaFileInfo = ((JavaFileInfoContainer) curNode).getJavaFileInfo();

        String className = getCapitalCase(javaFileInfo.getJavaName()) + MANAGER;

        initiateJavaFileGeneration(file, GENERATE_SERVICE_AND_MANAGER, imports, curNode, className);

        List<String> methods = new ArrayList<>();

        insertDataIntoJavaFile(file, LOGGER_STATEMENT);
        methods.add(addActivateMethod());
        methods.add(addDeActivateMethod());

        try {
            if (isAttrPresent) {
                /**
                 * Getter methods.
                 */
                methods.add(
                        getDataFromTempFileHandle(GETTER_FOR_CLASS_MASK,
                                ((TempJavaCodeFragmentFilesContainer) curNode)
                                        .getTempJavaCodeFragmentFiles().getServiceTempFiles()));
                /**
                 * Setter methods.
                 */
                methods.add(
                        getDataFromTempFileHandle(SETTER_FOR_CLASS_MASK,
                                ((TempJavaCodeFragmentFilesContainer) curNode)
                                        .getTempJavaCodeFragmentFiles().getServiceTempFiles())
                                + NEW_LINE);

            }
            if (((JavaCodeGeneratorInfo) curNode).getTempJavaCodeFragmentFiles().getServiceTempFiles() != null) {
                JavaCodeGeneratorInfo javaGeninfo = (JavaCodeGeneratorInfo) curNode;
                /**
                 * Rpc methods
                 */
                methods.add(getDataFromTempFileHandle(RPC_IMPL_MASK,
                        javaGeninfo.getTempJavaCodeFragmentFiles().getServiceTempFiles()));
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
     * @param file file
     * @param curNode current YANG node
     * @param isAttrPresent if any attribute is present or not
     * @return impl class file
     * @throws IOException when fails to write in file
     */
    public static File generateImplClassFile(File file, YangNode curNode, boolean isAttrPresent)
            throws IOException {

        JavaFileInfo javaFileInfo = ((JavaFileInfoContainer) curNode).getJavaFileInfo();
        YangPluginConfig pluginConfig = javaFileInfo.getPluginConfig();

        String className = getCapitalCase(javaFileInfo.getJavaName());
        String path = javaFileInfo.getBaseCodeGenPath() + javaFileInfo.getPackageFilePath();

        initiateJavaFileGeneration(file, className, IMPL_CLASS_MASK, null, path, pluginConfig);

        List<String> methods = new ArrayList<>();

        TempJavaCodeFragmentFiles javaCodeFragmentFiles = ((TempJavaCodeFragmentFilesContainer) curNode)
                .getTempJavaCodeFragmentFiles();
        boolean isAugmentationHolderExtended = isAugmentationHolderExtended(
                javaCodeFragmentFiles.getBeanTempFiles().getJavaExtendsListHolder().getExtendsList());
        /**
         * Add attribute for augmented info's list.
         */
        if (isAugmentationHolderExtended) {
            insertDataIntoJavaFile(file, getAugmentedInfoAttribute());
        }
        if (isAttrPresent) {
            /**
             * Add attribute strings.
             */
            try {
                insertDataIntoJavaFile(file,
                        NEW_LINE + FOUR_SPACE_INDENTATION + getDataFromTempFileHandle(ATTRIBUTES_MASK,
                                ((TempJavaCodeFragmentFilesContainer) curNode).getTempJavaCodeFragmentFiles()
                                        .getBeanTempFiles()));
            } catch (IOException e) {
                throw new IOException("No data found in temporary java code fragment files for " + className
                        + " while impl class file generation");
            }

            insertDataIntoJavaFile(file, NEW_LINE);
            try {
                /**
                 * Getter methods.
                 */
                methods.add(getDataFromTempFileHandle(GETTER_FOR_CLASS_MASK,
                        ((TempJavaCodeFragmentFilesContainer) curNode).getTempJavaCodeFragmentFiles()
                                .getBeanTempFiles()));

                /**
                 * Hash code method.
                 */
                methods.add(getHashCodeMethodClose(getHashCodeMethodOpen() +
                        getDataFromTempFileHandle(HASH_CODE_IMPL_MASK,
                                ((TempJavaCodeFragmentFilesContainer) curNode).getTempJavaCodeFragmentFiles()
                                        .getBeanTempFiles()).replace(NEW_LINE, EMPTY_STRING)));
                /**
                 * Equals method.
                 */
                methods.add(getEqualsMethodClose(
                        getEqualsMethodOpen(className + IMPL) + getDataFromTempFileHandle(EQUALS_IMPL_MASK,
                                ((TempJavaCodeFragmentFilesContainer) curNode).getTempJavaCodeFragmentFiles()
                                        .getBeanTempFiles())));
                /**
                 * To string method.
                 */
                methods.add(getToStringMethodOpen() + getDataFromTempFileHandle(TO_STRING_IMPL_MASK,
                        ((TempJavaCodeFragmentFilesContainer) curNode).getTempJavaCodeFragmentFiles()
                                .getBeanTempFiles())
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
            String constructor =
                    getConstructorStart(className, pluginConfig) + getDataFromTempFileHandle(CONSTRUCTOR_IMPL_MASK,
                            ((TempJavaCodeFragmentFilesContainer) curNode).getTempJavaCodeFragmentFiles()
                                    .getBeanTempFiles());

            methods.add(constructor + FOUR_SPACE_INDENTATION + CLOSE_CURLY_BRACKET);
        } catch (IOException e) {
            throw new IOException("No data found in temporary java code fragment files for " + className
                    + " while impl class file generation");
        }

        /**
         * Add method for augment info's list.
         */
        if (isAugmentationHolderExtended) {
            methods.add(getAddAugmentInfoMethodImpl());
            methods.add(getAugmentInfoListImpl());
            methods.add(getRemoveAugmentationImpl());
        }

        /**
         * Add methods in impl class.
         */
        for (String method : methods) {
            insertDataIntoJavaFile(file, FOUR_SPACE_INDENTATION + method + NEW_LINE);
        }
        insertDataIntoJavaFile(file, CLOSE_CURLY_BRACKET + NEW_LINE);

        return validateLineLength(file);
    }

    /**
     * Generates class file for type def.
     *
     * @param file generated file
     * @param curNode current YANG node
     * @param imports imports for file
     * @return type def class file
     * @throws IOException when fails to generate class file
     */
    public static File generateTypeDefClassFile(File file, YangNode curNode, List<String> imports)
            throws IOException {

        JavaFileInfo javaFileInfo = ((JavaFileInfoContainer) curNode).getJavaFileInfo();
        YangPluginConfig pluginConfig = javaFileInfo.getPluginConfig();

        String className = getCapitalCase(javaFileInfo.getJavaName());
        String path = javaFileInfo.getBaseCodeGenPath() + javaFileInfo.getPackageFilePath();

        initiateJavaFileGeneration(file, className, GENERATE_TYPEDEF_CLASS, imports, path, pluginConfig);

        List<String> methods = new ArrayList<>();

        /**
         * Add attribute strings.
         */
        try {
            insertDataIntoJavaFile(file,
                    NEW_LINE + FOUR_SPACE_INDENTATION + getDataFromTempFileHandle(ATTRIBUTES_MASK,
                            ((TempJavaCodeFragmentFilesContainer) curNode).getTempJavaCodeFragmentFiles()
                                    .getTypeTempFiles()));
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
                    ((TempJavaCodeFragmentFilesContainer) curNode).getTempJavaCodeFragmentFiles().getTypeTempFiles()));

            /**
             * Of method.
             */
            methods.add(getDataFromTempFileHandle(OF_STRING_IMPL_MASK,
                    ((TempJavaCodeFragmentFilesContainer) curNode).getTempJavaCodeFragmentFiles().getTypeTempFiles()));

            /**
             * Getter method.
             */
            methods.add(getDataFromTempFileHandle(GETTER_FOR_CLASS_MASK,
                    ((TempJavaCodeFragmentFilesContainer) curNode).getTempJavaCodeFragmentFiles().getTypeTempFiles()));

            /**
             * Hash code method.
             */
            methods.add(getHashCodeMethodClose(getHashCodeMethodOpen() +
                    getDataFromTempFileHandle(HASH_CODE_IMPL_MASK,
                            ((TempJavaCodeFragmentFilesContainer) curNode).getTempJavaCodeFragmentFiles()
                                    .getTypeTempFiles())
                                            .replace(NEW_LINE, EMPTY_STRING)));

            /**
             * Equals method.
             */
            methods.add(getEqualsMethodClose(getEqualsMethodOpen(className + EMPTY_STRING)
                    + getDataFromTempFileHandle(EQUALS_IMPL_MASK,
                    ((TempJavaCodeFragmentFilesContainer) curNode).getTempJavaCodeFragmentFiles().getTypeTempFiles())));

            /**
             * To string method.
             */
            methods.add(getToStringMethodOpen() + getDataFromTempFileHandle(TO_STRING_IMPL_MASK,
                    ((TempJavaCodeFragmentFilesContainer) curNode).getTempJavaCodeFragmentFiles().getTypeTempFiles())
                    + getToStringMethodClose());

            JavaCodeGeneratorInfo javaGeninfo = (JavaCodeGeneratorInfo) curNode;
            /**
             * From string method.
             */
            methods.add(getFromStringMethodSignature(className, pluginConfig)
                    + getDataFromTempFileHandle(FROM_STRING_IMPL_MASK, javaGeninfo.getTempJavaCodeFragmentFiles()
                    .getTypeTempFiles()) + getFromStringMethodClose());

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
     * @param file generated file
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
                                    .getTypeTempFiles()));
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
                    ((TempJavaCodeFragmentFilesContainer) curNode).getTempJavaCodeFragmentFiles().getTypeTempFiles()));

            /**
             * Of string method.
             */
            methods.add(getDataFromTempFileHandle(OF_STRING_IMPL_MASK,
                    ((TempJavaCodeFragmentFilesContainer) curNode).getTempJavaCodeFragmentFiles().getTypeTempFiles()));

            /**
             * Getter method.
             */
            methods.add(getDataFromTempFileHandle(GETTER_FOR_CLASS_MASK,
                    ((TempJavaCodeFragmentFilesContainer) curNode).getTempJavaCodeFragmentFiles().getTypeTempFiles()));

            /**
             * Hash code method.
             */
            methods.add(getHashCodeMethodClose(getHashCodeMethodOpen() +
                    getDataFromTempFileHandle(HASH_CODE_IMPL_MASK,
                            ((TempJavaCodeFragmentFilesContainer) curNode).getTempJavaCodeFragmentFiles()
                                    .getTypeTempFiles())
                                            .replace(NEW_LINE, EMPTY_STRING)));

            /**
             * Equals method.
             */
            methods.add(getEqualsMethodClose(getEqualsMethodOpen(className + EMPTY_STRING)
                    + getDataFromTempFileHandle(EQUALS_IMPL_MASK,
                    ((TempJavaCodeFragmentFilesContainer) curNode).getTempJavaCodeFragmentFiles().getTypeTempFiles())));

            /**
             * To string method.
             */
            methods.add(getToStringMethodOpen() + getOmitNullValueString() +
                    getDataFromTempFileHandle(TO_STRING_IMPL_MASK,
                            ((TempJavaCodeFragmentFilesContainer) curNode).getTempJavaCodeFragmentFiles()
                                    .getTypeTempFiles()) + getToStringMethodClose());

            /**
             * From string method.
             */
            methods.add(getFromStringMethodSignature(className, pluginConfig)
                    + getDataFromTempFileHandle(FROM_STRING_IMPL_MASK,
                    ((TempJavaCodeFragmentFilesContainer) curNode).getTempJavaCodeFragmentFiles().getTypeTempFiles())
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
     * @param file generated file
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
                            .getTempJavaCodeFragmentFiles().getEnumerationTempFiles()), COMMA), NEW_LINE)
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
                + getEnumsConstrcutor(getCapitalCase(className)) + NEW_LINE);

        TempJavaEnumerationFragmentFiles enumFragFiles =
                ((TempJavaCodeFragmentFilesContainer) curNode).getTempJavaCodeFragmentFiles()
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
                                    .getEnumerationTempFiles())
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
     * @param file generated file
     * @param curNode current YANG node
     * @param imports imports for file
     * @param isAttributePresent is attribute present
     * @return rpc class file
     * @throws IOException when fails to generate class file
     */
    public static File generateServiceInterfaceFile(File file, YangNode curNode, List<String> imports,
            boolean isAttributePresent)
            throws IOException {

        JavaFileInfo javaFileInfo = ((JavaFileInfoContainer) curNode).getJavaFileInfo();

        String className = getCapitalCase(javaFileInfo.getJavaName()) + SERVICE_METHOD_STRING;
        initiateJavaFileGeneration(file, GENERATE_SERVICE_AND_MANAGER, imports, curNode, className);

        List<String> methods = new ArrayList<>();

        try {
            if (isAttributePresent) {

                /**
                 * Getter methods.
                 */
                methods.add(getDataFromTempFileHandle(GETTER_FOR_INTERFACE_MASK,
                        ((TempJavaCodeFragmentFilesContainer) curNode).getTempJavaCodeFragmentFiles()
                                .getServiceTempFiles()));
                /**
                 * Setter methods.
                 */
                methods.add(getDataFromTempFileHandle(SETTER_FOR_INTERFACE_MASK,
                        ((TempJavaCodeFragmentFilesContainer) curNode).getTempJavaCodeFragmentFiles()
                                .getServiceTempFiles()));
            }
            if (((JavaCodeGeneratorInfo) curNode).getTempJavaCodeFragmentFiles().getServiceTempFiles() != null) {
                JavaCodeGeneratorInfo javaGeninfo = (JavaCodeGeneratorInfo) curNode;
                /**
                 * Rpc methods
                 */
                methods.add(getDataFromTempFileHandle(RPC_INTERFACE_MASK,
                        javaGeninfo.getTempJavaCodeFragmentFiles().getServiceTempFiles()));
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
     * @param file generated file
     * @param curNode current YANG node
     * @param imports imports for file
     * @throws IOException when fails to generate class file
     */
    public static void generateEventFile(File file, YangNode curNode, List<String> imports) throws IOException {

        String className =
                getCapitalCase(((JavaFileInfoContainer) curNode).getJavaFileInfo().getJavaName())
                        + EVENT_STRING;

        TempJavaServiceFragmentFiles tempFiles = ((TempJavaCodeFragmentFilesContainer) curNode)
                .getTempJavaCodeFragmentFiles().getServiceTempFiles();

        initiateJavaFileGeneration(file, GENERATE_EVENT_CLASS, imports, curNode, className);
        try {
            insertDataIntoJavaFile(file, NEW_LINE + getEventEnumTypeStart() +
                    trimAtLast(getDataFromTempFileHandle(EVENT_ENUM_MASK, tempFiles), COMMA)
                    + FOUR_SPACE_INDENTATION + CLOSE_CURLY_BRACKET + NEW_LINE);

            insertDataIntoJavaFile(file, getDataFromTempFileHandle(EVENT_METHOD_MASK, tempFiles));

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
     * @param file generated file
     * @param curNode current YANG node
     * @param imports imports for file
     * @throws IOException when fails to generate class file
     */
    public static void generateEventListenerFile(File file, YangNode curNode, List<String> imports)
            throws IOException {

        String className =
                getCapitalCase(((JavaFileInfoContainer) curNode).getJavaFileInfo().getJavaName())
                        + EVENT_LISTENER_STRING;

        initiateJavaFileGeneration(file, GENERATE_EVENT_LISTENER_INTERFACE, imports, curNode, className);
        insertDataIntoJavaFile(file, CLOSE_CURLY_BRACKET + NEW_LINE);
        validateLineLength(file);
    }

    /**
     * Generates event subject's file.
     *
     * @param file file handle
     * @param curNode current YANG node
     * @throws IOException when fails to do IO exceptions
     */
    public static void generateEventSubjectFile(File file, YangNode curNode)
            throws IOException {

        String className = getCapitalCase(((JavaFileInfoContainer) curNode).getJavaFileInfo().getJavaName())
                + EVENT_SUBJECT_NAME_SUFFIX;

        initiateJavaFileGeneration(file, GENERATE_EVENT_SUBJECT_CLASS, null, curNode, className);

        TempJavaServiceFragmentFiles tempFiles = ((TempJavaCodeFragmentFilesContainer) curNode)
                .getTempJavaCodeFragmentFiles().getServiceTempFiles();

        insertDataIntoJavaFile(file, NEW_LINE);
        try {
            insertDataIntoJavaFile(file, getDataFromTempFileHandle(EVENT_SUBJECT_ATTRIBUTE_MASK, tempFiles));

            insertDataIntoJavaFile(file, getDataFromTempFileHandle(EVENT_SUBJECT_GETTER_MASK, tempFiles));

            insertDataIntoJavaFile(file, getDataFromTempFileHandle(EVENT_SUBJECT_SETTER_MASK, tempFiles));

        } catch (IOException e) {
            throw new IOException("No data found in temporary java code fragment files for " + className
                    + " while event class file generation");
        }

        insertDataIntoJavaFile(file, CLOSE_CURLY_BRACKET + NEW_LINE);
        validateLineLength(file);
    }
}

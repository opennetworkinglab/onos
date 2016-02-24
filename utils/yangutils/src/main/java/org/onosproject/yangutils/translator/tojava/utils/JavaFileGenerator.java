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

package org.onosproject.yangutils.translator.tojava.utils;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import org.onosproject.yangutils.translator.GeneratedFileType;
import org.onosproject.yangutils.translator.tojava.AttributeInfo;
import org.onosproject.yangutils.utils.UtilConstants;
import org.onosproject.yangutils.utils.io.impl.CopyrightHeader;
import org.onosproject.yangutils.utils.io.impl.FileSystemUtil;
import org.onosproject.yangutils.utils.io.impl.JavaDocGen;
import org.onosproject.yangutils.utils.io.impl.TempDataStore;
import org.onosproject.yangutils.utils.io.impl.JavaDocGen.JavaDocType;
import org.onosproject.yangutils.utils.io.impl.TempDataStore.TempDataStoreType;

import static org.slf4j.LoggerFactory.getLogger;
import org.slf4j.Logger;

public final class JavaFileGenerator {

    private static final Logger log = getLogger(JavaFileGenerator.class);

    /**
     * Default constructor.
     */
    private JavaFileGenerator() {
    }

    /**
     * Returns generated interface file for current node.
     * @param file file
     * @param className class name
     * @param imports imports for the file
     * @param attrList attribute info
     * @param pkg generated file package
     * @return interface file
     * @throws IOException when fails to write in file.
     */
    public static File generateInterfaceFile(File file, String className, List<String> imports,
            List<AttributeInfo> attrList, String pkg) throws IOException {

        initiateFile(file, className, GeneratedFileType.INTERFACE, imports, pkg);
        List<String> methods = getMethodStrings(TempDataStoreType.GETTER_METHODS, GeneratedFileType.INTERFACE,
                className, file, attrList);

        /**
         * Add getter methods to interface file.
         */
        for (String method : methods) {
            appendMethod(file, method);
        }
        return file;
    }

    /**
     * Return generated builder interface file for current node.
     * @param file file
     * @param className class name
     * @param pkg generated file package
     * @param attrList attribute info
     * @return builder interface file
     * @throws IOException when fails to write in file.
     */
    public static File generateBuilderInterfaceFile(File file, String className, String pkg,
            List<AttributeInfo> attrList) throws IOException {

        initiateFile(file, className, GeneratedFileType.BUILDER_INTERFACE, null, pkg);
        List<String> methods = getMethodStrings(TempDataStoreType.BUILDER_INTERFACE_METHODS,
                GeneratedFileType.BUILDER_INTERFACE, className, file, attrList);

        /**
         * Add build method to builder interface file.
         */
        methods.add(MethodsGenerator.parseBuilderInterfaceBuildMethodString(className));

        /**
         * Add getters and setters in builder interface.
         */
        for (String method : methods) {
            appendMethod(file, UtilConstants.FOUR_SPACE_INDENTATION + method + UtilConstants.NEW_LINE);
        }

        insert(file, UtilConstants.CLOSE_CURLY_BRACKET + UtilConstants.NEW_LINE);
        return file;
    }

    /**
     * Returns generated builder class file for current node.
     * @param file file
     * @param className class name
     * @param imports imports for the file
     * @param pkg generated file package
     * @param attrList attribute info
     * @return builder class file
     * @throws IOException when fails to write in file.
     */
    public static File generateBuilderClassFile(File file, String className, List<String> imports, String pkg,
            List<AttributeInfo> attrList) throws IOException {

        initiateFile(file, className, GeneratedFileType.BUILDER_CLASS, imports, pkg);
        List<String> methods = getMethodStrings(TempDataStoreType.BUILDER_METHODS, GeneratedFileType.BUILDER_CLASS,
                className, file, attrList);

        /**
         * Add default constructor and build method impl.
         */
        methods.add(UtilConstants.NEW_LINE + UtilConstants.FOUR_SPACE_INDENTATION + UtilConstants.JAVA_DOC_FIRST_LINE
                + MethodsGenerator.getDefaultConstructorString(GeneratedFileType.BUILDER_CLASS, className));
        methods.add(MethodsGenerator.getBuildString(className));

        /**
         * Add attribute strings.
         */
        addAttributeSring(file, className, attrList, GeneratedFileType.BUILDER_CLASS);

        /**
         * Add methods in builder class.
         */
        for (String method : methods) {
            appendMethod(file, method + UtilConstants.NEW_LINE);
        }
        return file;
    }

    /**
     * Returns generated impl class file for current node.
     * @param file file
     * @param className class name
     * @param pkg generated file package
     * @param attrList attribute's info
     * @return impl class file
     * @throws IOException when fails to write in file.
     */
    public static File generateImplClassFile(File file, String className, String pkg, List<AttributeInfo> attrList)
            throws IOException {

        initiateFile(file, className, GeneratedFileType.IMPL, null, pkg);
        List<String> methods = getMethodStrings(TempDataStoreType.IMPL_METHODS, GeneratedFileType.IMPL, className, file,
                attrList);

        /**
         * Add attributes.
         */
        addAttributeSring(file, className, attrList, GeneratedFileType.IMPL);

        /**
         * Add default constructor and constructor methods.
         */
        methods.add(UtilConstants.JAVA_DOC_FIRST_LINE
                + MethodsGenerator.getDefaultConstructorString(GeneratedFileType.IMPL, className));
        methods.add(MethodsGenerator.getConstructorString(className));

        /**
         * Add methods in impl class.
         */
        for (String method : methods) {
            appendMethod(file, UtilConstants.FOUR_SPACE_INDENTATION + method + UtilConstants.NEW_LINE);
        }
        insert(file, UtilConstants.CLOSE_CURLY_BRACKET + UtilConstants.NEW_LINE);

        return file;
    }

    /**
     * Adds attribute string for generated files.
     *
     * @param className class name
     * @param file generated file
     * @param attrList attribute info
     * @param genFileType generated file type
     * @param IOException when fails to add attributes in files.
     */
    private static void addAttributeSring(File file, String className, List<AttributeInfo> attrList,
            GeneratedFileType genFileType) throws IOException {
        List<String> attributes = new LinkedList<>();
        try {
            attributes = TempDataStore.getTempData(TempDataStoreType.ATTRIBUTE, className);
        } catch (ClassNotFoundException | IOException e) {
            log.info("There is no attribute info of " + className + " YANG file in the serialized files.");
        }

        if (attrList != null) {
            MethodsGenerator.setAttrInfo(attrList);
            for (AttributeInfo attr : attrList) {
                if (attr.isListAttr()) {
                    attr.setAttributeType(AttributesJavaDataType.getListString(attr));
                }
                attributes.add(getAttributeString(attr, genFileType));
            }
        }

        /**
         * Add attributes to the file.
         */
        for (String attribute : attributes) {
            insert(file, UtilConstants.NEW_LINE + UtilConstants.FOUR_SPACE_INDENTATION + attribute);
        }
        insert(file, UtilConstants.NEW_LINE);
    }

    /**
     * Returns method strings for generated files.
     *
     * @param dataStoreType temp data store file type.
     * @param genFileType generated file type
     * @param className generated file name
     * @param attrList attribute info
     * @return method strings
     */
    private static List<String> getMethodStrings(TempDataStoreType dataStoreType, GeneratedFileType genFileType,
            String className, File file, List<AttributeInfo> attrList) {

        List<String> methods = new LinkedList<>();
        try {
            methods = TempDataStore.getTempData(dataStoreType, className);
        } catch (ClassNotFoundException | IOException e) {
            log.info("There is no attribute info of " + className + " YANG file in the serialized files.");
        }

        if (attrList != null) {
            MethodsGenerator.setAttrInfo(attrList);
            for (AttributeInfo attr : attrList) {
                if (attr.isListAttr()) {
                    attr.setAttributeType(AttributesJavaDataType.getListString(attr));
                }
                methods.add(MethodsGenerator.getMethodString(attr, genFileType));
            }
        }
        return methods;
    }

    /**
     * Initiate generation of file based on generated file type.
     *
     * @param file generated file
     * @param className generated file class name
     * @param type generated file type
     * @param imports imports for the file
     * @param pkg generated file package
     * @throws IOException when fails to generate a file
     */
    private static void initiateFile(File file, String className, GeneratedFileType type, List<String> imports,
            String pkg) throws IOException {
        try {
            file.createNewFile();
            appendContents(file, className, type, imports, pkg);
        } catch (IOException e) {
            throw new IOException("Failed to create " + file.getName() + " class file.");
        }
    }

    /**
     * Appends the temp files to main files.
     *
     * @param appendFile temp file
     * @param srcFile main file
     * @throws IOException when fails to append contents.
     */
    public static void appendFileContents(File appendFile, File srcFile) throws IOException {
        try {
            FileSystemUtil.appendFileContents(appendFile, srcFile);
        } catch (IOException e) {
            throw new IOException("Failed to append " + appendFile + " in " + srcFile);
        }
    }

    /**
     * Append methods to the generated files.
     *
     * @param file file in which method needs to be appended.
     * @param method method which needs to be appended.
     */
    private static void appendMethod(File file, String method) throws IOException {
        insert(file, method);
    }

    /**
     * Closes the current generated file.
     *
     * @param fileType generate file type
     * @param yangName file name
     * @return end of class definition string.
     */
    public static String closeFile(GeneratedFileType fileType, String yangName) {
        return JavaCodeSnippetGen.getJavaClassDefClose(fileType, yangName);
    }

    /**
     * Parses attribute info and fetch specific data and creates serialized
     * files of it.
     *
     * @param attr attribute info.
     * @param genFileType generated file type
     * @param className class name
     */
    public static void parseAttributeInfo(AttributeInfo attr, GeneratedFileType genFileType, String className) {

        String attrString = "";
        String methodString = "";
        String getterString = "";

        try {
            /*
             * Serialize attributes.
             */
            attrString = getAttributeString(attr, genFileType);
            attrString = attrString.replace("\"", "");
            TempDataStore.setTempData(attrString, TempDataStore.TempDataStoreType.ATTRIBUTE, className);

            if (genFileType.equals(GeneratedFileType.ALL)) {

                methodString = MethodsGenerator.getMethodString(attr, GeneratedFileType.INTERFACE);
                TempDataStore.setTempData(methodString, TempDataStore.TempDataStoreType.GETTER_METHODS, className);

                methodString = MethodsGenerator.getMethodString(attr, GeneratedFileType.BUILDER_CLASS);
                TempDataStore.setTempData(methodString, TempDataStore.TempDataStoreType.BUILDER_METHODS, className);

                methodString = MethodsGenerator.getMethodString(attr, GeneratedFileType.BUILDER_INTERFACE);
                TempDataStore.setTempData(methodString, TempDataStore.TempDataStoreType.BUILDER_INTERFACE_METHODS,
                        className);

                methodString = MethodsGenerator.getMethodString(attr, GeneratedFileType.IMPL);
                TempDataStore.setTempData(methodString, TempDataStore.TempDataStoreType.IMPL_METHODS, className);

            } else if (genFileType.equals(GeneratedFileType.INTERFACE)) {

                getterString = MethodsGenerator.getGetterString(attr);
                TempDataStore.setTempData(methodString, TempDataStore.TempDataStoreType.GETTER_METHODS, className);
            }
        } catch (IOException e) {
            log.info("Failed to get data for " + attr.getAttributeName() + " from serialized files.");
        }
    }

    /**
     * Returns attribute string.
     *
     * @param attr attribute info
     * @param genFileType generated file type
     * @return attribute string
     */
    private static String getAttributeString(AttributeInfo attr, GeneratedFileType genFileType) {
        return JavaCodeSnippetGen.getJavaAttributeInfo(genFileType, attr.getAttributeName(), attr.getAttributeType());
    }

    /**
     * Appends all the contents into a generated java file.
     *
     * @param file generated file
     * @param fileName generated file name
     * @param type generated file type
     * @param pkg generated file package
     * @throws IOException when fails to append contents.
     */
    private static void appendContents(File file, String fileName, GeneratedFileType type, List<String> importsList,
            String pkg) throws IOException {

        if (type.equals(GeneratedFileType.IMPL)) {

            write(file, fileName, type, JavaDocType.IMPL_CLASS);
        } else if (type.equals(GeneratedFileType.BUILDER_INTERFACE)) {

            write(file, fileName, type, JavaDocType.BUILDER_INTERFACE);
        } else {

            if (type.equals(GeneratedFileType.INTERFACE)) {
                insert(file, CopyrightHeader.getCopyrightHeader());
                insert(file, "package" + UtilConstants.SPACE + pkg + UtilConstants.SEMI_COLAN + UtilConstants.NEW_LINE);
                if (importsList != null) {
                    insert(file, UtilConstants.NEW_LINE);
                    for (String imports : importsList) {
                        insert(file, imports);
                    }
                    insert(file, UtilConstants.NEW_LINE);
                }
                insert(file, UtilConstants.NEW_LINE);
                write(file, fileName, type, JavaDocType.INTERFACE);
            } else if (type.equals(GeneratedFileType.BUILDER_CLASS)) {
                insert(file, CopyrightHeader.getCopyrightHeader());
                insert(file, "package" + UtilConstants.SPACE + pkg + UtilConstants.SEMI_COLAN + UtilConstants.NEW_LINE);
                if (importsList != null) {
                    insert(file, UtilConstants.NEW_LINE);
                    for (String imports : importsList) {
                        insert(file, imports);
                    }
                    insert(file, UtilConstants.NEW_LINE);
                }
                insert(file, UtilConstants.NEW_LINE);
                write(file, fileName, type, JavaDocType.BUILDER_CLASS);
            }
        }
    }

    /**
     * Write data to the specific generated file.
     *
     * @param file generated file
     * @param fileName file name
     * @param genType generated file type
     * @param javaDocType java doc type
     * @throws IOException when fails to write into a file.
     */
    private static void write(File file, String fileName, GeneratedFileType genType, JavaDocGen.JavaDocType javaDocType)
            throws IOException {

        insert(file, JavaDocGen.getJavaDoc(javaDocType, fileName));
        insert(file, JavaCodeSnippetGen.getJavaClassDefStart(genType, fileName));
    }

    /**
     * Insert in the generated file.
     *
     * @param file file in which need to be inserted.
     * @param data data which need to be inserted.
     * @throws IOException when fails to insert into file
     */
    public static void insert(File file, String data) throws IOException {
        try {
            FileSystemUtil.insertStringInFile(file, data);
        } catch (IOException e) {
            throw new IOException("Failed to insert in " + file + "file");
        }
    }

    /**
     * Removes temp files.
     *
     * @param file file to be removed.
     */
    public static void clean(File file) {
        if (file.exists()) {
            file.delete();
        }
    }
}

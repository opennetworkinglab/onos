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
import java.util.List;

import org.onosproject.yangutils.translator.tojava.AttributeInfo;
import org.onosproject.yangutils.utils.UtilConstants;
import org.onosproject.yangutils.utils.io.impl.CopyrightHeader;
import org.onosproject.yangutils.utils.io.impl.FileSystemUtil;
import org.onosproject.yangutils.utils.io.impl.JavaDocGen;
import org.onosproject.yangutils.utils.io.impl.JavaDocGen.JavaDocType;
import org.onosproject.yangutils.utils.io.impl.TempDataStore;
import org.onosproject.yangutils.utils.io.impl.TempDataStore.TempDataStoreType;
import org.slf4j.Logger;

import static org.onosproject.yangutils.translator.GeneratedFileType.BUILDER_CLASS_MASK;
import static org.onosproject.yangutils.translator.GeneratedFileType.BUILDER_INTERFACE_MASK;
import static org.onosproject.yangutils.translator.GeneratedFileType.IMPL_CLASS_MASK;
import static org.onosproject.yangutils.translator.GeneratedFileType.INTERFACE_MASK;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Generates java file.
 */
public final class JavaFileGenerator {

    private static final Logger log = getLogger(JavaFileGenerator.class);

    /**
     * Default constructor.
     */
    private JavaFileGenerator() {
    }

    /**
     * Returns a file object for generated file.
     *
     * @param fileName file name
     * @param filePath file package path
     * @param extension file extension
     * @return file object
     */
    public static File getFileObject(String filePath, String fileName, String extension) {
        return new File(UtilConstants.YANG_GEN_DIR + filePath + File.separator + fileName + extension);
    }

    /**
     * Returns generated interface file for current node.
     *
     * @param file file
     * @param className class name
     * @param imports imports for the file
     * @param attrList attribute info
     * @param pkg generated file package
     * @return interface file
     * @throws IOException when fails to write in file
     */
    public static File generateInterfaceFile(File file, String className, List<String> imports,
            List<AttributeInfo> attrList, String pkg) throws IOException {

        initiateFile(file, className, INTERFACE_MASK, imports, pkg);

        List<String> methods;
        try {
            methods = TempDataStore.getTempData(TempDataStoreType.GETTER_METHODS, className);
        } catch (ClassNotFoundException | IOException e) {
            log.info("There is no attribute info of " + className + " YANG file in the temporary files.");
            throw new IOException("Fail to read data from temp file.");
        }

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
     *
     * @param file file
     * @param className class name
     * @param pkg generated file package
     * @param attrList attribute info
     * @return builder interface file
     * @throws IOException when fails to write in file
     */
    public static File generateBuilderInterfaceFile(File file, String className, String pkg,
            List<AttributeInfo> attrList) throws IOException {

        initiateFile(file, className, BUILDER_INTERFACE_MASK, null, pkg);
        List<String> methods;
        try {
            methods = TempDataStore.getTempData(TempDataStoreType.BUILDER_INTERFACE_METHODS,
                    className + UtilConstants.BUILDER + UtilConstants.INTERFACE);
        } catch (ClassNotFoundException | IOException e) {
            log.info("There is no attribute info of " + className + " YANG file in the temporary files.");
            throw new IOException("Fail to read data from temp file.");
        }

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
     *
     * @param file file
     * @param className class name
     * @param imports imports for the file
     * @param pkg generated file package
     * @param attrList attribute info
     * @return builder class file
     * @throws IOException when fails to write in file
     */
    public static File generateBuilderClassFile(File file, String className, List<String> imports, String pkg,
            List<AttributeInfo> attrList) throws IOException {

        initiateFile(file, className, BUILDER_CLASS_MASK, imports, pkg);

        /**
         * Add attribute strings.
         */
        List<String> attributes;
        try {
            attributes = TempDataStore.getTempData(TempDataStoreType.ATTRIBUTE, className);
        } catch (ClassNotFoundException | IOException e) {
            log.info("There is no attribute info of " + className + " YANG file in the temporary files.");
            throw new IOException("Fail to read data from temp file.");
        }
        /**
         * Add attributes to the file.
         */
        for (String attribute : attributes) {
            insert(file, UtilConstants.NEW_LINE + UtilConstants.FOUR_SPACE_INDENTATION + attribute);
        }
        insert(file, UtilConstants.NEW_LINE);

        List<String> methods;
        try {
            methods = TempDataStore.getTempData(TempDataStoreType.BUILDER_METHODS, className + UtilConstants.BUILDER);
        } catch (ClassNotFoundException | IOException e) {
            log.info("There is no attribute info of " + className + " YANG file in the temporary files.");
            throw new IOException("Fail to read data from temp file.");
        }

        /**
         * Add default constructor and build method impl.
         */
        methods.add(UtilConstants.NEW_LINE + UtilConstants.FOUR_SPACE_INDENTATION + UtilConstants.JAVA_DOC_FIRST_LINE
                + MethodsGenerator.getDefaultConstructorString(BUILDER_CLASS_MASK, className));
        methods.add(MethodsGenerator.getBuildString(className));

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
     *
     * @param file file
     * @param className class name
     * @param pkg generated file package
     * @param attrList attribute's info
     * @return impl class file
     * @throws IOException when fails to write in file
     */
    public static File generateImplClassFile(File file, String className, String pkg, List<AttributeInfo> attrList)
            throws IOException {

        initiateFile(file, className, IMPL_CLASS_MASK, null, pkg);

        List<String> attributes;
        try {
            attributes = TempDataStore.getTempData(TempDataStoreType.ATTRIBUTE, className);
        } catch (ClassNotFoundException | IOException e) {
            log.info("There is no attribute info of " + className + " YANG file in the temporary files.");
            throw new IOException("Fail to read data from temp file.");
        }

        /**
         * Add attributes to the file.
         */
        for (String attribute : attributes) {
            insert(file, UtilConstants.NEW_LINE + UtilConstants.FOUR_SPACE_INDENTATION + attribute);
        }
        insert(file, UtilConstants.NEW_LINE);

        List<String> methods;
        try {
            methods = TempDataStore.getTempData(TempDataStoreType.IMPL_METHODS, className + UtilConstants.IMPL);
        } catch (ClassNotFoundException | IOException e) {
            log.info("There is no attribute info of " + className + " YANG file in the temporary files.");
            throw new IOException("Fail to read data from temp file.");
        }

        /**
         * Add default constructor and constructor methods.
         */
        methods.add(UtilConstants.JAVA_DOC_FIRST_LINE
                + MethodsGenerator.getDefaultConstructorString(IMPL_CLASS_MASK, className));
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
     * Initiate generation of file based on generated file type.
     *
     * @param file generated file
     * @param className generated file class name
     * @param type generated file type
     * @param imports imports for the file
     * @param pkg generated file package
     * @throws IOException when fails to generate a file
     */
    private static void initiateFile(File file, String className, int type, List<String> imports,
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
     * @throws IOException when fails to append contents
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
     * @param file file in which method needs to be appended
     * @param method method which needs to be appended
     * @exception IOException file operation exceptions
     */
    private static void appendMethod(File file, String method) throws IOException {
        insert(file, method);
    }

    /**
     * Closes the current generated file.
     *
     * @param fileType generate file type
     * @param yangName file name
     * @return end of class definition string
     */
    public static String closeFile(int fileType, String yangName) {
        return JavaCodeSnippetGen.getJavaClassDefClose(fileType, yangName);
    }

    /**
     * Parses attribute info and fetch specific data and creates serialized
     * files of it.
     *
     * @param attr attribute info
     * @param genFileType generated file type
     * @param className class name
     */
    public static void parseAttributeInfo(AttributeInfo attr, int genFileType, String className) {

        String attrString = "";
        String builderInterfaceMethodString = "";
        String builderClassMethodString = "";
        String implClassMethodString = "";
        String getterString = "";
        className = JavaIdentifierSyntax.getCaptialCase(className);

        try {
            /*
             * Get the attribute definition and save attributes to temporary
             * file.
             */
            attrString = JavaCodeSnippetGen.getJavaAttributeDefination(attr.getImportInfo().getPkgInfo(),
                    attr.getImportInfo().getClassInfo(),
                    attr.getAttributeName());
            TempDataStore.setTempData(attrString, TempDataStore.TempDataStoreType.ATTRIBUTE, className);

            if ((genFileType & INTERFACE_MASK) != 0) {
                getterString = MethodsGenerator.getGetterString(attr);
                TempDataStore.setTempData(getterString, TempDataStore.TempDataStoreType.GETTER_METHODS, className);
            }

            if ((genFileType & BUILDER_INTERFACE_MASK) != 0) {
                builderInterfaceMethodString = MethodsGenerator.parseBuilderInterfaceMethodString(attr, className);
                TempDataStore.setTempData(builderInterfaceMethodString,
                        TempDataStore.TempDataStoreType.BUILDER_INTERFACE_METHODS,
                        className + UtilConstants.BUILDER + UtilConstants.INTERFACE);
            }

            if ((genFileType & BUILDER_CLASS_MASK) != 0) {
                builderClassMethodString = MethodsGenerator.parseBuilderMethodString(attr, className);
                TempDataStore.setTempData(builderClassMethodString, TempDataStore.TempDataStoreType.BUILDER_METHODS,
                        className + UtilConstants.BUILDER);
            }

            if ((genFileType & IMPL_CLASS_MASK) != 0) {
                implClassMethodString = MethodsGenerator.parseImplMethodString(attr);
                TempDataStore.setTempData(implClassMethodString, TempDataStore.TempDataStoreType.IMPL_METHODS,
                        className + UtilConstants.IMPL);
            }
        } catch (IOException e) {
            log.info("Failed to set data for " + attr.getAttributeName() + " in temp data files.");
        }

    }

    /**
     * Appends all the contents into a generated java file.
     *
     * @param file generated file
     * @param fileName generated file name
     * @param type generated file type
     * @param pkg generated file package
     * @param importsList list of java imports
     * @throws IOException when fails to append contents
     */
    private static void appendContents(File file, String fileName, int type, List<String> importsList,
            String pkg) throws IOException {

        if ((type & IMPL_CLASS_MASK) != 0) {

            write(file, fileName, type, JavaDocType.IMPL_CLASS);
        } else if ((type & BUILDER_INTERFACE_MASK) != 0) {

            write(file, fileName, type, JavaDocType.BUILDER_INTERFACE);
        } else {

            if ((type & INTERFACE_MASK) != 0) {
                insert(file, CopyrightHeader.getCopyrightHeader());
                insert(file, "package" + UtilConstants.SPACE + pkg + UtilConstants.SEMI_COLAN + UtilConstants.NEW_LINE);
                if (importsList != null) {
                    insert(file, UtilConstants.NEW_LINE);
                    for (String imports : importsList) {
                        insert(file, imports);
                    }
                    insert(file, UtilConstants.NEW_LINE);
                }
                write(file, fileName, type, JavaDocType.INTERFACE);
            } else if ((type & BUILDER_CLASS_MASK) != 0) {
                insert(file, CopyrightHeader.getCopyrightHeader());
                insert(file, "package" + UtilConstants.SPACE + pkg + UtilConstants.SEMI_COLAN + UtilConstants.NEW_LINE);
                if (importsList != null) {
                    insert(file, UtilConstants.NEW_LINE);
                    for (String imports : importsList) {
                        insert(file, imports);
                    }
                    insert(file, UtilConstants.NEW_LINE);
                }
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
     * @throws IOException when fails to write into a file
     */
    private static void write(File file, String fileName, int genType, JavaDocGen.JavaDocType javaDocType)
            throws IOException {

        insert(file, JavaDocGen.getJavaDoc(javaDocType, fileName));
        insert(file, JavaCodeSnippetGen.getJavaClassDefStart(genType, fileName));
    }

    /**
     * Insert in the generated file.
     *
     * @param file file in which need to be inserted
     * @param data data which need to be inserted
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
     * @param file file to be removed
     */
    public static void clean(File file) {
        if (file.exists()) {
            file.delete();
        }
    }
}

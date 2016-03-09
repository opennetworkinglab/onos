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

import static org.onosproject.yangutils.translator.GeneratedFileType.BUILDER_CLASS_MASK;
import static org.onosproject.yangutils.translator.GeneratedFileType.BUILDER_INTERFACE_MASK;
import static org.onosproject.yangutils.translator.GeneratedFileType.GENERATE_TYPEDEF_CLASS;
import static org.onosproject.yangutils.translator.GeneratedFileType.IMPL_CLASS_MASK;
import static org.onosproject.yangutils.translator.GeneratedFileType.INTERFACE_MASK;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.onosproject.yangutils.translator.CachedFileHandle;
import org.onosproject.yangutils.translator.tojava.AttributeInfo;
import org.onosproject.yangutils.utils.UtilConstants;
import org.onosproject.yangutils.utils.io.impl.CopyrightHeader;
import org.onosproject.yangutils.utils.io.impl.FileSystemUtil;
import org.onosproject.yangutils.utils.io.impl.JavaDocGen;
import org.onosproject.yangutils.utils.io.impl.JavaDocGen.JavaDocType;
import org.onosproject.yangutils.utils.io.impl.YangIoUtils;
import org.slf4j.Logger;

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
     * @param handle cached file handle
     * @return file object
     */
    public static File getFileObject(String filePath, String fileName, String extension, CachedFileHandle handle) {
        return new File(handle.getCodeGenFilePath() + filePath + File.separator + fileName + extension);
    }

    /**
     * Returns generated interface file for current node.
     *
     * @param file file
     * @param className class name
     * @param imports imports for the file
     * @param attrList attribute info
     * @param pkg generated file package
     * @param handle cached file handle
     * @return interface file
     * @throws IOException when fails to write in file
     */
    public static File generateInterfaceFile(File file, String className, List<String> imports,
            List<AttributeInfo> attrList, String pkg, CachedFileHandle handle) throws IOException {
        String path = handle.getCodeGenFilePath() + pkg.replace(UtilConstants.PERIOD, UtilConstants.SLASH);
        initiateFile(file, className, INTERFACE_MASK, imports, pkg);

        if (!attrList.isEmpty()) {
            List<String> methods = new ArrayList<>();
            try {
                methods.add(handle.getTempData(TempDataStoreTypes.GETTER_METHODS, className, path));
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
     * @param handle cached file handle
     * @return builder interface file
     * @throws IOException when fails to write in file
     */
    public static File generateBuilderInterfaceFile(File file, String className, String pkg,
            List<AttributeInfo> attrList, CachedFileHandle handle) throws IOException {
        String path = handle.getCodeGenFilePath() + pkg.replace(UtilConstants.PERIOD, UtilConstants.SLASH);
        initiateFile(file, className, BUILDER_INTERFACE_MASK, null, pkg);
        List<String> methods = new ArrayList<>();

        if (!attrList.isEmpty()) {

            try {
                methods.add(handle.getTempData(TempDataStoreTypes.GETTER_METHODS, className, path));
                methods.add(handle.getTempData(TempDataStoreTypes.SETTER_METHODS, className, path));
            } catch (ClassNotFoundException | IOException e) {
                log.info("There is no attribute info of " + className + " YANG file in the temporary files.");
                throw new IOException("Fail to read data from temp file.");
            }
        }
        /**
         * Add build method to builder interface file.
         */
        methods.add(MethodsGenerator.parseBuilderInterfaceBuildMethodString(className));

        /**
         * Add getters and setters in builder interface.
         */
        for (String method : methods) {
            appendMethod(file, UtilConstants.FOUR_SPACE_INDENTATION + method);
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
     * @param handle cached file handle
     * @return builder class file
     * @throws IOException when fails to write in file
     */
    public static File generateBuilderClassFile(File file, String className, List<String> imports, String pkg,
            List<AttributeInfo> attrList, CachedFileHandle handle) throws IOException {
        String path = handle.getCodeGenFilePath() + pkg.replace(UtilConstants.PERIOD, UtilConstants.SLASH);
        initiateFile(file, className, BUILDER_CLASS_MASK, imports, pkg);

        List<String> methods = new ArrayList<>();
        if (!attrList.isEmpty()) {
            /**
             * Add attribute strings.
             */
            List<String> attributes = new ArrayList<>();
            try {
                attributes.add(handle.getTempData(TempDataStoreTypes.ATTRIBUTE, className, path));
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

            try {
                methods.add(handle.getTempData(TempDataStoreTypes.GETTER_METHODS_IMPL, className, path));
                methods.add(handle.getTempData(TempDataStoreTypes.SETTER_METHODS_IMPL, className, path));
            } catch (ClassNotFoundException | IOException e) {
                log.info("There is no attribute info of " + className + " YANG file in the temporary files.");
                throw new IOException("Fail to read data from temp file.");
            }

        }
        /**
         * Add default constructor and build method impl.
         */
        methods.add(
                UtilConstants.NEW_LINE + UtilConstants.FOUR_SPACE_INDENTATION + UtilConstants.JAVA_DOC_FIRST_LINE
                        + MethodsGenerator.getDefaultConstructorString(className + UtilConstants.BUILDER,
                                UtilConstants.PUBLIC));
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
     * @param handle cached file handle
     * @return impl class file
     * @throws IOException when fails to write in file
     */
    public static File generateImplClassFile(File file, String className, String pkg, List<AttributeInfo> attrList,
            CachedFileHandle handle)
                    throws IOException {
        String path = handle.getCodeGenFilePath() + pkg.replace(UtilConstants.PERIOD, UtilConstants.SLASH);
        initiateFile(file, className, IMPL_CLASS_MASK, null, path);

        List<String> methods = new ArrayList<>();
        if (!attrList.isEmpty()) {
            List<String> attributes = new ArrayList<>();
            try {
                attributes.add(handle.getTempData(TempDataStoreTypes.ATTRIBUTE, className, path));
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

            try {

                methods.add(handle.getTempData(TempDataStoreTypes.GETTER_METHODS_IMPL, className, path));

                methods.add(MethodsGenerator.getHashCodeMethodClose(MethodsGenerator.getHashCodeMethodOpen()
                        + YangIoUtils
                                .partString(handle.getTempData(TempDataStoreTypes.HASH_CODE, className, path).replace(
                                        UtilConstants.NEW_LINE, ""))));

                methods.add(MethodsGenerator
                        .getEqualsMethodClose(MethodsGenerator.getEqualsMethodOpen(className + UtilConstants.IMPL)
                                + handle.getTempData(TempDataStoreTypes.EQUALS, className, path)));

                methods.add(MethodsGenerator.getToStringMethodOpen()
                        + handle.getTempData(TempDataStoreTypes.TO_STRING, className, path)
                        + MethodsGenerator.getToStringMethodClose());

            } catch (ClassNotFoundException | IOException e) {
                log.info("There is no attribute info of " + className + " YANG file in the temporary files.");
                throw new IOException("Fail to read data from temp file.");
            }

        }

        try {
            methods.add(getConstructorString(className)
                    + handle.getTempData(TempDataStoreTypes.CONSTRUCTOR, className, path)
                    + UtilConstants.FOUR_SPACE_INDENTATION + UtilConstants.CLOSE_CURLY_BRACKET);
        } catch (ClassNotFoundException | IOException e) {
            log.info("There is no attribute info of " + className + " YANG file in the temporary files.");
            throw new IOException("Fail to read data from temp file.");
        }
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
     * Generate class file for type def.
     *
     * @param file generated file
     * @param className file name
     * @param imports imports for file
     * @param pkg package path
     * @param cachedAttributeList attribute list
     * @param handle  cached file handle
     * @return type def class file
     * @throws IOException when fails to generate class file
     */
    public static File generateTypeDefClassFile(File file, String className, List<String> imports,
            String pkg, List<AttributeInfo> cachedAttributeList, CachedFileHandle handle) throws IOException {
        String path = handle.getCodeGenFilePath() + pkg.replace(UtilConstants.PERIOD, UtilConstants.SLASH);
        initiateFile(file, className, GENERATE_TYPEDEF_CLASS, imports, pkg);

        List<String> typeDef = new ArrayList<>();
        try {
            typeDef.add(handle.getTempData(TempDataStoreTypes.TYPE_DEF, className, path));
        } catch (ClassNotFoundException | IOException e) {
            log.info("There is no attribute info of " + className + " YANG file in the temporary files.");
            throw new IOException("Fail to read data from temp file.");
        }

        /**
         * Add attributes to the file.
         */
        for (String attribute : typeDef) {
            insert(file, UtilConstants.NEW_LINE + UtilConstants.FOUR_SPACE_INDENTATION + attribute);
        }

        return file;
    }

    /**
     * Returns constructor string for impl class.
     *
     * @param yangName class name
     * @return constructor string
     */
    private static String getConstructorString(String yangName) {

        String builderAttribute = yangName.substring(0, 1).toLowerCase() + yangName.substring(1);
        String javadoc = MethodsGenerator.getConstructorString(yangName);
        String constructor = UtilConstants.FOUR_SPACE_INDENTATION + UtilConstants.PUBLIC + UtilConstants.SPACE
                + yangName + UtilConstants.IMPL + UtilConstants.OPEN_PARENTHESIS + yangName + UtilConstants.BUILDER
                + UtilConstants.SPACE + builderAttribute + UtilConstants.BUILDER + UtilConstants.OBJECT
                + UtilConstants.CLOSE_PARENTHESIS + UtilConstants.SPACE + UtilConstants.OPEN_CURLY_BRACKET
                + UtilConstants.NEW_LINE;
        return javadoc + constructor;
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
     * @param attr attribute info.
     * @param genFileType generated file type
     * @param className class name
     * @param path file path
     * @param handle cached file handle
     */
    public static void parseAttributeInfo(AttributeInfo attr, int genFileType, String className, String path,
            CachedFileHandle handle) {

        String attrString = "";

        String getterString = "";
        String getterImplString = "";

        String setterString = "";
        String setterImplString = "";

        String constructorString = "";
        String typeDefString = "";

        String toString = "";
        String hashCodeString = "";
        String equalsString = "";

        className = JavaIdentifierSyntax.getCaptialCase(className);

        try {
            /*
             * Get the attribute definition and save attributes to temporary
             * file.
             */

            boolean isList = attr.isListAttr();
            String attributeName = JavaIdentifierSyntax.getLowerCase(attr.getAttributeName());
            if (attr.isQualifiedName()) {
                attrString = JavaCodeSnippetGen.getJavaAttributeDefination(attr.getImportInfo().getPkgInfo(),
                        attr.getImportInfo().getClassInfo(),
                        attributeName, attr.isListAttr());
            } else {
                attrString = JavaCodeSnippetGen.getJavaAttributeDefination(null, attr.getImportInfo().getClassInfo(),
                        attributeName, attr.isListAttr());
            }
            handle.setTempData(attrString + UtilConstants.NEW_LINE + UtilConstants.FOUR_SPACE_INDENTATION,
                    TempDataStoreTypes.ATTRIBUTE, className,
                    path);

            if ((genFileType & INTERFACE_MASK) != 0) {
                getterString = MethodsGenerator.getGetterString(attr);
                handle.setTempData(getterString + UtilConstants.NEW_LINE,
                        TempDataStoreTypes.GETTER_METHODS,
                        className,
                        path);

            }

            if ((genFileType & BUILDER_INTERFACE_MASK) != 0) {
                setterString = MethodsGenerator.getSetterString(attr, className);
                handle.setTempData(setterString + UtilConstants.NEW_LINE,
                        TempDataStoreTypes.SETTER_METHODS,
                        className,
                        path);
            }

            if ((genFileType & BUILDER_CLASS_MASK) != 0) {
                getterImplString = MethodsGenerator.getGetterForClass(attr);
                handle.setTempData(
                        MethodsGenerator.getOverRideString() + getterImplString + UtilConstants.NEW_LINE,
                        TempDataStoreTypes.GETTER_METHODS_IMPL, className,
                        path);
                setterImplString = MethodsGenerator.getSetterForClass(attr, className);
                handle.setTempData(
                        MethodsGenerator.getOverRideString() + setterImplString + UtilConstants.NEW_LINE,
                        TempDataStoreTypes.SETTER_METHODS_IMPL, className,
                        path);
            }

            if ((genFileType & IMPL_CLASS_MASK) != 0) {
                constructorString = MethodsGenerator.getConstructor(className, attr);
                handle.setTempData(constructorString, TempDataStoreTypes.CONSTRUCTOR, className,
                        path);

                hashCodeString = MethodsGenerator.getHashCodeMethod(attr);
                handle.setTempData(hashCodeString + UtilConstants.NEW_LINE,
                        TempDataStoreTypes.HASH_CODE,
                        className,
                        path);
                equalsString = MethodsGenerator.getEqualsMethod(attr);
                handle.setTempData(equalsString + UtilConstants.NEW_LINE,
                        TempDataStoreTypes.EQUALS,
                        className,
                        path);

                toString = MethodsGenerator.getToStringMethod(attr);
                handle.setTempData(toString + UtilConstants.NEW_LINE,
                        TempDataStoreTypes.TO_STRING,
                        className,
                        path);

            }

            if ((genFileType & GENERATE_TYPEDEF_CLASS) != 0) {

                if (attr.isQualifiedName()) {
                    typeDefString = JavaCodeSnippetGen.getJavaAttributeDefination(attr.getImportInfo().getPkgInfo(),
                            attr.getImportInfo().getClassInfo(),
                            attributeName, attr.isListAttr()) + UtilConstants.NEW_LINE;
                } else {
                    typeDefString = JavaCodeSnippetGen.getJavaAttributeDefination(null,
                            attr.getImportInfo().getClassInfo(),
                            attributeName, attr.isListAttr()) + UtilConstants.NEW_LINE;
                }

                typeDefString = typeDefString + UtilConstants.NEW_LINE + UtilConstants.FOUR_SPACE_INDENTATION
                        + UtilConstants.JAVA_DOC_FIRST_LINE;

                typeDefString = typeDefString
                        + MethodsGenerator.getDefaultConstructorString(className, UtilConstants.PRIVATE)
                        + UtilConstants.NEW_LINE;

                typeDefString = typeDefString
                        + JavaDocGen.getJavaDoc(JavaDocType.TYPE_DEF_CONSTRUCTOR, className, isList)
                        + MethodsGenerator.getTypeDefConstructor(attr, className)
                        + UtilConstants.NEW_LINE;

                typeDefString = typeDefString + JavaDocGen.getJavaDoc(JavaDocType.OF, className, isList)
                        + MethodsGenerator.getOfMethod(className, attr) + UtilConstants.NEW_LINE;

                typeDefString = typeDefString + JavaDocGen.getJavaDoc(JavaDocType.GETTER, className, isList)
                        + MethodsGenerator.getGetterForClass(attr) + UtilConstants.NEW_LINE;

                typeDefString = typeDefString + JavaDocGen.getJavaDoc(JavaDocType.TYPE_DEF_SETTER, className, isList)
                        + MethodsGenerator.getSetterForTypeDefClass(attr)
                        + UtilConstants.NEW_LINE;

                hashCodeString = MethodsGenerator.getHashCodeMethodOpen()
                        + YangIoUtils.partString(
                                MethodsGenerator.getHashCodeMethod(attr).replace(UtilConstants.NEW_LINE, ""));
                hashCodeString = MethodsGenerator.getHashCodeMethodClose(hashCodeString) + UtilConstants.NEW_LINE;

                equalsString = MethodsGenerator.getEqualsMethodOpen(className) + UtilConstants.NEW_LINE
                        + MethodsGenerator.getEqualsMethod(attr);
                equalsString = MethodsGenerator.getEqualsMethodClose(equalsString) + UtilConstants.NEW_LINE;

                toString = MethodsGenerator.getToStringMethodOpen()
                        + MethodsGenerator.getToStringMethod(attr) + UtilConstants.NEW_LINE
                        + MethodsGenerator.getToStringMethodClose()
                        + UtilConstants.NEW_LINE;
                typeDefString = typeDefString + hashCodeString + equalsString + toString;
                handle.setTempData(typeDefString, TempDataStoreTypes.TYPE_DEF, className,
                        path);
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
     * @param importsList list of java imports.
     * @throws IOException when fails to append contents
     */
    private static void appendContents(File file, String fileName, int type, List<String> importsList,
            String pkg) throws IOException {

        if (pkg.contains(UtilConstants.YANG_GEN_DIR)) {
            String[] strArray = pkg.split(UtilConstants.YANG_GEN_DIR);
            pkg = strArray[1].replace(UtilConstants.SLASH, UtilConstants.PERIOD);
        }

        if ((type & IMPL_CLASS_MASK) != 0) {

            write(file, fileName, type, JavaDocType.IMPL_CLASS);
        } else if ((type & BUILDER_INTERFACE_MASK) != 0) {

            write(file, fileName, type, JavaDocType.BUILDER_INTERFACE);
        } else if ((type & GENERATE_TYPEDEF_CLASS) != 0) {
            insert(file, CopyrightHeader.getCopyrightHeader());
            insert(file, "package" + UtilConstants.SPACE + pkg + UtilConstants.SEMI_COLAN + UtilConstants.NEW_LINE);
            if (importsList != null) {
                insert(file, UtilConstants.NEW_LINE);
                for (String imports : importsList) {
                    insert(file, imports);
                }
            }
            write(file, fileName, type, JavaDocType.IMPL_CLASS);
        } else {

            if ((type & INTERFACE_MASK) != 0) {
                insert(file, CopyrightHeader.getCopyrightHeader());
                insert(file, "package" + UtilConstants.SPACE + pkg + UtilConstants.SEMI_COLAN + UtilConstants.NEW_LINE);
                if (importsList != null) {
                    insert(file, UtilConstants.NEW_LINE);
                    for (String imports : importsList) {
                        insert(file, imports);
                    }
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

        insert(file, JavaDocGen.getJavaDoc(javaDocType, fileName, false));
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
            FileSystemUtil.updateFileHandle(file, data, false);
        } catch (IOException e) {
            throw new IOException("Failed to insert in " + file + "file");
        }
    }

    /**
     * Closes the files handle for generate files.
     *
     * @param file generate files
     * @throws IOException when failed to close the file handle
     */
    public static void closeFileHandles(File file) throws IOException {
        try {
            FileSystemUtil.updateFileHandle(file, null, true);
        } catch (IOException e) {
            throw new IOException("Failed to close file handle for " + file + "file");
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

    /**
     * Removes temp files.
     *
     * @param tempDir temp directory
     * @throws IOException when fails to delete the directory
     */
    public static void cleanTempFiles(File tempDir) throws IOException {
        FileUtils.deleteDirectory(tempDir);
    }
}

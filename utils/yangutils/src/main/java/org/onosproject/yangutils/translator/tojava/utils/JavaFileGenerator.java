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
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.onosproject.yangutils.datamodel.YangNode;
import org.onosproject.yangutils.translator.tojava.HasJavaFileInfo;
import org.onosproject.yangutils.translator.tojava.HasTempJavaCodeFragmentFiles;
import org.onosproject.yangutils.translator.tojava.JavaFileInfo;
import org.onosproject.yangutils.translator.tojava.TempJavaCodeFragmentFiles;
import org.onosproject.yangutils.utils.UtilConstants;
import org.onosproject.yangutils.utils.io.impl.CopyrightHeader;
import org.onosproject.yangutils.utils.io.impl.FileSystemUtil;
import org.onosproject.yangutils.utils.io.impl.JavaDocGen;
import org.onosproject.yangutils.utils.io.impl.JavaDocGen.JavaDocType;

import static org.onosproject.yangutils.translator.tojava.GeneratedJavaFileType.BUILDER_CLASS_MASK;
import static org.onosproject.yangutils.translator.tojava.GeneratedJavaFileType.BUILDER_INTERFACE_MASK;
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
import static org.onosproject.yangutils.translator.tojava.utils.JavaIdentifierSyntax.getCaptialCase;
import static org.onosproject.yangutils.translator.tojava.utils.MethodsGenerator.getBuildString;
import static org.onosproject.yangutils.translator.tojava.utils.MethodsGenerator.getConstructorStart;
import static org.onosproject.yangutils.translator.tojava.utils.MethodsGenerator.getDefaultConstructorString;
import static org.onosproject.yangutils.translator.tojava.utils.MethodsGenerator.getEqualsMethodClose;
import static org.onosproject.yangutils.translator.tojava.utils.MethodsGenerator.getEqualsMethodOpen;
import static org.onosproject.yangutils.translator.tojava.utils.MethodsGenerator.getHashCodeMethodClose;
import static org.onosproject.yangutils.translator.tojava.utils.MethodsGenerator.getHashCodeMethodOpen;
import static org.onosproject.yangutils.translator.tojava.utils.MethodsGenerator.getToStringMethodClose;
import static org.onosproject.yangutils.translator.tojava.utils.MethodsGenerator.getToStringMethodOpen;
import static org.onosproject.yangutils.translator.tojava.utils.MethodsGenerator.parseBuilderInterfaceBuildMethodString;
import static org.onosproject.yangutils.utils.io.impl.YangIoUtils.partString;

/**
 * Generates java file.
 */
public final class JavaFileGenerator {

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
    public static File getFileObject(String filePath, String fileName, String extension, JavaFileInfo handle) {

        return new File(handle.getBaseCodeGenPath() + filePath + File.separator + fileName + extension);
    }

    /**
     * Returns generated interface file for current node.
     *
     * @param file file
     * @param imports imports for the file
     * @param curNode current YANG node
     * @return interface file
     * @throws IOException when fails to write in file
     */
    public static File generateInterfaceFile(File file, List<String> imports, YangNode curNode) throws IOException {

        JavaFileInfo javaFileInfo = ((HasJavaFileInfo) curNode).getJavaFileInfo();

        String className = getCaptialCase(javaFileInfo.getJavaName());
        String path = javaFileInfo.getBaseCodeGenPath() + javaFileInfo.getPackageFilePath();

        initiateFile(file, className, INTERFACE_MASK, imports, path);

        /**
         * Add getter methods to interface file.
         */
        try {
            appendMethod(file, getDataFromTempFileHandle(GETTER_FOR_INTERFACE_MASK, curNode));
        } catch (IOException e) {
            throw new IOException("No data found in temporary java code fragment files for " + className
                    + " while interface file generation");
        }
        return file;
    }

    /**
     * Return generated builder interface file for current node.
     *
     * @param file file
     * @param curNode current YANG node
     * @return builder interface file
     * @throws IOException when fails to write in file
     */
    public static File generateBuilderInterfaceFile(File file, YangNode curNode) throws IOException {

        JavaFileInfo javaFileInfo = ((HasJavaFileInfo) curNode).getJavaFileInfo();

        String className = getCaptialCase(javaFileInfo.getJavaName());
        String path = javaFileInfo.getBaseCodeGenPath() + javaFileInfo.getPackageFilePath();

        initiateFile(file, className, BUILDER_INTERFACE_MASK, null, path);
        List<String> methods = new ArrayList<>();

        try {
            methods.add(UtilConstants.FOUR_SPACE_INDENTATION
                    + getDataFromTempFileHandle(GETTER_FOR_INTERFACE_MASK, curNode));
            methods.add(UtilConstants.NEW_LINE);
            methods.add(UtilConstants.FOUR_SPACE_INDENTATION
                    + getDataFromTempFileHandle(SETTER_FOR_INTERFACE_MASK, curNode));
        } catch (IOException e) {
            throw new IOException("No data found in temporary java code fragment files for " + className
                    + " while builder interface file generation");
        }

        /**
         * Add build method to builder interface file.
         */
        methods.add(parseBuilderInterfaceBuildMethodString(className));

        /**
         * Add getters and setters in builder interface.
         */
        for (String method : methods) {
            appendMethod(file, method);
        }

        insert(file, UtilConstants.CLOSE_CURLY_BRACKET + UtilConstants.NEW_LINE);
        return file;
    }

    /**
     * Returns generated builder class file for current node.
     *
     * @param file file
     * @param imports imports for the file
     * @param curNode current YANG node
     * @return builder class file
     * @throws IOException when fails to write in file
     */
    public static File generateBuilderClassFile(File file, List<String> imports, YangNode curNode) throws IOException {

        JavaFileInfo javaFileInfo = ((HasJavaFileInfo) curNode).getJavaFileInfo();

        String className = getCaptialCase(javaFileInfo.getJavaName());
        String path = javaFileInfo.getBaseCodeGenPath() + javaFileInfo.getPackageFilePath();

        initiateFile(file, className, BUILDER_CLASS_MASK, imports, path);

        List<String> methods = new ArrayList<>();

        /**
         * Add attribute strings.
         */
        try {
            insert(file, UtilConstants.NEW_LINE + UtilConstants.FOUR_SPACE_INDENTATION
                    + getDataFromTempFileHandle(ATTRIBUTES_MASK, curNode));
        } catch (IOException e) {
            throw new IOException("No data found in temporary java code fragment files for " + className
                    + " while builder class file generation");
        }

        try {
            methods.add(getDataFromTempFileHandle(GETTER_FOR_CLASS_MASK, curNode));
            methods.add(getDataFromTempFileHandle(SETTER_FOR_CLASS_MASK, curNode) + UtilConstants.NEW_LINE);
        } catch (IOException e) {
            throw new IOException("No data found in temporary java code fragment files for " + className
                    + " while builder class file generation");
        }

        /**
         * Add default constructor and build method impl.
         */
        methods.add(getBuildString(className) + UtilConstants.NEW_LINE);
        methods.add(UtilConstants.NEW_LINE
                + getDefaultConstructorString(className + UtilConstants.BUILDER, UtilConstants.PUBLIC));

        /**
         * Add methods in builder class.
         */
        for (String method : methods) {
            appendMethod(file, method);
        }
        return file;
    }

    /**
     * Returns generated impl class file for current node.
     *
     * @param file file
     * @param curNode current YANG node
     * @return impl class file
     * @throws IOException when fails to write in file
     */
    public static File generateImplClassFile(File file, YangNode curNode)
            throws IOException {

        JavaFileInfo javaFileInfo = ((HasJavaFileInfo) curNode).getJavaFileInfo();

        String className = getCaptialCase(javaFileInfo.getJavaName());
        String path = javaFileInfo.getBaseCodeGenPath() + javaFileInfo.getPackageFilePath();

        initiateFile(file, className, IMPL_CLASS_MASK, null, path);

        List<String> methods = new ArrayList<>();

        /**
         * Add attribute strings.
         */
        try {
            insert(file, UtilConstants.NEW_LINE + UtilConstants.FOUR_SPACE_INDENTATION
                    + getDataFromTempFileHandle(ATTRIBUTES_MASK, curNode));
        } catch (IOException e) {
            throw new IOException("No data found in temporary java code fragment files for " + className
                    + " while impl class file generation");
        }

        insert(file, UtilConstants.NEW_LINE);
        try {

            methods.add(getDataFromTempFileHandle(GETTER_FOR_CLASS_MASK, curNode));

            methods.add(getHashCodeMethodClose(getHashCodeMethodOpen() + partString(
                    getDataFromTempFileHandle(HASH_CODE_IMPL_MASK, curNode).replace(UtilConstants.NEW_LINE,
                            UtilConstants.EMPTY_STRING))));

            methods.add(getEqualsMethodClose(getEqualsMethodOpen(className + UtilConstants.IMPL)
                    + getDataFromTempFileHandle(EQUALS_IMPL_MASK, curNode)));

            methods.add(getToStringMethodOpen() + getDataFromTempFileHandle(TO_STRING_IMPL_MASK, curNode)
                    + getToStringMethodClose());

        } catch (IOException e) {
            throw new IOException("No data found in temporary java code fragment files for " + className
                    + " while impl class file generation");
        }

        try {
            methods.add(getConstructorStart(className) + getDataFromTempFileHandle(CONSTRUCTOR_IMPL_MASK, curNode)
                    + UtilConstants.FOUR_SPACE_INDENTATION + UtilConstants.CLOSE_CURLY_BRACKET);
        } catch (IOException e) {
            throw new IOException("No data found in temporary java code fragment files for " + className
                    + " while impl class file generation");
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
     * Return data stored in temporary files.
     *
     * @param curNode current YANG node
     * @param generatedTempFiles mask for the types of files being generated
     * @return data stored in temporary files
     * @throws IOException when failed to get the data from temporary file
     *             handle
     */
    private static String getDataFromTempFileHandle(int generatedTempFiles, YangNode curNode) throws IOException {

        TempJavaCodeFragmentFiles tempJavaCodeFragmentFiles = ((HasTempJavaCodeFragmentFiles) curNode)
                .getTempJavaCodeFragmentFiles();

        if ((generatedTempFiles & ATTRIBUTES_MASK) != 0) {
            return tempJavaCodeFragmentFiles
                    .getTemporaryDataFromFileHandle(tempJavaCodeFragmentFiles.getAttributesTempFileHandle());
        } else if ((generatedTempFiles & GETTER_FOR_INTERFACE_MASK) != 0) {
            return tempJavaCodeFragmentFiles
                    .getTemporaryDataFromFileHandle(tempJavaCodeFragmentFiles.getGetterInterfaceTempFileHandle());
        } else if ((generatedTempFiles & SETTER_FOR_INTERFACE_MASK) != 0) {
            return tempJavaCodeFragmentFiles
                    .getTemporaryDataFromFileHandle(tempJavaCodeFragmentFiles.getSetterInterfaceTempFileHandle());
        } else if ((generatedTempFiles & GETTER_FOR_CLASS_MASK) != 0) {
            return tempJavaCodeFragmentFiles
                    .getTemporaryDataFromFileHandle(tempJavaCodeFragmentFiles.getGetterImplTempFileHandle());
        } else if ((generatedTempFiles & SETTER_FOR_CLASS_MASK) != 0) {
            return tempJavaCodeFragmentFiles
                    .getTemporaryDataFromFileHandle(tempJavaCodeFragmentFiles.getSetterImplTempFileHandle());
        } else if ((generatedTempFiles & CONSTRUCTOR_IMPL_MASK) != 0) {
            return tempJavaCodeFragmentFiles
                    .getTemporaryDataFromFileHandle(tempJavaCodeFragmentFiles.getConstructorImplTempFileHandle());
        } else if ((generatedTempFiles & HASH_CODE_IMPL_MASK) != 0) {
            return tempJavaCodeFragmentFiles
                    .getTemporaryDataFromFileHandle(tempJavaCodeFragmentFiles.getHashCodeImplTempFileHandle());
        } else if ((generatedTempFiles & EQUALS_IMPL_MASK) != 0) {
            return tempJavaCodeFragmentFiles
                    .getTemporaryDataFromFileHandle(tempJavaCodeFragmentFiles.getEqualsImplTempFileHandle());
        } else if ((generatedTempFiles & TO_STRING_IMPL_MASK) != 0) {
            return tempJavaCodeFragmentFiles
                    .getTemporaryDataFromFileHandle(tempJavaCodeFragmentFiles.getToStringImplTempFileHandle());
        }
        return null;
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
     * @throws IOException IO operation failure
     */
    private static void appendMethod(File file, String method) throws IOException {

        insert(file, method);
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

        String pkgString = parsePackageString(pkg, importsList);

        if ((type & IMPL_CLASS_MASK) != 0) {

            write(file, fileName, type, JavaDocType.IMPL_CLASS);
        } else if ((type & BUILDER_INTERFACE_MASK) != 0) {

            write(file, fileName, type, JavaDocType.BUILDER_INTERFACE);
        } else if ((type & GENERATE_TYPEDEF_CLASS) != 0) {
            appendHeaderContents(file, pkgString, importsList);
            write(file, fileName, type, JavaDocType.IMPL_CLASS);
        } else if ((type & INTERFACE_MASK) != 0) {

            appendHeaderContents(file, pkgString, importsList);
            write(file, fileName, type, JavaDocType.INTERFACE);
        } else if ((type & BUILDER_CLASS_MASK) != 0) {

            appendHeaderContents(file, pkgString, importsList);
            write(file, fileName, type, JavaDocType.BUILDER_CLASS);
        }
    }

    /**
     * Removes base directory path from package and generates package string for
     * file.
     *
     * @param pkg generated package
     * @param importsList list of imports
     * @return package string
     */
    private static String parsePackageString(String pkg, List<String> importsList) {

        if (pkg.contains(UtilConstants.ORG)) {
            String[] strArray = pkg.split(UtilConstants.ORG);
            pkg = UtilConstants.ORG + strArray[1].replace(UtilConstants.SLASH, UtilConstants.PERIOD);
        }
        if (importsList != null) {
            if (!importsList.isEmpty()) {
                return UtilConstants.PACKAGE + UtilConstants.SPACE + pkg + UtilConstants.SEMI_COLAN
                        + UtilConstants.NEW_LINE;
            } else {
                return UtilConstants.PACKAGE + UtilConstants.SPACE + pkg + UtilConstants.SEMI_COLAN;
            }
        } else {
            return UtilConstants.PACKAGE + UtilConstants.SPACE + pkg + UtilConstants.SEMI_COLAN;
        }
    }

    /**
     * Appends other contents to interface, builder and typedef classes. for
     * example : ONOS copyright, imports and package.
     *
     * @param file generated file
     * @param pkg generated package
     * @param importsList list of imports
     * @throws IOException when fails to append contents.
     */
    private static void appendHeaderContents(File file, String pkg, List<String> importsList) throws IOException {

        insert(file, CopyrightHeader.getCopyrightHeader());
        insert(file, pkg);

        /*
         * TODO: add the file header using
         * JavaCodeSnippetGen.getFileHeaderComment
         */

        if (importsList != null) {
            insert(file, UtilConstants.NEW_LINE);
            for (String imports : importsList) {
                insert(file, imports);
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

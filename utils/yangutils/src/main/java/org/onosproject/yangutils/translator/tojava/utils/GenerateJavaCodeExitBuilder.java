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

import org.onosproject.yangutils.datamodel.YangNode;
import org.onosproject.yangutils.translator.tojava.HasJavaFileInfo;
import org.onosproject.yangutils.translator.tojava.HasJavaImportData;
import org.onosproject.yangutils.translator.tojava.JavaFileInfo;
import org.onosproject.yangutils.translator.tojava.JavaImportData;
import org.onosproject.yangutils.translator.tojava.JavaQualifiedTypeInfo;
import org.onosproject.yangutils.utils.UtilConstants;

import static org.onosproject.yangutils.translator.tojava.GeneratedJavaFileType.BUILDER_CLASS_MASK;
import static org.onosproject.yangutils.translator.tojava.GeneratedJavaFileType.BUILDER_INTERFACE_MASK;
import static org.onosproject.yangutils.translator.tojava.GeneratedJavaFileType.GENERATE_INTERFACE_WITH_BUILDER;
import static org.onosproject.yangutils.translator.tojava.GeneratedJavaFileType.IMPL_CLASS_MASK;
import static org.onosproject.yangutils.translator.tojava.GeneratedJavaFileType.INTERFACE_MASK;
import static org.onosproject.yangutils.translator.tojava.utils.JavaFileGenerator.appendFileContents;
import static org.onosproject.yangutils.translator.tojava.utils.JavaFileGenerator.clean;
import static org.onosproject.yangutils.translator.tojava.utils.JavaFileGenerator.closeFileHandles;
import static org.onosproject.yangutils.translator.tojava.utils.JavaFileGenerator.generateBuilderClassFile;
import static org.onosproject.yangutils.translator.tojava.utils.JavaFileGenerator.generateBuilderInterfaceFile;
import static org.onosproject.yangutils.translator.tojava.utils.JavaFileGenerator.generateImplClassFile;
import static org.onosproject.yangutils.translator.tojava.utils.JavaFileGenerator.generateInterfaceFile;
import static org.onosproject.yangutils.translator.tojava.utils.JavaFileGenerator.getFileObject;
import static org.onosproject.yangutils.translator.tojava.utils.JavaFileGenerator.insert;

/**
 * Provides generate java code exit.
 */
public final class GenerateJavaCodeExitBuilder {

    private static final String JAVA_FILE_EXTENSION = ".java";

    /**
     * Default constructor.
     */
    private GenerateJavaCodeExitBuilder() {
    }

    /**
     * Sets import for hash and equals method.
     *
     * @return import string for implementing hash and equals
     */
    private static String setImportForHashAndEquals() {

        return UtilConstants.IMPORT + UtilConstants.JAVA_UTIL_OBJECTS_IMPORT_PKG + UtilConstants.PERIOD
                + UtilConstants.JAVA_UTIL_OBJECTS_IMPORT_CLASS;
    }

    /**
     * Sets import for to string method.
     *
     * @return import string for implementing to string
     */
    private static String setImportForToString() {

        return UtilConstants.IMPORT + UtilConstants.GOOGLE_MORE_OBJECT_IMPORT_PKG + UtilConstants.PERIOD
                + UtilConstants.GOOGLE_MORE_OBJECT_IMPORT_CLASS;
    }

    /**
     * Sets import for to list.
     *
     * @return import string for list collection
     */
    private static String setImportForList() {

        return UtilConstants.IMPORT + UtilConstants.COLLECTION_IMPORTS + UtilConstants.PERIOD
                + UtilConstants.LIST + UtilConstants.SEMI_COLAN + UtilConstants.NEW_LINE;
    }

    /**
     * Construct java code exit.
     *
     * @param fileType generated file type
     * @param curNode current YANG node
     * @throws IOException when fails to generate java files
     */
    public static void generateJavaFile(int fileType, YangNode curNode) throws IOException {

        JavaFileInfo javaFileInfo = ((HasJavaFileInfo) curNode).getJavaFileInfo();
        String className = JavaIdentifierSyntax.getCaptialCase(javaFileInfo.getJavaName());
        String pkg = javaFileInfo.getPackageFilePath();
        List<String> imports = getImports(((HasJavaImportData) curNode).getJavaImportData());

        /**
         * Start generation of files.
         */
        if ((fileType & INTERFACE_MASK) != 0 | (fileType & BUILDER_INTERFACE_MASK) != 0
                | fileType == GENERATE_INTERFACE_WITH_BUILDER) {

            /**
             * Create interface file.
             */
            String interfaceFileName = className;
            File interfaceFile = getFileObject(pkg, interfaceFileName, JAVA_FILE_EXTENSION, javaFileInfo);
            interfaceFile = generateInterfaceFile(interfaceFile, imports, curNode);
            /**
             * Create temp builder interface file.
             */
            String builderInterfaceFileName = className
                    + UtilConstants.BUILDER + UtilConstants.INTERFACE;
            File builderInterfaceFile = getFileObject(pkg, builderInterfaceFileName, JAVA_FILE_EXTENSION, javaFileInfo);
            builderInterfaceFile = generateBuilderInterfaceFile(builderInterfaceFile, curNode);
            /**
             * Append builder interface file to interface file and close it.
             */
            appendFileContents(builderInterfaceFile, interfaceFile);
            insert(interfaceFile, JavaCodeSnippetGen.getJavaClassDefClose());
            /**
             * Close file handle for interface files.
             */
            closeFileHandles(builderInterfaceFile);
            closeFileHandles(interfaceFile);

            /**
             * Remove temp files.
             */
            clean(builderInterfaceFile);
        }

        imports.add(setImportForHashAndEquals());
        imports.add(setImportForToString());
        java.util.Collections.sort(imports);

        if ((fileType & BUILDER_CLASS_MASK) != 0 | (fileType & IMPL_CLASS_MASK) != 0
                | fileType == GENERATE_INTERFACE_WITH_BUILDER) {

            /**
             * Create builder class file.
             */
            String builderFileName = className
                    + UtilConstants.BUILDER;
            File builderFile = getFileObject(pkg, builderFileName, JAVA_FILE_EXTENSION, javaFileInfo);
            builderFile = generateBuilderClassFile(builderFile, imports, curNode);
            /**
             * Create temp impl class file.
             */

            String implFileName = className + UtilConstants.IMPL;
            File implTempFile = getFileObject(pkg, implFileName, JAVA_FILE_EXTENSION, javaFileInfo);
            implTempFile = generateImplClassFile(implTempFile, curNode);
            /**
             * Append impl class to builder class and close it.
             */
            appendFileContents(implTempFile, builderFile);
            insert(builderFile, JavaCodeSnippetGen.getJavaClassDefClose());

            /**
             * Close file handle for classes files.
             */
            closeFileHandles(implTempFile);
            closeFileHandles(builderFile);

            /**
             * Remove temp files.
             */
            clean(implTempFile);
        }

        /**
         * if ((fileType & GENERATE_TYPEDEF_CLASS) != 0) {
         *
         * /** Create builder class file. //
         */
        //String typeDefFileName = className;
        //File typeDefFile = JavaFileGenerator.getFileObject(path, typeDefFileName, JAVA_FILE_EXTENSION,
        //      ((HasJavaFileInfo) curNode).getJavaFileInfo());
        //typeDefFile = JavaFileGenerator.generateTypeDefClassFile(typeDefFile, className, imports,
        //        path.replace('/', '.'), attrList, ((HasJavaFileInfo) curNode).getJavaFileInfo());
        // JavaFileGenerator.insert(typeDefFile, JavaCodeSnippetGen.getJavaClassDefClose());

        //  /**
        //     * Close file handle for classes files.
        //       */
        //        JavaFileGenerator.closeFileHandles(typeDefFile);
        //      }
        //
    }

    /**
     * Returns import for class.
     *
     * @param javaImportData import data
     * @return imports for class
     */
    private static List<String> getImports(JavaImportData javaImportData) {

        String importString;
        List<String> imports = new ArrayList<>();

        for (JavaQualifiedTypeInfo importInfo : javaImportData.getImportSet()) {
            importString = UtilConstants.IMPORT;
            if (importInfo.getPkgInfo() != "" && importInfo.getClassInfo() != null
                    && importInfo.getPkgInfo() != UtilConstants.JAVA_LANG) {
                importString = importString + importInfo.getPkgInfo() + ".";
                importString = importString + importInfo.getClassInfo() + UtilConstants.SEMI_COLAN
                        + UtilConstants.NEW_LINE;

                imports.add(importString);
            }
        }

        if (javaImportData.getIfListImported()) {
            imports.add(setImportForList());
        }

        java.util.Collections.sort(imports);
        return imports;
    }
}

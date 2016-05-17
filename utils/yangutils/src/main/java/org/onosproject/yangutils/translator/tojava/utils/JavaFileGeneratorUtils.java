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
import java.util.List;

import org.onosproject.yangutils.datamodel.YangNode;
import org.onosproject.yangutils.translator.exception.TranslatorException;
import org.onosproject.yangutils.translator.tojava.JavaFileInfo;
import org.onosproject.yangutils.translator.tojava.JavaFileInfoContainer;
import org.onosproject.yangutils.translator.tojava.TempJavaBeanFragmentFiles;
import org.onosproject.yangutils.translator.tojava.TempJavaEnumerationFragmentFiles;
import org.onosproject.yangutils.translator.tojava.TempJavaFragmentFiles;
import org.onosproject.yangutils.translator.tojava.TempJavaServiceFragmentFiles;
import org.onosproject.yangutils.translator.tojava.TempJavaTypeFragmentFiles;
import org.onosproject.yangutils.utils.io.impl.CopyrightHeader;
import org.onosproject.yangutils.utils.io.impl.JavaDocGen.JavaDocType;

import static org.onosproject.yangutils.translator.tojava.GeneratedJavaFileType.BUILDER_CLASS_MASK;
import static org.onosproject.yangutils.translator.tojava.GeneratedJavaFileType.BUILDER_INTERFACE_MASK;
import static org.onosproject.yangutils.translator.tojava.GeneratedJavaFileType.GENERATE_ENUM_CLASS;
import static org.onosproject.yangutils.translator.tojava.GeneratedJavaFileType.GENERATE_EVENT_CLASS;
import static org.onosproject.yangutils.translator.tojava.GeneratedJavaFileType.GENERATE_EVENT_LISTENER_INTERFACE;
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
import static org.onosproject.yangutils.translator.tojava.utils.ClassDefinitionGenerator.generateClassDefinition;
import static org.onosproject.yangutils.translator.tojava.utils.JavaIdentifierSyntax.getJavaPackageFromPackagePath;
import static org.onosproject.yangutils.translator.tojava.utils.JavaIdentifierSyntax.getSmallCase;
import static org.onosproject.yangutils.utils.UtilConstants.FOUR_SPACE_INDENTATION;
import static org.onosproject.yangutils.utils.UtilConstants.INT;
import static org.onosproject.yangutils.utils.UtilConstants.MANAGER;
import static org.onosproject.yangutils.utils.UtilConstants.NEW_LINE;
import static org.onosproject.yangutils.utils.UtilConstants.ORG;
import static org.onosproject.yangutils.utils.UtilConstants.PACKAGE;
import static org.onosproject.yangutils.utils.UtilConstants.PRIVATE;
import static org.onosproject.yangutils.utils.UtilConstants.SEMI_COLAN;
import static org.onosproject.yangutils.utils.UtilConstants.SERVICE;
import static org.onosproject.yangutils.utils.UtilConstants.SLASH;
import static org.onosproject.yangutils.utils.UtilConstants.SPACE;
import static org.onosproject.yangutils.utils.io.impl.JavaDocGen.getJavaDoc;
import static org.onosproject.yangutils.utils.io.impl.JavaDocGen.JavaDocType.BUILDER_CLASS;
import static org.onosproject.yangutils.utils.io.impl.JavaDocGen.JavaDocType.BUILDER_INTERFACE;
import static org.onosproject.yangutils.utils.io.impl.JavaDocGen.JavaDocType.ENUM_CLASS;
import static org.onosproject.yangutils.utils.io.impl.JavaDocGen.JavaDocType.EVENT;
import static org.onosproject.yangutils.utils.io.impl.JavaDocGen.JavaDocType.EVENT_LISTENER;
import static org.onosproject.yangutils.utils.io.impl.JavaDocGen.JavaDocType.IMPL_CLASS;
import static org.onosproject.yangutils.utils.io.impl.JavaDocGen.JavaDocType.INTERFACE;
import static org.onosproject.yangutils.utils.io.impl.JavaDocGen.JavaDocType.RPC_INTERFACE;
import static org.onosproject.yangutils.utils.io.impl.JavaDocGen.JavaDocType.RPC_MANAGER;
import static org.onosproject.yangutils.utils.io.impl.YangIoUtils.insertDataIntoJavaFile;

/**
 * Represents utilities for java file generator.
 */
public final class JavaFileGeneratorUtils {

    /**
     * Creates an instance of java file generator util.
     */
    private JavaFileGeneratorUtils() {
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

        return new File(handle.getBaseCodeGenPath() + filePath + SLASH + fileName + extension);
    }

    /**
     * Returns data stored in temporary files.
     *
     * @param generatedTempFiles temporary file types
     * @param tempJavaFragmentFiles temp java fragment files
     * @return data stored in temporary files
     * @throws IOException when failed to get the data from temporary file handle
     */
    public static String getDataFromTempFileHandle(int generatedTempFiles,
            TempJavaFragmentFiles tempJavaFragmentFiles)
            throws IOException {

        TempJavaTypeFragmentFiles typeFragmentFiles = null;

        if (tempJavaFragmentFiles instanceof TempJavaTypeFragmentFiles) {
            typeFragmentFiles = (TempJavaTypeFragmentFiles) tempJavaFragmentFiles;
        }

        TempJavaBeanFragmentFiles beanFragmentFiles = null;

        if (tempJavaFragmentFiles instanceof TempJavaBeanFragmentFiles) {
            beanFragmentFiles = (TempJavaBeanFragmentFiles) tempJavaFragmentFiles;
        }

        TempJavaServiceFragmentFiles serviceFragmentFiles = null;
        if (tempJavaFragmentFiles instanceof TempJavaServiceFragmentFiles) {
            serviceFragmentFiles = (TempJavaServiceFragmentFiles) tempJavaFragmentFiles;
        }
        if ((generatedTempFiles & ATTRIBUTES_MASK) != 0) {
            return tempJavaFragmentFiles
                    .getTemporaryDataFromFileHandle(tempJavaFragmentFiles.getAttributesTempFileHandle());
        } else if ((generatedTempFiles & GETTER_FOR_INTERFACE_MASK) != 0) {
            return tempJavaFragmentFiles
                    .getTemporaryDataFromFileHandle(tempJavaFragmentFiles.getGetterInterfaceTempFileHandle());
        } else if ((generatedTempFiles & SETTER_FOR_INTERFACE_MASK) != 0) {
            return tempJavaFragmentFiles
                    .getTemporaryDataFromFileHandle(tempJavaFragmentFiles.getSetterInterfaceTempFileHandle());
        } else if ((generatedTempFiles & GETTER_FOR_CLASS_MASK) != 0) {
            return tempJavaFragmentFiles
                    .getTemporaryDataFromFileHandle(tempJavaFragmentFiles.getGetterImplTempFileHandle());
        } else if ((generatedTempFiles & SETTER_FOR_CLASS_MASK) != 0) {
            return tempJavaFragmentFiles
                    .getTemporaryDataFromFileHandle(tempJavaFragmentFiles.getSetterImplTempFileHandle());
        } else if ((generatedTempFiles & CONSTRUCTOR_IMPL_MASK) != 0) {
            if (beanFragmentFiles == null) {
                throw new TranslatorException("Required constructor info is missing.");
            }
            return beanFragmentFiles
                    .getTemporaryDataFromFileHandle(beanFragmentFiles.getConstructorImplTempFileHandle());
        } else if ((generatedTempFiles & HASH_CODE_IMPL_MASK) != 0) {
            return tempJavaFragmentFiles
                    .getTemporaryDataFromFileHandle(tempJavaFragmentFiles.getHashCodeImplTempFileHandle());
        } else if ((generatedTempFiles & EQUALS_IMPL_MASK) != 0) {
            return tempJavaFragmentFiles
                    .getTemporaryDataFromFileHandle(tempJavaFragmentFiles.getEqualsImplTempFileHandle());
        } else if ((generatedTempFiles & TO_STRING_IMPL_MASK) != 0) {
            return tempJavaFragmentFiles
                    .getTemporaryDataFromFileHandle(tempJavaFragmentFiles.getToStringImplTempFileHandle());
        } else if ((generatedTempFiles & OF_STRING_IMPL_MASK) != 0) {
            if (typeFragmentFiles == null) {
                throw new TranslatorException("Required of string implementation info is missing.");
            }
            return typeFragmentFiles
                    .getTemporaryDataFromFileHandle(typeFragmentFiles.getOfStringImplTempFileHandle());
        } else if ((generatedTempFiles & CONSTRUCTOR_FOR_TYPE_MASK) != 0) {
            if (typeFragmentFiles == null) {
                throw new TranslatorException("Required constructor implementation info is missing.");
            }
            return typeFragmentFiles
                    .getTemporaryDataFromFileHandle(typeFragmentFiles.getConstructorForTypeTempFileHandle());
        } else if ((generatedTempFiles & FROM_STRING_IMPL_MASK) != 0) {
            return tempJavaFragmentFiles
                    .getTemporaryDataFromFileHandle(tempJavaFragmentFiles.getFromStringImplTempFileHandle());
        } else if ((generatedTempFiles & ENUM_IMPL_MASK) != 0) {
            if (!(tempJavaFragmentFiles instanceof TempJavaEnumerationFragmentFiles)) {
                throw new TranslatorException("Required enum info is missing.");
            }
            TempJavaEnumerationFragmentFiles enumFragmentFiles =
                    (TempJavaEnumerationFragmentFiles) tempJavaFragmentFiles;
            return enumFragmentFiles
                    .getTemporaryDataFromFileHandle(enumFragmentFiles.getEnumClassTempFileHandle());
        } else if ((generatedTempFiles & RPC_INTERFACE_MASK) != 0) {
            if (serviceFragmentFiles == null) {
                throw new TranslatorException("Required rpc interface info is missing.");
            }
            return serviceFragmentFiles
                    .getTemporaryDataFromFileHandle(serviceFragmentFiles.getRpcInterfaceTempFileHandle());
        } else if ((generatedTempFiles & RPC_IMPL_MASK) != 0) {
            if (serviceFragmentFiles == null) {
                throw new TranslatorException("Required rpc implementation info is missing.");
            }
            return serviceFragmentFiles
                    .getTemporaryDataFromFileHandle(serviceFragmentFiles.getRpcImplTempFileHandle());
        }
        return null;
    }

    /**
     * Initiates generation of file based on generated file type.
     *
     * @param file generated file
     * @param className generated file class name
     * @param type generated file type
     * @param imports imports for the file
     * @param pkg generated file package
     * @throws IOException when fails to generate a file
     */
    public static void initiateJavaFileGeneration(File file, String className, int type, List<String> imports,
            String pkg)
            throws IOException {

        try {
            file.createNewFile();
            appendContents(file, className, type, imports, pkg);
        } catch (IOException e) {
            throw new IOException("Failed to create " + file.getName() + " class file.");
        }
    }

    /**
     * Initiates generation of file based on generated file type.
     *
     * @param file generated file
     * @param type generated file type
     * @param imports imports for the file
     * @param curNode current YANG node
     * @param className class name
     * @throws IOException when fails to generate a file
     */
    public static void initiateJavaFileGeneration(File file, int type, List<String> imports,
            YangNode curNode, String className) throws IOException {

        try {
            file.createNewFile();
            appendContents(file, type, imports, curNode, className);
        } catch (IOException e) {
            throw new IOException("Failed to create " + file.getName() + " class file.");
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
    private static void appendContents(File file, int type, List<String> importsList, YangNode curNode,
            String className) throws IOException {

        JavaFileInfo javaFileInfo = ((JavaFileInfoContainer) curNode).getJavaFileInfo();

        String path = javaFileInfo.getBaseCodeGenPath() + javaFileInfo.getPackageFilePath();
        String pkgString = parsePackageString(path, importsList);

        switch (type) {
        case INTERFACE_MASK:
            appendHeaderContents(file, pkgString, importsList);
            write(file, type, INTERFACE, curNode, className);
            break;
        case GENERATE_SERVICE_AND_MANAGER:
            appendHeaderContents(file, pkgString, importsList);
            write(file, type, RPC_INTERFACE, curNode, className);
            break;
        case GENERATE_EVENT_CLASS:
            appendHeaderContents(file, pkgString, importsList);
            write(file, type, EVENT, curNode, className);
            break;
        default:
            break;
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
    private static void appendContents(File file, String fileName, int type, List<String> importsList, String pkg)
            throws IOException {

        String pkgString = parsePackageString(pkg, importsList);

        switch (type) {
        case IMPL_CLASS_MASK:
            write(file, fileName, type, IMPL_CLASS);
            break;
        case BUILDER_INTERFACE_MASK:
            write(file, fileName, type, BUILDER_INTERFACE);
            break;
        case GENERATE_TYPEDEF_CLASS:
            appendHeaderContents(file, pkgString, importsList);
            write(file, fileName, type, IMPL_CLASS);
            break;
        case BUILDER_CLASS_MASK:
            appendHeaderContents(file, pkgString, importsList);
            write(file, fileName, type, BUILDER_CLASS);
            break;
        case GENERATE_UNION_CLASS:
            appendHeaderContents(file, pkgString, importsList);
            write(file, fileName, type, IMPL_CLASS);
            break;
        case GENERATE_ENUM_CLASS:
            appendHeaderContents(file, pkgString, importsList);
            write(file, fileName, type, ENUM_CLASS);
            break;
        case GENERATE_EVENT_LISTENER_INTERFACE:
            appendHeaderContents(file, pkgString, importsList);
            write(file, fileName, type, EVENT_LISTENER);
            break;
        default:
            break;
        }
    }

    /**
     * Removes base directory path from package and generates package string for file.
     *
     * @param javaPkg generated java package
     * @param importsList list of imports
     * @return package string
     */
    private static String parsePackageString(String javaPkg, List<String> importsList) {

        if (javaPkg.contains(ORG)) {
            String[] strArray = javaPkg.split(ORG);
            javaPkg = ORG + getJavaPackageFromPackagePath(strArray[1]);
        }
        if (importsList != null) {
            if (!importsList.isEmpty()) {
                return PACKAGE + SPACE + javaPkg + SEMI_COLAN + NEW_LINE;
            } else {
                return PACKAGE + SPACE + javaPkg + SEMI_COLAN;
            }
        } else {
            return PACKAGE + SPACE + javaPkg + SEMI_COLAN;
        }
    }

    /**
     * Appends other contents to interface, builder and typedef classes.
     * for example : ONOS copyright, imports and package.
     *
     * @param file generated file
     * @param pkg generated package
     * @param importsList list of imports
     * @throws IOException when fails to append contents
     */
    private static void appendHeaderContents(File file, String pkg, List<String> importsList)
            throws IOException {

        insertDataIntoJavaFile(file, CopyrightHeader.getCopyrightHeader());
        insertDataIntoJavaFile(file, pkg);

        /*
         * TODO: add the file header using
         * JavaCodeSnippetGen.getFileHeaderComment
         */

        if (importsList != null) {
            insertDataIntoJavaFile(file, NEW_LINE);
            for (String imports : importsList) {
                insertDataIntoJavaFile(file, imports);
            }
        }
    }

    /**
     * Writes data to the specific generated file.
     *
     * @param file generated file
     * @param fileName file name
     * @param genType generated file type
     * @param javaDocType java doc type
     * @throws IOException when fails to write into a file
     */
    private static void write(File file, int genType, JavaDocType javaDocType, YangNode curNode, String fileName)
            throws IOException {

        if ((genType & GENERATE_SERVICE_AND_MANAGER) != 0) {
            if (!fileName.contains(SERVICE)) {
                insertDataIntoJavaFile(file, getJavaDoc(RPC_MANAGER, fileName + MANAGER, false));
            } else {
                insertDataIntoJavaFile(file, getJavaDoc(javaDocType, fileName, false));
            }
        } else {
            insertDataIntoJavaFile(file, getJavaDoc(javaDocType, fileName, false));
        }
        insertDataIntoJavaFile(file, generateClassDefinition(genType, fileName, curNode));
    }

    /**
     * Writes data to the specific generated file.
     *
     * @param file generated file
     * @param fileName file name
     * @param genType generated file type
     * @param javaDocType java doc type
     * @throws IOException when fails to write into a file
     */
    private static void write(File file, String fileName, int genType, JavaDocType javaDocType)
            throws IOException {
        insertDataIntoJavaFile(file, getJavaDoc(javaDocType, fileName, false));
        insertDataIntoJavaFile(file, generateClassDefinition(genType, fileName));
    }

    /**
     * Returns integer attribute for enum's class to get the values.
     *
     * @param className enum's class name
     * @return enum's attribute
     */
    public static String getEnumsValueAttribute(String className) {
        return NEW_LINE + FOUR_SPACE_INDENTATION + PRIVATE + SPACE + INT + SPACE + getSmallCase(className)
                + SEMI_COLAN + NEW_LINE;
    }

}

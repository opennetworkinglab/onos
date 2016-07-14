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

import org.onosproject.yangutils.datamodel.YangAtomicPath;
import org.onosproject.yangutils.datamodel.YangAugment;
import org.onosproject.yangutils.datamodel.YangNode;
import org.onosproject.yangutils.datamodel.YangNodeIdentifier;
import org.onosproject.yangutils.translator.exception.TranslatorException;
import org.onosproject.yangutils.translator.tojava.JavaCodeGeneratorInfo;
import org.onosproject.yangutils.translator.tojava.JavaFileInfo;
import org.onosproject.yangutils.translator.tojava.JavaFileInfoContainer;
import org.onosproject.yangutils.translator.tojava.JavaImportData;
import org.onosproject.yangutils.translator.tojava.JavaQualifiedTypeInfo;
import org.onosproject.yangutils.translator.tojava.TempJavaBeanFragmentFiles;
import org.onosproject.yangutils.translator.tojava.TempJavaCodeFragmentFiles;
import org.onosproject.yangutils.translator.tojava.TempJavaEnumerationFragmentFiles;
import org.onosproject.yangutils.translator.tojava.TempJavaEventFragmentFiles;
import org.onosproject.yangutils.translator.tojava.TempJavaFragmentFiles;
import org.onosproject.yangutils.translator.tojava.TempJavaServiceFragmentFiles;
import org.onosproject.yangutils.translator.tojava.TempJavaTypeFragmentFiles;
import org.onosproject.yangutils.utils.io.impl.CopyrightHeader;
import org.onosproject.yangutils.utils.io.impl.JavaDocGen.JavaDocType;
import org.onosproject.yangutils.utils.io.impl.YangPluginConfig;

import static org.onosproject.yangutils.translator.tojava.GeneratedJavaFileType.BUILDER_CLASS_MASK;
import static org.onosproject.yangutils.translator.tojava.GeneratedJavaFileType.BUILDER_INTERFACE_MASK;
import static org.onosproject.yangutils.translator.tojava.GeneratedJavaFileType.GENERATE_ENUM_CLASS;
import static org.onosproject.yangutils.translator.tojava.GeneratedJavaFileType.GENERATE_EVENT_CLASS;
import static org.onosproject.yangutils.translator.tojava.GeneratedJavaFileType.GENERATE_EVENT_LISTENER_INTERFACE;
import static org.onosproject.yangutils.translator.tojava.GeneratedJavaFileType.GENERATE_EVENT_SUBJECT_CLASS;
import static org.onosproject.yangutils.translator.tojava.GeneratedJavaFileType.GENERATE_IDENTITY_CLASS;
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
import static org.onosproject.yangutils.translator.tojava.JavaQualifiedTypeInfo.getQualifiedTypeInfoOfCurNode;
import static org.onosproject.yangutils.translator.tojava.YangJavaModelUtils.getAugmentedNodesPackage;
import static org.onosproject.yangutils.translator.tojava.utils.ClassDefinitionGenerator.generateClassDefinition;
import static org.onosproject.yangutils.utils.UtilConstants.CLOSE_CURLY_BRACKET;
import static org.onosproject.yangutils.utils.UtilConstants.NEW_LINE;
import static org.onosproject.yangutils.utils.UtilConstants.PACKAGE;
import static org.onosproject.yangutils.utils.UtilConstants.PERIOD;
import static org.onosproject.yangutils.utils.UtilConstants.REGEX_FOR_ANY_STRING_ENDING_WITH_SERVICE;
import static org.onosproject.yangutils.utils.UtilConstants.SEMI_COLAN;
import static org.onosproject.yangutils.utils.UtilConstants.SLASH;
import static org.onosproject.yangutils.utils.UtilConstants.SPACE;
import static org.onosproject.yangutils.utils.io.impl.JavaDocGen.JavaDocType.BUILDER_CLASS;
import static org.onosproject.yangutils.utils.io.impl.JavaDocGen.JavaDocType.BUILDER_INTERFACE;
import static org.onosproject.yangutils.utils.io.impl.JavaDocGen.JavaDocType.ENUM_CLASS;
import static org.onosproject.yangutils.utils.io.impl.JavaDocGen.JavaDocType.EVENT;
import static org.onosproject.yangutils.utils.io.impl.JavaDocGen.JavaDocType.EVENT_LISTENER;
import static org.onosproject.yangutils.utils.io.impl.JavaDocGen.JavaDocType.EVENT_SUBJECT_CLASS;
import static org.onosproject.yangutils.utils.io.impl.JavaDocGen.JavaDocType.IMPL_CLASS;
import static org.onosproject.yangutils.utils.io.impl.JavaDocGen.JavaDocType.INTERFACE;
import static org.onosproject.yangutils.utils.io.impl.JavaDocGen.JavaDocType.OPERATION_CLASS;
import static org.onosproject.yangutils.utils.io.impl.JavaDocGen.JavaDocType.RPC_INTERFACE;
import static org.onosproject.yangutils.utils.io.impl.JavaDocGen.JavaDocType.RPC_MANAGER;
import static org.onosproject.yangutils.utils.io.impl.JavaDocGen.getJavaDoc;
import static org.onosproject.yangutils.utils.io.impl.YangIoUtils.getCamelCase;
import static org.onosproject.yangutils.utils.io.impl.YangIoUtils.getCapitalCase;
import static org.onosproject.yangutils.utils.io.impl.YangIoUtils.getJavaPackageFromPackagePath;
import static org.onosproject.yangutils.utils.io.impl.YangIoUtils.insertDataIntoJavaFile;
import static org.onosproject.yangutils.utils.io.impl.YangIoUtils.parsePkg;

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
     * @param filePath     file package path
     * @param fileName     file name
     * @param extension    file extension
     * @param baseCodePath cached file handle
     * @return file object
     */
    public static File getFileObject(String filePath, String fileName, String extension, String baseCodePath) {

        return new File(baseCodePath + filePath + SLASH + fileName + extension);
    }

    /**
     * Returns data stored in temporary files.
     *
     * @param generatedTempFiles    temporary file types
     * @param tempJavaFragmentFiles temp java fragment files
     * @param absolutePath          absolute path
     * @return data stored in temporary files
     * @throws IOException when failed to get the data from temporary file handle
     */
    static String getDataFromTempFileHandle(int generatedTempFiles,
                                            TempJavaFragmentFiles tempJavaFragmentFiles, String absolutePath)
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

        TempJavaEventFragmentFiles eventFragmentFiles = null;
        if (tempJavaFragmentFiles instanceof TempJavaEventFragmentFiles) {
            eventFragmentFiles = (TempJavaEventFragmentFiles) tempJavaFragmentFiles;
        }

        if ((generatedTempFiles & ATTRIBUTES_MASK) != 0) {
            return tempJavaFragmentFiles
                    .getTemporaryDataFromFileHandle(tempJavaFragmentFiles.getAttributesTempFileHandle(),
                            absolutePath);
        } else if ((generatedTempFiles & GETTER_FOR_INTERFACE_MASK) != 0) {
            return tempJavaFragmentFiles
                    .getTemporaryDataFromFileHandle(tempJavaFragmentFiles.getGetterInterfaceTempFileHandle(),
                            absolutePath);
        } else if ((generatedTempFiles & SETTER_FOR_INTERFACE_MASK) != 0) {
            return tempJavaFragmentFiles
                    .getTemporaryDataFromFileHandle(tempJavaFragmentFiles.getSetterInterfaceTempFileHandle(),
                            absolutePath);
        } else if ((generatedTempFiles & GETTER_FOR_CLASS_MASK) != 0) {
            return tempJavaFragmentFiles
                    .getTemporaryDataFromFileHandle(tempJavaFragmentFiles.getGetterImplTempFileHandle(),
                            absolutePath);
        } else if ((generatedTempFiles & SETTER_FOR_CLASS_MASK) != 0) {
            return tempJavaFragmentFiles
                    .getTemporaryDataFromFileHandle(tempJavaFragmentFiles.getSetterImplTempFileHandle(),
                            absolutePath);
        } else if ((generatedTempFiles & CONSTRUCTOR_IMPL_MASK) != 0) {
            if (beanFragmentFiles == null) {
                throw new TranslatorException("Required constructor info is missing.");
            }
            return beanFragmentFiles
                    .getTemporaryDataFromFileHandle(beanFragmentFiles.getConstructorImplTempFileHandle(),
                            absolutePath);
        } else if ((generatedTempFiles & HASH_CODE_IMPL_MASK) != 0) {
            return tempJavaFragmentFiles
                    .getTemporaryDataFromFileHandle(tempJavaFragmentFiles.getHashCodeImplTempFileHandle(),
                            absolutePath);
        } else if ((generatedTempFiles & EQUALS_IMPL_MASK) != 0) {
            return tempJavaFragmentFiles
                    .getTemporaryDataFromFileHandle(tempJavaFragmentFiles.getEqualsImplTempFileHandle(),
                            absolutePath);
        } else if ((generatedTempFiles & TO_STRING_IMPL_MASK) != 0) {
            return tempJavaFragmentFiles
                    .getTemporaryDataFromFileHandle(tempJavaFragmentFiles.getToStringImplTempFileHandle(),
                            absolutePath);
        } else if ((generatedTempFiles & OF_STRING_IMPL_MASK) != 0) {
            if (typeFragmentFiles == null) {
                throw new TranslatorException("Required of string implementation info is missing.");
            }
            return typeFragmentFiles
                    .getTemporaryDataFromFileHandle(typeFragmentFiles.getOfStringImplTempFileHandle(), absolutePath);
        } else if ((generatedTempFiles & CONSTRUCTOR_FOR_TYPE_MASK) != 0) {
            if (typeFragmentFiles == null) {
                throw new TranslatorException("Required constructor implementation info is missing.");
            }
            return typeFragmentFiles
                    .getTemporaryDataFromFileHandle(typeFragmentFiles.getConstructorForTypeTempFileHandle(),
                            absolutePath);
        } else if ((generatedTempFiles & FROM_STRING_IMPL_MASK) != 0) {
            return tempJavaFragmentFiles
                    .getTemporaryDataFromFileHandle(tempJavaFragmentFiles.getFromStringImplTempFileHandle(),
                            absolutePath);
        } else if ((generatedTempFiles & ENUM_IMPL_MASK) != 0) {
            if (!(tempJavaFragmentFiles instanceof TempJavaEnumerationFragmentFiles)) {
                throw new TranslatorException("Required enum info is missing.");
            }
            TempJavaEnumerationFragmentFiles enumFragmentFiles =
                    (TempJavaEnumerationFragmentFiles) tempJavaFragmentFiles;
            return enumFragmentFiles
                    .getTemporaryDataFromFileHandle(enumFragmentFiles.getEnumClassTempFileHandle(), absolutePath);
        } else if ((generatedTempFiles & RPC_INTERFACE_MASK) != 0) {
            if (serviceFragmentFiles == null) {
                throw new TranslatorException("Required rpc interface info is missing.");
            }
            return serviceFragmentFiles
                    .getTemporaryDataFromFileHandle(serviceFragmentFiles.getRpcInterfaceTempFileHandle(),
                            absolutePath);
        } else if ((generatedTempFiles & RPC_IMPL_MASK) != 0) {
            if (serviceFragmentFiles == null) {
                throw new TranslatorException("Required rpc implementation info is missing.");
            }
            return serviceFragmentFiles
                    .getTemporaryDataFromFileHandle(serviceFragmentFiles.getRpcImplTempFileHandle(),
                            absolutePath);
        } else if ((generatedTempFiles & EVENT_ENUM_MASK) != 0) {
            if (eventFragmentFiles == null) {
                throw new TranslatorException("Required event enum implementation info is missing.");
            }
            return eventFragmentFiles
                    .getTemporaryDataFromFileHandle(eventFragmentFiles.getEventEnumTempFileHandle(),
                            absolutePath);
        } else if ((generatedTempFiles & EVENT_METHOD_MASK) != 0) {
            if (eventFragmentFiles == null) {
                throw new TranslatorException("Required event method implementation info is missing.");
            }
            return eventFragmentFiles
                    .getTemporaryDataFromFileHandle(eventFragmentFiles.getEventMethodTempFileHandle(),
                            absolutePath);
        } else if ((generatedTempFiles & EVENT_SUBJECT_GETTER_MASK) != 0) {
            if (eventFragmentFiles == null) {
                throw new TranslatorException("Required event subject getter implementation info is missing.");
            }
            return eventFragmentFiles
                    .getTemporaryDataFromFileHandle(eventFragmentFiles.getEventSubjectGetterTempFileHandle(),
                            absolutePath);
        } else if ((generatedTempFiles & EVENT_SUBJECT_SETTER_MASK) != 0) {
            if (eventFragmentFiles == null) {
                throw new TranslatorException("Required event subject setter implementation info is missing.");
            }
            return eventFragmentFiles
                    .getTemporaryDataFromFileHandle(eventFragmentFiles.getEventSubjectSetterTempFileHandle(),
                            absolutePath);
        } else if ((generatedTempFiles & EVENT_SUBJECT_ATTRIBUTE_MASK) != 0) {
            if (eventFragmentFiles == null) {
                throw new TranslatorException("Required event subject attribute implementation info is missing.");
            }
            return eventFragmentFiles
                    .getTemporaryDataFromFileHandle(eventFragmentFiles.getEventSubjectAttributeTempFileHandle(),
                            absolutePath);
        }
        return null;
    }

    /**
     * Initiates generation of file based on generated file type.
     *
     * @param file         generated file
     * @param className    generated file class name
     * @param genType      generated file type
     * @param imports      imports for the file
     * @param pkg          generated file package
     * @param pluginConfig plugin configurations
     * @throws IOException when fails to generate a file
     */
    public static void initiateJavaFileGeneration(File file, String className, int genType, List<String> imports,
                                                  String pkg, YangPluginConfig pluginConfig)
            throws IOException {

        boolean isFileCreated;
        try {
            isFileCreated = file.createNewFile();
            if (!isFileCreated) {
                throw new IOException("Failed to create " + file.getName() + " class file.");
            }
            appendContents(file, className, genType, imports, pkg, pluginConfig);
        } catch (IOException e) {
            throw new IOException("Failed to append contents in " + file.getName() + " class file.");
        }
    }

    /**
     * Initiates generation of file based on generated file type.
     *
     * @param file      generated file
     * @param genType   generated file type
     * @param imports   imports for the file
     * @param curNode   current YANG node
     * @param className class name
     * @throws IOException when fails to generate a file
     */
    public static void initiateJavaFileGeneration(File file, int genType, List<String> imports,
                                                  YangNode curNode, String className)
            throws IOException {

        boolean isFileCreated;
        try {
            isFileCreated = file.createNewFile();
            if (!isFileCreated) {
                throw new IOException("Failed to create " + file.getName() + " class file.");
            }
            appendContents(file, genType, imports, curNode, className);
        } catch (IOException e) {
            throw new IOException("Failed to append contents in " + file.getName() + " class file.");
        }
    }

    /**
     * Appends all the contents into a generated java file.
     *
     * @param file        generated file
     * @param genType     generated file type
     * @param importsList list of java imports
     * @param curNode     current YANG node
     * @param className   class name
     * @throws IOException
     */
    private static void appendContents(File file, int genType, List<String> importsList, YangNode curNode,
                                       String className)
            throws IOException {

        JavaFileInfo javaFileInfo = ((JavaFileInfoContainer) curNode).getJavaFileInfo();

        String name = javaFileInfo.getJavaName();
        String path = javaFileInfo.getBaseCodeGenPath() + javaFileInfo.getPackageFilePath();

        String pkgString;
        if (genType == GENERATE_EVENT_CLASS
                || genType == GENERATE_EVENT_LISTENER_INTERFACE
                || genType == GENERATE_EVENT_SUBJECT_CLASS) {
            pkgString = parsePackageString((path + PERIOD + name).toLowerCase(), importsList);
        } else {
            pkgString = parsePackageString(path, importsList);
        }
        switch (genType) {
            case INTERFACE_MASK:
                appendHeaderContents(file, pkgString, importsList);
                write(file, genType, INTERFACE, curNode, className);
                break;
            case IMPL_CLASS_MASK:
                appendHeaderContents(file, pkgString, importsList);
                write(file, genType, IMPL_CLASS, curNode, className);
                break;
            case OPERATION_CLASS_MASK:
                appendHeaderContents(file, pkgString, importsList);
                write(file, genType, OPERATION_CLASS, curNode, className);
                break;
            case BUILDER_CLASS_MASK:
                write(file, genType, BUILDER_CLASS, curNode, className);
                break;
            case OPERATION_BUILDER_CLASS_MASK:
                write(file, genType, BUILDER_CLASS, curNode, className);
                break;
            case BUILDER_INTERFACE_MASK:
                write(file, genType, BUILDER_INTERFACE, curNode, className);
                break;
            case GENERATE_SERVICE_AND_MANAGER:
                appendHeaderContents(file, pkgString, importsList);
                write(file, genType, RPC_INTERFACE, curNode, className);
                break;
            case GENERATE_EVENT_CLASS:
                appendHeaderContents(file, pkgString, importsList);
                write(file, genType, EVENT, curNode, className);
                break;
            case GENERATE_EVENT_LISTENER_INTERFACE:
                appendHeaderContents(file, pkgString, importsList);
                write(file, genType, EVENT_LISTENER, curNode, className);
                break;
            case GENERATE_EVENT_SUBJECT_CLASS:
                appendHeaderContents(file, pkgString, importsList);
                write(file, genType, EVENT_SUBJECT_CLASS, curNode, className);
                break;
            case GENERATE_IDENTITY_CLASS:
                appendHeaderContents(file, pkgString, importsList);
                write(file, genType, EVENT_SUBJECT_CLASS, curNode, className);
                insertDataIntoJavaFile(file, CLOSE_CURLY_BRACKET);
                break;
            default:
                break;
        }
    }

    /**
     * Appends all the contents into a generated java file.
     *
     * @param file         generated file
     * @param fileName     generated file name
     * @param genType      generated file type
     * @param importsList  list of java imports
     * @param pkg          generated file package
     * @param pluginConfig plugin configurations
     * @throws IOException when fails to append contents
     */
    private static void appendContents(File file, String fileName, int genType, List<String> importsList, String pkg,
                                       YangPluginConfig pluginConfig)
            throws IOException {

        String pkgString = parsePackageString(pkg, importsList);

        switch (genType) {
            case GENERATE_TYPEDEF_CLASS:
                appendHeaderContents(file, pkgString, importsList);
                write(file, fileName, genType, IMPL_CLASS, pluginConfig);
                break;
            case GENERATE_UNION_CLASS:
                appendHeaderContents(file, pkgString, importsList);
                write(file, fileName, genType, IMPL_CLASS, pluginConfig);
                break;
            case GENERATE_ENUM_CLASS:
                appendHeaderContents(file, pkgString, importsList);
                write(file, fileName, genType, ENUM_CLASS, pluginConfig);
                break;
            default:
                break;
        }
    }

    /**
     * Removes base directory path from package and generates package string for file.
     *
     * @param javaPkg     generated java package
     * @param importsList list of imports
     * @return package string
     */
    private static String parsePackageString(String javaPkg, List<String> importsList) {

        javaPkg = parsePkg(getJavaPackageFromPackagePath(javaPkg));
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
     * Appends other contents to interface, impl and typedef classes. for example : ONOS copyright, imports and
     * package.
     *
     * @param file        generated file
     * @param pkg         generated package
     * @param importsList list of imports
     * @throws IOException when fails to append contents
     */
    private static void appendHeaderContents(File file, String pkg, List<String> importsList)
            throws IOException {

        insertDataIntoJavaFile(file, CopyrightHeader.getCopyrightHeader());
        insertDataIntoJavaFile(file, pkg);

        /*
         * TODO: add the file header using comments for snippet of yang file.
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
     * @param file        generated file
     * @param genType     generated file type
     * @param javaDocType java doc type
     * @param curNode     current YANG node
     * @param fileName    file name
     * @throws IOException when fails to write into a file
     */
    private static void write(File file, int genType, JavaDocType javaDocType, YangNode curNode, String fileName)
            throws IOException {

        YangPluginConfig pluginConfig = ((JavaFileInfoContainer) curNode).getJavaFileInfo().getPluginConfig();
        if ((genType & GENERATE_SERVICE_AND_MANAGER) != 0) {
            if (!fileName.matches(REGEX_FOR_ANY_STRING_ENDING_WITH_SERVICE)) {
                insertDataIntoJavaFile(file, getJavaDoc(RPC_MANAGER, fileName, false, pluginConfig));
                insertDataIntoJavaFile(file, JavaCodeSnippetGen.addComponentString());
            } else {
                insertDataIntoJavaFile(file, getJavaDoc(javaDocType, fileName, false, pluginConfig));
            }
        } else {
            insertDataIntoJavaFile(file, getJavaDoc(javaDocType, fileName, false, pluginConfig));
        }
        insertDataIntoJavaFile(file, generateClassDefinition(genType, fileName, curNode));
    }

    /**
     * Writes data to the specific generated file.
     *
     * @param file         generated file
     * @param fileName     file name
     * @param genType      generated file type
     * @param javaDocType  java doc type
     * @param pluginConfig plugin configurations
     * @throws IOException when fails to write into a file
     */
    private static void write(File file, String fileName, int genType, JavaDocType javaDocType,
                              YangPluginConfig pluginConfig)
            throws IOException {
        insertDataIntoJavaFile(file, getJavaDoc(javaDocType, fileName, false, pluginConfig));
        insertDataIntoJavaFile(file, generateClassDefinition(genType, fileName));
    }

    /**
     * Returns set of node identifiers.
     *
     * @param parent parent node
     * @return set of node identifiers
     */
    static List<YangAtomicPath> getSetOfNodeIdentifiers(YangNode parent) {

        List<YangAtomicPath> targets = new ArrayList<>();
        YangNodeIdentifier nodeId;
        List<YangAugment> augments = getListOfAugments(parent);
        for (YangAugment augment : augments) {
            nodeId = augment.getTargetNode().get(0).getNodeIdentifier();

            if (validateNodeIdentifierInSet(nodeId, targets)) {
                targets.add(augment.getTargetNode().get(0));
            }
        }
        return targets;
    }

    /* Returns list of augments.*/
    private static List<YangAugment> getListOfAugments(YangNode parent) {
        List<YangAugment> augments = new ArrayList<>();
        YangNode child = parent.getChild();
        while (child != null) {
            if (child instanceof YangAugment) {
                augments.add((YangAugment) child);
            }
            child = child.getNextSibling();
        }
        return augments;
    }

    /*Validates the set for duplicate names of node identifiers.*/
    private static boolean validateNodeIdentifierInSet(YangNodeIdentifier nodeId, List<YangAtomicPath> targets) {
        boolean isPresent = true;
        for (YangAtomicPath target : targets) {
            if (target.getNodeIdentifier().getName().equals(nodeId.getName())) {
                if (target.getNodeIdentifier().getPrefix() != null) {
                    isPresent = !target.getNodeIdentifier().getPrefix().equals(nodeId.getPrefix());
                } else {
                    isPresent = nodeId.getPrefix() != null;
                }
            }
        }
        return isPresent;
    }

    /**
     * Adds resolved augmented node imports to manager class.
     *
     * @param parent parent node
     */
    public static void addResolvedAugmentedDataNodeImports(YangNode parent) {
        List<YangAtomicPath> targets = getSetOfNodeIdentifiers(parent);
        TempJavaCodeFragmentFiles tempJavaCodeFragmentFiles = ((JavaCodeGeneratorInfo) parent)
                .getTempJavaCodeFragmentFiles();
        YangNode augmentedNode;
        JavaQualifiedTypeInfo javaQualifiedTypeInfo;
        String curNodeName;
        JavaFileInfo parentInfo = ((JavaFileInfoContainer) parent).getJavaFileInfo();
        for (YangAtomicPath nodeId : targets) {
            augmentedNode = nodeId.getResolvedNode().getParent();
            if (((JavaFileInfoContainer) augmentedNode).getJavaFileInfo().getJavaName() != null) {
                curNodeName = ((JavaFileInfoContainer) augmentedNode).getJavaFileInfo().getJavaName();
            } else {
                curNodeName = getCapitalCase(getCamelCase(augmentedNode.getName(), parentInfo.getPluginConfig()
                        .getConflictResolver()));
            }

            javaQualifiedTypeInfo = getQualifiedTypeInfoOfAugmentedNode(augmentedNode, getCapitalCase(curNodeName),
                    parentInfo.getPluginConfig());
            tempJavaCodeFragmentFiles.getServiceTempFiles().getJavaImportData().addImportInfo(javaQualifiedTypeInfo,
                    parentInfo.getJavaName(), parentInfo.getPackage());

        }
    }

    /**
     * Returns qualified type info of augmented node.
     *
     * @param augmentedNode augmented node
     * @param curNodeName   current node name
     * @param pluginConfig  plugin configurations
     * @return qualified type info of augmented node
     */
    private static JavaQualifiedTypeInfo getQualifiedTypeInfoOfAugmentedNode(YangNode augmentedNode, String curNodeName,
                                                                             YangPluginConfig pluginConfig) {
        JavaQualifiedTypeInfo javaQualifiedTypeInfo = getQualifiedTypeInfoOfCurNode(augmentedNode,
                getCapitalCase(curNodeName));
        if (javaQualifiedTypeInfo.getPkgInfo() == null) {
            javaQualifiedTypeInfo.setPkgInfo(getAugmentedNodesPackage(augmentedNode,
                    pluginConfig));
        }
        return javaQualifiedTypeInfo;
    }

    /**
     * Validates if augmented node is imported in parent node.
     *
     * @param javaQualifiedTypeInfo qualified type info
     * @param importData            import data
     * @return true if present in imports
     */
    private static boolean validateQualifiedInfoOfAugmentedNode(JavaQualifiedTypeInfo javaQualifiedTypeInfo,
                                                                JavaImportData importData) {
        for (JavaQualifiedTypeInfo curImportInfo : importData.getImportSet()) {
            if (curImportInfo.getClassInfo()
                    .contentEquals(javaQualifiedTypeInfo.getClassInfo())) {
                return curImportInfo.getPkgInfo()
                        .contentEquals(javaQualifiedTypeInfo.getPkgInfo());
            }
        }
        return true;
    }

    /**
     * Return augmented class name for data methods in manager and service.
     *
     * @param augmentedNode augmented node
     * @param parent        parent node
     * @return augmented class name for data methods in manager and service
     */
    static String getAugmentedClassNameForDataMethods(YangNode augmentedNode, YangNode parent) {
        String curNodeName;
        JavaQualifiedTypeInfo javaQualifiedTypeInfo;
        JavaFileInfo parentInfo = ((JavaFileInfoContainer) parent).getJavaFileInfo();
        YangPluginConfig pluginConfig = parentInfo.getPluginConfig();
        TempJavaServiceFragmentFiles tempJavaServiceFragmentFiles = ((JavaCodeGeneratorInfo) parent)
                .getTempJavaCodeFragmentFiles().getServiceTempFiles();
        if (((JavaFileInfoContainer) augmentedNode).getJavaFileInfo().getJavaName() != null) {
            curNodeName = ((JavaFileInfoContainer) augmentedNode).getJavaFileInfo().getJavaName();
        } else {
            curNodeName = getCapitalCase(getCamelCase(augmentedNode.getName(), pluginConfig
                    .getConflictResolver()));
        }

        javaQualifiedTypeInfo = getQualifiedTypeInfoOfAugmentedNode(augmentedNode,
                getCapitalCase(curNodeName),
                parentInfo.getPluginConfig());
        if (validateQualifiedInfoOfAugmentedNode(javaQualifiedTypeInfo,
                tempJavaServiceFragmentFiles.getJavaImportData())) {
            return javaQualifiedTypeInfo.getClassInfo();
        } else {
            return javaQualifiedTypeInfo.getPkgInfo() + PERIOD + javaQualifiedTypeInfo.getClassInfo();
        }
    }

    /**
     * Returns parent node name for data methods in manager and service.
     *
     * @param parent       parent node
     * @param pluginConfig plugin configurations
     * @return parent node name for data methods in manager and service
     */
    static String getParentNodeNameForDataMethods(YangNode parent, YangPluginConfig pluginConfig) {
        JavaFileInfo parentInfo = ((JavaFileInfoContainer) parent).getJavaFileInfo();
        if (parentInfo.getJavaName() != null) {
            return getCapitalCase(parentInfo.getJavaName());
        }
        return getCapitalCase(getCamelCase(parent.getName(), pluginConfig
                .getConflictResolver()));

    }

}

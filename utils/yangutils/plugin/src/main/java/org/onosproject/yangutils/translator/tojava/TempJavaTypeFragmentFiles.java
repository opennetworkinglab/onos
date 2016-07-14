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
import java.util.ArrayList;
import java.util.List;

import org.onosproject.yangutils.datamodel.YangNode;
import org.onosproject.yangutils.datamodel.YangType;
import org.onosproject.yangutils.datamodel.YangTypeHolder;
import org.onosproject.yangutils.translator.exception.TranslatorException;
import org.onosproject.yangutils.translator.tojava.javamodel.YangJavaType;
import org.onosproject.yangutils.utils.io.impl.YangPluginConfig;

import static org.onosproject.yangutils.translator.tojava.GeneratedJavaFileType.GENERATE_TYPEDEF_CLASS;
import static org.onosproject.yangutils.translator.tojava.GeneratedJavaFileType.GENERATE_UNION_CLASS;
import static org.onosproject.yangutils.translator.tojava.GeneratedTempFileType.CONSTRUCTOR_FOR_TYPE_MASK;
import static org.onosproject.yangutils.translator.tojava.GeneratedTempFileType.FROM_STRING_IMPL_MASK;
import static org.onosproject.yangutils.translator.tojava.GeneratedTempFileType.OF_STRING_IMPL_MASK;
import static org.onosproject.yangutils.translator.tojava.JavaAttributeInfo.getAttributeInfoForTheData;
import static org.onosproject.yangutils.translator.tojava.utils.JavaFileGenerator.generateTypeDefClassFile;
import static org.onosproject.yangutils.translator.tojava.utils.JavaFileGenerator.generateUnionClassFile;
import static org.onosproject.yangutils.utils.io.impl.YangIoUtils.getCamelCase;
import static org.onosproject.yangutils.translator.tojava.utils.MethodsGenerator.getOfMethodStringAndJavaDoc;
import static org.onosproject.yangutils.translator.tojava.utils.MethodsGenerator.getTypeConstructorStringAndJavaDoc;
import static org.onosproject.yangutils.translator.tojava.utils.TempJavaCodeFragmentFilesUtils.closeFile;
import static org.onosproject.yangutils.utils.UtilConstants.EMPTY_STRING;
import static org.onosproject.yangutils.utils.UtilConstants.NEW_LINE;
import static org.onosproject.yangutils.translator.tojava.utils.JavaIdentifierSyntax.createPackage;

/**
 * Represents implementation of java data type code fragments temporary implementations.
 * Maintains the temp files required specific for user defined data type java snippet generation.
 */
public class TempJavaTypeFragmentFiles
        extends TempJavaFragmentFiles {

    /**
     * File name for of string method.
     */
    private static final String OF_STRING_METHOD_FILE_NAME = "OfString";

    /**
     * File name for construction for special type like union, typedef.
     */
    private static final String CONSTRUCTOR_FOR_TYPE_FILE_NAME = "ConstructorForType";

    /**
     * File name for typedef class file name suffix.
     */
    private static final String TYPEDEF_CLASS_FILE_NAME_SUFFIX = EMPTY_STRING;

    /**
     * File name for generated class file for special type like union, typedef
     * suffix.
     */
    private static final String UNION_TYPE_CLASS_FILE_NAME_SUFFIX = EMPTY_STRING;

    /**
     * Temporary file handle for of string method of class.
     */
    private File ofStringImplTempFileHandle;

    /**
     * Temporary file handle for constructor for type class.
     */
    private File constructorForTypeTempFileHandle;

    /**
     * Java file handle for typedef class file.
     */
    private File typedefClassJavaFileHandle;

    /**
     * Java file handle for type class like union, typedef file.
     */
    private File typeClassJavaFileHandle;

    /**
     * Creates an instance of temporary java code fragment.
     *
     * @param javaFileInfo generated java file info
     * @throws IOException when fails to create new file handle
     */
    public TempJavaTypeFragmentFiles(JavaFileInfo javaFileInfo)
            throws IOException {

        super(javaFileInfo);

        /*
         * Initialize getterImpl, attributes, hash code, equals and to strings
         * when generation file type matches to typeDef class mask.
         */
        addGeneratedTempFile(OF_STRING_IMPL_MASK);
        addGeneratedTempFile(CONSTRUCTOR_FOR_TYPE_MASK);
        addGeneratedTempFile(FROM_STRING_IMPL_MASK);

        setOfStringImplTempFileHandle(getTemporaryFileHandle(OF_STRING_METHOD_FILE_NAME));
        setConstructorForTypeTempFileHandle(getTemporaryFileHandle(CONSTRUCTOR_FOR_TYPE_FILE_NAME));

    }

    /**
     * Returns type class constructor method's temporary file handle.
     *
     * @return type class constructor method's temporary file handle
     */

    public File getConstructorForTypeTempFileHandle() {
        return constructorForTypeTempFileHandle;
    }

    /**
     * Sets type class constructor method's temporary file handle.
     *
     * @param constructorForTypeTempFileHandle type class constructor method's
     * temporary file handle
     */
    private void setConstructorForTypeTempFileHandle(File constructorForTypeTempFileHandle) {
        this.constructorForTypeTempFileHandle = constructorForTypeTempFileHandle;
    }

    /**
     * Returns java file handle for typedef class file.
     *
     * @return java file handle for typedef class file
     */
    File getTypedefClassJavaFileHandle() {
        return typedefClassJavaFileHandle;
    }

    /**
     * Sets the java file handle for typedef class file.
     *
     * @param typedefClassJavaFileHandle java file handle
     */
    private void setTypedefClassJavaFileHandle(File typedefClassJavaFileHandle) {
        this.typedefClassJavaFileHandle = typedefClassJavaFileHandle;
    }

    /**
     * Returns java file handle for type class file.
     *
     * @return java file handle for type class file
     */
    File getTypeClassJavaFileHandle() {
        return typeClassJavaFileHandle;
    }

    /**
     * Sets the java file handle for type class file.
     *
     * @param typeClassJavaFileHandle type file handle
     */
    private void setTypeClassJavaFileHandle(File typeClassJavaFileHandle) {
        this.typeClassJavaFileHandle = typeClassJavaFileHandle;
    }

    /**
     * Returns of string method's temporary file handle.
     *
     * @return of string method's temporary file handle
     */

    public File getOfStringImplTempFileHandle() {
        return ofStringImplTempFileHandle;
    }

    /**
     * Set of string method's temporary file handle.
     *
     * @param ofStringImplTempFileHandle of string method's temporary file
     * handle
     */
    private void setOfStringImplTempFileHandle(File ofStringImplTempFileHandle) {
        this.ofStringImplTempFileHandle = ofStringImplTempFileHandle;
    }

    /**
     * Adds all the type in the current data model node as part of the generated
     * temporary file.
     *
     * @param yangTypeHolder YANG java data model node which has type info, eg union /
     * typedef
     * @param pluginConfig plugin configurations for naming conventions
     * @throws IOException IO operation fail
     */
    public void addTypeInfoToTempFiles(YangTypeHolder yangTypeHolder, YangPluginConfig pluginConfig)
            throws IOException {

        List<YangType<?>> typeList = yangTypeHolder.getTypeList();
        if (typeList != null) {
            for (YangType<?> yangType : typeList) {
                if (!(yangType instanceof YangJavaType)) {
                    throw new TranslatorException("Type does not have Java info");
                }
                YangJavaType<?> javaType = (YangJavaType<?>) yangType;
                javaType.updateJavaQualifiedInfo(pluginConfig.getConflictResolver());
                String typeName = javaType.getDataTypeName();
                typeName = getCamelCase(typeName, pluginConfig.getConflictResolver());
                JavaAttributeInfo javaAttributeInfo = getAttributeInfoForTheData(
                        javaType.getJavaQualifiedInfo(),
                        typeName, javaType,
                        getIsQualifiedAccessOrAddToImportList(javaType.getJavaQualifiedInfo()),
                        false);
                addJavaSnippetInfoToApplicableTempFiles((YangNode) yangTypeHolder, javaAttributeInfo,
                        pluginConfig);
            }
        }
    }

    /**
     * Adds the new attribute info to the target generated temporary files for
     * union class.
     *
     * @param hasType the node for which the type is being added as an attribute
     * @param javaAttributeInfo the attribute info that needs to be added to
     * temporary files
     * @param pluginConfig plugin configurations
     * @throws IOException IO operation fail
     */
    private void addJavaSnippetInfoToApplicableTempFiles(YangNode hasType, JavaAttributeInfo javaAttributeInfo,
            YangPluginConfig pluginConfig)
            throws IOException {

        super.addJavaSnippetInfoToApplicableTempFiles(javaAttributeInfo, pluginConfig);

        if ((getGeneratedTempFiles() & OF_STRING_IMPL_MASK) != 0) {
            addOfStringMethod(javaAttributeInfo, pluginConfig);
        }
        if ((getGeneratedTempFiles() & CONSTRUCTOR_FOR_TYPE_MASK) != 0) {
            addTypeConstructor(javaAttributeInfo, pluginConfig);
        }
    }

    /**
     * Adds type constructor.
     *
     * @param attr attribute info
     * @param pluginConfig plugin configurations
     * @throws IOException when fails to append to temporary file
     */
    private void addTypeConstructor(JavaAttributeInfo attr, YangPluginConfig pluginConfig)
            throws IOException {
        appendToFile(getConstructorForTypeTempFileHandle(), getTypeConstructorStringAndJavaDoc(attr,
                getGeneratedJavaClassName(), pluginConfig) + NEW_LINE);
    }

    /**
     * Adds of string for type.
     *
     * @param attr attribute info
     * @param pluginConfig plugin configurations
     * @throws IOException when fails to append to temporary file
     */
    private void addOfStringMethod(JavaAttributeInfo attr, YangPluginConfig pluginConfig)
            throws IOException {
        appendToFile(getOfStringImplTempFileHandle(), getOfMethodStringAndJavaDoc(attr,
                getGeneratedJavaClassName(), pluginConfig)
                + NEW_LINE);
    }

    /**
     * Removes all temporary file handles.
     *
     * @param isErrorOccurred when translator fails to generate java files we
     * need to close all open file handles include temporary files
     * and java files.
     * @throws IOException when failed to delete the temporary files
     */
    @Override
    public void freeTemporaryResources(boolean isErrorOccurred)
            throws IOException {
        boolean isError = isErrorOccurred;

        if ((getGeneratedJavaFiles() & GENERATE_TYPEDEF_CLASS) != 0) {
            closeFile(getTypedefClassJavaFileHandle(), isError);
        }

        if ((getGeneratedJavaFiles() & GENERATE_UNION_CLASS) != 0) {
            closeFile(getTypeClassJavaFileHandle(), isError);
        }

        if ((getGeneratedTempFiles() & CONSTRUCTOR_FOR_TYPE_MASK) != 0) {
            closeFile(getConstructorForTypeTempFileHandle(), true);
        }
        if ((getGeneratedTempFiles() & OF_STRING_IMPL_MASK) != 0) {
            closeFile(getOfStringImplTempFileHandle(), true);
        }
        if ((getGeneratedTempFiles() & FROM_STRING_IMPL_MASK) != 0) {
            closeFile(getFromStringImplTempFileHandle(), true);
        }

        super.freeTemporaryResources(isErrorOccurred);

    }

    /**
     * Constructs java code exit.
     *
     * @param fileType generated file type
     * @param curNode current YANG node
     * @throws IOException when fails to generate java files
     */
    @Override
    public void generateJavaFile(int fileType, YangNode curNode)
            throws IOException {
        List<String> imports = new ArrayList<>();
        if (isAttributePresent()) {
            imports = getJavaImportData().getImports();
        }

        createPackage(curNode);

        /*
         * Creates type def class file.
         */
        if ((fileType & GENERATE_TYPEDEF_CLASS) != 0) {
            addImportsToStringAndHasCodeMethods(curNode, imports);
            setTypedefClassJavaFileHandle(getJavaFileHandle(getJavaClassName(TYPEDEF_CLASS_FILE_NAME_SUFFIX)));
            generateTypeDefClassFile(getTypedefClassJavaFileHandle(), curNode, imports);
        }
        /*
         * Creates type class file.
         */
        if ((fileType & GENERATE_UNION_CLASS) != 0) {
            addImportsToStringAndHasCodeMethods(curNode, imports);
            setTypeClassJavaFileHandle(getJavaFileHandle(getJavaClassName(UNION_TYPE_CLASS_FILE_NAME_SUFFIX)));
            generateUnionClassFile(getTypeClassJavaFileHandle(), curNode, imports);
        }

        /*
         * Close all the file handles.
         */
        freeTemporaryResources(false);
    }
}

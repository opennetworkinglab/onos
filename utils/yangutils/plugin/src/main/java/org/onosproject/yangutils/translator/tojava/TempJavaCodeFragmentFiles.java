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

import java.io.IOException;

import org.onosproject.yangutils.datamodel.YangNode;
import org.onosproject.yangutils.datamodel.YangTypeHolder;
import org.onosproject.yangutils.translator.exception.TranslatorException;
import org.onosproject.yangutils.utils.io.impl.YangPluginConfig;

import static org.onosproject.yangutils.translator.tojava.GeneratedJavaFileType.GENERATE_ENUM_CLASS;
import static org.onosproject.yangutils.translator.tojava.GeneratedJavaFileType.GENERATE_INTERFACE_WITH_BUILDER;
import static org.onosproject.yangutils.translator.tojava.GeneratedJavaFileType.GENERATE_SERVICE_AND_MANAGER;
import static org.onosproject.yangutils.translator.tojava.GeneratedJavaFileType.GENERATE_TYPE_CLASS;

/**
 * Represents implementation of java code fragments temporary implementations.
 * Contains fragment file object of different types of java file.
 * Uses required object(s) to generate the target java file(s).
 */
public class TempJavaCodeFragmentFiles {

    /**
     * Has the temporary files required for bean generated classes.
     */
    private TempJavaBeanFragmentFiles beanTempFiles;

    /**
     * Has the temporary files required for bean generated classes.
     */
    private TempJavaTypeFragmentFiles typeTempFiles;

    /**
     * Has the temporary files required for service generated classes.
     */
    private TempJavaServiceFragmentFiles serviceTempFiles;

    /**
     * Has the temporary files required for enumeration generated classes.
     */
    private TempJavaEnumerationFragmentFiles enumerationTempFiles;

    /**
     * Creates an instance of temporary java code fragment.
     *
     * @param javaFileInfo generated java file info
     * @throws IOException when fails to create new file handle
     */
    public TempJavaCodeFragmentFiles(JavaFileInfo javaFileInfo)
            throws IOException {

        if ((javaFileInfo.getGeneratedFileTypes() & GENERATE_INTERFACE_WITH_BUILDER) != 0) {
            setBeanTempFiles(new TempJavaBeanFragmentFiles(javaFileInfo));
        }

        if ((javaFileInfo.getGeneratedFileTypes() & GENERATE_TYPE_CLASS) != 0) {
            setTypeTempFiles(new TempJavaTypeFragmentFiles(javaFileInfo));
        }

        if ((javaFileInfo.getGeneratedFileTypes() & GENERATE_ENUM_CLASS) != 0) {
            setEnumerationTempFiles(new TempJavaEnumerationFragmentFiles(javaFileInfo));
        }

        if ((javaFileInfo.getGeneratedFileTypes() & GENERATE_SERVICE_AND_MANAGER) != 0) {
            setServiceTempFiles(new TempJavaServiceFragmentFiles(javaFileInfo));
        }

    }

    /**
     * Retrieves the temp file handle for bean file generation.
     *
     * @return temp file handle for bean file generation
     */
    public TempJavaBeanFragmentFiles getBeanTempFiles() {
        return beanTempFiles;
    }

    /**
     * Sets temp file handle for bean file generation.
     *
     * @param beanTempFiles temp file handle for bean file generation
     */
    public void setBeanTempFiles(TempJavaBeanFragmentFiles beanTempFiles) {
        this.beanTempFiles = beanTempFiles;
    }

    /**
     * Retrieves the temp file handle for data type file generation.
     *
     * @return temp file handle for data type file generation
     */
    public TempJavaTypeFragmentFiles getTypeTempFiles() {
        return typeTempFiles;
    }

    /**
     * Sets temp file handle for data type file generation.
     *
     * @param typeTempFiles temp file handle for data type file generation
     */
    public void setTypeTempFiles(TempJavaTypeFragmentFiles typeTempFiles) {
        this.typeTempFiles = typeTempFiles;
    }

    /**
     * Retrieves the temp file handle for service file generation.
     *
     * @return temp file handle for service file generation
     */
    public TempJavaServiceFragmentFiles getServiceTempFiles() {
        return serviceTempFiles;
    }

    /**
     * Sets temp file handle for service file generation.
     *
     * @param serviceTempFiles temp file handle for service file generation
     */
    public void setServiceTempFiles(TempJavaServiceFragmentFiles serviceTempFiles) {
        this.serviceTempFiles = serviceTempFiles;
    }

    /**
     * Retrieves the temp file handle for enumeration file generation.
     *
     * @return temp file handle for enumeration file generation
     */
    public TempJavaEnumerationFragmentFiles getEnumerationTempFiles() {
        return enumerationTempFiles;
    }

    /**
     * Sets temp file handle for enumeration file generation.
     *
     * @param enumerationTempFiles temp file handle for enumeration file generation
     */
    public void setEnumerationTempFiles(
            TempJavaEnumerationFragmentFiles enumerationTempFiles) {
        this.enumerationTempFiles = enumerationTempFiles;
    }

    /**
     * Constructs java code exit.
     *
     * @param fileType generated file type
     * @param curNode current YANG node
     * @throws IOException when fails to generate java files
     */
    public void generateJavaFile(int fileType, YangNode curNode)
            throws IOException {

        if ((fileType & GENERATE_INTERFACE_WITH_BUILDER) != 0) {
            getBeanTempFiles().generateJavaFile(fileType, curNode);
        }

        /*
         * Creates user defined data type class file.
         */
        if ((fileType & GENERATE_TYPE_CLASS) != 0) {
            getTypeTempFiles().generateJavaFile(fileType, curNode);
        }

        /*
         * Creates service and manager class file.
         */
        if (fileType == GENERATE_SERVICE_AND_MANAGER) {
            getServiceTempFiles().generateJavaFile(GENERATE_SERVICE_AND_MANAGER, curNode);
        }

        /*
         * Creats enumeration class file.
         */
        if (fileType == GENERATE_ENUM_CLASS) {
            getEnumerationTempFiles().generateJavaFile(GENERATE_ENUM_CLASS, curNode);
        }

        freeTemporaryResources(false);
    }

    /**
     * Adds the new attribute info to the target generated temporary files.
     *
     * @param newAttrInfo the attribute info that needs to be added to temporary
     * files
     * @param pluginConfig plugin configurations
     * @throws IOException IO operation fail
     */
    public void addJavaSnippetInfoToApplicableTempFiles(JavaAttributeInfo newAttrInfo,
            YangPluginConfig pluginConfig)
            throws IOException {

        if (getBeanTempFiles() != null) {
            getBeanTempFiles()
                    .addJavaSnippetInfoToApplicableTempFiles(newAttrInfo, pluginConfig);
        }

        /**
         * Creates user defined data type class file.
         */
        if (getTypeTempFiles() != null) {
            getTypeTempFiles()
                    .addJavaSnippetInfoToApplicableTempFiles(newAttrInfo, pluginConfig);
        }
    }

    /**
     * Add all the type in the current data model node as part of the
     * generated temporary file.
     *
     * @param yangTypeHolder YANG java data model node which has type info, eg union / typedef
     * @param pluginConfig plugin configurations for naming convention
     * @throws IOException IO operation fail
     */
    public void addTypeInfoToTempFiles(YangTypeHolder yangTypeHolder, YangPluginConfig pluginConfig)
            throws IOException {
        getTypeTempFiles()
                .addTypeInfoToTempFiles(yangTypeHolder, pluginConfig);
    }

    /**
     * Adds build method for interface.
     *
     * @param pluginConfig plugin configurations
     * @return build method for interface
     * @throws IOException when fails to append to temporary file
     */
    public String addBuildMethodForInterface(YangPluginConfig pluginConfig)
            throws IOException {
        if (getBeanTempFiles() != null) {
            return getBeanTempFiles().addBuildMethodForInterface(pluginConfig);
        }
        throw new TranslatorException("build method only supported for bean class");
    }

    /**
     * Adds default constructor for class.
     *
     * @param modifier modifier for constructor.
     * @param toAppend string which need to be appended with the class name
     * @param pluginConfig plugin configurations
     * @return default constructor for class
     * @throws IOException when fails to append to file
     */
    public String addDefaultConstructor(String modifier, String toAppend, YangPluginConfig pluginConfig)
            throws IOException {
        if (getTypeTempFiles() != null) {
            return getTypeTempFiles()
                    .addDefaultConstructor(modifier, toAppend, pluginConfig);
        }

        if (getBeanTempFiles() != null) {
            return getBeanTempFiles().addDefaultConstructor(modifier, toAppend, pluginConfig);
        }

        throw new TranslatorException("default constructor should not be added");
    }

    /**
     * Adds build method's implementation for class.
     *
     * @return build method implementation for class
     * @throws IOException when fails to append to temporary file
     */
    public String addBuildMethodImpl()
            throws IOException {
        if (getBeanTempFiles() != null) {
            return getBeanTempFiles().addBuildMethodImpl();
        }

        throw new TranslatorException("build should not be added");
    }

    /**
     * Removes all temporary file handles.
     *
     * @param isErrorOccurred when translator fails to generate java files we need to close
     * all open file handles include temporary files and java files.
     * @throws IOException when failed to delete the temporary files
     */
    public void freeTemporaryResources(boolean isErrorOccurred)
            throws IOException {

        if (getBeanTempFiles() != null) {
            getBeanTempFiles().freeTemporaryResources(isErrorOccurred);
        }

        if (getTypeTempFiles() != null) {
            getTypeTempFiles().freeTemporaryResources(isErrorOccurred);
        }

        if (getEnumerationTempFiles() != null) {
            getEnumerationTempFiles().freeTemporaryResources(isErrorOccurred);
        }

        if (getServiceTempFiles() != null) {
            getServiceTempFiles().freeTemporaryResources(isErrorOccurred);
        }

    }

}

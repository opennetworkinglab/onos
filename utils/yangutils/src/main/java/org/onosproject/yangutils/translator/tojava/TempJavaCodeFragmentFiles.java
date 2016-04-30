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

import org.onosproject.yangutils.datamodel.YangTypeContainer;
import org.onosproject.yangutils.datamodel.YangNode;
import org.onosproject.yangutils.translator.exception.TranslatorException;

import static org.onosproject.yangutils.translator.tojava.GeneratedJavaFileType.GENERATE_ENUM_CLASS;
import static org.onosproject.yangutils.translator.tojava.GeneratedJavaFileType.GENERATE_INTERFACE_WITH_BUILDER;
import static org.onosproject.yangutils.translator.tojava.GeneratedJavaFileType.GENERATE_RPC_INTERFACE;
import static org.onosproject.yangutils.translator.tojava.GeneratedJavaFileType.GENERATE_TYPE_CLASS;
import static org.onosproject.yangutils.translator.tojava.utils.JavaFileGenerator.getExtendsList;

/**
 * Represents implementation of java code fragments temporary implementations.
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

        /**
         * Creates user defined data type class file.
         */
        if ((javaFileInfo.getGeneratedFileTypes() & GENERATE_TYPE_CLASS) != 0) {
            setTypeTempFiles(new TempJavaTypeFragmentFiles(javaFileInfo));
        }

        /**
         * Creates enumeration class file.
         */
        if ((javaFileInfo.getGeneratedFileTypes() & GENERATE_ENUM_CLASS) != 0) {
            setEnumerationTempFiles(new TempJavaEnumerationFragmentFiles(javaFileInfo));
        }

        if ((javaFileInfo.getGeneratedFileTypes() & GENERATE_RPC_INTERFACE) != 0) {
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

        if (getBeanTempFiles() != null) {
            getBeanTempFiles().generateJavaFile(fileType, curNode);
        }

        /**
         * Creates user defined data type class file.
         */
        if (getTypeTempFiles() != null) {
            getTypeTempFiles().generateJavaFile(fileType, curNode);
        }

    }

    /**
     * Adds the new attribute info to the target generated temporary files.
     *
     * @param newAttrInfo the attribute info that needs to be added to temporary
     * files
     * @throws IOException IO operation fail
     */
    public void addJavaSnippetInfoToApplicableTempFiles(JavaAttributeInfo newAttrInfo)
            throws IOException {

        if (getBeanTempFiles() != null) {
            getBeanTempFiles()
                    .addJavaSnippetInfoToApplicableTempFiles(newAttrInfo);
        }

        /**
         * Creates user defined data type class file.
         */
        if (getTypeTempFiles() != null) {
            getTypeTempFiles()
                    .addJavaSnippetInfoToApplicableTempFiles(newAttrInfo);
        }
    }

    /**
     * Adds all the leaves in the current data model node as part of the
     * generated temporary file.
     *
     * @param curNode java file info of the generated file
     * @throws IOException IO operation fail
     */
    public void addCurNodeLeavesInfoToTempFiles(YangNode curNode)
            throws IOException {

        if (getBeanTempFiles() != null) {
            getBeanTempFiles().addCurNodeLeavesInfoToTempFiles(curNode);
        }

    }

    /**
     * Add all the type in the current data model node as part of the
     * generated temporary file.
     *
     * @param yangTypeContainer YANG java data model node which has type info, eg union / typedef
     * @throws IOException IO operation fail
     */
    public void addTypeInfoToTempFiles(YangTypeContainer yangTypeContainer)
            throws IOException {

        if (getTypeTempFiles() != null) {
            getTypeTempFiles()
                    .addTypeInfoToTempFiles(yangTypeContainer);
        }
    }

    /**
     * Adds class to the extends list.
     *
     * @param extend class to be extended
     */
    public void addToExtendsList(String extend) {
        getExtendsList().add(extend);
    }

    /**
     * Adds build method for interface.
     *
     * @return build method for interface
     * @throws IOException when fails to append to temporary file
     */
    public String addBuildMethodForInterface()
            throws IOException {
        if (getBeanTempFiles() != null) {
            return getBeanTempFiles().addBuildMethodForInterface();
        }
        throw new TranslatorException("build method only supported for bean class");
    }

    /**
     * Adds default constructor for class.
     *
     * @param modifier modifier for constructor.
     * @param toAppend string which need to be appended with the class name
     * @return default constructor for class
     * @throws IOException when fails to append to file
     */
    public String addDefaultConstructor(String modifier, String toAppend)
            throws IOException {
        if (getTypeTempFiles() != null) {
            return getTypeTempFiles()
                    .addDefaultConstructor(modifier, toAppend);
        }

        if (getBeanTempFiles() != null) {
            return getBeanTempFiles().addDefaultConstructor(modifier, toAppend);
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
    public void close(boolean isErrorOccurred)
            throws IOException {

        if (getBeanTempFiles() != null) {
            getBeanTempFiles().close(isErrorOccurred);
        }

        if (getTypeTempFiles() != null) {
            getTypeTempFiles().close(isErrorOccurred);
        }

        if (getEnumerationTempFiles() != null) {
            getEnumerationTempFiles().close(isErrorOccurred);
        }
    }


    /**
     * Adds enum attributes to temporary files.
     *
     * @param curNode current YANG node
     * @throws IOException when fails to do IO operations
     */
    public void addEnumAttributeToTempFiles(YangNode curNode)
            throws IOException {

        if (getEnumerationTempFiles() != null) {
            getEnumerationTempFiles().addEnumAttributeToTempFiles(curNode);
            return;
        }

        throw new TranslatorException("build should not be added");
    }

}

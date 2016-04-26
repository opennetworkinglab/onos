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
package org.onosproject.yangutils.translator.tojava.javamodel;

import java.io.IOException;
import org.onosproject.yangutils.datamodel.YangChoice;
import org.onosproject.yangutils.translator.exception.TranslatorException;
import org.onosproject.yangutils.translator.tojava.JavaCodeGenerator;
import org.onosproject.yangutils.translator.tojava.JavaFileInfo;
import org.onosproject.yangutils.translator.tojava.JavaImportData;
import org.onosproject.yangutils.translator.tojava.TempJavaCodeFragmentFiles;
import org.onosproject.yangutils.translator.tojava.utils.YangPluginConfig;

import static org.onosproject.yangutils.translator.tojava.GeneratedJavaFileType.INTERFACE_MASK;
import static org.onosproject.yangutils.translator.tojava.utils.YangJavaModelUtils.generateCodeOfNode;

/**
 * Represents choice information extended to support java code generation.
 */
public class YangJavaChoice extends YangChoice implements JavaCodeGeneratorInfo, JavaCodeGenerator {

    /**
     * Contains the information of the java file being generated.
     */
    private JavaFileInfo javaFileInfo;

    /**
     * Contains information of the imports to be inserted in the java file
     * generated.
     */
    private JavaImportData javaImportData;

    /**
     * File handle to maintain temporary java code fragments as per the code
     * snippet types.
     */
    private TempJavaCodeFragmentFiles tempFileHandle;

    /**
     * Creates YANG java choice object.
     */
    public YangJavaChoice() {
        super();
        setJavaFileInfo(new JavaFileInfo());
        setJavaImportData(new JavaImportData());
        getJavaFileInfo().setGeneratedFileTypes(INTERFACE_MASK);
    }

    /**
     * Returns the generated java file information.
     *
     * @return generated java file information
     */
    @Override
    public JavaFileInfo getJavaFileInfo() {
        if (javaFileInfo == null) {
            throw new TranslatorException("Missing java info in java datamodel node");
        }
        return javaFileInfo;
    }

    /**
     * Sets the java file info object.
     *
     * @param javaInfo java file info object
     */
    @Override
    public void setJavaFileInfo(JavaFileInfo javaInfo) {
        javaFileInfo = javaInfo;
    }

    /**
     * Returns the data of java imports to be included in generated file.
     *
     * @return data of java imports to be included in generated file
     */
    @Override
    public JavaImportData getJavaImportData() {
        return javaImportData;
    }

    /**
     * Sets the data of java imports to be included in generated file.
     *
     * @param javaImportData data of java imports to be included in generated
     *            file
     */
    @Override
    public void setJavaImportData(JavaImportData javaImportData) {
        this.javaImportData = javaImportData;
    }

    /**
     * Returns the temporary file handle.
     *
     * @return temporary file handle
     */
    @Override
    public TempJavaCodeFragmentFiles getTempJavaCodeFragmentFiles() {
        return tempFileHandle;
    }

    /**
     * Sets temporary file handle.
     *
     * @param fileHandle temporary file handle
     */
    @Override
    public void setTempJavaCodeFragmentFiles(TempJavaCodeFragmentFiles fileHandle) {
        tempFileHandle = fileHandle;
    }

    /**
     * Prepare the information for java code generation corresponding to YANG
     * choice info.
     *
     * @param yangPlugin YANG plugin config
     * @throws IOException IO operation fail
     */
    @Override
    public void generateCodeEntry(YangPluginConfig yangPlugin) throws IOException {
        generateCodeOfNode(this, yangPlugin, false);
    }

    /**
     * Creates a java file using the YANG choice info.
     */
    @Override
    public void generateCodeExit() throws IOException {
        getTempJavaCodeFragmentFiles().generateJavaFile(INTERFACE_MASK, this);
    }
}

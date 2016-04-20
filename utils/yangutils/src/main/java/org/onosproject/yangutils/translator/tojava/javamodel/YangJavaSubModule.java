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
import org.onosproject.yangutils.datamodel.YangBelongsTo;
import org.onosproject.yangutils.datamodel.YangSubModule;
import org.onosproject.yangutils.translator.exception.TranslatorException;
import org.onosproject.yangutils.translator.tojava.JavaCodeGenerator;
import org.onosproject.yangutils.translator.tojava.JavaFileInfo;
import org.onosproject.yangutils.translator.tojava.JavaImportData;
import org.onosproject.yangutils.translator.tojava.TempJavaCodeFragmentFiles;
import org.onosproject.yangutils.translator.tojava.utils.YangJavaModelUtils;
import org.onosproject.yangutils.translator.tojava.utils.YangPluginConfig;

import static org.onosproject.yangutils.translator.tojava.GeneratedJavaFileType.GENERATE_MANAGER_WITH_RPC;
import static org.onosproject.yangutils.translator.tojava.utils.JavaIdentifierSyntax.getRootPackage;

/**
 * Represents sub module information extended to support java code generation.
 */
public class YangJavaSubModule extends YangSubModule implements JavaCodeGeneratorInfo, JavaCodeGenerator {

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
     * Creates YANG java sub module object.
     */
    public YangJavaSubModule() {
        super();
        setJavaFileInfo(new JavaFileInfo());
        setJavaImportData(new JavaImportData());
        getJavaFileInfo().setGeneratedFileTypes(GENERATE_MANAGER_WITH_RPC);
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
     *                       file
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
     * Returns the name space of the module to which the sub module belongs to.
     *
     * @param belongsToInfo Information of the module to which the sub module
     *                      belongs
     * @return the name space string of the module.
     */
    private String getNameSpaceFromModule(YangBelongsTo belongsToInfo) {
        // TODO Auto-generated method stub
        return "";
    }

    /**
     * Prepares the information for java code generation corresponding to YANG
     * submodule info.
     *
     * @param yangPlugin YANG plugin config
     * @throws IOException IO operation fail
     */
    @Override
    public void generateCodeEntry(YangPluginConfig yangPlugin) throws IOException {
        String subModulePkg = getRootPackage(getVersion(), getNameSpaceFromModule(getBelongsTo()),
                getRevision().getRevDate());
        YangJavaModelUtils.generateCodeOfRootNode(this, yangPlugin, subModulePkg);
    }

    /**
     * Creates a java file using the YANG submodule info.
     */
    @Override
    public void generateCodeExit() {
        // TODO Auto-generated method stub
    }
}

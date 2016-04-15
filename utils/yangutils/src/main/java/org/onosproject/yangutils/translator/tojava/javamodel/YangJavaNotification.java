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

import org.onosproject.yangutils.datamodel.YangNotification;
import org.onosproject.yangutils.translator.exception.TranslatorException;
import org.onosproject.yangutils.translator.tojava.HasJavaFileInfo;
import org.onosproject.yangutils.translator.tojava.HasJavaImportData;
import org.onosproject.yangutils.translator.tojava.HasTempJavaCodeFragmentFiles;
import org.onosproject.yangutils.translator.tojava.JavaCodeGenerator;
import org.onosproject.yangutils.translator.tojava.JavaFileInfo;
import org.onosproject.yangutils.translator.tojava.JavaImportData;
import org.onosproject.yangutils.translator.tojava.TempJavaCodeFragmentFiles;
import org.onosproject.yangutils.translator.tojava.utils.YangPluginConfig;

import static org.onosproject.yangutils.translator.tojava.GeneratedJavaFileType.GENERATE_INTERFACE_WITH_BUILDER;
import static org.onosproject.yangutils.translator.tojava.utils.JavaIdentifierSyntax.getCamelCase;
import static org.onosproject.yangutils.translator.tojava.utils.JavaIdentifierSyntax.getCaptialCase;
import static org.onosproject.yangutils.translator.tojava.utils.JavaIdentifierSyntax.getCurNodePackage;
import static org.onosproject.yangutils.translator.tojava.utils.JavaIdentifierSyntax.getPackageDirPathFromJavaJPackage;
import static org.onosproject.yangutils.utils.io.impl.YangIoUtils.getAbsolutePackagePath;

/**
 * Represents notification information extended to support java code generation.
 */
public class YangJavaNotification extends YangNotification
        implements JavaCodeGenerator, HasJavaFileInfo,
        HasJavaImportData, HasTempJavaCodeFragmentFiles {

    /**
     * Contains information of the java file being generated.
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
     * Creates an instance of java Notification.
     */
    public YangJavaNotification() {
        super();
        setJavaFileInfo(new JavaFileInfo());
        setJavaImportData(new JavaImportData());
        getJavaFileInfo().setGeneratedFileTypes(GENERATE_INTERFACE_WITH_BUILDER);
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
     * notification info.
     *
     * @param yangPlugin YANG plugin config
     * @throws IOException IO operation fail
     */
    @Override
    public void generateCodeEntry(YangPluginConfig yangPlugin) throws IOException {

        getJavaFileInfo().setJavaName(getCaptialCase(getCamelCase(getName(), yangPlugin.getConflictResolver())));
        getJavaFileInfo().setPackage(getCurNodePackage(this));
        getJavaFileInfo().setPackageFilePath(
                getPackageDirPathFromJavaJPackage(getJavaFileInfo().getPackage()));
        getJavaFileInfo().setBaseCodeGenPath(yangPlugin.getCodeGenDir());

        String absolutePath = getAbsolutePackagePath(
                getJavaFileInfo().getBaseCodeGenPath(),
                getJavaFileInfo().getPackageFilePath());

        setTempJavaCodeFragmentFiles(new TempJavaCodeFragmentFiles(
                getJavaFileInfo().getGeneratedFileTypes(), absolutePath,
                getJavaFileInfo().getJavaName()));

        getTempJavaCodeFragmentFiles().addCurNodeLeavesInfoToTempFiles(this);

        getTempJavaCodeFragmentFiles().addCurNodeInfoInParentTempFile(this, false);
    }

    /**
     * Creates a java file using the YANG notification info.
     */
    @Override
    public void generateCodeExit() {
        // TODO Auto-generated method stub

    }
}

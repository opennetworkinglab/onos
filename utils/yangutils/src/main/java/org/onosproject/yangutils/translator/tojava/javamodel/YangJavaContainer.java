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
package org.onosproject.yangutils.translator.tojava.javamodel;

import java.io.IOException;

import org.onosproject.yangutils.datamodel.YangContainer;
import org.onosproject.yangutils.translator.tojava.HasJavaFileInfo;
import org.onosproject.yangutils.translator.tojava.HasJavaImportData;
import org.onosproject.yangutils.translator.tojava.HasTempJavaCodeFragmentFiles;
import org.onosproject.yangutils.translator.tojava.JavaCodeGenerator;
import org.onosproject.yangutils.translator.tojava.JavaFileInfo;
import org.onosproject.yangutils.translator.tojava.JavaImportData;
import org.onosproject.yangutils.translator.tojava.TempJavaCodeFragmentFiles;

import static org.onosproject.yangutils.translator.tojava.GeneratedJavaFileType.GENERATE_INTERFACE_WITH_BUILDER;
import static org.onosproject.yangutils.translator.tojava.utils.JavaIdentifierSyntax.getCamelCase;
import static org.onosproject.yangutils.translator.tojava.utils.JavaIdentifierSyntax.getCaptialCase;
import static org.onosproject.yangutils.translator.tojava.utils.JavaIdentifierSyntax.getCurNodePackage;
import static org.onosproject.yangutils.translator.tojava.utils.JavaIdentifierSyntax.getPackageDirPathFromJavaJPackage;
import static org.onosproject.yangutils.utils.io.impl.YangIoUtils.getAbsolutePackagePath;

/**
 * Container information extended to support java code generation.
 */
public class YangJavaContainer extends YangContainer
        implements JavaCodeGenerator, HasJavaFileInfo,
        HasJavaImportData, HasTempJavaCodeFragmentFiles {

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
     * Default constructor.
     */
    public YangJavaContainer() {
        super();
        setJavaFileInfo(new JavaFileInfo());
        setJavaImportData(new JavaImportData());
        getJavaFileInfo().setGeneratedFileTypes(GENERATE_INTERFACE_WITH_BUILDER);
    }

    /**
     * Get the generated java file information.
     *
     * @return generated java file information
     */
    @Override
    public JavaFileInfo getJavaFileInfo() {

        if (javaFileInfo == null) {
            throw new RuntimeException("Missing java info in java datamodel node");
        }
        return javaFileInfo;
    }

    /**
     * Set the java file info object.
     *
     * @param javaInfo java file info object
     */
    @Override
    public void setJavaFileInfo(JavaFileInfo javaInfo) {

        javaFileInfo = javaInfo;
    }

    /**
     * Get the data of java imports to be included in generated file.
     *
     * @return data of java imports to be included in generated file
     */
    @Override
    public JavaImportData getJavaImportData() {

        return javaImportData;
    }

    /**
     * Set the data of java imports to be included in generated file.
     *
     * @param javaImportData data of java imports to be included in generated
     *            file
     */
    @Override
    public void setJavaImportData(JavaImportData javaImportData) {

        this.javaImportData = javaImportData;
    }

    /**
     * Get the temporary file handle.
     *
     * @return temporary file handle
     */
    @Override
    public TempJavaCodeFragmentFiles getTempJavaCodeFragmentFiles() {

        if (tempFileHandle == null) {
            throw new RuntimeException("Missing temp file handle for current node "
                    + getJavaFileInfo().getJavaName());
        }
        return tempFileHandle;
    }

    /**
     * Set temporary file handle.
     *
     * @param fileHandle temporary file handle
     */
    @Override
    public void setTempJavaCodeFragmentFiles(TempJavaCodeFragmentFiles fileHandle) {

        tempFileHandle = fileHandle;
    }

    /**
     * Prepare the information for java code generation corresponding to YANG
     * container info.
     *
     * @param codeGenDir code generation directory
     * @throws IOException IO operation fail
     */
    @Override
    public void generateCodeEntry(String codeGenDir) throws IOException {

        getJavaFileInfo().setJavaName(getCaptialCase(getCamelCase(getName())));
        getJavaFileInfo().setPackage(getCurNodePackage(this));
        getJavaFileInfo().setPackageFilePath(
                getPackageDirPathFromJavaJPackage(getJavaFileInfo().getPackage()));
        getJavaFileInfo().setBaseCodeGenPath(codeGenDir);

        String absloutePath = getAbsolutePackagePath(
                getJavaFileInfo().getBaseCodeGenPath(),
                getJavaFileInfo().getPackageFilePath());

        setTempJavaCodeFragmentFiles(new TempJavaCodeFragmentFiles(
                getJavaFileInfo().getGeneratedFileTypes(), absloutePath,
                getJavaFileInfo().getJavaName()));

        getTempJavaCodeFragmentFiles().addCurNodeLeavesInfoToTempFiles(this);

        getTempJavaCodeFragmentFiles().addCurNodeInfoInParentTempFile(this, false);
    }

    /**
     * Create a java file using the YANG grouping info.
     *
     * @throws IOException IO operation fail
     */
    @Override
    public void generateCodeExit() throws IOException {

        getTempJavaCodeFragmentFiles().generateJavaFile(GENERATE_INTERFACE_WITH_BUILDER, this);
    }

}

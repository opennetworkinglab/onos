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

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.onosproject.yangutils.datamodel.YangIdentity;
import org.onosproject.yangutils.translator.exception.TranslatorException;
import org.onosproject.yangutils.translator.tojava.JavaCodeGenerator;
import org.onosproject.yangutils.translator.tojava.JavaCodeGeneratorInfo;
import org.onosproject.yangutils.translator.tojava.JavaFileInfo;
import org.onosproject.yangutils.translator.tojava.JavaImportData;
import org.onosproject.yangutils.translator.tojava.JavaQualifiedTypeInfo;
import org.onosproject.yangutils.translator.tojava.TempJavaCodeFragmentFiles;
import org.onosproject.yangutils.utils.io.impl.YangPluginConfig;

import static org.onosproject.yangutils.translator.tojava.GeneratedJavaFileType.GENERATE_IDENTITY_CLASS;
import static org.onosproject.yangutils.translator.tojava.GeneratedJavaFileType.GENERATE_INTERFACE_WITH_BUILDER;
import static org.onosproject.yangutils.translator.tojava.YangJavaModelUtils.updatePackageInfo;
import static org.onosproject.yangutils.translator.tojava.utils.JavaFileGeneratorUtils.getFileObject;
import static org.onosproject.yangutils.translator.tojava.utils.JavaFileGeneratorUtils.initiateJavaFileGeneration;
import static org.onosproject.yangutils.translator.tojava.utils.JavaIdentifierSyntax.createPackage;
import static org.onosproject.yangutils.utils.io.impl.FileSystemUtil.closeFile;
import static org.onosproject.yangutils.utils.io.impl.YangIoUtils.getCapitalCase;

/**
 * Represents input information extended to support java code generation.
 */
public class YangJavaIdentity extends YangIdentity
        implements JavaCodeGeneratorInfo, JavaCodeGenerator {

    //File type extension for java classes.
    private static final String JAVA_FILE_EXTENSION = ".java";


    //Contains the information of the java file being generated.
    private JavaFileInfo javaFileInfo;

    //Contains the information of the importd.
    private transient JavaImportData importData;

    /**
     * File handle to maintain temporary java code fragments as per the code
     * snippet types.
     */
    private TempJavaCodeFragmentFiles tempFileHandle;

    /**
     * Creates YANG java container object.
     */
    public YangJavaIdentity() {
        setJavaFileInfo(new JavaFileInfo());
        getJavaFileInfo().setGeneratedFileTypes(GENERATE_INTERFACE_WITH_BUILDER);
        importData = new JavaImportData();
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
     * container info.
     *
     * @param yangPlugin YANG plugin config
     * @throws TranslatorException translator operation fail
     */
    @Override
    public void generateCodeEntry(YangPluginConfig yangPlugin) throws TranslatorException {
        try {
            updatePackageInfo(this, yangPlugin);
            JavaQualifiedTypeInfo basePkgInfo = new JavaQualifiedTypeInfo();
            String className = getCapitalCase(getJavaFileInfo().getJavaName());
            String path = getJavaFileInfo().getPackageFilePath();
            createPackage(this);
            List<String> imports = null;
            boolean isQualified = false;

            if (getBaseNode() != null && getBaseNode().getReferredIdentity() != null) {
                if (!(getBaseNode().getReferredIdentity() instanceof YangJavaIdentity)) {
                    throw new TranslatorException("Failed to prepare generate code entry for base node");
                }
                YangJavaIdentity baseIdentity = (YangJavaIdentity) getBaseNode().getReferredIdentity();
                String baseClassName = getCapitalCase(baseIdentity.getJavaFileInfo().getJavaName());
                String basePkg =  baseIdentity.getJavaFileInfo().getPackage();
                basePkgInfo.setClassInfo(baseClassName);
                basePkgInfo.setPkgInfo(basePkg);
                isQualified = importData.addImportInfo(basePkgInfo, className, getJavaFileInfo().getPackage());
                if (!isQualified) {
                    imports = importData.getImports();
                }
            }

            File file = getFileObject(path, className, JAVA_FILE_EXTENSION, getJavaFileInfo().getBaseCodeGenPath());

            initiateJavaFileGeneration(file, GENERATE_IDENTITY_CLASS, imports, this, className);
            closeFile(file, false);
        } catch (IOException e) {
            throw new TranslatorException(
                    "Failed to prepare generate code entry for identity node " + this.getName());
        }
    }

    /**
     * Create a java file using the YANG container info.
     *
     * @throws TranslatorException translator operation fail
     */
    @Override
    public void generateCodeExit() throws TranslatorException {
        /* Do nothing, file is already generated in entry*/
    }
}


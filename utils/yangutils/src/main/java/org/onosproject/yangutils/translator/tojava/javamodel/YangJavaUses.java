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

import org.onosproject.yangutils.datamodel.YangUses;
import org.onosproject.yangutils.translator.tojava.HasJavaFileInfo;
import org.onosproject.yangutils.translator.tojava.HasJavaImportData;
import org.onosproject.yangutils.translator.tojava.JavaCodeGenerator;
import org.onosproject.yangutils.translator.tojava.JavaFileInfo;
import org.onosproject.yangutils.translator.tojava.JavaImportData;

import static org.onosproject.yangutils.translator.tojava.GeneratedJavaFileType.GENERATE_INTERFACE_WITH_BUILDER;
import static org.onosproject.yangutils.translator.tojava.utils.JavaIdentifierSyntax.getCamelCase;
import static org.onosproject.yangutils.translator.tojava.utils.JavaIdentifierSyntax.getCaptialCase;
import static org.onosproject.yangutils.translator.tojava.utils.JavaIdentifierSyntax.getCurNodePackage;
import static org.onosproject.yangutils.translator.tojava.utils.JavaIdentifierSyntax.getPackageDirPathFromJavaJPackage;

/**
 * Uses information extended to support java code generation.
 */
public class YangJavaUses extends YangUses implements JavaCodeGenerator, HasJavaFileInfo, HasJavaImportData {

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
     * Default constructor.
     */
    public YangJavaUses() {
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
     * Prepare the information for java code generation corresponding to YANG
     * container info.
     *
     * @param codeGenDir code generation directory
     */
    @Override
    public void generateCodeEntry(String codeGenDir) {
        getJavaFileInfo().setJavaName(getCaptialCase(getCamelCase(getName())));
        getJavaFileInfo().setPackage(getCurNodePackage(this));
        getJavaFileInfo().setPackageFilePath(
                getPackageDirPathFromJavaJPackage(getJavaFileInfo().getPackage()));
        getJavaFileInfo().setBaseCodeGenPath(codeGenDir);
        //TODO:addCurNodeLeavesInfoToTempFiles(this);
        //TODO:addCurNodeInfoInParentTempFile(this, false);
    }

    /**
     * Create a java file using the YANG grouping info.
     */
    @Override
    public void generateCodeExit() {
        // TODO Auto-generated method stub

    }
}

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

import java.io.IOException;
import org.onosproject.yangutils.datamodel.YangLeavesHolder;
import org.onosproject.yangutils.datamodel.YangNode;
import org.onosproject.yangutils.translator.tojava.TempJavaCodeFragmentFiles;
import org.onosproject.yangutils.translator.tojava.javamodel.JavaCodeGeneratorInfo;

import static org.onosproject.yangutils.translator.tojava.utils.JavaIdentifierSyntax.getCamelCase;
import static org.onosproject.yangutils.translator.tojava.utils.JavaIdentifierSyntax.getCaptialCase;
import static org.onosproject.yangutils.translator.tojava.utils.JavaIdentifierSyntax.getCurNodePackage;
import static org.onosproject.yangutils.translator.tojava.utils.JavaIdentifierSyntax.getPackageDirPathFromJavaJPackage;
import static org.onosproject.yangutils.utils.io.impl.YangIoUtils.getAbsolutePackagePath;

/**
 * Represents utility class for YANG java model.
 */
public final class YangJavaModelUtils {

    /**
     * Creates YANG java model utility.
     */
    private YangJavaModelUtils() {
    }

    /**
     * Updates YANG java file package information.
     *
     * @param javaCodeGeneratorInfo YANG java file info node
     * @param codeGenDir code generation directory
     * @throws IOException IO operations fails
     */
    private static void updatePackageInfo(JavaCodeGeneratorInfo javaCodeGeneratorInfo, String codeGenDir)
            throws IOException {
        javaCodeGeneratorInfo.getJavaFileInfo()
                .setJavaName(getCaptialCase(getCamelCase(((YangNode) javaCodeGeneratorInfo).getName())));
        javaCodeGeneratorInfo.getJavaFileInfo().setPackage(getCurNodePackage((YangNode) javaCodeGeneratorInfo));
        javaCodeGeneratorInfo.getJavaFileInfo().setPackageFilePath(
                getPackageDirPathFromJavaJPackage(javaCodeGeneratorInfo.getJavaFileInfo().getPackage()));
        javaCodeGeneratorInfo.getJavaFileInfo().setBaseCodeGenPath(codeGenDir);
    }

    /**
     * Updates YANG java file package information for specified package.
     *
     * @param javaCodeGeneratorInfo YANG java file info node
     * @param codeGenDir code generation directory
     * @throws IOException IO operations fails
     */
    private static void updatePackageInfo(JavaCodeGeneratorInfo javaCodeGeneratorInfo, String codeGenDir, String pkg)
            throws IOException {
        javaCodeGeneratorInfo.getJavaFileInfo()
                .setJavaName(getCaptialCase(getCamelCase(((YangNode) javaCodeGeneratorInfo).getName())));
        javaCodeGeneratorInfo.getJavaFileInfo().setPackage(pkg);
        javaCodeGeneratorInfo.getJavaFileInfo().setPackageFilePath(
                getPackageDirPathFromJavaJPackage(javaCodeGeneratorInfo.getJavaFileInfo().getPackage()));
        javaCodeGeneratorInfo.getJavaFileInfo().setBaseCodeGenPath(codeGenDir);
    }

    /**
     * Updates temporary java code fragment files.
     *
     * @param javaCodeGeneratorInfo YANG java file info node
     * @throws IOException IO operations fails
     */
    private static void createTempFragmentFile(JavaCodeGeneratorInfo javaCodeGeneratorInfo) throws IOException {
        String absolutePath = getAbsolutePackagePath(javaCodeGeneratorInfo.getJavaFileInfo().getBaseCodeGenPath(),
                javaCodeGeneratorInfo.getJavaFileInfo().getPackageFilePath());

        javaCodeGeneratorInfo.setTempJavaCodeFragmentFiles(
                new TempJavaCodeFragmentFiles(javaCodeGeneratorInfo.getJavaFileInfo().getGeneratedFileTypes(),
                        absolutePath, javaCodeGeneratorInfo.getJavaFileInfo().getJavaName()));
    }

    /**
     * Updates leaf information in temporary java code fragment files.
     *
     * @param javaCodeGeneratorInfo YANG java file info node
     * @throws IOException IO operations fails
     */
    private static void updateLeafInfoInTempFragmentFiles(JavaCodeGeneratorInfo javaCodeGeneratorInfo)
            throws IOException {

        if (javaCodeGeneratorInfo instanceof YangLeavesHolder) {
            javaCodeGeneratorInfo.getTempJavaCodeFragmentFiles()
                    .addCurNodeLeavesInfoToTempFiles((YangNode) javaCodeGeneratorInfo);
        } else {
            // TODO: either write a util for ENUM and UNION or, call the
            // corresponding implementation in ENUM and UNION
        }
    }

    /**
     * Process generate code entry of YANG node.
     *
     * @param javaCodeGeneratorInfo YANG java file info node
     * @param codeGenDir code generation directory
     * @throws IOException IO operations fails
     */
    private static void generateTempFiles(JavaCodeGeneratorInfo javaCodeGeneratorInfo, String codeGenDir)
            throws IOException {
        if (!(javaCodeGeneratorInfo instanceof YangNode)) {
            // TODO:throw exception
        }
        createTempFragmentFile(javaCodeGeneratorInfo);
        updateLeafInfoInTempFragmentFiles(javaCodeGeneratorInfo);

    }

    /**
     * Process generate code entry of YANG node.
     *
     * @param javaCodeGeneratorInfo YANG java file info node
     * @param codeGenDir code generation directory
     * @param isMultiInstance flag to indicate whether it's a list
     * @throws IOException IO operations fails
     */
    public static void generateCodeOfNode(JavaCodeGeneratorInfo javaCodeGeneratorInfo, String codeGenDir,
            boolean isMultiInstance) throws IOException {
        if (!(javaCodeGeneratorInfo instanceof YangNode)) {
            // TODO:throw exception
        }
        updatePackageInfo(javaCodeGeneratorInfo, codeGenDir);
        generateTempFiles(javaCodeGeneratorInfo, codeGenDir);

        javaCodeGeneratorInfo.getTempJavaCodeFragmentFiles()
                .addCurNodeInfoInParentTempFile((YangNode) javaCodeGeneratorInfo, isMultiInstance);
    }

    /**
     * Process generate code entry of root node.
     *
     * @param javaCodeGeneratorInfo YANG java file info node
     * @param codeGenDir code generation directory
     * @param rootPkg package of the root node
     * @throws IOException IO operations fails
     */
    public static void generateCodeOfRootNode(JavaCodeGeneratorInfo javaCodeGeneratorInfo, String codeGenDir,
            String rootPkg) throws IOException {
        if (!(javaCodeGeneratorInfo instanceof YangNode)) {
            // TODO:throw exception
        }
        updatePackageInfo(javaCodeGeneratorInfo, codeGenDir, rootPkg);
        generateTempFiles(javaCodeGeneratorInfo, codeGenDir);
    }
}

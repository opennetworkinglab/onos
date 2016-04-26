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
import org.onosproject.yangutils.datamodel.HasType;
import org.onosproject.yangutils.datamodel.YangAugment;
import org.onosproject.yangutils.datamodel.YangCase;
import org.onosproject.yangutils.datamodel.YangChoice;
import org.onosproject.yangutils.datamodel.YangContainer;
import org.onosproject.yangutils.datamodel.YangInput;
import org.onosproject.yangutils.datamodel.YangLeavesHolder;
import org.onosproject.yangutils.datamodel.YangList;
import org.onosproject.yangutils.datamodel.YangNode;
import org.onosproject.yangutils.datamodel.YangNotification;
import org.onosproject.yangutils.datamodel.YangOutput;
import org.onosproject.yangutils.translator.tojava.HasJavaFileInfo;
import org.onosproject.yangutils.translator.tojava.TempJavaCodeFragmentFiles;
import org.onosproject.yangutils.translator.tojava.javamodel.JavaCodeGeneratorInfo;
import org.onosproject.yangutils.translator.tojava.javamodel.YangJavaEnumeration;

import static org.onosproject.yangutils.translator.tojava.utils.JavaIdentifierSyntax.getCamelCase;
import static org.onosproject.yangutils.translator.tojava.utils.JavaIdentifierSyntax.getCaptialCase;
import static org.onosproject.yangutils.translator.tojava.utils.JavaIdentifierSyntax.getCurNodePackage;
import static org.onosproject.yangutils.translator.tojava.utils.JavaIdentifierSyntax.getPackageDirPathFromJavaJPackage;
import static org.onosproject.yangutils.utils.UtilConstants.AUGMENTED_INFO;
import static org.onosproject.yangutils.utils.UtilConstants.HAS_AUGMENTATION;
import static org.onosproject.yangutils.utils.UtilConstants.PERIOD;
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
     * @param hasJavaFileInfo YANG java file info node
     * @param yangPlugin      YANG plugin config
     * @throws IOException IO operations fails
     */
    public static void updatePackageInfo(HasJavaFileInfo hasJavaFileInfo, YangPluginConfig yangPlugin)
            throws IOException {
        hasJavaFileInfo.getJavaFileInfo()
                .setJavaName(getCaptialCase(
                        getCamelCase(((YangNode) hasJavaFileInfo).getName(), yangPlugin.getConflictResolver())));
        hasJavaFileInfo.getJavaFileInfo().setPackage(getCurNodePackage((YangNode) hasJavaFileInfo));
        hasJavaFileInfo.getJavaFileInfo().setPackageFilePath(
                getPackageDirPathFromJavaJPackage(hasJavaFileInfo.getJavaFileInfo().getPackage()));
        hasJavaFileInfo.getJavaFileInfo().setBaseCodeGenPath(yangPlugin.getCodeGenDir());
    }

    /**
     * Updates YANG java file package information for specified package.
     *
     * @param javaCodeGeneratorInfo YANG java file info node
     * @param yangPlugin            YANG plugin config
     * @throws IOException IO operations fails
     */
    private static void updatePackageInfo(JavaCodeGeneratorInfo javaCodeGeneratorInfo, YangPluginConfig yangPlugin,
                                          String pkg)
            throws IOException {
        javaCodeGeneratorInfo.getJavaFileInfo()
                .setJavaName(getCaptialCase(
                        getCamelCase(((YangNode) javaCodeGeneratorInfo).getName(), yangPlugin.getConflictResolver())));
        javaCodeGeneratorInfo.getJavaFileInfo().setPackage(pkg);
        javaCodeGeneratorInfo.getJavaFileInfo().setPackageFilePath(
                getPackageDirPathFromJavaJPackage(javaCodeGeneratorInfo.getJavaFileInfo().getPackage()));
        javaCodeGeneratorInfo.getJavaFileInfo().setBaseCodeGenPath(yangPlugin.getCodeGenDir());
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
        } else if (javaCodeGeneratorInfo instanceof HasType) {
            javaCodeGeneratorInfo.getTempJavaCodeFragmentFiles()
                    .addTypeInfoToTempFiles((HasType) javaCodeGeneratorInfo);
        } else if (javaCodeGeneratorInfo instanceof YangJavaEnumeration) {
            javaCodeGeneratorInfo.getTempJavaCodeFragmentFiles()
                    .addEnumAttributeToTempFiles((YangNode) javaCodeGeneratorInfo);
        } else {
            //TODO throw exception
        }
    }

    /**
     * Process generate code entry of YANG node.
     *
     * @param javaCodeGeneratorInfo YANG java file info node
     * @param codeGenDir            code generation directory
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
     * @param yangPlugin            YANG plugin config
     * @param isMultiInstance       flag to indicate whether it's a list
     * @throws IOException IO operations fails
     */
    public static void generateCodeOfNode(JavaCodeGeneratorInfo javaCodeGeneratorInfo, YangPluginConfig yangPlugin,
                                          boolean isMultiInstance) throws IOException {
        if (!(javaCodeGeneratorInfo instanceof YangNode)) {
            // TODO:throw exception
        }
        updatePackageInfo((HasJavaFileInfo) javaCodeGeneratorInfo, yangPlugin);
        generateTempFiles(javaCodeGeneratorInfo, yangPlugin.getCodeGenDir());

        if (!(javaCodeGeneratorInfo instanceof YangCase)) {
            javaCodeGeneratorInfo.getTempJavaCodeFragmentFiles()
                    .addCurNodeInfoInParentTempFile((YangNode) javaCodeGeneratorInfo, isMultiInstance);
        }

        /**
         * For augmentation of nodes.
         */
        if (javaCodeGeneratorInfo instanceof YangContainer
                || javaCodeGeneratorInfo instanceof YangCase
                || javaCodeGeneratorInfo instanceof YangChoice
                || javaCodeGeneratorInfo instanceof YangInput
                || javaCodeGeneratorInfo instanceof YangList
                || javaCodeGeneratorInfo instanceof YangNotification
                || javaCodeGeneratorInfo instanceof YangOutput) {
            javaCodeGeneratorInfo.getTempJavaCodeFragmentFiles().addToExtendsList(HAS_AUGMENTATION);
        } else if (javaCodeGeneratorInfo instanceof YangAugment) {
            javaCodeGeneratorInfo.getTempJavaCodeFragmentFiles().addToExtendsList(AUGMENTED_INFO);
        }

        if (javaCodeGeneratorInfo instanceof YangCase) {
            YangNode parent = ((YangCase) javaCodeGeneratorInfo).getParent();
            String curNodeName = ((YangCase) javaCodeGeneratorInfo).getName();
            if (!parent.getName().equals(curNodeName)) {
                javaCodeGeneratorInfo.getTempJavaCodeFragmentFiles().addToExtendsList(getCaptialCase(getCamelCase(
                    parent.getName(), null)));
                javaCodeGeneratorInfo.getTempJavaCodeFragmentFiles().addParentInfoInCurNodeTempFile((YangNode)
                    javaCodeGeneratorInfo);
            } else {
                String parentPackage = ((HasJavaFileInfo) parent).getJavaFileInfo().getPackage();
                String caseExtendInfo = parentPackage + PERIOD + parent.getName();
                javaCodeGeneratorInfo.getTempJavaCodeFragmentFiles().addToExtendsList(caseExtendInfo);
            }
        }
    }

    /**
     * Process generate code entry of YANG type.
     *
     * @param javaCodeGeneratorInfo YANG java file info node
     * @param yangPlugin            YANG plugin config
     * @throws IOException IO operations fails
     */
    public static void generateCodeOfNode(JavaCodeGeneratorInfo javaCodeGeneratorInfo, YangPluginConfig yangPlugin)
            throws IOException {
        if (!(javaCodeGeneratorInfo instanceof YangNode)) {
            // TODO:throw exception
        }
        updatePackageInfo((HasJavaFileInfo) javaCodeGeneratorInfo, yangPlugin);
        generateTempFiles(javaCodeGeneratorInfo, yangPlugin.getCodeGenDir());
    }

    /**
     * Process generate code entry of root node.
     *
     * @param javaCodeGeneratorInfo YANG java file info node
     * @param yangPlugin            YANG plugin config
     * @param rootPkg               package of the root node
     * @throws IOException IO operations fails
     */
    public static void generateCodeOfRootNode(JavaCodeGeneratorInfo javaCodeGeneratorInfo, YangPluginConfig yangPlugin,
                                              String rootPkg) throws IOException {
        if (!(javaCodeGeneratorInfo instanceof YangNode)) {
            // TODO:throw exception
        }
        updatePackageInfo(javaCodeGeneratorInfo, yangPlugin, rootPkg);
        generateTempFiles(javaCodeGeneratorInfo, yangPlugin.getCodeGenDir());
    }
}

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

import org.onosproject.yangutils.datamodel.YangTypeContainer;
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
import org.onosproject.yangutils.translator.exception.TranslatorException;
import org.onosproject.yangutils.translator.tojava.JavaFileInfoContainer;
import org.onosproject.yangutils.translator.tojava.TempJavaCodeFragmentFiles;
import org.onosproject.yangutils.translator.tojava.javamodel.JavaCodeGeneratorInfo;
import org.onosproject.yangutils.translator.tojava.javamodel.YangJavaEnumeration;

import static org.onosproject.yangutils.translator.tojava.TempJavaFragmentFiles.addCurNodeInfoInParentTempFile;
import static org.onosproject.yangutils.translator.tojava.utils.JavaIdentifierSyntax.getCamelCase;
import static org.onosproject.yangutils.translator.tojava.utils.JavaIdentifierSyntax.getCaptialCase;
import static org.onosproject.yangutils.translator.tojava.utils.JavaIdentifierSyntax.getCurNodePackage;
import static org.onosproject.yangutils.translator.tojava.utils.JavaIdentifierSyntax.getPackageDirPathFromJavaJPackage;
import static org.onosproject.yangutils.utils.UtilConstants.AUGMENTED_INFO;
import static org.onosproject.yangutils.utils.UtilConstants.HAS_AUGMENTATION;
import static org.onosproject.yangutils.utils.UtilConstants.PERIOD;

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
     * @param yangPlugin YANG plugin config
     * @throws IOException IO operations fails
     */
    public static void updatePackageInfo(JavaCodeGeneratorInfo javaCodeGeneratorInfo, YangPluginConfig yangPlugin)
            throws IOException {
        javaCodeGeneratorInfo.getJavaFileInfo()
                .setJavaName(getCaptialCase(
                        getCamelCase(((YangNode) javaCodeGeneratorInfo).getName(), yangPlugin.getConflictResolver())));
        javaCodeGeneratorInfo.getJavaFileInfo().setPackage(getCurNodePackage((YangNode) javaCodeGeneratorInfo));
        javaCodeGeneratorInfo.getJavaFileInfo().setPackageFilePath(
                getPackageDirPathFromJavaJPackage(javaCodeGeneratorInfo.getJavaFileInfo().getPackage()));
        javaCodeGeneratorInfo.getJavaFileInfo().setBaseCodeGenPath(yangPlugin.getCodeGenDir());
    }

    /**
     * Updates YANG java file package information for specified package.
     *
     * @param javaCodeGeneratorInfo YANG java file info node
     * @param yangPlugin YANG plugin config
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
    private static void createTempFragmentFile(JavaCodeGeneratorInfo javaCodeGeneratorInfo)
            throws IOException {
        javaCodeGeneratorInfo.setTempJavaCodeFragmentFiles(
                new TempJavaCodeFragmentFiles(javaCodeGeneratorInfo.getJavaFileInfo()));
    }

    /**
     * Updates leaf information in temporary java code fragment files.
     *
     * @param javaCodeGeneratorInfo YANG java file info node
     * @throws IOException IO operations fails
     */
    private static void updateTempFragmentFiles(JavaCodeGeneratorInfo javaCodeGeneratorInfo)
            throws IOException {
        if (javaCodeGeneratorInfo instanceof YangLeavesHolder) {
            javaCodeGeneratorInfo.getTempJavaCodeFragmentFiles()
                    .addCurNodeLeavesInfoToTempFiles((YangNode) javaCodeGeneratorInfo);
        } else if (javaCodeGeneratorInfo instanceof YangTypeContainer) {
            javaCodeGeneratorInfo.getTempJavaCodeFragmentFiles()
                    .addTypeInfoToTempFiles((YangTypeContainer) javaCodeGeneratorInfo);
        } else if (javaCodeGeneratorInfo instanceof YangJavaEnumeration) {
            javaCodeGeneratorInfo.getTempJavaCodeFragmentFiles()
                    .addEnumAttributeToTempFiles((YangNode) javaCodeGeneratorInfo);
        } else if (javaCodeGeneratorInfo instanceof YangChoice) {
            /*Do nothing, only the interface needs to be generated*/
        } else {
            throw new TranslatorException("Unsupported Node Translation");
        }
    }

    /**
     * Process generate code entry of YANG node.
     *
     * @param javaCodeGeneratorInfo YANG java file info node
     * @throws IOException IO operations fails
     */
    private static void generateTempFiles(JavaCodeGeneratorInfo javaCodeGeneratorInfo, String codeGenDir)
            throws IOException {
        if (!(javaCodeGeneratorInfo instanceof YangNode)) {
            throw new TranslatorException("translation is not supported for the node");
        }
        createTempFragmentFile(javaCodeGeneratorInfo);
        updateTempFragmentFiles(javaCodeGeneratorInfo);

    }

    /**
     * Process generate code entry of YANG node.
     *
     * @param javaCodeGeneratorInfo YANG java file info node
     * @param yangPlugin YANG plugin config
     * @param isMultiInstance flag to indicate whether it's a list
     * @throws IOException IO operations fails
     */
    public static void generateCodeAndUpdateInParent(JavaCodeGeneratorInfo javaCodeGeneratorInfo,
            YangPluginConfig yangPlugin, boolean isMultiInstance)
            throws IOException {
        if (!(javaCodeGeneratorInfo instanceof YangNode)) {
            throw new TranslatorException("Invalid node for translation");
        }

        /**
         * Generate the Java files corresponding to the current node.
         */
        generateCodeOfAugmentableNode(javaCodeGeneratorInfo, yangPlugin);

        /**
         * Update the current nodes info in its parent nodes generated files.
         */
        addCurNodeInfoInParentTempFile((YangNode) javaCodeGeneratorInfo, isMultiInstance);
    }

    /**
     * Process generate code entry of YANG type.
     *
     * @param javaCodeGeneratorInfo YANG java file info node
     * @param yangPlugin YANG plugin config
     * @throws IOException IO operations fails
     */
    public static void generateCodeOfAugmentableNode(JavaCodeGeneratorInfo javaCodeGeneratorInfo,
            YangPluginConfig yangPlugin)
            throws IOException {
        if (!(javaCodeGeneratorInfo instanceof YangNode)) {
            throw new TranslatorException("invalid node for translation");
        }

        generateCodeOfNode(javaCodeGeneratorInfo, yangPlugin);

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
                javaCodeGeneratorInfo.getTempJavaCodeFragmentFiles().getBeanTempFiles()
                        .addParentInfoInCurNodeTempFile((YangNode) javaCodeGeneratorInfo);
            } else {
                String parentPackage = ((JavaFileInfoContainer) parent).getJavaFileInfo().getPackage();
                String caseExtendInfo = parentPackage + PERIOD + parent.getName();
                javaCodeGeneratorInfo.getTempJavaCodeFragmentFiles().addToExtendsList(caseExtendInfo);
            }
        }
    }

    /**
     * Process generate code entry of YANG type.
     *
     * @param javaCodeGeneratorInfo YANG java file info node
     * @param yangPlugin YANG plugin config
     * @throws IOException IO operations fails
     */
    public static void generateCodeOfNode(JavaCodeGeneratorInfo javaCodeGeneratorInfo, YangPluginConfig yangPlugin)
            throws IOException {
        if (!(javaCodeGeneratorInfo instanceof YangNode)) {
            // TODO:throw exception
        }
        updatePackageInfo(javaCodeGeneratorInfo, yangPlugin);
        generateTempFiles(javaCodeGeneratorInfo, yangPlugin.getCodeGenDir());
    }

    /**
     * Process generate code entry of root node.
     *
     * @param javaCodeGeneratorInfo YANG java file info node
     * @param yangPlugin YANG plugin config
     * @param rootPkg package of the root node
     * @throws IOException IO operations fails
     */
    public static void generateCodeOfRootNode(JavaCodeGeneratorInfo javaCodeGeneratorInfo, YangPluginConfig yangPlugin,
            String rootPkg)
            throws IOException {
        if (!(javaCodeGeneratorInfo instanceof YangNode)) {
            // TODO:throw exception
        }
        updatePackageInfo(javaCodeGeneratorInfo, yangPlugin, rootPkg);
        generateTempFiles(javaCodeGeneratorInfo, yangPlugin.getCodeGenDir());
    }
}

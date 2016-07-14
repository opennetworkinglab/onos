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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.onosproject.yangutils.datamodel.RpcNotificationContainer;
import org.onosproject.yangutils.datamodel.YangAugment;
import org.onosproject.yangutils.datamodel.YangCase;
import org.onosproject.yangutils.datamodel.YangChoice;
import org.onosproject.yangutils.datamodel.YangLeavesHolder;
import org.onosproject.yangutils.datamodel.YangModule;
import org.onosproject.yangutils.datamodel.YangNode;
import org.onosproject.yangutils.datamodel.YangNodeIdentifier;
import org.onosproject.yangutils.datamodel.YangSubModule;
import org.onosproject.yangutils.datamodel.YangTranslatorOperatorNode;
import org.onosproject.yangutils.datamodel.YangTypeHolder;
import org.onosproject.yangutils.datamodel.utils.DataModelUtils;
import org.onosproject.yangutils.translator.exception.TranslatorException;
import org.onosproject.yangutils.translator.tojava.javamodel.YangJavaAugment;
import org.onosproject.yangutils.translator.tojava.javamodel.YangJavaEnumeration;
import org.onosproject.yangutils.translator.tojava.javamodel.YangJavaModule;
import org.onosproject.yangutils.translator.tojava.javamodel.YangJavaSubModule;
import org.onosproject.yangutils.utils.io.impl.YangPluginConfig;

import static org.onosproject.yangutils.datamodel.utils.DataModelUtils.isRpcChildNodePresent;
import static org.onosproject.yangutils.translator.tojava.GeneratedJavaFileType.GENERATE_SERVICE_AND_MANAGER;
import static org.onosproject.yangutils.translator.tojava.TempJavaFragmentFiles.addCurNodeInfoInParentTempFile;
import static org.onosproject.yangutils.translator.tojava.utils.JavaIdentifierSyntax.getRootPackage;
import static org.onosproject.yangutils.utils.UtilConstants.AUGMENTED;
import static org.onosproject.yangutils.utils.UtilConstants.MANAGER;
import static org.onosproject.yangutils.utils.UtilConstants.PERIOD;
import static org.onosproject.yangutils.utils.UtilConstants.YANG_AUGMENTED_INFO;
import static org.onosproject.yangutils.utils.UtilConstants.YANG_AUGMENTED_INFO_CLASS_IMPORT_PKG;
import static org.onosproject.yangutils.utils.io.impl.YangIoUtils.getCamelCase;
import static org.onosproject.yangutils.utils.io.impl.YangIoUtils.getCapitalCase;
import static org.onosproject.yangutils.utils.io.impl.YangIoUtils.getPackageDirPathFromJavaJPackage;

/**
 * Represents utility class for YANG java model.
 */
public final class YangJavaModelUtils {

    /**
     * Creates an instance of YANG java model utility.
     */
    private YangJavaModelUtils() {
    }

    /**
     * Updates YANG java file package information.
     *
     * @param javaCodeGeneratorInfo YANG java file info node
     * @param yangPluginConfig      YANG plugin config
     * @throws IOException IO operations fails
     */
    public static void updatePackageInfo(JavaCodeGeneratorInfo javaCodeGeneratorInfo,
                                         YangPluginConfig yangPluginConfig)
            throws IOException {
        if (javaCodeGeneratorInfo instanceof YangJavaAugment) {
            javaCodeGeneratorInfo.getJavaFileInfo()
                    .setJavaName(getAugmentClassName((YangJavaAugment) javaCodeGeneratorInfo,
                            yangPluginConfig));
        } else {
            javaCodeGeneratorInfo.getJavaFileInfo()
                    .setJavaName(getCamelCase(((YangNode) javaCodeGeneratorInfo).getName(),
                            yangPluginConfig.getConflictResolver()));
        }
        javaCodeGeneratorInfo.getJavaFileInfo().setPackage(getCurNodePackage((YangNode) javaCodeGeneratorInfo));
        javaCodeGeneratorInfo.getJavaFileInfo().setPackageFilePath(
                getPackageDirPathFromJavaJPackage(javaCodeGeneratorInfo.getJavaFileInfo().getPackage()));
        javaCodeGeneratorInfo.getJavaFileInfo().setBaseCodeGenPath(yangPluginConfig.getCodeGenDir());
        javaCodeGeneratorInfo.getJavaFileInfo().setPluginConfig(yangPluginConfig);
    }

    /**
     * Updates YANG java file package information for specified package.
     *
     * @param javaCodeGeneratorInfo YANG java file info node
     * @param yangPlugin            YANG plugin config
     * @throws IOException IO operations fails
     */
    private static void updatePackageInfo(JavaCodeGeneratorInfo javaCodeGeneratorInfo, YangPluginConfig yangPlugin,
                                          String pkg) throws IOException {
        javaCodeGeneratorInfo.getJavaFileInfo()
                .setJavaName(getCamelCase(((YangNode) javaCodeGeneratorInfo).getName(),
                        yangPlugin.getConflictResolver()));
        javaCodeGeneratorInfo.getJavaFileInfo().setPackage(pkg);
        javaCodeGeneratorInfo.getJavaFileInfo().setPackageFilePath(
                getPackageDirPathFromJavaJPackage(javaCodeGeneratorInfo.getJavaFileInfo().getPackage()));
        javaCodeGeneratorInfo.getJavaFileInfo().setBaseCodeGenPath(yangPlugin.getManagerCodeGenDir());
        javaCodeGeneratorInfo.getJavaFileInfo().setPluginConfig(yangPlugin);
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
    private static void updateTempFragmentFiles(JavaCodeGeneratorInfo javaCodeGeneratorInfo,
                                                YangPluginConfig yangPluginConfig)
            throws IOException {
        if (javaCodeGeneratorInfo instanceof RpcNotificationContainer) {
            /*
             * Module / sub module node code generation.
             */
            javaCodeGeneratorInfo.getTempJavaCodeFragmentFiles()
                    .getServiceTempFiles().addCurNodeLeavesInfoToTempFiles(
                    (YangNode) javaCodeGeneratorInfo, yangPluginConfig);
            if (javaCodeGeneratorInfo instanceof YangJavaModule) {
                if (!((YangJavaModule) javaCodeGeneratorInfo).getNotificationNodes().isEmpty()) {
                    updateNotificaitonNodeInfo(javaCodeGeneratorInfo, yangPluginConfig);
                }
            } else if (javaCodeGeneratorInfo instanceof YangJavaSubModule) {
                if (!((YangJavaSubModule) javaCodeGeneratorInfo).getNotificationNodes().isEmpty()) {
                    updateNotificaitonNodeInfo(javaCodeGeneratorInfo, yangPluginConfig);
                }
            }

        }
        if (javaCodeGeneratorInfo instanceof YangLeavesHolder) {
            /*
             * Container
             * Case
             * Grouping
             * Input
             * List
             * Notification
             * Output
             */
            javaCodeGeneratorInfo.getTempJavaCodeFragmentFiles()
                    .getBeanTempFiles().addCurNodeLeavesInfoToTempFiles(
                    (YangNode) javaCodeGeneratorInfo, yangPluginConfig);
        } else if (javaCodeGeneratorInfo instanceof YangTypeHolder) {
            /*
             * Typedef
             * Union
             */
            javaCodeGeneratorInfo.getTempJavaCodeFragmentFiles()
                    .addTypeInfoToTempFiles((YangTypeHolder) javaCodeGeneratorInfo, yangPluginConfig);
        } else if (javaCodeGeneratorInfo instanceof YangJavaEnumeration) {
            /*
             * Enumeration
             */
            javaCodeGeneratorInfo.getTempJavaCodeFragmentFiles().getEnumerationTempFiles()
                    .addEnumAttributeToTempFiles((YangNode) javaCodeGeneratorInfo, yangPluginConfig);

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
     * @param yangPluginConfig      plugin configurations
     * @throws IOException IO operations fails
     */
    private static void generateTempFiles(JavaCodeGeneratorInfo javaCodeGeneratorInfo,
                                          YangPluginConfig yangPluginConfig)
            throws IOException {
        if (!(javaCodeGeneratorInfo instanceof YangNode)) {
            throw new TranslatorException("translation is not supported for the node");
        }
        createTempFragmentFile(javaCodeGeneratorInfo);
        updateTempFragmentFiles(javaCodeGeneratorInfo, yangPluginConfig);

    }

    /**
     * Updates notification node info in service temporary file.
     *
     * @param javaCodeGeneratorInfo java code generator info
     * @param yangPluginConfig      plugin configurations
     * @throws IOException when fails to do IO operations
     */
    private static void updateNotificaitonNodeInfo(JavaCodeGeneratorInfo javaCodeGeneratorInfo,
                                                   YangPluginConfig yangPluginConfig)
            throws IOException {
        if (javaCodeGeneratorInfo instanceof YangJavaModule) {
            for (YangNode notificaiton : ((YangJavaModule) javaCodeGeneratorInfo).getNotificationNodes()) {
                javaCodeGeneratorInfo.getTempJavaCodeFragmentFiles()
                        .getEventFragmentFiles()
                        .addJavaSnippetOfEvent(notificaiton, yangPluginConfig);
            }
        }
        if (javaCodeGeneratorInfo instanceof YangJavaSubModule) {
            for (YangNode notificaiton : ((YangJavaSubModule) javaCodeGeneratorInfo)
                    .getNotificationNodes()) {
                javaCodeGeneratorInfo.getTempJavaCodeFragmentFiles()
                        .getEventFragmentFiles()
                        .addJavaSnippetOfEvent(notificaiton, yangPluginConfig);
            }
        }
    }

    /**
     * Generates code for the current ata model node and adds itself as an attribute in the parent.
     *
     * @param javaCodeGeneratorInfo YANG java file info node
     * @param yangPlugin            YANG plugin config
     * @param isMultiInstance       flag to indicate whether it's a list
     * @throws IOException IO operations fails
     */
    public static void generateCodeAndUpdateInParent(JavaCodeGeneratorInfo javaCodeGeneratorInfo,
                                                     YangPluginConfig yangPlugin, boolean isMultiInstance)
            throws IOException {
        if (!(javaCodeGeneratorInfo instanceof YangNode)) {
            throw new TranslatorException("Invalid node for translation");
        }

        /*
         * Generate the Java files corresponding to the current node.
         */
        generateCodeOfAugmentableNode(javaCodeGeneratorInfo, yangPlugin);

        /*
         * Update the current nodes info in its parent nodes generated files.
         */
        addCurNodeInfoInParentTempFile((YangNode) javaCodeGeneratorInfo, isMultiInstance, yangPlugin);
    }

    /**
     * Generates code for the current data model node and adds support for it to be augmented.
     *
     * @param javaCodeGeneratorInfo YANG java file info node
     * @param yangPlugin            YANG plugin config
     * @throws IOException IO operations fails
     */
    public static void generateCodeOfAugmentableNode(JavaCodeGeneratorInfo javaCodeGeneratorInfo,
                                                     YangPluginConfig yangPlugin)
            throws IOException {
        if (!(javaCodeGeneratorInfo instanceof YangNode)) {
            throw new TranslatorException("invalid node for translation");
        }

        generateCodeOfNode(javaCodeGeneratorInfo, yangPlugin);
        TempJavaCodeFragmentFiles tempJavaCodeFragmentFiles = javaCodeGeneratorInfo.getTempJavaCodeFragmentFiles();
        if (javaCodeGeneratorInfo instanceof YangJavaAugment) {

            JavaQualifiedTypeInfo yangAugmentedInfo = new JavaQualifiedTypeInfo();
            yangAugmentedInfo.setClassInfo(YANG_AUGMENTED_INFO);
            yangAugmentedInfo.setPkgInfo(YANG_AUGMENTED_INFO_CLASS_IMPORT_PKG);
            javaCodeGeneratorInfo.getTempJavaCodeFragmentFiles().getBeanTempFiles().getJavaExtendsListHolder()
                    .addToExtendsList(yangAugmentedInfo, (YangNode) javaCodeGeneratorInfo,
                            tempJavaCodeFragmentFiles.getBeanTempFiles());
        }
        if (javaCodeGeneratorInfo instanceof YangCase) {
            YangNode parent = ((YangCase) javaCodeGeneratorInfo).getParent();
            JavaQualifiedTypeInfo parentsInfo = new JavaQualifiedTypeInfo();
            String parentName = getCapitalCase(((JavaFileInfoContainer) parent).getJavaFileInfo().getJavaName());
            String parentPkg = ((JavaFileInfoContainer) parent).getJavaFileInfo().getPackage();
            parentsInfo.setClassInfo(parentName);
            parentsInfo.setPkgInfo(parentPkg);
            javaCodeGeneratorInfo.getTempJavaCodeFragmentFiles().getBeanTempFiles().getJavaExtendsListHolder()
                    .addToExtendsList(parentsInfo, (YangNode) javaCodeGeneratorInfo,
                            tempJavaCodeFragmentFiles.getBeanTempFiles());

            javaCodeGeneratorInfo.getTempJavaCodeFragmentFiles().getBeanTempFiles()
                    .addParentInfoInCurNodeTempFile((YangNode) javaCodeGeneratorInfo, yangPlugin);

        }
    }

    /**
     * Generates code for the current data model node.
     *
     * @param javaCodeGeneratorInfo YANG java file info node
     * @param yangPluginConfig      YANG plugin config
     * @throws IOException IO operations fails
     */
    public static void generateCodeOfNode(JavaCodeGeneratorInfo javaCodeGeneratorInfo,
                                          YangPluginConfig yangPluginConfig)
            throws IOException {
        if (!(javaCodeGeneratorInfo instanceof YangNode)) {
            throw new TranslatorException("invalid node for translation");
        }
        updatePackageInfo(javaCodeGeneratorInfo, yangPluginConfig);
        generateTempFiles(javaCodeGeneratorInfo, yangPluginConfig);
    }

    /**
     * Generates code for the root module/sub-module node.
     *
     * @param javaCodeGeneratorInfo YANG java file info node
     * @param yangPluginConfig      YANG plugin config
     * @param rootPkg               package of the root node
     * @throws IOException IO operations fails
     */
    public static void generateCodeOfRootNode(JavaCodeGeneratorInfo javaCodeGeneratorInfo,
                                              YangPluginConfig yangPluginConfig, String rootPkg)
            throws IOException {
        if (!(javaCodeGeneratorInfo instanceof YangNode)) {
            throw new TranslatorException("invalid node for translation");
        }
        updatePackageInfo(javaCodeGeneratorInfo, yangPluginConfig, rootPkg);

        if (isRpcChildNodePresent((YangNode) javaCodeGeneratorInfo)) {
            javaCodeGeneratorInfo.getJavaFileInfo().addGeneratedFileTypes(GENERATE_SERVICE_AND_MANAGER);
        }

        generateTempFiles(javaCodeGeneratorInfo, yangPluginConfig);
    }

    /**
     * Returns the node package string.
     *
     * @param curNode current java node whose package string needs to be set
     * @return returns the root package string
     */
    public static String getCurNodePackage(YangNode curNode) {

        String pkg;
        if (!(curNode instanceof JavaFileInfoContainer)
                || curNode.getParent() == null) {
            throw new TranslatorException("missing parent node to get current node's package");
        }

        YangNode parentNode = DataModelUtils.getParentNodeInGenCode(curNode);
        if (!(parentNode instanceof JavaFileInfoContainer)) {
            throw new TranslatorException("missing parent java node to get current node's package");
        }
        JavaFileInfo parentJavaFileHandle = ((JavaFileInfoContainer) parentNode).getJavaFileInfo();
        pkg = parentJavaFileHandle.getPackage() + PERIOD + parentJavaFileHandle.getJavaName();
        return pkg.toLowerCase();
    }

    /**
     * Returns true if root node contains any data node.
     *
     * @param node root YANG node
     * @return true if root node contains any data node
     */
    public static boolean isManagerCodeGenRequired(YangNode node) {

        List<YangNode> childNodes = new ArrayList<>();
        node = node.getChild();
        while (node != null) {
            childNodes.add(node);
            node = node.getNextSibling();
        }

        if (childNodes.size() == 0) {
            return false;
        } else if (childNodes.size() == 1) {
            return !(childNodes.get(0) instanceof YangTranslatorOperatorNode);
        }
        List<Boolean> booleanData = new ArrayList<>();
        for (YangNode child : childNodes) {
            if (child instanceof YangTranslatorOperatorNode) {
                booleanData.add(false);
            } else {
                booleanData.add(true);
            }
        }
        return booleanData.contains(true);
    }

    /**
     * Return false if files are already present.
     *
     * @param info java file info
     * @return false if files already present
     */
    public static boolean isGenerationOfCodeReq(JavaFileInfo info) {
        File codeGenDir = new File(info.getBaseCodeGenPath()
                + info.getPackageFilePath());
        File[] files = codeGenDir.listFiles();
        if (files.length >= 1) {
            for (File file : files) {
                if (file.getName().contentEquals(getCapitalCase(info.getJavaName() + MANAGER + ".java"))) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Returns augmented nodes package.
     *
     * @param node             augmented node
     * @param yangPluginConfig plugin config
     * @return java package
     */
    public static String getAugmentedNodesPackage(YangNode node, YangPluginConfig yangPluginConfig) {

        List<String> clsInfo = new ArrayList<>();
        while (node.getParent() != null) {
            if (!(node instanceof YangModule)
                    || !(node instanceof YangSubModule)) {
                if (node instanceof YangJavaAugment) {
                    clsInfo.add(getAugmentClassName((YangAugment) node, yangPluginConfig));
                } else {
                    clsInfo.add(getCamelCase(node.getName(), yangPluginConfig.getConflictResolver()));
                }
            }
            if (node instanceof YangJavaModule
                    || node instanceof YangJavaSubModule) {
                break;
            }
            node = node.getParent();
        }

        StringBuilder pkg = new StringBuilder();
        if (node instanceof YangJavaModule) {
            YangJavaModule module = (YangJavaModule) node;
            pkg.append(getRootPackage(module.getVersion(), module.getNameSpace().getUri(), module
                    .getRevision().getRevDate(), yangPluginConfig.getConflictResolver()));
        } else if (node instanceof YangJavaSubModule) {
            YangJavaSubModule submodule = (YangJavaSubModule) node;
            pkg.append(getRootPackage(submodule.getVersion(),
                    submodule.getNameSpaceFromModule(submodule.getBelongsTo()),
                    submodule.getRevision().getRevDate(), yangPluginConfig.getConflictResolver()));
        }
        for (int i = 1; i <= clsInfo.size(); i++) {
            pkg.append("." + clsInfo.get(clsInfo.size() - i));
        }

        return pkg.toString().toLowerCase();

    }

    /**
     * Returns augment class name.
     *
     * @param augment          YANG augment
     * @param yangPluginConfig plugin configurations
     * @return augment class name
     */
    public static String getAugmentClassName(YangAugment augment, YangPluginConfig yangPluginConfig) {
        YangNodeIdentifier nodeId = augment.getTargetNode().get(augment.getTargetNode().size() - 1).getNodeIdentifier();
        String name = getCapitalCase(getCamelCase(nodeId.getName(), yangPluginConfig.getConflictResolver()));
        if (nodeId.getPrefix() != null) {
            return AUGMENTED + getCapitalCase(nodeId.getPrefix()) + name;
        } else {
            return AUGMENTED + name;
        }
    }
}

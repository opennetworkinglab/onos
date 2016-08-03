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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.onosproject.yangutils.datamodel.RpcNotificationContainer;
import org.onosproject.yangutils.datamodel.YangAtomicPath;
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
import org.onosproject.yangutils.datamodel.javadatamodel.JavaFileInfo;
import org.onosproject.yangutils.datamodel.javadatamodel.YangPluginConfig;
import org.onosproject.yangutils.datamodel.utils.DataModelUtils;
import org.onosproject.yangutils.translator.exception.TranslatorException;
import org.onosproject.yangutils.translator.tojava.javamodel.YangJavaAugmentTranslator;
import org.onosproject.yangutils.translator.tojava.javamodel.YangJavaEnumerationTranslator;
import org.onosproject.yangutils.translator.tojava.javamodel.YangJavaModuleTranslator;
import org.onosproject.yangutils.translator.tojava.javamodel.YangJavaSubModuleTranslator;

import static org.onosproject.yangutils.datamodel.utils.DataModelUtils.isRpcChildNodePresent;
import static org.onosproject.yangutils.translator.tojava.GeneratedJavaFileType.GENERATE_SERVICE_AND_MANAGER;
import static org.onosproject.yangutils.translator.tojava.TempJavaFragmentFiles.addCurNodeInfoInParentTempFile;
import static org.onosproject.yangutils.translator.tojava.utils.JavaIdentifierSyntax.getRootPackage;
import static org.onosproject.yangutils.utils.UtilConstants.AUGMENTED;
import static org.onosproject.yangutils.utils.UtilConstants.PERIOD;
import static org.onosproject.yangutils.utils.io.impl.YangIoUtils.getCamelCase;
import static org.onosproject.yangutils.utils.io.impl.YangIoUtils.getCapitalCase;
import static org.onosproject.yangutils.utils.io.impl.YangIoUtils.getPackageDirPathFromJavaJPackage;
import static org.onosproject.yangutils.utils.io.impl.YangIoUtils.trimAtLast;

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
        if (javaCodeGeneratorInfo instanceof YangJavaAugmentTranslator) {
            updatePackageForAugmentInfo(javaCodeGeneratorInfo, yangPluginConfig);
        } else {
            javaCodeGeneratorInfo.getJavaFileInfo()
                    .setJavaName(getCamelCase(((YangNode) javaCodeGeneratorInfo).getName(),
                            yangPluginConfig.getConflictResolver()));
            javaCodeGeneratorInfo.getJavaFileInfo().setPackage(getCurNodePackage((YangNode) javaCodeGeneratorInfo));
        }
        javaCodeGeneratorInfo.getJavaFileInfo().setPackageFilePath(
                getPackageDirPathFromJavaJPackage(javaCodeGeneratorInfo.getJavaFileInfo().getPackage()));

        javaCodeGeneratorInfo.getJavaFileInfo().setBaseCodeGenPath(yangPluginConfig.getCodeGenDir());
        javaCodeGeneratorInfo.getJavaFileInfo().setPluginConfig(yangPluginConfig);

    }

    /**
     * Updates YANG java file package information.
     *
     * @param javaCodeGeneratorInfo YANG java file info node
     * @param yangPluginConfig      YANG plugin config
     * @throws IOException IO operations fails
     */
    private static void updatePackageForAugmentInfo(JavaCodeGeneratorInfo javaCodeGeneratorInfo,
                                                    YangPluginConfig yangPluginConfig)
            throws IOException {
        javaCodeGeneratorInfo.getJavaFileInfo()
                .setJavaName(getAugmentClassName((YangJavaAugmentTranslator) javaCodeGeneratorInfo,
                        yangPluginConfig));
        javaCodeGeneratorInfo.getJavaFileInfo().setPackage(
                getAugmentsNodePackage((YangNode) javaCodeGeneratorInfo, yangPluginConfig));
        javaCodeGeneratorInfo.getJavaFileInfo().setPackageFilePath(
                getPackageDirPathFromJavaJPackage(javaCodeGeneratorInfo.getJavaFileInfo().getPackage()));
        javaCodeGeneratorInfo.getJavaFileInfo().setBaseCodeGenPath(yangPluginConfig.getCodeGenDir());
        javaCodeGeneratorInfo.getJavaFileInfo().setPluginConfig(yangPluginConfig);
    }

    /**
     * Returns package for augment node.
     *
     * @param yangNode         augment node
     * @param yangPluginConfig plugin configurations
     * @return package for augment node
     */
    private static String getAugmentsNodePackage(YangNode yangNode, YangPluginConfig yangPluginConfig) {
        YangAugment augment = (YangAugment) yangNode;
        StringBuilder augmentPkg = new StringBuilder();
        augmentPkg.append(getCurNodePackage(augment));

        String pkg = PERIOD;
        for (YangAtomicPath atomicPath : augment.getTargetNode()) {
            pkg = pkg + getCamelCase(atomicPath.getNodeIdentifier().getName(), yangPluginConfig.getConflictResolver())
                    + PERIOD;
        }
        pkg = trimAtLast(pkg, PERIOD);
        augmentPkg.append(pkg.toLowerCase());
        return augmentPkg.toString();
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
        javaCodeGeneratorInfo.getJavaFileInfo().setBaseCodeGenPath(yangPlugin.getCodeGenDir());
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

        if (javaCodeGeneratorInfo instanceof YangModule
                || javaCodeGeneratorInfo instanceof YangSubModule) {
            javaCodeGeneratorInfo.getTempJavaCodeFragmentFiles().getBeanTempFiles().setRooNode(true);
        }

        if (javaCodeGeneratorInfo instanceof RpcNotificationContainer) {
            /*
             * Module / sub module node code generation.
             */
            javaCodeGeneratorInfo.getTempJavaCodeFragmentFiles()
                    .getServiceTempFiles().addCurNodeLeavesInfoToTempFiles(
                    (YangNode) javaCodeGeneratorInfo, yangPluginConfig);
            if (javaCodeGeneratorInfo instanceof YangJavaModuleTranslator) {
                if (!((YangJavaModuleTranslator) javaCodeGeneratorInfo).getNotificationNodes().isEmpty()) {
                    updateNotificationNodeInfo(javaCodeGeneratorInfo, yangPluginConfig);
                }
            } else if (javaCodeGeneratorInfo instanceof YangJavaSubModuleTranslator) {
                if (!((YangJavaSubModuleTranslator) javaCodeGeneratorInfo).getNotificationNodes().isEmpty()) {
                    updateNotificationNodeInfo(javaCodeGeneratorInfo, yangPluginConfig);
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
        } else if (javaCodeGeneratorInfo instanceof YangJavaEnumerationTranslator) {
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
    private static void updateNotificationNodeInfo(JavaCodeGeneratorInfo javaCodeGeneratorInfo,
                                                   YangPluginConfig yangPluginConfig)
            throws IOException {
        if (javaCodeGeneratorInfo instanceof YangJavaModuleTranslator) {
            for (YangNode notification : ((YangJavaModuleTranslator) javaCodeGeneratorInfo).getNotificationNodes()) {
                javaCodeGeneratorInfo.getTempJavaCodeFragmentFiles()
                        .getEventFragmentFiles()
                        .addJavaSnippetOfEvent(notification, yangPluginConfig);
            }
        }
        if (javaCodeGeneratorInfo instanceof YangJavaSubModuleTranslator) {
            for (YangNode notification : ((YangJavaSubModuleTranslator) javaCodeGeneratorInfo)
                    .getNotificationNodes()) {
                javaCodeGeneratorInfo.getTempJavaCodeFragmentFiles()
                        .getEventFragmentFiles()
                        .addJavaSnippetOfEvent(notification, yangPluginConfig);
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

        if (javaCodeGeneratorInfo instanceof YangCase) {
            YangNode parent = ((YangCase) javaCodeGeneratorInfo).getParent();
            if (parent instanceof YangAugment) {
                parent = ((YangAugment) parent).getAugmentedNode();
            }
            JavaQualifiedTypeInfoTranslator parentsInfo = new JavaQualifiedTypeInfoTranslator();
            JavaFileInfo parentInfo = ((JavaFileInfoContainer) parent).getJavaFileInfo();
            String parentName;
            String parentPkg;
            if (parentInfo.getPackage() != null) {
                parentName = getCapitalCase(((JavaFileInfoContainer) parent).getJavaFileInfo().getJavaName());
                parentPkg = ((JavaFileInfoContainer) parent).getJavaFileInfo().getPackage();
            } else {
                parentName = getCapitalCase(getCamelCase(parent.getName(), yangPlugin.getConflictResolver()));
                parentPkg = getNodesPackage(parent, yangPlugin);
            }
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
    public static boolean isRootNodesCodeGenRequired(YangNode node) {

        List<YangNode> childNodes = new ArrayList<>();
        YangNode tempNode = node.getChild();
        while (tempNode != null) {
            childNodes.add(tempNode);
            tempNode = tempNode.getNextSibling();
        }

        if (childNodes.size() == 0) {
            YangLeavesHolder leavesHolder = (YangLeavesHolder) node;
            return !leavesHolder.getListOfLeaf().isEmpty() || !leavesHolder.getListOfLeafList().isEmpty();
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
     * Returns nodes package.
     *
     * @param node             YANG node
     * @param yangPluginConfig plugin config
     * @return java package
     */
    public static String getNodesPackage(YangNode node, YangPluginConfig yangPluginConfig) {

        List<String> clsInfo = new ArrayList<>();
        while (node.getParent() != null) {
            if (node instanceof YangJavaAugmentTranslator) {
                clsInfo.add(getAugmentClassName((YangAugment) node, yangPluginConfig));
            } else {
                clsInfo.add(getCamelCase(node.getName(), yangPluginConfig.getConflictResolver()));
            }
            node = node.getParent();
        }

        StringBuilder pkg = new StringBuilder();
        if (node instanceof YangJavaModuleTranslator) {
            YangJavaModuleTranslator module = (YangJavaModuleTranslator) node;
            pkg.append(getRootPackage(module.getVersion(), module.getNameSpace().getUri(), module
                    .getRevision().getRevDate(), yangPluginConfig.getConflictResolver()));
        } else if (node instanceof YangJavaSubModuleTranslator) {
            YangJavaSubModuleTranslator subModule = (YangJavaSubModuleTranslator) node;
            pkg.append(getRootPackage(subModule.getVersion(),
                    subModule.getNameSpaceFromModule(subModule.getBelongsTo()),
                    subModule.getRevision().getRevDate(), yangPluginConfig.getConflictResolver()));
        }
        String concat = "";
        for (int i = 1; i <= clsInfo.size(); i++) {
            concat = concat + "." + clsInfo.get(clsInfo.size() - i);
        }
        pkg.append(concat);
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
        YangNodeIdentifier yangNodeIdentifier = augment.getTargetNode().get(augment.getTargetNode().size() - 1)
                .getNodeIdentifier();
        String name = getCapitalCase(getCamelCase(yangNodeIdentifier.getName(), yangPluginConfig
                .getConflictResolver()));
        if (yangNodeIdentifier.getPrefix() != null) {
            return AUGMENTED + getCapitalCase(yangNodeIdentifier.getPrefix()) + name;
        } else {
            return AUGMENTED + name;
        }
    }

}

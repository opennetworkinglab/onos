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

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.onosproject.yangutils.datamodel.YangNode;
import org.onosproject.yangutils.datamodel.YangNodeIdentifier;
import org.onosproject.yangutils.translator.exception.TranslatorException;
import org.onosproject.yangutils.translator.tojava.JavaFileInfoContainer;
import org.onosproject.yangutils.translator.tojava.JavaQualifiedTypeInfo;
import org.onosproject.yangutils.translator.tojava.TempJavaCodeFragmentFiles;
import org.onosproject.yangutils.translator.tojava.TempJavaCodeFragmentFilesContainer;
import org.onosproject.yangutils.translator.tojava.TempJavaFragmentFiles;
import org.onosproject.yangutils.translator.tojava.javamodel.YangJavaAugment;
import org.onosproject.yangutils.translator.tojava.javamodel.YangJavaModule;

import static java.util.Collections.sort;

import static org.onosproject.yangutils.translator.tojava.utils.JavaIdentifierSyntax.getCapitalCase;
import static org.onosproject.yangutils.translator.tojava.utils.JavaIdentifierSyntax.getSmallCase;
import static org.onosproject.yangutils.utils.UtilConstants.ADD_STRING;
import static org.onosproject.yangutils.utils.UtilConstants.AUGMENTATION;
import static org.onosproject.yangutils.utils.UtilConstants.AUGMENTATION_HOLDER;
import static org.onosproject.yangutils.utils.UtilConstants.AUGMENTED_INFO;
import static org.onosproject.yangutils.utils.UtilConstants.BUILDER;
import static org.onosproject.yangutils.utils.UtilConstants.CLOSE_PARENTHESIS;
import static org.onosproject.yangutils.utils.UtilConstants.EIGHT_SPACE_INDENTATION;
import static org.onosproject.yangutils.utils.UtilConstants.EQUAL;
import static org.onosproject.yangutils.utils.UtilConstants.IMPL;
import static org.onosproject.yangutils.utils.UtilConstants.IMPORT;
import static org.onosproject.yangutils.utils.UtilConstants.LISTENER_SERVICE;
import static org.onosproject.yangutils.utils.UtilConstants.NEW;
import static org.onosproject.yangutils.utils.UtilConstants.NEW_LINE;
import static org.onosproject.yangutils.utils.UtilConstants.OPEN_PARENTHESIS;
import static org.onosproject.yangutils.utils.UtilConstants.PERIOD;
import static org.onosproject.yangutils.utils.UtilConstants.SEMI_COLAN;
import static org.onosproject.yangutils.utils.UtilConstants.SPACE;
import static org.onosproject.yangutils.utils.UtilConstants.THIS;
import static org.onosproject.yangutils.utils.io.impl.FileSystemUtil.updateFileHandle;

/**
 * Represents utilities for temporary java code fragments.
 */
public final class TempJavaCodeFragmentFilesUtils {

    /**
     * Creates a private instance of temporary java code fragment utils.
     */
    private TempJavaCodeFragmentFilesUtils() {
    }

    /**
     * Adds import for AugmentationHolders class.
     *
     * @param curNode current YANG node
     * @param imports list of imports
     * @param operation add or delete import
     */
    public static void addAugmentationHoldersImport(YangNode curNode, List<String> imports, boolean operation) {
        String thisImport = getTempJavaFragement(curNode).getJavaImportData().getAugmentationHolderImport();
        performOperationOnImports(imports, thisImport, operation);
    }

    /**
     * Adds import for AugmentedInfo class.
     *
     * @param curNode current YANG node
     * @param imports list of imports
     * @param operation add or delete import
     */
    public static void addAugmentedInfoImport(YangNode curNode, List<String> imports, boolean operation) {
        String thisImport = getTempJavaFragement(curNode).getJavaImportData().getAugmentedInfoImport();
        performOperationOnImports(imports, thisImport, operation);
    }

    /**
     * Returns temp java fragment.
     *
     * @param curNode current YANG node
     * @return temp java fragments
     */
    public static TempJavaFragmentFiles getTempJavaFragement(YangNode curNode) {
        TempJavaCodeFragmentFiles container = ((TempJavaCodeFragmentFilesContainer) curNode)
                .getTempJavaCodeFragmentFiles();
        if (container.getBeanTempFiles() != null) {
            return container.getBeanTempFiles();
        }
        if (container.getEventTempFiles() != null) {
            return container.getEventTempFiles();
        }
        if (container.getEventListenerTempFiles() != null) {
            return container.getEventListenerTempFiles();
        }
        if (container.getServiceTempFiles() != null) {
            return container.getServiceTempFiles();
        }

        return null;
    }

    /**
     * Updated imports with augmented nodes import.
     *
     * @param curNode current YANG node
     * @param imports list of imports
     * @param operation to add or to delete
     */
    public static void addAugmentedNodesImport(YangNode curNode, List<String> imports, boolean operation) {

        String nodesImport = "";

        if (!(curNode instanceof YangJavaAugment)) {
            throw new TranslatorException("current node should be of type augment node.");
        }
        YangJavaAugment augment = (YangJavaAugment) curNode;
        List<YangNodeIdentifier> targetNodes = augment.getTargetNode();
        YangNode parent = curNode.getParent();
        if (parent instanceof YangJavaModule) {
            // Add impl class import.
            nodesImport = getAugmendtedNodesImports(parent, targetNodes, true) + SEMI_COLAN + NEW_LINE;
            performOperationOnImports(imports, nodesImport, operation);
            // Add builder class import.
            if (targetNodes.size() > 2) {
                nodesImport = getAugmendtedNodesImports(parent, targetNodes, false) + SEMI_COLAN + NEW_LINE;
                performOperationOnImports(imports, nodesImport, operation);
            }
        }
        // TODO: add functionality for submodule and uses.
    }

    /**
     * Returns imports for augmented node.
     *
     * @param parent parent YANG node
     * @param targetNodes list of target nodes
     * @param isImplClass if impl class's import required
     * @return imports for augmented node
     */
    private static String getAugmendtedNodesImports(YangNode parent, List<YangNodeIdentifier> targetNodes,
            boolean isImplClass) {
        String pkgInfo = ((JavaFileInfoContainer) parent).getJavaFileInfo().getPackage();

        for (int i = 0; i < targetNodes.size() - 1; i++) {
            pkgInfo = pkgInfo + PERIOD + targetNodes.get(i).getName();
        }
        String classInfo = targetNodes.get(targetNodes.size() - 1).getName();
        if (!isImplClass) {
            return IMPORT + pkgInfo.toLowerCase() + PERIOD + getCapitalCase(classInfo) + BUILDER;
        }
        return IMPORT + pkgInfo.toLowerCase() + PERIOD + getCapitalCase(classInfo) + BUILDER + PERIOD
                + getCapitalCase(classInfo) + IMPL;
    }

    /**
     * Provides string to be added in augment node's constructor.
     *
     * @param curNode current YANG node
     * @return constructors string
     */
    public static String getAugmentsAddToAugmentedClassString(YangNode curNode) {

        if (!(curNode instanceof YangJavaAugment)) {
            throw new TranslatorException("current node should be of type augment node.");
        }
        YangJavaAugment augment = (YangJavaAugment) curNode;
        List<YangNodeIdentifier> targetNodes = augment.getTargetNode();

        String name = targetNodes.get(targetNodes.size() - 1).getName();
        String captialCase = getCapitalCase(name);
        String smallCase = getSmallCase(captialCase);
        return EIGHT_SPACE_INDENTATION + captialCase + IMPL + SPACE + smallCase + IMPL + SPACE + EQUAL + SPACE + NEW
                + SPACE + captialCase + BUILDER + OPEN_PARENTHESIS + CLOSE_PARENTHESIS + PERIOD + NEW + SPACE
                + captialCase + IMPL + OPEN_PARENTHESIS + CLOSE_PARENTHESIS + SEMI_COLAN + NEW_LINE
                + EIGHT_SPACE_INDENTATION + smallCase + IMPL + PERIOD + ADD_STRING + AUGMENTATION + OPEN_PARENTHESIS
                + THIS + CLOSE_PARENTHESIS + SEMI_COLAN + NEW_LINE;

    }

    /**
     * Adds import for array list.
     *
     * @param curNode current YANG node
     * @param imports list of imports
     * @param operation add or delete import
     */
    public static void addArrayListImport(YangNode curNode, List<String> imports, boolean operation) {
        String arrayListImport = getTempJavaFragement(curNode).getJavaImportData().getImportForArrayList();
        String listImport = getTempJavaFragement(curNode).getJavaImportData().getImportForList();
        performOperationOnImports(imports, arrayListImport, operation);
        if (!imports.contains(listImport)) {
            /**
             * List can be there because of attribute also , so no need to remove it and operation will
             * always be add(true).
             */
            performOperationOnImports(imports, listImport, true);
        }
    }

    /**
     * Adds listener's imports.
     *
     * @param curNode currentYangNode.
     * @param imports import list
     * @param operation add or remove
     * @param classInfo class info to be added to import list
     */
    public static void addListnersImport(YangNode curNode, List<String> imports, boolean operation,
            String classInfo) {
        String thisImport = "";
        if (classInfo.equals(LISTENER_SERVICE)) {
            thisImport = getTempJavaFragement(curNode).getJavaImportData().getListenerServiceImport();
            performOperationOnImports(imports, thisImport, operation);
        } else {
            thisImport = getTempJavaFragement(curNode).getJavaImportData().getListenerRegistryImport();
            performOperationOnImports(imports, thisImport, operation);
        }
    }

    /**
     * Performs given operations on import list.
     *
     * @param imports list of imports
     * @param curImport current import
     * @param operation add or remove
     * @return import list
     */
    private static List<String> performOperationOnImports(List<String> imports, String curImport,
            boolean operation) {
        if (operation) {
            imports.add(curImport);
        } else {
            imports.remove(curImport);
        }
        sortImports(imports);
        return imports;
    }

    /**
     * Returns true if AugmentationHolder class needs to be extended.
     *
     * @param extendsList list of classes need to be extended
     * @return true or false
     */
    public static boolean isAugmentationHolderExtended(List<JavaQualifiedTypeInfo> extendsList) {
        for (JavaQualifiedTypeInfo info : extendsList) {
            return info.getClassInfo().equals(AUGMENTATION_HOLDER);
        }
        return false;
    }

    /**
     * Returns true if AugmentedInfo class needs to be extended.
     *
     * @param extendsList list of classes need to be extended
     * @return true or false
     */
    public static boolean isAugmentedInfoExtended(List<JavaQualifiedTypeInfo> extendsList) {
        for (JavaQualifiedTypeInfo info : extendsList) {
            return info.getClassInfo().equals(AUGMENTED_INFO);
        }
        return false;
    }

    /**
     * Closes the file handle for temporary file.
     *
     * @param file file to be closed
     * @param toBeDeleted flag to indicate if file needs to be deleted
     * @throws IOException when failed to close the file handle
     */
    public static void closeFile(File file, boolean toBeDeleted)
            throws IOException {

        if (file != null) {
            updateFileHandle(file, null, true);
            if (toBeDeleted) {
                file.delete();
            }
        }
    }

    /**
     * Detects collision between parent and child node which have same name.
     * When parent and child node both have the same name in that case child node should be used with
     * qualified name.
     *
     * @param curNode current YANG node
     * @param qualifiedTypeInfo current node's qualified info
     * @return true if collision is detected
     */
    public static boolean detectCollisionBwParentAndChildForImport(YangNode curNode,
            JavaQualifiedTypeInfo qualifiedTypeInfo) {

        YangNode parent = curNode.getParent();
        String parentsClassInfo = getCapitalCase(((JavaFileInfoContainer) parent).getJavaFileInfo().getJavaName());
        String childsClassInfo = qualifiedTypeInfo.getClassInfo();
        if (childsClassInfo.equals(parentsClassInfo)) {
            return true;
        }
        return false;
    }

    /**
     * Returns sorted import list.
     *
     * @param imports import list
     * @return sorted import list
     */
    public static List<String> sortImports(List<String> imports) {
        sort(imports);
        return imports;
    }

}

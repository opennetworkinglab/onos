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
import org.onosproject.yangutils.translator.tojava.JavaQualifiedTypeInfo;
import org.onosproject.yangutils.translator.tojava.TempJavaCodeFragmentFiles;
import org.onosproject.yangutils.translator.tojava.TempJavaCodeFragmentFilesContainer;
import org.onosproject.yangutils.translator.tojava.TempJavaFragmentFiles;

import static org.onosproject.yangutils.utils.UtilConstants.ACTIVATE_ANNOTATION_IMPORT;
import static org.onosproject.yangutils.utils.UtilConstants.AUGMENTATION_HOLDER;
import static org.onosproject.yangutils.utils.UtilConstants.AUGMENTED_INFO;
import static org.onosproject.yangutils.utils.UtilConstants.COMPONENT_ANNOTATION_IMPORT;
import static org.onosproject.yangutils.utils.UtilConstants.DEACTIVATE_ANNOTATION_IMPORT;
import static org.onosproject.yangutils.utils.UtilConstants.ENUM;
import static org.onosproject.yangutils.utils.UtilConstants.FOUR_SPACE_INDENTATION;
import static org.onosproject.yangutils.utils.UtilConstants.LISTENER_SERVICE;
import static org.onosproject.yangutils.utils.UtilConstants.LOGGER_FACTORY_IMPORT;
import static org.onosproject.yangutils.utils.UtilConstants.LOGGER_IMPORT;
import static org.onosproject.yangutils.utils.UtilConstants.NEW_LINE;
import static org.onosproject.yangutils.utils.UtilConstants.OPEN_CURLY_BRACKET;
import static org.onosproject.yangutils.utils.UtilConstants.PUBLIC;
import static org.onosproject.yangutils.utils.UtilConstants.SERVICE_ANNOTATION_IMPORT;
import static org.onosproject.yangutils.utils.UtilConstants.SPACE;
import static org.onosproject.yangutils.utils.UtilConstants.TYPE;
import static org.onosproject.yangutils.utils.io.impl.FileSystemUtil.updateFileHandle;
import static java.util.Collections.sort;

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
     * @param curNode   current YANG node
     * @param imports   list of imports
     * @param operation add or delete import
     */
    public static void addAugmentationHoldersImport(YangNode curNode, List<String> imports, boolean operation) {
        String thisImport = getTempJavaFragement(curNode).getJavaImportData().getAugmentationHolderImport();
        performOperationOnImports(imports, thisImport, operation);
    }

    /**
     * Adds import for AugmentedInfo class.
     *
     * @param curNode   current YANG node
     * @param imports   list of imports
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
        if (container.getServiceTempFiles() != null) {
            return container.getServiceTempFiles();
        }

        return null;
    }

    /**
     * Adds import for array list.
     *
     * @param curNode   current YANG node
     * @param imports   list of imports
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
     * @param curNode   currentYangNode.
     * @param imports   import list
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
     * Adds annotations imports.
     *
     * @param imports   list if imports
     * @param operation to add or to delete
     */
    public static void addAnnotationsImports(List<String> imports, boolean operation) {
        if (operation) {
            imports.add(ACTIVATE_ANNOTATION_IMPORT);
            imports.add(DEACTIVATE_ANNOTATION_IMPORT);
            imports.add(COMPONENT_ANNOTATION_IMPORT);
            imports.add(SERVICE_ANNOTATION_IMPORT);
            imports.add(LOGGER_FACTORY_IMPORT);
            imports.add(LOGGER_IMPORT);
        } else {
            imports.remove(ACTIVATE_ANNOTATION_IMPORT);
            imports.remove(DEACTIVATE_ANNOTATION_IMPORT);
            imports.remove(COMPONENT_ANNOTATION_IMPORT);
            imports.remove(SERVICE_ANNOTATION_IMPORT);
            imports.remove(LOGGER_FACTORY_IMPORT);
            imports.remove(LOGGER_IMPORT);
        }
        sortImports(imports);
    }

    /**
     * Performs given operations on import list.
     *
     * @param imports   list of imports
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
     * @param file        file to be closed
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
     * Returns sorted import list.
     *
     * @param imports import list
     * @return sorted import list
     */
    public static List<String> sortImports(List<String> imports) {
        sort(imports);
        return imports;
    }

    /**
     * Returns event enum start.
     *
     * @return event enum start
     */
    public static String getEventEnumTypeStart() {
        return FOUR_SPACE_INDENTATION + PUBLIC + SPACE + ENUM + SPACE + TYPE + SPACE + OPEN_CURLY_BRACKET
                + NEW_LINE;
    }
}

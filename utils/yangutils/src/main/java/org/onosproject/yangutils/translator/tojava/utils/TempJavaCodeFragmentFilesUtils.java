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
import org.onosproject.yangutils.translator.tojava.HasJavaImportData;

import static org.onosproject.yangutils.translator.tojava.JavaImportData.getAugmentedInfoImport;
import static org.onosproject.yangutils.translator.tojava.JavaImportData.getHasAugmentationImport;
import static org.onosproject.yangutils.translator.tojava.JavaImportData.getImportForArrayList;
import static org.onosproject.yangutils.translator.tojava.JavaImportData.getImportForList;
import static org.onosproject.yangutils.utils.UtilConstants.AUGMENTED_INFO;
import static org.onosproject.yangutils.utils.UtilConstants.HAS_AUGMENTATION;
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
     * Adds imports for ToString and HashCodeMethod.
     *
     * @param curNode current YANG node
     * @param imports import list
     * @return import list
     */
    public static List<String> addImportsToStringAndHasCodeMethods(YangNode curNode, List<String> imports) {
        if (curNode instanceof HasJavaImportData) {
            imports.add(((HasJavaImportData) curNode).getJavaImportData().getImportForHashAndEquals());
            imports.add(((HasJavaImportData) curNode).getJavaImportData().getImportForToString());
        }
        return imports;
    }

    /**
     * Adds import for HasAugmentation class.
     *
     * @param curNode current YANG node
     * @param imports list of imports
     * @param operation add or delete import
     * @return import for HasAugmentation class
     */
    public static List<String> addHasAugmentationImport(YangNode curNode, List<String> imports, boolean operation) {
        if (curNode instanceof HasJavaImportData) {
            String thisImport = getHasAugmentationImport();
            performOperationOnImports(imports, thisImport, operation);
        }
        return imports;
    }

    /**
     * Adds import for AugmentedInfo class.
     *
     * @param curNode current YANG node
     * @param imports list of imports
     * @param operation add or delete import
     * @return import for AugmentedInfo class
     */
    public static List<String> addAugmentedInfoImport(YangNode curNode, List<String> imports, boolean operation) {
        if (curNode instanceof HasJavaImportData) {
            String thisImport = getAugmentedInfoImport();
            performOperationOnImports(imports, thisImport, operation);
        }
        return imports;
    }

    /**
     * Adds import for array list.
     *
     * @param curNode current YANG node
     * @param imports list of imports
     * @param operation add or delete import
     * @return import for HasAugmentation class
     */
    public static List<String> addArrayListImport(YangNode curNode, List<String> imports, boolean operation) {
        if (curNode instanceof HasJavaImportData) {
            String arrayListImport = getImportForArrayList();
            String listImport = getImportForList();
            performOperationOnImports(imports, arrayListImport, operation);
            if (!imports.contains(listImport)) {
                /**
                 * List can be there because of attribute also , so no need to remove it and operation will
                 * always be add(true).
                 */
                performOperationOnImports(imports, listImport, true);
            }
        }

        return imports;
    }

    /**
     * Performs given operations on import list.
     *
     * @param imports list of imports
     * @param curImport current import
     * @param operation add or remove
     * @return import list
     */
    private static List<String> performOperationOnImports(List<String> imports, String curImport, boolean operation) {
        if (operation) {
            imports.add(curImport);
        } else {
            imports.remove(curImport);
        }
        sort(imports);
        return imports;
    }

    /**
     * Prepares java file generator for extends list.
     *
     * @param extendsList list of classes need to be extended
     */
    public static void prepareJavaFileGeneratorForExtendsList(List<String> extendsList) {

        if (extendsList != null && !extendsList.isEmpty()) {
            JavaFileGenerator.setExtendsList(extendsList);
            JavaFileGenerator.setIsExtendsList(true);
        } else {
            JavaFileGenerator.getExtendsList().clear();
            JavaFileGenerator.setIsExtendsList(false);
        }
    }

    /**
     * Returns true if HasAugmentation class needs to be extended.
     *
     * @param extendsList list of classes need to be extended
     * @return true or false
     */
    public static boolean isHasAugmentationExtended(List<String> extendsList) {
        return (extendsList != null && extendsList.contains(HAS_AUGMENTATION));
    }

    /**
     * Returns true if AugmentedInfo class needs to be extended.
     *
     * @param extendsList list of classes need to be extended
     * @return true or false
     */
    public static boolean isAugmentedInfoExtended(List<String> extendsList) {
        return (extendsList != null && extendsList.contains(AUGMENTED_INFO));
    }

    /**
     * Closes the file handle for temporary file.
     *
     * @param file file to be closed
     * @param toBeDeleted flag to indicate if file needs to be deleted
     * @throws IOException when failed to close the file handle
     */
    public static void closeFile(File file, boolean toBeDeleted) throws IOException {

        if (file != null) {
            updateFileHandle(file, null, true);
            if (toBeDeleted) {
                file.delete();
            }
        }
    }
}

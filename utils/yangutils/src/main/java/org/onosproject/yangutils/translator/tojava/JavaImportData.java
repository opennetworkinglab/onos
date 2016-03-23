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
package org.onosproject.yangutils.translator.tojava;

import java.util.ArrayList;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import org.onosproject.yangutils.datamodel.YangNode;

import static org.onosproject.yangutils.utils.UtilConstants.COLLECTION_IMPORTS;
import static org.onosproject.yangutils.utils.UtilConstants.EMPTY_STRING;
import static org.onosproject.yangutils.utils.UtilConstants.GOOGLE_MORE_OBJECT_IMPORT_CLASS;
import static org.onosproject.yangutils.utils.UtilConstants.GOOGLE_MORE_OBJECT_IMPORT_PKG;
import static org.onosproject.yangutils.utils.UtilConstants.IMPORT;
import static org.onosproject.yangutils.utils.UtilConstants.JAVA_LANG;
import static org.onosproject.yangutils.utils.UtilConstants.JAVA_UTIL_OBJECTS_IMPORT_CLASS;
import static org.onosproject.yangutils.utils.UtilConstants.JAVA_UTIL_OBJECTS_IMPORT_PKG;
import static org.onosproject.yangutils.utils.UtilConstants.LIST;
import static org.onosproject.yangutils.utils.UtilConstants.NEW_LINE;
import static org.onosproject.yangutils.utils.UtilConstants.PERIOD;
import static org.onosproject.yangutils.utils.UtilConstants.SEMI_COLAN;

/**
 * Generated Java file can contain imports.
 */
public class JavaImportData {

    /**
     * Flag to denote if any list in imported.
     */
    private boolean isListToImport;

    /**
     * Sorted set of import info, to be used to maintain the set of classes to
     * be imported in the generated class.
     */
    private SortedSet<JavaQualifiedTypeInfo> importSet;

    /**
     * Default constructor.
     */
    public JavaImportData() {
        setImportSet(new TreeSet<JavaQualifiedTypeInfo>());
    }

    /**
     * Get if the list needs to be imported.
     *
     * @return true if any of the attribute needs to be maintained as a list.
     */
    public boolean getIfListImported() {

        return isListToImport;
    }

    /**
     * Set the status of importing list.
     *
     * @param isList status to mention list is bing imported.
     */
    public void setIfListImported(boolean isList) {

        isListToImport = isList;
    }

    /**
     * Get the set containing the imported class/interface info.
     *
     * @return the set containing the imported class/interface info
     */
    public SortedSet<JavaQualifiedTypeInfo> getImportSet() {

        return importSet;
    }

    /**
     * Assign the set containing the imported class/interface info.
     *
     * @param importSet the set containing the imported class/interface info
     */
    private void setImportSet(SortedSet<JavaQualifiedTypeInfo> importSet) {

        this.importSet = importSet;
    }

    /**
     * Add an imported class/interface info if it is not already part of the
     * collection.
     *
     * If already part of the collection, check if the packages are same, if so
     * then return true, to denote it is already in the import collection, and
     * it can be accessed without qualified access. If the packages do not
     * match, then do not add to the import collection, and return false to
     * denote, it is not added to import collection and needs to be accessed in
     * a qualified manner.
     *
     * @param curNode current data model node
     * @param newImportInfo class/interface info being imported
     * @return status of new addition of class/interface to the import set
     */
    public boolean addImportInfo(YangNode curNode, JavaQualifiedTypeInfo newImportInfo) {

        if (!(curNode instanceof HasJavaImportData)) {
            throw new RuntimeException("missing import info in data model node");
        }
        for (JavaQualifiedTypeInfo curImportInfo : ((HasJavaImportData) curNode).getJavaImportData().getImportSet()) {
            if (curImportInfo.getClassInfo()
                    .contentEquals(newImportInfo.getClassInfo())) {
                return curImportInfo.getPkgInfo()
                        .contentEquals(newImportInfo.getPkgInfo());
            }
        }
        ((HasJavaImportData) curNode).getJavaImportData().getImportSet().add(newImportInfo);
        return true;
    }

    /**
     * Returns import for class.
     *
     * @param attr java attribute info
     * @return imports for class
     */
    public List<String> getImports(JavaAttributeInfo attr) {

        String importString;
        List<String> imports = new ArrayList<>();

        for (JavaQualifiedTypeInfo importInfo : getImportSet()) {
            importString = IMPORT;
            if (importInfo.getPkgInfo() != EMPTY_STRING && importInfo.getClassInfo() != null
                    && importInfo.getPkgInfo() != JAVA_LANG) {
                importString = importString + importInfo.getPkgInfo() + PERIOD + importInfo.getClassInfo() + SEMI_COLAN
                        + NEW_LINE;

                imports.add(importString);
            }
        }

        if (attr.isListAttr()) {
            imports.add(setImportForList());
        }

        java.util.Collections.sort(imports);
        return imports;
    }

    /**
     * Gets import for hash and equals method.
     *
     * @return import for hash and equals method
     */
    public String getImportForHashAndEquals() {

        return IMPORT + JAVA_UTIL_OBJECTS_IMPORT_PKG + PERIOD + JAVA_UTIL_OBJECTS_IMPORT_CLASS;
    }

    /**
     * Gets import for to string method.
     *
     * @return import for to string method
     */
    public String getImportForToString() {

        return IMPORT + GOOGLE_MORE_OBJECT_IMPORT_PKG + PERIOD + GOOGLE_MORE_OBJECT_IMPORT_CLASS;
    }

    /**
     * Sets import for to list.
     */
    private static String setImportForList() {

        return IMPORT + COLLECTION_IMPORTS + PERIOD + LIST + SEMI_COLAN + NEW_LINE;
    }
}

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

import java.util.ArrayList;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import static org.onosproject.yangutils.utils.UtilConstants.ABSTRACT_EVENT;
import static org.onosproject.yangutils.utils.UtilConstants.ARRAY_LIST;
import static org.onosproject.yangutils.utils.UtilConstants.AUGMENTATION_HOLDER_CLASS_IMPORT_CLASS;
import static org.onosproject.yangutils.utils.UtilConstants.AUGMENTED_INFO_CLASS_IMPORT_CLASS;
import static org.onosproject.yangutils.utils.UtilConstants.AUGMENTED_INFO_CLASS_IMPORT_PKG;
import static org.onosproject.yangutils.utils.UtilConstants.COLLECTION_IMPORTS;
import static org.onosproject.yangutils.utils.UtilConstants.EMPTY_STRING;
import static org.onosproject.yangutils.utils.UtilConstants.EVENT_LISTENER;
import static org.onosproject.yangutils.utils.UtilConstants.GOOGLE_MORE_OBJECT_IMPORT_CLASS;
import static org.onosproject.yangutils.utils.UtilConstants.GOOGLE_MORE_OBJECT_IMPORT_PKG;
import static org.onosproject.yangutils.utils.UtilConstants.IMPORT;
import static org.onosproject.yangutils.utils.UtilConstants.JAVA_LANG;
import static org.onosproject.yangutils.utils.UtilConstants.JAVA_UTIL_OBJECTS_IMPORT_CLASS;
import static org.onosproject.yangutils.utils.UtilConstants.JAVA_UTIL_OBJECTS_IMPORT_PKG;
import static org.onosproject.yangutils.utils.UtilConstants.LIST;
import static org.onosproject.yangutils.utils.UtilConstants.LISTENER_REG;
import static org.onosproject.yangutils.utils.UtilConstants.LISTENER_SERVICE;
import static org.onosproject.yangutils.utils.UtilConstants.NEW_LINE;
import static org.onosproject.yangutils.utils.UtilConstants.ONOS_EVENT_PKG;
import static org.onosproject.yangutils.utils.UtilConstants.PERIOD;
import static org.onosproject.yangutils.utils.UtilConstants.PROVIDED_AUGMENTATION_CLASS_IMPORT_PKG;
import static org.onosproject.yangutils.utils.UtilConstants.SEMI_COLAN;

import static java.util.Collections.sort;

/**
 * Represents that generated Java file can contain imports.
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
     * Creates java import data object.
     */
    public JavaImportData() {
        setImportSet(new TreeSet<JavaQualifiedTypeInfo>());
    }

    /**
     * Returns if the list needs to be imported.
     *
     * @return true if any of the attribute needs to be maintained as a list
     */
    public boolean getIfListImported() {
        return isListToImport;
    }

    /**
     * Sets the status of importing list.
     *
     * @param isList status to mention list is bing imported
     */
    public void setIfListImported(boolean isList) {
        isListToImport = isList;
    }

    /**
     * Returns the set containing the imported class/interface info.
     *
     * @return the set containing the imported class/interface info
     */
    public SortedSet<JavaQualifiedTypeInfo> getImportSet() {
        return importSet;
    }

    /**
     * Assigns the set containing the imported class/interface info.
     *
     * @param importSet the set containing the imported class/interface info
     */
    private void setImportSet(SortedSet<JavaQualifiedTypeInfo> importSet) {
        this.importSet = importSet;
    }

    /**
     * Adds an imported class/interface info if it is not already part of the
     * collection.
     *
     * If already part of the collection, check if the packages are same, if so
     * then return true, to denote it is already in the import collection, and
     * it can be accessed without qualified access. If the packages do not
     * match, then do not add to the import collection, and return false to
     * denote, it is not added to import collection and needs to be accessed in
     * a qualified manner.
     *
     * @param newImportInfo class/interface info being imported
     * @param className     name of the call being generated
     * @param classPkg      generated class package
     * @return qualified access status of the import node being added
     */
    public boolean addImportInfo(JavaQualifiedTypeInfo newImportInfo,
            String className, String classPkg) {

        if (newImportInfo.getClassInfo().contentEquals(className)) {
            /*
             * if the current class name is same as the attribute class name,
             * then the attribute must be accessed in a qualified manner.
             */
            return true;
        } else if (newImportInfo.getPkgInfo() == null) {
            /*
             * If the package info is null, then it is not a candidate for import / qualified access
             */
            return false;
        }

        /*
         * If the attribute type is having the package info, it is contender
         * for import list and also need to check if it needs to be a
         * qualified access.
         */
        if (newImportInfo.getPkgInfo().contentEquals(classPkg)) {
            /**
             * Package of the referred attribute and the generated class is same, so no need import
             * or qualified access.
             */
            return false;
        }

        for (JavaQualifiedTypeInfo curImportInfo : getImportSet()) {
            if (curImportInfo.getClassInfo()
                    .contentEquals(newImportInfo.getClassInfo())) {
                return !curImportInfo.getPkgInfo()
                        .contentEquals(newImportInfo.getPkgInfo());
            }
        }

        /*
         * import is added, so it is a member for non qualified access
         */
        getImportSet().add(newImportInfo);
        return false;
    }

    /**
     * Returns import for class.
     *
     * @return imports for class
     */
    public List<String> getImports() {

        String importString;
        List<String> imports = new ArrayList<>();

        for (JavaQualifiedTypeInfo importInfo : getImportSet()) {
            if (!importInfo.getPkgInfo().equals(EMPTY_STRING) && importInfo.getClassInfo() != null
                    && !importInfo.getPkgInfo().equals(JAVA_LANG)) {
                importString = IMPORT + importInfo.getPkgInfo() + PERIOD + importInfo.getClassInfo() + SEMI_COLAN
                        + NEW_LINE;

                imports.add(importString);
            }
        }

        if (getIfListImported()) {
            imports.add(getImportForList());
        }

        sort(imports);
        return imports;
    }

    /**
     * Returns import for hash and equals method.
     *
     * @return import for hash and equals method
     */
    public String getImportForHashAndEquals() {
        return IMPORT + JAVA_UTIL_OBJECTS_IMPORT_PKG + PERIOD + JAVA_UTIL_OBJECTS_IMPORT_CLASS;
    }

    /**
     * Returns import for to string method.
     *
     * @return import for to string method
     */
    public String getImportForToString() {
        return IMPORT + GOOGLE_MORE_OBJECT_IMPORT_PKG + PERIOD + GOOGLE_MORE_OBJECT_IMPORT_CLASS;
    }

    /**
     * Returns import for list attribute.
     *
     * @return import for list attribute
     */
    public String getImportForList() {
        return IMPORT + COLLECTION_IMPORTS + PERIOD + LIST + SEMI_COLAN + NEW_LINE;
    }

    /**
     * Returns import for array list attribute.
     *
     * @return import for array list attribute
     */
    public String getImportForArrayList() {
        return IMPORT + COLLECTION_IMPORTS + PERIOD + ARRAY_LIST + SEMI_COLAN + NEW_LINE;
    }

    /**
     * Returns import string for AugmentationHolder class.
     *
     * @return import string for AugmentationHolder class
     */
    public String getAugmentationHolderImport() {
        return IMPORT + PROVIDED_AUGMENTATION_CLASS_IMPORT_PKG + PERIOD + AUGMENTATION_HOLDER_CLASS_IMPORT_CLASS;
    }

    /**
     * Returns import string for AugmentedInfo class.
     *
     * @return import string for AugmentedInfo class
     */
    public String getAugmentedInfoImport() {
        return IMPORT + AUGMENTED_INFO_CLASS_IMPORT_PKG + PERIOD + AUGMENTED_INFO_CLASS_IMPORT_CLASS;
    }

    /**
     * Returns import string for ListenerService class.
     *
     * @return import string for ListenerService class
     */
    public String getListenerServiceImport() {
        return IMPORT + ONOS_EVENT_PKG + PERIOD + LISTENER_SERVICE + SEMI_COLAN + NEW_LINE;
    }

    /**
     * Returns import string for ListenerRegistry class.
     *
     * @return import string for ListenerRegistry class
     */
    public String getListenerRegistryImport() {
        return IMPORT + ONOS_EVENT_PKG + PERIOD + LISTENER_REG + SEMI_COLAN + NEW_LINE;
    }

    /**
     * Returns import string for AbstractEvent class.
     *
     * @return import string for AbstractEvent class
     */
    public String getAbstractEventsImport() {
        return IMPORT + ONOS_EVENT_PKG + PERIOD + ABSTRACT_EVENT + SEMI_COLAN + NEW_LINE;
    }

    /**
     * Returns import string for EventListener class.
     *
     * @return import string for EventListener class
     */
    public String getEventListenerImport() {
        return IMPORT + ONOS_EVENT_PKG + PERIOD + EVENT_LISTENER + SEMI_COLAN + NEW_LINE;
    }
}

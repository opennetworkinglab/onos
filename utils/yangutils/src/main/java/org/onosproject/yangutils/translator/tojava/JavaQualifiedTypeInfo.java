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

import java.util.Objects;
import org.onosproject.yangutils.datamodel.YangNode;
import org.onosproject.yangutils.datamodel.YangType;
import org.onosproject.yangutils.translator.exception.TranslatorException;
import org.onosproject.yangutils.translator.tojava.utils.AttributesJavaDataType;
import com.google.common.base.MoreObjects;

import static org.onosproject.yangutils.translator.tojava.utils.AttributesJavaDataType.getJavaImportClass;
import static org.onosproject.yangutils.translator.tojava.utils.AttributesJavaDataType.getJavaImportPackage;

/**
 * Represents the information about individual imports in the generated file.
 */
public class JavaQualifiedTypeInfo implements Comparable<JavaQualifiedTypeInfo> {

    /**
     * Package location where the imported class/interface is defined.
     */
    private String pkgInfo;

    /**
     * Class/interface being referenced.
     */
    private String classInfo;

    /**
     * Creates a java qualified type info object.
     */
    public JavaQualifiedTypeInfo() {
    }

    /**
     * Returns the imported package info.
     *
     * @return the imported package info
     */
    public String getPkgInfo() {
        return pkgInfo;
    }

    /**
     * Sets the imported package info.
     *
     * @param pkgInfo the imported package info
     */
    public void setPkgInfo(String pkgInfo) {
        this.pkgInfo = pkgInfo;
    }

    /**
     * Returns the imported class/interface info.
     *
     * @return the imported class/interface info
     */
    public String getClassInfo() {
        return classInfo;
    }

    /**
     * Sets the imported class/interface info.
     *
     * @param classInfo the imported class/interface info
     */
    public void setClassInfo(String classInfo) {
        this.classInfo = classInfo;
    }

    /**
     * Returns the import info for an attribute, which needs to be used for code
     * generation for import or for qualified access.
     *
     * @param curNode       current data model node for which the java file is being
     *                      generated
     * @param attrType      type of attribute being added, it will be null, when the
     *                      child class is added as an attribute
     * @param attributeName name of the attribute being added, it will used in
     *                      import info for child class
     * @param isListAttr    is the added attribute going to be used as a list
     * @return return the import info for this attribute
     */
    public static JavaQualifiedTypeInfo getQualifiedTypeInfoOfAttribute(YangNode curNode,
                                                                        YangType<?> attrType, String attributeName,
                                                                        boolean isListAttr) {

        JavaQualifiedTypeInfo importInfo = new JavaQualifiedTypeInfo();

        if (attrType == null) {
            throw new TranslatorException("missing data type of leaf " + attributeName);
        }

        /*
         * Current leaves holder is adding a leaf info as a attribute to the
         * current class.
         */
        String className = getJavaImportClass(attrType, isListAttr);
        if (className != null) {
            /*
             * Corresponding to the attribute type a class needs to be imported,
             * since it can be a derived type or a usage of wrapper classes.
             */
            importInfo.setClassInfo(className);
            String classPkg = getJavaImportPackage(attrType, isListAttr, className);
            if (classPkg == null) {
                throw new TranslatorException("import package cannot be null when the class is used");
            }
            importInfo.setPkgInfo(classPkg);
        } else {
            /*
             * The attribute does not need a class to be imported, for example
             * built in java types.
             */
            String dataTypeName = AttributesJavaDataType.getJavaDataType(attrType);
            if (dataTypeName == null) {
                throw new TranslatorException("not supported data type");
            }
            importInfo.setClassInfo(dataTypeName);
        }
        return importInfo;
    }

    /**
     * Returns the import info for an attribute, which needs to be used for code
     * generation for import or for qualified access.
     *
     * @param curNode       current data model node for which the java file is being
     *                      generated
     * @param attributeName name of the attribute being added, it will used in
     *                      import info for child class
     * @param isListAttr    is the added attribute going to be used as a list
     * @return return the import info for this attribute
     */
    public static JavaQualifiedTypeInfo getQualifiedTypeInfoOfCurNode(YangNode curNode,
                                                                      String attributeName, boolean isListAttr) {

        JavaQualifiedTypeInfo importInfo = new JavaQualifiedTypeInfo();

        if (!(curNode instanceof HasJavaFileInfo)) {
            throw new TranslatorException("missing java file information to get the package details "
                    + "of attribute corresponding to child node");
        }
        /*
         * The scenario when we need to add the child class as an attribute in
         * the current class. The child class is in the package of the current
         * classes package with current classes name.
         */
        importInfo.setClassInfo(attributeName);
        importInfo.setPkgInfo((((HasJavaFileInfo) curNode).getJavaFileInfo().getPackage() + "."
                + ((HasJavaFileInfo) curNode).getJavaFileInfo().getJavaName()).toLowerCase());

        return importInfo;
    }

    /**
     * Returns the java qualified type information for the wrapper classes.
     *
     * @param referredTypesAttrInfo attribute of referred type
     * @return return the import info for this attribute
     */
    public static JavaQualifiedTypeInfo getQualifiedInfoOfFromString(JavaAttributeInfo referredTypesAttrInfo) {
        /*
         * Get the java qualified type information for the wrapper classes and
         * set it in new java attribute information.
         */
        JavaQualifiedTypeInfo qualifiedInfoOfFromString = new JavaQualifiedTypeInfo();
        qualifiedInfoOfFromString.setClassInfo(
                getJavaImportClass(referredTypesAttrInfo.getAttributeType(), true));
        qualifiedInfoOfFromString.setPkgInfo(
                getJavaImportPackage(referredTypesAttrInfo.getAttributeType(), true, null));
        return qualifiedInfoOfFromString;
    }

    /**
     * Returns if the attribute needs to be accessed in a qualified manner or not,
     * if it needs to be imported, then the same needs to be done.
     *
     * @param curNode    current cache of the data model node for which java file
     *                   is bing generated
     * @param importInfo import info for the current attribute being added
     * @return status of the qualified access to the attribute
     */
    public static boolean getIsQualifiedAccessOrAddToImportList(YangNode curNode,
                                                                JavaQualifiedTypeInfo importInfo) {

        boolean isImportPkgEqualCurNodePkg;
        if (!(curNode instanceof HasJavaFileInfo)) {
            throw new TranslatorException("missing java file info for getting the qualified access");
        }
        if (importInfo.getClassInfo().contentEquals(
                ((HasJavaFileInfo) curNode).getJavaFileInfo().getJavaName())) {
            /*
             * if the current class name is same as the attribute class name,
             * then the attribute must be accessed in a qualified manner.
             */
            return true;
        } else if (importInfo.getPkgInfo() != null) {
            /*
             * If the attribute type is having the package info, it is contender
             * for import list and also need to check if it needs to be a
             * qualified access.
             */
            isImportPkgEqualCurNodePkg = isImportPkgEqualCurNodePkg(curNode, importInfo);
            if (!isImportPkgEqualCurNodePkg) {
                /*
                 * If the package of the attribute added is not same as the
                 * current class package, then it must either be imported for
                 * access or it must be a qualified access.
                 */
                if (!(curNode instanceof HasJavaImportData)) {
                    /*
                     * If the current data model node is not supposed to import
                     * data, then this is a usage issue and needs to be fixed.
                     */
                    throw new TranslatorException("Current node needs to support Imports");
                }

                boolean isImportAdded = ((HasJavaImportData) curNode).getJavaImportData()
                        .addImportInfo(curNode, importInfo);
                if (!isImportAdded) {
                    /*
                     * If the attribute type info is not imported, then it must
                     * be a qualified access.
                     */
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Checks if the import info is same as the package of the current generated
     * java file.
     *
     * @param curNode    Java identifier of the current data model node
     * @param importInfo import info for an attribute
     * @return true if the import info is same as the current nodes package
     * false otherwise
     */
    public static boolean isImportPkgEqualCurNodePkg(
            YangNode curNode, JavaQualifiedTypeInfo importInfo) {

        if (!(curNode instanceof HasJavaFileInfo)) {
            throw new TranslatorException("missing java file info for the data model node");
        }
        return ((HasJavaFileInfo) curNode).getJavaFileInfo().getPackage()
                .contentEquals(importInfo.getPkgInfo());
    }

    @Override
    public int hashCode() {
        return Objects.hash(pkgInfo, classInfo);
    }

    @Override
    public boolean equals(Object obj) {

        if (this == obj) {
            return true;
        }
        if (obj instanceof JavaQualifiedTypeInfo) {
            JavaQualifiedTypeInfo other = (JavaQualifiedTypeInfo) obj;
            return Objects.equals(pkgInfo, other.pkgInfo) &&
                    Objects.equals(classInfo, other.classInfo);
        }
        return false;
    }

    /**
     * Checks if the import info matches.
     *
     * @param importInfo matched import
     * @return if equal or not
     */
    public boolean exactMatch(JavaQualifiedTypeInfo importInfo) {
        return equals(importInfo)
                && Objects.equals(pkgInfo, importInfo.getPkgInfo())
                && Objects.equals(classInfo, importInfo.getClassInfo());
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("pkgInfo", pkgInfo)
                .add("classInfo", classInfo).toString();
    }

    /**
     * Checks that there is no 2 objects with the same class name.
     *
     * @param other compared import info.
     */
    @Override
    public int compareTo(JavaQualifiedTypeInfo other) {
        return getClassInfo().compareTo(other.getClassInfo());
    }

}

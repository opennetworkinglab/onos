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

import java.io.Serializable;
import java.util.Objects;

import org.onosproject.yangutils.datamodel.YangNode;
import org.onosproject.yangutils.translator.exception.TranslatorException;
import org.onosproject.yangutils.translator.tojava.javamodel.JavaLeafInfoContainer;
import org.onosproject.yangutils.translator.tojava.javamodel.AttributesJavaDataType;
import org.onosproject.yangutils.utils.io.impl.YangToJavaNamingConflictUtil;

import com.google.common.base.MoreObjects;

import static org.onosproject.yangutils.translator.tojava.javamodel.AttributesJavaDataType.getJavaImportClass;
import static org.onosproject.yangutils.translator.tojava.javamodel.AttributesJavaDataType.getJavaImportPackage;

/**
 * Represents the information about individual imports in the generated file.
 */
public class JavaQualifiedTypeInfo
        implements Comparable<JavaQualifiedTypeInfo>, Serializable {

    private static final long serialVersionUID = 806201634L;

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
     * Updates the leaf's java information.
     *
     * @param leaf leaf whose java information is being updated
     */
    public static void updateLeavesJavaQualifiedInfo(JavaLeafInfoContainer leaf) {

        JavaQualifiedTypeInfo importInfo = leaf.getJavaQualifiedInfo();

        if (leaf.getDataType() == null) {
            throw new TranslatorException("missing data type of leaf " + leaf.getName());
        }

        /*
         * Current leaves holder is adding a leaf info as a attribute to the
         * current class.
         */
        String className = AttributesJavaDataType.getJavaImportClass(leaf.getDataType(), leaf.isLeafList(),
                leaf.getConflictResolveConfig());
        if (className != null) {
            /*
             * Corresponding to the attribute type a class needs to be imported,
             * since it can be a derived type or a usage of wrapper classes.
             */
            importInfo.setClassInfo(className);
            String classPkg = AttributesJavaDataType.getJavaImportPackage(leaf.getDataType(),
                    leaf.isLeafList(), leaf.getConflictResolveConfig());
            if (classPkg == null) {
                throw new TranslatorException("import package cannot be null when the class is used");
            }
            importInfo.setPkgInfo(classPkg);
        } else {
            /*
             * The attribute does not need a class to be imported, for example
             * built in java types.
             */
            String dataTypeName = AttributesJavaDataType.getJavaDataType(leaf.getDataType());
            if (dataTypeName == null) {
                throw new TranslatorException("not supported data type");
            }
            importInfo.setClassInfo(dataTypeName);
        }
    }

    /**
     * Returns the import info for an attribute, which needs to be used for code
     * generation for import or for qualified access.
     *
     * @param curNode       current data model node for which the java file is being
     *                      generated
     * @param attributeName name of the attribute being added, it will used in
     *                      import info for child class
     * @return return the import info for this attribute
     */
    public static JavaQualifiedTypeInfo getQualifiedTypeInfoOfCurNode(YangNode curNode,
            String attributeName) {

        JavaQualifiedTypeInfo importInfo = new JavaQualifiedTypeInfo();

        if (!(curNode instanceof JavaFileInfoContainer)) {
            throw new TranslatorException("missing java file information to get the package details "
                    + "of attribute corresponding to child node");
        }

        importInfo.setClassInfo(attributeName);
        importInfo.setPkgInfo(((JavaFileInfoContainer) curNode)
                .getJavaFileInfo().getPackage());

        return importInfo;
    }

    /**
     * Returns the java qualified type information for the wrapper classes.
     *
     * @param referredTypesAttrInfo attribute of referred type
     * @param conflictResolver      plugin configurations
     * @return return the import info for this attribute
     */
    public static JavaQualifiedTypeInfo getQualifiedInfoOfFromString(JavaAttributeInfo referredTypesAttrInfo,
            YangToJavaNamingConflictUtil conflictResolver) {

        /*
         * Get the java qualified type information for the wrapper classes and
         * set it in new java attribute information.
         */
        JavaQualifiedTypeInfo qualifiedInfoOfFromString = new JavaQualifiedTypeInfo();

        qualifiedInfoOfFromString.setClassInfo(
                getJavaImportClass(referredTypesAttrInfo.getAttributeType(), true, conflictResolver));
        qualifiedInfoOfFromString.setPkgInfo(
                getJavaImportPackage(referredTypesAttrInfo.getAttributeType(), true, conflictResolver));
        return qualifiedInfoOfFromString;
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

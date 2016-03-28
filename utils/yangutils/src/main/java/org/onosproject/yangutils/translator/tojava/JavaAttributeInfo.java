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

import org.onosproject.yangutils.datamodel.YangNode;
import org.onosproject.yangutils.datamodel.YangType;
import org.onosproject.yangutils.translator.tojava.utils.AttributesJavaDataType;

import static org.onosproject.yangutils.translator.tojava.JavaQualifiedTypeInfo.getIsQualifiedAccessOrAddToImportList;
import static org.onosproject.yangutils.translator.tojava.JavaQualifiedTypeInfo.getQualifiedTypeInfoOfCurNode;
import static org.onosproject.yangutils.translator.tojava.JavaQualifiedTypeInfo.getQualifiedTypeInfoOfLeafAttribute;
import static org.onosproject.yangutils.translator.tojava.utils.JavaIdentifierSyntax.getCamelCase;

/**
 * Maintains the attribute info corresponding to class/interface generated.
 */
public final class JavaAttributeInfo {

    /**
     * The data type info of attribute.
     */
    private YangType<?> attrType;

    /**
     * Name of the attribute.
     */
    private String name;

    /**
     * If the added attribute is a list of info.
     */
    private boolean isListAttr = false;

    /**
     * If the added attribute has to be accessed in a fully qualified manner.
     */
    private boolean isQualifiedName = false;

    /**
     * The class info will be used to set the attribute type and package info
     * will be use for qualified name.
     */
    private JavaQualifiedTypeInfo importInfo;

    /**
     * Default constructor.
     */
    private JavaAttributeInfo() {
    }

    /**
     * Construct object of java attribute info.
     *
     * @param attrType YANG type
     * @param name attribute name
     * @param isListAttr is list attribute
     * @param isQualifiedName is qualified name
     */
    public JavaAttributeInfo(YangType<?> attrType, String name, boolean isListAttr, boolean isQualifiedName) {
        this.attrType = attrType;
        this.name = name;
        this.isListAttr = isListAttr;
        this.isQualifiedName = isQualifiedName;
    }

    /**
     * Get the data type info of attribute.
     *
     * @return the data type info of attribute
     */
    public YangType<?> getAttributeType() {

        if (attrType == null) {
            throw new RuntimeException("Expected java attribute type is null");
        }
        return attrType;
    }

    /**
     * Set the data type info of attribute.
     *
     * @param type the data type info of attribute
     */
    public void setAttributeType(YangType<?> type) {

        attrType = type;
    }

    /**
     * Get name of the attribute.
     *
     * @return name of the attribute
     */
    public String getAttributeName() {

        if (name == null) {
            throw new RuntimeException("Expected java attribute name is null");
        }
        return name;
    }

    /**
     * Set name of the attribute.
     *
     * @param attrName name of the attribute
     */
    public void setAttributeName(String attrName) {

        name = attrName;
    }

    /**
     * Get if the added attribute is a list of info.
     *
     * @return the if the added attribute is a list of info
     */
    public boolean isListAttr() {

        return isListAttr;
    }

    /**
     * Set if the added attribute is a list of info.
     *
     * @param isList if the added attribute is a list of info
     */
    public void setListAttr(boolean isList) {

        isListAttr = isList;
    }

    /**
     * Get if the added attribute has to be accessed in a fully qualified
     * manner.
     *
     * @return the if the added attribute has to be accessed in a fully
     *         qualified manner.
     */
    public boolean isQualifiedName() {

        return isQualifiedName;
    }

    /**
     * Set if the added attribute has to be accessed in a fully qualified
     * manner.
     *
     * @param isQualified if the added attribute has to be accessed in a fully
     *            qualified manner
     */
    public void setIsQualifiedAccess(boolean isQualified) {

        isQualifiedName = isQualified;
    }

    /**
     * Get the import info for the attribute type. It will be null, of the type
     * is basic built-in java type.
     *
     * @return import info
     */
    public JavaQualifiedTypeInfo getImportInfo() {

        return importInfo;
    }

    /**
     * Set the import info for the attribute type.
     *
     * @param importInfo import info for the attribute type
     */
    public void setImportInfo(JavaQualifiedTypeInfo importInfo) {

        this.importInfo = importInfo;
    }

    /**
     * Create an attribute info object corresponding to the passed leaf
     * information and return it.
     *
     * @param curNode current data model node for which the java file is being
     *            generated
     * @param attributeType leaf data type
     * @param attributeName leaf name
     * @param isListAttribute is the current added attribute needs to be a list
     * @return AttributeInfo attribute details required to add in temporary
     *         files
     */
    public static JavaAttributeInfo getAttributeInfoOfLeaf(YangNode curNode,
            YangType<?> attributeType, String attributeName,
            boolean isListAttribute) {

        JavaAttributeInfo newAttr = new JavaAttributeInfo();

        /*
         * Get the import info corresponding to the attribute for import in
         * generated java files or qualified access
         */
        JavaQualifiedTypeInfo importInfo = getQualifiedTypeInfoOfLeafAttribute(curNode,
                attributeType, attributeName, isListAttribute);
        newAttr.setImportInfo(importInfo);
        newAttr.setIsQualifiedAccess(getIsQualifiedAccessOrAddToImportList(
                curNode, importInfo));
        newAttr.setAttributeName(getCamelCase(attributeName));
        newAttr.setListAttr(isListAttribute);
        newAttr.setImportInfo(importInfo);
        newAttr.setAttributeType(attributeType);

        return newAttr;
    }

    /**
     * Create an attribute info object corresponding to a data model node and
     * return it.
     *
     * @param curNode current data model node for which the java code generation
     *            is being handled
     * @param parentNode parent node in which the current node is an attribute
     * @param isListNode is the current added attribute needs to be a list
     * @return AttributeInfo attribute details required to add in temporary
     *         files
     */
    public static JavaAttributeInfo getCurNodeAsAttributeInParent(
            YangNode curNode, YangNode parentNode, boolean isListNode) {

        JavaAttributeInfo newAttr = new JavaAttributeInfo();

        //        if (curNode instanceof HasJavaFileInfo) {
        //          throw new RuntimeException("translator data model node does not have java info");
        //    }

        String curNodeName = ((HasJavaFileInfo) curNode).getJavaFileInfo().getJavaName();

        /*
         * Get the import info corresponding to the attribute for import in
         * generated java files or qualified access
         */
        JavaQualifiedTypeInfo qualifiedTypeInfo = getQualifiedTypeInfoOfCurNode(parentNode,
                curNodeName, isListNode);
        newAttr.setImportInfo(qualifiedTypeInfo);
        newAttr.setIsQualifiedAccess(
                getIsQualifiedAccessOrAddToImportList(parentNode,
                        qualifiedTypeInfo));
        newAttr.setAttributeName(getCamelCase(curNodeName));
        newAttr.setListAttr(isListNode);
        newAttr.setImportInfo(qualifiedTypeInfo);
        newAttr.setAttributeType(null);

        return newAttr;
    }

    /**
     * Create an attribute info object corresponding to the passed type def attribute
     * information and return it.
     *
     * @param curNode current data model node for which the java file is being
     *            generated
     * @param attributeType leaf data type
     * @param attributeName leaf name
     * @param isListAttribute is the current added attribute needs to be a list
     * @return AttributeInfo attribute details required to add in temporary
     *         files
     */
    public static JavaAttributeInfo getAttributeInfoOfTypeDef(YangNode curNode,
            YangType<?> attributeType, String attributeName,
            boolean isListAttribute) {

        JavaAttributeInfo newAttr = new JavaAttributeInfo();

        /*
         * Get the import info corresponding to the attribute for import in
         * generated java files or qualified access
         */
        JavaQualifiedTypeInfo importInfo = getQualifiedTypeInfoOfLeafAttribute(curNode,
                attributeType, attributeName, isListAttribute);
        AttributesJavaDataType.addImportInfo(importInfo);
        newAttr.setImportInfo(importInfo);
        newAttr.setIsQualifiedAccess(getIsQualifiedAccessOrAddToImportList(
                curNode, importInfo));
        newAttr.setAttributeName(getCamelCase(attributeName));
        newAttr.setListAttr(isListAttribute);
        newAttr.setImportInfo(importInfo);
        newAttr.setAttributeType(attributeType);

        return newAttr;
    }
}

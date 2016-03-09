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

package org.onosproject.yangutils.translator.tojava.utils;

import org.onosproject.yangutils.translator.tojava.AttributeInfo;
import org.onosproject.yangutils.utils.UtilConstants;
import org.onosproject.yangutils.utils.io.impl.JavaDocGen;
import org.onosproject.yangutils.utils.io.impl.YangIoUtils;

/**
 * Generated methods for generated files based on the file type.
 */
public final class MethodsGenerator {

    /**
     * Default constructor.
     */
    private MethodsGenerator() {
    }

    /**
     * Returns the methods strings for builder interface.
     *
     * @param attr attribute info
     * @param className name of the java class being generated
     * @return method string for builder interface
     */
    static String parseBuilderInterfaceMethodString(AttributeInfo attr, String className) {

        return getGetterString(attr) + UtilConstants.NEW_LINE + getSetterString(attr, className);
    }

    /**
     * Returns the methods strings for builder interface.
     *
     * @param name attribute name
     * @return method string for builder interface
     */
    public static String parseBuilderInterfaceBuildMethodString(String name) {

        return JavaDocGen.getJavaDoc(JavaDocGen.JavaDocType.BUILD, name, false)
                + getBuildForInterface(name);
    }

    /**
     * Returns getter string.
     *
     * @param attr attribute info
     * @return getter string
     */
    public static String getGetterString(AttributeInfo attr) {

        String returnType = "";
        boolean isList = attr.isListAttr();
        if (attr.isQualifiedName() && (attr.getImportInfo().getPkgInfo() != null)) {
            returnType = attr.getImportInfo().getPkgInfo() + ".";
        }

        returnType = returnType + attr.getImportInfo().getClassInfo();
        String attributeName = JavaIdentifierSyntax.getLowerCase(attr.getAttributeName());

        return JavaDocGen.getJavaDoc(JavaDocGen.JavaDocType.GETTER, attributeName, isList) +

                getGetterForInterface(attributeName, returnType, attr.isListAttr())
                + UtilConstants.NEW_LINE;
    }

    /**
     * Returns setter string.
     *
     * @param attr attribute info
     * @param className java class name
     * @return setter string
     */
    public static String getSetterString(AttributeInfo attr, String className) {

        String attrType = "";
        boolean isList = attr.isListAttr();
        if (attr.isQualifiedName() && (attr.getImportInfo().getPkgInfo() != null)) {
            attrType = attr.getImportInfo().getPkgInfo() + ".";
        }

        attrType = attrType + attr.getImportInfo().getClassInfo();
        String attributeName = JavaIdentifierSyntax.getLowerCase(attr.getAttributeName());

        return JavaDocGen.getJavaDoc(JavaDocGen.JavaDocType.SETTER, attributeName, isList)
                + getSetterForInterface(attributeName, attrType, className, attr.isListAttr())
                + UtilConstants.NEW_LINE;
    }

    /**
     * Returns constructor method string.
     *
     * @param name class name
     * @return constructor string
     */
    public static String getConstructorString(String name) {

        return JavaDocGen.getJavaDoc(JavaDocGen.JavaDocType.CONSTRUCTOR, name, false);
    }

    /**
     * Returns default constructor method string.
     *
     * @param name class name
     * @param modifierType modifier type
     * @return default constructor string
     */
    public static String getDefaultConstructorString(String name, String modifierType) {

        return JavaDocGen.getJavaDoc(JavaDocGen.JavaDocType.DEFAULT_CONSTRUCTOR, name, false)
                + getDefaultConstructor(name, modifierType);
    }

    /**
     * Returns default constructor method string.
     *
     * @param attr attribute info
     * @param className class name
     * @return default constructor string
     */
    public static String getTypeDefConstructor(AttributeInfo attr, String className) {

        String attrQuaifiedType = "";
        if (attr.isQualifiedName() && (attr.getImportInfo().getPkgInfo() != null)) {
            attrQuaifiedType = attr.getImportInfo().getPkgInfo() + ".";
        }
        attrQuaifiedType = attrQuaifiedType + attr.getImportInfo().getClassInfo();
        String attributeName = JavaIdentifierSyntax.getLowerCase(attr.getAttributeName());

        if (!attr.isListAttr()) {
            return getTypeDefConstructorString(attrQuaifiedType, attributeName, className);
        }
        String listAttr = getListString() + attrQuaifiedType + UtilConstants.DIAMOND_CLOSE_BRACKET;
        return getTypeDefConstructorString(listAttr, attributeName, className);
    }

    /**
     * Returns type def's constructor for attribute.
     *
     * @param type data type
     * @param name attribute name
     * @param className class name
     * @return setter for type def's attribute
     */
    private static String getTypeDefConstructorString(String type, String name, String className) {

        return UtilConstants.FOUR_SPACE_INDENTATION + UtilConstants.PUBLIC + UtilConstants.SPACE
                + className + UtilConstants.OPEN_PARENTHESIS
                + type + UtilConstants.SPACE + "value" + UtilConstants.CLOSE_PARENTHESIS
                + UtilConstants.SPACE + UtilConstants.OPEN_CURLY_BRACKET + UtilConstants.NEW_LINE
                + UtilConstants.EIGHT_SPACE_INDENTATION + UtilConstants.THIS
                + UtilConstants.PERIOD + name + UtilConstants.SPACE + UtilConstants.EQUAL + UtilConstants.SPACE
                + "value" + UtilConstants.SEMI_COLAN + UtilConstants.NEW_LINE + UtilConstants.FOUR_SPACE_INDENTATION
                + UtilConstants.CLOSE_CURLY_BRACKET;
    }

    /**
     * Returns check not null string.
     *
     * @param name attribute name
     * @return check not null string
     */
    public static String getCheckNotNull(String name) {
        return UtilConstants.EIGHT_SPACE_INDENTATION + UtilConstants.CHECK_NOT_NULL_STRING
                + UtilConstants.OPEN_PARENTHESIS + name + UtilConstants.COMMA + UtilConstants.SPACE + name
                + UtilConstants.CLOSE_PARENTHESIS + UtilConstants.SEMI_COLAN + UtilConstants.NEW_LINE;
    }

    /**
     * Returns build method string.
     *
     * @param name class name
     * @return build string
     */
    public static String getBuildString(String name) {

        return UtilConstants.FOUR_SPACE_INDENTATION + UtilConstants.OVERRIDE + UtilConstants.NEW_LINE
                + getBuild(name);
    }

    /**
     * Returns the getter method strings for class file.
     *
     * @param attr attribute info
     * @return getter method for class
     */
    public static String getGetterForClass(AttributeInfo attr) {

        String attrQuaifiedType = "";
        if (attr.isQualifiedName() && (attr.getImportInfo().getPkgInfo() != null)) {
            attrQuaifiedType = attr.getImportInfo().getPkgInfo() + ".";
        }
        attrQuaifiedType = attrQuaifiedType + attr.getImportInfo().getClassInfo();
        String attributeName = JavaIdentifierSyntax.getLowerCase(attr.getAttributeName());

        if (!attr.isListAttr()) {
            return getGetter(attrQuaifiedType, attributeName);
        }
        String listAttr = getListString() + attrQuaifiedType + UtilConstants.DIAMOND_CLOSE_BRACKET;
        return getGetter(listAttr, attributeName);
    }

    /**
     * Returns getter for attribute.
     *
     * @param type return type
     * @param name attribute name
     * @return getter for attribute
     */
    private static String getGetter(String type, String name) {

        return UtilConstants.FOUR_SPACE_INDENTATION + UtilConstants.PUBLIC + UtilConstants.SPACE
                + type + UtilConstants.SPACE + UtilConstants.GET_METHOD_PREFIX
                + JavaIdentifierSyntax.getCaptialCase(name) + UtilConstants.OPEN_PARENTHESIS
                + UtilConstants.CLOSE_PARENTHESIS + UtilConstants.SPACE + UtilConstants.OPEN_CURLY_BRACKET
                + UtilConstants.NEW_LINE + UtilConstants.EIGHT_SPACE_INDENTATION + UtilConstants.RETURN
                + UtilConstants.SPACE + name + UtilConstants.SEMI_COLAN + UtilConstants.NEW_LINE
                + UtilConstants.FOUR_SPACE_INDENTATION + UtilConstants.CLOSE_CURLY_BRACKET;
    }

    /**
     * Returns the setter method strings for class file.
     *
     * @param attr attribute info
     * @param className name of the class
     * @return setter method for class
     */
    public static String getSetterForClass(AttributeInfo attr, String className) {

        String attrQuaifiedType = "";
        if (attr.isQualifiedName() && (attr.getImportInfo().getPkgInfo() != null)) {
            attrQuaifiedType = attr.getImportInfo().getPkgInfo() + ".";
        }
        attrQuaifiedType = attrQuaifiedType + attr.getImportInfo().getClassInfo();
        String attributeName = JavaIdentifierSyntax.getLowerCase(attr.getAttributeName());
        if (!attr.isListAttr()) {
            return getSetter(className, attributeName, attrQuaifiedType);
        }
        String listAttr = getListString() + attrQuaifiedType + UtilConstants.DIAMOND_CLOSE_BRACKET;
        return getSetter(className, attributeName, listAttr);
    }

    /**
     * Returns setter for attribute.
     *
     * @param className class name
     * @param name attribute name
     * @param type return type
     * @return setter for attribute
     */
    private static String getSetter(String className, String name, String type) {

        return UtilConstants.FOUR_SPACE_INDENTATION + UtilConstants.PUBLIC + UtilConstants.SPACE
                + className + UtilConstants.BUILDER + UtilConstants.SPACE + UtilConstants.SET_METHOD_PREFIX
                + JavaIdentifierSyntax.getCaptialCase(name) + UtilConstants.OPEN_PARENTHESIS
                + type + UtilConstants.SPACE + name + UtilConstants.CLOSE_PARENTHESIS
                + UtilConstants.SPACE + UtilConstants.OPEN_CURLY_BRACKET + UtilConstants.NEW_LINE
                + UtilConstants.EIGHT_SPACE_INDENTATION + UtilConstants.THIS + UtilConstants.PERIOD
                + name + UtilConstants.SPACE + UtilConstants.EQUAL + UtilConstants.SPACE
                + name + UtilConstants.SEMI_COLAN + UtilConstants.NEW_LINE + UtilConstants.EIGHT_SPACE_INDENTATION
                + UtilConstants.RETURN + UtilConstants.SPACE + UtilConstants.THIS + UtilConstants.SEMI_COLAN
                + UtilConstants.NEW_LINE + UtilConstants.FOUR_SPACE_INDENTATION + UtilConstants.CLOSE_CURLY_BRACKET;
    }

    /**
     * Returns the setter method strings for class file.
     *
     * @param attr attribute info
     * @return setter method for class
     */
    public static String getSetterForTypeDefClass(AttributeInfo attr) {

        String attrQuaifiedType = "";
        if (attr.isQualifiedName() && (attr.getImportInfo().getPkgInfo() != null)) {
            attrQuaifiedType = attr.getImportInfo().getPkgInfo() + ".";
        }
        attrQuaifiedType = attrQuaifiedType + attr.getImportInfo().getClassInfo();
        String attributeName = JavaIdentifierSyntax.getLowerCase(attr.getAttributeName());

        if (!attr.isListAttr()) {
            return getTypeDefSetter(attrQuaifiedType, attributeName);
        }
        String listAttr = getListString() + attrQuaifiedType + UtilConstants.DIAMOND_CLOSE_BRACKET;
        return getTypeDefSetter(listAttr, attributeName);
    }

    /**
     * Returns type def's setter for attribute.
     *
     * @param type data type
     * @param name attribute name
     * @return setter for type def's attribute
     */
    private static String getTypeDefSetter(String type, String name) {

        return UtilConstants.FOUR_SPACE_INDENTATION + UtilConstants.PUBLIC + UtilConstants.SPACE
                + UtilConstants.VOID + UtilConstants.SPACE + UtilConstants.SET_METHOD_PREFIX
                + JavaIdentifierSyntax.getCaptialCase(name) + UtilConstants.OPEN_PARENTHESIS
                + type + UtilConstants.SPACE + "value" + UtilConstants.CLOSE_PARENTHESIS
                + UtilConstants.SPACE + UtilConstants.OPEN_CURLY_BRACKET + UtilConstants.NEW_LINE
                + UtilConstants.EIGHT_SPACE_INDENTATION + UtilConstants.THIS + UtilConstants.PERIOD
                + name + UtilConstants.SPACE + UtilConstants.EQUAL + UtilConstants.SPACE
                + "value" + UtilConstants.SEMI_COLAN + UtilConstants.NEW_LINE + UtilConstants.FOUR_SPACE_INDENTATION
                + UtilConstants.CLOSE_CURLY_BRACKET;
    }

    /**
     * Returns override string.
     *
     * @return override string
     */
    public static String getOverRideString() {
        return UtilConstants.NEW_LINE + UtilConstants.FOUR_SPACE_INDENTATION
                + UtilConstants.OVERRIDE + UtilConstants.NEW_LINE;
    }

    /**
     * Returns the getter method strings for interface file.
     *
     * @param yangName name of the attribute
     * @param returnType return type of attribute
     * @param isList is list attribute
     * @return getter method for interface
     */
    public static String getGetterForInterface(String yangName, String returnType, boolean isList) {

        if (!isList) {
            return getGetterInterfaceString(returnType, yangName);
        }
        String listAttr = getListString() + returnType + UtilConstants.DIAMOND_CLOSE_BRACKET;
        return getGetterInterfaceString(listAttr, yangName);
    }

    /**
     * Returns getter for attribute in interface.
     *
     * @param returnType return type
     * @param yangName attribute name
     * @return getter for interface
     */
    private static String getGetterInterfaceString(String returnType, String yangName) {

        return UtilConstants.FOUR_SPACE_INDENTATION + returnType
                + UtilConstants.SPACE + UtilConstants.GET_METHOD_PREFIX
                + JavaIdentifierSyntax.getCaptialCase(yangName)
                + UtilConstants.OPEN_PARENTHESIS + UtilConstants.CLOSE_PARENTHESIS
                + UtilConstants.SEMI_COLAN;

    }

    /**
     * Returns the setter method strings for interface file.
     *
     * @param attrName name of the attribute
     * @param attrType return type of attribute
     * @param className name of the java class being generated
     * @param isList is list attribute
     * @return setter method for interface
     */
    public static String getSetterForInterface(String attrName, String attrType, String className, boolean isList) {

        if (!isList) {
            return getSetterInterfaceString(className, attrName, attrType);
        }
        String listAttr = getListString() + attrType + UtilConstants.DIAMOND_CLOSE_BRACKET;
        return getSetterInterfaceString(className, attrName, listAttr);
    }

    /**
     * Returns setter string for interface.
     *
     * @param className class name
     * @param attrName attribute name
     * @param attrType attribute type
     * @return setter string
     */
    private static String getSetterInterfaceString(String className, String attrName, String attrType) {

        return UtilConstants.FOUR_SPACE_INDENTATION + className + UtilConstants.BUILDER
                + UtilConstants.SPACE + UtilConstants.SET_METHOD_PREFIX
                + JavaIdentifierSyntax.getCaptialCase(attrName) + UtilConstants.OPEN_PARENTHESIS
                + attrType + UtilConstants.SPACE + attrName + UtilConstants.CLOSE_PARENTHESIS
                + UtilConstants.SEMI_COLAN;
    }

    /**
     * Returns list string.
     *
     * @return list string
     */
    private static String getListString() {
        return UtilConstants.LIST + UtilConstants.DIAMOND_OPEN_BRACKET;
    }

    /**
     * Returns the build method strings for interface file.
     *
     * @param yangName name of the interface
     * @return build method for interface
     */
    public static String getBuildForInterface(String yangName) {

        return UtilConstants.FOUR_SPACE_INDENTATION + yangName + UtilConstants.SPACE + UtilConstants.BUILD
                + UtilConstants.OPEN_PARENTHESIS + UtilConstants.CLOSE_PARENTHESIS + UtilConstants.SEMI_COLAN
                + UtilConstants.NEW_LINE;
    }

    /**
     * Returns the constructor strings for class file.
     *
     * @param yangName name of the class
     * @param attr attribute info
     * @return constructor for class
     */
    public static String getConstructor(String yangName, AttributeInfo attr) {

        String builderAttribute = JavaIdentifierSyntax.getLowerCase(yangName);
        String attributeName = JavaIdentifierSyntax.getLowerCase(attr.getAttributeName());
        String constructor = UtilConstants.EIGHT_SPACE_INDENTATION + UtilConstants.THIS
                + UtilConstants.PERIOD + JavaIdentifierSyntax.getCamelCase(attributeName)
                + UtilConstants.SPACE + UtilConstants.EQUAL + UtilConstants.SPACE + builderAttribute
                + UtilConstants.BUILDER + UtilConstants.OBJECT + UtilConstants.PERIOD + UtilConstants.GET_METHOD_PREFIX
                + JavaIdentifierSyntax.getCaptialCase(JavaIdentifierSyntax.getCamelCase(attributeName))
                + UtilConstants.OPEN_PARENTHESIS + UtilConstants.CLOSE_PARENTHESIS + UtilConstants.SEMI_COLAN
                + UtilConstants.NEW_LINE;

        return constructor;
    }

    /**
     * Returns the build method strings for class file.
     *
     * @param yangName class name
     * @return build method string for class
     */
    public static String getBuild(String yangName) {

        return UtilConstants.FOUR_SPACE_INDENTATION + UtilConstants.PUBLIC + UtilConstants.SPACE
                + yangName + UtilConstants.SPACE + UtilConstants.BUILD + UtilConstants.OPEN_PARENTHESIS
                + UtilConstants.CLOSE_PARENTHESIS + UtilConstants.SPACE + UtilConstants.OPEN_CURLY_BRACKET
                + UtilConstants.NEW_LINE + UtilConstants.EIGHT_SPACE_INDENTATION + UtilConstants.RETURN
                + UtilConstants.SPACE + UtilConstants.NEW + UtilConstants.SPACE + yangName + UtilConstants.IMPL
                + UtilConstants.OPEN_PARENTHESIS + UtilConstants.THIS + UtilConstants.CLOSE_PARENTHESIS
                + UtilConstants.SEMI_COLAN + UtilConstants.NEW_LINE + UtilConstants.FOUR_SPACE_INDENTATION
                + UtilConstants.CLOSE_CURLY_BRACKET;
    }

    /**
     * Returns the Default constructor strings for class file.
     *
     * @param name name of the class
     * @param modifierType modifier type for default constructor
     * @return Default constructor for class
     */
    private static String getDefaultConstructor(String name, String modifierType) {

        return UtilConstants.FOUR_SPACE_INDENTATION + modifierType + UtilConstants.SPACE + name
                + UtilConstants.OPEN_PARENTHESIS + UtilConstants.CLOSE_PARENTHESIS + UtilConstants.SPACE
                + UtilConstants.OPEN_CURLY_BRACKET + UtilConstants.NEW_LINE + UtilConstants.FOUR_SPACE_INDENTATION
                + UtilConstants.CLOSE_CURLY_BRACKET + UtilConstants.NEW_LINE;
    }

    /**
     * Returns to string method open strings.
     *
     * @return to string method open string
     */
    public static String getToStringMethodOpen() {

        return getOverRideString() + UtilConstants.FOUR_SPACE_INDENTATION + UtilConstants.PUBLIC + UtilConstants.SPACE
                + UtilConstants.STRING + UtilConstants.SPACE + "to" + UtilConstants.STRING
                + UtilConstants.OPEN_PARENTHESIS + UtilConstants.CLOSE_PARENTHESIS + UtilConstants.SPACE
                + UtilConstants.OPEN_CURLY_BRACKET + UtilConstants.NEW_LINE + UtilConstants.EIGHT_SPACE_INDENTATION
                + UtilConstants.RETURN + " MoreObjects.toStringHelper(getClass())" + UtilConstants.NEW_LINE;
    }

    /**
     * Returns to string methods close string.
     *
     * @return to string method close string
     */
    public static String getToStringMethodClose() {
        return UtilConstants.TWELVE_SPACE_INDENTATION + ".to" + UtilConstants.STRING
                + UtilConstants.OPEN_PARENTHESIS + UtilConstants.CLOSE_PARENTHESIS + UtilConstants.SEMI_COLAN
                + UtilConstants.NEW_LINE + UtilConstants.FOUR_SPACE_INDENTATION
                + UtilConstants.CLOSE_CURLY_BRACKET;
    }

    /**
     * To string method for class.
     *
     * @param attr attribute info
     * @return to string method
     */
    public static String getToStringMethod(AttributeInfo attr) {
        String attributeName = JavaIdentifierSyntax.getLowerCase(attr.getAttributeName());
        return UtilConstants.TWELVE_SPACE_INDENTATION + UtilConstants.PERIOD + UtilConstants.ADD_STRING
                + UtilConstants.OPEN_PARENTHESIS + UtilConstants.QUOTES
                + attributeName + UtilConstants.QUOTES + UtilConstants.COMMA + UtilConstants.SPACE + attributeName
                + UtilConstants.CLOSE_PARENTHESIS;

    }

    /**
     * Returns to hash code method open strings.
     *
     * @return to hash code method open string
     */
    public static String getHashCodeMethodOpen() {

        return getOverRideString() + UtilConstants.FOUR_SPACE_INDENTATION + UtilConstants.PUBLIC + UtilConstants.SPACE
                + UtilConstants.INT + UtilConstants.SPACE + UtilConstants.HASH_CODE_STRING
                + UtilConstants.OPEN_PARENTHESIS + UtilConstants.CLOSE_PARENTHESIS + UtilConstants.SPACE
                + UtilConstants.OPEN_CURLY_BRACKET + UtilConstants.NEW_LINE + UtilConstants.EIGHT_SPACE_INDENTATION
                + UtilConstants.RETURN + " Objects.hash" + UtilConstants.OPEN_PARENTHESIS;
    }

    /**
     * Returns to hash code methods close string.
     *
     * @param hashcodeString hash code string
     * @return to hash code method close string
     */
    public static String getHashCodeMethodClose(String hashcodeString) {
        hashcodeString = YangIoUtils.trimAtLast(hashcodeString, UtilConstants.COMMA);
        hashcodeString = YangIoUtils.trimAtLast(hashcodeString, UtilConstants.SPACE);
        return hashcodeString + UtilConstants.CLOSE_PARENTHESIS + UtilConstants.SEMI_COLAN + UtilConstants.NEW_LINE
                + UtilConstants.FOUR_SPACE_INDENTATION + UtilConstants.CLOSE_CURLY_BRACKET;
    }

    /**
     * Hash code method for class.
     *
     * @param attr attribute info
     * @return hash code method
     */
    public static String getHashCodeMethod(AttributeInfo attr) {
        String attributeName = JavaIdentifierSyntax.getLowerCase(attr.getAttributeName());
        return attributeName
                + UtilConstants.COMMA + UtilConstants.SPACE;

    }

    /**
     * Returns to equals method open strings.
     *
     * @param className class name
     * @return to equals method open string
     */
    public static String getEqualsMethodOpen(String className) {

        return getOverRideString() + UtilConstants.FOUR_SPACE_INDENTATION + UtilConstants.PUBLIC + UtilConstants.SPACE
                + UtilConstants.BOOLEAN + UtilConstants.SPACE + UtilConstants.EQUALS_STRING
                + UtilConstants.OPEN_PARENTHESIS + UtilConstants.OBJECT_STRING + UtilConstants.SPACE + "obj"
                + UtilConstants.CLOSE_PARENTHESIS + UtilConstants.SPACE + UtilConstants.OPEN_CURLY_BRACKET
                + UtilConstants.NEW_LINE + getEqualsMethodsCommonIfCondition()
                + getEqualsMethodsSpecificIfCondition(className);
    }

    /**
     * Returns equal methods if condition string.
     *
     * @return if condition string
     */
    private static String getEqualsMethodsCommonIfCondition() {
        return UtilConstants.EIGHT_SPACE_INDENTATION + UtilConstants.IF + UtilConstants.SPACE
                + UtilConstants.OPEN_PARENTHESIS + UtilConstants.THIS
                + UtilConstants.SPACE + UtilConstants.EQUAL + UtilConstants.EQUAL + UtilConstants.SPACE + "obj"
                + UtilConstants.CLOSE_PARENTHESIS + UtilConstants.SPACE + UtilConstants.OPEN_CURLY_BRACKET
                + UtilConstants.NEW_LINE + UtilConstants.TWELVE_SPACE_INDENTATION + UtilConstants.RETURN
                + UtilConstants.SPACE + UtilConstants.TRUE + UtilConstants.SEMI_COLAN + UtilConstants.NEW_LINE
                + UtilConstants.EIGHT_SPACE_INDENTATION + UtilConstants.CLOSE_CURLY_BRACKET + UtilConstants.NEW_LINE;
    }

    /**
     * Returns if condition for specific class object in equals method.
     *
     * @param className class name
     * @return if condition string
     */
    private static String getEqualsMethodsSpecificIfCondition(String className) {
        return UtilConstants.EIGHT_SPACE_INDENTATION + UtilConstants.IF + UtilConstants.SPACE
                + UtilConstants.OPEN_PARENTHESIS + "obj" + UtilConstants.INSTANCE_OF + className
                + UtilConstants.CLOSE_PARENTHESIS + UtilConstants.SPACE + UtilConstants.OPEN_CURLY_BRACKET
                + UtilConstants.NEW_LINE + UtilConstants.TWELVE_SPACE_INDENTATION + className + UtilConstants.SPACE
                + "other " + UtilConstants.EQUAL + UtilConstants.SPACE + UtilConstants.OPEN_PARENTHESIS + className
                + UtilConstants.CLOSE_PARENTHESIS + UtilConstants.SPACE + "obj" + UtilConstants.SEMI_COLAN
                + UtilConstants.NEW_LINE + UtilConstants.TWELVE_SPACE_INDENTATION + UtilConstants.RETURN
                + UtilConstants.NEW_LINE;
    }

    /**
     * Returns to equals methods close string.
     *
     * @param equalMethodString equal method string
     * @return to equals method close string
     */
    public static String getEqualsMethodClose(String equalMethodString) {
        equalMethodString = YangIoUtils.trimAtLast(equalMethodString, UtilConstants.AND);
        equalMethodString = YangIoUtils.trimAtLast(equalMethodString, UtilConstants.AND);
        equalMethodString = YangIoUtils.trimAtLast(equalMethodString, UtilConstants.SPACE);
        equalMethodString = YangIoUtils.trimAtLast(equalMethodString, UtilConstants.NEW_LINE) + UtilConstants.SEMI_COLAN
                + UtilConstants.NEW_LINE;
        return equalMethodString + UtilConstants.EIGHT_SPACE_INDENTATION
                + UtilConstants.CLOSE_CURLY_BRACKET + UtilConstants.NEW_LINE
                + UtilConstants.EIGHT_SPACE_INDENTATION + UtilConstants.RETURN + UtilConstants.SPACE
                + UtilConstants.FALSE + UtilConstants.SEMI_COLAN
                + UtilConstants.NEW_LINE + UtilConstants.FOUR_SPACE_INDENTATION
                + UtilConstants.CLOSE_CURLY_BRACKET;
    }

    /**
     * Equals method for class.
     *
     * @param attr attribute info
     * @return equals method
     */
    public static String getEqualsMethod(AttributeInfo attr) {
        String attributeName = JavaIdentifierSyntax.getLowerCase(attr.getAttributeName());
        return UtilConstants.SIXTEEN_SPACE_INDENTATION + UtilConstants.SPACE + UtilConstants.OBJECT_STRING + "s"
                + UtilConstants.PERIOD + UtilConstants.EQUALS_STRING + UtilConstants.OPEN_PARENTHESIS + attributeName
                + UtilConstants.COMMA + UtilConstants.SPACE + "other." + attributeName + UtilConstants.CLOSE_PARENTHESIS
                + UtilConstants.SPACE + UtilConstants.AND + UtilConstants.AND;

    }

    /**
     * Returns of method string for class.
     *
     * @param name class name
     * @param attr attribute info
     * @return of method string
     */
    public static String getOfMethod(String name, AttributeInfo attr) {

        String attrQuaifiedType = "";
        if (attr.isQualifiedName() && (attr.getImportInfo().getPkgInfo() != null)) {
            attrQuaifiedType = attr.getImportInfo().getPkgInfo() + ".";
        }
        attrQuaifiedType = attrQuaifiedType + attr.getImportInfo().getClassInfo();

        return UtilConstants.FOUR_SPACE_INDENTATION + UtilConstants.PUBLIC + UtilConstants.SPACE + UtilConstants.STATIC
                + UtilConstants.SPACE + name + UtilConstants.SPACE + UtilConstants.OF + UtilConstants.OPEN_PARENTHESIS
                + attrQuaifiedType + UtilConstants.SPACE + UtilConstants.VALUE + UtilConstants.CLOSE_PARENTHESIS
                + UtilConstants.SPACE + UtilConstants.OPEN_CURLY_BRACKET + UtilConstants.NEW_LINE
                + UtilConstants.EIGHT_SPACE_INDENTATION + UtilConstants.RETURN + UtilConstants.SPACE + UtilConstants.NEW
                + UtilConstants.SPACE + name + UtilConstants.OPEN_PARENTHESIS + UtilConstants.VALUE
                + UtilConstants.CLOSE_PARENTHESIS + UtilConstants.SEMI_COLAN + UtilConstants.NEW_LINE
                + UtilConstants.FOUR_SPACE_INDENTATION + UtilConstants.CLOSE_CURLY_BRACKET + UtilConstants.NEW_LINE;
    }

}

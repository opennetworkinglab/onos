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

import org.onosproject.yangutils.translator.tojava.JavaAttributeInfo;

import static org.onosproject.yangutils.translator.tojava.utils.JavaIdentifierSyntax.getCamelCase;
import static org.onosproject.yangutils.translator.tojava.utils.JavaIdentifierSyntax.getCaptialCase;
import static org.onosproject.yangutils.translator.tojava.utils.JavaIdentifierSyntax.getSmallCase;
import static org.onosproject.yangutils.utils.UtilConstants.ADD_STRING;
import static org.onosproject.yangutils.utils.UtilConstants.AND;
import static org.onosproject.yangutils.utils.UtilConstants.BOOLEAN_DATA_TYPE;
import static org.onosproject.yangutils.utils.UtilConstants.BUILD;
import static org.onosproject.yangutils.utils.UtilConstants.BUILDER;
import static org.onosproject.yangutils.utils.UtilConstants.CHECK_NOT_NULL_STRING;
import static org.onosproject.yangutils.utils.UtilConstants.CLOSE_CURLY_BRACKET;
import static org.onosproject.yangutils.utils.UtilConstants.CLOSE_PARENTHESIS;
import static org.onosproject.yangutils.utils.UtilConstants.COMMA;
import static org.onosproject.yangutils.utils.UtilConstants.DIAMOND_CLOSE_BRACKET;
import static org.onosproject.yangutils.utils.UtilConstants.DIAMOND_OPEN_BRACKET;
import static org.onosproject.yangutils.utils.UtilConstants.EIGHT_SPACE_INDENTATION;
import static org.onosproject.yangutils.utils.UtilConstants.EMPTY_STRING;
import static org.onosproject.yangutils.utils.UtilConstants.EQUAL;
import static org.onosproject.yangutils.utils.UtilConstants.EQUALS_STRING;
import static org.onosproject.yangutils.utils.UtilConstants.FALSE;
import static org.onosproject.yangutils.utils.UtilConstants.FOUR_SPACE_INDENTATION;
import static org.onosproject.yangutils.utils.UtilConstants.GET_METHOD_PREFIX;
import static org.onosproject.yangutils.utils.UtilConstants.GOOGLE_MORE_OBJECT_METHOD_STRING;
import static org.onosproject.yangutils.utils.UtilConstants.HASH;
import static org.onosproject.yangutils.utils.UtilConstants.HASH_CODE_STRING;
import static org.onosproject.yangutils.utils.UtilConstants.IF;
import static org.onosproject.yangutils.utils.UtilConstants.IMPL;
import static org.onosproject.yangutils.utils.UtilConstants.INSTANCE_OF;
import static org.onosproject.yangutils.utils.UtilConstants.INT;
import static org.onosproject.yangutils.utils.UtilConstants.LIST;
import static org.onosproject.yangutils.utils.UtilConstants.NEW;
import static org.onosproject.yangutils.utils.UtilConstants.NEW_LINE;
import static org.onosproject.yangutils.utils.UtilConstants.OBJ;
import static org.onosproject.yangutils.utils.UtilConstants.OBJECT;
import static org.onosproject.yangutils.utils.UtilConstants.OBJECT_STRING;
import static org.onosproject.yangutils.utils.UtilConstants.OF;
import static org.onosproject.yangutils.utils.UtilConstants.OPEN_CURLY_BRACKET;
import static org.onosproject.yangutils.utils.UtilConstants.OPEN_PARENTHESIS;
import static org.onosproject.yangutils.utils.UtilConstants.OTHER;
import static org.onosproject.yangutils.utils.UtilConstants.OVERRIDE;
import static org.onosproject.yangutils.utils.UtilConstants.PERIOD;
import static org.onosproject.yangutils.utils.UtilConstants.PUBLIC;
import static org.onosproject.yangutils.utils.UtilConstants.QUOTES;
import static org.onosproject.yangutils.utils.UtilConstants.RETURN;
import static org.onosproject.yangutils.utils.UtilConstants.SEMI_COLAN;
import static org.onosproject.yangutils.utils.UtilConstants.SET_METHOD_PREFIX;
import static org.onosproject.yangutils.utils.UtilConstants.SIXTEEN_SPACE_INDENTATION;
import static org.onosproject.yangutils.utils.UtilConstants.SPACE;
import static org.onosproject.yangutils.utils.UtilConstants.STATIC;
import static org.onosproject.yangutils.utils.UtilConstants.STRING_DATA_TYPE;
import static org.onosproject.yangutils.utils.UtilConstants.SUFFIX_S;
import static org.onosproject.yangutils.utils.UtilConstants.THIS;
import static org.onosproject.yangutils.utils.UtilConstants.TO;
import static org.onosproject.yangutils.utils.UtilConstants.TRUE;
import static org.onosproject.yangutils.utils.UtilConstants.TWELVE_SPACE_INDENTATION;
import static org.onosproject.yangutils.utils.UtilConstants.VALUE;
import static org.onosproject.yangutils.utils.UtilConstants.VOID;
import static org.onosproject.yangutils.utils.io.impl.JavaDocGen.getJavaDoc;
import static org.onosproject.yangutils.utils.io.impl.JavaDocGen.JavaDocType.BUILD_METHOD;
import static org.onosproject.yangutils.utils.io.impl.JavaDocGen.JavaDocType.CONSTRUCTOR;
import static org.onosproject.yangutils.utils.io.impl.JavaDocGen.JavaDocType.DEFAULT_CONSTRUCTOR;
import static org.onosproject.yangutils.utils.io.impl.JavaDocGen.JavaDocType.GETTER_METHOD;
import static org.onosproject.yangutils.utils.io.impl.JavaDocGen.JavaDocType.SETTER_METHOD;
import static org.onosproject.yangutils.utils.io.impl.YangIoUtils.trimAtLast;

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
     * @param name attribute name
     * @return method string for builder interface
     */
    public static String parseBuilderInterfaceBuildMethodString(String name) {

        return getJavaDoc(BUILD_METHOD, name, false) + getBuildForInterface(name);
    }

    /**
     * Returns getter string.
     *
     * @param attr attribute info
     * @return getter string
     */
    public static String getGetterString(JavaAttributeInfo attr) {

        String returnType = getReturnType(attr);
        String attributeName = getSmallCase(attr.getAttributeName());

        return getJavaDoc(GETTER_METHOD, attributeName, attr.isListAttr())
                + getGetterForInterface(attributeName, returnType, attr.isListAttr());
    }

    /**
     * Returns setter string.
     *
     * @param attr attribute info
     * @param className java class name
     * @return setter string
     */
    public static String getSetterString(JavaAttributeInfo attr, String className) {

        String attrType = getReturnType(attr);
        String attributeName = getSmallCase(attr.getAttributeName());

        return getJavaDoc(SETTER_METHOD, attributeName, attr.isListAttr())
                + getSetterForInterface(attributeName, attrType, className, attr.isListAttr());
    }

    /**
     * Returns constructor method string.
     *
     * @param name class name
     * @return constructor string
     */
    public static String getConstructorString(String name) {

        return getJavaDoc(CONSTRUCTOR, name, false);
    }

    /**
     * Returns default constructor method string.
     *
     * @param name class name
     * @param modifierType modifier type
     * @return default constructor string
     */
    public static String getDefaultConstructorString(String name, String modifierType) {

        return getJavaDoc(DEFAULT_CONSTRUCTOR, name, false) + getDefaultConstructor(name, modifierType);
    }

    /**
     * Returns default constructor method string.
     *
     * @param attr attribute info
     * @param className class name
     * @return default constructor string
     */
    public static String getTypeDefConstructor(JavaAttributeInfo attr, String className) {

        String attrQuaifiedType = getReturnType(attr);
        String attributeName = getSmallCase(attr.getAttributeName());

        if (!attr.isListAttr()) {
            return getTypeDefConstructorString(attrQuaifiedType, attributeName, className);
        }
        String listAttr = getListString() + attrQuaifiedType + DIAMOND_CLOSE_BRACKET;
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

        return FOUR_SPACE_INDENTATION + PUBLIC + SPACE + className + OPEN_PARENTHESIS + type + SPACE + VALUE
                + CLOSE_PARENTHESIS + SPACE + OPEN_CURLY_BRACKET + NEW_LINE + EIGHT_SPACE_INDENTATION + THIS + PERIOD
                + name + SPACE + EQUAL + SPACE + VALUE + SEMI_COLAN + NEW_LINE + FOUR_SPACE_INDENTATION
                + CLOSE_CURLY_BRACKET;
    }

    /**
     * Returns check not null string.
     *
     * @param name attribute name
     * @return check not null string
     */
    public static String getCheckNotNull(String name) {

        return EIGHT_SPACE_INDENTATION + CHECK_NOT_NULL_STRING + OPEN_PARENTHESIS + name + COMMA + SPACE + name
                + CLOSE_PARENTHESIS + SEMI_COLAN + NEW_LINE;
    }

    /**
     * Returns build method string.
     *
     * @param name class name
     * @return build string
     */
    public static String getBuildString(String name) {

        return FOUR_SPACE_INDENTATION + OVERRIDE + NEW_LINE + getBuild(name);
    }

    /**
     * Returns the getter method strings for class file.
     *
     * @param attr attribute info
     * @return getter method for class
     */
    public static String getGetterForClass(JavaAttributeInfo attr) {

        String attrQuaifiedType = getReturnType(attr);
        String attributeName = getSmallCase(attr.getAttributeName());

        if (!attr.isListAttr()) {
            return getGetter(attrQuaifiedType, attributeName);
        }
        String listAttr = getListString() + attrQuaifiedType + DIAMOND_CLOSE_BRACKET;
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

        return FOUR_SPACE_INDENTATION + PUBLIC + SPACE + type + SPACE + GET_METHOD_PREFIX + getCaptialCase(name)
                + OPEN_PARENTHESIS + CLOSE_PARENTHESIS + SPACE + OPEN_CURLY_BRACKET + NEW_LINE + EIGHT_SPACE_INDENTATION
                + RETURN + SPACE + name + SEMI_COLAN + NEW_LINE + FOUR_SPACE_INDENTATION + CLOSE_CURLY_BRACKET;
    }

    /**
     * Returns the setter method strings for class file.
     *
     * @param attr attribute info
     * @param className name of the class
     * @return setter method for class
     */
    public static String getSetterForClass(JavaAttributeInfo attr, String className) {

        String attrQuaifiedType = getReturnType(attr);
        String attributeName = getSmallCase(attr.getAttributeName());
        if (!attr.isListAttr()) {
            return getSetter(className, attributeName, attrQuaifiedType);
        }
        String listAttr = getListString() + attrQuaifiedType + DIAMOND_CLOSE_BRACKET;
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

        return FOUR_SPACE_INDENTATION + PUBLIC + SPACE + className + BUILDER + SPACE + SET_METHOD_PREFIX
                + getCaptialCase(name) + OPEN_PARENTHESIS + type + SPACE + name + CLOSE_PARENTHESIS + SPACE
                + OPEN_CURLY_BRACKET + NEW_LINE + EIGHT_SPACE_INDENTATION + THIS + PERIOD + name + SPACE + EQUAL + SPACE
                + name + SEMI_COLAN + NEW_LINE + EIGHT_SPACE_INDENTATION + RETURN + SPACE + THIS + SEMI_COLAN
                + NEW_LINE + FOUR_SPACE_INDENTATION + CLOSE_CURLY_BRACKET;
    }

    /**
     * Returns the setter method strings for class file.
     *
     * @param attr attribute info
     * @return setter method for class
     */
    public static String getSetterForTypeDefClass(JavaAttributeInfo attr) {

        String attrQuaifiedType = getReturnType(attr);
        String attributeName = getSmallCase(attr.getAttributeName());
        return getTypeDefSetter(attrQuaifiedType, attributeName);
    }

    /**
     * Returns type def's setter for attribute.
     *
     * @param type data type
     * @param name attribute name
     * @return setter for type def's attribute
     */
    private static String getTypeDefSetter(String type, String name) {

        return FOUR_SPACE_INDENTATION + PUBLIC + SPACE + VOID + SPACE + SET_METHOD_PREFIX + getCaptialCase(name)
                + OPEN_PARENTHESIS + type + SPACE + VALUE + CLOSE_PARENTHESIS + SPACE + OPEN_CURLY_BRACKET + NEW_LINE
                + EIGHT_SPACE_INDENTATION + THIS + PERIOD + name + SPACE + EQUAL + SPACE + VALUE + SEMI_COLAN + NEW_LINE
                + FOUR_SPACE_INDENTATION + CLOSE_CURLY_BRACKET;
    }

    /**
     * Returns override string.
     *
     * @return override string
     */
    public static String getOverRideString() {

        return NEW_LINE + FOUR_SPACE_INDENTATION + OVERRIDE + NEW_LINE;
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
        String listAttr = getListString() + returnType + DIAMOND_CLOSE_BRACKET;
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

        return FOUR_SPACE_INDENTATION + returnType + SPACE + GET_METHOD_PREFIX + getCaptialCase(yangName)
                + OPEN_PARENTHESIS + CLOSE_PARENTHESIS + SEMI_COLAN;
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
        String listAttr = getListString() + attrType + DIAMOND_CLOSE_BRACKET;
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

        return FOUR_SPACE_INDENTATION + className + BUILDER + SPACE + SET_METHOD_PREFIX + getCaptialCase(attrName)
                + OPEN_PARENTHESIS + attrType + SPACE + attrName + CLOSE_PARENTHESIS + SEMI_COLAN;
    }

    /**
     * Returns list string.
     *
     * @return list string
     */
    private static String getListString() {

        return LIST + DIAMOND_OPEN_BRACKET;
    }

    /**
     * Returns return type for attribute.
     *
     * @param attr attribute info
     * @return return type
     */
    private static String getReturnType(JavaAttributeInfo attr) {

        String returnType = EMPTY_STRING;
        if (attr.isQualifiedName() && (attr.getImportInfo().getPkgInfo() != null)) {
            returnType = attr.getImportInfo().getPkgInfo() + PERIOD;
        }
        returnType = returnType + attr.getImportInfo().getClassInfo();
        return returnType;
    }

    /**
     * Returns the build method strings for interface file.
     *
     * @param yangName name of the interface
     * @return build method for interface
     */
    public static String getBuildForInterface(String yangName) {

        return FOUR_SPACE_INDENTATION + yangName + SPACE + BUILD + OPEN_PARENTHESIS + CLOSE_PARENTHESIS + SEMI_COLAN
                + NEW_LINE;
    }

    /**
     * Returns constructor string for impl class.
     *
     * @param yangName class name
     * @return constructor string
     */
    public static String getConstructorStart(String yangName) {

        String javadoc = getConstructorString(yangName);
        String constructor = FOUR_SPACE_INDENTATION + PUBLIC + SPACE + yangName + IMPL + OPEN_PARENTHESIS + yangName
                + BUILDER + SPACE + BUILDER.toLowerCase() + OBJECT + CLOSE_PARENTHESIS + SPACE + OPEN_CURLY_BRACKET
                + NEW_LINE;
        return javadoc + constructor;
    }

    /**
     * Returns the constructor strings for class file.
     *
     * @param yangName name of the class
     * @param attr attribute info
     * @return constructor for class
     */
    public static String getConstructor(String yangName, JavaAttributeInfo attr) {

        String attributeName = getSmallCase(attr.getAttributeName());

        String constructor = EIGHT_SPACE_INDENTATION + THIS + PERIOD + getCamelCase(attributeName) + SPACE + EQUAL
                + SPACE + BUILDER.toLowerCase() + OBJECT + PERIOD + GET_METHOD_PREFIX
                + getCaptialCase(getCamelCase(attributeName)) + OPEN_PARENTHESIS + CLOSE_PARENTHESIS + SEMI_COLAN
                + NEW_LINE;

        return constructor;
    }

    /**
     * Returns the build method strings for class file.
     *
     * @param yangName class name
     * @return build method string for class
     */
    public static String getBuild(String yangName) {

        return FOUR_SPACE_INDENTATION + PUBLIC + SPACE + yangName + SPACE + BUILD + OPEN_PARENTHESIS + CLOSE_PARENTHESIS
                + SPACE + OPEN_CURLY_BRACKET + NEW_LINE + EIGHT_SPACE_INDENTATION + RETURN + SPACE + NEW + SPACE
                + yangName + IMPL + OPEN_PARENTHESIS + THIS + CLOSE_PARENTHESIS + SEMI_COLAN + NEW_LINE
                + FOUR_SPACE_INDENTATION + CLOSE_CURLY_BRACKET;
    }

    /**
     * Returns the Default constructor strings for class file.
     *
     * @param name name of the class
     * @param modifierType modifier type for default constructor
     * @return Default constructor for class
     */
    private static String getDefaultConstructor(String name, String modifierType) {

        return FOUR_SPACE_INDENTATION + modifierType + SPACE + name + OPEN_PARENTHESIS + CLOSE_PARENTHESIS + SPACE
                + OPEN_CURLY_BRACKET + NEW_LINE + FOUR_SPACE_INDENTATION + CLOSE_CURLY_BRACKET;
    }

    /**
     * Returns to string method open strings.
     *
     * @return to string method open string
     */
    public static String getToStringMethodOpen() {

        return getOverRideString() + FOUR_SPACE_INDENTATION + PUBLIC + SPACE + STRING_DATA_TYPE + SPACE + TO
                + STRING_DATA_TYPE + OPEN_PARENTHESIS + CLOSE_PARENTHESIS + SPACE + OPEN_CURLY_BRACKET + NEW_LINE
                + EIGHT_SPACE_INDENTATION + RETURN + GOOGLE_MORE_OBJECT_METHOD_STRING + NEW_LINE;
    }

    /**
     * Returns to string methods close string.
     *
     * @return to string method close string
     */
    public static String getToStringMethodClose() {

        return TWELVE_SPACE_INDENTATION + PERIOD + TO + STRING_DATA_TYPE + OPEN_PARENTHESIS + CLOSE_PARENTHESIS
                + SEMI_COLAN + NEW_LINE + FOUR_SPACE_INDENTATION + CLOSE_CURLY_BRACKET + NEW_LINE;
    }

    /**
     * To string method for class.
     *
     * @param attr attribute info
     * @return to string method
     */
    public static String getToStringMethod(JavaAttributeInfo attr) {

        String attributeName = getSmallCase(attr.getAttributeName());

        return TWELVE_SPACE_INDENTATION + PERIOD + ADD_STRING + OPEN_PARENTHESIS + QUOTES + attributeName + QUOTES
                + COMMA + SPACE + attributeName + CLOSE_PARENTHESIS;

    }

    /**
     * Returns to hash code method open strings.
     *
     * @return to hash code method open string
     */
    public static String getHashCodeMethodOpen() {

        return getOverRideString() + FOUR_SPACE_INDENTATION + PUBLIC + SPACE + INT + SPACE + HASH_CODE_STRING
                + OPEN_PARENTHESIS + CLOSE_PARENTHESIS + SPACE + OPEN_CURLY_BRACKET + NEW_LINE + EIGHT_SPACE_INDENTATION
                + RETURN + SPACE + OBJECT_STRING + SUFFIX_S + PERIOD + HASH + OPEN_PARENTHESIS;
    }

    /**
     * Returns to hash code methods close string.
     *
     * @param hashcodeString hash code string
     * @return to hash code method close string
     */
    public static String getHashCodeMethodClose(String hashcodeString) {

        hashcodeString = trimAtLast(hashcodeString, COMMA);
        hashcodeString = trimAtLast(hashcodeString, SPACE);
        return hashcodeString + CLOSE_PARENTHESIS + SEMI_COLAN + NEW_LINE + FOUR_SPACE_INDENTATION + CLOSE_CURLY_BRACKET
                + NEW_LINE;
    }

    /**
     * Hash code method for class.
     *
     * @param attr attribute info
     * @return hash code method
     */
    public static String getHashCodeMethod(JavaAttributeInfo attr) {

        return getSmallCase(attr.getAttributeName()) + COMMA + SPACE;
    }

    /**
     * Returns to equals method open strings.
     *
     * @param className class name
     * @return to equals method open string
     */
    public static String getEqualsMethodOpen(String className) {

        return getOverRideString() + FOUR_SPACE_INDENTATION + PUBLIC + SPACE + BOOLEAN_DATA_TYPE + SPACE + EQUALS_STRING
                + OPEN_PARENTHESIS + OBJECT_STRING + SPACE + OBJ + CLOSE_PARENTHESIS + SPACE + OPEN_CURLY_BRACKET
                + NEW_LINE + getEqualsMethodsCommonIfCondition() + getEqualsMethodsSpecificIfCondition(className);
    }

    /**
     * Returns equal methods if condition string.
     *
     * @return if condition string
     */
    private static String getEqualsMethodsCommonIfCondition() {

        return EIGHT_SPACE_INDENTATION + IF + SPACE + OPEN_PARENTHESIS + THIS + SPACE + EQUAL + EQUAL + SPACE + OBJ
                + CLOSE_PARENTHESIS + SPACE + OPEN_CURLY_BRACKET + NEW_LINE + TWELVE_SPACE_INDENTATION + RETURN + SPACE
                + TRUE + SEMI_COLAN + NEW_LINE + EIGHT_SPACE_INDENTATION + CLOSE_CURLY_BRACKET + NEW_LINE;
    }

    /**
     * Returns if condition for specific class object in equals method.
     *
     * @param className class name
     * @return if condition string
     */
    private static String getEqualsMethodsSpecificIfCondition(String className) {

        return EIGHT_SPACE_INDENTATION + IF + SPACE + OPEN_PARENTHESIS + OBJ + INSTANCE_OF + className
                + CLOSE_PARENTHESIS + SPACE + OPEN_CURLY_BRACKET + NEW_LINE + TWELVE_SPACE_INDENTATION + className
                + SPACE + OTHER + SPACE + EQUAL + SPACE + OPEN_PARENTHESIS + className + CLOSE_PARENTHESIS + SPACE
                + OBJ + SEMI_COLAN + NEW_LINE + TWELVE_SPACE_INDENTATION + RETURN + NEW_LINE;
    }

    /**
     * Returns to equals methods close string.
     *
     * @param equalMethodString equal method string
     * @return to equals method close string
     */
    public static String getEqualsMethodClose(String equalMethodString) {

        equalMethodString = trimAtLast(equalMethodString, AND);
        equalMethodString = trimAtLast(equalMethodString, AND);
        equalMethodString = trimAtLast(equalMethodString, SPACE);
        equalMethodString = trimAtLast(equalMethodString, NEW_LINE) + SEMI_COLAN + NEW_LINE;
        return equalMethodString + EIGHT_SPACE_INDENTATION + CLOSE_CURLY_BRACKET + NEW_LINE + EIGHT_SPACE_INDENTATION
                + RETURN + SPACE + FALSE + SEMI_COLAN + NEW_LINE + FOUR_SPACE_INDENTATION + CLOSE_CURLY_BRACKET
                + NEW_LINE;
    }

    /**
     * Equals method for class.
     *
     * @param attr attribute info
     * @return equals method
     */
    public static String getEqualsMethod(JavaAttributeInfo attr) {

        String attributeName = getSmallCase(attr.getAttributeName());

        return SIXTEEN_SPACE_INDENTATION + SPACE + OBJECT_STRING + SUFFIX_S + PERIOD + EQUALS_STRING + OPEN_PARENTHESIS
                + attributeName + COMMA + SPACE + OTHER + PERIOD + attributeName + CLOSE_PARENTHESIS + SPACE + AND
                + AND;

    }

    /**
     * Returns of method string for class.
     *
     * @param name class name
     * @param attr attribute info
     * @return of method string
     */
    public static String getOfMethod(String name, JavaAttributeInfo attr) {

        String attrQuaifiedType = getReturnType(attr);

        return FOUR_SPACE_INDENTATION + PUBLIC + SPACE + STATIC + SPACE + name + SPACE + OF + OPEN_PARENTHESIS
                + attrQuaifiedType + SPACE + VALUE + CLOSE_PARENTHESIS + SPACE + OPEN_CURLY_BRACKET + NEW_LINE
                + EIGHT_SPACE_INDENTATION + RETURN + SPACE + NEW + SPACE + name + OPEN_PARENTHESIS + VALUE
                + CLOSE_PARENTHESIS + SEMI_COLAN + NEW_LINE + FOUR_SPACE_INDENTATION + CLOSE_CURLY_BRACKET + NEW_LINE;
    }

}

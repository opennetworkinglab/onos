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
     * Returns the methods strings for builder class.
     *
     * @param attr attribute info
     * @param className java class name
     * @return method string for builder class
     */
    static String parseBuilderMethodString(AttributeInfo attr, String className) {
        String attrQuaifiedType = "";
        if (attr.getImportInfo().getPkgInfo() != null) {
            attrQuaifiedType = attr.getImportInfo().getPkgInfo() + ".";
        }
        attrQuaifiedType = attrQuaifiedType + attr.getImportInfo().getClassInfo();

        String overrideString = UtilConstants.NEW_LINE + UtilConstants.FOUR_SPACE_INDENTATION
                + UtilConstants.OVERRIDE + UtilConstants.NEW_LINE;
        String getterString = getGetterForClass(attr.getAttributeName(), attrQuaifiedType);
        String setterString = getSetterForClass(attr.getAttributeName(), attrQuaifiedType, className);
        return overrideString + getterString + UtilConstants.NEW_LINE + overrideString + setterString;
    }

    /**
     * Returns the methods strings for builder class.
     *
     * @param attr attribute info
     * @return method string for builder class
     */
    static String parseImplMethodString(AttributeInfo attr) {

        String attrQuaifiedType = "";
        if (attr.getImportInfo().getPkgInfo() != null) {
            attrQuaifiedType = attr.getImportInfo().getPkgInfo() + ".";
        }
        attrQuaifiedType = attrQuaifiedType + attr.getImportInfo().getClassInfo();

        return UtilConstants.NEW_LINE + UtilConstants.FOUR_SPACE_INDENTATION + UtilConstants.OVERRIDE
                + UtilConstants.NEW_LINE + getGetterForClass(attr.getAttributeName(), attrQuaifiedType);
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

        return JavaDocGen.getJavaDoc(JavaDocGen.JavaDocType.BUILD, name)
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
        if (attr.getImportInfo().getPkgInfo() != null) {
            returnType = attr.getImportInfo().getPkgInfo() + ".";
        }

        returnType = returnType + attr.getImportInfo().getClassInfo();

        return JavaDocGen.getJavaDoc(JavaDocGen.JavaDocType.GETTER, attr.getAttributeName())
                + getGetterForInterface(attr.getAttributeName(), returnType)
                + UtilConstants.NEW_LINE;
    }

    /**
     * Returns setter string.
     *
     * @param attr attribute info
     * @param className java class name
     * @return setter string
     */
    private static String getSetterString(AttributeInfo attr, String className) {

        String attrType = "";
        if (attr.getImportInfo().getPkgInfo() != null) {
            attrType = attr.getImportInfo().getPkgInfo() + ".";
        }

        attrType = attrType + attr.getImportInfo().getClassInfo();

        return JavaDocGen.getJavaDoc(JavaDocGen.JavaDocType.SETTER, attr.getAttributeName())
                + getSetterForInterface(attr.getAttributeName(), attrType, className);
    }

    /**
     * Returns constructor method string.
     *
     * @param name class name
     * @return constructor string
     */
    public static String getConstructorString(String name) {

        return JavaDocGen.getJavaDoc(JavaDocGen.JavaDocType.CONSTRUCTOR, name)
                + getConstructor(name);
    }

    /**
     * Returns default constructor method string.
     *
     * @param type generated file type
     * @param name class name
     * @return default constructor string
     */
    public static String getDefaultConstructorString(int type, String name) {

        return JavaDocGen.getJavaDoc(JavaDocGen.JavaDocType.DEFAULT_CONSTRUCTOR, name)
                + getDefaultConstructor(name + UtilConstants.BUILDER);
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
     * @param attrName name of the attribute
     * @param attrType return type of attribute
     * @return getter method for class
     */
    private static String getGetterForClass(String attrName, String attrType) {

        return UtilConstants.FOUR_SPACE_INDENTATION + UtilConstants.PUBLIC + UtilConstants.SPACE
                + attrType + UtilConstants.SPACE + UtilConstants.GET_METHOD_PREFIX
                + JavaIdentifierSyntax.getCaptialCase(attrName) + UtilConstants.OPEN_PARENTHESIS
                + UtilConstants.CLOSE_PARENTHESIS + UtilConstants.SPACE + UtilConstants.OPEN_CURLY_BRACKET
                + UtilConstants.NEW_LINE + UtilConstants.EIGHT_SPACE_INDENTATION + UtilConstants.RETURN
                + UtilConstants.SPACE + attrName + UtilConstants.SEMI_COLAN + UtilConstants.NEW_LINE
                + UtilConstants.FOUR_SPACE_INDENTATION + UtilConstants.CLOSE_CURLY_BRACKET;
    }

    /**
     * Returns the setter method strings for class file.
     *
     * @param attrName name of the attribute
     * @param attrType return type of attribute
     * @param className name of the class
     * @return setter method for class
     */
    private static String getSetterForClass(String attrName, String attrType, String className) {

        return UtilConstants.FOUR_SPACE_INDENTATION + UtilConstants.PUBLIC + UtilConstants.SPACE
                + className + UtilConstants.BUILDER + UtilConstants.SPACE + UtilConstants.SET_METHOD_PREFIX
                + JavaIdentifierSyntax.getCaptialCase(attrName) + UtilConstants.OPEN_PARENTHESIS
                + attrType + UtilConstants.SPACE + attrName + UtilConstants.CLOSE_PARENTHESIS
                + UtilConstants.SPACE + UtilConstants.OPEN_CURLY_BRACKET + UtilConstants.NEW_LINE
                + UtilConstants.EIGHT_SPACE_INDENTATION + UtilConstants.THIS + UtilConstants.PERIOD
                + attrName + UtilConstants.SPACE + UtilConstants.EQUAL + UtilConstants.SPACE + attrName
                + UtilConstants.SEMI_COLAN + UtilConstants.NEW_LINE + UtilConstants.EIGHT_SPACE_INDENTATION
                + UtilConstants.RETURN + UtilConstants.SPACE + UtilConstants.THIS + UtilConstants.SEMI_COLAN
                + UtilConstants.NEW_LINE + UtilConstants.FOUR_SPACE_INDENTATION + UtilConstants.CLOSE_CURLY_BRACKET;
    }

    /**
     * Returns the getter method strings for interface file.
     *
     * @param yangName name of the attribute
     * @param returnType return type of attribute
     * @return getter method for interface
     */
    private static String getGetterForInterface(String yangName, String returnType) {
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
     * @return setter method for interface
     */
    private static String getSetterForInterface(String attrName, String attrType, String className) {
        return UtilConstants.FOUR_SPACE_INDENTATION + className + UtilConstants.BUILDER
                + UtilConstants.SPACE + UtilConstants.SET_METHOD_PREFIX
                + JavaIdentifierSyntax.getCaptialCase(attrName) + UtilConstants.OPEN_PARENTHESIS
                + attrType + UtilConstants.SPACE + attrName + UtilConstants.CLOSE_PARENTHESIS
                + UtilConstants.SEMI_COLAN;
    }

    /**
     * Returns the build method strings for interface file.
     *
     * @param yangName name of the interface
     * @return build method for interface
     */
    private static String getBuildForInterface(String yangName) {

        return UtilConstants.FOUR_SPACE_INDENTATION + yangName + UtilConstants.SPACE + UtilConstants.BUILD
                + UtilConstants.OPEN_PARENTHESIS + UtilConstants.CLOSE_PARENTHESIS + UtilConstants.SEMI_COLAN;
    }

    /**
     * Returns the constructor strings for class file.
     *
     * @param yangName name of the class
     * @return constructor for class
     */
    private static String getConstructor(String yangName) {

        String builderAttribute = yangName.substring(0, 1).toLowerCase() + yangName.substring(1);
        String constructor = UtilConstants.FOUR_SPACE_INDENTATION + UtilConstants.PUBLIC + UtilConstants.SPACE
                + yangName + UtilConstants.IMPL + UtilConstants.OPEN_PARENTHESIS + yangName + UtilConstants.BUILDER
                + UtilConstants.SPACE + builderAttribute + UtilConstants.OBJECT + UtilConstants.CLOSE_PARENTHESIS
                + UtilConstants.SPACE + UtilConstants.OPEN_CURLY_BRACKET + UtilConstants.NEW_LINE;

        //        TODO: need to get the partial constructor from constructor temp file.
        //        if (getAttrInfo() != null) {
        //            for (AttributeInfo attribute : getAttrInfo()) {
        //                attribute.setAttributeName(JavaIdentifierSyntax.getCamelCase(attribute.getAttributeName()));
        //                constructor = constructor + UtilConstants.TWELVE_SPACE_INDENTATION + UtilConstants.THIS
        //                        + UtilConstants.PERIOD + attribute.getAttributeName() + UtilConstants.SPACE
        //                        + UtilConstants.EQUAL + UtilConstants.SPACE + builderAttribute + UtilConstants.OBJECT
        //                        + UtilConstants.PERIOD + UtilConstants.GET_METHOD_PREFIX
        //                        + JavaIdentifierSyntax.getCaptialCase(attribute.getAttributeName())
        //                        + UtilConstants.OPEN_PARENTHESIS
        //        + UtilConstants.CLOSE_PARENTHESIS + UtilConstants.SEMI_COLAN
        //                        + UtilConstants.NEW_LINE;
        //            }
        //            getAttrInfo().clear();
        //        }

        return constructor + UtilConstants.FOUR_SPACE_INDENTATION + UtilConstants.CLOSE_CURLY_BRACKET;
    }

    /**
     * Returns the build method strings for class file.
     *
     * @param yangName class name
     * @return build method string for class
     */
    private static String getBuild(String yangName) {

        return UtilConstants.FOUR_SPACE_INDENTATION + UtilConstants.PUBLIC + UtilConstants.SPACE + yangName
                + UtilConstants.SPACE + UtilConstants.BUILD + UtilConstants.OPEN_PARENTHESIS
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
     * @return Default constructor for class
     */
    private static String getDefaultConstructor(String name) {

        return UtilConstants.FOUR_SPACE_INDENTATION + UtilConstants.PUBLIC + UtilConstants.SPACE + name
                + UtilConstants.OPEN_PARENTHESIS + UtilConstants.CLOSE_PARENTHESIS + UtilConstants.SPACE
                + UtilConstants.OPEN_CURLY_BRACKET + UtilConstants.NEW_LINE + UtilConstants.FOUR_SPACE_INDENTATION
                + UtilConstants.CLOSE_CURLY_BRACKET + UtilConstants.NEW_LINE;
    }

}

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

import java.util.List;

import org.onosproject.yangutils.datamodel.YangType;
import org.onosproject.yangutils.translator.GeneratedFileType;
import org.onosproject.yangutils.translator.tojava.AttributeInfo;
import org.onosproject.yangutils.translator.tojava.GeneratedMethodTypes;
import org.onosproject.yangutils.utils.UtilConstants;
import org.onosproject.yangutils.utils.io.impl.JavaDocGen;

/**
 * Generated methods for generated files based on the file type.
 */
public final class MethodsGenerator {

    private static String builderClassName;
    private static List<AttributeInfo> attrInfo;

    /**
     * Sets the builder class name for setter methods of builder class.
     *
     * @param name builder class name
     */
    public static void setBuilderClassName(String name) {
        builderClassName = name;
    }

    /**
     * Sets the attribute info for the impl class's constructor method.
     *
     * @param attr list of attribute info
     */
    public static void setAttrInfo(List<AttributeInfo> attr) {
        attrInfo = attr;
    }

    /**
     * Returns attribute info for the impl class's constructor method.
     *
     * @return list of attribute info
     */
    public static List<AttributeInfo> getAttrInfo() {
        return attrInfo;
    }

    /**
     * Return the class name.
     *
     * @return class name
     */
    public static String getBuilderClassName() {
        return builderClassName;
    }

    /**
     * Default constructor.
     */
    private MethodsGenerator() {
    }

    /**
     * Return method strings.
     *
     * @param attr attribute info.
     * @param type generated file type
     * @return method string
     */
    public static String getMethodString(AttributeInfo attr, GeneratedFileType type) {

        if (type.equals(GeneratedFileType.BUILDER_CLASS)) {

            return parseBuilderMethodString(attr);
        } else if (type.equals(GeneratedFileType.INTERFACE)) {

            return getGetterString(attr);
        } else if (type.equals(GeneratedFileType.BUILDER_INTERFACE)) {

            return parseBuilderInterfaceMethodString(attr);
        } else if (type.equals(GeneratedFileType.IMPL)) {

            return parseImplMethodString(attr);
        }
        return null;
    }

    /**
     * Returns constructed method impl for specific generated file type.
     *
     * @param genFileTypes generated file type
     * @param yangName class name
     * @param methodTypes method types
     * @param returnType return type of method
     * @return constructed method impl
     */
    public static String constructMethodInfo(GeneratedFileType genFileTypes, String yangName,
            GeneratedMethodTypes methodTypes, YangType<?> returnType) {

        if (returnType == null) {
            YangType<?> type = new YangType();
            type.setDataTypeName(yangName);
            returnType = type;
        }

        if (genFileTypes.equals(GeneratedFileType.INTERFACE)) {

            /**
             * If interface, only getter will be there.
             */
            return getGetterForInterface(yangName, returnType);
        } else if (genFileTypes.equals(GeneratedFileType.BUILDER_INTERFACE)) {

            /**
             * If builder interface, getters and setters will be there.
             */
            if (methodTypes.equals(GeneratedMethodTypes.GETTER)) {

                return getGetterForInterface(yangName, returnType);
            } else if (methodTypes.equals(GeneratedMethodTypes.SETTER)) {

                return getSetterForInterface(yangName, returnType);
            } else if (methodTypes.equals(GeneratedMethodTypes.BUILD)) {

                return getBuildForInterface(yangName);
            }
        } else if (genFileTypes.equals(GeneratedFileType.BUILDER_CLASS)) {

            /**
             * If Builder class , getters, setters ,build and default constructor impls will be there.
             */
            if (methodTypes.equals(GeneratedMethodTypes.GETTER)) {

                return getGetterForClass(yangName, returnType);
            } else if (methodTypes.equals(GeneratedMethodTypes.SETTER)) {

                return getSetterForClass(yangName, returnType);
            } else if (methodTypes.equals(GeneratedMethodTypes.BUILD)) {

                return getBuild(yangName);
            } else if (methodTypes.equals(GeneratedMethodTypes.DEFAULT_CONSTRUCTOR)) {

                return getDefaultConstructor(yangName + UtilConstants.BUILDER);
            }
        } else if (genFileTypes.equals(GeneratedFileType.IMPL)) {

            if (methodTypes.equals(GeneratedMethodTypes.GETTER)) {

                return getGetterForClass(yangName, returnType);
            } else if (methodTypes.equals(GeneratedMethodTypes.CONSTRUCTOR)) {

                return getConstructor(yangName);
            } else if (methodTypes.equals(GeneratedMethodTypes.DEFAULT_CONSTRUCTOR)) {

                return getDefaultConstructor(yangName + UtilConstants.IMPL);
            }
        }
        return null;
    }

    /**
     * Returns the methods strings for builder class.
     *
     * @param attr attribute info.
     * @return method string for builder class.
     */
    private static String parseBuilderMethodString(AttributeInfo attr) {

        String overrideString = UtilConstants.NEW_LINE + UtilConstants.FOUR_SPACE_INDENTATION + UtilConstants.OVERRIDE
                + UtilConstants.NEW_LINE;
        String getterString = JavaCodeSnippetGen.getJavaMethodInfo(GeneratedFileType.BUILDER_CLASS,
                attr.getAttributeName(), GeneratedMethodTypes.GETTER, attr.getAttributeType());
        String setterString = JavaCodeSnippetGen.getJavaMethodInfo(GeneratedFileType.BUILDER_CLASS,
                attr.getAttributeName(), GeneratedMethodTypes.SETTER, attr.getAttributeType());
        return overrideString + getterString + UtilConstants.NEW_LINE + overrideString + setterString
                + UtilConstants.NEW_LINE;
    }

    /**
     * Returns the methods strings for builder class.
     *
     * @param attr attribute info.
     * @return method string for builder class.
     */
    private static String parseImplMethodString(AttributeInfo attr) {

        return UtilConstants.NEW_LINE + UtilConstants.FOUR_SPACE_INDENTATION + UtilConstants.OVERRIDE
                + UtilConstants.NEW_LINE + JavaCodeSnippetGen.getJavaMethodInfo(GeneratedFileType.BUILDER_CLASS,
                        attr.getAttributeName(), GeneratedMethodTypes.GETTER, attr.getAttributeType())
                + UtilConstants.NEW_LINE;
    }

    /**
     * Returns the methods strings for builder interface.
     *
     * @param attr attribute info.
     * @return method string for builder interface.
     */
    private static String parseBuilderInterfaceMethodString(AttributeInfo attr) {

        return getGetterString(attr) + UtilConstants.NEW_LINE + getSetterString(attr);
    }

    /**
     * Returns the methods strings for builder interface.
     *
     * @param name attribute name.
     * @return method string for builder interface.
     */
    public static String parseBuilderInterfaceBuildMethodString(String name) {

        return JavaDocGen.getJavaDoc(JavaDocGen.JavaDocType.BUILD, name) + JavaCodeSnippetGen
                .getJavaMethodInfo(GeneratedFileType.BUILDER_INTERFACE, name, GeneratedMethodTypes.BUILD, null);
    }

    /**
     * Returns getter string.
     *
     * @param attr attribute info.
     * @return getter string
     */
    public static String getGetterString(AttributeInfo attr) {

        return JavaDocGen.getJavaDoc(JavaDocGen.JavaDocType.GETTER, attr.getAttributeName())
                + JavaCodeSnippetGen.getJavaMethodInfo(GeneratedFileType.INTERFACE, attr.getAttributeName(),
                        GeneratedMethodTypes.GETTER, attr.getAttributeType())
                + UtilConstants.NEW_LINE;
    }

    /**
     * Returns setter string.
     *
     * @param attr attribute info.
     * @return setter string
     */
    private static String getSetterString(AttributeInfo attr) {

        return JavaDocGen.getJavaDoc(JavaDocGen.JavaDocType.SETTER, attr.getAttributeName())
                + JavaCodeSnippetGen.getJavaMethodInfo(GeneratedFileType.BUILDER_INTERFACE, attr.getAttributeName(),
                        GeneratedMethodTypes.SETTER, attr.getAttributeType());
    }

    /**
     * Returns constructor method string.
     *
     * @param name class name
     * @return constructor string
     */
    public static String getConstructorString(String name) {

        return JavaDocGen.getJavaDoc(JavaDocGen.JavaDocType.CONSTRUCTOR, name) + JavaCodeSnippetGen
                .getJavaMethodInfo(GeneratedFileType.IMPL, name, GeneratedMethodTypes.CONSTRUCTOR, null);
    }

    /**
     * Returns default constructor method string.
     *
     * @param type generated file type
     * @param name class name
     * @return default constructor string
     */
    public static String getDefaultConstructorString(GeneratedFileType type, String name) {

        return JavaDocGen.getJavaDoc(JavaDocGen.JavaDocType.DEFAULT_CONSTRUCTOR, name)
                + JavaCodeSnippetGen.getJavaMethodInfo(type, name, GeneratedMethodTypes.DEFAULT_CONSTRUCTOR, null);
    }

    /**
     * Returns build method string.
     *
     * @param name class name
     * @return build string
     */
    public static String getBuildString(String name) {

        return UtilConstants.FOUR_SPACE_INDENTATION + UtilConstants.OVERRIDE + UtilConstants.NEW_LINE
                + JavaCodeSnippetGen.getJavaMethodInfo(GeneratedFileType.BUILDER_CLASS, name,
                        GeneratedMethodTypes.BUILD, null);
    }

    /**
     * Returns the getter method strings for class file.
     *
     * @param yangName name of the attribute.
     * @param returnType return type of attribute
     * @return getter method for class.
     */
    private static String getGetterForClass(String yangName, YangType<?> returnType) {

        return UtilConstants.FOUR_SPACE_INDENTATION + UtilConstants.PUBLIC + UtilConstants.SPACE
                + JavaIdentifierSyntax.getCaptialCase(returnType.getDataTypeName()) + UtilConstants.SPACE
                + UtilConstants.GET_METHOD_PREFIX + JavaIdentifierSyntax.getCaptialCase(yangName)
                + UtilConstants.OPEN_PARENTHESIS + UtilConstants.CLOSE_PARENTHESIS + UtilConstants.SPACE
                + UtilConstants.OPEN_CURLY_BRACKET + UtilConstants.NEW_LINE + UtilConstants.EIGHT_SPACE_INDENTATION
                + UtilConstants.RETURN + UtilConstants.SPACE + yangName + UtilConstants.SEMI_COLAN
                + UtilConstants.NEW_LINE + UtilConstants.FOUR_SPACE_INDENTATION + UtilConstants.CLOSE_CURLY_BRACKET;
    }

    /**
     * Returns the setter method strings for class file.
     *
     * @param yangName name of the attribute.
     * @param returnType return type of attribute
     * @return setter method for class.
     */
    private static String getSetterForClass(String yangName, YangType<?> returnType) {

        return UtilConstants.FOUR_SPACE_INDENTATION + UtilConstants.PUBLIC + UtilConstants.SPACE + getBuilderClassName()
        + UtilConstants.SPACE + UtilConstants.SET_METHOD_PREFIX + JavaIdentifierSyntax.getCaptialCase(yangName)
        + UtilConstants.OPEN_PARENTHESIS + JavaIdentifierSyntax.getCaptialCase(returnType.getDataTypeName())
        + UtilConstants.SPACE + yangName + UtilConstants.CLOSE_PARENTHESIS + UtilConstants.SPACE
        + UtilConstants.OPEN_CURLY_BRACKET + UtilConstants.NEW_LINE + UtilConstants.EIGHT_SPACE_INDENTATION
        + UtilConstants.THIS + UtilConstants.PERIOD + yangName + UtilConstants.SPACE + UtilConstants.EQUAL
        + UtilConstants.SPACE + yangName + UtilConstants.SEMI_COLAN + UtilConstants.NEW_LINE
        + UtilConstants.EIGHT_SPACE_INDENTATION + UtilConstants.RETURN + UtilConstants.SPACE
        + UtilConstants.THIS + UtilConstants.SEMI_COLAN + UtilConstants.NEW_LINE
        + UtilConstants.FOUR_SPACE_INDENTATION + UtilConstants.CLOSE_CURLY_BRACKET;
    }

    /**
     * Returns the getter method strings for interface file.
     *
     * @param yangName name of the attribute.
     * @param returnType return type of attribute
     * @return getter method for interface.
     */
    private static String getGetterForInterface(String yangName, YangType<?> returnType) {
        returnType.setDataTypeName(returnType.getDataTypeName().replace("\"", ""));
        return UtilConstants.FOUR_SPACE_INDENTATION + JavaIdentifierSyntax.getCaptialCase(returnType.getDataTypeName())
        + UtilConstants.SPACE + UtilConstants.GET_METHOD_PREFIX + JavaIdentifierSyntax.getCaptialCase(yangName)
        + UtilConstants.OPEN_PARENTHESIS + UtilConstants.CLOSE_PARENTHESIS + UtilConstants.SEMI_COLAN;
    }

    /**
     * Returns the setter method strings for interface file.
     *
     * @param yangName name of the attribute.
     * @param returnType return type of attribute
     * @return setter method for interface.
     */
    private static String getSetterForInterface(String yangName, YangType<?> returnType) {
        return UtilConstants.FOUR_SPACE_INDENTATION + UtilConstants.BUILDER + UtilConstants.SPACE
                + UtilConstants.SET_METHOD_PREFIX + JavaIdentifierSyntax.getCaptialCase(yangName)
                + UtilConstants.OPEN_PARENTHESIS + JavaIdentifierSyntax.getCaptialCase(returnType.getDataTypeName())
                + UtilConstants.SPACE + yangName + UtilConstants.CLOSE_PARENTHESIS + UtilConstants.SEMI_COLAN;
    }

    /**
     * Returns the build method strings for interface file.
     *
     * @param yangName name of the attribute.
     * @param returnType return type of attribute
     * @return build method for interface.
     */
    private static String getBuildForInterface(String yangName) {

        return UtilConstants.FOUR_SPACE_INDENTATION + yangName + UtilConstants.SPACE + UtilConstants.BUILD
                + UtilConstants.OPEN_PARENTHESIS + UtilConstants.CLOSE_PARENTHESIS + UtilConstants.SEMI_COLAN;
    }

    /**
     * Returns the constructor strings for class file.
     *
     * @param yangName name of the class.
     * @return constructor for class
     */
    private static String getConstructor(String yangName) {

        String builderAttribute = (yangName.substring(0, 1).toLowerCase() + yangName.substring(1));
        String constructor = UtilConstants.FOUR_SPACE_INDENTATION + UtilConstants.PUBLIC + UtilConstants.SPACE
                + yangName + UtilConstants.IMPL + UtilConstants.OPEN_PARENTHESIS + yangName + UtilConstants.BUILDER
                + UtilConstants.SPACE + builderAttribute + UtilConstants.OBJECT + UtilConstants.CLOSE_PARENTHESIS
                + UtilConstants.SPACE + UtilConstants.OPEN_CURLY_BRACKET + UtilConstants.NEW_LINE;

        if (getAttrInfo() != null) {
            for (AttributeInfo attribute : getAttrInfo()) {
                attribute.setAttributeName(JavaIdentifierSyntax.getCamelCase(attribute.getAttributeName()));
                constructor = constructor + UtilConstants.TWELVE_SPACE_INDENTATION + UtilConstants.THIS
                        + UtilConstants.PERIOD + attribute.getAttributeName() + UtilConstants.SPACE
                        + UtilConstants.EQUAL + UtilConstants.SPACE + builderAttribute + UtilConstants.OBJECT
                        + UtilConstants.PERIOD + UtilConstants.GET_METHOD_PREFIX
                        + JavaIdentifierSyntax.getCaptialCase(attribute.getAttributeName())
                        + UtilConstants.OPEN_PARENTHESIS + UtilConstants.CLOSE_PARENTHESIS + UtilConstants.SEMI_COLAN
                        + UtilConstants.NEW_LINE;
            }
            getAttrInfo().clear();
        }
        return constructor + UtilConstants.FOUR_SPACE_INDENTATION + UtilConstants.CLOSE_CURLY_BRACKET
                + UtilConstants.NEW_LINE;
    }

    /**
     * Returns the build method strings for class file.
     *
     * @param yangName class name
     * @return build method string for class.
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
     * @param yangName name of the class.
     * @return Default constructor for class
     */
    private static String getDefaultConstructor(String name) {

        return UtilConstants.FOUR_SPACE_INDENTATION + UtilConstants.PUBLIC + UtilConstants.SPACE + name
                + UtilConstants.OPEN_PARENTHESIS + UtilConstants.CLOSE_PARENTHESIS + UtilConstants.SPACE
                + UtilConstants.OPEN_CURLY_BRACKET + UtilConstants.NEW_LINE + UtilConstants.FOUR_SPACE_INDENTATION
                + UtilConstants.CLOSE_CURLY_BRACKET + UtilConstants.NEW_LINE;
    }

}

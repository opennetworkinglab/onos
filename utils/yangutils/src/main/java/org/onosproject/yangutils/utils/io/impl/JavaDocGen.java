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

package org.onosproject.yangutils.utils.io.impl;

import org.onosproject.yangutils.translator.tojava.utils.JavaIdentifierSyntax;
import org.onosproject.yangutils.utils.UtilConstants;

/**
 * Provides javadoc for the generated classes.
 */
public final class JavaDocGen {

    /**
     * Default Constructor.
     */
    private JavaDocGen() {
    }

    /**
     * JavaDocs types.
     */
    public static enum JavaDocType {

        /**
         * For class.
         */
        IMPL_CLASS,

        /**
         * For builder class.
         */
        BUILDER_CLASS,

        /**
         * For interface.
         */
        INTERFACE,

        /**
         * For builder interface.
         */
        BUILDER_INTERFACE,

        /**
         * For package-info.
         */
        PACKAGE_INFO,

        /**
         * For getters.
         */
        GETTER,

        /**
         * For setters.
         */
        SETTER,

        /**
         * For type def's setters.
         */
        TYPE_DEF_SETTER,

        /**
         * For type def's constructor.
         */
        TYPE_DEF_CONSTRUCTOR,

        /**
         * For of method.
         */
        OF,

        /**
         * For default constructor.
         */
        DEFAULT_CONSTRUCTOR,

        /**
         * For constructor.
         */
        CONSTRUCTOR,

        /**
         * For build.
         */
        BUILD
    }

    /**
     * Returns java docs.
     *
     * @param type java doc type
     * @param name name of the YangNode
     * @param isList is list attribute
     * @return javadocs.
     */
    public static String getJavaDoc(JavaDocType type, String name, boolean isList) {
        name = JavaIdentifierSyntax.getLowerCase(JavaIdentifierSyntax.getCamelCase(name));
        String javaDoc = "";
        if (type.equals(JavaDocType.IMPL_CLASS)) {
            javaDoc = generateForImplClass(name);
        } else if (type.equals(JavaDocType.BUILDER_CLASS)) {
            javaDoc = generateForBuilderClass(name);
        } else if (type.equals(JavaDocType.INTERFACE)) {
            javaDoc = generateForInterface(name);
        } else if (type.equals(JavaDocType.BUILDER_INTERFACE)) {
            javaDoc = generateForBuilderInterface(name);
        } else if (type.equals(JavaDocType.PACKAGE_INFO)) {
            javaDoc = generateForPackage(name);
        } else if (type.equals(JavaDocType.GETTER)) {
            javaDoc = generateForGetters(name, isList);
        } else if (type.equals(JavaDocType.TYPE_DEF_SETTER)) {
            javaDoc = generateForTypeDefSetter(name);
        } else if (type.equals(JavaDocType.TYPE_DEF_CONSTRUCTOR)) {
            javaDoc = generateForTypeDefConstructor(name);
        } else if (type.equals(JavaDocType.SETTER)) {
            javaDoc = generateForSetters(name, isList);
        } else if (type.equals(JavaDocType.OF)) {
            javaDoc = generateForOf(name);
        } else if (type.equals(JavaDocType.DEFAULT_CONSTRUCTOR)) {
            javaDoc = generateForDefaultConstructors();
        } else if (type.equals(JavaDocType.BUILD)) {
            javaDoc = generateForBuild(name);
        } else {
            javaDoc = generateForConstructors(name);
        }
        return javaDoc;
    }

    /**
     * Generate javaDocs for getter method.
     *
     * @param attribute attribute
     * @param isList is list attribute
     * @return javaDocs
     */
    private static String generateForGetters(String attribute, boolean isList) {
        String getter = UtilConstants.NEW_LINE + UtilConstants.FOUR_SPACE_INDENTATION
                + UtilConstants.JAVA_DOC_FIRST_LINE
                + UtilConstants.FOUR_SPACE_INDENTATION + UtilConstants.JAVA_DOC_GETTERS + attribute
                + UtilConstants.PERIOD + UtilConstants.NEW_LINE + UtilConstants.FOUR_SPACE_INDENTATION
                + UtilConstants.NEW_LINE_ESTRIC + UtilConstants.FOUR_SPACE_INDENTATION + UtilConstants.JAVA_DOC_RETURN;
        if (isList) {
            attribute = UtilConstants.LIST.toLowerCase() + UtilConstants.SPACE + UtilConstants.OF + UtilConstants.SPACE
                    + attribute;
        }

        getter = getter + attribute + UtilConstants.NEW_LINE + UtilConstants.FOUR_SPACE_INDENTATION
                + UtilConstants.JAVA_DOC_END_LINE;
        return getter;
    }

    /**
     * Generates javaDocs for setter method.
     *
     * @param attribute attribute
     * @param isList is list attribute
     * @return javaDocs
     */
    private static String generateForSetters(String attribute, boolean isList) {
        String setter = UtilConstants.NEW_LINE + UtilConstants.FOUR_SPACE_INDENTATION
                + UtilConstants.JAVA_DOC_FIRST_LINE
                + UtilConstants.FOUR_SPACE_INDENTATION + UtilConstants.JAVA_DOC_SETTERS + attribute
                + UtilConstants.PERIOD + UtilConstants.NEW_LINE + UtilConstants.FOUR_SPACE_INDENTATION
                + UtilConstants.NEW_LINE_ESTRIC + UtilConstants.FOUR_SPACE_INDENTATION + UtilConstants.JAVA_DOC_PARAM
                + attribute + UtilConstants.SPACE;
        if (isList) {
            attribute = UtilConstants.LIST.toLowerCase() + UtilConstants.SPACE + UtilConstants.OF + UtilConstants.SPACE
                    + attribute;
        }

        setter = setter + attribute + UtilConstants.NEW_LINE
                + UtilConstants.FOUR_SPACE_INDENTATION + UtilConstants.JAVA_DOC_RETURN + UtilConstants.BUILDER_OBJECT
                + attribute + UtilConstants.NEW_LINE + UtilConstants.FOUR_SPACE_INDENTATION
                + UtilConstants.JAVA_DOC_END_LINE;
        return setter;
    }

    /**
     * Generates javaDocs for of method.
     *
     * @param attribute attribute
     * @return javaDocs
     */
    private static String generateForOf(String attribute) {
        return UtilConstants.NEW_LINE + UtilConstants.FOUR_SPACE_INDENTATION + UtilConstants.JAVA_DOC_FIRST_LINE
                + UtilConstants.FOUR_SPACE_INDENTATION + UtilConstants.JAVA_DOC_OF + attribute
                + UtilConstants.PERIOD + UtilConstants.NEW_LINE + UtilConstants.FOUR_SPACE_INDENTATION
                + UtilConstants.NEW_LINE_ESTRIC + UtilConstants.FOUR_SPACE_INDENTATION + UtilConstants.JAVA_DOC_PARAM
                + UtilConstants.VALUE + UtilConstants.SPACE + UtilConstants.VALUE + UtilConstants.NEW_LINE
                + UtilConstants.FOUR_SPACE_INDENTATION + UtilConstants.JAVA_DOC_RETURN + UtilConstants.OBJECT
                + UtilConstants.SPACE + UtilConstants.OF + UtilConstants.SPACE + attribute + UtilConstants.NEW_LINE
                + UtilConstants.FOUR_SPACE_INDENTATION + UtilConstants.JAVA_DOC_END_LINE;
    }

    /**
     * Generates javaDocs for typedef setter method.
     *
     * @param attribute attribute
     * @return javaDocs
     */
    private static String generateForTypeDefSetter(String attribute) {
        return (UtilConstants.NEW_LINE + UtilConstants.FOUR_SPACE_INDENTATION + UtilConstants.JAVA_DOC_FIRST_LINE
                + UtilConstants.FOUR_SPACE_INDENTATION + UtilConstants.JAVA_DOC_SETTERS_COMMON + attribute
                + UtilConstants.PERIOD + UtilConstants.NEW_LINE + UtilConstants.FOUR_SPACE_INDENTATION
                + UtilConstants.NEW_LINE_ESTRIC + UtilConstants.FOUR_SPACE_INDENTATION + UtilConstants.JAVA_DOC_PARAM
                + UtilConstants.VALUE + UtilConstants.SPACE + UtilConstants.VALUE + UtilConstants.NEW_LINE
                + UtilConstants.FOUR_SPACE_INDENTATION + UtilConstants.JAVA_DOC_END_LINE);
    }

    /**
     * Generates javaDocs for typedef constructor.
     *
     * @param attribute attribute
     * @return javaDocs
     */
    private static String generateForTypeDefConstructor(String attribute) {
        return (UtilConstants.NEW_LINE + UtilConstants.FOUR_SPACE_INDENTATION + UtilConstants.JAVA_DOC_FIRST_LINE
                + UtilConstants.FOUR_SPACE_INDENTATION + UtilConstants.JAVA_DOC_CONSTRUCTOR + attribute
                + UtilConstants.PERIOD + UtilConstants.NEW_LINE + UtilConstants.FOUR_SPACE_INDENTATION
                + UtilConstants.NEW_LINE_ESTRIC + UtilConstants.FOUR_SPACE_INDENTATION + UtilConstants.JAVA_DOC_PARAM
                + UtilConstants.VALUE + UtilConstants.SPACE + UtilConstants.VALUE + UtilConstants.NEW_LINE
                + UtilConstants.FOUR_SPACE_INDENTATION + UtilConstants.JAVA_DOC_END_LINE);
    }

    /**
     * Generate javaDocs for the impl class.
     *
     * @param className class name
     * @return javaDocs
     */
    private static String generateForImplClass(String className) {
        return UtilConstants.NEW_LINE + UtilConstants.JAVA_DOC_FIRST_LINE + UtilConstants.IMPL_CLASS_JAVA_DOC
                + className + UtilConstants.PERIOD + UtilConstants.NEW_LINE + UtilConstants.JAVA_DOC_END_LINE;
    }

    /**
     * Generate javaDocs for the builder class.
     *
     * @param className class name
     * @return javaDocs
     */
    private static String generateForBuilderClass(String className) {
        return UtilConstants.NEW_LINE + UtilConstants.JAVA_DOC_FIRST_LINE + UtilConstants.BUILDER_CLASS_JAVA_DOC
                + className + UtilConstants.PERIOD + UtilConstants.NEW_LINE + UtilConstants.JAVA_DOC_END_LINE;
    }

    /**
     * Generate javaDoc for the interface.
     *
     * @param interfaceName interface name
     * @return javaDocs
     */
    private static String generateForInterface(String interfaceName) {
        return UtilConstants.NEW_LINE + UtilConstants.JAVA_DOC_FIRST_LINE + UtilConstants.INTERFACE_JAVA_DOC
                + interfaceName + UtilConstants.PERIOD + UtilConstants.NEW_LINE + UtilConstants.JAVA_DOC_END_LINE;
    }

    /**
     * Generate javaDoc for the builder interface.
     *
     * @param builderforName builder for name
     * @return javaDocs
     */
    private static String generateForBuilderInterface(String builderforName) {
        return UtilConstants.JAVA_DOC_FIRST_LINE + UtilConstants.BUILDER_INTERFACE_JAVA_DOC + builderforName
                + UtilConstants.PERIOD + UtilConstants.NEW_LINE + UtilConstants.JAVA_DOC_END_LINE;
    }

    /**
     * Generate javaDocs for package-info.
     *
     * @param packageName package name
     * @return javaDocs
     */
    private static String generateForPackage(String packageName) {
        return UtilConstants.JAVA_DOC_FIRST_LINE + UtilConstants.PACKAGE_INFO_JAVADOC + packageName
                + UtilConstants.PERIOD + UtilConstants.NEW_LINE + UtilConstants.JAVA_DOC_END_LINE;
    }

    /**
     * Generate javaDocs for default constructor.
     *
     * @return javaDocs
     */
    private static String generateForDefaultConstructors() {
        return UtilConstants.FOUR_SPACE_INDENTATION + UtilConstants.JAVA_DOC_DEFAULT_CONSTRUCTOR
                + UtilConstants.FOUR_SPACE_INDENTATION + UtilConstants.JAVA_DOC_END_LINE;
    }

    /**
     * Generate javaDocs for constructor with parameters.
     *
     * @param className class name
     * @return javaDocs
     */
    private static String generateForConstructors(String className) {
        return UtilConstants.NEW_LINE + UtilConstants.FOUR_SPACE_INDENTATION + UtilConstants.JAVA_DOC_FIRST_LINE
                + UtilConstants.FOUR_SPACE_INDENTATION + UtilConstants.JAVA_DOC_CONSTRUCTOR + className
                + UtilConstants.IMPL + UtilConstants.PERIOD + UtilConstants.NEW_LINE
                + UtilConstants.FOUR_SPACE_INDENTATION + UtilConstants.NEW_LINE_ESTRIC
                + UtilConstants.FOUR_SPACE_INDENTATION + UtilConstants.JAVA_DOC_PARAM
                + className.substring(0, 1).toLowerCase() + className.substring(1) + UtilConstants.BUILDER
                + UtilConstants.OBJECT + UtilConstants.SPACE + UtilConstants.BUILDER_OBJECT + UtilConstants.SPACE
                + className + UtilConstants.NEW_LINE + UtilConstants.FOUR_SPACE_INDENTATION
                + UtilConstants.JAVA_DOC_END_LINE;
    }

    /**
     * Generate javaDocs for build.
     *
     * @param buildName builder name
     * @return javaDocs
     */
    private static String generateForBuild(String buildName) {
        return UtilConstants.NEW_LINE + UtilConstants.FOUR_SPACE_INDENTATION + UtilConstants.JAVA_DOC_FIRST_LINE
                + UtilConstants.FOUR_SPACE_INDENTATION + UtilConstants.JAVA_DOC_BUILD + buildName + UtilConstants.PERIOD
                + UtilConstants.NEW_LINE + UtilConstants.FOUR_SPACE_INDENTATION + UtilConstants.NEW_LINE_ESTRIC
                + UtilConstants.FOUR_SPACE_INDENTATION + UtilConstants.JAVA_DOC_RETURN
                + UtilConstants.JAVA_DOC_BUILD_RETURN + buildName + UtilConstants.PERIOD + UtilConstants.NEW_LINE
                + UtilConstants.FOUR_SPACE_INDENTATION + UtilConstants.JAVA_DOC_END_LINE;
    }
}

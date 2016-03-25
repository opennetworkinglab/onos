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

import static org.onosproject.yangutils.utils.UtilConstants.BUILDER;
import static org.onosproject.yangutils.utils.UtilConstants.BUILDER_CLASS_JAVA_DOC;
import static org.onosproject.yangutils.utils.UtilConstants.BUILDER_INTERFACE_JAVA_DOC;
import static org.onosproject.yangutils.utils.UtilConstants.BUILDER_OBJECT;
import static org.onosproject.yangutils.utils.UtilConstants.FOUR_SPACE_INDENTATION;
import static org.onosproject.yangutils.utils.UtilConstants.IMPL;
import static org.onosproject.yangutils.utils.UtilConstants.IMPL_CLASS_JAVA_DOC;
import static org.onosproject.yangutils.utils.UtilConstants.INTERFACE_JAVA_DOC;
import static org.onosproject.yangutils.utils.UtilConstants.JAVA_DOC_BUILD;
import static org.onosproject.yangutils.utils.UtilConstants.JAVA_DOC_BUILD_RETURN;
import static org.onosproject.yangutils.utils.UtilConstants.JAVA_DOC_CONSTRUCTOR;
import static org.onosproject.yangutils.utils.UtilConstants.JAVA_DOC_DEFAULT_CONSTRUCTOR;
import static org.onosproject.yangutils.utils.UtilConstants.JAVA_DOC_END_LINE;
import static org.onosproject.yangutils.utils.UtilConstants.JAVA_DOC_FIRST_LINE;
import static org.onosproject.yangutils.utils.UtilConstants.JAVA_DOC_GETTERS;
import static org.onosproject.yangutils.utils.UtilConstants.JAVA_DOC_OF;
import static org.onosproject.yangutils.utils.UtilConstants.JAVA_DOC_PARAM;
import static org.onosproject.yangutils.utils.UtilConstants.JAVA_DOC_RETURN;
import static org.onosproject.yangutils.utils.UtilConstants.JAVA_DOC_SETTERS;
import static org.onosproject.yangutils.utils.UtilConstants.JAVA_DOC_SETTERS_COMMON;
import static org.onosproject.yangutils.utils.UtilConstants.LIST;
import static org.onosproject.yangutils.utils.UtilConstants.NEW_LINE;
import static org.onosproject.yangutils.utils.UtilConstants.NEW_LINE_ASTERISK;
import static org.onosproject.yangutils.utils.UtilConstants.OBJECT;
import static org.onosproject.yangutils.utils.UtilConstants.OF;
import static org.onosproject.yangutils.utils.UtilConstants.PACKAGE_INFO_JAVADOC;
import static org.onosproject.yangutils.utils.UtilConstants.PERIOD;
import static org.onosproject.yangutils.utils.UtilConstants.SPACE;
import static org.onosproject.yangutils.utils.UtilConstants.VALUE;

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
        GETTER_METHOD,

        /**
         * For setters.
         */
        SETTER_METHOD,

        /**
         * For type def's setters.
         */
        TYPE_DEF_SETTER_METHOD,

        /**
         * For type def's constructor.
         */
        TYPE_DEF_CONSTRUCTOR,

        /**
         * For of method.
         */
        OF_METHOD,

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
        BUILD_METHOD
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

        name = JavaIdentifierSyntax.getSmallCase(JavaIdentifierSyntax.getCamelCase(name));
        String javaDoc = UtilConstants.EMPTY_STRING;
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
        } else if (type.equals(JavaDocType.GETTER_METHOD)) {
            javaDoc = generateForGetters(name, isList);
        } else if (type.equals(JavaDocType.TYPE_DEF_SETTER_METHOD)) {
            javaDoc = generateForTypeDefSetter(name);
        } else if (type.equals(JavaDocType.TYPE_DEF_CONSTRUCTOR)) {
            javaDoc = generateForTypeDefConstructor(name);
        } else if (type.equals(JavaDocType.SETTER_METHOD)) {
            javaDoc = generateForSetters(name, isList);
        } else if (type.equals(JavaDocType.OF_METHOD)) {
            javaDoc = generateForOf(name);
        } else if (type.equals(JavaDocType.DEFAULT_CONSTRUCTOR)) {
            javaDoc = generateForDefaultConstructors();
        } else if (type.equals(JavaDocType.BUILD_METHOD)) {
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

        String getter = NEW_LINE + FOUR_SPACE_INDENTATION + JAVA_DOC_FIRST_LINE + FOUR_SPACE_INDENTATION
                + JAVA_DOC_GETTERS + attribute + PERIOD + NEW_LINE + FOUR_SPACE_INDENTATION + NEW_LINE_ASTERISK
                + FOUR_SPACE_INDENTATION + JAVA_DOC_RETURN;
        if (isList) {
            String listAttribute = LIST.toLowerCase() + SPACE + OF + SPACE;
            getter = getter + listAttribute;
        } else {
            getter = getter + VALUE + SPACE + OF + SPACE;
        }

        getter = getter + attribute + NEW_LINE + FOUR_SPACE_INDENTATION + JAVA_DOC_END_LINE;
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

        String setter = NEW_LINE + FOUR_SPACE_INDENTATION + JAVA_DOC_FIRST_LINE + FOUR_SPACE_INDENTATION
                + JAVA_DOC_SETTERS + attribute + PERIOD + NEW_LINE + FOUR_SPACE_INDENTATION + NEW_LINE_ASTERISK
                + FOUR_SPACE_INDENTATION + JAVA_DOC_PARAM + attribute + SPACE;
        if (isList) {
            String listAttribute = LIST.toLowerCase() + SPACE + OF + SPACE;
            setter = setter + listAttribute;
        } else {
            setter = setter + VALUE + SPACE + OF + SPACE;
        }
        setter = setter + attribute + NEW_LINE + FOUR_SPACE_INDENTATION + JAVA_DOC_RETURN + BUILDER_OBJECT + attribute
                + NEW_LINE + FOUR_SPACE_INDENTATION + JAVA_DOC_END_LINE;
        return setter;
    }

    /**
     * Generates javaDocs for of method.
     *
     * @param attribute attribute
     * @return javaDocs
     */
    private static String generateForOf(String attribute) {

        return NEW_LINE + FOUR_SPACE_INDENTATION + JAVA_DOC_FIRST_LINE + FOUR_SPACE_INDENTATION + JAVA_DOC_OF
                + attribute + PERIOD + NEW_LINE + FOUR_SPACE_INDENTATION + NEW_LINE_ASTERISK + FOUR_SPACE_INDENTATION
                + JAVA_DOC_PARAM + VALUE + SPACE + VALUE + SPACE + OF + SPACE + attribute + NEW_LINE
                + FOUR_SPACE_INDENTATION + JAVA_DOC_RETURN + OBJECT + SPACE + OF + SPACE + attribute + NEW_LINE
                + FOUR_SPACE_INDENTATION + JAVA_DOC_END_LINE;
    }

    /**
     * Generates javaDocs for typedef setter method.
     *
     * @param attribute attribute
     * @return javaDocs
     */
    private static String generateForTypeDefSetter(String attribute) {

        return (NEW_LINE + FOUR_SPACE_INDENTATION + JAVA_DOC_FIRST_LINE + FOUR_SPACE_INDENTATION
                + JAVA_DOC_SETTERS_COMMON + attribute + PERIOD + NEW_LINE + FOUR_SPACE_INDENTATION + NEW_LINE_ASTERISK
                + FOUR_SPACE_INDENTATION + JAVA_DOC_PARAM + VALUE + SPACE + VALUE + SPACE + OF + SPACE + attribute
                + NEW_LINE + FOUR_SPACE_INDENTATION + JAVA_DOC_END_LINE);
    }

    /**
     * Generates javaDocs for typedef constructor.
     *
     * @param attribute attribute
     * @return javaDocs
     */
    private static String generateForTypeDefConstructor(String attribute) {

        return (NEW_LINE + FOUR_SPACE_INDENTATION + JAVA_DOC_FIRST_LINE + FOUR_SPACE_INDENTATION + JAVA_DOC_CONSTRUCTOR
                + attribute + PERIOD + NEW_LINE + FOUR_SPACE_INDENTATION + NEW_LINE_ASTERISK + FOUR_SPACE_INDENTATION
                + JAVA_DOC_PARAM + VALUE + SPACE + VALUE + SPACE + OF + SPACE + attribute + NEW_LINE
                + FOUR_SPACE_INDENTATION + JAVA_DOC_END_LINE);
    }

    /**
     * Generate javaDocs for the impl class.
     *
     * @param className class name
     * @return javaDocs
     */
    private static String generateForImplClass(String className) {

        return NEW_LINE + JAVA_DOC_FIRST_LINE + IMPL_CLASS_JAVA_DOC + className + PERIOD + NEW_LINE + JAVA_DOC_END_LINE;
    }

    /**
     * Generate javaDocs for the builder class.
     *
     * @param className class name
     * @return javaDocs
     */
    private static String generateForBuilderClass(String className) {

        return NEW_LINE + JAVA_DOC_FIRST_LINE + BUILDER_CLASS_JAVA_DOC + className + PERIOD + NEW_LINE
                + JAVA_DOC_END_LINE;
    }

    /**
     * Generate javaDoc for the interface.
     *
     * @param interfaceName interface name
     * @return javaDocs
     */
    private static String generateForInterface(String interfaceName) {

        return NEW_LINE + JAVA_DOC_FIRST_LINE + INTERFACE_JAVA_DOC + interfaceName + PERIOD + NEW_LINE
                + JAVA_DOC_END_LINE;
    }

    /**
     * Generate javaDoc for the builder interface.
     *
     * @param builderforName builder for name
     * @return javaDocs
     */
    private static String generateForBuilderInterface(String builderforName) {

        return JAVA_DOC_FIRST_LINE + BUILDER_INTERFACE_JAVA_DOC + builderforName + PERIOD + NEW_LINE
                + JAVA_DOC_END_LINE;
    }

    /**
     * Generate javaDocs for package-info.
     *
     * @param packageName package name
     * @return javaDocs
     */
    private static String generateForPackage(String packageName) {

        return JAVA_DOC_FIRST_LINE + PACKAGE_INFO_JAVADOC + packageName + PERIOD + NEW_LINE + JAVA_DOC_END_LINE;
    }

    /**
     * Generate javaDocs for default constructor.
     *
     * @return javaDocs
     */
    private static String generateForDefaultConstructors() {

        return FOUR_SPACE_INDENTATION + JAVA_DOC_FIRST_LINE + FOUR_SPACE_INDENTATION + JAVA_DOC_DEFAULT_CONSTRUCTOR
                + FOUR_SPACE_INDENTATION + JAVA_DOC_END_LINE;
    }

    /**
     * Generate javaDocs for constructor with parameters.
     *
     * @param className class name
     * @return javaDocs
     */
    private static String generateForConstructors(String className) {

        return NEW_LINE + FOUR_SPACE_INDENTATION + JAVA_DOC_FIRST_LINE
                + FOUR_SPACE_INDENTATION + JAVA_DOC_CONSTRUCTOR + className + IMPL + PERIOD + NEW_LINE
                + FOUR_SPACE_INDENTATION + NEW_LINE_ASTERISK + FOUR_SPACE_INDENTATION + JAVA_DOC_PARAM
                + BUILDER.toLowerCase() + OBJECT + SPACE + BUILDER_OBJECT + className + NEW_LINE
                + FOUR_SPACE_INDENTATION + JAVA_DOC_END_LINE;
    }

    /**
     * Generate javaDocs for build.
     *
     * @param buildName builder name
     * @return javaDocs
     */
    private static String generateForBuild(String buildName) {

        return NEW_LINE + FOUR_SPACE_INDENTATION + JAVA_DOC_FIRST_LINE + FOUR_SPACE_INDENTATION + JAVA_DOC_BUILD
                + buildName + PERIOD + NEW_LINE + FOUR_SPACE_INDENTATION + NEW_LINE_ASTERISK + FOUR_SPACE_INDENTATION
                + JAVA_DOC_RETURN + JAVA_DOC_BUILD_RETURN + buildName + PERIOD + NEW_LINE + FOUR_SPACE_INDENTATION
                + JAVA_DOC_END_LINE;
    }
}

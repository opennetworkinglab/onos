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

package org.onosproject.yangutils.translator.tojava.utils;

import org.onosproject.yangutils.translator.tojava.JavaQualifiedTypeInfo;
import org.onosproject.yangutils.utils.io.impl.YangPluginConfig;

import static org.onosproject.yangutils.translator.tojava.utils.JavaIdentifierSyntax.getEnumJavaAttribute;
import static org.onosproject.yangutils.utils.io.impl.YangIoUtils.getSmallCase;
import static org.onosproject.yangutils.utils.UtilConstants.ARRAY_LIST;
import static org.onosproject.yangutils.utils.UtilConstants.AUGMENTED_INFO;
import static org.onosproject.yangutils.utils.UtilConstants.CLOSE_CURLY_BRACKET;
import static org.onosproject.yangutils.utils.UtilConstants.CLOSE_PARENTHESIS;
import static org.onosproject.yangutils.utils.UtilConstants.COMMA;
import static org.onosproject.yangutils.utils.UtilConstants.DIAMOND_CLOSE_BRACKET;
import static org.onosproject.yangutils.utils.UtilConstants.DIAMOND_OPEN_BRACKET;
import static org.onosproject.yangutils.utils.UtilConstants.EQUAL;
import static org.onosproject.yangutils.utils.UtilConstants.FOUR_SPACE_INDENTATION;
import static org.onosproject.yangutils.utils.UtilConstants.IMPORT;
import static org.onosproject.yangutils.utils.UtilConstants.LIST;
import static org.onosproject.yangutils.utils.UtilConstants.NEW;
import static org.onosproject.yangutils.utils.UtilConstants.NEW_LINE;
import static org.onosproject.yangutils.utils.UtilConstants.OPEN_PARENTHESIS;
import static org.onosproject.yangutils.utils.UtilConstants.PERIOD;
import static org.onosproject.yangutils.utils.UtilConstants.PRIVATE;
import static org.onosproject.yangutils.utils.UtilConstants.SEMI_COLAN;
import static org.onosproject.yangutils.utils.UtilConstants.SPACE;
import static org.onosproject.yangutils.utils.io.impl.JavaDocGen.getJavaDoc;
import static org.onosproject.yangutils.utils.io.impl.JavaDocGen.JavaDocType.ENUM_ATTRIBUTE;

/**
 * Represents utility class to generate the java snippet.
 */
public final class JavaCodeSnippetGen {

    /**
     * Creates an instance of java code snippet gen.
     */
    private JavaCodeSnippetGen() {
    }

    /**
     * Returns the java file header comment.
     *
     * @return the java file header comment
     */
    public static String getFileHeaderComment() {

        /**
         * TODO return the file header.
         */
        return null;
    }

    /**
     * Returns the textual java code information corresponding to the import list.
     *
     * @param importInfo import info
     * @return the textual java code information corresponding to the import
     *         list
     */
    public static String getImportText(JavaQualifiedTypeInfo importInfo) {
        return IMPORT + importInfo.getPkgInfo() + PERIOD + importInfo.getClassInfo() + SEMI_COLAN + NEW_LINE;
    }

    /**
     * Returns the textual java code for attribute definition in class.
     *
     * @param javaAttributeTypePkg Package of the attribute type
     * @param javaAttributeType java attribute type
     * @param javaAttributeName name of the attribute
     * @param isList is list attribute
     * @return the textual java code for attribute definition in class
     */
    public static String getJavaAttributeDefination(String javaAttributeTypePkg, String javaAttributeType,
            String javaAttributeName, boolean isList) {

        String attributeDefination = PRIVATE + SPACE;

        if (!isList) {
            if (javaAttributeTypePkg != null) {
                attributeDefination = attributeDefination + javaAttributeTypePkg + PERIOD;
            }

            attributeDefination = attributeDefination + javaAttributeType + SPACE + javaAttributeName + SEMI_COLAN
                    + NEW_LINE;
        } else {
            attributeDefination = attributeDefination + LIST + DIAMOND_OPEN_BRACKET;
            if (javaAttributeTypePkg != null) {
                attributeDefination = attributeDefination + javaAttributeTypePkg + PERIOD;
            }

            attributeDefination = attributeDefination + javaAttributeType + DIAMOND_CLOSE_BRACKET + SPACE
                    + javaAttributeName + SEMI_COLAN + NEW_LINE;
        }
        return attributeDefination;
    }

    /**
     * Returns list attribute string.
     *
     * @param type attribute type
     * @return list attribute string
     */
    public static String getListAttribute(String type) {
        return LIST + DIAMOND_OPEN_BRACKET + type + DIAMOND_CLOSE_BRACKET;
    }

    /**
     * Returns attribute of augmented info for generated impl file.
     *
     * @return attribute of augmented info for generated impl file
     */
    public static String getAugmentedInfoAttribute() {
        return NEW_LINE + FOUR_SPACE_INDENTATION + PRIVATE + SPACE + getListAttribute(AUGMENTED_INFO) + SPACE
                + getSmallCase(AUGMENTED_INFO) + LIST + SPACE + EQUAL + SPACE + NEW + SPACE + ARRAY_LIST
                + DIAMOND_OPEN_BRACKET + DIAMOND_CLOSE_BRACKET + OPEN_PARENTHESIS + CLOSE_PARENTHESIS + SEMI_COLAN;
    }

    /**
     * Returns based on the file type and the YANG name of the file, generate the class
     * / interface definition close.
     *
     * @return corresponding textual java code information
     */
    public static String getJavaClassDefClose() {
        return CLOSE_CURLY_BRACKET;
    }

    /**
     * Returns string for enum's attribute.
     *
     * @param name name of attribute
     * @param value value of the enum
     * @param pluginConfig plugin configurations
     * @return string for enum's attribute
     */
    public static String generateEnumAttributeString(String name, int value, YangPluginConfig pluginConfig) {
        return getJavaDoc(ENUM_ATTRIBUTE, name, false, pluginConfig) + FOUR_SPACE_INDENTATION
                + getEnumJavaAttribute(name).toUpperCase() + OPEN_PARENTHESIS
                + value + CLOSE_PARENTHESIS + COMMA + NEW_LINE;
    }

}

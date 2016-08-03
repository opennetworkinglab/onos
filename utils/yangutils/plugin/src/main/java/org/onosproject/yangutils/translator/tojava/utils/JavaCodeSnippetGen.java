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

import java.util.List;

import org.onosproject.yangutils.datamodel.YangNode;
import org.onosproject.yangutils.datamodel.javadatamodel.YangPluginConfig;
import org.onosproject.yangutils.translator.tojava.JavaCodeGeneratorInfo;
import org.onosproject.yangutils.translator.tojava.JavaQualifiedTypeInfoTranslator;
import org.onosproject.yangutils.translator.tojava.TempJavaServiceFragmentFiles;

import static org.onosproject.yangutils.translator.tojava.utils.JavaIdentifierSyntax.getEnumJavaAttribute;
import static org.onosproject.yangutils.utils.UtilConstants.ARRAY_LIST;
import static org.onosproject.yangutils.utils.UtilConstants.CLASS_STRING;
import static org.onosproject.yangutils.utils.UtilConstants.CLOSE_CURLY_BRACKET;
import static org.onosproject.yangutils.utils.UtilConstants.CLOSE_PARENTHESIS;
import static org.onosproject.yangutils.utils.UtilConstants.COMMA;
import static org.onosproject.yangutils.utils.UtilConstants.DIAMOND_CLOSE_BRACKET;
import static org.onosproject.yangutils.utils.UtilConstants.DIAMOND_OPEN_BRACKET;
import static org.onosproject.yangutils.utils.UtilConstants.EIGHT_SPACE_INDENTATION;
import static org.onosproject.yangutils.utils.UtilConstants.ENUM;
import static org.onosproject.yangutils.utils.UtilConstants.EQUAL;
import static org.onosproject.yangutils.utils.UtilConstants.FOUR_SPACE_INDENTATION;
import static org.onosproject.yangutils.utils.UtilConstants.HASH_MAP;
import static org.onosproject.yangutils.utils.UtilConstants.IMPORT;
import static org.onosproject.yangutils.utils.UtilConstants.INT;
import static org.onosproject.yangutils.utils.UtilConstants.INT_MAX_RANGE_ATTR;
import static org.onosproject.yangutils.utils.UtilConstants.INT_MIN_RANGE_ATTR;
import static org.onosproject.yangutils.utils.UtilConstants.LIST;
import static org.onosproject.yangutils.utils.UtilConstants.LISTENER_SERVICE;
import static org.onosproject.yangutils.utils.UtilConstants.LONG_MAX_RANGE_ATTR;
import static org.onosproject.yangutils.utils.UtilConstants.LONG_MIN_RANGE_ATTR;
import static org.onosproject.yangutils.utils.UtilConstants.MAP;
import static org.onosproject.yangutils.utils.UtilConstants.NEW;
import static org.onosproject.yangutils.utils.UtilConstants.NEW_LINE;
import static org.onosproject.yangutils.utils.UtilConstants.OBJECT_STRING;
import static org.onosproject.yangutils.utils.UtilConstants.OPEN_CURLY_BRACKET;
import static org.onosproject.yangutils.utils.UtilConstants.OPEN_PARENTHESIS;
import static org.onosproject.yangutils.utils.UtilConstants.PERIOD;
import static org.onosproject.yangutils.utils.UtilConstants.PRIVATE;
import static org.onosproject.yangutils.utils.UtilConstants.PUBLIC;
import static org.onosproject.yangutils.utils.UtilConstants.QUESTION_MARK;
import static org.onosproject.yangutils.utils.UtilConstants.SEMI_COLAN;
import static org.onosproject.yangutils.utils.UtilConstants.SPACE;
import static org.onosproject.yangutils.utils.UtilConstants.TYPE;
import static org.onosproject.yangutils.utils.UtilConstants.UINT_MAX_RANGE_ATTR;
import static org.onosproject.yangutils.utils.UtilConstants.UINT_MIN_RANGE_ATTR;
import static org.onosproject.yangutils.utils.UtilConstants.ULONG_MAX_RANGE_ATTR;
import static org.onosproject.yangutils.utils.UtilConstants.ULONG_MIN_RANGE_ATTR;
import static org.onosproject.yangutils.utils.UtilConstants.YANG_AUGMENTED_INFO;
import static org.onosproject.yangutils.utils.io.impl.JavaDocGen.JavaDocType.ENUM_ATTRIBUTE;
import static org.onosproject.yangutils.utils.io.impl.JavaDocGen.getJavaDoc;
import static org.onosproject.yangutils.utils.io.impl.YangIoUtils.getSmallCase;
import static java.util.Collections.sort;

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

        /*
         * TODO return the file header.
         */
        return null;
    }

    /**
     * Returns the textual java code information corresponding to the import list.
     *
     * @param importInfo import info
     * @return the textual java code information corresponding to the import list
     */
    static String getImportText(JavaQualifiedTypeInfoTranslator importInfo) {
        return IMPORT + importInfo.getPkgInfo() + PERIOD + importInfo.getClassInfo() + SEMI_COLAN + NEW_LINE;
    }

    /**
     * Returns the textual java code for attribute definition in class.
     *
     * @param javaAttributeTypePkg Package of the attribute type
     * @param javaAttributeType    java attribute type
     * @param javaAttributeName    name of the attribute
     * @param isList               is list attribute
     * @param attributeAccessType  attribute access type
     * @return the textual java code for attribute definition in class
     */
    public static String getJavaAttributeDefinition(String javaAttributeTypePkg, String javaAttributeType,
                                                    String javaAttributeName, boolean isList,
                                                    String attributeAccessType) {

        String attributeDefinition = attributeAccessType + SPACE;

        if (!isList) {
            if (javaAttributeTypePkg != null) {
                attributeDefinition = attributeDefinition + javaAttributeTypePkg + PERIOD;
            }

            attributeDefinition = attributeDefinition + javaAttributeType + SPACE + javaAttributeName + SEMI_COLAN
                    + NEW_LINE;
        } else {
            attributeDefinition = attributeDefinition + LIST + DIAMOND_OPEN_BRACKET;
            if (javaAttributeTypePkg != null) {
                attributeDefinition = attributeDefinition + javaAttributeTypePkg + PERIOD;
            }

            attributeDefinition = attributeDefinition + javaAttributeType + DIAMOND_CLOSE_BRACKET + SPACE
                    + javaAttributeName + SPACE + EQUAL + SPACE + NEW + SPACE + ARRAY_LIST + SEMI_COLAN + NEW_LINE;
        }
        return attributeDefinition;
    }

    /**
     * Returns based on the file type and the YANG name of the file, generate the class / interface definition close.
     *
     * @return corresponding textual java code information
     */
    public static String getJavaClassDefClose() {
        return CLOSE_CURLY_BRACKET;
    }

    /**
     * Returns string for enum's attribute.
     *
     * @param name         name of attribute
     * @param value        value of the enum
     * @param pluginConfig plugin configurations
     * @return string for enum's attribute
     */
    public static String generateEnumAttributeString(String name, int value, YangPluginConfig pluginConfig) {
        return NEW_LINE + getJavaDoc(ENUM_ATTRIBUTE, name, false, pluginConfig)
                + EIGHT_SPACE_INDENTATION + getEnumJavaAttribute(name).toUpperCase() + OPEN_PARENTHESIS
                + value + CLOSE_PARENTHESIS + COMMA + NEW_LINE;
    }

    /**
     * Returns sorted import list.
     *
     * @param imports import list
     * @return sorted import list
     */
    public static List<String> sortImports(List<String> imports) {
        sort(imports);
        return imports;
    }

    /**
     * Returns event enum start.
     *
     * @return event enum start
     */
    static String getEventEnumTypeStart() {
        return FOUR_SPACE_INDENTATION + PUBLIC + SPACE + ENUM + SPACE + TYPE + SPACE + OPEN_CURLY_BRACKET
                + NEW_LINE;
    }

    /**
     * Adds listener's imports.
     *
     * @param curNode   currentYangNode.
     * @param imports   import list
     * @param operation add or remove
     * @param classInfo class info to be added to import list
     */
    public static void addListenersImport(YangNode curNode, List<String> imports, boolean operation,
                                          String classInfo) {
        String thisImport;
        TempJavaServiceFragmentFiles tempJavaServiceFragmentFiles = ((JavaCodeGeneratorInfo) curNode)
                .getTempJavaCodeFragmentFiles().getServiceTempFiles();
        if (classInfo.equals(LISTENER_SERVICE)) {
            thisImport = tempJavaServiceFragmentFiles.getJavaImportData().getListenerServiceImport();
            performOperationOnImports(imports, thisImport, operation);
        } else {
            thisImport = tempJavaServiceFragmentFiles.getJavaImportData().getListenerRegistryImport();
            performOperationOnImports(imports, thisImport, operation);
        }
    }

    /**
     * Performs given operations on import list.
     *
     * @param imports   list of imports
     * @param curImport current import
     * @param operation add or remove
     * @return import list
     */
    private static List<String> performOperationOnImports(List<String> imports, String curImport,
                                                          boolean operation) {
        if (operation) {
            imports.add(curImport);
        } else {
            imports.remove(curImport);
        }
        sortImports(imports);
        return imports;
    }

    /**
     * Returns integer attribute for enum's class to get the values.
     *
     * @param className enum's class name
     * @return enum's attribute
     */
    static String getEnumsValueAttribute(String className) {
        return NEW_LINE + FOUR_SPACE_INDENTATION + PRIVATE + SPACE + INT + SPACE + getSmallCase(className)
                + SEMI_COLAN + NEW_LINE;
    }

    /**
     * Returns attribute for augmentation.
     *
     * @return attribute for augmentation
     */
    static String addAugmentationAttribute() {
        return NEW_LINE + FOUR_SPACE_INDENTATION + PRIVATE + SPACE + MAP + DIAMOND_OPEN_BRACKET + CLASS_STRING
                + DIAMOND_OPEN_BRACKET + QUESTION_MARK + DIAMOND_CLOSE_BRACKET + COMMA + SPACE + OBJECT_STRING
                + DIAMOND_CLOSE_BRACKET + SPACE + getSmallCase(YANG_AUGMENTED_INFO) + MAP + SPACE + EQUAL + SPACE +
                NEW + SPACE + HASH_MAP + DIAMOND_OPEN_BRACKET + DIAMOND_CLOSE_BRACKET + OPEN_PARENTHESIS
                + CLOSE_PARENTHESIS + SEMI_COLAN;
    }

    /**
     * Adds attribute for int ranges.
     *
     * @param modifier modifier for attribute
     * @param addFirst true if int need to be added fist.
     * @return attribute for int ranges
     */
    static String addStaticAttributeIntRange(String modifier, boolean addFirst) {
        if (addFirst) {
            return NEW_LINE + FOUR_SPACE_INDENTATION + modifier + SPACE + INT_MIN_RANGE_ATTR + FOUR_SPACE_INDENTATION +
                    modifier +
                    SPACE + INT_MAX_RANGE_ATTR;
        } else {
            return NEW_LINE + FOUR_SPACE_INDENTATION + modifier + SPACE + UINT_MIN_RANGE_ATTR + FOUR_SPACE_INDENTATION +
                    modifier + SPACE + UINT_MAX_RANGE_ATTR;
        }
    }

    /**
     * Adds attribute for long ranges.
     *
     * @param modifier modifier for attribute
     * @param addFirst if need to be added first
     * @return attribute for long ranges
     */
    static String addStaticAttributeLongRange(String modifier, boolean addFirst) {
        if (addFirst) {
            return NEW_LINE + FOUR_SPACE_INDENTATION + modifier + SPACE + LONG_MIN_RANGE_ATTR + FOUR_SPACE_INDENTATION +
                    modifier + SPACE + LONG_MAX_RANGE_ATTR;
        } else {
            return NEW_LINE + FOUR_SPACE_INDENTATION + modifier + SPACE + ULONG_MIN_RANGE_ATTR +
                    FOUR_SPACE_INDENTATION + modifier + SPACE + ULONG_MAX_RANGE_ATTR;
        }
    }

    /**
     * Returns operation type enum.
     *
     * @return operation type enum
     */
    static String getOperationTypeEnum() {
        return "\n" +
                "    /**\n" +
                "     * Specify the node specific operation in protocols like NETCONF.\n" +
                "     * Applicable in protocol edit operation, not applicable in query operation\n" +
                "     */\n" +
                "    public enum OperationType {\n" +
                "        MERGE,\n" +
                "        REPLACE,\n" +
                "        CREATE,\n" +
                "        DELETE,\n" +
                "        REMOVE\n" +
                "    }\n";
    }

    /**
     * Returns operation type enum, leaf value set attribute and select leaf attribute.
     *
     * @return operation type enum, leaf value set attribute and select leaf attribute.
     */
    static String getOperationAttributes() {
        return "    /**\n" +
                "     * Identify the leafs whose value are explicitly set\n" +
                "     * Applicable in protocol edit and query operation\n" +
                "     */\n" +
                "    private BitSet _valueLeafFlags = new BitSet();\n" +
                "\n" +
                "    /**\n" +
                "     * Identify the leafs to be selected, in a query operation\n" +
                "     */\n" +
                "    private BitSet _selectLeafFlags = new BitSet();\n";
    }

    /**
     * Returns operation type enum, leaf value set attribute and select leaf attribute.
     *
     * @return operation type enum, leaf value set attribute and select leaf attribute.
     */
    static String getOperationTypeAttr() {
        return "\n    /**\n" +
                "     * Specify the node specific operation in protocols like NETCONF.\n" +
                "     * Applicable in protocol edit operation, will be ignored in query operation\n" +
                "     */\n" +
                "    private OperationType _operationType;\n" +
                "\n";
    }

    /**
     * Returns operation type enum, leaf value set attribute and select leaf attribute for constructor.
     *
     * @return operation type enum, leaf value set attribute and select leaf attribute for constructor
     */
    static String getOperationAttributeForConstructor() {
        return "        this._valueLeafFlags = builderObject.get_valueLeafFlags();\n" +
                "        this._selectLeafFlags = builderObject.get_selectLeafFlags();\n";
    }

    /**
     * Returns operation type enum, leaf value set attribute and select leaf attribute for constructor.
     *
     * @return operation type enum, leaf value set attribute and select leaf attribute for constructor
     */
    static String getOperationTypeForConstructor() {
        return "        this._operationType = builderObject.get_operationType();\n";
    }

}

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
import java.util.Map;

import org.onosproject.yangutils.datamodel.YangAtomicPath;
import org.onosproject.yangutils.datamodel.YangAugmentableNode;
import org.onosproject.yangutils.datamodel.YangLeaf;
import org.onosproject.yangutils.datamodel.YangLeafList;
import org.onosproject.yangutils.datamodel.YangLeavesHolder;
import org.onosproject.yangutils.datamodel.YangNode;
import org.onosproject.yangutils.datamodel.YangType;
import org.onosproject.yangutils.datamodel.utils.builtindatatype.YangDataTypes;
import org.onosproject.yangutils.translator.exception.TranslatorException;
import org.onosproject.yangutils.translator.tojava.JavaAttributeInfo;
import org.onosproject.yangutils.translator.tojava.JavaCodeGeneratorInfo;
import org.onosproject.yangutils.translator.tojava.JavaFileInfoContainer;
import org.onosproject.yangutils.translator.tojava.TempJavaBeanFragmentFiles;
import org.onosproject.yangutils.utils.io.impl.JavaDocGen;
import org.onosproject.yangutils.utils.io.impl.YangPluginConfig;

import static org.onosproject.yangutils.datamodel.utils.builtindatatype.YangDataTypes.BINARY;
import static org.onosproject.yangutils.datamodel.utils.builtindatatype.YangDataTypes.BITS;
import static org.onosproject.yangutils.datamodel.utils.builtindatatype.YangDataTypes.BOOLEAN;
import static org.onosproject.yangutils.datamodel.utils.builtindatatype.YangDataTypes.DECIMAL64;
import static org.onosproject.yangutils.datamodel.utils.builtindatatype.YangDataTypes.EMPTY;
import static org.onosproject.yangutils.datamodel.utils.builtindatatype.YangDataTypes.INT16;
import static org.onosproject.yangutils.datamodel.utils.builtindatatype.YangDataTypes.INT32;
import static org.onosproject.yangutils.datamodel.utils.builtindatatype.YangDataTypes.INT64;
import static org.onosproject.yangutils.datamodel.utils.builtindatatype.YangDataTypes.INT8;
import static org.onosproject.yangutils.datamodel.utils.builtindatatype.YangDataTypes.UINT16;
import static org.onosproject.yangutils.datamodel.utils.builtindatatype.YangDataTypes.UINT32;
import static org.onosproject.yangutils.datamodel.utils.builtindatatype.YangDataTypes.UINT64;
import static org.onosproject.yangutils.datamodel.utils.builtindatatype.YangDataTypes.UINT8;
import static org.onosproject.yangutils.translator.tojava.GeneratedJavaFileType.GENERATE_EVENT_SUBJECT_CLASS;
import static org.onosproject.yangutils.translator.tojava.GeneratedJavaFileType.GENERATE_SERVICE_AND_MANAGER;
import static org.onosproject.yangutils.translator.tojava.TempJavaFragmentFiles.getJavaAttributeOfLeaf;
import static org.onosproject.yangutils.translator.tojava.TempJavaFragmentFiles.getJavaAttributeOfLeafList;
import static org.onosproject.yangutils.translator.tojava.utils.JavaFileGeneratorUtils.getAugmentedClassNameForDataMethods;
import static org.onosproject.yangutils.translator.tojava.utils.JavaFileGeneratorUtils.getParentNodeNameForDataMethods;
import static org.onosproject.yangutils.translator.tojava.utils.JavaFileGeneratorUtils.getSetOfNodeIdentifiers;
import static org.onosproject.yangutils.utils.UtilConstants.ACTIVATE;
import static org.onosproject.yangutils.utils.UtilConstants.ACTIVATE_ANNOTATION;
import static org.onosproject.yangutils.utils.UtilConstants.ADD_STRING;
import static org.onosproject.yangutils.utils.UtilConstants.AND;
import static org.onosproject.yangutils.utils.UtilConstants.APP_INSTANCE;
import static org.onosproject.yangutils.utils.UtilConstants.AUGMENTED;
import static org.onosproject.yangutils.utils.UtilConstants.AUGMENTED_INFO;
import static org.onosproject.yangutils.utils.UtilConstants.BASE64;
import static org.onosproject.yangutils.utils.UtilConstants.BASE_CLASS;
import static org.onosproject.yangutils.utils.UtilConstants.BIG_DECIMAL;
import static org.onosproject.yangutils.utils.UtilConstants.BIG_INTEGER;
import static org.onosproject.yangutils.utils.UtilConstants.BITSET;
import static org.onosproject.yangutils.utils.UtilConstants.BOOLEAN_DATA_TYPE;
import static org.onosproject.yangutils.utils.UtilConstants.BOOLEAN_WRAPPER;
import static org.onosproject.yangutils.utils.UtilConstants.BUILD;
import static org.onosproject.yangutils.utils.UtilConstants.BUILDER;
import static org.onosproject.yangutils.utils.UtilConstants.BYTE;
import static org.onosproject.yangutils.utils.UtilConstants.BYTE_WRAPPER;
import static org.onosproject.yangutils.utils.UtilConstants.CASE;
import static org.onosproject.yangutils.utils.UtilConstants.CATCH;
import static org.onosproject.yangutils.utils.UtilConstants.CHECK_NOT_NULL_STRING;
import static org.onosproject.yangutils.utils.UtilConstants.CLASS;
import static org.onosproject.yangutils.utils.UtilConstants.CLASS_STRING;
import static org.onosproject.yangutils.utils.UtilConstants.CLOSE_CURLY_BRACKET;
import static org.onosproject.yangutils.utils.UtilConstants.CLOSE_PARENTHESIS;
import static org.onosproject.yangutils.utils.UtilConstants.COLAN;
import static org.onosproject.yangutils.utils.UtilConstants.COLON;
import static org.onosproject.yangutils.utils.UtilConstants.COMMA;
import static org.onosproject.yangutils.utils.UtilConstants.DEACTIVATE;
import static org.onosproject.yangutils.utils.UtilConstants.DEACTIVATE_ANNOTATION;
import static org.onosproject.yangutils.utils.UtilConstants.DECODE;
import static org.onosproject.yangutils.utils.UtilConstants.DEFAULT;
import static org.onosproject.yangutils.utils.UtilConstants.DIAMOND_CLOSE_BRACKET;
import static org.onosproject.yangutils.utils.UtilConstants.DIAMOND_OPEN_BRACKET;
import static org.onosproject.yangutils.utils.UtilConstants.DOUBLE;
import static org.onosproject.yangutils.utils.UtilConstants.EIGHT_SPACE_INDENTATION;
import static org.onosproject.yangutils.utils.UtilConstants.EMPTY_STRING;
import static org.onosproject.yangutils.utils.UtilConstants.EQUAL;
import static org.onosproject.yangutils.utils.UtilConstants.EQUALS_STRING;
import static org.onosproject.yangutils.utils.UtilConstants.EXCEPTION;
import static org.onosproject.yangutils.utils.UtilConstants.EXCEPTION_VAR;
import static org.onosproject.yangutils.utils.UtilConstants.FALSE;
import static org.onosproject.yangutils.utils.UtilConstants.FILTER_CONTENT_MATCH;
import static org.onosproject.yangutils.utils.UtilConstants.FILTER_LEAF;
import static org.onosproject.yangutils.utils.UtilConstants.FILTER_LEAF_LIST;
import static org.onosproject.yangutils.utils.UtilConstants.FOR;
import static org.onosproject.yangutils.utils.UtilConstants.FOUR_SPACE_INDENTATION;
import static org.onosproject.yangutils.utils.UtilConstants.FROM_STRING_METHOD_NAME;
import static org.onosproject.yangutils.utils.UtilConstants.FROM_STRING_PARAM_NAME;
import static org.onosproject.yangutils.utils.UtilConstants.GET;
import static org.onosproject.yangutils.utils.UtilConstants.GET_DECODER;
import static org.onosproject.yangutils.utils.UtilConstants.GET_FILTER_LEAF;
import static org.onosproject.yangutils.utils.UtilConstants.GET_FILTER_LEAF_LIST;
import static org.onosproject.yangutils.utils.UtilConstants.GET_METHOD_PREFIX;
import static org.onosproject.yangutils.utils.UtilConstants.GET_OPERATION_TYPE;
import static org.onosproject.yangutils.utils.UtilConstants.GET_SELECT_LEAF;
import static org.onosproject.yangutils.utils.UtilConstants.GET_SELECT_LEAF_LIST;
import static org.onosproject.yangutils.utils.UtilConstants.GOOGLE_MORE_OBJECT_METHOD_STRING;
import static org.onosproject.yangutils.utils.UtilConstants.HASH;
import static org.onosproject.yangutils.utils.UtilConstants.HASH_CODE_STRING;
import static org.onosproject.yangutils.utils.UtilConstants.IF;
import static org.onosproject.yangutils.utils.UtilConstants.ILLEGAL_ARGUMENT_EXCEPTION;
import static org.onosproject.yangutils.utils.UtilConstants.ILLEGAL_ARGUMENT_EXCEPTION_MSG;
import static org.onosproject.yangutils.utils.UtilConstants.INSTANCE_OF;
import static org.onosproject.yangutils.utils.UtilConstants.INT;
import static org.onosproject.yangutils.utils.UtilConstants.INTEGER_WRAPPER;
import static org.onosproject.yangutils.utils.UtilConstants.LIST;
import static org.onosproject.yangutils.utils.UtilConstants.LONG;
import static org.onosproject.yangutils.utils.UtilConstants.LONG_WRAPPER;
import static org.onosproject.yangutils.utils.UtilConstants.MAP;
import static org.onosproject.yangutils.utils.UtilConstants.NEW;
import static org.onosproject.yangutils.utils.UtilConstants.NEW_LINE;
import static org.onosproject.yangutils.utils.UtilConstants.NOT;
import static org.onosproject.yangutils.utils.UtilConstants.NULL;
import static org.onosproject.yangutils.utils.UtilConstants.OBJ;
import static org.onosproject.yangutils.utils.UtilConstants.OBJECT;
import static org.onosproject.yangutils.utils.UtilConstants.OBJECT_STRING;
import static org.onosproject.yangutils.utils.UtilConstants.OF;
import static org.onosproject.yangutils.utils.UtilConstants.OMIT_NULL_VALUE_STRING;
import static org.onosproject.yangutils.utils.UtilConstants.OPEN_CURLY_BRACKET;
import static org.onosproject.yangutils.utils.UtilConstants.OPEN_PARENTHESIS;
import static org.onosproject.yangutils.utils.UtilConstants.OPERATION;
import static org.onosproject.yangutils.utils.UtilConstants.OPERATION_ENUM;
import static org.onosproject.yangutils.utils.UtilConstants.OP_PARAM_TYPE;
import static org.onosproject.yangutils.utils.UtilConstants.OR_OPERATION;
import static org.onosproject.yangutils.utils.UtilConstants.OTHER;
import static org.onosproject.yangutils.utils.UtilConstants.OVERRIDE;
import static org.onosproject.yangutils.utils.UtilConstants.PARSE_BOOLEAN;
import static org.onosproject.yangutils.utils.UtilConstants.PARSE_BYTE;
import static org.onosproject.yangutils.utils.UtilConstants.PARSE_INT;
import static org.onosproject.yangutils.utils.UtilConstants.PARSE_LONG;
import static org.onosproject.yangutils.utils.UtilConstants.PARSE_SHORT;
import static org.onosproject.yangutils.utils.UtilConstants.PERIOD;
import static org.onosproject.yangutils.utils.UtilConstants.PUBLIC;
import static org.onosproject.yangutils.utils.UtilConstants.PUT;
import static org.onosproject.yangutils.utils.UtilConstants.QUESTION_MARK;
import static org.onosproject.yangutils.utils.UtilConstants.QUOTES;
import static org.onosproject.yangutils.utils.UtilConstants.RECEIVED_OBJECT;
import static org.onosproject.yangutils.utils.UtilConstants.REPLACE_STRING;
import static org.onosproject.yangutils.utils.UtilConstants.RETURN;
import static org.onosproject.yangutils.utils.UtilConstants.RPC_INPUT_VAR_NAME;
import static org.onosproject.yangutils.utils.UtilConstants.SELECT_LEAF;
import static org.onosproject.yangutils.utils.UtilConstants.SELECT_LEAF_LIST;
import static org.onosproject.yangutils.utils.UtilConstants.SEMI_COLAN;
import static org.onosproject.yangutils.utils.UtilConstants.SET_METHOD_PREFIX;
import static org.onosproject.yangutils.utils.UtilConstants.SET_OPERATION_TYPE;
import static org.onosproject.yangutils.utils.UtilConstants.SHORT;
import static org.onosproject.yangutils.utils.UtilConstants.SHORT_WRAPPER;
import static org.onosproject.yangutils.utils.UtilConstants.SINGLE_QUOTE;
import static org.onosproject.yangutils.utils.UtilConstants.SIXTEEN_SPACE_INDENTATION;
import static org.onosproject.yangutils.utils.UtilConstants.SPACE;
import static org.onosproject.yangutils.utils.UtilConstants.SPLIT_STRING;
import static org.onosproject.yangutils.utils.UtilConstants.SQUARE_BRACKETS;
import static org.onosproject.yangutils.utils.UtilConstants.STARTED_LOG_INFO;
import static org.onosproject.yangutils.utils.UtilConstants.STATIC;
import static org.onosproject.yangutils.utils.UtilConstants.STOPPED_LOG_INFO;
import static org.onosproject.yangutils.utils.UtilConstants.STRING_DATA_TYPE;
import static org.onosproject.yangutils.utils.UtilConstants.SUFFIX_S;
import static org.onosproject.yangutils.utils.UtilConstants.SWITCH;
import static org.onosproject.yangutils.utils.UtilConstants.THIS;
import static org.onosproject.yangutils.utils.UtilConstants.THROW;
import static org.onosproject.yangutils.utils.UtilConstants.TMP_VAL;
import static org.onosproject.yangutils.utils.UtilConstants.TO;
import static org.onosproject.yangutils.utils.UtilConstants.TRIM_STRING;
import static org.onosproject.yangutils.utils.UtilConstants.TRUE;
import static org.onosproject.yangutils.utils.UtilConstants.TRY;
import static org.onosproject.yangutils.utils.UtilConstants.TWELVE_SPACE_INDENTATION;
import static org.onosproject.yangutils.utils.UtilConstants.VALUE;
import static org.onosproject.yangutils.utils.UtilConstants.VOID;
import static org.onosproject.yangutils.utils.UtilConstants.YANG_AUGMENTED_INFO;
import static org.onosproject.yangutils.utils.UtilConstants.YANG_AUGMENTED_OP_PARAM_INFO;
import static org.onosproject.yangutils.utils.UtilConstants.YANG_UTILS_TODO;
import static org.onosproject.yangutils.utils.UtilConstants.ZERO;
import static org.onosproject.yangutils.utils.io.impl.JavaDocGen.JavaDocType.BUILD_METHOD;
import static org.onosproject.yangutils.utils.io.impl.JavaDocGen.JavaDocType.CONSTRUCTOR;
import static org.onosproject.yangutils.utils.io.impl.JavaDocGen.JavaDocType.DEFAULT_CONSTRUCTOR;
import static org.onosproject.yangutils.utils.io.impl.JavaDocGen.JavaDocType.FROM_METHOD;
import static org.onosproject.yangutils.utils.io.impl.JavaDocGen.JavaDocType.GETTER_METHOD;
import static org.onosproject.yangutils.utils.io.impl.JavaDocGen.JavaDocType.MANAGER_SETTER_METHOD;
import static org.onosproject.yangutils.utils.io.impl.JavaDocGen.JavaDocType.OF_METHOD;
import static org.onosproject.yangutils.utils.io.impl.JavaDocGen.JavaDocType.SETTER_METHOD;
import static org.onosproject.yangutils.utils.io.impl.JavaDocGen.JavaDocType.TYPE_CONSTRUCTOR;
import static org.onosproject.yangutils.utils.io.impl.JavaDocGen.generateForAddAugmentation;
import static org.onosproject.yangutils.utils.io.impl.JavaDocGen.generateForGetAugmentation;
import static org.onosproject.yangutils.utils.io.impl.JavaDocGen.getJavaDoc;
import static org.onosproject.yangutils.utils.io.impl.YangIoUtils.getCamelCase;
import static org.onosproject.yangutils.utils.io.impl.YangIoUtils.getCapitalCase;
import static org.onosproject.yangutils.utils.io.impl.YangIoUtils.getSmallCase;
import static org.onosproject.yangutils.utils.io.impl.YangIoUtils.trimAtLast;

/**
 * Represents generator for methods of generated files based on the file type.
 */
public final class MethodsGenerator {
    private static final String BITS_STRING_ARRAY_VAR = "bitsTemp";
    private static final String BIT_TEMP_VAR = "bitTemp";

    /**
     * Creates an instance of method generator.
     */
    private MethodsGenerator() {
    }

    /**
     * Returns the methods strings for builder interface.
     *
     * @param name         attribute name
     * @param pluginConfig plugin configurations
     * @return method string for builder interface
     */
    public static String parseBuilderInterfaceBuildMethodString(String name, YangPluginConfig pluginConfig) {
        return getJavaDoc(BUILD_METHOD, name, false, pluginConfig) + getBuildForInterface(name);
    }

    /**
     * Returns getter string.
     *
     * @param attr               attribute info
     * @param generatedJavaFiles generated java files
     * @param pluginConfig       plugin configurations
     * @return getter string
     */
    public static String getGetterString(JavaAttributeInfo attr, int generatedJavaFiles,
                                         YangPluginConfig pluginConfig) {

        String returnType = getReturnType(attr);
        String attributeName = attr.getAttributeName();

        return getJavaDoc(GETTER_METHOD, attributeName, attr.isListAttr(), pluginConfig)
                + getGetterForInterface(attributeName, returnType, attr.isListAttr(), generatedJavaFiles);
    }

    /**
     * Returns setter string.
     *
     * @param attr               attribute info
     * @param className          java class name
     * @param generatedJavaFiles generated java files
     * @param pluginConfig       plugin configurations
     * @return setter string
     */
    public static String getSetterString(JavaAttributeInfo attr, String className, int generatedJavaFiles,
                                         YangPluginConfig pluginConfig) {

        String attrType = getReturnType(attr);
        String attributeName = attr.getAttributeName();
        JavaDocGen.JavaDocType type;
        if ((generatedJavaFiles & GENERATE_SERVICE_AND_MANAGER) != 0) {
            type = MANAGER_SETTER_METHOD;
        } else {
            type = SETTER_METHOD;
        }

        return getJavaDoc(type, attributeName, attr.isListAttr(), pluginConfig)
                + getSetterForInterface(attributeName, attrType, className, attr.isListAttr(), generatedJavaFiles);
    }

    /**
     * Returns constructor method string.
     *
     * @param name         class name
     * @param pluginConfig plugin configurations
     * @return constructor string
     */
    public static String getConstructorString(String name, YangPluginConfig pluginConfig) {
        return getJavaDoc(CONSTRUCTOR, name, false, pluginConfig);
    }

    /**
     * Returns default constructor method string.
     *
     * @param name         class name
     * @param modifierType modifier type
     * @param pluginConfig plugin configurations
     * @return default constructor string
     */
    public static String getDefaultConstructorString(String name, String modifierType,
                                                     YangPluginConfig pluginConfig) {
        return getJavaDoc(DEFAULT_CONSTRUCTOR, name, false, pluginConfig)
                + getDefaultConstructor(name, modifierType)
                + NEW_LINE;
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
     * @param attr               attribute info
     * @param generatedJavaFiles for the type of java file being generated
     * @return getter method for class
     */
    public static String getGetterForClass(JavaAttributeInfo attr, int generatedJavaFiles) {

        String attrQuaifiedType = getReturnType(attr);
        String attributeName = attr.getAttributeName();

        if (!attr.isListAttr()) {
            return getGetter(attrQuaifiedType, attributeName, generatedJavaFiles);
        }
        String listAttr = getListString() + attrQuaifiedType + DIAMOND_CLOSE_BRACKET;
        return getGetter(listAttr, attributeName, generatedJavaFiles);
    }

    /**
     * Returns getter for attribute.
     *
     * @param type               return type
     * @param name               attribute name
     * @param generatedJavaFiles generated java files
     * @return getter for attribute
     */
    public static String getGetter(String type, String name, int generatedJavaFiles) {
        String ret = parseTypeForReturnValue(type);
        if ((generatedJavaFiles & GENERATE_SERVICE_AND_MANAGER) != 0) {
            return FOUR_SPACE_INDENTATION + PUBLIC + SPACE + type + SPACE + GET_METHOD_PREFIX + getCapitalCase(name)
                    + OPEN_PARENTHESIS + CLOSE_PARENTHESIS + SPACE + OPEN_CURLY_BRACKET + NEW_LINE +
                    EIGHT_SPACE_INDENTATION + YANG_UTILS_TODO + NEW_LINE + EIGHT_SPACE_INDENTATION +
                    RETURN + SPACE + ret + SEMI_COLAN + NEW_LINE + FOUR_SPACE_INDENTATION + CLOSE_CURLY_BRACKET;
        } else {
            return FOUR_SPACE_INDENTATION + PUBLIC + SPACE + type + SPACE + name
                    + OPEN_PARENTHESIS + CLOSE_PARENTHESIS + SPACE + OPEN_CURLY_BRACKET + NEW_LINE +
                    EIGHT_SPACE_INDENTATION + RETURN + SPACE + name + SEMI_COLAN + NEW_LINE + FOUR_SPACE_INDENTATION
                    + CLOSE_CURLY_BRACKET;
        }

    }

    /*Provides string to return for type.*/
    private static String parseTypeForReturnValue(String type) {
        switch (type) {
            case BYTE:
            case INT:
            case SHORT:
            case LONG:
            case DOUBLE:
                return "0";
            case BOOLEAN_DATA_TYPE:
                return FALSE;
            default:
                return null;
        }
    }

    /**
     * Returns the setter method strings for class file.
     *
     * @param attr               attribute info
     * @param className          name of the class
     * @param generatedJavaFiles generated java files
     * @return setter method for class
     */
    public static String getSetterForClass(JavaAttributeInfo attr, String className, int generatedJavaFiles) {

        String attrQuaifiedType = getReturnType(attr);
        String attributeName = attr.getAttributeName();
        if (!attr.isListAttr()) {
            return getSetter(className, attributeName, attrQuaifiedType, generatedJavaFiles);
        }
        String listAttr = getListString() + attrQuaifiedType + DIAMOND_CLOSE_BRACKET;
        return getSetter(className, attributeName, listAttr, generatedJavaFiles);
    }

    /**
     * Returns setter for attribute.
     *
     * @param className class name
     * @param name      attribute name
     * @param type      return type
     * @return setter for attribute
     */
    private static String getSetter(String className, String name, String type, int generatedJavaFiles) {
        if ((generatedJavaFiles & GENERATE_SERVICE_AND_MANAGER) != 0) {
            return FOUR_SPACE_INDENTATION + PUBLIC + SPACE + VOID + SPACE + SET_METHOD_PREFIX
                    + getCapitalCase(name) + OPEN_PARENTHESIS + type + SPACE + name + CLOSE_PARENTHESIS + SPACE +
                    OPEN_CURLY_BRACKET + NEW_LINE + EIGHT_SPACE_INDENTATION + YANG_UTILS_TODO +
                    NEW_LINE + FOUR_SPACE_INDENTATION + CLOSE_CURLY_BRACKET;
        } else if (generatedJavaFiles == GENERATE_EVENT_SUBJECT_CLASS) {
            return FOUR_SPACE_INDENTATION + PUBLIC + SPACE + VOID + SPACE + name + OPEN_PARENTHESIS + type + SPACE
                    + name + CLOSE_PARENTHESIS + SPACE + OPEN_CURLY_BRACKET + NEW_LINE + EIGHT_SPACE_INDENTATION
                    + THIS + PERIOD + name + SPACE + EQUAL + SPACE + name + SEMI_COLAN + NEW_LINE
                    + FOUR_SPACE_INDENTATION + CLOSE_CURLY_BRACKET;
        } else {
            return FOUR_SPACE_INDENTATION + PUBLIC + SPACE + className + BUILDER + SPACE +
                    name + OPEN_PARENTHESIS + type + SPACE + name + CLOSE_PARENTHESIS + SPACE
                    + OPEN_CURLY_BRACKET + NEW_LINE + EIGHT_SPACE_INDENTATION + THIS + PERIOD + name + SPACE
                    + EQUAL + SPACE + name + SEMI_COLAN + NEW_LINE + EIGHT_SPACE_INDENTATION + RETURN + SPACE
                    + THIS + SEMI_COLAN + NEW_LINE + FOUR_SPACE_INDENTATION + CLOSE_CURLY_BRACKET;
        }
    }

    /**
     * Returns the setter method strings for class file.
     *
     * @param attr attribute info
     * @return setter method for class
     */
    public static String getSetterForTypeDefClass(JavaAttributeInfo attr) {

        String attrQuaifiedType = getReturnType(attr);
        String attributeName = attr.getAttributeName();
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
        return FOUR_SPACE_INDENTATION + PUBLIC + SPACE + VOID + SPACE + SET_METHOD_PREFIX + getCapitalCase(name)
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
     * @param yangName           name of the attribute
     * @param returnType         return type of attribute
     * @param isList             is list attribute
     * @param generatedJavaFiles generated java files
     * @return getter method for interface
     */
    public static String getGetterForInterface(String yangName, String returnType, boolean isList,
                                               int generatedJavaFiles) {

        if (!isList) {
            return getGetterInterfaceString(returnType, yangName, generatedJavaFiles);
        }
        String listAttr = getListString() + returnType + DIAMOND_CLOSE_BRACKET;
        return getGetterInterfaceString(listAttr, yangName, generatedJavaFiles);
    }

    /**
     * Returns getter for attribute in interface.
     *
     * @param returnType return type
     * @param yangName   attribute name
     * @return getter for interface
     */
    private static String getGetterInterfaceString(String returnType, String yangName,
                                                   int generatedJavaFiles) {
        if ((generatedJavaFiles & GENERATE_SERVICE_AND_MANAGER) != 0) {
            return FOUR_SPACE_INDENTATION + returnType + SPACE + GET_METHOD_PREFIX + getCapitalCase(yangName)
                    + OPEN_PARENTHESIS + CLOSE_PARENTHESIS + SEMI_COLAN;
        } else {
            return FOUR_SPACE_INDENTATION + returnType + SPACE + yangName
                    + OPEN_PARENTHESIS + CLOSE_PARENTHESIS + SEMI_COLAN;
        }
    }

    /**
     * Returns the setter method strings for interface file.
     *
     * @param attrName           name of the attribute
     * @param attrType           return type of attribute
     * @param className          name of the java class being generated
     * @param isList             is list attribute
     * @param generatedJavaFiles generated java files
     * @return setter method for interface
     */
    public static String getSetterForInterface(String attrName, String attrType, String className,
                                               boolean isList, int generatedJavaFiles) {

        if (!isList) {
            return getSetterInterfaceString(className, attrName, attrType, generatedJavaFiles);
        }
        String listAttr = getListString() + attrType + DIAMOND_CLOSE_BRACKET;
        return getSetterInterfaceString(className, attrName, listAttr, generatedJavaFiles);
    }

    /**
     * Returns setter string for interface.
     *
     * @param className class name
     * @param attrName  attribute name
     * @param attrType  attribute type
     * @return setter string
     */
    private static String getSetterInterfaceString(String className, String attrName, String attrType,
                                                   int generatedJavaFiles) {
        if ((generatedJavaFiles & GENERATE_SERVICE_AND_MANAGER) != 0) {

            return FOUR_SPACE_INDENTATION + VOID + SPACE + SET_METHOD_PREFIX + getCapitalCase(attrName)
                    + OPEN_PARENTHESIS + attrType + SPACE + attrName + CLOSE_PARENTHESIS + SEMI_COLAN;
        } else {
            return FOUR_SPACE_INDENTATION + className + BUILDER + SPACE + attrName
                    + OPEN_PARENTHESIS + attrType + SPACE + attrName + CLOSE_PARENTHESIS + SEMI_COLAN;
        }
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
        if (attr.isQualifiedName() && attr.getImportInfo().getPkgInfo() != null) {
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
     * @param yangName     class name
     * @param pluginConfig plugin configurations
     * @return constructor string
     */
    public static String getConstructorStart(String yangName, YangPluginConfig pluginConfig) {

        String javadoc = getConstructorString(yangName, pluginConfig);
        String constructor = FOUR_SPACE_INDENTATION + PUBLIC + SPACE + getCapitalCase(DEFAULT) + yangName +
                OPEN_PARENTHESIS + yangName + BUILDER + SPACE + BUILDER.toLowerCase() + OBJECT
                + CLOSE_PARENTHESIS + SPACE + OPEN_CURLY_BRACKET + NEW_LINE;
        return javadoc + constructor;
    }

    /**
     * Returns constructor string for op param class.
     *
     * @param yangName     class name
     * @param pluginConfig plugin configurations
     * @return constructor string
     */
    public static String getOpParamConstructorStart(String yangName, YangPluginConfig pluginConfig) {

        String javadoc = getConstructorString(yangName, pluginConfig);
        String constructor = FOUR_SPACE_INDENTATION + PUBLIC + SPACE + yangName + OPERATION +
                OPEN_PARENTHESIS + yangName + OPERATION + BUILDER + SPACE + BUILDER.toLowerCase() + OBJECT
                + CLOSE_PARENTHESIS + SPACE + OPEN_CURLY_BRACKET + NEW_LINE;
        return javadoc + constructor;
    }

    /**
     * Returns the constructor strings for class file.
     *
     * @param attr               attribute info
     * @param generatedJavaFiles generated java files
     * @param pluginConfig       plugin configurations
     * @return constructor for class
     */
    public static String getConstructor(JavaAttributeInfo attr, int generatedJavaFiles,
                                        YangPluginConfig pluginConfig) {

        String attributeName = attr.getAttributeName();
        String constructor;

        if ((generatedJavaFiles & GENERATE_SERVICE_AND_MANAGER) != 0) {
            constructor = EIGHT_SPACE_INDENTATION + THIS + PERIOD
                    + getCamelCase(attributeName, pluginConfig.getConflictResolver()) + SPACE + EQUAL
                    + SPACE + BUILDER.toLowerCase() + OBJECT + PERIOD + GET_METHOD_PREFIX
                    + getCapitalCase(getCamelCase(attributeName, pluginConfig.getConflictResolver()))
                    + OPEN_PARENTHESIS + CLOSE_PARENTHESIS + SEMI_COLAN + NEW_LINE;
        } else {
            constructor = EIGHT_SPACE_INDENTATION + THIS + PERIOD
                    + getCamelCase(attributeName, pluginConfig.getConflictResolver()) + SPACE + EQUAL
                    + SPACE + BUILDER.toLowerCase() + OBJECT + PERIOD
                    + getCamelCase(attributeName, pluginConfig.getConflictResolver()) +
                    OPEN_PARENTHESIS + CLOSE_PARENTHESIS + SEMI_COLAN + NEW_LINE;
        }
        return constructor;
    }

    /**
     * Returns the rpc strings for service interface.
     *
     * @param rpcName      name of the rpc
     * @param inputName    name of input
     * @param outputName   name of output
     * @param pluginConfig plugin configurations
     * @return rpc method string
     */
    public static String getRpcServiceMethod(String rpcName, String inputName, String outputName,
                                             YangPluginConfig pluginConfig) {

        rpcName = getCamelCase(rpcName, pluginConfig.getConflictResolver());
        if (!inputName.equals(EMPTY_STRING)) {
            inputName = inputName + SPACE + RPC_INPUT_VAR_NAME;
        }
        return FOUR_SPACE_INDENTATION + outputName + SPACE + rpcName + OPEN_PARENTHESIS + inputName
                + CLOSE_PARENTHESIS + SEMI_COLAN;
    }

    /**
     * Returns the rpc strings for manager impl.
     *
     * @param rpcName      name of the rpc
     * @param inputName    name of input
     * @param outputName   name of output
     * @param pluginConfig plugin configurations
     * @return rpc method string
     */
    public static String getRpcManagerMethod(String rpcName, String inputName, String outputName,
                                             YangPluginConfig pluginConfig) {

        rpcName = getCamelCase(rpcName, pluginConfig.getConflictResolver());
        if (!inputName.equals(EMPTY_STRING)) {
            inputName = inputName + SPACE + RPC_INPUT_VAR_NAME;
        }

        String method = getOverRideString() + FOUR_SPACE_INDENTATION + PUBLIC + SPACE + outputName + SPACE + rpcName
                + OPEN_PARENTHESIS + inputName + CLOSE_PARENTHESIS + SPACE
                + OPEN_CURLY_BRACKET + NEW_LINE + EIGHT_SPACE_INDENTATION + YANG_UTILS_TODO + NEW_LINE;
        if (!outputName.contentEquals(VOID)) {
            method += EIGHT_SPACE_INDENTATION + RETURN + SPACE + parseTypeForReturnValue(outputName) + SEMI_COLAN
                    + NEW_LINE;
        }
        method += FOUR_SPACE_INDENTATION + CLOSE_CURLY_BRACKET;

        return method;
    }

    /**
     * Returns the build method strings for class file.
     *
     * @param yangName class name
     * @return build method string for class
     */
    public static String getBuild(String yangName) {
        return FOUR_SPACE_INDENTATION + PUBLIC + SPACE + yangName + SPACE + BUILD + OPEN_PARENTHESIS + CLOSE_PARENTHESIS
                + SPACE + OPEN_CURLY_BRACKET + NEW_LINE + EIGHT_SPACE_INDENTATION + RETURN + SPACE + NEW + SPACE +
                getCapitalCase(DEFAULT) + yangName + OPEN_PARENTHESIS + THIS + CLOSE_PARENTHESIS + SEMI_COLAN
                + NEW_LINE + FOUR_SPACE_INDENTATION + CLOSE_CURLY_BRACKET;
    }

    /**
     * Returns the Default constructor strings for class file.
     *
     * @param name         name of the class
     * @param modifierType modifier type for default constructor
     * @return Default constructor for class
     */
    private static String getDefaultConstructor(String name, String modifierType) {
        return FOUR_SPACE_INDENTATION + modifierType + SPACE + name + OPEN_PARENTHESIS + CLOSE_PARENTHESIS + SPACE
                + OPEN_CURLY_BRACKET + NEW_LINE + FOUR_SPACE_INDENTATION + CLOSE_CURLY_BRACKET;
    }

    /**
     * Returns to string method's open strings.
     *
     * @return string method's open string
     */
    public static String getToStringMethodOpen() {
        return getOverRideString() + FOUR_SPACE_INDENTATION + PUBLIC + SPACE + STRING_DATA_TYPE + SPACE + TO
                + STRING_DATA_TYPE + OPEN_PARENTHESIS + CLOSE_PARENTHESIS + SPACE + OPEN_CURLY_BRACKET + NEW_LINE
                + EIGHT_SPACE_INDENTATION + RETURN + GOOGLE_MORE_OBJECT_METHOD_STRING + NEW_LINE;
    }

    /**
     * Returns to string leaf for get method.
     *
     * @return string leaf for get method
     */
    public static String getToStringLeafgetter() {
        return NEW_LINE + NEW_LINE + FOUR_SPACE_INDENTATION + PUBLIC + SPACE + BITSET + SPACE + GET_FILTER_LEAF
                + OPEN_PARENTHESIS + CLOSE_PARENTHESIS + SPACE + OPEN_CURLY_BRACKET + NEW_LINE
                + EIGHT_SPACE_INDENTATION + RETURN + SPACE + FILTER_LEAF + SEMI_COLAN + NEW_LINE
                + FOUR_SPACE_INDENTATION + CLOSE_CURLY_BRACKET;
    }

    /**
     * Returns to string select leaf for get method.
     *
     * @return select leaf string for get method
     */
    public static String getToStringSelectLeafgetter() {
        return NEW_LINE + NEW_LINE + FOUR_SPACE_INDENTATION + PUBLIC + SPACE + BITSET + SPACE + GET_SELECT_LEAF
                + OPEN_PARENTHESIS + CLOSE_PARENTHESIS + SPACE + OPEN_CURLY_BRACKET + NEW_LINE
                + EIGHT_SPACE_INDENTATION + RETURN + SPACE + SELECT_LEAF + SEMI_COLAN + NEW_LINE
                + FOUR_SPACE_INDENTATION + CLOSE_CURLY_BRACKET;
    }

    /**
     * Returns to string leaf list for get method.
     *
     * @return string leaf list for get method
     */
    public static String getToStringLeafListgetter() {
        return NEW_LINE + NEW_LINE + FOUR_SPACE_INDENTATION + PUBLIC + SPACE + BITSET + SPACE + GET_FILTER_LEAF_LIST
                + OPEN_PARENTHESIS + CLOSE_PARENTHESIS + SPACE + OPEN_CURLY_BRACKET + NEW_LINE
                + EIGHT_SPACE_INDENTATION + RETURN + SPACE + FILTER_LEAF_LIST + SEMI_COLAN + NEW_LINE
                + FOUR_SPACE_INDENTATION + CLOSE_CURLY_BRACKET;
    }

    /**
     * Returns to string select leaf list for get.
     *
     * @return select leaf list list for get
     */
    public static String getToStringSelectLeafListgetter() {
        return NEW_LINE + NEW_LINE + FOUR_SPACE_INDENTATION + PUBLIC + SPACE + BITSET + SPACE + GET_SELECT_LEAF_LIST
                + OPEN_PARENTHESIS + CLOSE_PARENTHESIS + SPACE + OPEN_CURLY_BRACKET + NEW_LINE
                + EIGHT_SPACE_INDENTATION + RETURN + SPACE + SELECT_LEAF_LIST + SEMI_COLAN + NEW_LINE
                + FOUR_SPACE_INDENTATION + CLOSE_CURLY_BRACKET;
    }

    /**
     * Returns to string operation type for get method.
     *
     * @return operation type for get method
     */
    public static String getOperationTypegetter() {
        return NEW_LINE + NEW_LINE + FOUR_SPACE_INDENTATION + PUBLIC + SPACE + OPERATION_ENUM
                + SPACE + GET_OPERATION_TYPE + OPEN_PARENTHESIS + CLOSE_PARENTHESIS + SPACE
                + OPEN_CURLY_BRACKET + NEW_LINE + EIGHT_SPACE_INDENTATION + RETURN + SPACE
                + OP_PARAM_TYPE + SEMI_COLAN + NEW_LINE + FOUR_SPACE_INDENTATION + CLOSE_CURLY_BRACKET;
    }

    /**
     * Returns to string operation type for set.
     *
     * @return operation type set method
     */
    public static String getOperationTypeSetter() {
        return NEW_LINE + NEW_LINE + FOUR_SPACE_INDENTATION + PUBLIC + SPACE + VOID + SPACE + SET_OPERATION_TYPE
                + OPEN_PARENTHESIS + OPERATION_ENUM + SPACE + getSmallCase(OPERATION_ENUM) + CLOSE_PARENTHESIS
                + SPACE + OPEN_CURLY_BRACKET + NEW_LINE + EIGHT_SPACE_INDENTATION + THIS + PERIOD
                + OP_PARAM_TYPE + SPACE + EQUAL + SPACE + getSmallCase(OPERATION_ENUM) + SEMI_COLAN + NEW_LINE
                + FOUR_SPACE_INDENTATION + CLOSE_CURLY_BRACKET + NEW_LINE;
    }

    /**
     * Returns string for leaf set method.
     *
     * @param className    class name
     * @param curnode      current YANG node
     * @param pluginConfig plugin configurations
     * @return string for leaf set method
     */
    public static String getSetterForLeaf(String className, YangNode curnode, YangPluginConfig pluginConfig) {

        int numleaf = 1;
        String filterMethod = "";
        TempJavaBeanFragmentFiles tempFragmentFiles = ((JavaCodeGeneratorInfo) curnode)
                .getTempJavaCodeFragmentFiles().getBeanTempFiles();
        if (curnode instanceof YangLeavesHolder) {
            YangLeavesHolder leavesHolder = (YangLeavesHolder) curnode;
            List<YangLeaf> leaves = leavesHolder.getListOfLeaf();
            if (leaves !=  null && !leaves.isEmpty()) {
                for (YangLeaf leaf : leaves) {
                    JavaAttributeInfo javaAttributeInfo = getJavaAttributeOfLeaf(tempFragmentFiles, leaf, pluginConfig);
                    String attrQuaifiedType = getReturnType(javaAttributeInfo);
                    String attributeName = javaAttributeInfo.getAttributeName();

                    filterMethod = filterMethod + NEW_LINE + FOUR_SPACE_INDENTATION + OVERRIDE + NEW_LINE
                            + FOUR_SPACE_INDENTATION + PUBLIC + SPACE + className + BUILDER + SPACE
                            + attributeName + OPEN_PARENTHESIS + attrQuaifiedType
                            + SPACE + RECEIVED_OBJECT + attributeName + CLOSE_PARENTHESIS + SPACE + OPEN_CURLY_BRACKET
                            + NEW_LINE + EIGHT_SPACE_INDENTATION + attributeName + SPACE + EQUAL + SPACE
                            + RECEIVED_OBJECT + attributeName + SEMI_COLAN + NEW_LINE + EIGHT_SPACE_INDENTATION
                            + GET_FILTER_LEAF + OPEN_PARENTHESIS + CLOSE_PARENTHESIS + PERIOD + SET_METHOD_PREFIX
                            + OPEN_PARENTHESIS + String.valueOf(numleaf) + CLOSE_PARENTHESIS + SEMI_COLAN + NEW_LINE
                            + EIGHT_SPACE_INDENTATION + RETURN + SPACE + THIS + SEMI_COLAN + NEW_LINE
                            + FOUR_SPACE_INDENTATION + CLOSE_CURLY_BRACKET;
                    numleaf++;
                }
            }
        }
        return filterMethod;
    }

    /**
     * Returns string for leaf list set method.
     *
     * @param className    class name
     * @param curnode      current YANG node
     * @param pluginConfig plugin configurations
     * @return string for leaf list set method
     */
    public static String getSetterForLeafList(String className, YangNode curnode, YangPluginConfig pluginConfig) {

        int numleaf = 1;
        String filterMethod = "";
        TempJavaBeanFragmentFiles tempFragmentFiles = ((JavaCodeGeneratorInfo) curnode)
                .getTempJavaCodeFragmentFiles().getBeanTempFiles();
        if (curnode instanceof YangLeavesHolder) {
            YangLeavesHolder leavesHolder = (YangLeavesHolder) curnode;
            List<YangLeafList> listOfLeafList = leavesHolder.getListOfLeafList();

            if (listOfLeafList !=  null && !listOfLeafList.isEmpty()) {
                for (YangLeafList leafList : listOfLeafList) {
                    JavaAttributeInfo javaAttributeInfo = getJavaAttributeOfLeafList(tempFragmentFiles, leafList,
                            pluginConfig);
                    String attributeName = javaAttributeInfo.getAttributeName();
                    String attrQuaifiedType = getReturnType(javaAttributeInfo);
                    if (javaAttributeInfo.isListAttr()) {
                        attrQuaifiedType = getListString() + attrQuaifiedType + DIAMOND_CLOSE_BRACKET;
                    }
                    filterMethod = filterMethod + NEW_LINE + FOUR_SPACE_INDENTATION + OVERRIDE + NEW_LINE
                            + FOUR_SPACE_INDENTATION + PUBLIC + SPACE + className + BUILDER + SPACE
                            + attributeName + OPEN_PARENTHESIS + attrQuaifiedType
                            + SPACE + RECEIVED_OBJECT + attributeName + CLOSE_PARENTHESIS + SPACE + OPEN_CURLY_BRACKET
                            + NEW_LINE + EIGHT_SPACE_INDENTATION + attributeName + SPACE + EQUAL + SPACE
                            + RECEIVED_OBJECT + attributeName + SEMI_COLAN + NEW_LINE + EIGHT_SPACE_INDENTATION
                            + GET_FILTER_LEAF_LIST + OPEN_PARENTHESIS + CLOSE_PARENTHESIS + PERIOD + SET_METHOD_PREFIX
                            + OPEN_PARENTHESIS + String.valueOf(numleaf) + CLOSE_PARENTHESIS + SEMI_COLAN + NEW_LINE
                            + EIGHT_SPACE_INDENTATION + RETURN + SPACE + THIS + SEMI_COLAN + NEW_LINE
                            + FOUR_SPACE_INDENTATION + CLOSE_CURLY_BRACKET;
                    numleaf++;
                }
            }
        }
        return filterMethod;
    }

    /**
     * Returns whether the data type is of primitive data type.
     *
     * @param dataType data type to be checked
     * @return true, if data type can have primitive data type, false otherwise
     */
    public static boolean isPrimitiveDataType(YangDataTypes dataType) {
        return dataType == INT8
                || dataType == INT16
                || dataType == INT32
                || dataType == INT64
                || dataType == UINT8
                || dataType == UINT16
                || dataType == UINT32
                || dataType == UINT64
                || dataType == DECIMAL64
                || dataType == BOOLEAN
                || dataType == EMPTY;
    }

    /**
     * Returns string for is filter content match method.
     *
     * @param className    class name
     * @param curNode      current YANG node
     * @param pluginConfig plugin configurations
     * @return string for is filter content match method
     */
    public static String getIsFilterContentMatch(String className, YangNode curNode, YangPluginConfig pluginConfig) {

        int numleaf = 1;
        String filterMethod = NEW_LINE + NEW_LINE + FOUR_SPACE_INDENTATION + PUBLIC + SPACE + BOOLEAN_DATA_TYPE
                + SPACE + FILTER_CONTENT_MATCH + OPEN_PARENTHESIS + getCapitalCase(OBJECT)
                + SPACE + OBJ +
                CLOSE_PARENTHESIS + SPACE
                + OPEN_CURLY_BRACKET + NEW_LINE;
        TempJavaBeanFragmentFiles tempFragmentFiles = ((JavaCodeGeneratorInfo) curNode)
                .getTempJavaCodeFragmentFiles().getBeanTempFiles();
        filterMethod = filterMethod + EIGHT_SPACE_INDENTATION + getCapitalCase(DEFAULT)
                + getCapitalCase(className) + SPACE + APP_INSTANCE + SPACE + EQUAL + SPACE + OPEN_PARENTHESIS +
                getCapitalCase(DEFAULT) + getCapitalCase(className) + CLOSE_PARENTHESIS + OBJ +
                SEMI_COLAN + NEW_LINE;
        if (curNode instanceof YangAugmentableNode) {
            filterMethod = filterMethod + getAugmentableOpParamSyntax();
        }

        if (curNode instanceof YangLeavesHolder) {
            YangLeavesHolder leavesHolder = (YangLeavesHolder) curNode;
            List<YangLeaf> leaves = leavesHolder.getListOfLeaf();
            List<YangLeafList> listOfLeafList = leavesHolder.getListOfLeafList();
            if (leaves != null) {
                for (YangLeaf leaf : leaves) {
                    JavaAttributeInfo javaAttributeInfo = getJavaAttributeOfLeaf(tempFragmentFiles, leaf,
                            pluginConfig);
                    String attributeName = javaAttributeInfo.getAttributeName();

                    String attrQuaifiedType = "";
                    if (isPrimitiveDataType(leaf.getDataType().getDataType())) {
                        attrQuaifiedType = APP_INSTANCE + PERIOD + attributeName + OPEN_PARENTHESIS + CLOSE_PARENTHESIS
                                + SPACE + NOT + EQUAL + SPACE + attributeName + OPEN_PARENTHESIS
                                + CLOSE_PARENTHESIS;
                    } else {
                        attrQuaifiedType = APP_INSTANCE + PERIOD + attributeName + OPEN_PARENTHESIS + CLOSE_PARENTHESIS
                                + SPACE + EQUAL + EQUAL + SPACE + NULL + SPACE + OR_OPERATION + SPACE
                                + NOT + OPEN_PARENTHESIS + attributeName + OPEN_PARENTHESIS + CLOSE_PARENTHESIS
                                + PERIOD + EQUALS_STRING + OPEN_PARENTHESIS + APP_INSTANCE + PERIOD
                                + attributeName + OPEN_PARENTHESIS + CLOSE_PARENTHESIS
                                + CLOSE_PARENTHESIS + CLOSE_PARENTHESIS;
                    }

                    filterMethod = filterMethod + EIGHT_SPACE_INDENTATION + IF + SPACE + OPEN_PARENTHESIS
                            + GET_FILTER_LEAF + OPEN_PARENTHESIS + CLOSE_PARENTHESIS + PERIOD + GET_METHOD_PREFIX
                            + OPEN_PARENTHESIS + String.valueOf(numleaf) + CLOSE_PARENTHESIS + CLOSE_PARENTHESIS
                            + SPACE + OPEN_CURLY_BRACKET + NEW_LINE + TWELVE_SPACE_INDENTATION + IF + SPACE
                            + OPEN_PARENTHESIS + attrQuaifiedType + CLOSE_PARENTHESIS + SPACE
                            + OPEN_CURLY_BRACKET + NEW_LINE + SIXTEEN_SPACE_INDENTATION + RETURN + SPACE + FALSE
                            + SEMI_COLAN + NEW_LINE + TWELVE_SPACE_INDENTATION + CLOSE_CURLY_BRACKET + NEW_LINE
                            + EIGHT_SPACE_INDENTATION + CLOSE_CURLY_BRACKET + NEW_LINE;
                    numleaf++;
                }
            }

            if (listOfLeafList != null) {
                numleaf = 1;
                for (YangLeafList leafList : listOfLeafList) {
                    JavaAttributeInfo javaAttributeInfo = getJavaAttributeOfLeafList(tempFragmentFiles, leafList,
                            pluginConfig);
                    String attributeName = javaAttributeInfo.getAttributeName();
                    String attrQuaifiedType = "";
                    if (isPrimitiveDataType(leafList.getDataType().getDataType())) {
                        attrQuaifiedType = APP_INSTANCE + PERIOD + attributeName + OPEN_PARENTHESIS + CLOSE_PARENTHESIS
                                + SPACE + NOT + EQUAL + SPACE + attributeName + OPEN_PARENTHESIS
                                + CLOSE_PARENTHESIS;
                    } else {
                        attrQuaifiedType = APP_INSTANCE + PERIOD + attributeName + OPEN_PARENTHESIS + CLOSE_PARENTHESIS
                                + SPACE + EQUAL + EQUAL + SPACE + NULL + OR_OPERATION + OPEN_PARENTHESIS + NOT
                                + OPEN_PARENTHESIS + attributeName + OPEN_PARENTHESIS + CLOSE_PARENTHESIS
                                + PERIOD + EQUALS_STRING + OPEN_PARENTHESIS + APP_INSTANCE + PERIOD
                                + attributeName + OPEN_PARENTHESIS + CLOSE_PARENTHESIS + CLOSE_PARENTHESIS
                                + CLOSE_PARENTHESIS + CLOSE_PARENTHESIS;
                    }

                    filterMethod = filterMethod + EIGHT_SPACE_INDENTATION + IF + SPACE + OPEN_PARENTHESIS
                            + GET_FILTER_LEAF_LIST + OPEN_PARENTHESIS + CLOSE_PARENTHESIS + PERIOD + GET_METHOD_PREFIX
                            + OPEN_PARENTHESIS + String.valueOf(numleaf) + CLOSE_PARENTHESIS + CLOSE_PARENTHESIS
                            + SPACE + OPEN_CURLY_BRACKET + NEW_LINE + TWELVE_SPACE_INDENTATION + IF + SPACE
                            + OPEN_PARENTHESIS + attrQuaifiedType + CLOSE_PARENTHESIS + SPACE
                            + OPEN_CURLY_BRACKET + NEW_LINE + SIXTEEN_SPACE_INDENTATION + RETURN + SPACE + FALSE
                            + SEMI_COLAN + NEW_LINE + TWELVE_SPACE_INDENTATION + CLOSE_CURLY_BRACKET + NEW_LINE
                            + EIGHT_SPACE_INDENTATION + CLOSE_CURLY_BRACKET + NEW_LINE;
                    numleaf++;
                }
            }

            if (leaves !=  null || listOfLeafList != null) {
                filterMethod = filterMethod + EIGHT_SPACE_INDENTATION + RETURN + SPACE + TRUE + SEMI_COLAN +
                        NEW_LINE + FOUR_SPACE_INDENTATION + CLOSE_CURLY_BRACKET + NEW_LINE;
            }
        }
        return filterMethod;
    }

    /*Returns method string for op parms augmented syntax*/
    private static String getAugmentableOpParamSyntax() {
        return EIGHT_SPACE_INDENTATION + FOR + SPACE + OPEN_PARENTHESIS + YANG_AUGMENTED_INFO + SPACE +
                getSmallCase(YANG_AUGMENTED_INFO) + SPACE + COLAN + SPACE + APP_INSTANCE +
                PERIOD + GET_METHOD_PREFIX + YANG_AUGMENTED_INFO + MAP + OPEN_PARENTHESIS + CLOSE_PARENTHESIS + PERIOD
                + VALUE + "s" + OPEN_PARENTHESIS + CLOSE_PARENTHESIS + CLOSE_PARENTHESIS + SPACE + OPEN_CURLY_BRACKET +
                NEW_LINE + TWELVE_SPACE_INDENTATION + IF + SPACE + OPEN_PARENTHESIS + NOT + OPEN_PARENTHESIS +
                getSmallCase(YANG_AUGMENTED_INFO) + SPACE + INSTANCE_OF + SPACE + YANG_AUGMENTED_OP_PARAM_INFO +
                CLOSE_PARENTHESIS + CLOSE_PARENTHESIS + SPACE + OPEN_CURLY_BRACKET + NEW_LINE +
                SIXTEEN_SPACE_INDENTATION + THROW + SPACE + NEW + SPACE + ILLEGAL_ARGUMENT_EXCEPTION +
                OPEN_PARENTHESIS +
                ILLEGAL_ARGUMENT_EXCEPTION_MSG + CLOSE_PARENTHESIS + SEMI_COLAN + NEW_LINE + TWELVE_SPACE_INDENTATION +
                CLOSE_CURLY_BRACKET + NEW_LINE + NEW_LINE + TWELVE_SPACE_INDENTATION + YANG_AUGMENTED_INFO + SPACE +
                getSmallCase(YANG_AUGMENTED_OP_PARAM_INFO) + SPACE + EQUAL + SPACE + APP_INSTANCE + PERIOD +
                GET_METHOD_PREFIX + AUGMENTED_INFO + OPEN_PARENTHESIS + OPEN_PARENTHESIS + OPEN_PARENTHESIS +
                YANG_AUGMENTED_OP_PARAM_INFO + CLOSE_PARENTHESIS + SPACE + getSmallCase(YANG_AUGMENTED_INFO) +
                CLOSE_PARENTHESIS + PERIOD + GET_METHOD_PREFIX + BASE_CLASS + CLOSE_PARENTHESIS + SEMI_COLAN +
                NEW_LINE + TWELVE_SPACE_INDENTATION + IF + OPEN_PARENTHESIS + NOT +
                OPEN_PARENTHESIS + OPEN_PARENTHESIS + YANG_AUGMENTED_OP_PARAM_INFO + CLOSE_PARENTHESIS + SPACE +
                getSmallCase(YANG_AUGMENTED_INFO) + CLOSE_PARENTHESIS
                + PERIOD + "isFilterContentMatch" + OPEN_PARENTHESIS +
                getSmallCase(YANG_AUGMENTED_OP_PARAM_INFO) + CLOSE_PARENTHESIS +
                CLOSE_PARENTHESIS +
                SPACE + OPEN_CURLY_BRACKET + NEW_LINE + SIXTEEN_SPACE_INDENTATION + RETURN + SPACE + FALSE +
                SEMI_COLAN + NEW_LINE + TWELVE_SPACE_INDENTATION
                + CLOSE_CURLY_BRACKET + NEW_LINE + EIGHT_SPACE_INDENTATION + CLOSE_CURLY_BRACKET + NEW_LINE;
    }

    /**
     * Returns implementation of get base class method for op param files.
     *
     * @param baseClassName base class name
     * @return implementation of get base class method for op param files
     */
    public static String getBaseClassMethodImpl(String baseClassName) {
        return NEW_LINE + getOverRideString() + FOUR_SPACE_INDENTATION + PUBLIC + SPACE + CLASS_STRING +
                DIAMOND_OPEN_BRACKET +
                QUESTION_MARK +
                DIAMOND_CLOSE_BRACKET + SPACE + GET_METHOD_PREFIX + BASE_CLASS + SPACE + OPEN_CURLY_BRACKET + NEW_LINE +
                EIGHT_SPACE_INDENTATION + RETURN + SPACE + baseClassName + PERIOD + CLASS + SEMI_COLAN + NEW_LINE +
                FOUR_SPACE_INDENTATION +
                CLOSE_CURLY_BRACKET;

    }

    /*
     * Retruns omit null value string.
     *
     * @return omit null value string
     */
    public static String getOmitNullValueString() {
        return TWELVE_SPACE_INDENTATION + PERIOD + OMIT_NULL_VALUE_STRING + NEW_LINE;
    }

    /**
     * Returns to string method's close string.
     *
     * @return to string method close string
     */
    public static String getToStringMethodClose() {
        return TWELVE_SPACE_INDENTATION + PERIOD + TO + STRING_DATA_TYPE + OPEN_PARENTHESIS + CLOSE_PARENTHESIS
                + SEMI_COLAN + NEW_LINE + FOUR_SPACE_INDENTATION + CLOSE_CURLY_BRACKET + NEW_LINE;
    }

    /**
     * Returns to string method for class.
     *
     * @param attr attribute info
     * @return to string method
     */
    public static String getToStringMethod(JavaAttributeInfo attr) {

        String attributeName = attr.getAttributeName();
        return TWELVE_SPACE_INDENTATION + PERIOD + ADD_STRING + OPEN_PARENTHESIS + QUOTES + attributeName + QUOTES
                + COMMA + SPACE + attributeName + CLOSE_PARENTHESIS;
    }

    /**
     * Returns from string method's open string.
     *
     * @param className    name of the class
     * @param pluginConfig plugin configurations
     * @return from string method's open string
     */
    public static String getFromStringMethodSignature(String className, YangPluginConfig pluginConfig) {
        return getJavaDoc(FROM_METHOD, className, false, pluginConfig) + FOUR_SPACE_INDENTATION + PUBLIC + SPACE
                + STATIC + SPACE + className + SPACE + FROM_STRING_METHOD_NAME + OPEN_PARENTHESIS
                + STRING_DATA_TYPE + SPACE + FROM_STRING_PARAM_NAME + CLOSE_PARENTHESIS + SPACE
                + OPEN_CURLY_BRACKET + NEW_LINE;
    }

    /**
     * Return from string method's close string.
     *
     * @return from string method's close string
     */
    public static String getFromStringMethodClose() {
        return EIGHT_SPACE_INDENTATION + RETURN + SPACE + NULL + SEMI_COLAN + NEW_LINE + FOUR_SPACE_INDENTATION +
                CLOSE_CURLY_BRACKET + NEW_LINE;
    }

    /**
     * Return from string method's body string.
     *
     * @param attr                    attribute info
     * @param fromStringAttributeInfo attribute info for the from string wrapper type
     * @return from string method's body string
     */
    public static String getFromStringMethod(JavaAttributeInfo attr,
                                             JavaAttributeInfo fromStringAttributeInfo) {

        return EIGHT_SPACE_INDENTATION + getTrySubString() + NEW_LINE + TWELVE_SPACE_INDENTATION
                + getParsedSubString(attr, fromStringAttributeInfo) + SEMI_COLAN + NEW_LINE + TWELVE_SPACE_INDENTATION
                + getReturnOfSubString() + NEW_LINE + EIGHT_SPACE_INDENTATION + getCatchSubString()
                + NEW_LINE + EIGHT_SPACE_INDENTATION + CLOSE_CURLY_BRACKET;
    }

    /**
     * Returns sub string with try statement for union's from string method.
     *
     * @return sub string with try statement for union's from string method
     */
    public static String getTrySubString() {
        return TRY + SPACE + OPEN_CURLY_BRACKET;
    }

    /**
     * Returns sub string with return statement for union's from string method.
     *
     * @return sub string with return statement for union's from string method
     */
    public static String getReturnOfSubString() {
        return RETURN + SPACE + OF + OPEN_PARENTHESIS + TMP_VAL + CLOSE_PARENTHESIS + SEMI_COLAN;
    }

    /**
     * Returns sub string with catch statement for union's from string method.
     *
     * @return sub string with catch statement for union's from string method
     */
    public static String getCatchSubString() {
        return CLOSE_CURLY_BRACKET + SPACE + CATCH + SPACE + OPEN_PARENTHESIS + EXCEPTION + SPACE + EXCEPTION_VAR
                + CLOSE_PARENTHESIS + SPACE + OPEN_CURLY_BRACKET;
    }

    /**
     * Returns sub string with parsed statement for union's from string method.
     *
     * @param attr attribute info
     * @return sub string with parsed statement for union's from string method
     */
    private static String getParsedSubString(JavaAttributeInfo attr,
                                             JavaAttributeInfo fromStringAttributeInfo) {

        String targetDataType = getReturnType(attr);
        if (fromStringAttributeInfo.getAttributeType().getDataType() == BITS) {
            String lines = targetDataType + SPACE + TMP_VAL + SPACE + EQUAL + SPACE + NEW + SPACE + targetDataType
                    + OPEN_PARENTHESIS + CLOSE_PARENTHESIS + SEMI_COLAN + NEW_LINE;
            lines += TWELVE_SPACE_INDENTATION + FROM_STRING_PARAM_NAME + SPACE + EQUAL + SPACE + FROM_STRING_PARAM_NAME
                    + PERIOD + REPLACE_STRING + OPEN_PARENTHESIS + SINGLE_QUOTE + OPEN_CURLY_BRACKET + SINGLE_QUOTE
                    + COMMA + SPACE + SINGLE_QUOTE + SPACE + SINGLE_QUOTE + CLOSE_PARENTHESIS + SEMI_COLAN + NEW_LINE;
            lines += TWELVE_SPACE_INDENTATION + FROM_STRING_PARAM_NAME + SPACE + EQUAL + SPACE + FROM_STRING_PARAM_NAME
                    + PERIOD + REPLACE_STRING + OPEN_PARENTHESIS + SINGLE_QUOTE + CLOSE_CURLY_BRACKET + SINGLE_QUOTE
                    + COMMA + SPACE + SINGLE_QUOTE + SPACE + SINGLE_QUOTE + CLOSE_PARENTHESIS + SEMI_COLAN + NEW_LINE;
            lines += TWELVE_SPACE_INDENTATION + FROM_STRING_PARAM_NAME + SPACE + EQUAL + SPACE + FROM_STRING_PARAM_NAME
                    + PERIOD + TRIM_STRING + OPEN_PARENTHESIS + CLOSE_PARENTHESIS + SEMI_COLAN + NEW_LINE;
            lines += TWELVE_SPACE_INDENTATION + STRING_DATA_TYPE + SQUARE_BRACKETS + SPACE + BITS_STRING_ARRAY_VAR
                    + SPACE + EQUAL + SPACE + FROM_STRING_PARAM_NAME + PERIOD + SPLIT_STRING + OPEN_PARENTHESIS
                    + QUOTES + COMMA + QUOTES + COMMA + SPACE + ZERO + CLOSE_PARENTHESIS + SEMI_COLAN + NEW_LINE;
            lines += TWELVE_SPACE_INDENTATION + FOR + SPACE + OPEN_PARENTHESIS + STRING_DATA_TYPE + SPACE
                    + BIT_TEMP_VAR + SPACE + COLON + SPACE + BITS_STRING_ARRAY_VAR + CLOSE_PARENTHESIS + SPACE
                    + OPEN_CURLY_BRACKET + NEW_LINE;
            lines += SIXTEEN_SPACE_INDENTATION + BIT_TEMP_VAR + SPACE + EQUAL + SPACE + BIT_TEMP_VAR + PERIOD
                    + TRIM_STRING + OPEN_PARENTHESIS + CLOSE_PARENTHESIS + SEMI_COLAN + NEW_LINE;
            lines += SIXTEEN_SPACE_INDENTATION + TMP_VAL + PERIOD + SET_METHOD_PREFIX + OPEN_PARENTHESIS
                    + INTEGER_WRAPPER + PERIOD + PARSE_INT + OPEN_PARENTHESIS + BIT_TEMP_VAR + CLOSE_PARENTHESIS
                    + CLOSE_PARENTHESIS + SEMI_COLAN + NEW_LINE;
            lines += TWELVE_SPACE_INDENTATION + CLOSE_CURLY_BRACKET;
            return lines;
        } else if (attr.getAttributeType().getDataType() == BINARY) {
            return targetDataType + SPACE + TMP_VAL + SPACE + EQUAL + SPACE + BASE64 + PERIOD
                    + GET_DECODER + OPEN_PARENTHESIS + CLOSE_PARENTHESIS + PERIOD + DECODE + OPEN_PARENTHESIS
                    + FROM_STRING_PARAM_NAME + CLOSE_PARENTHESIS + SEMI_COLAN;
        } else {
            String parseFromStringMethod = getParseFromStringMethod(targetDataType,
                    fromStringAttributeInfo.getAttributeType());
            return targetDataType + SPACE + TMP_VAL + SPACE + EQUAL + SPACE + parseFromStringMethod
                    + OPEN_PARENTHESIS + FROM_STRING_PARAM_NAME + CLOSE_PARENTHESIS + SEMI_COLAN;
        }
    }

    /**
     * Returns hash code method open strings.
     *
     * @return hash code method open string
     */
    public static String getHashCodeMethodOpen() {
        return getOverRideString() + FOUR_SPACE_INDENTATION + PUBLIC + SPACE + INT + SPACE + HASH_CODE_STRING
                + OPEN_PARENTHESIS + CLOSE_PARENTHESIS + SPACE + OPEN_CURLY_BRACKET + NEW_LINE + EIGHT_SPACE_INDENTATION
                + RETURN + SPACE + OBJECT_STRING + SUFFIX_S + PERIOD + HASH + OPEN_PARENTHESIS;
    }

    /**
     * Returns hash code methods close string.
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
     * Returns hash code method for class.
     *
     * @param attr attribute info
     * @return hash code method
     */
    public static String getHashCodeMethod(JavaAttributeInfo attr) {
        return attr.getAttributeName() + COMMA + SPACE;
    }

    /**
     * Returns equals method open strings.
     *
     * @param className class name
     * @return equals method open string
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
     * Returns equals methods close string.
     *
     * @param equalMethodString equal method string
     * @return equals method close string
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
     * Returns equals method for class.
     *
     * @param attr attribute info
     * @return equals method
     */
    public static String getEqualsMethod(JavaAttributeInfo attr) {

        String attributeName = attr.getAttributeName();
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

        String attrQualifiedType = getReturnType(attr);

        return FOUR_SPACE_INDENTATION + PUBLIC + SPACE + STATIC + SPACE + name + SPACE + OF + OPEN_PARENTHESIS
                + attrQualifiedType + SPACE + VALUE + CLOSE_PARENTHESIS + SPACE + OPEN_CURLY_BRACKET + NEW_LINE
                + EIGHT_SPACE_INDENTATION + RETURN + SPACE + NEW + SPACE + name + OPEN_PARENTHESIS + VALUE
                + CLOSE_PARENTHESIS + SEMI_COLAN + NEW_LINE + FOUR_SPACE_INDENTATION + CLOSE_CURLY_BRACKET + NEW_LINE;
    }

    /**
     * Returns of method's string and java doc for special type.
     *
     * @param attr                   attribute info
     * @param generatedJavaClassName class name
     * @param pluginConfig           plugin configurations
     * @return of method's string and java doc for special type
     */
    public static String getOfMethodStringAndJavaDoc(JavaAttributeInfo attr, String generatedJavaClassName,
                                                     YangPluginConfig pluginConfig) {

        String attrType = getReturnType(attr);
        String attrName = attr.getAttributeName();

        return getJavaDoc(OF_METHOD, generatedJavaClassName + " for type " + attrName, false, pluginConfig)
                + getOfMethodString(attrType, generatedJavaClassName);
    }

    /**
     * Returns of method's string.
     *
     * @param type      data type
     * @param className class name
     * @return of method's string
     */
    private static String getOfMethodString(String type, String className) {

        return FOUR_SPACE_INDENTATION + PUBLIC + SPACE + STATIC + SPACE + className + SPACE + OF + OPEN_PARENTHESIS
                + type + SPACE + VALUE + CLOSE_PARENTHESIS + SPACE + OPEN_CURLY_BRACKET + NEW_LINE
                + EIGHT_SPACE_INDENTATION + RETURN + SPACE + NEW + SPACE + className + OPEN_PARENTHESIS + VALUE
                + CLOSE_PARENTHESIS + SEMI_COLAN + NEW_LINE + FOUR_SPACE_INDENTATION + CLOSE_CURLY_BRACKET;
    }

    /**
     * Returns string and java doc for constructor of type class.
     *
     * @param attr                   attribute info
     * @param generatedJavaClassName class name
     * @param pluginConfig           plugin configurations
     * @return string and java doc for constructor of type class
     */
    public static String getTypeConstructorStringAndJavaDoc(JavaAttributeInfo attr,
                                                            String generatedJavaClassName,
                                                            YangPluginConfig pluginConfig) {

        String attrType = getReturnType(attr);
        String attrName = attr.getAttributeName();

        return getJavaDoc(TYPE_CONSTRUCTOR, generatedJavaClassName + " for type " + attrName, false, pluginConfig)
                + getTypeConstructorString(attrType, attrName, generatedJavaClassName);
    }

    /**
     * Returns type constructor string.
     *
     * @param type      data type
     * @param name      attribute name
     * @param className class name
     * @return type constructor string
     */
    private static String getTypeConstructorString(String type, String name, String className) {

        return FOUR_SPACE_INDENTATION + PUBLIC + SPACE + className + OPEN_PARENTHESIS + type + SPACE + VALUE
                + CLOSE_PARENTHESIS + SPACE + OPEN_CURLY_BRACKET + NEW_LINE + EIGHT_SPACE_INDENTATION + THIS + PERIOD
                + name + SPACE + EQUAL + SPACE + VALUE + SEMI_COLAN + NEW_LINE + FOUR_SPACE_INDENTATION
                + CLOSE_CURLY_BRACKET;
    }

    /**
     * Returns implementation of add augmentation.
     *
     * @return implementation of add augmentation
     */
    static String getAddAugmentInfoMethodImpl() {
        return NEW_LINE + generateForAddAugmentation() + FOUR_SPACE_INDENTATION + PUBLIC + SPACE + VOID + SPACE +
                ADD_STRING + AUGMENTED_INFO + OPEN_PARENTHESIS + YANG_AUGMENTED_INFO + SPACE + VALUE + COMMA +
                SPACE + CLASS_STRING + SPACE + CLASS + OBJECT_STRING + CLOSE_PARENTHESIS + SPACE + OPEN_CURLY_BRACKET +
                NEW_LINE + EIGHT_SPACE_INDENTATION + getSmallCase(YANG_AUGMENTED_INFO) + MAP + PERIOD + PUT +
                OPEN_PARENTHESIS + CLASS + OBJECT_STRING + COMMA + SPACE + VALUE +
                CLOSE_PARENTHESIS + SEMI_COLAN + NEW_LINE + FOUR_SPACE_INDENTATION +
                CLOSE_CURLY_BRACKET + NEW_LINE;
    }

    /**
     * Returns implementation of get augment info.
     *
     * @return implementation of get augment info
     */
    static String getAugmentInfoImpl() {

        return generateForGetAugmentation() + FOUR_SPACE_INDENTATION + PUBLIC + SPACE +
                YANG_AUGMENTED_INFO + SPACE + GET_METHOD_PREFIX +
                AUGMENTED_INFO + OPEN_PARENTHESIS + CLASS_STRING + SPACE + CLASS + OBJECT_STRING +
                CLOSE_PARENTHESIS + SPACE + OPEN_CURLY_BRACKET + NEW_LINE + EIGHT_SPACE_INDENTATION +
                RETURN + SPACE + getSmallCase(YANG_AUGMENTED_INFO) + MAP + PERIOD + GET + OPEN_PARENTHESIS + CLASS +
                OBJECT_STRING + CLOSE_PARENTHESIS + SEMI_COLAN + NEW_LINE + FOUR_SPACE_INDENTATION +
                CLOSE_CURLY_BRACKET + NEW_LINE;
    }

    /**
     * Returns implementation of get augment info.
     *
     * @return implementation of get augment info
     */
    static String getAugmentInfoMapImpl(YangPluginConfig pluginConfig) {

        return getJavaDoc(GETTER_METHOD, getSmallCase(YANG_AUGMENTED_INFO) + MAP, false, pluginConfig)
                + FOUR_SPACE_INDENTATION + PUBLIC + SPACE +
                MAP + DIAMOND_OPEN_BRACKET + CLASS_STRING + DIAMOND_OPEN_BRACKET + QUESTION_MARK +
                DIAMOND_CLOSE_BRACKET + COMMA + SPACE + YANG_AUGMENTED_INFO + DIAMOND_CLOSE_BRACKET + SPACE +
                GET_METHOD_PREFIX + YANG_AUGMENTED_INFO + MAP + OPEN_PARENTHESIS +
                CLOSE_PARENTHESIS + SPACE + OPEN_CURLY_BRACKET + NEW_LINE + EIGHT_SPACE_INDENTATION +
                RETURN + SPACE + getSmallCase(YANG_AUGMENTED_INFO) + MAP + SEMI_COLAN + NEW_LINE
                + FOUR_SPACE_INDENTATION + CLOSE_CURLY_BRACKET + NEW_LINE;
    }

    /**
     * Returns enum's constructor.
     *
     * @param className enum's class name
     * @return enum's constructor
     */
    public static String getEnumsConstructor(String className) {
        return FOUR_SPACE_INDENTATION + className + OPEN_PARENTHESIS + INT + SPACE + VALUE + CLOSE_PARENTHESIS + SPACE
                + OPEN_CURLY_BRACKET + NEW_LINE + EIGHT_SPACE_INDENTATION + getSmallCase(className) + SPACE + EQUAL
                + SPACE + VALUE + SEMI_COLAN + NEW_LINE + FOUR_SPACE_INDENTATION + CLOSE_CURLY_BRACKET;
    }

    /**
     * Returns of method for enum class.
     *
     * @param className    class name
     * @param attr         java attribute
     * @param enumMap      enum's sets map
     * @param enumList     enum's sets list
     * @param pluginConfig plugin configurations
     * @return of method
     */
    public static String getEnumsOfMethod(String className, JavaAttributeInfo attr,
                                          Map<String, Integer> enumMap, List<String> enumList,
                                          YangPluginConfig pluginConfig) {
        String attrType = getReturnType(attr);
        String attrName = attr.getAttributeName();

        String method = FOUR_SPACE_INDENTATION + PUBLIC + SPACE + STATIC + SPACE + getCapitalCase(className) + SPACE
                + OF + OPEN_PARENTHESIS
                + attrType + SPACE + VALUE + CLOSE_PARENTHESIS + SPACE + OPEN_CURLY_BRACKET + NEW_LINE
                + EIGHT_SPACE_INDENTATION + SWITCH + SPACE + OPEN_PARENTHESIS + VALUE
                + CLOSE_PARENTHESIS + SPACE + OPEN_CURLY_BRACKET + NEW_LINE;
        int value = 0;
        for (String str : enumList) {

            value = enumMap.get(str);
            method = method + TWELVE_SPACE_INDENTATION + CASE + SPACE + value + COLON + NEW_LINE
                    + SIXTEEN_SPACE_INDENTATION + RETURN + SPACE + getCapitalCase(className) + PERIOD
                    + str + SEMI_COLAN + NEW_LINE;
        }
        method = method + TWELVE_SPACE_INDENTATION + DEFAULT + SPACE + COLON + NEW_LINE + SIXTEEN_SPACE_INDENTATION
                + RETURN + SPACE + NULL + SEMI_COLAN + NEW_LINE + EIGHT_SPACE_INDENTATION + CLOSE_CURLY_BRACKET
                + NEW_LINE + FOUR_SPACE_INDENTATION + CLOSE_CURLY_BRACKET;

        return getJavaDoc(OF_METHOD, getCapitalCase(className) + " for type" + attrName, false, pluginConfig)
                + method;
    }

    /**
     * Returns activate method string.
     *
     * @return activate method string
     */
    public static String addActivateMethod() {
        return FOUR_SPACE_INDENTATION + ACTIVATE_ANNOTATION + FOUR_SPACE_INDENTATION
                + PUBLIC + SPACE + VOID + SPACE + ACTIVATE + OPEN_PARENTHESIS
                + CLOSE_PARENTHESIS + SPACE + OPEN_CURLY_BRACKET
                + NEW_LINE + EIGHT_SPACE_INDENTATION
                + YANG_UTILS_TODO
                + NEW_LINE + EIGHT_SPACE_INDENTATION
                + STARTED_LOG_INFO + FOUR_SPACE_INDENTATION
                + CLOSE_CURLY_BRACKET + NEW_LINE;
    }

    /**
     * Returns deactivate method string.
     *
     * @return deactivate method string
     */
    public static String addDeActivateMethod() {
        return NEW_LINE + FOUR_SPACE_INDENTATION + DEACTIVATE_ANNOTATION + FOUR_SPACE_INDENTATION
                + PUBLIC + SPACE + VOID + SPACE + DEACTIVATE + OPEN_PARENTHESIS
                + CLOSE_PARENTHESIS + SPACE + OPEN_CURLY_BRACKET + NEW_LINE + EIGHT_SPACE_INDENTATION
                + YANG_UTILS_TODO + NEW_LINE + EIGHT_SPACE_INDENTATION
                + STOPPED_LOG_INFO + FOUR_SPACE_INDENTATION + CLOSE_CURLY_BRACKET + NEW_LINE;
    }

    /**
     * Returns from string method parsed string.
     *
     * @param targetDataType target data type
     * @param yangType       YANG type
     * @return parsed string
     */
    public static String getParseFromStringMethod(String targetDataType, YangType<?> yangType) {

        YangDataTypes type = yangType.getDataType();

        switch (type) {
            case INT8:
                return BYTE_WRAPPER + PERIOD + PARSE_BYTE;
            case INT16:
                return SHORT_WRAPPER + PERIOD + PARSE_SHORT;
            case INT32:
                return INTEGER_WRAPPER + PERIOD + PARSE_INT;
            case INT64:
                return LONG_WRAPPER + PERIOD + PARSE_LONG;
            case UINT8:
                return SHORT_WRAPPER + PERIOD + PARSE_SHORT;
            case UINT16:
                return INTEGER_WRAPPER + PERIOD + PARSE_INT;
            case UINT32:
                return LONG_WRAPPER + PERIOD + PARSE_LONG;
            case UINT64:
                return NEW + SPACE + BIG_INTEGER;
            case DECIMAL64:
                return NEW + SPACE + BIG_DECIMAL;
            case STRING:
                return EMPTY_STRING;
            case EMPTY:
            case BOOLEAN:
                return BOOLEAN_WRAPPER + PERIOD + PARSE_BOOLEAN;
            case BITS:
            case UNION:
            case ENUMERATION:
            case DERIVED:
                return targetDataType + PERIOD + FROM_STRING_METHOD_NAME;
            default:
                throw new TranslatorException("given data type is not supported.");
        }
    }

    /**
     * Returns the constructor strings for class file.
     *
     * @param attr         attribute info
     * @param pluginConfig plugin configurations
     * @return constructor for class
     */
    public static String getAugmentedConstructor(JavaAttributeInfo attr, YangPluginConfig pluginConfig) {

        String attributeName = getCamelCase(attr.getAttributeName(), pluginConfig.getConflictResolver());

        return EIGHT_SPACE_INDENTATION + THIS + PERIOD + attributeName + OPEN_PARENTHESIS
                + VALUE + PERIOD + attributeName + OPEN_PARENTHESIS + CLOSE_PARENTHESIS
                + CLOSE_PARENTHESIS + SEMI_COLAN + NEW_LINE;
    }

    /**
     * Returns augmented data getter and setter methods for service class.
     *
     * @param parent parent node
     * @return augmented data getter and setter methods for service class
     */
    static String getAugmentsDataMethodForService(YangNode parent) {
        List<YangAtomicPath> targets = getSetOfNodeIdentifiers(parent);
        YangNode augmentedNode;
        String curNodeName;
        String method;
        StringBuilder methods = new StringBuilder();
        String parentName;
        String returnType;
        YangPluginConfig pluginConfig = ((JavaFileInfoContainer) parent).getJavaFileInfo().getPluginConfig();
        for (YangAtomicPath nodeId : targets) {
            augmentedNode = nodeId.getResolvedNode().getParent();
            if (((JavaFileInfoContainer) augmentedNode).getJavaFileInfo().getJavaName() != null) {
                curNodeName = ((JavaFileInfoContainer) augmentedNode).getJavaFileInfo().getJavaName();
            } else {
                curNodeName = getCapitalCase(getCamelCase(augmentedNode.getName(), pluginConfig
                        .getConflictResolver()));
            }
            returnType = getAugmentedClassNameForDataMethods(augmentedNode, parent);
            parentName = getParentNodeNameForDataMethods(augmentedNode, pluginConfig);
            method = getJavaDoc(GETTER_METHOD, getSmallCase(AUGMENTED + parentName + curNodeName), false,
                    pluginConfig) + getGetterForInterface(AUGMENTED + parentName
                            + getCapitalCase(curNodeName),
                    returnType, false, GENERATE_SERVICE_AND_MANAGER)
                    + NEW_LINE;
            methods.append(method);

            method = getJavaDoc(MANAGER_SETTER_METHOD, AUGMENTED +
                    getCapitalCase(parentName) + curNodeName, false, pluginConfig) +
                    getSetterForInterface(getSmallCase(AUGMENTED) + parentName +
                                    getCapitalCase(curNodeName), returnType, parentName,
                            false,
                            GENERATE_SERVICE_AND_MANAGER) + NEW_LINE;
            methods.append(method);
        }
        return methods.toString();
    }

    /**
     * Returns augmented data getter and setter methods for manager class.
     *
     * @param parent parent node
     * @return augmented data getter and setter methods for manager class
     */
    static String getAugmentsDataMethodForManager(YangNode parent) {
        List<YangAtomicPath> targets = getSetOfNodeIdentifiers(parent);
        YangNode augmentedNode;
        String curNodeName;
        String returnType;
        String method;
        StringBuilder methods = new StringBuilder();
        String parentName;
        YangPluginConfig pluginConfig = ((JavaFileInfoContainer) parent).getJavaFileInfo().getPluginConfig();
        for (YangAtomicPath nodeId : targets) {
            augmentedNode = nodeId.getResolvedNode().getParent();
            if (((JavaFileInfoContainer) augmentedNode).getJavaFileInfo().getJavaName() != null) {
                curNodeName = ((JavaFileInfoContainer) augmentedNode).getJavaFileInfo().getJavaName();
            } else {
                curNodeName = getCapitalCase(getCamelCase(augmentedNode.getName(), pluginConfig
                        .getConflictResolver()));
            }
            returnType = getAugmentedClassNameForDataMethods(augmentedNode, parent);
            parentName = getParentNodeNameForDataMethods(augmentedNode, pluginConfig);
            method = getOverRideString() + getGetter(returnType, AUGMENTED
                            + parentName + getCapitalCase(curNodeName),
                    GENERATE_SERVICE_AND_MANAGER) + NEW_LINE;
            methods.append(method);

            method = getOverRideString() + getSetter(parentName, getSmallCase(AUGMENTED) +
                            getCapitalCase(parentName) + getCapitalCase(curNodeName),
                    returnType,
                    GENERATE_SERVICE_AND_MANAGER)
                    + NEW_LINE;
            methods.append(method);
        }
        return methods.toString();
    }

}

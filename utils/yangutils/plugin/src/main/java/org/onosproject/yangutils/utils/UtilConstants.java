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

package org.onosproject.yangutils.utils;

import java.io.File;
import java.util.Arrays;
import java.util.List;

/**
 * Represents utilities constants which are used while generating java files.
 */
public final class UtilConstants {

    /**
     * JavaDocs for impl class.
     */
    public static final String IMPL_CLASS_JAVA_DOC = " * Represents the implementation of ";

    /**
     * JavaDocs for builder class.
     */
    public static final String BUILDER_CLASS_JAVA_DOC = " * Represents the builder implementation of ";

    /**
     * JavaDocs for interface class.
     */
    public static final String INTERFACE_JAVA_DOC = " * Abstraction of an entity which represents the"
            + " functionality of ";

    /**
     * JavaDocs for event.
     */
    public static final String EVENT_JAVA_DOC = " * Represents event implementation of ";

    /**
     * JavaDocs for op param class.
     */
    public static final String OP_PARAM_JAVA_DOC = " * Represents operation parameter implementation of ";

    /**
     * JavaDocs for event listener.
     */
    public static final String EVENT_LISTENER_JAVA_DOC = " * Abstraction for event listener of ";

    /**
     * JavaDocs for builder interface class.
     */
    public static final String BUILDER_INTERFACE_JAVA_DOC = " * Builder for ";

    /**
     * JavaDocs for enum class.
     */
    public static final String ENUM_CLASS_JAVADOC = " * Represents ENUM data of ";

    /**
     * JavaDocs for enum attribute.
     */
    public static final String ENUM_ATTRIBUTE_JAVADOC = " * Represents ";

    /**
     * JavaDocs for package info class.
     */
    public static final String PACKAGE_INFO_JAVADOC = " * Implementation of YANG node ";

    /**
     * JavaDocs for package info class.
     */
    public static final String PACKAGE_INFO_JAVADOC_OF_CHILD = "'s children nodes";

    /**
     * JavaDocs's first line.
     */
    public static final String JAVA_DOC_FIRST_LINE = "/**\n";

    /**
     * JavaDocs's last line.
     */
    public static final String JAVA_DOC_END_LINE = " */\n";

    /**
     * JavaDocs's param annotation.
     */
    public static final String JAVA_DOC_PARAM = " * @param ";

    /**
     * JavaDocs's return annotation.
     */
    public static final String JAVA_DOC_RETURN = " * @return ";

    /**
     * JavaDocs's description for setter method.
     */
    public static final String JAVA_DOC_SETTERS = " * Returns the builder object of ";

    /**
     * JavaDocs's description for add to list method.
     */
    public static final String JAVA_DOC_ADD_TO_LIST = " * Adds to the list of ";

    /**
     * JavaDocs's description for setter method.
     */
    public static final String JAVA_DOC_MANAGER_SETTERS = " * Sets the value to attribute ";

    /**
     * JavaDocs's description for OF method.
     */
    public static final String JAVA_DOC_OF = " * Returns the object of ";

    /**
     * JavaDocs's description for typedef' setter method.
     */
    public static final String JAVA_DOC_SETTERS_COMMON = " * Sets the value of ";

    /**
     * JavaDocs's description for getter method.
     */
    public static final String JAVA_DOC_GETTERS = " * Returns the attribute ";

    /**
     * JavaDocs's description for getter method.
     */
    public static final String JAVA_DOC_FOR_VALIDATOR = " * Validates if value is in given range.";

    /**
     * JavaDocs's description for getter method.
     */
    public static final String JAVA_DOC_FOR_VALIDATOR_RETURN = " * @return true if value is in range";

    /**
     * JavaDocs's description for constructor.
     */
    public static final String JAVA_DOC_CONSTRUCTOR = " * Creates an instance of ";

    /**
     * JavaDocs's description for build method.
     */
    public static final String JAVA_DOC_BUILD = " * Builds object of ";

    /**
     * JavaDocs's return statement for build method.
     */
    public static final String JAVA_DOC_BUILD_RETURN = "object of ";

    /**
     * JavaDocs's statement for builder object.
     */
    public static final String BUILDER_OBJECT = "builder object of ";

    /**
     * JavaDocs's statement for rpc method.
     */
    public static final String JAVA_DOC_RPC = " * Service interface of ";

    /**
     * JavaDocs's statement for rpc's input string.
     */
    public static final String RPC_INPUT_STRING = "input of service interface ";

    /**
     * JavaDocs's statement for rpc's output string.
     */
    public static final String RPC_OUTPUT_STRING = "output of service interface ";

    /**
     * Static attribute for new line.
     */
    public static final String NEW_LINE = "\n";

    /**
     * Static attribute for default.
     */
    public static final String DEFAULT = "default";

    /**
     * Static attribute for op param class.
     */
    public static final String OPERATION = "OpParam";

    /**
     * Static attribute for java code generation for sbi.
     */
    public static final String SBI = "sbi";

    /**
     * Static attribute for multiple new line.
     */
    public static final String MULTIPLE_NEW_LINE = "\n\n";

    /**
     * Static attribute for empty line.
     */
    public static final String EMPTY_STRING = "";

    /**
     * Static attribute for new line with asterisk.
     */
    public static final String NEW_LINE_ASTERISK = " *\n";

    /**
     * Static attribute for period.
     */
    public static final String PERIOD = ".";

    /**
     * Static attribute for period.
     */
    public static final String INVOKE = "invoke";

    /**
     * Static attribute for parse byte.
     */
    public static final String PARSE_BYTE = "parseByte";

    /**
     * Static attribute for parse boolean.
     */
    public static final String PARSE_BOOLEAN = "parseBoolean";

    /**
     * Static attribute for parse short.
     */
    public static final String PARSE_SHORT = "parseShort";

    /**
     * Static attribute for parse int.
     */
    public static final String PARSE_INT = "parseInt";

    /**
     * Static attribute for parse long.
     */
    public static final String PARSE_LONG = "parseLong";

    /**
     * Static attribute for base64.
     */
    public static final String BASE64 = "Base64";

    /**
     * Static attribute for getEncoder.
     */
    public static final String GET_ENCODER = "getEncoder";

    /**
     * Static attribute for encodeToString.
     */
    public static final String ENCODE_TO_STRING = "encodeToString";

    /**
     * Static attribute for getDecoder.
     */
    public static final String GET_DECODER = "getDecoder";

    /**
     * Static attribute for decode.
     */
    public static final String DECODE = "decode";

    /**
     * Static attribute for omit null value.
     */
    public static final String OMIT_NULL_VALUE_STRING = "omitNullValues()";

    /**
     * Static attribute for colan.
     */
    public static final String COLAN = ":";

    /**
     * Static attribute for underscore.
     */
    public static final String UNDER_SCORE = "_";

    /**
     * Static attribute for semi-colan.
     */
    public static final String SEMI_COLAN = ";";

    /**
     * Static attribute for hyphen.
     */
    public static final String HYPHEN = "-";

    /**
     * Static attribute for space.
     */
    public static final String SPACE = " ";

    /**
     * Static attribute for validateRange.
     */
    public static final String VALIDATE_RANGE = "validateRange";

    /**
     * Static attribute for minRange.
     */
    public static final String MIN_RANGE = "minRange";

    /**
     * Static attribute for maxRange.
     */
    public static final String MAX_RANGE = "maxRange";

    /**
     * Static attribute for minRange.
     */
    public static final String INT_MIN_RANGE_ATTR = "static final int INT32_MIN_RANGE = -2147483648;\n";

    /**
     * Static attribute for minRange.
     */
    public static final String INT_MIN_RANGE = "INT32_MIN_RANGE";

    /**
     * Static attribute for minRange.
     */
    public static final String INT_MAX_RANGE = "INT32_MAX_RANGE";

    /**
     * Static attribute for maxRange.
     */
    public static final String INT_MAX_RANGE_ATTR = "static final int INT32_MAX_RANGE = 2147483647;";


    /**
     * Static attribute for minRange.
     */
    public static final String UINT_MIN_RANGE_ATTR = "static final int UINT16_MIN_RANGE = 0;\n";

    /**
     * Static attribute for maxRange.
     */
    public static final String UINT_MAX_RANGE_ATTR = "static final int UINT16_MAX_RANGE = 2147483647;";


    /**
     * Static attribute for minRange.
     */
    public static final String UINT_MIN_RANGE = "UINT16_MIN_RANGE";

    /**
     * Static attribute for maxRange.
     */
    public static final String UINT_MAX_RANGE = "UINT16_MAX_RANGE";

    /**
     * Static attribute for minRange.
     */
    public static final String LONG_MIN_RANGE_ATTR = "static final BigInteger INT64_MIN_RANGE =" +
            " new BigInteger(\"-9223372036854775808\");\n";

    /**
     * Static attribute for maxRange.
     */
    public static final String LONG_MAX_RANGE_ATTR = "static final BigInteger INT64_MAX_RANGE =" +
            " new BigInteger(\"9223372036854775807\");";

    /**
     * Static attribute for minRange.
     */
    public static final String LONG_MIN_RANGE = "INT64_MIN_RANGE";

    /**
     * Static attribute for maxRange.
     */
    public static final String LONG_MAX_RANGE = "INT64_MAX_RANGE";

    /**
     * Static attribute for minRange.
     */
    public static final String ULONG_MIN_RANGE_ATTR = "static final BigInteger UINT32_MIN_RANGE =" +
            " new BigInteger(\"0\");\n";

    /**
     * Static attribute for maxRange.
     */
    public static final String ULONG_MAX_RANGE_ATTR = "static final BigInteger UINT32_MAX_RANGE =" +
            " new BigInteger(\"9223372036854775807\");";


    /**
     * Static attribute for minRange.
     */
    public static final String ULONG_MIN_RANGE = "UINT32_MIN_RANGE";

    /**
     * Static attribute for maxRange.
     */
    public static final String ULONG_MAX_RANGE = "UINT32_MAX_RANGE";

    /**
     * Static attribute for subject.
     */
    public static final String SUBJECT = "Subject";

    /**
     * Static attribute for ListenerRegistry.
     */
    public static final String LISTENER_REG = "ListenerRegistry";

    /**
     * Static attribute for ListenerService.
     */
    public static final String LISTENER_SERVICE = "ListenerService";

    /**
     * Static attribute for listener package.
     */
    public static final String ONOS_EVENT_PKG = "org.onosproject.event";

    /**
     * Static attribute for colon.
     */
    public static final String COLON = ":";

    /**
     * Static attribute for caret.
     */
    public static final String CARET = "^";

    /**
     * Static attribute for input string.
     */
    public static final String INPUT = "input";

    /**
     * Static attribute for output string.
     */
    public static final String OUTPUT = "output";

    /**
     * Static attribute for current string.
     */
    public static final String CURRENT = "current";

    /**
     * Static attribute for leafref string.
     */
    public static final String LEAFREF = "leafref";

    /**
     * Static attribute for identityref string.
     */
    public static final String IDENTITYREF = "identityref";

    /**
     * Static attribute for output variable of rpc.
     */
    public static final String RPC_INPUT_VAR_NAME = "inputVar";

    /**
     * Static attribute for new line.
     */
    public static final String EQUAL = "=";

    /**
     * Static attribute for slash syntax.
     */
    public static final String SLASH = File.separator;

    /**
     * Static attribute for add syntax.
     */
    public static final String ADD = "+";

    /**
     * Static attribute for single quote.
     */
    public static final String SINGLE_QUOTE = "\'";

    /**
     * Static attribute for quotes.
     */
    public static final String QUOTES = "\"";

    /**
     * Static attribute for zero.
     */
    public static final String ZERO = "0";

    /**
     * Static attribute for ampersand.
     */
    public static final String AND = "&";

    /**
     * Static attribute for comma.
     */
    public static final String COMMA = ",";

    /**
     * Static attribute for class.
     */
    public static final String CLASS_STRING = "Class";

    /**
     * Static attribute for put.
     */
    public static final String PUT = "put";

    /**
     * Static attribute for get.
     */
    public static final String GET = "get";

    /**
     * Static attribute for slash character.
     */
    public static final char CHAR_OF_SLASH = '/';

    /**
     * Static attribute for open square bracket character.
     */
    public static final char CHAR_OF_OPEN_SQUARE_BRACKET = '[';

    /**
     * Static attribute for close square bracket character.
     */
    public static final char CHAR_OF_CLOSE_SQUARE_BRACKET = ']';

    /**
     * Static attribute for slash string.
     */
    public static final String SLASH_FOR_STRING = "/";

    /**
     * Static attribute for open square bracket.
     */
    public static final String OPEN_SQUARE_BRACKET = "[";

    /**
     * Static attribute for ancestor accessor.
     */
    public static final String ANCESTOR_ACCESSOR = "..";

    /**
     * Static attribute for ancestor accessor along with path.
     */
    public static final String ANCESTOR_ACCESSOR_IN_PATH = "../";

    /**
     * Static attribute for add syntax.
     */
    public static final String ADD_STRING = "add";

    /**
     * Static attribute for string replace syntax.
     */
    public static final String REPLACE_STRING = "replace";

    /**
     * Static attribute for string trim syntax.
     */
    public static final String TRIM_STRING = "trim";

    /**
     * Static attribute for string split syntax.
     */
    public static final String SPLIT_STRING = "split";

    /**
     * Static attribute for from syntax.
     */
    public static final String FROM_STRING_METHOD_NAME = "fromString";

    /**
     * Static attribute for check not null syntax.
     */
    public static final String CHECK_NOT_NULL_STRING = "checkNotNull";

    /**
     * Static attribute for hash code syntax.
     */
    public static final String HASH_CODE_STRING = "hashCode";

    /**
     * Static attribute for equals syntax.
     */
    public static final String EQUALS_STRING = "equals";

    /**
     * Static attribute for object.
     */
    public static final String OBJECT_STRING = "Object";

    /**
     * Static attribute for instance of syntax.
     */
    public static final String INSTANCE_OF = " instanceof ";

    /**
     * Static attribute for value syntax.
     */
    public static final String VALUE = "value";

    /**
     * Static attribute for suffix s.
     */
    public static final String SUFFIX_S = "s";

    /**
     * Static attribute for if.
     */
    public static final String IF = "if";

    /**
     * Static attribute for of.
     */
    public static final String OF = "of";

    /**
     * Static attribute for other.
     */
    public static final String OTHER = "other";

    /**
     * Static attribute for obj syntax.
     */
    public static final String OBJ = "obj";

    /**
     * Static attribute for hash syntax.
     */
    public static final String HASH = "hash";

    /**
     * Static attribute for to syntax.
     */
    public static final String TO = "to";

    /**
     * Static attribute for true syntax.
     */
    public static final String TRUE = "true";

    /**
     * Static attribute for false syntax.
     */
    public static final String FALSE = "false";

    /**
     * Static attribute for org.
     */
    public static final String ORG = "org";

    /**
     * Static attribute for temp.
     */
    public static final String TEMP = "temp";

    /**
     * Static attribute for YANG file directory.
     */
    public static final String YANG_RESOURCES = "yang/resources";

    /**
     * Static attribute for diamond close bracket syntax.
     */
    public static final String DIAMOND_OPEN_BRACKET = "<";

    /**
     * Static attribute for diamond close bracket syntax.
     */
    public static final String DIAMOND_CLOSE_BRACKET = ">";

    /**
     * Static attribute for exception syntax.
     */
    public static final String EXCEPTION = "Exception";

    /**
     * Static attribute for exception variable syntax.
     */
    public static final String EXCEPTION_VAR = "e";

    /**
     * Static attribute for open parenthesis syntax.
     */
    public static final String OPEN_PARENTHESIS = "(";

    /**
     * Static attribute for switch syntax.
     */
    public static final String SWITCH = "switch";

    /**
     * Static attribute for case syntax.
     */
    public static final String CASE = "case";

    /**
     * Static attribute for temp val syntax.
     */
    public static final String TMP_VAL = "tmpVal";

    /**
     * Static attribute for close curly bracket syntax.
     */
    public static final String ELSE = "else";

    /**
     * From string parameter name.
     */
    public static final String FROM_STRING_PARAM_NAME = "valInString";

    /**
     * Static attribute for close parenthesis syntax.
     */
    public static final String CLOSE_PARENTHESIS = ")";

    /**
     * Static attribute for open curly bracket syntax.
     */
    public static final String OPEN_CURLY_BRACKET = "{";

    /**
     * Static attribute for close curly bracket syntax.
     */
    public static final String CLOSE_CURLY_BRACKET = "}";

    /**
     * Static attribute for square brackets syntax.
     */
    public static final String SQUARE_BRACKETS = "[]";

    /**
     * Static attribute for getter method prefix.
     */
    public static final String GET_METHOD_PREFIX = "get";

    /**
     * Static attribute for getter method prefix.
     */
    public static final String GET_METHOD = "getMethod";

    /**
     * Static attribute for getter method prefix.
     */
    public static final String GET_CLASS = "getClass()";

    /**
     * Static attribute for setter method prefix.
     */
    public static final String SET_METHOD_PREFIX = "set";

    /**
     * Static attribute for get filter leaf flags.
     */
    public static final String GET_FILTER_LEAF = "get_valueLeafFlags";

    /**
     * Static attribute for getLeafIndex.
     */
    public static final String GET_LEAF_INDEX = "getLeafIndex()";

    /**
     * Static attribute for op param.
     */
    public static final String OP_PARAM = "OpParam";


    /**
     * Static attribute for is filter content match method prefix.
     */
    public static final String FILTER_CONTENT_MATCH = "isFilterContentMatch";

    /**
     * Static attribute for flag prefix.
     */
    public static final String FLAG = "flag";

    /**
     * Static attribute for break prefix.
     */
    public static final String BREAK = "break";

    /**
     * Static attribute for break prefix.
     */
    public static final String IS_EMPTY = "isEmpty()";

    /**
     * Static attribute for is isLeafValueSet method prefix.
     */
    public static final String VALUE_LEAF_SET = "isLeafValueSet";

    /**
     * Static attribute for is isSelectLeaf method prefix.
     */
    public static final String IS_SELECT_LEAF = "isSelectLeaf";

    /**
     * Static attribute for is selectLeaf method prefix.
     */
    public static final String SET_SELECT_LEAF = "selectLeaf";

    /**
     * Static attribute for is LeafIdentifier enum prefix.
     */
    public static final String LEAF_IDENTIFIER = "LeafIdentifier";

    /**
     * Static attribute for four space indentation.
     */
    public static final String FOUR_SPACE_INDENTATION = "    ";

    /**
     * Static attribute for not syntax.
     */
    public static final String NOT = "!";

    /**
     * Static attribute for try syntax.
     */
    public static final String TRY = "try";

    /**
     * Static attribute for catch syntax.
     */
    public static final String CATCH = "catch";

    /**
     * Static attribute for eight space indentation.
     */
    public static final String EIGHT_SPACE_INDENTATION = FOUR_SPACE_INDENTATION + FOUR_SPACE_INDENTATION;

    /**
     * Static attribute for twelve space indentation.
     */
    public static final String TWELVE_SPACE_INDENTATION = FOUR_SPACE_INDENTATION + EIGHT_SPACE_INDENTATION;

    /**
     * Static attribute for sixteen space indentation.
     */
    public static final String SIXTEEN_SPACE_INDENTATION = EIGHT_SPACE_INDENTATION + EIGHT_SPACE_INDENTATION;

    /**
     * Static attribute for twenty space indentation.
     */
    public static final String TWENTY_SPACE_INDENTATION = FOUR_SPACE_INDENTATION + SIXTEEN_SPACE_INDENTATION;

    /**
     * Static attribute for twenty four space indentation.
     */
    public static final String TWENTY_FOUR_SPACE_INDENTATION = EIGHT_SPACE_INDENTATION + SIXTEEN_SPACE_INDENTATION;

    /**
     * Static attribute for generated code path.
     */
    public static final String YANG_GEN_DIR = "src/main/java/";

    /**
     * Static attribute for base package.
     */
    public static final String DEFAULT_BASE_PKG = "org.onosproject.yang.gen";

    /**
     * Static attribute for YANG date prefix.
     */
    public static final String REVISION_PREFIX = "rev";

    /**
     * Static attribute for YANG automatic prefix for identifiers with keywords and beginning with digits.
     */
    public static final String YANG_AUTO_PREFIX = "yangAutoPrefix";

    /**
     * Static attribute for YANG version prefix.
     */
    public static final String VERSION_PREFIX = "v";

    /**
     * Static attribute for private modifier.
     */
    public static final String PRIVATE = "private";

    /**
     * Static attribute for public modifier.
     */
    public static final String PUBLIC = "public";

    /**
     * Static attribute for abstract modifier.
     */
    public static final String ABSTRACT = "abstract";

    /**
     * Static attribute for protected modifier.
     */
    public static final String PROTECTED = "protected";

    /**
     * Void java type.
     */
    public static final String VOID = "void";

    /**
     * String built in java type.
     */
    public static final String STRING_DATA_TYPE = "String";

    /**
     * Java.lang.* packages.
     */
    public static final String JAVA_LANG = "java.lang";

    /**
     * Java.math.* packages.
     */
    public static final String JAVA_MATH = "java.math";

    /**
     * Boolean built in java type.
     */
    public static final String BOOLEAN_DATA_TYPE = "boolean";

    /**
     * BigInteger built in java type.
     */
    public static final String BIG_INTEGER = "BigInteger";

    /**
     * BigDecimal built in java type.
     */
    public static final String BIG_DECIMAL = "BigDecimal";

    /**
     * BitSet built in java type.
     */
    public static final String BIT_SET = "BitSet";

    /**
     * Byte java built in type.
     */
    public static final String BYTE = "byte";

    /**
     * Short java built in type.
     */
    public static final String SHORT = "short";

    /**
     * Int java built in type.
     */
    public static final String INT = "int";

    /**
     * Long java built in type.
     */
    public static final String LONG = "long";

    /**
     * Double java built in type.
     */
    public static final String DOUBLE = "double";

    /**
     * Boolean built in java wrapper type.
     */
    public static final String BOOLEAN_WRAPPER = "Boolean";

    /**
     * Byte java built in wrapper type.
     */
    public static final String BYTE_WRAPPER = "Byte";

    /**
     * Short java built in wrapper type.
     */
    public static final String SHORT_WRAPPER = "Short";

    /**
     * Integer java built in wrapper type.
     */
    public static final String INTEGER_WRAPPER = "Integer";

    /**
     * Long java built in wrapper type.
     */
    public static final String LONG_WRAPPER = "Long";

    /**
     * Static variable for question mark.
     */
    public static final String QUESTION_MARK = "?";

    /**
     * List of keywords in java, this is used for checking if the input does not contain these keywords.
     */
    public static final List<String> JAVA_KEY_WORDS = Arrays.asList(
            "abstract", "assert", "boolean", "break", "byte", "case", "catch", "char", "class", "const", "continue",
            "default", "do", "double", "else", "extends", "false", "final", "finally", "float", "for", "goto", "if",
            "implements", "import", "instanceof", "enum", "int", "interface", "long", "native", "new", "null",
            "package", "private", "protected", "public", "return", "short", "static", "strictfp", "super", "switch",
            "synchronized", "this", "throw", "throws", "transient", "true", "try", "void", "volatile", "while");

    /**
     * Static attribute for regex for all the special characters.
     */
    public static final String REGEX_WITH_ALL_SPECIAL_CHAR = "\\p{Punct}+";

    /**
     * Static attribute for regex for three special characters used in identifier.
     */
    public static final String REGEX_FOR_IDENTIFIER_SPECIAL_CHAR = "[. _ -]+";

    /**
     * Static attribute for regex for period.
     */
    public static final String REGEX_FOR_PERIOD = "[.]";

    /**
     * Static attribute for regex for underscore.
     */
    public static final String REGEX_FOR_UNDERSCORE = "[_]";

    /**
     * Static attribute for regex for hyphen.
     */
    public static final String REGEX_FOR_HYPHEN = "[-]";

    /**
     * Static attribute for regex for digits.
     */
    public static final String REGEX_FOR_FIRST_DIGIT = "\\d.*";

    /**
     * Static attribute for regex with digits.
     */
    public static final String REGEX_WITH_DIGITS = "(?=\\d+)";

    /**
     * Static attribute for regex for single letter.
     */
    public static final String REGEX_FOR_SINGLE_LETTER = "[a-zA-Z]";

    /**
     * Static attribute for regex for digits with single letter.
     */
    public static final String REGEX_FOR_DIGITS_WITH_SINGLE_LETTER = "[0-9]+[a-zA-Z]";

    /**
     * Static attribute for regex with uppercase.
     */
    public static final String REGEX_WITH_UPPERCASE = "(?=\\p{Upper})";

    /**
     * Static attribute for regex for single capital case letter.
     */
    public static final String REGEX_WITH_SINGLE_CAPITAL_CASE = "[A-Z]";

    /**
     * Static attribute for regex for capital case letter with any number of digits and small case letters.
     */
    public static final String REGEX_WITH_SINGLE_CAPITAL_CASE_AND_DIGITS_SMALL_CASES = "[A-Z][0-9a-z]+";

    /**
     * Static attribute for regex for any string ending with service.
     */
    public static final String REGEX_FOR_ANY_STRING_ENDING_WITH_SERVICE = ".+Service";

    /**
     * Static attribute for class syntax.
     */
    public static final String CLASS = "class";

    /**
     * Static attribute for builder syntax.
     */
    public static final String BUILDER = "Builder";

    /**
     * Static attribute for manager syntax.
     */
    public static final String MANAGER = "Manager";

    /**
     * Static attribute for service syntax.
     */
    public static final String SERVICE = "Service";

    /**
     * Static attribute for interface syntax.
     */
    public static final String INTERFACE = "interface";

    /**
     * Static attribute for enum syntax.
     */
    public static final String ENUM = "enum";

    /**
     * Static attribute for type syntax.
     */
    public static final String TYPE = "Type";

    /**
     * Static attribute for static syntax.
     */
    public static final String STATIC = "static";

    /**
     * Static attribute for final syntax.
     */
    public static final String FINAL = "final";

    /**
     * Static attribute for package syntax.
     */
    public static final String PACKAGE = "package";

    /**
     * Static attribute for import syntax.
     */
    public static final String IMPORT = "import ";

    /**
     * Static attribute for null syntax.
     */
    public static final String NULL = "null";

    /**
     * Static attribute for return syntax.
     */
    public static final String RETURN = "return";

    /**
     * Static attribute for java new syntax.
     */
    public static final String NEW = "new";

    /**
     * Static attribute for this syntax.
     */
    public static final String THIS = "this";

    /**
     * Static attribute for implements syntax.
     */
    public static final String IMPLEMENTS = "implements";

    /**
     * Static attribute for extends syntax.
     */
    public static final String EXTEND = "extends";

    /**
     * Static attribute for service interface suffix syntax.
     */
    public static final String SERVICE_METHOD_STRING = "Service";

    /**
     * For event file generation.
     */
    public static final String EVENT_STRING = "Event";

    /**
     * For event listener file generation.
     */
    public static final String EVENT_LISTENER_STRING = "EventListener";

    /**
     * For event subject file generation.
     */
    public static final String EVENT_SUBJECT_NAME_SUFFIX = "EventSubject";

    /**
     * Static attribute for build method syntax.
     */
    public static final String BUILD = "build";

    /**
     * Static attribute for object.
     */
    public static final String OBJECT = "Object";

    /**
     * Static attribute for app instance.
     */
    public static final String APP_INSTANCE = "appInstance";

    /**
     * Static attribute for override annotation.
     */
    public static final String OVERRIDE = "@Override";

    /**
     * Static attribute for collections.
     */
    public static final String COLLECTION_IMPORTS = "java.util";

    /**
     * Static attribute for map.
     */
    public static final String MAP = "Map";

    /**
     * Static attribute for hash map.
     */
    public static final String HASH_MAP = "HashMap";


    /**
     * Static attribute for more object import package.
     */
    public static final String GOOGLE_MORE_OBJECT_IMPORT_PKG = "com.google.common.base";

    /**
     * Static attribute for more object import class.
     */
    public static final String GOOGLE_MORE_OBJECT_IMPORT_CLASS = "MoreObjects;\n";

    /**
     * Static attribute for to string method.
     */
    public static final String GOOGLE_MORE_OBJECT_METHOD_STRING = " MoreObjects.toStringHelper(getClass())";

    /**
     * Static attribute for java utilities import package.
     */
    public static final String JAVA_UTIL_OBJECTS_IMPORT_PKG = "java.util";

    /**
     * Static attribute for bitset.
     */
    public static final String BITSET = "BitSet";

    /**
     * Static attribute for java utilities objects import class.
     */
    public static final String JAVA_UTIL_OBJECTS_IMPORT_CLASS = "Objects;\n";

    /**
     * Static attribute for java utilities import base64 class.
     */
    public static final String JAVA_UTIL_IMPORT_BASE64_CLASS = "Base64;\n";

    /**
     * Static attribute for AugmentedInfo class.
     */
    public static final String YANG_AUGMENTED_INFO = "YangAugmentedInfo";

    /**
     * Static attribute for augmented.
     */
    public static final String AUGMENTED = "Augmented";

    /**
     * Static attribute for list.
     */
    public static final String LIST = "List";

    /**
     * Comment to be added for auto generated impl methods.
     */
    public static final String YANG_UTILS_TODO = "//TODO: YANG utils generated code";

    /**
     * Static attribute for AbstractEvent.
     */
    public static final String ABSTRACT_EVENT = "AbstractEvent";

    /**
     * Static attribute for EventListener.
     */
    public static final String EVENT_LISTENER = "EventListener";

    /**
     * Static attribute for or operator.
     */
    public static final String OR_OPERATION = "||";

    /**
     * Static attribute for YANG file error.
     */
    public static final String YANG_FILE_ERROR = "YANG file error : ";

    /**
     * Static attribute for unsupported error information.
     */
    public static final String UNSUPPORTED_YANG_CONSTRUCT = " is not supported.";

    /**
     * Static attribute for currently unsupported error information.
     */
    public static final String CURRENTLY_UNSUPPORTED = " is not supported in current version, please check wiki" +
            " for YANG utils road map.";

    /**
     * Static attribute for typedef linker error information.
     */
    public static final String TYPEDEF_LINKER_ERROR = "YANG file error: Unable to find base "
            + "typedef for given type";

    /**
     * Static attribute for grouping linker error information.
     */
    public static final String GROUPING_LINKER_ERROR = "YANG file error: Unable to find base "
            + "grouping for given uses";

    /**
     * Static attribute for if-feature linker error information.
     */
    public static final String FEATURE_LINKER_ERROR = "YANG file error: Unable to find feature "
            + "for given if-feature";

    /**
     * Static attribute for leafref linker error information.
     */
    public static final String LEAFREF_LINKER_ERROR = "YANG file error: Unable to find base "
            + "leaf/leaf-list for given leafref";

    /**
     * Static attribute for base linker error information.
     */
    public static final String BASE_LINKER_ERROR = "YANG file error: Unable to find base "
            + "identity for given base";

    /**
     * Static attribute for identityref linker error information.
     */
    public static final String IDENTITYREF_LINKER_ERROR = "YANG file error: Unable to find base "
            + "identity for given base";

    /**
     * Static attribute for jar.
     */
    public static final String JAR = "jar";

    /**
     * Static attribute for for.
     */
    public static final String FOR = "for";

    /**
     * Static attribute for YangAugmentedOpParamInfo.
     */
    public static final String YANG_AUGMENTED_OP_PARAM_INFO = "YangAugmentedOpParamInfo";

    /**
     * Static attribute for NoSuchMethodException.
     */
    public static final String NO_SUCH_METHOD_EXCEPTION = "NoSuchMethodException";

    /**
     * Static attribute for InvocationTargetException.
     */
    public static final String INVOCATION_TARGET_EXCEPTION = "InvocationTargetException";

    /**
     * Static attribute for InvocationTargetException.
     */
    public static final String INVOCATION_TARGET_EXCEPTION_IMPORT = "import" +
            " java.lang.reflect.InvocationTargetException;\n";
    /**
     * Static attribute for IllegalAccessException.
     */
    public static final String ILLEGAL_ACCESS_EXCEPTION = "IllegalAccessException";

    /**
     * Static attribute for arrayList.
     */
    public static final String ARRAY_LIST = "ArrayList<>()";

    /**
     * Static attribute for arrayList import.
     */
    public static final String ARRAY_LIST_IMPORT = IMPORT + COLLECTION_IMPORTS + ".ArrayList;\n";

    /**
     * Creates an instance of util constants.
     */
    private UtilConstants() {
    }
}

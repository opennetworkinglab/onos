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
     * JavaDocs's throw annotation.
     */
    public static final String JAVA_DOC_THROWS = " * @throws ";

    /**
     * JavaDocs's description for setter method.
     */
    public static final String JAVA_DOC_SETTERS = " * Returns the builder object of ";

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
     * Static attribute for leafref string.
     */
    public static final String LEAFREF = "leafref";

    /**
     * Static attribute for identityref string.
     */
    public static final String IDENTITYREF = "identityref";

    /**
     * Static attribute for instance identifier string.
     */
    public static final String INSTANCE_IDENTIFIER = "instance-identifier";

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
     * Static attribute for asterisk.
     */
    public static final String ASTERISK = "*";

    /**
     * Static attribute for at.
     */
    public static final String AT = "@";

    /**
     * Static attribute for quotes.
     */
    public static final String QUOTES = "\"";

    /**
     * Static attribute for ampersand.
     */
    public static final String AND = "&";

    /**
     * Static attribute for comma.
     */
    public static final String COMMA = ",";

    /**
     * Static attribute for add syntax.
     */
    public static final String ADD_STRING = "add";

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
     * Static attribute for enumValue syntax.
     */
    public static final String ENUM_VALUE = "enumValue";

    /**
     * Static attribute for suffix s.
     */
    public static final String SUFFIX_S = "s";

    /**
     * Static attribute for if.
     */
    public static final String IF = "if";

    /**
     * Static attribute for for.
     */
    public static final String FOR = "for";

    /**
     * Static attribute for while.
     */
    public static final String WHILE = "while";

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
     * Static attribute for clear syntax.
     */
    public static final String CLEAR = "clear";

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
     * Static attribute for getter method prefix.
     */
    public static final String GET_METHOD_PREFIX = "get";

    /**
     * Static attribute for setter method prefix.
     */
    public static final String SET_METHOD_PREFIX = "set";

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
     * Static attribute for YANG version perifx.
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
     * Float java built in type.
     */
    public static final String FLOAT = "float";

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
     * YangUint64 java built in wrapper type.
     */
    public static final String YANG_UINT64 = "YangUint64";

    /**
     * List of keywords in java, this is used for checking if the input does not contain these keywords.
     */
    public static final List<String> JAVA_KEY_WORDS = Arrays.asList(
            "abstract", "assert", "boolean", "break", "byte", "case", "catch", "char", "class", "const", "continue",
            "default", "do", "double", "else", "extends", "false", "final", "finally", "float", "for", "goto", "if",
            "implements", "import", "instanceof", "int", "interface", "long", "native", "new", "null", "package",
            "private", "protected", "public", "return", "short", "static", "strictfp", "super", "switch",
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
    public static final String EVENT_LISTENER_STRING = "Listener";

    /**
     * For event subject file generation.
     */
    public static final String EVENT_SUBJECT_NAME_SUFFIX = "EventSubject";

    /**
     * Static attribute for impl syntax.
     */
    public static final String IMPL = "Impl";

    /**
     * Static attribute for build method syntax.
     */
    public static final String BUILD = "build";

    /**
     * Static attribute for object.
     */
    public static final String OBJECT = "Object";

    /**
     * Static attribute for override annotation.
     */
    public static final String OVERRIDE = "@Override";

    /**
     * Static attribute for new line.
     */
    public static final String COLLECTION_IMPORTS = "java.util";

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
     * Static attribute for java utilities objects import class.
     */
    public static final String JAVA_UTIL_OBJECTS_IMPORT_CLASS = "Objects;\n";

    /**
     * Static attribute for AugmentationHolder class import package.
     */
    public static final String PROVIDED_AUGMENTATION_CLASS_IMPORT_PKG =
            "org.onosproject.yangutils.utils";

    /**
     * Static attribute for AugmentationHolder class import class.
     */
    public static final String AUGMENTATION_HOLDER_CLASS_IMPORT_CLASS = "AugmentationHolder;\n";

    /**
     * Static attribute for AugmentedInfo class import package.
     */
    public static final String AUGMENTED_INFO_CLASS_IMPORT_PKG = "org.onosproject.yangutils.utils";

    /**
     * Static attribute for AugmentedInfo class import class.
     */
    public static final String AUGMENTED_INFO_CLASS_IMPORT_CLASS = "AugmentedInfo;\n";

    /**
     * Static attribute for augmentation class.
     */
    public static final String AUGMENTATION = "Augmentation";

    /**
     * Static attribute for AugmentationHolder class.
     */
    public static final String AUGMENTATION_HOLDER = "AugmentationHolder";

    /**
     * Static attribute for AugmentedInfo class.
     */
    public static final String AUGMENTED_INFO = "AugmentedInfo";

    /**
     * Static attribute for augmentable.
     */
    public static final String AUGMENTABLE = "Augmentable";

    /**
     * Static attribute for list.
     */
    public static final String LIST = "List";

    /**
     * Static attribute for array list.
     */
    public static final String ARRAY_LIST = "ArrayList";

    /**
     * Comment to be added for autogenerated impl methods.
     */
    public static final String YANG_UTILS_TODO = "//TODO: YANG utils generated code";

    /**
     * Static attribute for activate annotation.
     */
    public static final String ACTIVATE_ANNOTATION = "@Activate\n";

    /**
     * Static attribute for activate.
     */
    public static final String ACTIVATE = "activate";

    /**
     * Static attribute for activate annotation import.
     */
    public static final String ACTIVATE_ANNOTATION_IMPORT = "import org.apache.felix.scr.annotations.Activate;\n";

    /**
     * Static attribute for deactivate annotation.
     */
    public static final String DEACTIVATE_ANNOTATION = "@Deactivate\n";

    /**
     * Static attribute for deactivate.
     */
    public static final String DEACTIVATE = "deactivate";

    /**
     * Static attribute for deactivate annotation import.
     */
    public static final String DEACTIVATE_ANNOTATION_IMPORT =
            "import org.apache.felix.scr.annotations.Deactivate;\n";

    /**
     * Static attribute for component annotation.
     */
    public static final String COMPONENT_ANNOTATION = "@Component";

    /**
     * Static attribute for component.
     */
    public static final String COMPONENT = "Component";

    /**
     * Static attribute for immediate.
     */
    public static final String IMMEDIATE = "immediate";

    /**
     * Static attribute for component annotation import.
     */
    public static final String COMPONENT_ANNOTATION_IMPORT =
            "import org.apache.felix.scr.annotations.Component;\n";

    /**
     * Static attribute for service annotation.
     */
    public static final String SERVICE_ANNOTATION = "@Service\n";

    /**
     * Static attribute for service annotation import.
     */
    public static final String SERVICE_ANNOTATION_IMPORT =
            "import org.apache.felix.scr.annotations.Service;\n";

    /**
     * Static attribute for logger factory import.
     */
    public static final String LOGGER_FACTORY_IMPORT =
            "import static org.slf4j.LoggerFactory.getLogger;\n";

    /**
     * Static attribute for logger import.
     */
    public static final String LOGGER_IMPORT =
            "import org.slf4j.Logger;\n";

    /**
     * Static attribute for logger statement.
     */
    public static final String LOGGER_STATEMENT =
            "\n    private final Logger log = getLogger(getClass());\n";

    /**
     * Static attribute for logger statement for started.
     */
    public static final String STARTED_LOG_INFO =
            "log.info(\"Started\");\n";

    /**
     * Static attribute for logger statement for stopped.
     */
    public static final String STOPPED_LOG_INFO =
            "log.info(\"Stopped\");\n";

    /**
     * Static attribute for AbstractEvent.
     */
    public static final String ABSTRACT_EVENT = "AbstractEvent";

    /**
     * Static attribute for EventListener.
     */
    public static final String EVENT_LISTENER = "EventListener";

    /**
     * Static attribute for YangBinary class.
     */
    public static final String YANG_BINARY_CLASS = "YangBinary";

    /**
     * Static attribute for YangBinary class.
     */
    public static final String YANG_BITS_CLASS = "YangBits";

    /**
     * Static attribute for YANG types package.
     */
    public static final String YANG_TYPES_PKG = "org.onosproject.yangutils.datamodel.utils.builtindatatype";

    /**
     * Static attribute for MathContext class.
     */
    public static final String MATH_CONTEXT = "MathContext";

    /**
     * Static attribute for DECIMAL64 class.
     */
    public static final String YANG_DECIMAL64_CLASS = "YangDecimal64";


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
     * Static attribute for reference.
     */
    public static final String REFERENCE = "Reference";

    /**
     * Static attribute for ReferenceCardinality.
     */
    public static final String REFERENCE_CARDINALITY = "ReferenceCardinality";

    /**
     * Static attribute for jar.
     */
    public static final String JAR = "jar";

    /**
     * Creates an instance of util constants.
     */
    private UtilConstants() {
    }
}

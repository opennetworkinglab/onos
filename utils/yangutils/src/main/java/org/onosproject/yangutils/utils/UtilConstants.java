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

package org.onosproject.yangutils.utils;

import java.util.Arrays;
import java.util.List;

/**
 * Provides utility constants while generating java files.
 */
public final class UtilConstants {

    /**
     * Default constructor.
     */
    private UtilConstants() {
    }

    /**
     * For java-docs.
     */
    public static final String IMPL_CLASS_JAVA_DOC = " * Provides the implementation of ";
    public static final String BUILDER_CLASS_JAVA_DOC = " * Provides the builder implementation of ";
    public static final String INTERFACE_JAVA_DOC = " * Abstraction of an entity which provides functionalities of ";
    public static final String BUILDER_INTERFACE_JAVA_DOC = " * Builder for ";
    public static final String PACKAGE_INFO_JAVADOC = " * Generated java code corresponding to YANG ";
    public static final String JAVA_DOC_FIRST_LINE = "/**\n";
    public static final String JAVA_DOC_END_LINE = " */\n";
    public static final String JAVA_DOC_PARAM = " * @param ";
    public static final String JAVA_DOC_RETURN = " * @return ";
    public static final String JAVA_DOC_THROWS = " * @throws ";
    public static final String JAVA_DOC_SETTERS = " * Returns the builder object of ";
    public static final String JAVA_DOC_OF = " * Returns the object of ";
    public static final String JAVA_DOC_SETTERS_COMMON = " * Sets the value of ";
    public static final String JAVA_DOC_GETTERS = " * Returns the attribute ";
    public static final String JAVA_DOC_DEFAULT_CONSTRUCTOR = " * Default Constructor.\n";
    public static final String JAVA_DOC_CONSTRUCTOR = " * Construct the object of ";
    public static final String JAVA_DOC_BUILD = " * Builds object of ";
    public static final String JAVA_DOC_BUILD_RETURN = "object of ";

    /**
     * Basic requirements.
     */
    public static final String NEW_LINE = "\n";
    public static final String EMPTY_STRING = "";
    public static final String NEW_LINE_ESTRIC = " *\n";
    public static final String PERIOD = ".";
    public static final String COLAN = ":";
    public static final String UNDER_SCORE = "_";
    public static final String SEMI_COLAN = ";";
    public static final String HYPHEN = "-";
    public static final String SPACE = " ";
    public static final String TAB = "\t";
    public static final String EQUAL = "=";
    public static final String SLASH = "/";
    public static final String ADD = "+";
    public static final String ASTERISK = "*";
    public static final String AT = "@";
    public static final String QUOTES = "\"";
    public static final String AND = "&";
    public static final String COMMA = ",";
    public static final String ADD_STRING = "add";
    public static final String CHECK_NOT_NULL_STRING = "checkNotNull";
    public static final String HASH_CODE_STRING = "hashCode";
    public static final String EQUALS_STRING = "equals";
    public static final String OBJECT_STRING = "Object";
    public static final String INSTANCE_OF = " instanceof ";

    public static final String VALUE = "value";

    public static final String IF = "if";
    public static final String FOR = "for";
    public static final String WHILE = "while";
    public static final String OF = "of";

    public static final String TRUE = "true";
    public static final String FALSE = "false";

    /**
     * For brackets.
     */
    public static final String DIAMOND_OPEN_BRACKET = "<";
    public static final String DIAMOND_CLOSE_BRACKET = ">";
    public static final String SQUARE_OPEN_BRACKET = "[";
    public static final String SQUARE_CLOSE_BRACKET = "]";
    public static final String OPEN_PARENTHESIS = "(";
    public static final String CLOSE_PARENTHESIS = ")";
    public static final String OPEN_CURLY_BRACKET = "{";
    public static final String CLOSE_CURLY_BRACKET = "}";

    /**
     * For methods.
     */
    public static final String GET_METHOD_PREFIX = "get";
    public static final String SET_METHOD_PREFIX = "set";

    /**
     * For indentation.
     */
    public static final String FOUR_SPACE_INDENTATION = "    ";
    public static final String EIGHT_SPACE_INDENTATION = FOUR_SPACE_INDENTATION + FOUR_SPACE_INDENTATION;
    public static final String TWELVE_SPACE_INDENTATION = FOUR_SPACE_INDENTATION + EIGHT_SPACE_INDENTATION;
    public static final String SIXTEEN_SPACE_INDENTATION = EIGHT_SPACE_INDENTATION + EIGHT_SPACE_INDENTATION;

    /**
     * For directories.
     */
    public static final String YANG_GEN_DIR = "src/main/java/";
    public static final String DEFAULT_BASE_PKG = "org.onosproject.yang.gen";
    public static final String REVISION_PREFIX = "rev";
    public static final String VERSION_PREFIX = "v";

    /**
     * For class modifiers.
     */
    public static final String PRIVATE = "private";
    public static final String PUBLIC = "public";
    public static final String PROTECTED = "protected";

    /**
     * For data types.
     */
    /**
     * Void java type.
     */
    public static final String VOID = "void";

    /**
     * String built in java type.
     */
    public static final String STRING = "String";
    /**
     * java.lang.* packages.
     */
    public static final String JAVA_LANG = "java.lang";

    /**
     * boolean built in java type.
     */
    public static final String BOOLEAN = "boolean";

    /**
     * byte java built in type.
     */
    public static final String BYTE = "byte";

    /**
     * short java built in type.
     */
    public static final String SHORT = "short";

    /**
     * int java built in type.
     */
    public static final String INT = "int";

    /**
     * long java built in type.
     */
    public static final String LONG = "long";

    /**
     * float java built in type.
     */
    public static final String FLOAT = "float";

    /**
     * double java built in type.
     */
    public static final String DOUBLE = "double";

    /**
     * boolean built in java wrapper type.
     */
    public static final String BOOLEAN_WRAPPER = "Boolean";

    /**
     * byte java built in wrapper type.
     */
    public static final String BYTE_WRAPPER = "Byte";

    /**
     * short java built in wrapper type.
     */
    public static final String SHORT_WRAPPER = "Short";

    /**
     * Integer java built in wrapper type.
     */
    public static final String INTEGER_WRAPPER = "Integer";

    /**
     * long java built in wrapper type.
     */
    public static final String LONG_WRAPPER = "Long";

    /**
     * float java built in wrapper type.
     */
    public static final String FLOAT_WRAPPER = "Float";

    /**
     * double java built in wrapper type.
     */
    public static final String DOUBLE_WRAPPER = "Double";

    /**
     * List of keywords in java, this is used for checking if the input does not contain these keywords.
     */
    public static final List JAVA_KEY_WORDS = Arrays.asList("abstract", "assert", "boolean", "break", "byte", "case",
            "catch", "char", "class", "const", "continue", "default", "do", "double", "else", "extends", "false",
            "final", "finally", "float", "for", "goto", "if", "implements", "import", "instanceof", "int", "interface",
            "long", "native", "new", "null", "package", "private", "protected", "public", "return", "short", "static",
            "strictfp", "super", "switch", "synchronized", "this", "throw", "throws", "transient", "true", "try",
            "void", "volatile", "while");

    /**
     * Defining regular expression.
     */
    public static final String REGEX_WITH_SPECIAL_CHAR = "[ : / - @ $ # ' * + , ; = ]+";
    public static final String REGEX_FOR_FIRST_DIGIT = "\\d.*";

    /**
     * For identifiers.
     */
    public static final String CLASS = "class";
    public static final String BUILDER = "Builder";
    public static final String BUILDER_OBJECT = "builder object of ";
    public static final String INTERFACE = "interface";
    public static final String ENUM = "enum";
    public static final String STATIC = "static";
    public static final String FINAL = "final";
    public static final String PACKAGE = "package";
    public static final String IMPORT = "import ";
    public static final String NULL = "null";
    public static final String RETURN = "return";
    public static final String NEW = "new";
    public static final String THIS = "this";
    public static final String IMPLEMENTS = "implements";
    public static final String EXTEND = "extends";
    public static final String IMPL = "Impl";
    public static final String BUILD = "build";
    public static final String OBJECT = "Object";
    public static final String OVERRIDE = "@Override";
    public static final String CHILDREN = "'s children";

    /**
     * For collections.
     */
    public static final String COLLECTION_IMPORTS = "java.util";
    public static final String MORE_OBJECT_IMPORT = "import com.google.common.base.MoreObjects;\n";
    public static final String JAVA_UTIL_OBJECTS_IMPORT = "import java.util.Objects;\n";
    public static final String ABSTRACT_COLLECTION = "AbstractCollection";

    public static final String LIST = "List";
    public static final String LINKED_LIST = "LinkedList";
    public static final String ARRAY_LIST = "ArrayList";
    public static final String ABSTRACT_LIST = "AbstractList";
    public static final String ABSTRACT_SEQUENTAIL_LIST = "AbstractSequentialList";

    public static final String SET = "Set";
    public static final String HASH_SET = "HashSet";
    public static final String ABSTRACT_SET = "AbstractSet";
    public static final String LINKED_HASH_SET = "LinkedHashSet";
    public static final String TREE_SET = "TreeSet";

    public static final String MAP = "Map";
    public static final String ABSTRACT_MAP = "AbstractMap";
    public static final String HASH_MAP = "HashMap";
    public static final String TREE_MAP = "TreeMap";
    public static final String CONCURRENT_MAP = "ConcurrentMap";
    public static final String EVENTUALLY_CONSISTENT_MAP = "EventuallyConsitentMap";
    public static final String STACK = "stack";
}

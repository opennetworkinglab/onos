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

import java.io.File;
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
     * JavaDocs for impl class.
     */
    public static final String IMPL_CLASS_JAVA_DOC = " * Provides the implementation of ";

    /**
     * JavaDocs for builder class.
     */
    public static final String BUILDER_CLASS_JAVA_DOC = " * Provides the builder implementation of ";

    /**
     * JavaDocs for interface class.
     */
    public static final String INTERFACE_JAVA_DOC = " * Abstraction of an entity which provides functionalities of ";

    /**
     * JavaDocs for builder interface class.
     */
    public static final String BUILDER_INTERFACE_JAVA_DOC = " * Builder for ";

    /**
     * JavaDocs for package info class.
     */
    public static final String PACKAGE_INFO_JAVADOC = " * Implementation of YANG file ";

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
     * JavaDocs's description for default constructor.
     */
    public static final String JAVA_DOC_DEFAULT_CONSTRUCTOR = " * Default Constructor.\n";

    /**
     * JavaDocs's description for constructor.
     */
    public static final String JAVA_DOC_CONSTRUCTOR = " * Construct the object of ";

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
     * Static attribute for new line.
     */
    public static final String NEW_LINE = "\n";

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
     * Static attribute for tab.
     */
    public static final String TAB = "\t";

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
     * Static attribute for square open bracket syntax.
     */
    public static final String SQUARE_OPEN_BRACKET = "[";

    /**
     * Static attribute for square close bracket syntax.
     */
    public static final String SQUARE_CLOSE_BRACKET = "]";

    /**
     * Static attribute for open parenthesis syntax.
     */
    public static final String OPEN_PARENTHESIS = "(";

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
     * java.lang.* packages.
     */
    public static final String JAVA_LANG = "java.lang";

    /**
     * boolean built in java type.
     */
    public static final String BOOLEAN_DATA_TYPE = "boolean";

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
    public static final List<String> JAVA_KEY_WORDS = Arrays.asList(
            "abstract", "assert", "boolean", "break", "byte", "case", "catch", "char", "class", "const", "continue",
            "default", "do", "double", "else", "extends", "false", "final", "finally", "float", "for", "goto", "if",
            "implements", "import", "instanceof", "int", "interface", "long", "native", "new", "null", "package",
            "private", "protected", "public", "return", "short", "static", "strictfp", "super", "switch",
            "synchronized", "this", "throw", "throws", "transient", "true", "try", "void", "volatile", "while");

    /**
     * Static attribute for regex for special char.
     */
    public static final String REGEX_WITH_SPECIAL_CHAR = "[ : / - @ $ # ' * + , ; = ]+";

    /**
     * Static attribute for regex for digits.
     */
    public static final String REGEX_FOR_FIRST_DIGIT = "\\d.*";

    /**
     * Static attribute for class syntax.
     */
    public static final String CLASS = "class";

    /**
     * Static attribute for builder syntax.
     */
    public static final String BUILDER = "Builder";

    /**
     * Static attribute for interface syntax.
     */
    public static final String INTERFACE = "interface";

    /**
     * Static attribute for enum syntax.
     */
    public static final String ENUM = "enum";

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
     * Static attribute for abstract collection.
     */
    public static final String ABSTRACT_COLLECTION = "AbstractCollection";

    /**
     * Static attribute for list.
     */
    public static final String LIST = "List";

    /**
     * Static attribute for linked list.
     */
    public static final String LINKED_LIST = "LinkedList";

    /**
     * Static attribute for array list.
     */
    public static final String ARRAY_LIST = "ArrayList";

    /**
     * Static attribute for abstract list.
     */
    public static final String ABSTRACT_LIST = "AbstractList";

    /**
     * Static attribute for abstract sequential list.
     */
    public static final String ABSTRACT_SEQUENTAIL_LIST = "AbstractSequentialList";

    /**
     * Static attribute for set.
     */
    public static final String SET = "Set";

    /**
     * Static attribute for hash set.
     */
    public static final String HASH_SET = "HashSet";

    /**
     * Static attribute for abstract set.
     */
    public static final String ABSTRACT_SET = "AbstractSet";

    /**
     * Static attribute for linked has set.
     */
    public static final String LINKED_HASH_SET = "LinkedHashSet";

    /**
     * Static attribute for tree set.
     */
    public static final String TREE_SET = "TreeSet";

    /**
     * Static attribute for map.
     */
    public static final String MAP = "Map";

    /**
     * Static attribute for abstract map.
     */
    public static final String ABSTRACT_MAP = "AbstractMap";

    /**
     * Static attribute for hash map.
     */
    public static final String HASH_MAP = "HashMap";

    /**
     * Static attribute for tree map.
     */
    public static final String TREE_MAP = "TreeMap";

    /**
     * Static attribute for concurrent map.
     */
    public static final String CONCURRENT_MAP = "ConcurrentMap";

    /**
     * Static attribute for eventually consistent map.
     */
    public static final String EVENTUALLY_CONSISTENT_MAP = "EventuallyConsitentMap";

    /**
     * Static attribute for stack syntax.
     */
    public static final String STACK = "stack";
}

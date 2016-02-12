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
    public static final String PACKAGE_INFO_JAVADOC = " * Generated java code for the YANG file ";
    public static final String JAVA_DOC_FIRST_LINE = "/**\n";
    public static final String JAVA_DOC_END_LINE = " */\n";
    public static final String JAVA_DOC_PARAM = " * @param ";
    public static final String JAVA_DOC_RETURN = " * @return ";
    public static final String JAVA_DOC_THROWS = " * @throws ";
    public static final String JAVA_DOC_SETTERS = " * Returns the builder object of ";
    public static final String JAVA_DOC_GETTERS = " * Returns the attribute ";
    public static final String JAVA_DOC_DEFAULT_CONSTRUCTOR = " * Default Constructor.\n";
    public static final String JAVA_DOC_CONSTRUCTOR = " * Construct the object of ";
    public static final String JAVA_DOC_BUILD = " * Builds object of ";
    public static final String JAVA_DOC_BUILD_RETURN = "object of ";

    /**
     * Basic requirements.
     */
    public static final String NEW_LINE = "\n";
    public static final String NEW_LINE_ESTRIC = " *\n";
    public static final String PERIOD = ".";
    public static final String COLAN = ":";
    public static final String SEMI_COLAN = ";";
    public static final String HYPHEN = "-";
    public static final String SPACE = " ";
    public static final String TAB = "\t";
    public static final String EQUAL = "=";
    public static final String SLASH = "/";
    public static final String ADD = "+";
    public static final String ASTERISK = "*";
    public static final String AT = "@";

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
    public static final String YANG_GEN_DIR = "src/main/yangmodal/";
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
    public static final String INT = "int";
    public static final String VOID = "void";
    public static final String SHORT = "short";
    public static final String LONG = "long";
    public static final String BOOLEAN = "boolean";
    public static final String STRING = "String";
    public static final String FLOAT = "float";
    public static final String BYTE = "byte";
    public static final String DOUBLE = "double";

    /**
     * For idenifiers.
     */
    public static final String CLASS = "class";
    public static final String BUILDER = "Builder";
    public static final String BUILDER_OBJECT = "builder object of ";
    public static final String INTERFACE = "interface";
    public static final String ENUM = "enum";
    public static final String STATIC = "static";
    public static final String FINAL = "final";
    public static final String PACKAGE = "package";
    public static final String IMPORT = "import";
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

    /**
     * For collections.
     */
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

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

/**
 * This is a YANG grammar for lexer based on which ANTLR will generate YANG lexer.
 */

lexer grammar YangLexer;

    // Statements keywords
    ANYXML_KEYWORD      : 'anyxml';
    ARGUMENT_KEYWORD    : 'argument';
    AUGMENT_KEYWORD     : 'augment';
    BASE_KEYWORD        : 'base';
    BELONGS_TO_KEYWORD  : 'belongs-to';
    BIT_KEYWORD         : 'bit';
    CASE_KEYWORD        : 'case';
    CHOICE_KEYWORD      : 'choice';
    CONFIG_KEYWORD      : 'config';
    CONTACT_KEYWORD     : 'contact';
    CONTAINER_KEYWORD   : 'container';
    DEFAULT_KEYWORD     : 'default';
    DESCRIPTION_KEYWORD : 'description';
    ENUM_KEYWORD        : 'enum';
    ERROR_APP_TAG_KEYWORD : 'error-app-tag';
    ERROR_MESSAGE_KEYWORD : 'error-message';
    EXTENSION_KEYWORD   : 'extension';
    DEVIATION_KEYWORD   : 'deviation';
    DEVIATE_KEYWORD     : 'deviate';
    FEATURE_KEYWORD     : 'feature';
    FRACTION_DIGITS_KEYWORD : 'fraction-digits';
    GROUPING_KEYWORD    : 'grouping';
    IDENTITY_KEYWORD    : 'identity';
    IF_FEATURE_KEYWORD  : 'if-feature';
    IMPORT_KEYWORD      : 'import';
    INCLUDE_KEYWORD     : 'include';
    INPUT_KEYWORD       : 'input';
    KEY_KEYWORD         : 'key';
    LEAF_KEYWORD        : 'leaf';
    LEAF_LIST_KEYWORD   : 'leaf-list';
    LENGTH_KEYWORD      : 'length';
    LIST_KEYWORD        : 'list';
    MANDATORY_KEYWORD   : 'mandatory';
    MAX_ELEMENTS_KEYWORD : 'max-elements';
    MIN_ELEMENTS_KEYWORD : 'min-elements';
    MODULE_KEYWORD      : 'module';
    MUST_KEYWORD        : 'must';
    NAMESPACE_KEYWORD   : 'namespace';
    NOTIFICATION_KEYWORD: 'notification';
    ORDERED_BY_KEYWORD  : 'ordered-by';
    ORGANIZATION_KEYWORD: 'organization';
    OUTPUT_KEYWORD      : 'output';
    PATH_KEYWORD        : 'path';
    PATTERN_KEYWORD     : 'pattern';
    POSITION_KEYWORD    : 'position';
    PREFIX_KEYWORD      : 'prefix';
    PRESENCE_KEYWORD    : 'presence';
    RANGE_KEYWORD       : 'range';
    REFERENCE_KEYWORD   : 'reference';
    REFINE_KEYWORD      : 'refine';
    REQUIRE_INSTANCE_KEYWORD : 'require-instance';
    REVISION_KEYWORD    : 'revision';
    REVISION_DATE_KEYWORD : 'revision-date';
    RPC_KEYWORD         : 'rpc';
    STATUS_KEYWORD      : 'status';
    SUBMODULE_KEYWORD   : 'submodule';
    TYPE_KEYWORD        : 'type';
    TYPEDEF_KEYWORD     : 'typedef';
    UNIQUE_KEYWORD      : 'unique';
    UNITS_KEYWORD       : 'units';
    USES_KEYWORD        : 'uses';
    VALUE_KEYWORD       : 'value';
    WHEN_KEYWORD        : 'when';
    YANG_VERSION_KEYWORD: 'yang-version';
    YIN_ELEMENT_KEYWORD : 'yin-element';
    ADD_KEYWORD         : 'add';
    CURRENT_KEYWORD     : 'current';
    DELETE_KEYWORD      : 'delete';
    DEPRECATED_KEYWORD  : 'deprecated';
    FALSE_KEYWORD       : 'false';
    MAX_KEYWORD         : 'max';
    MIN_KEYWORD         : 'min';
    NOT_SUPPORTED_KEYWORD : 'not-supported';
    OBSOLETE_KEYWORD    : 'obsolete';
    REPLACE_KEYWORD     : 'replace';
    SYSTEM_KEYWORD      : 'system';
    TRUE_KEYWORD        : 'true';
    UNBOUNDED_KEYWORD   : 'unbounded';
    USER_KEYWORD        : 'user';

    // Lexer tokens to be skipped
    COMMENT
        :   '/*' .*? '*/'    -> channel(HIDDEN)
        ;
    WS  :   [ \r\t\u000C\n]+ -> channel(HIDDEN)
        ;
    LINE_COMMENT
        : '//' ~[\r\n]* '\r'? '\n' -> channel(HIDDEN)
        ;

    // Additional rules
    INTEGER             : DIGIT+;
    DATE_ARG            : DIGIT DIGIT DIGIT DIGIT '-' DIGIT DIGIT '-' DIGIT DIGIT;
    LEFT_CURLY_BRACE    : '{';
    RIGHT_CURLY_BRACE   : '}';
    IDENTIFIER          : (ALPHA | '_')
                          (ALPHA | DIGIT | '_' | '-' | '.')*;
    STMTEND             : ';';
    DQUOTE              : '"';
    COLON               : ':';
    PLUS : '+';
    MINUS: '-';

    STRING : ((~( '\r' | '\n' | '\t' | ' ' | ';' | '{' | '"' | '\'')~( '\r' | '\n' | '\t' | ' ' | ';' | '{' )* ) | SUB_STRING );

    //Fragment rules
    fragment SUB_STRING : ('"' (ESC | ~["])*'"') | ('\'' (ESC | ~['])*'\'') ;
    fragment ESC :  '\\' (["\\/bfnrt] | UNICODE) ;
    fragment UNICODE : 'u' HEX HEX HEX HEX ;
    fragment HEX : [0-9a-fA-F] ;
    fragment ALPHA      : [A-Za-z];
    fragment DIGIT      : [0-9];
    fragment URN        : [u][r][n];
    fragment HTTP       : [h][t][t][p];
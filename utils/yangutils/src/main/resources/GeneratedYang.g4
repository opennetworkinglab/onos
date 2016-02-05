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

/**
 * This is a YANG grammar for parser based on which ANTLR will generate YANG parser.
 */

grammar GeneratedYang;
import YangLexer;

@header {
package org.onosproject.yangutils.parser.antlrgencode;
}

    yangfile : module_stmt
             | submodule_stmt;

    /**
     * module-stmt         = optsep module-keyword sep identifier-arg-str
     *                       optsep
     *                       "{" stmtsep
     *                           module-header-stmts
     *                           linkage-stmts
     *                           meta-stmts
     *                           revision-stmts
     *                           body-stmts
     *                       "}" optsep
     */

    module_stmt : MODULE_KEYWORD IDENTIFIER LEFT_CURLY_BRACE module_body* RIGHT_CURLY_BRACE;

    module_body : module_header_statement linkage_stmts meta_stmts revision_stmts body_stmts;

    /**
     * module-header-stmts = ;; these stmts can appear in any order
     *                       [yang-version-stmt stmtsep]
     *                        namespace-stmt stmtsep
     *                        prefix-stmt stmtsep
     */

    module_header_statement : yang_version_stmt? namespace_stmt prefix_stmt
                            | yang_version_stmt? prefix_stmt namespace_stmt
                            | namespace_stmt yang_version_stmt? prefix_stmt
                            | namespace_stmt prefix_stmt yang_version_stmt?
                            | prefix_stmt namespace_stmt yang_version_stmt?
                            | prefix_stmt yang_version_stmt? namespace_stmt?
                            ;

    /**
     * linkage-stmts       = ;; these stmts can appear in any order
     *                       *(import-stmt stmtsep)
     *                       *(include-stmt stmtsep)
     */

    linkage_stmts : (import_stmt
                  | include_stmt)*;

    /**
     * meta-stmts          = ;; these stmts can appear in any order
     *                       [organization-stmt stmtsep]
     *                       [contact-stmt stmtsep]
     *                       [description-stmt stmtsep]
     *                       [reference-stmt stmtsep]
     */

    meta_stmts : organization_stmt? contact_stmt? description_stmt? reference_stmt?
               | organization_stmt? contact_stmt? reference_stmt? description_stmt?
               | organization_stmt? description_stmt? contact_stmt? reference_stmt?
               | organization_stmt? description_stmt? reference_stmt? contact_stmt?
               | organization_stmt? reference_stmt? contact_stmt? description_stmt?
               | organization_stmt? reference_stmt? description_stmt? contact_stmt?
               | contact_stmt? organization_stmt? description_stmt? reference_stmt?
               | contact_stmt? organization_stmt? reference_stmt? description_stmt?
               | contact_stmt? reference_stmt? organization_stmt? description_stmt?
               | contact_stmt? reference_stmt? description_stmt? organization_stmt?
               | contact_stmt? description_stmt? reference_stmt? organization_stmt?
               | contact_stmt? description_stmt? organization_stmt? reference_stmt?
               | reference_stmt? contact_stmt? organization_stmt? description_stmt?
               | reference_stmt? contact_stmt? description_stmt? organization_stmt?
               | reference_stmt? organization_stmt? contact_stmt? description_stmt?
               | reference_stmt? organization_stmt? description_stmt? contact_stmt?
               | reference_stmt? description_stmt? organization_stmt? contact_stmt?
               | reference_stmt? description_stmt? contact_stmt? organization_stmt?
               | description_stmt? reference_stmt? contact_stmt? organization_stmt?
               | description_stmt? reference_stmt? organization_stmt? contact_stmt?
               | description_stmt? contact_stmt? reference_stmt? organization_stmt?
               | description_stmt? contact_stmt? organization_stmt? reference_stmt?
               | description_stmt? organization_stmt? contact_stmt? reference_stmt?
               | description_stmt? organization_stmt? reference_stmt? contact_stmt?
               ;

    // revision-stmts      = *(revision-stmt stmtsep)
    revision_stmts : revision_stmt*;

    /**
     * body-stmts          = *((extension-stmt /
     *                          feature-stmt /
     *                          identity-stmt /
     *                          typedef-stmt /
     *                          grouping-stmt /
     *                          data-def-stmt /
     *                          augment-stmt /
     *                          rpc-stmt /
     *                          notification-stmt /
     *                          deviation-stmt) stmtsep)
     */

    body_stmts : (extension_stmt
               | feature_stmt
               | identity_stmt
               | typedef_stmt
               | grouping_stmt
               | data_def_stmt
               | augment_stmt
               | rpc_stmt
               | notification_stmt
               | deviation_stmt)*
               ;

    /**
     * yang-version-stmt   = yang-version-keyword sep yang-version-arg-str
     *                       optsep stmtend
     */

    yang_version_stmt :   YANG_VERSION_KEYWORD INTEGER STMTEND;


    /**
     * namespace-stmt      = namespace-keyword sep uri-str optsep stmtend
     * For namespace validation TODO in Listener
     */
    namespace_stmt : NAMESPACE_KEYWORD string STMTEND;

    /**
     * prefix-stmt         = prefix-keyword sep prefix-arg-str
     *                       optsep stmtend
     */
    prefix_stmt : PREFIX_KEYWORD IDENTIFIER STMTEND;

    /**
     * import-stmt         = import-keyword sep identifier-arg-str optsep
     *                       "{" stmtsep
     *                           prefix-stmt stmtsep
     *                           [revision-date-stmt stmtsep]
     *                        "}"
     */
    import_stmt : IMPORT_KEYWORD IDENTIFIER LEFT_CURLY_BRACE import_stmt_body RIGHT_CURLY_BRACE;

    import_stmt_body : prefix_stmt revision_date_stmt?;

    // revision-date-stmt = revision-date-keyword sep revision-date stmtend
    revision_date_stmt : REVISION_DATE_KEYWORD DATE_ARG STMTEND;

    revision_date_stmt_body : revision_date_stmt;

    /**
     * include-stmt        = include-keyword sep identifier-arg-str optsep
     *                             (";" /
     *                              "{" stmtsep
     *                                  [revision-date-stmt stmtsep]
     *                            "}")
     */
    include_stmt : INCLUDE_KEYWORD IDENTIFIER (STMTEND | LEFT_CURLY_BRACE revision_date_stmt_body? RIGHT_CURLY_BRACE);

    /**
     * organization-stmt   = organization-keyword sep string
     *                            optsep stmtend
     */
    organization_stmt : ORGANIZATION_KEYWORD string STMTEND;

    // contact-stmt        = contact-keyword sep string optsep stmtend
    contact_stmt : CONTACT_KEYWORD string STMTEND;

    // description-stmt    = description-keyword sep string optsep stmtend
    description_stmt : DESCRIPTION_KEYWORD string STMTEND;

    // reference-stmt      = reference-keyword sep string optsep stmtend
    reference_stmt : REFERENCE_KEYWORD string STMTEND;

    /**
     * revision-stmt       = revision-keyword sep revision-date optsep
     *                             (";" /
     *                              "{" stmtsep
     *                                  [description-stmt stmtsep]
     *                                  [reference-stmt stmtsep]
     *                              "}")
     */
    revision_stmt : REVISION_KEYWORD DATE_ARG (STMTEND | LEFT_CURLY_BRACE revision_stmt_body RIGHT_CURLY_BRACE);
    revision_stmt_body : description_stmt? reference_stmt?;

    /**
     * submodule-stmt      = optsep submodule-keyword sep identifier-arg-str
     *                             optsep
     *                             "{" stmtsep
     *                                 submodule-header-stmts
     *                                 linkage-stmts
     *                                 meta-stmts
     *                                 revision-stmts
     *                                 body-stmts
     *                             "}" optsep
     */
    submodule_stmt : SUBMODULE_KEYWORD IDENTIFIER LEFT_CURLY_BRACE submodule_body* RIGHT_CURLY_BRACE;
    submodule_body : submodule_header_statement linkage_stmts meta_stmts revision_stmts body_stmts;

    /** submodule-header-stmts =
     *                            ;; these stmts can appear in any order
     *                            [yang-version-stmt stmtsep]
     *                             belongs-to-stmt stmtsep
     */
    submodule_header_statement : yang_version_stmt? belongs_to_stmt
                               | belongs_to_stmt yang_version_stmt?
                              ;

    /**
     * belongs-to-stmt     = belongs-to-keyword sep identifier-arg-str
     *                       optsep
     *                       "{" stmtsep
     *                           prefix-stmt stmtsep
     *                       "}"
     */
    belongs_to_stmt : BELONGS_TO_KEYWORD IDENTIFIER LEFT_CURLY_BRACE belongs_to_stmt_body RIGHT_CURLY_BRACE;
    belongs_to_stmt_body : prefix_stmt;

    /**
     * extension-stmt      = extension-keyword sep identifier-arg-str optsep
     *                       (";" /
     *                        "{" stmtsep
     *                            ;; these stmts can appear in any order
     *                            [argument-stmt stmtsep]
     *                            [status-stmt stmtsep]
     *                            [description-stmt stmtsep]
     *                            [reference-stmt stmtsep]
     *                        "}")
     */
    extension_stmt : EXTENSION_KEYWORD IDENTIFIER (STMTEND | LEFT_CURLY_BRACE extension_body RIGHT_CURLY_BRACE);
    extension_body : argument_stmt? status_stmt? description_stmt? reference_stmt?
                   | argument_stmt? status_stmt? reference_stmt? description_stmt?
                   | argument_stmt? description_stmt? status_stmt? reference_stmt?
                   | argument_stmt? description_stmt? reference_stmt? status_stmt?
                   | argument_stmt? reference_stmt? description_stmt? status_stmt?
                   | argument_stmt? reference_stmt? status_stmt? description_stmt?
                   | status_stmt? reference_stmt? argument_stmt? description_stmt?
                   | status_stmt? reference_stmt? description_stmt? argument_stmt?
                   | status_stmt? description_stmt? reference_stmt? argument_stmt?
                   | status_stmt? description_stmt? argument_stmt? reference_stmt?
                   | status_stmt? argument_stmt? reference_stmt? description_stmt?
                   | status_stmt? argument_stmt? description_stmt? reference_stmt?
                   | description_stmt? argument_stmt? status_stmt? reference_stmt?
                   | description_stmt? argument_stmt? reference_stmt? status_stmt?
                   | description_stmt? status_stmt? argument_stmt? reference_stmt?
                   | description_stmt? status_stmt? reference_stmt? argument_stmt?
                   | description_stmt? reference_stmt? status_stmt? argument_stmt?
                   | description_stmt? reference_stmt? argument_stmt? status_stmt?
                   | reference_stmt? description_stmt? argument_stmt? status_stmt?
                   | reference_stmt? description_stmt? status_stmt? argument_stmt?
                   | reference_stmt? status_stmt? argument_stmt? description_stmt?
                   | reference_stmt? status_stmt? description_stmt? argument_stmt?
                   | reference_stmt? argument_stmt? description_stmt? status_stmt?
                   | reference_stmt? argument_stmt? status_stmt? description_stmt?
                   ;

    /**
     * argument-stmt       = argument-keyword sep identifier-arg-str optsep
     *                       (";" /
     *                        "{" stmtsep
     *                            [yin-element-stmt stmtsep]
     *                        "}")
     */
    argument_stmt : ARGUMENT_KEYWORD IDENTIFIER (STMTEND | LEFT_CURLY_BRACE argument_body RIGHT_CURLY_BRACE);
    argument_body : yin_element_stmt?;

    /**
     * yin-element-stmt    = yin-element-keyword sep yin-element-arg-str
     *                       stmtend
     */
    yin_element_stmt : YIN_ELEMENT_KEYWORD (TRUE_KEYWORD | FALSE_KEYWORD) STMTEND;

    /**
     * identity-stmt       = identity-keyword sep identifier-arg-str optsep
     *                       (";" /
     *                        "{" stmtsep
     *                            ;; these stmts can appear in any order
     *                            [base-stmt stmtsep]
     *                            [status-stmt stmtsep]
     *                            [description-stmt stmtsep]
     *                            [reference-stmt stmtsep]
     *                        "}")
     */
    identity_stmt : IDENTITY_KEYWORD IDENTIFIER (STMTEND | LEFT_CURLY_BRACE identity_body RIGHT_CURLY_BRACE);
    identity_body : base_stmt? status_stmt? description_stmt? reference_stmt?
                  | base_stmt? status_stmt? reference_stmt? description_stmt?
                  | base_stmt? description_stmt? status_stmt? reference_stmt?
                  | base_stmt? description_stmt? reference_stmt? status_stmt?
                  | base_stmt? reference_stmt? description_stmt? status_stmt?
                  | base_stmt? reference_stmt? status_stmt? description_stmt?
                  | reference_stmt? base_stmt? status_stmt? description_stmt?
                  | reference_stmt? base_stmt? description_stmt? status_stmt?
                  | reference_stmt? status_stmt? base_stmt? description_stmt?
                  | reference_stmt? status_stmt? description_stmt? base_stmt?
                  | reference_stmt? description_stmt? status_stmt? base_stmt?
                  | reference_stmt? description_stmt? base_stmt? status_stmt?
                  | description_stmt? reference_stmt? status_stmt? base_stmt?
                  | description_stmt? reference_stmt? status_stmt? base_stmt?
                  | description_stmt? reference_stmt? base_stmt? status_stmt?
                  | description_stmt? status_stmt? base_stmt? reference_stmt?
                  | description_stmt? status_stmt? reference_stmt? base_stmt?
                  | description_stmt? base_stmt? reference_stmt? status_stmt?
                  | description_stmt? base_stmt? status_stmt? reference_stmt?
                  | status_stmt? base_stmt? description_stmt? reference_stmt?
                  | status_stmt? base_stmt? reference_stmt? description_stmt?
                  | status_stmt? description_stmt? base_stmt? reference_stmt?
                  | status_stmt? description_stmt? reference_stmt? base_stmt?
                  | status_stmt? reference_stmt? description_stmt? base_stmt?
                  | status_stmt? reference_stmt? base_stmt? description_stmt?
                  ;

    /**
     * base-stmt           = base-keyword sep identifier-ref-arg-str
     *                          optsep stmtend*
     * identifier-ref-arg  = [prefix ":"] identifier
     */
    base_stmt : BASE_KEYWORD string STMTEND;

    /**
     *  feature-stmt        = feature-keyword sep identifier-arg-str optsep
     *                        (";" /
     *                         "{" stmtsep
     *                             ;; these stmts can appear in any order
     *                             *(if-feature-stmt stmtsep)
     *                             [status-stmt stmtsep]
     *                             [description-stmt stmtsep]
     *                             [reference-stmt stmtsep]
     *                         "}")
     */
    feature_stmt : FEATURE_KEYWORD string (STMTEND | LEFT_CURLY_BRACE feature_body RIGHT_CURLY_BRACE);
    feature_body : if_feature_stmt* status_stmt? description_stmt? reference_stmt?
                 | if_feature_stmt* status_stmt? reference_stmt? description_stmt?
                 | if_feature_stmt* description_stmt? status_stmt? reference_stmt?
                 | if_feature_stmt* description_stmt? reference_stmt? status_stmt?
                 | if_feature_stmt* reference_stmt? status_stmt? description_stmt?
                 | if_feature_stmt* reference_stmt? description_stmt? status_stmt?
                 | status_stmt? if_feature_stmt* description_stmt? reference_stmt?
                 | status_stmt? if_feature_stmt* reference_stmt? description_stmt?
                 | status_stmt? description_stmt? if_feature_stmt* reference_stmt?
                 | status_stmt? description_stmt? reference_stmt? if_feature_stmt*
                 | status_stmt? reference_stmt? if_feature_stmt* description_stmt?
                 | status_stmt? reference_stmt? description_stmt? if_feature_stmt*
                 | description_stmt? if_feature_stmt* status_stmt? reference_stmt?
                 | description_stmt? if_feature_stmt* reference_stmt? status_stmt?
                 | description_stmt? status_stmt? if_feature_stmt* reference_stmt?
                 | description_stmt? status_stmt? reference_stmt? if_feature_stmt*
                 | description_stmt? reference_stmt* status_stmt? if_feature_stmt*
                 | description_stmt? reference_stmt* if_feature_stmt? status_stmt?
                 | reference_stmt? if_feature_stmt* status_stmt? description_stmt?
                 | reference_stmt? if_feature_stmt* description_stmt? status_stmt?
                 | reference_stmt? description_stmt? status_stmt? if_feature_stmt*
                 | reference_stmt? description_stmt? if_feature_stmt* status_stmt?
                 | reference_stmt? status_stmt? description_stmt? if_feature_stmt*
                 | reference_stmt? status_stmt? if_feature_stmt* description_stmt?
                 ;

    /**
     *  data-def-stmt       = container-stmt /
     *                       leaf-stmt /
     *                       leaf-list-stmt /
     *                       list-stmt /
     *                       choice-stmt /
     *                       anyxml-stmt /
     *                       uses-stmt
     */
    data_def_stmt : container_stmt
                    | leaf_stmt
                    | leaf_list_stmt
                    | list_stmt
                    | choice_stmt
                    | uses_stmt;

    /**
     *  if-feature-stmt     = if-feature-keyword sep identifier-ref-arg-str
     *                        optsep stmtend
     */
    if_feature_stmt : IF_FEATURE_KEYWORD string STMTEND;

    /**
    *    units-stmt          = units-keyword sep string optsep stmtend
    */
    units_stmt : UNITS_KEYWORD string STMTEND;

    /**
     *   typedef-stmt        = typedef-keyword sep identifier-arg-str optsep
     *                         "{" stmtsep
     *                             ;; these stmts can appear in any order
     *                             type-stmt stmtsep
     *                            [units-stmt stmtsep]
     *                             [default-stmt stmtsep]
     *                             [status-stmt stmtsep]
     *                             [description-stmt stmtsep]
     *                             [reference-stmt stmtsep]
     *                           "}"
     * TODO : 0..1 occurance to be validated in listener
     */
    typedef_stmt : TYPEDEF_KEYWORD IDENTIFIER LEFT_CURLY_BRACE
                   (type_stmt | units_stmt | default_stmt | status_stmt | description_stmt | reference_stmt)*
                   RIGHT_CURLY_BRACE;

    /**
     *  type-stmt           = type-keyword sep identifier-ref-arg-str optsep
     *                        (";" /
     *                         "{" stmtsep
     *                            type-body-stmts
     *                         "}")
     */
    type_stmt : TYPE_KEYWORD string (STMTEND | LEFT_CURLY_BRACE type_body_stmts RIGHT_CURLY_BRACE);

    /**
     *  type-body-stmts     = numerical-restrictions /
     *                        decimal64-specification /
     *                       string-restrictions /
     *                        enum-specification /
     *                        leafref-specification /
     *                        identityref-specification /
     *                        instance-identifier-specification /
     *                        bits-specification /
     *                        union-specification
     * TODO : decimal64-specification to be added
     */
    type_body_stmts : numerical_restrictions | string_restrictions | enum_specification
                    | leafref_specification | identityref_specification | instance_identifier_specification
                    | bits_specification | union_specification;

    /**
     *  numerical-restrictions = range-stmt stmtsep
     */
    numerical_restrictions : range_stmt;

    /**
     *  range-stmt          = range-keyword sep range-arg-str optsep
     *                        (";" /
     *                         "{" stmtsep
     *                             ;; these stmts can appear in any order
     *                             [error-message-stmt stmtsep]
     *                             [error-app-tag-stmt stmtsep]
     *                             [description-stmt stmtsep]
     *                             [reference-stmt stmtsep]
     *                          "}")
     */
    range_stmt : RANGE_KEYWORD string (STMTEND | LEFT_CURLY_BRACE common_stmts RIGHT_CURLY_BRACE);

    common_stmts : error_message_stmt? error_app_tag_stmt? description_stmt? reference_stmt?
                 | error_message_stmt? error_app_tag_stmt? reference_stmt? description_stmt?
                 | error_message_stmt? description_stmt? error_app_tag_stmt? reference_stmt?
                 | error_message_stmt? description_stmt? reference_stmt? error_app_tag_stmt?
                 | error_message_stmt? reference_stmt? error_app_tag_stmt? description_stmt?
                 | error_message_stmt? reference_stmt? description_stmt? error_app_tag_stmt?
                 | error_app_tag_stmt? error_message_stmt? description_stmt? reference_stmt?
                 | error_app_tag_stmt? error_message_stmt? reference_stmt? description_stmt?
                 | error_app_tag_stmt? description_stmt? description_stmt? error_message_stmt?
                 | error_app_tag_stmt? description_stmt? error_message_stmt? description_stmt?
                 | error_app_tag_stmt? reference_stmt? error_message_stmt? description_stmt?
                 | error_app_tag_stmt? reference_stmt? description_stmt? error_message_stmt?
                 | description_stmt? error_message_stmt? error_app_tag_stmt? reference_stmt?
                 | description_stmt? error_message_stmt? reference_stmt? error_app_tag_stmt?
                 | description_stmt? error_app_tag_stmt? error_message_stmt? reference_stmt?
                 | description_stmt? error_app_tag_stmt? reference_stmt? error_message_stmt?
                 | description_stmt? reference_stmt? error_message_stmt? error_app_tag_stmt?
                 | description_stmt? reference_stmt? error_app_tag_stmt? error_message_stmt?
                 | reference_stmt? error_message_stmt? description_stmt? error_app_tag_stmt?
                 | reference_stmt? error_message_stmt? error_app_tag_stmt? description_stmt?
                 | reference_stmt? error_app_tag_stmt? description_stmt? error_message_stmt?
                 | reference_stmt? error_app_tag_stmt? error_message_stmt? description_stmt?
                 | reference_stmt? description_stmt? error_message_stmt? error_app_tag_stmt?
                 | reference_stmt? description_stmt? error_app_tag_stmt? error_message_stmt?
                 ;

    /**
     *  string-restrictions = ;; these stmts can appear in any order
     *                        [length-stmt stmtsep]
     *                        *(pattern-stmt stmtsep)
     */
    string_restrictions : ((length_stmt)? (pattern_stmt)*) | ((pattern_stmt)* (length_stmt)?);

    /**
     *  length-stmt         = length-keyword sep length-arg-str optsep
     *                        (";" /
     *                         "{" stmtsep
     *                             ;; these stmts can appear in any order
     *                             [error-message-stmt stmtsep]
     *                             [error-app-tag-stmt stmtsep]
     *                             [description-stmt stmtsep]
     *                             [reference-stmt stmtsep]
     *                          "}")
     */
    length_stmt : LENGTH_KEYWORD string
                  (STMTEND | LEFT_CURLY_BRACE common_stmts RIGHT_CURLY_BRACE);

    /**
     *  pattern-stmt        = pattern-keyword sep string optsep
     *                        (";" /
     *                         "{" stmtsep
     *                             ;; these stmts can appear in any order
     *                             [error-message-stmt stmtsep]
     *                             [error-app-tag-stmt stmtsep]
     *                             [description-stmt stmtsep]
     *                             [reference-stmt stmtsep]
     *                          "}")
     */
    pattern_stmt : PATTERN_KEYWORD string (STMTEND | LEFT_CURLY_BRACE common_stmts RIGHT_CURLY_BRACE);

    /**
     *  default-stmt        = default-keyword sep string stmtend
     */
    default_stmt : DEFAULT_KEYWORD string STMTEND;

    /**
     *  enum-specification  = 1*(enum-stmt stmtsep)
     */
    enum_specification : enum_stmt+;

    /**
     *  enum-stmt           = enum-keyword sep string optsep
     *                        (";" /
     *                         "{" stmtsep
     *                             ;; these stmts can appear in any order
     *                             [value-stmt stmtsep]
     *                             [status-stmt stmtsep]
     *                             [description-stmt stmtsep]
     *                             [reference-stmt stmtsep]
     *                          "}")
     */
    enum_stmt : ENUM_KEYWORD string (STMTEND | LEFT_CURLY_BRACE enum_stmt_body RIGHT_CURLY_BRACE);

    enum_stmt_body : value_stmt? status_stmt? description_stmt? reference_stmt?
                   | value_stmt? status_stmt? reference_stmt? description_stmt?
                   | value_stmt? description_stmt? status_stmt? reference_stmt?
                   | value_stmt? description_stmt? reference_stmt? status_stmt?
                   | value_stmt? reference_stmt? status_stmt? description_stmt?
                   | value_stmt? reference_stmt? description_stmt? status_stmt?
                   | status_stmt? value_stmt? description_stmt? reference_stmt?
                   | status_stmt? value_stmt? reference_stmt? description_stmt?
                   | status_stmt? description_stmt? description_stmt? value_stmt?
                   | status_stmt? description_stmt? value_stmt? description_stmt?
                   | status_stmt? reference_stmt? value_stmt? description_stmt?
                   | status_stmt? reference_stmt? description_stmt? value_stmt?
                   | description_stmt? value_stmt? status_stmt? reference_stmt?
                   | description_stmt? value_stmt? reference_stmt? status_stmt?
                   | description_stmt? status_stmt? value_stmt? reference_stmt?
                   | description_stmt? status_stmt? reference_stmt? value_stmt?
                   | description_stmt? reference_stmt? value_stmt? status_stmt?
                   | description_stmt? reference_stmt? status_stmt? value_stmt?
                   | reference_stmt? value_stmt? description_stmt? status_stmt?
                   | reference_stmt? value_stmt? status_stmt? description_stmt?
                   | reference_stmt? status_stmt? description_stmt? value_stmt?
                   | reference_stmt? status_stmt? value_stmt? description_stmt?
                   | reference_stmt? description_stmt? value_stmt? status_stmt?
                   | reference_stmt? description_stmt? status_stmt? value_stmt?
                   ;

    /**
     *  leafref-specification =
     *                        ;; these stmts can appear in any order
     *                        path-stmt stmtsep
     *                        [require-instance-stmt stmtsep]
     */
    leafref_specification : (path_stmt (require_instance_stmt)?) | ((require_instance_stmt)? path_stmt);

    /**
     *  path-stmt           = path-keyword sep path-arg-str stmtend
     */
    path_stmt : PATH_KEYWORD string STMTEND;

    /**
     *  require-instance-stmt = require-instance-keyword sep
     *                           require-instance-arg-str stmtend
     *  require-instance-arg-str = < a string that matches the rule
     *                             require-instance-arg >
     *  require-instance-arg = true-keyword / false-keyword
     */
    require_instance_stmt : REQUIRE_INSTANCE_KEYWORD (TRUE_KEYWORD | FALSE_KEYWORD) STMTEND;

    /**
     *  instance-identifier-specification =
     *                        [require-instance-stmt stmtsep]
     */
    instance_identifier_specification : require_instance_stmt?;

    /**
     * identityref-specification =
     *                        base-stmt stmtsep
     */
    identityref_specification : base_stmt;

    /**
     *  union-specification = 1*(type-stmt stmtsep)
     */
    union_specification : type_stmt+;

    /**
     *  bits-specification  = 1*(bit-stmt stmtsep)
     */
    bits_specification : bit_stmt+;

    /**
     * bit-stmt            = bit-keyword sep identifier-arg-str optsep
     *                       (";" /
     *                        "{" stmtsep
     *                            ;; these stmts can appear in any order
     *                            [position-stmt stmtsep]
     *                            [status-stmt stmtsep]
     *                            [description-stmt stmtsep]
     *                            [reference-stmt stmtsep]
     *                          "}"
     *                        "}")
     */
    bit_stmt : BIT_KEYWORD IDENTIFIER (STMTEND | LEFT_CURLY_BRACE bit_body_stmt RIGHT_CURLY_BRACE);

    bit_body_stmt : position_stmt? status_stmt? description_stmt? reference_stmt?
                  | position_stmt? status_stmt? reference_stmt? description_stmt?
                  | position_stmt? description_stmt? status_stmt? reference_stmt?
                  | position_stmt? description_stmt? reference_stmt? status_stmt?
                  | position_stmt? reference_stmt? status_stmt? description_stmt?
                  | position_stmt? reference_stmt? description_stmt? status_stmt?
                  | status_stmt? position_stmt? description_stmt? reference_stmt?
                  | status_stmt? position_stmt? reference_stmt? description_stmt?
                  | status_stmt? description_stmt? description_stmt? position_stmt?
                  | status_stmt? description_stmt? position_stmt? description_stmt?
                  | status_stmt? reference_stmt? position_stmt? description_stmt?
                  | status_stmt? reference_stmt? description_stmt? position_stmt?
                  | description_stmt? position_stmt? status_stmt? reference_stmt?
                  | description_stmt? position_stmt? reference_stmt? status_stmt?
                  | description_stmt? status_stmt? position_stmt? reference_stmt?
                  | description_stmt? status_stmt? reference_stmt? position_stmt?
                  | description_stmt? reference_stmt? position_stmt? status_stmt?
                  | description_stmt? reference_stmt? status_stmt? position_stmt?
                  | reference_stmt? position_stmt? description_stmt? status_stmt?
                  | reference_stmt? position_stmt? status_stmt? description_stmt?
                  | reference_stmt? status_stmt? description_stmt? position_stmt?
                  | reference_stmt? status_stmt? position_stmt? description_stmt?
                  | reference_stmt? description_stmt? position_stmt? status_stmt?
                  | reference_stmt? description_stmt? status_stmt? position_stmt?
                  ;

    /**
     *  position-stmt       = position-keyword sep
     *                        position-value-arg-str stmtend
     *  position-value-arg-str = < a string that matches the rule
     *                              position-value-arg >
     *  position-value-arg  = non-negative-integer-value
     */
    position_stmt : POSITION_KEYWORD INTEGER STMTEND;

    /**
     *  status-stmt         = status-keyword sep status-arg-str stmtend
     *  status-arg-str      = < a string that matches the rule
     *                         status-arg >
     *  status-arg          = current-keyword /
     *                        obsolete-keyword /
     *                        deprecated-keyword
     */
    status_stmt : STATUS_KEYWORD (CURRENT_KEYWORD | OBSOLETE_KEYWORD | DEPRECATED_KEYWORD) STMTEND;

    /**
     *  config-stmt         = config-keyword sep
     *                        config-arg-str stmtend
     *  config-arg-str      = < a string that matches the rule
     *                          config-arg >
     *  config-arg          = true-keyword / false-keyword
     */
    config_stmt : CONFIG_KEYWORD (TRUE_KEYWORD | FALSE_KEYWORD) STMTEND;

    /**
     *  mandatory-stmt      = mandatory-keyword sep
     *                        mandatory-arg-str stmtend
     * 
     *  mandatory-arg-str   = < a string that matches the rule
     *                          mandatory-arg >
     * 
     *  mandatory-arg       = true-keyword / false-keyword 
     */
    mandatory_stmt : MANDATORY_KEYWORD (TRUE_KEYWORD | FALSE_KEYWORD) STMTEND;

    /**
     *  presence-stmt       = presence-keyword sep string stmtend
     */
    presence_stmt : PRESENCE_KEYWORD string STMTEND;

    /**
     *  ordered-by-stmt     = ordered-by-keyword sep
     *                        ordered-by-arg-str stmtend
     * 
     *  ordered-by-arg-str  = < a string that matches the rule
     *                          ordered-by-arg >
     * 
     *  ordered-by-arg      = user-keyword / system-keyword
     */
    ordered_by_stmt : ORDERED_BY_KEYWORD (USER_KEYWORD | SYSTEM_KEYWORD) STMTEND;

    /**
     *  must-stmt           = must-keyword sep string optsep
     *                        (";" /
     *                         "{" stmtsep
     *                             ;; these stmts can appear in any order
     *                             [error-message-stmt stmtsep]
     *                             [error-app-tag-stmt stmtsep]
     *                             [description-stmt stmtsep]
     *                             [reference-stmt stmtsep]
     *                          "}")
     */
    must_stmt : MUST_KEYWORD string (STMTEND | LEFT_CURLY_BRACE common_stmts RIGHT_CURLY_BRACE);

    /**
     *   error-message-stmt  = error-message-keyword sep string stmtend
     */
    error_message_stmt : ERROR_MESSAGE_KEYWORD string STMTEND;

    /**
     *  error-app-tag-stmt  = error-app-tag-keyword sep string stmtend
     */
    error_app_tag_stmt : ERROR_APP_TAG_KEYWORD string STMTEND;

    /**
     *  min-elements-stmt   = min-elements-keyword sep
     *                        min-value-arg-str stmtend
     *  min-value-arg-str   = < a string that matches the rule
     *                          min-value-arg >
     *  min-value-arg       = non-negative-integer-value
     */
    min_elements_stmt : MIN_ELEMENTS_KEYWORD INTEGER STMTEND;

    /**
     *  max-elements-stmt   = max-elements-keyword sep
     *                        max-value-arg-str stmtend
     *  max-value-arg-str   = < a string that matches the rule
     *                          max-value-arg >
     
     */
    max_elements_stmt :  MAX_ELEMENTS_KEYWORD max_value_arg STMTEND;

    /**
     *  max-value-arg       = unbounded-keyword /
     *                        positive-integer-value
     */
    max_value_arg : UNBOUNDED_KEYWORD | INTEGER;

    /**
     *  value-stmt          = value-keyword sep integer-value stmtend
     */
    value_stmt : VALUE_KEYWORD ((MINUS INTEGER) | INTEGER) STMTEND;

    /**
     *   grouping-stmt       = grouping-keyword sep identifier-arg-str optsep
     *                        (";" /
     *                         "{" stmtsep
     *                            ;; these stmts can appear in any order
     *                            [status-stmt stmtsep]
     *                             [description-stmt stmtsep]
     *                             [reference-stmt stmtsep]
     *                             *((typedef-stmt /
     *                                grouping-stmt) stmtsep)
     *                             *(data-def-stmt stmtsep)
     *                         "}")
     * TODO : 0..1 occurance to be checked in listener
     */
    grouping_stmt : GROUPING_KEYWORD IDENTIFIER (STMTEND | LEFT_CURLY_BRACE
                      (status_stmt | description_stmt | reference_stmt | typedef_stmt | grouping_stmt
                       | data_def_stmt)* RIGHT_CURLY_BRACE);

    /**
     *  container-stmt      = container-keyword sep identifier-arg-str optsep
     *                        (";" /
     *                         "{" stmtsep
     *                             ;; these stmts can appear in any order
     *                             [when-stmt stmtsep]
     *                             *(if-feature-stmt stmtsep)
     *                             *(must-stmt stmtsep)
     *                             [presence-stmt stmtsep]
     *                             [config-stmt stmtsep]
     *                             [status-stmt stmtsep]
     *                             [description-stmt stmtsep]
     *                             [reference-stmt stmtsep]
     *                             *((typedef-stmt /
     *                                grouping-stmt) stmtsep)
     *                             *(data-def-stmt stmtsep)
     *                         "}")
     * TODO : 0..1 occurance to be checked in listener
     */
    container_stmt : CONTAINER_KEYWORD IDENTIFIER
                     (STMTEND | LEFT_CURLY_BRACE (when_stmt | if_feature_stmt | must_stmt | presence_stmt | config_stmt
                     | status_stmt | description_stmt | reference_stmt | typedef_stmt | grouping_stmt
                     | data_def_stmt)* RIGHT_CURLY_BRACE);

    /**
     *  leaf-stmt           = leaf-keyword sep identifier-arg-str optsep
     *                        "{" stmtsep
     *                            ;; these stmts can appear in any order
     *                            [when-stmt stmtsep]
     *                            *(if-feature-stmt stmtsep)
     *                            type-stmt stmtsep
     *                            [units-stmt stmtsep]
     *                            *(must-stmt stmtsep)
     *                            [default-stmt stmtsep]
     *                            [config-stmt stmtsep]
     *                            [mandatory-stmt stmtsep]
     *                            [status-stmt stmtsep]
     *                            [description-stmt stmtsep]
     *                            [reference-stmt stmtsep]
     *                         "}"
     * TODO : 0..1 occurance to be checked in listener
     */
    leaf_stmt : LEAF_KEYWORD IDENTIFIER LEFT_CURLY_BRACE (when_stmt | if_feature_stmt | type_stmt | units_stmt
              | must_stmt | default_stmt | config_stmt | mandatory_stmt | status_stmt  | description_stmt
              | reference_stmt)* RIGHT_CURLY_BRACE;

    /**
     *  leaf-list-stmt      = leaf-list-keyword sep identifier-arg-str optsep
     *                        "{" stmtsep
     *                            ;; these stmts can appear in any order
     *                            [when-stmt stmtsep]
     *                            *(if-feature-stmt stmtsep)
     *                            type-stmt stmtsep
     *                            [units-stmt stmtsep]
     *                            *(must-stmt stmtsep)
     *                            [config-stmt stmtsep]
     *                            [min-elements-stmt stmtsep]
     *                            [max-elements-stmt stmtsep]
     *                            [ordered-by-stmt stmtsep]
     *                            [status-stmt stmtsep]
     *                            [description-stmt stmtsep]
     *                            [reference-stmt stmtsep]
     *                         "}"
     * TODO : 0..1 occurance to be checked in listener
     */
    leaf_list_stmt : LEAF_LIST_KEYWORD IDENTIFIER LEFT_CURLY_BRACE (when_stmt | if_feature_stmt | type_stmt
                     | units_stmt | must_stmt | config_stmt | min_elements_stmt | max_elements_stmt | ordered_by_stmt
                     | status_stmt | description_stmt | reference_stmt)* RIGHT_CURLY_BRACE;

    /**
     *  list-stmt           = list-keyword sep identifier-arg-str optsep
     *                        "{" stmtsep
     *                            ;; these stmts can appear in any order
     *                            [when-stmt stmtsep]
     *                            *(if-feature-stmt stmtsep)
     *                            *(must-stmt stmtsep)
     *                            [key-stmt stmtsep]
     *                            *(unique-stmt stmtsep)
     *                            [config-stmt stmtsep]
     *                            [min-elements-stmt stmtsep]
     *                            [max-elements-stmt stmtsep]
     *                            [ordered-by-stmt stmtsep]
     *                            [status-stmt stmtsep]
     *                            [description-stmt stmtsep]
     *                            [reference-stmt stmtsep]
     *                            *((typedef-stmt /
     *                               grouping-stmt) stmtsep)
     *                            1*(data-def-stmt stmtsep)
     *                         "}"
     * TODO : 0..1 occurance to be checked in listener
     */
    list_stmt : LIST_KEYWORD IDENTIFIER LEFT_CURLY_BRACE (when_stmt | if_feature_stmt | must_stmt | key_stmt
              | unique_stmt | config_stmt | min_elements_stmt | max_elements_stmt | ordered_by_stmt | status_stmt
              | description_stmt | reference_stmt | typedef_stmt | grouping_stmt| data_def_stmt)* RIGHT_CURLY_BRACE;

    /**
     *  key-stmt            = key-keyword sep key-arg-str stmtend
     */
    key_stmt : KEY_KEYWORD string STMTEND;

    /**
     *  unique-stmt         = unique-keyword sep unique-arg-str stmtend
     */
    unique_stmt: UNIQUE_KEYWORD string STMTEND;

    /**
     *  choice-stmt         = choice-keyword sep identifier-arg-str optsep
     *                        (";" /
     *                         "{" stmtsep
     *                             ;; these stmts can appear in any order
     *                             [when-stmt stmtsep]
     *                             *(if-feature-stmt stmtsep)
     *                             [default-stmt stmtsep]
     *                             [config-stmt stmtsep]
     *                             [mandatory-stmt stmtsep]
     *                             [status-stmt stmtsep]
     *                             [description-stmt stmtsep]
     *                             [reference-stmt stmtsep]
     *                             *((short-case-stmt / case-stmt) stmtsep)
     *                         "}")
     * TODO : 0..1 occurance to be checked in listener
     */
    choice_stmt : CHOICE_KEYWORD IDENTIFIER (STMTEND | LEFT_CURLY_BRACE (when_stmt | if_feature_stmt | default_stmt
                  | config_stmt | mandatory_stmt | status_stmt | description_stmt | reference_stmt | short_case_stmt
                  | case_stmt)* RIGHT_CURLY_BRACE);

    /**
     *  short-case-stmt     = container-stmt /
     *                        leaf-stmt /
     *                        leaf-list-stmt /
     *                        list-stmt /
     *                        anyxml-stmt
     */
    short_case_stmt : container_stmt | leaf_stmt | leaf_list_stmt | list_stmt;

    /**
     *  case-stmt           = case-keyword sep identifier-arg-str optsep
     *                        (";" /
     *                         "{" stmtsep
     *                             ;; these stmts can appear in any order
     *                             [when-stmt stmtsep]
     *                             *(if-feature-stmt stmtsep)
     *                             [status-stmt stmtsep]
     *                             [description-stmt stmtsep]
     *                             [reference-stmt stmtsep]
     *                             *(data-def-stmt stmtsep)
     *                         "}")
     * TODO : 0..1 occurance to be checked in listener
     */
    case_stmt : CASE_KEYWORD IDENTIFIER (STMTEND | LEFT_CURLY_BRACE (when_stmt | if_feature_stmt | status_stmt
              | description_stmt | reference_stmt | data_def_stmt)* RIGHT_CURLY_BRACE);

    /**
     *  uses-stmt           = uses-keyword sep identifier-ref-arg-str optsep
     *                        (";" /
     *                         "{" stmtsep
     *                             ;; these stmts can appear in any order
     *                             [when-stmt stmtsep]
     *                             *(if-feature-stmt stmtsep)
     *                             [status-stmt stmtsep]
     *                             [description-stmt stmtsep]
     *                             [reference-stmt stmtsep]
     *                             *(refine-stmt stmtsep)
     *                             *(uses-augment-stmt stmtsep)
     *                         "}")
     * TODO : 0..1 occurance to be checked in listener
     */
    uses_stmt : USES_KEYWORD string (STMTEND | LEFT_CURLY_BRACE (when_stmt | if_feature_stmt | status_stmt
                | description_stmt | reference_stmt | refine_stmt | uses_augment_stmt)* RIGHT_CURLY_BRACE);

    /**
     *  refine-stmt         = refine-keyword sep refine-arg-str optsep
     *                        (";" /
     *                         "{" stmtsep
     *                             (refine-container-stmts /
     *                              refine-leaf-stmts /
     *                              refine-leaf-list-stmts /
     *                              refine-list-stmts /
     *                              refine-choice-stmts /
     *                              refine-case-stmts /
     *                              refine-anyxml-stmts)
     *                         "}")
     */
    refine_stmt : REFINE_KEYWORD string (STMTEND  | LEFT_CURLY_BRACE (refine_container_stmts | refine_leaf_stmts
                  | refine_leaf_list_stmts | refine_list_stmts | refine_choice_stmts | refine_case_stmts)
                  RIGHT_CURLY_BRACE);

    /**
     *  refine-container-stmts =
     *                        ;; these stmts can appear in any order
     *                        *(must-stmt stmtsep)
     *                        [presence-stmt stmtsep]
     *                        [config-stmt stmtsep]
     *                        [description-stmt stmtsep]
     *                         [reference-stmt stmtsep]
     * TODO : 0..1 occurance to be checked in listener
     */
    refine_container_stmts : (must_stmt | presence_stmt | config_stmt | description_stmt | reference_stmt)* ;

    /**
     *   refine-leaf-stmts   = ;; these stmts can appear in any order
     *                         *(must-stmt stmtsep)
     *                         [default-stmt stmtsep]
     *                         [config-stmt stmtsep]
     *                        [mandatory-stmt stmtsep]
     *                        [description-stmt stmtsep]
     *                        [reference-stmt stmtsep]
     * TODO : 0..1 occurance to be checked in listener
     */
    refine_leaf_stmts : (must_stmt | default_stmt | config_stmt | mandatory_stmt | description_stmt | reference_stmt)*;

    /**
     *  refine-leaf-list-stmts =
     *                        ;; these stmts can appear in any order
     *                        *(must-stmt stmtsep)
     *                        [config-stmt stmtsep]
     *                        [min-elements-stmt stmtsep]
     *                        [max-elements-stmt stmtsep]
     *                        [description-stmt stmtsep]
     *                        [reference-stmt stmtsep]
     * TODO : 0..1 occurance to be checked in listener
     */
    refine_leaf_list_stmts : (must_stmt | config_stmt | min_elements_stmt | max_elements_stmt | description_stmt
                             | reference_stmt)*;

    /**
     *  refine-list-stmts   = ;; these stmts can appear in any order
     *                        *(must-stmt stmtsep)
     *                        [config-stmt stmtsep]
     *                        [min-elements-stmt stmtsep]
     *                        [max-elements-stmt stmtsep]
     *                        [description-stmt stmtsep]
     *                        [reference-stmt stmtsep]
     * TODO : 0..1 occurance to be checked in listener
     */
    refine_list_stmts : (must_stmt | config_stmt | min_elements_stmt | max_elements_stmt | description_stmt
                        | reference_stmt)*;

    /**
     *  refine-choice-stmts = ;; these stmts can appear in any order
     *                        [default-stmt stmtsep]
     *                        [config-stmt stmtsep]
     *                        [mandatory-stmt stmtsep]
     *                        [description-stmt stmtsep]
     *                        [reference-stmt stmtsep]
     * TODO : 0..1 occurance to be checked in listener
     */
    refine_choice_stmts : (default_stmt | config_stmt | mandatory_stmt | description_stmt | reference_stmt)*;

    /**
     *  refine-case-stmts   = ;; these stmts can appear in any order
     *                        [description-stmt stmtsep]
     *                        [reference-stmt stmtsep]
     *
     */
    refine_case_stmts : (description_stmt | reference_stmt)? | (reference_stmt | description_stmt)?;

    /**
     *  uses-augment-stmt   = augment-keyword sep uses-augment-arg-str optsep
     *                        "{" stmtsep
     *                            ;; these stmts can appear in any order
     *                            [when-stmt stmtsep]
     *                            *(if-feature-stmt stmtsep)
     *                            [status-stmt stmtsep]
     *                            [description-stmt stmtsep]
     *                            [reference-stmt stmtsep]
     *                            1*((data-def-stmt stmtsep) /
     *                               (case-stmt stmtsep))
     *                         "}"
     * TODO : 0..1 occurance to be checked in listener
     */
    uses_augment_stmt : AUGMENT_KEYWORD string LEFT_CURLY_BRACE (when_stmt | if_feature_stmt | status_stmt
                        | description_stmt | reference_stmt | data_def_stmt | case_stmt)* RIGHT_CURLY_BRACE;

    /**
     *  augment-stmt        = augment-keyword sep augment-arg-str optsep
     *                        "{" stmtsep
     *                            ;; these stmts can appear in any order
     *                            [when-stmt stmtsep]
     *                            *(if-feature-stmt stmtsep)
     *                            [status-stmt stmtsep]
     *                            [description-stmt stmtsep]
     *                            [reference-stmt stmtsep]
     *                            1*((data-def-stmt stmtsep) /
     *                               (case-stmt stmtsep))
     *                         "}"
     * TODO : 0..1 occurance to be checked in listener
     */
    augment_stmt : AUGMENT_KEYWORD string LEFT_CURLY_BRACE (when_stmt | if_feature_stmt | status_stmt
                   | description_stmt | reference_stmt | data_def_stmt  | case_stmt)* RIGHT_CURLY_BRACE;

    /**
     *  when-stmt           = when-keyword sep string optsep
     *                        (";" /
     *                         "{" stmtsep
     *                             ;; these stmts can appear in any order
     *                             [description-stmt stmtsep]
     *                             [reference-stmt stmtsep]
     *                          "}")
     *
     */
    when_stmt : WHEN_KEYWORD string (STMTEND | LEFT_CURLY_BRACE ((description_stmt? reference_stmt?)
                       | (reference_stmt? description_stmt?)) RIGHT_CURLY_BRACE);

    /**
     *  rpc-stmt            = rpc-keyword sep identifier-arg-str optsep
     *                        (";" /
     *                         "{" stmtsep
     *                             ;; these stmts can appear in any order
     *                             *(if-feature-stmt stmtsep)
     *                             [status-stmt stmtsep]
     *                             [description-stmt stmtsep]
     *                             [reference-stmt stmtsep]
     *                             *((typedef-stmt /
     *                                grouping-stmt) stmtsep)
     *                             [input-stmt stmtsep]
     *                             [output-stmt stmtsep]
     *                         "}")
     * TODO : 0..1 occurance to be checked in listener
     */
    rpc_stmt : RPC_KEYWORD IDENTIFIER (STMTEND | LEFT_CURLY_BRACE (if_feature_stmt | status_stmt | description_stmt
                | reference_stmt | typedef_stmt | grouping_stmt | input_stmt | output_stmt)* RIGHT_CURLY_BRACE);

    /**
     * input-stmt          = input-keyword optsep
     *                       "{" stmtsep
     *                           ;; these stmts can appear in any order
     *                           *((typedef-stmt /
     *                              grouping-stmt) stmtsep)
     *                           1*(data-def-stmt stmtsep)
     *                         "}"
     */
    input_stmt : INPUT_KEYWORD LEFT_CURLY_BRACE
                 ((typedef_stmt | grouping_stmt)* | data_def_stmt+)
                 | (data_def_stmt+ | (typedef_stmt | grouping_stmt)*)RIGHT_CURLY_BRACE;

    /**
     *  output-stmt         = output-keyword optsep
     *                        "{" stmtsep
     *                            ;; these stmts can appear in any order
     *                            *((typedef-stmt /
     *                               grouping-stmt) stmtsep)
     *                            1*(data-def-stmt stmtsep)
     *                        "}"
     */
    output_stmt : OUTPUT_KEYWORD LEFT_CURLY_BRACE
                 ((typedef_stmt | grouping_stmt)* | data_def_stmt+)
                 | (data_def_stmt+ | (typedef_stmt | grouping_stmt)*)RIGHT_CURLY_BRACE;

    /**
     *  notification-stmt   = notification-keyword sep
     *                        identifier-arg-str optsep
     *                        (";" /
     *                         "{" stmtsep
     *                             ;; these stmts can appear in any order
     *                             *(if-feature-stmt stmtsep)
     *                             [status-stmt stmtsep]
     *                             [description-stmt stmtsep]
     *                             [reference-stmt stmtsep]
     *                             *((typedef-stmt /
     *                                grouping-stmt) stmtsep)
     *                             *(data-def-stmt stmtsep)
     *                         "}")
     * TODO : 0..1 occurance to be checked in listener
     */
     notification_stmt : NOTIFICATION_KEYWORD IDENTIFIER (STMTEND | LEFT_CURLY_BRACE (if_feature_stmt | status_stmt
                        | description_stmt | reference_stmt | typedef_stmt | grouping_stmt | data_def_stmt)*
                        RIGHT_CURLY_BRACE);

    /**
     *  deviation-stmt      = deviation-keyword sep
     *                        deviation-arg-str optsep
     *                        "{" stmtsep
     *                            ;; these stmts can appear in any order
     *                            [description-stmt stmtsep]
     *                            [reference-stmt stmtsep]
     *                            (deviate-not-supported-stmt /
     *                              1*(deviate-add-stmt /
     *                                 deviate-replace-stmt /
     *                                 deviate-delete-stmt))
     *                        "}"
     * TODO : 0..1 occurance to be checked in listener
     */
    deviation_stmt: DEVIATION_KEYWORD string LEFT_CURLY_BRACE (description_stmt | reference_stmt
                    | deviate_not_supported_stmt | deviate_add_stmt | deviate_replace_stmt | deviate_delete_stmt)*
                    RIGHT_CURLY_BRACE;

    /**
     * deviate-not-supported-stmt =
     *                       deviate-keyword sep
     *                       not-supported-keyword optsep
     *                       (";" /
     *                        "{" stmtsep
     *                        "}")
     */
    deviate_not_supported_stmt: DEVIATE_KEYWORD NOT_SUPPORTED_KEYWORD (STMTEND | LEFT_CURLY_BRACE RIGHT_CURLY_BRACE);

    /**
     *  deviate-add-stmt    = deviate-keyword sep add-keyword optsep
     *                        (";" /
     *                         "{" stmtsep
     *                             [units-stmt stmtsep]
     *                             *(must-stmt stmtsep)
     *                             *(unique-stmt stmtsep)
     *                             [default-stmt stmtsep]
     *                             [config-stmt stmtsep]
     *                             [mandatory-stmt stmtsep]
     *                             [min-elements-stmt stmtsep]
     *                             [max-elements-stmt stmtsep]
     *                         "}")
     */
    deviate_add_stmt: DEVIATE_KEYWORD ADD_KEYWORD (STMTEND | (LEFT_CURLY_BRACE units_stmt? must_stmt* unique_stmt*
                      default_stmt? config_stmt? mandatory_stmt? min_elements_stmt? max_elements_stmt?
                      RIGHT_CURLY_BRACE));

    /**
     *  deviate-delete-stmt = deviate-keyword sep delete-keyword optsep
     *                        (";" /
     *                         "{" stmtsep
     *                             [units-stmt stmtsep]
     *                             *(must-stmt stmtsep)
     *                             *(unique-stmt stmtsep)
     *                               [default-stmt stmtsep]
     *                           "}")
     */
    deviate_delete_stmt: DEVIATE_KEYWORD DELETE_KEYWORD (STMTEND
                       | (LEFT_CURLY_BRACE  units_stmt? must_stmt* unique_stmt* default_stmt? RIGHT_CURLY_BRACE));

    /** 
     *   deviate-replace-stmt = deviate-keyword sep replace-keyword optsep
     *                         (";" /
     *                          "{" stmtsep
     *                              [type-stmt stmtsep]
     *                              [units-stmt stmtsep]
     *                              [default-stmt stmtsep]
     *                              [config-stmt stmtsep]
     *                              [mandatory-stmt stmtsep]
     *                              [min-elements-stmt stmtsep]
     *                              [max-elements-stmt stmtsep]
     *                          "}")
     */
    deviate_replace_stmt: DEVIATE_KEYWORD REPLACE_KEYWORD (STMTEND | (LEFT_CURLY_BRACE type_stmt? units_stmt?
                           default_stmt? config_stmt? mandatory_stmt? min_elements_stmt?
                           max_elements_stmt? RIGHT_CURLY_BRACE));

    string : STRING (PLUS STRING)*;


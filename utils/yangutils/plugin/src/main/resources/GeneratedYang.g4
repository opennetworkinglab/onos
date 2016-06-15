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
 * This is a YANG grammar for parser based on which ANTLR will generate YANG parser.
 */

grammar GeneratedYang;
import YangLexer;

@header {
package org.onosproject.yangutils.parser.antlrgencode;
}

    yangfile : moduleStatement EOF
             | subModuleStatement EOF;

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

    moduleStatement : MODULE_KEYWORD identifier LEFT_CURLY_BRACE moduleBody RIGHT_CURLY_BRACE;

    moduleBody : moduleHeaderStatement linkageStatements metaStatements revisionStatements bodyStatements;

    /**
     * module-header-stmts = ;; these stmts can appear in any order
     *                       [yang-version-stmt stmtsep]
     *                        namespace-stmt stmtsep
     *                        prefix-stmt stmtsep
     */

    moduleHeaderStatement : yangVersionStatement? namespaceStatement prefixStatement
                            | yangVersionStatement? prefixStatement namespaceStatement
                            | namespaceStatement yangVersionStatement? prefixStatement
                            | namespaceStatement prefixStatement yangVersionStatement?
                            | prefixStatement namespaceStatement yangVersionStatement?
                            | prefixStatement yangVersionStatement? namespaceStatement
                            ;

    /**
     * linkage-stmts       = ;; these stmts can appear in any order
     *                       *(import-stmt stmtsep)
     *                       *(include-stmt stmtsep)
     */
    linkageStatements : (importStatement
                  | includeStatement)*;

    /**
     * meta-stmts          = ;; these stmts can appear in any order
     *                       [organization-stmt stmtsep]
     *                       [contact-stmt stmtsep]
     *                       [description-stmt stmtsep]
     *                       [reference-stmt stmtsep]
     */
    metaStatements : organizationStatement? contactStatement? descriptionStatement? referenceStatement?
               | organizationStatement? contactStatement? referenceStatement? descriptionStatement?
               | organizationStatement? descriptionStatement? contactStatement? referenceStatement?
               | organizationStatement? descriptionStatement? referenceStatement? contactStatement?
               | organizationStatement? referenceStatement? contactStatement? descriptionStatement?
               | organizationStatement? referenceStatement? descriptionStatement? contactStatement?
               | contactStatement? organizationStatement? descriptionStatement? referenceStatement?
               | contactStatement? organizationStatement? referenceStatement? descriptionStatement?
               | contactStatement? referenceStatement? organizationStatement? descriptionStatement?
               | contactStatement? referenceStatement? descriptionStatement? organizationStatement?
               | contactStatement? descriptionStatement? referenceStatement? organizationStatement?
               | contactStatement? descriptionStatement? organizationStatement? referenceStatement?
               | referenceStatement? contactStatement? organizationStatement? descriptionStatement?
               | referenceStatement? contactStatement? descriptionStatement? organizationStatement?
               | referenceStatement? organizationStatement? contactStatement? descriptionStatement?
               | referenceStatement? organizationStatement? descriptionStatement? contactStatement?
               | referenceStatement? descriptionStatement? organizationStatement? contactStatement?
               | referenceStatement? descriptionStatement? contactStatement? organizationStatement?
               | descriptionStatement? referenceStatement? contactStatement? organizationStatement?
               | descriptionStatement? referenceStatement? organizationStatement? contactStatement?
               | descriptionStatement? contactStatement? referenceStatement? organizationStatement?
               | descriptionStatement? contactStatement? organizationStatement? referenceStatement?
               | descriptionStatement? organizationStatement? contactStatement? referenceStatement?
               | descriptionStatement? organizationStatement? referenceStatement? contactStatement?
               ;

    // revision-stmts      = *(revision-stmt stmtsep)
    revisionStatements : revisionStatement*;

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
    bodyStatements : (extensionStatement
               | featureStatement
               | identityStatement
               | typedefStatement
               | groupingStatement
               | dataDefStatement
               | augmentStatement
               | rpcStatement
               | notificationStatement
               | deviationStatement)*
               ;

    /**
     * yang-version-stmt   = yang-version-keyword sep yang-version-arg-str
     *                       optsep stmtend
     */
    yangVersionStatement :   YANG_VERSION_KEYWORD version STMTEND;


    /**
     * namespace-stmt      = namespace-keyword sep uri-str optsep stmtend
     * For namespace validation TODO in Listener
     */
    namespaceStatement : NAMESPACE_KEYWORD string STMTEND;

    /**
     * prefix-stmt         = prefix-keyword sep prefix-arg-str
     *                       optsep stmtend
     */
    prefixStatement : PREFIX_KEYWORD identifier STMTEND;

    /**
     * import-stmt         = import-keyword sep identifier-arg-str optsep
     *                       "{" stmtsep
     *                           prefix-stmt stmtsep
     *                           [revision-date-stmt stmtsep]
     *                        "}"
     */
    importStatement : IMPORT_KEYWORD identifier LEFT_CURLY_BRACE importStatementBody RIGHT_CURLY_BRACE;

    importStatementBody : prefixStatement revisionDateStatement?;

    // revision-date-stmt = revision-date-keyword sep revision-date stmtend
    revisionDateStatement : REVISION_DATE_KEYWORD dateArgumentString STMTEND;

    /**
     * include-stmt        = include-keyword sep identifier-arg-str optsep
     *                             (";" /
     *                              "{" stmtsep
     *                                  [revision-date-stmt stmtsep]
     *                            "}")
     */
    includeStatement : INCLUDE_KEYWORD identifier (STMTEND | LEFT_CURLY_BRACE revisionDateStatement? RIGHT_CURLY_BRACE);

    /**
     * organization-stmt   = organization-keyword sep string
     *                            optsep stmtend
     */
    organizationStatement : ORGANIZATION_KEYWORD string STMTEND;

    // contact-stmt        = contact-keyword sep string optsep stmtend
    contactStatement : CONTACT_KEYWORD string STMTEND;

    // description-stmt    = description-keyword sep string optsep stmtend
    descriptionStatement : DESCRIPTION_KEYWORD string STMTEND;

    // reference-stmt      = reference-keyword sep string optsep stmtend
    referenceStatement : REFERENCE_KEYWORD string STMTEND;

    /**
     * revision-stmt       = revision-keyword sep revision-date optsep
     *                             (";" /
     *                              "{" stmtsep
     *                                  [description-stmt stmtsep]
     *                                  [reference-stmt stmtsep]
     *                              "}")
     */
    revisionStatement : REVISION_KEYWORD dateArgumentString (STMTEND | LEFT_CURLY_BRACE revisionStatementBody RIGHT_CURLY_BRACE);
    revisionStatementBody : descriptionStatement? referenceStatement?;

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
    subModuleStatement : SUBMODULE_KEYWORD identifier LEFT_CURLY_BRACE submoduleBody RIGHT_CURLY_BRACE;
    submoduleBody : submoduleHeaderStatement linkageStatements metaStatements revisionStatements bodyStatements;

    /** submodule-header-stmts =
     *                            ;; these stmts can appear in any order
     *                            [yang-version-stmt stmtsep]
     *                             belongs-to-stmt stmtsep
     */
    submoduleHeaderStatement : yangVersionStatement? belongstoStatement
                               | belongstoStatement yangVersionStatement?
                              ;

    /**
     * belongs-to-stmt     = belongs-to-keyword sep identifier-arg-str
     *                       optsep
     *                       "{" stmtsep
     *                           prefix-stmt stmtsep
     *                       "}"
     */
    belongstoStatement : BELONGS_TO_KEYWORD identifier LEFT_CURLY_BRACE belongstoStatementBody RIGHT_CURLY_BRACE;
    belongstoStatementBody : prefixStatement;

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
    extensionStatement : EXTENSION_KEYWORD identifier (STMTEND | LEFT_CURLY_BRACE extensionBody RIGHT_CURLY_BRACE);
    extensionBody : argumentStatement? statusStatement? descriptionStatement? referenceStatement?
                   | argumentStatement? statusStatement? referenceStatement? descriptionStatement?
                   | argumentStatement? descriptionStatement? statusStatement? referenceStatement?
                   | argumentStatement? descriptionStatement? referenceStatement? statusStatement?
                   | argumentStatement? referenceStatement? descriptionStatement? statusStatement?
                   | argumentStatement? referenceStatement? statusStatement? descriptionStatement?
                   | statusStatement? referenceStatement? argumentStatement? descriptionStatement?
                   | statusStatement? referenceStatement? descriptionStatement? argumentStatement?
                   | statusStatement? descriptionStatement? referenceStatement? argumentStatement?
                   | statusStatement? descriptionStatement? argumentStatement? referenceStatement?
                   | statusStatement? argumentStatement? referenceStatement? descriptionStatement?
                   | statusStatement? argumentStatement? descriptionStatement? referenceStatement?
                   | descriptionStatement? argumentStatement? statusStatement? referenceStatement?
                   | descriptionStatement? argumentStatement? referenceStatement? statusStatement?
                   | descriptionStatement? statusStatement? argumentStatement? referenceStatement?
                   | descriptionStatement? statusStatement? referenceStatement? argumentStatement?
                   | descriptionStatement? referenceStatement? statusStatement? argumentStatement?
                   | descriptionStatement? referenceStatement? argumentStatement? statusStatement?
                   | referenceStatement? descriptionStatement? argumentStatement? statusStatement?
                   | referenceStatement? descriptionStatement? statusStatement? argumentStatement?
                   | referenceStatement? statusStatement? argumentStatement? descriptionStatement?
                   | referenceStatement? statusStatement? descriptionStatement? argumentStatement?
                   | referenceStatement? argumentStatement? descriptionStatement? statusStatement?
                   | referenceStatement? argumentStatement? statusStatement? descriptionStatement?
                   ;

    /**
     * argument-stmt       = argument-keyword sep identifier-arg-str optsep
     *                       (";" /
     *                        "{" stmtsep
     *                            [yin-element-stmt stmtsep]
     *                        "}")
     */
    argumentStatement : ARGUMENT_KEYWORD identifier (STMTEND | LEFT_CURLY_BRACE argumentBody RIGHT_CURLY_BRACE);
    argumentBody : yinElementStatement?;

    /**
     * yin-element-stmt    = yin-element-keyword sep yin-element-arg-str
     *                       stmtend
     */
    yinElementStatement : YIN_ELEMENT_KEYWORD (TRUE_KEYWORD | FALSE_KEYWORD) STMTEND;

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
    identityStatement : IDENTITY_KEYWORD identifier (STMTEND | LEFT_CURLY_BRACE identityBody RIGHT_CURLY_BRACE);
    identityBody : baseStatement? statusStatement? descriptionStatement? referenceStatement?
                  | baseStatement? statusStatement? referenceStatement? descriptionStatement?
                  | baseStatement? descriptionStatement? statusStatement? referenceStatement?
                  | baseStatement? descriptionStatement? referenceStatement? statusStatement?
                  | baseStatement? referenceStatement? descriptionStatement? statusStatement?
                  | baseStatement? referenceStatement? statusStatement? descriptionStatement?
                  | referenceStatement? baseStatement? statusStatement? descriptionStatement?
                  | referenceStatement? baseStatement? descriptionStatement? statusStatement?
                  | referenceStatement? statusStatement? baseStatement? descriptionStatement?
                  | referenceStatement? statusStatement? descriptionStatement? baseStatement?
                  | referenceStatement? descriptionStatement? statusStatement? baseStatement?
                  | referenceStatement? descriptionStatement? baseStatement? statusStatement?
                  | descriptionStatement? referenceStatement? statusStatement? baseStatement?
                  | descriptionStatement? referenceStatement? statusStatement? baseStatement?
                  | descriptionStatement? referenceStatement? baseStatement? statusStatement?
                  | descriptionStatement? statusStatement? baseStatement? referenceStatement?
                  | descriptionStatement? statusStatement? referenceStatement? baseStatement?
                  | descriptionStatement? baseStatement? referenceStatement? statusStatement?
                  | descriptionStatement? baseStatement? statusStatement? referenceStatement?
                  | statusStatement? baseStatement? descriptionStatement? referenceStatement?
                  | statusStatement? baseStatement? referenceStatement? descriptionStatement?
                  | statusStatement? descriptionStatement? baseStatement? referenceStatement?
                  | statusStatement? descriptionStatement? referenceStatement? baseStatement?
                  | statusStatement? referenceStatement? descriptionStatement? baseStatement?
                  | statusStatement? referenceStatement? baseStatement? descriptionStatement?
                  ;

    /**
     * base-stmt           = base-keyword sep identifier-ref-arg-str
     *                          optsep stmtend*
     * identifier-ref-arg  = [prefix ":"] identifier
     */
    baseStatement : BASE_KEYWORD string STMTEND;

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
    featureStatement : FEATURE_KEYWORD string (STMTEND | LEFT_CURLY_BRACE featureBody RIGHT_CURLY_BRACE);
    featureBody : ifFeatureStatement* statusStatement? descriptionStatement? referenceStatement?
                 | ifFeatureStatement* statusStatement? referenceStatement? descriptionStatement?
                 | ifFeatureStatement* descriptionStatement? statusStatement? referenceStatement?
                 | ifFeatureStatement* descriptionStatement? referenceStatement? statusStatement?
                 | ifFeatureStatement* referenceStatement? statusStatement? descriptionStatement?
                 | ifFeatureStatement* referenceStatement? descriptionStatement? statusStatement?
                 | statusStatement? ifFeatureStatement* descriptionStatement? referenceStatement?
                 | statusStatement? ifFeatureStatement* referenceStatement? descriptionStatement?
                 | statusStatement? descriptionStatement? ifFeatureStatement* referenceStatement?
                 | statusStatement? descriptionStatement? referenceStatement? ifFeatureStatement*
                 | statusStatement? referenceStatement? ifFeatureStatement* descriptionStatement?
                 | statusStatement? referenceStatement? descriptionStatement? ifFeatureStatement*
                 | descriptionStatement? ifFeatureStatement* statusStatement? referenceStatement?
                 | descriptionStatement? ifFeatureStatement* referenceStatement? statusStatement?
                 | descriptionStatement? statusStatement? ifFeatureStatement* referenceStatement?
                 | descriptionStatement? statusStatement? referenceStatement? ifFeatureStatement*
                 | descriptionStatement? referenceStatement* statusStatement? ifFeatureStatement*
                 | descriptionStatement? referenceStatement* ifFeatureStatement? statusStatement?
                 | referenceStatement? ifFeatureStatement* statusStatement? descriptionStatement?
                 | referenceStatement? ifFeatureStatement* descriptionStatement? statusStatement?
                 | referenceStatement? descriptionStatement? statusStatement? ifFeatureStatement*
                 | referenceStatement? descriptionStatement? ifFeatureStatement* statusStatement?
                 | referenceStatement? statusStatement? descriptionStatement? ifFeatureStatement*
                 | referenceStatement? statusStatement? ifFeatureStatement* descriptionStatement?
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
    dataDefStatement : containerStatement
                    | leafStatement
                    | leafListStatement
                    | listStatement
                    | choiceStatement
                    | anyxmlStatement
                    | usesStatement;

    /**
     *  if-feature-stmt     = if-feature-keyword sep identifier-ref-arg-str
     *                        optsep stmtend
     */
    ifFeatureStatement : IF_FEATURE_KEYWORD string STMTEND;

    /**
    *    units-stmt          = units-keyword sep string optsep stmtend
    */
    unitsStatement : UNITS_KEYWORD string STMTEND;

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
    typedefStatement : TYPEDEF_KEYWORD identifier LEFT_CURLY_BRACE
                   (typeStatement | unitsStatement | defaultStatement | statusStatement | descriptionStatement | referenceStatement)*
                   RIGHT_CURLY_BRACE;

    /**
     *  type-stmt           = type-keyword sep identifier-ref-arg-str optsep
     *                        (";" /
     *                         "{" stmtsep
     *                            type-body-stmts
     *                         "}")
     */
    typeStatement : TYPE_KEYWORD string (STMTEND | LEFT_CURLY_BRACE typeBodyStatements RIGHT_CURLY_BRACE);

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
     *
     */
    typeBodyStatements : numericalRestrictions | decimal64Specification | stringRestrictions | enumSpecification
                    | leafrefSpecification | identityrefSpecification | instanceIdentifierSpecification
                    | bitsSpecification | unionSpecification;

    /**
     *  fraction-digits-stmt = fraction-digits-keyword sep
     *                         fraction-digits-arg-str stmtend
     *
     *  fraction-digits-arg-str = < a string that matches the rule
     *                             fraction-digits-arg >
     *
     *  fraction-digits-arg = ("1" ["0" / "1" / "2" / "3" / "4" /
     *                              "5" / "6" / "7" / "8"])
     *                        / "2" / "3" / "4" / "5" / "6" / "7" / "8" / "9"
     */
    decimal64Specification : FRACTION_DIGITS_KEYWORD fraction STMTEND;

    /**
     *  numerical-restrictions = range-stmt stmtsep
     */
    numericalRestrictions : rangeStatement;

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
    rangeStatement : RANGE_KEYWORD range (STMTEND | LEFT_CURLY_BRACE commonStatements RIGHT_CURLY_BRACE);

    commonStatements : errorMessageStatement? errorAppTagStatement? descriptionStatement? referenceStatement?
                 | errorMessageStatement? errorAppTagStatement? referenceStatement? descriptionStatement?
                 | errorMessageStatement? descriptionStatement? errorAppTagStatement? referenceStatement?
                 | errorMessageStatement? descriptionStatement? referenceStatement? errorAppTagStatement?
                 | errorMessageStatement? referenceStatement? errorAppTagStatement? descriptionStatement?
                 | errorMessageStatement? referenceStatement? descriptionStatement? errorAppTagStatement?
                 | errorAppTagStatement? errorMessageStatement? descriptionStatement? referenceStatement?
                 | errorAppTagStatement? errorMessageStatement? referenceStatement? descriptionStatement?
                 | errorAppTagStatement? descriptionStatement? descriptionStatement? errorMessageStatement?
                 | errorAppTagStatement? descriptionStatement? errorMessageStatement? descriptionStatement?
                 | errorAppTagStatement? referenceStatement? errorMessageStatement? descriptionStatement?
                 | errorAppTagStatement? referenceStatement? descriptionStatement? errorMessageStatement?
                 | descriptionStatement? errorMessageStatement? errorAppTagStatement? referenceStatement?
                 | descriptionStatement? errorMessageStatement? referenceStatement? errorAppTagStatement?
                 | descriptionStatement? errorAppTagStatement? errorMessageStatement? referenceStatement?
                 | descriptionStatement? errorAppTagStatement? referenceStatement? errorMessageStatement?
                 | descriptionStatement? referenceStatement? errorMessageStatement? errorAppTagStatement?
                 | descriptionStatement? referenceStatement? errorAppTagStatement? errorMessageStatement?
                 | referenceStatement? errorMessageStatement? descriptionStatement? errorAppTagStatement?
                 | referenceStatement? errorMessageStatement? errorAppTagStatement? descriptionStatement?
                 | referenceStatement? errorAppTagStatement? descriptionStatement? errorMessageStatement?
                 | referenceStatement? errorAppTagStatement? errorMessageStatement? descriptionStatement?
                 | referenceStatement? descriptionStatement? errorMessageStatement? errorAppTagStatement?
                 | referenceStatement? descriptionStatement? errorAppTagStatement? errorMessageStatement?
                 ;

    /**
     *  string-restrictions = ;; these stmts can appear in any order
     *                        [length-stmt stmtsep]
     *                        *(pattern-stmt stmtsep)
     */
    stringRestrictions : ((lengthStatement)? (patternStatement)*) | ((patternStatement)* (lengthStatement)?);

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
    lengthStatement : LENGTH_KEYWORD length
                  (STMTEND | LEFT_CURLY_BRACE commonStatements RIGHT_CURLY_BRACE);

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
    patternStatement : PATTERN_KEYWORD string (STMTEND | LEFT_CURLY_BRACE commonStatements RIGHT_CURLY_BRACE);

    /**
     *  default-stmt        = default-keyword sep string stmtend
     */
    defaultStatement : DEFAULT_KEYWORD string STMTEND;

    /**
     *  enum-specification  = 1*(enum-stmt stmtsep)
     */
    enumSpecification : enumStatement+;

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
    enumStatement : ENUM_KEYWORD string (STMTEND | LEFT_CURLY_BRACE enumStatementBody RIGHT_CURLY_BRACE);

    enumStatementBody : valueStatement? statusStatement? descriptionStatement? referenceStatement?
                   | valueStatement? statusStatement? referenceStatement? descriptionStatement?
                   | valueStatement? descriptionStatement? statusStatement? referenceStatement?
                   | valueStatement? descriptionStatement? referenceStatement? statusStatement?
                   | valueStatement? referenceStatement? statusStatement? descriptionStatement?
                   | valueStatement? referenceStatement? descriptionStatement? statusStatement?
                   | statusStatement? valueStatement? descriptionStatement? referenceStatement?
                   | statusStatement? valueStatement? referenceStatement? descriptionStatement?
                   | statusStatement? descriptionStatement? descriptionStatement? valueStatement?
                   | statusStatement? descriptionStatement? valueStatement? descriptionStatement?
                   | statusStatement? referenceStatement? valueStatement? descriptionStatement?
                   | statusStatement? referenceStatement? descriptionStatement? valueStatement?
                   | descriptionStatement? valueStatement? statusStatement? referenceStatement?
                   | descriptionStatement? valueStatement? referenceStatement? statusStatement?
                   | descriptionStatement? statusStatement? valueStatement? referenceStatement?
                   | descriptionStatement? statusStatement? referenceStatement? valueStatement?
                   | descriptionStatement? referenceStatement? valueStatement? statusStatement?
                   | descriptionStatement? referenceStatement? statusStatement? valueStatement?
                   | referenceStatement? valueStatement? descriptionStatement? statusStatement?
                   | referenceStatement? valueStatement? statusStatement? descriptionStatement?
                   | referenceStatement? statusStatement? descriptionStatement? valueStatement?
                   | referenceStatement? statusStatement? valueStatement? descriptionStatement?
                   | referenceStatement? descriptionStatement? valueStatement? statusStatement?
                   | referenceStatement? descriptionStatement? statusStatement? valueStatement?
                   ;

    /**
     *  leafref-specification =
     *                        ;; these stmts can appear in any order
     *                        path-stmt stmtsep
     *                        [require-instance-stmt stmtsep]
     */
    leafrefSpecification : (pathStatement (requireInstanceStatement)?) | ((requireInstanceStatement)? pathStatement);

    /**
     *  path-stmt           = path-keyword sep path-arg-str stmtend
     */
    pathStatement : PATH_KEYWORD path STMTEND;

    /**
     *  require-instance-stmt = require-instance-keyword sep
     *                           require-instance-arg-str stmtend
     *  require-instance-arg-str = < a string that matches the rule
     *                             require-instance-arg >
     *  require-instance-arg = true-keyword / false-keyword
     */
    requireInstanceStatement : REQUIRE_INSTANCE_KEYWORD (TRUE_KEYWORD | FALSE_KEYWORD) STMTEND;

    /**
     *  instance-identifier-specification =
     *                        [require-instance-stmt stmtsep]
     */
    instanceIdentifierSpecification : requireInstanceStatement?;

    /**
     * identityref-specification =
     *                        base-stmt stmtsep
     */
    identityrefSpecification : baseStatement;

    /**
     *  union-specification = 1*(type-stmt stmtsep)
     */
    unionSpecification : typeStatement+;

    /**
     *  bits-specification  = 1*(bit-stmt stmtsep)
     */
    bitsSpecification : bitStatement+;

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
    bitStatement : BIT_KEYWORD identifier (STMTEND | LEFT_CURLY_BRACE bitBodyStatement RIGHT_CURLY_BRACE);

    bitBodyStatement : positionStatement? statusStatement? descriptionStatement? referenceStatement?
                  | positionStatement? statusStatement? referenceStatement? descriptionStatement?
                  | positionStatement? descriptionStatement? statusStatement? referenceStatement?
                  | positionStatement? descriptionStatement? referenceStatement? statusStatement?
                  | positionStatement? referenceStatement? statusStatement? descriptionStatement?
                  | positionStatement? referenceStatement? descriptionStatement? statusStatement?
                  | statusStatement? positionStatement? descriptionStatement? referenceStatement?
                  | statusStatement? positionStatement? referenceStatement? descriptionStatement?
                  | statusStatement? descriptionStatement? descriptionStatement? positionStatement?
                  | statusStatement? descriptionStatement? positionStatement? descriptionStatement?
                  | statusStatement? referenceStatement? positionStatement? descriptionStatement?
                  | statusStatement? referenceStatement? descriptionStatement? positionStatement?
                  | descriptionStatement? positionStatement? statusStatement? referenceStatement?
                  | descriptionStatement? positionStatement? referenceStatement? statusStatement?
                  | descriptionStatement? statusStatement? positionStatement? referenceStatement?
                  | descriptionStatement? statusStatement? referenceStatement? positionStatement?
                  | descriptionStatement? referenceStatement? positionStatement? statusStatement?
                  | descriptionStatement? referenceStatement? statusStatement? positionStatement?
                  | referenceStatement? positionStatement? descriptionStatement? statusStatement?
                  | referenceStatement? positionStatement? statusStatement? descriptionStatement?
                  | referenceStatement? statusStatement? descriptionStatement? positionStatement?
                  | referenceStatement? statusStatement? positionStatement? descriptionStatement?
                  | referenceStatement? descriptionStatement? positionStatement? statusStatement?
                  | referenceStatement? descriptionStatement? statusStatement? positionStatement?
                  ;

    /**
     *  position-stmt       = position-keyword sep
     *                        position-value-arg-str stmtend
     *  position-value-arg-str = < a string that matches the rule
     *                              position-value-arg >
     *  position-value-arg  = non-negative-integer-value
     */
    positionStatement : POSITION_KEYWORD position STMTEND;

    /**
     *  status-stmt         = status-keyword sep status-arg-str stmtend
     *  status-arg-str      = < a string that matches the rule
     *                         status-arg >
     *  status-arg          = current-keyword /
     *                        obsolete-keyword /
     *                        deprecated-keyword
     */
    statusStatement : STATUS_KEYWORD status STMTEND;

    /**
     *  config-stmt         = config-keyword sep
     *                        config-arg-str stmtend
     *  config-arg-str      = < a string that matches the rule
     *                          config-arg >
     *  config-arg          = true-keyword / false-keyword
     */
    configStatement : CONFIG_KEYWORD config STMTEND;

    /**
     *  mandatory-stmt      = mandatory-keyword sep
     *                        mandatory-arg-str stmtend
     *
     *  mandatory-arg-str   = < a string that matches the rule
     *                          mandatory-arg >
     *
     *  mandatory-arg       = true-keyword / false-keyword
     */
    mandatoryStatement : MANDATORY_KEYWORD mandatory STMTEND;

    /**
     *  presence-stmt       = presence-keyword sep string stmtend
     */
    presenceStatement : PRESENCE_KEYWORD string STMTEND;

    /**
     *  ordered-by-stmt     = ordered-by-keyword sep
     *                        ordered-by-arg-str stmtend
     *
     *  ordered-by-arg-str  = < a string that matches the rule
     *                          ordered-by-arg >
     *
     *  ordered-by-arg      = user-keyword / system-keyword
     */
    orderedByStatement : ORDERED_BY_KEYWORD orderedBy STMTEND;

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
    mustStatement : MUST_KEYWORD string (STMTEND | LEFT_CURLY_BRACE commonStatements RIGHT_CURLY_BRACE);

    /**
     *   error-message-stmt  = error-message-keyword sep string stmtend
     */
    errorMessageStatement : ERROR_MESSAGE_KEYWORD string STMTEND;

    /**
     *  error-app-tag-stmt  = error-app-tag-keyword sep string stmtend
     */
    errorAppTagStatement : ERROR_APP_TAG_KEYWORD string STMTEND;

    /**
     *  min-elements-stmt   = min-elements-keyword sep
     *                        min-value-arg-str stmtend
     *  min-value-arg-str   = < a string that matches the rule
     *                          min-value-arg >
     *  min-value-arg       = non-negative-integer-value
     */
    minElementsStatement : MIN_ELEMENTS_KEYWORD minValue STMTEND;

    /**
     *  max-elements-stmt   = max-elements-keyword sep
     *                        max-value-arg-str stmtend
     *  max-value-arg-str   = < a string that matches the rule
     *                          max-value-arg >
     *  max-value-arg       = unbounded-keyword /
     *                        positive-integer-value
     */
    maxElementsStatement :  MAX_ELEMENTS_KEYWORD maxValue STMTEND;

    /**
     *  value-stmt          = value-keyword sep integer-value stmtend
     */
    valueStatement : VALUE_KEYWORD value STMTEND;

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
    groupingStatement : GROUPING_KEYWORD identifier (STMTEND | LEFT_CURLY_BRACE
                      (statusStatement | descriptionStatement | referenceStatement | typedefStatement | groupingStatement
                       | dataDefStatement)* RIGHT_CURLY_BRACE);

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
    containerStatement : CONTAINER_KEYWORD identifier
                     (STMTEND | LEFT_CURLY_BRACE (whenStatement | ifFeatureStatement | mustStatement | presenceStatement | configStatement
                     | statusStatement | descriptionStatement | referenceStatement | typedefStatement | groupingStatement
                     | dataDefStatement)* RIGHT_CURLY_BRACE);

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
    leafStatement : LEAF_KEYWORD identifier LEFT_CURLY_BRACE (whenStatement | ifFeatureStatement | typeStatement | unitsStatement
              | mustStatement | defaultStatement | configStatement | mandatoryStatement | statusStatement  | descriptionStatement
              | referenceStatement)* RIGHT_CURLY_BRACE;

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
    leafListStatement : LEAF_LIST_KEYWORD identifier LEFT_CURLY_BRACE (whenStatement | ifFeatureStatement | typeStatement
                     | unitsStatement | mustStatement | configStatement | minElementsStatement | maxElementsStatement | orderedByStatement
                     | statusStatement | descriptionStatement | referenceStatement)* RIGHT_CURLY_BRACE;

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
    listStatement : LIST_KEYWORD identifier LEFT_CURLY_BRACE (whenStatement | ifFeatureStatement | mustStatement | keyStatement
              | uniqueStatement | configStatement | minElementsStatement | maxElementsStatement | orderedByStatement | statusStatement
              | descriptionStatement | referenceStatement | typedefStatement | groupingStatement| dataDefStatement)* RIGHT_CURLY_BRACE;

    /**
     *  key-stmt            = key-keyword sep key-arg-str stmtend
     */
    keyStatement : KEY_KEYWORD key STMTEND;

    /**
     *  unique-stmt         = unique-keyword sep unique-arg-str stmtend
     */
    uniqueStatement: UNIQUE_KEYWORD unique STMTEND;

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
    choiceStatement : CHOICE_KEYWORD identifier (STMTEND | LEFT_CURLY_BRACE (whenStatement | ifFeatureStatement | defaultStatement
                  | configStatement | mandatoryStatement | statusStatement | descriptionStatement | referenceStatement | shortCaseStatement
                  | caseStatement)* RIGHT_CURLY_BRACE);

    /**
     *  short-case-stmt     = container-stmt /
     *                        leaf-stmt /
     *                        leaf-list-stmt /
     *                        list-stmt /
     *                        anyxml-stmt
     */
    shortCaseStatement : containerStatement | leafStatement | leafListStatement | listStatement | anyxmlStatement;

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
    caseStatement : CASE_KEYWORD identifier (STMTEND | LEFT_CURLY_BRACE (whenStatement | ifFeatureStatement | statusStatement
              | descriptionStatement | referenceStatement | dataDefStatement)* RIGHT_CURLY_BRACE);

    /**
     *    anyxml-stmt         = anyxml-keyword sep identifier-arg-str optsep
     *                         (";" /
     *                         "{" stmtsep
     *                             ;; these stmts can appear in any order
     *                             [when-stmt stmtsep]
     *                             *(if-feature-stmt stmtsep)
     *                             *(must-stmt stmtsep)
     *                             [config-stmt stmtsep]
     *                             [mandatory-stmt stmtsep]
     *                             [status-stmt stmtsep]
     *                             [description-stmt stmtsep]
     *                             [reference-stmt stmtsep]
     *                          "}")
     */
     anyxmlStatement : ANYXML_KEYWORD identifier (STMTEND | LEFT_CURLY_BRACE (whenStatement | ifFeatureStatement
                     | mustStatement | configStatement | mandatoryStatement | statusStatement | descriptionStatement
                     | referenceStatement)* RIGHT_CURLY_BRACE);

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
    usesStatement : USES_KEYWORD string (STMTEND | LEFT_CURLY_BRACE (whenStatement | ifFeatureStatement | statusStatement
                | descriptionStatement | referenceStatement | refineStatement | augmentStatement)* RIGHT_CURLY_BRACE);

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
    refineStatement : REFINE_KEYWORD refine (STMTEND  | LEFT_CURLY_BRACE (refineContainerStatements
                    | refineLeafStatements | refineLeafListStatements | refineListStatements | refineChoiceStatements
                    | refineCaseStatements | refineAnyxmlStatements) RIGHT_CURLY_BRACE);

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
    refineContainerStatements : (mustStatement | presenceStatement | configStatement | descriptionStatement | referenceStatement)* ;

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
    refineLeafStatements : (mustStatement | defaultStatement | configStatement | mandatoryStatement | descriptionStatement | referenceStatement)*;

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
    refineLeafListStatements : (mustStatement | configStatement | minElementsStatement | maxElementsStatement | descriptionStatement
                             | referenceStatement)*;

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
    refineListStatements : (mustStatement | configStatement | minElementsStatement | maxElementsStatement | descriptionStatement
                        | referenceStatement)*;

    /**
     *  refine-choice-stmts = ;; these stmts can appear in any order
     *                        [default-stmt stmtsep]
     *                        [config-stmt stmtsep]
     *                        [mandatory-stmt stmtsep]
     *                        [description-stmt stmtsep]
     *                        [reference-stmt stmtsep]
     * TODO : 0..1 occurance to be checked in listener
     */
    refineChoiceStatements : (defaultStatement | configStatement | mandatoryStatement | descriptionStatement | referenceStatement)*;

    /**
     *  refine-case-stmts   = ;; these stmts can appear in any order
     *                        [description-stmt stmtsep]
     *                        [reference-stmt stmtsep]
     *
     */
    refineCaseStatements : (descriptionStatement | referenceStatement)? | (referenceStatement | descriptionStatement)?;

    /**
     *  refine-anyxml-stmts = ;; these stmts can appear in any order
     *                        *(must-stmt stmtsep)
     *                        [config-stmt stmtsep]
     *                        [mandatory-stmt stmtsep]
     *                        [description-stmt stmtsep]
     *                        [reference-stmt stmtsep]
     */
     refineAnyxmlStatements : (mustStatement | configStatement | mandatoryStatement | descriptionStatement
                            | referenceStatement)*;

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
    augmentStatement : AUGMENT_KEYWORD augment LEFT_CURLY_BRACE (whenStatement | ifFeatureStatement | statusStatement
                   | descriptionStatement | referenceStatement | dataDefStatement  | caseStatement)* RIGHT_CURLY_BRACE;

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
    whenStatement : WHEN_KEYWORD string (STMTEND | LEFT_CURLY_BRACE ((descriptionStatement? referenceStatement?)
                       | (referenceStatement? descriptionStatement?)) RIGHT_CURLY_BRACE);

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
     */
    rpcStatement : RPC_KEYWORD identifier (STMTEND | LEFT_CURLY_BRACE (ifFeatureStatement | statusStatement | descriptionStatement
                | referenceStatement | typedefStatement | groupingStatement | inputStatement | outputStatement)* RIGHT_CURLY_BRACE);

    /**
     * input-stmt          = input-keyword optsep
     *                       "{" stmtsep
     *                           ;; these stmts can appear in any order
     *                           *((typedef-stmt /
     *                              grouping-stmt) stmtsep)
     *                           1*(data-def-stmt stmtsep)
     *                         "}"
     */
    inputStatement : INPUT_KEYWORD LEFT_CURLY_BRACE (typedefStatement | groupingStatement | dataDefStatement)* RIGHT_CURLY_BRACE;

    /**
     *  output-stmt         = output-keyword optsep
     *                        "{" stmtsep
     *                            ;; these stmts can appear in any order
     *                            *((typedef-stmt /
     *                               grouping-stmt) stmtsep)
     *                            1*(data-def-stmt stmtsep)
     *                        "}"
     */
    outputStatement : OUTPUT_KEYWORD LEFT_CURLY_BRACE (typedefStatement | groupingStatement | dataDefStatement)* RIGHT_CURLY_BRACE;

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
     notificationStatement : NOTIFICATION_KEYWORD identifier (STMTEND | LEFT_CURLY_BRACE (ifFeatureStatement
                           | statusStatement | descriptionStatement | referenceStatement | typedefStatement
                           | groupingStatement | dataDefStatement)* RIGHT_CURLY_BRACE);

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
    deviationStatement: DEVIATION_KEYWORD deviation LEFT_CURLY_BRACE (descriptionStatement | referenceStatement
                      | deviateNotSupportedStatement | deviateAddStatement | deviateReplaceStatement
                      | deviateDeleteStatement)* RIGHT_CURLY_BRACE;

    /**
     * deviate-not-supported-stmt =
     *                       deviate-keyword sep
     *                       not-supported-keyword optsep
     *                       (";" /
     *                        "{" stmtsep
     *                        "}")
     */
    deviateNotSupportedStatement: DEVIATE_KEYWORD NOT_SUPPORTED_KEYWORD (STMTEND | LEFT_CURLY_BRACE RIGHT_CURLY_BRACE);

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
    deviateAddStatement: DEVIATE_KEYWORD ADD_KEYWORD (STMTEND | (LEFT_CURLY_BRACE unitsStatement? mustStatement* uniqueStatement*
                      defaultStatement? configStatement? mandatoryStatement? minElementsStatement? maxElementsStatement?
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
    deviateDeleteStatement: DEVIATE_KEYWORD DELETE_KEYWORD (STMTEND
                       | (LEFT_CURLY_BRACE  unitsStatement? mustStatement* uniqueStatement* defaultStatement? RIGHT_CURLY_BRACE));

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
    deviateReplaceStatement: DEVIATE_KEYWORD REPLACE_KEYWORD (STMTEND | (LEFT_CURLY_BRACE typeStatement? unitsStatement?
                           defaultStatement? configStatement? mandatoryStatement? minElementsStatement?
                           maxElementsStatement? RIGHT_CURLY_BRACE));

    string : STRING (PLUS STRING)*
           | IDENTIFIER
           | INTEGER
           | yangConstruct;

    identifier : STRING (PLUS STRING)*
               | IDENTIFIER
               | yangConstruct;

    dateArgumentString : DATE_ARG
                       | STRING (PLUS STRING)*;

    version : string;

    range : string;

    length : string;

    path : string;

    position : string;

    status : string;

    config : string;

    mandatory : string;

    orderedBy : string;

    minValue : string;

    maxValue : string;

    key : string;

    unique : string;

    refine : string;

    augment : string;

    deviation : string;

    value : string;

    fraction : string;

    yangConstruct : ANYXML_KEYWORD | ARGUMENT_KEYWORD | AUGMENT_KEYWORD | BASE_KEYWORD | BELONGS_TO_KEYWORD
                  | BIT_KEYWORD | CASE_KEYWORD | CHOICE_KEYWORD | CONFIG_KEYWORD | CONTACT_KEYWORD | CONTAINER_KEYWORD
                  | DEFAULT_KEYWORD | DESCRIPTION_KEYWORD | ENUM_KEYWORD ERROR_APP_TAG_KEYWORD | ERROR_MESSAGE_KEYWORD
                  | EXTENSION_KEYWORD | DEVIATION_KEYWORD | DEVIATE_KEYWORD | FEATURE_KEYWORD
                  | FRACTION_DIGITS_KEYWORD | GROUPING_KEYWORD | IDENTITY_KEYWORD | IF_FEATURE_KEYWORD
                  | IMPORT_KEYWORD | INCLUDE_KEYWORD | INPUT_KEYWORD | KEY_KEYWORD | LEAF_KEYWORD | LEAF_LIST_KEYWORD
                  | LENGTH_KEYWORD | LIST_KEYWORD | MANDATORY_KEYWORD | MAX_ELEMENTS_KEYWORD | MIN_ELEMENTS_KEYWORD
                  | MODULE_KEYWORD | MUST_KEYWORD | NAMESPACE_KEYWORD | NOTIFICATION_KEYWORD | ORDERED_BY_KEYWORD
                  | ORGANIZATION_KEYWORD | OUTPUT_KEYWORD | PATH_KEYWORD | PATTERN_KEYWORD |POSITION_KEYWORD
                  | PREFIX_KEYWORD | PRESENCE_KEYWORD | RANGE_KEYWORD | REFERENCE_KEYWORD | REFINE_KEYWORD
                  | REQUIRE_INSTANCE_KEYWORD | REVISION_KEYWORD | REVISION_DATE_KEYWORD | RPC_KEYWORD
                  | STATUS_KEYWORD | SUBMODULE_KEYWORD | TYPE_KEYWORD | TYPEDEF_KEYWORD | UNIQUE_KEYWORD
                  | UNITS_KEYWORD | USES_KEYWORD | VALUE_KEYWORD | WHEN_KEYWORD | YANG_VERSION_KEYWORD
                  | YIN_ELEMENT_KEYWORD | ADD_KEYWORD | CURRENT_KEYWORD | DELETE_KEYWORD | DEPRECATED_KEYWORD
                  | FALSE_KEYWORD | MAX_KEYWORD | MIN_KEYWORD | NOT_SUPPORTED_KEYWORD | OBSOLETE_KEYWORD
                  | REPLACE_KEYWORD | SYSTEM_KEYWORD | TRUE_KEYWORD | UNBOUNDED_KEYWORD | USER_KEYWORD;
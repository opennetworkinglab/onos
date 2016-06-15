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

// Generated from GeneratedYang.g4 by ANTLR 4.5


package org.onosproject.yangutils.parser.antlrgencode;

import org.antlr.v4.runtime.tree.ParseTreeListener;

/**
 * Represents ANTLR interfaces to be implemented by listener to traverse the parse tree.
 */
public interface GeneratedYangListener extends ParseTreeListener {

    /**
     * Enters a parse tree produced by GeneratedYangParser for grammar rule
     * yangfile.
     *
     * @param currentContext current context in the parsed tree
     */
    void enterYangfile(GeneratedYangParser.YangfileContext currentContext);

    /**
     * Exits a parse tree produced by GeneratedYangParser for grammar rule
     * yangfile.
     *
     * @param currentContext current context in the parsed tree
     */
    void exitYangfile(GeneratedYangParser.YangfileContext currentContext);

    /**
     * Enters a parse tree produced by GeneratedYangParser for grammar rule
     * moduleStatement.
     *
     * @param currentContext current context in the parsed tree
     */
    void enterModuleStatement(GeneratedYangParser.ModuleStatementContext currentContext);

    /**
     * Exits a parse tree produced by GeneratedYangParser for grammar rule
     * moduleStatement.
     *
     * @param currentContext current context in the parsed tree
     */
    void exitModuleStatement(GeneratedYangParser.ModuleStatementContext currentContext);

    /**
     * Enters a parse tree produced by GeneratedYangParser for grammar rule
     * moduleBody.
     *
     * @param currentContext current context in the parsed tree
     */
    void enterModuleBody(GeneratedYangParser.ModuleBodyContext currentContext);

    /**
     * Exits a parse tree produced by GeneratedYangParser for grammar rule
     * moduleBody.
     *
     * @param currentContext current context in the parsed tree
     */
    void exitModuleBody(GeneratedYangParser.ModuleBodyContext currentContext);

    /**
     * Enters a parse tree produced by GeneratedYangParser for grammar rule
     * moduleHeaderStatement.
     *
     * @param currentContext current context in the parsed tree
     */
    void enterModuleHeaderStatement(GeneratedYangParser.ModuleHeaderStatementContext currentContext);

    /**
     * Exits a parse tree produced by GeneratedYangParser for grammar rule
     * moduleHeaderStatement.
     *
     * @param currentContext current context in the parsed tree
     */
    void exitModuleHeaderStatement(GeneratedYangParser.ModuleHeaderStatementContext currentContext);

    /**
     * Enters a parse tree produced by GeneratedYangParser for grammar rule
     * linkageStatements.
     *
     * @param currentContext current context in the parsed tree
     */
    void enterLinkageStatements(GeneratedYangParser.LinkageStatementsContext currentContext);

    /**
     * Exits a parse tree produced by GeneratedYangParser for grammar rule
     * linkageStatements.
     *
     * @param currentContext current context in the parsed tree
     */
    void exitLinkageStatements(GeneratedYangParser.LinkageStatementsContext currentContext);

    /**
     * Enters a parse tree produced by GeneratedYangParser for grammar rule
     * metaStatements.
     *
     * @param currentContext current context in the parsed tree
     */
    void enterMetaStatements(GeneratedYangParser.MetaStatementsContext currentContext);

    /**
     * Exits a parse tree produced by GeneratedYangParser for grammar rule
     * metaStatements.
     *
     * @param currentContext current context in the parsed tree
     */
    void exitMetaStatements(GeneratedYangParser.MetaStatementsContext currentContext);

    /**
     * Enters a parse tree produced by GeneratedYangParser for grammar rule
     * revisionStatements.
     *
     * @param currentContext current context in the parsed tree
     */
    void enterRevisionStatements(GeneratedYangParser.RevisionStatementsContext currentContext);

    /**
     * Exits a parse tree produced by GeneratedYangParser for grammar rule
     * revisionStatements.
     *
     * @param currentContext current context in the parsed tree
     */
    void exitRevisionStatements(GeneratedYangParser.RevisionStatementsContext currentContext);

    /**
     * Enters a parse tree produced by GeneratedYangParser for grammar rule
     * bodyStatements.
     *
     * @param currentContext current context in the parsed tree
     */
    void enterBodyStatements(GeneratedYangParser.BodyStatementsContext currentContext);

    /**
     * Exits a parse tree produced by GeneratedYangParser for grammar rule
     * bodyStatements.
     *
     * @param currentContext current context in the parsed tree
     */
    void exitBodyStatements(GeneratedYangParser.BodyStatementsContext currentContext);

    /**
     * Enters a parse tree produced by GeneratedYangParser for grammar rule
     * yangVersionStatement.
     *
     * @param currentContext current context in the parsed tree
     */
    void enterYangVersionStatement(GeneratedYangParser.YangVersionStatementContext currentContext);

    /**
     * Exits a parse tree produced by GeneratedYangParser for grammar rule
     * yangVersionStatement.
     *
     * @param currentContext current context in the parsed tree
     */
    void exitYangVersionStatement(GeneratedYangParser.YangVersionStatementContext currentContext);

    /**
     * Enters a parse tree produced by GeneratedYangParser for grammar rule
     * namespaceStatement.
     *
     * @param currentContext current context in the parsed tree
     */
    void enterNamespaceStatement(GeneratedYangParser.NamespaceStatementContext currentContext);

    /**
     * Exits a parse tree produced by GeneratedYangParser for grammar rule
     * namespaceStatement.
     *
     * @param currentContext current context in the parsed tree
     */
    void exitNamespaceStatement(GeneratedYangParser.NamespaceStatementContext currentContext);

    /**
     * Enters a parse tree produced by GeneratedYangParser for grammar rule
     * prefixStatement.
     *
     * @param currentContext current context in the parsed tree
     */
    void enterPrefixStatement(GeneratedYangParser.PrefixStatementContext currentContext);

    /**
     * Exits a parse tree produced by GeneratedYangParser for grammar rule
     * prefixStatement.
     *
     * @param currentContext current context in the parsed tree
     */
    void exitPrefixStatement(GeneratedYangParser.PrefixStatementContext currentContext);

    /**
     * Enters a parse tree produced by GeneratedYangParser for grammar rule
     * importStatement.
     *
     * @param currentContext current context in the parsed tree
     */
    void enterImportStatement(GeneratedYangParser.ImportStatementContext currentContext);

    /**
     * Exits a parse tree produced by GeneratedYangParser for grammar rule
     * importStatement.
     *
     * @param currentContext current context in the parsed tree
     */
    void exitImportStatement(GeneratedYangParser.ImportStatementContext currentContext);

    /**
     * Enters a parse tree produced by GeneratedYangParser for grammar rule
     * importStatementBody.
     *
     * @param currentContext current context in the parsed tree
     */
    void enterImportStatementBody(GeneratedYangParser.ImportStatementBodyContext currentContext);

    /**
     * Exits a parse tree produced by GeneratedYangParser for grammar rule
     * importStatementBody.
     *
     * @param currentContext current context in the parsed tree
     */
    void exitImportStatementBody(GeneratedYangParser.ImportStatementBodyContext currentContext);

    /**
     * Enters a parse tree produced by GeneratedYangParser for grammar rule
     * revisionDateStatement.
     *
     * @param currentContext current context in the parsed tree
     */
    void enterRevisionDateStatement(GeneratedYangParser.RevisionDateStatementContext currentContext);

    /**
     * Exits a parse tree produced by GeneratedYangParser for grammar rule
     * revisionDateStatement.
     *
     * @param currentContext current context in the parsed tree
     */
    void exitRevisionDateStatement(GeneratedYangParser.RevisionDateStatementContext currentContext);

    /**
     * Enters a parse tree produced by GeneratedYangParser for grammar rule
     * includeStatement.
     *
     * @param currentContext current context in the parsed tree
     */
    void enterIncludeStatement(GeneratedYangParser.IncludeStatementContext currentContext);

    /**
     * Exits a parse tree produced by GeneratedYangParser for grammar rule
     * includeStatement.
     *
     * @param currentContext current context in the parsed tree
     */
    void exitIncludeStatement(GeneratedYangParser.IncludeStatementContext currentContext);

    /**
     * Enters a parse tree produced by GeneratedYangParser for grammar rule
     * organizationStatement.
     *
     * @param currentContext current context in the parsed tree
     */
    void enterOrganizationStatement(GeneratedYangParser.OrganizationStatementContext currentContext);

    /**
     * Exits a parse tree produced by GeneratedYangParser for grammar rule
     * organizationStatement.
     *
     * @param currentContext current context in the parsed tree
     */
    void exitOrganizationStatement(GeneratedYangParser.OrganizationStatementContext currentContext);

    /**
     * Enters a parse tree produced by GeneratedYangParser for grammar rule
     * contactStatement.
     *
     * @param currentContext current context in the parsed tree
     */
    void enterContactStatement(GeneratedYangParser.ContactStatementContext currentContext);

    /**
     * Exits a parse tree produced by GeneratedYangParser for grammar rule
     * contactStatement.
     *
     * @param currentContext current context in the parsed tree
     */
    void exitContactStatement(GeneratedYangParser.ContactStatementContext currentContext);

    /**
     * Enters a parse tree produced by GeneratedYangParser for grammar rule
     * descriptionStatement.
     *
     * @param currentContext current context in the parsed tree
     */
    void enterDescriptionStatement(GeneratedYangParser.DescriptionStatementContext currentContext);

    /**
     * Exits a parse tree produced by GeneratedYangParser for grammar rule
     * descriptionStatement.
     *
     * @param currentContext current context in the parsed tree
     */
    void exitDescriptionStatement(GeneratedYangParser.DescriptionStatementContext currentContext);

    /**
     * Enters a parse tree produced by GeneratedYangParser for grammar rule
     * referenceStatement.
     *
     * @param currentContext current context in the parsed tree
     */
    void enterReferenceStatement(GeneratedYangParser.ReferenceStatementContext currentContext);

    /**
     * Exits a parse tree produced by GeneratedYangParser for grammar rule
     * referenceStatement.
     *
     * @param currentContext current context in the parsed tree
     */
    void exitReferenceStatement(GeneratedYangParser.ReferenceStatementContext currentContext);

    /**
     * Enters a parse tree produced by GeneratedYangParser for grammar rule
     * revisionStatement.
     *
     * @param currentContext current context in the parsed tree
     */
    void enterRevisionStatement(GeneratedYangParser.RevisionStatementContext currentContext);

    /**
     * Exits a parse tree produced by GeneratedYangParser for grammar rule
     * revisionStatement.
     *
     * @param currentContext current context in the parsed tree
     */
    void exitRevisionStatement(GeneratedYangParser.RevisionStatementContext currentContext);

    /**
     * Enters a parse tree produced by GeneratedYangParser for grammar rule
     * revisionStatementBody.
     *
     * @param currentContext current context in the parsed tree
     */
    void enterRevisionStatementBody(GeneratedYangParser.RevisionStatementBodyContext currentContext);

    /**
     * Exits a parse tree produced by GeneratedYangParser for grammar rule
     * revisionStatementBody.
     *
     * @param currentContext current context in the parsed tree
     */
    void exitRevisionStatementBody(GeneratedYangParser.RevisionStatementBodyContext currentContext);

    /**
     * Enters a parse tree produced by GeneratedYangParser for grammar rule
     * subModuleStatement.
     *
     * @param currentContext current context in the parsed tree
     */
    void enterSubModuleStatement(GeneratedYangParser.SubModuleStatementContext currentContext);

    /**
     * Exits a parse tree produced by GeneratedYangParser for grammar rule
     * subModuleStatement.
     *
     * @param currentContext current context in the parsed tree
     */
    void exitSubModuleStatement(GeneratedYangParser.SubModuleStatementContext currentContext);

    /**
     * Enters a parse tree produced by GeneratedYangParser for grammar rule
     * submoduleBody.
     *
     * @param currentContext current context in the parsed tree
     */
    void enterSubmoduleBody(GeneratedYangParser.SubmoduleBodyContext currentContext);

    /**
     * Exits a parse tree produced by GeneratedYangParser for grammar rule
     * submoduleBody.
     *
     * @param currentContext current context in the parsed tree
     */
    void exitSubmoduleBody(GeneratedYangParser.SubmoduleBodyContext currentContext);

    /**
     * Enters a parse tree produced by GeneratedYangParser for grammar rule
     * submoduleHeaderStatement.
     *
     * @param currentContext current context in the parsed tree
     */
    void enterSubmoduleHeaderStatement(GeneratedYangParser.SubmoduleHeaderStatementContext currentContext);

    /**
     * Exits a parse tree produced by GeneratedYangParser for grammar rule
     * submoduleHeaderStatement.
     *
     * @param currentContext current context in the parsed tree
     */
    void exitSubmoduleHeaderStatement(GeneratedYangParser.SubmoduleHeaderStatementContext currentContext);

    /**
     * Enters a parse tree produced by GeneratedYangParser for grammar rule
     * belongstoStatement.
     *
     * @param currentContext current context in the parsed tree
     */
    void enterBelongstoStatement(GeneratedYangParser.BelongstoStatementContext currentContext);

    /**
     * Exits a parse tree produced by GeneratedYangParser for grammar rule
     * belongstoStatement.
     *
     * @param currentContext current context in the parsed tree
     */
    void exitBelongstoStatement(GeneratedYangParser.BelongstoStatementContext currentContext);

    /**
     * Enters a parse tree produced by GeneratedYangParser for grammar rule
     * belongstoStatementBody.
     *
     * @param currentContext current context in the parsed tree
     */
    void enterBelongstoStatementBody(GeneratedYangParser.BelongstoStatementBodyContext currentContext);

    /**
     * Exits a parse tree produced by GeneratedYangParser for grammar rule
     * belongstoStatementBody.
     *
     * @param currentContext current context in the parsed tree
     */
    void exitBelongstoStatementBody(GeneratedYangParser.BelongstoStatementBodyContext currentContext);

    /**
     * Enters a parse tree produced by GeneratedYangParser for grammar rule
     * extensionStatement.
     *
     * @param currentContext current context in the parsed tree
     */
    void enterExtensionStatement(GeneratedYangParser.ExtensionStatementContext currentContext);

    /**
     * Exits a parse tree produced by GeneratedYangParser for grammar rule
     * extensionStatement.
     *
     * @param currentContext current context in the parsed tree
     */
    void exitExtensionStatement(GeneratedYangParser.ExtensionStatementContext currentContext);

    /**
     * Enters a parse tree produced by GeneratedYangParser for grammar rule
     * extensionBody.
     *
     * @param currentContext current context in the parsed tree
     */
    void enterExtensionBody(GeneratedYangParser.ExtensionBodyContext currentContext);

    /**
     * Exits a parse tree produced by GeneratedYangParser for grammar rule
     * extensionBody.
     *
     * @param currentContext current context in the parsed tree
     */
    void exitExtensionBody(GeneratedYangParser.ExtensionBodyContext currentContext);

    /**
     * Enters a parse tree produced by GeneratedYangParser for grammar rule
     * argumentStatement.
     *
     * @param currentContext current context in the parsed tree
     */
    void enterArgumentStatement(GeneratedYangParser.ArgumentStatementContext currentContext);

    /**
     * Exits a parse tree produced by GeneratedYangParser for grammar rule
     * argumentStatement.
     *
     * @param currentContext current context in the parsed tree
     */
    void exitArgumentStatement(GeneratedYangParser.ArgumentStatementContext currentContext);

    /**
     * Enters a parse tree produced by GeneratedYangParser for grammar rule
     * argumentBody.
     *
     * @param currentContext current context in the parsed tree
     */
    void enterArgumentBody(GeneratedYangParser.ArgumentBodyContext currentContext);

    /**
     * Exits a parse tree produced by GeneratedYangParser for grammar rule
     * argumentBody.
     *
     * @param currentContext current context in the parsed tree
     */
    void exitArgumentBody(GeneratedYangParser.ArgumentBodyContext currentContext);

    /**
     * Enters a parse tree produced by GeneratedYangParser for grammar rule
     * yinElementStatement.
     *
     * @param currentContext current context in the parsed tree
     */
    void enterYinElementStatement(GeneratedYangParser.YinElementStatementContext currentContext);

    /**
     * Exits a parse tree produced by GeneratedYangParser for grammar rule
     * yinElementStatement.
     *
     * @param currentContext current context in the parsed tree
     */
    void exitYinElementStatement(GeneratedYangParser.YinElementStatementContext currentContext);

    /**
     * Enters a parse tree produced by GeneratedYangParser for grammar rule
     * identityStatement.
     *
     * @param currentContext current context in the parsed tree
     */
    void enterIdentityStatement(GeneratedYangParser.IdentityStatementContext currentContext);

    /**
     * Exits a parse tree produced by GeneratedYangParser for grammar rule
     * identityStatement.
     *
     * @param currentContext current context in the parsed tree
     */
    void exitIdentityStatement(GeneratedYangParser.IdentityStatementContext currentContext);

    /**
     * Enters a parse tree produced by GeneratedYangParser for grammar rule
     * identityBody.
     *
     * @param currentContext current context in the parsed tree
     */
    void enterIdentityBody(GeneratedYangParser.IdentityBodyContext currentContext);

    /**
     * Exits a parse tree produced by GeneratedYangParser for grammar rule
     * identityBody.
     *
     * @param currentContext current context in the parsed tree
     */
    void exitIdentityBody(GeneratedYangParser.IdentityBodyContext currentContext);

    /**
     * Enters a parse tree produced by GeneratedYangParser for grammar rule
     * baseStatement.
     *
     * @param currentContext current context in the parsed tree
     */
    void enterBaseStatement(GeneratedYangParser.BaseStatementContext currentContext);

    /**
     * Exits a parse tree produced by GeneratedYangParser for grammar rule
     * baseStatement.
     *
     * @param currentContext current context in the parsed tree
     */
    void exitBaseStatement(GeneratedYangParser.BaseStatementContext currentContext);

    /**
     * Enters a parse tree produced by GeneratedYangParser for grammar rule
     * featureStatement.
     *
     * @param currentContext current context in the parsed tree
     */
    void enterFeatureStatement(GeneratedYangParser.FeatureStatementContext currentContext);

    /**
     * Exits a parse tree produced by GeneratedYangParser for grammar rule
     * featureStatement.
     *
     * @param currentContext current context in the parsed tree
     */
    void exitFeatureStatement(GeneratedYangParser.FeatureStatementContext currentContext);

    /**
     * Enters a parse tree produced by GeneratedYangParser for grammar rule
     * featureBody.
     *
     * @param currentContext current context in the parsed tree
     */
    void enterFeatureBody(GeneratedYangParser.FeatureBodyContext currentContext);

    /**
     * Exits a parse tree produced by GeneratedYangParser for grammar rule
     * featureBody.
     *
     * @param currentContext current context in the parsed tree
     */
    void exitFeatureBody(GeneratedYangParser.FeatureBodyContext currentContext);

    /**
     * Enters a parse tree produced by GeneratedYangParser for grammar rule
     * dataDefStatement.
     *
     * @param currentContext current context in the parsed tree
     */
    void enterDataDefStatement(GeneratedYangParser.DataDefStatementContext currentContext);

    /**
     * Exits a parse tree produced by GeneratedYangParser for grammar rule
     * dataDefStatement.
     *
     * @param currentContext current context in the parsed tree
     */
    void exitDataDefStatement(GeneratedYangParser.DataDefStatementContext currentContext);

    /**
     * Enters a parse tree produced by GeneratedYangParser for grammar rule
     * ifFeatureStatement.
     *
     * @param currentContext current context in the parsed tree
     */
    void enterIfFeatureStatement(GeneratedYangParser.IfFeatureStatementContext currentContext);

    /**
     * Exits a parse tree produced by GeneratedYangParser for grammar rule
     * ifFeatureStatement.
     *
     * @param currentContext current context in the parsed tree
     */
    void exitIfFeatureStatement(GeneratedYangParser.IfFeatureStatementContext currentContext);

    /**
     * Enters a parse tree produced by GeneratedYangParser for grammar rule
     * unitsStatement.
     *
     * @param currentContext current context in the parsed tree
     */
    void enterUnitsStatement(GeneratedYangParser.UnitsStatementContext currentContext);

    /**
     * Exits a parse tree produced by GeneratedYangParser for grammar rule
     * unitsStatement.
     *
     * @param currentContext current context in the parsed tree
     */
    void exitUnitsStatement(GeneratedYangParser.UnitsStatementContext currentContext);

    /**
     * Enters a parse tree produced by GeneratedYangParser for grammar rule
     * typedefStatement.
     *
     * @param currentContext current context in the parsed tree
     */
    void enterTypedefStatement(GeneratedYangParser.TypedefStatementContext currentContext);

    /**
     * Exits a parse tree produced by GeneratedYangParser for grammar rule
     * typedefStatement.
     *
     * @param currentContext current context in the parsed tree
     */
    void exitTypedefStatement(GeneratedYangParser.TypedefStatementContext currentContext);

    /**
     * Enters a parse tree produced by GeneratedYangParser for grammar rule
     * typeStatement.
     *
     * @param currentContext current context in the parsed tree
     */
    void enterTypeStatement(GeneratedYangParser.TypeStatementContext currentContext);

    /**
     * Exits a parse tree produced by GeneratedYangParser for grammar rule
     * typeStatement.
     *
     * @param currentContext current context in the parsed tree
     */
    void exitTypeStatement(GeneratedYangParser.TypeStatementContext currentContext);

    /**
     * Enters a parse tree produced by GeneratedYangParser for grammar rule
     * typeBodyStatements.
     *
     * @param currentContext current context in the parsed tree
     */
    void enterTypeBodyStatements(GeneratedYangParser.TypeBodyStatementsContext currentContext);

    /**
     * Exits a parse tree produced by GeneratedYangParser for grammar rule
     * typeBodyStatements.
     *
     * @param currentContext current context in the parsed tree
     */
    void exitTypeBodyStatements(GeneratedYangParser.TypeBodyStatementsContext currentContext);

    /**
     * Enters a parse tree produced by GeneratedYangParser for grammar rule
     * numericalRestrictions.
     *
     * @param currentContext current context in the parsed tree
     */
    void enterDecimal64Specification(GeneratedYangParser.Decimal64SpecificationContext currentContext);

    /**
     * Exits a parse tree produced by GeneratedYangParser for grammar rule
     * numericalRestrictions.
     *
     * @param currentContext current context in the parsed tree
     */
    void exitDecimal64Specification(GeneratedYangParser.Decimal64SpecificationContext currentContext);

    /**
     * Enters a parse tree produced by GeneratedYangParser for grammar rule
     * numericalRestrictions.
     *
     * @param currentContext current context in the parsed tree
     */
    void enterNumericalRestrictions(GeneratedYangParser.NumericalRestrictionsContext currentContext);

    /**
     * Exits a parse tree produced by GeneratedYangParser for grammar rule
     * numericalRestrictions.
     *
     * @param currentContext current context in the parsed tree
     */
    void exitNumericalRestrictions(GeneratedYangParser.NumericalRestrictionsContext currentContext);

    /**
     * Enters a parse tree produced by GeneratedYangParser for grammar rule
     * rangeStatement.
     *
     * @param currentContext current context in the parsed tree
     */
    void enterRangeStatement(GeneratedYangParser.RangeStatementContext currentContext);

    /**
     * Exits a parse tree produced by GeneratedYangParser for grammar rule
     * rangeStatement.
     *
     * @param currentContext current context in the parsed tree
     */
    void exitRangeStatement(GeneratedYangParser.RangeStatementContext currentContext);

    /**
     * Enters a parse tree produced by GeneratedYangParser for grammar rule
     * commonStatements.
     *
     * @param currentContext current context in the parsed tree
     */
    void enterCommonStatements(GeneratedYangParser.CommonStatementsContext currentContext);

    /**
     * Exits a parse tree produced by GeneratedYangParser for grammar rule
     * commonStatements.
     *
     * @param currentContext current context in the parsed tree
     */
    void exitCommonStatements(GeneratedYangParser.CommonStatementsContext currentContext);

    /**
     * Enters a parse tree produced by GeneratedYangParser for grammar rule
     * stringRestrictions.
     *
     * @param currentContext current context in the parsed tree
     */
    void enterStringRestrictions(GeneratedYangParser.StringRestrictionsContext currentContext);

    /**
     * Exits a parse tree produced by GeneratedYangParser for grammar rule
     * stringRestrictions.
     *
     * @param currentContext current context in the parsed tree
     */
    void exitStringRestrictions(GeneratedYangParser.StringRestrictionsContext currentContext);

    /**
     * Enters a parse tree produced by GeneratedYangParser for grammar rule
     * lengthStatement.
     *
     * @param currentContext current context in the parsed tree
     */
    void enterLengthStatement(GeneratedYangParser.LengthStatementContext currentContext);

    /**
     * Exits a parse tree produced by GeneratedYangParser for grammar rule
     * lengthStatement.
     *
     * @param currentContext current context in the parsed tree
     */
    void exitLengthStatement(GeneratedYangParser.LengthStatementContext currentContext);

    /**
     * Enters a parse tree produced by GeneratedYangParser for grammar rule
     * patternStatement.
     *
     * @param currentContext current context in the parsed tree
     */
    void enterPatternStatement(GeneratedYangParser.PatternStatementContext currentContext);

    /**
     * Exits a parse tree produced by GeneratedYangParser for grammar rule
     * patternStatement.
     *
     * @param currentContext current context in the parsed tree
     */
    void exitPatternStatement(GeneratedYangParser.PatternStatementContext currentContext);

    /**
     * Enters a parse tree produced by GeneratedYangParser for grammar rule
     * defaultStatement.
     *
     * @param currentContext current context in the parsed tree
     */
    void enterDefaultStatement(GeneratedYangParser.DefaultStatementContext currentContext);

    /**
     * Exits a parse tree produced by GeneratedYangParser for grammar rule
     * defaultStatement.
     *
     * @param currentContext current context in the parsed tree
     */
    void exitDefaultStatement(GeneratedYangParser.DefaultStatementContext currentContext);

    /**
     * Enters a parse tree produced by GeneratedYangParser for grammar rule
     * enumSpecification.
     *
     * @param currentContext current context in the parsed tree
     */
    void enterEnumSpecification(GeneratedYangParser.EnumSpecificationContext currentContext);

    /**
     * Exits a parse tree produced by GeneratedYangParser for grammar rule
     * enumSpecification.
     *
     * @param currentContext current context in the parsed tree
     */
    void exitEnumSpecification(GeneratedYangParser.EnumSpecificationContext currentContext);

    /**
     * Enters a parse tree produced by GeneratedYangParser for grammar rule
     * enumStatement.
     *
     * @param currentContext current context in the parsed tree
     */
    void enterEnumStatement(GeneratedYangParser.EnumStatementContext currentContext);

    /**
     * Exits a parse tree produced by GeneratedYangParser for grammar rule
     * enumStatement.
     *
     * @param currentContext current context in the parsed tree
     */
    void exitEnumStatement(GeneratedYangParser.EnumStatementContext currentContext);

    /**
     * Enters a parse tree produced by GeneratedYangParser for grammar rule
     * enumStatementBody.
     *
     * @param currentContext current context in the parsed tree
     */
    void enterEnumStatementBody(GeneratedYangParser.EnumStatementBodyContext currentContext);

    /**
     * Exits a parse tree produced by GeneratedYangParser for grammar rule
     * enumStatementBody.
     *
     * @param currentContext current context in the parsed tree
     */
    void exitEnumStatementBody(GeneratedYangParser.EnumStatementBodyContext currentContext);

    /**
     * Enters a parse tree produced by GeneratedYangParser for grammar rule
     * leafrefSpecification.
     *
     * @param currentContext current context in the parsed tree
     */
    void enterLeafrefSpecification(GeneratedYangParser.LeafrefSpecificationContext currentContext);

    /**
     * Exits a parse tree produced by GeneratedYangParser for grammar rule
     * leafrefSpecification.
     *
     * @param currentContext current context in the parsed tree
     */
    void exitLeafrefSpecification(GeneratedYangParser.LeafrefSpecificationContext currentContext);

    /**
     * Enters a parse tree produced by GeneratedYangParser for grammar rule
     * pathStatement.
     *
     * @param currentContext current context in the parsed tree
     */
    void enterPathStatement(GeneratedYangParser.PathStatementContext currentContext);

    /**
     * Exits a parse tree produced by GeneratedYangParser for grammar rule
     * pathStatement.
     *
     * @param currentContext current context in the parsed tree
     */
    void exitPathStatement(GeneratedYangParser.PathStatementContext currentContext);

    /**
     * Enters a parse tree produced by GeneratedYangParser for grammar rule
     * requireInstanceStatement.
     *
     * @param currentContext current context in the parsed tree
     */
    void enterRequireInstanceStatement(GeneratedYangParser.RequireInstanceStatementContext currentContext);

    /**
     * Exits a parse tree produced by GeneratedYangParser for grammar rule
     * requireInstanceStatement.
     *
     * @param currentContext current context in the parsed tree
     */
    void exitRequireInstanceStatement(GeneratedYangParser.RequireInstanceStatementContext currentContext);

    /**
     * Enters a parse tree produced by GeneratedYangParser for grammar rule
     * instanceIdentifierSpecification.
     *
     * @param currentContext current context in the parsed tree
     */
    void enterInstanceIdentifierSpecification(
            GeneratedYangParser.InstanceIdentifierSpecificationContext currentContext);

    /**
     * Exits a parse tree produced by GeneratedYangParser for grammar rule
     * instanceIdentifierSpecification.
     *
     * @param currentContext current context in the parsed tree
     */
    void exitInstanceIdentifierSpecification(GeneratedYangParser.InstanceIdentifierSpecificationContext currentContext);

    /**
     * Enters a parse tree produced by GeneratedYangParser for grammar rule
     * identityrefSpecification.
     *
     * @param currentContext current context in the parsed tree
     */
    void enterIdentityrefSpecification(GeneratedYangParser.IdentityrefSpecificationContext currentContext);

    /**
     * Exits a parse tree produced by GeneratedYangParser for grammar rule
     * identityrefSpecification.
     *
     * @param currentContext current context in the parsed tree
     */
    void exitIdentityrefSpecification(GeneratedYangParser.IdentityrefSpecificationContext currentContext);

    /**
     * Enters a parse tree produced by GeneratedYangParser for grammar rule
     * unionSpecification.
     *
     * @param currentContext current context in the parsed tree
     */
    void enterUnionSpecification(GeneratedYangParser.UnionSpecificationContext currentContext);

    /**
     * Exits a parse tree produced by GeneratedYangParser for grammar rule
     * unionSpecification.
     *
     * @param currentContext current context in the parsed tree
     */
    void exitUnionSpecification(GeneratedYangParser.UnionSpecificationContext currentContext);

    /**
     * Enters a parse tree produced by GeneratedYangParser for grammar rule
     * bitsSpecification.
     *
     * @param currentContext current context in the parsed tree
     */
    void enterBitsSpecification(GeneratedYangParser.BitsSpecificationContext currentContext);

    /**
     * Exits a parse tree produced by GeneratedYangParser for grammar rule
     * bitsSpecification.
     *
     * @param currentContext current context in the parsed tree
     */
    void exitBitsSpecification(GeneratedYangParser.BitsSpecificationContext currentContext);

    /**
     * Enters a parse tree produced by GeneratedYangParser for grammar rule
     * bitStatement.
     *
     * @param currentContext current context in the parsed tree
     */
    void enterBitStatement(GeneratedYangParser.BitStatementContext currentContext);

    /**
     * Exits a parse tree produced by GeneratedYangParser for grammar rule
     * bitStatement.
     *
     * @param currentContext current context in the parsed tree
     */
    void exitBitStatement(GeneratedYangParser.BitStatementContext currentContext);

    /**
     * Enters a parse tree produced by GeneratedYangParser for grammar rule
     * bitBodyStatement.
     *
     * @param currentContext current context in the parsed tree
     */
    void enterBitBodyStatement(GeneratedYangParser.BitBodyStatementContext currentContext);

    /**
     * Exits a parse tree produced by GeneratedYangParser for grammar rule
     * bitBodyStatement.
     *
     * @param currentContext current context in the parsed tree
     */
    void exitBitBodyStatement(GeneratedYangParser.BitBodyStatementContext currentContext);

    /**
     * Enters a parse tree produced by GeneratedYangParser for grammar rule
     * positionStatement.
     *
     * @param currentContext current context in the parsed tree
     */
    void enterPositionStatement(GeneratedYangParser.PositionStatementContext currentContext);

    /**
     * Exits a parse tree produced by GeneratedYangParser for grammar rule
     * positionStatement.
     *
     * @param currentContext current context in the parsed tree
     */
    void exitPositionStatement(GeneratedYangParser.PositionStatementContext currentContext);

    /**
     * Enters a parse tree produced by GeneratedYangParser for grammar rule
     * statusStatement.
     *
     * @param currentContext current context in the parsed tree
     */
    void enterStatusStatement(GeneratedYangParser.StatusStatementContext currentContext);

    /**
     * Exits a parse tree produced by GeneratedYangParser for grammar rule
     * statusStatement.
     *
     * @param currentContext current context in the parsed tree
     */
    void exitStatusStatement(GeneratedYangParser.StatusStatementContext currentContext);

    /**
     * Enters a parse tree produced by GeneratedYangParser for grammar rule
     * configStatement.
     *
     * @param currentContext current context in the parsed tree
     */
    void enterConfigStatement(GeneratedYangParser.ConfigStatementContext currentContext);

    /**
     * Exits a parse tree produced by GeneratedYangParser for grammar rule
     * configStatement.
     *
     * @param currentContext current context in the parsed tree
     */
    void exitConfigStatement(GeneratedYangParser.ConfigStatementContext currentContext);

    /**
     * Enters a parse tree produced by GeneratedYangParser for grammar rule
     * mandatoryStatement.
     *
     * @param currentContext current context in the parsed tree
     */
    void enterMandatoryStatement(GeneratedYangParser.MandatoryStatementContext currentContext);

    /**
     * Exits a parse tree produced by GeneratedYangParser for grammar rule
     * mandatoryStatement.
     *
     * @param currentContext current context in the parsed tree
     */
    void exitMandatoryStatement(GeneratedYangParser.MandatoryStatementContext currentContext);

    /**
     * Enters a parse tree produced by GeneratedYangParser for grammar rule
     * presenceStatement.
     *
     * @param currentContext current context in the parsed tree
     */
    void enterPresenceStatement(GeneratedYangParser.PresenceStatementContext currentContext);

    /**
     * Exits a parse tree produced by GeneratedYangParser for grammar rule
     * presenceStatement.
     *
     * @param currentContext current context in the parsed tree
     */
    void exitPresenceStatement(GeneratedYangParser.PresenceStatementContext currentContext);

    /**
     * Enters a parse tree produced by GeneratedYangParser for grammar rule
     * orderedByStatement.
     *
     * @param currentContext current context in the parsed tree
     */
    void enterOrderedByStatement(GeneratedYangParser.OrderedByStatementContext currentContext);

    /**
     * Exits a parse tree produced by GeneratedYangParser for grammar rule
     * orderedByStatement.
     *
     * @param currentContext current context in the parsed tree
     */
    void exitOrderedByStatement(GeneratedYangParser.OrderedByStatementContext currentContext);

    /**
     * Enters a parse tree produced by GeneratedYangParser for grammar rule
     * mustStatement.
     *
     * @param currentContext current context in the parsed tree
     */
    void enterMustStatement(GeneratedYangParser.MustStatementContext currentContext);

    /**
     * Exits a parse tree produced by GeneratedYangParser for grammar rule
     * mustStatement.
     *
     * @param currentContext current context in the parsed tree
     */
    void exitMustStatement(GeneratedYangParser.MustStatementContext currentContext);

    /**
     * Enters a parse tree produced by GeneratedYangParser for grammar rule
     * errorMessageStatement.
     *
     * @param currentContext current context in the parsed tree
     */
    void enterErrorMessageStatement(GeneratedYangParser.ErrorMessageStatementContext currentContext);

    /**
     * Exits a parse tree produced by GeneratedYangParser for grammar rule
     * errorMessageStatement.
     *
     * @param currentContext current context in the parsed tree
     */
    void exitErrorMessageStatement(GeneratedYangParser.ErrorMessageStatementContext currentContext);

    /**
     * Enters a parse tree produced by GeneratedYangParser for grammar rule
     * errorAppTagStatement.
     *
     * @param currentContext current context in the parsed tree
     */
    void enterErrorAppTagStatement(GeneratedYangParser.ErrorAppTagStatementContext currentContext);

    /**
     * Exits a parse tree produced by GeneratedYangParser for grammar rule
     * errorAppTagStatement.
     *
     * @param currentContext current context in the parsed tree
     */
    void exitErrorAppTagStatement(GeneratedYangParser.ErrorAppTagStatementContext currentContext);

    /**
     * Enters a parse tree produced by GeneratedYangParser for grammar rule
     * minElementsStatement.
     *
     * @param currentContext current context in the parsed tree
     */
    void enterMinElementsStatement(GeneratedYangParser.MinElementsStatementContext currentContext);

    /**
     * Exits a parse tree produced by GeneratedYangParser for grammar rule
     * minElementsStatement.
     *
     * @param currentContext current context in the parsed tree
     */
    void exitMinElementsStatement(GeneratedYangParser.MinElementsStatementContext currentContext);

    /**
     * Enters a parse tree produced by GeneratedYangParser for grammar rule
     * maxElementsStatement.
     *
     * @param currentContext current context in the parsed tree
     */
    void enterMaxElementsStatement(GeneratedYangParser.MaxElementsStatementContext currentContext);

    /**
     * Exits a parse tree produced by GeneratedYangParser for grammar rule
     * maxElementsStatement.
     *
     * @param currentContext current context in the parsed tree
     */
    void exitMaxElementsStatement(GeneratedYangParser.MaxElementsStatementContext currentContext);

    /**
     * Enters a parse tree produced by GeneratedYangParser for grammar rule
     * valueStatement.
     *
     * @param currentContext current context in the parsed tree
     */
    void enterValueStatement(GeneratedYangParser.ValueStatementContext currentContext);

    /**
     * Exits a parse tree produced by GeneratedYangParser for grammar rule
     * valueStatement.
     *
     * @param currentContext current context in the parsed tree
     */
    void exitValueStatement(GeneratedYangParser.ValueStatementContext currentContext);

    /**
     * Enters a parse tree produced by GeneratedYangParser for grammar rule
     * groupingStatement.
     *
     * @param currentContext current context in the parsed tree
     */
    void enterGroupingStatement(GeneratedYangParser.GroupingStatementContext currentContext);

    /**
     * Exits a parse tree produced by GeneratedYangParser for grammar rule
     * groupingStatement.
     *
     * @param currentContext current context in the parsed tree
     */
    void exitGroupingStatement(GeneratedYangParser.GroupingStatementContext currentContext);

    /**
     * Enters a parse tree produced by GeneratedYangParser for grammar rule
     * containerStatement.
     *
     * @param currentContext current context in the parsed tree
     */
    void enterContainerStatement(GeneratedYangParser.ContainerStatementContext currentContext);

    /**
     * Exits a parse tree produced by GeneratedYangParser for grammar rule
     * containerStatement.
     *
     * @param currentContext current context in the parsed tree
     */
    void exitContainerStatement(GeneratedYangParser.ContainerStatementContext currentContext);

    /**
     * Enters a parse tree produced by GeneratedYangParser for grammar rule
     * leafStatement.
     *
     * @param currentContext current context in the parsed tree
     */
    void enterLeafStatement(GeneratedYangParser.LeafStatementContext currentContext);

    /**
     * Exits a parse tree produced by GeneratedYangParser for grammar rule
     * leafStatement.
     *
     * @param currentContext current context in the parsed tree
     */
    void exitLeafStatement(GeneratedYangParser.LeafStatementContext currentContext);

    /**
     * Enters a parse tree produced by GeneratedYangParser for grammar rule
     * leafListStatement.
     *
     * @param currentContext current context in the parsed tree
     */
    void enterLeafListStatement(GeneratedYangParser.LeafListStatementContext currentContext);

    /**
     * Exits a parse tree produced by GeneratedYangParser for grammar rule
     * leafListStatement.
     *
     * @param currentContext current context in the parsed tree
     */
    void exitLeafListStatement(GeneratedYangParser.LeafListStatementContext currentContext);

    /**
     * Enters a parse tree produced by GeneratedYangParser for grammar rule
     * listStatement.
     *
     * @param currentContext current context in the parsed tree
     */
    void enterListStatement(GeneratedYangParser.ListStatementContext currentContext);

    /**
     * Exits a parse tree produced by GeneratedYangParser for grammar rule
     * listStatement.
     *
     * @param currentContext current context in the parsed tree
     */
    void exitListStatement(GeneratedYangParser.ListStatementContext currentContext);

    /**
     * Enters a parse tree produced by GeneratedYangParser for grammar rule
     * keyStatement.
     *
     * @param currentContext current context in the parsed tree
     */
    void enterKeyStatement(GeneratedYangParser.KeyStatementContext currentContext);

    /**
     * Exits a parse tree produced by GeneratedYangParser for grammar rule
     * keyStatement.
     *
     * @param currentContext current context in the parsed tree
     */
    void exitKeyStatement(GeneratedYangParser.KeyStatementContext currentContext);

    /**
     * Enters a parse tree produced by GeneratedYangParser for grammar rule
     * uniqueStatement.
     *
     * @param currentContext current context in the parsed tree
     */
    void enterUniqueStatement(GeneratedYangParser.UniqueStatementContext currentContext);

    /**
     * Exits a parse tree produced by GeneratedYangParser for grammar rule uniqueStatement.
     *
     * @param currentContext current context in the parsed tree
     */
    void exitUniqueStatement(GeneratedYangParser.UniqueStatementContext currentContext);

    /**
     * Enters a parse tree produced by GeneratedYangParser for grammar rule choiceStatement.
     *
     * @param currentContext current context in the parsed tree
     */
    void enterChoiceStatement(GeneratedYangParser.ChoiceStatementContext currentContext);

    /**
     * Exits a parse tree produced by GeneratedYangParser for grammar rule choiceStatement.
     *
     * @param currentContext current context in the parsed tree
     */
    void exitChoiceStatement(GeneratedYangParser.ChoiceStatementContext currentContext);

    /**
     * Enters a parse tree produced by GeneratedYangParser for grammar rule shortCaseStatement.
     *
     * @param currentContext current context in the parsed tree
     */
    void enterShortCaseStatement(GeneratedYangParser.ShortCaseStatementContext currentContext);

    /**
     * Exits a parse tree produced by GeneratedYangParser for grammar rule shortCaseStatement.
     *
     * @param currentContext current context in the parsed tree
     */
    void exitShortCaseStatement(GeneratedYangParser.ShortCaseStatementContext currentContext);

    /**
     * Enters a parse tree produced by GeneratedYangParser for grammar rule caseStatement.
     *
     * @param currentContext current context in the parsed tree
     */
    void enterCaseStatement(GeneratedYangParser.CaseStatementContext currentContext);

    /**
     * Exits a parse tree produced by GeneratedYangParser for grammar rule caseStatement.
     *
     * @param currentContext current context in the parsed tree
     */
    void exitCaseStatement(GeneratedYangParser.CaseStatementContext currentContext);

    /**
     * Enters a parse tree produced by GeneratedYangParser for grammar rule anyxmlStatement.
     *
     * @param currentContext current context in the parsed tree
     */
    void enterAnyxmlStatement(GeneratedYangParser.AnyxmlStatementContext currentContext);

    /**
     * Exits a parse tree produced by GeneratedYangParser for grammar rule anyxmlStatement.
     *
     * @param currentContext current context in the parsed tree
     */
    void exitAnyxmlStatement(GeneratedYangParser.AnyxmlStatementContext currentContext);

    /**
     * Enters a parse tree produced by GeneratedYangParser for grammar rule usesStatement.
     *
     * @param currentContext current context in the parsed tree
     */
    void enterUsesStatement(GeneratedYangParser.UsesStatementContext currentContext);

    /**
     * Exits a parse tree produced by GeneratedYangParser for grammar rule usesStatement.
     *
     * @param currentContext current context in the parsed tree
     */
    void exitUsesStatement(GeneratedYangParser.UsesStatementContext currentContext);

    /**
     * Enters a parse tree produced by GeneratedYangParser for grammar rule refineStatement.
     *
     * @param currentContext current context in the parsed tree
     */
    void enterRefineStatement(GeneratedYangParser.RefineStatementContext currentContext);

    /**
     * Exits a parse tree produced by GeneratedYangParser for grammar rule refineStatement.
     *
     * @param currentContext current context in the parsed tree
     */
    void exitRefineStatement(GeneratedYangParser.RefineStatementContext currentContext);

    /**
     * Enters a parse tree produced by GeneratedYangParser for grammar rule refineContainerStatements.
     *
     * @param currentContext current context in the parsed tree
     */
    void enterRefineContainerStatements(GeneratedYangParser.RefineContainerStatementsContext currentContext);

    /**
     * Exits a parse tree produced by GeneratedYangParser for grammar rule refineContainerStatements.
     *
     * @param currentContext current context in the parsed tree
     */
    void exitRefineContainerStatements(GeneratedYangParser.RefineContainerStatementsContext currentContext);

    /**
     * Enters a parse tree produced by GeneratedYangParser for grammar rule refineLeafStatements.
     *
     * @param currentContext current context in the parsed tree
     */
    void enterRefineLeafStatements(GeneratedYangParser.RefineLeafStatementsContext currentContext);

    /**
     * Exits a parse tree produced by GeneratedYangParser for grammar rule refineLeafStatements.
     *
     * @param currentContext current context in the parsed tree
     */
    void exitRefineLeafStatements(GeneratedYangParser.RefineLeafStatementsContext currentContext);

    /**
     * Enters a parse tree produced by GeneratedYangParser for grammar rule refineLeafListStatements.
     *
     * @param currentContext current context in the parsed tree
     */
    void enterRefineLeafListStatements(GeneratedYangParser.RefineLeafListStatementsContext currentContext);

    /**
     * Exits a parse tree produced by GeneratedYangParser for grammar rule refineLeafListStatements.
     *
     * @param currentContext current context in the parsed tree
     */
    void exitRefineLeafListStatements(GeneratedYangParser.RefineLeafListStatementsContext currentContext);

    /**
     * Enters a parse tree produced by GeneratedYangParser for grammar rule refineListStatements.
     *
     * @param currentContext current context in the parsed tree
     */
    void enterRefineListStatements(GeneratedYangParser.RefineListStatementsContext currentContext);

    /**
     * Exits a parse tree produced by GeneratedYangParser for grammar rule refineListStatements.
     *
     * @param currentContext current context in the parsed tree
     */
    void exitRefineListStatements(GeneratedYangParser.RefineListStatementsContext currentContext);

    /**
     * Enters a parse tree produced by GeneratedYangParser for grammar rule refineChoiceStatements.
     *
     * @param currentContext current context in the parsed tree
     */
    void enterRefineChoiceStatements(GeneratedYangParser.RefineChoiceStatementsContext currentContext);

    /**
     * Exits a parse tree produced by GeneratedYangParser for grammar rule refineChoiceStatements.
     *
     * @param currentContext current context in the parsed tree
     */
    void exitRefineChoiceStatements(GeneratedYangParser.RefineChoiceStatementsContext currentContext);

    /**
     * Enters a parse tree produced by GeneratedYangParser for grammar rule refineCaseStatements.
     *
     * @param currentContext current context in the parsed tree
     */
    void enterRefineCaseStatements(GeneratedYangParser.RefineCaseStatementsContext currentContext);

    /**
     * Exits a parse tree produced by GeneratedYangParser for grammar rule refineCaseStatements.
     *
     * @param currentContext current context in the parsed tree
     */
    void exitRefineCaseStatements(GeneratedYangParser.RefineCaseStatementsContext currentContext);

    /**
     * Enters a parse tree produced by GeneratedYangParser for grammar rule refineAnyxmlStatements.
     *
     * @param currentContext current context in the parsed tree
     */
    void enterRefineAnyxmlStatements(GeneratedYangParser.RefineAnyxmlStatementsContext currentContext);

    /**
     * Exits a parse tree produced by GeneratedYangParser for grammar rule refineAnyxmlStatements.
     *
     * @param currentContext current context in the parsed tree
     */
    void exitRefineAnyxmlStatements(GeneratedYangParser.RefineAnyxmlStatementsContext currentContext);

    /**
     * Enters a parse tree produced by GeneratedYangParser for grammar rule augmentStatement.
     *
     * @param currentContext current context in the parsed tree
     */
    void enterAugmentStatement(GeneratedYangParser.AugmentStatementContext currentContext);

    /**
     * Exits a parse tree produced by GeneratedYangParser for grammar rule augmentStatement.
     *
     * @param currentContext current context in the parsed tree
     */
    void exitAugmentStatement(GeneratedYangParser.AugmentStatementContext currentContext);

    /**
     * Enters a parse tree produced by GeneratedYangParser for grammar rule whenStatement.
     *
     * @param currentContext current context in the parsed tree
     */
    void enterWhenStatement(GeneratedYangParser.WhenStatementContext currentContext);

    /**
     * Exits a parse tree produced by GeneratedYangParser for grammar rule whenStatement.
     *
     * @param currentContext current context in the parsed tree
     */
    void exitWhenStatement(GeneratedYangParser.WhenStatementContext currentContext);

    /**
     * Enters a parse tree produced by GeneratedYangParser for grammar rule rpcStatement.
     *
     * @param currentContext current context in the parsed tree
     */
    void enterRpcStatement(GeneratedYangParser.RpcStatementContext currentContext);

    /**
     * Exits a parse tree produced by GeneratedYangParser for grammar rule rpcStatement.
     *
     * @param currentContext current context in the parsed tree
     */
    void exitRpcStatement(GeneratedYangParser.RpcStatementContext currentContext);

    /**
     * Enters a parse tree produced by GeneratedYangParser for grammar rule inputStatement.
     *
     * @param currentContext current context in the parsed tree
     */
    void enterInputStatement(GeneratedYangParser.InputStatementContext currentContext);

    /**
     * Exits a parse tree produced by GeneratedYangParser for grammar rule inputStatement.
     *
     * @param currentContext current context in the parsed tree
     */
    void exitInputStatement(GeneratedYangParser.InputStatementContext currentContext);

    /**
     * Enters a parse tree produced by GeneratedYangParser for grammar rule outputStatement.
     *
     * @param currentContext current context in the parsed tree
     */
    void enterOutputStatement(GeneratedYangParser.OutputStatementContext currentContext);

    /**
     * Exits a parse tree produced by GeneratedYangParser for grammar rule outputStatement.
     *
     * @param currentContext current context in the parsed tree
     */
    void exitOutputStatement(GeneratedYangParser.OutputStatementContext currentContext);

    /**
     * Enters a parse tree produced by GeneratedYangParser for grammar rule notificationStatement.
     *
     * @param currentContext current context in the parsed tree
     */
    void enterNotificationStatement(GeneratedYangParser.NotificationStatementContext currentContext);

    /**
     * Exits a parse tree produced by GeneratedYangParser for grammar rule notificationStatement.
     *
     * @param currentContext current context in the parsed tree
     */
    void exitNotificationStatement(GeneratedYangParser.NotificationStatementContext currentContext);

    /**
     * Enters a parse tree produced by GeneratedYangParser for grammar rule deviationStatement.
     *
     * @param currentContext current context in the parsed tree
     */
    void enterDeviationStatement(GeneratedYangParser.DeviationStatementContext currentContext);

    /**
     * Exits a parse tree produced by GeneratedYangParser for grammar rule deviationStatement.
     *
     * @param currentContext current context in the parsed tree
     */
    void exitDeviationStatement(GeneratedYangParser.DeviationStatementContext currentContext);

    /**
     * Enters a parse tree produced by GeneratedYangParser for grammar rule deviateNotSupportedStatement.
     *
     * @param currentContext current context in the parsed tree
     */
    void enterDeviateNotSupportedStatement(GeneratedYangParser.DeviateNotSupportedStatementContext currentContext);

    /**
     * Exits a parse tree produced by GeneratedYangParser for grammar rule deviateNotSupportedStatement.
     *
     * @param currentContext current context in the parsed tree
     */
    void exitDeviateNotSupportedStatement(GeneratedYangParser.DeviateNotSupportedStatementContext currentContext);

    /**
     * Enters a parse tree produced by GeneratedYangParser for grammar rule deviateAddStatement.
     *
     * @param currentContext current context in the parsed tree
     */
    void enterDeviateAddStatement(GeneratedYangParser.DeviateAddStatementContext currentContext);

    /**
     * Exits a parse tree produced by GeneratedYangParser for grammar rule deviateAddStatement.
     *
     * @param currentContext current context in the parsed tree
     */
    void exitDeviateAddStatement(GeneratedYangParser.DeviateAddStatementContext currentContext);

    /**
     * Enters a parse tree produced by GeneratedYangParser for grammar rule deviateDeleteStatement.
     *
     * @param currentContext current context in the parsed tree
     */
    void enterDeviateDeleteStatement(GeneratedYangParser.DeviateDeleteStatementContext currentContext);

    /**
     * Exits a parse tree produced by GeneratedYangParser for grammar rule deviateDeleteStatement.
     *
     * @param currentContext current context in the parsed tree
     */
    void exitDeviateDeleteStatement(GeneratedYangParser.DeviateDeleteStatementContext currentContext);

    /**
     * Enters a parse tree produced by GeneratedYangParser for grammar rule deviateReplaceStatement.
     *
     * @param currentContext current context in the parsed tree
     */
    void enterDeviateReplaceStatement(GeneratedYangParser.DeviateReplaceStatementContext currentContext);

    /**
     * Exits a parse tree produced by GeneratedYangParser for grammar rule deviateReplaceStatement.
     *
     * @param currentContext current context in the parsed tree
     */
    void exitDeviateReplaceStatement(GeneratedYangParser.DeviateReplaceStatementContext currentContext);

    /**
     * Enters a parse tree produced by GeneratedYangParser for grammar rule string.
     *
     * @param currentContext current context in the parsed tree
     */
    void enterString(GeneratedYangParser.StringContext currentContext);

    /**
     * Exits a parse tree produced by GeneratedYangParser for grammar rule string.
     *
     * @param currentContext current context in the parsed tree
     */
    void exitString(GeneratedYangParser.StringContext currentContext);

    /**
     * Enters a parse tree produced by GeneratedYangParser for grammar rule identifier.
     *
     * @param currentContext current context in the parsed tree
     */
    void enterIdentifier(GeneratedYangParser.IdentifierContext currentContext);

    /**
     * Exits a parse tree produced by GeneratedYangParser for grammar rule identifier.
     *
     * @param currentContext current context in the parsed tree
     */
    void exitIdentifier(GeneratedYangParser.IdentifierContext currentContext);

    /**
     * Enters a parse tree produced by GeneratedYangParser for grammar rule version.
     *
     * @param currentContext current context in the parsed tree
     */
    void enterVersion(GeneratedYangParser.VersionContext currentContext);

    /**
     * Exits a parse tree produced by GeneratedYangParser for grammar rule version.
     *
     * @param currentContext current context in the parsed tree
     */
    void exitVersion(GeneratedYangParser.VersionContext currentContext);

    /**
     * Enters a parse tree produced by GeneratedYangParser for grammar rule range.
     *
     * @param currentContext current context in the parsed tree
     */
    void enterRange(GeneratedYangParser.RangeContext currentContext);

    /**
     * Exits a parse tree produced by GeneratedYangParser for grammar rule range.
     *
     * @param currentContext current context in the parsed tree
     */
    void exitRange(GeneratedYangParser.RangeContext currentContext);

    /**
     * Enters a parse tree produced by GeneratedYangParser for grammar rule dateArgumentString.
     *
     * @param currentContext current context in the parsed tree
     */
    void enterDateArgumentString(GeneratedYangParser.DateArgumentStringContext currentContext);

    /**
     * Exits a parse tree produced by GeneratedYangParser for grammar rule dateArgumentString.
     *
     * @param currentContext current context in the parsed tree
     */
    void exitDateArgumentString(GeneratedYangParser.DateArgumentStringContext currentContext);

    /**
     * Enters a parse tree produced by GeneratedYangParser for grammar rule length.
     *
     * @param currentContext current context in the parsed tree
     */
    void enterLength(GeneratedYangParser.LengthContext currentContext);

    /**
     * Exits a parse tree produced by GeneratedYangParser for grammar rule length.
     *
     * @param currentContext current context in the parsed tree
     */
    void exitLength(GeneratedYangParser.LengthContext currentContext);

    /**
     * Enters a parse tree produced by GeneratedYangParser for grammar rule path.
     *
     * @param currentContext current context in the parsed tree
     */
    void enterPath(GeneratedYangParser.PathContext currentContext);

    /**
     * Exits a parse tree produced by GeneratedYangParser for grammar rule path.
     *
     * @param currentContext current context in the parsed tree
     */
    void exitPath(GeneratedYangParser.PathContext currentContext);

    /**
     * Enters a parse tree produced by GeneratedYangParser for grammar rule position.
     *
     * @param currentContext current context in the parsed tree
     */
    void enterPosition(GeneratedYangParser.PositionContext currentContext);

    /**
     * Exits a parse tree produced by GeneratedYangParser for grammar rule position.
     *
     * @param currentContext current context in the parsed tree
     */
    void exitPosition(GeneratedYangParser.PositionContext currentContext);

    /**
     * Enters a parse tree produced by GeneratedYangParser for grammar rule status.
     *
     * @param currentContext current context in the parsed tree
     */
    void enterStatus(GeneratedYangParser.StatusContext currentContext);

    /**
     * Exits a parse tree produced by GeneratedYangParser for grammar rule status.
     *
     * @param currentContext current context in the parsed tree
     */
    void exitStatus(GeneratedYangParser.StatusContext currentContext);

    /**
     * Enters a parse tree produced by GeneratedYangParser for grammar rule config.
     *
     * @param currentContext current context in the parsed tree
     */
    void enterConfig(GeneratedYangParser.ConfigContext currentContext);

    /**
     * Exits a parse tree produced by GeneratedYangParser for grammar rule config.
     *
     * @param currentContext current context in the parsed tree
     */
    void exitConfig(GeneratedYangParser.ConfigContext currentContext);

    /**
     * Enters a parse tree produced by GeneratedYangParser for grammar rule mandatory.
     *
     * @param currentContext current context in the parsed tree
     */
    void enterMandatory(GeneratedYangParser.MandatoryContext currentContext);

    /**
     * Exits a parse tree produced by GeneratedYangParser for grammar rule mandatory.
     *
     * @param currentContext current context in the parsed tree
     */
    void exitMandatory(GeneratedYangParser.MandatoryContext currentContext);

    /**
     * Enters a parse tree produced by GeneratedYangParser for grammar rule ordered-by.
     *
     * @param currentContext current context in the parsed tree
     */
    void enterOrderedBy(GeneratedYangParser.OrderedByContext currentContext);

    /**
     * Exits a parse tree produced by GeneratedYangParser for grammar rule ordered-by.
     *
     * @param currentContext current context in the parsed tree
     */
    void exitOrderedBy(GeneratedYangParser.OrderedByContext currentContext);

    /**
     * Enters a parse tree produced by GeneratedYangParser for grammar rule min elements value.
     *
     * @param currentContext current context in the parsed tree
     */
    void enterMinValue(GeneratedYangParser.MinValueContext currentContext);

    /**
     * Exits a parse tree produced by GeneratedYangParser for grammar rule min elements value.
     *
     * @param currentContext current context in the parsed tree
     */
    void exitMinValue(GeneratedYangParser.MinValueContext currentContext);

    /**
     * Enters a parse tree produced by GeneratedYangParser for grammar rule  max elements value.
     *
     * @param currentContext current context in the parsed tree
     */
    void enterMaxValue(GeneratedYangParser.MaxValueContext currentContext);

    /**
     * Exits a parse tree produced by GeneratedYangParser for grammar rule max elements value.
     *
     * @param currentContext current context in the parsed tree
     */
    void exitMaxValue(GeneratedYangParser.MaxValueContext currentContext);

    /**
     * Enters a parse tree produced by GeneratedYangParser for grammar rule key.
     *
     * @param currentContext current context in the parsed tree
     */
    void enterKey(GeneratedYangParser.KeyContext currentContext);

    /**
     * Exits a parse tree produced by GeneratedYangParser for grammar rule key.
     *
     * @param currentContext current context in the parsed tree
     */
    void exitKey(GeneratedYangParser.KeyContext currentContext);

    /**
     * Enters a parse tree produced by GeneratedYangParser for grammar rule unique.
     *
     * @param currentContext current context in the parsed tree
     */
    void enterUnique(GeneratedYangParser.UniqueContext currentContext);

    /**
     * Exits a parse tree produced by GeneratedYangParser for grammar rule unique.
     *
     * @param currentContext current context in the parsed tree
     */
    void exitUnique(GeneratedYangParser.UniqueContext currentContext);

    /**
     * Enters a parse tree produced by GeneratedYangParser for grammar rule refine.
     *
     * @param currentContext current context in the parsed tree
     */
    void enterRefine(GeneratedYangParser.RefineContext currentContext);

    /**
     * Exits a parse tree produced by GeneratedYangParser for grammar rule refine.
     *
     * @param currentContext current context in the parsed tree
     */
    void exitRefine(GeneratedYangParser.RefineContext currentContext);

    /**
     * Enters a parse tree produced by GeneratedYangParser for grammar rule augment.
     *
     * @param currentContext current context in the parsed tree
     */
    void enterAugment(GeneratedYangParser.AugmentContext currentContext);

    /**
     * Exits a parse tree produced by GeneratedYangParser for grammar rule augment.
     *
     * @param currentContext current context in the parsed tree
     */
    void exitAugment(GeneratedYangParser.AugmentContext currentContext);

    /**
     * Enters a parse tree produced by GeneratedYangParser for grammar rule augment.
     *
     * @param currentContext current context in the parsed tree
     */
    void enterFraction(GeneratedYangParser.FractionContext currentContext);

    /**
     * Exits a parse tree produced by GeneratedYangParser for grammar rule augment.
     *
     * @param currentContext current context in the parsed tree
     */
    void exitFraction(GeneratedYangParser.FractionContext currentContext);

    /**
     * Enters a parse tree produced by GeneratedYangParser for grammar rule deviation.
     *
     * @param currentContext current context in the parsed tree
     */
    void enterDeviation(GeneratedYangParser.DeviationContext currentContext);

    /**
     * Exits a parse tree produced by GeneratedYangParser for grammar rule deviation.
     *
     * @param currentContext current context in the parsed tree
     */
    void exitDeviation(GeneratedYangParser.DeviationContext currentContext);

    /**
     * Enters a parse tree produced by GeneratedYangParser for grammar rule deviation.
     *
     * @param currentContext current context in the parsed tree
     */
    void enterValue(GeneratedYangParser.ValueContext currentContext);

    /**
     * Exits a parse tree produced by GeneratedYangParser for grammar rule deviation.
     *
     * @param currentContext current context in the parsed tree
     */
    void exitValue(GeneratedYangParser.ValueContext currentContext);

    /**
     * Enters a parse tree produced by GeneratedYangParser for grammar rule yang construct.
     *
     * @param currentContext current context in the parsed tree
     */
    void enterYangConstruct(GeneratedYangParser.YangConstructContext currentContext);

    /**
     * Exits a parse tree produced by GeneratedYangParser for grammar rule yang construct.
     *
     * @param currentContext current context in the parsed tree
     */
    void exitYangConstruct(GeneratedYangParser.YangConstructContext currentContext);
}

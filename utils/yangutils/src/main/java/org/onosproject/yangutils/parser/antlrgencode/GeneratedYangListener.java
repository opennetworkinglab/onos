// Generated from GeneratedYang.g4 by ANTLR 4.5
/*
 * Copyright 2016 Open Networking Laboratory
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package org.onosproject.yangutils.parser.antlrgencode;

import org.antlr.v4.runtime.tree.ParseTreeListener;

/**
 * ANTLR interfaces to be implemented by listener to traverse the parse tree.
 */
public interface GeneratedYangListener extends ParseTreeListener {
    /**
     * Enter a parse tree produced by GeneratedYangParser for grammar rule
     * yangfile.
     *
     * @param currentContext current context in the parsed tree
     */
    void enterYangfile(GeneratedYangParser.YangfileContext currentContext);

    /**
     * Exit a parse tree produced by GeneratedYangParser for grammar rule
     * yangfile.
     *
     * @param currentContext current context in the parsed tree
     */
    void exitYangfile(GeneratedYangParser.YangfileContext currentContext);

    /**
     * Enter a parse tree produced by GeneratedYangParser for grammar rule
     * moduleStatement.
     *
     * @param currentContext current context in the parsed tree
     */
    void enterModuleStatement(GeneratedYangParser.ModuleStatementContext currentContext);

    /**
     * Exit a parse tree produced by GeneratedYangParser for grammar rule
     * moduleStatement.
     *
     * @param currentContext current context in the parsed tree
     */
    void exitModuleStatement(GeneratedYangParser.ModuleStatementContext currentContext);

    /**
     * Enter a parse tree produced by GeneratedYangParser for grammar rule
     * moduleBody.
     *
     * @param currentContext current context in the parsed tree
     */
    void enterModuleBody(GeneratedYangParser.ModuleBodyContext currentContext);

    /**
     * Exit a parse tree produced by GeneratedYangParser for grammar rule
     * moduleBody.
     *
     * @param currentContext current context in the parsed tree
     */
    void exitModuleBody(GeneratedYangParser.ModuleBodyContext currentContext);

    /**
     * Enter a parse tree produced by GeneratedYangParser for grammar rule
     * moduleHeaderStatement.
     *
     * @param currentContext current context in the parsed tree
     */
    void enterModuleHeaderStatement(GeneratedYangParser.ModuleHeaderStatementContext currentContext);

    /**
     * Exit a parse tree produced by GeneratedYangParser for grammar rule
     * moduleHeaderStatement.
     *
     * @param currentContext current context in the parsed tree
     */
    void exitModuleHeaderStatement(GeneratedYangParser.ModuleHeaderStatementContext currentContext);

    /**
     * Enter a parse tree produced by GeneratedYangParser for grammar rule
     * linkageStatements.
     *
     * @param currentContext current context in the parsed tree
     */
    void enterLinkageStatements(GeneratedYangParser.LinkageStatementsContext currentContext);

    /**
     * Exit a parse tree produced by GeneratedYangParser for grammar rule
     * linkageStatements.
     *
     * @param currentContext current context in the parsed tree
     */
    void exitLinkageStatements(GeneratedYangParser.LinkageStatementsContext currentContext);

    /**
     * Enter a parse tree produced by GeneratedYangParser for grammar rule
     * metaStatements.
     *
     * @param currentContext current context in the parsed tree
     */
    void enterMetaStatements(GeneratedYangParser.MetaStatementsContext currentContext);

    /**
     * Exit a parse tree produced by GeneratedYangParser for grammar rule
     * metaStatements.
     *
     * @param currentContext current context in the parsed tree
     */
    void exitMetaStatements(GeneratedYangParser.MetaStatementsContext currentContext);

    /**
     * Enter a parse tree produced by GeneratedYangParser for grammar rule
     * revisionStatements.
     *
     * @param currentContext current context in the parsed tree
     */
    void enterRevisionStatements(GeneratedYangParser.RevisionStatementsContext currentContext);

    /**
     * Exit a parse tree produced by GeneratedYangParser for grammar rule
     * revisionStatements.
     *
     * @param currentContext current context in the parsed tree
     */
    void exitRevisionStatements(GeneratedYangParser.RevisionStatementsContext currentContext);

    /**
     * Enter a parse tree produced by GeneratedYangParser for grammar rule
     * bodyStatements.
     *
     * @param currentContext current context in the parsed tree
     */
    void enterBodyStatements(GeneratedYangParser.BodyStatementsContext currentContext);

    /**
     * Exit a parse tree produced by GeneratedYangParser for grammar rule
     * bodyStatements.
     *
     * @param currentContext current context in the parsed tree
     */
    void exitBodyStatements(GeneratedYangParser.BodyStatementsContext currentContext);

    /**
     * Enter a parse tree produced by GeneratedYangParser for grammar rule
     * yangVersionStatement.
     *
     * @param currentContext current context in the parsed tree
     */
    void enterYangVersionStatement(GeneratedYangParser.YangVersionStatementContext currentContext);

    /**
     * Exit a parse tree produced by GeneratedYangParser for grammar rule
     * yangVersionStatement.
     *
     * @param currentContext current context in the parsed tree
     */
    void exitYangVersionStatement(GeneratedYangParser.YangVersionStatementContext currentContext);

    /**
     * Enter a parse tree produced by GeneratedYangParser for grammar rule
     * namespaceStatement.
     *
     * @param currentContext current context in the parsed tree
     */
    void enterNamespaceStatement(GeneratedYangParser.NamespaceStatementContext currentContext);

    /**
     * Exit a parse tree produced by GeneratedYangParser for grammar rule
     * namespaceStatement.
     *
     * @param currentContext current context in the parsed tree
     */
    void exitNamespaceStatement(GeneratedYangParser.NamespaceStatementContext currentContext);

    /**
     * Enter a parse tree produced by GeneratedYangParser for grammar rule
     * prefixStatement.
     *
     * @param currentContext current context in the parsed tree
     */
    void enterPrefixStatement(GeneratedYangParser.PrefixStatementContext currentContext);

    /**
     * Exit a parse tree produced by GeneratedYangParser for grammar rule
     * prefixStatement.
     *
     * @param currentContext current context in the parsed tree
     */
    void exitPrefixStatement(GeneratedYangParser.PrefixStatementContext currentContext);

    /**
     * Enter a parse tree produced by GeneratedYangParser for grammar rule
     * importStatement.
     *
     * @param currentContext current context in the parsed tree
     */
    void enterImportStatement(GeneratedYangParser.ImportStatementContext currentContext);

    /**
     * Exit a parse tree produced by GeneratedYangParser for grammar rule
     * importStatement.
     *
     * @param currentContext current context in the parsed tree
     */
    void exitImportStatement(GeneratedYangParser.ImportStatementContext currentContext);

    /**
     * Enter a parse tree produced by GeneratedYangParser for grammar rule
     * importStatementBody.
     *
     * @param currentContext current context in the parsed tree
     */
    void enterImportStatementBody(GeneratedYangParser.ImportStatementBodyContext currentContext);

    /**
     * Exit a parse tree produced by GeneratedYangParser for grammar rule
     * importStatementBody.
     *
     * @param currentContext current context in the parsed tree
     */
    void exitImportStatementBody(GeneratedYangParser.ImportStatementBodyContext currentContext);

    /**
     * Enter a parse tree produced by GeneratedYangParser for grammar rule
     * revisionDateStatement.
     *
     * @param currentContext current context in the parsed tree
     */
    void enterRevisionDateStatement(GeneratedYangParser.RevisionDateStatementContext currentContext);

    /**
     * Exit a parse tree produced by GeneratedYangParser for grammar rule
     * revisionDateStatement.
     *
     * @param currentContext current context in the parsed tree
     */
    void exitRevisionDateStatement(GeneratedYangParser.RevisionDateStatementContext currentContext);

    /**
     * Enter a parse tree produced by GeneratedYangParser for grammar rule
     * includeStatement.
     *
     * @param currentContext current context in the parsed tree
     */
    void enterIncludeStatement(GeneratedYangParser.IncludeStatementContext currentContext);

    /**
     * Exit a parse tree produced by GeneratedYangParser for grammar rule
     * includeStatement.
     *
     * @param currentContext current context in the parsed tree
     */
    void exitIncludeStatement(GeneratedYangParser.IncludeStatementContext currentContext);

    /**
     * Enter a parse tree produced by GeneratedYangParser for grammar rule
     * organizationStatement.
     *
     * @param currentContext current context in the parsed tree
     */
    void enterOrganizationStatement(GeneratedYangParser.OrganizationStatementContext currentContext);

    /**
     * Exit a parse tree produced by GeneratedYangParser for grammar rule
     * organizationStatement.
     *
     * @param currentContext current context in the parsed tree
     */
    void exitOrganizationStatement(GeneratedYangParser.OrganizationStatementContext currentContext);

    /**
     * Enter a parse tree produced by GeneratedYangParser for grammar rule
     * contactStatement.
     *
     * @param currentContext current context in the parsed tree
     */
    void enterContactStatement(GeneratedYangParser.ContactStatementContext currentContext);

    /**
     * Exit a parse tree produced by GeneratedYangParser for grammar rule
     * contactStatement.
     *
     * @param currentContext current context in the parsed tree
     */
    void exitContactStatement(GeneratedYangParser.ContactStatementContext currentContext);

    /**
     * Enter a parse tree produced by GeneratedYangParser for grammar rule
     * descriptionStatement.
     *
     * @param currentContext current context in the parsed tree
     */
    void enterDescriptionStatement(GeneratedYangParser.DescriptionStatementContext currentContext);

    /**
     * Exit a parse tree produced by GeneratedYangParser for grammar rule
     * descriptionStatement.
     *
     * @param currentContext current context in the parsed tree
     */
    void exitDescriptionStatement(GeneratedYangParser.DescriptionStatementContext currentContext);

    /**
     * Enter a parse tree produced by GeneratedYangParser for grammar rule
     * referenceStatement.
     *
     * @param currentContext current context in the parsed tree
     */
    void enterReferenceStatement(GeneratedYangParser.ReferenceStatementContext currentContext);

    /**
     * Exit a parse tree produced by GeneratedYangParser for grammar rule
     * referenceStatement.
     *
     * @param currentContext current context in the parsed tree
     */
    void exitReferenceStatement(GeneratedYangParser.ReferenceStatementContext currentContext);

    /**
     * Enter a parse tree produced by GeneratedYangParser for grammar rule
     * revisionStatement.
     *
     * @param currentContext current context in the parsed tree
     */
    void enterRevisionStatement(GeneratedYangParser.RevisionStatementContext currentContext);

    /**
     * Exit a parse tree produced by GeneratedYangParser for grammar rule
     * revisionStatement.
     *
     * @param currentContext current context in the parsed tree
     */
    void exitRevisionStatement(GeneratedYangParser.RevisionStatementContext currentContext);

    /**
     * Enter a parse tree produced by GeneratedYangParser for grammar rule
     * revisionStatementBody.
     *
     * @param currentContext current context in the parsed tree
     */
    void enterRevisionStatementBody(GeneratedYangParser.RevisionStatementBodyContext currentContext);

    /**
     * Exit a parse tree produced by GeneratedYangParser for grammar rule
     * revisionStatementBody.
     *
     * @param currentContext current context in the parsed tree
     */
    void exitRevisionStatementBody(GeneratedYangParser.RevisionStatementBodyContext currentContext);

    /**
     * Enter a parse tree produced by GeneratedYangParser for grammar rule
     * subModuleStatement.
     *
     * @param currentContext current context in the parsed tree
     */
    void enterSubModuleStatement(GeneratedYangParser.SubModuleStatementContext currentContext);

    /**
     * Exit a parse tree produced by GeneratedYangParser for grammar rule
     * subModuleStatement.
     *
     * @param currentContext current context in the parsed tree
     */
    void exitSubModuleStatement(GeneratedYangParser.SubModuleStatementContext currentContext);

    /**
     * Enter a parse tree produced by GeneratedYangParser for grammar rule
     * submoduleBody.
     *
     * @param currentContext current context in the parsed tree
     */
    void enterSubmoduleBody(GeneratedYangParser.SubmoduleBodyContext currentContext);

    /**
     * Exit a parse tree produced by GeneratedYangParser for grammar rule
     * submoduleBody.
     *
     * @param currentContext current context in the parsed tree
     */
    void exitSubmoduleBody(GeneratedYangParser.SubmoduleBodyContext currentContext);

    /**
     * Enter a parse tree produced by GeneratedYangParser for grammar rule
     * submoduleHeaderStatement.
     *
     * @param currentContext current context in the parsed tree
     */
    void enterSubmoduleHeaderStatement(GeneratedYangParser.SubmoduleHeaderStatementContext currentContext);

    /**
     * Exit a parse tree produced by GeneratedYangParser for grammar rule
     * submoduleHeaderStatement.
     *
     * @param currentContext current context in the parsed tree
     */
    void exitSubmoduleHeaderStatement(GeneratedYangParser.SubmoduleHeaderStatementContext currentContext);

    /**
     * Enter a parse tree produced by GeneratedYangParser for grammar rule
     * belongstoStatement.
     *
     * @param currentContext current context in the parsed tree
     */
    void enterBelongstoStatement(GeneratedYangParser.BelongstoStatementContext currentContext);

    /**
     * Exit a parse tree produced by GeneratedYangParser for grammar rule
     * belongstoStatement.
     *
     * @param currentContext current context in the parsed tree
     */
    void exitBelongstoStatement(GeneratedYangParser.BelongstoStatementContext currentContext);

    /**
     * Enter a parse tree produced by GeneratedYangParser for grammar rule
     * belongstoStatementBody.
     *
     * @param currentContext current context in the parsed tree
     */
    void enterBelongstoStatementBody(GeneratedYangParser.BelongstoStatementBodyContext currentContext);

    /**
     * Exit a parse tree produced by GeneratedYangParser for grammar rule
     * belongstoStatementBody.
     *
     * @param currentContext current context in the parsed tree
     */
    void exitBelongstoStatementBody(GeneratedYangParser.BelongstoStatementBodyContext currentContext);

    /**
     * Enter a parse tree produced by GeneratedYangParser for grammar rule
     * extensionStatement.
     *
     * @param currentContext current context in the parsed tree
     */
    void enterExtensionStatement(GeneratedYangParser.ExtensionStatementContext currentContext);

    /**
     * Exit a parse tree produced by GeneratedYangParser for grammar rule
     * extensionStatement.
     *
     * @param currentContext current context in the parsed tree
     */
    void exitExtensionStatement(GeneratedYangParser.ExtensionStatementContext currentContext);

    /**
     * Enter a parse tree produced by GeneratedYangParser for grammar rule
     * extensionBody.
     *
     * @param currentContext current context in the parsed tree
     */
    void enterExtensionBody(GeneratedYangParser.ExtensionBodyContext currentContext);

    /**
     * Exit a parse tree produced by GeneratedYangParser for grammar rule
     * extensionBody.
     *
     * @param currentContext current context in the parsed tree
     */
    void exitExtensionBody(GeneratedYangParser.ExtensionBodyContext currentContext);

    /**
     * Enter a parse tree produced by GeneratedYangParser for grammar rule
     * argumentStatement.
     *
     * @param currentContext current context in the parsed tree
     */
    void enterArgumentStatement(GeneratedYangParser.ArgumentStatementContext currentContext);

    /**
     * Exit a parse tree produced by GeneratedYangParser for grammar rule
     * argumentStatement.
     *
     * @param currentContext current context in the parsed tree
     */
    void exitArgumentStatement(GeneratedYangParser.ArgumentStatementContext currentContext);

    /**
     * Enter a parse tree produced by GeneratedYangParser for grammar rule
     * argumentBody.
     *
     * @param currentContext current context in the parsed tree
     */
    void enterArgumentBody(GeneratedYangParser.ArgumentBodyContext currentContext);

    /**
     * Exit a parse tree produced by GeneratedYangParser for grammar rule
     * argumentBody.
     *
     * @param currentContext current context in the parsed tree
     */
    void exitArgumentBody(GeneratedYangParser.ArgumentBodyContext currentContext);

    /**
     * Enter a parse tree produced by GeneratedYangParser for grammar rule
     * yinElementStatement.
     *
     * @param currentContext current context in the parsed tree
     */
    void enterYinElementStatement(GeneratedYangParser.YinElementStatementContext currentContext);

    /**
     * Exit a parse tree produced by GeneratedYangParser for grammar rule
     * yinElementStatement.
     *
     * @param currentContext current context in the parsed tree
     */
    void exitYinElementStatement(GeneratedYangParser.YinElementStatementContext currentContext);

    /**
     * Enter a parse tree produced by GeneratedYangParser for grammar rule
     * identityStatement.
     *
     * @param currentContext current context in the parsed tree
     */
    void enterIdentityStatement(GeneratedYangParser.IdentityStatementContext currentContext);

    /**
     * Exit a parse tree produced by GeneratedYangParser for grammar rule
     * identityStatement.
     *
     * @param currentContext current context in the parsed tree
     */
    void exitIdentityStatement(GeneratedYangParser.IdentityStatementContext currentContext);

    /**
     * Enter a parse tree produced by GeneratedYangParser for grammar rule
     * identityBody.
     *
     * @param currentContext current context in the parsed tree
     */
    void enterIdentityBody(GeneratedYangParser.IdentityBodyContext currentContext);

    /**
     * Exit a parse tree produced by GeneratedYangParser for grammar rule
     * identityBody.
     *
     * @param currentContext current context in the parsed tree
     */
    void exitIdentityBody(GeneratedYangParser.IdentityBodyContext currentContext);

    /**
     * Enter a parse tree produced by GeneratedYangParser for grammar rule
     * baseStatement.
     *
     * @param currentContext current context in the parsed tree
     */
    void enterBaseStatement(GeneratedYangParser.BaseStatementContext currentContext);

    /**
     * Exit a parse tree produced by GeneratedYangParser for grammar rule
     * baseStatement.
     *
     * @param currentContext current context in the parsed tree
     */
    void exitBaseStatement(GeneratedYangParser.BaseStatementContext currentContext);

    /**
     * Enter a parse tree produced by GeneratedYangParser for grammar rule
     * featureStatement.
     *
     * @param currentContext current context in the parsed tree
     */
    void enterFeatureStatement(GeneratedYangParser.FeatureStatementContext currentContext);

    /**
     * Exit a parse tree produced by GeneratedYangParser for grammar rule
     * featureStatement.
     *
     * @param currentContext current context in the parsed tree
     */
    void exitFeatureStatement(GeneratedYangParser.FeatureStatementContext currentContext);

    /**
     * Enter a parse tree produced by GeneratedYangParser for grammar rule
     * featureBody.
     *
     * @param currentContext current context in the parsed tree
     */
    void enterFeatureBody(GeneratedYangParser.FeatureBodyContext currentContext);

    /**
     * Exit a parse tree produced by GeneratedYangParser for grammar rule
     * featureBody.
     *
     * @param currentContext current context in the parsed tree
     */
    void exitFeatureBody(GeneratedYangParser.FeatureBodyContext currentContext);

    /**
     * Enter a parse tree produced by GeneratedYangParser for grammar rule
     * dataDefStatement.
     *
     * @param currentContext current context in the parsed tree
     */
    void enterDataDefStatement(GeneratedYangParser.DataDefStatementContext currentContext);

    /**
     * Exit a parse tree produced by GeneratedYangParser for grammar rule
     * dataDefStatement.
     *
     * @param currentContext current context in the parsed tree
     */
    void exitDataDefStatement(GeneratedYangParser.DataDefStatementContext currentContext);

    /**
     * Enter a parse tree produced by GeneratedYangParser for grammar rule
     * ifFeatureStatement.
     *
     * @param currentContext current context in the parsed tree
     */
    void enterIfFeatureStatement(GeneratedYangParser.IfFeatureStatementContext currentContext);

    /**
     * Exit a parse tree produced by GeneratedYangParser for grammar rule
     * ifFeatureStatement.
     *
     * @param currentContext current context in the parsed tree
     */
    void exitIfFeatureStatement(GeneratedYangParser.IfFeatureStatementContext currentContext);

    /**
     * Enter a parse tree produced by GeneratedYangParser for grammar rule
     * unitsStatement.
     *
     * @param currentContext current context in the parsed tree
     */
    void enterUnitsStatement(GeneratedYangParser.UnitsStatementContext currentContext);

    /**
     * Exit a parse tree produced by GeneratedYangParser for grammar rule
     * unitsStatement.
     *
     * @param currentContext current context in the parsed tree
     */
    void exitUnitsStatement(GeneratedYangParser.UnitsStatementContext currentContext);

    /**
     * Enter a parse tree produced by GeneratedYangParser for grammar rule
     * typedefStatement.
     *
     * @param currentContext current context in the parsed tree
     */
    void enterTypedefStatement(GeneratedYangParser.TypedefStatementContext currentContext);

    /**
     * Exit a parse tree produced by GeneratedYangParser for grammar rule
     * typedefStatement.
     *
     * @param currentContext current context in the parsed tree
     */
    void exitTypedefStatement(GeneratedYangParser.TypedefStatementContext currentContext);

    /**
     * Enter a parse tree produced by GeneratedYangParser for grammar rule
     * typeStatement.
     *
     * @param currentContext current context in the parsed tree
     */
    void enterTypeStatement(GeneratedYangParser.TypeStatementContext currentContext);

    /**
     * Exit a parse tree produced by GeneratedYangParser for grammar rule
     * typeStatement.
     *
     * @param currentContext current context in the parsed tree
     */
    void exitTypeStatement(GeneratedYangParser.TypeStatementContext currentContext);

    /**
     * Enter a parse tree produced by GeneratedYangParser for grammar rule
     * typeBodyStatements.
     *
     * @param currentContext current context in the parsed tree
     */
    void enterTypeBodyStatements(GeneratedYangParser.TypeBodyStatementsContext currentContext);

    /**
     * Exit a parse tree produced by GeneratedYangParser for grammar rule
     * typeBodyStatements.
     *
     * @param currentContext current context in the parsed tree
     */
    void exitTypeBodyStatements(GeneratedYangParser.TypeBodyStatementsContext currentContext);

    /**
     * Enter a parse tree produced by GeneratedYangParser for grammar rule
     * numericalRestrictions.
     *
     * @param currentContext current context in the parsed tree
     */
    void enterNumericalRestrictions(GeneratedYangParser.NumericalRestrictionsContext currentContext);

    /**
     * Exit a parse tree produced by GeneratedYangParser for grammar rule
     * numericalRestrictions.
     *
     * @param currentContext current context in the parsed tree
     */
    void exitNumericalRestrictions(GeneratedYangParser.NumericalRestrictionsContext currentContext);

    /**
     * Enter a parse tree produced by GeneratedYangParser for grammar rule
     * rangeStatement.
     *
     * @param currentContext current context in the parsed tree
     */
    void enterRangeStatement(GeneratedYangParser.RangeStatementContext currentContext);

    /**
     * Exit a parse tree produced by GeneratedYangParser for grammar rule
     * rangeStatement.
     *
     * @param currentContext current context in the parsed tree
     */
    void exitRangeStatement(GeneratedYangParser.RangeStatementContext currentContext);

    /**
     * Enter a parse tree produced by GeneratedYangParser for grammar rule
     * commonStatements.
     *
     * @param currentContext current context in the parsed tree
     */
    void enterCommonStatements(GeneratedYangParser.CommonStatementsContext currentContext);

    /**
     * Exit a parse tree produced by GeneratedYangParser for grammar rule
     * commonStatements.
     *
     * @param currentContext current context in the parsed tree
     */
    void exitCommonStatements(GeneratedYangParser.CommonStatementsContext currentContext);

    /**
     * Enter a parse tree produced by GeneratedYangParser for grammar rule
     * stringRestrictions.
     *
     * @param currentContext current context in the parsed tree
     */
    void enterStringRestrictions(GeneratedYangParser.StringRestrictionsContext currentContext);

    /**
     * Exit a parse tree produced by GeneratedYangParser for grammar rule
     * stringRestrictions.
     *
     * @param currentContext current context in the parsed tree
     */
    void exitStringRestrictions(GeneratedYangParser.StringRestrictionsContext currentContext);

    /**
     * Enter a parse tree produced by GeneratedYangParser for grammar rule
     * lengthStatement.
     *
     * @param currentContext current context in the parsed tree
     */
    void enterLengthStatement(GeneratedYangParser.LengthStatementContext currentContext);

    /**
     * Exit a parse tree produced by GeneratedYangParser for grammar rule
     * lengthStatement.
     *
     * @param currentContext current context in the parsed tree
     */
    void exitLengthStatement(GeneratedYangParser.LengthStatementContext currentContext);

    /**
     * Enter a parse tree produced by GeneratedYangParser for grammar rule
     * patternStatement.
     *
     * @param currentContext current context in the parsed tree
     */
    void enterPatternStatement(GeneratedYangParser.PatternStatementContext currentContext);

    /**
     * Exit a parse tree produced by GeneratedYangParser for grammar rule
     * patternStatement.
     *
     * @param currentContext current context in the parsed tree
     */
    void exitPatternStatement(GeneratedYangParser.PatternStatementContext currentContext);

    /**
     * Enter a parse tree produced by GeneratedYangParser for grammar rule
     * defaultStatement.
     *
     * @param currentContext current context in the parsed tree
     */
    void enterDefaultStatement(GeneratedYangParser.DefaultStatementContext currentContext);

    /**
     * Exit a parse tree produced by GeneratedYangParser for grammar rule
     * defaultStatement.
     *
     * @param currentContext current context in the parsed tree
     */
    void exitDefaultStatement(GeneratedYangParser.DefaultStatementContext currentContext);

    /**
     * Enter a parse tree produced by GeneratedYangParser for grammar rule
     * enumSpecification.
     *
     * @param currentContext current context in the parsed tree
     */
    void enterEnumSpecification(GeneratedYangParser.EnumSpecificationContext currentContext);

    /**
     * Exit a parse tree produced by GeneratedYangParser for grammar rule
     * enumSpecification.
     *
     * @param currentContext current context in the parsed tree
     */
    void exitEnumSpecification(GeneratedYangParser.EnumSpecificationContext currentContext);

    /**
     * Enter a parse tree produced by GeneratedYangParser for grammar rule
     * enumStatement.
     *
     * @param currentContext current context in the parsed tree
     */
    void enterEnumStatement(GeneratedYangParser.EnumStatementContext currentContext);

    /**
     * Exit a parse tree produced by GeneratedYangParser for grammar rule
     * enumStatement.
     *
     * @param currentContext current context in the parsed tree
     */
    void exitEnumStatement(GeneratedYangParser.EnumStatementContext currentContext);

    /**
     * Enter a parse tree produced by GeneratedYangParser for grammar rule
     * enumStatementBody.
     *
     * @param currentContext current context in the parsed tree
     */
    void enterEnumStatementBody(GeneratedYangParser.EnumStatementBodyContext currentContext);

    /**
     * Exit a parse tree produced by GeneratedYangParser for grammar rule
     * enumStatementBody.
     *
     * @param currentContext current context in the parsed tree
     */
    void exitEnumStatementBody(GeneratedYangParser.EnumStatementBodyContext currentContext);

    /**
     * Enter a parse tree produced by GeneratedYangParser for grammar rule
     * leafrefSpecification.
     *
     * @param currentContext current context in the parsed tree
     */
    void enterLeafrefSpecification(GeneratedYangParser.LeafrefSpecificationContext currentContext);

    /**
     * Exit a parse tree produced by GeneratedYangParser for grammar rule
     * leafrefSpecification.
     *
     * @param currentContext current context in the parsed tree
     */
    void exitLeafrefSpecification(GeneratedYangParser.LeafrefSpecificationContext currentContext);

    /**
     * Enter a parse tree produced by GeneratedYangParser for grammar rule
     * pathStatement.
     *
     * @param currentContext current context in the parsed tree
     */
    void enterPathStatement(GeneratedYangParser.PathStatementContext currentContext);

    /**
     * Exit a parse tree produced by GeneratedYangParser for grammar rule
     * pathStatement.
     *
     * @param currentContext current context in the parsed tree
     */
    void exitPathStatement(GeneratedYangParser.PathStatementContext currentContext);

    /**
     * Enter a parse tree produced by GeneratedYangParser for grammar rule
     * requireInstanceStatement.
     *
     * @param currentContext current context in the parsed tree
     */
    void enterRequireInstanceStatement(GeneratedYangParser.RequireInstanceStatementContext currentContext);

    /**
     * Exit a parse tree produced by GeneratedYangParser for grammar rule
     * requireInstanceStatement.
     *
     * @param currentContext current context in the parsed tree
     */
    void exitRequireInstanceStatement(GeneratedYangParser.RequireInstanceStatementContext currentContext);

    /**
     * Enter a parse tree produced by GeneratedYangParser for grammar rule
     * instanceIdentifierSpecification.
     *
     * @param currentContext current context in the parsed tree
     */
    void enterInstanceIdentifierSpecification(
            GeneratedYangParser.InstanceIdentifierSpecificationContext currentContext);

    /**
     * Exit a parse tree produced by GeneratedYangParser for grammar rule
     * instanceIdentifierSpecification.
     *
     * @param currentContext current context in the parsed tree
     */
    void exitInstanceIdentifierSpecification(GeneratedYangParser.InstanceIdentifierSpecificationContext currentContext);

    /**
     * Enter a parse tree produced by GeneratedYangParser for grammar rule
     * identityrefSpecification.
     *
     * @param currentContext current context in the parsed tree
     */
    void enterIdentityrefSpecification(GeneratedYangParser.IdentityrefSpecificationContext currentContext);

    /**
     * Exit a parse tree produced by GeneratedYangParser for grammar rule
     * identityrefSpecification.
     *
     * @param currentContext current context in the parsed tree
     */
    void exitIdentityrefSpecification(GeneratedYangParser.IdentityrefSpecificationContext currentContext);

    /**
     * Enter a parse tree produced by GeneratedYangParser for grammar rule
     * unionSpecification.
     *
     * @param currentContext current context in the parsed tree
     */
    void enterUnionSpecification(GeneratedYangParser.UnionSpecificationContext currentContext);

    /**
     * Exit a parse tree produced by GeneratedYangParser for grammar rule
     * unionSpecification.
     *
     * @param currentContext current context in the parsed tree
     */
    void exitUnionSpecification(GeneratedYangParser.UnionSpecificationContext currentContext);

    /**
     * Enter a parse tree produced by GeneratedYangParser for grammar rule
     * bitsSpecification.
     *
     * @param currentContext current context in the parsed tree
     */
    void enterBitsSpecification(GeneratedYangParser.BitsSpecificationContext currentContext);

    /**
     * Exit a parse tree produced by GeneratedYangParser for grammar rule
     * bitsSpecification.
     *
     * @param currentContext current context in the parsed tree
     */
    void exitBitsSpecification(GeneratedYangParser.BitsSpecificationContext currentContext);

    /**
     * Enter a parse tree produced by GeneratedYangParser for grammar rule
     * bitStatement.
     *
     * @param currentContext current context in the parsed tree
     */
    void enterBitStatement(GeneratedYangParser.BitStatementContext currentContext);

    /**
     * Exit a parse tree produced by GeneratedYangParser for grammar rule
     * bitStatement.
     *
     * @param currentContext current context in the parsed tree
     */
    void exitBitStatement(GeneratedYangParser.BitStatementContext currentContext);

    /**
     * Enter a parse tree produced by GeneratedYangParser for grammar rule
     * bitBodyStatement.
     *
     * @param currentContext current context in the parsed tree
     */
    void enterBitBodyStatement(GeneratedYangParser.BitBodyStatementContext currentContext);

    /**
     * Exit a parse tree produced by GeneratedYangParser for grammar rule
     * bitBodyStatement.
     *
     * @param currentContext current context in the parsed tree
     */
    void exitBitBodyStatement(GeneratedYangParser.BitBodyStatementContext currentContext);

    /**
     * Enter a parse tree produced by GeneratedYangParser for grammar rule
     * positionStatement.
     *
     * @param currentContext current context in the parsed tree
     */
    void enterPositionStatement(GeneratedYangParser.PositionStatementContext currentContext);

    /**
     * Exit a parse tree produced by GeneratedYangParser for grammar rule
     * positionStatement.
     *
     * @param currentContext current context in the parsed tree
     */
    void exitPositionStatement(GeneratedYangParser.PositionStatementContext currentContext);

    /**
     * Enter a parse tree produced by GeneratedYangParser for grammar rule
     * statusStatement.
     *
     * @param currentContext current context in the parsed tree
     */
    void enterStatusStatement(GeneratedYangParser.StatusStatementContext currentContext);

    /**
     * Exit a parse tree produced by GeneratedYangParser for grammar rule
     * statusStatement.
     *
     * @param currentContext current context in the parsed tree
     */
    void exitStatusStatement(GeneratedYangParser.StatusStatementContext currentContext);

    /**
     * Enter a parse tree produced by GeneratedYangParser for grammar rule
     * configStatement.
     *
     * @param currentContext current context in the parsed tree
     */
    void enterConfigStatement(GeneratedYangParser.ConfigStatementContext currentContext);

    /**
     * Exit a parse tree produced by GeneratedYangParser for grammar rule
     * configStatement.
     *
     * @param currentContext current context in the parsed tree
     */
    void exitConfigStatement(GeneratedYangParser.ConfigStatementContext currentContext);

    /**
     * Enter a parse tree produced by GeneratedYangParser for grammar rule
     * mandatoryStatement.
     *
     * @param currentContext current context in the parsed tree
     */
    void enterMandatoryStatement(GeneratedYangParser.MandatoryStatementContext currentContext);

    /**
     * Exit a parse tree produced by GeneratedYangParser for grammar rule
     * mandatoryStatement.
     *
     * @param currentContext current context in the parsed tree
     */
    void exitMandatoryStatement(GeneratedYangParser.MandatoryStatementContext currentContext);

    /**
     * Enter a parse tree produced by GeneratedYangParser for grammar rule
     * presenceStatement.
     *
     * @param currentContext current context in the parsed tree
     */
    void enterPresenceStatement(GeneratedYangParser.PresenceStatementContext currentContext);

    /**
     * Exit a parse tree produced by GeneratedYangParser for grammar rule
     * presenceStatement.
     *
     * @param currentContext current context in the parsed tree
     */
    void exitPresenceStatement(GeneratedYangParser.PresenceStatementContext currentContext);

    /**
     * Enter a parse tree produced by GeneratedYangParser for grammar rule
     * orderedByStatement.
     *
     * @param currentContext current context in the parsed tree
     */
    void enterOrderedByStatement(GeneratedYangParser.OrderedByStatementContext currentContext);

    /**
     * Exit a parse tree produced by GeneratedYangParser for grammar rule
     * orderedByStatement.
     *
     * @param currentContext current context in the parsed tree
     */
    void exitOrderedByStatement(GeneratedYangParser.OrderedByStatementContext currentContext);

    /**
     * Enter a parse tree produced by GeneratedYangParser for grammar rule
     * mustStatement.
     *
     * @param currentContext current context in the parsed tree
     */
    void enterMustStatement(GeneratedYangParser.MustStatementContext currentContext);

    /**
     * Exit a parse tree produced by GeneratedYangParser for grammar rule
     * mustStatement.
     *
     * @param currentContext current context in the parsed tree
     */
    void exitMustStatement(GeneratedYangParser.MustStatementContext currentContext);

    /**
     * Enter a parse tree produced by GeneratedYangParser for grammar rule
     * errorMessageStatement.
     *
     * @param currentContext current context in the parsed tree
     */
    void enterErrorMessageStatement(GeneratedYangParser.ErrorMessageStatementContext currentContext);

    /**
     * Exit a parse tree produced by GeneratedYangParser for grammar rule
     * errorMessageStatement.
     *
     * @param currentContext current context in the parsed tree
     */
    void exitErrorMessageStatement(GeneratedYangParser.ErrorMessageStatementContext currentContext);

    /**
     * Enter a parse tree produced by GeneratedYangParser for grammar rule
     * errorAppTagStatement.
     *
     * @param currentContext current context in the parsed tree
     */
    void enterErrorAppTagStatement(GeneratedYangParser.ErrorAppTagStatementContext currentContext);

    /**
     * Exit a parse tree produced by GeneratedYangParser for grammar rule
     * errorAppTagStatement.
     *
     * @param currentContext current context in the parsed tree
     */
    void exitErrorAppTagStatement(GeneratedYangParser.ErrorAppTagStatementContext currentContext);

    /**
     * Enter a parse tree produced by GeneratedYangParser for grammar rule
     * minElementsStatement.
     *
     * @param currentContext current context in the parsed tree
     */
    void enterMinElementsStatement(GeneratedYangParser.MinElementsStatementContext currentContext);

    /**
     * Exit a parse tree produced by GeneratedYangParser for grammar rule
     * minElementsStatement.
     *
     * @param currentContext current context in the parsed tree
     */
    void exitMinElementsStatement(GeneratedYangParser.MinElementsStatementContext currentContext);

    /**
     * Enter a parse tree produced by GeneratedYangParser for grammar rule
     * maxElementsStatement.
     *
     * @param currentContext current context in the parsed tree
     */
    void enterMaxElementsStatement(GeneratedYangParser.MaxElementsStatementContext currentContext);

    /**
     * Exit a parse tree produced by GeneratedYangParser for grammar rule
     * maxElementsStatement.
     *
     * @param currentContext current context in the parsed tree
     */
    void exitMaxElementsStatement(GeneratedYangParser.MaxElementsStatementContext currentContext);

    /**
     * Enter a parse tree produced by GeneratedYangParser for grammar rule
     * maxValueArgument.
     *
     * @param currentContext current context in the parsed tree
     */
    void enterMaxValueArgument(GeneratedYangParser.MaxValueArgumentContext currentContext);

    /**
     * Exit a parse tree produced by GeneratedYangParser for grammar rule
     * maxValueArgument.
     *
     * @param currentContext current context in the parsed tree
     */
    void exitMaxValueArgument(GeneratedYangParser.MaxValueArgumentContext currentContext);

    /**
     * Enter a parse tree produced by GeneratedYangParser for grammar rule
     * valueStatement.
     *
     * @param currentContext current context in the parsed tree
     */
    void enterValueStatement(GeneratedYangParser.ValueStatementContext currentContext);

    /**
     * Exit a parse tree produced by GeneratedYangParser for grammar rule
     * valueStatement.
     *
     * @param currentContext current context in the parsed tree
     */
    void exitValueStatement(GeneratedYangParser.ValueStatementContext currentContext);

    /**
     * Enter a parse tree produced by GeneratedYangParser for grammar rule
     * groupingStatement.
     *
     * @param currentContext current context in the parsed tree
     */
    void enterGroupingStatement(GeneratedYangParser.GroupingStatementContext currentContext);

    /**
     * Exit a parse tree produced by GeneratedYangParser for grammar rule
     * groupingStatement.
     *
     * @param currentContext current context in the parsed tree
     */
    void exitGroupingStatement(GeneratedYangParser.GroupingStatementContext currentContext);

    /**
     * Enter a parse tree produced by GeneratedYangParser for grammar rule
     * containerStatement.
     *
     * @param currentContext current context in the parsed tree
     */
    void enterContainerStatement(GeneratedYangParser.ContainerStatementContext currentContext);

    /**
     * Exit a parse tree produced by GeneratedYangParser for grammar rule
     * containerStatement.
     *
     * @param currentContext current context in the parsed tree
     */
    void exitContainerStatement(GeneratedYangParser.ContainerStatementContext currentContext);

    /**
     * Enter a parse tree produced by GeneratedYangParser for grammar rule
     * leafStatement.
     *
     * @param currentContext current context in the parsed tree
     */
    void enterLeafStatement(GeneratedYangParser.LeafStatementContext currentContext);

    /**
     * Exit a parse tree produced by GeneratedYangParser for grammar rule
     * leafStatement.
     *
     * @param currentContext current context in the parsed tree
     */
    void exitLeafStatement(GeneratedYangParser.LeafStatementContext currentContext);

    /**
     * Enter a parse tree produced by GeneratedYangParser for grammar rule
     * leafListStatement.
     *
     * @param currentContext current context in the parsed tree
     */
    void enterLeafListStatement(GeneratedYangParser.LeafListStatementContext currentContext);

    /**
     * Exit a parse tree produced by GeneratedYangParser for grammar rule
     * leafListStatement.
     *
     * @param currentContext current context in the parsed tree
     */
    void exitLeafListStatement(GeneratedYangParser.LeafListStatementContext currentContext);

    /**
     * Enter a parse tree produced by GeneratedYangParser for grammar rule
     * listStatement.
     *
     * @param currentContext current context in the parsed tree
     */
    void enterListStatement(GeneratedYangParser.ListStatementContext currentContext);

    /**
     * Exit a parse tree produced by GeneratedYangParser for grammar rule
     * listStatement.
     *
     * @param currentContext current context in the parsed tree
     */
    void exitListStatement(GeneratedYangParser.ListStatementContext currentContext);

    /**
     * Enter a parse tree produced by GeneratedYangParser for grammar rule
     * keyStatement.
     *
     * @param currentContext current context in the parsed tree
     */
    void enterKeyStatement(GeneratedYangParser.KeyStatementContext currentContext);

    /**
     * Exit a parse tree produced by GeneratedYangParser for grammar rule
     * keyStatement.
     *
     * @param currentContext current context in the parsed tree
     */
    void exitKeyStatement(GeneratedYangParser.KeyStatementContext currentContext);

    /**
     * Enter a parse tree produced by GeneratedYangParser for grammar rule
     * uniqueStatement.
     *
     * @param currentContext current context in the parsed tree
     */
    void enterUniqueStatement(GeneratedYangParser.UniqueStatementContext currentContext);

    /**
     * Exit a parse tree produced by GeneratedYangParser for grammar rule
     * uniqueStatement.
     *
     * @param currentContext current context in the parsed tree
     */
    void exitUniqueStatement(GeneratedYangParser.UniqueStatementContext currentContext);

    /**
     * Enter a parse tree produced by GeneratedYangParser for grammar rule
     * choiceStatement.
     *
     * @param currentContext current context in the parsed tree
     */
    void enterChoiceStatement(GeneratedYangParser.ChoiceStatementContext currentContext);

    /**
     * Exit a parse tree produced by GeneratedYangParser for grammar rule
     * choiceStatement.
     *
     * @param currentContext current context in the parsed tree
     */
    void exitChoiceStatement(GeneratedYangParser.ChoiceStatementContext currentContext);

    /**
     * Enter a parse tree produced by GeneratedYangParser for grammar rule
     * shortCaseStatement.
     *
     * @param currentContext current context in the parsed tree
     */
    void enterShortCaseStatement(GeneratedYangParser.ShortCaseStatementContext currentContext);

    /**
     * Exit a parse tree produced by GeneratedYangParser for grammar rule
     * shortCaseStatement.
     *
     * @param currentContext current context in the parsed tree
     */
    void exitShortCaseStatement(GeneratedYangParser.ShortCaseStatementContext currentContext);

    /**
     * Enter a parse tree produced by GeneratedYangParser for grammar rule
     * caseStatement.
     *
     * @param currentContext current context in the parsed tree
     */
    void enterCaseStatement(GeneratedYangParser.CaseStatementContext currentContext);

    /**
     * Exit a parse tree produced by GeneratedYangParser for grammar rule
     * caseStatement.
     *
     * @param currentContext current context in the parsed tree
     */
    void exitCaseStatement(GeneratedYangParser.CaseStatementContext currentContext);

    /**
     * Enter a parse tree produced by GeneratedYangParser for grammar rule
     * usesStatement.
     *
     * @param currentContext current context in the parsed tree
     */
    void enterUsesStatement(GeneratedYangParser.UsesStatementContext currentContext);

    /**
     * Exit a parse tree produced by GeneratedYangParser for grammar rule
     * usesStatement.
     *
     * @param currentContext current context in the parsed tree
     */
    void exitUsesStatement(GeneratedYangParser.UsesStatementContext currentContext);

    /**
     * Enter a parse tree produced by GeneratedYangParser for grammar rule
     * refineStatement.
     *
     * @param currentContext current context in the parsed tree
     */
    void enterRefineStatement(GeneratedYangParser.RefineStatementContext currentContext);

    /**
     * Exit a parse tree produced by GeneratedYangParser for grammar rule
     * refineStatement.
     *
     * @param currentContext current context in the parsed tree
     */
    void exitRefineStatement(GeneratedYangParser.RefineStatementContext currentContext);

    /**
     * Enter a parse tree produced by GeneratedYangParser for grammar rule
     * refineContainerStatements.
     *
     * @param currentContext current context in the parsed tree
     */
    void enterRefineContainerStatements(GeneratedYangParser.RefineContainerStatementsContext currentContext);

    /**
     * Exit a parse tree produced by GeneratedYangParser for grammar rule
     * refineContainerStatements.
     *
     * @param currentContext current context in the parsed tree
     */
    void exitRefineContainerStatements(GeneratedYangParser.RefineContainerStatementsContext currentContext);

    /**
     * Enter a parse tree produced by GeneratedYangParser for grammar rule
     * refineLeafStatements.
     *
     * @param currentContext current context in the parsed tree
     */
    void enterRefineLeafStatements(GeneratedYangParser.RefineLeafStatementsContext currentContext);

    /**
     * Exit a parse tree produced by GeneratedYangParser for grammar rule
     * refineLeafStatements.
     *
     * @param currentContext current context in the parsed tree
     */
    void exitRefineLeafStatements(GeneratedYangParser.RefineLeafStatementsContext currentContext);

    /**
     * Enter a parse tree produced by GeneratedYangParser for grammar rule
     * refineLeafListStatements.
     *
     * @param currentContext current context in the parsed tree
     */
    void enterRefineLeafListStatements(GeneratedYangParser.RefineLeafListStatementsContext currentContext);

    /**
     * Exit a parse tree produced by GeneratedYangParser for grammar rule
     * refineLeafListStatements.
     *
     * @param currentContext current context in the parsed tree
     */
    void exitRefineLeafListStatements(GeneratedYangParser.RefineLeafListStatementsContext currentContext);

    /**
     * Enter a parse tree produced by GeneratedYangParser for grammar rule
     * refineListStatements.
     *
     * @param currentContext current context in the parsed tree
     */
    void enterRefineListStatements(GeneratedYangParser.RefineListStatementsContext currentContext);

    /**
     * Exit a parse tree produced by GeneratedYangParser for grammar rule
     * refineListStatements.
     *
     * @param currentContext current context in the parsed tree
     */
    void exitRefineListStatements(GeneratedYangParser.RefineListStatementsContext currentContext);

    /**
     * Enter a parse tree produced by GeneratedYangParser for grammar rule
     * refineChoiceStatements.
     *
     * @param currentContext current context in the parsed tree
     */
    void enterRefineChoiceStatements(GeneratedYangParser.RefineChoiceStatementsContext currentContext);

    /**
     * Exit a parse tree produced by GeneratedYangParser for grammar rule
     * refineChoiceStatements.
     *
     * @param currentContext current context in the parsed tree
     */
    void exitRefineChoiceStatements(GeneratedYangParser.RefineChoiceStatementsContext currentContext);

    /**
     * Enter a parse tree produced by GeneratedYangParser for grammar rule
     * refineCaseStatements.
     *
     * @param currentContext current context in the parsed tree
     */
    void enterRefineCaseStatements(GeneratedYangParser.RefineCaseStatementsContext currentContext);

    /**
     * Exit a parse tree produced by GeneratedYangParser for grammar rule
     * refineCaseStatements.
     *
     * @param currentContext current context in the parsed tree
     */
    void exitRefineCaseStatements(GeneratedYangParser.RefineCaseStatementsContext currentContext);

    /**
     * Enter a parse tree produced by GeneratedYangParser for grammar rule
     * usesAugmentStatement.
     *
     * @param currentContext current context in the parsed tree
     */
    void enterUsesAugmentStatement(GeneratedYangParser.UsesAugmentStatementContext currentContext);

    /**
     * Exit a parse tree produced by GeneratedYangParser for grammar rule
     * usesAugmentStatement.
     *
     * @param currentContext current context in the parsed tree
     */
    void exitUsesAugmentStatement(GeneratedYangParser.UsesAugmentStatementContext currentContext);

    /**
     * Enter a parse tree produced by GeneratedYangParser for grammar rule
     * augmentStatement.
     *
     * @param currentContext current context in the parsed tree
     */
    void enterAugmentStatement(GeneratedYangParser.AugmentStatementContext currentContext);

    /**
     * Exit a parse tree produced by GeneratedYangParser for grammar rule
     * augmentStatement.
     *
     * @param currentContext current context in the parsed tree
     */
    void exitAugmentStatement(GeneratedYangParser.AugmentStatementContext currentContext);

    /**
     * Enter a parse tree produced by GeneratedYangParser for grammar rule
     * whenStatement.
     *
     * @param currentContext current context in the parsed tree
     */
    void enterWhenStatement(GeneratedYangParser.WhenStatementContext currentContext);

    /**
     * Exit a parse tree produced by GeneratedYangParser for grammar rule
     * whenStatement.
     *
     * @param currentContext current context in the parsed tree
     */
    void exitWhenStatement(GeneratedYangParser.WhenStatementContext currentContext);

    /**
     * Enter a parse tree produced by GeneratedYangParser for grammar rule
     * rpcStatement.
     *
     * @param currentContext current context in the parsed tree
     */
    void enterRpcStatement(GeneratedYangParser.RpcStatementContext currentContext);

    /**
     * Exit a parse tree produced by GeneratedYangParser for grammar rule
     * rpcStatement.
     *
     * @param currentContext current context in the parsed tree
     */
    void exitRpcStatement(GeneratedYangParser.RpcStatementContext currentContext);

    /**
     * Enter a parse tree produced by GeneratedYangParser for grammar rule
     * inputStatement.
     *
     * @param currentContext current context in the parsed tree
     */
    void enterInputStatement(GeneratedYangParser.InputStatementContext currentContext);

    /**
     * Exit a parse tree produced by GeneratedYangParser for grammar rule
     * inputStatement.
     *
     * @param currentContext current context in the parsed tree
     */
    void exitInputStatement(GeneratedYangParser.InputStatementContext currentContext);

    /**
     * Enter a parse tree produced by GeneratedYangParser for grammar rule
     * outputStatement.
     *
     * @param currentContext current context in the parsed tree
     */
    void enterOutputStatement(GeneratedYangParser.OutputStatementContext currentContext);

    /**
     * Exit a parse tree produced by GeneratedYangParser for grammar rule
     * outputStatement.
     *
     * @param currentContext current context in the parsed tree
     */
    void exitOutputStatement(GeneratedYangParser.OutputStatementContext currentContext);

    /**
     * Enter a parse tree produced by GeneratedYangParser for grammar rule
     * notificationStatement.
     *
     * @param currentContext current context in the parsed tree
     */
    void enterNotificationStatement(GeneratedYangParser.NotificationStatementContext currentContext);

    /**
     * Exit a parse tree produced by GeneratedYangParser for grammar rule
     * notificationStatement.
     *
     * @param currentContext current context in the parsed tree
     */
    void exitNotificationStatement(GeneratedYangParser.NotificationStatementContext currentContext);

    /**
     * Enter a parse tree produced by GeneratedYangParser for grammar rule
     * deviationStatement.
     *
     * @param currentContext current context in the parsed tree
     */
    void enterDeviationStatement(GeneratedYangParser.DeviationStatementContext currentContext);

    /**
     * Exit a parse tree produced by GeneratedYangParser for grammar rule
     * deviationStatement.
     *
     * @param currentContext current context in the parsed tree
     */
    void exitDeviationStatement(GeneratedYangParser.DeviationStatementContext currentContext);

    /**
     * Enter a parse tree produced by GeneratedYangParser for grammar rule
     * deviateNotSupportedStatement.
     *
     * @param currentContext current context in the parsed tree
     */
    void enterDeviateNotSupportedStatement(GeneratedYangParser.DeviateNotSupportedStatementContext currentContext);

    /**
     * Exit a parse tree produced by GeneratedYangParser for grammar rule
     * deviateNotSupportedStatement.
     *
     * @param currentContext current context in the parsed tree
     */
    void exitDeviateNotSupportedStatement(GeneratedYangParser.DeviateNotSupportedStatementContext currentContext);

    /**
     * Enter a parse tree produced by GeneratedYangParser for grammar rule
     * deviateAddStatement.
     *
     * @param currentContext current context in the parsed tree
     */
    void enterDeviateAddStatement(GeneratedYangParser.DeviateAddStatementContext currentContext);

    /**
     * Exit a parse tree produced by GeneratedYangParser for grammar rule
     * deviateAddStatement.
     *
     * @param currentContext current context in the parsed tree
     */
    void exitDeviateAddStatement(GeneratedYangParser.DeviateAddStatementContext currentContext);

    /**
     * Enter a parse tree produced by GeneratedYangParser for grammar rule
     * deviateDeleteStatement.
     *
     * @param currentContext current context in the parsed tree
     */
    void enterDeviateDeleteStatement(GeneratedYangParser.DeviateDeleteStatementContext currentContext);

    /**
     * Exit a parse tree produced by GeneratedYangParser for grammar rule
     * deviateDeleteStatement.
     *
     * @param currentContext current context in the parsed tree
     */
    void exitDeviateDeleteStatement(GeneratedYangParser.DeviateDeleteStatementContext currentContext);

    /**
     * Enter a parse tree produced by GeneratedYangParser for grammar rule
     * deviateReplaceStatement.
     *
     * @param currentContext current context in the parsed tree
     */
    void enterDeviateReplaceStatement(GeneratedYangParser.DeviateReplaceStatementContext currentContext);

    /**
     * Exit a parse tree produced by GeneratedYangParser for grammar rule
     * deviateReplaceStatement.
     *
     * @param currentContext current context in the parsed tree
     */
    void exitDeviateReplaceStatement(GeneratedYangParser.DeviateReplaceStatementContext currentContext);

    /**
     * Enter a parse tree produced by GeneratedYangParser for grammar rule
     * string.
     *
     * @param currentContext current context in the parsed tree
     */
    void enterString(GeneratedYangParser.StringContext currentContext);

    /**
     * Exit a parse tree produced by GeneratedYangParser for grammar rule
     * string.
     *
     * @param currentContext current context in the parsed tree
     */
    void exitString(GeneratedYangParser.StringContext currentContext);

    /**
     * Enter a parse tree produced by GeneratedYangParser for grammar rule
     * identifier.
     *
     * @param currentContext current context in the parsed tree
     */
    void enterIdentifier(GeneratedYangParser.IdentifierContext currentContext);

    /**
     * Exit a parse tree produced by GeneratedYangParser for grammar rule
     * identifier.
     *
     * @param currentContext current context in the parsed tree
     */
    void exitIdentifier(GeneratedYangParser.IdentifierContext currentContext);


    /**
     * Enter a parse tree produced by GeneratedYangParser for grammar rule
     * dateArgumentString.
     *
     * @param currentContext current context in the parsed tree
     */
    void enterDateArgumentString(GeneratedYangParser.DateArgumentStringContext currentContext);

    /**
     * Exit a parse tree produced by GeneratedYangParser for grammar rule
     * dateArgumentString.
     *
     * @param currentContext current context in the parsed tree
     */
    void exitDateArgumentString(GeneratedYangParser.DateArgumentStringContext currentContext);
}

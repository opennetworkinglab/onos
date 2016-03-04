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

package org.onosproject.yangutils.parser.impl;

import java.util.Stack;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ErrorNode;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.onosproject.yangutils.datamodel.YangNode;
import org.onosproject.yangutils.parser.Parsable;
import org.onosproject.yangutils.parser.antlrgencode.GeneratedYangListener;
import org.onosproject.yangutils.parser.antlrgencode.GeneratedYangParser;
import org.onosproject.yangutils.parser.impl.listeners.BaseFileListener;
import org.onosproject.yangutils.parser.impl.listeners.BelongsToListener;
import org.onosproject.yangutils.parser.impl.listeners.BitListener;
import org.onosproject.yangutils.parser.impl.listeners.BitsListener;
import org.onosproject.yangutils.parser.impl.listeners.ConfigListener;
import org.onosproject.yangutils.parser.impl.listeners.ContactListener;
import org.onosproject.yangutils.parser.impl.listeners.ContainerListener;
import org.onosproject.yangutils.parser.impl.listeners.DefaultListener;
import org.onosproject.yangutils.parser.impl.listeners.DescriptionListener;
import org.onosproject.yangutils.parser.impl.listeners.EnumListener;
import org.onosproject.yangutils.parser.impl.listeners.EnumerationListener;
import org.onosproject.yangutils.parser.impl.listeners.ImportListener;
import org.onosproject.yangutils.parser.impl.listeners.IncludeListener;
import org.onosproject.yangutils.parser.impl.listeners.KeyListener;
import org.onosproject.yangutils.parser.impl.listeners.LeafListListener;
import org.onosproject.yangutils.parser.impl.listeners.LeafListener;
import org.onosproject.yangutils.parser.impl.listeners.ListListener;
import org.onosproject.yangutils.parser.impl.listeners.MandatoryListener;
import org.onosproject.yangutils.parser.impl.listeners.MaxElementsListener;
import org.onosproject.yangutils.parser.impl.listeners.MinElementsListener;
import org.onosproject.yangutils.parser.impl.listeners.ModuleListener;
import org.onosproject.yangutils.parser.impl.listeners.NamespaceListener;
import org.onosproject.yangutils.parser.impl.listeners.OrganizationListener;
import org.onosproject.yangutils.parser.impl.listeners.PositionListener;
import org.onosproject.yangutils.parser.impl.listeners.PrefixListener;
import org.onosproject.yangutils.parser.impl.listeners.PresenceListener;
import org.onosproject.yangutils.parser.impl.listeners.ReferenceListener;
import org.onosproject.yangutils.parser.impl.listeners.RevisionDateListener;
import org.onosproject.yangutils.parser.impl.listeners.RevisionListener;
import org.onosproject.yangutils.parser.impl.listeners.StatusListener;
import org.onosproject.yangutils.parser.impl.listeners.SubModuleListener;
import org.onosproject.yangutils.parser.impl.listeners.TypeDefListener;
import org.onosproject.yangutils.parser.impl.listeners.TypeListener;
import org.onosproject.yangutils.parser.impl.listeners.UnitsListener;
import org.onosproject.yangutils.parser.impl.listeners.ValueListener;
import org.onosproject.yangutils.parser.impl.listeners.VersionListener;

/**
 * ANTLR generates a parse-tree listener interface that responds to events
 * triggered by the built-in tree walker. The methods in listener are just
 * callbacks. This class implements listener interface and generates the
 * corresponding data model tree.
 */
public class TreeWalkListener implements GeneratedYangListener {

    // List of parsable node entries maintained in stack
    private Stack<Parsable> parsedDataStack = new Stack<>();

    // Parse tree root node
    private YangNode rootNode;

    /**
     * Returns stack of parsable data.
     *
     * @return stack of parsable data
     */
    public Stack<Parsable> getParsedDataStack() {
        return parsedDataStack;
    }

    /**
     * Returns root node.
     *
     * @return rootNode of data model tree
     */
    public YangNode getRootNode() {
        return rootNode;
    }

    /**
     * Set parsed data stack.
     *
     * @param parsedDataStack stack of parsable data objects
     */
    public void setParsedDataStack(Stack<Parsable> parsedDataStack) {
        this.parsedDataStack = parsedDataStack;
    }

    /**
     * Set root node.
     *
     * @param rootNode root node of data model tree
     */
    public void setRootNode(YangNode rootNode) {
        this.rootNode = rootNode;
    }

    @Override
    public void enterYangfile(GeneratedYangParser.YangfileContext ctx) {
        BaseFileListener.processYangFileEntry(this, ctx);
    }

    @Override
    public void exitYangfile(GeneratedYangParser.YangfileContext ctx) {
        BaseFileListener.processYangFileExit(this, ctx);
    }

    @Override
    public void enterModuleStatement(GeneratedYangParser.ModuleStatementContext ctx) {
        ModuleListener.processModuleEntry(this, ctx);
    }

    @Override
    public void exitModuleStatement(GeneratedYangParser.ModuleStatementContext ctx) {
        ModuleListener.processModuleExit(this, ctx);
    }

    @Override
    public void enterModuleBody(GeneratedYangParser.ModuleBodyContext ctx) {
        // TODO: implement the method.
    }

    @Override
    public void exitModuleBody(GeneratedYangParser.ModuleBodyContext ctx) {
        // TODO: implement the method.
    }

    @Override
    public void enterModuleHeaderStatement(GeneratedYangParser.ModuleHeaderStatementContext ctx) {
        // TODO: implement the method.
    }

    @Override
    public void exitModuleHeaderStatement(GeneratedYangParser.ModuleHeaderStatementContext ctx) {
        // TODO: implement the method.
    }

    @Override
    public void enterLinkageStatements(GeneratedYangParser.LinkageStatementsContext ctx) {
        // TODO: implement the method.
    }

    @Override
    public void exitLinkageStatements(GeneratedYangParser.LinkageStatementsContext ctx) {
        // TODO: implement the method.
    }

    @Override
    public void enterMetaStatements(GeneratedYangParser.MetaStatementsContext ctx) {
        // TODO: implement the method.
    }

    @Override
    public void exitMetaStatements(GeneratedYangParser.MetaStatementsContext ctx) {
        // TODO: implement the method.
    }

    @Override
    public void enterRevisionStatements(GeneratedYangParser.RevisionStatementsContext ctx) {
        // TODO: implement the method.
    }

    @Override
    public void exitRevisionStatements(GeneratedYangParser.RevisionStatementsContext ctx) {
        // TODO: implement the method.
    }

    @Override
    public void enterBodyStatements(GeneratedYangParser.BodyStatementsContext ctx) {
        // TODO: implement the method.
    }

    @Override
    public void exitBodyStatements(GeneratedYangParser.BodyStatementsContext ctx) {
        // TODO: implement the method.
    }

    @Override
    public void enterYangVersionStatement(GeneratedYangParser.YangVersionStatementContext ctx) {
        VersionListener.processVersionEntry(this, ctx);
    }

    @Override
    public void exitYangVersionStatement(GeneratedYangParser.YangVersionStatementContext ctx) {
        // TODO: implement the method.
    }

    @Override
    public void enterNamespaceStatement(GeneratedYangParser.NamespaceStatementContext ctx) {
        NamespaceListener.processNamespaceEntry(this, ctx);
    }

    @Override
    public void exitNamespaceStatement(GeneratedYangParser.NamespaceStatementContext ctx) {
        // TODO: implement the method.
    }

    @Override
    public void enterPrefixStatement(GeneratedYangParser.PrefixStatementContext ctx) {
        PrefixListener.processPrefixEntry(this, ctx);
    }

    @Override
    public void exitPrefixStatement(GeneratedYangParser.PrefixStatementContext ctx) {
        // TODO: implement the method.
    }

    @Override
    public void enterImportStatement(GeneratedYangParser.ImportStatementContext ctx) {
        ImportListener.processImportEntry(this, ctx);
    }

    @Override
    public void exitImportStatement(GeneratedYangParser.ImportStatementContext ctx) {
        ImportListener.processImportExit(this, ctx);
    }

    @Override
    public void enterImportStatementBody(GeneratedYangParser.ImportStatementBodyContext ctx) {
        // TODO: implement the method.
    }

    @Override
    public void exitImportStatementBody(GeneratedYangParser.ImportStatementBodyContext ctx) {
        // TODO: implement the method.
    }

    @Override
    public void enterRevisionDateStatement(GeneratedYangParser.RevisionDateStatementContext ctx) {
        RevisionDateListener.processRevisionDateEntry(this, ctx);
    }

    @Override
    public void exitRevisionDateStatement(GeneratedYangParser.RevisionDateStatementContext ctx) {
        // TODO: implement the method.
    }

    @Override
    public void enterIncludeStatement(GeneratedYangParser.IncludeStatementContext ctx) {
        IncludeListener.processIncludeEntry(this, ctx);
    }

    @Override
    public void exitIncludeStatement(GeneratedYangParser.IncludeStatementContext ctx) {
        IncludeListener.processIncludeExit(this, ctx);
    }

    @Override
    public void enterOrganizationStatement(GeneratedYangParser.OrganizationStatementContext ctx) {
        OrganizationListener.processOrganizationEntry(this, ctx);
    }

    @Override
    public void exitOrganizationStatement(GeneratedYangParser.OrganizationStatementContext ctx) {
        // TODO: implement the method.
    }

    @Override
    public void enterContactStatement(GeneratedYangParser.ContactStatementContext ctx) {
        ContactListener.processContactEntry(this, ctx);
    }

    @Override
    public void exitContactStatement(GeneratedYangParser.ContactStatementContext ctx) {
        // TODO: implement the method.
    }

    @Override
    public void enterDescriptionStatement(GeneratedYangParser.DescriptionStatementContext ctx) {
        DescriptionListener.processDescriptionEntry(this, ctx);
    }

    @Override
    public void exitDescriptionStatement(GeneratedYangParser.DescriptionStatementContext ctx) {
        // TODO: implement the method.
    }

    @Override
    public void enterReferenceStatement(GeneratedYangParser.ReferenceStatementContext ctx) {
        ReferenceListener.processReferenceEntry(this, ctx);
    }

    @Override
    public void exitReferenceStatement(GeneratedYangParser.ReferenceStatementContext ctx) {
        // TODO: implement the method.
    }

    @Override
    public void enterRevisionStatement(GeneratedYangParser.RevisionStatementContext ctx) {
        RevisionListener.processRevisionEntry(this, ctx);
    }

    @Override
    public void exitRevisionStatement(GeneratedYangParser.RevisionStatementContext ctx) {
        RevisionListener.processRevisionExit(this, ctx);
    }

    @Override
    public void enterRevisionStatementBody(GeneratedYangParser.RevisionStatementBodyContext ctx) {
        // TODO: implement the method.
    }

    @Override
    public void exitRevisionStatementBody(GeneratedYangParser.RevisionStatementBodyContext ctx) {
        // TODO: implement the method.
    }

    @Override
    public void enterSubModuleStatement(GeneratedYangParser.SubModuleStatementContext ctx) {
        SubModuleListener.processSubModuleEntry(this, ctx);
    }

    @Override
    public void exitSubModuleStatement(GeneratedYangParser.SubModuleStatementContext ctx) {
        SubModuleListener.processSubModuleExit(this, ctx);
    }

    @Override
    public void enterSubmoduleBody(GeneratedYangParser.SubmoduleBodyContext ctx) {
        // TODO: implement the method.
    }

    @Override
    public void exitSubmoduleBody(GeneratedYangParser.SubmoduleBodyContext ctx) {
        // TODO: implement the method.
    }

    @Override
    public void enterSubmoduleHeaderStatement(GeneratedYangParser.SubmoduleHeaderStatementContext ctx) {
        // TODO: implement the method.
    }

    @Override
    public void exitSubmoduleHeaderStatement(GeneratedYangParser.SubmoduleHeaderStatementContext ctx) {
        // TODO: implement the method.
    }

    @Override
    public void enterBelongstoStatement(GeneratedYangParser.BelongstoStatementContext ctx) {
        BelongsToListener.processBelongsToEntry(this, ctx);
    }

    @Override
    public void exitBelongstoStatement(GeneratedYangParser.BelongstoStatementContext ctx) {
        BelongsToListener.processBelongsToExit(this, ctx);
    }

    @Override
    public void enterBelongstoStatementBody(GeneratedYangParser.BelongstoStatementBodyContext ctx) {
        // TODO: implement the method.
    }

    @Override
    public void exitBelongstoStatementBody(GeneratedYangParser.BelongstoStatementBodyContext ctx) {
        // TODO: implement the method.
    }

    @Override
    public void enterExtensionStatement(GeneratedYangParser.ExtensionStatementContext ctx) {
        // TODO: implement the method.
    }

    @Override
    public void exitExtensionStatement(GeneratedYangParser.ExtensionStatementContext ctx) {
        // TODO: implement the method.
    }

    @Override
    public void enterExtensionBody(GeneratedYangParser.ExtensionBodyContext ctx) {
        // TODO: implement the method.
    }

    @Override
    public void exitExtensionBody(GeneratedYangParser.ExtensionBodyContext ctx) {
        // TODO: implement the method.
    }

    @Override
    public void enterArgumentStatement(GeneratedYangParser.ArgumentStatementContext ctx) {
        // TODO: implement the method.
    }

    @Override
    public void exitArgumentStatement(GeneratedYangParser.ArgumentStatementContext ctx) {
        // TODO: implement the method.
    }

    @Override
    public void enterArgumentBody(GeneratedYangParser.ArgumentBodyContext ctx) {
        // TODO: implement the method.
    }

    @Override
    public void exitArgumentBody(GeneratedYangParser.ArgumentBodyContext ctx) {
        // TODO: implement the method.
    }

    @Override
    public void enterYinElementStatement(GeneratedYangParser.YinElementStatementContext ctx) {
        // TODO: implement the method.
    }

    @Override
    public void exitYinElementStatement(GeneratedYangParser.YinElementStatementContext ctx) {
        // TODO: implement the method.
    }

    @Override
    public void enterIdentityStatement(GeneratedYangParser.IdentityStatementContext ctx) {
        // TODO: implement the method.
    }

    @Override
    public void exitIdentityStatement(GeneratedYangParser.IdentityStatementContext ctx) {
        // TODO: implement the method.
    }

    @Override
    public void enterIdentityBody(GeneratedYangParser.IdentityBodyContext ctx) {
        // TODO: implement the method.
    }

    @Override
    public void exitIdentityBody(GeneratedYangParser.IdentityBodyContext ctx) {
        // TODO: implement the method.
    }

    @Override
    public void enterBaseStatement(GeneratedYangParser.BaseStatementContext ctx) {
        // TODO: implement the method.
    }

    @Override
    public void exitBaseStatement(GeneratedYangParser.BaseStatementContext ctx) {
        // TODO: implement the method.
    }

    @Override
    public void enterFeatureStatement(GeneratedYangParser.FeatureStatementContext ctx) {
        // TODO: implement the method.
    }

    @Override
    public void exitFeatureStatement(GeneratedYangParser.FeatureStatementContext ctx) {
        // TODO: implement the method.
    }

    @Override
    public void enterFeatureBody(GeneratedYangParser.FeatureBodyContext ctx) {
        // TODO: implement the method.
    }

    @Override
    public void exitFeatureBody(GeneratedYangParser.FeatureBodyContext ctx) {
        // TODO: implement the method.
    }

    @Override
    public void enterDataDefStatement(GeneratedYangParser.DataDefStatementContext ctx) {
        // TODO: implement the method.
    }

    @Override
    public void exitDataDefStatement(GeneratedYangParser.DataDefStatementContext ctx) {
        // TODO: implement the method.
    }

    @Override
    public void enterIfFeatureStatement(GeneratedYangParser.IfFeatureStatementContext ctx) {
        // TODO: implement the method.
    }

    @Override
    public void exitIfFeatureStatement(GeneratedYangParser.IfFeatureStatementContext ctx) {
        // TODO: implement the method.
    }

    @Override
    public void enterUnitsStatement(GeneratedYangParser.UnitsStatementContext ctx) {
        UnitsListener.processUnitsEntry(this, ctx);
    }

    @Override
    public void exitUnitsStatement(GeneratedYangParser.UnitsStatementContext ctx) {
        // TODO: implement the method.
    }

    @Override
    public void enterTypedefStatement(GeneratedYangParser.TypedefStatementContext ctx) {
        TypeDefListener.processTypeDefEntry(this, ctx);
    }

    @Override
    public void exitTypedefStatement(GeneratedYangParser.TypedefStatementContext ctx) {
        TypeDefListener.processTypeDefExit(this, ctx);
    }

    @Override
    public void enterTypeStatement(GeneratedYangParser.TypeStatementContext ctx) {
        TypeListener.processTypeEntry(this, ctx);
    }

    @Override
    public void exitTypeStatement(GeneratedYangParser.TypeStatementContext ctx) {
        TypeListener.processTypeExit(this, ctx);
    }

    @Override
    public void enterTypeBodyStatements(GeneratedYangParser.TypeBodyStatementsContext ctx) {
        // TODO: implement the method.
    }

    @Override
    public void exitTypeBodyStatements(GeneratedYangParser.TypeBodyStatementsContext ctx) {
        // TODO: implement the method.
    }

    @Override
    public void enterNumericalRestrictions(GeneratedYangParser.NumericalRestrictionsContext ctx) {
        // TODO: implement the method.
    }

    @Override
    public void exitNumericalRestrictions(GeneratedYangParser.NumericalRestrictionsContext ctx) {
        // TODO: implement the method.
    }

    @Override
    public void enterRangeStatement(GeneratedYangParser.RangeStatementContext ctx) {
        // TODO: implement the method.
    }

    @Override
    public void exitRangeStatement(GeneratedYangParser.RangeStatementContext ctx) {
        // TODO: implement the method.
    }

    @Override
    public void enterCommonStatements(GeneratedYangParser.CommonStatementsContext ctx) {
        // TODO: implement the method.
    }

    @Override
    public void exitCommonStatements(GeneratedYangParser.CommonStatementsContext ctx) {
        // TODO: implement the method.
    }

    @Override
    public void enterStringRestrictions(GeneratedYangParser.StringRestrictionsContext ctx) {
        // TODO: implement the method.
    }

    @Override
    public void exitStringRestrictions(GeneratedYangParser.StringRestrictionsContext ctx) {
        // TODO: implement the method.
    }

    @Override
    public void enterLengthStatement(GeneratedYangParser.LengthStatementContext ctx) {
        // TODO: implement the method.
    }

    @Override
    public void exitLengthStatement(GeneratedYangParser.LengthStatementContext ctx) {
        // TODO: implement the method.
    }

    @Override
    public void enterPatternStatement(GeneratedYangParser.PatternStatementContext ctx) {
        // TODO: implement the method.
    }

    @Override
    public void exitPatternStatement(GeneratedYangParser.PatternStatementContext ctx) {
        // TODO: implement the method.
    }

    @Override
    public void enterDefaultStatement(GeneratedYangParser.DefaultStatementContext ctx) {
        DefaultListener.processDefaultEntry(this, ctx);
    }

    @Override
    public void exitDefaultStatement(GeneratedYangParser.DefaultStatementContext ctx) {
        // TODO: implement the method.
    }

    @Override
    public void enterEnumSpecification(GeneratedYangParser.EnumSpecificationContext ctx) {
        EnumerationListener.processEnumerationEntry(this, ctx);
    }

    @Override
    public void exitEnumSpecification(GeneratedYangParser.EnumSpecificationContext ctx) {
        EnumerationListener.processEnumerationExit(this, ctx);
    }

    @Override
    public void enterEnumStatement(GeneratedYangParser.EnumStatementContext ctx) {
        EnumListener.processEnumEntry(this, ctx);
    }

    @Override
    public void exitEnumStatement(GeneratedYangParser.EnumStatementContext ctx) {
        EnumListener.processEnumExit(this, ctx);
    }

    @Override
    public void enterEnumStatementBody(GeneratedYangParser.EnumStatementBodyContext ctx) {
        // TODO: implement the method.
    }

    @Override
    public void exitEnumStatementBody(GeneratedYangParser.EnumStatementBodyContext ctx) {
        // TODO: implement the method.
    }

    @Override
    public void enterLeafrefSpecification(GeneratedYangParser.LeafrefSpecificationContext ctx) {
        // TODO: implement the method.
    }

    @Override
    public void exitLeafrefSpecification(GeneratedYangParser.LeafrefSpecificationContext ctx) {
        // TODO: implement the method.
    }

    @Override
    public void enterPathStatement(GeneratedYangParser.PathStatementContext ctx) {
        // TODO: implement the method.
    }

    @Override
    public void exitPathStatement(GeneratedYangParser.PathStatementContext ctx) {
        // TODO: implement the method.
    }

    @Override
    public void enterRequireInstanceStatement(GeneratedYangParser.RequireInstanceStatementContext ctx) {
        // TODO: implement the method.
    }

    @Override
    public void exitRequireInstanceStatement(GeneratedYangParser.RequireInstanceStatementContext ctx) {
        // TODO: implement the method.
    }

    @Override
    public void enterInstanceIdentifierSpecification(GeneratedYangParser.InstanceIdentifierSpecificationContext ctx) {
        // TODO: implement the method.
    }

    @Override
    public void exitInstanceIdentifierSpecification(GeneratedYangParser.InstanceIdentifierSpecificationContext ctx) {
        // TODO: implement the method.
    }

    @Override
    public void enterIdentityrefSpecification(GeneratedYangParser.IdentityrefSpecificationContext ctx) {
        // TODO: implement the method.
    }

    @Override
    public void exitIdentityrefSpecification(GeneratedYangParser.IdentityrefSpecificationContext ctx) {
        // TODO: implement the method.
    }

    @Override
    public void enterUnionSpecification(GeneratedYangParser.UnionSpecificationContext ctx) {
        // TODO: implement the method.
    }

    @Override
    public void exitUnionSpecification(GeneratedYangParser.UnionSpecificationContext ctx) {
        // TODO: implement the method.
    }

    @Override
    public void enterBitsSpecification(GeneratedYangParser.BitsSpecificationContext ctx) {
        BitsListener.processBitsEntry(this, ctx);
    }

    @Override
    public void exitBitsSpecification(GeneratedYangParser.BitsSpecificationContext ctx) {
        BitsListener.processBitsExit(this, ctx);
    }

    @Override
    public void enterBitStatement(GeneratedYangParser.BitStatementContext ctx) {
        BitListener.processBitEntry(this, ctx);
    }

    @Override
    public void exitBitStatement(GeneratedYangParser.BitStatementContext ctx) {
        BitListener.processBitExit(this, ctx);
    }

    @Override
    public void enterBitBodyStatement(GeneratedYangParser.BitBodyStatementContext ctx) {
        // TODO: implement the method.
    }

    @Override
    public void exitBitBodyStatement(GeneratedYangParser.BitBodyStatementContext ctx) {
        // TODO: implement the method.
    }

    @Override
    public void enterPositionStatement(GeneratedYangParser.PositionStatementContext ctx) {
        PositionListener.processPositionEntry(this, ctx);
    }

    @Override
    public void exitPositionStatement(GeneratedYangParser.PositionStatementContext ctx) {
        // TODO: implement the method.
    }

    @Override
    public void enterStatusStatement(GeneratedYangParser.StatusStatementContext ctx) {
        StatusListener.processStatusEntry(this, ctx);
    }

    @Override
    public void exitStatusStatement(GeneratedYangParser.StatusStatementContext ctx) {
        // TODO: implement the method.
    }

    @Override
    public void enterConfigStatement(GeneratedYangParser.ConfigStatementContext ctx) {
        ConfigListener.processConfigEntry(this, ctx);
    }

    @Override
    public void exitConfigStatement(GeneratedYangParser.ConfigStatementContext ctx) {
        // TODO: implement the method.
    }

    @Override
    public void enterMandatoryStatement(GeneratedYangParser.MandatoryStatementContext ctx) {
        MandatoryListener.processMandatoryEntry(this, ctx);
    }

    @Override
    public void exitMandatoryStatement(GeneratedYangParser.MandatoryStatementContext ctx) {
        // TODO: implement the method.
    }

    @Override
    public void enterPresenceStatement(GeneratedYangParser.PresenceStatementContext ctx) {
        PresenceListener.processPresenceEntry(this, ctx);
    }

    @Override
    public void exitPresenceStatement(GeneratedYangParser.PresenceStatementContext ctx) {
        // TODO: implement the method.
    }

    @Override
    public void enterOrderedByStatement(GeneratedYangParser.OrderedByStatementContext ctx) {
        // TODO: implement the method.
    }

    @Override
    public void exitOrderedByStatement(GeneratedYangParser.OrderedByStatementContext ctx) {
        // TODO: implement the method.
    }

    @Override
    public void enterMustStatement(GeneratedYangParser.MustStatementContext ctx) {
        // TODO: implement the method.
    }

    @Override
    public void exitMustStatement(GeneratedYangParser.MustStatementContext ctx) {
        // TODO: implement the method.
    }

    @Override
    public void enterErrorMessageStatement(GeneratedYangParser.ErrorMessageStatementContext ctx) {
        // TODO: implement the method.
    }

    @Override
    public void exitErrorMessageStatement(GeneratedYangParser.ErrorMessageStatementContext ctx) {
        // TODO: implement the method.
    }

    @Override
    public void enterErrorAppTagStatement(GeneratedYangParser.ErrorAppTagStatementContext ctx) {
        // TODO: implement the method.
    }

    @Override
    public void exitErrorAppTagStatement(GeneratedYangParser.ErrorAppTagStatementContext ctx) {
        // TODO: implement the method.
    }

    @Override
    public void enterMinElementsStatement(GeneratedYangParser.MinElementsStatementContext ctx) {
        MinElementsListener.processMinElementsEntry(this, ctx);
    }

    @Override
    public void exitMinElementsStatement(GeneratedYangParser.MinElementsStatementContext ctx) {
        // TODO: implement the method.
    }

    @Override
    public void enterMaxElementsStatement(GeneratedYangParser.MaxElementsStatementContext ctx) {
        MaxElementsListener.processMaxElementsEntry(this, ctx);
    }

    @Override
    public void exitMaxElementsStatement(GeneratedYangParser.MaxElementsStatementContext ctx) {
        // TODO: implement the method.
    }

    @Override
    public void enterMaxValueArgument(GeneratedYangParser.MaxValueArgumentContext ctx) {
        // TODO: implement the method.
    }

    @Override
    public void exitMaxValueArgument(GeneratedYangParser.MaxValueArgumentContext ctx) {
        // TODO: implement the method.
    }

    @Override
    public void enterValueStatement(GeneratedYangParser.ValueStatementContext ctx) {
        ValueListener.processValueEntry(this, ctx);
    }

    @Override
    public void exitValueStatement(GeneratedYangParser.ValueStatementContext ctx) {
        // TODO: implement the method.
    }

    @Override
    public void enterGroupingStatement(GeneratedYangParser.GroupingStatementContext ctx) {
        // TODO: implement the method.
    }

    @Override
    public void exitGroupingStatement(GeneratedYangParser.GroupingStatementContext ctx) {
        // TODO: implement the method.
    }

    @Override
    public void enterContainerStatement(GeneratedYangParser.ContainerStatementContext ctx) {
        ContainerListener.processContainerEntry(this, ctx);
    }

    @Override
    public void exitContainerStatement(GeneratedYangParser.ContainerStatementContext ctx) {
        ContainerListener.processContainerExit(this, ctx);
    }

    @Override
    public void enterLeafStatement(GeneratedYangParser.LeafStatementContext ctx) {
        LeafListener.processLeafEntry(this, ctx);
    }

    @Override
    public void exitLeafStatement(GeneratedYangParser.LeafStatementContext ctx) {
        LeafListener.processLeafExit(this, ctx);
    }

    @Override
    public void enterLeafListStatement(GeneratedYangParser.LeafListStatementContext ctx) {
        LeafListListener.processLeafListEntry(this, ctx);
    }

    @Override
    public void exitLeafListStatement(GeneratedYangParser.LeafListStatementContext ctx) {
        LeafListListener.processLeafListExit(this, ctx);
    }

    @Override
    public void enterListStatement(GeneratedYangParser.ListStatementContext ctx) {
        ListListener.processListEntry(this, ctx);
    }

    @Override
    public void exitListStatement(GeneratedYangParser.ListStatementContext ctx) {
        ListListener.processListExit(this, ctx);
    }

    @Override
    public void enterKeyStatement(GeneratedYangParser.KeyStatementContext ctx) {
        KeyListener.processKeyEntry(this, ctx);
    }

    @Override
    public void exitKeyStatement(GeneratedYangParser.KeyStatementContext ctx) {
        // TODO: implement the method.
    }

    @Override
    public void enterUniqueStatement(GeneratedYangParser.UniqueStatementContext ctx) {
        // TODO: implement the method.
    }

    @Override
    public void exitUniqueStatement(GeneratedYangParser.UniqueStatementContext ctx) {
        // TODO: implement the method.
    }

    @Override
    public void enterChoiceStatement(GeneratedYangParser.ChoiceStatementContext ctx) {
        // TODO: implement the method.
    }

    @Override
    public void exitChoiceStatement(GeneratedYangParser.ChoiceStatementContext ctx) {
        // TODO: implement the method.
    }

    @Override
    public void enterShortCaseStatement(GeneratedYangParser.ShortCaseStatementContext ctx) {
        // TODO: implement the method.
    }

    @Override
    public void exitShortCaseStatement(GeneratedYangParser.ShortCaseStatementContext ctx) {
        // TODO: implement the method.
    }

    @Override
    public void enterCaseStatement(GeneratedYangParser.CaseStatementContext ctx) {
        // TODO: implement the method.
    }

    @Override
    public void exitCaseStatement(GeneratedYangParser.CaseStatementContext ctx) {
        // TODO: implement the method.
    }

    @Override
    public void enterUsesStatement(GeneratedYangParser.UsesStatementContext ctx) {
        // TODO: implement the method.
    }

    @Override
    public void exitUsesStatement(GeneratedYangParser.UsesStatementContext ctx) {
        // TODO: implement the method.
    }

    @Override
    public void enterRefineStatement(GeneratedYangParser.RefineStatementContext ctx) {
        // TODO: implement the method.
    }

    @Override
    public void exitRefineStatement(GeneratedYangParser.RefineStatementContext ctx) {
        // TODO: implement the method.
    }

    @Override
    public void enterRefineContainerStatements(GeneratedYangParser.RefineContainerStatementsContext ctx) {
        // TODO: implement the method.
    }

    @Override
    public void exitRefineContainerStatements(GeneratedYangParser.RefineContainerStatementsContext ctx) {
        // TODO: implement the method.
    }

    @Override
    public void enterRefineLeafStatements(GeneratedYangParser.RefineLeafStatementsContext ctx) {
        // TODO: implement the method.
    }

    @Override
    public void exitRefineLeafStatements(GeneratedYangParser.RefineLeafStatementsContext ctx) {
        // TODO: implement the method.
    }

    @Override
    public void enterRefineLeafListStatements(GeneratedYangParser.RefineLeafListStatementsContext ctx) {
        // TODO: implement the method.
    }

    @Override
    public void exitRefineLeafListStatements(GeneratedYangParser.RefineLeafListStatementsContext ctx) {
        // TODO: implement the method.
    }

    @Override
    public void enterRefineListStatements(GeneratedYangParser.RefineListStatementsContext ctx) {
        // TODO: implement the method.
    }

    @Override
    public void exitRefineListStatements(GeneratedYangParser.RefineListStatementsContext ctx) {
        // TODO: implement the method.
    }

    @Override
    public void enterRefineChoiceStatements(GeneratedYangParser.RefineChoiceStatementsContext ctx) {
        // TODO: implement the method.
    }

    @Override
    public void exitRefineChoiceStatements(GeneratedYangParser.RefineChoiceStatementsContext ctx) {
        // TODO: implement the method.
    }

    @Override
    public void enterRefineCaseStatements(GeneratedYangParser.RefineCaseStatementsContext ctx) {
        // TODO: implement the method.
    }

    @Override
    public void exitRefineCaseStatements(GeneratedYangParser.RefineCaseStatementsContext ctx) {
        // TODO: implement the method.
    }

    @Override
    public void enterUsesAugmentStatement(GeneratedYangParser.UsesAugmentStatementContext ctx) {
        // TODO: implement the method.
    }

    @Override
    public void exitUsesAugmentStatement(GeneratedYangParser.UsesAugmentStatementContext ctx) {
        // TODO: implement the method.
    }

    @Override
    public void enterAugmentStatement(GeneratedYangParser.AugmentStatementContext ctx) {
        // TODO: implement the method.
    }

    @Override
    public void exitAugmentStatement(GeneratedYangParser.AugmentStatementContext ctx) {
        // TODO: implement the method.
    }

    @Override
    public void enterWhenStatement(GeneratedYangParser.WhenStatementContext ctx) {
        // TODO: implement the method.
    }

    @Override
    public void exitWhenStatement(GeneratedYangParser.WhenStatementContext ctx) {
        // TODO: implement the method.
    }

    @Override
    public void enterRpcStatement(GeneratedYangParser.RpcStatementContext ctx) {
        // TODO: implement the method.
    }

    @Override
    public void exitRpcStatement(GeneratedYangParser.RpcStatementContext ctx) {
        // TODO: implement the method.
    }

    @Override
    public void enterInputStatement(GeneratedYangParser.InputStatementContext ctx) {
        // TODO: implement the method.
    }

    @Override
    public void exitInputStatement(GeneratedYangParser.InputStatementContext ctx) {
        // TODO: implement the method.
    }

    @Override
    public void enterOutputStatement(GeneratedYangParser.OutputStatementContext ctx) {
        // TODO: implement the method.
    }

    @Override
    public void exitOutputStatement(GeneratedYangParser.OutputStatementContext ctx) {
        // TODO: implement the method.
    }

    @Override
    public void enterNotificationStatement(GeneratedYangParser.NotificationStatementContext ctx) {
        // TODO: implement the method.
    }

    @Override
    public void exitNotificationStatement(GeneratedYangParser.NotificationStatementContext ctx) {
        // TODO: implement the method.
    }

    @Override
    public void enterDeviationStatement(GeneratedYangParser.DeviationStatementContext ctx) {
        // TODO: implement the method.
    }

    @Override
    public void exitDeviationStatement(GeneratedYangParser.DeviationStatementContext ctx) {
        // TODO: implement the method.
    }

    @Override
    public void enterDeviateNotSupportedStatement(GeneratedYangParser.DeviateNotSupportedStatementContext ctx) {
        // TODO: implement the method.
    }

    @Override
    public void exitDeviateNotSupportedStatement(GeneratedYangParser.DeviateNotSupportedStatementContext ctx) {
        // TODO: implement the method.
    }

    @Override
    public void enterDeviateAddStatement(GeneratedYangParser.DeviateAddStatementContext ctx) {
        // TODO: implement the method.
    }

    @Override
    public void exitDeviateAddStatement(GeneratedYangParser.DeviateAddStatementContext ctx) {
        // TODO: implement the method.
    }

    @Override
    public void enterDeviateDeleteStatement(GeneratedYangParser.DeviateDeleteStatementContext ctx) {
        // TODO: implement the method.
    }

    @Override
    public void exitDeviateDeleteStatement(GeneratedYangParser.DeviateDeleteStatementContext ctx) {
        // TODO: implement the method.
    }

    @Override
    public void enterDeviateReplaceStatement(GeneratedYangParser.DeviateReplaceStatementContext ctx) {
        // TODO: implement the method.
    }

    @Override
    public void exitDeviateReplaceStatement(GeneratedYangParser.DeviateReplaceStatementContext ctx) {
        // TODO: implement the method.
    }

    @Override
    public void enterString(GeneratedYangParser.StringContext ctx) {
        // TODO: implement the method.
    }

    @Override
    public void exitString(GeneratedYangParser.StringContext ctx) {
        // TODO: implement the method.
    }

    @Override
    public void enterIdentifier(GeneratedYangParser.IdentifierContext ctx) {
        // TODO: implement the method.
    }

    @Override
    public void exitIdentifier(GeneratedYangParser.IdentifierContext ctx) {
        // TODO: implement the method.
    }

    @Override
    public void enterDateArgumentString(GeneratedYangParser.DateArgumentStringContext ctx) {
        // TODO: implement the method.
    }

    @Override
    public void exitDateArgumentString(GeneratedYangParser.DateArgumentStringContext ctx) {
        // TODO: implement the method.
    }

    @Override
    public void visitTerminal(TerminalNode terminalNode) {
        // TODO: implement the method.
    }

    @Override
    public void visitErrorNode(ErrorNode errorNode) {
        // TODO: implement the method.
    }

    @Override
    public void enterEveryRule(ParserRuleContext parserRuleContext) {
        // TODO: implement the method.
    }

    @Override
    public void exitEveryRule(ParserRuleContext parserRuleContext) {
        // TODO: implement the method.
    }
}

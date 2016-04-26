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

package org.onosproject.yangutils.linker;

import java.io.IOException;
import java.util.ListIterator;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.onosproject.yangutils.datamodel.ResolvableStatus;
import org.onosproject.yangutils.datamodel.YangContainer;
import org.onosproject.yangutils.datamodel.YangDataTypes;
import org.onosproject.yangutils.datamodel.YangGrouping;
import org.onosproject.yangutils.datamodel.YangLeaf;
import org.onosproject.yangutils.datamodel.YangList;
import org.onosproject.yangutils.datamodel.YangModule;
import org.onosproject.yangutils.datamodel.YangNode;
import org.onosproject.yangutils.datamodel.YangNodeType;
import org.onosproject.yangutils.datamodel.YangTypeDef;
import org.onosproject.yangutils.datamodel.YangUses;
import org.onosproject.yangutils.parser.exceptions.ParserException;
import org.onosproject.yangutils.parser.impl.YangUtilsParserManager;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

/**
 * Test cases for testing uses intra file linking.
 */
public class IntraFileUsesLinkingTest {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    private final YangUtilsParserManager manager = new YangUtilsParserManager();

    /**
     * Checks self resolution when grouping and uses are siblings.
     * Grouping followed by uses.
     */
    @Test
    public void processSelfResolutionWhenUsesAndGroupingAtRootLevel()
            throws IOException, ParserException {

        YangNode node = manager.getDataModel("src/test/resources/SelfResolutionWhenUsesAndGroupingAtRootLevel.yang");

        // Check whether the data model tree returned is of type module.
        assertThat((node instanceof YangModule), is(true));

        // Check whether the node type is set properly to module.
        assertThat(node.getNodeType(), is(YangNodeType.MODULE_NODE));

        // Check whether the module name is set correctly.
        YangModule yangNode = (YangModule) node;
        assertThat(yangNode.getName(), is("Test"));

        ListIterator<YangLeaf> leafIterator;
        YangLeaf leafInfo;

        // Check whether grouping is the sibling of module's child.
        assertThat((yangNode.getChild().getNextSibling() instanceof YangGrouping), is(true));

        YangGrouping grouping = (YangGrouping) yangNode.getChild().getNextSibling();
        leafIterator = grouping.getListOfLeaf().listIterator();
        leafInfo = leafIterator.next();

        // Check whether the information in the leaf is correct under grouping.
        assertThat(leafInfo.getLeafName(), is("hello"));
        assertThat(leafInfo.getDataType().getDataTypeName(), is("String"));
        assertThat(leafInfo.getDataType().getDataType(), is(YangDataTypes.STRING));

        // Check whether uses is module's child.
        assertThat((yangNode.getChild() instanceof YangUses), is(true));
        YangUses uses = (YangUses) yangNode.getChild();

        // Check whether uses get resolved.
        assertThat(uses.getResolvableStatus(),
                is(ResolvableStatus.RESOLVED));

        leafIterator = yangNode.getListOfLeaf().listIterator();
        leafInfo = leafIterator.next();

        // Check whether the information in the leaf is correct under module.
        assertThat(leafInfo.getLeafName(), is("hello"));
        assertThat(leafInfo.getDataType().getDataTypeName(), is("String"));
        assertThat(leafInfo.getDataType().getDataType(), is(YangDataTypes.STRING));

    }

    /**
     * Checks self resolution when grouping and uses are siblings.
     * Grouping has a child node.
     */
    @Test
    public void processSelfResolutionWhenUsesAndGroupingAtRootLevelGroupingWithChild()
            throws IOException, ParserException {

        YangNode node = manager.getDataModel(
                "src/test/resources/SelfResolutionWhenUsesAndGroupingAtRootLevelGroupingWithChild.yang");

        // Check whether the data model tree returned is of type module.
        assertThat((node instanceof YangModule), is(true));

        // Check whether the node type is set properly to module.
        assertThat(node.getNodeType(), is(YangNodeType.MODULE_NODE));

        // Check whether the module name is set correctly.
        YangModule yangNode = (YangModule) node;
        assertThat(yangNode.getName(), is("Test"));

        ListIterator<YangLeaf> leafIterator;
        YangLeaf leafInfo;

        // Check whether grouping is the sibling of module's child.
        assertThat((yangNode.getChild().getNextSibling() instanceof YangGrouping), is(true));

        YangGrouping grouping = (YangGrouping) yangNode.getChild().getNextSibling();
        leafIterator = grouping.getListOfLeaf().listIterator();
        leafInfo = leafIterator.next();

        // Check whether the information in the leaf is correct under grouping.
        assertThat(leafInfo.getLeafName(), is("treat"));
        assertThat(leafInfo.getDataType().getDataTypeName(), is("String"));
        assertThat(leafInfo.getDataType().getDataType(), is(YangDataTypes.STRING));

        // Check whether container is the child of grouping.
        assertThat((grouping.getChild() instanceof YangContainer), is(true));
        YangContainer container = (YangContainer) grouping.getChild();

        // Check whether the container name is set correctly which is under grouping.
        assertThat(container.getName(), is("test"));

        leafIterator = container.getListOfLeaf().listIterator();
        leafInfo = leafIterator.next();

        // Check whether the information in the leaf is correct under container which is under grouping.
        assertThat(leafInfo.getLeafName(), is("leaf2"));
        assertThat(leafInfo.getDataType().getDataTypeName(), is("String"));
        assertThat(leafInfo.getDataType().getDataType(), is(YangDataTypes.STRING));

        // Check whether uses is module's child.
        assertThat((yangNode.getChild() instanceof YangUses), is(true));
        YangUses uses = (YangUses) yangNode.getChild();

        // Check whether uses get resolved.
        assertThat(uses.getResolvableStatus(),
                is(ResolvableStatus.RESOLVED));

        leafIterator = yangNode.getListOfLeaf().listIterator();
        leafInfo = leafIterator.next();

        // Check whether the information in the leaf is correct under module.
        assertThat(leafInfo.getLeafName(), is("treat"));
        assertThat(leafInfo.getDataType().getDataTypeName(), is("String"));
        assertThat(leafInfo.getDataType().getDataType(), is(YangDataTypes.STRING));

        // Check whether container is the child of module.
        assertThat((grouping.getNextSibling() instanceof YangContainer), is(true));
        container = (YangContainer) grouping.getNextSibling();

        // Check whether the container name is set correctly which is under module.
        assertThat(container.getName(), is("test"));

        leafIterator = container.getListOfLeaf().listIterator();
        leafInfo = leafIterator.next();

        // Check whether the information in the leaf is correct under container which is under module.
        assertThat(leafInfo.getLeafName(), is("leaf2"));
        assertThat(leafInfo.getDataType().getDataTypeName(), is("String"));
        assertThat(leafInfo.getDataType().getDataType(), is(YangDataTypes.STRING));
    }

    /**
     * Checks self resolution when grouping in rpc and uses in output of the same rpc.
     * Uses is followed by grouping.
     */
    @Test
    public void processSelfResolutionGroupingInRpcAndUsesInOutput()
            throws IOException, ParserException {

        YangNode node = manager
                .getDataModel("src/test/resources/SelfResolutionGroupingInRpcAndUsesInOutput.yang");

        // Check whether the data model tree returned is of type module.
        assertThat((node instanceof YangModule), is(true));

        // Check whether the node type is set properly to module.
        assertThat(node.getNodeType(), is(YangNodeType.MODULE_NODE));

        // Check whether the module name is set correctly.
        YangModule yangNode = (YangModule) node;
        assertThat(yangNode.getName(), is("rock"));

        ListIterator<YangLeaf> leafIterator;
        YangLeaf leafInfo;

        // Check whether grouping is the child of rpc.
        assertThat((yangNode.getChild().getChild() instanceof YangGrouping), is(true));
        YangGrouping grouping = (YangGrouping) yangNode.getChild().getChild();

        // Check whether the grouping name is set correctly.
        assertThat(grouping.getName(), is("hello"));

        // Check whether list is the child of grouping.
        assertThat((grouping.getChild() instanceof YangList), is(true));
        YangList yangListNode = (YangList) grouping.getChild();

        // Check whether the list name is set correctly.
        assertThat(yangListNode.getName(), is("valid"));

        leafIterator = yangListNode.getListOfLeaf().listIterator();
        leafInfo = leafIterator.next();

        // Check whether the information in the leaf is correct under list which is under grouping.
        assertThat(leafInfo.getLeafName(), is("invalid-interval"));
        assertThat(leafInfo.getDataType().getDataTypeName(), is("uint16"));
        assertThat(leafInfo.getDataType().getDataType(), is(YangDataTypes.UINT16));
        assertThat(leafInfo.getUnits(), is("\"seconds\""));
        assertThat(leafInfo.getReference(), is("\"RFC 6020\""));

        // Check whether uses is input's child.
        assertThat((yangNode.getChild().getChild().getNextSibling().getChild() instanceof YangUses), is(true));
        YangUses uses = (YangUses) yangNode.getChild().getChild().getNextSibling().getChild();

        // Check whether uses get resolved.
        assertThat(uses.getResolvableStatus(),
                is(ResolvableStatus.RESOLVED));

        // Check whether list is the sibling of uses which has been deep copied from grouping.
        assertThat((yangNode.getChild().getChild().getNextSibling().getChild().getNextSibling() instanceof YangList),
                is(true));
        YangList yangList = (YangList) yangNode.getChild().getChild().getNextSibling().getChild().getNextSibling();

        // Check whether the list name is set correctly.
        assertThat(yangList.getName(), is("valid"));

        leafIterator = yangList.getListOfLeaf().listIterator();
        leafInfo = leafIterator.next();

        // Check whether the information in the leaf is correct under list which is deep copied.
        assertThat(leafInfo.getLeafName(), is("invalid-interval"));
        assertThat(leafInfo.getDataType().getDataTypeName(), is("uint16"));
        assertThat(leafInfo.getDataType().getDataType(), is(YangDataTypes.UINT16));
        assertThat(leafInfo.getUnits(), is("\"seconds\""));
        assertThat(leafInfo.getReference(), is("\"RFC 6020\""));

        // Check whether uses is output's child.
        assertThat((yangNode.getChild().getChild().getNextSibling().getNextSibling().getChild() instanceof YangUses),
                is(true));
        YangUses usesInOuput = (YangUses) yangNode.getChild().getChild().getNextSibling().getNextSibling().getChild();

        // Check whether uses get resolved.
        assertThat(usesInOuput.getResolvableStatus(),
                is(ResolvableStatus.RESOLVED));

        // Check whether list is the sibling of uses which has been deep copied from grouping.
        assertThat((yangNode.getChild().getChild().getNextSibling().getChild().getNextSibling() instanceof YangList),
                is(true));

        YangList yangListInOutput = (YangList) yangNode.getChild().getChild().getNextSibling().getNextSibling()
                .getChild().getNextSibling();

        // Check whether the list name is set correctly.
        assertThat(yangListInOutput.getName(), is("valid"));

        leafIterator = yangListInOutput.getListOfLeaf().listIterator();
        leafInfo = leafIterator.next();

        // Check whether the information in the leaf is correct under list which is deep copied.
        assertThat(leafInfo.getLeafName(), is("invalid-interval"));
        assertThat(leafInfo.getDataType().getDataTypeName(), is("uint16"));
        assertThat(leafInfo.getDataType().getDataType(), is(YangDataTypes.UINT16));
        assertThat(leafInfo.getUnits(), is("\"seconds\""));
        assertThat(leafInfo.getReference(), is("\"RFC 6020\""));
    }

    /**
     * Checks the failure scenario when uses is referring to its own grouping directly.
     */
    @Test
    public void processSelfResolutionGroupingReferencingItselfFailureScenerio()
            throws IOException, ParserException {

        thrown.expect(ParserException.class);
        thrown.expectMessage(
                "YANG file error: Duplicate input identifier detected, same as leaf \"zip-code\"");
        YangNode node = manager
                .getDataModel("src/test/resources/SelfResolutionGroupingReferencingItselfFailureScenerio.yang");

    }

    /**
     * Checks the when multiple uses are present and are referred to the grouping at different levels.
     */
    @Test
    public void processSelfResolutionGroupingWithMultipleUses()
            throws IOException, ParserException {

        YangNode node = manager
                .getDataModel("src/test/resources/SelfResolutionGroupingWithMultipleUses.yang");

        // Check whether the data model tree returned is of type module.
        assertThat((node instanceof YangModule), is(true));

        // Check whether the node type is set properly to module.
        assertThat(node.getNodeType(), is(YangNodeType.MODULE_NODE));

        // Check whether the module name is set correctly.
        YangModule yangNode = (YangModule) node;
        assertThat(yangNode.getName(), is("Test"));

        ListIterator<YangLeaf> leafIterator;
        YangLeaf leafInfo;

        // Check whether grouping is the child of container.
        assertThat((yangNode.getChild().getChild() instanceof YangGrouping), is(true));
        YangGrouping grouping = (YangGrouping) yangNode.getChild().getChild();

        // Check whether the grouping name is set correctly.
        assertThat(grouping.getName(), is("endpoint"));

        // Check whether uses is endpoint-grouping's child.
        assertThat((grouping.getChild() instanceof YangUses), is(true));
        YangUses firstUses = (YangUses) grouping.getChild();

        // Check whether uses get resolved.
        assertThat(firstUses.getResolvableStatus(),
                is(ResolvableStatus.RESOLVED));

        // Check whether container is the sibling of uses.
        assertThat((firstUses.getNextSibling() instanceof YangContainer), is(true));
        YangContainer yangContainer = (YangContainer) firstUses.getNextSibling();

        // Check whether the container name is set correctly.
        assertThat(yangContainer.getName(), is("design"));

        leafIterator = yangContainer.getListOfLeaf().listIterator();
        leafInfo = leafIterator.next();

        // Check whether the information in the leaf is correct under design-container.
        assertThat(leafInfo.getLeafName(), is("ink"));
        assertThat(leafInfo.getDataType().getDataTypeName(), is("int32"));
        assertThat(leafInfo.getDataType().getDataType(), is(YangDataTypes.INT32));

        // Check whether uses is design-container's child.
        assertThat((yangContainer.getChild() instanceof YangUses), is(true));
        YangUses secondUses = (YangUses) yangContainer.getChild();

        // Check whether uses get resolved.
        assertThat(secondUses.getResolvableStatus(),
                is(ResolvableStatus.RESOLVED));

        // Check whether container is the sibling of uses.
        assertThat((secondUses.getNextSibling() instanceof YangContainer), is(true));
        YangContainer yangContainer2 = (YangContainer) secondUses.getNextSibling();
        assertThat(yangContainer2.getName(), is("correct"));

        leafIterator = yangContainer2.getListOfLeaf().listIterator();
        leafInfo = leafIterator.next();

        // Check whether the information in the leaf is correct under correct-container.
        assertThat(leafInfo.getLeafName(), is("newone"));
        assertThat(leafInfo.getDataType().getDataTypeName(), is("string"));
        assertThat(leafInfo.getDataType().getDataType(), is(YangDataTypes.STRING));

        // Check whether uses is correct container's child.
        assertThat((yangContainer2.getChild() instanceof YangUses), is(true));
        YangUses thirdUses = (YangUses) yangContainer2.getChild();

        // Check whether uses get resolved.
        assertThat(thirdUses.getResolvableStatus(),
                is(ResolvableStatus.RESOLVED));

        // Check whether container is the sibling of uses.
        assertThat((thirdUses.getNextSibling() instanceof YangContainer), is(true));

        YangContainer yangContainer3 = (YangContainer) thirdUses.getNextSibling();
        assertThat(yangContainer3.getName(), is("value"));

        leafIterator = yangContainer3.getListOfLeaf().listIterator();
        leafInfo = leafIterator.next();

        // Check whether the information in the leaf is correct under container
        // which has been deep copied from grouping.
        assertThat(leafInfo.getLeafName(), is("zip-code"));
        assertThat(leafInfo.getDataType().getDataTypeName(), is("string"));
        assertThat(leafInfo.getDataType().getDataType(), is(YangDataTypes.STRING));

        // Check whether uses is the sibling of container-design.
        assertThat((yangContainer.getNextSibling() instanceof YangUses), is(true));
        YangUses fourthUses = (YangUses) yangContainer.getNextSibling();

        // Check whether uses get resolved.
        assertThat(fourthUses.getResolvableStatus(),
                is(ResolvableStatus.RESOLVED));

        // Check whether uses is the sibling of previous uses.
        assertThat((fourthUses.getNextSibling() instanceof YangUses), is(true));
        YangUses fifthUses = (YangUses) fourthUses.getNextSibling();

        // Check whether uses get resolved.
        assertThat(fifthUses.getResolvableStatus(),
                is(ResolvableStatus.RESOLVED));

        // Check whether list is the sibling of uses.
        assertThat((fifthUses.getNextSibling() instanceof YangList), is(true));
        YangList yangList = (YangList) fifthUses.getNextSibling();
        assertThat(yangList.getName(), is("valid"));

        leafIterator = yangList.getListOfLeaf().listIterator();
        leafInfo = leafIterator.next();

        // Check whether the information in the leaf is correct under list which has been deep copied from grouping.
        assertThat(leafInfo.getLeafName(), is("invalid-interval"));
        assertThat(leafInfo.getDataType().getDataTypeName(), is("uint16"));
        assertThat(leafInfo.getDataType().getDataType(), is(YangDataTypes.UINT16));
        assertThat(leafInfo.getUnits(), is("\"seconds\""));
        assertThat(leafInfo.getReference(), is("\"RFC 6020\""));

        // Check whether typedef is the sibling of list.
        assertThat((yangList.getNextSibling() instanceof YangTypeDef), is(true));
        YangTypeDef yangTypeDef = (YangTypeDef) yangList.getNextSibling();
        assertThat(yangTypeDef.getName(), is("my-type"));

        leafIterator = grouping.getListOfLeaf().listIterator();
        leafInfo = leafIterator.next();

        // Check whether the information in the leaf is correct under grouping.
        assertThat(leafInfo.getLeafName(), is("zip-code"));
        assertThat(leafInfo.getDataType().getDataTypeName(), is("string"));
        assertThat(leafInfo.getDataType().getDataType(), is(YangDataTypes.STRING));

        // Check whether uses is endpoint-grouping's sibling.
        assertThat((grouping.getNextSibling() instanceof YangUses), is(true));
        YangUses endpointUses = (YangUses) grouping.getNextSibling();

        // Check whether uses get resolved.
        assertThat(endpointUses.getResolvableStatus(),
                is(ResolvableStatus.RESOLVED));

        assertThat((endpointUses.getNextSibling().getNextSibling().getNextSibling().getNextSibling().getNextSibling()
                .getNextSibling() instanceof YangUses), is(true));

        YangUses yangUsesInEndpoint = (YangUses) endpointUses.getNextSibling().getNextSibling().getNextSibling()
                .getNextSibling().getNextSibling().getNextSibling();
        assertThat(yangUsesInEndpoint.getResolvableStatus(),
                is(ResolvableStatus.RESOLVED));

        assertThat((yangUsesInEndpoint.getNextSibling() instanceof YangContainer), is(true));
        YangContainer yangContainerInEndPoint = (YangContainer) yangUsesInEndpoint.getNextSibling();

        assertThat(yangContainerInEndPoint.getName(), is("design"));
    }

    /**
     * Checks the failure scenario when uses is present under the same node many times.
     */
    @Test
    public void processSelfResolutionGroupingHavingSameUsesManyTimes()
            throws IOException, ParserException {

        thrown.expect(ParserException.class);
        thrown.expectMessage(
                "YANG file error: Duplicate input identifier detected, same as uses \"failure\"");
        YangNode node = manager
                .getDataModel("src/test/resources/SelfResolutionGroupingHavingSameUsesManyTimes.yang");
    }

    /**
     * Checks the rpc having both typedef and grouping.
     * It also checks that the grouping under different nodes will not give any problem in resolving uses.
     */
    @Test
    public void processSelfResolutionRpcWithOneTypedefAndTwoGroupingUnderDifferentNode()
            throws IOException, ParserException {

        YangNode node = manager
                .getDataModel(
                        "src/test/resources/SelfResolutionRpcWithOneTypedefAndTwoGroupingUnderDifferentNode.yang");

        // Check whether the data model tree returned is of type module.
        assertThat((node instanceof YangModule), is(true));

        // Check whether the node type is set properly to module.
        assertThat(node.getNodeType(), is(YangNodeType.MODULE_NODE));

        // Check whether the module name is set correctly.
        YangModule yangNode = (YangModule) node;
        assertThat(yangNode.getName(), is("rock"));

        ListIterator<YangLeaf> leafIterator;
        YangLeaf leafInfo;

        // Check whether grouping is the child of input.
        assertThat((yangNode.getChild().getChild().getChild() instanceof YangGrouping), is(true));
        YangGrouping groupingUnderInput = (YangGrouping) yangNode.getChild().getChild().getChild();

        // Check whether the grouping name is set correctly.
        assertThat(groupingUnderInput.getName(), is("creative"));

        leafIterator = groupingUnderInput.getListOfLeaf().listIterator();
        leafInfo = leafIterator.next();

        // Check whether the information in the leaf is correct under grouping.
        assertThat(leafInfo.getLeafName(), is("carry"));
        assertThat(leafInfo.getDataType().getDataTypeName(), is("string"));
        assertThat(leafInfo.getDataType().getDataType(), is(YangDataTypes.STRING));

        // Check whether grouping is the child of output.
        assertThat((yangNode.getChild().getChild().getNextSibling().getChild() instanceof YangGrouping), is(true));
        YangGrouping grouping = (YangGrouping) yangNode.getChild().getChild().getNextSibling().getChild();
        assertThat(grouping.getName(), is("creative"));

        // Check whether typedef is the sibling of grouping.
        assertThat((grouping.getNextSibling() instanceof YangTypeDef), is(true));

        YangTypeDef typedef = (YangTypeDef) grouping.getNextSibling();
        assertThat(typedef.getName(), is("my-type"));

        // Check whether uses is the sibling of typedef.
        assertThat((typedef.getNextSibling() instanceof YangUses), is(true));

        // Check whether uses get resolved.
        YangUses uses = (YangUses) typedef.getNextSibling();
        assertThat(uses.getName(), is("creative"));
        assertThat(uses.getResolvableStatus(),
                is(ResolvableStatus.RESOLVED));

        // Check whether list is the sibling of uses.
        assertThat((uses.getNextSibling() instanceof YangList), is(true));
        YangList list = (YangList) uses.getNextSibling();
        assertThat(list.getName(), is("valid"));

        leafIterator = list.getListOfLeaf().listIterator();
        leafInfo = leafIterator.next();

        // Check whether the information in the leaf is correct under list.
        assertThat(leafInfo.getLeafName(), is("invalid-interval"));
        assertThat(leafInfo.getDataType().getDataTypeName(), is("uint16"));
        assertThat(leafInfo.getDataType().getDataType(), is(YangDataTypes.UINT16));
    }

    /**
     * Checks the failure scenario when uses is cannot resolve its grouping.
     */
    @Test
    public void processSelfResolutionNestedGroupingWithUnresolvedUses()
            throws IOException, ParserException {

        thrown.expect(ParserException.class);
        thrown.expectMessage(
                "YANG file error: Unable to find base typedef/grouping for given type/uses");

        YangNode node = manager
                .getDataModel("src/test/resources/SelfResolutionNestedGroupingWithUnresolvedUses.yang");
    }

    /**
     * Checks self resolution when typedef hierarchical references are present
     * with last type is unresolved.
     */
    @Test
    public void processSelfFileLinkingWithGroupingHierarchicalRefUnresolved()
            throws IOException, ParserException {

        YangNode node = manager
                .getDataModel("src/test/resources/SelfFileLinkingWithGroupingHierarchicalRefUnresolved.yang");

        // Check whether the data model tree returned is of type module.
        assertThat((node instanceof YangModule), is(true));

        // Check whether the node type is set properly to module.
        assertThat(node.getNodeType(), is(YangNodeType.MODULE_NODE));

        // Check whether the module name is set correctly.
        YangModule yangNode = (YangModule) node;
        assertThat(yangNode.getName(), is("Test"));

        // Check whether container is the sibling of grouping.
        assertThat((yangNode.getChild().getNextSibling() instanceof YangContainer), is(true));
        YangContainer containerWithUses = (YangContainer) yangNode.getChild().getNextSibling();
        assertThat(containerWithUses.getName(), is("test"));

        // Check whether uses is the child of container.
        assertThat((containerWithUses.getChild() instanceof YangUses), is(true));
        YangUses uses = (YangUses) containerWithUses.getChild();
        assertThat(uses.getName(), is("create"));

        // Check whether uses is getting resolved.
        assertThat(uses.getResolvableStatus(),
                is(ResolvableStatus.RESOLVED));

        // Check whether grouping is the child of module.
        assertThat((yangNode.getChild() instanceof YangGrouping), is(true));
        YangGrouping groupingWithUses = (YangGrouping) yangNode.getChild();
        assertThat(groupingWithUses.getName(), is("create"));

        // Check whether uses with prefix from from other file, is the child of grouping.
        assertThat((groupingWithUses.getChild() instanceof YangUses), is(true));
        YangUses uses1 = (YangUses) groupingWithUses.getChild();
        assertThat(uses1.getName(), is("valid"));

        // Check whether this uses is getting intra-file-resolved.
        assertThat(uses1.getResolvableStatus(),
                is(ResolvableStatus.INTRA_FILE_RESOLVED));
    }

    /**
     * Checks self resolution when uses has prefix of self module.
     */
    @Test
    public void processSelfFileLinkingWithGroupingWithSelfModulePrefix()
            throws IOException, ParserException {

        YangNode node = manager.getDataModel("src/test/resources/SelfFileLinkingWithGroupingWithSelfModulePrefix.yang");

        // Check whether the data model tree returned is of type module.
        assertThat((node instanceof YangModule), is(true));

        // Check whether the node type is set properly to module.
        assertThat(node.getNodeType(), is(YangNodeType.MODULE_NODE));

        // Check whether the module name is set correctly.
        YangModule yangNode = (YangModule) node;
        assertThat(yangNode.getName(), is("Test"));

        // Check whether container is the sibling of grouping.
        YangContainer yangContainer = (YangContainer) node.getChild().getNextSibling();

        // Check whether list is the child of container.
        YangList yangList = (YangList) yangContainer.getChild();

        // Check whether uses is the child of list.
        assertThat((yangList.getChild() instanceof YangUses), is(true));
        YangUses yangUses1 = (YangUses) yangList.getChild();
        assertThat(yangUses1.getName(), is("FirstClass"));

        // Check whether uses is getting resolved.
        assertThat(yangUses1.getResolvableStatus(),
                is(ResolvableStatus.RESOLVED));

        // Check whether grouping is the sibling of uses.
        YangGrouping yangGrouping1 = (YangGrouping) yangUses1.getNextSibling();
        assertThat(yangGrouping1.getName(), is("FirstClass"));

        // Check whether uses is the child of grouping.
        YangUses yangUses2 = (YangUses) yangGrouping1.getChild();
        assertThat(yangUses2.getName(), is("PassingClass"));

        // Check the uses gets resolved.
        assertThat(yangUses2.getResolvableStatus(),
                is(ResolvableStatus.RESOLVED));

        // Check whether grouping is the sibling of list.
        YangGrouping yangGrouping2 = (YangGrouping) yangList.getNextSibling();
        assertThat(yangGrouping2.getName(), is("PassingClass"));

        // Check uses is the child of that grouping which has prefix of the same module.
        YangUses yangUses3 = (YangUses) yangGrouping2.getChild();
        assertThat(yangUses3.getName(), is("Percentage"));

        // Check uses is getting resolved.
        assertThat(yangUses3.getResolvableStatus(),
                is(ResolvableStatus.RESOLVED));

        // Check grouping is the child of module.
        YangGrouping yangGrouping3 = (YangGrouping) node.getChild();

        ListIterator<YangLeaf> leafIterator;
        YangLeaf leafInfo;
        leafIterator = yangGrouping3.getListOfLeaf().listIterator();
        leafInfo = leafIterator.next();

        // Check whether the information in the leaf is correct under grouping.
        assertThat(leafInfo.getLeafName(), is("hello"));
        assertThat(leafInfo.getDataType().getDataTypeName(), is("String"));
        assertThat(leafInfo.getDataType().getDataType(), is(YangDataTypes.STRING));

    }

    /**
     * Checks self resolution when some type uses prefix of self module
     * some uses external prefix.
     */
    @Test
    public void processSelfFileLinkingWithGroupingWithSelfAndExternalPrefixMix()
            throws IOException, ParserException {

        YangNode node = manager
                .getDataModel("src/test/resources/SelfFileLinkingWithGroupingWithSelfAndExternalPrefixMix.yang");

        // Check whether the data model tree returned is of type module.
        assertThat((node instanceof YangModule), is(true));

        // Check whether the node type is set properly to module.
        assertThat(node.getNodeType(), is(YangNodeType.MODULE_NODE));

        // Check whether the module name is set correctly.
        YangModule yangNode = (YangModule) node;
        assertThat(yangNode.getName(), is("Test"));

        // Check whether container is the sibling of grouping.
        YangContainer yangContainer = (YangContainer) node.getChild().getNextSibling();

        // Check whether list is the child of container.
        YangList yangList = (YangList) yangContainer.getChild();

        // Check whether uses is the child of list.
        assertThat((yangList.getChild() instanceof YangUses), is(true));
        YangUses yangUses1 = (YangUses) yangList.getChild();
        assertThat(yangUses1.getName(), is("FirstClass"));

        // Check whether uses is getting resolved.
        assertThat(yangUses1.getResolvableStatus(),
                is(ResolvableStatus.RESOLVED));

        // Check whether grouping is the sibling of uses.
        YangGrouping yangGrouping1 = (YangGrouping) yangUses1.getNextSibling();
        assertThat(yangGrouping1.getName(), is("FirstClass"));

        // Check whether uses is the child of grouping which has prefix from other module.
        YangUses yangUses2 = (YangUses) yangGrouping1.getChild();
        assertThat(yangUses2.getName(), is("PassingClass"));

        // Check whether uses gets intra-file-resolved.
        assertThat(yangUses2.getResolvableStatus(),
                is(ResolvableStatus.INTRA_FILE_RESOLVED));

        // Check whether grouping is the sibling of list.
        YangGrouping yangGrouping2 = (YangGrouping) yangList.getNextSibling();
        assertThat(yangGrouping2.getName(), is("PassingClass"));

        // Check uses is the child of that grouping which has prefix of the same module.
        YangUses yangUses3 = (YangUses) yangGrouping2.getChild();
        assertThat(yangUses3.getName(), is("Percentage"));

        // Check uses is getting resolved.
        assertThat(yangUses3.getResolvableStatus(),
                is(ResolvableStatus.RESOLVED));

        // Check grouping is the child of module.
        YangGrouping yangGrouping3 = (YangGrouping) node.getChild();
        ListIterator<YangLeaf> leafIterator;
        YangLeaf leafInfo;
        leafIterator = yangGrouping3.getListOfLeaf().listIterator();
        leafInfo = leafIterator.next();

        // Check whether the information in the leaf is correct under grouping.
        assertThat(leafInfo.getLeafName(), is("hello"));
        assertThat(leafInfo.getDataType().getDataTypeName(), is("String"));
        assertThat(leafInfo.getDataType().getDataType(), is(YangDataTypes.STRING));
    }

}

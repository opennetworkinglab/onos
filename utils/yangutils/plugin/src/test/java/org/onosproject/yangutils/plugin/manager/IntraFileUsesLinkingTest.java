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

package org.onosproject.yangutils.plugin.manager;

import java.io.IOException;
import java.util.ListIterator;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.onosproject.yangutils.datamodel.YangContainer;
import org.onosproject.yangutils.datamodel.YangGrouping;
import org.onosproject.yangutils.datamodel.YangInput;
import org.onosproject.yangutils.datamodel.YangLeaf;
import org.onosproject.yangutils.datamodel.YangList;
import org.onosproject.yangutils.datamodel.YangModule;
import org.onosproject.yangutils.datamodel.YangNode;
import org.onosproject.yangutils.datamodel.YangNodeType;
import org.onosproject.yangutils.datamodel.YangTypeDef;
import org.onosproject.yangutils.datamodel.YangUses;
import org.onosproject.yangutils.datamodel.utils.ResolvableStatus;
import org.onosproject.yangutils.datamodel.utils.builtindatatype.YangDataTypes;
import org.onosproject.yangutils.linker.exceptions.LinkerException;
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

        ListIterator<YangLeaf> leafIterator = yangNode.getListOfLeaf().listIterator();
        YangLeaf leafInfo = leafIterator.next();

        // Check whether the information in the leaf is correct under module.
        assertThat(leafInfo.getName(), is("hello"));
        assertThat(leafInfo.getDataType().getDataTypeName(), is("string"));
        assertThat(leafInfo.getDataType().getDataType(), is(YangDataTypes.STRING));

        // Check whether grouping is the sibling of module's child.
        assertThat((yangNode.getChild().getNextSibling() instanceof YangGrouping), is(true));

        YangGrouping grouping = (YangGrouping) yangNode.getChild().getNextSibling();
        leafIterator = grouping.getListOfLeaf().listIterator();
        leafInfo = leafIterator.next();

        // Check whether the information in the leaf is correct under grouping.
        assertThat(leafInfo.getName(), is("hello"));
        assertThat(leafInfo.getDataType().getDataTypeName(), is("string"));
        assertThat(leafInfo.getDataType().getDataType(), is(YangDataTypes.STRING));

        // Check whether uses is module's child.
        assertThat((yangNode.getChild() instanceof YangUses), is(true));
        YangUses uses = (YangUses) yangNode.getChild();

        // Check whether uses get resolved
        assertThat(uses.getResolvableStatus(),
                is(ResolvableStatus.RESOLVED));
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

        ListIterator<YangLeaf> leafIterator1 = yangNode.getListOfLeaf().listIterator();
        YangLeaf leafInfo1 = leafIterator1.next();

        // Check whether the information in the leaf is correct under module.
        assertThat(leafInfo1.getName(), is("treat"));
        assertThat(leafInfo1.getDataType().getDataTypeName(), is("string"));
        assertThat(leafInfo1.getDataType().getDataType(), is(YangDataTypes.STRING));

        YangContainer container = (YangContainer) yangNode.getChild().getNextSibling().getNextSibling();

        // Check whether the container name is set correctly which is under module.
        assertThat(container.getName(), is("test"));

        leafIterator = container.getListOfLeaf().listIterator();
        leafInfo = leafIterator.next();

        // Check whether the information in the leaf is correct under container which is under module.
        assertThat(leafInfo.getName(), is("leaf2"));
        assertThat(leafInfo.getDataType().getDataTypeName(), is("string"));
        assertThat(leafInfo.getDataType().getDataType(), is(YangDataTypes.STRING));

        // Check whether grouping is the sibling of module's child.
        assertThat((yangNode.getChild().getNextSibling() instanceof YangGrouping), is(true));

        YangGrouping grouping = (YangGrouping) yangNode.getChild().getNextSibling();
        leafIterator = grouping.getListOfLeaf().listIterator();
        leafInfo = leafIterator.next();

        // Check whether the information in the leaf is correct under grouping.
        assertThat(leafInfo.getName(), is("treat"));
        assertThat(leafInfo.getDataType().getDataTypeName(), is("string"));
        assertThat(leafInfo.getDataType().getDataType(), is(YangDataTypes.STRING));

        // Check whether container is the child of grouping.
        assertThat((grouping.getChild() instanceof YangContainer), is(true));
        container = (YangContainer) grouping.getChild();

        // Check whether the container name is set correctly which is under grouping.
        assertThat(container.getName(), is("test"));

        leafIterator = container.getListOfLeaf().listIterator();
        leafInfo = leafIterator.next();

        // Check whether the information in the leaf is correct under container which is under grouping.
        assertThat(leafInfo.getName(), is("leaf2"));
        assertThat(leafInfo.getDataType().getDataTypeName(), is("string"));
        assertThat(leafInfo.getDataType().getDataType(), is(YangDataTypes.STRING));

        // Check whether uses is module's child.
        assertThat((yangNode.getChild() instanceof YangUses), is(true));
        YangUses uses = (YangUses) yangNode.getChild();

        // Check whether uses get resolved.
        assertThat(uses.getResolvableStatus(),
                is(ResolvableStatus.RESOLVED));

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
        assertThat(leafInfo.getName(), is("invalid-interval"));
        assertThat(leafInfo.getDataType().getDataTypeName(), is("uint16"));
        assertThat(leafInfo.getDataType().getDataType(), is(YangDataTypes.UINT16));
        assertThat(leafInfo.getUnits(), is("\"seconds\""));
        assertThat(leafInfo.getReference(), is("\"RFC 6020\""));

        // Check whether uses is input's child.
        assertThat((yangNode.getChild().getChild().getNextSibling().getChild() instanceof YangUses), is(true));
        YangUses uses = (YangUses) yangNode.getChild().getChild().getNextSibling().getChild();

        // Check whether uses get resolved.
        assertThat(uses.getResolvableStatus(), is(ResolvableStatus.RESOLVED));

        YangInput inputNode = ((YangInput) yangNode.getChild().getChild().getNextSibling());
        assertThat((inputNode.getChild() instanceof YangUses), is(true));

        YangList yangList = ((YangList) inputNode.getChild().getNextSibling());
        assertThat(yangList.getName(), is("valid"));

        leafIterator = yangList.getListOfLeaf().listIterator();
        leafInfo = leafIterator.next();

        // Check whether the information in the leaf is correct under list which is deep copied.
        assertThat(leafInfo.getName(), is("invalid-interval"));
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
            throws IOException {

        thrown.expect(LinkerException.class);
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
        assertThat(firstUses.getResolvableStatus(), is(ResolvableStatus.RESOLVED));

        // Validate first uses child is cloned properly
        assertThat((firstUses.getNextSibling().getNextSibling()
                .getNextSibling().getNextSibling() instanceof YangList), is(true));
        YangList firstUsesChild = ((YangList) firstUses.getNextSibling().getNextSibling().getNextSibling()
                .getNextSibling());
        assertThat(firstUsesChild.getName(), is("valid"));

        leafIterator = firstUsesChild.getListOfLeaf().listIterator();
        leafInfo = leafIterator.next();

        // Check whether the information in the leaf is correct under list which has been deep copied from grouping.
        assertThat(leafInfo.getName(), is("invalid-interval"));
        assertThat(leafInfo.getDataType().getDataTypeName(), is("uint16"));
        assertThat(leafInfo.getDataType().getDataType(), is(YangDataTypes.UINT16));
        assertThat(leafInfo.getUnits(), is("\"seconds\""));
        assertThat(leafInfo.getReference(), is("\"RFC 6020\""));

        //validate uses second
        assertThat((firstUses.getNextSibling() instanceof YangContainer), is(true));
        YangContainer container = (YangContainer) firstUses.getNextSibling();
        assertThat(container.getName(), is("design"));

        assertThat((container.getChild() instanceof YangUses), is(true));
        assertThat((container.getListOfLeaf().iterator().next().getName()), is("ink"));

        //validate uses third
        assertThat((container.getChild().getNextSibling() instanceof YangContainer), is(true));
        YangContainer container2 = ((YangContainer) container.getChild().getNextSibling());
        assertThat(container2.getName(), is("correct"));
        assertThat((container2.getChild() instanceof YangUses), is(true));
        assertThat((container2.getChild().getNextSibling() instanceof YangContainer), is(true));
        YangContainer thirdUsesChild = ((YangContainer) container2.getChild().getNextSibling());
        assertThat(thirdUsesChild.getListOfLeaf().iterator().next().getName(), is("zip-code"));

        //validate fourth uses
        assertThat((firstUses.getNextSibling().getNextSibling() instanceof YangUses), is(true));
        YangUses fourthUses = ((YangUses) firstUses.getNextSibling().getNextSibling());
        assertThat((fourthUses.getNextSibling().getNextSibling().getNextSibling() instanceof YangTypeDef),
                is(true));
        assertThat(fourthUses.getNextSibling().getNextSibling().getNextSibling().getName(), is("my-type"));

        //validate fifth uses
        assertThat((firstUses.getNextSibling().getNextSibling().getNextSibling() instanceof YangUses),
                is(true));

        //validate end point uses
        assertThat(grouping.getNextSibling() instanceof YangUses, is(true));
        assertThat(grouping.getNextSibling().getNextSibling().getNextSibling().getNextSibling()
                        .getNextSibling().getNextSibling().getNextSibling().getNextSibling() instanceof YangContainer,
                is(true));
        container = (YangContainer) grouping.getNextSibling().getNextSibling().getNextSibling().getNextSibling()
                .getNextSibling().getNextSibling().getNextSibling().getNextSibling();
        assertThat(container.getName(), is("design"));
        container2 = (YangContainer) container.getChild().getNextSibling();
        assertThat(container2.getName(), is("correct"));
        assertThat(container2.getChild().getNextSibling().getName(), is("value"));
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
        assertThat(leafInfo.getName(), is("carry"));
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
    }

    /**
     * Checks the failure scenario when uses is cannot resolve its grouping.
     */
    @Test
    public void processSelfResolutionNestedGroupingWithUnresolvedUses()
            throws IOException, LinkerException {

        thrown.expect(LinkerException.class);
        thrown.expectMessage(
                "YANG file error: Unable to find base grouping for given uses");

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
        assertThat(leafInfo.getName(), is("hello"));
        assertThat(leafInfo.getDataType().getDataTypeName(), is("string"));
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
        assertThat(leafInfo.getName(), is("hello"));
        assertThat(leafInfo.getDataType().getDataTypeName(), is("string"));
        assertThat(leafInfo.getDataType().getDataType(), is(YangDataTypes.STRING));
    }

}

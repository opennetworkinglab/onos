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
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.onosproject.yangutils.datamodel.YangAtomicPath;
import org.onosproject.yangutils.datamodel.YangContainer;
import org.onosproject.yangutils.datamodel.utils.builtindatatype.YangDataTypes;
import org.onosproject.yangutils.datamodel.YangInput;
import org.onosproject.yangutils.datamodel.YangLeaf;
import org.onosproject.yangutils.datamodel.YangLeafList;
import org.onosproject.yangutils.datamodel.YangLeafRef;
import org.onosproject.yangutils.datamodel.YangList;
import org.onosproject.yangutils.datamodel.YangModule;
import org.onosproject.yangutils.datamodel.YangNode;
import org.onosproject.yangutils.datamodel.YangNodeType;
import org.onosproject.yangutils.datamodel.YangPathArgType;
import org.onosproject.yangutils.datamodel.YangPathOperator;
import org.onosproject.yangutils.datamodel.YangPathPredicate;
import org.onosproject.yangutils.datamodel.YangRelativePath;
import org.onosproject.yangutils.datamodel.utils.ResolvableStatus;
import org.onosproject.yangutils.linker.exceptions.LinkerException;
import org.onosproject.yangutils.parser.exceptions.ParserException;
import org.onosproject.yangutils.parser.impl.YangUtilsParserManager;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;

/**
 * Test cases for testing leafref intra file linking.
 */
public class IntraFileLeafrefLinkingTest {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    private final YangUtilsParserManager manager = new YangUtilsParserManager();

    /**
     * Checks self resolution when leafref under module refers to leaf in container.
     */
    @Test
    public void processSelfResolutionWhenLeafrefReferToContainerLeaf()
            throws IOException, ParserException {

        YangNode node = manager.getDataModel("src/test/resources/SelfResolutionWhenLeafrefReferToContainerLeaf.yang");

        // Check whether the data model tree returned is of type module.
        assertThat((node instanceof YangModule), is(true));

        // Check whether the node type is set properly to module.
        assertThat(node.getNodeType(), is(YangNodeType.MODULE_NODE));

        // Check whether the module name is set correctly.
        YangModule yangNode = (YangModule) node;
        assertThat(yangNode.getName(), is("ietf-network"));

        ListIterator<YangLeaf> leafIterator;
        YangLeaf leafInfo;

        leafIterator = yangNode.getListOfLeaf().listIterator();
        leafInfo = leafIterator.next();

        // Check whether the information in the leaf is correct.
        assertThat(leafInfo.getName(), is("network-ref"));
        assertThat(leafInfo.getDataType().getDataTypeName(), is("leafref"));
        assertThat(leafInfo.getDataType().getDataType(), is(YangDataTypes.LEAFREF));
        YangLeafRef leafref = (YangLeafRef) (leafInfo.getDataType().getDataTypeExtendedInfo());

        // Check whether leafref type got resolved.
        assertThat(leafref.getResolvableStatus(),
                is(ResolvableStatus.RESOLVED));

        // Check the effective type for the leaf.
        assertThat(leafref.getEffectiveDataType().getDataType(),
                is(YangDataTypes.UINT8));
    }

    /**
     * Checks self resolution when leafref under module refers to leaf in input of rpc.
     */
    @Test
    public void processSelfResolutionWhenLeafrefInModuleReferToLeafInInputOfRpc()
            throws IOException, ParserException {

        YangNode node = manager
                .getDataModel("src/test/resources/SelfResolutionWhenLeafrefInModuleReferToLeafInInputOfRpc.yang");

        // Check whether the data model tree returned is of type module.
        assertThat((node instanceof YangModule), is(true));

        // Check whether the node type is set properly to module.
        assertThat(node.getNodeType(), is(YangNodeType.MODULE_NODE));

        // Check whether the module name is set correctly.
        YangModule yangNode = (YangModule) node;
        assertThat(yangNode.getName(), is("ietf-network"));

        ListIterator<YangLeaf> leafIterator;
        YangLeaf leafInfo;

        leafIterator = yangNode.getListOfLeaf().listIterator();
        leafInfo = leafIterator.next();

        // Check whether the information in the leaf is correct.
        assertThat(leafInfo.getName(), is("network-ref"));
        assertThat(leafInfo.getDataType().getDataTypeName(), is("leafref"));
        assertThat(leafInfo.getDataType().getDataType(), is(YangDataTypes.LEAFREF));
        YangLeafRef leafref = (YangLeafRef) (leafInfo.getDataType().getDataTypeExtendedInfo());

        // Check whether leafref type got resolved.
        assertThat(leafref.getResolvableStatus(),
                is(ResolvableStatus.RESOLVED));

        // Check the effective type for the leaf.
        assertThat(leafref.getEffectiveDataType().getDataType(),
                is(YangDataTypes.UINT8));
    }

    /**
     * Checks self resolution when leafref under module refers to grouping rpc with input as name.
     * Rpc has input child also. So here the node search must be done by taking input node.
     */
    @Test
    public void processSelfResolutionWhenLeafrefInModuleReferToGroupingWithInputInRpc()
            throws IOException, ParserException {

        YangNode node = manager
                .getDataModel("src/test/resources/SelfResolutionWhenLeafrefInModuleReferToGroupingWithInputInRpc.yang");

        // Check whether the data model tree returned is of type module.
        assertThat((node instanceof YangModule), is(true));

        // Check whether the node type is set properly to module.
        assertThat(node.getNodeType(), is(YangNodeType.MODULE_NODE));

        // Check whether the module name is set correctly.
        YangModule yangNode = (YangModule) node;
        assertThat(yangNode.getName(), is("ietf-network"));

        ListIterator<YangLeaf> leafIterator;
        YangLeaf leafInfo;

        leafIterator = yangNode.getListOfLeaf().listIterator();
        leafInfo = leafIterator.next();

        // Check whether the information in the leaf is correct.
        assertThat(leafInfo.getName(), is("network-ref"));
        assertThat(leafInfo.getDataType().getDataTypeName(), is("leafref"));
        assertThat(leafInfo.getDataType().getDataType(), is(YangDataTypes.LEAFREF));
        YangLeafRef leafref = (YangLeafRef) (leafInfo.getDataType().getDataTypeExtendedInfo());

        // Check whether leafref type got resolved.
        assertThat(leafref.getResolvableStatus(),
                is(ResolvableStatus.RESOLVED));

        // Check the effective type for the leaf.
        assertThat(leafref.getEffectiveDataType().getDataType(),
                is(YangDataTypes.UINT8));
    }

    /**
     * Checks self resolution when leafref under module refers to grouping under module.
     * Grouping/typedef cannot be referred.
     */
    @Test
    public void processSelfResolutionWhenLeafrefInModuleReferToGrouping()
            throws IOException, ParserException {

        thrown.expect(LinkerException.class);
        thrown.expectMessage(
                "YANG file error: The target node of leafref is invalid.");
        YangNode node = manager
                .getDataModel("src/test/resources/SelfResolutionWhenLeafrefInModuleReferToGroupingInModule.yang");
    }

    /**
     * Checks self resolution error scenerio where leafref is without path.
     */
    @Test
    public void processSelfResolutionWhenLeafrefDoesntHavePath()
            throws IOException, ParserException {

        thrown.expect(ParserException.class);
        thrown.expectMessage(
                "YANG file error : a type leafref must have one path statement.");
        YangNode node = manager
                .getDataModel("src/test/resources/SelfResolutionWhenLeafrefDoesntHavePath.yang");
    }

    /**
     * Checks self resolution when leafref under module refers to invalid node.
     * Inter file linking also has to be done to know the error message.
     */
    @Test
    public void processSelfResolutionWhenLeafrefInModuleReferToInvalidNode()
            throws IOException, ParserException {

        YangNode node = manager
                .getDataModel("src/test/resources/SelfResolutionWhenLeafrefInModuleReferToInvalidNode.yang");
        // Check whether the data model tree returned is of type module.
        assertThat((node instanceof YangModule), is(true));

        // Check whether the node type is set properly to module.
        assertThat(node.getNodeType(), is(YangNodeType.MODULE_NODE));

        // Check whether the module name is set correctly.
        YangModule yangNode = (YangModule) node;
        assertThat(yangNode.getName(), is("ietf-network"));

        ListIterator<YangLeaf> leafIterator;
        YangLeaf leafInfo;

        leafIterator = yangNode.getListOfLeaf().listIterator();
        leafInfo = leafIterator.next();

        // Check whether the information in the leaf is correct.
        assertThat(leafInfo.getName(), is("network-ref"));
        assertThat(leafInfo.getDataType().getDataTypeName(), is("leafref"));
        assertThat(leafInfo.getDataType().getDataType(), is(YangDataTypes.LEAFREF));
        YangLeafRef leafref = (YangLeafRef) (leafInfo.getDataType().getDataTypeExtendedInfo());

        // Check whether leafref type got intra file resolved.
        assertThat(leafref.getResolvableStatus(),
                is(ResolvableStatus.INTRA_FILE_RESOLVED));
    }

    /**
     * Checks self resolution when leafref under module refers to invalid node.
     * Inter file linking also has to be done to know the error message.
     */
    @Test
    public void processSelfResolutionWhenLeafrefIsInDeepTreeAndLeafIsInModuleWithReferredTypeUnion()
            throws IOException, ParserException {

        YangNode node = manager.getDataModel(
                "src/test/resources/SelfResolutionWhenLeafrefIsInDeepTreeAndLeafIsInModuleWithReferredTypeUnion.yang");
        // Check whether the data model tree returned is of type module.
        assertThat((node instanceof YangModule), is(true));

        // Check whether the node type is set properly to module.
        assertThat(node.getNodeType(), is(YangNodeType.MODULE_NODE));

        // Check whether the module name is set correctly.
        YangModule yangNode = (YangModule) node;
        assertThat(yangNode.getName(), is("Test"));

        YangContainer containerParent = (YangContainer) yangNode.getChild().getChild().getChild();
        ListIterator<YangLeaf> leafIterator;
        YangLeaf leafInfo;

        leafIterator = containerParent.getListOfLeaf().listIterator();
        leafInfo = leafIterator.next();

        // Check whether the information in the leaf is correct.
        assertThat(leafInfo.getName(), is("name"));
        assertThat(leafInfo.getDataType().getDataTypeName(), is("leafref"));
        assertThat(leafInfo.getDataType().getDataType(), is(YangDataTypes.LEAFREF));
        YangLeafRef leafref = (YangLeafRef) (leafInfo.getDataType().getDataTypeExtendedInfo());

        // Check whether leafref type got resolved.
        assertThat(leafref.getResolvableStatus(),
                is(ResolvableStatus.RESOLVED));

        // Check the effective type for the leaf.
        assertThat(leafref.getEffectiveDataType().getDataType(),
                is(YangDataTypes.UNION));
    }

    /**
     * Checks self resolution when leafref of leaf-list under module refers to leaf in container.
     */
    @Test
    public void processSelfResolutionWhenLeafrefReferToContainerLeafList()
            throws IOException, ParserException {

        YangNode node = manager
                .getDataModel("src/test/resources/SelfResolutionWhenLeafrefReferToContainerLeafList.yang");

        // Check whether the data model tree returned is of type module.
        assertThat((node instanceof YangModule), is(true));

        // Check whether the node type is set properly to module.
        assertThat(node.getNodeType(), is(YangNodeType.MODULE_NODE));

        // Check whether the module name is set correctly.
        YangModule yangNode = (YangModule) node;
        assertThat(yangNode.getName(), is("ietf-network"));

        ListIterator<YangLeafList> leafListIterator;
        YangLeafList leafListInfo;

        leafListIterator = yangNode.getListOfLeafList().listIterator();
        leafListInfo = leafListIterator.next();

        // Check whether the information in the leaf is correct.
        assertThat(leafListInfo.getName(), is("network-ref"));
        assertThat(leafListInfo.getDataType().getDataTypeName(), is("leafref"));
        assertThat(leafListInfo.getDataType().getDataType(), is(YangDataTypes.LEAFREF));
        YangLeafRef leafref = (YangLeafRef) (leafListInfo.getDataType().getDataTypeExtendedInfo());

        // Check whether leafref type got resolved.
        assertThat(leafref.getResolvableStatus(),
                is(ResolvableStatus.RESOLVED));

        // Check the effective type for the leaf.
        assertThat(leafref.getEffectiveDataType().getDataType(),
                is(YangDataTypes.UINT8));
    }

    /**
     * Checks self resolution when leafref of leaf-list under module refers to leaf-list in input of rpc.
     */
    @Test
    public void processSelfResolutionWhenLeafrefInModuleReferToLeafListInInputOfRpc()
            throws IOException, ParserException {

        YangNode node = manager
                .getDataModel("src/test/resources/SelfResolutionWhenLeafrefInModuleReferToLeafListInInputOfRpc.yang");

        // Check whether the data model tree returned is of type module.
        assertThat((node instanceof YangModule), is(true));

        // Check whether the node type is set properly to module.
        assertThat(node.getNodeType(), is(YangNodeType.MODULE_NODE));

        // Check whether the module name is set correctly.
        YangModule yangNode = (YangModule) node;
        assertThat(yangNode.getName(), is("ietf-network"));

        ListIterator<YangLeafList> leafListIterator;
        YangLeafList leafListInfo;

        leafListIterator = yangNode.getListOfLeafList().listIterator();
        leafListInfo = leafListIterator.next();

        // Check whether the information in the leaf is correct.
        assertThat(leafListInfo.getName(), is("network-ref"));
        assertThat(leafListInfo.getDataType().getDataTypeName(), is("leafref"));
        assertThat(leafListInfo.getDataType().getDataType(), is(YangDataTypes.LEAFREF));
        YangLeafRef leafref = (YangLeafRef) (leafListInfo.getDataType().getDataTypeExtendedInfo());

        // Check whether leafref type got resolved.
        assertThat(leafref.getResolvableStatus(),
                is(ResolvableStatus.RESOLVED));

        // Check the effective type for the leaf.
        assertThat(leafref.getEffectiveDataType().getDataType(),
                is(YangDataTypes.UINT8));
    }

    /**
     * Checks self resolution when leafref of leaf-list under module refers to invalid node.
     * Inter file linking also has to be done to know the error message.
     */
    @Test
    public void processSelfResolutionWhenLeafrefIsInDeepTreeAndLeafListIsInModuleWithReferredTypeEnumeration()
            throws IOException, ParserException {

        YangNode node = manager.getDataModel(
                "src/test/resources/" +
                        "SelfResolutionWhenLeafrefIsInDeepTreeAndLeafListIsInModuleWithReferredTypeEnumeration.yang");
        // Check whether the data model tree returned is of type module.
        assertThat((node instanceof YangModule), is(true));

        // Check whether the node type is set properly to module.
        assertThat(node.getNodeType(), is(YangNodeType.MODULE_NODE));

        // Check whether the module name is set correctly.
        YangModule yangNode = (YangModule) node;
        assertThat(yangNode.getName(), is("Test"));

        YangContainer containerParent = (YangContainer) yangNode.getChild().getChild().getChild();
        ListIterator<YangLeafList> leafListListIterator;
        YangLeafList leafListInfo;

        leafListListIterator = containerParent.getListOfLeafList().listIterator();
        leafListInfo = leafListListIterator.next();

        // Check whether the information in the leaf is correct.
        assertThat(leafListInfo.getName(), is("name"));
        assertThat(leafListInfo.getDataType().getDataTypeName(), is("leafref"));
        assertThat(leafListInfo.getDataType().getDataType(), is(YangDataTypes.LEAFREF));
        YangLeafRef leafref = (YangLeafRef) (leafListInfo.getDataType().getDataTypeExtendedInfo());

        // Check whether leafref type got resolved.
        assertThat(leafref.getResolvableStatus(),
                is(ResolvableStatus.RESOLVED));

        // Check the effective type for the leaf.
        assertThat(leafref.getEffectiveDataType().getDataType(),
                is(YangDataTypes.ENUMERATION));
    }

    /**
     * Checks the error scenerio when the referred node is not a leaf or leaf-list.
     */
    @Test
    public void processSelfResolutionWhenLeafrefDoesNotReferToLeafOrLeafList()
            throws IOException, ParserException {

        YangNode node = manager
                .getDataModel("src/test/resources/SelfResolutionWhenLeafrefDoesNotReferToLeafOrLeafList.yang");

        // Check whether the data model tree returned is of type module.
        assertThat((node instanceof YangModule), is(true));

        // Check whether the node type is set properly to module.
        assertThat(node.getNodeType(), is(YangNodeType.MODULE_NODE));

        // Check whether the module name is set correctly.
        YangModule yangNode = (YangModule) node;
        assertThat(yangNode.getName(), is("ietf-network"));

        ListIterator<YangLeaf> leafIterator;
        YangLeaf leafInfo;

        //YangGrouping grouping = (YangGrouping) yangNode.getChild().getNextSibling();
        leafIterator = yangNode.getListOfLeaf().listIterator();
        leafInfo = leafIterator.next();

        // Check whether the information in the leaf is correct under grouping.
        assertThat(leafInfo.getName(), is("network-ref"));
        assertThat(leafInfo.getDataType().getDataTypeName(), is("leafref"));
        assertThat(leafInfo.getDataType().getDataType(), is(YangDataTypes.LEAFREF));
        YangLeafRef leafref = (YangLeafRef) (leafInfo.getDataType().getDataTypeExtendedInfo());

        assertThat(leafref.getResolvableStatus(),
                is(ResolvableStatus.INTRA_FILE_RESOLVED));
    }

    /**
     * Checks self resolution when leafref of leaf-list under module refers to leaf in container.
     */
    @Test
    public void processSelfResolutionWhenLeafrefInTypedefReferToContainer()
            throws IOException, ParserException {

        YangNode node = manager
                .getDataModel("src/test/resources/SelfResolutionWhenLeafrefInTypedefReferToContainer.yang");

        // Check whether the data model tree returned is of type module.
        assertThat((node instanceof YangModule), is(true));

        // Check whether the node type is set properly to module.
        assertThat(node.getNodeType(), is(YangNodeType.MODULE_NODE));

        // Check whether the module name is set correctly.
        YangModule yangNode = (YangModule) node;
        assertThat(yangNode.getName(), is("ietf-network"));

        YangContainer yangContainer = (YangContainer) yangNode.getChild();
        ListIterator<YangLeaf> leafIterator;
        YangLeaf leafInfo;
        leafIterator = yangContainer.getListOfLeaf().listIterator();
        leafInfo = leafIterator.next();

        // Check whether the information in the leaf is correct under grouping.
        assertThat(leafInfo.getName(), is("network-id"));
        assertThat(leafInfo.getDataType().getDataType(), is(YangDataTypes.LEAFREF));

        YangLeafRef leafref = (YangLeafRef) (leafInfo.getDataType().getDataTypeExtendedInfo());

        // Check whether leafref type got resolved.
        assertThat(leafref.getResolvableStatus(),
                is(ResolvableStatus.RESOLVED));

        // Check the effective type for the leaf.
        assertThat(leafref.getEffectiveDataType().getDataType(),
                is(YangDataTypes.UINT8));
    }

    /**
     * Checks self resolution when leafref of leaf-list under module refers to leaf-list in input of rpc.
     */
    @Test
    public void processSelfResolutionWhenLeafrefInTypedefModuleReferToLeafListInInputOfRpc()
            throws IOException, ParserException {

        YangNode node = manager.getDataModel(
                "src/test/resources/SelfResolutionWhenLeafrefInTypedefModuleReferToLeafListInInputOfRpc.yang");

        // Check whether the data model tree returned is of type module.
        assertThat((node instanceof YangModule), is(true));

        // Check whether the node type is set properly to module.
        assertThat(node.getNodeType(), is(YangNodeType.MODULE_NODE));

        // Check whether the module name is set correctly.
        YangModule yangNode = (YangModule) node;
        assertThat(yangNode.getName(), is("ietf-network"));

        YangInput yangInput = (YangInput) yangNode.getChild().getChild();

        ListIterator<YangLeafList> leafListIterator;
        YangLeafList yangLeafListInfo;
        leafListIterator = yangInput.getListOfLeafList().listIterator();
        yangLeafListInfo = leafListIterator.next();

        // Check whether the information in the leaf is correct under grouping.
        assertThat(yangLeafListInfo.getName(), is("network-id"));
        assertThat(yangLeafListInfo.getDataType().getDataType(), is(YangDataTypes.LEAFREF));

        YangLeafRef leafref = (YangLeafRef) (yangLeafListInfo.getDataType().getDataTypeExtendedInfo());

        // Check whether leafref type got resolved.
        assertThat(leafref.getResolvableStatus(),
                is(ResolvableStatus.RESOLVED));

        // Check the effective type for the leaf.
        assertThat(leafref.getEffectiveDataType().getDataType(),
                is(YangDataTypes.UINT8));
    }

    /**
     * Checks self resolution when leafref of leaf-list under module refers to invalid node.
     * Inter file linking also has to be done to know the error message.
     */
    @Test
    public void processSelfResolutionWhenLeafrefInTypedefIsInDeepTreeAndLeafListIsInModuleWithReferredTypeEnumeration()
            throws IOException, ParserException {

        YangNode node = manager.getDataModel(
                "src/test/resources/" +
                        "SelfResolutionWhenLeafrefInTypedefIs" +
                        "InDeepTreeAndLeafListIsInModuleWithReferredTypeEnumeration.yang");
        // Check whether the data model tree returned is of type module.
        assertThat((node instanceof YangModule), is(true));

        // Check whether the node type is set properly to module.
        assertThat(node.getNodeType(), is(YangNodeType.MODULE_NODE));

        // Check whether the module name is set correctly.
        YangModule yangNode = (YangModule) node;
        assertThat(yangNode.getName(), is("Test"));

        YangContainer yangContainer = (YangContainer) yangNode.getChild().getChild().getChild().getNextSibling();

        ListIterator<YangLeaf> leafIterator;
        YangLeaf yangLeafInfo;
        leafIterator = yangContainer.getListOfLeaf().listIterator();
        yangLeafInfo = leafIterator.next();

        // Check whether the information in the leaf is correct under grouping.
        assertThat(yangLeafInfo.getName(), is("interval"));
        assertThat(yangLeafInfo.getDataType().getDataType(), is(YangDataTypes.LEAFREF));

        YangLeafRef leafref = (YangLeafRef) (yangLeafInfo.getDataType().getDataTypeExtendedInfo());

        // Check whether leafref type got resolved.
        assertThat(leafref.getResolvableStatus(),
                is(ResolvableStatus.RESOLVED));

        // Check the effective type for the leaf.
        assertThat(leafref.getEffectiveDataType().getDataType(),
                is(YangDataTypes.ENUMERATION));
    }

    /**
     * Checks self resolution when grouping and uses are siblings.
     * Grouping followed by uses.
     */
    @Test
    public void processSelfResolutionWhenLeafrefRefersAnotherLeafref()
            throws IOException, ParserException {

        YangNode node = manager.getDataModel("src/test/resources/SelfResolutionWhenLeafrefReferToAnotherLeafref.yang");
        // Check whether the data model tree returned is of type module.
        assertThat((node instanceof YangModule), is(true));

        // Check whether the node type is set properly to module.
        assertThat(node.getNodeType(), is(YangNodeType.MODULE_NODE));

        // Check whether the module name is set correctly.
        YangModule yangNode = (YangModule) node;
        assertThat(yangNode.getName(), is("ietf-network"));

        ListIterator<YangLeaf> leafIterator;
        YangLeaf leafInfo;

        //YangGrouping grouping = (YangGrouping) yangNode.getChild().getNextSibling();
        leafIterator = yangNode.getListOfLeaf().listIterator();
        leafInfo = leafIterator.next();

        // Check whether the information in the leaf is correct under grouping.
        assertThat(leafInfo.getName(), is("network-ref"));
        assertThat(leafInfo.getDataType().getDataTypeName(), is("leafref"));
        assertThat(leafInfo.getDataType().getDataType(), is(YangDataTypes.LEAFREF));
        YangLeafRef leafref = (YangLeafRef) (leafInfo.getDataType().getDataTypeExtendedInfo());

        assertThat(leafref.getResolvableStatus(),
                is(ResolvableStatus.RESOLVED));

        assertThat(leafref.getEffectiveDataType().getDataType(),
                is(YangDataTypes.UINT8));
    }

    /**
     * Checks self resolution when leafref refers to many other leafref.
     */
    @Test
    public void processSelfResolutionWhenLeafrefReferToMultipleLeafref()
            throws IOException, ParserException {

        YangNode node = manager.getDataModel("src/test/resources/SelfResolutionWhenLeafrefReferToMultipleLeafref.yang");
        // Check whether the data model tree returned is of type module.
        assertThat((node instanceof YangModule), is(true));

        // Check whether the node type is set properly to module.
        assertThat(node.getNodeType(), is(YangNodeType.MODULE_NODE));

        // Check whether the module name is set correctly.
        YangModule yangNode = (YangModule) node;
        assertThat(yangNode.getName(), is("Test"));

        YangContainer containerInModule = (YangContainer) yangNode.getChild().getNextSibling();
        YangContainer containerInList = (YangContainer) containerInModule.getChild().getChild();

        ListIterator<YangLeaf> leafIterator;
        YangLeaf leafInfo;

        leafIterator = containerInList.getListOfLeaf().listIterator();
        leafInfo = leafIterator.next();

        // Check whether the information in the leaf is correct under grouping.
        assertThat(leafInfo.getName(), is("remove"));
        assertThat(leafInfo.getDataType().getDataTypeName(), is("leafref"));
        assertThat(leafInfo.getDataType().getDataType(), is(YangDataTypes.LEAFREF));
        YangLeafRef leafref = (YangLeafRef) (leafInfo.getDataType().getDataTypeExtendedInfo());

        assertThat(leafref.getResolvableStatus(),
                is(ResolvableStatus.RESOLVED));

        assertThat(leafref.getEffectiveDataType().getDataType(),
                is(YangDataTypes.ENUMERATION));
    }

    /**
     * Checks self resolution when grouping and uses are siblings.
     * Grouping followed by uses.
     */
    @Test
    public void processSelfResolutionWhenLeafrefRefersAnotherDerivedType()
            throws IOException, ParserException {

        YangNode node = manager
                .getDataModel("src/test/resources/SelfResolutionWhenLeafrefReferToAnotherDerivedType.yang");
        // Check whether the data model tree returned is of type module.
        assertThat((node instanceof YangModule), is(true));

        // Check whether the node type is set properly to module.
        assertThat(node.getNodeType(), is(YangNodeType.MODULE_NODE));

        // Check whether the module name is set correctly.
        YangModule yangNode = (YangModule) node;
        assertThat(yangNode.getName(), is("ietf-network"));

        ListIterator<YangLeaf> leafIterator;
        YangLeaf leafInfo;

        //YangGrouping grouping = (YangGrouping) yangNode.getChild().getNextSibling();
        leafIterator = yangNode.getListOfLeaf().listIterator();
        leafInfo = leafIterator.next();

        // Check whether the information in the leaf is correct under grouping.
        assertThat(leafInfo.getName(), is("network-ref"));
        assertThat(leafInfo.getDataType().getDataTypeName(), is("leafref"));
        assertThat(leafInfo.getDataType().getDataType(), is(YangDataTypes.LEAFREF));
        YangLeafRef leafref = (YangLeafRef) (leafInfo.getDataType().getDataTypeExtendedInfo());

        assertThat(leafref.getResolvableStatus(),
                is(ResolvableStatus.RESOLVED));

        assertThat(leafref.getEffectiveDataType().getDataType(),
                is(YangDataTypes.DERIVED));
    }

    /**
     * Checks self resolution when leafref refers to many other leafref.
     */
    @Test
    public void processSelfResolutionWhenLeafrefReferToMultipleTypedef()
            throws IOException, ParserException {

        YangNode node = manager.getDataModel("src/test/resources/SelfResolutionWhenLeafrefReferToMultipleTypedef.yang");
        // Check whether the data model tree returned is of type module.
        assertThat((node instanceof YangModule), is(true));

        // Check whether the node type is set properly to module.
        assertThat(node.getNodeType(), is(YangNodeType.MODULE_NODE));

        // Check whether the module name is set correctly.
        YangModule yangNode = (YangModule) node;
        assertThat(yangNode.getName(), is("Test"));

        YangContainer containerInModule = (YangContainer) yangNode.getChild().getNextSibling();
        YangContainer containerInList = (YangContainer) containerInModule.getChild().getChild();

        ListIterator<YangLeaf> leafIterator;
        YangLeaf leafInfo;

        leafIterator = containerInList.getListOfLeaf().listIterator();
        leafInfo = leafIterator.next();

        // Check whether the information in the leaf is correct under grouping.
        assertThat(leafInfo.getName(), is("remove"));
        assertThat(leafInfo.getDataType().getDataTypeName(), is("leafref"));
        assertThat(leafInfo.getDataType().getDataType(), is(YangDataTypes.LEAFREF));
        YangLeafRef leafref = (YangLeafRef) (leafInfo.getDataType().getDataTypeExtendedInfo());

        assertThat(leafref.getResolvableStatus(),
                is(ResolvableStatus.RESOLVED));

        assertThat(leafref.getEffectiveDataType().getDataType(),
                is(YangDataTypes.DERIVED));
    }

    /**
     * Checks self resolution when leafref refers to many other leaf with derived type
     * which in turn referring to another leaf.
     */
    @Test
    public void processSelfResolutionWhenLeafrefReferToDerivedTypeReferringToLeafWithLeafref()
            throws IOException, ParserException {

        YangNode node = manager.getDataModel(
                "src/test/resources/SelfResolutionWhenLeafrefReferToDerivedTypeReferringToLeafWithLeafref.yang");
        // Check whether the data model tree returned is of type module.
        assertThat((node instanceof YangModule), is(true));

        // Check whether the node type is set properly to module.
        assertThat(node.getNodeType(), is(YangNodeType.MODULE_NODE));

        // Check whether the module name is set correctly.
        YangModule yangNode = (YangModule) node;
        assertThat(yangNode.getName(), is("Test"));

        YangContainer containerInModule = (YangContainer) yangNode.getChild().getNextSibling();
        YangContainer containerInList = (YangContainer) containerInModule.getChild().getChild();

        ListIterator<YangLeaf> leafIterator;
        YangLeaf leafInfo;

        leafIterator = containerInList.getListOfLeaf().listIterator();
        leafInfo = leafIterator.next();

        // Check whether the information in the leaf is correct under grouping.
        assertThat(leafInfo.getName(), is("remove"));
        assertThat(leafInfo.getDataType().getDataTypeName(), is("leafref"));
        assertThat(leafInfo.getDataType().getDataType(), is(YangDataTypes.LEAFREF));
        YangLeafRef leafref = (YangLeafRef) leafInfo.getDataType().getDataTypeExtendedInfo();

        assertThat(leafref.getResolvableStatus(),
                is(ResolvableStatus.RESOLVED));

        assertThat(leafref.getEffectiveDataType().getDataType(),
                is(YangDataTypes.ENUMERATION));
    }

    /**
     * Checks self resolution when leafref under module refers to leaf in container with relative path.
     */
    @Test
    public void processSelfResolutionWhenLeafrefReferToContainerLeafRelPath()
            throws IOException, ParserException {

        YangNode node = manager
                .getDataModel("src/test/resources/SelfResolutionWhenLeafrefReferToContainerLeafRelPath.yang");

        // Check whether the data model tree returned is of type module.
        assertThat((node instanceof YangModule), is(true));

        // Check whether the node type is set properly to module.
        assertThat(node.getNodeType(), is(YangNodeType.MODULE_NODE));

        // Check whether the module name is set correctly.
        YangModule yangNode = (YangModule) node;
        assertThat(yangNode.getName(), is("ietf-network"));

        ListIterator<YangLeaf> leafIterator;
        YangLeaf leafInfo;

        leafIterator = yangNode.getListOfLeaf().listIterator();
        leafInfo = leafIterator.next();

        // Check whether the information in the leaf is correct.
        assertThat(leafInfo.getName(), is("network-ref"));
        assertThat(leafInfo.getDataType().getDataTypeName(), is("leafref"));
        assertThat(leafInfo.getDataType().getDataType(), is(YangDataTypes.LEAFREF));
        YangLeafRef leafref = (YangLeafRef) (leafInfo.getDataType().getDataTypeExtendedInfo());

        // Check whether leafref type got resolved.
        assertThat(leafref.getResolvableStatus(),
                is(ResolvableStatus.RESOLVED));

        // Check the effective type for the leaf.
        assertThat(leafref.getEffectiveDataType().getDataType(),
                is(YangDataTypes.UINT8));
    }

    /**
     * Checks self resolution when leafref under module refers to grouping rpc with input as name.
     * Rpc has input child also. So here the node search must be done by taking input node using relative path.
     */
    @Test
    public void processSelfResolutionWhenLeafrefInModuleReferToGroupingWithInputInRpcRelPath()
            throws IOException, ParserException {

        YangNode node = manager.getDataModel(
                "src/test/resources/SelfResolutionWhenLeafrefInModuleReferToGroupingWithInputInRpcRelPath.yang");

        // Check whether the data model tree returned is of type module.
        assertThat((node instanceof YangModule), is(true));

        // Check whether the node type is set properly to module.
        assertThat(node.getNodeType(), is(YangNodeType.MODULE_NODE));

        // Check whether the module name is set correctly.
        YangModule yangNode = (YangModule) node;
        assertThat(yangNode.getName(), is("ietf-network"));

        ListIterator<YangLeaf> leafIterator;
        YangLeaf leafInfo;

        leafIterator = yangNode.getListOfLeaf().listIterator();
        leafInfo = leafIterator.next();

        // Check whether the information in the leaf is correct.
        assertThat(leafInfo.getName(), is("network-ref"));
        assertThat(leafInfo.getDataType().getDataTypeName(), is("leafref"));
        assertThat(leafInfo.getDataType().getDataType(), is(YangDataTypes.LEAFREF));
        YangLeafRef leafref = (YangLeafRef) (leafInfo.getDataType().getDataTypeExtendedInfo());

        // Check whether leafref type got resolved.
        assertThat(leafref.getResolvableStatus(),
                is(ResolvableStatus.RESOLVED));

        // Check the effective type for the leaf.
        assertThat(leafref.getEffectiveDataType().getDataType(),
                is(YangDataTypes.UINT8));
    }

    /**
     * Checks self resolution when leafref under module refers to invalid root node with relative path.
     */
    @Test
    public void processSelfResolutionWhenLeafrefInModuleReferToInvalidRootNodeRelPath()
            throws IOException, ParserException {

        thrown.expect(LinkerException.class);
        thrown.expectMessage(
                "YANG file error: The target node of leafref is invalid.");
        YangNode node = manager
                .getDataModel("src/test/resources/SelfResolutionWhenLeafrefInModuleReferToInvalidRootNodeRelPath.yang");
    }

    /**
     * Checks self resolution when leafref under module refers to invalid node.
     * Inter file linking also has to be done to know the error message with relative path.
     */
    @Test
    public void processSelfResolutionWhenLeafrefInModuleReferToInvalidNodeRelPath()
            throws IOException, ParserException {

        YangNode node = manager
                .getDataModel("src/test/resources/SelfResolutionWhenLeafrefInModuleReferToInvalidNodeRelPath.yang");
        // Check whether the data model tree returned is of type module.
        assertThat((node instanceof YangModule), is(true));

        // Check whether the node type is set properly to module.
        assertThat(node.getNodeType(), is(YangNodeType.MODULE_NODE));

        // Check whether the module name is set correctly.
        YangModule yangNode = (YangModule) node;
        assertThat(yangNode.getName(), is("ietf-network"));

        ListIterator<YangLeaf> leafIterator;
        YangLeaf leafInfo;

        leafIterator = yangNode.getListOfLeaf().listIterator();
        leafInfo = leafIterator.next();

        // Check whether the information in the leaf is correct.
        assertThat(leafInfo.getName(), is("network-ref"));
        assertThat(leafInfo.getDataType().getDataTypeName(), is("leafref"));
        assertThat(leafInfo.getDataType().getDataType(), is(YangDataTypes.LEAFREF));
        YangLeafRef leafref = (YangLeafRef) (leafInfo.getDataType().getDataTypeExtendedInfo());

        // Check whether leafref type got intra file resolved.
        assertThat(leafref.getResolvableStatus(),
                is(ResolvableStatus.INTRA_FILE_RESOLVED));
    }

    /**
     * Checks self resolution when leafref of leaf-list under module refers to leaf in container with relative path.
     */
    @Test
    public void processSelfResolutionWhenLeafrefInTypedefReferToContainerRelPath()
            throws IOException, ParserException {

        YangNode node = manager
                .getDataModel("src/test/resources/SelfResolutionWhenLeafrefInTypedefReferToContainerRelPath.yang");

        // Check whether the data model tree returned is of type module.
        assertThat((node instanceof YangModule), is(true));

        // Check whether the node type is set properly to module.
        assertThat(node.getNodeType(), is(YangNodeType.MODULE_NODE));

        // Check whether the module name is set correctly.
        YangModule yangNode = (YangModule) node;
        assertThat(yangNode.getName(), is("ietf-network"));
        ListIterator<YangLeaf> leafIterator;
        YangLeaf leafInfo;
        YangContainer yangContainer = (YangContainer) yangNode.getChild();
        leafIterator = yangContainer.getListOfLeaf().listIterator();
        leafInfo = leafIterator.next();

        // Check whether the information in the leaf is correct under grouping.
        assertThat(leafInfo.getName(), is("network-id"));
        assertThat(leafInfo.getDataType().getDataType(), is(YangDataTypes.LEAFREF));

        YangLeafRef leafref = (YangLeafRef) (leafInfo.getDataType().getDataTypeExtendedInfo());

        // Check whether leafref type got resolved.
        assertThat(leafref.getResolvableStatus(),
                is(ResolvableStatus.RESOLVED));

        // Check the effective type for the leaf.
        assertThat(leafref.getEffectiveDataType().getDataType(),
                is(YangDataTypes.UINT8));
    }

    /**
     * Checks self resolution when leafref refers to many other leafref with relative path.
     */
    @Test
    public void processSelfResolutionWhenLeafrefReferToMultipleLeafrefRelPath()
            throws IOException, ParserException {

        YangNode node = manager
                .getDataModel("src/test/resources/SelfResolutionWhenLeafrefReferToMultipleLeafrefRelPath.yang");
        // Check whether the data model tree returned is of type module.
        assertThat((node instanceof YangModule), is(true));

        // Check whether the node type is set properly to module.
        assertThat(node.getNodeType(), is(YangNodeType.MODULE_NODE));

        // Check whether the module name is set correctly.
        YangModule yangNode = (YangModule) node;
        assertThat(yangNode.getName(), is("Test"));

        YangContainer containerInModule = (YangContainer) yangNode.getChild().getNextSibling();
        YangContainer containerInList = (YangContainer) containerInModule.getChild().getChild();

        ListIterator<YangLeaf> leafIterator;
        YangLeaf leafInfo;

        leafIterator = containerInList.getListOfLeaf().listIterator();
        leafInfo = leafIterator.next();

        // Check whether the information in the leaf is correct under grouping.
        assertThat(leafInfo.getName(), is("remove"));
        assertThat(leafInfo.getDataType().getDataTypeName(), is("leafref"));
        assertThat(leafInfo.getDataType().getDataType(), is(YangDataTypes.LEAFREF));
        YangLeafRef leafref = (YangLeafRef) (leafInfo.getDataType().getDataTypeExtendedInfo());

        assertThat(leafref.getResolvableStatus(),
                is(ResolvableStatus.RESOLVED));

        assertThat(leafref.getEffectiveDataType().getDataType(),
                is(YangDataTypes.ENUMERATION));
    }

    /**
     * Checks self resolution when leafref refers to many other leaf with derived type
     * which in turn referring to another leaf with relative type.
     */
    @Test
    public void processSelfResolutionWhenLeafrefReferToDerivedTypeReferringToLeafWithLeafrefRelType()
            throws IOException, ParserException {

        YangNode node = manager.getDataModel(
                "src/test/resources/SelfResolutionWhenLeafrefReferToDerivedTypeReferringToLeafWithLeafrefRelType.yang");
        // Check whether the data model tree returned is of type module.
        assertThat((node instanceof YangModule), is(true));

        // Check whether the node type is set properly to module.
        assertThat(node.getNodeType(), is(YangNodeType.MODULE_NODE));

        // Check whether the module name is set correctly.
        YangModule yangNode = (YangModule) node;
        assertThat(yangNode.getName(), is("Test"));

        YangContainer containerInModule = (YangContainer) yangNode.getChild().getNextSibling();
        YangContainer containerInList = (YangContainer) containerInModule.getChild().getChild();

        ListIterator<YangLeaf> leafIterator;
        YangLeaf leafInfo;

        leafIterator = containerInList.getListOfLeaf().listIterator();
        leafInfo = leafIterator.next();

        // Check whether the information in the leaf is correct under grouping.
        assertThat(leafInfo.getName(), is("remove"));
        assertThat(leafInfo.getDataType().getDataTypeName(), is("leafref"));
        assertThat(leafInfo.getDataType().getDataType(), is(YangDataTypes.LEAFREF));
        YangLeafRef leafref = (YangLeafRef) leafInfo.getDataType().getDataTypeExtendedInfo();

        assertThat(leafref.getResolvableStatus(),
                is(ResolvableStatus.RESOLVED));

        assertThat(leafref.getEffectiveDataType().getDataType(),
                is(YangDataTypes.ENUMERATION));
    }

    /**
     * Checks the valid scenerios of path argument having proper setters.
     */
    @Test
    public void processPathArgumentStatement()
            throws IOException, ParserException {

        YangNode node = manager.getDataModel("src/test/resources/PathListener.yang");
        // Check whether the data model tree returned is of type module.
        assertThat((node instanceof YangModule), is(true));

        // Check whether the node type is set properly to module.
        assertThat(node.getNodeType(), is(YangNodeType.MODULE_NODE));

        // Check whether the module name is set correctly.
        YangModule yangNode = (YangModule) node;
        assertThat(yangNode.getName(), is("PathListener"));
        YangList listInModule = (YangList) yangNode.getChild();

        YangContainer containerInModule = (YangContainer) yangNode.getChild().getNextSibling();
        ListIterator<YangLeaf> leafIterator;
        YangLeaf leafInfo;

        YangLeaf leafNameInList = listInModule.getListOfLeaf().listIterator().next();

        leafIterator = containerInModule.getListOfLeaf().listIterator();
        leafInfo = leafIterator.next();

        // Check whether the information in the leaf is correct under grouping.
        assertThat(leafInfo.getName(), is("ifname"));
        assertThat(leafInfo.getDataType().getDataTypeName(), is("leafref"));
        assertThat(leafInfo.getDataType().getDataType(), is(YangDataTypes.LEAFREF));
        YangLeafRef leafref = (YangLeafRef) leafInfo.getDataType().getDataTypeExtendedInfo();
        assertThat(leafref.getPathType(), is(YangPathArgType.RELATIVE_PATH));

        YangRelativePath relativePathForName = leafref.getRelativePath();
        assertThat(relativePathForName.getAncestorNodeCount(), is(2));
        List<YangAtomicPath> absPathForName = relativePathForName.getAtomicPathList();
        Iterator<YangAtomicPath> absPathIteratorForName = absPathForName.listIterator();
        YangAtomicPath abspathForName = absPathIteratorForName.next();
        assertThat(abspathForName.getNodeIdentifier().getName(), is("interface"));
        assertThat(abspathForName.getNodeIdentifier().getPrefix(), is("test"));
        YangAtomicPath abspath1 = absPathIteratorForName.next();
        assertThat(abspath1.getNodeIdentifier().getName(), is("name"));
        assertThat(abspath1.getNodeIdentifier().getPrefix(), is("test"));

        YangLeaf leafInfo1 = leafIterator.next();
        // Check whether the information in the leaf is correct under grouping.
        assertThat(leafInfo1.getName(), is("status"));
        assertThat(leafInfo1.getDataType().getDataTypeName(), is("leafref"));
        assertThat(leafInfo1.getDataType().getDataType(), is(YangDataTypes.LEAFREF));

        YangLeafRef leafref1 = (YangLeafRef) leafInfo1.getDataType().getDataTypeExtendedInfo();
        assertThat(leafref1.getPathType(), is(YangPathArgType.ABSOLUTE_PATH));

        List<YangAtomicPath> absolutePathList = leafref1.getAtomicPath();
        Iterator<YangAtomicPath> absPathIterator = absolutePathList.listIterator();
        YangAtomicPath abspath = absPathIterator.next();
        assertThat(abspath.getNodeIdentifier().getName(), is("interface"));
        assertThat(abspath.getNodeIdentifier().getPrefix(), is("test"));

        List<YangPathPredicate> pathPredicateList = abspath.getPathPredicatesList();
        Iterator<YangPathPredicate> pathPredicate = pathPredicateList.listIterator();
        YangPathPredicate pathPredicate1 = pathPredicate.next();
        assertThat(pathPredicate1.getNodeIdentifier().getName(), is("name"));
        assertThat(pathPredicate1.getNodeIdentifier().getPrefix(), nullValue());
        assertThat(pathPredicate1.getRightRelativePath().getAncestorNodeCount(), is(1));
        assertThat(pathPredicate1.getPathOperator(), is(YangPathOperator.EQUALTO));
        assertThat(pathPredicate1.getRightRelativePath().getAtomicPathList().listIterator().next().getNodeIdentifier()
                .getName(), is("ifname"));
        YangAtomicPath abspath2 = absPathIterator.next();
        assertThat(abspath2.getNodeIdentifier().getName(), is("admin-status"));
        assertThat(abspath2.getNodeIdentifier().getPrefix(), is("test"));

        assertThat(pathPredicate1.getLeftAxisNode(), is(leafNameInList));
        assertThat(pathPredicate1.getRightAxisNode(), is(leafInfo));
    }
}

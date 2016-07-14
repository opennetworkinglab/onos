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

import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.onosproject.yangutils.datamodel.YangAtomicPath;
import org.onosproject.yangutils.datamodel.YangAugment;
import org.onosproject.yangutils.datamodel.YangContainer;
import org.onosproject.yangutils.datamodel.YangFeature;
import org.onosproject.yangutils.datamodel.YangIfFeature;
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
import org.onosproject.yangutils.datamodel.utils.builtindatatype.YangDataTypes;
import org.onosproject.yangutils.linker.exceptions.LinkerException;
import org.onosproject.yangutils.linker.impl.YangLinkerManager;
import org.onosproject.yangutils.parser.exceptions.ParserException;
import org.onosproject.yangutils.parser.impl.YangUtilsParserManager;
import org.onosproject.yangutils.utils.io.impl.YangFileScanner;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.onosproject.yangutils.datamodel.YangNodeType.MODULE_NODE;
import static org.onosproject.yangutils.datamodel.utils.ResolvableStatus.RESOLVED;
import static org.onosproject.yangutils.datamodel.utils.builtindatatype.YangDataTypes.LEAFREF;

/**
 * Test cases for testing leafref intra file linking.
 */
public class IntraFileLeafrefLinkingTest {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    private final YangUtilManager utilManager = new YangUtilManager();
    private final YangLinkerManager yangLinkerManager = new YangLinkerManager();
    private final YangUtilsParserManager manager = new YangUtilsParserManager();

    /**
     * Checks self resolution when leafref under module refers to leaf in container.
     */
    @Test
    public void processSelfResolutionWhenLeafrefReferToContainerLeaf()
            throws IOException, ParserException {

        String searchDir = "src/test/resources/leafreflinker/intrafile/simpleleafref";
        utilManager.createYangFileInfoSet(YangFileScanner.getYangFiles(searchDir));
        utilManager.parseYangFileInfoSet();
        utilManager.createYangNodeSet();
        YangNode selfNode = null;

        // Create YANG node set
        yangLinkerManager.createYangNodeSet(utilManager.getYangNodeSet());

        // Add references to import list.
        yangLinkerManager.addRefToYangFilesImportList(utilManager.getYangNodeSet());

        // Carry out inter-file linking.
        yangLinkerManager.processInterFileLinking(utilManager.getYangNodeSet());

        Iterator<YangNode> yangNodeIterator = utilManager.getYangNodeSet().iterator();

        YangNode rootNode = yangNodeIterator.next();

        if (rootNode.getName().equals("SelfResolutionWhenLeafrefReferToContainerLeaf")) {
            selfNode = rootNode;
        }

        // Check whether the data model tree returned is of type module.
        assertThat((selfNode instanceof YangModule), is(true));

        // Check whether the node type is set properly to module.
        assertThat(selfNode.getNodeType(), is(YangNodeType.MODULE_NODE));

        // Check whether the module name is set correctly.
        YangModule yangNode = (YangModule) selfNode;
        assertThat(yangNode.getName(), is("SelfResolutionWhenLeafrefReferToContainerLeaf"));

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

        String searchDir = "src/test/resources/leafreflinker/intrafile/leafrefwithrpc";
        utilManager.createYangFileInfoSet(YangFileScanner.getYangFiles(searchDir));
        utilManager.parseYangFileInfoSet();
        utilManager.createYangNodeSet();
        YangNode selfNode = null;

        // Create YANG node set
        yangLinkerManager.createYangNodeSet(utilManager.getYangNodeSet());

        // Add references to import list.
        yangLinkerManager.addRefToYangFilesImportList(utilManager.getYangNodeSet());

        // Carry out inter-file linking.
        yangLinkerManager.processInterFileLinking(utilManager.getYangNodeSet());

        Iterator<YangNode> yangNodeIterator = utilManager.getYangNodeSet().iterator();

        YangNode rootNode = yangNodeIterator.next();

        if (rootNode.getName().equals("SelfResolutionWhenLeafrefInModuleReferToLeafInInputOfRpc")) {
            selfNode = rootNode;
        }

        // Check whether the data model tree returned is of type module.
        assertThat((selfNode instanceof YangModule), is(true));

        // Check whether the node type is set properly to module.
        assertThat(selfNode.getNodeType(), is(YangNodeType.MODULE_NODE));

        // Check whether the module name is set correctly.
        YangModule yangNode = (YangModule) selfNode;
        assertThat(yangNode.getName(), is("SelfResolutionWhenLeafrefInModuleReferToLeafInInputOfRpc"));

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
     * TODO: When path has RPC's input but grouping & typedef with the same name occurs.
     */
    @Ignore
    public void processSelfResolutionWhenLeafrefInModuleReferToGroupingWithInputInRpc()
            throws IOException, ParserException {

        String searchDir = "src/test/resources/leafreflinker/intrafile/leafrefwithrpcandgrouping";
        utilManager.createYangFileInfoSet(YangFileScanner.getYangFiles(searchDir));
        utilManager.parseYangFileInfoSet();
        utilManager.createYangNodeSet();
        YangNode selfNode = null;

        // Create YANG node set
        yangLinkerManager.createYangNodeSet(utilManager.getYangNodeSet());

        // Add references to import list.
        yangLinkerManager.addRefToYangFilesImportList(utilManager.getYangNodeSet());

        // Carry out inter-file linking.
        yangLinkerManager.processInterFileLinking(utilManager.getYangNodeSet());

        Iterator<YangNode> yangNodeIterator = utilManager.getYangNodeSet().iterator();

        YangNode rootNode = yangNodeIterator.next();

        if (rootNode.getName().equals("SelfResolutionWhenLeafrefInModuleReferToGroupingWithInputInRpc")) {
            selfNode = rootNode;
        }

        // Check whether the data model tree returned is of type module.
        assertThat((selfNode instanceof YangModule), is(true));

        // Check whether the node type is set properly to module.
        assertThat(selfNode.getNodeType(), is(YangNodeType.MODULE_NODE));

        // Check whether the module name is set correctly.
        YangModule yangNode = (YangModule) selfNode;
        assertThat(yangNode.getName(), is("SelfResolutionWhenLeafrefInModuleReferToGroupingWithInputInRpc"));

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
                "YANG file error: The target node, in the leafref path /networks/network-id, is invalid.");

        String searchDir = "src/test/resources/leafreflinker/intrafile/invalidscenerioforgrouping";
        utilManager.createYangFileInfoSet(YangFileScanner.getYangFiles(searchDir));
        utilManager.parseYangFileInfoSet();
        utilManager.createYangNodeSet();

        // Create YANG node set
        yangLinkerManager.createYangNodeSet(utilManager.getYangNodeSet());

        // Add references to import list.
        yangLinkerManager.addRefToYangFilesImportList(utilManager.getYangNodeSet());

        // Carry out inter-file linking.
        yangLinkerManager.processInterFileLinking(utilManager.getYangNodeSet());
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
     */
    @Test
    public void processSelfResolutionWhenLeafrefInModuleReferToInvalidNode()
            throws IOException, ParserException {

        thrown.expect(LinkerException.class);
        thrown.expectMessage(
                "YANG file error: Unable to find base leaf/leaf-list for given leafref path /define/network-id");
        String searchDir = "src/test/resources/leafreflinker/intrafile/invalidsceneriowithinvalidnode";
        utilManager.createYangFileInfoSet(YangFileScanner.getYangFiles(searchDir));
        utilManager.parseYangFileInfoSet();
        utilManager.createYangNodeSet();

        // Create YANG node set
        yangLinkerManager.createYangNodeSet(utilManager.getYangNodeSet());

        // Add references to import list.
        yangLinkerManager.addRefToYangFilesImportList(utilManager.getYangNodeSet());

        // Carry out inter-file linking.
        yangLinkerManager.processInterFileLinking(utilManager.getYangNodeSet());
    }

    /**
     * Checks self resolution when leafref under module refers to invalid node.
     * Inter file linking also has to be done to know the error message.
     */
    @Test
    public void processSelfResolutionWhenLeafrefIsInDeepTreeAndLeafIsInModuleWithReferredTypeUnion()
            throws IOException, ParserException {

        String searchDir = "src/test/resources/leafreflinker/intrafile/leafreflinking";
        utilManager.createYangFileInfoSet(YangFileScanner.getYangFiles(searchDir));
        utilManager.parseYangFileInfoSet();
        utilManager.createYangNodeSet();
        YangNode selfNode = null;

        // Create YANG node set
        yangLinkerManager.createYangNodeSet(utilManager.getYangNodeSet());

        // Add references to import list.
        yangLinkerManager.addRefToYangFilesImportList(utilManager.getYangNodeSet());

        // Carry out inter-file linking.
        yangLinkerManager.processInterFileLinking(utilManager.getYangNodeSet());

        Iterator<YangNode> yangNodeIterator = utilManager.getYangNodeSet().iterator();

        YangNode rootNode = yangNodeIterator.next();

        if (rootNode.getName().equals("Test")) {
            selfNode = rootNode;
        }

        // Check whether the data model tree returned is of type module.
        assertThat((selfNode instanceof YangModule), is(true));

        // Check whether the node type is set properly to module.
        assertThat(selfNode.getNodeType(), is(YangNodeType.MODULE_NODE));

        // Check whether the module name is set correctly.
        YangModule yangNode = (YangModule) selfNode;
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

        String searchDir = "src/test/resources/leafreflinker/intrafile/leafrefreferingtoleaflist";
        utilManager.createYangFileInfoSet(YangFileScanner.getYangFiles(searchDir));
        utilManager.parseYangFileInfoSet();
        utilManager.createYangNodeSet();
        YangNode selfNode = null;

        // Create YANG node set
        yangLinkerManager.createYangNodeSet(utilManager.getYangNodeSet());

        // Add references to import list.
        yangLinkerManager.addRefToYangFilesImportList(utilManager.getYangNodeSet());

        // Carry out inter-file linking.
        yangLinkerManager.processInterFileLinking(utilManager.getYangNodeSet());

        Iterator<YangNode> yangNodeIterator = utilManager.getYangNodeSet().iterator();

        YangNode rootNode = yangNodeIterator.next();

        if (rootNode.getName().equals("ietf-network")) {
            selfNode = rootNode;
        }

        // Check whether the data model tree returned is of type module.
        assertThat((selfNode instanceof YangModule), is(true));

        // Check whether the node type is set properly to module.
        assertThat(selfNode.getNodeType(), is(YangNodeType.MODULE_NODE));

        // Check whether the module name is set correctly.
        YangModule yangNode = (YangModule) selfNode;
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

        String searchDir = "src/test/resources/leafreflinker/intrafile/leafreftoinputinrpc";
        utilManager.createYangFileInfoSet(YangFileScanner.getYangFiles(searchDir));
        utilManager.parseYangFileInfoSet();
        utilManager.createYangNodeSet();
        YangNode selfNode = null;

        // Create YANG node set
        yangLinkerManager.createYangNodeSet(utilManager.getYangNodeSet());

        // Add references to import list.
        yangLinkerManager.addRefToYangFilesImportList(utilManager.getYangNodeSet());

        // Carry out inter-file linking.
        yangLinkerManager.processInterFileLinking(utilManager.getYangNodeSet());

        Iterator<YangNode> yangNodeIterator = utilManager.getYangNodeSet().iterator();

        YangNode rootNode = yangNodeIterator.next();

        if (rootNode.getName().equals("ietf-network")) {
            selfNode = rootNode;
        }

        // Check whether the node type is set properly to module.
        assertThat(selfNode.getNodeType(), is(YangNodeType.MODULE_NODE));

        // Check whether the module name is set correctly.
        YangModule yangNode = (YangModule) selfNode;
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

        String searchDir = "src/test/resources/leafreflinker/intrafile/leafrefwithrefleafderived";
        utilManager.createYangFileInfoSet(YangFileScanner.getYangFiles(searchDir));
        utilManager.parseYangFileInfoSet();
        utilManager.createYangNodeSet();
        YangNode selfNode = null;

        // Create YANG node set
        yangLinkerManager.createYangNodeSet(utilManager.getYangNodeSet());

        // Add references to import list.
        yangLinkerManager.addRefToYangFilesImportList(utilManager.getYangNodeSet());

        // Carry out inter-file linking.
        yangLinkerManager.processInterFileLinking(utilManager.getYangNodeSet());

        Iterator<YangNode> yangNodeIterator = utilManager.getYangNodeSet().iterator();

        YangNode rootNode = yangNodeIterator.next();

        if (rootNode.getName().equals("Test")) {
            selfNode = rootNode;
        }

        // Check whether the node type is set properly to module.
        assertThat(selfNode.getNodeType(), is(YangNodeType.MODULE_NODE));

        // Check whether the module name is set correctly.
        YangModule yangNode = (YangModule) selfNode;

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

        thrown.expect(LinkerException.class);
        thrown.expectMessage(
                "YANG file error: Unable to find base leaf/leaf-list for given leafref path /networks");
        String searchDir = "src/test/resources/leafreflinker/intrafile/invalidsceneriowithnorefleaf";
        utilManager.createYangFileInfoSet(YangFileScanner.getYangFiles(searchDir));
        utilManager.parseYangFileInfoSet();
        utilManager.createYangNodeSet();

        // Create YANG node set
        yangLinkerManager.createYangNodeSet(utilManager.getYangNodeSet());

        // Add references to import list.
        yangLinkerManager.addRefToYangFilesImportList(utilManager.getYangNodeSet());

        // Carry out inter-file linking.
        yangLinkerManager.processInterFileLinking(utilManager.getYangNodeSet());
    }

    /**
     * Checks self resolution when leafref of leaf-list under module refers to leaf in container.
     */
    @Test
    public void processSelfResolutionWhenLeafrefInTypedefReferToContainer()
            throws IOException, ParserException {
        String searchDir = "src/test/resources/leafreflinker/intrafile/leafrefintypedef";
        utilManager.createYangFileInfoSet(YangFileScanner.getYangFiles(searchDir));
        utilManager.parseYangFileInfoSet();
        utilManager.createYangNodeSet();
        YangNode selfNode = null;
        // Create YANG node set
        yangLinkerManager.createYangNodeSet(utilManager.getYangNodeSet());

        // Add references to import list.
        yangLinkerManager.addRefToYangFilesImportList(utilManager.getYangNodeSet());

        // Carry out inter-file linking.
        yangLinkerManager.processInterFileLinking(utilManager.getYangNodeSet());

        Iterator<YangNode> yangNodeIterator = utilManager.getYangNodeSet().iterator();

        YangNode rootNode = yangNodeIterator.next();

        if (rootNode.getName().equals("ietf-network")) {
            selfNode = rootNode;
        }
        // Check whether the data model tree returned is of type module.
        assertThat((selfNode instanceof YangModule), is(true));

        // Check whether the node type is set properly to module.
        assertThat(selfNode.getNodeType(), is(YangNodeType.MODULE_NODE));

        // Check whether the module name is set correctly.
        YangModule yangNode = (YangModule) selfNode;
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

        String searchDir = "src/test/resources/leafreflinker/intrafile/leafreftorpcinputleaflist";
        utilManager.createYangFileInfoSet(YangFileScanner.getYangFiles(searchDir));
        utilManager.parseYangFileInfoSet();
        utilManager.createYangNodeSet();
        YangNode selfNode = null;
        // Create YANG node set
        yangLinkerManager.createYangNodeSet(utilManager.getYangNodeSet());

        // Add references to import list.
        yangLinkerManager.addRefToYangFilesImportList(utilManager.getYangNodeSet());

        // Carry out inter-file linking.
        yangLinkerManager.processInterFileLinking(utilManager.getYangNodeSet());

        Iterator<YangNode> yangNodeIterator = utilManager.getYangNodeSet().iterator();

        YangNode rootNode = yangNodeIterator.next();

        if (rootNode.getName().equals("ietf-network")) {
            selfNode = rootNode;
        }
        // Check whether the data model tree returned is of type module.
        assertThat((selfNode instanceof YangModule), is(true));

        // Check whether the node type is set properly to module.
        assertThat(selfNode.getNodeType(), is(YangNodeType.MODULE_NODE));

        // Check whether the module name is set correctly.
        YangModule yangNode = (YangModule) selfNode;
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

        String searchDir = "src/test/resources/leafreflinker/intrafile/leafreftoleafrefwithtypedef";
        utilManager.createYangFileInfoSet(YangFileScanner.getYangFiles(searchDir));
        utilManager.parseYangFileInfoSet();
        utilManager.createYangNodeSet();
        YangNode selfNode = null;
        // Create YANG node set
        yangLinkerManager.createYangNodeSet(utilManager.getYangNodeSet());

        // Add references to import list.
        yangLinkerManager.addRefToYangFilesImportList(utilManager.getYangNodeSet());

        // Carry out inter-file linking.
        yangLinkerManager.processInterFileLinking(utilManager.getYangNodeSet());

        Iterator<YangNode> yangNodeIterator = utilManager.getYangNodeSet().iterator();

        YangNode rootNode = yangNodeIterator.next();

        if (rootNode.getName().equals("Test")) {
            selfNode = rootNode;
        }
        // Check whether the data model tree returned is of type module.
        assertThat((selfNode instanceof YangModule), is(true));

        // Check whether the node type is set properly to module.
        assertThat(selfNode.getNodeType(), is(YangNodeType.MODULE_NODE));

        // Check whether the module name is set correctly.
        YangModule yangNode = (YangModule) selfNode;
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

        String searchDir = "src/test/resources/leafreflinker/intrafile/leafreftoleafref";
        utilManager.createYangFileInfoSet(YangFileScanner.getYangFiles(searchDir));
        utilManager.parseYangFileInfoSet();
        utilManager.createYangNodeSet();
        YangNode selfNode = null;
        // Create YANG node set
        yangLinkerManager.createYangNodeSet(utilManager.getYangNodeSet());

        // Add references to import list.
        yangLinkerManager.addRefToYangFilesImportList(utilManager.getYangNodeSet());

        // Carry out inter-file linking.
        yangLinkerManager.processInterFileLinking(utilManager.getYangNodeSet());

        Iterator<YangNode> yangNodeIterator = utilManager.getYangNodeSet().iterator();

        YangNode rootNode = yangNodeIterator.next();

        if (rootNode.getName().equals("ietf-network")) {
            selfNode = rootNode;
        }
        // Check whether the data model tree returned is of type module.
        assertThat((selfNode instanceof YangModule), is(true));

        // Check whether the node type is set properly to module.
        assertThat(selfNode.getNodeType(), is(YangNodeType.MODULE_NODE));

        // Check whether the module name is set correctly.
        YangModule yangNode = (YangModule) selfNode;
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

        String searchDir = "src/test/resources/leafreflinker/intrafile/leafreftomultileafref";
        utilManager.createYangFileInfoSet(YangFileScanner.getYangFiles(searchDir));
        utilManager.parseYangFileInfoSet();
        utilManager.createYangNodeSet();
        YangNode selfNode = null;
        // Create YANG node set
        yangLinkerManager.createYangNodeSet(utilManager.getYangNodeSet());

        // Add references to import list.
        yangLinkerManager.addRefToYangFilesImportList(utilManager.getYangNodeSet());

        // Carry out inter-file linking.
        yangLinkerManager.processInterFileLinking(utilManager.getYangNodeSet());

        Iterator<YangNode> yangNodeIterator = utilManager.getYangNodeSet().iterator();

        YangNode rootNode = yangNodeIterator.next();

        if (rootNode.getName().equals("Test")) {
            selfNode = rootNode;
        }
        // Check whether the data model tree returned is of type module.
        assertThat((selfNode instanceof YangModule), is(true));

        // Check whether the node type is set properly to module.
        assertThat(selfNode.getNodeType(), is(YangNodeType.MODULE_NODE));

        // Check whether the module name is set correctly.
        YangModule yangNode = (YangModule) selfNode;
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

        String searchDir = "src/test/resources/leafreflinker/intrafile/leafreftoderivedtype";
        utilManager.createYangFileInfoSet(YangFileScanner.getYangFiles(searchDir));
        utilManager.parseYangFileInfoSet();
        utilManager.createYangNodeSet();
        YangNode selfNode = null;
        // Create YANG node set
        yangLinkerManager.createYangNodeSet(utilManager.getYangNodeSet());

        // Add references to import list.
        yangLinkerManager.addRefToYangFilesImportList(utilManager.getYangNodeSet());

        // Carry out inter-file linking.
        yangLinkerManager.processInterFileLinking(utilManager.getYangNodeSet());

        Iterator<YangNode> yangNodeIterator = utilManager.getYangNodeSet().iterator();

        YangNode rootNode = yangNodeIterator.next();

        if (rootNode.getName().equals("ietf-network")) {
            selfNode = rootNode;
        }
        // Check whether the data model tree returned is of type module.
        assertThat((selfNode instanceof YangModule), is(true));

        // Check whether the node type is set properly to module.
        assertThat(selfNode.getNodeType(), is(YangNodeType.MODULE_NODE));

        // Check whether the module name is set correctly.
        YangModule yangNode = (YangModule) selfNode;
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

        String searchDir = "src/test/resources/leafreflinker/intrafile/leafreftomultitypedef";
        utilManager.createYangFileInfoSet(YangFileScanner.getYangFiles(searchDir));
        utilManager.parseYangFileInfoSet();
        utilManager.createYangNodeSet();
        YangNode selfNode = null;
        // Create YANG node set
        yangLinkerManager.createYangNodeSet(utilManager.getYangNodeSet());

        // Add references to import list.
        yangLinkerManager.addRefToYangFilesImportList(utilManager.getYangNodeSet());

        // Carry out inter-file linking.
        yangLinkerManager.processInterFileLinking(utilManager.getYangNodeSet());

        Iterator<YangNode> yangNodeIterator = utilManager.getYangNodeSet().iterator();

        YangNode rootNode = yangNodeIterator.next();

        if (rootNode.getName().equals("Test")) {
            selfNode = rootNode;
        }
        // Check whether the data model tree returned is of type module.
        assertThat((selfNode instanceof YangModule), is(true));

        // Check whether the node type is set properly to module.
        assertThat(selfNode.getNodeType(), is(YangNodeType.MODULE_NODE));

        // Check whether the module name is set correctly.
        YangModule yangNode = (YangModule) selfNode;
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

        String searchDir = "src/test/resources/leafreflinker/intrafile/leafreftotypedefwithleafref";
        utilManager.createYangFileInfoSet(YangFileScanner.getYangFiles(searchDir));
        utilManager.parseYangFileInfoSet();
        utilManager.createYangNodeSet();
        YangNode selfNode = null;
        // Create YANG node set
        yangLinkerManager.createYangNodeSet(utilManager.getYangNodeSet());

        // Add references to import list.
        yangLinkerManager.addRefToYangFilesImportList(utilManager.getYangNodeSet());

        // Carry out inter-file linking.
        yangLinkerManager.processInterFileLinking(utilManager.getYangNodeSet());

        Iterator<YangNode> yangNodeIterator = utilManager.getYangNodeSet().iterator();

        YangNode rootNode = yangNodeIterator.next();

        if (rootNode.getName().equals("Test")) {
            selfNode = rootNode;
        }
        // Check whether the data model tree returned is of type module.
        assertThat((selfNode instanceof YangModule), is(true));

        // Check whether the node type is set properly to module.
        assertThat(selfNode.getNodeType(), is(YangNodeType.MODULE_NODE));

        // Check whether the module name is set correctly.
        YangModule yangNode = (YangModule) selfNode;
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

        String searchDir = "src/test/resources/leafreflinker/intrafile/relativepath/simpleleafref";
        utilManager.createYangFileInfoSet(YangFileScanner.getYangFiles(searchDir));
        utilManager.parseYangFileInfoSet();
        utilManager.createYangNodeSet();
        YangNode selfNode = null;
        // Create YANG node set
        yangLinkerManager.createYangNodeSet(utilManager.getYangNodeSet());

        // Add references to import list.
        yangLinkerManager.addRefToYangFilesImportList(utilManager.getYangNodeSet());

        // Carry out inter-file linking.
        yangLinkerManager.processInterFileLinking(utilManager.getYangNodeSet());

        Iterator<YangNode> yangNodeIterator = utilManager.getYangNodeSet().iterator();

        YangNode rootNode = yangNodeIterator.next();

        if (rootNode.getName().equals("ietf-network")) {
            selfNode = rootNode;
        }
        // Check whether the data model tree returned is of type module.
        assertThat((selfNode instanceof YangModule), is(true));

        // Check whether the node type is set properly to module.
        assertThat(selfNode.getNodeType(), is(YangNodeType.MODULE_NODE));

        // Check whether the module name is set correctly.
        YangModule yangNode = (YangModule) selfNode;
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
    @Ignore
    public void processSelfResolutionWhenLeafrefInModuleReferToGroupingWithInputInRpcRelPath()
            throws IOException, ParserException {

        String searchDir = "src/test/resources/leafreflinker/intrafile/relativepath/leafreftoinputwithgroupinginrpc";
        utilManager.createYangFileInfoSet(YangFileScanner.getYangFiles(searchDir));
        utilManager.parseYangFileInfoSet();
        utilManager.createYangNodeSet();
        YangNode selfNode = null;
        // Create YANG node set
        yangLinkerManager.createYangNodeSet(utilManager.getYangNodeSet());

        // Add references to import list.
        yangLinkerManager.addRefToYangFilesImportList(utilManager.getYangNodeSet());

        // Carry out inter-file linking.
        yangLinkerManager.processInterFileLinking(utilManager.getYangNodeSet());

        Iterator<YangNode> yangNodeIterator = utilManager.getYangNodeSet().iterator();

        YangNode rootNode = yangNodeIterator.next();

        if (rootNode.getName().equals("ietf-network")) {
            selfNode = rootNode;
        }
        // Check whether the data model tree returned is of type module.
        assertThat((selfNode instanceof YangModule), is(true));

        // Check whether the node type is set properly to module.
        assertThat(selfNode.getNodeType(), is(YangNodeType.MODULE_NODE));

        // Check whether the module name is set correctly.
        YangModule yangNode = (YangModule) selfNode;
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
                "YANG file error: The target node, in the leafref path ../../../define/network-id, is invalid.");
        String searchDir = "src/test/resources/leafreflinker/intrafile/relativepath/invalidrelativeancestoraccess";
        utilManager.createYangFileInfoSet(YangFileScanner.getYangFiles(searchDir));
        utilManager.parseYangFileInfoSet();
        utilManager.createYangNodeSet();

        // Create YANG node set
        yangLinkerManager.createYangNodeSet(utilManager.getYangNodeSet());

        // Add references to import list.
        yangLinkerManager.addRefToYangFilesImportList(utilManager.getYangNodeSet());

        // Carry out inter-file linking.
        yangLinkerManager.processInterFileLinking(utilManager.getYangNodeSet());
    }

    /**
     * Checks self resolution when leafref under module refers to invalid node.
     * Inter file linking also has to be done to know the error message with relative path.
     */
    @Test
    public void processSelfResolutionWhenLeafrefInModuleReferToInvalidNodeRelPath()
            throws IOException, ParserException {


        thrown.expect(LinkerException.class);
        thrown.expectMessage(
                "YANG file error: Unable to find base leaf/leaf-list for given leafref path ../define/network-id");
        String searchDir = "src/test/resources/leafreflinker/intrafile/relativepath/invalidnode";
        utilManager.createYangFileInfoSet(YangFileScanner.getYangFiles(searchDir));
        utilManager.parseYangFileInfoSet();
        utilManager.createYangNodeSet();

        // Create YANG node set
        yangLinkerManager.createYangNodeSet(utilManager.getYangNodeSet());

        // Add references to import list.
        yangLinkerManager.addRefToYangFilesImportList(utilManager.getYangNodeSet());

        //Carry out inter-file linking.
        yangLinkerManager.processInterFileLinking(utilManager.getYangNodeSet());
    }

    /**
     * Checks self resolution when leafref of leaf-list under module refers to leaf in container with relative path.
     */
    @Test
    public void processSelfResolutionWhenLeafrefInTypedefReferToContainerRelPath()
            throws IOException, ParserException {

        String searchDir = "src/test/resources/leafreflinker/intrafile/relativepath/leafrefintypedef";
        utilManager.createYangFileInfoSet(YangFileScanner.getYangFiles(searchDir));
        utilManager.parseYangFileInfoSet();
        utilManager.createYangNodeSet();
        YangNode selfNode = null;
        // Create YANG node set
        yangLinkerManager.createYangNodeSet(utilManager.getYangNodeSet());

        // Add references to import list.
        yangLinkerManager.addRefToYangFilesImportList(utilManager.getYangNodeSet());

        // Carry out inter-file linking.
        yangLinkerManager.processInterFileLinking(utilManager.getYangNodeSet());

        Iterator<YangNode> yangNodeIterator = utilManager.getYangNodeSet().iterator();

        YangNode rootNode = yangNodeIterator.next();

        if (rootNode.getName().equals("ietf-network")) {
            selfNode = rootNode;
        }
        // Check whether the data model tree returned is of type module.
        assertThat((selfNode instanceof YangModule), is(true));

        // Check whether the node type is set properly to module.
        assertThat(selfNode.getNodeType(), is(YangNodeType.MODULE_NODE));

        // Check whether the module name is set correctly.
        YangModule yangNode = (YangModule) selfNode;
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

        String searchDir = "src/test/resources/leafreflinker/intrafile/relativepath/leafreftomultileafref";
        utilManager.createYangFileInfoSet(YangFileScanner.getYangFiles(searchDir));
        utilManager.parseYangFileInfoSet();
        utilManager.createYangNodeSet();
        YangNode selfNode = null;
        // Create YANG node set
        yangLinkerManager.createYangNodeSet(utilManager.getYangNodeSet());

        // Add references to import list.
        yangLinkerManager.addRefToYangFilesImportList(utilManager.getYangNodeSet());

        // Carry out inter-file linking.
        yangLinkerManager.processInterFileLinking(utilManager.getYangNodeSet());

        Iterator<YangNode> yangNodeIterator = utilManager.getYangNodeSet().iterator();

        YangNode rootNode = yangNodeIterator.next();

        if (rootNode.getName().equals("Test")) {
            selfNode = rootNode;
        }
        // Check whether the data model tree returned is of type module.
        assertThat((selfNode instanceof YangModule), is(true));

        // Check whether the node type is set properly to module.
        assertThat(selfNode.getNodeType(), is(YangNodeType.MODULE_NODE));

        // Check whether the module name is set correctly.
        YangModule yangNode = (YangModule) selfNode;
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

        String searchDir = "src/test/resources/leafreflinker/intrafile/relativepath/leafreftotypedef";
        utilManager.createYangFileInfoSet(YangFileScanner.getYangFiles(searchDir));
        utilManager.parseYangFileInfoSet();
        utilManager.createYangNodeSet();
        YangNode selfNode = null;
        // Create YANG node set
        yangLinkerManager.createYangNodeSet(utilManager.getYangNodeSet());

        // Add references to import list.
        yangLinkerManager.addRefToYangFilesImportList(utilManager.getYangNodeSet());

        // Carry out inter-file linking.
        yangLinkerManager.processInterFileLinking(utilManager.getYangNodeSet());

        Iterator<YangNode> yangNodeIterator = utilManager.getYangNodeSet().iterator();

        YangNode rootNode = yangNodeIterator.next();

        if (rootNode.getName().equals("Test")) {
            selfNode = rootNode;
        }
        // Check whether the data model tree returned is of type module.
        assertThat((selfNode instanceof YangModule), is(true));

        // Check whether the node type is set properly to module.
        assertThat(selfNode.getNodeType(), is(YangNodeType.MODULE_NODE));

        // Check whether the module name is set correctly.
        YangModule yangNode = (YangModule) selfNode;
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

        String searchDir = "src/test/resources/leafreflinker/intrafile/relativepath/pathlistener";
        utilManager.createYangFileInfoSet(YangFileScanner.getYangFiles(searchDir));
        utilManager.parseYangFileInfoSet();
        utilManager.createYangNodeSet();
        YangNode selfNode = null;
        // Create YANG node set
        yangLinkerManager.createYangNodeSet(utilManager.getYangNodeSet());

        // Add references to import list.
        yangLinkerManager.addRefToYangFilesImportList(utilManager.getYangNodeSet());

        // Carry out inter-file linking.
        yangLinkerManager.processInterFileLinking(utilManager.getYangNodeSet());

        Iterator<YangNode> yangNodeIterator = utilManager.getYangNodeSet().iterator();

        YangNode rootNode = yangNodeIterator.next();

        if (rootNode.getName().equals("PathListener")) {
            selfNode = rootNode;
        }
        // Check whether the data model tree returned is of type module.
        assertThat((selfNode instanceof YangModule), is(true));

        // Check whether the node type is set properly to module.
        assertThat(selfNode.getNodeType(), is(YangNodeType.MODULE_NODE));

        // Check whether the module name is set correctly.
        YangModule yangNode = (YangModule) selfNode;
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
        assertThat(leafref.getPathType(), is(YangPathArgType.ABSOLUTE_PATH));

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
        //TODO : Fill the path predicates
//        assertThat(pathPredicate1.getLeftAxisNode(), is(leafNameInList));
//        assertThat(pathPredicate1.getRightAxisNode(), is(leafInfo));
    }

    /**
     * Checks inter file resolution when leafref refers to multiple leafrefs through many files.
     */
    @Test
    public void processInterFileLeafrefRefersToMultipleLeafrefInMultipleFiles()
            throws IOException, ParserException {

        String searchDir = "src/test/resources/leafreflinker/interfile" +
                "/interfileleafrefreferstomultipleleafrefinmultiplefiles";
        utilManager.createYangFileInfoSet(YangFileScanner.getYangFiles(searchDir));
        utilManager.parseYangFileInfoSet();
        utilManager.createYangNodeSet();
        YangNode refNode1 = null;
        YangNode refNode2 = null;
        YangNode selfNode = null;

        // Create YANG node set
        yangLinkerManager.createYangNodeSet(utilManager.getYangNodeSet());

        // Add references to import list.
        yangLinkerManager.addRefToYangFilesImportList(utilManager.getYangNodeSet());

        // Carry out inter-file linking.
        yangLinkerManager.processInterFileLinking(utilManager.getYangNodeSet());

        for (YangNode rootNode : utilManager.getYangNodeSet()) {
            if (rootNode.getName().equals("ietf-network-topology")) {
                selfNode = rootNode;
            } else if (rootNode.getName().equals("ietf-network")) {
                refNode1 = rootNode;
            } else {
                refNode2 = rootNode;
            }
        }
        // Check whether the data model tree returned is of type module.
        assertThat(selfNode instanceof YangModule, is(true));

        // Check whether the node type is set properly to module.
        assertThat(selfNode.getNodeType(), is(MODULE_NODE));

        // Check whether the module name is set correctly.
        YangModule yangNode = (YangModule) selfNode;
        assertThat(yangNode.getName(), is("ietf-network-topology"));

        YangList list = (YangList) yangNode.getChild().getChild();
        ListIterator<YangLeaf> leafIterator = list.getListOfLeaf().listIterator();
        YangLeaf leafInfo = leafIterator.next();

        assertThat(leafInfo.getName(), is("link-tp"));
        assertThat(leafInfo.getDataType().getDataTypeName(), is("leafref"));
        assertThat(leafInfo.getDataType().getDataType(), is(LEAFREF));

        YangLeafRef leafref = (YangLeafRef) leafInfo.getDataType().getDataTypeExtendedInfo();

        YangLeaf leafInfo2 = (YangLeaf) leafref.getReferredLeafOrLeafList();
        assertThat(leafref.getReferredLeafOrLeafList(), is(leafInfo2));
        assertThat(leafref.getResolvableStatus(), is(RESOLVED));

        assertThat(leafref.getEffectiveDataType().getDataType(),
                is(YangDataTypes.STRING));
    }


    /**
     * Checks addition of if-feature list to leafref.
     */
    @Test
    public void processSelfFileLinkingWithFeatureReferredByLeafref()
            throws IOException, ParserException {

        String searchDir = "src/test/resources/leafreflinker/iffeatuinleafref/simpleleafrefwithiffeature";
        utilManager.createYangFileInfoSet(YangFileScanner.getYangFiles(searchDir));
        utilManager.parseYangFileInfoSet();
        utilManager.createYangNodeSet();
        YangNode selfNode = null;
        // Create YANG node set
        yangLinkerManager.createYangNodeSet(utilManager.getYangNodeSet());

        // Add references to import list.
        yangLinkerManager.addRefToYangFilesImportList(utilManager.getYangNodeSet());

        // Carry out inter-file linking.
        yangLinkerManager.processInterFileLinking(utilManager.getYangNodeSet());

        Iterator<YangNode> yangNodeIterator = utilManager.getYangNodeSet().iterator();

        YangNode rootNode = yangNodeIterator.next();

        if (rootNode.getName().equals("syslog")) {
            selfNode = rootNode;
        }
        // Check whether the data model tree returned is of type module.
        assertThat((selfNode instanceof YangModule), is(true));

        // Check whether the node type is set properly to module.
        assertThat(selfNode.getNodeType(), is(YangNodeType.MODULE_NODE));

        // Check whether the module name is set correctly.
        YangModule yangNode = (YangModule) selfNode;
        assertThat(yangNode.getName(), is("syslog"));

        List<YangFeature> featureList = yangNode.getFeatureList();
        YangFeature feature = featureList.iterator().next();
        assertThat(feature.getName(), is("local-storage"));

        YangContainer container = (YangContainer) yangNode.getChild();
        assertThat(container.getName(), is("speed"));

        List<YangLeaf> listOfLeaf = container.getListOfLeaf();
        YangLeaf leaf = listOfLeaf.iterator().next();
        assertThat(leaf.getName(), is("local-storage-limit"));

        List<YangIfFeature> ifFeatureList = leaf.getIfFeatureList();
        YangIfFeature ifFeature = ifFeatureList.iterator().next();
        assertThat(ifFeature.getName().getName(), is("local-storage"));
        assertThat(ifFeature.getResolvableStatus(), is(ResolvableStatus.RESOLVED));

        ListIterator<YangLeaf> listOfLeafInModule = yangNode.getListOfLeaf().listIterator();
        YangLeaf yangLeaf = listOfLeafInModule.next();
        assertThat(yangLeaf.getName(), is("storage-value"));

        YangLeafRef leafRef = (YangLeafRef) yangLeaf.getDataType().getDataTypeExtendedInfo();

        assertThat(leafRef.getEffectiveDataType().getDataType(), is(YangDataTypes.UINT64));

        List<YangIfFeature> ifFeatureListInLeafref = leafRef.getIfFeatureList();
        YangIfFeature ifFeatureInLeafref = ifFeatureListInLeafref.iterator().next();
        assertThat(ifFeatureInLeafref.getName().getName(), is("local-storage"));
        assertThat(ifFeatureInLeafref.getResolvableStatus(), is(ResolvableStatus.RESOLVED));
    }

    /**
     * Checks addition of if-feature list to leafref when referred leaf is again having leafref in it.
     */
    @Test
    public void processSelfFileLinkingWithFeatureReferredByMultiLeafref()
            throws IOException, ParserException {

        String searchDir = "src/test/resources/leafreflinker/iffeatuinleafref/featurebymultileafref";
        utilManager.createYangFileInfoSet(YangFileScanner.getYangFiles(searchDir));
        utilManager.parseYangFileInfoSet();
        utilManager.createYangNodeSet();
        YangNode selfNode = null;
        // Create YANG node set
        yangLinkerManager.createYangNodeSet(utilManager.getYangNodeSet());

        // Add references to import list.
        yangLinkerManager.addRefToYangFilesImportList(utilManager.getYangNodeSet());

        // Carry out inter-file linking.
        yangLinkerManager.processInterFileLinking(utilManager.getYangNodeSet());

        Iterator<YangNode> yangNodeIterator = utilManager.getYangNodeSet().iterator();

        YangNode rootNode = yangNodeIterator.next();

        if (rootNode.getName().equals("syslog")) {
            selfNode = rootNode;
        }
        // Check whether the data model tree returned is of type module.
        assertThat((selfNode instanceof YangModule), is(true));

        // Check whether the node type is set properly to module.
        assertThat(selfNode.getNodeType(), is(YangNodeType.MODULE_NODE));

        // Check whether the module name is set correctly.
        YangModule yangNode = (YangModule) selfNode;
        assertThat(yangNode.getName(), is("syslog"));

        List<YangFeature> featureList = yangNode.getFeatureList();
        YangFeature feature = featureList.iterator().next();
        assertThat(feature.getName(), is("local-storage"));

        YangContainer container = (YangContainer) yangNode.getChild();
        assertThat(container.getName(), is("speed"));

        List<YangLeaf> listOfLeaf = container.getListOfLeaf();
        YangLeaf leaf = listOfLeaf.iterator().next();
        assertThat(leaf.getName(), is("local-storage-limit"));

        List<YangIfFeature> ifFeatureList = leaf.getIfFeatureList();
        YangIfFeature ifFeature = ifFeatureList.iterator().next();
        assertThat(ifFeature.getName().getName(), is("local-storage"));
        assertThat(ifFeature.getResolvableStatus(), is(ResolvableStatus.RESOLVED));

        ListIterator<YangLeaf> listOfLeafInModule = yangNode.getListOfLeaf().listIterator();
        YangLeaf yangLeaf = listOfLeafInModule.next();
        assertThat(yangLeaf.getName(), is("storage-value"));

        YangLeafRef leafRef = (YangLeafRef) yangLeaf.getDataType().getDataTypeExtendedInfo();

        assertThat(leafRef.getEffectiveDataType().getDataType(), is(YangDataTypes.UINT64));

        List<YangIfFeature> ifFeatureListInLeafref = leafRef.getIfFeatureList();
        YangIfFeature ifFeatureInLeafref = ifFeatureListInLeafref.iterator().next();

        assertThat(ifFeatureInLeafref.getName().getName(), is("main-storage"));
        assertThat(ifFeatureInLeafref.getResolvableStatus(), is(ResolvableStatus.RESOLVED));

        YangIfFeature ifFeatureInLeafref1 = ifFeatureListInLeafref.iterator().next();

        assertThat(ifFeatureInLeafref1.getName().getName(), is("main-storage"));
        assertThat(ifFeatureInLeafref1.getResolvableStatus(), is(ResolvableStatus.RESOLVED));
    }

    /**
     * Checks self resolution when leafref in grouping is copied to augment.
     */
    @Test
    public void processSelfResolutionWhenLeafrefInGroupingIsUnderAugment()
            throws IOException, ParserException {

        String searchDir = "src/test/resources/leafreflinker/intrafile/leafrefInAugment";
        utilManager.createYangFileInfoSet(YangFileScanner.getYangFiles(searchDir));
        utilManager.parseYangFileInfoSet();
        utilManager.createYangNodeSet();
        YangNode selfNode = null;

        // Create YANG node set
        yangLinkerManager.createYangNodeSet(utilManager.getYangNodeSet());

        // Add references to import list.
        yangLinkerManager.addRefToYangFilesImportList(utilManager.getYangNodeSet());

        // Carry out inter-file linking.
        yangLinkerManager.processInterFileLinking(utilManager.getYangNodeSet());

        Iterator<YangNode> yangNodeIterator = utilManager.getYangNodeSet().iterator();

        YangNode rootNode = yangNodeIterator.next();

        if (rootNode.getName().equals("topology")) {
            selfNode = rootNode;
        }

        // Check whether the data model tree returned is of type module.
        assertThat((selfNode instanceof YangModule), is(true));

        // Check whether the node type is set properly to module.
        assertThat(selfNode.getNodeType(), is(YangNodeType.MODULE_NODE));

        // Check whether the module name is set correctly.
        YangModule yangNode = (YangModule) selfNode;
        assertThat(yangNode.getName(), is("topology"));

        ListIterator<YangLeaf> leafIterator;
        YangLeaf leafInfo;

        YangAugment augment = (YangAugment) yangNode.getChild().getNextSibling();

        YangList list = (YangList) augment.getChild().getChild().getChild().getChild().getChild();

        leafIterator = list.getListOfLeaf().listIterator();
        leafInfo = leafIterator.next();

        // Check whether the information in the leaf is correct.
        assertThat(leafInfo.getName(), is("src-tp-ref"));
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
     * Checks self resolution when leafref under grouping's uses.
     */
    @Test
    public void processSelfResolutionWhenLeafrefUnderGroupingUses()
            throws IOException, ParserException {

        String searchDir = "src/test/resources/leafreflinker/intrafile/leafrefinusesundergrouping";
        utilManager.createYangFileInfoSet(YangFileScanner.getYangFiles(searchDir));
        utilManager.parseYangFileInfoSet();
        utilManager.createYangNodeSet();
        YangNode selfNode = null;
        YangNode refNode = null;

        // Create YANG node set
        yangLinkerManager.createYangNodeSet(utilManager.getYangNodeSet());

        // Add references to import list.
        yangLinkerManager.addRefToYangFilesImportList(utilManager.getYangNodeSet());

        // Carry out inter-file linking.
        yangLinkerManager.processInterFileLinking(utilManager.getYangNodeSet());

        Iterator<YangNode> yangNodeIterator = utilManager.getYangNodeSet().iterator();

        YangNode rootNode = yangNodeIterator.next();

        if (rootNode.getName().equals("ietf-network")) {
            selfNode = rootNode;
            refNode = yangNodeIterator.next();
        } else {
            refNode = rootNode;
            selfNode = yangNodeIterator.next();
        }

        // Check whether the data model tree returned is of type module.
        assertThat(selfNode instanceof YangModule, is(true));

        // Check whether the node type is set properly to module.
        assertThat(selfNode.getNodeType(), is(MODULE_NODE));

        // Check whether the module name is set correctly.
        YangModule yangNode = (YangModule) selfNode;
        assertThat(yangNode.getName(), is("ietf-network"));

        // Check whether the module name is set correctly.
        YangModule yangNode1 = (YangModule) refNode;
        assertThat(yangNode1.getName(), is("network"));

        YangContainer yangContainer = (YangContainer) yangNode.getChild().getNextSibling().getNextSibling();
        assertThat(yangContainer.getName(), is("fine"));

        YangContainer yangContainer1 = (YangContainer) yangContainer.getChild().getNextSibling();
        assertThat(yangContainer1.getName(), is("hi"));

        ListIterator<YangLeaf> leafIterator;
        YangLeaf leafInfo;

        leafIterator = yangContainer1.getListOfLeaf().listIterator();
        leafInfo = leafIterator.next();

        // Check whether the information in the leaf is correct.
        assertThat(leafInfo.getName(), is("network-id-ref"));
        assertThat(leafInfo.getDataType().getDataTypeName(), is("leafref"));
        assertThat(leafInfo.getDataType().getDataType(), is(YangDataTypes.LEAFREF));
        YangLeafRef leafref = (YangLeafRef) (leafInfo.getDataType().getDataTypeExtendedInfo());

        // Check whether leafref type got resolved.
        assertThat(leafref.getResolvableStatus(),
                is(ResolvableStatus.RESOLVED));

        // Check the effective type for the leaf.
        assertThat(leafref.getEffectiveDataType().getDataType(),
                is(YangDataTypes.DERIVED));
    }
}

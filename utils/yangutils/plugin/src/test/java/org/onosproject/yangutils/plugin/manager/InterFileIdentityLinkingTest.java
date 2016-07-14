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

import org.apache.maven.plugin.MojoExecutionException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.onosproject.yangutils.datamodel.YangIdentity;
import org.onosproject.yangutils.datamodel.YangIdentityRef;
import org.onosproject.yangutils.datamodel.YangLeaf;
import org.onosproject.yangutils.datamodel.YangLeafList;
import org.onosproject.yangutils.datamodel.YangModule;
import org.onosproject.yangutils.datamodel.YangNode;
import org.onosproject.yangutils.datamodel.YangType;
import org.onosproject.yangutils.datamodel.YangTypeDef;
import org.onosproject.yangutils.datamodel.utils.ResolvableStatus;
import org.onosproject.yangutils.datamodel.utils.builtindatatype.YangDataTypes;
import org.onosproject.yangutils.linker.exceptions.LinkerException;
import org.onosproject.yangutils.linker.impl.YangLinkerManager;
import org.onosproject.yangutils.parser.exceptions.ParserException;
import org.onosproject.yangutils.utils.io.impl.YangFileScanner;

import java.io.IOException;
import java.util.ListIterator;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.onosproject.yangutils.datamodel.YangNodeType.MODULE_NODE;

/**
 * Test cases for testing inter file linking for identity.
 */
public class InterFileIdentityLinkingTest {
    @Rule
    public ExpectedException thrown = ExpectedException.none();

    private final YangUtilManager utilManager = new YangUtilManager();
    private final YangLinkerManager yangLinkerManager = new YangLinkerManager();

    /**
     * Checks inter file feature linking with imported file.
     */
    @Test
    public void processIdentityInImportedFile()
            throws IOException, ParserException, MojoExecutionException {

        String searchDir = "src/test/resources/interfileidentityimport";
        utilManager.createYangFileInfoSet(YangFileScanner.getYangFiles(searchDir));
        utilManager.parseYangFileInfoSet();
        utilManager.createYangNodeSet();

        YangNode selfNode = null;
        YangNode refNode1 = null;
        YangNode refNode2 = null;

        // Create YANG node set
        yangLinkerManager.createYangNodeSet(utilManager.getYangNodeSet());

        // Add references to import list.
        yangLinkerManager.addRefToYangFilesImportList(utilManager.getYangNodeSet());

        // Carry out inter-file linking.
        yangLinkerManager.processInterFileLinking(utilManager.getYangNodeSet());

        for (YangNode rootNode : utilManager.getYangNodeSet()) {
            if (rootNode.getName().equals("IdentityIntraFile")) {
                selfNode = rootNode;
            } else if (rootNode.getName().equals("IdentityInModule")) {
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
        assertThat(yangNode.getName(), is("IdentityIntraFile"));

        YangIdentity yangIdentity = (YangIdentity) yangNode.getChild();
        assertThat(yangIdentity.getName(), is("ipv4-address-family"));
        assertThat(yangIdentity.getBaseNode().getBaseIdentifier().getName(), is("ref-address-family"));
        assertThat(yangIdentity.getBaseNode().getReferredIdentity().getName(), is("ref-address-family"));
        assertThat(yangIdentity.getBaseNode().getResolvableStatus(), is(ResolvableStatus.RESOLVED));

        yangIdentity = (YangIdentity) yangNode.getChild().getNextSibling();
        assertThat(yangIdentity.getName(), is("ipv6-address-family"));
        assertThat(yangIdentity.getBaseNode().getBaseIdentifier().getName(), is("ref-address-family"));
        assertThat(yangIdentity.getBaseNode().getReferredIdentity().getName(), is("ref-address-family"));
        assertThat(yangIdentity.getBaseNode().getResolvableStatus(), is(ResolvableStatus.RESOLVED));

        ListIterator<YangLeaf> leafIterator = yangNode.getListOfLeaf().listIterator();
        YangLeaf leafInfo = leafIterator.next();

        assertThat(leafInfo.getName(), is("tunnel"));
        assertThat(leafInfo.getDataType().getDataTypeName(), is("identityref"));
        assertThat(leafInfo.getDataType().getDataType(), is(YangDataTypes.IDENTITYREF));
        YangIdentityRef yangIdentityRef = (YangIdentityRef) leafInfo.getDataType().getDataTypeExtendedInfo();
        assertThat(yangIdentityRef.getName(), is("ref-address-family"));
        assertThat(yangIdentityRef.getBaseIdentity().getName(), is("ref-address-family"));
        assertThat(yangIdentityRef.getReferredIdentity().getName(), is("ref-address-family"));
        assertThat(yangIdentityRef.getResolvableStatus(), is(ResolvableStatus.RESOLVED));

        ListIterator<YangLeafList> leafListIterator = yangNode.getListOfLeafList().listIterator();
        YangLeafList leafListInfo = leafListIterator.next();

        // Check whether the information in the leaf is correct.
        assertThat(leafListInfo.getName(), is("network-ref"));
        assertThat(leafListInfo.getDataType().getDataTypeName(), is("identityref"));
        assertThat(leafListInfo.getDataType().getDataType(), is(YangDataTypes.IDENTITYREF));
        yangIdentityRef = (YangIdentityRef) (leafListInfo.getDataType().getDataTypeExtendedInfo());
        assertThat(yangIdentityRef.getBaseIdentity().getName(), is("ref-address-family"));
        assertThat(yangIdentityRef.getReferredIdentity().getName(), is("ref-address-family"));
        assertThat(yangIdentityRef.getResolvableStatus(), is(ResolvableStatus.RESOLVED));

    }

    /**
     * Checks inter file feature linking with included file.
     */
    @Test
    public void processIdentityInIncludedFile()
            throws IOException, ParserException, MojoExecutionException {

        String searchDir = "src/test/resources/interfileidentityinlude";
        utilManager.createYangFileInfoSet(YangFileScanner.getYangFiles(searchDir));
        utilManager.parseYangFileInfoSet();
        utilManager.createYangNodeSet();

        YangNode selfNode = null;
        YangNode refNode1 = null;
        YangNode refNode2 = null;

        // Create YANG node set
        yangLinkerManager.createYangNodeSet(utilManager.getYangNodeSet());

        // Carry out linking of sub module with module.
        yangLinkerManager.linkSubModulesToParentModule(utilManager.getYangNodeSet());

        // Add references to import list.
        yangLinkerManager.addRefToYangFilesImportList(utilManager.getYangNodeSet());

        // Add references to include list.
        yangLinkerManager.addRefToYangFilesIncludeList(utilManager.getYangNodeSet());

        // Carry out inter-file linking.
        yangLinkerManager.processInterFileLinking(utilManager.getYangNodeSet());

        for (YangNode rootNode : utilManager.getYangNodeSet()) {
            if (rootNode.getName().equals("syslog3")) {
                selfNode = rootNode;
            } else if (rootNode.getName().equals("syslog4")) {
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
        assertThat(yangNode.getName(), is("syslog3"));

        YangIdentity yangIdentity = (YangIdentity) yangNode.getChild();
        assertThat(yangIdentity.getName(), is("ipv4-address-family"));
        assertThat(yangIdentity.getBaseNode().getBaseIdentifier().getName(), is("ref-address-family"));
        assertThat(yangIdentity.getBaseNode().getReferredIdentity().getName(), is("ref-address-family"));
        assertThat(yangIdentity.getBaseNode().getResolvableStatus(), is(ResolvableStatus.RESOLVED));

        yangIdentity = (YangIdentity) yangNode.getChild().getNextSibling();
        assertThat(yangIdentity.getName(), is("ipv6-address-family"));
        assertThat(yangIdentity.getBaseNode().getBaseIdentifier().getName(), is("ref-address-family"));
        assertThat(yangIdentity.getBaseNode().getReferredIdentity().getName(), is("ref-address-family"));
        assertThat(yangIdentity.getBaseNode().getResolvableStatus(), is(ResolvableStatus.RESOLVED));

        ListIterator<YangLeaf> leafIterator = yangNode.getListOfLeaf().listIterator();
        YangLeaf leafInfo = leafIterator.next();

        assertThat(leafInfo.getName(), is("tunnel"));
        assertThat(leafInfo.getDataType().getDataTypeName(), is("identityref"));
        assertThat(leafInfo.getDataType().getDataType(), is(YangDataTypes.IDENTITYREF));
        YangIdentityRef yangIdentityRef = (YangIdentityRef) leafInfo.getDataType().getDataTypeExtendedInfo();
        assertThat(yangIdentityRef.getName(), is("ref-address-family"));
        assertThat(yangIdentityRef.getBaseIdentity().getName(), is("ref-address-family"));
        assertThat(yangIdentityRef.getReferredIdentity().getName(), is("ref-address-family"));
        assertThat(yangIdentityRef.getResolvableStatus(), is(ResolvableStatus.RESOLVED));

        ListIterator<YangLeafList> leafListIterator = yangNode.getListOfLeafList().listIterator();
        YangLeafList leafListInfo = leafListIterator.next();

        // Check whether the information in the leaf is correct.
        assertThat(leafListInfo.getName(), is("network-ref"));
        assertThat(leafListInfo.getDataType().getDataTypeName(), is("identityref"));
        assertThat(leafListInfo.getDataType().getDataType(), is(YangDataTypes.IDENTITYREF));
        yangIdentityRef = (YangIdentityRef) (leafListInfo.getDataType().getDataTypeExtendedInfo());
        assertThat(yangIdentityRef.getBaseIdentity().getName(), is("ref-address-family"));
        assertThat(yangIdentityRef.getReferredIdentity().getName(), is("ref-address-family"));
        assertThat(yangIdentityRef.getResolvableStatus(), is(ResolvableStatus.RESOLVED));
    }

    /**
     * Checks inter file feature linking with imported file with dependency.
     */
    @Test
    public void processIdentityInImportedFileWithDependency()
            throws IOException, ParserException, MojoExecutionException {

        String searchDir = "src/test/resources/interfileidentityimportdependency";
        utilManager.createYangFileInfoSet(YangFileScanner.getYangFiles(searchDir));
        utilManager.parseYangFileInfoSet();
        utilManager.createYangNodeSet();

        YangNode selfNode = null;
        YangNode refNode1 = null;
        YangNode refNode2 = null;

        // Create YANG node set
        yangLinkerManager.createYangNodeSet(utilManager.getYangNodeSet());

        // Add references to import list.
        yangLinkerManager.addRefToYangFilesImportList(utilManager.getYangNodeSet());

        // Carry out inter-file linking.
        yangLinkerManager.processInterFileLinking(utilManager.getYangNodeSet());

        for (YangNode rootNode : utilManager.getYangNodeSet()) {
            if (rootNode.getName().equals("syslog1")) {
                selfNode = rootNode;
            } else if (rootNode.getName().equals("syslog2")) {
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
        assertThat(yangNode.getName(), is("syslog1"));

        YangIdentity yangIdentity = (YangIdentity) yangNode.getChild();
        assertThat(yangIdentity.getName(), is("ipv4-address-family"));
        assertThat(yangIdentity.getBaseNode().getBaseIdentifier().getName(), is("ref-address-family"));
        assertThat(yangIdentity.getBaseNode().getReferredIdentity().getName(), is("ref-address-family"));
        assertThat(yangIdentity.getBaseNode().getResolvableStatus(), is(ResolvableStatus.RESOLVED));

        yangIdentity = (YangIdentity) yangNode.getChild().getNextSibling();
        assertThat(yangIdentity.getName(), is("ipv6-address-family"));
        assertThat(yangIdentity.getBaseNode().getBaseIdentifier().getName(), is("ref-address-family"));
        assertThat(yangIdentity.getBaseNode().getReferredIdentity().getName(), is("ref-address-family"));
        assertThat(yangIdentity.getBaseNode().getResolvableStatus(), is(ResolvableStatus.RESOLVED));

        ListIterator<YangLeaf> leafIterator = yangNode.getListOfLeaf().listIterator();
        YangLeaf leafInfo = leafIterator.next();

        assertThat(leafInfo.getName(), is("tunnel"));
        assertThat(leafInfo.getDataType().getDataTypeName(), is("identityref"));
        assertThat(leafInfo.getDataType().getDataType(), is(YangDataTypes.IDENTITYREF));
        YangIdentityRef yangIdentityRef = (YangIdentityRef) leafInfo.getDataType().getDataTypeExtendedInfo();
        assertThat(yangIdentityRef.getName(), is("ref-address-family"));
        assertThat(yangIdentityRef.getBaseIdentity().getName(), is("ref-address-family"));
        assertThat(yangIdentityRef.getReferredIdentity().getName(), is("ref-address-family"));
        assertThat(yangIdentityRef.getResolvableStatus(), is(ResolvableStatus.RESOLVED));

        ListIterator<YangLeafList> leafListIterator = yangNode.getListOfLeafList().listIterator();
        YangLeafList leafListInfo = leafListIterator.next();

        // Check whether the information in the leaf is correct.
        assertThat(leafListInfo.getName(), is("network-ref"));
        assertThat(leafListInfo.getDataType().getDataTypeName(), is("identityref"));
        assertThat(leafListInfo.getDataType().getDataType(), is(YangDataTypes.IDENTITYREF));
        yangIdentityRef = (YangIdentityRef) (leafListInfo.getDataType().getDataTypeExtendedInfo());
        assertThat(yangIdentityRef.getBaseIdentity().getName(), is("ref-address-family"));
        assertThat(yangIdentityRef.getReferredIdentity().getName(), is("ref-address-family"));
        assertThat(yangIdentityRef.getResolvableStatus(), is(ResolvableStatus.RESOLVED));
   }

    /**
     * Checks inter file feature linking with included file with dependency.
     */
    @Test
    public void processIdentityInIncludedFileWithDependency()
            throws IOException, ParserException, MojoExecutionException {

        String searchDir = "src/test/resources/interfileidentityincludedependency";
        utilManager.createYangFileInfoSet(YangFileScanner.getYangFiles(searchDir));
        utilManager.parseYangFileInfoSet();
        utilManager.createYangNodeSet();

        YangNode selfNode = null;
        YangNode refNode1 = null;
        YangNode refNode2 = null;

        // Create YANG node set
        yangLinkerManager.createYangNodeSet(utilManager.getYangNodeSet());

        // Carry out linking of sub module with module.
        yangLinkerManager.linkSubModulesToParentModule(utilManager.getYangNodeSet());

        // Add references to include list.
        yangLinkerManager.addRefToYangFilesIncludeList(utilManager.getYangNodeSet());

        // Add references to import list.
        yangLinkerManager.addRefToYangFilesImportList(utilManager.getYangNodeSet());

        // Carry out inter-file linking.
        yangLinkerManager.processInterFileLinking(utilManager.getYangNodeSet());

        for (YangNode rootNode : utilManager.getYangNodeSet()) {
            if (rootNode.getName().equals("syslog1")) {
                selfNode = rootNode;
            } else if (rootNode.getName().equals("syslog2")) {
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
        assertThat(yangNode.getName(), is("syslog1"));

        YangIdentity yangIdentity = (YangIdentity) yangNode.getChild();
        assertThat(yangIdentity.getName(), is("ipv4-address-family"));
        assertThat(yangIdentity.getBaseNode().getBaseIdentifier().getName(), is("ref-address-family"));
        assertThat(yangIdentity.getBaseNode().getReferredIdentity().getName(), is("ref-address-family"));
        assertThat(yangIdentity.getBaseNode().getResolvableStatus(), is(ResolvableStatus.RESOLVED));

        yangIdentity = (YangIdentity) yangNode.getChild().getNextSibling();
        assertThat(yangIdentity.getName(), is("ipv6-address-family"));
        assertThat(yangIdentity.getBaseNode().getBaseIdentifier().getName(), is("ref-address-family"));
        assertThat(yangIdentity.getBaseNode().getReferredIdentity().getName(), is("ref-address-family"));
        assertThat(yangIdentity.getBaseNode().getResolvableStatus(), is(ResolvableStatus.RESOLVED));

        ListIterator<YangLeaf> leafIterator = yangNode.getListOfLeaf().listIterator();
        YangLeaf leafInfo = leafIterator.next();

        assertThat(leafInfo.getName(), is("tunnel"));
        assertThat(leafInfo.getDataType().getDataTypeName(), is("identityref"));
        assertThat(leafInfo.getDataType().getDataType(), is(YangDataTypes.IDENTITYREF));
        YangIdentityRef yangIdentityRef = (YangIdentityRef) leafInfo.getDataType().getDataTypeExtendedInfo();
        assertThat(yangIdentityRef.getName(), is("ref-address-family"));
        assertThat(yangIdentityRef.getBaseIdentity().getName(), is("ref-address-family"));
        assertThat(yangIdentityRef.getReferredIdentity().getName(), is("ref-address-family"));
        assertThat(yangIdentityRef.getResolvableStatus(), is(ResolvableStatus.RESOLVED));

        ListIterator<YangLeafList> leafListIterator = yangNode.getListOfLeafList().listIterator();
        YangLeafList leafListInfo = leafListIterator.next();

        // Check whether the information in the leaf is correct.
        assertThat(leafListInfo.getName(), is("network-ref"));
        assertThat(leafListInfo.getDataType().getDataTypeName(), is("identityref"));
        assertThat(leafListInfo.getDataType().getDataType(), is(YangDataTypes.IDENTITYREF));
        yangIdentityRef = (YangIdentityRef) (leafListInfo.getDataType().getDataTypeExtendedInfo());
        assertThat(yangIdentityRef.getBaseIdentity().getName(), is("ref-address-family"));
        assertThat(yangIdentityRef.getReferredIdentity().getName(), is("ref-address-family"));
        assertThat(yangIdentityRef.getResolvableStatus(), is(ResolvableStatus.RESOLVED));
    }

    /**
     * Checks inter file feature linking with imported file with dependency
     * feature undefined.
     */
    @Test
    public void processIdentityInImportedFileWithDependencyUndefined()
            throws IOException, LinkerException, MojoExecutionException {
        thrown.expect(LinkerException.class);
        thrown.expectMessage("YANG file error: Unable to find base identity for given base");

        String searchDir = "src/test/resources/interfileidentityimportdependencyUndefined";
        utilManager.createYangFileInfoSet(YangFileScanner.getYangFiles(searchDir));
        utilManager.parseYangFileInfoSet();
        utilManager.createYangNodeSet();

        YangNode selfNode = null;
        YangNode refNode1 = null;
        YangNode refNode2 = null;

        // Create YANG node set
        yangLinkerManager.createYangNodeSet(utilManager.getYangNodeSet());

        // Add references to import list.
        yangLinkerManager.addRefToYangFilesImportList(utilManager.getYangNodeSet());

        // Carry out inter-file linking.
        yangLinkerManager.processInterFileLinking(utilManager.getYangNodeSet());
    }

    /**
     * Checks inter file feature linking with included file with dependency
     * feature undefined.
     */
    @Test
    public void processIdentityInIncludedFileWithDependencyUndefined()
            throws IOException, LinkerException, MojoExecutionException {
        thrown.expect(LinkerException.class);
        thrown.expectMessage("YANG file error: Unable to find base identity for given base");

        String searchDir = "src/test/resources/interfileidentityincludedependencyUndefined";
        utilManager.createYangFileInfoSet(YangFileScanner.getYangFiles(searchDir));
        utilManager.parseYangFileInfoSet();
        utilManager.createYangNodeSet();

        YangNode selfNode = null;
        YangNode refNode1 = null;
        YangNode refNode2 = null;

        // Create YANG node set
        yangLinkerManager.createYangNodeSet(utilManager.getYangNodeSet());

        // Carry out linking of sub module with module.
        yangLinkerManager.linkSubModulesToParentModule(utilManager.getYangNodeSet());

        // Add references to import list.
        yangLinkerManager.addRefToYangFilesImportList(utilManager.getYangNodeSet());

        // Add references to include list.
        yangLinkerManager.addRefToYangFilesIncludeList(utilManager.getYangNodeSet());

        // Carry out inter-file linking.
        yangLinkerManager.processInterFileLinking(utilManager.getYangNodeSet());
    }

    /**
     * Checks inter file feature linking with imported file.
     */
    @Test
    public void processIdentityTypedefUnresolvedInImportedFile()
            throws IOException, ParserException, MojoExecutionException {

        String searchDir = "src/test/resources/interfileidentitytypedef";
        utilManager.createYangFileInfoSet(YangFileScanner.getYangFiles(searchDir));
        utilManager.parseYangFileInfoSet();
        utilManager.createYangNodeSet();

        YangNode selfNode = null;
        YangNode refNode1 = null;
        YangNode refNode2 = null;

        // Create YANG node set
        yangLinkerManager.createYangNodeSet(utilManager.getYangNodeSet());

        // Add references to import list.
        yangLinkerManager.addRefToYangFilesImportList(utilManager.getYangNodeSet());

        // Carry out inter-file linking.
        yangLinkerManager.processInterFileLinking(utilManager.getYangNodeSet());

        for (YangNode rootNode : utilManager.getYangNodeSet()) {
            if (rootNode.getName().equals("IdentityIntraFile")) {
                selfNode = rootNode;
            } else if (rootNode.getName().equals("IdentityInModule")) {
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
        assertThat(yangNode.getName(), is("IdentityIntraFile"));

        YangIdentity yangIdentity = (YangIdentity) yangNode.getChild();
        assertThat(yangIdentity.getName(), is("ipv4-address-family"));
        assertThat(yangIdentity.getBaseNode().getBaseIdentifier().getName(), is("ref-address-family"));
        assertThat(yangIdentity.getBaseNode().getReferredIdentity().getName(), is("ref-address-family"));
        assertThat(yangIdentity.getBaseNode().getResolvableStatus(), is(ResolvableStatus.RESOLVED));

        yangIdentity = (YangIdentity) yangNode.getChild().getNextSibling();
        assertThat(yangIdentity.getName(), is("ipv6-address-family"));
        assertThat(yangIdentity.getBaseNode().getBaseIdentifier().getName(), is("ref-address-family"));
        assertThat(yangIdentity.getBaseNode().getReferredIdentity().getName(), is("ref-address-family"));
        assertThat(yangIdentity.getBaseNode().getResolvableStatus(), is(ResolvableStatus.RESOLVED));

        ListIterator<YangLeaf> leafIterator = yangNode.getListOfLeaf().listIterator();
        YangLeaf leafInfo = leafIterator.next();

        assertThat(leafInfo.getName(), is("tunnel"));
        assertThat(leafInfo.getDataType().getDataTypeName(), is("identityref"));
        assertThat(leafInfo.getDataType().getDataType(), is(YangDataTypes.IDENTITYREF));
        YangIdentityRef yangIdentityRef = (YangIdentityRef) leafInfo.getDataType().getDataTypeExtendedInfo();
        assertThat(yangIdentityRef.getName(), is("ref-address-family"));
        assertThat(yangIdentityRef.getBaseIdentity().getName(), is("ref-address-family"));
        assertThat(yangIdentityRef.getReferredIdentity().getName(), is("ref-address-family"));
        assertThat(yangIdentityRef.getResolvableStatus(), is(ResolvableStatus.RESOLVED));

        ListIterator<YangLeafList> leafListIterator = yangNode.getListOfLeafList().listIterator();
        YangLeafList leafListInfo = leafListIterator.next();

        // Check whether the information in the leaf is correct.
        assertThat(leafListInfo.getName(), is("network-ref"));
        assertThat(leafListInfo.getDataType().getDataTypeName(), is("identityref"));
        assertThat(leafListInfo.getDataType().getDataType(), is(YangDataTypes.IDENTITYREF));
        yangIdentityRef = (YangIdentityRef) (leafListInfo.getDataType().getDataTypeExtendedInfo());
        // Check whether leafref type got resolved.
        assertThat(yangIdentityRef.getResolvableStatus(), is(ResolvableStatus.RESOLVED));

        YangTypeDef typedef = (YangTypeDef) yangNode.getChild().getNextSibling().getNextSibling();
        assertThat(typedef.getName(), is("type15"));

        YangType type = typedef.getTypeList().iterator().next();
        assertThat(type.getDataType(), is(YangDataTypes.IDENTITYREF));
        assertThat(type.getDataTypeName(), is("identityref"));

        YangIdentityRef identityRef = (YangIdentityRef) type.getDataTypeExtendedInfo();
        assertThat(identityRef.getName(), is("ref-address-family"));
        assertThat(identityRef.getBaseIdentity().getName(), is("ref-address-family"));
        assertThat(identityRef.getResolvableStatus(), is(ResolvableStatus.UNRESOLVED));
    }

    /**
     * Checks inter file feature linking with imported file.
     */
    @Test
    public void processIdentityTypedefInImportedFile()
            throws IOException, ParserException, MojoExecutionException {

        String searchDir = "src/test/resources/interfileidentitytypedef";
        utilManager.createYangFileInfoSet(YangFileScanner.getYangFiles(searchDir));
        utilManager.parseYangFileInfoSet();
        utilManager.createYangNodeSet();

        YangNode selfNode = null;
        YangNode refNode1 = null;
        YangNode refNode2 = null;

        // Create YANG node set
        yangLinkerManager.createYangNodeSet(utilManager.getYangNodeSet());

        // Add references to import list.
        yangLinkerManager.addRefToYangFilesImportList(utilManager.getYangNodeSet());

        // Carry out inter-file linking.
        yangLinkerManager.processInterFileLinking(utilManager.getYangNodeSet());

        for (YangNode rootNode : utilManager.getYangNodeSet()) {
            if (rootNode.getName().equals("IdentityTypedef")) {
                selfNode = rootNode;
            } else if (rootNode.getName().equals("IdentityInModule")) {
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
        assertThat(yangNode.getName(), is("IdentityTypedef"));

        YangIdentity yangIdentity = (YangIdentity) yangNode.getChild();
        assertThat(yangIdentity.getName(), is("ipv4-address-family"));
        assertThat(yangIdentity.getBaseNode().getBaseIdentifier().getName(), is("ref-address-family"));
        assertThat(yangIdentity.getBaseNode().getReferredIdentity().getName(), is("ref-address-family"));
        assertThat(yangIdentity.getBaseNode().getResolvableStatus(), is(ResolvableStatus.RESOLVED));

        yangIdentity = (YangIdentity) yangNode.getChild().getNextSibling();
        assertThat(yangIdentity.getName(), is("ipv6-address-family"));
        assertThat(yangIdentity.getBaseNode().getBaseIdentifier().getName(), is("ref-address-family"));
        assertThat(yangIdentity.getBaseNode().getReferredIdentity().getName(), is("ref-address-family"));
        assertThat(yangIdentity.getBaseNode().getResolvableStatus(), is(ResolvableStatus.RESOLVED));

        YangTypeDef typedef = (YangTypeDef) yangNode.getChild().getNextSibling().getNextSibling();
        assertThat(typedef.getName(), is("type15"));

        YangType type = typedef.getTypeList().iterator().next();
        assertThat(type.getDataType(), is(YangDataTypes.IDENTITYREF));
        assertThat(type.getDataTypeName(), is("identityref"));

        YangIdentityRef identityRef = (YangIdentityRef) type.getDataTypeExtendedInfo();
        assertThat(identityRef.getName(), is("ref-address-family"));
        assertThat(identityRef.getBaseIdentity().getName(), is("ref-address-family"));

        ListIterator<YangLeaf> leafIterator = yangNode.getListOfLeaf().listIterator();
        YangLeaf leafInfo = leafIterator.next();

        assertThat(leafInfo.getName(), is("tunnel"));
        assertThat(leafInfo.getDataType().getDataTypeName(), is("identityref"));
        assertThat(leafInfo.getDataType().getDataType(), is(YangDataTypes.IDENTITYREF));
        YangIdentityRef yangIdentityRef = (YangIdentityRef) leafInfo.getDataType().getDataTypeExtendedInfo();
        assertThat(yangIdentityRef.getName(), is("ref-address-family"));
        assertThat(yangIdentityRef.getBaseIdentity().getName(), is("ref-address-family"));
        assertThat(yangIdentityRef.getReferredIdentity().getName(), is("ref-address-family"));
        assertThat(yangIdentityRef.getResolvableStatus(), is(ResolvableStatus.RESOLVED));

        ListIterator<YangLeafList> leafListIterator = yangNode.getListOfLeafList().listIterator();
        YangLeafList leafListInfo = leafListIterator.next();

        // Check whether the information in the leaf is correct.
        assertThat(leafListInfo.getName(), is("network-ref"));
        assertThat(leafListInfo.getDataType().getDataTypeName(), is("identityref"));
        assertThat(leafListInfo.getDataType().getDataType(), is(YangDataTypes.IDENTITYREF));
        yangIdentityRef = (YangIdentityRef) (leafListInfo.getDataType().getDataTypeExtendedInfo());
        // Check whether leafref type got resolved.
        assertThat(yangIdentityRef.getResolvableStatus(), is(ResolvableStatus.RESOLVED));
    }
}

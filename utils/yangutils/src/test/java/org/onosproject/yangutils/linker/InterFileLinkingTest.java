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
import java.util.Iterator;
import java.util.ListIterator;
import org.apache.maven.plugin.MojoExecutionException;
import org.junit.Test;
import org.onosproject.yangutils.datamodel.YangDataTypes;
import org.onosproject.yangutils.datamodel.YangDerivedInfo;
import org.onosproject.yangutils.datamodel.YangGrouping;
import org.onosproject.yangutils.datamodel.YangLeaf;
import org.onosproject.yangutils.datamodel.YangModule;
import org.onosproject.yangutils.datamodel.YangNode;
import org.onosproject.yangutils.datamodel.YangNodeType;
import org.onosproject.yangutils.datamodel.YangTypeDef;
import org.onosproject.yangutils.datamodel.YangUses;
import org.onosproject.yangutils.linker.impl.ResolvableStatus;
import org.onosproject.yangutils.linker.impl.YangLinkerManager;
import org.onosproject.yangutils.parser.exceptions.ParserException;
import org.onosproject.yangutils.parser.impl.YangUtilsParserManager;
import org.onosproject.yangutils.plugin.manager.YangFileInfo;
import org.onosproject.yangutils.plugin.manager.YangUtilManager;
import org.onosproject.yangutils.utils.io.impl.YangFileScanner;

import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.onosproject.yangutils.datamodel.YangDataTypes.DERIVED;
import static org.onosproject.yangutils.datamodel.YangDataTypes.STRING;
import static org.onosproject.yangutils.datamodel.YangNodeType.MODULE_NODE;
import static org.onosproject.yangutils.linker.impl.ResolvableStatus.RESOLVED;

/**
 * Test cases for testing inter file linking.
 */
public class InterFileLinkingTest {

    private final YangUtilsParserManager manager = new YangUtilsParserManager();
    private final YangUtilManager utilManager = new YangUtilManager();
    private final YangLinkerManager yangLinkerManager = new YangLinkerManager();

    /**
     * Checks inter file type linking.
     */
    @Test
    public void processInterFileTypeLinking()
            throws IOException, ParserException, MojoExecutionException {

        String searchDir = "src/test/resources/interfiletype";
        utilManager.createYangFileInfoSet(YangFileScanner.getYangFiles(searchDir));
        utilManager.parseYangFileInfoSet();

        YangNode refNode = null;
        YangNode selfNode = null;

        // Add references to import list.
        yangLinkerManager.addRefToYangFilesImportList(utilManager.getYangFileInfoSet());

        // Carry out inter-file linking.
        yangLinkerManager.processInterFileLinking(utilManager.getYangFileInfoSet());

        Iterator<YangFileInfo> yangFileInfoIterator = utilManager.getYangFileInfoSet().iterator();

        YangFileInfo yangFileInfo = yangFileInfoIterator.next();

        if (yangFileInfo.getRootNode().getName().equals("module1")) {
            selfNode = yangFileInfo.getRootNode();
            refNode = yangFileInfoIterator.next().getRootNode();
        } else {
            refNode = yangFileInfo.getRootNode();
            selfNode = yangFileInfoIterator.next().getRootNode();
        }

        // Check whether the data model tree returned is of type module.
        assertThat(selfNode instanceof YangModule, is(true));

        // Check whether the node type is set properly to module.
        assertThat(selfNode.getNodeType(), is(MODULE_NODE));

        // Check whether the module name is set correctly.
        YangModule yangNode = (YangModule) selfNode;
        assertThat(yangNode.getName(), is("module1"));

        ListIterator<YangLeaf> leafIterator = yangNode.getListOfLeaf().listIterator();
        YangLeaf leafInfo = leafIterator.next();

        assertThat(leafInfo.getName(), is("invalid-interval"));
        assertThat(leafInfo.getDataType().getDataTypeName(), is("hello"));
        assertThat(leafInfo.getDataType().getDataType(), is(DERIVED));

        assertThat(((YangDerivedInfo<?>) leafInfo.getDataType().getDataTypeExtendedInfo()).getReferredTypeDef(),
                is((YangTypeDef) refNode.getChild()));

        assertThat(leafInfo.getDataType().getResolvableStatus(), is(RESOLVED));

        YangDerivedInfo<?> derivedInfo = (YangDerivedInfo<?>) leafInfo.getDataType().getDataTypeExtendedInfo();

        // Check for the effective built-in type.
        assertThat(derivedInfo.getEffectiveBuiltInType(), is(STRING));

        // Check for the restriction.
        assertThat(derivedInfo.getLengthRestrictionString(), is(nullValue()));
        assertThat(derivedInfo.getRangeRestrictionString(), is(nullValue()));
        assertThat(derivedInfo.getPatternRestriction(), is(nullValue()));
        assertThat(derivedInfo.getResolvedExtendedInfo(), is(nullValue()));
    }

    /**
     * Checks inter file uses linking.
     */
    @Test
    public void processInterFileUsesLinking()
            throws IOException, ParserException, MojoExecutionException {

        String searchDir = "src/test/resources/interfileuses";
        utilManager.createYangFileInfoSet(YangFileScanner.getYangFiles(searchDir));
        utilManager.parseYangFileInfoSet();

        YangNode refNode = null;
        YangNode selfNode = null;

        // Add references to import list.
        yangLinkerManager.addRefToYangFilesImportList(utilManager.getYangFileInfoSet());

        // Carry out inter-file linking.
        yangLinkerManager.processInterFileLinking(utilManager.getYangFileInfoSet());

        Iterator<YangFileInfo> yangFileInfoIterator = utilManager.getYangFileInfoSet().iterator();

        YangFileInfo yangFileInfo = yangFileInfoIterator.next();

        if (yangFileInfo.getRootNode().getName().equals("module1")) {
            selfNode = yangFileInfo.getRootNode();
            refNode = yangFileInfoIterator.next().getRootNode();
        } else {
            refNode = yangFileInfo.getRootNode();
            selfNode = yangFileInfoIterator.next().getRootNode();
        }

        // Check whether the data model tree returned is of type module.
        assertThat((selfNode instanceof YangModule), is(true));

        // Check whether the node type is set properly to module.
        assertThat(selfNode.getNodeType(), is(YangNodeType.MODULE_NODE));

        // Check whether the module name is set correctly.
        YangModule yangNode = (YangModule) selfNode;
        assertThat(yangNode.getName(), is("module1"));

        ListIterator<YangLeaf> leafIterator;
        YangLeaf leafInfo;

        // Check whether grouping is the sibling of module's child.
        assertThat((refNode.getChild() instanceof YangGrouping), is(true));

        YangGrouping grouping = (YangGrouping) refNode.getChild();
        leafIterator = grouping.getListOfLeaf().listIterator();
        leafInfo = leafIterator.next();

        // Check whether the information in the leaf is correct under grouping.
        assertThat(leafInfo.getName(), is("hello"));
        assertThat(leafInfo.getDataType().getDataTypeName(), is("string"));
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
        assertThat(leafInfo.getName(), is("hello"));
        assertThat(leafInfo.getDataType().getDataTypeName(), is("string"));
        assertThat(leafInfo.getDataType().getDataType(), is(YangDataTypes.STRING));
    }

    /**
     * Checks inter file type linking with include list.
     */
    @Test
    public void processInterFileTypeLinkingWithIncludeList()
            throws IOException, ParserException, MojoExecutionException {

        String searchDir = "src/test/resources/interfiletypewithinclude";
        utilManager.createYangFileInfoSet(YangFileScanner.getYangFiles(searchDir));
        utilManager.parseYangFileInfoSet();

        YangNode refNode = null;
        YangNode selfNode = null;

        // Carry out linking of sub module with module.
        yangLinkerManager.linkSubModulesToParentModule(utilManager.getYangFileInfoSet());

        // Add reference to include list.
        yangLinkerManager.addRefToYangFilesIncludeList(utilManager.getYangFileInfoSet());

        // Carry out inter-file linking.
        yangLinkerManager.processInterFileLinking(utilManager.getYangFileInfoSet());

        Iterator<YangFileInfo> yangFileInfoIterator = utilManager.getYangFileInfoSet().iterator();

        YangFileInfo yangFileInfo = yangFileInfoIterator.next();

        if (yangFileInfo.getRootNode().getName().equals("module1")) {
            selfNode = yangFileInfo.getRootNode();
            refNode = yangFileInfoIterator.next().getRootNode();
        } else {
            refNode = yangFileInfo.getRootNode();
            selfNode = yangFileInfoIterator.next().getRootNode();
        }

        // Check whether the data model tree returned is of type module.
        assertThat(selfNode instanceof YangModule, is(true));

        // Check whether the node type is set properly to module.
        assertThat(selfNode.getNodeType(), is(MODULE_NODE));

        // Check whether the module name is set correctly.
        YangModule yangNode = (YangModule) selfNode;
        assertThat(yangNode.getName(), is("module1"));

        ListIterator<YangLeaf> leafIterator = yangNode.getListOfLeaf().listIterator();
        YangLeaf leafInfo = leafIterator.next();

        assertThat(leafInfo.getName(), is("invalid-interval"));
        assertThat(leafInfo.getDataType().getDataTypeName(), is("hello"));
        assertThat(leafInfo.getDataType().getDataType(), is(DERIVED));

        assertThat(((YangDerivedInfo<?>) leafInfo.getDataType().getDataTypeExtendedInfo()).getReferredTypeDef(),
                is((YangTypeDef) refNode.getChild()));

        assertThat(leafInfo.getDataType().getResolvableStatus(), is(RESOLVED));

        YangDerivedInfo<?> derivedInfo = (YangDerivedInfo<?>) leafInfo.getDataType().getDataTypeExtendedInfo();

        // Check for the effective built-in type.
        assertThat(derivedInfo.getEffectiveBuiltInType(), is(STRING));

        // Check for the restriction.
        assertThat(derivedInfo.getLengthRestrictionString(), is(nullValue()));
        assertThat(derivedInfo.getRangeRestrictionString(), is(nullValue()));
        assertThat(derivedInfo.getPatternRestriction(), is(nullValue()));
        assertThat(derivedInfo.getResolvedExtendedInfo(), is(nullValue()));
    }

    /**
     * Checks inter file uses linking with include list.
     */
    @Test
    public void processInterFileUsesLinkingWithInclude()
            throws IOException, ParserException, MojoExecutionException {

        String searchDir = "src/test/resources/interfileuseswithinclude";
        utilManager.createYangFileInfoSet(YangFileScanner.getYangFiles(searchDir));
        utilManager.parseYangFileInfoSet();

        YangNode refNode = null;
        YangNode selfNode = null;

        // Carry out linking of sub module with module.
        yangLinkerManager.linkSubModulesToParentModule(utilManager.getYangFileInfoSet());

        // Add reference to include list.
        yangLinkerManager.addRefToYangFilesIncludeList(utilManager.getYangFileInfoSet());

        // Carry out inter-file linking.
        yangLinkerManager.processInterFileLinking(utilManager.getYangFileInfoSet());

        Iterator<YangFileInfo> yangFileInfoIterator = utilManager.getYangFileInfoSet().iterator();

        YangFileInfo yangFileInfo = yangFileInfoIterator.next();

        if (yangFileInfo.getRootNode().getName().equals("module1")) {
            selfNode = yangFileInfo.getRootNode();
            refNode = yangFileInfoIterator.next().getRootNode();
        } else {
            refNode = yangFileInfo.getRootNode();
            selfNode = yangFileInfoIterator.next().getRootNode();
        }

        // Check whether the data model tree returned is of type module.
        assertThat((selfNode instanceof YangModule), is(true));

        // Check whether the node type is set properly to module.
        assertThat(selfNode.getNodeType(), is(YangNodeType.MODULE_NODE));

        // Check whether the module name is set correctly.
        YangModule yangNode = (YangModule) selfNode;
        assertThat(yangNode.getName(), is("module1"));

        ListIterator<YangLeaf> leafIterator;
        YangLeaf leafInfo;

        // Check whether grouping is the sibling of module's child.
        assertThat((refNode.getChild() instanceof YangGrouping), is(true));

        YangGrouping grouping = (YangGrouping) refNode.getChild();
        leafIterator = grouping.getListOfLeaf().listIterator();
        leafInfo = leafIterator.next();

        // Check whether the information in the leaf is correct under grouping.
        assertThat(leafInfo.getName(), is("hello"));
        assertThat(leafInfo.getDataType().getDataTypeName(), is("string"));
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
        assertThat(leafInfo.getName(), is("hello"));
        assertThat(leafInfo.getDataType().getDataTypeName(), is("string"));
        assertThat(leafInfo.getDataType().getDataType(), is(YangDataTypes.STRING));
    }

    /**
     * Checks inter file type linking with revision.
     */
    @Test
    public void processInterFileTypeLinkingWithRevision()
            throws IOException, ParserException, MojoExecutionException {

        String searchDir = "src/test/resources/interfiletypewithrevision";
        utilManager.createYangFileInfoSet(YangFileScanner.getYangFiles(searchDir));
        utilManager.parseYangFileInfoSet();

        YangNode refNode = null;
        YangNode selfNode = null;

        // Add references to import list.
        yangLinkerManager.addRefToYangFilesImportList(utilManager.getYangFileInfoSet());

        // Carry out inter-file linking.
        yangLinkerManager.processInterFileLinking(utilManager.getYangFileInfoSet());

        Iterator<YangFileInfo> yangFileInfoIterator = utilManager.getYangFileInfoSet().iterator();

        YangFileInfo yangFileInfo = yangFileInfoIterator.next();

        if (yangFileInfo.getRootNode().getName().equals("module1")) {
            selfNode = yangFileInfo.getRootNode();
            refNode = yangFileInfoIterator.next().getRootNode();
        } else {
            refNode = yangFileInfo.getRootNode();
            selfNode = yangFileInfoIterator.next().getRootNode();
        }

        // Check whether the data model tree returned is of type module.
        assertThat(selfNode instanceof YangModule, is(true));

        // Check whether the node type is set properly to module.
        assertThat(selfNode.getNodeType(), is(MODULE_NODE));

        // Check whether the module name is set correctly.
        YangModule yangNode = (YangModule) selfNode;
        assertThat(yangNode.getName(), is("module1"));

        ListIterator<YangLeaf> leafIterator = yangNode.getListOfLeaf().listIterator();
        YangLeaf leafInfo = leafIterator.next();

        assertThat(leafInfo.getName(), is("invalid-interval"));
        assertThat(leafInfo.getDataType().getDataTypeName(), is("hello"));
        assertThat(leafInfo.getDataType().getDataType(), is(DERIVED));

        assertThat(((YangDerivedInfo<?>) leafInfo.getDataType().getDataTypeExtendedInfo()).getReferredTypeDef(),
                is((YangTypeDef) refNode.getChild()));

        assertThat(leafInfo.getDataType().getResolvableStatus(), is(RESOLVED));

        YangDerivedInfo<?> derivedInfo = (YangDerivedInfo<?>) leafInfo.getDataType().getDataTypeExtendedInfo();

        // Check for the effective built-in type.
        assertThat(derivedInfo.getEffectiveBuiltInType(), is(STRING));

        // Check for the restriction.
        assertThat(derivedInfo.getLengthRestrictionString(), is(nullValue()));
        assertThat(derivedInfo.getRangeRestrictionString(), is(nullValue()));
        assertThat(derivedInfo.getPatternRestriction(), is(nullValue()));
        assertThat(derivedInfo.getResolvedExtendedInfo(), is(nullValue()));
    }

    /**
     * Checks inter file type linking with revision in name.
     */
    @Test
    public void processInterFileTypeLinkingWithRevisionInName()
            throws IOException, ParserException, MojoExecutionException {

        String searchDir = "src/test/resources/interfiletypewithrevisioninname";
        utilManager.createYangFileInfoSet(YangFileScanner.getYangFiles(searchDir));
        utilManager.parseYangFileInfoSet();

        YangNode refNode = null;
        YangNode selfNode = null;

        // Add references to import list.
        yangLinkerManager.addRefToYangFilesImportList(utilManager.getYangFileInfoSet());

        // Carry out inter-file linking.
        yangLinkerManager.processInterFileLinking(utilManager.getYangFileInfoSet());

        Iterator<YangFileInfo> yangFileInfoIterator = utilManager.getYangFileInfoSet().iterator();

        YangFileInfo yangFileInfo = yangFileInfoIterator.next();

        if (yangFileInfo.getRootNode().getName().equals("module1")) {
            selfNode = yangFileInfo.getRootNode();
            refNode = yangFileInfoIterator.next().getRootNode();
        } else {
            refNode = yangFileInfo.getRootNode();
            selfNode = yangFileInfoIterator.next().getRootNode();
        }

        // Check whether the data model tree returned is of type module.
        assertThat(selfNode instanceof YangModule, is(true));

        // Check whether the node type is set properly to module.
        assertThat(selfNode.getNodeType(), is(MODULE_NODE));

        // Check whether the module name is set correctly.
        YangModule yangNode = (YangModule) selfNode;
        assertThat(yangNode.getName(), is("module1"));

        ListIterator<YangLeaf> leafIterator = yangNode.getListOfLeaf().listIterator();
        YangLeaf leafInfo = leafIterator.next();

        assertThat(leafInfo.getName(), is("invalid-interval"));
        assertThat(leafInfo.getDataType().getDataTypeName(), is("hello"));
        assertThat(leafInfo.getDataType().getDataType(), is(DERIVED));

        assertThat(((YangDerivedInfo<?>) leafInfo.getDataType().getDataTypeExtendedInfo()).getReferredTypeDef(),
                is((YangTypeDef) refNode.getChild()));

        assertThat(leafInfo.getDataType().getResolvableStatus(), is(RESOLVED));

        YangDerivedInfo<?> derivedInfo = (YangDerivedInfo<?>) leafInfo.getDataType().getDataTypeExtendedInfo();

        // Check for the effective built-in type.
        assertThat(derivedInfo.getEffectiveBuiltInType(), is(STRING));

        // Check for the restriction.
        assertThat(derivedInfo.getLengthRestrictionString(), is(nullValue()));
        assertThat(derivedInfo.getRangeRestrictionString(), is(nullValue()));
        assertThat(derivedInfo.getPatternRestriction(), is(nullValue()));
        assertThat(derivedInfo.getResolvedExtendedInfo(), is(nullValue()));
    }

    /**
     * Checks hierarchical inter file type linking.
     */
    @Test
    public void processHierarchicalInterFileTypeLinking()
            throws IOException, ParserException, MojoExecutionException {

        String searchDir = "src/test/resources/hierarchicalinterfiletype";
        utilManager.createYangFileInfoSet(YangFileScanner.getYangFiles(searchDir));
        utilManager.parseYangFileInfoSet();

        YangNode refNode1 = null;
        YangNode refNode2 = null;
        YangNode selfNode = null;

        // Add references to import list.
        yangLinkerManager.addRefToYangFilesImportList(utilManager.getYangFileInfoSet());

        // Carry out inter-file linking.
        yangLinkerManager.processInterFileLinking(utilManager.getYangFileInfoSet());

        for (YangFileInfo yangFile : utilManager.getYangFileInfoSet()) {
            if (yangFile.getRootNode().getName().equals("ietf-network-topology")) {
                selfNode = yangFile.getRootNode();
            } else if (yangFile.getRootNode().getName().equals("ietf-network")) {
                refNode1 = yangFile.getRootNode();
            } else {
                refNode2 = yangFile.getRootNode();
            }
        }

        // Check whether the data model tree returned is of type module.
        assertThat(selfNode instanceof YangModule, is(true));

        // Check whether the node type is set properly to module.
        assertThat(selfNode.getNodeType(), is(MODULE_NODE));

        // Check whether the module name is set correctly.
        YangModule yangNode = (YangModule) selfNode;
        assertThat(yangNode.getName(), is("ietf-network-topology"));

        ListIterator<YangLeaf> leafIterator = yangNode.getListOfLeaf().listIterator();
        YangLeaf leafInfo = leafIterator.next();

        assertThat(leafInfo.getName(), is("source-node"));
        assertThat(leafInfo.getDataType().getDataTypeName(), is("node-id"));
        assertThat(leafInfo.getDataType().getDataType(), is(DERIVED));

        assertThat(((YangDerivedInfo<?>) leafInfo.getDataType().getDataTypeExtendedInfo()).getReferredTypeDef(),
                is((YangTypeDef) refNode1.getChild()));

        assertThat(leafInfo.getDataType().getResolvableStatus(), is(RESOLVED));

        YangDerivedInfo<?> derivedInfo = (YangDerivedInfo<?>) leafInfo.getDataType().getDataTypeExtendedInfo();

        // Check for the effective built-in type.
        assertThat(derivedInfo.getEffectiveBuiltInType(), is(STRING));

        // Check for the restriction.
        assertThat(derivedInfo.getLengthRestrictionString(), is(nullValue()));
        assertThat(derivedInfo.getRangeRestrictionString(), is(nullValue()));
        assertThat(derivedInfo.getPatternRestriction(), is(nullValue()));
        assertThat(derivedInfo.getResolvedExtendedInfo(), is(nullValue()));
    }

    /**
     * Checks hierarchical intra with inter file type linking.
     */
    @Test
    public void processHierarchicalIntraWithInterFileTypeLinking()
            throws IOException, ParserException, MojoExecutionException {

        String searchDir = "src/test/resources/hierarchicalintrawithinterfiletype";
        utilManager.createYangFileInfoSet(YangFileScanner.getYangFiles(searchDir));
        utilManager.parseYangFileInfoSet();

        YangNode refNode1 = null;
        YangNode selfNode = null;

        // Add references to import list.
        yangLinkerManager.addRefToYangFilesImportList(utilManager.getYangFileInfoSet());

        // Carry out inter-file linking.
        yangLinkerManager.processInterFileLinking(utilManager.getYangFileInfoSet());

        for (YangFileInfo yangFile : utilManager.getYangFileInfoSet()) {
            if (yangFile.getRootNode().getName().equals("ietf-network")) {
                selfNode = yangFile.getRootNode();
            } else if (yangFile.getRootNode().getName().equals("ietf-inet-types")) {
                refNode1 = yangFile.getRootNode();
            }
        }

        // Check whether the data model tree returned is of type module.
        assertThat(selfNode instanceof YangModule, is(true));

        // Check whether the node type is set properly to module.
        assertThat(selfNode.getNodeType(), is(MODULE_NODE));

        // Check whether the module name is set correctly.
        YangModule yangNode = (YangModule) selfNode;
        assertThat(yangNode.getName(), is("ietf-network"));

        ListIterator<YangLeaf> leafIterator = yangNode.getListOfLeaf().listIterator();
        YangLeaf leafInfo = leafIterator.next();

        assertThat(leafInfo.getName(), is("node-ref"));
        assertThat(leafInfo.getDataType().getDataTypeName(), is("node-id"));
        assertThat(leafInfo.getDataType().getDataType(), is(DERIVED));

        assertThat(((YangDerivedInfo<?>) leafInfo.getDataType().getDataTypeExtendedInfo()).getReferredTypeDef(),
                is((YangTypeDef) selfNode.getChild()));

        assertThat(leafInfo.getDataType().getResolvableStatus(), is(RESOLVED));

        YangDerivedInfo<?> derivedInfo = (YangDerivedInfo<?>) leafInfo.getDataType().getDataTypeExtendedInfo();

        // Check for the effective built-in type.
        assertThat(derivedInfo.getEffectiveBuiltInType(), is(STRING));

        // Check for the restriction.
        assertThat(derivedInfo.getLengthRestrictionString(), is(nullValue()));
        assertThat(derivedInfo.getRangeRestrictionString(), is(nullValue()));
        assertThat(derivedInfo.getPatternRestriction(), is(nullValue()));
        assertThat(derivedInfo.getResolvedExtendedInfo(), is(nullValue()));
    }
}

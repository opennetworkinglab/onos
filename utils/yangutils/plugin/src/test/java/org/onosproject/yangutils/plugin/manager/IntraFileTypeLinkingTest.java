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
import org.junit.Test;
import org.onosproject.yangutils.datamodel.YangContainer;
import org.onosproject.yangutils.datamodel.YangDerivedInfo;
import org.onosproject.yangutils.datamodel.YangLeaf;
import org.onosproject.yangutils.datamodel.YangList;
import org.onosproject.yangutils.datamodel.YangModule;
import org.onosproject.yangutils.datamodel.YangNode;
import org.onosproject.yangutils.datamodel.YangTypeDef;
import org.onosproject.yangutils.linker.exceptions.LinkerException;
import org.onosproject.yangutils.parser.exceptions.ParserException;
import org.onosproject.yangutils.parser.impl.YangUtilsParserManager;

import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.onosproject.yangutils.datamodel.YangDataTypes.BINARY;
import static org.onosproject.yangutils.datamodel.YangDataTypes.DERIVED;
import static org.onosproject.yangutils.datamodel.YangDataTypes.INT32;
import static org.onosproject.yangutils.datamodel.YangDataTypes.STRING;
import static org.onosproject.yangutils.datamodel.YangNodeType.MODULE_NODE;
import static org.onosproject.yangutils.datamodel.utils.ResolvableStatus.INTRA_FILE_RESOLVED;
import static org.onosproject.yangutils.datamodel.utils.ResolvableStatus.RESOLVED;

/**
 * Test cases for testing "type" intra file linking.
 */
public class IntraFileTypeLinkingTest {

    private final YangUtilsParserManager manager = new YangUtilsParserManager();

    /**
     * Checks self resolution when typedef and leaf using type are siblings.
     */
    @Test
    public void processSelfResolutionWhenTypeAndTypedefAtRootLevel()
            throws IOException, ParserException {

        YangNode node = manager.getDataModel("src/test/resources/SelfResolutionWhenTypeAndTypedefAtRootLevel.yang");

        // Check whether the data model tree returned is of type module.
        assertThat(node instanceof YangModule, is(true));

        // Check whether the node type is set properly to module.
        assertThat(node.getNodeType(), is(MODULE_NODE));

        // Check whether the module name is set correctly.
        YangModule yangNode = (YangModule) node;
        assertThat(yangNode.getName(), is("Test"));

        ListIterator<YangLeaf> leafIterator = yangNode.getListOfLeaf().listIterator();
        YangLeaf leafInfo = leafIterator.next();

        assertThat(leafInfo.getName(), is("invalid-interval"));
        assertThat(leafInfo.getDataType().getDataTypeName(), is("hello"));
        assertThat(leafInfo.getDataType().getDataType(), is(DERIVED));

        assertThat(((YangDerivedInfo<?>) leafInfo.getDataType().getDataTypeExtendedInfo()).getReferredTypeDef(),
                is((YangTypeDef) node.getChild()));

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
     * Checks self resolution when typedef and leaf using type are at different
     * level where typedef is at the root.
     */
    @Test
    public void processSelfFileLinkingTypedefAtRootTypeTwoLevelInHierarchy()
            throws IOException, ParserException {

        YangNode node =
                manager.getDataModel("src/test/resources/SelfFileLinkingTypedefAtRootTypeTwoLevelInHierarchy.yang");

        // Check whether the data model tree returned is of type module.
        assertThat((node instanceof YangModule), is(true));

        // Check whether the node type is set properly to module.
        assertThat(node.getNodeType(), is(MODULE_NODE));

        // Check whether the module name is set correctly.
        YangModule yangNode = (YangModule) node;
        assertThat(yangNode.getName(), is("Test"));

        YangContainer yangContainer = (YangContainer) node.getChild().getNextSibling();

        YangList yangList = (YangList) yangContainer.getChild();

        ListIterator<YangLeaf> leafIterator = yangList.getListOfLeaf().listIterator();
        YangLeaf leafInfo = leafIterator.next();

        assertThat(leafInfo.getName(), is("invalid-interval"));
        assertThat(leafInfo.getDataType().getDataTypeName(), is("hello"));
        assertThat(leafInfo.getDataType().getDataType(), is(DERIVED));

        assertThat(((YangDerivedInfo<?>) leafInfo.getDataType().getDataTypeExtendedInfo()).getReferredTypeDef(),
                is((YangTypeDef) node.getChild()));

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
     * Checks self resolution when typedef and leaf using type are at different
     * level where typedef is at the root and defined after parent holder
     * of type.
     */
    @Test
    public void processSelfFileLinkingTypedefAtRootIsAfterContainerHavingType()
            throws IOException, ParserException {

        YangNode node =
                manager.getDataModel("src/test/resources/SelfFileLinkingTypedefAtRootIsAfterContainerHavingType.yang");

        // Check whether the data model tree returned is of type module.
        assertThat((node instanceof YangModule), is(true));

        // Check whether the node type is set properly to module.
        assertThat(node.getNodeType(), is(MODULE_NODE));

        // Check whether the module name is set correctly.
        YangModule yangNode = (YangModule) node;
        assertThat(yangNode.getName(), is("Test"));

        YangContainer yangContainer = (YangContainer) node.getChild();

        YangList yangList = (YangList) yangContainer.getChild();

        ListIterator<YangLeaf> leafIterator = yangList.getListOfLeaf().listIterator();
        YangLeaf leafInfo = leafIterator.next();

        assertThat(leafInfo.getName(), is("invalid-interval"));
        assertThat(leafInfo.getDataType().getDataTypeName(), is("hello"));
        assertThat(leafInfo.getDataType().getDataType(), is(DERIVED));

        assertThat(((YangDerivedInfo<?>) leafInfo.getDataType().getDataTypeExtendedInfo()).getReferredTypeDef(),
                is((YangTypeDef) node.getChild().getNextSibling()));

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
     * Checks self resolution when typedef and leaf using type are at different
     * level where typedef is at the level of root+1 and defined after parent
     * holder of type.
     */
    @Test
    public void processSelfFileLinkingTypedefAtMiddleLevelAfterParentHolder()
            throws IOException, ParserException {

        YangNode node =
                manager.getDataModel("src/test/resources/SelfFileLinkingTypedefAtMiddleLevelAfterParentHolder.yang");

        // Check whether the data model tree returned is of type module.
        assertThat((node instanceof YangModule), is(true));

        // Check whether the node type is set properly to module.
        assertThat(node.getNodeType(), is(MODULE_NODE));

        // Check whether the module name is set correctly.
        YangModule yangNode = (YangModule) node;
        assertThat(yangNode.getName(), is("Test"));

        YangContainer yangContainer = (YangContainer) node.getChild();

        YangList yangList = (YangList) yangContainer.getChild();

        ListIterator<YangLeaf> leafIterator = yangList.getListOfLeaf().listIterator();
        YangLeaf leafInfo = leafIterator.next();

        assertThat(leafInfo.getName(), is("invalid-interval"));
        assertThat(leafInfo.getDataType().getDataTypeName(), is("hello"));
        assertThat(leafInfo.getDataType().getDataType(), is(DERIVED));

        assertThat(((YangDerivedInfo<?>) leafInfo.getDataType().getDataTypeExtendedInfo()).getReferredTypeDef(),
                is((YangTypeDef) yangContainer.getChild().getNextSibling()));

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
     * Checks self resolution when typedef hierarchical references are present.
     */
    @Test
    public void processSelfFileLinkingWithTypdefHierarchicalReference()
            throws IOException, ParserException {

        YangNode node =
                manager.getDataModel("src/test/resources/SelfFileLinkingWithTypdefHierarchicalReference.yang");

        // Check whether the data model tree returned is of type module.
        assertThat((node instanceof YangModule), is(true));

        // Check whether the node type is set properly to module.
        assertThat(node.getNodeType(), is(MODULE_NODE));

        // Check whether the module name is set correctly.
        YangModule yangNode = (YangModule) node;
        assertThat(yangNode.getName(), is("Test"));

        YangContainer yangContainer = (YangContainer) node.getChild().getNextSibling();

        YangList yangList = (YangList) yangContainer.getChild();

        ListIterator<YangLeaf> leafIterator = yangList.getListOfLeaf().listIterator();
        YangLeaf leafInfo = leafIterator.next();

        assertThat(leafInfo.getName(), is("invalid-interval"));
        assertThat(leafInfo.getDataType().getDataTypeName(), is("FirstClass"));
        assertThat(leafInfo.getDataType().getDataType(), is(DERIVED));

        assertThat(((YangDerivedInfo<?>) leafInfo.getDataType().getDataTypeExtendedInfo()).getReferredTypeDef(),
                is((YangTypeDef) yangList.getChild()));
        assertThat(leafInfo.getDataType().getResolvableStatus(),
                is(RESOLVED));

        YangTypeDef typeDef1 = (YangTypeDef) yangList.getChild();

        assertThat(((YangDerivedInfo<?>) typeDef1.getTypeDefBaseType().getDataTypeExtendedInfo()).getReferredTypeDef(),
                is((YangTypeDef) yangContainer.getChild().getNextSibling()));
        assertThat(typeDef1.getTypeDefBaseType().getResolvableStatus(),
                is(RESOLVED));

        YangTypeDef typeDef2 = (YangTypeDef) yangContainer.getChild().getNextSibling();

        assertThat(((YangDerivedInfo<?>) typeDef2.getTypeDefBaseType().getDataTypeExtendedInfo()).getReferredTypeDef(),
                is((YangTypeDef) node.getChild()));
        assertThat(typeDef2.getTypeDefBaseType().getResolvableStatus(),
                is(RESOLVED));

        YangDerivedInfo<?> derivedInfo = (YangDerivedInfo<?>) leafInfo.getDataType().getDataTypeExtendedInfo();

        // Check for the effective built-in type.
        assertThat(derivedInfo.getEffectiveBuiltInType(), is(INT32));

        // Check for the restriction.
        assertThat(derivedInfo.getLengthRestrictionString(), is(nullValue()));
        assertThat(derivedInfo.getRangeRestrictionString(), is(nullValue()));
        assertThat(derivedInfo.getPatternRestriction(), is(nullValue()));
        assertThat(derivedInfo.getResolvedExtendedInfo(), is(nullValue()));
    }

    /**
     * Checks self resolution when typedef hierarchical references are present
     * with last type is unresolved.
     */
    @Test
    public void processSelfFileLinkingWithTypdefHierarchicalRefUnresolved()
            throws IOException, ParserException {

        YangNode node =
                manager.getDataModel("src/test/resources/SelfFileLinkingWithTypdefHierarchicalRefUnresolved.yang");

        // Check whether the data model tree returned is of type module.
        assertThat((node instanceof YangModule), is(true));

        // Check whether the node type is set properly to module.
        assertThat(node.getNodeType(), is(MODULE_NODE));

        // Check whether the module name is set correctly.
        YangModule yangNode = (YangModule) node;
        assertThat(yangNode.getName(), is("Test"));

        YangContainer yangContainer = (YangContainer) node.getChild().getNextSibling();

        YangList yangList = (YangList) yangContainer.getChild();

        ListIterator<YangLeaf> leafIterator = yangList.getListOfLeaf().listIterator();
        YangLeaf leafInfo = leafIterator.next();

        assertThat(leafInfo.getName(), is("invalid-interval"));
        assertThat(leafInfo.getDataType().getDataTypeName(), is("FirstClass"));
        assertThat(leafInfo.getDataType().getDataType(), is(DERIVED));

        assertThat(((YangDerivedInfo<?>) leafInfo.getDataType().getDataTypeExtendedInfo()).getReferredTypeDef(),
                is((YangTypeDef) yangList.getChild()));
        assertThat(leafInfo.getDataType().getResolvableStatus(),
                is(INTRA_FILE_RESOLVED));

        YangTypeDef typeDef1 = (YangTypeDef) yangList.getChild();

        assertThat(((YangDerivedInfo<?>) typeDef1.getTypeDefBaseType().getDataTypeExtendedInfo()).getReferredTypeDef(),
                is((YangTypeDef) yangContainer.getChild().getNextSibling()));
        assertThat(typeDef1.getTypeDefBaseType().getResolvableStatus(),
                is(INTRA_FILE_RESOLVED));

        YangTypeDef typeDef2 = (YangTypeDef) yangContainer.getChild().getNextSibling();

        assertThat(((YangDerivedInfo<?>) typeDef2.getTypeDefBaseType().getDataTypeExtendedInfo()).getReferredTypeDef(),
                is((YangTypeDef) node.getChild()));
        assertThat(typeDef2.getTypeDefBaseType().getResolvableStatus(),
                is(INTRA_FILE_RESOLVED));

        YangDerivedInfo<?> derivedInfo = (YangDerivedInfo<?>) leafInfo.getDataType().getDataTypeExtendedInfo();

        // Check for the effective built-in type.
        assertThat(derivedInfo.getEffectiveBuiltInType(), is(nullValue()));

        // Check for the restriction.
        assertThat(derivedInfo.getLengthRestrictionString(), is(nullValue()));
        assertThat(derivedInfo.getRangeRestrictionString(), is(nullValue()));
        assertThat(derivedInfo.getPatternRestriction(), is(nullValue()));
        assertThat(derivedInfo.getResolvedExtendedInfo(), is(nullValue()));
    }

    /**
     * Checks self resolution when type uses prefix of self module.
     */
    @Test
    public void processSelfFileLinkingWithTypeWithSelfModulePrefix()
            throws IOException, ParserException {

        YangNode node =
                manager.getDataModel("src/test/resources/SelfFileLinkingWithTypeWithSelfModulePrefix.yang");

        // Check whether the data model tree returned is of type module.
        assertThat((node instanceof YangModule), is(true));

        // Check whether the node type is set properly to module.
        assertThat(node.getNodeType(), is(MODULE_NODE));

        // Check whether the module name is set correctly.
        YangModule yangNode = (YangModule) node;
        assertThat(yangNode.getName(), is("Test"));

        YangContainer yangContainer = (YangContainer) node.getChild().getNextSibling();

        YangList yangList = (YangList) yangContainer.getChild();

        ListIterator<YangLeaf> leafIterator = yangList.getListOfLeaf().listIterator();
        YangLeaf leafInfo = leafIterator.next();

        assertThat(leafInfo.getName(), is("invalid-interval"));
        assertThat(leafInfo.getDataType().getDataTypeName(), is("FirstClass"));
        assertThat(leafInfo.getDataType().getDataType(), is(DERIVED));

        assertThat(((YangDerivedInfo<?>) leafInfo.getDataType().getDataTypeExtendedInfo()).getReferredTypeDef(),
                is((YangTypeDef) yangList.getChild()));
        assertThat(leafInfo.getDataType().getResolvableStatus(),
                is(RESOLVED));

        YangTypeDef typeDef1 = (YangTypeDef) yangList.getChild();

        assertThat(((YangDerivedInfo<?>) typeDef1.getTypeDefBaseType().getDataTypeExtendedInfo()).getReferredTypeDef(),
                is((YangTypeDef) yangContainer.getChild().getNextSibling()));
        assertThat(typeDef1.getTypeDefBaseType().getResolvableStatus(),
                is(RESOLVED));

        YangTypeDef typeDef2 = (YangTypeDef) yangContainer.getChild().getNextSibling();

        assertThat(((YangDerivedInfo<?>) typeDef2.getTypeDefBaseType().getDataTypeExtendedInfo()).getReferredTypeDef(),
                is((YangTypeDef) node.getChild()));
        assertThat(typeDef2.getTypeDefBaseType().getResolvableStatus(),
                is(RESOLVED));

        YangDerivedInfo<?> derivedInfo = (YangDerivedInfo<?>) leafInfo.getDataType().getDataTypeExtendedInfo();

        // Check for the effective built-in type.
        assertThat(derivedInfo.getEffectiveBuiltInType(), is(INT32));

        // Check for the restriction.
        assertThat(derivedInfo.getLengthRestrictionString(), is(nullValue()));
        assertThat(derivedInfo.getRangeRestrictionString(), is(nullValue()));
        assertThat(derivedInfo.getPatternRestriction(), is(nullValue()));
        assertThat(derivedInfo.getResolvedExtendedInfo(), is(nullValue()));
    }

    /**
     * Checks self resolution when some type uses prefix of self module
     * some uses external prefix.
     */
    @Test
    public void processSelfFileLinkingWithTypeWithSelfAndExternalPrefixMix()
            throws IOException, ParserException {

        YangNode node =
                manager.getDataModel("src/test/resources/SelfFileLinkingWithTypeWithSelfAndExternalPrefixMix.yang");

        // Check whether the data model tree returned is of type module.
        assertThat((node instanceof YangModule), is(true));

        // Check whether the node type is set properly to module.
        assertThat(node.getNodeType(), is(MODULE_NODE));

        // Check whether the module name is set correctly.
        YangModule yangNode = (YangModule) node;
        assertThat(yangNode.getName(), is("Test"));

        YangContainer yangContainer = (YangContainer) node.getChild().getNextSibling();

        YangList yangList = (YangList) yangContainer.getChild();

        ListIterator<YangLeaf> leafIterator = yangList.getListOfLeaf().listIterator();
        YangLeaf leafInfo = leafIterator.next();

        assertThat(leafInfo.getName(), is("invalid-interval"));
        assertThat(leafInfo.getDataType().getDataTypeName(), is("FirstClass"));
        assertThat(leafInfo.getDataType().getDataType(), is(DERIVED));

        assertThat(((YangDerivedInfo<?>) leafInfo.getDataType().getDataTypeExtendedInfo()).getReferredTypeDef(),
                is((YangTypeDef) yangList.getChild()));
        assertThat(leafInfo.getDataType().getResolvableStatus(),
                is(INTRA_FILE_RESOLVED));

        YangTypeDef typeDef1 = (YangTypeDef) yangList.getChild();

        YangTypeDef typeDef2 = (YangTypeDef) yangContainer.getChild().getNextSibling();

        assertThat(((YangDerivedInfo<?>) typeDef2.getTypeDefBaseType().getDataTypeExtendedInfo()).getReferredTypeDef(),
                is((YangTypeDef) node.getChild()));
        assertThat(typeDef2.getTypeDefBaseType().getResolvableStatus(),
                is(RESOLVED));

        YangDerivedInfo<?> derivedInfo = (YangDerivedInfo<?>) leafInfo.getDataType().getDataTypeExtendedInfo();

        // Check for the effective built-in type.
        assertThat(derivedInfo.getEffectiveBuiltInType(), is(nullValue()));

        // Check for the restriction.
        assertThat(derivedInfo.getLengthRestrictionString(), is(nullValue()));
        assertThat(derivedInfo.getRangeRestrictionString(), is(nullValue()));
        assertThat(derivedInfo.getPatternRestriction(), is(nullValue()));
        assertThat(derivedInfo.getResolvedExtendedInfo(), is(nullValue()));
    }

    /**
     * Check self resolution when type referred typedef is not available in
     * file.
     */
    @Test(expected = LinkerException.class)
    public void processSelfResolutionWhenTypeReferredTypedefNotDefined()
            throws IOException, LinkerException {

        YangNode node =
                manager.getDataModel("src/test/resources/SelfResolutionWhenTypeReferredTypedefNotDefined.yang");
    }

    /**
     * Checks self resolution when typedef and leaf using type are at different
     * level where typedef is is not an ancestor of type.
     */
    @Test(expected = LinkerException.class)
    public void processSelfFileLinkingTypedefNotFound()
            throws IOException, LinkerException {

        YangNode node = manager.getDataModel("src/test/resources/SelfFileLinkingTypedefNotFound.yang");
    }

    /**
     * Checks hierarchical self resolution with self resolution failure scenario.
     */
    @Test(expected = LinkerException.class)
    public void processSelfFileLinkingWithHierarchicalTypeFailureScenario()
            throws IOException, LinkerException {

        YangNode node =
                manager.getDataModel("src/test/resources/SelfFileLinkingWithHierarchicalTypeFailureScenario.yang");
    }

    /**
     * Checks self resolution when typedef and leaf using type are siblings for binary type.
     */
    @Test
    public void processSelfResolutionWhenTypeAndTypedefAtRootLevelForBinary()
            throws IOException, ParserException {

        YangNode node
                = manager.getDataModel("src/test/resources/SelfResolutionWhenTypeAndTypedefAtRootLevelForBinary.yang");

        // Check whether the data model tree returned is of type module.
        assertThat(node instanceof YangModule, is(true));

        // Check whether the node type is set properly to module.
        assertThat(node.getNodeType(), is(MODULE_NODE));

        // Check whether the module name is set correctly.
        YangModule yangNode = (YangModule) node;
        assertThat(yangNode.getName(), is("ospf"));

        ListIterator<YangLeaf> leafIterator = yangNode.getListOfLeaf().listIterator();
        YangLeaf leafInfo = leafIterator.next();

        assertThat(leafInfo.getName(), is("typedef14"));
        assertThat(leafInfo.getDataType().getDataTypeName(), is("type14"));
        assertThat(leafInfo.getDataType().getDataType(), is(DERIVED));

        assertThat(((YangDerivedInfo<?>) leafInfo.getDataType().getDataTypeExtendedInfo()).getReferredTypeDef(),
                is((YangTypeDef) node.getChild()));

        assertThat(leafInfo.getDataType().getResolvableStatus(), is(RESOLVED));

        YangDerivedInfo<?> derivedInfo = (YangDerivedInfo<?>) leafInfo.getDataType().getDataTypeExtendedInfo();

        // Check for the effective built-in type.
        assertThat(derivedInfo.getEffectiveBuiltInType(), is(BINARY));

        // Check for the restriction.
        assertThat(derivedInfo.getLengthRestrictionString(), is(nullValue()));
        assertThat(derivedInfo.getRangeRestrictionString(), is(nullValue()));
        assertThat(derivedInfo.getPatternRestriction(), is(nullValue()));
        assertThat(derivedInfo.getResolvedExtendedInfo(), is(nullValue()));
    }
}

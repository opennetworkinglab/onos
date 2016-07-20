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
import java.math.BigInteger;
import java.util.ListIterator;
import org.junit.Test;
import org.onosproject.yangutils.datamodel.YangDerivedInfo;
import org.onosproject.yangutils.datamodel.YangLeaf;
import org.onosproject.yangutils.datamodel.YangModule;
import org.onosproject.yangutils.datamodel.YangNode;
import org.onosproject.yangutils.datamodel.YangPatternRestriction;
import org.onosproject.yangutils.datamodel.YangRangeInterval;
import org.onosproject.yangutils.datamodel.YangRangeRestriction;
import org.onosproject.yangutils.datamodel.YangStringRestriction;
import org.onosproject.yangutils.datamodel.YangTypeDef;
import org.onosproject.yangutils.datamodel.exceptions.DataModelException;
import org.onosproject.yangutils.datamodel.utils.builtindatatype.YangInt32;
import org.onosproject.yangutils.datamodel.utils.builtindatatype.YangUint64;
import org.onosproject.yangutils.linker.exceptions.LinkerException;
import org.onosproject.yangutils.parser.exceptions.ParserException;
import org.onosproject.yangutils.parser.impl.YangUtilsParserManager;

import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.onosproject.yangutils.datamodel.YangDataTypes.DERIVED;
import static org.onosproject.yangutils.datamodel.YangDataTypes.INT32;
import static org.onosproject.yangutils.datamodel.YangDataTypes.STRING;
import static org.onosproject.yangutils.datamodel.YangNodeType.MODULE_NODE;
import static org.onosproject.yangutils.datamodel.utils.ResolvableStatus.RESOLVED;

/**
 * Test cases for testing restriction resolution.
 */
public final class RestrictionResolutionTest {

    private final YangUtilsParserManager manager = new YangUtilsParserManager();

    /**
     * Checks length restriction in typedef.
     */
    @Test
    public void processLengthRestrictionInTypedef()
            throws IOException, ParserException, DataModelException {

        YangNode node = manager.getDataModel("src/test/resources/LengthRestrictionInTypedef.yang");

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
        assertThat(derivedInfo.getResolvedExtendedInfo(), is(notNullValue()));

        // Check for the restriction value.
        YangStringRestriction stringRestriction = (YangStringRestriction) derivedInfo.getResolvedExtendedInfo();
        YangRangeRestriction lengthRestriction = stringRestriction.getLengthRestriction();

        ListIterator<YangRangeInterval> lengthListIterator = lengthRestriction.getAscendingRangeIntervals()
                .listIterator();

        YangRangeInterval rangeInterval = lengthListIterator.next();

        assertThat(((YangUint64) rangeInterval.getStartValue()).getValue(), is(BigInteger.valueOf(0)));
        assertThat(((YangUint64) rangeInterval.getEndValue()).getValue(), is(BigInteger.valueOf(100)));
    }

    /**
     * Checks length restriction in referred type.
     */
    @Test
    public void processLengthRestrictionInRefType()
            throws IOException, ParserException, DataModelException {

        YangNode node = manager.getDataModel("src/test/resources/LengthRestrictionInRefType.yang");

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
        assertThat(derivedInfo.getLengthRestrictionString(), is(notNullValue()));
        assertThat(derivedInfo.getRangeRestrictionString(), is(nullValue()));
        assertThat(derivedInfo.getPatternRestriction(), is(nullValue()));
        assertThat(derivedInfo.getResolvedExtendedInfo(), is(notNullValue()));

        // Check for the restriction value.
        YangStringRestriction stringRestriction = (YangStringRestriction) derivedInfo.getResolvedExtendedInfo();
        YangRangeRestriction lengthRestriction = stringRestriction.getLengthRestriction();

        ListIterator<YangRangeInterval> lengthListIterator = lengthRestriction.getAscendingRangeIntervals()
                .listIterator();

        YangRangeInterval rangeInterval = lengthListIterator.next();

        assertThat(((YangUint64) rangeInterval.getStartValue()).getValue(), is(BigInteger.valueOf(0)));
        assertThat(((YangUint64) rangeInterval.getEndValue()).getValue(), is(BigInteger.valueOf(100)));
    }

    /**
     * Checks length restriction in typedef and in type with stricter value.
     */
    @Test
    public void processLengthRestrictionInTypedefAndTypeValid()
            throws IOException, ParserException, DataModelException {

        YangNode node = manager.getDataModel("src/test/resources/LengthRestrictionInTypedefAndTypeValid.yang");

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
        assertThat(derivedInfo.getLengthRestrictionString(), is(notNullValue()));
        assertThat(derivedInfo.getRangeRestrictionString(), is(nullValue()));
        assertThat(derivedInfo.getPatternRestriction(), is(nullValue()));
        assertThat(derivedInfo.getResolvedExtendedInfo(), is(notNullValue()));

        // Check for the restriction value.
        YangStringRestriction stringRestriction = (YangStringRestriction) derivedInfo.getResolvedExtendedInfo();
        YangRangeRestriction lengthRestriction = stringRestriction.getLengthRestriction();

        ListIterator<YangRangeInterval> lengthListIterator = lengthRestriction.getAscendingRangeIntervals()
                .listIterator();

        YangRangeInterval rangeInterval1 = lengthListIterator.next();

        assertThat(((YangUint64) rangeInterval1.getStartValue()).getValue(), is(BigInteger.valueOf(0)));
        assertThat(((YangUint64) rangeInterval1.getEndValue()).getValue(), is(BigInteger.valueOf(20)));

        YangRangeInterval rangeInterval2 = lengthListIterator.next();

        assertThat(((YangUint64) rangeInterval2.getStartValue()).getValue(), is(BigInteger.valueOf(201)));
        assertThat(((YangUint64) rangeInterval2.getEndValue()).getValue(), is(BigInteger.valueOf(300)));
    }

    /**
     * Checks length restriction in typedef and in type with not stricter value.
     */
    @Test(expected = LinkerException.class)
    public void processLengthRestrictionInTypedefAndTypeInValid()
            throws IOException, DataModelException {
        YangNode node = manager.getDataModel("src/test/resources/LengthRestrictionInTypedefAndTypeInValid.yang");
    }

    /**
     * Checks range restriction in typedef.
     */
    @Test
    public void processRangeRestrictionInTypedef()
            throws IOException, ParserException, DataModelException {

        YangNode node = manager.getDataModel("src/test/resources/RangeRestrictionInTypedef.yang");

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
        assertThat(derivedInfo.getEffectiveBuiltInType(), is(INT32));

        // Check for the restriction.
        assertThat(derivedInfo.getLengthRestrictionString(), is(nullValue()));
        assertThat(derivedInfo.getRangeRestrictionString(), is(nullValue()));
        assertThat(derivedInfo.getPatternRestriction(), is(nullValue()));
        assertThat(derivedInfo.getResolvedExtendedInfo(), is(notNullValue()));

        // Check for the restriction value.
        YangRangeRestriction rangeRestriction = (YangRangeRestriction) derivedInfo.getResolvedExtendedInfo();

        ListIterator<YangRangeInterval> rangeListIterator = rangeRestriction.getAscendingRangeIntervals()
                .listIterator();

        YangRangeInterval rangeInterval1 = rangeListIterator.next();

        assertThat(((YangInt32) rangeInterval1.getStartValue()).getValue(), is(1));
        assertThat(((YangInt32) rangeInterval1.getEndValue()).getValue(), is(4));

        YangRangeInterval rangeInterval2 = rangeListIterator.next();

        assertThat(((YangInt32) rangeInterval2.getStartValue()).getValue(), is(10));
        assertThat(((YangInt32) rangeInterval2.getEndValue()).getValue(), is(20));
    }

    /**
     * Checks range restriction in referred type.
     */
    @Test
    public void processRangeRestrictionInRefType()
            throws IOException, ParserException, DataModelException {

        YangNode node = manager.getDataModel("src/test/resources/RangeRestrictionInRefType.yang");

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
        assertThat(derivedInfo.getEffectiveBuiltInType(), is(INT32));

        // Check for the restriction.
        assertThat(derivedInfo.getLengthRestrictionString(), is(nullValue()));
        assertThat(derivedInfo.getRangeRestrictionString(), is(notNullValue()));
        assertThat(derivedInfo.getPatternRestriction(), is(nullValue()));
        assertThat(derivedInfo.getResolvedExtendedInfo(), is(notNullValue()));

        // Check for the restriction value.
        YangRangeRestriction rangeRestriction = (YangRangeRestriction) derivedInfo.getResolvedExtendedInfo();

        ListIterator<YangRangeInterval> rangeListIterator = rangeRestriction.getAscendingRangeIntervals()
                .listIterator();

        YangRangeInterval rangeInterval1 = rangeListIterator.next();

        assertThat(((YangInt32) rangeInterval1.getStartValue()).getValue(), is(1));
        assertThat(((YangInt32) rangeInterval1.getEndValue()).getValue(), is(4));

        YangRangeInterval rangeInterval2 = rangeListIterator.next();

        assertThat(((YangInt32) rangeInterval2.getStartValue()).getValue(), is(10));
        assertThat(((YangInt32) rangeInterval2.getEndValue()).getValue(), is(20));
    }

    /**
     * Checks range restriction in typedef and stricter in referred type.
     */
    @Test
    public void processRangeRestrictionInRefTypeAndTypedefValid()
            throws IOException, ParserException, DataModelException {

        YangNode node = manager.getDataModel("src/test/resources/RangeRestrictionInRefTypeAndTypedefValid.yang");

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
        assertThat(derivedInfo.getEffectiveBuiltInType(), is(INT32));

        // Check for the restriction.
        assertThat(derivedInfo.getLengthRestrictionString(), is(nullValue()));
        assertThat(derivedInfo.getRangeRestrictionString(), is(notNullValue()));
        assertThat(derivedInfo.getPatternRestriction(), is(nullValue()));
        assertThat(derivedInfo.getResolvedExtendedInfo(), is(notNullValue()));

        // Check for the restriction value.
        YangRangeRestriction rangeRestriction = (YangRangeRestriction) derivedInfo.getResolvedExtendedInfo();

        ListIterator<YangRangeInterval> rangeListIterator = rangeRestriction.getAscendingRangeIntervals()
                .listIterator();

        YangRangeInterval rangeInterval1 = rangeListIterator.next();

        assertThat(((YangInt32) rangeInterval1.getStartValue()).getValue(), is(1));
        assertThat(((YangInt32) rangeInterval1.getEndValue()).getValue(), is(4));

        YangRangeInterval rangeInterval2 = rangeListIterator.next();

        assertThat(((YangInt32) rangeInterval2.getStartValue()).getValue(), is(10));
        assertThat(((YangInt32) rangeInterval2.getEndValue()).getValue(), is(20));
    }

    /**
     * Checks range restriction in typedef and not stricter in referred type.
     */
    @Test(expected = LinkerException.class)
    public void processRangeRestrictionInRefTypeAndTypedefInValid()
            throws IOException, ParserException, DataModelException {
        YangNode node = manager.getDataModel("src/test/resources/RangeRestrictionInRefTypeAndTypedefInValid.yang");
    }

    /**
     * Checks range restriction for string.
     */
    @Test(expected = ParserException.class)
    public void processRangeRestrictionInString()
            throws IOException, ParserException, DataModelException {
        YangNode node = manager.getDataModel("src/test/resources/RangeRestrictionInString.yang");
    }

    /**
     * Checks range restriction for string in referred type.
     */
    @Test(expected = LinkerException.class)
    public void processRangeRestrictionInStringInRefType()
            throws IOException, DataModelException {
        YangNode node = manager.getDataModel("src/test/resources/RangeRestrictionInStringInRefType.yang");
    }

    /**
     * Checks pattern restriction in typedef.
     */
    @Test
    public void processPatternRestrictionInTypedef()
            throws IOException, ParserException, DataModelException {

        YangNode node = manager.getDataModel("src/test/resources/PatternRestrictionInTypedef.yang");

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
        assertThat(derivedInfo.getResolvedExtendedInfo(), is(notNullValue()));

        // Check for the restriction value.
        YangStringRestriction stringRestriction = (YangStringRestriction) derivedInfo.getResolvedExtendedInfo();
        YangPatternRestriction patternRestriction = stringRestriction.getPatternRestriction();

        ListIterator<String> patternListIterator = patternRestriction.getPatternList().listIterator();
        String pattern1 = patternListIterator.next();

        assertThat(pattern1, is("[a-zA-Z]"));
    }

    /**
     * Checks pattern restriction in referred type.
     */
    @Test
    public void processPatternRestrictionInRefType()
            throws IOException, ParserException, DataModelException {

        YangNode node = manager.getDataModel("src/test/resources/PatternRestrictionInRefType.yang");

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
        assertThat(derivedInfo.getPatternRestriction(), is(notNullValue()));
        assertThat(derivedInfo.getResolvedExtendedInfo(), is(notNullValue()));

        // Check for the restriction value.
        YangStringRestriction stringRestriction = (YangStringRestriction) derivedInfo.getResolvedExtendedInfo();
        YangPatternRestriction patternRestriction = stringRestriction.getPatternRestriction();

        ListIterator<String> patternListIterator = patternRestriction.getPatternList().listIterator();
        String pattern1 = patternListIterator.next();

        assertThat(pattern1, is("[a-zA-Z]"));
    }

    /**
     * Checks pattern restriction in referred type and typedef.
     */
    @Test
    public void processPatternRestrictionInRefTypeAndTypedef()
            throws IOException, ParserException, DataModelException {

        YangNode node = manager.getDataModel("src/test/resources/PatternRestrictionInRefTypeAndTypedef.yang");

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
        assertThat(derivedInfo.getPatternRestriction(), is(notNullValue()));
        assertThat(derivedInfo.getResolvedExtendedInfo(), is(notNullValue()));

        // Check for the restriction value.
        YangStringRestriction stringRestriction = (YangStringRestriction) derivedInfo.getResolvedExtendedInfo();
        YangPatternRestriction patternRestriction = stringRestriction.getPatternRestriction();

        ListIterator<String> patternListIterator = patternRestriction.getPatternList().listIterator();
        String pattern1 = patternListIterator.next();

        assertThat(pattern1, is("[a-zA-Z]"));

        String pattern2 = patternListIterator.next();

        assertThat(pattern2, is("[0-9]"));
    }

    /**
     * Checks multiple pattern restriction in referred type and typedef.
     */
    @Test
    public void processMultiplePatternRestriction()
            throws IOException, ParserException, DataModelException {

        YangNode node = manager.getDataModel("src/test/resources/MultiplePatternRestrictionInRefTypeAndTypedef.yang");

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
        assertThat(derivedInfo.getPatternRestriction(), is(notNullValue()));
        assertThat(derivedInfo.getResolvedExtendedInfo(), is(notNullValue()));

        // Check for the restriction value.
        YangStringRestriction stringRestriction = (YangStringRestriction) derivedInfo.getResolvedExtendedInfo();
        YangPatternRestriction patternRestriction = stringRestriction.getPatternRestriction();

        ListIterator<String> patternListIterator = patternRestriction.getPatternList().listIterator();
        String pattern1 = patternListIterator.next();

        assertThat(pattern1, is("[a-z]"));

        String pattern2 = patternListIterator.next();

        assertThat(pattern2, is("[A-Z]"));

        String pattern3 = patternListIterator.next();

        assertThat(pattern3, is("[0-9]"));

        String pattern4 = patternListIterator.next();

        assertThat(pattern4, is("[\\n]"));
    }

    /**
     * Checks multiple pattern and length restriction in referred type and
     * typedef.
     */
    @Test
    public void processMultiplePatternAndLengthRestriction()
            throws IOException, ParserException, DataModelException {

        YangNode node = manager.getDataModel("src/test/resources/MultiplePatternAndLengthRestriction.yang");

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
        assertThat(derivedInfo.getLengthRestrictionString(), is(notNullValue()));
        assertThat(derivedInfo.getRangeRestrictionString(), is(nullValue()));
        assertThat(derivedInfo.getPatternRestriction(), is(notNullValue()));
        assertThat(derivedInfo.getResolvedExtendedInfo(), is(notNullValue()));

        // Check for the restriction value.
        YangStringRestriction stringRestriction = (YangStringRestriction) derivedInfo.getResolvedExtendedInfo();

        // Check for pattern restriction.
        YangPatternRestriction patternRestriction = stringRestriction.getPatternRestriction();
        ListIterator<String> patternListIterator = patternRestriction.getPatternList().listIterator();
        String pattern1 = patternListIterator.next();

        assertThat(pattern1, is("[a-z]"));

        String pattern2 = patternListIterator.next();

        assertThat(pattern2, is("[A-Z]"));

        String pattern3 = patternListIterator.next();

        assertThat(pattern3, is("[0-9]"));

        String pattern4 = patternListIterator.next();

        assertThat(pattern4, is("[\\n]"));

        // Check for length restriction.
        YangRangeRestriction lengthRestriction = stringRestriction.getLengthRestriction();
        ListIterator<YangRangeInterval> lengthListIterator = lengthRestriction.getAscendingRangeIntervals()
                .listIterator();

        YangRangeInterval rangeInterval1 = lengthListIterator.next();

        assertThat(((YangUint64) rangeInterval1.getStartValue()).getValue(), is(BigInteger.valueOf(0)));
        assertThat(((YangUint64) rangeInterval1.getEndValue()).getValue(), is(BigInteger.valueOf(20)));

        YangRangeInterval rangeInterval2 = lengthListIterator.next();

        assertThat(((YangUint64) rangeInterval2.getStartValue()).getValue(), is(BigInteger.valueOf(201)));
        assertThat(((YangUint64) rangeInterval2.getEndValue()).getValue(), is(BigInteger.valueOf(300)));
    }

    /**
     * Checks multiple pattern and length restriction in referred type and
     * typedef.
     */
    @Test
    public void processMultiplePatternAndLengthRestrictionValid()
            throws IOException, ParserException, DataModelException {

        YangNode node = manager.getDataModel("src/test/resources/MultiplePatternAndLengthRestrictionValid.yang");

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
        assertThat(derivedInfo.getLengthRestrictionString(), is(notNullValue()));
        assertThat(derivedInfo.getRangeRestrictionString(), is(nullValue()));
        assertThat(derivedInfo.getPatternRestriction(), is(notNullValue()));
        assertThat(derivedInfo.getResolvedExtendedInfo(), is(notNullValue()));

        // Check for the restriction value.
        YangStringRestriction stringRestriction = (YangStringRestriction) derivedInfo.getResolvedExtendedInfo();

        // Check for pattern restriction.
        YangPatternRestriction patternRestriction = stringRestriction.getPatternRestriction();
        ListIterator<String> patternListIterator = patternRestriction.getPatternList().listIterator();
        String pattern1 = patternListIterator.next();

        assertThat(pattern1, is("[a-z]"));

        String pattern2 = patternListIterator.next();

        assertThat(pattern2, is("[A-Z]"));

        String pattern3 = patternListIterator.next();

        assertThat(pattern3, is("[0-9]"));

        String pattern4 = patternListIterator.next();

        assertThat(pattern4, is("[\\n]"));

        // Check for length restriction.
        YangRangeRestriction lengthRestriction = stringRestriction.getLengthRestriction();
        ListIterator<YangRangeInterval> lengthListIterator = lengthRestriction.getAscendingRangeIntervals()
                .listIterator();

        YangRangeInterval rangeInterval1 = lengthListIterator.next();

        assertThat(((YangUint64) rangeInterval1.getStartValue()).getValue(), is(BigInteger.valueOf(0)));
        assertThat(((YangUint64) rangeInterval1.getEndValue()).getValue(), is(BigInteger.valueOf(20)));

        YangRangeInterval rangeInterval2 = lengthListIterator.next();

        assertThat(((YangUint64) rangeInterval2.getStartValue()).getValue(), is(BigInteger.valueOf(100)));
        assertThat(((YangUint64) rangeInterval2.getEndValue()).getValue(),
                is(new BigInteger("18446744073709551615")));
    }

    /**
     * Checks multiple pattern and length restriction in referred type and
     * typedef invalid scenario.
     */
    @Test(expected = LinkerException.class)
    public void processMultiplePatternAndLengthRestrictionInValid()
            throws IOException, DataModelException {
        YangNode node = manager.getDataModel("src/test/resources/MultiplePatternAndLengthRestrictionInValid.yang");
    }
}

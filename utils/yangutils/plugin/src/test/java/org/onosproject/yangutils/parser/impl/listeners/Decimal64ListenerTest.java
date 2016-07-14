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

package org.onosproject.yangutils.parser.impl.listeners;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.onosproject.yangutils.datamodel.YangDecimal64;
import org.onosproject.yangutils.datamodel.YangDerivedInfo;
import org.onosproject.yangutils.datamodel.YangLeaf;
import org.onosproject.yangutils.datamodel.YangModule;
import org.onosproject.yangutils.datamodel.YangNode;
import org.onosproject.yangutils.datamodel.YangNodeType;
import org.onosproject.yangutils.datamodel.YangRangeRestriction;
import org.onosproject.yangutils.datamodel.YangRangeInterval;
import org.onosproject.yangutils.datamodel.YangType;
import org.onosproject.yangutils.datamodel.YangTypeDef;
import org.onosproject.yangutils.datamodel.exceptions.DataModelException;
import org.onosproject.yangutils.linker.exceptions.LinkerException;
import org.onosproject.yangutils.parser.exceptions.ParserException;
import org.onosproject.yangutils.parser.impl.YangUtilsParserManager;
import org.onosproject.yangutils.datamodel.utils.builtindatatype.YangDataTypes;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ListIterator;

/**
 * Test cases for decimal64 listener.
 */
public class Decimal64ListenerTest {
    @Rule
    public ExpectedException thrown = ExpectedException.none();

    private final YangUtilsParserManager manager = new YangUtilsParserManager();

    /**
     * Checks decimal64 statement with fraction-digits.
     */
    @Test
    public void processDecimal64TypeStatement() throws IOException, ParserException {

        YangNode node = manager.getDataModel("src/test/resources/decimal64/Decimal64TypeStatement.yang");

        // Check whether the data model tree returned is of type module.
        assertThat((node instanceof YangModule), is(true));

        // Check whether the node type is set properly to module.
        assertThat(node.getNodeType(), is(YangNodeType.MODULE_NODE));

        // Check whether the module name is set correctly.
        YangModule yangNode = (YangModule) node;
        assertThat(yangNode.getName(), is("Test"));

        ListIterator<YangLeaf> leafIterator = yangNode.getListOfLeaf().listIterator();
        YangLeaf leafInfo = leafIterator.next();

        assertThat(leafInfo.getName(), is("validDecimal"));
        assertThat(leafInfo.getDataType().getDataTypeName(), is("decimal64"));
        assertThat(leafInfo.getDataType().getDataType(), is(YangDataTypes.DECIMAL64));
        assertThat(((YangDecimal64) leafInfo.getDataType().getDataTypeExtendedInfo()).getFractionDigit(),
                is(2));
    }

    /**
     * Checks decimal64 statement with range statement.
     */
    @Test
    public void processDecimal64TypeWithRangeStatement() throws IOException, ParserException {

        YangNode node = manager.getDataModel("src/test/resources/decimal64/Decimal64TypeWithRangeStatement.yang");

        // Check whether the data model tree returned is of type module.
        assertThat((node instanceof YangModule), is(true));

        // Check whether the node type is set properly to module.
        assertThat(node.getNodeType(), is(YangNodeType.MODULE_NODE));

        // Check whether the module name is set correctly.
        YangModule yangNode = (YangModule) node;
        assertThat(yangNode.getName(), is("Test"));

        ListIterator<YangLeaf> leafIterator = yangNode.getListOfLeaf().listIterator();
        YangLeaf leafInfo = leafIterator.next();

        assertThat(leafInfo.getName(), is("validDecimal"));
        assertThat(leafInfo.getDataType().getDataTypeName(), is("decimal64"));
        assertThat(leafInfo.getDataType().getDataType(), is(YangDataTypes.DECIMAL64));
        assertThat(((YangDecimal64) leafInfo.getDataType().getDataTypeExtendedInfo()).getFractionDigit(),
                   is(8));

        YangRangeRestriction rangeRestriction = ((YangDecimal64<YangRangeRestriction>) leafInfo.getDataType()
                .getDataTypeExtendedInfo())
                .getRangeRestrictedExtendedInfo();

        ListIterator<YangRangeInterval> rangeListIterator = rangeRestriction.getAscendingRangeIntervals()
                .listIterator();
        YangRangeInterval rangeInterval = rangeListIterator.next();
        assertThat(((YangDecimal64) rangeInterval.getStartValue()).getValue().doubleValue(), is(-92233720368.54775808));
        assertThat(((YangDecimal64) rangeInterval.getEndValue()).getValue().doubleValue(), is(92233720368.54775807));
    }

    /**
     * Successful validation of decimal64 statement.
     */
    @Test
    public void processDecimal64ValueSuccessfulValidation() throws IOException, ParserException, DataModelException {

        YangNode node = manager.getDataModel("src/test/resources/decimal64/Decimal64TypeValidation.yang");

        // Check whether the data model tree returned is of type module.
        assertThat((node instanceof YangModule), is(true));

        // Check whether the node type is set properly to module.
        assertThat(node.getNodeType(), is(YangNodeType.MODULE_NODE));

        // Check whether the module name is set correctly.
        YangModule yangNode = (YangModule) node;
        assertThat(yangNode.getName(), is("Test"));

        ListIterator<YangLeaf> leafIterator = yangNode.getListOfLeaf().listIterator();
        YangLeaf leafInfo = leafIterator.next();
        YangDecimal64 decimal64 = (YangDecimal64) leafInfo.getDataType().getDataTypeExtendedInfo();

        assertThat(leafInfo.getName(), is("validDecimal"));
        assertThat(leafInfo.getDataType().getDataTypeName(), is("decimal64"));
        assertThat(leafInfo.getDataType().getDataType(), is(YangDataTypes.DECIMAL64));
        assertThat(decimal64.getFractionDigit(), is(18));

        decimal64.setValue(new BigDecimal(-9.223372036854775808));
        decimal64.validateValue();
        decimal64.setValue(new BigDecimal(9.223372036854775807));
        decimal64.validateValue();
    }

    /**
     * Failure validation of decimal64 statement.
     */
    @Test
    public void processDecimal64ValueFailureValidation() throws IOException, ParserException, DataModelException {
        thrown.expect(DataModelException.class);
        thrown.expectMessage("YANG file error : decimal64 validation failed.");

        YangNode node = manager.getDataModel("src/test/resources/decimal64/Decimal64TypeValidation.yang");

        // Check whether the data model tree returned is of type module.
        assertThat((node instanceof YangModule), is(true));

        // Check whether the node type is set properly to module.
        assertThat(node.getNodeType(), is(YangNodeType.MODULE_NODE));

        // Check whether the module name is set correctly.
        YangModule yangNode = (YangModule) node;
        assertThat(yangNode.getName(), is("Test"));

        ListIterator<YangLeaf> leafIterator = yangNode.getListOfLeaf().listIterator();
        YangLeaf leafInfo = leafIterator.next();
        YangDecimal64 decimal64 = (YangDecimal64) leafInfo.getDataType().getDataTypeExtendedInfo();

        assertThat(leafInfo.getName(), is("validDecimal"));
        assertThat(leafInfo.getDataType().getDataTypeName(), is("decimal64"));
        assertThat(leafInfo.getDataType().getDataType(), is(YangDataTypes.DECIMAL64));
        assertThat(decimal64.getFractionDigit(), is(18));

        decimal64.setValue(new BigDecimal(-92233720368547758.08));
        // validation should fail
        decimal64.validateValue();
    }

    /**
     * Validation of invalid maximum value limit of fraction-digits.
     */
    @Test
    public void processDecimal64InvalidMaxFraction() throws IOException, ParserException, DataModelException {
        thrown.expect(ParserException.class);
        thrown.expectMessage("YANG file error : fraction-digits value should be between 1 and 18.");

        manager.getDataModel("src/test/resources/decimal64/Decimal64TypeInvalidMaxValueFraction.yang");
    }

    /**
     * Validation of invalid (0) minimum value limit of fraction-digits.
     */
    @Test
    public void processDecimal64InvalidMinFraction1() throws IOException, ParserException, DataModelException {
        thrown.expect(ParserException.class);
        thrown.expectMessage("YANG file error : fraction-digits value should be between 1 and 18.");

        manager.getDataModel("src/test/resources/decimal64/Decimal64TypeInvalidMinValueFraction1.yang");
    }

    /**
     * Validation of invalid (-1) minimum value limit of fraction-digits.
     */
    @Test
    public void processDecimal64InvalidMinFraction2() throws IOException, ParserException, DataModelException {
        thrown.expect(ParserException.class);
        thrown.expectMessage("YANG file error : fraction-digits value should be between 1 and 18.");

        manager.getDataModel("src/test/resources/decimal64/Decimal64TypeInvalidMinValueFraction2.yang");
    }

    /**
     * Validation of decimal64 range statement.
     */
    @Test
    public void processDecimal64TypeWithMultiValueRangeStatement() throws IOException, ParserException {

        YangNode node = manager.getDataModel("src/test/resources/decimal64/Decimal64TypeWithMultiValueRangeStmnt.yang");

        // Check whether the data model tree returned is of type module.
        assertThat((node instanceof YangModule), is(true));

        // Check whether the node type is set properly to module.
        assertThat(node.getNodeType(), is(YangNodeType.MODULE_NODE));

        // Check whether the module name is set correctly.
        YangModule yangNode = (YangModule) node;
        assertThat(yangNode.getName(), is("Test"));

        ListIterator<YangLeaf> leafIterator = yangNode.getListOfLeaf().listIterator();
        YangLeaf leafInfo = leafIterator.next();

        assertThat(leafInfo.getName(), is("validDecimal"));
        assertThat(leafInfo.getDataType().getDataTypeName(), is("decimal64"));
        assertThat(leafInfo.getDataType().getDataType(), is(YangDataTypes.DECIMAL64));
        assertThat(((YangDecimal64) leafInfo.getDataType().getDataTypeExtendedInfo()).getFractionDigit(),
                   is(18));

        YangRangeRestriction rangeRestriction = ((YangDecimal64<YangRangeRestriction>) leafInfo.getDataType()
                .getDataTypeExtendedInfo())
                .getRangeRestrictedExtendedInfo();

        ListIterator<YangRangeInterval> rangeListIterator = rangeRestriction.getAscendingRangeIntervals()
                .listIterator();
        // range "1 .. 3.14 | 10 | 20..max";
        // check first range 1 .. 3.14
        YangRangeInterval rangeInterval = rangeListIterator.next();
        assertThat(((YangDecimal64) rangeInterval.getStartValue()).getValue().doubleValue(), is(-9.22));
        assertThat(((YangDecimal64) rangeInterval.getEndValue()).getValue().doubleValue(), is(7.22));
        // check second range 10
        rangeInterval = rangeListIterator.next();
        assertThat(((YangDecimal64) rangeInterval.getStartValue()).getValue().doubleValue(), is(8.0));
        assertThat(((YangDecimal64) rangeInterval.getEndValue()).getValue().doubleValue(), is(8.0));
        // check third range 20..max
        rangeInterval = rangeListIterator.next();
        assertThat(((YangDecimal64) rangeInterval.getStartValue()).getValue().doubleValue(), is(9.0));
        assertThat(((YangDecimal64) rangeInterval.getEndValue()).getValue().doubleValue(), is(9.223372036854776));
    }

    /**
     * Validation of decimal64 with invalid range.
     */
    @Test
    public void processDecimal64InvalidRange() throws IOException, ParserException, DataModelException {
        thrown.expect(ParserException.class);
        thrown.expectMessage("YANG file error : decimal64 validation failed.");

        manager.getDataModel("src/test/resources/decimal64/Decimal64TypeInvalidRangeStmnt.yang");
    }

    /**
     * Validation of decimal64 without fraction-digits. Fraction-digits must be present for decimal64.
     */
    @Test
    public void processDecimal64WithoutFraction() throws IOException, ParserException, DataModelException {
        thrown.expect(ParserException.class);
        thrown.expectMessage("YANG file error : a type decimal64 must have fraction-digits statement.");

        manager.getDataModel("src/test/resources/decimal64/Decimal64TypeWithoutFraction.yang");
    }

    /**
     * Checks decimal64 with typedef statement.
     */
    @Test
    public void processDecimal64TypedefStatement() throws IOException, ParserException {

        YangNode node = manager.getDataModel("src/test/resources/decimal64/Decimal64TypedefStatement.yang");

        // Check whether the data model tree returned is of type module.
        assertThat((node instanceof YangModule), is(true));

        // Check whether the node type is set properly to module.
        assertThat(node.getNodeType(), is(YangNodeType.MODULE_NODE));

        // Check whether the module name is set correctly.
        YangModule yangNode = (YangModule) node;
        assertThat(yangNode.getName(), is("Test"));

        ListIterator<YangLeaf> leafIterator = yangNode.getListOfLeaf().listIterator();
        YangLeaf leafInfo = leafIterator.next();

        assertThat(leafInfo.getName(), is("setFourDecimal"));
        assertThat(leafInfo.getDataType().getDataTypeName(), is("validDecimal"));
        YangType<YangDerivedInfo> derivedInfoType = (YangType<YangDerivedInfo>) leafInfo.getDataType();
        YangDerivedInfo derivedInfo = (YangDerivedInfo) derivedInfoType.getDataTypeExtendedInfo();
        YangTypeDef typedef = (YangTypeDef) derivedInfo.getReferredTypeDef();
        assertThat(typedef.getName(), is("validDecimal"));

        assertThat(leafInfo.getDataType().getDataType(), is(YangDataTypes.DERIVED));
        YangType type = typedef.getTypeList().iterator().next();
        assertThat(type.getDataType(), is(YangDataTypes.DECIMAL64));
        assertThat(type.getDataTypeName(), is("decimal64"));
        YangType<YangDecimal64> decimal64Type = (YangType<YangDecimal64>) typedef.getTypeList().iterator().next();
        YangDecimal64 decimal64 = decimal64Type.getDataTypeExtendedInfo();
        assertThat(decimal64.getFractionDigit(), is(4));
    }

    /**
     * Checks decimal64 with multiple typedef statement.
     */
    @Test
    public void processDecimal64MultiTypedefStatement() throws IOException, ParserException {

        YangNode node = manager.getDataModel("src/test/resources/decimal64/Decimal64MultiTypedefStatement.yang");

        // Check whether the data model tree returned is of type module.
        assertThat((node instanceof YangModule), is(true));

        // Check whether the node type is set properly to module.
        assertThat(node.getNodeType(), is(YangNodeType.MODULE_NODE));

        // Check whether the module name is set correctly.
        YangModule yangNode = (YangModule) node;
        assertThat(yangNode.getName(), is("Test"));

        ListIterator<YangLeaf> leafIterator = yangNode.getListOfLeaf().listIterator();
        YangLeaf leafInfo = leafIterator.next();

        // check leaf type
        assertThat(leafInfo.getName(), is("lowerDecimal"));
        assertThat(leafInfo.getDataType().getDataTypeName(), is("midDecimal"));
        YangType<YangDerivedInfo> derivedInfoType = (YangType<YangDerivedInfo>) leafInfo.getDataType();
        assertThat(derivedInfoType.getDataType(), is(YangDataTypes.DERIVED));
        YangDerivedInfo derivedInfo = (YangDerivedInfo) derivedInfoType.getDataTypeExtendedInfo();

        // check previous typedef
        YangTypeDef prevTypedef = (YangTypeDef) derivedInfo.getReferredTypeDef();
        assertThat(prevTypedef.getName(), is("midDecimal"));
        YangType type = prevTypedef.getTypeList().iterator().next();
        assertThat(type.getDataType(), is(YangDataTypes.DERIVED));
        derivedInfo = (YangDerivedInfo) type.getDataTypeExtendedInfo();

        // check top typedef
        YangTypeDef topTypedef = (YangTypeDef) derivedInfo.getReferredTypeDef();
        assertThat(topTypedef.getName(), is("topDecimal"));
        type = topTypedef.getTypeList().iterator().next();
        assertThat(type.getDataType(), is(YangDataTypes.DECIMAL64));
        assertThat(type.getDataTypeName(), is("decimal64"));
        YangType<YangDecimal64> decimal64Type = (YangType<YangDecimal64>) type;
        YangDecimal64 decimal64 = decimal64Type.getDataTypeExtendedInfo();
        assertThat(decimal64.getFractionDigit(), is(4));
    }

    /**
     * Checks decimal64 with multiple typedef with single range statement.
     * Range value in typedef statement.
     */
    @Test
    public void processDecimal64MultiTypedefRangeStatement() throws IOException, ParserException {

        YangNode node = manager.getDataModel("src/test/resources/decimal64/Decimal64MultiTypedefRangeStatement.yang");

        // Check whether the data model tree returned is of type module.
        assertThat((node instanceof YangModule), is(true));

        // Check whether the node type is set properly to module.
        assertThat(node.getNodeType(), is(YangNodeType.MODULE_NODE));

        // Check whether the module name is set correctly.
        YangModule yangNode = (YangModule) node;
        assertThat(yangNode.getName(), is("Test"));

        ListIterator<YangLeaf> leafIterator = yangNode.getListOfLeaf().listIterator();
        YangLeaf leafInfo = leafIterator.next();

        // check leaf type
        assertThat(leafInfo.getName(), is("lowerDecimal"));
        assertThat(leafInfo.getDataType().getDataTypeName(), is("midDecimal"));
        YangType<YangDerivedInfo> derivedInfoType = (YangType<YangDerivedInfo>) leafInfo.getDataType();
        assertThat(derivedInfoType.getDataType(), is(YangDataTypes.DERIVED));
        YangDerivedInfo derivedInfo = (YangDerivedInfo) derivedInfoType.getDataTypeExtendedInfo();

        // Check range restriction
        YangRangeRestriction rangeRestriction = (YangRangeRestriction) derivedInfo.getResolvedExtendedInfo();
        ListIterator<YangRangeInterval> rangeListIterator = rangeRestriction.getAscendingRangeIntervals()
                .listIterator();
        YangRangeInterval rangeInterval = rangeListIterator.next();
        assertThat(((YangDecimal64) rangeInterval.getStartValue()).getValue().doubleValue(), is(1.0));
        assertThat(((YangDecimal64) rangeInterval.getEndValue()).getValue().doubleValue(), is(12.0));

        // check previous typedef
        YangTypeDef prevTypedef = (YangTypeDef) derivedInfo.getReferredTypeDef();
        assertThat(prevTypedef.getName(), is("midDecimal"));
        YangType type = prevTypedef.getTypeList().iterator().next();
        assertThat(type.getDataType(), is(YangDataTypes.DERIVED));
        derivedInfo = (YangDerivedInfo) type.getDataTypeExtendedInfo();

        // check top typedef
        YangTypeDef topTypedef = (YangTypeDef) derivedInfo.getReferredTypeDef();
        assertThat(topTypedef.getName(), is("topDecimal"));
        type = topTypedef.getTypeList().iterator().next();
        assertThat(type.getDataType(), is(YangDataTypes.DECIMAL64));
        assertThat(type.getDataTypeName(), is("decimal64"));
        YangType<YangDecimal64> decimal64Type = (YangType<YangDecimal64>) type;
        YangDecimal64 decimal64 = decimal64Type.getDataTypeExtendedInfo();
        assertThat(decimal64.getFractionDigit(), is(4));

        // Check range restriction
        rangeRestriction = (YangRangeRestriction) decimal64.getRangeRestrictedExtendedInfo();
        rangeListIterator = rangeRestriction.getAscendingRangeIntervals().listIterator();
        rangeInterval = rangeListIterator.next();
        assertThat(((YangDecimal64) rangeInterval.getStartValue()).getValue().doubleValue(), is(1.0));
        assertThat(((YangDecimal64) rangeInterval.getEndValue()).getValue().doubleValue(), is(12.0));
    }

    /**
     * Checks decimal64 with multiple typedef with single range statement.
     * Range value in leaf statement.
     */
    @Test
    public void processDecimal64MultiTypedefRangeInLeafStatement() throws IOException, ParserException {

        YangNode node = manager
                .getDataModel("src/test/resources/decimal64/Decimal64MultiTypedefRangeInLeafStatement.yang");

        // Check whether the data model tree returned is of type module.
        assertThat((node instanceof YangModule), is(true));

        // Check whether the node type is set properly to module.
        assertThat(node.getNodeType(), is(YangNodeType.MODULE_NODE));

        // Check whether the module name is set correctly.
        YangModule yangNode = (YangModule) node;
        assertThat(yangNode.getName(), is("Test"));

        ListIterator<YangLeaf> leafIterator = yangNode.getListOfLeaf().listIterator();
        YangLeaf leafInfo = leafIterator.next();

        // check leaf type
        assertThat(leafInfo.getName(), is("lowerDecimal"));
        assertThat(leafInfo.getDataType().getDataTypeName(), is("midDecimal"));
        YangType<YangDerivedInfo> derivedInfoType = (YangType<YangDerivedInfo>) leafInfo.getDataType();
        assertThat(derivedInfoType.getDataType(), is(YangDataTypes.DERIVED));
        YangDerivedInfo derivedInfo = (YangDerivedInfo) derivedInfoType.getDataTypeExtendedInfo();

        // Check range restriction
        YangRangeRestriction rangeRestriction = (YangRangeRestriction) derivedInfo.getResolvedExtendedInfo();
        ListIterator<YangRangeInterval> rangeListIterator = rangeRestriction.getAscendingRangeIntervals()
                .listIterator();
        YangRangeInterval rangeInterval = rangeListIterator.next();
        assertThat(((YangDecimal64) rangeInterval.getStartValue()).getValue().doubleValue(), is(1.0));
        assertThat(((YangDecimal64) rangeInterval.getEndValue()).getValue().doubleValue(), is(12.0));

        // check previous typedef
        YangTypeDef prevTypedef = (YangTypeDef) derivedInfo.getReferredTypeDef();
        assertThat(prevTypedef.getName(), is("midDecimal"));
        YangType type = prevTypedef.getTypeList().iterator().next();
        assertThat(type.getDataType(), is(YangDataTypes.DERIVED));
        derivedInfo = (YangDerivedInfo) type.getDataTypeExtendedInfo();

        // check top typedef
        YangTypeDef topTypedef = (YangTypeDef) derivedInfo.getReferredTypeDef();
        assertThat(topTypedef.getName(), is("topDecimal"));
        type = topTypedef.getTypeList().iterator().next();
        assertThat(type.getDataType(), is(YangDataTypes.DECIMAL64));
        assertThat(type.getDataTypeName(), is("decimal64"));
        YangType<YangDecimal64> decimal64Type = (YangType<YangDecimal64>) type;
        YangDecimal64 decimal64 = decimal64Type.getDataTypeExtendedInfo();
        assertThat(decimal64.getFractionDigit(), is(4));
    }

    /**
     * Checks decimal64 with multiple typedef with multiple range statement.
     * Having more restricted range at leaf.
     */
    @Test
    public void processDecimal64MultiTypedefMultipleRangeStatement() throws IOException, ParserException {

        YangNode node = manager
                .getDataModel("src/test/resources/decimal64/Decimal64MultiTypedefMultiRangeStatement.yang");

        // Check whether the data model tree returned is of type module.
        assertThat((node instanceof YangModule), is(true));

        // Check whether the node type is set properly to module.
        assertThat(node.getNodeType(), is(YangNodeType.MODULE_NODE));

        // Check whether the module name is set correctly.
        YangModule yangNode = (YangModule) node;
        assertThat(yangNode.getName(), is("Test"));

        ListIterator<YangLeaf> leafIterator = yangNode.getListOfLeaf().listIterator();
        YangLeaf leafInfo = leafIterator.next();

        // check leaf type
        assertThat(leafInfo.getName(), is("lowerDecimal"));
        assertThat(leafInfo.getDataType().getDataTypeName(), is("midDecimal"));
        YangType<YangDerivedInfo> derivedInfoType = (YangType<YangDerivedInfo>) leafInfo.getDataType();
        assertThat(derivedInfoType.getDataType(), is(YangDataTypes.DERIVED));
        YangDerivedInfo derivedInfo = (YangDerivedInfo) derivedInfoType.getDataTypeExtendedInfo();

        // Check range restriction
        YangRangeRestriction rangeRestriction = (YangRangeRestriction) derivedInfo.getResolvedExtendedInfo();

        ListIterator<YangRangeInterval> rangeListIterator = rangeRestriction.getAscendingRangeIntervals()
                .listIterator();
        YangRangeInterval rangeInterval = rangeListIterator.next();
        assertThat(((YangDecimal64) rangeInterval.getStartValue()).getValue().doubleValue(), is(4.0));
        assertThat(((YangDecimal64) rangeInterval.getEndValue()).getValue().doubleValue(), is(11.0));

        // check previous typedef
        YangTypeDef prevTypedef = (YangTypeDef) derivedInfo.getReferredTypeDef();
        assertThat(prevTypedef.getName(), is("midDecimal"));
        YangType type = prevTypedef.getTypeList().iterator().next();
        assertThat(type.getDataType(), is(YangDataTypes.DERIVED));
        derivedInfo = (YangDerivedInfo) type.getDataTypeExtendedInfo();

        // check top typedef
        YangTypeDef topTypedef = (YangTypeDef) derivedInfo.getReferredTypeDef();
        assertThat(topTypedef.getName(), is("topDecimal"));
        type = topTypedef.getTypeList().iterator().next();
        assertThat(type.getDataType(), is(YangDataTypes.DECIMAL64));
        assertThat(type.getDataTypeName(), is("decimal64"));
        YangType<YangDecimal64> decimal64Type = (YangType<YangDecimal64>) type;
        YangDecimal64 decimal64 = decimal64Type.getDataTypeExtendedInfo();
        assertThat(decimal64.getFractionDigit(), is(4));

        // Check range restriction
        rangeRestriction = (YangRangeRestriction) decimal64.getRangeRestrictedExtendedInfo();
        rangeListIterator = rangeRestriction.getAscendingRangeIntervals().listIterator();
        rangeInterval = rangeListIterator.next();
        assertThat(((YangDecimal64) rangeInterval.getStartValue()).getValue().doubleValue(), is(1.0));
        assertThat(((YangDecimal64) rangeInterval.getEndValue()).getValue().doubleValue(), is(12.0));
    }

    /**
     * Checks decimal64 with multiple typedef with multiple range statement.
     * But having more restricted range at top of typedef.
     */
    @Test
    public void processDecimal64MultiTypedefMultiInvalidRangeStatement() throws IOException, LinkerException {
        thrown.expect(LinkerException.class);
        thrown.expectMessage(" Range interval doesn't fall within the referred restriction ranges");

        manager.getDataModel("src/test/resources/decimal64/Decimal64MultiTypedefMultiInvalidRangeStatement.yang");
    }

    /**
     * Checks decimal64 with multiple typedef with max range statement.
     */
    @Test
    public void processDecimal64MultiTypedefWithMaxRange() throws IOException, ParserException {

        YangNode node = manager.getDataModel("src/test/resources/decimal64/Decimal64MultiTypedefWithMaxRange.yang");

        // Check whether the data model tree returned is of type module.
        assertThat((node instanceof YangModule), is(true));

        // Check whether the node type is set properly to module.
        assertThat(node.getNodeType(), is(YangNodeType.MODULE_NODE));

        // Check whether the module name is set correctly.
        YangModule yangNode = (YangModule) node;
        assertThat(yangNode.getName(), is("Test"));

        ListIterator<YangLeaf> leafIterator = yangNode.getListOfLeaf().listIterator();
        YangLeaf leafInfo = leafIterator.next();

        // check leaf type
        assertThat(leafInfo.getName(), is("lowerDecimal"));
        assertThat(leafInfo.getDataType().getDataTypeName(), is("midDecimal"));
        YangType<YangDerivedInfo> derivedInfoType = (YangType<YangDerivedInfo>) leafInfo.getDataType();
        assertThat(derivedInfoType.getDataType(), is(YangDataTypes.DERIVED));
        YangDerivedInfo derivedInfo = (YangDerivedInfo) derivedInfoType.getDataTypeExtendedInfo();

        // Check range restriction
        YangRangeRestriction rangeRestriction = (YangRangeRestriction) derivedInfo.getResolvedExtendedInfo();

        ListIterator<YangRangeInterval> rangeListIterator = rangeRestriction.getAscendingRangeIntervals()
                .listIterator();
        YangRangeInterval rangeInterval = rangeListIterator.next();
        assertThat(((YangDecimal64) rangeInterval.getStartValue()).getValue().doubleValue(), is(4.0));
        assertThat(((YangDecimal64) rangeInterval.getEndValue()).getValue().doubleValue(), is(12.0));

        // check previous typedef
        YangTypeDef prevTypedef = (YangTypeDef) derivedInfo.getReferredTypeDef();
        assertThat(prevTypedef.getName(), is("midDecimal"));
        YangType type = prevTypedef.getTypeList().iterator().next();
        assertThat(type.getDataType(), is(YangDataTypes.DERIVED));
        derivedInfo = (YangDerivedInfo) type.getDataTypeExtendedInfo();

        // check top typedef
        YangTypeDef topTypedef = (YangTypeDef) derivedInfo.getReferredTypeDef();
        assertThat(topTypedef.getName(), is("topDecimal"));
        type = topTypedef.getTypeList().iterator().next();
        assertThat(type.getDataType(), is(YangDataTypes.DECIMAL64));
        assertThat(type.getDataTypeName(), is("decimal64"));
        YangType<YangDecimal64> decimal64Type = (YangType<YangDecimal64>) type;
        YangDecimal64 decimal64 = decimal64Type.getDataTypeExtendedInfo();
        assertThat(decimal64.getFractionDigit(), is(4));

        // Check range restriction
        rangeRestriction = (YangRangeRestriction) decimal64.getRangeRestrictedExtendedInfo();
        rangeListIterator = rangeRestriction.getAscendingRangeIntervals().listIterator();
        rangeInterval = rangeListIterator.next();
        assertThat(((YangDecimal64) rangeInterval.getStartValue()).getValue().doubleValue(), is(1.0));
        assertThat(((YangDecimal64) rangeInterval.getEndValue()).getValue().doubleValue(), is(12.0));
    }
}

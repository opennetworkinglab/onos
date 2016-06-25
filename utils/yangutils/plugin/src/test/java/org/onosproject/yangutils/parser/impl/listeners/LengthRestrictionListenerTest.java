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

package org.onosproject.yangutils.parser.impl.listeners;

import java.io.IOException;
import java.math.BigInteger;
import java.util.ListIterator;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.onosproject.yangutils.datamodel.YangDataTypes;
import org.onosproject.yangutils.datamodel.YangLeaf;
import org.onosproject.yangutils.datamodel.YangLeafList;
import org.onosproject.yangutils.datamodel.YangModule;
import org.onosproject.yangutils.datamodel.YangNode;
import org.onosproject.yangutils.datamodel.YangNodeType;
import org.onosproject.yangutils.datamodel.YangRangeInterval;
import org.onosproject.yangutils.datamodel.YangRangeRestriction;
import org.onosproject.yangutils.datamodel.YangStringRestriction;
import org.onosproject.yangutils.datamodel.YangTypeDef;
import org.onosproject.yangutils.parser.exceptions.ParserException;
import org.onosproject.yangutils.parser.impl.YangUtilsParserManager;
import org.onosproject.yangutils.datamodel.utils.builtindatatype.YangUint64;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

/**
 * Test cases for length restriction listener.
 */
public class LengthRestrictionListenerTest {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    private final YangUtilsParserManager manager = new YangUtilsParserManager();

    /**
     * Checks valid length statement as sub-statement of leaf statement.
     */
    @Test
    public void processValidLengthStatement() throws IOException, ParserException {

        YangNode node = manager.getDataModel("src/test/resources/ValidLengthStatement.yang");

        assertThat((node instanceof YangModule), is(true));
        assertThat(node.getNodeType(), is(YangNodeType.MODULE_NODE));
        YangModule yangNode = (YangModule) node;
        assertThat(yangNode.getName(), is("Test"));

        ListIterator<YangLeaf> leafIterator = yangNode.getListOfLeaf().listIterator();
        YangLeaf leafInfo = leafIterator.next();

        assertThat(leafInfo.getName(), is("invalid-interval"));
        assertThat(leafInfo.getDataType().getDataTypeName(), is("string"));
        assertThat(leafInfo.getDataType().getDataType(), is(YangDataTypes.STRING));
        YangStringRestriction stringRestriction = (YangStringRestriction) leafInfo
                .getDataType().getDataTypeExtendedInfo();
        YangRangeRestriction lengthRestriction = stringRestriction.getLengthRestriction();

        ListIterator<YangRangeInterval> lengthListIterator = lengthRestriction.getAscendingRangeIntervals()
                .listIterator();

        YangRangeInterval rangeInterval = lengthListIterator.next();

        assertThat(((YangUint64) rangeInterval.getStartValue()).getValue(), is(BigInteger.valueOf(0)));
        assertThat(((YangUint64) rangeInterval.getEndValue()).getValue(), is(BigInteger.valueOf(100)));
    }

    /**
     * Checks valid length statement as sub-statement of leaf-list.
     */
    @Test
    public void processLengthStatementInsideLeafList() throws IOException, ParserException {

        YangNode node = manager.getDataModel("src/test/resources/LengthStatementInsideLeafList.yang");

        assertThat((node instanceof YangModule), is(true));
        assertThat(node.getNodeType(), is(YangNodeType.MODULE_NODE));
        YangModule yangNode = (YangModule) node;
        assertThat(yangNode.getName(), is("Test"));

        ListIterator<YangLeafList> leafListIterator = yangNode.getListOfLeafList().listIterator();
        YangLeafList leafListInfo = leafListIterator.next();

        assertThat(leafListInfo.getName(), is("invalid-interval"));
        assertThat(leafListInfo.getDataType().getDataTypeName(), is("string"));
        assertThat(leafListInfo.getDataType().getDataType(), is(YangDataTypes.STRING));
        YangStringRestriction stringRestriction = (YangStringRestriction) leafListInfo
                .getDataType().getDataTypeExtendedInfo();
        YangRangeRestriction lengthRestriction = stringRestriction.getLengthRestriction();

        ListIterator<YangRangeInterval> lengthListIterator = lengthRestriction.getAscendingRangeIntervals()
                .listIterator();

        YangRangeInterval rangeInterval = lengthListIterator.next();
        assertThat(((YangUint64) rangeInterval.getStartValue()).getValue(), is(BigInteger.valueOf(1)));
        assertThat(((YangUint64) rangeInterval.getEndValue()).getValue(), is(BigInteger.valueOf(100)));
    }

    /**
     * Checks valid length statement as sub-statement of typedef.
     */
    @Test
    public void processLengthStatementInsideTypeDef() throws IOException, ParserException {

        YangNode node = manager.getDataModel("src/test/resources/LengthStatementInsideTypeDef.yang");

        assertThat((node instanceof YangModule), is(true));
        assertThat(node.getNodeType(), is(YangNodeType.MODULE_NODE));
        YangModule yangNode = (YangModule) node;
        assertThat(yangNode.getName(), is("Test"));

        YangTypeDef typedef = (YangTypeDef) yangNode.getChild();
        YangStringRestriction stringRestriction = (YangStringRestriction) typedef.getTypeDefBaseType()
                .getDataTypeExtendedInfo();

        YangRangeRestriction lengthRestriction = stringRestriction.getLengthRestriction();
        ListIterator<YangRangeInterval> lengthListIterator = lengthRestriction.getAscendingRangeIntervals()
                .listIterator();
        YangRangeInterval rangeInterval = lengthListIterator.next();
        assertThat(((YangUint64) rangeInterval.getStartValue()).getValue(), is(BigInteger.valueOf(1)));
        assertThat(((YangUint64) rangeInterval.getEndValue()).getValue(), is(BigInteger.valueOf(100)));
    }

    /**
     * Checks length statement with invalid type.
     */
    @Test
    public void processLengthWithInvalidType() throws IOException, ParserException {
        thrown.expect(ParserException.class);
        thrown.expectMessage("YANG file error : length name \"1..100\" can be used to restrict the built-in type" +
                " string/binary or types derived from string/binary.");
        YangNode node = manager.getDataModel("src/test/resources/LengthWithInvalidType.yang");
    }

    /**
     * Checks length statement with only start interval.
     */
    @Test
    public void processLengthWithOneInterval() throws IOException, ParserException {


        YangNode node = manager.getDataModel("src/test/resources/LengthWithOneInterval.yang");

        assertThat((node instanceof YangModule), is(true));
        assertThat(node.getNodeType(), is(YangNodeType.MODULE_NODE));
        YangModule yangNode = (YangModule) node;
        assertThat(yangNode.getName(), is("Test"));

        ListIterator<YangLeafList> leafListIterator = yangNode.getListOfLeafList().listIterator();
        YangLeafList leafListInfo = leafListIterator.next();

        assertThat(leafListInfo.getName(), is("invalid-interval"));
        assertThat(leafListInfo.getDataType().getDataTypeName(), is("string"));
        assertThat(leafListInfo.getDataType().getDataType(), is(YangDataTypes.STRING));
        YangStringRestriction stringRestriction = (YangStringRestriction) leafListInfo
                .getDataType().getDataTypeExtendedInfo();
        YangRangeRestriction lengthRestriction = stringRestriction.getLengthRestriction();

        ListIterator<YangRangeInterval> lengthListIterator = lengthRestriction.getAscendingRangeIntervals()
                .listIterator();

        YangRangeInterval rangeInterval = lengthListIterator.next();
        assertThat(((YangUint64) rangeInterval.getStartValue()).getValue(), is(BigInteger.valueOf(1)));
        assertThat(((YangUint64) rangeInterval.getEndValue()).getValue(), is(BigInteger.valueOf(1)));
    }

    /**
     * Checks length statement with min and max.
     */
    @Test
    public void processLengthWithMinMax() throws IOException, ParserException {


        YangNode node = manager.getDataModel("src/test/resources/LengthWithMinMax.yang");

        assertThat((node instanceof YangModule), is(true));
        assertThat(node.getNodeType(), is(YangNodeType.MODULE_NODE));
        YangModule yangNode = (YangModule) node;
        assertThat(yangNode.getName(), is("Test"));

        ListIterator<YangLeafList> leafListIterator = yangNode.getListOfLeafList().listIterator();
        YangLeafList leafListInfo = leafListIterator.next();

        assertThat(leafListInfo.getName(), is("invalid-interval"));
        assertThat(leafListInfo.getDataType().getDataTypeName(), is("string"));
        assertThat(leafListInfo.getDataType().getDataType(), is(YangDataTypes.STRING));
        YangStringRestriction stringRestriction = (YangStringRestriction) leafListInfo
                .getDataType().getDataTypeExtendedInfo();
        YangRangeRestriction lengthRestriction = stringRestriction.getLengthRestriction();

        ListIterator<YangRangeInterval> lengthListIterator = lengthRestriction.getAscendingRangeIntervals()
                .listIterator();

        YangRangeInterval rangeInterval = lengthListIterator.next();
        assertThat(((YangUint64) rangeInterval.getStartValue()).getValue(), is(BigInteger.valueOf(0)));
        assertThat(((YangUint64) rangeInterval.getEndValue()).getValue(), is(new BigInteger("18446744073709551615")));
    }

    /**
     * Checks length statement with invalid integer pattern.
     */
    @Test
    public void processLengthWithInvalidIntegerPattern() throws IOException, ParserException {
        thrown.expect(ParserException.class);
        thrown.expectMessage("YANG file error : Input value \"a\" is not a valid uint64.");
        YangNode node = manager.getDataModel("src/test/resources/LengthWithInvalidIntegerPattern.yang");
    }

    /**
     * Checks length statement with invalid interval.
     */
    @Test
    public void processLengthWithInvalidInterval() throws IOException, ParserException {
        thrown.expect(ParserException.class);
        thrown.expectMessage("YANG file error : 18446744073709551617 is greater than maximum value" +
                " 18446744073709551615.");
        YangNode node = manager.getDataModel("src/test/resources/LengthWithInvalidInterval.yang");
    }

    /**
     * Checks valid length substatements.
     */
    @Test
    public void processLengthSubStatements() throws IOException, ParserException {

        YangNode node = manager.getDataModel("src/test/resources/LengthSubStatements.yang");

        assertThat((node instanceof YangModule), is(true));
        assertThat(node.getNodeType(), is(YangNodeType.MODULE_NODE));
        YangModule yangNode = (YangModule) node;
        assertThat(yangNode.getName(), is("Test"));

        ListIterator<YangLeaf> leafIterator = yangNode.getListOfLeaf().listIterator();
        YangLeaf leafInfo = leafIterator.next();

        assertThat(leafInfo.getName(), is("invalid-interval"));
        assertThat(leafInfo.getDataType().getDataTypeName(), is("string"));
        assertThat(leafInfo.getDataType().getDataType(), is(YangDataTypes.STRING));
        YangStringRestriction stringRestriction = (YangStringRestriction) leafInfo
                .getDataType().getDataTypeExtendedInfo();
        YangRangeRestriction lengthRestriction = stringRestriction.getLengthRestriction();

        assertThat(lengthRestriction.getDescription(), is("\"length description\""));
        assertThat(lengthRestriction.getReference(), is("\"length reference\""));

        ListIterator<YangRangeInterval> lengthListIterator = lengthRestriction.getAscendingRangeIntervals()
                .listIterator();

        YangRangeInterval rangeInterval = lengthListIterator.next();

        assertThat(((YangUint64) rangeInterval.getStartValue()).getValue(), is(BigInteger.valueOf(0)));
        assertThat(((YangUint64) rangeInterval.getEndValue()).getValue(), is(BigInteger.valueOf(100)));
    }

    /**
     * Checks whether space can be allowed when length statement is present.
     */
    @Test
    public void processLengthStatementWithSpace() throws IOException, ParserException {

        YangNode node = manager.getDataModel("src/test/resources/LengthStatementWithSpace.yang");

        assertThat((node instanceof YangModule), is(true));
        assertThat(node.getNodeType(), is(YangNodeType.MODULE_NODE));
        YangModule yangNode = (YangModule) node;
        assertThat(yangNode.getName(), is("Test"));

        ListIterator<YangLeaf> leafIterator = yangNode.getListOfLeaf().listIterator();
        YangLeaf leafInfo = leafIterator.next();

        assertThat(leafInfo.getName(), is("invalid-interval"));
        assertThat(leafInfo.getDataType().getDataTypeName(), is("string"));
        assertThat(leafInfo.getDataType().getDataType(), is(YangDataTypes.STRING));
        YangStringRestriction stringRestriction = (YangStringRestriction) leafInfo
                .getDataType().getDataTypeExtendedInfo();
        YangRangeRestriction lengthRestriction = stringRestriction.getLengthRestriction();

        ListIterator<YangRangeInterval> lengthListIterator = lengthRestriction.getAscendingRangeIntervals()
                .listIterator();

        YangRangeInterval rangeInterval = lengthListIterator.next();

        assertThat(((YangUint64) rangeInterval.getStartValue()).getValue(), is(BigInteger.valueOf(0)));
        assertThat(((YangUint64) rangeInterval.getEndValue()).getValue(), is(BigInteger.valueOf(100)));
    }
}

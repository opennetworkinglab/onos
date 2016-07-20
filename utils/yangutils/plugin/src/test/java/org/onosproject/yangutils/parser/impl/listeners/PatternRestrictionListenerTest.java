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
import java.util.ListIterator;

import org.junit.Test;
import org.onosproject.yangutils.datamodel.YangDataTypes;
import org.onosproject.yangutils.datamodel.YangLeaf;
import org.onosproject.yangutils.datamodel.YangLeafList;
import org.onosproject.yangutils.datamodel.YangModule;
import org.onosproject.yangutils.datamodel.YangNode;
import org.onosproject.yangutils.datamodel.YangNodeType;
import org.onosproject.yangutils.datamodel.YangPatternRestriction;
import org.onosproject.yangutils.datamodel.YangStringRestriction;
import org.onosproject.yangutils.datamodel.YangTypeDef;
import org.onosproject.yangutils.parser.exceptions.ParserException;
import org.onosproject.yangutils.parser.impl.YangUtilsParserManager;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

/**
 * Test cases for pattern restriction listener.
 */
public class PatternRestrictionListenerTest {

    private final YangUtilsParserManager manager = new YangUtilsParserManager();

    /**
     * Checks valid pattern statement as sub-statement of leaf statement.
     */
    @Test
    public void processValidPatternStatement() throws IOException, ParserException {

        YangNode node = manager.getDataModel("src/test/resources/ValidPatternStatement.yang");

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
        ListIterator<String> patternListIterator = stringRestriction.getPatternRestriction()
                .getPatternList().listIterator();
        assertThat(patternListIterator.next(), is("[a-zA-Z]"));
    }

    /**
     * Checks valid pattern statement as sub-statement of leaf-list.
     */
    @Test
    public void processPatternStatementInsideLeafList() throws IOException, ParserException {

        YangNode node = manager.getDataModel("src/test/resources/PatternStatementInsideLeafList.yang");

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
        ListIterator<String> patternListIterator = stringRestriction.getPatternRestriction()
                .getPatternList().listIterator();
        assertThat(patternListIterator.next(), is("[a-zA-Z]"));
    }

    /**
     * Checks valid pattern statement as sub-statement of typedef.
     */
    @Test
    public void processPatternStatementInsideTypeDef() throws IOException, ParserException {

        YangNode node = manager.getDataModel("src/test/resources/PatternStatementInsideTypeDef.yang");

        assertThat((node instanceof YangModule), is(true));
        assertThat(node.getNodeType(), is(YangNodeType.MODULE_NODE));
        YangModule yangNode = (YangModule) node;
        assertThat(yangNode.getName(), is("Test"));

        YangTypeDef typedef = (YangTypeDef) yangNode.getChild();
        YangStringRestriction stringRestriction = (YangStringRestriction) typedef.getTypeDefBaseType()
                .getDataTypeExtendedInfo();

        YangPatternRestriction yangPatternRestriction = stringRestriction.getPatternRestriction();
        assertThat(yangPatternRestriction.getPatternList().listIterator().next(), is("[a-zA-Z]"));
    }

    /**
     * Checks valid multiple pattern statements.
     */
    @Test
    public void processMultiplePatternStatement() throws IOException, ParserException {

        YangNode node = manager.getDataModel("src/test/resources/MultiplePatternStatement.yang");

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
        ListIterator<String> patternListIterator = stringRestriction.getPatternRestriction()
                .getPatternList().listIterator();
        assertThat(patternListIterator.next(), is("[a-zA-Z]"));
    }

    /**
     * Checks valid pattern statement with plus symbol in pattern.
     */
    @Test
    public void processPatternStatementWithPlus() throws IOException, ParserException {

        YangNode node = manager.getDataModel("src/test/resources/PatternStatementWithPlus.yang");

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
        ListIterator<String> patternListIterator = stringRestriction.getPatternRestriction()
                .getPatternList().listIterator();
        assertThat(patternListIterator.next(), is("-[0-9]+|[0-9]+"));
    }

    /**
     * Checks valid pattern substatement.
     */
    @Test
    public void processPatternSubStatements() throws IOException, ParserException {

        YangNode node = manager.getDataModel("src/test/resources/PatternSubStatements.yang");

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
        assertThat(stringRestriction.getDescription(), is("\"pattern description\""));
        assertThat(stringRestriction.getReference(), is("\"pattern reference\""));
        ListIterator<String> patternListIterator = stringRestriction.getPatternRestriction()
                .getPatternList().listIterator();
        assertThat(patternListIterator.next(), is("[a-zA-Z]"));
    }

    /**
     * Checks invalid pattern sub-statement.
     */
    @Test(expected = ParserException.class)
    public void processInvalidPatternSubStatements() throws IOException, ParserException {
        YangNode node = manager.getDataModel("src/test/resources/InvalidPatternSubStatements.yang");
    }
}

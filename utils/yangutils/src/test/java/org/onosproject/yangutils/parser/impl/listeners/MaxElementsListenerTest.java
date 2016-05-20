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

import java.io.IOException;
import java.util.ListIterator;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.onosproject.yangutils.datamodel.YangLeafList;
import org.onosproject.yangutils.datamodel.YangList;
import org.onosproject.yangutils.datamodel.YangModule;
import org.onosproject.yangutils.datamodel.YangNode;
import org.onosproject.yangutils.datamodel.YangNodeType;
import org.onosproject.yangutils.parser.exceptions.ParserException;
import org.onosproject.yangutils.parser.impl.YangUtilsParserManager;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

/**
 * Test cases for testing max-elements listener.
 */
public class MaxElementsListenerTest {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    private final YangUtilsParserManager manager = new YangUtilsParserManager();

    /**
     * Checks max-elements as sub-statements of leaf-list.
     */
    @Test
    public void processLeafListSubStatementMaxElements() throws IOException, ParserException {

        YangNode node = manager.getDataModel("src/test/resources/LeafListSubStatementMaxElements.yang");

        // Check whether the data model tree returned is of type module.
        assertThat((node instanceof YangModule), is(true));

        // Check whether the node type is set properly to module.
        assertThat(node.getNodeType(), is(YangNodeType.MODULE_NODE));

        // Check whether the module name is set correctly.
        YangModule yangNode = (YangModule) node;
        assertThat(yangNode.getName(), is("Test"));

        ListIterator<YangLeafList> leafListIterator = yangNode.getListOfLeafList().listIterator();
        YangLeafList leafListInfo = leafListIterator.next();

        assertThat(leafListInfo.getName(), is("invalid-interval"));
        assertThat(leafListInfo.getMaxElelements(), is(3));
    }

    /**
     * Checks max-elements as sub-statements of list.
     */
    @Test
    public void processListSubStatementMaxElements() throws IOException, ParserException {

        YangNode node = manager.getDataModel("src/test/resources/ListSubStatementMaxElements.yang");

        assertThat((node instanceof YangModule), is(true));

        // Check whether the node type is set properly to module.
        assertThat(node.getNodeType(), is(YangNodeType.MODULE_NODE));

        // Check whether the module name is set correctly.
        YangModule yangNode = (YangModule) node;
        assertThat(yangNode.getName(), is("Test"));

        // Check whether the list is child of module
        YangList yangList = (YangList) yangNode.getChild();
        assertThat(yangList.getName(), is("valid"));
        assertThat(yangList.getMaxElements(), is(3));
    }

    /**
     * Checks whether exception is thrown when invalid max-elements keyword is
     * given as input.
     */
    @Test
    public void processMaxElementsInvalidStatement() throws IOException, ParserException {
        thrown.expect(ParserException.class);
        thrown.expectMessage("extraneous input 'max-element' expecting {'config', 'description', 'if-feature',"
                + " 'max-elements', 'min-elements', 'must', 'ordered-by', 'reference', 'status', 'type', 'units', "
                + "'when', '}'}");
        YangNode node = manager.getDataModel("src/test/resources/MaxElementsInvalidStatement.yang");
    }

    /**
     * Checks whether exception is thrown when max-elements statement without
     * statement end is given as input.
     */
    @Test
    public void processMaxElementsWithoutStatementEnd() throws IOException, ParserException {
        thrown.expect(ParserException.class);
        thrown.expectMessage("missing ';' at 'description'");
        YangNode node = manager.getDataModel("src/test/resources/MaxElementsWithoutStatementEnd.yang");
    }

    /**
     * Checks whether exception is thrown when max-elements cardinality is not
     * as per the grammar.
     */
    @Test
    public void processMaxElementsCardinality() throws IOException, ParserException {
        thrown.expect(ParserException.class);
        thrown.expectMessage("YANG file error: \"max-elements\" is defined more than once in \"leaf-list " +
                "invalid-interval\".");
        YangNode node = manager.getDataModel("src/test/resources/MaxElementsCardinality.yang");
    }

    /**
     * Checks unbounded value of max-elements statement.
     */
    @Test
    public void processMaxElementsUnbounded() throws IOException, ParserException {

        YangNode node = manager.getDataModel("src/test/resources/MaxElementsUnbounded.yang");

        // Check whether the data model tree returned is of type module.
        assertThat((node instanceof YangModule), is(true));

        // Check whether the node type is set properly to module.
        assertThat(node.getNodeType(), is(YangNodeType.MODULE_NODE));

        // Check whether the module name is set correctly.
        YangModule yangNode = (YangModule) node;
        assertThat(yangNode.getName(), is("Test"));

        ListIterator<YangLeafList> leafListIterator = yangNode.getListOfLeafList().listIterator();
        YangLeafList leafListInfo = leafListIterator.next();

        assertThat(leafListInfo.getName(), is("invalid-interval"));
        assertThat(leafListInfo.getMaxElelements(), is(2147483647));
    }

    /**
     * Checks default value of max-elements statement.
     */
    @Test
    public void processMaxElementsDefaultValue() throws IOException, ParserException {

        YangNode node = manager.getDataModel("src/test/resources/MaxElementsDefaultValue.yang");

        // Check whether the data model tree returned is of type module.
        assertThat((node instanceof YangModule), is(true));

        // Check whether the node type is set properly to module.
        assertThat(node.getNodeType(), is(YangNodeType.MODULE_NODE));

        // Check whether the module name is set correctly.
        YangModule yangNode = (YangModule) node;
        assertThat(yangNode.getName(), is("Test"));

        ListIterator<YangLeafList> leafListIterator = yangNode.getListOfLeafList().listIterator();
        YangLeafList leafListInfo = leafListIterator.next();

        assertThat(leafListInfo.getName(), is("invalid-interval"));
        assertThat(leafListInfo.getMaxElelements(), is(2147483647));
    }

    /**
     * Checks whether exception is thrown when invalid min-elements value is
     * given as input.
     */
    @Test
    public void processMaxElementsMaxValue() throws IOException, ParserException {
        thrown.expect(ParserException.class);
        thrown.expectMessage("YANG file error : max-elements value 77777777777777777777777 is not valid.");
        YangNode node = manager.getDataModel("src/test/resources/MaxElementsMaxValue.yang");
    }
}

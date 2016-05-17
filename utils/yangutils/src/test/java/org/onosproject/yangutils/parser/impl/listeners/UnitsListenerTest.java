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
import org.onosproject.yangutils.datamodel.YangLeaf;
import org.onosproject.yangutils.datamodel.YangLeafList;
import org.onosproject.yangutils.datamodel.YangModule;
import org.onosproject.yangutils.datamodel.YangNode;
import org.onosproject.yangutils.datamodel.YangNodeType;
import org.onosproject.yangutils.datamodel.YangStatusType;
import org.onosproject.yangutils.parser.exceptions.ParserException;
import org.onosproject.yangutils.parser.impl.YangUtilsParserManager;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.Is.is;

/**
 * Test cases for units listener.
 */
public class UnitsListenerTest {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    private final YangUtilsParserManager manager = new YangUtilsParserManager();

    /**
     * Checks valid units statement.
     */
    @Test
    public void processUnitsStatement() throws IOException, ParserException {

        YangNode node = manager.getDataModel("src/test/resources/UnitsStatement.yang");

        // Check whether the data model tree returned is of type module.
        assertThat(node instanceof YangModule, is(true));

        // Check whether the node type is set properly to module.
        assertThat(node.getNodeType(), is(YangNodeType.MODULE_NODE));

        // Check whether the module name is set correctly.
        YangModule yangNode = (YangModule) node;
        assertThat(yangNode.getName(), is("Test"));

        ListIterator<YangLeaf> leafIterator = yangNode.getListOfLeaf().listIterator();
        YangLeaf leafInfo = leafIterator.next();

        // Check whether units value is set correctly.
        assertThat(leafInfo.getName(), is("invalid-interval"));
        assertThat(leafInfo.getUnits(), is("\"seconds\""));
    }

    /**
     * Checks invalid units statement as sub-statement of module.
     */
    @Test
    public void processModuleSubStatementUnits() throws IOException, ParserException {
        thrown.expect(ParserException.class);
        thrown.expectMessage("mismatched input 'type' expecting {'anyxml', 'augment', 'choice', 'contact', "
                + "'container', 'description', 'extension', 'deviation', 'feature', 'grouping', 'identity',"
                + " 'import', 'include', 'leaf', 'leaf-list', 'list', 'notification', 'organization', "
                + "'reference', 'revision', 'rpc', 'typedef', 'uses', '}'}");
        YangNode node = manager.getDataModel("src/test/resources/ModuleSubStatementUnits.yang");
    }

    /**
     * Checks invalid units statement(without statement end).
     */
    @Test
    public void processUnitsWithoutStatementEnd() throws IOException, ParserException {
        thrown.expect(ParserException.class);
        thrown.expectMessage("mismatched input '}' expecting {';', '+'}");
        YangNode node = manager.getDataModel("src/test/resources/UnitsWithoutStatementEnd.yang");
    }

    /**
     * Checks order of units statement in leaf.
     */
    @Test
    public void processUnitsStatementOrder() throws IOException, ParserException {

        YangNode node = manager.getDataModel("src/test/resources/UnitsStatementOrder.yang");

        // Check whether the data model tree returned is of type module.
        assertThat(node instanceof YangModule, is(true));

        // Check whether the node type is set properly to module.
        assertThat(node.getNodeType(), is(YangNodeType.MODULE_NODE));

        // Check whether the module name is set correctly.
        YangModule yangNode = (YangModule) node;
        assertThat(yangNode.getName(), is("Test"));

        ListIterator<YangLeaf> leafIterator = yangNode.getListOfLeaf().listIterator();
        YangLeaf leafInfo = leafIterator.next();

        // Check whether leaf properties is set correctly.
        assertThat(leafInfo.getName(), is("invalid-interval"));
        assertThat(leafInfo.getDataType().getDataTypeName(), is("uint16"));
        assertThat(leafInfo.getUnits(), is("\"seconds\""));
        assertThat(leafInfo.getDescription(), is("\"Interval before a route is declared invalid\""));
        assertThat(leafInfo.isConfig(), is(true));
        assertThat(leafInfo.isMandatory(), is(true));
        assertThat(leafInfo.getStatus(), is(YangStatusType.CURRENT));
        assertThat(leafInfo.getReference(), is("\"RFC 6020\""));
    }

    /**
     * Checks the default value of unit statement.
     */
    @Test
    public void processUnitsDefaultValue() throws IOException, ParserException {

        YangNode node = manager.getDataModel("src/test/resources/UnitsDefaultValue.yang");

        // Check whether the data model tree returned is of type module.
        assertThat(node instanceof YangModule, is(true));

        // Check whether the node type is set properly to module.
        assertThat(node.getNodeType(), is(YangNodeType.MODULE_NODE));

        // Check whether the module name is set correctly.
        YangModule yangNode = (YangModule) node;
        assertThat(yangNode.getName(), is("Test"));

        ListIterator<YangLeaf> leafIterator = yangNode.getListOfLeaf().listIterator();
        YangLeaf leafInfo = leafIterator.next();

        assertThat(leafInfo.getName(), is("invalid-interval"));
        assertThat(leafInfo.getUnits(), is(nullValue()));
    }

    /**
     * Checks invalid occurance of units statement as sub-statement of leaf.
     */
    @Test
    public void processUnitsStatementCardinality() throws IOException, ParserException {
        thrown.expect(ParserException.class);
        thrown.expectMessage("YANG file error: \"units\" is defined more than once in \"leaf invalid-interval\".");
        YangNode node = manager.getDataModel("src/test/resources/UnitsStatementCardinality.yang");
    }

    /**
     * Checks valid units statement as sub-statement of leaf-list.
     */
    @Test
    public void processLeafListSubStatementUnits() throws IOException, ParserException {

        YangNode node = manager.getDataModel("src/test/resources/LeafListSubStatementUnits.yang");

        // Check whether the data model tree returned is of type module.
        assertThat(node instanceof YangModule, is(true));

        // Check whether the node type is set properly to module.
        assertThat(node.getNodeType(), is(YangNodeType.MODULE_NODE));

        // Check whether the module name is set correctly.
        YangModule yangNode = (YangModule) node;
        assertThat(yangNode.getName(), is("Test"));

        ListIterator<YangLeafList> leafListIterator = yangNode.getListOfLeafList().listIterator();
        YangLeafList leafListInfo = leafListIterator.next();

        // Check whether units value is set correctly.
        assertThat(leafListInfo.getName(), is("invalid-interval"));
        assertThat(leafListInfo.getUnits(), is("\"seconds\""));
    }
}

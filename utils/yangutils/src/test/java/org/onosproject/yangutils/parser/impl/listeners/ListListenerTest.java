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
import org.onosproject.yangutils.datamodel.YangContainer;
import org.onosproject.yangutils.datamodel.YangDataTypes;
import org.onosproject.yangutils.datamodel.YangLeaf;
import org.onosproject.yangutils.datamodel.YangList;
import org.onosproject.yangutils.datamodel.YangModule;
import org.onosproject.yangutils.datamodel.YangNode;
import org.onosproject.yangutils.datamodel.YangNodeType;
import org.onosproject.yangutils.datamodel.YangStatusType;
import org.onosproject.yangutils.parser.exceptions.ParserException;
import org.onosproject.yangutils.parser.impl.YangUtilsParserManager;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

/**
 * Test cases for testing list listener.
 */
public class ListListenerTest {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    private final YangUtilsParserManager manager = new YangUtilsParserManager();

    /**
     * Checks list statement as sub-statement of module.
     */
    @Test
    public void processModuleSubStatementList() throws IOException, ParserException {

        YangNode node = manager.getDataModel("src/test/resources/ModuleSubStatementList.yang");

        assertThat((node instanceof YangModule), is(true));

        // Check whether the node type is set properly to module.
        assertThat(node.getNodeType(), is(YangNodeType.MODULE_NODE));

        // Check whether the module name is set correctly.
        YangModule yangNode = (YangModule) node;
        assertThat(yangNode.getName(), is("Test"));

        // Check whether the list is child of module
        YangList yangList = (YangList) yangNode.getChild();
        assertThat(yangList.getName(), is("valid"));

        ListIterator<String> keyList = yangList.getKeyList().listIterator();
        assertThat(keyList.next(), is("invalid-interval"));
    }

    /**
     * Checks list statement as sub-statement of container.
     */
    @Test
    public void processContainerSubStatementList() throws IOException, ParserException {

        YangNode node = manager.getDataModel("src/test/resources/ContainerSubStatementList.yang");

        assertThat((node instanceof YangModule), is(true));

        // Check whether the node type is set properly to module.
        assertThat(node.getNodeType(), is(YangNodeType.MODULE_NODE));

        // Check whether the module name is set correctly.
        YangModule yangNode = (YangModule) node;
        assertThat(yangNode.getName(), is("Test"));

        // Check whether the container is child of module
        YangContainer yangContainer = (YangContainer) yangNode.getChild();
        assertThat(yangContainer.getName(), is("ospf"));

        // Check whether the list is child of container
        YangList yangList = (YangList) yangContainer.getChild();
        assertThat(yangList.getName(), is("valid"));
        assertThat(yangList.getKeyList().contains("invalid-interval"), is(true));
    }

    /**
     * Checks list statement as sub-statement of list.
     */
    @Test
    public void processListSubStatementList() throws IOException, ParserException {

        YangNode node = manager.getDataModel("src/test/resources/ListSubStatementList.yang");

        assertThat((node instanceof YangModule), is(true));

        // Check whether the node type is set properly to module.
        assertThat(node.getNodeType(), is(YangNodeType.MODULE_NODE));

        // Check whether the module name is set correctly.
        YangModule yangNode = (YangModule) node;
        assertThat(yangNode.getName(), is("Test"));

        // Check whether the list is child of module
        YangList yangList1 = (YangList) yangNode.getChild();
        assertThat(yangList1.getName(), is("ospf"));
        assertThat(yangList1.getKeyList().contains("process-id"), is(true));

        // Check whether the list is child of list
        YangList yangList = (YangList) yangList1.getChild();
        assertThat(yangList.getName(), is("valid"));
        assertThat(yangList.getKeyList().contains("invalid-interval"), is(true));
    }

    /**
     * Checks list with all its sub-statements.
     */
    @Test
    public void processListSubStatements() throws IOException, ParserException {

        YangNode node = manager.getDataModel("src/test/resources/ListSubStatements.yang");

        assertThat((node instanceof YangModule), is(true));

        // Check whether the node type is set properly to module.
        assertThat(node.getNodeType(), is(YangNodeType.MODULE_NODE));

        // Check whether the module name is set correctly.
        YangModule yangNode = (YangModule) node;
        assertThat(yangNode.getName(), is("Test"));

        // Check whether the list is child of module
        YangList yangList = (YangList) yangNode.getChild();

        // Check whether list properties as set correctly.
        assertThat(yangList.getName(), is("ospf"));
        assertThat(yangList.getKeyList().contains("invalid-interval"), is(true));

        assertThat(yangList.isConfig(), is(true));
        assertThat(yangList.getMaxElements(), is(10));
        assertThat(yangList.getMinElements(), is(3));
        assertThat(yangList.getDescription(), is("\"list description\""));
        assertThat(yangList.getStatus(), is(YangStatusType.CURRENT));
        assertThat(yangList.getReference(), is("\"list reference\""));

        // Check whether leaf properties as set correctly.
        ListIterator<YangLeaf> leafIterator = yangList.getListOfLeaf().listIterator();
        YangLeaf leafInfo = leafIterator.next();

        assertThat(leafInfo.getName(), is("invalid-interval"));
        assertThat(leafInfo.getDataType().getDataTypeName(), is("uint16"));
        assertThat(leafInfo.getDataType().getDataType(), is(YangDataTypes.UINT16));
        assertThat(leafInfo.getUnits(), is("\"seconds\""));
        assertThat(leafInfo.getStatus(), is(YangStatusType.CURRENT));
        assertThat(leafInfo.getReference(), is("\"RFC 6020\""));
    }

    /**
     * Checks cardinality of sub-statements of list.
     */
    @Test
    public void processListSubStatementsCardinality() throws IOException, ParserException {
        thrown.expect(ParserException.class);
        thrown.expectMessage("YANG file error: \"reference\" is defined more than once in \"list valid\".");
        YangNode node = manager.getDataModel("src/test/resources/ListSubStatementsCardinality.yang");
    }

    /**
     * Checks list statement without child.
     */
    @Test
    public void processListStatementWithoutChild() throws IOException, ParserException {
        thrown.expect(ParserException.class);
        thrown.expectMessage("YANG file error: Missing \"data-def-substatements\" in \"list valid\".");
        YangNode node = manager.getDataModel("src/test/resources/ListStatementWithoutChild.yang");
    }

    /**
     * Checks list as root node.
     */
    @Test
    public void processListAsRootNode() throws IOException, ParserException {
        thrown.expect(ParserException.class);
        thrown.expectMessage("no viable alternative at input 'list'");
        YangNode node = manager.getDataModel("src/test/resources/ListAsRootNode.yang");
    }

    /**
     * Checks invalid identifier for list statement.
     */
    @Test
    public void processListInvalidIdentifier() throws IOException, ParserException {
        thrown.expect(ParserException.class);
        thrown.expectMessage("YANG file error : list name 1valid is not valid.");
        YangNode node = manager.getDataModel("src/test/resources/ListInvalidIdentifier.yang");
    }
}

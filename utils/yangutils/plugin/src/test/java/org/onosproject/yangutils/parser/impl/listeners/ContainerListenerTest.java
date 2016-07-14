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
 * Test cases for testing container listener.
 */
public class ContainerListenerTest {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    private final YangUtilsParserManager manager = new YangUtilsParserManager();

    /**
     * Checks container statement as sub-statement of module.
     */
    @Test
    public void processModuleSubStatementContainer() throws IOException, ParserException {

        YangNode node = manager.getDataModel("src/test/resources/ModuleSubStatementContainer.yang");

        assertThat((node instanceof YangModule), is(true));

        // Check whether the node type is set properly to module.
        assertThat(node.getNodeType(), is(YangNodeType.MODULE_NODE));

        // Check whether the module name is set correctly.
        YangModule yangNode = (YangModule) node;
        assertThat(yangNode.getName(), is("Test"));

        // Check whether the container is child of module
        YangContainer yangContainer = (YangContainer) yangNode.getChild();
        assertThat(yangContainer.getName(), is("valid"));
    }

    /**
     * Checks if container identifier in module is duplicate.
     */
    @Test(expected = ParserException.class)
    public void processModuleDuplicateContainer() throws IOException, ParserException {

        YangNode node = manager.getDataModel("src/test/resources/ModuleDuplicateContainer.yang");
    }

    /**
     * Checks if container identifier in container is duplicate.
     */
    @Test(expected = ParserException.class)
    public void processContainerDuplicateContainer() throws IOException, ParserException {

        YangNode node = manager.getDataModel("src/test/resources/ContainerDuplicateContainer.yang");
    }

    /**
     * Checks if container identifier in list is duplicate.
     */
    @Test(expected = ParserException.class)
    public void processListDuplicateContainer() throws IOException, ParserException {

        YangNode node = manager.getDataModel("src/test/resources/ListDuplicateContainer.yang");
    }

    /**
     * Checks if container identifier collides with list at same level.
     */
    @Test(expected = ParserException.class)
    public void processDuplicateContainerAndList() throws IOException, ParserException {

        YangNode node = manager.getDataModel("src/test/resources/DuplicateContainerAndList.yang");
    }

    /**
     * Checks container statement as sub-statement of container.
     */
    @Test
    public void processContainerSubStatementContainer() throws IOException, ParserException {

        YangNode node = manager.getDataModel("src/test/resources/ContainerSubStatementContainer.yang");

        assertThat((node instanceof YangModule), is(true));

        // Check whether the node type is set properly to module.
        assertThat(node.getNodeType(), is(YangNodeType.MODULE_NODE));

        // Check whether the module name is set correctly.
        YangModule yangNode = (YangModule) node;
        assertThat(yangNode.getName(), is("Test"));

        // Check whether the container is child of module
        YangContainer yangContainer = (YangContainer) yangNode.getChild();
        assertThat(yangContainer.getName(), is("ospf"));

        // Check whether the container is child of container
        YangContainer yangContainer1 = (YangContainer) yangContainer.getChild();
        assertThat(yangContainer1.getName(), is("valid"));
    }

    /**
     * Checks container statement as sub-statement of list.
     */
    @Test
    public void processListSubStatementContainer() throws IOException, ParserException {

        YangNode node = manager.getDataModel("src/test/resources/ListSubStatementContainer.yang");

        assertThat((node instanceof YangModule), is(true));

        // Check whether the node type is set properly to module.
        assertThat(node.getNodeType(), is(YangNodeType.MODULE_NODE));

        // Check whether the module name is set correctly.
        YangModule yangNode = (YangModule) node;
        assertThat(yangNode.getName(), is("Test"));

        // Check whether the list is child of module
        YangList yangList1 = (YangList) yangNode.getChild();
        assertThat(yangList1.getName(), is("ospf"));

        ListIterator<String> keyList = yangList1.getKeyList().listIterator();
        assertThat(keyList.next(), is("process-id"));

        // Check whether the list is child of list
        YangContainer yangContainer = (YangContainer) yangList1.getChild();
        assertThat(yangContainer.getName(), is("interface"));
    }

    /**
     * Checks container with all its sub-statements.
     */
    @Test
    public void processContainerSubStatements() throws IOException, ParserException {

        YangNode node = manager.getDataModel("src/test/resources/ContainerSubStatements.yang");

        assertThat((node instanceof YangModule), is(true));

        // Check whether the node type is set properly to module.
        assertThat(node.getNodeType(), is(YangNodeType.MODULE_NODE));

        // Check whether the module name is set correctly.
        YangModule yangNode = (YangModule) node;
        assertThat(yangNode.getName(), is("Test"));

        // Check whether the container is child of module
        YangContainer yangContainer = (YangContainer) yangNode.getChild();

        // Check whether container properties as set correctly.
        assertThat(yangContainer.getName(), is("ospf"));

        assertThat(yangContainer.isConfig(), is(true));
        assertThat(yangContainer.getPresence(), is("\"ospf logs\""));
        assertThat(yangContainer.getDescription(), is("\"container description\""));
        assertThat(yangContainer.getStatus(), is(YangStatusType.CURRENT));
        assertThat(yangContainer.getReference(), is("\"container reference\""));

        // Check whether leaf properties as set correctly.
        ListIterator<YangLeaf> leafIterator = yangContainer.getListOfLeaf().listIterator();
        YangLeaf leafInfo = leafIterator.next();

        assertThat(leafInfo.getName(), is("invalid-interval"));
        assertThat(leafInfo.getDataType().getDataTypeName(), is("uint16"));
        assertThat(leafInfo.getDataType().getDataType(), is(YangDataTypes.UINT16));
        assertThat(leafInfo.getUnits(), is("\"seconds\""));
        assertThat(leafInfo.getStatus(), is(YangStatusType.CURRENT));
        assertThat(leafInfo.getReference(), is("\"RFC 6020\""));
    }

    /**
     * Checks cardinality of sub-statements of container.
     */
    @Test
    public void processContainerSubStatementCardinality() throws IOException, ParserException {
        thrown.expect(ParserException.class);
        thrown.expectMessage("YANG file error: \"reference\" is defined more than once in \"container valid\".");
        YangNode node = manager.getDataModel("src/test/resources/ContainerSubStatementCardinality.yang");
    }

    /**
     * Checks container as root node.
     */
    @Test
    public void processContainerRootNode() throws IOException, ParserException {
        thrown.expect(ParserException.class);
        thrown.expectMessage("no viable alternative at input 'container'");
        YangNode node = manager.getDataModel("src/test/resources/ContainerRootNode.yang");
    }

    /**
     * Checks invalid identifier for container statement.
     */
    @Test
    public void processContainerInvalidIdentifier() throws IOException, ParserException {
        thrown.expect(ParserException.class);
        thrown.expectMessage("YANG file error : container name 1valid is not valid.");
        YangNode node = manager.getDataModel("src/test/resources/ContainerInvalidIdentifier.yang");
    }
}

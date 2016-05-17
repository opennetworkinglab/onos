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
import org.onosproject.yangutils.datamodel.YangLeafList;
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
 * Test cases for status listener.
 */
public class StatusListenerTest {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    private final YangUtilsParserManager manager = new YangUtilsParserManager();

    /**
     * Checks valid status statement.
     */
    @Test
    public void processStatusStatementCurrent() throws IOException, ParserException {

        YangNode node = manager.getDataModel("src/test/resources/StatusStatementCurrent.yang");

        // Check whether the data model tree returned is of type module.
        assertThat((node instanceof YangModule), is(true));

        // Check whether the node type is set properly to module.
        assertThat(node.getNodeType(), is(YangNodeType.MODULE_NODE));

        // Check whether the module name is set correctly.
        YangModule yangNode = (YangModule) node;
        assertThat(yangNode.getName(), is("Test"));

        ListIterator<YangLeaf> leafIterator = yangNode.getListOfLeaf().listIterator();
        YangLeaf leafInfo = leafIterator.next();

        // Check whether the status is set correctly.
        assertThat(leafInfo.getName(), is("invalid-interval"));
        assertThat(leafInfo.getStatus(), is(YangStatusType.CURRENT));
    }

    /**
     * Checks valid status statement.
     */
    @Test
    public void processStatusStatementDeprecated() throws IOException, ParserException {

        YangNode node = manager.getDataModel("src/test/resources/StatusStatementDeprecated.yang");

        // Check whether the data model tree returned is of type module.
        assertThat((node instanceof YangModule), is(true));

        // Check whether the node type is set properly to module.
        assertThat(node.getNodeType(), is(YangNodeType.MODULE_NODE));

        // Check whether the module name is set correctly.
        YangModule yangNode = (YangModule) node;
        assertThat(yangNode.getName(), is("Test"));

        ListIterator<YangLeaf> leafIterator = yangNode.getListOfLeaf().listIterator();
        YangLeaf leafInfo = leafIterator.next();

        // Check whether the status is set correctly.
        assertThat(leafInfo.getName(), is("invalid-interval"));
        assertThat(leafInfo.getStatus(), is(YangStatusType.DEPRECATED));
    }

    /**
     * Checks valid status statement.
     */
    @Test
    public void processStatusStatementObsolete() throws IOException, ParserException {

        YangNode node = manager.getDataModel("src/test/resources/StatusStatementObsolete.yang");

        // Check whether the data model tree returned is of type module.
        assertThat((node instanceof YangModule), is(true));

        // Check whether the node type is set properly to module.
        assertThat(node.getNodeType(), is(YangNodeType.MODULE_NODE));

        // Check whether the module name is set correctly.
        YangModule yangNode = (YangModule) node;
        assertThat(yangNode.getName(), is("Test"));

        ListIterator<YangLeaf> leafIterator = yangNode.getListOfLeaf().listIterator();
        YangLeaf leafInfo = leafIterator.next();

        // Check whether the status is set correctly.
        assertThat(leafInfo.getName(), is("invalid-interval"));
        assertThat(leafInfo.getStatus(), is(YangStatusType.OBSOLETE));
    }

    /**
     * Checks whether exception is thrown for invalid status statement.
     */
    @Test
    public void processStatusWithoutStatementEnd() throws IOException, ParserException {
        thrown.expect(ParserException.class);
        thrown.expectMessage("missing ';' at '}'");
        YangNode node = manager.getDataModel("src/test/resources/StatusWithoutStatementEnd.yang");
    }

    /**
     * Checks whether exception is thrown for invalid status statement.
     */
    @Test
    public void processStatusInvalidValue() throws IOException, ParserException {
        thrown.expect(ParserException.class);
        thrown.expectMessage("YANG file error : status invalid is not valid.");
        YangNode node = manager.getDataModel("src/test/resources/StatusInvalidValue.yang");
    }

    /**
     * Checks status statement as sub-statement of module.
     */
    @Test
    public void processModuleSubStatementStatus() throws IOException, ParserException {
        thrown.expect(ParserException.class);
        thrown.expectMessage("mismatched input 'status' expecting {'anyxml', 'augment', 'choice', 'contact', "
                + "'container', 'description', 'extension', 'deviation', 'feature', 'grouping', 'identity', 'import',"
                + " 'include', 'leaf', 'leaf-list', 'list', 'notification', 'organization', 'reference', "
                + "'revision', 'rpc', 'typedef', 'uses', '}'}");
        YangNode node = manager.getDataModel("src/test/resources/ModuleSubStatementStatus.yang");
    }

    /**
     * Checks status statement as sub-statement of container.
     */
    @Test
    public void processContainerSubStatementStatus() throws IOException, ParserException {

        YangNode node = manager.getDataModel("src/test/resources/ContainerSubStatementStatus.yang");

        // Check whether the data model tree returned is of type module.
        assertThat((node instanceof YangModule), is(true));

        // Check whether the node type is set properly to module.
        assertThat(node.getNodeType(), is(YangNodeType.MODULE_NODE));

        // Check whether the module name is set correctly.
        YangModule yangNode = (YangModule) node;
        assertThat(yangNode.getName(), is("Test"));

        // Check whether status is set correctly.
        YangContainer container = (YangContainer) yangNode.getChild();
        assertThat(container.getName(), is("valid"));
        assertThat(container.isConfig(), is(true));
        assertThat(container.getStatus(), is(YangStatusType.OBSOLETE));

        // Check whether leaf properties as set correctly.
        ListIterator<YangLeaf> leafIterator = container.getListOfLeaf().listIterator();
        YangLeaf leafInfo = leafIterator.next();

        assertThat(leafInfo.getName(), is("invalid-interval"));
        assertThat(leafInfo.getDataType().getDataTypeName(), is("uint16"));
        assertThat(leafInfo.getDataType().getDataType(), is(YangDataTypes.UINT16));
        assertThat(leafInfo.getUnits(), is("\"seconds\""));
        assertThat(leafInfo.getDescription(), is("\"Interval before a route is declared invalid\""));
        assertThat(leafInfo.isMandatory(), is(true));
        assertThat(leafInfo.getStatus(), is(YangStatusType.CURRENT));
        assertThat(leafInfo.getReference(), is("\"RFC 6020\""));
    }

    /**
     * Checks status statement as sub-statement of list.
     */
    @Test
    public void processListSubStatementStatus() throws IOException, ParserException {

        YangNode node = manager.getDataModel("src/test/resources/ListSubStatementStatus.yang");

        assertThat((node instanceof YangModule), is(true));

        // Check whether the node type is set properly to module.
        assertThat(node.getNodeType(), is(YangNodeType.MODULE_NODE));

        // Check whether the module name is set correctly.
        YangModule yangNode = (YangModule) node;
        assertThat(yangNode.getName(), is("Test"));

        // Check whether the list is child of module and status is set.
        YangList yangList = (YangList) yangNode.getChild();
        assertThat(yangList.getName(), is("valid"));
        assertThat(yangList.isConfig(), is(true));
        assertThat(yangList.getStatus(), is(YangStatusType.CURRENT));

        // Check whether leaf properties as set correctly.
        ListIterator<YangLeaf> leafIterator = yangList.getListOfLeaf().listIterator();
        YangLeaf leafInfo = leafIterator.next();

        assertThat(leafInfo.getName(), is("invalid-interval"));
        assertThat(leafInfo.getDataType().getDataTypeName(), is("uint16"));
        assertThat(leafInfo.getDataType().getDataType(), is(YangDataTypes.UINT16));
        assertThat(leafInfo.getUnits(), is("\"seconds\""));
        assertThat(leafInfo.getDescription(), is("\"Interval before a route is declared invalid\""));
        assertThat(leafInfo.isMandatory(), is(true));
        assertThat(leafInfo.getStatus(), is(YangStatusType.CURRENT));
        assertThat(leafInfo.getReference(), is("\"RFC 6020\""));
    }

    /**
     * Checks valid status statement as sub-statement of leaf-list.
     */
    @Test
    public void processLeafListSubStatementStatus() throws IOException, ParserException {

        YangNode node = manager.getDataModel("src/test/resources/LeafListSubStatementStatus.yang");

        // Check whether the data model tree returned is of type module.
        assertThat((node instanceof YangModule), is(true));

        // Check whether the node type is set properly to module.
        assertThat(node.getNodeType(), is(YangNodeType.MODULE_NODE));

        // Check whether the module name is set correctly.
        YangModule yangNode = (YangModule) node;
        assertThat(yangNode.getName(), is("Test"));

        ListIterator<YangLeafList> leafListIterator = yangNode.getListOfLeafList().listIterator();
        YangLeafList leafListInfo = leafListIterator.next();

        // Check whether status is set correctly.
        assertThat(leafListInfo.getName(), is("invalid-interval"));
        assertThat(leafListInfo.isConfig(), is(true));
        assertThat(leafListInfo.getStatus(), is(YangStatusType.CURRENT));
    }

    /**
     * Checks default value of status statement.
     */
    @Test
    public void processStatusDefaultValue() throws IOException, ParserException {

        YangNode node = manager.getDataModel("src/test/resources/StatusDefaultValue.yang");

        // Check whether the data model tree returned is of type module.
        assertThat((node instanceof YangModule), is(true));

        // Check whether the node type is set properly to module.
        assertThat(node.getNodeType(), is(YangNodeType.MODULE_NODE));

        // Check whether the module name is set correctly.
        YangModule yangNode = (YangModule) node;
        assertThat(yangNode.getName(), is("Test"));

        ListIterator<YangLeafList> leafListIterator = yangNode.getListOfLeafList().listIterator();
        YangLeafList leafListInfo = leafListIterator.next();

        // Check whether status is set correctly.
        assertThat(leafListInfo.getName(), is("invalid-interval"));
        assertThat(leafListInfo.isConfig(), is(true));
        assertThat(leafListInfo.getStatus(), is(YangStatusType.CURRENT));
    }
}

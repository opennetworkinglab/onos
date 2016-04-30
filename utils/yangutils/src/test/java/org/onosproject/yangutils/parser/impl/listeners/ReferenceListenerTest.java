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
 * Test case for reference listener.
 */
public class ReferenceListenerTest {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    private final YangUtilsParserManager manager = new YangUtilsParserManager();

    /**
     * Checks valid reference statement.
     */
    @Test
    public void processReferenceStatement() throws IOException, ParserException {

        YangNode node = manager.getDataModel("src/test/resources/ReferenceStatement.yang");

        // Check whether the data model tree returned is of type module.
        assertThat((node instanceof YangModule), is(true));

        // Check whether the node type is set properly to module.
        assertThat(node.getNodeType(), is(YangNodeType.MODULE_NODE));

        // Check whether the module name is set correctly.
        YangModule yangNode = (YangModule) node;
        assertThat(yangNode.getName(), is("Test"));

        ListIterator<YangLeaf> leafIterator = yangNode.getListOfLeaf().listIterator();
        YangLeaf leafInfo = leafIterator.next();

        // Check whether the reference is set correctly.
        assertThat(leafInfo.getName(), is("invalid-interval"));
        assertThat(leafInfo.getReference(), is("\"RFC 6020\""));
    }

    /**
     * Checks whether exception is thrown for invalid reference statement.
     */
    @Test
    public void processReferenceWithoutStatementEnd() throws IOException, ParserException {
        thrown.expect(ParserException.class);
        thrown.expectMessage("mismatched input '}' expecting {';', '+'}");
        YangNode node = manager.getDataModel("src/test/resources/ReferenceWithoutStatementEnd.yang");
    }

    /**
     * Checks valid reference statement under module.
     */
    @Test
    public void processModuleSubStatementReference() throws IOException, ParserException {

        YangNode node = manager.getDataModel("src/test/resources/ModuleSubStatementReference.yang");

        // Check whether the data model tree returned is of type module.
        assertThat((node instanceof YangModule), is(true));

        // Check whether the node type is set properly to module.
        assertThat(node.getNodeType(), is(YangNodeType.MODULE_NODE));

        // Check whether the module name is set correctly.
        YangModule yangNode = (YangModule) node;
        assertThat(yangNode.getName(), is("Test"));

        // Check whether the reference is set correctly.
        assertThat(yangNode.getReference(), is("\"RFC 6020\""));
    }

    /**
     * Checks valid reference statement under module.
     */
    @Test
    public void processReferenceEmptyStatement() throws IOException, ParserException {

        YangNode node = manager.getDataModel("src/test/resources/ReferenceEmptyStatement.yang");

        // Check whether the data model tree returned is of type module.
        assertThat((node instanceof YangModule), is(true));

        // Check whether the node type is set properly to module.
        assertThat(node.getNodeType(), is(YangNodeType.MODULE_NODE));

        // Check whether the module name is set correctly.
        YangModule yangNode = (YangModule) node;
        assertThat(yangNode.getName(), is("Test"));

        // Check whether the reference is set correctly.
        assertThat(yangNode.getReference(), is("\"\""));
    }

    /**
     * Checks valid reference statement as sub-statement of revision.
     */
    @Test
    public void processRevisionSubStatementReference() throws IOException, ParserException {

        YangNode node = manager.getDataModel("src/test/resources/RevisionSubStatementReference.yang");

        // Check whether the data model tree returned is of type module.
        assertThat((node instanceof YangModule), is(true));

        // Check whether the node type is set properly to module.
        assertThat(node.getNodeType(), is(YangNodeType.MODULE_NODE));

        // Check whether the module name is set correctly.
        YangModule yangNode = (YangModule) node;
        assertThat(yangNode.getName(), is("Test"));

        // Check whether the reference is set correctly.
        assertThat(yangNode.getRevision().getReference(), is("\"revision reference\""));
    }

    /**
     * Checks reference statement as sub-statement of container.
     */
    @Test
    public void processContainerSubStatementReference() throws IOException, ParserException {

        YangNode node = manager.getDataModel("src/test/resources/ContainerSubStatementReference.yang");

        // Check whether the data model tree returned is of type module.
        assertThat((node instanceof YangModule), is(true));

        // Check whether the node type is set properly to module.
        assertThat(node.getNodeType(), is(YangNodeType.MODULE_NODE));

        // Check whether the module name is set correctly.
        YangModule yangNode = (YangModule) node;
        assertThat(yangNode.getName(), is("Test"));

        // Check whether the reference value is set correctly.
        YangContainer container = (YangContainer) yangNode.getChild();
        assertThat(container.getName(), is("valid"));
        assertThat(container.getReference(), is("\"container reference\""));

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
     * Checks reference statement as sub-statement of list.
     */
    @Test
    public void processListSubStatementReference() throws IOException, ParserException {

        YangNode node = manager.getDataModel("src/test/resources/ListSubStatementReference.yang");

        assertThat((node instanceof YangModule), is(true));

        // Check whether the node type is set properly to module.
        assertThat(node.getNodeType(), is(YangNodeType.MODULE_NODE));

        // Check whether the module name is set correctly.
        YangModule yangNode = (YangModule) node;
        assertThat(yangNode.getName(), is("Test"));

        // Check whether the list is child of module and description value is set correctly.
        YangList yangList = (YangList) yangNode.getChild();
        assertThat(yangList.getName(), is("valid"));
        assertThat(yangList.isConfig(), is(true));
        assertThat(yangList.getReference(), is("\"list reference\""));

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
     * Checks valid reference statement as sub-statement of leaf-list.
     */
    @Test
    public void processLeafListSubStatementReference() throws IOException, ParserException {

        YangNode node = manager.getDataModel("src/test/resources/LeafListSubStatementReference.yang");

        // Check whether the data model tree returned is of type module.
        assertThat((node instanceof YangModule), is(true));

        // Check whether the node type is set properly to module.
        assertThat(node.getNodeType(), is(YangNodeType.MODULE_NODE));

        // Check whether the module name is set correctly.
        YangModule yangNode = (YangModule) node;
        assertThat(yangNode.getName(), is("Test"));

        ListIterator<YangLeafList> leafListIterator = yangNode.getListOfLeafList().listIterator();
        YangLeafList leafListInfo = leafListIterator.next();

        // Check whether description value is set correctly.
        assertThat(leafListInfo.getName(), is("invalid-interval"));
        assertThat(leafListInfo.getReference(), is("\"RFC 6020\""));
    }
}

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

import org.junit.Rule;
import org.junit.Test;

import org.junit.rules.ExpectedException;
import org.onosproject.yangutils.datamodel.YangLeaf;
import org.onosproject.yangutils.datamodel.YangModule;
import org.onosproject.yangutils.datamodel.YangNode;
import org.onosproject.yangutils.datamodel.YangNodeType;
import org.onosproject.yangutils.datamodel.YangDataTypes;
import org.onosproject.yangutils.datamodel.YangStatusType;
import org.onosproject.yangutils.datamodel.YangContainer;
import org.onosproject.yangutils.datamodel.YangList;
import org.onosproject.yangutils.datamodel.YangLeafList;
import org.onosproject.yangutils.parser.exceptions.ParserException;
import org.onosproject.yangutils.parser.impl.YangUtilsParserManager;

import java.io.IOException;
import java.util.ListIterator;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

/**
 * Test cases for config listener.
 */
public class ConfigListenerTest {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    private final YangUtilsParserManager manager = new YangUtilsParserManager();

    /**
     * Checks valid config statement.
     */
    @Test
    public void processConfigTrue() throws IOException, ParserException {

        YangNode node = manager.getDataModel("src/test/resources/ConfigTrue.yang");

        // Check whether the data model tree returned is of type module.
        assertThat((node instanceof YangModule), is(true));

        // Check whether the node type is set properly to module.
        assertThat(node.getNodeType(), is(YangNodeType.MODULE_NODE));

        // Check whether the module name is set correctly.
        YangModule yangNode = (YangModule) node;
        assertThat(yangNode.getName(), is("Test"));

        ListIterator<YangLeaf<?>> leafIterator = yangNode.getListOfLeaf().listIterator();
        YangLeaf<?> leafInfo = leafIterator.next();

        // Check whether the Config value is set correctly.
        assertThat(leafInfo.getLeafName(), is("invalid-interval"));
        assertThat(leafInfo.isConfig(), is(true));
    }

    /**
     * Checks valid config statement.
     */
    @Test
    public void processConfigFalse() throws IOException, ParserException {

        YangNode node = manager.getDataModel("src/test/resources/ConfigFalse.yang");

        // Check whether the data model tree returned is of type module.
        assertThat((node instanceof YangModule), is(true));

        // Check whether the node type is set properly to module.
        assertThat(node.getNodeType(), is(YangNodeType.MODULE_NODE));

        // Check whether the module name is set correctly.
        YangModule yangNode = (YangModule) node;
        assertThat(yangNode.getName(), is("Test"));

        ListIterator<YangLeaf<?>> leafIterator = yangNode.getListOfLeaf().listIterator();
        YangLeaf<?> leafInfo = leafIterator.next();

        // Check whether the Config value is set correctly.
        assertThat(leafInfo.getLeafName(), is("invalid-interval"));
        assertThat(leafInfo.isConfig(), is(false));
    }

    /**
     * Checks invalid config statement and expects parser exception.
     */
    @Test
    public void processConfigWithoutStatementEnd() throws IOException, ParserException {
        thrown.expect(ParserException.class);
        thrown.expectMessage("missing ';' at '}'");
        YangNode node = manager.getDataModel("src/test/resources/ConfigWithoutStatementEnd.yang");
    }

    /**
     * Checks invalid config statement and expects parser exception.
     */
    @Test
    public void processConfigInvalidValue() throws IOException, ParserException {
        thrown.expect(ParserException.class);
        thrown.expectMessage("mismatched input 'invalid' expecting {'false', 'true'}");
        YangNode node = manager.getDataModel("src/test/resources/ConfigInvalidValue.yang");
    }

    /**
     * Checks invalid config statement and expects parser exception.
     */
    @Test
    public void processConfigEmptyValue() throws IOException, ParserException {
        thrown.expect(ParserException.class);
        thrown.expectMessage("missing {'false', 'true'} at ';'");
        YangNode node = manager.getDataModel("src/test/resources/ConfigEmptyValue.yang");
    }

    /**
     * Checks config statement as sub-statement of module.
     */
    @Test
    public void processModuleSubStatementConfig() throws IOException, ParserException {
        thrown.expect(ParserException.class);
        thrown.expectMessage("mismatched input 'config' expecting {'augment', 'choice', 'contact', 'container',"
                + " 'description', 'extension', 'deviation', 'feature', 'grouping', 'identity', 'import', 'include', "
                + "'leaf', 'leaf-list', 'list', 'namespace', 'notification', 'organization', 'prefix', 'reference',"
                + " 'revision', 'rpc', 'typedef', 'uses', 'yang-version', '}'}");
        YangNode node = manager.getDataModel("src/test/resources/ModuleSubStatementConfig.yang");
    }

    /**
     * Checks config statement as sub-statement of container.
     */
    @Test
    public void processContainerSubStatementConfig() throws IOException, ParserException {

        YangNode node = manager.getDataModel("src/test/resources/ContainerSubStatementConfig.yang");

        // Check whether the data model tree returned is of type module.
        assertThat((node instanceof YangModule), is(true));

        // Check whether the node type is set properly to module.
        assertThat(node.getNodeType(), is(YangNodeType.MODULE_NODE));

        // Check whether the module name is set correctly.
        YangModule yangNode = (YangModule) node;
        assertThat(yangNode.getName(), is("Test"));

        // Check whether the config value is set correctly.
        YangContainer container = (YangContainer) yangNode.getChild();
        assertThat(container.getName(), is("valid"));
        assertThat(container.isConfig(), is(true));

        // Check whether leaf properties as set correctly.
        ListIterator<YangLeaf<?>> leafIterator = container.getListOfLeaf().listIterator();
        YangLeaf<?> leafInfo = leafIterator.next();

        assertThat(leafInfo.getLeafName(), is("invalid-interval"));
        assertThat(leafInfo.getDataType().getDataTypeName(), is("\"uint16\""));
        assertThat(leafInfo.getDataType().getDataType(), is(YangDataTypes.UINT16));
        assertThat(leafInfo.getUnits(), is("\"seconds\""));
        assertThat(leafInfo.getDescription(), is("\"Interval before a route is declared invalid\""));
        assertThat(leafInfo.isMandatory(), is(true));
        assertThat(leafInfo.getStatus(), is(YangStatusType.CURRENT));
        assertThat(leafInfo.getReference(), is("\"RFC 6020\""));
    }

    /**
     * Checks config statement as sub-statement of list.
     */
    @Test
    public void processListSubStatementConfig() throws IOException, ParserException {

        YangNode node = manager.getDataModel("src/test/resources/ListSubStatementConfig.yang");

        assertThat((node instanceof YangModule), is(true));

        // Check whether the node type is set properly to module.
        assertThat(node.getNodeType(), is(YangNodeType.MODULE_NODE));

        // Check whether the module name is set correctly.
        YangModule yangNode = (YangModule) node;
        assertThat(yangNode.getName(), is("Test"));

        // Check whether the list is child of module and config value is set.
        YangList yangList = (YangList) yangNode.getChild();
        assertThat(yangList.getName(), is("valid"));
        assertThat(yangList.isConfig(), is(true));

        // Check whether leaf properties as set correctly.
        ListIterator<YangLeaf<?>> leafIterator = yangList.getListOfLeaf().listIterator();
        YangLeaf<?> leafInfo = leafIterator.next();

        assertThat(leafInfo.getLeafName(), is("invalid-interval"));
        assertThat(leafInfo.getDataType().getDataTypeName(), is("\"uint16\""));
        assertThat(leafInfo.getDataType().getDataType(), is(YangDataTypes.UINT16));
        assertThat(leafInfo.getUnits(), is("\"seconds\""));
        assertThat(leafInfo.getDescription(), is("\"Interval before a route is declared invalid\""));
        assertThat(leafInfo.isMandatory(), is(true));
        assertThat(leafInfo.getStatus(), is(YangStatusType.CURRENT));
        assertThat(leafInfo.getReference(), is("\"RFC 6020\""));
    }

    /**
     * Checks valid config statement as sub-statement of leaf-list.
     */
    @Test
    public void processLeafListSubStatementConfig() throws IOException, ParserException {

        YangNode node = manager.getDataModel("src/test/resources/LeafListSubStatementConfig.yang");

        // Check whether the data model tree returned is of type module.
        assertThat((node instanceof YangModule), is(true));

        // Check whether the node type is set properly to module.
        assertThat(node.getNodeType(), is(YangNodeType.MODULE_NODE));

        // Check whether the module name is set correctly.
        YangModule yangNode = (YangModule) node;
        assertThat(yangNode.getName(), is("Test"));

        ListIterator<YangLeafList<?>> leafListIterator = yangNode.getListOfLeafList().listIterator();
        YangLeafList<?> leafListInfo = leafListIterator.next();

        // Check whether config value is set correctly.
        assertThat(leafListInfo.getLeafName(), is("invalid-interval"));
        assertThat(leafListInfo.isConfig(), is(true));
    }
}
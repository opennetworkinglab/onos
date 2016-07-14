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

        ListIterator<YangLeaf> leafIterator = yangNode.getListOfLeaf().listIterator();
        YangLeaf leafInfo = leafIterator.next();

        // Check whether the Config value is set correctly.
        assertThat(leafInfo.getName(), is("invalid-interval"));
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

        ListIterator<YangLeaf> leafIterator = yangNode.getListOfLeaf().listIterator();
        YangLeaf leafInfo = leafIterator.next();

        // Check whether the Config value is set correctly.
        assertThat(leafInfo.getName(), is("invalid-interval"));
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
        thrown.expectMessage("YANG file error : config value invalid is not valid.");
        YangNode node = manager.getDataModel("src/test/resources/ConfigInvalidValue.yang");
    }

    /**
     * Checks invalid config statement and expects parser exception.
     */
    @Test
    public void processConfigEmptyValue() throws IOException, ParserException {
        thrown.expect(ParserException.class);
        thrown.expectMessage("no viable alternative at input ';'");
        YangNode node = manager.getDataModel("src/test/resources/ConfigEmptyValue.yang");
    }

    /**
     * Checks config statement as sub-statement of module.
     */
    @Test
    public void processModuleSubStatementConfig() throws IOException, ParserException {
        thrown.expect(ParserException.class);
        thrown.expectMessage("mismatched input 'config' expecting {'anyxml', 'augment', 'choice', 'contact', "
                + "'container', 'description', 'extension', 'deviation', 'feature', 'grouping', 'identity', 'import',"
                + " 'include', 'leaf', 'leaf-list', 'list', 'notification', 'organization', 'reference',"
                + " 'revision', 'rpc', 'typedef', 'uses', '}'}");
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

        ListIterator<YangLeafList> leafListIterator = yangNode.getListOfLeafList().listIterator();
        YangLeafList leafListInfo = leafListIterator.next();

        // Check whether config value is set correctly.
        assertThat(leafListInfo.getName(), is("invalid-interval"));
        assertThat(leafListInfo.isConfig(), is(true));
    }

    /**
     * Checks config statement's default Value.
     */
    @Test
    public void processConfigDefaultValue() throws IOException, ParserException {

        YangNode node = manager.getDataModel("src/test/resources/ConfigDefaultValue.yang");

        assertThat((node instanceof YangModule), is(true));
        assertThat(node.getNodeType(), is(YangNodeType.MODULE_NODE));
        YangModule yangNode = (YangModule) node;
        assertThat(yangNode.getName(), is("Test"));

        // Check whether the config value is set correctly.
        YangContainer container = (YangContainer) yangNode.getChild();
        assertThat(container.getName(), is("valid"));
        assertThat(container.isConfig(), is(true));

        // Check whether leaf properties as set correctly.
        ListIterator<YangLeaf> leafIterator = container.getListOfLeaf().listIterator();
        YangLeaf leafInfo = leafIterator.next();

        assertThat(leafInfo.getName(), is("invalid-interval"));
        assertThat(leafInfo.isConfig(), is(true));
    }

    /**
     * Checks whether exception is throw when node's parent config set to false,
     * no node underneath it can have config set to true.
     */
    @Test
    public void processConfigFalseParentContainerChildLeafList() throws IOException, ParserException {
        thrown.expect(ParserException.class);
        thrown.expectMessage("Internal parser error detected: Unhandled parsed data at container \"valid\" after "
                + "processing.\nError Information: If a container has \"config\" set to \"false\", no node underneath "
                + "it can have \"config\" set to \"true\".");
        YangNode node = manager.getDataModel("src/test/resources/ConfigFalseParentContainerChildLeafList.yang");
    }

    /**
     * Checks whether exception is throw when node's parent config set to false,
     * no node underneath it can have config set to true.
     */
    @Test
    public void processConfigFalseParentContainerChildLeaf() throws IOException, ParserException {
        thrown.expect(ParserException.class);
        thrown.expectMessage("Internal parser error detected: Unhandled parsed data at container \"valid\" after "
                + "processing.\nError Information: If a container has \"config\" set to \"false\", no node underneath "
                + "it can have \"config\" set to \"true\".");
        YangNode node = manager.getDataModel("src/test/resources/ConfigFalseParentContainerChildLeaf.yang");
    }

    /**
     * Checks whether exception is throw when node's parent config set to false,
     * no node underneath it can have config set to true.
     */
    @Test
    public void processConfigFalseParentListChildLeafList() throws IOException, ParserException {
        thrown.expect(ParserException.class);
        thrown.expectMessage("Internal parser error detected: Unhandled parsed data at list \"valid\" after"
                + " processing.\nError Information: If a list has \"config\" set to \"false\", no node underneath"
                + " it can have \"config\" set to \"true\".");
        YangNode node = manager.getDataModel("src/test/resources/ConfigFalseParentListChildLeafList.yang");
    }

    /**
     * Checks whether exception is throw when node's parent config set to false,
     * no node underneath it can have config set to true.
     */
    @Test
    public void processConfigFalseParentListChildLeaf() throws IOException, ParserException {
        thrown.expect(ParserException.class);
        thrown.expectMessage("Internal parser error detected: Unhandled parsed data at list \"valid\" after"
                + " processing.\nError Information: If a list has \"config\" set to \"false\", no node underneath"
                + " it can have \"config\" set to \"true\".");
        YangNode node = manager.getDataModel("src/test/resources/ConfigFalseParentListChildLeaf.yang");
    }

    /**
     * Checks when config is not specified, the default is same as the parent's schema node's
     * config statement's value.
     */
    @Test
    public void processNoConfigContainerSubStatementContainer() throws IOException, ParserException {

        YangNode node = manager.getDataModel("src/test/resources/NoConfigContainerSubStatementContainer.yang");

        assertThat((node instanceof YangModule), is(true));
        assertThat(node.getNodeType(), is(YangNodeType.MODULE_NODE));
        YangModule yangNode = (YangModule) node;
        assertThat(yangNode.getName(), is("Test"));

        // Check whether the config value is set correctly.
        YangContainer container = (YangContainer) yangNode.getChild();
        assertThat(container.getName(), is("hello"));
        assertThat(container.isConfig(), is(true));

        YangNode containerNode = container.getChild();
        assertThat(containerNode instanceof YangContainer, is(true));
        YangContainer childContainer = (YangContainer) containerNode;
        assertThat(childContainer.getName(), is("valid"));
        assertThat(childContainer.isConfig(), is(true));
    }

    /**
     * Checks when config is not specified, the default is same as the parent's schema node's
     * config statement's value.
     */
    @Test
    public void processNoConfigContainerSubStatementLeafList() throws IOException, ParserException {

        YangNode node = manager.getDataModel("src/test/resources/NoConfigContainerSubStatementLeafList.yang");

        assertThat((node instanceof YangModule), is(true));
        assertThat(node.getNodeType(), is(YangNodeType.MODULE_NODE));
        YangModule yangNode = (YangModule) node;
        assertThat(yangNode.getName(), is("Test"));

        // Check whether the config value is set correctly.
        YangContainer container = (YangContainer) yangNode.getChild();
        assertThat(container.getName(), is("valid"));
        assertThat(container.isConfig(), is(true));

        ListIterator<YangLeafList> leafListIterator = container.getListOfLeafList().listIterator();
        YangLeafList leafListInfo = leafListIterator.next();

        // Check whether config value is set correctly.
        assertThat(leafListInfo.getName(), is("invalid-interval"));
        assertThat(leafListInfo.isConfig(), is(true));

    }

    /**
     * Checks when config is not specified, the default is same as the parent's schema node's
     * config statement's value.
     */
    @Test
    public void processNoConfigContainerSubStatementLeaf() throws IOException, ParserException {

        YangNode node = manager.getDataModel("src/test/resources/NoConfigContainerSubStatementLeaf.yang");

        assertThat((node instanceof YangModule), is(true));
        assertThat(node.getNodeType(), is(YangNodeType.MODULE_NODE));
        YangModule yangNode = (YangModule) node;
        assertThat(yangNode.getName(), is("Test"));

        // Check whether the config value is set correctly.
        YangContainer container = (YangContainer) yangNode.getChild();
        assertThat(container.getName(), is("valid"));
        assertThat(container.isConfig(), is(true));

        // Check whether leaf properties as set correctly.
        ListIterator<YangLeaf> leafIterator = container.getListOfLeaf().listIterator();
        YangLeaf leafInfo = leafIterator.next();

        assertThat(leafInfo.getName(), is("invalid-interval"));
        assertThat(leafInfo.isConfig(), is(true));
    }

    /**
     * Checks when config is not specified, the default is same as the parent's schema node's
     * config statement's value.
     */
    @Test
    public void processNoConfigContainerSubStatementList() throws IOException, ParserException {

        YangNode node = manager.getDataModel("src/test/resources/NoConfigContainerSubStatementList.yang");

        assertThat((node instanceof YangModule), is(true));
        assertThat(node.getNodeType(), is(YangNodeType.MODULE_NODE));
        YangModule yangNode = (YangModule) node;
        assertThat(yangNode.getName(), is("Test"));

        // Check whether the config value is set correctly.
        YangContainer container = (YangContainer) yangNode.getChild();
        assertThat(container.getName(), is("hello"));
        assertThat(container.isConfig(), is(true));

        YangNode listNode = container.getChild();
        assertThat(listNode instanceof YangList, is(true));
        YangList childList = (YangList) listNode;
        assertThat(childList.getName(), is("valid"));
        assertThat(childList.isConfig(), is(true));

    }

    /**
     * Checks when config is not specified, the default is same as the parent's schema node's
     * config statement's value.
     */
    @Test
    public void processNoConfigListSubStatementContainer() throws IOException, ParserException {

        YangNode node = manager.getDataModel("src/test/resources/NoConfigListSubStatementContainer.yang");

        assertThat((node instanceof YangModule), is(true));
        assertThat(node.getNodeType(), is(YangNodeType.MODULE_NODE));
        YangModule yangNode = (YangModule) node;
        assertThat(yangNode.getName(), is("Test"));

        // Check whether the config value is set correctly.
        YangList list1 = (YangList) yangNode.getChild();
        assertThat(list1.getName(), is("list1"));
        assertThat(list1.isConfig(), is(true));

        YangNode containerNode = list1.getChild();
        assertThat(containerNode instanceof YangContainer, is(true));
        YangContainer childContainer = (YangContainer) containerNode;
        assertThat(childContainer.getName(), is("container1"));
        assertThat(childContainer.isConfig(), is(true));
    }

    /**
     * Checks when config is not specified, the default is same as the parent's schema node's
     * config statement's value.
     */
    @Test
    public void processNoConfigListSubStatementLeafList() throws IOException, ParserException {

        YangNode node = manager.getDataModel("src/test/resources/NoConfigListSubStatementLeafList.yang");

        assertThat((node instanceof YangModule), is(true));
        assertThat(node.getNodeType(), is(YangNodeType.MODULE_NODE));
        YangModule yangNode = (YangModule) node;
        assertThat(yangNode.getName(), is("Test"));

        // Check whether the config value is set correctly.
        YangList list1 = (YangList) yangNode.getChild();
        assertThat(list1.getName(), is("valid"));
        assertThat(list1.isConfig(), is(true));

        ListIterator<YangLeafList> leafListIterator = list1.getListOfLeafList().listIterator();
        YangLeafList leafListInfo = leafListIterator.next();

        // Check whether config value is set correctly.
        assertThat(leafListInfo.getName(), is("invalid-interval"));
        assertThat(leafListInfo.isConfig(), is(true));
    }

    /**
     * Checks when config is not specified, the default is same as the parent's schema node's
     * config statement's value.
     */
    @Test
    public void processNoConfigListSubStatementLeaf() throws IOException, ParserException {

        YangNode node = manager.getDataModel("src/test/resources/NoConfigListSubStatementLeaf.yang");

        assertThat((node instanceof YangModule), is(true));
        assertThat(node.getNodeType(), is(YangNodeType.MODULE_NODE));
        YangModule yangNode = (YangModule) node;
        assertThat(yangNode.getName(), is("Test"));

        // Check whether the config value is set correctly.
        YangList list1 = (YangList) yangNode.getChild();
        assertThat(list1.getName(), is("valid"));
        assertThat(list1.isConfig(), is(true));

        // Check whether leaf properties as set correctly.
        ListIterator<YangLeaf> leafIterator = list1.getListOfLeaf().listIterator();
        YangLeaf leafInfo = leafIterator.next();

        assertThat(leafInfo.getName(), is("invalid-interval"));
        assertThat(leafInfo.isConfig(), is(true));
    }

    /**
     * Checks when config is not specified, the default is same as the parent's schema node's
     * config statement's value.
     */
    @Test
    public void processNoConfigListSubStatementList() throws IOException, ParserException {

        YangNode node = manager.getDataModel("src/test/resources/NoConfigListSubStatementList.yang");

        assertThat((node instanceof YangModule), is(true));
        assertThat(node.getNodeType(), is(YangNodeType.MODULE_NODE));
        YangModule yangNode = (YangModule) node;
        assertThat(yangNode.getName(), is("Test"));

        // Check whether the config value is set correctly.
        YangList list1 = (YangList) yangNode.getChild();
        assertThat(list1.getName(), is("valid"));
        assertThat(list1.isConfig(), is(true));

        YangNode listNode = list1.getChild();
        assertThat(listNode instanceof YangList, is(true));
        YangList childList = (YangList) listNode;
        assertThat(childList.getName(), is("list1"));
        assertThat(childList.isConfig(), is(true));
    }
}

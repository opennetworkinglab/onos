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
import org.junit.Test;
import org.onosproject.yangutils.datamodel.YangContainer;
import org.onosproject.yangutils.datamodel.YangGrouping;
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
 * Test cases for testing grouping listener.
 */
public class GroupingListenerTest {

    private final YangUtilsParserManager manager = new YangUtilsParserManager();

    /**
     * Checks grouping statement inside module.
     */
    @Test
    public void processGroupingInModule() throws IOException, ParserException {

        YangNode node = manager.getDataModel("src/test/resources/GroupingInModule.yang");

        // Check whether the data model tree returned is of type module.
        assertThat((node instanceof YangModule), is(true));

        // Check whether the node type is set properly to module.
        assertThat(node.getNodeType(), is(YangNodeType.MODULE_NODE));

        // Check whether the module name is set correctly.
        YangModule yangNode = (YangModule) node;
        assertThat(yangNode.getName(), is("Test"));

        YangGrouping yangGrouping = (YangGrouping) yangNode.getChild();
        assertThat(yangGrouping.getName(), is("endpoint"));

        ListIterator<YangLeaf> leafIterator = yangGrouping.getListOfLeaf().listIterator();
        YangLeaf leafInfo = leafIterator.next();

        assertThat(leafInfo.getName(), is("address"));
    }

    /**
     * Checks grouping statement inside container.
     */
    @Test
    public void processGroupingInContainer() throws IOException, ParserException {

        YangNode node = manager.getDataModel("src/test/resources/GroupingInContainer.yang");

        // Check whether the data model tree returned is of type module.
        assertThat((node instanceof YangModule), is(true));

        // Check whether the node type is set properly to module.
        assertThat(node.getNodeType(), is(YangNodeType.MODULE_NODE));

        // Check whether the module name is set correctly.
        YangModule yangNode = (YangModule) node;
        assertThat(yangNode.getName(), is("Test"));

        YangContainer yangContainer = (YangContainer) yangNode.getChild();
        assertThat(yangContainer.getName(), is("valid"));

        YangGrouping yangGrouping = (YangGrouping) yangContainer.getChild();
        assertThat(yangGrouping.getName(), is("endpoint"));

        ListIterator<YangLeaf> leafIterator = yangGrouping.getListOfLeaf().listIterator();
        YangLeaf leafInfo = leafIterator.next();

        assertThat(leafInfo.getName(), is("address"));
    }

    /**
     * Checks grouping statement inside list.
     */
    @Test
    public void processGroupingInList() throws IOException, ParserException {

        YangNode node = manager.getDataModel("src/test/resources/GroupingInList.yang");

        // Check whether the data model tree returned is of type module.
        assertThat((node instanceof YangModule), is(true));

        // Check whether the node type is set properly to module.
        assertThat(node.getNodeType(), is(YangNodeType.MODULE_NODE));

        // Check whether the module name is set correctly.
        YangModule yangNode = (YangModule) node;
        assertThat(yangNode.getName(), is("Test"));

        YangList yangList = (YangList) yangNode.getChild();
        assertThat(yangList.getName(), is("valid"));

        YangGrouping yangGrouping = (YangGrouping) yangList.getChild();
        assertThat(yangGrouping.getName(), is("endpoint"));

        ListIterator<YangLeaf> leafIterator = yangGrouping.getListOfLeaf().listIterator();
        YangLeaf leafInfo = leafIterator.next();

        assertThat(leafInfo.getName(), is("address"));
    }

    /**
     * Checks grouping with attributes.
     */
    @Test
    public void processGroupingAttributes() throws IOException, ParserException {

        YangNode node = manager.getDataModel("src/test/resources/GroupingAttributes.yang");

        // Check whether the data model tree returned is of type module.
        assertThat((node instanceof YangModule), is(true));

        // Check whether the node type is set properly to module.
        assertThat(node.getNodeType(), is(YangNodeType.MODULE_NODE));

        // Check whether the module name is set correctly.
        YangModule yangNode = (YangModule) node;
        assertThat(yangNode.getName(), is("Test"));

        YangList yangList = (YangList) yangNode.getChild();
        assertThat(yangList.getName(), is("valid"));

        YangGrouping yangGrouping = (YangGrouping) yangList.getChild();
        assertThat(yangGrouping.getName(), is("endpoint"));
        assertThat(yangGrouping.getStatus(), is(YangStatusType.CURRENT));
        assertThat(yangGrouping.getReference(), is("\"RFC 6020\""));
        assertThat(yangGrouping.getDescription(), is("\"grouping under test\""));

        ListIterator<YangLeaf> leafIterator = yangGrouping.getListOfLeaf().listIterator();
        YangLeaf leafInfo = leafIterator.next();

        assertThat(leafInfo.getName(), is("address"));
    }

    /**
     * Checks duplicate grouping in list.
     */
    @Test(expected = ParserException.class)
    public void processDuplicateGroupingInList() throws IOException, ParserException {

        YangNode node = manager.getDataModel("src/test/resources/DuplicateGroupingInList.yang");
    }

    /**
     * Checks duplicate grouping in container.
     */
    @Test (expected = ParserException.class)
    public void processDuplicateGroupingInContainer() throws IOException, ParserException {

        YangNode node = manager.getDataModel("src/test/resources/DuplicateGroupingInContainer.yang");
    }

    /**
     * Checks duplicate grouping in module.
     */
    @Test (expected = ParserException.class)
    public void processDuplicateGroupingInModule() throws IOException, ParserException {

        YangNode node = manager.getDataModel("src/test/resources/DuplicateGroupingInModule.yang");
    }
}

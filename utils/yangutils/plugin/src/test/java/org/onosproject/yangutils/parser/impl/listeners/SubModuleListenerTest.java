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

import org.junit.Test;
import org.onosproject.yangutils.datamodel.YangNode;
import org.onosproject.yangutils.datamodel.YangNodeType;
import org.onosproject.yangutils.datamodel.YangSubModule;
import org.onosproject.yangutils.parser.exceptions.ParserException;
import org.onosproject.yangutils.parser.impl.YangUtilsParserManager;

import java.io.IOException;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;

/**
 * Test cases for testing submodule listener functionality.
 */
public class SubModuleListenerTest {

    private final YangUtilsParserManager manager = new YangUtilsParserManager();

    /**
     * Checks if the sub module listeners updates the data model tree.
     */
    @Test
    public void processSubModuleValidEntry() throws IOException, ParserException {

        YangNode node = manager.getDataModel("src/test/resources/SubModuleValidEntry.yang");

        // Check whether the data model tree returned is of type module.
        assertThat((node instanceof YangSubModule), is(true));

        // Check whether the node type is set properly to module.
        assertThat(node.getNodeType(), is(YangNodeType.SUB_MODULE_NODE));

        YangSubModule yangNode = (YangSubModule) node;
        // Check whether the module name is set correctly.
        assertThat(yangNode.getName(), is("Test"));
        // Checks for the version value in data model tree.
        assertThat(yangNode.getVersion(), is((byte) 1));
        // Checks identifier of belongsto in data model tree.
        assertThat(yangNode.getBelongsTo().getBelongsToModuleName(), is("ONOS"));
        // Checks for the version value in data model tree.
        assertThat(yangNode.getBelongsTo().getPrefix(), is("On1"));
    }

    /**
     * Checks if the yang version and belongs to can come in any order in sub
     * module.
     */
    @Test
    public void processSubModuleOrder() throws IOException, ParserException {

        YangNode node = manager.getDataModel("src/test/resources/SubModuleOrder.yang");

        // Check whether the data model tree returned is of type module.
        assertThat((node instanceof YangSubModule), is(true));

        // Check whether the node type is set properly to module.
        assertThat(node.getNodeType(), is(YangNodeType.SUB_MODULE_NODE));

        YangSubModule yangNode = (YangSubModule) node;
        // Check whether the module name is set correctly.
        assertThat(yangNode.getName(), is("Test"));
        // Checks for the version value in data model tree.
        assertThat(yangNode.getVersion(), is((byte) 1));
        // Checks identifier of belongsto in data model tree.
        assertThat(yangNode.getBelongsTo().getBelongsToModuleName(), is("ONOS"));
        // Checks for the version value in data model tree.
        assertThat(yangNode.getBelongsTo().getPrefix(), is("On1"));
    }

    /**
     * Checks if yang version is optional.
     */
    @Test
    public void processSubModuleWithoutVersion() throws IOException, ParserException {

        YangNode node = manager.getDataModel("src/test/resources/SubModuleWithoutVersion.yang");

        // Check whether the data model tree returned is of type module.
        assertThat((node instanceof YangSubModule), is(true));

        // Check whether the node type is set properly to module.
        assertThat(node.getNodeType(), is(YangNodeType.SUB_MODULE_NODE));

        YangSubModule yangNode = (YangSubModule) node;
        // Check whether the module name is set correctly.
        assertThat(yangNode.getName(), is("Test"));
        // Checks identifier of belongsto in data model tree.
        assertThat(yangNode.getBelongsTo().getBelongsToModuleName(), is("ONOS"));
        // Checks for the version value in data model tree.
        assertThat(yangNode.getBelongsTo().getPrefix(), is("On1"));
        //Checks the revision with current date is created for empty revision statement.
        assertThat(((YangSubModule) node).getRevision().getRevDate(), notNullValue());
    }

    /**
     * Checks if sub module name is correct.
     */
    @Test(expected = ParserException.class)
    public void processSubModuleInvalidName() throws IOException, ParserException {

        YangNode node = manager.getDataModel("src/test/resources/SubModuleInvalidName.yang");
    }

    /**
     * Checks if sub module has invalid modules construct eg namespace.
     */
    @Test(expected = ParserException.class)
    public void processSubModuleWithNamespace() throws IOException, ParserException {

        YangNode node = manager.getDataModel("src/test/resources/SubModuleWithNamespace.yang");
    }
}
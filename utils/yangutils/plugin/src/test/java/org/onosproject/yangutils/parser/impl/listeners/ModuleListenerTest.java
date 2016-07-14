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

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.onosproject.yangutils.datamodel.YangModule;
import org.onosproject.yangutils.datamodel.YangNode;
import org.onosproject.yangutils.datamodel.YangNodeType;
import org.onosproject.yangutils.parser.exceptions.ParserException;
import org.onosproject.yangutils.parser.impl.YangUtilsParserManager;

import java.io.IOException;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

/**
 * Test cases for testing module listener functionality.
 */
public class ModuleListenerTest {

    private final YangUtilsParserManager manager = new YangUtilsParserManager();

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    /**
     * Checks if module listener updates the data model root node.
     */
    @Test
    public void processModuleValidEntry() throws IOException, ParserException {

        YangNode node = manager.getDataModel("src/test/resources/ModuleValidEntry.yang");

        // Check whether the data model tree returned is of type module.
        assertThat((node instanceof YangModule), is(true));

        // Check whether the node type is set properly to module.
        assertThat(node.getNodeType(), is(YangNodeType.MODULE_NODE));

        // Check whether the module name is set correctly.
        YangModule yangNode = (YangModule) node;
        assertThat(yangNode.getName(), is("Test"));
    }

    /**
     * Checks if module name is set correctly.
     */
    @Test(expected = ParserException.class)
    public void processModuleInvalidEntryTest() throws IOException, ParserException {

        YangNode node = manager.getDataModel("src/test/resources/ModuleWithInvalidIdentifier.yang");
    }

    /**
     * Checks whether exception is thrown when module length is greater than 64 characters.
     */
    @Test
    public void processModuleInvalidIdentifierLength() throws IOException, ParserException {
        thrown.expect(ParserException.class);
        thrown.expectMessage("YANG file error : module name Testttttttttttttttttttttttttttttttttttttttttttttttttttt" +
                "tttttttttt is greater than 64 characters.");
        YangNode node = manager.getDataModel("src/test/resources/ModuleInvalidIdentifierLength.yang");
    }
}
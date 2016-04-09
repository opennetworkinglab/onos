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
import org.onosproject.yangutils.datamodel.YangModule;
import org.onosproject.yangutils.datamodel.YangNode;
import org.onosproject.yangutils.parser.exceptions.ParserException;
import org.onosproject.yangutils.parser.impl.YangUtilsParserManager;

import java.io.IOException;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

/**
 * Test cases for testing namespace listener functionality.
 */
public class NamespaceListenerTest {

    private final YangUtilsParserManager manager = new YangUtilsParserManager();

    /**
     * Checks that value of namespace shouldn't have invalid spaces.
     */
    @Test(expected = ParserException.class)
    public void processNamespaceWithInvalidSpaces() throws IOException, ParserException {

        YangNode node = manager.getDataModel("src/test/resources/NamespaceWithInvalidSpaces.yang");
    }

    /**
     * Checks if namespace with double quotes is allowed.
     */
    @Test()
    public void processNamespaceInDoubleQuotes() throws IOException, ParserException {

        YangNode node = manager.getDataModel("src/test/resources/NamespaceInDoubleQuotes.yang");

        // Checks for the version value in data model tree.
        assertThat(((YangModule) node).getNameSpace().getUri(), is("\"urn:ietf:params:xml:ns:yang:ietf-ospf\""));
    }

    /**
     * Checks if namespace without double quotes is allowed.
     */
    @Test()
    public void processNamespaceWithoutQuotes() throws IOException, ParserException {

        YangNode node = manager.getDataModel("src/test/resources/NamespaceWithoutQuotes.yang");

        // Checks for the version value in data model tree.
        assertThat(((YangModule) node).getNameSpace().getUri(), is("urn:ietf:params:xml:ns:yang:ietf-ospf"));
    }

    /**
     * Checks if namespace is present only once.
     */
    @Test(expected = ParserException.class)
    public void processNamespaceDualEntry() throws IOException, ParserException {

        YangNode node = manager.getDataModel("src/test/resources/NamespaceDualEntry.yang");
    }

    /**
     * Checks if mandatory parameter namespace is present.
     */
    @Test(expected = ParserException.class)
    public void processNamespaceNoEntryTest() throws IOException, ParserException {

        YangNode node = manager.getDataModel("src/test/resources/NamespaceNoEntryTest.yang");
    }
}
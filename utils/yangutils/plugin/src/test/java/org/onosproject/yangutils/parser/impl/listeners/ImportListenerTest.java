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
 * Test cases for testing import listener functionality.
 */
public class ImportListenerTest {

    private final YangUtilsParserManager manager = new YangUtilsParserManager();

    /**
     * Checks if mandatory parameter prefix is present in import.
     */
    @Test(expected = ParserException.class)
    public void processImportWithoutPrefix() throws IOException, ParserException {

        YangNode node = manager.getDataModel("src/test/resources/ImportWithoutPrefix.yang");
    }

    /**
     * Checks that prefix must be present only once in import.
     */
    @Test(expected = ParserException.class)
    public void processImportWithDualPrefix() throws IOException, ParserException {

        YangNode node = manager.getDataModel("src/test/resources/ImportWithDualPrefix.yang");
    }

    /**
     * Checks for the correct order of prefix in import.
     */
    @Test(expected = ParserException.class)
    public void processImportInvalidOrder() throws IOException, ParserException {

        YangNode node = manager.getDataModel("src/test/resources/ImportInvalidOrder.yang");
    }

    /**
     * Checks if import listener updates the data model tree.
     */
    @Test
    public void processImportValidEntry() throws IOException, ParserException {

        YangNode node = manager.getDataModel("src/test/resources/ImportValidEntry.yang");

        // Checks for the revision value in data model tree.
        assertThat(((YangModule) node).getImportList().get(0).getRevision(), is("2015-02-03"));
        // Checks for the prefix id in data model tree.
        assertThat(((YangModule) node).getImportList().get(0).getPrefixId(), is("On2"));
        // Checks for the module name in data model tree.
        assertThat(((YangModule) node).getImportList().get(0).getModuleName(), is("ietf"));
    }

    /**
     * Checks if optional parameter revision is not mandatory in import.
     */
    @Test
    public void processImportWithoutRevision() throws IOException, ParserException {

        YangNode node = manager.getDataModel("src/test/resources/ImportWithoutRevision.yang");

        // Checks for the prefix id in data model tree.
        assertThat(((YangModule) node).getImportList().get(0).getPrefixId(), is("On2"));
        // Checks for the module name in data model tree.
        assertThat(((YangModule) node).getImportList().get(0).getModuleName(), is("ietf"));
    }

    /**
     * Checks if multiple imports are allowed.
     */
    @Test()
    public void processImportMultipleInstance() throws IOException, ParserException {

        YangNode node = manager.getDataModel("src/test/resources/ImportMultipleInstance.yang");

        // Checks for the prefix id in data model tree.
        assertThat(((YangModule) node).getImportList().get(0).getPrefixId(), is("On2"));
        // Checks for the module name in data model tree.
        assertThat(((YangModule) node).getImportList().get(0).getModuleName(), is("ietf"));

        // Checks for the prefix id in data model tree.
        assertThat(((YangModule) node).getImportList().get(1).getPrefixId(), is("On3"));
        // Checks for the module name in data model tree.
        assertThat(((YangModule) node).getImportList().get(1).getModuleName(), is("itut"));
    }
}
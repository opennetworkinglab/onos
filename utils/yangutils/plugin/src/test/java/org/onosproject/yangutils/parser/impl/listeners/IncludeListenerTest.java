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
 * Test cases for testing include listener functionality.
 */
public class IncludeListenerTest {

    private final YangUtilsParserManager manager = new YangUtilsParserManager();

    /**
     * Checks if include listener with ; is valid and updates the data
     * model tree.
     */
    @Test
    public void processIncludeWithStmtend() throws IOException, ParserException {

        YangNode node = manager.getDataModel("src/test/resources/IncludeWithStmtend.yang");

        // Checks for the sub module name in data model tree.
        assertThat(((YangModule) node).getIncludeList().get(0).getSubModuleName(), is("itut"));
    }

    /**
     * Checks if include listener with braces and without revision date is valid
     * and updates the data model tree.
     */
    @Test
    public void processIncludeWithEmptyBody() throws IOException, ParserException {

        YangNode node = manager.getDataModel("src/test/resources/IncludeWithEmptyBody.yang");

        // Checks for the sub module name in data model tree.
        assertThat(((YangModule) node).getIncludeList().get(0).getSubModuleName(), is("itut"));
    }

    /**
     * Checks if include listener with braces and with revision date is valid
     * and updates the data model tree.
     */
    @Test
    public void processIncludeWithDate() throws IOException, ParserException {

        YangNode node = manager.getDataModel("src/test/resources/IncludeWithDate.yang");

        // Checks for the sub module name in data model tree.
        assertThat(((YangModule) node).getIncludeList().get(0).getSubModuleName(), is("itut"));
        assertThat(((YangModule) node).getIncludeList().get(0).getRevision(), is("2016-02-03"));
    }

    /**
     * Checks if include has more than one occurrence.
     */
    @Test
    public void processIncludeMultiInstance() throws IOException, ParserException {

        YangNode node = manager.getDataModel("src/test/resources/IncludeMultiInstance.yang");

        // Checks for the sub module name in data model tree.
        assertThat(((YangModule) node).getIncludeList().get(0).getSubModuleName(), is("itut"));
        assertThat(((YangModule) node).getIncludeList().get(0).getRevision(), is("2016-02-03"));
        assertThat(((YangModule) node).getIncludeList().get(1).getSubModuleName(), is("sdn"));
        assertThat(((YangModule) node).getIncludeList().get(1).getRevision(), is("2014-02-03"));
    }

    /**
     * Checks if include and import can come in any order.
     */
    @Test
    public void processIncludeImportAnyOrder() throws IOException, ParserException {

        YangNode node = manager.getDataModel("src/test/resources/IncludeImportAnyOrder.yang");

        // Checks for the sub module name in data model tree.
        assertThat(((YangModule) node).getIncludeList().get(0).getSubModuleName(), is("itut"));
        assertThat(((YangModule) node).getIncludeList().get(0).getRevision(), is("2016-02-03"));
        assertThat(((YangModule) node).getIncludeList().get(1).getSubModuleName(), is("sdn"));
        assertThat(((YangModule) node).getIncludeList().get(1).getRevision(), is("2014-02-03"));
    }

    /**
     * Checks if syntax of Include is not correct.
     */
    @Test(expected = ParserException.class)
    public void processIncludeInvalidSyntax() throws IOException, ParserException {

        YangNode node = manager.getDataModel("src/test/resources/IncludeInvalidSyntax.yang");
    }

    /**
     * Checks if syntax of revision date in Include is not correct.
     */
    @Test(expected = ParserException.class)
    public void processIncludeInvalidDateSyntax() throws IOException, ParserException {

        YangNode node = manager.getDataModel("src/test/resources/IncludeInvalidDateSyntax.yang");
    }
}
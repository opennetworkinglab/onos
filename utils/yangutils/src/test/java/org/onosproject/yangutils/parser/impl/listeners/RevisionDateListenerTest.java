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
 * Test cases for testing revision date listener functionality.
 */
public class RevisionDateListenerTest {

    private final YangUtilsParserManager manager = new YangUtilsParserManager();

    /**
     * Checks if revision date syntax is correct in include.
     */
    @Test(expected = ParserException.class)
    public void processRevisionDateInvalidSyntaxAtInclude() throws IOException, ParserException {

        YangNode node = manager.getDataModel("src/test/resources/RevisionDateInvalidSyntaxAtInclude.yang");
    }

    /**
     * Checks if revision date syntax is correct in import.
     */
    @Test(expected = ParserException.class)
    public void processRevisionDateInvalidSyntaxAtImport() throws IOException, ParserException {

        YangNode node = manager.getDataModel("src/test/resources/RevisionDateInvalidSyntaxAtImport.yang");
    }

    /**
     * Checks revision date in quotes inside include.
     */
    @Test
    public void processRevisionDateInQuotesAtInclude() throws IOException, ParserException {

        YangNode node = manager.getDataModel("src/test/resources/RevisionDateInQuotesAtInclude.yang");
        // Checks for the version value in data model tree.
        assertThat(((YangModule) node).getImportList().get(0).getRevision(), is("2015-02-03"));
        assertThat(((YangModule) node).getIncludeList().get(0).getRevision(), is("2016-02-03"));
        assertThat(((YangModule) node).getIncludeList().get(1).getRevision(), is("2014-02-03"));
    }

    /**
     * Checks revision date in quotes inside import.
     */
    @Test
    public void processRevisionDateInQuotesAtImport() throws IOException, ParserException {

        YangNode node = manager.getDataModel("src/test/resources/RevisionDateInQuotesAtImport.yang");
        // Checks for the version value in data model tree.
        assertThat(((YangModule) node).getImportList().get(0).getRevision(), is("2015-02-03"));
        assertThat(((YangModule) node).getIncludeList().get(0).getRevision(), is("2016-02-03"));
        assertThat(((YangModule) node).getIncludeList().get(1).getRevision(), is("2014-02-03"));
    }

    /**
     * Checks if revision date follows YYYY-MM-DD format.
     */
    @Test(expected = ParserException.class)
    public void processRevisionDateInvalidFormat() throws IOException, ParserException {

        YangNode node = manager.getDataModel("src/test/resources/RevisionDateInvalidFormat.yang");
    }

    /**
     * Checks if revision date is correct.
     */
    @Test(expected = ParserException.class)
    public void processRevisionDateInvalid() throws IOException, ParserException {

        YangNode node = manager.getDataModel("src/test/resources/RevisionDateInvalid.yang");
    }

    /**
     * Checks if revision date listener updates the data model tree.
     */
    @Test
    public void processRevisionDateValidEntry() throws IOException, ParserException {

        YangNode node = manager.getDataModel("src/test/resources/RevisionDateValidEntry.yang");

        // Checks for the version value in data model tree.
        assertThat(((YangModule) node).getImportList().get(0).getRevision(), is("2015-02-03"));
        assertThat(((YangModule) node).getIncludeList().get(0).getRevision(), is("2016-02-03"));
        assertThat(((YangModule) node).getIncludeList().get(1).getRevision(), is("2014-02-03"));
    }
}
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

import org.junit.Test;
import org.onosproject.yangutils.datamodel.YangModule;
import org.onosproject.yangutils.datamodel.YangNode;
import org.onosproject.yangutils.parser.exceptions.ParserException;
import org.onosproject.yangutils.parser.impl.YangUtilsParserManager;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

/**
 * Test cases for testing version listener functionality.
 */
public class VersionListenerTest {

    private final YangUtilsParserManager manager = new YangUtilsParserManager();

    /**
     * Checks if value of version is correct.
     */
    @Test(expected = ParserException.class)
    public void processVersionInvalidValue() throws IOException, ParserException {

        YangNode node = manager.getDataModel("src/test/resources/VersionInvalidValue.yang");
    }

    /**
     * Checks if version listener updates the data model tree.
     */
    @Test
    public void processVersionValidEntry() throws IOException, ParserException {

        YangNode node = manager.getDataModel("src/test/resources/VersionValidEntry.yang");

        // Checks for the version value in data model tree.
        assertThat(((YangModule) node).getVersion(), is((byte) 1));
    }

    /**
     * Checks version in double quotes.
     */
    @Test
    public void processValidVersionWithDoubleQuotes() throws IOException, ParserException {

        YangNode node = manager.getDataModel("src/test/resources/ValidVersionWithDoubleQuotes.yang");

        // Checks for the version value in data model tree.
        assertThat(((YangModule) node).getVersion(), is((byte) 1));
    }

    /**
     * Checks if version which is optional paramater is not present.
     */
    @Test
    public void processVersionNotPresent() throws IOException, ParserException {

        YangNode node = manager.getDataModel("src/test/resources/VersionNotPresent.yang");

        // Checks for the version value in data model tree.
        assertThat(((YangModule) node).getVersion(), is((byte) 1));
    }

    /**
     * Checks that version should be present only once.
     */
    @Test(expected = ParserException.class)
    public void processVersionDualEntry() throws IOException, ParserException {

        YangNode node = manager.getDataModel("src/test/resources/VersionDualEntry.yang");
    }

    /**
     * Checks if version can appear in any order in module header.
     */
    @Test
    public void processVersionOrder() throws IOException, ParserException {

        YangNode node = manager.getDataModel("src/test/resources/VersionOrder.yang");

        // Checks for the version value in data model tree.
        assertThat(((YangModule) node).getVersion(), is((byte) 1));
    }

    /**
     * Checks if sytax of version entry is not correct.
     */
    @Test(expected = ParserException.class)
    public void processVersionInvalidSyntax() throws IOException, ParserException {

        YangNode node = manager.getDataModel("src/test/resources/VersionInvalidSyntax.yang");
    }
}
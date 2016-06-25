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
 * Test cases for testing prefix listener functionality.
 */
public class PrefixListenerTest {

    private final YangUtilsParserManager manager = new YangUtilsParserManager();

    /**
     * Checks if value of prefix is correct.
     */
    @Test(expected = ParserException.class)
    public void processPrefixInvalidValue() throws IOException, ParserException {

        YangNode node = manager.getDataModel("src/test/resources/PrefixInvalidValue.yang");
    }

    /**
     * Checks if prefix listener updates the data model tree.
     */
    @Test
    public void processPrefixValidEntry() throws IOException, ParserException {

        YangNode node = manager.getDataModel("src/test/resources/PrefixValidEntry.yang");

        // Checks for the version value in data model tree.
        assertThat(((YangModule) node).getPrefix(), is("On"));
    }

    /**
     * Checks prefix value with double quotes.
     */
    @Test
    public void processPrefixWithDoubleQuotes() throws IOException, ParserException {

        YangNode node = manager.getDataModel("src/test/resources/PrefixWithDoubleQuotes.yang");
        assertThat(((YangModule) node).getPrefix(), is("On"));
    }

    /**
     * Checks that prefix should be present just once.
     */
    @Test(expected = ParserException.class)
    public void processPrefixDualEntry() throws IOException, ParserException {

        YangNode node = manager.getDataModel("src/test/resources/PrefixDualEntry.yang");
    }

    /**
     * Checks if prefix syntax is followed.
     */
    @Test(expected = ParserException.class)
    public void processPrefixMissingValue() throws IOException, ParserException {

        YangNode node = manager.getDataModel("src/test/resources/PrefixMissingValue.yang");
    }

    /**
     * Checks that exception should be reported if prefix is missing.
     */
    @Test(expected = ParserException.class)
    public void processPrefixOrder() throws IOException, ParserException {

        YangNode node = manager.getDataModel("src/test/resources/PrefixOrder.yang");
    }
}
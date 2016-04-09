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
import org.onosproject.yangutils.parser.exceptions.ParserException;
import org.onosproject.yangutils.parser.impl.YangUtilsParserManager;

import java.io.IOException;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

/**
 * Test cases for testing contact listener functionality.
 */
public class ContactListenerTest {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    private final YangUtilsParserManager manager = new YangUtilsParserManager();

    /**
     * Checks if contact listener updates the data model tree.
     */
    @Test
    public void processContactValidEntry() throws IOException, ParserException {

        YangNode node = manager.getDataModel("src/test/resources/ContactValidEntry.yang");

        // Checks for the contact value in data model tree.
        assertThat(((YangModule) node).getContact(), is("\"WG List:  <mailto:spring@ietf.org>\nEditor:    "
                + "Stephane Litkowski\n           " + "<mailto:stephane.litkowski@orange.com>\""));
    }

    /**
     * Checks that contact must be present only once.
     */
    @Test(expected = ParserException.class)
    public void processContactDualEntryTest() throws IOException, ParserException {

        YangNode node = manager.getDataModel("src/test/resources/ContactDualEntryTest.yang");

    }

    /**
     * Checks that contact can have a string value without double quotes.
     */
    @Test
    public void processContactWithoutQuotes() throws IOException, ParserException {

        YangNode node = manager.getDataModel("src/test/resources/ContactWithoutQuotes.yang");

        // Checks for the contact value in data model tree.
        assertThat(((YangModule) node).getContact(), is("WG"));
    }

    /**
     * Checks if contact is not empty.
     */
    @Test(expected = ParserException.class)
    public void processContactWithEmptyString() throws IOException, ParserException {

        YangNode node = manager.getDataModel("src/test/resources/ContactWithEmptyString.yang");
    }

    /**
     * Checks that contact must be present after namespace.
     */
    @Test(expected = ParserException.class)
    public void processContactIncorrectOrder() throws IOException, ParserException {

        YangNode node = manager.getDataModel("src/test/resources/ContactIncorrectOrder.yang");
    }
}
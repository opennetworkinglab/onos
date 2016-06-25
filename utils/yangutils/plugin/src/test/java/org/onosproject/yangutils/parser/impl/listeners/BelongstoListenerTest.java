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
import org.onosproject.yangutils.datamodel.YangNode;
import org.onosproject.yangutils.datamodel.YangSubModule;
import org.onosproject.yangutils.parser.exceptions.ParserException;
import org.onosproject.yangutils.parser.impl.CustomExceptionMatcher;
import org.onosproject.yangutils.parser.impl.YangUtilsParserManager;

import java.io.IOException;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

/**
 * Test cases for testing belongsto listener functionality.
 */
public class BelongstoListenerTest {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    private final YangUtilsParserManager manager = new YangUtilsParserManager();

    /**
     * Checks if mandatory belongsto parameter "prefix" is not present.
     */
    @Test
    public void processBelongsToWithoutPrefix() throws IOException, ParserException {
        thrown.expect(ParserException.class);
        thrown.expectMessage("mismatched input '}' expecting 'prefix'");
        thrown.expect(CustomExceptionMatcher.errorLocation(4, 0));
        YangNode node = manager.getDataModel("src/test/resources/BelongsToWithoutPrefix.yang");
    }

    /**
     * Checks that prefix must be present only once in belongsto.
     */
    @Test
    public void processBelongsToDualPrefix() throws IOException, ParserException {
        thrown.expect(ParserException.class);
        thrown.expectMessage("mismatched input 'prefix' expecting '}'");
        thrown.expect(CustomExceptionMatcher.errorLocation(5, 0));
        YangNode node = manager.getDataModel("src/test/resources/BelongsToDualPrefix.yang");
    }

    /**
     * Checks if belongsto listener updates the date model tree.
     */
    @Test
    public void processBelongsToWithPrefix() throws IOException, ParserException {

        YangNode node = manager.getDataModel("src/test/resources/BelongsToWithPrefix.yang");
        YangSubModule yangNode = (YangSubModule) node;
        assertThat(yangNode.getBelongsTo().getBelongsToModuleName(), is("ONOS"));
    }

    /**
     * Checks if mandatory parameter "belongsto" is present.
     */
    @Test
    public void processSubModuleWithoutBelongsTo() throws IOException, ParserException {
        thrown.expect(ParserException.class);
        thrown.expectMessage("mismatched input '}' expecting 'belongs-to'");
        thrown.expect(CustomExceptionMatcher.errorLocation(3, 0));
        YangNode node = manager.getDataModel("src/test/resources/SubModuleWithoutBelongsTo.yang");
    }
}
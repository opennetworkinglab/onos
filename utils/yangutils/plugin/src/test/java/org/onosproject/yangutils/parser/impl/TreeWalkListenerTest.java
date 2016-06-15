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

package org.onosproject.yangutils.parser.impl;

import java.io.IOException;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.onosproject.yangutils.parser.exceptions.ParserException;

/**
 * Test cases for testing tree walk listener functionality.
 */
public class TreeWalkListenerTest {

    private final YangUtilsParserManager manager = new YangUtilsParserManager();

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    /**
     * Checks whether exception is thrown for ordered statement.
     */
    @Test
    public void processOrderedByStatement() throws IOException, ParserException {
        thrown.expect(ParserException.class);
        thrown.expectMessage("YANG file error : \"ordered-by\" is not supported in current version, please check wiki" +
                " for YANG utils road map.");
        manager.getDataModel("src/test/resources/OrderedByStatement.yang");
    }

    /**
     * Checks whether exception is thrown for anyxml statement.
     */
    @Test
    public void processAnyXmlStatement() throws IOException, ParserException {
        thrown.expect(ParserException.class);
        thrown.expectMessage("YANG file error : \"anyxml\" is not supported.");
        manager.getDataModel("src/test/resources/AnyxmlStatement.yang");
    }

    /**
     * Checks whether exception is thrown when extra brace is added in the EOF.
     */
    @Test
    public void processFileWithExtraBrace() throws IOException, ParserException {
        thrown.expect(ParserException.class);
        thrown.expectMessage("mismatched input '}' expecting <EOF>");
        manager.getDataModel("src/test/resources/ProcessFileWithExtraBrace.yang");
    }

    /**
     * Checks whether exception is thrown when leaf is given after module ends.
     */
    @Test
    public void processFileWithExtraLeaf() throws IOException, ParserException {
        thrown.expect(ParserException.class);
        thrown.expectMessage("mismatched input 'leaf' expecting <EOF>");
        manager.getDataModel("src/test/resources/ProcessFileWithExtraLeaf.yang");
    }

    /**
     * Checks whether exception is thrown when extra brace is added in between the EOF.
     */
    @Test
    public void processFileWithExtraBraceInBetween() throws IOException, ParserException {
        thrown.expect(ParserException.class);
        thrown.expectMessage("mismatched input 'container' expecting <EOF>");
        manager.getDataModel("src/test/resources/ProcessFileWithExtraBraceInBetween.yang");
    }
}

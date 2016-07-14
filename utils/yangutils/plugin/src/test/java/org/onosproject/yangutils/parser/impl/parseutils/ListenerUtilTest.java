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

package org.onosproject.yangutils.parser.impl.parseutils;

import java.io.IOException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.onosproject.yangutils.parser.exceptions.ParserException;
import org.onosproject.yangutils.parser.impl.YangUtilsParserManager;

/**
 * Test case for testing listener util.
 */
public class ListenerUtilTest {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    private final YangUtilsParserManager manager = new YangUtilsParserManager();

    /**
     * Checks whether exception is thrown when identifier starts with xml.
     */
    @Test
    public void validateIdentifierStartsWithXml() throws IOException {
        thrown.expect(ParserException.class);
        thrown.expectMessage("YANG file error : module identifier xMlTest must not start" +
                " with (('X'|'x') ('M'|'m') ('L'|'l'))");
        manager.getDataModel("src/test/resources/InValidIdentifierXML.yang");
    }
}

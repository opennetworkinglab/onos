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

import org.antlr.v4.runtime.ANTLRFileStream;
import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.onosproject.yangutils.parser.antlrgencode.GeneratedYangLexer;
import org.onosproject.yangutils.parser.antlrgencode.GeneratedYangParser;
import org.onosproject.yangutils.parser.exceptions.ParserException;
import org.onosproject.yangutils.parser.impl.CustomExceptionMatcher;
import org.onosproject.yangutils.parser.impl.YangUtilsParserManager;
import org.onosproject.yangutils.parser.impl.parserutils.ParseTreeErrorListener;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;

/**
 * Test case for testing parse tree error listener.
 */
public class ParseTreeErrorListenerTest {

    YangUtilsParserManager manager = new YangUtilsParserManager();
    File file;
    BufferedWriter out;

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    /**
     * Checks that no exception is generated for YANG file with valid syntax.
     */
    @Test
    public void checkValidYangFileForNoSyntaxError() throws IOException {

        ANTLRInputStream input = new ANTLRFileStream("src/test/resources/YangFileWithoutSyntaxError.yang");

        // Create a lexer that feeds off of input char stream.
        GeneratedYangLexer lexer = new GeneratedYangLexer(input);
        // Create a buffer of tokens pulled from the lexer.
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        // Create a parser that feeds off the tokens buffer.
        GeneratedYangParser parser = new GeneratedYangParser(tokens);
        // Remove console error listener.
        parser.removeErrorListeners();
        // Create instance of customized error listener.
        ParseTreeErrorListener parseTreeErrorListener = new ParseTreeErrorListener();
        // Add customized error listener to catch errors during parsing.
        parser.addErrorListener(parseTreeErrorListener);
        // Begin parsing YANG file and generate parse tree.
        ParseTree tree = parser.yangfile();
    }

    /**
     * Checks that exception is generated for YANG file with invalid syntax.
     */
    @Test
    public void checkInvalidYangFileForSyntaxError() throws IOException {

        // Get the exception occurred during parsing.
        thrown.expect(ParserException.class);
        thrown.expect(CustomExceptionMatcher.errorLocation(3, 0));
        thrown.expectMessage("no viable alternative at input 'yang-version 1\\nnamespace'");

        ANTLRInputStream input = new ANTLRFileStream("src/test/resources/YangFileWithSyntaxError.yang");

        // Create a lexer that feeds off of input char stream.
        GeneratedYangLexer lexer = new GeneratedYangLexer(input);
        // Create a buffer of tokens pulled from the lexer.
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        // Create a parser that feeds off the tokens buffer.
        GeneratedYangParser parser = new GeneratedYangParser(tokens);
        // Remove console error listener.
        parser.removeErrorListeners();
        // Create instance of customized error listener.
        ParseTreeErrorListener parseTreeErrorListener = new ParseTreeErrorListener();
        // Add customized error listener to catch errors during parsing.
        parser.addErrorListener(parseTreeErrorListener);
        // Begin parsing YANG file and generate parse tree.
        ParseTree tree = parser.yangfile();
    }
}
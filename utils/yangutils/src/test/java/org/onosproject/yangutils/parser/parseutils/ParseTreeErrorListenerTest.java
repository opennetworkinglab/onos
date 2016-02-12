/*
 * Copyright 2016 Open Networking Laboratory
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
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onosproject.yangutils.parser.antlrgencode.GeneratedYangLexer;
import org.onosproject.yangutils.parser.antlrgencode.GeneratedYangParser;
import org.onosproject.yangutils.parser.exceptions.ParserException;
import org.onosproject.yangutils.parser.impl.YangUtilsParserManager;
import org.onosproject.yangutils.parser.impl.parserutils.ParseTreeErrorListener;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

/**
 * Test case for testing parse tree error listener.
 */
public class ParseTreeErrorListenerTest {

    YangUtilsParserManager manager = new YangUtilsParserManager();
    File file;
    BufferedWriter out;

    @Before
    public void setUp() throws Exception {
        file = new File("demo.yang");
        out = new BufferedWriter(new FileWriter(file));
    }
    @After
    public void tearDown() throws Exception {
        file.delete();
    }

    /**
     * This test case checks whether the error received from parser is correctly
     * handled.
     */
    @Test
    public void syntaxErrorValidationTest() throws IOException {

        out.write("module ONOS {\n");
        out.write("yang-version 1\n");
        out.write("namespace urn:ietf:params:xml:ns:yang:ietf-ospf;\n");
        out.write("prefix On;\n");
        out.write("}\n");
        out.close();

        ANTLRInputStream input = new ANTLRFileStream("demo.yang");

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
        // Get the exception occurred during parsing.
        ParserException parserException = parseTreeErrorListener.getParserException();

        /**
         * Check for the values set in syntax error function. If not set properly
         * report an assert.
         */
        assertThat(parseTreeErrorListener.isExceptionFlag(), is(true));
        assertThat(parserException.getLineNumber(), is(3));
        assertThat(parserException.getCharPositionInLine(), is(0));
    }
}
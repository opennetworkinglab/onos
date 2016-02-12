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

package org.onosproject.yangutils.parser.impl;

import org.antlr.v4.runtime.ANTLRFileStream;
import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.onosproject.yangutils.datamodel.YangNode;
import org.onosproject.yangutils.parser.YangUtilsParser;
import org.onosproject.yangutils.parser.antlrgencode.GeneratedYangLexer;
import org.onosproject.yangutils.parser.antlrgencode.GeneratedYangParser;
import org.onosproject.yangutils.parser.exceptions.ParserException;
import org.onosproject.yangutils.parser.impl.parserutils.ParseTreeErrorListener;

import java.io.IOException;

/**
 * Manages file parsing, parse tree creation and data model tree creation
 * corresponding to an input YANG file.
 */
public class YangUtilsParserManager implements YangUtilsParser {

    @Override
    public YangNode getDataModel(String yangFile) throws IOException, ParserException {

        /**
          * Create a char stream that reads from YANG file. Throws an exception
          * in case input YANG file is either null or non existent.
          */
        ANTLRInputStream input = null;
        try {
            input = new ANTLRFileStream(yangFile);
        } catch (IOException e) {
            e.printStackTrace();
            throw e;
        }

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

        /**
         * Throws an parser Exception if exception flag is set i.e. exception has
         * occurred during parsing.
         */
        if (parseTreeErrorListener.isExceptionFlag()) {
            // Get the exception occurred during parsing.
            ParserException parserException = parseTreeErrorListener.getParserException();
            parserException.setFileName(yangFile);
            throw parserException;
        }

        // Create a walker to walk the parse tree.
        ParseTreeWalker walker = new ParseTreeWalker();

        // Create a listener implementation class object.
        TreeWalkListener treeWalker = new TreeWalkListener();

        /**
          * Walk parse tree, provide call backs to methods in listener and
          * build data model tree.
          */
        walker.walk(treeWalker, tree);

        // Throws an parser exception which has occurred during listener walk.
        if (treeWalker.getErrorInformation().isErrorFlag()) {
            // Create object of listener exception
            ParserException listenerException = new ParserException();
            listenerException.setMsg(treeWalker.getErrorInformation().getErrorMsg());
            listenerException.setFileName(yangFile);
            throw listenerException;
        }

        // Returns the Root Node of the constructed data model tree.
        return treeWalker.getRootNode();
    }
}

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

package org.onosproject.yangutils.parser.impl.parserutils;

import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.onosproject.yangutils.parser.exceptions.ParserException;

/**
 * Represent the parse tree error listener.
 * By default, ANTLR sends all errors to standard error, this is changed by
 * providing this new implementation of interface ANTLRErrorListener. The
 * interface has a syntaxError() method that applies to both lexer and parser.
 */
public class ParseTreeErrorListener extends BaseErrorListener {

    @Override
    public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol, int line, int charPositionInLine,
            String msg, RecognitionException e) {

        ParserException parserException = new ParserException(msg);
        parserException.setLine(line);
        parserException.setCharPosition(charPositionInLine);
        throw parserException;
    }
}
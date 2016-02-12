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

package org.onosproject.yangutils.parser.impl.parserutils;

import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.onosproject.yangutils.parser.exceptions.ParserException;

/**
 * By default, ANTLR sends all errors to standard error, this is changed by
 * providing this new implementation of interface ANTLRErrorListener. The
 * interface has a syntaxError() method that applies to both lexer and
 * parser.
 */
public class ParseTreeErrorListener extends BaseErrorListener {

    // Exception of type parser exceptions are catched during parsing.
    private ParserException parserException = new ParserException();

    // Flag to indicate presence of exception.
    private boolean exceptionFlag = false;

    /**
     * Returns the status of exception flag.
     *
     * @return flag which contains the status of exception.
     */
    public boolean isExceptionFlag() {
        return exceptionFlag;
    }

    /**
     * Returns the parser exception object populated with line, character
     * position and message.
     *
     * @return object of parser exception.
     */
    public ParserException getParserException() {
        return parserException;
    }

    @Override
    public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol, int line, int charPositionInLine,
                            String msg, RecognitionException e) {
        parserException.setLine(line);
        parserException.setCharPosition(charPositionInLine);
        parserException.setMsg(msg);
        exceptionFlag = true;
    }
}
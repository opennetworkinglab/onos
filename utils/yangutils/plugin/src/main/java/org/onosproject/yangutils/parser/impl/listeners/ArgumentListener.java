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

import org.onosproject.yangutils.datamodel.YangExtension;
import org.onosproject.yangutils.datamodel.utils.Parsable;
import org.onosproject.yangutils.parser.antlrgencode.GeneratedYangParser;
import org.onosproject.yangutils.parser.exceptions.ParserException;
import org.onosproject.yangutils.parser.impl.TreeWalkListener;

import static org.onosproject.yangutils.datamodel.utils.YangConstructType.ARGUMENT_DATA;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerErrorLocation.ENTRY;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerErrorMessageConstruction.constructListenerErrorMessage;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerErrorType.INVALID_HOLDER;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerErrorType.MISSING_HOLDER;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerUtil.getValidIdentifier;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerValidation.checkStackIsNotEmpty;

/*
 * Reference: RFC6020 and YANG ANTLR Grammar
 *
 * ABNF grammar as per RFC6020
 * argument-stmt       = argument-keyword sep identifier-arg-str optsep
 *                       (";" /
 *                        "{" stmtsep
 *                            [yin-element-stmt stmtsep]
 *                        "}")
 * *
 * ANTLR grammar rule
 * argumentStatement : ARGUMENT_KEYWORD identifier (STMTEND | LEFT_CURLY_BRACE argumentBody RIGHT_CURLY_BRACE);
 * argumentBody : yinElementStatement?;
 */

/**
 * Represents listener based call back function corresponding to the "argument"
 * rule defined in ANTLR grammar file for corresponding ABNF rule in RFC 6020.
 */
public final class ArgumentListener {

    /**
     * Creates a new argument listener.
     */
    private ArgumentListener() {
    }

    /**
     * It is called when parser receives an input matching the grammar rule
     * (argument), performs validation and updates the data model tree.
     *
     * @param listener listener's object
     * @param ctx      context object of the grammar rule
     */
    public static void processArgumentEntry(TreeWalkListener listener,
                                            GeneratedYangParser.ArgumentStatementContext ctx) {

        // Check for stack to be non empty.
        checkStackIsNotEmpty(listener, MISSING_HOLDER, ARGUMENT_DATA, ctx.identifier().getText(), ENTRY);

        String identifier = getValidIdentifier(ctx.identifier().getText(), ARGUMENT_DATA, ctx);

        Parsable curData = listener.getParsedDataStack().peek();
        if (curData instanceof YangExtension) {
            YangExtension extension = ((YangExtension) curData);
            extension.setArgumentName(identifier);
        } else {
            throw new ParserException(constructListenerErrorMessage(INVALID_HOLDER, ARGUMENT_DATA,
                    ctx.identifier().getText(), ENTRY));
        }
    }
}

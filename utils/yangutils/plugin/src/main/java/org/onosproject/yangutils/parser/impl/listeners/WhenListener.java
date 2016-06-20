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

import org.onosproject.yangutils.datamodel.YangWhen;
import org.onosproject.yangutils.datamodel.YangWhenHolder;
import org.onosproject.yangutils.datamodel.utils.Parsable;
import org.onosproject.yangutils.parser.antlrgencode.GeneratedYangParser;
import org.onosproject.yangutils.parser.exceptions.ParserException;
import org.onosproject.yangutils.parser.impl.TreeWalkListener;

import static org.onosproject.yangutils.datamodel.utils.YangConstructType.WHEN_DATA;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerErrorLocation.ENTRY;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerErrorLocation.EXIT;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerErrorMessageConstruction.constructListenerErrorMessage;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerErrorType.INVALID_HOLDER;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerErrorType.MISSING_CURRENT_HOLDER;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerErrorType.MISSING_HOLDER;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerUtil.removeQuotesAndHandleConcat;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerValidation.checkStackIsNotEmpty;

/*
 * Reference: RFC6020 and YANG ANTLR Grammar
 *
 * ABNF grammar as per RFC6020
 *
 *  when-stmt           = when-keyword sep string optsep
 *                        (";" /
 *                         "{" stmtsep
 *                             ;; these stmts can appear in any order
 *                             [description-stmt stmtsep]
 *                             [reference-stmt stmtsep]
 *                          "}")
 *
 * ANTLR grammar rule
 * whenStatement : WHEN_KEYWORD string (STMTEND | LEFT_CURLY_BRACE ((descriptionStatement? referenceStatement?)
 *       | (referenceStatement? descriptionStatement?)) RIGHT_CURLY_BRACE);
 */

/**
 * Represents listener based call back function corresponding to the
 * "when" rule defined in ANTLR grammar file for corresponding ABNF rule
 * in RFC 6020.
 */
public final class WhenListener {

    /**
     * Creates a new when listener.
     */
    private WhenListener() {
    }

    /**
     * Perform validations and updates the data model tree.It is called when parser
     * receives an input matching the grammar rule (when).
     *
     * @param listener listener's object
     * @param ctx      context object of the grammar rule
     */
    public static void processWhenEntry(TreeWalkListener listener,
                                        GeneratedYangParser.WhenStatementContext ctx) {

        // Check for stack to be non empty.
        checkStackIsNotEmpty(listener, MISSING_HOLDER, WHEN_DATA, ctx.string().getText(), ENTRY);
        String condition = removeQuotesAndHandleConcat(ctx.string().getText());

        YangWhenHolder whenHolder;

        // Obtain the node of the stack.
        Parsable tmpNode = listener.getParsedDataStack().peek();
        if (tmpNode instanceof YangWhenHolder) {
            whenHolder = (YangWhenHolder) tmpNode;

            YangWhen when = new YangWhen();
            when.setCondition(condition);

            whenHolder.setWhen(when);
            listener.getParsedDataStack().push(when);
        } else {
            throw new ParserException(constructListenerErrorMessage(INVALID_HOLDER,
                    WHEN_DATA, ctx.string().getText(), ENTRY));
        }
    }

    /**
     * Performs validation and updates the data model tree.It is called when parser
     * exits from grammar rule (when).
     *
     * @param listener listener's object
     * @param ctx      context object of the grammar rule
     */
    public static void processWhenExit(TreeWalkListener listener,
                                       GeneratedYangParser.WhenStatementContext ctx) {

        // Check for stack to be non empty.
        checkStackIsNotEmpty(listener, MISSING_HOLDER, WHEN_DATA, ctx.string().getText(), EXIT);

        if (listener.getParsedDataStack().peek() instanceof YangWhen) {
            listener.getParsedDataStack().pop();
        } else {
            throw new ParserException(constructListenerErrorMessage(MISSING_CURRENT_HOLDER, WHEN_DATA,
                    ctx.string().getText(), EXIT));
        }
    }
}

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

import org.onosproject.yangutils.datamodel.YangBelongsTo;
import org.onosproject.yangutils.datamodel.YangSubModule;
import org.onosproject.yangutils.datamodel.utils.Parsable;
import org.onosproject.yangutils.parser.antlrgencode.GeneratedYangParser;
import org.onosproject.yangutils.parser.exceptions.ParserException;
import org.onosproject.yangutils.parser.impl.TreeWalkListener;

import static org.onosproject.yangutils.datamodel.utils.YangConstructType.BELONGS_TO_DATA;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerErrorLocation.ENTRY;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerErrorLocation.EXIT;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerErrorMessageConstruction.constructListenerErrorMessage;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerErrorType.INVALID_HOLDER;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerErrorType.MISSING_CURRENT_HOLDER;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerErrorType.MISSING_HOLDER;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerUtil.getValidIdentifier;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerValidation.checkStackIsNotEmpty;

/*
 * Reference: RFC6020 and YANG ANTLR Grammar
 *
 * ABNF grammar as per RFC6020
 * submodule-header-stmts =
 *                            ;; these stmts can appear in any order
 *                            [yang-version-stmt stmtsep]
 *                             belongs-to-stmt stmtsep
 *
 * belongs-to-stmt     = belongs-to-keyword sep identifier-arg-str
 *                       optsep
 *                       "{" stmtsep
 *                           prefix-stmt stmtsep
 *                       "}"
 *
 * ANTLR grammar rule
 * submodule_header_statement : yang_version_stmt? belongs_to_stmt
 *                            | belongs_to_stmt yang_version_stmt?
 *                            ;
 * belongs_to_stmt : BELONGS_TO_KEYWORD identifier LEFT_CURLY_BRACE belongs_to_stmt_body RIGHT_CURLY_BRACE;
 * belongs_to_stmt_body : prefix_stmt;
 */

/**
 * Represents listener based call back function corresponding to the
 * "belongs to" rule defined in ANTLR grammar file for corresponding ABNF rule
 * in RFC 6020.
 */
public final class BelongsToListener {

    /**
     * Creates a new belongto listener.
     */
    private BelongsToListener() {
    }

    /**
     * It is called when parser receives an input matching the grammar rule
     * (belongsto), perform validations and update the data model tree.
     *
     * @param listener Listener's object
     * @param ctx      context object of the grammar rule
     */
    public static void processBelongsToEntry(TreeWalkListener listener,
                                             GeneratedYangParser.BelongstoStatementContext ctx) {

        // Check for stack to be non empty.
        checkStackIsNotEmpty(listener, MISSING_HOLDER, BELONGS_TO_DATA, ctx.identifier().getText(),
                ENTRY);

        String identifier = getValidIdentifier(ctx.identifier().getText(), BELONGS_TO_DATA, ctx);

        YangBelongsTo belongstoNode = new YangBelongsTo();
        belongstoNode.setBelongsToModuleName(identifier);

        // Set the line number and character position in line for the belongs to.
        int errorLine = ctx.getStart().getLine();
        int errorPosition = ctx.getStart().getCharPositionInLine();
        belongstoNode.setLineNumber(errorLine);
        belongstoNode.setCharPosition(errorPosition);

        // Push belongsto into the stack.
        listener.getParsedDataStack().push(belongstoNode);
    }

    /**
     * It is called when parser exits from grammar rule (belongsto), it perform
     * validations and update the data model tree.
     *
     * @param listener Listener's object
     * @param ctx      context object of the grammar rule
     */
    public static void processBelongsToExit(TreeWalkListener listener,
                                            GeneratedYangParser.BelongstoStatementContext ctx) {

        // Check for stack to be non empty.
        checkStackIsNotEmpty(listener, MISSING_HOLDER, BELONGS_TO_DATA, ctx.identifier().getText(),
                EXIT);

        Parsable tmpBelongstoNode = listener.getParsedDataStack().peek();
        if (tmpBelongstoNode instanceof YangBelongsTo) {
            listener.getParsedDataStack().pop();

            // Check for stack to be empty.
            checkStackIsNotEmpty(listener, MISSING_HOLDER, BELONGS_TO_DATA,
                    ctx.identifier().getText(), EXIT);

            Parsable tmpNode = listener.getParsedDataStack().peek();
            switch (tmpNode.getYangConstructType()) {
                case SUB_MODULE_DATA: {
                    YangSubModule subModule = (YangSubModule) tmpNode;
                    subModule.setBelongsTo((YangBelongsTo) tmpBelongstoNode);
                    subModule.setPrefix(subModule.getBelongsTo().getPrefix());
                    break;
                }
                default:
                    throw new ParserException(constructListenerErrorMessage(INVALID_HOLDER, BELONGS_TO_DATA,
                            ctx.identifier().getText(),
                            EXIT));
            }
        } else {
            throw new ParserException(constructListenerErrorMessage(MISSING_CURRENT_HOLDER, BELONGS_TO_DATA,
                    ctx.identifier().getText(), EXIT));
        }
    }
}

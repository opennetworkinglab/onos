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

import org.onosproject.yangutils.datamodel.YangModule;
import org.onosproject.yangutils.datamodel.YangRevision;
import org.onosproject.yangutils.datamodel.YangSubModule;
import org.onosproject.yangutils.datamodel.utils.Parsable;
import org.onosproject.yangutils.parser.antlrgencode.GeneratedYangParser;
import org.onosproject.yangutils.parser.exceptions.ParserException;
import org.onosproject.yangutils.parser.impl.TreeWalkListener;

import static org.onosproject.yangutils.datamodel.utils.YangConstructType.REVISION_DATA;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerErrorLocation.ENTRY;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerErrorLocation.EXIT;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerErrorMessageConstruction.constructListenerErrorMessage;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerErrorType.INVALID_HOLDER;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerErrorType.MISSING_CURRENT_HOLDER;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerErrorType.MISSING_HOLDER;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerUtil.isDateValid;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerUtil.removeQuotesAndHandleConcat;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerValidation.checkStackIsNotEmpty;

/*
 * Reference: RFC6020 and YANG ANTLR Grammar
 *
 * ABNF grammar as per RFC6020
 * module-stmt         = optsep module-keyword sep identifier-arg-str
 *                       optsep
 *                       "{" stmtsep
 *                           module-header-stmts
 *                           linkage-stmts
 *                           meta-stmts
 *                           revision-stmts
 *                           body-stmts
 *                       "}" optsep
 *
 * revision-stmt       = revision-keyword sep revision-date optsep
 *                             (";" /
 *                              "{" stmtsep
 *                                  [description-stmt stmtsep]
 *                                  [reference-stmt stmtsep]
 *                              "}")
 *
 * ANTLR grammar rule
 * module_stmt : MODULE_KEYWORD IDENTIFIER LEFT_CURLY_BRACE module_body* RIGHT_CURLY_BRACE;
 *
 * revision_stmt : REVISION_KEYWORD DATE_ARG (STMTEND | LEFT_CURLY_BRACE revision_stmt_body RIGHT_CURLY_BRACE);
 * revision_stmt_body : description_stmt? reference_stmt?;
 */

/**
 * Represents listener based call back function corresponding to the "revision"
 * rule defined in ANTLR grammar file for corresponding ABNF rule in RFC 6020.
 */
public final class RevisionListener {

    /**
     * Creates a new revision listener.
     */
    private RevisionListener() {
    }

    /**
     * It is called when parser receives an input matching the grammar rule
     * (revision),perform validations and update the data model tree.
     *
     * @param listener Listener's object
     * @param ctx context object of the grammar rule
     */
    public static void processRevisionEntry(TreeWalkListener listener,
                                            GeneratedYangParser.RevisionStatementContext ctx) {

        // Check for stack to be non empty.
        checkStackIsNotEmpty(listener, MISSING_HOLDER, REVISION_DATA, ctx.dateArgumentString().getText(), ENTRY);

        // Validate for reverse chronological order of revision & for revision
        // value.
        if (!validateRevision(listener, ctx)) {
            return;
            // TODO to be implemented.
        }

        String date = removeQuotesAndHandleConcat(ctx.dateArgumentString().getText());
        if (!isDateValid(date)) {
            ParserException parserException = new ParserException("YANG file error: Input date is not correct");
            parserException.setLine(ctx.getStart().getLine());
            parserException.setCharPosition(ctx.getStart().getCharPositionInLine());
            throw parserException;
        }

        YangRevision revisionNode = new YangRevision();
        revisionNode.setRevDate(date);

        listener.getParsedDataStack().push(revisionNode);
    }

    /**
     * It is called when parser exits from grammar rule (revision), it perform
     * validations and update the data model tree.
     *
     * @param listener Listener's object
     * @param ctx context object of the grammar rule
     */
    public static void processRevisionExit(TreeWalkListener listener, GeneratedYangParser.RevisionStatementContext
            ctx) {

        // Check for stack to be non empty.
        checkStackIsNotEmpty(listener, MISSING_HOLDER, REVISION_DATA, ctx.dateArgumentString().getText(), EXIT);

        Parsable tmpRevisionNode = listener.getParsedDataStack().peek();
        if (tmpRevisionNode instanceof YangRevision) {
            listener.getParsedDataStack().pop();

            // Check for stack to be non empty.
            checkStackIsNotEmpty(listener, MISSING_HOLDER, REVISION_DATA, ctx.dateArgumentString().getText(),
                                 EXIT);

            Parsable tmpNode = listener.getParsedDataStack().peek();
            switch (tmpNode.getYangConstructType()) {
                case MODULE_DATA: {
                    YangModule module = (YangModule) tmpNode;
                    module.setRevision((YangRevision) tmpRevisionNode);
                    break;
                }
                case SUB_MODULE_DATA: {
                    YangSubModule subModule = (YangSubModule) tmpNode;
                    subModule.setRevision((YangRevision) tmpRevisionNode);
                    break;
                }
                default:
                    throw new ParserException(constructListenerErrorMessage(INVALID_HOLDER, REVISION_DATA,
                            ctx.dateArgumentString().getText(),
                            EXIT));
            }
        } else {
            throw new ParserException(constructListenerErrorMessage(MISSING_CURRENT_HOLDER, REVISION_DATA,
                                                                    ctx.dateArgumentString().getText(), EXIT));
        }
    }

    /**
     * Validate revision.
     *
     * @param listener Listener's object
     * @param ctx context object of the grammar rule
     * @return validation result
     */
    private static boolean validateRevision(TreeWalkListener listener,
                                            GeneratedYangParser.RevisionStatementContext ctx) {
        // TODO to be implemented
        return true;
    }
}

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

import org.onosproject.yangutils.datamodel.YangImport;
import org.onosproject.yangutils.datamodel.YangInclude;
import org.onosproject.yangutils.datamodel.utils.Parsable;
import org.onosproject.yangutils.parser.antlrgencode.GeneratedYangParser;
import org.onosproject.yangutils.parser.exceptions.ParserException;
import org.onosproject.yangutils.parser.impl.TreeWalkListener;

import static org.onosproject.yangutils.datamodel.utils.YangConstructType.REVISION_DATE_DATA;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerErrorLocation.ENTRY;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerErrorMessageConstruction.constructListenerErrorMessage;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerErrorType.INVALID_HOLDER;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerErrorType.MISSING_HOLDER;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerUtil.isDateValid;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerUtil.removeQuotesAndHandleConcat;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerValidation.checkStackIsNotEmpty;

/*
 * Reference: RFC6020 and YANG ANTLR Grammar
 *
 * ABNF grammar as per RFC6020
 * import-stmt         = import-keyword sep identifier-arg-str optsep
 *                       "{" stmtsep
 *                           prefix-stmt stmtsep
 *                           [revision-date-stmt stmtsep]
 *                        "}"
 * include-stmt        = include-keyword sep identifier-arg-str optsep
 *                             (";" /
 *                              "{" stmtsep
 *                                  [revision-date-stmt stmtsep]
 *                            "}")
 * revision-date-stmt = revision-date-keyword sep revision-date stmtend
 *
 * ANTLR grammar rule
 * import_stmt : IMPORT_KEYWORD IDENTIFIER LEFT_CURLY_BRACE import_stmt_body
 *               RIGHT_CURLY_BRACE;
 * import_stmt_body : prefix_stmt revision_date_stmt?;
 *
 * include_stmt : INCLUDE_KEYWORD IDENTIFIER (STMTEND | LEFT_CURLY_BRACE
 *                revision_date_stmt_body? RIGHT_CURLY_BRACE);
 *
 * revision_date_stmt : REVISION_DATE_KEYWORD DATE_ARG STMTEND;
 *
 */

/**
 * Represents listener based call back function corresponding to the
 * "revision date" rule defined in ANTLR grammar file for corresponding ABNF
 * rule in RFC 6020.
 */
public final class RevisionDateListener {

    /**
     * Creates a new revision date listener.
     */
    private RevisionDateListener() {
    }

    /**
     * It is called when parser receives an input matching the grammar rule
     * (revision date),perform validations and update the data model tree.
     *
     * @param listener Listener's object
     * @param ctx context object of the grammar rule
     */
    public static void processRevisionDateEntry(TreeWalkListener listener,
            GeneratedYangParser.RevisionDateStatementContext ctx) {

        // Check for stack to be non empty.
        checkStackIsNotEmpty(listener, MISSING_HOLDER, REVISION_DATE_DATA, ctx.dateArgumentString().getText(),
                ENTRY);

        String date = removeQuotesAndHandleConcat(ctx.dateArgumentString().getText());
        if (!isDateValid(date)) {
            ParserException parserException = new ParserException("YANG file error: Input date is not correct");
            parserException.setLine(ctx.getStart().getLine());
            parserException.setCharPosition(ctx.getStart().getCharPositionInLine());
            throw parserException;
        }

        // Obtain the node of the stack.
        Parsable tmpNode = listener.getParsedDataStack().peek();
        switch (tmpNode.getYangConstructType()) {
            case IMPORT_DATA: {
                YangImport importNode = (YangImport) tmpNode;
                importNode.setRevision(date);
                break;
            }
            case INCLUDE_DATA: {
                YangInclude includeNode = (YangInclude) tmpNode;
                includeNode.setRevision(date);
                break;
            }
            default:
                throw new ParserException(constructListenerErrorMessage(INVALID_HOLDER, REVISION_DATE_DATA,
                        ctx.dateArgumentString().getText(), ENTRY));
        }
    }
}
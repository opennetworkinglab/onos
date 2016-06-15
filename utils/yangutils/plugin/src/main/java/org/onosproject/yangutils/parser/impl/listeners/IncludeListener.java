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

import org.onosproject.yangutils.datamodel.YangInclude;
import org.onosproject.yangutils.datamodel.YangModule;
import org.onosproject.yangutils.datamodel.YangSubModule;
import org.onosproject.yangutils.datamodel.utils.Parsable;
import org.onosproject.yangutils.parser.antlrgencode.GeneratedYangParser;
import org.onosproject.yangutils.parser.exceptions.ParserException;
import org.onosproject.yangutils.parser.impl.TreeWalkListener;

import static org.onosproject.yangutils.datamodel.utils.YangConstructType.INCLUDE_DATA;
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
 * linkage-stmts       = ;; these stmts can appear in any order
 *                       *(import-stmt stmtsep)
 *                       *(include-stmt stmtsep)
 *
 * include-stmt        = include-keyword sep identifier-arg-str optsep
 *                             (";" /
 *                              "{" stmtsep
 *                                  [revision-date-stmt stmtsep]
 *                            "}")
 *
 * ANTLR grammar rule
 * linkage_stmts : (import_stmt
 *               | include_stmt)*;
 * include_stmt : INCLUDE_KEYWORD identifier (STMTEND | LEFT_CURLY_BRACE
 *                revision_date_stmt? RIGHT_CURLY_BRACE);
 */

/**
 * Represents listener based call back function corresponding to the "include"
 * rule defined in ANTLR grammar file for corresponding ABNF rule in RFC 6020.
 */
public final class IncludeListener {

    /**
     * Creates a new include listener.
     */
    private IncludeListener() {
    }

    /**
     * It is called when parser receives an input matching the grammar rule
     * (include), perform validations and update the data model tree.
     *
     * @param listener Listener's object
     * @param ctx      context object of the grammar rule
     */
    public static void processIncludeEntry(TreeWalkListener listener, GeneratedYangParser.IncludeStatementContext ctx) {

        // Check for stack to be non empty.
        checkStackIsNotEmpty(listener, MISSING_HOLDER, INCLUDE_DATA, ctx.identifier().getText(),
                ENTRY);

        String identifier = getValidIdentifier(ctx.identifier().getText(), INCLUDE_DATA, ctx);

        YangInclude includeNode = new YangInclude();
        includeNode.setSubModuleName(identifier);

        // Set the line number and character position in line for the belongs to.
        int errorLine = ctx.getStart().getLine();
        int errorPosition = ctx.getStart().getCharPositionInLine();
        includeNode.setLineNumber(errorLine);
        includeNode.setCharPosition(errorPosition);

        listener.getParsedDataStack().push(includeNode);
    }

    /**
     * It is called when parser exits from grammar rule (include), it perform
     * validations and update the data model tree.
     *
     * @param listener Listener's object
     * @param ctx      context object of the grammar rule
     */
    public static void processIncludeExit(TreeWalkListener listener, GeneratedYangParser.IncludeStatementContext ctx) {

        // Check for stack to be non empty.
        checkStackIsNotEmpty(listener, MISSING_HOLDER, INCLUDE_DATA, ctx.identifier().getText(), EXIT);

        Parsable tmpIncludeNode = listener.getParsedDataStack().peek();
        if (tmpIncludeNode instanceof YangInclude) {
            listener.getParsedDataStack().pop();

            // Check for stack to be non empty.
            checkStackIsNotEmpty(listener, MISSING_HOLDER, INCLUDE_DATA, ctx.identifier().getText(),
                    EXIT);

            Parsable tmpNode = listener.getParsedDataStack().peek();
            switch (tmpNode.getYangConstructType()) {
                case MODULE_DATA: {
                    YangModule module = (YangModule) tmpNode;
                    module.addToIncludeList((YangInclude) tmpIncludeNode);
                    break;
                }
                case SUB_MODULE_DATA: {
                    YangSubModule subModule = (YangSubModule) tmpNode;
                    subModule.addToIncludeList((YangInclude) tmpIncludeNode);
                    break;
                }
                default:
                    throw new ParserException(constructListenerErrorMessage(INVALID_HOLDER, INCLUDE_DATA,
                            ctx.identifier().getText(),
                            EXIT));
            }
        } else {
            throw new ParserException(constructListenerErrorMessage(MISSING_CURRENT_HOLDER, INCLUDE_DATA,
                    ctx.identifier().getText(), EXIT));
        }
    }
}

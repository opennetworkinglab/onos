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
import org.onosproject.yangutils.datamodel.YangModule;
import org.onosproject.yangutils.datamodel.YangSubModule;
import org.onosproject.yangutils.datamodel.utils.Parsable;
import org.onosproject.yangutils.parser.antlrgencode.GeneratedYangParser;
import org.onosproject.yangutils.parser.exceptions.ParserException;
import org.onosproject.yangutils.parser.impl.TreeWalkListener;

import static org.onosproject.yangutils.datamodel.utils.YangConstructType.IMPORT_DATA;
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
 * import-stmt         = import-keyword sep identifier-arg-str optsep
 *                       "{" stmtsep
 *                           prefix-stmt stmtsep
 *                           [revision-date-stmt stmtsep]
 *                        "}"
 *
 * ANTLR grammar rule
 * linkage_stmts : (import_stmt
 *               | include_stmt)*;
 * import_stmt : IMPORT_KEYWORD identifier LEFT_CURLY_BRACE import_stmt_body
 *               RIGHT_CURLY_BRACE;
 * import_stmt_body : prefix_stmt revision_date_stmt?;
 */

/**
 * Represents listener based call back function corresponding to the "import"
 * rule defined in ANTLR grammar file for corresponding ABNF rule in RFC 6020.
 */
public final class ImportListener {

    /**
     * Creates a new import listener.
     */
    private ImportListener() {
    }

    /**
     * It is called when parser receives an input matching the grammar rule
     * (import), perform validations and update the data model tree.
     *
     * @param listener Listener's object
     * @param ctx      context object of the grammar rule
     */
    public static void processImportEntry(TreeWalkListener listener, GeneratedYangParser.ImportStatementContext ctx) {

        // Check for stack to be non empty.
        checkStackIsNotEmpty(listener, MISSING_HOLDER, IMPORT_DATA, ctx.identifier().getText(), ENTRY);

        String identifier = getValidIdentifier(ctx.identifier().getText(), IMPORT_DATA, ctx);

        YangImport importNode = new YangImport();
        importNode.setModuleName(identifier);

        // Set the line number and character position in line for the belongs to.
        int errorLine = ctx.getStart().getLine();
        int errorPosition = ctx.getStart().getCharPositionInLine();
        importNode.setLineNumber(errorLine);
        importNode.setCharPosition(errorPosition);

        // Push import node to the stack.
        listener.getParsedDataStack().push(importNode);
    }

    /**
     * It is called when parser exits from grammar rule (import), it perform
     * validations and update the data model tree.
     *
     * @param listener Listener's object
     * @param ctx      context object of the grammar rule
     */
    public static void processImportExit(TreeWalkListener listener, GeneratedYangParser.ImportStatementContext ctx) {

        // Check for stack to be non empty.
        checkStackIsNotEmpty(listener, MISSING_HOLDER, IMPORT_DATA, ctx.identifier().getText(), EXIT);

        Parsable tmpImportNode = listener.getParsedDataStack().peek();
        if (tmpImportNode instanceof YangImport) {
            listener.getParsedDataStack().pop();

            // Check for stack to be non empty.
            checkStackIsNotEmpty(listener, MISSING_HOLDER, IMPORT_DATA, ctx.identifier().getText(),
                    EXIT);

            Parsable tmpNode = listener.getParsedDataStack().peek();
            switch (tmpNode.getYangConstructType()) {
                case MODULE_DATA: {
                    YangModule module = (YangModule) tmpNode;
                    module.addToImportList((YangImport) tmpImportNode);
                    break;
                }
                case SUB_MODULE_DATA: {
                    YangSubModule subModule = (YangSubModule) tmpNode;
                    subModule.addToImportList((YangImport) tmpImportNode);
                    break;
                }
                default:
                    throw new ParserException(constructListenerErrorMessage(INVALID_HOLDER, IMPORT_DATA,
                            ctx.identifier().getText(),
                            EXIT));
            }
        } else {
            throw new ParserException(constructListenerErrorMessage(MISSING_CURRENT_HOLDER, IMPORT_DATA,
                    ctx.identifier().getText(), EXIT));
        }
    }
}

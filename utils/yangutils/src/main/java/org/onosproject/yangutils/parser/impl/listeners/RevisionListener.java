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

package org.onosproject.yangutils.parser.impl.listeners;

import org.onosproject.yangutils.datamodel.YangModule;
import org.onosproject.yangutils.datamodel.YangRevision;
import org.onosproject.yangutils.datamodel.YangSubModule;
import org.onosproject.yangutils.parser.Parsable;
import org.onosproject.yangutils.parser.ParsableDataType;
import org.onosproject.yangutils.parser.antlrgencode.GeneratedYangParser;
import org.onosproject.yangutils.parser.exceptions.ParserException;
import org.onosproject.yangutils.parser.impl.TreeWalkListener;
import org.onosproject.yangutils.parser.impl.parserutils.ListenerErrorLocation;
import org.onosproject.yangutils.parser.impl.parserutils.ListenerErrorMessageConstruction;
import org.onosproject.yangutils.parser.impl.parserutils.ListenerErrorType;
import org.onosproject.yangutils.parser.impl.parserutils.ListenerValidation;

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
 * Implements listener based call back function corresponding to the "revision"
 * rule defined in ANTLR grammar file for corresponding ABNF rule in RFC 6020.
 */
public final class RevisionListener {

    /**
     * Creates a new revision listener.
     */
    private RevisionListener() {
    }

    public static void processRevisionEntry(TreeWalkListener listener,
                                            GeneratedYangParser.RevisionStatementContext ctx) {

        // Check for stack to be non empty.
        ListenerValidation.checkStackIsNotEmpty(listener, ListenerErrorType.MISSING_HOLDER,
                                                ParsableDataType.REVISION_DATA,
                                                String.valueOf(ctx.DATE_ARG().getText()),
                                                ListenerErrorLocation.ENTRY);

        // Validate for reverse chronological order of revision & for revision value.
        if (!validateRevision(listener, ctx)) {
            return;
            // TODO to be implemented.
        }

        YangRevision revisionNode = new YangRevision();
        revisionNode.setRevDate(String.valueOf(ctx.DATE_ARG().getText()));

        listener.getParsedDataStack().push(revisionNode);
    }

    /**
     * It is called when parser exits from grammar rule (revision), it perform
     * validations and update the data model tree.
     *
     * @param listener Listener's object.
     * @param ctx context object of the grammar rule.
     */
    public static void processRevisionExit(TreeWalkListener listener,
                                           GeneratedYangParser.RevisionStatementContext ctx) {

        // Check for stack to be non empty.
        ListenerValidation
                .checkStackIsNotEmpty(listener, ListenerErrorType.MISSING_HOLDER, ParsableDataType.REVISION_DATA,
                                      String.valueOf(ctx.DATE_ARG().getText()), ListenerErrorLocation.EXIT);

        Parsable tmpRevisionNode = listener.getParsedDataStack().peek();
        if (tmpRevisionNode instanceof YangRevision) {
            listener.getParsedDataStack().pop();

            // Check for stack to be non empty.
            ListenerValidation.checkStackIsNotEmpty(listener, ListenerErrorType.MISSING_HOLDER,
                                                    ParsableDataType.REVISION_DATA,
                                                    String.valueOf(ctx.DATE_ARG().getText()),
                                                    ListenerErrorLocation.EXIT);

            Parsable tmpNode = listener.getParsedDataStack().peek();
            switch (tmpNode.getParsableDataType()) {
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
                throw new ParserException(
                                          ListenerErrorMessageConstruction
                                                  .constructListenerErrorMessage(ListenerErrorType.INVALID_HOLDER,
                                                                                 ParsableDataType.REVISION_DATA,
                                                                                 String.valueOf(ctx.DATE_ARG()
                                                                                         .getText()),
                                                                                 ListenerErrorLocation.EXIT));
            }
        } else {
            throw new ParserException(
                                      ListenerErrorMessageConstruction
                                              .constructListenerErrorMessage(ListenerErrorType.MISSING_CURRENT_HOLDER,
                                                                             ParsableDataType.REVISION_DATA, String
                                                                                     .valueOf(ctx.DATE_ARG()
                                                                                             .getText()),
                                                                             ListenerErrorLocation.EXIT));
        }
    }

    /**
     * Validate revision.
     *
     * @param listener Listener's object.
     * @param ctx context object of the grammar rule.
     * @return validation result
     */
    private static boolean validateRevision(TreeWalkListener listener,
                                            GeneratedYangParser.RevisionStatementContext ctx) {
        // TODO to be implemented
        return true;
    }
}
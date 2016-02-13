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

import org.onosproject.yangutils.datamodel.YangInclude;
import org.onosproject.yangutils.datamodel.YangModule;
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
 * include_stmt : INCLUDE_KEYWORD IDENTIFIER (STMTEND | LEFT_CURLY_BRACE
 *                revision_date_stmt? RIGHT_CURLY_BRACE);
 */

/**
 * Implements listener based call back function corresponding to the "include"
 * rule defined in ANTLR grammar file for corresponding ABNF rule in RFC 6020.
 */
public final class IncludeListener {

    /**
     * Creates a new include listener.
     */
    private IncludeListener() {
    }

    /**
     * It is called when parser receives an input matching the grammar
     * rule (include), perform validations and update the data model
     * tree.
     *
     * @param listener Listener's object.
     * @param ctx context object of the grammar rule.
     */
    public static void processIncludeEntry(TreeWalkListener listener, GeneratedYangParser.IncludeStatementContext ctx) {

        // Check for stack to be non empty.
        ListenerValidation.checkStackIsNotEmpty(listener, ListenerErrorType.MISSING_HOLDER,
                                                ParsableDataType.INCLUDE_DATA,
                                                String.valueOf(ctx.IDENTIFIER().getText()),
                                                ListenerErrorLocation.ENTRY);

        YangInclude includeNode = new YangInclude();
        includeNode.setSubModuleName(String.valueOf(ctx.IDENTIFIER().getText()));

        listener.getParsedDataStack().push(includeNode);
    }

    /**
     * It is called when parser exits from grammar rule (include), it perform
     * validations and update the data model tree.
     *
     * @param listener Listener's object.
     * @param ctx context object of the grammar rule.
     */
    public static void processIncludeExit(TreeWalkListener listener, GeneratedYangParser.IncludeStatementContext ctx) {

        // Check for stack to be non empty.
        ListenerValidation.checkStackIsNotEmpty(listener, ListenerErrorType.MISSING_HOLDER,
                                                ParsableDataType.INCLUDE_DATA,
                                                String.valueOf(ctx.IDENTIFIER().getText()),
                                                ListenerErrorLocation.EXIT);

        Parsable tmpIncludeNode = listener.getParsedDataStack().peek();
        if (tmpIncludeNode instanceof YangInclude) {
            listener.getParsedDataStack().pop();

            // Check for stack to be non empty.
            ListenerValidation.checkStackIsNotEmpty(listener, ListenerErrorType.MISSING_HOLDER,
                                                    ParsableDataType.INCLUDE_DATA,
                                                    String.valueOf(ctx.IDENTIFIER().getText()),
                                                    ListenerErrorLocation.EXIT);

            Parsable tmpNode = listener.getParsedDataStack().peek();
            switch (tmpNode.getParsableDataType()) {
            case MODULE_DATA: {
                YangModule module = (YangModule) tmpNode;
                module.addIncludedInfo((YangInclude) tmpIncludeNode);
                break;
            }
            case SUB_MODULE_DATA: {
                YangSubModule subModule = (YangSubModule) tmpNode;
                subModule.addIncludedInfo((YangInclude) tmpIncludeNode);
                break;
            }
            default:
                throw new ParserException(
                                          ListenerErrorMessageConstruction
                                                  .constructListenerErrorMessage(ListenerErrorType.INVALID_HOLDER,
                                                                                 ParsableDataType.INCLUDE_DATA,
                                                                                 String.valueOf(ctx.IDENTIFIER()
                                                                                         .getText()),
                                                                                 ListenerErrorLocation.EXIT));
            }
        } else {
            throw new ParserException(
                                      ListenerErrorMessageConstruction
                                              .constructListenerErrorMessage(ListenerErrorType.MISSING_CURRENT_HOLDER,
                                                                             ParsableDataType.INCLUDE_DATA, String
                                                                                     .valueOf(ctx.IDENTIFIER()
                                                                                             .getText()),
                                                                             ListenerErrorLocation.EXIT));
        }
    }
}
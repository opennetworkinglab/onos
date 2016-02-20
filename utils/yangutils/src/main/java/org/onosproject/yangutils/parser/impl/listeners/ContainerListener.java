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

import org.onosproject.yangutils.datamodel.YangContainer;
import org.onosproject.yangutils.datamodel.YangList;
import org.onosproject.yangutils.datamodel.YangModule;
import org.onosproject.yangutils.datamodel.YangNode;
import org.onosproject.yangutils.datamodel.exceptions.DataModelException;
import org.onosproject.yangutils.parser.Parsable;
import org.onosproject.yangutils.parser.ParsableDataType;
import org.onosproject.yangutils.parser.antlrgencode.GeneratedYangParser;
import org.onosproject.yangutils.parser.exceptions.ParserException;
import org.onosproject.yangutils.parser.impl.TreeWalkListener;
import org.onosproject.yangutils.parser.impl.YangUtilsParserManager;

import static org.onosproject.yangutils.parser.ParsableDataType.CONTAINER_DATA;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerErrorLocation.ENTRY;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerErrorLocation.EXIT;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerErrorMessageConstruction.constructListenerErrorMessage;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerErrorMessageConstruction.constructExtendedListenerErrorMessage;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerErrorType.INVALID_HOLDER;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerErrorType.MISSING_HOLDER;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerErrorType.UNHANDLED_PARSED_DATA;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerErrorType.INVALID_CARDINALITY;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerErrorType.MISSING_CURRENT_HOLDER;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerValidation.checkStackIsNotEmpty;

/*
 * Reference: RFC6020 and YANG ANTLR Grammar
 *
 * ABNF grammar as per RFC6020
 *  container-stmt      = container-keyword sep identifier-arg-str optsep
 *                        (";" /
 *                         "{" stmtsep
 *                             ;; these stmts can appear in any order
 *                             [when-stmt stmtsep]
 *                             *(if-feature-stmt stmtsep)
 *                             *(must-stmt stmtsep)
 *                             [presence-stmt stmtsep]
 *                             [config-stmt stmtsep]
 *                             [status-stmt stmtsep]
 *                             [description-stmt stmtsep]
 *                             [reference-stmt stmtsep]
 *                             *((typedef-stmt /
 *                                grouping-stmt) stmtsep)
 *                             *(data-def-stmt stmtsep)
 *                         "}")
 *
 * ANTLR grammar rule
 *  containerStatement : CONTAINER_KEYWORD IDENTIFIER
 *                   (STMTEND | LEFT_CURLY_BRACE (whenStatement | ifFeatureStatement | mustStatement |
 *                   presenceStatement | configStatement | statusStatement | descriptionStatement |
 *                   referenceStatement | typedefStatement | groupingStatement
 *                    | dataDefStatement)* RIGHT_CURLY_BRACE);
 */

/**
 * Implements listener based call back function corresponding to the "container"
 * rule defined in ANTLR grammar file for corresponding ABNF rule in RFC 6020.
 */
public final class ContainerListener {

    private static ParsableDataType yangConstruct;

    /**
     * Creates a new container listener.
     */
    private ContainerListener() {
    }

    /**
     * It is called when parser receives an input matching the grammar
     * rule (container), performs validation and updates the data model
     * tree.
     *
     * @param listener listener's object.
     * @param ctx context object of the grammar rule.
     */
    public static void processContainerEntry(TreeWalkListener listener,
                                             GeneratedYangParser.ContainerStatementContext ctx) {

        // Check for stack to be non empty.
        checkStackIsNotEmpty(listener, MISSING_HOLDER, CONTAINER_DATA, ctx.IDENTIFIER().getText(), ENTRY);

        boolean result = validateSubStatementsCardinality(ctx);
        if (!result) {
            throw new ParserException(constructListenerErrorMessage(INVALID_CARDINALITY, yangConstruct, "", ENTRY));
        }

        YangContainer container = new YangContainer();
        container.setName(ctx.IDENTIFIER().getText());

        Parsable curData = listener.getParsedDataStack().peek();

        if ((curData instanceof YangModule) || (curData instanceof YangContainer)
                || (curData instanceof YangList)) {
            YangNode curNode = (YangNode) curData;
            try {
                curNode.addChild(container);
            } catch (DataModelException e) {
                throw new ParserException(constructExtendedListenerErrorMessage(UNHANDLED_PARSED_DATA,
                                CONTAINER_DATA, ctx.IDENTIFIER().getText(), ENTRY, e.getMessage()));
            }
            listener.getParsedDataStack().push(container);
        } else {
            throw new ParserException(constructListenerErrorMessage(INVALID_HOLDER, CONTAINER_DATA,
                    ctx.IDENTIFIER().getText(), ENTRY));
        }
    }

    /**
     * It is called when parser exits from grammar rule (container), it perform
     * validations and updates the data model tree.
     *
     * @param listener listener's object.
     * @param ctx context object of the grammar rule.
     */
    public static void processContainerExit(TreeWalkListener listener,
                                            GeneratedYangParser.ContainerStatementContext ctx) {

        // Check for stack to be non empty.
        checkStackIsNotEmpty(listener, MISSING_HOLDER, CONTAINER_DATA, ctx.IDENTIFIER().getText(), EXIT);

        if (listener.getParsedDataStack().peek() instanceof YangContainer) {
            listener.getParsedDataStack().pop();
        } else {
            throw new ParserException(constructListenerErrorMessage(MISSING_CURRENT_HOLDER, CONTAINER_DATA,
                            ctx.IDENTIFIER().getText(), EXIT));
        }
    }

    /**
     * Validates the cardinality of container sub-statements as per grammar.
     *
     * @param ctx context object of the grammar rule.
     * @return true/false validation success or failure.
     */
    private static boolean validateSubStatementsCardinality(GeneratedYangParser.ContainerStatementContext ctx) {

        if ((!ctx.presenceStatement().isEmpty())
                && (ctx.presenceStatement().size() != YangUtilsParserManager.SUB_STATEMENT_CARDINALITY)) {
            yangConstruct = ParsableDataType.PRESENCE_DATA;
            return false;
        }

        if ((!ctx.configStatement().isEmpty())
                && (ctx.configStatement().size() != YangUtilsParserManager.SUB_STATEMENT_CARDINALITY)) {
            yangConstruct = ParsableDataType.CONFIG_DATA;
            return false;
        }

        if ((!ctx.descriptionStatement().isEmpty())
                && (ctx.descriptionStatement().size() != YangUtilsParserManager.SUB_STATEMENT_CARDINALITY)) {
            yangConstruct = ParsableDataType.DESCRIPTION_DATA;
            return false;
        }

        if ((!ctx.referenceStatement().isEmpty())
                && (ctx.referenceStatement().size() != YangUtilsParserManager.SUB_STATEMENT_CARDINALITY)) {
            yangConstruct = ParsableDataType.REFERENCE_DATA;
            return false;
        }

        if ((!ctx.statusStatement().isEmpty())
                && (ctx.statusStatement().size() != YangUtilsParserManager.SUB_STATEMENT_CARDINALITY)) {
            yangConstruct = ParsableDataType.STATUS_DATA;
            return false;
        }

        return true;
    }
}

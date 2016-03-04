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
import org.onosproject.yangutils.parser.antlrgencode.GeneratedYangParser;
import org.onosproject.yangutils.parser.exceptions.ParserException;
import org.onosproject.yangutils.parser.impl.TreeWalkListener;
import org.onosproject.yangutils.parser.impl.parserutils.ListenerValidation;

import static org.onosproject.yangutils.parser.impl.parserutils.ListenerUtil.getValidIdentifier;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerCollisionDetector.detectCollidingChildUtil;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerErrorLocation.ENTRY;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerErrorLocation.EXIT;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerErrorMessageConstruction.constructExtendedListenerErrorMessage;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerErrorMessageConstruction.constructListenerErrorMessage;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerErrorType.INVALID_HOLDER;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerErrorType.MISSING_CURRENT_HOLDER;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerErrorType.MISSING_HOLDER;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerErrorType.UNHANDLED_PARSED_DATA;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerValidation.checkStackIsNotEmpty;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerValidation.validateCardinality;
import static org.onosproject.yangutils.utils.YangConstructType.CONFIG_DATA;
import static org.onosproject.yangutils.utils.YangConstructType.CONTAINER_DATA;
import static org.onosproject.yangutils.utils.YangConstructType.DESCRIPTION_DATA;
import static org.onosproject.yangutils.utils.YangConstructType.PRESENCE_DATA;
import static org.onosproject.yangutils.utils.YangConstructType.REFERENCE_DATA;
import static org.onosproject.yangutils.utils.YangConstructType.STATUS_DATA;

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
 *  containerStatement : CONTAINER_KEYWORD identifier
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

    /**
     * Creates a new container listener.
     */
    private ContainerListener() {
    }

    /**
     * It is called when parser receives an input matching the grammar rule
     * (container), performs validation and updates the data model tree.
     *
     * @param listener listener's object
     * @param ctx context object of the grammar rule
     */
    public static void processContainerEntry(TreeWalkListener listener,
            GeneratedYangParser.ContainerStatementContext ctx) {

        // Check for stack to be non empty.
        checkStackIsNotEmpty(listener, MISSING_HOLDER, CONTAINER_DATA, ctx.identifier().getText(), ENTRY);

        String identifier = getValidIdentifier(ctx.identifier().getText(), CONTAINER_DATA, ctx);

        // Validate sub statement cardinality.
        validateSubStatementsCardinality(ctx);

        // Check for identifier collision
        int line = ctx.getStart().getLine();
        int charPositionInLine = ctx.getStart().getCharPositionInLine();
        detectCollidingChildUtil(listener, line, charPositionInLine, identifier, CONTAINER_DATA);

        YangContainer container = new YangContainer();
        container.setName(identifier);

        /*
         * If "config" is not specified, the default is the same as the parent
         * schema node's "config" value.
         */
        if (ctx.configStatement().isEmpty()) {
            boolean parentConfig = ListenerValidation.getParentNodeConfig(listener);
            container.setConfig(parentConfig);
        }

        Parsable curData = listener.getParsedDataStack().peek();
        if ((curData instanceof YangModule) || (curData instanceof YangContainer)
                || (curData instanceof YangList)) {
            YangNode curNode = (YangNode) curData;
            try {
                curNode.addChild(container);
            } catch (DataModelException e) {
                throw new ParserException(constructExtendedListenerErrorMessage(UNHANDLED_PARSED_DATA,
                                CONTAINER_DATA, ctx.identifier().getText(), ENTRY, e.getMessage()));
            }
            listener.getParsedDataStack().push(container);
        } else {
            throw new ParserException(constructListenerErrorMessage(INVALID_HOLDER, CONTAINER_DATA,
                    ctx.identifier().getText(), ENTRY));
        }
    }

    /**
     * It is called when parser exits from grammar rule (container), it perform
     * validations and updates the data model tree.
     *
     * @param listener listener's object
     * @param ctx context object of the grammar rule
     */
    public static void processContainerExit(TreeWalkListener listener,
            GeneratedYangParser.ContainerStatementContext ctx) {

        // Check for stack to be non empty.
        checkStackIsNotEmpty(listener, MISSING_HOLDER, CONTAINER_DATA, ctx.identifier().getText(), EXIT);

        if (listener.getParsedDataStack().peek() instanceof YangContainer) {
            YangContainer yangContainer = (YangContainer) listener.getParsedDataStack().peek();
            try {
                yangContainer.validateDataOnExit();
            } catch (DataModelException e) {
                throw new ParserException(constructExtendedListenerErrorMessage(UNHANDLED_PARSED_DATA,
                        CONTAINER_DATA, ctx.identifier().getText(), EXIT, e.getMessage()));
            }
            listener.getParsedDataStack().pop();
        } else {
            throw new ParserException(constructListenerErrorMessage(MISSING_CURRENT_HOLDER, CONTAINER_DATA,
                            ctx.identifier().getText(), EXIT));
        }
    }

    /**
     * Validates the cardinality of container sub-statements as per grammar.
     *
     * @param ctx context object of the grammar rule
     */
    private static void validateSubStatementsCardinality(GeneratedYangParser.ContainerStatementContext ctx) {

        validateCardinality(ctx.presenceStatement(), PRESENCE_DATA, CONTAINER_DATA, ctx.identifier().getText());
        validateCardinality(ctx.configStatement(), CONFIG_DATA, CONTAINER_DATA, ctx.identifier().getText());
        validateCardinality(ctx.descriptionStatement(), DESCRIPTION_DATA, CONTAINER_DATA, ctx.identifier().getText());
        validateCardinality(ctx.referenceStatement(), REFERENCE_DATA, CONTAINER_DATA, ctx.identifier().getText());
        validateCardinality(ctx.statusStatement(), STATUS_DATA, CONTAINER_DATA, ctx.identifier().getText());
        // TODO when, grouping, typedef.
    }
}

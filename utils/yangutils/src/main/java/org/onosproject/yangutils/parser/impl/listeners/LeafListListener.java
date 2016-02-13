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

import org.onosproject.yangutils.datamodel.YangLeafList;
import org.onosproject.yangutils.datamodel.YangLeavesHolder;
import org.onosproject.yangutils.parser.Parsable;
import org.onosproject.yangutils.parser.ParsableDataType;
import org.onosproject.yangutils.parser.antlrgencode.GeneratedYangParser;
import org.onosproject.yangutils.parser.exceptions.ParserException;
import org.onosproject.yangutils.parser.impl.TreeWalkListener;
import org.onosproject.yangutils.parser.impl.YangUtilsParserManager;
import org.onosproject.yangutils.parser.impl.parserutils.ListenerErrorLocation;
import org.onosproject.yangutils.parser.impl.parserutils.ListenerErrorMessageConstruction;
import org.onosproject.yangutils.parser.impl.parserutils.ListenerErrorType;
import org.onosproject.yangutils.parser.impl.parserutils.ListenerValidation;

/*
 * Reference: RFC6020 and YANG ANTLR Grammar
 *
 * ABNF grammar as per RFC6020
 *  leaf-list-stmt      = leaf-list-keyword sep identifier-arg-str optsep
 *                        "{" stmtsep
 *                            ;; these stmts can appear in any order
 *                            [when-stmt stmtsep]
 *                            *(if-feature-stmt stmtsep)
 *                            type-stmt stmtsep
 *                            [units-stmt stmtsep]
 *                            *(must-stmt stmtsep)
 *                            [config-stmt stmtsep]
 *                            [min-elements-stmt stmtsep]
 *                            [max-elements-stmt stmtsep]
 *                            [ordered-by-stmt stmtsep]
 *                            [status-stmt stmtsep]
 *                            [description-stmt stmtsep]
 *                            [reference-stmt stmtsep]
 *                         "}"
 *
 * ANTLR grammar rule
 *  leafListStatement : LEAF_LIST_KEYWORD IDENTIFIER LEFT_CURLY_BRACE (whenStatement | ifFeatureStatement |
 *  typeStatement | unitsStatement | mustStatement | configStatement | minElementsStatement | maxElementsStatement |
 *  orderedByStatement | statusStatement | descriptionStatement | referenceStatement)* RIGHT_CURLY_BRACE;
 */

/**
 * Implements listener based call back function corresponding to the "leaf-list"
 * rule defined in ANTLR grammar file for corresponding ABNF rule in RFC 6020.
 */
public final class LeafListListener {

    private static ParsableDataType yangConstruct;

    /**
     * Creates a new leaf list listener.
     */
    private LeafListListener() {
    }

    /**
     * It is called when parser receives an input matching the grammar
     * rule (leaf-list), performs validation and updates the data model
     * tree.
     *
     * @param listener listener's object.
     * @param ctx context object of the grammar rule.
     */
    public static void processLeafListEntry(TreeWalkListener listener,
                                            GeneratedYangParser.LeafListStatementContext ctx) {

        // Check for stack to be non empty.
        ListenerValidation.checkStackIsNotEmpty(listener, ListenerErrorType.MISSING_HOLDER,
                ParsableDataType.LEAF_LIST_DATA, String.valueOf(ctx.IDENTIFIER().getText()),
                ListenerErrorLocation.ENTRY);

        boolean result = validateSubStatementsCardinality(ctx);
        if (!result) {
            throw new ParserException(ListenerErrorMessageConstruction
                    .constructListenerErrorMessage(ListenerErrorType.INVALID_CARDINALITY,
                            yangConstruct, "", ListenerErrorLocation.ENTRY));
        }

        YangLeafList leafList = new YangLeafList();
        leafList.setLeafName(ctx.IDENTIFIER().getText());

        Parsable tmpData = listener.getParsedDataStack().peek();
        YangLeavesHolder leaves;

        if (tmpData instanceof YangLeavesHolder) {
            leaves = (YangLeavesHolder) tmpData;
            leaves.addLeafList(leafList);
        } else {
            throw new ParserException(ListenerErrorMessageConstruction
                    .constructListenerErrorMessage(ListenerErrorType.INVALID_HOLDER,
                            ParsableDataType.LEAF_LIST_DATA,
                            String.valueOf(ctx.IDENTIFIER().getText()),
                            ListenerErrorLocation.ENTRY));
        }
        listener.getParsedDataStack().push(leafList);
    }

    /**
     * It is called when parser exits from grammar rule (leaf-list), it performs
     * validation and updates the data model tree.
     *
     * @param listener listener's object.
     * @param ctx context object of the grammar rule.
     */
    public static void processLeafListExit(TreeWalkListener listener,
                                           GeneratedYangParser.LeafListStatementContext ctx) {

        // Check for stack to be non empty.
        ListenerValidation.checkStackIsNotEmpty(listener, ListenerErrorType.MISSING_HOLDER,
                ParsableDataType.LEAF_LIST_DATA, String.valueOf(ctx.IDENTIFIER().getText()),
                ListenerErrorLocation.EXIT);

        if (listener.getParsedDataStack().peek() instanceof YangLeafList) {
            listener.getParsedDataStack().pop();
        } else {
            throw new ParserException(ListenerErrorMessageConstruction
                    .constructListenerErrorMessage(ListenerErrorType.INVALID_HOLDER,
                            ParsableDataType.LEAF_LIST_DATA,
                            String.valueOf(ctx.IDENTIFIER().getText()),
                            ListenerErrorLocation.EXIT));
        }
    }

    /**
     * Validates the cardinality of leaf-list sub-statements as per grammar.
     *
     * @param ctx context object of the grammar rule.
     * @return true/false validation success or failure.
     */
    public static boolean validateSubStatementsCardinality(GeneratedYangParser
            .LeafListStatementContext ctx) {

        if (ctx.typeStatement().isEmpty()
                || (ctx.typeStatement().size() != YangUtilsParserManager.SUB_STATEMENT_CARDINALITY)) {
            yangConstruct = ParsableDataType.TYPE_DATA;
            return false;
        }

        if ((!ctx.unitsStatement().isEmpty())
                && (ctx.unitsStatement().size() != YangUtilsParserManager.SUB_STATEMENT_CARDINALITY)) {
            yangConstruct = ParsableDataType.UNITS_DATA;
            return false;
        }

        if ((!ctx.configStatement().isEmpty())
                && (ctx.configStatement().size() != YangUtilsParserManager.SUB_STATEMENT_CARDINALITY)) {
            yangConstruct = ParsableDataType.CONFIG_DATA;
            return false;
        }

        if ((!ctx.maxElementsStatement().isEmpty())
                && (ctx.maxElementsStatement().size() != YangUtilsParserManager.SUB_STATEMENT_CARDINALITY)) {
            yangConstruct = ParsableDataType.MAX_ELEMENT_DATA;
            return false;
        }

        if ((!ctx.minElementsStatement().isEmpty())
                && (ctx.minElementsStatement().size() != YangUtilsParserManager.SUB_STATEMENT_CARDINALITY)) {
            yangConstruct = ParsableDataType.MIN_ELEMENT_DATA;
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
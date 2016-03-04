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
import org.onosproject.yangutils.parser.antlrgencode.GeneratedYangParser;
import org.onosproject.yangutils.parser.exceptions.ParserException;
import org.onosproject.yangutils.parser.impl.TreeWalkListener;

import static org.onosproject.yangutils.parser.impl.parserutils.ListenerUtil.getValidIdentifier;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerCollisionDetector.detectCollidingChildUtil;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerErrorLocation.ENTRY;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerErrorLocation.EXIT;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerErrorMessageConstruction.constructListenerErrorMessage;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerErrorType.INVALID_HOLDER;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerErrorType.MISSING_CURRENT_HOLDER;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerErrorType.MISSING_HOLDER;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerValidation.checkStackIsNotEmpty;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerValidation.validateCardinality;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerValidation.validateCardinalityEqualsOne;
import static org.onosproject.yangutils.utils.YangConstructType.CONFIG_DATA;
import static org.onosproject.yangutils.utils.YangConstructType.DESCRIPTION_DATA;
import static org.onosproject.yangutils.utils.YangConstructType.LEAF_LIST_DATA;
import static org.onosproject.yangutils.utils.YangConstructType.MAX_ELEMENT_DATA;
import static org.onosproject.yangutils.utils.YangConstructType.MIN_ELEMENT_DATA;
import static org.onosproject.yangutils.utils.YangConstructType.REFERENCE_DATA;
import static org.onosproject.yangutils.utils.YangConstructType.STATUS_DATA;
import static org.onosproject.yangutils.utils.YangConstructType.TYPE_DATA;
import static org.onosproject.yangutils.utils.YangConstructType.UNITS_DATA;

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
 *  leafListStatement : LEAF_LIST_KEYWORD identifier LEFT_CURLY_BRACE (whenStatement | ifFeatureStatement |
 *  typeStatement | unitsStatement | mustStatement | configStatement | minElementsStatement | maxElementsStatement |
 *  orderedByStatement | statusStatement | descriptionStatement | referenceStatement)* RIGHT_CURLY_BRACE;
 */

/**
 * Implements listener based call back function corresponding to the "leaf-list"
 * rule defined in ANTLR grammar file for corresponding ABNF rule in RFC 6020.
 */
public final class LeafListListener {

    /**
     * Creates a new leaf list listener.
     */
    private LeafListListener() {
    }

    /**
     * It is called when parser receives an input matching the grammar rule
     * (leaf-list), performs validation and updates the data model tree.
     *
     * @param listener listener's object
     * @param ctx context object of the grammar rule
     */
    public static void processLeafListEntry(TreeWalkListener listener,
            GeneratedYangParser.LeafListStatementContext ctx) {

        // Check for stack to be non empty.
        checkStackIsNotEmpty(listener, MISSING_HOLDER, LEAF_LIST_DATA, ctx.identifier().getText(), ENTRY);

        String identifier = getValidIdentifier(ctx.identifier().getText(), LEAF_LIST_DATA, ctx);

        // Validate sub statement cardinality.
        validateSubStatementsCardinality(ctx);

        // Check for identifier collision
        int line = ctx.getStart().getLine();
        int charPositionInLine = ctx.getStart().getCharPositionInLine();
        detectCollidingChildUtil(listener, line, charPositionInLine, identifier, LEAF_LIST_DATA);

        YangLeafList leafList = new YangLeafList();
        leafList.setLeafName(identifier);

        Parsable tmpData = listener.getParsedDataStack().peek();
        YangLeavesHolder leaves;

        if (tmpData instanceof YangLeavesHolder) {
            leaves = (YangLeavesHolder) tmpData;
            leaves.addLeafList(leafList);
        } else {
            throw new ParserException(constructListenerErrorMessage(INVALID_HOLDER, LEAF_LIST_DATA,
                            ctx.identifier().getText(), ENTRY));
        }
        listener.getParsedDataStack().push(leafList);
    }

    /**
     * It is called when parser exits from grammar rule (leaf-list), it performs
     * validation and updates the data model tree.
     *
     * @param listener listener's object
     * @param ctx context object of the grammar rule
     */
    public static void processLeafListExit(TreeWalkListener listener,
            GeneratedYangParser.LeafListStatementContext ctx) {

        // Check for stack to be non empty.
        checkStackIsNotEmpty(listener, MISSING_HOLDER, LEAF_LIST_DATA, ctx.identifier().getText(), EXIT);

        if (listener.getParsedDataStack().peek() instanceof YangLeafList) {
            listener.getParsedDataStack().pop();
        } else {
            throw new ParserException(constructListenerErrorMessage(MISSING_CURRENT_HOLDER, LEAF_LIST_DATA,
                    ctx.identifier().getText(), EXIT));
        }
    }

    /**
     * Validates the cardinality of leaf-list sub-statements as per grammar.
     *
     * @param ctx context object of the grammar rule
     */
    private static void validateSubStatementsCardinality(GeneratedYangParser.LeafListStatementContext ctx) {

        validateCardinalityEqualsOne(ctx.typeStatement(), TYPE_DATA, LEAF_LIST_DATA, ctx.identifier().getText());
        validateCardinality(ctx.unitsStatement(), UNITS_DATA, LEAF_LIST_DATA, ctx.identifier().getText());
        validateCardinality(ctx.configStatement(), CONFIG_DATA, LEAF_LIST_DATA, ctx.identifier().getText());
        validateCardinality(ctx.maxElementsStatement(), MAX_ELEMENT_DATA, LEAF_LIST_DATA, ctx.identifier().getText());
        validateCardinality(ctx.minElementsStatement(), MIN_ELEMENT_DATA, LEAF_LIST_DATA, ctx.identifier().getText());
        validateCardinality(ctx.descriptionStatement(), DESCRIPTION_DATA, LEAF_LIST_DATA, ctx.identifier().getText());
        validateCardinality(ctx.referenceStatement(), REFERENCE_DATA, LEAF_LIST_DATA, ctx.identifier().getText());
        validateCardinality(ctx.statusStatement(), STATUS_DATA, LEAF_LIST_DATA, ctx.identifier().getText());
        //TODO ordered by
    }
}

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

/**
 * Implements listener based call back function corresponding to the "leaf"
 * rule defined in ANTLR grammar file for corresponding ABNF rule in RFC 6020.
 */
package org.onosproject.yangutils.parser.impl.listeners;

import org.onosproject.yangutils.datamodel.YangLeaf;
import org.onosproject.yangutils.datamodel.YangLeavesHolder;
import org.onosproject.yangutils.datamodel.utils.Parsable;
import org.onosproject.yangutils.parser.antlrgencode.GeneratedYangParser;
import org.onosproject.yangutils.parser.exceptions.ParserException;
import org.onosproject.yangutils.parser.impl.TreeWalkListener;

import static org.onosproject.yangutils.datamodel.utils.GeneratedLanguage.JAVA_GENERATION;
import static org.onosproject.yangutils.datamodel.utils.YangConstructType.CONFIG_DATA;
import static org.onosproject.yangutils.datamodel.utils.YangConstructType.DESCRIPTION_DATA;
import static org.onosproject.yangutils.datamodel.utils.YangConstructType.LEAF_DATA;
import static org.onosproject.yangutils.datamodel.utils.YangConstructType.MANDATORY_DATA;
import static org.onosproject.yangutils.datamodel.utils.YangConstructType.REFERENCE_DATA;
import static org.onosproject.yangutils.datamodel.utils.YangConstructType.STATUS_DATA;
import static org.onosproject.yangutils.datamodel.utils.YangConstructType.TYPE_DATA;
import static org.onosproject.yangutils.datamodel.utils.YangConstructType.UNITS_DATA;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerCollisionDetector.detectCollidingChildUtil;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerErrorLocation.ENTRY;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerErrorLocation.EXIT;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerErrorMessageConstruction
        .constructListenerErrorMessage;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerErrorType.INVALID_HOLDER;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerErrorType.MISSING_CURRENT_HOLDER;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerErrorType.MISSING_HOLDER;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerUtil.getValidIdentifier;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerValidation.checkStackIsNotEmpty;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerValidation.validateCardinalityEqualsOne;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerValidation.validateCardinalityMaxOne;
import static org.onosproject.yangutils.translator.tojava.YangDataModelFactory.getYangLeaf;

/*
 * Reference: RFC6020 and YANG ANTLR Grammar
 *
 * ABNF grammar as per RFC6020
 *  leaf-stmt           = leaf-keyword sep identifier-arg-str optsep
 *                        "{" stmtsep
 *                            ;; these stmts can appear in any order
 *                            [when-stmt stmtsep]
 *                            *(if-feature-stmt stmtsep)
 *                            type-stmt stmtsep
 *                            [units-stmt stmtsep]
 *                            *(must-stmt stmtsep)
 *                            [default-stmt stmtsep]
 *                            [config-stmt stmtsep]
 *                            [mandatory-stmt stmtsep]
 *                            [status-stmt stmtsep]
 *                            [description-stmt stmtsep]
 *                            [reference-stmt stmtsep]
 *                         "}"
 *
 * ANTLR grammar rule
 *  leafStatement : LEAF_KEYWORD identifier LEFT_CURLY_BRACE (whenStatement | ifFeatureStatement | typeStatement |
 *  unitsStatement | mustStatement | defaultStatement | configStatement | mandatoryStatement | statusStatement  |
 *  descriptionStatement | referenceStatement)* RIGHT_CURLY_BRACE;
 */

/**
 * Represents listener based call back function corresponding to the "leaf" rule
 * defined in ANTLR grammar file for corresponding ABNF rule in RFC 6020.
 */
public final class LeafListener {

    /**
     * Creates a new leaf listener.
     */
    private LeafListener() {
    }

    /**
     * It is called when parser receives an input matching the grammar rule
     * (leaf), performs validation and updates the data model tree.
     *
     * @param listener listener's object
     * @param ctx context object of the grammar rule
     */
    public static void processLeafEntry(TreeWalkListener listener,
            GeneratedYangParser.LeafStatementContext ctx) {

        // Check for stack to be non empty.
        checkStackIsNotEmpty(listener, MISSING_HOLDER, LEAF_DATA, ctx.identifier().getText(), ENTRY);

        String identifier = getValidIdentifier(ctx.identifier().getText(), LEAF_DATA, ctx);

        // Validate sub statement cardinality.
        validateSubStatementsCardinality(ctx);

        // Check for identifier collision
        int line = ctx.getStart().getLine();
        int charPositionInLine = ctx.getStart().getCharPositionInLine();
        detectCollidingChildUtil(listener, line, charPositionInLine, identifier, LEAF_DATA);

        YangLeaf leaf = getYangLeaf(JAVA_GENERATION);
        leaf.setLeafName(identifier);

        Parsable tmpData = listener.getParsedDataStack().peek();
        YangLeavesHolder leavesHolder;

        if (tmpData instanceof YangLeavesHolder) {
            leavesHolder = (YangLeavesHolder) tmpData;
            leavesHolder.addLeaf(leaf);
            leaf.setContainedIn(leavesHolder);
        } else {
            throw new ParserException(constructListenerErrorMessage(INVALID_HOLDER, LEAF_DATA,
                    ctx.identifier().getText(), ENTRY));
        }

        listener.getParsedDataStack().push(leaf);
    }

    /**
     * It is called when parser exits from grammar rule (leaf), performs
     * validation and updates the data model tree.
     *
     * @param listener listener's object
     * @param ctx context object of the grammar rule
     */
    public static void processLeafExit(TreeWalkListener listener,
            GeneratedYangParser.LeafStatementContext ctx) {

        // Check for stack to be non empty.
        checkStackIsNotEmpty(listener, MISSING_HOLDER, LEAF_DATA, ctx.identifier().getText(), EXIT);

        if (listener.getParsedDataStack().peek() instanceof YangLeaf) {
            listener.getParsedDataStack().pop();
        } else {
            throw new ParserException(constructListenerErrorMessage(MISSING_CURRENT_HOLDER, LEAF_DATA,
                    ctx.identifier().getText(), EXIT));
        }
    }

    /**
     * Validates the cardinality of leaf sub-statements as per grammar.
     *
     * @param ctx context object of the grammar rule
     */
    private static void validateSubStatementsCardinality(GeneratedYangParser.LeafStatementContext ctx) {

        validateCardinalityEqualsOne(ctx.typeStatement(), TYPE_DATA, LEAF_DATA, ctx.identifier().getText(), ctx);
        validateCardinalityMaxOne(ctx.unitsStatement(), UNITS_DATA, LEAF_DATA, ctx.identifier().getText());
        validateCardinalityMaxOne(ctx.configStatement(), CONFIG_DATA, LEAF_DATA, ctx.identifier().getText());
        validateCardinalityMaxOne(ctx.mandatoryStatement(), MANDATORY_DATA, LEAF_DATA, ctx.identifier().getText());
        validateCardinalityMaxOne(ctx.descriptionStatement(), DESCRIPTION_DATA, LEAF_DATA, ctx.identifier().getText());
        validateCardinalityMaxOne(ctx.referenceStatement(), REFERENCE_DATA, LEAF_DATA, ctx.identifier().getText());
        validateCardinalityMaxOne(ctx.statusStatement(), STATUS_DATA, LEAF_DATA, ctx.identifier().getText());
        //TODO when.
    }
}

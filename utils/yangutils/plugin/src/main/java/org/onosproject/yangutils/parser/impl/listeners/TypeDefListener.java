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

import org.onosproject.yangutils.datamodel.YangContainer;
import org.onosproject.yangutils.datamodel.YangGrouping;
import org.onosproject.yangutils.datamodel.YangInput;
import org.onosproject.yangutils.datamodel.YangList;
import org.onosproject.yangutils.datamodel.YangModule;
import org.onosproject.yangutils.datamodel.YangNode;
import org.onosproject.yangutils.datamodel.YangNotification;
import org.onosproject.yangutils.datamodel.YangOutput;
import org.onosproject.yangutils.datamodel.YangRpc;
import org.onosproject.yangutils.datamodel.YangSubModule;
import org.onosproject.yangutils.datamodel.YangTypeDef;
import org.onosproject.yangutils.datamodel.exceptions.DataModelException;
import org.onosproject.yangutils.datamodel.utils.Parsable;
import org.onosproject.yangutils.parser.antlrgencode.GeneratedYangParser;
import org.onosproject.yangutils.parser.exceptions.ParserException;
import org.onosproject.yangutils.parser.impl.TreeWalkListener;

import static org.onosproject.yangutils.datamodel.utils.GeneratedLanguage.JAVA_GENERATION;
import static org.onosproject.yangutils.datamodel.utils.YangConstructType.DEFAULT_DATA;
import static org.onosproject.yangutils.datamodel.utils.YangConstructType.DESCRIPTION_DATA;
import static org.onosproject.yangutils.datamodel.utils.YangConstructType.REFERENCE_DATA;
import static org.onosproject.yangutils.datamodel.utils.YangConstructType.STATUS_DATA;
import static org.onosproject.yangutils.datamodel.utils.YangConstructType.TYPEDEF_DATA;
import static org.onosproject.yangutils.datamodel.utils.YangConstructType.TYPE_DATA;
import static org.onosproject.yangutils.datamodel.utils.YangConstructType.UNITS_DATA;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerCollisionDetector.detectCollidingChildUtil;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerErrorLocation.ENTRY;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerErrorLocation.EXIT;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerErrorMessageConstruction.constructExtendedListenerErrorMessage;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerErrorMessageConstruction.constructListenerErrorMessage;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerErrorType.INVALID_CONTENT;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerErrorType.INVALID_HOLDER;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerErrorType.MISSING_CURRENT_HOLDER;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerErrorType.MISSING_HOLDER;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerErrorType.UNHANDLED_PARSED_DATA;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerUtil.getValidIdentifier;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerValidation.checkStackIsNotEmpty;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerValidation.validateCardinalityEqualsOne;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerValidation.validateCardinalityMaxOne;
import static org.onosproject.yangutils.translator.tojava.YangDataModelFactory.getYangTypeDefNode;

/*
 * Reference: RFC6020 and YANG ANTLR Grammar
 *
 * body-stmts          = *((extension-stmt /
 *                          feature-stmt /
 *                          identity-stmt /
 *                          typedef-stmt /
 *                          grouping-stmt /
 *                          data-def-stmt /
 *                          augment-stmt /
 *                          rpc-stmt /
 *                          notification-stmt /
 *                          deviation-stmt) stmtsep)
 *
 * typedef-stmt        = typedef-keyword sep identifier-arg-str optsep
 *                       "{" stmtsep
 *                           ;; these stmts can appear in any order
 *                           type-stmt stmtsep
 *                          [units-stmt stmtsep]
 *                           [default-stmt stmtsep]
 *                           [status-stmt stmtsep]
 *                           [description-stmt stmtsep]
 *                           [reference-stmt stmtsep]
 *                         "}"
 *
 * ANTLR grammar rule
 * typedefStatement : TYPEDEF_KEYWORD identifier LEFT_CURLY_BRACE
 *                (typeStatement | unitsStatement | defaultStatement | statusStatement
 *                | descriptionStatement | referenceStatement)* RIGHT_CURLY_BRACE;
 */

/**
 * Represents listener based call back function corresponding to the "typedef"
 * rule defined in ANTLR grammar file for corresponding ABNF rule in RFC 6020.
 */
public final class TypeDefListener {

    /**
     * Creates a new typedef listener.
     */
    private TypeDefListener() {
    }

    /**
     * It is called when parser enters grammar rule (typedef), it perform
     * validations and updates the data model tree.
     *
     * @param listener listener's object
     * @param ctx context object of the grammar rule
     */
    public static void processTypeDefEntry(TreeWalkListener listener,
            GeneratedYangParser.TypedefStatementContext ctx) {

        // Check for stack to be non empty.
        checkStackIsNotEmpty(listener, MISSING_HOLDER, TYPEDEF_DATA, ctx.identifier().getText(), ENTRY);

        String identifier = getValidIdentifier(ctx.identifier().getText(), TYPEDEF_DATA, ctx);

        // Validate sub statement cardinality.
        validateSubStatementsCardinality(ctx);

        // Check for identifier collision
        int line = ctx.getStart().getLine();
        int charPositionInLine = ctx.getStart().getCharPositionInLine();
        detectCollidingChildUtil(listener, line, charPositionInLine, identifier, TYPEDEF_DATA);

        /*
         * Create a derived type information, the base type must be set in type
         * listener.
         */
        YangTypeDef typeDefNode = getYangTypeDefNode(JAVA_GENERATION);
        typeDefNode.setName(identifier);

        Parsable curData = listener.getParsedDataStack().peek();

        if (curData instanceof YangModule || curData instanceof YangSubModule || curData instanceof YangContainer
                || curData instanceof YangList || curData instanceof YangNotification || curData instanceof YangRpc
                || curData instanceof YangInput || curData instanceof YangOutput || curData instanceof YangGrouping) {

            YangNode curNode = (YangNode) curData;
            try {
                curNode.addChild(typeDefNode);
            } catch (DataModelException e) {
                throw new ParserException(constructExtendedListenerErrorMessage(UNHANDLED_PARSED_DATA,
                        TYPEDEF_DATA, ctx.identifier().getText(), ENTRY, e.getMessage()));
            }
            listener.getParsedDataStack().push(typeDefNode);
        } else {
            throw new ParserException(constructListenerErrorMessage(INVALID_HOLDER,
                    TYPEDEF_DATA, ctx.identifier().getText(), ENTRY));
        }
    }

    /**
     * It is called when parser exits from grammar rule (typedef), it perform
     * validations and updates the data model tree.
     *
     * @param listener listener's object
     * @param ctx context object of the grammar rule
     */
    public static void processTypeDefExit(TreeWalkListener listener,
            GeneratedYangParser.TypedefStatementContext ctx) {

        // Check for stack to be non empty.
        checkStackIsNotEmpty(listener, MISSING_HOLDER, TYPEDEF_DATA, ctx.identifier().getText(), EXIT);

        if (listener.getParsedDataStack().peek() instanceof YangTypeDef) {
            YangTypeDef typeDefNode = (YangTypeDef) listener.getParsedDataStack().peek();
            try {
                typeDefNode.validateDataOnExit();
            } catch (DataModelException e) {
                throw new ParserException(constructListenerErrorMessage(INVALID_CONTENT, TYPEDEF_DATA,
                        ctx.identifier().getText(), EXIT));
            }

            listener.getParsedDataStack().pop();
        } else {
            throw new ParserException(constructListenerErrorMessage(MISSING_CURRENT_HOLDER, TYPEDEF_DATA,
                    ctx.identifier().getText(), EXIT));
        }
    }

    /**
     * Validates the cardinality of typedef sub-statements as per grammar.
     *
     * @param ctx context object of the grammar rule
     */
    private static void validateSubStatementsCardinality(GeneratedYangParser.TypedefStatementContext ctx) {

        validateCardinalityMaxOne(ctx.unitsStatement(), UNITS_DATA, TYPEDEF_DATA, ctx.identifier().getText());
        validateCardinalityMaxOne(ctx.defaultStatement(), DEFAULT_DATA, TYPEDEF_DATA, ctx.identifier().getText());
        validateCardinalityEqualsOne(ctx.typeStatement(), TYPE_DATA, TYPEDEF_DATA, ctx.identifier().getText(), ctx);
        validateCardinalityMaxOne(ctx.descriptionStatement(), DESCRIPTION_DATA, TYPEDEF_DATA,
                ctx.identifier().getText());
        validateCardinalityMaxOne(ctx.referenceStatement(), REFERENCE_DATA, TYPEDEF_DATA, ctx.identifier().getText());
        validateCardinalityMaxOne(ctx.statusStatement(), STATUS_DATA, TYPEDEF_DATA, ctx.identifier().getText());
    }
}

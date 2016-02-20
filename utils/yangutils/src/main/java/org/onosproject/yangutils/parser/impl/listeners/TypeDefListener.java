/*
 * Copyright 2014-2016 Open Networking Laboratory
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
 * typedefStatement : TYPEDEF_KEYWORD IDENTIFIER LEFT_CURLY_BRACE
 *                (typeStatement | unitsStatement | defaultStatement | statusStatement
 *                | descriptionStatement | referenceStatement)* RIGHT_CURLY_BRACE;
 */

import org.onosproject.yangutils.datamodel.YangContainer;
import org.onosproject.yangutils.datamodel.YangList;
import org.onosproject.yangutils.datamodel.YangModule;
import org.onosproject.yangutils.datamodel.YangNode;
import org.onosproject.yangutils.datamodel.YangSubModule;
import org.onosproject.yangutils.datamodel.YangTypeDef;
import org.onosproject.yangutils.datamodel.exceptions.DataModelException;
import org.onosproject.yangutils.parser.Parsable;
import org.onosproject.yangutils.parser.ParsableDataType;
import static org.onosproject.yangutils.parser.ParsableDataType.TYPEDEF_DATA;
import static org.onosproject.yangutils.parser.ParsableDataType.UNITS_DATA;
import static org.onosproject.yangutils.parser.ParsableDataType.DEFAULT_DATA;
import static org.onosproject.yangutils.parser.ParsableDataType.TYPE_DATA;
import static org.onosproject.yangutils.parser.ParsableDataType.DESCRIPTION_DATA;
import static org.onosproject.yangutils.parser.ParsableDataType.REFERENCE_DATA;
import static org.onosproject.yangutils.parser.ParsableDataType.STATUS_DATA;
import org.onosproject.yangutils.parser.antlrgencode.GeneratedYangParser;
import org.onosproject.yangutils.parser.exceptions.ParserException;
import org.onosproject.yangutils.parser.impl.TreeWalkListener;
import org.onosproject.yangutils.parser.impl.YangUtilsParserManager;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerErrorLocation.ENTRY;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerErrorLocation.EXIT;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerErrorMessageConstruction.constructExtendedListenerErrorMessage;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerErrorMessageConstruction.constructListenerErrorMessage;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerErrorType.MISSING_HOLDER;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerErrorType.INVALID_CARDINALITY;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerErrorType.INVALID_HOLDER;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerErrorType.UNHANDLED_PARSED_DATA;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerErrorType.MISSING_CURRENT_HOLDER;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerValidation.checkStackIsNotEmpty;

/**
 * Implements listener based call back function corresponding to the "typedef"
 * rule defined in ANTLR grammar file for corresponding ABNF rule in RFC 6020.
 */
public final class TypeDefListener {

    private static ParsableDataType yangConstruct;

    /**
     * Creates a new typedef listener.
     */
    private TypeDefListener() {
    }

    /**
     * It is called when parser enters grammar rule (typedef), it perform
     * validations and updates the data model tree.
     *
     * @param listener listener's object.
     * @param ctx context object of the grammar rule.
     */
    public static void processTypeDefEntry(TreeWalkListener listener,
            GeneratedYangParser.TypedefStatementContext ctx) {

        // Check for stack to be non empty.
        checkStackIsNotEmpty(listener, MISSING_HOLDER, TYPEDEF_DATA, ctx.IDENTIFIER().getText(), ENTRY);

        boolean result = validateSubStatementsCardinality(ctx);
        if (!result) {
            throw new ParserException(constructListenerErrorMessage(INVALID_CARDINALITY, yangConstruct, "", ENTRY));
        }

        YangTypeDef typeDefNode = new YangTypeDef();
        typeDefNode.setDerivedName(ctx.IDENTIFIER().getText());

        Parsable curData = listener.getParsedDataStack().peek();

        if (curData instanceof YangModule | curData instanceof YangSubModule | curData instanceof YangContainer
                | curData instanceof YangList) {
            /*
             * TODO YangGrouping, YangRpc, YangInput, YangOutput, Notification.
             */
            YangNode curNode = (YangNode) curData;
            try {
                curNode.addChild(typeDefNode);
            } catch (DataModelException e) {
                throw new ParserException(constructExtendedListenerErrorMessage(UNHANDLED_PARSED_DATA,
                        TYPEDEF_DATA, ctx.IDENTIFIER().getText(), ENTRY, e.getMessage()));
            }
            listener.getParsedDataStack().push(typeDefNode);
        } else {
            throw new ParserException(constructListenerErrorMessage(INVALID_HOLDER,
                    TYPEDEF_DATA, ctx.IDENTIFIER().getText(), ENTRY));
        }
    }

    /**
     * It is called when parser exits from grammar rule (typedef), it perform
     * validations and updates the data model tree.
     *
     * @param listener listener's object.
     * @param ctx context object of the grammar rule.
     */
    public static void processTypeDefExit(TreeWalkListener listener,
            GeneratedYangParser.TypedefStatementContext ctx) {

        // Check for stack to be non empty.
        checkStackIsNotEmpty(listener, MISSING_HOLDER, TYPEDEF_DATA, ctx.IDENTIFIER().getText(), EXIT);

        if (listener.getParsedDataStack().peek() instanceof YangTypeDef) {
            listener.getParsedDataStack().pop();
        } else {
            listener.getErrorInformation().setErrorFlag(true);
            throw new ParserException(constructListenerErrorMessage(MISSING_CURRENT_HOLDER, TYPEDEF_DATA,
                    ctx.IDENTIFIER().getText(), EXIT));
        }
    }

    /**
     * Validates the cardinality of typedef sub-statements as per grammar.
     *
     * @param ctx context object of the grammar rule.
     * @return true/false validation success or failure.
     */
    private static boolean validateSubStatementsCardinality(GeneratedYangParser.TypedefStatementContext ctx) {

        if ((!ctx.unitsStatement().isEmpty())
                && (ctx.unitsStatement().size() != YangUtilsParserManager.SUB_STATEMENT_CARDINALITY)) {
            yangConstruct = UNITS_DATA;
            return false;
        }

        if ((!ctx.defaultStatement().isEmpty())
                && (ctx.defaultStatement().size() != YangUtilsParserManager.SUB_STATEMENT_CARDINALITY)) {
            yangConstruct = DEFAULT_DATA;
            return false;
        }

        if (ctx.typeStatement().size() != YangUtilsParserManager.SUB_STATEMENT_CARDINALITY) {
            yangConstruct = TYPE_DATA;
            return false;
        }

        if ((!ctx.descriptionStatement().isEmpty())
                && (ctx.descriptionStatement().size() != YangUtilsParserManager.SUB_STATEMENT_CARDINALITY)) {
            yangConstruct = DESCRIPTION_DATA;
            return false;
        }

        if ((!ctx.referenceStatement().isEmpty())
                && (ctx.referenceStatement().size() != YangUtilsParserManager.SUB_STATEMENT_CARDINALITY)) {
            yangConstruct = REFERENCE_DATA;
            return false;
        }

        if ((!ctx.statusStatement().isEmpty())
                && (ctx.statusStatement().size() != YangUtilsParserManager.SUB_STATEMENT_CARDINALITY)) {
            yangConstruct = STATUS_DATA;
            return false;
        }
        return true;
    }
}

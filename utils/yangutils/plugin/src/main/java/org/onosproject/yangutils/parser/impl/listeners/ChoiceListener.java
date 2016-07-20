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

import org.onosproject.yangutils.datamodel.YangAugment;
import org.onosproject.yangutils.datamodel.YangCase;
import org.onosproject.yangutils.datamodel.YangChoice;
import org.onosproject.yangutils.datamodel.YangContainer;
import org.onosproject.yangutils.datamodel.YangGrouping;
import org.onosproject.yangutils.datamodel.YangInput;
import org.onosproject.yangutils.datamodel.YangList;
import org.onosproject.yangutils.datamodel.YangModule;
import org.onosproject.yangutils.datamodel.YangNode;
import org.onosproject.yangutils.datamodel.YangNotification;
import org.onosproject.yangutils.datamodel.YangOutput;
import org.onosproject.yangutils.datamodel.YangSubModule;
import org.onosproject.yangutils.datamodel.exceptions.DataModelException;
import org.onosproject.yangutils.datamodel.utils.Parsable;
import org.onosproject.yangutils.parser.antlrgencode.GeneratedYangParser;
import org.onosproject.yangutils.parser.exceptions.ParserException;
import org.onosproject.yangutils.parser.impl.TreeWalkListener;

import static org.onosproject.yangutils.datamodel.utils.GeneratedLanguage.JAVA_GENERATION;
import static org.onosproject.yangutils.datamodel.utils.YangConstructType.CASE_DATA;
import static org.onosproject.yangutils.datamodel.utils.YangConstructType.CHOICE_DATA;
import static org.onosproject.yangutils.datamodel.utils.YangConstructType.CONFIG_DATA;
import static org.onosproject.yangutils.datamodel.utils.YangConstructType.DEFAULT_DATA;
import static org.onosproject.yangutils.datamodel.utils.YangConstructType.DESCRIPTION_DATA;
import static org.onosproject.yangutils.datamodel.utils.YangConstructType.MANDATORY_DATA;
import static org.onosproject.yangutils.datamodel.utils.YangConstructType.REFERENCE_DATA;
import static org.onosproject.yangutils.datamodel.utils.YangConstructType.SHORT_CASE_DATA;
import static org.onosproject.yangutils.datamodel.utils.YangConstructType.STATUS_DATA;
import static org.onosproject.yangutils.datamodel.utils.YangConstructType.WHEN_DATA;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerCollisionDetector.detectCollidingChildUtil;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerErrorLocation.ENTRY;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerErrorLocation.EXIT;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerErrorMessageConstruction.constructExtendedListenerErrorMessage;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerErrorMessageConstruction.constructListenerErrorMessage;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerErrorType.INVALID_HOLDER;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerErrorType.MISSING_CURRENT_HOLDER;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerErrorType.MISSING_HOLDER;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerErrorType.UNHANDLED_PARSED_DATA;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerUtil.getValidIdentifier;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerValidation.checkStackIsNotEmpty;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerValidation.validateCardinalityMaxOne;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerValidation.validateMutuallyExclusiveChilds;
import static org.onosproject.yangutils.translator.tojava.YangDataModelFactory.getYangChoiceNode;

/*
 * Reference: RFC6020 and YANG ANTLR Grammar
 *
 * ABNF grammar as per RFC6020
 *  choice-stmt         = choice-keyword sep identifier-arg-str optsep
 *                        (";" /
 *                         "{" stmtsep
 *                             ;; these stmts can appear in any order
 *                             [when-stmt stmtsep]
 *                             *(if-feature-stmt stmtsep)
 *                             [default-stmt stmtsep]
 *                             [config-stmt stmtsep]
 *                             [mandatory-stmt stmtsep]
 *                             [status-stmt stmtsep]
 *                             [description-stmt stmtsep]
 *                             [reference-stmt stmtsep]
 *                             *((short-case-stmt / case-stmt) stmtsep)
 *                         "}")
 *
 * ANTLR grammar rule
 * choiceStatement : CHOICE_KEYWORD identifier (STMTEND | LEFT_CURLY_BRACE (whenStatement | ifFeatureStatement
 *                 | defaultStatement | configStatement | mandatoryStatement | statusStatement | descriptionStatement
 *                 | referenceStatement | shortCaseStatement | caseStatement)* RIGHT_CURLY_BRACE);
 */

/**
 * Represents listener based call back function corresponding to the "choice"
 * rule defined in ANTLR grammar file for corresponding ABNF rule in RFC 6020.
 */
public final class ChoiceListener {

    /**
     * Create a new choice listener.
     */
    private ChoiceListener() {
    }

    /**
     * It is called when parser receives an input matching the grammar rule
     * (choice), perform validations and update the data model tree.
     *
     * @param listener Listener's object
     * @param ctx context object of the grammar rule
     */
    public static void processChoiceEntry(TreeWalkListener listener,
            GeneratedYangParser.ChoiceStatementContext ctx) {

        // Check for stack to be non empty.
        checkStackIsNotEmpty(listener, MISSING_HOLDER, CHOICE_DATA, ctx.identifier().getText(), ENTRY);

        // Check validity of identifier and remove double quotes.
        String identifier = getValidIdentifier(ctx.identifier().getText(), CHOICE_DATA, ctx);

        // Validate sub statement cardinality.
        validateSubStatementsCardinality(ctx);

        Parsable curData = listener.getParsedDataStack().peek();

        // Check for identifier collision
        int line = ctx.getStart().getLine();
        int charPositionInLine = ctx.getStart().getCharPositionInLine();
        detectCollidingChildUtil(listener, line, charPositionInLine, identifier, CHOICE_DATA);

        if (curData instanceof YangModule || curData instanceof YangSubModule || curData instanceof YangContainer
                || curData instanceof YangList || curData instanceof YangCase || curData instanceof YangGrouping
                || curData instanceof YangAugment  || curData instanceof YangInput || curData instanceof YangOutput
                || curData instanceof YangNotification) {

            YangChoice choiceNode = getYangChoiceNode(JAVA_GENERATION);
            choiceNode.setName(identifier);

            YangNode curNode = (YangNode) curData;
            try {
                curNode.addChild(choiceNode);
            } catch (DataModelException e) {
                throw new ParserException(constructExtendedListenerErrorMessage(UNHANDLED_PARSED_DATA,
                        CHOICE_DATA, ctx.identifier().getText(), ENTRY, e.getMessage()));
            }
            listener.getParsedDataStack().push(choiceNode);
        } else {
            throw new ParserException(constructListenerErrorMessage(INVALID_HOLDER,
                    CHOICE_DATA, ctx.identifier().getText(), ENTRY));
        }
    }

    /**
     * It is called when parser exits from grammar rule (choice), it perform
     * validations and update the data model tree.
     *
     * @param listener Listener's object
     * @param ctx context object of the grammar rule
     */
    public static void processChoiceExit(TreeWalkListener listener,
            GeneratedYangParser.ChoiceStatementContext ctx) {

        // Check for stack to be non empty.
        checkStackIsNotEmpty(listener, MISSING_HOLDER, CHOICE_DATA, ctx.identifier().getText(), EXIT);

        if (listener.getParsedDataStack().peek() instanceof YangChoice) {
            listener.getParsedDataStack().pop();
        } else {
            throw new ParserException(constructListenerErrorMessage(MISSING_CURRENT_HOLDER, CHOICE_DATA,
                    ctx.identifier().getText(), EXIT));
        }
    }

    /**
     * Validates the cardinality of choice sub-statements as per grammar.
     *
     * @param ctx context object of the grammar rule.
     */
    private static void validateSubStatementsCardinality(GeneratedYangParser.ChoiceStatementContext ctx) {

        validateCardinalityMaxOne(ctx.whenStatement(), WHEN_DATA, CHOICE_DATA, ctx.identifier().getText());
        validateCardinalityMaxOne(ctx.defaultStatement(), DEFAULT_DATA, CHOICE_DATA, ctx.identifier().getText());
        validateCardinalityMaxOne(ctx.configStatement(), CONFIG_DATA, CHOICE_DATA, ctx.identifier().getText());
        validateCardinalityMaxOne(ctx.mandatoryStatement(), MANDATORY_DATA, CHOICE_DATA, ctx.identifier().getText());
        validateCardinalityMaxOne(ctx.statusStatement(), STATUS_DATA, CHOICE_DATA, ctx.identifier().getText());
        validateCardinalityMaxOne(ctx.descriptionStatement(), DESCRIPTION_DATA, CHOICE_DATA,
                ctx.identifier().getText());
        validateCardinalityMaxOne(ctx.referenceStatement(), REFERENCE_DATA, CHOICE_DATA, ctx.identifier().getText());
        validateMutuallyExclusiveChilds(ctx.shortCaseStatement(), SHORT_CASE_DATA, ctx.caseStatement(), CASE_DATA,
                CHOICE_DATA, ctx.identifier().getText());
    }
}

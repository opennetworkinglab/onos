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

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ParseTree;
import org.onosproject.yangutils.datamodel.YangCase;
import org.onosproject.yangutils.datamodel.YangChoice;
import org.onosproject.yangutils.datamodel.exceptions.DataModelException;
import org.onosproject.yangutils.parser.antlrgencode.GeneratedYangParser;
import org.onosproject.yangutils.parser.exceptions.ParserException;
import org.onosproject.yangutils.parser.impl.TreeWalkListener;

import static org.onosproject.yangutils.datamodel.utils.GeneratedLanguage.JAVA_GENERATION;
import static org.onosproject.yangutils.datamodel.utils.YangConstructType.CASE_DATA;
import static org.onosproject.yangutils.datamodel.utils.YangConstructType.SHORT_CASE_DATA;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerCollisionDetector.detectCollidingChildUtil;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerErrorLocation.ENTRY;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerErrorLocation.EXIT;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerErrorMessageConstruction.constructExtendedListenerErrorMessage;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerErrorMessageConstruction.constructListenerErrorMessage;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerErrorType.INVALID_CHILD;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerErrorType.INVALID_HOLDER;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerErrorType.MISSING_CURRENT_HOLDER;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerErrorType.MISSING_HOLDER;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerErrorType.UNHANDLED_PARSED_DATA;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerUtil.getValidIdentifier;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerValidation.checkStackIsNotEmpty;
import static org.onosproject.yangutils.translator.tojava.YangDataModelFactory.getYangCaseNode;

/*
 * Reference: RFC6020 and YANG ANTLR Grammar
 *
 * ABNF grammar as per RFC6020
 * short-case-stmt     = container-stmt /
 *                       leaf-stmt /
 *                       leaf-list-stmt /
 *                       list-stmt /
 *                       anyxml-stmt
 *
 * ANTLR grammar rule
 * shortCaseStatement : containerStatement | leafStatement | leafListStatement | listStatement;
 */

/**
 * Represents listener based call back function corresponding to the "short
 * case" rule defined in ANTLR grammar file for corresponding ABNF rule in RFC
 * 6020.
 */
public final class ShortCaseListener {

    /**
     * Create a new short case listener.
     */
    private ShortCaseListener() {
    }

    /**
     * It is called when parser enters grammar rule (short case), it perform
     * validations and updates the data model tree.
     *
     * @param listener listener's object
     * @param ctx context object of the grammar rule
     */
    public static void processShortCaseEntry(TreeWalkListener listener,
            GeneratedYangParser.ShortCaseStatementContext ctx) {

        ParseTree errorConstructContext;

        // Check for stack to be non empty.
        checkStackIsNotEmpty(listener, MISSING_HOLDER, SHORT_CASE_DATA, "", ENTRY);

        YangCase caseNode = getYangCaseNode(JAVA_GENERATION);

        if (ctx.containerStatement() != null) {
            caseNode.setName(getValidIdentifier(ctx.containerStatement().identifier().getText(), CASE_DATA, ctx));
            errorConstructContext = ctx.containerStatement();
        } else if (ctx.listStatement() != null) {
            caseNode.setName(getValidIdentifier(ctx.listStatement().identifier().getText(), CASE_DATA, ctx));
            errorConstructContext = ctx.listStatement();
        } else if (ctx.leafListStatement() != null) {
            caseNode.setName(getValidIdentifier(ctx.leafListStatement().identifier().getText(), CASE_DATA, ctx));
            errorConstructContext = ctx.leafListStatement();
        } else if (ctx.leafStatement() != null) {
            caseNode.setName(getValidIdentifier(ctx.leafStatement().identifier().getText(), CASE_DATA, ctx));
            errorConstructContext = ctx.leafStatement();
        } else {
            throw new ParserException(constructListenerErrorMessage(INVALID_CHILD, SHORT_CASE_DATA, "", ENTRY));
        }
        // TODO implement for augment.

        int line = ((ParserRuleContext) errorConstructContext).getStart().getLine();
        int charPositionInLine = ((ParserRuleContext) errorConstructContext).getStart().getCharPositionInLine();

        // Check for identifier collision
        detectCollidingChildUtil(listener, line, charPositionInLine, caseNode.getName(), CASE_DATA);

        if ((listener.getParsedDataStack().peek()) instanceof YangChoice) {
            try {
                ((YangChoice) listener.getParsedDataStack().peek()).addChild(caseNode);
            } catch (DataModelException e) {
                throw new ParserException(constructExtendedListenerErrorMessage(UNHANDLED_PARSED_DATA,
                        SHORT_CASE_DATA, caseNode.getName(), ENTRY, e.getMessage()));
            }
            listener.getParsedDataStack().push(caseNode);
        } else {
            throw new ParserException(constructListenerErrorMessage(INVALID_HOLDER, SHORT_CASE_DATA,
                    caseNode.getName(), ENTRY));
        }
    }

    /**
     * It is called when parser exits from grammar rule (short case), it perform
     * validations and update the data model tree.
     *
     * @param listener Listener's object
     * @param ctx context object of the grammar rule
     */
    public static void processShortCaseExit(TreeWalkListener listener,
            GeneratedYangParser.ShortCaseStatementContext ctx) {

        // Check for stack to be non empty.
        checkStackIsNotEmpty(listener, MISSING_HOLDER, SHORT_CASE_DATA, "", EXIT);

        if (listener.getParsedDataStack().peek() instanceof YangCase) {
            listener.getParsedDataStack().pop();
        } else {
            throw new ParserException(constructListenerErrorMessage(MISSING_CURRENT_HOLDER, SHORT_CASE_DATA,
                    "", EXIT));
        }
    }
}

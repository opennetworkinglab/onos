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
 * ABNF grammar as per RFC6020
 *  enum-stmt           = enum-keyword sep string optsep
 *                        (";" /
 *                         "{" stmtsep
 *                             ;; these stmts can appear in any order
 *                             [value-stmt stmtsep]
 *                             [status-stmt stmtsep]
 *                             [description-stmt stmtsep]
 *                             [reference-stmt stmtsep]
 *                          "}")
 *
 * ANTLR grammar rule
 * enumStatement : ENUM_KEYWORD string (STMTEND | LEFT_CURLY_BRACE enumStatementBody RIGHT_CURLY_BRACE);
 *
 *         enumStatementBody : valueStatement? statusStatement? descriptionStatement? referenceStatement?
 *         | valueStatement? statusStatement? referenceStatement? descriptionStatement?
 *         | valueStatement? descriptionStatement? statusStatement? referenceStatement?
 *         | valueStatement? descriptionStatement? referenceStatement? statusStatement?
 *         | valueStatement? referenceStatement? statusStatement? descriptionStatement?
 *         | valueStatement? referenceStatement? descriptionStatement? statusStatement?
 *         | statusStatement? valueStatement? descriptionStatement? referenceStatement?
 *         | statusStatement? valueStatement? referenceStatement? descriptionStatement?
 *         | statusStatement? descriptionStatement? descriptionStatement? valueStatement?
 *         | statusStatement? descriptionStatement? valueStatement? descriptionStatement?
 *         | statusStatement? referenceStatement? valueStatement? descriptionStatement?
 *         | statusStatement? referenceStatement? descriptionStatement? valueStatement?
 *         | descriptionStatement? valueStatement? statusStatement? referenceStatement?
 *         | descriptionStatement? valueStatement? referenceStatement? statusStatement?
 *         | descriptionStatement? statusStatement? valueStatement? referenceStatement?
 *         | descriptionStatement? statusStatement? referenceStatement? valueStatement?
 *         | descriptionStatement? referenceStatement? valueStatement? statusStatement?
 *         | descriptionStatement? referenceStatement? statusStatement? valueStatement?
 *         | referenceStatement? valueStatement? descriptionStatement? statusStatement?
 *         | referenceStatement? valueStatement? statusStatement? descriptionStatement?
 *         | referenceStatement? statusStatement? descriptionStatement? valueStatement?
 *         | referenceStatement? statusStatement? valueStatement? descriptionStatement?
 *         | referenceStatement? descriptionStatement? valueStatement? statusStatement?
 *         | referenceStatement? descriptionStatement? statusStatement? valueStatement?
 *         ;
 */

import org.onosproject.yangutils.datamodel.YangEnum;
import org.onosproject.yangutils.datamodel.YangEnumeration;
import org.onosproject.yangutils.datamodel.exceptions.DataModelException;
import org.onosproject.yangutils.parser.Parsable;
import static org.onosproject.yangutils.utils.YangConstructType.ENUM_DATA;
import org.onosproject.yangutils.parser.antlrgencode.GeneratedYangParser;
import org.onosproject.yangutils.parser.exceptions.ParserException;
import org.onosproject.yangutils.parser.impl.TreeWalkListener;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerErrorLocation.ENTRY;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerErrorLocation.EXIT;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerErrorMessageConstruction.constructExtendedListenerErrorMessage;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerErrorMessageConstruction.constructListenerErrorMessage;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerErrorType.DUPLICATE_ENTRY;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerErrorType.INVALID_HOLDER;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerErrorType.MISSING_CURRENT_HOLDER;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerErrorType.MISSING_HOLDER;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerValidation.checkStackIsNotEmpty;

/**
 * Implements listener based call back function corresponding to the "enum" rule
 * defined in ANTLR grammar file for corresponding ABNF rule in RFC 6020.
 */
public final class EnumListener {

    /**
     * Creates a new enum listener.
     */
    private EnumListener() {
    }

    /**
     * It is called when parser enters grammar rule (enum), it perform
     * validations and updates the data model tree.
     *
     * @param listener listener's object
     * @param ctx context object of the grammar rule
     */
    public static void processEnumEntry(TreeWalkListener listener, GeneratedYangParser.EnumStatementContext ctx) {

        // Check for stack to be non empty.
        checkStackIsNotEmpty(listener, MISSING_HOLDER, ENUM_DATA, ctx.string().getText(), ENTRY);

        YangEnum enumNode = new YangEnum();
        enumNode.setNamedValue(ctx.string().getText());
        listener.getParsedDataStack().push(enumNode);
    }

    /**
     * It is called when parser exits from grammar rule (enum), it perform
     * validations and update the data model tree.
     *
     * @param listener Listener's object
     * @param ctx context object of the grammar rule
     */
    public static void processEnumExit(TreeWalkListener listener, GeneratedYangParser.EnumStatementContext ctx) {

        // Check for stack to be non empty.
        checkStackIsNotEmpty(listener, MISSING_HOLDER, ENUM_DATA, ctx.string().getText(), EXIT);

        Parsable tmpEnumNode = listener.getParsedDataStack().peek();
        if (tmpEnumNode instanceof YangEnum) {
            listener.getParsedDataStack().pop();

            // Check for stack to be non empty.
            checkStackIsNotEmpty(listener, MISSING_HOLDER, ENUM_DATA, ctx.string().getText(), EXIT);

            Parsable tmpNode = listener.getParsedDataStack().peek();
            switch (tmpNode.getYangConstructType()) {
                case ENUMERATION_DATA: {
                    YangEnumeration yangEnumeration = (YangEnumeration) tmpNode;
                    if ((ctx.enumStatementBody() == null) || (ctx.enumStatementBody().valueStatement() == null)) {
                        int maxValue = 0;
                        boolean isValuePresent = false;

                        for (YangEnum curEnum : yangEnumeration.getEnumSet()) {
                            if (maxValue <= curEnum.getValue()) {
                                maxValue = curEnum.getValue();
                                isValuePresent = true;
                            }
                        }
                        if (isValuePresent) {
                            maxValue++;
                        }
                        ((YangEnum) tmpEnumNode).setValue(maxValue);
                    }
                    try {
                        yangEnumeration.addEnumInfo((YangEnum) tmpEnumNode);
                    } catch (DataModelException e) {
                        ParserException parserException = new ParserException(constructExtendedListenerErrorMessage(
                                DUPLICATE_ENTRY, ENUM_DATA, ctx.string().getText(), EXIT, e.getMessage()));
                        parserException.setLine(ctx.string().STRING(0).getSymbol().getLine());
                        parserException.setCharPosition(ctx.string().STRING(0).getSymbol().getCharPositionInLine());
                        throw parserException;
                    }
                    break;
                }
                default:
                    throw new ParserException(
                            constructListenerErrorMessage(INVALID_HOLDER, ENUM_DATA, ctx.string().getText(), EXIT));
            }
        } else {
            throw new ParserException(
                    constructListenerErrorMessage(MISSING_CURRENT_HOLDER, ENUM_DATA, ctx.string().getText(), EXIT));
        }
    }
}

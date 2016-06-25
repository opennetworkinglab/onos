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

import org.onosproject.yangutils.datamodel.YangDerivedInfo;
import org.onosproject.yangutils.datamodel.YangRangeRestriction;
import org.onosproject.yangutils.datamodel.YangType;
import org.onosproject.yangutils.datamodel.exceptions.DataModelException;
import org.onosproject.yangutils.datamodel.utils.Parsable;
import org.onosproject.yangutils.parser.antlrgencode.GeneratedYangParser;
import org.onosproject.yangutils.parser.exceptions.ParserException;
import org.onosproject.yangutils.parser.impl.TreeWalkListener;

import static org.onosproject.yangutils.datamodel.YangDataTypes.DERIVED;
import static org.onosproject.yangutils.datamodel.utils.RestrictionResolver.isOfRangeRestrictedType;
import static org.onosproject.yangutils.datamodel.utils.RestrictionResolver.processRangeRestriction;
import static org.onosproject.yangutils.datamodel.utils.YangConstructType.RANGE_DATA;
import static org.onosproject.yangutils.datamodel.utils.YangConstructType.TYPE_DATA;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerErrorLocation.ENTRY;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerErrorLocation.EXIT;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerErrorMessageConstruction.constructListenerErrorMessage;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerErrorType.INVALID_HOLDER;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerErrorType.MISSING_CURRENT_HOLDER;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerErrorType.MISSING_HOLDER;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerValidation.checkStackIsNotEmpty;

/*
 * Reference: RFC6020 and YANG ANTLR Grammar
 *
 * ABNF grammar as per RFC6020
 *  range-stmt          = range-keyword sep range-arg-str optsep
 *                        (";" /
 *                         "{" stmtsep
 *                             ;; these stmts can appear in any order
 *                             [error-message-stmt stmtsep]
 *                             [error-app-tag-stmt stmtsep]
 *                             [description-stmt stmtsep]
 *                             [reference-stmt stmtsep]
 *                          "}")
 *
 * ANTLR grammar rule
 *  rangeStatement : RANGE_KEYWORD range (STMTEND | LEFT_CURLY_BRACE commonStatements RIGHT_CURLY_BRACE);
 */

/**
 * Represents listener based call back function corresponding to the "range"
 * rule defined in ANTLR grammar file for corresponding ABNF rule in RFC 6020.
 */
public final class RangeRestrictionListener {

    /**
     * Creates a new range restriction listener.
     */
    private RangeRestrictionListener() {
    }

    /**
     * It is called when parser receives an input matching the grammar
     * rule (range), performs validation and updates the data model
     * tree.
     *
     * @param listener listener's object
     * @param ctx      context object of the grammar rule
     */
    public static void processRangeRestrictionEntry(TreeWalkListener listener,
                                                    GeneratedYangParser.RangeStatementContext ctx) {

        // Check for stack to be non empty.
        checkStackIsNotEmpty(listener, MISSING_HOLDER, RANGE_DATA, ctx.range().getText(), ENTRY);

        Parsable tmpData = listener.getParsedDataStack().peek();
        if (tmpData.getYangConstructType() == TYPE_DATA) {
            YangType type = (YangType) tmpData;
            setRangeRestriction(listener, type, ctx);
        } else {
            throw new ParserException(constructListenerErrorMessage(INVALID_HOLDER, RANGE_DATA,
                    ctx.range().getText(), ENTRY));
        }
    }

    /**
     * Sets the range restriction to type.
     *
     * @param listener listener's object
     * @param type     YANG type for which range restriction to be added
     * @param ctx      context object of the grammar rule
     */
    private static void setRangeRestriction(TreeWalkListener listener, YangType type,
                                            GeneratedYangParser.RangeStatementContext ctx) {

        if (type.getDataType() == DERIVED) {
            ((YangDerivedInfo<YangRangeRestriction>) type.getDataTypeExtendedInfo())
                    .setRangeRestrictionString(ctx.range().getText());
            ((YangDerivedInfo<YangRangeRestriction>) type.getDataTypeExtendedInfo())
                    .setLineNumber(ctx.getStart().getLine());
            ((YangDerivedInfo<YangRangeRestriction>) type.getDataTypeExtendedInfo())
                    .setCharPosition(ctx.getStart().getCharPositionInLine());
            return;
        }

        if (!(isOfRangeRestrictedType(type.getDataType()))) {
            ParserException parserException = new ParserException("YANG file error: Range restriction can't be " +
                    "applied to a given type");
            parserException.setLine(ctx.getStart().getLine());
            parserException.setCharPosition(ctx.getStart().getCharPositionInLine());
            throw parserException;
        }

        YangRangeRestriction rangeRestriction = null;
        try {
            rangeRestriction = processRangeRestriction(null, ctx.getStart().getLine(),
                    ctx.getStart().getCharPositionInLine(), false, ctx.range().getText(), type.getDataType());
        } catch (DataModelException e) {
            ParserException parserException = new ParserException(e.getMessage());
            parserException.setCharPosition(e.getCharPositionInLine());
            parserException.setLine(e.getLineNumber());
            throw parserException;
        }

        if (rangeRestriction != null) {
            type.setDataTypeExtendedInfo(rangeRestriction);
        }
        listener.getParsedDataStack().push(rangeRestriction);
    }

    /**
     * Performs validation and updates the data model tree.
     * It is called when parser exits from grammar rule (range).
     *
     * @param listener listener's object
     * @param ctx      context object of the grammar rule
     */
    public static void processRangeRestrictionExit(TreeWalkListener listener,
                                                   GeneratedYangParser.RangeStatementContext ctx) {

        // Check for stack to be non empty.
        checkStackIsNotEmpty(listener, MISSING_HOLDER, RANGE_DATA, ctx.range().getText(), EXIT);

        Parsable tmpData = listener.getParsedDataStack().peek();
        if (tmpData instanceof YangRangeRestriction) {
            listener.getParsedDataStack().pop();
        } else if (tmpData instanceof YangType
                && ((YangType) tmpData).getDataType() == DERIVED) {
            // TODO : need to handle in linker
        } else {
            throw new ParserException(constructListenerErrorMessage(MISSING_CURRENT_HOLDER, RANGE_DATA,
                    ctx.range().getText(), EXIT));
        }
    }
}

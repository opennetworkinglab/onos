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

import java.util.regex.Pattern;

import org.onosproject.yangutils.datamodel.YangRangeInterval;
import org.onosproject.yangutils.datamodel.YangRangeRestriction;
import org.onosproject.yangutils.datamodel.YangType;
import org.onosproject.yangutils.datamodel.YangDerivedInfo;
import org.onosproject.yangutils.datamodel.exceptions.DataModelException;
import org.onosproject.yangutils.datamodel.YangDataTypes;
import org.onosproject.yangutils.parser.Parsable;
import org.onosproject.yangutils.parser.antlrgencode.GeneratedYangParser;
import org.onosproject.yangutils.parser.exceptions.ParserException;
import org.onosproject.yangutils.parser.impl.TreeWalkListener;
import org.onosproject.yangutils.utils.YangConstructType;
import org.onosproject.yangutils.utils.builtindatatype.DataTypeException;
import org.onosproject.yangutils.utils.builtindatatype.YangBuiltInDataTypeInfo;

import static org.onosproject.yangutils.parser.impl.parserutils.ListenerErrorLocation.ENTRY;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerErrorMessageConstruction.constructExtendedListenerErrorMessage;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerErrorMessageConstruction.constructListenerErrorMessage;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerErrorType.INVALID_HOLDER;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerErrorType.MISSING_HOLDER;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerErrorType.UNHANDLED_PARSED_DATA;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerUtil.removeQuotesAndHandleConcat;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerValidation.checkStackIsNotEmpty;
import static org.onosproject.yangutils.utils.YangConstructType.TYPE_DATA;
import static org.onosproject.yangutils.utils.YangConstructType.RANGE_DATA;
import static org.onosproject.yangutils.utils.builtindatatype.BuiltInTypeObjectFactory.getDataObjectFromString;

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

    private static final String PIPE = "|";
    private static final String RANGE_INTERVAL = "..";
    private static final int MAX_RANGE_BOUNDARY = 2;
    private static final int MIN_RANGE_BOUNDARY = 1;

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
     * @param ctx context object of the grammar rule
     */
    public static void processRangeRestrictionEntry(TreeWalkListener listener,
                                                      GeneratedYangParser.RangeStatementContext ctx) {

        // Check for stack to be non empty.
        checkStackIsNotEmpty(listener, MISSING_HOLDER, RANGE_DATA, ctx.range().getText(), ENTRY);

        Parsable tmpData = listener.getParsedDataStack().peek();
        if (tmpData.getYangConstructType() == TYPE_DATA) {
            YangType type = (YangType) tmpData;
            setRangeRestriction(type, ctx);
        } else {
            throw new ParserException(constructListenerErrorMessage(INVALID_HOLDER, RANGE_DATA,
                    ctx.range().getText(), ENTRY));
        }
    }

    /**
     * Sets the range restriction to type.
     *
     * @param type YANG type for which range restriction to be added
     * @param ctx context object of the grammar rule
     */
    private static void setRangeRestriction(YangType type,
            GeneratedYangParser.RangeStatementContext ctx) {

        YangBuiltInDataTypeInfo<?> startValue;
        YangBuiltInDataTypeInfo<?> endValue;
        YangRangeRestriction rangeRestriction = new YangRangeRestriction();

        String rangeArgument = removeQuotesAndHandleConcat(ctx.range().getText());
        String[] rangeArguments = rangeArgument.trim().split(Pattern.quote(PIPE));

        for (String rangePart : rangeArguments) {
            String startInterval;
            String endInterval;
            YangRangeInterval rangeInterval = new YangRangeInterval();
            String[] rangeBoundary = rangePart.trim().split(Pattern.quote(RANGE_INTERVAL));

            if (rangeBoundary.length > MAX_RANGE_BOUNDARY) {
                ParserException parserException = new ParserException("YANG file error : " +
                        YangConstructType.getYangConstructType(RANGE_DATA) + " " + rangeArgument +
                        " is not valid.");
                parserException.setLine(ctx.getStart().getLine());
                parserException.setCharPosition(ctx.getStart().getCharPositionInLine());
                throw parserException;
            }

            if (rangeBoundary.length == MIN_RANGE_BOUNDARY) {
                startInterval = rangeBoundary[0];
                endInterval = rangeBoundary[0];
            } else {
                startInterval = rangeBoundary[0];
                endInterval = rangeBoundary[1];
            }

            try {
                startValue = getDataObjectFromString(startInterval, type.getDataType());
                endValue = getDataObjectFromString(endInterval, type.getDataType());
            } catch (DataTypeException e) {
                ParserException parserException = new ParserException(e.getMessage());
                parserException.setLine(ctx.getStart().getLine());
                parserException.setCharPosition(ctx.getStart().getCharPositionInLine());
                throw parserException;
            }

            rangeInterval.setStartValue(startValue);
            rangeInterval.setEndValue(endValue);

            try {
                rangeRestriction.addRangeRestrictionInterval(rangeInterval);
            } catch (DataModelException e) {
                ParserException parserException = new ParserException(constructExtendedListenerErrorMessage(
                        UNHANDLED_PARSED_DATA, RANGE_DATA, rangeArgument, ENTRY, e.getMessage()));
                parserException.setLine(ctx.getStart().getLine());
                parserException.setCharPosition(ctx.getStart().getCharPositionInLine());
                throw parserException;
            }
        }

        if (rangeRestriction != null) {
            if (type.getDataType() == YangDataTypes.DERIVED) {
                ((YangDerivedInfo<YangRangeRestriction>) type.getDataTypeExtendedInfo())
                        .setExtendedInfo(rangeRestriction);
            } else {
                type.setDataTypeExtendedInfo(rangeRestriction);
            }
        }

    }
}
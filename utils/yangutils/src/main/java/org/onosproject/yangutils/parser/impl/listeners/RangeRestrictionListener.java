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

import java.math.BigInteger;
import java.util.regex.Pattern;

import org.onosproject.yangutils.datamodel.YangType;
import org.onosproject.yangutils.datamodel.YangRangeRestriction;
import org.onosproject.yangutils.datamodel.YangRangeInterval;
import org.onosproject.yangutils.datamodel.YangDataTypes;
import org.onosproject.yangutils.datamodel.exceptions.DataModelException;
import org.onosproject.yangutils.parser.Parsable;
import org.onosproject.yangutils.parser.antlrgencode.GeneratedYangParser;
import org.onosproject.yangutils.parser.exceptions.ParserException;
import org.onosproject.yangutils.parser.impl.TreeWalkListener;

import static org.onosproject.yangutils.utils.YangConstructType.RANGE_DATA;
import static org.onosproject.yangutils.utils.YangConstructType.TYPE_DATA;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerErrorLocation.ENTRY;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerErrorMessageConstruction.constructExtendedListenerErrorMessage;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerErrorMessageConstruction.constructListenerErrorMessage;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerErrorType.INVALID_HOLDER;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerErrorType.MISSING_HOLDER;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerErrorType.UNHANDLED_PARSED_DATA;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerValidation.checkStackIsNotEmpty;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerUtil.removeQuotesAndHandleConcat;

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
     * @param listener listener's object.
     * @param ctx context object of the grammar rule.
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
     * @param type YANG type for which range restriction to be added.
     * @param ctx context object of the grammar rule.
     */
    private static void setRangeRestriction(YangType type,
                                              GeneratedYangParser.RangeStatementContext ctx) {
        YangRangeRestriction<?> rangeRestriction = null;
        YangRangeInterval rangeInterval;

        String rangeArgument = removeQuotesAndHandleConcat(ctx.range().getText());
        String[] rangeArguments = rangeArgument.trim().split(Pattern.quote(PIPE));

        for (String rangePart : rangeArguments) {
            String[] rangeBoundary = rangePart.trim().split(Pattern.quote(RANGE_INTERVAL));

            if (rangeBoundary.length == 1) {
                rangeBoundary[1] = rangeBoundary[0];
            }

            if (type.getDataType() == YangDataTypes.INT8) {
                rangeRestriction = new YangRangeRestriction<Byte>();
                rangeInterval = new YangRangeInterval<Byte>();
                rangeInterval.setStartValue(Byte.parseByte(rangeBoundary[0]));
                rangeInterval.setEndValue(Byte.parseByte(rangeBoundary[1]));
            } else if ((type.getDataType() == YangDataTypes.INT16)
                    || (type.getDataType() == YangDataTypes.UINT8)) {
                rangeRestriction = new YangRangeRestriction<Short>();
                rangeInterval = new YangRangeInterval<Short>();
                rangeInterval.setStartValue(Short.parseShort(rangeBoundary[0]));
                rangeInterval.setEndValue(Short.parseShort(rangeBoundary[1]));
            } else if ((type.getDataType() == YangDataTypes.INT32)
                    || (type.getDataType() == YangDataTypes.UINT16)) {
                rangeRestriction = new YangRangeRestriction<Integer>();
                rangeInterval = new YangRangeInterval<Integer>();
                rangeInterval.setStartValue(Integer.parseInt(rangeBoundary[0]));
                rangeInterval.setEndValue(Integer.parseInt(rangeBoundary[1]));
            } else if ((type.getDataType() == YangDataTypes.INT64)
                    || (type.getDataType() == YangDataTypes.UINT32)) {
                rangeRestriction = new YangRangeRestriction<Long>();
                rangeInterval = new YangRangeInterval<Long>();
                rangeInterval.setStartValue(Long.parseLong(rangeBoundary[0]));
                rangeInterval.setEndValue(Long.parseLong(rangeBoundary[1]));
            } else if (type.getDataType() == YangDataTypes.UINT64) {
                rangeRestriction = new YangRangeRestriction<BigInteger>();
                rangeInterval = new YangRangeInterval<BigInteger>();
                rangeInterval.setStartValue(new BigInteger(rangeBoundary[0]));
                rangeInterval.setEndValue(new BigInteger(rangeBoundary[0]));
            } else {
                //TODO: support derived for base built in type of string
                throw new ParserException(constructListenerErrorMessage(INVALID_HOLDER, RANGE_DATA,
                        rangeArgument, ENTRY));
            }

            try {
                rangeRestriction.addRangeRestrictionInterval(rangeInterval);
            } catch (DataModelException e) {
                throw new ParserException(constructExtendedListenerErrorMessage(UNHANDLED_PARSED_DATA, RANGE_DATA,
                        rangeArgument, ENTRY, e.getMessage()));
            }
        }

        if (rangeRestriction != null) {
            type.setDataTypeExtendedInfo(rangeRestriction);
        }

    }
}
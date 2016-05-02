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

package org.onosproject.yangutils.utils;

import java.util.regex.Pattern;
import org.onosproject.yangutils.datamodel.YangDataTypes;
import org.onosproject.yangutils.datamodel.YangRangeInterval;
import org.onosproject.yangutils.datamodel.YangRangeRestriction;
import org.onosproject.yangutils.datamodel.exceptions.DataModelException;
import org.onosproject.yangutils.parser.exceptions.ParserException;
import org.onosproject.yangutils.utils.builtindatatype.DataTypeException;
import org.onosproject.yangutils.utils.builtindatatype.YangBuiltInDataTypeInfo;

import static org.onosproject.yangutils.datamodel.YangDataTypes.DECIMAL64;
import static org.onosproject.yangutils.datamodel.YangDataTypes.INT16;
import static org.onosproject.yangutils.datamodel.YangDataTypes.INT32;
import static org.onosproject.yangutils.datamodel.YangDataTypes.INT64;
import static org.onosproject.yangutils.datamodel.YangDataTypes.INT8;
import static org.onosproject.yangutils.datamodel.YangDataTypes.UINT16;
import static org.onosproject.yangutils.datamodel.YangDataTypes.UINT32;
import static org.onosproject.yangutils.datamodel.YangDataTypes.UINT64;
import static org.onosproject.yangutils.datamodel.YangDataTypes.UINT8;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerErrorLocation.ENTRY;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerErrorMessageConstruction.constructExtendedListenerErrorMessage;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerErrorType.UNHANDLED_PARSED_DATA;
import static org.onosproject.yangutils.parser.impl.parserutils.ListenerUtil.removeQuotesAndHandleConcat;
import static org.onosproject.yangutils.utils.YangConstructType.LENGTH_DATA;
import static org.onosproject.yangutils.utils.YangConstructType.RANGE_DATA;
import static org.onosproject.yangutils.utils.builtindatatype.BuiltInTypeObjectFactory.getDataObjectFromString;

/**
 * Represents restriction resolver which provide common utility used by parser
 * and during linking for restriction resolution.
 */
public final class RestrictionResolver {

    private static final String PIPE = "|";
    private static final String INTERVAL = "..";
    private static final int MAX_RANGE_BOUNDARY = 2;
    private static final int MIN_RANGE_BOUNDARY = 1;
    private static final String MIN_KEYWORD = "min";
    private static final String MAX_KEYWORD = "max";

    /**
     * Creates a restriction resolver.
     */
    private RestrictionResolver() {
    }

    /**
     * Processes the range restriction for parser and linker.
     *
     * @param refRangeRestriction    range restriction of referred typedef
     * @param lineNumber             error line number
     * @param charPositionInLine     error character position in line
     * @param hasReferredRestriction whether has referred restriction
     * @param curRangeString         caller type's range string
     * @param effectiveType          effective type, when called from linker
     * @return YANG range restriction
     */
    public static YangRangeRestriction processRangeRestriction(YangRangeRestriction refRangeRestriction,
                                                               int lineNumber, int charPositionInLine,
                                                               boolean hasReferredRestriction,
                                                               String curRangeString, YangDataTypes effectiveType) {
        YangBuiltInDataTypeInfo<?> startValue;
        YangBuiltInDataTypeInfo<?> endValue;
        YangRangeRestriction rangeRestriction = new YangRangeRestriction();

        String rangeArgument = removeQuotesAndHandleConcat(curRangeString);
        String[] rangeArguments = rangeArgument.trim().split(Pattern.quote(PIPE));

        for (String rangePart : rangeArguments) {
            String startInterval;
            String endInterval;
            YangRangeInterval rangeInterval = new YangRangeInterval();
            String[] rangeBoundary = rangePart.trim().split(Pattern.quote(INTERVAL));

            if (rangeBoundary.length > MAX_RANGE_BOUNDARY) {
                ParserException parserException = new ParserException("YANG file error : " +
                        YangConstructType.getYangConstructType(RANGE_DATA) + " " + rangeArgument +
                        " is not valid.");
                parserException.setLine(lineNumber);
                parserException.setCharPosition(charPositionInLine);
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
                if (hasReferredRestriction && startInterval.equals(MIN_KEYWORD)
                        && refRangeRestriction.getMinRestrictedvalue() != null) {
                    startValue = refRangeRestriction.getMinRestrictedvalue();
                } else if (hasReferredRestriction && startInterval.equals(MAX_KEYWORD)
                        && refRangeRestriction.getMaxRestrictedvalue() != null) {
                    startValue = refRangeRestriction.getMaxRestrictedvalue();
                } else {
                    startValue = getDataObjectFromString(startInterval, effectiveType);
                }
                if (hasReferredRestriction && endInterval.equals(MIN_KEYWORD)
                        && refRangeRestriction.getMinRestrictedvalue() != null) {
                    endValue = refRangeRestriction.getMinRestrictedvalue();
                } else if (hasReferredRestriction && endInterval.equals(MAX_KEYWORD)
                        && refRangeRestriction.getMaxRestrictedvalue() != null) {
                    endValue = refRangeRestriction.getMaxRestrictedvalue();
                } else {
                    endValue = getDataObjectFromString(endInterval, effectiveType);
                }
            } catch (DataTypeException | DataModelException e) {
                ParserException parserException = new ParserException(e.getMessage());
                parserException.setLine(lineNumber);
                parserException.setCharPosition(charPositionInLine);
                throw parserException;
            }

            rangeInterval.setStartValue(startValue);
            rangeInterval.setEndValue(endValue);

            try {
                rangeRestriction.addRangeRestrictionInterval(rangeInterval);
            } catch (DataModelException e) {
                ParserException parserException = new ParserException(constructExtendedListenerErrorMessage(
                        UNHANDLED_PARSED_DATA, RANGE_DATA, rangeArgument, ENTRY, e.getMessage()));
                parserException.setLine(lineNumber);
                parserException.setCharPosition(charPositionInLine);
                throw parserException;
            }
        }
        return rangeRestriction;
    }

    /**
     * Processes the length restriction for parser and linker.
     *
     * @param refLengthRestriction   length restriction of referred typedef
     * @param lineNumber             error line number
     * @param charPositionInLine     error character position in line
     * @param hasReferredRestriction whether has referred restriction
     * @param curLengthString        caller type's length string
     * @return YANG range restriction
     */
    public static YangRangeRestriction processLengthRestriction(YangRangeRestriction refLengthRestriction,
                                                                int lineNumber, int charPositionInLine,
                                                                boolean hasReferredRestriction,
                                                                String curLengthString) {

        YangBuiltInDataTypeInfo<?> startValue;
        YangBuiltInDataTypeInfo<?> endValue;
        YangRangeRestriction lengthRestriction = new YangRangeRestriction<>();

        String rangeArgument = removeQuotesAndHandleConcat(curLengthString);
        String[] rangeArguments = rangeArgument.trim().split(Pattern.quote(PIPE));

        for (String rangePart : rangeArguments) {
            String startInterval;
            String endInterval;
            YangRangeInterval rangeInterval = new YangRangeInterval<>();
            String[] rangeBoundary = rangePart.trim().split(Pattern.quote(INTERVAL));

            if (rangeBoundary.length > MAX_RANGE_BOUNDARY) {
                ParserException parserException = new ParserException("YANG file error : " +
                        YangConstructType.getYangConstructType(LENGTH_DATA) + " " + rangeArgument +
                        " is not valid.");
                parserException.setLine(lineNumber);
                parserException.setCharPosition(charPositionInLine);
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
                if (hasReferredRestriction && startInterval.equals(MIN_KEYWORD)
                        && refLengthRestriction.getMinRestrictedvalue() != null) {
                    startValue = refLengthRestriction.getMinRestrictedvalue();
                } else if (hasReferredRestriction && startInterval.equals(MAX_KEYWORD)
                        && refLengthRestriction.getMaxRestrictedvalue() != null) {
                    startValue = refLengthRestriction.getMaxRestrictedvalue();
                } else {
                    startValue = getDataObjectFromString(startInterval, YangDataTypes.UINT64);
                }
                if (hasReferredRestriction && endInterval.equals(MIN_KEYWORD)
                        && refLengthRestriction.getMinRestrictedvalue() != null) {
                    endValue = refLengthRestriction.getMinRestrictedvalue();
                } else if (hasReferredRestriction && endInterval.equals(MAX_KEYWORD)
                        && refLengthRestriction.getMaxRestrictedvalue() != null) {
                    endValue = refLengthRestriction.getMaxRestrictedvalue();
                } else {
                    endValue = getDataObjectFromString(endInterval, YangDataTypes.UINT64);
                }
            } catch (DataTypeException | DataModelException e) {
                ParserException parserException = new ParserException(e.getMessage());
                parserException.setLine(lineNumber);
                parserException.setCharPosition(charPositionInLine);
                throw parserException;
            }

            rangeInterval.setStartValue(startValue);
            rangeInterval.setEndValue(endValue);

            try {
                lengthRestriction.addRangeRestrictionInterval(rangeInterval);
            } catch (DataModelException e) {
                ParserException parserException = new ParserException(constructExtendedListenerErrorMessage(
                        UNHANDLED_PARSED_DATA, LENGTH_DATA, rangeArgument, ENTRY, e.getMessage()));
                parserException.setLine(lineNumber);
                parserException.setCharPosition(charPositionInLine);
                throw parserException;
            }
        }
        return lengthRestriction;
    }

    /**
     * Returns whether the data type is of range restricted type.
     *
     * @param dataType data type to be checked
     * @return true, if data type can have range restrictions, false otherwise
     */
    public static boolean isOfRangeRestrictedType(YangDataTypes dataType) {
        return (dataType == INT8
                || dataType == INT16
                || dataType == INT32
                || dataType == INT64
                || dataType == UINT8
                || dataType == UINT16
                || dataType == UINT32
                || dataType == UINT64
                || dataType == DECIMAL64);
    }
}

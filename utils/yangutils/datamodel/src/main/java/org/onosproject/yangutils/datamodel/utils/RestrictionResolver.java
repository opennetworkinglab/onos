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

package org.onosproject.yangutils.datamodel.utils;

import static org.onosproject.yangutils.datamodel.YangDataTypes.DECIMAL64;
import static org.onosproject.yangutils.datamodel.YangDataTypes.INT16;
import static org.onosproject.yangutils.datamodel.YangDataTypes.INT32;
import static org.onosproject.yangutils.datamodel.YangDataTypes.INT64;
import static org.onosproject.yangutils.datamodel.YangDataTypes.INT8;
import static org.onosproject.yangutils.datamodel.YangDataTypes.UINT16;
import static org.onosproject.yangutils.datamodel.YangDataTypes.UINT32;
import static org.onosproject.yangutils.datamodel.YangDataTypes.UINT64;
import static org.onosproject.yangutils.datamodel.YangDataTypes.UINT8;
import static org.onosproject.yangutils.datamodel.utils.YangConstructType.LENGTH_DATA;
import static org.onosproject.yangutils.datamodel.utils.YangConstructType.RANGE_DATA;
import static org.onosproject.yangutils.datamodel.utils.builtindatatype.BuiltInTypeObjectFactory.getDataObjectFromString;

import java.util.regex.Pattern;

import org.onosproject.yangutils.datamodel.YangDataTypes;
import org.onosproject.yangutils.datamodel.YangRangeInterval;
import org.onosproject.yangutils.datamodel.YangRangeRestriction;
import org.onosproject.yangutils.datamodel.exceptions.DataModelException;
import org.onosproject.yangutils.datamodel.utils.builtindatatype.YangBuiltInDataTypeInfo;

/**
 * Represents restriction resolver which provide common utility used by parser
 * and during linking for restriction resolution.
 */
public final class RestrictionResolver {

    private static final String PIPE = "|";
    private static final String ADD = "+";
    private static final String EMPTY_STRING = "";
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
     * @throws DataModelException a violation in data model rule
     */
    public static YangRangeRestriction processRangeRestriction(YangRangeRestriction refRangeRestriction,
            int lineNumber, int charPositionInLine,
            boolean hasReferredRestriction,
            String curRangeString, YangDataTypes effectiveType)
            throws DataModelException {
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
                DataModelException dataModelException = new DataModelException("YANG file error : " +
                        YangConstructType.getYangConstructType(RANGE_DATA) + " " + rangeArgument +
                        " is not valid.");
                dataModelException.setLine(lineNumber);
                dataModelException.setCharPosition(charPositionInLine);
                throw dataModelException;
            }

            if (rangeBoundary.length == MIN_RANGE_BOUNDARY) {
                startInterval = rangeBoundary[0].trim();
                endInterval = rangeBoundary[0].trim();
            } else {
                startInterval = rangeBoundary[0].trim();
                endInterval = rangeBoundary[1].trim();
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
            } catch (Exception e) {
                DataModelException dataModelException = new DataModelException(e.getMessage());
                dataModelException.setLine(lineNumber);
                dataModelException.setCharPosition(charPositionInLine);
                throw dataModelException;
            }

            rangeInterval.setStartValue(startValue);
            rangeInterval.setEndValue(endValue);

            try {
                rangeRestriction.addRangeRestrictionInterval(rangeInterval);
            } catch (DataModelException dataModelException) {
                dataModelException.setLine(lineNumber);
                dataModelException.setCharPosition(charPositionInLine);
                throw dataModelException;
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
     * @throws DataModelException a violation in data model rule
     */
    public static YangRangeRestriction processLengthRestriction(YangRangeRestriction refLengthRestriction,
            int lineNumber, int charPositionInLine,
            boolean hasReferredRestriction,
            String curLengthString) throws DataModelException {

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
                DataModelException dataModelException = new DataModelException("YANG file error : " +
                        YangConstructType.getYangConstructType(LENGTH_DATA) + " " + rangeArgument +
                        " is not valid.");
                dataModelException.setLine(lineNumber);
                dataModelException.setCharPosition(charPositionInLine);
                throw dataModelException;
            }

            if (rangeBoundary.length == MIN_RANGE_BOUNDARY) {
                startInterval = rangeBoundary[0].trim();
                endInterval = rangeBoundary[0].trim();
            } else {
                startInterval = rangeBoundary[0].trim();
                endInterval = rangeBoundary[1].trim();
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
            } catch (Exception e) {
                DataModelException dataModelException = new DataModelException(e.getMessage());
                dataModelException.setLine(lineNumber);
                dataModelException.setCharPosition(charPositionInLine);
                throw dataModelException;
            }

            rangeInterval.setStartValue(startValue);
            rangeInterval.setEndValue(endValue);

            try {
                lengthRestriction.addRangeRestrictionInterval(rangeInterval);
            } catch (DataModelException dataModelException) {
                dataModelException.setLine(lineNumber);
                dataModelException.setCharPosition(charPositionInLine);
                throw dataModelException;
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
        return dataType == INT8
                || dataType == INT16
                || dataType == INT32
                || dataType == INT64
                || dataType == UINT8
                || dataType == UINT16
                || dataType == UINT32
                || dataType == UINT64
                || dataType == DECIMAL64;
    }

    /**
     * Removes doubles quotes and concatenates if string has plus symbol.
     *
     * @param yangStringData string from yang file
     * @return concatenated string after removing double quotes
     */
    private static String removeQuotesAndHandleConcat(String yangStringData) {

        yangStringData = yangStringData.replace("\"", EMPTY_STRING);
        String[] tmpData = yangStringData.split(Pattern.quote(ADD));
        StringBuilder builder = new StringBuilder();
        for (String yangString : tmpData) {
            builder.append(yangString);
        }
        return builder.toString();
    }
}

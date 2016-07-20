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

package org.onosproject.yangutils.datamodel;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;

import org.onosproject.yangutils.datamodel.exceptions.DataModelException;
import org.onosproject.yangutils.datamodel.utils.Parsable;
import org.onosproject.yangutils.datamodel.utils.YangConstructType;
import org.onosproject.yangutils.datamodel.utils.builtindatatype.YangBuiltInDataTypeInfo;

import static org.onosproject.yangutils.datamodel.utils.builtindatatype.BuiltInTypeObjectFactory.getDataObjectFromString;

import static com.google.common.base.Preconditions.checkNotNull;

/*-
 * Reference RFC 6020.
 *
 * The range Statement
 *
 *  The "range" statement, which is an optional sub-statement to the
 *  "type" statement, takes as an argument a range expression string.  It
 *  is used to restrict integer and decimal built-in types, or types
 *  derived from those.
 *
 *  A range consists of an explicit value, or a lower-inclusive bound,
 *  two consecutive dots "..", and an upper-inclusive bound.  Multiple
 *  values or ranges can be given, separated by "|".  If multiple values
 *  or ranges are given, they all MUST be disjoint and MUST be in
 *  ascending order.  If a range restriction is applied to an already
 *  range-restricted type, the new restriction MUST be equal or more
 *  limiting, that is raising the lower bounds, reducing the upper
 *  bounds, removing explicit values or ranges, or splitting ranges into
 *  multiple ranges with intermediate gaps.  Each explicit value and
 *  range boundary value given in the range expression MUST match the
 *  type being restricted, or be one of the special values "min" or
 *  "max". "min" and "max" mean the minimum and maximum value accepted
 *  for the type being restricted, respectively.
 */

/**
 * Represents ascending range restriction information.
 *
 * @param <T> range type (data type)
 */
public class YangRangeRestriction<T extends YangBuiltInDataTypeInfo<T>>
        implements YangDesc, YangReference, YangAppErrorInfo, Parsable, Serializable {

    private static final long serialVersionUID = 8062016051L;

    /**
     * Ascending list of range interval restriction. If the restriction is a
     * single value, the start and end length of the range is same.
     */
    private List<YangRangeInterval<T>> ascendingRangeIntervals;

    /**
     * Textual reference.
     */
    private String reference;

    /**
     * Application's error message, to be used for data error.
     */
    private String errorMessage;

    /**
     * Application's error tag, to be filled in data validation error response.
     */
    private String errorAppTag;

    /**
     * Textual description.
     */
    private String description;

    /**
     * Creates YANG range restriction object.
     */
    public YangRangeRestriction() {
    }

    /**
     * Returns the list of range interval restriction in ascending order.
     *
     * @return list of range interval restriction in ascending order
     */
    public List<YangRangeInterval<T>> getAscendingRangeIntervals() {
        return ascendingRangeIntervals;
    }

    /**
     * Sets the list of range interval restriction in ascending order.
     *
     * @param rangeList list of range interval restriction in ascending order
     */
    private void setAscendingRangeIntervals(List<YangRangeInterval<T>> rangeList) {
        ascendingRangeIntervals = rangeList;
    }

    /**
     * Returns the minimum valid value as per the restriction.
     *
     * @return minimum restricted value
     * @throws DataModelException data model exception for minimum restriction
     */
    public T getMinRestrictedvalue() throws DataModelException {
        if (getAscendingRangeIntervals() == null) {
            throw new DataModelException("No range restriction info");
        }
        if (getAscendingRangeIntervals().isEmpty()) {
            throw new DataModelException("No range interval info");
        }
        return getAscendingRangeIntervals().get(0).getStartValue();
    }

    /**
     * Returns the maximum valid value as per the restriction.
     *
     * @return minimum maximum value
     * @throws DataModelException data model exception for maximum restriction
     */
    public T getMaxRestrictedvalue() throws DataModelException {
        if (getAscendingRangeIntervals() == null) {
            throw new DataModelException("No range restriction info");
        }
        if (getAscendingRangeIntervals().isEmpty()) {
            throw new DataModelException("No range interval info");
        }
        return getAscendingRangeIntervals()
                .get(getAscendingRangeIntervals().size() - 1).getEndValue();
    }

    /**
     * Adds new interval to extend its range in the last. i.e. newly added
     * interval needs to be bigger than the biggest interval in the list.
     *
     * @param newInterval restricted length interval
     * @throws DataModelException data model exception for range restriction
     */
    public void addRangeRestrictionInterval(YangRangeInterval<T> newInterval) throws DataModelException {

        checkNotNull(newInterval);
        checkNotNull(newInterval.getStartValue());

        if (getAscendingRangeIntervals() == null) {
            /*
             * First interval that is being added, and it must be the smallest
             * interval.
             */
            setAscendingRangeIntervals(new LinkedList<YangRangeInterval<T>>());
            getAscendingRangeIntervals().add(newInterval);
            return;
        }

        T curMaxvalue = getMaxRestrictedvalue();

        if (newInterval.getStartValue().compareTo(curMaxvalue) != 1) {
            throw new DataModelException(
                    "New added range interval is lesser than the old interval(s)");
        }

        getAscendingRangeIntervals()
                .add(getAscendingRangeIntervals().size(), newInterval);
    }

    /**
     * Validates if the given value is correct as per the restriction.
     *
     * @param valueInString value
     * @return true, if the value is confirming to restriction, false otherwise
     * @throws DataModelException data model error
     */
    public boolean isValidValueString(String valueInString) throws DataModelException {

        if (getAscendingRangeIntervals() == null
                || getAscendingRangeIntervals().isEmpty()) {
            // Throw exception, At least one default range needs to be set in
            // constructor or in linker.
            throw new DataModelException("Range interval missing in range restriction.");

        }

        YangDataTypes type = getAscendingRangeIntervals().get(0).getStartValue().getYangType();
        YangBuiltInDataTypeInfo<?> value = getDataObjectFromString(valueInString, type);

        for (YangRangeInterval<T> interval : getAscendingRangeIntervals()) {
            int rangeStartCompareRes = interval.getStartValue().compareTo((T) value);
            int rangeEndCompareRes = interval.getEndValue().compareTo((T) value);

            if (rangeStartCompareRes <= 0 && rangeEndCompareRes >= 0) {
                return true;
            }
        }

        return false;
    }

    /**
     * Validates if the given interval is correct as per the restriction.
     *
     * @param rangeInterval range interval
     * @return true, if the interval is confirming to restriction, false otherwise
     * @throws DataModelException data model error
     */
    public boolean isValidInterval(YangRangeInterval rangeInterval) throws DataModelException {

        if (getAscendingRangeIntervals() == null
                || getAscendingRangeIntervals().isEmpty()) {
            // Throw exception, At least one default range needs to be set in
            // constructor or in linker.
            throw new DataModelException("Range interval missing in range restriction.");
        }

        for (YangRangeInterval<T> interval : getAscendingRangeIntervals()) {
            int rangeStartCompareRes = interval.getStartValue().compareTo((T) rangeInterval.getStartValue());
            int rangeEndCompareRes = interval.getEndValue().compareTo((T) rangeInterval.getEndValue());

            if (rangeStartCompareRes <= 0 && rangeEndCompareRes >= 0) {
                return true;
            }
        }
        throw new DataModelException("Range interval doesn't fall within the referred restriction ranges");
    }

    /**
     * Returns the textual reference of the length restriction.
     *
     * @return textual reference of the length restriction
     */
    @Override
    public String getReference() {
        return reference;
    }

    /**
     * Sets the textual reference of the length restriction.
     *
     * @param ref textual reference of the length restriction
     */
    @Override
    public void setReference(String ref) {
        reference = ref;
    }

    /**
     * Returns the description of the length restriction.
     *
     * @return description of the length restriction
     */
    @Override
    public String getDescription() {
        return description;
    }

    /**
     * Sets the description of the length restriction.
     *
     * @param desc description of the length restriction
     */
    @Override
    public void setDescription(String desc) {
        description = desc;

    }

    /**
     * Returns application's error message, to be used for data error.
     *
     * @return Application's error message, to be used for data error
     */
    @Override
    public String getGetErrorMessage() {
        return errorMessage;
    }

    /**
     * Sets Application's error message, to be used for data error.
     *
     * @param errMsg Application's error message, to be used for data error
     */
    @Override
    public void setErrorMessage(String errMsg) {
        errorMessage = errMsg;

    }

    /**
     * Returns application's error tag, to be used for data error.
     *
     * @return application's error tag, to be used for data error
     */
    @Override
    public String getGetErrorAppTag() {
        return errorAppTag;
    }

    /**
     * Sets application's error tag, to be used for data error.
     *
     * @param errTag application's error tag, to be used for data error.
     */
    @Override
    public void setErrorAppTag(String errTag) {
        errorAppTag = errTag;
    }

    @Override
    public YangConstructType getYangConstructType() {
        return YangConstructType.RANGE_DATA;
    }

    @Override
    public void validateDataOnEntry() throws DataModelException {
        // TODO: implement the method.
    }

    @Override
    public void validateDataOnExit() throws DataModelException {
        // TODO: implement the method.
    }
}

/*
 * Copyright 2016 Open Networking Laboratory
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

import java.util.LinkedList;
import java.util.List;

import org.onosproject.yangutils.datamodel.exceptions.DataModelException;

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
 * Ascending range restriction information.
 *
 * @param <T> range type (data type)
 */
public class YangRangeRestriction<T extends Comparable<T>> implements YangDesc, YangReference, YangAppErrorInfo {

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
     * Default constructor.
     */
    public YangRangeRestriction() {
    }

    /**
     * Get the list of range interval restriction in ascending order.
     *
     * @return list of range interval restriction in ascending order.
     */
    public List<YangRangeInterval<T>> getAscendingRangeIntervals() {
        return ascendingRangeIntervals;
    }

    /**
     * Set the list of range interval restriction in ascending order.
     *
     * @param rangeList list of range interval restriction in ascending order.
     */
    private void setAscendingRangeIntervals(List<YangRangeInterval<T>> rangeList) {
        ascendingRangeIntervals = rangeList;
    }

    /**
     * Get the minimum valid value as per the restriction.
     *
     * @throws DataModelException data model exception for minimum restriction.
     *
     * @return minimum restricted value.
     */
    public T getMinRestrictedvalue() throws DataModelException {
        if (getAscendingRangeIntervals() == null) {
            throw new DataModelException("No range restriction info");
        }
        if (getAscendingRangeIntervals().size() == 0) {
            throw new DataModelException("No range interval info");
        }
        return getAscendingRangeIntervals().get(0).getStartValue();
    }

    /**
     * Get the maximum valid value as per the restriction.
     *
     * @throws DataModelException data model exception for maximum restriction.
     *
     * @return minimum maximum value.
     */
    public T getMaxRestrictedvalue() throws DataModelException {
        if (getAscendingRangeIntervals() == null) {
            throw new DataModelException("No range restriction info");
        }
        if (getAscendingRangeIntervals().size() == 0) {
            throw new DataModelException("No range interval info");
        }
        return getAscendingRangeIntervals()
                .get(getAscendingRangeIntervals().size() - 1).getEndValue();
    }

    /**
     * Add new interval to extend its range in the last. i.e. newly added
     * interval needs to be bigger than the biggest interval in the list.
     *
     * @param newInterval restricted length interval.
     * @throws DataModelException data model exception for range restriction.
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
     * Get the textual reference of the length restriction.
     *
     * @return textual reference of the length restriction.
     */
    @Override
    public String getReference() {
        return reference;
    }

    /**
     * Set the textual reference of the length restriction.
     *
     * @param ref textual reference of the length restriction.
     */
    @Override
    public void setReference(String ref) {
        reference = ref;
    }

    /**
     * Get the description of the length restriction.
     *
     * @return description of the length restriction.
     */
    @Override
    public String getDescription() {
        return description;
    }

    /**
     * Set the description of the length restriction.
     *
     * @param desc description of the length restriction.
     */
    @Override
    public void setDescription(String desc) {
        description = desc;

    }

    /**
     * Get application's error message, to be used for data error.
     *
     * @return Application's error message, to be used for data error.
     */
    @Override
    public String getGetErrorMessage() {
        return errorMessage;
    }

    /**
     * Set Application's error message, to be used for data error.
     *
     * @param errMsg Application's error message, to be used for data error.
     */
    @Override
    public void setErrorMessage(String errMsg) {
        errorMessage = errMsg;

    }

    /**
     * Get application's error tag, to be used for data error.
     *
     * @return application's error tag, to be used for data error.
     */
    @Override
    public String getGetErrorAppTag() {
        return errorAppTag;
    }

    /**
     * Set application's error tag, to be used for data error.
     *
     * @param errTag application's error tag, to be used for data error.
     */
    @Override
    public void setErrorAppTag(String errTag) {
        errorAppTag = errTag;
    }
}

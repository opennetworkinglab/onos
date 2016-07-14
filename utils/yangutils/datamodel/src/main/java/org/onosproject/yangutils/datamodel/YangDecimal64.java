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

import org.onosproject.yangutils.datamodel.exceptions.DataModelException;
import org.onosproject.yangutils.datamodel.utils.FractionDigits;
import org.onosproject.yangutils.datamodel.utils.Parsable;
import org.onosproject.yangutils.datamodel.utils.YangConstructType;
import org.onosproject.yangutils.datamodel.utils.builtindatatype.YangBuiltInDataTypeInfo;
import org.onosproject.yangutils.datamodel.utils.builtindatatype.YangDataTypes;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ListIterator;

/**
 * Represents YANG decimal 64.
 */
public class YangDecimal64<T>
        implements YangBuiltInDataTypeInfo<YangDecimal64>, Parsable, Serializable, Comparable<YangDecimal64> {

    private static final long serialVersionUID = 8006201668L;

    /**
     * YANG's min keyword.
     */
    private static final String MIN_KEYWORD = "min";

    /**
     * YANG's max keyword.
     */
    private static final String MAX_KEYWORD = "max";

    /**
     * Valid minimum value of YANG's fraction-digits.
     */
    public static final int MIN_FRACTION_DIGITS_VALUE = 1;

    /**
     * Valid maximum value of YANG's fraction-digits.
     */
    public static final int MAX_FRACTION_DIGITS_VALUE = 18;

    // Decimal64 value
    private BigDecimal value;

    // fraction-digits
    private int fractionDigit;

    /**
     * Additional information about range restriction.
     */
    private T rangeRestrictedExtendedInfo;

    /**
     * Creates an instance of YANG decimal64.
     */
    public YangDecimal64() {
    }

    /**
     * Creates an instance of YANG decimal64.
     *
     * @param value of decimal64
     */
    public YangDecimal64(BigDecimal value) {
        setValue(value);
    }

    /**
     * Creates an instance of YANG decimal64.
     *
     * @param value of decimal64 in string
     * @throws DataModelException a violation of data model rules
     */
    public YangDecimal64(String value) throws DataModelException {
        fromString(value);
    }

    /**
     * Returns decimal64 value.
     *
     * @return value
     */
    public BigDecimal getValue() {
        return value;
    }

    /**
     * Sets the decimal64 value.
     *
     * @param value of decimal64
     */
    public void setValue(BigDecimal value) {
        this.value = value;
    }

    /**
     * Returns fraction digit.
     *
     * @return the fractionDigit
     */
    public int getFractionDigit() {
        return fractionDigit;
    }

    /**
     * Sets fraction digit.
     *
     * @param fractionDigit fraction digits.
     */
    public void setFractionDigit(int fractionDigit) {
        this.fractionDigit = fractionDigit;
    }

    /**
     * Returns additional information about range restriction.
     *
     * @return resolved range restricted extended information
     */
    public T getRangeRestrictedExtendedInfo() {
        return rangeRestrictedExtendedInfo;
    }

    /**
     * Sets additional information about range restriction.
     *
     * @param resolvedExtendedInfo resolved range restricted extended information
     */
    public void setRangeRestrictedExtendedInfo(T resolvedExtendedInfo) {
        this.rangeRestrictedExtendedInfo = resolvedExtendedInfo;
    }

    /**
     * Returns object of YANG decimal64.
     *
     * @param value of decimal64
     * @return YANG decimal64 object
     */
    public static YangDecimal64 of(BigDecimal value) {
        return new YangDecimal64(value);
    }

    @Override
    public YangDataTypes getYangType() {
        return YangDataTypes.DECIMAL64;
    }

    @Override
    public YangConstructType getYangConstructType() {
        return YangConstructType.DECIMAL64_DATA;
    }

    @Override
    public String toString() {
        return value.toString();
    }

    /**
     * Returns the object of YANG decimal64 from input string.
     *
     * @param valInString input String
     * @return Object of YANG decimal64
     * @throws DataModelException a violation of data model rules
     */
    public static YangDecimal64 fromString(String valInString) throws DataModelException {
        YangDecimal64 decimal64;
        decimal64 = of(new BigDecimal(valInString));
        decimal64.validateValue();
        return decimal64;
    }

    /**
     * Validate decimal64 value.
     *
     * @throws DataModelException a violation of data model rules
     */
    public void validateValue() throws DataModelException {
        if (!(FractionDigits.isValidDecimal64(this.value, this.fractionDigit))) {
            throw new DataModelException("YANG file error : decimal64 validation failed.");
        }
    }

    /**
     * Validate range restriction values based on fraction-digits decimal64 range value.
     *
     * @throws DataModelException a violation of data model rules
     */
    public void validateRange() throws DataModelException {
        YangRangeRestriction rangeRestriction = (YangRangeRestriction) getRangeRestrictedExtendedInfo();
        if (rangeRestriction == null) {
            return;
        }

        ListIterator<YangRangeInterval> rangeListIterator = rangeRestriction.getAscendingRangeIntervals()
                .listIterator();
        while (rangeListIterator.hasNext()) {
            YangRangeInterval rangeInterval = rangeListIterator.next();
            if (!(FractionDigits.isValidDecimal64(((YangDecimal64) rangeInterval.getStartValue()).getValue(),
                                                  getFractionDigit()))) {
                throw new DataModelException("YANG file error : decimal64 validation failed.");
            }

            if (!(FractionDigits.isValidDecimal64(((YangDecimal64) rangeInterval.getEndValue()).getValue(),
                                                  getFractionDigit()))) {
                throw new DataModelException("YANG file error : decimal64 validation failed.");
            }
        }
    }

    @Override
    public int compareTo(YangDecimal64 o) {
        return Double.compare(value.doubleValue(), o.value.doubleValue());
    }

    /**
     * Returns decimal64 default range restriction based on fraction-digits.
     * If range restriction is not provided then this default range value will be applicable.
     *
     * @return range restriction
     * @throws DataModelException a violation of data model rules
     */
    public YangRangeRestriction getDefaultRangeRestriction() throws DataModelException {
        YangRangeRestriction refRangeRestriction = new YangRangeRestriction();
        YangRangeInterval rangeInterval = new YangRangeInterval<>();
        FractionDigits.Range range = FractionDigits.getRange(this.fractionDigit);
        rangeInterval.setStartValue(new YangDecimal64(new BigDecimal((range.getMin()))));
        rangeInterval.setEndValue(new YangDecimal64(new BigDecimal((range.getMax()))));
        refRangeRestriction.addRangeRestrictionInterval(rangeInterval);
        return refRangeRestriction;
    }

    /**
     * Validates the data on entering the corresponding parse tree node.
     *
     * @throws DataModelException a violation of data model rules
     */
    @Override
    public void validateDataOnEntry() throws DataModelException {
        // TODO auto-generated method stub, to be implemented by parser
    }

    /**
     * Validates the data on exiting the corresponding parse tree node.
     *
     * @throws DataModelException a violation of data model rules
     */
    @Override
    public void validateDataOnExit() throws DataModelException {
        // TODO auto-generated method stub, to be implemented by parser
    }
}

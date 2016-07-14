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

import org.onosproject.yangutils.datamodel.exceptions.DataModelException;

import java.math.BigDecimal;
import java.util.ArrayList;

/**
 * The "fraction-digits" statement, which is a substatement to the
 * "type" statement, MUST be present if the type is "decimal64".  It
 * takes as an argument an integer between 1 and 18, inclusively.  It
 * controls the size of the minimum difference between values of a
 * decimal64 type, by restricting the value space to numbers that are
 * expressible as "i x 10^-n" where n is the fraction-digits argument.
 *
 * +----------------+-----------------------+----------------------+
 * | fraction-digit | min                   | max                  |
 * +----------------+-----------------------+----------------------+
 * | 1              | -922337203685477580.8 | 922337203685477580.7 |
 * | 2              | -92233720368547758.08 | 92233720368547758.07 |
 * | 3              | -9223372036854775.808 | 9223372036854775.807 |
 * | 4              | -922337203685477.5808 | 922337203685477.5807 |
 * | 5              | -92233720368547.75808 | 92233720368547.75807 |
 * | 6              | -9223372036854.775808 | 9223372036854.775807 |
 * | 7              | -922337203685.4775808 | 922337203685.4775807 |
 * | 8              | -92233720368.54775808 | 92233720368.54775807 |
 * | 9              | -9223372036.854775808 | 9223372036.854775807 |
 * | 10             | -922337203.6854775808 | 922337203.6854775807 |
 * | 11             | -92233720.36854775808 | 92233720.36854775807 |
 * | 12             | -9223372.036854775808 | 9223372.036854775807 |
 * | 13             | -922337.2036854775808 | 922337.2036854775807 |
 * | 14             | -92233.72036854775808 | 92233.72036854775807 |
 * | 15             | -9223.372036854775808 | 9223.372036854775807 |
 * | 16             | -922.3372036854775808 | 922.3372036854775807 |
 * | 17             | -92.23372036854775808 | 92.23372036854775807 |
 * | 18             | -9.223372036854775808 | 9.223372036854775807 |
 * +----------------+-----------------------+----------------------+
 */

/**
 * Represents the decimal64 value range based on fraction-digits.
 */
public final class FractionDigits {

    public static class Range {
        private double min;
        private double max;

        /**
         * Creates an instance of range.
         *
         * @param min minimum value of decimal64
         * @param max maximum value of decimal64
         */
        protected Range(double min, double max) {
            this.min = min;
            this.max = max;
        }

        /**
         * Retrieve minimum value range.
         *
         * @return minimum value range
         */
        public double getMin() {
            return min;
        }

        /**
         * Retrieve maximum value range.
         *
         * @return maximum value range
         */
        public double getMax() {
            return max;
        }
    }

    private static ArrayList<Range> decimal64ValueRange = null;

    /**
     * Creates a fraction-digits instance.
     */
    private FractionDigits() {
    }

    /**
     * Generates decimal64 value range based on fraction-digits.
     *
     * @return decimal64 value range by fraction-digits as index
     */
    private static ArrayList<Range> getDecimal64ValueRange() {
        if (decimal64ValueRange == null) {
            decimal64ValueRange = new ArrayList<>();
            decimal64ValueRange.add(new Range(-922337203685477580.8, 922337203685477580.7)); // fraction-digit: 1
            decimal64ValueRange.add(new Range(-92233720368547758.08, 92233720368547758.07)); // fraction-digit: 2
            decimal64ValueRange.add(new Range(-9223372036854775.808, 9223372036854775.807)); // fraction-digit: 3
            decimal64ValueRange.add(new Range(-922337203685477.5808, 922337203685477.5807)); // fraction-digit: 4
            decimal64ValueRange.add(new Range(-92233720368547.75808, 92233720368547.75807)); // fraction-digit: 5
            decimal64ValueRange.add(new Range(-9223372036854.775808, 9223372036854.775807)); // fraction-digit: 6
            decimal64ValueRange.add(new Range(-922337203685.4775808, 922337203685.4775807)); // fraction-digit: 7
            decimal64ValueRange.add(new Range(-92233720368.54775808, 92233720368.54775807)); // fraction-digit: 8
            decimal64ValueRange.add(new Range(-9223372036.854775808, 9223372036.854775807)); // fraction-digit: 9
            decimal64ValueRange.add(new Range(-922337203.6854775808, 922337203.6854775807)); // fraction-digit: 10
            decimal64ValueRange.add(new Range(-92233720.36854775808, 92233720.36854775807)); // fraction-digit: 11
            decimal64ValueRange.add(new Range(-9223372.036854775808, 9223372.036854775807)); // fraction-digit: 12
            decimal64ValueRange.add(new Range(-922337.2036854775808, 922337.2036854775807)); // fraction-digit: 13
            decimal64ValueRange.add(new Range(-92233.72036854775808, 92233.72036854775807)); // fraction-digit: 14
            decimal64ValueRange.add(new Range(-9223.372036854775808, 9223.372036854775807)); // fraction-digit: 15
            decimal64ValueRange.add(new Range(-922.3372036854775808, 922.3372036854775807)); // fraction-digit: 16
            decimal64ValueRange.add(new Range(-92.23372036854775808, 92.23372036854775807)); // fraction-digit: 17
            decimal64ValueRange.add(new Range(-9.223372036854775808, 9.223372036854775807)); // fraction-digit: 18
        }
        return decimal64ValueRange;
    }

    /**
     * Checks given decimal64 value is in the specific range based on given fraction-digit.
     *
     * @param value decimal64 value
     * @param fractionDigit fraction-digits
     * @return success when it is in specific range otherwise false
     */
    public static boolean isValidDecimal64(BigDecimal value, int fractionDigit) {
        if (!((fractionDigit >= 1) && (fractionDigit <= 18))) {
            return false;
        }

        // ArrayList index starts from 0.
        Range range = getDecimal64ValueRange().get(fractionDigit - 1);
        if ((value.doubleValue() >= range.min) && (value.doubleValue() <= range.max)) {
            return true;
        }
        return false;
    }

    /**
     * Retrieve range based on fraction-digits.
     *
     * @param fractionDigit fraction-digits
     * @return range
     * @throws DataModelException a violation of data model rules
     */
    public static Range getRange(int fractionDigit) throws DataModelException {
        if (!((fractionDigit >= 1) && (fractionDigit <= 18))) {
            throw new DataModelException("YANG file error : given fraction-digit is not in its range (1..18).");
        }

        return getDecimal64ValueRange().get(fractionDigit - 1);
    }
}

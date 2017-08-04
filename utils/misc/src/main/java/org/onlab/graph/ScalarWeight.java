/*
 * Copyright 2016-present Open Networking Foundation
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

package org.onlab.graph;

import static com.google.common.base.MoreObjects.toStringHelper;

import com.google.common.math.DoubleMath;

import java.util.Objects;

/**
 * Weight implementation based on a double value.
 */
public class ScalarWeight implements Weight {

    /**
     * Instance of scalar weight to mark links/paths which
     * can not be traversed.
     */
    public static final ScalarWeight NON_VIABLE_WEIGHT =
            new ScalarWeight(Double.POSITIVE_INFINITY);

    private static double samenessThreshold = Double.MIN_VALUE;

    private final double value;

    /**
     * Creates a new scalar weight with the given double value.
     * @param value double value
     * @return scalar weight instance
     */
    public static ScalarWeight toWeight(double value) {
        return new ScalarWeight(value);
    }

    /**
     * Creates a new scalar weight with the given double value.
     * @param value double value
     */
    public ScalarWeight(double value) {
        this.value = value;
    }

    @Override
    public Weight merge(Weight otherWeight) {
        return new ScalarWeight(value + ((ScalarWeight) otherWeight).value);
    }

    @Override
    public Weight subtract(Weight otherWeight) {
        return new ScalarWeight(value - ((ScalarWeight) otherWeight).value);
    }

    @Override
    public boolean isViable() {
        return !this.equals(NON_VIABLE_WEIGHT);
    }

    @Override
    public int compareTo(Weight otherWeight) {
        //check equality with samenessThreshold
        if (equals(otherWeight)) {
            return 0;
        }
        return Double.compare(value, ((ScalarWeight) otherWeight).value);
    }

    @Override
    public boolean equals(Object obj) {
        return ((obj instanceof ScalarWeight) &&
                (DoubleMath.fuzzyEquals(value, ((ScalarWeight) obj).value,
                        samenessThreshold)));
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public boolean isNegative() {
        return value < 0;
    }

    @Override
    public String toString() {
        return toStringHelper(this).add("value", value).toString();
    }

    /**
     * Returns inner double value.
     *
     * @return double value
     */
    public double value() {
        return value;
    }

    /**
     * Sets a new sameness threshold for comparing cost values; default is
     * is {@link Double#MIN_VALUE}.
     *
     * @param threshold fractional double value
     */
    public static void setSamenessThreshold(double threshold) {
        samenessThreshold = threshold;
    }

    /**
     * Returns the current sameness threshold for comparing cost values.
     *
     * @return current threshold
     */
    public static double samenessThreshold() {
        return samenessThreshold;
    }
}

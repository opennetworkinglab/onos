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

package org.onlab.graph;

import com.google.common.math.DoubleMath;

import java.util.Objects;

/**
 * Test weight (based on double).
 */
public class TestDoubleWeight implements Weight {

    /**
     * Instance of negative test weight.
     */
    public static final TestDoubleWeight NEGATIVE_WEIGHT = new TestDoubleWeight(-1);

    /**
     * Instance of test weight to mark links/paths which
     * can not be traversed.
     */
    public static final TestDoubleWeight NON_VIABLE_WEIGHT =
            new TestDoubleWeight(Double.POSITIVE_INFINITY);

    private final double value;

    /**
     * Creates a new test weight with the given double value.
     * @param value double weight
     */
    public TestDoubleWeight(double value) {
        this.value = value;
    }

    @Override
    public Weight merge(Weight otherWeight) {
        return new TestDoubleWeight(value + ((TestDoubleWeight) otherWeight).value);
    }

    @Override
    public Weight subtract(Weight otherWeight) {
        return new TestDoubleWeight(value - ((TestDoubleWeight) otherWeight).value);
    }

    @Override
    public boolean isViable() {
        return !this.equals(NON_VIABLE_WEIGHT);
    }

    @Override
    public int compareTo(Weight otherWeight) {
        return Double.compare(value, ((TestDoubleWeight) otherWeight).value);
    }

    @Override
    public boolean equals(Object obj) {
        return (DoubleMath.fuzzyEquals(value, ((TestDoubleWeight) obj).value, 0.1));
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
        return String.valueOf(value);
    }
}

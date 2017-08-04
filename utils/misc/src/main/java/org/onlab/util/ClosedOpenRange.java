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
package org.onlab.util;

import com.google.common.annotations.Beta;
import com.google.common.collect.DiscreteDomain;
import com.google.common.collect.Range;

import java.util.Objects;

/**
 * Represent a closed-open range.
 * The primary user of this class is the ResourceService implementation.
 */
@Beta
public final class ClosedOpenRange {
    private final int lowerBound;   // inclusive
    private final int upperBound;   // exclusive

    /**
     * Creates a range from a Guava's range.
     *
     * @param range Guava's range
     * @return this range
     */
    public static ClosedOpenRange of(Range<Integer> range) {
        return new ClosedOpenRange(
                range.canonical(DiscreteDomain.integers()).lowerEndpoint(),
                range.canonical(DiscreteDomain.integers()).upperEndpoint());
    }

    /**
     * Create a range with a lower bound and an upper bound.
     *
     * @param lowerBound lower bound (inclusive)
     * @param upperBound upper bound (exclusive)
     * @return this range
     */
    public static ClosedOpenRange of(int lowerBound, int upperBound) {
        return new ClosedOpenRange(lowerBound, upperBound);
    }

    private ClosedOpenRange(int lowerBound, int upperBound) {
        this.lowerBound = lowerBound;
        this.upperBound = upperBound;
    }

    /**
     * Returns the lower bound.
     *
     * @return the lower bound
     */
    public int lowerBound() {
        return lowerBound;
    }

    /**
     * Returns the upper bound.
     *
     * @return the upper bound
     */
    public int upperBound() {
        return upperBound;
    }

    @Override
    public int hashCode() {
        return Objects.hash(lowerBound, upperBound);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (!(obj instanceof ClosedOpenRange)) {
            return false;
        }

        final ClosedOpenRange other = (ClosedOpenRange) obj;
        return Objects.equals(this.lowerBound, other.lowerBound)
                && Objects.equals(this.upperBound, other.upperBound);
    }

    @Override
    public String toString() {
        return "[" + lowerBound + ".." + upperBound + ")";
    }
}

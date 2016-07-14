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
package org.onlab.util;

import com.google.common.collect.ComparisonChain;

/**
 * Representation of bandwidth.
 * Use the static factory method corresponding to the unit (like Kbps) you desire on instantiation.
 */
final class LongBandwidth implements Bandwidth {

    private final long bps;

    /**
     * Creates a new instance with given bandwidth.
     *
     * @param bps bandwidth value to be assigned
     */
    LongBandwidth(long bps) {
        this.bps = bps;
    }

    // Constructor for serialization
    private LongBandwidth() {
        this.bps = 0;
    }
    /**
     * Returns bandwidth in bps.
     *
     * @return bandwidth in bps.
     */
    public double bps() {
        return bps;
    }

    /**
     * Returns a Bandwidth whose value is (this + value).
     *
     * @param value value to be added to this Frequency
     * @return this + value
     */
    public Bandwidth add(Bandwidth value) {
        if (value instanceof LongBandwidth) {
            return Bandwidth.bps(this.bps + ((LongBandwidth) value).bps);
        }
        return Bandwidth.bps(this.bps + value.bps());
    }

    /**
     * Returns a Bandwidth whose value is (this - value).
     *
     * @param value value to be added to this Frequency
     * @return this - value
     */
    public Bandwidth subtract(Bandwidth value) {
        if (value instanceof LongBandwidth) {
            return Bandwidth.bps(this.bps - ((LongBandwidth) value).bps);
        }
        return Bandwidth.bps(this.bps - value.bps());
    }

    @Override
    public int compareTo(Bandwidth other) {
        if (other instanceof LongBandwidth) {
            return ComparisonChain.start()
                    .compare(this.bps, ((LongBandwidth) other).bps)
                    .result();
        }
        return ComparisonChain.start()
                .compare(this.bps, other.bps())
                .result();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }

        if (obj instanceof Bandwidth) {
            return this.compareTo((Bandwidth) obj) == 0;
        }

        return false;
    }

    @Override
    public int hashCode() {
        return Long.hashCode(bps);
    }

    @Override
    public String toString() {
        return String.valueOf(this.bps);
    }
}

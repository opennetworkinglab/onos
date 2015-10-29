/*
 * Copyright 2015 Open Networking Laboratory
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

import java.util.Objects;

/**
 * Representation of bandwidth.
 * Use the static factory method corresponding to the unit (like Kbps) you desire on instantiation.
 */
public final class Bandwidth implements RichComparable<Bandwidth> {

    private final double bps;

    /**
     * Creates a new instance with given bandwidth.
     *
     * @param bps bandwidth value to be assigned
     */
    private Bandwidth(double bps) {
        this.bps = bps;
    }

    // Constructor for serialization
    private Bandwidth() {
        this.bps = 0;
    }

    /**
     * Creates a new instance with given bandwidth.
     *
     * @param v         bandwidth value
     * @param unit      {@link DataRateUnit} of {@code v}
     * @return {@link Bandwidth} instance with given bandwidth
     */
    public static Bandwidth of(double v, DataRateUnit unit) {
        return new Bandwidth(unit.toBitsPerSecond(v));
    }

    /**
     * Creates a new instance with given bandwidth in bps.
     *
     * @param bps bandwidth value to be assigned
     * @return {@link Bandwidth} instance with given bandwidth
     */
    public static Bandwidth bps(double bps) {
        return new Bandwidth(bps);
    }

    /**
     * Creates a new instance with given bandwidth in Kbps.
     *
     * @param kbps bandwidth value to be assigned
     * @return {@link Bandwidth} instance with given bandwidth
     */
    public static Bandwidth kbps(double kbps) {
        return bps(kbps * 1_000L);
    }

    /**
     * Creates a new instance with given bandwidth in Mbps.
     *
     * @param mbps bandwidth value to be assigned
     * @return {@link Bandwidth} instance with given bandwidth
     */
    public static Bandwidth mbps(double mbps) {
        return bps(mbps * 1_000_000L);
    }

    /**
     * Creates a new instance with given bandwidth in Gbps.
     *
     * @param gbps bandwidth value to be assigned
     * @return {@link Bandwidth} instance with given bandwidth
     */
    public static Bandwidth gbps(double gbps) {
        return bps(gbps * 1_000_000_000L);
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
        return bps(this.bps + value.bps);
    }

    /**
     * Returns a Bandwidth whose value is (this - value).
     *
     * @param value value to be added to this Frequency
     * @return this - value
     */
    public Bandwidth subtract(Bandwidth value) {
        return bps(this.bps - value.bps);
    }

    @Override
    public int compareTo(Bandwidth other) {
        return ComparisonChain.start()
                .compare(this.bps, other.bps)
                .result();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Bandwidth) {
            Bandwidth that = (Bandwidth) obj;
            return Objects.equals(this.bps, that.bps);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Long.hashCode(Double.doubleToLongBits(bps));
    }

    @Override
    public String toString() {
        return String.valueOf(this.bps);
    }
}

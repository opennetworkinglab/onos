/*
 * Copyright 2015-present Open Networking Foundation
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
public interface Bandwidth extends RichComparable<Bandwidth> {

    /**
     * Creates a new instance with given bandwidth.
     *
     * @param v         bandwidth value
     * @param unit      {@link DataRateUnit} of {@code v}
     * @return {@link Bandwidth} instance with given bandwidth
     */
    static Bandwidth of(long v, DataRateUnit unit) {
        return new LongBandwidth(unit.toBitsPerSecond(v));
    }

    /**
     * Creates a new instance with given bandwidth.
     *
     * @param v         bandwidth value
     * @param unit      {@link DataRateUnit} of {@code v}
     * @return {@link Bandwidth} instance with given bandwidth
     */
    static Bandwidth of(double v, DataRateUnit unit) {
        return new DoubleBandwidth(unit.toBitsPerSecond(v));
    }

    /**
     * Creates a new instance with given bandwidth in bps.
     *
     * @param bps bandwidth value to be assigned
     * @return {@link Bandwidth} instance with given bandwidth
     */
    static Bandwidth bps(long bps) {
        return new LongBandwidth(bps);
    }

    /**
     * Creates a new instance with given bandwidth in bps.
     *
     * @param bps bandwidth value to be assigned
     * @return {@link Bandwidth} instance with given bandwidth
     */
    static Bandwidth bps(double bps) {
        return new DoubleBandwidth(bps);
    }

    /**
     * Creates a new instance with given bandwidth in Kbps.
     *
     * @param kbps bandwidth value to be assigned
     * @return {@link Bandwidth} instance with given bandwidth
     */
    static Bandwidth kbps(long kbps) {
        return bps(kbps * 1_000L);
    }

    /**
     * Creates a new instance with given bandwidth in Kbps.
     *
     * @param kbps bandwidth value to be assigned
     * @return {@link Bandwidth} instance with given bandwidth
     */
    static Bandwidth kbps(double kbps) {
        return bps(kbps * 1_000L);
    }

    /**
     * Creates a new instance with given bandwidth in KBps.
     *
     * @param kBps bandwidth value to be assigned
     * @return {@link Bandwidth} instance with given bandwidth
     */
    static Bandwidth kBps(long kBps) {
        return bps(kBps * 8_000L);
    }

    /**
     * Creates a new instance with given bandwidth in KBps.
     *
     * @param kBps bandwidth value to be assigned
     * @return {@link Bandwidth} instance with given bandwidth
     */
    static Bandwidth kBps(double kBps) {
        return bps(kBps * 8_000L);
    }

    /**
     * Creates a new instance with given bandwidth in Mbps.
     *
     * @param mbps bandwidth value to be assigned
     * @return {@link Bandwidth} instance with given bandwidth
     */
    static Bandwidth mbps(long mbps) {
        return bps(mbps * 1_000_000L);
    }

    /**
     * Creates a new instance with given bandwidth in Mbps.
     *
     * @param mbps bandwidth value to be assigned
     * @return {@link Bandwidth} instance with given bandwidth
     */
    static Bandwidth mbps(double mbps) {
        return bps(mbps * 1_000_000L);
    }

    /**
     * Creates a new instance with given bandwidth in MBps.
     *
     * @param mBps bandwidth value to be assigned
     * @return {@link Bandwidth} instance with given bandwidth
     */
    static Bandwidth mBps(long mBps) {
        return bps(mBps * 8_000_000L);
    }

    /**
     * Creates a new instance with given bandwidth in MBps.
     *
     * @param mBps bandwidth value to be assigned
     * @return {@link Bandwidth} instance with given bandwidth
     */
    static Bandwidth mBps(double mBps) {
        return bps(mBps * 8_000_000L);
    }

    /**
     * Creates a new instance with given bandwidth in Gbps.
     *
     * @param gbps bandwidth value to be assigned
     * @return {@link Bandwidth} instance with given bandwidth
     */
    static Bandwidth gbps(long gbps) {
        return bps(gbps * 1_000_000_000L);
    }

    /**
     * Creates a new instance with given bandwidth in Gbps.
     *
     * @param gbps bandwidth value to be assigned
     * @return {@link Bandwidth} instance with given bandwidth
     */
    static Bandwidth gbps(double gbps) {
        return bps(gbps * 1_000_000_000L);
    }

    /**
     * Creates a new instance with given bandwidth in GBps.
     *
     * @param gBps bandwidth value to be assigned
     * @return {@link Bandwidth} instance with given bandwidth
     */
    static Bandwidth gBps(long gBps) {
        return bps(gBps * 8_000_000_000L);
    }

    /**
     * Creates a new instance with given bandwidth in GBps.
     *
     * @param gBps bandwidth value to be assigned
     * @return {@link Bandwidth} instance with given bandwidth
     */
    static Bandwidth gBps(double gBps) {
        return bps(gBps * 8_000_000_000L);
    }

    /**
     * Returns bandwidth in bps.
     *
     * @return bandwidth in bps.
     */
    double bps();

    /**
     * Returns a Bandwidth whose value is (this + value).
     *
     * @param value value to be added to this Frequency
     * @return this + value
     */
    default Bandwidth add(Bandwidth value) {
        return bps(this.bps() + value.bps());
    }

    /**
     * Returns a Bandwidth whose value is (this - value).
     *
     * @param value value to be added to this Frequency
     * @return this - value
     */
    default Bandwidth subtract(Bandwidth value) {
        return bps(this.bps() - value.bps());
    }

    @Override
    default int compareTo(Bandwidth other) {
        return ComparisonChain.start()
                .compare(this.bps(), other.bps())
                .result();
    }
}

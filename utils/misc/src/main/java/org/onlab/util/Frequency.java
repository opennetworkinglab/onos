/*
 * Copyright 2015-present Open Networking Laboratory
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

import com.google.common.base.MoreObjects;
import com.google.common.collect.ComparisonChain;

/**
 * Class representing frequency. This class is intended to be used for a value whose unit is Hz
 * and its family (KHz, MHz, etc.).
 *
 * <p>
 * Note: this class is mainly intended to be used for lambda, which
 * represents THz order. Long has enough space to represent over THz frequency as Hz,
 * and the underlying value is long as Hz. This means this class can't represent
 * sub-Hz accuracy.
 * </p>
 */
public final class Frequency implements RichComparable<Frequency> {

    private static final long KHZ = 1_000L;
    private static final long MHZ = 1_000_000L;
    private static final long GHZ = 1_000_000_000L;
    private static final long THZ = 1_000_000_000_000L;

    private final long frequency;   // frequency in Hz

    /**
     * Creates an instance representing the specified frequency in Hz.
     *
     * @param frequency frequency in Hz
     */
    private Frequency(long frequency) {
        this.frequency = frequency;
    }

    /**
     * Return the value this instance represents as Hz.
     *
     * @return frequency in Hz
     */
    public long asHz() {
        return frequency;
    }

    /**
     * Return the value this instance represents as KHz.
     *
     * @return frequency in kHz
     */
    public double asKHz() {
        return (double) frequency / KHZ;
    }

    /**
     * Return the value this instance represents as MHz.
     *
     * @return frequency in MHz
     */
    public double asMHz() {
        return (double) frequency / MHZ;
    }

    /**
     * Return the value this instance represents as GHz.
     *
     * @return frequency in GHz
     */
    public double asGHz() {
        return (double) frequency / GHZ;
    }

    /**
     * Return the value this instance represents as THz.
     *
     * @return frequency in THz
     */
    public double asTHz() {
        return (double) frequency / THZ;
    }

    /**
     * Returns an instance representing the specified value in Hz.
     *
     * @param value frequency in Hz
     * @return instance representing the given frequency
     */
    public static Frequency ofHz(long value) {
        return new Frequency(value);
    }

    /**
     * Returns an instance representing the specified value in KHz.
     *
     * @param value frequency in KHz
     * @return instance representing the given frequency
     */
    public static Frequency ofKHz(long value) {
        return new Frequency(value * KHZ);
    }

    /**
     * Returns an instance representing the specified value in KHz.
     *
     * @param value frequency in KHz
     * @return instance representing the given frequency
     */
    public static Frequency ofKHz(double value) {
        return new Frequency((long) (value * KHZ));
    }

    /**
     * Returns an instance representing the specified value in MHz.
     *
     * @param value frequency in MHz
     * @return instance representing the given frequency
     */
    public static Frequency ofMHz(long value) {
        return new Frequency(value * MHZ);
    }

    /**
     * Returns an instance representing the specified value in MHz.
     *
     * @param value frequency in MHz
     * @return instance representing the given frequency
     */
    public static Frequency ofMHz(double value) {
        return new Frequency((long) (value * MHZ));
    }

    /**
     * Returns an instance representing the specified value in GHz.
     *
     * @param value frequency in GHz
     * @return instance representing the given frequency
     */
    public static Frequency ofGHz(long value) {
        return new Frequency(value * GHZ);
    }

    /**
     * Returns an instance representing the specified value in GHz.
     *
     * @param value frequency in GHz
     * @return instance representing the given frequency
     */
    public static Frequency ofGHz(double value) {
        return new Frequency((long) (value * GHZ));
    }

    /**
     * Returns an instance representing the specified value in THz.
     *
     * @param value frequency in THz
     * @return instance representing the given frequency
     */
    public static Frequency ofTHz(long value) {
        return new Frequency(value * THZ);
    }

    /**
     * Returns an instance representing the specified value in THz.
     *
     * @param value frequency in THz
     * @return instance representing the given frequency
     */
    public static Frequency ofTHz(double value) {
        return new Frequency((long) (value * THZ));
    }

    /**
     * Returns a Frequency whose value is (this + value).
     *
     * @param value value to be added to this Frequency
     * @return this + value
     */
    public Frequency add(Frequency value) {
        return new Frequency(this.frequency + value.frequency);
    }

    /**
     * Returns a Frequency whose value is (this - value).
     *
     * @param value value to be subtracted from this Frequency
     * @return this - value
     */
    public Frequency subtract(Frequency value) {
        return new Frequency(this.frequency - value.frequency);
    }

    /**
     * Returns a Frequency whose value is (this * value).
     *
     * @param value value to be multiplied by this Frequency
     * @return this * value
     */
    public Frequency multiply(long value) {
        return new Frequency(this.frequency * value);
    }

    /**
     * Returns a Frequency whose value is Math.floorDiv(this, value).
     *
     * @param value value to be divided by this Frequency
     * @return Math.floorDiv(this, value)
     */
    public Frequency floorDivision(long value) {
        return new Frequency(Math.floorDiv(this.frequency, value));
    }

    @Override
    public int compareTo(Frequency other) {
        return ComparisonChain.start()
                .compare(this.frequency, other.frequency)
                .result();
    }

    @Override
    public int hashCode() {
        return Long.hashCode(frequency);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (!(obj instanceof Frequency)) {
            return false;
        }

        final Frequency other = (Frequency) obj;
        return this.frequency == other.frequency;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("frequency", frequency + "Hz")
                .toString();
    }
}

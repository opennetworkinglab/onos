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

import com.google.common.annotations.Beta;

/**
 * Data rate unit.
 */
@Beta
public enum DataRateUnit {
    /**
     * Bit per second.
     */
    BPS(1L),
    /**
     * Kilobit per second.
     * (Decimal/SI)
     */
    KBPS(1_000L),
    /**
     * Megabit per second.
     * (Decimal/SI)
     */
    MBPS(1_000_000L),
    /**
     * Gigabit per second.
     * (Decimal/SI)
     */
    GBPS(1_000_000_000L);

    private final long multiplier;

    DataRateUnit(long multiplier) {
        this.multiplier = multiplier;
    }

    /**
     * Returns the multiplier to use, when converting value of this unit to bps.
     *
     * @return multiplier
     */
    public long multiplier() {
        return multiplier;
    }

    /**
     * Converts given value in this unit to bits per seconds.
     *
     * @param v data rate value
     * @return {@code v} in bits per seconds
     */
    public long toBitsPerSecond(long v) {
        return v * multiplier;
    }

    /**
     * Converts given value in this unit to bits per seconds.
     *
     * @param v data rate value
     * @return {@code v} in bits per seconds
     */
    public double toBitsPerSecond(double v) {
        return v * multiplier;
    }
}

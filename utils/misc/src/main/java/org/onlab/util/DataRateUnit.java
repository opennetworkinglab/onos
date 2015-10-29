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

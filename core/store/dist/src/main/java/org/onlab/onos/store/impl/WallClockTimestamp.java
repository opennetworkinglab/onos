package org.onlab.onos.store.impl;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.Objects;

import org.onlab.onos.store.Timestamp;

import com.google.common.base.MoreObjects;
import com.google.common.collect.ComparisonChain;

/**
 * A Timestamp that derives its value from the prevailing
 * wallclock time on the controller where it is generated.
 */
public class WallClockTimestamp implements Timestamp {

    private final long unixTimestamp;

    public WallClockTimestamp() {
        unixTimestamp = System.currentTimeMillis();
    }

    @Override
    public int compareTo(Timestamp o) {
        checkArgument(o instanceof WallClockTimestamp,
                "Must be WallClockTimestamp", o);
        WallClockTimestamp that = (WallClockTimestamp) o;

        return ComparisonChain.start()
                .compare(this.unixTimestamp, that.unixTimestamp)
                .result();
    }
    @Override
    public int hashCode() {
        return Objects.hash(unixTimestamp);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof WallClockTimestamp)) {
            return false;
        }
        WallClockTimestamp that = (WallClockTimestamp) obj;
        return Objects.equals(this.unixTimestamp, that.unixTimestamp);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                    .add("unixTimestamp", unixTimestamp)
                    .toString();
    }

    /**
     * Returns the unixTimestamp.
     *
     * @return unix timestamp
     */
    public long unixTimestamp() {
        return unixTimestamp;
    }
}

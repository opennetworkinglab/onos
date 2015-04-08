package org.onosproject.store.impl;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.Objects;

import org.onosproject.store.Timestamp;

import com.google.common.base.MoreObjects;
import com.google.common.collect.ComparisonChain;

/**
 * Timestamp based on logical sequence value.
 * <p>
 * LogicalTimestamps are ordered by their sequence values.
 */
public class LogicalTimestamp implements Timestamp {

    private final long value;

    public LogicalTimestamp(long value) {
        this.value = value;
    }

    @Override
    public int compareTo(Timestamp o) {
        checkArgument(o instanceof LogicalTimestamp,
                "Must be LogicalTimestamp", o);
        LogicalTimestamp that = (LogicalTimestamp) o;

        return ComparisonChain.start()
                .compare(this.value, that.value)
                .result();
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof LogicalTimestamp)) {
            return false;
        }
        LogicalTimestamp that = (LogicalTimestamp) obj;
        return Objects.equals(this.value, that.value);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                    .add("value", value)
                    .toString();
    }

    /**
     * Returns the sequence value.
     *
     * @return sequence value
     */
    public long value() {
        return this.value;
    }
}

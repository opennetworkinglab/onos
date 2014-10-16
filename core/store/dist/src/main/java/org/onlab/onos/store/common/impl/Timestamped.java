package org.onlab.onos.store.common.impl;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Objects;

import org.onlab.onos.store.Timestamp;

import com.google.common.base.MoreObjects;

/**
 * Wrapper class to store Timestamped value.
 *
 * @param <T> Timestamped value type
 */
public final class Timestamped<T> {

    private final Timestamp timestamp;
    private final T value;

    /**
     * Creates a time stamped value.
     *
     * @param value to be timestamp
     * @param timestamp the timestamp
     */
    public Timestamped(T value, Timestamp timestamp) {
        this.value = checkNotNull(value);
        this.timestamp = checkNotNull(timestamp);
    }

    /**
     * Returns the value.
     *
     * @return value
     */
    public T value() {
        return value;
    }

    /**
     * Returns the time stamp.
     *
     * @return time stamp
     */
    public Timestamp timestamp() {
        return timestamp;
    }

    /**
     * Tests if this timestamped value is newer than the other.
     *
     * @param other timestamped value
     * @return true if this instance is newer.
     */
    public boolean isNewer(Timestamped<T> other) {
        return isNewer(checkNotNull(other).timestamp());
    }

    /**
     * Tests if this timestamp is newer than the specified timestamp.
     * @param other timestamp to compare against
     * @return true if this instance is newer
     */
    public boolean isNewer(Timestamp other) {
        return this.timestamp.compareTo(checkNotNull(other)) > 0;
    }

    @Override
    public int hashCode() {
        return timestamp.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof Timestamped)) {
            return false;
        }
        @SuppressWarnings("unchecked")
        Timestamped<T> that = (Timestamped<T>) obj;
        return Objects.equals(this.timestamp, that.timestamp);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                    .add("timestamp", timestamp)
                    .add("value", value)
                    .toString();
    }

    // Default constructor for serialization
    @Deprecated
    private Timestamped() {
        this.value = null;
        this.timestamp = null;
    }
}

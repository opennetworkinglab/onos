package org.onosproject.store.ecmap;

import org.onosproject.store.Timestamp;
import com.google.common.base.MoreObjects;

/**
 * Representation of a value in EventuallyConsistentMap.
 *
 * @param <V> value type
 */
public class MapValue<V> implements Comparable<MapValue<V>> {
    private final Timestamp timestamp;
    private final V value;

    public MapValue(V value, Timestamp timestamp) {
        this.value = value;
        this.timestamp = timestamp;
    }

    public boolean isTombstone() {
        return value == null;
    }

    public boolean isAlive() {
        return value != null;
    }

    public Timestamp timestamp() {
        return timestamp;
    }

    public V get() {
        return value;
    }

    @Override
    public int compareTo(MapValue<V> o) {
        return this.timestamp.compareTo(o.timestamp);
    }

    public boolean isNewerThan(MapValue<V> other) {
        return timestamp.isNewerThan(other.timestamp);
    }

    public boolean isNewerThan(Timestamp timestamp) {
        return timestamp.isNewerThan(timestamp);
    }

    public Digest digest() {
        return new Digest(timestamp, isTombstone());
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("timestamp", timestamp)
                .add("value", value)
                .toString();
    }

    @SuppressWarnings("unused")
    private MapValue() {
        this.timestamp = null;
        this.value = null;
    }

    /**
     * Digest or summary of a MapValue for use during Anti-Entropy exchanges.
     */
    public static class Digest {
        private final Timestamp timestamp;
        private final boolean isTombstone;

        public Digest(Timestamp timestamp, boolean isTombstone) {
            this.timestamp = timestamp;
            this.isTombstone = isTombstone;
        }

        public Timestamp timestamp() {
            return timestamp;
        }

        public boolean isTombstone() {
            return isTombstone;
        }

        public boolean isNewerThan(Digest other) {
            return timestamp.isNewerThan(other.timestamp);
        }

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(getClass())
                    .add("timestamp", timestamp)
                    .add("isTombstone", isTombstone)
                    .toString();
        }
    }
}

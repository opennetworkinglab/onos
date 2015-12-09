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
package org.onosproject.store.ecmap;

import org.onosproject.store.Timestamp;

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Representation of a value in EventuallyConsistentMap.
 *
 * @param <V> value type
 */
public class MapValue<V> implements Comparable<MapValue<V>> {
    private final Timestamp timestamp;
    private final V value;

    /**
     * Creates a tombstone value with the specified timestamp.
     * @param timestamp timestamp for tombstone
     * @return tombstone MapValue
     *
     * @param <U> value type
     */
    public static <U> MapValue<U> tombstone(Timestamp timestamp) {
        return new MapValue<>(null, timestamp);
    }

    public MapValue(V value, Timestamp timestamp) {
        this.value = value;
        this.timestamp = checkNotNull(timestamp, "Timestamp cannot be null");
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
        if (other == null) {
            return true;
        }
        return this.timestamp.isNewerThan(other.timestamp);
    }

    public boolean isNewerThan(Timestamp timestamp) {
        return this.timestamp.isNewerThan(timestamp);
    }

    public Digest digest() {
        return new Digest(timestamp, isTombstone());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(timestamp, value);
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean equals(Object other) {
        if (other instanceof MapValue) {
            MapValue<V> that = (MapValue<V>) other;
            return Objects.equal(this.timestamp, that.timestamp) &&
                    Objects.equal(this.value, that.value);
        }
        return false;
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
        public int hashCode() {
            return Objects.hashCode(timestamp, isTombstone);
        }

        @Override
        public boolean equals(Object other) {
            if (other instanceof Digest) {
                Digest that = (Digest) other;
                return Objects.equal(this.timestamp, that.timestamp) &&
                        Objects.equal(this.isTombstone, that.isTombstone);
            }
            return false;
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

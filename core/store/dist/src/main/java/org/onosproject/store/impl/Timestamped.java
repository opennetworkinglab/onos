/*
 * Copyright 2014-present Open Networking Laboratory
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
package org.onosproject.store.impl;

import com.google.common.base.MoreObjects;
import org.onosproject.store.Timestamp;

import java.util.Objects;

import static com.google.common.base.Preconditions.checkNotNull;

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
        return isNewerThan(checkNotNull(other).timestamp());
    }

    /**
     * Tests if this timestamp is newer than the specified timestamp.
     *
     * @param other timestamp to compare against
     * @return true if this instance is newer
     */
    public boolean isNewerThan(Timestamp other) {
        return timestamp.isNewerThan(other);
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

    /**
     * Constructs an empty object. Required for serialization.
     */
    private Timestamped() {
        this.value = null;
        this.timestamp = null;
    }
}

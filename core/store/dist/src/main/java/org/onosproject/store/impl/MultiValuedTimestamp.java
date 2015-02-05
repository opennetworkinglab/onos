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
package org.onosproject.store.impl;

import com.google.common.base.MoreObjects;
import com.google.common.collect.ComparisonChain;
import org.onosproject.store.Timestamp;

import java.util.Objects;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * A logical timestamp that derives its value from two input values. Value1
 * always takes precedence over value2 when comparing timestamps.
 */
public class MultiValuedTimestamp implements Timestamp {

    private final Timestamp timestamp;
    private final long value2;

    /**
     * Creates a new timestamp based on two values. The first value has higher
     * precedence than the second when comparing timestamps.
     *
     * @param timestamp first value
     * @param value2 second value
     */
    public MultiValuedTimestamp(Timestamp timestamp, long value2) {
        this.timestamp = timestamp;
        this.value2 = value2;
    }

    @Override
    public int compareTo(Timestamp o) {
        checkArgument(o instanceof MultiValuedTimestamp,
                      "Must be MultiValuedTimestamp", o);
        MultiValuedTimestamp that = (MultiValuedTimestamp) o;

        return ComparisonChain.start()
                .compare(this.timestamp, that.timestamp)
                .compare(this.value2, that.value2)
                .result();
    }

    @Override
    public int hashCode() {
        return Objects.hash(timestamp, value2);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof MultiValuedTimestamp)) {
            return false;
        }
        MultiValuedTimestamp that = (MultiValuedTimestamp) obj;
        return Objects.equals(this.timestamp, that.timestamp) &&
                Objects.equals(this.value2, that.value2);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("timestamp", timestamp)
                .add("value2", value2)
                .toString();
    }

    /**
     * Returns the first value.
     *
     * @return first value
     */
    public Timestamp timestamp() {
        return timestamp;
    }

    /**
     * Returns the second value.
     *
     * @return second value
     */
    public long sequenceNumber() {
        return value2;
    }

    // Default constructor for serialization
    @SuppressWarnings("unused")
    private MultiValuedTimestamp() {
        this.timestamp = null;
        this.value2 = -1;
    }
}

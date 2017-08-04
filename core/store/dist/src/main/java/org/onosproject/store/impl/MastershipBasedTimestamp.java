/*
 * Copyright 2014-present Open Networking Foundation
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

import static com.google.common.base.Preconditions.checkArgument;

import java.util.Objects;

import org.onosproject.store.Timestamp;

import com.google.common.base.MoreObjects;
import com.google.common.collect.ComparisonChain;

/**
 * A logical timestamp that derives its value from two things:
 * <ul>
 * <li> The current mastership term of the device.</li>
 * <li> The value of the counter used for tracking topology events observed from
 * the device during that current time of a device. </li>
 * </ul>
 */
public final class MastershipBasedTimestamp implements Timestamp {

    private final long termNumber;
    private final long sequenceNumber;

    /**
     * Default constructor for serialization.
     */
    protected MastershipBasedTimestamp() {
        this.termNumber = -1;
        this.sequenceNumber = -1;
    }

    /**
     * Default version tuple.
     *
     * @param termNumber the mastership termNumber
     * @param sequenceNumber  the sequenceNumber number within the termNumber
     */
    public MastershipBasedTimestamp(long termNumber, long sequenceNumber) {
        this.termNumber = termNumber;
        this.sequenceNumber = sequenceNumber;
    }

    @Override
    public int compareTo(Timestamp o) {
        checkArgument(o instanceof MastershipBasedTimestamp,
                "Must be MastershipBasedTimestamp", o);
        MastershipBasedTimestamp that = (MastershipBasedTimestamp) o;

        return ComparisonChain.start()
                .compare(this.termNumber, that.termNumber)
                .compare(this.sequenceNumber, that.sequenceNumber)
                .result();
    }

    @Override
    public int hashCode() {
        return Objects.hash(termNumber, sequenceNumber);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof MastershipBasedTimestamp)) {
            return false;
        }
        MastershipBasedTimestamp that = (MastershipBasedTimestamp) obj;
        return Objects.equals(this.termNumber, that.termNumber) &&
                Objects.equals(this.sequenceNumber, that.sequenceNumber);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                    .add("termNumber", termNumber)
                    .add("sequenceNumber", sequenceNumber)
                    .toString();
    }

    /**
     * Returns the termNumber.
     *
     * @return termNumber
     */
    public long termNumber() {
        return termNumber;
    }

    /**
     * Returns the sequenceNumber number.
     *
     * @return sequenceNumber
     */
    public long sequenceNumber() {
        return sequenceNumber;
    }

}

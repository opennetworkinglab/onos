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
 * A Timestamp that derives its value from the system clock time (in ns)
 * on the controller where it is generated.
 */
public class SystemClockTimestamp implements Timestamp {

    private final long unixTimestamp;

    public SystemClockTimestamp() {
        unixTimestamp = System.nanoTime();
    }

    @Override
    public int compareTo(Timestamp o) {
        checkArgument(o instanceof SystemClockTimestamp,
                "Must be SystemClockTimestamp", o);
        SystemClockTimestamp that = (SystemClockTimestamp) o;

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
        if (!(obj instanceof SystemClockTimestamp)) {
            return false;
        }
        SystemClockTimestamp that = (SystemClockTimestamp) obj;
        return Objects.equals(this.unixTimestamp, that.unixTimestamp);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                    .add("unixTimestamp", unixTimestamp)
                    .toString();
    }

    public long systemTimestamp() {
        return unixTimestamp;
    }
}

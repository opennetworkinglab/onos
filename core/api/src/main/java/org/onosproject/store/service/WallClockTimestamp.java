/*
 * Copyright 2015-present Open Networking Laboratory
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
package org.onosproject.store.service;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.Objects;

import org.onosproject.store.Timestamp;

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

    public WallClockTimestamp(long timestamp) {
        unixTimestamp = timestamp;
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
        return Long.hashCode(unixTimestamp);
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

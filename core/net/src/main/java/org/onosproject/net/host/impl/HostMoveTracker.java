/*
 * Copyright 2015-present Open Networking Foundation
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

package org.onosproject.net.host.impl;

import org.onosproject.net.HostLocation;

import java.util.Objects;
import java.util.Set;

import static com.google.common.base.MoreObjects.toStringHelper;

/**
 * Used for tracking of the host move.
 */
public class HostMoveTracker {
    private Integer counter;
    private Long timeStamp;
    private Set<HostLocation> locations;

    /**
     * Initialize the instance of HostMoveTracker.
     *
     * @param locations List of locations where host is present
     */
    public HostMoveTracker(Set<HostLocation> locations) {
        counter = 1;
        timeStamp = System.currentTimeMillis();
        this.locations = locations;
    }

    /**
     * Updates locations in HostMoveTracker.
     *
     * @param locations List of locations where host is present
     */
    public void updateHostMoveTracker(Set<HostLocation> locations) {
        counter += 1;
        timeStamp = System.currentTimeMillis();
        this.locations = locations;
    }

    /**
     * Reset hostmove count,timestamp and updated locations.
     *
     * @param locations List of locations where host is present
     */
    public void resetHostMoveTracker(Set<HostLocation> locations) {
        counter = 0;
        timeStamp = System.currentTimeMillis();
        this.locations = locations;
    }

    /**
     * Reset hostmove count and timestamp.
     */
    public void resetHostMoveTracker() {
        counter = 0;
        timeStamp = System.currentTimeMillis();
    }

    public Long getTimeStamp() {
        return timeStamp;
    }

    public Integer getCounter() {
        return counter;
    }


    public Set<HostLocation> getLocations() {
        return locations;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        HostMoveTracker that = (HostMoveTracker) o;
        return Objects.equals(locations, that.locations) &&
                Objects.equals(counter, that.counter);
    }

    @Override
    public int hashCode() {

        return Objects.hash(locations, counter);
    }

    @Override
    public String toString() {
        return toStringHelper(this)
                .add("counter", getCounter())
                .add("timeStamp", getTimeStamp())
                .add("locations", getLocations())
                .toString();
    }
}

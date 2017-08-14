/*
 * Copyright 2017-present Open Networking Foundation
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

package org.onosproject.store.host.impl;

import org.onosproject.net.HostId;
import org.onosproject.net.HostLocation;

import java.util.Objects;

import static com.google.common.base.MoreObjects.toStringHelper;

/**
 * Internal data structure to record the info of a host with location that is under verification.
 */
class PendingHostLocation {
    private HostId hostId;
    private HostLocation location;
    private boolean expired;

    /**
     * Constructs PendingHostLocation.
     *
     * @param hostId Host ID
     * @param location location to be verified
     */
    PendingHostLocation(HostId hostId, HostLocation location) {
        this.hostId = hostId;
        this.location = location;
        this.expired = false;
    }

    /**
     * Gets HostId of this entry.
     *
     * @return host id
     */
    HostId hostId() {
        return hostId;
    }

    /**
     * Gets HostLocation of this entry.
     *
     * @return host location
     */
    HostLocation location() {
        return location;
    }

    /**
     * Determine whether this probe is expired or not.
     *
     * @return true if this entry is expired and waiting to be removed from the cache
     */
    boolean expired() {
        return expired;
    }

    /**
     * Sets whether this probe is expired or not.
     *
     * @param expired true if this entry is expired and waiting to be removed from the cache
     */
    void setExpired(boolean expired) {
        this.expired = expired;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof PendingHostLocation)) {
            return false;
        }
        PendingHostLocation that = (PendingHostLocation) o;
        return (Objects.equals(this.hostId, that.hostId) &&
                Objects.equals(this.location, that.location) &&
                Objects.equals(this.expired, that.expired));
    }
    @Override
    public int hashCode() {
        return Objects.hash(hostId, location, expired);
    }

    @Override
    public String toString() {
        return toStringHelper(getClass())
                .add("hostId", hostId)
                .add("location", location)
                .add("expired", expired)
                .toString();
    }
}

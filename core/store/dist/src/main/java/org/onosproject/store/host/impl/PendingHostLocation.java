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

import org.onosproject.net.ConnectPoint;
import org.onosproject.net.HostId;
import org.onosproject.net.host.HostLocationProbingService.ProbeMode;

import java.util.Objects;

import static com.google.common.base.MoreObjects.toStringHelper;

/**
 * Internal data structure to record the info of a host with location that is under verification.
 */
class PendingHostLocation {
    private HostId hostId;
    private ConnectPoint connectPoint;
    private boolean expired;
    private ProbeMode probeMode;

    /**
     * Constructs PendingHostLocation.
     *
     * @param hostId Host ID
     * @param connectPoint location to be verified
     * @param probeMode probe mode
     */
    PendingHostLocation(HostId hostId, ConnectPoint connectPoint, ProbeMode probeMode) {
        this.hostId = hostId;
        this.connectPoint = connectPoint;
        this.expired = false;
        this.probeMode = probeMode;
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
     * Gets connect point of this entry.
     *
     * @return connect point
     */
    ConnectPoint connectPoint() {
        return connectPoint;
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

    /**
     * Gets probe mode of this entry.
     *
     * @return probe mode
     */
    ProbeMode probeMode() {
        return probeMode;
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
                Objects.equals(this.connectPoint, that.connectPoint) &&
                Objects.equals(this.expired, that.expired) &&
                Objects.equals(this.probeMode, that.probeMode));
    }
    @Override
    public int hashCode() {
        return Objects.hash(hostId, connectPoint, expired, probeMode);
    }

    @Override
    public String toString() {
        return toStringHelper(getClass())
                .add("hostId", hostId)
                .add("location", connectPoint)
                .add("expired", expired)
                .add("probeMode", probeMode)
                .toString();
    }
}

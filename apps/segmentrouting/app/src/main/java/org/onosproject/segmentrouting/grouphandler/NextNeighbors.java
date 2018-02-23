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

package org.onosproject.segmentrouting.grouphandler;

import static com.google.common.base.MoreObjects.toStringHelper;

import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.onosproject.net.DeviceId;

/**
 * Represents the nexthop information associated with a route-path towards a
 * set of destinations.
 */
public class NextNeighbors {
    private final Map<DeviceId, Set<DeviceId>> dstNextHops;
    private final int nextId;

    /**
     * Constructor.
     *
     * @param dstNextHops map of destinations and the next-hops towards each dest
     * @param nextId id of nextObjective that manifests the next-hop info
     */
    public NextNeighbors(Map<DeviceId, Set<DeviceId>> dstNextHops, int nextId) {
        this.dstNextHops = dstNextHops;
        this.nextId = nextId;
    }

    /**
     * Returns a map of destinations and the next-hops towards them.
     *
     * @return map of destinations and the next-hops towards them
     */
    public Map<DeviceId, Set<DeviceId>> dstNextHops() {
        return dstNextHops;
    }

    /**
     * Set of next-hops towards the given destination.
     *
     * @param deviceId the destination
     * @return set of nexthops towards the destination
     */
    public Set<DeviceId> nextHops(DeviceId deviceId) {
        return dstNextHops.get(deviceId);
    }

    /**
     * Return the nextId representing the nextObjective towards the next-hops.
     *
     * @return nextId representing the nextObjective towards the next-hops
     */
    public int nextId() {
        return nextId;
    }

    /**
     * Checks if the given nextHopId is a valid next hop to any one of the
     * destinations.
     *
     * @param nextHopId the deviceId for the next hop
     * @return true if given next
     */
    public boolean containsNextHop(DeviceId nextHopId) {
        for (Set<DeviceId> nextHops : dstNextHops.values()) {
            if (nextHops != null && nextHops.contains(nextHopId)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns a set of destinations which have the given nextHopId as one
     * of the next-hops to that destination.
     *
     * @param nextHopId the deviceId for the next hop
     * @return set of deviceIds that have the given nextHopId as a next-hop
     *          which could be empty if no destinations were found
     */
    public Set<DeviceId> getDstForNextHop(DeviceId nextHopId) {
        Set<DeviceId> dstSet = new HashSet<>();
        for (DeviceId dstKey : dstNextHops.keySet()) {
            if (dstNextHops.get(dstKey).contains(nextHopId)) {
                dstSet.add(dstKey);
            }
        }
        return dstSet;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof NextNeighbors)) {
            return false;
        }
        NextNeighbors that = (NextNeighbors) o;
        return (this.nextId == that.nextId) &&
                this.dstNextHops.equals(that.dstNextHops);
    }

    @Override
    public int hashCode() {
        return Objects.hash(nextId, dstNextHops);
    }

    @Override
    public String toString() {
        return toStringHelper(this)
                .add("nextId", nextId)
                .add("dstNextHops", dstNextHops)
                .toString();
    }
}

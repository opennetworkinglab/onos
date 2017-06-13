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

public class NextNeighbors {
    private final Map<DeviceId, Set<DeviceId>> dstNextHops;
    private final int nextId;

    public NextNeighbors(Map<DeviceId, Set<DeviceId>> dstNextHops, int nextId) {
        this.dstNextHops = dstNextHops;
        this.nextId = nextId;
    }

    public Map<DeviceId, Set<DeviceId>> dstNextHops() {
        return dstNextHops;
    }

    public Set<DeviceId> nextHops(DeviceId deviceId) {
        return dstNextHops.get(deviceId);
    }

    public int nextId() {
        return nextId;
    }

    public boolean containsNextHop(DeviceId nextHopId) {
        for (Set<DeviceId> nextHops : dstNextHops.values()) {
            if (nextHops != null && nextHops.contains(nextHopId)) {
                return true;
            }
        }
        return false;
    }

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

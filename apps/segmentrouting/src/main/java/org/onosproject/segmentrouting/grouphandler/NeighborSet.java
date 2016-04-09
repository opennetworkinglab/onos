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

package org.onosproject.segmentrouting.grouphandler;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import org.onosproject.net.DeviceId;

/**
 * Representation of a set of neighbor switch dpids along with edge node
 * label. Meant to be used as a lookup-key in a hash-map to retrieve an
 * ECMP-group that hashes packets to a set of ports connecting to the
 * neighbors in this set.
 */
public class NeighborSet {
    private final Set<DeviceId> neighbors;
    private final int edgeLabel;
    public static final int NO_EDGE_LABEL = -1;

    /**
     * Constructor with set of neighbors. Edge label is
     * default to -1.
     *
     * @param neighbors set of neighbors to be part of neighbor set
     */
    public NeighborSet(Set<DeviceId> neighbors) {
        checkNotNull(neighbors);
        this.edgeLabel = NO_EDGE_LABEL;
        this.neighbors = new HashSet<>();
        this.neighbors.addAll(neighbors);
    }

    /**
     * Constructor with set of neighbors and edge label.
     *
     * @param neighbors set of neighbors to be part of neighbor set
     * @param edgeLabel label to be pushed as part of group operation
     */
    public NeighborSet(Set<DeviceId> neighbors, int edgeLabel) {
        checkNotNull(neighbors);
        this.edgeLabel = edgeLabel;
        this.neighbors = new HashSet<>();
        this.neighbors.addAll(neighbors);
    }

    /**
     * Default constructor for kryo serialization.
     */
    public NeighborSet() {
        this.edgeLabel = NO_EDGE_LABEL;
        this.neighbors = new HashSet<>();
    }

    /**
     * Gets the neighbors part of neighbor set.
     *
     * @return set of neighbor identifiers
     */
    public Set<DeviceId> getDeviceIds() {
        return neighbors;
    }

    /**
     * Gets the label associated with neighbor set.
     *
     * @return integer
     */
    public int getEdgeLabel() {
        return edgeLabel;
    }

    // The list of neighbor ids and label are used for comparison.
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof NeighborSet)) {
            return false;
        }
        NeighborSet that = (NeighborSet) o;
        return (this.neighbors.containsAll(that.neighbors) &&
                that.neighbors.containsAll(this.neighbors) &&
                (this.edgeLabel == that.edgeLabel));
    }

    // The list of neighbor ids and label are used for comparison.
    @Override
    public int hashCode() {
        int result = 17;
        int combinedHash = 0;
        for (DeviceId d : neighbors) {
            combinedHash = combinedHash + Objects.hash(d);
        }
        result = 31 * result + combinedHash + Objects.hash(edgeLabel);

        return result;
    }

    @Override
    public String toString() {
        return " Neighborset Sw: " + neighbors
                + " and Label: " + edgeLabel;
    }
}

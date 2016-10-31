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

import org.onosproject.net.DeviceId;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import static com.google.common.base.MoreObjects.toStringHelper;
import static com.google.common.base.Preconditions.checkNotNull;

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
    private boolean mplsSet;

    /**
     * Constructor with set of neighbors. Edge label is
     * default to -1.
     *
     * @param neighbors set of neighbors to be part of neighbor set
     * @param isMplsSet indicates if it is a mpls neighbor set
     */
    public NeighborSet(Set<DeviceId> neighbors, boolean isMplsSet) {
        checkNotNull(neighbors);
        this.edgeLabel = NO_EDGE_LABEL;
        this.neighbors = new HashSet<>();
        this.neighbors.addAll(neighbors);
        this.mplsSet = isMplsSet;
    }

    /**
     * Constructor with set of neighbors and edge label.
     *
     * @param neighbors set of neighbors to be part of neighbor set
     * @param isMplsSet indicates if it is a mpls neighbor set
     * @param edgeLabel label to be pushed as part of group operation
     */
    public NeighborSet(Set<DeviceId> neighbors, boolean isMplsSet, int edgeLabel) {
        checkNotNull(neighbors);
        this.edgeLabel = edgeLabel;
        this.neighbors = new HashSet<>();
        this.neighbors.addAll(neighbors);
        this.mplsSet = isMplsSet;
    }

    /**
     * Default constructor for kryo serialization.
     */
    public NeighborSet() {
        this.edgeLabel = NO_EDGE_LABEL;
        this.neighbors = new HashSet<>();
        this.mplsSet = true;
    }

    /**
     * Factory method for NeighborSet hierarchy.
     *
     * @param random the expected behavior.
     * @param neighbors the set of neighbors to be part of neighbor set
     * @param isMplsSet indicates if it is a mpls neighbor set
     * @return the neighbor set object.
     */
    public static NeighborSet neighborSet(boolean random, Set<DeviceId> neighbors, boolean isMplsSet) {
        return random ?
                new RandomNeighborSet(neighbors) :
                new NeighborSet(neighbors, isMplsSet);
    }

    /**
     * Factory method for NeighborSet hierarchy.
     *
     * @param random the expected behavior.
     * @param neighbors the set of neighbors to be part of neighbor set
     * @param isMplsSet indicates if it is a mpls neighbor set
     * @param edgeLabel label to be pushed as part of group operation
     * @return the neighbor set object
     */
    public static NeighborSet neighborSet(boolean random, Set<DeviceId> neighbors, boolean isMplsSet, int edgeLabel) {
        return random ?
                new RandomNeighborSet(neighbors, edgeLabel) :
                new NeighborSet(neighbors, isMplsSet, edgeLabel);
    }

    /**
     * Factory method for NeighborSet hierarchy.
     *
     * @param random the expected behavior.
     * @return the neighbor set object
     */
    public static NeighborSet neighborSet(boolean random) {
        return random ? new RandomNeighborSet() : new NeighborSet();
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

    /**
     * Gets the first neighbor of the set. The default
     * implementation assure the first neighbor is the
     * first of the set. Subclasses can modify this.
     *
     * @return the first neighbor of the set
     */
    public DeviceId getFirstNeighbor() {
        return neighbors.isEmpty() ? DeviceId.NONE : neighbors.iterator().next();
    }

    /**
     * Gets the value of mplsSet.
     *
     * @return the value of mplsSet
     */
    public boolean mplsSet() {
        return mplsSet;
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
                (this.edgeLabel == that.edgeLabel) &&
                (this.mplsSet == that.mplsSet));
    }

    // The list of neighbor ids and label are used for comparison.
    @Override
    public int hashCode() {
        return Objects.hash(neighbors, edgeLabel, mplsSet);
    }

    @Override
    public String toString() {
        return toStringHelper(this)
                .add("Neighborset Sw", neighbors)
                .add("Label", edgeLabel)
                .add("MplsSet", mplsSet)
                .toString();
    }
}

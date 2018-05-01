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

import org.onosproject.net.DeviceId;
import org.slf4j.Logger;

import com.google.common.base.MoreObjects.ToStringHelper;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import static com.google.common.base.MoreObjects.toStringHelper;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Representation of a set of destination switch dpids along with their
 * edge-node labels. Meant to be used as a lookup-key in a hash-map to retrieve
 * an ECMP-group that hashes packets towards a specific destination switch, or
 * paired-destination switches. May also be used to represent cases where the
 * forwarding does not use ECMP groups (ie SIMPLE next objectives)
 */
public final class DestinationSet {
    public static final int NO_EDGE_LABEL = -1;
    private static final int NOT_ASSIGNED = 0;
    private final DeviceId dstSw1;
    private final int edgeLabel1;
    private final DeviceId dstSw2;
    private final int edgeLabel2;
    private final DestinationSetType typeOfDstSet;

    private static final Logger log = getLogger(DestinationSet.class);

    /**
     * Constructor for a single destination with no Edge label.
     *
     * @param dsType type of next objective
     * @param dstSw the destination switch
     */
    private DestinationSet(DestinationSetType dsType, DeviceId dstSw) {
        this.edgeLabel1 = NO_EDGE_LABEL;
        this.dstSw1 = dstSw;
        this.edgeLabel2 = NOT_ASSIGNED;
        this.dstSw2 = null;
        this.typeOfDstSet = dsType;
    }

    /**
     * Constructor for a single destination with Edge label.
     *
     * @param dsType type of next objective
     * @param edgeLabel label to be pushed as part of group operation
     * @param dstSw the destination switch
     */
    private DestinationSet(DestinationSetType dsType,
                          int edgeLabel, DeviceId dstSw) {
        this.edgeLabel1 = edgeLabel;
        this.dstSw1 = dstSw;
        this.edgeLabel2 = NOT_ASSIGNED;
        this.dstSw2 = null;
        this.typeOfDstSet = dsType;
    }

    /**
     * Constructor for paired destination switches and their associated edge
     * labels.
     *
     * @param dsType type of next objective
     * @param edgeLabel1 label to be pushed as part of group operation for
     *            dstSw1
     * @param dstSw1 one of the paired destination switches
     * @param edgeLabel2 label to be pushed as part of group operation for
     *            dstSw2
     * @param dstSw2 the other paired destination switch
     */
    private DestinationSet(DestinationSetType dsType,
                          int edgeLabel1, DeviceId dstSw1,
                          int edgeLabel2, DeviceId dstSw2) {
        if (dstSw1.toString().compareTo(dstSw2.toString()) <= 0) {
            this.edgeLabel1 = edgeLabel1;
            this.dstSw1 = dstSw1;
            this.edgeLabel2 = edgeLabel2;
            this.dstSw2 = dstSw2;
        } else {
            this.edgeLabel1 = edgeLabel2;
            this.dstSw1 = dstSw2;
            this.edgeLabel2 = edgeLabel1;
            this.dstSw2 = dstSw1;
        }
        this.typeOfDstSet = dsType;
    }
    /**
     * Default constructor for kryo serialization.
     */
    private DestinationSet() {
        this.edgeLabel1 = NOT_ASSIGNED;
        this.edgeLabel2 = NOT_ASSIGNED;
        this.dstSw1 = DeviceId.NONE;
        this.dstSw2 = DeviceId.NONE;
        this.typeOfDstSet = null;
    }

    /**
     * Gets the label associated with given destination switch.
     *
     * @param dstSw the destination switch
     * @return integer the label associated with the destination switch
     */
    public int getEdgeLabel(DeviceId dstSw) {
        if (dstSw.equals(dstSw1)) {
            return edgeLabel1;
        } else if (dstSw.equals(dstSw2)) {
            return edgeLabel2;
        }
        return NOT_ASSIGNED;
    }

    /**
     * Gets all the destination switches in this destination set.
     *
     * @return a set of destination switch ids
     */
    public Set<DeviceId> getDestinationSwitches() {
        Set<DeviceId> dests = new HashSet<>();
        dests.add(dstSw1);
        if (dstSw2 != null) {
            dests.add(dstSw2);
        }
        return dests;
    }

    /**
     * Returns the type of this ds.
     *
     * @return the type of the destination set
     */
    public DestinationSetType getTypeOfDstSet() {
        return typeOfDstSet;
    }

    /**
     * Returns true if the next objective represented by this destination set
     * is of type SWAP_NOT_BOS or POP_NOT_BOS.
     *
     * @return the value of notBos
     */
    public boolean notBos() {
        if ((typeOfDstSet == DestinationSetType.SWAP_NOT_BOS) || (typeOfDstSet == DestinationSetType.POP_NOT_BOS)) {
            return true;
        }
        return false;
    }

    /**
     * Returns true if the next objective represented by this destination set
     * is of type SWAP_NOT_BOS or SWAP_BOS.
     *
     * @return the value of swap
     */
    public boolean swap() {
        if ((typeOfDstSet == DestinationSetType.SWAP_BOS) || (typeOfDstSet == DestinationSetType.SWAP_NOT_BOS)) {
            return true;
        }
        return false;
    }

    // The list of destination ids and label are used for comparison.
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof DestinationSet)) {
            return false;
        }
        DestinationSet that = (DestinationSet) o;
        if (this.typeOfDstSet != that.typeOfDstSet) {
            return false;
        }
        boolean equal = (this.edgeLabel1 == that.edgeLabel1 &&
                            this.dstSw1.equals(that.dstSw1));
        if (this.dstSw2 != null && that.dstSw2 == null ||
                this.dstSw2 == null && that.dstSw2 != null) {
            return false;
        }
        if (this.dstSw2 != null && that.dstSw2 != null) {
            equal = equal && (this.edgeLabel2 == that.edgeLabel2 &&
                                this.dstSw2.equals(that.dstSw2));
        }
        return equal;
    }

    // The list of destination ids and label are used for comparison.
    @Override
    public int hashCode() {
        if (dstSw2 == null) {
            return Objects.hash(typeOfDstSet, edgeLabel1, dstSw1);
        }
        return Objects.hash(typeOfDstSet, edgeLabel1, dstSw1, edgeLabel2,
                            dstSw2);
    }

    @Override
    public String toString() {
        ToStringHelper h = toStringHelper(this)
                                .add("Type", typeOfDstSet.getType())
                                .add("DstSw1", dstSw1)
                                .add("Label1", edgeLabel1);
        if (dstSw2 != null) {
            h.add("DstSw2", dstSw2)
             .add("Label2", edgeLabel2);
        }
        return h.toString();
    }

    public enum DestinationSetType {
        /**
         * Used to represent DestinationSetType where the next hop
         * is the same as the final destination.
         */
        PUSH_NONE("pushnon"),
        /**
         * Used to represent DestinationSetType where we need to
         * push a single mpls label, that of the destination.
         */
        PUSH_BOS("pushbos"),
        /**
         * Used to represent DestinationSetType where we need to pop
         * an mpls label which has the bos bit set.
         */
        POP_BOS("pop-bos"),
        /**
         * Used to represent DestinationSetType where we swap the outer
         * mpls label with a new one, and where the outer label has the
         * bos bit set.
         */
        SWAP_BOS("swapbos"),
        /**
         * Used to represent DestinationSetType where we need to pop
         * an mpls label which does not have the bos bit set.
         */
        POP_NOT_BOS("popnbos"),
        /**
         * Used to represent DestinationSetType where we swap the outer
         * mpls label with a new one, and where the outer label does not
         * have the bos bit set.
         */
        SWAP_NOT_BOS("swap-nb");

        private final String typeOfDstDest;
        DestinationSetType(String s) {
            typeOfDstDest = s;
        }

        public String getType() {
            return typeOfDstDest;
        }
    }

    /*
     * Static methods for creating DestinationSet objects in
     * order to remove ambiquity with multiple constructors.
     */

    /**
     * Returns a DestinationSet with type PUSH_NONE.
     *
     * @param destSw The deviceId for this next objective.
     * @return The DestinationSet of this type.
     */
    public static DestinationSet createTypePushNone(DeviceId destSw) {
        return new DestinationSet(DestinationSetType.PUSH_NONE, destSw);
    }

    /**
     * Returns a DestinationSet with type PUSH_BOS.
     *
     * @param edgeLabel1 The mpls label to push.
     * @param destSw1 The device on which the label is assigned.
     * @return The DestinationSet of this type.
     */
    public static DestinationSet createTypePushBos(int edgeLabel1, DeviceId destSw1) {
        return new DestinationSet(DestinationSetType.PUSH_BOS, edgeLabel1, destSw1);
    }

    /**
     * Returns a DestinationSet with type PUSH_BOS used for paired leafs.
     *
     * @param edgeLabel1 The label of first paired leaf.
     * @param destSw1 The device id of first paired leaf.
     * @param edgeLabel2 The label of the second leaf.
     * @param destSw2 The device id of the second leaf.
     * @return The DestinationSet of this type.
     */
    public static DestinationSet createTypePushBos(int edgeLabel1, DeviceId destSw1, int edgeLabel2, DeviceId destSw2) {
        return new DestinationSet(DestinationSetType.PUSH_BOS, edgeLabel1, destSw1, edgeLabel2, destSw2);
    }

    /**
     * Returns a DestinationSet with type POP_BOS.
     *
     * @param deviceId The deviceId for this next objective.
     * @return The DestinationSet of this type.
     */
    public static DestinationSet createTypePopBos(DeviceId deviceId) {
        return new DestinationSet(DestinationSetType.POP_BOS, deviceId);
    }

    /**
     * Returns a DestinationSet with type SWAP_BOS.
     *
     * @param edgeLabel The edge label to swap with.
     * @param deviceId The deviceId for this next objective.
     * @return The DestinationSet of this type.
     */
    public static DestinationSet createTypeSwapBos(int edgeLabel, DeviceId deviceId) {
        return new DestinationSet(DestinationSetType.SWAP_BOS, edgeLabel, deviceId);
    }

    /**
     * Returns a DestinationSet with type POP_NOT_BOS.
     *
     * @param deviceId The device-id this next objective should be installed.
     * @return The DestinationSet of this type.
     */
    public static DestinationSet createTypePopNotBos(DeviceId deviceId) {
        return new DestinationSet(DestinationSetType.POP_NOT_BOS, deviceId);
    }

    /**
     * Returns a DestinationSet with type SWAP_NOT_BOS.
     *
     * @param edgeLabel The edge label to swap with.
     * @param deviceId The deviceId for this next objective.
     * @return The DestinationSet of this type.
     */
    public static DestinationSet createTypeSwapNotBos(int edgeLabel, DeviceId deviceId) {
        return new DestinationSet(DestinationSetType.SWAP_NOT_BOS, edgeLabel, deviceId);
    }
}

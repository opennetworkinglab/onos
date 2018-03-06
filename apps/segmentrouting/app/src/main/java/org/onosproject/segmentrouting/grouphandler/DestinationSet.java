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
public class DestinationSet {
    public static final int NO_EDGE_LABEL = -1;
    private static final int NOT_ASSIGNED = 0;
    private boolean notBos; // XXX replace with enum
    private boolean swap; // XXX replace with enum
    private final DeviceId dstSw1;
    private final int edgeLabel1;
    private final DeviceId dstSw2;
    private final int edgeLabel2;


    private static final Logger log = getLogger(DestinationSet.class);

    /**
     * Constructor for a single destination with no Edge label.
     *
     * @param isNotBos indicates if it is meant for a non bottom-of-stack label
     * @param isSwap indicates if it is meant for a swap action
     * @param dstSw the destination switch
     */
    public DestinationSet(boolean isNotBos, boolean isSwap, DeviceId dstSw) {
        this.edgeLabel1 = NO_EDGE_LABEL;
        this.notBos = isNotBos;
        this.swap = isSwap;
        this.dstSw1 = dstSw;
        this.edgeLabel2 = NOT_ASSIGNED;
        this.dstSw2 = null;
    }

    /**
     * Constructor for a single destination with Edge label.
     *
     * @param isNotBos indicates if it is meant for a non bottom-of-stack label
     * @param isSwap indicates if it is meant for a swap action
     * @param edgeLabel label to be pushed as part of group operation
     * @param dstSw the destination switch
     */
    public DestinationSet(boolean isNotBos, boolean isSwap,
                       int edgeLabel, DeviceId dstSw) {
        this.notBos = isNotBos;
        this.swap = isSwap;
        this.edgeLabel1 = edgeLabel;
        this.dstSw1 = dstSw;
        this.edgeLabel2 = NOT_ASSIGNED;
        this.dstSw2 = null;
    }

    /**
     * Constructor for paired destination switches and their associated edge
     * labels.
     *
     * @param isNotBos indicates if it is meant for a non bottom-of-stack label
     * @param isSwap indicates if it is meant for a swap action
     * @param edgeLabel1 label to be pushed as part of group operation for
     *            dstSw1
     * @param dstSw1 one of the paired destination switches
     * @param edgeLabel2 label to be pushed as part of group operation for
     *            dstSw2
     * @param dstSw2 the other paired destination switch
     */
    public DestinationSet(boolean isNotBos, boolean isSwap,
                          int edgeLabel1, DeviceId dstSw1,
                          int edgeLabel2, DeviceId dstSw2) {
        this.notBos = isNotBos;
        this.swap = isSwap;
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
    }

    /**
     * Default constructor for kryo serialization.
     */
    public DestinationSet() {
        this.edgeLabel1 = NOT_ASSIGNED;
        this.edgeLabel2 = NOT_ASSIGNED;
        this.notBos = true;
        this.swap = true;
        this.dstSw1 = DeviceId.NONE;
        this.dstSw2 = DeviceId.NONE;
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
     * Gets the value of notBos.
     *
     * @return the value of notBos
     */
    public boolean notBos() {
        return notBos;
    }

    /**
     * Gets the value of swap.
     *
     * @return the value of swap
     */
    public boolean swap() {
        return swap;
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
        boolean equal = (this.edgeLabel1 == that.edgeLabel1 &&
                            this.notBos == that.notBos &&
                            this.swap == that.swap &&
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
            return Objects.hash(notBos, swap, edgeLabel1, dstSw1);
        }
        return Objects.hash(notBos, swap, edgeLabel1, dstSw1, edgeLabel2,
                            dstSw2);
    }

    @Override
    public String toString() {
        String type;
        if (!notBos && !swap) {
            type = "default";
        } else if (!notBos) {
            type = "swapbos";
        } else if (!swap) {
            type = "not-bos";
        } else {
            type = "swap-nb";
        }
        ToStringHelper h = toStringHelper(this)
                                .add("Type", type)
                                .add("DstSw1", dstSw1)
                                .add("Label1", edgeLabel1);
        if (dstSw2 != null) {
            h.add("DstSw2", dstSw2)
             .add("Label2", edgeLabel2);
        }
        return h.toString();
    }
}

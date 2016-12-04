/*
 * Copyright 2016 Open Networking Laboratory
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
package org.onosproject.tetopology.management.api.node;

import java.util.BitSet;
import java.util.List;
import java.util.Map;

import org.onosproject.tetopology.management.api.TeStatus;
import org.onosproject.tetopology.management.api.TeTopologyKey;

/**
 * Abstraction of a TE node.
 */
public interface TeNode {
    /**
     * Indicates that the TE node belongs to an abstract topology.
     */
    public static final short BIT_ABSTRACT = 0;

    /**
     * Indicates that the TE node is disabled.
     */
    public static final short BIT_DISABLED = 1;

    /**
     * Returns the TE node identifier.
     *
     * @return TE node identifier
     */
    long teNodeId();

    /**
     * Returns the TE node name.
     *
     * @return the te node name
     */
    String name();

    /**
     * Returns the flags.
     *
     * @return the flags
     */
    BitSet flags();

    /**
     * Returns the underlay TE topology identifier for the node.
     *
     * @return the underlay TE topology id
     */
    TeTopologyKey underlayTeTopologyId();

    /**
     * Returns the supporting TE node identifier.
     *
     * @return the id of the supporting node
     */
    TeNodeKey supportingTeNodeId();

    /**
     * Returns the source TE node identifier.
     *
     * @return the id of the source node
     */
    TeNodeKey sourceTeNodeId();

    /**
     * Returns the connectivity matrix table of the node.
     *
     * @return the connectivity matrix table
     */
    Map<Long, ConnectivityMatrix> connectivityMatrices();

    /**
     * Returns the connectivity matrix identified by its entry identifier.
     *
     * @param entryId connection matrix id
     * @return the connectivity matrix
     */
    ConnectivityMatrix connectivityMatrix(long entryId);

    /**
     * Returns a list of TE link identifiers originating from the node.
     *
     * @return a list of TE link ids
     */
    List<Long> teLinkIds();

    /**
     * Returns a collection of currently known tunnel termination points.
     *
     * @return a collection of tunnel termination points associated with this node
     */
    Map<Long, TunnelTerminationPoint> tunnelTerminationPoints();

    /**
     * Returns a tunnel termination point identified by its identifier.
     *
     * @param ttpId tunnel termination point identifier
     * @return the tunnel termination point
     */
    TunnelTerminationPoint tunnelTerminationPoint(long ttpId);

    /**
     * Returns the admin status.
     *
     * @return the adminStatus
     */
    TeStatus adminStatus();

    /**
     * Returns the operational status.
     *
     * @return the opStatus
     */
    TeStatus opStatus();

    /**
     * Returns a collection of currently known TE termination point identifiers.
     *
     * @return a collection of termination point ids associated with this node
     */
    List<Long> teTerminationPointIds();
}

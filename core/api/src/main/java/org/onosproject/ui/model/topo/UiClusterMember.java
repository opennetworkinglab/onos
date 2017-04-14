/*
 *  Copyright 2016-present Open Networking Laboratory
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.onosproject.ui.model.topo;

import org.onlab.packet.IpAddress;
import org.onosproject.cluster.ControllerNode;
import org.onosproject.cluster.NodeId;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.onosproject.cluster.ControllerNode.State.INACTIVE;

/**
 * Represents an individual member of the cluster (ONOS instance).
 */
public class UiClusterMember extends UiElement {

    private static final String NODE_CANNOT_BE_NULL = "Node cannot be null";

    private final UiTopology topology;
    private final NodeId nodeId;

    private ControllerNode.State state = INACTIVE;

    /**
     * Constructs a UI cluster member, with a reference to the parent
     * topology instance and the specified controller node instance.
     *
     * @param topology parent topology containing this cluster member
     * @param cnode    underlying controller node
     */
    public UiClusterMember(UiTopology topology, ControllerNode cnode) {
        checkNotNull(cnode, NODE_CANNOT_BE_NULL);
        this.topology = topology;
        this.nodeId = cnode.id();
    }

    @Override
    public String toString() {
        return "UiClusterMember{" + nodeId +
                ", online=" + isOnline() +
                ", ready=" + isReady() +
                "}";
    }

    @Override
    public String idAsString() {
        return id().toString();
    }

    /**
     * Returns the controller node instance backing this UI cluster member.
     *
     * @return the backing controller node instance
     */
    public ControllerNode backingNode() {
        return topology.services.cluster().getNode(nodeId);
    }

    /**
     * Sets the state of this cluster member.
     *
     * @param state the state
     */
    public void setState(ControllerNode.State state) {
        this.state = state;
    }

    /**
     * Returns the identity of the cluster member.
     *
     * @return member identifier
     */
    public NodeId id() {
        return nodeId;
    }

    /**
     * Returns the IP address of the cluster member.
     *
     * @return the IP address
     */
    public IpAddress ip() {
        return backingNode().ip();
    }

    /**
     * Returns true if this cluster member is online (active).
     *
     * @return true if online, false otherwise
     */
    public boolean isOnline() {
        return state.isActive();
    }

    /**
     * Returns true if this cluster member is considered ready.
     *
     * @return true if ready, false otherwise
     */
    public boolean isReady() {
        return state.isReady();
    }
}

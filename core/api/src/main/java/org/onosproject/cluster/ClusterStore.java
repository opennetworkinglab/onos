/*
 * Copyright 2014-present Open Networking Foundation
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
package org.onosproject.cluster;

import org.onlab.packet.IpAddress;
import org.onosproject.core.Version;
import org.onosproject.store.Store;

import java.time.Instant;
import java.util.Set;

/**
 * Manages inventory of controller cluster nodes; not intended for direct use.
 */
public interface ClusterStore extends Store<ClusterEvent, ClusterStoreDelegate> {

    /**
     * Returns the local controller node.
     *
     * @return local controller instance
     */
    ControllerNode getLocalNode();

    /**
     * Returns the set of storage nodes.
     *
     * @return set of storage nodes
     */
    Set<Node> getStorageNodes();

    /**
     * Returns the set of current cluster members.
     *
     * @return set of cluster members
     */
    Set<ControllerNode> getNodes();

    /**
     * Returns the specified controller node.
     *
     * @param nodeId controller instance identifier
     * @return controller instance
     */
    ControllerNode getNode(NodeId nodeId);

    /**
     * Returns the availability state of the specified controller node.
     *
     * @param nodeId controller instance identifier
     * @return availability state
     */
    ControllerNode.State getState(NodeId nodeId);

    /**
     * Returns the version of the specified controller node.
     *
     * @param nodeId controller instance identifier
     * @return controller version
     */
    Version getVersion(NodeId nodeId);

    /**
     * Marks the current node as fully started.
     *
     * @param started true indicates all components have been started
     */
    void markFullyStarted(boolean started);

    /**
     * Returns the system when the availability state was last updated.
     *
     * @param nodeId controller node identifier
     * @return system time when the availability state was last updated.
     */
    Instant getLastUpdatedInstant(NodeId nodeId);

    /**
     * Adds a new controller node to the cluster.
     *
     * @param nodeId  controller node identifier
     * @param ip      node IP listen address
     * @param tcpPort tcp listen port
     * @return newly added node
     */
    ControllerNode addNode(NodeId nodeId, IpAddress ip, int tcpPort);

    /**
     * Removes the specified node from the inventory of cluster nodes.
     *
     * @param nodeId controller instance identifier
     */
    void removeNode(NodeId nodeId);

}

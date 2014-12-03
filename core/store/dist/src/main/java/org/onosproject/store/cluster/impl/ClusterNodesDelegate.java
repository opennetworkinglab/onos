/*
 * Copyright 2014 Open Networking Laboratory
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
package org.onosproject.store.cluster.impl;

import org.onosproject.cluster.DefaultControllerNode;
import org.onosproject.cluster.NodeId;
import org.onlab.packet.IpAddress;

// Not used right now
/**
 * Simple back interface through which connection manager can interact with
 * the cluster store.
 */
public interface ClusterNodesDelegate {

    /**
     * Notifies about cluster node coming online.
     *
     * @param nodeId  newly detected cluster node id
     * @param ip      node IP listen address
     * @param tcpPort node TCP listen port
     * @return the controller node
     */
    DefaultControllerNode nodeDetected(NodeId nodeId, IpAddress ip,
                                       int tcpPort);

    /**
     * Notifies about cluster node going offline.
     *
     * @param nodeId identifier of the cluster node that vanished
     */
    void nodeVanished(NodeId nodeId);

    /**
     * Notifies about remote request to remove node from cluster.
     *
     * @param nodeId identifier of the cluster node that was removed
     */
    void nodeRemoved(NodeId nodeId);

}

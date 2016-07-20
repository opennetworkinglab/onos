/*
 * Copyright 2014-present Open Networking Laboratory
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

import java.util.Set;

import org.joda.time.DateTime;
import org.onosproject.event.ListenerService;

/**
 * Service for obtaining information about the individual nodes within
 * the controller cluster.
 */
public interface ClusterService
    extends ListenerService<ClusterEvent, ClusterEventListener> {

    /**
     * Returns the local controller node.
     *
     * @return local controller node
     */
    ControllerNode getLocalNode();

    /**
     * Returns the set of current cluster members.
     *
     * @return set of cluster members
     */
    Set<ControllerNode> getNodes();

    /**
     * Returns the specified controller node.
     *
     * @param nodeId controller node identifier
     * @return controller node
     */
    ControllerNode getNode(NodeId nodeId);

    /**
     * Returns the availability state of the specified controller node. Note
     * that this does not imply that all the core and application components
     * have been fully activated; only that the node has joined the cluster.
     *
     * @param nodeId controller node identifier
     * @return availability state
     */
    ControllerNode.State getState(NodeId nodeId);

    /**
     * Returns the system time when the availability state was last updated.
     *
     * @param nodeId controller node identifier
     * @return system time when the availability state was last updated.
     */
    DateTime getLastUpdated(NodeId nodeId);

}

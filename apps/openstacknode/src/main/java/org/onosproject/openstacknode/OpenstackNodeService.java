/*
 * Copyright 2016-present Open Networking Laboratory
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
package org.onosproject.openstacknode;

import java.util.List;

/**
 * Handles the bootstrap request for compute/gateway node.
 */
public interface OpenstackNodeService {

    public enum OpenstackNodeType {
        /**
         * Compute or Gateway Node.
         */
        COMPUTENODE,
        GATEWAYNODE
    }
    /**
     * Adds a new node to the service.
     *
     * @param node openstack node
     */
    void addNode(OpenstackNode node);

    /**
     * Deletes a node from the service.
     *
     * @param node openstack node
     */
    void deleteNode(OpenstackNode node);

    /**
     * Returns nodes known to the service for designated openstacktype.
     *
     * @param openstackNodeType openstack node type
     * @return list of nodes
     */
    List<OpenstackNode> getNodes(OpenstackNodeType openstackNodeType);

    /**
     * Returns the NodeState for a given node.
     *
     * @param node openstack node
     * @return true if the NodeState for a given node is COMPLETE, false otherwise
     */
    boolean isComplete(OpenstackNode node);
}

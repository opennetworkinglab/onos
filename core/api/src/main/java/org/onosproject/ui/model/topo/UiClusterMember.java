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

import org.onosproject.cluster.ControllerNode;
import org.onosproject.cluster.NodeId;

/**
 * Represents an individual member of the cluster (ONOS instance).
 */
public class UiClusterMember extends UiElement {

    private final ControllerNode cnode;

    /**
     * Constructs a cluster member, with a reference to the specified
     * controller node instance.
     *
     * @param cnode underlying controller node.
     */
    public UiClusterMember(ControllerNode cnode) {
        this.cnode = cnode;
    }

    /**
     * Updates the information about this cluster member.
     *
     * @param cnode underlying controller node
     */
    public void update(ControllerNode cnode) {
        // TODO: update our information cache appropriately
    }

    /**
     * Returns the identity of the cluster member.
     *
     * @return member identifier
     */
    public NodeId id() {
        return cnode.id();
    }
}

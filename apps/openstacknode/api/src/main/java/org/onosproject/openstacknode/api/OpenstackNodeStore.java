/*
 * Copyright 2017-present Open Networking Foundation
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
package org.onosproject.openstacknode.api;

import org.onosproject.store.Store;

import java.util.Set;

/**
 * Manages inventory of OpenstackNode; not intended for direct use.
 */
public interface OpenstackNodeStore extends Store<OpenstackNodeEvent, OpenstackNodeStoreDelegate> {

    /**
     * Creates a new node.
     *
     * @param osNode openstack node
     */
    void createNode(OpenstackNode osNode);

    /**
     * Updates the node.
     *
     * @param osNode openstack node
     */
    void updateNode(OpenstackNode osNode);

    /**
     * Removes the node.
     *
     * @param hostname openstack node hostname
     * @return removed openstack node; null if no node mapped for the hostname
     */
    OpenstackNode removeNode(String hostname);

    /**
     * Returns all registered nodes.
     *
     * @return set of openstack nodes
     */
    Set<OpenstackNode> nodes();

    /**
     * Returns the node with the specified hostname.
     *
     * @param hostname hostname
     * @return openstack node
     */
    OpenstackNode node(String hostname);
}

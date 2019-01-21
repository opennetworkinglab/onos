/*
 * Copyright 2019-present Open Networking Foundation
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
package org.onosproject.k8snode.api;

import org.onosproject.store.Store;

import java.util.Set;

/**
 * Manages inventory of Kubernetes Node; not intended for direct use.
 */
public interface K8sNodeStore extends Store<K8sNodeEvent, K8sNodeStoreDelegate> {

    /**
     * Creates a new node.
     *
     * @param node kubernetes node
     */
    void createNode(K8sNode node);

    /**
     * Updates the node.
     *
     * @param node kubernetes node
     */
    void updateNode(K8sNode node);

    /**
     * Removes the node with the given hostname.
     *
     * @param hostname kubernetes node hostname
     * @return removed kubernetes node; null if no node is associated with the hostname
     */
    K8sNode removeNode(String hostname);

    /**
     * Returns all registered nodes.
     *
     * @return set of kubernetes nodes
     */
    Set<K8sNode> nodes();

    /**
     * Returns the node with the specified hostname.
     *
     * @param hostname hostname
     * @return kubernetes node
     */
    K8sNode node(String hostname);
}

/*
 * Copyright 2020-present Open Networking Foundation
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
package org.onosproject.kubevirtnode.api;

import org.onosproject.store.Store;

import java.util.Set;

/**
 * Manages inventory of KubevirtNode; not intended for direct use.
 */
public interface KubevirtNodeStore extends Store<KubevirtNodeEvent, KubevirtNodeStoreDelegate> {

    /**
     * Creates a new node.
     *
     * @param node kubevirt node
     */
    void createNode(KubevirtNode node);

    /**
     * Updates the node.
     *
     * @param node kubevirt node
     */
    void updateNode(KubevirtNode node);

    /**
     * Removes the node.
     *
     * @param hostname kubevirt node hostname
     * @return removed kubevirt node; null if no node mapped for the hostname
     */
    KubevirtNode removeNode(String hostname);

    /**
     * Returns all registered nodes.
     *
     * @return set of kubevirt nodes
     */
    Set<KubevirtNode> nodes();

    /**
     * Returns the node with the specified hostname.
     *
     * @param hostname hostname
     * @return kubevirt node
     */
    KubevirtNode node(String hostname);
}

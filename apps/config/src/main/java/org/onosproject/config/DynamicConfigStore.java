/*
 * Copyright 2016-present Open Networking Foundation
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
package org.onosproject.config;

import com.google.common.annotations.Beta;
import org.onosproject.yang.model.DataNode;
import org.onosproject.yang.model.ResourceId;
import org.onosproject.store.Store;

import java.util.concurrent.CompletableFuture;

/**
 * Store interface for storing and distributing dynamic configuration data.
 */
@Beta
public interface DynamicConfigStore
        extends Store<DynamicConfigEvent, DynamicConfigStoreDelegate> {
    /**
     * Adds a new node in the dynamic config store. The new node will not be
     * added if there is a node with the same identifier, already present at
     * the specified path or any of the parent nodes were not present in the
     * path leading up to the requested node.
     *
     * @param path data structure with absolute path to the parent
     * @param node recursive data structure, holding a leaf node or a subtree
     * @return future that is completed with {@code true} if the new node was
     * successfully added or completed exceptionally with
     * {@code FailedException} if node addition failed
     */

    CompletableFuture<Boolean> addNode(ResourceId path, DataNode node);

    /**
     * Reads the requested node from the dynamic config store.
     * This operation would get translated to reading a leaf node or a subtree.
     * This would fail if the requested node was not present or any parent nodes
     * in the path were not present.
     *
     * @param path data structure with absolute path to the intended node
     * @param filter filtering conditions to be applied on the result list of nodes
     * @return future that will be completed with a DataNode (will be an empty
     * DataNode if after applying the filter, the result is an empty list of nodes)
     * or completed with {@code FailedException} if the node could not be read
     */
    CompletableFuture<DataNode> readNode(ResourceId path, Filter filter);

    /**
     * Updates an existing node in the dynamic config store.
     * Any missing children will be created with this request. The update will
     * fail if the requested node or any of the parent nodes in the path
     * were not present.
     *
     * @param path data structure with absolute path to the parent
     * @param node recursive data structure, holding a leaf node or a subtree
     * @return future that is completed with {@code true} if the node was
     * successfully updated or completed exceptionally with
     * {@code FailedException} if the update request failed
     */
    CompletableFuture<Boolean> updateNode(ResourceId path, DataNode node);

    /**
     * Replaces nodes in the dynamic config store.
     * This will ensure that only the tree structure in the given DataNode will
     * be in place after a replace. This would fail if the requested node or
     * any of the parent nodes in the path were not present.
     *
     * @param path data structure with absolute path to the parent
     * @param node recursive data structure, holding a leaf node or a subtree
     * @return future that is completed with {@code true} if the node was
     * successfully replaced or completed exceptionally with
     * {@code FailedException} if the replace request failed
     */
    CompletableFuture<Boolean> replaceNode(ResourceId path, DataNode node);

    /**
     * Removes  a node from the dynamic config store.
     * This would fail if the requested node or any of the parent nodes in the
     * path were not present or the specified node is the root node or has one
     * or more children.
     *
     * @param path data structure with absolute path to the intended node
     * @return future that is completed with {@code true} if the node was
     * successfully deleted or completed exceptionally with
     * {@code FailedException} if the delete request failed
     */
    CompletableFuture<Boolean> deleteNode(ResourceId path);

    /**
     * Removes a subtree from the dynamic config store.
     * This will delete all the children recursively, under the given node.
     * Will fail if the requested node or any of the parent nodes in
     * the path were not present.
     *
     * @param path data structure with absolute path to the intended node
     * @return future that is completed with {@code true} if the delete was
     * successful or completed exceptionally with
     * {@code FailedException} if the delete request failed
     */
    CompletableFuture<Boolean> deleteNodeRecursive(ResourceId path);

    /**
     * Returns whether the requested node exists in the Dynamic Config store.
     *
     * @param path data structure with absolute path to the intended node
     * @return future that is completed with {@code true} if the node existed
     * in the store, {@code false} otherwise
     */
    CompletableFuture<Boolean> nodeExist(ResourceId path);
}
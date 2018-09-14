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
import org.onosproject.event.ListenerService;
import org.onosproject.yang.model.RpcInput;
import org.onosproject.yang.model.RpcOutput;

import java.util.concurrent.CompletableFuture;

/**
 * Service for storing and distributing dynamic configuration data.
 */
@Beta
public interface DynamicConfigService
        extends ListenerService<DynamicConfigEvent, DynamicConfigListener> {

    // FIXME revisit and verify ResourceId documentation.
    // it is likely that it actually is not expecting absolute ResourceId

    // TODO revisit which path ResourceId these API should accepting.
    // there is inconsistency, some expect parent, some expect node itself

    /**
     * Creates a new node in the dynamic config store.
     * This method would throw an exception if there is a node with the same
     * identifier, already present at the specified path or any of the parent
     * nodes were not present in the path leading up to the requested node.
     * Failure reason will be the error message in the exception.
     *
     * @param parent data structure with absolute path to the parent
     * @param node recursive data structure, holding a leaf node or a subtree
     * @throws FailedException if the new node could not be created
     */
    void createNode(ResourceId parent, DataNode node);

    /**
     * Reads the requested node form the dynamic config store.
     * This operation would get translated to reading a leaf node or a subtree.
     * Will return an empty DataNode if after applying the filter, the result
     * is an empty list of nodes. This method would throw an exception if the
     * requested node or any parent nodes in the path were not present.
     * Failure reason will be the error message in the exception.
     *
     * @param path data structure with absolute path to the intended node
     * @param filter filtering conditions to be applied on the result list of nodes
     * @return a recursive data structure, holding a leaf node or a subtree
     * @throws FailedException if the requested node could not be read
     */
    DataNode readNode(ResourceId path, Filter filter);

    /**
     * Returns whether the requested node exists in the Dynamic Config store.
     *
     * @param path data structure with absolute path to the intended node
     * @return {@code true} if the node existed in the store
     * {@code false} otherwise
     */
    Boolean nodeExist(ResourceId path);

    /**
     * Updates an existing node in the dynamic config store.
     * Existing nodes will be updated and missing nodes will be created as needed.
     * This method would throw an exception if the requested node or any of the
     * parent nodes in the path were not present.
     * Failure reason will be the error message in the exception.
     *
     * @param parent data structure with absolute path to the parent
     * @param node recursive data structure, holding a leaf node or a subtree
     * @throws FailedException if the update request failed for any reason
     */
    void updateNode(ResourceId parent, DataNode node);

    /**
     * Replaces nodes in the dynamic config store.
     * This will ensure that only the tree structure in the given DataNode will
     * be in place after a replace. This method would throw an exception if
     * the requested node or any of the parent nodes in the path were not
     * present. Failure reason will be the error message in the exception.
     *
     * @param parent data structure with absolute path to the parent
     * @param node recursive data structure, holding a leaf node or a subtree
     * @throws FailedException if the replace request failed
     */
    void replaceNode(ResourceId parent, DataNode node);

    /**
     * Removes a node from the dynamic config store.
     * If the node pointed to a subtree, that will be deleted recursively.
     * It will throw an exception if the requested node or any of the parent nodes in the
     * path were not present; Failure reason will be the error message in the exception.
     *
     * @param path data structure with absolute path to the intended node
     * @throws FailedException if the delete request failed
     */
    void deleteNode(ResourceId path);

    /**
     * Invokes an RPC.
     *
     * @param input RPC input with ResourceId and DataNode
     * @return future that will be completed with RpcOutput
     * @throws FailedException if the RPC could not be invoked
     */
    CompletableFuture<RpcOutput> invokeRpc(RpcInput input);
}
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

package org.onosproject.config;

import com.google.common.annotations.Beta;
import org.onosproject.yang.model.DataNode;
import org.onosproject.yang.model.ResourceId;
import org.onosproject.event.ListenerService;
import org.onosproject.yang.model.RpcCaller;
import org.onosproject.yang.model.RpcCommand;
import org.onosproject.yang.model.RpcHandler;
import org.onosproject.yang.model.RpcInput;
import org.onosproject.yang.model.RpcOutput;

/**
 * Service for storing and distributing dynamic configuration data.
 */
@Beta
public interface DynamicConfigService
        extends ListenerService<DynamicConfigEvent, DynamicConfigListener> {
    /**
     * Creates a new node in the dynamic config store.
     * This method would throw an exception if there is a node with the same
     * identifier, already present at the specified path or any of the parent
     * nodes were not present in the path leading up to the requested node.
     * Failure reason will be the error message in the exception.
     *
     * @param path data structure with absolute path to the parent
     * @param node recursive data structure, holding a leaf node or a subtree
     * @throws FailedException if the new node could not be created
     */
    void createNodeRecursive(ResourceId path, DataNode node);

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
     * Returns the number of children under the node at the given path.
     * This method would throw an exception if the requested node or any parent
     * nodes in the path were not present.
     * Failure reason will be the error message in the exception.
     *
     * @param path data structure with absolute path to the intended node
     * @param filter filtering conditions to be applied on the result list of nodes
     * @return the number of children after applying the filtering conditions if any
     * @throws FailedException if the request failed
     */
    Integer getNumberOfChildren(ResourceId path, Filter filter);

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
     * Any missing children nodes will be created with this request.
     * This method would throw an exception if the requested node or any of the
     * parent nodes in the path were not present.
     * Failure reason will be the error message in the exception.
     *
     * @param path data structure with absolute path to the parent
     * @param node recursive data structure, holding a leaf node or a subtree
     * @throws FailedException if the update request failed for any reason
     */
    void updateNode(ResourceId path, DataNode node);

    /**
     * Replaces nodes in the dynamic config store.
     * This will ensure that only the tree structure in the given DataNode will
     * be in place after a replace. This method would throw an exception if
     * the requested node or any of the parent nodes in the path were not
     * present. Failure reason will be the error message in the exception.
     *
     * @param path data structure with absolute path to the parent
     * @param node recursive data structure, holding a leaf node or a subtree
     * @throws FailedException if the replace request failed
     */
    void replaceNode(ResourceId path, DataNode node);

    /**
     * Removes a leaf node from the dynamic config store.
     * This method would throw an exception if the requested node or any of the
     * parent nodes in the path were not present or the specified node is the
     * root node or has one or more children.
     * Failure reason will be the error message in the exception.
     *
     * @param path data structure with absolute path to the intended node
     * @throws FailedException if the delete request failed
     */
    void deleteNode(ResourceId path);

    /**
     * Removes a subtree from the dynamic config store.
     * This method will delete all the children recursively, under the given
     * node. It will throw an exception if the requested node or any of the
     * parent nodes in the path were not present.
     * Failure reason will be the error message in the exception.
     *
     * @param path data structure with absolute path to the intended node
     * @throws FailedException if the delete request failed
     */
    void deleteNodeRecursive(ResourceId path);

    /**
     * Registers an RPC handler.
     *
     * @param handler RPC handler
     * @param command RPC command
     * @throws FailedException if the handler could not be added
     */
    void registerHandler(RpcHandler handler, RpcCommand command);

    /**
     * Unregisters an RPC receiver.
     *
     * @param handler RPC handler
     * @param command RPC command
     * @throws FailedException if the handler could not be removed
     */
    void unRegisterHandler(RpcHandler handler, RpcCommand command);

    /**
     * Invokes an RPC.
     *
     * @param caller of the of the RPC
     * @param msgId RPC message id
     * @param command RPC command
     * @param input RPC input
     * @throws FailedException if the RPC could not be invoked
     */
    void invokeRpc(RpcCaller caller, Integer msgId, RpcCommand command, RpcInput input);

    /**
     * Provides response to a a previously invoked RPC.
     *
     * @param msgId of a previously invoked RPC
     * @param output data from the RPC execution
     * @throws FailedException if the RPC response was invalid
     * (or the msg id was not recognised by the store)
     */
    void rpcResponse(Integer msgId, RpcOutput output);
}
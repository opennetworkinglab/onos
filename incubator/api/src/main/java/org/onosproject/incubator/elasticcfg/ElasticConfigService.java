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
package org.onosproject.incubator.elasticcfg;

import org.onosproject.event.ListenerService;

import java.util.concurrent.CompletableFuture;

/**
 * Service for storing and distributing elastic configuration data.
 */
public interface ElasticConfigService
        extends ListenerService<ElasticConfigEvent, ElasticConfigListener> {
    /**
     * Adds a new node to the elastic config store.
     *
     * @param store type of store to which the application wants to add the node to.
     * @param path  data structure with absolute path to the parent and relative
     *              path to the node
     * @param node  data structure with nodetype and value to be stored at the node
     * @return future that is completed with {@code true} if the new node was successfully
     * added. Future will be completed with {@code false} if a node already exists at
     * the specified path. Future will be completed exceptionally with a
     * {@code FailedException} if the parent node (for the node to create) does not exist.
     */
    CompletableFuture<Boolean> addNode(ConfigStoreType store,
                                       ConfigNodePath path, ConfigNode node);

    /**
     * Removes  a node from the elastic config store.
     *
     * @param store type of store which the application wants to access.
     * @param path  data structure with absolute path to the parent and
     *              relative path to the node
     * @return future for the previous value. Future will be completed with a
     * {@code FailedException} if the node to be removed is either the root
     * node or has one or more children or if the node to be removed does not exist.
     */
    CompletableFuture<ConfigNode> removeNode(ConfigStoreType store, ConfigNodePath path);

    /**
     * Creates/Updates  a node in the elastic config store.
     *
     * @param store type of store which the application wants to access.
     * @param path  data structure with absolute path to the parent and
     *              relative path to the node
     * @param node  data structure with nodetype and new value to be stored at the node
     * @return future for the previous value. Future will be completed with {@code null}
     * if there was no node previously at that path.
     * Future will be completed with a {@code FailedException}
     * if the parent node (for the node to create/update) does not exist.
     */
    CompletableFuture<ConfigNode> updateNode(ConfigStoreType store,
                                             ConfigNodePath path, ConfigNode node);

    /**
     * Creates nodes in the elastic config store, recursively by creating
     * all missing intermediate nodes in the path.
     *
     * @param store type of store which the application wants to access.
     * @param path  data structure with absolute path to the parent and relative
     *              path to the node
     * @param node  recursive data structure with nodetype and value to
     *              be stored at the node
     * @return future that is completed with {@code true} if all the new
     * nodes were  successfully
     * created. Future will be completed with {@code false} if any node
     * already exists at the specified path
     * //TODO
     */
    CompletableFuture<Boolean> createRecursive(ConfigStoreType store,
                                               ConfigNodePath path, ConfigNode node);

    /**
     * Delete nodes in the elastic config store, recursively by deleting all
     * intermediate nodes in the path.
     *
     * @param store type of store which the appplication wants to access.
     * @param path  data structure with absolute path to the parent and
     *              relative path to the node
     * @return future that is completed with {@code true} if all the
     * nodes under the given path including the node at the path could
     * be successfully deleted. Future will be completed with {@code false}
     * if the node at the given path or any parent node
     * did not exist //TODO
     */
    CompletableFuture<Boolean> deleteRecursive(ConfigStoreType store, ConfigNodePath path);

    /**
     * Creates/Updates nodes in the elastic config store, recursively by creating
     * all missing intermediate nodes in the path.
     *
     * @param store type of store which the appplication wants to access.
     * @param path  data structure with absolute path to the parent and
     *              relative path to the node
     * @param node  recursive data structure with nodetype and value to
     *              be stored at the node
     * @return future that is completed with {@code true} if all the
     * nodes under the given path
     * including the node at the path could be successfully updated.
     * Future will be completed with {@code false} if the node at the
     * given path or any parent node
     * did not exist //TODO
     */
    CompletableFuture<Boolean> updateRecursive(ConfigStoreType store,
                                               ConfigNodePath path, ConfigNode node);

    /**
     * Returns a value node or subtree under the given path.
     *
     * @param store type of store which the application wants to access.
     * @param path  data structure with absolute path to the parent and
     *              relative path to the node
     * @param mode whether to retrieve the nodes recursively or not
     * @param filter filtering conditions to be applied on the result
     *               list of nodes.
     * @return future that will be completed either with a value node
     * or a recursive data structure containing the subtree;
     * will be completed with {@code null} if
     * after applying the filter, the result is an empty list of nodes.
     * Future will be completed with a {@code FailedException} if path
     * does not point to a valid node.
     */
    CompletableFuture<ConfigNode> getNode(ConfigStoreType store,
                                          ConfigNodePath path, TraversalMode mode,
                                          ConfigFilter filter);

    /**
     * Returns the number of children under the given path, excluding
     * the node at the path.
     *
     * @param store type of store which the application wants to access.
     * @param path  data structure with absolute path to the parent and
     *              relative path to the node
     * @param filter how the value nodes should be filtered
     * @return future that will be completed with {@code Integer}, the
     * count of the children
     * after applying the filtering conditions as well.
     * Future will be completed with a {@code FailedException} if path
     * does not point to a valid node
     */
    CompletableFuture<Integer> getNumberOfChildren(ConfigStoreType store,
                                                   ConfigNodePath path, ConfigFilter filter);

    //TODO
    /**
     * Filter
     * What should be the filtering conditions?
     * a list of keys? node attribute/s? level1 children?
     * Merge Trees
     * add sub tree
     * delete subtree
     * update sub tree
     * get sub tree
     * How to handle big subtrees?
     * how many levels? how many results to limit?
     */

    /**
     * Registers a listener to be notified when the subtree rooted at
     * the specified path
     * is modified.
     *
     * @param store type of store which the application wants to access.
     * @param path  data structure with absolute path to the parent and
     *              relative path to the node
     * @param listener listener to be notified
     * @return a future that is completed when the operation completes
     */
    CompletableFuture<Void> addConfigListener(ConfigStoreType store,
                                              ConfigNodePath path,
                                              ElasticConfigListener listener);

    /**
     * Unregisters a previously added listener.
     *
     * @param listener listener to unregister
     * @return a future that is completed when the operation completes
     */
    CompletableFuture<Void> removeConfigListener(ElasticConfigListener listener);
}

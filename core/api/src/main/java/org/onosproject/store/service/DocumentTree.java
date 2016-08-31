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

package org.onosproject.store.service;

import org.onosproject.store.primitives.DocumentTreeNode;

import java.util.Iterator;

/**
 * Interface for a tree with structure wherein keys hold information
 * about the hierarchical structure of the tree (name of parent,
 * grandparent etc.).
 */
public interface DocumentTree<V> {

    /**
     * Returns the root of the tree. This will be the first part of any fully
     * qualified path and will enable discovery of the entire tree.
     *
     * @return a string that is the name of the root of the tree.
     */
    DocumentPath root();

    /**
     * Returns a sorted list containing all key value pairs that are direct
     * descendants of the supplied parent. The returned list will be immutable.
     * If the specified parent does not exist this method will fail with an
     * exception.
     *
     * @throws NoSuchDocumentPathException if the parent does not exist
     * @param parentPath the path to the parent of the desired nodes
     * @return an iterator of the children of the specified parent
     */
    Iterator<DocumentTreeNode<V>> getChildren(DocumentPath parentPath);

    /**
     * Returns the value associated with the supplied key or null if no such
     * node exists.
     *
     * @param key the key to query
     * @return a value or null
     */
    DocumentTreeNode<V> getNode(DocumentPath key);

    /**
     * Takes a string that specifies the complete path to the mapping to be
     * added or updated and creates the key value mapping or updates the value.
     * If the specified parent cannot be found the operation fails with an
     * error.
     *
     * @throws NoSuchDocumentPathException if the specified parent does not
     * exist.
     * @param key the fully qualified key of the entry to be added or updated
     * @param value the non-null value to be associated with the key
     * @return the previous mapping or null if there was no previous mapping
     */
    V putNode(DocumentPath key, V value);

    /**
     * Takes the fully qualified  name of the node to be added along with
     * the value to be added. If the specified key already exists it doesnot
     * update anything & returns false. If the parent does not exist the
     * operation fails with an exception.
     *
     * @throws NoSuchDocumentPathException if the specified parent does not
     * exist.
     * @param key the fully qualified key of the entry to be added or updated
     * @param value the non-null value to be associated with the key
     * @return returns true if the mapping could be added successfully
     */
    boolean createNode(DocumentPath key, V value);

    /**
     * Removes the node with the specified fully qualified key. Returns null if
     * the node did not exist. This method will throw an exception if called on
     * a non-leaf node.
     *
     * @throws IllegalDocumentModificationException if the node had children.
     * @param key the fully qualified key of the node to be removed
     * @return the previous value of the node or null if it did not exist
     */
    V removeNode(DocumentPath key);
}

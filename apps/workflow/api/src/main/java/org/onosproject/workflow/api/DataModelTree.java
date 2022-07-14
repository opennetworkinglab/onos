/*
 * Copyright 2018-present Open Networking Foundation
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
package org.onosproject.workflow.api;

/**
 * Interface for data model tree.
 */
public interface DataModelTree {

    /**
     * Data model tree node type (map or array).
     */
    enum Nodetype {

        /**
         * Map type data model tree node.
         */
        MAP,

        /**
         * Array type data model tree node.
         */
        ARRAY
    }

    /**
     * Returns subtree on the path.
     * @param path data model tree path
     * @return subtree on the path
     */
    DataModelTree subtree(String path);

    /**
     * Attaches subtree on the path.
     * @param path data model tree path where subtree will be attached
     * @param tree subtree to be attached
     * @throws WorkflowException workflow exception
     */
    void attach(String path, DataModelTree tree) throws WorkflowException;

    /**
     * Allocates leaf node on the path.
     * @param path data model tree path where new leaf node will be allocated
     * @param leaftype leaf node type
     * @return data model tree
     * @throws WorkflowException workflow exception
     */
    DataModelTree alloc(String path, Nodetype leaftype) throws WorkflowException;

    /**
     * Remove node on the path. This removes the entity.
     * @param path data model tree path
     * @throws WorkflowException workflow exception
     */
    void remove(String path) throws WorkflowException;

    /**
     * Clear node on the path. This does not remove the entry. This initializes the value to Null.
     * @param path data model tree path
     * @throws WorkflowException workflow exception
     */
    void clear(String path) throws WorkflowException;

}


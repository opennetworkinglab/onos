/*
 * Copyright 2016 Open Networking Laboratory
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
package org.onosproject.teyang.api;

/**
 * The operation type.
 */
public enum OperationType {

    /**
     * The configuration data identified by the element
     * containing this attribute is merged with the configuration
     * at the corresponding level in the configuration datastore.
     */
    MERGE,

    /**
     * The configuration data identified by the element
     * containing this attribute replaces any related configuration
     * in the configuration datastore. If no such configuration
     * data exists in the configuration datastore, it is created.
     */
    REPLACE,

    /**
     * The configuration data identified by the element
     * containing this attribute is added to the configuration if
     * and only if the configuration data does not already exist in
     * the configuration datastore.  If the configuration data
     * exists, an error is returned.
     */
    CREATE,

    /**
     * The configuration data identified by the element
     * containing this attribute is deleted from the configuration
     * if and only if the configuration data currently exists in
     * the configuration datastore.  If the configuration data does
     * not exist, an error is returned".
     */
    DELETE,

    /**
     * The configuration data identified by the element
     * containing this attribute is deleted from the configuration
     * if the configuration data currently exists in the
     * configuration datastore.  If the configuration data does not
     * exist, the "remove" operation is silently ignored by the
     * server.
     */
    REMOVE,

    /**
     * The node is used as a containment node to reach the child node,
     * There is no change in the data store for the values of this node in the
     * edit request.
     */
    NONE,

//    /**
//     * The YANG based request is to edit a config node / subtree in the data
//     * store.
//     */
//    EDIT_CONFIG,
//
//    /**
//     * The YANG based request is to query a config node / subtree in the data
//     * store.
//     */
//    QUERY_CONFIG,
//
    /**
     * The YANG based request is to query a node / subtree in the data store.
     */
    QUERY,

//    /**
//     * The YANG based request is to execute an RPC defined in YANG.
//     */
//    RPC,
//
//    /**
//     * The YANG based request is to execute an RPC defined in YANG.
//     */
//    NOTIFICATION
}

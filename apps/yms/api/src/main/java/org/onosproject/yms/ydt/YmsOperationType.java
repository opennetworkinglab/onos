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

package org.onosproject.yms.ydt;

/**
 * Represents type of root level operation for the request.
 *
 * This is used by protocols to specify the root level operation associated
 * with the request. YMS data validation and data handling will vary based
 * on the edit operation type, for an instance YANG specified "mandatory"
 * leafs needn't be present for QUERY_CONFIG and QUERY, but they may be
 * mandatory to be present in request for EDIT_CONFIG type. The validation
 * and handling is further dependent on edit operation type.
 *
 * In SBI, driver/provider must provide this information to YMS which needs
 * to encode this information in corresponding data format.
 *
 * YmsOperationType MUST be specified by protocol.
 */

/*
 * Yang interaction type with RESTCONF and NETCONF as example:
 * +--------------+-------------------+-------------+
 * | RESTCONF     | NETCONF           | EditOpType  |
 * +--------------+-------------------+-------------+
 * | OPTIONS      | NA                | NA          |
 * | HEAD         | NA                | NA          |
 * | GET          | <get>             | QUERY       |
 * | none         | <get-config>      | QUERY_CONFIG|
 * | POST (data)  | <edit-config>     | EDIT_CONFIG |
 * | PUT          | <edit-config>     | EDIT_CONFIG |
 * | PATCH        | <edit-config>     | EDIT_CONFIG |
 * | DELETE       | <edit-config>     | EDIT_CONFIG |
 * | POST (op)    | <rpc>             | RPC         |
 * +--------------+-------------------+-------------+
 *
 * Note: Additionally RESTCONF must use API resource to figure out whether
 * request contains data resource or it's for data model specific operation.
 * +--rw restconf
 * +--rw data
 * +--rw operations
 */
public enum YmsOperationType {
    /**
     * The YANG based request is to edit a config node / subtree in the data
     * store.
     */
    EDIT_CONFIG_REQUEST,

    /**
     * The YANG based request is to query a config node / subtree in the data
     * store.
     */
    QUERY_CONFIG_REQUEST,

    /**
     * The YANG based request is to query a node / subtree in the data store.
     */
    QUERY_REQUEST,

    /**
     * The YANG based request is to execute an RPC defined in YANG.
     */
    RPC_REQUEST,

    /**
     * The YANG based response is for edit operation.
     */
    EDIT_CONFIG_REPLY,

    /**
     * The YANG based response is for query config operation.
     */
    QUERY_CONFIG_REPLY,

    /**
     * The YANG based response is for query operation.
     */
    QUERY_REPLY,

    /**
     * The YANG based response is for a RPC operation.
     */
    RPC_REPLY,

    /**
     * The YANG based request is to execute an RPC defined in YANG.
     */
    NOTIFICATION
}

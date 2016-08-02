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
 * Represents type of YANG data tree node operation.
 *
 * This is used by protocols to specify edit operation associate with the node.
 * YMS data validation and data handling will vary based on the edit operation
 * type, for an instance, default leafs if not present in data should be added
 * if edit operation type is create and shouldn't be added if operation type is
 * delete.
 * Edit operation type is mapped to "operation type" of YANG utils generated
 * classes by YMS.
 *
 * In case of SBI driver/provider creates JAVA object (of YANG utils generated
 * classes) and specifies the edit operation type in "operation type" field.
 * YMS map this operation type to "edit operation type" of YDT, which will be
 * further encoded in corresponding data format.
 *
 * This is only applicable if YANG root level interaction type is EDIT_CONFIG.
 * If edit operation type is not specified when root interaction type is
 * EDIT_CONFIG then default operation type will be selected.
 * By default "default operation type" is "merge" unless explicitly specified
 * by protocol.
 */

/*
 * Edit operation type mapping with RESTCONF and NETCONF as example:
 * +----------+----------------------------+------------+
 * | RESTCONF | NETCONF                    | EditOpType |
 * +----------+----------------------------+------------+
 * | OPTIONS  | none                       | NA         |
 * | HEAD     | none                       | NA         |
 * | GET      | <get-config>, <get>        | NA         |
 * | POST     | (operation="create")       | CREATE     |
 * | PUT      | (operation="replace")      | REPLACE    |
 * | PATCH    | (operation="merge")        | MERGE      |
 * | DELETE   | (operation="delete")       | DELETE     |
 * | none     | (operation="remove")       | REMOVE     |
 * +----------+----------------------------+------------+
 * Note: Additionally RESTCONF must use API resource to figure out whether
 * request contains data resource or it's for data model specific operation.
 *     +--rw restconf
 *       +--rw data
 *       +--rw operations
 * Edit operation type is only applicable for data resource.
 *
 * Additionally protocols has to use operation type NONE to specify the URI
 * path.
 */
public enum YdtContextOperationType {

    /**
     * Type of YANG data tree action for below action:
     * The configuration data identified by the element
     * containing this attribute is added to the configuration if
     * and only if the configuration data does not already exist in
     * the configuration datastore.  If the configuration data
     * exists, an error is returned.
     */
    CREATE,

    /**
     * Type of YANG data tree action for below action:
     * The configuration data identified by the element
     * containing this attribute is deleted from the configuration
     * if and only if the configuration data currently exists in
     * the configuration datastore.  If the configuration data does
     * not exist, an error is returned".
     */
    DELETE,

    /**
     * Type of YANG data tree action for below action:
     * The configuration data identified by the element
     * containing this attribute is merged with the configuration
     * at the corresponding level in the configuration datastore.
     */
    MERGE,

    /**
     * Type of YANG data tree action for below action:
     * The configuration data identified by the element
     * containing this attribute replaces any related configuration
     * in the configuration datastore. If no such configuration
     * data exists in the configuration datastore, it is created.
     */
    REPLACE,

    /**
     * Type of YANG data tree action for below action:
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
    NONE
}


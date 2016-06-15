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
package org.onosproject.yangutils.datamodel;

/**
 * Represents node type in data model tree corresponding to YANG schema.
 */
public enum YangNodeType {
    /**
     * Node contains module information.
     */
    MODULE_NODE,

    /**
     * Node contains sub module information.
     */
    SUB_MODULE_NODE,

    /**
     * Node contains "YANG's typedef" information.
     */
    TYPEDEF_NODE,

    /**
     * Node contains "YANG's type" information.
     */
    TYPE_NODE,

    /**
     * Node contains "YANG's choice" information.
     */
    CHOICE_NODE,

    /**
     * Node contains "YANG's case" information.
     */
    CASE_NODE,

    /**
     * Node contains "YANG's enumeration" information.
     */
    ENUMERATION_NODE,

    /**
     * Node contains grouping information.
     */
    GROUPING_NODE,

    /**
     * Node contains "YANG's uses" information.
     */
    USES_NODE,

    /**
     * Node contains augmentation information.
     */
    AUGMENT_NODE,

    /**
     * Node contains "YANG's container" information.
     */
    CONTAINER_NODE,

    /**
     * Node contains "YANG's notification" information.
     */
    NOTIFICATION_NODE,

    /**
     * Node contains "YANG's input" information.
     */
    INPUT_NODE,

    /**
     * Node contains "YANG's output" information.
     */
    OUTPUT_NODE,

    /**
     * Node contains "YANG's rpc" information.
     */
    RPC_NODE,

    /**
     * Node contains "YANG's union" information.
     */
    UNION_NODE,

    /**
     * Node contains "YANG's list" information.
     */
    LIST_NODE
}

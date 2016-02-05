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
package org.onosproject.yangutils.parser;

/**
 * ENUM to represent the type of data in parse tree.
 */
public enum ParsableDataType {
    /**
     * Identifies the module parsed data.
     */
    MODULE_DATA,

    /**
     * Identifies the sub module parsed data.
     */
    SUB_MODULE_DATA,

    /**
     * Identifies the typedef parsed data.
     */
    TYPEDEF_DATA,

    /**
     * Identifies the type parsed data.
     */
    TYPE_DATA,

    /**
     * Identifies the choice parsed data.
     */
    CHOICE_DATA,

    /**
     * Identifies the case parsed data.
     */
    CASE_DATA,

    /**
     * Identifies the YANG enumeration parsed data.
     */
    ENUMERATION_DATA,

    /**
     * Identifies the grouping parsed data.
     */
    GROUPING_DATA,

    /**
     * Identifies the uses parsed data.
     */
    USES_DATA,

    /**
     * Identifies the augment parsed data.
     */
    AUGMENT_DATA,

    /**
     * Identifies the container parsed data.
     */
    CONTAINER_DATA,

    /**
     * Identifies the YANG list parsed data.
     */
    LIST_DATA,

    /**
     * Identifies the YANG belongs-to parsed data.
     */
    BELONGS_TO_DATA,

    /**
     * Identifies the YANG bit parsed data.
     */
    BIT_DATA,

    /**
     * Identifies the YANG bits parsed data.
     */
    BITS_DATA,

    /**
     * Identifies the YANG enum parsed data.
     */
    ENUM_DATA,

    /**
     * Identifies the YANG import parsed data.
     */
    IMPORT_DATA,

    /**
     * Identifies the YANG include parsed data.
     */
    INCLUDE_DATA,

    /**
     * Identifies the YANG leaf parsed data.
     */
    LEAF_DATA,

    /**
     * Identifies the YANG leaf list parsed data.
     */
    LEAF_LIST_DATA,

    /**
     * Identifies the YANG must parsed data.
     */
    MUST_DATA,

    /**
     * Identifies the YANG revision parsed data.
     */
    REVISION_DATA,

    /**
     * Identifies the YANG namespace parsed data.
     */
    NAMESPACE_DATA
}

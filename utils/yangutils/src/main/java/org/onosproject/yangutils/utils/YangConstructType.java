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
package org.onosproject.yangutils.utils;

/**
 * ENUM to represent the type of data in parse tree.
 */
public enum YangConstructType {
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
     * Identifies the YANG revision date parsed data.
     */
    REVISION_DATE_DATA,

    /**
     * Identifies the YANG namespace parsed data.
     */
    NAMESPACE_DATA,

    /**
     * Identifies the YANG contact parsed data.
     */
    CONTACT_DATA,

    /**
     * Identifies the YANG config parsed data.
     */
    CONFIG_DATA,

    /**
     * Identifies the YANG description parsed data.
     */
    DESCRIPTION_DATA,

    /**
     * Identifies the YANG key parsed data.
     */
    KEY_DATA,

    /**
     * Identifies the YANG mandatory parsed data.
     */
    MANDATORY_DATA,

    /**
     * Identifies the YANG max element parsed data.
     */
    MAX_ELEMENT_DATA,

    /**
     * Identifies the YANG min element parsed data.
     */
    MIN_ELEMENT_DATA,

    /**
     * Identifies the YANG presence element parsed data.
     */
    PRESENCE_DATA,

    /**
     * Identifies the YANG reference element parsed data.
     */
    REFERENCE_DATA,

    /**
     * Identifies the YANG status element parsed data.
     */
    STATUS_DATA,

    /**
     * Identifies the YANG units element parsed data.
     */
    UNITS_DATA,

    /**
     * Identifies the YANG version element parsed data.
     */
    VERSION_DATA,

    /**
     * Identifies the YANG base element parsed data.
     */
    YANGBASE_DATA,

    /**
     * Identifies the YANG prefix element parsed data.
     */
    PREFIX_DATA,

    /**
     * Identifies the YANG default element parsed data.
     */
    DEFAULT_DATA,

    /**
     * Identifies the YANG value element parsed data.
     */
    VALUE_DATA,

    /**
     * Identifies the YANG organization parsed data.
     */
    ORGANIZATION_DATA,

    /**
     * Identifies the YANG position element parsed data.
     */
    POSITION_DATA,

    /**
     * Identifies the YANG data definition statements.
     */
    DATA_DEF_DATA,

    /**
     * Identifies the derived data type.
     */
    DERIVED;

    /**
     * Returns the YANG construct keyword corresponding to enum values.
     *
     * @param yangConstructType enum value for parsable data type.
     * @return YANG construct keyword.
     */
    public static String getYangConstructType(YangConstructType yangConstructType) {

        switch (yangConstructType) {
            case MODULE_DATA:
                return "module";
            case SUB_MODULE_DATA:
                return "submodule";
            case TYPEDEF_DATA:
                return "typedef";
            case TYPE_DATA:
                return "type";
            case CHOICE_DATA:
                return "choice";
            case CASE_DATA:
                return "case";
            case ENUMERATION_DATA:
                return "enumeration";
            case GROUPING_DATA:
                return "grouping";
            case USES_DATA:
                return "uses";
            case AUGMENT_DATA:
                return "augment";
            case CONTAINER_DATA:
                return "container";
            case LIST_DATA:
                return "list";
            case BELONGS_TO_DATA:
                return "belongs-to";
            case BIT_DATA:
                return "bit";
            case BITS_DATA:
                return "bits";
            case ENUM_DATA:
                return "enum";
            case IMPORT_DATA:
                return "import";
            case INCLUDE_DATA:
                return "include";
            case LEAF_DATA:
                return "leaf";
            case LEAF_LIST_DATA:
                return "leaf-list";
            case MUST_DATA:
                return "must";
            case REVISION_DATA:
                return "revision";
            case REVISION_DATE_DATA:
                return "revision-date";
            case NAMESPACE_DATA:
                return "namespace";
            case CONTACT_DATA:
                return "contact";
            case CONFIG_DATA:
                return "config";
            case DESCRIPTION_DATA:
                return "description";
            case KEY_DATA:
                return "key";
            case MANDATORY_DATA:
                return "mandatory";
            case MAX_ELEMENT_DATA:
                return "max-elements";
            case MIN_ELEMENT_DATA:
                return "min-elements";
            case PRESENCE_DATA:
                return "presence";
            case REFERENCE_DATA:
                return "reference";
            case STATUS_DATA:
                return "status";
            case UNITS_DATA:
                return "units";
            case VERSION_DATA:
                return "version";
            case YANGBASE_DATA:
                return "yangbase";
            case PREFIX_DATA:
                return "prefix";
            case ORGANIZATION_DATA:
                return "organization";
            case VALUE_DATA:
                return "value";
            case POSITION_DATA:
                return "position";
            case DEFAULT_DATA:
                return "default";
            case DATA_DEF_DATA:
                return "data-def-substatements";
            case DERIVED:
                return "derived";
            default:
                return "yang";
        }
    }
}

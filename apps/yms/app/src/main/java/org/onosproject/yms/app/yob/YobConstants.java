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

package org.onosproject.yms.app.yob;

/**
 * Represents common constant utility for YANG object builder.
 */
final class YobConstants {

    private YobConstants() {
    }

    static final String FROM_STRING = "fromString";
    static final String BUILD = "build";
    static final String OP_PARAM = "OpParam";
    static final String DEFAULT = "Default";
    static final String ADD_TO = "addTo";
    static final String VALUE_OF = "valueOf";
    static final String OP_TYPE = "OpType";
    static final String ONOS_YANG_OP_TYPE = "OnosYangOpType";
    static final String OF = "of";
    static final String PERIOD = ".";
    static final String SPACE = " ";
    static final String ADD_AUGMENT_METHOD = "addYangAugmentedInfo";
    static final String YANG = "yang";
    static final String JAVA_LANG = "java.lang";
    static final String LEAF_IDENTIFIER = "LeafIdentifier";
    static final String SELECT_LEAF = "selectLeaf";
    static final String EVENT_SUBJECT = "EventSubject";
    static final String EVENT = "Event";
    static final String TYPE = "Type";

    //Error strings
    static final String E_NO_HANDLE_FOR_YDT = "No handler for YDT node";
    static final String E_HAS_NO_CHILD = " does not have child ";
    static final String E_SET_OP_TYPE_FAIL = "Failed to set Operation Type";
    static final String E_FAIL_TO_BUILD = "Failed to build the object: ";
    static final String L_FAIL_TO_BUILD = "Failed to build the object: {}";
    static final String E_FAIL_TO_GET_FIELD = "Failed to get field for class: ";
    static final String L_FAIL_TO_GET_FIELD =
            "Failed to get field for class: {}";
    static final String E_FAIL_TO_GET_METHOD =
            "Failed to get method for class: ";
    static final String L_FAIL_TO_GET_METHOD =
            "Failed to get method for class: {}";
    static final String E_FAIL_TO_LOAD_CLASS =
            "Failed to load class for class: ";
    static final String L_FAIL_TO_LOAD_CLASS =
            "Failed to load class for class: {}";
    static final String E_YDT_TYPE_IS_NOT_SUPPORT =
            "Given YDT type is not supported.";
    static final String E_FAIL_TO_CREATE_OBJ =
            "Failed to create an object for class: ";
    static final String L_FAIL_TO_CREATE_OBJ =
            "Failed to create an object for class: {}";
    static final String E_REFLECTION_FAIL_TO_CREATE_OBJ =
            "Reflection failed to create an object for class: ";
    static final String L_REFLECTION_FAIL_TO_CREATE_OBJ =
            "Reflection failed to create an object for class: {}";
    static final String E_FAIL_TO_LOAD_CONSTRUCTOR =
            "Failed to load constructor for class: {}";
    static final String E_FAIL_TO_INVOKE_METHOD =
            "Failed to invoke method for class: ";
    static final String L_FAIL_TO_INVOKE_METHOD =
            "Failed to invoke method for class: {}";
    static final String E_DATA_TYPE_NOT_SUPPORT =
            "Given data type is not supported.";
    static final String E_OBJ_IS_ALREADY_BUILT_NOT_FETCH =
            "Object is already built, cannot fetch builder";
    static final String E_BUILDER_IS_NOT_SET =
            "Builder is not yet set, cannot fetch it";
    static final String E_BUILT_OBJ_IS_NOT_SET =
            "Built object is not set";
    static final String E_OBJ_IS_ALREADY_BUILT_NOT_SET =
            "Object is already built, cannot set builder";
    static final String E_BUILDER_IS_NOT_ALREADY_SET =
            "Builder is not already set";
    static final String E_OBJ_IS_NOT_SET_NOT_FETCH =
            "Builder is not yet set, cannot fetch it";
    static final String E_OBJ_IS_ALREADY_BUILT_NOT_BUILD =
            "Object is already built, cannot build again";
    static final String E_OBJ_BUILDING_WITHOUT_BUILDER =
            "Object building without builder";
    static final String E_MISSING_DATA_IN_NODE =
            "YANG data tree is missing the data required for YOB";
    static final String E_INVALID_DATA_TREE =
            "YANG tree does not have a application root";
    static final String E_INVALID_EMPTY_DATA =
            "Value for empty data type is invalid";
}

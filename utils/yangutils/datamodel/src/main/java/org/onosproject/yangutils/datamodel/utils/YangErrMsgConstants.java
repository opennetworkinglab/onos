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

package org.onosproject.yangutils.datamodel.utils;

/**
 * Represents default YANG error message types.
 */
public final class YangErrMsgConstants {

    /**
     * Static attribute for operation failed error tag.
     */
    public static final String OPERATION_FAILED_ERROR_TAG = "operation-failed";

    /**
     * Static attribute for data missing error tag.
     */
    public static final String DATA_MISSING_ERROR_TAG = "data-missing";

    /**
     * Static attribute for bad attribute error tag.
     */
    public static final String BAD_ATTRIBUTE_ERROR_TAG = "bad-attribute";

    /**
     * Static attribute for data not unique error app tag.
     */
    public static final String DATA_NOT_UNIQUE_ERROR_APP_TAG = "data-not-unique";

    /**
     * Static attribute for too many elements error app tag.
     */
    public static final String TOO_MANY_ELEMENTS_ERROR_APP_TAG = "too-many-elements";

    /**
     * Static attribute for too few elements error app tag.
     */
    public static final String TOO_FEW_ELEMENTS_ERROR_APP_TAG = "too-few-elements";

    /**
     * Static attribute for must violation error app tag.
     */
    public static final String MUST_VIOLATION_ERROR_APP_TAG = "must-violation";

    /**
     * Static attribute for instance required error app tag.
     */
    public static final String INSTANCE_REQUIRED_ERROR_APP_TAG = "instance-required";

    /**
     * Static attribute for missing choice error app tag.
     */
    public static final String MISSING_CHOICE_ERROR_APP_TAG = "missing-choice";

    /**
     * Static attribute for missing instance error app tag.
     */
    public static final String MISSING_INSTANCE_ERROR_APP_TAG = "missing-instance";

    /**
     * TODO: Static attribute for error path to the instance-identifier leaf.
     */
    public static final String ERROR_PATH_INSTANCE_IDENTIFIER_LEAF = "Path to the instance-identifier leaf.";

    /**
     * Static attribute for error path to the missing choice.
     */
    public static final String ERROR_PATH_MISSING_CHOICE = "Path to the element with the missing choice.";

    /**
     * Static attribute for error path to the leafref leaf.
     */
    public static final String ERROR_PATH_LEAFREF_LEAF = "Path to the leafref leaf.";

    /**
     * Creates an instance of yang error message constants.
     */
    private YangErrMsgConstants() {
    }
}

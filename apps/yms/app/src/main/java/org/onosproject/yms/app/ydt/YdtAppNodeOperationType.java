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

package org.onosproject.yms.app.ydt;

/**
 * Represents type of YANG data tree node operation.
 */
public enum YdtAppNodeOperationType {

    /**
     * Type of YANG application node operation for below action:
     * The application containing this attribute has edit operation
     * type as delete/remove in its complete ydtTree.
     */
    DELETE_ONLY,

    /**
     * Type of YANG application node operation for below action:
     * The application containing this attribute has edit operation
     * type other than delete/remove in its complete ydtTree.
     */
    OTHER_EDIT,

    /**
     * Type of YANG application node operation for below action:
     * The application containing this attribute has edit operation
     * type of combination of any edit operation type in its complete ydtTree.
     */
    BOTH
}


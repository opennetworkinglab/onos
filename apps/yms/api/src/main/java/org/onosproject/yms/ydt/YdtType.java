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
 * Represents type of node in YANG data tree. Protocols based on input data
 * format provide this information to YMS during YDT building. YMS use this
 * information to carry out the validation against the schema information
 * obtained as a part of application registration. Also as a part of response
 * YMS encode this information in YDT node, protocol may use this information
 * while construction the data format string.
 *
 * Protocols unaware of node type like NETCONF, may opt not to provide this
 * information.
 */
public enum YdtType {

    /**
     * Single instance node.
     */
    SINGLE_INSTANCE_NODE,

    /**
     * Multi instance node.
     */
    MULTI_INSTANCE_NODE,

    /**
     * Single instance leaf node.
     */
    SINGLE_INSTANCE_LEAF_VALUE_NODE,

    /**
     * Multi instance leaf node.
     */
    MULTI_INSTANCE_LEAF_VALUE_NODE
}

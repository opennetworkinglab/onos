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
 * Abstraction of an entity which represent YANG data tree. This is used
 * for exchanging information between YANG management system and NBI protocol.
 */
public interface Ydt {

    /**
     * Returns the root context information available in YDT node. This root
     * node is a logical container of a protocol which holds the complete data
     * tree. After building YANG data tree, root node can be obtained from this.
     *
     * @return root YDT context which is logical container of a protocol which
     * is holder of the complete tree
     */
    YdtContext getRootNode();

    /**
     * Returns YANG management system operation type. It represents type of
     * root level operation for the request. This is used by protocols to
     * specify the root level operation associated with the request.
     *
     * @return YANG management system operation type
     */
    YmsOperationType getYmsOperationType();
}

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

package org.onosproject.yms.app.ytb;

import org.onosproject.yangutils.datamodel.YangNode;
import org.onosproject.yms.app.utils.TraversalType;

/**
 * Represents YTB Traversal info which is needed every time the traversal of
 * a YANG node happens. This contains YANG node and its corresponding traversal
 * type information.
 */
public class YtbTraversalInfo {

    /**
     * YANG node of the current traversal.
     */
    private YangNode yangNode;

    /**
     * Traverse type of the current traversal.
     */
    private TraversalType traverseType;

    /**
     * Creates YTB traversal info by taking the traversal type and the YANG
     * node.
     *
     * @param yangNode     YANG node
     * @param traverseType traversal type
     */
    public YtbTraversalInfo(YangNode yangNode, TraversalType traverseType) {
        this.yangNode = yangNode;
        this.traverseType = traverseType;
    }

    /**
     * Returns the YANG node of the current traversal.
     *
     * @return YANG node
     */
    public YangNode getYangNode() {
        return yangNode;
    }

    /**
     * Returns the traversal type of the current traversal.
     *
     * @return traversal type
     */
    public TraversalType getTraverseType() {
        return traverseType;
    }
}

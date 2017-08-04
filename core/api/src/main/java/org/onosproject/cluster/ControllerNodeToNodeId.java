/*
 * Copyright 2014-present Open Networking Foundation
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
package org.onosproject.cluster;

import com.google.common.base.Function;

/**
 * Function to convert ControllerNode to NodeId.
 */
public final class ControllerNodeToNodeId
    implements Function<ControllerNode, NodeId> {

    private static final ControllerNodeToNodeId INSTANCE = new ControllerNodeToNodeId();

    @Override
    public NodeId apply(ControllerNode input) {
        if (input == null) {
            return null;
        } else {
            return input.id();
        }
    }

    /**
     * Returns a Function to convert ControllerNode to NodeId.
     *
     * @return ControllerNodeToNodeId instance.
     */
    public static ControllerNodeToNodeId toNodeId() {
        return INSTANCE;
    }
}

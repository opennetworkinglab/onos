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
package org.onosproject.tetopology.management.api.node;

import java.util.List;
import java.util.Map;

import org.onosproject.tetopology.management.api.KeyId;

/**
 * Abstraction of a network node.
 */
public interface NetworkNode {

    /**
     * Returns the node identifier.
     *
     * @return node identifier
     */
    KeyId nodeId();

    /**
     * Returns the supporting node identifiers.
     *
     * @return list of the ids of the supporting nodes
     */
    List<NetworkNodeKey> supportingNodeIds();

    /**
     * Returns the node TE extension attributes.
     *
     * @return node TE extension attributes
     */
    TeNode teNode();

    /**
     * Returns a collection of currently known termination points.
     *
     * @return a collection of termination points associated with this node
     */
    Map<KeyId, TerminationPoint> terminationPoints();

    /**
     * Returns the termination point.
     *
     * @param tpId termination point id
     * @return value of termination point
     */
    TerminationPoint terminationPoint(KeyId tpId);

}

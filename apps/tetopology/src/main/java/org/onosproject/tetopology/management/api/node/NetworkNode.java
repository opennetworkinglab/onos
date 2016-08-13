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

import org.onosproject.tetopology.management.api.KeyId;
import org.onosproject.tetopology.management.api.TeTopologyEventSubject;

/**
 * Abstraction of a network node.
 */
public interface NetworkNode extends TeTopologyEventSubject {

    /**
     * Returns the node id.
     *
     * @return node identifier
     */
    KeyId nodeId();

    /**
     * Returns the supporting node ids.
     *
     * @return list of the ids of the supporting nodes
     */
    List<NetworkNodeKey> getSupportingNodeIds();

    /**
     * Returns the node TE extension attributes.
     *
     * @return node TE extension attributes
     */
    TeNode getTe();

    /**
     * Returns a collection of currently known termination points.
     *
     * @return a collection of termination points associated with this node
     */
    List<TerminationPoint> getTerminationPoints();

    /**
     * Returns the termination point.
     *
     * @param  tpId termination point id
     * @return value of termination point
     */
    TerminationPoint getTerminationPoint(KeyId tpId);

}

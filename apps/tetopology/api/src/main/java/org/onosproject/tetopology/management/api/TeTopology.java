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
package org.onosproject.tetopology.management.api;

import org.onosproject.net.DeviceId;
import org.onosproject.tetopology.management.api.link.TeLink;
import org.onosproject.tetopology.management.api.link.TeLinkTpKey;
import org.onosproject.tetopology.management.api.node.TeNode;

import java.util.BitSet;
import java.util.Map;

/**
 * Abstraction of a TE topology.
 */
public interface TeTopology extends TeTopologyEventSubject {
    /**
     * Indicates that the specified topology is not usable for
     * any ACTN operations.
     */
    public static final int BIT_DISABLED = 0;

    /**
     * Indicates that the topology is auto-constructed by
     * the controller by an auto-discovery mechanism.
     */
    public static final int BIT_LEARNT = 1;

    /**
     * Indicates that the topology is merged from other
     * supporting topologies.
     */
    public static final int BIT_MERGED = 2;

    /**
     * Indicates that the topology is customized based on
     * another topology.
     */
    public static final int BIT_CUSTOMIZED = 3;

    /**
     * Returns the TE topology identifier.
     *
     * @return the topology id
     */
    TeTopologyKey teTopologyId();

    /**
     * Returns the topology characteristics flags.
     *
     * @return the flags
     */
    public BitSet flags();

    /**
     * Returns the topology optimization criteria.
     *
     * @return the optimization
     */
    public OptimizationType optimization();

    /**
     * Returns a collection of TE nodes in the topology.
     *
     * @return a collection of currently known TE nodes
     */
    Map<Long, TeNode> teNodes();

    /**
     * Returns a TE node in the topology that matches the given node
     * identifier. A null will be returned if no such node exists.
     *
     * @param teNodeId the TE node id
     * @return the corresponding node; or null if not found
     */
    TeNode teNode(long teNodeId);

    /**
     * Returns a collection of links in the topology.
     *
     * @return a collection of currently known te links
     */
    Map<TeLinkTpKey, TeLink> teLinks();

    /**
     * Returns a TE link in the topology that matches the given
     * link identifier. If no such a link is found, a null is returned.
     *
     * @param teLinkId the TE link id
     * @return the corresponding link; or null if not found
     */
    TeLink teLink(TeLinkTpKey teLinkId);

    /**
     * Returns the TE topology identifier string value.
     *
     * @return the topology id in String format
     */
    String teTopologyIdStringValue();

    /**
     * Returns the network identifier.
     *
     * @return network identifier
     */
    KeyId networkId();

    /**
     * Returns the identity of the controller owning this abstracted topology.
     *
     * @return the controller id
     */
    DeviceId ownerId();

}

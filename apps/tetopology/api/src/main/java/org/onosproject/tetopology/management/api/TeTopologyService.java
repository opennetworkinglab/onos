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
package org.onosproject.tetopology.management.api;

import org.onosproject.event.ListenerService;
import org.onosproject.tetopology.management.api.link.NetworkLinkKey;
import org.onosproject.tetopology.management.api.link.TeLink;
import org.onosproject.tetopology.management.api.link.TeLinkTpGlobalKey;
import org.onosproject.tetopology.management.api.node.NetworkNodeKey;
import org.onosproject.tetopology.management.api.node.TeNode;
import org.onosproject.tetopology.management.api.node.TeNodeKey;
import org.onosproject.tetopology.management.api.node.TerminationPointKey;
import org.onosproject.tetopology.management.api.node.TtpKey;
import org.onosproject.tetopology.management.api.node.TunnelTerminationPoint;

/**
 * TE Topology Service API.
 */
public interface TeTopologyService
        extends ListenerService<TeTopologyEvent, TeTopologyListener> {

    /**
     * Returns a collection of currently known networks.
     *
     * @return a collection of networks
     */
    Networks networks();

    /**
     * Returns the network identified by its network identifier. if no
     * network can be found, a null is returned.
     *
     * @param networkId network id in URI format
     * @return the corresponding network; or null if not found
     */
    Network network(KeyId networkId);

    /**
     * Updates the network.
     *
     * @param network network to be updated
     */
    void updateNetwork(Network network);

    /**
     * Removes the network corresponding to the given network identifier.
     *
     * @param networkId network id in URI format
     */
    void removeNetwork(KeyId networkId);

    /**
     * Returns a collection of currently known TE topologies.
     *
     * @return a collection of topologies
     */
    TeTopologies teTopologies();

    /**
     * Returns the TE Topology identified by the given key.
     *
     * @param topologyKey the given TE topology Key
     * @return the corresponding TE topology
     */
    TeTopology teTopology(TeTopologyKey topologyKey);

    /**
     * Returns the merged topology in MDSC.
     *
     * @return the merged topology
     */
    TeTopology mergedTopology();

    /**
     * Creates or Updates a TE topology based on the given topology.
     *
     * @param teTopology the given TE topology
     */
    void updateTeTopology(TeTopology teTopology);

    /**
     * Removes the TE Topology identified by its key. Does nothing if
     * no topology is found which matches the key.
     *
     * @param topologyKey the TE topology key
     */
    void removeTeTopology(TeTopologyKey topologyKey);

    /**
     * Returns the TE node identified by the given node key. If no TE
     * node is found, a null is returned.
     *
     * @param nodeKey the TE node key
     * @return the corresponding TE node,or null
     */
    TeNode teNode(TeNodeKey nodeKey);

    /**
     * Returns the TE link identified by the given TE link key. If no
     * TE link is found, a null is returned.
     *
     * @param linkKey the TE link key
     * @return the corresponding TE link or null
     */
    TeLink teLink(TeLinkTpGlobalKey linkKey);

    /**
     * Returns a tunnel termination point identified by the given tunnel
     * termination point key. If no tunnel termination point is found,
     * a null is returned.
     *
     * @param ttpKey the tunnel termination point key
     * @return the corresponding tunnel termination point
     */
    TunnelTerminationPoint tunnelTerminationPoint(TtpKey ttpKey);

    /**
     * Returns the network Id for a TE Topology key.
     *
     * @param teTopologyKey the TE topology key
     * @return value of network Id
     */
    KeyId networkId(TeTopologyKey teTopologyKey);

    /**
     * Returns the network node key for a TE node key.
     *
     * @param teNodeKey a TE node key
     * @return value of network node key
     */
    NetworkNodeKey nodeKey(TeNodeKey teNodeKey);

    /**
     * Returns the network link key for a TE link key.
     *
     * @param teLinkKey a TE node key
     * @return value of network link key
     */
    NetworkLinkKey linkKey(TeLinkTpGlobalKey teLinkKey);

    /**
     * Returns the termination point key for a TE termination point key.
     *
     * @param teTpKey a TE termination point key
     * @return value of termination point key
     */
    TerminationPointKey terminationPointKey(TeLinkTpGlobalKey teTpKey);

    /**
     * Returns the TE controller global identification.
     *
     * @return value of controller id
     */
    long teContollerId();
}

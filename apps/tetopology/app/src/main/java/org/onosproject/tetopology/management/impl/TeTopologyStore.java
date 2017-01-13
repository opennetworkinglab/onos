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
package org.onosproject.tetopology.management.impl;

import java.util.List;
import java.util.concurrent.BlockingQueue;

import org.onosproject.store.Store;
import org.onosproject.tetopology.management.api.KeyId;
import org.onosproject.tetopology.management.api.Network;
import org.onosproject.tetopology.management.api.TeTopologies;
import org.onosproject.tetopology.management.api.TeTopology;
import org.onosproject.tetopology.management.api.TeTopologyEvent;
import org.onosproject.tetopology.management.api.TeTopologyKey;
import org.onosproject.tetopology.management.api.link.NetworkLink;
import org.onosproject.tetopology.management.api.link.NetworkLinkKey;
import org.onosproject.tetopology.management.api.link.TeLink;
import org.onosproject.tetopology.management.api.link.TeLinkTpGlobalKey;
import org.onosproject.tetopology.management.api.node.NetworkNode;
import org.onosproject.tetopology.management.api.node.NetworkNodeKey;
import org.onosproject.tetopology.management.api.node.TeNode;
import org.onosproject.tetopology.management.api.node.TeNodeKey;
import org.onosproject.tetopology.management.api.node.TerminationPoint;
import org.onosproject.tetopology.management.api.node.TerminationPointKey;
import org.onosproject.tetopology.management.api.node.TtpKey;
import org.onosproject.tetopology.management.api.node.TunnelTerminationPoint;

/**
 * Inventory of TE network topology.
 */
public interface TeTopologyStore
        extends Store<TeTopologyEvent, TeTopologyStoreDelegate> {

    /**
     * Returns a collection of currently known networks.
     *
     * @return a collection of stored networks
     */
    List<Network> networks();

    /**
     * Returns a network.
     *
     * @param  networkId network id in URI format
     * @return value of network
     */
    Network network(KeyId networkId);

    /**
     * Updates a network.
     *
     * @param network value of the network to be updated
     */
    void updateNetwork(Network network);

    /**
     * Removes a network.
     *
     * @param  networkId network id in URI format
     */
    void removeNetwork(KeyId networkId);

    /**
     * Returns a network link.
     *
     * @param linkKey link key
     * @return value of network link
     */
    NetworkLink networkLink(NetworkLinkKey linkKey);

    /**
     * Updates a network link.
     *
     * @param linkKey link key
     * @param link link object to be updated
    */
    void updateNetworkLink(NetworkLinkKey linkKey, NetworkLink link);

    /**
     * Removes a network link.
     *
     * @param linkKey link key
     */
    void removeNetworkLink(NetworkLinkKey linkKey);

    /**
     * Returns a network node.
     *
     * @param nodeKey node key
     * @return value of network node
     */
    NetworkNode networkNode(NetworkNodeKey nodeKey);

    /**
     * Updates a network node.
     *
     * @param nodeKey node key
     * @param node node object to be updated
     */
    void updateNetworkNode(NetworkNodeKey nodeKey, NetworkNode node);

    /**
     * Removes a network node.
     *
     * @param nodeKey node key
     */
    void removeNetworkNode(NetworkNodeKey nodeKey);

    /**
     * Updates a terminationPoint.
     *
     * @param terminationPointKey termination point id
     * @param terminationPoint termination point object to be updated
     */
    void updateTerminationPoint(TerminationPointKey terminationPointKey,
                                TerminationPoint terminationPoint);

    /**
     * Removes a terminationPoint.
     *
     * @param terminationPointKey termination point id
     */
    void removeTerminationPoint(TerminationPointKey terminationPointKey);

    /**
     * Returns a collection of currently known TE topologies.
     *
     * @return a collection of topologies
     */
    TeTopologies teTopologies();

    /**
     * Returns the TE Topology identified by its Id.
     *
     * @param  topologyId TE topology Key
     * @return value of TeTopology
     */
    TeTopology teTopology(TeTopologyKey topologyId);

    /**
     * Creates or updates a TE topology.
     *
     * @param teTopology value of the TE topology to be updated
     */
    void updateTeTopology(TeTopology teTopology);

    /**
     * Removes the TE Topology identified by its Id.
     *
     * @param topologyId TE topology key
     */
    void removeTeTopology(TeTopologyKey topologyId);

    /**
     * Returns the TE node identified by its Id.
     *
     * @param  nodeId the te node key
     * @return value of node
     */
    TeNode teNode(TeNodeKey nodeId);

    /**
     * Creates or updates a TE Node.
     *
     * @param nodeKey te node id
     * @param node node object to be updated
     */
    void updateTeNode(TeNodeKey nodeKey, TeNode node);

    /**
     * Removes the TE node identified by its Id.
     *
     * @param  nodeId the te node key
     */
    void removeTeNode(TeNodeKey nodeId);

    /**
     * Returns the TE link identified by its Id.
     *
     * @param  linkId the te link key
     * @return value of link
     */
    TeLink teLink(TeLinkTpGlobalKey linkId);

    /**
     * Creates or updates a TE Link.
     *
     * @param linkKey link id
     * @param link teLink object to be updated
     */
    void updateTeLink(TeLinkTpGlobalKey linkKey, TeLink link);

    /**
     * Removes the TE link identified by its Id.
     *
     * @param  linkId the te link key
     */
    void removeTeLink(TeLinkTpGlobalKey linkId);

    /**
     * Returns a tunnel termination point identified by its id.
     *
     * @param  ttpId the tunnel termination point key
     * @return the tunnel termination point
     */
    TunnelTerminationPoint tunnelTerminationPoint(TtpKey ttpId);

    /**
     * Returns the network Id for a TE Topology key.
     *
     * @param  teTopologyKey a TE topology key
     * @return value of network Id
     */
    KeyId networkId(TeTopologyKey teTopologyKey);

    /**
     * Returns the network node key for a TE node key.
     *
     * @param  teNodeKey a TE node key
     * @return value of network node key
     */
    NetworkNodeKey nodeKey(TeNodeKey teNodeKey);

    /**
     * Returns the network link key for a TE link key.
     *
     * @param  teLinkKey a TE node key
     * @return value of network link key
     */
    NetworkLinkKey linkKey(TeLinkTpGlobalKey teLinkKey);

    /**
     * Returns the termination point key for a TE termination point key.
     *
     * @param  teTpKey a TE termination point key
     * @return value of termination point key
     */
    TerminationPointKey terminationPointKey(TeLinkTpGlobalKey teTpKey);

    /**
     * Returns the long value of next available TE topology id.
     *
     * @return value of TE topology id
     */
    long nextTeTopologyId();

    /**
     * Returns the next available TE node Id in a TE topology.
     *
     * @param topologyKey TE topology key
     * @return value of TE node id
     */
    long nextTeNodeId(TeTopologyKey topologyKey);

    /**
     * Sets the next available TE node Id in a TE topology.
     *
     * @param topologyKey TE topology key
     * @param nextNodeId value of next TE node id
     */
    void setNextTeNodeId(TeTopologyKey topologyKey, long nextNodeId);

    /**
     * Returns the queue to store the events originating from consistent maps.
     *
     * @return value of the blocking queue
     */
    BlockingQueue<TeTopologyMapEvent> mapEventQueue();

    /**
     * Sets the provider ID.
     *
     * @param providerId value of provider Id
     */
    void setProviderId(long providerId);

}

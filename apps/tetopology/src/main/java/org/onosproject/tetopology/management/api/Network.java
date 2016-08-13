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

import java.util.List;

import org.onosproject.tetopology.management.api.link.NetworkLink;
import org.onosproject.tetopology.management.api.node.NetworkNode;

/**
 * Abstraction of a network element.
 */
public interface Network extends TeTopologyEventSubject {

    /**
     * Returns the network identifier / key of this element.
     *
     * @return network identifier
     */
    KeyId networkId();

    /**
     * Returns the attribute of supporting Network.
     *
     * @return list of the ids of the supporting networks
     */
    List<KeyId> getSupportingNetworkIds();

    /**
     * Returns a collection of nodes in the network identified by the specified
     * network id.
     *
     * @return a collection of currently known nodes
     */
    List<NetworkNode> getNodes();

    /**
     * Returns the node.
     *
     * @param  nodeId node id URI format
     * @return value of node
     */
    NetworkNode getNode(KeyId nodeId);

    /**
     * Returns a collection of links in the network identified by the specified
     * network id.
     *
     * @return a collection of currently known links
     */
    List<NetworkLink> getLinks();

    /**
     * Returns the link.
     *
     * @param  linkId link id in URI format
     * @return value of link
     */
    NetworkLink getLink(KeyId linkId);

    /**
     * Returns if the network is provided by a server or is configured by a
     * client.
     *
     * @return true if the network is provided by a server; false otherwise
     */
    boolean isServerProvided();

    /**
     * Returns the TE topology Id.
     *
     * @return TE topology id for this network
     */
    TeTopologyId getTeTopologyId();
}

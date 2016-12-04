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
import java.util.Map;

import org.onosproject.net.DeviceId;
import org.onosproject.tetopology.management.api.link.NetworkLink;
import org.onosproject.tetopology.management.api.node.NetworkNode;

/**
 * Abstraction of a network element.
 */
public interface Network extends TeTopologyEventSubject {

    /**
     * Returns the network identifier / key.
     *
     * @return network identifier
     */
    KeyId networkId();

    /**
     * Returns the network keys (or identifiers) of the supporting
     * networks which serve as the underlay networks of the current
     * network which is mapped by the specified network identifier.
     *
     * @return list of network keys
     */
    List<KeyId> supportingNetworkIds();

    /**
     * Returns a collection of the network nodes of the network mapped
     * by the specified network identifier.
     *
     * @return a collection of network nodes
     */
    Map<KeyId, NetworkNode> nodes();

    /**
     * Returns the network node corresponding to the given identifier
     * which is encoded as a URI. If no node is found, a null
     * is returned.
     *
     * @param nodeId node id
     * @return value of node or null
     */
    NetworkNode node(KeyId nodeId);

    /**
     * Returns a collection of links in the network mapped by the specified
     * network identifier.
     *
     * @return a collection of currently known links
     */
    Map<KeyId, NetworkLink> links();

    /**
     * Returns the link corresponding to the given identifier which is
     * encoded as a URI. If no such a link is found, a null is returned.
     *
     * @param linkId link id
     * @return value of the link
     */
    NetworkLink link(KeyId linkId);

    /**
     * Returns true if the network is provided by a server, or false if
     * configured by a client.
     *
     * @return true if the network is provided by a server; false otherwise
     */
    boolean isServerProvided();

    /**
     * Returns the TE topology identifier for this network.
     *
     * @return TE topology id
     */
    TeTopologyId teTopologyId();

    /**
     * Returns the controller identifier owning this abstracted topology.
     *
     * @return the controller id
     */
    DeviceId ownerId();
}

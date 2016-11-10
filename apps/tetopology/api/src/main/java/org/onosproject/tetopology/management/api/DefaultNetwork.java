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

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.onosproject.net.DeviceId;
import org.onosproject.tetopology.management.api.link.NetworkLink;
import org.onosproject.tetopology.management.api.node.NetworkNode;

import java.util.List;
import java.util.Map;

/**
 * Default Network implementation.
 */
public class DefaultNetwork implements Network {
    private final KeyId networkId;
    private final List<KeyId> supportingNetworkIds;
    private final Map<KeyId, NetworkNode> nodes;
    private final Map<KeyId, NetworkLink> links;
    private final TeTopologyId teTopologyId;
    private final boolean serverProvided;
    private final DeviceId ownerId;

    /**
     * Creates an instance of DefaultNetwork.
     *
     * @param networkId            network identifier
     * @param supportingNetworkIds supporting network identifier
     * @param nodes                list of nodes within the network
     * @param links                list of links within the network
     * @param teTopologyId         TE topology identifier
     * @param serverProvided       whether the network is received from server
     * @param ownerId              the the controller identifier owning this topology
     */
    public DefaultNetwork(KeyId networkId, List<KeyId> supportingNetworkIds,
                          Map<KeyId, NetworkNode> nodes, Map<KeyId, NetworkLink> links,
                          TeTopologyId teTopologyId, boolean serverProvided,
                          DeviceId ownerId) {
        this.networkId = networkId;
        this.supportingNetworkIds = supportingNetworkIds != null ?
                Lists.newArrayList(supportingNetworkIds) : null;
        this.nodes = nodes != null ? Maps.newHashMap(nodes) : null;
        this.links = links != null ? Maps.newHashMap(links) : null;
        this.teTopologyId = teTopologyId;
        this.serverProvided = serverProvided;
        this.ownerId = ownerId;
    }


    @Override
    public KeyId networkId() {
        return networkId;
    }

    @Override
    public List<KeyId> supportingNetworkIds() {
        if (supportingNetworkIds == null) {
            return null;
        }
        return ImmutableList.copyOf(supportingNetworkIds);
    }

    @Override
    public Map<KeyId, NetworkNode> nodes() {
        if (nodes == null) {
            return null;
        }
        return ImmutableMap.copyOf(nodes);
    }

    @Override
    public NetworkNode node(KeyId nodeId) {
        return nodes.get(nodeId);
    }

    @Override
    public Map<KeyId, NetworkLink> links() {
        if (links == null) {
            return null;
        }
        return ImmutableMap.copyOf(links);
    }

    @Override
    public NetworkLink link(KeyId linkId) {
        return links.get(linkId);
    }

    @Override
    public boolean isServerProvided() {
        return serverProvided;
    }

    @Override
    public TeTopologyId teTopologyId() {
        return teTopologyId;
    }

    @Override
    public DeviceId ownerId() {
        return ownerId;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(networkId, supportingNetworkIds,
                                nodes, links, serverProvided, teTopologyId, ownerId);
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (object instanceof DefaultNetwork) {
            DefaultNetwork that = (DefaultNetwork) object;
            return Objects.equal(networkId, that.networkId) &&
                    Objects.equal(supportingNetworkIds, that.supportingNetworkIds) &&
                    Objects.equal(nodes, that.nodes) &&
                    Objects.equal(links, that.links) &&
                    Objects.equal(serverProvided, that.serverProvided) &&
                    Objects.equal(teTopologyId, that.teTopologyId) &&
                    Objects.equal(ownerId, that.ownerId);
        }
        return false;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("networkId", networkId)
                .add("supportingNetworkIds", supportingNetworkIds)
                .add("nodes", nodes)
                .add("links", links)
                .add("serverProvided", serverProvided)
                .add("teTopologyId", teTopologyId)
                .add("ownerId", ownerId)
                .toString();
    }
}

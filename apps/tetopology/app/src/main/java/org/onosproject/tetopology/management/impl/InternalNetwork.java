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
import java.util.Map;

import org.apache.commons.collections.MapUtils;
import org.onosproject.tetopology.management.api.KeyId;
import org.onosproject.tetopology.management.api.Network;
import org.onosproject.tetopology.management.api.TeTopologyKey;
import org.onosproject.tetopology.management.api.link.NetworkLink;
import org.onosproject.tetopology.management.api.link.NetworkLinkKey;
import org.onosproject.tetopology.management.api.node.NetworkNode;
import org.onosproject.tetopology.management.api.node.NetworkNodeKey;

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

/**
 * Network representation in store.
 */
public class InternalNetwork {
    private TeTopologyKey teTopologyKey;
    private List<KeyId> supportingNetworkIds;
    private boolean serverProvided;
    private List<NetworkNodeKey> nodeIds;
    private List<NetworkLinkKey> linkIds;
    private boolean childUpdate = false;

    /**
     * Creates an instance of InternalNetwork.
     *
     * @param network the Network object
     */
    public InternalNetwork(Network network) {
        this.supportingNetworkIds = network
                .supportingNetworkIds() == null ? null
                                                : Lists.newArrayList(network
                                                        .supportingNetworkIds());
        this.serverProvided = network.isServerProvided();
        // NetworkNodeKey
        if (MapUtils.isNotEmpty(network.nodes())) {
            this.nodeIds = Lists.newArrayList();
            for (Map.Entry<KeyId, NetworkNode> entry : network.nodes().entrySet()) {
                this.nodeIds.add(new NetworkNodeKey(network.networkId(), entry.getKey()));
            }
        }
        // NetworkLinkKey
        if (MapUtils.isNotEmpty(network.links())) {
            this.linkIds = Lists.newArrayList();
            for (Map.Entry<KeyId, NetworkLink> entry : network.links().entrySet()) {
                this.linkIds.add(new NetworkLinkKey(network.networkId(), entry.getKey()));
            }
        }
    }

    /**
     * Creates a default instance of InternalNetwork.
     */
    public InternalNetwork() {
    }

    /**
     * Returns the supporting network Ids.
     *
     * @return the supportingNetworkIds
     */
    public List<KeyId> supportingNetworkIds() {
        if (supportingNetworkIds == null) {
            return null;
        }
        return ImmutableList.copyOf(supportingNetworkIds);
    }

    /**
     * Returns if the network topology is provided by a server or is
     * configured by a client.
     *
     * @return true if the network is provided by a server; false otherwise
     */
    public boolean serverProvided() {
        return serverProvided;
    }

    /**
     * @param serverProvided the serverProvided to set
     */
    public void setServerProvided(boolean serverProvided) {
        this.serverProvided = serverProvided;
    }

    /**
     * Returns the list of node Ids in the network.
     *
     * @return the nodeIds
     */
    public List<NetworkNodeKey> nodeIds() {
        return nodeIds;
    }

    /**
     * Returns the TE topology key for the network.
     *
     * @return the teTopologyKey
     */
    public TeTopologyKey teTopologyKey() {
        return teTopologyKey;
    }

    /**
     * Sets the TE topology key for the network.
     *
     * @param teTopologyKey the teTopologyKey to set
     */
    public void setTeTopologyKey(TeTopologyKey teTopologyKey) {
        this.teTopologyKey = teTopologyKey;
    }

    /**
     * Set the list of node Ids in the network.
     *
     * @param nodeIds the nodeIds to set
     */
    public void setNodeIds(List<NetworkNodeKey> nodeIds) {
        this.nodeIds = nodeIds;
    }

    /**
     * Returns the list of link Ids in the network.
     *
     * @return the linkIds
     */
    public List<NetworkLinkKey> linkIds() {
        return linkIds;
    }

    /**
     * Set the list of link Ids in the network.
     *
     * @param linkIds the linkIds to set
     */
    public void setLinkIds(List<NetworkLinkKey> linkIds) {
        this.linkIds = linkIds;
    }

    /**
     * Returns the flag if the data was updated by child change.
     *
     * @return value of childUpdate
     */
    public boolean childUpdate() {
        return childUpdate;
    }

    /**
     * Sets the flag if the data was updated by child change.
     *
     * @param childUpdate the childUpdate value to set
     */
    public void setChildUpdate(boolean childUpdate) {
        this.childUpdate = childUpdate;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(teTopologyKey, nodeIds, linkIds,
                supportingNetworkIds, serverProvided);
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (object instanceof InternalNetwork) {
            InternalNetwork that = (InternalNetwork) object;
            return Objects.equal(this.teTopologyKey, that.teTopologyKey) &&
                    Objects.equal(this.nodeIds, that.nodeIds) &&
                    Objects.equal(this.linkIds, that.linkIds) &&
                    Objects.equal(this.supportingNetworkIds, that.supportingNetworkIds) &&
                    Objects.equal(this.serverProvided, that.serverProvided);
        }
        return false;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("teTopologyKey", teTopologyKey)
                .add("nodeIds", nodeIds)
                .add("linkIds", linkIds)
                .add("supportingNetworkIds", supportingNetworkIds)
                .add("serverProvided", serverProvided)
                .add("childUpdate", childUpdate)
                .toString();
    }

}

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
import org.onlab.packet.Ip4Address;
import org.onosproject.tetopology.management.api.DefaultNetwork;
import org.onosproject.tetopology.management.api.KeyId;
import org.onosproject.tetopology.management.api.Network;
import org.onosproject.tetopology.management.api.TeTopology;
import org.onosproject.tetopology.management.api.TeTopologyId;
import org.onosproject.tetopology.management.api.TeTopologyKey;
import org.onosproject.tetopology.management.api.link.DefaultNetworkLink;
import org.onosproject.tetopology.management.api.link.NetworkLink;
import org.onosproject.tetopology.management.api.link.NetworkLinkKey;
import org.onosproject.tetopology.management.api.link.TeLink;
import org.onosproject.tetopology.management.api.link.TeLinkTpGlobalKey;
import org.onosproject.tetopology.management.api.link.TeLinkTpKey;
import org.onosproject.tetopology.management.api.node.DefaultNetworkNode;
import org.onosproject.tetopology.management.api.node.DefaultTerminationPoint;
import org.onosproject.tetopology.management.api.node.NetworkNode;
import org.onosproject.tetopology.management.api.node.NetworkNodeKey;
import org.onosproject.tetopology.management.api.node.NodeTpKey;
import org.onosproject.tetopology.management.api.node.TeNode;
import org.onosproject.tetopology.management.api.node.TeNodeKey;
import org.onosproject.tetopology.management.api.node.TerminationPoint;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

/**
 * TE Topology Manager utility functions.
 */
public final class TeMgrUtil {
    private static final String TENODE_ID = "teNodeId/";
    private static final String TELINK_ID = "/teLinkId/";
    private static final String PROVIDER_ID = "providerId/";
    private static final String CLIENT_ID = "/clientId/";
    private static final String TOPOLOGY_ID = "/topologyId/";

    // no instantiation
    private TeMgrUtil() {
    }

    /**
     * Returns the network link id for a TE link local key.
     *
     * @param  key TE link local key
     * @return value of network link id
     */
    public static KeyId toNetworkLinkId(TeLinkTpKey key) {
        return KeyId.keyId(new StringBuilder()
                .append(TENODE_ID)
                .append(Ip4Address.valueOf((int) key.teNodeId()).toString())
                .append(TELINK_ID)
                .append(key.teLinkTpId()).toString());
    }

    /**
     * Returns the network id for a TE topology id.
     *
     * @param  teTopologyId TE topology id
     * @return value of network id
     */
    public static KeyId toNetworkId(TeTopologyId teTopologyId) {
        return KeyId.keyId(new StringBuilder()
                .append(PROVIDER_ID)
                .append(teTopologyId.providerId())
                .append(CLIENT_ID)
                .append(teTopologyId.clientId())
                .append(TOPOLOGY_ID)
                .append(teTopologyId.topologyId()).toString());
    }

    /**
     * Returns the network id for a TE topology key.
     *
     * @param  teTopologyKey TE topology key
     * @return value of network id
     */
    public static KeyId toNetworkId(TeTopologyKey teTopologyKey) {
        return KeyId.keyId(new StringBuilder()
                .append(PROVIDER_ID)
                .append(teTopologyKey.providerId())
                .append(CLIENT_ID)
                .append(teTopologyKey.clientId())
                .append(TOPOLOGY_ID)
                .append(teTopologyKey.topologyId()).toString());
    }

    /**
     * Returns the network node key for a TE node global key.
     *
     * @param  teNodeKey TE node global key
     * @return value of network node key
     */
    public static NetworkNodeKey networkNodeKey(TeNodeKey teNodeKey) {
        return new NetworkNodeKey(toNetworkId(teNodeKey.teTopologyKey()),
                                  KeyId.keyId(Ip4Address
                                          .valueOf((int) teNodeKey.teNodeId())
                                          .toString()));
    }

    /**
     * Returns the network link key for a TE link global key.
     *
     * @param  teLinkKey TE link global key
     * @return value of network link key
     */
    public static NetworkLinkKey networkLinkKey(TeLinkTpGlobalKey teLinkKey) {
        return new NetworkLinkKey(toNetworkId(teLinkKey.teTopologyKey()),
                                  toNetworkLinkId(teLinkKey.teLinkTpKey()));
    }

    /**
     * Returns the TE topology id for a TE topology.
     *
     * @param  teTopology an instance of TE topology
     * @return value of TE topology id
     */
    public static TeTopologyId teTopologyId(TeTopology teTopology) {
        return new TeTopologyId(teTopology.teTopologyId().providerId(),
                                teTopology.teTopologyId().clientId(),
                                teTopology.teTopologyIdStringValue());
    }

    /**
     * Returns a default instance of termination point for a TE termination point id.
     *
     * @param  teTpId TE termination point id
     * @return an instance of termination point
     */
    private static TerminationPoint tpBuilder(long teTpId) {
        return new DefaultTerminationPoint(KeyId.keyId(Long.toString(teTpId)), null, teTpId);
    }

    /**
     * Returns an instance of network node for a TE node.
     *
     * @param id     value of the network node id
     * @param teNode value of TE node
     * @return an instance of network node
     */
    public static NetworkNode nodeBuilder(KeyId id, TeNode teNode) {
        List<NetworkNodeKey> supportingNodeIds = null;
        if (teNode.supportingTeNodeId() != null) {
            supportingNodeIds = Lists.newArrayList(networkNodeKey(teNode.supportingTeNodeId()));
        }
        Map<KeyId, TerminationPoint> tps = Maps.newConcurrentMap();
        for (Long teTpid : teNode.teTerminationPointIds()) {
            tps.put(KeyId.keyId(Long.toString(teTpid)), tpBuilder(teTpid));
        }
        return new DefaultNetworkNode(id, supportingNodeIds, teNode, tps);
    }

    /**
     * Returns the network node termination point key for a TE link end point key.
     *
     * @param  teLinkKey TE link end point key
     * @return value of network node termination point key
     */
    public static NodeTpKey nodeTpKey(TeLinkTpKey teLinkKey) {
        return new NodeTpKey(KeyId.keyId(Ip4Address
                .valueOf((int) teLinkKey.teNodeId()).toString()),
                             KeyId.keyId(Long.toString(teLinkKey.teLinkTpId())));
    }

    /**
     * Returns an instance of network link for a TE link.
     *
     * @param id     value of the network link id
     * @param teLink value of TE link
     * @return an instance of network link
     */
    public static NetworkLink linkBuilder(KeyId id, TeLink teLink) {
        NodeTpKey source = nodeTpKey(teLink.teLinkKey());
        NodeTpKey destination = null;
        if (teLink.peerTeLinkKey() != null) {
            destination = nodeTpKey(teLink.peerTeLinkKey());
        }
        List<NetworkLinkKey> supportingLinkIds = null;
        if (teLink.supportingTeLinkId() != null) {
            supportingLinkIds = Lists.newArrayList(networkLinkKey(teLink.supportingTeLinkId()));
        }
        return new DefaultNetworkLink(id, source, destination, supportingLinkIds, teLink);
    }

    /**
     * Returns an instance of network for a TE topology.
     *
     * @param  teTopology value of TE topology
     * @return an instance of network
     */
    public static Network networkBuilder(TeTopology teTopology) {
        KeyId networkId = TeMgrUtil.toNetworkId(teTopology.teTopologyId());
        TeTopologyId topologyId = teTopologyId(teTopology);
        Map<KeyId, NetworkNode> nodes = null;
        if (MapUtils.isNotEmpty(teTopology.teNodes())) {
            nodes = Maps.newHashMap();
            for (TeNode tenode : teTopology.teNodes().values()) {
                KeyId key = KeyId.keyId(Ip4Address
                        .valueOf((int) tenode.teNodeId()).toString());
                nodes.put(key, nodeBuilder(key, tenode));
            }
        }
        Map<KeyId, NetworkLink> links = null;
        if (MapUtils.isNotEmpty(teTopology.teLinks())) {
            links = Maps.newHashMap();
            for (TeLink telink : teTopology.teLinks().values()) {
                KeyId key = toNetworkLinkId(telink.teLinkKey());
                links.put(key, linkBuilder(key, telink));

            }
        }
        return new DefaultNetwork(networkId, null, nodes, links,
                                  topologyId, false, teTopology.ownerId());
    }

}

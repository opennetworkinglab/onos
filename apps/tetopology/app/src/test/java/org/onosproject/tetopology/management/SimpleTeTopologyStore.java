/**
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
package org.onosproject.tetopology.management;

import static org.onosproject.tetopology.management.api.TeTopologyEvent.Type.NETWORK_ADDED;
import static org.onosproject.tetopology.management.api.TeTopologyEvent.Type.NETWORK_REMOVED;
import static org.onosproject.tetopology.management.api.TeTopologyEvent.Type.TE_TOPOLOGY_ADDED;
import static org.onosproject.tetopology.management.api.TeTopologyEvent.Type.TE_TOPOLOGY_REMOVED;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.BitSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Service;
import org.onosproject.net.DeviceId;
import org.onosproject.store.AbstractStore;
import org.onosproject.tetopology.management.api.CommonTopologyData;
import org.onosproject.tetopology.management.api.DefaultNetwork;
import org.onosproject.tetopology.management.api.DefaultTeTopologies;
import org.onosproject.tetopology.management.api.DefaultTeTopology;
import org.onosproject.tetopology.management.api.KeyId;
import org.onosproject.tetopology.management.api.Network;
import org.onosproject.tetopology.management.api.OptimizationType;
import org.onosproject.tetopology.management.api.TeConstants;
import org.onosproject.tetopology.management.api.TeTopologies;
import org.onosproject.tetopology.management.api.TeTopology;
import org.onosproject.tetopology.management.api.TeTopologyEvent;
import org.onosproject.tetopology.management.api.TeTopologyId;
import org.onosproject.tetopology.management.api.TeTopologyKey;
import org.onosproject.tetopology.management.api.TeUtils;
import org.onosproject.tetopology.management.api.link.DefaultNetworkLink;
import org.onosproject.tetopology.management.api.link.DefaultTeLink;
import org.onosproject.tetopology.management.api.link.NetworkLink;
import org.onosproject.tetopology.management.api.link.NetworkLinkKey;
import org.onosproject.tetopology.management.api.link.TeLink;
import org.onosproject.tetopology.management.api.link.TeLinkTpGlobalKey;
import org.onosproject.tetopology.management.api.link.TeLinkTpKey;
import org.onosproject.tetopology.management.api.node.ConnectivityMatrix;
import org.onosproject.tetopology.management.api.node.ConnectivityMatrixKey;
import org.onosproject.tetopology.management.api.node.DefaultNetworkNode;
import org.onosproject.tetopology.management.api.node.DefaultTeNode;
import org.onosproject.tetopology.management.api.node.DefaultTerminationPoint;
import org.onosproject.tetopology.management.api.node.NetworkNode;
import org.onosproject.tetopology.management.api.node.NetworkNodeKey;
import org.onosproject.tetopology.management.api.node.NodeTpKey;
import org.onosproject.tetopology.management.api.node.TeNode;
import org.onosproject.tetopology.management.api.node.TeNodeKey;
import org.onosproject.tetopology.management.api.node.TerminationPoint;
import org.onosproject.tetopology.management.api.node.TerminationPointKey;
import org.onosproject.tetopology.management.api.node.TtpKey;
import org.onosproject.tetopology.management.api.node.TunnelTerminationPoint;
import org.onosproject.tetopology.management.impl.InternalNetwork;
import org.onosproject.tetopology.management.impl.InternalNetworkLink;
import org.onosproject.tetopology.management.impl.InternalNetworkNode;
import org.onosproject.tetopology.management.impl.InternalTeLink;
import org.onosproject.tetopology.management.impl.InternalTeNode;
import org.onosproject.tetopology.management.impl.InternalTeTopology;
import org.onosproject.tetopology.management.impl.InternalTerminationPoint;
import org.onosproject.tetopology.management.impl.TeMgrUtil;
import org.onosproject.tetopology.management.impl.TeTopologyMapEvent;
import org.onosproject.tetopology.management.impl.TeTopologyStore;
import org.onosproject.tetopology.management.impl.TeTopologyStoreDelegate;
import org.slf4j.Logger;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

/**
 * Implementation of the TE network store.
 */
@Component(immediate = true)
@Service
public class SimpleTeTopologyStore
        extends AbstractStore<TeTopologyEvent, TeTopologyStoreDelegate>
        implements TeTopologyStore {
    private static final String STORE_NAME = "TE_NETWORK_TOPOLOGY_STORE";
    private final Logger log = getLogger(getClass());

    // Track TE topologies by TE Topology key
    private Map<TeTopologyKey, InternalTeTopology> teTopologyMap = Maps
            .newConcurrentMap();
    // Track networks by network Id
    private Map<KeyId, InternalNetwork> networkMap = Maps.newConcurrentMap();
    // Track TE nodes by TE node key
    private Map<TeNodeKey, InternalTeNode> teNodeMap = Maps.newConcurrentMap();
    // Track ConnectivityMatrix by its key
    private Map<ConnectivityMatrixKey, ConnectivityMatrix> connMatrixMap = Maps
            .newConcurrentMap();
    // Track Tunnel Termination Points by its key
    private Map<TtpKey, TunnelTerminationPoint> ttpMap = Maps
            .newConcurrentMap();
    // Track network nodes by network node key
    private Map<NetworkNodeKey, InternalNetworkNode> networkNodeMap = Maps
            .newConcurrentMap();
    // Track TE links by its key
    private Map<TeLinkTpGlobalKey, InternalTeLink> teLinkMap = Maps
            .newConcurrentMap();
    // Track network links by network link key
    private Map<NetworkLinkKey, InternalNetworkLink> networkLinkMap = Maps
            .newConcurrentMap();
    // Track Termination points by termination point key
    private Map<TerminationPointKey, InternalTerminationPoint> tpMap = Maps
            .newConcurrentMap();
    // Track termination point keys by TE termination point Key
    private Map<TeLinkTpGlobalKey, TerminationPointKey> tpKeyMap = Maps
            .newConcurrentMap();
    private long providerId;

    @Activate
    public void activate() {
        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        teTopologyMap.clear();
        networkMap.clear();
        teNodeMap.clear();
        connMatrixMap.clear();
        networkNodeMap.clear();
        teLinkMap.clear();
        networkLinkMap.clear();
        tpMap.clear();
        tpKeyMap.clear();
        ttpMap.clear();
        log.info("Stopped");
    }

    @Override
    public TeTopologies teTopologies() {
        Map<TeTopologyKey, TeTopology> teTopologies = Maps.newHashMap();
        if (MapUtils.isNotEmpty(teTopologyMap)) {
            for (TeTopologyKey key : teTopologyMap.keySet()) {
                teTopologies.put(key, teTopology(key));
            }
        }
        return new DefaultTeTopologies(STORE_NAME, teTopologies);
    }

    private TeTopology teTopology(TeTopologyKey topologyId,
                                  InternalTeTopology intTopology) {
        if (intTopology == null) {
            return null;
        }
        Map<Long, TeNode> teNodes = null;
        if (CollectionUtils.isNotEmpty(intTopology.teNodeKeys())) {
            teNodes = Maps.newHashMap();
            for (TeNodeKey key : intTopology.teNodeKeys()) {
                teNodes.put(key.teNodeId(), teNode(key));
            }
        }
        Map<TeLinkTpKey, TeLink> teLinks = null;
        if (CollectionUtils.isNotEmpty(intTopology.teLinkKeys())) {
            teLinks = Maps.newHashMap();
            for (TeLinkTpGlobalKey key : intTopology.teLinkKeys()) {
                teLinks.put(key.teLinkTpKey(), teLink(key));
            }
        }
        return new DefaultTeTopology(topologyId, teNodes, teLinks,
                                     intTopology.teTopologyId(),
                                     intTopology.topologyData());
    }

    @Override
    public TeTopology teTopology(TeTopologyKey topologyId) {
        InternalTeTopology intTopology = teTopologyMap.get(topologyId);
        return teTopology(topologyId, intTopology);
    }

    private void removeTopologyeMapEntrys(InternalTeTopology curTopology) {
        // Remove TE nodes
        if (CollectionUtils.isNotEmpty(curTopology.teNodeKeys())) {
            for (TeNodeKey key : curTopology.teNodeKeys()) {
                removeTeNode(key, true);
            }
        }
        // Remove TE Links
        if (CollectionUtils.isNotEmpty(curTopology.teLinkKeys())) {
            for (TeLinkTpGlobalKey key : curTopology.teLinkKeys()) {
                removeTeLink(key, true);
            }
        }
    }

    @Override
    public void updateTeTopology(TeTopology teTopology) {
        InternalTeTopology curTopology = teTopologyMap
                .get(teTopology.teTopologyId());
        if (curTopology != null) {
            // Existing topology update
            // Remove existing map entries first, which should be removed by
            // its own events
            removeTopologyeMapEntrys(curTopology);
        }
        // Update TE nodes
        List<NetworkNodeKey> nodeIds = null;
        if (MapUtils.isNotEmpty(teTopology.teNodes())) {
            nodeIds = Lists.newArrayList();
            for (Map.Entry<Long, TeNode> entry : teTopology.teNodes()
                    .entrySet()) {
                TeNodeKey teNodeKey = new TeNodeKey(teTopology.teTopologyId(),
                                                    entry.getKey());
                NetworkNodeKey nodeKey = TeMgrUtil.networkNodeKey(teNodeKey);
                updateTeNode(teNodeKey, entry.getValue(), true, true, nodeKey);
                nodeIds.add(nodeKey);
            }
        }
        // Update TE links
        List<NetworkLinkKey> linkIds = null;
        if (MapUtils.isNotEmpty(teTopology.teLinks())) {
            linkIds = Lists.newArrayList();
            for (Map.Entry<TeLinkTpKey, TeLink> entry : teTopology.teLinks()
                    .entrySet()) {
                TeLinkTpGlobalKey teLinkKey = new TeLinkTpGlobalKey(teTopology
                        .teTopologyId(), entry.getKey());
                NetworkLinkKey linkKey = TeMgrUtil.networkLinkKey(teLinkKey);
                updateTeLink(teLinkKey, entry.getValue(), true, true, linkKey);
                linkIds.add(linkKey);
            }
        }
        // Finally Update teTopologyMap
        InternalTeTopology newTopology = new InternalTeTopology(teTopology);
        teTopologyMap.put(teTopology.teTopologyId(), newTopology);

        if (curTopology == null) {
            // New topology, update networkMap
            InternalNetwork intNetwork = new InternalNetwork();
            intNetwork.setServerProvided(false);
            intNetwork.setTeTopologyKey(teTopology.teTopologyId());
            intNetwork.setNodeIds(nodeIds);
            intNetwork.setLinkIds(linkIds);
            networkMap.put(teTopology.networkId(), intNetwork);
        }
    }

    @Override
    public void removeTeTopology(TeTopologyKey topologyId) {
        // Remove it from teTopologyMap
        InternalTeTopology topology = teTopologyMap.remove(topologyId);
        if (topology != null) {
            removeTopologyeMapEntrys(topology);
            // Remove it from networkMap;
            networkMap.remove(topology.topologyData().networkId());
        }
    }

    @Override
    public List<Network> networks() {
        if (MapUtils.isEmpty(networkMap)) {
            return null;
        }
        List<Network> networks = Lists.newArrayList();
        for (KeyId networkId : networkMap.keySet()) {
            networks.add(network(networkId));
        }
        return networks;
    }

    private Network network(KeyId networkId, InternalNetwork curNetwork) {
        if (curNetwork == null) {
            return null;
        }
        List<KeyId> supportingNetworkIds = curNetwork.supportingNetworkIds();
        Map<KeyId, NetworkNode> nodes = null;
        if (CollectionUtils.isNotEmpty(curNetwork.nodeIds())) {
            nodes = Maps.newHashMap();
            for (NetworkNodeKey key : curNetwork.nodeIds()) {
                nodes.put(key.nodeId(), networkNode(key));
            }
        }
        Map<KeyId, NetworkLink> links = null;
        if (CollectionUtils.isNotEmpty(curNetwork.linkIds())) {
            links = Maps.newHashMap();
            for (NetworkLinkKey key : curNetwork.linkIds()) {
                links.put(key.linkId(), networkLink(key));
            }
        }
        TeTopologyId topologyId = null;
        DeviceId ownerId = null;
        if (curNetwork.teTopologyKey() != null
                && teTopology(curNetwork.teTopologyKey()) != null) {
            topologyId = new TeTopologyId(curNetwork.teTopologyKey()
                    .providerId(), curNetwork.teTopologyKey().clientId(),
                                          teTopology(curNetwork.teTopologyKey())
                                                  .teTopologyIdStringValue());
            ownerId = teTopology(curNetwork.teTopologyKey()).ownerId();

        }
        return new DefaultNetwork(networkId, supportingNetworkIds, nodes, links,
                                  topologyId, curNetwork.serverProvided(),
                                  ownerId);
    }

    @Override
    public Network network(KeyId networkId) {
        InternalNetwork curNetwork = networkMap.get(networkId);
        return network(networkId, curNetwork);
    }

    private void removeNetworkMapEntrys(InternalNetwork curNetwork,
                                        boolean teRemove) {
        // Remove TE nodes
        if (CollectionUtils.isNotEmpty(curNetwork.nodeIds())) {
            for (NetworkNodeKey key : curNetwork.nodeIds()) {
                removeNetworkNode(key, teRemove);
            }
        }
        // Remove TE Links
        if (CollectionUtils.isNotEmpty(curNetwork.linkIds())) {
            for (NetworkLinkKey key : curNetwork.linkIds()) {
                removeNetworkLink(key, teRemove);
            }
        }
    }

    private TeTopologyKey newTeTopologyKey(TeTopologyId teTopologyId) {
        long idValue;
        try {
            idValue = Long.parseLong(teTopologyId.topologyId());
        } catch (NumberFormatException e) {
            // Can't get the long value from the string.
            // Use an assigned id value from local id pool,
            // Ideally id should be assigned per provider base.
            idValue = nextTeTopologyId();
        }
        return new TeTopologyKey(teTopologyId.providerId(),
                                 teTopologyId.clientId(), idValue);
    }

    @Override
    public void updateNetwork(Network network) {
        InternalNetwork curNetwork = networkMap.get(network.networkId());
        if (curNetwork != null) {
            // Existing topology update
            // Remove existing map entries first,
            removeNetworkMapEntrys(curNetwork, false);
        }
        TeTopologyKey topoKey = null;
        if (network.teTopologyId() != null) {
            topoKey = newTeTopologyKey(network.teTopologyId());
        }
        // Update TE nodes
        List<TeNodeKey> teNodeKeys = null;
        if (MapUtils.isNotEmpty(network.nodes())) {
            teNodeKeys = Lists.newArrayList();
            for (Map.Entry<KeyId, NetworkNode> entry : network.nodes()
                    .entrySet()) {
                NetworkNodeKey nodeKey = new NetworkNodeKey(network.networkId(),
                                                            entry.getKey());
                TeNodeKey teNodeKey = null;
                if (topoKey != null && entry.getValue().teNode() != null) {
                    teNodeKey = new TeNodeKey(topoKey, entry.getValue().teNode()
                            .teNodeId());
                }
                updateNetworkNode(nodeKey, entry.getValue(), true, false,
                                  teNodeKey);
                teNodeKeys.add(teNodeKey);
            }
        }
        // Update TE links
        List<TeLinkTpGlobalKey> teLinkKeys = null;
        if (MapUtils.isNotEmpty(network.links())) {
            teLinkKeys = Lists.newArrayList();
            for (Map.Entry<KeyId, NetworkLink> entry : network.links()
                    .entrySet()) {
                NetworkLinkKey linkKey = new NetworkLinkKey(network.networkId(),
                                                            entry.getKey());
                TeLinkTpGlobalKey teLinkKey = null;
                if (topoKey != null && entry.getValue().teLink() != null) {
                    teLinkKey = new TeLinkTpGlobalKey(topoKey, entry.getValue()
                            .teLink().teLinkKey());
                }
                updateNetworkLink(linkKey, entry.getValue(), true, false,
                                  teLinkKey);
                teLinkKeys.add(teLinkKey);
            }
        }

        // New network, update TE Topology first
        if (curNetwork == null) {
            InternalTeTopology intTopo = new InternalTeTopology(network
                    .teTopologyId().topologyId());
            intTopo.setTeNodeKeys(teNodeKeys);
            intTopo.setTeLinkKeys(teLinkKeys);
            BitSet flags = new BitSet(TeConstants.FLAG_MAX_BITS);
            flags.set(TeTopology.BIT_LEARNT);
            if (network.teTopologyId().clientId() == providerId) {
                // Hard rule for now
                flags.set(TeTopology.BIT_CUSTOMIZED);
            }
            CommonTopologyData common = new CommonTopologyData(network
                    .networkId(), OptimizationType.NOT_OPTIMIZED, flags, network
                            .ownerId());
            intTopo.setTopologydata(common);
            teTopologyMap.put(topoKey, intTopo);
            // Assume new topology
            TeTopologyEvent topologyEvent = new TeTopologyEvent(TE_TOPOLOGY_ADDED,
                                                                teTopology(topoKey));
            notifyDelegate(topologyEvent);
        }
        // Finally Update networkMap
        InternalNetwork newNetwork = new InternalNetwork(network);
        newNetwork.setTeTopologyKey(topoKey);
        networkMap.put(network.networkId(), newNetwork);
        // Assume new network
        TeTopologyEvent topologyEvent = new TeTopologyEvent(NETWORK_ADDED,
                                                            network(network
                                                                    .networkId()));
        notifyDelegate(topologyEvent);
    }

    @Override
    public void removeNetwork(KeyId networkId) {
        // Remove it from networkMap
        InternalNetwork network = networkMap.remove(networkId);
        TeTopologyEvent topologyEvent = new TeTopologyEvent(NETWORK_REMOVED,
                                                            new DefaultNetwork(networkId,
                                                                               null,
                                                                               null,
                                                                               null,
                                                                               null,
                                                                               false,
                                                                               null));
        notifyDelegate(topologyEvent);
        if (network != null && network.teTopologyKey() != null) {
            removeNetworkMapEntrys(network, false);
            teTopologyMap.remove(network.teTopologyKey());
            topologyEvent = new TeTopologyEvent(TE_TOPOLOGY_REMOVED,
                                                new DefaultTeTopology(network
                                                        .teTopologyKey(), null, null, null, null));
            notifyDelegate(topologyEvent);
        }
    }

    private TeNode teNode(TeNodeKey nodeKey, InternalTeNode intNode) {
        if (intNode == null) {
            return null;
        }
        Map<Long, ConnectivityMatrix> connMatrices = null;
        if (CollectionUtils.isNotEmpty(intNode.connMatrixKeys())) {
            connMatrices = Maps.newHashMap();
            for (ConnectivityMatrixKey key : intNode.connMatrixKeys()) {
                connMatrices.put(key.entryId(), connMatrixMap.get(key));
            }
        }
        List<Long> teLinkIds = null;
        if (CollectionUtils.isNotEmpty(intNode.teLinkTpKeys())) {
            teLinkIds = Lists.newArrayList();
            for (TeLinkTpGlobalKey key : intNode.teLinkTpKeys()) {
                teLinkIds = TeUtils.addListElement(teLinkIds, key.teLinkTpId());
            }
        }
        List<Long> tps = null;
        if (CollectionUtils.isNotEmpty(intNode.teTpKeys())) {
            tps = Lists.newArrayList();
            for (TeLinkTpGlobalKey key : intNode.teTpKeys()) {
                tps = TeUtils.addListElement(tps, key.teLinkTpId());
            }
        }
        Map<Long, TunnelTerminationPoint> ttps = null;
        if (CollectionUtils.isNotEmpty(intNode.ttpKeys())) {
            ttps = Maps.newHashMap();
            for (TtpKey key : intNode.ttpKeys()) {
                ttps.put(key.ttpId(), ttpMap.get(key));
            }
        }
        return new DefaultTeNode(nodeKey.teNodeId(),
                                 intNode.underlayTopologyKey(),
                                 intNode.supportNodeKey(),
                                 intNode.sourceTeNodeKey(), intNode.teData(),
                                 connMatrices, teLinkIds, ttps, tps);
    }

    @Override
    public TeNode teNode(TeNodeKey nodeKey) {
        InternalTeNode intNode = teNodeMap.get(nodeKey);
        return teNode(nodeKey, intNode);
    }

    private void removeTeNodeMapEntrys(InternalTeNode intNode) {
        // Remove connMatrixMap entries for the node
        if (CollectionUtils.isNotEmpty(intNode.connMatrixKeys())) {
            for (ConnectivityMatrixKey key : intNode.connMatrixKeys()) {
                connMatrixMap.remove(key);
            }
        }
        // Remove ttpMap entries for the node
        if (CollectionUtils.isNotEmpty(intNode.ttpKeys())) {
            for (TtpKey key : intNode.ttpKeys()) {
                ttpMap.remove(key);
            }
        }
    }

    private void updateTeNode(TeNodeKey nodeKey, TeNode node,
                              boolean parentUpdate, boolean teNodeUpdate,
                              NetworkNodeKey networkNodeKey) {
        InternalTeTopology intTopo = teTopologyMap.get(nodeKey.teTopologyKey());
        if (intTopo == null && !parentUpdate) {
            log.error("TE Topology is not in dataStore for nodeUpdate {}",
                      nodeKey);
            return;
        }
        InternalTeNode curNode = teNodeMap.get(nodeKey);
        // Update connMatrixMap
        if (MapUtils.isNotEmpty(node.connectivityMatrices())) {
            for (Map.Entry<Long, ConnectivityMatrix> entry : node
                    .connectivityMatrices().entrySet()) {
                connMatrixMap
                        .put(new ConnectivityMatrixKey(nodeKey, entry.getKey()),
                             entry.getValue());
            }
        }
        // Update ttpMap
        if (MapUtils.isNotEmpty(node.tunnelTerminationPoints())) {
            for (Map.Entry<Long, TunnelTerminationPoint> entry : node
                    .tunnelTerminationPoints().entrySet()) {
                ttpMap.put(new TtpKey(nodeKey, entry.getKey()),
                           entry.getValue());
            }
        }
        // Update TE Termination Points

        // Update teNodeMap
        InternalTeNode intNode = new InternalTeNode(nodeKey, node,
                                                    networkNodeKey,
                                                    parentUpdate);
        teNodeMap.put(nodeKey, intNode);
        if (curNode == null && !parentUpdate && intTopo != null) {
            // Update InternalTeTopology
            intTopo.setChildUpdate(true);
            TeUtils.addListElement(intTopo.teNodeKeys(), nodeKey);
        }
        // Update networkNodeMap
        if (teNodeUpdate) {
            updateNetworkNode(networkNodeKey, networkNode(node), parentUpdate,
                              teNodeUpdate, nodeKey);
        }
    }

    private NetworkNode networkNode(TeNode node) {
        KeyId nodeId = KeyId.keyId(Long.toString(node.teNodeId()));
        List<NetworkNodeKey> supportingNodeIds = null;
        if (node.supportingTeNodeId() != null) {
            supportingNodeIds = Lists.newArrayList();
            supportingNodeIds.add(new NetworkNodeKey(TeMgrUtil.toNetworkId((node
                    .supportingTeNodeId().teTopologyKey())), KeyId.keyId(Long
                            .toString(node.supportingTeNodeId().teNodeId()))));
        }
        Map<KeyId, TerminationPoint> tps = null;
        if (node.teTerminationPointIds() != null) {
            tps = Maps.newHashMap();
            for (Long teTpId : node.teTerminationPointIds()) {
                tps.put(KeyId.keyId(Long.toString(teTpId)),
                        new DefaultTerminationPoint(KeyId
                                .keyId(Long.toString(teTpId)), null, teTpId));
            }
        }
        return new DefaultNetworkNode(nodeId, supportingNodeIds, node, tps);
    }

    @Override
    public void updateTeNode(TeNodeKey nodeKey, TeNode node) {
        updateTeNode(nodeKey, node, false, true,
                     TeMgrUtil.networkNodeKey(nodeKey));
    }

    private void removeTeNode(TeNodeKey nodeKey, boolean teNodeRemove) {
        // Remove it from InternalTeTopology first
        InternalTeTopology intTopo = teTopologyMap.get(nodeKey.teTopologyKey());
        if (intTopo != null
                && CollectionUtils.isNotEmpty(intTopo.teNodeKeys())) {
            intTopo.setChildUpdate(true);
            intTopo.teNodeKeys().remove(nodeKey);
        }
        // Then remove it from teNodeMap
        InternalTeNode node = teNodeMap.remove(nodeKey);
        removeTeNodeMapEntrys(node);
        // Remove it from networkNodeMap
        if (teNodeRemove && node != null) {
            removeNetworkNode(node.networkNodeKey(), teNodeRemove);
        }
    }

    @Override
    public void removeTeNode(TeNodeKey nodeKey) {
        removeTeNode(nodeKey, true);
    }

    private NetworkNode networkNode(NetworkNodeKey nodeKey,
                                    InternalNetworkNode intNode) {
        if (intNode == null) {
            return null;
        }
        Map<KeyId, TerminationPoint> tps = Maps.newHashMap();
        for (KeyId tpId : intNode.tpIds()) {
            tps.put(tpId,
                    terminationPoint(new TerminationPointKey(nodeKey, tpId)));

        }
        return new DefaultNetworkNode(nodeKey.nodeId(),
                                      intNode.supportingNodeIds(),
                                      teNode(intNode.teNodeKey()), tps);
    }

    @Override
    public NetworkNode networkNode(NetworkNodeKey nodeKey) {
        InternalNetworkNode intNode = networkNodeMap.get(nodeKey);
        return networkNode(nodeKey, intNode);
    }

    private void updateNetworkNode(NetworkNodeKey nodeKey, NetworkNode node,
                                   boolean parentUpdate, boolean teNodeUpdate,
                                   TeNodeKey teNodeKey) {
        InternalNetwork intNework = null;
        if (!parentUpdate) {
            intNework = networkMap.get(nodeKey.networkId());
            if (intNework == null) {
                log.error("Network is not in dataStore for nodeUpdate {}",
                          nodeKey);
                return;
            }
        }

        InternalNetworkNode exNode = networkNodeMap.get(nodeKey);
        if (exNode != null && CollectionUtils.isNotEmpty(exNode.tpIds())) {
            // Remove the TerminationPoints first
            for (KeyId tpId : exNode.tpIds()) {
                removeTerminationPoint(new TerminationPointKey(nodeKey, tpId));
            }
        }

        if (MapUtils.isNotEmpty(node.terminationPoints())) {
            // Update with new TerminationPoints
            for (Map.Entry<KeyId, TerminationPoint> entry : node
                    .terminationPoints().entrySet()) {
                updateTerminationPoint(new TerminationPointKey(nodeKey,
                                                               entry.getKey()),
                                       entry.getValue(), parentUpdate,
                                       teNodeKey);
            }
        }

        // Update teNodeMap first
        if (!teNodeUpdate && teNodeKey != null && node.teNode() != null) {
            updateTeNode(teNodeKey, node.teNode(), parentUpdate, teNodeUpdate,
                         nodeKey);
        }
        // Update networkNodeMap
        InternalNetworkNode intNode = new InternalNetworkNode(node,
                                                              parentUpdate);
        intNode.setTeNodeKey(teNodeKey);
        networkNodeMap.put(nodeKey, intNode);
        if (exNode == null && !parentUpdate && intNework != null) {
            // Update the InternalNetwork
            intNework.setChildUpdate(true);
            TeUtils.addListElement(intNework.nodeIds(), nodeKey);
        }
    }

    @Override
    public void updateNetworkNode(NetworkNodeKey nodeKey, NetworkNode node) {
        TeNodeKey teNodeKey = null;
        if (node.teNode() != null) {
            teNodeKey = new TeNodeKey(networkMap.get(nodeKey.networkId())
                    .teTopologyKey(), node.teNode().teNodeId());
        }
        updateNetworkNode(nodeKey, node, false, false, teNodeKey);
    }

    private void removeNetworkNode(NetworkNodeKey nodeKey,
                                   boolean teNodeRemove) {
        // Update the InternalNetwork
        InternalNetwork intNework = networkMap.get(nodeKey.networkId());
        if (intNework != null
                && CollectionUtils.isNotEmpty(intNework.nodeIds())) {
            intNework.setChildUpdate(true);
            intNework.nodeIds().remove(nodeKey.nodeId());
        }
        InternalNetworkNode intNode = networkNodeMap.remove(nodeKey);
        if (intNode != null && CollectionUtils.isNotEmpty(intNode.tpIds())) {
            // Remove the TerminationPoints first
            for (KeyId tpId : intNode.tpIds()) {
                removeTerminationPoint(new TerminationPointKey(nodeKey, tpId));
            }
        }
        if (!teNodeRemove && intNode != null) {
            // Now remove it from teNodeMap
            removeTeNode(intNode.teNodeKey(), teNodeRemove);
        }
    }

    @Override
    public void removeNetworkNode(NetworkNodeKey nodeKey) {
        removeNetworkNode(nodeKey, false);
    }

    private TeLink teLink(TeLinkTpGlobalKey linkKey, InternalTeLink intLink) {
        if (intLink == null) {
            return null;
        }
        return new DefaultTeLink(linkKey.teLinkTpKey(), intLink.peerTeLinkKey(),
                                 intLink.underlayTopologyKey(),
                                 intLink.supportingLinkKey(),
                                 intLink.sourceTeLinkKey(), intLink.teData());
    }

    @Override
    public TeLink teLink(TeLinkTpGlobalKey linkKey) {
        InternalTeLink intLink = teLinkMap.get(linkKey);
        return teLink(linkKey, intLink);
    }

    private void updateTeLink(TeLinkTpGlobalKey linkKey, TeLink link,
                              boolean parentUpdate, boolean teLinkUpdate,
                              NetworkLinkKey networkLinkKey) {
        InternalTeTopology intTopo = teTopologyMap.get(linkKey.teTopologyKey());
        if (intTopo == null && !parentUpdate) {
            log.error("TE Topology is not in dataStore for linkUpdate {}",
                      linkKey);
            return;
        }
        InternalTeNode intNode = teNodeMap.get(linkKey.teNodeKey());
        if (intNode == null && !parentUpdate) {
            log.error("TE node is not in dataStore for linkUpdate {}", linkKey);
            return;
        }
        InternalTeLink exLink = teLinkMap.get(linkKey);

        // Update teLinkMap
        InternalTeLink intLink = new InternalTeLink(link, parentUpdate);
        intLink.setNetworkLinkKey(networkLinkKey);
        teLinkMap.put(linkKey, intLink);
        if (exLink == null && !parentUpdate) {
            if (intTopo != null) {
                // Update the InternalTeTopology
                intTopo.setChildUpdate(true);
                intTopo.setTeLinkKeys(TeUtils
                        .addListElement(intTopo.teLinkKeys(), linkKey));
            }
            if (intNode != null) {
                // Update the InternalNode
                intNode.setChildUpdate(true);
                intNode.setTeLinkTpKeys(TeUtils
                        .addListElement(intNode.teLinkTpKeys(), linkKey));
            }
        }

        // Update networkLinkMap
        if (teLinkUpdate) {
            updateNetworkLink(networkLinkKey, networkLink(link), parentUpdate,
                              teLinkUpdate, linkKey);
        }
    }

    private NetworkLink networkLink(TeLink link) {
        KeyId linkId = TeMgrUtil.toNetworkLinkId(link.teLinkKey());
        NodeTpKey source = null;
        if (link.teLinkKey() != null) {
            source = new NodeTpKey(KeyId
                    .keyId(Long.toString(link.teLinkKey().teNodeId())), KeyId
                            .keyId(Long
                                    .toString(link.teLinkKey().teLinkTpId())));
        }
        NodeTpKey dest = null;
        if (link.peerTeLinkKey() != null) {
            dest = new NodeTpKey(KeyId
                    .keyId(Long.toString(link.peerTeLinkKey().teNodeId())),
                                 KeyId.keyId(Long.toString(link.peerTeLinkKey()
                                         .teLinkTpId())));
        }
        List<NetworkLinkKey> supportingLinkIds = null;
        if (link.supportingTeLinkId() != null) {
            supportingLinkIds = Lists.newArrayList();
            supportingLinkIds.add(new NetworkLinkKey(TeMgrUtil.toNetworkId(link
                    .supportingTeLinkId().teTopologyKey()), TeMgrUtil
                            .toNetworkLinkId(link.supportingTeLinkId()
                                    .teLinkTpKey())));
        }
        return new DefaultNetworkLink(linkId, source, dest, supportingLinkIds,
                                      link);
    }

    @Override
    public void updateTeLink(TeLinkTpGlobalKey linkKey, TeLink link) {
        updateTeLink(linkKey, link, false, true,
                     TeMgrUtil.networkLinkKey(linkKey));
    }

    private void removeTeLink(TeLinkTpGlobalKey linkKey, boolean teLinkRemove) {
        // Remove it from InternalTeTopology first
        InternalTeTopology intTopo = teTopologyMap.get(linkKey.teTopologyKey());
        if (intTopo != null
                && CollectionUtils.isNotEmpty(intTopo.teLinkKeys())) {
            intTopo.setChildUpdate(true);
            intTopo.teLinkKeys().remove(linkKey);
        }
        // Remove it from InternalTeNode
        InternalTeNode intNode = teNodeMap.get(linkKey.teNodeKey());
        if (intNode != null
                && CollectionUtils.isNotEmpty(intNode.teLinkTpKeys())) {
            intNode.setChildUpdate(true);
            intNode.teLinkTpKeys().remove(linkKey);
        }
        // Then remove it from teLinkMap
        InternalTeLink link = teLinkMap.remove(linkKey);
        if (teLinkRemove && link != null) {
            // Remove it from networkLinkMap
            removeNetworkLink(link.networkLinkKey(), teLinkRemove);
        }
    }

    @Override
    public void removeTeLink(TeLinkTpGlobalKey linkKey) {
        removeTeLink(linkKey, true);
    }

    private NetworkLink networkLink(NetworkLinkKey linkKey,
                                    InternalNetworkLink intLink) {
        if (intLink == null) {
            return null;
        }
        return new DefaultNetworkLink(linkKey.linkId(), intLink.source(),
                                      intLink.destination(),
                                      intLink.supportingLinkIds(),
                                      teLink(intLink.teLinkKey()));
    }

    @Override
    public NetworkLink networkLink(NetworkLinkKey linkKey) {
        InternalNetworkLink intLink = networkLinkMap.get(linkKey);
        return networkLink(linkKey, intLink);
    }

    private void updateNetworkLink(NetworkLinkKey linkKey, NetworkLink link,
                                   boolean parentUpdate, boolean teLinkUpdate,
                                   TeLinkTpGlobalKey teLinkKey) {
        InternalNetwork intNework = null;
        if (!parentUpdate) {
            intNework = networkMap.get(linkKey.networkId());
            if (intNework == null) {
                log.error("Network is not in dataStore for linkUpdate {}",
                          linkKey);
                return;
            }
        }

        InternalNetworkLink exLink = networkLinkMap.get(linkKey);

        // Now update teLinkMap first
        if (!teLinkUpdate && teLinkKey != null) {
            updateTeLink(teLinkKey, link.teLink(), parentUpdate, teLinkUpdate,
                         linkKey);
        }
        // Update networkLinkMap
        InternalNetworkLink intLink = new InternalNetworkLink(link,
                                                              parentUpdate);
        intLink.setTeLinkKey(teLinkKey);
        networkLinkMap.put(linkKey, intLink);
        if (exLink == null && !parentUpdate && intNework != null) {
            // Update the InternalNetwork
            intNework.setChildUpdate(true);
            TeUtils.addListElement(intNework.linkIds(), linkKey);
        }
    }

    @Override
    public void updateNetworkLink(NetworkLinkKey linkKey, NetworkLink link) {
        TeLinkTpGlobalKey teLinkKey = null;
        if (link.teLink() != null) {
            teLinkKey = new TeLinkTpGlobalKey(networkMap
                    .get(linkKey.networkId()).teTopologyKey(),
                                              link.teLink().teLinkKey());
        }

        updateNetworkLink(linkKey, link, false, false, teLinkKey);
    }

    private void removeNetworkLink(NetworkLinkKey linkKey,
                                   boolean teLinkRemove) {
        // Update the InternalNetwork
        InternalNetwork intNework = networkMap.get(linkKey.networkId());
        if (intNework != null
                && CollectionUtils.isNotEmpty(intNework.linkIds())) {
            intNework.setChildUpdate(true);
            intNework.linkIds().remove(linkKey.linkId());
        }
        // Remove it from networkLinkMap
        InternalNetworkLink intLink = networkLinkMap.remove(linkKey);
        if (!teLinkRemove && intLink != null && intLink.teLinkKey() != null) {
            // Now remove it from teLinkMap
            removeTeLink(intLink.teLinkKey(), teLinkRemove);
        }
    }

    @Override
    public void removeNetworkLink(NetworkLinkKey linkKey) {
        removeNetworkLink(linkKey, false);
    }

    private TerminationPoint terminationPoint(TerminationPointKey tpKey) {
        InternalTerminationPoint intTp = tpMap.get(tpKey);
        if (intTp == null) {
            return null;
        }
        return new DefaultTerminationPoint(tpKey.tpId(),
                                           intTp.supportingTpIds(),
                                           intTp.teTpKey().teLinkTpId());
    }

    private void updateTerminationPoint(TerminationPointKey tpKey,
                                        TerminationPoint tp,
                                        boolean parentUpdate,
                                        TeNodeKey teNodeKey) {
        TeNodeKey myTeNodeKey;
        InternalNetworkNode intNode = null;
        if (!parentUpdate) {
            intNode = networkNodeMap.get(tpKey.nodeId());
            if (intNode == null) {
                log.error(" node is not in dataStore for tp update {}", tpKey);
                return;
            }
            myTeNodeKey = intNode.teNodeKey();
        } else {
            myTeNodeKey = teNodeKey;
        }
        TeLinkTpGlobalKey teTpKey = new TeLinkTpGlobalKey(myTeNodeKey,
                                                          tp.teTpId());

        boolean newTp = tpMap.get(tpKey) == null;
        InternalTerminationPoint intTp = new InternalTerminationPoint(tp);
        intTp.setTeTpKey(teTpKey);
        tpMap.put(tpKey, intTp);
        if (newTp) {
            // Update tpKeyMap
            tpKeyMap.put(teTpKey, tpKey);
            if (!parentUpdate && intNode != null) {
                // Update InternalNetworkNode
                intNode.setChildUpdate(true);
                intNode.setTpIds(TeUtils.addListElement(intNode.tpIds(),
                                                        tpKey.tpId()));
            }
        }
    }

    @Override
    public void updateTerminationPoint(TerminationPointKey tpKey,
                                       TerminationPoint tp) {
        updateTerminationPoint(tpKey, tp, false, null);
    }

    @Override
    public void removeTerminationPoint(TerminationPointKey tpKey) {
        // Update InternalNetworkNode
        InternalNetworkNode intNode = networkNodeMap.get(tpKey.nodeId());
        if (intNode != null && CollectionUtils.isNotEmpty(intNode.tpIds())) {
            intNode.setChildUpdate(true);
            intNode.tpIds().remove(tpKey.tpId());
        }
        // Remove it from tpMap
        InternalTerminationPoint tp = tpMap.remove(tpKey);
        // Remove it from tpKeyMap
        if (tp != null) {
            tpKeyMap.remove(tp.teTpKey());
        }
    }

    @Override
    public TunnelTerminationPoint tunnelTerminationPoint(TtpKey ttpId) {
        return ttpMap.get(ttpId);
    }

    @Override
    public long nextTeTopologyId() {
        return 0;
    }

    @Override
    public long nextTeNodeId(TeTopologyKey topoKey) {
        return teTopologyMap.get(topoKey).nextTeNodeId();
    }

    @Override
    public void setNextTeNodeId(TeTopologyKey topoKey, long nextNodeId) {
        teTopologyMap.get(topoKey).setNextTeNodeId(nextNodeId);
    }

    @Override
    public KeyId networkId(TeTopologyKey teTopologyKey) {
        return teTopologyMap.get(teTopologyKey).topologyData().networkId();
    }

    @Override
    public NetworkNodeKey nodeKey(TeNodeKey teNodeKey) {
        return teNodeMap.get(teNodeKey).networkNodeKey();
    }

    @Override
    public NetworkLinkKey linkKey(TeLinkTpGlobalKey teLinkKey) {
        return teLinkMap.get(teLinkKey).networkLinkKey();
    }

    @Override
    public TerminationPointKey terminationPointKey(TeLinkTpGlobalKey teTpKey) {
        return tpKeyMap.get(teTpKey);
    }

    @Override
    public void setProviderId(long providerId) {
        this.providerId = providerId;
    }

    @Override
    public BlockingQueue<TeTopologyMapEvent> mapEventQueue() {
        return null;
    }
}


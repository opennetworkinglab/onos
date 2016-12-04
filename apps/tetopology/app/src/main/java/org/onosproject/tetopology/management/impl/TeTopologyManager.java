/*
 * Copyright 2016-present Open Networking Laboratory
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onosproject.tetopology.management.impl;

import static java.util.concurrent.Executors.newFixedThreadPool;
import static org.onlab.util.Tools.groupedThreads;
import static org.onosproject.net.config.NetworkConfigEvent.Type.CONFIG_ADDED;
import static org.onosproject.net.config.NetworkConfigEvent.Type.CONFIG_UPDATED;
import static org.onosproject.net.config.basics.SubjectFactories.APP_SUBJECT_FACTORY;
import static org.onosproject.tetopology.management.api.TeTopologyEvent.Type.LINK_ADDED;
import static org.onosproject.tetopology.management.api.TeTopologyEvent.Type.LINK_REMOVED;
import static org.onosproject.tetopology.management.api.TeTopologyEvent.Type.LINK_UPDATED;
import static org.onosproject.tetopology.management.api.TeTopologyEvent.Type.NETWORK_ADDED;
import static org.onosproject.tetopology.management.api.TeTopologyEvent.Type.NETWORK_REMOVED;
import static org.onosproject.tetopology.management.api.TeTopologyEvent.Type.NODE_ADDED;
import static org.onosproject.tetopology.management.api.TeTopologyEvent.Type.NODE_REMOVED;
import static org.onosproject.tetopology.management.api.TeTopologyEvent.Type.NODE_UPDATED;
import static org.onosproject.tetopology.management.api.TeTopologyEvent.Type.TE_LINK_ADDED;
import static org.onosproject.tetopology.management.api.TeTopologyEvent.Type.TE_LINK_REMOVED;
import static org.onosproject.tetopology.management.api.TeTopologyEvent.Type.TE_LINK_UPDATED;
import static org.onosproject.tetopology.management.api.TeTopologyEvent.Type.TE_NODE_ADDED;
import static org.onosproject.tetopology.management.api.TeTopologyEvent.Type.TE_NODE_REMOVED;
import static org.onosproject.tetopology.management.api.TeTopologyEvent.Type.TE_NODE_UPDATED;
import static org.onosproject.tetopology.management.api.TeTopologyEvent.Type.TE_TOPOLOGY_ADDED;
import static org.onosproject.tetopology.management.api.TeTopologyEvent.Type.TE_TOPOLOGY_REMOVED;

import java.util.BitSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onlab.packet.Ip4Address;
import org.onosproject.app.ApplicationException;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.incubator.net.config.basics.ConfigException;
import org.onosproject.net.DeviceId;
import org.onosproject.net.config.ConfigFactory;
import org.onosproject.net.config.NetworkConfigEvent;
import org.onosproject.net.config.NetworkConfigListener;
import org.onosproject.net.config.NetworkConfigRegistry;
import org.onosproject.net.device.DeviceProviderRegistry;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.link.LinkProviderRegistry;
import org.onosproject.net.link.LinkService;
import org.onosproject.net.provider.AbstractListenerProviderRegistry;
import org.onosproject.net.provider.AbstractProviderService;
import org.onosproject.tetopology.management.api.CommonTopologyData;
import org.onosproject.tetopology.management.api.DefaultNetwork;
import org.onosproject.tetopology.management.api.DefaultNetworks;
import org.onosproject.tetopology.management.api.DefaultTeTopologies;
import org.onosproject.tetopology.management.api.DefaultTeTopology;
import org.onosproject.tetopology.management.api.KeyId;
import org.onosproject.tetopology.management.api.Network;
import org.onosproject.tetopology.management.api.Networks;
import org.onosproject.tetopology.management.api.OptimizationType;
import org.onosproject.tetopology.management.api.TeConstants;
import org.onosproject.tetopology.management.api.TeTopologies;
import org.onosproject.tetopology.management.api.TeTopology;
import org.onosproject.tetopology.management.api.TeTopologyEvent;
import org.onosproject.tetopology.management.api.TeTopologyKey;
import org.onosproject.tetopology.management.api.TeTopologyListener;
import org.onosproject.tetopology.management.api.TeTopologyProvider;
import org.onosproject.tetopology.management.api.TeTopologyProviderRegistry;
import org.onosproject.tetopology.management.api.TeTopologyProviderService;
import org.onosproject.tetopology.management.api.TeTopologyService;
import org.onosproject.tetopology.management.api.link.CommonLinkData;
import org.onosproject.tetopology.management.api.link.DefaultTeLink;
import org.onosproject.tetopology.management.api.link.ExternalLink;
import org.onosproject.tetopology.management.api.link.LinkBandwidth;
import org.onosproject.tetopology.management.api.link.NetworkLink;
import org.onosproject.tetopology.management.api.link.NetworkLinkEventSubject;
import org.onosproject.tetopology.management.api.link.NetworkLinkKey;
import org.onosproject.tetopology.management.api.link.TeLink;
import org.onosproject.tetopology.management.api.link.TeLinkEventSubject;
import org.onosproject.tetopology.management.api.link.TeLinkTpGlobalKey;
import org.onosproject.tetopology.management.api.link.TeLinkTpKey;
import org.onosproject.tetopology.management.api.link.TePathAttributes;
import org.onosproject.tetopology.management.api.link.UnderlayPath;
import org.onosproject.tetopology.management.api.node.CommonNodeData;
import org.onosproject.tetopology.management.api.node.ConnectivityMatrix;
import org.onosproject.tetopology.management.api.node.DefaultTeNode;
import org.onosproject.tetopology.management.api.node.NetworkNode;
import org.onosproject.tetopology.management.api.node.NetworkNodeEventSubject;
import org.onosproject.tetopology.management.api.node.NetworkNodeKey;
import org.onosproject.tetopology.management.api.node.TeNode;
import org.onosproject.tetopology.management.api.node.TeNodeEventSubject;
import org.onosproject.tetopology.management.api.node.TeNodeKey;
import org.onosproject.tetopology.management.api.node.TerminationPoint;
import org.onosproject.tetopology.management.api.node.TerminationPointKey;
import org.onosproject.tetopology.management.api.node.TtpKey;
import org.onosproject.tetopology.management.api.node.TunnelTerminationPoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.MoreObjects;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

/**
 * Implementation of the topology management service.
 */
@Component(immediate = true)
@Service
public class TeTopologyManager
    extends AbstractListenerProviderRegistry<TeTopologyEvent, TeTopologyListener,
                                             TeTopologyProvider, TeTopologyProviderService>
    implements TeTopologyService, TeTopologyProviderRegistry {
    private static final String APP_NAME = "org.onosproject.tetopology";
    public static final long DEFAULT_PROVIDER_ID = 0x0a0a0a0aL;
    private static final long DEFAULT_CLIENT_ID = 0x00L;
    private long providerId = DEFAULT_PROVIDER_ID;
    private static final int MAX_THREADS = 1;

    private static final Ip4Address DEFAULT_TENODE_ID_START = Ip4Address.valueOf("1.1.1.1");
    private static final Ip4Address DEFAULT_TENODE_ID_END = Ip4Address.valueOf("250.250.250.250");
    private Ip4Address teNodeIpStart = DEFAULT_TENODE_ID_START;
    private Ip4Address teNodeIpEnd = DEFAULT_TENODE_ID_END;
    private long nextTeNodeId = teNodeIpStart.toInt();
    private final Logger log = LoggerFactory.getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected NetworkConfigRegistry cfgService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DeviceService deviceService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected LinkService linkService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DeviceProviderRegistry deviceProviderRegistry;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected LinkProviderRegistry linkProviderRegistry;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    public TeTopologyStore store;

    private TeTopologyStoreDelegate delegate = this::post;
    private final BlockingQueue<TeTopologyEvent> eventQueue = new LinkedBlockingQueue<>();
    private final BlockingQueue<TeTopologyMapEvent> mapEventQueue = new LinkedBlockingQueue<>();
    private final ConfigFactory<ApplicationId, TeTopologyConfig> factory =
            new ConfigFactory<ApplicationId, TeTopologyConfig>(APP_SUBJECT_FACTORY,
                    TeTopologyConfig.class,
                    "teTopologyCfg",
                    false) {
        @Override
        public TeTopologyConfig createConfig() {
            return new TeTopologyConfig();
        }
    };
    private final NetworkConfigListener cfgLister = new InternalConfigListener();
    private ApplicationId appId;
    // The topology merged in MDSC
    private TeTopology mergedTopology = null;
    private TeTopologyKey mergedTopologyKey;
    private Network mergedNetwork = null;
    // Track new TE node id by its source TE node key
    private Map<TeNodeKey, Long> sourceNewTeNodeIdMap = Maps.newHashMap();
    // Track the external link keys by the plugId
    private Map<Long, LinkKeyPair> externalLinkMap = Maps.newHashMap();
    private ExecutorService executor;

    /**
     * Activation helper function.
     */
    public void activateBasics() {
        store.setDelegate(delegate);
        store.setMapEventQueue(mapEventQueue);
        eventDispatcher.addSink(TeTopologyEvent.class, listenerRegistry);
    }

    /**
     * Deactivation helper function.
     */
    public void deactivateBasics() {
        store.unsetDelegate(delegate);
        eventDispatcher.removeSink(TeTopologyEvent.class);
    }

    @Activate
    public void activate() {
        activateBasics();
        appId = coreService.registerApplication(APP_NAME);
        cfgService.registerConfigFactory(factory);
        executor = newFixedThreadPool(MAX_THREADS, groupedThreads("onos/tetopology", "build-%d", log));

        cfgService.addListener(cfgLister);
        executor.execute(new TopologyMergerTask());
        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        deactivateBasics();
        externalLinkMap.clear();
        cfgService.removeListener(cfgLister);
        cfgService.unregisterConfigFactory(factory);
        executor.shutdownNow();
        executor = null;
        eventQueue.clear();
        log.info("Stopped");
    }

    @Override
    protected TeTopologyProviderService createProviderService(TeTopologyProvider provider) {
        return new InternalTopologyProviderService(provider);
    }

    private class InternalTopologyProviderService
                      extends AbstractProviderService<TeTopologyProvider>
                      implements TeTopologyProviderService {

        protected InternalTopologyProviderService(TeTopologyProvider provider) {
            super(provider);
        }

        @Override
        public void networkUpdated(Network network) {
            store.updateNetwork(network);
        }

        @Override
        public void networkRemoved(KeyId networkId) {
            store.removeNetwork(networkId);
        }

        @Override
        public void linkUpdated(NetworkLinkKey linkKey, NetworkLink link) {
            store.updateNetworkLink(linkKey, link);
        }

        @Override
        public void linkRemoved(NetworkLinkKey linkKey) {
            store.removeNetworkLink(linkKey);
        }

        @Override
        public void nodeUpdated(NetworkNodeKey nodeKey, NetworkNode node) {
            store.updateNetworkNode(nodeKey, node);
        }

        @Override
        public void nodeRemoved(NetworkNodeKey nodeKey) {
            store.removeNetworkNode(nodeKey);
        }

        @Override
        public void terminationPointUpdated(TerminationPointKey terminationPointKey,
                TerminationPoint terminationPoint) {
            store.updateTerminationPoint(terminationPointKey, terminationPoint);
        }

        @Override
        public void terminationPointRemoved(TerminationPointKey terminationPointKey) {
            store.removeTerminationPoint(terminationPointKey);
        }
    }

    private boolean isCustomizedLearnedTopology(TeTopologyKey key) {
        if (store.teTopology(key).flags().get(TeTopology.BIT_CUSTOMIZED) &&
                store.teTopology(key).flags().get(TeTopology.BIT_LEARNT)) {
            return true;
        }
        return false;
    }

    // Task for merge the learned topology.
    private class TopologyMergerTask implements Runnable {

        public TopologyMergerTask() {
        }

        @Override
        public void run() {
            try {
                TeTopologyMapEvent event;
                while ((event = mapEventQueue.take()) != null) {
                    switch (event.type()) {
                    case TE_TOPOLOGY_ADDED:
                    case TE_TOPOLOGY_UPDATED:
                        TeTopology teTopology = store.teTopology(event.teTopologyKey());
                        post(new TeTopologyEvent(event.type(), teTopology));
                        if (event.type() == TE_TOPOLOGY_ADDED &&
                                teTopology.flags().get(TeTopology.BIT_CUSTOMIZED) &&
                                teTopology.flags().get(TeTopology.BIT_LEARNT)) {
                            mergeTopology(teTopology);
                        }
                        break;
                    case TE_TOPOLOGY_REMOVED:
                        post(new TeTopologyEvent(TE_TOPOLOGY_REMOVED,
                                                 new DefaultTeTopology(event.teTopologyKey(),
                                                                      null, null, null, null)));
                        break;
                    case TE_NODE_ADDED:
                    case TE_NODE_UPDATED:
                        TeNode teNode = store.teNode(event.teNodeKey());
                        post(new TeTopologyEvent(event.type(),
                                                 new TeNodeEventSubject(event.teNodeKey(), teNode)));
                        if (isCustomizedLearnedTopology(event.teNodeKey().teTopologyKey())) {
                            updateSourceTeNode(mergedTopology.teNodes(),
                                               event.teNodeKey().teTopologyKey(), teNode, true);
                        }
                        break;
                    case TE_NODE_REMOVED:
                        post(new TeTopologyEvent(TE_NODE_REMOVED,
                                                 new TeNodeEventSubject(event.teNodeKey(), null)));
                        if (isCustomizedLearnedTopology(event.teNodeKey().teTopologyKey())) {
                            removeSourceTeNode(mergedTopology.teNodes(), event.teNodeKey(), true);
                        }
                        break;
                    case TE_LINK_ADDED:
                    case TE_LINK_UPDATED:
                        TeLink teLink = store.teLink(event.teLinkKey());
                        post(new TeTopologyEvent(event.type(),
                                                 new TeLinkEventSubject(event.teLinkKey(), teLink)));
                        if (isCustomizedLearnedTopology(event.teLinkKey().teTopologyKey())) {
                            Map<TeLinkTpKey, TeLink> teLinks = Maps.newHashMap(mergedTopology.teLinks());
                            updateSourceTeLink(teLinks, event.teLinkKey().teTopologyKey(), teLink, true);
                            updateMergedTopology(mergedTopology.teNodes(), teLinks);
                        }
                        break;
                    case TE_LINK_REMOVED:
                        post(new TeTopologyEvent(TE_LINK_REMOVED,
                                                 new TeLinkEventSubject(event.teLinkKey(), null)));
                        if (isCustomizedLearnedTopology(event.teLinkKey().teTopologyKey())) {
                            Map<TeLinkTpKey, TeLink> teLinks = Maps.newHashMap(mergedTopology.teLinks());
                            removeSourceTeLink(teLinks, event.teLinkKey(), true);
                            updateMergedTopology(mergedTopology.teNodes(), teLinks);
                        }
                        break;
                    case NETWORK_ADDED:
                    case NETWORK_UPDATED:
                        Network network = store.network(event.networkKey());
                        post(new TeTopologyEvent(event.type(), network));
                        break;
                    case NETWORK_REMOVED:
                        post(new TeTopologyEvent(NETWORK_REMOVED,
                                                 new DefaultNetwork(event.networkKey(),
                                                                    null, null, null, null, false, null)));
                        break;
                    case NODE_ADDED:
                    case NODE_UPDATED:
                        NetworkNode node = store.networkNode(event.networkNodeKey());
                        post(new TeTopologyEvent(event.type(),
                                                 new NetworkNodeEventSubject(event.networkNodeKey(), node)));
                        break;
                    case NODE_REMOVED:
                        post(new TeTopologyEvent(NODE_REMOVED,
                                                 new NetworkNodeEventSubject(event.networkNodeKey(), null)));
                        break;
                    case LINK_ADDED:
                    case LINK_UPDATED:
                        NetworkLink link = store.networkLink(event.networkLinkKey());
                        post(new TeTopologyEvent(event.type(),
                                                 new NetworkLinkEventSubject(event.networkLinkKey(), link)));
                        break;
                    case LINK_REMOVED:
                        post(new TeTopologyEvent(LINK_REMOVED,
                                                 new NetworkLinkEventSubject(event.networkLinkKey(), null)));
                        break;
                    default:
                        break;
                    }
                }
            } catch (InterruptedException e) {
                log.warn("TopologyMergerTask is interrupted");
            } catch (Exception e) {
                log.warn("Unable to merge topology", e);
            }
        }
    }

    private void removeSourceTeNode(Map<Long, TeNode> teNodes,
                                    TeNodeKey srcNodeKey, boolean postEvent) {
       Long mergedTeNodeId = sourceNewTeNodeIdMap.remove(srcNodeKey);
       if (mergedTeNodeId == null) {
           return;
       }
       if (teNodes.remove(mergedTeNodeId) != null && postEvent) {
           post(new TeTopologyEvent(TE_NODE_REMOVED,
                                    new TeNodeEventSubject(
                                            new TeNodeKey(mergedTopologyKey,
                                                          mergedTeNodeId),
                                            null)));
       }
    }

    private void updateSourceTeNode(Map<Long, TeNode> teNodes, TeTopologyKey srcTopoKey,
                                    TeNode srcNode, boolean postEvent) {
        TeNodeKey sourceTeNodeId = new TeNodeKey(srcTopoKey, srcNode.teNodeId());
        Long mergedTeNodeId = sourceNewTeNodeIdMap.get(sourceTeNodeId);
        boolean addNode = false;
        if (mergedTeNodeId == null) {
            // New node
            addNode = true;
            mergedTeNodeId = nextTeNodeId;
            nextTeNodeId++;
            if (nextTeNodeId >= teNodeIpEnd.toInt()) {
                nextTeNodeId = teNodeIpStart.toInt();
                log.warn("TE node Id is wrapped back");
            }
            sourceNewTeNodeIdMap.put(sourceTeNodeId, mergedTeNodeId);
        }
        TeTopologyKey underlayTopologyId = null; // No underlay
        TeNodeKey supportTeNodeId = null; // No supporting

        CommonNodeData common = new CommonNodeData(srcNode.name(), srcNode.adminStatus(),
                                                   srcNode.opStatus(), srcNode.flags()); // No change
        Map<Long, ConnectivityMatrix> connMatrices = srcNode.connectivityMatrices();
        List<Long> teLinkIds = srcNode.teLinkIds(); // No change
        Map<Long, TunnelTerminationPoint> ttps = srcNode.tunnelTerminationPoints();
        List<Long> teTpIds = srcNode.teTerminationPointIds(); // No change
        DefaultTeNode newNode = new DefaultTeNode(mergedTeNodeId, underlayTopologyId,
                supportTeNodeId, sourceTeNodeId, common, connMatrices, teLinkIds,
                ttps, teTpIds);
        teNodes.put(mergedTeNodeId, newNode);
        if (postEvent) {
            //Post event for the TE node in the merged topology
            TeNodeKey globalKey = new TeNodeKey(mergedTopologyKey, mergedTeNodeId);
            post(new TeTopologyEvent(addNode ? TE_NODE_ADDED : TE_NODE_UPDATED,
                                     new TeNodeEventSubject(globalKey, newNode)));
            post(new TeTopologyEvent(addNode ? NODE_ADDED : NODE_UPDATED,
                                     new NetworkNodeEventSubject(TeMgrUtil.networkNodeKey(globalKey),
                                             TeMgrUtil.nodeBuilder(
                                                     KeyId.keyId(Long.toString(newNode.teNodeId())),
                                                     newNode))));
        }
    }

    // Merge TE nodes
    private void mergeNodes(Map<Long, TeNode> nodes, TeTopology topology) {

        if (!MapUtils.isEmpty(topology.teNodes())) {
            for (Map.Entry<Long, TeNode> entry : topology.teNodes().entrySet()) {
                updateSourceTeNode(nodes, topology.teTopologyId(), entry.getValue(),
                                   mergedTopology != null);
            }
        }
    }

    // Returns a new TeLink based on an existing TeLink with new attributes
    private TeLink updateTeLink(TeLinkTpKey newKey, TeLinkTpKey peerTeLinkKey,
            TeTopologyKey underlayTopologyId, TeLinkTpGlobalKey supportTeLinkId,
            TeLinkTpGlobalKey sourceTeLinkId, ExternalLink externalLink,
            TeLink exLink) {
        UnderlayPath underlayPath = new UnderlayPath(exLink.primaryPath(),
                exLink.backupPaths(), exLink.tunnelProtectionType(),
                exLink.sourceTtpId(), exLink.destinationTtpId(),
                exLink.teTunnelId()
                );
        TePathAttributes teAttributes = new TePathAttributes(exLink.cost(),
                exLink.delay(), exLink.srlgs());
        LinkBandwidth bandwidth = new LinkBandwidth(exLink.maxBandwidth(),
                                                    exLink.availBandwidth(),
                                                    exLink.maxAvailLspBandwidth(),
                                                    exLink.minAvailLspBandwidth(),
                                                    exLink.oduResource());
        CommonLinkData common = new CommonLinkData(exLink.adminStatus(), exLink.opStatus(),
                exLink.flags(), exLink.switchingLayer(), exLink.encodingLayer(),
                externalLink, underlayPath, teAttributes,
                exLink.administrativeGroup(), exLink.interLayerLocks(),
                bandwidth);
        return new DefaultTeLink(newKey, peerTeLinkKey, underlayTopologyId,
                supportTeLinkId, sourceTeLinkId, common);
    }

    private class LinkKeyPair {
        private TeLinkTpKey firstKey;
        private TeLinkTpKey secondKey;

        public LinkKeyPair(TeLinkTpKey firstKey) {
            this.firstKey = firstKey;
        }

        public TeLinkTpKey firstKey() {
            return firstKey;
        }

        public void setFirstKey(TeLinkTpKey firstKey) {
            this.firstKey = firstKey;
        }

        public TeLinkTpKey secondKey() {
            return secondKey;
        }

        public void setSecondKey(TeLinkTpKey secondKey) {
            this.secondKey = secondKey;
        }

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(this)
                    .add("firstKey", firstKey)
                    .add("secondKey", secondKey)
                    .toString();
        }
    }

    private void removeSourceTeLink(Map<TeLinkTpKey, TeLink> teLinks, TeLinkTpGlobalKey teLinkKey,
                                    boolean postEvent) {
        TeNodeKey sourceTeNodeKey = teLinkKey.teNodeKey();
        Long newTeNodeId = sourceNewTeNodeIdMap.get(sourceTeNodeKey);
        if (newTeNodeId == null) {
            return;
        }
        TeLinkTpKey newLinkKey = new TeLinkTpKey(newTeNodeId, teLinkKey.teLinkTpId());
        TeLink teLink = teLinks.remove(newLinkKey);
        if (teLink == null) {
            return;
        }
        //Post event
        if (postEvent) {
            post(new TeTopologyEvent(TE_LINK_REMOVED,
                                     new TeLinkEventSubject(
                                             new TeLinkTpGlobalKey(mergedTopologyKey,
                                                                   newLinkKey),
                                             null)));
        }

        if (teLink.externalLink() != null && teLink.externalLink().plugId() != null) {
            // Update the LinkKeyPair in externalLinkMap
            LinkKeyPair pair = externalLinkMap.get(teLink.externalLink().plugId());
            if (pair != null && pair.firstKey() != null &&
                    pair.firstKey().equals(newLinkKey)) {
                pair.setFirstKey(null);
            } else if (pair != null && pair.secondKey() != null &&
                    pair.secondKey().equals(newLinkKey)) {
                pair.setSecondKey(null);
            }
            if (pair != null && pair.firstKey() == null &&
                    pair.secondKey() == null) {
                externalLinkMap.remove(teLink.externalLink().plugId());
            }
        }
        TeLinkTpKey peerTeLinkKey = teLink.peerTeLinkKey();
        if (peerTeLinkKey != null) {
            // Update peerLink's peerTeLinkKey to null
            TeLink peerLink = teLinks.get(peerTeLinkKey);
            if (peerLink == null) {
                return;
            }
            TeLink newPeerLink = updateTeLink(peerTeLinkKey, null,
                                       peerLink.underlayTeTopologyId(), peerLink.supportingTeLinkId(),
                                       peerLink.sourceTeLinkId(), peerLink.externalLink(), peerLink);
            teLinks.put(peerTeLinkKey, newPeerLink);
            if (postEvent) {
                post(new TeTopologyEvent(TE_LINK_UPDATED,
                                         new TeLinkEventSubject(
                                                 new TeLinkTpGlobalKey(mergedTopologyKey,
                                                                       peerTeLinkKey),
                                                 newPeerLink)));
            }
        }
    }

    private void updateSourceTeLink(Map<TeLinkTpKey, TeLink> teLinks, TeTopologyKey srcTopoKey,
                                    TeLink srcLink, boolean postEvent) {
        TeNodeKey sourceTeNodeId = new TeNodeKey(srcTopoKey,
                                                 srcLink.teLinkKey().teNodeId());
        TeLinkTpKey newKey = new TeLinkTpKey(
                sourceNewTeNodeIdMap.get(sourceTeNodeId),
                srcLink.teLinkKey().teLinkTpId());
        TeLinkTpKey peerTeLinkKey = null;
        if (srcLink.peerTeLinkKey() != null) {
            TeNodeKey sourcePeerNode = new TeNodeKey(srcTopoKey,
                                                     srcLink.peerTeLinkKey().teNodeId());
            peerTeLinkKey = new TeLinkTpKey(
                    sourceNewTeNodeIdMap.get(sourcePeerNode),
                    srcLink.peerTeLinkKey().teLinkTpId());
        }

        if (srcLink.externalLink() != null &&
                srcLink.externalLink().plugId() != null) {
            // externalLinkKey doesn't have topology Id.
            // using plugId for now
            LinkKeyPair pair = externalLinkMap.get(srcLink.externalLink().plugId());
            if (pair != null) {
                if (pair.firstKey() == null) {
                    peerTeLinkKey = pair.secondKey;
                    pair.setFirstKey(newKey);
                } else {
                    peerTeLinkKey = pair.firstKey;
                    pair.setSecondKey(newKey);
                }

                TeLink peerLink = teLinks.get(peerTeLinkKey);
                if (peerLink != null) {
                    // Update peer Link with local link key
                    TeLink newPeerLink = updateTeLink(peerTeLinkKey, newKey,
                                                      peerLink.underlayTeTopologyId(),
                                                      peerLink.supportingTeLinkId(),
                                                      peerLink.sourceTeLinkId(),
                                                      peerLink.externalLink(), peerLink);
                    teLinks.put(peerTeLinkKey, newPeerLink);
                    if (postEvent) {
                        post(new TeTopologyEvent(TE_LINK_UPDATED,
                                                 new TeLinkEventSubject(
                                                         new TeLinkTpGlobalKey(mergedTopologyKey,
                                                                               peerTeLinkKey),
                                                         newPeerLink)));
                    }

               }
            } else {
                // Store it in the map
                externalLinkMap.put(srcLink.externalLink().plugId(), new LinkKeyPair(newKey));
            }
        }

        TeTopologyKey underlayTopologyId = null; // No underlay
        TeLinkTpGlobalKey supportTeLinkId = null; // No support
        // Source link for the new updated link
        TeLinkTpGlobalKey sourceTeLinkId = new TeLinkTpGlobalKey(srcTopoKey, srcLink.teLinkKey());
        TeLink updatedLink = updateTeLink(newKey, peerTeLinkKey, underlayTopologyId,
                                      supportTeLinkId, sourceTeLinkId,
                                      srcLink.externalLink(), srcLink);
        TeLinkTpGlobalKey newGlobalKey = new TeLinkTpGlobalKey(mergedTopologyKey, newKey);
        boolean newLink = teLinks.get(newGlobalKey) == null ? true : false;
        teLinks.put(newKey, updatedLink);
        if (postEvent) {
            //Post event
            post(new TeTopologyEvent(newLink ? TE_LINK_ADDED : TE_LINK_UPDATED,
                                     new TeLinkEventSubject(newGlobalKey, updatedLink)));
            post(new TeTopologyEvent(newLink ? LINK_ADDED : LINK_UPDATED,
                                     new NetworkLinkEventSubject(TeMgrUtil.networkLinkKey(newGlobalKey),
                                             TeMgrUtil.linkBuilder(TeMgrUtil.toNetworkLinkId(
                                                                          updatedLink.teLinkKey()),
                                                                   updatedLink))));
        }
    }

    // Merge TE links
    private void mergeLinks(Map<TeLinkTpKey, TeLink> teLinks, TeTopology topology) {
        if (!MapUtils.isEmpty(topology.teLinks())) {
            for (Map.Entry<TeLinkTpKey, TeLink> entry : topology.teLinks().entrySet()) {
                TeLink srcLink = entry.getValue();
                updateSourceTeLink(teLinks, topology.teTopologyId(), srcLink,
                                   mergedTopology != null);
            }
        }
    }

    // Update the merged topology with new TE nodes and links
    private void updateMergedTopology(Map<Long, TeNode> teNodes, Map<TeLinkTpKey, TeLink> teLinks) {
        boolean newTopology = mergedTopology == null;
        BitSet flags = newTopology ? new BitSet(TeConstants.FLAG_MAX_BITS) : mergedTopology.flags();
        flags.set(TeTopology.BIT_MERGED);
        CommonTopologyData commonData  = new CommonTopologyData(newTopology ?
                                                                TeMgrUtil.toNetworkId(mergedTopologyKey) :
                                                                mergedTopology.networkId(),
                                                                OptimizationType.NOT_OPTIMIZED,
                                                                flags, DeviceId.deviceId("localHost"));
        mergedTopology = new DefaultTeTopology(mergedTopologyKey, teNodes, teLinks,
                                               Long.toString(mergedTopologyKey.topologyId()), commonData);
        mergedNetwork = TeMgrUtil.networkBuilder(mergedTopology);
        log.info("Nodes# {}, Links# {}", mergedTopology.teNodes().size(), mergedTopology.teLinks().size());
    }

    // Merge the new learned topology
    private void mergeTopology(TeTopology topology) {
        boolean newTopology = mergedTopology == null;
        mergedTopologyKey = newTopology ?
                            new TeTopologyKey(providerId, DEFAULT_CLIENT_ID,
                                              store.nextTeTopologyId()) :
                            mergedTopology.teTopologyId();

        Map<Long, TeNode> teNodes = newTopology || mergedTopology.teNodes() == null ?
                Maps.newHashMap() : Maps.newHashMap(mergedTopology.teNodes());
        mergeNodes(teNodes, topology);
        Map<TeLinkTpKey, TeLink> teLinks = newTopology || mergedTopology.teLinks() == null ?
                Maps.newHashMap() : Maps.newHashMap(mergedTopology.teLinks());
        mergeLinks(teLinks, topology);
        updateMergedTopology(teNodes, teLinks);
        log.info("mergedTopology {}", mergedTopology);

        if (newTopology) {
            // Post events for the merged network topology;
            post(new TeTopologyEvent(TE_TOPOLOGY_ADDED, mergedTopology));
            post(new TeTopologyEvent(NETWORK_ADDED, mergedNetwork));
        }
    }

    private TeTopologyKey newTeTopologyKey(TeTopology teTopology) {
        TeTopologyKey key = teTopology.teTopologyId();
        if (key == null || teTopology.teTopologyIdStringValue() == null) {
            log.error("Ignoring the non-TE topology");
            throw new ApplicationException("Missing TE topology ID");
        }
        // Get the topologyId numeric value
        long idValue = key.topologyId();
        if (idValue == TeConstants.NIL_LONG_VALUE) {
            if (teTopology.teTopologyIdStringValue() != null) {
                try {
                    idValue = Long.parseLong(teTopology.teTopologyIdStringValue());
                } catch (NumberFormatException e) {
                    // Can't get the long value from the string.
                    // Use an assigned id value from local id pool,
                    idValue = store.nextTeTopologyId();
                }
                return new TeTopologyKey(key.providerId(), key.clientId(), idValue);
            }
        }
        return null;
    }

    private class InternalConfigListener implements NetworkConfigListener {

        @Override
        public void event(NetworkConfigEvent event) {
            try {
                providerId = cfgService.getConfig(appId, TeTopologyConfig.class)
                                       .providerId();
                teNodeIpStart = cfgService.getConfig(appId, TeTopologyConfig.class)
                                          .teNodeIpStart();
                teNodeIpEnd = cfgService.getConfig(appId, TeTopologyConfig.class)
                                        .teNodeIpEnd();
                nextTeNodeId = teNodeIpStart.toInt();
            } catch (ConfigException e) {
                log.error("Configuration error {}", e);
            }
        }

        @Override
        public boolean isRelevant(NetworkConfigEvent event) {
            return event.configClass().equals(TeTopologyConfig.class) &&
                    (event.type() == CONFIG_ADDED ||
                    event.type() == CONFIG_UPDATED);
        }
    }

    @Override
    public TeTopologies teTopologies() {
        Map<TeTopologyKey, TeTopology> map;
        if (MapUtils.isNotEmpty(store.teTopologies().teTopologies())) {
            map = Maps.newHashMap(store.teTopologies().teTopologies());
        } else {
            map = Maps.newHashMap();
        }
        if (mergedTopology != null) {
            map.put(mergedTopologyKey, mergedTopology);
        }
        return new DefaultTeTopologies(store.teTopologies().name(), map);
    }

    @Override
    public TeTopology teTopology(TeTopologyKey topologyId) {
        if (mergedTopology != null &&
                topologyId.equals(mergedTopologyKey)) {
            return mergedTopology;
        }
        return store.teTopology(topologyId);
    }

    @Override
    public TeTopology mergedTopology() {
        return mergedTopology;
    }

    @Override
    public void updateTeTopology(TeTopology teTopology) {
        TeTopologyKey newKey = null;
        try {
            newKey = newTeTopologyKey(teTopology);
        } catch (ApplicationException e) {
            log.error("Ignoring the non-TE topology");
            return;
        }

        // TE topology is updated here from other APP or NBI, the flag
        // BIT_CUSTOMIZED or BIT_MERGED should be set.
        BitSet flags = teTopology.flags();
        if (flags == null ||
                !(flags.get(TeTopology.BIT_CUSTOMIZED) ||
                  flags.get(TeTopology.BIT_MERGED))) {
            log.error("TE topology flags {} are not set properly", flags);
            return;
        }

        if (newKey != null) {
            DefaultTeTopology newTopology = new DefaultTeTopology(
                    newKey == null ? teTopology.teTopologyId() : newKey,
                    teTopology.teNodes(), teTopology.teLinks(),
                    teTopology.teTopologyIdStringValue(), new CommonTopologyData(teTopology));
            // Update with new data
            store.updateTeTopology(newTopology);
        } else {
            store.updateTeTopology(teTopology);
        }
    }

    @Override
    public void removeTeTopology(TeTopologyKey topologyId) {
        store.removeTeTopology(topologyId);
    }

    @Override
    public Networks networks() {
        List<Network> networks;
        if (CollectionUtils.isNotEmpty(store.networks())) {
            networks = Lists.newArrayList(store.networks());
        } else {
            networks = Lists.newArrayList();
        }
        if (mergedNetwork != null) {
            networks.add(mergedNetwork);
        }
        return new DefaultNetworks(networks);
    }

    @Override
    public Network network(KeyId networkId) {
        if (mergedNetwork != null &&
                mergedNetwork.networkId().equals(networkId)) {
            return mergedNetwork;
        }
        return store.network(networkId);
    }

    @Override
    public void updateNetwork(Network network) {
        // TODO: This will be implemented if required.
    }

    @Override
    public void removeNetwork(KeyId networkId) {
        // TODO: This will be implemented if required.
    }

    @Override
    public TeNode teNode(TeNodeKey nodeId) {
        if (nodeId.teTopologyKey().equals(mergedTopologyKey)) {
            return mergedTopology.teNode(nodeId.teNodeId());
        }
        return store.teNode(nodeId);
    }

    @Override
    public TeLink teLink(TeLinkTpGlobalKey linkId) {
        if (linkId.teTopologyKey().equals(mergedTopologyKey)) {
            return mergedTopology.teLink(linkId.teLinkTpKey());
        }
        return store.teLink(linkId);
    }

    @Override
    public TunnelTerminationPoint tunnelTerminationPoint(TtpKey ttpId) {
        if (ttpId.teTopologyKey().equals(mergedTopologyKey)) {
            return mergedTopology.teNode(ttpId.teNodeId())
                    .tunnelTerminationPoint(ttpId.ttpId());
        }
        return store.tunnelTerminationPoint(ttpId);
    }

    @Override
    public KeyId networkId(TeTopologyKey teTopologyKey) {
        return store.networkId(teTopologyKey);
    }

    @Override
    public NetworkNodeKey nodeKey(TeNodeKey teNodeKey) {
        return store.nodeKey(teNodeKey);
    }

    @Override
    public NetworkLinkKey linkKey(TeLinkTpGlobalKey teLinkKey) {
        return store.linkKey(teLinkKey);
    }

    @Override
    public TerminationPointKey terminationPointKey(TeLinkTpGlobalKey teTpKey) {
        return store.terminationPointKey(teTpKey);
    }
}

/*
 * Copyright 2016 Open Networking Laboratory
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

import static org.onosproject.net.config.NetworkConfigEvent.Type.CONFIG_ADDED;
import static org.onosproject.net.config.NetworkConfigEvent.Type.CONFIG_UPDATED;
import static org.onosproject.net.config.basics.SubjectFactories.APP_SUBJECT_FACTORY;

import java.util.ArrayList;
import java.util.List;
import java.util.Collection;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onlab.packet.Ip4Address;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.incubator.net.config.basics.ConfigException;
import org.onosproject.net.DeviceId;
import org.onosproject.net.MastershipRole;
import org.onosproject.net.PortNumber;
import org.onosproject.net.config.ConfigFactory;
import org.onosproject.net.config.NetworkConfigEvent;
import org.onosproject.net.config.NetworkConfigListener;
import org.onosproject.net.config.NetworkConfigRegistry;
import org.onosproject.net.device.DeviceProvider;
import org.onosproject.net.device.DeviceProviderRegistry;
import org.onosproject.net.device.DeviceProviderService;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.link.LinkProvider;
import org.onosproject.net.link.LinkProviderRegistry;
import org.onosproject.net.link.LinkProviderService;
import org.onosproject.net.link.LinkService;
import org.onosproject.net.provider.AbstractListenerProviderRegistry;
import org.onosproject.net.provider.AbstractProviderService;
import org.onosproject.net.provider.ProviderId;
import org.onosproject.tetopology.management.api.DefaultNetwork;
import org.onosproject.tetopology.management.api.DefaultNetworks;
import org.onosproject.tetopology.management.api.InternalTeNetwork;
import org.onosproject.tetopology.management.api.KeyId;
import org.onosproject.tetopology.management.api.Network;
import org.onosproject.tetopology.management.api.Networks;
import org.onosproject.tetopology.management.api.TeTopologyEvent;
import org.onosproject.tetopology.management.api.TeTopologyId;
import org.onosproject.tetopology.management.api.TeTopologyListener;
import org.onosproject.tetopology.management.api.TeTopologyProvider;
import org.onosproject.tetopology.management.api.TeTopologyProviderRegistry;
import org.onosproject.tetopology.management.api.TeTopologyProviderService;
import org.onosproject.tetopology.management.api.TeTopologyService;
import org.onosproject.tetopology.management.api.TeTopologyStore;
import org.onosproject.tetopology.management.api.TeTopologyStoreDelegate;
import org.onosproject.tetopology.management.api.TeTopologyType;
import org.onosproject.tetopology.management.api.link.DefaultNetworkLink;
import org.onosproject.tetopology.management.api.link.NetworkLink;
import org.onosproject.tetopology.management.api.link.NetworkLinkKey;
import org.onosproject.tetopology.management.api.node.ConnectivityMatrix;
import org.onosproject.tetopology.management.api.node.DefaultNetworkNode;
import org.onosproject.tetopology.management.api.node.DefaultTerminationPoint;
import org.onosproject.tetopology.management.api.node.NetworkNode;
import org.onosproject.tetopology.management.api.node.NetworkNodeKey;
import org.onosproject.tetopology.management.api.node.TeNode;
import org.onosproject.tetopology.management.api.node.TerminationPoint;
import org.onosproject.tetopology.management.api.node.TerminationPointKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;

/**
 * Implementation of the topology management service.
 */
@Component(immediate = true)
@Service
public class TeTopologyManager
        extends AbstractListenerProviderRegistry<TeTopologyEvent, TeTopologyListener,
        TeTopologyProvider, TeTopologyProviderService>
        implements TeTopologyService, TeTopologyProviderRegistry, DeviceProvider, LinkProvider {
    private static final String APP_NAME = "org.onosproject.tetopology";
    private static final String IETF_TE_TOPOLOGY_MANAGER = "ietf-te-topology-manager";
    private static final String PROVIDER = "org.onosproject.provider.ietfte.objects";
    private static final long MY_PROVIDER_ID = 0x0a0a0a0aL;
    private static final long DEFAUL_CLIENT_ID = 0x00L;
    private static final String MY_NATIVE_TOPOLOGY_ID = "onos-sc-topo-1";
    private static final TeTopologyId DEFAULT_TOPOLOGY_ID = new TeTopologyId(MY_PROVIDER_ID,
                                                                             DEFAUL_CLIENT_ID,
                                                                             MY_NATIVE_TOPOLOGY_ID);
    //teTopologyId is configurable from Network Config
    private TeTopologyId teTopologyId = DEFAULT_TOPOLOGY_ID;

    private static final Ip4Address NEW_TE_NODE_ID_START = Ip4Address.valueOf("1.1.1.1");
    private static final Ip4Address NEW_TE_NODE_ID_END = Ip4Address.valueOf("1.1.250.250");
    private static final String MDSC_URI_PREFIX = "MDSC";
    private static Ip4Address newTeNodeId = NEW_TE_NODE_ID_START;

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

    //Only network level data is stored in this subsystem.
    //Link and Device details is stored in Link and Device subsystems.
    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    public TeTopologyStore store;

    //private TeTopologyStoreDelegate delegate = this::post;
    private final TeTopologyStoreDelegate delegate = new InternalStoreDelegate();

    private final ConfigFactory<ApplicationId, TeTopologyIdConfig> factory =
            new ConfigFactory<ApplicationId, TeTopologyIdConfig>(APP_SUBJECT_FACTORY,
                                                                 TeTopologyIdConfig.class,
                                                                 "teTopologyId",
                                                                 true) {
                @Override
                public TeTopologyIdConfig createConfig() {
                    return new TeTopologyIdConfig();
                }
            };
    private final NetworkConfigListener cfgLister = new InternalConfigListener();
    private ApplicationId appId;

    private DeviceProviderService deviceProviderService;
    private LinkProviderService linkProviderService;

    /**
     * Activation helper function.
     */
    public void activateBasics() {
        store.setDelegate(delegate);
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
        cfgService.addListener(cfgLister);

        deviceProviderService = deviceProviderRegistry.register(this);
        linkProviderService = linkProviderRegistry.register(this);

        //TODO: Needs to add the event listener into LINK and Device subsystem

        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        deactivateBasics();

        cfgService.removeListener(cfgLister);
        cfgService.unregisterConfigFactory(factory);

        deviceProviderRegistry.unregister(this);
        linkProviderRegistry.unregister(this);

        //TODO: Needs to remove the event listener from LINK and Device subsystem

        log.info("Stopped");
    }

    @Override
    public Networks getNetworks() {
        // return a list of the native networks
        List<InternalTeNetwork> teNetworks = store.getNetworks(TeTopologyType.NATIVE);

        List<Network> defaultNetworks = new ArrayList<>();
        for (InternalTeNetwork teNetwork : teNetworks) {
            defaultNetworks.add(teNetwork);
        }

        return (new DefaultNetworks(defaultNetworks));
    }

    @Override
    public Network getNetwork(KeyId networkId) {
        return new DefaultNetwork(store.getNetwork(networkId));
    }

    @Override
    public void updateNetwork(Network network) {
        store.updateNetwork(new InternalTeNetwork(TeTopologyType.CONFIGURED, new DefaultNetwork(network)));
        //TODO: Need to update nodes and links to Device/Link subsystems.
    }

    @Override
    public void removeNetwork(KeyId networkId) {
        store.removeNetwork(networkId);
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
            // Store received network data into the local TE topology data store
            InternalTeNetwork teNetwork = new InternalTeNetwork(TeTopologyType.SUBORDINATE, network);
            store.updateNetwork(teNetwork);

            // let's do it here for now
            mergeNetworks();

            //TODO: Store node and link in Device/Link subsystem
            //deviceProviderService.deviceConnected(arg0, arg1);
            //linkProviderService.linkDetected(arg0);
        }

        @Override
        public void networkRemoved(KeyId networkId) {
            store.removeNetwork(networkId);
        }

        @Override
        public void linkUpdated(NetworkLinkKey linkKey, NetworkLink link) {
            // Need to check if this is a new link

            //deviceProviderService.deviceConnected(arg0, arg1);
        }

        @Override
        public void linkRemoved(NetworkLinkKey linkKey) {
            // No action is required (TODO: Auto-generated method stub)
        }

        @Override
        public void nodeUpdated(NetworkNodeKey nodeKey, NetworkNode node) {
            // Need to check if this is a new node

            // No action is required (TODO: Auto-generated method stub)
        }

        @Override
        public void nodeRemoved(NetworkNodeKey nodeKey) {
            // No action is required (TODO: Auto-generated method stub)
        }

        @Override
        public void terminationPointUpdated(TerminationPointKey terminationPointKey,
                                            TerminationPoint terminationPoint) {
            // No action is required (TODO: Auto-generated method stub)
        }

        @Override
        public void terminationPointRemoved(TerminationPointKey terminationPointKey) {
            // No action is required (TODO: Auto-generated method stub)
        }

    }

    private class InternalStoreDelegate implements TeTopologyStoreDelegate {
        @Override
        public void notify(TeTopologyEvent event) {
            if (event != null) {
                //post(event);
                processEvent(event);
            }
        }
    }

    private void processEvent(TeTopologyEvent event) {
        log.info("ProcessEvent {}", event.type().toString());

        //TODO - partial merge when network is updated
        if (event.type() == TeTopologyEvent.Type.NETWORK_ADDED) {
            // move network merging to networkUpdated()
            //mergeNetworks();
        }

        //TODO: Merge node and links from Device/Links subsytems if required.

        post(event);
    }

    private void mergeNetworks() {
        /*
           * Merge all subordinate TE topologies, create a simple merged native topology
           * and store it in the topology store.
           */
         /* TODO - generate new id based on its provider id + network id */
        KeyId newNetworkId = KeyId.keyId(Long.toString(teTopologyId.providerId()) + "-" + teTopologyId.topologyId());
        store.removeNetwork(newNetworkId);
         /* create list of links, nodes and termination points */
        List<NetworkLink> allLinks = new ArrayList<>();
        List<NetworkNode> allNodes = new ArrayList<>();
        List<KeyId> allSupportingNetworkIds = new ArrayList<>();

         /* translate keys for links/nodes/tps */
        List<InternalTeNetwork> subordNetworks = store.getNetworks(TeTopologyType.SUBORDINATE);
        for (InternalTeNetwork network : subordNetworks) {
            allSupportingNetworkIds.add(network.networkId());

                /* create and add new nodes */
            List<NetworkNode> nodes = network.getNodes();
            for (NetworkNode node : nodes) {

                KeyId newNodeId = KeyId.keyId(MDSC_URI_PREFIX + node.nodeId());
                TeNode newTeNode = null;
                TeNode origTeNode = node.getTe();
                if (origTeNode != null) {
                    newTeNode = new TeNode(origTeNode.teNodeId());
                    newTeNode.setName(origTeNode.name());
                    newTeNode.setAdminStatus(origTeNode.adminStatus());
                    newTeNode.setOpStatus(origTeNode.opStatus());
                    newTeNode.setAbstract(origTeNode.isAbstract());
                    List<ConnectivityMatrix> newConnMatrices = new ArrayList<>();

                    for (ConnectivityMatrix conn : origTeNode.connectivityMatrices()) {
                        KeyId tpId = conn.from().tpId();
                        KeyId newFromTpId = KeyId.keyId(MDSC_URI_PREFIX + tpId);
                        TerminationPointKey newFrom = new TerminationPointKey(newNetworkId, newNodeId, newFromTpId);

                        tpId = conn.to().tpId();
                        KeyId newToTpId = KeyId.keyId(MDSC_URI_PREFIX + tpId);
                        TerminationPointKey newTo = new TerminationPointKey(newNetworkId, newNodeId, newToTpId);
                        ConnectivityMatrix newConnMatrix =
                                new ConnectivityMatrix(conn.id(), newFrom, newTo, conn.isAllowed());
                        newConnMatrices.add(newConnMatrix);
                    }
                    newTeNode.setConnectivityMatrices(newConnMatrices);
                    newTeNode.setUnderlayTopology(origTeNode.underlayTopology());
                    newTeNode.setTunnelTerminationPoints(origTeNode.tunnelTerminationPoints());
                }
                List<NetworkNodeKey> supportingNodes = Lists.newArrayList();
                supportingNodes.add(new NetworkNodeKey(network.networkId(), node.nodeId()));
                DefaultNetworkNode newNode =
                        new DefaultNetworkNode(newNodeId, supportingNodes, newTeNode);
                List<TerminationPoint> newTps = Lists.newArrayList();

                List<TerminationPoint> origTps = node.getTerminationPoints();
                if (nonEmpty(origTps)) {
                    for (TerminationPoint tp : origTps) {
                        DefaultTerminationPoint newTp =
                            new DefaultTerminationPoint(KeyId.keyId(MDSC_URI_PREFIX + tp.id()));
                        List<TerminationPointKey> supportTps = Lists.newArrayList();
                        supportTps.add(new TerminationPointKey(network.networkId(), node.nodeId(), tp.id()));
                        newTp.setSupportingTpIds(supportTps);
                        newTps.add(newTp);
                    }
                }
                newNode.setTerminationPoints(newTps);
                allNodes.add(newNode);
            }

                /* create and add new links */
            List<NetworkLink> links = network.getLinks();
            if (nonEmpty(links)) {
                for (NetworkLink link : links) {
                    KeyId newLinkId = KeyId.keyId(MDSC_URI_PREFIX + link.linkId());
                    KeyId k = link.getSource().nodeId();
                    KeyId newSourceNodeId =
                            KeyId.keyId(MDSC_URI_PREFIX + k);
                    k = link.getSource().tpId();
                    KeyId newSourceNodeTpId =
                            KeyId.keyId(MDSC_URI_PREFIX + k);
                    k = link.getDestination().nodeId();
                    KeyId newDestNodeId =
                            KeyId.keyId(MDSC_URI_PREFIX + k);
                    k = link.getDestination().tpId();
                    KeyId newDestNodeTpId =
                            KeyId.keyId(MDSC_URI_PREFIX + k);
                    TerminationPointKey newSourceNodeTp =
                            new TerminationPointKey(newNetworkId, newSourceNodeId, newSourceNodeTpId);
                    TerminationPointKey newDestNodeTp =
                            new TerminationPointKey(newNetworkId, newDestNodeId, newDestNodeTpId);

                    DefaultNetworkLink newLink = new DefaultNetworkLink(newLinkId);
                    newLink.setSource(newSourceNodeTp);
                    newLink.setDestination(newDestNodeTp);
                    List<NetworkLinkKey> supportLinks = Lists.newArrayList();
                    supportLinks.add(new NetworkLinkKey(network.networkId(), link.linkId()));
                    newLink.setSupportingLinkIds(supportLinks);
                    newLink.setTe(link.getTe());

                    allLinks.add(newLink);
                }
            }
        }

         /* save generated native TE network into the store */
        if (allNodes.size() > 0) {
            //TeTopologyId newTopoId = new TeTopologyId(MY_PROVIDER_ID, 0L, NATIVE_TOPOLOGY_ID);
            DefaultNetwork nativeDefaultNetwork =
                    new DefaultNetwork(newNetworkId, allSupportingNetworkIds, allNodes, allLinks, teTopologyId, true);
            InternalTeNetwork newTeNetwork = new InternalTeNetwork(TeTopologyType.NATIVE, nativeDefaultNetwork);
            store.updateNetwork(newTeNetwork);
        }
    }

    @Override
    public ProviderId id() {
        return new ProviderId(IETF_TE_TOPOLOGY_MANAGER, PROVIDER);
    }

    private class InternalConfigListener implements NetworkConfigListener {

        @Override
        public void event(NetworkConfigEvent event) {
            try {
                teTopologyId = cfgService.getConfig(appId, TeTopologyIdConfig.class).getTeTopologyId();
            } catch (ConfigException e) {
                log.error("Configuration error {}", e);
            }
            log.info("new teTopologyId is {}", teTopologyId);
        }

        @Override
        public boolean isRelevant(NetworkConfigEvent event) {
            return event.configClass().equals(TeTopologyIdConfig.class) &&
                    (event.type() == CONFIG_ADDED ||
                            event.type() == CONFIG_UPDATED);
        }
    }

    @Override
    public void changePortState(DeviceId arg0, PortNumber arg1, boolean arg2) {
        // TODO: This will be implemented if required.
    }

    @Override
    public boolean isReachable(DeviceId arg0) {
        // TODO: This will be implemented if required.
        return false;
    }

    @Override
    public void roleChanged(DeviceId arg0, MastershipRole arg1) {
        // TODO: This will be implemented if required.
    }

    @Override
    public void triggerProbe(DeviceId arg0) {
        // TODO: This will be implemented if required.
    }

    private Ip4Address assignTeNodeId() {
        int value = newTeNodeId.toInt();

        if (value >= NEW_TE_NODE_ID_END.toInt()) {
            value = NEW_TE_NODE_ID_START.toInt();
        }
        return Ip4Address.valueOf(value);
    }

    private static boolean nonEmpty(Collection<?> c) {
        return c != null && !c.isEmpty();
    }
}

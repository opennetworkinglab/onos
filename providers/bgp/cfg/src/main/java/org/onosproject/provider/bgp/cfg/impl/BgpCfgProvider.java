/*
 * Copyright 2015-present Open Networking Laboratory
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
package org.onosproject.provider.bgp.cfg.impl;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;

import org.onosproject.bgp.controller.BgpCfg;
import org.onosproject.bgp.controller.BgpPeerCfg;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.net.config.ConfigFactory;
import org.onosproject.net.config.NetworkConfigEvent;
import org.onosproject.net.config.NetworkConfigListener;
import org.onosproject.net.config.NetworkConfigRegistry;
import org.onosproject.net.config.NetworkConfigService;
import org.onosproject.net.config.basics.SubjectFactories;
import org.onosproject.net.provider.AbstractProvider;
import org.onosproject.net.provider.ProviderId;
import org.onosproject.bgp.controller.BgpController;
import org.slf4j.Logger;
import org.osgi.service.component.ComponentContext;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * BGP config provider to validate and populate the configuration.
 */
@Component(immediate = true)
public class BgpCfgProvider extends AbstractProvider {

    private static final Logger log = getLogger(BgpCfgProvider.class);

    static final String PROVIDER_ID = "org.onosproject.provider.bgp.cfg";

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected BgpController bgpController;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected NetworkConfigRegistry configRegistry;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected NetworkConfigService configService;

    private final ConfigFactory configFactory =
            new ConfigFactory(SubjectFactories.APP_SUBJECT_FACTORY, BgpAppConfig.class, "bgpapp") {
                @Override
                public BgpAppConfig createConfig() {
                    return new BgpAppConfig();
                }
            };

    private final NetworkConfigListener configListener = new InternalConfigListener();

    private ApplicationId appId;

    /**
     * Creates a Bgp config provider.
     */
    public BgpCfgProvider() {
        super(new ProviderId("bgp", PROVIDER_ID));
    }

    @Activate
    public void activate(ComponentContext context) {
        appId = coreService.registerApplication(PROVIDER_ID);
        configService.addListener(configListener);
        configRegistry.registerConfigFactory(configFactory);
        readConfiguration();
        log.info("BGP cfg provider started");
    }

    @Deactivate
    public void deactivate(ComponentContext context) {
        configRegistry.unregisterConfigFactory(configFactory);
        configService.removeListener(configListener);
    }

    void setBgpController(BgpController bgpController) {
        this.bgpController = bgpController;
    }

    /**
     * Reads the configuration and set it to the BGP-LS south bound protocol.
     */
    private void readConfiguration() {
        BgpCfg bgpConfig = null;
        List<BgpAppConfig.BgpPeerConfig> nodes;
        bgpConfig = bgpController.getConfig();
        BgpAppConfig config = configRegistry.getConfig(appId, BgpAppConfig.class);

        if (config == null) {
            log.warn("No configuration found");
            return;
        }

        /*Set the configuration */
        bgpConfig.setRouterId(config.routerId());
        bgpConfig.setAsNumber(config.localAs());
        bgpConfig.setLsCapability(config.lsCapability());
        bgpConfig.setHoldTime(config.holdTime());
        bgpConfig.setMaxSession(config.maxSession());
        bgpConfig.setLargeASCapability(config.largeAsCapability());

        if (config.flowSpecCapability() == null) {
            bgpConfig.setFlowSpecCapability(BgpCfg.FlowSpec.NONE);
        } else {
            if (config.flowSpecCapability().equals("IPV4")) {
                bgpConfig.setFlowSpecCapability(BgpCfg.FlowSpec.IPV4);
            } else if (config.flowSpecCapability().equals("VPNV4")) {
                bgpConfig.setFlowSpecCapability(BgpCfg.FlowSpec.VPNV4);
            } else if (config.flowSpecCapability().equals("IPV4_VPNV4")) {
                bgpConfig.setFlowSpecCapability(BgpCfg.FlowSpec.IPV4_VPNV4);
            } else {
                bgpConfig.setFlowSpecCapability(BgpCfg.FlowSpec.NONE);
            }
        }
        bgpConfig.setFlowSpecRpdCapability(config.rpdCapability());

        nodes = config.bgpPeer();
        for (int i = 0; i < nodes.size(); i++) {
            String connectMode = nodes.get(i).connectMode();
            bgpConfig.addPeer(nodes.get(i).hostname(), nodes.get(i).asNumber(), nodes.get(i).holdTime());
            if (connectMode.equals(BgpAppConfig.PEER_CONNECT_ACTIVE)) {
                bgpConfig.connectPeer(nodes.get(i).hostname());
            }
        }
    }

    /**
     * Read the configuration and update it to the BGP-LS south bound protocol.
     */
    private void updateConfiguration() {
        BgpCfg bgpConfig = null;
        List<BgpAppConfig.BgpPeerConfig> nodes;
        TreeMap<String, BgpPeerCfg> bgpPeerTree;
        bgpConfig = bgpController.getConfig();
        BgpPeerCfg peer = null;
        BgpAppConfig config = configRegistry.getConfig(appId, BgpAppConfig.class);

        if (config == null) {
            log.warn("No configuration found");
            return;
        }


        /* Update the self configuration */
        if (bgpController.connectedPeerCount() != 0) {
            //TODO: If connections already exist, disconnect
            bgpController.closeConnectedPeers();
        }
        bgpConfig.setRouterId(config.routerId());
        bgpConfig.setAsNumber(config.localAs());
        bgpConfig.setLsCapability(config.lsCapability());
        bgpConfig.setHoldTime(config.holdTime());
        bgpConfig.setMaxSession(config.maxSession());
        bgpConfig.setLargeASCapability(config.largeAsCapability());

        if (config.flowSpecCapability() == null) {
            bgpConfig.setFlowSpecCapability(BgpCfg.FlowSpec.NONE);
        } else {
            if (config.flowSpecCapability().equals("IPV4")) {
                bgpConfig.setFlowSpecCapability(BgpCfg.FlowSpec.IPV4);
            } else if (config.flowSpecCapability().equals("VPNV4")) {
                bgpConfig.setFlowSpecCapability(BgpCfg.FlowSpec.VPNV4);
            } else if (config.flowSpecCapability().equals("IPV4_VPNV4")) {
                bgpConfig.setFlowSpecCapability(BgpCfg.FlowSpec.IPV4_VPNV4);
            } else {
                bgpConfig.setFlowSpecCapability(BgpCfg.FlowSpec.NONE);
            }
        }
        bgpConfig.setFlowSpecRpdCapability(config.rpdCapability());

        /* update the peer configuration */
        bgpPeerTree = bgpConfig.getPeerTree();
        if (bgpPeerTree.isEmpty()) {
            log.info("There are no BGP peers to iterate");
        } else {
            Set set = bgpPeerTree.entrySet();
            Iterator i = set.iterator();
            List<BgpPeerCfg> absPeerList = new ArrayList<BgpPeerCfg>();

            boolean exists = false;

            while (i.hasNext()) {
                Map.Entry me = (Map.Entry) i.next();
                peer = (BgpPeerCfg) me.getValue();

                nodes = config.bgpPeer();
                for (int j = 0; j < nodes.size(); j++) {
                    String peerIp = nodes.get(j).hostname();
                    if (peerIp.equals(peer.getPeerRouterId())) {

                        if (bgpConfig.isPeerConnectable(peer.getPeerRouterId())) {
                            peer.setAsNumber(nodes.get(j).asNumber());
                            peer.setHoldtime(nodes.get(j).holdTime());
                            log.debug("Peer neighbor IP successfully modified :" + peer.getPeerRouterId());
                        } else {
                            log.debug("Peer neighbor IP cannot be modified :" + peer.getPeerRouterId());
                        }

                        nodes.remove(j);
                        exists = true;
                        break;
                    }
                }

                if (!exists) {
                    absPeerList.add(peer);
                    exists = false;
                }

                if (peer.connectPeer() != null) {
                    peer.connectPeer().disconnectPeer();
                    peer.setConnectPeer(null);
                }
            }

            /* Remove the absent nodes. */
            for (int j = 0; j < absPeerList.size(); j++) {
                bgpConfig.removePeer(absPeerList.get(j).getPeerRouterId());
            }
        }


        nodes = config.bgpPeer();
        for (int i = 0; i < nodes.size(); i++) {
            String connectMode = nodes.get(i).connectMode();
            bgpConfig.addPeer(nodes.get(i).hostname(), nodes.get(i).asNumber(), nodes.get(i).holdTime());
            if (connectMode.equals(BgpAppConfig.PEER_CONNECT_ACTIVE)) {
                bgpConfig.connectPeer(nodes.get(i).hostname());
            }
        }

    }

    /**
     * BGP config listener to populate the configuration.
     */
    private class InternalConfigListener implements NetworkConfigListener {

        @Override
        public void event(NetworkConfigEvent event) {
            if (!event.configClass().equals(BgpAppConfig.class)) {
                return;
            }

            switch (event.type()) {
                case CONFIG_ADDED:
                    readConfiguration();
                    break;
                case CONFIG_UPDATED:
                    updateConfiguration();
                    break;
                case CONFIG_REMOVED:
                default:
                    break;
            }
        }
    }
}

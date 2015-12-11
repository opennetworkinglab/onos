/*
 * Copyright 2015 Open Networking Laboratory
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

import java.util.List;

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
     *
     * @return void
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
                    readConfiguration();
                    break;
                case CONFIG_REMOVED:
                default:
                    break;
            }
        }
    }
}

/*
 * Copyright 2014-2015 Open Networking Laboratory
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
package org.onosproject.cordvtn;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.net.config.ConfigFactory;
import org.onosproject.net.config.NetworkConfigEvent;
import org.onosproject.net.config.NetworkConfigListener;
import org.onosproject.net.config.NetworkConfigRegistry;
import org.onosproject.net.config.NetworkConfigService;
import org.onosproject.net.config.basics.SubjectFactories;
import org.slf4j.Logger;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Reads node information from the network config file and handles the config
 * update events.
 * Only a leader controller performs the node addition or deletion.
 */
@Component(immediate = true)
public class CordVtnConfigManager {

    protected final Logger log = getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected NetworkConfigRegistry configRegistry;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected NetworkConfigService configService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CordVtnService cordVtnService;

    private final ConfigFactory configFactory =
            new ConfigFactory(SubjectFactories.APP_SUBJECT_FACTORY, CordVtnConfig.class, "cordvtn") {
                @Override
                public CordVtnConfig createConfig() {
                    return new CordVtnConfig();
                }
            };

    private final NetworkConfigListener configListener = new InternalConfigListener();

    private ApplicationId appId;

    @Activate
    protected void active() {
        appId = coreService.getAppId(CordVtnService.CORDVTN_APP_ID);

        configService.addListener(configListener);
        configRegistry.registerConfigFactory(configFactory);
    }

    @Deactivate
    protected void deactivate() {
        configRegistry.unregisterConfigFactory(configFactory);
        configService.removeListener(configListener);
    }

    private void readConfiguration() {
        CordVtnConfig config = configRegistry.getConfig(appId, CordVtnConfig.class);

        if (config == null) {
            log.warn("No configuration found");
            return;
        }

        config.cordVtnNodes().forEach(node -> {
            CordVtnNode cordVtnNode = new CordVtnNode(
                    node.hostname(), node.ovsdbIp(), node.ovsdbPort(), node.bridgeId());
            cordVtnService.addNode(cordVtnNode);
        });
    }

    private class InternalConfigListener implements NetworkConfigListener {

        @Override
        public void event(NetworkConfigEvent event) {
            if (!event.configClass().equals(CordVtnConfig.class)) {
                return;
            }

            switch (event.type()) {
                case CONFIG_ADDED:
                    log.info("Network configuration added");
                    readConfiguration();
                    break;
                case CONFIG_UPDATED:
                    log.info("Network configuration updated");
                    readConfiguration();
                    break;
                default:
                    break;
            }
        }
    }
}

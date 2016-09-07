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
package org.onosproject.provider.ospf.cfg.impl;

import com.fasterxml.jackson.databind.JsonNode;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Service;
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
import org.onosproject.net.provider.AbstractProvider;
import org.onosproject.net.provider.ProviderId;
import org.onosproject.ospf.controller.OspfController;
import org.slf4j.Logger;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Provider which advertises device descriptions to the core.
 */
@Component(immediate = true)
@Service
public class OspfCfgProvider extends AbstractProvider {

    static final String PROVIDER_ID = "org.onosproject.provider.ospf.cfg";
    private static final Logger log = getLogger(OspfCfgProvider.class);
    private final ConfigFactory configFactory =
            new ConfigFactory(SubjectFactories.APP_SUBJECT_FACTORY, OspfAppConfig.class, "ospfapp") {
                @Override
                public OspfAppConfig createConfig() {
                    return new OspfAppConfig();
                }
            };
    private final NetworkConfigListener configListener = new InternalConfigListener();
    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected OspfController ospfController;
    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CoreService coreService;
    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected NetworkConfigRegistry configRegistry;
    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected NetworkConfigService configService;
    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected OspfController controller;
    private ApplicationId appId;

    /**
     * Creates an OSPF device provider.
     */
    public OspfCfgProvider() {
        super(new ProviderId("ospf", PROVIDER_ID));
    }

    public void setOspfController(OspfController ospfController) {
        this.ospfController = ospfController;
    }

    @Activate
    public void activate() {
        appId = coreService.registerApplication(PROVIDER_ID);
        configService.addListener(configListener);
        configRegistry.registerConfigFactory(configFactory);
        log.info("activated...!!!");
    }

    @Deactivate
    public void deactivate() {
        configRegistry.unregisterConfigFactory(configFactory);
        configService.removeListener(configListener);
        log.info("deactivated...!!!");
    }

    private void updateConfig() {
        OspfAppConfig ospfAppConfig = configRegistry.getConfig(appId, OspfAppConfig.class);
        if ("ADD".equalsIgnoreCase(ospfAppConfig.method())) {
            JsonNode jsonNode = ospfAppConfig.processes();
            ospfController.updateConfig(jsonNode);
        } else {
            log.debug("Please signify prop1 and prop2");
        }
    }

    /**
     * OSPF config listener to populate the configuration.
     */
    private class InternalConfigListener implements NetworkConfigListener {
        @Override
        public void event(NetworkConfigEvent event) {
            log.debug("InternalConfigListener:: event is getting called");
            if (!event.configClass().equals(OspfAppConfig.class)) {
                return;
            }
            switch (event.type()) {
                case CONFIG_ADDED:
                    updateConfig();
                    break;
                case CONFIG_UPDATED:
                    updateConfig();
                    break;
                case CONFIG_REMOVED:
                    break;
                default:
                    break;
            }
        }
    }
}
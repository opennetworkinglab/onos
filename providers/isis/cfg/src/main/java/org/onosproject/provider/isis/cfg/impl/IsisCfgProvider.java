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

package org.onosproject.provider.isis.cfg.impl;

import com.fasterxml.jackson.databind.JsonNode;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.isis.controller.IsisController;
import org.onosproject.net.config.ConfigFactory;
import org.onosproject.net.config.NetworkConfigEvent;
import org.onosproject.net.config.NetworkConfigListener;
import org.onosproject.net.config.NetworkConfigRegistry;
import org.onosproject.net.config.NetworkConfigService;
import org.onosproject.net.config.basics.SubjectFactories;
import org.onosproject.net.provider.AbstractProvider;
import org.onosproject.net.provider.ProviderId;
import org.slf4j.Logger;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * ISIS config provider to validate and populate the configuration.
 */
@Component(immediate = true)
@Service
public class IsisCfgProvider extends AbstractProvider {

    private static final String PROVIDER_ID = "org.onosproject.provider.isis.cfg";
    private static final Logger log = getLogger(IsisCfgProvider.class);
    private final ConfigFactory configFactory =
            new ConfigFactory(SubjectFactories.APP_SUBJECT_FACTORY, IsisAppConfig.class, "isisapp") {
                @Override
                public IsisAppConfig createConfig() {
                    return new IsisAppConfig();
                }
            };
    private final NetworkConfigListener configListener = new InternalConfigListener();
    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected IsisController isisController;
    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CoreService coreService;
    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected NetworkConfigRegistry configRegistry;
    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected NetworkConfigService configService;
    private ApplicationId appId;

    /**
     * Creates an ISIS config provider.
     */
    public IsisCfgProvider() {
        super(new ProviderId("isis", PROVIDER_ID));
    }

    @Activate
    public void activate() {
        log.debug("Activate...!!!");
        appId = coreService.registerApplication(PROVIDER_ID);
        configService.addListener(configListener);
        configRegistry.registerConfigFactory(configFactory);
        log.debug("ISIS cfg service got started");
    }

    @Deactivate
    public void deactivate() {
        log.debug("Deactivate...!!!");
        configRegistry.unregisterConfigFactory(configFactory);
        configService.removeListener(configListener);
    }

    /**
     * Pushes the configuration changes to ISIS controller.
     */
    private void updateConfig() {
        IsisAppConfig isisAppConfig = configRegistry.getConfig(appId, IsisAppConfig.class);
        log.debug("IsisAppConfig::config processes::" + isisAppConfig.processes());
        log.debug("IsisAppConfig::prop 1::" + isisAppConfig.method());
        if ("ADD".equalsIgnoreCase(isisAppConfig.method())) {
            JsonNode jsonNode = isisAppConfig.processes();
            isisController.updateConfig(jsonNode);
        }
    }

    /**
     * Isis config listener to populate the configuration.
     */
    private class InternalConfigListener implements NetworkConfigListener {

        @Override
        public void event(NetworkConfigEvent event) {
            log.debug("config event is getting called...!!!");
            if (!event.configClass().equals(IsisAppConfig.class)) {
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
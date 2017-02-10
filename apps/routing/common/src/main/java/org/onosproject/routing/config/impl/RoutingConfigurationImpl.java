/*
 * Copyright 2017-present Open Networking Laboratory
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

package org.onosproject.routing.config.impl;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.incubator.net.intf.InterfaceService;
import org.onosproject.net.config.ConfigFactory;
import org.onosproject.net.config.NetworkConfigRegistry;
import org.onosproject.net.config.NetworkConfigService;
import org.onosproject.net.config.basics.SubjectFactories;
import org.onosproject.routing.config.BgpConfig;
import org.onosproject.routing.config.RouterConfig;
import org.onosproject.routing.config.RoutersConfig;
import org.onosproject.routing.config.RoutingConfigurationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of RoutingConfigurationService which reads routing
 * configuration from the network configuration service.
 */
@Component(immediate = true)
@Service
public class RoutingConfigurationImpl implements RoutingConfigurationService {

    private final Logger log = LoggerFactory.getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected NetworkConfigRegistry registry;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected NetworkConfigService configService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected InterfaceService interfaceService;

    private ConfigFactory<ApplicationId, BgpConfig> bgpConfigFactory =
            new ConfigFactory<ApplicationId, BgpConfig>(
                    SubjectFactories.APP_SUBJECT_FACTORY, BgpConfig.class, "bgp") {
        @Override
        public BgpConfig createConfig() {
            return new BgpConfig();
        }
    };

    private ConfigFactory<ApplicationId, RouterConfig> routerConfigFactory =
            new ConfigFactory<ApplicationId, RouterConfig>(
                    SubjectFactories.APP_SUBJECT_FACTORY, RouterConfig.class, "router") {
        @Override
        public RouterConfig createConfig() {
            return new RouterConfig();
        }
    };

    private ConfigFactory<ApplicationId, RoutersConfig> routersConfigFactory =
            new ConfigFactory<ApplicationId, RoutersConfig>(
                    SubjectFactories.APP_SUBJECT_FACTORY, RoutersConfig.class, "routers", true) {
                @Override
                public RoutersConfig createConfig() {
                    return new RoutersConfig();
                }
            };

    @Activate
    public void activate() {
        registry.registerConfigFactory(bgpConfigFactory);
        registry.registerConfigFactory(routerConfigFactory);
        registry.registerConfigFactory(routersConfigFactory);
        log.info("Routing configuration service started");
    }

    @Deactivate
    public void deactivate() {
        registry.unregisterConfigFactory(bgpConfigFactory);
        registry.unregisterConfigFactory(routerConfigFactory);
        registry.unregisterConfigFactory(routersConfigFactory);
        log.info("Routing configuration service stopped");
    }

}

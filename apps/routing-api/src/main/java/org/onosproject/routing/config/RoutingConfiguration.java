/*
 * Copyright 2017-present Open Networking Foundation
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

package org.onosproject.routing.config;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;
import org.onosproject.core.ApplicationId;
import org.onosproject.net.config.ConfigFactory;
import org.onosproject.net.config.NetworkConfigRegistry;
import org.onosproject.net.config.NetworkConfigService;
import org.onosproject.net.config.basics.SubjectFactories;
import org.onosproject.routing.RoutingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Set;

/**
 * Helper class to manage routing configurations.
 */
public final class RoutingConfiguration {

    private static final String WARNING =
            "Config apps/org.onosproject.routing/router is deprecated "
                    + "and will be removed in a future release.";
    private static final String WARNING2 =
            "Use apps/org.onosproject.routing/routers instead";

    private static final Logger log = LoggerFactory.getLogger(RoutingConfiguration.class);

    private static ConfigFactory<ApplicationId, BgpConfig> bgpConfigFactory =
            new ConfigFactory<ApplicationId, BgpConfig>(
                    SubjectFactories.APP_SUBJECT_FACTORY, BgpConfig.class, "bgp") {
                @Override
                public BgpConfig createConfig() {
                    return new BgpConfig();
                }
            };

    private static ConfigFactory<ApplicationId, RouterConfig> routerConfigFactory =
            new ConfigFactory<ApplicationId, RouterConfig>(
                    SubjectFactories.APP_SUBJECT_FACTORY, RouterConfig.class, "router") {
                @Override
                public RouterConfig createConfig() {
                    return new RouterConfig();
                }
            };

    private static ConfigFactory<ApplicationId, RoutersConfig> routersConfigFactory =
            new ConfigFactory<ApplicationId, RoutersConfig>(
                    SubjectFactories.APP_SUBJECT_FACTORY, RoutersConfig.class, "routers", true) {
                @Override
                public RoutersConfig createConfig() {
                    return new RoutersConfig();
                }
            };

    private static ImmutableList<ConfigFactory<?, ?>> factories = ImmutableList.<ConfigFactory<?, ?>>builder()
            .add(bgpConfigFactory)
            .add(routerConfigFactory)
            .add(routersConfigFactory)
            .build();

    private static Integer registrations = 0;
    private static final Object REGISTRATIONS_LOCK = new Object();

    private RoutingConfiguration() {
        // make checkstyle happy
    }

    /**
     * Registers the routing configuration factories.
     *
     * @param registry network config registry service
     */
    public static void register(NetworkConfigRegistry registry) {
        synchronized (REGISTRATIONS_LOCK) {
            if (registrations == 0) {
                factories.forEach(registry::registerConfigFactory);
            }
            registrations++;
        }
    }

    /**
     * Unregisters the routing configuration factories.
     * <p>The factories will only be unregistered from the network config
     * registry if no other routing applications are using them. Any components
     * that call NetworkConfigHelper#registerConfigFactories need to also call
     * this method when they are no longer using the config
     * </p>
     *
     * @param registry network config registry service
     */
    public static void unregister(NetworkConfigRegistry registry) {
        synchronized (REGISTRATIONS_LOCK) {
            registrations--;
            if (registrations == 0) {
                factories.forEach(registry::unregisterConfigFactory);
            }
        }
    }

    /**
     * Retrieves the router configurations.
     *
     * @param configService network config service
     * @param routingAppId routing app ID
     * @return set of router configurations
     */
    public static Set<RoutersConfig.Router> getRouterConfigurations(
            NetworkConfigService configService, ApplicationId routingAppId) {

        RouterConfig config = configService.getConfig(
                routingAppId, RoutingService.ROUTER_CONFIG_CLASS);
        RoutersConfig multiConfig = configService.getConfig(routingAppId, RoutersConfig.class);

        if (config != null) {
            log.warn(WARNING);
            log.warn(WARNING2);

            return Collections.singleton(
                    new RoutersConfig.Router(config.getControlPlaneConnectPoint(),
                            config.getOspfEnabled(),
                            Sets.newHashSet(config.getInterfaces())));
        } else if (multiConfig != null) {
            return multiConfig.getRouters();
        } else {
            return Collections.emptySet();
        }
    }
}

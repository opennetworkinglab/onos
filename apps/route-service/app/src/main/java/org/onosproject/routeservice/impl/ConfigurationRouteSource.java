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

package org.onosproject.routeservice.impl;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.onosproject.core.ApplicationId;
import org.onosproject.routeservice.Route;
import org.onosproject.routeservice.RouteAdminService;
import org.onosproject.routeservice.RouteConfig;
import org.onosproject.net.config.ConfigFactory;
import org.onosproject.net.config.NetworkConfigEvent;
import org.onosproject.net.config.NetworkConfigListener;
import org.onosproject.net.config.NetworkConfigRegistry;
import org.onosproject.net.config.basics.SubjectFactories;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Route source that installs static routes configured in the network configuration.
 */
@Component(immediate = true)
public class ConfigurationRouteSource {

    private static final Logger log = LoggerFactory.getLogger(ConfigurationRouteSource.class);

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected NetworkConfigRegistry netcfgRegistry;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected RouteAdminService routeService;

    private final ConfigFactory<ApplicationId, RouteConfig> routeConfigFactory =
            new ConfigFactory<ApplicationId, RouteConfig>(
                    SubjectFactories.APP_SUBJECT_FACTORY,
                    RouteConfig.class, "routes", true) {
                @Override
                public RouteConfig createConfig() {
                    return new RouteConfig();
                }
            };
    private final InternalNetworkConfigListener netcfgListener =
            new InternalNetworkConfigListener();

    @Activate
    protected void activate() {
        netcfgRegistry.addListener(netcfgListener);
        netcfgRegistry.registerConfigFactory(routeConfigFactory);

        // Read initial routes in netcfg
        netcfgRegistry.getSubjects(ApplicationId.class, RouteConfig.class).forEach(subject -> {
            Optional.ofNullable(netcfgRegistry.getConfig(subject, RouteConfig.class))
                    .map(RouteConfig::getRoutes)
                    .ifPresent(routes -> {
                        log.info("Load initial routes from netcfg: {}", routes);
                        routeService.update(routes);
                    });
        });

    }

    @Deactivate
    protected void deactivate() {
        netcfgRegistry.removeListener(netcfgListener);
        netcfgRegistry.unregisterConfigFactory(routeConfigFactory);
    }

    private void processRouteConfigAdded(NetworkConfigEvent event) {
        Set<Route> routes = ((RouteConfig) event.config().get()).getRoutes();
        routeService.update(routes);
    }

    private void processRouteConfigUpdated(NetworkConfigEvent event) {
        Set<Route> routes = ((RouteConfig) event.config().get()).getRoutes();
        Set<Route> prevRoutes = ((RouteConfig) event.prevConfig().get()).getRoutes();
        Set<Route> pendingRemove = prevRoutes.stream()
                .filter(prevRoute -> routes.stream()
                        .noneMatch(route -> route.prefix().equals(prevRoute.prefix())))
                .collect(Collectors.toSet());
        Set<Route> pendingUpdate = routes.stream()
                .filter(route -> !pendingRemove.contains(route)).collect(Collectors.toSet());
        routeService.update(pendingUpdate);
        routeService.withdraw(pendingRemove);
    }

    private void processRouteConfigRemoved(NetworkConfigEvent event) {
        Set<Route> prevRoutes = ((RouteConfig) event.prevConfig().get()).getRoutes();
        routeService.withdraw(prevRoutes);
    }

    private class InternalNetworkConfigListener implements
            NetworkConfigListener {
        @Override
        public void event(NetworkConfigEvent event) {
            if (event.configClass().equals(RouteConfig.class)) {
                switch (event.type()) {
                case CONFIG_ADDED:
                    processRouteConfigAdded(event);
                    break;
                case CONFIG_UPDATED:
                    processRouteConfigUpdated(event);
                    break;
                case CONFIG_REMOVED:
                    processRouteConfigRemoved(event);
                    break;
                default:
                    break;
                }
            }
        }
    }
}

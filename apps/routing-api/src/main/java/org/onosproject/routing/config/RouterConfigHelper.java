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

package org.onosproject.routing.config;

import com.google.common.collect.Sets;
import org.onosproject.core.ApplicationId;
import org.onosproject.net.config.NetworkConfigService;
import org.onosproject.routing.RoutingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Set;

/**
 * Helper class to manage retrieving config from multiple Config sections.
 * Should be unnecessary once old config is removed.
 */
public final class RouterConfigHelper {

    private static final String WARNING =
            "Config apps/org.onosproject.routing/router is deprecated "
                    + "and will be removed in a future release.";
    private static final String WARNING2 =
            "Use apps/org.onosproject.routing/routers instead";

    private static final Logger log = LoggerFactory.getLogger(RouterConfigHelper.class);

    private RouterConfigHelper() {
        // make checkstyle happy
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

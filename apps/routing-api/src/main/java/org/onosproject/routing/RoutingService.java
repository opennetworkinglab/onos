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
package org.onosproject.routing;

import org.onosproject.routing.config.BgpConfig;
import org.onosproject.routing.config.RouterConfig;

/**
 * Historical interface now used only as a centralised place to define routing
 * config related constants.
 */
public interface RoutingService {

    String ROUTER_APP_ID = "org.onosproject.router";

    Class<BgpConfig> CONFIG_CLASS = BgpConfig.class;
    Class<RouterConfig> ROUTER_CONFIG_CLASS = RouterConfig.class;

    /**
     * Empty method to pacify checkstyle.
     */
    default void nothing() {
    }
}

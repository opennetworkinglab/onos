/*
 * Copyright 2016 Open Networking Laboratory
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
package org.onosproject.openstacknetworking.routing;

import org.onosproject.core.ApplicationId;
import org.onosproject.net.config.Config;
import org.slf4j.Logger;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Configuration object for OpenstackRouting service.
 */
public class OpenstackRoutingConfig extends Config<ApplicationId> {
    protected final Logger log = getLogger(getClass());

    public static final String PHYSICAL_ROUTER_MAC = "physicalRouterMac";
    public static final String GATEWAY_BRIDGE_ID = "gatewayBridgeId";
    public static final String GATEWAY_EXTERNAL_INTERFACE_NAME = "gatewayExternalInterfaceName";
    public static final String GATEWAY_EXTERNAL_INTERFACE_MAC = "gatewayExternalInterfaceMac";

    /**
     * Returns physical router mac.
     *
     * @return physical router mac
     */
    public String physicalRouterMac() {
        return this.get("physicalRouterMac", "");
    }

    /**
     * Returns gateway's bridge id.
     *
     * @return bridge id
     */
    public String gatewayBridgeId() {
        return this.get("gatewayBridgeId", "");
    }

    /**
     * Returns gateway's external interface name.
     *
     * @return external interface name
     */
    public String gatewayExternalInterfaceName() {
        return this.get("gatewayExternalInterfaceName", "");
    }

    /**
     * Returns gateway's external interface mac.
     *
     * @return external interface mac
     */
    public String gatewayExternalInterfaceMac() {
        return this.get("gatewayExternalInterfaceMac", "");
    }
}

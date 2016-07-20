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
package org.onosproject.openstacknetworking;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.Maps;
import org.onlab.packet.Ip4Address;
import org.onosproject.net.DeviceId;
import org.onosproject.net.config.Config;
import org.slf4j.Logger;

import java.util.Map;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Network Config for OpenstackNetworking application.
 *
 */
public class OpenstackNetworkingConfig extends Config<String> {

    protected final Logger log = getLogger(getClass());

    public static final String PHYSICAL_ROUTER_MAC = "physicalRouterMac";
    public static final String GATEWAY_BRIDGE_ID = "gatewayBridgeId";
    public static final String GATEWAY_EXTERNAL_INTERFACE_NAME = "gatewayExternalInterfaceName";
    public static final String GATEWAY_EXTERNAL_INTERFACE_MAC = "gatewayExternalInterfaceMac";

    public static final String NODES = "nodes";
    public static final String DATAPLANE_IP = "dataPlaneIp";
    public static final String BRIDGE_ID = "bridgeId";


    /**
     * Returns physical router mac.
     *
     * @return physical router mac
     */
    public String physicalRouterMac() {
        return this.get(PHYSICAL_ROUTER_MAC, "");
    }

    /**
     * Returns gateway's bridge id.
     *
     * @return bridge id
     */
    public String gatewayBridgeId() {
        return this.get(GATEWAY_BRIDGE_ID, "");
    }

    /**
     * Returns gateway's external interface name.
     *
     * @return external interface name
     */
    public String gatewayExternalInterfaceName() {
        return this.get(GATEWAY_EXTERNAL_INTERFACE_NAME, "");
    }

    /**
     * Returns gateway's external interface mac.
     *
     * @return external interface mac
     */
    public String gatewayExternalInterfaceMac() {
        return this.get(GATEWAY_EXTERNAL_INTERFACE_MAC, "");
    }

    /**
     * Returns the data plane IP map of nodes read from network config.
     *
     * @return data plane IP map
     */
    public Map<DeviceId, Ip4Address> nodes() {
        Map<DeviceId, Ip4Address> nodeMap = Maps.newHashMap();

        JsonNode jsonNodes = object.get(NODES);
        if (jsonNodes == null) {
            log.error("There's no node information");
            return null;
        }

        jsonNodes.forEach(jsonNode -> {
            try {
                nodeMap.putIfAbsent(DeviceId.deviceId(jsonNode.path(BRIDGE_ID).asText()),
                        Ip4Address.valueOf(jsonNode.path(DATAPLANE_IP).asText()));
            } catch (IllegalArgumentException | NullPointerException e) {
                log.error("Failed to read {}", e.toString());
            }
        });
        return nodeMap;
    }

}

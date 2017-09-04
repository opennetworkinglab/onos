/*
 * Copyright 2016-present Open Networking Foundation
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
package org.onosproject.scalablegateway.api;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Sets;
import org.onlab.packet.Ip4Address;
import org.onosproject.core.ApplicationId;
import org.onosproject.net.DeviceId;
import org.onosproject.net.config.Config;
import org.slf4j.Logger;

import java.util.Set;
import java.util.stream.StreamSupport;

import static org.onosproject.net.config.Config.FieldPresence.MANDATORY;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Configuration object for OpensatckNode service.
 */
public class GatewayNodeConfig extends Config<ApplicationId> {

    protected final Logger log = getLogger(getClass());

    public static final String NODES = "nodes";
    public static final String BRIDGE_ID = "bridgeId";
    public static final String DATAPLANE_IP = "dataPlaneIp";
    public static final String UPLINK_INTERFACE_NAME = "uplinkInterface";

    /**
     * Returns the set of nodes read from network config.
     *
     * @return set of OpenstackNodeConfig or null
     */
    public Set<GatewayNode> gatewayNodes() {

        Set<GatewayNode> nodes = Sets.newHashSet();

        JsonNode jsonNodes = object.get(NODES);
        if (jsonNodes == null) {
            return null;
        }

        jsonNodes.forEach(jsonNode -> {
            try {
                nodes.add(new GatewayNode.Builder()
                        .gatewayDeviceId(DeviceId.deviceId(jsonNode.path(BRIDGE_ID).asText()))
                        .uplinkIntf(jsonNode.path(UPLINK_INTERFACE_NAME).asText())
                        .dataIpAddress(Ip4Address.valueOf(jsonNode.path(DATAPLANE_IP).asText())).build());
            } catch (IllegalArgumentException | NullPointerException e) {
                log.error("Failed to read {}", e.toString());
            }
        });
        return nodes;
    }

    @Override
    public boolean isValid() {
        JsonNode jsonNodes = object.get(NODES);

        if (jsonNodes == null) {
            return false;
        }

        return hasOnlyFields(NODES)
                && StreamSupport.stream(jsonNodes.spliterator(), false).allMatch(this::checkValid);
    }

    private boolean checkValid(JsonNode jsonNode) {
        ObjectNode objectNode = (ObjectNode) jsonNode;
        return isString(objectNode, BRIDGE_ID, MANDATORY)
                && isIpAddress(objectNode, DATAPLANE_IP, MANDATORY)
                && isString(objectNode, UPLINK_INTERFACE_NAME, MANDATORY);
    }

}

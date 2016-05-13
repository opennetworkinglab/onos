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
package org.onosproject.scalablegateway.api;


import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.onlab.packet.Ip4Address;
import org.onosproject.core.ApplicationId;
import org.onosproject.net.DeviceId;
import org.onosproject.net.config.Config;
import org.slf4j.Logger;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.slf4j.LoggerFactory.getLogger;
import static org.onosproject.net.config.Config.FieldPresence.MANDATORY;

/**
 * Configuration object for OpensatckNode service.
 */
public class GatewayNodeConfig extends Config<ApplicationId> {

    protected final Logger log = getLogger(getClass());

    public static final String NODES = "nodes";
    public static final String BRIDGE_ID = "bridgeId";
    public static final String DATAPLANE_IP = "dataPlaneIp";
    public static final String EXTERNAL_INTERFACE_NAME = "gatewayExternalInterfaceName";

    /**
     * Returns the set of nodes read from network config.
     *
     * @return set of OpensatckNodeConfig or null
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
                        .gatewayExternalInterfaceNames(
                                getExternalInterfaceName(jsonNode.path(EXTERNAL_INTERFACE_NAME).asText()))
                        .dataIpAddress(Ip4Address.valueOf(jsonNode.path(DATAPLANE_IP).asText())).build());
            } catch (IllegalArgumentException | NullPointerException e) {
                log.error("Failed to read {}", e.toString());
            }
        });
        return nodes;
    }

    private List<String> getExternalInterfaceName(String s) {
        List<String> list = Lists.newArrayList();
        return Collections.addAll(list, s.split(",")) ? list : null;
    }

    @Override
    public boolean isValid() {
        return hasOnlyFields(NODES, BRIDGE_ID, DATAPLANE_IP, EXTERNAL_INTERFACE_NAME) &&
                isIpAddress(DATAPLANE_IP, MANDATORY) && isString(BRIDGE_ID, MANDATORY) &&
                isString(EXTERNAL_INTERFACE_NAME, MANDATORY);
    }

}

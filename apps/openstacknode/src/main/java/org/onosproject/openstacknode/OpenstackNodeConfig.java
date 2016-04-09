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
package org.onosproject.openstacknode;


import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.Sets;
import org.onlab.packet.Ip4Address;
import org.onlab.packet.MacAddress;
import org.onlab.packet.TpPort;
import org.onosproject.core.ApplicationId;
import org.onosproject.net.DeviceId;
import org.slf4j.Logger;
import java.util.Set;
import org.onosproject.net.config.Config;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Configuration object for OpensatckNode service.
 */
public class OpenstackNodeConfig extends Config<ApplicationId> {

    protected final Logger log = getLogger(getClass());


    public static final String NODES = "nodes";
    public static final String HOST_NAME = "hostname";
    public static final String OVSDB_IP = "ovsdbIp";
    public static final String OVSDB_PORT = "ovsdbPort";
    public static final String BRIDGE_ID = "bridgeId";
    public static final String NODE_TYPE = "openstackNodeType";
    public static final String GATEWAY_EXTERNAL_INTERFACE_NAME = "gatewayExternalInterfaceName";
    public static final String GATEWAY_EXTERNAL_INTERFACE_MAC = "gatewayExternalInterfaceMac";

    /**
     * Returns the set of nodes read from network config.
     *
     * @return set of OpensatckNodeConfig or null
     */
    public Set<OpenstackNode> openstackNodes() {

        Set<OpenstackNode> nodes = Sets.newHashSet();

        JsonNode jsonNodes = object.get(NODES);
        if (jsonNodes == null) {
            return null;
        }

        jsonNodes.forEach(jsonNode -> {
            try {
                if (OpenstackNodeService.OpenstackNodeType.valueOf(jsonNode.path(NODE_TYPE).asText()) ==
                        OpenstackNodeService.OpenstackNodeType.COMPUTENODE) {
                    nodes.add(new OpenstackNode(
                            jsonNode.path(HOST_NAME).asText(),
                            Ip4Address.valueOf(jsonNode.path(OVSDB_IP).asText()),
                            TpPort.tpPort(jsonNode.path(OVSDB_PORT).asInt()),
                            DeviceId.deviceId(jsonNode.path(BRIDGE_ID).asText()),
                            OpenstackNodeService.OpenstackNodeType.valueOf(jsonNode.path(NODE_TYPE).asText()),
                            null, MacAddress.NONE));
                } else {
                    nodes.add(new OpenstackNode(
                            jsonNode.path(HOST_NAME).asText(),
                            Ip4Address.valueOf(jsonNode.path(OVSDB_IP).asText()),
                            TpPort.tpPort(jsonNode.path(OVSDB_PORT).asInt()),
                            DeviceId.deviceId(jsonNode.path(BRIDGE_ID).asText()),
                            OpenstackNodeService.OpenstackNodeType.valueOf(jsonNode.path(NODE_TYPE).asText()),
                            jsonNode.path(GATEWAY_EXTERNAL_INTERFACE_NAME).asText(),
                            MacAddress.valueOf(jsonNode.path(GATEWAY_EXTERNAL_INTERFACE_MAC).asText())));
                }
            } catch (IllegalArgumentException | NullPointerException e) {
                log.error("Failed to read {}", e.toString());
            }
        });
        return nodes;
    }
}

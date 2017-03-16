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
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Sets;
import org.onlab.packet.IpAddress;
import org.onosproject.core.ApplicationId;
import org.onosproject.net.DeviceId;
import org.onosproject.openstacknode.OpenstackNodeService.NodeType;
import java.util.Set;
import org.onosproject.net.config.Config;

import static org.onosproject.net.config.Config.FieldPresence.MANDATORY;
import static org.onosproject.net.config.Config.FieldPresence.OPTIONAL;
import static org.onosproject.openstacknode.OpenstackNodeService.NodeType.GATEWAY;

/**
 * Configuration object for OpensatckNode service.
 */
public final class OpenstackNodeConfig extends Config<ApplicationId> {

    private static final String NODES = "nodes";
    private static final String HOST_NAME = "hostname";
    private static final String TYPE = "type";
    private static final String MANAGEMENT_IP = "managementIp";
    private static final String DATA_IP = "dataIp";
    private static final String INTEGRATION_BRIDGE = "integrationBridge";

    // GATEWAY node specific fields
    private static final String ROUTER_BRIDGE = "routerBridge";
    private static final String UPLINK_PORT_NAME = "uplinkPort";
    // TODO remove this when vRouter supports multiple switches
    private static final String ROUTER_CONTROLLER = "routerController";
    private static final String VLAN_PORT_NAME = "vlanPort";

    @Override
    public boolean isValid() {
        boolean result = hasOnlyFields(NODES);

        if (object.get(NODES) == null || object.get(NODES).size() < 1) {
            final String msg = "No node is present";
            throw new IllegalArgumentException(msg);
        }

        for (JsonNode node : object.get(NODES)) {
            if (get(node, DATA_IP) == null && get(node, VLAN_PORT_NAME) == null) {
                final String msg = "There is neither tunnel interface nor vlan port";
                throw new IllegalArgumentException(msg);
            }
            ObjectNode osNode = (ObjectNode) node;
            result &= hasOnlyFields(osNode,
                    HOST_NAME,
                    TYPE,
                    MANAGEMENT_IP,
                    DATA_IP,
                    INTEGRATION_BRIDGE,
                    ROUTER_BRIDGE,
                    UPLINK_PORT_NAME,
                    ROUTER_CONTROLLER,
                    VLAN_PORT_NAME
            );

            result &= isString(osNode, HOST_NAME, MANDATORY);
            result &= isString(osNode, TYPE, MANDATORY);
            result &= isIpAddress(osNode, MANAGEMENT_IP, MANDATORY);
            result &= isString(osNode, INTEGRATION_BRIDGE, MANDATORY);
            result &= isString(osNode, VLAN_PORT_NAME, OPTIONAL);
            result &= isIpAddress(osNode, DATA_IP, OPTIONAL);

            DeviceId.deviceId(osNode.get(INTEGRATION_BRIDGE).asText());
            NodeType.valueOf(osNode.get(TYPE).asText());

            if (osNode.get(TYPE).asText().equals(GATEWAY.name())) {
                result &= isString(osNode, ROUTER_BRIDGE, MANDATORY);
                DeviceId.deviceId(osNode.get(ROUTER_BRIDGE).asText());
                result &= isString(osNode, UPLINK_PORT_NAME, MANDATORY);
                result &= isIpAddress(osNode, ROUTER_CONTROLLER, MANDATORY);
            }
        }
        return result;
    }

    /**
     * Returns the set of nodes read from network config.
     *
     * @return set of openstack nodes
     */
    public Set<OpenstackNode> openstackNodes() {
        Set<OpenstackNode> nodes = Sets.newHashSet();
        for (JsonNode node : object.get(NODES)) {
            NodeType type = NodeType.valueOf(get(node, TYPE));
            OpenstackNode.Builder nodeBuilder = OpenstackNode.builder()
                    .integrationBridge(DeviceId.deviceId(get(node, INTEGRATION_BRIDGE)))
                    .managementIp(IpAddress.valueOf(get(node, MANAGEMENT_IP)))
                    .type(type)
                    .hostname(get(node, HOST_NAME));

            if (get(node, DATA_IP) != null) {
                nodeBuilder.dataIp(IpAddress.valueOf(get(node, DATA_IP)));
            }

            if (get(node, VLAN_PORT_NAME) != null) {
                nodeBuilder.vlanPort(get(node, VLAN_PORT_NAME));
            }

            if (type.equals(GATEWAY)) {
                nodeBuilder.routerBridge(DeviceId.deviceId(get(node, ROUTER_BRIDGE)))
                        .uplink(get(node, UPLINK_PORT_NAME))
                        .routerController(IpAddress.valueOf(get(node, ROUTER_CONTROLLER)));
            }
            nodes.add(nodeBuilder.build());
        }
        return nodes;
    }

    private String get(JsonNode jsonNode, String path) {
        JsonNode jNode = jsonNode.get(path);
        if (jNode == null || jNode.isMissingNode()) {
            return null;
        }
        return jNode.asText();
    }
}
